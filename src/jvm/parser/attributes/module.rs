//! The JPMS module-system attributes (JVMS §4.7.25–27), found only in a
//! `module-info.class`: `Module` (the descriptor — `requires`/`exports`/`opens`/
//! `uses`/`provides`), `ModulePackages`, `ModuleMainClass`, plus the JDK-internal
//! `ModuleTarget` and `ModuleHashes` that jlink emits.
//!
//! javap dumps these as raw `#index,flags` rows with a resolved `// …` comment,
//! the `//` column sitting at `indent + 40`.

use super::super::reader::ClassReader;
use crate::jvm::class_file::ClassFile;
use crate::jvm::parser::constant_pool::comment_quote;
use crate::jvm::parser::ConstantPoolEntry;

/// The class-header declaration for a module: `module java.base@25.0.3`
/// (`open module …` when the module is open). Reads the `Module` attribute.
pub fn declaration(cf: &ClassFile) -> Option<String> {
    let info = attribute(cf, "Module")?;
    let mut r = ClassReader::new(info);
    let name_index = r.read_u16().ok()?;
    let flags = r.read_u16().ok()?;
    let version_index = r.read_u16().ok()?;
    let name = module_name(cf, name_index);
    let open = if flags & 0x0020 != 0 { "open " } else { "" };
    let mut decl = format!("{open}module {name}");
    if version_index != 0 {
        if let Some(v) = cf.utf8(version_index) {
            decl.push('@');
            decl.push_str(v);
        }
    }
    Some(decl)
}

/// Prints the `Module:` block (the descriptor): the module name/flags/version,
/// then the `requires`/`exports`/`opens`/`uses`/`provides` directive groups.
pub fn print(cf: &ClassFile, info: &[u8]) {
    let mut r = ClassReader::new(info);
    let Ok(name_index) = r.read_u16() else { return };
    let Ok(flags) = r.read_u16() else { return };
    let Ok(version_index) = r.read_u16() else { return };
    crate::pln!("Module:");
    emit(2, &format!("#{name_index},{flags}"), &comment_quote(module_name(cf, name_index)));
    let version = if version_index == 0 { "" } else { cf.utf8(version_index).unwrap_or("") };
    emit(2, &format!("#{version_index}"), version);

    // requires: { module_index, flags, version_index }
    let Ok(requires) = r.read_u16() else { return };
    emit(2, &requires.to_string(), "requires");
    for _ in 0..requires {
        let (Ok(idx), Ok(f), Ok(_ver)) = (r.read_u16(), r.read_u16(), r.read_u16()) else {
            return;
        };
        emit(4, &format!("#{idx},{f}"), &comment_quote(module_name(cf, idx)));
    }

    // exports / opens share a layout: { package_index, flags, [to module…] }
    if exports_like(cf, &mut r, "exports").is_none() {
        return;
    }
    if exports_like(cf, &mut r, "opens").is_none() {
        return;
    }

    // uses: just a list of service classes.
    let Ok(uses) = r.read_u16() else { return };
    emit(2, &uses.to_string(), "uses");
    for _ in 0..uses {
        let Ok(idx) = r.read_u16() else { return };
        emit(4, &format!("#{idx}"), &comment_quote(class_name(cf, idx)));
    }

    // provides: { service_class, [with implementation_class…] }
    let Ok(provides) = r.read_u16() else { return };
    emit(2, &provides.to_string(), "provides");
    for _ in 0..provides {
        let (Ok(idx), Ok(with_count)) = (r.read_u16(), r.read_u16()) else { return };
        emit(
            4,
            &format!("#{idx}"),
            &format!("{} with ... {with_count}", comment_quote(class_name(cf, idx))),
        );
        for _ in 0..with_count {
            let Ok(w) = r.read_u16() else { return };
            emit(6, &format!("#{w}"), &format!("... with {}", comment_quote(class_name(cf, w))));
        }
    }
}

/// Renders one `exports`/`opens` group (they have the identical structure). Each
/// entry is a package plus an optional `to <module>…` target list.
fn exports_like(cf: &ClassFile, r: &mut ClassReader, label: &str) -> Option<()> {
    let count = r.read_u16().ok()?;
    emit(2, &count.to_string(), label);
    for _ in 0..count {
        let pkg = r.read_u16().ok()?;
        let flags = r.read_u16().ok()?;
        let to_count = r.read_u16().ok()?;
        let mut comment = comment_quote(package_name(cf, pkg));
        if to_count != 0 {
            comment.push_str(&format!(" to ... {to_count}"));
        }
        emit(4, &format!("#{pkg},{flags}"), &comment);
        for _ in 0..to_count {
            let tgt = r.read_u16().ok()?;
            emit(6, &format!("#{tgt}"), &format!("... to {}", comment_quote(module_name(cf, tgt))));
        }
    }
    Some(())
}

