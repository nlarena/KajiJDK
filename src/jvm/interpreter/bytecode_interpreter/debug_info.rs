//! **Snapshot de depuración** (Hito I5b): la metadata de clases/métodos/líneas que un cliente JDWP
//! necesita para **traducir una posición de fuente** (`Add.java:12`) a la `(methodID, índice)` que la
//! VM usa para un breakpoint.
//!
//! ## Por qué un snapshot (el muro de inmutabilidad)
//!
//! Listar los métodos de una clase con su **`MethodId` real** exige **resolverlos** (`&mut metaspace`),
//! porque la resolución parsea el `Code` la primera vez. Pero dentro de un callback de evento el
//! metaspace es **inmutable** —es lo que hace posible el destructure disjunto del [`super::jvmti`]—, así
//! que no se puede resolver ahí. La salida: **capturar** todo una vez, al attachear (cuando todavía hay
//! `&mut metaspace`), en esta estructura de solo lectura que el [`bridge`](super::bridge) sirve durante
//! los eventos. Como `resolve_method` **cachea** por `(clase, nombre, descriptor)`, el `MethodId` del
//! snapshot es **el mismo** que la VM usa al correr — por eso un breakpoint puesto por su `methodID`
//! frena de verdad.
//!
//! **Límite conocido**: el snapshot es de las clases **ya cargadas** al attachear. Una clase que se
//! carga después no aparece (haría falta refrescarlo en un evento `ClassPrepare`, diferido).

use super::super::metaspace::MetaspaceService;
use super::super::super::class_file::ClassFile;

/// Una **variable local** de un método (de la `LocalVariableTable`, que solo emite `javac -g`): su
/// nombre, descriptor, slot y el rango de bytecode donde está en alcance. Para `Method.VariableTable`.
#[derive(Debug, Clone)]
pub struct LocalVarMeta {
    pub name: String,
    pub signature: String,
    pub slot: u16,
    pub start_pc: u64,
    pub length: u64,
}

/// La metadata de un **método**: su `MethodId` real (id JDWP), nombre, descriptor, flags de acceso, la
/// **tabla de líneas** (`índice de bytecode → línea de fuente`), el largo del código (para el `end` de
/// `Method.LineTable`), las **variables locales** (si el `.class` trae `LocalVariableTable`) y cuántos
/// slots ocupan los argumentos (el `argCnt` de `Method.VariableTable`).
#[derive(Debug, Clone)]
pub struct MethodMeta {
    pub id: u64,
    pub name: String,
    pub signature: String,
    pub mod_bits: u32,
    pub line_table: Vec<(u64, i32)>,
    pub code_len: u64,
    pub variables: Vec<LocalVarMeta>,
    pub arg_slots: u32,
}

/// La metadata de un **campo**: su `fieldID` (index+1 dentro de la clase; el 0 es null en JDWP), nombre,
/// descriptor y flags. Para `ReferenceType.Fields` y para mapear un field watchpoint.
#[derive(Debug, Clone)]
pub struct FieldMeta {
    pub id: u64,
    pub name: String,
    pub signature: String,
    pub mod_bits: u32,
    /// El offset en bytes del campo **dentro del objeto** (incluye el header), para leer su valor del
    /// heap con `ObjectReference.GetValues`. `0` para un campo estático (no vive en el objeto).
    pub offset: usize,
}

/// La metadata de una **clase**: su `referenceTypeID` (índice estable en el snapshot), su firma JVM
/// (`LAdd;`), el archivo fuente (si el `.class` lo trae), sus métodos y sus campos.
#[derive(Debug, Clone)]
pub struct ClassMeta {
    pub id: u64,
    pub signature: String,
    /// El nombre binario de la superclase (`None` si es `java/lang/Object`). Para `ClassType.Superclass`,
    /// que un cliente usa al caminar la jerarquía resolviendo un campo heredado.
    pub super_name: Option<String>,
    pub source_file: Option<String>,
    pub methods: Vec<MethodMeta>,
    pub fields: Vec<FieldMeta>,
}

/// El snapshot completo: las clases cargadas con su metadata de depuración. Solo lectura; lo sirve el
/// bridge durante los eventos.
#[derive(Debug, Clone, Default)]
pub struct VmSnapshot {
    classes: Vec<ClassMeta>,
}

