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

// -- the descriptor as *data* (Fase J: jlink) -----------------------------
//
// Everything above renders the attribute for `javap`. What follows reads the same
// bytes into a structure, because resolving a module graph needs to *ask questions*
// of a descriptor (what does this require? does it export that package?), not print
// it. Same split as `annotations.rs`: printers for javap, accessors for the tools.

/// One `requires` directive: the module depended on, plus the two modifiers that
/// change what the dependency *means*. `transitive` grants implied readability (whoever
/// reads me also reads this); `static_phase` is compile-time only, so resolution at
/// link/run time ignores it. `mandated` marks the implicit `requires java.base`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Requires {
    pub name: String,
    pub transitive: bool,
    pub static_phase: bool,
    pub mandated: bool,
}

/// One `exports` or `opens` directive. An empty `to` is the unqualified form (any
/// module may read the package); a non-empty `to` restricts it to those modules.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Exports {
    pub package: String,
    pub to: Vec<String>,
    pub mandated: bool,
}

/// One `provides S with I1, I2…`: a service and its implementations.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Provides {
    pub service: String,
    pub with: Vec<String>,
}

/// A module's descriptor, read out of its `module-info.class` — the unit a module
/// graph is resolved over. Package and class names are **dotted** here (the class
/// file stores them internally, with `/`), because that is how they are written in
/// source and printed by `--describe-module`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ModuleDescriptor {
    pub name: String,
    pub version: Option<String>,
    pub open: bool,
    pub requires: Vec<Requires>,
    pub exports: Vec<Exports>,
    pub opens: Vec<Exports>,
    pub uses: Vec<String>,
    pub provides: Vec<Provides>,
    /// From `ModulePackages`: every package in the module, exported or not.
    pub packages: Vec<String>,
    /// From `ModuleMainClass`.
    pub main_class: Option<String>,
}

// Flag bits of the `Module` attribute (JVMS §4.7.25).
const ACC_OPEN: u16 = 0x0020;
const ACC_TRANSITIVE: u16 = 0x0020;
const ACC_STATIC_PHASE: u16 = 0x0040;
const ACC_MANDATED: u16 = 0x8000;

/// Reads the `Module` attribute (plus `ModulePackages`/`ModuleMainClass` when present)
/// into a [`ModuleDescriptor`]. `None` if this isn't a `module-info.class`.
pub fn descriptor(cf: &ClassFile) -> Option<ModuleDescriptor> {
    let info = attribute(cf, "Module")?;
    let mut r = ClassReader::new(info);
    let name_index = r.read_u16().ok()?;
    let flags = r.read_u16().ok()?;
    let version_index = r.read_u16().ok()?;

    let mut requires = Vec::new();
    for _ in 0..r.read_u16().ok()? {
        let (idx, f) = (r.read_u16().ok()?, r.read_u16().ok()?);
        let _version = r.read_u16().ok()?;
        requires.push(Requires {
            name: module_name(cf, idx).to_string(),
            transitive: f & ACC_TRANSITIVE != 0,
            static_phase: f & ACC_STATIC_PHASE != 0,
            mandated: f & ACC_MANDATED != 0,
        });
    }
    let exports = exports_data(cf, &mut r)?;
    let opens = exports_data(cf, &mut r)?;

    let mut uses = Vec::new();
    for _ in 0..r.read_u16().ok()? {
        let idx = r.read_u16().ok()?;
        uses.push(class_name(cf, idx).replace('/', "."));
    }
    let mut provides = Vec::new();
    for _ in 0..r.read_u16().ok()? {
        let service = class_name(cf, r.read_u16().ok()?).replace('/', ".");
        let mut with = Vec::new();
        for _ in 0..r.read_u16().ok()? {
            with.push(class_name(cf, r.read_u16().ok()?).replace('/', "."));
        }
        provides.push(Provides { service, with });
    }

    // The companion attributes are optional; a module-info without them is still valid.
    let packages = attribute(cf, "ModulePackages")
        .map(|info| {
            let mut r = ClassReader::new(info);
            let count = r.read_u16().unwrap_or(0);
            (0..count)
                .filter_map(|_| r.read_u16().ok())
                .map(|i| package_name(cf, i).replace('/', "."))
                .collect()
        })
        .unwrap_or_default();
    let main_class = attribute(cf, "ModuleMainClass")
        .filter(|i| i.len() >= 2)
        .map(|i| class_name(cf, u16::from_be_bytes([i[0], i[1]])).replace('/', "."));

    Some(ModuleDescriptor {
        name: module_name(cf, name_index).to_string(),
        version: version_index
            .checked_sub(1)
            .and_then(|_| cf.utf8(version_index))
            .map(str::to_string),
        open: flags & ACC_OPEN != 0,
        requires,
        exports,
        opens,
        uses,
        provides,
        packages,
        main_class,
    })
}

