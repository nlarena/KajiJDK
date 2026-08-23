//! La **pasada 1** del análisis semántico: recorre el AST y construye la
//! [`SymbolTable`](super::symbol::SymbolTable) — **sin mirar los cuerpos** de los métodos.
//!
//! Se subdivide en dos, en este orden (no negociable):
//! - **Enter**: crea un símbolo de clase por cada tipo declarado, para que el *namespace de
//!   tipos* esté completo antes de resolver cualquier contenido (referencias mutuas).
//! - **MemberEnter**: entra los miembros (campos, métodos, constructores) de cada clase a su
//!   scope, registrando su **firma**.
//!
//! Acumula los errores en un `Vec<Error>` (no aborta al primero — recuperación de errores).
//! Hoy detecta: tipos duplicados, campos duplicados y métodos con firma duplicada.
//!
//! *Limitación conocida:* el AST todavía no lleva posiciones de fuente, así que los errores
//! salen sin línea/columna (0:0), con el nombre en el mensaje. Cuando el AST tenga *spans*,
//! se enriquecen.

use std::cell::RefCell;
use std::collections::{HashMap, HashSet};
use std::path::PathBuf;

use super::ast::{
    Block, CaseLabel, ClassDecl, CompilationUnit, Expr, ExprKind, LambdaBody, MethodRefQualifier,
    Member, MethodDecl, Modifier, Param, Pattern, Pos, PrimType, Stmt, StmtKind, SwitchBody, Type,
    TypeArg, TypeKind, TypeParam as AstTypeParam,
};
use super::classfile::{self, ExternalClass};
use super::symbol::{
    ParamSig, RType, RTypeArg, Resolved, ScopeId, Symbol, SymbolId, SymbolKind, SymbolTable,
};
use super::Error;

/// Los tipos **core** de `java.lang`: se cargan siempre del classpath (con su jerarquía real), y
/// los que no se encuentren quedan como *stub* sin miembros — el último recurso, para que un
/// classpath ausente no convierta todo en "símbolo no encontrado".
const JAVA_LANG: &[&str] = &[
    "Object", "String", "Integer", "Long", "Double", "Float", "Boolean", "Character", "Byte",
    "Short", "Number", "CharSequence", "Comparable", "Cloneable", "Runnable", "Iterable",
    "Thread", "Throwable", "Exception", "RuntimeException", "Error", "System", "Math", "Class",
    "Void", "StringBuilder", "Enum", "Record", "AssertionError", "AutoCloseable",
    // Lo usa el `catch` sintético de la población del `$SwitchMap` (switch sobre enum).
    "NoSuchFieldError",
    // Lo lanza el `switch` sobre patterns cuando el selector es `null` y no hay `case null` (§14.11.1).
    "NullPointerException",
    // Lo lanza el `valueOf(String)` sintético de todo `enum` cuando el nombre no matchea (§8.9.3).
    "IllegalArgumentException",
];

/// Busca `.class` en un classpath de directorios y los lee con el lector propio del
/// compilador ([`classfile`]). Es el reemplazo real del set modelado.
struct ClassFinder {
    classpath: Vec<PathBuf>,
    /// **Memoización de misses** (finding #19): nombres internos que ya se buscaron y **no** están en
    /// ningún directorio del classpath. Un tipo ausente referenciado transitivamente por muchas clases
    /// (p. ej. `RegexParser`, que no está en el `-cp`) se re-buscaba en disco cada vez —lectura fallida
    /// por cada directorio—; con esto la segunda búsqueda del mismo nombre es O(1).
    missed: RefCell<HashSet<String>>,
}

impl ClassFinder {
    /// Classpath por defecto: **primero el JDK 25 real** (`java.base` explotado), que es la
    /// semántica a la que apunta el compilador; `boot/` queda solo de respaldo.
    ///
    /// El orden importa: `boot/` es el runtime **mínimo del intérprete** (un puñado de clases
    /// recortadas — su `Integer extends Object` y no existe `Number`). Si tapara al JDK, el
    /// compilador razonaría sobre una jerarquía falsa y, por ejemplo, el *boxing* de la fase 2
    /// del overload resolution no podría probar `Integer <: Number`. `boot/` es del intérprete,
    /// no del compilador.
    /// `extra` son directorios de `.class` **antepuestos** al classpath por defecto (finding #7):
    /// se buscan **primero**, así un tipo ahí (KajiLibrary ya compilado) sombrea al del JDK.
    fn new(extra: &[PathBuf]) -> Self {
        let mut classpath: Vec<PathBuf> = extra.to_vec();
        classpath.push(PathBuf::from(".jdk25_tmp/classes/java.base"));
        classpath.push(PathBuf::from("boot"));
        ClassFinder { classpath, missed: RefCell::new(HashSet::new()) }
    }

    /// Lee `<internal>.class` (nombre interno con `/`) del primer directorio donde aparezca. Un miss
    /// se **memoiza** (finding #19): un nombre ya sabido ausente se descarta sin volver a tocar el
    /// disco. No se cachean los *hits*: el llamador registra el tipo cargado en la tabla y su guardia
    /// (`table.external(...)`) evita re-cargarlo, así que el mismo nombre no se re-busca cuando existe.
    fn find(&self, internal: &str) -> Option<ExternalClass> {
        if self.missed.borrow().contains(internal) {
            return None;
        }
        for dir in &self.classpath {
            if let Ok(bytes) = std::fs::read(dir.join(format!("{internal}.class"))) {
                return classfile::read(&bytes);
            }
        }
        self.missed.borrow_mut().insert(internal.to_string());
        None
    }
}

/// Carga desde el classpath los tipos externos **referenciados** en supertipos y firmas,
/// creando su símbolo (con miembros reales). Los que no se encuentran quedan para el respaldo.
fn load_externals(table: &mut SymbolTable, finder: &ClassFinder, unit: &CompilationUnit, imports: &Imports) {
    let mut names: HashSet<String> = HashSet::new();
    for class in &unit.types {
        collect_type_names(class, &mut names);
        // Los **tipos de anotación** también se cargan: sin esto, `@Deprecated` (java.lang, externo)
        // no resolvía y su descriptor salía `LDeprecated;` en vez de `Ljava/lang/Deprecated;` en
        // `RuntimeVisible[Parameter]Annotations` — un tipo que la reflexión no encuentra.
        collect_annotation_names(class, &mut names);
    }
    // Los tipos **core** de `java.lang` se cargan siempre, aunque ninguna firma los nombre: el
    // fuente los usa igual sin declararlos (un literal `"x"` es un `String`, `id(1)` boxea a
    // `Integer`), y sin su jerarquía real quedarían **opacos** — o sea, indulgentes: `Integer n =
    // "hola";` no daría error. `collect_type_names` solo mira firmas, no cuerpos, así que no
    // alcanza.
    for name in JAVA_LANG {
        names.insert((*name).to_string());
    }
    for name in names {
        try_load(table, finder, &name, imports, unit.package.as_deref());
    }
}

fn collect_type_names(class: &ClassDecl, out: &mut HashSet<String>) {
    for ty in class.extends.iter().chain(class.implements.iter()) {
        collect_from_type(ty, out);
    }
    for c in &class.components {
        collect_from_type(&c.ty, out);
    }
    for member in &class.members {
        match member {
            Member::Field(f) => {
                collect_from_type(&f.ty, out);
                if let Some(e) = &f.init {
                    collect_from_expr(e, out);
                }
            }
            Member::Method(m) => {
                collect_from_type(&m.return_type, out);
                for p in &m.params {
                    collect_from_type(&p.ty, out);
                }
                for t in &m.throws {
                    collect_from_type(t, out);
                }
                // Los tipos usados **dentro del cuerpo** (el tipo de un local, un `cast`, un `new`,
                // la interfaz funcional de una lambda) también se cargan: sin esto, algo que solo
                // aparece en un cuerpo —como `java.util.function.Function`— quedaba sin resolver.
                if let Some(b) = &m.body {
                    collect_from_block(b, out);
                }
            }
            Member::Type(nested) => collect_type_names(nested, out),
            Member::StaticInit(b) | Member::InstanceInit(b) => collect_from_block(b, out),
        }
    }
}

/// Los nombres de los **tipos de anotación** usados en las declaraciones (clase, parámetros de tipo,
/// constantes de `enum`, campos, métodos, parámetros), para que el finder los cargue del classpath —
/// así `@Deprecated` resuelve a `java.lang.Deprecated` y su descriptor sale cualificado.
fn collect_annotation_names(class: &ClassDecl, out: &mut HashSet<String>) {
    fn add(anns: &[super::ast::Annotation], out: &mut HashSet<String>) {
        for a in anns {
            out.insert(a.name.clone());
        }
    }
    add(&class.annotations, out);
    for tp in &class.type_params {
        add(&tp.annotations, out);
    }
    for ec in &class.enum_constants {
        add(&ec.annotations, out);
    }
    for member in &class.members {
        match member {
            Member::Field(f) => add(&f.annotations, out),
            Member::Method(m) => {
                add(&m.annotations, out);
                for p in &m.params {
                    add(&p.annotations, out);
                }
                for tp in &m.type_params {
                    add(&tp.annotations, out);
                }
            }
            Member::Type(nested) => collect_annotation_names(nested, out),
            _ => {}
        }
    }
}

fn collect_from_type(ty: &Type, out: &mut HashSet<String>) {
    match ty {
        Type::Class(name) => {
            out.insert(name.clone());
            insert_outer_segments(name, out);
        }
        // Un parametrizado aporta su base **y** sus argumentos: `List<String>` necesita cargar
        // tanto `List` como `String`.
        Type::Parameterized { base, args } => {
            out.insert(base.clone());
            insert_outer_segments(base, out);
            for a in args {
                collect_from_type_arg(a, out);
            }
        }
        Type::Array(inner) => collect_from_type(inner, out),
        // Sin nombres de tipo que cargar (explícito, para que agregar una variante rompa acá).
        Type::Void | Type::Prim(_) | Type::Var => {}
    }
}

/// Para un nombre **anidado** (`Map.Entry`, `Outer.Mid.Inner`), agrega los nombres de sus
/// contenedores (`Map`; `Outer.Mid` y `Outer`) al conjunto a cargar. Sin cargar el **outer**, su
/// `build_external` nunca registra el nested, y `Map.Entry` no resolvería en firma. Los segmentos que
/// en realidad son paquete (`java.util`) simplemente no encuentran clase y no molestan.
fn insert_outer_segments(name: &str, out: &mut HashSet<String>) {
    let mut rest = name;
    while let Some((outer, _)) = rest.rsplit_once('.') {
        out.insert(outer.to_string());
        rest = outer;
    }
}

fn collect_from_type_arg(arg: &TypeArg, out: &mut HashSet<String>) {
    match arg {
        TypeArg::Type(t) => collect_from_type(t, out),
        TypeArg::Extends(t) | TypeArg::Super(t) => collect_from_type(t, out),
        TypeArg::Wildcard => {}
    }
}

// ---- recolección de tipos usados en los **cuerpos** (para cargarlos) ----

fn collect_from_block(b: &Block, out: &mut HashSet<String>) {
    for s in &b.0 {
        collect_from_stmt(s, out);
    }
}

fn collect_from_pattern(p: &Pattern, out: &mut HashSet<String>) {
    match p {
        Pattern::Type { ty, .. } => collect_from_type(ty, out),
        Pattern::Record { ty, components } => {
            collect_from_type(ty, out);
            components.iter().for_each(|c| collect_from_pattern(c, out));
        }
    }
}

fn collect_from_stmt(s: &Stmt, out: &mut HashSet<String>) {
    match &s.kind {
        StmtKind::LocalVar { ty, init, .. } => {
            collect_from_type(ty, out);
            if let Some(e) = init {
                collect_from_expr(e, out);
            }
        }
        StmtKind::Return(e) => {
            if let Some(e) = e {
                collect_from_expr(e, out);
            }
        }
        StmtKind::Expr(e) | StmtKind::Throw(e) | StmtKind::Yield(e) => collect_from_expr(e, out),
        StmtKind::Block(b) => collect_from_block(b, out),
        StmtKind::If { cond, then, els } => {
            collect_from_expr(cond, out);
            collect_from_stmt(then, out);
            if let Some(e) = els {
                collect_from_stmt(e, out);
            }
        }
        StmtKind::While { cond, body } | StmtKind::Do { body, cond } => {
            collect_from_expr(cond, out);
            collect_from_stmt(body, out);
        }
        StmtKind::For { init, cond, update, body } => {
            if let Some(i) = init {
                collect_from_stmt(i, out);
            }
            if let Some(c) = cond {
                collect_from_expr(c, out);
            }
            update.iter().for_each(|u| collect_from_expr(u, out));
            collect_from_stmt(body, out);
        }
        StmtKind::ForEach { ty, iterable, body, .. } => {
            collect_from_type(ty, out);
            collect_from_expr(iterable, out);
            collect_from_stmt(body, out);
        }
        StmtKind::Synchronized { lock, body } => {
            collect_from_expr(lock, out);
            collect_from_block(body, out);
        }
        StmtKind::Try { resources, body, catches, finally } => {
            resources.iter().for_each(|r| collect_from_stmt(r, out));
            collect_from_block(body, out);
            for c in catches {
                c.types.iter().for_each(|t| collect_from_type(t, out));
                collect_from_block(&c.body, out);
            }
            if let Some(f) = finally {
                collect_from_block(f, out);
            }
        }
        StmtKind::Switch { selector, cases } => {
            collect_from_expr(selector, out);
            for c in cases {
                for l in &c.labels {
                    if let super::ast::CaseLabel::Pattern(p) = l {
                        collect_from_pattern(p, out);
                    }
                }
                if let Some(g) = &c.guard {
                    collect_from_expr(g, out);
                }
                match &c.body {
                    SwitchBody::Arrow(st) => collect_from_stmt(st, out),
                    SwitchBody::Colon(ss) => ss.iter().for_each(|s| collect_from_stmt(s, out)),
                }
            }
        }
        StmtKind::Assert { cond, message } => {
            collect_from_expr(cond, out);
            if let Some(m) = message {
                collect_from_expr(m, out);
            }
        }
        StmtKind::Labeled { body, .. } => collect_from_stmt(body, out),
        StmtKind::LocalClass(c) => collect_type_names(c, out),
        StmtKind::Break(_) | StmtKind::Continue(_) | StmtKind::Empty => {}
    }
}

