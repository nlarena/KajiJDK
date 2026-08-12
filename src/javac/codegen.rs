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
//!   los estados que llegan ahí. Se emite siempre `full_frame`: legal en cualquier posición y evita
//!   las formas comprimidas. El emisor lleva la pila de operandos **tipada**, no solo su altura —
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
    AssignOp, BinOp, Binding, BootstrapArg, Block, CaseLabel, CatchClause, Expr, ExprKind, Member,
    MethodDecl, Modifier, Pos, PrimType, Stmt, StmtKind, SwitchBody, SwitchCase, Type, UnOp,
};
use super::Error;
use std::collections::{BTreeMap, HashMap, HashSet};

use super::class_writer::{
    BootstrapMethod, ClassFile, ConstantPool, ExceptionEntry, FieldInfo, MethodInfo,
};
use super::symbol::{RType, Resolved, ScopeId, SymbolId, SymbolKind, SymbolTable};

// Flags de acceso (JVMS Table 4.1-B / 4.6-A).
const ACC_PUBLIC: u16 = 0x0001;
const ACC_PRIVATE: u16 = 0x0002;
const ACC_PROTECTED: u16 = 0x0004;
const ACC_STATIC: u16 = 0x0008;
const ACC_FINAL: u16 = 0x0010;
const ACC_SUPER: u16 = 0x0020;
const ACC_ABSTRACT: u16 = 0x0400;

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
    for class in &unit.types {
        gen_type(class, base, table, &mut out, &mut errors);
    }
    // Si algo no se pudo emitir, **no** se devuelve un `.class` a medias.
    match errors.into_iter().next() {
        Some(first) => Err(first),
        None => Ok(out),
    }
}

/// Emite `class` y, **recursivamente**, sus tipos anidados (`Member::Type`) — cada uno como su propio
/// `.class`. El nombre cualificado se arma con `.` (así lo indexa la tabla); el *binary name* con `$`
/// lo pone la propia clase.
fn gen_type(
    class: &super::ast::ClassDecl,
    enclosing: &str,
    table: &SymbolTable,
    out: &mut Vec<(String, Vec<u8>)>,
    errors: &mut Vec<Error>,
) {
    let fqn = if enclosing.is_empty() { class.name.clone() } else { format!("{enclosing}.{}", class.name) };
    if let Some(cid) = table.class(&fqn) {
        let bytes = gen_class(class, cid, table, errors);
        out.push((internal_name(table, cid), bytes));
    }
    for member in &class.members {
        if let Member::Type(nested) = member {
            gen_type(nested, &fqn, table, out, errors);
        }
    }
}

/// Compila **una** clase a los bytes de su `.class`.
fn gen_class(
    class: &super::ast::ClassDecl,
    cid: SymbolId,
    table: &SymbolTable,
    errors: &mut Vec<Error>,
) -> Vec<u8> {
    let scope = member_scope(table, cid);

    let mut cf = ClassFile::new();
    let this_internal = internal_name(table, cid);
    cf.this_class = cf.pool.class(&this_internal);
    let super_internal = super_internal(table, cid, class, scope);
    cf.super_class = cf.pool.class(&super_internal);
    cf.access_flags = class_flags(&class.modifiers) | ACC_SUPER;
    cf.source_file = Some(cf.pool.utf8(&format!("{}.java", class.name)));

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
                let mi = gen_method(
                    &mut cf.pool,
                    table,
                    scope,
                    m,
                    &this_internal,
                    &super_internal,
                    &mut bootstraps,
                    errors,
                );
                cf.methods.push(mi);
            }
            // Los campos **declarados**: sin esta sección el `.class` referencia un `getfield` a un
            // campo que no existe, y la carga falla al buscar su offset.
            Member::Field(f) => {
                let name_index = cf.pool.utf8(&f.name);
                let descriptor_index = cf.pool.utf8(&type_desc(table, scope, &f.ty));
                cf.fields.push(FieldInfo {
                    access_flags: class_flags(&f.modifiers),
                    name_index,
                    descriptor_index,
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
            annotations: Vec::new(),
            pos: Pos::default(),
            modifiers: vec![Modifier::Static],
            type_params: Vec::new(),
            return_type: Type::Void,
            name: "<clinit>".to_string(),
            params: Vec::new(),
            throws: Vec::new(),
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
            &mut bootstraps,
            errors,
        );
        cf.methods.push(mi);
    }

    // Sin constructor explícito, se sintetiza el por defecto: `super()` + `return`.
    if !has_ctor {
        let ctor = default_ctor(&mut cf.pool, &super_internal);
        cf.methods.push(ctor);
    }

    cf.bootstrap_methods = bootstraps;
    cf.to_bytes()
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
    table.resolve_type(scope, name).or_else(|| table.external(name))
}

