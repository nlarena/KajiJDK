//! Pretty, boxed rendering of a `ClassFile`, plus an ASCII tree of the constant
//! pool's cross-references (each entry points at others by index).

use std::collections::HashSet;

use crate::jvm::class_file::ClassFile;
use crate::jvm::opcode;
use crate::jvm::parser::{ConstantPoolEntry, MemberInfo};

/// Renders a `ClassFile` to stdout: a header box, the flat constant pool, and
/// the constant pool drawn as a reference tree.
pub struct PrettyClassVisualizer<'a> {
    class_file: &'a ClassFile,
}

impl<'a> PrettyClassVisualizer<'a> {
    pub fn new(class_file: &'a ClassFile) -> Self {
        Self { class_file }
    }

    pub fn print(&self) {
        let cf = self.class_file;
        let pool = &cf.constant_pool;

        // 1. Header box.
        draw_box(
            "CLASS FILE",
            &[
                format!("major version : {} ({})", cf.major_version, java_version(cf.major_version)),
                format!("minor version : {}", cf.minor_version),
                format!("access flags  : {:#06X} ({})", cf.access_flags, access_flags_str(cf)),
                format!("this class    : #{} ({})", cf.this_class, cf.class_name(cf.this_class).unwrap_or("?")),
                format!("super class   : #{} ({})", cf.super_class, cf.class_name(cf.super_class).unwrap_or("—")),
                format!("interfaces    : {}", interfaces_str(cf)),
                format!("constant pool : {} entries", pool.len()),
            ],
        );
        println!();

        // 2. Flat constant pool (same content as before, boxed).
        let mut lines = Vec::new();
        for (i, entry) in pool.iter().enumerate() {
            if matches!(entry, ConstantPoolEntry::Tombstone) {
                continue;
            }
            lines.push(format!("{:>4} = {entry}", format!("#{}", i + 1)));
        }
        draw_box("CONSTANT POOL", &lines);
        println!();

        // 3. The reference tree (a forest, one tree per "root" entry).
        println!("CONSTANT POOL TREE  (indices resolved)");
        println!("──────────────────────────────────────");
        let roots = roots(pool);
        for (i, &root) in roots.iter().enumerate() {
            print_tree(pool, root, "", true, true);
            if i + 1 < roots.len() {
                println!();
            }
        }

        // 4. Fields, methods, and class-level attributes.
        print_members("FIELDS", &cf.fields, cf);
        print_members("METHODS", &cf.methods, cf);

        println!("\nCLASS ATTRIBUTES ({})", cf.attributes.len());
        println!("{}", "─".repeat(38));
        for a in &cf.attributes {
            println!("  {} ({} bytes)", cf.utf8(a.name_index).unwrap_or("?"), a.info.len());
        }
    }
}

/// Prints a `FIELDS`/`METHODS` section: name, descriptor, decoded flags and the
/// names of each member's attributes (e.g. `Code`).
fn print_members(title: &str, members: &[MemberInfo], cf: &ClassFile) {
    println!("\n{} ({})", title, members.len());
    println!("{}", "─".repeat(38));
    for m in members {
        let name = cf.utf8(m.name_index).unwrap_or("?");
        let desc = cf.utf8(m.descriptor_index).unwrap_or("?");
        let attrs: Vec<&str> = m
            .attributes
            .iter()
            .map(|a| cf.utf8(a.name_index).unwrap_or("?"))
            .collect();
        let attr_str = if attrs.is_empty() {
            String::new()
        } else {
            format!("   attrs: {}", attrs.join(", "))
        };
        println!(
            "  {:<10} {:<22} {}{}",
            name,
            desc,
            member_flags_str(m.access_flags),
            attr_str
        );
        // If the member has a Code attribute, show its frame sizes and the raw
        // bytecode bytes (to be disassembled into opcodes next).
        if let Some(code) = cf.member_code(m) {
            println!(
                "      Code: stack={} locals={} ({} bytes)",
                code.max_stack,
                code.max_locals,
                code.code.len()
            );
            for ins in opcode::disassemble(&code.code) {
                if ins.operands.is_empty() {
                    println!("      {:>4}: {}", ins.pc, ins.mnemonic);
                } else {
                    println!("      {:>4}: {:<14} {}", ins.pc, ins.mnemonic, ins.operands);
                }
            }
            for ex in &code.exception_table {
                println!(
                    "            catch [{}..{}) -> handler {} (type #{})",
                    ex.start_pc, ex.end_pc, ex.handler_pc, ex.catch_type
                );
            }
            if !code.attributes.is_empty() {
                let nested: Vec<&str> = code
                    .attributes
                    .iter()
                    .map(|a| cf.utf8(a.name_index).unwrap_or("?"))
                    .collect();
                println!("            nested: {}", nested.join(", "));
            }
        }
    }
}