/// Si `e` es un nombre simple (`Objects`) o una cadena `a.b.C` de puros nombres/campos (el receptor
/// de una llamada o campo estático), lo devuelve **punteado** como candidato a **tipo**. `None` para
/// cualquier otra expresión (una llamada, un índice, `this`…). Distinguir tipo de variable es de la
/// pasada 2; acá solo se decide qué **intentar cargar** del classpath.
fn qualifier_name(e: &Expr) -> Option<String> {
    match &e.kind {
        ExprKind::Name(n) => Some(n.clone()),
        ExprKind::Field { expr, name } => Some(format!("{}.{name}", qualifier_name(expr)?)),
        _ => None,
    }
}

fn collect_from_expr(e: &Expr, out: &mut HashSet<String>) {
    match &e.kind {
        // Las formas que **introducen** un nombre de tipo.
        ExprKind::Cast { ty, expr } => {
            collect_from_type(ty, out);
            collect_from_expr(expr, out);
        }
        ExprKind::InstanceOf { expr, ty, .. } => {
            collect_from_type(ty, out);
            collect_from_expr(expr, out);
        }
        ExprKind::ClassLit(ty) => collect_from_type(ty, out),
        // `Outer.this` menciona el tipo de la clase envolvente.
        ExprKind::QualifiedThis(ty) => collect_from_type(ty, out),
        ExprKind::NewObject { ty, args, body, outer } => {
            collect_from_type(ty, out);
            if let Some(o) = outer {
                collect_from_expr(o, out);
            }
            args.iter().for_each(|a| collect_from_expr(a, out));
            if let Some(members) = body {
                for m in members {
                    if let Member::Method(me) = m {
                        if let Some(b) = &me.body {
                            collect_from_block(b, out);
                        }
                    }
                }
            }
        }
        ExprKind::NewArray { elem, dims, init } => {
            collect_from_type(elem, out);
            dims.iter().flatten().for_each(|d| collect_from_expr(d, out));
            if let Some(es) = init {
                es.iter().for_each(|x| collect_from_expr(x, out));
            }
        }
        ExprKind::Lambda { params, body } => {
            for p in params {
                collect_from_type(&p.ty, out);
            }
            match body.as_ref() {
                LambdaBody::Expr(e) => collect_from_expr(e, out),
                LambdaBody::Block(b) => collect_from_block(b, out),
            }
        }
        ExprKind::MethodRef { qualifier, .. } => {
            if let MethodRefQualifier::Type(t) = qualifier.as_ref() {
                collect_from_type(t, out);
            }
        }
        // El resto: recursión en los sub-nodos.
        ExprKind::Binary { lhs, rhs, .. } => {
            collect_from_expr(lhs, out);
            collect_from_expr(rhs, out);
        }
        ExprKind::Unary { expr, .. } => collect_from_expr(expr, out),
        ExprKind::Assign { target, value, .. } => {
            collect_from_expr(target, out);
            collect_from_expr(value, out);
        }
        ExprKind::Ternary { cond, then, els } => {
            collect_from_expr(cond, out);
            collect_from_expr(then, out);
            collect_from_expr(els, out);
        }
        ExprKind::Call { target, args, type_args, .. } => {
            if let Some(t) = target {
                // El **receptor** de una llamada puede ser un **tipo** (llamada estática:
                // `Objects.requireNonNull(x)`, `Sib.v()`). Se marca como candidato a cargar — que de
                // veras sea un tipo lo decide la pasada 2; acá solo hay que intentar traerlo del
                // classpath. Sin esto, las utilidades de `java.util` (y cualquier tipo referenciado
                // **solo** por una llamada estática) nunca se cargaban (findings #11 y #7).
                if let Some(q) = qualifier_name(t) {
                    out.insert(q);
                }
                collect_from_expr(t, out);
            }
            args.iter().for_each(|a| collect_from_expr(a, out));
            type_args.iter().for_each(|a| collect_from_type_arg(a, out));
        }
        // Ídem para un **campo** estático (`Integer.MAX_VALUE`, `System.out`): el receptor es un tipo.
        ExprKind::Field { expr, .. } => {
            if let Some(q) = qualifier_name(expr) {
                out.insert(q);
            }
            collect_from_expr(expr, out);
        }
        ExprKind::Index { array, index } => {
            collect_from_expr(array, out);
            collect_from_expr(index, out);
        }
        ExprKind::Switch { selector, cases } => {
            collect_from_expr(selector, out);
            for c in cases {
                if let Some(g) = &c.guard {
                    collect_from_expr(g, out);
                }
                match &c.body {
                    SwitchBody::Arrow(st) => collect_from_stmt(st, out),
                    SwitchBody::Colon(ss) => ss.iter().for_each(|s| collect_from_stmt(s, out)),
                }
            }
        }
        // Sin tipos ni sub-expresiones que aporten.
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
        // `Indy` lo produce el desugar, muy posterior a Enter: nunca aparece en esta pasada.
        | ExprKind::Indy { .. }
        // Un nodo de error no menciona ningún tipo que cargar.
        | ExprKind::Error
        | ExprKind::Super => {}
    }
}

/// Intenta cargar `name` desde el classpath (si no es ya del fuente o un externo cargado).
/// Prueba rutas candidatas: el nombre cualificado, o `java/lang/<simple>` + los imports.
fn try_load(
    table: &mut SymbolTable,
    finder: &ClassFinder,
    name: &str,
    imports: &Imports,
    pkg: Option<&str>,
) {
    let simple = name.rsplit('.').next().unwrap_or(name);
    if table.class(name).is_some() || table.external(simple).is_some() {
        return;
    }
    let mut candidates: Vec<String> = Vec::new();
    if name.contains('.') {
        candidates.push(name.replace('.', "/"));
    } else {
        candidates.push(format!("java/lang/{name}"));
        if let Some(fqn) = imports.single.get(name) {
            candidates.push(fqn.replace('.', "/"));
        }
        // Mismo paquete (§6.5.5.1): un tipo del **paquete actual** en el classpath es visible por su
        // nombre simple, sin `import` (finding #4). P. ej. `List` desde `package java.util` → busca
        // `java/util/List`. Antes solo se probaba `java/lang/List`, y quedaba sin resolver.
        if let Some(p) = pkg {
            candidates.push(format!("{}/{name}", p.replace('.', "/")));
        }
        // Nombre **pelado**: un tipo del **paquete por defecto** en el classpath (su nombre interno
        // es el simple, sin paquete). Es lo que permite referenciar una clase KajiLibrary-only por
        // `-cp` (finding #7). Va último: `java.lang` y los imports tienen precedencia.
        candidates.push(name.to_string());
    }
    // **source-shadows-classpath** (findings #5/#7/#19): si **algún** candidato corresponde a un tipo
    // **fuente** de esta compilación, no se carga nada del classpath —el tipo ya existe—. La guardia de
    // arriba solo mira el nombre tal cual (`table.class(name)`), que no matchea cuando el fuente está
    // en un **paquete** y se lo referencia por su nombre simple (`K1` vs el registrado `p.K1`). Sin
    // esto, compilar con el propio output en el `-cp` cargaba cada clase hermana como un externo
    // redundante que arrastra su jerarquía — el disparo lento del #19.
    if candidates.iter().any(|c| table.class(&c.replace('/', ".")).is_some()) {
        return;
    }
    for internal in candidates {
        if let Some(ext) = finder.find(&internal) {
            build_external(table, finder, &ext);
            return;
        }
    }
}

/// Construye el símbolo de un tipo externo a partir de lo leído del `.class`, con sus campos y
/// métodos. Se registra por nombre simple (sin dueño → invisible al volcado).
fn build_external(table: &mut SymbolTable, finder: &ClassFinder, ext: &ExternalClass) {
    let simple = ext.name.rsplit('.').next().unwrap_or(&ext.name).to_string();
    if table.external(&simple).is_some() {
        return;
    }
    let members = table.new_scope(None, None);
    // Con el atributo `Signature` tenemos la jerarquía **genérica** (`List<E> extends
    // Collection<E>`); sin él, solo la borrada que dan `super_class`/`interfaces` del class file.
    let (extends, implements) = match &ext.signature {
        Some(sig) => (sig.super_type.clone(), sig.interfaces.clone()),
        None => (
            ext.super_name.clone().map(Type::Class),
            ext.interfaces.iter().cloned().map(Type::Class).collect(),
        ),
    };
    let cid = table.new_symbol(Symbol {
        name: simple.clone(),
        kind: SymbolKind::Class {
            kind: if ext.is_interface { TypeKind::Interface } else { TypeKind::Class },
            binary: ext.name.clone(),
            extends,
            implements,
            permits: Vec::new(),
            members,
        },
        owner: None,
        modifiers: Vec::new(),
    });
    table.set_scope_owner(members, cid);
    table.register_external(&simple, cid);

    // Los parámetros de tipo de la clase (`<E>` de `List<E>`): sin estos símbolos, las `E` de sus
    // firmas no resolverían a nada.
    if let Some(sig) = &ext.signature {
        define_type_params(table, members, cid, &sig.type_params);
    }

    for f in &ext.fields {
        // La firma genérica gana sobre el descriptor borrado (`E` en vez de `Object`).
        let ty = f.generic_ty.clone().unwrap_or_else(|| f.ty.clone());
        let s = table.new_symbol(Symbol {
            name: f.name.clone(),
            kind: SymbolKind::Field { ty },
            owner: Some(cid),
            modifiers: Vec::new(),
        });
        table.define(members, &f.name, s);
    }
    for m in &ext.methods {
        let (params, ret) = match &m.signature {
            Some(sig) => (sig.params.clone(), sig.ret.clone()),
            None => (m.params.clone(), m.ret.clone()),
        };
        let last = params.len().saturating_sub(1);
        let psig: Vec<ParamSig> = params
            .iter()
            .enumerate()
            .map(|(i, t)| ParamSig {
                ty: t.clone(),
                name: format!("a{i}"),
                // `ACC_VARARGS` marca el método; el varargs es siempre el último parámetro.
                varargs: m.varargs && i == last,
            })
            .collect();
        // Los flags de acceso que la detección del **SAM** de una interfaz funcional necesita
        // (un `default`/`static` no es el método abstracto único).
        let mut mmods = Vec::new();
        if m.is_abstract {
            mmods.push(Modifier::Abstract);
        }
        if m.is_static {
            mmods.push(Modifier::Static);
        }
        // Un método de **interfaz** externa que no es ni `abstract` ni `static` es un `default` (§9.4):
        // lleva cuerpo, o sea **implementa**. Marcarlo evita que el chequeo de completitud de
        // abstractos (§8.1.1.1) lo reclame como pendiente (finding #8). `<init>`/`<clinit>` no cuentan.
        if ext.is_interface && !m.is_abstract && !m.is_static && !m.name.starts_with('<') {
            mmods.push(Modifier::Default);
        }
        let mid = table.new_symbol(Symbol {
            name: m.name.clone(),
            kind: SymbolKind::Method {
                params: psig,
                return_type: ret,
                is_constructor: m.name == "<init>",
                throws: Vec::new(),
            },
            owner: Some(cid),
            modifiers: mmods,
        });
        table.define(members, &m.name, mid);
        // Los parámetros de tipo del **método** (`<T> T[] toArray(T[])`) viven en su propio
        // scope, colgando del método.
        if let Some(sig) = &m.signature {
            if !sig.type_params.is_empty() {
                let mscope = table.new_scope(Some(members), Some(mid));
                define_type_params(table, mscope, mid, &sig.type_params);
            }
        }
    }

    // **Carga transitiva** de la jerarquía: la superclase y las interfaces, por su nombre interno.
    // Sin esto, un método **heredado** (`s.hashCode()`, de `Object`) no se encontraría, y para no
    // falso-rechazarlo habría que ser indulgente con **todo** miss. Cargándola, un miss genuino
    // (`s.noExiste()`) sí se puede reportar. El símbolo ya está registrado arriba, así que la
    // recursión termina en `Object` (sin super) o en un `.class` que no está en el classpath.
    // Los nombres que devuelve `classfile::read` vienen con **puntos** (`java.util.AbstractList`);
    // `finder.find` espera el **nombre interno** con barras. Sin convertir, la ruta salía mal y la
    // superclase no cargaba —el eslabón que dejaba la jerarquía «incompleta» y sin cerrar la
    // indulgencia—.
    let supers: Vec<String> =
        ext.super_name.iter().chain(ext.interfaces.iter()).cloned().collect();
    for dotted in supers {
        let simple = dotted.rsplit('.').next().unwrap_or(&dotted);
        // No re-cargar un supertipo ya externo, ni uno que es un tipo **fuente** de esta compilación
        // (source-shadows-classpath, #19: evita arrastrar la jerarquía externa redundante cuando el
        // propio output está en el `-cp`).
        if table.external(simple).is_none() && table.class(&dotted).is_none() {
            if let Some(sup) = finder.find(&dotted.replace('.', "/")) {
                build_external(table, finder, &sup);
            }
        }
    }

    // Tipos **anidados** directos (`InnerClasses`, §4.7.6): se cargan y se registran como
    // **miembros-tipo** de esta clase, para que `Outer.Inner` de una clase externa resuelva
    // (`Diagnostic.Kind`, `Map.Entry`). Solo los **directos** (`Outer$Inner`, no `Outer$Inner$Deep`,
    // que es miembro de `Inner` y se carga al construir `Inner`).
    let prefix = format!("{}$", ext.name);
    for nested_dotted in &ext.nested {
        let Some(suffix) = nested_dotted.strip_prefix(&prefix) else { continue };
        if suffix.is_empty() || suffix.contains('$') {
            continue;
        }
        // La clave con que `build_external` lo registra en externals (su nombre simple *dotted*,
        // `Diagnostic$Kind`) vs. el nombre por el que se lo referencia anidado (`Kind`).
        let ext_key = nested_dotted.rsplit('.').next().unwrap_or(nested_dotted).to_string();
        if table.external(&ext_key).is_none() && table.class(nested_dotted).is_none() {
            if let Some(nested_ext) = finder.find(&nested_dotted.replace('.', "/")) {
                build_external(table, finder, &nested_ext);
            }
        }
        if let Some(nsid) = table.external(&ext_key) {
            table.define(members, suffix, nsid);
        }
    }
}