impl VmSnapshot {
    /// **Captura** la metadata de todas las clases cargadas en `ms`. Resuelve cada método (para su
    /// `MethodId` real) y le extrae el descriptor y la `LineNumberTable`. Se llama al attachear, con
    /// `&mut metaspace` disponible.
    pub fn capture(ms: &mut MetaspaceService) -> Self {
        let names = ms.loaded_class_names();
        let mut classes = Vec::with_capacity(names.len());
        for (idx, name) in names.iter().enumerate() {
            // Primero, con un préstamo **inmutable**, juntamos métodos (nombre/descriptor/flags), campos,
            // el archivo fuente y la superclase.
            let (members, field_infos, source_file, super_name) = match ms.get(name) {
                Some(cf) => {
                    let members: Vec<(String, String, u16)> = cf
                        .methods
                        .iter()
                        .filter_map(|m| {
                            Some((
                                cf.utf8(m.name_index)?.to_string(),
                                cf.utf8(m.descriptor_index)?.to_string(),
                                m.access_flags,
                            ))
                        })
                        .collect();
                    // Los campos como tuplas (el offset se computa después, necesita `&mut ms`).
                    let field_infos: Vec<(String, String, u16)> = cf
                        .fields
                        .iter()
                        .filter_map(|f| {
                            Some((
                                cf.utf8(f.name_index)?.to_string(),
                                cf.utf8(f.descriptor_index)?.to_string(),
                                f.access_flags,
                            ))
                        })
                        .collect();
                    let super_name = cf.class_name(cf.super_class).map(str::to_string);
                    (members, field_infos, source_file_of(cf).map(str::to_string), super_name)
                }
                None => continue,
            };
            // Los campos con su offset en el objeto (`field_offset` necesita `&mut ms`). fieldID = index+1.
            let fields: Vec<FieldMeta> = field_infos
                .into_iter()
                .enumerate()
                .map(|(i, (fname, fdesc, fflags))| {
                    let is_static = fflags & 0x0008 != 0;
                    let offset = if is_static {
                        0
                    } else {
                        super::objects_operations::field_offset(ms, name, &fname)
                    };
                    FieldMeta {
                        id: i as u64 + 1,
                        name: fname,
                        signature: fdesc,
                        mod_bits: fflags as u32,
                        offset,
                    }
                })
                .collect();
            let signature = binary_to_signature(name);
            let mut methods = Vec::with_capacity(members.len());
            for (mname, descriptor, flags) in members {
                // Resolución (préstamo **mutable**): el `MethodId` real que usará la VM.
                let Some(method_id) = ms.resolve_method(name, &mname, &descriptor) else { continue };
                // La tabla de líneas, el largo del código y las variables locales (préstamo inmutable).
                let (line_table, code_len, variables) =
                    ms.get(name).and_then(|cf| method_debug(cf, &mname, &descriptor)).unwrap_or_default();
                // `argCnt`: los slots que ocupan los argumentos (+1 por `this` si no es estático).
                let is_static = flags & 0x0008 != 0;
                let param_slots: usize = MetaspaceService::param_slot_widths(&descriptor).iter().sum();
                let arg_slots = (param_slots + usize::from(!is_static)) as u32;
                methods.push(MethodMeta {
                    id: method_id as u64,
                    name: mname,
                    signature: descriptor,
                    mod_bits: flags as u32,
                    line_table,
                    code_len,
                    variables,
                    arg_slots,
                });
            }
            // El `referenceTypeID` arranca en 1: en JDWP el id 0 significa **null** y un cliente real
            // (el JDI de `jdb`/IntelliJ) lo rechaza al construir el ReferenceType.
            classes.push(ClassMeta {
                id: idx as u64 + 1,
                signature,
                super_name,
                source_file,
                methods,
                fields,
            });
        }
        VmSnapshot { classes }
    }

    /// Todas las clases del snapshot.
    pub fn classes(&self) -> &[ClassMeta] {
        &self.classes
    }

    /// La clase con ese `referenceTypeID`.
    pub fn class(&self, id: u64) -> Option<&ClassMeta> {
        self.classes.iter().find(|c| c.id == id)
    }

