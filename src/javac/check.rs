//! La pasada **Check**: bien-formación de las *declaraciones*, no de las expresiones. Es el
//! `Check.java` de javac, y corre después de Attribute — necesita la tabla ya resuelta para poder
//! comparar firmas a través de la jerarquía.
//!
//! Attribute pregunta «¿este *uso* tiene sentido?»; Check pregunta «¿esta *declaración* es legal
//! dado lo que hay arriba de ella?». Son preguntas distintas y por eso viven aparte.
//!
//! ## Qué chequea
//!
//! **Override** (§8.4.8): cuando un método tiene la misma firma que uno heredado —mismo nombre y
//! mismos tipos de parámetro **borrados** (§8.4.2)— tiene que respetar cuatro reglas:
//!
//! - El de arriba **no** puede ser `final` (§8.4.3.3).
//! - No se cambia de «lado»: un método de instancia no sobrescribe uno `static` ni al revés
//!   (§8.4.8.2 — eso sería *hiding*, y con firmas iguales es ilegal).
//! - No se **reduce** la visibilidad (§8.4.8.3): `public` → `protected` → *package* → `private`
//!   solo se recorre hacia arriba.
//! - El retorno es **covariante** (§8.4.8.3): idéntico para primitivos y `void`, y un subtipo para
//!   referencias — `Object f()` puede volverse `String f()`, no al revés.
//!
//! **Métodos abstractos sin implementar** (§8.1.1.1): una clase concreta tiene que tener un cuerpo
//! para cada método `abstract` que hereda.
//!
//! **Excepciones chequeadas** (§11.2): en el cuerpo de un método, toda excepción **chequeada** (un
//! `Throwable` que no es `RuntimeException` ni `Error`) que se pueda lanzar tiene que estar
//! **capturada** por un `try` que la encierre o **declarada** en el `throws`. Se recorre el cuerpo
//! llevando el conjunto *handled* (los `throws` declarados + los `catch` de cada `try` que se
//! atraviesa) y se contrasta contra cada `throw` y cada llamada (que propaga el `throws` de su
//! callee). Los tipos externos cuya jerarquía no cargó del todo se tratan como **no chequeados** —
//! misma indulgencia—. Requirió retener la cláusula `throws` en el parser y en la tabla de símbolos,
//! que antes se descartaba (`skip_throws`).
//!
//! **Control de acceso** (§6.6): cada **uso** de un miembro (campo o método) tiene que ser accesible
//! desde donde se lo usa. Se recorre el cuerpo de cada método/inicializador mirando el `binding` que
//! dejó Attribute: `public` siempre; `private` solo dentro del mismo **tipo top-level** (así una
//! anidada ve los privados de la que la encierra); `protected` en el mismo paquete o en una subclase;
//! sin modificador, en el mismo paquete. Corre sobre el árbol **fuente**, antes del desugar, a
//! propósito: el código sintético invoca cosas que no serían escribibles a mano (el constructor del
//! `enum` llama a `Enum.<init>`, `protected`). Los tipos **externos** se eximen (flags no confiables).
//!
//! ## Qué no chequea (y por qué)
//!
//! - **`@Override`** sobre un método que no sobrescribe nada: el parser **saltea** las anotaciones
//!   (no llegan al AST), así que no hay nada que mirar. Es la anotación la que hace falta, no la
//!   regla.
//! - **`throws`** más ancho que el heredado (§8.4.8.3): `MethodDecl` todavía no lleva la cláusula.
//! - Las clases con algún supertipo **externo** quedan **exentas** del chequeo de abstractos: de un
//!   tipo de afuera solo modelamos los miembros que aparecen en alguna firma, así que un
//!   `class Mia implements List<String>` reportaría como «sin implementar» decenas de métodos que
//!   sí están. Preferimos no chequear a mentir. La regla de override, en cambio, **sí** corre
//!   contra externos: ahí lo que hay es una firma concreta que comparar, no una ausencia que
//!   interpretar.

use super::ast::*;
use super::symbol::{RType, Resolved, SymbolId, SymbolKind, SymbolTable};
use super::types;
use super::Error;

/// Corre los chequeos de declaración sobre la unidad ya atribuida.
pub fn check(unit: &CompilationUnit, table: &SymbolTable) -> Vec<Error> {
    let mut cx = Checker { table, errors: Vec::new() };
    let base = unit.package.as_deref().unwrap_or("");
    if let Some(module) = &unit.module {
        cx.module_decl(module);
    }
    for class in &unit.types {
        cx.class(class, base);
    }
    cx.errors
}

struct Checker<'a> {
    table: &'a SymbolTable,
    errors: Vec<Error>,
}

/// La firma **borrada** de un método: lo que decide si dos métodos son *override-equivalentes*
/// (§8.4.2). Los argumentos de tipo no cuentan — `f(List<String>)` y `f(List<Integer>)` chocan.
fn erased_params(table: &SymbolTable, m: SymbolId) -> Option<Vec<RType>> {
    match table.resolved(m) {
        Some(Resolved::Method { params, .. }) => {
            Some(params.iter().map(|p| types::erasure(table, p)).collect())
        }
        _ => None,
    }
}

/// Los parámetros del método `m` **como se los ve desde una subclase** (§8.4.8.1): se aplica la
/// sustitución `subst` (los args de tipo del supertipo) y **después** se borra. Es lo que hace que un
/// `compareTo(T)` heredado de `Comparable<Foo>` matchee con `compareTo(Foo)` en la subclase.
fn erased_params_as_seen(
    table: &SymbolTable,
    m: SymbolId,
    subst: &types::Subst,
) -> Option<Vec<RType>> {
    match table.resolved(m) {
        Some(Resolved::Method { params, .. }) => Some(
            params
                .iter()
                .map(|p| types::erasure(table, &types::substitute(p, subst)))
                .collect(),
        ),
        _ => None,
    }
}

fn ret_of(table: &SymbolTable, m: SymbolId) -> Option<RType> {
    match table.resolved(m) {
        Some(Resolved::Method { ret, .. }) => Some(ret.clone()),
        _ => None,
    }
}

fn is_static(table: &SymbolTable, m: SymbolId) -> bool {
    table.symbol(m).modifiers.contains(&Modifier::Static)
}

/// ¿El método lleva la anotación `@Override` (§9.6.4.4)? Se compara por nombre **simple**.
fn has_override(annotations: &[Annotation]) -> bool {
    annotations.iter().any(|a| a.name == "Override")
}

/// ¿El nombre corresponde a un método **sobrescribible de `Object`**? Se usa para no reportar un
/// `@Override` sobre `toString`/`equals`/`hashCode`/… como si no sobrescribiera nada cuando el único
/// ancestro externo es `Object` (cuyos miembros no siempre están todos cargados).
fn is_object_method(name: &str) -> bool {
    matches!(name, "toString" | "hashCode" | "equals" | "clone" | "finalize")
}

/// El nivel de acceso, ordenado de menos a más visible — así «reducir» es simplemente bajar.
fn access_level(mods: &[Modifier]) -> u8 {
    if mods.contains(&Modifier::Private) {
        0
    } else if mods.contains(&Modifier::Protected) {
        2
    } else if mods.contains(&Modifier::Public) {
        3
    } else {
        1 // sin modificador: acceso de paquete
    }
}

/// El **tipo top-level** que encierra a un *binary name*: se corta en el primer `$` (el *nesting*).
/// `com.foo.Outer$Inner` → `com.foo.Outer`; `Outer` → `Outer`. Es la unidad del acceso `private`.
fn top_level_of(binary: &str) -> &str {
    match binary.find('$') {
        Some(i) => &binary[..i],
        None => binary,
    }
}

/// El **paquete** de un *binary name* (lo previo al nombre del tipo top-level). Vacío = paquete sin
/// nombre.
fn package_of(binary: &str) -> &str {
    let top = top_level_of(binary);
    match top.rfind('.') {
        Some(i) => &top[..i],
        None => "",
    }
}

fn access_name(level: u8) -> &'static str {
    match level {
        0 => "private",
        2 => "protected",
        3 => "public",
        _ => "de paquete",
    }
}

