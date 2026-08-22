//! El pase de **lint** (`-Xlint`): avisos de calidad que **no** cortan la compilación (§9.6.4.5 —un
//! *warning* no es un error—). Corre sobre el AST **ya atribuido** (antes del desugar, para que las
//! posiciones caigan en el fuente y los nodos sintéticos no disparen falsos avisos) y produce
//! `Error`s de severidad `Warning`. Cada aviso pertenece a una **categoría** (`fallthrough`, `cast`,
//! …) que `-Xlint:<cat>` habilita y que un `@SuppressWarnings("<cat>")` (o `"all"`) en una
//! declaración **envolvente** silencia (§9.6.4.5).
//!
//! Es la primera pieza del frente **"Nivel herramienta"** de B6 (el `javac` como herramienta, no como
//! compilador de lenguaje). Arranca con dos chequeos autocontenidos —`empty` y `cast`— y la
//! maquinaria para sumar el resto de a uno.

use std::collections::HashSet;

use super::ast::*;
use super::symbol::{RType, RTypeArg, Resolved, SymbolId, SymbolKind, SymbolTable};
use super::{types, Error};

/// Una categoría de `-Xlint`. El `id` es el nombre que usan `-Xlint:<id>` y `@SuppressWarnings("<id>")`.
#[derive(Clone, Copy, PartialEq, Eq, Hash, Debug)]
pub enum Lint {
    /// Una sentencia **vacía** (`;`) como cuerpo de un `if`/`while`/`for`/`do` — casi siempre un bug.
    Empty,
    /// Un **cast redundante**: el operando ya es del tipo (o un subtipo) al que se castea.
    Cast,
    /// Un `case` de **dos puntos** que **cae** al siguiente (completa normalmente sin `break`/`return`/…).
    Fallthrough,
    /// El uso de un elemento marcado **`@Deprecated`** (§9.6.4.6).
    Deprecation,
    /// El uso de un tipo **raw**: un genérico sin sus argumentos de tipo (`List` en vez de `List<E>`, §4.8).
    Rawtypes,
    /// Una operación **sin chequear** (§5.1.9): conversión raw→parametrizado, llamada a un método con
    /// type-vars sobre un receptor raw, o cast a un tipo parametrizado no reificable.
    Unchecked,
    /// Una cláusula **`finally`** que no puede completar normalmente (§14.20.2) — descarta la
    /// excepción o el `return` del `try`.
    Finally,
    /// Una clase **`Serializable`** sin un `serialVersionUID` bien declarado (§ serialización).
    Serial,
    /// Un constructor que llama a un método **sobreescribible por un subtipo externo** — `this` puede
    /// "escapar" antes de que la subclase esté inicializada (JEP de JDK 21).
    ThisEscape,
}

impl Lint {
    /// El identificador textual (el de `-Xlint:<id>` y `@SuppressWarnings`).
    pub fn id(self) -> &'static str {
        match self {
            Lint::Empty => "empty",
            Lint::Cast => "cast",
            Lint::Fallthrough => "fallthrough",
            Lint::Deprecation => "deprecation",
            Lint::Rawtypes => "rawtypes",
            Lint::Unchecked => "unchecked",
            Lint::Finally => "finally",
            Lint::Serial => "serial",
            Lint::ThisEscape => "this-escape",
        }
    }

    /// Todas las categorías soportadas — lo que habilita `-Xlint`/`-Xlint:all`.
    pub fn all() -> &'static [Lint] {
        &[
            Lint::Empty,
            Lint::Cast,
            Lint::Fallthrough,
            Lint::Deprecation,
            Lint::Rawtypes,
            Lint::Unchecked,
            Lint::Finally,
            Lint::Serial,
            Lint::ThisEscape,
        ]
    }

    fn from_id(s: &str) -> Option<Lint> {
        Lint::all().iter().copied().find(|l| l.id() == s)
    }
}

/// El conjunto de categorías **habilitadas** por la línea de comandos.
#[derive(Clone, Default)]
pub struct LintSet(HashSet<Lint>);

impl LintSet {
    /// Ninguna categoría (el default: los avisos son *opt-in*, como en `javac`).
    pub fn none() -> LintSet {
        LintSet(HashSet::new())
    }

    /// Todas las categorías (`-Xlint` o `-Xlint:all`).
    pub fn all() -> LintSet {
        LintSet(Lint::all().iter().copied().collect())
    }

    /// Interpreta el argumento de `-Xlint[:spec]`: `all`, `none`, o una lista `cat1,cat2` (con `-cat`
    /// para desactivar una). Un `spec` vacío o `all` habilita todo. Las categorías desconocidas se
    /// ignoran (como `javac`, que solo avisa con `-Xlint:-<desconocida>` no soportada).
    pub fn from_spec(spec: &str) -> LintSet {
        let spec = spec.trim();
        if spec.is_empty() || spec == "all" {
            return LintSet::all();
        }
        if spec == "none" {
            return LintSet::none();
        }
        let mut set = HashSet::new();
        for item in spec.split(',').map(str::trim).filter(|s| !s.is_empty()) {
            if item == "all" {
                set.extend(Lint::all().iter().copied());
            } else if let Some(off) = item.strip_prefix('-') {
                if let Some(l) = Lint::from_id(off) {
                    set.remove(&l);
                }
            } else if let Some(l) = Lint::from_id(item) {
                set.insert(l);
            }
        }
        LintSet(set)
    }

    fn is_enabled(&self, l: Lint) -> bool {
        self.0.contains(&l)
    }

    /// ¿Hay alguna categoría habilitada? Si no, el pase se saltea entero.
    pub fn is_empty(&self) -> bool {
        self.0.is_empty()
    }
}

/// Corre el lint sobre la unidad **ya atribuida** y devuelve los avisos (severidad `Warning`) de las
/// categorías habilitadas, respetando los `@SuppressWarnings` de las declaraciones envolventes.
pub fn lint(unit: &CompilationUnit, table: &SymbolTable, set: &LintSet) -> Vec<Error> {
    if set.is_empty() {
        return Vec::new();
    }
    // Los `SymbolId` de los elementos `@Deprecated` **de esta unidad** (los externos no se rastrean:
    // el símbolo no guarda sus anotaciones), para avisar en cada uso (categoría `deprecation`).
    let mut deprecated = HashSet::new();
    if set.is_enabled(Lint::Deprecation) {
        collect_deprecated(table, &unit.types, unit.package.as_deref().unwrap_or(""), &mut deprecated);
    }
    let base = unit.package.as_deref().unwrap_or("");
    let mut lx = Linter {
        table,
        set,
        out: Vec::new(),
        suppressed: Vec::new(),
        deprecated,
        top: None,
        current_class: None,
        current_return: None,
        class_subclassable: false,
        in_constructor: false,
        ctor_escaped: false,
    };
    for ty in &unit.types {
        // La **clase top-level** en curso: un uso de un `@Deprecated` de la **misma** top-level no
        // avisa (§9.6.4.6). Se fija acá y no cambia al bajar a los tipos anidados.
        let fqn = if base.is_empty() { ty.name.clone() } else { format!("{base}.{}", ty.name) };
        lx.top = table.class(&fqn);
        lx.class(ty, &fqn);
    }
    lx.out
}