    /// Un método por su `methodID` (buscando en todas las clases).
    pub fn method(&self, id: u64) -> Option<&MethodMeta> {
        self.classes.iter().flat_map(|c| &c.methods).find(|m| m.id == id)
    }

    /// El `referenceTypeID` de la clase que **declara** un método. Lo necesita la *location* de un
    /// evento/frame: en JDWP la location lleva el classID, y un cliente real lo valida (classID `0` es
    /// null → «Invalid frame location»).
    pub fn class_id_of_method(&self, method_id: u64) -> Option<u64> {
        self.classes.iter().find(|c| c.methods.iter().any(|m| m.id == method_id)).map(|c| c.id)
    }

    /// El campo `(classID, fieldID)` → su `(nombre de clase, nombre de campo)` — para traducir un field
    /// watchpoint que el cliente pidió por ids a los nombres que usa el gancho de la VM.
    pub fn field_name(&self, class_id: u64, field_id: u64) -> Option<(&str, &str)> {
        let class = self.class(class_id)?;
        let field = class.fields.iter().find(|f| f.id == field_id)?;
        let binary = class.signature.strip_prefix('L')?.strip_suffix(';')?;
        Some((binary, field.name.as_str()))
    }

    /// La inversa: `(nombre binario de clase, nombre de campo)` → `(classID, fieldID)` — para armar el
    /// evento `Composite` de un field watchpoint (el gancho de la VM da nombres).
    pub fn field_id(&self, class_binary: &str, field: &str) -> Option<(u64, u64)> {
        let signature = binary_to_signature(class_binary);
        let class = self.classes.iter().find(|c| c.signature == signature)?;
        let f = class.fields.iter().find(|f| f.name == field)?;
        Some((class.id, f.id))
    }
}

/// El nombre binario (`Add`, `java/lang/Object`) a firma JVM (`LAdd;`, `Ljava/lang/Object;`).
pub fn binary_to_signature(binary_name: &str) -> String {
    format!("L{binary_name};")
}

/// Lee el atributo `SourceFile` de una clase (índice Utf8 → nombre), si lo trae.
fn source_file_of(cf: &ClassFile) -> Option<&str> {
    let attr = cf.attributes.iter().find(|a| cf.utf8(a.name_index) == Some("SourceFile"))?;
    let idx = u16::from_be_bytes([*attr.info.first()?, *attr.info.get(1)?]);
    cf.utf8(idx)
}

/// La tabla de líneas, el largo del código y la tabla de variables locales de un método (de su `Code`:
/// `LineNumberTable` + `LocalVariableTable`, esta última solo si se compiló con `javac -g`).
fn method_debug(
    cf: &ClassFile,
    name: &str,
    descriptor: &str,
) -> Option<(Vec<(u64, i32)>, u64, Vec<LocalVarMeta>)> {
    let member = cf.methods.iter().find(|m| {
        cf.utf8(m.name_index) == Some(name) && cf.utf8(m.descriptor_index) == Some(descriptor)
    })?;
    let code = cf.member_code(member)?;
    let code_len = code.code.len() as u64;
    let line_table = code
        .attributes
        .iter()
        .find(|a| cf.utf8(a.name_index) == Some("LineNumberTable"))
        .map(|a| decode_line_number_table(&a.info))
        .unwrap_or_default();
    let variables = code
        .attributes
        .iter()
        .find(|a| cf.utf8(a.name_index) == Some("LocalVariableTable"))
        .map(|a| decode_local_variable_table(cf, &a.info))
        .unwrap_or_default();
    Some((line_table, code_len, variables))
}

/// Decodifica un `LocalVariableTable` resolviendo los índices de nombre/descriptor contra el pool.
fn decode_local_variable_table(cf: &ClassFile, bytes: &[u8]) -> Vec<LocalVarMeta> {
    crate::jvm::parser::attributes::local_variables::parse(bytes)
        .into_iter()
        .filter_map(|v| {
            Some(LocalVarMeta {
                name: cf.utf8(v.name_index)?.to_string(),
                signature: cf.utf8(v.type_index)?.to_string(),
                slot: v.slot,
                start_pc: v.start_pc as u64,
                length: v.length as u64,
            })
        })
        .collect()
}