impl Checker<'_> {
    fn error(&mut self, pos: Pos, message: String) {
        self.errors.push(Error::new(message, pos.line, pos.col));
    }

    /// Buena formación de una declaración de **módulo** (§7.7): sin directivas **duplicadas** para el
    /// mismo módulo/paquete/servicio, y sin auto-`requires`. La resolución del grafo (que los módulos
    /// requeridos existan y sean legibles) es de tiempo de ejecución (JPMS), sesión aparte.
    fn module_decl(&mut self, m: &ModuleDecl) {
        use std::collections::HashSet;
        let pos = m.pos;
        let (mut req, mut exp, mut opn, mut uses, mut prov) = (
            HashSet::new(),
            HashSet::new(),
            HashSet::new(),
            HashSet::new(),
            HashSet::new(),
        );
        for d in &m.directives {
            match d {
                ModuleDirective::Requires { name, .. } => {
                    if *name == m.name {
                        self.error(pos, format!("el módulo `{name}` no puede requerirse a sí mismo"));
                    }
                    if !req.insert(name) {
                        self.error(pos, format!("`requires` duplicado del módulo `{name}`"));
                    }
                }
                ModuleDirective::Exports { package, .. } => {
                    if !exp.insert(package) {
                        self.error(pos, format!("`exports` duplicado del paquete `{package}`"));
                    }
                }
                ModuleDirective::Opens { package, .. } => {
                    if !opn.insert(package) {
                        self.error(pos, format!("`opens` duplicado del paquete `{package}`"));
                    }
                }
                ModuleDirective::Uses { service } => {
                    if !uses.insert(service) {
                        self.error(pos, format!("`uses` duplicado del servicio `{service}`"));
                    }
                }
                ModuleDirective::Provides { service, with } => {
                    if !prov.insert(service) {
                        self.error(pos, format!("`provides` duplicado del servicio `{service}`"));
                    }
                    let mut seen = HashSet::new();
                    for imp in with {
                        if !seen.insert(imp) {
                            self.error(pos, format!("implementación repetida en `provides`: `{imp}`"));
                        }
                    }
                }
            }
        }
    }

    fn class(&mut self, class: &ClassDecl, enclosing: &str) {
        let fqn =
            if enclosing.is_empty() { class.name.clone() } else { format!("{enclosing}.{}", class.name) };
        if let Some(cid) = self.table.class(&fqn) {
            // Los métodos sobrecargados comparten nombre: para saber **cuál** símbolo es cada
            // declaración se las cuenta por (nombre, aridad) en orden — el mismo en que `enter` los
            // registró.
            let mut rank: Vec<(String, usize)> = Vec::new();
            for member in &class.members {
                let Member::Method(m) = member else { continue };
                if m.is_constructor {
                    continue;
                }
                let key = (m.name.clone(), m.params.len());
                let nth = rank.iter().filter(|k| **k == key).count();
                rank.push(key);
                self.method(m, cid, nth);
            }
            self.abstracts(class, cid);
            self.sealed_rules(class, cid);
            // Control de acceso (§6.6): cada **uso** de un miembro tiene que ser accesible desde
            // esta clase. Se recorre el cuerpo de cada método/inicializador y se mira el binding que
            // dejó Attribute. Corre sobre el árbol **fuente** (antes del desugar) a propósito: el
            // código sintético llama a cosas que las reglas de acceso no permitirían escribir a mano
            // (el constructor del `enum` invoca `Enum.<init>`, que es `protected`).
            for member in &class.members {
                match member {
                    Member::Method(m) => {
                        if let Some(b) = &m.body {
                            self.walk_block(cid, b);
                        }
                    }
                    Member::Field(f) => {
                        if let Some(e) = &f.init {
                            self.walk_expr(cid, e);
                        }
                    }
                    Member::StaticInit(b) | Member::InstanceInit(b) => self.walk_block(cid, b),
                    Member::Type(_) => {}
                }
            }
            // Excepciones chequeadas (§11.2): en el cuerpo de cada método/constructor, toda
            // excepción chequeada que se pueda lanzar tiene que estar **capturada** por un `try`
            // que la encierre o **declarada** en el `throws`. También sobre el árbol fuente.
            for member in &class.members {
                if let Member::Method(m) = member {
                    if let Some(body) = &m.body {
                        let declared: Vec<RType> =
                            m.throws.iter().filter_map(|t| self.resolve_exc(cid, t)).collect();
                        self.exc_block(cid, &declared, body);
                    }
                }
            }
        }
        for member in &class.members {
            if let Member::Type(nested) = member {
                self.class(nested, &fqn);
            }
        }
    }

    // ---- excepciones chequeadas (§11.2) ----

    /// Una excepción es **chequeada** si es un `Throwable` que no es `RuntimeException` ni `Error`
    /// (§11.1.1). Si su jerarquía no se puede resolver —un tipo externo que no cargó del todo— se es
    /// **indulgente** (no chequeada): mejor no inventar un error que forzar uno dudoso.
    fn is_checked(&self, e: &RType) -> bool {
        if types::erased_id(e).is_none() {
            return false;
        }
        let sub = |name: &str| {
            self.table
                .external(name)
                .is_some_and(|id| types::is_subtype(self.table, e, &RType::Class(id)))
        };
        sub("Throwable") && !sub("RuntimeException") && !sub("Error")
    }

    /// Resuelve un tipo de excepción (de un `throws`/`catch`) a su [`RType`]. `None` si no nombra un
    /// tipo resoluble.
    fn resolve_exc(&self, cid: SymbolId, ty: &Type) -> Option<RType> {
        let name = match ty {
            Type::Class(n) | Type::Parameterized { base: n, .. } => n,
            _ => return None,
        };
        let scope = match &self.table.symbol(cid).kind {
            SymbolKind::Class { members, .. } => *members,
            _ => return None,
        };
        let id = self.table.resolve_type(scope, name).or_else(|| self.table.external(name))?;
        Some(RType::Class(id))
    }

    /// Chequea una excepción `thrown` lanzada en `pos`: error si es **chequeada** y ningún tipo de
    /// `handled` (los `throws` declarados + los `catch` que la encierran) la cubre.
    fn check_thrown(&mut self, handled: &[RType], thrown: &RType, pos: Pos) {
        if !self.is_checked(thrown) {
            return;
        }
        if handled.iter().any(|h| types::is_subtype(self.table, thrown, h)) {
            return;
        }
        let name = self.table.symbol(types::erased_id(thrown).unwrap()).name.clone();
        self.error(pos, format!("excepción chequeada `{name}` sin capturar ni declarar en `throws`"));
    }

    fn exc_block(&mut self, cid: SymbolId, handled: &[RType], block: &Block) {
        for s in &block.0 {
            self.exc_stmt(cid, handled, s);
        }
    }

    fn exc_stmt(&mut self, cid: SymbolId, handled: &[RType], s: &Stmt) {
        match &s.kind {
            StmtKind::Throw(e) => {
                self.exc_expr(cid, handled, e);
                if let Some(t) = &e.ty {
                    self.check_thrown(handled, t, e.pos);
                }
            }
            StmtKind::Try { resources, body, catches, finally } => {
                // Dentro del `try`, lo que atrapan los `catch` pasa a estar **manejado**.
                let mut inner: Vec<RType> = handled.to_vec();
                for c in catches {
                    inner.extend(c.types.iter().filter_map(|t| self.resolve_exc(cid, t)));
                }
                resources.iter().for_each(|r| self.exc_stmt(cid, &inner, r));
                self.exc_block(cid, &inner, body);
                // Los `catch` y el `finally` corren **fuera** del alcance de esos handlers.
                for c in catches {
                    self.exc_block(cid, handled, &c.body);
                }
                if let Some(f) = finally {
                    self.exc_block(cid, handled, f);
                }
            }
            StmtKind::LocalVar { init, .. } => {
                if let Some(e) = init {
                    self.exc_expr(cid, handled, e);
                }
            }
            StmtKind::Return(e) => {
                if let Some(e) = e {
                    self.exc_expr(cid, handled, e);
                }
            }
            StmtKind::Expr(e) | StmtKind::Yield(e) => self.exc_expr(cid, handled, e),
            StmtKind::Block(b) => self.exc_block(cid, handled, b),
            StmtKind::If { cond, then, els } => {
                self.exc_expr(cid, handled, cond);
                self.exc_stmt(cid, handled, then);
                if let Some(e) = els {
                    self.exc_stmt(cid, handled, e);
                }
            }
            StmtKind::While { cond, body } | StmtKind::Do { body, cond } => {
                self.exc_expr(cid, handled, cond);
                self.exc_stmt(cid, handled, body);
            }
            StmtKind::For { init, cond, update, body } => {
                if let Some(i) = init {
                    self.exc_stmt(cid, handled, i);
                }
                if let Some(c) = cond {
                    self.exc_expr(cid, handled, c);
                }
                update.iter().for_each(|u| self.exc_expr(cid, handled, u));
                self.exc_stmt(cid, handled, body);
            }
            StmtKind::ForEach { iterable, body, .. } => {
                self.exc_expr(cid, handled, iterable);
                self.exc_stmt(cid, handled, body);
            }
            StmtKind::Synchronized { lock, body } => {
                self.exc_expr(cid, handled, lock);
                self.exc_block(cid, handled, body);
            }
            StmtKind::Switch { selector, cases } => {
                self.exc_expr(cid, handled, selector);
                for c in cases {
                    if let Some(g) = &c.guard {
                        self.exc_expr(cid, handled, g);
                    }
                    match &c.body {
                        SwitchBody::Arrow(st) => self.exc_stmt(cid, handled, st),
                        SwitchBody::Colon(ss) => ss.iter().for_each(|s| self.exc_stmt(cid, handled, s)),
                    }
                }
            }
            StmtKind::Assert { cond, message } => {
                self.exc_expr(cid, handled, cond);
                if let Some(m) = message {
                    self.exc_expr(cid, handled, m);
                }
            }
            StmtKind::Labeled { body, .. } => self.exc_stmt(cid, handled, body),
            StmtKind::LocalClass(_)
            | StmtKind::Break(_)
            | StmtKind::Continue(_)
            | StmtKind::Empty => {}
        }
    }

    fn exc_expr(&mut self, cid: SymbolId, handled: &[RType], e: &Expr) {
        // Una llamada/`new` propaga lo que su método/constructor **declara** en su `throws`.
        if let Some(Binding::Method(callee)) = e.binding {
            if let Some(Resolved::Method { throws, .. }) = self.table.resolved(callee) {
                for t in throws.clone() {
                    self.check_thrown(handled, &t, e.pos);
                }
            }
        }
        match &e.kind {
            ExprKind::Binary { lhs, rhs, .. } => {
                self.exc_expr(cid, handled, lhs);
                self.exc_expr(cid, handled, rhs);
            }
            ExprKind::Unary { expr, .. } | ExprKind::Cast { expr, .. } | ExprKind::InstanceOf { expr, .. } => {
                self.exc_expr(cid, handled, expr)
            }
            ExprKind::Assign { target, value, .. } => {
                self.exc_expr(cid, handled, target);
                self.exc_expr(cid, handled, value);
            }
            ExprKind::Ternary { cond, then, els } => {
                self.exc_expr(cid, handled, cond);
                self.exc_expr(cid, handled, then);
                self.exc_expr(cid, handled, els);
            }
            ExprKind::Call { target, args, .. } => {
                if let Some(t) = target {
                    self.exc_expr(cid, handled, t);
                }
                args.iter().for_each(|a| self.exc_expr(cid, handled, a));
            }
            ExprKind::Field { expr, .. } => self.exc_expr(cid, handled, expr),
            ExprKind::Index { array, index } => {
                self.exc_expr(cid, handled, array);
                self.exc_expr(cid, handled, index);
            }
            ExprKind::NewObject { args, .. } => args.iter().for_each(|a| self.exc_expr(cid, handled, a)),
            ExprKind::NewArray { dims, init, .. } => {
                dims.iter().flatten().for_each(|d| self.exc_expr(cid, handled, d));
                if let Some(es) = init {
                    es.iter().for_each(|x| self.exc_expr(cid, handled, x));
                }
            }
            ExprKind::Switch { selector, cases } => {
                self.exc_expr(cid, handled, selector);
                for c in cases {
                    if let Some(g) = &c.guard {
                        self.exc_expr(cid, handled, g);
                    }
                    match &c.body {
                        SwitchBody::Arrow(st) => self.exc_stmt(cid, handled, st),
                        SwitchBody::Colon(ss) => ss.iter().for_each(|s| self.exc_stmt(cid, handled, s)),
                    }
                }
            }
            // El cuerpo de una lambda tiene su propio contexto de `throws` (el del SAM); se compila
            // aparte (barrera), así que acá no se atraviesa.
            ExprKind::Lambda { .. }
            | ExprKind::Name(_)
            | ExprKind::IntLit(_)
            | ExprKind::LongLit(_)
            | ExprKind::FloatLit(_)
            | ExprKind::DoubleLit(_)
            | ExprKind::CharLit(_)
            | ExprKind::StringLit(_)
            | ExprKind::BoolLit(_)
            | ExprKind::Null
            | ExprKind::This
            | ExprKind::QualifiedThis(_)
            | ExprKind::Super
            | ExprKind::ClassLit(_)
            | ExprKind::MethodRef { .. }
            // `Indy` lo produce el desugar, después de esta pasada: nunca llega acá.
            | ExprKind::Indy { .. }
            // Un nodo de error no lanza nada: ya se reportó su error de sintaxis.
            | ExprKind::Error => {}
        }
    }

    // ---- control de acceso (§6.6) ----

    /// Chequea que el miembro `member`, usado en `pos`, sea accesible desde la clase `from`.
    ///

    // ---- control de acceso (§6.6) ----

    /// Chequea que el miembro `member`, usado en `pos`, sea accesible desde la clase `from`.
    ///
    /// Los tipos **externos** se eximen: se cargan parciales y sus flags de acceso no son
    /// confiables (es la misma indulgencia que ya rige para ellos). La regla de `protected` usa la
    /// forma simple del §6.6.1 (mismo paquete **o** subclase); la restricción extra del §6.6.2
    /// —sobre el tipo del receptor— queda para después.
    fn check_access(&mut self, from: SymbolId, member: SymbolId, pos: Pos) {
        let sym = self.table.symbol(member);
        let Some(owner) = sym.owner else { return }; // un local/parámetro no tiene dueño-clase
        if !matches!(self.table.symbol(owner).kind, SymbolKind::Class { .. }) {
            return;
        }
        if self.is_external(owner) {
            return;
        }
        let mods = sym.modifiers.clone();
        if mods.contains(&Modifier::Public) {
            return;
        }
        let owner_bin = self.binary(owner);
        let from_bin = self.binary(from);
        let ok = if mods.contains(&Modifier::Private) {
            // `private`: solo dentro del mismo **tipo top-level** (§6.6.1) — así una anidada ve los
            // privados de la que la encierra, y viceversa.
            top_level_of(&owner_bin) == top_level_of(&from_bin)
        } else if mods.contains(&Modifier::Protected) {
            package_of(&owner_bin) == package_of(&from_bin) || self.is_subclass(from, owner)
        } else {
            // Acceso de paquete (sin modificador): mismo paquete.
            package_of(&owner_bin) == package_of(&from_bin)
        };
        if !ok {
            let word = access_name(access_level(&mods));
            let kind = if matches!(sym.kind, SymbolKind::Method { .. }) { "el método" } else { "el campo" };
            let msg = format!(
                "{kind} `{}` es `{word}` en `{}` y no es accesible desde `{}`",
                sym.name,
                self.table.symbol(owner).name,
                self.table.symbol(from).name
            );
            self.error(pos, msg);
        }
    }

    /// `from` **hereda** de `owner` (para la regla de `protected`).
    fn is_subclass(&self, from: SymbolId, owner: SymbolId) -> bool {
        self.ancestors(from).contains(&owner)
    }

    fn binary(&self, cid: SymbolId) -> String {
        match &self.table.symbol(cid).kind {
            SymbolKind::Class { binary, .. } => binary.clone(),
            _ => self.table.symbol(cid).name.clone(),
        }
    }

    fn walk_block(&mut self, from: SymbolId, block: &Block) {
        for s in &block.0 {
            self.walk_stmt(from, s);
        }
    }

    fn walk_stmt(&mut self, from: SymbolId, s: &Stmt) {
        match &s.kind {
            StmtKind::LocalVar { init, .. } => {
                if let Some(e) = init {
                    self.walk_expr(from, e);
                }
            }
            StmtKind::Return(e) => {
                if let Some(e) = e {
                    self.walk_expr(from, e);
                }
            }
            StmtKind::Expr(e) | StmtKind::Throw(e) | StmtKind::Yield(e) => self.walk_expr(from, e),
            StmtKind::Block(b) => self.walk_block(from, b),
            StmtKind::If { cond, then, els } => {
                self.walk_expr(from, cond);
                self.walk_stmt(from, then);
                if let Some(e) = els {
                    self.walk_stmt(from, e);
                }
            }
            StmtKind::While { cond, body } | StmtKind::Do { body, cond } => {
                self.walk_expr(from, cond);
                self.walk_stmt(from, body);
            }
            StmtKind::For { init, cond, update, body } => {
                if let Some(i) = init {
                    self.walk_stmt(from, i);
                }
                if let Some(c) = cond {
                    self.walk_expr(from, c);
                }
                update.iter().for_each(|u| self.walk_expr(from, u));
                self.walk_stmt(from, body);
            }
            StmtKind::ForEach { iterable, body, .. } => {
                self.walk_expr(from, iterable);
                self.walk_stmt(from, body);
            }
            StmtKind::Synchronized { lock, body } => {
                self.walk_expr(from, lock);
                self.walk_block(from, body);
            }
            StmtKind::Try { resources, body, catches, finally } => {
                resources.iter().for_each(|r| self.walk_stmt(from, r));
                self.walk_block(from, body);
                catches.iter().for_each(|c| self.walk_block(from, &c.body));
                if let Some(f) = finally {
                    self.walk_block(from, f);
                }
            }
            StmtKind::Switch { selector, cases } => {
                self.exhaustive_switch(from, selector, cases, false);
                self.walk_expr(from, selector);
                for c in cases {
                    if let Some(g) = &c.guard {
                        self.walk_expr(from, g);
                    }
                    match &c.body {
                        SwitchBody::Arrow(st) => self.walk_stmt(from, st),
                        SwitchBody::Colon(ss) => ss.iter().for_each(|s| self.walk_stmt(from, s)),
                    }
                }
            }
            StmtKind::Assert { cond, message } => {
                self.walk_expr(from, cond);
                if let Some(m) = message {
                    self.walk_expr(from, m);
                }
            }
            StmtKind::Labeled { body, .. } => self.walk_stmt(from, body),
            // Una clase local tiene su propio contexto de acceso; se compila aparte (barrera).
            StmtKind::LocalClass(_)
            | StmtKind::Break(_)
            | StmtKind::Continue(_)
            | StmtKind::Empty => {}
        }
    }

    fn walk_expr(&mut self, from: SymbolId, e: &Expr) {
        // El binding que dejó Attribute: si apunta a un miembro, se chequea acá.
        match e.binding {
            Some(Binding::Field(f)) => self.check_access(from, f, e.pos),
            Some(Binding::Method(m)) => self.check_access(from, m, e.pos),
            _ => {}
        }
        match &e.kind {
            ExprKind::Binary { lhs, rhs, .. } => {
                self.walk_expr(from, lhs);
                self.walk_expr(from, rhs);
            }
            ExprKind::Unary { expr, .. } | ExprKind::Cast { expr, .. } | ExprKind::InstanceOf { expr, .. } => {
                self.walk_expr(from, expr)
            }
            ExprKind::Assign { target, value, .. } => {
                self.walk_expr(from, target);
                self.walk_expr(from, value);
            }
            ExprKind::Ternary { cond, then, els } => {
                self.walk_expr(from, cond);
                self.walk_expr(from, then);
                self.walk_expr(from, els);
            }
            ExprKind::Call { target, args, .. } => {
                if let Some(t) = target {
                    self.walk_expr(from, t);
                }
                args.iter().for_each(|a| self.walk_expr(from, a));
            }
            ExprKind::Field { expr, .. } => self.walk_expr(from, expr),
            ExprKind::Index { array, index } => {
                self.walk_expr(from, array);
                self.walk_expr(from, index);
            }
            ExprKind::NewObject { args, .. } => args.iter().for_each(|a| self.walk_expr(from, a)),
            ExprKind::NewArray { dims, init, .. } => {
                dims.iter().flatten().for_each(|d| self.walk_expr(from, d));
                if let Some(es) = init {
                    es.iter().for_each(|x| self.walk_expr(from, x));
                }
            }
            ExprKind::Switch { selector, cases } => {
                self.exhaustive_switch(from, selector, cases, true);
                self.walk_expr(from, selector);
                for c in cases {
                    if let Some(g) = &c.guard {
                        self.walk_expr(from, g);
                    }
                    if let SwitchBody::Arrow(st) = &c.body {
                        self.walk_stmt(from, st);
                    } else if let SwitchBody::Colon(ss) = &c.body {
                        ss.iter().for_each(|s| self.walk_stmt(from, s));
                    }
                }
            }
            ExprKind::Lambda { body, .. } => match body.as_ref() {
                LambdaBody::Expr(e) => self.walk_expr(from, e),
                LambdaBody::Block(b) => self.walk_block(from, b),
            },
            // Sin sub-expresiones con binding que chequear.
            ExprKind::Name(_)
            | ExprKind::IntLit(_)
            | ExprKind::LongLit(_)
            | ExprKind::FloatLit(_)
            | ExprKind::DoubleLit(_)
            | ExprKind::CharLit(_)
            | ExprKind::StringLit(_)
            | ExprKind::BoolLit(_)
            | ExprKind::Null
            | ExprKind::This
            | ExprKind::QualifiedThis(_)
            | ExprKind::Super
            | ExprKind::ClassLit(_)
            | ExprKind::MethodRef { .. }
            | ExprKind::Indy { .. }
            | ExprKind::Error => {}
        }
    }

    /// Los cuatro chequeos de §8.4.8 sobre un método que sobrescribe.
    fn method(&mut self, decl: &MethodDecl, cid: SymbolId, nth: usize) {
        let Some(mine) = self.find_own(cid, decl, nth) else { return };
        let Some(sig) = erased_params(self.table, mine) else { return };
        let parent = self.overridden(cid, &decl.name, &sig);

        // `@Override` (§9.6.4.4): la anotación exige que el método **sobrescriba o implemente** algo.
        // Reportar que no lo hace es **sound** solo si podemos descartar que sobrescriba un método
        // externo que no cargamos: se pide que los únicos ancestros externos sean `Object` —cuyo set de
        // métodos es fijo y conocido— y que el método no sea uno de los suyos (`toString`, `equals`…).
        // Con un supertipo del JDK (`extends ArrayList`) se calla, como el chequeo de abstractos.
        if parent.is_none() && has_override(&decl.annotations) {
            let externals_only_object = self
                .ancestors(cid)
                .iter()
                .filter(|&&a| self.is_external(a))
                .all(|&a| self.table.symbol(a).name == "Object");
            if externals_only_object && !is_object_method(&decl.name) {
                let msg =
                    format!("`{}` lleva `@Override` pero no sobrescribe ningún método", decl.name);
                self.error(decl.pos, msg);
            }
        }

        let Some(parent) = parent else { return };

        let owner = self.table.symbol(parent).owner;
        let owner_name = owner.map_or("?".to_string(), |o| self.table.symbol(o).name.clone());
        let parent_mods = self.table.symbol(parent).modifiers.clone();

        if parent_mods.contains(&Modifier::Final) {
            let msg = format!("`{}` es `final` en `{owner_name}`: no se puede sobrescribir", decl.name);
            self.error(decl.pos, msg);
        }

        // Un `static` y uno de instancia con la misma firma no se sobrescriben: colisionan.
        let (mine_static, parent_static) = (is_static(self.table, mine), is_static(self.table, parent));
        if mine_static != parent_static {
            let (a, b) = if mine_static { ("static", "de instancia") } else { ("de instancia", "static") };
            let msg = format!(
                "`{}` es {a} acá y {b} en `{owner_name}`: un método no puede sobrescribir uno del otro tipo",
                decl.name
            );
            self.error(decl.pos, msg);
        }

        let (mine_acc, parent_acc) = (access_level(&decl.modifiers), access_level(&parent_mods));
        if mine_acc < parent_acc {
            let msg = format!(
                "`{}` reduce la visibilidad heredada: es `{}` y en `{owner_name}` era `{}`",
                decl.name,
                access_name(mine_acc),
                access_name(parent_acc)
            );
            self.error(decl.pos, msg);
        }

        self.return_type(decl, mine, parent, &owner_name);
        self.throws_wider(decl, cid, parent, &owner_name);
    }

    /// Un override **no puede declarar `throws` más ancho** (§8.4.8.3): cada excepción **chequeada**
    /// que lanza tiene que ser subtipo de alguna que declare el método sobrescrito. Las no-chequeadas
    /// (`RuntimeException`/`Error`) no cuentan.
    fn throws_wider(&mut self, decl: &MethodDecl, cid: SymbolId, parent: SymbolId, owner: &str) {
        let allowed: Vec<RType> = match self.table.resolved(parent) {
            Some(Resolved::Method { throws, .. }) => throws.clone(),
            _ => return,
        };
        for ty in &decl.throws {
            let Some(t) = self.resolve_exc(cid, ty) else { continue };
            if !self.is_checked(&t) {
                continue;
            }
            if allowed.iter().any(|a| types::is_subtype(self.table, &t, a)) {
                continue;
            }
            let name = self.name_of(&t);
            let msg = format!(
                "`{}` declara lanzar `{name}`, más ancho que lo que permite `{owner}` (§8.4.8.3)",
                decl.name
            );
            self.error(decl.pos, msg);
        }
    }

    /// El retorno es **covariante** para referencias e **idéntico** para primitivos y `void`
    /// (§8.4.8.3). El del padre se ve *desde acá*: si hereda de `Caja<String>`, su `T get()` es un
    /// `String get()`, no un `T get()`.
    fn return_type(&mut self, decl: &MethodDecl, mine: SymbolId, parent: SymbolId, owner: &str) {
        let (Some(mine_ret), Some(parent_ret)) = (ret_of(self.table, mine), ret_of(self.table, parent))
        else {
            return;
        };
        let parent_ret = self.as_seen_from(mine, parent, &parent_ret);
        let ok = match (&mine_ret, &parent_ret) {
            // Sin resolver de un lado: no hay nada que comparar sin inventar un error.
            (RType::Unresolved, _) | (_, RType::Unresolved) => true,
            (RType::Prim(a), RType::Prim(b)) => a == b,
            (RType::Void, RType::Void) => true,
            (RType::Prim(_), _) | (_, RType::Prim(_)) | (RType::Void, _) | (_, RType::Void) => false,
            _ => types::is_subtype(self.table, &mine_ret, &parent_ret),
        };
        if !ok {
            let msg = format!(
                "el retorno de `{}` no es compatible con el de `{owner}`: {} no es un subtipo de {}",
                decl.name,
                self.name_of(&mine_ret),
                self.name_of(&parent_ret)
            );
            self.error(decl.pos, msg);
        }
    }

    /// El tipo de retorno heredado, **como se lo ve desde la clase que hace el override** (§8.4.8.1):
    /// se sustituyen los parámetros de tipo del supertipo por los argumentos con que **esta** clase lo
    /// extiende/implementa. Para `class MyList<E> implements List<E>` con `E get(int)`, el `E` de
    /// `List.get` (el parámetro de `List`) se sustituye por el `E` de `MyList`, así el retorno del
    /// padre queda `E` (el de MyList) y el override covariante chequea idéntico en vez de comparar dos
    /// variables `E` distintas. También cubre el caso no genérico (`extends Caja<String>` → `String`).
    ///
    /// El bug previo (finding #9) buscaba el supertipo entre los supertipos del **tipo de retorno**
    /// (una variable de tipo, cuyos supertipos son sus cotas) en vez de entre los de la **clase**, así
    /// que nunca hallaba `List<E>` y no sustituía nada.
    fn as_seen_from(&self, mine: SymbolId, parent: SymbolId, ret: &RType) -> RType {
        let mut result = ret.clone();
        // (1) Parámetros de tipo de **clase** del supertipo (finding #9): el self-type de la clase
        //     (`MyList<E>`) → su supertipo `owner` (`List<E>`) da la sustitución `{parámetro de List →
        //     E}`, que se aplica al retorno heredado. También el caso no genérico (`Caja<String>`).
        if let (Some(owner), Some(my_class)) =
            (self.table.symbol(parent).owner, self.table.symbol(mine).owner)
        {
            let self_ty = types::self_type(self.table, my_class);
            let subst = types::subst_for(self.table, &self_ty, owner);
            result = types::substitute(&result, &subst);
        }
        // (2) Parámetros de tipo de **método** (finding #17): los del método del padre corresponden
        //     **posicionalmente** con los del override (§8.4.2), así que el `R` del padre se reescribe
        //     al `R` de la impl — si no, un retorno `R` pelado compararía dos variables distintas.
        let parent_tps = super::infer::method_type_params(self.table, parent);
        let mine_tps = super::infer::method_type_params(self.table, mine);
        if !parent_tps.is_empty() && parent_tps.len() == mine_tps.len() {
            let msubst: types::Subst = parent_tps
                .iter()
                .zip(&mine_tps)
                .map(|(&p, &m)| (p, RType::TypeVar(m)))
                .collect();
            result = types::substitute(&result, &msubst);
        }
        result
    }

    /// El símbolo de **este** método: el `nth`-ésimo con su nombre y aridad.
    fn find_own(&self, cid: SymbolId, decl: &MethodDecl, nth: usize) -> Option<SymbolId> {
        let want = decl.params.len();
        self.table
            .members_of(cid)
            .into_iter()
            .filter(|&id| {
                self.table.symbol(id).name == decl.name
                    && matches!(&self.table.symbol(id).kind, SymbolKind::Method { params, .. } if params.len() == want)
            })
            .nth(nth)
    }

    /// El método heredado con la **misma firma borrada**, buscando primero por la cadena de
    /// superclases y después por las interfaces (§8.4.8.1).
    fn overridden(&self, cid: SymbolId, name: &str, sig: &[RType]) -> Option<SymbolId> {
        for sup in self.ancestors(cid) {
            for id in self.table.members_of(sup) {
                if self.table.symbol(id).name != name {
                    continue;
                }
                if !matches!(self.table.symbol(id).kind, SymbolKind::Method { .. }) {
                    continue;
                }
                if erased_params(self.table, id).as_deref() == Some(sig) {
                    return Some(id);
                }
            }
        }
        None
    }

    /// Los supertipos de `cid` en orden de búsqueda: superclases primero, después interfaces —
    /// ambas transitivas, sin repetir y sin colgarse en un ciclo.
    fn ancestors(&self, cid: SymbolId) -> Vec<SymbolId> {
        let mut out = Vec::new();
        let mut queue = vec![cid];
        let mut seen = vec![cid];
        while let Some(c) = queue.pop() {
            let mut next: Vec<SymbolId> = Vec::new();
            if let Some(s) = self.table.super_class(c) {
                next.push(s);
            }
            next.extend(self.table.interfaces(c));
            for s in next {
                if seen.contains(&s) {
                    continue;
                }
                seen.push(s);
                out.push(s);
                queue.push(s);
            }
        }
        out
    }

    /// §8.1.1.1: una clase concreta tiene que implementar todo lo `abstract` que hereda.
    ///
    /// Se saltea entera si algún ancestro es **externo**: de esos solo conocemos los miembros que
    /// aparecieron en alguna firma, así que la ausencia de un método no prueba nada.
    fn abstracts(&mut self, class: &ClassDecl, cid: SymbolId) {
        if class.modifiers.contains(&Modifier::Abstract) || class.kind != TypeKind::Class {
            return;
        }
        let ancestors = self.ancestors(cid);
        // Se exime solo si algún ancestro es una **clase externa distinta de `Object`**: de una clase
        // externa (p. ej. `AbstractList`) podríamos no ver todas las implementaciones concretas que
        // hereda, y falso-reclamaríamos. Cuando los ancestros externos son **interfaces**, el finder
        // ya cargó sus métodos —abstractos + `default` marcados— transitivamente, así que el chequeo
        // es confiable (finding #8: `implements List` sin implementar sus métodos ahora sí se reporta).
        // `Object` siempre está y sus métodos son concretos/conocidos: no exime.
        let externally_extends_a_class = ancestors.iter().any(|&a| {
            self.is_external(a)
                && !self.is_interface(a)
                && self.binary(a).replace('/', ".") != "java.lang.Object"
        });
        if externally_extends_a_class {
            return;
        }
        // Se recorre la jerarquía juntando las dos listas por **firma borrada**, y al final se
        // reclama lo que quedó abstracto sin una implementación que lo tape. La firma se computa
        // **como se ve desde la clase** (§8.4.8.1): los parámetros de tipo del supertipo se sustituyen
        // por sus argumentos antes de borrar. Sin esto, `Comparable<Foo>.compareTo(T)` borraría a
        // `(Object)` y `Foo.compareTo(Foo)` a `(Foo)`, no matchearían, y se reclamaría un método ya
        // implementado (el puente `compareTo(Object)` se sintetiza recién en codegen).
        let self_ty = types::self_type(self.table, cid);
        let mut implemented: Vec<(String, Vec<RType>)> = Vec::new();
        let mut pending: Vec<(String, Vec<RType>, String)> = Vec::new();
        for &c in std::iter::once(&cid).chain(ancestors.iter()) {
            let cname = self.table.symbol(c).name.clone();
            let in_interface = self.is_interface(c);
            let subst = types::subst_for(self.table, &self_ty, c);
            for id in self.table.members_of(c) {
                let sym = self.table.symbol(id);
                if !matches!(sym.kind, SymbolKind::Method { .. }) {
                    continue;
                }
                let Some(sig) = erased_params_as_seen(self.table, id, &subst) else { continue };
                let key = (sym.name.clone(), sig);
                // En una interfaz, `abstract` es implícito: lo que **no** es abstracto es lo que
                // trae `default` o `static`.
                let abstracto = if in_interface {
                    !sym.modifiers.contains(&Modifier::Default)
                        && !sym.modifiers.contains(&Modifier::Static)
                } else {
                    sym.modifiers.contains(&Modifier::Abstract)
                };
                if abstracto {
                    if !pending.iter().any(|(n, s, _)| *n == key.0 && *s == key.1) {
                        pending.push((key.0, key.1, cname.clone()));
                    }
                } else {
                    implemented.push(key);
                }
            }
        }
        pending.retain(|(n, s, _)| !implemented.iter().any(|(in_, is_)| in_ == n && is_ == s));
        for (name, _, owner) in pending {
            let msg =
                format!("`{}` no es abstracta y no implementa `{name}` de `{owner}`", class.name);
            self.error(class.pos, msg);
        }
    }

    /// Las reglas de buena formación de `sealed` (§8.1.1.2 / §9.1.1.4 / §8.1.6). El `permits`
    /// resuelto (incluido el **implícito**) ya vive en el grafo, así que acá solo se contrastan
    /// modificadores y la relación de subtipado **directo**.
    fn sealed_rules(&mut self, class: &ClassDecl, cid: SymbolId) {
        let is_sealed = class.modifiers.contains(&Modifier::Sealed);
        let is_non_sealed = class.modifiers.contains(&Modifier::NonSealed);

        // Combinaciones contradictorias de modificadores.
        if is_sealed && class.modifiers.contains(&Modifier::Final) {
            self.error(class.pos, format!("`{}` no puede ser `sealed` y `final` a la vez", class.name));
        }
        if is_sealed && is_non_sealed {
            self.error(class.pos, format!("`{}` no puede ser `sealed` y `non-sealed`", class.name));
        }

        // Supertipos **directos** de este tipo.
        let mut supers = Vec::new();
        if let Some(s) = self.table.super_class(cid) {
            supers.push(s);
        }
        supers.extend(self.table.interfaces(cid));

        // §8.1.1.2: `non-sealed` exige un supertipo directo `sealed`.
        if is_non_sealed && !supers.iter().any(|&s| self.table.is_sealed(s)) {
            self.error(
                class.pos,
                format!("`non-sealed` en `{}` sin un supertipo directo `sealed`", class.name),
            );
        }

        // Este tipo como **subtipo** de un `sealed`: debe cerrar su extensión (final/sealed/
        // non-sealed —o ser implícitamente final: enum/record—) y estar en su `permits`.
        let ok_modifier = is_sealed
            || is_non_sealed
            || class.modifiers.contains(&Modifier::Final)
            || matches!(class.kind, TypeKind::Enum | TypeKind::Record);
        for &s in &supers {
            if !self.table.is_sealed(s) {
                continue;
            }
            let sname = self.table.symbol(s).name.clone();
            if !ok_modifier {
                self.error(class.pos, format!(
                    "`{}` extiende el tipo `sealed` `{sname}`: debe ser `final`, `sealed` o `non-sealed`",
                    class.name,
                ));
            }
            let permitted: Vec<SymbolId> =
                self.table.permitted(s).iter().filter_map(types::erased_id).collect();
            if !permitted.contains(&cid) {
                self.error(class.pos, format!(
                    "`{}` no está autorizada a extender `{sname}` (falta en su `permits`)",
                    class.name,
                ));
            }
        }

        if is_sealed {
            // §8.1.6: un `sealed` necesita al menos un subtipo autorizado.
            if self.table.permitted(cid).is_empty() {
                self.error(
                    class.pos,
                    format!("el tipo `sealed` `{}` no declara subtipos permitidos", class.name),
                );
            }
            // Con `permits` **explícito**, cada tipo listado debe ser subtipo **directo**.
            if !class.permits.is_empty() {
                let listed: Vec<SymbolId> =
                    self.table.permitted(cid).iter().filter_map(types::erased_id).collect();
                for pid in listed {
                    let mut psupers = Vec::new();
                    if let Some(ps) = self.table.super_class(pid) {
                        psupers.push(ps);
                    }
                    psupers.extend(self.table.interfaces(pid));
                    if !psupers.contains(&cid) {
                        let pname = self.table.symbol(pid).name.clone();
                        self.error(
                            class.pos,
                            format!("`permits` de `{}`: `{pname}` no es un subtipo directo", class.name),
                        );
                    }
                }
            }
        }
    }

    // ---- exhaustividad de `switch` (§14.11.1.1) ----

    /// Un `switch`-**expresión** (siempre) y un `switch`-**sentencia con patterns**/`case null` deben
    /// cubrir todos los valores posibles. Se comprueba para `sealed` (recursivo hasta las hojas),
    /// `enum` (todas las constantes) y `boolean` (`true`/`false`). Un `default` cubre por definición.
    fn exhaustive_switch(&mut self, from: SymbolId, selector: &Expr, cases: &[SwitchCase], is_expr: bool) {
        if cases.iter().any(|c| c.is_default) {
            return;
        }
        // Un `switch`-sentencia "clásico" (solo constantes, sin patterns) no exige exhaustividad
        // (§14.11.1.1): puede no cubrir todo y simplemente no hacer nada.
        let uses_patterns = cases
            .iter()
            .flat_map(|c| &c.labels)
            .any(|l| matches!(l, CaseLabel::Pattern(_) | CaseLabel::Null));
        if !is_expr && !uses_patterns {
            return;
        }
        // Sin tipo de selector resuelto no se opina (indulgencia, como en el resto del chequeo).
        let Some(sel) = selector.ty.as_ref() else { return };
        if self.switch_covers(from, sel, cases) {
            return;
        }
        let what = if is_expr { "la expresión `switch`" } else { "el `switch`" };
        self.error(
            selector.pos,
            format!("{what} no es exhaustivo: no cubre todos los valores posibles"),
        );
    }

    /// ¿Los brazos cubren todo el tipo del selector?
    fn switch_covers(&self, from: SymbolId, sel: &RType, cases: &[SwitchCase]) -> bool {
        // `boolean`: hacen falta `true` y `false` (patrones de primitivo, §14.11.1.1 con JEP 455).
        if matches!(sel, RType::Prim(PrimType::Boolean)) {
            let (mut t, mut f) = (false, false);
            for l in cases.iter().flat_map(|c| &c.labels) {
                if let CaseLabel::Constant(e) = l {
                    match &e.kind {
                        ExprKind::BoolLit(true) => t = true,
                        ExprKind::BoolLit(false) => f = true,
                        _ => {}
                    }
                }
            }
            return t && f;
        }
        let total = self.total_pattern_types(from, cases);
        let Some(id) = types::erased_id(sel) else { return false };
        // `enum`: un patrón total que lo cubra, o todas sus constantes presentes.
        if self.is_enum(id) {
            if total.iter().any(|p| types::is_subtype(self.table, sel, p)) {
                return true;
            }
            return self.enum_constants_covered(id, cases);
        }
        // `sealed`/referencia: cobertura por tipos totales, recursiva sobre la jerarquía sellada.
        self.covers_type(sel, &total)
    }

    /// `T` está cubierto si algún patrón **total** lo abarca (`T <: patrón`), o si `T` es `sealed` y
    /// **cada** subtipo autorizado está cubierto (recursión hasta las hojas `final`/`non-sealed`).
    fn covers_type(&self, t: &RType, total: &[RType]) -> bool {
        if total.iter().any(|p| types::is_subtype(self.table, t, p)) {
            return true;
        }
        if let Some(id) = types::erased_id(t) {
            if self.table.is_sealed(id) {
                let perm = self.table.permitted(id);
                if !perm.is_empty() && perm.iter().all(|s| self.covers_type(s, total)) {
                    return true;
                }
            }
        }
        false
    }

    /// Los tipos de los patrones **totales**: patrones sin `when` (una guarda puede fallar, así que
    /// no cubre). Un patrón de `record` se toma como total para su tipo (aproximación suficiente para
    /// las jerarquías selladas de records).
    fn total_pattern_types(&self, from: SymbolId, cases: &[SwitchCase]) -> Vec<RType> {
        let mut out = Vec::new();
        for c in cases {
            if c.guard.is_some() {
                continue;
            }
            for l in &c.labels {
                if let CaseLabel::Pattern(p) = l {
                    let ty = match p {
                        Pattern::Type { ty, .. } | Pattern::Record { ty, .. } => ty,
                    };
                    if let Some(id) = self.resolve_type_id(from, ty) {
                        out.push(RType::Class(id));
                    }
                }
            }
        }
        out
    }

    /// Resuelve un nombre de tipo (de un patrón) a su `SymbolId`, en el scope de la clase o entre los
    /// tipos externos.
    fn resolve_type_id(&self, from: SymbolId, ty: &Type) -> Option<SymbolId> {
        let name = match ty {
            Type::Class(n) | Type::Parameterized { base: n, .. } => n,
            _ => return None,
        };
        let scope = match &self.table.symbol(from).kind {
            SymbolKind::Class { members, .. } => *members,
            _ => return None,
        };
        self.table.resolve_type(scope, name).or_else(|| self.table.external(name))
    }

    fn is_enum(&self, cid: SymbolId) -> bool {
        matches!(&self.table.symbol(cid).kind, SymbolKind::Class { kind: TypeKind::Enum, .. })
    }

    /// Los nombres de las constantes de un `enum`: sus campos `static` cuyo tipo es el propio `enum`
    /// (así se descartan el `$VALUES` sintético y cualquier otro campo).
    fn enum_constant_names(&self, enum_id: SymbolId) -> Vec<String> {
        self.table
            .members_of(enum_id)
            .into_iter()
            .filter_map(|m| {
                let sym = self.table.symbol(m);
                if !matches!(sym.kind, SymbolKind::Field { .. }) {
                    return None;
                }
                match self.table.resolved(m) {
                    Some(Resolved::Field(RType::Class(fid))) if *fid == enum_id => Some(sym.name.clone()),
                    _ => None,
                }
            })
            .collect()
    }

    /// ¿Están **todas** las constantes del `enum` como etiquetas `case NAME`?
    fn enum_constants_covered(&self, enum_id: SymbolId, cases: &[SwitchCase]) -> bool {
        let consts = self.enum_constant_names(enum_id);
        if consts.is_empty() {
            return false;
        }
        let mut covered = std::collections::HashSet::new();
        for l in cases.iter().flat_map(|c| &c.labels) {
            if let CaseLabel::Constant(e) = l {
                if let ExprKind::Name(n) = &e.kind {
                    covered.insert(n.as_str());
                }
            }
        }
        consts.iter().all(|c| covered.contains(c.as_str()))
    }

    fn is_interface(&self, cid: SymbolId) -> bool {
        matches!(&self.table.symbol(cid).kind, SymbolKind::Class { kind: TypeKind::Interface, .. })
    }

    /// Un tipo **externo** (modelado por el *class finder*, con sus miembros incompletos).
    fn is_external(&self, cid: SymbolId) -> bool {
        self.table.external(&self.table.symbol(cid).name) == Some(cid)
    }

    fn name_of(&self, ty: &RType) -> String {
        match ty {
            RType::Void => "void".to_string(),
            RType::Prim(p) => format!("{p:?}").to_lowercase(),
            RType::Array(e) => format!("{}[]", self.name_of(e)),
            RType::Class(id) | RType::TypeVar(id) => self.table.symbol(*id).name.clone(),
            RType::Parameterized { base, .. } => self.table.symbol(*base).name.clone(),
            RType::Capture { upper, .. } => self.name_of(upper),
            RType::Intersection(ms) => ms.first().map_or_else(|| "?".to_string(), |m| self.name_of(m)),
            RType::InferVar(_) => "?".to_string(),
            RType::Unresolved => "?".to_string(),
        }
    }
}

