//! `jlink` — assembles a custom runtime image from a set of modules (JEP 282).
//!
//! Fase J. The real tool does three things: read module descriptors, resolve the module
//! graph from a set of roots, and package what the graph reaches into an image. So far:
//!
//! - **J0** `--describe-module` — read a `module-info.class` into a descriptor.
//! - **J1** `--add-modules` — resolve the graph: the transitive closure over `requires`
//!   plus the readability relation that `requires transitive` implies.
//! - **J2** `--output` — write the image: the resolved modules' resources plus a
//!   `release` file. *Exploded* (`modules/<name>/…`) rather than the real tool's
//!   `lib/modules` container; that container is J3/J4.
//!
//! Both mirror commands of the real toolchain (`java --describe-module`,
//! `java --list-modules` over what `jlink --add-modules` produced), which is the point:
//! their output is the fixture we diff against, exactly as `javap` was for the parser.
//!
//! Uso:
//!   jlink --describe-module <module-info.class>
//!   jlink --module-path <dir>[;<dir>…] --add-modules <m>[,<m>…] [--output <dir>]
//!         [--jimage] [--strip-debug] [--add-options <opts>] [--launcher <n>=<m>[/<cls>]]
//!         [--show-reads]

use std::fs;
use std::path::{Path, PathBuf};
use std::process::ExitCode;

use jvm::jvm::class_file::{self, ClassFile};
use jvm::jvm::modules::{resolve, ModulePath};
use jvm::jvm::parser::attributes::module;

fn main() -> ExitCode {
    let args: Vec<String> = std::env::args().skip(1).collect();
    if args.first().map(String::as_str) == Some("--describe-module") && args.len() == 2 {
        return describe(&args[1]);
    }
    match parse_link_args(&args) {
        Some((path, roots, show_reads, output, jimage, plugins)) => {
            link(path, roots, show_reads, output, jimage, plugins)
        }
        None => {
            eprintln!("uso: jlink --describe-module <module-info.class>");
            eprintln!("     jlink --module-path <dir>[;<dir>…] --add-modules <m>[,<m>…] [--output <dir>] [--show-reads]");
            ExitCode::FAILURE
        }
    }
}

/// Pulls `--module-path`, `--add-modules` and the `--show-reads` switch out of the line.
/// Both list options accept the platform-ish separators the real tool does: `;` `,`.
fn parse_link_args(
    args: &[String],
) -> Option<(Vec<PathBuf>, Vec<String>, bool, Option<PathBuf>, bool, Plugins)> {
    let (mut dirs, mut roots, mut show_reads) = (Vec::new(), Vec::new(), false);
    let (mut output, mut jimage) = (None, false);
    let mut plugins = Plugins::default();
    let mut it = args.iter();
    while let Some(arg) = it.next() {
        match arg.as_str() {
            "--module-path" | "-p" => {
                dirs = it.next()?.split(&[';', ','][..]).map(PathBuf::from).collect()
            }
            "--add-modules" => {
                roots = it.next()?.split(&[',', ';'][..]).map(str::to_string).collect()
            }
            "--output" | "-o" => output = Some(PathBuf::from(it.next()?)),
            "--jimage" => jimage = true,
            "--strip-debug" | "-G" => plugins.strip_debug = true,
            "--compress" => plugins.compress = Some(it.next()?.clone()),
            "--add-options" => plugins.add_options = Some(it.next()?.clone()),
            "--launcher" => plugins.launcher = Some(it.next()?.clone()),
            "--show-reads" => show_reads = true,
            _ => return None,
        }
    }
    (!dirs.is_empty() && !roots.is_empty())
        .then_some((dirs, roots, show_reads, output, jimage, plugins))
}

/// Reads `path` as a `module-info.class` and prints its descriptor (J0).
fn describe(path: &str) -> ExitCode {
    let class_file = match ClassFile::from_path(path) {
        Ok(cf) => cf,
        Err(e) => {
            eprintln!("jlink: no se pudo leer '{path}': {e}");
            return ExitCode::FAILURE;
        }
    };
    match module::descriptor(&class_file) {
        Some(d) => {
            print!("{}", d.describe());
            ExitCode::SUCCESS
        }
        None => {
            eprintln!("jlink: '{path}' no es un module-info.class (no tiene atributo Module)");
            ExitCode::FAILURE
        }
    }
}

