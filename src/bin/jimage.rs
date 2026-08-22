//! `jimage` — inspects a JDK runtime image (`lib/modules`).
//!
//! Fase J, hito J3: the **reader** for the container jlink writes. It comes before the
//! writer (J4) on purpose — there is a real 145 MB image on disk to check every step
//! against, and building a format you cannot yet read is backwards. Same order the
//! project already took once: the class file parser (and `javap`) before the emitter.
//!
//! Uso:  `jimage info <lib/modules>`
//!       `jimage list [--verbose] <lib/modules>`
//!       `jimage extract --dir=<dir> [--include=<texto>] <lib/modules>`

use std::io::Read;
use std::process::ExitCode;

use jvm::jvm::jimage::{Header, Index, HEADER_SIZE};

fn main() -> ExitCode {
    let args: Vec<String> = std::env::args().skip(1).collect();
    match (args.first().map(String::as_str), args.len()) {
        (Some("info"), 2) => info(&args[1]),
        (Some("list"), 2) => list(&args[1], false),
        (Some("list"), 3) if args[1] == "--verbose" => list(&args[2], true),
        (Some("extract"), _) if args.len() >= 3 => extract(&args[1..]),
        _ => {
            eprintln!("uso: jimage info <lib/modules>");
            eprintln!("     jimage list [--verbose] <lib/modules>");
            eprintln!("     jimage extract --dir=<dir> [--include=<texto>] <lib/modules>");
            ExitCode::FAILURE
        }
    }
}

/// Prints the image header. Only the first bytes are read: an image is ~145 MB and the
/// header is 28 of them, so there is no reason to pull the file into memory for this.
fn info(path: &str) -> ExitCode {
    let mut bytes = [0u8; HEADER_SIZE];
    let read = std::fs::File::open(path).and_then(|mut f| f.read_exact(&mut bytes));
    if let Err(e) = read {
        eprintln!("jimage: no se pudo leer '{path}': {e}");
        return ExitCode::FAILURE;
    }
    match Header::parse(&bytes) {
        Some(header) => {
            print!("{}", header.info());
            ExitCode::SUCCESS
        }
        None => {
            eprintln!("jimage: '{path}' no es una imagen (magic distinto de 0xCAFEDADA)");
            ExitCode::FAILURE
        }
    }
}

/// Lists every entry, grouped by module. Only the index is read — the resource bytes are
/// the other 143 MB and none of them are needed to say what is in there.
///
/// The image also holds *meta* entries (one per module and per package) that the reference
/// tool does not print: they describe the image to itself rather than being resources. They
/// are recognised by their module name and skipped, which is why the count here is smaller
/// than the header's `resource_count`.
fn list(path: &str, verbose: bool) -> ExitCode {
    let mut head = [0u8; HEADER_SIZE];
    let mut file = match std::fs::File::open(path) {
        Ok(f) => f,
        Err(e) => {
            eprintln!("jimage: no se pudo leer '{path}': {e}");
            return ExitCode::FAILURE;
        }
    };
    if file.read_exact(&mut head).is_err() {
        eprintln!("jimage: '{path}' es demasiado corto para ser una imagen");
        return ExitCode::FAILURE;
    }
    let Some(header) = Header::parse(&head) else {
        eprintln!("jimage: '{path}' no es una imagen (magic distinto de 0xCAFEDADA)");
        return ExitCode::FAILURE;
    };
    let mut index_bytes = vec![0u8; header.index_size() as usize];
    use std::io::Seek;
    if file.rewind().and_then(|_| file.read_exact(&mut index_bytes)).is_err() {
        eprintln!("jimage: el índice está truncado");
        return ExitCode::FAILURE;
    }
    let Some(index) = Index::parse(&index_bytes) else {
        eprintln!("jimage: no se pudo leer el índice");
        return ExitCode::FAILURE;
    };

    // Los módulos se ordenan por su **directorio** (`nombre/`), no por el nombre pelado.
    // La diferencia no es cosmética: comparando `java.management/` con `java.management.rmi/`,
    // en la posición 15 hay `/` (47) contra `.` (46) — y `.` es menor, así que el módulo más
    // largo va **antes** que aquel del que es extensión. Ordenar por el nombre pelado da el
    // orden inverso para esos seis pares, que es como se descubrió la regla.
    let mut by_module: std::collections::BTreeMap<String, Vec<jvm::jvm::jimage::Location>> =
        std::collections::BTreeMap::new();
    for location in index.entries() {
        if location.module.is_empty() || location.module == "modules" || location.module == "packages" {
            continue; // entradas meta, no recursos
        }
        by_module.entry(format!("{}/", location.module)).or_default().push(location);
    }

    println!("jimage: {path}");
    for (directory, mut locations) in by_module {
        let module = directory.trim_end_matches('/');
        locations.sort_by(|a, b| a.path().cmp(&b.path()));
        println!();
        println!("Module: {module}");
        if verbose {
            println!("Offset       Size       Compressed Entry");
        }
        for l in locations {
            if verbose {
                println!("{:12} {:10} {:10} {}", l.offset, l.uncompressed, l.compressed, l.path());
            } else {
                println!("    {}", l.path());
            }
        }
    }
    ExitCode::SUCCESS
}