/// `ModulePackages`: the list of every package in the module (dotted names).
pub fn print_packages(cf: &ClassFile, info: &[u8]) {
    let mut r = ClassReader::new(info);
    let Ok(count) = r.read_u16() else { return };
    crate::pln!("ModulePackages:");
    for _ in 0..count {
        let Ok(idx) = r.read_u16() else { return };
        emit(2, &format!("#{idx}"), &package_name(cf, idx).replace('/', "."));
    }
}

/// `ModuleMainClass`: the module's entry-point class (dotted name).
pub fn print_main_class(cf: &ClassFile, info: &[u8]) {
    if info.len() < 2 {
        return;
    }
    let idx = u16::from_be_bytes([info[0], info[1]]);
    crate::pln!("ModuleMainClass:");
    emit(2, &format!("#{idx}"), &class_name(cf, idx).replace('/', "."));
}

/// `ModuleTarget` (JDK-internal): the target platform string, e.g. `windows-amd64`.
pub fn print_target(cf: &ClassFile, info: &[u8]) {
    if info.len() < 2 {
        return;
    }
    let idx = u16::from_be_bytes([info[0], info[1]]);
    crate::pln!("ModuleTarget:");
    emit(2, &format!("target_platform: #{idx}"), cf.utf8(idx).unwrap_or(""));
}

/// `ModuleHashes` (JDK-internal): a hash algorithm plus, per dependent module,
/// its name and the raw hash bytes (rendered as lowercase hex).
pub fn print_hashes(cf: &ClassFile, info: &[u8]) {
    let mut r = ClassReader::new(info);
    let Ok(algorithm) = r.read_u16() else { return };
    crate::pln!("ModuleHashes:");
    emit(2, &format!("algorithm: #{algorithm}"), cf.utf8(algorithm).unwrap_or(""));
    let Ok(count) = r.read_u16() else { return };
    emit(2, &count.to_string(), "hashes");
    for _ in 0..count {
        let Ok(idx) = r.read_u16() else { return };
        // Module names are already dotted here, and javap leaves them unquoted.
        emit(2, &format!("#{idx}"), module_name(cf, idx));
        let Ok(len) = r.read_u16() else { return };
        crate::pln!("  hash_length: {len}");
        let Ok(bytes) = r.read_bytes(len as usize) else { return };
        let hex: String = bytes.iter().map(|b| format!("{b:02x}")).collect();
        crate::pln!("  hash: [{hex}]");
    }
}

// -- helpers -------------------------------------------------------------

/// Emits one `<indent><left>  // <comment>` row, with the `//` column at
/// `indent + 40`. When `comment` is empty, just the left part is printed.
fn emit(indent: usize, left: &str, comment: &str) {
    let body = format!("{}{left}", " ".repeat(indent));
    if comment.is_empty() {
        crate::pln!("{body}");
    } else {
        let width = indent + 40;
        crate::pln!("{body:<width$}// {comment}");
    }
}

/// Finds a top-level attribute's body by name.
fn attribute<'a>(cf: &'a ClassFile, name: &str) -> Option<&'a [u8]> {
    cf.attributes
        .iter()
        .find(|a| cf.utf8(a.name_index) == Some(name))
        .map(|a| a.info.as_slice())
}

/// Resolves a `CONSTANT_Module` index to its name (falling back to a direct Utf8).
fn module_name(cf: &ClassFile, index: u16) -> &str {
    match cf.constant_pool.get((index as usize).wrapping_sub(1)) {
        Some(ConstantPoolEntry::Module { name_index }) => cf.utf8(*name_index).unwrap_or(""),
        _ => cf.utf8(index).unwrap_or(""),
    }
}

/// Resolves a `CONSTANT_Package` index to its (slash-separated) name.
fn package_name(cf: &ClassFile, index: u16) -> &str {
    match cf.constant_pool.get((index as usize).wrapping_sub(1)) {
        Some(ConstantPoolEntry::Package { name_index }) => cf.utf8(*name_index).unwrap_or(""),
        _ => cf.utf8(index).unwrap_or(""),
    }
}

/// Resolves a `CONSTANT_Class` index to its (slash-separated) binary name.
fn class_name(cf: &ClassFile, index: u16) -> &str {
    cf.class_name(index).unwrap_or("")
}