#[cfg(test)]
mod tests {
    /// Corre el pipeline semántico y devuelve los mensajes de error. (`javac::check` es la
    /// **función** del pipeline; este módulo es la pasada. Rust los distingue por namespace.)
    fn errors(src: &str) -> Vec<String> {
        crate::javac::check(src)
            .expect("el fuente debe parsear")
            .into_iter()
            .map(|e| e.message)
            .collect()
    }

    fn ok(src: &str) {
        let e = errors(src);
        assert!(e.is_empty(), "no debería haber errores, salieron: {e:?}");
    }

    /// Que haya **un** error y que mencione `needle`.
    fn one(src: &str, needle: &str) {
        let e = errors(src);
        assert_eq!(e.len(), 1, "se esperaba un solo error, salieron: {e:?}");
        assert!(e[0].contains(needle), "el mensaje no menciona `{needle}`: {}", e[0]);
    }

    // ---- retorno covariante (§8.4.8.3) ----

    #[test]
    fn an_incompatible_return_type_is_rejected() {
        one(
            "class A { Object f() { return null; } }              class B extends A { int f() { return 0; } }",
            "no es compatible",
        );
    }

    #[test]
    fn a_covariant_return_type_is_accepted() {
        // Estrechar el retorno **sí** vale: es lo que permite `clone()` tipado (§8.4.8.3).
        ok("class A { Object f() { return null; } }             class B extends A { String f() { return null; } }");
    }