/// Resolves the graph and prints the resolved set — one module per line, sorted, the
/// same shape `java --list-modules` prints for a linked image (J1).
fn link(
    dirs: Vec<PathBuf>,
    roots: Vec<String>,
    show_reads: bool,
    output: Option<PathBuf>,
    jimage: bool,
    plugins: Plugins,
) -> ExitCode {
    let path = ModulePath::new(dirs);
    let configuration = match resolve(&path, &roots) {
        Ok(c) => c,
        Err(e) => {
            eprintln!("jlink: {e}");
            return ExitCode::FAILURE;
        }
    };
    if let Some(dir) = output {
        return match write_image(&path, &configuration, &dir, jimage, &plugins) {
            Ok(count) => {
                println!(
                    "jlink: imagen en {} — {} módulos, {count} recursos",
                    dir.display(),
                    configuration.modules.len()
                );
                ExitCode::SUCCESS
            }
            Err(e) => {
                eprintln!("jlink: no se pudo escribir la imagen: {e}");
                ExitCode::FAILURE
            }
        };
    }
    for name in configuration.module_names() {
        match &configuration.modules[name].version {
            Some(v) => println!("{name}@{v}"),
            None => println!("{name}"),
        }
    }
    if show_reads {
        println!();
        for (name, targets) in &configuration.reads {
            println!("{name} reads {}", targets.iter().cloned().collect::<Vec<_>>().join(" "));
        }
    }
    ExitCode::SUCCESS
}

/// Writes the resolved modules into an **exploded** image and returns how many resources
/// it copied.
///
/// Layout: `<out>/modules/<module>/…` (the JDK's own exploded-build shape) plus a
/// `release` file at the root. The real tool packs the same content into the `lib/modules`
/// jimage container instead — same *contents*, different container, which is exactly the
/// seam J3/J4 slot into. `release` is written in the reference format so it can be diffed:
/// `JAVA_VERSION` and a space-separated `MODULES`.
fn write_image(
    path: &ModulePath,
    configuration: &jvm::jvm::modules::Configuration,
    out: &Path,
    jimage: bool,
    plugins: &Plugins,
) -> std::io::Result<usize> {
    let copied = if jimage {
        pack_jimage(path, configuration, out, plugins)?
    } else {
        let modules_dir = out.join("modules");
        fs::create_dir_all(&modules_dir)?;
        let mut copied = 0;
        for name in configuration.module_names() {
            let source = path.locate(name).ok_or_else(|| {
                std::io::Error::new(std::io::ErrorKind::NotFound, format!("módulo '{name}'"))
            })?;
            copied += copy_tree(&source, &modules_dir.join(name))?;
        }
        copied
    };

    // The version of an image is the version its modules carry (they all agree in a real
    // JDK); with unversioned modules — ours — there is simply nothing to claim.
    // A version is only claimed when the modules carry one; ours do not, and an empty
    // `JAVA_VERSION=""` would assert a version rather than admit there is none.
    let version = configuration.modules.values().find_map(|d| d.version.clone());
    let names = configuration.module_names().join(" ");
    let mut release = String::new();
    if let Some(v) = version {
        release.push_str(&format!("JAVA_VERSION=\"{v}\"\n"));
    }
    release.push_str(&format!("MODULES=\"{names}\"\n"));
    fs::write(out.join("release"), release)?;
    Ok(copied)
}

/// Copies a directory tree, returning the number of files written.
fn copy_tree(from: &Path, to: &Path) -> std::io::Result<usize> {
    fs::create_dir_all(to)?;
    let mut count = 0;
    for entry in fs::read_dir(from)? {
        let entry = entry?;
        let (source, target) = (entry.path(), to.join(entry.file_name()));
        if entry.file_type()?.is_dir() {
            count += copy_tree(&source, &target)?;
        } else {
            fs::copy(&source, &target)?;
            count += 1;
        }
    }
    Ok(count)
}

