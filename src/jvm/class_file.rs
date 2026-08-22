//! The in-memory model of a parsed `.class` file.

use super::parser::attributes::{annotations, bootstrap_methods};
use super::parser::{
    attribute, code, constant_pool, member, AttributeInfo, ClassReader, Code, ConstantPoolEntry,
    MemberInfo, ParseError,
};

/// Every `.class` starts with this magic number.
const MAGIC: u32 = 0xCAFE_BABE;

// Class access-flag bits (JVM spec §4.1, Table 4.1-B).
const ACC_PUBLIC: u16 = 0x0001;
const ACC_FINAL: u16 = 0x0010;
const ACC_SUPER: u16 = 0x0020;
const ACC_INTERFACE: u16 = 0x0200;
const ACC_ABSTRACT: u16 = 0x0400;
const ACC_SYNTHETIC: u16 = 0x1000;
const ACC_ANNOTATION: u16 = 0x2000;
const ACC_ENUM: u16 = 0x4000;
const ACC_MODULE: u16 = 0x8000;

/// The nine `MethodHandle` reference kinds (JVMS §4.4.8, table 4.4.8-A). The kind is
/// what turns a symbolic reference into a *behaviour*: the same `Methodref` means a
/// virtual call under `InvokeVirtual` and a constructor invocation under
/// `NewInvokeSpecial`.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MethodHandleKind {
    GetField,
    GetStatic,
    PutField,
    PutStatic,
    InvokeVirtual,
    InvokeStatic,
    InvokeSpecial,
    NewInvokeSpecial,
    InvokeInterface,
}

impl MethodHandleKind {
    /// The kind byte as stored in the constant pool; `None` for anything outside 1..=9.
    fn from_byte(kind: u8) -> Option<Self> {
        Some(match kind {
            1 => Self::GetField,
            2 => Self::GetStatic,
            3 => Self::PutField,
            4 => Self::PutStatic,
            5 => Self::InvokeVirtual,
            6 => Self::InvokeStatic,
            7 => Self::InvokeSpecial,
            8 => Self::NewInvokeSpecial,
            9 => Self::InvokeInterface,
            _ => return None,
        })
    }

    /// The `REF_*` byte (JVMS Table 5.4.3.5-A) — the inverse of [`from_byte`]. Used to carry the
    /// kind into a materialised `MethodHandle` object (an `ldc` of a `MethodHandle` constant).
    pub fn to_byte(self) -> u8 {
        match self {
            Self::GetField => 1,
            Self::GetStatic => 2,
            Self::PutField => 3,
            Self::PutStatic => 4,
            Self::InvokeVirtual => 5,
            Self::InvokeStatic => 6,
            Self::InvokeSpecial => 7,
            Self::NewInvokeSpecial => 8,
            Self::InvokeInterface => 9,
        }
    }

    /// Whether the handle's pool index names a **field** (kinds 1–4) rather than a
    /// method — the fork that decides how the reference is resolved.
    pub fn names_a_field(self) -> bool {
        matches!(self, Self::GetField | Self::GetStatic | Self::PutField | Self::PutStatic)
    }
}

/// What a `MethodHandle` constant resolves to: the member it names, plus the kind that
/// says how it would be accessed. Borrows from the class file's pool.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct MethodHandleRef<'a> {
    pub kind: MethodHandleKind,
    pub class: &'a str,
    pub name: &'a str,
    pub descriptor: &'a str,
}

/// A parsed Java class file.
///
/// This is the *result* of parsing: the `parser` module walks the raw bytes and
/// fills this struct in. Fields are added as we parse each section.
pub struct ClassFile {
    /// Minor part of the class file format version.
    pub minor_version: u16,
    /// Major part of the version (65 = Java 21, 52 = Java 8, …).
    pub major_version: u16,
    /// The constant pool entries, in order. `constant_pool[0]` is spec entry #1.
    /// (The header's `constant_pool_count` is just `constant_pool.len() + 1`.)
    pub constant_pool: Vec<ConstantPoolEntry>,
    /// Class-level access flags: a fixed `u2` bitmask (ACC_PUBLIC, ACC_FINAL, …).
    pub access_flags: u16,
    /// Constant-pool index of the `Class` entry naming THIS class.
    pub this_class: u16,
    /// Constant-pool index of the superclass's `Class` entry (0 only for Object).
    pub super_class: u16,
    /// Constant-pool indices of the directly implemented interfaces (each points
    /// to a `Class` entry). Empty if the class implements none.
    pub interfaces: Vec<u16>,
    /// The class's fields (`field_info[]`).
    pub fields: Vec<MemberInfo>,
    /// The class's methods (`method_info[]`); each method's bytecode lives in its
    /// `Code` attribute.
    pub methods: Vec<MemberInfo>,
    /// Class-level attributes (`SourceFile`, `InnerClasses`, …).
    pub attributes: Vec<AttributeInfo>,
}