    #[test]
    fn widening_the_return_type_is_rejected() {
        one(
            "class A { String f() { return null; } }              class B extends A { Object f() { return null; } }",
            "no es compatible",
        );
    }

    #[test]
    fn a_different_primitive_return_is_rejected() {
        // Entre primitivos no hay covarianza: tiene que ser **idéntico**.
        one(
            "class A { int f() { return 0; } }              class B extends A { long f() { return 0; } }",
            "no es compatible",
        );
    }

    #[test]
    fn a_generic_covariant_override_substitutes_the_type_variable() {
        // finding #9: `class MyBox<E> implements Box<E>` con `E get()` — el `T` del padre (parámetro
        // de `Box`) se sustituye por el `E` de `MyBox`, así el retorno chequea idéntico en vez de
        // comparar dos variables de tipo distintas.
        ok("interface Box<T> { T get(); } \
            class MyBox<E> implements Box<E> { public E get() { return null; } }");
    }

    #[test]
    fn a_generic_override_with_a_bare_method_type_variable_return_is_accepted() {
        // finding #17: `<R> R foo(R x)` en la impl contra `<R> R foo(R x)` en la interfaz — los `R` de
        // **método** corresponden posicionalmente (§8.4.2), no se comparan como variables distintas.
        // Es el análogo a nivel método del #9 (que arregló las de clase).
        ok("interface I<T> { <R> R foo(R x); } \
            class C<T> implements I<T> { public <R> R foo(R x) { return x; } }");
    }