fn class_flags(mods: &[Modifier]) -> u16 {
    mods.iter().fold(0, |f, m| f | modifier_flag(*m))
}

fn modifier_flag(m: Modifier) -> u16 {
    match m {
        Modifier::Public => ACC_PUBLIC,
        Modifier::Private => ACC_PRIVATE,
        Modifier::Protected => ACC_PROTECTED,
        Modifier::Static => ACC_STATIC,
        Modifier::Final => ACC_FINAL,
        Modifier::Abstract => ACC_ABSTRACT,
        _ => 0,
    }
}

// ---- descriptores (JVMS §4.3) ----

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
        RType::Class(id) | RType::TypeVar(id) => format!("L{};", internal_name(table, *id)),
        RType::Parameterized { base, .. } => format!("L{};", internal_name(table, *base)),
        RType::Unresolved => "Ljava/lang/Object;".to_string(),
    }
}

fn method_descriptor(table: &SymbolTable, scope: ScopeId, m: &MethodDecl) -> String {
    let params: String = m.params.iter().map(|p| type_desc(table, scope, &p.ty)).collect();
    let ret = if m.is_constructor { "V".to_string() } else { type_desc(table, scope, &m.return_type) };
    format!("({params}){ret}")
}

// ---- generación de un método ----

fn gen_method(
    pool: &mut ConstantPool,
    table: &SymbolTable,
    scope: ScopeId,
    m: &MethodDecl,
    this_internal: &str,
    super_internal: &str,
    bootstraps: &mut Vec<BootstrapMethod>,
    errors: &mut Vec<Error>,
) -> MethodInfo {
    let name = if m.is_constructor { "<init>" } else { &m.name };
    let name_index = pool.utf8(name);
    let descriptor_index = pool.utf8(&method_descriptor(table, scope, m));

    let mut e = Emitter::new(
        pool,
        table,
        scope,
        this_internal.to_string(),
        super_internal.to_string(),
        bootstraps,
        errors,
    );
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

    // Un constructor arranca invocando a **otro** constructor (§8.8.7): el de su superclase o, con
    // `this(...)`, uno de los suyos. Sin ese `invokespecial` el `this` queda **sin inicializar** y
    // el verificador rechaza cualquier `putfield` sobre él.
    //
    // Si el cuerpo ya arranca con un `super(...)`/`this(...)` **explícito**, ese es el que va: el
    // implícito se omite, porque inicializar dos veces el mismo objeto es ilegal (§8.8.7.1).
    if m.is_constructor && !m.body.as_ref().is_some_and(explicit_ctor_call) {
        let super_init = e.pool.methodref(super_internal, "<init>", "()V");
        e.load_this(); // todavía `UninitThis`
        e.op(INVOKESPECIAL);
        e.u16(super_init);
        e.pop(1);
        e.init_this(); // ya inicializado
    }

    if let Some(body) = &m.body {
        for s in &body.0 {
            e.stmt(s);
        }
    }
    // Un `void`/constructor puede omitir el `return` final; lo agregamos.
    if m.is_constructor || matches!(m.return_type, Type::Void) {
        e.op(RETURN);
    }
    e.patch(); // resolver los saltos ahora que se conocen todos los offsets
    let stack_map = e.stack_map();

    MethodInfo {
        access_flags: class_flags(&m.modifiers),
        name_index,
        descriptor_index,
        max_stack: e.max_stack as u16,
        max_locals: e.max_locals,
        code: e.bytes,
        stack_map,
        exceptions: e.exceptions,
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
fn default_ctor(pool: &mut ConstantPool, super_internal: &str) -> MethodInfo {
    let name_index = pool.utf8("<init>");
    let descriptor_index = pool.utf8("()V");
    let super_init = pool.methodref(super_internal, "<init>", "()V");
    let code = vec![ALOAD_0, INVOKESPECIAL, (super_init >> 8) as u8, super_init as u8, RETURN];
    MethodInfo {
        access_flags: ACC_PUBLIC,
        name_index,
        descriptor_index,
        max_stack: 1,
        max_locals: 1,
        code,
        stack_map: None, // sin saltos: no lleva tabla
        exceptions: Vec::new(),
    }
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
const DUP2: u8 = 0x5c;
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
        RType::Void | RType::Unresolved => VType::Top,
        RType::Array(_) => VType::Object(rtype_desc(table, rt)),
        RType::Class(id) | RType::TypeVar(id) => VType::Object(internal_name(table, *id)),
        RType::Parameterized { base, .. } => VType::Object(internal_name(table, *base)),
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
    /// Si el código puede **caer** hasta el punto actual (falso tras un `goto`/`return`).
    reachable: bool,
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
    ) -> Self {
        Emitter {
            pool,
            table,
            scope,
            this_class,
            super_class,
            errors,
            bootstraps,
            bytes: Vec::new(),
            stack: Vec::new(),
            max_stack: 0,
            max_locals: 0,
            labels: Vec::new(),
            fixups: Vec::new(),
            wide_fixups: Vec::new(),
            blocks: Vec::new(),
            pending_label: None,
            locals_t: Vec::new(),
            pending: HashMap::new(),
            frames: BTreeMap::new(),
            targets: HashSet::new(),
            exceptions: Vec::new(),
            reachable: true,
        }
    }

    // ---- etiquetas y saltos ----

    fn new_label(&mut self) -> Label {
        self.labels.push(None);
        self.labels.len() - 1
    }

    /// Anota el tipo de un local (y deja en `Top` la segunda mitad de un `long`/`double`).
    fn set_local(&mut self, slot: u16, t: VType) {
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

    /// Serializa el `StackMapTable` (JVMS §4.7.4). Se usa **siempre `full_frame`** (tag 255): es
    /// legal en cualquier posición y evita el zoo de formas comprimidas (`same`/`chop`/`append`), a
    /// costa de unos bytes más. El `offset_delta` del primer frame es su offset; los siguientes van
    /// **menos uno** respecto del anterior (así un delta de 0 sigue siendo un avance real).
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
        let mut prev: Option<usize> = None;
        for (off, locals, stack) in frames {
            let delta = match prev {
                None => off,
                Some(p) => off - p - 1,
            };
            prev = Some(off);
            out.push(255); // full_frame
            out.extend_from_slice(&(delta as u16).to_be_bytes());
            out.extend_from_slice(&(locals.len() as u16).to_be_bytes());
            for t in &locals {
                self.write_vtype(&mut out, t);
            }
            out.extend_from_slice(&(stack.len() as u16).to_be_bytes());
            for t in &stack {
                self.write_vtype(&mut out, t);
            }
        }
        Some(out)
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
        for &(operand, at, l) in &self.fixups {
            let target = self.labels[l].expect("etiqueta sin fijar");
            let delta = target as i32 - at as i32;
            let bytes = (delta as i16).to_be_bytes();
            self.bytes[operand] = bytes[0];
            self.bytes[operand + 1] = bytes[1];
        }
        for &(operand, base, l) in &self.wide_fixups {
            let target = self.labels[l].expect("etiqueta sin fijar");
            let delta = (target as i64 - base as i64) as i32;
            self.bytes[operand..operand + 4].copy_from_slice(&delta.to_be_bytes());
        }
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
        self.errors.push(Error {
            message: format!("el generador de bytecode todavía no soporta {what}"),
            line: pos.line,
            col: pos.col,
        });
    }

    fn ty_of(&self, e: &Expr) -> RType {
        e.ty.clone().unwrap_or(RType::Prim(PrimType::Int))
    }

    // ---- sentencias ----

    fn stmt(&mut self, s: &Stmt) {
        // La etiqueta que dejó un `Labeled` es de **esta** sentencia; se consume al entrar para que
        // no se filtre a una anidada.
        let lbl = self.pending_label.take();
        match &s.kind {
            StmtKind::Block(b) => b.0.iter().for_each(|s| self.stmt(s)),
            StmtKind::LocalVar { init, .. } => {
                if let (Some(e), Some(local)) = (init, &s.local) {
                    self.expr(e);
                    let cat = category(&local.ty);
                    let vt = vtype_of(self.table, &local.ty);
                    self.set_local(local.slot, vt);
                    self.store(cat, local.slot);
                }
            }
            StmtKind::Return(e) => {
                match e {
                    Some(e) => {
                        let cat = category(&self.ty_of(e));
                        self.expr(e);
                        self.op(IRETURN + cat);
                        self.pop(1);
                    }
                    None => self.op(RETURN),
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
                self.blocks.push(Breakable { label: lbl, brk: end, cont: Some(top) });
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
                self.blocks.push(Breakable { label: lbl, brk: end, cont: Some(cont) });
                self.stmt(body);
                self.blocks.pop();
                self.bind(cont); // un `continue` va a reevaluar la condición
                self.branch_if(cond, top, true);
                self.bind(end);
            }
            StmtKind::For { init, cond, update, body } => {
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
                self.blocks.push(Breakable { label: lbl, brk: end, cont: Some(cont) });
                self.stmt(body);
                self.blocks.pop();
                self.bind(cont);
                for u in update {
                    self.discard(u);
                }
                self.jump(GOTO, top);
                self.reachable = false;
                self.bind(end);
            }
            StmtKind::Break(label) => match self.break_target(label.as_deref()) {
                Some(t) => {
                    self.jump(GOTO, t);
                    self.reachable = false;
                }
                None => self.unsupported(s.pos, "un `break` sin destino (¿lo dejó pasar el flujo?)"),
            },
            StmtKind::Continue(label) => match self.continue_target(label.as_deref()) {
                Some(t) => {
                    self.jump(GOTO, t);
                    self.reachable = false;
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
            // Estas tendrían que haber desaparecido en el desugar: si llegan acá es un bug de esa pasada.
            StmtKind::ForEach { .. } | StmtKind::Assert { .. } | StmtKind::Yield(_) => {
                self.unsupported(s.pos, "una construcción que el desugar debía haber bajado")
            }
        }
    }

    /// El destino de un `break`: la sentencia más interna de la que se puede salir, o la que lleve
    /// la etiqueta pedida.
    fn break_target(&self, label: Option<&str>) -> Option<Label> {
        match label {
            None => self.blocks.last().map(|b| b.brk),
            Some(l) => self.blocks.iter().rev().find(|b| b.label.as_deref() == Some(l)).map(|b| b.brk),
        }
    }

    /// El destino de un `continue`: el **bucle** más interno (un `switch` interpuesto no cuenta), o
    /// el que lleve la etiqueta pedida.
    fn continue_target(&self, label: Option<&str>) -> Option<Label> {
        self.blocks
            .iter()
            .rev()
            .find(|b| b.cont.is_some() && label.is_none_or(|l| b.label.as_deref() == Some(l)))
            .and_then(|b| b.cont)
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
    fn switch_stmt(&mut self, s: &Stmt, selector: &Expr, cases: &[SwitchCase], lbl: Option<String>) {
        if category(&self.ty_of(selector)) != 0 {
            self.unsupported(s.pos, "un `switch` cuyo selector no es `int` (lo baja el desugar)");
            return;
        }
        // (valor, índice del grupo) por cada etiqueta, más cuál es el `default`.
        let mut keys: Vec<(i32, usize)> = Vec::new();
        let mut default_case: Option<usize> = None;
        for (i, c) in cases.iter().enumerate() {
            if let Some(g) = &c.guard {
                self.unsupported(g.pos, "una guarda `when` (el desugar debía haberla bajado)");
                return;
            }
            if c.is_default {
                default_case = Some(i);
            }
            for l in &c.labels {
                let CaseLabel::Constant(e) = l else {
                    self.unsupported(s.pos, "una etiqueta `case` que el desugar debía haber bajado");
                    return;
                };
                match const_int(e) {
                    Some(v) => keys.push((v, i)),
                    None => {
                        self.unsupported(e.pos, "un `case` que no es una constante entera");
                        return;
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

        // Los grupos, en orden de fuente — de ahí sale el *fall-through* de la forma de dos puntos.
        self.blocks.push(Breakable { label: lbl, brk: end, cont: None });
        for (i, c) in cases.iter().enumerate() {
            self.bind(arms[i]);
            match &c.body {
                // La flecha **no** cae al grupo siguiente (§14.11.2): sale sola.
                SwitchBody::Arrow(b) => {
                    self.stmt(b);
                    if self.reachable {
                        self.jump(GOTO, end);
                        self.reachable = false;
                    }
                }
                SwitchBody::Colon(ss) => ss.iter().for_each(|s| self.stmt(s)),
            }
        }
        self.blocks.pop();
        self.bind(end);
    }

    /// Relleno hasta el próximo múltiplo de 4 **desde el arranque del método** (JVMS §6,
    /// `tableswitch`): como `bytes` empieza en el pc 0, su largo *es* el pc.
    fn pad4(&mut self) {
        while self.bytes.len() % 4 != 0 {
            self.op(0);
        }
    }

    /// `synchronized (lock) { … }` (§14.19, JVMS §3.14): `monitorenter` al entrar y `monitorexit`
    /// en **las dos** salidas — la normal y la excepcional. Esta última es un handler *catch-all*
    /// que suelta el monitor y re-lanza; sin él, una excepción dejaría el monitor tomado para
    /// siempre y cualquier otro hilo que lo pidiera quedaría colgado.
    ///
    /// El handler **no** guarda la excepción en un local: la deja en la pila, empuja el lock encima
    /// (`monitorexit` consume solo ese) y hace `athrow` sobre lo que quedó.
    ///
    /// El monitor viene copiado a un local por el desugar, así que releerlo no reevalúa nada.
    fn sync_stmt(&mut self, s: &Stmt, lock: &Expr, body: &Block) {
        let Some(Binding::Local { slot }) = lock.binding else {
            self.unsupported(s.pos, "un `synchronized` cuyo monitor no se copió a un local");
            return;
        };
        let entry_locals = self.locals_t.clone();
        self.load(4, slot);
        self.op(MONITORENTER);
        self.pop(1);

        let start = self.bytes.len();
        for s in &body.0 {
            self.stmt(s);
        }
        let end = self.bytes.len();
        let after = self.new_label();
        if self.reachable {
            self.load(4, slot);
            self.op(MONITOREXIT);
            self.pop(1);
            self.jump(GOTO, after);
            self.reachable = false;
        }

        let handler = self.bytes.len();
        self.handler_frame(handler, &entry_locals, "java/lang/Throwable");
        self.stack = vec![VType::Object("java/lang/Throwable".to_string())];
        self.max_stack = self.max_stack.max(2); // la excepción + el lock
        self.locals_t = entry_locals;
        self.load(4, slot);
        self.op(MONITOREXIT);
        self.pop(1);
        self.op(ATHROW);
        self.pop(1);
        self.reachable = false;
        self.exceptions.push(ExceptionEntry {
            start_pc: start as u16,
            end_pc: end as u16,
            handler_pc: handler as u16,
            catch_type: 0, // cualquier Throwable
        });
        self.bind(after);
    }

    /// `try { … } catch (E e) { … } [finally { … }]`.
    ///
    /// El cuerpo protegido queda en `[start, end)`; cada `catch` instala una entrada en la tabla de
    /// excepciones. Al entrar a un *handler* la JVM **limpia la pila y deja ahí la excepción**, así
    /// que su frame lleva `stack = [E]` y arranca guardándola en el slot de su variable.
    ///
    /// El `finally` se emite **duplicado** (§14.20.2): una copia en la salida normal y otra en un
    /// handler *catch-all* (`catch_type = 0`) que lo corre y re-lanza — la v69 ya no admite `jsr`.
    fn try_stmt(&mut self, body: &Block, catches: &[CatchClause], finally: &Option<Block>) {
        let entry_locals = self.locals_t.clone();
        let start = self.bytes.len();
        for s in &body.0 {
            self.stmt(s);
        }
        let end = self.bytes.len();
        let after = self.new_label();
        if self.reachable {
            if let Some(f) = finally {
                for s in &f.0 {
                    self.stmt(s);
                }
            }
            self.jump(GOTO, after);
            self.reachable = false;
        }

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
            if let Some(slot) = c.slot {
                self.set_local(slot, VType::Object(exc.clone()));
                self.store(4, slot); // astore: la excepción es una referencia
            } else {
                self.op(POP);
                self.pop(1);
            }
            for s in &c.body.0 {
                self.stmt(s);
            }
            if self.reachable {
                if let Some(f) = finally {
                    for s in &f.0 {
                        self.stmt(s);
                    }
                }
                self.jump(GOTO, after);
                self.reachable = false;
            }
            let ct = self.pool.class(&exc);
            self.exceptions.push(ExceptionEntry {
                start_pc: start as u16,
                end_pc: end as u16,
                handler_pc: handler as u16,
                catch_type: ct,
            });
        }

        // El `finally` también corre si algo se escapa: handler catch-all que lo ejecuta y re-lanza.
        if let Some(f) = finally {
            let handler = self.bytes.len();
            self.handler_frame(handler, &entry_locals, "java/lang/Throwable");
            self.stack = vec![VType::Object("java/lang/Throwable".to_string())];
            self.max_stack = self.max_stack.max(1);
            self.locals_t = entry_locals.clone();
            for s in &f.0 {
                self.stmt(s);
            }
            self.op(ATHROW);
            self.pop(1);
            self.reachable = false;
            self.exceptions.push(ExceptionEntry {
                start_pc: start as u16,
                end_pc: end as u16,
                handler_pc: handler as u16,
                catch_type: 0, // cualquier Throwable
            });
        }
        self.bind(after);
    }

    /// Registra el frame de un *handler*: se alcanza solo por excepción, así que no hay salto que lo
    /// apunte y hay que forzarlo.
    fn handler_frame(&mut self, at: usize, locals: &[VType], exc: &str) {
        let l = self.new_label();
        self.labels[l] = Some(at);
        self.targets.insert(l);
        self.frames.insert(at, (locals.to_vec(), vec![VType::Object(exc.to_string())]));
    }

    /// Evalúa una expresión **descartando** su valor (el `update` de un `for`, o una
    /// sentencia-expresión): una asignación no deja nada, el resto se saca con `pop`.
    fn discard(&mut self, e: &Expr) {
        if let ExprKind::Assign { op: AssignOp::Assign, target, value } = &e.kind {
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
                for a in args {
                    self.expr(a);
                }
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
                    self.invoke(e, args, is_static);
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
                self.op(INSTANCEOF);
                self.u16(idx);
                self.pop(1);
                self.push(VType::Int); // el 0/1 del resultado
            }
            ExprKind::Super => self.unsupported(e.pos, "`super`"),
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
            ExprKind::Switch { .. } => self.unsupported(e.pos, "una switch-expresión embebida"),
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
            // desugar deja a propósito para el emisor — y que todavía no resuelve.
            ExprKind::Assign { .. } => {
                self.unsupported(e.pos, "una asignación compuesta sobre un destino con efectos")
            }
        }
    }

    fn invoke(&mut self, call: &Expr, args: &[Expr], is_static: bool) {
        let Some(Binding::Method(mid)) = call.binding else { return };
        let Some(owner) = self.table.symbol(mid).owner else { return };
        let class_internal = internal_name(self.table, owner);
        let is_ctor = matches!(self.table.symbol(mid).kind, SymbolKind::Method { is_constructor: true, .. });
        let mname = if is_ctor { "<init>".to_string() } else { self.table.symbol(mid).name.clone() };
        let (desc, ret) = match self.table.resolved(mid) {
            Some(Resolved::Method { params, ret, .. }) => {
                let ps: String = params.iter().map(|t| rtype_desc(self.table, t)).collect();
                (format!("({ps}){}", rtype_desc(self.table, ret)), ret.clone())
            }
            _ => ("()V".to_string(), RType::Void),
        };
        let mref = self.pool.methodref(&class_internal, &mname, &desc);

        // `invokestatic` sin receptor; `invokespecial` para un constructor; si no, despacho virtual.
        self.op(if is_static {
            INVOKESTATIC
        } else if is_ctor {
            INVOKESPECIAL
        } else {
            INVOKEVIRTUAL
        });
        self.u16(mref);
        // Consume los argumentos (y el receptor, si lo hubo) y deja el retorno si no es `void`.
        self.pop(args.len() + usize::from(!is_static));
        if !matches!(ret, RType::Void) {
            let t = vtype_of(self.table, &ret);
            self.push(t);
        }
    }

    // ---- condiciones como saltos ----

    /// Emite un salto a `target` **cuando `cond` valga `when`**. Es la forma canónica de compilar
    /// condiciones: `&&`/`||` se vuelven saltos (así se cortocircuitan de verdad) en vez de calcular
    /// un booleano, y negar la condición es simplemente invertir el sentido del salto.
    fn branch_if(&mut self, cond: &Expr, target: Label, when: bool) {
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
                let idx = cmp_index(*op, when).unwrap();
                let cat = category(&self.ty_of(lhs)).max(category(&self.ty_of(rhs)));
                self.expr(lhs);
                self.convert(category(&self.ty_of(lhs)), cat);
                self.expr(rhs);
                self.convert(category(&self.ty_of(rhs)), cat);
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
                        self.op(LCMP + (cat - 1) * 2);
                        self.pop(2);
                        self.push(VType::Int);
                        self.pop(1);
                        self.jump(IFEQ + idx, target);
                    }
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

    /// Materializa una condición como el **valor** `0`/`1` (para `boolean b = x > 0;`).
    fn bool_value(&mut self, cond: &Expr) {
        let t = self.new_label();
        let end = self.new_label();
        self.branch_if(cond, t, true);
        self.push_int(0);
        // Un destino de salto con la pila **no vacía**: el valor booleano ya está empujado.
        self.jump(GOTO, end);
        self.reachable = false;
        self.bind(t);
        self.pop(1); // los dos caminos empujan un solo valor, no dos
        self.push_int(1);
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

    /// Emite `target = value`. `leave` dice si debe **dejar el valor** en la pila: una asignación
    /// usada como *expresión* sí (`a = b = 1`), como *sentencia* no.
    fn assign(&mut self, target: &Expr, value: &Expr, leave: bool) {
        // `a[i] = v` — no pasa por un binding: consume (arrayref, índice, valor).
        if let ExprKind::Index { array, index } = &target.kind {
            if leave {
                self.unsupported(target.pos, "usar el valor de una asignación a un array");
                return;
            }
            self.expr(array);
            self.expr(index);
            self.expr(value);
            let kind = array_kind(&self.ty_of(target));
            self.op(IASTORE + kind);
            self.pop(3); // arrayref, índice y valor
            return;
        }
        match target.binding {
            Some(Binding::Local { slot }) => {
                self.expr(value);
                let ty = self.ty_of(target);
                let cat = category(&ty);
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
        for a in args {
            self.expr(a);
        }
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
                self.op(CHECKCAST);
                self.u16(cidx);
                self.pop(1);
                self.push(VType::Object(cls));
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
    use super::{BIPUSH, INVOKEDYNAMIC, INVOKESPECIAL, LOOKUPSWITCH, MONITOREXIT, TABLESWITCH};
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
    fn a_case_label_that_is_not_a_constant_is_rejected() {
        // Una `static final int` como etiqueta necesita **plegado de constantes**, que todavía no
        // hay: la barrera avisa en vez de emitir un `case` con basura.
        let msg = rejected(
            "public class M { static final int K = 1; \
             public static int f(int n) { switch (n) { case K: return 1; default: return 0; } } }",
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
        assert_eq!(handlers, 1, "un handler catch-all que suelte el monitor");
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
}