impl ClassFile {
    /// Loads the `.class` file at `path` and parses it into a `ClassFile`.
    ///
    /// Returns an error instead of panicking: I/O failures, a bad magic number,
    /// truncated files, etc. all surface as a [`ParseError`].
    pub fn from_path(path: &str) -> Result<Self, ParseError> {
        // Read the whole file into an owned Vec<u8>. `?` turns an io::Error into
        // a ParseError via the `From` impl.
        let bytes = std::fs::read(path)?;
        Self::from_bytes(&bytes)
    }

    /// Parses a class from raw `.class` bytes — for classes the VM **generates at runtime**
    /// (a lambda's implementing class, spun by the metafactory through the `.class` writer)
    /// rather than reading from disk. Same parser as [`from_path`], minus the file read.
    pub fn from_bytes(bytes: &[u8]) -> Result<Self, ParseError> {
        // The reader borrows those bytes; it lives only for this function.
        let mut reader = ClassReader::new(bytes);

        // --- Fixed-position header (first 10 bytes, identical in every .class) ---
        let magic = reader.read_u32()?;
        if magic != MAGIC {
            return Err(ParseError::BadMagic(magic));
        }

        let minor_version = reader.read_u16()?;
        let major_version = reader.read_u16()?;
        // `constant_pool_count` only drives the loop below; no need to store it.
        let constant_pool_count = reader.read_u16()?;

        // --- Constant pool: count - 1 entries, variable length ---
        let constant_pool = constant_pool::parse(&mut reader, constant_pool_count)?;

        // --- access_flags: a fixed u2 bitmask (always 2 bytes) ---
        let access_flags = reader.read_u16()?;

        // --- this_class / super_class: u2 indices into the constant pool ---
        let this_class = reader.read_u16()?;
        let super_class = reader.read_u16()?;

        // --- interfaces: a u2 count, then that many u2 class indices ---
        let interfaces_count = reader.read_u16()?;
        let mut interfaces = Vec::with_capacity(interfaces_count as usize);
        for _ in 0..interfaces_count {
            interfaces.push(reader.read_u16()?);
        }

        // --- fields, methods, attributes: each is its own "count + N elements".
        //     (fields and methods share MemberInfo; this reads to end of file.) ---
        let fields = member::parse_members(&mut reader)?;
        let methods = member::parse_members(&mut reader)?;
        let attributes = attribute::parse_attributes(&mut reader)?;

        Ok(ClassFile {
            minor_version,
            major_version,
            constant_pool,
            access_flags,
            this_class,
            super_class,
            interfaces,
            fields,
            methods,
            attributes,
        })
        // `reader` and `bytes` are dropped here; the ClassFile owns its own data.
    }

    // --- Access-flag queries: each tests one bit of the u2 mask on demand,
    //     so we get 9 booleans without storing 9 redundant bytes. ---
    pub fn is_public(&self) -> bool { self.access_flags & ACC_PUBLIC != 0 }
    pub fn is_final(&self) -> bool { self.access_flags & ACC_FINAL != 0 }
    pub fn is_super(&self) -> bool { self.access_flags & ACC_SUPER != 0 }
    pub fn is_interface(&self) -> bool { self.access_flags & ACC_INTERFACE != 0 }
    pub fn is_abstract(&self) -> bool { self.access_flags & ACC_ABSTRACT != 0 }
    pub fn is_synthetic(&self) -> bool { self.access_flags & ACC_SYNTHETIC != 0 }
    pub fn is_annotation(&self) -> bool { self.access_flags & ACC_ANNOTATION != 0 }
    pub fn is_enum(&self) -> bool { self.access_flags & ACC_ENUM != 0 }
    pub fn is_module(&self) -> bool { self.access_flags & ACC_MODULE != 0 }

    /// Resolves a constant-pool index pointing to a `Class` entry into the
    /// class's binary name (e.g. "java/lang/Object"). Returns `None` for index 0
    /// ("no class", as in `Object`'s super) or if the indices don't line up.
    ///
    /// Follows two hops: index -> `Class { name_index }` -> `Utf8`.
    pub fn class_name(&self, class_index: u16) -> Option<&str> {
        if class_index == 0 {
            return None;
        }
        let name_index = match self.constant_pool.get((class_index - 1) as usize)? {
            ConstantPoolEntry::Class { name_index } => *name_index,
            _ => return None,
        };
        match self.constant_pool.get((name_index - 1) as usize)? {
            ConstantPoolEntry::Utf8(name) => Some(name),
            _ => None,
        }
    }