/// Decodifica un `LineNumberTable`: `u16 count` + `count × (u16 start_pc, u16 line)`. Devuelve pares
/// `(índice de bytecode, número de línea)`.
fn decode_line_number_table(bytes: &[u8]) -> Vec<(u64, i32)> {
    if bytes.len() < 2 {
        return Vec::new();
    }
    let count = u16::from_be_bytes([bytes[0], bytes[1]]) as usize;
    let mut out = Vec::with_capacity(count);
    let mut p = 2;
    for _ in 0..count {
        if p + 4 > bytes.len() {
            break;
        }
        let start_pc = u16::from_be_bytes([bytes[p], bytes[p + 1]]);
        let line = u16::from_be_bytes([bytes[p + 2], bytes[p + 3]]);
        out.push((start_pc as u64, line as i32));
        p += 4;
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::path::PathBuf;

    #[test]
    fn line_number_table_decodes_pairs() {
        // count=2, (0 → línea 3), (4 → línea 5).
        let bytes = [0, 2, 0, 0, 0, 3, 0, 4, 0, 5];
        assert_eq!(decode_line_number_table(&bytes), vec![(0, 3), (4, 5)]);
        assert_eq!(decode_line_number_table(&[]), vec![]);
    }

    #[test]
    fn binary_names_become_jvm_signatures() {
        assert_eq!(binary_to_signature("Add"), "LAdd;");
        assert_eq!(binary_to_signature("java/lang/Object"), "Ljava/lang/Object;");
    }

    #[test]
    fn capture_snapshots_a_loaded_class_with_real_method_ids() {
        // Carga java/Add.class (desde la raíz del repo, cwd de `cargo test`) y captura.
        let cf = match ClassFile::from_path("java/Add.class") {
            Ok(cf) => cf,
            Err(_) => return, // sin el fixture, el test no aplica
        };
        let name = cf.class_name(cf.this_class).unwrap().to_string();
        let mut ms = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        ms.add(name.clone(), cf);

        // El `MethodId` real de `add` según la VM.
        let real_add = ms.resolve_method(&name, "add", "(II)I").expect("add existe");

        let snap = VmSnapshot::capture(&mut ms);
        let class = snap.classes().iter().find(|c| c.signature == binary_to_signature(&name)).expect("la clase está");
        let add = class.methods.iter().find(|m| m.name == "add").expect("add en el snapshot");
        assert_eq!(add.id, real_add as u64, "el methodID del snapshot = el que usa la VM");
        assert_eq!(add.signature, "(II)I");
        assert!(!add.line_table.is_empty(), "add trae LineNumberTable");
        // el índice 0 mapea a alguna línea de Add.java.
        assert_eq!(add.line_table[0].0, 0);
        assert!(snap.method(real_add as u64).is_some(), "se encuentra por methodID");
    }

    #[test]
    fn capture_reads_the_local_variable_table_of_a_g_compiled_class() {
        // java/Locals.class se compila con `javac -g`, así que trae LocalVariableTable.
        let cf = match ClassFile::from_path("java/Locals.class") {
            Ok(cf) => cf,
            Err(_) => return, // sin el fixture -g, el test no aplica
        };
        let name = cf.class_name(cf.this_class).unwrap().to_string();
        let mut ms = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        ms.add(name.clone(), cf);

        let snap = VmSnapshot::capture(&mut ms);
        let class = snap.classes().iter().find(|c| c.signature == binary_to_signature(&name)).unwrap();
        let compute = class.methods.iter().find(|m| m.name == "compute").expect("compute");
        assert_eq!(compute.arg_slots, 2, "compute(int,int) estático → 2 slots de argumentos");
        // Las cuatro variables locales, por nombre y slot.
        let by_slot: Vec<(&str, u16)> =
            compute.variables.iter().map(|v| (v.name.as_str(), v.slot)).collect();
        assert!(by_slot.contains(&("a", 0)), "{by_slot:?}");
        assert!(by_slot.contains(&("b", 1)), "{by_slot:?}");
        assert!(by_slot.contains(&("sum", 2)), "{by_slot:?}");
        assert!(by_slot.contains(&("doubled", 3)), "{by_slot:?}");
        // `sum` (slot 2) recién entra en alcance en el índice 4 (tras el primer istore).
        let sum = compute.variables.iter().find(|v| v.name == "sum").unwrap();
        assert_eq!((sum.start_pc, sum.signature.as_str()), (4, "I"));
    }
}