/// Crea los símbolos `TypeVar` de una lista de parámetros de tipo, en `scope`, con dueño `owner`.
fn define_type_params(table: &mut SymbolTable, scope: ScopeId, owner: SymbolId, params: &[AstTypeParam]) {
    for tp in params {
        let ts = table.new_symbol(Symbol {
            name: tp.name.clone(),
            kind: SymbolKind::TypeVar { bounds: tp.bounds.clone() },
            owner: Some(owner),
            modifiers: Vec::new(),
        });
        table.define(scope, &tp.name, ts);
    }
}

/// Corre la pasada 1 completa (Enter + MemberEnter) sobre `unit`, devolviendo la tabla de
/// símbolos y la lista de errores acumulados.
pub fn enter(unit: &CompilationUnit) -> (SymbolTable, Vec<Error>) {
    enter_cp(unit, &[])
}

/// [`enter`] con un **classpath extra** de directorios de `.class` (finding #7): se **antepone** al
/// classpath por defecto, así los tipos que estén ahí (p. ej. los ya compilados de KajiLibrary)
/// **sombrean** a los del JDK. `extra_classpath` vacío = comportamiento por defecto.
pub fn enter_cp(unit: &CompilationUnit, extra_classpath: &[PathBuf]) -> (SymbolTable, Vec<Error>) {
    let mut table = SymbolTable::new();
    let mut errors = Vec::new();
    let pkg = unit.package.as_deref();

    // El paquete (o el paquete sin nombre) es el dueño de los tipos top-level.
    let pkg_id = table.get_or_create_package(pkg);
    let pkg_scope = match &table.symbol(pkg_id).kind {
        SymbolKind::Package { members } => *members,
        _ => unreachable!("get_or_create_package devuelve un Package"),
    };
    // Prefijo para cualificar los nombres: el paquete ("" = sin nombre).
    let base = pkg.unwrap_or("");

    // Enter — **todos** los símbolos de clase primero (top-level y anidados, recursivo), así
    // cualquier nombre de tipo tiene a quién resolver.
    for class in &unit.types {
        enter_type(&mut table, &mut errors, class, pkg_id, pkg_scope, base, base, false);
    }
    // MemberEnter — recién ahora los contenidos de cada clase (recursivo en las anidadas).
    for class in &unit.types {
        member_enter_type(&mut table, &mut errors, class, base);
    }
    // Las clases **locales** (§14.3) no se registran acá: viven en cuerpos de método y su registro
    // pide **renombrarlas** a un nombre único (`1L`, `2L`…) para que dos homónimas en distintos
    // bloques del mismo enclosing no colisionen (la tabla clavea el tipo por nombre y el codegen deriva
    // el binary de él). Eso muta el AST, así que va en la pasada `register_local_classes`, que corre
    // con `&mut unit` justo después de `enter`.

    // Resolución — cierra la pasada 1: carga tipos externos, procesa imports y valida que los
    // tipos de supertipos y firmas existan, reportando los que no se encuentran.
    let imports = Imports::from_unit(unit);
    // **Class finder real**: carga desde el classpath los tipos externos referenciados, con
    // sus miembros leídos del `.class`.
    let finder = ClassFinder::new(extra_classpath);
    load_externals(&mut table, &finder, unit, &imports);
    // Respaldo: stubs modelados de `java.lang` para lo que no se encontró en disco.
    for &name in JAVA_LANG {
        table.add_external(name);
    }
    // Registrar los `import static` como salida para la pasada 2 (no afectan la resolución de
    // *tipos* de la pasada 1 — importan miembros estáticos, que se resuelven en los cuerpos).
    for imp in &unit.imports {
        if imp.is_static {
            if imp.wildcard {
                table.static_on_demand.push(imp.path.clone());
            } else if let Some((owner, member)) = imp.path.rsplit_once('.') {
                table.static_single.insert(member.to_string(), owner.to_string());
            }
        }
    }
    let mut reported = HashSet::new();
    for class in &unit.types {
        resolve_type_decl(&mut table, &mut errors, class, base, &imports, &mut reported);
    }
    // Persistir los tipos resueltos: el grafo `clase→super/interfaces` y las firmas
    // `campo/método → RType` — salida para la pasada 2.
    resolve_symbols(&mut table);
    detect_cycles(&table, &mut errors);
    (table, errors)
}

/// Resuelve los tipos **sintácticos** guardados en cada símbolo a [`RType`]/`SymbolId` y los
/// persiste en la tabla. No reporta errores (la validación ya lo hizo).
fn resolve_symbols(table: &mut SymbolTable) {
    for id in 0..table.symbol_count() {
        // Clonamos el kind para no sostener el borrow mientras resolvemos y escribimos.
        let resolved = match table.symbol(id).kind.clone() {
            SymbolKind::Class { kind, extends, implements, permits, members, .. } => {
                // Los supertipos se guardan **con sus argumentos** (`extends Base<T>`): el
                // subtipado genérico los sustituye al subir por la jerarquía. Se resuelven en el
                // scope de la clase, que es donde están sus propios parámetros de tipo.
                // Un `enum` extiende implícitamente `java.lang.Enum` (JLS §8.9) — de ahí saca
                // `ordinal()`/`name()`/`compareTo`, que el `$SwitchMap` del desugar necesita.
                let super_type = match &extends {
                    Some(t) => Some(resolve_rtype(table, members, t)),
                    None if kind == TypeKind::Enum => {
                        Some(resolve_rtype(table, members, &Type::Class("Enum".into())))
                    }
                    // Un `record` extiende implícitamente `java.lang.Record` (§8.10) — es lo que la
                    // reflexión chequea para `isRecord()`, junto al atributo `Record` y el `final`.
                    None if kind == TypeKind::Record => {
                        Some(resolve_rtype(table, members, &Type::Class("Record".into())))
                    }
                    // Una **clase** sin `extends` explícito hereda de `java.lang.Object` (§8.1.4): sin
                    // esto, `super_class` quedaba `None` y la resolución de miembros sobre `this` no
                    // alcanzaba los métodos de `Object` (`getClass`/`hashCode`/`toString`) — finding #22.
                    // Se enlaza solo si `Object` resolvió a una clase concreta **distinta de esta misma**
                    // (evita el ciclo de la propia `Object`, y no cambia nada si `Object` no está cargado —
                    // p.ej. tests sin classpath—). Las **interfaces** no tienen superclase.
                    None if kind == TypeKind::Class => {
                        match resolve_rtype(table, members, &Type::Class("Object".into())) {
                            RType::Class(c) if c != id => Some(RType::Class(c)),
                            _ => None,
                        }
                    }
                    None => None,
                };
                let interface_types =
                    implements.iter().map(|t| resolve_rtype(table, members, t)).collect();
                // Los subtipos de `permits` se resuelven en el scope de la clase (donde viven sus
                // parámetros de tipo). El chequeo de buena formación (§8.1.6) lo hace la pasada 3.
                let permitted =
                    permits.iter().map(|t| resolve_rtype(table, members, t)).collect();
                Some(Resolved::Class { super_type, interface_types, permitted })
            }
            SymbolKind::Field { ty } => {
                let scope = owner_scope(table, id);
                Some(Resolved::Field(resolve_rtype(table, scope, &ty)))
            }
            SymbolKind::Method { params, return_type, throws, .. } => {
                let scope = owner_scope(table, id);
                let ps = params.iter().map(|p| resolve_rtype(table, scope, &p.ty)).collect();
                // Solo el **último** parámetro puede ser varargs (JLS §8.4.1).
                let varargs = params.last().is_some_and(|p| p.varargs);
                let thr = throws.iter().map(|t| resolve_rtype(table, scope, t)).collect();
                Some(Resolved::Method {
                    params: ps,
                    ret: resolve_rtype(table, scope, &return_type),
                    varargs,
                    throws: thr,
                })
            }
            // Las cotas de un parámetro de tipo se resuelven en el scope de su dueño (pueden
            // nombrar a otros params: `<T extends Comparable<T>>`).
            SymbolKind::TypeVar { bounds } => {
                let scope = owner_scope(table, id);
                Some(Resolved::TypeVar {
                    bounds: bounds.iter().map(|b| resolve_rtype(table, scope, b)).collect(),
                })
            }
            SymbolKind::Package { .. } => None,
        };
        if let Some(r) = resolved {
            table.set_resolved(id, r);
        }
    }
    fill_implicit_permits(table);
}

/// El `permits` **implícito** (§8.1.6): un tipo `sealed` que no escribió `permits` autoriza a los
/// subtipos **directos** declarados en la misma unidad. Se rellena el `permitted` resuelto con ellos
/// para que el chequeo de exhaustividad y el de las reglas de sealed vean el conjunto completo. Corre
/// al final de cada `resolve_symbols` (idempotente): así una re-resolución no lo pierde.
fn fill_implicit_permits(table: &mut SymbolTable) {
    // Subtipos directos por supertipo (erasure), sobre todas las clases conocidas.
    let mut subs: std::collections::HashMap<SymbolId, Vec<SymbolId>> = std::collections::HashMap::new();
    for id in 0..table.symbol_count() {
        if !matches!(table.symbol(id).kind, SymbolKind::Class { .. }) {
            continue;
        }
        if let Some(s) = table.super_class(id) {
            subs.entry(s).or_default().push(id);
        }
        for i in table.interfaces(id) {
            subs.entry(i).or_default().push(id);
        }
    }
    for id in 0..table.symbol_count() {
        if table.is_sealed(id) && table.permitted(id).is_empty() {
            if let Some(children) = subs.get(&id) {
                let rts = children.iter().map(|&c| RType::Class(c)).collect();
                table.set_permitted(id, rts);
            }
        }
    }
}

/// El scope donde se resuelven los tipos de un miembro: el de su clase dueña.
fn owner_scope(table: &SymbolTable, id: SymbolId) -> ScopeId {
    // Un método **genérico** declara sus propios parámetros de tipo: su firma resuelve ahí
    // (`<T> T id(T x)` — la `T` no existe en la clase). El scope cuelga del de la clase, así que
    // los tipos de afuera se siguen viendo.
    if matches!(table.symbol(id).kind, SymbolKind::Method { .. }) {
        if let Some(own) = table.own_scope(id) {
            return own;
        }
    }
    match table.symbol(id).owner {
        Some(owner) => match &table.symbol(owner).kind {
            SymbolKind::Class { members, .. } => *members,
            // Un parámetro de tipo de un **método** (`<L extends Lst<T>>`) resuelve su cota en el
            // scope propio del método: ahí viven los params **hermanos** (`T`) y, encadenando hacia
            // arriba, las clases del paquete (`Lst`). Con `global` ni uno ni otro se veían.
            SymbolKind::Method { .. } => table.own_scope(owner).unwrap_or(table.global),
            _ => table.global,
        },
        None => table.global,
    }
}