struct Linter<'a> {
    table: &'a SymbolTable,
    set: &'a LintSet,
    out: Vec<Error>,
    /// Pila de conjuntos de categorías silenciadas por los `@SuppressWarnings` de las declaraciones
    /// envolventes. Un aviso se emite solo si su categoría no está en **ninguno** de los marcos.
    suppressed: Vec<HashSet<String>>,
    /// Los `SymbolId` de los elementos `@Deprecated` de la unidad (tipos/métodos/campos).
    deprecated: HashSet<SymbolId>,
    /// La clase **top-level** que se está recorriendo — para no avisar sobre un `@Deprecated` de la
    /// misma top-level (§9.6.4.6).
    top: Option<SymbolId>,
    /// La clase **en curso** (puede ser una anidada) — su scope de miembros resuelve los nombres de
    /// tipo para el aviso `rawtypes`.
    current_class: Option<SymbolId>,
    /// El tipo de **retorno** del método en curso (resuelto) — destino de una conversión unchecked en
    /// un `return`.
    current_return: Option<RType>,
    /// La clase en curso es **subclaseable externamente** (`public`, no `final`, `class`) — condición
    /// para `this-escape`.
    class_subclassable: bool,
    /// Se está recorriendo el cuerpo de un **constructor** — `this-escape` solo aplica ahí.
    in_constructor: bool,
    /// Ya se avisó `this-escape` en el constructor en curso — javac coalesce a **uno** por constructor.
    ctor_escaped: bool,
}

/// La clase **top-level** que encierra a `id`: sube por la cadena de `owner` mientras el dueño sea una
/// clase. Un método/campo llega a su clase; una anidada, a su ancestro top-level.
fn top_level_class(table: &SymbolTable, id: SymbolId) -> SymbolId {
    let mut cur = id;
    while let Some(owner) = table.symbol(cur).owner {
        if matches!(table.symbol(owner).kind, SymbolKind::Class { .. }) {
            cur = owner;
        } else {
            break;
        }
    }
    cur
}

/// ¿La lista de anotaciones incluye `@Deprecated` (§9.6.4.6)?
fn has_deprecated(annotations: &[Annotation]) -> bool {
    annotations
        .iter()
        .any(|a| a.name == "Deprecated" || a.name == "java.lang.Deprecated")
}

/// Recolecta los `SymbolId` de los elementos **`@Deprecated`** de la unidad —tipos, métodos y
/// campos—, recursivo por los tipos anidados. Un método se ubica en el scope de su clase por
/// **nombre + aridad** (suficiente para distinguir sobrecargas en la práctica).
fn collect_deprecated(
    table: &SymbolTable,
    types: &[ClassDecl],
    enclosing: &str,
    out: &mut HashSet<SymbolId>,
) {
    for class in types {
        let fqn = if enclosing.is_empty() {
            class.name.clone()
        } else {
            format!("{enclosing}.{}", class.name)
        };
        let Some(cid) = table.class(&fqn) else { continue };
        if has_deprecated(&class.annotations) {
            out.insert(cid);
        }
        let scope = match &table.symbol(cid).kind {
            SymbolKind::Class { members, .. } => *members,
            _ => continue,
        };
        for member in &class.members {
            match member {
                Member::Field(f) if has_deprecated(&f.annotations) => {
                    for &sid in table.scope(scope).get(&f.name) {
                        if matches!(table.symbol(sid).kind, SymbolKind::Field { .. }) {
                            out.insert(sid);
                        }
                    }
                }
                Member::Method(m) if has_deprecated(&m.annotations) => {
                    for &sid in table.scope(scope).get(&m.name) {
                        if let SymbolKind::Method { params, .. } = &table.symbol(sid).kind {
                            if params.len() == m.params.len() {
                                out.insert(sid);
                            }
                        }
                    }
                }
                Member::Type(nested) => {
                    collect_deprecated(table, std::slice::from_ref(nested), &fqn, out)
                }
                _ => {}
            }
        }
    }
}

/// Las categorías que silencia un `@SuppressWarnings(...)` de una lista de anotaciones: los `String`
/// de su valor (único o arreglo). `"all"` silencia todas.
fn suppressed_by(annotations: &[Annotation]) -> HashSet<String> {
    let mut out = HashSet::new();
    for a in annotations {
        if a.name != "SuppressWarnings" && a.name != "java.lang.SuppressWarnings" {
            continue;
        }
        for arg in &a.args {
            collect_string_values(&arg.value, &mut out);
        }
    }
    out
}

fn collect_string_values(v: &AnnotationValue, out: &mut HashSet<String>) {
    match v {
        AnnotationValue::Expr(e) => {
            if let ExprKind::StringLit(s) = &e.kind {
                out.insert(s.clone());
            }
        }
        AnnotationValue::Array(items) => {
            for it in items {
                collect_string_values(it, out);
            }
        }
        AnnotationValue::Nested(_) => {}
    }
}

