//! El **escritor de `.class`**: el back-end que arma el **constant pool** (deduplicando entradas)
//! y serializa la estructura completa del `class_file` a bytes big-endian, según la JVMS §4.
//!
//! Es la pieza que mantiene el compilador **desacoplado**: tiene su **propio** builder de constant
//! pool, en vez de reusar las estructuras de *lectura* del parser de la JVM (`src/jvm`). El único
//! contrato con el mundo es el formato `.class`.

use std::collections::HashMap;

/// La versión mayor del class file — **JDK 25 = 69** (JVMS §4.1, `major_version`).
const MAJOR_VERSION: u16 = 69;

// Tags del constant pool (JVMS Table 4.4-B).
const TAG_UTF8: u8 = 1;
const TAG_INTEGER: u8 = 3;
const TAG_FLOAT: u8 = 4;
const TAG_LONG: u8 = 5;
const TAG_DOUBLE: u8 = 6;
const TAG_CLASS: u8 = 7;
const TAG_STRING: u8 = 8;
const TAG_FIELDREF: u8 = 9;
const TAG_METHODREF: u8 = 10;
const TAG_INTERFACE_METHODREF: u8 = 11;
const TAG_NAME_AND_TYPE: u8 = 12;
const TAG_METHOD_HANDLE: u8 = 15;
const TAG_METHOD_TYPE: u8 = 16;
const TAG_INVOKE_DYNAMIC: u8 = 18;
const TAG_MODULE: u8 = 19;
const TAG_PACKAGE: u8 = 20;

/// Una entrada del constant pool, ya con sus índices resueltos (los que apuntan a otras entradas).
enum Const {
    Utf8(String),
    Integer(i32),
    Float(f32),
    Long(i64),
    Double(f64),
    Class(u16),
    /// `String`: apunta al `Utf8` con sus caracteres.
    Str(u16),
    Fieldref(u16, u16),
    Methodref(u16, u16),
    /// `InterfaceMethodref` (§4.4.2): un método declarado en una **interfaz**; lo referencia
    /// `invokeinterface`.
    InterfaceMethodref(u16, u16),
    NameAndType(u16, u16),
    /// `MethodHandle` (§4.4.8): un *reference kind* (1..=9) y el índice de lo referido (un
    /// `Methodref`/`Fieldref`). Lo pide el `invokedynamic` de las lambdas.
    MethodHandle(u8, u16),
    /// `MethodType` (§4.4.9): apunta al `Utf8` de un descriptor de método.
    MethodType(u16),
    /// `InvokeDynamic` (§4.4.10): el índice del *bootstrap method* (en el atributo
    /// `BootstrapMethods`) y un `NameAndType` (el nombre del SAM + el descriptor invocado).
    InvokeDynamic(u16, u16),
    /// `Module` (§4.4.11): apunta al `Utf8` del nombre del módulo (con puntos). Lo usa el atributo
    /// `Module` de un `module-info`.
    Module(u16),
    /// `Package` (§4.4.12): apunta al `Utf8` del nombre de un paquete (en forma interna, con `/`).
    Package(u16),
    /// El hueco que sigue a un `Long`/`Double`: **ocupan dos entradas** del pool y la segunda es
    /// inutilizable (JVMS §4.4.5, "a somewhat unfortunate choice"). No se serializa.
    Unusable,
}

/// El **constant pool** en construcción: la lista de entradas (1-indexada) y las cachés de
/// *deduplicación*, para no repetir el mismo Utf8/Class/etc.
#[derive(Default)]
pub struct ConstantPool {
    entries: Vec<Const>, // la entrada de índice `i` (1-based) vive en `entries[i-1]`
    utf8: HashMap<String, u16>,
    class: HashMap<String, u16>,
    name_and_type: HashMap<(String, String), u16>,
    methodref: HashMap<(String, String, String), u16>,
    fieldref: HashMap<(String, String, String), u16>,
    integer: HashMap<i32, u16>,
    string: HashMap<String, u16>,
    long: HashMap<i64, u16>,
    // Los flotantes se cachean por su **patrón de bits**: `f32`/`f64` no son `Hash`/`Eq`, y además
    // así `0.0` y `-0.0` (bits distintos) no se confunden.
    float: HashMap<u32, u16>,
    double: HashMap<u64, u16>,
}

impl ConstantPool {
    pub fn new() -> Self {
        Self::default()
    }

    fn add(&mut self, c: Const) -> u16 {
        self.entries.push(c);
        self.entries.len() as u16 // el índice es 1-based = la nueva longitud
    }

    /// `constant_pool_count`: uno **más** que el número de entradas (JVMS §4.1).
    fn count(&self) -> u16 {
        self.entries.len() as u16 + 1
    }

    pub fn utf8(&mut self, s: &str) -> u16 {
        if let Some(&i) = self.utf8.get(s) {
            return i;
        }
        let i = self.add(Const::Utf8(s.to_string()));
        self.utf8.insert(s.to_string(), i);
        i
    }

    /// Una entrada `Class` a partir de un **nombre interno** (`java/lang/Object`, `Add`).
    pub fn class(&mut self, internal: &str) -> u16 {
        if let Some(&i) = self.class.get(internal) {
            return i;
        }
        let name = self.utf8(internal);
        let i = self.add(Const::Class(name));
        self.class.insert(internal.to_string(), i);
        i
    }

    /// Una entrada `Module` (§4.4.11) a partir del nombre del módulo (con **puntos**, tal cual).
    pub fn module(&mut self, name: &str) -> u16 {
        let n = self.utf8(name);
        self.add(Const::Module(n))
    }

    /// Una entrada `Package` (§4.4.12) a partir de un nombre de paquete; se guarda en **forma
    /// interna** (los `.` pasan a `/`).
    pub fn package(&mut self, dotted: &str) -> u16 {
        let n = self.utf8(&dotted.replace('.', "/"));
        self.add(Const::Package(n))
    }

    pub fn name_and_type(&mut self, name: &str, descriptor: &str) -> u16 {
        let key = (name.to_string(), descriptor.to_string());
        if let Some(&i) = self.name_and_type.get(&key) {
            return i;
        }
        let n = self.utf8(name);
        let d = self.utf8(descriptor);
        let i = self.add(Const::NameAndType(n, d));
        self.name_and_type.insert(key, i);
        i
    }

    pub fn methodref(&mut self, class: &str, name: &str, descriptor: &str) -> u16 {
        let key = (class.to_string(), name.to_string(), descriptor.to_string());
        if let Some(&i) = self.methodref.get(&key) {
            return i;
        }
        let c = self.class(class);
        let nat = self.name_and_type(name, descriptor);
        let i = self.add(Const::Methodref(c, nat));
        self.methodref.insert(key, i);
        i
    }

