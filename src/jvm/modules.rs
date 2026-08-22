//! **Module graph resolution** (JPMS) — Fase J, hito J1.
//!
//! Given a *module path* and a set of *root* modules, resolution answers two questions:
//! **which** modules are needed, and **who may read whom**. jlink packages the first
//! answer; the VM enforces the second.
//!
//! Two subtleties carry all the weight, and both live in the `requires` flags:
//!
//! - `requires transitive N` grants **implied readability**: anyone who reads me also
//!   reads `N`. That is how a module can re-export an API it depends on (`java.sql`
//!   requires `java.xml` transitively, so code reading `java.sql` sees `java.xml` types
//!   in its signatures without requiring it).
//! - `requires static N` is a **compile-time only** dependency. It is *not* resolved
//!   here: the module may well be absent at link and run time, and that is legal.
//!
//! Resolution is a plain reachability closure; the interesting part is that readability
//! is a *different*, larger relation than the requires edges it is computed from.

use std::collections::{BTreeMap, BTreeSet, VecDeque};
use std::path::{Path, PathBuf};

use crate::jvm::class_file::ClassFile;
use crate::jvm::parser::attributes::module::{self, ModuleDescriptor};

/// Where modules are looked up: directories that each hold **exploded** modules, i.e.
/// `<dir>/<module-name>/module-info.class`. (Real jlink also reads `.jmod` and modular
/// JARs; those are containers around the same descriptor.)
pub struct ModulePath {
    dirs: Vec<PathBuf>,
}

/// Why a resolution failed. Both are *link-time* errors: jlink refuses to build an
/// image it knows cannot run.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ResolveError {
    /// A required module isn't on the module path.
    NotFound { module: String, required_by: String },
    /// Two resolved modules contain the same package. Forbidden: a package must come
    /// from exactly one module, or class loading would be ambiguous.
    SplitPackage { package: String, first: String, second: String },
}

impl std::fmt::Display for ResolveError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            ResolveError::NotFound { module, required_by } => {
                write!(f, "módulo '{module}' no encontrado (requerido por '{required_by}')")
            }
            ResolveError::SplitPackage { package, first, second } => {
                write!(f, "paquete '{package}' partido entre '{first}' y '{second}'")
            }
        }
    }
}

/// The outcome of a successful resolution: the modules to package, and the readability
/// graph over them.
#[derive(Debug)]
pub struct Configuration {
    /// Every resolved module, by name.
    pub modules: BTreeMap<String, ModuleDescriptor>,
    /// `reads[m]` = every module `m` may read, implied readability included.
    pub reads: BTreeMap<String, BTreeSet<String>>,
}

impl ModulePath {
    pub fn new(dirs: Vec<PathBuf>) -> Self {
        ModulePath { dirs }
    }

    /// Reads `<dir>/<name>/module-info.class` from the first directory that has it.
    pub fn find(&self, name: &str) -> Option<ModuleDescriptor> {
        for dir in &self.dirs {
            let path = dir.join(name).join("module-info.class");
            if let Some(d) = read_descriptor(&path) {
                return Some(d);
            }
        }
        None
    }

    /// The directory a module lives in (`<dir>/<name>`), for reading its resources.
    pub fn locate(&self, name: &str) -> Option<PathBuf> {
        self.dirs
            .iter()
            .map(|dir| dir.join(name))
            .find(|d| d.join("module-info.class").is_file())
    }

    /// Every module observable on this path, sorted — the `--list-modules` view.
    pub fn observable(&self) -> Vec<String> {
        let mut names = BTreeSet::new();
        for dir in &self.dirs {
            let Ok(entries) = std::fs::read_dir(dir) else { continue };
            for entry in entries.flatten() {
                if read_descriptor(&entry.path().join("module-info.class")).is_some() {
                    if let Some(n) = entry.file_name().to_str() {
                        names.insert(n.to_string());
                    }
                }
            }
        }
        names.into_iter().collect()
    }
}

fn read_descriptor(path: &Path) -> Option<ModuleDescriptor> {
    let text = path.to_str()?;
    module::descriptor(&ClassFile::from_path(text).ok()?)
}


/// The readability relation over an already-known set of modules.
///
/// A module reads what it requires; and through any of those required **`transitive`**, it
/// also reads what *they* require transitively, and so on. Kept separate from [`resolve`]
/// because a VM booted from an image already *has* its modules — it needs the relation, not
/// the reachability search that produced them.
pub fn readability(
    modules: &BTreeMap<String, ModuleDescriptor>,
) -> BTreeMap<String, BTreeSet<String>> {
    let mut reads = BTreeMap::new();
    for (name, descriptor) in modules {
        let mut visible = BTreeSet::new();
        let mut pending: VecDeque<&str> = descriptor
            .requires
            .iter()
            .filter(|r| !r.static_phase)
            .map(|r| r.name.as_str())
            .collect();
        while let Some(other) = pending.pop_front() {
            if !visible.insert(other.to_string()) {
                continue;
            }
            // Only `transitive` edges keep propagating — that is what "implied" means.
            if let Some(d) = modules.get(other) {
                for r in d.requires.iter().filter(|r| r.transitive && !r.static_phase) {
                    pending.push_back(&r.name);
                }
            }
        }
        reads.insert(name.clone(), visible);
    }
    reads
}