impl Linter<'_> {
    /// Emite un aviso de la categoría `l` si está habilitada y no la silencia ningún
    /// `@SuppressWarnings` envolvente.
    fn warn(&mut self, l: Lint, pos: Pos, message: impl Into<String>) {
        if !self.set.is_enabled(l) {
            return;
        }
        let id = l.id();
        let silenced = self
            .suppressed
            .iter()
            .any(|frame| frame.contains(id) || frame.contains("all"));
        if silenced {
            return;
        }
        self.out.push(Error::warning(message, pos.line, pos.col));
    }

    /// Ejecuta `f` con las categorías silenciadas por `annotations` empujadas al marco de supresión.
    fn with_suppressed(&mut self, annotations: &[Annotation], f: impl FnOnce(&mut Self)) {
        self.suppressed.push(suppressed_by(annotations));
        f(self);
        self.suppressed.pop();
    }

    fn class(&mut self, class: &ClassDecl, fqn: &str) {
        let saved = self.current_class;
        let saved_sub = self.class_subclassable;
        self.current_class = self.table.class(fqn);
        // `this-escape`: solo una clase **pública, no final** puede tener un subtipo **externo** que
        // sobreescriba (un `enum`/`record`/interfaz o una clase final/package-private, no).
        self.class_subclassable = class.kind == TypeKind::Class
            && class.modifiers.contains(&Modifier::Public)
            && !class.modifiers.contains(&Modifier::Final);
        let anns = class.annotations.clone();
        self.with_suppressed(&anns, |lx| {
            lx.check_serial(class); // `serial`: clase `Serializable` sin `serialVersionUID` bien declarado
            for member in &class.members {
                match member {
                    Member::Method(m) => lx.method(m),
                    Member::Type(nested) => {
                        let nested_fqn = format!("{fqn}.{}", nested.name);
                        lx.class(nested, &nested_fqn);
                    }
                    Member::Field(f) => {
                        lx.with_suppressed(&f.annotations, |lx| {
                            lx.check_raw(&f.ty, f.pos); // `rawtypes`: tipo del campo
                            if let Some(init) = &f.init {
                                lx.expr(init);
                            }
                        });
                    }
                    Member::StaticInit(b) | Member::InstanceInit(b) => lx.block(b),
                }
            }
        });
        self.current_class = saved;
        self.class_subclassable = saved_sub;
    }

    fn method(&mut self, m: &MethodDecl) {
        let anns = m.annotations.clone();
        // El retorno resuelto es el **destino** de una conversión unchecked en un `return`.
        let saved_ret = self.current_return.take();
        self.current_return =
            (!m.is_constructor).then(|| self.resolve_rtype(&m.return_type)).flatten();
        // `this-escape`: se rastrea solo dentro de un constructor, con un aviso **por** constructor.
        let saved_ctor = self.in_constructor;
        let saved_esc = self.ctor_escaped;
        self.in_constructor = m.is_constructor;
        self.ctor_escaped = false;
        self.with_suppressed(&anns, |lx| {
            // `rawtypes`: retorno y parámetros (los `Type` sintácticos no llevan posición propia, así
            // que el aviso se ancla en la declaración del método).
            if !m.is_constructor {
                lx.check_raw(&m.return_type, m.pos);
            }
            for p in &m.params {
                lx.check_raw(&p.ty, m.pos);
            }
            if let Some(body) = &m.body {
                lx.block(body);
            }
        });
        self.current_return = saved_ret;
        self.in_constructor = saved_ctor;
        self.ctor_escaped = saved_esc;
    }

    /// Resuelve un `Type` sintáctico a [`RType`] en el scope de la clase en curso. `None` si no hay
    /// clase (o el tipo no resuelve — queda `Unresolved`, que los chequeos tratan como "no opino").
    fn resolve_rtype(&self, ty: &Type) -> Option<RType> {
        let scope = match self.current_class.map(|c| &self.table.symbol(c).kind) {
            Some(SymbolKind::Class { members, .. }) => *members,
            _ => return None,
        };
        Some(super::attribute::resolve_rtype(self.table, scope, ty))
    }

    /// `unchecked`: una **conversión** de un tipo raw a uno parametrizado del mismo genérico (§5.1.9).
    /// `value` es la expresión origen; `target` el tipo destino ya resuelto.
    fn check_unchecked_conversion(&mut self, target: &RType, value: &Expr) {
        // `this`/`super` en el cuerpo de un genérico se tipan como el `RType::Class(id)` **borrado**,
        // pero **no** son un uso raw (son `C<T>`): no disparan la conversión.
        if is_self_expr(value) {
            return;
        }
        if let Some(src) = value.ty.as_ref() {
            if is_unchecked_conversion(self.table, src, target) {
                self.warn(Lint::Unchecked, value.pos, "conversión unchecked (tipo raw a parametrizado)");
            }
        }
    }

    /// `serial`: una **clase** (no `enum`/`record`/interfaz) que implementa `Serializable` debe declarar
    /// un `serialVersionUID` bien formado (`static final long`). Se detecta la cláusula `implements`
    /// **directa** por nombre (`Serializable`/`java.io.Serializable`); el caso **transitivo** (heredarlo
    /// de un supertipo) queda afuera —pediría el grafo de tipos, que `Serializable` externo no completa—.
    fn check_serial(&mut self, class: &ClassDecl) {
        if class.kind != TypeKind::Class {
            return; // los `enum`/`record` tienen serialización propia; javac no avisa
        }
        let serializable = class
            .implements
            .iter()
            .any(|t| matches!(type_simple_name(t), "Serializable"));
        if !serializable {
            return;
        }
        let suid = class.members.iter().find_map(|m| match m {
            Member::Field(f) if f.name == "serialVersionUID" => Some(f),
            _ => None,
        });
        match suid {
            None => self.warn(
                Lint::Serial,
                class.pos,
                format!("la clase serializable `{}` no define `serialVersionUID`", class.name),
            ),
            Some(f) => {
                let well_formed = f.modifiers.contains(&Modifier::Static)
                    && f.modifiers.contains(&Modifier::Final)
                    && matches!(f.ty, Type::Prim(PrimType::Long));
                if !well_formed {
                    self.warn(
                        Lint::Serial,
                        f.pos,
                        "`serialVersionUID` debe declararse `static final long`",
                    );
                }
            }
        }
    }

    fn block(&mut self, block: &Block) {
        for s in &block.0 {
            self.stmt(s);
        }
    }

    fn stmt(&mut self, s: &Stmt) {
        match &s.kind {
            // `empty`: una sentencia vacía como **cuerpo** de un if/while/for/do (§14.6) — casi siempre
            // un `;` de más (`if (c);`). No aplica a un `;` suelto en un bloque.
            StmtKind::If { cond, then, els } => {
                self.expr(cond);
                self.check_empty_body(then);
                self.stmt(then);
                if let Some(e) = els {
                    self.stmt(e);
                }
            }
            StmtKind::While { cond, body } => {
                self.expr(cond);
                self.check_empty_body(body);
                self.stmt(body);
            }
            StmtKind::Do { body, cond } => {
                self.check_empty_body(body);
                self.stmt(body);
                self.expr(cond);
            }
            StmtKind::For { init, cond, update, body } => {
                if let Some(i) = init {
                    self.stmt(i);
                }
                if let Some(c) = cond {
                    self.expr(c);
                }
                for u in update {
                    self.expr(u);
                }
                self.check_empty_body(body);
                self.stmt(body);
            }
            StmtKind::ForEach { iterable, body, .. } => {
                self.expr(iterable);
                self.check_empty_body(body);
                self.stmt(body);
            }
            StmtKind::Block(b) => self.block(b),
            StmtKind::LocalVar { ty, init, .. } => {
                self.check_raw(ty, s.pos); // `rawtypes`: tipo de la variable local
                if let Some(e) = init {
                    // `unchecked`: `Box<String> b = raw();` (raw asignado a parametrizado).
                    if let Some(target) = self.resolve_rtype(ty) {
                        self.check_unchecked_conversion(&target, e);
                    }
                    self.expr(e);
                }
            }
            StmtKind::Expr(e) => self.expr(e),
            StmtKind::Return(Some(e)) => {
                if let Some(ret) = self.current_return.clone() {
                    self.check_unchecked_conversion(&ret, e); // `unchecked` en un `return`
                }
                self.expr(e);
            }
            StmtKind::Throw(e) => self.expr(e),
            StmtKind::Labeled { body, .. } => self.stmt(body),
            StmtKind::Yield(e) => self.expr(e),
            StmtKind::Synchronized { lock, body } => {
                self.expr(lock);
                self.block(body);
            }
            StmtKind::Switch { selector, cases } => {
                self.expr(selector);
                for (i, c) in cases.iter().enumerate() {
                    if let Some(g) = &c.guard {
                        self.expr(g);
                    }
                    match &c.body {
                        SwitchBody::Arrow(st) => self.stmt(st),
                        SwitchBody::Colon(sts) => {
                            sts.iter().for_each(|st| self.stmt(st));
                            // `fallthrough`: un grupo **no vacío** que **completa normalmente** (no
                            // termina en `break`/`return`/`throw`/`continue`/`yield`) y tiene un `case`
                            // **después** cae en él sin querer (§14.11.3). Un grupo vacío (`case A: case
                            // B:`) es un agrupamiento intencional y no avisa.
                            if let (Some(next), Some(last)) = (cases.get(i + 1), sts.last()) {
                                if completes_normally(last) {
                                    // Se apunta al `case` **receptor** (como javac), o al último enunciado
                                    // del grupo que cae si el receptor no tiene una posición clara.
                                    let pos = receiving_case_pos(next).unwrap_or(last.pos);
                                    self.warn(
                                        Lint::Fallthrough,
                                        pos,
                                        "posible caída a este `case` (¿falta un `break`?)",
                                    );
                                }
                            }
                        }
                    }
                }
            }
            StmtKind::Try { resources, body, catches, finally } => {
                for r in resources {
                    self.stmt(r);
                }
                self.block(body);
                for c in catches {
                    self.block(&c.body);
                }
                if let Some(f) = finally {
                    // `finally`: si el bloque **no completa normalmente** (termina en `return`/`throw`/
                    // `break`/`continue`) descarta lo que traía el `try` (§14.20.2). Un `finally` vacío
                    // completa normalmente y no avisa.
                    if let Some(last) = f.0.last() {
                        if !completes_normally(last) {
                            self.warn(
                                Lint::Finally,
                                last.pos,
                                "la cláusula `finally` no puede completar normalmente",
                            );
                        }
                    }
                    self.block(f);
                }
            }
            StmtKind::Assert { cond, message } => {
                self.expr(cond);
                if let Some(m) = message {
                    self.expr(m);
                }
            }
            _ => {}
        }
    }

    /// Avisa si `body` es una sentencia **vacía** (`;`).
    fn check_empty_body(&mut self, body: &Stmt) {
        if matches!(body.kind, StmtKind::Empty) {
            self.warn(Lint::Empty, body.pos, "sentencia vacía como cuerpo de control (`;` de más)");
        }
    }

    /// `rawtypes`: avisa si `ty` es el uso **raw** de un tipo genérico —un `Type::Class(n)` cuyo `n`
    /// resuelve a una clase con parámetros de tipo, sin haberlos dado (§4.8)—. Un `Type::Parameterized`
    /// (incluido el diamante `<>`) no es raw; un tipo no genérico (`String`) tampoco. Recurre por los
    /// arrays (`List[]` es raw en su elemento).
    fn check_raw(&mut self, ty: &Type, pos: Pos) {
        match ty {
            Type::Class(name) => {
                if let Some(cid) = self.resolve_type_name(name) {
                    if !types::type_params_of(self.table, cid).is_empty() {
                        self.warn(
                            Lint::Rawtypes,
                            pos,
                            format!("uso del tipo raw `{name}`: faltan sus argumentos de tipo"),
                        );
                    }
                }
            }
            Type::Array(inner) => self.check_raw(inner, pos),
            _ => {}
        }
    }

    /// Resuelve un nombre de tipo en el scope de miembros de la clase en curso, con respaldo a los
    /// tipos externos (como hace `check`). `None` si no resuelve (no se opina).
    fn resolve_type_name(&self, name: &str) -> Option<SymbolId> {
        let scope = match self.current_class.map(|c| &self.table.symbol(c).kind) {
            Some(SymbolKind::Class { members, .. }) => *members,
            _ => return None,
        };
        self.table.resolve_type(scope, name).or_else(|| self.table.external(name))
    }

    fn expr(&mut self, e: &Expr) {
        // `deprecation`: un uso que **vincula** (§9.6.4.6) a un elemento `@Deprecated` de la unidad.
        if !self.deprecated.is_empty() {
            let sym = match e.binding {
                Some(Binding::Field(s)) | Some(Binding::Method(s)) | Some(Binding::Class(s)) => Some(s),
                _ => None,
            };
            if let Some(s) = sym {
                // No se avisa si el uso vive en la **misma clase top-level** que la declaración
                // `@Deprecated` (§9.6.4.6).
                let same_top = self.top == Some(top_level_class(self.table, s));
                if self.deprecated.contains(&s) && !same_top {
                    let name = self.table.symbol(s).name.clone();
                    self.warn(Lint::Deprecation, e.pos, format!("`{name}` está marcado como obsoleto (`@Deprecated`)"));
                }
            }
        }
        if let ExprKind::Cast { expr: inner, .. } = &e.kind {
            if let (Some(target), Some(src)) = (e.ty.as_ref(), inner.ty.as_ref()) {
                // `cast`: un cast de **referencia** cuyo operando ya es del tipo destino (o un subtipo)
                // es redundante (§5.1.5/§5.5). Un cast primitivo (`(int) d`) convierte el valor, nunca sobra.
                if is_reference(target) && is_reference(src) && types::is_subtype(self.table, src, target)
                {
                    self.warn(Lint::Cast, e.pos, "cast redundante: el operando ya es de ese tipo");
                }
                // `unchecked`: un cast a un tipo **parametrizado no reificable** (con algún argumento
                // que no es `?`) que el operando no satisface estáticamente (§5.5.1) — no se puede
                // chequear en runtime.
                else if is_unchecked_cast(target) && !types::is_subtype(self.table, src, target) {
                    self.warn(Lint::Unchecked, e.pos, "cast unchecked a un tipo parametrizado");
                }
            }
        }
        // `unchecked`: una **llamada** a un método con parámetros que mencionan un type-var de la clase,
        // sobre un receptor de tipo **raw** (§4.8): la firma real está borrada, el argumento no se chequea.
        if let ExprKind::Call { target: Some(recv), name, .. } = &e.kind {
            // Un receptor `this`/`super` en un genérico se tipa borrado pero **no** es raw.
            if let (false, Some(RType::Class(cid)), Some(Binding::Method(m))) =
                (is_self_expr(recv), recv.ty.as_ref(), e.binding)
            {
                if !types::type_params_of(self.table, *cid).is_empty()
                    && method_has_class_typevar_param(self.table, m, *cid)
                {
                    self.warn(
                        Lint::Unchecked,
                        e.pos,
                        format!("llamada unchecked a `{name}` sobre un tipo raw"),
                    );
                }
            }
        }
        // `this-escape`: dentro de un constructor de una clase subclaseable externamente, una llamada
        // **sin calificar** o por `this` a un método sobreescribible por un subtipo externo deja
        // escapar `this` a código de la subclase antes de que esté inicializada. Un aviso por
        // constructor (javac coalesce).
        if self.in_constructor && self.class_subclassable && !self.ctor_escaped {
            if let ExprKind::Call { target, .. } = &e.kind {
                let on_this = match target {
                    None => true,
                    Some(t) => matches!(t.kind, ExprKind::This),
                };
                if on_this {
                    if let Some(Binding::Method(m)) = e.binding {
                        if is_externally_overridable(self.table, m) {
                            self.warn(
                                Lint::ThisEscape,
                                e.pos,
                                "posible escape de `this` antes de que la subclase esté inicializada",
                            );
                            self.ctor_escaped = true;
                        }
                    }
                }
            }
        }
        // Recursión por las sub-expresiones.
        match &e.kind {
            ExprKind::Binary { lhs, rhs, .. } => {
                self.expr(lhs);
                self.expr(rhs);
            }
            ExprKind::Unary { expr, .. } | ExprKind::Cast { expr, .. } => self.expr(expr),
            ExprKind::Assign { target, value, .. } => {
                // `unchecked`: `p = raw();` con `p` de tipo parametrizado.
                if let Some(t) = target.ty.as_ref() {
                    let t = t.clone();
                    self.check_unchecked_conversion(&t, value);
                }
                self.expr(target);
                self.expr(value);
            }
            ExprKind::Ternary { cond, then, els } => {
                self.expr(cond);
                self.expr(then);
                self.expr(els);
            }
            ExprKind::Call { target, args, .. } => {
                if let Some(t) = target {
                    self.expr(t);
                }
                args.iter().for_each(|a| self.expr(a));
            }
            ExprKind::Field { expr, .. } => self.expr(expr),
            ExprKind::Index { array, index } => {
                self.expr(array);
                self.expr(index);
            }
            ExprKind::InstanceOf { expr, .. } => self.expr(expr),
            ExprKind::NewObject { ty, args, .. } => {
                self.check_raw(ty, e.pos); // `rawtypes`: `new List()` sin argumentos
                args.iter().for_each(|a| self.expr(a));
            }
            ExprKind::NewArray { elem, dims, init, .. } => {
                self.check_raw(elem, e.pos); // `rawtypes`: `new List[]`
                dims.iter().flatten().for_each(|d| self.expr(d));
                if let Some(es) = init {
                    es.iter().for_each(|x| self.expr(x));
                }
            }
            _ => {}
        }
    }
}