/// Lee el índice de una imagen (cabecera + tablas), sin los bytes de los recursos.
fn read_index(path: &str) -> Option<Index> {
    use std::io::Seek;
    let mut head = [0u8; HEADER_SIZE];
    let mut file = std::fs::File::open(path).ok()?;
    file.read_exact(&mut head).ok()?;
    let header = Header::parse(&head)?;
    let mut bytes = vec![0u8; header.index_size() as usize];
    file.rewind().ok()?;
    file.read_exact(&mut bytes).ok()?;
    Index::parse(&bytes)
}

/// Extracts entries to a directory, laid out as `<dir>/<módulo>/<ruta>` — the same shape
/// the reference tool writes.
///
/// The bytes of an entry live at `index_size + location.offset` and run for `uncompressed`
/// bytes. **Compression is not handled**: every entry in a stock JDK image has
/// `compressed == 0`, and an image built with `jlink --compress` would need the decompressor
/// (that plugin is J6). An entry that claims to be compressed is reported, not silently
/// written wrong.
fn extract(args: &[String]) -> ExitCode {
    let mut dir = std::path::PathBuf::from(".");
    let mut include: Option<String> = None;
    let mut image = None;
    for arg in args {
        if let Some(v) = arg.strip_prefix("--dir=") {
            dir = std::path::PathBuf::from(v);
        } else if let Some(v) = arg.strip_prefix("--include=") {
            include = Some(v.to_string());
        } else {
            image = Some(arg.clone());
        }
    }
    let Some(image) = image else {
        eprintln!("jimage: falta la imagen");
        return ExitCode::FAILURE;
    };
    let Some(index) = read_index(&image) else {
        eprintln!("jimage: no se pudo leer el índice de '{image}'");
        return ExitCode::FAILURE;
    };
    let mut file = match std::fs::File::open(&image) {
        Ok(f) => f,
        Err(e) => {
            eprintln!("jimage: no se pudo abrir '{image}': {e}");
            return ExitCode::FAILURE;
        }
    };

    let data_start = index.header.index_size() as u64;
    let (mut written, skipped) = (0usize, 0usize);
    for location in index.entries() {
        if location.module.is_empty() || location.module == "modules" || location.module == "packages" {
            continue;
        }
        let full = location.full_name();
        if include.as_ref().is_some_and(|needle| !full.contains(needle.as_str())) {
            continue;
        }
        let target = dir.join(&location.module).join(location.path());
        let ok = (|| -> std::io::Result<()> {
            use std::io::Seek;
            // Un recurso comprimido guarda `compressed` bytes (cabecera + zlib) que hay que
            // inflar; uno normal guarda directamente sus `uncompressed`.
            let stored =
                if location.compressed != 0 { location.compressed } else { location.uncompressed };
            let mut bytes = vec![0u8; stored as usize];
            file.seek(std::io::SeekFrom::Start(data_start + location.offset))?;
            file.read_exact(&mut bytes)?;
            if location.compressed != 0 {
                bytes = jvm::jvm::jimage::decompress_resource(&bytes).ok_or_else(|| {
                    std::io::Error::new(
                        std::io::ErrorKind::InvalidData,
                        format!("no se pudo descomprimir '{full}'"),
                    )
                })?;
            }
            if let Some(parent) = target.parent() {
                std::fs::create_dir_all(parent)?;
            }
            std::fs::write(&target, &bytes)
        })();
        match ok {
            Ok(()) => written += 1,
            Err(e) => {
                eprintln!("jimage: no se pudo escribir {}: {e}", target.display());
                return ExitCode::FAILURE;
            }
        }
    }
    println!("jimage: {written} recursos extraídos en {}", dir.display());
    if skipped > 0 {
        println!("jimage: {skipped} omitidos por estar comprimidos");
    }
    ExitCode::SUCCESS
}