    pub fn fieldref(&mut self, class: &str, name: &str, descriptor: &str) -> u16 {
        let key = (class.to_string(), name.to_string(), descriptor.to_string());
        if let Some(&i) = self.fieldref.get(&key) {
            return i;
        }
        let c = self.class(class);
        let nat = self.name_and_type(name, descriptor);
        let i = self.add(Const::Fieldref(c, nat));
        self.fieldref.insert(key, i);
        i
    }

    /// Un `InterfaceMethodref` (§4.4.2): el método de una **interfaz**, para `invokeinterface`. No se
    /// deduplica (son pocos).
    pub fn interface_methodref(&mut self, class: &str, name: &str, descriptor: &str) -> u16 {
        let c = self.class(class);
        let nat = self.name_and_type(name, descriptor);
        self.add(Const::InterfaceMethodref(c, nat))
    }

    /// Un `MethodHandle` (§4.4.8). Según su *reference kind* (§5.4.3.5) referencia un `Fieldref`
    /// (kinds 1–4: `getField`/`getStatic`/`putField`/`putStatic`, p. ej. los *getters* de un `record`)
    /// o un `Methodref` (5–8: los `invoke*` y `newInvokeSpecial`). No se deduplica (son pocos).
    pub fn method_handle(&mut self, kind: u8, class: &str, name: &str, descriptor: &str) -> u16 {
        let reference = if (1..=4).contains(&kind) {
            self.fieldref(class, name, descriptor)
        } else if kind == 9 {
            self.interface_methodref(class, name, descriptor) // REF_invokeInterface
        } else {
            self.methodref(class, name, descriptor)
        };
        self.add(Const::MethodHandle(kind, reference))
    }

    /// Un `MethodType` (§4.4.9) a partir de un descriptor de método.
    pub fn method_type(&mut self, descriptor: &str) -> u16 {
        let d = self.utf8(descriptor);
        self.add(Const::MethodType(d))
    }

    /// Un `InvokeDynamic` (§4.4.10): el índice del *bootstrap method* (en `BootstrapMethods`) y el
    /// nombre/descriptor del *call site*.
    pub fn invoke_dynamic(&mut self, bootstrap_index: u16, name: &str, descriptor: &str) -> u16 {
        let nat = self.name_and_type(name, descriptor);
        self.add(Const::InvokeDynamic(bootstrap_index, nat))
    }

    pub fn integer(&mut self, v: i32) -> u16 {
        if let Some(&i) = self.integer.get(&v) {
            return i;
        }
        let i = self.add(Const::Integer(v));
        self.integer.insert(v, i);
        i
    }

    /// Un literal `String`: la entrada `String` más el `Utf8` con sus caracteres.
    pub fn string(&mut self, s: &str) -> u16 {
        if let Some(&i) = self.string.get(s) {
            return i;
        }
        let text = self.utf8(s);
        let i = self.add(Const::Str(text));
        self.string.insert(s.to_string(), i);
        i
    }

    pub fn float(&mut self, v: f32) -> u16 {
        let key = v.to_bits();
        if let Some(&i) = self.float.get(&key) {
            return i;
        }
        let i = self.add(Const::Float(v));
        self.float.insert(key, i);
        i
    }

    /// Un `long`: ocupa **dos** entradas (ver [`Const::Unusable`]).
    pub fn long(&mut self, v: i64) -> u16 {
        if let Some(&i) = self.long.get(&v) {
            return i;
        }
        let i = self.add_wide(Const::Long(v));
        self.long.insert(v, i);
        i
    }

    /// Un `double`: ocupa **dos** entradas (ver [`Const::Unusable`]).
    pub fn double(&mut self, v: f64) -> u16 {
        let key = v.to_bits();
        if let Some(&i) = self.double.get(&key) {
            return i;
        }
        let i = self.add_wide(Const::Double(v));
        self.double.insert(key, i);
        i
    }

    /// Agrega una entrada **ancha** (`Long`/`Double`) y reserva el hueco siguiente, para que los
    /// índices posteriores queden corridos como manda la JVMS.
    fn add_wide(&mut self, c: Const) -> u16 {
        let i = self.add(c);
        self.entries.push(Const::Unusable);
        i
    }

    fn write(&self, out: &mut Vec<u8>) {
        out.extend_from_slice(&self.count().to_be_bytes());
        for entry in &self.entries {
            match entry {
                Const::Utf8(s) => {
                    out.push(TAG_UTF8);
                    out.extend_from_slice(&(s.len() as u16).to_be_bytes());
                    out.extend_from_slice(s.as_bytes());
                }
                Const::Integer(v) => {
                    out.push(TAG_INTEGER);
                    out.extend_from_slice(&v.to_be_bytes());
                }
                Const::Float(v) => {
                    out.push(TAG_FLOAT);
                    out.extend_from_slice(&v.to_bits().to_be_bytes());
                }
                Const::Long(v) => {
                    out.push(TAG_LONG);
                    out.extend_from_slice(&v.to_be_bytes());
                }
                Const::Double(v) => {
                    out.push(TAG_DOUBLE);
                    out.extend_from_slice(&v.to_bits().to_be_bytes());
                }
                Const::Str(t) => {
                    out.push(TAG_STRING);
                    out.extend_from_slice(&t.to_be_bytes());
                }
                // El hueco de un `Long`/`Double`: no se serializa, solo corre los índices.
                Const::Unusable => {}
                Const::Class(n) => {
                    out.push(TAG_CLASS);
                    out.extend_from_slice(&n.to_be_bytes());
                }
                Const::Fieldref(c, nt) => {
                    out.push(TAG_FIELDREF);
                    out.extend_from_slice(&c.to_be_bytes());
                    out.extend_from_slice(&nt.to_be_bytes());
                }
                Const::Methodref(c, nt) => {
                    out.push(TAG_METHODREF);
                    out.extend_from_slice(&c.to_be_bytes());
                    out.extend_from_slice(&nt.to_be_bytes());
                }
                Const::InterfaceMethodref(c, nt) => {
                    out.push(TAG_INTERFACE_METHODREF);
                    out.extend_from_slice(&c.to_be_bytes());
                    out.extend_from_slice(&nt.to_be_bytes());
                }
                Const::NameAndType(n, d) => {
                    out.push(TAG_NAME_AND_TYPE);
                    out.extend_from_slice(&n.to_be_bytes());
                    out.extend_from_slice(&d.to_be_bytes());
                }
                Const::MethodHandle(kind, r) => {
                    out.push(TAG_METHOD_HANDLE);
                    out.push(*kind);
                    out.extend_from_slice(&r.to_be_bytes());
                }
                Const::MethodType(d) => {
                    out.push(TAG_METHOD_TYPE);
                    out.extend_from_slice(&d.to_be_bytes());
                }
                Const::InvokeDynamic(b, nt) => {
                    out.push(TAG_INVOKE_DYNAMIC);
                    out.extend_from_slice(&b.to_be_bytes());
                    out.extend_from_slice(&nt.to_be_bytes());
                }
                Const::Module(n) => {
                    out.push(TAG_MODULE);
                    out.extend_from_slice(&n.to_be_bytes());
                }
                Const::Package(n) => {
                    out.push(TAG_PACKAGE);
                    out.extend_from_slice(&n.to_be_bytes());
                }
            }
        }
    }
}