    #[test]
    fn a_generic_override_with_a_concrete_type_argument_is_accepted() {
        // `class StringBox implements Box<String>` con `String get()`: el `T` de `Box` es `String`.
        ok("interface Box<T> { T get(); } \
            class StringBox implements Box<String> { public String get() { return null; } }");
    }

    #[test]
    fn a_same_package_classpath_type_resolves_by_simple_name() {
        // finding #4: `List` desde `package java.util` resuelve por su nombre simple (busca
        // `java/util/List` en el classpath), sin `import`. Antes daba "no se encuentra el símbolo: List".
        let e = errors("package java.util; public class P { List<Object> f() { return null; } }");
        assert!(
            !e.iter().any(|m| m.contains("símbolo") && m.contains("List")),
            "`List` debe resolver por nombre simple en su paquete: {e:?}",
        );
    }

    #[test]
    fn a_static_call_on_a_java_util_utility_resolves() {
        // finding #11: `Objects.requireNonNull(x)` — el receptor `Objects` (utilidad de java.util) se
        // carga del classpath porque ahora se **recolecta** el receptor de la llamada estática, y su
        // método resuelve. Antes fallaba con "no se encuentra el símbolo: Objects".
        ok("import java.util.Objects; \
            public class M { void r(Object x) { Objects.requireNonNull(x); } }");
    }

