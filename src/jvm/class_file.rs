//! The in-memory model of a parsed `.class` file.

use super::parser::attributes::bootstrap_methods;
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
        // The reader borrows those bytes; it lives only for this function.
        let mut reader = ClassReader::new(&bytes);

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

    /// Parses the class's `BootstrapMethods` attribute (§4.7.23) — the table an
    /// `invokedynamic` indexes into. Empty when the class has no indy call sites.
    pub fn bootstrap_methods(&self) -> Vec<bootstrap_methods::BootstrapMethod> {
        self.attributes
            .iter()
            .find(|a| self.utf8(a.name_index) == Some("BootstrapMethods"))
            .map(|a| bootstrap_methods::parse(&a.info))
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