/// Un campo listo para serializar (JVMS §4.5).
pub struct FieldInfo {
    pub access_flags: u16,
    pub name_index: u16,
    pub descriptor_index: u16,
    /// El **cuerpo** ya serializado del atributo `RuntimeVisibleAnnotations` (§4.7.16):
    /// `num_annotations` + los `annotation`. `None` si el campo no lleva anotaciones *runtime*.
    pub annotations: Option<Vec<u8>>,
    /// Índice `Utf8` de la firma genérica (atributo `Signature`, §4.7.9), o `None` si el tipo del
    /// campo no usa genéricos (el descriptor alcanza).
    pub signature: Option<u16>,
    /// Índice al pool (`Integer`/`Long`/`Float`/`Double`/`String`) del atributo `ConstantValue`
    /// (§4.7.2): el valor de un `static final` con inicializador de **expresión constante** (§15.29),
    /// que la JVM asigna al preparar la clase. `None` si el campo no es una constante de compilación.
    pub constant_value: Option<u16>,
    /// Cuerpo ya serializado del atributo `RuntimeVisibleTypeAnnotations` (§4.7.20) del campo —las
    /// anotaciones sobre el **uso del tipo** del campo (target `0x13`)—, o `None` si no lleva.
    pub type_annotations: Option<Vec<u8>>,
}

/// El cuerpo de un método listo para serializar: sus flags, nombre/descriptor (índices al pool) y
/// el atributo `Code`.
pub struct MethodInfo {
    pub access_flags: u16,
    pub name_index: u16,
    pub descriptor_index: u16,
    pub max_stack: u16,
    pub max_locals: u16,
    pub code: Vec<u8>,
    /// El **cuerpo** ya serializado del atributo `StackMapTable` (`number_of_entries` + los frames),
    /// o `None` si el método no tiene saltos y no lo necesita.
    pub stack_map: Option<Vec<u8>>,
    /// La tabla de excepciones del `Code` (JVMS §4.7.3): qué rango protege cada handler.
    pub exceptions: Vec<ExceptionEntry>,
    /// El cuerpo ya serializado del atributo `RuntimeVisibleAnnotations` (§4.7.16) del método, o
    /// `None` si no lleva anotaciones *runtime*.
    pub annotations: Option<Vec<u8>>,
    /// Índice `Utf8` de la firma genérica (atributo `Signature`, §4.7.9), o `None` si el método no
    /// usa genéricos (ni parámetros de tipo, ni tipos parametrizados/variables en su firma).
    pub signature: Option<u16>,
    /// Los parámetros formales para el atributo `MethodParameters` (§4.7.24): sus nombres + flags.
    /// Vacío si el método no tiene parámetros (o es sintético sin nombres que retener).
    pub parameters: Vec<ParamInfo>,
    /// Los índices `Class` de la cláusula `throws` para el atributo `Exceptions` (§4.7.5) —las
    /// excepciones **chequeadas** que el método declara lanzar—. Vacío si no hay `throws`. Ojo: es
    /// distinto de `exceptions` (arriba), que es la tabla de *handlers* del `Code`.
    pub thrown_exceptions: Vec<u16>,
    /// Pares `(start_pc, line_number)` para el atributo `LineNumberTable` (§4.7.12) —anidado en el
    /// `Code`—: qué offset de bytecode arranca cada línea del fuente. Vacío si no hay `Code` o no se
    /// pudo mapear (un método sintético). Da los números de línea de los stack traces y del debugger.
    pub line_numbers: Vec<(u16, u16)>,
    /// Entradas del `LocalVariableTable` (§4.7.13) —anidado en el `Code`—: por cada variable local
    /// viva, `(start_pc, length, name_index, descriptor_index, slot)`, con `name`/`descriptor` ya
    /// interned como `Utf8`. Da los **nombres** de `this`, parámetros y locales al debugger. Vacío si
    /// no hay `Code`.
    pub local_vars: Vec<(u16, u16, u16, u16, u16)>,
    /// Cuerpo ya serializado del atributo `RuntimeVisibleTypeAnnotations` (§4.7.20) del método —las
    /// anotaciones sobre usos de tipo en su firma: parámetros de tipo (target `0x01`), retorno
    /// (`0x14`), parámetros formales (`0x16`), `throws` (`0x17`), cotas (`0x12`)—, o `None`.
    pub type_annotations: Option<Vec<u8>>,
    /// Cuerpo ya serializado del `RuntimeVisibleTypeAnnotations` que va **dentro del `Code`** (§4.7.20):
    /// las anotaciones sobre usos de tipo en posiciones de bytecode del cuerpo —cast (`0x47`),
    /// `instanceof` (`0x43`), `new` (`0x44`)—, con el offset del opcode como `target_info`, o `None`.
    pub code_type_annotations: Option<Vec<u8>>,
    /// Cuerpo ya serializado del atributo `RuntimeVisibleParameterAnnotations` (§4.7.18): `num_parameters`
    /// (u1) + por cada parámetro su `num_annotations` (u2) y sus `annotation`. Son las anotaciones de
    /// **declaración** sobre los parámetros formales (`void m(@Deprecated String s)`), o `None`.
    pub parameter_annotations: Option<Vec<u8>>,
    /// El `element_value` ya serializado del atributo `AnnotationDefault` (§4.7.22): el valor **por
    /// defecto** de un elemento de `@interface` (`String value() default "x";`), o `None`.
    pub annotation_default: Option<Vec<u8>>,
}

/// Un componente del atributo `Record` (§4.7.30): su nombre + descriptor, y una `Signature`
/// opcional si su tipo usa genéricos.
pub struct RecordComponent {
    pub name: u16,
    pub descriptor: u16,
    pub signature: Option<u16>,
}