/// ¿La sentencia puede **completar normalmente** (§14.22)? Aproximación conservadora para el aviso
/// `fallthrough`: `break`/`continue`/`return`/`throw`/`yield` **no** completan; un bloque/`if` delega
/// en su última rama; el resto (asignaciones, bucles, `switch`, `try`…) se toma como que **sí**
/// completa. Ante la duda devuelve `true`, así solo se avisa cuando la caída es clara.
fn completes_normally(s: &Stmt) -> bool {
    match &s.kind {
        StmtKind::Break(_) | StmtKind::Continue(_) | StmtKind::Return(_) | StmtKind::Throw(_)
        | StmtKind::Yield(_) => false,
        StmtKind::Block(b) => b.0.last().map_or(true, completes_normally),
        StmtKind::Labeled { body, .. } => completes_normally(body),
        StmtKind::Synchronized { body, .. } => b_last_completes(&body.0),
        // Un `if` sin `else` siempre puede completar (por la rama ausente); con `else`, si **alguna**
        // rama completa.
        StmtKind::If { then, els: Some(e), .. } => completes_normally(then) || completes_normally(e),
        StmtKind::If { els: None, .. } => true,
        _ => true,
    }
}

fn b_last_completes(stmts: &[Stmt]) -> bool {
    stmts.last().map_or(true, completes_normally)
}