fn resolve_rtype(table: &SymbolTable, scope: ScopeId, ty: &Type) -> RType {
    match ty {
        Type::Void => RType::Void,
        Type::Prim(p) => RType::Prim(*p),
        Type::Array(inner) => RType::Array(Box::new(resolve_rtype(table, scope, inner))),
        Type::Var => RType::Unresolved,
        Type::Class(name) => match resolve_name_to_sym(table, scope, name) {
            Some((id, true)) => RType::TypeVar(id),
            Some((id, false)) => RType::Class(id),
            None => RType::Unresolved,
        },
        Type::Parameterized { base, args } => match resolve_name_to_sym(table, scope, base) {
            // Un parámetro de tipo no puede llevar argumentos (`T<X>` no existe): se ignoran.
            Some((id, true)) => RType::TypeVar(id),
            Some((id, false)) => RType::Parameterized {
                base: id,
                args: args.iter().map(|a| resolve_rtype_arg(table, scope, a)).collect(),
            },
            None => RType::Unresolved,
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

/// Resuelve un nombre de tipo a su símbolo (y si es un parámetro de tipo). Mismo orden que
/// `resolve_class_name`, pero devolviendo el `SymbolId`.
fn resolve_name_to_sym(table: &SymbolTable, scope: ScopeId, name: &str) -> Option<(SymbolId, bool)> {
    // Un supertipo leído del `.class` viene como **nombre interno** (`java/util/AbstractList`); se
    // normaliza a puntos para tratarlo igual que uno del fuente.
    let dotted = name.replace('/', ".");
    if let Some(rest) = dotted.strip_prefix("java.lang.") {
        // **Shadowing** del fuente (semántica `--patch-module`, finding #5): si esta compilación
        // **declara** `java.lang.X` (p. ej. se está compilando `java.lang.String` en sí), ese tipo
        // del fuente gana sobre el externo del classpath. Sin esto, el `String` que devuelve
        // `Object.toString()` (leído del `.class`) ligaba al `String` **externo**, distinto del que se
        // compila, y el override covariante los veía como dos tipos distintos.
        return table.class(&dotted).or_else(|| table.external(rest)).map(|id| (id, false));
    }
    if dotted.contains('.') {
        // Cualificado: una clase del **fuente** (por su nombre completo) o un **externo** por su
        // nombre simple. El fallback a externo es lo que hace resolver `java.util.AbstractList` a la
        // `AbstractList` cargada —los externos se registran por nombre simple—, sin lo cual la
        // cadena de supertipos de un genérico del JDK quedaba rota (y la indulgencia, sin cerrar).
        let simple = dotted.rsplit('.').next().unwrap_or(&dotted);
        if let Some(id) = table.class(&dotted).or_else(|| table.external(simple)) {
            return Some((id, false));
        }
        // Tipo **anidado** `Outer.Inner` (`Map.Entry`, `Outer.Mid.Inner`): se parte en el **último**
        // `.`, se resuelve el `outer` y se baja a su miembro-tipo. Es la misma bajada que hace el
        // path de expresión (`nested_type`), traída a la posición de tipo.
        if let Some((outer, inner)) = dotted.rsplit_once('.') {
            if let Some((oid, _)) = resolve_name_to_sym(table, scope, outer) {
                return super::attribute::nested_type_in(table, oid, inner).map(|id| (id, false));
            }
        }
        return None;
    }
    if let Some(id) = table.resolve_type(scope, name) {
        let is_var = matches!(table.symbol(id).kind, SymbolKind::TypeVar { .. });
        return Some((id, is_var));
    }
    table.external(name).map(|id| (id, false))
}

/// Detecta ciclos de herencia (`A extends B extends A`) entre las clases del fuente (§8.1.5),
/// siguiendo el grafo `super_class` ya persistido por `resolve_symbols`.
fn detect_cycles(table: &SymbolTable, errors: &mut Vec<Error>) {
    let mut in_cycle: HashSet<SymbolId> = HashSet::new();
    for start in table.source_classes() {
        if in_cycle.contains(&start) {
            continue;
        }
        let (mut cur, mut seen, mut path) = (start, HashSet::new(), Vec::new());
        loop {
            if !seen.insert(cur) {
                let (line, col) = table.pos_of(cur);
                error(errors, Pos { line, col }, format!("ciclo de herencia: {}", table.symbol(cur).name));
                in_cycle.extend(path.iter().copied());
                in_cycle.insert(cur);
                break;
            }
            path.push(cur);
            // Seguir la superclase solo si es del fuente (los externos cortan la cadena).
            match table.super_class(cur) {
                Some(next) if table.symbol(next).owner.is_some() => cur = next,
                _ => break,
            }
        }
    }
}

/// Los `import`s de tipo de una unidad (los `static` no aportan tipos).
struct Imports {
    single: HashMap<String, String>, // nombre simple → nombre cualificado
    has_wildcard: bool,
}

impl Imports {
    fn from_unit(unit: &CompilationUnit) -> Imports {
        let mut single = HashMap::new();
        let mut has_wildcard = false;
        for imp in &unit.imports {
            if imp.is_static {
                continue;
            }
            if imp.wildcard {
                has_wildcard = true;
            } else if let Some(simple) = imp.path.rsplit('.').next() {
                single.insert(simple.to_string(), imp.path.clone());
            }
        }
        Imports { single, has_wildcard }
    }
}

/// Valida los tipos declarados de un tipo (supertipos, firmas, componentes) y recurre en los
/// anidados. Reporta cada nombre no resuelto una sola vez (vía `reported`).
fn resolve_type_decl(
    table: &mut SymbolTable,
    errors: &mut Vec<Error>,
    class: &ClassDecl,
    enclosing: &str,
    imports: &Imports,
    reported: &mut HashSet<String>,
) {
    let fqn = qualify(enclosing, &class.name);
    let Some(cid) = table.class(&fqn) else { return };
    let scope = match &table.symbol(cid).kind {
        SymbolKind::Class { members, .. } => *members,
        _ => return,
    };

    let empty: HashSet<String> = HashSet::new();
    let cpos = class.pos;
    for ty in class.extends.iter().chain(class.implements.iter()) {
        check_type(table, errors, scope, ty, imports, reported, &empty, cpos);
    }
    for comp in &class.components {
        check_type(table, errors, scope, &comp.ty, imports, reported, &empty, cpos);
    }
    for member in &class.members {
        match member {
            Member::Field(f) => check_type(table, errors, scope, &f.ty, imports, reported, &empty, f.pos),
            Member::Method(m) => {
                // Los type params del propio método resuelven en su firma (leniency local).
                let locals: HashSet<String> = m.type_params.iter().map(|p| p.name.clone()).collect();
                check_type(table, errors, scope, &m.return_type, imports, reported, &locals, m.pos);
                for p in &m.params {
                    check_type(table, errors, scope, &p.ty, imports, reported, &locals, m.pos);
                }
            }
            Member::Type(nested) => resolve_type_decl(table, errors, nested, &fqn, imports, reported),
            Member::StaticInit(_) | Member::InstanceInit(_) => {} // sin firma que validar
        }
    }
}

/// Chequea que un tipo referenciado exista; si es una clase no resoluble (ni un type param
/// local), reporta el error en `pos`. `locals` son los parámetros de tipo en alcance.
#[allow(clippy::too_many_arguments)]
fn check_type(
    table: &SymbolTable,
    errors: &mut Vec<Error>,
    scope: ScopeId,
    ty: &Type,
    imports: &Imports,
    reported: &mut HashSet<String>,
    locals: &HashSet<String>,
    pos: Pos,
) {
    match ty {
        Type::Class(name) => {
            if locals.contains(name) {
                return;
            }
            if !resolve_class_name(table, scope, name, imports) && reported.insert(name.clone()) {
                error(errors, pos, format!("no se encuentra el símbolo: {name}"));
            }
        }
        // Se valida la base **y** cada argumento: `List<NoExiste>` también es un error.
        Type::Parameterized { base, args } => {
            if !locals.contains(base)
                && !resolve_class_name(table, scope, base, imports)
                && reported.insert(base.clone())
            {
                error(errors, pos, format!("no se encuentra el símbolo: {base}"));
            }
            for a in args {
                match a {
                    TypeArg::Type(t) => {
                        check_type(table, errors, scope, t, imports, reported, locals, pos)
                    }
                    TypeArg::Extends(t) | TypeArg::Super(t) => {
                        check_type(table, errors, scope, t, imports, reported, locals, pos)
                    }
                    TypeArg::Wildcard => {}
                }
            }
        }
        Type::Array(inner) => check_type(table, errors, scope, inner, imports, reported, locals, pos),
        // Nada que validar (explícito, para que agregar una variante rompa acá).
        Type::Void | Type::Prim(_) | Type::Var => {}
    }
}

/// ¿Resuelve el nombre de tipo `name`? Orden: scope (anidados/mismo paquete) → import
/// single-type → `java.lang` (class finder) → cualificado. Ante un `import *` se es indulgente
/// (no podemos saber sin classpath). Es la resolución de la **pasada 1** (solo existencia).
fn resolve_class_name(table: &SymbolTable, scope: ScopeId, name: &str, imports: &Imports) -> bool {
    if let Some(rest) = name.strip_prefix("java.lang.") {
        return table.external(rest).is_some();
    }
    if name.contains('.') {
        if table.class(name).is_some() {
            return true;
        }
        // Tipo **anidado** `Outer.Inner` (`Map.Entry`, `Diagnostic.Kind`): resolver el `outer` y ver
        // si tiene ese miembro-tipo. Sin esto, un tipo anidado en firma daba un falso "no se
        // encuentra el símbolo" (la pasada 1 solo miraba `table.class` para los cualificados).
        if let Some((outer, inner)) = name.rsplit_once('.') {
            if let Some((oid, _)) = resolve_name_to_sym(table, scope, outer) {
                return super::attribute::nested_type_in(table, oid, inner).is_some();
            }
        }
        return false;
    }
    // Nombre simple:
    if table.resolve_type(scope, name).is_some() {
        return true;
    }
    if imports.single.contains_key(name) {
        return true; // importado explícitamente (lo damos por existente)
    }
    if table.external(name).is_some() {
        return true; // java.lang, auto-importado
    }
    imports.has_wildcard // con `import *` no podemos descartar
}

/// Nombre cualificado de un tipo dado el nombre de su contenedor (paquete o clase externa).
fn qualify(enclosing: &str, name: &str) -> String {
    if enclosing.is_empty() {
        name.to_string()
    } else {
        format!("{enclosing}.{name}")
    }
}

/// Reporta un error de la pasada 1 en la posición de la declaración culpable.
fn error(errors: &mut Vec<Error>, pos: Pos, message: String) {
    errors.push(Error::new(message, pos.line, pos.col));
}

/// **Enter** de un tipo: crea su `ClassSymbol` (dueño = `owner`, en `owner_scope`) y recurre
/// en sus tipos anidados. `enclosing` es el nombre cualificado del contenedor.
#[allow(clippy::too_many_arguments)]
fn enter_type(
    table: &mut SymbolTable,
    errors: &mut Vec<Error>,
    class: &ClassDecl,
    owner: SymbolId,
    owner_scope: ScopeId,
    enclosing_fqn: &str,
    enclosing_binary: &str,
    nested: bool,
) {
    let fqn = qualify(enclosing_fqn, &class.name);
    // El *binary name*: en el paquete se une con `.`; anidado, con `$` (`Outer$Inner`).
    let binary = if nested {
        format!("{enclosing_binary}${}", class.name)
    } else {
        qualify(enclosing_binary, &class.name)
    };
    if table.class(&fqn).is_some() {
        error(errors, class.pos, format!("tipo duplicado: {fqn}"));
        return;
    }
    // El scope de miembros se enlaza al de su contenedor (paquete o clase externa).
    let members = table.new_scope(Some(owner_scope), None);
    let sym = table.new_symbol(Symbol {
        name: class.name.clone(),
        kind: SymbolKind::Class {
            kind: class.kind,
            binary: binary.clone(),
            extends: class.extends.clone(),
            implements: class.implements.clone(),
            permits: class.permits.clone(),
            members,
        },
        owner: Some(owner),
        modifiers: class.modifiers.clone(),
    });
    table.set_scope_owner(members, sym);
    table.register_class(&fqn, sym);
    table.define(owner_scope, &class.name, sym);
    table.set_pos(sym, class.pos.line, class.pos.col);

    // Parámetros de tipo genéricos: `TypeVar` en el scope de la clase (para que resuelvan al
    // aparecer en las firmas, p. ej. `class Box<T> { T value; }`).
    define_type_params(table, members, sym, &class.type_params);

    // Recursión: los tipos anidados son símbolos de la clase, en su scope de miembros.
    for member in &class.members {
        if let Member::Type(inner) = member {
            enter_type(table, errors, inner, sym, members, &fqn, &binary, true);
        }
    }
}

/// **MemberEnter** de un tipo: entra sus campos/métodos con sus firmas, y recurre en los
/// tipos anidados.
fn member_enter_type(table: &mut SymbolTable, errors: &mut Vec<Error>, class: &ClassDecl, enclosing: &str) {
    let fqn = qualify(enclosing, &class.name);
    let Some(cid) = table.class(&fqn) else { return };
    let scope = match &table.symbol(cid).kind {
        SymbolKind::Class { members, .. } => *members,
        _ => return,
    };

    // `record`: sus componentes son campos `private final` sintéticos.
    if class.kind == TypeKind::Record {
        for comp in &class.components {
            let sym = table.new_symbol(Symbol {
                name: comp.name.clone(),
                kind: SymbolKind::Field { ty: comp.ty.clone() },
                owner: Some(cid),
                modifiers: vec![Modifier::Private, Modifier::Final],
            });
            table.define(scope, &comp.name, sym);
        }
    }
    // `enum`: sus constantes son campos `public static final` del propio enum.
    if class.kind == TypeKind::Enum {
        for c in &class.enum_constants {
            let sym = table.new_symbol(Symbol {
                name: c.name.clone(),
                kind: SymbolKind::Field { ty: Type::Class(class.name.clone()) },
                owner: Some(cid),
                modifiers: vec![Modifier::Public, Modifier::Static, Modifier::Final],
            });
            table.define(scope, &c.name, sym);
        }
    }

    for member in &class.members {
        match member {
            Member::Field(f) => {
                let dup = table
                    .scope(scope)
                    .get(&f.name)
                    .iter()
                    .any(|&id| matches!(table.symbol(id).kind, SymbolKind::Field { .. }));
                if dup {
                    error(errors, f.pos, format!("campo duplicado: {}.{}", class.name, f.name));
                }
                let sym = table.new_symbol(Symbol {
                    name: f.name.clone(),
                    kind: SymbolKind::Field { ty: f.ty.clone() },
                    owner: Some(cid),
                    modifiers: implicit_field_mods(class.kind, &f.modifiers),
                });
                table.define(scope, &f.name, sym);
            }
            Member::Method(m) => {
                let params: Vec<ParamSig> = m
                    .params
                    .iter()
                    .map(|p| ParamSig { ty: p.ty.clone(), name: p.name.clone(), varargs: p.varargs })
                    .collect();
                let sig: Vec<Type> = params.iter().map(|p| p.ty.clone()).collect();
                // Duplicado = mismo nombre y **mismos tipos de parámetro** (overload si difieren).
                let dup = table.scope(scope).get(&m.name).iter().any(|&id| {
                    if let SymbolKind::Method { params: existing, .. } = &table.symbol(id).kind {
                        existing.iter().map(|p| &p.ty).eq(sig.iter())
                    } else {
                        false
                    }
                });
                if dup {
                    let what = if m.is_constructor { "constructor" } else { "método" };
                    error(errors, m.pos, format!("{what} duplicado: {}.{}(...)", class.name, m.name));
                }
                let sym = table.new_symbol(Symbol {
                    name: m.name.clone(),
                    kind: SymbolKind::Method {
                        params,
                        return_type: m.return_type.clone(),
                        is_constructor: m.is_constructor,
                        throws: m.throws.clone(),
                    },
                    owner: Some(cid),
                    modifiers: implicit_method_mods(class.kind, m),
                });
                table.define(scope, &m.name, sym);
                // Los parámetros de tipo de un método **genérico** (`<T> T id(T x)`) viven en su
                // propio scope, enlazado al de la clase: así la `T` de su firma resuelve a un
                // símbolo real y no queda `Unresolved`.
                if !m.type_params.is_empty() {
                    let mscope = table.new_scope(Some(scope), Some(sym));
                    define_type_params(table, mscope, sym, &m.type_params);
                }
            }
            Member::Type(nested) => member_enter_type(table, errors, nested, &fqn),
            Member::StaticInit(_) | Member::InstanceInit(_) => {} // no declara miembros
        }
    }

    // Constructor por defecto si no se declaró ninguno (clases/enum/record; no interfaces).
    // Para un `record`, el canónico toma los componentes (JLS §8.10.4 / §8.8.9).
    if matches!(class.kind, TypeKind::Class | TypeKind::Enum | TypeKind::Record) {
        let has_ctor = table
            .scope(scope)
            .get(&class.name)
            .iter()
            .any(|&id| matches!(table.symbol(id).kind, SymbolKind::Method { is_constructor: true, .. }));
        if !has_ctor {
            let params = if class.kind == TypeKind::Record {
                class
                    .components
                    .iter()
                    .map(|c| ParamSig { ty: c.ty.clone(), name: c.name.clone(), varargs: c.varargs })
                    .collect()
            } else {
                Vec::new()
            };
            let sym = table.new_symbol(Symbol {
                name: class.name.clone(),
                kind: SymbolKind::Method { params, return_type: Type::Void, is_constructor: true, throws: Vec::new() },
                owner: Some(cid),
                modifiers: Vec::new(),
            });
            table.define(scope, &class.name, sym);
        }
    }

    // Métodos implícitos de `enum` (§8.9.3) y `record` (§8.10.3).
    synth_implicit_methods(table, class, cid, scope);
}

/// Transforma cada **clase anónima** (`new Tipo(){ miembros }`, §15.9.5) en una clase **local**
/// sintética `$N` + un `new $N()`, para que recorra el mismo camino que una local (registro, tipado
/// in situ, levantado, captura). Corre **después** de `enter` —los tipos ya están resueltos, para
/// decidir `extends` (clase) vs `implements` (interfaz)— y antes de Attribute. Este incremento cubre
/// las anónimas **sin argumentos de super** (`new Tipo(){…}`); las que los llevan quedan para el
/// emisor (barrera).
pub fn hoist_anonymous(unit: &mut CompilationUnit, table: &mut SymbolTable, errors: &mut Vec<Error>) {
    let base = unit.package.as_deref().unwrap_or("").to_string();
    {
        let mut h = Hoister { table: &mut *table, errors };
        for class in &mut unit.types {
            h.class(class, &base);
        }
    }
    // Las clases anónimas sintéticas se registran **después** del último `resolve_symbols` (el de
    // `enter` y el de `register_local_classes`), así que sus miembros quedan **sin resolver**. Se
    // re-resuelve (idempotente) para que un campo propio de una anónima tenga su `Resolved::Field`
    // — sin esto una referencia **pelada** a ese campo (`c`, no `this.c`) no se encontraba.
    resolve_symbols(table);
}

struct Hoister<'a> {
    table: &'a mut SymbolTable,
    errors: &'a mut Vec<Error>,
}

impl Hoister<'_> {
    fn class(&mut self, class: &mut ClassDecl, enclosing_fqn: &str) {
        let fqn = qualify(enclosing_fqn, &class.name);
        let Some(cid) = self.table.class(&fqn) else { return };
        let (scope, binary) = match &self.table.symbol(cid).kind {
            SymbolKind::Class { members, binary, .. } => (*members, binary.clone()),
            _ => return,
        };
        let mut counter = 0u32;
        for member in &mut class.members {
            match member {
                Member::Method(m) => {
                    if let Some(body) = &mut m.body {
                        self.block(body, cid, scope, &binary, &fqn, &mut counter);
                    }
                }
                Member::StaticInit(b) | Member::InstanceInit(b) => {
                    self.block(b, cid, scope, &binary, &fqn, &mut counter);
                }
                Member::Type(nested) => self.class(nested, &fqn),
                Member::Field(_) => {}
            }
        }
    }

    /// Reescribe un bloque intercalando, **antes** de cada sentencia, las `LocalClass` sintéticas de
    /// las anónimas que aparezcan en sus expresiones (así los locales capturados están en scope).
    fn block(
        &mut self,
        block: &mut Block,
        owner: SymbolId,
        scope: ScopeId,
        binary: &str,
        fqn: &str,
        counter: &mut u32,
    ) {
        let mut out: Vec<Stmt> = Vec::with_capacity(block.0.len());
        for mut stmt in block.0.drain(..) {
            let mut decls: Vec<Stmt> = Vec::new();
            self.stmt(&mut stmt, owner, scope, binary, fqn, counter, &mut decls);
            out.append(&mut decls);
            out.push(stmt);
        }
        block.0 = out;
    }

    #[allow(clippy::too_many_arguments)]
    fn stmt(
        &mut self,
        stmt: &mut Stmt,
        owner: SymbolId,
        scope: ScopeId,
        binary: &str,
        fqn: &str,
        counter: &mut u32,
        decls: &mut Vec<Stmt>,
    ) {
        match &mut stmt.kind {
            StmtKind::LocalVar { init: Some(e), .. }
            | StmtKind::Return(Some(e))
            | StmtKind::Expr(e)
            | StmtKind::Throw(e)
            | StmtKind::Yield(e) => self.expr(e, owner, scope, binary, fqn, counter, decls),
            StmtKind::If { cond, then, els } => {
                self.expr(cond, owner, scope, binary, fqn, counter, decls);
                self.stmt(then, owner, scope, binary, fqn, counter, decls);
                if let Some(e) = els {
                    self.stmt(e, owner, scope, binary, fqn, counter, decls);
                }
            }
            StmtKind::While { cond, body } | StmtKind::Do { body, cond } => {
                self.expr(cond, owner, scope, binary, fqn, counter, decls);
                self.stmt(body, owner, scope, binary, fqn, counter, decls);
            }
            StmtKind::For { init, cond, update, body } => {
                if let Some(i) = init {
                    self.stmt(i, owner, scope, binary, fqn, counter, decls);
                }
                if let Some(c) = cond {
                    self.expr(c, owner, scope, binary, fqn, counter, decls);
                }
                for u in update.iter_mut() {
                    self.expr(u, owner, scope, binary, fqn, counter, decls);
                }
                self.stmt(body, owner, scope, binary, fqn, counter, decls);
            }
            StmtKind::ForEach { iterable, body, .. } => {
                self.expr(iterable, owner, scope, binary, fqn, counter, decls);
                self.stmt(body, owner, scope, binary, fqn, counter, decls);
            }
            // Un bloque anidado maneja sus propias anónimas (con su propio scope de locales).
            StmtKind::Block(b) => self.block(b, owner, scope, binary, fqn, counter),
            StmtKind::Synchronized { lock, body } => {
                self.expr(lock, owner, scope, binary, fqn, counter, decls);
                self.block(body, owner, scope, binary, fqn, counter);
            }
            StmtKind::Try { resources, body, catches, finally } => {
                for r in resources.iter_mut() {
                    self.stmt(r, owner, scope, binary, fqn, counter, decls);
                }
                self.block(body, owner, scope, binary, fqn, counter);
                for c in catches.iter_mut() {
                    self.block(&mut c.body, owner, scope, binary, fqn, counter);
                }
                if let Some(f) = finally {
                    self.block(f, owner, scope, binary, fqn, counter);
                }
            }
            StmtKind::Switch { selector, cases } => {
                self.expr(selector, owner, scope, binary, fqn, counter, decls);
                for c in cases.iter_mut() {
                    if let Some(g) = &mut c.guard {
                        self.expr(g, owner, scope, binary, fqn, counter, decls);
                    }
                    match &mut c.body {
                        SwitchBody::Arrow(s) => self.stmt(s, owner, scope, binary, fqn, counter, decls),
                        SwitchBody::Colon(ss) => {
                            for s in ss.iter_mut() {
                                self.stmt(s, owner, scope, binary, fqn, counter, decls);
                            }
                        }
                    }
                }
            }
            StmtKind::Labeled { body, .. } => {
                self.stmt(body, owner, scope, binary, fqn, counter, decls)
            }
            _ => {}
        }
    }

    #[allow(clippy::too_many_arguments)]
    fn expr(
        &mut self,
        e: &mut Expr,
        owner: SymbolId,
        scope: ScopeId,
        binary: &str,
        fqn: &str,
        counter: &mut u32,
        decls: &mut Vec<Stmt>,
    ) {
        match &mut e.kind {
            ExprKind::Binary { lhs, rhs, .. } => {
                self.expr(lhs, owner, scope, binary, fqn, counter, decls);
                self.expr(rhs, owner, scope, binary, fqn, counter, decls);
            }
            ExprKind::Unary { expr, .. }
            | ExprKind::Field { expr, .. }
            | ExprKind::Cast { expr, .. }
            | ExprKind::InstanceOf { expr, .. } => {
                self.expr(expr, owner, scope, binary, fqn, counter, decls)
            }
            ExprKind::Assign { target, value, .. } => {
                self.expr(target, owner, scope, binary, fqn, counter, decls);
                self.expr(value, owner, scope, binary, fqn, counter, decls);
            }
            ExprKind::Ternary { cond, then, els } => {
                self.expr(cond, owner, scope, binary, fqn, counter, decls);
                self.expr(then, owner, scope, binary, fqn, counter, decls);
                self.expr(els, owner, scope, binary, fqn, counter, decls);
            }
            ExprKind::Call { target, args, .. } => {
                if let Some(t) = target {
                    self.expr(t, owner, scope, binary, fqn, counter, decls);
                }
                for a in args.iter_mut() {
                    self.expr(a, owner, scope, binary, fqn, counter, decls);
                }
            }
            ExprKind::Index { array, index } => {
                self.expr(array, owner, scope, binary, fqn, counter, decls);
                self.expr(index, owner, scope, binary, fqn, counter, decls);
            }
            ExprKind::NewArray { dims, init, .. } => {
                for d in dims.iter_mut().flatten() {
                    self.expr(d, owner, scope, binary, fqn, counter, decls);
                }
                if let Some(es) = init {
                    for e in es.iter_mut() {
                        self.expr(e, owner, scope, binary, fqn, counter, decls);
                    }
                }
            }
            ExprKind::NewObject { args, body, ty, outer } => {
                if let Some(o) = outer {
                    self.expr(o, owner, scope, binary, fqn, counter, decls);
                }
                for a in args.iter_mut() {
                    self.expr(a, owner, scope, binary, fqn, counter, decls);
                }
                // Anónima → local sintética `$N`. Los **argumentos de super** (`new Base(10){…}`) se
                // conservan en el `new $N(10)`: `make_anon` sintetiza en `$N` un constructor que los
                // reenvía a `super(...)`.
                if body.is_some() {
                    let members = body.take().unwrap();
                    let ty_clone = ty.clone();
                    let arity = args.len();
                    if let Some((name, decl)) =
                        self.make_anon(&ty_clone, members, arity, owner, scope, binary, fqn, counter)
                    {
                        decls.push(Stmt::new(Pos::default(), StmtKind::LocalClass(decl)));
                        *ty = Type::Class(name);
                    }
                }
            }
            _ => {}
        }
    }

    /// Sintetiza y **registra** la clase local `$N` de una anónima: su supertipo sale de `ty`
    /// (`extends` si es clase, `implements` si es interfaz) y sus miembros son el cuerpo. Devuelve su
    /// nombre y su `ClassDecl` (para el `LocalClass` que la declara).
    #[allow(clippy::too_many_arguments)]
    fn make_anon(
        &mut self,
        ty: &Type,
        members: Vec<Member>,
        arity: usize,
        owner: SymbolId,
        scope: ScopeId,
        binary: &str,
        fqn: &str,
        counter: &mut u32,
    ) -> Option<(String, ClassDecl)> {
        // Supertipo: si `ty` es una **interfaz**, la anónima extiende `Object` y la implementa; si es
        // una **clase**, la extiende. El grafo **resuelto** (`super_type`/`interface_types`) se fija
        // explícitamente porque `$N` se registra después de la resolución de Enter —sin él, un
        // `Base b = new Base(){…}` no vería `$N <: Base`—.
        let ty_sym = self.type_symbol(ty, scope);
        let object = self.table.external("Object").map(RType::Class);
        let is_iface = ty_sym.is_some_and(|id| {
            matches!(&self.table.symbol(id).kind, SymbolKind::Class { kind: TypeKind::Interface, .. })
        });

        // **Argumentos de super** (`new Base(10){…}`): se sintetiza en `$N` un constructor de reenvío
        // `$N(params) { super(params); }`, con los parámetros del constructor del supertipo elegido
        // por **aridad**. Sin un constructor que encaje, no se baja (lo corta la barrera del emisor).
        let mut members = members;
        let mut fwd_resolved: Option<Vec<RType>> = None;
        if arity > 0 {
            let sup = ty_sym.filter(|_| !is_iface)?;
            let ctor = super::attribute::constructors(self.table, sup)
                .into_iter()
                .find(|&c| matches!(&self.table.symbol(c).kind, SymbolKind::Method { params, .. } if params.len() == arity))?;
            let syn: Vec<Param> = match &self.table.symbol(ctor).kind {
                SymbolKind::Method { params, .. } => params
                    .iter()
                    .enumerate()
                    .map(|(i, p)| Param {
                        annotations: Vec::new(),
                        ty: p.ty.clone(),
                        name: format!("$a{i}"),
                        varargs: false,
                        is_final: false,
                        type_annos: Vec::new(),
                    })
                    .collect(),
                _ => Vec::new(),
            };
            fwd_resolved = Some(match self.table.resolved(ctor) {
                Some(Resolved::Method { params, .. }) => params.clone(),
                _ => vec![RType::Unresolved; arity],
            });
            let super_args: Vec<Expr> = (0..arity)
                .map(|i| Expr::new(Pos::default(), ExprKind::Name(format!("$a{i}"))))
                .collect();
            let super_call = Expr::new(
                Pos::default(),
                ExprKind::Call { target: None, name: "super".to_string(), args: super_args, type_args: Vec::new() },
            );
            members.push(Member::Method(MethodDecl {
                doc: None,
                annotations: Vec::new(),
                return_annos: Vec::new(),
                throws_annos: Vec::new(),
                pos: Pos::default(),
                modifiers: Vec::new(),
                type_params: Vec::new(),
                return_type: Type::Void,
                name: String::new(), // se completa abajo con el nombre `$N`
                params: syn,
                throws: Vec::new(),
                body: Some(Block(vec![Stmt::new(Pos::default(), StmtKind::Expr(super_call))])),
                is_constructor: true,
            }));
        }

        *counter += 1;
        let name = format!("${}", *counter);
        let anon_fqn = qualify(fqn, &name);
        let anon_binary = format!("{binary}${}", *counter);
        // Completar el nombre del constructor de reenvío con el nombre real de la clase.
        for m in members.iter_mut() {
            if let Member::Method(me) = m {
                if me.is_constructor && me.name.is_empty() {
                    me.name = name.clone();
                }
            }
        }

        let (extends, implements, super_rt, iface_rts) = if is_iface {
            (None, vec![ty.clone()], object, ty_sym.map(RType::Class).into_iter().collect())
        } else {
            (Some(ty.clone()), Vec::new(), ty_sym.map(RType::Class).or(object), Vec::new())
        };

        let members_scope = self.table.new_scope(Some(scope), None);
        let sym = self.table.new_symbol(Symbol {
            name: name.clone(),
            kind: SymbolKind::Class {
                kind: TypeKind::Class,
                binary: anon_binary,
                extends: extends.clone(),
                implements: implements.clone(),
                permits: Vec::new(),
                members: members_scope,
            },
            owner: Some(owner),
            modifiers: Vec::new(),
        });
        self.table.set_scope_owner(members_scope, sym);
        self.table.register_class(&anon_fqn, sym);
        self.table.define(scope, &name, sym);
        self.table
            .set_resolved(sym, Resolved::Class { super_type: super_rt, interface_types: iface_rts, permitted: Vec::new() });

        let decl = ClassDecl {
            doc: None,
            pos: Pos::default(),
            annotations: Vec::new(),
            modifiers: Vec::new(),
            kind: TypeKind::Class,
            name: name.clone(),
            type_params: Vec::new(),
            components: Vec::new(),
            extends,
            extends_annos: Vec::new(),
            implements,
            implements_annos: Vec::new(),
            permits: Vec::new(),
            enum_constants: Vec::new(),
            members,
            annotation_defaults: Vec::new(),
        };
        member_enter_type(self.table, self.errors, &decl, fqn);
        // El constructor de reenvío quedó registrado por `member_enter` **sin `Resolved`** (la anónima
        // se registra después de la resolución de Enter): se lo fijamos con los parámetros del
        // constructor del supertipo, para que `new $N(args)` resuelva y el desugar le anteponga las
        // capturas.
        if let Some(rparams) = fwd_resolved {
            if let Some(ctor) = super::attribute::constructors(self.table, sym)
                .into_iter()
                .find(|&c| matches!(&self.table.symbol(c).kind, SymbolKind::Method { params, .. } if params.len() == arity))
            {
                self.table.set_resolved(
                    ctor,
                    Resolved::Method { params: rparams, ret: RType::Void, varargs: false, throws: Vec::new() },
                );
            }
        }
        Some((name, decl))
    }

    /// El símbolo al que resuelve un `Type` (clase del fuente o externo), o `None`.
    fn type_symbol(&self, ty: &Type, scope: ScopeId) -> Option<SymbolId> {
        let name = match ty {
            Type::Class(n) | Type::Parameterized { base: n, .. } => n.as_str(),
            _ => return None,
        };
        self.table.resolve_type(scope, name).or_else(|| self.table.external(name))
    }
}

/// **Registra las clases locales** (§14.3) tras `enter`, con `&mut unit`/`&mut table`. Hace lo que
/// `enter` no puede porque muta el AST: además de crear el símbolo de cada local, la **renombra** a un
/// nombre único por unidad —`L` → `1L`, `2L`…— y **reescribe sus referencias** dentro del bloque donde
/// es visible, de modo que dos locales homónimas en distintos bloques del mismo enclosing dejen de
/// colisionar río abajo (la tabla clavea el tipo por nombre y el codegen deriva el binary de él).
///
/// El alcance es **léxico por bloque** (§14.3: visible de su declaración al fin del bloque): un stack
/// de `(nombre_fuente → nombre_único)` se empuja al declarar y se descarta al cerrar el bloque, y una
/// referencia a un nombre visible toma el de la local **más cercana**. Resuelta la reescritura, el
/// resto del pipeline ve nombres únicos en un scope plano y es ajeno a la colisión. El anidamiento
/// **local-en-local** queda pendiente: una local dentro de otra no se registra (aunque sus referencias
/// a las visibles sí se reescriben).
pub fn register_local_classes(
    unit: &mut CompilationUnit,
    table: &mut SymbolTable,
    errors: &mut Vec<Error>,
) {
    let base = unit.package.as_deref().unwrap_or("").to_string();
    {
        let mut namer = LocalNamer { table, errors, n: 0 };
        for class in &mut unit.types {
            namer.class(class, &base);
        }
    }
    // Las locales recién registradas traen sus firmas **sintácticas**: `resolve_symbols` ya corrió en
    // `enter` (antes que esta pasada), así que se re-corre para decorar sus métodos/campos con su
    // `Resolved` —sin él, `resolve_overload` no encuentra una firma aplicable y `l.m()` no resuelve—.
    resolve_symbols(table);
}

/// El contexto de la clase **enclosing** donde se registran las locales de un cuerpo de método: su
/// símbolo, su scope de miembros y su FQN/binary para derivar los de cada local.
struct LocalCtx {
    cid: SymbolId,
    scope: ScopeId,
    fqn: String,
    binary: String,
}

struct LocalNamer<'a> {
    table: &'a mut SymbolTable,
    errors: &'a mut Vec<Error>,
    /// Contador **global a la unidad**: da el prefijo único (`1L`, `2L`…), al estilo de javac.
    n: u32,
}