/// Una entrada del atributo `InnerClasses` (§4.7.6): la relación anidada de una clase. `outer` es 0
/// para las **locales/anónimas** (no son miembros de otra clase); `name` es 0 para las **anónimas**
/// (no tienen nombre). Todos los índices son al pool (`Class`/`Utf8`), 0 = ausente.
pub struct InnerClassEntry {
    pub inner: u16,
    pub outer: u16,
    pub name: u16,
    pub flags: u16,
}

/// Un parámetro formal para el atributo `MethodParameters` (§4.7.24): su nombre (`Utf8`, o 0) y sus
/// flags (`ACC_FINAL`/`ACC_SYNTHETIC`/`ACC_MANDATED`).
pub struct ParamInfo {
    pub name: u16,
    pub flags: u16,
}

/// Una entrada de la tabla de excepciones: el rango `[start_pc, end_pc)` protegido, dónde salta y
/// qué atrapa (`catch_type` = 0 significa **cualquier** `Throwable`, o sea un `finally`).
pub struct ExceptionEntry {
    pub start_pc: u16,
    pub end_pc: u16,
    pub handler_pc: u16,
    pub catch_type: u16,
}

/// La clase completa en construcción. El `codegen` la llena y `to_bytes` la serializa.
pub struct ClassFile {
    pub pool: ConstantPool,
    pub access_flags: u16,
    pub this_class: u16,
    pub super_class: u16,
    /// Los índices `Class` de las **super-interfaces** (§4.1): el `implements` de una clase y el
    /// `extends` de una interfaz. Vacío si no hay.
    pub interfaces: Vec<u16>,
    pub fields: Vec<FieldInfo>,
    pub methods: Vec<MethodInfo>,
    /// Índice Utf8 del nombre del fuente (`Add.java`), para el atributo `SourceFile`.
    pub source_file: Option<u16>,
    /// Los *bootstrap methods* del atributo `BootstrapMethods` (§4.7.23) — uno por *call site* de
    /// `invokedynamic`. Vacío si la clase no usa lambdas/refs.
    pub bootstrap_methods: Vec<BootstrapMethod>,
    /// El cuerpo ya serializado del atributo `RuntimeVisibleAnnotations` (§4.7.16) de la clase, o
    /// `None` si la clase no lleva anotaciones *runtime*.
    pub annotations: Option<Vec<u8>>,
    /// Los índices `Class` de los subtipos autorizados de un tipo `sealed`, para el atributo
    /// `PermittedSubclasses` (§4.7.31). Vacío si la clase no es `sealed`.
    pub permitted_subclasses: Vec<u16>,
    /// El **cuerpo** ya serializado del atributo `Module` (§4.7.25) de un `module-info` —desde
    /// `module_name_index`—, o `None` si no es un descriptor de módulo.
    pub module: Option<Vec<u8>>,
    /// Índice `Utf8` de la firma genérica de la **clase** (atributo `Signature`, §4.7.9): sus
    /// parámetros de tipo + super/interfaces genéricos. `None` si no usa genéricos.
    pub signature: Option<u16>,
    /// Las entradas del atributo `InnerClasses` (§4.7.6): la relación de anidamiento que este
    /// `.class` menciona. Vacío si no hay clases anidadas involucradas.
    pub inner_classes: Vec<InnerClassEntry>,
    /// El atributo `EnclosingMethod` (§4.7.7) de una clase **local/anónima**: `(class_index,
    /// method_index)` — la clase envolvente y el `NameAndType` del método (0 si no está en uno).
    /// `None` si la clase no es local/anónima.
    pub enclosing_method: Option<(u16, u16)>,
    /// Los componentes del atributo `Record` (§4.7.30). `Some` (aunque vacío) marca que la clase es
    /// un `record`; `None` si no lo es.
    pub record_components: Option<Vec<RecordComponent>>,
    /// Índice `Class` del **nest host** (§4.7.28): la clase top-level del *nest* al que pertenece una
    /// clase **anidada**. `None` en una top-level (que es su propio host).
    pub nest_host: Option<u16>,
    /// Índices `Class` de los **nest members** (§4.7.29): todos los anidados del *nest* que hostea
    /// una clase top-level. Vacío si no hostea ninguno.
    pub nest_members: Vec<u16>,
    /// Cuerpo ya serializado del atributo `RuntimeVisibleTypeAnnotations` (§4.7.20) de la clase —las
    /// anotaciones sobre usos de tipo en su cabecera: parámetros de tipo (target `0x00`), sus cotas
    /// (`0x11`), y `extends`/`implements` (`0x10`)—, o `None` si no lleva.
    pub type_annotations: Option<Vec<u8>>,
    /// Índice Utf8 de `"Code"` (se necesita en cada método), reservado una sola vez.
    code_attr_name: u16,
    source_attr_name: u16,
    stack_map_attr_name: u16,
    bootstrap_attr_name: u16,
    rva_attr_name: u16,
    permitted_attr_name: u16,
    module_attr_name: u16,
    signature_attr_name: u16,
    inner_classes_attr_name: u16,
    enclosing_method_attr_name: u16,
    record_attr_name: u16,
    nest_host_attr_name: u16,
    nest_members_attr_name: u16,
    method_params_attr_name: u16,
    exceptions_attr_name: u16,
    line_number_attr_name: u16,
    local_var_attr_name: u16,
    constant_value_attr_name: u16,
    rvta_attr_name: u16,
    rvpa_attr_name: u16,
    annotation_default_attr_name: u16,
}

/// Una entrada del atributo `BootstrapMethods` (§4.7.23): el `MethodHandle` del *bootstrap* y sus
/// argumentos estáticos (índices al pool).
pub struct BootstrapMethod {
    pub method_handle: u16,
    pub args: Vec<u16>,
}