/// La posición del `case` **receptor** de una caída, para anclar el aviso `fallthrough` como javac:
/// la de su primera etiqueta constante (`case 2:`), o la de su primer enunciado. `None` si no tiene
/// ninguna (un grupo vacío `default:` sin cuerpo), y el llamador usa un respaldo.
fn receiving_case_pos(case: &SwitchCase) -> Option<Pos> {
    for l in &case.labels {
        if let CaseLabel::Constant(e) = l {
            return Some(e.pos);
        }
    }
    if let SwitchBody::Colon(sts) = &case.body {
        if let Some(first) = sts.first() {
            return Some(first.pos);
        }
    }
    None
}

/// ¿La expresión es `this` o `super`? Su tipo estático dentro de un genérico se representa borrado
/// (`RType::Class(id)`), pero **no** es un uso raw, así que no debe disparar avisos `unchecked`.
fn is_self_expr(e: &Expr) -> bool {
    matches!(e.kind, ExprKind::This | ExprKind::Super)
}

/// ¿El método `m` puede ser **sobreescrito por un subtipo externo**? (Para `this-escape`.) Lo es un
/// método de **instancia** accesible desde afuera (`public`/`protected`), no `final` ni `private`.
fn is_externally_overridable(table: &SymbolTable, m: SymbolId) -> bool {
    let mods = &table.symbol(m).modifiers;
    !mods.contains(&Modifier::Static)
        && !mods.contains(&Modifier::Private)
        && !mods.contains(&Modifier::Final)
        && (mods.contains(&Modifier::Public) || mods.contains(&Modifier::Protected))
}