impl LocalNamer<'_> {
    fn class(&mut self, class: &mut ClassDecl, enclosing_fqn: &str) {
        let fqn = qualify(enclosing_fqn, &class.name);
        let Some(cid) = self.table.class(&fqn) else { return };
        let (scope, binary) = match &self.table.symbol(cid).kind {
            SymbolKind::Class { members, binary, .. } => (*members, binary.clone()),
            _ => return,
        };
        let ctx = LocalCtx { cid, scope, fqn: fqn.clone(), binary };
        for member in &mut class.members {
            match member {
                Member::Method(m) => {
                    if let Some(body) = &mut m.body {
                        // Cada cuerpo de método arranca con un scope de locales **fresco**: una local no
                        // escapa a otro método.
                        self.block(&mut body.0, &ctx, true, &mut Vec::new());
                    }
                }
                Member::StaticInit(b) | Member::InstanceInit(b) => {
                    self.block(&mut b.0, &ctx, true, &mut Vec::new());
                }
                Member::Type(nested) => self.class(nested, &fqn),
                Member::Field(_) => {}
            }
        }
    }

    /// Recorre un bloque manteniendo el **scope léxico**: lo que declara se descarta al cerrarlo. Con
    /// `reg` en `true` registra las locales que aparecen; en `false` (dentro del cuerpo de otra clase)
    /// solo reescribe referencias.
    fn block(
        &mut self,
        stmts: &mut [Stmt],
        ctx: &LocalCtx,
        reg: bool,
        stack: &mut Vec<(String, String)>,
    ) {
        let mark = stack.len();
        for s in stmts.iter_mut() {
            self.stmt(s, ctx, reg, stack);
        }
        stack.truncate(mark);
    }

    #[allow(clippy::too_many_arguments)]
    fn stmt(&mut self, s: &mut Stmt, ctx: &LocalCtx, reg: bool, stack: &mut Vec<(String, String)>) {
        match &mut s.kind {
            StmtKind::LocalClass(lc) => {
                if reg {
                    self.register_local(lc, ctx, stack);
                } else {
                    // Local-en-local (cola aparte): no se registra; solo se reescribe con lo visible.
                    self.rewrite_class(lc, ctx, stack);
                }
            }
            StmtKind::LocalVar { ty, init, .. } => {
                rewrite_type(ty, stack);
                if let Some(e) = init {
                    self.expr(e, ctx, reg, stack);
                }
            }
            StmtKind::ForEach { ty, iterable, body, .. } => {
                rewrite_type(ty, stack);
                self.expr(iterable, ctx, reg, stack);
                self.stmt(body, ctx, reg, stack);
            }
            StmtKind::Return(Some(e))
            | StmtKind::Expr(e)
            | StmtKind::Throw(e)
            | StmtKind::Yield(e) => self.expr(e, ctx, reg, stack),
            StmtKind::Assert { cond, message } => {
                self.expr(cond, ctx, reg, stack);
                if let Some(m) = message {
                    self.expr(m, ctx, reg, stack);
                }
            }
            StmtKind::If { cond, then, els } => {
                self.expr(cond, ctx, reg, stack);
                self.stmt(then, ctx, reg, stack);
                if let Some(e) = els {
                    self.stmt(e, ctx, reg, stack);
                }
            }
            StmtKind::While { cond, body } | StmtKind::Do { body, cond } => {
                self.expr(cond, ctx, reg, stack);
                self.stmt(body, ctx, reg, stack);
            }
            StmtKind::For { init, cond, update, body } => {
                // El `for` abre su propio scope: un tipo declarado en el `init` no escapa al bucle.
                let mark = stack.len();
                if let Some(i) = init {
                    self.stmt(i, ctx, reg, stack);
                }
                if let Some(c) = cond {
                    self.expr(c, ctx, reg, stack);
                }
                for u in update.iter_mut() {
                    self.expr(u, ctx, reg, stack);
                }
                self.stmt(body, ctx, reg, stack);
                stack.truncate(mark);
            }
            StmtKind::Block(b) => self.block(&mut b.0, ctx, reg, stack),
            StmtKind::Synchronized { lock, body } => {
                self.expr(lock, ctx, reg, stack);
                self.block(&mut body.0, ctx, reg, stack);
            }
            StmtKind::Try { resources, body, catches, finally } => {
                self.block(resources, ctx, reg, stack); // los recursos son declaraciones de variable
                self.block(&mut body.0, ctx, reg, stack);
                for c in catches.iter_mut() {
                    for t in c.types.iter_mut() {
                        rewrite_type(t, stack);
                    }
                    self.block(&mut c.body.0, ctx, reg, stack);
                }
                if let Some(f) = finally {
                    self.block(&mut f.0, ctx, reg, stack);
                }
            }
            StmtKind::Switch { selector, cases } => {
                self.expr(selector, ctx, reg, stack);
                for c in cases.iter_mut() {
                    for l in c.labels.iter_mut() {
                        rewrite_case_label(l, stack);
                    }
                    if let Some(g) = &mut c.guard {
                        self.expr(g, ctx, reg, stack);
                    }
                    match &mut c.body {
                        SwitchBody::Arrow(st) => self.stmt(st, ctx, reg, stack),
                        SwitchBody::Colon(ss) => {
                            for st in ss.iter_mut() {
                                self.stmt(st, ctx, reg, stack);
                            }
                        }
                    }
                }
            }
            StmtKind::Labeled { body, .. } => self.stmt(body, ctx, reg, stack),
            _ => {}
        }
    }

    /// Registra una clase local: la **renombra** a única (`1L`), la hace visible en el `stack`,
    /// reescribe sus **firmas** (antes de `member_enter_type`, que las guarda sintácticas), la entra a
    /// la tabla como tipo anidado del enclosing, y **recorre su cuerpo con `reg=true`** tomándola a
    /// ella como nuevo enclosing — así una **local dentro de otra** (§14.3) también se registra, como
    /// tipo anidado suyo (`Outer$1L1$2L2`).
    fn register_local(&mut self, lc: &mut ClassDecl, ctx: &LocalCtx, stack: &mut Vec<(String, String)>) {
        self.n += 1;
        let unique = format!("{}{}", self.n, lc.name);
        let source = std::mem::replace(&mut lc.name, unique.clone());
        // Visible (nombre_fuente → único) para su propio cuerpo y el resto del bloque.
        stack.push((source, unique.clone()));
        // Firmas antes de `member_enter_type`; los cuerpos van después (necesitan el símbolo creado).
        self.rewrite_signatures(lc, stack);
        let lc_fqn = qualify(&ctx.fqn, &unique);
        let lc_binary = format!("{}${}", ctx.binary, unique);
        let members = self.table.new_scope(Some(ctx.scope), None);
        let sym = self.table.new_symbol(Symbol {
            name: unique.clone(),
            kind: SymbolKind::Class {
                kind: lc.kind,
                binary: lc_binary.clone(),
                extends: lc.extends.clone(),
                implements: lc.implements.clone(),
                permits: lc.permits.clone(),
                members,
            },
            owner: Some(ctx.cid),
            modifiers: lc.modifiers.clone(),
        });
        self.table.set_scope_owner(members, sym);
        self.table.register_class(&lc_fqn, sym);
        self.table.define(ctx.scope, &unique, sym);
        self.table.set_pos(sym, lc.pos.line, lc.pos.col);
        define_type_params(self.table, members, sym, &lc.type_params);
        member_enter_type(self.table, self.errors, lc, &ctx.fqn);
        // El cuerpo de la local se recorre **con ella misma como enclosing** (`reg=true`): una local
        // anidada se registra como su tipo anidado; las referencias siguen usando el `stack` común.
        let sub = LocalCtx { cid: sym, scope: members, fqn: lc_fqn, binary: lc_binary };
        for ec in lc.enum_constants.iter_mut() {
            for a in ec.args.iter_mut() {
                self.expr(a, &sub, true, stack);
            }
        }
        self.walk_bodies(&mut lc.members, &sub, true, stack);
    }

    /// Reescribe las **firmas** de una clase (extends/implements/componentes/cotas y, por miembro, el
    /// tipo de cada campo y la firma de cada método) con las locales visibles — sin tocar los cuerpos.
    fn rewrite_signatures(&mut self, lc: &mut ClassDecl, stack: &[(String, String)]) {
        if let Some(e) = &mut lc.extends {
            rewrite_type(e, stack);
        }
        for i in lc.implements.iter_mut() {
            rewrite_type(i, stack);
        }
        for c in lc.components.iter_mut() {
            rewrite_type(&mut c.ty, stack);
        }
        for tp in lc.type_params.iter_mut() {
            for b in tp.bounds.iter_mut() {
                rewrite_type(b, stack);
            }
        }
        rewrite_member_sigs(&mut lc.members, stack);
    }

    /// Reescribe **solo** los tipos del subárbol de una clase (una anónima, o una local dentro del
    /// cuerpo de otra clase) sin registrar nada — `reg=false` en los cuerpos.
    fn rewrite_class(&mut self, lc: &mut ClassDecl, ctx: &LocalCtx, stack: &mut Vec<(String, String)>) {
        self.rewrite_signatures(lc, stack);
        for ec in lc.enum_constants.iter_mut() {
            for a in ec.args.iter_mut() {
                self.expr(a, ctx, false, stack);
            }
        }
        self.walk_bodies(&mut lc.members, ctx, false, stack);
    }

    /// Recorre los **cuerpos** de los miembros (inicializadores de campo, cuerpos de método, `static`/
    /// instance init) — las firmas ya las hizo [`rewrite_signatures`]—, propagando `reg`: con `true` se
    /// registran las locales que aparezcan; con `false` solo se reescribe.
    fn walk_bodies(
        &mut self,
        members: &mut [Member],
        ctx: &LocalCtx,
        reg: bool,
        stack: &mut Vec<(String, String)>,
    ) {
        for m in members.iter_mut() {
            match m {
                Member::Field(f) => {
                    if let Some(e) = &mut f.init {
                        self.expr(e, ctx, reg, stack);
                    }
                }
                Member::Method(me) => {
                    if let Some(b) = &mut me.body {
                        self.block(&mut b.0, ctx, reg, stack);
                    }
                }
                // Una clase **miembro** de una local (no una local anidada): fuera de alcance de esta
                // cola; se reescriben sus tipos pero no se registra.
                Member::Type(nested) => self.rewrite_class(nested, ctx, stack),
                Member::StaticInit(b) | Member::InstanceInit(b) => {
                    self.block(&mut b.0, ctx, reg, stack)
                }
            }
        }
    }

    fn expr(&mut self, e: &mut Expr, ctx: &LocalCtx, reg: bool, stack: &mut Vec<(String, String)>) {
        match &mut e.kind {
            ExprKind::Binary { lhs, rhs, .. } => {
                self.expr(lhs, ctx, reg, stack);
                self.expr(rhs, ctx, reg, stack);
            }
            ExprKind::Unary { expr, .. } => self.expr(expr, ctx, reg, stack),
            ExprKind::Assign { target, value, .. } => {
                self.expr(target, ctx, reg, stack);
                self.expr(value, ctx, reg, stack);
            }
            ExprKind::Ternary { cond, then, els } => {
                self.expr(cond, ctx, reg, stack);
                self.expr(then, ctx, reg, stack);
                self.expr(els, ctx, reg, stack);
            }
            ExprKind::Call { target, args, type_args, .. } => {
                if let Some(t) = target {
                    self.expr(t, ctx, reg, stack);
                }
                for a in args.iter_mut() {
                    self.expr(a, ctx, reg, stack);
                }
                for ta in type_args.iter_mut() {
                    rewrite_type_arg(ta, stack);
                }
            }
            ExprKind::Field { expr, .. } => self.expr(expr, ctx, reg, stack),
            ExprKind::Index { array, index } => {
                self.expr(array, ctx, reg, stack);
                self.expr(index, ctx, reg, stack);
            }
            ExprKind::Cast { ty, expr } => {
                rewrite_type(ty, stack);
                self.expr(expr, ctx, reg, stack);
            }
            ExprKind::InstanceOf { expr, ty, .. } => {
                rewrite_type(ty, stack);
                self.expr(expr, ctx, reg, stack);
            }
            ExprKind::ClassLit(ty) | ExprKind::QualifiedThis(ty) => rewrite_type(ty, stack),
            ExprKind::NewObject { ty, args, body, outer } => {
                rewrite_type(ty, stack);
                if let Some(o) = outer {
                    self.expr(o, ctx, reg, stack);
                }
                for a in args.iter_mut() {
                    self.expr(a, ctx, reg, stack);
                }
                if let Some(members) = body {
                    // Cuerpo de una anónima: sus tipos ven las locales visibles; sus propias anónimas las
                    // baja `hoist_anonymous` aparte, así que acá solo se reescribe (`reg=false`).
                    rewrite_member_sigs(members, stack);
                    self.walk_bodies(members, ctx, false, stack);
                }
            }
            ExprKind::NewArray { elem, dims, init } => {
                rewrite_type(elem, stack);
                for d in dims.iter_mut().flatten() {
                    self.expr(d, ctx, reg, stack);
                }
                if let Some(es) = init {
                    for x in es.iter_mut() {
                        self.expr(x, ctx, reg, stack);
                    }
                }
            }
            ExprKind::Switch { selector, cases } => {
                self.expr(selector, ctx, reg, stack);
                for c in cases.iter_mut() {
                    for l in c.labels.iter_mut() {
                        rewrite_case_label(l, stack);
                    }
                    if let Some(g) = &mut c.guard {
                        self.expr(g, ctx, reg, stack);
                    }
                    match &mut c.body {
                        SwitchBody::Arrow(st) => self.stmt(st, ctx, reg, stack),
                        SwitchBody::Colon(ss) => {
                            for st in ss.iter_mut() {
                                self.stmt(st, ctx, reg, stack);
                            }
                        }
                    }
                }
            }
            ExprKind::Lambda { params, body } => {
                for p in params.iter_mut() {
                    rewrite_type(&mut p.ty, stack);
                }
                // El cuerpo de una lambda es un bloque nuevo; una local declarada adentro pertenece al
                // método sintético de la lambda (cola aparte), así que no se registra (`reg=false`).
                match body.as_mut() {
                    LambdaBody::Expr(x) => self.expr(x, ctx, false, stack),
                    LambdaBody::Block(b) => self.block(&mut b.0, ctx, false, stack),
                }
            }
            ExprKind::MethodRef { qualifier, type_args, .. } => {
                match qualifier.as_mut() {
                    MethodRefQualifier::Expr(x) => self.expr(x, ctx, reg, stack),
                    MethodRefQualifier::Type(t) => rewrite_type(t, stack),
                }
                for ta in type_args.iter_mut() {
                    rewrite_type_arg(ta, stack);
                }
            }
            _ => {}
        }
    }
}

