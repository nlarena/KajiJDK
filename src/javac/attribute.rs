//! La **pasada 2 — Attribute** (atribución): entra a los **cuerpos** de los métodos, resuelve
//! nombres y **tipa** cada expresión, consumiendo el grafo tipado que dejó la pasada 1
//! (`super::enter` / `super::symbol`). Ver el diseño en `docs/pasada2-attribute.md`.
//!
//! **Decora el AST in situ** (como el `Attr` de javac con `JCTree.type`/`sym`): a cada
//! [`Expr`] le escribe su `ty` y su `binding`, y a cada declaración de local su `local`
//! (slot + tipo resuelto). Esa decoración es la **salida** de la pasada — sin ella el codegen
//! no sabría qué `invoke*`/`get*` emitir, ni con qué descriptor, ni de qué slot leer.
//!
//! Cubre: resolución de nombres en los cuerpos (locales/params → campos, por la jerarquía),
//! tipado de todas las expresiones (con promoción numérica y `lub` en el ternario/`switch`),
//! **overload resolution en 3 fases** con boxing y varargs (§15.12.2), **genéricos** (subtipado
//! y sustitución de los argumentos del receptor, apoyado en [`types`](super::types)),
//! **inferencia** de los argumentos de tipo de una llamada ([`infer`](super::infer), Cap. 18), y
//! el chequeo de las sentencias.
//!
//! Cola larga: la inferencia desde el **target type** (`List<String> xs = emptyList()`, el
//! diamante `new ArrayList<>()`) — necesita el modo *checking* del chequeo bidireccional, y hoy
//! esto solo **sintetiza**; las *poly expressions* (lambdas/method refs); el *capture conversion*
//! (§5.1.10); y los inicializadores de campo.

use std::collections::HashMap;

use super::ast::{
    AssignOp, BinOp, Binding, Block, CaseLabel, ClassDecl, CompilationUnit, Expr, ExprKind,
    LambdaBody, LocalInfo, Member, MethodRefQualifier, Modifier, Pattern, Pos, PrimType, Stmt,
    StmtKind, SwitchBody, Type, TypeArg, TypeKind, UnOp,
};
use super::symbol::{RType, RTypeArg, Resolved, ScopeId, SymbolId, SymbolKind, SymbolTable};
use super::infer;
use super::types;
use super::Error;

/// Corre la atribución sobre todos los cuerpos de `unit`, **decorándolo** y devolviendo los
/// errores encontrados.
pub fn attribute(unit: &mut CompilationUnit, table: &SymbolTable) -> Vec<Error> {
    let mut errors = Vec::new();
    let base = unit.package.clone().unwrap_or_default();
    for class in &mut unit.types {
        attrib_class(table, &mut errors, class, &base);
    }
    errors
}

fn qualify(enclosing: &str, name: &str) -> String {
    if enclosing.is_empty() { name.to_string() } else { format!("{enclosing}.{name}") }
}

fn attrib_class(table: &SymbolTable, errors: &mut Vec<Error>, class: &mut ClassDecl, enclosing: &str) {
    let fqn = qualify(enclosing, &class.name);
    let Some(cid) = table.class(&fqn) else { return };
    let scope = member_scope(table, cid);
    for member in &mut class.members {
        match member {
            Member::Method(m) => {
                let ret = resolve_rtype(table, scope, &m.return_type);
                // Slots de la frame (JVMS §2.6.1): en un método de instancia el slot 0 es
                // `this`; los parámetros siguen en orden, y un `long`/`double` ocupa dos.
                let has_this = !m.modifiers.contains(&Modifier::Static);
                let mut env = Env {
                    table,
                    errors,
                    class: cid,
                    class_scope: scope,
                    ret,
                    has_this,
                    final_field_ok: m.is_constructor,
                    locals: Vec::new(),
                    next_slot: if has_this { 1 } else { 0 },
                };
                env.push();
                let params: Vec<(String, RType)> = m
                    .params
                    .iter()
                    .map(|p| (p.name.clone(), resolve_rtype(table, scope, &p.ty)))
                    .collect();
                for (name, rt) in params {
                    env.define(&name, rt);
                }
                if let Some(body) = &mut m.body {
                    attrib_block(&mut env, body);
                }
                env.pop();
            }
            Member::Type(nested) => attrib_class(table, errors, nested, &fqn),
            // Un inicializador estático: contexto **static** (sin `this`), retorno `void`, sin params.
            Member::StaticInit(block) => {
                let mut env = Env {
                    table,
                    errors,
                    class: cid,
                    class_scope: scope,
                    ret: RType::Void,
                    has_this: false,
                    final_field_ok: true,
                    locals: Vec::new(),
                    next_slot: 0,
                };
                env.push();
                attrib_block(&mut env, block);
                env.pop();
            }
            // Un inicializador de instancia corre **dentro del constructor**: tiene `this`.
            Member::InstanceInit(block) => {
                let mut env = Env {
                    table,
                    errors,
                    class: cid,
                    class_scope: scope,
                    ret: RType::Void,
                    has_this: true,
                    final_field_ok: true,
                    locals: Vec::new(),
                    next_slot: 1, // el 0 es `this`
                };
                env.push();
                attrib_block(&mut env, block);
                env.pop();
            }
            // El **inicializador de un campo** (§8.3.2): se tipa *in situ* contra el tipo declarado,
            // en contexto de asignación (con boxing/unboxing). Uno `static` corre en el `<clinit>` (sin
            // `this`); uno de instancia, dentro del constructor tras el `super()` (con `this`).
            Member::Field(f) => {
                if let Some(init) = &mut f.init {
                    let declared = resolve_rtype(table, scope, &f.ty);
                    let is_static = f.modifiers.contains(&Modifier::Static);
                    let mut env = Env {
                        table,
                        errors,
                        class: cid,
                        class_scope: scope,
                        ret: RType::Void,
                        has_this: !is_static,
                        final_field_ok: true,
                        locals: Vec::new(),
                        next_slot: if is_static { 0 } else { 1 },
                    };
                    env.push();
                    let it = attrib_expr_to(&mut env, init, Some(&declared));
                    // §5.2: una **constante** `int` que entra en el rango se asigna a `byte`/`short`/
                    // `char` sin cast (`static final byte MIN = -128;`).
                    let ok = assignable(env.table, &it, &declared)
                        || constant_narrowing_ok(&declared, init);
                    if !ok {
                        let name = f.name.clone();
                        env.error(f.pos, format!("tipo incompatible en el inicializador de `{name}`"));
                    }
                    env.pop();
                }
            }
        }
    }
}

/// Tipa el cuerpo de una **clase local** (§14.3) in situ, dentro de la atribución del método donde se
/// declara. A diferencia de `attrib_class`, sus métodos ven —además de sus propios miembros— los
/// **locales del método envolvente** (`env.locals` clonados): un local capturado resuelve a
/// `Binding::Local`, lo que deja al desugar detectarlo y bajarlo a un campo `val$x`. Los slots de
/// esta pasada son de descarte: el desugar levanta la clase y se re-atribuye después. El acceso a
/// miembros del enclosing (campos/métodos de instancia) ya lo cubre la cadena de `owner` de
/// `resolve_name` —una local es dueña del enclosing igual que una interna—, y su captura de `this$0`
/// la reusa el desugar.
fn attrib_local_class(env: &mut Env, lc: &mut ClassDecl) {
    let Some(cid) = env.table.resolve_type(env.class_scope, &lc.name) else { return };
    let scope = member_scope(env.table, cid);
    for member in &mut lc.members {
        let Member::Method(m) = member else { continue };
        let ret = resolve_rtype(env.table, scope, &m.return_type);
        let has_this = !m.modifiers.contains(&Modifier::Static);
        let mut sub = Env {
            table: env.table,
            errors: &mut *env.errors,
            class: cid,
            class_scope: scope,
            ret,
            has_this,
            final_field_ok: m.is_constructor,
            locals: env.locals.clone(),
            next_slot: if has_this { 1 } else { 0 },
        };
        sub.push();
        let params: Vec<(String, RType)> = m
            .params
            .iter()
            .map(|p| (p.name.clone(), resolve_rtype(env.table, scope, &p.ty)))
            .collect();
        for (name, rt) in params {
            sub.define(&name, rt);
        }
        if let Some(body) = &mut m.body {
            attrib_block(&mut sub, body);
        }
        sub.pop();
    }
}

// ---- entorno de atribución ----

/// Un ámbito de variables locales: sus declaraciones y el slot en que arrancó, para poder
/// **reusar los slots** al salir del bloque (como javac).
#[derive(Clone)]
struct LocalScope {
    vars: HashMap<String, (RType, u16)>,
    entry_slot: u16,
}

struct Env<'a> {
    table: &'a SymbolTable,
    errors: &'a mut Vec<Error>,
    class: SymbolId,
    class_scope: ScopeId,
    ret: RType,
    /// Si el método tiene `this` (slot 0) — falso en los `static`.
    has_this: bool,
    /// Si acá se puede **asignar un campo `final`** (§8.3.1.2): solo en un constructor o un
    /// inicializador (donde un *blank final* recibe su valor), no en un método normal.
    final_field_ok: bool,
    locals: Vec<LocalScope>,
    next_slot: u16,
}

impl Env<'_> {
    fn push(&mut self) {
        self.locals.push(LocalScope { vars: HashMap::new(), entry_slot: self.next_slot });
    }
    fn pop(&mut self) {
        if let Some(scope) = self.locals.pop() {
            self.next_slot = scope.entry_slot; // los slots del bloque quedan libres
        }
    }
    /// Declara un local y le asigna su **slot** (2 para `long`/`double`, JVMS §2.6.1).
    fn define(&mut self, name: &str, ty: RType) -> u16 {
        let slot = self.next_slot;
        self.next_slot += slot_width(&ty);
        self.locals.last_mut().unwrap().vars.insert(name.to_string(), (ty, slot));
        slot
    }
    fn lookup_local(&self, name: &str) -> Option<(RType, u16)> {
        self.locals.iter().rev().find_map(|s| s.vars.get(name).cloned())
    }
    fn error(&mut self, pos: Pos, message: String) {
        self.errors.push(Error::new(message, pos.line, pos.col));
    }
    /// Como [`error`](Self::error) pero con las sub-líneas `símbolo:`/`ubicación:` de `javac`.
    fn error_with_notes(&mut self, pos: Pos, message: String, notes: Vec<String>) {
        self.errors.push(Error::new(message, pos.line, pos.col).with_notes(notes));
    }
}

/// Las notas de un *cannot find symbol* al estilo `javac`: qué se buscaba (`símbolo:`) y dónde
/// (`ubicación:`), con las etiquetas alineadas (el valor arranca en la misma columna).
fn symbol_notes(symbol: String, location: String) -> Vec<String> {
    vec![format!("símbolo:   {symbol}"), format!("ubicación: {location}")]
}

/// **Distancia de edición** (Levenshtein) entre dos nombres — para el *did-you-mean*. Clásica DP por
/// filas, sobre `char`s (no bytes, por los identificadores con acentos).
fn levenshtein(a: &str, b: &str) -> usize {
    let a: Vec<char> = a.chars().collect();
    let b: Vec<char> = b.chars().collect();
    let mut prev: Vec<usize> = (0..=b.len()).collect();
    let mut cur = vec![0usize; b.len() + 1];
    for i in 1..=a.len() {
        cur[0] = i;
        for j in 1..=b.len() {
            let cost = if a[i - 1] == b[j - 1] { 0 } else { 1 };
            cur[j] = (prev[j] + 1).min(cur[j - 1] + 1).min(prev[j - 1] + cost);
        }
        std::mem::swap(&mut prev, &mut cur);
    }
    prev[b.len()]
}

/// La nota *did-you-mean* (`¿quisiste decir 'x'?`) con el nombre en `candidates` **más cercano** a
/// `name`, si cae dentro del umbral (`long/3`, mínimo 1 — así `fro`→`from` y `lenght`→`length`
/// sugieren, pero un nombre totalmente distinto no). `None` si ninguno está lo bastante cerca.
fn did_you_mean<'a>(name: &str, candidates: impl IntoIterator<Item = &'a str>) -> Option<String> {
    let threshold = (name.chars().count() / 3).max(1);
    let mut best: Option<(usize, &str)> = None;
    for c in candidates {
        if c == name {
            continue;
        }
        let d = levenshtein(name, c);
        if d <= threshold && best.map_or(true, |(bd, _)| d < bd) {
            best = Some((d, c));
        }
    }
    best.map(|(_, c)| format!("¿quisiste decir '{c}'?"))
}

/// Los **nombres** de los miembros (métodos o campos) alcanzables sobre `class`, recorriendo todo el
/// grafo de supertipos —igual que [`candidates`]—, deduplicados. Alimenta el *did-you-mean* de un
/// método/campo no hallado.
fn member_names(table: &SymbolTable, class: SymbolId, want_method: bool) -> Vec<String> {
    let mut out: Vec<String> = Vec::new();
    let mut stack = vec![class];
    let mut visited = vec![class];
    while let Some(c) = stack.pop() {
        for id in table.members_of(c) {
            let matches_kind = match table.symbol(id).kind {
                SymbolKind::Method { is_constructor: false, .. } => want_method,
                SymbolKind::Field { .. } => !want_method,
                _ => false,
            };
            if matches_kind {
                let n = table.symbol(id).name.clone();
                if !out.contains(&n) {
                    out.push(n);
                }
            }
        }
        let mut supers: Vec<SymbolId> = table.interfaces(c);
        supers.extend(table.super_class(c));
        for sup in supers {
            if !visited.contains(&sup) {
                visited.push(sup);
                stack.push(sup);
            }
        }
    }
    out
}

/// Una nota por cada **candidato de sobrecarga descartado** (§15.12.2), al estilo `javac`: la firma
/// del candidato (con el receptor sustituido) y, en la línea de abajo, **por qué** no aplica. El
/// render indenta cada línea de la nota, así el motivo queda anidado bajo la firma.
fn overload_candidate_notes(
    table: &SymbolTable,
    cands: &[SymbolId],
    args: &[RType],
    recv: &RType,
) -> Vec<String> {
    cands
        .iter()
        .filter_map(|&m| {
            let (raw, varargs) = signature_of(table, m)?;
            let params: Vec<RType> =
                raw.iter().map(|p| substitute_member(table, recv, m, p)).collect();
            let owner = table
                .symbol(m)
                .owner
                .map(|o| format!("{}.", table.symbol(o).name))
                .unwrap_or_default();
            let sig: Vec<String> = params.iter().map(|p| table.rtype_str(p)).collect();
            let head =
                format!("método {owner}{}({}) no es aplicable", table.symbol(m).name, sig.join(", "));
            Some(format!("{head}\n  ({})", candidate_reason(table, &params, varargs, args)))
        })
        .collect()
}

/// El **motivo** por el que un candidato no aplica a `args` (§15.12.2): distinta cantidad de
/// argumentos, o el primer argumento que no se convierte a su parámetro. Espeja lo que chequea
/// [`applicable`] (con *boxing*); si no encuentra un motivo simple —típicamente una inferencia
/// genérica insatisfacible— cae a un texto genérico.
fn candidate_reason(table: &SymbolTable, params: &[RType], varargs: bool, args: &[RType]) -> String {
    let arity_mismatch = if varargs { args.len() + 1 < params.len() } else { params.len() != args.len() };
    if arity_mismatch {
        return "las listas de argumentos difieren en longitud".to_string();
    }
    let fixed = if varargs { params.len().saturating_sub(1) } else { params.len() };
    for i in 0..fixed.min(args.len()) {
        if !convertible(table, &args[i], &params[i], true) {
            return format!(
                "los argumentos no coinciden: {} no se convierte a {}",
                table.rtype_str(&args[i]),
                table.rtype_str(&params[i])
            );
        }
    }
    if varargs {
        if let Some(RType::Array(elem)) = params.last() {
            for a in &args[fixed..] {
                if !convertible(table, a, elem, true) {
                    return format!(
                        "los argumentos no coinciden: {} no se convierte a {}",
                        table.rtype_str(a),
                        table.rtype_str(elem)
                    );
                }
            }
        }
    }
    "no se pueden inferir sus argumentos de tipo".to_string()
}

/// Cuántos slots ocupa un tipo en la frame: los de **categoría 2** (`long`/`double`) ocupan dos.
fn slot_width(ty: &RType) -> u16 {
    match ty {
        RType::Prim(PrimType::Long | PrimType::Double) => 2,
        _ => 1,
    }
}

// ---- sentencias ----

fn attrib_block(env: &mut Env, block: &mut Block) {
    env.push();
    for stmt in &mut block.0 {
        attrib_stmt(env, stmt);
    }
    env.pop();
}

