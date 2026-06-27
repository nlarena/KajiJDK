//! Helpers shared by the `brief` and `verbose` javap dumps: source file, class
//! declaration, humanized signatures, access-flag rendering and descriptors.

use super::Visibility;
use crate::jvm::class_file::ClassFile;
use crate::jvm::parser::attributes::{exceptions, signature};
use crate::jvm::parser::MemberInfo;

/// The ` throws X, Y` clause from a method's `Exceptions` attribute (dotted
/// names), or `""` if it declares none.
fn throws_clause(cf: &ClassFile, m: &MemberInfo) -> String {
    let Some(attr) = m.attributes.iter().find(|a| cf.utf8(a.name_index) == Some("Exceptions"))
    else {
        return String::new();
    };
    let names: Vec<String> = exceptions::parse(&attr.info)
        .iter()
        .map(|&i| cf.class_name(i).unwrap_or("?").replace('/', "."))
        .collect();
    if names.is_empty() {
        String::new()
    } else {
        format!(" throws {}", names.join(", "))
    }
}

/// The `Signature` attribute string on a member (field/method), if present.
pub fn member_signature<'a>(cf: &'a ClassFile, m: &MemberInfo) -> Option<&'a str> {
    let attr = m.attributes.iter().find(|a| cf.utf8(a.name_index) == Some("Signature"))?;
    cf.utf8(signature::index(&attr.info)?)
}

/// The class-level `Signature` attribute string, if present.
pub fn class_signature(cf: &ClassFile) -> Option<&str> {
    let attr = cf.attributes.iter().find(|a| cf.utf8(a.name_index) == Some("Signature"))?;
    cf.utf8(signature::index(&attr.info)?)
}

/// Fields then methods (tagged with `is_method`), filtered by the visibility
/// level. Both the brief and verbose dumps iterate members this same way.
pub fn visible_members(cf: &ClassFile, visibility: Visibility) -> Vec<(&MemberInfo, bool)> {
    cf.fields
        .iter()
        .map(|m| (m, false))
        .chain(cf.methods.iter().map(|m| (m, true)))
        .filter(|(m, _)| visibility.is_visible(m.access_flags))
        .collect()
}

/// The `.java` recorded in the class's `SourceFile` attribute, if present.
pub fn source_file(cf: &ClassFile) -> Option<&str> {
    let attr = cf
        .attributes
        .iter()
        .find(|a| cf.utf8(a.name_index) == Some("SourceFile"))?;
    if attr.info.len() < 2 {
        return None;
    }
    let index = ((attr.info[0] as u16) << 8) | attr.info[1] as u16;
    cf.utf8(index)
}

/// `public class Foo implements A,B` (or `interface X extends Y` for an
/// interface). javap joins the interface names with a bare comma, no space.
pub fn class_declaration(cf: &ClassFile, verbose: bool) -> String {
    // A module-info renders `module <name>@<version>` from its Module attribute,
    // not a class/interface declaration.
    if cf.access_flags & 0x8000 != 0 {
        if let Some(decl) = crate::jvm::parser::attributes::module::declaration(cf) {
            return decl;
        }
    }
    let this = cf.class_name(cf.this_class).unwrap_or("?").replace('/', ".");
    // With a class-level `Signature`, javap renders the whole declaration from
    // it (type parameters + generic super/interfaces) instead of the descriptor.
    if let Some(sig) = class_signature(cf) {
        let is_interface = cf.access_flags & 0x0200 != 0;
        return format!(
            "{}{}{}",
            class_modifiers(cf.access_flags),
            this,
            signature::class_clause(sig, is_interface, verbose)
        );
    }
    let is_interface = cf.access_flags & 0x0200 != 0;
    let mut decl = format!("{}{}", class_modifiers(cf.access_flags), this);
    // A non-interface class shows `extends Super` unless the super is Object.
    if !is_interface {
        let super_name = cf.class_name(cf.super_class).unwrap_or("").replace('/', ".");
        if !super_name.is_empty() && super_name != "java.lang.Object" {
            decl.push_str(&format!(" extends {super_name}"));
        }
    }
    if !cf.interfaces.is_empty() {
        let names: Vec<String> = cf
            .interfaces
            .iter()
            .map(|&i| cf.class_name(i).unwrap_or("?").replace('/', "."))
            .collect();
        let keyword = if is_interface { "extends" } else { "implements" };
        decl.push_str(&format!(" {keyword} {}", names.join(",")));
    }
    decl
}