/// Reescribe un tipo si nombra una clase local **visible**: `L` → su nombre único (`1L`). Recurre por
/// los argumentos de tipo y el tipo elemento de un array.
fn rewrite_type(ty: &mut Type, stack: &[(String, String)]) {
    match ty {
        Type::Class(name) => {
            if let Some(u) = visible(stack, name) {
                *name = u;
            }
        }
        Type::Parameterized { base, args } => {
            if let Some(u) = visible(stack, base) {
                *base = u;
            }
            for a in args.iter_mut() {
                rewrite_type_arg(a, stack);
            }
        }
        Type::Array(inner) => rewrite_type(inner, stack),
        _ => {}
    }
}

/// Reescribe las firmas (tipo de campo, retorno/parámetros/`throws` de método) de una lista de
/// miembros — el cuerpo de una clase local o anónima— con las locales visibles.
fn rewrite_member_sigs(members: &mut [Member], stack: &[(String, String)]) {
    for m in members.iter_mut() {
        match m {
            Member::Field(f) => rewrite_type(&mut f.ty, stack),
            Member::Method(me) => {
                rewrite_type(&mut me.return_type, stack);
                for p in me.params.iter_mut() {
                    rewrite_type(&mut p.ty, stack);
                }
                for t in me.throws.iter_mut() {
                    rewrite_type(t, stack);
                }
            }
            _ => {}
        }
    }
}