impl ClassFile {
    pub fn new() -> Self {
        let mut pool = ConstantPool::new();
        // Los nombres de atributo se internan una vez, al principio.
        let code_attr_name = pool.utf8("Code");
        let source_attr_name = pool.utf8("SourceFile");
        let stack_map_attr_name = pool.utf8("StackMapTable");
        let bootstrap_attr_name = pool.utf8("BootstrapMethods");
        let rva_attr_name = pool.utf8("RuntimeVisibleAnnotations");
        let permitted_attr_name = pool.utf8("PermittedSubclasses");
        let module_attr_name = pool.utf8("Module");
        let signature_attr_name = pool.utf8("Signature");
        let inner_classes_attr_name = pool.utf8("InnerClasses");
        let enclosing_method_attr_name = pool.utf8("EnclosingMethod");
        let record_attr_name = pool.utf8("Record");
        let nest_host_attr_name = pool.utf8("NestHost");
        let nest_members_attr_name = pool.utf8("NestMembers");
        let method_params_attr_name = pool.utf8("MethodParameters");
        let exceptions_attr_name = pool.utf8("Exceptions");
        let line_number_attr_name = pool.utf8("LineNumberTable");
        let local_var_attr_name = pool.utf8("LocalVariableTable");
        let constant_value_attr_name = pool.utf8("ConstantValue");
        let rvta_attr_name = pool.utf8("RuntimeVisibleTypeAnnotations");
        let rvpa_attr_name = pool.utf8("RuntimeVisibleParameterAnnotations");
        let annotation_default_attr_name = pool.utf8("AnnotationDefault");
        ClassFile {
            pool,
            access_flags: 0,
            this_class: 0,
            super_class: 0,
            interfaces: Vec::new(),
            fields: Vec::new(),
            methods: Vec::new(),
            source_file: None,
            bootstrap_methods: Vec::new(),
            annotations: None,
            permitted_subclasses: Vec::new(),
            module: None,
            signature: None,
            inner_classes: Vec::new(),
            enclosing_method: None,
            record_components: None,
            nest_host: None,
            nest_members: Vec::new(),
            type_annotations: None,
            code_attr_name,
            source_attr_name,
            stack_map_attr_name,
            bootstrap_attr_name,
            rva_attr_name,
            permitted_attr_name,
            module_attr_name,
            signature_attr_name,
            inner_classes_attr_name,
            enclosing_method_attr_name,
            record_attr_name,
            nest_host_attr_name,
            nest_members_attr_name,
            method_params_attr_name,
            exceptions_attr_name,
            line_number_attr_name,
            local_var_attr_name,
            constant_value_attr_name,
            rvta_attr_name,
            rvpa_attr_name,
            annotation_default_attr_name,
        }
    }

    /// Emite un atributo `RuntimeVisibleParameterAnnotations` (§4.7.18) dado su **cuerpo** ya
    /// serializado (`num_parameters` + las listas por parámetro): nombre + longitud + cuerpo.
    fn write_rvpa(&self, body: &[u8], out: &mut Vec<u8>) {
        out.extend_from_slice(&self.rvpa_attr_name.to_be_bytes());
        out.extend_from_slice(&(body.len() as u32).to_be_bytes());
        out.extend_from_slice(body);
    }

    /// Emite un atributo `AnnotationDefault` (§4.7.22) dado el `element_value` ya serializado.
    fn write_annotation_default(&self, value: &[u8], out: &mut Vec<u8>) {
        out.extend_from_slice(&self.annotation_default_attr_name.to_be_bytes());
        out.extend_from_slice(&(value.len() as u32).to_be_bytes());
        out.extend_from_slice(value);
    }

    /// Emite un atributo `RuntimeVisibleTypeAnnotations` (§4.7.20) dado su **cuerpo** ya serializado
    /// (`num_annotations` + los `type_annotation`): nombre + longitud + cuerpo.
    fn write_rvta(&self, body: &[u8], out: &mut Vec<u8>) {
        out.extend_from_slice(&self.rvta_attr_name.to_be_bytes());
        out.extend_from_slice(&(body.len() as u32).to_be_bytes());
        out.extend_from_slice(body);
    }

    /// Emite el atributo `RuntimeVisibleAnnotations` (§4.7.16) dado su **cuerpo** ya serializado
    /// (`num_annotations` + los `annotation`): nombre + longitud + cuerpo.
    fn write_rva(&self, body: &[u8], out: &mut Vec<u8>) {
        out.extend_from_slice(&self.rva_attr_name.to_be_bytes());
        out.extend_from_slice(&(body.len() as u32).to_be_bytes());
        out.extend_from_slice(body);
    }

    /// Emite el atributo `Exceptions` (§4.7.5): las clases de la cláusula `throws`. Formato:
    /// `number_of_exceptions (u2)` + `exception_index_table[]` (índices `Class`).
    fn write_exceptions(&self, classes: &[u16], out: &mut Vec<u8>) {
        out.extend_from_slice(&self.exceptions_attr_name.to_be_bytes());
        out.extend_from_slice(&((2 + classes.len() * 2) as u32).to_be_bytes());
        out.extend_from_slice(&(classes.len() as u16).to_be_bytes());
        for &c in classes {
            out.extend_from_slice(&c.to_be_bytes());
        }
    }

    /// Emite el atributo `Signature` (§4.7.9): nombre + longitud (siempre 2) + `signature_index`.
    fn write_signature(&self, idx: u16, out: &mut Vec<u8>) {
        out.extend_from_slice(&self.signature_attr_name.to_be_bytes());
        out.extend_from_slice(&2u32.to_be_bytes());
        out.extend_from_slice(&idx.to_be_bytes());
    }

    /// Emite el atributo `MethodParameters` (§4.7.24). Ojo: `parameters_count` es un **`u1`** (un
    /// byte), seguido de `{ name_index (u2), access_flags (u2) }` por parámetro.
    fn write_method_parameters(&self, params: &[ParamInfo], out: &mut Vec<u8>) {
        out.extend_from_slice(&self.method_params_attr_name.to_be_bytes());
        out.extend_from_slice(&((1 + params.len() * 4) as u32).to_be_bytes());
        out.push(params.len() as u8); // parameters_count: u1
        for p in params {
            out.extend_from_slice(&p.name.to_be_bytes());
            out.extend_from_slice(&p.flags.to_be_bytes());
        }
    }