fn attrib_stmt(env: &mut Env, stmt: &mut Stmt) {
    let pos = stmt.pos;
    // El match produce la decoración de la sentencia (solo las que declaran una variable la
    // tienen); se escribe al final, cuando ya no hay préstamo sobre `stmt.kind`.
    let local = match &mut stmt.kind {
        StmtKind::LocalVar { ty, name, init, .. } => {
            let declared = resolve_rtype(env.table, env.class_scope, ty);
            let is_var = matches!(ty, Type::Var);
            let target = match init {
                Some(e) => {
                    // El tipo declarado es el *target* del inicializador — salvo con `var`, donde
                    // no hay nada declarado todavía (§14.4.1: es al revés, el inicializador manda).
                    let expected = (!is_var).then(|| declared.clone());
                    let it = attrib_expr_to(env, e, expected.as_ref());
                    // §5.2: `byte b = 5;` — una constante `int` en rango narrowea a byte/short/char.
                    let ok = is_var
                        || assignable(env.table, &it, &declared)
                        || constant_narrowing_ok(&declared, e);
                    if !ok {
                        env.error(pos, format!("tipo incompatible en `{name}`"));
                    }
                    // `var`: el tipo del local es el del inicializador.
                    if is_var { it } else { declared }
                }
                None => declared,
            };
            let slot = env.define(name, target.clone());
            Some(LocalInfo { slot, ty: target })
        }
        StmtKind::Return(e) => {
            match (e, env.ret.clone()) {
                (None, RType::Void) => {}
                (None, _) => env.error(pos, "falta el valor de retorno".into()),
                (Some(e), RType::Void) => {
                    let p = e.pos;
                    attrib_expr(env, e);
                    env.error(p, "`return` con valor en un método `void`".into());
                }
                (Some(e), ret) => {
                    let p = e.pos;
                    let t = attrib_expr_to(env, e, Some(&ret)); // el retorno declarado es el target
                    if !assignable(env.table, &t, &ret) {
                        env.error(p, "tipo de retorno incompatible".into());
                    }
                }
            }
            None
        }
        StmtKind::Expr(e) => {
            attrib_expr(env, e);
            None
        }
        StmtKind::If { cond, then, els } => {
            require_boolean(env, cond, "if");
            attrib_stmt(env, then);
            if let Some(e) = els {
                attrib_stmt(env, e);
            }
            None
        }
        StmtKind::While { cond, body } => {
            require_boolean(env, cond, "while");
            attrib_stmt(env, body);
            None
        }
        StmtKind::Do { body, cond } => {
            attrib_stmt(env, body);
            require_boolean(env, cond, "do-while");
            None
        }
        StmtKind::For { init, cond, update, body } => {
            env.push();
            if let Some(i) = init {
                attrib_stmt(env, i);
            }
            if let Some(c) = cond {
                require_boolean(env, c, "for");
            }
            for u in update {
                attrib_expr(env, u);
            }
            attrib_stmt(env, body);
            env.pop();
            None
        }
        StmtKind::ForEach { ty, name, iterable, body, .. } => {
            env.push();
            attrib_expr(env, iterable);
            let rt = resolve_rtype(env.table, env.class_scope, ty);
            let slot = env.define(name, rt.clone());
            attrib_stmt(env, body);
            env.pop();
            Some(LocalInfo { slot, ty: rt })
        }
        StmtKind::Block(b) => {
            attrib_block(env, b);
            None
        }
        StmtKind::Synchronized { lock, body } => {
            attrib_expr(env, lock);
            attrib_block(env, body);
            None
        }
        StmtKind::Throw(e) => {
            attrib_expr(env, e);
            None
        }
        StmtKind::Switch { selector, cases } => {
            attrib_expr(env, selector);
            for c in cases {
                // El scope del `case` aloja sus **variables de patrón** (`case Integer i`), visibles
                // tanto en la guarda (`when i > 0`) como en el cuerpo.
                env.push();
                bind_patterns(env, &mut c.labels);
                if let Some(g) = &mut c.guard {
                    attrib_expr(env, g);
                }
                match &mut c.body {
                    SwitchBody::Arrow(s) => attrib_stmt(env, s),
                    SwitchBody::Colon(ss) => {
                        for s in ss {
                            attrib_stmt(env, s);
                        }
                    }
                }
                env.pop();
            }
            None
        }
        StmtKind::Yield(e) => {
            attrib_expr(env, e);
            None
        }
        StmtKind::Assert { cond, message } => {
            require_boolean(env, cond, "assert");
            if let Some(m) = message {
                attrib_expr(env, m);
            }
            None
        }
        StmtKind::Try { resources, body, catches, finally } => {
            env.push();
            for r in resources {
                attrib_stmt(env, r);
            }
            attrib_block(env, body);
            env.pop();
            for c in catches {
                env.push();
                let rt = c
                    .types
                    .first()
                    .map(|t| resolve_rtype(env.table, env.class_scope, t))
                    .unwrap_or(RType::Unresolved);
                // La variable del catch entra en su propio slot; se decora para la pasada Flow.
                c.slot = Some(env.define(&c.name, rt));
                attrib_block(env, &mut c.body);
                env.pop();
            }
            if let Some(f) = finally {
                attrib_block(env, f);
            }
            None
        }
        StmtKind::Labeled { body, .. } => {
            attrib_stmt(env, body);
            None
        }
        StmtKind::Break(_) | StmtKind::Continue(_) | StmtKind::Empty => None,
        // Una **clase local** (§14.3): `enter` ya la registró como tipo anidado del enclosing, así
        // que acá se **tipa su cuerpo in situ** — con los locales del método envolvente **visibles**,
        // para que un local capturado resuelva a `Binding::Local` y el desugar lo detecte (`val$x`).
        StmtKind::LocalClass(lc) => {
            attrib_local_class(env, lc);
            None
        }
    };
    stmt.local = local;
}

fn require_boolean(env: &mut Env, e: &mut Expr, ctx: &str) {
    let pos = e.pos;
    let t = attrib_expr(env, e);
    if !matches!(t, RType::Prim(PrimType::Boolean) | RType::Unresolved) {
        env.error(pos, format!("la condición de `{ctx}` debe ser boolean"));
    }
}

// ---- expresiones ----

/// Tipa una expresión y la **decora** (`ty` + `binding`), devolviendo su tipo.
/// Atribuye una expresión **sin** tipo esperado: el modo *synthesis* del chequeo bidireccional
/// («¿qué tipo tiene esto?»).
fn attrib_expr(env: &mut Env, expr: &mut Expr) -> RType {
    attrib_expr_to(env, expr, None)
}

/// El constructor de `cid` que aplica a esos argumentos. `None` si la clase no declara ninguno —
/// ahí rige el **implícito** `()V`, que no tiene símbolo y el emisor referencia directo.
fn ctor_binding(env: &mut Env, cid: SymbolId, args: &[RType], pos: Pos) -> Option<Binding> {
    let cands = constructors(env.table, cid);
    if cands.is_empty() {
        return None;
    }
    let name = env.table.symbol(cid).name.clone();
    resolve_overload(env, &cands, args, &name, pos, &RType::Class(cid)).map(Binding::Method)
}

/// **Fase 2** de la resolución de sobrecarga (§15.12.2.6): re-atribuye los argumentos **poly** de una
/// llamada con el tipo de su parámetro —ya sustituido por el receptor— como *target*, que en la fase 1
/// (síntesis, sin target) no estaba. Los no-poly no se tocan (evita re-resolver de más y re-emitir).
fn reattribute_poly_args(env: &mut Env, recv: &RType, m: SymbolId, args: &mut [Expr]) {
    let params: Vec<RType> = match env.table.resolved(m) {
        Some(Resolved::Method { params, .. }) => {
            params.iter().map(|p| substitute_member(env.table, recv, m, p)).collect()
        }
        _ => return,
    };
    for (i, a) in args.iter_mut().enumerate() {
        if is_poly_arg(&a.kind) {
            attrib_expr_to(env, a, params.get(i));
        }
    }
}

/// Los argumentos que son **llamadas a método genérico** (`pick(empty(), …)`): candidatos a resolverse
/// **junto** con la llamada externa (§18.5.2.1), en vez de aislada (que perdía su contexto). Devuelve
/// `(índice, método, receptor)` de cada uno.
fn nested_generic_calls(table: &SymbolTable, args: &[Expr]) -> Vec<infer::NestedCall> {
    let mut out = Vec::new();
    for (i, a) in args.iter().enumerate() {
        if !matches!(a.kind, ExprKind::Call { .. }) {
            continue;
        }
        if let Some(Binding::Method(f)) = a.binding {
            if !infer::method_type_params(table, f).is_empty() {
                out.push(infer::NestedCall { arg_index: i, method: f, recv: call_recv(table, a, f) });
            }
        }
    }
    out
}

/// El **receptor** de una llamada `call` a `f`, para fijar los parámetros de tipo de **clase** de su
/// retorno: el tipo del *target* si lo hay, o la clase dueña de `f` (estático / `this`).
fn call_recv(table: &SymbolTable, call: &Expr, f: SymbolId) -> RType {
    if let ExprKind::Call { target: Some(t), .. } = &call.kind {
        if let Some(ty) = &t.ty {
            return ty.clone();
        }
    }
    table.symbol(f).owner.map_or(RType::Unresolved, RType::Class)
}

/// Re-tipa cada **llamada anidada** con la sustitución que le tocó del solving combinado (§18.5.2.1):
/// su `ty` pasa de la que resolvió aislada (`Box<Object>`) a la correcta (`Box<String>`), para que el
/// contexto que la usa vea el tipo bien inferido.
fn retype_nested_calls(table: &SymbolTable, args: &mut [Expr], nres: &[infer::NestedResult]) {
    for nr in nres {
        let idx = nr.arg_index;
        let (f, ret_raw, recv) = match args.get(idx) {
            Some(a) => match a.binding {
                Some(Binding::Method(f)) => {
                    let ret = match table.resolved(f) {
                        Some(Resolved::Method { ret, .. }) => ret.clone(),
                        _ => continue,
                    };
                    (f, ret, call_recv(table, a, f))
                }
                _ => continue,
            },
            None => continue,
        };
        // Fijar los parámetros de **clase** por el receptor y los de **método** por lo inferido.
        let ret = types::substitute(&substitute_member(table, &recv, f, &ret_raw), &nr.subst);
        if let Some(a) = args.get_mut(idx) {
            a.ty = Some(ret);
        }
    }
}

/// ¿Es `kind` un argumento **poly** que sin *target* queda roto o sin instanciar? — una **lambda** o
/// **referencia a método** (sin target no tipan su cuerpo/SAM: quedaban `Unresolved` y el emisor no
/// podía bajarlas) o un **diamante** `new C<>()` (sin target no infiere sus argumentos de tipo). Una
/// llamada a método genérico se deja: tipa leniente en la fase 1 y la *erasure* la hace invariante.
fn is_poly_arg(kind: &ExprKind) -> bool {
    match kind {
        ExprKind::Lambda { .. } | ExprKind::MethodRef { .. } => true,
        ExprKind::NewObject { ty: Type::Parameterized { args, .. }, .. } => args.is_empty(),
        _ => false,
    }
}