fn rewrite_type_arg(a: &mut TypeArg, stack: &[(String, String)]) {
    match a {
        TypeArg::Type(t) => rewrite_type(t, stack),
        TypeArg::Extends(t) | TypeArg::Super(t) => rewrite_type(t, stack),
        TypeArg::Wildcard => {}
    }
}

fn rewrite_case_label(l: &mut CaseLabel, stack: &[(String, String)]) {
    if let CaseLabel::Pattern(p) = l {
        rewrite_pattern(p, stack);
    }
}

fn rewrite_pattern(p: &mut Pattern, stack: &[(String, String)]) {
    match p {
        Pattern::Type { ty, .. } => rewrite_type(ty, stack),
        Pattern::Record { ty, components } => {
            rewrite_type(ty, stack);
            for c in components.iter_mut() {
                rewrite_pattern(c, stack);
            }
        }
    }
}

/// El nombre único de la local **visible** más cercana con nombre fuente `name` (la última empujada),
/// o `None` si `name` no es una local en scope.
fn visible(stack: &[(String, String)], name: &str) -> Option<String> {
    stack.iter().rev().find(|(src, _)| src == name).map(|(_, u)| u.clone())
}

/// Sintetiza los métodos que la spec agrega implícitamente a enums y records (los que el
/// usuario no haya declarado).
fn synth_implicit_methods(table: &mut SymbolTable, class: &ClassDecl, cid: SymbolId, scope: ScopeId) {
    let public = &[Modifier::Public];
    let public_static = &[Modifier::Public, Modifier::Static];
    match class.kind {
        TypeKind::Enum => {
            let e = Type::Class(class.name.clone());
            add_synth(table, scope, cid, "values", Vec::new(), Type::Array(Box::new(e.clone())), public_static);
            add_synth(table, scope, cid, "valueOf", vec![psig("name", str_ty())], e, public_static);
        }
        TypeKind::Record => {
            for c in &class.components {
                add_synth(table, scope, cid, &c.name, Vec::new(), c.ty.clone(), public);
            }
            add_synth(table, scope, cid, "equals", vec![psig("o", Type::Class("Object".into()))], Type::Prim(PrimType::Boolean), public);
            add_synth(table, scope, cid, "hashCode", Vec::new(), Type::Prim(PrimType::Int), public);
            add_synth(table, scope, cid, "toString", Vec::new(), str_ty(), public);
        }
        _ => {}
    }
}

fn str_ty() -> Type {
    Type::Class("String".to_string())
}

fn psig(name: &str, ty: Type) -> ParamSig {
    ParamSig { ty, name: name.to_string(), varargs: false }
}

/// Agrega un método sintético `name` **si no fue declarado** por el usuario.
fn add_synth(table: &mut SymbolTable, scope: ScopeId, cid: SymbolId, name: &str, params: Vec<ParamSig>, ret: Type, mods: &[Modifier]) {
    let declared = table
        .scope(scope)
        .get(name)
        .iter()
        .any(|&id| matches!(table.symbol(id).kind, SymbolKind::Method { is_constructor: false, .. }));
    if declared {
        return;
    }
    let sym = table.new_symbol(Symbol {
        name: name.to_string(),
        kind: SymbolKind::Method { params, return_type: ret, is_constructor: false, throws: Vec::new() },
        owner: Some(cid),
        modifiers: mods.to_vec(),
    });
    table.define(scope, name, sym);
}