/// Reads one `exports`/`opens` group — identical layout, so both use this.
fn exports_data(cf: &ClassFile, r: &mut ClassReader) -> Option<Vec<Exports>> {
    let count = r.read_u16().ok()?;
    let mut out = Vec::with_capacity(count as usize);
    for _ in 0..count {
        let pkg = r.read_u16().ok()?;
        let flags = r.read_u16().ok()?;
        let to_count = r.read_u16().ok()?;
        let mut to = Vec::with_capacity(to_count as usize);
        for _ in 0..to_count {
            to.push(module_name(cf, r.read_u16().ok()?).to_string());
        }
        out.push(Exports {
            package: package_name(cf, pkg).replace('/', "."),
            to,
            mandated: flags & ACC_MANDATED != 0,
        });
    }
    Some(out)
}

impl ModuleDescriptor {
    /// Renders the descriptor the way `java --describe-module` does: the name (with
    /// version) first, then the directives. Unqualified `exports`/`opens` are sorted
    /// and come before the qualified ones — matching the reference tool's grouping, so
    /// the output can be diffed against it directly.
    pub fn describe(&self) -> String {
        let mut out = String::new();
        let open = if self.open { "open " } else { "" };
        match &self.version {
            Some(v) => out.push_str(&format!("{open}{}@{v}\n", self.name)),
            None => out.push_str(&format!("{open}{}\n", self.name)),
        }

        let mut plain: Vec<&Exports> = self.exports.iter().filter(|e| e.to.is_empty()).collect();
        plain.sort_by(|a, b| a.package.cmp(&b.package));
        for e in plain {
            out.push_str(&format!("exports {}\n", e.package));
        }
        for r in &self.requires {
            let mut line = format!("requires {}", r.name);
            if r.transitive {
                line.push_str(" transitive");
            }
            if r.static_phase {
                line.push_str(" static");
            }
            if r.mandated {
                line.push_str(" mandated");
            }
            out.push_str(&line);
            out.push('\n');
        }
        for u in &self.uses {
            out.push_str(&format!("uses {u}\n"));
        }
        for p in &self.provides {
            out.push_str(&format!("provides {} with {}\n", p.service, p.with.join(" ")));
        }
        let mut qualified: Vec<&Exports> = self.exports.iter().filter(|e| !e.to.is_empty()).collect();
        qualified.sort_by(|a, b| a.package.cmp(&b.package));
        for e in qualified {
            out.push_str(&format!("qualified exports {} to {}\n", e.package, e.to.join(" ")));
        }
        let mut opens_plain: Vec<&Exports> = self.opens.iter().filter(|o| o.to.is_empty()).collect();
        opens_plain.sort_by(|a, b| a.package.cmp(&b.package));
        for o in opens_plain {
            out.push_str(&format!("opens {}\n", o.package));
        }
        let mut opens_q: Vec<&Exports> = self.opens.iter().filter(|o| !o.to.is_empty()).collect();
        opens_q.sort_by(|a, b| a.package.cmp(&b.package));
        for o in opens_q {
            out.push_str(&format!("qualified opens {} to {}\n", o.package, o.to.join(" ")));
        }
        // `contains` = a package the module has but neither exports nor opens.
        let mut contained: Vec<&String> = self
            .packages
            .iter()
            .filter(|p| {
                !self.exports.iter().any(|e| &e.package == *p)
                    && !self.opens.iter().any(|o| &o.package == *p)
            })
            .collect();
        contained.sort();
        for p in contained {
            out.push_str(&format!("contains {p}\n"));
        }
        out
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

#[cfg(test)]
mod tests {
    use super::*;

    /// The fixture exercises every directive and both `requires` modifiers, so parsing it
    /// covers the whole `Module` attribute layout. Verified against the real `javap -v`.
    fn fixture() -> ClassFile {
        ClassFile::from_path("java/kaji.sample/module-info.class")
            .expect("java/kaji.sample/module-info.class (compilar con bin/javac-frozen.exe --emit)")
    }

    #[test]
    fn the_descriptor_reads_the_module_header_and_its_modifiers() {
        let d = descriptor(&fixture()).expect("module-info tiene atributo Module");
        assert_eq!(d.name, "kaji.sample");
        assert!(d.open, "el fixture es un `open module`");

        // `requires` carries meaning in its flags: transitive propagates readability,
        // static is compile-time only. An explicit `requires java.base` is NOT mandated.
        let names: Vec<&str> = d.requires.iter().map(|r| r.name.as_str()).collect();
        assert_eq!(names, ["java.base", "java.logging", "java.compiler"]);
        let logging = d.requires.iter().find(|r| r.name == "java.logging").unwrap();
        assert!(logging.transitive && !logging.static_phase);
        let compiler = d.requires.iter().find(|r| r.name == "java.compiler").unwrap();
        assert!(compiler.static_phase && !compiler.transitive);
    }

    #[test]
    fn exports_and_opens_keep_their_qualified_target_lists() {
        let d = descriptor(&fixture()).unwrap();
        // Package names come back dotted, not in the class file's internal `/` form.
        let plain = d.exports.iter().find(|e| e.package == "com.kaji.api").unwrap();
        assert!(plain.to.is_empty(), "sin `to` es la forma no cualificada");
        let qualified = d.exports.iter().find(|e| e.package == "com.kaji.internal").unwrap();
        assert_eq!(qualified.to, ["kaji.friend", "kaji.other"]);
        let opens = d.opens.iter().find(|o| o.package == "com.kaji.deep").unwrap();
        assert_eq!(opens.to, ["kaji.friend"]);
    }

    #[test]
    fn services_carry_every_implementation() {
        let d = descriptor(&fixture()).unwrap();
        assert_eq!(d.uses, ["com.kaji.api.Service"]);
        assert_eq!(d.provides.len(), 1);
        assert_eq!(d.provides[0].service, "com.kaji.api.Service");
        assert_eq!(d.provides[0].with, ["com.kaji.internal.Impl", "com.kaji.internal.Alt"]);
    }

    #[test]
    fn describe_renders_the_reference_tool_layout() {
        let text = descriptor(&fixture()).unwrap().describe();
        let lines: Vec<&str> = text.lines().collect();
        // `java --describe-module` leads with the header, then unqualified exports, and
        // puts the qualified ones after the service directives.
        assert_eq!(lines[0], "open kaji.sample");
        assert_eq!(lines[1], "exports com.kaji.api");
        assert!(text.contains("requires java.logging transitive"));
        assert!(text.contains("requires java.compiler static"));
        assert!(text.contains("qualified exports com.kaji.internal to kaji.friend kaji.other"));
        assert!(text.contains("provides com.kaji.api.Service with com.kaji.internal.Impl com.kaji.internal.Alt"));
        assert!(text.contains("qualified opens com.kaji.deep to kaji.friend"));
    }

    /// A regular class has no `Module` attribute — the reader must say so rather than
    /// inventing an empty descriptor.
    #[test]
    fn a_plain_class_has_no_module_descriptor() {
        let cf = ClassFile::from_path("java/Add.class").expect("java/Add.class");
        assert!(descriptor(&cf).is_none());
    }
}