/// Atribuye una expresión con un ***target type*** opcional: el modo *checking* («¿puede esto ser
/// un `T`?»). Solo lo consumen las expresiones **poly** —una llamada a un método genérico y el
/// diamante—, que sin él dejarían variables de tipo sin instanciar; el resto lo ignora.
///
/// El target **no** se propaga hacia adentro salvo por el ternario, cuyas dos ramas están en el
/// mismo contexto que el todo (§15.25).
fn attrib_expr_to(env: &mut Env, expr: &mut Expr, target: Option<&RType>) -> RType {
    let pos = expr.pos;
    // `super(...)` / `this(...)`: la **invocación explícita de constructor** (§8.8.7.1). El parser
    // la codifica como una llamada cuyo nombre es la keyword — no puede chocar con un método real,
    // porque `super` y `this` no son identificadores válidos. Se resuelve antes del match general
    // porque no busca un método: busca un **constructor**, y en otra clase.
    if let ExprKind::Call { target: None, name, args, .. } = &mut expr.kind {
        if name == "super" || name == "this" {
            let of_super = name == "super";
            let arg_types: Vec<RType> = args.iter_mut().map(|a| attrib_expr(env, a)).collect();
            let owner = if of_super { env.table.super_class(env.class) } else { Some(env.class) };
            let binding = owner.and_then(|cid| ctor_binding(env, cid, &arg_types, pos));
            expr.ty = Some(RType::Void);
            expr.binding = binding;
            return RType::Void;
        }
    }
    // Un `Indy` ya **bajado** por el desugar (LambdaToMethod, §15.27): su `ty` —la interfaz
    // funcional— ya lo fijó aquella pasada y no se recomputa. Lo único a re-atribuir son las
    // **capturas**: nombres del método envolvente que, tras la re-atribución, toman su slot en el
    // frame nuevo (el emisor los empuja antes del `invokedynamic`).
    if matches!(expr.kind, ExprKind::Indy { .. }) {
        let ty = expr.ty.clone().unwrap_or(RType::Unresolved);
        if let ExprKind::Indy { captures, .. } = &mut expr.kind {
            captures.iter_mut().for_each(|c| {
                attrib_expr(env, c);
            });
        }
        return ty;
    }
    // El match calcula (tipo, binding); se escriben al final, cuando ya no hay préstamo sobre
    // `expr.kind`.
    let (ty, binding) = match &mut expr.kind {
        // Un nodo de error (recuperación del parser): ya se reportó el error de sintaxis, así que
        // **no** se emite uno nuevo; queda `Unresolved` para no encadenar diagnósticos derivados.
        ExprKind::Error => (RType::Unresolved, None),
        ExprKind::IntLit(_) => (RType::Prim(PrimType::Int), None),
        ExprKind::LongLit(_) => (RType::Prim(PrimType::Long), None),
        ExprKind::FloatLit(_) => (RType::Prim(PrimType::Float), None),
        ExprKind::DoubleLit(_) => (RType::Prim(PrimType::Double), None),
        ExprKind::CharLit(_) => (RType::Prim(PrimType::Char), None),
        ExprKind::BoolLit(_) => (RType::Prim(PrimType::Boolean), None),
        ExprKind::StringLit(_) => (class_rtype(env.table, "String"), None),
        ExprKind::Null => (RType::Unresolved, None),
        ExprKind::This => {
            // `this` vive en el slot 0 de la frame (→ `aload_0`), y **no existe** en un contexto
            // estático (§8.4.3.2): un método/inicializador `static` no tiene instancia.
            if !env.has_this {
                env.error(pos, "`this` no se puede usar en un contexto estático".into());
            }
            let b = if env.has_this { Some(Binding::Local { slot: 0 }) } else { None };
            (RType::Class(env.class), b)
        }
        ExprKind::Super => {
            if !env.has_this {
                env.error(pos, "`super` no se puede usar en un contexto estático".into());
            }
            let t = match env.table.super_class(env.class) {
                Some(s) => RType::Class(s),
                None => RType::Unresolved,
            };
            let b = if env.has_this { Some(Binding::Local { slot: 0 }) } else { None };
            (t, b)
        }
        ExprKind::QualifiedThis(ty) => {
            // `Outer.this` (§15.8.4): la instancia envolvente de tipo `Outer`. Se tipa como esa
            // clase para que el acceso a miembros a través de ella (`Outer.this.f`) resuelva; el
            // desugar la reescribe a la cadena de `this$0`.
            if !env.has_this {
                env.error(pos, "`Clase.this` no se puede usar en un contexto estático".into());
            }
            (resolve_rtype(env.table, env.class_scope, ty), None)
        }
        ExprKind::Name(name) => {
            let name = name.clone();
            resolve_name(env, &name, pos)
        }
        ExprKind::Binary { op, lhs, rhs } => {
            let op = *op;
            let l = attrib_expr(env, lhs);
            let r = attrib_expr(env, rhs);
            (binary_type(env, op, &l, &r, pos), None)
        }
        ExprKind::Unary { op, expr, .. } => {
            let op = *op;
            let t = attrib_expr(env, expr);
            (unary_type(op, &t), None)
        }
        ExprKind::Assign { op, target: lhs, value } => {
            let lt = attrib_expr(env, lhs);
            // Reasignar un campo **`final`** solo vale en un constructor o inicializador (§8.3.1.2):
            // en un método normal el *blank final* ya recibió su valor y no se puede tocar. (La
            // regla fina de «exactamente una vez» es de análisis de flujo; esto corta el caso claro.)
            if !env.final_field_ok {
                if let Some(Binding::Field(f)) = lhs.binding {
                    if env.table.symbol(f).modifiers.contains(&Modifier::Final) {
                        let name = env.table.symbol(f).name.clone();
                        env.error(pos, format!("no se puede asignar el campo `final` `{name}`"));
                    }
                }
            }
            // El tipo del destino es el *target* del valor: `Caja<String> c; c = fabricar();`.
            let vt = attrib_expr_to(env, value, Some(&lt));
            // Una asignación **compuesta** (`+=`) lleva un cast implícito al tipo del destino
            // (§15.26.2: `E1 op= E2` es `E1 = (T)(E1 op E2)`), así que no exige asignabilidad
            // directa; una simple (`=`) sí — con el narrowing de constante del §5.2 (`b = 5`).
            if *op == AssignOp::Assign
                && !assignable(env.table, &vt, &lt)
                && !constant_narrowing_ok(&lt, value)
            {
                env.error(pos, "asignación de tipo incompatible".into());
            }
            (lt, None)
        }
        // Una **lambda** es una *poly expression* (§15.27.3): su tipo es la *functional interface*
        // que le da el contexto. Con ese target se **tipa el cuerpo** — se ligan los parámetros a
        // los tipos del SAM y se atribuye el cuerpo, chequeando que su resultado sea compatible con
        // el retorno del SAM. Sin un target funcional no hay contra qué tiparla: queda `Unresolved`
        // (indulgente, para no falso-rechazar cuando el target no llegó). La **emisión** con
        // `invokedynamic` es la Etapa 2; hasta entonces la corta la barrera del emisor.
        ExprKind::Lambda { params, body } => {
            match target.and_then(|t| functional_sam(env.table, types::erased_id(t)?).map(|s| (t, s))) {
                Some((t, sam)) => {
                    let t = t.clone();
                    let subst = types::subst_of(env.table, &t);
                    let (sam_params, sam_ret) = match env.table.resolved(sam) {
                        Some(Resolved::Method { params: ps, ret, .. }) => (
                            ps.iter().map(|p| types::substitute(p, &subst)).collect::<Vec<_>>(),
                            types::substitute(ret, &subst),
                        ),
                        _ => (Vec::new(), RType::Unresolved),
                    };
                    if params.len() != sam_params.len() {
                        env.error(pos, format!(
                            "la lambda tiene {} parámetro(s) pero el método `{}` espera {}",
                            params.len(),
                            env.table.symbol(sam).name,
                            sam_params.len(),
                        ));
                    }
                    // El cuerpo captura los locales del método que la construye: se atribuye en el
                    // **mismo** env, con un scope nuevo para los parámetros de la lambda.
                    env.push();
                    for (p, pt) in params.iter().zip(&sam_params) {
                        // Inferido (`x ->`, `Type::Var`) ⇒ el tipo del SAM; explícito ⇒ el declarado.
                        let ty = if matches!(p.ty, Type::Var) {
                            pt.clone()
                        } else {
                            resolve_rtype(env.table, env.class_scope, &p.ty)
                        };
                        env.define(&p.name, ty);
                    }
                    match body.as_mut() {
                        LambdaBody::Expr(e) => {
                            let bt = attrib_expr_to(env, e, Some(&sam_ret));
                            // Un SAM `void` acepta cualquier expresión (se descarta); si no, el
                            // resultado tiene que ser **convertible** al retorno — contexto de
                            // asignación, con boxing/unboxing (`int` ⇒ `Integer`, §5.2).
                            if !matches!(sam_ret, RType::Void | RType::Unresolved)
                                && !convertible(env.table, &bt, &sam_ret, true)
                            {
                                env.error(e.pos, "el cuerpo de la lambda no es compatible con el retorno de la interfaz".into());
                            }
                        }
                        LambdaBody::Block(b) => {
                            // Un `return` del bloque se chequea contra el retorno del **SAM**, no del
                            // método envolvente: se intercambia `env.ret` mientras dura el cuerpo.
                            let saved = std::mem::replace(&mut env.ret, sam_ret.clone());
                            attrib_block(env, b);
                            env.ret = saved;
                        }
                    }
                    env.pop();
                    (t, None)
                }
                None => (RType::Unresolved, None),
            }
        }
        // Una **referencia a método** (§15.13) es una *poly expression*: su tipo es la *functional
        // interface* del contexto. Se atribuye el *qualifier* (para decorar sus nodos) y, con un
        // target funcional, se verifica que el método/constructor **exista** en él —la resolución
        // fina del §15.13.1 (bound/unbound/static, y la compatibilidad exacta con el SAM) queda
        // acotada—; el tipo de la referencia es esa interfaz.
        ExprKind::MethodRef { qualifier, name, .. } => {
            let name = name.clone();
            let qual_type = match qualifier.as_mut() {
                MethodRefQualifier::Expr(e) => attrib_expr(env, e),
                MethodRefQualifier::Type(t) => resolve_rtype(env.table, env.class_scope, t),
            };
            let functional =
                target.filter(|t| types::erased_id(t).is_some_and(|c| functional_sam(env.table, c).is_some()));
            match functional {
                Some(t) => {
                    // `new` referencia un constructor; el resto, un método por nombre. Se reporta
                    // solo si **falta con seguridad** (jerarquía completa) — igual que las llamadas.
                    if name != "new" {
                        if let Some(c) = types::erased_id(&qual_type) {
                            if candidates(env.table, c, &name).is_empty()
                                && hierarchy_complete(env.table, c)
                            {
                                let mut notes = symbol_notes(
                                    format!("método {name}"),
                                    format!("clase {}", env.table.rtype_str(&qual_type)),
                                );
                                let names = member_names(env.table, c, true);
                                if let Some(s) = did_you_mean(&name, names.iter().map(|s| s.as_str())) {
                                    notes.push(s);
                                }
                                env.error_with_notes(
                                    pos,
                                    format!("no se encuentra el método: {name}"),
                                    notes,
                                );
                            }
                        }
                    }
                    (t.clone(), None)
                }
                None => (RType::Unresolved, None),
            }
        }
        ExprKind::Ternary { cond, then, els } => {
            require_boolean(env, cond, "?:");
            // Las dos ramas están en el **mismo** contexto que el ternario entero (§15.25).
            let a = attrib_expr_to(env, then, target);
            let b = attrib_expr_to(env, els, target);
            // El tipo del ternario es el **lub** de sus ramas (§15.25). Con primitivos manda la
            // promoción numérica; con referencias, el supertipo común.
            let t = if matches!(a, RType::Prim(_)) && matches!(b, RType::Prim(_)) {
                numeric_result(env, &a, &b, pos)
            } else {
                types::lub(env.table, &[a, b])
            };
            (t, None)
        }
        // `receiver` es el objeto sobre el que se llama; no confundirlo con `target`, que acá es
        // el **tipo esperado** del resultado.
        ExprKind::Call { target: receiver, name, args, type_args } => {
            let name = name.clone();
            let type_args = type_args.clone();
            // Los tipos de los argumentos son la entrada del overload resolution. Se les aplica
            // **capture conversion** (§5.1.10): un argumento `List<?>` se convierte a `List<CAP>`,
            // lo que permite **inferir** `T` al pasarlo a un `<T> m(List<T>)` (el caso `swap`).
            let arg_types: Vec<RType> = args
                .iter_mut()
                .map(|a| {
                    let t = attrib_expr(env, a);
                    types::capture(env.table, &t)
                })
                .collect();
            let recv = match receiver {
                Some(t) => attrib_expr(env, t),
                // Sin receptor: el método puede ser de esta clase, heredado, o de una clase
                // **envolvente** (inner class). Se busca por la cadena de `owner`; el desugar
                // reescribe el acceso a un método envolvente como `this$0.m()`.
                None => {
                    let mut found = env.class;
                    let mut c = Some(env.class);
                    while let Some(cl) = c {
                        if !candidates(env.table, cl, &name).is_empty() {
                            found = cl;
                            break;
                        }
                        c = enclosing_class(env.table, cl);
                    }
                    RType::Class(found)
                }
            };
            // **Capture conversion** del receptor (§5.1.10), **una sola vez**: así todas las
            // sustituciones de esta llamada (parámetros y retorno) comparten las **mismas** variables
            // de captura. `capture` preserva la *erasure*, así que la búsqueda de candidatos sigue
            // igual. Un receptor sin wildcards se devuelve tal cual.
            let recv = types::capture(env.table, &recv);
            // El receptor puede ser crudo (`C`) o parametrizado (`List<String>`): se busca sobre
            // su erasure, y después se **sustituyen** los argumentos en la firma.
            match types::erased_id(&recv) {
                Some(c) => {
                    let cands = candidates(env.table, c, &name);
                    // Indulgencia con externos, **acotada**: solo si la jerarquía del receptor está
                    // **incompleta** (algún supertipo no cargó del classpath), un nombre no hallado
                    // podría estar heredado de esa parte que falta. Con la jerarquía completa —ahora
                    // que se cargan los supertipos transitivamente— un miss es genuino y se reporta:
                    // `"x".noExiste()` ya no pasa. La indulgencia por **sobrecarga que no matchea**
                    // (abajo) sí se mantiene sobre externos: nuestra resolución no cubre toda firma.
                    let complete = hierarchy_complete(env.table, c);
                    if cands.is_empty() {
                        if complete {
                            let sig: Vec<String> =
                                arg_types.iter().map(|a| env.table.rtype_str(a)).collect();
                            let mut notes = symbol_notes(
                                format!("método {name}({})", sig.join(", ")),
                                format!("clase {}", env.table.rtype_str(&recv)),
                            );
                            let names = member_names(env.table, c, true);
                            if let Some(s) = did_you_mean(&name, names.iter().map(|s| s.as_str())) {
                                notes.push(s);
                            }
                            env.error_with_notes(
                                pos,
                                format!("no se encuentra el método: {name}"),
                                notes,
                            );
                        }
                        (RType::Unresolved, None)
                    } else {
                        // **Desambiguación por el argumento poly** (§15.12.2): un arg lambda se tipa
                        // `Unresolved` (indulgente), así que sin esto sería aplicable a **toda**
                        // sobrecarga con un parámetro funcional. Se descartan las incompatibles con la
                        // **forma** de la lambda (aridad del SAM, y valor-vs-`void`): `f(x -> x + 1)`
                        // elige `Function` sobre `Consumer`.
                        let cands = disambiguate_by_lambda(env.table, &recv, &cands, args);
                        match resolve_overload(env, &cands, &arg_types, &name, pos, &recv) {
                            Some(m) => {
                                // **Fase 2** (§15.12.2.6 / §18.5.2): resuelta la sobrecarga se conocen
                                // los tipos de los parámetros, y recién ahora se re-atribuye cada
                                // argumento **poly** —una lambda, un *method ref*, un diamante o una
                                // llamada a genérico— con el del suyo como *target*. Es lo único que
                                // tipa el cuerpo de una **lambda-argumento** (sin él quedaba
                                // `Unresolved` y el emisor no podía bajarla) e instancia las variables
                                // de un diamante/genérico argumento. La fase 1 usó el receptor para
                                // resolver; el *target* de cada parámetro sale de sustituirlo con él.
                                reattribute_poly_args(env, &recv, m, args);
                                let ret = match env.table.resolved(m) {
                                    Some(Resolved::Method { ret, .. }) => ret.clone(),
                                    _ => RType::Unresolved,
                                };
                                // Dos sustituciones, en orden: la que impone el **receptor**
                                // (`List<String>.get` devuelve `String`, no `E`) y después la
                                // **inferida** para los params de tipo del propio método
                                // (`id("hola")` ⇒ `T = String`, JLS §18.5.2).
                                let ret = substitute_member(env.table, &recv, m, &ret);
                                // Un **type witness** (`this.<Integer>id(x)`) **fija** los parámetros
                                // de tipo del método; sin él, se infieren (§18.5.2). Aplicar el
                                // witness cierra el hueco de que un override deliberado se ignorara.
                                let subst = if type_args.is_empty() {
                                    // §18.5.2.1 — **solving combinado**: las llamadas genéricas anidadas
                                    // en los argumentos (`pick(empty(), …)`) se resuelven **junto** con
                                    // esta, así `empty()` toma su `E` del contexto en vez de dar
                                    // `Box<Object>` aislada.
                                    let nested = nested_generic_calls(env.table, args);
                                    let (s, nres, ok) = infer::infer_call_nested(
                                        env.table, &recv, m, &arg_types, &nested, target,
                                    );
                                    // §18.5.1: constraints **insatisfacibles** (igualdades incompatibles,
                                    // o argumento fuera de la cota declarada) ⇒ la inferencia **falla**.
                                    // Se reporta solo en métodos **propios** (con un externo se mantiene la
                                    // indulgencia: no modelamos toda firma del JDK).
                                    if !ok
                                        && env.table.symbol(m).owner.is_some_and(|o| !is_external(env.table, o))
                                    {
                                        env.error(
                                            pos,
                                            format!(
                                                "no se pueden inferir los argumentos de tipo de `{name}`: \
                                                 restricciones de tipo incompatibles"
                                            ),
                                        );
                                    }
                                    // Re-tipar cada llamada anidada con su parte de la solución combinada.
                                    retype_nested_calls(env.table, args, &nres);
                                    s
                                } else {
                                    witness_subst(env, m, &type_args)
                                };
                                let ret = if subst.is_empty() {
                                    ret
                                } else {
                                    types::substitute(&ret, &subst)
                                };
                                (ret, Some(Binding::Method(m)))
                            }
                            None => {
                                // El nombre existe pero ninguna sobrecarga aplicó. Sobre un tipo
                                // **externo** se sigue siendo indulgente: nuestra resolución no
                                // modela toda firma del JDK (genéricos, varargs), así que un no-match
                                // puede ser una limitación nuestra, no un error del fuente.
                                if !is_external(env.table, c) {
                                    let sig: Vec<String> =
                                        arg_types.iter().map(|a| env.table.rtype_str(a)).collect();
                                    let notes =
                                        overload_candidate_notes(env.table, &cands, &arg_types, &recv);
                                    env.error_with_notes(
                                        pos,
                                        format!(
                                            "no se encontró un método `{name}({})` aplicable",
                                            sig.join(", ")
                                        ),
                                        notes,
                                    );
                                }
                                (RType::Unresolved, None)
                            }
                        }
                    }
                }
                None => (RType::Unresolved, None),
            }
        }
        // Ojo con el nombre: `receiver` es el objeto del que se lee, no la expresión entera — que
        // en este arm queda **tapada** por el binding del patrón.
        ExprKind::Field { expr: receiver, name } => {
            let name = name.clone();
            // **Capture conversion** (§5.1.10): un acceso a campo sobre `Pair<? extends N>` ve el
            // componente por su variable de captura (usable como `N`). Preserva la *erasure*.
            let recv = types::capture(env.table, &attrib_expr(env, receiver));
            // `Outer.Inner` — acceso a un **tipo anidado** cualificado: si el receptor es un nombre
            // de tipo y `name` nombra un tipo anidado suyo, esto es una **referencia de tipo**, no un
            // acceso a campo. Sin este caso, `Outer.Inner.v()` fallaba buscando un campo `Inner`.
            let nested = match receiver.binding {
                Some(Binding::Class(owner)) => nested_type(env.table, owner, &name),
                _ => None,
            };
            if let Some(nid) = nested {
                (RType::Class(nid), Some(Binding::Class(nid)))
            } else if name == "length" && matches!(recv, RType::Array(_)) {
                // `a.length` no es un campo: es una forma propia del lenguaje (§10.7), y un array no
                // tiene scope de miembros donde buscarla. Sin este caso quedaba `Unresolved`, que la
                // **categoría** del emisor lee como referencia — y `i < a.length` salía `if_acmpne`.
                (RType::Prim(PrimType::Int), None)
            } else {
                match types::erased_id(&recv) {
                    Some(c) => match lookup_field(env.table, c, &name) {
                        Some(f) => {
                            let t = match env.table.resolved(f) {
                                Some(Resolved::Field(t)) => t.clone(),
                                _ => RType::Unresolved,
                            };
                            let t = substitute_member(env.table, &recv, f, &t);
                            (t, Some(Binding::Field(f)))
                        }
                        // Ídem que en las llamadas: indulgente solo si la jerarquía está incompleta.
                        None if !hierarchy_complete(env.table, c) => (RType::Unresolved, None),
                        None => {
                            let mut notes = symbol_notes(
                                format!("variable {name}"),
                                format!("clase {}", env.table.rtype_str(&recv)),
                            );
                            let names = member_names(env.table, c, false);
                            if let Some(s) = did_you_mean(&name, names.iter().map(|s| s.as_str())) {
                                notes.push(s);
                            }
                            env.error_with_notes(
                                pos,
                                format!("no se encuentra el campo: {name}"),
                                notes,
                            );
                            (RType::Unresolved, None)
                        }
                    },
                    None => (RType::Unresolved, None),
                }
            }
        }
        ExprKind::Index { array, index } => {
            let a = attrib_expr(env, array);
            let ipos = index.pos;
            let i = attrib_expr(env, index);
            if !is_integral(&i) && !matches!(i, RType::Unresolved) {
                env.error(ipos, "el índice de un array debe ser entero".into());
            }
            match a {
                RType::Array(elem) => (*elem, None),
                _ => (RType::Unresolved, None),
            }
        }
        ExprKind::Cast { ty, expr } => {
            attrib_expr(env, expr);
            (resolve_rtype(env.table, env.class_scope, ty), None)
        }
        ExprKind::InstanceOf { expr, ty, binding, slot } => {
            attrib_expr(env, expr);
            // `e instanceof T v` declara `v` (§14.30.2). El alcance real es "donde la condición es
            // verdadera"; acá se declara en el ámbito en curso — más amplio, pero suficiente.
            if let Some(n) = binding {
                let rt = resolve_rtype(env.table, env.class_scope, ty);
                let n = n.clone();
                *slot = Some(env.define(&n, rt));
            }
            (RType::Prim(PrimType::Boolean), None)
        }
        // `C.class` vale un `java.lang.Class` (se ignora el argumento genérico `<C>`, que la
        // *erasure* borra igual).
        ExprKind::ClassLit(_) => {
            (resolve_rtype(env.table, env.class_scope, &Type::Class("Class".into())), None)
        }
        // `body` (una **clase anónima**) no se atribuye acá: sus miembros son un tipo aparte, y
        // entrarlos/tiparlos es de la fase de tipos —igual que la lambda—. Los **argumentos** sí se
        // evalúan (van al `super(...)` de la anónima) y el tipo del `new` es el `ty` extendido.
        ExprKind::NewObject { ty, args, body: _, outer } => {
            // `outer.new Inner(...)` (§15.9.2): el **calificador** designa la instancia envolvente
            // que se pasa como `this$0`. Se atribuye para tiparlo (el desugar lo empuja como primer
            // argumento del ctor); `Inner` se resuelve igual que en un `new` normal.
            if let Some(o) = outer {
                attrib_expr(env, o);
            }
            let arg_types: Vec<RType> = args.iter_mut().map(|a| attrib_expr(env, a)).collect();
            let rt = resolve_rtype(env.table, env.class_scope, ty);
            // Se decora con el **constructor** resuelto: el codegen necesita su descriptor para
            // emitir el `invokespecial <init>`. Sin candidatos (tipo externo) queda en `None` y el
            // codegen cae al `()V`.
            let binding = match &rt {
                RType::Class(cid) | RType::Parameterized { base: cid, .. } => {
                    let cands = constructors(env.table, *cid);
                    let cname = env.table.symbol(*cid).name.clone();
                    if cands.is_empty() {
                        None
                    } else {
                        resolve_overload(env, &cands, &arg_types, &cname, pos, &rt).map(Binding::Method)
                    }
                }
                _ => None,
            };
            // El **diamante** (`new Caja<>()`) llega acá como un parametrizado con **cero**
            // argumentos: los que faltan se infieren del target y del constructor (§15.9.3). Sin
            // esto quedaría un tipo con la lista vacía, que se compara con cualquier cosa sin
            // comparar nada — pasaba por indulgente, no por correcto.
            let rt = match &rt {
                RType::Parameterized { base, args } if args.is_empty() => match target {
                    // Con **target** (init/`return`/`=`, o la **fase 2** de un argumento) el diamante
                    // infiere sus argumentos de él y del constructor (§15.9.3).
                    Some(_) => {
                        let ctor = match binding {
                            Some(Binding::Method(m)) => Some(m),
                            _ => None,
                        };
                        let subst = infer::infer_diamond(env.table, *base, ctor, &arg_types, target);
                        let vars = types::type_params_of(env.table, *base);
                        if subst.is_empty() {
                            rt.clone()
                        } else {
                            let args = vars
                                .iter()
                                .map(|v| {
                                    RTypeArg::Type(subst.get(v).cloned().unwrap_or(RType::Unresolved))
                                })
                                .collect();
                            RType::Parameterized { base: *base, args }
                        }
                    }
                    // Sin target se deja **crudo** (`Box<>`): la aplicabilidad lo acepta unchecked
                    // (§4.8), y como argumento la fase 2 lo re-atribuye ya con el target del parámetro.
                    None => rt.clone(),
                },
                _ => rt.clone(),
            };
            // **Fase 2** del constructor (§15.12.2.6 / §18.5.2): re-atribuir los argumentos **poly**
            // (lambda, *method ref*, diamante) con el tipo de su parámetro —ya sustituido por los
            // argumentos de tipo de la instancia (`Box<long[]>` ⇒ `Supplier<A>` → `Supplier<long[]>`)—
            // como *target*. Sin esto, una lambda argumento de un constructor genérico
            // (`new Box<long[]>(() -> …)`) quedaba `Unresolved` y el emisor no podía bajarla a
            // `invokedynamic` (finding #16). Es el espejo de [`reattribute_poly_args`] de una llamada.
            if let Some(Binding::Method(ctor)) = &binding {
                reattribute_poly_args(env, &rt, *ctor, args);
            }
            (rt, binding)
        }
        ExprKind::NewArray { elem, dims, init } => {
            for d in dims.iter_mut().flatten() {
                attrib_expr(env, d);
            }
            if let Some(es) = init {
                for e in es {
                    attrib_expr(env, e);
                }
            }
            (RType::Array(Box::new(resolve_rtype(env.table, env.class_scope, elem))), None)
        }
        ExprKind::Switch { selector, cases } => {
            attrib_expr(env, selector);
            // El tipo de una switch expression es el **lub** de los valores de sus brazos (§15.28).
            let mut arms: Vec<RType> = Vec::new();
            for c in cases {
                env.push(); // scope del `case`: sus variables de patrón
                bind_patterns(env, &mut c.labels);
                if let Some(g) = &mut c.guard {
                    attrib_expr(env, g);
                }
                match &mut c.body {
                    SwitchBody::Arrow(s) => {
                        if let StmtKind::Expr(e) = &mut s.kind {
                            arms.push(attrib_expr(env, e));
                        } else {
                            attrib_stmt(env, s);
                        }
                    }
                    SwitchBody::Colon(ss) => {
                        for s in ss {
                            attrib_stmt(env, s);
                        }
                    }
                }
                env.pop();
            }
            // Los `yield` de los brazos con bloque todavía no aportan su tipo (cola larga).
            let t = if arms.iter().all(|a| matches!(a, RType::Prim(_))) && !arms.is_empty() {
                arms.iter().skip(1).fold(arms[0].clone(), |acc, b| numeric_result(env, &acc, b, pos))
            } else {
                types::lub(env.table, &arms)
            };
            (t, None)
        }
        // `Indy` se atiende **antes** de este match (early return): acá es inalcanzable.
        ExprKind::Indy { .. } => unreachable!("Indy se re-atribuye antes del match general"),
    };
    expr.ty = Some(ty.clone());
    expr.binding = binding;
    ty
}