/// Modificadores implícitos de un campo: en una interfaz, `public static final`.
fn implicit_field_mods(kind: TypeKind, declared: &[Modifier]) -> Vec<Modifier> {
    let mut m = declared.to_vec();
    if matches!(kind, TypeKind::Interface | TypeKind::Annotation) {
        for imp in [Modifier::Public, Modifier::Static, Modifier::Final] {
            if !m.contains(&imp) {
                m.push(imp);
            }
        }
    }
    m
}

/// Modificadores implícitos de un método: en una interfaz, un método sin cuerpo (ni `static`
/// ni `default`) es `public abstract`.
fn implicit_method_mods(kind: TypeKind, m: &MethodDecl) -> Vec<Modifier> {
    let mut mods = m.modifiers.clone();
    if matches!(kind, TypeKind::Interface | TypeKind::Annotation)
        && m.body.is_none()
        && !mods.contains(&Modifier::Static)
        && !mods.contains(&Modifier::Default)
    {
        for imp in [Modifier::Public, Modifier::Abstract] {
            if !mods.contains(&imp) {
                mods.push(imp);
            }
        }
    }
    mods
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::javac::{lexer::tokenize, parser::parse};

    fn enter_src(src: &str) -> (SymbolTable, Vec<Error>) {
        enter(&parse(tokenize(src).unwrap()).0)
    }

    #[test]
    fn a_generic_method_declares_its_own_type_variable() {
        // `<T> T id(T x)`: la `T` es del **método**, no de la clase, y tiene que resolver a un
        // símbolo real — si no, la firma queda `Unresolved` y la inferencia no tendría qué inferir.
        let (t, errs) = enter_src("class C { <T> T id(T x) { return x; } }");
        assert!(errs.is_empty(), "{errs:?}");
        let cid = t.class("C").unwrap();
        let id = t.members_of(cid).into_iter().find(|&m| t.symbol(m).name == "id").unwrap();
        let Some(Resolved::Method { params, ret, .. }) = t.resolved(id) else { panic!() };
        assert!(matches!(ret, RType::TypeVar(_)), "el retorno es la variable T: {ret:?}");
        assert_eq!(params[0], *ret, "el param y el retorno son la **misma** T");
    }

    #[test]
    fn reads_the_generic_signature_of_a_jdk_class() {
        // `java.util.List<E>`: sin el atributo `Signature` solo veríamos `List`, y su `E` no
        // existiría como símbolo.
        let (t, _e) = enter_src("import java.util.List;\nclass A { List<String> xs; }");
        let list = t.external("List").expect("List cargado del classpath");
        let tps = crate::javac::types::type_params_of(&t, list);
        assert_eq!(tps.len(), 1, "List declara un parámetro de tipo");
        assert_eq!(t.symbol(tps[0]).name, "E");
    }

    #[test]
    fn a_jdk_generic_method_keeps_its_type_variable() {
        // `List<E>.get(int)` devuelve `E`, no `Object`: eso solo se sabe por el `Signature`.
        let (t, _e) = enter_src("import java.util.List;\nclass A { List<String> xs; }");
        let list = t.external("List").expect("List cargado");
        let get = t
            .members_of(list)
            .into_iter()
            .find(|&m| t.symbol(m).name == "get")
            .expect("List.get existe");
        let Some(Resolved::Method { ret, .. }) = t.resolved(get) else { panic!() };
        assert!(
            matches!(ret, RType::TypeVar(_)),
            "el retorno de List.get debería ser la variable `E`, no `Object`: {ret:?}"
        );
    }

    #[test]
    fn loads_wrappers_with_their_real_hierarchy() {
        // El boxing de la fase 2 del overload resolution necesita `Integer <: Number`, así que
        // los wrappers se cargan del classpath aunque el fuente no los nombre.
        let (t, _e) = enter_src("class A {}");
        let integer = t.external("Integer").expect("Integer registrado");
        let number = t.external("Number").expect("Number registrado");
        assert_eq!(
            t.super_class(integer),
            Some(number),
            "`Integer` debe traer su superclase real del JDK 25; si resolvió a `Object`, el finder \
             está leyendo el `Integer` recortado de `boot/` en vez del del classpath"
        );
    }

    #[test]
    fn builds_class_and_member_symbols() {
        let (t, errs) = enter_src("class A { int x; int f(int a) { return a; } A() {} }");
        assert!(errs.is_empty(), "errores inesperados: {errs:?}");
        let a = t.class("A").expect("A registrada");
        // Miembros en orden de declaración: x, f, ctor A.
        assert_eq!(t.members_of(a).len(), 3);
    }

    #[test]
    fn package_qualifies_the_name() {
        let (t, _errs) = enter_src("package p.q; class A {}");
        assert!(t.class("p.q.A").is_some());
        assert!(t.class("A").is_none());
    }

    #[test]
    fn creates_package_symbol_and_owns_classes() {
        let (t, _errs) = enter_src("package p.q; class A {} class B {}");
        // El paquete existe y es dueño de las dos clases.
        let pkg = t.package("p.q").expect("paquete p.q");
        assert_eq!(t.members_of(pkg).len(), 2);
        // Sin `package` → el paquete sin nombre (clave "").
        let (t2, _e) = enter_src("class C {}");
        let unnamed = t2.package("").expect("paquete sin nombre");
        assert_eq!(t2.members_of(unnamed).len(), 1);
    }

    #[test]
    fn detects_duplicate_field_and_method() {
        // campo `x` dos veces + método `m(int)` dos veces (misma firma).
        let (_t, errs) = enter_src("class A { int x; int x; void m(int a) {} void m(int b) {} }");
        assert_eq!(errs.len(), 2, "esperaba 2 errores, hubo: {errs:?}");
    }

    #[test]
    fn overload_is_not_duplicate() {
        // Mismo nombre, distintos tipos de parámetro → sobrecarga válida.
        let (_t, errs) = enter_src("class A { void m(int a) {} void m(long a) {} void m() {} }");
        assert!(errs.is_empty(), "la sobrecarga no debe dar error: {errs:?}");
    }

    #[test]
    fn detects_duplicate_type() {
        let (_t, errs) = enter_src("class A {} class A {}");
        assert_eq!(errs.len(), 1);
    }

    #[test]
    fn enters_nested_types() {
        let (t, errs) = enter_src("class Outer { int a; static class Inner { int b; } }");
        assert!(errs.is_empty(), "errores inesperados: {errs:?}");
        let outer = t.class("Outer").expect("Outer");
        let inner = t.class("Outer.Inner").expect("Outer.Inner anidada");
        // Inner pertenece a Outer. Con el ctor por defecto sintetizado:
        // Outer = `a` + clase `Inner` + ctor = 3;  Inner = `b` + ctor = 2.
        assert_eq!(t.symbol(inner).owner, Some(outer));
        assert_eq!(t.members_of(outer).len(), 3);
        assert_eq!(t.members_of(inner).len(), 2);
    }

    #[test]
    fn nested_type_has_binary_name() {
        let (t, _e) = enter_src("class Outer { class Inner {} }");
        let inner = t.class("Outer.Inner").expect("Outer.Inner");
        let SymbolKind::Class { binary, .. } = &t.symbol(inner).kind else { panic!() };
        assert_eq!(binary, "Outer$Inner");
    }

    #[test]
    fn enters_enum_and_record() {
        let (t, errs) =
            enter_src("enum Color { RED, GREEN, BLUE; int rgb; } record Point(int x, int y) {}");
        assert!(errs.is_empty(), "errores inesperados: {errs:?}");
        // Color: 3 constantes + rgb + ctor + values() + valueOf() = 7.
        let color = t.class("Color").expect("Color");
        assert_eq!(t.members_of(color).len(), 7);
        // Point: x,y + ctor canónico + x(),y() + equals + hashCode + toString = 8.
        let point = t.class("Point").expect("Point");
        assert_eq!(t.members_of(point).len(), 8);
    }

    fn has_ctor(t: &SymbolTable, cid: SymbolId) -> Option<SymbolId> {
        t.members_of(cid)
            .into_iter()
            .find(|&id| matches!(t.symbol(id).kind, SymbolKind::Method { is_constructor: true, .. }))
    }

    #[test]
    fn synthesizes_default_constructor() {
        let (t, _e) = enter_src("class A { int x; }");
        assert!(has_ctor(&t, t.class("A").unwrap()).is_some());
    }

    #[test]
    fn record_gets_canonical_constructor() {
        let (t, _e) = enter_src("record P(int x, int y) {}");
        let ctor = has_ctor(&t, t.class("P").unwrap()).expect("ctor canónico");
        let SymbolKind::Method { params, .. } = &t.symbol(ctor).kind else { panic!() };
        assert_eq!(params.len(), 2);
    }

    #[test]
    fn resolves_java_lang_and_reports_unknown() {
        // `Thread` y `String` resuelven (java.lang); `Foo` no.
        let (_t, errs) = enter_src("class A extends Thread { String s; Foo bad; }");
        assert_eq!(errs.len(), 1, "solo Foo debe faltar: {errs:?}");
        assert!(errs[0].message.contains("Foo"));
    }

    #[test]
    fn wildcard_import_suppresses_unknown() {
        // Con `import *` no podemos descartar → no se reporta `Foo`.
        let (_t, errs) = enter_src("import java.util.*; class A { Foo x; }");
        assert!(errs.is_empty(), "{errs:?}");
    }

    #[test]
    fn generic_type_params_resolve() {
        // `T` (de la clase) y `U` (del método) resuelven en sus firmas — sin "no se encuentra".
        let (_t, errs) = enter_src("class Box<T> { T value; <U> U id(U x) { return x; } }");
        assert!(errs.is_empty(), "{errs:?}");
    }

    #[test]
    fn detects_inheritance_cycle() {
        let (_t, errs) = enter_src("class A extends B {} class B extends A {}");
        assert_eq!(errs.len(), 1, "un solo error de ciclo: {errs:?}");
        assert!(errs[0].message.contains("ciclo"));
    }

    #[test]
    fn computes_nest_host() {
        let (t, _e) = enter_src("class Outer { class Mid { class Inner {} } }");
        let inner = t.class("Outer.Mid.Inner").unwrap();
        assert_eq!(t.nest_host(inner), t.class("Outer").unwrap());
    }

    #[test]
    fn error_carries_source_position() {
        // El campo `Foo x` de tipo inexistente está en la línea 2 → el error se ubica ahí.
        let (_t, errs) = enter_src("class A {\n    Foo x;\n}");
        assert_eq!(errs.len(), 1);
        assert_eq!(errs[0].line, 2, "error mal ubicado: {:?}", errs[0]);
    }

    #[test]
    fn records_static_imports() {
        let (t, _e) = enter_src(
            "import static java.lang.Math.max; import static java.lang.Math.*; class A {}",
        );
        assert_eq!(t.static_single.get("max").map(String::as_str), Some("java.lang.Math"));
        assert!(t.static_on_demand.iter().any(|s| s == "java.lang.Math"));
    }

    #[test]
    fn a_plain_class_inherits_object_implicitly() {
        // Finding #22: una clase sin `extends` explícito debe enlazar a `java.lang.Object` en el grafo
        // de símbolos, para que la resolución de miembros sobre `this` alcance los métodos heredados
        // (`getClass`/`hashCode`/`toString`). Antes quedaba `super_class = None`.
        let (t, _e) = enter_src("class A {}");
        let a = t.class("A").expect("clase A");
        let object = t.external("Object").expect("Object cargado del classpath");
        assert_eq!(t.super_class(a), Some(object), "A hereda de Object implícitamente");
        // La propia `Object` no se hereda a sí misma (sin ciclo).
        assert_ne!(t.super_class(object), Some(object), "Object no es su propia superclase");
    }

    #[test]
    fn an_interface_has_no_implicit_object_superclass() {
        // Una interfaz **no** tiene superclase (§9.1.3): el default de Object es solo para clases.
        let (t, _e) = enter_src("interface I {}");
        let i = t.class("I").expect("interfaz I");
        assert_eq!(t.super_class(i), None, "una interfaz no hereda de Object como superclase");
    }

    #[test]
    fn class_finder_loads_real_members() {
        // `Thread` se carga del classpath (`boot/`) con sus miembros reales, no un stub vacío.
        let (t, _e) = enter_src("class A extends Thread {}");
        let thread = t.external("Thread").expect("Thread cargado del classpath");
        assert!(!t.members_of(thread).is_empty(), "Thread debería tener miembros reales");
    }

    #[test]
    fn persists_resolved_super_and_signatures() {
        let (t, _e) = enter_src("class A extends Thread { String s; int f(A a) { return 0; } }");
        let a = t.class("A").unwrap();
        // Grafo: A → Thread (el símbolo externo cargado).
        assert_eq!(t.super_class(a), Some(t.external("Thread").unwrap()));
        // Campo `s`: tipo resuelto = la clase String (externa).
        let string = t.external("String").unwrap();
        let s = t.members_of(a).iter().find(|&&id| t.symbol(id).name == "s").copied().unwrap();
        assert!(matches!(t.resolved(s), Some(Resolved::Field(RType::Class(c))) if *c == string));
        // Método `f(A): int`: param resuelto a la clase A, retorno int.
        let f = t.members_of(a).iter().find(|&&id| t.symbol(id).name == "f").copied().unwrap();
        let Some(Resolved::Method { params, ret, .. }) = t.resolved(f) else { panic!() };
        assert_eq!(params[0], RType::Class(a));
        assert_eq!(*ret, RType::Prim(super::super::ast::PrimType::Int));
    }
}
