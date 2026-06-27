use std::process::ExitCode;

use jvm::javap::Javap;
use jvm::jvm::class_file::ClassFile;
use jvm::pretty_class_visualizer::PrettyClassVisualizer;

fn main() -> ExitCode {
    let args: Vec<String> = std::env::args().skip(1).collect();
    // Flags, javap-style:
    //   -v / -verbose  -> javap VERBOSE dump (constant pool + bytecode + …)
    //   --javap        -> javap brief listing (signatures only)
    //   (none)         -> our pretty boxed/tree view
    let verbose = args.iter().any(|a| a == "-v" || a == "-verbose");
    let javap_mode = verbose || args.iter().any(|a| a == "--javap");
    // Visibility filter (-public/-protected/-package/-private | -p); default package.
    let visibility = jvm::jvm::parser::printers::Visibility::from_args(&args);
    let path = match args.iter().find(|a| !a.starts_with('-')) {
        Some(p) => p,
        None => {
            eprintln!("usage: jvm [--javap | -v] <file.class>");
            return ExitCode::FAILURE;
        }
    };

    // Parse the whole class file. Errors (missing file, bad magic, truncated…)
    // come back as a ParseError, which we print and turn into a failure code.
    let class_file = match ClassFile::from_path(path) {
        Ok(cf) => cf,
        Err(e) => {
            eprintln!("error parsing '{path}': {e}");
            return ExitCode::FAILURE;
        }
    };

    if javap_mode {
        Javap::new(&class_file, path).run(verbose, visibility);
    } else {
        PrettyClassVisualizer::new(&class_file).print();
    }

    ExitCode::SUCCESS
}