/// Resolves `roots` against `path`: the transitive closure over `requires`, then the
/// readability graph, then the split-package check.
pub fn resolve(path: &ModulePath, roots: &[String]) -> Result<Configuration, ResolveError> {
    // ---- 1. reachability: which modules do we need at all? ----
    // `static` requires are skipped: they exist only for the compiler.
    let mut modules: BTreeMap<String, ModuleDescriptor> = BTreeMap::new();
    let mut queue: VecDeque<(String, String)> =
        roots.iter().map(|r| (r.clone(), "<root>".to_string())).collect();
    while let Some((name, required_by)) = queue.pop_front() {
        if modules.contains_key(&name) {
            continue;
        }
        let descriptor = path
            .find(&name)
            .ok_or(ResolveError::NotFound { module: name.clone(), required_by })?;
        for r in &descriptor.requires {
            if !r.static_phase && !modules.contains_key(&r.name) {
                queue.push_back((r.name.clone(), name.clone()));
            }
        }
        modules.insert(name, descriptor);
    }

    // ---- 2. readability, which is *not* the same relation ----
    let reads = readability(&modules);

    // ---- 3. split packages ----
    // Checked over the *resolved* set: two modules may each contain package P and be
    // fine on their own, but not together in one image.
    let mut owner: BTreeMap<&str, &str> = BTreeMap::new();
    for (name, descriptor) in &modules {
        for package in &descriptor.packages {
            if let Some(first) = owner.insert(package, name) {
                return Err(ResolveError::SplitPackage {
                    package: package.clone(),
                    first: first.to_string(),
                    second: name.clone(),
                });
            }
        }
    }

    Ok(Configuration { modules, reads })
}

impl Configuration {
    /// Builds a configuration over modules that are already known — the case of a VM
    /// booted from an image, whose modules are whatever the image holds.
    pub fn of(modules: BTreeMap<String, ModuleDescriptor>) -> Configuration {
        let reads = readability(&modules);
        Configuration { modules, reads }
    }

    /// The resolved module names, sorted — what jlink packages, and what
    /// `java --list-modules` prints for an image.
    pub fn module_names(&self) -> Vec<&str> {
        self.modules.keys().map(String::as_str).collect()
    }

    /// Whether `from` may read `to` — the check the VM makes before letting code in
    /// `from` touch a type in `to`. A module always reads itself.
    pub fn reads(&self, from: &str, to: &str) -> bool {
        from == to || self.reads.get(from).is_some_and(|s| s.contains(to))
    }

    /// Whether `to` exports `package` **to** `from` (unqualified, or qualified with
    /// `from` among its targets). Readability alone isn't access: the owning module
    /// must also have exported the package.
    pub fn exports_to(&self, to: &str, package: &str, from: &str) -> bool {
        self.modules.get(to).is_some_and(|d| {
            d.exports
                .iter()
                .any(|e| e.package == package && (e.to.is_empty() || e.to.iter().any(|t| t == from)))
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A tiny in-memory-ish path built from the fixture module: the resolver is exercised
    /// against real JDK descriptors in the differential test (`tools/`), so here we check
    /// the *rules* on shapes we control.
    fn fixture_path() -> ModulePath {
        ModulePath::new(vec![PathBuf::from("java")])
    }

    #[test]
    fn a_missing_module_names_who_required_it() {
        // The fixture requires java.base/java.logging, which aren't on this path.
        let err = resolve(&fixture_path(), &["kaji.sample".to_string()]).unwrap_err();
        match err {
            ResolveError::NotFound { module, required_by } => {
                assert_eq!(required_by, "kaji.sample");
                assert!(module == "java.base" || module == "java.logging", "fue {module}");
            }
            other => panic!("esperaba NotFound, fue {other:?}"),
        }
    }

    #[test]
    fn an_unknown_root_is_reported_against_the_root_marker() {
        let err = resolve(&fixture_path(), &["no.such.module".to_string()]).unwrap_err();
        assert_eq!(
            err,
            ResolveError::NotFound {
                module: "no.such.module".to_string(),
                required_by: "<root>".to_string()
            }
        );
    }

    #[test]
    fn the_fixture_module_is_observable_on_its_directory() {
        // In an exploded module path the *directory name is the module name*, so the path
        // entry is `java/` and the module is the `kaji.sample/` directory inside it.
        let path = ModulePath::new(vec![PathBuf::from("java")]);
        assert!(path.observable().contains(&"kaji.sample".to_string()));
    }
}