    #[test]
    fn a_source_core_type_shadows_the_external_one_in_the_override_check() {
        // finding #5: al compilar `java.lang.String` en sí, su `toString()` que devuelve `String` no
        // debe chocar con el `String` **externo** del classpath — el tipo del fuente lo sombrea
        // (semántica `--patch-module`), así que ambos lados del override son el mismo `String`.
        ok("package java.lang; \
            public class String implements CharSequence { \
                public String toString() { return this; } \
                public int length() { return 0; } \
                public char charAt(int i) { return 'a'; } \
                public CharSequence subSequence(int a, int b) { return this; } }");
    }

    #[test]
    fn a_generic_override_returning_the_wrong_type_is_still_rejected() {
        // El fix no debe volverse permisivo: si el retorno no matchea el parámetro sustituido, error.
        one(
            "interface Box<T> { T get(); } \
             class BadBox implements Box<String> { public Integer get() { return null; } }",
            "no es compatible",
        );
    }

    // ---- `@Override` (§9.6.4.4) ----

    #[test]
    fn override_on_a_real_override_is_fine() {
        ok("class A { void m() {} } class B extends A { @Override void m() {} }");
    }

    #[test]
    fn override_implementing_an_interface_method_is_fine() {
        ok("interface I { void m(); } class A implements I { @Override public void m() {} }");
    }