    /// Serializa la clase a los bytes de un `.class` (JVMS §4.1).
    pub fn to_bytes(&self) -> Vec<u8> {
        let mut out = Vec::new();
        out.extend_from_slice(&0xCAFE_BABEu32.to_be_bytes()); // magic
        out.extend_from_slice(&0u16.to_be_bytes()); // minor_version
        out.extend_from_slice(&MAJOR_VERSION.to_be_bytes()); // major_version = 69
        self.pool.write(&mut out);
        out.extend_from_slice(&self.access_flags.to_be_bytes());
        out.extend_from_slice(&self.this_class.to_be_bytes());
        out.extend_from_slice(&self.super_class.to_be_bytes());
        out.extend_from_slice(&(self.interfaces.len() as u16).to_be_bytes());
        for &i in &self.interfaces {
            out.extend_from_slice(&i.to_be_bytes());
        }
        out.extend_from_slice(&(self.fields.len() as u16).to_be_bytes());
        for f in &self.fields {
            out.extend_from_slice(&f.access_flags.to_be_bytes());
            out.extend_from_slice(&f.name_index.to_be_bytes());
            out.extend_from_slice(&f.descriptor_index.to_be_bytes());
            let fattrs = f.annotations.is_some() as u16
                + f.signature.is_some() as u16
                + f.constant_value.is_some() as u16
                + f.type_annotations.is_some() as u16;
            out.extend_from_slice(&fattrs.to_be_bytes()); // attributes_count
            if let Some(cv) = f.constant_value {
                out.extend_from_slice(&self.constant_value_attr_name.to_be_bytes());
                out.extend_from_slice(&2u32.to_be_bytes()); // attribute_length = 2
                out.extend_from_slice(&cv.to_be_bytes());
            }
            if let Some(body) = &f.annotations {
                self.write_rva(body, &mut out);
            }
            if let Some(sig) = f.signature {
                self.write_signature(sig, &mut out);
            }
            if let Some(body) = &f.type_annotations {
                self.write_rvta(body, &mut out);
            }
        }
        out.extend_from_slice(&(self.methods.len() as u16).to_be_bytes());
        for m in &self.methods {
            self.write_method(m, &mut out);
        }
        // Atributos de la clase: `SourceFile`, `BootstrapMethods` y/o `RuntimeVisibleAnnotations`.
        let count = self.source_file.is_some() as u16
            + !self.bootstrap_methods.is_empty() as u16
            + self.annotations.is_some() as u16
            + !self.permitted_subclasses.is_empty() as u16
            + self.module.is_some() as u16
            + self.signature.is_some() as u16
            + !self.inner_classes.is_empty() as u16
            + self.enclosing_method.is_some() as u16
            + self.record_components.is_some() as u16
            + self.nest_host.is_some() as u16
            + !self.nest_members.is_empty() as u16
            + self.type_annotations.is_some() as u16;
        out.extend_from_slice(&count.to_be_bytes());
        if let Some(body) = &self.type_annotations {
            self.write_rvta(body, &mut out);
        }
        if let Some(name) = self.source_file {
            out.extend_from_slice(&self.source_attr_name.to_be_bytes());
            out.extend_from_slice(&2u32.to_be_bytes()); // attribute_length
            out.extend_from_slice(&name.to_be_bytes());
        }
        if !self.bootstrap_methods.is_empty() {
            self.write_bootstrap_methods(&mut out);
        }
        if let Some(body) = &self.annotations {
            self.write_rva(body, &mut out);
        }
        if !self.permitted_subclasses.is_empty() {
            self.write_permitted_subclasses(&mut out);
        }
        if let Some(body) = &self.module {
            out.extend_from_slice(&self.module_attr_name.to_be_bytes());
            out.extend_from_slice(&(body.len() as u32).to_be_bytes());
            out.extend_from_slice(body);
        }
        if let Some(sig) = self.signature {
            self.write_signature(sig, &mut out);
        }
        if let Some(host) = self.nest_host {
            out.extend_from_slice(&self.nest_host_attr_name.to_be_bytes());
            out.extend_from_slice(&2u32.to_be_bytes()); // attribute_length = 2
            out.extend_from_slice(&host.to_be_bytes());
        }
        if !self.nest_members.is_empty() {
            out.extend_from_slice(&self.nest_members_attr_name.to_be_bytes());
            let n = self.nest_members.len();
            out.extend_from_slice(&((2 + n * 2) as u32).to_be_bytes());
            out.extend_from_slice(&(n as u16).to_be_bytes());
            for &c in &self.nest_members {
                out.extend_from_slice(&c.to_be_bytes());
            }
        }
        if let Some((class_index, method_index)) = self.enclosing_method {
            out.extend_from_slice(&self.enclosing_method_attr_name.to_be_bytes());
            out.extend_from_slice(&4u32.to_be_bytes()); // attribute_length = 4
            out.extend_from_slice(&class_index.to_be_bytes());
            out.extend_from_slice(&method_index.to_be_bytes());
        }
        if let Some(components) = &self.record_components {
            out.extend_from_slice(&self.record_attr_name.to_be_bytes());
            // length = components_count (2) + Σ[ name (2) + desc (2) + attrs_count (2) + Signature? (8) ].
            let body_len: usize =
                2 + components.iter().map(|c| 6 + c.signature.is_some() as usize * 8).sum::<usize>();
            out.extend_from_slice(&(body_len as u32).to_be_bytes());
            out.extend_from_slice(&(components.len() as u16).to_be_bytes());
            for c in components {
                out.extend_from_slice(&c.name.to_be_bytes());
                out.extend_from_slice(&c.descriptor.to_be_bytes());
                out.extend_from_slice(&(c.signature.is_some() as u16).to_be_bytes()); // attributes_count
                if let Some(sig) = c.signature {
                    self.write_signature(sig, &mut out);
                }
            }
        }
        if !self.inner_classes.is_empty() {
            out.extend_from_slice(&self.inner_classes_attr_name.to_be_bytes());
            // length = number_of_classes (2) + n·(4 índices · 2 bytes).
            let n = self.inner_classes.len();
            out.extend_from_slice(&((2 + n * 8) as u32).to_be_bytes());
            out.extend_from_slice(&(n as u16).to_be_bytes());
            for e in &self.inner_classes {
                out.extend_from_slice(&e.inner.to_be_bytes());
                out.extend_from_slice(&e.outer.to_be_bytes());
                out.extend_from_slice(&e.name.to_be_bytes());
                out.extend_from_slice(&e.flags.to_be_bytes());
            }
        }
        out
    }

    /// El atributo `PermittedSubclasses` (§4.7.31): la lista de índices `Class` de los subtipos que
    /// un tipo `sealed` autoriza. `attribute_length` = `number_of_classes` (2) + n·2.
    fn write_permitted_subclasses(&self, out: &mut Vec<u8>) {
        out.extend_from_slice(&self.permitted_attr_name.to_be_bytes());
        let n = self.permitted_subclasses.len();
        out.extend_from_slice(&((2 + n * 2) as u32).to_be_bytes());
        out.extend_from_slice(&(n as u16).to_be_bytes());
        for &c in &self.permitted_subclasses {
            out.extend_from_slice(&c.to_be_bytes());
        }
    }

    /// El atributo `BootstrapMethods` (§4.7.23): un `MethodHandle` + sus argumentos estáticos por
    /// cada *call site* de `invokedynamic`.
    fn write_bootstrap_methods(&self, out: &mut Vec<u8>) {
        out.extend_from_slice(&self.bootstrap_attr_name.to_be_bytes());
        // `attribute_length` = num_bootstrap_methods (2) + Σ[ ref (2) + num_args (2) + args*2 ].
        let body_len: usize =
            2 + self.bootstrap_methods.iter().map(|b| 4 + b.args.len() * 2).sum::<usize>();
        out.extend_from_slice(&(body_len as u32).to_be_bytes());
        out.extend_from_slice(&(self.bootstrap_methods.len() as u16).to_be_bytes());
        for b in &self.bootstrap_methods {
            out.extend_from_slice(&b.method_handle.to_be_bytes());
            out.extend_from_slice(&(b.args.len() as u16).to_be_bytes());
            for &a in &b.args {
                out.extend_from_slice(&a.to_be_bytes());
            }
        }
    }