    /// Resolves a constant-pool index that points **directly** to a `Utf8` entry
    /// (used for field/method names, descriptors and attribute names).
    pub fn utf8(&self, index: u16) -> Option<&str> {
        match self.constant_pool.get((index.checked_sub(1)?) as usize)? {
            ConstantPoolEntry::Utf8(s) => Some(s),
            _ => None,
        }
    }

    /// Resolves a `String` constant (tag 8) to its text, following `String → Utf8`.
    /// What `ldc "..."` loads.
    pub fn string_constant(&self, index: u16) -> Option<&str> {
        match self.constant_pool.get((index.checked_sub(1)?) as usize)? {
            ConstantPoolEntry::String { string_index } => self.utf8(*string_index),
            _ => None,
        }
    }

    /// Resolves an `Integer` constant (tag 3) to its value (an `ldc` of a big int).
    pub fn integer_constant(&self, index: u16) -> Option<i32> {
        match self.constant_pool.get((index.checked_sub(1)?) as usize)? {
            ConstantPoolEntry::Integer(v) => Some(*v),
            _ => None,
        }
    }

    /// Resolves a `Long` constant (tag 5) to its value — what an `ldc2_w` of a
    /// `long` literal loads. (Long/Double occupy two pool slots; the index still
    /// points at the first.)
    pub fn long_constant(&self, index: u16) -> Option<i64> {
        match self.constant_pool.get((index.checked_sub(1)?) as usize)? {
            ConstantPoolEntry::Long(v) => Some(*v),
            _ => None,
        }
    }

    /// Resolves a `Double` constant (tag 6) to its value — what an `ldc2_w` of a
    /// `double` literal loads.
    pub fn double_constant(&self, index: u16) -> Option<f64> {
        match self.constant_pool.get((index.checked_sub(1)?) as usize)? {
            ConstantPoolEntry::Double(v) => Some(*v),
            _ => None,
        }
    }

    /// Resolves a `Float` constant (tag 4) to its value — what an `ldc`/`ldc_w` of a
    /// `float` literal loads. `float` is category-1, so it comes through `ldc` (not
    /// `ldc2_w`).
    pub fn float_constant(&self, index: u16) -> Option<f32> {
        match self.constant_pool.get((index.checked_sub(1)?) as usize)? {
            ConstantPoolEntry::Float(v) => Some(*v),
            _ => None,
        }
    }

    /// The name part of a `NameAndType` constant (e.g. the method name an
    /// `EnclosingMethod` points at).
    pub fn name_and_type_name(&self, index: u16) -> Option<&str> {
        match self.constant_pool.get((index.checked_sub(1)?) as usize)? {
            ConstantPoolEntry::NameAndType { name_index, .. } => self.utf8(*name_index),
            _ => None,
        }
    }

    /// Resolves a `NameAndType` constant to its `(name, descriptor)`. Unlike
    /// [`Self::methodref_name_and_type`], this takes the `NameAndType` index *directly* —
    /// which is what an `InvokeDynamic` entry carries. An indy call site names no owning
    /// class, because its target isn't in the pool at all: a bootstrap method produces it
    /// at first execution.
    pub fn name_and_type(&self, index: u16) -> Option<(&str, &str)> {
        match self.constant_pool.get((index.checked_sub(1)?) as usize)? {
            ConstantPoolEntry::NameAndType { name_index, descriptor_index } => {
                Some((self.utf8(*name_index)?, self.utf8(*descriptor_index)?))
            }
            _ => None,
        }
    }

    /// Resolves an `InvokeDynamic` constant to `(bootstrap method index, name,
    /// descriptor)`. The index is into the class's `BootstrapMethods` attribute; the
    /// descriptor is the call site's *shape* — what it pops and what it pushes.
    pub fn invokedynamic_site(&self, index: u16) -> Option<(u16, &str, &str)> {
        let (bsm_index, nt_index) = match self.constant_pool.get((index.checked_sub(1)?) as usize)? {
            ConstantPoolEntry::InvokeDynamic { bootstrap_method_attr_index, name_and_type_index } => {
                (*bootstrap_method_attr_index, *name_and_type_index)
            }
            _ => return None,
        };
        let (name, descriptor) = self.name_and_type(nt_index)?;
        Some((bsm_index, name, descriptor))
    }