    #[test]
    fn override_on_a_method_that_overrides_nothing_is_rejected() {
        one(
            "class A {} class B extends A { @Override void g() {} }",
            "no sobrescribe",
        );
    }

    #[test]
    fn override_on_an_object_method_is_not_second_guessed() {
        // `Object` es externo y sus miembros no siempre están cargados: un `@Override` sobre uno de sus
        // métodos (`toString`) no se reporta como si no sobrescribiera nada.
        ok("class A { @Override public String toString() { return \"\"; } }");
    }

    // ---- `throws` más ancho (§8.4.8.3) ----

    #[test]
    fn a_wider_throws_in_an_override_is_rejected() {
        one(
            "class A { void m() {} } class B extends A { void m() throws Exception {} }",
            "más ancho",
        );
    }

    #[test]
    fn a_narrower_throws_in_an_override_is_fine() {
        // `Exception` <: `Throwable`: estrechar el `throws` es legal.
        ok("class A { void m() throws Throwable {} } class B extends A { void m() throws Exception {} }");
    }

    #[test]
    fn an_unchecked_throws_in_an_override_is_fine() {
        // `RuntimeException` no es chequeada: no cuenta para la regla del `throws`.
        ok("class A { void m() {} } class B extends A { void m() throws RuntimeException {} }");
    }

    // ---- visibilidad (§8.4.8.3) ----

    #[test]
    fn reducing_visibility_is_rejected() {
        one(
            "class A { public int f() { return 0; } }              class B extends A { private int f() { return 0; } }",
            "reduce la visibilidad",
        );
    }

    #[test]
    fn widening_visibility_is_accepted() {
        ok("class A { protected int f() { return 0; } }             class B extends A { public int f() { return 0; } }");
    }

    // ---- `final` y `static` (§8.4.3.3 / §8.4.8.2) ----

    #[test]
    fn overriding_a_final_method_is_rejected() {
        one(
            "class A { public final int f() { return 0; } }              class B extends A { public int f() { return 0; } }",
            "es `final`",
        );
    }

    #[test]
    fn crossing_the_static_boundary_is_rejected() {
        one(
            "class A { public static int f() { return 0; } }              class B extends A { public int f() { return 0; } }",
            "no puede sobrescribir uno del otro tipo",
        );
    }

    // ---- lo que **no** es un override ----

    #[test]
    fn a_different_signature_is_an_overload_not_an_override() {
        // Distinta aridad ⇒ no sobrescribe ⇒ ninguna regla aplica.
        ok("class A { public final int f() { return 0; } }             class B extends A { public String f(int x) { return null; } }");
    }

    #[test]
    fn the_erasure_is_what_decides() {
        // `f(Caja<String>)` y `f(Caja<Integer>)` tienen la **misma** firma borrada: sobrescribe, y
        // el retorno incompatible se reporta.
        one(
            "class Caja<T> {}              class A { Object f(Caja<String> c) { return null; } }              class B extends A { int f(Caja<Integer> c) { return 0; } }",
            "no es compatible",
        );
    }

    #[test]
    fn an_interface_method_is_overridden_too() {
        one(
            "interface I { Object f(); }              class B implements I { public int f() { return 0; } }",
            "no es compatible",
        );
    }

    // ---- métodos abstractos sin implementar (§8.1.1.1) ----

    #[test]
    fn a_concrete_class_must_implement_its_interface() {
        one("interface I { int f(); } class B implements I { }", "no implementa `f`");
    }

    #[test]
    fn implementing_it_satisfies_the_check() {
        ok("interface I { int f(); } class B implements I { public int f() { return 0; } }");
    }

    #[test]
    fn an_abstract_class_may_leave_it_pending() {
        ok("interface I { int f(); } abstract class B implements I { }");
    }

    #[test]
    fn an_intermediate_class_can_implement_it() {
        ok("interface I { int f(); }             abstract class M implements I { public int f() { return 0; } }             class B extends M { }");
    }

    #[test]
    fn a_default_method_needs_no_implementation() {
        ok("interface I { default int f() { return 0; } } class B implements I { }");
    }

    #[test]
    fn an_inherited_abstract_method_is_reported() {
        one(
            "abstract class A { abstract int f(); } class B extends A { }",
            "no implementa `f`",
        );
    }

    #[test]
    fn an_external_interface_no_longer_exempts_an_unimplemented_method() {
        // finding #8: el finder carga los métodos de una **interfaz** externa, así que implementar
        // `Runnable` sin su `run()` abstracto **sí** se reporta (antes se eximía toda la jerarquía).
        one("class B implements Runnable { }", "no implementa `run`");
    }

    #[test]
    fn an_external_class_ancestor_still_exempts() {
        // De una **clase** externa (`Thread`) podríamos no ver sus implementaciones concretas
        // heredadas: se sigue eximiendo el chequeo, para no inventar errores.
        ok("abstract class Base extends Thread { abstract void extra(); } class B extends Base { }");
    }

    #[test]
    fn not_implementing_an_external_interfaces_methods_is_reported() {
        // finding #8, caso de KajiLibrary: `implements List` con solo `size()` reclama el resto.
        let e = errors(
            "package java.util; import java.util.List; \
             public class P<E> implements List<E> { public int size() { return 0; } }",
        );
        assert!(
            e.iter().any(|m| m.contains("no implementa")),
            "debe reclamar métodos abstractos de List sin implementar: {e:?}",
        );
    }

    #[test]
    fn fully_implementing_an_external_interface_is_fine() {
        // Contraprueba (sin falso positivo): implementar del todo `Runnable` compila.
        ok("class C implements Runnable { public void run() {} }");
    }

    #[test]
    fn default_methods_of_an_external_interface_need_not_be_overridden() {
        // Los `default` externos (`Iterator.remove`/`forEachRemaining`) NO se reclaman — se marcan al
        // cargar la interfaz. Solo hacen falta los abstractos (`hasNext`/`next`). (finding #8.)
        ok("import java.util.Iterator; \
            public class It implements Iterator<Object> { \
                public boolean hasNext() { return false; } \
                public Object next() { return null; } }");
    }

    // ---- control de acceso (§6.6) ----

    #[test]
    fn a_private_field_is_not_accessible_from_another_class() {
        one(
            "class A { private int secret; } class B { int m(A a) { return a.secret; } }",
            "no es accesible",
        );
    }

