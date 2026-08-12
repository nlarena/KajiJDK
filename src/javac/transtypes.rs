//! **TransTypes** (§5.1.7 / §5.1.8): inserta las conversiones de **boxing/unboxing** que el contexto
//! pide pero que el bytecode necesita **explícitas** — `Integer.valueOf(x)` y `x.intValue()`—. Es el
//! `TransTypes` de javac, acotado a la parte de *boxing* (la *erasure* de genéricos ya la resuelve el
//! sistema de tipos por *erased ids*).
//!
//! Corre **después** de la atribución y de Flow, y **antes** del desugar: recorre el árbol ya tipado
//! (usa el `ty` de cada nodo y el `binding` resuelto de cada llamada) y, en cada **sitio de
//! conversión**, envuelve la expresión:
//!
//! - **box**: un valor primitivo donde se espera una **referencia** (`Integer`/`Number`/`Object`, o un
//!   parámetro de tipo borrado) → `Wrapper.valueOf(v)`.
//! - **unbox**: un *wrapper* donde se espera un **primitivo** (una asignación a `int`, o un operando de
//!   aritmética) → `v.intValue()` (etc.).
//!
//! Las llamadas insertadas quedan **sin decorar**; la re-atribución (posterior al desugar) las tipa y
//! resuelve, y el emisor saca de ahí el `invokestatic valueOf` / `invokevirtual xxxValue`. Contextos de
//! asignación cubiertos (§5.2): inicializador de `local`/campo, `return`, `=`, **argumentos** de
//! llamada/constructor, **operandos** de aritmética/relacionales, ramas del ternario, y el **cuerpo de
//! una lambda** contra el retorno de su SAM (el caso `Function<Integer,Integer> f = x -> x + 1`).

use super::ast::{
    BinOp, Binding, ClassDecl, CompilationUnit, Expr, ExprKind, LambdaBody, Member, PrimType, Stmt,
    StmtKind, SwitchBody, Type,
};
use super::attribute::{functional_sam, member_scope, resolve_rtype, substitute_member};
use super::symbol::{RType, Resolved, ScopeId, SymbolTable};
use super::types;

/// Punto de entrada: inserta las conversiones de boxing/unboxing en toda la unidad.
pub fn trans_types(unit: &mut CompilationUnit, table: &SymbolTable) {
    let base = unit.package.as_deref().unwrap_or("").to_string();
    let t = Trans { table };
    for class in &mut unit.types {
        t.class(class, &base);
    }
}

struct Trans<'a> {
    table: &'a SymbolTable,
}