    /// Resolves a `Dynamic` constant (tag 17 — a *constant* dynamic, "condy") to
    /// `(bootstrap method index, name, descriptor)`.
    ///
    /// The twin of [`Self::invokedynamic_site`], and structurally identical: both name a
    /// bootstrap method that computes something at first use. The difference is only what
    /// they produce — a call site there, a **value** here.
    pub fn dynamic_constant(&self, index: u16) -> Option<(u16, &str, &str)> {
        let (bsm_index, nt_index) = match self.constant_pool.get((index.checked_sub(1)?) as usize)? {
            ConstantPoolEntry::Dynamic { bootstrap_method_attr_index, name_and_type_index } => {
                (*bootstrap_method_attr_index, *name_and_type_index)
            }
            _ => return None,
        };
        let (name, descriptor) = self.name_and_type(nt_index)?;
        Some((bsm_index, name, descriptor))
    }

    /// Resolves a `MethodHandle` constant (§4.4.8) to **what it points at plus how it
    /// would be invoked**.
    ///
    /// A handle is a *reference kind* and a pool index, and the kind decides which kind
    /// of entry that index names: kinds 1–4 (`getField`…`putStatic`) point at a
    /// `Fieldref`, the rest at a `Methodref`/`InterfaceMethodref`. Resolving only the
    /// method side is what made a `record`'s component getters — which arrive as
    /// `REF_getField` handles — unresolvable.
    ///
    /// The kind is part of the answer, not a detail to discard: `REF_invokeVirtual` and
    /// `REF_invokeStatic` can name the very same method and mean different calls.
    pub fn method_handle(&self, index: u16) -> Option<MethodHandleRef<'_>> {
        let (raw_kind, reference_index) =
            match self.constant_pool.get((index.checked_sub(1)?) as usize)? {
                ConstantPoolEntry::MethodHandle { reference_kind, reference_index } => {
                    (*reference_kind, *reference_index)
                }
                _ => return None,
            };
        let kind = MethodHandleKind::from_byte(raw_kind)?;
        let (class, name, descriptor) = if kind.names_a_field() {
            self.fieldref_target(reference_index)?
        } else {
            self.methodref_target(reference_index)?
        };
        Some(MethodHandleRef { kind, class, name, descriptor })
    }

    /// The descriptor a `CONSTANT_MethodType` constant names (§4.4.9) — e.g.
    /// `"(Ljava/lang/String;)Ljava/lang/String;"`. `None` if `index` isn't a `MethodType`. Used by
    /// `ldc` to materialise a `MethodType` object; `javac` never emits this, so it's exercised only
    /// through hand-written class files (the `.class` writer).
    pub fn method_type_descriptor(&self, index: u16) -> Option<&str> {
        match self.constant_pool.get((index.checked_sub(1)?) as usize)? {
            ConstantPoolEntry::MethodType { descriptor_index } => self.utf8(*descriptor_index),
            _ => None,
        }
    }

    /// Parses the class's `BootstrapMethods` attribute (§4.7.23) — the table an
    /// `invokedynamic` indexes into. Empty when the class has no indy call sites.
    pub fn bootstrap_methods(&self) -> Vec<bootstrap_methods::BootstrapMethod> {
        self.attributes
            .iter()
            .find(|a| self.utf8(a.name_index) == Some("BootstrapMethods"))
            .map(|a| bootstrap_methods::parse(&a.info))
            .unwrap_or_default()
    }

    /// The type descriptors (`Lpkg/Name;`) of the class's `RuntimeVisibleAnnotations`
    /// (§4.7.16) — the annotations written on the class itself that survive to
    /// runtime (`RetentionPolicy.RUNTIME`). Empty when the class carries none.
    /// Backs `Class.isAnnotationPresent`.
    pub fn runtime_visible_annotation_types(&self) -> Vec<String> {
        self.attributes
            .iter()
            .find(|a| self.utf8(a.name_index) == Some("RuntimeVisibleAnnotations"))
            .map(|a| annotations::type_descriptors(self, &a.info))
            .unwrap_or_default()
    }

    /// Finds a member's `Code` attribute (if any) and parses its body. Abstract
    /// and native methods have none, so this returns `None` for them.
    pub fn member_code(&self, member: &MemberInfo) -> Option<Code> {
        let attr = member
            .attributes
            .iter()
            .find(|a| self.utf8(a.name_index) == Some("Code"))?;
        code::parse(&attr.info).ok()
    }

    /// Resolves a `MethodRef` (or `InterfaceMethodRef`) constant-pool index to its
    /// target's `(name, descriptor)`. The interpreter uses this to figure out
    /// which method an `invokestatic`/`invoke*` operand names.
    pub fn methodref_name_and_type(&self, index: u16) -> Option<(&str, &str)> {
        let nt_index = match self.constant_pool.get((index.checked_sub(1)?) as usize)? {
            ConstantPoolEntry::MethodRef { name_and_type_index, .. }
            | ConstantPoolEntry::InterfaceMethodRef { name_and_type_index, .. } => {
                *name_and_type_index
            }
            _ => return None,
        };
        match self.constant_pool.get((nt_index.checked_sub(1)?) as usize)? {
            ConstantPoolEntry::NameAndType { name_index, descriptor_index } => {
                Some((self.utf8(*name_index)?, self.utf8(*descriptor_index)?))
            }
            _ => None,
        }
    }

    /// Resolves a `MethodRef`/`InterfaceMethodRef` index to its full target:
    /// `(owning class, method name, descriptor)`. The interpreter needs the class
    /// too — not just name+descriptor — so it can resolve cross-class calls
    /// through the metaspace.
    pub fn methodref_target(&self, index: u16) -> Option<(&str, &str, &str)> {
        let (class_index, nt_index) = match self.constant_pool.get((index.checked_sub(1)?) as usize)? {
            ConstantPoolEntry::MethodRef { class_index, name_and_type_index }
            | ConstantPoolEntry::InterfaceMethodRef { class_index, name_and_type_index } => {
                (*class_index, *name_and_type_index)
            }
            _ => return None,
        };
        let class = self.class_name(class_index)?;
        match self.constant_pool.get((nt_index.checked_sub(1)?) as usize)? {
            ConstantPoolEntry::NameAndType { name_index, descriptor_index } => {
                Some((class, self.utf8(*name_index)?, self.utf8(*descriptor_index)?))
            }
            _ => None,
        }
    }

    /// Resolves a `FieldRef` index to its full target: `(owning class, field name,
    /// descriptor)`. The interpreter uses this for `getfield`/`putfield` — the class
    /// names where the field is *declared* (which fixes its slot in the layout).
    pub fn fieldref_target(&self, index: u16) -> Option<(&str, &str, &str)> {
        let (class_index, nt_index) = match self.constant_pool.get((index.checked_sub(1)?) as usize)? {
            ConstantPoolEntry::FieldRef { class_index, name_and_type_index } => {
                (*class_index, *name_and_type_index)
            }
            _ => return None,
        };
        let class = self.class_name(class_index)?;
        match self.constant_pool.get((nt_index.checked_sub(1)?) as usize)? {
            ConstantPoolEntry::NameAndType { name_index, descriptor_index } => {
                Some((class, self.utf8(*name_index)?, self.utf8(*descriptor_index)?))
            }
            _ => None,
        }
    }

    /// Finds a method by `name` and `descriptor` and parses its `Code`. Used to
    /// resolve a same-class call target; `None` if there's no such method (e.g. a
    /// cross-class call, unsupported for now) or it has no body.
    pub fn method_code_by_name(&self, name: &str, descriptor: &str) -> Option<Code> {
        let member = self.methods.iter().find(|m| {
            self.utf8(m.name_index) == Some(name)
                && self.utf8(m.descriptor_index) == Some(descriptor)
        })?;
        self.member_code(member)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// True if the constant pool contains a Utf8 entry equal to `text`.
    fn has_utf8(class_file: &ClassFile, text: &str) -> bool {
        class_file
            .constant_pool
            .iter()
            .any(|e| matches!(e, ConstantPoolEntry::Utf8(s) if s.as_str() == text))
    }

    /// A `record` is the smallest class file carrying `MethodHandle`s of **two**
    /// different kinds: the `REF_invokeStatic` of `ObjectMethods.bootstrap`, and one
    /// `REF_getField` per component. Resolving only the method side — which is what the
    /// first cut did — left the getters unresolvable, because a field-kind handle points
    /// at a `Fieldref` and not a `Methodref`.
    #[test]
    fn resolves_method_handles_of_both_member_kinds() {
        let class = ClassFile::from_path("java/Point.class").unwrap();
        let bootstraps = class.bootstrap_methods();
        let bootstrap = bootstraps.first().expect("a record has a BootstrapMethods entry");

        let factory = class.method_handle(bootstrap.method_ref).expect("bootstrap handle");
        assert_eq!(factory.kind, MethodHandleKind::InvokeStatic);
        assert_eq!(factory.class, "java/lang/runtime/ObjectMethods");
        assert_eq!(factory.name, "bootstrap");
        assert!(!factory.kind.names_a_field());

        // The component getters ride along as static bootstrap arguments.
        let getters: Vec<_> =
            bootstrap.arguments.iter().filter_map(|&i| class.method_handle(i)).collect();
        assert_eq!(getters.len(), 2, "one getter per record component");
        for getter in &getters {
            assert_eq!(getter.kind, MethodHandleKind::GetField);
            assert!(getter.kind.names_a_field(), "a getField handle names a field");
            assert_eq!(getter.class, "Point");
            assert_eq!(getter.descriptor, "I"); // the *field's* descriptor, not a method's
        }
        assert_eq!(getters[0].name, "x");
        assert_eq!(getters[1].name, "y");
    }

    /// The kind byte is the fork that decides how a handle resolves, so the whole table
    /// matters — and anything outside 1..=9 is malformed, not a kind we merely don't
    /// model yet.
    #[test]
    fn method_handle_kinds_cover_the_whole_table() {
        use MethodHandleKind::*;
        let table = [
            (1, GetField),
            (2, GetStatic),
            (3, PutField),
            (4, PutStatic),
            (5, InvokeVirtual),
            (6, InvokeStatic),
            (7, InvokeSpecial),
            (8, NewInvokeSpecial),
            (9, InvokeInterface),
        ];
        for (byte, expected) in table {
            assert_eq!(MethodHandleKind::from_byte(byte), Some(expected));
        }
        // Only kinds 1–4 read a Fieldref; the rest name methods.
        assert_eq!(table.iter().filter(|(_, k)| k.names_a_field()).count(), 4);

        assert_eq!(MethodHandleKind::from_byte(0), None);
        assert_eq!(MethodHandleKind::from_byte(10), None);
    }

    #[test]
    fn reads_header_of_sample() {
        let class_file = ClassFile::from_path("java/Sample.class").unwrap();
        assert_eq!(class_file.major_version, 65); // Java 21
        assert_eq!(class_file.minor_version, 0);
        assert_eq!(class_file.access_flags, 0x0021); // ACC_PUBLIC | ACC_SUPER
        assert!(class_file.is_public());
        assert!(class_file.is_super());
        assert!(!class_file.is_final());
        // Identity: this class is "Sample", its super is java/lang/Object.
        assert_eq!(class_file.class_name(class_file.this_class), Some("Sample"));
        assert_eq!(class_file.class_name(class_file.super_class), Some("java/lang/Object"));
        // Sample implements no interfaces.
        assert!(class_file.interfaces.is_empty());
        // Members: one field "value"; two methods (<init> and getValue).
        assert_eq!(class_file.fields.len(), 1);
        assert_eq!(class_file.utf8(class_file.fields[0].name_index), Some("value"));
        assert_eq!(class_file.methods.len(), 2);
        // SourceFile is among the class-level attributes.
        assert!(class_file
            .attributes
            .iter()
            .any(|a| class_file.utf8(a.name_index) == Some("SourceFile")));
        // getValue's Code attribute parses and has non-empty bytecode.
        let get_value = class_file
            .methods
            .iter()
            .find(|m| class_file.utf8(m.name_index) == Some("getValue"))
            .expect("Sample.getValue not found");
        let code = class_file.member_code(get_value).expect("getValue has no Code");
        assert!(code.max_locals >= 1); // at least `this`
        assert!(!code.code.is_empty()); // it has bytecode
    }

    #[test]
    fn parses_constant_pool_of_sample() {
        let class_file = ClassFile::from_path("java/Sample.class").unwrap();
        // #1 is always the superclass constructor reference (javac convention).
        assert!(matches!(class_file.constant_pool[0], ConstantPoolEntry::MethodRef { .. }));
        // The pool must contain these names from the source.
        assert!(has_utf8(&class_file, "java/lang/Object"));
        assert!(has_utf8(&class_file, "Sample"));
        assert!(has_utf8(&class_file, "getValue"));
        assert!(has_utf8(&class_file, "value"));
    }
}