    fn write_method(&self, m: &MethodInfo, out: &mut Vec<u8>) {
        out.extend_from_slice(&m.access_flags.to_be_bytes());
        out.extend_from_slice(&m.name_index.to_be_bytes());
        out.extend_from_slice(&m.descriptor_index.to_be_bytes());
        // Un método `abstract` o `native` **no lleva `Code`** (§4.6): se emite con cero atributos.
        // Es lo que hace legal una interfaz o un método abstracto de una clase.
        const ACC_ABSTRACT: u16 = 0x0400;
        const ACC_NATIVE: u16 = 0x0100;
        if m.access_flags & (ACC_ABSTRACT | ACC_NATIVE) != 0 {
            let mattrs = !m.thrown_exceptions.is_empty() as u16
                + m.annotations.is_some() as u16
                + m.signature.is_some() as u16
                + !m.parameters.is_empty() as u16
                + m.type_annotations.is_some() as u16
                + m.parameter_annotations.is_some() as u16
                + m.annotation_default.is_some() as u16;
            out.extend_from_slice(&mattrs.to_be_bytes()); // attributes_count
            if let Some(value) = &m.annotation_default {
                self.write_annotation_default(value, out);
            }
            if !m.thrown_exceptions.is_empty() {
                self.write_exceptions(&m.thrown_exceptions, out);
            }
            if let Some(body) = &m.annotations {
                self.write_rva(body, out);
            }
            if let Some(body) = &m.parameter_annotations {
                self.write_rvpa(body, out);
            }
            if let Some(sig) = m.signature {
                self.write_signature(sig, out);
            }
            if !m.parameters.is_empty() {
                self.write_method_parameters(&m.parameters, out);
            }
            if let Some(body) = &m.type_annotations {
                self.write_rvta(body, out);
            }
            return;
        }
        // `Code`, más `Exceptions`/`RuntimeVisibleAnnotations`/`Signature`/`MethodParameters`/
        // `RuntimeVisibleTypeAnnotations` si corresponden.
        out.extend_from_slice(
            &(1 + !m.thrown_exceptions.is_empty() as u16
                + m.annotations.is_some() as u16
                + m.signature.is_some() as u16
                + !m.parameters.is_empty() as u16
                + m.type_annotations.is_some() as u16
                + m.parameter_annotations.is_some() as u16
                + m.annotation_default.is_some() as u16)
                .to_be_bytes(),
        );

        // Atributo Code (JVMS §4.7.3): max_stack, max_locals, code, sin tabla de excepciones. Lleva
        // adentro el `StackMapTable` (si el método tiene saltos; la v69 lo exige) y el
        // `LineNumberTable` (si se mapearon líneas), como **atributos anidados** del `Code`.
        let code_len = m.code.len();
        // 6 = name_index (2) + attribute_length (4) del header de cada atributo anidado.
        let smt_len = m.stack_map.as_ref().map_or(0, |b| b.len() + 6);
        let lnt_len =
            if m.line_numbers.is_empty() { 0 } else { 6 + 2 + m.line_numbers.len() * 4 };
        let lvt_len = if m.local_vars.is_empty() { 0 } else { 6 + 2 + m.local_vars.len() * 10 };
        // `RuntimeVisibleTypeAnnotations` anidado en el `Code` (targets de posición-bytecode).
        let crta_len = m.code_type_annotations.as_ref().map_or(0, |b| b.len() + 6);
        let code_attr_count = m.stack_map.is_some() as u16
            + !m.line_numbers.is_empty() as u16
            + !m.local_vars.is_empty() as u16
            + m.code_type_annotations.is_some() as u16;
        let attr_len = 2 + 2 + 4 + code_len + 2 + m.exceptions.len() * 8 + 2 + smt_len + lnt_len
            + lvt_len
            + crta_len;
        out.extend_from_slice(&self.code_attr_name.to_be_bytes());
        out.extend_from_slice(&(attr_len as u32).to_be_bytes());
        out.extend_from_slice(&m.max_stack.to_be_bytes());
        out.extend_from_slice(&m.max_locals.to_be_bytes());
        out.extend_from_slice(&(code_len as u32).to_be_bytes());
        out.extend_from_slice(&m.code);
        out.extend_from_slice(&(m.exceptions.len() as u16).to_be_bytes());
        for x in &m.exceptions {
            out.extend_from_slice(&x.start_pc.to_be_bytes());
            out.extend_from_slice(&x.end_pc.to_be_bytes());
            out.extend_from_slice(&x.handler_pc.to_be_bytes());
            out.extend_from_slice(&x.catch_type.to_be_bytes());
        }
        out.extend_from_slice(&code_attr_count.to_be_bytes()); // attributes_count del Code
        if let Some(body) = &m.stack_map {
            out.extend_from_slice(&self.stack_map_attr_name.to_be_bytes());
            out.extend_from_slice(&(body.len() as u32).to_be_bytes());
            out.extend_from_slice(body);
        }
        if !m.line_numbers.is_empty() {
            out.extend_from_slice(&self.line_number_attr_name.to_be_bytes());
            out.extend_from_slice(&((2 + m.line_numbers.len() * 4) as u32).to_be_bytes());
            out.extend_from_slice(&(m.line_numbers.len() as u16).to_be_bytes());
            for &(pc, line) in &m.line_numbers {
                out.extend_from_slice(&pc.to_be_bytes());
                out.extend_from_slice(&line.to_be_bytes());
            }
        }
        if !m.local_vars.is_empty() {
            out.extend_from_slice(&self.local_var_attr_name.to_be_bytes());
            out.extend_from_slice(&((2 + m.local_vars.len() * 10) as u32).to_be_bytes());
            out.extend_from_slice(&(m.local_vars.len() as u16).to_be_bytes());
            for &(start_pc, length, name, desc, slot) in &m.local_vars {
                out.extend_from_slice(&start_pc.to_be_bytes());
                out.extend_from_slice(&length.to_be_bytes());
                out.extend_from_slice(&name.to_be_bytes());
                out.extend_from_slice(&desc.to_be_bytes());
                out.extend_from_slice(&slot.to_be_bytes());
            }
        }
        // `RuntimeVisibleTypeAnnotations` **anidado en el `Code`**: mismo formato que el del método,
        // pero con targets de posición-bytecode (cast/`instanceof`/`new`).
        if let Some(body) = &m.code_type_annotations {
            self.write_rvta(body, out);
        }
        // `Exceptions`/`RuntimeVisibleAnnotations`/`Signature` del método van **junto** al `Code`,
        // como atributos del método (no anidados en el `Code`). `Exceptions` va primero, como javac.
        if !m.thrown_exceptions.is_empty() {
            self.write_exceptions(&m.thrown_exceptions, out);
        }
        if let Some(value) = &m.annotation_default {
            self.write_annotation_default(value, out);
        }
        if let Some(body) = &m.annotations {
            self.write_rva(body, out);
        }
        if let Some(body) = &m.parameter_annotations {
            self.write_rvpa(body, out);
        }
        if let Some(sig) = m.signature {
            self.write_signature(sig, out);
        }
        if !m.parameters.is_empty() {
            self.write_method_parameters(&m.parameters, out);
        }
        if let Some(body) = &m.type_annotations {
            self.write_rvta(body, out);
        }
    }
}