/// Resuelve un `Name`: local/param → campo (por la jerarquía) → nombre de tipo → error.
/// El **tipo anidado** `name` declarado en `owner` (§8.5), o `None`. Busca un símbolo de clase con
/// ese nombre en el scope de miembros del dueño.
fn nested_type(table: &SymbolTable, owner: SymbolId, name: &str) -> Option<SymbolId> {
    let scope = member_scope(table, owner);
    table
        .scope(scope)
        .get(name)
        .iter()
        .copied()
        .find(|&id| matches!(table.symbol(id).kind, SymbolKind::Class { .. }))
}

fn resolve_name(env: &mut Env, name: &str, pos: Pos) -> (RType, Option<Binding>) {
    if let Some((ty, slot)) = env.lookup_local(name) {
        return (ty, Some(Binding::Local { slot }));
    }
    if let Some(f) = lookup_field(env.table, env.class, name) {
        if let Some(Resolved::Field(t)) = env.table.resolved(f) {
            return (t.clone(), Some(Binding::Field(f)));
        }
    }
    // Un campo de instancia de una clase **envolvente** (inner class): se busca por la cadena de
    // `owner`. El desugar reescribirá el acceso como `this$0.campo`.
    let mut encl = enclosing_class(env.table, env.class);
    while let Some(o) = encl {
        if let Some(f) = lookup_field(env.table, o, name) {
            if let Some(Resolved::Field(t)) = env.table.resolved(f) {
                return (t.clone(), Some(Binding::Field(f)));
            }
        }
        encl = enclosing_class(env.table, o);
    }
    // ¿Un nombre de tipo (para un acceso estático `Tipo.x`)? Lo damos como la clase.
    if let Some(id) = env.table.resolve_type(env.class_scope, name) {
        return (RType::Class(id), Some(Binding::Class(id)));
    }
    if let Some(id) = env.table.external(name) {
        return (RType::Class(id), Some(Binding::Class(id)));
    }
    let mut notes = symbol_notes(
        format!("variable {name}"),
        format!("clase {}", env.table.symbol(env.class).name),
    );
    // did-you-mean sobre lo que sí está en scope: los **locales** visibles y los **campos** de la clase.
    let mut names: Vec<String> = env.locals.iter().flat_map(|s| s.vars.keys().cloned()).collect();
    names.extend(member_names(env.table, env.class, false));
    if let Some(s) = did_you_mean(name, names.iter().map(|s| s.as_str())) {
        notes.push(s);
    }
    env.error_with_notes(pos, format!("no se encuentra el símbolo: {name}"), notes);
    (RType::Unresolved, None)
}

// ---- reglas de tipo ----

fn binary_type(env: &mut Env, op: BinOp, l: &RType, r: &RType, pos: Pos) -> RType {
    use BinOp::*;
    match op {
        Add if is_string(env.table, l) || is_string(env.table, r) => class_rtype(env.table, "String"),
        Add | Sub | Mul | Div | Rem => numeric_result(env, l, r, pos),
        Lt | Gt | Le | Ge => {
            numeric_result(env, l, r, pos);
            RType::Prim(PrimType::Boolean)
        }
        Eq | Ne => RType::Prim(PrimType::Boolean),
        And | Or => {
            require_bool_operand(env, l, pos);
            require_bool_operand(env, r, pos);
            RType::Prim(PrimType::Boolean)
        }
        BitAnd | BitOr | BitXor => {
            if is_bool(l) && is_bool(r) {
                RType::Prim(PrimType::Boolean)
            } else {
                numeric_result(env, l, r, pos)
            }
        }
        Shl | Shr | UShr => promote_unary(l),
    }
}

fn numeric_result(env: &mut Env, l: &RType, r: &RType, pos: Pos) -> RType {
    // **Unboxing** (§5.1.8): un envoltorio numérico (`Integer`, `Double`…) se desempaqueta a su
    // primitivo antes de la promoción binaria. Sin esto, `x + 1` con `x` de tipo `Integer` —el caso
    // típico de una lambda `Function<Integer,Integer>`— fallaba por "operando no numérico".
    let l = &unbox(env.table, l);
    let r = &unbox(env.table, r);
    if lenient(l) || lenient(r) {
        return if lenient(l) { promote_unary(r) } else { promote_unary(l) };
    }
    if !is_numeric(l) || !is_numeric(r) {
        env.error(pos, "operando no numérico".into());
        return RType::Unresolved;
    }
    let (a, b) = (prim_rank(l), prim_rank(r));
    rank_to_rtype(a.max(b).max(3)) // el mínimo de la promoción binaria es `int`
}

/// Desempaqueta un envoltorio numérico a su primitivo (`Integer` → `int`, §5.1.8); cualquier otro
/// tipo queda igual.
fn unbox(table: &SymbolTable, t: &RType) -> RType {
    if let Some(id) = types::erased_id(t) {
        if let Some(p) = types::unboxed(table, id) {
            return RType::Prim(p);
        }
    }
    t.clone()
}

fn unary_type(op: UnOp, t: &RType) -> RType {
    match op {
        UnOp::Not => RType::Prim(PrimType::Boolean),
        UnOp::Plus | UnOp::Neg => promote_unary(t),
        UnOp::BitNot | UnOp::Inc | UnOp::Dec => t.clone(),
    }
}

fn promote_unary(t: &RType) -> RType {
    match t {
        RType::Prim(p) if prim_rank_of(*p) < 3 => RType::Prim(PrimType::Int),
        _ => t.clone(),
    }
}

fn require_bool_operand(env: &mut Env, t: &RType, pos: Pos) {
    if !is_bool(t) && !lenient(t) {
        env.error(pos, "se esperaba un operando boolean".into());
    }
}

// ---- asignabilidad y subtipado ----

/// **Narrowing de una constante** (§5.2): una expresión constante de tipo `int` es asignable a
/// `byte`/`short`/`char` **si su valor entra** en el rango del destino. Hace legal `byte b = 5;`,
/// `static final short MAX = 32767;` y también `byte b = 1 + 2;` — sin cast. El valor lo pliega el
/// evaluador §15.29 de `codegen` (literales, aritmética, bits y shift), restringido a las constantes
/// de tipo **`int`** (una `long`/`float`/`double` sí necesita cast explícito).
fn constant_narrowing_ok(to: &RType, init: &Expr) -> bool {
    let (lo, hi): (i32, i32) = match to {
        RType::Prim(PrimType::Byte) => (-128, 127),
        RType::Prim(PrimType::Short) => (-32768, 32767),
        RType::Prim(PrimType::Char) => (0, 65535),
        _ => return false,
    };
    // El atributado corre **antes** de plegar los campos constantes de la unidad, así que las
    // referencias a otra `final` no se resuelven aquí (mapa vacío): un `byte b = A;` con `A` constante
    // es un caso diferido. Sí pliega literales y aritmética entre literales (`byte b = 1 + 2;`).
    matches!(
        super::codegen::const_eval_int(init, &super::codegen::ConstFieldMap::new()),
        Some(v) if v >= lo && v <= hi
    )
}

fn assignable(table: &SymbolTable, from: &RType, to: &RType) -> bool {
    if from == to || lenient(from) || lenient(to) {
        return true;
    }
    match (from, to) {
        (RType::Prim(a), RType::Prim(b)) => widening_ok(*a, *b),
        (RType::Array(a), RType::Array(b)) => assignable(table, a, b),
        // **Boxing** (§5.1.7) seguido opcionalmente de *widening* de referencia: `int` → `Integer`,
        // y también `int` → `Number`/`Object` (el wrapper es subtipo). El contexto de asignación
        // (§5.2) lo permite; la conversión concreta (`Integer.valueOf`) la inserta `transtypes`.
        (RType::Prim(p), t) if is_reference(t) => match table.external(types::wrapper_of(*p)) {
            Some(w) => is_subtype(table, &RType::Class(w), t),
            None => false,
        },
        // **Unboxing** (§5.1.8) seguido opcionalmente de *widening* primitivo: `Integer` → `int`,
        // `Integer` → `long`/`double`.
        (RType::Class(c), RType::Prim(b)) => {
            types::unboxed(table, *c).is_some_and(|p| widening_ok(p, *b))
        }
        // Tipos referencia (crudos o parametrizados): subtipado, con indulgencia para externos.
        _ if is_reference(from) && is_reference(to) => is_subtype(table, from, to),
        _ => false,
    }
}

fn is_reference(t: &RType) -> bool {
    matches!(t, RType::Class(_) | RType::Parameterized { .. } | RType::TypeVar(_) | RType::Capture { .. } | RType::Intersection(_))
}

/// Subtipo **probado**, delegando en el álgebra de tipos ([`types::is_subtype`]): sube por la
/// jerarquía **sustituyendo** los argumentos, y compara los de la misma base por *containment*.
fn is_subtype_strict(table: &SymbolTable, a: &RType, b: &RType) -> bool {
    types::is_subtype(table, a, b)
}

/// Subtipo para **chequear asignaciones**: como [`is_subtype_strict`], pero indulgente si
/// interviene un tipo del que no sabemos nada ([`is_opaque`]) — ahí no podemos probar ni refutar,
/// y preferimos callar antes que inventar un error. El overload resolution **no** usa esta:
/// necesita precisión, porque la indulgencia haría aplicable a cualquier candidato.
fn is_subtype(table: &SymbolTable, a: &RType, b: &RType) -> bool {
    if types::is_subtype(table, a, b) {
        return true;
    }
    match (types::erased_id(a), types::erased_id(b)) {
        (Some(x), Some(y)) => is_opaque(table, x) || is_opaque(table, y),
        _ => false,
    }
}

/// ¿Es un tipo del que **no tenemos la jerarquía**? Son los que el class finder no encontró en el
/// classpath y quedaron como *stub* de `JAVA_LANG`, o los que sí cargamos pero cuya superclase no
/// pudimos resolver (su `.class` no está). Con esos, un subtipado que no se prueba no significa
/// que sea falso: no marcamos error.
///
/// **No** basta con "es externo": ahora que leemos el atributo `Signature`, la mayoría de los
/// tipos del JDK traen su jerarquía real y se pueden chequear como cualquier otro.
fn is_opaque(table: &SymbolTable, class: SymbolId) -> bool {
    if table.symbol(class).owner.is_some() {
        return false; // es del fuente: sabemos todo
    }
    if table.symbol(class).name == "Object" {
        return false; // la raíz legítimamente no tiene superclase
    }
    match table.super_type(class) {
        None => true,                        // stub: nunca se cargó
        Some(RType::Unresolved) => true,     // cargado, pero su super no resolvió
        Some(_) => false,                    // jerarquía real
    }
}

fn widening_ok(from: PrimType, to: PrimType) -> bool {
    if from == to {
        return true;
    }
    // Sin conversiones desde/hacia boolean; el resto por rango numérico (JLS §5.1.2).
    if matches!(from, PrimType::Boolean) || matches!(to, PrimType::Boolean) {
        return false;
    }
    prim_rank_of(from) <= prim_rank_of(to)
}

// ---- helpers de tipos primitivos ----

fn is_numeric(t: &RType) -> bool {
    matches!(t, RType::Prim(p) if !matches!(p, PrimType::Boolean))
}
fn is_bool(t: &RType) -> bool {
    matches!(t, RType::Prim(PrimType::Boolean))
}
fn is_integral(t: &RType) -> bool {
    matches!(t, RType::Prim(PrimType::Int | PrimType::Long | PrimType::Short | PrimType::Byte | PrimType::Char))
}
fn lenient(t: &RType) -> bool {
    matches!(t, RType::Unresolved | RType::TypeVar(_))
}
fn prim_rank(t: &RType) -> u8 {
    match t {
        RType::Prim(p) => prim_rank_of(*p),
        _ => 0,
    }
}
fn prim_rank_of(p: PrimType) -> u8 {
    match p {
        PrimType::Boolean => 0,
        PrimType::Byte => 1,
        PrimType::Short | PrimType::Char => 2,
        PrimType::Int => 3,
        PrimType::Long => 4,
        PrimType::Float => 5,
        PrimType::Double => 6,
    }
}
fn rank_to_rtype(rank: u8) -> RType {
    RType::Prim(match rank {
        4 => PrimType::Long,
        5 => PrimType::Float,
        6 => PrimType::Double,
        _ => PrimType::Int,
    })
}

fn is_string(table: &SymbolTable, t: &RType) -> bool {
    matches!(t, RType::Class(c) if table.symbol(*c).name == "String")
}
fn class_rtype(table: &SymbolTable, simple: &str) -> RType {
    match table.external(simple) {
        Some(id) => RType::Class(id),
        None => RType::Unresolved,
    }
}

// ---- overload resolution en 3 fases (JLS §15.12.2) ----

/// El contexto de conversión de cada **fase**. Java prueba las fases **en orden y corta en la
/// primera que encuentre algún aplicable** (JLS §15.12.2): así, al agregar boxing y varargs en
/// Java 5, ningún programa previo cambió de significado — la fase 1 replica las reglas de antes.
#[derive(Clone, Copy, PartialEq)]
enum Phase {
    /// §15.12.2.2 — identidad, *widening* primitivo y de referencia. Sin boxing ni varargs.
    Strict,
    /// §15.12.2.3 — lo anterior **+ boxing/unboxing**. Sin varargs.
    Loose,
    /// §15.12.2.4 — lo anterior **+ aridad variable**.
    Varargs,
}