// -- `--strip-debug`: dropping the attributes only a debugger needs (Fase J / J6) --------

/// The attributes `jlink --strip-debug` removes. They carry no runtime semantics: the VM
/// executes identically without them. What they *do* carry is the mapping back to source —
/// which is exactly what a debugger needs, so stripping an image is what makes our own
/// `jdb` (Fase I) lose `locals` and line numbers on it. A plugin that breaks another
/// milestone, same as in the real JDK.
const DEBUG_ATTRIBUTES: [&str; 4] =
    ["LineNumberTable", "LocalVariableTable", "LocalVariableTypeTable", "SourceFile"];

/// Rewrites a class file without its debug attributes, or `None` if the bytes don't parse
/// as one.
///
/// This is surgery on the raw bytes rather than parse-and-re-emit: the constant pool is
/// copied through untouched (leaving now-unreferenced `Utf8` entries behind, exactly as the
/// real tool does — a pool entry costs little and renumbering the whole file costs a lot).
/// Only the attribute tables shrink, and every enclosing length is recomputed, including
/// `Code`'s, since the debug attributes are nested inside it.
pub fn strip_debug(bytes: &[u8]) -> Option<Vec<u8>> {
    let mut r = Cursor::new(bytes);
    let mut out = Vec::with_capacity(bytes.len());

    if r.u32()? != MAGIC {
        return None;
    }
    out.extend_from_slice(&bytes[..8]); // magic + minor + major
    r.skip(4)?; // minor + major, ya copiados

    // El pool se copia tal cual; sólo hace falta *saltearlo*, y para eso alcanza saber el
    // tamaño de cada entrada por su tag. Las entradas `long`/`double` ocupan dos slots
    // (JVMS §4.4.5), una rareza histórica que hay que respetar o se desfasa todo.
    let pool_start = r.at;
    let count = r.u16()?;
    let mut i = 1;
    while i < count {
        let tag = r.u8()?;
        let width = match tag {
            1 => { let len = r.u16()? as usize; r.skip(len)?; 0 }
            7 | 8 | 16 | 19 | 20 => 2,
            15 => 3,
            3 | 4 | 9 | 10 | 11 | 12 | 17 | 18 => 4,
            5 | 6 => 8,
            _ => return None,
        };
        r.skip(width)?;
        i += if tag == 5 || tag == 6 { 2 } else { 1 };
    }
    let names = pool_utf8(bytes, pool_start, count)?;
    out.extend_from_slice(&bytes[pool_start..r.at]);

    // access_flags, this_class, super_class, interfaces
    out.extend_from_slice(r.take(6)?);
    let interfaces = r.u16()?;
    out.extend_from_slice(&interfaces.to_be_bytes());
    out.extend_from_slice(r.take(interfaces as usize * 2)?);

    // fields y methods tienen la misma forma: cabecera de 6 bytes + tabla de atributos.
    for _ in 0..2 {
        let members = r.u16()?;
        out.extend_from_slice(&members.to_be_bytes());
        for _ in 0..members {
            out.extend_from_slice(r.take(6)?);
            strip_attributes(&mut r, &mut out, &names)?;
        }
    }
    strip_attributes(&mut r, &mut out, &names)?; // atributos de la clase
    Some(out)
}