/// Packs the resolved modules into a real `lib/modules` **jimage** (J4) instead of an
/// exploded tree. Same contents, the container the reference tool uses.
fn pack_jimage(
    path: &ModulePath,
    configuration: &jvm::jvm::modules::Configuration,
    out: &Path,
    plugins: &Plugins,
) -> std::io::Result<usize> {
    let mut resources = Vec::new();
    for module in configuration.module_names() {
        let root = path.locate(module).ok_or_else(|| {
            std::io::Error::new(std::io::ErrorKind::NotFound, format!("módulo '{module}'"))
        })?;
        collect(&root, &root, module, &mut resources)?;
    }
    plugins.apply(&mut resources, out)?;
    let compress = plugins.compress.is_some();
    let bytes = jvm::jvm::jimage::write_image_with(&resources, compress);
    let lib = out.join("lib");
    fs::create_dir_all(&lib)?;
    fs::write(lib.join("modules"), bytes)?;
    Ok(resources.len())
}

/// Walks a module directory, turning each file into a named resource.
fn collect(
    root: &Path,
    dir: &Path,
    module: &str,
    out: &mut Vec<jvm::jvm::jimage::Resource>,
) -> std::io::Result<()> {
    for entry in fs::read_dir(dir)? {
        let entry = entry?;
        let path = entry.path();
        if entry.file_type()?.is_dir() {
            collect(root, &path, module, out)?;
        } else {
            let relative = path.strip_prefix(root).unwrap().to_string_lossy().replace('\\', "/");
            out.push(jvm::jvm::jimage::Resource {
                name: format!("/{module}/{relative}"),
                bytes: fs::read(&path)?,
            });
        }
    }
    Ok(())
}

/// The transform pipeline. Real jlink models these as pluggable stages between reading the
/// modules and writing the image; here they are the three that need no compressor.
#[derive(Default)]
struct Plugins {
    /// Drop the attributes only a debugger reads.
    strip_debug: bool,
    /// VM options baked into the image, as a `jdk/internal/vm/options` resource.
    add_options: Option<String>,
    /// `<name>=<module>[/<mainclass>]` — a script in `bin/`.
    launcher: Option<String>,
    /// `zip-[0-9]`. Only `zip-0` is honoured: the resources are stored as compressed
    /// resources with a valid zlib stream of *stored* blocks. See `jimage::zlib_stored`.
    compress: Option<String>,
}

impl Plugins {
    /// Runs the pipeline over the resources on their way into the image (and writes the
    /// launcher, which lands outside the container).
    fn apply(
        &self,
        resources: &mut Vec<jvm::jvm::jimage::Resource>,
        out: &Path,
    ) -> std::io::Result<()> {
        if self.strip_debug {
            let mut stripped = 0;
            let mut saved = 0usize;
            for resource in resources.iter_mut() {
                if !resource.name.ends_with(".class") {
                    continue;
                }
                if let Some(lean) = class_file::strip_debug(&resource.bytes) {
                    saved += resource.bytes.len().saturating_sub(lean.len());
                    resource.bytes = lean;
                    stripped += 1;
                }
            }
            println!("jlink: --strip-debug: {stripped} clases, {saved} bytes menos");
        }
        if let Some(options) = &self.add_options {
            // El nombre del recurso es el que la VM real lee al arrancar.
            resources.push(jvm::jvm::jimage::Resource {
                name: "/java.base/jdk/internal/vm/options".to_string(),
                bytes: options.as_bytes().to_vec(),
            });
            println!("jlink: --add-options: {options:?} embebido en la imagen");
        }
        if let Some(level) = &self.compress {
            if level != "zip-0" && level != "0" {
                println!(
                    "jlink: --compress {level}: sólo zip-0 está implementado — los recursos van                      como recursos comprimidos con un zlib de bloques *stored* (sin encoger)"
                );
            }
        }
        if let Some(spec) = &self.launcher {
            let (name, target) = spec.split_once('=').unwrap_or((spec.as_str(), ""));
            let bin = out.join("bin");
            fs::create_dir_all(&bin)?;
            // El script se arma con bytes: un .bat quiere CRLF, y escribir los saltos
            // como escapes en el fuente es justo donde se cuelan CR sueltos.
            let mut script = Vec::new();
            for line in [
                "@echo off".to_string(),
                format!("rem lanzador generado por jlink para {target}"),
            ] {
                script.extend_from_slice(line.as_bytes());
                script.extend_from_slice(&[13, 10]);
            }
            fs::write(bin.join(format!("{name}.bat")), &script)?;
            println!("jlink: --launcher: bin/{name}.bat -> {target}");
        }
        Ok(())
    }
}