/// Humanized member signature, e.g. `public static int add(int, int)`.
pub fn signature(
    cf: &ClassFile,
    m: &MemberInfo,
    name: &str,
    desc: &str,
    is_method: bool,
    verbose: bool,
) -> String {
    let mut mods = member_modifiers(m.access_flags, is_method);
    let sig = member_signature(cf, m);
    if !is_method {
        // field: `<mods><type> <name>` — type from the generic signature if any.
        let ty = match sig {
            Some(s) => signature::field_type(s),
            None => parse_type(desc.as_bytes(), 0).0,
        };
        return format!("{mods}{ty} {name}");
    }
    // `<clinit>` renders as `{}` and never carries a throws clause.
    if name == "<clinit>" {
        return format!("{mods}{{}}");
    }
    let varargs = m.access_flags & 0x0080 != 0;
    // Constructors render with the fully-qualified (dotted) class name; a
    // generic constructor uses its signature for the parameter types.
    if name == "<init>" {
        let class = cf.class_name(cf.this_class).unwrap_or("?").replace('/', ".");
        let base = match sig {
            Some(s) => signature::constructor_decl(s, &class, verbose, varargs),
            None => {
                let (mut args, _) = parse_method_descriptor(desc);
                varargs_last(&mut args, varargs);
                format!("{class}({})", args.join(", "))
            }
        };
        return format!("{mods}{base}{}", throws_clause(cf, m));
    }
    // A concrete (non-static, non-abstract, non-private) interface instance
    // method is a `default` method — javap shows the keyword, though it is no
    // access flag. Placed after the visibility modifiers.
    if cf.access_flags & 0x0200 != 0 && m.access_flags & (0x0008 | 0x0400 | 0x0002) == 0 {
        mods.push_str("default ");
    }
    let base = match sig {
        Some(s) => signature::method_decl(s, name, verbose, varargs),
        None => {
            let (mut args, ret) = parse_method_descriptor(desc);
            varargs_last(&mut args, varargs);
            format!("{ret} {name}({})", args.join(", "))
        }
    };
    // Throws: from the signature's `^…` if present, otherwise the Exceptions attr.
    let sig_throws = sig.map(signature::method_throws).unwrap_or_default();
    let throws = if sig_throws.is_empty() {
        throws_clause(cf, m)
    } else {
        format!(" throws {}", sig_throws.join(", "))
    };
    format!("{mods}{base}{throws}")
}

/// In a varargs method, renders the last parameter's trailing `[]` as `...`.
fn varargs_last(args: &mut [String], varargs: bool) {
    if !varargs {
        return;
    }
    if let Some(last) = args.last_mut() {
        if let Some(base) = last.strip_suffix("[]") {
            *last = format!("{base}...");
        }
    }
}

// -- access flags --------------------------------------------------------

pub fn class_modifiers(flags: u16) -> String {
    let mut s = String::new();
    if flags & 0x0001 != 0 {
        s.push_str("public ");
    }
    if flags & 0x0010 != 0 {
        s.push_str("final ");
    }
    if flags & 0x0200 != 0 {
        s.push_str("interface ");
    } else {
        // javap renders an enum (ACC_ENUM) as a plain `class`, not `enum`.
        if flags & 0x0400 != 0 {
            s.push_str("abstract ");
        }
        s.push_str("class ");
    }
    s
}

pub fn class_flag_names(flags: u16) -> Vec<&'static str> {
    const TABLE: [(u16, &str); 9] = [
        (0x0001, "ACC_PUBLIC"),
        (0x0010, "ACC_FINAL"),
        (0x0020, "ACC_SUPER"),
        (0x0200, "ACC_INTERFACE"),
        (0x0400, "ACC_ABSTRACT"),
        (0x1000, "ACC_SYNTHETIC"),
        (0x2000, "ACC_ANNOTATION"),
        (0x4000, "ACC_ENUM"),
        (0x8000, "ACC_MODULE"),
    ];
    TABLE.iter().filter(|(b, _)| flags & b != 0).map(|(_, n)| *n).collect()
}