/// ¿`from` se convierte a `to` en un contexto de invocación (JLS §5.3)? Con `boxing`, además de
/// identidad/*widening*, admite boxing (`int → Integer [→ Number/Object]`) y unboxing
/// (`Integer → int [→ long]`).
///
/// Usa el subtipado **estricto** a propósito: con la versión indulgente, cualquier par de tipos
/// externos sería convertible y **todos** los candidatos resultarían aplicables.
fn convertible(table: &SymbolTable, from: &RType, to: &RType, boxing: bool) -> bool {
    if from == to || lenient(from) || lenient(to) {
        return true;
    }
    match (from, to) {
        (RType::Prim(a), RType::Prim(b)) => widening_ok(*a, *b),
        (RType::Array(a), RType::Array(b)) => convertible(table, a, b, false),
        // Boxing, opcionalmente seguido de *widening* de referencia (§5.1.7 + §5.1.5).
        (RType::Prim(p), t) if boxing && is_reference(t) => match table.external(types::wrapper_of(*p)) {
            Some(w) => is_subtype_strict(table, &RType::Class(w), t),
            None => false,
        },
        // Unboxing, opcionalmente seguido de *widening* primitivo (§5.1.8 + §5.1.2).
        (RType::Class(c), RType::Prim(b)) if boxing => {
            types::unboxed(table, *c).is_some_and(|p| widening_ok(p, *b))
        }
        // Tipos referencia: subtipado estricto (con genéricos y containment).
        _ if is_reference(from) && is_reference(to) => is_subtype_strict(table, from, to),
        _ => false,
    }
}

/// La firma resuelta de un método: `(params, varargs)`.
fn signature_of(table: &SymbolTable, m: SymbolId) -> Option<(Vec<RType>, bool)> {
    match table.resolved(m) {
        Some(Resolved::Method { params, varargs, .. }) => Some((params.clone(), *varargs)),
        _ => None,
    }
}

/// Descarta las sobrecargas cuyo parámetro funcional es **incompatible con la forma** de un argumento
/// lambda (§15.12.2.5): distinta **aridad** del SAM, o un cuerpo que **produce un valor** contra un SAM
/// `void`. Solo actúa con ≥2 candidatos y algún arg lambda; si el filtro los vacía, se deja la lista
/// original (mejor "ambiguo" que perder el candidato correcto). Los *method refs* no se filtran acá.
fn disambiguate_by_lambda(
    table: &SymbolTable,
    recv: &RType,
    cands: &[SymbolId],
    args: &[Expr],
) -> Vec<SymbolId> {
    let has_lambda = args.iter().any(|a| matches!(a.kind, ExprKind::Lambda { .. }));
    if cands.len() < 2 || !has_lambda {
        return cands.to_vec();
    }
    let filtered: Vec<SymbolId> = cands
        .iter()
        .copied()
        .filter(|&m| {
            let Some((params, _)) = signature_of(table, m) else { return true };
            args.iter().enumerate().all(|(i, a)| {
                let ExprKind::Lambda { params: lp, body } = &a.kind else { return true };
                let Some(pt) = params.get(i) else { return true };
                let pt = substitute_member(table, recv, m, pt);
                let Some(fi) = types::erased_id(&pt) else { return false };
                let Some(sam) = functional_sam(table, fi) else { return false };
                let Some((sparams, _)) = signature_of(table, sam) else { return true };
                if sparams.len() != lp.len() {
                    return false; // aridad del SAM distinta de la de la lambda
                }
                let sam_void = matches!(sam_ret(table, sam), RType::Void);
                // Un cuerpo que solo produce un **valor** no es *void-compatible* (§15.27.2).
                !(sam_void && lambda_value_only(body))
            })
        })
        .collect();
    if filtered.is_empty() {
        cands.to_vec()
    } else {
        filtered
    }
}

/// El retorno resuelto de un método (`Unresolved` si no se puede).
fn sam_ret(table: &SymbolTable, m: SymbolId) -> RType {
    match table.resolved(m) {
        Some(Resolved::Method { ret, .. }) => ret.clone(),
        _ => RType::Unresolved,
    }
}

/// ¿El cuerpo de la lambda **solo produce un valor** (§15.27.2)? Un cuerpo-expresión que no es una
/// **sentencia-expresión** (`x + 1`, un literal, una comparación) no es *void-compatible*; un bloque, o
/// una expresión que además es sentencia (llamada, asignación, `new`, `++`/`--`), se deja pasar.
fn lambda_value_only(body: &LambdaBody) -> bool {
    match body {
        LambdaBody::Expr(e) => !is_statement_expr(e),
        LambdaBody::Block(_) => false,
    }
}

fn is_statement_expr(e: &Expr) -> bool {
    matches!(
        &e.kind,
        ExprKind::Call { .. }
            | ExprKind::Assign { .. }
            | ExprKind::NewObject { .. }
            | ExprKind::Unary { op: UnOp::Inc | UnOp::Dec, .. }
    )
}

/// El **SAM** (*single abstract method*) de una interfaz funcional (§9.8), o `None` si `cid` no es
/// una interfaz con **exactamente uno** de esos métodos. Un lambda/method ref se tipa contra este
/// método. No cuentan: los `default`/`static`, ni los que tienen la forma de un método público de
/// `Object` (`equals`/`hashCode`/`toString`) —por eso `Comparator`, que redeclara `equals`, sigue
/// siendo funcional—. Recorre las super-interfaces, deduplicando por firma borrada (una SAM
/// heredada y redeclarada cuenta una sola vez).
pub(crate) fn functional_sam(table: &SymbolTable, cid: SymbolId) -> Option<SymbolId> {
    if !matches!(&table.symbol(cid).kind, SymbolKind::Class { kind: TypeKind::Interface, .. }) {
        return None;
    }
    let mut sams: Vec<SymbolId> = Vec::new();
    let mut seen: Vec<(String, Vec<RType>)> = Vec::new();
    let mut stack = vec![cid];
    let mut visited = vec![cid];
    while let Some(c) = stack.pop() {
        for id in table.members_of(c) {
            let sym = table.symbol(id);
            if !matches!(sym.kind, SymbolKind::Method { is_constructor: false, .. }) {
                continue;
            }
            if sym.modifiers.contains(&Modifier::Static) || !sym.modifiers.contains(&Modifier::Abstract) {
                continue; // `static` o `default` (con cuerpo → no abstracto)
            }
            if matches!(sym.name.as_str(), "equals" | "hashCode" | "toString") {
                continue; // forma de un método público de `Object` (§9.8)
            }
            let erased: Vec<RType> = signature_of(table, id)
                .map(|(p, _)| p.iter().map(|t| types::erasure(table, t)).collect())
                .unwrap_or_default();
            let key = (sym.name.clone(), erased);
            if seen.contains(&key) {
                continue; // heredada y ya contada
            }
            seen.push(key);
            sams.push(id);
        }
        for sup in table.interfaces(c) {
            if !visited.contains(&sup) {
                visited.push(sup);
                stack.push(sup);
            }
        }
    }
    if sams.len() == 1 {
        Some(sams[0])
    } else {
        None
    }
}

/// Los métodos con ese nombre visibles desde `class`, subiendo por la jerarquía — los
/// *potencialmente aplicables* (JLS §15.12.2.1). Un método de una subclase **tapa** al de la
/// superclase con la misma firma (override), así no compite consigo mismo.
///
/// Limitación conocida: sube solo por `super_class`; los métodos `default` heredados de
/// interfaces todavía no entran.
/// La sustitución que impone un **type witness** (`m.<A, B>f(...)`): cada parámetro de tipo del
/// método, en orden, atado al argumento explícito. Reemplaza a la inferencia (§18.5.2). Un desajuste
/// de aridad —o un argumento que no sea un tipo concreto— se salta; el chequeo estricto es de otra
/// fase.
fn witness_subst(env: &Env, m: SymbolId, type_args: &[TypeArg]) -> types::Subst {
    let params = infer::method_type_params(env.table, m);
    let mut subst = types::Subst::new();
    for (p, a) in params.iter().zip(type_args) {
        if let TypeArg::Type(ty) = a {
            subst.insert(*p, resolve_rtype(env.table, env.class_scope, ty));
        }
    }
    subst
}

pub(crate) fn candidates(table: &SymbolTable, class: SymbolId, name: &str) -> Vec<SymbolId> {
    let mut out: Vec<SymbolId> = Vec::new();
    // Se recorre **todo** el grafo de supertipos —superclase y **superinterfaces**— no solo la cadena
    // de `super_class`: un método heredado por una superinterfaz (`apply` de `BiFunction`, sobre un
    // `BinaryOperator extends BiFunction<T,T,T>`) también es candidato (finding #15). La clase se procesa
    // **primero**, así sus overrides tapan a los del supertipo (dedup por firma borrada). La sustitución
    // de los args de tipo del supertipo (`BiFunction<T,U,R>` → `<Integer,Integer,Integer>`) la hace luego
    // `substitute_member` con el receptor.
    let mut stack = vec![class];
    let mut visited = vec![class];
    while let Some(c) = stack.pop() {
        for &id in table.scope(member_scope(table, c)).get(name) {
            if !matches!(table.symbol(id).kind, SymbolKind::Method { is_constructor: false, .. }) {
                continue;
            }
            let sig = signature_of(table, id).map(|(p, _)| p);
            let overridden = out.iter().any(|&o| signature_of(table, o).map(|(p, _)| p) == sig);
            if !overridden {
                out.push(id);
            }
        }
        let mut supers: Vec<SymbolId> = table.interfaces(c);
        supers.extend(table.super_class(c));
        for sup in supers {
            if !visited.contains(&sup) {
                visited.push(sup);
                stack.push(sup);
            }
        }
    }
    out
}

/// ¿Es `m` aplicable a `args` en esta `phase`?
fn applicable(table: &SymbolTable, m: SymbolId, args: &[RType], phase: Phase, recv: &RType) -> bool {
    let Some((raw, varargs)) = signature_of(table, m) else { return false };
    // Se **sustituye el receptor** en los parámetros (§15.12.2.2): una variable de tipo de **clase**
    // (`E` de `List<E>`) pasa a su argumento —incluida la variable de **captura** de un `? super B`,
    // que fuerza el chequeo del lado escritura (`list.add(x)`)—. Las de **método** (`<T>`) no las
    // toca el receptor: quedan como variable, indulgentes, para que la inferencia las resuelva.
    let params: Vec<RType> = raw.iter().map(|p| substitute_member(table, recv, m, p)).collect();
    match phase {
        // Fases 1 y 2: aridad **exacta**. Un método varargs también puede entrar acá si se le
        // pasa el array directo (`f(arr)` con `f(int... xs)`) — su último param *es* `int[]`.
        Phase::Strict | Phase::Loose => {
            let boxing = phase == Phase::Loose;
            params.len() == args.len()
                && params.iter().zip(args).all(|(p, a)| convertible(table, a, p, boxing))
        }
        // Fase 3: los `n-1` primeros contra sus params, y el **resto** contra el tipo elemento
        // del varargs.
        Phase::Varargs => {
            if !varargs || args.len() + 1 < params.len() {
                return false;
            }
            let fixed = params.len() - 1;
            if !params[..fixed].iter().zip(args).all(|(p, a)| convertible(table, a, p, true)) {
                return false;
            }
            let RType::Array(elem) = &params[fixed] else { return false };
            args[fixed..].iter().all(|a| convertible(table, a, elem, true))
        }
    }
}

/// §18.5.1 — filtro de aplicabilidad por **inferencia**: un método genérico **propio** solo es
/// aplicable si existe una instanciación de sus variables que satisfaga los argumentos (`m(T, List<T>)`
/// **no** aplica a `(List<String>, List<String>)`). Sobre un **externo** se mantiene la indulgencia (no
/// modelamos toda firma del JDK), así que no se corre el filtro.
fn passes_inference(table: &SymbolTable, m: SymbolId, args: &[RType], recv: &RType) -> bool {
    let external = match table.symbol(m).owner {
        Some(o) => is_external(table, o),
        None => true,
    };
    external || infer::applicable_by_inference(table, recv, m, args)
}

/// ¿Es `a` **más específico** que `b` (JLS §15.12.2.5)? Lo es si cada parámetro de `a` es
/// subtipo del de `b` — intuitivamente, si `a` acepta menos cosas. Para los primitivos el
/// subtipado es el *widening* (`int <: long`, §4.10.1), así que `f(int)` gana a `f(long)`.
fn more_specific(table: &SymbolTable, a: SymbolId, b: SymbolId) -> bool {
    let (Some((pa, _)), Some((pb, _))) = (signature_of(table, a), signature_of(table, b)) else {
        return false;
    };
    pa.len() == pb.len() && pa.iter().zip(&pb).all(|(x, y)| convertible(table, x, y, false))
}

/// Corre las **3 fases** sobre los candidatos y elige el método. `None` si ninguno aplica.
/// Reporta la ambigüedad (§15.12.2.5) cuando ningún aplicable es el más específico.
fn resolve_overload(
    env: &mut Env,
    cands: &[SymbolId],
    args: &[RType],
    name: &str,
    pos: Pos,
    recv: &RType,
) -> Option<SymbolId> {
    for phase in [Phase::Strict, Phase::Loose, Phase::Varargs] {
        let mut applicables: Vec<SymbolId> =
            cands.iter().copied().filter(|&m| applicable(env.table, m, args, phase, recv)).collect();
        if applicables.is_empty() {
            continue; // esta fase no encontró nada: probar la siguiente
        }
        // §18.5.1 — con **varios** candidatos, se descartan los que la **inferencia** hace inaplicables
        // (`m(T, Lst<T>)` no aplica a `(Lst<String>, Lst<String>)`), lo que puede desempatar. Con un
        // **único** candidato no se filtra: ahí, aplicable o no, el resultado (o el error) lo da el
        // sitio de la llamada —y así no se rompe el *solving combinado* de un argumento poly, cuyo tipo
        // provisional todavía es prematuro—.
        if applicables.len() > 1 {
            let refined: Vec<SymbolId> =
                applicables.iter().copied().filter(|&m| passes_inference(env.table, m, args, recv)).collect();
            if !refined.is_empty() {
                applicables = refined;
            }
        }
        // Si hay algún candidato con la firma **totalmente resuelta**, descartar los que tengan
        // parámetros `Unresolved` (aplicables solo por indulgencia): si no, empatarían con el
        // candidato correcto y la llamada saldría "ambigua" (p. ej. `StringBuilder.append`).
        if applicables.iter().any(|&m| !has_unresolved_params(env.table, m)) {
            applicables.retain(|&m| !has_unresolved_params(env.table, m));
        }
        // Hay aplicables: **se corta acá**, aunque una fase posterior tuviera un match "mejor".
        return Some(choose_most_specific(env, &applicables, name, pos));
    }
    None
}

/// Elige el más específico entre los aplicables de una fase; si no hay uno que gane a todos,
/// la llamada es **ambigua** (JLS §15.12.2.5).
fn choose_most_specific(env: &mut Env, applicables: &[SymbolId], name: &str, pos: Pos) -> SymbolId {
    let mut best = applicables[0];
    for &m in &applicables[1..] {
        if more_specific(env.table, m, best) {
            best = m;
        }
    }
    let wins_all = applicables.iter().all(|&m| m == best || more_specific(env.table, best, m));
    if !wins_all {
        env.error(pos, format!("la referencia a `{name}` es ambigua"));
    }
    best
}

/// ¿Alguno de los tipos de parámetro de `m` quedó **sin resolver** (un tipo que no pudimos cargar
/// del classpath)? Un parámetro `Unresolved` es aplicable por indulgencia a cualquier argumento, y
/// eso rompe el desempate por *más específico* — así que estos candidatos se posponen.
fn has_unresolved_params(table: &SymbolTable, m: SymbolId) -> bool {
    fn unresolved(t: &RType) -> bool {
        match t {
            RType::Unresolved => true,
            RType::Array(e) => unresolved(e),
            _ => false,
        }
    }
    match table.resolved(m) {
        Some(Resolved::Method { params, .. }) => params.iter().any(unresolved),
        _ => false,
    }
}

// ---- lookup de miembros por la jerarquía ----

/// Aplica a la firma de un miembro la sustitución que impone el **receptor**: mirando
/// `xs.get(0)` con `xs: List<String>`, el `E` declarado en `List<E>` se ve como `String`.
/// Sube por la jerarquía para los miembros heredados (`ArrayList<String>` → `List<String>`).
pub(crate) fn substitute_member(table: &SymbolTable, recv: &RType, member: SymbolId, ty: &RType) -> RType {
    let Some(owner) = table.symbol(member).owner else { return ty.clone() };
    let subst = types::subst_for(table, recv, owner);
    if subst.is_empty() {
        return ty.clone();
    }
    types::substitute(ty, &subst)
}

/// ¿Es `class` un tipo **externo** (cargado del classpath), no uno del fuente? Los externos no
/// tienen dueño en la tabla (se registran por nombre simple); los del fuente pertenecen a su
/// paquete o clase envolvente. Se usa para no reportar miembros faltantes sobre jerarquías que
/// solo cargamos en parte.
fn is_external(table: &SymbolTable, class: SymbolId) -> bool {
    table.symbol(class).owner.is_none()
}