impl Default for ClassFile {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::jvm::class_file::ClassFile as JvmClass;
    use crate::jvm::parser::ConstantPoolEntry;
    use std::sync::atomic::{AtomicUsize, Ordering};

    /// Emite una clase con las tres entradas de `invokedynamic` y un `BootstrapMethods`, y la
    /// **re-parsea con la JVM propia** — el oráculo de que el `.class` quedó bien formado.
    #[test]
    fn serializes_invokedynamic_and_bootstrap_methods() {
        let mut cf = ClassFile::new();
        cf.this_class = cf.pool.class("C");
        cf.super_class = cf.pool.class("java/lang/Object");
        // El *bootstrap* estándar de las lambdas: `LambdaMetafactory.metafactory` (invokeStatic).
        let mh = cf.pool.method_handle(
            6,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;\
             Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;\
             Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)\
             Ljava/lang/invoke/CallSite;",
        );
        let mt = cf.pool.method_type("()V");
        cf.bootstrap_methods.push(BootstrapMethod { method_handle: mh, args: vec![mt] });
        let _indy = cf.pool.invoke_dynamic(0, "run", "()Ljava/lang/Runnable;");

        let bytes = cf.to_bytes();

        static N: AtomicUsize = AtomicUsize::new(0);
        let n = N.fetch_add(1, Ordering::Relaxed);
        let dir = std::env::temp_dir().join(format!("cw_indy_{}_{n}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        let path = dir.join("C.class");
        std::fs::write(&path, &bytes).unwrap();
        let jvm = JvmClass::from_path(path.to_str().unwrap()).expect("el .class debe parsear");
        let _ = std::fs::remove_dir_all(&dir);

        let has = |f: fn(&ConstantPoolEntry) -> bool| jvm.constant_pool.iter().any(f);
        assert!(has(|e| matches!(e, ConstantPoolEntry::InvokeDynamic { .. })), "InvokeDynamic");
        assert!(has(|e| matches!(e, ConstantPoolEntry::MethodHandle { .. })), "MethodHandle");
        assert!(has(|e| matches!(e, ConstantPoolEntry::MethodType { .. })), "MethodType");
        assert!(
            jvm.attributes.iter().any(|a| jvm.utf8(a.name_index) == Some("BootstrapMethods")),
            "atributo BootstrapMethods presente",
        );
    }

    /// Emite un tipo `sealed` con su atributo `PermittedSubclasses` y lo re-parsea con la JVM propia.
    #[test]
    fn serializes_permitted_subclasses() {
        let mut cf = ClassFile::new();
        cf.this_class = cf.pool.class("Shape");
        cf.super_class = cf.pool.class("java/lang/Object");
        cf.permitted_subclasses = vec![cf.pool.class("Circle"), cf.pool.class("Square")];

        let bytes = cf.to_bytes();

        static N: AtomicUsize = AtomicUsize::new(0);
        let n = N.fetch_add(1, Ordering::Relaxed);
        let dir = std::env::temp_dir().join(format!("cw_sealed_{}_{n}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        let path = dir.join("Shape.class");
        std::fs::write(&path, &bytes).unwrap();
        let jvm = JvmClass::from_path(path.to_str().unwrap()).expect("el .class debe parsear");
        let _ = std::fs::remove_dir_all(&dir);

        assert!(
            jvm.attributes.iter().any(|a| jvm.utf8(a.name_index) == Some("PermittedSubclasses")),
            "atributo PermittedSubclasses presente",
        );
    }

    /// Un método con un `invokedynamic` **pasa el verificador propio**: el opcode empuja el tipo de
    /// retorno del *call site* (acá `Object`), que el `areturn` consume. Es el oráculo de la Etapa 4.
    #[test]
    fn an_invokedynamic_method_passes_the_verifier() {
        use crate::jvm::class_file::ClassFile as JvmClass;
        use crate::jvm::interpreter::metaspace::MetaspaceService;
        use crate::jvm::verifier::verify_method;
        use std::path::PathBuf;

        let mut cf = ClassFile::new();
        cf.this_class = cf.pool.class("C");
        cf.super_class = cf.pool.class("java/lang/Object");
        let mh = cf.pool.method_handle(
            6,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            "()Ljava/lang/invoke/CallSite;",
        );
        cf.bootstrap_methods.push(BootstrapMethod { method_handle: mh, args: Vec::new() });
        // `make()Ljava/lang/Object;` = `invokedynamic get()Ljava/lang/Object;; areturn`.
        let indy = cf.pool.invoke_dynamic(0, "get", "()Ljava/lang/Object;");
        let name_index = cf.pool.utf8("make");
        let descriptor_index = cf.pool.utf8("()Ljava/lang/Object;");
        let code = vec![0xba, (indy >> 8) as u8, indy as u8, 0, 0, 0xb0];
        cf.methods.push(MethodInfo {
            access_flags: 0x0009, // public static
            name_index,
            descriptor_index,
            max_stack: 1,
            max_locals: 0,
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
        });

        static N: AtomicUsize = AtomicUsize::new(0);
        let n = N.fetch_add(1, Ordering::Relaxed);
        let dir = std::env::temp_dir().join(format!("cw_vf_{}_{n}", std::process::id()));
        std::fs::create_dir_all(&dir).unwrap();
        let path = dir.join("C.class");
        std::fs::write(&path, &cf.to_bytes()).unwrap();
        let jvm = JvmClass::from_path(path.to_str().unwrap()).expect("parsea");
        let mut ms = MetaspaceService::new(vec![PathBuf::from("boot")], vec![dir.clone()]);
        for m in &jvm.methods {
            verify_method(&mut ms, &jvm, m).expect("el invokedynamic debe verificar");
        }
        let _ = std::fs::remove_dir_all(&dir);
    }
}