/// Copies an attribute table, dropping the debug ones and recursing into `Code`.
fn strip_attributes(r: &mut Cursor, out: &mut Vec<u8>, names: &[Option<String>]) -> Option<()> {
    let count = r.u16()?;
    let count_at = out.len();
    out.extend_from_slice(&0u16.to_be_bytes()); // se corrige al final
    let mut kept = 0u16;
    for _ in 0..count {
        let name_index = r.u16()?;
        let length = r.u32()? as usize;
        let name = names.get(name_index as usize).and_then(Option::as_deref).unwrap_or("");
        if DEBUG_ATTRIBUTES.contains(&name) {
            r.skip(length)?;
            continue;
        }
        kept += 1;
        out.extend_from_slice(&name_index.to_be_bytes());
        if name == "Code" {
            // `Code` lleva atributos adentro, así que su longitud cambia: se emite un
            // placeholder, se copia el cuerpo, y recién entonces se sabe cuánto midió.
            let length_at = out.len();
            out.extend_from_slice(&0u32.to_be_bytes());
            let body_at = out.len();
            out.extend_from_slice(r.take(4)?); // max_stack + max_locals
            let code_length = r.u32()?;
            out.extend_from_slice(&code_length.to_be_bytes());
            out.extend_from_slice(r.take(code_length as usize)?);
            let handlers = r.u16()?;
            out.extend_from_slice(&handlers.to_be_bytes());
            out.extend_from_slice(r.take(handlers as usize * 8)?);
            strip_attributes(r, out, names)?;
            let body = (out.len() - body_at) as u32;
            out[length_at..length_at + 4].copy_from_slice(&body.to_be_bytes());
        } else {
            out.extend_from_slice(&(length as u32).to_be_bytes());
            out.extend_from_slice(r.take(length)?);
        }
    }
    out[count_at..count_at + 2].copy_from_slice(&kept.to_be_bytes());
    Some(())
}