/// La jerarquía de `class` está **completa** si su cadena de superclases resuelve entera (hasta un
/// tipo sin super, como `Object`). Si algún super quedó **sin cargar** —un `.class` que no estaba en
/// el classpath—, un miembro no hallado podría estar heredado de esa parte que falta, y hay que ser
/// indulgente. Reemplaza a la vieja indulgencia con *todo* tipo externo: ahora que la carga es
/// transitiva, la mayoría de los externos quedan completos y sus misses genuinos se reportan.
fn hierarchy_complete(table: &SymbolTable, class: SymbolId) -> bool {
    let mut cur = class;
    for _ in 0..64 {
        match table.super_type(cur).and_then(types::erased_id) {
            Some(sup) => cur = sup,
            // `None` significa **o** que no hay super declarada (raíz alcanzada, completa) **o** que
            // la declarada no resolvió (incompleta). Se distinguen mirando si había una.
            None => return table.super_type(cur).is_none(),
        }
    }
    true // cadena larguísima: se asume completa (no colgar)
}

/// Declara las **variables de patrón** de un `case` (`case Integer i`) en el scope del case, con su
/// tipo resuelto, y les decora el `slot` — [`super::flow`] lo necesita para saber que la variable
/// está definitivamente asignada dentro del brazo (como la del `catch`).
fn bind_patterns(env: &mut Env, labels: &mut [CaseLabel]) {
    for l in labels {
        if let CaseLabel::Pattern(p) = l {
            bind_pattern(env, p);
        }
    }
}

/// Declara las variables de un patrón, **recursivamente**: una deconstrucción de `record` bindea las
/// de cada componente (que a su vez pueden deconstruir).
fn bind_pattern(env: &mut Env, p: &mut Pattern) {
    match p {
        Pattern::Type { ty, name, slot } => {
            let rt = resolve_rtype(env.table, env.class_scope, ty);
            *slot = Some(env.define(name, rt));
        }
        Pattern::Record { components, .. } => {
            for c in components {
                bind_pattern(env, c);
            }
        }
    }
}

pub(crate) fn member_scope(table: &SymbolTable, cid: SymbolId) -> ScopeId {
    match &table.symbol(cid).kind {
        SymbolKind::Class { members, .. } => *members,
        _ => table.global,
    }
}

/// Los **constructores** de una clase. No se heredan (§8.8), así que se buscan solo en su propio
/// scope, donde `enter` los registra bajo el nombre de la clase.
/// Los constructores de una clase. Se buscan bajo **dos** nombres: el simple, que es como los
/// declara el fuente, y `<init>`, que es como los nombra un `.class` — o sea, como llegan los tipos
/// **externos** leídos del classpath. Sin el segundo, `super(...)` contra una clase de afuera
/// (`java.lang.Enum`, sin ir más lejos) no encontraría nada.
pub(crate) fn constructors(table: &SymbolTable, class: SymbolId) -> Vec<SymbolId> {
    let scope = member_scope(table, class);
    let simple = table.symbol(class).name.clone();
    let mut out: Vec<SymbolId> = table.scope(scope).get(&simple).to_vec();
    out.extend_from_slice(table.scope(scope).get("<init>"));
    out.retain(|&id| matches!(table.symbol(id).kind, SymbolKind::Method { is_constructor: true, .. }));
    out
}

/// La clase que **encierra léxicamente** a `cid` (su `owner`, si es una clase): el enclosing de una
/// inner class, por donde se resuelven sus miembros capturados. `None` para una top-level.
pub(crate) fn enclosing_class(table: &SymbolTable, cid: SymbolId) -> Option<SymbolId> {
    let owner = table.symbol(cid).owner?;
    matches!(table.symbol(owner).kind, SymbolKind::Class { .. }).then_some(owner)
}

fn lookup_field(table: &SymbolTable, class: SymbolId, name: &str) -> Option<SymbolId> {
    let mut cur = Some(class);
    while let Some(c) = cur {
        for &id in table.scope(member_scope(table, c)).get(name) {
            if matches!(table.symbol(id).kind, SymbolKind::Field { .. }) {
                return Some(id);
            }
        }
        cur = table.super_class(c);
    }
    None
}

/// Resuelve un `Type` sintáctico a [`RType`] en `scope`.
pub(crate) fn resolve_rtype(table: &SymbolTable, scope: ScopeId, ty: &Type) -> RType {
    match ty {
        Type::Void => RType::Void,
        Type::Prim(p) => RType::Prim(*p),
        Type::Array(inner) => RType::Array(Box::new(resolve_rtype(table, scope, inner))),
        Type::Var => RType::Unresolved,
        Type::Class(name) => resolve_type_name(table, scope, name),
        Type::Parameterized { base, args } => match resolve_type_name(table, scope, base) {
            // Un parámetro de tipo no lleva argumentos (`T<X>` no existe).
            RType::TypeVar(id) => RType::TypeVar(id),
            RType::Class(id) => RType::Parameterized {
                base: id,
                args: args.iter().map(|a| resolve_rtype_arg(table, scope, a)).collect(),
            },
            other => other,
        },
    }
}

fn resolve_rtype_arg(table: &SymbolTable, scope: ScopeId, arg: &TypeArg) -> RTypeArg {
    match arg {
        TypeArg::Type(t) => RTypeArg::Type(resolve_rtype(table, scope, t)),
        TypeArg::Extends(t) => RTypeArg::Extends(Box::new(resolve_rtype(table, scope, t))),
        TypeArg::Super(t) => RTypeArg::Super(Box::new(resolve_rtype(table, scope, t))),
        TypeArg::Wildcard => RTypeArg::Wildcard,
    }
}