pub fn member_flag_names(flags: u16, is_method: bool) -> Vec<&'static str> {
    // Bits 0x40 and 0x80 mean different things for fields vs methods, so the two
    // need distinct tables. Order is ascending by bit, matching javap.
    const METHOD: &[(u16, &str)] = &[
        (0x0001, "ACC_PUBLIC"),
        (0x0002, "ACC_PRIVATE"),
        (0x0004, "ACC_PROTECTED"),
        (0x0008, "ACC_STATIC"),
        (0x0010, "ACC_FINAL"),
        (0x0020, "ACC_SYNCHRONIZED"),
        (0x0040, "ACC_BRIDGE"),
        (0x0080, "ACC_VARARGS"),
        (0x0100, "ACC_NATIVE"),
        (0x0400, "ACC_ABSTRACT"),
        (0x0800, "ACC_STRICT"),
        (0x1000, "ACC_SYNTHETIC"),
    ];
    const FIELD: &[(u16, &str)] = &[
        (0x0001, "ACC_PUBLIC"),
        (0x0002, "ACC_PRIVATE"),
        (0x0004, "ACC_PROTECTED"),
        (0x0008, "ACC_STATIC"),
        (0x0010, "ACC_FINAL"),
        (0x0040, "ACC_VOLATILE"),
        (0x0080, "ACC_TRANSIENT"),
        (0x1000, "ACC_SYNTHETIC"),
        (0x4000, "ACC_ENUM"),
    ];
    let table = if is_method { METHOD } else { FIELD };
    table.iter().filter(|(b, _)| flags & b != 0).map(|(_, n)| *n).collect()
}

pub fn member_modifiers(flags: u16, is_method: bool) -> String {
    // Bits 0x40/0x80 differ between fields (volatile/transient) and methods
    // (bridge/varargs, which aren't shown as source modifiers).
    let table: &[(u16, &str)] = if is_method {
        &[
            (0x0001, "public "),
            (0x0002, "private "),
            (0x0004, "protected "),
            (0x0008, "static "),
            (0x0010, "final "),
            (0x0400, "abstract "),
            (0x0020, "synchronized "),
            (0x0100, "native "),
        ]
    } else {
        &[
            (0x0001, "public "),
            (0x0002, "private "),
            (0x0004, "protected "),
            (0x0008, "static "),
            (0x0010, "final "),
            (0x0040, "volatile "),
            (0x0080, "transient "),
        ]
    };
    let mut s = String::new();
    for (bit, kw) in table {
        if flags & bit != 0 {
            s.push_str(kw);
        }
    }
    s
}

/// Modifiers javap shows for an `InnerClasses` entry, with its trailing space.
/// A distinct set from class/member modifiers: `interface`, `enum`, `synthetic`
/// and `annotation` are never shown here, and the order is fixed.
pub fn inner_class_modifiers(flags: u16) -> String {
    let mut s = String::new();
    let is_interface = flags & 0x0200 != 0;
    for (bit, kw) in [
        (0x0001u16, "public "),
        (0x0002, "private "),
        (0x0004, "protected "),
        (0x0008, "static "),
        (0x0010, "final "),
        (0x0400, "abstract "),
    ] {
        // `abstract` is implicit for interfaces; javap omits it (and `interface`).
        if bit == 0x0400 && is_interface {
            continue;
        }
        if flags & bit != 0 {
            s.push_str(kw);
        }
    }
    s
}

// -- descriptors ---------------------------------------------------------

/// Parses one field-type descriptor at `i`, returning (java type, bytes used).
pub fn parse_type(desc: &[u8], i: usize) -> (String, usize) {
    match desc.get(i).copied().unwrap_or(b'?') {
        b'B' => ("byte".into(), 1),
        b'C' => ("char".into(), 1),
        b'D' => ("double".into(), 1),
        b'F' => ("float".into(), 1),
        b'I' => ("int".into(), 1),
        b'J' => ("long".into(), 1),
        b'S' => ("short".into(), 1),
        b'Z' => ("boolean".into(), 1),
        b'V' => ("void".into(), 1),
        b'[' => {
            let (inner, n) = parse_type(desc, i + 1);
            (format!("{inner}[]"), n + 1)
        }
        b'L' => {
            let mut j = i + 1;
            while j < desc.len() && desc[j] != b';' {
                j += 1;
            }
            let name = std::str::from_utf8(&desc[i + 1..j]).unwrap_or("?").replace('/', ".");
            (name, j - i + 1)
        }
        _ => ("?".into(), 1),
    }
}

/// Splits a method descriptor `(args)ret` into (arg types, return type).
pub fn parse_method_descriptor(desc: &str) -> (Vec<String>, String) {
    let b = desc.as_bytes();
    let mut args = Vec::new();
    let mut i = 1; // skip '('
    while i < b.len() && b[i] != b')' {
        let (t, n) = parse_type(b, i);
        args.push(t);
        i += n;
    }
    let (ret, _) = parse_type(b, i + 1); // skip ')'
    (args, ret)
}

/// Number of declared parameters in a method descriptor. Like javap's
/// `args_size`, long/double count once (not as two slots).
pub fn arg_count(desc: &str) -> usize {
    let b = desc.as_bytes();
    let mut i = 1;
    let mut count = 0;
    while i < b.len() && b[i] != b')' {
        let (_, n) = parse_type(b, i);
        count += 1;
        i += n;
    }
    count
}