/// The `Utf8` entries of the pool, by index — enough to recognise an attribute by name.
fn pool_utf8(bytes: &[u8], start: usize, count: u16) -> Option<Vec<Option<String>>> {
    let mut r = Cursor::new(bytes);
    r.at = start;
    r.u16()?;
    let mut names = vec![None; count as usize + 1];
    let mut i = 1usize;
    while i < count as usize {
        let tag = r.u8()?;
        match tag {
            1 => {
                let len = r.u16()? as usize;
                names[i] = String::from_utf8(r.take(len)?.to_vec()).ok();
            }
            7 | 8 | 16 | 19 | 20 => r.skip(2)?,
            15 => r.skip(3)?,
            3 | 4 | 9 | 10 | 11 | 12 | 17 | 18 => r.skip(4)?,
            5 | 6 => r.skip(8)?,
            _ => return None,
        }
        i += if tag == 5 || tag == 6 { 2 } else { 1 };
    }
    Some(names)
}

/// A minimal big-endian cursor over the raw class file — the parser's `ClassReader` returns
/// `Result`s and owns its own reading; here all that is needed is to walk and slice.
struct Cursor<'a> {
    bytes: &'a [u8],
    at: usize,
}

impl<'a> Cursor<'a> {
    fn new(bytes: &'a [u8]) -> Self {
        Cursor { bytes, at: 0 }
    }
    fn take(&mut self, n: usize) -> Option<&'a [u8]> {
        let slice = self.bytes.get(self.at..self.at + n)?;
        self.at += n;
        Some(slice)
    }
    fn skip(&mut self, n: usize) -> Option<()> {
        self.take(n).map(|_| ())
    }
    fn u8(&mut self) -> Option<u8> {
        self.take(1).map(|b| b[0])
    }
    fn u16(&mut self) -> Option<u16> {
        self.take(2).map(|b| u16::from_be_bytes([b[0], b[1]]))
    }
    fn u32(&mut self) -> Option<u32> {
        self.take(4).map(|b| u32::from_be_bytes([b[0], b[1], b[2], b[3]]))
    }
}