/// Resuelve un nombre de tipo a [`RType`]: primero por scope (anidados, params de tipo, mismo
/// paquete), después como externo del classpath.
fn resolve_type_name(table: &SymbolTable, scope: ScopeId, name: &str) -> RType {
    if let Some(id) = table.resolve_type(scope, name) {
        if matches!(table.symbol(id).kind, SymbolKind::TypeVar { .. }) {
            RType::TypeVar(id)
        } else {
            RType::Class(id)
        }
    } else if let Some(id) = table.external(name) {
        RType::Class(id)
    } else if let Some((_, simple)) = name.rsplit_once('.') {
        // Un nombre **cualificado** (`java.lang.Object`): el *class finder* registra los externos por
        // su nombre **simple**, así que se resuelve por el último segmento. Sin esto, un tipo FQN en un
        // `new` (`new java.lang.Object()`) quedaba `Unresolved` y el codegen no podía emitir el `new` +
        // `invokespecial` — finding #20. (En otras posiciones no se notaba: un `Unresolved` de un local
        // o retorno no rompe la emisión.)
        match table.external(simple) {
            Some(id) => RType::Class(id),
            None => RType::Unresolved,
        }
    } else {
        RType::Unresolved
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::javac::{enter::enter, lexer::tokenize, parser::parse};

    fn check(src: &str) -> Vec<Error> {
        let mut unit = parse(tokenize(src).unwrap()).0;
        let (table, _e) = enter(&unit);
        attribute(&mut unit, &table)
    }

    #[test]
    fn a_call_with_contradictory_type_inference_is_reported() {
        // `m(Lst<Foo>, Lst<Bar>)` sobre `<T> void m(Lst<T>, Lst<T>)`: `T` no puede ser `Foo` **y**
        // `Bar` a la vez (§18.5.1). Antes se aceptaba en silencio (se elegía una arbitraria); ahora se
        // reporta el error de inferencia.
        let errs = check(
            "class Lst<E> {} class Foo {} class Bar {}
             class C { <T> void m(Lst<T> a, Lst<T> b) {}
                       void use(Lst<Foo> f, Lst<Bar> b) { m(f, b); } }",
        );
        assert!(
            errs.iter().any(|e| e.message.contains("inferir los argumentos de tipo")),
            "{errs:?}"
        );
    }

    #[test]
    fn an_argument_outside_a_type_bound_is_reported() {
        // `m(Other)` sobre `<T extends Num> T m(T)`: `Other` no está dentro de la cota `Num` (§18.3 /
        // §4.5.1). La aplicabilidad, indulgente con la variable de método, no lo cazaba; ahora la
        // inferencia lo marca insatisfacible y se reporta.
        let errs = check(
            "class Num {} class Other {}
             class C { <T extends Num> T m(T x) { return x; }
                       void use(Other o) { m(o); } }",
        );
        assert!(
            errs.iter().any(|e| e.message.contains("inferir los argumentos de tipo")),
            "{errs:?}"
        );
    }

    #[test]
    fn a_call_with_consistent_type_inference_is_fine() {
        // El mismo método con dos `Lst<Foo>`: `T = Foo` de las dos, consistente — sin error.
        let errs = check(
            "class Lst<E> {} class Foo {}
             class C { <T> void m(Lst<T> a, Lst<T> b) {}
                       void use(Lst<Foo> f, Lst<Foo> g) { m(f, g); } }",
        );
        assert!(!errs.iter().any(|e| e.message.contains("inferir")), "{errs:?}");
    }

    #[test]
    fn a_field_initializer_is_type_checked() {
        // Antes se salteaba; ahora `int f = "x"` se rechaza *in situ* (§8.3.2).
        let errs = check("class C { int f = \"x\"; }");
        assert!(errs.iter().any(|e| e.message.contains("inicializador")), "{errs:?}");
    }

    #[test]
    fn a_boxed_field_initializer_is_fine() {
        // Contexto de asignación con boxing: `Integer f = 5` es válido.
        assert!(check("class C { Integer f = 5; }").is_empty());
    }

    #[test]
    fn an_instance_field_initializer_can_use_this_context() {
        // Un inicializador de instancia corre en el constructor (con `this`): puede llamar un método.
        assert!(check("class C { int m() { return 1; } int f = m(); }").is_empty());
    }

    #[test]
    fn a_constant_int_narrows_to_byte_short_char() {
        // §5.2 (lo destapó KajiLibrary con `Byte.MIN_VALUE = -128`): una **constante** `int` en rango
        // se asigna a `byte`/`short`/`char` **sin cast**, en campo o en local.
        assert!(check("class C { static final byte B = -128; static final short S = 32767; static final char CH = 65; }").is_empty());
        assert!(check("class C { void m() { byte b = 5; short s = -1; char c = 90; } }").is_empty());
        // §15.29: ahora también **expresiones constantes** (aritmética, shift, bits) que entran.
        assert!(
            check("class C { void m() { byte b = 1 + 2; short s = 1 << 4; byte m = 100 + 27; char c = 'A' + 1; } }")
                .is_empty(),
            "una expresión constante en rango narrowea sin cast",
        );
        // Fuera de rango, sigue siendo error — literal o expresión.
        assert_eq!(check("class C { static final byte B = 200; }").len(), 1, "200 no entra en byte");
        assert_eq!(
            check("class C { void m() { byte b = 100 + 100; } }").len(),
            1,
            "100+100=200 no entra en byte",
        );
        // Una constante `long` no narrowea por §5.2: necesita cast explícito.
        assert_eq!(
            check("class C { void m() { byte b = 3L; } }").len(),
            1,
            "una constante long a byte necesita cast",
        );
    }

    /// Parsea + atribuye, devolviendo la unidad **decorada** (para inspeccionar el AST).
    fn attrib(src: &str) -> crate::javac::ast::CompilationUnit {
        let mut unit = parse(tokenize(src).unwrap()).0;
        let (table, _e) = enter(&unit);
        attribute(&mut unit, &table);
        unit
    }

    /// Las sentencias del **primer método** de la primera clase (salteando campos y demás).
    fn body_of(unit: &crate::javac::ast::CompilationUnit) -> &[Stmt] {
        let m = unit.types[0]
            .members
            .iter()
            .find_map(|m| match m {
                Member::Method(m) => Some(m),
                _ => None,
            })
            .expect("esperaba un método");
        &m.body.as_ref().unwrap().0
    }

    #[test]
    fn types_a_well_formed_method() {
        let errs = check("class C { int a; int add(int x, int y) { int s = x + y; return s + a; } }");
        assert!(errs.is_empty(), "no debería haber errores: {errs:?}");
    }

    #[test]
    fn flags_undefined_name() {
        let errs = check("class C { void m() { z = 1; } }");
        assert_eq!(errs.len(), 1);
        assert!(errs[0].message.contains("z"));
    }

    #[test]
    fn flags_non_boolean_condition() {
        let errs = check("class C { void m() { if (1) return; } }");
        assert_eq!(errs.len(), 1);
        assert!(errs[0].message.contains("boolean"));
    }

    #[test]
    fn flags_return_type_mismatch() {
        let errs = check("class C { boolean m() { return 1; } }");
        assert_eq!(errs.len(), 1);
        assert!(errs[0].message.contains("retorno"));
    }

    #[test]
    fn resolves_field_and_call() {
        let errs = check("class C { int a; int get() { return a; } int use() { return get(); } }");
        assert!(errs.is_empty(), "{errs:?}");
    }

    #[test]
    fn external_members_resolve() {
        // `System.out.println` toca campos/métodos de tipos externos (`System.out` es un
        // `PrintStream`): con la carga transitiva de la jerarquía, resuelve y no se reporta.
        let errs = check("class C { void m(int x) { System.out.println(x); } }");
        assert!(errs.is_empty(), "{errs:?}");
    }

    // ---- decoración (la salida de la pasada) ----

    #[test]
    fn decorates_expressions_with_their_type() {
        let unit = attrib("class C { void m() { int s = 1 + 2; } }");
        let StmtKind::LocalVar { init: Some(init), .. } = &body_of(&unit)[0].kind else { panic!() };
        assert_eq!(init.ty, Some(RType::Prim(PrimType::Int)));
        // También los operandos, no solo la raíz.
        let ExprKind::Binary { lhs, .. } = &init.kind else { panic!() };
        assert_eq!(lhs.ty, Some(RType::Prim(PrimType::Int)));
    }

    #[test]
    fn binds_names_to_locals_with_their_slot() {
        // Método de instancia: slot 0 = `this`, el param `x` va al 1 y el local `y` al 2.
        let unit = attrib("class C { void m(int x) { int y = x; } }");
        let StmtKind::LocalVar { init: Some(init), .. } = &body_of(&unit)[0].kind else { panic!() };
        assert_eq!(init.binding, Some(Binding::Local { slot: 1 }), "`x` es el param → slot 1");
        assert_eq!(body_of(&unit)[0].local.as_ref().unwrap().slot, 2, "`y` es el local → slot 2");
    }

    #[test]
    fn static_method_has_no_this_slot() {
        let unit = attrib("class C { static void m(int x) { int y = x; } }");
        let StmtKind::LocalVar { init: Some(init), .. } = &body_of(&unit)[0].kind else { panic!() };
        assert_eq!(init.binding, Some(Binding::Local { slot: 0 }), "sin `this`, el param va al slot 0");
    }

    #[test]
    fn category_2_locals_take_two_slots() {
        // `long` ocupa 2 slots (JVMS §2.6.1): el siguiente local arranca en el 3, no en el 2.
        let unit = attrib("class C { static void m() { long a = 1L; int b = 2; } }");
        assert_eq!(body_of(&unit)[0].local.as_ref().unwrap().slot, 0);
        assert_eq!(body_of(&unit)[1].local.as_ref().unwrap().slot, 2);
    }

    #[test]
    fn binds_field_access_to_its_symbol() {
        let unit = attrib("class C { int a; void m() { int b = a; } }");
        let StmtKind::LocalVar { init: Some(init), .. } = &body_of(&unit)[0].kind else { panic!() };
        assert!(matches!(init.binding, Some(Binding::Field(_))), "`a` es un campo, no un local");
    }

    #[test]
    fn binds_call_to_its_method() {
        let unit = attrib("class C { int f() { return 1; } void m() { int b = f(); } }");
        let Member::Method(m) = &unit.types[0].members[1] else { panic!() };
        let StmtKind::LocalVar { init: Some(init), .. } = &m.body.as_ref().unwrap().0[0].kind else {
            panic!()
        };
        assert!(matches!(init.binding, Some(Binding::Method(_))));
    }

    #[test]
    fn var_local_takes_the_initializer_type() {
        let unit = attrib("class C { static void m() { var x = 1L; } }");
        let info = body_of(&unit)[0].local.as_ref().unwrap();
        assert_eq!(info.ty, RType::Prim(PrimType::Long));
        assert_eq!(info.slot, 0);
    }

    // ---- genéricos (etapa B) ----

    #[test]
    fn enforces_generic_invariance_in_bodies() {
        // `Lst<String>` no es asignable a `Lst<Object>` (§4.5.1). Regresión: los cuerpos
        // resolvían los tipos por su base, borrando los argumentos, y esto pasaba en silencio.
        let errs = check("class Lst<T> {} class C { void m(Lst<String> s) { Lst<Object> o = s; } }");
        assert_eq!(errs.len(), 1, "{errs:?}");
        assert!(errs[0].message.contains("incompatible"));
    }

    #[test]
    fn accepts_a_wildcard_that_contains_the_argument() {
        let errs =
            check("class Lst<T> {} class C { void m(Lst<String> s) { Lst<? extends Object> u = s; } }");
        assert!(errs.is_empty(), "{errs:?}");
    }

    #[test]
    fn decorates_a_local_with_its_parameterized_type() {
        // El tipo decorado tiene que conservar los argumentos, no ser el crudo.
        let unit = attrib("class Lst<T> {} class C { void m(Lst<String> s) { Lst<String> x = s; } }");
        let m = unit.types[1].members.iter().find_map(|mem| match mem {
            Member::Method(m) if m.name == "m" => Some(m),
            _ => None,
        });
        let body = &m.expect("método m").body.as_ref().unwrap().0;
        let info = body[0].local.as_ref().expect("el local está decorado");
        assert!(
            matches!(info.ty, RType::Parameterized { .. }),
            "esperaba un tipo parametrizado, no el crudo: {:?}",
            info.ty
        );
    }

    #[test]
    fn substitutes_the_receiver_type_arguments_in_a_jdk_call() {
        // `List<String>.get(int)` devuelve `String`: se lee el `E` del atributo `Signature` de
        // `java.util.List` y se sustituye `E:=String` por el receptor.
        let ok = check("import java.util.List;\nclass C { void m(List<String> xs) { String s = xs.get(0); } }");
        assert!(ok.is_empty(), "{ok:?}");

        let bad = check("import java.util.List;\nclass C { void m(List<String> xs) { Integer n = xs.get(0); } }");
        assert_eq!(bad.len(), 1, "get(0) es String, no Integer: {bad:?}");
    }

    // ---- capture conversion (§5.1.10) ----
    //
    // Se usa un **campo** `E val` en vez de métodos: el acceso a campo pasa por `assignable` →
    // `is_subtype`, que sí ejercita las cotas de la captura. La resolución de sobrecarga, en cambio,
    // trata un parámetro que es variable de tipo de clase como **indulgente** (no sustituye el
    // receptor en los params), así que el lado de **escritura** de `? super` no se observa por una
    // llamada — sí por una asignación a campo.
    const BOX: &str = "class Box<E> { E val; } ";

    #[test]
    fn capture_of_extends_reads_as_its_upper_bound() {
        // `Box<? extends Number>.val` captura a una variable con cota superior `Number`.
        assert!(check(&format!("{BOX} class C {{ void m(Box<? extends Number> b) {{ Number n = b.val; }} }}")).is_empty());
        let bad = check(&format!("{BOX} class C {{ void m(Box<? extends Number> b) {{ String s = b.val; }} }}"));
        assert_eq!(bad.len(), 1, "la captura de `? extends Number` no es String: {bad:?}");
    }

    #[test]
    fn capture_of_an_unbounded_wildcard_reads_as_object() {
        // `Box<?>.val` captura con cota superior `Object`: sale un `Object`, no un `Number`.
        assert!(check(&format!("{BOX} class C {{ void m(Box<?> b) {{ Object o = b.val; }} }}")).is_empty());
        let bad = check(&format!("{BOX} class C {{ void m(Box<?> b) {{ Number n = b.val; }} }}"));
        assert_eq!(bad.len(), 1, "de `Box<?>` solo se lee un `Object`: {bad:?}");
    }

    #[test]
    fn capture_of_super_reads_as_object() {
        // `Box<? super Integer>.val` leído da `Object` (cota superior de la captura).
        assert!(check(&format!("{BOX} class C {{ void m(Box<? super Integer> b) {{ Object o = b.val; }} }}")).is_empty());
        let bad = check(&format!("{BOX} class C {{ void m(Box<? super Integer> b) {{ Number n = b.val; }} }}"));
        assert_eq!(bad.len(), 1, "de `? super` solo se lee `Object`, no `Number`: {bad:?}");
    }

    #[test]
    fn capture_of_super_accepts_its_lower_bound_on_write() {
        // El lado **escritura** de `? super Integer`: se puede asignar un `Integer` (cota inferior de
        // la captura), pero **no** un `Object` (la lista podría ser `Box<Integer>`).
        assert!(check(&format!("{BOX} class C {{ void m(Box<? super Integer> b, Integer i) {{ b.val = i; }} }}")).is_empty());
        let bad = check(&format!("{BOX} class C {{ void m(Box<? super Integer> b, Object o) {{ b.val = o; }} }}"));
        assert_eq!(bad.len(), 1, "no se puede escribir un `Object` en `Box<? super Integer>`: {bad:?}");
    }

    #[test]
    fn distinct_captures_of_the_same_wildcard_do_not_unify() {
        // Frescura (§5.1.10): la captura de `a` y la de `b` son **distintas**, así que el valor de
        // `b.val` no se puede escribir en `a.val` — el clásico rechazo de captura.
        let bad = check(&format!("{BOX} class C {{ void m(Box<?> a, Box<?> b) {{ a.val = b.val; }} }}"));
        assert_eq!(bad.len(), 1, "capturas distintas no unifican: {bad:?}");
    }

    // Un `Box<E>` con un método de **escritura** (para el lado `? super` por llamada).
    const WBOX: &str = "class Box<E> { void put(E e) {} } ";

    #[test]
    fn capture_super_write_via_a_call_accepts_the_lower_bound() {
        // Con la sustitución del receptor en los params, el lado **escritura** de `? super` se
        // chequea por **llamada** (antes solo por campo): `put(Integer)` vale.
        assert!(check(&format!("{WBOX} class C {{ void m(Box<? super Integer> b, Integer i) {{ b.put(i); }} }}")).is_empty());
    }

    #[test]
    fn capture_super_write_via_a_call_rejects_a_wider_type() {
        // `put(Object)` sobre `Box<? super Integer>` se **rechaza** (antes se aceptaba por indulgencia).
        let bad = check(&format!("{WBOX} class C {{ void m(Box<? super Integer> b, Object o) {{ b.put(o); }} }}"));
        assert_eq!(bad.len(), 1, "no se puede `put(Object)` en `Box<? super Integer>`: {bad:?}");
    }

    #[test]
    fn a_parameterized_call_still_accepts_a_subtype_argument() {
        // La sustitución del receptor no rompe una llamada legítima: `Box<Number>.put(Integer)` vale
        // (el argumento es subtipo del parámetro sustituido).
        assert!(check(&format!("{WBOX} class C {{ void m(Box<Number> b, Integer i) {{ b.put(i); }} }}")).is_empty());
    }

    #[test]
    fn a_ternary_of_two_wrappers_is_usable_as_either_shared_supertype() {
        // `lub(Integer, Long)` = `Number & Comparable`: el resultado del ternario tipa como **ambos**.
        assert!(check(
            "class C { void m(boolean b, Integer i, Long l) { \
             Number n = b ? i : l; Comparable c = b ? i : l; } }"
        )
        .is_empty());
    }

    #[test]
    fn a_ternary_intersection_is_not_an_unrelated_type() {
        // Pero `Number & Comparable` **no** es `String`.
        let bad =
            check("class C { String m(boolean b, Integer i, Long l) { return b ? i : l; } }");
        assert!(!bad.is_empty(), "Number&Comparable no es asignable a String");
    }

    #[test]
    fn capture_enables_inference_passing_a_wildcard_to_a_generic_method() {
        // El caso insignia (§5.1.10 + Cap. 18): `helper(b)` con `b: Box<?>` se captura a `Box<CAP>`
        // y la sobrecarga genérica **infiere** `T` = la captura, así que la llamada tipa.
        let ok = check(
            "class Box<E> {} \
             class C { \
               static void swap(Box<?> b) { helper(b); } \
               static <T> void helper(Box<T> b) {} \
             }",
        );
        assert!(ok.is_empty(), "la captura debería permitir inferir T: {ok:?}");
    }

    #[test]
    fn substitutes_through_an_inherited_generic_member() {
        // `get` se declara en `Base<T>`; visto desde `StrBox` hay que sustituir `T:=String`
        // subiendo por la jerarquía.
        let ok = check(
            "class Base<T> { T get() { return null; } }
             class StrBox extends Base<String> {}
             class C { void m(StrBox b) { String s = b.get(); } }",
        );
        assert!(ok.is_empty(), "{ok:?}");

        let bad = check(
            "class Base<T> { T get() { return null; } }
             class StrBox extends Base<String> {}
             class C { void m(StrBox b) { Integer n = b.get(); } }",
        );
        assert_eq!(bad.len(), 1, "b.get() es String: {bad:?}");
    }

    #[test]
    fn unrelated_jdk_types_are_no_longer_leniently_assignable() {
        // Antes cualquier par de externos pasaba por indulgencia; ahora que traen su jerarquía
        // real del classpath, se chequean como cualquier otro tipo.
        let errs = check("class C { void m(String s) { Integer n = s; } }");
        assert_eq!(errs.len(), 1, "String no es asignable a Integer: {errs:?}");
    }

    #[test]
    fn a_wildcard_over_an_inference_variable_is_applicable() {
        // `<T> T top(Lst<? extends T> l)` con `Lst<Dog>`: la aplicabilidad es **indulgente** con el
        // `? extends T` (T es variable de inferencia), y la inferencia fija `T = Dog` (§18.2.3). javac
        // lo acepta; antes daba "no aplicable". El `? extends` de un tipo concreto sí sigue estricto.
        let ok = check(
            "class Lst<E> {} class Animal {} class Dog extends Animal {}
             class C { <T> T top(Lst<? extends T> l) { return null; }
                       void m(Lst<Dog> l) { Animal a = top(l); } }",
        );
        assert!(ok.is_empty(), "{ok:?}");
    }

    #[test]
    fn infers_a_type_variable_under_a_wildcard_in_the_return_type() {
        // §18.3 (capture-in-bounds, §18.5.2.1→§18.3.2): un método que devuelve `Box<? extends T>`
        // toma `T` del *target*. `Box<? extends Number> b = make()` fija `T = Number`. Antes la
        // inferencia no miraba dentro del wildcard del retorno, `T` caía a `Object`, y
        // `Box<? extends Object>` no era asignable a `Box<? extends Number>` → rechazo espurio.
        let ok = check(
            "class Box<E> {} class Number {}
             class C { <T> Box<? extends T> make() { return null; }
                       void m() { Box<? extends Number> b = make(); } }",
        );
        assert!(ok.is_empty(), "extends/extends deberia inferir T=Number: {ok:?}");

        // Contravariante: `Box<? super T> mk()` con target `Box<? super Integer>` da la cota
        // inferior `Integer <: T` (§18.3.2 caso d).
        let ok2 = check(
            "class Box<E> {} class Number {} class Integer extends Number {}
             class C { <T> Box<? super T> mk() { return null; }
                       void m() { Box<? super Integer> b = mk(); } }",
        );
        assert!(ok2.is_empty(), "super/super deberia inferir: {ok2:?}");

        // Un *target* invariante contra un retorno con wildcard NO es asignable (§18.3.2: `false`):
        // `Box<Number> b = make()` con `make(): Box<? extends T>` se rechaza, como javac.
        let bad = check(
            "class Box<E> {} class Number {}
             class C { <T> Box<? extends T> make() { return null; }
                       void m() { Box<Number> b = make(); } }",
        );
        assert!(!bad.is_empty(), "target invariante deberia rechazarse: {bad:?}");
    }

    #[test]
    fn a_wildcard_argument_captured_against_an_invariant_parameter_conflicts_with_a_concrete_target() {
        // §5.1.10 (capture conversion del argumento): pasar `Box<? extends T>` (de una llamada anidada
        // `make()`) a un parámetro INVARIANTE `Box<U>` captura ⇒ `U = CAP` (fresca). Si el target además
        // fija `U = Number`, son igualdades incompatibles ⇒ error, como javac. Antes lo aceptabamos: el
        // wildcard del argumento anidado se caía sin producir bound sobre `U`.
        let bad = check(
            "class Box<E> {} class Number {}
             class C { <T> Box<? extends T> make() { return null; }
                       <U> Box<U> id(Box<U> b) { return b; }
                       Box<Number> use() { return id(make()); } }",
        );
        assert!(!bad.is_empty(), "captura contra param invariante + target concreto: {bad:?}");

        // Contraprueba: capturar un **valor** wildcard contra un parámetro invariante y asignar a un
        // target compatible sí anda (`U = CAP <: Number`, y `CAP` cabe en `Number`).
        let ok = check(
            "class Box<E> {} class Number {}
             class C { <U> U first(Box<U> b) { return null; }
                       Number use(Box<? extends Number> x) { return first(x); } }",
        );
        assert!(ok.is_empty(), "captura de un valor wildcard a target compatible: {ok:?}");
    }

    #[test]
    fn a_nested_wildcard_returning_call_does_not_flow_the_outer_target_into_its_variable() {
        // §18.4: una llamada anidada que devuelve `Box<? extends T>` tiene su `T` bajo un wildcard;
        // esa variable se resuelve por SUS cotas (ninguna ⇒ su cota declarada `Object`), NO por el
        // target externo. `take(make())` con target `Number` ⇒ `U :> Object` (de `make`) y
        // `U <: Number` (del target) ⇒ incompatibles ⇒ error, como javac. Antes lo aceptabamos: la
        // clausura transitiva heredaba `Number` a la `T` interna y la bajaba a `Number`.
        let bad = check(
            "class Box<E> {} class Number {}
             class C { <T> Box<? extends T> make() { return null; }
                       <U> U take(Box<? extends U> b) { return null; }
                       Number use() { return take(make()); } }",
        );
        assert!(!bad.is_empty(), "target Number deberia ser incompatible: {bad:?}");

        // Contraprueba: con target `Object` sí resuelve (`U = Object`), como javac.
        let ok = check(
            "class Box<E> {} class Number {}
             class C { <T> Box<? extends T> make() { return null; }
                       <U> U take(Box<? extends U> b) { return null; }
                       Object use() { return take(make()); } }",
        );
        assert!(ok.is_empty(), "target Object deberia resolver U=Object: {ok:?}");
    }

    #[test]
    fn a_recovered_error_node_does_not_cascade_on_later_uses() {
        // Recuperación a nivel expresión: `int x = @@@;` deja `x` declarado con un nodo de error, que
        // la atribución tipa `Unresolved` **sin** un error nuevo. Así `x + 1` no cascadea
        // "no se encuentra: x" — el único error es el de sintaxis del inicializador.
        // (El error de sintaxis lo produce el parser; este helper solo devuelve los de la atribución.)
        // Lo que importa: la atribución **no agrega nada** —`x` está declarado, así que `x + 1` resuelve
        // y el nodo de error queda `Unresolved` en silencio—; antes, sin `x`, habría dos "no se encuentra".
        let errs = check("class C { void m() { int x = @@@; int z = x + 1; } }");
        assert!(errs.is_empty(), "la atribución no debe cascadear sobre `x`: {errs:?}");
    }

    #[test]
    fn cannot_find_symbol_attaches_symbol_and_location_notes() {
        // Un método inexistente sobre un receptor conocido lleva las notas `símbolo:`/`ubicación:`
        // (con la firma de argumentos y la clase del receptor), como javac.
        let errs = check("class C { void m(String s) { int n = s.fro(1); } }");
        assert_eq!(errs.len(), 1, "{errs:?}");
        assert_eq!(errs[0].message, "no se encuentra el método: fro");
        assert_eq!(
            errs[0].notes,
            vec!["símbolo:   método fro(int)".to_string(), "ubicación: clase String".to_string()]
        );

        // Un campo inexistente: `variable <name>` en la clase del receptor.
        let f = check("class C { void m(String s) { int k = s.missing; } }");
        assert_eq!(f[0].notes, vec![
            "símbolo:   variable missing".to_string(),
            "ubicación: clase String".to_string(),
        ]);

        // Un nombre pelado desconocido: `variable <name>` en la clase **envolvente**.
        let n = check("class C { void m() { int n = nope; } }");
        assert_eq!(n[0].notes, vec![
            "símbolo:   variable nope".to_string(),
            "ubicación: clase C".to_string(),
        ]);
    }

    #[test]
    fn an_inapplicable_overload_lists_the_discarded_candidates_with_reasons() {
        // §15.12.2: ninguna sobrecarga de `f` aplica a `f("h")` — la de un `int` no convierte el
        // `String`, la de dos difiere en aridad. El error lista **ambos** candidatos con su motivo.
        let errs = check(
            "class C { int f(int a) { return a; } int f(int a, int b) { return a + b; }
                       void m() { int x = f(\"h\"); } }",
        );
        assert_eq!(errs.len(), 1, "{errs:?}");
        assert!(errs[0].message.contains("`f(String)`"), "{}", errs[0].message);
        assert!(
            errs[0].notes.iter().any(|n| n.contains("f(int) no es aplicable")
                && n.contains("String no se convierte a int")),
            "{:?}",
            errs[0].notes
        );
        assert!(
            errs[0].notes.iter().any(|n| n.contains("f(int, int) no es aplicable")
                && n.contains("difieren en longitud")),
            "{:?}",
            errs[0].notes
        );
    }

    #[test]
    fn a_misspelled_name_suggests_the_closest_one_in_scope() {
        // did-you-mean sobre un método del receptor (`lenght` → `length` de String).
        let m = check("class C { void m(String s) { int y = s.lenght(); } }");
        assert!(
            m[0].notes.iter().any(|n| n == "¿quisiste decir 'length'?"),
            "{:?}",
            m[0].notes
        );
        // …y sobre un **campo** de la clase envolvente para un nombre pelado (`valorr` → `valor`).
        let f = check("class C { int valor = 3; int m() { return valorr; } }");
        assert!(
            f[0].notes.iter().any(|n| n == "¿quisiste decir 'valor'?"),
            "{:?}",
            f[0].notes
        );
        // Un nombre sin nada parecido no sugiere (no hay falso positivo).
        let none = check("class C { int valor = 3; int m() { return xyzzy; } }");
        assert!(
            !none[0].notes.iter().any(|n| n.starts_with("¿quisiste decir")),
            "{:?}",
            none[0].notes
        );
    }

    #[test]
    fn a_method_inherited_through_a_generic_superinterface_resolves() {
        // Finding #15: `apply` se declara en `BiFunction<T,U,R>` y se hereda vía
        // `BinaryOperator<T> extends BiFunction<T,T,T>`. Al buscar el miembro sobre un
        // `BinaryOperator<Integer>` hay que caminar el grafo de supertipos y sustituir los
        // argumentos de tipo de la superinterfaz (`<T,U,R>` → `<Integer,Integer,Integer>`),
        // así `apply(a, b)` devuelve `Integer`. Antes: "no se encuentra el método: apply".
        let ok = check(
            "interface BiFunction<T, U, R> { R apply(T t, U u); }
             interface BinaryOperator<T> extends BiFunction<T, T, T> {}
             class C { Integer reduce(BinaryOperator<Integer> op, Integer a, Integer b) {
                           return op.apply(a, b); } }",
        );
        assert!(ok.is_empty(), "{ok:?}");
    }

    #[test]
    fn a_generic_overload_with_unsatisfiable_inference_is_not_applicable() {
        // §18.5.1: `m(T, Lst<T>)` NO es aplicable a `(Lst<String>, Lst<String>)` —el 1º arg fija
        // `T = Lst<String>` (por `T`), el 2º pide `Lst<String>` contra `Lst<T>` (⇒ `T = String`), y no
        // hay `T` que cumpla los dos—. Solo `m(T, T)` aplica (T = Lst<String>) y devuelve `String`, que
        // no cabe en `int` → error. javac reporta lo mismo; antes lo aceptabamos (indulgencia).
        let errs = check(
            "class Lst<E> {}
             class C { <T> String m(T a, T b) { return null; }
                       <T> int m(T a, Lst<T> b) { return 0; }
                       int use(Lst<String> x, Lst<String> y) { return m(x, y); } }",
        );
        assert!(!errs.is_empty(), "deberia rechazar: {errs:?}");
    }

    #[test]
    fn a_generic_overload_that_does_apply_is_still_chosen() {
        // Contraprueba: `m(Lst<T>)` (mas especifico) sigue aplicando sobre `Lst<String>` y se elige.
        let errs = check(
            "class Lst<E> {}
             class C { <T> String m(T x) { return null; }
                       <T> int m(Lst<T> x) { return 0; }
                       int use(Lst<String> l) { return m(l); } }",
        );
        assert!(errs.is_empty(), "{errs:?}");
    }

    #[test]
    fn a_nested_generic_call_infers_from_the_outer_context() {
        // §18.5.2.1: `pick(empty(), strs())` — `empty()` (`<E> Box<E>`) toma su `E` del **contexto**
        // (el otro argumento es `Box<String>`), así que la llamada es `Box<String>` y asigna sin error.
        // Aislada, `empty()` daba `Box<Object>` y la llamada se rechazaba por igualdades incompatibles.
        let ok = check(
            "class Box<E> {}
             class C { <E> Box<E> empty() { return null; }
                       <T> Box<T> pick(Box<T> a, Box<T> b) { return null; }
                       Box<String> strs() { return null; }
                       Box<String> m() { return pick(empty(), strs()); } }",
        );
        assert!(ok.is_empty(), "{ok:?}");
    }

    #[test]
    fn infers_the_type_argument_of_a_generic_call() {
        // `<T> T id(T x)` con `"hola"` ⇒ `T = String`, así que la llamada **es** un String.
        let ok = check("class C { static <T> T id(T x) { return x; } void m() { String s = id(\"hola\"); } }");
        assert!(ok.is_empty(), "{ok:?}");

        let bad = check("class C { static <T> T id(T x) { return x; } void m() { Integer n = id(\"hola\"); } }");
        assert_eq!(bad.len(), 1, "id(\"hola\") es String, no Integer: {bad:?}");
    }

    // ---- overload resolution en 3 fases (JLS §15.12.2) ----

    /// Un [`RType`] como texto legible, para asertar **qué firma** se eligió.
    fn rt_name(table: &SymbolTable, t: &RType) -> String {
        match t {
            RType::Prim(p) => format!("{p:?}").to_lowercase(),
            RType::Void => "void".to_string(),
            RType::Class(id) | RType::TypeVar(id) => table.symbol(*id).name.clone(),
            RType::Parameterized { base, args } => {
                let a: Vec<String> = args
                    .iter()
                    .map(|x| match x {
                        crate::javac::symbol::RTypeArg::Type(t) => rt_name(table, t),
                        crate::javac::symbol::RTypeArg::Wildcard => "?".to_string(),
                        crate::javac::symbol::RTypeArg::Extends(t) => format!("? extends {}", rt_name(table, t)),
                        crate::javac::symbol::RTypeArg::Super(t) => format!("? super {}", rt_name(table, t)),
                    })
                    .collect();
                format!("{}<{}>", table.symbol(*base).name, a.join(","))
            }
            RType::Array(e) => format!("{}[]", rt_name(table, e)),
            RType::Capture { upper, .. } => format!("capture of {}", rt_name(table, upper)),
            RType::Intersection(ms) => ms.iter().map(|m| rt_name(table, m)).collect::<Vec<_>>().join("&"),
            RType::InferVar(id) => format!("infer#{id}"),
            RType::Unresolved => "?".to_string(),
        }
    }

    /// Corre el front-end sobre `src` y devuelve la **firma del método elegido** por la primera
    /// sentencia del método `m` (que debe ser una llamada), p. ej. `"(long)"`.
    fn picked(src: &str) -> String {
        let mut unit = parse(tokenize(src).unwrap()).0;
        let (table, _e) = enter(&unit);
        let errs = attribute(&mut unit, &table);
        assert!(errs.is_empty(), "no se esperaban errores: {errs:?}");
        let m = unit.types[0]
            .members
            .iter()
            .find_map(|mem| match mem {
                Member::Method(m) if m.name == "m" => Some(m),
                _ => None,
            })
            .expect("esperaba un método `m`");
        let StmtKind::Expr(call) = &m.body.as_ref().unwrap().0[0].kind else {
            panic!("esperaba una llamada como primera sentencia de `m`")
        };
        let Some(Binding::Method(id)) = call.binding else {
            panic!("la llamada quedó sin binding: {:?}", call.binding)
        };
        let (params, varargs) = signature_of(&table, id).expect("firma resuelta");
        let mut ps: Vec<String> = params.iter().map(|p| rt_name(&table, p)).collect();
        if varargs {
            if let Some(last) = ps.last_mut() {
                *last = format!("{}...", last.trim_end_matches("[]"));
            }
        }
        format!("({})", ps.join(","))
    }

    #[test]
    fn phase_1_prefers_widening_over_boxing_and_varargs() {
        // El caso canónico: con las tres sobrecargas presentes, gana `f(long)` — `int → long` es
        // widening (fase 1), y ahí se corta: boxing y varargs ni se miran.
        let src = "class C {
            static void f(long x) {}
            static void f(Integer x) {}
            static void f(int... x) {}
            static void m() { f(1); }
        }";
        assert_eq!(picked(src), "(long)");
    }

    #[test]
    fn phase_2_boxes_only_when_phase_1_finds_nothing() {
        // Sin `f(long)`, la fase 1 queda vacía → la 2 admite boxing `int → Integer`.
        let src = "class C {
            static void f(Integer x) {}
            static void f(int... x) {}
            static void m() { f(1); }
        }";
        assert_eq!(picked(src), "(Integer)");
    }

    #[test]
    fn phase_3_varargs_is_the_last_resort() {
        // Solo cuando ni la 1 ni la 2 encuentran nada se llega a la aridad variable.
        let src = "class C {
            static void f(int... x) {}
            static void m() { f(1); }
        }";
        assert_eq!(picked(src), "(int...)");
    }

    #[test]
    fn boxing_then_widening_reference_reaches_object() {
        // `int → Integer → Object`: boxing + widening de referencia, todo en la fase 2. Requiere
        // la jerarquía real del wrapper (por eso los cargamos del classpath).
        let src = "class C {
            static void f(Object x) {}
            static void f(int... x) {}
            static void m() { f(1); }
        }";
        assert_eq!(picked(src), "(Object)", "la fase 2 gana antes de llegar a varargs");
    }

    #[test]
    fn boxing_reaches_an_intermediate_supertype() {
        // `int → Integer → Number`, que solo resuelve si `Integer` se cargó con su superclase.
        let src = "class C {
            static void f(Number x) {}
            static void m() { f(1); }
        }";
        assert_eq!(picked(src), "(Number)");
    }

    #[test]
    fn chooses_the_most_specific_overload() {
        // Ambas aplicables en la fase 1; gana la más específica (§15.12.2.5).
        let src = "class C {
            static void f(Object x) {}
            static void f(String x) {}
            static void m() { f(\"hola\"); }
        }";
        assert_eq!(picked(src), "(String)");
    }

    #[test]
    fn exact_match_beats_widening() {
        let src = "class C {
            static void f(long x) {}
            static void f(int x) {}
            static void m() { f(1); }
        }";
        assert_eq!(picked(src), "(int)");
    }

    #[test]
    fn overloads_by_arity() {
        let src = "class C {
            static void f(int a) {}
            static void f(int a, int b) {}
            static void m() { f(1, 2); }
        }";
        assert_eq!(picked(src), "(int,int)");
    }

    #[test]
    fn varargs_accepts_several_arguments() {
        let src = "class C {
            static void f(String s, int... xs) {}
            static void m() { f(\"a\", 1, 2, 3); }
        }";
        assert_eq!(picked(src), "(String,int...)");
    }

    #[test]
    fn varargs_accepts_zero_variable_arguments() {
        let src = "class C {
            static void f(String s, int... xs) {}
            static void m() { f(\"a\"); }
        }";
        assert_eq!(picked(src), "(String,int...)");
    }

    #[test]
    fn flags_ambiguous_call() {
        // Ninguna es más específica que la otra: `Integer`/`Long` no se relacionan por subtipado,
        // y ambas son aplicables por unboxing en la fase 2.
        let errs = check(
            "class C {
                static void f(Integer a, long b) {}
                static void f(long a, Integer b) {}
                static void m() { Integer i = 1; f(i, i); }
            }",
        );
        assert!(errs.iter().any(|e| e.message.contains("ambigua")), "{errs:?}");
    }

    #[test]
    fn flags_call_with_no_applicable_overload() {
        let errs = check("class C { static void f(String s) {} static void m() { f(true); } }");
        assert_eq!(errs.len(), 1);
        assert!(errs[0].message.contains("aplicable"), "{errs:?}");
    }

    #[test]
    fn resolves_overload_inherited_from_the_superclass() {
        let errs = check(
            "class P { void f(long x) {} }
             class C extends P { void f(String s) {} void m() { f(1); } }",
        );
        assert!(errs.is_empty(), "`f(1)` debe resolver al `f(long)` heredado: {errs:?}");
    }

    #[test]
    fn errors_point_at_the_expression_not_the_method() {
        // El `z` indefinido está en la línea 3: el error debe ubicarse ahí, no en la firma.
        let errs = check("class C {\n  void m() {\n    int a = z;\n  }\n}");
        assert_eq!(errs.len(), 1);
        assert_eq!(errs[0].line, 3, "el error va en la línea de la expresión");
        assert_eq!(errs[0].col, 13, "y en la columna de `z`");
    }

    // ---- `this`/`super` en contexto estático (§8.4.3.2) ----

    #[test]
    fn this_in_a_static_method_is_rejected() {
        let errs = check("class C { static int s; static void m() { int x = this.s; } }");
        assert!(errs.iter().any(|e| e.message.contains("contexto estático")), "{errs:?}");
    }

    #[test]
    fn super_in_a_static_method_is_rejected() {
        let errs = check("class C { static Object m() { return super.toString(); } }");
        assert!(errs.iter().any(|e| e.message.contains("contexto estático")), "{errs:?}");
    }

    #[test]
    fn this_in_an_instance_method_is_fine() {
        let errs = check("class C { int inst; int m() { return this.inst; } }");
        assert!(!errs.iter().any(|e| e.message.contains("contexto estático")), "{errs:?}");
    }

    // ---- asignar un campo `final` (§8.3.1.2) ----

    #[test]
    fn reassigning_a_final_field_in_a_method_is_rejected() {
        let errs = check("class C { final int x = 1; void m() { this.x = 2; } }");
        assert!(errs.iter().any(|e| e.message.contains("final")), "{errs:?}");
    }

    #[test]
    fn assigning_a_blank_final_in_a_constructor_is_fine() {
        let errs = check("class C { final int x; C() { this.x = 1; } }");
        assert!(!errs.iter().any(|e| e.message.contains("final")), "{errs:?}");
    }

    #[test]
    fn assigning_a_non_final_field_in_a_method_is_fine() {
        let errs = check("class C { int x; void m() { this.x = 2; } }");
        assert!(errs.is_empty(), "{errs:?}");
    }

    // ---- SAM / interfaces funcionales (§9.8) ----

    #[test]
    fn detects_the_sam_of_functional_interfaces() {
        // Nombrarlas en firmas las carga (con sus flags de acceso).
        let src = "class C { Runnable r; java.util.function.Function<Integer,Integer> f; \
                   java.util.Comparator<String> cmp; CharSequence cs; }";
        let unit = parse(tokenize(src).unwrap()).0;
        let (table, _e) = enter(&unit);
        let sam_name = |iface: &str| {
            let cid = table.external(iface).unwrap_or_else(|| panic!("no cargó {iface}"));
            super::functional_sam(&table, cid).map(|m| table.symbol(m).name.clone())
        };
        assert_eq!(sam_name("Runnable").as_deref(), Some("run"));
        assert_eq!(sam_name("Function").as_deref(), Some("apply"));
        // `Comparator` redeclara `equals` (excluido por §9.8) y trae `default`s: sigue siendo funcional.
        assert_eq!(sam_name("Comparator").as_deref(), Some("compare"));
        // `CharSequence` tiene varios métodos abstractos: **no** es funcional.
        assert_eq!(sam_name("CharSequence"), None);
    }

    // ---- tipado de lambdas contra la interfaz funcional (§15.27.3) ----

    #[test]
    fn a_well_typed_lambda_binds_params_and_checks_the_body() {
        let errs = check(
            "class C { interface IntFn { int apply(int x); } \
             void m() { IntFn f = x -> x + 1; } }",
        );
        assert!(errs.is_empty(), "{errs:?}");
    }

    #[test]
    fn a_lambda_with_the_wrong_arity_is_rejected() {
        let errs = check(
            "class C { interface IntFn { int apply(int x); } \
             void m() { IntFn f = () -> 0; } }",
        );
        assert!(errs.iter().any(|e| e.message.contains("parámetro")), "{errs:?}");
    }

    #[test]
    fn a_lambda_body_incompatible_with_the_sam_return_is_rejected() {
        // `get()` devuelve `String`; la lambda devuelve un `int`.
        let errs = check(
            "class C { interface Sup { String get(); } \
             void m() { Sup s = () -> 42; } }",
        );
        assert!(errs.iter().any(|e| e.message.contains("no es compatible")), "{errs:?}");
    }

    #[test]
    fn a_lambda_param_is_typed_from_the_sam() {
        // `s` se liga a `String` (el param del SAM), así que `s.length()` resuelve.
        let errs = check(
            "class C { interface Fn { int f(String s); } \
             void m() { Fn g = s -> s.length(); } }",
        );
        assert!(errs.is_empty(), "{errs:?}");
    }

    #[test]
    fn a_lambda_against_a_stdlib_functional_interface() {
        // `Function` se carga **bajo demanda** (escaneo del cuerpo); `x` se liga a `Integer`, con
        // unboxing en `x + 1` y boxing del `int` al retorno `Integer`.
        let errs = check(
            "import java.util.function.Function; \
             class C { void m() { Function<Integer,Integer> f = x -> x + 1; } }",
        );
        assert!(errs.is_empty(), "{errs:?}");
    }

    #[test]
    fn a_stdlib_lambda_still_catches_a_bad_return() {
        let errs = check(
            "import java.util.function.Function; \
             class C { void m() { Function<Integer,String> f = x -> x + 1; } }",
        );
        assert!(errs.iter().any(|e| e.message.contains("no es compatible")), "{errs:?}");
    }

    // ---- indulgencia con externos, acotada (§6.5 / carga transitiva) ----

    #[test]
    fn a_genuine_miss_on_an_external_is_now_reported() {
        // `String` tiene su jerarquía **completa** (hasta `Object`): un método inexistente ya no se
        // acepta en silencio.
        let errs = check("class B { int m() { return \"x\".noExiste(); } }");
        assert!(errs.iter().any(|e| e.message.contains("noExiste")), "{errs:?}");
    }

    #[test]
    fn a_real_external_method_still_resolves() {
        let errs = check("class B { int m() { return \"x\".length(); } }");
        assert!(errs.is_empty(), "{errs:?}");
    }

    #[test]
    fn an_inherited_external_method_still_resolves() {
        // `hashCode`/`toString` son de `Object`: se llega por la cadena de supertipos, que ahora se
        // carga transitivamente.
        let errs = check("class B { int a() { return \"x\".hashCode(); } String b() { return \"x\".toString(); } }");
        assert!(errs.is_empty(), "{errs:?}");
    }

    // ---- tipo anidado cualificado (§6.5.5) ----

    #[test]
    fn a_qualified_nested_type_access_resolves() {
        // `Outer.Inner.v()` — antes fallaba buscando un campo `Inner`; ahora lo lee como tipo.
        let errs = check(
            "class Outer { static class Inner { static int v() { return 5; } } \
             int use() { return Outer.Inner.v(); } }",
        );
        assert!(errs.is_empty(), "{errs:?}");
    }
}
