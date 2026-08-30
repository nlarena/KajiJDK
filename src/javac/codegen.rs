//! El **generador de código**: recorre el AST ya validado y decorado por la pasada 2 y emite el
//! bytecode de cada método, delegando en [`class_writer`](super::class_writer) el constant pool y
//! la serialización.
//!
//! ## Alcance
//!
//! - **Aritmética y tipos**: las cinco categorías (`int`/`long`/`float`/`double`/referencia) con sus
//!   familias de opcodes, literales de cada tipo (`ldc2_w` para los anchos, `ldc` para `String`) y la
//!   **promoción numérica binaria** (§5.6.2) con los doce `x2y` — con los desplazamientos exceptuados
//!   (§15.19), que no promueven su operando derecho.
//! - **Locales y objetos**: `iload`/`istore` por los slots de la pasada 2, `new`+`dup`+
//!   `invokespecial`, `getfield`/`putfield`, `getstatic`/`putstatic`, `invokevirtual`, `checkcast`,
//!   asignación a local y a campo, `++`/`--` (con `iinc` cuando alcanza), y el `super()` implícito de
//!   todo constructor (§8.8.7).
//! - **Flujo de control**: etiquetas con parcheo de saltos; `if`/`while`/`do`/`for`, comparaciones
//!   por categoría, el **ternario** (§15.25, con sus dos ramas promovidas al tipo del todo) y
//!   `&&`/`||` compilados como **saltos** (cortocircuito real, no un booleano calculado).
//! - **`switch`** en bytecode: `tableswitch` o `lookupswitch` según la **densidad** de las
//!   etiquetas, con el relleno de alineación y los desplazamientos de 4 bytes que piden. Preserva el
//!   *fall-through* de la forma de dos puntos y lo corta en la de flecha (§14.11.2). Los `switch`
//!   sobre `String`, `enum` y con *patterns* llegan acá ya bajados a `int` por el desugar.
//! - **`break`/`continue`, con y sin etiqueta**: una pila de sentencias "de las que se puede salir"
//!   distingue los dos destinos — un `switch` interpuesto captura un `break` pero **no** un
//!   `continue` (§14.16) — y soporta el **bloque etiquetado**, del que solo se sale con `break lbl`.
//! - **Excepciones y monitores**: `try`/`catch` con su tabla, `throw`, el `finally` **duplicado** en
//!   la salida normal y en un handler *catch-all* que re-lanza (§14.20.2) — la v69 ya no acepta
//!   `jsr`/`ret` — y `synchronized` con el mismo patrón: `monitorexit` en las dos salidas, para que
//!   una excepción no deje el monitor tomado.
//! - **`StackMapTable`** (§4.7.4): el frame de cada destino de salto, calculado como el **merge** de
//!   los estados que llegan ahí, y serializado en la forma **más compacta** según el frame anterior
//!   (`same`/`same_locals_1`/`append`/`chop`, con `full_frame` de respaldo). El emisor lleva la pila
//!   de operandos **tipada**, no solo su altura —
//!   que es lo que un frame tiene que declarar—, así que un destino alcanzado con algo ya empujado
//!   (`f(a, b > 0 ? 1 : 2)`) lo declara bien. Eso incluye el tipo `uninitialized` (tag 8): un objeto
//!   recién creado se identifica por el **offset de su `new`**, y cuando corre su `<init>` todas sus
//!   apariciones —pila y locales— pasan al tipo definitivo (§4.10.2.4).
//!
//! Toda construcción fuera de ese alcance pasa por [`Emitter::unsupported`], la **barrera**: se
//! acumula como error y `generate` no devuelve bytes. Emitir un `.class` a medias es el bug que ya
//! nos mordió dos veces —el `if` descartado, el `++` en posición de valor—; fallar fuerte no.
//!
//! **Falta**: `invokedynamic` (lambdas, referencias a método, el `toString`/`equals`/`hashCode` de
//! un `record`), las clases internas y anónimas, y el plegado de constantes que haría legal una
//! `static final int` como etiqueta de `case`.

use super::ast::{
    Annotation, AnnotationValue, AssignOp, BinOp, Binding, BootstrapArg, Block, CaseLabel,
    CatchClause, ClassDecl, CompilationUnit, Expr, ExprKind, Member, MethodDecl, Modifier, Pos,
    Param, PrimType, Stmt, StmtKind, SwitchBody, SwitchCase, Type, TypeArg, TypeKind, TypeParam,
    TypePathStep, TypeUseAnnot, UnOp,
};
use super::Error;
use std::collections::{BTreeMap, HashMap, HashSet};

use super::class_writer::{
    BootstrapMethod, ClassFile, ConstantPool, ExceptionEntry, FieldInfo, InnerClassEntry, MethodInfo,
    ParamInfo, RecordComponent,
};
use super::symbol::{RType, Resolved, ScopeId, SymbolId, SymbolKind, SymbolTable};
use super::types;

// Flags de acceso (JVMS Table 4.1-B / 4.6-A).
const ACC_PUBLIC: u16 = 0x0001;
const ACC_PRIVATE: u16 = 0x0002;
const ACC_PROTECTED: u16 = 0x0004;
const ACC_STATIC: u16 = 0x0008;
const ACC_FINAL: u16 = 0x0010;
const ACC_SUPER: u16 = 0x0020;
const ACC_INTERFACE: u16 = 0x0200;
const ACC_ABSTRACT: u16 = 0x0400;
const ACC_NATIVE: u16 = 0x0100; // método implementado por el VM, sin `Code` (§4.6)
// `ACC_VARARGS`: el último parámetro es un `T...` (§4.6). NO es decorativo y no lo cubre el
// descriptor, que dice `[LT;` y nada más. Es lo ÚNICO que le dice a **otra** unidad de
// compilación que la llamada puede escribirse desplegada: sin el flag, un `f(a, b)` contra
// este método no encuentra sobrecarga aplicable (#118).
const ACC_VARARGS: u16 = 0x0080;
// Flags **de campo** (§4.5). Comparten bit con dos de método —`ACC_VOLATILE` con `ACC_BRIDGE`,
// `ACC_TRANSIENT` con `ACC_VARARGS`— y por eso no pueden salir de la misma tabla que los demás: son
// tablas distintas, no un espacio común. Es lo que dejó a `volatile`/`transient` sin emitir (#115,
// #236) mientras el resto de los modificadores sí salían.
const ACC_VOLATILE: u16 = 0x0040;
const ACC_TRANSIENT: u16 = 0x0080;
const ACC_SYNCHRONIZED: u16 = 0x0020; // el método toma el monitor del receptor al entrar (§4.6)
const ACC_BRIDGE: u16 = 0x0040; // método puente sintetizado (§4.6)
const ACC_SYNTHETIC: u16 = 0x1000; // no aparece en el fuente (§4.6)
const ACC_ENUM: u16 = 0x4000; // tipo/campo `enum` (§4.1/§4.5)
const ACC_ANNOTATION: u16 = 0x2000; // el tipo es un `@interface` (§4.1)
const ACC_MODULE: u16 = 0x8000; // el `.class` es un descriptor de módulo (§4.1)
const ACC_OPEN: u16 = 0x0020; // `open module` (§4.7.25)
const ACC_TRANSITIVE: u16 = 0x0020; // `requires transitive` (§4.7.25)
const ACC_STATIC_PHASE: u16 = 0x0040; // `requires static` (§4.7.25)
const ACC_MANDATED: u16 = 0x8000; // implícito, no escrito en el fuente (el `requires java.base`)

/// Compila **cada tipo** de `unit` a su propio `.class` — top-level y anidados (§7.6, §8.1.3): una
/// unidad puede declarar varios tipos, y cada clase interna, la anónima `C$1` del `switch`-enum, etc.
/// son clases aparte. Devuelve `(nombre interno, bytes)` por cada una. La unidad ya pasó por Enter +
/// Attribute (necesita la decoración y la tabla).
///
/// Antes se emitía **solo el primer tipo**, con el nombre del archivo: un segundo `class` en el mismo
/// `.java` desaparecía en silencio, y el `.class` decía llamarse como el archivo aunque contuviera
/// otra clase — la última fuga silenciosa del back-end.
pub fn generate(
    unit: &super::ast::CompilationUnit,
    table: &SymbolTable,
) -> super::Result<Vec<(String, Vec<u8>)>> {
    let mut errors: Vec<Error> = Vec::new();
    let mut out: Vec<(String, Vec<u8>)> = Vec::new();
    let base = unit.package.as_deref().unwrap_or("");
    // Los tipos de anotación **retenidos en runtime** (§9.6.4.2): los `@interface` del fuente con
    // `@Retention(RUNTIME)` más las conocidas del JDK. Solo esas van a `RuntimeVisibleAnnotations`.
    let rt = runtime_retained_annotations(unit);
    // Qué anotaciones son type annotations por su `@Target` (§9.6.4.1) — para rutearlas a
    // `RuntimeVisibleTypeAnnotations` en vez de (o además de) `RuntimeVisibleAnnotations`.
    let tu = type_use_info(unit);
    // El `SourceFile` (4.7.10) es un atributo de la **unidad de compilacion**, no de cada clase:
    // las secundarias y las anidadas comparten archivo con la principal.
    let src = unit_source_file(unit);
    for class in &unit.types {
        gen_type(class, base, table, &rt, &tu, &src, &mut out, &mut errors);
    }
    // Un `module-info.java` (§7.7) produce un `module-info.class` con el atributo `Module`.
    if let Some(module) = &unit.module {
        out.push(("module-info".to_string(), gen_module_info(module)));
    }
    // Si algo no se pudo emitir, **no** se devuelve un `.class` a medias.
    match errors.into_iter().next() {
        Some(first) => Err(first),
        None => Ok(out),
    }
}

/// Emite el `module-info.class` (§4.1/§4.7.25): `ACC_MODULE`, `this_class = module-info`, sin
/// super/campos/métodos, y el atributo **`Module`** con las directivas. Se agrega el `requires
/// java.base` **mandated** implícito (salvo que el módulo sea `java.base` o ya lo requiera).
fn gen_module_info(module: &super::ast::ModuleDecl) -> Vec<u8> {
    let mut cf = ClassFile::new();
    cf.access_flags = ACC_MODULE;
    cf.this_class = cf.pool.class("module-info");
    cf.super_class = 0;
    cf.source_file = Some(cf.pool.utf8("module-info.java"));
    cf.module = Some(build_module_attr(&mut cf.pool, module));
    cf.to_bytes()
}

/// Serializa el **cuerpo** del atributo `Module` (§4.7.25): nombre + flags + versión, y las cinco
/// listas de directivas. Los nombres de módulo van con **puntos**; los de paquete/servicio, en
/// forma interna (con `/`), que ponen `pool.package`/`pool.class`.
fn build_module_attr(pool: &mut ConstantPool, module: &super::ast::ModuleDecl) -> Vec<u8> {
    use super::ast::ModuleDirective as D;
    let mut b = Vec::new();
    b.extend_from_slice(&pool.module(&module.name).to_be_bytes());
    b.extend_from_slice(&(if module.open { ACC_OPEN } else { 0 }).to_be_bytes());
    b.extend_from_slice(&0u16.to_be_bytes()); // module_version_index

    // requires — con el `java.base` mandated implícito al frente.
    let mut requires: Vec<(u16, u16)> = Vec::new();
    let mut has_java_base = false;
    for d in &module.directives {
        if let D::Requires { transitive, is_static, name } = d {
            if name == "java.base" {
                has_java_base = true;
            }
            let mut f = 0u16;
            if *transitive {
                f |= ACC_TRANSITIVE;
            }
            if *is_static {
                f |= ACC_STATIC_PHASE;
            }
            requires.push((pool.module(name), f));
        }
    }
    if !has_java_base && module.name != "java.base" {
        let jb = pool.module("java.base");
        requires.insert(0, (jb, ACC_MANDATED));
    }
    b.extend_from_slice(&(requires.len() as u16).to_be_bytes());
    for (m, f) in &requires {
        b.extend_from_slice(&m.to_be_bytes());
        b.extend_from_slice(&f.to_be_bytes());
        b.extend_from_slice(&0u16.to_be_bytes()); // requires_version_index
    }

    // exports / opens — misma forma (§4.7.25): paquete + flags + lista `to` de módulos.
    let emit_qualified = |b: &mut Vec<u8>, pool: &mut ConstantPool, pkgs: &[(&String, &Vec<String>)]| {
        b.extend_from_slice(&(pkgs.len() as u16).to_be_bytes());
        for (pkg, to) in pkgs {
            b.extend_from_slice(&pool.package(pkg).to_be_bytes());
            b.extend_from_slice(&0u16.to_be_bytes()); // flags
            b.extend_from_slice(&(to.len() as u16).to_be_bytes());
            for t in to.iter() {
                b.extend_from_slice(&pool.module(t).to_be_bytes());
            }
        }
    };
    let exports: Vec<(&String, &Vec<String>)> = module
        .directives
        .iter()
        .filter_map(|d| match d {
            D::Exports { package, to } => Some((package, to)),
            _ => None,
        })
        .collect();
    emit_qualified(&mut b, pool, &exports);
    let opens: Vec<(&String, &Vec<String>)> = module
        .directives
        .iter()
        .filter_map(|d| match d {
            D::Opens { package, to } => Some((package, to)),
            _ => None,
        })
        .collect();
    emit_qualified(&mut b, pool, &opens);

    // uses — lista de `Class` de servicios.
    let uses: Vec<&String> = module
        .directives
        .iter()
        .filter_map(|d| match d {
            D::Uses { service } => Some(service),
            _ => None,
        })
        .collect();
    b.extend_from_slice(&(uses.len() as u16).to_be_bytes());
    for s in &uses {
        let c = pool.class(&s.replace('.', "/"));
        b.extend_from_slice(&c.to_be_bytes());
    }

    // provides — servicio (`Class`) + lista de implementaciones (`Class`).
    let provides: Vec<(&String, &Vec<String>)> = module
        .directives
        .iter()
        .filter_map(|d| match d {
            D::Provides { service, with } => Some((service, with)),
            _ => None,
        })
        .collect();
    b.extend_from_slice(&(provides.len() as u16).to_be_bytes());
    for (service, with) in &provides {
        let c = pool.class(&service.replace('.', "/"));
        b.extend_from_slice(&c.to_be_bytes());
        b.extend_from_slice(&(with.len() as u16).to_be_bytes());
        for w in with.iter() {
            let wc = pool.class(&w.replace('.', "/"));
            b.extend_from_slice(&wc.to_be_bytes());
        }
    }
    b
}

/// Emite `class` y, **recursivamente**, sus tipos anidados (`Member::Type`) — cada uno como su propio
/// `.class`. El nombre cualificado se arma con `.` (así lo indexa la tabla); el *binary name* con `$`
/// lo pone la propia clase.
fn gen_type(
    class: &super::ast::ClassDecl,
    enclosing: &str,
    table: &SymbolTable,
    rt: &std::collections::HashSet<String>,
    tu: &TypeUseInfo,
    src: &str,
    out: &mut Vec<(String, Vec<u8>)>,
    errors: &mut Vec<Error>,
) {
    let fqn = if enclosing.is_empty() { class.name.clone() } else { format!("{enclosing}.{}", class.name) };
    if let Some(cid) = table.class(&fqn) {
        let bytes = gen_class(class, cid, table, rt, tu, src, errors);
        out.push((internal_name(table, cid), bytes));
    }
    for member in &class.members {
        if let Member::Type(nested) = member {
            gen_type(nested, &fqn, table, rt, tu, src, out, errors);
        }
    }
}

/// El nombre del archivo de la unidad para el atributo `SourceFile` (4.7.10).
///
/// El compilador no recibe la **ruta** de la fuente, asi que el nombre se deduce del tipo que le da
/// nombre al archivo: el **publico** de nivel superior si lo hay -que segun 7.6 obliga a que el
/// archivo se llame como el-, y si no el primero declarado. Es exacto para toda unidad que respete
/// esa regla, que es toda la que compile un `javac` con el chequeo de nombres puesto.
///
/// Antes cada clase escribia **su propio** nombre, asi que una secundaria decia "Secundaria.java" y
/// una anidada "Kind.java" -archivos que no existen-. Un depurador que quiera abrir la fuente por
/// ese nombre no la encuentra.
fn unit_source_file(unit: &super::ast::CompilationUnit) -> String {
    let publica = unit.types.iter().find(|c| c.modifiers.contains(&Modifier::Public));
    match publica.or_else(|| unit.types.first()) {
        Some(c) => format!("{}.java", c.name),
        None => "unknown.java".to_string(),
    }
}

/// Compila **una** clase a los bytes de su `.class`.
fn gen_class(
    class: &super::ast::ClassDecl,
    cid: SymbolId,
    table: &SymbolTable,
    rt: &std::collections::HashSet<String>,
    tu: &TypeUseInfo,
    src: &str,
    errors: &mut Vec<Error>,
) -> Vec<u8> {
    let scope = member_scope(table, cid);
    audit_declared_types(table, scope, class, errors);

    let mut cf = ClassFile::new();
    let this_internal = internal_name(table, cid);
    cf.this_class = cf.pool.class(&this_internal);
    let super_internal = super_internal(table, cid, class, scope);
    // `java.lang.Object` es la **única** clase sin superclase (JVMS §4.1): su `super_class` es `0`.
    // Cualquier otra —incluida una que por defecto extiende Object— lleva el índice `Class`. Sin este
    // caso, Object salía con `super_class` apuntándose a sí mismo y el intérprete entraba en bucle al
    // armar la vtable (Object → Object → …).
    cf.super_class =
        if this_internal == "java/lang/Object" { 0 } else { cf.pool.class(&super_internal) };
    // Una **interfaz** (o `@interface`) es `ACC_INTERFACE | ACC_ABSTRACT`, **sin** `ACC_SUPER` (§4.1);
    // una clase lleva `ACC_SUPER`.
    let is_interface = matches!(class.kind, TypeKind::Interface | TypeKind::Annotation);
    // `ACC_STATIC`/`ACC_PRIVATE`/`ACC_PROTECTED` de un tipo **anidado** son flags **de miembro**: van
    // en su entrada de `InnerClasses` (§4.7.6), **no** en los access_flags de clase (§4.1), que solo
    // admiten public/final/super/interface/abstract/…—. Emitirlos ahí da un `.class` que la JVM
    // rechaza (`Unmatched bit 0x8` para `static`). El nivel de acceso real de un anidado lo lleva
    // `InnerClasses`; a nivel clase, un anidado package-private queda solo con `ACC_SUPER`.
    let mut class_level = class_flags(&class.modifiers) & !(ACC_STATIC | ACC_PRIVATE | ACC_PROTECTED);
    // §9.5 — un tipo **miembro de una interfaz** es implícitamente `public` (y `static`; el `static`
    // va en la entrada de `InnerClasses`, no acá).
    if is_interface_member(table, cid) {
        class_level |= ACC_PUBLIC;
    }
    cf.access_flags = if is_interface {
        class_level | ACC_INTERFACE | ACC_ABSTRACT
    } else {
        class_level | ACC_SUPER
    };
    // Un `record` es implícitamente `final` (§8.10) — lo que la reflexión exige para `isRecord()`.
    if class.kind == TypeKind::Record {
        cf.access_flags |= ACC_FINAL;
    }
    // Un `@interface` (§9.6) es una **interfaz de anotación**, y eso son **dos** cosas que la spec
    // pone y el fuente no escribe: el flag `ACC_ANNOTATION` (§4.1) y el `extends
    // java.lang.annotation.Annotation` implícito (§9.6). Sin el flag, `Class.isAnnotation()` niega
    // que lo sea; sin la superinterfaz, una anotación no es asignable a `Annotation`, que es el
    // tipo por el que la reflexión las devuelve — o sea que ninguna de las dos mitades de la
    // reflexión de anotaciones podía funcionar (#276).
    //
    // La superinterfaz se agrega **acá** y no en la lista de `implements` de más abajo a propósito:
    // no está escrita en el fuente, así que agregarla al AST la haría aparecer en el `Signature` y
    // en los chequeos de override como si el programador la hubiera puesto.
    if class.kind == TypeKind::Annotation {
        cf.access_flags |= ACC_ANNOTATION;
    }
    // Un `enum` (§8.9) lleva `ACC_ENUM`, y es implícitamente `final` salvo que declare un método
    // `abstract` (que obligaría a cuerpos de constante). Real javac: enum simple = `FINAL|SUPER|ENUM`.
    // Sin esto, la reflexión no lo ve como enum (`Class.isEnum()`) y falta el `final`.
    if class.kind == TypeKind::Enum {
        cf.access_flags |= ACC_ENUM;
        let has_abstract_method = class.members.iter().any(|m| {
            matches!(m, Member::Method(me) if me.modifiers.contains(&Modifier::Abstract))
        });
        cf.access_flags |= if has_abstract_method { ACC_ABSTRACT } else { ACC_FINAL };
    }
    cf.source_file = Some(cf.pool.utf8(src));
    cf.annotations = build_annotations(&mut cf.pool, table, scope, &class.annotations, rt, tu);
    // `RuntimeVisibleTypeAnnotations` (§4.7.20) de la clase, juntando: parámetros de tipo
    // (`class C<@Foo T>`, target 0x00), sus **cotas** (`<T extends @A A>`, 0x11), el `extends`
    // (`extends @A Base`, 0x10 con `supertype_index = 0xFFFF`) y cada interfaz de `implements`
    // (`implements @A I`, 0x10 con el índice de la interfaz). Cada uno con su `type_path`.
    let mut class_ta = type_param_entries(&mut cf.pool, table, scope, &class.type_params, false, rt);
    class_ta.extend(type_use_bound_entries(&mut cf.pool, table, scope, &class.type_params, false, rt));
    class_ta.extend(type_use_nested_entries(
        &mut cf.pool, table, scope, &class.extends_annos, 0x10, &0xFFFFu16.to_be_bytes(), rt,
    ));
    for (i, iface_annos) in class.implements_annos.iter().enumerate() {
        class_ta.extend(type_use_nested_entries(
            &mut cf.pool, table, scope, iface_annos, 0x10, &(i as u16).to_be_bytes(), rt,
        ));
    }
    cf.type_annotations = wrap_type_annotations(&class_ta);
    // `Signature` (§4.7.9) de la clase: sus parámetros de tipo + super/interfaces genéricos.
    cf.signature = class_signature(table, scope, class, &this_internal).map(|s| cf.pool.utf8(&s));
    // `InnerClasses` (§4.7.6): la cadena de enclosing de esta clase + las que contiene.
    cf.inner_classes = build_inner_classes(&mut cf.pool, table, cid);
    // `EnclosingMethod` (§4.7.7): solo si es local/anónima.
    cf.enclosing_method = enclosing_method_attr(&mut cf.pool, table, cid);
    // `Record` (§4.7.30): los componentes de un `record` (aunque sea de cero componentes).
    if class.kind == TypeKind::Record {
        cf.record_components =
            Some(build_record_components(&mut cf.pool, table, scope, &class.components));
    }
    // `NestHost`/`NestMembers` (§4.7.28/§4.7.29): el *nest* que da acceso privado entre anidadas.
    match nest_host_of(table, cid) {
        Some(host) => {
            let hb = internal_name(table, host);
            cf.nest_host = Some(cf.pool.class(&hb));
        }
        None => {
            // Es top-level: hostea a todas sus anidadas (transitivas).
            cf.nest_members = nest_members_of(table, cid)
                .into_iter()
                .map(|m| {
                    let mb = internal_name(table, m);
                    cf.pool.class(&mb)
                })
                .collect();
        }
    }
    // Super-interfaces: el `implements` de una clase, y el `extends` de una interfaz —el parser lo
    // guarda también en `implements`—.
    for imp in &class.implements {
        if let Type::Class(n) | Type::Parameterized { base: n, .. } = imp {
            if let Some(id) = resolve_type_id(table, scope, n) {
                let idx = cf.pool.class(&internal_name(table, id));
                cf.interfaces.push(idx);
            }
        }
    }
    // El `extends java.lang.annotation.Annotation` implícito de un `@interface` (§9.6). Va después
    // de las escritas para que el orden de las escritas no cambie.
    if class.kind == TypeKind::Annotation && this_internal != "java/lang/annotation/Annotation" {
        let idx = cf.pool.class("java/lang/annotation/Annotation");
        if !cf.interfaces.contains(&idx) {
            cf.interfaces.push(idx);
        }
    }
    // `PermittedSubclasses` (§4.7.31): un tipo `sealed` graba en el `.class` sus subtipos
    // autorizados (el `permits` explícito o el implícito de la misma unidad). Sin esto, el tipo no
    // quedaría realmente sellado para la JVM.
    if table.is_sealed(cid) {
        for perm in table.permitted(cid) {
            if let Some(id) = super::types::erased_id(perm) {
                let idx = cf.pool.class(&internal_name(table, id));
                cf.permitted_subclasses.push(idx);
            }
        }
    }

    // Los *bootstrap methods* de todos los `invokedynamic` de la clase: se acumulan a medida que se
    // emite cada método (el índice que referencia el pool es su posición aquí) y se vuelcan al final.
    let mut bootstraps: Vec<BootstrapMethod> = Vec::new();

    let mut has_ctor = false;
    for member in &class.members {
        match member {
            Member::Method(m) => {
                if m.is_constructor {
                    has_ctor = true;
                }
                let mut mi = gen_method(
                    &mut cf.pool,
                    table,
                    scope,
                    m,
                    &this_internal,
                    &super_internal,
                    is_interface,
                    class.kind == TypeKind::Enum,
                    rt,
                    tu,
                    &mut bootstraps,
                    errors,
                );
                // `AnnotationDefault` (§4.7.22): si `m` es un elemento de `@interface` con un `default`,
                // su valor va como `element_value` en el atributo del método.
                if let Some((_, value)) =
                    class.annotation_defaults.iter().find(|(n, _)| *n == m.name)
                {
                    mi.annotation_default = encode_value(&mut cf.pool, table, scope, value);
                }
                cf.methods.push(mi);
            }
            // Los campos **declarados**: sin esta sección el `.class` referencia un `getfield` a un
            // campo que no existe, y la carga falla al buscar su offset.
            Member::Field(f) => {
                let name_index = cf.pool.utf8(&f.name);
                let descriptor_index = cf.pool.utf8(&type_desc(table, scope, &f.ty));
                let annotations = build_annotations(&mut cf.pool, table, scope, &f.annotations, rt, tu);
                let signature = field_signature(table, scope, &f.ty).map(|s| cf.pool.utf8(&s));
                // `RuntimeVisibleTypeAnnotations` del campo (target `0x13`): las anotaciones **líder**
                // que son `@Target(TYPE_USE)` sobre el tipo del campo (`@NonNull String x`), path vacío.
                let mut field_ta =
                    type_use_lead_entries(&mut cf.pool, table, scope, &f.annotations, 0x13, &[], rt, tu);
                field_ta.extend(type_use_nested_entries(
                    &mut cf.pool, table, scope, &f.type_annos, 0x13, &[], rt,
                ));
                let type_annotations = wrap_type_annotations(&field_ta);
                // `ConstantValue` (§4.7.2): un `static final` con inicializador de expresión constante.
                // El desugar lo dejó sin bajar al `<clinit>` (dejó `f.init` en su lugar), justamente
                // para que se emita acá; los que no son constantes tienen `f.init == None`.
                // **Finding #238**: acá se miraban los modificadores **declarados** (`f.modifiers`),
                // y en una interfaz esos vienen vacíos — `long NOPOS = -1L;` no escribe
                // `public static final` porque JLS §9.3 los da por implícitos. Resultado: el campo
                // salía con `flags: (0x0000)` (ni público, ni estático, ni final) y **sin**
                // `ConstantValue`, o sea un class file que el `javap` real marca como inválido.
                //
                // Se usa la **misma** función que `enter` aplica al símbolo, no una copia: si las dos
                // divergieran, el campo se emitiría con unos flags y se resolvería con otros — que es
                // exactamente la clase de desajuste que produjo #110 y #112.
                let field_mods = super::enter::implicit_field_mods(class.kind, &f.modifiers);
                let is_const_field =
                    field_mods.contains(&Modifier::Static) && field_mods.contains(&Modifier::Final);
                let constant_value = f
                    .init
                    .as_ref()
                    .filter(|_| is_const_field)
                    .and_then(|init| const_field_value(&f.ty, init, table.const_fields()))
                    .map(|v| match v {
                        ConstVal::Int(n) => cf.pool.integer(n),
                        ConstVal::Long(n) => cf.pool.long(n),
                        ConstVal::Float(n) => cf.pool.float(n),
                        ConstVal::Double(n) => cf.pool.double(n),
                        ConstVal::Str(s) => cf.pool.string(&s),
                    });
                // Flags del campo, con los extras de `enum` (§4.5): una **constante** lleva `ACC_ENUM`
                // (lo que la reflexión usa para `Field.isEnumConstant()`), y el arreglo sintético
                // `$VALUES` lleva `ACC_SYNTHETIC`. Real javac: constante = `0x4019`, `$VALUES` = `0x101a`.
                let mut field_flags = self::field_flags(&field_mods);
                if class.kind == TypeKind::Enum {
                    if class.enum_constants.iter().any(|c| c.name == f.name) {
                        field_flags |= ACC_ENUM;
                    } else if f.name == "$VALUES" {
                        field_flags |= ACC_SYNTHETIC;
                    }
                }
                cf.fields.push(FieldInfo {
                    access_flags: field_flags,
                    name_index,
                    descriptor_index,
                    annotations,
                    signature,
                    constant_value,
                    type_annotations,
                });
            }
            _ => {}
        }
    }
    // El **`<clinit>`**: los bloques de inicialización estática, concatenados en orden de fuente
    // (§12.4.2). El desugar ya movió ahí los inicializadores de los campos `static`, así que este es
    // el único lugar donde un `static int s = 3;` se convierte en bytecode. Sin esto, los campos que
    // el propio desugar sintetiza —el `$VALUES` de un `enum`, el `$SwitchMap` de su `switch`, el
    // guard del `assert`— quedaban **declarados y en cero**.
    let statics: Vec<Stmt> = class
        .members
        .iter()
        .filter_map(|m| match m {
            Member::StaticInit(b) => Some(b.0.clone()),
            _ => None,
        })
        .flatten()
        .collect();
    if !statics.is_empty() {
        let clinit = MethodDecl {
            doc: None,
            annotations: Vec::new(),
            return_annos: Vec::new(),
            pos: Pos::default(),
            modifiers: vec![Modifier::Static],
            type_params: Vec::new(),
            return_type: Type::Void,
            name: "<clinit>".to_string(),
            params: Vec::new(),
            throws: Vec::new(),
            throws_annos: Vec::new(),
            body: Some(Block(statics)),
            is_constructor: false,
        };
        let mi = gen_method(
            &mut cf.pool,
            table,
            scope,
            &clinit,
            &this_internal,
            &super_internal,
            false,
            class.kind == TypeKind::Enum,
            rt,
            tu,
            &mut bootstraps,
            errors,
        );
        cf.methods.push(mi);
    }

    // Sin constructor explícito, se sintetiza el por defecto: `super()` + `return`. Una **interfaz**
    // no tiene constructor.
    if !has_ctor && !is_interface {
        // El ctor por defecto toma el **acceso de la clase** (§8.8.9): `public class` → ctor `public`,
        // un anidado package-private → ctor package-private (antes salía siempre `public`).
        let access = class_flags(&class.modifiers) & (ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE);
        let ctor = default_ctor(&mut cf.pool, &super_internal, access);
        cf.methods.push(ctor);
    }

    // **Métodos puente** (§8.4.8.3 / §15.12.4.5): sintéticos, con el descriptor **borrado del
    // supertipo**, que reenvían al override real. Sin ellos la sobrescritura no funciona a nivel de
    // bytecode cuando la *erasure* difiere (parámetro genérico o retorno covariante).
    for br in bridge_methods(&mut cf.pool, table, class, cid, &this_internal) {
        cf.methods.push(br);
    }

    // #268: accesores sintéticos para los métodos públicos heredados de una superclase
    // package-private (que un caller externo no podría invocar por no poder nombrar la super).
    for fw in accessor_forwarders(&mut cf.pool, table, class, cid) {
        cf.methods.push(fw);
    }

    cf.bootstrap_methods = bootstraps;
    cf.to_bytes()
}

/// La firma **resuelta** (sin borrar) de un método: `(params, retorno)`.
fn method_sig(table: &SymbolTable, m: SymbolId) -> Option<(Vec<RType>, RType)> {
    match table.resolved(m) {
        Some(Resolved::Method { params, ret, .. }) => Some((params.clone(), ret.clone())),
        _ => None,
    }
}

/// El descriptor `(p…)ret` de una firma ya en [`RType`] (se le aplica su *erasure*).
fn sig_desc(table: &SymbolTable, params: &[RType], ret: &RType) -> String {
    let ps: String = params.iter().map(|p| rtype_desc(table, p)).collect();
    format!("({ps}){}", rtype_desc(table, ret))
}

/// El *offset* dentro de una familia de opcodes (`ILOAD`/`IRETURN`, consecutivas i/l/f/d/a).
fn cat_offset(rt: &RType) -> u8 {
    match rt {
        RType::Prim(PrimType::Long) => 1,
        RType::Prim(PrimType::Float) => 2,
        RType::Prim(PrimType::Double) => 3,
        RType::Prim(_) => 0,
        _ => 4, // referencia (clase/array/var/captura)
    }
}

/// El ancho en slots/categoría de un tipo: 2 para `long`/`double`, 1 para el resto.
fn cat_width(rt: &RType) -> u16 {
    matches!(rt, RType::Prim(PrimType::Long | PrimType::Double)) as u16 + 1
}

/// El **menor slot** que declara algún local del bloque (recorriendo lo anidado): la *base* de los
/// locales de un `finally`. javac reubica esos locales **por encima** del temporal del `return` (o de
/// la excepción aparcada del catch-all) en cada copia inline; para lograr el mismo byte-exacto se
/// desplaza cada slot `>= base` por un delta al emitir la copia. `None` si el bloque no declara
/// ningún local (no hace falta reubicar nada).
fn finally_local_base(stmts: &[Stmt]) -> Option<u16> {
    let mut out: Vec<u16> = Vec::new();
    collect_local_slots(stmts, &mut out);
    out.into_iter().min()
}

fn collect_local_slots(stmts: &[Stmt], out: &mut Vec<u16>) {
    for s in stmts {
        collect_stmt_slots(s, out);
    }
}

fn collect_stmt_slots(s: &Stmt, out: &mut Vec<u16>) {
    if let Some(l) = s.local.as_ref() {
        out.push(l.slot);
    }
    match &s.kind {
        StmtKind::If { then, els, .. } => {
            collect_stmt_slots(then, out);
            if let Some(e) = els {
                collect_stmt_slots(e, out);
            }
        }
        StmtKind::While { body, .. }
        | StmtKind::Do { body, .. }
        | StmtKind::ForEach { body, .. }
        | StmtKind::Labeled { body, .. } => collect_stmt_slots(body, out),
        StmtKind::For { init, body, .. } => {
            if let Some(i) = init {
                collect_stmt_slots(i, out);
            }
            collect_stmt_slots(body, out);
        }
        StmtKind::Block(b) => collect_local_slots(&b.0, out),
        StmtKind::Synchronized { body, .. } => collect_local_slots(&body.0, out),
        StmtKind::Try { resources, body, catches, finally } => {
            collect_local_slots(resources, out);
            collect_local_slots(&body.0, out);
            for c in catches {
                if let Some(sl) = c.slot {
                    out.push(sl);
                }
                collect_local_slots(&c.body.0, out);
            }
            if let Some(f) = finally {
                collect_local_slots(&f.0, out);
            }
        }
        StmtKind::Switch { cases, .. } => {
            for c in cases {
                match &c.body {
                    SwitchBody::Arrow(st) => collect_stmt_slots(st, out),
                    SwitchBody::Colon(sts) => collect_local_slots(sts, out),
                }
            }
        }
        _ => {}
    }
}

/// Parte `[start, end)` excluyendo los `gaps` (las copias inline del `finally` que corren en las
/// salidas abruptas): javac deja **fuera** de la región protegida cada copia inyectada —una excepción
/// dentro de ella ya no pertenece al `try`, sino que la toma el catch-all de más afuera—. Devuelve los
/// sub-rangos resultantes (ninguno vacío), en orden ascendente.
fn split_range(start: usize, end: usize, gaps: &[(usize, usize)]) -> Vec<(usize, usize)> {
    let mut sorted: Vec<(usize, usize)> =
        gaps.iter().filter(|(s, e)| e > s).cloned().collect();
    sorted.sort();
    let mut out = Vec::new();
    let mut cur = start;
    for (gs, ge) in sorted {
        let gs = gs.max(start);
        let ge = ge.min(end);
        if gs >= end {
            break;
        }
        if gs > cur {
            out.push((cur, gs));
        }
        cur = cur.max(ge);
    }
    if cur < end {
        out.push((cur, end));
    }
    out
}

/// Anidamiento máximo de `synchronized` de un cuerpo, para reservar los slots de sus monitores (uno
/// por nivel) y los aparcaderos de excepción de sus handlers (otro por nivel) por debajo de los
/// temporales dinámicos del método.
fn max_sync_depth(stmts: &[Stmt]) -> u16 {
    stmts.iter().map(stmt_sync_depth).max().unwrap_or(0)
}

fn stmt_sync_depth(s: &Stmt) -> u16 {
    match &s.kind {
        StmtKind::Synchronized { body, .. } => 1 + max_sync_depth(&body.0),
        StmtKind::If { then, els, .. } => {
            stmt_sync_depth(then).max(els.as_ref().map_or(0, |e| stmt_sync_depth(e)))
        }
        StmtKind::While { body, .. }
        | StmtKind::Do { body, .. }
        | StmtKind::ForEach { body, .. }
        | StmtKind::For { body, .. }
        | StmtKind::Labeled { body, .. } => stmt_sync_depth(body),
        StmtKind::Block(b) => max_sync_depth(&b.0),
        StmtKind::Try { resources, body, catches, finally } => {
            let mut m = max_sync_depth(resources).max(max_sync_depth(&body.0));
            for c in catches {
                m = m.max(max_sync_depth(&c.body.0));
            }
            if let Some(f) = finally {
                m = m.max(max_sync_depth(&f.0));
            }
            m
        }
        StmtKind::Switch { cases, .. } => {
            let mut m = 0;
            for c in cases {
                m = m.max(match &c.body {
                    SwitchBody::Arrow(st) => stmt_sync_depth(st),
                    SwitchBody::Colon(sts) => max_sync_depth(sts),
                });
            }
            m
        }
        _ => 0,
    }
}

/// Un tipo de **referencia** (JVMS 4.3.2): clase, interfaz o array. Lo unico sobre lo que un
/// `checkcast` tiene sentido.
fn is_ref(rt: &RType) -> bool {
    matches!(rt, RType::Class(_) | RType::Parameterized { .. } | RType::Array(_))
}

/// El nombre para un `CHECKCAST` sobre `rt`: el interno de la clase, o el descriptor de un array.
fn checkcast_name(table: &SymbolTable, rt: &RType) -> String {
    match rt {
        RType::Array(_) => rtype_desc(table, rt), // `[Ljava/lang/String;`
        _ => match types::erased_id(rt) {
            Some(id) => internal_name(table, id),
            None => "java/lang/Object".to_string(),
        },
    }
}

/// Los **métodos puente** que `cid` necesita (§8.4.8.3 / §15.12.4.5). Por cada método propio
/// concreto de instancia que **sobrescribe** uno de un supertipo cuya *erasure* difiere —por un
/// parámetro **genérico** borrado (`Node<Integer>.setData(T)` → `setData(Object)`), o por un
/// **retorno covariante** (`A.f():Object` → `B.f():String`)—, se sintetiza un método con el
/// descriptor **borrado del supertipo** que reenvía al real. Vale para clases y para **interfaces**:
/// en una interfaz el puente es un método `default` sintético que reenvía con `invokeinterface` al
/// `default` real (§9.4.1.3). Un `@interface` no tiene `default`s ni supertipos genéricos: sin puentes.
fn bridge_methods(
    pool: &mut ConstantPool,
    table: &SymbolTable,
    class: &ClassDecl,
    cid: SymbolId,
    this_internal: &str,
) -> Vec<MethodInfo> {
    if matches!(class.kind, TypeKind::Annotation) {
        return Vec::new();
    }
    let is_interface = matches!(class.kind, TypeKind::Interface);
    let supers = types::supertypes_of(table, &RType::Class(cid));
    // Descriptores **ya presentes** como métodos reales: un puente no debe pisar ninguno.
    let mut present: std::collections::HashSet<(String, String)> = std::collections::HashSet::new();
    for id in table.members_of(cid) {
        if let Some((ps, ret)) = method_sig(table, id) {
            let ep: Vec<RType> = ps.iter().map(|p| types::erasure(table, p)).collect();
            present.insert((table.symbol(id).name.clone(), sig_desc(table, &ep, &types::erasure(table, &ret))));
        }
    }
    let mut out = Vec::new();
    let mut emitted: std::collections::HashSet<(String, String)> = std::collections::HashSet::new();
    for m in table.members_of(cid) {
        let sym = table.symbol(m);
        if !matches!(sym.kind, SymbolKind::Method { is_constructor: false, .. }) {
            continue;
        }
        // Un `static` no entra en la vtable, asi que no hay nada que puentear. Un **abstracto** si:
        // el puente lo necesita el *llamador* que ve el supertipo, no la implementacion. javac lo
        // emite igual —concreto, con cuerpo `aload_0; invokevirtual <el angosto>; areturn`— y el
        // despacho virtual lo lleva al override real de la subclase concreta. Saltearlos costaba
        // ~25 miembros de `java.nio` (`Buffer slice()`, `duplicate()`, …) (#233).
        if sym.modifiers.contains(&Modifier::Static) {
            continue;
        }
        let name = sym.name.clone();
        let Some((m_raw_params, m_raw_ret)) = method_sig(table, m) else { continue };
        let m_params: Vec<RType> = m_raw_params.iter().map(|p| types::erasure(table, p)).collect();
        let m_ret = types::erasure(table, &m_raw_ret);
        let m_desc = sig_desc(table, &m_params, &m_ret);

        for sup in &supers {
            let Some(sup_id) = types::erased_id(sup) else { continue };
            if sup_id == cid {
                continue;
            }
            let subst = types::subst_of(table, sup);
            for sm in table.members_of(sup_id) {
                if table.symbol(sm).name != name {
                    continue;
                }
                let Some((sm_params, sm_ret)) = method_sig(table, sm) else { continue };
                if sm_params.len() != m_params.len() {
                    continue;
                }
                // ¿`m` sobrescribe a `sm`? Los params de `sm` **sustituidos** por el supertipo
                // (`T := Integer`) y **borrados** tienen que coincidir con los de `m` (§8.4.2).
                let sm_over: Vec<RType> =
                    sm_params.iter().map(|p| types::erasure(table, &types::substitute(p, &subst))).collect();
                if sm_over != m_params {
                    continue;
                }
                // La firma del puente: la *erasure* de `sm` **sin** sustituir (lo que ve el llamador
                // por el supertipo).
                let br_params: Vec<RType> = sm_params.iter().map(|p| types::erasure(table, p)).collect();
                let br_ret = types::erasure(table, &sm_ret);
                let br_desc = sig_desc(table, &br_params, &br_ret);
                if br_desc == m_desc {
                    continue; // misma erasure: no hace falta puente
                }
                let key = (name.clone(), br_desc.clone());
                if present.contains(&key) || !emitted.insert(key) {
                    continue;
                }
                out.push(emit_bridge(
                    pool, table, this_internal, &name, &br_params, &br_ret, &m_params, &m_ret,
                    &m_desc, is_interface,
                ));
            }
        }
    }
    out
}

/// Emite el método puente: `this` + los argumentos (con `checkcast` al tipo del método real cuando
/// difieren) + la llamada al real + `return` del valor (su subtipo es asignable al retorno del
/// puente). En una **interfaz** el reenvío es `invokeinterface` a un `InterfaceMethodref` (§6.5); en
/// una clase, `invokevirtual` a un `Methodref`. Sin saltos: no lleva `StackMapTable`.
#[allow(clippy::too_many_arguments)]
fn emit_bridge(
    pool: &mut ConstantPool,
    table: &SymbolTable,
    this_internal: &str,
    name: &str,
    br_params: &[RType],
    br_ret: &RType,
    m_params: &[RType],
    m_ret: &RType,
    m_desc: &str,
    is_interface: bool,
) -> MethodInfo {
    let name_index = pool.utf8(name);
    let descriptor_index = pool.utf8(&sig_desc(table, br_params, br_ret));

    let mut code = vec![ALOAD_0];
    let mut slot = 1u16;
    let mut on_stack = 1u16; // this
    for (i, bp) in br_params.iter().enumerate() {
        code.push(ILOAD + cat_offset(bp));
        code.push(slot as u8);
        slot += cat_width(bp);
        on_stack += cat_width(bp);
        // El método real toma un tipo más **angosto** (`Integer` vs `Object`): castear.
        let mp = &m_params[i];
        if mp != bp && cat_offset(mp) == 4 {
            let cc = pool.class(&checkcast_name(table, mp));
            code.push(CHECKCAST);
            code.push((cc >> 8) as u8);
            code.push(cc as u8);
        }
    }
    let max_stack = on_stack.max(cat_width(m_ret));
    if is_interface {
        // `invokeinterface`: índice + `count` (slots del receptor + argumentos) + un byte cero (§6.5).
        let imref = pool.interface_methodref(this_internal, name, m_desc);
        code.push(INVOKEINTERFACE);
        code.push((imref >> 8) as u8);
        code.push(imref as u8);
        let count: u16 = m_params.iter().map(|p| cat_width(p)).sum::<u16>() + 1;
        code.push(count as u8);
        code.push(0);
    } else {
        let target = pool.methodref(this_internal, name, m_desc);
        code.push(INVOKEVIRTUAL);
        code.push((target >> 8) as u8);
        code.push(target as u8);
    }
    if matches!(m_ret, RType::Void) {
        code.push(RETURN);
    } else {
        code.push(IRETURN + cat_offset(m_ret));
    }

    MethodInfo {
        access_flags: ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC,
        name_index,
        descriptor_index,
        max_stack,
        max_locals: slot,
        code,
        stack_map: None,
        exceptions: Vec::new(),
        annotations: None,
        signature: None, // un puente es la firma **borrada**: sin `Signature`
        parameters: Vec::new(),
        thrown_exceptions: Vec::new(), // un puente no declara `throws`
        line_numbers: Vec::new(),      // sintético: sin líneas de fuente
        local_vars: Vec::new(),
        type_annotations: None,
        code_type_annotations: None,
        parameter_annotations: None,
        annotation_default: None,
    }
}

/// Los **accesores sintéticos** que exige la forma "clase pública sobre superclase package-private"
/// (finding #268). Cuando una clase **pública** `C` hereda un método **público** de una superclase
/// **package-private** `P` y no lo sobrescribe, un llamador de otro paquete no puede **nombrar** `P`,
/// así que la llamada resuelta fallaría el chequeo de acceso (JVMS 5.4.4) → `IllegalAccessError`.
/// `javac` lo evita sintetizando en `C` un **forwarder** (`ACC_PUBLIC|ACC_BRIDGE|ACC_SYNTHETIC`) que
/// reenvía al método real con `invokespecial P.m` (mismo descriptor, sin covarianza).
///
/// Es la familia hermana de [`bridge_methods`]: aquellos cubren los métodos que `C` **sí** sobrescribe
/// (retorno covariante / genérico borrado); éstos, los que `C` **hereda tal cual**. Por eso se saltea
/// todo `(nombre, params-borrados)` que `C` declara: de ése ya se ocupa el puente. Sólo aplica a una
/// clase pública con al menos una superclase package-private (el caso `StringBuilder`/`StringBuffer`
/// sobre `AbstractStringBuilder`); para cualquier otra clase, el bucle no encuentra nada y es no-op.
fn accessor_forwarders(
    pool: &mut ConstantPool,
    table: &SymbolTable,
    class: &ClassDecl,
    cid: SymbolId,
) -> Vec<MethodInfo> {
    // El problema sólo existe si `C` es **pública** (una clase no-pública ya comparte la
    // inaccesibilidad de su super, así que no hay nada que exponer).
    if !class.modifiers.contains(&Modifier::Public) {
        return Vec::new();
    }
    // Lo que `C` declara, por `(nombre, params borrados)`: si está acá, `C` lo sobrescribe y su
    // puente (o el método mismo) ya lo cubre — no va forwarder.
    let mut declared: std::collections::HashSet<(String, String)> = std::collections::HashSet::new();
    for id in table.members_of(cid) {
        if let Some((ps, _)) = method_sig(table, id) {
            let pdesc: String =
                ps.iter().map(|p| rtype_desc(table, &types::erasure(table, p))).collect();
            declared.insert((table.symbol(id).name.clone(), pdesc));
        }
    }
    let mut out = Vec::new();
    let mut emitted: std::collections::HashSet<(String, String)> = std::collections::HashSet::new();
    for sup in types::supertypes_of(table, &RType::Class(cid)) {
        let Some(pid) = types::erased_id(&sup) else { continue };
        if pid == cid {
            continue;
        }
        let ps = table.symbol(pid);
        // Sólo una **clase** (no interfaz/anotación) **package-private** dispara el problema.
        let is_pkg_priv_class = matches!(&ps.kind, SymbolKind::Class { kind: TypeKind::Class, .. })
            && !ps.modifiers.contains(&Modifier::Public);
        if !is_pkg_priv_class {
            continue;
        }
        let p_internal = internal_name(table, pid);
        for sm in table.members_of(pid) {
            let m = table.symbol(sm);
            if !matches!(m.kind, SymbolKind::Method { is_constructor: false, .. }) {
                continue;
            }
            // Público, de instancia y con cuerpo: un `static` no está en la vtable y un `abstract` no
            // tiene a qué reenviar.
            if !m.modifiers.contains(&Modifier::Public)
                || m.modifiers.contains(&Modifier::Static)
                || m.modifiers.contains(&Modifier::Abstract)
            {
                continue;
            }
            let name = m.name.clone();
            let Some((params, ret)) = method_sig(table, sm) else { continue };
            let eparams: Vec<RType> = params.iter().map(|p| types::erasure(table, p)).collect();
            let eret = types::erasure(table, &ret);
            let pdesc: String = eparams.iter().map(|p| rtype_desc(table, p)).collect();
            if declared.contains(&(name.clone(), pdesc.clone())) {
                continue; // `C` lo sobrescribe → lo cubre el puente (#233), no un forwarder
            }
            let desc = sig_desc(table, &eparams, &eret);
            if !emitted.insert((name.clone(), desc.clone())) {
                continue; // ya reenviado desde una super más cercana
            }
            out.push(emit_forwarder(pool, table, &p_internal, &name, &eparams, &eret, &desc));
        }
    }
    out
}

/// Emite un forwarder de #268: `this` + argumentos + `invokespecial P.m` (mismo descriptor) + el
/// `return` correspondiente. Sin covarianza ⇒ sin `checkcast`; sin saltos ⇒ sin `StackMapTable`.
fn emit_forwarder(
    pool: &mut ConstantPool,
    _table: &SymbolTable,
    super_internal: &str,
    name: &str,
    params: &[RType],
    ret: &RType,
    desc: &str,
) -> MethodInfo {
    let name_index = pool.utf8(name);
    let descriptor_index = pool.utf8(desc);

    let mut code = vec![ALOAD_0];
    let mut slot = 1u16;
    let mut on_stack = 1u16;
    for p in params {
        code.push(ILOAD + cat_offset(p));
        code.push(slot as u8);
        slot += cat_width(p);
        on_stack += cat_width(p);
    }
    let max_stack = on_stack.max(cat_width(ret));
    let target = pool.methodref(super_internal, name, desc);
    code.push(INVOKESPECIAL);
    code.push((target >> 8) as u8);
    code.push(target as u8);
    if matches!(ret, RType::Void) {
        code.push(RETURN);
    } else {
        code.push(IRETURN + cat_offset(ret));
    }

    MethodInfo {
        access_flags: ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC,
        name_index,
        descriptor_index,
        max_stack,
        max_locals: slot,
        code,
        stack_map: None,
        exceptions: Vec::new(),
        annotations: None,
        signature: None,
        parameters: Vec::new(),
        thrown_exceptions: Vec::new(),
        line_numbers: Vec::new(),
        local_vars: Vec::new(),
        type_annotations: None,
        code_type_annotations: None,
        parameter_annotations: None,
        annotation_default: None,
    }
}

fn member_scope(table: &SymbolTable, cid: SymbolId) -> ScopeId {
    match &table.symbol(cid).kind {
        SymbolKind::Class { members, .. } => *members,
        _ => table.global,
    }
}

/// El **nombre interno** de una clase (con `/`), a partir de su *binary name*.
pub(crate) fn internal_name(table: &SymbolTable, class_id: SymbolId) -> String {
    let bin = match &table.symbol(class_id).kind {
        SymbolKind::Class { binary, .. } => binary.clone(),
        _ => table.symbol(class_id).name.clone(),
    };
    bin.replace('.', "/")
}

/// El nombre interno de la **superclase**.
///
/// Se pregunta primero a la **tabla**, no al AST: hay supertipos que el fuente no escribe y `enter`
/// sí resolvió — un `enum` extiende `java.lang.Enum` sin decirlo (§8.9). Emitir `Object` ahí dejaría
/// un `.class` que llama a `Enum.<init>` sobre algo que no es un `Enum`, y el verificador lo
/// rechaza. El AST queda de respaldo.
fn super_internal(
    table: &SymbolTable,
    cid: SymbolId,
    class: &super::ast::ClassDecl,
    scope: ScopeId,
) -> String {
    if let Some(sup) = table.super_class(cid) {
        return internal_name(table, sup);
    }
    match &class.extends {
        Some(Type::Class(name)) | Some(Type::Parameterized { base: name, .. }) => {
            match resolve_type_id(table, scope, name) {
                Some(id) => internal_name(table, id),
                None => "java/lang/Object".to_string(),
            }
        }
        _ => "java/lang/Object".to_string(),
    }
}

fn resolve_type_id(table: &SymbolTable, scope: ScopeId, name: &str) -> Option<SymbolId> {
    if let Some(id) = table.resolve_type(scope, name).or_else(|| table.external(name)) {
        return Some(id);
    }
    // Cualificado / **anidado** (`Map.Entry`, `Diagnostic.Kind`, `Outer.Mid.Inner`): por nombre simple
    // (externo registrado así) o, si no, resolviendo el `outer` y bajando a su miembro-tipo. Sin esto,
    // el descriptor de un `Map.Entry<..>` en firma se emitía borrado a `Object` y el `Signature` con el
    // nombre crudo `Map/Entry` en vez del binario `java/util/Map$Entry`.
    if let Some((outer, simple)) = name.rsplit_once('.') {
        if let Some(id) = table.external(simple) {
            return Some(id);
        }
        if let Some(oid) = resolve_type_id(table, scope, outer) {
            return super::attribute::nested_type_in(table, oid, simple);
        }
    }
    None
}

/// Audita que **todo nombre de tipo escrito en las declaraciones** de la clase resuelva a un
/// símbolo, y reporta el que no. Sin esto el generador **fabrica** un artefacto plausible en vez de
/// fallar: el descriptor degrada a `Ljava/lang/Object;` y el `Signature` escribe el nombre tal como
/// se lo escribió (`LInner;`), que no es ninguna clase. Los dos caminos calculan lo mismo por
/// separado, así que ni siquiera coinciden entre sí — el descriptor miente por lo bajo y la firma
/// miente por lo alto (#208).
///
/// Que el nombre no resuelva **acá** no es un error del programa: el chequeo ya rechaza los tipos
/// que no existen. Es que la resolución del chequeo y la del generador no coincidieron, y eso es un
/// defecto del compilador que hay que ver, no tapar con un `Object`.
fn audit_declared_types(
    table: &SymbolTable,
    scope: ScopeId,
    class: &ClassDecl,
    errors: &mut Vec<Error>,
) {
    // Los parámetros de tipo **en alcance**: los de la clase más los de **cada** método. Que los de
    // un método se toleren en otro es a propósito: el alcance de un `<V>` ya lo chequea la fase
    // semántica (§8.4.4), y acá hace falta la vista ancha porque el **desazucarado** ya corrió — el
    // método sintético de una lambda (`lambda$andThen$0`) hereda en su firma el `V` del método que
    // la contiene, pero no su lista de parámetros de tipo. Sin la vista ancha, cada lambda dentro
    // de un método genérico se reportaría como tipo irresoluble.
    let mut tvars: HashSet<String> = class.type_params.iter().map(|p| p.name.clone()).collect();
    for member in &class.members {
        if let Member::Method(m) = member {
            tvars.extend(m.type_params.iter().map(|p| p.name.clone()));
        }
    }
    let report = |ty: &Type, pos: Pos, tvars: &HashSet<String>, errors: &mut Vec<Error>| {
        let mut malos = Vec::new();
        collect_unresolved(table, scope, tvars, ty, &mut malos);
        for name in malos {
            errors.push(Error::new(
                format!("el generador de bytecode no puede resolver el tipo `{name}`"),
                pos.line,
                pos.col,
            ));
        }
    };
    // La cabecera: cotas de los parámetros de tipo, `extends`, `implements`, `permits`. El `extends`
    // que no resuelve es el que hacía desaparecer la cláusula del class file en silencio.
    for tp in &class.type_params {
        for b in &tp.bounds {
            report(b, class.pos, &tvars, errors);
        }
    }
    for ty in class.extends.iter().chain(&class.implements).chain(&class.permits) {
        report(ty, class.pos, &tvars, errors);
    }
    for c in &class.components {
        report(&c.ty, class.pos, &tvars, errors);
    }
    for member in &class.members {
        match member {
            Member::Field(f) => report(&f.ty, f.pos, &tvars, errors),
            Member::Method(m) => {
                for tp in &m.type_params {
                    for b in &tp.bounds {
                        report(b, m.pos, &tvars, errors);
                    }
                }
                report(&m.return_type, m.pos, &tvars, errors);
                for prm in &m.params {
                    report(&prm.ty, m.pos, &tvars, errors);
                }
                for t in &m.throws {
                    report(t, m.pos, &tvars, errors);
                }
            }
            // Un tipo anidado se audita en **su** `gen_class`, con su propio alcance.
            _ => {}
        }
    }
}

/// Los nombres de [`Type`] que no resuelven, bajando por arrays y argumentos de tipo.
fn collect_unresolved(
    table: &SymbolTable,
    scope: ScopeId,
    tvars: &HashSet<String>,
    ty: &Type,
    out: &mut Vec<String>,
) {
    match ty {
        Type::Prim(_) | Type::Void | Type::Var => {}
        Type::Array(e) => collect_unresolved(table, scope, tvars, e, out),
        Type::Class(name) => {
            if !tvars.contains(name) && resolve_type_id(table, scope, name).is_none() {
                out.push(name.clone());
            }
        }
        Type::Parameterized { base, args } => {
            if !tvars.contains(base) && resolve_type_id(table, scope, base).is_none() {
                out.push(base.clone());
            }
            for a in args {
                match a {
                    TypeArg::Type(t) => collect_unresolved(table, scope, tvars, t, out),
                    TypeArg::Extends(t) | TypeArg::Super(t) => {
                        collect_unresolved(table, scope, tvars, t, out);
                    }
                    TypeArg::Wildcard => {}
                }
            }
        }
    }
}

/// Si el método declara un `T...`.
///
/// Solo el **último** parámetro puede serlo (JLS §8.4.1), así que basta mirar ese; el resto de la
/// firma no cambia y el descriptor tampoco (`T...` y `T[]` son el mismo `[LT;`). Lo que cambia es
/// que quien llame desde otra unidad puede desplegar los argumentos, y eso viaja solo en el flag.
fn is_varargs(m: &MethodDecl) -> bool {
    m.params.last().is_some_and(|p| p.varargs)
}

fn class_flags(mods: &[Modifier]) -> u16 {
    mods.iter().fold(0, |f, m| f | modifier_flag(*m))
}

/// Los flags de un **campo** (§4.5): los comunes, más `volatile` y `transient`, que **solo** existen
/// para campos. `strictfp` no se mapea a propósito: desde la v17 es implícito y el javac real
/// tampoco emite `ACC_STRICT` (avisa que el modificador sobra).
fn field_flags(mods: &[Modifier]) -> u16 {
    mods.iter().fold(0, |f, m| {
        f | match m {
            Modifier::Volatile => ACC_VOLATILE,
            Modifier::Transient => ACC_TRANSIENT,
            other => modifier_flag(*other),
        }
    })
}

fn modifier_flag(m: Modifier) -> u16 {
    match m {
        Modifier::Public => ACC_PUBLIC,
        Modifier::Private => ACC_PRIVATE,
        Modifier::Protected => ACC_PROTECTED,
        Modifier::Static => ACC_STATIC,
        Modifier::Final => ACC_FINAL,
        Modifier::Abstract => ACC_ABSTRACT,
        Modifier::Native => ACC_NATIVE,
        // `ACC_SYNCHRONIZED` no es decorativo: es lo **único** que hace que la JVM tome el monitor
        // del receptor al entrar al método y lo suelte en cualquier salida (§2.11.10). Sin él, un
        // `wait()`/`notifyAll()` en el cuerpo corre sin el monitor y tira
        // `IllegalMonitorStateException`, así que todo diseño de espera/aviso queda inejecutable —
        // y no hay rodeo posible, porque el modificador es parte de la API (#255).
        //
        // Comparte el bit 0x0020 con `ACC_SUPER` de las clases y con `ACC_OPEN`/`ACC_TRANSITIVE` de
        // los módulos, pero no hay ambigüedad: son tablas de flags **distintas** (§4.1 vs §4.6), y
        // esta función solo se aplica a miembros.
        Modifier::Synchronized => ACC_SYNCHRONIZED,
        _ => 0,
    }
}

// ---- descriptores (JVMS §4.3) ----

/// El envoltorio cuyo campo `TYPE` es el *mirror* de este primitivo (§15.8.2), o `None` si el tipo
/// no es primitivo. `void` también tiene el suyo: `java.lang.Void.TYPE`.
fn primitive_wrapper(ty: &Type) -> Option<&'static str> {
    Some(match ty {
        Type::Void => "java/lang/Void",
        Type::Prim(p) => match p {
            PrimType::Byte => "java/lang/Byte",
            PrimType::Char => "java/lang/Character",
            PrimType::Double => "java/lang/Double",
            PrimType::Float => "java/lang/Float",
            PrimType::Int => "java/lang/Integer",
            PrimType::Long => "java/lang/Long",
            PrimType::Short => "java/lang/Short",
            PrimType::Boolean => "java/lang/Boolean",
        },
        _ => return None,
    })
}

fn prim_desc(p: PrimType) -> &'static str {
    match p {
        PrimType::Byte => "B",
        PrimType::Char => "C",
        PrimType::Double => "D",
        PrimType::Float => "F",
        PrimType::Int => "I",
        PrimType::Long => "J",
        PrimType::Short => "S",
        PrimType::Boolean => "Z",
    }
}

/// El descriptor de un tipo **sintáctico** (resolviendo los nombres de clase a su nombre interno).
fn type_desc(table: &SymbolTable, scope: ScopeId, ty: &Type) -> String {
    match ty {
        Type::Void => "V".to_string(),
        Type::Prim(p) => prim_desc(*p).to_string(),
        Type::Array(inner) => format!("[{}", type_desc(table, scope, inner)),
        Type::Var => "Ljava/lang/Object;".to_string(),
        Type::Class(name) | Type::Parameterized { base: name, .. } => {
            match resolve_type_id(table, scope, name) {
                // Una **variable de tipo** (`T`) se **borra** a su cota (§4.6): su descriptor es el de
                // la erasure (`Object`, o la primera cota), **no** `LT;` —que referenciaría una clase
                // inexistente y no verificaría—.
                Some(id) if is_type_var(table, id) => {
                    rtype_desc(table, &super::types::erasure(table, &RType::TypeVar(id)))
                }
                Some(id) => format!("L{};", internal_name(table, id)),
                None => "Ljava/lang/Object;".to_string(),
            }
        }
    }
}

/// El descriptor de un [`RType`] ya resuelto (la *erasure* de los genéricos).
pub(crate) fn rtype_desc(table: &SymbolTable, rt: &RType) -> String {
    match rt {
        RType::Void => "V".to_string(),
        RType::Prim(p) => prim_desc(*p).to_string(),
        RType::Array(inner) => format!("[{}", rtype_desc(table, inner)),
        RType::Class(id) => format!("L{};", internal_name(table, *id)),
        // Una **variable de tipo** se **borra** a su cota (§4.6): `T` → `Ljava/lang/Object;` (o la
        // primera cota), no `LT;` —que referenciaría una clase inexistente—.
        RType::TypeVar(_) => rtype_desc(table, &super::types::erasure(table, rt)),
        RType::Parameterized { base, .. } => format!("L{};", internal_name(table, *base)),
        // Una variable de captura se emite por su cota superior (su *erasure*).
        RType::Capture { upper, .. } => rtype_desc(table, upper),
        // La intersección se emite por su primer miembro (su *erasure*, §4.6).
        RType::Intersection(ms) => ms.first().map_or_else(|| "Ljava/lang/Object;".to_string(), |m| rtype_desc(table, m)),
        // Una variable de inferencia no debería emitirse (se resuelve antes); fallback a `Object`.
        RType::InferVar(_) | RType::Unresolved => "Ljava/lang/Object;".to_string(),
    }
}

pub(crate) fn method_descriptor(table: &SymbolTable, scope: ScopeId, m: &MethodDecl) -> String {
    let tv = &m.type_params;
    let params: String = m.params.iter().map(|p| type_desc_m(table, scope, tv, &p.ty)).collect();
    let ret = if m.is_constructor {
        "V".to_string()
    } else {
        type_desc_m(table, scope, tv, &m.return_type)
    };
    format!("({params}){ret}")
}

/// El descriptor de un tipo **dentro de la firma de un método genérico**. Igual que [`type_desc`],
/// salvo que los parámetros de tipo **del propio método** (`<N extends Number> N f(Class<N>)`) no
/// viven en el scope de la clase: `resolve_type_id` no los encontraba y se caía al `Object` por
/// defecto.
///
/// Que fuera *silencioso* es lo que lo hizo durar: el `Signature` sale **bien** —lo arma otro camino,
/// que sí recibe la lista de `type_params`— y el **descriptor** mal. Es la peor combinación posible,
/// porque el `javap` muestra la firma genérica correcta y el desajuste solo aparece al **sobreescribir**:
/// un override escrito con la borradura correcta tiene otro descriptor, no sobreescribe, y da
/// `AbstractMethodError` en runtime (#100/#241).
///
/// La borradura de una variable de tipo es la de su **primera cota** (§4.6), que puede a su vez
/// nombrar a un hermano (`<A, B extends A>`); de ahí la recursión, acotada por `depth` para que un
/// ciclo declarado (`<A extends B, B extends A>`) no cuelgue el compilador.
fn type_desc_m(table: &SymbolTable, scope: ScopeId, tvars: &[TypeParam], ty: &Type) -> String {
    type_desc_bounded(table, scope, tvars, ty, 0)
}

fn type_desc_bounded(
    table: &SymbolTable,
    scope: ScopeId,
    tvars: &[TypeParam],
    ty: &Type,
    depth: u8,
) -> String {
    if depth > 8 {
        return "Ljava/lang/Object;".to_string(); // cota cíclica: la erasure es `Object`
    }
    match ty {
        Type::Array(inner) => format!("[{}", type_desc_bounded(table, scope, tvars, inner, depth)),
        Type::Class(name) | Type::Parameterized { base: name, .. } => {
            match tvars.iter().find(|tp| &tp.name == name) {
                // Una variable de tipo **del método**: su descriptor es el de su primera cota, o
                // `Object` si no declaró ninguna.
                Some(tp) => match tp.bounds.first() {
                    Some(b) => type_desc_bounded(table, scope, tvars, b, depth + 1),
                    None => "Ljava/lang/Object;".to_string(),
                },
                // Cualquier otra cosa —incluidas las variables de tipo de la **clase**, que sí
                // están en el scope— la resuelve el camino de siempre.
                None => type_desc(table, scope, ty),
            }
        }
        _ => type_desc(table, scope, ty),
    }
}

/// Los parámetros formales para `MethodParameters` (§4.7.24): el nombre + `ACC_FINAL` si se declaró
/// `final`, y `ACC_SYNTHETIC` para los **sintéticos** (las capturas `this$0`/`val$x` que inyecta el
/// desugar, reconocibles por el `$` en el nombre).
fn build_method_parameters(pool: &mut ConstantPool, m: &MethodDecl) -> Vec<ParamInfo> {
    m.params
        .iter()
        .map(|p| {
            let mut flags = 0u16;
            if p.is_final {
                flags |= ACC_FINAL;
            }
            if p.name.contains('$') {
                flags |= ACC_SYNTHETIC;
            }
            ParamInfo { name: pool.utf8(&p.name), flags }
        })
        .collect()
}

// ---- atributo Signature (§4.7.9): la firma **genérica** que la erasure borra del descriptor ----

fn is_type_var(table: &SymbolTable, id: SymbolId) -> bool {
    matches!(table.symbol(id).kind, SymbolKind::TypeVar { .. })
}

/// La firma (`§4.7.9.1`) de un tipo. `tvars` son los nombres de las variables de tipo **en alcance**
/// (parámetros de la clase y/o del método): un `Type::Class` que sea una de ellas es una
/// `TypeVariableSignature` (`TX;`), no una clase (`LX;`).
fn sig_type(table: &SymbolTable, scope: ScopeId, tvars: &HashSet<String>, ty: &Type) -> String {
    match ty {
        Type::Prim(p) => prim_desc(*p).to_string(),
        Type::Void => "V".to_string(),
        Type::Array(e) => format!("[{}", sig_type(table, scope, tvars, e)),
        Type::Var => "Ljava/lang/Object;".to_string(),
        Type::Class(name) => {
            if tvars.contains(name) {
                format!("T{name};")
            } else if let Some(id) = resolve_type_id(table, scope, name) {
                if is_type_var(table, id) {
                    format!("T{};", table.symbol(id).name)
                } else {
                    format!("L{};", internal_name(table, id))
                }
            } else {
                format!("L{};", name.replace('.', "/"))
            }
        }
        Type::Parameterized { base, args } => {
            let internal = match resolve_type_id(table, scope, base) {
                Some(id) => internal_name(table, id),
                None => base.replace('.', "/"),
            };
            let a: String = args.iter().map(|x| sig_type_arg(table, scope, tvars, x)).collect();
            format!("L{internal}<{a}>;")
        }
    }
}

/// La firma de un argumento de tipo: `+T` (`extends`), `-T` (`super`), `*` (wildcard) o el tipo.
fn sig_type_arg(table: &SymbolTable, scope: ScopeId, tvars: &HashSet<String>, arg: &TypeArg) -> String {
    match arg {
        TypeArg::Type(t) => sig_type(table, scope, tvars, t),
        TypeArg::Wildcard => "*".to_string(),
        TypeArg::Extends(t) => format!("+{}", sig_type(table, scope, tvars, t)),
        TypeArg::Super(t) => format!("-{}", sig_type(table, scope, tvars, t)),
    }
}

/// ¿El tipo **usa genéricos** (una variable de tipo o un parametrizado)? Solo entonces hace falta un
/// `Signature`: si no, el descriptor borrado ya lo describe entero.
fn sig_is_generic(table: &SymbolTable, scope: ScopeId, tvars: &HashSet<String>, ty: &Type) -> bool {
    match ty {
        Type::Parameterized { .. } => true,
        Type::Array(e) => sig_is_generic(table, scope, tvars, e),
        Type::Class(name) => {
            tvars.contains(name)
                || resolve_type_id(table, scope, name).is_some_and(|id| is_type_var(table, id))
        }
        _ => false,
    }
}

/// ¿La cota resuelve a una **interfaz**? En la firma de un parámetro de tipo, una cota de interfaz va
/// tras un `:` **extra** (la cota de clase queda vacía): `<T::LComparable<TT;>;>`.
fn bound_is_interface(table: &SymbolTable, scope: ScopeId, ty: &Type) -> bool {
    let name = match ty {
        Type::Class(n) | Type::Parameterized { base: n, .. } => n,
        _ => return false,
    };
    resolve_type_id(table, scope, name)
        .is_some_and(|id| matches!(table.symbol(id).kind, SymbolKind::Class { kind: TypeKind::Interface, .. }))
}

/// La parte `<T:cota…>` de una `ClassSignature`/`MethodSignature` (§4.7.9.1). Vacía si no hay
/// parámetros de tipo. Sin cota declarada, la cota de clase es `Object`.
fn sig_type_params(table: &SymbolTable, scope: ScopeId, tvars: &HashSet<String>, tps: &[TypeParam]) -> String {
    if tps.is_empty() {
        return String::new();
    }
    let mut s = String::from("<");
    for tp in tps {
        s.push_str(&tp.name);
        if tp.bounds.is_empty() {
            s.push_str(":Ljava/lang/Object;");
        } else {
            // La **primera** cota, si es interfaz, deja la cota de clase vacía (un `:` extra).
            if bound_is_interface(table, scope, &tp.bounds[0]) {
                s.push(':');
            }
            for b in &tp.bounds {
                s.push(':');
                s.push_str(&sig_type(table, scope, tvars, b));
            }
        }
    }
    s.push('>');
    s
}

/// Los nombres de los parámetros de tipo de una clase (para el conjunto `tvars`).
fn class_tvar_names(class: &ClassDecl) -> HashSet<String> {
    class.type_params.iter().map(|tp| tp.name.clone()).collect()
}

/// La `ClassSignature` (§4.7.9.1) de una clase, o `None` si no usa genéricos (ni parámetros de tipo,
/// ni super/interfaces parametrizados).
fn class_signature(
    table: &SymbolTable,
    scope: ScopeId,
    class: &ClassDecl,
    this_internal: &str,
) -> Option<String> {
    let tvars = class_tvar_names(class);
    // Un `enum` extiende **implícitamente** `Enum<Self>` (§8.9), un supertipo **parametrizado**: por
    // eso siempre lleva `Signature` (`Ljava/lang/Enum<LSelf;>;`), aunque el `extends` no esté escrito.
    // Sin esto la reflexión veía `Enum` crudo y el `.class` divergía de javac.
    if class.kind == TypeKind::Enum {
        let mut s = format!("Ljava/lang/Enum<L{this_internal};>;");
        for i in &class.implements {
            s.push_str(&sig_type(table, scope, &tvars, i));
        }
        return Some(s);
    }
    let super_generic = class.extends.as_ref().is_some_and(|t| sig_is_generic(table, scope, &tvars, t));
    let iface_generic = class.implements.iter().any(|t| sig_is_generic(table, scope, &tvars, t));
    if class.type_params.is_empty() && !super_generic && !iface_generic {
        return None;
    }
    let mut s = sig_type_params(table, scope, &tvars, &class.type_params);
    match &class.extends {
        Some(t) => s.push_str(&sig_type(table, scope, &tvars, t)),
        // Un `record` extiende implícitamente `java.lang.Record` (§8.10): su `Signature` genérica debe
        // nombrarlo como superclase, no `Object`. Sin esto un `record` con parámetros de tipo se
        // desensamblaba como `class GenRec<A,B>` (sin `extends java.lang.Record`).
        None if class.kind == TypeKind::Record => s.push_str("Ljava/lang/Record;"),
        None => s.push_str("Ljava/lang/Object;"),
    }
    for i in &class.implements {
        s.push_str(&sig_type(table, scope, &tvars, i));
    }
    Some(s)
}

/// La `MethodSignature` (§4.7.9.1) de un método, o `None` si no usa genéricos. Las variables de tipo
/// de la **clase** resuelven por el `scope`; solo las **propias** del método (que viven en otro
/// scope) se pasan explícitas en `tvars`.
fn method_signature(table: &SymbolTable, scope: ScopeId, m: &MethodDecl, is_enum: bool) -> Option<String> {
    // El constructor de un `enum` lleva **siempre** `Signature: ()V` (§8.9.2): su descriptor arranca
    // con los dos parámetros sintéticos `(String, int)` que `java.lang.Enum` exige, pero la firma
    // *declarada* los elide, así que javac emite el atributo para registrar la firma sin ellos.
    if m.is_constructor && is_enum {
        return Some("()V".to_string());
    }
    let tvars: HashSet<String> = m.type_params.iter().map(|tp| tp.name.clone()).collect();

    let params_g = m.params.iter().any(|p| sig_is_generic(table, scope, &tvars, &p.ty));
    let ret_g = !m.is_constructor && sig_is_generic(table, scope, &tvars, &m.return_type);
    let throws_g = m.throws.iter().any(|t| sig_is_generic(table, scope, &tvars, t));
    if m.type_params.is_empty() && !params_g && !ret_g && !throws_g {
        return None;
    }
    let mut s = sig_type_params(table, scope, &tvars, &m.type_params);
    s.push('(');
    for p in &m.params {
        s.push_str(&sig_type(table, scope, &tvars, &p.ty));
    }
    s.push(')');
    if m.is_constructor {
        s.push('V');
    } else {
        s.push_str(&sig_type(table, scope, &tvars, &m.return_type));
    }
    // Solo se listan las excepciones en `throws` si **alguna** es genérica (§4.7.9.1).
    if throws_g {
        for t in &m.throws {
            s.push('^');
            s.push_str(&sig_type(table, scope, &tvars, t));
        }
    }
    Some(s)
}

/// La `FieldSignature` (§4.7.9.1) de un campo, o `None` si su tipo no usa genéricos. Las variables de
/// tipo de la clase resuelven por el `scope`, así que no hace falta pasarlas.
fn field_signature(table: &SymbolTable, scope: ScopeId, ty: &Type) -> Option<String> {
    let none = HashSet::new();
    sig_is_generic(table, scope, &none, ty).then(|| sig_type(table, scope, &none, ty))
}

// ---- atributo InnerClasses (§4.7.6): la relación de anidamiento ----

/// El dueño de `id` **si es una clase** (o sea, `id` es un tipo anidado); `None` si es top-level
/// (dueño = paquete) o no tiene dueño.
fn class_owner(table: &SymbolTable, id: SymbolId) -> Option<SymbolId> {
    let owner = table.symbol(id).owner?;
    matches!(table.symbol(owner).kind, SymbolKind::Class { .. }).then_some(owner)
}

/// ¿Es `id` un tipo **miembro de una interfaz**? Sus miembros son implícitamente `public` y
/// `static` (§9.5), igual que los campos lo son `public static final` (§9.3) y los métodos `public`
/// (§9.4). Sin esto, un tipo anidado de una interfaz salía **package-private** y quedaba inusable
/// desde otro paquete — lo que obligó a escribir el `public` a mano en los ocho anidados de
/// `javax.lang.model.element.ModuleElement` (#242).
fn is_interface_member(table: &SymbolTable, id: SymbolId) -> bool {
    class_owner(table, id).is_some_and(|o| {
        matches!(
            table.symbol(o).kind,
            SymbolKind::Class { kind: TypeKind::Interface | TypeKind::Annotation, .. }
        )
    })
}

/// Las entradas `InnerClasses` (§4.7.6) que el `.class` de `cid` debe listar: su **cadena de
/// enclosing** (él mismo si es anidado + sus ancestros anidados) y las clases que **contiene** (las
/// de dueño `cid`). Es lo que menciona el `.class` — lo que necesita la reflexión para reconstruir
/// `getEnclosingClass`/`getDeclaringClass`/`isAnonymousClass`.
fn build_inner_classes(pool: &mut ConstantPool, table: &SymbolTable, cid: SymbolId) -> Vec<InnerClassEntry> {
    let mut ids: Vec<SymbolId> = Vec::new();
    // Cadena de enclosing: `cid` y sus ancestros que sean anidados.
    let mut c = Some(cid);
    while let Some(cur) = c {
        match class_owner(table, cur) {
            Some(o) => {
                if !ids.contains(&cur) {
                    ids.push(cur);
                }
                c = Some(o);
            }
            None => c = None,
        }
    }
    // Clases contenidas directamente (dueño = `cid`): miembros, locales y anónimas ya levantadas.
    for id in 0..table.symbol_count() {
        if matches!(table.symbol(id).kind, SymbolKind::Class { .. })
            && class_owner(table, id) == Some(cid)
            && !ids.contains(&id)
        {
            ids.push(id);
        }
    }
    ids.iter().map(|&id| inner_entry(pool, table, id)).collect()
}

/// Una entrada `InnerClasses` para `id`, clasificando por el sufijo del *binary name*: `Outer$Inner`
/// (**miembro**: con dueño y nombre), `Outer$1L` (**local**: sin dueño, con nombre), `Outer$1`
/// (**anónima**: sin dueño ni nombre).
fn inner_entry(pool: &mut ConstantPool, table: &SymbolTable, id: SymbolId) -> InnerClassEntry {
    let binary = internal_name(table, id);
    let inner = pool.class(&binary);
    let simple = binary.rsplit(['$', '/']).next().unwrap_or(&binary).to_string();
    let starts_digit = simple.chars().next().is_some_and(|c| c.is_ascii_digit());
    let all_digits = !simple.is_empty() && simple.chars().all(|c| c.is_ascii_digit());

    let sym = table.symbol(id);
    let mut flags = 0u16;
    for m in &sym.modifiers {
        flags |= modifier_flag(*m);
    }
    if matches!(sym.kind, SymbolKind::Class { kind: TypeKind::Interface | TypeKind::Annotation, .. }) {
        flags |= ACC_INTERFACE | ACC_ABSTRACT;
    }
    // §9.5, la otra mitad: acá sí van el `public` **y** el `static` implícitos. El javac real emite
    // `public static` para los tres casos (interfaz, clase y enum miembros de una interfaz).
    if is_interface_member(table, id) {
        flags |= ACC_PUBLIC | ACC_STATIC;
    }

    let (outer, name) = if all_digits {
        (0, 0) // anónima: sin dueño ni nombre
    } else if starts_digit {
        // Local: sin dueño, con nombre — y el nombre es el del FUENTE, sin el prefijo numérico.
        // Ese prefijo existe sólo para desambiguar el nombre BINARIO (dos clases locales
        // llamadas igual en dos métodos distintos compartirían archivo si no), y meterlo acá
        // hace que `getSimpleName()` devuelva `1Local` en vez de `Local`.
        let source = simple.trim_start_matches(|c: char| c.is_ascii_digit());
        (0, pool.utf8(source))
    } else {
        // miembro: dueño = su clase envolvente, nombre = el simple
        let outer = class_owner(table, id)
            .map(|o| {
                let ob = internal_name(table, o);
                pool.class(&ob)
            })
            .unwrap_or(0);
        (outer, pool.utf8(&simple))
    };
    InnerClassEntry { inner, outer, name, flags }
}

/// El atributo `EnclosingMethod` (§4.7.7): obligatorio **solo** para clases **local/anónimas**
/// (sufijo del binary con dígito). `class_index` = la clase envolvente; `method_index` = el
/// `NameAndType` del método que la declara (0 si se declaró en un inicializador, o si no se sabe).
fn enclosing_method_attr(pool: &mut ConstantPool, table: &SymbolTable, cid: SymbolId) -> Option<(u16, u16)> {
    let binary = internal_name(table, cid);
    let simple = binary.rsplit(['$', '/']).next().unwrap_or(&binary);
    if !simple.chars().next().is_some_and(|c| c.is_ascii_digit()) {
        return None; // no es local/anónima → no lleva EnclosingMethod
    }
    let owner = class_owner(table, cid)?;
    let owner_binary = internal_name(table, owner);
    let class_index = pool.class(&owner_binary);
    let method_index = match table.enclosing_method(cid) {
        Some((name, desc)) => pool.name_and_type(name, desc),
        None => 0,
    };
    Some((class_index, method_index))
}

/// El **nest host** de `cid` (§4.7.28): la clase **top-level** de su cadena de dueños. `None` si
/// `cid` ya es top-level (es su propio host, no lleva `NestHost`).
fn nest_host_of(table: &SymbolTable, cid: SymbolId) -> Option<SymbolId> {
    let mut host = None;
    let mut cur = cid;
    while let Some(owner) = class_owner(table, cur) {
        host = Some(owner);
        cur = owner;
    }
    host
}

/// Los **nest members** de una clase top-level `host` (§4.7.29): todas las clases anidadas cuyo nest
/// host es `host` (transitivo: miembros, locales y anónimas a cualquier profundidad).
fn nest_members_of(table: &SymbolTable, host: SymbolId) -> Vec<SymbolId> {
    (0..table.symbol_count())
        .filter(|&id| {
            matches!(table.symbol(id).kind, SymbolKind::Class { .. })
                && nest_host_of(table, id) == Some(host)
        })
        .collect()
}

/// Los componentes del atributo `Record` (§4.7.30): nombre + descriptor, y una `Signature` por
/// componente si su tipo usa genéricos (`record Box<T>(T val)` → componente `val` con firma `TT;`).
fn build_record_components(
    pool: &mut ConstantPool,
    table: &SymbolTable,
    scope: ScopeId,
    components: &[Param],
) -> Vec<RecordComponent> {
    components
        .iter()
        .map(|p| RecordComponent {
            name: pool.utf8(&p.name),
            descriptor: pool.utf8(&type_desc(table, scope, &p.ty)),
            signature: field_signature(table, scope, &p.ty).map(|s| pool.utf8(&s)),
        })
        .collect()
}

// ---- generación de un método ----

#[allow(clippy::too_many_arguments)]
fn gen_method(
    pool: &mut ConstantPool,
    table: &SymbolTable,
    scope: ScopeId,
    m: &MethodDecl,
    this_internal: &str,
    super_internal: &str,
    is_interface: bool,
    is_enum: bool,
    rt: &std::collections::HashSet<String>,
    tu: &TypeUseInfo,
    bootstraps: &mut Vec<BootstrapMethod>,
    errors: &mut Vec<Error>,
) -> MethodInfo {
    // Los tipos de la firma/cuerpo de un método **genérico** se resuelven en su scope propio (el que
    // ve sus `<T>`), no en el de la clase: así una variable de tipo `T extends Comparable<T>` **borra a
    // su cota** (`Comparable`) en el descriptor y en los frames del verificador —no a `Object`, que da
    // un `.class` que no verifica al invocar un método de la cota sobre el parámetro—. Un método sin
    // parámetros de tipo devuelve el scope de la clase intacto.
    let scope = super::attribute::method_sig_scope(table, scope, m);
    let name = if m.is_constructor { "<init>" } else { &m.name };
    let name_index = pool.utf8(name);
    let descriptor_index = pool.utf8(&method_descriptor(table, scope, m));
    let annotations = build_annotations(pool, table, scope, &m.annotations, rt, tu);
    // `Signature` (§4.7.9): parámetros de tipo del método + params/retorno/throws genéricos.
    let signature = method_signature(table, scope, m, is_enum).map(|s| pool.utf8(&s));
    // `MethodParameters` (§4.7.24): los nombres (+ flags) de los parámetros formales.
    let parameters = build_method_parameters(pool, m);
    // `RuntimeVisibleTypeAnnotations` (§4.7.20) del método, juntando: parámetros de tipo (`<@Foo T>`,
    // target 0x01), el **retorno** (`@NonNull String m()`, 0x14) y cada **parámetro formal**
    // (`m(@NonNull String s)`, 0x16, con su índice). Para cada uno, tanto la anotación **líder**
    // (path vacío) como los usos **anidados** dentro del tipo (`List<@A String>`, `int @A []`,
    // wildcards), cada uno con su `type_path` reconstruido por el parser.
    let mut method_ta = type_param_entries(pool, table, scope, &m.type_params, true, rt);
    // Cotas de los parámetros de tipo del método (`<T extends @A A> void m()`, target 0x12).
    method_ta.extend(type_use_bound_entries(pool, table, scope, &m.type_params, true, rt));
    if !m.is_constructor {
        method_ta.extend(type_use_lead_entries(pool, table, scope, &m.annotations, 0x14, &[], rt, tu));
        method_ta.extend(type_use_nested_entries(pool, table, scope, &m.return_annos, 0x14, &[], rt));
    }
    for (i, p) in m.params.iter().enumerate() {
        method_ta.extend(type_use_lead_entries(pool, table, scope, &p.annotations, 0x16, &[i as u8], rt, tu));
        method_ta.extend(type_use_nested_entries(pool, table, scope, &p.type_annos, 0x16, &[i as u8], rt));
    }
    // Tipos de la cláusula `throws` (`throws @A E`, target 0x17 con el índice en la cláusula).
    for (i, throw_annos) in m.throws_annos.iter().enumerate() {
        method_ta.extend(type_use_nested_entries(
            pool, table, scope, throw_annos, 0x17, &(i as u16).to_be_bytes(), rt,
        ));
    }
    let type_annotations = wrap_type_annotations(&method_ta);
    // `RuntimeVisibleParameterAnnotations` (§4.7.18): las anotaciones de **declaración** de los params.
    let parameter_annotations = build_parameter_annotations(pool, table, scope, &m.params, rt, tu);
    // `Exceptions` (§4.7.5): las clases de la cláusula `throws` (excepciones **chequeadas**). Se
    // resuelve cada tipo a su nombre interno y se lo agrega como `Class` al pool. Una variable de
    // tipo en `throws` (`throws X`) se borra a su cota (lo hace `vtype_of_type`).
    let thrown_exceptions: Vec<u16> = m
        .throws
        .iter()
        .filter_map(|t| match vtype_of_type(table, scope, t) {
            VType::Object(n) => Some(pool.class(&n)),
            _ => None,
        })
        .collect();

    // Un método **sin cuerpo** no lleva `Code` (§4.6). Dos formas bien distintas: `native` —lo
    // implementa el VM (`ACC_NATIVE`), legal en una clase concreta— y `abstract` —la firma de una
    // interfaz o de un método abstracto de una clase (`ACC_ABSTRACT`)—. `class_flags` ya aporta el
    // `ACC_NATIVE` desde el modificador; solo al abstracto hay que ponérselo. Confundirlos emitía un
    // `native` como `abstract`, dejando una clase concreta con métodos abstractos: un `.class` roto.
    if m.body.is_none() && !m.is_constructor {
        let mut flags = class_flags(&m.modifiers);
        if !m.modifiers.contains(&Modifier::Native) {
            flags |= ACC_ABSTRACT;
        }
        if is_interface {
            flags |= ACC_PUBLIC;
        }
        if is_varargs(m) {
            flags |= ACC_VARARGS;
        }
        return MethodInfo {
            access_flags: flags,
            name_index,
            descriptor_index,
            max_stack: 0,
            max_locals: 0,
            code: Vec::new(),
            stack_map: None,
            exceptions: Vec::new(),
            annotations,
            signature,
            parameters,
            thrown_exceptions,
            line_numbers: Vec::new(), // un método sin `Code` no lleva `LineNumberTable`
            local_vars: Vec::new(),
            type_annotations,
            code_type_annotations: None, // sin `Code`, no hay posiciones de bytecode que anotar
            parameter_annotations,
            annotation_default: None,
        };
    }

    let mut e = Emitter::new(
        pool,
        table,
        scope,
        this_internal.to_string(),
        super_internal.to_string(),
        bootstraps,
        errors,
        rt,
        tu,
    );
    // La categoría del retorno sale del **mismo** descriptor que se declara arriba: el opcode
    // `Xreturn` y el descriptor no pueden discrepar (§4.6, §6.5).
    e.ret_cat = if m.is_constructor {
        0
    } else {
        cat_of_desc(&type_desc(table, scope, &m.return_type))
    };
    // `this` (slot 0) en los métodos de instancia y constructores; luego los parámetros. Se anotan
    // también sus **tipos de verificación**: son los locales ya asignados al entrar, y de ahí parte
    // el cálculo de los frames.
    let is_static = m.modifiers.contains(&Modifier::Static);
    let mut slot = 0u16;
    if !is_static || m.is_constructor {
        // En un constructor el `this` está **sin inicializar** hasta que corra el `super()`.
        let this_t = if m.is_constructor {
            VType::UninitThis
        } else {
            VType::Object(this_internal.to_string())
        };
        e.set_local(0, this_t);
        slot = 1;
    }
    for p in &m.params {
        e.set_local(slot, vtype_of_type(table, scope, &p.ty));
        slot += type_width(&p.ty);
    }
    e.max_locals = slot;
    e.next_free = slot; // cursor dinámico: arranca justo encima de `this` y los parámetros
    // Reservar la región de los `synchronized` (monitores + aparcaderos de excepción, dos slots por
    // nivel de anidamiento) justo encima de los parámetros, como javac; el cursor dinámico arranca por
    // encima de ella para que los temporales de `return`/`finally` no la pisen.
    e.sync_base = e.next_free;
    e.sync_max_depth = m.body.as_ref().map_or(0, |b| max_sync_depth(&b.0));
    e.next_free += 2 * e.sync_max_depth;
    e.max_locals = e.max_locals.max(e.next_free);

    // `LocalVariableTable`: el scope del método envuelve `this` y los parámetros —vivos [0, code_len)—.
    // Los locales del cuerpo van en scopes anidados (bloques), que reflejan el reuso de slots.
    e.open_scope();
    let mut lv_slot = 0u16;
    if !is_static || m.is_constructor {
        e.open_local(0, "this", &format!("L{this_internal};"), &[]);
        lv_slot = 1;
    }
    for p in &m.params {
        e.open_local(lv_slot, &p.name, &type_desc(table, scope, &p.ty), &[]);
        lv_slot += type_width(&p.ty);
    }

    // Un constructor arranca invocando a **otro** constructor (§8.8.7): el de su superclase o, con
    // `this(...)`, uno de los suyos. Sin ese `invokespecial` el `this` queda **sin inicializar** y
    // el verificador rechaza cualquier `putfield` sobre él.
    //
    // Si el cuerpo ya arranca con un `super(...)`/`this(...)` **explícito**, ese es el que va: el
    // implícito se omite, porque inicializar dos veces el mismo objeto es ilegal (§8.8.7.1).
    if m.is_constructor && !m.body.as_ref().is_some_and(explicit_ctor_call) {
        if this_internal == "java/lang/Object" {
            // `java.lang.Object` no tiene superclase: su `<init>` **no** llama a `super()` (sería a
            // sí mismo → recursión infinita al construir). El `this` se considera inicializado al
            // entrar (JVMS §4.10.2.4), así que solo lo marcamos; el cuerpo (vacío) y el `return` siguen.
            e.init_this();
        } else {
            // El `super()` implícito va en pc 0: se le mapea la línea de la declaración del
            // constructor, así el `LineNumberTable` no deja el arranque sin línea (como javac).
            e.mark_line(m.pos.line);
            let super_init = e.pool.methodref(super_internal, "<init>", "()V");
            e.load_this(); // todavía `UninitThis`
            e.op(INVOKESPECIAL);
            e.u16(super_init);
            e.pop(1);
            e.init_this(); // ya inicializado
        }
    }

    if let Some(body) = &m.body {
        e.block_scoped(&body.0);
    }
    // Un `void`/constructor puede omitir el `return` final; lo agregamos —pero **solo si el final es
    // alcanzable**. Si el cuerpo termina en `throw`/`return` (p.ej. un `close()` que siempre lanza, o
    // el cuerpo de un try-with-resources que sale por excepción), agregarlo dejaba un `return` muerto
    // tras un `athrow`: código inalcanzable sin frame que el verificador estricto de la JVM rechaza
    // («Expecting a stack map frame»). javac tampoco lo emite.
    if (m.is_constructor || matches!(m.return_type, Type::Void)) && e.reachable {
        e.op(RETURN);
    }
    e.close_scope(); // cierra el scope del método: `this`/parámetros toman su rango [0, code_len)
    e.patch(); // resolver los saltos ahora que se conocen todos los offsets
    let stack_map = e.stack_map();
    // `RuntimeVisibleTypeAnnotations` **dentro del `Code`** (§4.7.20): los targets de posición-bytecode
    // (cast/`instanceof`/`new`) que el emisor fue anotando con el offset de cada opcode.
    let code_type_annotations = wrap_type_annotations(&e.code_type_annotations);

    // Un método de interfaz **con cuerpo** (`default`/`static`, §9.4) es implícitamente `public` salvo
    // que sea `private` (Java 9+): sin `ACC_PUBLIC` el `.class` diverge de javac y la reflexión lo ve
    // de paquete. El camino **sin** cuerpo (arriba) ya lo hacía; este —el `default`— faltaba.
    let mut method_flags = class_flags(&m.modifiers);
    if is_interface && !m.modifiers.contains(&Modifier::Private) {
        method_flags |= ACC_PUBLIC;
    }
    // `ACC_VARARGS` (§4.6): el último parámetro se declaró con `...`. Sin él la reflexión no ve el
    // método como varargs y javap lo desensambla como `T[]` en vez de `T...`.
    if m.params.last().is_some_and(|p| p.varargs) {
        method_flags |= ACC_VARARGS;
    }
    // El método sintético `$values()` de un `enum` (§8.9.3) —el que arma el arreglo `$VALUES`— lleva
    // `ACC_SYNTHETIC`, igual que el campo `$VALUES`. Real javac: `$values()` = `0x100a`.
    if m.name == "$values" {
        method_flags |= ACC_SYNTHETIC;
    }
    if is_varargs(m) {
        method_flags |= ACC_VARARGS;
    }
    MethodInfo {
        access_flags: method_flags,
        name_index,
        descriptor_index,
        max_stack: e.max_stack as u16,
        max_locals: e.max_locals,
        code: e.bytes,
        stack_map,
        exceptions: e.exceptions,
        annotations,
        signature,
        parameters,
        thrown_exceptions,
        line_numbers: e.line_numbers,
        local_vars: e.local_vars,
        type_annotations,
        code_type_annotations,
        parameter_annotations,
        annotation_default: None,
    }
}

/// Si el cuerpo de un constructor arranca con un `super(...)`/`this(...)` **explícito**. El parser
/// los codifica como una llamada cuyo nombre es la keyword (§8.8.7.1).
fn explicit_ctor_call(body: &Block) -> bool {
    matches!(
        body.0.first().map(|s| &s.kind),
        Some(StmtKind::Expr(e))
            if matches!(&e.kind, ExprKind::Call { name, .. } if name == "super" || name == "this")
    )
}

/// El [`VType`] de un tipo **sintáctico** (los parámetros de un método vienen así).
fn vtype_of_type(table: &SymbolTable, scope: ScopeId, ty: &Type) -> VType {
    match ty {
        Type::Prim(PrimType::Long) => VType::Long,
        Type::Prim(PrimType::Float) => VType::Float,
        Type::Prim(PrimType::Double) => VType::Double,
        Type::Prim(_) => VType::Int,
        Type::Void => VType::Top,
        Type::Array(_) => VType::Object(type_desc(table, scope, ty)),
        Type::Class(name) | Type::Parameterized { base: name, .. } => {
            match resolve_type_id(table, scope, name) {
                Some(id) => VType::Object(internal_name(table, id)),
                None => VType::Object("java/lang/Object".to_string()),
            }
        }
        Type::Var => VType::Object("java/lang/Object".to_string()),
    }
}

/// El constructor por defecto: `aload_0; invokespecial <super>.<init>()V; return`.
fn default_ctor(pool: &mut ConstantPool, super_internal: &str, access: u16) -> MethodInfo {
    let name_index = pool.utf8("<init>");
    let descriptor_index = pool.utf8("()V");
    let super_init = pool.methodref(super_internal, "<init>", "()V");
    let code = vec![ALOAD_0, INVOKESPECIAL, (super_init >> 8) as u8, super_init as u8, RETURN];
    MethodInfo {
        access_flags: access,
        name_index,
        descriptor_index,
        max_stack: 1,
        max_locals: 1,
        code,
        stack_map: None, // sin saltos: no lleva tabla
        exceptions: Vec::new(),
        annotations: None,
        signature: None,
        parameters: Vec::new(), // el ctor por defecto no tiene parámetros
        thrown_exceptions: Vec::new(),
        line_numbers: Vec::new(), // sintético: sin líneas de fuente
        local_vars: Vec::new(),
        type_annotations: None,
        code_type_annotations: None,
        parameter_annotations: None,
        annotation_default: None,
    }
}

// ---- RuntimeVisibleAnnotations (§4.7.16) ----

/// Los nombres **simples** de los tipos de anotación **retenidos en runtime** (§9.6.4.2): los
/// `@interface` del fuente con `@Retention(RUNTIME)`, más las conocidas del JDK (`@Deprecated`,
/// `@FunctionalInterface`). Solo esas van a `RuntimeVisibleAnnotations`; las de retención
/// `SOURCE`/`CLASS` (como `@Override`) no.
fn runtime_retained_annotations(unit: &CompilationUnit) -> std::collections::HashSet<String> {
    let mut out = std::collections::HashSet::new();
    out.insert("Deprecated".to_string());
    out.insert("FunctionalInterface".to_string());
    fn scan(class: &ClassDecl, out: &mut std::collections::HashSet<String>) {
        if class.kind == TypeKind::Annotation && is_runtime_retention(&class.annotations) {
            out.insert(class.name.clone());
        }
        for m in &class.members {
            if let Member::Type(nested) = m {
                scan(nested, out);
            }
        }
    }
    for class in &unit.types {
        scan(class, &mut out);
    }
    out
}

/// ¿La declaración lleva `@Retention(RetentionPolicy.RUNTIME)`? Se detecta por un valor cuyo nombre
/// termina en `RUNTIME` (la constante del enum `RetentionPolicy`).
fn is_runtime_retention(annotations: &[Annotation]) -> bool {
    annotations.iter().any(|a| {
        a.name == "Retention" && a.args.iter().any(|arg| value_names_runtime(&arg.value))
    })
}

fn value_names_runtime(v: &AnnotationValue) -> bool {
    let AnnotationValue::Expr(e) = v else { return false };
    match &e.kind {
        ExprKind::Field { name, .. } => name == "RUNTIME",
        ExprKind::Name(n) => n == "RUNTIME",
        _ => false,
    }
}

/// Serializa el **cuerpo** del atributo `RuntimeVisibleAnnotations` (§4.7.16) —`num_annotations` + los
/// `annotation`— para las anotaciones de `annos` que estén **retenidas en runtime** (`rt`), o `None`
/// si no queda ninguna. Una anotación con un valor que no sabemos codificar se **descarta entera**
/// (mejor no emitirla que emitir bytes inválidos).
fn build_annotations(
    pool: &mut ConstantPool,
    table: &SymbolTable,
    scope: ScopeId,
    annos: &[Annotation],
    rt: &std::collections::HashSet<String>,
    tu: &TypeUseInfo,
) -> Option<Vec<u8>> {
    let mut encoded: Vec<Vec<u8>> = Vec::new();
    for a in annos {
        let simple = a.name.rsplit('.').next().unwrap_or(&a.name);
        if !rt.contains(simple) {
            continue;
        }
        // Una anotación **solo** `@Target(TYPE_USE)` no es de declaración: va únicamente al
        // `RuntimeVisibleTypeAnnotations`, no acá (finding de fidelidad de type annotations).
        if tu.type_use_only.contains(simple) {
            continue;
        }
        if let Some(bytes) = encode_annotation(pool, table, scope, a) {
            encoded.push(bytes);
        }
    }
    if encoded.is_empty() {
        return None;
    }
    let mut body = Vec::new();
    body.extend_from_slice(&(encoded.len() as u16).to_be_bytes());
    for e in &encoded {
        body.extend_from_slice(e);
    }
    Some(body)
}

/// El nombre **simple** de una anotación (`java.lang.Foo` → `Foo`).
fn anno_simple(a: &Annotation) -> &str {
    a.name.rsplit('.').next().unwrap_or(&a.name)
}

/// Cuerpo del atributo `RuntimeVisibleParameterAnnotations` (§4.7.18): `num_parameters` (u1) y, por
/// cada parámetro formal, `num_annotations` (u2) + sus `annotation` (lo que produce
/// [`build_annotations`]: las de **declaración** retenidas en runtime, excluyendo las `TYPE_USE`-only
/// —que van al `RuntimeVisibleTypeAnnotations`—). Un parámetro sin anotaciones aporta `num_annotations
/// = 0`. `None` si **ningún** parámetro tiene una anotación (javac omite el atributo entero).
fn build_parameter_annotations(
    pool: &mut ConstantPool,
    table: &SymbolTable,
    scope: ScopeId,
    params: &[Param],
    rt: &std::collections::HashSet<String>,
    tu: &TypeUseInfo,
) -> Option<Vec<u8>> {
    let mut per_param: Vec<Vec<u8>> = Vec::new();
    let mut any = false;
    for p in params {
        match build_annotations(pool, table, scope, &p.annotations, rt, tu) {
            Some(body) => {
                any = true;
                per_param.push(body);
            }
            None => per_param.push(vec![0, 0]), // num_annotations = 0
        }
    }
    if !any {
        return None;
    }
    let mut out = Vec::new();
    out.push(params.len() as u8); // num_parameters (u1)
    for e in &per_param {
        out.extend_from_slice(e);
    }
    Some(out)
}

/// Qué anotaciones del fuente son **type annotations** por su `@Target` (§9.6.4.1). `type_use`: su
/// `@Target` incluye `TYPE_USE` (van al `RuntimeVisibleTypeAnnotations`). `type_use_only`: además no
/// tiene ningún target de **declaración**, así que **no** va al `RuntimeVisibleAnnotations`. Una
/// anotación sin `@Target`, o externa, no se considera TYPE_USE (default seguro: queda de declaración).
struct TypeUseInfo {
    type_use: std::collections::HashSet<String>,
    type_use_only: std::collections::HashSet<String>,
}

/// Recolecta los nombres de constante `ElementType.X` de un valor de `@Target` (un `ElementType.X`
/// suelto o un arreglo `{ … }`).
fn element_type_names(v: &AnnotationValue, out: &mut Vec<String>) {
    match v {
        AnnotationValue::Array(items) => items.iter().for_each(|i| element_type_names(i, out)),
        AnnotationValue::Expr(e) => match &e.kind {
            ExprKind::Field { name, .. } => out.push(name.clone()),
            ExprKind::Name(n) => out.push(n.clone()),
            _ => {}
        },
        AnnotationValue::Nested(_) => {}
    }
}

fn type_use_info(unit: &CompilationUnit) -> TypeUseInfo {
    let mut type_use = std::collections::HashSet::new();
    let mut type_use_only = std::collections::HashSet::new();
    fn scan(
        class: &ClassDecl,
        tu: &mut std::collections::HashSet<String>,
        tuo: &mut std::collections::HashSet<String>,
    ) {
        if class.kind == TypeKind::Annotation {
            let mut targets = Vec::new();
            for a in &class.annotations {
                if anno_simple(a) == "Target" {
                    for arg in &a.args {
                        element_type_names(&arg.value, &mut targets);
                    }
                }
            }
            if targets.iter().any(|t| t == "TYPE_USE") {
                tu.insert(class.name.clone());
                // Un target que no es TYPE_USE ni TYPE_PARAMETER es de **declaración**.
                let has_decl = targets.iter().any(|t| t != "TYPE_USE" && t != "TYPE_PARAMETER");
                if !has_decl {
                    tuo.insert(class.name.clone());
                }
            }
        }
        for m in &class.members {
            if let Member::Type(n) = m {
                scan(n, tu, tuo);
            }
        }
    }
    for c in &unit.types {
        scan(c, &mut type_use, &mut type_use_only);
    }
    TypeUseInfo { type_use, type_use_only }
}

/// Un `type_annotation` (§4.7.20): `target_type` (u1) + `target_info` + `type_path` + el `annotation`.
fn type_annotation_entry(target_type: u8, target_info: &[u8], type_path: &[u8], ann: &[u8]) -> Vec<u8> {
    let mut e = Vec::with_capacity(1 + target_info.len() + type_path.len() + ann.len());
    e.push(target_type);
    e.extend_from_slice(target_info);
    e.extend_from_slice(type_path);
    e.extend_from_slice(ann);
    e
}

/// Envuelve las `entries` en el **cuerpo** del atributo (`num_annotations` + las entradas), o `None`.
fn wrap_type_annotations(entries: &[Vec<u8>]) -> Option<Vec<u8>> {
    if entries.is_empty() {
        return None;
    }
    let mut body = Vec::new();
    body.extend_from_slice(&(entries.len() as u16).to_be_bytes());
    for e in entries {
        body.extend_from_slice(e);
    }
    Some(body)
}

/// Entradas para las anotaciones sobre **parámetros de tipo** (target `0x00` clase / `0x01` método):
/// `target_info` = `type_parameter_index`, `type_path` vacío.
fn type_param_entries(
    pool: &mut ConstantPool,
    table: &SymbolTable,
    scope: ScopeId,
    type_params: &[TypeParam],
    on_method: bool,
    rt: &std::collections::HashSet<String>,
) -> Vec<Vec<u8>> {
    let target_type = if on_method { 0x01u8 } else { 0x00u8 };
    let mut entries = Vec::new();
    for (i, tp) in type_params.iter().enumerate() {
        for a in &tp.annotations {
            if !rt.contains(anno_simple(a)) {
                continue;
            }
            if let Some(ann) = encode_annotation(pool, table, scope, a) {
                entries.push(type_annotation_entry(target_type, &[i as u8], &[0u8], &ann));
            }
        }
    }
    entries
}

/// Entradas para las anotaciones **líder** que son TYPE_USE (§9.7.4), en la posición `target_type`
/// (`0x13` campo, `0x14` retorno, `0x16` parámetro) con el `target_info` dado y `type_path` vacío.
/// Solo las que estén en `tu.type_use` y retenidas en runtime (`rt`).
fn type_use_lead_entries(
    pool: &mut ConstantPool,
    table: &SymbolTable,
    scope: ScopeId,
    annos: &[Annotation],
    target_type: u8,
    target_info: &[u8],
    rt: &std::collections::HashSet<String>,
    tu: &TypeUseInfo,
) -> Vec<Vec<u8>> {
    let mut entries = Vec::new();
    for a in annos {
        let simple = anno_simple(a);
        if !tu.type_use.contains(simple) || !rt.contains(simple) {
            continue;
        }
        if let Some(ann) = encode_annotation(pool, table, scope, a) {
            entries.push(type_annotation_entry(target_type, target_info, &[0u8], &ann));
        }
    }
    entries
}

/// Serializa un `type_path` (§4.7.20.2): `path_length` (u1) seguido de ese número de pares
/// `{type_path_kind (u1), type_argument_index (u1)}`. Los *kinds*: `0` array, `1` nested (`Outer.Inner`),
/// `2` cota de wildcard, `3` argumento de tipo (con su índice). El path vacío (`[0]`) es la posición
/// del tipo entero; cada paso baja un nivel hacia el tipo anotado.
fn serialize_type_path(path: &[TypePathStep]) -> Vec<u8> {
    let mut out = Vec::with_capacity(1 + path.len() * 2);
    out.push(path.len() as u8);
    for step in path {
        let (kind, arg) = match step {
            TypePathStep::Array => (0u8, 0u8),
            TypePathStep::Nested => (1u8, 0u8),
            TypePathStep::WildcardBound => (2u8, 0u8),
            TypePathStep::TypeArgument(i) => (3u8, *i),
        };
        out.push(kind);
        out.push(arg);
    }
    out
}

/// Entradas para las anotaciones TYPE_USE en posiciones **anidadas** del tipo (`List<@A String>`,
/// `int @A []`, `Map<String, @A ? extends Number>`), cada una con su `type_path` reconstruido por el
/// parser. `target_type`/`target_info` son los del elemento que declara el tipo (`0x13` campo,
/// `0x14` retorno, `0x16` parámetro). Solo las retenidas en runtime (`rt`).
fn type_use_nested_entries(
    pool: &mut ConstantPool,
    table: &SymbolTable,
    scope: ScopeId,
    tas: &[TypeUseAnnot],
    target_type: u8,
    target_info: &[u8],
    rt: &std::collections::HashSet<String>,
) -> Vec<Vec<u8>> {
    let mut entries = Vec::new();
    for ta in tas {
        if !rt.contains(anno_simple(&ta.annotation)) {
            continue;
        }
        if let Some(ann) = encode_annotation(pool, table, scope, &ta.annotation) {
            let path = serialize_type_path(&ta.path);
            entries.push(type_annotation_entry(target_type, target_info, &path, &ann));
        }
    }
    entries
}

/// Entradas para las anotaciones sobre las **cotas** de los parámetros de tipo (`<T extends @A A & @B B>`),
/// target `0x11` (clase) / `0x12` (método). `target_info` = `{type_parameter_index, bound_index}`.
/// El `bound_index` sigue a real javac: si la **primera** cota escrita es una interfaz, hay una cota de
/// clase implícita `Object` en el índice 0, así que las escritas arrancan en 1; si es una clase, en 0.
fn type_use_bound_entries(
    pool: &mut ConstantPool,
    table: &SymbolTable,
    scope: ScopeId,
    type_params: &[TypeParam],
    on_method: bool,
    rt: &std::collections::HashSet<String>,
) -> Vec<Vec<u8>> {
    let target_type = if on_method { 0x12u8 } else { 0x11u8 };
    let mut entries = Vec::new();
    for (tpi, tp) in type_params.iter().enumerate() {
        // Si la primera cota es una interfaz, javac cuenta desde 1 (el 0 lo ocupa el `Object` implícito).
        let base = match tp.bounds.first() {
            Some(b) if !bound_is_interface(table, scope, b) => 0usize,
            _ => 1usize,
        };
        for (bi, bound_annos) in tp.bound_annos.iter().enumerate() {
            if bound_annos.is_empty() {
                continue;
            }
            let target_info = [tpi as u8, (base + bi) as u8];
            entries.extend(type_use_nested_entries(
                pool, table, scope, bound_annos, target_type, &target_info, rt,
            ));
        }
    }
    entries
}

/// Una `annotation` (§4.7.16): `type_index` (descriptor del tipo) + `num_element_value_pairs` + los
/// pares `nombre → element_value`. `None` si algún valor no se sabe codificar.
fn encode_annotation(
    pool: &mut ConstantPool,
    table: &SymbolTable,
    scope: ScopeId,
    a: &Annotation,
) -> Option<Vec<u8>> {
    let type_index = pool.utf8(&annotation_descriptor(table, scope, &a.name));
    let mut pairs = Vec::new();
    for arg in &a.args {
        // La forma de **valor único** (`@Ann(x)`) nombra el elemento implícito `value`.
        let elem = arg.name.clone().unwrap_or_else(|| "value".to_string());
        let name_index = pool.utf8(&elem);
        let value = encode_value(pool, table, scope, &arg.value)?;
        pairs.extend_from_slice(&name_index.to_be_bytes());
        pairs.extend_from_slice(&value);
    }
    let mut out = Vec::new();
    out.extend_from_slice(&type_index.to_be_bytes());
    out.extend_from_slice(&(a.args.len() as u16).to_be_bytes());
    out.extend_from_slice(&pairs);
    Some(out)
}

/// El descriptor de campo del tipo de anotación: `Ljava/lang/Deprecated;`. Se resuelve por la tabla;
/// si no está, cae a `Lnombre/con/barras;`.
fn annotation_descriptor(table: &SymbolTable, scope: ScopeId, name: &str) -> String {
    let simple = name.rsplit('.').next().unwrap_or(name);
    match resolve_type_id(table, scope, simple) {
        Some(id) => format!("L{};", internal_name(table, id)),
        None => format!("L{};", name.replace('.', "/")),
    }
}

/// Un `element_value` (§4.7.16.1) con su `tag`: `s`/`I`/`Z`/…, `c` (literal de clase), `e` (constante
/// de enum), `@` (anotación anidada) o `[` (arreglo). `None` si no se sabe codificar (el llamador
/// descarta la anotación entera). Un entero sin el tipo del elemento a mano se etiqueta como `I`.
fn encode_value(
    pool: &mut ConstantPool,
    table: &SymbolTable,
    scope: ScopeId,
    v: &AnnotationValue,
) -> Option<Vec<u8>> {
    let mut out = Vec::new();
    match v {
        AnnotationValue::Array(items) => {
            out.push(b'[');
            out.extend_from_slice(&(items.len() as u16).to_be_bytes());
            for it in items {
                out.extend_from_slice(&encode_value(pool, table, scope, it)?);
            }
        }
        AnnotationValue::Nested(a) => {
            out.push(b'@');
            out.extend_from_slice(&encode_annotation(pool, table, scope, a)?);
        }
        AnnotationValue::Expr(e) => match &e.kind {
            ExprKind::StringLit(s) => {
                out.push(b's');
                out.extend_from_slice(&pool.utf8(s).to_be_bytes());
            }
            ExprKind::IntLit(n) => {
                out.push(b'I');
                out.extend_from_slice(&pool.integer(*n as i32).to_be_bytes());
            }
            ExprKind::BoolLit(b) => {
                out.push(b'Z');
                out.extend_from_slice(&pool.integer(*b as i32).to_be_bytes());
            }
            ExprKind::CharLit(c) => {
                out.push(b'C');
                out.extend_from_slice(&pool.integer(*c as i32).to_be_bytes());
            }
            ExprKind::LongLit(n) => {
                out.push(b'J');
                out.extend_from_slice(&pool.long(*n).to_be_bytes());
            }
            ExprKind::DoubleLit(d) => {
                out.push(b'D');
                out.extend_from_slice(&pool.double(*d).to_be_bytes());
            }
            ExprKind::FloatLit(f) => {
                out.push(b'F');
                out.extend_from_slice(&pool.float(*f as f32).to_be_bytes());
            }
            ExprKind::ClassLit(ty) => {
                out.push(b'c');
                out.extend_from_slice(&pool.utf8(&type_desc(table, scope, ty)).to_be_bytes());
            }
            // `Tipo.CONST` (constante de enum): descriptor del tipo + nombre. Best-effort: solo la
            // forma cualificada por un nombre simple.
            ExprKind::Field { expr, name } => {
                let ExprKind::Name(tn) = &expr.kind else { return None };
                out.push(b'e');
                out.extend_from_slice(&pool.utf8(&annotation_descriptor(table, scope, tn)).to_be_bytes());
                out.extend_from_slice(&pool.utf8(name).to_be_bytes());
            }
            _ => return None,
        },
    }
    Some(out)
}

fn type_width(ty: &Type) -> u16 {
    matches!(ty, Type::Prim(PrimType::Long | PrimType::Double)) as u16 + 1
}

// ---- opcodes (JVMS §6) ----

const ACONST_NULL: u8 = 0x01;
const ICONST_M1: u8 = 0x02;
const LCONST_0: u8 = 0x09; // lconst_0 / lconst_1
const FCONST_0: u8 = 0x0b; // fconst_0 / fconst_1 / fconst_2
const DCONST_0: u8 = 0x0e; // dconst_0 / dconst_1
const BIPUSH: u8 = 0x10;
const SIPUSH: u8 = 0x11;
const LDC: u8 = 0x12;
const LDC_W: u8 = 0x13;
const LDC2_W: u8 = 0x14; // el único que carga una constante **ancha** (long/double)
const ILOAD: u8 = 0x15; // base de la familia i/l/f/d/a (consecutivas)
const ILOAD_0: u8 = 0x1a; // base de las formas cortas (cada familia, 4 opcodes)
const ISTORE: u8 = 0x36;
const ISTORE_0: u8 = 0x3b;
const ALOAD_0: u8 = 0x2a;
const POP: u8 = 0x57;
const POP2: u8 = 0x58;
const IINC: u8 = 0x84; // incrementa un local `int` **sin tocar la pila**
const DUP: u8 = 0x59;
const DUP_X2: u8 = 0x5b;
const DUP2: u8 = 0x5c;
const DUP2_X2: u8 = 0x5e;
const GETSTATIC: u8 = 0xb2;
const PUTSTATIC: u8 = 0xb3;
const GETFIELD: u8 = 0xb4;
const PUTFIELD: u8 = 0xb5;
const INVOKEVIRTUAL: u8 = 0xb6;
const NEW: u8 = 0xbb;
const NEWARRAY: u8 = 0xbc; // arrays de primitivos (lleva un código de tipo)
const ANEWARRAY: u8 = 0xbd; // arrays de referencias (lleva el índice de la clase)
const ARRAYLENGTH: u8 = 0xbe;
const ATHROW: u8 = 0xbf;
const CHECKCAST: u8 = 0xc0;
const INSTANCEOF: u8 = 0xc1;
const IALOAD: u8 = 0x2e; // base de la familia i l f d a b c s
const IASTORE: u8 = 0x4f; // ídem, para escribir
const MONITORENTER: u8 = 0xc2;
const MONITOREXIT: u8 = 0xc3;
const TABLESWITCH: u8 = 0xaa; // salto múltiple por índice directo
const LOOKUPSWITCH: u8 = 0xab; // salto múltiple por búsqueda binaria

/// El offset dentro de las familias `Xaload`/`Xastore`, que van `i l f d a b c s` — o sea que
/// `byte`/`char`/`short` tienen su **propio** opcode aunque compartan la categoría `int`.
fn array_kind(elem: &RType) -> u8 {
    match elem {
        RType::Prim(PrimType::Long) => 1,
        RType::Prim(PrimType::Float) => 2,
        RType::Prim(PrimType::Double) => 3,
        RType::Prim(PrimType::Byte | PrimType::Boolean) => 5,
        RType::Prim(PrimType::Char) => 6,
        RType::Prim(PrimType::Short) => 7,
        RType::Prim(_) => 0,
        _ => 4, // referencia
    }
}

/// El código de tipo de `newarray` (JVMS Table 6.5-newarray-A).
fn newarray_code(p: PrimType) -> u8 {
    match p {
        PrimType::Boolean => 4,
        PrimType::Char => 5,
        PrimType::Float => 6,
        PrimType::Double => 7,
        PrimType::Byte => 8,
        PrimType::Short => 9,
        PrimType::Int => 10,
        PrimType::Long => 11,
    }
}
const IADD: u8 = 0x60; // add=0x60 sub=0x64 mul=0x68 div=0x6c rem=0x70 (+ cat)
const IRETURN: u8 = 0xac; // base i/l/f/d/a
const RETURN: u8 = 0xb1;
const INVOKESPECIAL: u8 = 0xb7;
const INVOKESTATIC: u8 = 0xb8;
const INVOKEINTERFACE: u8 = 0xb9; // método de interfaz: índice + `count` + un byte cero
const INVOKEDYNAMIC: u8 = 0xba; // call site dinámico: índice InvokeDynamic + dos bytes cero

/// El *reference kind* `REF_invokeStatic` (§5.4.3.5): todo *bootstrap method* (`LambdaMetafactory` u
/// `ObjectMethods`) es un método estático.
const REF_INVOKE_STATIC: u8 = 6;
const LCMP: u8 = 0x94; // lcmp / fcmpl / fcmpg / dcmpl / dcmpg
const IFEQ: u8 = 0x99; // base de la familia de 1 operando: eq ne lt ge gt le
const IF_ICMPEQ: u8 = 0x9f; // base de la familia de 2 enteros, mismo orden
const IF_ACMPEQ: u8 = 0xa5; // solo eq/ne (referencias)
const GOTO: u8 = 0xa7;

/// El índice de comparación dentro de una familia (`eq ne lt ge gt le`), ya **invertido** si el
/// salto es "cuando la condición sea falsa": saltar si `a < b` es falso ⇒ saltar si `a >= b`.
/// Cuanto RESTARLE al opcode de comparacion para elegir su forma.
///
/// La aritmetica de `LCMP + (cat - 1) * 2` aterriza en 0x96 (`fcmpg`) y 0x98 (`dcmpg`), o sea
/// que la base ya es la forma `g`; devolver 1 la baja a la `l` (0x95 / 0x97) y 0 la deja.
/// Para `long` siempre 0: `lcmp` es unico y restarle daria otro opcode.
///
/// La eleccion NO es cosmetica y no la cubre el descriptor. Con NaN toda comparacion tiene que
/// dar falso (menos `!=`), y el opcode de comparacion no sabe que rama viene despues: lo unico
/// que puede hacer es entregar un -1 o un +1 elegido para que la rama que SI viene salga como
/// corresponde.
///
/// De ahi la regla, que depende del operador del FUENTE y no de la rama emitida -- una condicion
/// negada (`when == false`) invierte la rama pero no invierte cual de las dos formas sirve:
///
/// - `<` y `<=` piden la `g`: NaN da +1, que hace fallar la prueba de "menor" y acertar su negacion.
/// - `>` y `>=` piden la `l`: NaN da -1, simetricamente.
/// - `==` y `!=` funcionan con cualquiera (NaN nunca da 0); se emite la `l`, como javac.
///
/// Emitir siempre la `g` -- que es lo que se hacia -- deja `x >= NaN` y `x > NaN` en **true**
/// (finding #266).
fn nan_variant(cat: u8, op: BinOp) -> u8 {
    if cat == 1 {
        return 0; // `lcmp`: no hay NaN entre los longs, y no hay segunda forma
    }
    match op {
        BinOp::Lt | BinOp::Le => 0, // la `g`, que es la base
        _ => 1,                     // la `l`
    }
}

fn cmp_index(op: BinOp, when: bool) -> Option<u8> {
    let idx = match op {
        BinOp::Eq => 0,
        BinOp::Ne => 1,
        BinOp::Lt => 2,
        BinOp::Ge => 3,
        BinOp::Gt => 4,
        BinOp::Le => 5,
        _ => return None,
    };
    // Los pares opuestos son (0,1) (2,3) (4,5): invertir es alternar el bit bajo.
    Some(if when { idx } else { idx ^ 1 })
}

/// La comparación con los operandos **intercambiados** (`a op b` ⟺ `b rev(op) a`): `<`↔`>`, `<=`↔`>=`;
/// `==`/`!=` son simétricas. Para emitir `0 op x` como el `x rev(op) 0` de un solo operando.
fn reverse_cmp(op: BinOp) -> BinOp {
    match op {
        BinOp::Lt => BinOp::Gt,
        BinOp::Gt => BinOp::Lt,
        BinOp::Le => BinOp::Ge,
        BinOp::Ge => BinOp::Le,
        other => other,
    }
}

/// La categoría de tipo para elegir la variante de un opcode: 0=`int`, 1=`long`, 2=`float`,
/// 3=`double`, 4=referencia. Las familias de opcodes están ordenadas así, con offset `cat`.
fn category(rt: &RType) -> u8 {
    match rt {
        RType::Prim(PrimType::Long) => 1,
        RType::Prim(PrimType::Float) => 2,
        RType::Prim(PrimType::Double) => 3,
        RType::Prim(_) | RType::Void => 0,
        _ => 4,
    }
}

/// La categoría de un valor **tal como está en la pila** — no la de su tipo estático. Es la que
/// manda para decidir si hace falta una ampliación: `boolean`/`byte`/`char`/`short`/`int` viven
/// todos como `Int`.
fn cat_of_vtype(vt: &VType) -> u8 {
    match vt {
        VType::Int => 0,
        VType::Long => 1,
        VType::Float => 2,
        VType::Double => 3,
        _ => 4,
    }
}

/// La categoría que corresponde a un **descriptor de retorno**. Se saca del descriptor y no del
/// tipo sintáctico a propósito: el opcode `Xreturn` y el descriptor que declara el método tienen
/// que salir de la **misma** fuente, o el class file queda estructuralmente inválido (#217).
fn cat_of_desc(desc: &str) -> u8 {
    match desc.as_bytes().first() {
        Some(b'J') => 1,
        Some(b'F') => 2,
        Some(b'D') => 3,
        Some(b'L') | Some(b'[') => 4,
        _ => 0,
    }
}

/// Un `verification_type_info` de la `StackMapTable` (JVMS §4.7.4). `boolean`/`byte`/`char`/`short`
/// no existen para el verificador: todos son `Integer`.
#[derive(Clone, PartialEq, Debug)]
enum VType {
    Top,
    Int,
    Float,
    Double,
    Long,
    /// Una referencia, por su **nombre interno** (para un array, su descriptor: `[I`).
    Object(String),
    /// El `this` de un constructor antes de su `super()`.
    UninitThis,
    /// Un objeto **recién creado** y todavía sin constructor, identificado por el offset de su
    /// `new` (tag 8). Sobre él solo vale llamar a `<init>`: cualquier otro uso lo rechaza el
    /// verificador (§4.10.2.4). Cuando el `<init>` corre, **todas** sus apariciones —en la pila y en
    /// los locales— pasan a ser el tipo ya inicializado.
    Uninit(usize),
}

/// El [`VType`] de un **descriptor** ya serializado — el camino corto cuando lo que hay a mano es
/// el descriptor del campo, no su `RType`.
fn vtype_of_desc(desc: &str) -> VType {
    match desc.as_bytes().first() {
        Some(b'J') => VType::Long,
        Some(b'D') => VType::Double,
        Some(b'F') => VType::Float,
        // Un array se nombra por su descriptor completo (`[I`); una clase, sin la `L`/`;`.
        Some(b'[') => VType::Object(desc.to_string()),
        Some(b'L') => VType::Object(desc[1..desc.len() - 1].to_string()),
        _ => VType::Int, // byte/char/short/boolean/int: todos `Integer` para el verificador
    }
}

fn vtype_of(table: &SymbolTable, rt: &RType) -> VType {
    match rt {
        RType::Prim(PrimType::Long) => VType::Long,
        RType::Prim(PrimType::Float) => VType::Float,
        RType::Prim(PrimType::Double) => VType::Double,
        RType::Prim(_) => VType::Int,
        RType::Void | RType::Unresolved | RType::InferVar(_) => VType::Top,
        RType::Array(_) => VType::Object(rtype_desc(table, rt)),
        RType::Class(id) | RType::TypeVar(id) => VType::Object(internal_name(table, *id)),
        RType::Parameterized { base, .. } => VType::Object(internal_name(table, *base)),
        // El verificador ve una variable de captura por su cota superior (su *erasure*).
        RType::Capture { upper, .. } => vtype_of(table, upper),
        RType::Intersection(ms) => ms.first().map_or(VType::Top, |m| vtype_of(table, m)),
    }
}

/// El **merge** de los estados que llegan a un destino de salto: un slot que no coincide en todos
/// los caminos no tiene un tipo válido ahí, y pasa a `Top` (inutilizable).
fn merge_states(states: &[(Vec<VType>, Vec<VType>)]) -> Option<(Vec<VType>, Vec<VType>)> {
    let (first, rest) = states.split_first()?;
    let mut locals = first.0.clone();
    for (other, _) in rest {
        locals.resize(locals.len().min(other.len()), VType::Top);
        for (i, t) in locals.iter_mut().enumerate() {
            if *t != other[i] {
                *t = VType::Top;
            }
        }
    }
    // La **pila** no se puede degradar a `Top`: tiene que coincidir en todos los caminos (§4.10.1.2),
    // y el emisor se encarga de que así sea (el ternario promueve sus dos ramas al mismo tipo). Se
    // toma la del primer camino.
    Some((locals, first.1.clone()))
}

/// El valor de una etiqueta `case`, que tiene que ser una **constante de compilación** (§14.11).
/// Acá solo se ven las formas que sobreviven al desugar: literales `int`/`char` y sus envoltorios
/// sintácticos. Una `static final int` usada como etiqueta todavía no se pliega — cae en la
/// barrera, que es lo que corresponde mientras no haya evaluación de constantes.
fn const_int(e: &Expr) -> Option<i32> {
    match &e.kind {
        ExprKind::IntLit(n) => Some(*n as i32),
        ExprKind::CharLit(c) => Some(*c as i32),
        ExprKind::Unary { op: UnOp::Neg, expr, .. } => const_int(expr).map(|v| -v),
        ExprKind::Unary { op: UnOp::Plus, expr, .. } => const_int(expr),
        ExprKind::Cast { expr, .. } => const_int(expr),
        _ => None,
    }
}

/// El valor constante de un campo, ya tipado para el atributo `ConstantValue` (§4.7.2): la variante
/// determina qué entrada del pool se emite (`Integer`/`Long`/`Float`/`Double`/`String`).
#[derive(Clone)]
pub(crate) enum ConstVal {
    Int(i32),
    Long(i64),
    Float(f32),
    Double(f64),
    Str(String),
}

/// El valor **numérico** de una expresión constante de compilación (§15.29), con su tipo (para la
/// promoción §5.6.2 y el truncado al tipo del campo). `char`/`byte`/`short`/`int` se representan como
/// `Int`; su tipo declarado solo importa al coercionar en [`const_field_value`].
#[derive(Clone, Copy)]
enum NumV {
    Int(i32),
    Long(i64),
    Float(f32),
    Double(f64),
}

/// El mapa de **valores de campos constantes** de la unidad (`SymbolId del campo → valor plegado`),
/// que permite inlinear referencias entre `final`: numéricas (`static final int B = A * 2;`) y de
/// `String` (`static final String T = S + "!";`). Se construye por fixpoint en [`collect_const_fields`]
/// y se consulta al plegar. Vacío durante el atributado.
pub(crate) type ConstFieldMap = std::collections::HashMap<SymbolId, ConstVal>;

/// Evalúa una **expresión constante numérica** (§15.29): literales, `+`/`-`/`~` unarios, cast entre
/// primitivos, y los binarios aritméticos, de bits y de shift con la **promoción numérica binaria**
/// (§5.6.2). `None` si no es una constante plegable (una `final` referenciada, una división por cero,
/// un operando booleano…): esos casos caen al `<clinit>` — correcto, solo no *inlineados*.
fn const_eval_num(e: &Expr, consts: &ConstFieldMap) -> Option<NumV> {
    Some(match &e.kind {
        ExprKind::IntLit(n) => NumV::Int(*n as i32),
        ExprKind::CharLit(c) => NumV::Int(*c as i32),
        ExprKind::LongLit(n) => NumV::Long(*n),
        ExprKind::FloatLit(f) => NumV::Float(*f as f32),
        ExprKind::DoubleLit(d) => NumV::Double(*d),
        ExprKind::Unary { op: UnOp::Plus, expr, .. } => const_eval_num(expr, consts)?,
        ExprKind::Unary { op: UnOp::Neg, expr, .. } => match const_eval_num(expr, consts)? {
            NumV::Int(x) => NumV::Int(x.wrapping_neg()),
            NumV::Long(x) => NumV::Long(x.wrapping_neg()),
            NumV::Float(x) => NumV::Float(-x),
            NumV::Double(x) => NumV::Double(-x),
        },
        ExprKind::Unary { op: UnOp::BitNot, expr, .. } => match const_eval_num(expr, consts)? {
            NumV::Int(x) => NumV::Int(!x),
            NumV::Long(x) => NumV::Long(!x),
            _ => return None, // `~` solo aplica a enteros
        },
        ExprKind::Cast { ty, expr } => cast_num(ty, const_eval_num(expr, consts)?)?,
        ExprKind::Binary { op, lhs, rhs } => {
            eval_binop_num(*op, const_eval_num(lhs, consts)?, const_eval_num(rhs, consts)?)?
        }
        // Referencia a otra **constante** de la unidad (`static final int B = A * 2;`): si `A` ya se
        // plegó a un valor conocido, se **inlinea** aquí (§13.4.9/§15.29). Vale tanto para un nombre
        // suelto (`A`) como para el acceso cualificado (`Clase.A`), ambos con `Binding::Field`.
        ExprKind::Name(_) | ExprKind::Field { .. } => match e.binding {
            Some(Binding::Field(sym)) => match consts.get(&sym)? {
                ConstVal::Int(n) => NumV::Int(*n),
                ConstVal::Long(n) => NumV::Long(*n),
                ConstVal::Float(n) => NumV::Float(*n),
                ConstVal::Double(n) => NumV::Double(*n),
                ConstVal::Str(_) => return None, // una `String` constante no es un valor numérico
            },
            _ => return None,
        },
        _ => return None,
    })
}

/// Evalúa una **expresión constante de tipo `String`** (§15.29/§15.18.1): un literal, la
/// **concatenación** `+` con al menos un operando `String`, o una **referencia** a otra `String`
/// constante de la unidad (`Binding::Field` que resolvió a un `ConstVal::Str`). `None` si no es una
/// constante `String` plegable —entonces la init cae al `<clinit>` con su `new StringBuilder`—.
///
/// La disyuntiva `"a" + 2` (concat) vs `1 + 2` (aritmética) se decide por §15.18.1: el `+` es concat
/// **solo si algún operando es de tipo `String`**. Por eso `const_eval_str` devuelve `Some` únicamente
/// para expresiones cuyo *tipo* es `String` (literal, ref a `String` final, o un `+` que ya es concat);
/// un `char`/`boolean`/número suelto da `None` aquí y solo se **convierte a texto** como operando de un
/// concat ya decidido (ver [`operand_to_string`]).
fn const_eval_str(e: &Expr, consts: &ConstFieldMap) -> Option<String> {
    match &e.kind {
        ExprKind::StringLit(s) => Some(s.clone()),
        ExprKind::Binary { op: BinOp::Add, lhs, rhs } => {
            let (ls, rs) = (const_eval_str(lhs, consts), const_eval_str(rhs, consts));
            if ls.is_none() && rs.is_none() {
                return None; // ningún operando es `String`: es aritmética, no concatenación
            }
            Some(operand_to_string(lhs, consts)? + &operand_to_string(rhs, consts)?)
        }
        ExprKind::Name(_) | ExprKind::Field { .. } => match e.binding {
            Some(Binding::Field(sym)) => match consts.get(&sym)? {
                ConstVal::Str(s) => Some(s.clone()),
                _ => None, // un campo numérico no es de tipo `String`
            },
            _ => None,
        },
        _ => None,
    }
}

/// La **conversión a texto** (§5.1.11) de un operando **constante** de una concatenación ya decidida
/// como tal: una `String` va tal cual; un `boolean`/`char` literal por su forma textual; un número por
/// su representación decimal. Un `char` referenciado por campo se pierde a decimal (el mapa lo guarda
/// como entero) — limitación menor y muy poco frecuente en constantes.
fn operand_to_string(e: &Expr, consts: &ConstFieldMap) -> Option<String> {
    if let Some(s) = const_eval_str(e, consts) {
        return Some(s);
    }
    match &e.kind {
        ExprKind::BoolLit(b) => return Some(b.to_string()),
        // Un sustituto suelto no tiene `char` de Rust, asi que no se pliega: lo emite el
        // camino normal, que trabaja sobre la unidad de codigo.
        ExprKind::CharLit(c) => return char::from_u32(u32::from(*c)).map(|ch| ch.to_string()),
        _ => {}
    }
    Some(match const_eval_num(e, consts)? {
        NumV::Int(n) => n.to_string(),
        NumV::Long(n) => n.to_string(),
        NumV::Float(f) => java_fp_string(f as f64),
        NumV::Double(d) => java_fp_string(d),
    })
}

/// Aproxima `Double.toString`/`Float.toString` de Java para la concatenación de constantes: como Rust
/// imprime `1.0` como `"1"`, se le agrega `.0` a los valores **integrales** finitos. No reproduce el
/// algoritmo de dígitos mínimos de Java, pero cubre los casos usuales (`"v=" + 1.5` → `"v=1.5"`,
/// `"v=" + 1.0` → `"v=1.0"`).
fn java_fp_string(d: f64) -> String {
    let s = d.to_string();
    if d.is_finite() && !s.contains(['.', 'e', 'E']) {
        format!("{s}.0")
    } else {
        s
    }
}

/// El valor de una **expresión constante booleana** (§15.28): `true`/`false`, `!`, los lógicos y de
/// bits sobre `boolean` (`&&`, `||`, `&`, `|`, `^`), la igualdad `==`/`!=` entre booleanos, y las
/// comparaciones (`<`, `<=`, `>`, `>=`, `==`, `!=`) entre **constantes numéricas** (literales, con la
/// promoción §5.6.2). `None` si no es plegable. Los lógicos exigen **ambos** operandos constantes
/// (§15.28): `false && x` con `x` no constante **no** es una constante (aunque valga `false`).
fn const_eval_bool(e: &Expr, consts: &ConstFieldMap) -> Option<bool> {
    Some(match &e.kind {
        ExprKind::BoolLit(b) => *b,
        ExprKind::Unary { op: UnOp::Not, expr, .. } => !const_eval_bool(expr, consts)?,
        ExprKind::Binary { op, lhs, rhs } => {
            use BinOp::*;
            match op {
                And | Or | BitAnd | BitOr | BitXor => {
                    let (a, b) = (const_eval_bool(lhs, consts)?, const_eval_bool(rhs, consts)?);
                    match op {
                        And => a && b,
                        Or => a || b,
                        BitAnd => a & b,
                        BitOr => a | b,
                        _ => a ^ b, // BitXor
                    }
                }
                // `==`/`!=` es booleano (dos `boolean` constantes) o numérico (dos números constantes).
                Eq | Ne => {
                    if let (Some(a), Some(b)) =
                        (const_eval_bool(lhs, consts), const_eval_bool(rhs, consts))
                    {
                        if matches!(op, Eq) { a == b } else { a != b }
                    } else {
                        compare_num(*op, const_eval_num(lhs, consts)?, const_eval_num(rhs, consts)?)?
                    }
                }
                Lt | Le | Gt | Ge => {
                    compare_num(*op, const_eval_num(lhs, consts)?, const_eval_num(rhs, consts)?)?
                }
                _ => return None,
            }
        }
        _ => return None,
    })
}

/// Compara dos constantes numéricas ya evaluadas con la promoción binaria (§5.6.2). Una comparación
/// con `NaN` no es plegable (`partial_cmp` da `None`): cae a runtime, donde el bytecode la resuelve
/// bien (todas `false`, salvo `!=` que es `true`).
fn compare_num(op: BinOp, a: NumV, b: NumV) -> Option<bool> {
    use std::cmp::Ordering;
    let fp = matches!(a, NumV::Double(_) | NumV::Float(_))
        || matches!(b, NumV::Double(_) | NumV::Float(_));
    let ord = if fp {
        num_to_f64(a).partial_cmp(&num_to_f64(b))?
    } else {
        num_to_i64(a).cmp(&num_to_i64(b))
    };
    Some(match op {
        BinOp::Lt => ord == Ordering::Less,
        BinOp::Le => ord != Ordering::Greater,
        BinOp::Gt => ord == Ordering::Greater,
        BinOp::Ge => ord != Ordering::Less,
        BinOp::Eq => ord == Ordering::Equal,
        BinOp::Ne => ord != Ordering::Equal,
        _ => return None,
    })
}

/// El valor de una **expresión constante booleana** de la unidad, sin resolver referencias a campos
/// (mapa vacío) — así el *flow* (que corre antes de plegar los campos) y el codegen coinciden bit a
/// bit en qué condiciones son constantes. Lo usan la reachability §14.21 y el plegado de `branch_if`.
pub(crate) fn const_bool_expr(e: &Expr) -> Option<bool> {
    const_eval_bool(e, &ConstFieldMap::new())
}

fn num_to_i64(v: NumV) -> i64 {
    match v {
        NumV::Int(x) => x as i64,
        NumV::Long(x) => x,
        NumV::Float(x) => x as i64,
        NumV::Double(x) => x as i64,
    }
}

fn num_to_f64(v: NumV) -> f64 {
    match v {
        NumV::Int(x) => x as f64,
        NumV::Long(x) => x as f64,
        NumV::Float(x) => x as f64,
        NumV::Double(x) => x,
    }
}

/// Un *cast* entre primitivos aplicado a un valor constante (§5.1.2/§5.1.3). Los estrechamientos a
/// entero angosto van por `int` primero (`f2i` satura, luego trunca), como en la JVM.
fn cast_num(ty: &Type, v: NumV) -> Option<NumV> {
    let Type::Prim(p) = ty else { return None };
    Some(match p {
        PrimType::Long => NumV::Long(num_to_i64(v)),
        PrimType::Float => NumV::Float(num_to_f64(v) as f32),
        PrimType::Double => NumV::Double(num_to_f64(v)),
        PrimType::Int => NumV::Int(num_to_i64(v) as i32),
        PrimType::Byte => NumV::Int(num_to_i64(v) as i32 as i8 as i32),
        PrimType::Short => NumV::Int(num_to_i64(v) as i32 as i16 as i32),
        PrimType::Char => NumV::Int(num_to_i64(v) as i32 as u16 as i32),
        PrimType::Boolean => return None,
    })
}

/// Un binario numérico sobre dos constantes ya evaluadas. Los shifts promocionan cada operando por
/// separado y dan el tipo del izquierdo (§15.19); el resto usa la promoción binaria (§5.6.2). Los
/// operadores no-numéricos (comparaciones, `&&`/`||`) devuelven `None` (no producen un `NumV`).
fn eval_binop_num(op: BinOp, a: NumV, b: NumV) -> Option<NumV> {
    use BinOp::*;
    if matches!(op, Shl | Shr | UShr) {
        let amt = num_to_i64(b);
        return Some(match a {
            NumV::Int(x) => {
                let s = (amt as u32) & 31;
                NumV::Int(match op {
                    Shl => x.wrapping_shl(s),
                    Shr => x.wrapping_shr(s),
                    _ => ((x as u32) >> s) as i32,
                })
            }
            NumV::Long(x) => {
                let s = (amt as u32) & 63;
                NumV::Long(match op {
                    Shl => x.wrapping_shl(s),
                    Shr => x.wrapping_shr(s),
                    _ => ((x as u64) >> s) as i64,
                })
            }
            _ => return None, // el shift solo aplica a enteros
        });
    }
    // Promoción binaria (§5.6.2): al tipo más ancho de los dos.
    let is_dbl = matches!(a, NumV::Double(_)) || matches!(b, NumV::Double(_));
    let is_flt = matches!(a, NumV::Float(_)) || matches!(b, NumV::Float(_));
    let is_lng = matches!(a, NumV::Long(_)) || matches!(b, NumV::Long(_));
    if is_dbl || is_flt {
        let (x, y) = (num_to_f64(a), num_to_f64(b));
        let r = match op {
            Add => x + y,
            Sub => x - y,
            Mul => x * y,
            Div => x / y,
            Rem => x % y,
            _ => return None, // bitwise/comparaciones no aplican a punto flotante
        };
        return Some(if is_dbl { NumV::Double(r) } else { NumV::Float(r as f32) });
    }
    if is_lng {
        let (x, y) = (num_to_i64(a), num_to_i64(b));
        return Some(NumV::Long(match op {
            Add => x.wrapping_add(y),
            Sub => x.wrapping_sub(y),
            Mul => x.wrapping_mul(y),
            Div => (y != 0).then(|| x.wrapping_div(y))?,
            Rem => (y != 0).then(|| x.wrapping_rem(y))?,
            BitAnd => x & y,
            BitOr => x | y,
            BitXor => x ^ y,
            _ => return None,
        }));
    }
    let (NumV::Int(x), NumV::Int(y)) = (a, b) else { return None };
    Some(NumV::Int(match op {
        Add => x.wrapping_add(y),
        Sub => x.wrapping_sub(y),
        Mul => x.wrapping_mul(y),
        Div => (y != 0).then(|| x.wrapping_div(y))?,
        Rem => (y != 0).then(|| x.wrapping_rem(y))?,
        BitAnd => x & y,
        BitOr => x | y,
        BitXor => x ^ y,
        _ => return None,
    }))
}

/// El valor **entero** de una expresión constante numérica (§15.29). `None` si es de punto flotante:
/// un `float`/`double` sin *cast* explícito no va a un campo entero.
fn const_eval_i64(e: &Expr, consts: &ConstFieldMap) -> Option<i64> {
    match const_eval_num(e, consts)? {
        NumV::Int(v) => Some(v as i64),
        NumV::Long(v) => Some(v),
        NumV::Float(_) | NumV::Double(_) => None,
    }
}

/// El valor de una expresión constante numérica como `f64` (para campos `float`/`double`).
fn const_eval_f64(e: &Expr, consts: &ConstFieldMap) -> Option<f64> {
    Some(num_to_f64(const_eval_num(e, consts)?))
}

/// El valor de una expresión constante de tipo **`int`** (§15.29), o `None` si no es una constante
/// **int** (una `long`/`float`/`double`, o algo no plegable). Es el predicado del **narrowing por
/// constante** del §5.2 (`byte b = 1 + 2;`): solo un `int` constante que entra en el rango del
/// destino se asigna a `byte`/`short`/`char` sin cast.
pub(crate) fn const_eval_int(e: &Expr, consts: &ConstFieldMap) -> Option<i32> {
    match const_eval_num(e, consts)? {
        NumV::Int(v) => Some(v),
        _ => None,
    }
}

/// El valor del atributo `ConstantValue` de un campo con inicializador de **expresión constante**
/// (§15.29), coercionado a su tipo declarado. Pliega literales **y** expresiones aritméticas/de bits/
/// shift (`2 + 3`, `60 * 60`, `1 << 4`, …). `None` si no es plegable —una referencia a otra `final`,
/// un `String` no literal— y entonces la init cae al `<clinit>` (correcto, solo no *inlineada*).
/// El mismo predicado lo usa el desugar para **no** bajar la init al `<clinit>` si va a `ConstantValue`.
pub(crate) fn const_field_value(field_ty: &Type, init: &Expr, consts: &ConstFieldMap) -> Option<ConstVal> {
    let p = match field_ty {
        Type::Prim(p) => *p,
        Type::Class(n) if n == "String" || n == "java.lang.String" => {
            return const_eval_str(init, consts).map(ConstVal::Str);
        }
        _ => return None,
    };
    match p {
        PrimType::Boolean => match &init.kind {
            ExprKind::BoolLit(b) => Some(ConstVal::Int(*b as i32)),
            _ => None,
        },
        // Los enteros angostos se guardan como `Integer`, truncados a su rango (§5.1.3).
        PrimType::Int => const_eval_i64(init, consts).map(|v| ConstVal::Int(v as i32)),
        PrimType::Short => const_eval_i64(init, consts).map(|v| ConstVal::Int(v as i16 as i32)),
        PrimType::Byte => const_eval_i64(init, consts).map(|v| ConstVal::Int(v as i8 as i32)),
        PrimType::Char => const_eval_i64(init, consts).map(|v| ConstVal::Int(v as u16 as i32)),
        PrimType::Long => const_eval_i64(init, consts).map(ConstVal::Long),
        PrimType::Float => const_eval_f64(init, consts).map(|v| ConstVal::Float(v as f32)),
        PrimType::Double => const_eval_f64(init, consts).map(ConstVal::Double),
    }
}

/// Construye el mapa de **campos constantes** de la unidad por *fixpoint* (§13.4.9/§15.29). Recorre
/// todos los campos `static final` de tipo numérico y, mientras alguno se pueda plegar con lo ya
/// conocido, lo agrega —así una constante que depende de otra (`B = A * 2`) se resuelve en una pasada
/// posterior—. Converge cuando una iteración completa no agrega ninguno (o quedan solo referencias a
/// campos externos / no plegables, que caen al `<clinit>`). El desugar y el codegen consultan este
/// mapa para inlinear referencias y **no** duplicar la init en el `<clinit>`.
pub(crate) fn collect_const_fields(
    unit: &super::ast::CompilationUnit,
    table: &SymbolTable,
) -> ConstFieldMap {
    // Junta `(SymbolId, tipo, init)` de cada `static final` numérico de todas las clases de la unidad.
    let mut pending: Vec<(SymbolId, &Type, &Expr)> = Vec::new();
    for class in &unit.types {
        collect_static_final_fields(class, "", table, &mut pending);
    }
    let mut map = ConstFieldMap::new();
    // Fixpoint: repetir mientras una pasada agregue algún campo nuevo. `const_field_value` pliega ya
    // coercionado al tipo del campo (numérico o `String`) y resuelve referencias contra el mapa
    // parcial, así una constante que depende de otra (`B = A * 2`, `T = S + "!"`) converge en una
    // pasada posterior. Los que nunca plegan (ref a constante externa) quedan pendientes sin efecto.
    loop {
        let mut changed = false;
        pending.retain(|&(sym, ty, init)| match const_field_value(ty, init, &map) {
            Some(v) => {
                map.insert(sym, v);
                changed = true;
                false // resuelto: sale de la lista de pendientes
            }
            None => true, // aún no plegable: reintentar en la próxima pasada
        });
        if !changed {
            break;
        }
    }
    map
}

/// Recolecta los campos `static final` de tipo **primitivo o `String`** (con su `SymbolId`, tipo
/// declarado e init) de una clase y de sus tipos anidados. El `SymbolId` de cada campo sale del scope
/// de miembros de su clase, que se resuelve por **nombre cualificado** (igual que [`gen_type`]).
fn collect_static_final_fields<'a>(
    class: &'a super::ast::ClassDecl,
    enclosing: &str,
    table: &SymbolTable,
    out: &mut Vec<(SymbolId, &'a Type, &'a Expr)>,
) {
    let fqn = if enclosing.is_empty() {
        class.name.clone()
    } else {
        format!("{enclosing}.{}", class.name)
    };
    if let Some(cid) = table.class(&fqn) {
        let scope = member_scope(table, cid);
        for member in &class.members {
            match member {
                Member::Field(f)
                    if f.modifiers.contains(&Modifier::Static)
                        && f.modifiers.contains(&Modifier::Final) =>
                {
                    if let (Some(init), Some(&sym)) =
                        (f.init.as_ref(), table.scope(scope).get(&f.name).first())
                    {
                        out.push((sym, &f.ty, init));
                    }
                }
                Member::Type(nested) => {
                    collect_static_final_fields(nested, &fqn, table, out)
                }
                _ => {}
            }
        }
    }
}

/// Si conviene un `tableswitch` sobre un `lookupswitch` para estas claves (ya ordenadas). Es la
/// heurística de javac: se comparan los costos en espacio (bytes de la instrucción) sumándoles tres
/// veces el costo en tiempo (la tabla es O(1), la búsqueda O(n) en el peor caso).
fn use_tableswitch(keys: &[(i32, usize)]) -> bool {
    let Some((&(low, _), &(high, _))) = keys.first().zip(keys.last()) else {
        return false; // solo `default`: no hay tabla que armar
    };
    let n = keys.len() as i64;
    let span = high as i64 - low as i64 + 1;
    let table = 4 + span + 3 * 3; // cabecera + una entrada por valor del rango
    let lookup = 3 + 2 * n + 3 * n; // cabecera + un par por clave
    table <= lookup
}

/// Cuántas posiciones de pila ocupa un valor de esa categoría (`long`/`double` = 2).
fn stack_width(cat: u8) -> i32 {
    if cat == 1 || cat == 3 { 2 } else { 1 }
}

struct Emitter<'a> {
    pool: &'a mut ConstantPool,
    table: &'a SymbolTable,
    /// El scope de la clase — para resolver los tipos sintácticos de un `catch`.
    scope: ScopeId,
    /// El nombre interno de **esta** clase: el tipo que toma `this` una vez inicializado.
    this_class: String,
    /// El de su superclase — para el `super()` **implícito** de una clase padre que no declara
    /// ningún constructor: ahí no hay símbolo que resolver, pero el `()V` existe igual.
    super_class: String,
    /// Construcciones que el emisor **no sabe** emitir. Se acumulan en vez de descartarse en
    /// silencio: emitir un `.class` a medias es peor que no emitir nada.
    errors: &'a mut Vec<Error>,
    /// Los *bootstrap methods* de la clase, compartidos por todos sus métodos: cada `invokedynamic`
    /// agrega el suyo y el índice que emite al pool es su posición aquí (§4.7.23).
    bootstraps: &'a mut Vec<BootstrapMethod>,
    bytes: Vec<u8>,
    /// La pila de operandos **con tipos**, no solo su altura — que es lo que un frame tiene que
    /// declarar. Llevar solo el alto fue el bug: un destino de salto alcanzado con algo ya empujado
    /// (`f(a > b ? 1 : 2)`) declaraba la pila vacía y el verificador lo rechazaba, pero recién
    /// después de emitir el `.class`.
    stack: Vec<VType>,
    max_stack: i32,
    max_locals: u16,
    /// Offset de cada etiqueta, o `None` mientras no se sabe (salto hacia adelante).
    labels: Vec<Option<usize>>,
    /// Saltos por resolver: (posición del operando de 2 bytes, offset del propio salto, etiqueta).
    fixups: Vec<(usize, usize, Label)>,
    /// Ídem para los operandos de **4 bytes** de `tableswitch`/`lookupswitch`, cuyo origen es el
    /// opcode del switch (no cada entrada).
    wide_fixups: Vec<(usize, usize, Label)>,
    /// Pila de sentencias de las que se puede **salir**: bucles, `switch` y bloques etiquetados.
    blocks: Vec<Breakable>,
    /// Pila de switch-**expresiones** abiertas: `(fin, categoría, VType del resultado)`. Un `yield`
    /// usa la del tope para ajustar su valor y saltar al fin.
    yield_targets: Vec<(Label, u8, VType)>,
    /// La etiqueta Java (`lbl:`) que precede a la sentencia que estamos por emitir. La deja el arm
    /// de `Labeled` y la consume la sentencia etiquetada al entrar.
    pending_label: Option<String>,
    /// Tipos de los locales **en este punto**, por slot (`Top` = sin asignar). La segunda mitad de
    /// un `long`/`double` queda en `Top`.
    locals_t: Vec<VType>,
    /// Estados `(locales, pila)` que llegan a cada etiqueta desde un salto, para el merge.
    pending: HashMap<Label, Vec<(Vec<VType>, Vec<VType>)>>,
    /// Los frames candidatos: offset de cada etiqueta → `(locales, pila)`.
    frames: BTreeMap<usize, (Vec<VType>, Vec<VType>)>,
    /// Etiquetas a las que **apunta algún salto**: solo esas necesitan frame. Los *handlers* de
    /// excepción también se anotan acá — no los alcanza ningún salto, pero el verificador igual
    /// necesita su frame.
    targets: HashSet<Label>,
    /// La tabla de excepciones que se irá llenando con cada `try`.
    exceptions: Vec<ExceptionEntry>,
    /// Pares `(start_pc, line)` para el `LineNumberTable` (§4.7.12): se anota la línea del fuente al
    /// entrar a cada sentencia. `mark_line` deduplica por pc y por línea.
    line_numbers: Vec<(u16, u16)>,
    /// Entradas ya cerradas del `LocalVariableTable` (§4.7.13): `(start_pc, length, name_idx,
    /// desc_idx, slot)`.
    local_vars: Vec<(u16, u16, u16, u16, u16)>,
    /// Pila de scopes de locales **abiertos**: cada scope guarda `(slot, name_idx, desc_idx,
    /// start_pc, type_annos)`; al cerrarlo, cada uno se vuelca a `local_vars` con su rango de vida
    /// `[start, pc)`, y si lleva type annotations, al `RuntimeVisibleTypeAnnotations` del `Code` como
    /// target `0x40`. La estructura de scopes **espeja** la de la pasada 2 (que reusa slots al salir de
    /// un bloque), para que dos locales que comparten slot no den rangos solapados.
    open_locals: Vec<Vec<(u16, u16, u16, u16, Vec<TypeUseAnnot>)>>,
    /// Si el código puede **caer** hasta el punto actual (falso tras un `goto`/`return`).
    reachable: bool,
    /// Anotaciones de tipo **retenidas en runtime** (por nombre simple): filtro para las de posición-Code.
    rt: &'a std::collections::HashSet<String>,
    /// Clasificación TYPE_USE de las anotaciones (por `@Target`): para un local, solo las de tipo van al
    /// target `0x40`; una anotación de declaración pura (`@Deprecated int x`) en esa posición **no**.
    tu: &'a TypeUseInfo,
    /// Entradas del `RuntimeVisibleTypeAnnotations` que va **dentro del `Code`** (§4.7.20): los targets
    /// de posición-bytecode (cast 0x47, `instanceof` 0x43, `new` 0x44, local 0x40), cada uno con el
    /// offset del opcode como `target_info`. Se llenan al emitir cada instrucción.
    code_type_annotations: Vec<Vec<u8>>,
    /// Los bloques `finally` que **encierran** el punto actual (el más interno al final). Un
    /// `return` que sale de un `try` corre estos bloques antes de retornar (§14.20.2): la v69 no
    /// tiene `jsr`/`ret`, así que el `finally` se **inyecta** en cada salida abrupta, igual que en la
    /// salida normal. Cada entrada acumula los `gaps` —las copias inline que corren en sus salidas
    /// abruptas— para recortarlos de la región protegida en la tabla de excepciones.
    finally_stack: Vec<PendingExit>,
    /// Cursor **dinámico** del primer slot libre (`nextreg` de javac): el máximo `slot + ancho` de los
    /// locales vivos en el punto actual. Sube al declarar un local y se restaura al cerrar el scope.
    /// De acá sale el temporal del `return` (justo en `next-free`, no por encima de todo el método) y
    /// la base sobre la que se reubican los locales del `finally` en cada copia inline.
    next_free: u16,
    /// `next_free` guardado al abrir cada scope, para restaurarlo al cerrarlo (los slots de un bloque
    /// se liberan al salir y los reusa el bloque hermano).
    scope_next_free: Vec<u16>,
    /// Reubicación de slots activa mientras se emite una copia inline de un `finally`: `(base, delta)`
    /// desplaza todo slot `>= base` por `delta`. Fuera de esa emisión es `None` (identidad).
    slot_remap: Option<(u16, i32)>,
    /// Categoría (0=int-fam,1=long,2=float,3=double,4=ref) del tipo de retorno del método
    /// en curso: cada `return e` convierte `e` a ella (contexto de asignación §5.2).
    ret_cat: u8,
    /// Base de la región reservada para los `synchronized`: el `next_free` inicial (tras `this` y los
    /// parámetros, = el `nextreg` de javac al entrar al método). Los monitores van en
    /// `sync_base + profundidad` y los aparcaderos de excepción por encima de todos ellos; el cursor
    /// dinámico `next_free` arranca por **encima** de esta región para no pisarla.
    sync_base: u16,
    /// Cuántos `synchronized` están **abiertos** en el punto actual: da el slot del monitor de uno
    /// nuevo (`sync_base + sync_depth`) y, con `sync_max_depth`, el de su aparcadero de excepción.
    sync_depth: u16,
    /// Anidamiento máximo de `synchronized` del método, para dimensionar la región reservada.
    sync_max_depth: u16,
}

/// Una salida **pendiente** que encierra el punto actual: un bloque `finally` (con los `gaps` que sus
/// copias inline dejan en la región protegida, para recortar la tabla de excepciones) o el
/// `monitorexit` de un `synchronized` (con el slot del local que guarda la referencia bloqueada). Una
/// salida abrupta —`return`, `break`/`continue` que sale— corre las que cruza antes de saltar
/// (§14.20.2), igual que en la salida normal: un `synchronized` es, a estos efectos, un
/// `try { monitorenter; body } finally { monitorexit }`.
#[derive(Clone)]
enum PendingExit {
    Finally { block: Block, gaps: Vec<(usize, usize)> },
    Monitor(u16),
}

/// Un destino de salto todavía sin dirección: se resuelve al final, parcheando los operandos.
type Label = usize;

/// Una sentencia de la que se puede **salir**: un bucle, un `switch` o un bloque etiquetado.
///
/// `break` sin etiqueta va a la más interna (sea bucle o `switch`); `continue` sin etiqueta, al
/// bucle más interno — de ahí que `cont` sea opcional: un `switch` interpuesto **no** captura un
/// `continue` (§14.16). Con etiqueta, se busca la que la lleve.
struct Breakable {
    label: Option<String>,
    brk: Label,
    /// El destino de un `continue` — `None` si no es un bucle.
    cont: Option<Label>,
    /// Profundidad del `finally_stack` cuando se abrió esta sentencia. Un `break`/`continue` que
    /// salta acá cruza —y por lo tanto debe correr— los `finally` que se apilaron **después** (§14.20.2).
    finally_depth: usize,
}

impl<'a> Emitter<'a> {
    fn new(
        pool: &'a mut ConstantPool,
        table: &'a SymbolTable,
        scope: ScopeId,
        this_class: String,
        super_class: String,
        bootstraps: &'a mut Vec<BootstrapMethod>,
        errors: &'a mut Vec<Error>,
        rt: &'a std::collections::HashSet<String>,
        tu: &'a TypeUseInfo,
    ) -> Self {
        Emitter {
            pool,
            table,
            scope,
            this_class,
            super_class,
            errors,
            bootstraps,
            rt,
            tu,
            code_type_annotations: Vec::new(),
            finally_stack: Vec::new(),
            next_free: 0,
            scope_next_free: Vec::new(),
            sync_base: 0,
            sync_depth: 0,
            sync_max_depth: 0,
            slot_remap: None,
            ret_cat: 0,
            bytes: Vec::new(),
            stack: Vec::new(),
            max_stack: 0,
            max_locals: 0,
            labels: Vec::new(),
            fixups: Vec::new(),
            wide_fixups: Vec::new(),
            blocks: Vec::new(),
            yield_targets: Vec::new(),
            pending_label: None,
            locals_t: Vec::new(),
            pending: HashMap::new(),
            frames: BTreeMap::new(),
            targets: HashSet::new(),
            exceptions: Vec::new(),
            line_numbers: Vec::new(),
            local_vars: Vec::new(),
            open_locals: Vec::new(),
            reachable: true,
        }
    }

    // ---- LocalVariableTable (§4.7.13): scopes de variables locales ----

    /// Aplica la reubicación de slots activa (dentro de una copia inline de `finally`): todo slot por
    /// encima de la base del `finally` se corre por el delta; el resto (—`this`, parámetros, locales de
    /// afuera—) queda igual. Fuera de esa emisión es la identidad.
    fn rmap(&self, slot: u16) -> u16 {
        match self.slot_remap {
            Some((base, delta)) if slot >= base => (slot as i32 + delta) as u16,
            _ => slot,
        }
    }

    /// Registra que un local vive en `slot` (ya reubicado): sube el cursor `next_free` para que el
    /// próximo temporal/local no lo pise.
    fn note_local(&mut self, slot: u16, width: u16) {
        self.next_free = self.next_free.max(slot + width);
    }

    /// Abre un scope de locales (espejo de un `env.push()` de la pasada 2).
    fn open_scope(&mut self) {
        self.open_locals.push(Vec::new());
        self.scope_next_free.push(self.next_free);
    }

    /// Cierra el scope corriente: cada local abierto se vuelca a `local_vars` con su rango de vida
    /// `[start, pc_actual)` (se descartan los de largo 0: se declararon pero no vivieron sobre código).
    fn close_scope(&mut self) {
        if let Some(saved) = self.scope_next_free.pop() {
            self.next_free = saved;
        }
        let pc = self.bytes.len() as u16;
        if let Some(scope) = self.open_locals.pop() {
            for (slot, name, desc, start, type_annos) in scope {
                if pc > start {
                    self.local_vars.push((start, pc - start, name, desc, slot));
                    // `LOCAL_VARIABLE` (target 0x40): el `target_info` es la tabla de rangos de vida
                    // `{ start_pc, length, index }` (aquí un único rango), y el offset **no** entra —
                    // el rango sí. Solo las de tipo (`@Target(TYPE_USE)`) y retenidas en runtime.
                    for ta in &type_annos {
                        let simple = anno_simple(&ta.annotation);
                        if !self.tu.type_use.contains(simple) || !self.rt.contains(simple) {
                            continue;
                        }
                        if let Some(ann) =
                            encode_annotation(self.pool, self.table, self.scope, &ta.annotation)
                        {
                            let mut ti = Vec::with_capacity(8);
                            ti.extend_from_slice(&1u16.to_be_bytes()); // table_length = 1
                            ti.extend_from_slice(&start.to_be_bytes());
                            ti.extend_from_slice(&(pc - start).to_be_bytes());
                            ti.extend_from_slice(&slot.to_be_bytes());
                            let path = serialize_type_path(&ta.path);
                            self.code_type_annotations
                                .push(type_annotation_entry(0x40, &ti, &path, &ann));
                        }
                    }
                }
            }
        }
    }

    /// Registra una variable local, viva desde el offset actual, en el scope corriente. `type_annos`
    /// son las anotaciones de tipo del local (target 0x40); vacío para `this`, parámetros y `catch`.
    fn open_local(&mut self, slot: u16, name: &str, desc: &str, type_annos: &[TypeUseAnnot]) {
        let slot = self.rmap(slot);
        // Ancho del local: un `long`/`double` ocupa dos slots (una sola entrada, pero el cursor sube 2).
        let width = if matches!(desc.as_bytes().first(), Some(b'J') | Some(b'D')) { 2 } else { 1 };
        self.note_local(slot, width);
        let name_idx = self.pool.utf8(name);
        let desc_idx = self.pool.utf8(desc);
        let start = self.bytes.len() as u16;
        if let Some(scope) = self.open_locals.last_mut() {
            scope.push((slot, name_idx, desc_idx, start, type_annos.to_vec()));
        }
    }

    /// Emite una secuencia de sentencias como un **bloque con scope** de locales.
    fn block_scoped(&mut self, stmts: &[Stmt]) {
        self.open_scope();
        for s in stmts {
            self.stmt(s);
        }
        self.close_scope();
    }

    /// Anota, para el `LineNumberTable`, que en el offset actual arranca la línea `line` del fuente.
    /// Deduplica: si el último par ya está en este mismo pc, lo **reemplaza** (la sentencia previa no
    /// emitió código); si ya está en esta misma línea, no repite. Ignora `line == 0` (sin posición).
    fn mark_line(&mut self, line: u32) {
        if line == 0 {
            return;
        }
        let pc = self.bytes.len() as u16;
        let line = line as u16;
        match self.line_numbers.last_mut() {
            Some((last_pc, last_line)) if *last_pc == pc => *last_line = line,
            Some((_, last_line)) if *last_line == line => {}
            _ => self.line_numbers.push((pc, line)),
        }
    }

    // ---- etiquetas y saltos ----

    fn new_label(&mut self) -> Label {
        self.labels.push(None);
        self.labels.len() - 1
    }

    /// Anota el tipo de un local (y deja en `Top` la segunda mitad de un `long`/`double`).
    fn set_local(&mut self, slot: u16, t: VType) {
        let slot = self.rmap(slot);
        let i = slot as usize;
        if self.locals_t.len() < i + 2 {
            self.locals_t.resize(i + 2, VType::Top);
        }
        let wide = matches!(t, VType::Long | VType::Double);
        self.locals_t[i] = t;
        if wide {
            self.locals_t[i + 1] = VType::Top;
        }
    }

    /// Fija la etiqueta acá y calcula el **frame** del destino: el merge de los estados que llegan
    /// por cada salto, más el de caída si el código anterior podía llegar hasta acá.
    ///
    /// La pila que se declara es la **real** en este punto, no una que le pasen: casi siempre está
    /// vacía porque los destinos caen en bordes de sentencia, pero un ternario en posición de
    /// argumento (`f(a, b > 0 ? 1 : 2)`) llega acá con lo anterior ya empujado.
    fn bind(&mut self, l: Label) {
        let off = self.bytes.len();
        self.labels[l] = Some(off);
        let mut states = self.pending.remove(&l).unwrap_or_default();
        if self.reachable {
            states.push((self.locals_t.clone(), self.stack.clone()));
        }
        if let Some((locals, st)) = merge_states(&states) {
            self.locals_t = locals.clone();
            self.stack = st.clone();
            // Se guarda el frame de **toda** etiqueta; cuáles hacen falta se decide al final: un
            // salto **hacia atrás** (el del bucle) se emite después de fijar su destino, así que acá
            // todavía no se sabe si alguien le apunta.
            self.frames.insert(off, (locals, st));
        }
        // A un destino de salto se llega salvo que nadie lo apunte y el camino de caída esté cortado
        // (un bloque etiquetado cuyo cuerpo siempre retorna): ahí lo que siga es código muerto.
        self.reachable = !states.is_empty();
    }

    /// Emite un salto con el operando en blanco (se parchea al final) y anota el estado que llega
    /// al destino por este camino.
    fn jump(&mut self, op: u8, l: Label) {
        let at = self.bytes.len();
        self.op(op);
        let operand = self.bytes.len();
        self.u16(0);
        self.fixups.push((operand, at, l));
        self.arrives(l);
    }

    /// Una entrada de la tabla de un `tableswitch`/`lookupswitch`: un desplazamiento de **4 bytes**
    /// relativo al opcode del switch (`base`), no a la propia entrada.
    fn jump4(&mut self, base: usize, l: Label) {
        let operand = self.bytes.len();
        self.bytes.extend_from_slice(&0u32.to_be_bytes());
        self.wide_fixups.push((operand, base, l));
        self.arrives(l);
    }

    /// Anota que a `l` se llega con el estado actual (y que, por lo tanto, necesita frame).
    fn arrives(&mut self, l: Label) {
        self.targets.insert(l);
        self.pending.entry(l).or_default().push((self.locals_t.clone(), self.stack.clone()));
    }

    /// Serializa el `StackMapTable` (JVMS §4.7.4) eligiendo la forma **más compacta** de cada frame
    /// según el anterior: `same_frame`/`same_frame_extended` (locales iguales, pila vacía),
    /// `same_locals_1_stack_item` (un ítem de pila), `append`/`chop` (1–3 locales de más/de menos al
    /// final), o `full_frame` cuando no encaja ninguna. El `offset_delta` del primer frame es su
    /// offset; los siguientes van **menos uno** respecto del anterior (así un delta 0 sigue avanzando).
    fn stack_map(&mut self) -> Option<Vec<u8>> {
        // Solo los offsets a los que **apunta algún salto** (ya resueltos todos, incluidos los de
        // vuelta atrás de los bucles).
        let wanted: HashSet<usize> =
            self.targets.iter().filter_map(|&l| self.labels[l]).collect();
        let frames: Vec<(usize, Vec<VType>, Vec<VType>)> = self
            .frames
            .iter()
            .filter(|(off, _)| wanted.contains(off))
            .map(|(off, (l, s))| (*off, Self::frame_locals(l), s.clone()))
            .collect();
        if frames.is_empty() {
            return None;
        }
        let mut out = Vec::new();
        out.extend_from_slice(&(frames.len() as u16).to_be_bytes());
        let mut prev_off: Option<usize> = None;
        // El primer frame no tiene un frame previo **en la tabla** contra el que comprimir (compararlo
        // con el frame inicial del método daría algún byte más, pero pide reconstruir sus locales): va
        // como `full_frame`. El resto se comprime contra su antecesor.
        let mut prev_locals: Option<Vec<VType>> = None;
        for (off, locals, stack) in frames {
            let delta = match prev_off {
                None => off,
                Some(p) => off - p - 1,
            } as u16;
            prev_off = Some(off);
            self.write_frame(&mut out, delta, &locals, &stack, prev_locals.as_deref());
            prev_locals = Some(locals);
        }
        Some(out)
    }

    /// Escribe un frame en la forma **más compacta** válida respecto de `prev` (los locales del frame
    /// anterior), cayendo a `full_frame` (tag 255) cuando ninguna comprimida aplica.
    fn write_frame(
        &mut self,
        out: &mut Vec<u8>,
        delta: u16,
        locals: &[VType],
        stack: &[VType],
        prev: Option<&[VType]>,
    ) {
        if let Some(prev) = prev {
            if stack.is_empty() {
                if locals == prev {
                    if delta <= 63 {
                        out.push(delta as u8); // same_frame (0–63)
                    } else {
                        out.push(251); // same_frame_extended
                        out.extend_from_slice(&delta.to_be_bytes());
                    }
                    return;
                }
                // `append_frame` (252–254): `prev` seguido de 1–3 locales nuevos.
                if locals.len() > prev.len()
                    && locals.len() - prev.len() <= 3
                    && &locals[..prev.len()] == prev
                {
                    let k = locals.len() - prev.len();
                    out.push((251 + k) as u8);
                    out.extend_from_slice(&delta.to_be_bytes());
                    for t in &locals[prev.len()..] {
                        self.write_vtype(out, t);
                    }
                    return;
                }
                // `chop_frame` (248–250): `prev` con 1–3 locales **de menos** al final.
                if prev.len() > locals.len()
                    && prev.len() - locals.len() <= 3
                    && &prev[..locals.len()] == locals
                {
                    let k = prev.len() - locals.len();
                    out.push((251 - k) as u8);
                    out.extend_from_slice(&delta.to_be_bytes());
                    return;
                }
            } else if stack.len() == 1 && locals == prev {
                // `same_locals_1_stack_item_frame` (64–127) / su forma _extended (247).
                if delta <= 63 {
                    out.push(64 + delta as u8);
                } else {
                    out.push(247);
                    out.extend_from_slice(&delta.to_be_bytes());
                }
                self.write_vtype(out, &stack[0]);
                return;
            }
        }
        // full_frame (255): todo explícito.
        out.push(255);
        out.extend_from_slice(&delta.to_be_bytes());
        out.extend_from_slice(&(locals.len() as u16).to_be_bytes());
        for t in locals {
            self.write_vtype(out, t);
        }
        out.extend_from_slice(&(stack.len() as u16).to_be_bytes());
        for t in stack {
            self.write_vtype(out, t);
        }
    }

    fn write_vtype(&mut self, out: &mut Vec<u8>, t: &VType) {
        match t {
            VType::Top => out.push(0),
            VType::Int => out.push(1),
            VType::Float => out.push(2),
            VType::Double => out.push(3),
            VType::Long => out.push(4),
            VType::UninitThis => out.push(6),
            VType::Object(name) => {
                out.push(7);
                let idx = self.pool.class(name);
                out.extend_from_slice(&idx.to_be_bytes());
            }
            // No lleva el tipo sino el **offset de su `new`**: así el verificador distingue dos
            // objetos sin inicializar de la misma clase vivos a la vez.
            VType::Uninit(off) => {
                out.push(8);
                out.extend_from_slice(&(*off as u16).to_be_bytes());
            }
        }
    }

    /// La lista de locales de un frame: **una entrada por variable** (un `long`/`double` ocupa dos
    /// slots pero se escribe una sola vez), y sin los `Top` del final (el verificador los asume).
    fn frame_locals(locals: &[VType]) -> Vec<VType> {
        let mut end = locals.len();
        while end > 0 && locals[end - 1] == VType::Top {
            end -= 1;
        }
        let mut out = Vec::new();
        let mut i = 0;
        while i < end {
            let t = locals[i].clone();
            i += if matches!(t, VType::Long | VType::Double) { 2 } else { 1 };
            out.push(t);
        }
        out
    }

    /// Resuelve todos los saltos. El desplazamiento es **relativo al propio salto** (JVMS §6).
    fn patch(&mut self) {
        // Paso 1: escribir cada salto con su destino **directo**. Así los `goto` ya llevan su offset
        // real en el buffer y el paso 2 puede seguirlos.
        for &(operand, at, l) in &self.fixups {
            let target = self.labels[l].expect("etiqueta sin fijar");
            let bytes = ((target as i32 - at as i32) as i16).to_be_bytes();
            self.bytes[operand] = bytes[0];
            self.bytes[operand + 1] = bytes[1];
        }
        for &(operand, base, l) in &self.wide_fixups {
            let target = self.labels[l].expect("etiqueta sin fijar");
            let delta = (target as i64 - base as i64) as i32;
            self.bytes[operand..operand + 4].copy_from_slice(&delta.to_be_bytes());
        }
        // Paso 2: encadenar los saltos cuyo destino es un `goto` hasta su destino final (fiel a
        // javac). Solo cambia el **operando** de cada salto, nunca la posición de una instrucción.
        for &(operand, at, l) in &self.fixups {
            let target = self.chase_goto(self.labels[l].expect("etiqueta sin fijar"));
            let bytes = ((target as i32 - at as i32) as i16).to_be_bytes();
            self.bytes[operand] = bytes[0];
            self.bytes[operand + 1] = bytes[1];
        }
        for &(operand, base, l) in &self.wide_fixups {
            let target = self.chase_goto(self.labels[l].expect("etiqueta sin fijar"));
            let delta = (target as i64 - base as i64) as i32;
            self.bytes[operand..operand + 4].copy_from_slice(&delta.to_be_bytes());
        }
    }

    /// Encadena un salto cuyo destino es un `goto` hasta su destino **final** (fiel al `Code.resolve`
    /// de javac: saltar a un `goto L` equivale a saltar a `L`). Preserva la semántica —un `goto` no
    /// toca la pila— y evita el rebote `ifeq→goto→destino` que javac colapsa a `ifeq destino`. El
    /// tope de iteraciones corta un ciclo de `goto`s (código muerto que la práctica no produce).
    fn chase_goto(&self, mut target: usize) -> usize {
        for _ in 0..self.bytes.len() {
            if self.bytes.get(target) != Some(&GOTO) {
                break;
            }
            let delta =
                i16::from_be_bytes([self.bytes[target + 1], self.bytes[target + 2]]) as i32;
            let next = (target as i32 + delta) as usize;
            if next == target {
                break; // `goto` a sí mismo: no encadenar
            }
            target = next;
        }
        target
    }

    fn op(&mut self, b: u8) {
        self.bytes.push(b);
    }
    fn u16(&mut self, v: u16) {
        self.bytes.extend_from_slice(&v.to_be_bytes());
    }
    fn u32(&mut self, v: i32) {
        self.bytes.extend_from_slice(&v.to_be_bytes());
    }
    /// Empuja un valor a la pila de operandos y recuerda el máximo (para `max_stack`).
    fn push(&mut self, t: VType) {
        self.stack.push(t);
        self.max_stack = self.max_stack.max(self.height());
    }

    /// Empuja un valor por su **categoría** — cuando el opcode ya fijó qué deja y solo importa su
    /// ancho (el resultado de una aritmética, una conversión).
    fn push_cat(&mut self, cat: u8) {
        self.push(match cat {
            1 => VType::Long,
            2 => VType::Float,
            3 => VType::Double,
            4 => VType::Object("java/lang/Object".to_string()),
            _ => VType::Int,
        });
    }

    /// Saca `n` **valores** (no slots: un `long` es uno solo).
    fn pop(&mut self, n: usize) {
        let keep = self.stack.len().saturating_sub(n);
        self.stack.truncate(keep);
    }

    /// La altura en **slots**: lo que va al `max_stack` del atributo `Code`.
    fn height(&self) -> i32 {
        self.stack.iter().map(|t| if matches!(t, VType::Long | VType::Double) { 2 } else { 1 }).sum()
    }

    /// El `this` de un constructor pasa de `UninitThis` a su tipo definitivo cuando corre el
    /// `super(...)`/`this(...)`. Hasta ese momento el verificador prohíbe usarlo para cualquier
    /// otra cosa (§4.10.2.4), que es justo lo que hace ilegal leer un campo antes de inicializar.
    fn init_this(&mut self) {
        let done = VType::Object(self.this_class.clone());
        for t in self.stack.iter_mut().chain(self.locals_t.iter_mut()) {
            if *t == VType::UninitThis {
                *t = done.clone();
            }
        }
    }

    /// Sustituye un objeto **sin inicializar** por su tipo definitivo en todas partes —pila y
    /// locales— justo después de que corra su `<init>` (JVMS §4.10.2.4). El `dup` previo al
    /// `invokespecial` deja dos copias del mismo `new`, y las dos tienen que quedar inicializadas.
    fn initialized(&mut self, at: usize, class: &str) {
        let done = VType::Object(class.to_string());
        for t in self.stack.iter_mut().chain(self.locals_t.iter_mut()) {
            if *t == VType::Uninit(at) {
                *t = done.clone();
            }
        }
    }
    fn use_slot(&mut self, slot: u16, width: i32) {
        self.max_locals = self.max_locals.max(slot + width as u16);
    }

    /// Registra que el emisor **no soporta** algo, con su posición. Emitir bytecode a medias por una
    /// construcción no cubierta es la clase de bug que ya nos mordió dos veces (el `if` descartado y
    /// el `++` en posición de valor): mejor fallar fuerte.
    fn unsupported(&mut self, pos: Pos, what: &str) {
        self.errors.push(Error::new(
            format!("el generador de bytecode todavía no soporta {what}"),
            pos.line,
            pos.col,
        ));
    }

    fn ty_of(&self, e: &Expr) -> RType {
        e.ty.clone().unwrap_or(RType::Prim(PrimType::Int))
    }

    // ---- sentencias ----

    fn stmt(&mut self, s: &Stmt) {
        // `LineNumberTable` (§4.7.12): al entrar a la sentencia, mapear el offset actual a su línea.
        // Un `Block` no aporta línea propia (delega en sus hijas); el resto sí.
        if !matches!(s.kind, StmtKind::Block(_)) {
            self.mark_line(s.pos.line);
        }
        // La etiqueta que dejó un `Labeled` es de **esta** sentencia; se consume al entrar para que
        // no se filtre a una anidada.
        let lbl = self.pending_label.take();
        match &s.kind {
            StmtKind::Block(b) => self.block_scoped(&b.0),
            StmtKind::LocalVar { name, init, type_annos, .. } => {
                if let Some(local) = &s.local {
                    if let Some(e) = init {
                        let cat = category(&local.ty);
                        // Conversión en contexto de asignación (§5.2): un inicializador de una categoría
                        // numérica más angosta se **ensancha** al tipo de la variable. Una **constante**
                        // se pliega directo al tipo destino (`double s = 0` ⇒ `dconst_0`, como javac); un
                        // valor no constante se emite y se convierte (`double s = i` ⇒ `iload; i2d`).
                        let from = category(&self.ty_of(e));
                        if from != cat && from <= 3 && cat <= 3 {
                            match const_eval_num(e, &ConstFieldMap::new()) {
                                Some(n) => self.push_num_as(n, cat),
                                None => {
                                    self.expr(e);
                                    self.convert(from, cat);
                                }
                            }
                        } else {
                            self.expr(e);
                        }
                        let vt = vtype_of(self.table, &local.ty);
                        self.set_local(local.slot, vt);
                        self.store(cat, local.slot);
                    }
                    // `LocalVariableTable`: la variable vive desde acá hasta el fin de su bloque. Y sus
                    // type annotations (`@A int x`) van al `Code` como target 0x40 con ese rango.
                    let desc = rtype_desc(self.table, &local.ty);
                    self.open_local(local.slot, name, &desc, type_annos);
                }
            }
            StmtKind::Return(e) => {
                match e {
                    Some(e) => {
                        let from = category(&self.ty_of(e));
                        self.expr(e);
                        // Conversión al tipo de retorno (§5.2): p. ej. `f2d` si el `return` de un
                        // método `double` rinde un `float`. `convert` es no-op si ya coinciden o si
                        // alguno es referencia.
                        self.convert(from, self.ret_cat);
                        let cat = self.ret_cat;
                        if self.finally_stack.is_empty() {
                            self.op(IRETURN + cat);
                            self.pop(1);
                        } else if !self.pending_has_finally() {
                            // Solo `monitorexit` pendientes (ningún `finally`): el valor puede quedar
                            // en la pila **bajo** la referencia del monitor mientras se sueltan los
                            // monitores (§14.19) —cada `aload lock; monitorexit` no lo toca—, sin
                            // aparcarlo en un temporal. Byte-fiel a javac.
                            let starts = self.run_finallys_down_to(0, self.next_free);
                            self.op(IRETURN + cat);
                            self.pop(1);
                            self.close_gaps(starts);
                        } else {
                            // Salida **abrupta** de un `try` (§14.20.2): el valor no puede quedar en la
                            // pila mientras corre el `finally` (que la usa), así que se guarda en un
                            // temporal —en el **next-free** vivo, como javac—, se corren los `finally`
                            // pendientes (con sus locales reubicados por encima de ese temporal), y se
                            // recarga antes del `ireturn`.
                            let tmp = self.next_free;
                            let vt = self.stack.last().cloned().unwrap_or(VType::Top);
                            self.set_local(tmp, vt);
                            self.store(cat, tmp);
                            let rebase_top = tmp + stack_width(cat) as u16;
                            let saved = self.next_free;
                            self.next_free = rebase_top; // el temporal ocupa [tmp, rebase_top)
                            let starts = self.run_finallys_down_to(0, rebase_top);
                            self.next_free = saved;
                            self.load(cat, tmp);
                            self.op(IRETURN + cat);
                            self.pop(1);
                            self.close_gaps(starts);
                        }
                    }
                    None => {
                        // `return;` sin valor: no hay temporal, los `finally` se reubican en el
                        // next-free vivo.
                        let starts = self.run_finallys_down_to(0, self.next_free);
                        self.op(RETURN);
                        self.close_gaps(starts);
                    }
                }
                self.reachable = false; // lo que siga solo se alcanza por un salto
            }
            StmtKind::Expr(e) => self.discard(e),
            StmtKind::If { cond, then, els } => {
                let otherwise = self.new_label();
                self.branch_if(cond, otherwise, false); // condición falsa ⇒ saltar el `then`
                self.stmt(then);
                match els {
                    Some(e) => {
                        let end = self.new_label();
                        self.jump(GOTO, end);
                        self.reachable = false;
                        self.bind(otherwise);
                        self.stmt(e);
                        self.bind(end);
                    }
                    None => self.bind(otherwise),
                }
            }
            StmtKind::While { cond, body } => {
                let top = self.new_label();
                let end = self.new_label();
                self.bind(top);
                self.branch_if(cond, end, false);
                self.blocks.push(Breakable { label: lbl, brk: end, cont: Some(top), finally_depth: self.finally_stack.len() });
                self.stmt(body);
                self.blocks.pop();
                self.jump(GOTO, top);
                self.reachable = false;
                self.bind(end);
            }
            StmtKind::Do { body, cond } => {
                let top = self.new_label();
                let cont = self.new_label();
                let end = self.new_label();
                self.bind(top);
                self.blocks.push(Breakable { label: lbl, brk: end, cont: Some(cont), finally_depth: self.finally_stack.len() });
                self.stmt(body);
                self.blocks.pop();
                self.bind(cont); // un `continue` va a reevaluar la condición
                self.branch_if(cond, top, true);
                self.bind(end);
            }
            StmtKind::For { init, cond, update, body } => {
                // Scope para la variable del `init` (§14.14.1): su slot se libera al salir del `for`,
                // así que su rango en el `LocalVariableTable` no debe pasarse de acá (espeja la pasada 2).
                self.open_scope();
                if let Some(i) = init {
                    self.stmt(i);
                }
                let top = self.new_label();
                let cont = self.new_label(); // el `continue` salta al `update`, no al `cond`
                let end = self.new_label();
                self.bind(top);
                if let Some(c) = cond {
                    self.branch_if(c, end, false);
                }
                self.blocks.push(Breakable { label: lbl, brk: end, cont: Some(cont), finally_depth: self.finally_stack.len() });
                self.stmt(body);
                self.blocks.pop();
                self.bind(cont);
                for u in update {
                    self.discard(u);
                }
                self.jump(GOTO, top);
                self.reachable = false;
                self.bind(end);
                self.close_scope(); // cierra el scope de la variable del `for`
            }
            StmtKind::Break(label) => match self.break_target(label.as_deref()) {
                Some((t, depth)) => {
                    // §14.20.2: correr los `finally` que el salto cruza (reubicados en el next-free vivo).
                    let starts = self.run_finallys_down_to(depth, self.next_free);
                    self.jump(GOTO, t);
                    self.reachable = false;
                    self.close_gaps(starts);
                }
                None => self.unsupported(s.pos, "un `break` sin destino (¿lo dejó pasar el flujo?)"),
            },
            StmtKind::Continue(label) => match self.continue_target(label.as_deref()) {
                Some((t, depth)) => {
                    let starts = self.run_finallys_down_to(depth, self.next_free);
                    self.jump(GOTO, t);
                    self.reachable = false;
                    self.close_gaps(starts);
                }
                None => {
                    self.unsupported(s.pos, "un `continue` sin destino (¿lo dejó pasar el flujo?)")
                }
            },
            StmtKind::Switch { selector, cases } => self.switch_stmt(s, selector, cases, lbl),
            StmtKind::Synchronized { lock, body } => self.sync_stmt(s, lock, body),
            StmtKind::Try { body, catches, finally, .. } => self.try_stmt(body, catches, finally),
            StmtKind::Throw(e) => {
                self.expr(e);
                self.op(ATHROW);
                self.pop(1);
                self.reachable = false;
            }
            // Sobre un bucle o un `switch`, la etiqueta se la queda **esa** sentencia: así un
            // `continue lbl` encuentra el encabezado al que volver. Sobre cualquier otra, es un
            // **bloque etiquetado**, del que solo se sale con `break lbl` (§14.7).
            StmtKind::Labeled { label, body } => {
                if matches!(
                    body.kind,
                    StmtKind::While { .. }
                        | StmtKind::Do { .. }
                        | StmtKind::For { .. }
                        | StmtKind::Switch { .. }
                ) {
                    self.pending_label = Some(label.clone());
                    self.stmt(body);
                } else {
                    let end = self.new_label();
                    self.blocks.push(Breakable {
                        label: Some(label.clone()),
                        brk: end,
                        cont: None,
                        finally_depth: self.finally_stack.len(),
                    });
                    self.stmt(body);
                    self.blocks.pop();
                    self.bind(end);
                }
            }
            StmtKind::Empty => {}
            StmtKind::LocalClass(_) => {
                self.unsupported(s.pos, "una clase local (necesita una clase sintética)")
            }
            // `yield e` de una switch-expresión embebida (las lowerable las baja el desugar a
            // asignaciones): deja el valor en la pila y salta al fin del switch más interno.
            StmtKind::Yield(e) => self.yield_value(e, false),
            // Estas tendrían que haber desaparecido en el desugar: si llegan acá es un bug de esa pasada.
            StmtKind::ForEach { .. } | StmtKind::Assert { .. } => {
                self.unsupported(s.pos, "una construcción que el desugar debía haber bajado")
            }
        }
    }

    /// El destino de un `break`: la sentencia más interna de la que se puede salir, o la que lleve
    /// la etiqueta pedida.
    fn break_target(&self, label: Option<&str>) -> Option<(Label, usize)> {
        match label {
            None => self.blocks.last().map(|b| (b.brk, b.finally_depth)),
            Some(l) => self
                .blocks
                .iter()
                .rev()
                .find(|b| b.label.as_deref() == Some(l))
                .map(|b| (b.brk, b.finally_depth)),
        }
    }

    /// El destino de un `continue`: el **bucle** más interno (un `switch` interpuesto no cuenta), o
    /// el que lleve la etiqueta pedida.
    fn continue_target(&self, label: Option<&str>) -> Option<(Label, usize)> {
        self.blocks
            .iter()
            .rev()
            .find(|b| b.cont.is_some() && label.is_none_or(|l| b.label.as_deref() == Some(l)))
            .map(|b| (b.cont.unwrap(), b.finally_depth))
    }

    /// `switch` como **sentencia**, ya reducido a un selector `int`: los de `String`, `enum` y con
    /// *patterns* los bajó el desugar, así que acá solo llegan etiquetas constantes.
    ///
    /// Hay dos instrucciones de salto múltiple (JVMS §6), y se elige entre ellas por **densidad**:
    /// - `tableswitch`: el selector indexa directo en una tabla `[low, high]` — O(1), pero gasta una
    ///   entrada por cada valor **ausente** del rango (`case 1, 1000` reservaría mil).
    /// - `lookupswitch`: pares `(clave, destino)` **ordenados**, que la JVM busca binariamente —
    ///   O(log n), sin desperdicio.
    ///
    /// El criterio es el de javac: la tabla gana si su costo en espacio, más tres veces su ventaja
    /// en tiempo, sale mejor. Las dos instrucciones llevan **relleno** hasta alinear a 4 bytes desde
    /// el arranque del método, y sus desplazamientos son de **4 bytes** relativos al opcode.
    fn emit_switch_dispatch(
        &mut self,
        pos: Pos,
        selector: &Expr,
        cases: &[SwitchCase],
    ) -> Option<(Vec<Label>, Label, Label)> {
        if category(&self.ty_of(selector)) != 0 {
            self.unsupported(pos, "un `switch` cuyo selector no es `int` (lo baja el desugar)");
            return None;
        }
        // (valor, índice del grupo) por cada etiqueta, más cuál es el `default`.
        let mut keys: Vec<(i32, usize)> = Vec::new();
        let mut default_case: Option<usize> = None;
        for (i, c) in cases.iter().enumerate() {
            if let Some(g) = &c.guard {
                self.unsupported(g.pos, "una guarda `when` (el desugar debía haberla bajado)");
                return None;
            }
            if c.is_default {
                default_case = Some(i);
            }
            for l in &c.labels {
                let CaseLabel::Constant(e) = l else {
                    self.unsupported(pos, "una etiqueta `case` que el desugar debía haber bajado");
                    return None;
                };
                match const_int(e) {
                    Some(v) => keys.push((v, i)),
                    None => {
                        self.unsupported(e.pos, "un `case` que no es una constante entera");
                        return None;
                    }
                }
            }
        }
        keys.sort_by_key(|(v, _)| *v);

        let arms: Vec<Label> = cases.iter().map(|_| self.new_label()).collect();
        let end = self.new_label();
        // Sin `default`, el selector que no matchea sale del switch.
        let default_l = default_case.map_or(end, |i| arms[i]);

        self.expr(selector);
        self.pop(1); // la instrucción consume el selector
        let base = self.bytes.len();
        if use_tableswitch(&keys) {
            let (low, high) = (keys[0].0, keys[keys.len() - 1].0);
            self.op(TABLESWITCH);
            self.pad4();
            self.jump4(base, default_l);
            self.u32(low);
            self.u32(high);
            for v in low..=high {
                // Los huecos del rango van al `default`: es lo que paga la indexación directa.
                let arm = keys.iter().find(|(k, _)| *k == v).map_or(default_l, |(_, i)| arms[*i]);
                self.jump4(base, arm);
            }
        } else {
            self.op(LOOKUPSWITCH);
            self.pad4();
            self.jump4(base, default_l);
            self.u32(keys.len() as i32);
            for &(v, i) in &keys {
                self.u32(v);
                self.jump4(base, arms[i]);
            }
        }
        self.reachable = false; // del switch no se **cae** al primer grupo: se salta
        Some((arms, end, default_l))
    }

    /// `switch`-**sentencia**: el salto múltiple y cada grupo como sentencias (la flecha no cae al
    /// siguiente; la forma de dos puntos sí).
    fn switch_stmt(&mut self, s: &Stmt, selector: &Expr, cases: &[SwitchCase], lbl: Option<String>) {
        let Some((arms, end, _)) = self.emit_switch_dispatch(s.pos, selector, cases) else {
            return;
        };
        // Los grupos, en orden de fuente — de ahí sale el *fall-through* de la forma de dos puntos.
        // Los locales declarados en un `case:` viven en el bloque del `switch` (§14.11.2), un solo
        // scope que envuelve todos los grupos; una flecha con `{ }` abre además su propio bloque.
        self.open_scope();
        self.blocks.push(Breakable { label: lbl, brk: end, cont: None, finally_depth: self.finally_stack.len() });
        for (i, c) in cases.iter().enumerate() {
            self.bind(arms[i]);
            match &c.body {
                // La flecha **no** cae al grupo siguiente (§14.11.2): sale sola.
                SwitchBody::Arrow(b) => {
                    self.stmt(b);
                    // El brazo **físicamente último** cae al `end` (que se fija justo después) sin
                    // `goto`: javac elide todo salto al opcode siguiente. Los demás sí saltan.
                    let last = i == cases.len() - 1;
                    if self.reachable && !last {
                        self.jump(GOTO, end);
                        self.reachable = false;
                    }
                }
                SwitchBody::Colon(ss) => ss.iter().for_each(|s| self.stmt(s)),
            }
        }
        self.blocks.pop();
        self.bind(end);
        self.close_scope();
    }

    /// `switch`-**expresión** embebida (`1 + switch(y){…}`): igual que la sentencia, pero cada brazo
    /// **produce un valor**. `case X -> v` es un *yield* implícito; un bloque o la forma de dos puntos
    /// yieldan con `yield`. Todos los caminos dejan **un** valor (del tipo del switch) en la pila —
    /// el resultado de la expresión, que converge en `end`. Solo selector `int`: las switch-expresión
    /// sobre `String`/`enum`/patterns se bajan en posición *lowerable* (las embebidas de esos tipos
    /// todavía cortan).
    fn switch_expr(&mut self, e: &Expr, selector: &Expr, cases: &[SwitchCase]) {
        // Alcance: brazos de **flecha** con expresión (`-> v`, el *yield* implícito) o `-> throw …`.
        // Los brazos de **bloque** (`-> { … yield … }`) o de **dos puntos** con `yield` embebidos
        // todavía cortan: la resolución de sus locales dentro de una switch-expr embebida necesita
        // trabajo en la atribución (hoy un `yield t` de un local del bloque no lo resuelve). Mejor un
        // error honesto que bytecode incorrecto.
        let simple = cases.iter().all(|c| {
            matches!(&c.body, SwitchBody::Arrow(b) if matches!(b.kind, StmtKind::Expr(_) | StmtKind::Throw(_)))
        });
        if !simple {
            return self
                .unsupported(e.pos, "una switch-expresión embebida con brazos de bloque o `:`");
        }
        let cat = category(&self.ty_of(e));
        let vt = vtype_of(self.table, &self.ty_of(e));
        let Some((arms, end, _)) = self.emit_switch_dispatch(e.pos, selector, cases) else {
            return;
        };
        self.open_scope();
        self.yield_targets.push((end, cat, vt));
        for (i, c) in cases.iter().enumerate() {
            self.bind(arms[i]);
            // El brazo **físicamente último** cae al `end` sin `goto` (fiel a javac): su valor queda
            // en la pila y `bind(end)` toma esa pila real. Los demás saltan al `end`.
            let tail = i == cases.len() - 1;
            match &c.body {
                // `case X -> v` en una expresión es el *yield* de `v`.
                SwitchBody::Arrow(b) if matches!(b.kind, StmtKind::Expr(_)) => {
                    if let StmtKind::Expr(v) = &b.kind {
                        self.yield_value(v, tail);
                    }
                }
                // `case X -> throw …`: transfiere el control por su cuenta, no yieldea.
                SwitchBody::Arrow(b) => self.stmt(b),
                SwitchBody::Colon(_) => unreachable!("filtrado por `simple`"),
            }
        }
        self.yield_targets.pop();
        self.bind(end); // acá converge el valor de todos los brazos
        self.close_scope();
    }

    /// Emite un `yield v`: evalúa `v`, lo ajusta al tipo de la switch-expresión (para que todos los
    /// brazos coincidan en el destino) y salta a su fin. Con `tail = true` es el brazo físicamente
    /// último: deja el valor en la pila y **cae** al `end` sin `goto` (así lo emite javac);
    /// `reachable` queda en `true` para que `bind(end)` guarde la pila real.
    fn yield_value(&mut self, v: &Expr, tail: bool) {
        let Some((end, cat, vt)) = self.yield_targets.last().cloned() else {
            return self.unsupported(v.pos, "un `yield` fuera de una switch-expresión");
        };
        self.expr(v);
        self.convert(category(&self.ty_of(v)), cat);
        self.pop(1);
        self.push(vt);
        if tail {
            return; // cae al `end` con el valor en la pila
        }
        self.jump(GOTO, end);
        self.reachable = false;
    }

    /// Relleno hasta el próximo múltiplo de 4 **desde el arranque del método** (JVMS §6,
    /// `tableswitch`): como `bytes` empieza en el pc 0, su largo *es* el pc.
    fn pad4(&mut self) {
        while self.bytes.len() % 4 != 0 {
            self.op(0);
        }
    }

    /// `synchronized (e) { body }` (§14.19), byte-fiel a javac y modelado como un
    /// `try { monitorenter; body } finally { monitorexit }`: el `monitorexit` corre en la salida
    /// normal, en el handler catch-all (que re-lanza) **y** en toda salida abrupta del cuerpo
    /// (`return`/`break`/`continue`), esto último vía la entrada `PendingExit::Monitor` en el
    /// `finally_stack`. El monitor se copia a un local sintético tomado del cursor dinámico
    /// `next_free` (= el `nextreg` de javac), reservado mientras se emite el cuerpo y liberado al
    /// salir; el handler aparca la excepción en el slot siguiente para soltar el monitor con la pila
    /// limpia.
    fn sync_stmt(&mut self, _s: &Stmt, lock: &Expr, body: &Block) {
        // Slot del monitor: uno por nivel, desde la base reservada (= `nextreg` de javac). El
        // aparcadero de la excepción del handler va **por encima de todos** los monitores; los
        // handlers se emiten de adentro hacia afuera y javac le da al más interno el slot más bajo de
        // esa zona, así que se invierte la profundidad.
        let mon = self.sync_base + self.sync_depth;
        let exc_slot = self.sync_base + 2 * self.sync_max_depth - 1 - self.sync_depth;
        self.sync_depth += 1;

        // `<e>; dup; astore mon; monitorenter`: evalúa la referencia una vez y guarda una copia.
        self.expr(lock);
        let lock_t = self
            .stack
            .last()
            .cloned()
            .unwrap_or(VType::Object("java/lang/Object".to_string()));
        self.op(DUP);
        self.push(lock_t.clone());
        self.set_local(mon, lock_t);
        self.store(4, mon);
        self.op(MONITORENTER);
        self.pop(1);

        // El frame del handler: los locales de entrada al bloque, con el monitor ya guardado.
        let entry_locals = self.locals_t.clone();
        let start = self.bytes.len();
        // Mientras se emite el cuerpo, este `monitorexit` queda pendiente: un `return`/`break`/
        // `continue` de adentro lo corre antes de saltar (§14.20.2), igual que un `finally`.
        self.finally_stack.push(PendingExit::Monitor(mon));
        self.block_scoped(&body.0);
        self.finally_stack.pop();
        self.sync_depth -= 1;

        let after = self.new_label();
        // Salida normal: soltar el monitor y saltar. El `monitorexit` queda **dentro** del rango
        // protegido (si él mismo tira, el handler lo reintenta), pero el `goto` no.
        let end = if self.reachable {
            self.load(4, mon);
            self.op(MONITOREXIT);
            self.pop(1);
            let e = self.bytes.len();
            self.jump(GOTO, after);
            self.reachable = false;
            e
        } else {
            self.bytes.len()
        };

        // Handler catch-all (§14.20.2): aparca la excepción, suelta el monitor y re-lanza.
        let handler = self.bytes.len();
        self.exceptions.push(ExceptionEntry {
            start_pc: start as u16,
            end_pc: end as u16,
            handler_pc: handler as u16,
            catch_type: 0, // cualquier Throwable
        });
        self.handler_frame(handler, &entry_locals, "java/lang/Throwable");
        self.stack = vec![VType::Object("java/lang/Throwable".to_string())];
        self.max_stack = self.max_stack.max(1);
        self.locals_t = entry_locals;
        self.set_local(exc_slot, VType::Object("java/lang/Throwable".to_string()));
        self.store(4, exc_slot);
        self.load(4, mon);
        self.op(MONITOREXIT);
        self.pop(1);
        let handler_end = self.bytes.len();
        // El propio `monitorexit` del handler está protegido por el mismo handler: si soltar el
        // monitor tira, se reintenta (la segunda entrada de la tabla, sobre sí misma, como javac).
        self.exceptions.push(ExceptionEntry {
            start_pc: handler as u16,
            end_pc: handler_end as u16,
            handler_pc: handler as u16,
            catch_type: 0,
        });
        self.load(4, exc_slot);
        self.op(ATHROW);
        self.pop(1);
        self.reachable = false;
        self.bind(after);
    }

    /// ¿Hay algún bloque `finally` (no solo `monitorexit`) pendiente? Un `return` con valor aparca el
    /// valor en un temporal solo si un `finally` va a usar la pila; para puros `monitorexit` el valor
    /// puede quedarse debajo de la referencia del monitor.
    fn pending_has_finally(&self) -> bool {
        self.finally_stack
            .iter()
            .any(|p| matches!(p, PendingExit::Finally { .. }))
    }

    /// Emite una copia inline de un `finally`, **reubicando** sus locales por encima de `rebase_top`
    /// (el temporal del `return` o la excepción aparcada del catch-all), como hace javac en cada copia.
    /// La reubicación se restaura al terminar el bloque.
    fn emit_finally(&mut self, block: &Block, rebase_top: u16) {
        let saved = self.slot_remap;
        self.slot_remap = match finally_local_base(&block.0) {
            Some(fb) if rebase_top as i32 != fb as i32 => Some((fb, rebase_top as i32 - fb as i32)),
            _ => None,
        };
        self.block_scoped(&block.0);
        self.slot_remap = saved;
    }

    /// Inyecta los `finally` pendientes **por encima** de `target_depth`, del más interno al más
    /// externo. Cada bloque se emite con el stack de `finally` **truncado** a los que lo encierran, para
    /// que un `return` dentro de un `finally` corra solo los de más afuera (nunca a sí mismo). Sus
    /// locales se reubican sobre `rebase_top`. Devuelve `(nivel, offset)` del arranque de cada copia,
    /// para que [`close_gaps`](Self::close_gaps) los cierre como *gaps* de la región protegida.
    ///
    /// `target_depth = 0` corre **todos** los pendientes (un `return`); un `break`/`continue` pasa la
    /// profundidad de la sentencia a la que salta —cruza solo los de más adentro que el destino—.
    fn run_finallys_down_to(&mut self, target_depth: usize, rebase_top: u16) -> Vec<(usize, usize)> {
        let pending = self.finally_stack.clone();
        let mut starts = Vec::new();
        for i in (target_depth..pending.len()).rev() {
            self.finally_stack.truncate(i);
            match &pending[i] {
                PendingExit::Finally { block, .. } => {
                    let gap_start = self.bytes.len();
                    self.emit_finally(block, rebase_top);
                    starts.push((i, gap_start));
                }
                PendingExit::Monitor(slot) => {
                    // Soltar el monitor antes del salto: `aload lock; monitorexit`. No abre un `gap`
                    // (el `synchronized` gestiona su propia tabla de excepciones, no una región de
                    // `finally` que recortar).
                    self.load(4, *slot);
                    self.op(MONITOREXIT);
                    self.pop(1);
                }
            }
        }
        self.finally_stack = pending;
        starts
    }

    /// Cierra los *gaps* que abrieron las copias inline de una salida abrupta: cada uno va desde su
    /// arranque hasta el offset actual (ya emitidos el `ireturn`/`goto` de cierre). Se anotan en la
    /// entrada del `finally_stack` de su `try`, que los recorta de la región protegida en su tabla.
    fn close_gaps(&mut self, starts: Vec<(usize, usize)>) {
        let gap_end = self.bytes.len();
        for (level, gap_start) in starts {
            if let Some(PendingExit::Finally { gaps, .. }) = self.finally_stack.get_mut(level) {
                gaps.push((gap_start, gap_end));
            }
        }
    }

    /// `try { … } catch (E e) { … } [finally { … }]`.
    ///
    /// El cuerpo protegido queda en `[start, end)`; cada `catch` instala una entrada en la tabla de
    /// excepciones. Al entrar a un *handler* la JVM **limpia la pila y deja ahí la excepción**, así
    /// que su frame lleva `stack = [E]` y arranca guardándola en el slot de su variable.
    ///
    /// El `finally` se emite **duplicado** (§14.20.2): una copia en **cada** salida —normal, `return`,
    /// `break`/`continue` (inyectada donde ocurre la salida abrupta)— y otra en un handler *catch-all*
    /// (`catch_type = 0`) que además protege los cuerpos de los `catch`, lo corre y re-lanza. La v69 ya
    /// no admite `jsr`/`ret`, así que duplicar es la única vía.
    fn try_stmt(&mut self, body: &Block, catches: &[CatchClause], finally: &Option<Block>) {
        let entry_locals = self.locals_t.clone();
        let start = self.bytes.len();
        // Mientras se emite el cuerpo, este `finally` queda **pendiente**: un `return` de adentro lo
        // corre (§14.20.2). Se saca antes de emitir la copia de la salida normal (que ya es el `finally`
        // en sí y no debe correrse a sí misma). Al sacarlo se recuperan los `gaps` que sus copias
        // inline dejaron en el cuerpo, para recortarlos de la región protegida.
        if let Some(f) = finally {
            self.finally_stack.push(PendingExit::Finally { block: f.clone(), gaps: Vec::new() });
        }
        self.block_scoped(&body.0);
        let body_gaps = if finally.is_some() {
            match self.finally_stack.pop() {
                Some(PendingExit::Finally { gaps, .. }) => gaps,
                _ => Vec::new(),
            }
        } else {
            Vec::new()
        };
        let end = self.bytes.len();
        let after = self.new_label();
        if self.reachable {
            // Salida normal: sin temporal, el `finally` se reubica en el next-free vivo.
            if let Some(f) = finally {
                self.emit_finally(f, self.next_free);
            }
            self.jump(GOTO, after);
            self.reachable = false;
        }

        // Rango de cada cuerpo de `catch`, para que el `finally` catch-all también lo proteja: si un
        // `catch` **lanza** (o retorna), el `finally` debe correr igual (§14.20.2). Cada rango lleva sus
        // propios `gaps` (las copias inline de un `return` del `catch`), a recortar de la protección.
        let mut catch_ranges: Vec<(usize, usize, Vec<(usize, usize)>)> = Vec::new();
        for c in catches {
            let handler = self.bytes.len();
            let exc = c
                .types
                .first()
                .map(|t| match vtype_of_type(self.table, self.scope, t) {
                    VType::Object(n) => n,
                    _ => "java/lang/Throwable".to_string(),
                })
                .unwrap_or_else(|| "java/lang/Throwable".to_string());
            // Frame del handler: los locales de entrada al `try`, con la excepción en la pila.
            self.handler_frame(handler, &entry_locals, &exc);
            self.stack = vec![VType::Object(exc.clone())];
            self.max_stack = self.max_stack.max(1);
            self.locals_t = entry_locals.clone();
            // Un *handler* **siempre** es alcanzable: se llega por excepción, no por caída. Si el
            // cuerpo del `try` termina en `throw` o `return`, `reachable` quedó en `false`, y sin
            // resetearlo acá el `if self.reachable` del final del `catch` no emitía **ni la copia
            // en línea del `finally` ni el `goto`**: el `catch` caía derecho dentro del handler
            // catch-all, que hace `astore` de un throwable que nadie apiló (#257). El caso es
            // exactamente "el `catch` de verdad se dispara y hay `finally`", que es la forma más
            // común de las tres.
            self.reachable = true;
            // Scope del `catch`: la variable de la excepción vive en el handler (§14.20).
            self.open_scope();
            if let Some(slot) = c.slot {
                self.set_local(slot, VType::Object(exc.clone()));
                self.store(4, slot); // astore: la excepción es una referencia
                self.open_local(slot, &c.name, &format!("L{exc};"), &[]);
            } else {
                self.op(POP);
                self.pop(1);
            }
            // El `finally` también encierra el cuerpo del `catch`: un `return` de acá lo corre.
            if let Some(f) = finally {
                self.finally_stack.push(PendingExit::Finally { block: f.clone(), gaps: Vec::new() });
            }
            for s in &c.body.0 {
                self.stmt(s);
            }
            // El cuerpo del `catch` va de su handler hasta acá (antes de la copia del `finally` de salida
            // normal): ese rango —recortadas sus propias copias inline— lo protege el catch-all.
            let catch_end = self.bytes.len();
            let catch_gaps = if finally.is_some() {
                match self.finally_stack.pop() {
                    Some(PendingExit::Finally { gaps, .. }) => gaps,
                    _ => Vec::new(),
                }
            } else {
                Vec::new()
            };
            if finally.is_some() {
                catch_ranges.push((handler, catch_end, catch_gaps));
            }
            // Cerrar el scope del `catch` **antes** de la copia del `finally` de salida normal: así su
            // variable libera el slot y el `finally` lo reusa (como javac, que corre el finalizador con
            // `nextreg` ya restaurado tras la variable del `catch`).
            self.close_scope();
            if self.reachable {
                if let Some(f) = finally {
                    self.emit_finally(f, self.next_free);
                }
                self.jump(GOTO, after);
                self.reachable = false;
            }
            let ct = self.pool.class(&exc);
            // `try → catch` (tipada), recortando las copias inline del cuerpo del `try`.
            for (s0, e0) in split_range(start, end, &body_gaps) {
                self.exceptions.push(ExceptionEntry {
                    start_pc: s0 as u16,
                    end_pc: e0 as u16,
                    handler_pc: handler as u16,
                    catch_type: ct,
                });
            }
        }

        // El `finally` también corre si algo se escapa: handler catch-all que lo ejecuta y re-lanza.
        if let Some(f) = finally {
            let handler = self.bytes.len();
            self.handler_frame(handler, &entry_locals, "java/lang/Throwable");
            self.stack = vec![VType::Object("java/lang/Throwable".to_string())];
            self.max_stack = self.max_stack.max(1);
            self.locals_t = entry_locals.clone();
            // Aparcar la excepción en el **high-water** global de locales (el `newRegSegment` de javac,
            // que lleva `nextreg` al máximo alcanzado por todas las rutas previas) en vez de dejarla en la
            // pila mientras corre el `finally`: así el `finally` usa la pila sin restricciones.
            let park = self.max_locals;
            self.set_local(park, VType::Object("java/lang/Throwable".to_string()));
            self.store(4, park); // astore park: saca la excepción de la pila
            let catchall_finally_start = self.bytes.len();
            self.emit_finally(f, park + 1); // el `finally` corre con la pila limpia, reubicado sobre `park`
            self.load(4, park); // aload park: la recupera para re-lanzarla
            self.op(ATHROW);
            self.pop(1);
            self.reachable = false;
            // `try → catch-all`, recortando las copias inline del cuerpo del `try`.
            for (s0, e0) in split_range(start, end, &body_gaps) {
                self.exceptions.push(ExceptionEntry {
                    start_pc: s0 as u16,
                    end_pc: e0 as u16,
                    handler_pc: handler as u16,
                    catch_type: 0,
                });
            }
            // …y sobre cada cuerpo de `catch` (recortadas sus propias copias inline): un `throw`/`return`
            // ahí adentro pasa por el mismo catch-all, que corre el `finally` y re-lanza.
            for (cs, ce, gaps) in &catch_ranges {
                for (s0, e0) in split_range(*cs, *ce, gaps) {
                    self.exceptions.push(ExceptionEntry {
                        start_pc: s0 as u16,
                        end_pc: e0 as u16,
                        handler_pc: handler as u16,
                        catch_type: 0,
                    });
                }
            }
            // Auto-protección: la parte del catch-all previa a su propia copia del `finally` (el `astore`
            // de la excepción) la protege él mismo —como emite javac—.
            if catchall_finally_start > handler {
                self.exceptions.push(ExceptionEntry {
                    start_pc: handler as u16,
                    end_pc: catchall_finally_start as u16,
                    handler_pc: handler as u16,
                    catch_type: 0,
                });
            }
        }
        self.bind(after);
    }

    /// Registra el frame de un *handler*: se alcanza solo por excepción, así que no hay salto que lo
    /// apunte y hay que forzarlo. Un handler es **siempre alcanzable** (por la arista de excepción),
    /// así que se marca `reachable`: sin esto, un `finally`/`catch` que se emite tras un camino cortado
    /// (p.ej. la copia catch-all del `finally`, que sigue a un `catch` terminado en `throw`) heredaba
    /// `reachable == false` y se comía los `goto` de sus try/catch internos —dejando que el flujo cayera
    /// dentro de su propio handler y produciendo un `StackMapTable` inverificable.
    fn handler_frame(&mut self, at: usize, locals: &[VType], exc: &str) {
        let l = self.new_label();
        self.labels[l] = Some(at);
        self.targets.insert(l);
        self.frames.insert(at, (locals.to_vec(), vec![VType::Object(exc.to_string())]));
        self.reachable = true;
    }

    /// Evalúa una expresión **descartando** su valor (el `update` de un `for`, o una
    /// sentencia-expresión): una asignación no deja nada, el resto se saca con `pop`.
    fn discard(&mut self, e: &Expr) {
        if let ExprKind::Assign { op: AssignOp::Assign, target, value } = &e.kind {
            // Peephole `iinc` para `x = x ± c` (la forma desugarada de `x++`/`x += c`) en descarte.
            if self.try_iinc(target, value) {
                return;
            }
            self.assign(target, value, false);
            return;
        }
        // `x++;` suelto: se incrementa sin dejar nada (y así el `for` emite un `iinc` pelado).
        if let ExprKind::Unary { op: op @ (UnOp::Inc | UnOp::Dec), expr: target, prefix } = &e.kind {
            self.incdec(target, *op == UnOp::Inc, *prefix, false);
            return;
        }
        let ty = self.ty_of(e);
        self.expr(e);
        if !matches!(ty, RType::Void) {
            let cat = category(&ty);
            self.op(if stack_width(cat) == 2 { POP2 } else { POP });
            self.pop(1);
        }
    }

    // ---- expresiones (dejan su valor en la pila) ----

    fn expr(&mut self, e: &Expr) {
        match &e.kind {
            // Un nodo de error nunca llega al codegen: la compilación aborta si hubo errores.
            ExprKind::Error => unreachable!("ExprKind::Error en el codegen (la compilación debió abortar)"),
            ExprKind::IntLit(n) => self.push_int(*n as i32),
            ExprKind::CharLit(c) => self.push_int(*c as i32),
            ExprKind::BoolLit(b) => self.push_int(*b as i32),
            ExprKind::LongLit(n) => self.push_long(*n),
            ExprKind::FloatLit(f) => self.push_float(*f as f32),
            ExprKind::DoubleLit(d) => self.push_double(*d),
            ExprKind::StringLit(s) => {
                let idx = self.pool.string(s);
                self.ldc(idx);
                self.push(VType::Object("java/lang/String".to_string()));
            }
            ExprKind::Null => {
                self.op(ACONST_NULL);
                // `null` es asignable a cualquier referencia; el verificador tiene su propio tipo
                // para eso, pero declararlo `Object` alcanza mientras no se mezcle en un merge.
                self.push(VType::Object("java/lang/Object".to_string()));
            }
            ExprKind::This => self.load_this(),
            // `Outer.this` lo baja el desugar a la cadena de `this$0` (`this.this$0…`); si llega
            // hasta acá es que no se resolvió su clase envolvente.
            ExprKind::QualifiedThis(_) => {
                self.unsupported(e.pos, "un `Clase.this` que el desugar debía haber bajado")
            }
            // Un nombre suelto: un local (slot de la frame) o un campo **implícito** de `this`.
            ExprKind::Name(_) => match e.binding {
                Some(Binding::Local { slot }) => {
                    let cat = category(&self.ty_of(e));
                    self.load(cat, slot);
                }
                Some(Binding::Field(fid)) => self.read_field(fid, None),
                _ => {}
            },
            // `a.length` no es un campo: tiene su propio opcode.
            ExprKind::Field { expr: recv, name: f }
                if f == "length" && matches!(self.ty_of(recv), RType::Array(_)) =>
            {
                self.expr(recv);
                self.op(ARRAYLENGTH);
                self.pop(1);
                self.push(VType::Int);
            }
            ExprKind::Field { expr: recv, .. } => {
                if let Some(Binding::Field(fid)) = e.binding {
                    self.read_field(fid, Some(recv));
                }
            }
            ExprKind::Assign { op: AssignOp::Assign, target, value } => {
                self.assign(target, value, true) // como expresión, deja el valor
            }
            // `C.class` → `ldc` de una entrada `Class` del pool (la JVM la resuelve al objeto).
            ExprKind::ClassLit(ty) => {
                // §15.8.2 — el literal de un **primitivo** no puede ser un `ldc` de clase: no hay
                // entrada `CONSTANT_Class` para `int`. Es el campo `TYPE` de su envoltorio, que es
                // lo que emite el javac real. Un **array** sí es un `ldc`, pero de su **descriptor**
                // (`[I`), no de un nombre interno.
                if let Some(wrapper) = primitive_wrapper(ty) {
                    let fref = self.pool.fieldref(wrapper, "TYPE", "Ljava/lang/Class;");
                    self.op(GETSTATIC);
                    self.u16(fref);
                    self.push(VType::Object("java/lang/Class".to_string()));
                    return;
                }
                let internal = match vtype_of_type(self.table, self.scope, ty) {
                    VType::Object(n) => n,
                    _ => "java/lang/Object".to_string(),
                };
                let idx = self.pool.class(&internal);
                self.ldc(idx);
                self.push(VType::Object("java/lang/Class".to_string()));
            }
            ExprKind::Cast { expr: inner, .. } => self.cast(e, inner),
            ExprKind::NewObject { args, body, .. } => {
                if body.is_some() {
                    self.unsupported(e.pos, "una clase anónima (necesita una clase sintética anidada)");
                } else {
                    self.new_object(e, args);
                }
            }
            // Una comparación **como valor** (`boolean b = x > 0;`) se materializa con saltos.
            ExprKind::Binary { op, .. } if cmp_index(*op, true).is_some() => self.bool_value(e),
            ExprKind::Binary { op: BinOp::And | BinOp::Or, .. } => self.bool_value(e),
            ExprKind::Unary { op: UnOp::Not, .. } => self.bool_value(e),
            ExprKind::Unary { op: op @ (UnOp::Inc | UnOp::Dec), expr: target, prefix } => {
                self.incdec(target, *op == UnOp::Inc, *prefix, true)
            }
            // `-<literal numérico>` se pliega a la constante **negada** (`-1` → `iconst_m1`, `-100`
            // → `bipush -100`), como javac, en vez de `push <literal>; neg`. Lo destapó el diferencial.
            ExprKind::Unary { op: UnOp::Neg, expr: inner, .. }
                if matches!(
                    inner.kind,
                    ExprKind::IntLit(_)
                        | ExprKind::LongLit(_)
                        | ExprKind::FloatLit(_)
                        | ExprKind::DoubleLit(_)
                ) =>
            {
                match &inner.kind {
                    ExprKind::IntLit(n) => self.push_int((*n as i32).wrapping_neg()),
                    ExprKind::LongLit(n) => self.push_long(n.wrapping_neg()),
                    ExprKind::FloatLit(f) => self.push_float(-*f),
                    ExprKind::DoubleLit(d) => self.push_double(-*d),
                    _ => unreachable!(),
                }
            }
            ExprKind::Unary { op, expr: inner, .. } => {
                self.expr(inner);
                let cat = category(&self.ty_of(e));
                match op {
                    UnOp::Neg => self.op(0x74 + cat), // ineg / lneg / fneg / dneg
                    UnOp::Plus => {}                  // el `+` unario no emite nada
                    UnOp::BitNot => {
                        // `~x` no tiene opcode: es `x ^ -1`.
                        if cat == 1 {
                            self.push_long(-1);
                        } else {
                            self.push_int(-1);
                        }
                        self.op(0x82 + u8::from(cat == 1)); // ixor / lxor
                        self.pop(2);
                        self.push_cat(cat);
                    }
                    UnOp::Not | UnOp::Inc | UnOp::Dec => unreachable!("los maneja `incdec`"),
                }
            }
            ExprKind::Binary { op, lhs, rhs } => {
                // **Promoción numérica binaria** (§5.6.2): el operando más angosto se ensancha al
                // tipo del resultado. Sin esto, `longA + 1` haría `ladd` con un `int` en la pila.
                let cat = category(&self.ty_of(e));
                self.expr(lhs);
                self.convert(category(&self.ty_of(lhs)), cat);
                self.expr(rhs);
                // Los **desplazamientos** son la excepción: promocionan cada operando por separado y
                // el de la derecha va como `int` (§15.19) — `lshl` toma (long, int).
                let rhs_target = if matches!(op, BinOp::Shl | BinOp::Shr | BinOp::UShr) { 0 } else { cat };
                self.convert(category(&self.ty_of(rhs)), rhs_target);
                self.arith(*op, cat);
            }
            ExprKind::Call { target, name, args, .. } => {
                // `array.clone()` (§10.7): no resuelve a un símbolo de método (los arrays no lo tienen
                // en la tabla), así que `invoke` no sabría emitirlo. Se emite estructuralmente.
                if name == "clone" && args.is_empty() {
                    if let Some(t) = target {
                        if matches!(self.ty_of(t), RType::Array(_)) {
                            self.array_clone(t);
                            return;
                        }
                    }
                }
                // Una llamada de **instancia** empuja primero el receptor (el explícito, o `this`).
                let is_static = match e.binding {
                    Some(Binding::Method(mid)) => {
                        self.table.symbol(mid).modifiers.contains(&Modifier::Static)
                    }
                    // Sin binding se asume estático (no hay receptor que empujar) — salvo un
                    // `super()`/`this()` implícito, que sí lleva el `this`.
                    _ => name != "super" && name != "this",
                };
                if !is_static {
                    match target {
                        Some(t) => self.expr(t),
                        None => self.load_this(),
                    }
                }
                self.emit_args(e, args);
                // `super.m(...)` **no** despacha virtualmente: va por `invokespecial` con la
                // **superclase directa** como dueño del methodref — no la clase que declara el
                // método. (Comprobado contra el javac del JDK 25: para `C extends B extends A` con
                // `f` declarado en `A`, emite `invokespecial B.f`, no `A.f`.)
                let via_super = target.as_ref().is_some_and(|t| matches!(t.kind, ExprKind::Super));
                let ctor_call = name == "super" || name == "this";
                // Un `super(...)`/`this(...)` **sin binding** es una clase padre que no declara
                // constructores: rige el `()V` implícito, que no tiene símbolo. Con argumentos, en
                // cambio, no hay a qué llamar — y dejarlos empujados corrompería la pila.
                if ctor_call && e.binding.is_none() {
                    if args.is_empty() {
                        let owner =
                            if name == "super" { self.super_class.clone() } else { self.this_class.clone() };
                        let mref = self.pool.methodref(&owner, "<init>", "()V");
                        self.op(INVOKESPECIAL);
                        self.u16(mref);
                        self.pop(1);
                    } else {
                        self.unsupported(e.pos, "un `super(...)`/`this(...)` que no resolvió a ningún constructor");
                    }
                } else {
                    self.invoke(e, args, is_static, via_super);
                }
                // Un `super(...)`/`this(...)` explícito **inicializa** el `this`: de acá en más el
                // verificador lo deja usar.
                if ctor_call {
                    self.init_this();
                }
            }
            ExprKind::Index { array, index } => {
                self.expr(array);
                self.expr(index);
                let kind = array_kind(&self.ty_of(e));
                self.op(IALOAD + kind);
                // Consume (arrayref, índice) y deja el elemento.
                let elem = vtype_of(self.table, &self.ty_of(e));
                self.pop(2);
                self.push(elem);
            }
            ExprKind::NewArray { dims, init, .. } => self.new_array(e, dims, init),
            ExprKind::InstanceOf { expr: inner, ty, .. } => {
                self.expr(inner);
                let internal = match vtype_of_type(self.table, self.scope, ty) {
                    VType::Object(n) => n,
                    _ => "java/lang/Object".to_string(),
                };
                let idx = self.pool.class(&internal);
                let off = self.bytes.len() as u16; // offset del `instanceof` (target 0x43)
                self.op(INSTANCEOF);
                self.u16(idx);
                self.pop(1);
                self.push(VType::Int); // el 0/1 del resultado
                self.code_type_annos(0x43, &off.to_be_bytes(), &e.type_annos);
            }
            // `super` **como receptor** es el mismo `this`: lo que cambia no es el objeto sino el
            // **despacho** (§15.12.4.4), que pasa a `invokespecial` sobre la superclase — eso lo
            // resuelve el arm de `Call`. Acá alcanza con empujarlo. El verificador lo acepta porque
            // `this` es subtipo de su superclase (#231/#125).
            ExprKind::Super => self.load_this(),
            // `c ? a : b` es un `if/else` que deja **un** valor en la pila. Cada rama se promueve al
            // tipo del ternario entero (§15.25): `flag ? 1 : 2L` tiene que dejar un `long` por los
            // dos caminos, o el frame del destino no cerraría.
            ExprKind::Ternary { cond, then, els } => {
                let cat = category(&self.ty_of(e));
                let vt = vtype_of(self.table, &self.ty_of(e));
                let otherwise = self.new_label();
                let end = self.new_label();
                self.branch_if(cond, otherwise, false);
                self.expr(then);
                self.convert(category(&self.ty_of(then)), cat);
                // El valor de la rama se declara con el tipo del ternario **entero**, no con el
                // de la rama: es lo que las hace coincidir en el destino.
                self.pop(1);
                self.push(vt.clone());
                self.jump(GOTO, end);
                self.reachable = false;
                self.bind(otherwise);
                // Las dos ramas empujan **el mismo** valor, no uno cada una.
                self.pop(1);
                self.expr(els);
                self.convert(category(&self.ty_of(els)), cat);
                self.pop(1);
                self.push(vt);
                self.bind(end);
            }
            ExprKind::Switch { selector, cases } => self.switch_expr(e, selector, cases),
            // Un *call site* de `invokedynamic` ya bajado por el desugar (LambdaToMethod): se
            // empujan las capturas —el receptor `this` primero, si la impl es de instancia— y se
            // emite el opcode. El *bootstrap* es `LambdaMetafactory.metafactory`, con el `MethodType`
            // borrado, el `MethodHandle` de la implementación `lambda$…` y el `MethodType`
            // instanciado como argumentos estáticos (§4.7.23 / §5.4.3.6).
            ExprKind::Indy { info, captures } => {
                for c in captures {
                    self.expr(c);
                }
                // El *bootstrap method* (siempre `REF_invokeStatic`) y sus argumentos estáticos, cada
                // uno traducido a su entrada del pool según su clase.
                let bsm = self.pool.method_handle(
                    REF_INVOKE_STATIC,
                    &info.bootstrap_owner,
                    &info.bootstrap_name,
                    &info.bootstrap_desc,
                );
                let args: Vec<u16> = info
                    .bootstrap_args
                    .iter()
                    .map(|a| match a {
                        BootstrapArg::MethodType(d) => self.pool.method_type(d),
                        BootstrapArg::MethodHandle { kind, owner, name, desc } => {
                            self.pool.method_handle(*kind, owner, name, desc)
                        }
                        BootstrapArg::Class(n) => self.pool.class(n),
                        BootstrapArg::Str(s) => self.pool.string(s),
                    })
                    .collect();
                let idx = self.bootstraps.len() as u16;
                self.bootstraps.push(BootstrapMethod { method_handle: bsm, args });
                let site = self.pool.invoke_dynamic(idx, &info.name, &info.descriptor);
                self.op(INVOKEDYNAMIC);
                self.u16(site);
                self.u16(0); // los dos bytes cero que exige el formato del opcode (§6.5)
                // La pila: se consumen las capturas y queda el **retorno del descriptor** del call
                // site (más fiable que `e.ty`, que para un `record` no describe el resultado).
                self.pop(captures.len());
                let ret = &info.descriptor[info.descriptor.rfind(')').map_or(0, |i| i + 1)..];
                if ret != "V" {
                    self.push(vtype_of_desc(ret));
                }
            }
            // Una **lambda** que llegó **sin bajar** (target funcional no resuelto): la corta la
            // barrera, como antes. Con target resuelto, el desugar ya la convirtió en `Indy`.
            ExprKind::Lambda { .. } => {
                self.unsupported(e.pos, "una expresión lambda (necesita `invokedynamic`)")
            }
            ExprKind::MethodRef { .. } => {
                self.unsupported(e.pos, "una referencia a método (necesita `invokedynamic`)")
            }
            // Una asignación **compuesta** que llegó hasta acá es la del lvalue con efectos, que el
            // desugar deja a propósito para el emisor (ver `compound_effectful`).
            ExprKind::Assign { op, target, value } => {
                self.compound_effectful(e, *op, target, value)
            }
        }
    }

    /// `array.clone()` (§10.7): empuja el array, `invokevirtual "[desc]".clone:()Ljava/lang/Object;`
    /// y **`checkcast [desc]`** —el descriptor del `clone` heredado devuelve `Object`, pero el lenguaje
    /// tipa el resultado como el **tipo del array** (retorno covariante), así que hay que estrecharlo—.
    /// Es exactamente lo que emite javac (p. ej. en el `values()` de un `enum`).
    fn array_clone(&mut self, receiver: &Expr) {
        let arr_ty = self.ty_of(receiver);
        let adesc = rtype_desc(self.table, &arr_ty); // `[LEnums;` / `[I`
        self.expr(receiver);
        let mref = self.pool.methodref(&adesc, "clone", "()Ljava/lang/Object;");
        self.op(INVOKEVIRTUAL);
        self.u16(mref);
        let cc = self.pool.class(&adesc);
        self.op(CHECKCAST);
        self.u16(cc);
        self.pop(1); // sale el receptor, entra el array clonado (del tipo del array)
        self.push(vtype_of(self.table, &arr_ty));
    }

    fn invoke(&mut self, call: &Expr, args: &[Expr], is_static: bool, via_super: bool) {
        // Sin binding **no hay nada que emitir**, y salir en silencio es la peor salida posible: los
        // argumentos ya se empujaron, así que el método sigue con la pila corrida y el `.class` sale
        // igual. Lo destapó `s.length(1, 2)` contra un tipo del classpath, que compilaba a
        // `iconst_1; iconst_2; ireturn` — sin receptor, sin llamada y sin un solo diagnóstico (#261).
        // La pasada 2 es **indulgente** a propósito con las sobrecargas de un tipo externo (no
        // modelamos toda firma del JDK); esa indulgencia puede terminar en "no sé el tipo", pero no
        // puede terminar en un class file roto y mudo.
        let Some(Binding::Method(mid)) = call.binding else {
            self.unsupported(call.pos, "una llamada que no resolvió a ningún método");
            return;
        };
        let Some(owner) = self.table.symbol(mid).owner else {
            self.unsupported(call.pos, "una llamada a un método sin clase declarante");
            return;
        };
        let class_internal = internal_name(self.table, owner);
        let is_ctor = matches!(self.table.symbol(mid).kind, SymbolKind::Method { is_constructor: true, .. });
        let mname = if is_ctor { "<init>".to_string() } else { self.table.symbol(mid).name.clone() };
        let (params, ret) = match self.table.resolved(mid) {
            Some(Resolved::Method { params, ret, .. }) => (params.clone(), ret.clone()),
            _ => (Vec::new(), RType::Void),
        };
        let ps: String = params.iter().map(|t| rtype_desc(self.table, t)).collect();
        let desc = format!("({ps}){}", rtype_desc(self.table, &ret));
        let owner_is_interface = matches!(
            &self.table.symbol(owner).kind,
            SymbolKind::Class { kind: TypeKind::Interface, .. }
        );
        // A `private` instance method is **not virtual** — it never enters the vtable (it can't be
        // overridden), so the JVM invokes it with `invokespecial`, not `invokevirtual` (JVMS §6.5).
        // Emitting `invokevirtual` compiles fine and passes `javap`, but at run time the vtable
        // lookup misses and the VM throws `NoSuchMethodError`. Static privates go through the
        // `is_static` branch (invokestatic); private *interface* methods are left to the interface
        // branch below.
        let is_private = self.table.symbol(mid).modifiers.contains(&Modifier::Private);

        // El owner del `Methodref`/`InterfaceMethodref` de una invocación de despacho es el **tipo
        // estático del receptor** (§5.4.3.3/§5.4.3.4), no la clase que *declara* el método: `xs.add(i)`
        // con `xs : List` emite `List.add`, aunque `add` se herede de `Collection`. Solo se calcula para
        // un receptor **explícito** que no sea `this`/`super`; el implícito/`this`/`super` conserva el
        // owner del caso de abajo.
        let explicit_recv_owner: Option<String> = match &call.kind {
            ExprKind::Call { target: Some(t), .. }
                if !matches!(t.kind, ExprKind::This | ExprKind::Super) =>
            {
                super::types::erased_id(&self.ty_of(t)).map(|id| internal_name(self.table, id))
            }
            _ => None,
        };

        // `invokestatic` sin receptor; `invokespecial` para un constructor o un método `private` de
        // instancia; `invokeinterface` si el método pertenece a una **interfaz** (§6.5, con
        // `count` = slots del receptor + argumentos); si no, despacho virtual.
        if is_static {
            let mref = self.pool.methodref(&class_internal, &mname, &desc);
            self.op(INVOKESTATIC);
            self.u16(mref);
        } else if via_super {
            // `super.m(...)`: `invokespecial` con la **superclase directa** como dueño. Que sea la
            // superclase y no la clase declarante es lo que hace el javac real, y es lo que da la
            // semántica de §15.12.4.4 — saltear el override de *esta* clase y empezar a buscar
            // desde arriba, aunque el método venga heredado de más lejos.
            let sup = self.super_class.clone();
            let mref = self.pool.methodref(&sup, &mname, &desc);
            self.op(INVOKESPECIAL);
            self.u16(mref);
        } else if is_ctor {
            let mref = self.pool.methodref(&class_internal, &mname, &desc);
            self.op(INVOKESPECIAL);
            self.u16(mref);
        } else if is_private && !owner_is_interface {
            let mref = self.pool.methodref(&class_internal, &mname, &desc);
            self.op(INVOKESPECIAL);
            self.u16(mref);
        } else if owner_is_interface {
            let iowner = explicit_recv_owner.clone().unwrap_or_else(|| class_internal.clone());
            let imref = self.pool.interface_methodref(&iowner, &mname, &desc);
            self.op(INVOKEINTERFACE);
            self.u16(imref);
            let count: u16 = 1 + params
                .iter()
                .map(|t| u16::from(matches!(t, RType::Prim(PrimType::Long | PrimType::Double))) + 1)
                .sum::<u16>();
            self.op(count as u8);
            self.op(0);
        } else {
            // El owner del `Methodref` de una invocación **virtual** es el **tipo estático del
            // receptor**, no la clase que *declara* el método (§5.4.3.3): para un receptor implícito o
            // `this`, ese tipo es la clase actual. Sin esto, `ordinal()` heredado de `java.lang.Enum`
            // emitía owner `java/lang/Enum`; javac emite la propia clase (`Enums`). Se acota a virtual y
            // a receptor implícito/`this` — `invokespecial`/`invokestatic`/`super` no se tocan.
            let receiver_is_self = match &call.kind {
                ExprKind::Call { target, .. } => {
                    matches!(target.as_deref(), None | Some(Expr { kind: ExprKind::This, .. }))
                }
                _ => false,
            };
            let owner_internal = explicit_recv_owner.unwrap_or_else(|| {
                if receiver_is_self { self.this_class.clone() } else { class_internal }
            });
            let mref = self.pool.methodref(&owner_internal, &mname, &desc);
            self.op(INVOKEVIRTUAL);
            self.u16(mref);
        }
        // Consume los argumentos (y el receptor, si lo hubo) y deja el retorno si no es `void`.
        self.pop(args.len() + usize::from(!is_static));
        if !matches!(ret, RType::Void) {
            let t = vtype_of(self.table, &ret);
            self.push(t);
            self.synthetic_cast(call, &ret);
        }
    }

    /// El **cast sintetico** (JLS 5.5, JVMS 4.10.1.9) que sigue a una llamada cuyo retorno declarado
    /// se **borra**: `List<String>.get` esta declarado `E` y su descriptor dice `Object`, asi que lo
    /// que queda en la pila es un `Object`. Encadenar ahi -`l.get(0).length()`- emite un
    /// `invokevirtual String.length` sobre un `Object`, y eso **no verifica**: la JVM real lo rechaza
    /// con `VerifyError` antes de ejecutar una sola instruccion.
    ///
    /// Nuestro interprete no lo rechazaba -despacha por el objeto real- y por eso el defecto era
    /// invisible de este lado: es de la familia "compila, corre aca, revienta en la JVM de verdad".
    ///
    /// **Diferencia deliberada con el javac real:** el javac lo inserta solo donde el contexto pide
    /// el tipo angosto (`return`, argumento, receptor) y lo omite cuando el destino es mas ancho
    /// (`Object o = l.get(0);`) o cuando el valor se descarta. Aca se inserta siempre que el tipo del
    /// sitio sea **estrictamente mas angosto** que el retorno borrado, que es correcto -el cast no
    /// puede fallar en un programa bien tipado- y cuesta un `checkcast` de mas en esas dos formas.
    /// Saber el contexto pide un pase aparte; el `checkcast` de mas no rompe nada.
    fn synthetic_cast(&mut self, call: &Expr, declared: &RType) {
        let Some(site) = call.ty.clone() else { return };
        let want = types::erasure(self.table, &site);
        let have = types::erasure(self.table, declared);
        // Solo referencias, solo si el sitio es **mas angosto**, y nunca si ya coinciden.
        if want == have || !is_ref(&want) || !is_ref(&have) {
            return;
        }
        if !types::is_subtype(self.table, &want, &have) {
            return;
        }
        let name = checkcast_name(self.table, &want);
        let cc = self.pool.class(&name);
        self.op(CHECKCAST);
        self.u16(cc);
        self.pop(1);
        self.push(vtype_of(self.table, &want));
    }

    // ---- condiciones como saltos ----

    /// Emite un salto a `target` **cuando `cond` valga `when`**. Es la forma canónica de compilar
    /// condiciones: `&&`/`||` se vuelven saltos (así se cortocircuitan de verdad) en vez de calcular
    /// un booleano, y negar la condición es simplemente invertir el sentido del salto.
    fn branch_if(&mut self, cond: &Expr, target: Label, when: bool) {
        // §15.28: una condición **constante** no se testea en runtime. Si su valor coincide con `when`
        // el salto es incondicional (`goto`); si no, no se emite nada (se cae). No cambia la estructura
        // ni `reachable` (la rama muerta se sigue emitiendo, pero saltada), así que es consistente con
        // lo que calculó el *flow* con el mismo evaluador. Una constante no tiene efectos que perder.
        if let Some(v) = const_bool_expr(cond) {
            if v == when {
                self.jump(GOTO, target);
            }
            return;
        }
        match &cond.kind {
            // `!c` ⇒ saltar cuando `c` valga lo contrario.
            ExprKind::Unary { op: UnOp::Not, expr, .. } => self.branch_if(expr, target, !when),
            ExprKind::Binary { op: BinOp::And, lhs, rhs } => {
                if when {
                    // Salta solo si ambos: si el primero falla, ni se evalúa el segundo.
                    let skip = self.new_label();
                    self.branch_if(lhs, skip, false);
                    self.branch_if(rhs, target, true);
                    self.bind(skip);
                } else {
                    self.branch_if(lhs, target, false);
                    self.branch_if(rhs, target, false);
                }
            }
            ExprKind::Binary { op: BinOp::Or, lhs, rhs } => {
                if when {
                    self.branch_if(lhs, target, true);
                    self.branch_if(rhs, target, true);
                } else {
                    let skip = self.new_label();
                    self.branch_if(lhs, skip, true);
                    self.branch_if(rhs, target, false);
                    self.bind(skip);
                }
            }
            ExprKind::Binary { op, lhs, rhs } if cmp_index(*op, true).is_some() => {
                let lcat = category(&self.ty_of(lhs));
                let rcat = category(&self.ty_of(rhs));
                // Comparación de un `int` contra la constante literal **0**: los saltos de **un solo
                // operando** (`ifeq`/`ifne`/`iflt`/`ifge`/`ifgt`/`ifle`, 0x99..0x9e) en vez de
                // `iconst_0; if_icmp…`, como javac. Si el `0` va a la izquierda, la comparación se
                // **invierte** (`0 < x` ⟺ `x > 0`). Lo destapó el diferencial de emisión.
                let is_zero = |x: &Expr| matches!(&x.kind, ExprKind::IntLit(0));
                if lcat == 0 && rcat == 0 && (is_zero(lhs) || is_zero(rhs)) {
                    let (operand, cmp_op): (&Expr, BinOp) = if is_zero(rhs) {
                        (lhs, *op)
                    } else {
                        (rhs, reverse_cmp(*op))
                    };
                    self.expr(operand);
                    self.pop(1);
                    self.jump(IFEQ + cmp_index(cmp_op, when).unwrap(), target);
                    return;
                }
                let idx = cmp_index(*op, when).unwrap();
                let cat = lcat.max(rcat);
                self.expr(lhs);
                self.convert(lcat, cat);
                self.expr(rhs);
                self.convert(rcat, cat);
                match cat {
                    // Los operandos se consumen **antes** de saltar: el estado que se anota para
                    // el destino es el de después de la comparación, no el de antes.
                    0 => {
                        self.pop(2);
                        self.jump(IF_ICMPEQ + idx, target);
                    }
                    4 => {
                        // Referencias: solo `==`/`!=` (índices 0 y 1).
                        self.pop(2);
                        self.jump(IF_ACMPEQ + idx.min(1), target);
                    }
                    _ => {
                        // `long`/`float`/`double`: primero comparar a un `int` (-1/0/1), luego saltar.
                        self.op(LCMP + (cat - 1) * 2 - nan_variant(cat, *op));
                        self.pop(2);
                        self.push(VType::Int);
                        self.pop(1);
                        self.jump(IFEQ + idx, target);
                    }
                }
            }
            // `x instanceof T t`: además del test, la rama **verdadera** liga el pattern (recargar el
            // operando, `checkcast T`, `astore t`), justo donde javac lo emite (§14.30.2). Sin pattern
            // (slot `None`) cae al caso genérico de abajo, que solo emite el test booleano.
            ExprKind::InstanceOf { expr: operand, ty, slot: Some(slot), .. } => {
                let internal = match vtype_of_type(self.table, self.scope, ty) {
                    VType::Object(n) => n,
                    _ => "java/lang/Object".to_string(),
                };
                let cidx = self.pool.class(&internal);
                self.expr(operand);
                self.op(INSTANCEOF);
                self.u16(cidx);
                self.pop(1);
                self.push(VType::Int);
                self.pop(1); // el booleano lo consume el salto
                if when {
                    // Salta a `target` cuando es verdadero, ligando **antes** de saltar; si es falso, cae.
                    let fall = self.new_label();
                    self.jump(IFEQ, fall);
                    self.bind_pattern(operand, &internal, *slot);
                    self.jump(GOTO, target);
                    self.bind(fall);
                } else {
                    // Salta a `target` (el `else`) cuando es falso; en la caída (verdadero) liga el pattern.
                    self.jump(IFEQ, target);
                    self.bind_pattern(operand, &internal, *slot);
                }
            }
            // Cualquier otro booleano: se calcula y se compara contra cero.
            _ => {
                self.expr(cond);
                self.pop(1);
                self.jump(IFEQ + u8::from(when), target); // ifeq si `when` es falso, ifne si verdadero
            }
        }
    }

    /// Liga la variable de un *type pattern* (`instanceof T t`) en la rama en que el test dio verdadero:
    /// recarga el operando, lo `checkcast`ea a `T` y lo guarda en el slot de `t` — la secuencia
    /// `aload …; checkcast T; astore t` que emite javac. El operando se **reevalúa** (para un local/param
    /// es un `aload` idempotente, como en el corpus); un operando con efectos se re-ejecutaría, esquina
    /// que javac cubre con un temporal y que acá no se da.
    fn bind_pattern(&mut self, operand: &Expr, internal: &str, slot: u16) {
        self.expr(operand);
        let cidx = self.pool.class(internal);
        self.op(CHECKCAST);
        self.u16(cidx);
        self.pop(1);
        self.push(VType::Object(internal.to_string()));
        self.set_local(slot, VType::Object(internal.to_string()));
        self.store(4, slot);
    }

    /// Materializa una condición como el **valor** `0`/`1` (para `boolean b = x > 0;`).
    fn bool_value(&mut self, cond: &Expr) {
        // Se materializa igual que javac (`CondItem.load`): se **salta cuando la condición es falsa**
        // al camino que empuja `0`, y la caída (condición verdadera) empuja `1`. El orden importa para
        // la fidelidad byte-a-byte: p. ej. `!x` sale como `ifne …; iconst_1; goto …; iconst_0` (no como
        // `ifeq …; iconst_0; …; iconst_1`), que es lo que emite el compilador de referencia.
        let f = self.new_label();
        let end = self.new_label();
        self.branch_if(cond, f, false); // salto al `0` cuando la condición es falsa
        self.push_int(1); // caída: condición verdadera
        // Un destino de salto con la pila **no vacía**: el valor booleano ya está empujado.
        self.jump(GOTO, end);
        self.reachable = false;
        self.bind(f);
        self.pop(1); // los dos caminos empujan un solo valor, no dos
        self.push_int(0);
        self.bind(end);
    }

    // ---- campos, asignación, objetos ----

    /// Lo que necesita un `get*`/`put*`: clase dueña, nombre, descriptor, si es estático y su
    /// categoría. `None` si el símbolo no está resuelto (tipo externo opaco).
    fn field_info(&self, fid: SymbolId) -> Option<(String, String, String, bool, u8)> {
        let sym = self.table.symbol(fid);
        let owner = sym.owner?;
        let is_static = sym.modifiers.contains(&Modifier::Static);
        let name = sym.name.clone();
        let Some(Resolved::Field(rt)) = self.table.resolved(fid) else { return None };
        let rt = rt.clone();
        let desc = rtype_desc(self.table, &rt);
        Some((internal_name(self.table, owner), name, desc, is_static, category(&rt)))
    }

    /// Lee un campo. `recv` es el receptor explícito (`o.f`); sin él es un campo **implícito** de
    /// `this` — o estático, y entonces no se empuja receptor.
    fn read_field(&mut self, fid: SymbolId, recv: Option<&Expr>) {
        let Some((cls, name, desc, is_static, _)) = self.field_info(fid) else { return };
        let fref = self.pool.fieldref(&cls, &name, &desc);
        if is_static {
            self.op(GETSTATIC);
            self.u16(fref);
            self.push(vtype_of_desc(&desc));
        } else {
            match recv {
                Some(r) => self.expr(r),
                None => self.load_this(),
            }
            self.op(GETFIELD);
            self.u16(fref);
            self.pop(1); // el receptor
            self.push(vtype_of_desc(&desc));
        }
    }

    /// `a[i()] op= v` con un destino de **efectos**: el desugar deja este caso para el emisor
    /// (bajar `x op= v` a `x = x op v` re-evaluaría el destino y re-ejecutaría sus efectos). Se
    /// resuelve con **juego de pila**: `(arrayref, índice)` se evalúan una sola vez y se `dup2`ean,
    /// para cargar `a[i]`, combinarlo con `v` y volver a guardarlo. Deja el resultado en la pila
    /// (semántica de expresión; una sentencia lo descarta con `discard`).
    ///
    /// Cubre destinos **array** sin cruce de categorías: mismo tipo (`int[] += int`, incluidos
    /// `byte`/`short`/`char`, cuyo `*astore` trunca solo), o un shift con RHS `int`. Con promoción
    /// (`int[] += long`), elemento de referencia (`String[] +=`) o un campo con receptor de efectos,
    /// sigue cortando fuerte — mejor un error que bytecode a medias.
    fn compound_effectful(&mut self, e: &Expr, op: AssignOp, target: &Expr, value: &Expr) {
        let bail = |s: &mut Self| {
            s.unsupported(e.pos, "una asignación compuesta sobre un destino con efectos");
        };
        let binop = match op {
            AssignOp::Add => BinOp::Add,
            AssignOp::Sub => BinOp::Sub,
            AssignOp::Mul => BinOp::Mul,
            AssignOp::Div => BinOp::Div,
            AssignOp::Rem => BinOp::Rem,
            AssignOp::And => BinOp::BitAnd,
            AssignOp::Or => BinOp::BitOr,
            AssignOp::Xor => BinOp::BitXor,
            AssignOp::Shl => BinOp::Shl,
            AssignOp::Shr => BinOp::Shr,
            AssignOp::UShr => BinOp::UShr,
            AssignOp::Assign => return bail(self), // el `=` puro lo maneja `assign`; no debería llegar
        };
        let is_shift = matches!(binop, BinOp::Shl | BinOp::Shr | BinOp::UShr);
        // Por ahora solo destino array (un campo con efectos sigue cortando).
        let ExprKind::Index { array, index } = &target.kind else { return bail(self) };
        let tcat = category(&self.ty_of(target)); // categoría del elemento
        let vcat = category(&self.ty_of(value));
        let handled = tcat != 4 && if is_shift { vcat == 0 } else { tcat == vcat };
        if !handled {
            return bail(self);
        }
        let kind = array_kind(&self.ty_of(target));

        // (arrayref, índice) evaluados una vez y duplicados: sirven para el load y para el store.
        self.expr(array);
        self.expr(index);
        self.op(DUP2);
        let idx_t = self.stack[self.stack.len() - 1].clone();
        let aref_t = self.stack[self.stack.len() - 2].clone();
        self.push(aref_t);
        self.push(idx_t); // modelo: [.., aref, idx, aref, idx]

        self.op(IALOAD + kind); // carga a[i] con la copia de (aref, idx)
        self.pop(2);
        self.push_cat(tcat); // [.., aref, idx, a[i]]
        self.expr(value);
        self.arith(binop, tcat); // [.., aref, idx, result]

        // Deja el resultado en la pila: copia por debajo de (aref, idx) y guarda.
        self.op(if stack_width(tcat) == 2 { DUP2_X2 } else { DUP_X2 });
        self.max_stack = self.max_stack.max(self.height() + stack_width(tcat));
        self.op(IASTORE + kind);
        self.pop(3);
        self.push_cat(tcat); // sobrevive la copia como valor de la expresión
    }

    /// Emite `target = value`. `leave` dice si debe **dejar el valor** en la pila: una asignación
    /// usada como *expresión* sí (`a = b = 1`), como *sentencia* no.
    fn assign(&mut self, target: &Expr, value: &Expr, leave: bool) {
        // `a[i] = v` — no pasa por un binding: consume (arrayref, índice, valor).
        if let ExprKind::Index { array, index } = &target.kind {
            self.expr(array);
            self.expr(index);
            self.expr(value);
            self.widen_cat(category(&self.ty_of(target)));
            let kind = array_kind(&self.ty_of(target));
            if leave {
                // Usada como expresión (`b = a[i] = v`): hay que dejar el valor en la pila.
                // `dup_x2` (o `dup2_x2` si es categoría 2) copia el valor **por debajo** de
                // (arrayref, índice); el store consume esos tres y la copia sobrevive como
                // resultado. Modelamos el pico de `max_stack` a mano (la copia es transitoria).
                let cat = category(&self.ty_of(target));
                let vt = self.stack.last().cloned().unwrap_or(VType::Top);
                self.op(if stack_width(cat) == 2 { DUP2_X2 } else { DUP_X2 });
                self.max_stack = self.max_stack.max(self.height() + stack_width(cat));
                self.op(IASTORE + kind);
                self.pop(3); // arrayref, índice y valor (queda la copia duplicada)
                self.push(vt);
                return;
            }
            self.op(IASTORE + kind);
            self.pop(3); // arrayref, índice y valor
            return;
        }
        match target.binding {
            Some(Binding::Local { slot }) => {
                self.expr(value);
                let ty = self.ty_of(target);
                let cat = category(&ty);
                self.widen_cat(cat);
                if leave {
                    self.dup(cat);
                }
                let vt = vtype_of(self.table, &ty);
                self.set_local(slot, vt);
                self.store(cat, slot);
            }
            Some(Binding::Field(fid)) => {
                let Some((cls, name, desc, is_static, cat)) = self.field_info(fid) else { return };
                let fref = self.pool.fieldref(&cls, &name, &desc);
                if is_static {
                    self.expr(value);
                    self.widen_cat(cat);
                    if leave {
                        self.dup(cat);
                    }
                    self.op(PUTSTATIC);
                    self.u16(fref);
                    self.pop(1);
                } else {
                    // El receptor va **antes** del valor: `putfield` consume (objectref, valor).
                    match &target.kind {
                        ExprKind::Field { expr: recv, .. } => self.expr(recv),
                        // `f = v` dentro de un método de instancia
                        _ => self.load_this(),
                    }
                    self.expr(value);
                    self.widen_cat(cat);
                    self.op(PUTFIELD);
                    self.u16(fref);
                    self.pop(2); // el receptor y el valor
                }
            }
            _ => {}
        }
    }

    /// `new T(args)` → `new`, `dup`, args, `invokespecial T.<init>`. El `dup` es lo que deja la
    /// referencia en la pila después de que el constructor consuma la suya.
    fn new_object(&mut self, e: &Expr, args: &[Expr]) {
        let rt = self.ty_of(e);
        // Un tipo que no resolvió no tiene nombre interno que poner en el `new`. Callarse acá dejaba
        // el `athrow` siguiente sin nada que lanzar — y el `.class` salía igual.
        let (RType::Class(id) | RType::Parameterized { base: id, .. }) = rt else {
            self.unsupported(e.pos, "un `new` de un tipo que no se pudo resolver");
            return;
        };
        let cls = internal_name(self.table, id);
        let cidx = self.pool.class(&cls);
        let at = self.bytes.len(); // el offset del `new` **es** la identidad del objeto
        self.op(NEW);
        self.u16(cidx);
        self.push(VType::Uninit(at));
        self.dup1(); // la copia que sobrevive al constructor
        self.emit_args(e, args);
        // El descriptor sale del constructor que resolvió la pasada 2; sin él, `()V`.
        let desc = match e.binding {
            Some(Binding::Method(mid)) => match self.table.resolved(mid) {
                Some(Resolved::Method { params, .. }) => {
                    let ps: String = params.iter().map(|t| rtype_desc(self.table, t)).collect();
                    format!("({ps})V")
                }
                _ => "()V".to_string(),
            },
            _ => "()V".to_string(),
        };
        let mref = self.pool.methodref(&cls, "<init>", &desc);
        self.op(INVOKESPECIAL);
        self.u16(mref);
        self.pop(args.len() + 1); // los argumentos y la referencia duplicada
        // Corrido el constructor, el objeto **ya está inicializado** en todas partes (§4.10.2.4).
        self.initialized(at, &cls);
        // `new @A Foo()` — target 0x44, con el offset del `new` como identidad.
        self.code_type_annos(0x44, &(at as u16).to_be_bytes(), &e.type_annos);
    }

    /// `new T[n]` o `new T[]{a, b, c}`. El inicializador se emite elemento por elemento sobre el
    /// array recién creado: `dup`, índice, valor, `Xastore` — el `dup` es lo que deja la referencia
    /// para el siguiente (y para el resultado).
    fn new_array(&mut self, e: &Expr, dims: &[Option<Expr>], init: &Option<Vec<Expr>>) {
        let RType::Array(elem) = self.ty_of(e) else {
            self.unsupported(e.pos, "un array de más de una dimensión");
            return;
        };
        let kind = array_kind(&elem);
        // El target `0x44` de un array apunta al **inicio** de la creación (donde arranca el cálculo de
        // la dimensión), no al `newarray`/`anewarray` en sí — así lo hace javac.
        let off = self.bytes.len() as u16;

        // La longitud: la del inicializador, o la dimensión pedida.
        match init {
            Some(es) => self.push_int(es.len() as i32),
            None => match dims.first().and_then(|d| d.as_ref()) {
                Some(d) => self.expr(d),
                None => {
                    self.unsupported(e.pos, "un array sin dimensión ni inicializador");
                    return;
                }
            },
        }
        match &*elem {
            RType::Prim(p) => {
                self.op(NEWARRAY);
                self.op(newarray_code(*p));
            }
            other => {
                let internal = match vtype_of(self.table, other) {
                    VType::Object(n) => n,
                    _ => "java/lang/Object".to_string(),
                };
                let idx = self.pool.class(&internal);
                self.op(ANEWARRAY);
                self.u16(idx);
            }
        }
        let desc = rtype_desc(self.table, &RType::Array(elem.clone()));
        self.pop(1); // la longitud
        self.push(VType::Object(desc));
        self.code_type_annos(0x44, &off.to_be_bytes(), &e.type_annos);

        if let Some(es) = init {
            for (i, v) in es.iter().enumerate() {
                self.dup1();
                self.push_int(i as i32);
                self.expr(v);
                self.op(IASTORE + kind);
                self.pop(3); // arrayref, índice y valor
            }
        }
    }

    /// Registra las type annotations de una expresión de **posición-Code** (§4.7.20) en el buffer que
    /// irá al `RuntimeVisibleTypeAnnotations` **dentro del `Code`**: `target_type` (0x43 `instanceof`,
    /// 0x44 `new`, 0x47 cast) con el `target_info` ya armado (el offset del opcode, más el
    /// `type_argument_index` en el cast). Solo las retenidas en runtime (`rt`), con su `type_path`.
    fn code_type_annos(&mut self, target_type: u8, target_info: &[u8], annos: &[TypeUseAnnot]) {
        for ta in annos {
            if !self.rt.contains(anno_simple(&ta.annotation)) {
                continue;
            }
            if let Some(ann) = encode_annotation(self.pool, self.table, self.scope, &ta.annotation) {
                let path = serialize_type_path(&ta.path);
                self.code_type_annotations
                    .push(type_annotation_entry(target_type, target_info, &path, &ann));
            }
        }
    }

    /// Un cast: entre numéricos es una conversión (con el estrechamiento a `byte`/`char`/`short`
    /// que no tiene opcode propio y va tras un `int`); entre referencias, un `checkcast`.
    fn cast(&mut self, e: &Expr, inner: &Expr) {
        self.expr(inner);
        let to = self.ty_of(e);
        let from_cat = category(&self.ty_of(inner));
        let to_cat = category(&to);
        if to_cat == 4 {
            if let RType::Class(id) | RType::Parameterized { base: id, .. } = &to {
                let cls = internal_name(self.table, *id);
                let cidx = self.pool.class(&cls);
                let off = self.bytes.len() as u16; // offset del `checkcast` (target 0x47)
                self.op(CHECKCAST);
                self.u16(cidx);
                self.pop(1);
                self.push(VType::Object(cls));
                if !e.type_annos.is_empty() {
                    // `type_argument_target`: offset + `type_argument_index` (0 en un cast simple; solo
                    // un cast a tipo intersección `(A & B)` usaría otros índices, que no modelamos).
                    let mut ti = off.to_be_bytes().to_vec();
                    ti.push(0);
                    self.code_type_annos(0x47, &ti, &e.type_annos);
                }
            }
            return;
        }
        self.convert(from_cat, to_cat);
        // `byte`/`char`/`short` comparten la categoría `int`: su estrechamiento es explícito.
        match to {
            RType::Prim(PrimType::Byte) => self.op(0x91),  // i2b
            RType::Prim(PrimType::Char) => self.op(0x92),  // i2c
            RType::Prim(PrimType::Short) => self.op(0x93), // i2s
            _ => {}
        }
    }

    /// `x++` / `++x` / `x--` / `--x`. `leave` indica si la expresión debe dejar su valor: el **post**
    /// deja el **viejo** y el **pre** el nuevo — de ahí que el `dup` vaya antes o después de sumar.
    ///
    /// Para un local `int` alcanza `iinc`, que incrementa **sin tocar la pila**: por eso `x++` como
    /// valor es `iload` (deja el viejo) seguido de `iinc`, y `++x` es al revés. Los demás locales van
    /// por la forma general leer-modificar-escribir.
    fn incdec(&mut self, target: &Expr, up: bool, prefix: bool, leave: bool) {
        let Some(Binding::Local { slot }) = target.binding else { return };
        let ty = self.ty_of(target);
        let cat = category(&ty);
        let delta: i32 = if up { 1 } else { -1 };

        if cat == 0 && slot <= 255 && (-128..=127).contains(&delta) {
            if leave && !prefix {
                self.load(cat, slot); // post: el valor de la expresión es el **previo**
            }
            self.op(IINC);
            self.op(slot as u8);
            self.op(delta as i8 as u8);
            if leave && prefix {
                self.load(cat, slot); // pre: el ya incrementado
            }
            return;
        }

        // Forma general (`long`/`double`/`float`, o un slot que no entra en un byte).
        self.load(cat, slot);
        if leave && !prefix {
            self.dup(cat);
        }
        match cat {
            1 => self.push_long(delta as i64),
            2 => self.push_float(delta as f32),
            3 => self.push_double(delta as f64),
            _ => self.push_int(delta),
        }
        self.op(IADD + cat);
        self.pop(2);
        self.push_cat(cat);
        if leave && prefix {
            self.dup(cat);
        }
        let vt = vtype_of(self.table, &ty);
        self.set_local(slot, vt);
        self.store(cat, slot);
    }

    /// Peephole: una asignación en **descarte** de la forma `x = x + c` / `x = x - c` sobre un local
    /// **`int`** (la forma a la que el desugar baja `x++`/`x--`/`x += c`) se emite como un `iinc` —una
    /// instrucción, sin tocar la pila— igual que javac, en vez de `iload`/`const`/`iadd`/`istore`. Solo
    /// aplica a `int` exacto: un `byte`/`short`/`char` lleva un cast **truncante** que `iinc` no hace.
    /// Devuelve `true` si lo emitió (el llamador ya no debe emitir la asignación).
    fn try_iinc(&mut self, target: &Expr, value: &Expr) -> bool {
        let Some(Binding::Local { slot }) = target.binding else { return false };
        if !matches!(self.ty_of(target), RType::Prim(PrimType::Int)) {
            return false;
        }
        // El desugar envuelve el resultado en el cast de reducción `(int)`; se desenvuelve.
        let inner = match &value.kind {
            ExprKind::Cast { expr, .. } => expr.as_ref(),
            _ => value,
        };
        let ExprKind::Binary { op, lhs, rhs } = &inner.kind else { return false };
        let as_lit = |e: &Expr| if let ExprKind::IntLit(n) = &e.kind { Some(*n) } else { None };
        let is_var = |e: &Expr| matches!(e.binding, Some(Binding::Local { slot: s }) if s == slot);
        // `x + c` (conmutativo) da `+c`; `x - c` (la variable **a la izquierda**) da `-c`.
        let delta: i64 = match op {
            BinOp::Add if is_var(lhs) => match as_lit(rhs) { Some(c) => c, None => return false },
            BinOp::Add if is_var(rhs) => match as_lit(lhs) { Some(c) => c, None => return false },
            BinOp::Sub if is_var(lhs) => match as_lit(rhs) { Some(c) => -c, None => return false },
            _ => return false,
        };
        // `iinc` regular: índice en un byte, incremento en un byte con signo. Fuera de rango, se deja
        // que el llamador use la forma general (leer-modificar-escribir).
        let slot = self.rmap(slot); // reubicado si estamos dentro de una copia inline de `finally`
        if slot > 255 || !(-128..=127).contains(&delta) {
            return false;
        }
        self.op(IINC);
        self.op(slot as u8);
        self.op(delta as i8 as u8);
        self.use_slot(slot, 1);
        true
    }

    /// Duplica el tope de la pila (`dup2` si el valor es de categoría 2).
    fn dup(&mut self, cat: u8) {
        self.op(if stack_width(cat) == 2 { DUP2 } else { DUP });
        let t = self.stack.last().cloned().unwrap_or(VType::Top);
        self.push(t);
    }

    /// `dup` de un valor de categoría 1, conservando su tipo (una referencia recién creada).
    fn dup1(&mut self) {
        self.op(DUP);
        let t = self.stack.last().cloned().unwrap_or(VType::Top);
        self.push(t);
    }

    /// `aload_0` con el tipo que `this` tenga **en este punto**: en un constructor es `UninitThis`
    /// hasta que corre el `super()`, y el verificador lo distingue.
    fn load_this(&mut self) {
        self.op(ALOAD_0);
        let t = self.locals_t.first().cloned().unwrap_or(VType::Top);
        self.push(t);
    }

    // ---- emisión de bajo nivel ----

    fn push_int(&mut self, n: i32) {
        match n {
            -1 => self.op(ICONST_M1),
            0..=5 => self.op(0x03 + n as u8), // iconst_0..iconst_5
            -128..=127 => {
                self.op(BIPUSH);
                self.op(n as i8 as u8);
            }
            -32768..=32767 => {
                self.op(SIPUSH);
                self.u16(n as i16 as u16);
            }
            _ => {
                let idx = self.pool.integer(n);
                self.ldc(idx);
            }
        }
        self.push(VType::Int);
    }

    /// Emite la conversión numérica de la categoría `from` a `to`, si hacen falta. Los 12 opcodes
    /// `x2y` (JVMS §6, `i2l`=0x85 … `d2f`=0x90) están en bloques de 3 por tipo origen, saltándose el
    /// propio: origen `f`, destino `t` ⇒ `0x85 + f*3 + (t>f ? t-1 : t)`. Solo aplica a numéricos
    /// (categorías 0..3); una referencia (4) no se convierte.
    fn convert(&mut self, from: u8, to: u8) {
        if from == to || from > 3 || to > 3 {
            return;
        }
        let offset = if to > from { to - 1 } else { to };
        self.op(0x85 + from * 3 + offset);
        self.pop(1);
        self.push_cat(to);
    }

    /// **Ampliación primitiva implícita** (JLS §5.1.2). `self.expr(..)` deja el valor con el ancho
    /// de su *propio* tipo; si el contexto pide uno más ancho —retorno, inicialización o asignación
    /// de local, asignación de campo, paso de argumento, `array store`— hay que emitir el `i2l`/
    /// `i2d`/`l2d`/… o el class file queda **estructuralmente inválido**: un `ireturn` en un
    /// descriptor `()J` no lo arregla nadie después (#217).
    ///
    /// El origen se toma de la **pila**, no del tipo estático: es lo único que describe con certeza
    /// qué hay ahí (un `Integer` desempaquetado ya es `Int`, y `boolean`/`byte`/`char`/`short`
    /// también). Solo se amplía: `from < to` en la escala int(0) < long(1) < float(2) < double(3).
    /// Un destino de categoría 4 es una referencia — eso es *boxing*, y no se decide acá.
    fn widen_cat(&mut self, to: u8) {
        if to > 3 {
            return;
        }
        let Some(from) = self.stack.last().map(cat_of_vtype) else {
            return;
        };
        if from <= 3 && from < to {
            self.convert(from, to);
        }
    }

    /// `widen_cat` a partir del tipo destino.
    fn widen_to(&mut self, target: &RType) {
        self.widen_cat(category(target));
    }

    /// Los tipos de parámetro del método al que resolvió esta llamada — para ampliar cada argumento
    /// al tipo del parámetro. `None` si la llamada no resolvió: sin firma no hay a qué ampliar.
    fn param_types(&self, call: &Expr) -> Option<Vec<RType>> {
        let Some(Binding::Method(mid)) = call.binding else {
            return None;
        };
        match self.table.resolved(mid) {
            Some(Resolved::Method { params, .. }) => Some(params.clone()),
            _ => None,
        }
    }

    /// Emite los argumentos de una llamada, cada uno **ampliado** al tipo de su parámetro.
    fn emit_args(&mut self, call: &Expr, args: &[Expr]) {
        let ptys = self.param_types(call);
        for (i, a) in args.iter().enumerate() {
            self.expr(a);
            // `.get(i)`: con varargs la aridad puede no coincidir; sin parámetro, no se amplía.
            if let Some(p) = ptys.as_ref().and_then(|v| v.get(i)).cloned() {
                self.widen_to(&p);
            }
        }
    }

    /// Carga una constante **angosta** del pool: `ldc` si el índice entra en un byte, si no `ldc_w`.
    fn ldc(&mut self, idx: u16) {
        if idx <= 255 {
            self.op(LDC);
            self.op(idx as u8);
        } else {
            self.op(LDC_W);
            self.u16(idx);
        }
    }

    /// Empuja una **constante numérica** ya plegada al tipo destino `cat` (0=int, 1=long, 2=float,
    /// 3=double): el resultado de ensanchar una constante en contexto de asignación (`double s = 0`).
    fn push_num_as(&mut self, n: NumV, cat: u8) {
        match cat {
            1 => self.push_long(match n {
                NumV::Int(x) => x as i64,
                NumV::Long(x) => x,
                NumV::Float(x) => x as i64,
                NumV::Double(x) => x as i64,
            }),
            2 => self.push_float(match n {
                NumV::Int(x) => x as f32,
                NumV::Long(x) => x as f32,
                NumV::Float(x) => x,
                NumV::Double(x) => x as f32,
            }),
            3 => self.push_double(match n {
                NumV::Int(x) => x as f64,
                NumV::Long(x) => x as f64,
                NumV::Float(x) => x as f64,
                NumV::Double(x) => x,
            }),
            _ => self.push_int(match n {
                NumV::Int(x) => x,
                NumV::Long(x) => x as i32,
                NumV::Float(x) => x as i32,
                NumV::Double(x) => x as i32,
            }),
        }
    }

    /// `long`: `lconst_0`/`lconst_1` para 0 y 1, si no `ldc2_w`. Ocupa **dos** lugares de pila.
    fn push_long(&mut self, n: i64) {
        match n {
            0 | 1 => self.op(LCONST_0 + n as u8),
            _ => {
                let idx = self.pool.long(n);
                self.op(LDC2_W);
                self.u16(idx);
            }
        }
        self.push(VType::Long);
    }

    /// `double`: `dconst_0`/`dconst_1`, si no `ldc2_w`. Ocupa **dos** lugares de pila.
    fn push_double(&mut self, d: f64) {
        // Comparación por bits: `-0.0 == 0.0` es cierto, pero `dconst_0` empuja `+0.0`.
        if d.to_bits() == 0.0f64.to_bits() {
            self.op(DCONST_0);
        } else if d.to_bits() == 1.0f64.to_bits() {
            self.op(DCONST_0 + 1);
        } else {
            let idx = self.pool.double(d);
            self.op(LDC2_W);
            self.u16(idx);
        }
        self.push(VType::Double);
    }

    /// `float`: `fconst_0/1/2`, si no `ldc`.
    fn push_float(&mut self, f: f32) {
        if f.to_bits() == 0.0f32.to_bits() {
            self.op(FCONST_0);
        } else if f.to_bits() == 1.0f32.to_bits() {
            self.op(FCONST_0 + 1);
        } else if f.to_bits() == 2.0f32.to_bits() {
            self.op(FCONST_0 + 2);
        } else {
            let idx = self.pool.float(f);
            self.ldc(idx);
        }
        self.push(VType::Float);
    }

    fn load(&mut self, cat: u8, slot: u16) {
        let slot = self.rmap(slot);
        if slot <= 3 {
            self.op(ILOAD_0 + cat * 4 + slot as u8);
        } else {
            self.op(ILOAD + cat);
            self.op(slot as u8);
        }
        let w = stack_width(cat);
        // El tipo exacto lo sabe el slot; la categoría es el respaldo si nunca se anotó.
        match self.locals_t.get(slot as usize) {
            Some(VType::Top) | None => self.push_cat(cat),
            Some(t) => {
                let t = t.clone();
                self.push(t);
            }
        }
        self.use_slot(slot, w);
    }

    fn store(&mut self, cat: u8, slot: u16) {
        let slot = self.rmap(slot);
        if slot <= 3 {
            self.op(ISTORE_0 + cat * 4 + slot as u8);
        } else {
            self.op(ISTORE + cat);
            self.op(slot as u8);
        }
        let w = stack_width(cat);
        self.pop(1);
        self.use_slot(slot, w);
    }

    /// Un operador aritmético/de bits → su opcode, en la categoría del resultado.
    fn arith(&mut self, op: BinOp, cat: u8) {
        let bitwise = matches!(
            op,
            BinOp::Shl | BinOp::Shr | BinOp::UShr | BinOp::BitAnd | BinOp::BitOr | BinOp::BitXor
        );
        let base = match op {
            BinOp::Add => IADD,
            BinOp::Sub => IADD + 4,
            BinOp::Mul => IADD + 8,
            BinOp::Div => IADD + 12,
            BinOp::Rem => IADD + 16,
            BinOp::Shl => 0x78,
            BinOp::Shr => 0x7a,
            BinOp::UShr => 0x7c,
            BinOp::BitAnd => 0x7e,
            BinOp::BitOr => 0x80,
            BinOp::BitXor => 0x82,
            _ => IADD, // comparaciones/lógicos: próxima tanda (necesitan saltos)
        };
        // Los de bits solo tienen variante `int`/`long`.
        let opcode = if bitwise { base + if cat == 1 { 1 } else { 0 } } else { base + cat };
        self.op(opcode);
        // Consume dos operandos, deja uno (misma categoría).
        self.pop(2);
        self.push_cat(cat);
    }
}

#[cfg(test)]
mod tests {
    // El test **diferencial** cruza al runtime (la JVM propia) para ejecutar el `.class` generado.
    // El compilador en sí sigue desacoplado; esto es solo el arnés de verificación.
    use crate::jvm::class_file::ClassFile;
    use crate::jvm::interpreter::bytecode_interpreter::{Step, JVM};
    use crate::jvm::interpreter::frame::{Frame, Value};
    use crate::jvm::interpreter::metaspace::MetaspaceService;
    use crate::jvm::verifier::verify_method;
    use super::{
        ATHROW, BIPUSH, DUP, GETSTATIC, INVOKEDYNAMIC, INVOKESPECIAL, IRETURN, LDC, LOOKUPSWITCH,
        MONITORENTER, MONITOREXIT, TABLESWITCH,
    };
    use std::path::PathBuf;
    use std::sync::atomic::{AtomicUsize, Ordering};

    /// Compila por el **pipeline real** (`javac::compile`): las cinco pasadas, incluidos Flow y
    /// Desugar con la re-atribución. Devuelve un `(nombre, bytes)` por clase.
    fn compile_all(src: &str) -> Vec<(String, Vec<u8>)> {
        crate::javac::compile(src).expect("el fuente de prueba debe compilar sin errores")
    }

    /// Los bytes de la **primera** clase — para los tests de una sola clase que inspeccionan el
    /// `.class` directo.
    fn compile(src: &str) -> Vec<u8> {
        compile_all(src).into_iter().next().expect("al menos una clase").1
    }

    /// Escribe **todas** las clases de `src` en `dir` (cada una por su nombre), para que las
    /// referencias entre ellas —un `switch`-enum y su holder `C$1`— resuelvan al cargar.
    fn write_classes(src: &str, dir: &std::path::Path) {
        for (internal, bytes) in compile_all(src) {
            let simple = internal.rsplit('/').next().unwrap_or(&internal).to_string();
            std::fs::write(dir.join(format!("{simple}.class")), &bytes).unwrap();
        }
    }

    static COUNTER: AtomicUsize = AtomicUsize::new(0);

    /// Compila `src`, escribe el `.class`, lo carga en la **JVM propia** y ejecuta el método
    /// estático `method(args)` — el diferencial del loop cerrado.
    fn run_int(src: &str, class: &str, method: &str, args: Vec<i32>) -> i32 {
        match run(src, class, method, args.into_iter().map(Value::Int).collect()) {
            Some(Value::Int(n)) => n,
            other => panic!("se esperaba un int, salió {other:?}"),
        }
    }

    fn run_long(src: &str, class: &str, method: &str, args: Vec<i64>) -> i64 {
        match run(src, class, method, args.into_iter().map(Value::Long).collect()) {
            Some(Value::Long(n)) => n,
            other => panic!("se esperaba un long, salió {other:?}"),
        }
    }

    fn run_double(src: &str, class: &str, method: &str, args: Vec<f64>) -> f64 {
        match run(src, class, method, args.into_iter().map(Value::Double).collect()) {
            Some(Value::Double(d)) => d,
            other => panic!("se esperaba un double, salió {other:?}"),
        }
    }

    /// Compila y corre el **verificador JVMS-estricto** propio sobre cada método. Cuando hay
    /// `StackMapTable`, el verificador hace *cross-check* de nuestros frames contra su propia
    /// inferencia por punto fijo: una tabla mal calculada **falla acá**. Es el oráculo del B3.
    fn verify_all(src: &str, class: &str) {
        let n = COUNTER.fetch_add(1, Ordering::Relaxed);
        let dir = std::env::temp_dir().join(format!("javac_vf_{}_{n}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        write_classes(src, &dir);
        let path = dir.join(format!("{class}.class"));
        let p = path.to_str().unwrap();

        let loaded = ClassFile::from_path(p).expect("el .class debe parsear");
        let name = loaded.class_name(loaded.this_class).expect("nombre").to_string();
        let mut ms = MetaspaceService::new(vec![PathBuf::from("boot")], vec![dir.clone()]);
        ms.add(name, loaded);

        let cf = ClassFile::from_path(p).expect("el .class debe parsear");
        for m in &cf.methods {
            let mname = cf.utf8(m.name_index).unwrap_or("?").to_string();
            if let Err(e) = verify_method(&mut ms, &cf, m) {
                let _ = std::fs::remove_dir_all(&dir);
                panic!("el verificador rechazó `{mname}`: {e:?}");
            }
        }
        let _ = std::fs::remove_dir_all(&dir);
    }

    /// El `Code` de un método del `.class` recién compilado: su bytecode y cuántos handlers lleva.
    /// Mirar el atributo de verdad es más honesto que buscar un byte suelto en el archivo entero,
    /// que también podría estar en el constant pool.
    fn code_of(src: &str, class: &str, method: &str) -> (Vec<u8>, usize) {
        let n = COUNTER.fetch_add(1, Ordering::Relaxed);
        let dir = std::env::temp_dir().join(format!("javac_co_{}_{n}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        write_classes(src, &dir);
        let path = dir.join(format!("{class}.class"));
        let cf = ClassFile::from_path(path.to_str().unwrap()).expect("el .class debe parsear");
        let m = cf
            .methods
            .iter()
            .find(|m| cf.utf8(m.name_index) == Some(method))
            .expect("el método existe");
        let code = cf.member_code(m).expect("el método tiene Code");
        let out = (code.code.clone(), code.exception_table.len());
        let _ = std::fs::remove_dir_all(&dir);
        out
    }

    fn run(src: &str, class: &str, method: &str, args: Vec<Value>) -> Option<Value> {
        let n = COUNTER.fetch_add(1, Ordering::Relaxed);
        let dir = std::env::temp_dir().join(format!("javac_cg_{}_{n}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        write_classes(src, &dir);
        let path = dir.join(format!("{class}.class"));

        let cf = ClassFile::from_path(path.to_str().unwrap()).expect("el .class debe parsear");
        let name = cf.class_name(cf.this_class).expect("nombre de clase").to_string();
        let desc = cf
            .methods
            .iter()
            .find(|m| cf.utf8(m.name_index) == Some(method))
            .and_then(|m| cf.utf8(m.descriptor_index))
            .expect("el método existe")
            .to_string();

        let mut ms = MetaspaceService::new(vec![PathBuf::from("boot")], vec![dir.clone()]);
        ms.add(name.clone(), cf);
        let id = ms.resolve_method(&name, method, &desc).expect("método resuelto");
        let max_locals = ms.max_locals(id);
        // `for_call` (no `new`) porque un `long`/`double` ocupa **dos** slots: el argumento
        // siguiente arranca uno más allá, que es justo lo que emite el codegen (`dload_2`).
        let widths: Vec<usize> = args
            .iter()
            .map(|v| if matches!(v, Value::Long(_) | Value::Double(_)) { 2 } else { 1 })
            .collect();
        let mut interp = JVM::new(ms, Frame::for_call(id, max_locals, args, &widths));

        for _ in 0..100_000 {
            if let Step::Return(v) = interp.exec().step() {
                let _ = std::fs::remove_dir_all(&dir);
                return v;
            }
        }
        panic!("el método no terminó (¿bucle?)");
    }

    #[test]
    fn class_file_has_the_v69_header() {
        let bytes = compile("public class Add { public static int add(int a, int b) { return a + b; } }");
        assert_eq!(&bytes[0..4], &[0xCA, 0xFE, 0xBA, 0xBE], "magic");
        assert_eq!(u16::from_be_bytes([bytes[6], bytes[7]]), 69, "major version JDK 25");
    }

    // ---- el loop cerrado: compilar → correr en la JVM propia → comparar ----

    #[test]
    fn compiles_and_runs_integer_arithmetic() {
        let src = "public class Add { public static int add(int a, int b) { return a + b; } }";
        assert_eq!(run_int(src, "Add", "add", vec![2, 3]), 5);
    }

    #[test]
    fn compiles_and_runs_subtraction() {
        let src = "public class Add { public static int sub(int a, int b) { return a - b; } }";
        assert_eq!(run_int(src, "Add", "sub", vec![7, 4]), 3);
    }

    #[test]
    fn an_array_assignment_used_as_a_value_leaves_it_on_the_stack() {
        // E2: `b = (a[0] = 7)` — usar el **valor** de una asignación a un array (dup_x2).
        // Antes bailaba: "el generador de bytecode todavía no soporta usar el valor de una
        // asignación a un array". El valor del store debe sobrevivir para el `+`.
        let src = "public class ArrE { public static int run() {
            int[] a = new int[1]; int b = (a[0] = 7); return b + a[0];
        } }";
        assert_eq!(run_int(src, "ArrE", "run", vec![]), 14);
    }

    #[test]
    fn an_array_assignment_value_works_for_category_2_elements() {
        // Rama dup2_x2: el elemento es `long` (categoría 2, ocupa dos slots).
        let src = "public class ArrL { public static long run() {
            long[] a = new long[1]; long b = (a[0] = 100L); return b + a[0];
        } }";
        assert_eq!(run(src, "ArrL", "run", vec![]), Some(Value::Long(200)));
    }

    #[test]
    fn a_compound_assignment_on_an_effectful_index_evaluates_it_once() {
        // E1: `a[idx()] += 5` — el índice tiene efectos, debe evaluarse **una sola vez** (dup2).
        // Antes bailaba: "todavía no soporta una asignación compuesta sobre un destino con efectos".
        let src = "public class CompA {
            static int calls = 0;
            static int idx() { calls++; return 1; }
            public static int run() {
                int[] a = {10, 20, 30}; a[idx()] += 5;
                return a[1] * 100 + calls; // 25*100 + 1 (idx llamado una vez)
            }
        }";
        assert_eq!(run_int(src, "CompA", "run", vec![]), 2501);
    }

    #[test]
    fn a_compound_assignment_on_a_byte_array_truncates_via_the_store() {
        // Elemento `byte`: `bastore` trunca solo, sin i2b explícito. 100+50=150 → (byte)150 = -106.
        let src = "public class CompB {
            static int c = 0; static int idx() { c++; return 0; }
            public static int run() {
                byte[] b = new byte[1]; b[0] = 100; b[idx()] += 50;
                return b[0] + c * 1000; // -106 + 1000
            }
        }";
        assert_eq!(run_int(src, "CompB", "run", vec![]), 894);
    }

    #[test]
    fn a_compound_shift_assignment_on_an_effectful_index_works() {
        // Shift con RHS `int`: `a[idx()] <<= 4`. 3<<4 = 48, idx una vez.
        let src = "public class CompS {
            static int c = 0; static int idx() { c++; return 0; }
            public static int run() { int[] a = {3}; a[idx()] <<= 4; return a[0] + c; }
        }";
        assert_eq!(run_int(src, "CompS", "run", vec![]), 49);
    }

    #[test]
    fn a_compound_array_assignment_used_as_a_value_leaves_the_result() {
        // Como expresión: `int b = (a[idx()] += 5)`. a[0]=15, b=15, idx una vez → 15+15+1.
        let src = "public class CompE {
            static int c = 0; static int idx() { c++; return 0; }
            public static int run() { int[] a = {10}; int b = (a[idx()] += 5); return b + a[0] + c; }
        }";
        assert_eq!(run_int(src, "CompE", "run", vec![]), 31);
    }

    #[test]
    fn an_embedded_switch_expression_yields_its_value() {
        // E3: `1 + switch(y){…}` — la switch-expr está **embebida** (no en posición lowerable), así
        // que la emite el codegen dejando su valor en la pila. Antes bailaba "switch-expresión embebida".
        let src = "public class SwE {
            public static int run() {
                int y = 2; return 1 + switch (y) { case 1 -> 10; case 2 -> 20; default -> 0; };
            }
        }";
        assert_eq!(run_int(src, "SwE", "run", vec![]), 21);
    }

    #[test]
    fn an_embedded_switch_expression_with_a_throw_arm() {
        // Un brazo `-> throw …` transfiere el control por su cuenta; los demás yieldan. y=2 → 20+5.
        let src = "public class SwET {
            public static int run() {
                int y = 2;
                return switch (y) { case 1 -> throw new RuntimeException(); case 2 -> 20; default -> 0; } + 5;
            }
        }";
        assert_eq!(run_int(src, "SwET", "run", vec![]), 25);
    }

    #[test]
    fn an_embedded_sparse_switch_expression_uses_lookupswitch() {
        // Claves ralas → `lookupswitch`; embebida en un `*`.
        let src = "public class SwES {
            public static int run() {
                int y = 1000; return switch (y) { case 1 -> 1; case 1000 -> 5; default -> 0; } * 2;
            }
        }";
        assert_eq!(run_int(src, "SwES", "run", vec![]), 10);
    }

    #[test]
    fn an_embedded_switch_expression_of_long_type() {
        // Resultado de categoría 2 (`long`): el `vt`/convert del `yield` debe ser `Long`.
        let src = "public class SwEL {
            public static long run() {
                int y = 2; return 1L + switch (y) { case 1 -> 10L; case 2 -> 20L; default -> 0L; };
            }
        }";
        assert_eq!(run(src, "SwEL", "run", vec![]), Some(Value::Long(21)));
    }

    #[test]
    fn compiles_locals_and_static_calls() {
        // `compute()` usa un local `r` y llama a `add`/`sub` (invokestatic anidado).
        let src = "public class Add {
            public static int add(int a, int b) { return a + b; }
            public static int sub(int a, int b) { return a - b; }
            public static int compute() { int r = add(2, 3); return sub(r, 1); }
        }";
        assert_eq!(run_int(src, "Add", "compute", vec![]), 4);
    }

    #[test]
    fn runs_a_mix_of_arithmetic_operators() {
        let src = "public class M { public static int f(int a, int b) { return a * b + a - b; } }";
        // 5*3 + 5 - 3 = 17
        assert_eq!(run_int(src, "M", "f", vec![5, 3]), 17);
    }

    #[test]
    fn precedence_is_respected_in_the_bytecode() {
        let src = "public class M { public static int f(int a, int b, int c) { return a + b * c; } }";
        // 2 + 3*4 = 14 (no (2+3)*4=20)
        assert_eq!(run_int(src, "M", "f", vec![2, 3, 4]), 14);
    }

    #[test]
    fn large_int_literals_use_the_constant_pool() {
        let src = "public class M { public static int big() { return 1000000; } }";
        assert_eq!(run_int(src, "M", "big", vec![]), 1_000_000);
    }

    // ---- tipos anchos (categoría 2) y `String` ----

    #[test]
    fn compiles_and_runs_long_arithmetic() {
        let src = "public class M { public static long add(long a, long b) { return a + b; } }";
        assert_eq!(run_long(src, "M", "add", vec![4_000_000_000, 1]), 4_000_000_001);
    }

    #[test]
    fn a_long_literal_too_big_for_int_uses_ldc2_w() {
        let src = "public class M { public static long big() { return 10000000000L; } }";
        assert_eq!(run_long(src, "M", "big", vec![]), 10_000_000_000);
    }

    #[test]
    fn mixing_an_int_literal_into_long_arithmetic_widens_it() {
        // `a + 1`: el `1` es un `int` y necesita `i2l` antes del `ladd` (§5.6.2). Sin la promoción
        // el bytecode ni siquiera pasa un verificador.
        let src = "public class M { public static long inc(long a) { return a + 1; } }";
        assert_eq!(run_long(src, "M", "inc", vec![41]), 42);
    }

    #[test]
    fn compiles_and_runs_double_arithmetic() {
        // `b` vive en el slot **2**: un `double` ocupa dos.
        let src = "public class M { public static double avg(double a, double b) { return (a + b) / 2.0; } }";
        assert_eq!(run_double(src, "M", "avg", vec![3.0, 5.0]), 4.0);
    }

    #[test]
    fn widening_from_long_to_double_is_emitted() {
        // Entra un `long` y sale un `double`: hace falta `l2d` antes del `dmul`.
        let src = "public class M { public static double half(long n) { return n * 0.5; } }";
        match run(src, "M", "half", vec![Value::Long(7)]) {
            Some(Value::Double(d)) => assert_eq!(d, 3.5),
            other => panic!("se esperaba un double, salió {other:?}"),
        }
    }

    #[test]
    fn implicit_widening_reaches_all_five_assignment_contexts() {
        // #217 — la ampliación primitiva implícita (§5.1.2) no es solo la promoción binaria: hace
        // falta en **cinco** posiciones más, y en cada una su ausencia produce un class file
        // estructuralmente inválido (un `ireturn` en un `()J`), no un resultado equivocado.
        // El harness verifica antes de correr, así que cada caso falla si falta el `i2l`.
        let long_of = |body: &str| {
            let src = format!(
                "public class M {{ static int n = 3; static long f; static long id(long v) {{ return v; }}                  public static long test() {{ {body} }} }}"
            );
            run_long(&src, "M", "test", vec![])
        };
        assert_eq!(long_of("return M.n;"), 3); // 1. return
        assert_eq!(long_of("long x = M.n; return x;"), 3); // 2. init de local
        assert_eq!(long_of("long x = 0L; x = M.n; return x;"), 3); // 3. asignación a local
        assert_eq!(long_of("M.f = M.n; return M.f;"), 3); // 4. asignación a campo
        assert_eq!(long_of("return M.id(M.n);"), 3); // 5. paso de argumento
        assert_eq!(
            long_of("long[] a = new long[1]; a[0] = M.n; return a[0];"),
            3
        ); // 6. array store
    }

    #[test]
    fn implicit_widening_also_emits_i2d_not_just_i2l() {
        // El mismo agujero llegaba a `double`: se reportó como "falta el `i2l`", pero el destino
        // más ancho puede ser cualquiera de la escala int < long < float < double.
        let src = "public class M { static int n = 3; static double take(double v) { return v; }                    public static double test() { double x = M.n; return M.take(x) + M.n; } }";
        match run(src, "M", "test", vec![]) {
            Some(Value::Double(d)) => assert_eq!(d, 6.0),
            other => panic!("se esperaba un double, salió {other:?}"),
        }
    }

    // ---- objetos: `new`, campos, `invokevirtual` ----

    #[test]
    fn creates_an_object_and_reads_a_field_through_a_getter() {
        // Ejercita `new` + `dup` + `invokespecial <init>`, `putfield`, `getfield` e `invokevirtual`.
        let src = "public class M { int v; M(int x) { this.v = x; } int get() { return this.v; } \
                   public static int test() { M m = new M(7); return m.get(); } }";
        assert_eq!(run_int(src, "M", "test", vec![]), 7);
    }

    #[test]
    fn mutates_a_field_through_a_compound_assignment() {
        // `this.v += n` es el caso que antes emitía `pop`: ahora baja a un
        // `aload_0, aload_0, getfield, iload, iadd, putfield` como el de javac.
        let src = "public class M { int v; M(int x) { this.v = x; } void add(int n) { this.v += n; } \
                   int get() { return this.v; } \
                   public static int test() { M m = new M(10); m.add(5); return m.get(); } }";
        assert_eq!(run_int(src, "M", "test", vec![]), 15);
    }

    #[test]
    fn assigns_to_a_local_as_a_statement() {
        let src = "public class M { public static int test() { int x = 1; x = 41; return x + 1; } }";
        assert_eq!(run_int(src, "M", "test", vec![]), 42);
    }

    // ---- flujo de control ----

    #[test]
    fn compiles_an_if_else() {
        let src = "public class M { public static int sign(int a) { \
                   if (a > 0) { return 1; } else if (a < 0) { return -1; } return 0; } }";
        assert_eq!(run_int(src, "M", "sign", vec![7]), 1);
        assert_eq!(run_int(src, "M", "sign", vec![-7]), -1);
        assert_eq!(run_int(src, "M", "sign", vec![0]), 0);
    }

    #[test]
    fn compiles_a_while_loop() {
        let src = "public class M { public static int sum(int n) { \
                   int s = 0; int i = 1; while (i <= n) { s = s + i; i = i + 1; } return s; } }";
        assert_eq!(run_int(src, "M", "sum", vec![10]), 55);
    }

    #[test]
    fn compiles_a_for_loop_with_increment() {
        // Ejercita el `for` completo y el `i++` que baja a `i = (int)(i + 1)`.
        let src = "public class M { public static int fact(int n) { \
                   int r = 1; for (int i = 2; i <= n; i++) { r = r * i; } return r; } }";
        assert_eq!(run_int(src, "M", "fact", vec![5]), 120);
    }

    #[test]
    fn incrementing_an_int_local_emits_iinc() {
        // El diferencial de emisión mostró que `x++`/`x += c`/`x -= c` sobre un `int` local deben ir
        // por un `iinc` (una instrucción), como javac —no `iload`/`const`/`iadd`/`istore`—.
        let (inc, _) = code_of("public class C { public void m(int x) { x++; } }", "C", "m");
        assert!(inc.contains(&super::IINC), "`x++` debe emitir iinc");
        let (add, _) = code_of("public class C { public void m(int x) { x += 5; } }", "C", "m");
        assert!(add.contains(&super::IINC), "`x += 5` debe emitir iinc");
        let (dec, _) = code_of("public class C { public void m(int x) { x -= 3; } }", "C", "m");
        assert!(dec.contains(&super::IINC), "`x -= 3` debe emitir iinc");
        // El `i++` del `update` de un `for` también.
        let (loop_, _) = code_of(
            "public class C { public int s(int n) { int t = 0; for (int i = 0; i < n; i++) t += i; return t; } }",
            "C",
            "s",
        );
        assert!(loop_.contains(&super::IINC), "el `i++` del for debe emitir iinc");
        // Un `byte` lleva un cast **truncante**: NO usa iinc.
        let (by, _) = code_of("public class C { public void m(byte b) { b++; } }", "C", "m");
        assert!(!by.contains(&super::IINC), "`byte b++` no usa iinc (cast truncante)");
        // Un método sin incremento no lo emite.
        let (id, _) = code_of("public class C { public int id(int x) { return x; } }", "C", "id");
        assert!(!id.contains(&super::IINC));
    }

    #[test]
    fn compiles_break_and_continue() {
        let src = "public class M { public static int f(int n) { \
                   int s = 0; for (int i = 0; i < n; i++) { \
                   if (i == 3) { continue; } if (i == 6) { break; } s = s + i; } return s; } }";
        // 0+1+2+4+5 = 12 (saltea el 3, corta en el 6)
        assert_eq!(run_int(src, "M", "f", vec![10]), 12);
    }

    #[test]
    fn short_circuit_and_or_are_jumps() {
        let src = "public class M { public static int f(int a, int b) { \
                   if (a > 0 && b > 0) { return 1; } if (a > 0 || b > 0) { return 2; } return 3; } }";
        assert_eq!(run_int(src, "M", "f", vec![1, 1]), 1);
        assert_eq!(run_int(src, "M", "f", vec![1, -1]), 2);
        assert_eq!(run_int(src, "M", "f", vec![-1, -1]), 3);
    }

    #[test]
    fn a_recursive_method_with_a_branch() {
        let src = "public class M { public static int fib(int n) { \
                   if (n < 2) { return n; } return fib(n - 1) + fib(n - 2); } }";
        assert_eq!(run_int(src, "M", "fib", vec![10]), 55);
    }

    // ---- incremento/decremento en posición de valor ----

    #[test]
    fn post_increment_yields_the_old_value_and_still_increments() {
        // El bug histórico: `y = x++` guardaba el viejo en `y` pero **nunca incrementaba `x`**.
        let src = "public class M { public static int post(int x) { int y = x++; return x * 100 + y; } }";
        assert_eq!(run_int(src, "M", "post", vec![5]), 605); // x=6, y=5
    }

    #[test]
    fn pre_increment_yields_the_new_value() {
        let src = "public class M { public static int pre(int x) { int y = ++x; return x * 100 + y; } }";
        assert_eq!(run_int(src, "M", "pre", vec![5]), 606); // x=6, y=6
    }

    #[test]
    fn post_decrement_in_value_position() {
        let src = "public class M { public static int f(int x) { int y = x--; return x * 100 + y; } }";
        assert_eq!(run_int(src, "M", "f", vec![5]), 405); // x=4, y=5
    }

    #[test]
    fn increment_of_a_long_local_uses_the_general_form() {
        // Un `long` no entra en `iinc`: va por leer-modificar-escribir.
        let src = "public class M { public static long f(long x) { long y = x++; return x * 100 + y; } }";
        assert_eq!(run_long(src, "M", "f", vec![5]), 605);
    }

    #[test]
    fn increment_in_value_position_passes_the_verifier() {
        verify_all(
            "public class M { public static int f(int n) { int s = 0; \
             for (int i = 0; i < n; i++) { s = s + i++; } return s; } }",
            "M",
        );
    }

    // ---- miembros sintetizados de un `record` ----

    #[test]
    fn a_record_is_constructible_and_its_accessors_run() {
        // Cierra el circuito: los miembros implícitos que sintetiza el desugar se emiten de verdad
        // y la JVM propia los ejecuta.
        let src = "public record M(int a, int b) { public static int test() { \
                   M m = new M(3, 4); return m.a() * 10 + m.b(); } }";
        assert_eq!(run_int(src, "M", "test", vec![]), 34);
    }

    #[test]
    fn a_record_passes_the_strict_verifier() {
        verify_all(
            "public record M(int a, int b) { public static int test() { M m = new M(1, 2); return m.a(); } }",
            "M",
        );
    }

    #[test]
    fn a_record_gets_its_object_methods_via_object_methods_bootstrap() {
        // `equals`/`hashCode`/`toString` son un único `invokedynamic` a `ObjectMethods.bootstrap`
        // (§8.10.2). Se prueban con un componente primitivo y uno de referencia.
        let src = "public record P(int a, String b) {}";
        assert!(code_of(src, "P", "equals").0.contains(&INVOKEDYNAMIC));
        assert!(code_of(src, "P", "hashCode").0.contains(&INVOKEDYNAMIC));
        assert!(code_of(src, "P", "toString").0.contains(&INVOKEDYNAMIC));
        verify_all(src, "P");
    }

    #[test]
    fn a_record_can_override_its_object_methods() {
        // Un `toString` declarado a mano **gana** (§8.10.4): no se sintetiza el del bootstrap.
        let src = "public record P(int a) { public String toString() { return \"P\"; } }";
        // El `toString` propio no lleva invokedynamic; `equals`/`hashCode` sí siguen sintetizándose.
        assert!(!code_of(src, "P", "toString").0.contains(&INVOKEDYNAMIC));
        assert!(code_of(src, "P", "equals").0.contains(&INVOKEDYNAMIC));
        verify_all(src, "P");
    }

    // ---- clases internas de instancia (captura de `this$0`) ----

    #[test]
    fn an_inner_class_reads_an_enclosing_field() {
        // `Inner.get()` lee `f` de `Outer` vía el `this$0` sintético; `new Inner()` en un método de
        // instancia de `Outer` pasa `this`.
        let src = "public class Outer { int f; Outer() { f = 7; } \
                   class Inner { int get() { return f; } } \
                   int use() { return new Inner().get(); } \
                   public static int test() { return new Outer().use(); } }";
        assert_eq!(run_int(src, "Outer", "test", vec![]), 7);
    }

    #[test]
    fn an_inner_class_calls_an_enclosing_method() {
        let src = "public class Outer { int base() { return 10; } \
                   class Inner { int get() { return base() + 5; } } \
                   int use() { return new Inner().get(); } \
                   public static int test() { return new Outer().use(); } }";
        assert_eq!(run_int(src, "Outer", "test", vec![]), 15);
    }

    #[test]
    fn an_inner_class_passes_the_strict_verifier() {
        let src = "public class Outer { int f; Outer() { f = 3; } \
                   class Inner { int get() { return f; } } \
                   int use() { return new Inner().get(); } }";
        verify_all(src, "Outer");
        verify_all(src, "Outer$Inner");
    }

    #[test]
    fn qualified_this_reads_the_enclosing_field() {
        // `Outer.this.f` desde la interna → `this.this$0.f` (§15.8.4). Aunque `f` no está sombreado
        // acá, el `Outer.this` explícito debe bajar a la cadena de `this$0` igual que el acceso
        // implícito.
        let src = "public class Outer { int f; Outer() { f = 9; } \
                   class Inner { int get() { return Outer.this.f; } } \
                   int use() { return new Inner().get(); } \
                   public static int test() { return new Outer().use(); } }";
        assert_eq!(run_int(src, "Outer", "test", vec![]), 9);
        verify_all(src, "Outer$Inner");
    }

    #[test]
    fn qualified_this_disambiguates_a_shadowed_field() {
        // `f` está sombreado por el campo de `Inner`; `Outer.this.f` alcanza el del enclosing (8),
        // mientras `this.f` es el de `Inner` (3). La suma comprueba que cada `this` va a su clase.
        let src = "public class Outer { int f; Outer() { f = 8; } \
                   class Inner { int f; Inner() { f = 3; } \
                       int get() { return Outer.this.f * 10 + this.f; } } \
                   int use() { return new Inner().get(); } \
                   public static int test() { return new Outer().use(); } }";
        assert_eq!(run_int(src, "Outer", "test", vec![]), 83);
    }

    #[test]
    fn a_qualified_new_creates_an_inner_from_an_explicit_enclosing() {
        // `o.new Inner()` (§15.9.2): la instancia envolvente es el calificador `o`, no `this`. Se
        // construye desde un método **estático**, donde no hay `this` que pasar.
        let src = "public class Outer { int f; Outer() { f = 6; } \
                   class Inner { int get() { return f; } } \
                   public static int test() { Outer o = new Outer(); return o.new Inner().get(); } }";
        assert_eq!(run_int(src, "Outer", "test", vec![]), 6);
        verify_all(src, "Outer$Inner");
    }

    #[test]
    fn a_two_level_inner_reads_a_grandparent_field() {
        // `C` (interna de `B`, interna de `A`) lee `f` de `A` **sin cualificar** (§8.1.3): el desugar
        // encadena dos `this$0` → `this.this$0.this$0.f`. `new C()`/`new B()` construyen la cadena.
        let src = "public class A { int f; A() { f = 42; } \
                   class B { class C { int get() { return f; } } \
                       int useC() { return new C().get(); } } \
                   int useB() { return new B().useC(); } \
                   public static int test() { return new A().useB(); } }";
        assert_eq!(run_int(src, "A", "test", vec![]), 42);
        verify_all(src, "A$B");
        verify_all(src, "A$B$C");
    }

    #[test]
    fn a_two_level_inner_calls_a_grandparent_method() {
        // Igual que el anterior pero con una **llamada** a un método de `A` desde `C`: el receptor
        // implícito se reescribe a `this.this$0.this$0.base()`.
        let src = "public class A { int base() { return 100; } \
                   class B { class C { int get() { return base() + 7; } } \
                       int useC() { return new C().get(); } } \
                   int useB() { return new B().useC(); } \
                   public static int test() { return new A().useB(); } }";
        assert_eq!(run_int(src, "A", "test", vec![]), 107);
    }

    // ---- clases locales (captura de locales `val$`) ----

    #[test]
    fn a_local_class_captures_an_effectively_final_local() {
        // `L` captura el local `c`: se baja a un campo `val$c` + parámetro de constructor; `new L()`
        // lo pasa. Corre en la JVM propia.
        let src = "public class M { public static int test() { \
                   int c = 5; \
                   class L { int get() { return c + 1; } } \
                   return new L().get(); } }";
        assert_eq!(run_int(src, "M", "test", vec![]), 6);
    }

    #[test]
    fn a_local_class_captures_multiple_locals() {
        let src = "public class M { public static int test() { \
                   int a = 3; int b = 4; \
                   class L { int sum() { return a * 10 + b; } } \
                   return new L().sum(); } }";
        assert_eq!(run_int(src, "M", "test", vec![]), 34);
    }

    #[test]
    fn a_local_class_passes_the_strict_verifier() {
        let src = "public class M { public static int test() { \
                   int base = 10; \
                   class L { int add(int x) { return base + x; } } \
                   return new L().add(3); } }";
        verify_all(src, "M");
        verify_all(src, "M$1L");
    }

    #[test]
    fn a_local_class_captures_both_the_enclosing_instance_and_a_local() {
        // En un método de **instancia**, `L.get()` lee `f` (campo del enclosing, vía `this$0`) y
        // `local` (capturado, vía `val$local`): el ctor es `L(Outer this$0, int val$local)`.
        let src = "public class Outer { int f; Outer() { f = 100; } \
                   int use() { int local = 5; \
                   class L { int get() { return f + local; } } \
                   return new L().get(); } \
                   public static int test() { return new Outer().use(); } }";
        assert_eq!(run_int(src, "Outer", "test", vec![]), 105);
        verify_all(src, "Outer");
        verify_all(src, "Outer$1L");
    }

    #[test]
    fn two_local_classes_with_the_same_name_do_not_collide() {
        // Dos `L` homónimas en métodos distintos del mismo enclosing: el registro las renombra a
        // únicas (`1L`, `2L`) con binarios `M$1L`/`M$2L`, y cada `new L()` resuelve a la suya —antes se
        // descartaba la segunda en silencio y ambas caían en la primera—.
        let src = "public class M { \
                   static int a() { class L { int v() { return 1; } } return new L().v(); } \
                   static int b() { class L { int v() { return 2; } } return new L().v(); } \
                   public static int test() { return a() * 10 + b(); } }";
        assert_eq!(run_int(src, "M", "test", vec![]), 12);
        verify_all(src, "M$1L");
        verify_all(src, "M$2L");
    }

    #[test]
    fn same_named_local_classes_in_sibling_blocks_are_distinct() {
        // Bloques **hermanos** de un mismo método (§14.3): cada `L` vive solo en su bloque, así que
        // comparten nombre fuente pero son tipos distintos (`1L`/`2L`). Comprueba el alcance léxico
        // por bloque, no solo por método.
        let src = "public class M { public static int test() { \
                   int r = 0; \
                   { class L { int v() { return 3; } } r += new L().v(); } \
                   { class L { int v() { return 40; } } r += new L().v(); } \
                   return r; } }";
        assert_eq!(run_int(src, "M", "test", vec![]), 43);
    }

    #[test]
    fn a_local_class_inside_another_local_class() {
        // `L2` es una local dentro del método de otra local `L1` (§14.3): se registra como tipo
        // anidado de `L1` (`Outer$1L1$2L2`) y ambas corren. Sin captura (valor constante).
        let src = "public class Outer { public static int test() { \
                   class L1 { int a() { \
                       class L2 { int v() { return 7; } } \
                       return new L2().v(); } } \
                   return new L1().a(); } }";
        assert_eq!(run_int(src, "Outer", "test", vec![]), 7);
        verify_all(src, "Outer$1L1");
        verify_all(src, "Outer$1L1$2L2");
    }

    #[test]
    fn a_local_in_local_reads_the_outer_local_field() {
        // `L2` lee `f` de la local **externa** `L1` (§14.3): captura la instancia de `L1` en su
        // `this$0`, y el acceso sin cualificar se reescribe a `this$0.f`.
        let src = "public class Outer { public static int test() { \
                   class L1 { int f; L1() { f = 10; } \
                       int a() { \
                           class L2 { int v() { return f + 5; } } \
                           return new L2().v(); } } \
                   return new L1().a(); } }";
        assert_eq!(run_int(src, "Outer", "test", vec![]), 15);
    }

    // ---- boxing / unboxing dirigido por tipo (TransTypes, §5.1.7/§5.1.8) ----
    // El oráculo es el **verificador estricto**: TransTypes inserta `Integer.valueOf`/`x.intValue`, el
    // emisor los baja a `invokestatic`/`invokevirtual`, y el verificador confirma que la pila queda
    // consistente (`int` → `Integer` y viceversa). La **ejecución** de esos métodos vive en la JVM
    // (KajiJDK), track aparte.

    #[test]
    fn boxes_an_int_into_an_integer_local() {
        // `Integer i = 5` → `Integer.valueOf(5)`; `return i` en contexto `int` → `i.intValue()`.
        let src = "public class M { public static int test() { Integer i = 5; return i; } }";
        verify_all(src, "M");
    }

    #[test]
    fn unboxes_in_arithmetic() {
        // `i + 1` con `i` de tipo `Integer`: se desempaqueta a `int` antes de sumar, y el resultado se
        // devuelve como `int`.
        let src = "public class M { public static int test() { Integer i = 5; return i + 1; } }";
        verify_all(src, "M");
    }

    #[test]
    fn boxes_a_call_argument() {
        // `f(5)` con `f(Integer)`: el `int` se boxea a `Integer` en el sitio de la llamada.
        let src = "public class M { static int f(Integer n) { return n.intValue(); } \
                   public static int test() { return f(5); } }";
        verify_all(src, "M");
    }

    #[test]
    fn boxes_the_result_of_arithmetic_into_a_wrapper() {
        // `Integer r = a + b`: la suma es `int`, y el resultado se boxea al asignar a `Integer`.
        let src = "public class M { public static Integer test() { int a = 2; int b = 3; \
                   Integer r = a + b; return r; } }";
        verify_all(src, "M");
    }

    #[test]
    fn unboxing_to_a_wider_primitive_widens() {
        // `long l = anInteger` / `double d = anInteger`: unbox al primitivo del wrapper (`intValue()`,
        // `int`) y **widening** al target más ancho (`i2l`/`i2d`). Sin el widening la pila quedaría un
        // `int` sobre un slot ancho y el verificador lo rechazaría.
        let long_src =
            "public class M { public static long test() { Integer i = 5; long l = i; return l; } }";
        verify_all(long_src, "M");
        let dbl_src =
            "public class M { public static double test() { Integer i = 5; double d = i; return d; } }";
        verify_all(dbl_src, "M");
    }

    #[test]
    fn a_generic_lambda_boxes_its_body_end_to_end() {
        // El caso insignia: `Function<Integer,Integer> f = x -> x + 1`. El cuerpo `x + 1` desempaqueta
        // `x` (Integer→int), suma, y el resultado `int` se boxea al `Integer` del retorno del SAM.
        let src = "import java.util.function.Function; \
                   public class M { public static Function<Integer,Integer> make() { return x -> x + 1; } }";
        verify_all(src, "M");
    }

    // ---- plegado de constantes en `case` (§15.28) ----

    #[test]
    fn a_static_final_int_folds_as_a_case_label() {
        // `case LOW`/`case HIGH` (una `static final int` referida a otra) y `case 1 + 4` (aritmética
        // constante) se **pliegan** a literales, y el switch corre.
        let src = "public class C { \
                   static final int LOW = 1; \
                   static final int HIGH = LOW + 2; \
                   public static int test(int x) { \
                       switch (x) { case LOW: return 10; case HIGH: return 30; \
                                    case 1 + 4: return 50; default: return 0; } } }";
        assert_eq!(run_int(src, "C", "test", vec![3]), 30);
        assert_eq!(run_int(src, "C", "test", vec![5]), 50);
        assert_eq!(run_int(src, "C", "test", vec![1]), 10);
    }

    #[test]
    fn a_qualified_static_final_int_folds_as_a_case_label() {
        // `case K.A` (constante cualificada de otra clase): se pliega a `7` e **inlinea**, así `C`
        // corre sin depender de `K` en runtime.
        let src = "class K { static final int A = 7; } \
                   public class C { public static int test(int x) { \
                       switch (x) { case K.A: return 42; default: return 0; } } }";
        assert_eq!(run_int(src, "C", "test", vec![7]), 42);
    }

    // ---- RuntimeVisibleAnnotations (§4.7.16) ----

    #[test]
    fn runtime_visible_annotations_are_emitted() {
        // `@Deprecated` (retención RUNTIME) sobre clase/campo/método → el atributo
        // `RuntimeVisibleAnnotations` aparece en cada uno; `@Override` (SOURCE) **no** se emite.
        let src = "@Deprecated public class C { \
                   @Deprecated int f; \
                   @Deprecated void m() {} \
                   @Override public String toString() { return \"\"; } }";
        let n = COUNTER.fetch_add(1, Ordering::Relaxed);
        let dir = std::env::temp_dir().join(format!("javac_rva_{}_{n}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        write_classes(src, &dir);
        let cf = ClassFile::from_path(dir.join("C.class").to_str().unwrap()).expect("parsea");
        let rva = Some("RuntimeVisibleAnnotations");
        let on_class = cf.attributes.iter().any(|a| cf.utf8(a.name_index) == rva);
        let on_field = cf
            .fields
            .iter()
            .any(|f| f.attributes.iter().any(|a| cf.utf8(a.name_index) == rva));
        let on_m = cf
            .methods
            .iter()
            .filter(|m| cf.utf8(m.name_index) == Some("m"))
            .any(|m| m.attributes.iter().any(|a| cf.utf8(a.name_index) == rva));
        let on_ts = cf
            .methods
            .iter()
            .filter(|m| cf.utf8(m.name_index) == Some("toString"))
            .any(|m| m.attributes.iter().any(|a| cf.utf8(a.name_index) == rva));
        let _ = std::fs::remove_dir_all(&dir);
        assert!(on_class, "RVA en la clase");
        assert!(on_field, "RVA en el campo");
        assert!(on_m, "RVA en el método `m`");
        assert!(!on_ts, "`@Override` (SOURCE) no emite RVA");
    }

    // ---- enum con constantes parametrizadas (§8.9.2) ----

    #[test]
    fn an_enum_with_parameterized_constants_runs() {
        // `SMALL(1)`/`LARGE(3)` con un ctor propio `Size(int n)`: el desugar le antepone
        // `(String, int)` + `super(...)`, y cada constante se construye con su argumento.
        let src = "public enum Size { SMALL(1), LARGE(3); \
                   int n; Size(int n) { this.n = n; } \
                   int val() { return n; } \
                   public static int test() { return SMALL.val() + LARGE.val(); } }";
        assert_eq!(run_int(src, "Size", "test", vec![]), 4);
    }

    #[test]
    fn an_enum_with_a_string_constant_argument_verifies() {
        // El caso clásico `ROJO("rojo")` con un campo `String`: se compila y verifica (ejecutar el
        // `String` es igual que cualquier objeto).
        let src = "public enum Color { ROJO(\"rojo\"), VERDE(\"verde\"); \
                   private final String label; Color(String label) { this.label = label; } \
                   public String get() { return label; } }";
        verify_all(src, "Color");
    }

    // ---- inferencia en posición de argumento (§15.12.2.6, fase 2) ----

    #[test]
    fn a_lambda_as_a_call_argument_is_typed_and_lowered() {
        // `call(x -> x + 1)`: la fase 2 re-atribuye la lambda con el tipo del parámetro (`F`) como
        // target — recién ahí tipa su cuerpo y se puede bajar a `invokedynamic`. Sin fase 2 quedaba
        // `Unresolved` y la barrera del emisor la cortaba.
        let src = "interface F { int apply(int x); } \
                   public class M { static int call(F f) { return f.apply(3); } \
                   public static int use() { return call(x -> x + 1); } }";
        verify_all(src, "M");
    }

    #[test]
    fn a_method_ref_as_a_call_argument_is_typed_and_lowered() {
        // `call(M::inc)` contra el parámetro `F`: igual que la lambda, la fase 2 le da el target.
        let src = "interface F { int apply(int x); } \
                   public class M { static int inc(int x) { return x + 1; } \
                   static int call(F f) { return f.apply(3); } \
                   public static int use() { return call(M::inc); } }";
        verify_all(src, "M");
    }

    #[test]
    fn a_diamond_as_a_call_argument_infers_its_type_arguments() {
        // `take(new Box<>())` contra `take(Box<String>)`: el diamante infiere `String` del parámetro.
        let src = "class Box<T> {} \
                   public class M { static void take(Box<String> b) {} \
                   public static void use() { take(new Box<>()); } }";
        verify_all(src, "M");
    }

    #[test]
    fn a_lambda_arg_disambiguates_overloads_by_shape() {
        // `f(x -> x + 1)` con `f(Fn)` (SAM que **devuelve valor**) y `f(Cons)` (SAM `void`): la lambda
        // produce un valor, así que se elige `Fn`. Sin la desambiguación quedaba **ambiguo**; si se
        // eligiera `Cons` (void), el cuerpo con valor **no compilaría**. Que verifique prueba `Fn`.
        let src = "interface Fn { int apply(int x); } \
                   interface Cons { void accept(int x); } \
                   public class M { static int f(Fn g) { return g.apply(2); } \
                   static int f(Cons c) { c.accept(2); return 0; } \
                   public static int use() { return f(x -> x + 1); } }";
        verify_all(src, "M");
    }

    // ---- clases anónimas (§15.9.5, bajadas a clase local sintética) ----

    #[test]
    fn an_anonymous_class_extends_a_class_and_overrides() {
        // `new Base(){ … }` → una clase local sintética `M$1 extends Base` que sobrescribe `val()`.
        let src = "public class M { static class Base { int val() { return 7; } } \
                   public static int test() { \
                   Base b = new Base() { int val() { return 42; } }; \
                   return b.val(); } }";
        assert_eq!(run_int(src, "M", "test", vec![]), 42);
        verify_all(src, "M$1");
    }

    #[test]
    fn an_anonymous_class_captures_a_local() {
        // La anónima captura `c` en un campo `val$c` + parámetro de constructor; `new $1(c)` lo pasa.
        let src = "public class M { static class Base { int val() { return 0; } } \
                   public static int test() { int c = 42; \
                   Base b = new Base() { int val() { return c; } }; \
                   return b.val(); } }";
        assert_eq!(run_int(src, "M", "test", vec![]), 42);
        verify_all(src, "M$1");
    }

    // ---- anónimas con argumentos de super (§15.9.5) ----

    #[test]
    fn an_anonymous_class_forwards_super_arguments() {
        // `new Base(42){…}`: el constructor sintético de `$1` reenvía `42` a `super(int)`.
        let src = "public class M { static class Base { int v; Base(int x) { v = x; } int get() { return v; } } \
                   static Base make() { return new Base(42) { int get() { return v + 1; } }; } \
                   public static int test() { return make().get(); } }";
        assert_eq!(run_int(src, "M", "test", vec![]), 43);
        verify_all(src, "M$1");
    }

    #[test]
    fn an_anonymous_class_forwards_super_args_and_captures_a_local() {
        // El ctor sintético es `$1(int val$c, int $a0)`: primero la captura, después el super-arg.
        let src = "public class M { static class Base { int v; Base(int x) { v = x; } int get() { return v; } } \
                   public static int test() { int c = 5; \
                   Base b = new Base(c * 2) { int get() { return v + c; } }; \
                   return b.get(); } }";
        assert_eq!(run_int(src, "M", "test", vec![]), 15);
        verify_all(src, "M$1");
    }

    // ---- emisión de interfaces propias ----

    #[test]
    fn an_interface_and_an_implementing_class_verify() {
        // La interfaz `M$F` se emite con `ACC_INTERFACE` y su método `g` **abstracto sin `Code`**.
        let src = "public class M { interface F { int g(); } \
                   static class Impl implements F { public int g() { return 7; } } }";
        verify_all(src, "M$F");
        verify_all(src, "M$Impl");
    }

    #[test]
    fn an_interface_method_is_called_via_invokeinterface() {
        let src = "public class M { interface F { int g(); } \
                   static class Impl implements F { public int g() { return 7; } } \
                   public static int test() { F f = new Impl(); return f.g(); } }";
        assert!(
            code_of(src, "M", "test").0.contains(&super::INVOKEINTERFACE),
            "una llamada sobre un receptor de interfaz usa invokeinterface"
        );
        assert_eq!(run_int(src, "M", "test", vec![]), 7);
    }

    #[test]
    fn an_anonymous_class_implementing_an_interface_runs() {
        // El caso más común de anónima, ahora desbloqueado: `new F(){…}` sobre una interfaz propia.
        let src = "public class M { interface F { int g(int x); } \
                   static F make() { return new F() { public int g(int x) { return x + 1; } }; } \
                   public static int test() { return make().g(41); } }";
        assert_eq!(run_int(src, "M", "test", vec![]), 42);
        verify_all(src, "M$1");
    }

    #[test]
    fn a_lambda_against_an_own_functional_interface_verifies() {
        // Con la emisión de interfaces propias, una lambda se tipa y emite contra una interfaz
        // funcional **nuestra** (la ejecución del `invokedynamic` vive en KajiJDK).
        let src = "public class M { interface F { int g(int x); } \
                   static F make() { return x -> x + 1; } }";
        verify_all(src, "M");
        verify_all(src, "M$F");
    }

    #[test]
    fn an_anonymous_class_captures_the_enclosing_instance() {
        // En un método de instancia, la anónima lee `f` del enclosing → captura `this$0`; su ctor es
        // `$1(Outer this$0)` y el sitio pasa `new $1(this)`.
        let src = "public class Outer { static class Base { int val() { return 0; } } \
                   int f; Outer() { f = 100; } \
                   Base make() { return new Base() { int val() { return f; } }; } \
                   public static int test() { return new Outer().make().val(); } }";
        assert_eq!(run_int(src, "Outer", "test", vec![]), 100);
        verify_all(src, "Outer$1");
    }

    // ---- barrera de seguridad ----

    /// Compila esperando que el emisor **rechace** la construcción, y devuelve el mensaje.
    fn rejected(src: &str) -> String {
        match crate::javac::compile(src) {
            Err(e) => e.message,
            Ok(_) => panic!("debería haber sido rechazado, no emitido"),
        }
    }

    // ---- arrays e `instanceof` ----

    #[test]
    fn creates_reads_and_writes_an_array() {
        let src = "public class M { public static int f() { int[] a = new int[3]; \
                   a[0] = 7; a[1] = 5; return a[0] * 10 + a[1] + a.length; } }";
        assert_eq!(run_int(src, "M", "f", vec![]), 78); // 70 + 5 + 3
    }

    #[test]
    fn an_array_initializer_fills_the_elements() {
        let src = "public class M { public static int f() { int[] a = new int[]{4, 5, 6}; \
                   return a[0] + a[1] + a[2]; } }";
        assert_eq!(run_int(src, "M", "f", vec![]), 15);
    }

    #[test]
    fn a_varargs_call_now_compiles_and_runs() {
        // El desugar lo baja a `g(new int[]{1,2,3})`: antes fallaba por la barrera.
        let src = "public class M { static int g(int[] xs) { return xs.length; } \
                   public static int f() { return g(new int[]{1, 2, 3}); } }";
        assert_eq!(run_int(src, "M", "f", vec![]), 3);
    }

    #[test]
    fn a_pattern_switch_now_compiles_and_runs() {
        // Baja a una cadena de `instanceof`: antes fallaba por la barrera.
        let src = "public class M { public static int f() { Object o = \"hola\"; switch (o) { \
                   case String s -> { return 1; } default -> { return 0; } } } }";
        assert_eq!(run_int(src, "M", "f", vec![]), 1);
    }

    #[test]
    fn arrays_and_instanceof_pass_the_strict_verifier() {
        verify_all(
            "public class M { public static int f(Object o) { \
             int[] a = new int[]{1, 2}; a[0] = a[1] + a.length; \
             if (o instanceof String) { return a[0]; } return 0; } }",
            "M",
        );
    }

    #[test]
    fn a_non_constant_case_label_is_rejected() {
        // Un `static` **no final** no es una constante de compilación (§15.28): no se pliega, y la
        // barrera del emisor avisa (la `static final int` **sí** se pliega — ver
        // `a_static_final_int_folds_as_a_case_label`).
        let msg = rejected(
            "public class M { static int k = 1; \
             public static int f(int n) { switch (n) { case k: return 1; default: return 0; } } }",
        );
        assert!(msg.contains("constante entera"), "{msg}");
    }

    // ---- literal de clase ----

    #[test]
    fn a_class_literal_emits_an_ldc_of_a_class_entry() {
        let bytes = compile("public class M { public static Class c() { return M.class; } }");
        // El pool debe traer el nombre de la clase y el descriptor de retorno `Class`.
        assert!(find_bytes(&bytes, b"java/lang/Class"), "el retorno es un Class");
        assert!(find_bytes(&bytes, b"M"), "y el literal referencia a M");
    }

    #[test]
    fn a_class_literal_passes_the_strict_verifier() {
        verify_all("public class M { public static Class c() { return M.class; } }", "M");
    }

    fn find_bytes(hay: &[u8], needle: &[u8]) -> bool {
        hay.windows(needle.len()).any(|w| w == needle)
    }

    // ---- try / catch / finally ----

    #[test]
    fn catches_a_thrown_exception() {
        let src = "public class M { public static int f(int n) { \
                   try { if (n > 0) { throw new RuntimeException(); } return 1; } \
                   catch (RuntimeException e) { return 2; } } }";
        assert_eq!(run_int(src, "M", "f", vec![5]), 2);
        assert_eq!(run_int(src, "M", "f", vec![0]), 1);
    }

    #[test]
    fn finally_runs_on_the_normal_path() {
        let src = "public class M { public static int f(int n) { int r = 0; \
                   try { r = 1; } finally { r = r + 10; } return r; } }";
        assert_eq!(run_int(src, "M", "f", vec![0]), 11);
    }

    #[test]
    fn finally_runs_when_the_try_returns() {
        // §14.20.2: un `return` **dentro** del `try` corre el `finally` antes de retornar. Antes se
        // salteaba (el `return` emitía `ireturn` directo). `m(5)` retorna 5 **y** deja `r = 99`.
        let src = "public class M { static int r = 0; \
                   static int m(int x) { try { return x; } finally { r = 99; } } \
                   public static int f(int n) { int v = m(5); return v * 1000 + r; } }";
        assert_eq!(run_int(src, "M", "f", vec![0]), 5099);
    }

    #[test]
    fn finally_runs_when_the_catch_returns() {
        // El `finally` también corre cuando la salida abrupta es un `return` del `catch`.
        // `m(-3)` lanza → el `catch` retorna -1, el `finally` deja `r = 7` ⇒ -1*1000 + 7.
        let src = "public class M { static int r = 0; \
                   static int m(int x) { try { if (x < 0) throw new RuntimeException(); return x; } \
                   catch (RuntimeException e) { return -1; } finally { r = 7; } } \
                   public static int f(int n) { int v = m(-3); return v * 1000 + r; } }";
        assert_eq!(run_int(src, "M", "f", vec![0]), -993);
    }

    #[test]
    fn finally_runs_when_a_break_leaves_the_try() {
        // Un `break` que **sale** de un `try` cruza y corre su `finally` (§14.20.2). Antes emitía el
        // `goto` de salida directo, salteándolo: `m()` daba 0 en vez de 1.
        let src = "public class M { static int r = 0; public static int f(int n) { \
                   for (int i = 0; i < 3; i++) { try { break; } finally { r = r + 1; } } return r; } }";
        assert_eq!(run_int(src, "M", "f", vec![0]), 1);
    }

    #[test]
    fn finally_runs_when_the_catch_throws() {
        // Un `throw` en el `catch` no se escapa sin correr el `finally`: el catch-all también protege
        // el cuerpo del `catch`. `inner` deja `r = 5` antes de propagar; el `f` de afuera lo atrapa.
        let src = "public class M { static int r = 0; \
                   static void inner() { try { throw new RuntimeException(); } \
                   catch (RuntimeException e) { throw new RuntimeException(); } finally { r = 5; } } \
                   public static int f(int n) { try { inner(); } catch (RuntimeException e) {} return r; } }";
        assert_eq!(run_int(src, "M", "f", vec![0]), 5);
    }

    #[test]
    fn try_catch_passes_the_strict_verifier() {
        // El frame de un handler es especial: la JVM limpia la pila y deja ahí la excepción.
        verify_all(
            "public class M { public static int f(int n) { \
             try { if (n > 0) { throw new RuntimeException(); } return 1; } \
             catch (RuntimeException e) { return 2; } } }",
            "M",
        );
    }

    #[test]
    fn try_finally_passes_the_strict_verifier() {
        // El `finally` va **duplicado**: en la salida normal y en un handler catch-all que re-lanza.
        verify_all(
            "public class M { public static int f(int n) { int r = 0; \
             try { r = 1; if (n > 0) { return r; } } finally { r = r + 10; } return r; } }",
            "M",
        );
    }

    // ---- try-with-resources (§14.20.3) ----

    #[test]
    fn try_with_resources_passes_the_strict_verifier() {
        // §14.20.3.1 baja `try (R r = …) { … }` a un `try/catch(Throwable)/finally` que cierra el
        // recurso suprimiendo la excepción del `close()` dentro de la primaria (vía `addSuppressed`).
        // El `finally` va **duplicado** (salida normal + handler catch-all): la copia catch-all seguía
        // a un `catch` terminado en `throw`, así que heredaba `reachable == false` y se comía el `goto`
        // que saltea el handler del `close` interno —el flujo caía dentro de su propio handler y el
        // `StackMapTable` no verificaba—. Además, un `close()`/cuerpo que **siempre lanza** dejaba un
        // `return` muerto tras el `athrow` (código inalcanzable sin frame), otro rechazo del verificador.
        // Cuerpo que retorna:
        verify_all(
            "public class M { static class R implements AutoCloseable { public void close() {} } \
             public static int f(int n) { try (R r = new R()) { if (n > 0) { return n; } return 0; } } }",
            "M",
        );
        // Cuerpo que lanza, con `catch` externo:
        verify_all(
            "public class M { static class R implements AutoCloseable { public void close() {} } \
             public static int f(int n) { try { try (R r = new R()) { throw new RuntimeException(); } } \
             catch (RuntimeException e) { return 1; } } }",
            "M",
        );
        // `void … throws` que sale por excepción (sin `return` de caída):
        verify_all(
            "public class M { static class R implements AutoCloseable { public void close() {} } \
             static void f() throws Throwable { try (R r = new R()) { throw new java.lang.ArithmeticException(); } } }",
            "M",
        );
        // `close()` que **siempre lanza** (el que dejaba el `return` muerto tras el `athrow`):
        verify_all(
            "public class M { static class R implements AutoCloseable { public void close() { throw new RuntimeException(); } } \
             static int f() { try (R r = new R()) { return 1; } } }",
            "M$R",
        );
    }

    #[test]
    fn try_with_resources_closes_the_resource_end_to_end() {
        // Corrida end-to-end en el intérprete propio: el recurso **se cierra** tanto en salida normal
        // como cuando el cuerpo lanza, y la excepción del cuerpo se propaga como **primaria** (no la
        // tapa el `close`). `log` acumula el orden: 1 = cuerpo, 2 = `close`. f(0): normal ⇒ el recurso
        // se cierra (log 12) ⇒ 100+12. f(1): el cuerpo lanza ⇒ el recurso **igual se cierra** (log 12)
        // y se propaga su ArithmeticException, atrapada afuera ⇒ 12.
        //
        // (La supresión con **doble** excepción —`close()` que también lanza— se verifica a nivel de
        // bytecode con `verify_all` de arriba; ejecutarla end-to-end depende de `Throwable.addSuppressed`,
        // que hoy el intérprete propio no soporta —limitación de biblioteca, ajena a este codegen—.)
        let src = "public class M { static int log = 0; \
                   static class R implements AutoCloseable { public void close() { log = log * 10 + 2; } } \
                   static int f(int n) { log = 0; \
                     try { \
                       try (R r = new R()) { log = log * 10 + 1; if (n >= 1) throw new ArithmeticException(); } \
                       return 100 + log; \
                     } catch (ArithmeticException e) { return log; } } }";
        assert_eq!(run_int(src, "M", "f", vec![0]), 112); // normal: cerrado
        assert_eq!(run_int(src, "M", "f", vec![1]), 12); // cuerpo lanza: cerrado + primaria preservada
    }

    #[test]
    fn a_method_ending_in_throw_emits_no_dead_return() {
        // Un `void` cuyo cuerpo termina en `throw` no debe llevar un `return` de caída: sería código
        // muerto tras el `athrow` (sin frame) que el verificador estricto rechaza. javac tampoco lo emite.
        verify_all("public class M { static void a() { throw new RuntimeException(); } }", "M");
        let (code, _) = code_of("public class M { static void a() { throw new RuntimeException(); } }", "M", "a");
        assert_eq!(*code.last().unwrap(), ATHROW, "el último opcode es `athrow`, sin `return` muerto detrás");
    }

    #[test]
    fn a_catch_that_fires_still_runs_its_finally_and_jumps_past_the_handler() {
        // #257 — el caso que faltaba: el `catch` se dispara **de verdad** y hay `finally`. El cuerpo
        // del `try` termina en `throw`, así que `reachable` quedaba en `false` y el final del
        // `catch` no emitía ni la copia en línea del `finally` ni el `goto`: el `catch` caía derecho
        // dentro del handler catch-all, que hace `astore` de un throwable que nadie apiló. Un
        // handler siempre es alcanzable — se llega por excepción, no por caída.
        let src = "public class M { static int t; public static int f() { t = 0;                    try { throw new RuntimeException(); }                    catch (RuntimeException e) { t = t + 1; }                    finally { t = t + 10; } return t; } }";
        assert_eq!(run_int(src, "M", "f", vec![]), 11); // el `catch` Y el `finally`, una sola vez cada uno
        verify_all(src, "M");
        // Y los controles que ya andaban siguen andando: sin excepción, y con la excepción saliendo.
        let sin = "public class M { static int t; public static int f() { t = 0;                    try { t = t + 1; } catch (RuntimeException e) { t = t + 100; }                    finally { t = t + 10; } return t; } }";
        assert_eq!(run_int(sin, "M", "f", vec![]), 11);
        verify_all(sin, "M");
    }

    // ---- StackMapTable ----

    #[test]
    fn loops_and_branches_pass_the_strict_verifier() {
        verify_all(
            "public class M { public static int f(int n) { int s = 0; \
             for (int i = 0; i < n; i++) { if (i == 3) { continue; } if (i == 7) { break; } s = s + i; } \
             if (s > 100) { return 1; } return s; } }",
            "M",
        );
    }

    #[test]
    fn a_boolean_valued_comparison_passes_the_verifier() {
        // El único destino de salto con la pila **no vacía**: el frame lleva `stack = [int]`.
        verify_all(
            "public class M { public static boolean pos(int n) { boolean b = n > 0; return b; } }",
            "M",
        );
    }

    #[test]
    fn wide_locals_in_frames_pass_the_verifier() {
        // Un `long` ocupa dos slots pero **una sola entrada** en la lista de locales del frame.
        verify_all(
            "public class M { public static long f(long a, int n) { long s = a; \
             for (int i = 0; i < n; i++) { s = s + i; } return s; } }",
            "M",
        );
    }

    #[test]
    fn objects_and_branches_pass_the_verifier() {
        verify_all(
            "public class M { int v; M(int x) { this.v = x; } \
             int pick(int n) { if (n > 0) { return this.v; } return 0; } \
             public static int test() { M m = new M(3); return m.pick(1); } }",
            "M",
        );
    }

    // ---- ternario ----

    #[test]
    fn the_ternary_operator_picks_a_branch() {
        let src = "public class M { public static int max(int a, int b) { return a > b ? a : b; } }";
        assert_eq!(run_int(src, "M", "max", vec![3, 7]), 7);
        assert_eq!(run_int(src, "M", "max", vec![9, 2]), 9);
    }

    #[test]
    fn a_ternary_promotes_both_branches_to_its_own_type() {
        // `1` es un `int` en la fuente, pero el ternario es `long`: sin la promoción, una rama
        // dejaría un `int` y el frame del destino no cerraría (§15.25).
        let src = "public class M { public static long f(long n) { return n > 0 ? 1 : 2L; } }";
        assert_eq!(run_long(src, "M", "f", vec![5]), 1);
        assert_eq!(run_long(src, "M", "f", vec![-5]), 2);
    }

    #[test]
    fn a_ternary_passes_the_strict_verifier() {
        verify_all("public class M { public static int f(int a) { return a > 0 ? a : -a; } }", "M");
        verify_all("public class M { public static long f(int a) { return a > 0 ? 1 : 2L; } }", "M");
        // Anidado y en posición de argumento, no solo de retorno.
        verify_all(
            "public class M { static int g(int x) { return x; } \
             public static int f(int a, int b) { return g(a > b ? (a > 0 ? a : 0) : b); } }",
            "M",
        );
    }

    // ---- switch en bytecode ----

    #[test]
    fn a_dense_switch_uses_tableswitch() {
        let src = "public class M { public static int f(int x) { \
                   switch (x) { case 0: return 10; case 1: return 11; case 2: return 12; \
                   default: return -1; } } }";
        assert!(code_of(src, "M", "f").0.contains(&TABLESWITCH), "un rango denso indexa directo");
        assert_eq!(run_int(src, "M", "f", vec![1]), 11);
        assert_eq!(run_int(src, "M", "f", vec![2]), 12);
        assert_eq!(run_int(src, "M", "f", vec![9]), -1);
    }

    #[test]
    fn a_sparse_switch_uses_lookupswitch() {
        // Con `case 1` y `case 1000`, una tabla gastaría 998 entradas vacías.
        let src = "public class M { public static int f(int x) { \
                   switch (x) { case 1: return 1; case 1000: return 2; default: return 3; } } }";
        assert!(code_of(src, "M", "f").0.contains(&LOOKUPSWITCH), "un rango ralo busca por clave");
        assert_eq!(run_int(src, "M", "f", vec![1000]), 2);
        assert_eq!(run_int(src, "M", "f", vec![7]), 3);
    }

    #[test]
    fn switch_falls_through_until_a_break() {
        // `case 1:` no tiene cuerpo: cae en el de `case 2` — el *fall-through* de §14.11.
        let src = "public class M { public static int f(int x) { int r = 0; \
                   switch (x) { case 1: case 2: r = 20; break; case 3: r = 30; default: r = r + 1; } \
                   return r; } }";
        assert_eq!(run_int(src, "M", "f", vec![1]), 20);
        assert_eq!(run_int(src, "M", "f", vec![2]), 20);
        assert_eq!(run_int(src, "M", "f", vec![3]), 31, "el `case 3` sin break cae en el default");
        assert_eq!(run_int(src, "M", "f", vec![9]), 1);
    }

    #[test]
    fn an_arrow_switch_does_not_fall_through() {
        let src = "public class M { public static int f(int x) { int r = -1; \
                   switch (x) { case 3 -> r = 30; case 4 -> r = 40; } return r; } }";
        assert_eq!(run_int(src, "M", "f", vec![3]), 30, "la flecha no cae al brazo siguiente");
        assert_eq!(run_int(src, "M", "f", vec![5]), -1, "sin default, no matchear sale del switch");
    }

    #[test]
    fn a_string_switch_finally_reaches_bytecode() {
        // El desugar lo baja a **dos** switches sobre `int` desde 2026; hasta ahora ninguno de los
        // dos se podía emitir. Es el que más ganó con `lookupswitch`: las claves son hashes.
        verify_all(
            "public class M { public static int f(String s) { \
             switch (s) { case \"uno\": return 1; case \"dos\": return 2; default: return 0; } } }",
            "M",
        );
    }

    #[test]
    fn switches_pass_the_strict_verifier() {
        verify_all(
            "public class M { public static int f(int x) { int r = 0; \
             switch (x) { case 0: r = 1; break; case 1: case 2: r = 2; default: r = r + 9; } return r; } }",
            "M",
        );
        verify_all(
            "public class M { public static int f(int x) { \
             switch (x) { case 100: return 1; case 5000: return 2; default: return 0; } } }",
            "M",
        );
    }

    // ---- break/continue etiquetados ----

    #[test]
    fn a_labeled_break_leaves_the_outer_loop() {
        let src = "public class M { public static int f(int n) { int c = 0; \
                   outer: for (int i = 0; i < n; i++) { for (int j = 0; j < n; j++) { \
                   if (i * j > 6) { break outer; } c = c + 1; } } return c; } }";
        // Cuenta 4+4+4 (i=0,1,2, ningún producto pasa de 6) y 3 más en i=3, hasta que 3*3=9 corta.
        assert_eq!(run_int(src, "M", "f", vec![4]), 15);
    }

    #[test]
    fn a_labeled_continue_jumps_to_the_outer_header() {
        let src = "public class M { public static int f(int n) { int c = 0; \
                   outer: for (int i = 0; i < n; i++) { for (int j = 0; j < n; j++) { \
                   if (j == 2) { continue outer; } c = c + 1; } } return c; } }";
        // Cada `i` alcanza a sumar j=0 y j=1 antes de saltar al `i++`.
        assert_eq!(run_int(src, "M", "f", vec![5]), 10);
    }

    #[test]
    fn a_labeled_break_from_inside_a_switch_leaves_the_loop() {
        // Sin etiqueta, el `break` saldría del `switch`: es exactamente lo que distingue a los dos.
        let src = "public class M { public static int f(int n) { int r = 0; \
                   loop: for (int i = 0; i < n; i++) { switch (i) { case 2: break loop; \
                   default: r = r + i; } } return r; } }";
        assert_eq!(run_int(src, "M", "f", vec![5]), 1, "suma 0 y 1, y en i=2 sale del for");
    }

    #[test]
    fn an_unlabeled_break_inside_a_loop_only_leaves_the_switch() {
        let src = "public class M { public static int f(int n) { int r = 0; \
                   for (int i = 0; i < n; i++) { switch (i) { case 2: break; \
                   default: r = r + i; } } return r; } }";
        assert_eq!(run_int(src, "M", "f", vec![5]), 8, "0+1+3+4: solo se saltea el 2");
    }

    #[test]
    fn a_labeled_block_is_left_with_break() {
        let src = "public class M { public static int f(int x) { int r = 0; \
                   done: { if (x < 0) { break done; } r = x * 2; } return r; } }";
        assert_eq!(run_int(src, "M", "f", vec![21]), 42);
        assert_eq!(run_int(src, "M", "f", vec![-1]), 0, "el break saltea el resto del bloque");
    }

    #[test]
    fn labeled_jumps_pass_the_strict_verifier() {
        verify_all(
            "public class M { public static int f(int n) { int c = 0; \
             a: for (int i = 0; i < n; i++) { b: for (int j = 0; j < n; j++) { \
             if (j == 1) { continue a; } if (i == 3) { break a; } if (j == 2) { break b; } c = c + 1; } } \
             return c; } }",
            "M",
        );
        // La combinación filosa: un `switch` adentro del bucle, con las dos clases de salida.
        verify_all(
            "public class M { public static int f(int n) { int r = 0; \
             loop: for (int i = 0; i < n; i++) { switch (i) { case 0: break; case 2: break loop; \
             default: r = r + i; } r = r + 1; } return r; } }",
            "M",
        );
    }

    // ---- synchronized ----

    #[test]
    fn synchronized_takes_and_releases_the_monitor() {
        let src = "public class M { static int c = 0; \
                   public static int f(int n) { Object lock = new Object(); \
                   synchronized (lock) { c = c + n; } return c; } }";
        assert_eq!(run_int(src, "M", "f", vec![5]), 5);
    }

    #[test]
    fn synchronized_releases_the_monitor_on_the_exceptional_path() {
        // La tabla de excepciones tiene que llevar el handler catch-all que suelta el monitor: sin
        // él, una excepción adentro del bloque dejaría el lock tomado para siempre.
        let src = "public class M { public static int f(Object o, int n) { \
                   synchronized (o) { if (n < 0) { throw new RuntimeException(); } } return n; } }";
        let (code, handlers) = code_of(src, "M", "f");
        assert_eq!(handlers, 2, "catch-all que suelta el monitor + su auto-protección, como javac");
        assert_eq!(
            code.iter().filter(|&&b| b == MONITOREXIT).count(),
            2,
            "se suelta en las dos salidas: la normal y la excepcional"
        );
    }

    #[test]
    fn synchronized_passes_the_strict_verifier() {
        verify_all(
            "public class M { static int c = 0; \
             public static int f(Object o, int n) { synchronized (o) { c = c + n; \
             if (n > 0) { return c; } } return c; } }",
            "M",
        );
    }

    // ---- `synchronized`: soltar el monitor en toda salida (§14.19) ----

    #[test]
    fn a_return_inside_synchronized_releases_the_monitor_before_returning() {
        // El bug: `synchronized (o) { … return; }` no soltaba el monitor antes del `return` (monitor
        // leak). Espejando a javac, el `monitorexit` tiene que ir **antes** del `ireturn`, no solo en
        // el handler. Además la referencia se `dup`-lica para guardar la copia del monitor.
        let src = "public class M { static int r; \
                   static int m(Object o) { synchronized (o) { r = 1; return 7; } } }";
        let (code, handlers) = code_of(src, "M", "m");
        let first_ireturn = code.iter().position(|&b| b == IRETURN).expect("hay un ireturn");
        let exit_before_return = code[..first_ireturn].iter().any(|&b| b == MONITOREXIT);
        assert!(exit_before_return, "el `monitorexit` debe correr antes del `ireturn`, no filtrar el monitor");
        assert!(code.contains(&DUP), "la referencia del monitor se `dup`-lica (como javac)");
        assert!(code.contains(&MONITORENTER), "se toma el monitor");
        // Dos monitorexit: el del `return` y el del handler catch-all.
        assert_eq!(code.iter().filter(|&&b| b == MONITOREXIT).count(), 2, "return + catch-all");
        assert_eq!(handlers, 2, "handler catch-all + auto-protección del handler, como javac");
    }

    #[test]
    fn a_break_out_of_a_synchronized_in_a_loop_releases_the_monitor() {
        // Un `break` que sale del `synchronized` también tiene que soltar el monitor antes del salto:
        // hay tres `monitorexit` — el del `break`, el de la salida normal, y el del handler.
        let src = "public class M { static int f(Object o) { int s = 0; \
                   for (int i = 0; i < 3; i++) { synchronized (o) { if (i == 1) { break; } s += i; } } \
                   return s; } }";
        let (code, _handlers) = code_of(src, "M", "f");
        assert_eq!(
            code.iter().filter(|&&b| b == MONITOREXIT).count(),
            3,
            "break + salida normal + handler catch-all"
        );
    }

    #[test]
    fn a_synchronized_with_a_return_runs_and_releases_the_monitor() {
        // Ejecución de verdad: tras un `return` desde dentro de un `synchronized`, el hilo **no** debe
        // seguir teniendo el monitor (`Thread.holdsLock`). Antes del fix quedaba tomado (leak) → 1.
        let src = "public class M { static int r; \
                   static int g(Object o) { synchronized (o) { r = 1; return 7; } } \
                   public static int f() { Object o = new Object(); int v = g(o); \
                   return v + (Thread.holdsLock(o) ? 100 : 0); } }";
        // g devuelve 7 y suelta el monitor: f = 7 + 0.
        assert_eq!(run_int(src, "M", "f", vec![]), 7, "valor correcto y monitor liberado tras el return");
    }

    // ---- inicializadores de campo y `<clinit>` ----

    #[test]
    fn a_static_field_initializer_runs_in_the_clinit() {
        let src = "public class M { static int s = 3; static { s = s + 10; }                    public static int test() { return s; } }";
        assert_eq!(run_int(src, "M", "test", vec![]), 13, "el init y el bloque, en orden");
    }

    #[test]
    fn an_instance_field_initializer_runs_in_the_constructor() {
        let src = "public class M { int v = 7;                    public static int test() { M m = new M(); return m.v; } }";
        assert_eq!(run_int(src, "M", "test", vec![]), 7);
    }

    #[test]
    fn an_instance_initializer_runs_before_the_constructor_body() {
        let src = "public class M { int v = 1; { this.v = this.v + 2; }                    M() { this.v = this.v * 10; }                    public static int test() { M m = new M(); return m.v; } }";
        assert_eq!(run_int(src, "M", "test", vec![]), 30, "(1+2)*10: el orden de §12.5");
    }

    // ---- miembros implícitos de un `enum` (§8.9.3) ----

    #[test]
    fn an_enum_gets_its_constants_and_values() {
        let src = "public enum E { A, B, C;                    public static int test() { return E.values().length; } }";
        assert_eq!(run_int(src, "E", "test", vec![]), 3);
    }

    #[test]
    fn enum_constants_carry_their_ordinal() {
        let src = "public enum E { A, B, C;                    public static int test() { return E.C.ordinal() * 10 + E.A.ordinal(); } }";
        assert_eq!(run_int(src, "E", "test", vec![]), 20);
    }

    #[test]
    fn values_returns_a_fresh_array_each_time() {
        // Lo que importa de la copia: mutar lo devuelto **no** toca el estado del enum.
        let src = "public enum E { A, B;                    public static int test() { E[] v = E.values(); v[0] = null;                    return E.values()[0] == null ? 1 : 0; } }";
        assert_eq!(run_int(src, "E", "test", vec![]), 0, "la segunda llamada trae A intacto");
    }

    #[test]
    fn value_of_finds_the_constant_by_name() {
        let src = "public enum E { A, B;                    public static int test() { return E.valueOf(\"B\") == E.B ? 1 : 0; } }";
        assert_eq!(run_int(src, "E", "test", vec![]), 1);
    }

    #[test]
    fn an_enum_passes_the_strict_verifier() {
        verify_all("public enum E { A, B, C }", "E");
    }

    #[test]
    fn a_switch_over_an_enum_runs_end_to_end() {
        // El cierre de un camino largo: el desugar lo baja a un `$SwitchMap` en una clase-holder
        // `E$1`, el enum trae sus miembros implícitos, y **ahora que se emite un `.class` por clase**
        // las dos llegan al disco y el `switch` corre de verdad.
        let src = "public enum E { A, B, C; \
                   public static int pick(int i) { E e = E.values()[i]; \
                   switch (e) { case A: return 10; case B: return 20; default: return 99; } } }";
        assert_eq!(run_int(src, "E", "pick", vec![0]), 10);
        assert_eq!(run_int(src, "E", "pick", vec![1]), 20);
        assert_eq!(run_int(src, "E", "pick", vec![2]), 99, "C cae en el default");
    }

    #[test]
    fn two_top_level_classes_both_emit_and_run() {
        // Antes el segundo `class` desaparecía en silencio. Ahora `Helper.answer()` existe y se
        // invoca desde `Main`.
        let src = "public class Main { public static int go() { return Helper.answer() + 1; } } \
                   class Helper { static int answer() { return 41; } }";
        assert_eq!(run_int(src, "Main", "go", vec![]), 42);
    }

    #[test]
    fn a_nested_class_is_emitted_and_usable() {
        // Cualificado (`Outer.Inner.v()`): `Outer$Inner.class` se emite, y el acceso a tipo anidado
        // cualificado resuelve — las dos cosas que faltaban.
        let src = "public class Outer { static class Inner { static int v() { return 5; } } \
                   public static int use() { return Outer.Inner.v() * 2; } }";
        assert_eq!(run_int(src, "Outer", "use", vec![]), 10);
    }

    #[test]
    fn a_nested_class_keeps_member_flags_out_of_the_class_access() {
        // §4.1: `ACC_STATIC`/`PRIVATE`/`PROTECTED` de un anidado son flags **de miembro** (van en
        // `InnerClasses`), no en los access_flags de clase —la JVM rechaza un `.class` con ellos—.
        // Lo destapó el diferencial de emisión (`Unmatched bit 0x8` en javap).
        let jvm = compiled_class("class Outer { private static class Inner {} }", "Outer$Inner");
        let member = super::ACC_STATIC | super::ACC_PRIVATE | super::ACC_PROTECTED;
        assert_eq!(jvm.access_flags & member, 0, "flags de miembro a nivel clase: {:#x}", jvm.access_flags);
    }

    #[test]
    fn the_default_constructor_takes_the_class_access() {
        // §8.8.9: el ctor por defecto hereda el **acceso de la clase** (antes salía siempre `public`).
        let find_ctor = |jvm: &ClassFile| -> u16 {
            jvm.methods
                .iter()
                .find(|m| jvm.utf8(m.name_index) == Some("<init>"))
                .map(|m| m.access_flags)
                .expect("hay ctor")
        };
        assert_eq!(
            find_ctor(&compiled_class("class C {}", "C")) & super::ACC_PUBLIC,
            0,
            "clase package-private → ctor package-private",
        );
        assert_ne!(
            find_ctor(&compiled_class("public class C {}", "C")) & super::ACC_PUBLIC,
            0,
            "clase public → ctor public",
        );
    }

    // ---- invocación explícita de constructor (§8.8.7.1) ----

    #[test]
    fn an_explicit_super_call_passes_its_arguments() {
        // Solo se **verifica**, no se ejecuta: `--emit` escribe un `.class` por archivo, así que la
        // superclase no llega al disco y el runtime no podría cargarla. El `invokespecial` con su
        // descriptor `(I)V` sí queda en el pool, que es lo que este test mira.
        let src = "public class Sub extends Base { Sub() { super(7); } int get() { return 0; } }                    class Base { int v; Base(int x) { this.v = x; } }";
        verify_all(src, "Sub");
        let (code, _) = code_of(src, "Sub", "<init>");
        assert_eq!(
            code.iter().filter(|&&b| b == INVOKESPECIAL).count(),
            1,
            "un solo invokespecial: el explícito, sin el implícito encima"
        );
        assert!(code.contains(&BIPUSH) || code.contains(&0x10), "el argumento 7 se empuja");
    }

    #[test]
    fn an_implicit_default_super_still_works() {
        // La clase padre no declara constructores: no hay símbolo que resolver, pero el `()V`
        // implícito existe igual y el `super()` explícito tiene que emitirlo.
        let src = "public class M { int v; M() { super(); this.v = 3; }                    public static int test() { M m = new M(); return m.v; } }";
        assert_eq!(run_int(src, "M", "test", vec![]), 3);
    }

    #[test]
    fn this_delegation_runs_the_other_constructor() {
        let src = "public class M { int v; M(int x) { this.v = x; } M() { this(9); }                    public static int test() { M m = new M(); return m.v; } }";
        assert_eq!(run_int(src, "M", "test", vec![]), 9);
    }

    #[test]
    fn only_one_constructor_call_is_emitted() {
        // El `super()` implícito **no** se agrega si ya hay uno explícito: inicializar dos veces el
        // mismo objeto es ilegal, y el verificador lo rechazaría.
        let src = "public class M { int v; M(int x) { this.v = x; } M() { this(9); } }";
        let (code, _) = code_of(src, "M", "<init>");
        assert_eq!(
            code.iter().filter(|&&b| b == INVOKESPECIAL).count(),
            1,
            "un solo invokespecial en el constructor delegante"
        );
    }

    #[test]
    fn explicit_constructor_calls_pass_the_strict_verifier() {
        // El `this` es `UninitThis` hasta que corre el `super(...)`: si el emisor no lo marcara
        // como inicializado después, el `putfield` siguiente sería ilegal.
        verify_all(
            "public class M { int v; M(int x) { this.v = x; }              M() { this(1); this.v = this.v + 1; } }",
            "M",
        );
    }

    // ---- la pila **tipada**: frames en destinos alcanzados con algo ya empujado ----

    #[test]
    fn an_uninitialized_object_survives_a_branch_target() {
        // El `new` y su `dup` siguen vivos en la pila mientras se evalúa el argumento, así que el
        // frame del ternario tiene que declarar **dos** `Uninit` debajo del valor. Con la pila como
        // simple contador de altura declaraba una pila vacía, y el `.class` salía inverificable.
        verify_all(
            "public class M { int v; M(int x) { this.v = x; }              public static int f(int a) { M m = new M(a > 0 ? 1 : 2); return m.v; } }",
            "M",
        );
    }

    #[test]
    fn a_ternary_in_argument_position_declares_the_stack_below_it() {
        verify_all(
            "public class M { static int g(int a, int b) { return a + b; }              public static int f(int x) { return g(7, x > 0 ? 1 : 2); } }",
            "M",
        );
    }

    #[test]
    fn a_short_circuit_inside_an_argument_declares_the_stack_below_it() {
        verify_all(
            "public class M { static int g(int a, boolean b) { return a; }              public static int f(int x) { return g(3, x > 0 && x < 10); } }",
            "M",
        );
    }

    #[test]
    fn a_ternary_under_an_array_store_declares_the_stack_below_it() {
        // El `arrayref` y el índice ya están empujados cuando el ternario fija sus etiquetas.
        let src = "public class M { public static int f(int x) { int[] a = new int[2];                    a[1] = x > 0 ? 10 : 20; return a[1]; } }";
        verify_all(src, "M");
        assert_eq!(run_int(src, "M", "f", vec![5]), 10);
        assert_eq!(run_int(src, "M", "f", vec![-5]), 20);
    }

    #[test]
    fn a_string_literal_goes_through_the_constant_pool() {
        let bytes = compile("public class M { public static String hi() { return \"hola\"; } }");
        // El `.class` debe llevar los caracteres del literal en su pool.
        let needle = b"hola";
        assert!(
            bytes.windows(needle.len()).any(|w| w == needle),
            "el literal debería estar en el constant pool"
        );
    }

    // ---- lambdas: invokedynamic + LambdaMetafactory (LambdaToMethod) ----
    //
    // El desugar baja cada lambda a un método sintético `lambda$…` + un nodo `Indy`, y el emisor
    // lo cierra: empuja las capturas y emite el `invokedynamic` con `LambdaMetafactory.metafactory`
    // de *bootstrap*. La **ejecución** del call site vive en KajiJDK; acá el oráculo es el
    // verificador estricto —que hace *cross-check* de nuestros frames— más la inspección del sitio.

    #[test]
    fn a_non_capturing_lambda_lowers_to_indy_and_a_synthetic_method() {
        // `() -> {}` contra `Runnable`: el sitio emite `invokedynamic`…
        let src = "public class M { public static Runnable make() { return () -> {}; } }";
        assert!(
            code_of(src, "M", "make").0.contains(&INVOKEDYNAMIC),
            "el sitio de la lambda debe emitir invokedynamic"
        );
        // …y el desugar sintetizó su método de implementación (`code_of` panica si no existe).
        code_of(src, "M", "lambda$make$0");
        // Ambos métodos pasan el verificador JVMS-estricto.
        verify_all(src, "M");
    }

    #[test]
    fn a_capturing_lambda_passes_the_strict_verifier() {
        // Captura el parámetro `r`: la impl es **estática** con `r` de parámetro de cabecera, y el
        // sitio lo empuja antes del `invokedynamic` (descriptor `(Runnable)Runnable`).
        let src =
            "public class M { public static Runnable wrap(Runnable r) { return () -> r.run(); } }";
        assert!(code_of(src, "M", "wrap").0.contains(&INVOKEDYNAMIC));
        code_of(src, "M", "lambda$wrap$0");
        verify_all(src, "M");
    }

    #[test]
    fn a_this_capturing_lambda_uses_an_instance_impl_method() {
        // Un miembro de instancia en el cuerpo captura `this`: la impl es un método de **instancia**
        // (`REF_invokeSpecial` en el bootstrap) y el sitio empuja `this` de receptor.
        let src =
            "public class M { int v; Runnable make() { return () -> { int x = this.v; }; } }";
        assert!(code_of(src, "M", "make").0.contains(&INVOKEDYNAMIC));
        verify_all(src, "M");
    }

    // ---- method references: las cuatro formas del §15.13.1 ----
    //
    // Reusan el nodo `Indy` y el mismo bootstrap; lo distinto es el `MethodHandle`, que apunta al
    // método/constructor **real** (sin sintetizar nada). Que el fuente **compile** ya prueba que la
    // bajada funcionó: si `lower_method_ref` no la reconociera, el nodo quedaría como `MethodRef` y
    // la barrera del emisor cortaría la compilación.

    #[test]
    fn a_static_method_ref_lowers_to_invokedynamic() {
        // `M::hello` contra `Supplier<String>`: `REF_invokeStatic`, sin captura.
        let src = "import java.util.function.Supplier; \
                   public class M { static String hello() { return \"hi\"; } \
                   public static Supplier<String> make() { return M::hello; } }";
        assert!(code_of(src, "M", "make").0.contains(&INVOKEDYNAMIC));
        verify_all(src, "M");
    }

    #[test]
    fn an_unbound_instance_method_ref_passes_the_verifier() {
        // `String::length` contra `Function<String,Integer>`: el receptor es el **primer parámetro**
        // del SAM (`REF_invokeVirtual`), sin captura.
        let src = "import java.util.function.Function; \
                   public class M { public static Function<String,Integer> make() { return String::length; } }";
        assert!(code_of(src, "M", "make").0.contains(&INVOKEDYNAMIC));
        verify_all(src, "M");
    }

    #[test]
    fn a_bound_instance_method_ref_captures_its_receiver() {
        // `s::length` contra `Supplier<Integer>`: el receptor es el **valor** `s`, que se captura y
        // el sitio empuja antes del `invokedynamic` (descriptor `(String)Supplier`).
        let src = "import java.util.function.Supplier; \
                   public class M { public static Supplier<Integer> make(String s) { return s::length; } }";
        assert!(code_of(src, "M", "make").0.contains(&INVOKEDYNAMIC));
        verify_all(src, "M");
    }

    #[test]
    fn a_constructor_ref_uses_new_invoke_special() {
        // `M::new` contra `Supplier<M>`: `REF_newInvokeSpecial` sobre el constructor real.
        let src = "import java.util.function.Supplier; \
                   public class M { public M() {} public static Supplier<M> make() { return M::new; } }";
        assert!(code_of(src, "M", "make").0.contains(&INVOKEDYNAMIC));
        verify_all(src, "M");
    }

    #[test]
    fn a_new_with_a_fully_qualified_class_name_resolves_and_emits() {
        // Finding #20: `new java.lang.Object()` (nombre **cualificado**) resolvía a `Unresolved`, y el
        // codegen no podía emitir el `new` + `invokespecial <init>` (fallaba con "tipo que no se pudo
        // resolver"). Ahora el FQN se resuelve por su último segmento entre los externos. Compila,
        // emite el `new`, y **verifica**.
        let src = "public class Z { public static Object make() { return new java.lang.Object(); } }";
        assert!(code_of(src, "Z", "make").0.contains(&0xbb), "debe emitir el opcode `new` (0xbb)");
        verify_all(src, "Z");
    }

    #[test]
    fn a_lambda_argument_to_a_generic_constructor_lowers_to_invokedynamic() {
        // Finding #16: la lambda infiere su target (`Supplier<long[]>`) **a través** del type-param del
        // constructor (`Box<A>(Supplier<A>)` con `Box<long[]>`). Antes quedaba sin target (`Unresolved`)
        // y el emisor no podía bajarla —"necesita invokedynamic"—; ahora la **fase 2** del constructor le
        // da el target del parámetro y baja a `invokedynamic`.
        let src = "import java.util.function.Supplier; \
                   public class M { static final class Box<A> { Box(Supplier<A> s) {} } \
                                    static Box<long[]> make() { return new Box<long[]>(() -> new long[1]); } }";
        assert!(code_of(src, "M", "make").0.contains(&INVOKEDYNAMIC), "la lambda debe bajar a invokedynamic");
        verify_all(src, "M");
    }

    #[test]
    fn a_bare_reference_to_an_anonymous_class_own_field_resolves() {
        // Un campo propio de una clase **anónima** referenciado **pelado** (`c`, no `this.c`) debe
        // resolver. La anónima se hoistea a una local sintética **después** del último
        // `resolve_symbols`, así que sus campos quedaban sin `Resolved::Field` y el nombre pelado —que
        // lo exige— fallaba con "no se encuentra: c"; `this.c` (que tolera `Unresolved`) sí pasaba.
        // `hoist_anonymous` re-resuelve ahora.
        let src = "public class C { interface I { int f(); } \
                   I it() { return new I() { int c = 5; public int f() { return c; } }; } }";
        verify_all(src, "C");
        verify_all(src, "C$1");
    }

    #[test]
    fn an_anonymous_class_in_a_generic_enclosing_class_captures_this0() {
        // Finding #13: una clase **anónima** dentro de una clase **genérica** que llama métodos del
        // envolvente debe capturar `this$0`. Antes, con la envolvente genérica, no se generaba el
        // campo/param `this$0` (y la anónima ni se emitía). El `.class` de la anónima **verifica** —una
        // llamada sin receptor, o sin el `this$0`, no pasaría el verificador—.
        let src = "public class Gen<E> { interface Iter<T> { boolean hasNext(); } \
                   int size() { return 0; } \
                   Iter<E> it() { return new Iter<E>() { public boolean hasNext() { return size() > 0; } }; } }";
        verify_all(src, "Gen");
        verify_all(src, "Gen$1");
    }

    #[test]
    fn a_text_block_compiles_and_verifies_as_a_string_constant() {
        // Un text block se decodifica en el parser a un `String` común; el emisor lo baja a un `ldc`.
        let src = "public class T { public static String s() { return \"\"\"\n            hola\n            mundo\n            \"\"\"; } }";
        verify_all(src, "T");
    }

    // ---- métodos puente (§8.4.8.3 / §15.12.4.5) ----

    const ACC_BRIDGE: u16 = 0x0040;
    const ACC_SYNTHETIC: u16 = 0x1000;

    /// Compila `src` y re-parsea la clase `simple` con la JVM propia.
    /// #208 — un tipo que el generador **no** resuelve no se emite: se reporta.
    ///
    /// El `import` de tipo único se da por bueno en la fase semántica (no hay forma de descartarlo
    /// sin classpath), así que el nombre llega sin resolver al generador. Antes salía un `.class`
    /// con dos mentiras distintas: descriptor `Ljava/lang/Object;` y `Signature: LNoExiste;`, que no
    /// es ninguna clase. Ahora falla, que es lo único honesto.
    #[test]
    fn an_unresolvable_type_in_a_signature_is_reported_not_invented() {
        let e = crate::javac::compile("import p.NoExiste; class C { void f(NoExiste x, Class<?> t) {} }")
            .expect_err("un tipo que no resuelve no puede emitirse");
        assert!(
            e.message.contains("NoExiste"),
            "el error tiene que nombrar el tipo: {}",
            e.message
        );
    }

    /// La contraprueba: una **variable de tipo** del método no es un tipo sin resolver. Su
    /// descriptor es el de su erasure, y eso ya andaba; lo que no puede es dispararse la auditoría.
    #[test]
    fn a_method_type_variable_is_not_reported_as_unresolvable() {
        let c = compiled_class("class C { <T extends Number> T id(T x) { return x; } }", "C");
        assert!(has_method(&c, "id", "(Ljava/lang/Number;)Ljava/lang/Number;", 0));
    }

    /// El SAM puede estar declarado en una **superinterfaz**, y entonces sus tipos hablan de las
    /// variables de *esa* interfaz. Sin sustituir por la cadena de herencia, el método sintético de
    /// la lambda quedaba declarando un retorno `R2` —el parámetro de `F2`, que no es ningún tipo—.
    /// La erasure lo tapaba borrándolo a `Object`; se ve recién cuando la variable tiene cota.
    #[test]
    fn a_lambda_of_an_inherited_sam_substitutes_the_superinterfaces_type_variables() {
        let c = compiled_class(
            "interface F2<A, B, R2 extends Number> { R2 ap(A a, B b); }              interface Bin extends F2<String, String, Integer> {                  static Bin uno() { return (a, b) -> Integer.valueOf(1); } }",
            "Bin",
        );
        // El método sintético de la lambda devuelve `Integer` —el argumento que `Bin` le dio a
        // `R2`—, no la cota de `R2` ni `Object`.
        assert!(
            c.methods.iter().any(|m| {
                let n = c.utf8(m.name_index).unwrap_or("");
                let d = c.utf8(m.descriptor_index).unwrap_or("");
                n.starts_with("lambda$") && d.ends_with(")Ljava/lang/Integer;")
            }),
            "descriptores emitidos: {:?}",
            c.methods
                .iter()
                .map(|m| (c.utf8(m.name_index), c.utf8(m.descriptor_index)))
                .collect::<Vec<_>>()
        );
    }

    /// El bytecode del primer metodo llamado `name`.
    fn code_named(jvm: &ClassFile, name: &str) -> Vec<u8> {
        let m = jvm
            .methods
            .iter()
            .find(|m| jvm.utf8(m.name_index) == Some(name))
            .unwrap_or_else(|| panic!("el metodo {name}"));
        jvm.member_code(m).expect("con Code").code.clone()
    }

    /// El valor del atributo `SourceFile` (4.7.10) de la clase.
    fn source_file_of(jvm: &ClassFile) -> Option<String> {
        let a = jvm.attributes.iter().find(|a| jvm.utf8(a.name_index) == Some("SourceFile"))?;
        let idx = u16::from_be_bytes([a.info[0], a.info[1]]);
        jvm.utf8(idx).map(str::to_string)
    }

    /// Hay un `Fieldref` a `clase.nombre` en el pool.
    fn has_fieldref(jvm: &ClassFile, class: &str, name: &str) -> bool {
        use crate::jvm::parser::constant_pool::ConstantPoolEntry as E;
        jvm.constant_pool.iter().any(|e| {
            let E::FieldRef { class_index, name_and_type_index } = e else { return false };
            let Some(E::Class { name_index }) = jvm.constant_pool.get(*class_index as usize - 1)
            else {
                return false;
            };
            let Some(E::NameAndType { name_index: n, .. }) =
                jvm.constant_pool.get(*name_and_type_index as usize - 1)
            else {
                return false;
            };
            jvm.utf8(*name_index) == Some(class) && jvm.utf8(*n) == Some(name)
        })
    }

    /// El **cast sintetico** (5.5): el retorno de un metodo generico llega **borrado** a la pila,
    /// asi que encadenar sobre el emitia un `invokevirtual Hoja.n` sobre un `Object`. Eso no
    /// verifica —la JVM real lo rechaza con `VerifyError` antes de ejecutar nada— y de este lado no
    /// se veia porque nuestro interprete despacha por el objeto real.
    #[test]
    fn a_call_on_an_erased_generic_return_gets_its_checkcast() {
        let c = compiled_class(
            "class Hoja { int n() { return 3; } }              interface Caja<T> { T get(); }              class C { static int f(Caja<Hoja> b) { return b.get().n(); } }",
            "C",
        );
        let code = code_named(&c, "f");
        let cc = code.iter().position(|&b| b == super::CHECKCAST).expect("hay checkcast");
        let iv = code.iter().position(|&b| b == 0xb6).expect("hay invokevirtual");
        assert!(cc < iv, "el checkcast va **antes** del despacho: {code:?}");
    }

    /// #276 — un `@interface` es una **interfaz de anotacion**, y eso son **dos** cosas que la
    /// spec pone y el fuente no escribe: el flag `ACC_ANNOTATION` (JVMS 4.1) y el `extends
    /// java.lang.annotation.Annotation` implicito (JLS 9.6).
    ///
    /// Sin el flag, `Class.isAnnotation()` **niega** que lo sea; sin la superinterfaz, una
    /// anotacion no es asignable a `Annotation`, que es el tipo por el que la reflexion las
    /// devuelve. O sea que ninguna de las dos mitades de la reflexion de anotaciones podia
    /// funcionar — con la clase entera bien formada y todos sus miembros correctos, que es por lo
    /// que ninguna comparacion de firmas lo veia.
    #[test]
    fn an_annotation_type_gets_acc_annotation_and_its_implicit_superinterface() {
        let c = compiled_class("@interface Marca { String valor(); }", "Marca");
        assert_eq!(
            c.access_flags & 0x2000,
            0x2000,
            "falta ACC_ANNOTATION: flags = {:#06x}",
            c.access_flags
        );
        // Y sigue siendo una interfaz: ACC_ANNOTATION **acompania** a ACC_INTERFACE, no lo suple.
        assert_eq!(c.access_flags & 0x0200, 0x0200, "ACC_INTERFACE tambien va");
        let supers: Vec<&str> = c.interfaces.iter().filter_map(|&i| c.class_name(i)).collect();
        assert!(
            supers.contains(&"java/lang/annotation/Annotation"),
            "falta la superinterfaz implicita: {supers:?}"
        );
    }

    /// #209 - el literal de clase de un **primitivo** (15.8.2). No hay entrada
    /// `CONSTANT_Class` para `int`, asi que no es un `ldc`: es el campo `TYPE` de su
    /// envoltorio. Antes ni parseaba, con lo cual no habia **ninguna** expresion Java cuyo
    /// valor fuera el mirror de un primitivo.
    #[test]
    fn a_primitive_class_literal_is_the_wrappers_type_field() {
        let c = compiled_class(
            "class C { static Class<?> a() { return int.class; } \
             static Class<?> b() { return void.class; } \
             static Class<?> d() { return int[].class; } }",
            "C",
        );
        assert_eq!(code_named(&c, "a")[0], GETSTATIC, "`int.class` es un getstatic, no un ldc");
        assert!(has_fieldref(&c, "java/lang/Integer", "TYPE"), "el campo es `Integer.TYPE`");
        assert!(has_fieldref(&c, "java/lang/Void", "TYPE"), "`void.class` es `Void.TYPE`");
        // Un **array** de primitivo si es un `ldc`, pero de su descriptor (`[I`).
        assert_eq!(code_named(&c, "d")[0], LDC);
    }

    /// #235 - el `SourceFile` (4.7.10) es de la **unidad**, no de cada clase. Una secundaria
    /// decia "Secundaria.java" y una anidada "Anidada.java": archivos que no existen, y que un
    /// depurador no encuentra.
    #[test]
    fn every_class_of_a_unit_reports_the_same_source_file() {
        let src = "public class Principal { static class Anidada {} } class Secundaria {}";
        for simple in ["Principal", "Principal$Anidada", "Secundaria"] {
            let c = compiled_class(src, simple);
            assert_eq!(
                source_file_of(&c).as_deref(),
                Some("Principal.java"),
                "el SourceFile de {simple}"
            );
        }
    }

    fn compiled_class(src: &str, simple: &str) -> ClassFile {
        let (_, bytes) = compile_all(src)
            .into_iter()
            .find(|(n, _)| n.rsplit('/').next() == Some(simple))
            .unwrap_or_else(|| panic!("la clase {simple}"));
        let n = COUNTER.fetch_add(1, Ordering::Relaxed);
        let dir = std::env::temp_dir().join(format!("javac_bridge_{}_{n}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        let path = dir.join(format!("{simple}.class"));
        std::fs::write(&path, &bytes).unwrap();
        let jvm = ClassFile::from_path(path.to_str().unwrap()).expect("el .class debe parsear");
        let _ = std::fs::remove_dir_all(&dir);
        jvm
    }

    /// ¿La clase tiene un método `name` con descriptor `desc` y los flags dados prendidos?
    fn has_method(jvm: &ClassFile, name: &str, desc: &str, flags: u16) -> bool {
        jvm.methods.iter().any(|m| {
            jvm.utf8(m.name_index) == Some(name)
                && jvm.utf8(m.descriptor_index) == Some(desc)
                && m.access_flags & flags == flags
        })
    }

    #[test]
    fn an_enum_in_a_named_package_keeps_its_machinery() {
        // Finding #21 (regresión): un `enum` en un paquete **nombrado** perdía TODA la maquinaria
        // —constantes, `$VALUES`, `values()`, `valueOf()`, `<clinit>`, y hasta el ctor `(String,int)`—
        // porque el FQN que el desugar usaba para `table.class(fqn)` no llevaba el paquete, así que la
        // síntesis se salteaba y salía un `final class extends Enum` vacío. Un enum en el paquete por
        // defecto no se veía afectado (de ahí que ningún test lo cazara).
        let jvm = compiled_class("package pk; public enum E { A, B, C }", "E");
        for c in ["A", "B", "C", "$VALUES"] {
            assert!(
                jvm.fields.iter().any(|f| jvm.utf8(f.name_index) == Some(c)),
                "falta el campo `{c}`",
            );
        }
        for meth in ["values", "valueOf", "<init>", "<clinit>"] {
            assert!(
                jvm.methods.iter().any(|m| jvm.utf8(m.name_index) == Some(meth)),
                "falta el método `{meth}`",
            );
        }
    }

    // ---- atributo MethodParameters (§4.7.24) ----

    /// `(nombre?, flags)` de cada parámetro formal del método `name` (`parameters_count` es un `u1`).
    fn method_params(jvm: &ClassFile, name: &str) -> Option<Vec<(Option<String>, u16)>> {
        let m = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some(name))?;
        let a = m.attributes.iter().find(|a| jvm.utf8(a.name_index) == Some("MethodParameters"))?;
        let b = &a.info;
        let n = b[0] as usize; // ¡u1!
        let mut out = Vec::new();
        for i in 0..n {
            let off = 1 + i * 4;
            let name_idx = u16::from_be_bytes([b[off], b[off + 1]]);
            let flags = u16::from_be_bytes([b[off + 2], b[off + 3]]);
            let pname = (name_idx != 0).then(|| jvm.utf8(name_idx).map(str::to_string)).flatten();
            out.push((pname, flags));
        }
        Some(out)
    }

    #[test]
    fn a_method_records_its_parameter_names_and_final_flag() {
        let jvm = compiled_class("class C { int f(int x, final String y) { return x; } }", "C");
        assert_eq!(
            method_params(&jvm, "f"),
            Some(vec![(Some("x".to_string()), 0u16), (Some("y".to_string()), 0x0010)]), // `y` es final
        );
    }

    #[test]
    fn a_no_arg_method_has_no_method_parameters() {
        let jvm = compiled_class("class C { int f() { return 0; } }", "C");
        assert_eq!(method_params(&jvm, "f"), None);
    }

    #[test]
    fn a_record_canonical_constructor_keeps_the_component_names() {
        let jvm = compiled_class("public record P(int a, String b) {}", "P");
        assert_eq!(
            method_params(&jvm, "<init>"),
            Some(vec![(Some("a".to_string()), 0u16), (Some("b".to_string()), 0u16)]),
        );
    }

    #[test]
    fn a_synthetic_captured_parameter_is_marked_synthetic() {
        // El `this$0` que el desugar inyecta en el ctor de una interna de instancia va `ACC_SYNTHETIC`.
        let jvm = compiled_class("class Outer { int v; class Inner { int g() { return v; } } }", "Outer$Inner");
        let params = method_params(&jvm, "<init>").expect("el ctor tiene parámetros");
        assert!(
            params.iter().any(|(_, f)| f & 0x1000 != 0),
            "algún parámetro sintético (`this$0`): {params:?}",
        );
    }

    // ---- atributo Exceptions (§4.7.5) ----

    /// Las clases de la cláusula `throws` del método `name`, vía el atributo `Exceptions` (o `None`
    /// si el método no lleva el atributo, es decir no declara `throws`).
    fn thrown_exceptions(jvm: &ClassFile, name: &str) -> Option<Vec<String>> {
        let m = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some(name))?;
        let a = m.attributes.iter().find(|a| jvm.utf8(a.name_index) == Some("Exceptions"))?;
        let b = &a.info;
        let n = u16::from_be_bytes([b[0], b[1]]) as usize;
        Some(
            (0..n)
                .filter_map(|i| {
                    let idx = u16::from_be_bytes([b[2 + i * 2], b[3 + i * 2]]);
                    jvm.class_name(idx).map(str::to_string)
                })
                .collect(),
        )
    }

    #[test]
    fn a_throws_clause_emits_the_exceptions_attribute_in_order() {
        let jvm = compiled_class("class C { void f() throws Exception, RuntimeException {} }", "C");
        assert_eq!(
            thrown_exceptions(&jvm, "f"),
            Some(vec!["java/lang/Exception".to_string(), "java/lang/RuntimeException".to_string()]),
            "las clases del `throws`, en orden",
        );
    }

    #[test]
    fn a_method_without_throws_has_no_exceptions_attribute() {
        let jvm = compiled_class("class C { void f() {} }", "C");
        assert_eq!(thrown_exceptions(&jvm, "f"), None);
    }

    #[test]
    fn an_abstract_method_keeps_its_throws() {
        // Un método sin `Code` (abstracto) igual lleva `Exceptions` como atributo del método.
        let jvm =
            compiled_class("abstract class C { abstract void f() throws RuntimeException; }", "C");
        assert_eq!(
            thrown_exceptions(&jvm, "f"),
            Some(vec!["java/lang/RuntimeException".to_string()]),
        );
    }

    // ---- atributo LineNumberTable (§4.7.12) ----

    /// Los pares `(start_pc, line)` del `LineNumberTable` del método `name` (`None` si no lo lleva).
    fn line_number_table(jvm: &ClassFile, name: &str) -> Option<Vec<(u16, u16)>> {
        let m = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some(name))?;
        let code = jvm.member_code(m)?;
        let a = code.attributes.iter().find(|a| jvm.utf8(a.name_index) == Some("LineNumberTable"))?;
        let b = &a.info;
        let n = u16::from_be_bytes([b[0], b[1]]) as usize;
        Some(
            (0..n)
                .map(|i| {
                    let o = 2 + i * 4;
                    (u16::from_be_bytes([b[o], b[o + 1]]), u16::from_be_bytes([b[o + 2], b[o + 3]]))
                })
                .collect(),
        )
    }

    #[test]
    fn a_method_body_maps_bytecode_offsets_to_source_lines() {
        // Cada sentencia en su propia línea (la `\` continúa la string sin meter un `\n` de más).
        let src = "class C {\n\
                   int f(int n) {\n\
                   int a = n + 1;\n\
                   int b = a * 2;\n\
                   return b;\n\
                   }\n\
                   }";
        let jvm = compiled_class(src, "C");
        let lnt = line_number_table(&jvm, "f").expect("f tiene LineNumberTable");
        let lines: Vec<u16> = lnt.iter().map(|&(_, l)| l).collect();
        assert_eq!(lines, vec![3, 4, 5], "una entrada por sentencia, en orden de línea");
        assert!(
            lnt.windows(2).all(|w| w[0].0 < w[1].0),
            "start_pc estrictamente creciente y sin repetir: {lnt:?}",
        );
    }

    #[test]
    fn a_constructor_maps_the_implicit_super_call_to_its_declaration_line() {
        // El `super()` implícito (pc 0) se mapea a la línea del ctor, no queda sin línea.
        let src = "class C {\n\
                   C() {\n\
                   }\n\
                   }";
        let jvm = compiled_class(src, "C");
        let lnt = line_number_table(&jvm, "<init>").expect("el ctor tiene LineNumberTable");
        assert_eq!(lnt.first().map(|&(pc, l)| (pc, l)), Some((0, 2)), "pc 0 → línea 2: {lnt:?}");
    }

    // ---- atributo LocalVariableTable (§4.7.13) ----

    /// Las entradas del `LocalVariableTable` del método `name`: `(start_pc, length, nombre, desc,
    /// slot)`. `None` si el método no lleva el atributo.
    fn local_var_table(jvm: &ClassFile, name: &str) -> Option<Vec<(u16, u16, String, String, u16)>> {
        let m = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some(name))?;
        let code = jvm.member_code(m)?;
        let a =
            code.attributes.iter().find(|a| jvm.utf8(a.name_index) == Some("LocalVariableTable"))?;
        let b = &a.info;
        let n = u16::from_be_bytes([b[0], b[1]]) as usize;
        Some(
            (0..n)
                .map(|i| {
                    let o = 2 + i * 10;
                    let rd = |k: usize| u16::from_be_bytes([b[o + k], b[o + k + 1]]);
                    let name = jvm.utf8(rd(4)).unwrap_or("?").to_string();
                    let desc = jvm.utf8(rd(6)).unwrap_or("?").to_string();
                    (rd(0), rd(2), name, desc, rd(8))
                })
                .collect(),
        )
    }

    #[test]
    fn records_this_params_and_locals_with_names_descriptors_and_slots() {
        let jvm =
            compiled_class("class C { int f(int n, String s) { int a = n + 1; return a; } }", "C");
        let lvt = local_var_table(&jvm, "f").expect("f tiene LocalVariableTable");
        let by_name: std::collections::HashMap<String, (String, u16)> =
            lvt.iter().map(|(_, _, n, d, s)| (n.clone(), (d.clone(), *s))).collect();
        assert_eq!(by_name.get("this"), Some(&("LC;".to_string(), 0)), "`this` en slot 0");
        assert_eq!(by_name.get("n"), Some(&("I".to_string(), 1)));
        assert_eq!(by_name.get("s"), Some(&("Ljava/lang/String;".to_string(), 2)));
        assert_eq!(by_name.get("a"), Some(&("I".to_string(), 3)));
    }

    #[test]
    fn a_static_method_has_no_this_in_the_local_variable_table() {
        let jvm = compiled_class("class C { static int f(int n) { return n; } }", "C");
        let lvt = local_var_table(&jvm, "f").expect("tiene LVT");
        assert!(lvt.iter().all(|(_, _, n, _, _)| n != "this"), "un `static` no tiene `this`: {lvt:?}");
        assert!(lvt.iter().any(|(_, _, n, _, s)| n == "n" && *s == 0), "`n` en slot 0: {lvt:?}");
    }

    #[test]
    fn reused_slots_get_disjoint_ranges() {
        // Dos locales **vivos** en bloques disjuntos comparten slot; sus rangos NO deben solaparse.
        // (Deben estar vivos: un local muerto lo descarta el largo-0, igual que javac.)
        let jvm = compiled_class("class C { void m() { { int a = 1; a++; } { int b = 2; b++; } } }", "C");
        let lvt = local_var_table(&jvm, "m").expect("tiene LVT");
        let a = lvt.iter().find(|e| e.2 == "a").expect("a");
        let b = lvt.iter().find(|e| e.2 == "b").expect("b");
        assert_eq!(a.4, b.4, "a y b reusan el mismo slot: {lvt:?}");
        for i in 0..lvt.len() {
            for j in i + 1..lvt.len() {
                let (si, li, _, _, sa) = &lvt[i];
                let (sj, lj, _, _, sb) = &lvt[j];
                if sa == sb {
                    let overlap = si < &(sj + lj) && sj < &(si + li);
                    assert!(!overlap, "rangos solapados en slot {sa}: {:?} vs {:?}", lvt[i], lvt[j]);
                }
            }
        }
    }

    // ---- atributo ConstantValue (§4.7.2) ----

    /// Describe el `ConstantValue` del campo `name` como texto normalizado (`None` si no lo lleva).
    fn constant_value(jvm: &ClassFile, name: &str) -> Option<String> {
        use crate::jvm::parser::ConstantPoolEntry as CP;
        let f = jvm.fields.iter().find(|f| jvm.utf8(f.name_index) == Some(name))?;
        let a = f.attributes.iter().find(|a| jvm.utf8(a.name_index) == Some("ConstantValue"))?;
        let idx = u16::from_be_bytes([a.info[0], a.info[1]]);
        Some(match jvm.constant_pool.get((idx - 1) as usize)? {
            CP::Integer(v) => format!("int:{v}"),
            CP::Long(v) => format!("long:{v}"),
            CP::Float(v) => format!("float:{v}"),
            CP::Double(v) => format!("double:{v}"),
            CP::String { string_index } => format!("String:{}", jvm.utf8(*string_index)?),
            _ => return None,
        })
    }

    #[test]
    fn static_final_constant_fields_get_a_constant_value_attribute() {
        let src = "class C { \
            static final int I = 42; \
            static final long L = 7; \
            static final float F = 1.5f; \
            static final double D = 3.5; \
            static final boolean B = true; \
            static final byte BY = 5; \
            static final String S = \"hi\"; \
        }";
        let jvm = compiled_class(src, "C");
        assert_eq!(constant_value(&jvm, "I").as_deref(), Some("int:42"));
        assert_eq!(constant_value(&jvm, "L").as_deref(), Some("long:7"));
        assert_eq!(constant_value(&jvm, "F").as_deref(), Some("float:1.5"));
        assert_eq!(constant_value(&jvm, "D").as_deref(), Some("double:3.5"));
        assert_eq!(constant_value(&jvm, "B").as_deref(), Some("int:1"), "boolean true = 1");
        assert_eq!(constant_value(&jvm, "BY").as_deref(), Some("int:5"));
        assert_eq!(constant_value(&jvm, "S").as_deref(), Some("String:hi"));
    }

    #[test]
    fn static_final_constant_expressions_are_folded_for_constant_value() {
        // §15.29 fuera del `case`: el `ConstantValue` no solo pliega literales, sino expresiones
        // aritméticas, de bits y de shift, con la promoción binaria (§5.6.2) y los casts.
        let src = "class C { \
            static final int SUM = 2 + 3; \
            static final int SECONDS = 60 * 60; \
            static final int MASK = 1 << 4; \
            static final int BITS = 0xF0 | 0x0F; \
            static final int PREC = 1 + 2 * 3; \
            static final long BIG = 1000L * 1000L; \
            static final byte NARROW = (byte)(100 + 100); \
            static final double HALF = 1.0 / 2.0; \
            static final int MIXED = (int)(3L + 4); \
        }";
        let jvm = compiled_class(src, "C");
        assert_eq!(constant_value(&jvm, "SUM").as_deref(), Some("int:5"));
        assert_eq!(constant_value(&jvm, "SECONDS").as_deref(), Some("int:3600"));
        assert_eq!(constant_value(&jvm, "MASK").as_deref(), Some("int:16"));
        assert_eq!(constant_value(&jvm, "BITS").as_deref(), Some("int:255"));
        assert_eq!(constant_value(&jvm, "PREC").as_deref(), Some("int:7"), "1 + 2*3, no (1+2)*3");
        assert_eq!(constant_value(&jvm, "BIG").as_deref(), Some("long:1000000"));
        assert_eq!(constant_value(&jvm, "NARROW").as_deref(), Some("int:-56"), "(byte)200 = -56");
        assert_eq!(constant_value(&jvm, "HALF").as_deref(), Some("double:0.5"));
        assert_eq!(constant_value(&jvm, "MIXED").as_deref(), Some("int:7"));
    }

    #[test]
    fn references_to_other_final_constants_are_folded_into_constant_value() {
        // A#3 (§13.4.9/§15.29): un `static final` que **referencia** a otra constante de la unidad
        // se pliega igual. El fixpoint resuelve las cadenas de dependencia en cualquier orden de
        // fuente (`C` referencia a `B`, que referencia a `A`, declaradas después).
        let src = "class C { \
            static final int A = 10; \
            static final int B = A * 2; \
            static final int C2 = B + A; \
            static final int MASKED = FLAG_A | FLAG_B; \
            static final int FLAG_A = 1; \
            static final int FLAG_B = 2; \
            static final long BIGREF = A + 1L; \
            static final byte NB = (byte)(A * 20); \
            static final int QUAL = Other.K + 1; \
        } \
        class Other { static final int K = 41; }";
        let jvm = compiled_class(src, "C");
        assert_eq!(constant_value(&jvm, "A").as_deref(), Some("int:10"));
        assert_eq!(constant_value(&jvm, "B").as_deref(), Some("int:20"), "A*2");
        assert_eq!(constant_value(&jvm, "C2").as_deref(), Some("int:30"), "B+A = 20+10");
        assert_eq!(constant_value(&jvm, "MASKED").as_deref(), Some("int:3"), "1|2, decl. después");
        assert_eq!(constant_value(&jvm, "BIGREF").as_deref(), Some("long:11"), "int A promovido a long");
        assert_eq!(constant_value(&jvm, "NB").as_deref(), Some("int:-56"), "(byte)200");
        // Referencia cualificada a una constante de **otra** clase de la misma unidad (`Other.K`).
        assert_eq!(constant_value(&jvm, "QUAL").as_deref(), Some("int:42"), "Other.K + 1");
        assert_eq!(constant_value(&compiled_class(src, "Other"), "K").as_deref(), Some("int:41"));
    }

    #[test]
    fn constant_conditions_fold_the_branch_and_keep_semantics() {
        // A#4 (§15.28): condiciones constantes no se testean en runtime, pero el resultado es correcto.
        // `if` con comparación constante-verdadera: toma el `then`.
        let t = "public class K { public static int run() { if (1 < 2) return 5; return 9; } }";
        assert_eq!(run_int(t, "K", "run", vec![]), 5);
        // `if` constante-falsa: toma el `else` (rama muerta no ejecutada).
        let f = "public class K { public static int run() { if (2 < 1) return 5; return 9; } }";
        assert_eq!(run_int(f, "K", "run", vec![]), 9);
        // Lógico constante.
        let a = "public class K { public static int run() { if (true && false) return 5; return 9; } }";
        assert_eq!(run_int(a, "K", "run", vec![]), 9);
    }

    #[test]
    fn constant_string_concatenation_is_folded_for_constant_value() {
        // A#2 (§15.29/§15.18.1): la concatenación `+` de constantes de tipo `String` es una constante
        // `String` → va a `ConstantValue`, sin `StringBuilder`. Incluye la conversión a texto (§5.1.11)
        // de operandos `int`/`long`/`char`/`boolean`, y referencias a otras `String`/numéricas finales.
        let src = "class C { \
            static final String AB = \"a\" + \"b\"; \
            static final String NUM = \"n=\" + 42; \
            static final String EXPR = \"x=\" + (1 + 2); \
            static final String CH = \"c=\" + 'z'; \
            static final String BOOL = \"b=\" + true; \
            static final String LONG = \"l=\" + 100L; \
            static final String HELLO = HI + \", \" + WHO + \"!\"; \
            static final String HI = \"Hola\"; \
            static final String WHO = \"mundo\"; \
            static final String WITHREF = \"v=\" + N; \
            static final int N = 7; \
        }";
        let jvm = compiled_class(src, "C");
        assert_eq!(constant_value(&jvm, "AB").as_deref(), Some("String:ab"));
        assert_eq!(constant_value(&jvm, "NUM").as_deref(), Some("String:n=42"));
        assert_eq!(constant_value(&jvm, "EXPR").as_deref(), Some("String:x=3"), "1+2 aritmética, no concat");
        assert_eq!(constant_value(&jvm, "CH").as_deref(), Some("String:c=z"), "char → 'z'");
        assert_eq!(constant_value(&jvm, "BOOL").as_deref(), Some("String:b=true"));
        assert_eq!(constant_value(&jvm, "LONG").as_deref(), Some("String:l=100"));
        assert_eq!(constant_value(&jvm, "HELLO").as_deref(), Some("String:Hola, mundo!"), "cadena de refs");
        assert_eq!(constant_value(&jvm, "WITHREF").as_deref(), Some("String:v=7"), "ref a int final");
    }

    #[test]
    fn non_constant_string_concatenation_has_no_constant_value() {
        // Si un operando **no** es constante (un parámetro, un `new`), la concatenación no es constante:
        // cae al `<clinit>` con su `StringBuilder`, sin `ConstantValue`.
        let src = "class C { \
            static String mk() { return \"y\"; } \
            static final String NC = \"x\" + mk(); \
        }";
        let jvm = compiled_class(src, "C");
        assert_eq!(constant_value(&jvm, "NC"), None, "concat con llamada no es constante");
    }

    #[test]
    fn non_final_or_non_constant_static_fields_have_no_constant_value() {
        let src = "class C { \
            static int x = 1; \
            static final int y = f(); \
            static int f() { return 2; } \
        }";
        let jvm = compiled_class(src, "C");
        assert_eq!(constant_value(&jvm, "x"), None, "no-`final`: se inicializa en `<clinit>`");
        assert_eq!(constant_value(&jvm, "y"), None, "init no-constante: se inicializa en `<clinit>`");
    }

    #[test]
    fn a_constant_field_is_not_also_stored_in_clinit() {
        // Con `ConstantValue`, javac **no** emite el store en `<clinit>`: una clase con solo un campo
        // constante no lleva `<clinit>` en absoluto.
        let jvm = compiled_class("class C { static final int X = 42; }", "C");
        assert!(constant_value(&jvm, "X").is_some(), "X lleva ConstantValue");
        assert!(
            jvm.methods.iter().all(|m| jvm.utf8(m.name_index) != Some("<clinit>")),
            "no debería haber `<clinit>` para un campo puramente constante",
        );
    }

    // ---- java.lang.Object: super_class = 0 y <init> sin super() (finding #6) ----

    #[test]
    fn object_gets_super_class_zero_and_an_init_without_self_super_call() {
        // Solo `java.lang.Object` no tiene superclase: `super_class = 0` y su `<init>` no llama a
        // `super()` (sería a sí mismo). El resto de las clases sí llevan super_class y super() implícito.
        let jvm =
            compiled_class("package java.lang; public class Object { public Object() {} }", "Object");
        assert_eq!(jvm.super_class, 0, "solo java.lang.Object tiene super_class = 0");
        let init = jvm
            .methods
            .iter()
            .find(|m| jvm.utf8(m.name_index) == Some("<init>"))
            .expect("Object tiene <init>");
        let code = jvm.member_code(init).expect("<init> tiene Code");
        const INVOKESPECIAL: u8 = 0xb7;
        assert!(
            !code.code.contains(&INVOKESPECIAL),
            "Object.<init> no debe llamar a super() (self-call): {:?}",
            code.code,
        );
    }

    #[test]
    fn an_ordinary_class_still_has_a_real_super_class_and_super_call() {
        // Contraprueba: una clase común mantiene super_class = Object y el `super()` implícito.
        let jvm = compiled_class("public class C {}", "C");
        assert_ne!(jvm.super_class, 0, "una clase común sí tiene super_class");
        assert_eq!(jvm.class_name(jvm.super_class), Some("java/lang/Object"));
        let init = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some("<init>")).unwrap();
        let code = jvm.member_code(init).expect("Code");
        assert!(code.code.contains(&0xb7), "el ctor por defecto llama a super()");
    }

    // ---- RuntimeVisibleTypeAnnotations (§4.7.20): parámetros de tipo ----

    #[test]
    fn a_class_type_parameter_annotation_is_emitted() {
        // `class C<@Foo T>` → RuntimeVisibleTypeAnnotations con target_type 0x00 (param de tipo de
        // clase), type_parameter_index 0, type_path vacío. `@Foo` es `@Retention(RUNTIME)`.
        let src = "import java.lang.annotation.*; \
                   @Retention(RetentionPolicy.RUNTIME) @interface Foo {} \
                   public class C<@Foo T> {}";
        let jvm = compiled_class(src, "C");
        let a = jvm
            .attributes
            .iter()
            .find(|a| jvm.utf8(a.name_index) == Some("RuntimeVisibleTypeAnnotations"))
            .expect("C tiene RuntimeVisibleTypeAnnotations");
        let b = &a.info;
        assert_eq!(u16::from_be_bytes([b[0], b[1]]), 1, "una type annotation");
        assert_eq!(b[2], 0x00, "target_type 0x00 = parámetro de tipo de clase");
        assert_eq!(b[3], 0, "type_parameter_index 0");
        assert_eq!(b[4], 0, "type_path vacío (path_length 0)");
    }

    #[test]
    fn a_method_type_parameter_annotation_uses_target_0x01() {
        let src = "import java.lang.annotation.*; \
                   @Retention(RetentionPolicy.RUNTIME) @interface Foo {} \
                   public class C { <@Foo T> void m() {} }";
        let jvm = compiled_class(src, "C");
        let m = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some("m")).expect("m");
        let a = m
            .attributes
            .iter()
            .find(|a| jvm.utf8(a.name_index) == Some("RuntimeVisibleTypeAnnotations"))
            .expect("m tiene RuntimeVisibleTypeAnnotations");
        assert_eq!(a.info[2], 0x01, "target_type 0x01 = parámetro de tipo de método");
    }

    #[test]
    fn a_type_use_annotation_on_a_field_is_a_type_annotation_not_a_declaration_one() {
        let src = "import java.lang.annotation.*; \
                   @Target(ElementType.TYPE_USE) @Retention(RetentionPolicy.RUNTIME) @interface Tu {} \
                   public class C { @Tu String f; }";
        let jvm = compiled_class(src, "C");
        let f = jvm.fields.iter().find(|f| jvm.utf8(f.name_index) == Some("f")).unwrap();
        let ta = f
            .attributes
            .iter()
            .find(|a| jvm.utf8(a.name_index) == Some("RuntimeVisibleTypeAnnotations"))
            .expect("el campo tiene RuntimeVisibleTypeAnnotations");
        assert_eq!(ta.info[2], 0x13, "target_type 0x13 = tipo del campo");
        assert!(
            !f.attributes.iter().any(|a| jvm.utf8(a.name_index) == Some("RuntimeVisibleAnnotations")),
            "una anotación TYPE_USE-only NO va a las anotaciones de declaración",
        );
    }

    #[test]
    fn a_cast_type_annotation_goes_inside_the_code_attribute_as_target_0x47() {
        // `(@Tu String) o` → RuntimeVisibleTypeAnnotations **dentro del Code**, target 0x47 (CAST),
        // con el offset del `checkcast` (tras el `aload_0`, offset 1) y `type_argument_index` 0.
        let src = "import java.lang.annotation.*; \
                   @Target(ElementType.TYPE_USE) @Retention(RetentionPolicy.RUNTIME) @interface Tu {} \
                   public class C { Object m(Object o) { return (@Tu String) o; } }";
        let jvm = compiled_class(src, "C");
        let m = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some("m")).expect("m");
        let code = jvm.member_code(m).expect("Code");
        let ta = code
            .attributes
            .iter()
            .find(|a| jvm.utf8(a.name_index) == Some("RuntimeVisibleTypeAnnotations"))
            .expect("el Code tiene RuntimeVisibleTypeAnnotations");
        assert_eq!(u16::from_be_bytes([ta.info[0], ta.info[1]]), 1, "una type annotation");
        assert_eq!(ta.info[2], 0x47, "target_type 0x47 = cast");
        assert_eq!(u16::from_be_bytes([ta.info[3], ta.info[4]]), 1, "offset del checkcast");
        assert_eq!(ta.info[5], 0, "type_argument_index 0 (cast simple)");
    }

    #[test]
    fn a_local_variable_type_annotation_uses_target_0x40_with_a_live_range() {
        // `@Tu String s = ...;` → target 0x40 (LOCAL_VARIABLE) en el Code, con `target_info` = una
        // tabla de un rango `{start_pc, length, index}` (el mismo rango que el LocalVariableTable).
        let src = "import java.lang.annotation.*; \
                   @Target(ElementType.TYPE_USE) @Retention(RetentionPolicy.RUNTIME) @interface Tu {} \
                   public class C { int m() { @Tu String s = \"hi\"; return s.length(); } }";
        let jvm = compiled_class(src, "C");
        let m = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some("m")).expect("m");
        let code = jvm.member_code(m).expect("Code");
        let ta = code
            .attributes
            .iter()
            .find(|a| jvm.utf8(a.name_index) == Some("RuntimeVisibleTypeAnnotations"))
            .expect("el Code tiene RuntimeVisibleTypeAnnotations");
        assert_eq!(u16::from_be_bytes([ta.info[0], ta.info[1]]), 1, "una type annotation");
        assert_eq!(ta.info[2], 0x40, "target_type 0x40 = variable local");
        assert_eq!(u16::from_be_bytes([ta.info[3], ta.info[4]]), 1, "table_length = 1 (un rango)");
        // ...seguido de {start_pc(2), length(2), index(2)} y luego el type_path (path_length 0).
        assert_eq!(ta.info[9], 0, "type_path vacío tras la tabla de rangos");
    }

    #[test]
    fn a_type_use_annotation_on_a_return_and_a_parameter() {
        let src = "import java.lang.annotation.*; \
                   @Target(ElementType.TYPE_USE) @Retention(RetentionPolicy.RUNTIME) @interface Tu {} \
                   public class C { @Tu String m(@Tu String s) { return s; } }";
        let jvm = compiled_class(src, "C");
        let m = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some("m")).unwrap();
        let ta = m
            .attributes
            .iter()
            .find(|a| jvm.utf8(a.name_index) == Some("RuntimeVisibleTypeAnnotations"))
            .expect("el método tiene RuntimeVisibleTypeAnnotations");
        let b = &ta.info;
        // Dos type_annotations: el retorno (0x14) y el parámetro 0 (0x16).
        assert_eq!(u16::from_be_bytes([b[0], b[1]]), 2, "retorno + parámetro");
        let targets: Vec<u8> = {
            // primera entrada arranca en b[2]; retorno (0x14, sin target_info) tiene largo 1+0+1+ann;
            // en vez de parsear largos, basta con que ambos targets 0x14 y 0x16 aparezcan.
            b.iter().copied().filter(|&x| x == 0x14 || x == 0x16).collect()
        };
        assert!(targets.contains(&0x14), "retorno target 0x14: {b:?}");
        assert!(targets.contains(&0x16), "parámetro target 0x16: {b:?}");
    }

    #[test]
    fn a_source_retention_type_parameter_annotation_is_not_emitted() {
        // Sin `@Retention(RUNTIME)` la anotación no va al atributo *visible* (igual que las normales).
        let jvm = compiled_class("@interface Bar {} public class C<@Bar T> {}", "C");
        assert!(
            !jvm.attributes.iter().any(|a| jvm.utf8(a.name_index) == Some("RuntimeVisibleTypeAnnotations")),
            "una anotación de retención SOURCE/CLASS no emite RuntimeVisibleTypeAnnotations",
        );
    }

    // ---- AnnotationDefault (§4.7.22) ----

    #[test]
    fn an_annotation_element_default_string_is_emitted() {
        let jvm = compiled_class("public @interface Foo { String value() default \"hi\"; }", "Foo");
        let m = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some("value")).unwrap();
        let a = m
            .attributes
            .iter()
            .find(|a| jvm.utf8(a.name_index) == Some("AnnotationDefault"))
            .expect("el elemento `value` tiene AnnotationDefault");
        // element_value: tag 's' (String) + const_value_index (Utf8 "hi").
        assert_eq!(a.info[0], b's', "tag 's' (String)");
        let idx = u16::from_be_bytes([a.info[1], a.info[2]]);
        assert_eq!(jvm.utf8(idx), Some("hi"), "el valor por defecto es \"hi\"");
    }

    #[test]
    fn an_annotation_element_without_default_has_no_attribute() {
        let jvm = compiled_class("public @interface Foo { String value(); }", "Foo");
        let m = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some("value")).unwrap();
        assert!(
            !m.attributes.iter().any(|a| jvm.utf8(a.name_index) == Some("AnnotationDefault")),
            "un elemento sin `default` no lleva AnnotationDefault",
        );
    }

    // ---- RuntimeVisibleParameterAnnotations (§4.7.18) ----

    #[test]
    fn a_parameter_declaration_annotation_is_emitted() {
        // `@Deprecated` es retenida en runtime y no es TYPE_USE → va al RuntimeVisibleParameterAnnotations.
        let jvm = compiled_class("public class C { void m(@Deprecated String s) {} }", "C");
        let m = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some("m")).unwrap();
        let a = m
            .attributes
            .iter()
            .find(|a| jvm.utf8(a.name_index) == Some("RuntimeVisibleParameterAnnotations"))
            .expect("el método tiene RuntimeVisibleParameterAnnotations");
        let b = &a.info;
        assert_eq!(b[0], 1, "num_parameters = 1");
        assert_eq!(u16::from_be_bytes([b[1], b[2]]), 1, "el parámetro 0 tiene 1 anotación");
    }

    #[test]
    fn parameters_without_annotations_get_empty_entries() {
        let jvm = compiled_class("public class C { void m(int a, @Deprecated int b) {} }", "C");
        let m = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some("m")).unwrap();
        let b = &m
            .attributes
            .iter()
            .find(|a| jvm.utf8(a.name_index) == Some("RuntimeVisibleParameterAnnotations"))
            .unwrap()
            .info;
        assert_eq!(b[0], 2, "num_parameters = 2");
        assert_eq!(u16::from_be_bytes([b[1], b[2]]), 0, "param 0 (`a`) sin anotaciones");
        // Tras la entrada vacía del param 0 (2 bytes), la del param 1 (`b`) tiene 1 anotación.
        assert_eq!(u16::from_be_bytes([b[3], b[4]]), 1, "param 1 (`b`) con 1 anotación");
    }

    #[test]
    fn a_jdk_annotation_gets_a_fully_qualified_descriptor() {
        use crate::jvm::parser::ConstantPoolEntry as CP;
        // `@Deprecated` (java.lang, externo) se carga → su descriptor sale cualificado
        // (`Ljava/lang/Deprecated;`), no `LDeprecated;` (que la reflexión no encontraría).
        let jvm = compiled_class("public class C { @Deprecated void m() {} }", "C");
        let has = |s: &str| {
            jvm.constant_pool.iter().any(|e| matches!(e, CP::Utf8(u) if u == s))
        };
        assert!(has("Ljava/lang/Deprecated;"), "descriptor cualificado de @Deprecated");
        assert!(!has("LDeprecated;"), "no el descriptor sin paquete");
    }

    #[test]
    fn a_method_without_parameter_annotations_has_no_such_attribute() {
        let jvm = compiled_class("public class C { void m(int a) {} }", "C");
        let m = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some("m")).unwrap();
        assert!(
            !m.attributes.iter().any(|a| jvm.utf8(a.name_index) == Some("RuntimeVisibleParameterAnnotations")),
            "sin anotaciones de parámetro, no se emite el atributo",
        );
    }

    // ---- flags de enum (§4.1/§4.5) y enum anidado en interfaz (finding #12) ----

    #[test]
    fn an_enum_gets_enum_and_final_flags_with_enum_constant_fields() {
        let jvm = compiled_class("public enum E { A, B }", "E");
        const ACC_FINAL: u16 = 0x0010;
        const ACC_ENUM: u16 = 0x4000;
        const ACC_SYNTHETIC: u16 = 0x1000;
        assert!(jvm.access_flags & ACC_ENUM != 0, "la clase enum lleva ACC_ENUM: {:#x}", jvm.access_flags);
        assert!(jvm.access_flags & ACC_FINAL != 0, "un enum simple es final: {:#x}", jvm.access_flags);
        let flags = |name: &str| {
            jvm.fields.iter().find(|f| jvm.utf8(f.name_index) == Some(name)).map(|f| f.access_flags)
        };
        assert!(flags("A").unwrap() & ACC_ENUM != 0, "la constante `A` lleva ACC_ENUM");
        assert!(flags("$VALUES").unwrap() & ACC_SYNTHETIC != 0, "`$VALUES` es sintético");
    }

    #[test]
    fn an_enum_nested_in_an_interface_gets_the_full_machinery() {
        // finding #12: un `enum` anidado en una **interfaz** ya no sale degenerado — misma maquinaria
        // y flags que uno anidado en una clase (constantes, `$VALUES`, `values`/`valueOf`, `<clinit>`).
        let jvm = compiled_class("public interface I { enum E { A, B } }", "I$E");
        const ACC_ENUM: u16 = 0x4000;
        assert!(jvm.access_flags & ACC_ENUM != 0, "el enum en interfaz lleva ACC_ENUM: {:#x}", jvm.access_flags);
        let has_method = |n: &str| jvm.methods.iter().any(|m| jvm.utf8(m.name_index) == Some(n));
        assert!(has_method("values"), "tiene `values()`");
        assert!(has_method("valueOf"), "tiene `valueOf()`");
        assert!(has_method("<clinit>"), "tiene `<clinit>`");
        assert!(
            jvm.fields.iter().any(|f| jvm.utf8(f.name_index) == Some("A")),
            "declara la constante `A`",
        );
    }

    // ---- classpath / -cp (finding #7) ----

    #[test]
    fn a_classpath_dir_lets_a_file_reference_a_separately_compiled_type() {
        // Compilar `Sib` a `.class` en un dir, luego analizar `User` que lo referencia con ese dir en
        // el classpath: `Sib` (que no está ni en la unidad ni en el JDK) resuelve.
        let n = COUNTER.fetch_add(1, Ordering::Relaxed);
        let dir = std::env::temp_dir().join(format!("javac_cp_{}_{n}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        let sib =
            crate::javac::compile("public class Sib { public static int v() { return 42; } }").unwrap();
        for (internal, bytes) in &sib {
            let simple = internal.rsplit('/').next().unwrap_or(internal);
            std::fs::write(dir.join(format!("{simple}.class")), bytes).unwrap();
        }
        let (_u, _t, errors) = crate::javac::analyze_cp(
            "public class User { int r() { return Sib.v(); } }",
            &[dir.clone()],
        )
        .unwrap();
        let msgs: Vec<String> = errors.iter().map(|e| e.to_string()).collect();
        let _ = std::fs::remove_dir_all(&dir);
        assert!(!msgs.iter().any(|m| m.contains("Sib")), "`Sib` debe resolver vía classpath: {msgs:?}");
    }

    #[test]
    fn a_method_inherited_from_a_classpath_superinterface_resolves() {
        // Finding #14: con `-cp`, un método **heredado** de una superinterfaz cargada por `-cp`
        // (`size()` de `Collection`, con `List extends Collection`, **ambas** en el classpath) debe
        // resolver sobre un `List`. Antes daba "no se encuentra el método: size" — el finder cargaba
        // el tipo nombrado pero no caminaba sus superinterfaces del `-cp`. El fix de #15 (candidates
        // camina el grafo **completo** de supertipos) lo cubre también para las externas del `-cp`.
        let n = COUNTER.fetch_add(1, Ordering::Relaxed);
        let dir = std::env::temp_dir().join(format!("javac_f14_{}_{n}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        let write = |classes: &[(String, Vec<u8>)]| {
            for (internal, b) in classes {
                let simple = internal.rsplit('/').next().unwrap_or(internal);
                std::fs::write(dir.join(format!("{simple}.class")), b).unwrap();
            }
        };
        write(&crate::javac::compile("public interface Collection<E> { int size(); }").unwrap());
        write(
            &crate::javac::compile_cp(
                "public interface List<E> extends Collection<E> {}",
                &[dir.clone()],
            )
            .unwrap(),
        );
        let (_u, _t, errors) = crate::javac::analyze_cp(
            "public class User { int f(List<String> l) { return l.size(); } }",
            &[dir.clone()],
        )
        .unwrap();
        let msgs: Vec<String> = errors.iter().map(|e| e.to_string()).collect();
        let _ = std::fs::remove_dir_all(&dir);
        assert!(
            !msgs.iter().any(|m| m.contains("size")),
            "`size` heredado de Collection vía -cp debe resolver: {msgs:?}"
        );
    }

    #[test]
    fn without_the_classpath_a_cross_file_reference_is_unresolved() {
        // Contraprueba: sin `-cp`, un tipo que no está en la unidad ni en el JDK no resuelve.
        let (_u, _t, errors) =
            crate::javac::analyze("public class User2 { int r() { return Zzq.v(); } }").unwrap();
        let msgs: Vec<String> = errors.iter().map(|e| e.to_string()).collect();
        assert!(msgs.iter().any(|m| m.contains("Zzq")), "sin classpath `Zzq` no resuelve: {msgs:?}");
    }

    #[test]
    fn a_source_type_shadowed_on_the_classpath_is_not_loaded_as_a_redundant_external() {
        // Finding #19 (source-shadows-classpath): compilar un tipo **empaquetado** cuyo propio `.class`
        // está en el `-cp` (p. ej. su output previo) NO debe cargarlo como un externo **redundante** —
        // el fuente sombrea—. Antes, un fuente `p.A` referenciado por su nombre simple `A` no matcheaba
        // `table.class("A")` (registrado como `p.A`) y se cargaba del `-cp`, arrastrando su jerarquía:
        // el disparo del hang del #19. Se comprueba que **no** quede un externo `A`.
        let n = COUNTER.fetch_add(1, Ordering::Relaxed);
        let dir = std::env::temp_dir().join(format!("javac_f19_{}_{n}", std::process::id()));
        std::fs::create_dir_all(dir.join("p")).unwrap();
        let a = crate::javac::compile("package p; public class A {}").unwrap();
        for (internal, bytes) in &a {
            let simple = internal.rsplit('/').next().unwrap_or(internal);
            std::fs::write(dir.join("p").join(format!("{simple}.class")), bytes).unwrap();
        }
        // El fuente referencia `A` por su nombre simple (retorno), lo que dispara el intento de carga.
        let (_u, t, _e) = crate::javac::analyze_cp(
            "package p; public class A { A self() { return this; } }",
            &[dir.clone()],
        )
        .unwrap();
        let external_a = t.external("A").is_some();
        let _ = std::fs::remove_dir_all(&dir);
        assert!(!external_a, "el fuente `p.A` sombrea el `-cp`: no debe cargarse un externo `A`");
    }

    // ---- atributos NestHost / NestMembers (§4.7.28 / §4.7.29) ----

    fn nest_host(jvm: &ClassFile) -> Option<String> {
        let a = jvm.attributes.iter().find(|a| jvm.utf8(a.name_index) == Some("NestHost"))?;
        let idx = u16::from_be_bytes([a.info[0], a.info[1]]);
        jvm.class_name(idx).map(str::to_string)
    }
    fn nest_members(jvm: &ClassFile) -> Vec<String> {
        let Some(a) = jvm.attributes.iter().find(|a| jvm.utf8(a.name_index) == Some("NestMembers"))
        else {
            return Vec::new();
        };
        let b = &a.info;
        let n = u16::from_be_bytes([b[0], b[1]]) as usize;
        (0..n)
            .filter_map(|i| {
                let idx = u16::from_be_bytes([b[2 + i * 2], b[3 + i * 2]]);
                jvm.class_name(idx).map(str::to_string)
            })
            .collect()
    }

    #[test]
    fn a_nest_host_lists_its_member_and_the_member_points_back() {
        let src = "class Outer { class Inner {} }";
        let outer = compiled_class(src, "Outer");
        assert!(nest_host(&outer).is_none(), "una top-level es su propio host");
        assert!(nest_members(&outer).contains(&"Outer$Inner".to_string()), "el host lista al miembro");
        let inner = compiled_class(src, "Outer$Inner");
        assert_eq!(nest_host(&inner).as_deref(), Some("Outer"), "el miembro apunta al host");
        assert!(nest_members(&inner).is_empty(), "un miembro no hostea nada");
    }

    #[test]
    fn the_nest_is_flat_and_transitive() {
        // Todo el árbol de anidamiento cae en **un** nest, hosteado por la top-level: un `Deep`
        // apunta a `Outer`, no a `Inner`, y `Outer` lista a los dos.
        let src = "class Outer { class Inner { class Deep {} } }";
        let outer = compiled_class(src, "Outer");
        let members = nest_members(&outer);
        assert!(members.contains(&"Outer$Inner".to_string()));
        assert!(members.contains(&"Outer$Inner$Deep".to_string()), "transitivo: {members:?}");
        let deep = compiled_class(src, "Outer$Inner$Deep");
        assert_eq!(nest_host(&deep).as_deref(), Some("Outer"), "el host es la top-level");
    }

    #[test]
    fn a_local_class_is_a_nestmate() {
        let src = "class M { Object f() { class L {} return new L(); } }";
        let m = compiled_class(src, "M");
        assert!(nest_members(&m).contains(&"M$1L".to_string()), "la local es nestmate: {:?}", nest_members(&m));
        let l = compiled_class(src, "M$1L");
        assert_eq!(nest_host(&l).as_deref(), Some("M"));
    }

    #[test]
    fn a_top_level_class_without_nested_has_no_nest_attributes() {
        let jvm = compiled_class("class C {}", "C");
        assert!(nest_host(&jvm).is_none());
        assert!(nest_members(&jvm).is_empty());
    }

    // ---- atributo Record (§4.7.30) ----

    /// `(nombre, descriptor, signature?)` de cada componente del atributo `Record`, o `None` si no lo
    /// lleva.
    fn record_components(jvm: &ClassFile) -> Option<Vec<(String, String, Option<String>)>> {
        let a = jvm.attributes.iter().find(|a| jvm.utf8(a.name_index) == Some("Record"))?;
        let b = &a.info;
        let rd = |k: usize| u16::from_be_bytes([b[k], b[k + 1]]);
        let n = rd(0) as usize;
        let mut off = 2;
        let mut out = Vec::new();
        for _ in 0..n {
            let name = jvm.utf8(rd(off))?.to_string();
            let desc = jvm.utf8(rd(off + 2))?.to_string();
            let attrs = rd(off + 4) as usize;
            off += 6;
            let mut sig = None;
            for _ in 0..attrs {
                let aname = jvm.utf8(rd(off)).map(str::to_string);
                let alen = u32::from_be_bytes([b[off + 2], b[off + 3], b[off + 4], b[off + 5]]) as usize;
                if aname.as_deref() == Some("Signature") {
                    sig = jvm.utf8(rd(off + 6)).map(str::to_string);
                }
                off += 6 + alen;
            }
            out.push((name, desc, sig));
        }
        Some(out)
    }

    #[test]
    fn a_record_emits_its_components_super_and_final() {
        let jvm = compiled_class("public record P(int a, String b) {}", "P");
        assert_eq!(
            record_components(&jvm),
            Some(vec![
                ("a".to_string(), "I".to_string(), None),
                ("b".to_string(), "Ljava/lang/String;".to_string(), None),
            ]),
        );
        assert_eq!(jvm.class_name(jvm.super_class), Some("java/lang/Record"), "extiende Record");
        assert!(jvm.access_flags & 0x0010 != 0, "un record es `final`: flags {:#x}", jvm.access_flags);
    }

    #[test]
    fn a_generic_record_component_gets_a_signature() {
        let jvm = compiled_class("public record Box<T>(T val) {}", "Box");
        let comps = record_components(&jvm).expect("atributo Record");
        assert_eq!(comps.len(), 1);
        assert_eq!(comps[0].0, "val");
        assert_eq!(comps[0].1, "Ljava/lang/Object;", "descriptor **borrado**");
        assert_eq!(comps[0].2.as_deref(), Some("TT;"), "la firma genérica del componente");
    }

    #[test]
    fn a_non_record_class_has_no_record_attribute() {
        let jvm = compiled_class("class C { int a; }", "C");
        assert!(record_components(&jvm).is_none());
    }

    #[test]
    fn a_generic_record_compiles_and_verifies_end_to_end() {
        // Con la erasure del descriptor de la variable de tipo, un record genérico verifica: sus
        // campo/accessor/ctor usan `Object` borrado, y su firma genérica va en `Signature`/`Record`.
        verify_all("public record Box<T>(T val) {}", "Box");
    }

    // ---- atributo EnclosingMethod (§4.7.7) ----

    /// `(clase_envolvente, (nombre, descriptor)?)` del atributo `EnclosingMethod`, o `None` si no lo
    /// lleva. El método es `None` cuando `method_index` es 0 (no está en un método).
    fn enclosing_method(jvm: &ClassFile) -> Option<(String, Option<(String, String)>)> {
        use crate::jvm::parser::ConstantPoolEntry as CP;
        let a = jvm.attributes.iter().find(|a| jvm.utf8(a.name_index) == Some("EnclosingMethod"))?;
        let b = &a.info;
        let class_idx = u16::from_be_bytes([b[0], b[1]]);
        let method_idx = u16::from_be_bytes([b[2], b[3]]);
        let class = jvm.class_name(class_idx)?.to_string();
        let method = if method_idx == 0 {
            None
        } else {
            match jvm.constant_pool.get((method_idx - 1) as usize)? {
                CP::NameAndType { name_index, descriptor_index } => {
                    Some((jvm.utf8(*name_index)?.to_string(), jvm.utf8(*descriptor_index)?.to_string()))
                }
                _ => return None,
            }
        };
        Some((class, method))
    }

    #[test]
    fn a_local_class_in_a_method_records_its_enclosing_method() {
        let src = "class M { Object f() { class L {} return new L(); } }";
        let jvm = compiled_class(src, "M$1L");
        assert_eq!(
            enclosing_method(&jvm),
            Some(("M".to_string(), Some(("f".to_string(), "()Ljava/lang/Object;".to_string())))),
        );
    }

    #[test]
    fn an_anonymous_class_in_a_method_records_its_enclosing_method() {
        let src = "class Base {} class M { Object f() { return new Base(){}; } }";
        let jvm = compiled_class(src, "M$1");
        assert_eq!(
            enclosing_method(&jvm),
            Some(("M".to_string(), Some(("f".to_string(), "()Ljava/lang/Object;".to_string())))),
        );
    }

    #[test]
    fn an_anonymous_class_in_a_static_initializer_has_no_method() {
        // En un inicializador (no un método): `class_index` sí, `method_index` = 0.
        let src = "class Base {} class M { static { Object x = new Base(){}; } }";
        let jvm = compiled_class(src, "M$1");
        assert_eq!(enclosing_method(&jvm), Some(("M".to_string(), None)));
    }

    #[test]
    fn a_member_class_has_no_enclosing_method() {
        // `EnclosingMethod` es **solo** para local/anónimas (§4.7.7).
        let jvm = compiled_class("class Outer { class Inner {} }", "Outer$Inner");
        assert_eq!(enclosing_method(&jvm), None);
    }

    // ---- atributo InnerClasses (§4.7.6) ----

    /// `(inner_binary, outer_binary?, inner_name?, flags)` de cada entrada `InnerClasses`.
    fn inner_classes(jvm: &ClassFile) -> Vec<(String, Option<String>, Option<String>, u16)> {
        let Some(a) = jvm.attributes.iter().find(|a| jvm.utf8(a.name_index) == Some("InnerClasses"))
        else {
            return Vec::new();
        };
        let b = &a.info;
        let n = u16::from_be_bytes([b[0], b[1]]) as usize;
        let mut out = Vec::new();
        for i in 0..n {
            let o = 2 + i * 8;
            let rd = |k: usize| u16::from_be_bytes([b[o + k], b[o + k + 1]]);
            let (inner, outer, name, flags) = (rd(0), rd(2), rd(4), rd(6));
            let cname = |idx: u16| jvm.class_name(idx).map(str::to_string);
            out.push((
                cname(inner).unwrap_or_default(),
                (outer != 0).then(|| cname(outer)).flatten(),
                (name != 0).then(|| jvm.utf8(name).map(str::to_string)).flatten(),
                flags,
            ));
        }
        out
    }

    #[test]
    fn a_member_class_is_listed_in_both_class_files() {
        let src = "class Outer { class Inner {} }";
        let expected = ("Outer$Inner".to_string(), Some("Outer".to_string()), Some("Inner".to_string()));
        for name in ["Outer", "Outer$Inner"] {
            let jvm = compiled_class(src, name);
            let has = inner_classes(&jvm)
                .iter()
                .any(|(i, o, n, _)| (i.clone(), o.clone(), n.clone()) == expected);
            assert!(has, "`{name}.class` debe listar `Inner` como miembro: {:?}", inner_classes(&jvm));
        }
    }

    #[test]
    fn a_static_nested_class_carries_acc_static() {
        let jvm = compiled_class("class Outer { static class Inner {} }", "Outer");
        let e = inner_classes(&jvm).into_iter().find(|(i, ..)| i == "Outer$Inner").expect("Inner");
        assert_eq!(e.1.as_deref(), Some("Outer"), "outer");
        assert!(e.3 & 0x0008 != 0, "ACC_STATIC en un nested estático: flags {:#x}", e.3);
    }

    #[test]
    fn a_local_class_has_a_name_but_no_outer() {
        // Una local: sin `outer` (no es miembro), y con el nombre **del fuente**.
        //
        // El `1` del binario `M$1L` es del compilador, no del programa: está ahí para que dos
        // locales llamadas `L` en dos métodos distintos no colisionen. `inner_name` es lo que
        // `getSimpleName()` devuelve, así que dejarle el prefijo hacía que una clase declarada
        // `L` dijera llamarse `1L` — un nombre que no se puede escribir en Java (#278).
        let src = "class M { Object f() { class L {} return new L(); } }";
        let jvm = compiled_class(src, "M$1L");
        let e = inner_classes(&jvm).into_iter().find(|(i, ..)| i == "M$1L").expect("la local");
        assert_eq!(e.1, None, "una local no tiene outer");
        assert_eq!(e.2.as_deref(), Some("L"), "una local se llama como la declararon");
    }

    #[test]
    fn an_anonymous_class_has_neither_outer_nor_name() {
        let src = "class Base {} class M { Object f() { return new Base(){}; } }";
        let jvm = compiled_class(src, "M$1");
        let e = inner_classes(&jvm).into_iter().find(|(i, ..)| i == "M$1").expect("la anónima");
        assert_eq!(e.1, None, "una anónima no tiene outer");
        assert_eq!(e.2, None, "una anónima no tiene nombre");
    }

    #[test]
    fn a_top_level_class_has_no_inner_classes() {
        let jvm = compiled_class("class C {}", "C");
        assert!(inner_classes(&jvm).is_empty(), "una clase top-level sin anidadas no lleva InnerClasses");
    }

    #[test]
    fn a_native_method_emits_acc_native_not_abstract() {
        // Un `native` en una clase **concreta** debe salir `ACC_NATIVE` (0x0100), **no** `ACC_ABSTRACT`
        // (0x0400): si no, una clase no-abstracta quedaría con métodos abstractos — un `.class`
        // inválido. Es el primer bug que destapó el dogfooding de KajiLibrary (los `native` de
        // `Object`/`String`).
        let jvm = compiled_class(
            "final class S { public native int len(); public int two() { return 2; } }",
            "S",
        );
        let native = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some("len")).unwrap();
        assert!(native.access_flags & 0x0100 != 0, "`len` debe ser ACC_NATIVE");
        assert!(native.access_flags & 0x0400 == 0, "`len` no debe ser ACC_ABSTRACT");
        // Un método normal sigue con su `Code`, sin ninguno de esos flags.
        let normal = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some("two")).unwrap();
        assert!(normal.access_flags & (0x0100 | 0x0400) == 0, "`two` es un método normal");
    }

    #[test]
    fn a_synchronized_method_emits_acc_synchronized() {
        // #255 — el flag es lo **único** que hace que la JVM tome el monitor del receptor (§2.11.10).
        // Sin él, un `wait()`/`notifyAll()` en el cuerpo tira `IllegalMonitorStateException`, así que
        // no es un adorno del `javap`: es la diferencia entre que el método funcione o no.
        let jvm = compiled_class(
            "class S { public synchronized int uno() { return 1; }              public static synchronized int dos() { return 2; }              public int tres() { return 3; } }",
            "S",
        );
        let flags = |n: &str| {
            jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some(n)).unwrap().access_flags
        };
        assert_eq!(flags("uno"), 0x0001 | 0x0020, "public synchronized");
        assert_eq!(flags("dos"), 0x0001 | 0x0008 | 0x0020, "public static synchronized");
        assert_eq!(flags("tres") & 0x0020, 0, "un método normal no lleva ACC_SYNCHRONIZED");
    }

    #[test]
    fn a_method_type_parameter_erases_to_its_bound_not_to_object() {
        // #100/#241 — la borradura de una variable de tipo es su **primera cota** (§4.6). Funcionaba
        // para los parámetros de tipo de la **clase** y no para los del **método**: esos no viven en
        // el scope de la clase, así que `resolve_type_id` fallaba y se caía al `Object` por defecto.
        // El `Signature` salía bien por otro camino, que es lo que lo hizo durar tanto: el `javap`
        // muestra la firma genérica correcta y el desajuste solo aparece al sobreescribir
        // (`AbstractMethodError`). Los descriptores de abajo se cotejaron contra el javac del JDK 25.
        let jvm = compiled_class(
            "public class M<A extends Number, T> {                public <N extends Number> N met(Class<N> c) { return null; }                public <U extends Comparable<U>> void cmp(U u) { }                public <X> void sinCota(X x) { }                public <B extends Number> B[] arreglo(B[] b) { return b; }                public void deClase(A a, T t) { } }",
            "M",
        );
        let desc = |n: &str| {
            let m = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some(n)).unwrap();
            jvm.utf8(m.descriptor_index).unwrap().to_string()
        };
        assert_eq!(desc("met"), "(Ljava/lang/Class;)Ljava/lang/Number;");
        assert_eq!(desc("cmp"), "(Ljava/lang/Comparable;)V");
        assert_eq!(desc("sinCota"), "(Ljava/lang/Object;)V"); // sin cota, la erasure sí es Object
        assert_eq!(desc("arreglo"), "([Ljava/lang/Number;)[Ljava/lang/Number;"); // atraviesa el array
        // Control: los parámetros de tipo de la **clase** seguían andando y siguen.
        assert_eq!(desc("deClase"), "(Ljava/lang/Number;Ljava/lang/Object;)V");
    }

    #[test]
    fn a_field_gets_acc_volatile_and_acc_transient() {
        // #115/#236 — los dos comparten bit con flags de **método** (`ACC_BRIDGE` 0x0040 y
        // `ACC_VARARGS` 0x0080), asi que no pueden salir de la misma tabla: son tablas distintas
        // (§4.5 vs §4.6), no un espacio comun. Esa colision es la razon de que faltaran.
        // `strictfp` no emite nada a proposito: desde la v17 es implicito y el javac real tampoco
        // lo emite (avisa que el modificador sobra).
        let jvm = compiled_class(
            "public class M { public volatile int v; public transient int t;              public volatile transient long vt; public int normal;              public strictfp double s() { return 1.0; } }",
            "M",
        );
        let ff = |n: &str| {
            jvm.fields.iter().find(|f| jvm.utf8(f.name_index) == Some(n)).unwrap().access_flags
        };
        assert_eq!(ff("v"), 0x0041); // ACC_PUBLIC | ACC_VOLATILE
        assert_eq!(ff("t"), 0x0081); // ACC_PUBLIC | ACC_TRANSIENT
        assert_eq!(ff("vt"), 0x00c1); // los dos a la vez
        assert_eq!(ff("normal"), 0x0001);
        let m = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some("s")).unwrap();
        assert_eq!(m.access_flags, 0x0001, "`strictfp` no emite ACC_STRICT en la v17+");
    }

    #[test]
    fn a_member_type_of_an_interface_is_implicitly_public() {
        // #242 — §9.5: los tipos miembro de una interfaz son implicitamente `public` y `static`,
        // igual que sus campos son `public static final` (§9.3) y sus metodos `public` (§9.4).
        // Sin esto quedaban package-private e inusables desde otro paquete. Los valores se
        // cotejaron contra el javac del JDK 25.
        let src = "public interface I { interface In { } class C { } enum E { A } }";
        assert_eq!(compiled_class(src, "I$In").access_flags, 0x0601); // PUBLIC|INTERFACE|ABSTRACT
        assert_eq!(compiled_class(src, "I$C").access_flags, 0x0021); // PUBLIC|SUPER
        assert_eq!(compiled_class(src, "I$E").access_flags, 0x4031); // PUBLIC|FINAL|SUPER|ENUM
        // Control: un miembro de una **clase** no recibe nada implicito.
        let dentro = compiled_class("public class K { interface In { } }", "K$In");
        assert_eq!(dentro.access_flags, 0x0600, "un anidado de clase no es publico por implicacion");
    }

    #[test]
    fn a_super_call_uses_invokespecial_on_the_direct_superclass() {
        // #231/#125 — `super.m()` no despacha virtualmente (§15.12.4.4): saltea el override de
        // **esta** clase. El emisor ni siquiera soportaba `super` como receptor.
        // Dos cosas se cotejaron contra el javac del JDK 25: que el opcode es `invokespecial`, y
        // que el dueño del methodref es la **superclase directa** y no la clase que declara el
        // metodo — con `C extends B extends A` y `f` declarado en `A`, javac emite `B.f`.
        let src = "public class M {                    static class A { int f() { return 1; } int g() { return 9; } }                    static class B extends A { }                    static class C extends B { int f() { return super.f() + 1; }                                               int h() { return super.g(); } } }";
        // El comportamiento en runtime lo cubre `repros/finding_231.java` (da 2); acá se verifica la
        // **forma**, que es donde estaba el defecto, y que el resultado pasa el verificador estricto.
        verify_all(src, "M$C");
        let c = compiled_class(src, "M$C");
        let code = |n: &str| {
            let m = c.methods.iter().find(|m| c.utf8(m.name_index) == Some(n)).unwrap();
            c.member_code(m).expect("con Code").code.clone()
        };
        // aload_0; invokespecial …  (0x2a, 0xb7)
        assert_eq!(&code("f")[0..2], &[0x2a, 0xb7]);
        assert_eq!(&code("h")[0..2], &[0x2a, 0xb7]);
    }

    #[test]
    fn an_abstract_covariant_override_still_gets_its_bridge() {
        // #233 — el puente lo necesita el **llamador** que ve el supertipo, no la implementacion,
        // asi que hace falta aunque el metodo que estrecha el retorno sea `abstract`. javac lo
        // emite igual, y **concreto**: `aload_0; invokevirtual <el angosto>; areturn`. El despacho
        // virtual lo lleva al override real de la subclase concreta.
        let jvm = compiled_class(
            "public class M {                abstract static class Base { abstract Object dame(); }                abstract static class Media extends Base { public abstract String dame(); } }",
            "M$Media",
        );
        let br = jvm
            .methods
            .iter()
            .find(|m| jvm.utf8(m.descriptor_index) == Some("()Ljava/lang/Object;"))
            .expect("el puente `Object dame()` debe estar");
        assert_eq!(br.access_flags, 0x1041); // ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC
        let code = jvm.member_code(br).expect("el puente lleva cuerpo, no es abstracto").code;
        assert_eq!(code[0], 0x2a, "aload_0");
        assert_eq!(code[1], 0xb6, "invokevirtual, no invokespecial");
        assert_eq!(*code.last().unwrap(), 0xb0, "areturn");
        // Y el metodo angosto sigue siendo el abstracto declarado.
        let real = jvm
            .methods
            .iter()
            .find(|m| jvm.utf8(m.descriptor_index) == Some("()Ljava/lang/String;"))
            .expect("el metodo declarado");
        assert!(real.access_flags & 0x0400 != 0, "sigue ACC_ABSTRACT");
    }

    // ---- atributo Signature (§4.7.9) ----

    fn sig_string(jvm: &ClassFile, attrs: &[crate::jvm::parser::AttributeInfo]) -> Option<String> {
        let a = attrs.iter().find(|a| jvm.utf8(a.name_index) == Some("Signature"))?;
        let idx = crate::jvm::parser::attributes::signature::index(&a.info)?;
        jvm.utf8(idx).map(str::to_string)
    }
    fn class_sig(jvm: &ClassFile) -> Option<String> {
        sig_string(jvm, &jvm.attributes)
    }
    fn method_sig(jvm: &ClassFile, name: &str) -> Option<String> {
        let m = jvm.methods.iter().find(|m| jvm.utf8(m.name_index) == Some(name))?;
        sig_string(jvm, &m.attributes)
    }
    fn field_sig(jvm: &ClassFile, name: &str) -> Option<String> {
        let f = jvm.fields.iter().find(|f| jvm.utf8(f.name_index) == Some(name))?;
        sig_string(jvm, &f.attributes)
    }

    #[test]
    fn a_generic_class_gets_a_class_signature() {
        let box_ = compiled_class("class Box<T> {}", "Box");
        assert_eq!(class_sig(&box_).as_deref(), Some("<T:Ljava/lang/Object;>Ljava/lang/Object;"));
    }

    #[test]
    fn a_string_switch_expression_in_return_position_runs() {
        // `return switch (s) { case "a" -> … }` sobre `String`: el lowering de la switch-expr genera un
        // switch-**sentencia** que **además** hay que bajar a dos int-switches (hashCode+equals). Antes
        // llegaba crudo al emisor (que no soporta un switch no-`int`). Lo destapó el diferencial.
        let src = "public class C { public static int run() { return sw(\"a\") * 100 + sw(\"b\") * 10 + sw(\"z\"); } \
                   static int sw(String s) { return switch (s) { case \"a\" -> 1; case \"b\" -> 2; default -> 9; }; } }";
        assert_eq!(run_int(src, "C", "run", vec![]), 129); // 1*100 + 2*10 + 9
    }

    #[test]
    fn a_return_string_switch_matches_javac_slots_and_stack() {
        // `return switch (s) { case "a" -> 1; … }`: se baja a los dos int-switches (hashCode+equals),
        // pero la switch-**expresión** de cola deja el valor **en la pila** —sin temporal de
        // resultado— igual que javac. Byte a byte eso fija los slots: `s` es el parámetro (slot 1),
        // y los sintéticos `$s`/`$i` toman los **inmediatos** 2 y 3 (no hay un slot de resultado que
        // se cuele en el 2). El método arranca `aload_1; astore_2; iconst_m1; istore_3` y **termina**
        // dejando el valor del brazo en la pila para el `ireturn` (`… iconst_0; ireturn`), sin
        // `istore`/`iload` de un temporal.
        let src = "public class M { public int sw(String s) { \
                   return switch (s) { case \"a\" -> 1; case \"b\" -> 2; default -> 0; }; } }";
        let (code, _) = code_of(src, "M", "sw");
        // Prólogo: $s → slot 2 (`astore_2`), $i → slot 3 (`istore_3`) — pegados al parámetro.
        assert_eq!(
            &code[0..4],
            &[0x2b, 0x4d, 0x02, 0x3e],
            "aload_1; astore_2; iconst_m1; istore_3 (slots 2 y 3 para $s/$i)"
        );
        // Epílogo: el brazo `default` (iconst_0) cae al `ireturn` con el valor en la pila.
        assert_eq!(
            code[code.len() - 2..],
            [0x03, 0xac],
            "iconst_0; ireturn: el valor viaja por la pila, sin temporal de resultado"
        );
        // Y pasa el verificador estricto (el `StackMapTable` del doble switch es válido).
        verify_all(src, "M");
        // La semántica se conserva: "a"→1, "b"→2, cualquier otra → default 0.
        let run = "public class M { public int sw(String s) { \
                   return switch (s) { case \"a\" -> 1; case \"b\" -> 2; default -> 0; }; } \
                   public static int run() { M m = new M(); return m.sw(\"a\") * 100 + m.sw(\"b\") * 10 + m.sw(\"z\"); } }";
        assert_eq!(run_int(run, "M", "run", vec![]), 120);
    }

    #[test]
    fn array_clone_returns_an_independent_fresh_copy() {
        // `array.clone()` (§10.7): el emisor lo compila a `invokevirtual clone` + `checkcast`, y la VM
        // lo intrinseca a una copia fresca —mismo contenido, independiente del original—. Es lo que
        // usa el `values()` de un enum; lo destapó el diferencial de emisión.
        let src = "public class C { public static int run() { \
                   int[] a = {1, 2, 3}; int[] b = (int[]) a.clone(); b[0] = 99; \
                   return a[0] * 100 + b[0] + b[1] + b[2]; } }";
        // a[0] sigue 1 (100), b = {99,2,3} (104) → 204.
        assert_eq!(run_int(src, "C", "run", vec![]), 204);
    }

    #[test]
    fn an_enum_gets_an_enum_of_self_class_signature() {
        // Un `enum` extiende implícitamente `Enum<Self>` (§8.9): su `Signature` de clase lo refleja
        // (`Ljava/lang/Enum<LColor;>;`), aunque el `extends` no esté escrito. Lo destapó el diferencial
        // de emisión contra javac.
        let e = compiled_class("enum Color { RED, GREEN }", "Color");
        assert_eq!(class_sig(&e).as_deref(), Some("Ljava/lang/Enum<LColor;>;"));
    }

    // ---- fidelidad byte-exacta del codegen de `enum` (corpus `Enums.java`) ----

    /// Todos los `Methodref` de la clase resueltos a `(owner, nombre, descriptor)`.
    fn method_refs(jvm: &ClassFile) -> Vec<(String, String, String)> {
        use crate::jvm::parser::ConstantPoolEntry as CP;
        let mut out = Vec::new();
        for e in &jvm.constant_pool {
            let CP::MethodRef { class_index, name_and_type_index } = e else { continue };
            let Some(owner) = jvm.class_name(*class_index) else { continue };
            if let Some(CP::NameAndType { name_index, descriptor_index }) =
                jvm.constant_pool.get((*name_and_type_index - 1) as usize)
            {
                if let (Some(n), Some(d)) = (jvm.utf8(*name_index), jvm.utf8(*descriptor_index)) {
                    out.push((owner.to_string(), n.to_string(), d.to_string()));
                }
            }
        }
        out
    }

    /// ¿Alguna entrada `Utf8` del pool contiene `needle`?
    fn pool_mentions(jvm: &ClassFile, needle: &str) -> bool {
        use crate::jvm::parser::ConstantPoolEntry as CP;
        jvm.constant_pool.iter().any(|e| matches!(e, CP::Utf8(s) if s.contains(needle)))
    }

    #[test]
    fn an_enum_valueof_is_self_contained() {
        // **Finding #250.** Este test afirmaba lo contrario: que `valueOf` **delegara** en
        // `Enum.valueOf(Class, String)` "byte a byte como javac". Esa delegación es exactamente la
        // regresión: `java.lang.Enum` de KajiLibrary **no declara** ese método —y su ausencia es
        // deliberada, porque el real va por reflexión sobre el `$VALUES` de otra clase— así que
        // delegar dejó sin compilar a **70 de 941** fuentes de la biblioteca, toda la que declarara
        // un `enum`.
        //
        // El criterio correcto es el inverso: el `valueOf` sintetizado tiene que ser
        // **autocontenido**, comparando nombres literales acá adentro. `values()` sí se pudo alinear
        // con javac (`$VALUES.clone()`); la diferencia es que ahí el opcode existe y acá la
        // dependencia sería la reflexión.
        let jvm = compiled_class("enum E { A, B }", "E");
        let refs = method_refs(&jvm);
        assert!(
            !refs.iter().any(|(o, n, _)| o == "java/lang/Enum" && n == "valueOf"),
            "`valueOf` no debe delegar en `Enum.valueOf` (finding #250): {refs:?}",
        );
        assert!(
            refs.iter().any(|(o, n, _)| o == "java/lang/String" && n == "equals"),
            "`valueOf` compara los nombres con `String.equals`: {refs:?}",
        );
        assert!(
            pool_mentions(&jvm, "IllegalArgumentException"),
            "un nombre desconocido tira `IllegalArgumentException` (§8.9.3)",
        );
    }

    #[test]
    fn an_enum_factors_the_values_array_into_a_synthetic_values_method() {
        // javac factoriza la construcción de `$VALUES` en un método sintético `private static E[]
        // $values()` que el `<clinit>` invoca con `invokestatic`.
        let jvm = compiled_class("enum E { A, B }", "E");
        assert!(
            has_method(&jvm, "$values", "()[LE;", super::ACC_PRIVATE | super::ACC_STATIC | ACC_SYNTHETIC),
            "`$values` debe ser `private static synthetic ()[LE;`",
        );
        let refs = method_refs(&jvm);
        assert!(
            refs.iter().any(|(o, n, d)| o == "E" && n == "$values" && d == "()[LE;"),
            "el `<clinit>` debe invocar `$values`: {refs:?}",
        );
    }

    #[test]
    fn an_enum_virtual_call_on_this_targets_the_enum_class_not_the_declaring_class() {
        // §5.4.3.3: el owner del `Methodref` de una invocación virtual es el **tipo estático del
        // receptor**. `ordinal()` heredado de `java.lang.Enum`, llamado sin receptor, apunta a la propia
        // clase (`E`), no a `java/lang/Enum` — igual que javac.
        let jvm = compiled_class("enum E { A, B; public int code() { return ordinal() + 1; } }", "E");
        let refs = method_refs(&jvm);
        let ordinal: Vec<_> = refs.iter().filter(|(_, n, _)| n == "ordinal").collect();
        assert!(!ordinal.is_empty(), "hay una invocación a `ordinal`: {refs:?}");
        assert!(
            ordinal.iter().all(|(o, _, _)| o == "E"),
            "el owner de `ordinal()` es la clase enum `E`, no `java/lang/Enum`: {ordinal:?}",
        );
    }

    #[test]
    fn an_enum_constructor_gets_a_void_signature_that_elides_the_synthetic_params() {
        // §8.9.2: el ctor de un `enum` lleva siempre `Signature: ()V` — su descriptor arranca con los
        // dos parámetros sintéticos `(String, int)`, pero la firma declarada los elide.
        let jvm = compiled_class("enum E { A, B }", "E");
        assert_eq!(
            method_sig(&jvm, "<init>").as_deref(),
            Some("()V"),
            "el ctor del enum lleva `Signature: ()V`",
        );
    }

    #[test]
    fn a_generic_superclass_is_in_the_class_signature() {
        let src = "class Base<T> {} class Node<T> extends Base<T> {}";
        let node = compiled_class(src, "Node");
        assert_eq!(class_sig(&node).as_deref(), Some("<T:Ljava/lang/Object;>LBase<TT;>;"));
    }

    #[test]
    fn a_bounded_type_parameter_uses_its_bound() {
        let box_ = compiled_class("class Box<T extends Number> {}", "Box");
        assert_eq!(class_sig(&box_).as_deref(), Some("<T:Ljava/lang/Number;>Ljava/lang/Object;"));
    }

    #[test]
    fn an_interface_bound_leaves_the_class_bound_empty() {
        // Una cota de interfaz va tras un `:` extra (la cota de clase vacía).
        let box_ = compiled_class("class Box<T extends Comparable<T>> {}", "Box");
        assert_eq!(
            class_sig(&box_).as_deref(),
            Some("<T::Ljava/lang/Comparable<TT;>;>Ljava/lang/Object;"),
        );
    }

    #[test]
    fn a_generic_method_gets_a_method_signature() {
        let c = compiled_class("class C { <T> T id(T x) { return x; } }", "C");
        assert_eq!(method_sig(&c, "id").as_deref(), Some("<T:Ljava/lang/Object;>(TT;)TT;"));
    }

    #[test]
    fn a_type_variable_field_gets_a_field_signature() {
        let box_ = compiled_class("class Box<T> { T val; }", "Box");
        assert_eq!(field_sig(&box_, "val").as_deref(), Some("TT;"));
    }

    #[test]
    fn a_parameterized_field_gets_a_field_signature() {
        let c = compiled_class("class Box<T> {} class C { Box<String> b; }", "C");
        assert_eq!(field_sig(&c, "b").as_deref(), Some("LBox<Ljava/lang/String;>;"));
    }

    #[test]
    fn a_wildcard_field_signature_uses_plus_for_extends() {
        let c = compiled_class("class Box<T> {} class C { Box<? extends Number> b; }", "C");
        assert_eq!(field_sig(&c, "b").as_deref(), Some("LBox<+Ljava/lang/Number;>;"));
    }

    #[test]
    fn a_non_generic_element_gets_no_signature() {
        let c = compiled_class("class C { int f() { return 0; } }", "C");
        assert_eq!(class_sig(&c), None, "clase sin genéricos: sin Signature");
        assert_eq!(method_sig(&c, "f"), None, "método sin genéricos: sin Signature");
    }

    // ---- módulos (§7.7 / §4.7.25) ----

    #[test]
    fn emits_a_module_info_class_with_the_module_attribute() {
        use crate::jvm::parser::ConstantPoolEntry as CP;
        let src = "open module com.example.foo { \
                     requires transitive com.example.bar; \
                     exports com.example.foo.api; \
                     uses com.example.spi.Service; \
                     provides com.example.spi.Service with com.example.foo.Impl; }";
        let jvm = compiled_class(src, "module-info");
        assert!(jvm.access_flags & 0x8000 != 0, "ACC_MODULE");
        assert!(
            jvm.attributes.iter().any(|a| jvm.utf8(a.name_index) == Some("Module")),
            "atributo Module presente",
        );
        let modules: Vec<String> = jvm
            .constant_pool
            .iter()
            .filter_map(|e| match e {
                CP::Module { name_index } => jvm.utf8(*name_index).map(str::to_string),
                _ => None,
            })
            .collect();
        assert!(modules.contains(&"com.example.foo".to_string()), "el módulo propio");
        assert!(modules.contains(&"com.example.bar".to_string()), "el requires");
        assert!(modules.contains(&"java.base".to_string()), "`java.base` mandated implícito");
        let pkgs: Vec<String> = jvm
            .constant_pool
            .iter()
            .filter_map(|e| match e {
                CP::Package { name_index } => jvm.utf8(*name_index).map(str::to_string),
                _ => None,
            })
            .collect();
        assert!(pkgs.contains(&"com/example/foo/api".to_string()), "el paquete exportado en forma interna");
    }

    #[test]
    fn java_base_is_not_duplicated_when_required_explicitly() {
        use crate::jvm::parser::ConstantPoolEntry as CP;
        let jvm = compiled_class("module m { requires java.base; }", "module-info");
        let count = jvm
            .constant_pool
            .iter()
            .filter(|e| matches!(e, CP::Module { name_index } if jvm.utf8(*name_index) == Some("java.base")))
            .count();
        assert_eq!(count, 1, "`java.base` no se duplica");
    }

    #[test]
    fn a_covariant_return_synthesizes_a_bridge() {
        // `B.get():String` sobre `A.get():Object` → puente `Object get()` marcado `ACC_BRIDGE`
        // `ACC_SYNTHETIC`, junto al `get` real `()Ljava/lang/String;`.
        let src = "class A { Object get() { return null; } } \
                   class B extends A { String get() { return null; } }";
        let b = compiled_class(src, "B");
        assert!(
            has_method(&b, "get", "()Ljava/lang/Object;", ACC_BRIDGE | ACC_SYNTHETIC),
            "falta el puente `Object get()`",
        );
        assert!(has_method(&b, "get", "()Ljava/lang/String;", 0), "falta el `get` real");
    }

    #[test]
    fn a_generic_method_infers_its_type_var_through_the_target_with_a_lambda_arg() {
        // §18.5.2: `make(() -> "x")` sobre `<U> Box<U> make(Sup<U>)` con *target* `Box<String>`.
        // `U` se fija por el target; el argumento **lambda** —re-atribuido con `Sup<U>` crudo— no debe
        // aportar un `U = U` espurio que choque con `U = String` (era el falso "restricciones
        // incompatibles"). Y la lambda tiene que **bajar**: su tipo funcional se instancia a
        // `Sup<String>` con la `U` ya inferida, si no el emisor no puede resolverla.
        // Que compile ya ejercita los dos arreglos: la inferencia resuelve `U = String` (sin el falso
        // conflicto) y el emisor baja la lambda con `Sup<String>` (sin el "no puede resolver `U`"). Si
        // cualquiera fallara, `compile_units_cp` devolvería `Err`.
        let src = "interface Sup<U> { U get(); } class Box<U> { } \
                   class T { static <U> Box<U> make(Sup<U> s) { return null; } \
                             static Box<String> go() { return make(() -> \"x\"); } }";
        crate::javac::compile_units_cp(&[src], &[]).expect("compila con la inferencia §18");
    }

    #[test]
    fn a_public_class_forwards_public_methods_of_a_package_private_super() {
        // #268: `Sub3` (**pública**) hereda `len()` de `Base3` (**package-private**) sin
        // sobrescribirlo. Un llamador de otro paquete no puede nombrar `Base3`, así que la llamada
        // resuelta fallaría el chequeo de acceso (JVMS 5.4.4). javac sintetiza en `Sub3` un
        // **forwarder** `len()` (`ACC_BRIDGE ACC_SYNTHETIC`) que reenvía con `invokespecial
        // Base3.len`. El override covariante `self()` va por el puente de #233, no por acá.
        let src = "abstract class Base3 { public int len() { return 1; } \
                   public Base3 self() { return this; } } \
                   public final class Sub3 extends Base3 { public Sub3 self() { return this; } }";
        let sub = compiled_class(src, "Sub3");
        assert!(
            has_method(&sub, "len", "()I", ACC_BRIDGE | ACC_SYNTHETIC),
            "falta el forwarder `len()` de #268",
        );
        // `self()` sigue teniendo su override real y su puente covariante (#233), no duplicado acá.
        assert!(has_method(&sub, "self", "()LSub3;", 0), "falta el `self` real");
        assert!(
            has_method(&sub, "self", "()LBase3;", ACC_BRIDGE | ACC_SYNTHETIC),
            "falta el puente covariante de `self`",
        );
        verify_all(src, "Sub3");
    }

    #[test]
    fn a_generic_parameter_override_synthesizes_a_bridge() {
        // `INode.set(Integer)` sobre `Node<Integer>` (`set(T)` → `set(Object)`) → puente
        // `set(Object)`. Verifica de punta a punta.
        let src = "class Node<T> { void set(T v) {} } \
                   class INode extends Node<Integer> { void set(Integer v) {} }";
        let n = compiled_class(src, "INode");
        assert!(
            has_method(&n, "set", "(Ljava/lang/Object;)V", ACC_BRIDGE | ACC_SYNTHETIC),
            "falta el puente `set(Object)`",
        );
        verify_all(src, "INode");
    }

    #[test]
    fn implementing_a_generic_interface_synthesizes_a_bridge() {
        // El caso clásico: `Foo implements Comparable<Foo>` → puente `compareTo(Object)`.
        let src = "class Foo implements Comparable<Foo> { public int compareTo(Foo o) { return 0; } }";
        let foo = compiled_class(src, "Foo");
        assert!(
            has_method(&foo, "compareTo", "(Ljava/lang/Object;)I", ACC_BRIDGE | ACC_SYNTHETIC),
            "falta el puente `compareTo(Object)`",
        );
        verify_all(src, "Foo");
    }

    #[test]
    fn an_interface_default_method_is_emitted_public() {
        // §9.4: un método `default` (o `static`) de interfaz sin `public` explícito es implícitamente
        // **público** en el `.class` — lo destapó el diferencial de emisión contra javac.
        let src = "interface I { default String hi() { return \"x\"; } static int z() { return 0; } }";
        let i = compiled_class(src, "I");
        assert!(
            has_method(&i, "hi", "()Ljava/lang/String;", super::ACC_PUBLIC),
            "el `default` debe llevar ACC_PUBLIC",
        );
        assert!(
            has_method(&i, "z", "()I", super::ACC_PUBLIC),
            "el `static` de interfaz también es implícitamente público",
        );
    }

    #[test]
    fn an_interface_default_override_synthesizes_a_bridge() {
        // B (§9.4.1.3): una interfaz con un `default` que sobrescribe un método genérico borrado de
        // un superinterfaz (`A<String>.get():T` → `get():Object`) sintetiza un puente `default
        // Object get()` marcado `ACC_BRIDGE ACC_SYNTHETIC`, que reenvía con `invokeinterface`.
        let src = "interface A<T> { T get(); } \
                   interface B extends A<String> { default String get() { return \"hi\"; } }";
        let b = compiled_class(src, "B");
        assert!(
            has_method(&b, "get", "()Ljava/lang/Object;", ACC_BRIDGE | ACC_SYNTHETIC),
            "falta el puente `Object get()` en la interfaz",
        );
        assert!(has_method(&b, "get", "()Ljava/lang/String;", 0), "falta el `get` real");
        // El puente de interfaz reenvía con `invokeinterface`, no `invokevirtual`.
        let bridge = b
            .methods
            .iter()
            .find(|m| {
                b.utf8(m.name_index) == Some("get")
                    && b.utf8(m.descriptor_index) == Some("()Ljava/lang/Object;")
            })
            .expect("el puente existe");
        let code = &b.member_code(bridge).expect("el puente tiene Code").code;
        assert!(code.contains(&super::INVOKEINTERFACE), "el puente de interfaz usa invokeinterface");
        assert!(!code.contains(&super::INVOKEVIRTUAL), "no debe usar invokevirtual");
        verify_all(src, "B");
    }

    #[test]
    fn an_interface_bridge_is_inherited_and_dispatches_from_an_implementor() {
        // Correctitud de punta a punta: `A<String> a = new C()` con `C implements B`; `a.get()` se ve
        // como `A.get():Object` → `invokeinterface get()Object`, que resuelve al puente `default` que
        // C **hereda** de B y reenvía a `B.get():String`. Sin el puente de interfaz no resolvería.
        let src = "interface A<T> { T get(); } \
                   interface B extends A<String> { default String get() { return \"hi\"; } } \
                   class C implements B {} \
                   class M { static int f() { A a = new C(); Object o = a.get(); return o == null ? 0 : 1; } }";
        assert_eq!(run_int(src, "M", "f", vec![]), 1, "el puente de interfaz heredado debe reenviar");
    }

    #[test]
    fn a_plain_inherited_default_method_dispatches_via_the_interface() {
        // Consecuencia del arreglo de vtable de B: un `default` **corriente** (sin genéricos) heredado
        // por una clase que no lo sobrescribe resuelve por `invokeinterface` sobre la clase receptora.
        let src = "interface Greeter { default int greet() { return 42; } } \
                   class Impl implements Greeter {} \
                   class M { static int f() { Greeter g = new Impl(); return g.greet(); } }";
        assert_eq!(run_int(src, "M", "f", vec![]), 42, "el default heredado debe despachar");
    }

    #[test]
    fn a_bridge_dispatches_through_the_supertype_reference() {
        // Prueba de **despacho**: `a.pick()` (visto como `A.pick():Object`) sobre un `B` corre el
        // puente, que reenvía a `B.pick():String` — sin puente daría `null` (0); con puente, `1`.
        let src = "class A { Object pick() { return null; } } \
                   class B extends A { String pick() { return \"hi\"; } } \
                   class M { static int f() { A a = new B(); Object o = a.pick(); return o == null ? 0 : 1; } }";
        assert_eq!(run_int(src, "M", "f", vec![]), 1, "el puente debería reenviar a B.pick()");
    }

    #[test]
    fn no_bridge_when_the_erasure_already_matches() {
        // `B.get():Object` (mismo retorno) **no** genera puente: solo está el `get` real.
        let src = "class A { Object get() { return null; } } \
                   class B extends A { Object get() { return null; } }";
        let b = compiled_class(src, "B");
        let gets = b
            .methods
            .iter()
            .filter(|m| b.utf8(m.name_index) == Some("get"))
            .count();
        assert_eq!(gets, 1, "no debería haber puente redundante");
    }

    #[test]
    fn a_sealed_type_emits_the_permitted_subclasses_attribute() {
        // El pipeline completo compila una jerarquía sellada y el `.class` de `Shape` lleva su
        // `PermittedSubclasses` (§4.7.31), re-parseado con la JVM propia.
        let src = "sealed interface Shape permits Circle, Square {} \
                   final class Circle implements Shape {} final class Square implements Shape {}";
        let (_, bytes) = compile_all(src)
            .into_iter()
            .find(|(n, _)| n.ends_with("Shape"))
            .expect("la clase Shape");
        let n = COUNTER.fetch_add(1, Ordering::Relaxed);
        let dir = std::env::temp_dir().join(format!("javac_sealed_{}_{n}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        let path = dir.join("Shape.class");
        std::fs::write(&path, &bytes).unwrap();
        let jvm = ClassFile::from_path(path.to_str().unwrap()).expect("el .class debe parsear");
        let _ = std::fs::remove_dir_all(&dir);
        assert!(
            jvm.attributes.iter().any(|a| jvm.utf8(a.name_index) == Some("PermittedSubclasses")),
            "`Shape` debe llevar el atributo PermittedSubclasses",
        );
    }

    // ---- APT fase 4: el mecanismo del Filer ----

    /// De punta a punta: un programa corre en la VM, usa el `Filer` para fabricar `Foo`, escribe su
    /// fuente en el `StringWriter` que el Filer le entrega, y el lado Rust **recupera** ese texto.
    /// Ejercita Filer → StringWriter → nativo → cola → lectura reentrante (el `toString()` del
    /// writer invocado por el VM sobre el **mismo heap** que corrió el programa). Es el hito mínimo
    /// del Filer; el re-enganche al round loop (fase 2) queda aparte.
    #[test]
    fn the_filer_hands_generated_source_back_to_rust() {
        use crate::jvm::interpreter::natives::{drain_filer, install_filer};

        let n = COUNTER.fetch_add(1, Ordering::Relaxed);
        let dir = std::env::temp_dir().join(format!("javac_filer_{}_{n}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();

        // Compila una unidad (contra KajiLibrary + lo ya emitido en `dir`) y escribe cada clase bajo
        // su ruta de paquete, para que el classpath de runtime la resuelva por nombre interno.
        let kaji = PathBuf::from("KajiLibrary");
        let compile_into = |source: &str| {
            let classes = crate::javac::compile_cp(source, &[kaji.clone(), dir.clone()])
                .expect("la fuente del Filer debe compilar");
            for (internal, bytes) in classes {
                let path = dir.join(format!("{internal}.class"));
                std::fs::create_dir_all(path.parent().unwrap()).unwrap();
                std::fs::write(path, bytes).unwrap();
            }
        };

        // Los archivos reales de KajiLibrary (el entregable), en orden de dependencia:
        // KajiFiler crea un KajiSourceFile, así que este último se compila primero.
        compile_into(
            &std::fs::read_to_string(kaji.join("javax/annotation/processing/KajiSourceFile.java")).unwrap(),
        );
        compile_into(
            &std::fs::read_to_string(kaji.join("javax/annotation/processing/KajiFiler.java")).unwrap(),
        );
        // El manejador de prueba: usa el Filer como lo haría un procesador de anotaciones.
        // `w` se maneja como el `StringWriter` concreto que el Filer entrega: el VM propio todavía
        // no le da slot de vtable a un método **abstracto** heredado (p. ej. `Writer.close()`), así
        // que resolver `close()` contra el tipo estático `Writer` fallaría. Ortogonal al Filer.
        compile_into(
            "package javax.annotation.processing; \
             import javax.tools.JavaFileObject; import java.io.StringWriter; \
             public class FilerDriver { \
                 public static int drive() { \
                     Filer f = new KajiFiler(); \
                     JavaFileObject jfo = f.createSourceFile(\"Foo\"); \
                     StringWriter w = (StringWriter) jfo.openWriter(); \
                     w.write(\"class Foo {}\"); \
                     w.close(); \
                     return 0; \
                 } \
             }",
        );

        // Boot = KajiLibrary (StringWriter, Filer, ...) + boot/; app = las clases recién compiladas.
        let mut ms = MetaspaceService::new(vec![kaji.clone(), PathBuf::from("boot")], vec![dir.clone()]);
        let driver = ms
            .resolve_method("javax/annotation/processing/FilerDriver", "drive", "()I")
            .expect("FilerDriver.drive resuelto");
        let max_locals = ms.max_locals(driver);
        let mut jvm = JVM::new(ms, Frame::for_call(driver, max_locals, Vec::new(), &[]));

        // Armar el Filer, correr el programa, drenar lo registrado y recuperar el texto de cada uno.
        install_filer();
        for _ in 0..100_000 {
            if let Step::Return(_) = jvm.exec().step() {
                break;
            }
        }
        // Una colección **explícita** antes de drenar. El canal del Filer guarda offsets crudos y
        // sobrevive a los frames que crearon los writers, así que si no fuera raíz del GC el
        // writer se movería y el texto volvería vacío. Sin este `gc_minor` el test solo fallaba
        // cuando el programa alcanzaba a llenar Eden por su cuenta — una casualidad, no una prueba.
        jvm.exec().gc_minor();

        let pending = drain_filer();
        let recovered: Vec<(String, String)> = pending
            .into_iter()
            .map(|(name, writer_ref)| (name, jvm.read_generated_text(writer_ref as usize)))
            .collect();
        let _ = std::fs::remove_dir_all(&dir);

        assert_eq!(recovered, vec![("Foo".to_string(), "class Foo {}".to_string())]);
    }
}