/// Decodes the common field/method access flags into `[public static …]`.
fn member_flags_str(flags: u16) -> String {
    const TABLE: [(u16, &str); 6] = [
        (0x0001, "public"),
        (0x0002, "private"),
        (0x0004, "protected"),
        (0x0008, "static"),
        (0x0010, "final"),
        (0x0400, "abstract"),
    ];
    let names: Vec<&str> = TABLE
        .iter()
        .filter(|(bit, _)| flags & bit != 0)
        .map(|(_, name)| *name)
        .collect();
    if names.is_empty() {
        String::new()
    } else {
        format!("[{}]", names.join(" "))
    }
}

// --------------------------------------------------------------------------
//  Constant pool graph helpers
// --------------------------------------------------------------------------

/// 1-indexed access: spec entry `#index` is `pool[index - 1]`.
fn get(pool: &[ConstantPoolEntry], index: u16) -> &ConstantPoolEntry {
    &pool[(index - 1) as usize]
}

/// Short type name of an entry (e.g. "Methodref", "Utf8").
fn kind_name(entry: &ConstantPoolEntry) -> &'static str {
    use ConstantPoolEntry::*;
    match entry {
        Utf8(_) => "Utf8",
        Integer(_) => "Integer",
        Float(_) => "Float",
        Long(_) => "Long",
        Double(_) => "Double",
        Class { .. } => "Class",
        String { .. } => "String",
        FieldRef { .. } => "Fieldref",
        MethodRef { .. } => "Methodref",
        InterfaceMethodRef { .. } => "InterfaceMethodref",
        NameAndType { .. } => "NameAndType",
        MethodHandle { .. } => "MethodHandle",
        MethodType { .. } => "MethodType",
        Dynamic { .. } => "Dynamic",
        InvokeDynamic { .. } => "InvokeDynamic",
        Module { .. } => "Module",
        Package { .. } => "Package",
        Tombstone => "(tombstone)",
    }
}

/// The constant pool indices this entry references (its children in the tree).
fn children(entry: &ConstantPoolEntry) -> Vec<u16> {
    use ConstantPoolEntry::*;
    match entry {
        Class { name_index } => vec![*name_index],
        String { string_index } => vec![*string_index],
        FieldRef { class_index, name_and_type_index }
        | MethodRef { class_index, name_and_type_index }
        | InterfaceMethodRef { class_index, name_and_type_index } => {
            vec![*class_index, *name_and_type_index]
        }
        NameAndType { name_index, descriptor_index } => vec![*name_index, *descriptor_index],
        MethodType { descriptor_index } => vec![*descriptor_index],
        MethodHandle { reference_index, .. } => vec![*reference_index],
        Dynamic { name_and_type_index, .. } | InvokeDynamic { name_and_type_index, .. } => {
            vec![*name_and_type_index]
        }
        Module { name_index } | Package { name_index } => vec![*name_index],
        _ => vec![], // Utf8, Integer, Float, Long, Double, Tombstone: leaves
    }
}

