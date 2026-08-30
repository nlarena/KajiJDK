// Runner headless de compatibilidad: corre una clase/método con el intérprete del REMOTO, para
// probar que NUESTRAS clases (boot + KajiLibrary + fixtures) ejecutan bien sobre él.
use std::path::{Path, PathBuf};

use jvm::jvm::class_file::ClassFile;
use jvm::jvm::interpreter::bytecode_interpreter::execute_reporting;
use jvm::jvm::interpreter::frame::{Frame, Value};
use jvm::jvm::interpreter::metaspace::MetaspaceService;

fn main() {
    let args: Vec<String> = std::env::args().skip(1).collect();
    if args.len() < 2 {
        eprintln!("usage: run-headless <File.class> <method> [intArg ...]");
        std::process::exit(2);
    }
    let path = &args[0];
    let method = &args[1];
    let call_args: Vec<Value> =
        args[2..].iter().map(|a| Value::Int(a.parse().unwrap_or(0))).collect();

    let cf = ClassFile::from_path(path).expect("load class");
    let name = cf.class_name(cf.this_class).unwrap().to_string();
    let descriptor = cf
        .methods
        .iter()
        .find(|m| cf.utf8(m.name_index) == Some(method.as_str()))
        .and_then(|m| cf.utf8(m.descriptor_index))
        .expect("method descriptor")
        .to_string();

    // Boot = KajiLibrary (la biblioteca propia, fuente de verdad) y despues boot/ como relleno
    // de lo que todavia solo existe compilado alli (Thread rico, Thread$State, Record, ...).
    // El orden importa: en las clases que colisionan gana KajiLibrary, que es el superset.
    // `--boot <dir>` apunta el bootclasspath a un solo directorio — por ejemplo el
    // `modules/java.base` de una imagen producida por nuestro jlink, que es como se
    // comprueba que la imagen sirve de runtime. Sin la opcion, los directorios de siempre.
    let boot = match args.iter().position(|a| a == "--boot") {
        Some(i) => vec![PathBuf::from(args.get(i + 1).expect("--boot necesita un directorio"))],
        None => vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")],
    };
    let mut app: Vec<PathBuf> = Path::new(path).parent().map(PathBuf::from).into_iter().collect();
    app.push(PathBuf::from("java"));

    let mut ms = MetaspaceService::new(boot, app);
    // `--boot-image <lib/modules>` arranca desde una imagen de runtime en vez de directorios:
    // la prueba de que la imagen que produce nuestro jlink sirve para *ejecutar*, no solo para
    // inspeccionar.
    if let Some(i) = args.iter().position(|a| a == "--boot-image") {
        let path = args.get(i + 1).expect("--boot-image necesita la ruta de lib/modules");
        assert!(ms.boot_from_image(path), "no se pudo abrir la imagen {path}");
        eprintln!("run-headless: booteando desde {path} ({} clases)", ms.boot_image_classes());
    }
    ms.add(name.clone(), cf);
    let entry = ms.resolve_method(&name, method, &descriptor).expect("resolve method");
    let max_locals = ms.max_locals(entry);
    let frame = Frame::new(entry, max_locals, call_args);
    let report = execute_reporting(ms, frame);
    // The result line first, and byte for byte what it always was: the fuzzer's runner parses it,
    // and a tool that changes its output format to fix a reporting bug has traded one for another.
    println!("{path} {method}{descriptor} -> {:?}", report.value);
    // FZ-001. What the program printed goes to **stderr**, so it cannot be mistaken for the result
    // line by anything reading stdout. It carries the uncaught-exception report the VM has always
    // built and never showed: class, detail message and the backtrace captured at throw time.
    if !report.console.is_empty() {
        eprint!("{}", report.console);
    }
    // A real `java` exits non-zero when the main thread dies with an exception escaping, and until
    // now this exited 0 — so a caller could not distinguish "returned void" from "died". Exit 1,
    // which is what `java` uses, and which the runner already reads as *the program threw* rather
    // than *the VM crashed* (that one is a panic, and it is recognised by its own signature).
    if report.uncaught.is_some() {
        std::process::exit(1);
    }
}
