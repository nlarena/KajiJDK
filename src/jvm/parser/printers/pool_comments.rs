//! Resolves constant-pool indices into javap's `// …` comment text. Used both
//! by the `Constant pool:` listing and by the per-instruction `#index`
//! comments in the bytecode disassembly.

use crate::jvm::class_file::ClassFile;
use crate::jvm::parser::constant_pool::escape;
use crate::jvm::parser::float_to_decimal;
use crate::jvm::parser::ConstantPoolEntry;

/// Prints the `Constant pool:` block, right-aligning `#N` and appending the
/// resolved `// …` comment when there is one.
pub fn print_listing(cf: &ClassFile) {
    crate::pln!("Constant pool:");
    // javap right-aligns `#N` to the width of `constant_pool_count` (= entries
    // + 1), not the largest index — they differ at digit boundaries (9→10, …).
    let index_width = format!("#{}", cf.constant_pool.len() + 1).len() + 2;
    for (i, entry) in cf.constant_pool.iter().enumerate() {
        if matches!(entry, ConstantPoolEntry::Tombstone) {
            continue;
        }
        let index = (i + 1) as u16;
        let left = format!("{:>index_width$} = {}", format!("#{index}"), entry);
        // `// …` is shown for any entry that carries a comment, even when the
        // value is empty (then `pln!` trims it down to just `//`).
        match entry_comment(cf, index) {
            Some(comment) => crate::pln!("{left:<42}// {comment}"),
            None => crate::pln!("{left}"),
        }
    }
}

/// The `// Method …` text shown on a bytecode instruction whose operand is a
/// `#index`. Public because the member dump needs it for the disassembly.
pub fn instruction_comment(cf: &ClassFile, index: u16) -> String {
    // Loadable literal constants (`ldc`): int/long/float/double/String.
    let literal = constant_value_text(cf, index);
    if !literal.is_empty() {
        return literal;
    }
    let (word, text) = match get(cf, index) {
        Some(ConstantPoolEntry::MethodRef { .. }) => ("Method", ref_text(cf, index)),
        Some(ConstantPoolEntry::FieldRef { .. }) => ("Field", ref_text(cf, index)),
        Some(ConstantPoolEntry::InterfaceMethodRef { .. }) => {
            ("InterfaceMethod", ref_text(cf, index))
        }
        Some(ConstantPoolEntry::Class { name_index }) => {
            ("class", class_comment_name(cf.utf8(*name_index).unwrap_or("")))
        }
        Some(ConstantPoolEntry::InvokeDynamic { .. }) => ("InvokeDynamic", indy_text(cf, index)),
        _ => ("", String::new()),
    };
    if text.is_empty() {
        String::new()
    } else {
        format!("{word} {text}")
    }
}

/// Renders a loadable literal constant as javap does — for an `ldc` operand or a
/// field's `ConstantValue`: `int 5`, `long 99l`, `float 1.5f`, `double 3.14d`,
/// `String hi`. Empty for any non-literal entry.
pub fn constant_value_text(cf: &ClassFile, index: u16) -> String {
    match get(cf, index) {
        Some(ConstantPoolEntry::Integer(v)) => format!("int {v}"),
        Some(ConstantPoolEntry::Float(v)) => format!("float {}f", float_to_decimal::java_float(*v)),
        Some(ConstantPoolEntry::Long(v)) => format!("long {v}l"),
        Some(ConstantPoolEntry::Double(v)) => format!("double {}d", float_to_decimal::java_double(*v)),
        Some(ConstantPoolEntry::String { string_index }) => {
            format!("String {}", escape(cf.utf8(*string_index).unwrap_or("")))
        }
        _ => String::new(),
    }
}

/// 1-indexed constant pool access.
fn get(cf: &ClassFile, index: u16) -> Option<&ConstantPoolEntry> {
    cf.constant_pool.get((index.checked_sub(1)?) as usize)
}