/// El **nombre simple** de un tipo escrito (el último segmento tras el `.`): `java.io.Serializable`
/// → `Serializable`. Para reconocer la cláusula `implements` sin resolver el tipo.
fn type_simple_name(t: &Type) -> &str {
    let name = match t {
        Type::Class(n) | Type::Parameterized { base: n, .. } => n.as_str(),
        _ => return "",
    };
    name.rsplit('.').next().unwrap_or(name)
}

/// `unchecked`: ¿convertir de `src` a `target` es una conversión sin chequear (§5.1.9)? Lo es cuando
/// `src` es el uso **raw** de un genérico (`RType::Class(id)` con `id` genérico) y `target` es una
/// **parametrización del mismo genérico** (`RType::Parameterized { base: id }`).
fn is_unchecked_conversion(table: &SymbolTable, src: &RType, target: &RType) -> bool {
    if let (RType::Class(sid), RType::Parameterized { base, .. }) = (src, target) {
        return base == sid && !types::type_params_of(table, *sid).is_empty();
    }
    false
}

/// `unchecked`: ¿un cast a `rt` no es reificable (§4.7)? Lo es un tipo **parametrizado** con algún
/// argumento que **no** es el comodín `?` (un `<?>` sí es reificable).
fn is_unchecked_cast(rt: &RType) -> bool {
    matches!(rt, RType::Parameterized { args, .. } if args.iter().any(|a| !matches!(a, RTypeArg::Wildcard)))
}

/// ¿El método `m` tiene algún parámetro que menciona un **type-var de la clase `cid`**? (Los type-vars
/// del **propio método** no cuentan: se infieren aparte, no los borra el uso raw del receptor.)
fn method_has_class_typevar_param(table: &SymbolTable, m: SymbolId, cid: SymbolId) -> bool {
    let class_tvs: std::collections::HashSet<SymbolId> =
        types::type_params_of(table, cid).into_iter().collect();
    matches!(
        table.resolved(m),
        Some(Resolved::Method { params, .. })
            if params.iter().any(|p| mentions_typevar(p, &class_tvs))
    )
}

/// ¿`rt` menciona alguno de los type-vars de `tvs` (directo o dentro de un array/parametrización)?
fn mentions_typevar(rt: &RType, tvs: &std::collections::HashSet<SymbolId>) -> bool {
    match rt {
        RType::TypeVar(id) => tvs.contains(id),
        RType::Array(e) => mentions_typevar(e, tvs),
        RType::Parameterized { args, .. } => args.iter().any(|a| match a {
            RTypeArg::Type(t) => mentions_typevar(t, tvs),
            RTypeArg::Extends(t) | RTypeArg::Super(t) => mentions_typevar(t, tvs),
            RTypeArg::Wildcard => false,
        }),
        _ => false,
    }
}