/// A node's label: `#n  Kind` plus its literal value for leaf-ish entries.
fn node_label(pool: &[ConstantPoolEntry], index: u16) -> String {
    // Note: no `use ConstantPoolEntry::*` here — the `String` variant would
    // shadow the std `String` type and break `String::new()` below.
    let entry = get(pool, index);
    let detail = match entry {
        ConstantPoolEntry::Utf8(s) => format!(" \"{s}\""),
        ConstantPoolEntry::Integer(v) => format!(" {v}"),
        ConstantPoolEntry::Float(v) => format!(" {v}"),
        ConstantPoolEntry::Long(v) => format!(" {v}"),
        ConstantPoolEntry::Double(v) => format!(" {v}"),
        _ => String::new(),
    };
    format!("#{index}  {}{detail}", kind_name(entry))
}

/// "Roots" = entries that no other entry references. They are the tops of the
/// trees (the *Ref entries and the stand-alone Utf8s used by methods/attrs).
fn roots(pool: &[ConstantPoolEntry]) -> Vec<u16> {
    let mut referenced = HashSet::new();
    for entry in pool {
        for child in children(entry) {
            referenced.insert(child);
        }
    }
    let mut result = Vec::new();
    for (i, entry) in pool.iter().enumerate() {
        let index = (i + 1) as u16;
        if !matches!(entry, ConstantPoolEntry::Tombstone) && !referenced.contains(&index) {
            result.push(index);
        }
    }
    result
}

/// Recursively prints the subtree rooted at `index` with box-drawing branches.
fn print_tree(pool: &[ConstantPoolEntry], index: u16, prefix: &str, is_last: bool, is_root: bool) {
    let connector = if is_root {
        ""
    } else if is_last {
        "└── "
    } else {
        "├── "
    };
    println!("{prefix}{connector}{}", node_label(pool, index));

    let kids = children(get(pool, index));
    let child_prefix = if is_root {
        String::new()
    } else if is_last {
        format!("{prefix}    ")
    } else {
        format!("{prefix}│   ")
    };
    let n = kids.len();
    for (i, &child) in kids.iter().enumerate() {
        print_tree(pool, child, &child_prefix, i + 1 == n, false);
    }
}

// --------------------------------------------------------------------------
//  Presentation helpers
// --------------------------------------------------------------------------

/// Decodes the class access flags into a human list (`ACC_PUBLIC, …`) using the
/// boolean query methods on `ClassFile`.
fn access_flags_str(cf: &ClassFile) -> String {
    let flags: [(bool, &str); 9] = [
        (cf.is_public(), "ACC_PUBLIC"),
        (cf.is_final(), "ACC_FINAL"),
        (cf.is_super(), "ACC_SUPER"),
        (cf.is_interface(), "ACC_INTERFACE"),
        (cf.is_abstract(), "ACC_ABSTRACT"),
        (cf.is_synthetic(), "ACC_SYNTHETIC"),
        (cf.is_annotation(), "ACC_ANNOTATION"),
        (cf.is_enum(), "ACC_ENUM"),
        (cf.is_module(), "ACC_MODULE"),
    ];
    let names: Vec<&str> = flags
        .iter()
        .filter(|(on, _)| *on)
        .map(|(_, name)| *name)
        .collect();
    if names.is_empty() {
        "—".to_string()
    } else {
        names.join(", ")
    }
}

/// Lists the implemented interfaces by resolved name, or "(none)".
fn interfaces_str(cf: &ClassFile) -> String {
    if cf.interfaces.is_empty() {
        return "(none)".to_string();
    }
    cf.interfaces
        .iter()
        .map(|&i| cf.class_name(i).unwrap_or("?"))
        .collect::<Vec<_>>()
        .join(", ")
}

/// Maps a class file major version to its Java release name (49 = Java 5 …).
fn java_version(major: u16) -> String {
    if major >= 49 {
        format!("Java {}", major - 44)
    } else {
        format!("major {major}")
    }
}

/// Draws a titled box sized to its content.
fn draw_box(title: &str, lines: &[String]) {
    let width = std::iter::once(title.chars().count())
        .chain(lines.iter().map(|l| l.chars().count()))
        .max()
        .unwrap_or(0);
    let bar = "─".repeat(width + 2);
    println!("┌{bar}┐");
    println!("│ {title:<width$} │");
    println!("├{bar}┤");
    for line in lines {
        println!("│ {line:<width$} │");
    }
    println!("└{bar}┘");
}