impl Trans<'_> {
    fn class(&self, class: &mut ClassDecl, enclosing_fqn: &str) {
        let fqn = qualify(enclosing_fqn, &class.name);
        let Some(cid) = self.table.class(&fqn) else { return };
        let scope = member_scope(self.table, cid);
        for member in &mut class.members {
            match member {
                Member::Method(m) => {
                    let ret = resolve_rtype(self.table, scope, &m.return_type);
                    if let Some(body) = &mut m.body {
                        self.block(&mut body.0, scope, &ret);
                    }
                }
                // Un inicializador de campo se ejecuta en `<init>`/`<clinit>`: su valor se convierte al
                // tipo del campo.
                Member::Field(f) => {
                    let fty = resolve_rtype(self.table, scope, &f.ty);
                    if let Some(e) = &mut f.init {
                        self.expr(e, scope);
                        self.coerce(e, &fty);
                    }
                }
                Member::StaticInit(b) | Member::InstanceInit(b) => {
                    self.block(&mut b.0, scope, &RType::Void);
                }
                Member::Type(nested) => self.class(nested, &fqn),
            }
        }
    }

    /// `ret` es el tipo de retorno del método en curso — el *target* de un `return` (Void si no aplica).
    fn block(&self, stmts: &mut [Stmt], scope: ScopeId, ret: &RType) {
        for s in stmts.iter_mut() {
            self.stmt(s, scope, ret);
        }
    }

    fn stmt(&self, s: &mut Stmt, scope: ScopeId, ret: &RType) {
        // El tipo declarado de la variable de un `local` (ya resuelto por la atribución) es el *target*
        // de su inicializador — se toma antes de tomar prestado `s.kind`.
        let local_ty = s.local.as_ref().map(|l| l.ty.clone());
        match &mut s.kind {
            StmtKind::LocalVar { init: Some(e), .. } => {
                self.expr(e, scope);
                if let Some(t) = &local_ty {
                    self.coerce(e, t);
                }
            }
            StmtKind::ForEach { iterable, body, .. } => {
                self.expr(iterable, scope);
                self.stmt(body, scope, ret);
            }
            StmtKind::Return(Some(e)) => {
                self.expr(e, scope);
                self.coerce(e, ret);
            }
            StmtKind::Expr(e) | StmtKind::Throw(e) | StmtKind::Yield(e) => self.expr(e, scope),
            StmtKind::Assert { cond, message } => {
                self.expr(cond, scope);
                if let Some(m) = message {
                    self.expr(m, scope);
                }
            }
            StmtKind::If { cond, then, els } => {
                self.expr(cond, scope);
                self.coerce(cond, &RType::Prim(PrimType::Boolean));
                self.stmt(then, scope, ret);
                if let Some(e) = els {
                    self.stmt(e, scope, ret);
                }
            }
            StmtKind::While { cond, body } | StmtKind::Do { body, cond } => {
                self.expr(cond, scope);
                self.coerce(cond, &RType::Prim(PrimType::Boolean));
                self.stmt(body, scope, ret);
            }
            StmtKind::For { init, cond, update, body } => {
                if let Some(i) = init {
                    self.stmt(i, scope, ret);
                }
                if let Some(c) = cond {
                    self.expr(c, scope);
                    self.coerce(c, &RType::Prim(PrimType::Boolean));
                }
                for u in update.iter_mut() {
                    self.expr(u, scope);
                }
                self.stmt(body, scope, ret);
            }
            StmtKind::Block(b) => self.block(&mut b.0, scope, ret),
            StmtKind::Synchronized { lock, body } => {
                self.expr(lock, scope);
                self.block(&mut body.0, scope, ret);
            }
            StmtKind::Try { resources, body, catches, finally } => {
                for r in resources.iter_mut() {
                    self.stmt(r, scope, ret);
                }
                self.block(&mut body.0, scope, ret);
                for c in catches.iter_mut() {
                    self.block(&mut c.body.0, scope, ret);
                }
                if let Some(f) = finally {
                    self.block(&mut f.0, scope, ret);
                }
            }
            StmtKind::Switch { selector, cases } => {
                self.expr(selector, scope);
                for c in cases.iter_mut() {
                    if let Some(g) = &mut c.guard {
                        self.expr(g, scope);
                    }
                    match &mut c.body {
                        SwitchBody::Arrow(st) => self.stmt(st, scope, ret),
                        SwitchBody::Colon(ss) => {
                            for st in ss.iter_mut() {
                                self.stmt(st, scope, ret);
                            }
                        }
                    }
                }
            }
            StmtKind::Labeled { body, .. } => self.stmt(body, scope, ret),
            _ => {}
        }
    }

    fn expr(&self, e: &mut Expr, scope: ScopeId) {
        // La **lambda** se maneja antes del `match &mut e.kind`, porque necesita leer `e.ty` (su
        // interfaz funcional) para sacar el retorno del SAM, y no se puede con `e.kind` ya prestado.
        if matches!(e.kind, ExprKind::Lambda { .. }) {
            let sam_ret = self.lambda_sam_ret(e.ty.clone());
            if let ExprKind::Lambda { body, .. } = &mut e.kind {
                match body.as_mut() {
                    LambdaBody::Expr(x) => {
                        self.expr(x, scope);
                        if let Some(r) = &sam_ret {
                            self.coerce(x, r);
                        }
                    }
                    LambdaBody::Block(b) => {
                        let r = sam_ret.unwrap_or(RType::Unresolved);
                        self.block(&mut b.0, scope, &r);
                    }
                }
            }
            return;
        }
        match &mut e.kind {
            ExprKind::Binary { op, lhs, rhs } => {
                let op = *op;
                self.expr(lhs, scope);
                self.expr(rhs, scope);
                // Los operandos de una operación **numérica/relacional/lógica** se desempaquetan a su
                // primitivo (§5.6). La igualdad `==`/`!=` se deja: con dos *wrappers* es identidad de
                // referencia, no de valor.
                if !matches!(op, BinOp::Eq | BinOp::Ne) {
                    self.unbox_operand(lhs);
                    self.unbox_operand(rhs);
                }
            }
            ExprKind::Unary { expr, .. } => self.expr(expr, scope),
            ExprKind::Assign { op, target, value } => {
                let op = *op;
                self.expr(target, scope);
                self.expr(value, scope);
                // Una asignación **simple** convierte el valor al tipo del destino; la **compuesta**
                // lleva su propio cast (la baja el desugar), así que acá no se toca.
                if matches!(op, super::ast::AssignOp::Assign) {
                    if let Some(t) = target.ty.clone() {
                        self.coerce(value, &t);
                    }
                }
            }
            ExprKind::Ternary { cond, then, els } => {
                self.expr(cond, scope);
                self.coerce(cond, &RType::Prim(PrimType::Boolean));
                self.expr(then, scope);
                self.expr(els, scope);
                // Las dos ramas se promueven al tipo del todo (§15.25): si el ternario es de un
                // *wrapper* y una rama es primitiva, ésta se boxea (y viceversa).
                if let Some(t) = e.ty.clone() {
                    self.coerce(then, &t);
                    self.coerce(els, &t);
                }
            }
            ExprKind::Call { target, args, .. } => {
                if let Some(t) = target {
                    self.expr(t, scope);
                }
                for a in args.iter_mut() {
                    self.expr(a, scope);
                }
            }
            ExprKind::NewObject { outer, args, body, .. } => {
                if let Some(o) = outer {
                    self.expr(o, scope);
                }
                for a in args.iter_mut() {
                    self.expr(a, scope);
                }
                if let Some(members) = body {
                    // Cuerpo de una anónima: se recorre igual (sus métodos tienen sus propios retornos).
                    self.anon_members(members, scope);
                }
            }
            ExprKind::Field { expr, .. } => self.expr(expr, scope),
            ExprKind::Index { array, index } => {
                self.expr(array, scope);
                self.expr(index, scope);
                // El índice de un array es `int`: un `Integer` se desempaqueta.
                self.coerce(index, &RType::Prim(PrimType::Int));
            }
            ExprKind::Cast { expr, .. } => self.expr(expr, scope),
            ExprKind::InstanceOf { expr, .. } => self.expr(expr, scope),
            ExprKind::NewArray { dims, init, .. } => {
                for d in dims.iter_mut().flatten() {
                    self.expr(d, scope);
                    self.coerce(d, &RType::Prim(PrimType::Int));
                }
                if let Some(es) = init {
                    for x in es.iter_mut() {
                        self.expr(x, scope);
                    }
                }
            }
            ExprKind::Switch { selector, cases } => {
                self.expr(selector, scope);
                for c in cases.iter_mut() {
                    if let Some(g) = &mut c.guard {
                        self.expr(g, scope);
                    }
                    match &mut c.body {
                        SwitchBody::Arrow(st) => self.stmt(st, scope, &RType::Unresolved),
                        SwitchBody::Colon(ss) => {
                            for st in ss.iter_mut() {
                                self.stmt(st, scope, &RType::Unresolved);
                            }
                        }
                    }
                }
            }
            _ => {}
        }
        // Los **argumentos** de una llamada/constructor se convierten al tipo de su parámetro **después**
        // del match (acá `e.kind` ya no está prestado por el arm). No hace nada si `e` no es una llamada.
        self.coerce_args(e);
    }

    /// Recorre los cuerpos de método de una clase anónima (para el boxing de sus `return`/operandos).
    fn anon_members(&self, members: &mut [Member], scope: ScopeId) {
        for m in members.iter_mut() {
            match m {
                Member::Method(me) => {
                    let ret = resolve_rtype(self.table, scope, &me.return_type);
                    if let Some(b) = &mut me.body {
                        self.block(&mut b.0, scope, &ret);
                    }
                }
                Member::Field(f) => {
                    if let Some(e) = &mut f.init {
                        self.expr(e, scope);
                    }
                }
                Member::StaticInit(b) | Member::InstanceInit(b) => {
                    self.block(&mut b.0, scope, &RType::Void)
                }
                Member::Type(_) => {}
            }
        }
    }

    /// Convierte cada **argumento** de una llamada/constructor al tipo de su parámetro resuelto (§5.3).
    /// Un parámetro de tipo genérico está **borrado** a referencia, así que un `int` se boxea.
    fn coerce_args(&self, e: &mut Expr) {
        let Some(Binding::Method(mid)) = e.binding else { return };
        let Some(Resolved::Method { params, varargs, .. }) = self.table.resolved(mid) else { return };
        let params = params.clone();
        let varargs = *varargs;
        // Con varargs los argumentos de cola aún no se empaquetaron en un array (eso lo hace el
        // desugar, después): se convierten solo los del prefijo fijo.
        let fixed = if varargs { params.len().saturating_sub(1) } else { params.len() };
        let args = match &mut e.kind {
            ExprKind::Call { args, .. } | ExprKind::NewObject { args, .. } => args,
            _ => return,
        };
        for (i, a) in args.iter_mut().enumerate() {
            if i < fixed {
                if let Some(pt) = params.get(i) {
                    self.coerce(a, pt);
                }
            }
        }
    }

    /// El retorno del SAM de una interfaz funcional, sustituido con los argumentos de tipo del
    /// *target* de la lambda (`Function<Integer,Integer>` → `Integer`). `None` si no se puede resolver.
    fn lambda_sam_ret(&self, target: Option<RType>) -> Option<RType> {
        let target = target?;
        let cid = types::erased_id(&target)?;
        let sam = functional_sam(self.table, cid)?;
        let raw = match self.table.resolved(sam) {
            Some(Resolved::Method { ret, .. }) => ret.clone(),
            _ => return None,
        };
        Some(substitute_member(self.table, &target, sam, &raw))
    }

    /// Envuelve `e` con la conversión que lleva de su tipo al `target`, si hace falta una (§5.1.7/8).
    fn coerce(&self, e: &mut Expr, target: &RType) {
        let Some(src) = e.ty.clone() else { return };
        match (&src, target) {
            // box: primitivo → referencia (wrapper, super del wrapper, o parámetro de tipo borrado).
            (RType::Prim(p), t) if is_ref(t) => self.wrap_box(e, *p),
            // unbox: wrapper → primitivo.
            (RType::Class(c), RType::Prim(tp)) => {
                if let Some(p) = types::unboxed(self.table, *c) {
                    self.wrap_unbox(e, p);
                    // `long l = anInteger`: tras `intValue()` (int), un *widening* primitivo al target
                    // más ancho (§5.1.8 + §5.1.2). El `Cast` lo baja el codegen a `i2l`/`i2d`/…
                    if p != *tp {
                        self.wrap_cast(e, *tp);
                    }
                }
            }
            _ => {}
        }
    }

    fn wrap_cast(&self, e: &mut Expr, p: PrimType) {
        let pos = e.pos;
        let inner = std::mem::replace(e, Expr::new(pos, ExprKind::Null));
        e.kind = ExprKind::Cast { ty: Type::Prim(p), expr: Box::new(inner) };
        e.ty = Some(RType::Prim(p));
        e.binding = None;
    }

    /// Desempaqueta un operando si es un *wrapper* (para la aritmética): `Integer` → `x.intValue()`.
    fn unbox_operand(&self, e: &mut Expr) {
        if let Some(RType::Class(c)) = &e.ty {
            if let Some(p) = types::unboxed(self.table, *c) {
                self.wrap_unbox(e, p);
            }
        }
    }

    fn wrap_box(&self, e: &mut Expr, p: PrimType) {
        let wname = types::wrapper_of(p).to_string();
        let pos = e.pos;
        let inner = std::mem::replace(e, Expr::new(pos, ExprKind::Null));
        e.kind = ExprKind::Call {
            target: Some(Box::new(Expr::new(pos, ExprKind::Name(wname.clone())))),
            name: "valueOf".to_string(),
            args: vec![inner],
            type_args: Vec::new(),
        };
        e.ty = self.table.external(&wname).map(RType::Class);
        e.binding = None;
    }

    fn wrap_unbox(&self, e: &mut Expr, p: PrimType) {
        let pos = e.pos;
        let inner = std::mem::replace(e, Expr::new(pos, ExprKind::Null));
        e.kind = ExprKind::Call {
            target: Some(Box::new(inner)),
            name: format!("{}Value", prim_name(p)),
            args: Vec::new(),
            type_args: Vec::new(),
        };
        e.ty = Some(RType::Prim(p));
        e.binding = None;
    }
}

/// ¿Es `t` un **tipo referencia** (destino válido de un *boxing*)? — una clase/parametrizado/array o
/// un parámetro de tipo (borrado a referencia). No un primitivo ni `void`.
fn is_ref(t: &RType) -> bool {
    matches!(
        t,
        RType::Class(_) | RType::Parameterized { .. } | RType::Array(_) | RType::TypeVar(_)
    )
}

/// El nombre del primitivo, para armar el método de *unboxing* `xxxValue` (`int` → `intValue`).
fn prim_name(p: PrimType) -> &'static str {
    match p {
        PrimType::Int => "int",
        PrimType::Long => "long",
        PrimType::Short => "short",
        PrimType::Byte => "byte",
        PrimType::Char => "char",
        PrimType::Boolean => "boolean",
        PrimType::Float => "float",
        PrimType::Double => "double",
    }
}

fn qualify(enclosing: &str, name: &str) -> String {
    if enclosing.is_empty() {
        name.to_string()
    } else {
        format!("{enclosing}.{name}")
    }
}