/// ¿`t` es un tipo **referencia** (clase/array/variable de tipo/…)? Un cast entre primitivos convierte
/// el valor, así que nunca es "redundante"; solo se avisa sobre referencias.
fn is_reference(t: &super::symbol::RType) -> bool {
    use super::symbol::RType;
    matches!(
        t,
        RType::Class(_)
            | RType::Parameterized { .. }
            | RType::Array(_)
            | RType::TypeVar(_)
            | RType::Capture { .. }
            | RType::Intersection(_)
    )
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Corre el lint con `spec` (`-Xlint:<spec>`) y devuelve los mensajes de aviso.
    fn warns(src: &str, spec: &str) -> Vec<String> {
        let set = LintSet::from_spec(spec);
        crate::javac::lint_source(src, &set)
            .expect("el fuente debe parsear")
            .into_iter()
            .map(|w| w.message)
            .collect()
    }

    #[test]
    fn empty_body_of_an_if_is_warned() {
        let w = warns("class C { void m(int p) { if (p > 0); } }", "empty");
        assert_eq!(w.len(), 1, "{w:?}");
        assert!(w[0].contains("vacía"));
    }

    #[test]
    fn empty_body_of_a_while_and_for_is_warned() {
        assert_eq!(warns("class C { void m(int p) { while (p > 0); } }", "empty").len(), 1);
        assert_eq!(warns("class C { void m() { for (int i = 0; i < 3; i++); } }", "empty").len(), 1);
    }

    #[test]
    fn a_real_if_body_is_not_warned() {
        // Un cuerpo real (bloque o sentencia) no dispara `empty`; tampoco un `;` suelto en un bloque.
        assert!(warns("class C { void m(int p) { if (p > 0) {} ; } }", "empty").is_empty());
    }

    #[test]
    fn a_redundant_cast_is_warned() {
        // `(String) s` con `s` ya `String`: cast redundante.
        let w = warns("class C { String m(String s) { return (String) s; } }", "cast");
        assert_eq!(w.len(), 1, "{w:?}");
        assert!(w[0].contains("redundante"));
    }

    #[test]
    fn a_widening_or_narrowing_cast_is_not_warned() {
        // Cast que **sí** hace algo: a un supertipo distinto no se avisa (no es identidad), y un cast
        // de estrechamiento (`(String) obj`) tampoco (Object no es subtipo de String).
        assert!(warns("class C { Object m(String s) { return s; } }", "cast").is_empty());
        assert!(warns("class C { String m(Object o) { return (String) o; } }", "cast").is_empty());
    }

    #[test]
    fn a_primitive_cast_is_never_redundant() {
        // `(int) d` convierte el valor: nunca es un cast redundante.
        assert!(warns("class C { int m(double d) { return (int) d; } }", "cast").is_empty());
    }

    #[test]
    fn suppresswarnings_silences_the_category() {
        // `@SuppressWarnings` en el método silencia su categoría; `all` silencia todo.
        assert!(warns("class C { @SuppressWarnings(\"empty\") void m(int p) { if (p > 0); } }", "empty").is_empty());
        assert!(warns("class C { @SuppressWarnings(\"all\") void m(int p) { if (p > 0); } }", "empty").is_empty());
        // Pero una categoría **distinta** no lo silencia.
        assert_eq!(warns("class C { @SuppressWarnings(\"cast\") void m(int p) { if (p > 0); } }", "empty").len(), 1);
    }

    #[test]
    fn a_disabled_category_produces_no_warning() {
        // Sin la categoría habilitada (`-Xlint:none` o un spec que no la incluye), no hay aviso.
        assert!(warns("class C { void m(int p) { if (p > 0); } }", "none").is_empty());
        assert!(warns("class C { void m(int p) { if (p > 0); } }", "cast").is_empty());
    }

    #[test]
    fn xlint_all_enables_every_category() {
        // `-Xlint:all` (o `-Xlint` a secas) prende todo: el `empty` y el `cast` salen juntos.
        let w = warns(
            "class C { String m(String s, int p) { if (p > 0); return (String) s; } }",
            "all",
        );
        assert_eq!(w.len(), 2, "{w:?}");
    }

    // ---- fallthrough ----

    #[test]
    fn a_case_that_falls_through_is_warned() {
        let w = warns(
            "class C { int m(int p) { int x = 0; switch (p) { \
             case 1: x = 1; case 2: x = 2; break; } return x; } }",
            "fallthrough",
        );
        assert_eq!(w.len(), 1, "{w:?}");
        assert!(w[0].contains("caída"));
    }

    #[test]
    fn cases_that_break_or_return_do_not_fall_through() {
        // Cada grupo termina abrupto (break/return): sin caída.
        assert!(warns(
            "class C { int m(int p) { int x = 0; switch (p) { \
             case 1: x = 1; break; case 2: return 2; } return x; } }",
            "fallthrough",
        )
        .is_empty());
    }

    #[test]
    fn an_empty_case_group_does_not_warn() {
        // `case 1: case 2:` es un agrupamiento intencional (grupo vacío): no avisa.
        assert!(warns(
            "class C { int m(int p) { int x = 0; switch (p) { \
             case 1: case 2: x = 2; break; } return x; } }",
            "fallthrough",
        )
        .is_empty());
    }

    #[test]
    fn an_arrow_switch_never_falls_through() {
        assert!(warns(
            "class C { int m(int p) { int x = 0; switch (p) { \
             case 1 -> x = 1; case 2 -> x = 2; default -> x = 9; } return x; } }",
            "fallthrough",
        )
        .is_empty());
    }

    // ---- deprecation ----

    #[test]
    fn using_a_deprecated_element_from_another_class_is_warned() {
        let w = warns(
            "class A { @Deprecated static int old() { return 1; } } \
             class B { int m() { return A.old(); } }",
            "deprecation",
        );
        assert_eq!(w.len(), 1, "{w:?}");
        assert!(w[0].contains("obsoleto"));
    }

    #[test]
    fn using_a_deprecated_element_within_the_same_top_level_class_is_not_warned() {
        // §9.6.4.6: el uso dentro de la misma clase top-level (incluida una anidada) no avisa.
        assert!(warns(
            "class D { @Deprecated static int old() { return 1; } int m() { return old(); } }",
            "deprecation",
        )
        .is_empty());
        assert!(warns(
            "class A { @Deprecated static int old() { return 1; } \
             static class Inner { int m() { return old(); } } }",
            "deprecation",
        )
        .is_empty());
    }

    #[test]
    fn a_deprecated_field_use_is_warned_across_classes() {
        let w = warns(
            "class A { @Deprecated static int F = 1; } class B { int m() { return A.F; } }",
            "deprecation",
        );
        assert_eq!(w.len(), 1, "{w:?}");
    }

    #[test]
    fn suppresswarnings_deprecation_silences_the_use() {
        assert!(warns(
            "class A { @Deprecated static int old() { return 1; } } \
             class B { @SuppressWarnings(\"deprecation\") int m() { return A.old(); } }",
            "deprecation",
        )
        .is_empty());
    }

    // ---- rawtypes ----

    #[test]
    fn raw_generic_uses_are_warned_in_every_position() {
        // Campo, retorno, parámetro, local y `new` sin argumentos de tipo → 5 avisos (como javac).
        let w = warns(
            "class Box<T> { T v; } \
             class U { Box f; Box m(Box p) { Box local = new Box(); return local; } }",
            "rawtypes",
        );
        assert_eq!(w.len(), 5, "{w:?}");
        assert!(w[0].contains("raw"));
    }

    #[test]
    fn a_parameterized_or_diamond_use_is_not_raw() {
        assert!(warns(
            "class Box<T> { T v; } class U { Box<String> f; Box<String> m() { return new Box<String>(); } }",
            "rawtypes",
        )
        .is_empty());
        // El diamante tampoco es raw.
        assert!(warns(
            "class Box<T> { T v; } class U { Box<String> m() { Box<String> b = new Box<>(); return b; } }",
            "rawtypes",
        )
        .is_empty());
    }

    #[test]
    fn a_non_generic_type_is_never_raw() {
        assert!(warns("class U { String f; Object m(int p) { return f; } }", "rawtypes").is_empty());
    }

    #[test]
    fn a_raw_array_element_is_warned() {
        let w = warns("class Box<T> { T v; } class U { Box[] a; }", "rawtypes");
        assert_eq!(w.len(), 1, "{w:?}");
    }

    #[test]
    fn suppresswarnings_rawtypes_silences_the_use() {
        assert!(warns(
            "class Box<T> { T v; } class U { @SuppressWarnings(\"rawtypes\") Box f; }",
            "rawtypes",
        )
        .is_empty());
    }

    // ---- unchecked ----

    const BOX: &str = "class Box<T> { T v; void set(T x) { v = x; } Box<T> self() { return this; } } ";

    #[test]
    fn an_unchecked_conversion_from_raw_to_parameterized_is_warned() {
        let w = warns(
            &format!("{BOX} class U {{ Box raw() {{ return new Box(); }} \
                      void a() {{ Box<String> p = raw(); p.set(\"x\"); }} }}"),
            "unchecked",
        );
        assert_eq!(w.len(), 1, "{w:?}");
        assert!(w[0].contains("conversión"));
    }

    #[test]
    fn an_unchecked_call_on_a_raw_receiver_is_warned() {
        let w = warns(
            &format!("{BOX} class U {{ void b(Box raw) {{ raw.set(\"x\"); }} }}"),
            "unchecked",
        );
        assert_eq!(w.len(), 1, "{w:?}");
        assert!(w[0].contains("llamada"));
    }

    #[test]
    fn an_unchecked_cast_to_a_parameterized_type_is_warned() {
        let w = warns(
            &format!("{BOX} class U {{ void c(Object o) {{ Box<String> p = (Box<String>) o; p.set(\"x\"); }} }}"),
            "unchecked",
        );
        assert_eq!(w.len(), 1, "{w:?}");
        assert!(w[0].contains("cast"));
    }

    #[test]
    fn a_call_on_a_parameterized_receiver_is_not_unchecked() {
        assert!(warns(
            &format!("{BOX} class U {{ void d(Box<String> p) {{ p.set(\"ok\"); }} }}"),
            "unchecked",
        )
        .is_empty());
    }

    #[test]
    fn a_cast_to_an_unbounded_wildcard_is_reifiable_and_not_unchecked() {
        // `(Box<?>) o` es reificable: no avisa unchecked.
        assert!(warns(
            &format!("{BOX} class U {{ void c(Object o) {{ Box<?> p = (Box<?>) o; }} }}"),
            "unchecked",
        )
        .is_empty());
    }

    #[test]
    fn suppresswarnings_unchecked_silences_the_operation() {
        assert!(warns(
            &format!("{BOX} class U {{ Box raw() {{ return new Box(); }} \
                      @SuppressWarnings(\"unchecked\") void a() {{ Box<String> p = raw(); }} }}"),
            "unchecked",
        )
        .is_empty());
    }

    // ---- finally ----

    #[test]
    fn a_finally_that_returns_or_throws_is_warned() {
        assert_eq!(
            warns("class C { int a() { try { return 1; } finally { return 2; } } }", "finally").len(),
            1,
        );
        assert_eq!(
            warns("class C { void b() { try {} finally { throw new RuntimeException(); } } }", "finally").len(),
            1,
        );
    }

    #[test]
    fn a_finally_that_breaks_is_warned() {
        assert_eq!(
            warns(
                "class C { void d(int p) { while (p > 0) { try {} finally { break; } } } }",
                "finally",
            )
            .len(),
            1,
        );
    }

    #[test]
    fn a_finally_that_completes_normally_is_not_warned() {
        assert!(warns("class C { int c() { try { return 1; } finally { int x = 0; } } }", "finally")
            .is_empty());
        // Un `finally` vacío completa normalmente.
        assert!(warns("class C { int c() { try { return 1; } finally {} } }", "finally").is_empty());
    }

    #[test]
    fn suppresswarnings_finally_silences_it() {
        assert!(warns(
            "class C { @SuppressWarnings(\"finally\") int a() { try { return 1; } finally { return 2; } } }",
            "finally",
        )
        .is_empty());
    }

    // ---- serial ----

    const SER: &str = "import java.io.Serializable; ";

    #[test]
    fn a_serializable_class_without_serialversionuid_is_warned() {
        let w = warns(&format!("{SER} class A implements Serializable {{}}"), "serial");
        assert_eq!(w.len(), 1, "{w:?}");
        assert!(w[0].contains("serialVersionUID"));
        // Una clase abstracta serializable también avisa.
        assert_eq!(
            warns(&format!("{SER} abstract class G implements Serializable {{}}"), "serial").len(),
            1,
        );
    }

    #[test]
    fn a_proper_serialversionuid_is_not_warned() {
        assert!(warns(
            &format!("{SER} class B implements Serializable {{ private static final long serialVersionUID = 1L; }}"),
            "serial",
        )
        .is_empty());
    }

    #[test]
    fn a_malformed_serialversionuid_is_warned() {
        // No `static final`: mal declarado.
        let w = warns(
            &format!("{SER} class C implements Serializable {{ long serialVersionUID = 1L; }}"),
            "serial",
        );
        assert_eq!(w.len(), 1, "{w:?}");
        assert!(w[0].contains("static final long"));
    }

    #[test]
    fn an_enum_or_non_serializable_class_is_not_warned() {
        assert!(warns(&format!("{SER} enum E implements Serializable {{ X }}"), "serial").is_empty());
        assert!(warns("class F {}", "serial").is_empty());
    }

    #[test]
    fn suppresswarnings_serial_silences_it() {
        assert!(warns(
            &format!("{SER} @SuppressWarnings(\"serial\") class A implements Serializable {{}}"),
            "serial",
        )
        .is_empty());
    }

    // ---- this-escape ----

    #[test]
    fn a_constructor_calling_an_overridable_method_is_warned() {
        let w = warns(
            "public class P { public P() { m(); } public void m() {} }",
            "this-escape",
        );
        assert_eq!(w.len(), 1, "{w:?}");
        assert!(w[0].contains("this"));
        // `this.m()` explícito también.
        assert_eq!(
            warns("public class P { public P() { this.m(); } public void m() {} }", "this-escape").len(),
            1,
        );
        // `protected` también es sobreescribible externamente.
        assert_eq!(
            warns("public class P { public P() { m(); } protected void m() {} }", "this-escape").len(),
            1,
        );
    }

    #[test]
    fn a_final_private_or_static_target_does_not_escape() {
        assert!(warns("public class P { public P() { m(); } public final void m() {} }", "this-escape").is_empty());
        assert!(warns("public class P { public P() { m(); } private void m() {} }", "this-escape").is_empty());
        assert!(warns("public class P { public P() { s(); } public static void s() {} }", "this-escape").is_empty());
        // package-private (no accesible a un subtipo externo) tampoco.
        assert!(warns("public class P { public P() { m(); } void m() {} }", "this-escape").is_empty());
    }

    #[test]
    fn a_non_subclassable_class_does_not_escape() {
        // Clase final o package-private: no hay subtipo externo que sobreescriba.
        assert!(warns("public final class P { public P() { m(); } public void m() {} }", "this-escape").is_empty());
        assert!(warns("class P { P() { m(); } public void m() {} }", "this-escape").is_empty());
    }

    #[test]
    fn multiple_escapes_in_one_constructor_warn_once() {
        // javac coalesce a un aviso por constructor.
        let w = warns(
            "public class P { public P() { a(); b(); } public void a() {} public void b() {} }",
            "this-escape",
        );
        assert_eq!(w.len(), 1, "{w:?}");
    }

    #[test]
    fn suppresswarnings_this_escape_silences_it() {
        assert!(warns(
            "public class P { @SuppressWarnings(\"this-escape\") public P() { m(); } public void m() {} }",
            "this-escape",
        )
        .is_empty());
    }
}