#[cfg(test)]
mod strip_debug_tests {
    use super::*;

    /// Una clase de la biblioteca, que trae debug de verdad (13 `LineNumberTable`).
    fn library_class() -> Vec<u8> {
        std::fs::read("KajiLibrary/java/lang/StringBuilder.class")
            .expect("KajiLibrary/java/lang/StringBuilder.class")
    }

    fn attribute_names(cf: &ClassFile) -> Vec<String> {
        let mut names: Vec<String> = cf
            .attributes
            .iter()
            .filter_map(|a| cf.utf8(a.name_index).map(str::to_string))
            .collect();
        for method in &cf.methods {
            for attribute in &method.attributes {
                if let Some(n) = cf.utf8(attribute.name_index) {
                    names.push(n.to_string());
                }
            }
        }
        names
    }

    #[test]
    fn stripping_removes_the_debug_attributes_and_keeps_the_rest() {
        let original = library_class();
        let before = ClassFile::from_bytes(&original).expect("la clase original parsea");
        assert!(attribute_names(&before).iter().any(|n| n == "SourceFile"));

        let stripped = strip_debug(&original).expect("se puede despojar");
        assert!(stripped.len() < original.len(), "despojar tiene que achicar");
        let after = ClassFile::from_bytes(&stripped).expect("la clase despojada sigue parseando");

        let names = attribute_names(&after);
        for debug in DEBUG_ATTRIBUTES {
            assert!(!names.iter().any(|n| n == debug), "quedó {debug}");
        }
        // Lo que el runtime necesita sigue estando: mismos métodos, con su `Code`.
        assert_eq!(after.methods.len(), before.methods.len());
        assert!(names.iter().any(|n| n == "Code"));
    }

    #[test]
    fn the_constant_pool_is_left_alone() {
        // Los `Utf8` de los atributos borrados quedan, sin referencias: renumerar el pool
        // costaría mucho más que los pocos bytes que ahorra, y el tool real hace lo mismo.
        let stripped = strip_debug(&library_class()).unwrap();
        let after = ClassFile::from_bytes(&stripped).unwrap();
        let pool_has_it = (1..after.constant_pool.len())
            .any(|i| after.utf8(i as u16) == Some("LineNumberTable"));
        assert!(pool_has_it, "la entrada del pool se conserva aunque el atributo no");
    }

    #[test]
    fn stripping_twice_changes_nothing_more() {
        let once = strip_debug(&library_class()).unwrap();
        let twice = strip_debug(&once).unwrap();
        assert_eq!(once, twice, "la operación es idempotente");
    }

    #[test]
    fn something_that_is_not_a_class_is_rejected() {
        assert!(strip_debug(b"no soy una clase").is_none());
        assert!(strip_debug(&[]).is_none());
    }
}
