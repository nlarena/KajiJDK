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
const TAG_NAME_AND_TYPE: u8 = 12;
const TAG_METHOD_HANDLE: u8 = 15;
const TAG_METHOD_TYPE: u8 = 16;
const TAG_INVOKE_DYNAMIC: u8 = 18;

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
    NameAndType(u16, u16),
    /// `MethodHandle` (§4.4.8): un *reference kind* (1..=9) y el índice de lo referido (un
    /// `Methodref`/`Fieldref`). Lo pide el `invokedynamic` de las lambdas.
    MethodHandle(u8, u16),
    /// `MethodType` (§4.4.9): apunta al `Utf8` de un descriptor de método.
    MethodType(u16),
    /// `InvokeDynamic` (§4.4.10): el índice del *bootstrap method* (en el atributo
    /// `BootstrapMethods`) y un `NameAndType` (el nombre del SAM + el descriptor invocado).
    InvokeDynamic(u16, u16),
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

    /// Un `MethodHandle` (§4.4.8). Según su *reference kind* (§5.4.3.5) referencia un `Fieldref`
    /// (kinds 1–4: `getField`/`getStatic`/`putField`/`putStatic`, p. ej. los *getters* de un `record`)
    /// o un `Methodref` (5–8: los `invoke*` y `newInvokeSpecial`). No se deduplica (son pocos).
    pub fn method_handle(&mut self, kind: u8, class: &str, name: &str, descriptor: &str) -> u16 {
        let reference = if (1..=4).contains(&kind) {
            self.fieldref(class, name, descriptor)
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
            }
        }
    }
}

/// Un campo listo para serializar (JVMS §4.5). Sin atributos por ahora — un `ConstantValue` haría
/// falta solo para un `static final` con inicializador constante.
pub struct FieldInfo {
    pub access_flags: u16,
    pub name_index: u16,
    pub descriptor_index: u16,
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
    pub fields: Vec<FieldInfo>,
    pub methods: Vec<MethodInfo>,
    /// Índice Utf8 del nombre del fuente (`Add.java`), para el atributo `SourceFile`.
    pub source_file: Option<u16>,
    /// Los *bootstrap methods* del atributo `BootstrapMethods` (§4.7.23) — uno por *call site* de
    /// `invokedynamic`. Vacío si la clase no usa lambdas/refs.
    pub bootstrap_methods: Vec<BootstrapMethod>,
    /// Índice Utf8 de `"Code"` (se necesita en cada método), reservado una sola vez.
    code_attr_name: u16,
    source_attr_name: u16,
    stack_map_attr_name: u16,
    bootstrap_attr_name: u16,
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
        ClassFile {
            pool,
            access_flags: 0,
            this_class: 0,
            super_class: 0,
            fields: Vec::new(),
            methods: Vec::new(),
            source_file: None,
            bootstrap_methods: Vec::new(),
            code_attr_name,
            source_attr_name,
            stack_map_attr_name,
            bootstrap_attr_name,
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
        out.extend_from_slice(&0u16.to_be_bytes()); // interfaces_count
        out.extend_from_slice(&(self.fields.len() as u16).to_be_bytes());
        for f in &self.fields {
            out.extend_from_slice(&f.access_flags.to_be_bytes());
            out.extend_from_slice(&f.name_index.to_be_bytes());
            out.extend_from_slice(&f.descriptor_index.to_be_bytes());
            out.extend_from_slice(&0u16.to_be_bytes()); // attributes_count
        }
        out.extend_from_slice(&(self.methods.len() as u16).to_be_bytes());
        for m in &self.methods {
            self.write_method(m, &mut out);
        }
        // Atributos de la clase: `SourceFile` y/o `BootstrapMethods`, los que haya.
        let count = self.source_file.is_some() as u16 + !self.bootstrap_methods.is_empty() as u16;
        out.extend_from_slice(&count.to_be_bytes());
        if let Some(name) = self.source_file {
            out.extend_from_slice(&self.source_attr_name.to_be_bytes());
            out.extend_from_slice(&2u32.to_be_bytes()); // attribute_length
            out.extend_from_slice(&name.to_be_bytes());
        }
        if !self.bootstrap_methods.is_empty() {
            self.write_bootstrap_methods(&mut out);
        }
        out
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
        out.extend_from_slice(&1u16.to_be_bytes()); // attributes_count: solo `Code`

        // Atributo Code (JVMS §4.7.3): max_stack, max_locals, code, sin tabla de excepciones. Lleva
        // adentro el `StackMapTable` si el método tiene saltos (la v69 lo exige).
        let code_len = m.code.len();
        // 6 = name_index (2) + attribute_length (4) del atributo anidado.
        let smt_len = m.stack_map.as_ref().map_or(0, |b| b.len() + 6);
        let attr_len = 2 + 2 + 4 + code_len + 2 + m.exceptions.len() * 8 + 2 + smt_len;
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
        match &m.stack_map {
            Some(body) => {
                out.extend_from_slice(&1u16.to_be_bytes()); // attributes_count del Code
                out.extend_from_slice(&self.stack_map_attr_name.to_be_bytes());
                out.extend_from_slice(&(body.len() as u32).to_be_bytes());
                out.extend_from_slice(body);
            }
            None => out.extend_from_slice(&0u16.to_be_bytes()),
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