/// A `Class` constant's name as javap shows it in comments — quoted when it is
/// not a plain binary name (see [`crate::jvm::parser::constant_pool::comment_quote`]).
fn class_comment_name(name: &str) -> String {
    crate::jvm::parser::constant_pool::comment_quote(name)
}

/// The `// …` text for a constant pool entry. `Some` for entries that carry a
/// comment (even when the resolved value is empty — javap still prints `//`);
/// `None` for entries with no comment (Utf8, the numeric literals, …).
///
/// Variants are qualified on purpose: a `use ConstantPoolEntry::*` would bring
/// the `String` variant in and shadow the std `String` type.
fn entry_comment(cf: &ClassFile, index: u16) -> Option<String> {
    Some(match get(cf, index)? {
        ConstantPoolEntry::Class { name_index } => {
            class_comment_name(cf.utf8(*name_index).unwrap_or(""))
        }
        ConstantPoolEntry::String { string_index } => escape(cf.utf8(*string_index).unwrap_or("")),
        ConstantPoolEntry::NameAndType { name_index, descriptor_index } => {
            name_and_type(cf, *name_index, *descriptor_index)
        }
        ConstantPoolEntry::FieldRef { .. }
        | ConstantPoolEntry::MethodRef { .. }
        | ConstantPoolEntry::InterfaceMethodRef { .. } => match ref_parts(cf, index) {
            Some((class, nt)) => format!("{class}.{nt}"),
            None => String::new(),
        },
        ConstantPoolEntry::MethodHandle { .. } => method_handle_text(cf, index),
        // MethodType's comment carries a leading space in javap: `//  ()V`.
        ConstantPoolEntry::MethodType { descriptor_index } => {
            format!(" {}", cf.utf8(*descriptor_index).unwrap_or(""))
        }
        ConstantPoolEntry::InvokeDynamic { .. } | ConstantPoolEntry::Dynamic { .. } => {
            indy_text(cf, index)
        }
        // The module-system tags carry the referenced name as their comment
        // (module names are dotted → quoted; package names use slashes).
        ConstantPoolEntry::Module { name_index } | ConstantPoolEntry::Package { name_index } => {
            class_comment_name(cf.utf8(*name_index).unwrap_or(""))
        }
        _ => return None,
    })
}

/// javap's name for a `MethodHandle` reference kind (JVMS §4.4.8).
fn ref_kind_name(kind: u8) -> &'static str {
    match kind {
        1 => "REF_getField",
        2 => "REF_getStatic",
        3 => "REF_putField",
        4 => "REF_putStatic",
        5 => "REF_invokeVirtual",
        6 => "REF_invokeStatic",
        7 => "REF_invokeSpecial",
        8 => "REF_newInvokeSpecial",
        9 => "REF_invokeInterface",
        _ => "REF_unknown",
    }
}

/// Resolves a `NameAndType` index to `name:descriptor`.
fn nt_text(cf: &ClassFile, nt_index: u16) -> String {
    match get(cf, nt_index) {
        Some(ConstantPoolEntry::NameAndType { name_index, descriptor_index }) => {
            name_and_type(cf, *name_index, *descriptor_index)
        }
        _ => String::new(),
    }
}

/// `#bsm:name:descriptor` for an `InvokeDynamic`/`Dynamic` entry — its `// …`.
fn indy_text(cf: &ClassFile, index: u16) -> String {
    match get(cf, index) {
        Some(
            ConstantPoolEntry::InvokeDynamic { bootstrap_method_attr_index, name_and_type_index }
            | ConstantPoolEntry::Dynamic { bootstrap_method_attr_index, name_and_type_index },
        ) => format!("#{}:{}", bootstrap_method_attr_index, nt_text(cf, *name_and_type_index)),
        _ => String::new(),
    }
}