    #[test]
    fn a_private_method_is_not_accessible_from_another_class() {
        one(
            "class A { private int h() { return 1; } } class B { int m(A a) { return a.h(); } }",
            "no es accesible",
        );
    }

    #[test]
    fn a_private_member_is_accessible_within_its_own_class() {
        ok("class A { private int x = 1; int self() { return this.x; } }");
    }

    #[test]
    fn a_nested_class_reaches_the_outer_private() {
        // `private` alcanza a todo el tipo top-level (§6.6.1): una anidada ve el privado de la que
        // la encierra.
        ok("class A { private int x; static class Inner { int reach(A a) { return a.x; } } }");
    }

    #[test]
    fn a_package_private_member_is_accessible_in_the_same_package() {
        // Sin paquete declarado, las dos clases están en el paquete sin nombre — el mismo.
        ok("class A { int pkg; } class B { int m(A a) { return a.pkg; } }");
    }

    #[test]
    fn a_protected_member_is_accessible_from_a_subclass() {
        ok("class A { protected int p = 1; } class B extends A { int m() { return this.p; } }");
    }

    #[test]
    fn a_public_member_is_always_accessible() {
        ok("class A { public int p; } class B { int m(A a) { return a.p; } }");
    }

    #[test]
    fn an_external_members_access_is_not_second_guessed() {
        // De un tipo externo no conocemos sus flags con confianza: se exime (indulgencia conocida).
        ok("class B { int m(String s) { return s.length(); } }");
    }

    // ---- excepciones chequeadas (§11.2) ----

    #[test]
    fn throwing_a_checked_exception_unhandled_is_rejected() {
        one("class A { void m() { throw new Exception(); } }", "sin capturar ni declarar");
    }

    #[test]
    fn calling_a_throwing_method_unhandled_is_rejected() {
        one(
            "class A { void m() throws Exception {} void c() { m(); } }",
            "sin capturar ni declarar",
        );
    }

    #[test]
    fn a_checked_exception_caught_in_a_try_is_fine() {
        ok("class A { void m() throws Exception {} void c() { try { m(); } catch (Exception e) {} } }");
    }

    #[test]
    fn a_checked_exception_declared_in_throws_is_fine() {
        ok("class A { void m() throws Exception {} void c() throws Exception { m(); } }");
    }

    #[test]
    fn a_supertype_in_throws_covers_it() {
        // `throws Throwable` cubre una `Exception` por subtipado (§11.2.3).
        ok("class A { void m() throws Exception {} void c() throws Throwable { m(); } }");
    }

    #[test]
    fn an_unchecked_exception_needs_no_handling() {
        ok("class A { void m() { throw new RuntimeException(); } }");
    }

    #[test]
    fn the_catch_body_is_outside_its_own_handler() {
        // Relanzar dentro del `catch` **no** está manejado por ese mismo `catch`.
        one(
            "class A { void m() throws Exception {} \
             void c() { try { m(); } catch (Exception e) { m(); } } }",
            "sin capturar ni declarar",
        );
    }

    // ---- sealed (§8.1.1.2 / §9.1.1.4 / §8.1.6) ----

    /// Que **algún** error mencione `needle` (los casos de sealed pueden dar más de uno).
    fn some(src: &str, needle: &str) {
        let e = errors(src);
        assert!(e.iter().any(|m| m.contains(needle)), "ningún error menciona `{needle}`: {e:?}");
    }

    #[test]
    fn a_well_formed_sealed_hierarchy_is_accepted() {
        ok("sealed interface Shape permits Circle, Square {} \
            final class Circle implements Shape {} \
            final class Square implements Shape {}");
    }

    #[test]
    fn an_implicit_permits_from_unit_subtypes_is_accepted() {
        // Sin `permits`: los subtipos directos de la misma unidad son los autorizados.
        ok("sealed interface Shape {} \
            final class Circle implements Shape {} \
            final class Square implements Shape {}");
    }

    #[test]
    fn a_non_sealed_subtype_reopens_extension() {
        ok("sealed class Base permits Sub {} \
            non-sealed class Sub extends Base {}");
    }

    #[test]
    fn a_subtype_of_a_sealed_type_must_close_its_extension() {
        // `Sub` no es `final`/`sealed`/`non-sealed`: ilegal.
        some(
            "sealed class Base permits Sub {} class Sub extends Base {}",
            "debe ser `final`, `sealed` o `non-sealed`",
        );
    }

    #[test]
    fn an_unlisted_subtype_of_an_explicit_permits_is_rejected() {
        some(
            "sealed interface Shape permits Circle {} \
             final class Circle implements Shape {} \
             final class Triangle implements Shape {}",
            "no está autorizada a extender",
        );
    }

    #[test]
    fn a_permits_entry_that_is_not_a_direct_subtype_is_rejected() {
        some(
            "sealed interface Shape permits Stranger {} final class Stranger {}",
            "no es un subtipo directo",
        );
    }

    #[test]
    fn a_non_sealed_without_a_sealed_supertype_is_rejected() {
        some("non-sealed class Lonely {}", "sin un supertipo directo `sealed`");
    }

    #[test]
    fn a_sealed_type_without_subtypes_is_rejected() {
        some("sealed interface Empty {}", "no declara subtipos permitidos");
    }

    // ---- exhaustividad de switch (§14.11.1.1) ----

    const SHAPES: &str = "sealed interface Shape permits Circle, Square {} \
        final class Circle implements Shape {} \
        final class Square implements Shape {} ";

    #[test]
    fn a_sealed_switch_expression_covering_all_subtypes_is_exhaustive() {
        ok(&format!(
            "{SHAPES} class U {{ int f(Shape s) {{ \
             return switch (s) {{ case Circle c -> 1; case Square q -> 2; }}; }} }}"
        ));
    }

    #[test]
    fn a_sealed_switch_missing_a_subtype_is_rejected() {
        some(
            &format!(
                "{SHAPES} class U {{ int f(Shape s) {{ \
                 return switch (s) {{ case Circle c -> 1; }}; }} }}"
            ),
            "no es exhaustivo",
        );
    }

    #[test]
    fn a_total_pattern_on_the_selector_type_is_exhaustive() {
        ok(&format!(
            "{SHAPES} class U {{ int f(Shape s) {{ \
             return switch (s) {{ case Shape any -> 0; }}; }} }}"
        ));
    }

    #[test]
    fn a_guarded_pattern_does_not_count_towards_coverage() {
        some(
            &format!(
                "{SHAPES} class U {{ int f(Shape s, int k) {{ \
                 return switch (s) {{ case Circle c when k > 0 -> 1; case Square q -> 2; }}; }} }}"
            ),
            "no es exhaustivo",
        );
    }

    #[test]
    fn a_default_makes_a_switch_expression_exhaustive() {
        ok(&format!(
            "{SHAPES} class U {{ int f(Shape s) {{ \
             return switch (s) {{ case Circle c -> 1; default -> 0; }}; }} }}"
        ));
    }

    #[test]
    fn a_pattern_switch_statement_must_be_exhaustive() {
        some(
            &format!(
                "{SHAPES} class U {{ void f(Shape s) {{ \
                 switch (s) {{ case Circle c -> {{}} }} }} }}"
            ),
            "no es exhaustivo",
        );
    }

    #[test]
    fn an_enum_switch_expression_covering_all_constants_is_exhaustive() {
        ok("enum Color { RED, GREEN } \
            class U { int f(Color c) { return switch (c) { case RED -> 1; case GREEN -> 2; }; } }");
    }

    #[test]
    fn an_enum_switch_expression_missing_a_constant_is_rejected() {
        some(
            "enum Color { RED, GREEN } \
             class U { int f(Color c) { return switch (c) { case RED -> 1; }; } }",
            "no es exhaustivo",
        );
    }

    #[test]
    fn a_legacy_enum_switch_statement_needs_no_exhaustiveness() {
        // Un `switch`-sentencia clásico (solo constantes) puede no cubrir todo: no se exige.
        ok("enum Color { RED, GREEN } \
            class U { void f(Color c) { switch (c) { case RED: break; } } }");
    }

    #[test]
    fn a_boolean_switch_expression_covering_both_values_is_exhaustive() {
        ok("class U { int f(boolean b) { \
            return switch (b) { case true -> 1; case false -> 0; }; } }");
    }

    #[test]
    fn a_boolean_switch_expression_missing_a_value_is_rejected() {
        some(
            "class U { int f(boolean b) { return switch (b) { case true -> 1; }; } }",
            "no es exhaustivo",
        );
    }

    // ---- módulos (§7.7) ----

    #[test]
    fn a_well_formed_module_is_accepted() {
        ok("module m { requires java.base; exports p.api; opens p.impl; uses p.S; provides p.S with p.Impl; }");
    }

    #[test]
    fn a_duplicate_requires_is_rejected() {
        one("module m { requires a.b; requires a.b; }", "`requires` duplicado");
    }

    #[test]
    fn a_self_requires_is_rejected() {
        one("module m.foo { requires m.foo; }", "no puede requerirse a sí mismo");
    }

    #[test]
    fn a_duplicate_exports_is_rejected() {
        // El mismo paquete no puede exportarse dos veces, aunque cambie el `to`.
        one("module m { exports p.api; exports p.api to other; }", "`exports` duplicado");
    }

    #[test]
    fn a_repeated_provides_implementation_is_rejected() {
        one("module m { provides p.S with p.A, p.A; }", "implementación repetida");
    }
}