/// `REF_<kind> <class>.<name>:<descriptor>` for a `MethodHandle`. Public because
/// the `BootstrapMethods` block renders bootstrap-method handles this same way.
/// Note: unlike instruction comments, the class prefix is never elided.
pub fn method_handle_text(cf: &ClassFile, index: u16) -> String {
    match get(cf, index) {
        Some(ConstantPoolEntry::MethodHandle { reference_kind, reference_index }) => {
            let kind = ref_kind_name(*reference_kind);
            match ref_parts(cf, *reference_index) {
                Some((class, nt)) => format!("{kind} {class}.{nt}"),
                None => kind.to_string(),
            }
        }
        _ => String::new(),
    }
}

/// Renders one `BootstrapMethods` static argument the way javap does: a
/// `MethodType` as its bare descriptor, a `MethodHandle` as `REF_… …`, and any
/// other loadable constant via the instruction-comment rendering.
pub fn bootstrap_arg_text(cf: &ClassFile, index: u16) -> String {
    match get(cf, index) {
        Some(ConstantPoolEntry::MethodType { descriptor_index }) => {
            cf.utf8(*descriptor_index).unwrap_or("").to_string()
        }
        Some(ConstantPoolEntry::MethodHandle { .. }) => method_handle_text(cf, index),
        // A class argument shows the bare name (no `class` prefix, unlike a
        // bytecode operand comment).
        Some(ConstantPoolEntry::Class { name_index }) => {
            class_comment_name(cf.utf8(*name_index).unwrap_or(""))
        }
        // A string argument shows the raw value (no `String` prefix, no quotes).
        Some(ConstantPoolEntry::String { string_index }) => {
            escape(cf.utf8(*string_index).unwrap_or(""))
        }
        // Numeric arguments show the bare value (no `int`/`long`/… type word that
        // the `ldc` comment carries) — keeping only the wide-literal suffix.
        Some(ConstantPoolEntry::Integer(v)) => format!("{v}"),
        Some(ConstantPoolEntry::Long(v)) => format!("{v}l"),
        Some(ConstantPoolEntry::Float(v)) => format!("{}f", float_to_decimal::java_float(*v)),
        Some(ConstantPoolEntry::Double(v)) => format!("{}d", float_to_decimal::java_double(*v)),
        _ => instruction_comment(cf, index),
    }
}

/// `name:descriptor`, quoting special names like `<init>` (as javap does).
fn name_and_type(cf: &ClassFile, name_index: u16, descriptor_index: u16) -> String {
    let name = cf.utf8(name_index).unwrap_or("?");
    let desc = cf.utf8(descriptor_index).unwrap_or("?");
    let name = if name.starts_with('<') {
        format!("\"{name}\"")
    } else {
        name.to_string()
    };
    format!("{name}:{desc}")
}

/// Splits a Field/Method/InterfaceMethod ref into (class name, name:desc).
fn ref_parts(cf: &ClassFile, index: u16) -> Option<(String, String)> {
    use ConstantPoolEntry::*;
    let (class_index, nt_index) = match get(cf, index)? {
        FieldRef { class_index, name_and_type_index }
        | MethodRef { class_index, name_and_type_index }
        | InterfaceMethodRef { class_index, name_and_type_index } => {
            (*class_index, *name_and_type_index)
        }
        _ => return None,
    };
    // An array-typed owner (e.g. `[LColor;`) is quoted, like any class name.
    let class = class_comment_name(cf.class_name(class_index)?);
    let nt = match get(cf, nt_index)? {
        NameAndType { name_index, descriptor_index } => {
            name_and_type(cf, *name_index, *descriptor_index)
        }
        _ => return None,
    };
    Some((class, nt))
}

/// For an instruction ref: drop the class prefix when it is the current class.
fn ref_text(cf: &ClassFile, index: u16) -> String {
    match ref_parts(cf, index) {
        Some((class, nt)) => {
            let this = cf.class_name(cf.this_class).unwrap_or("");
            if class == this {
                nt
            } else {
                format!("{class}.{nt}")
            }
        }
        None => String::new(),
    }
}
