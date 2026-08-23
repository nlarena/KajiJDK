//! CLI de `javac`. Mientras el back-end (sema + codegen) está en construcción, el binario
//! expone el **front-end** para inspección — el estilo de tooling del proyecto (como
//! `javap`/`jvm-step`):
//!
//! - `javac <archivo.java>`            → parsea y dibuja el **AST** como árbol.
//! - `javac --tokens <archivo.java>`   → vuelca la lista de tokens (lexer).
//! - `javac --symbols <archivo.java>`  → corre la pasada 1 y vuelca la **tabla de símbolos**.
//! - `javac --ast-raw <archivo.java>`  → el AST como volcado `{:#?}` crudo de Rust.
//!
//! Cuando el codegen (B3) esté listo, el modo por defecto pasará a escribir el `.class`.

use std::env;
use std::fs;
use std::process;

fn main() {
    // Se extrae primero un `-cp`/`--classpath <dirs>` (finding #7): directorios de `.class`
    // **antepuestos** al classpath por defecto, para que los tipos ahí (KajiLibrary ya compilado)
    // sombreen al JDK. Los dirs van separados por el separador del SO (`;` en Windows). Lo que queda
    // tras sacarlos se parsea como `[modo] <archivo>`, igual que antes.
    let raw: Vec<String> = env::args().skip(1).collect();
    let mut extra_classpath: Vec<std::path::PathBuf> = Vec::new();
    let mut lint_spec: Option<String> = None;
    // Annotation processing (APT fase 2): `-processor <FQN[,FQN...]>` lista los processors a correr
    // (descubrimiento explícito, no por `META-INF/services` todavía), y `-processorpath <dirs>` es
    // el classpath donde encontrarlos. Ambos se acumulan igual que `-cp`/`-Xlint`.
    let mut processors: Vec<String> = Vec::new();
    let mut processor_path: Vec<std::path::PathBuf> = Vec::new();
    let mut args: Vec<String> = Vec::new();
    let mut it = raw.into_iter();
    while let Some(a) = it.next() {
        if a == "-cp" || a == "--classpath" {
            if let Some(val) = it.next() {
                extra_classpath.extend(env::split_paths(&val));
            }
        } else if a == "-processor" {
            // Nombres separados por coma, en notación con puntos (`com.foo.MyProc`); se pasan a la VM
            // como nombres internos (con `/`).
            if let Some(val) = it.next() {
                processors.extend(
                    val.split(',').filter(|s| !s.is_empty()).map(|s| s.replace('.', "/")),
                );
            }
        } else if a == "-processorpath" {
            if let Some(val) = it.next() {
                processor_path.extend(env::split_paths(&val));
            }
        } else if a == "-Xlint" {
            lint_spec = Some("all".to_string()); // `-Xlint` a secas = todas las categorías
        } else if let Some(spec) = a.strip_prefix("-Xlint:") {
            lint_spec = Some(spec.to_string()); // `-Xlint:cat1,cat2` / `:all` / `:none`
        } else {
            args.push(a);
        }
    }
    // El conjunto de avisos habilitados (`-Xlint`): vacío si no se pasó el flag (opt-in, como `javac`).
    let lint_set = match &lint_spec {
        Some(spec) => jvm::javac::lint::LintSet::from_spec(spec),
        None => jvm::javac::lint::LintSet::none(),
    };
    // Annotation processing (APT fase 2): si se pidieron processors, se corre el *round loop* de
    // JSR 269 antes de la compilación. Es el hito mínimo demostrable — instancia cada processor en
    // la VM, corre `init(env)` y el bucle de rondas (una normal + la final `processingOver`) — y
    // todavía **no** reifica elementos ni alimenta lo generado de vuelta a la compilación.
    if !processors.is_empty() {
        let boot = vec![std::path::PathBuf::from("KajiLibrary"), std::path::PathBuf::from("boot")];
        // El loader de aplicación ve primero el `-processorpath`, luego el `-cp`.
        let mut app = processor_path.clone();
        app.extend(extra_classpath.iter().cloned());
        let outcome = jvm::jvm::interpreter::apt::run_processors(&processors, boot, app);
        print!("{}", outcome.console);
        if let Some(err) = &outcome.error {
            eprintln!("javac: annotation processing falló: {err}");
            process::exit(1);
        }
        eprintln!(
            "javac: annotation processing: {} processor(s), {} ronda(s), {} llamada(s) a process",
            processors.len(),
            outcome.rounds,
            outcome.process_calls
        );
        // Modo "sólo procesar": sin archivo fuente, el round loop es todo lo que había para hacer.
        if args.is_empty() {
            return;
        }
    }

    let (mode, path) = match args.first().map(String::as_str) {
        Some("--tokens") => (Mode::Tokens, args.get(1)),
        Some("--symbols") => (Mode::Symbols, args.get(1)),
        Some("--check") => (Mode::Check, args.get(1)),
        Some("--desugar") => (Mode::Desugar, args.get(1)),
        Some("--emit") => (Mode::Emit, args.get(1)),
        Some("--ast-raw") => (Mode::AstRaw, args.get(1)),
        Some(_) => (Mode::AstTree, args.first()),
        None => (Mode::AstTree, None),
    };
    let Some(input) = path else {
        eprintln!("uso: javac [-cp <dirs>] [--tokens | --symbols | --check | --desugar | --emit | --ast-raw] <archivo.java>");
        process::exit(2);
    };

    let source = fs::read_to_string(input).unwrap_or_else(|err| {
        eprintln!("javac: no se pudo leer {input}: {err}");
        process::exit(1);
    });

    match mode {
        Mode::Tokens => match jvm::javac::lexer::tokenize(&source) {
            Ok(tokens) => print_token_table(&tokens),
            Err(err) => fail(input, &source, err),
        },
        Mode::AstTree => match jvm::javac::parse(&source) {
            Ok(unit) => print!("{}", jvm::javac::ast_view::tree(&unit)),
            Err(err) => fail(input, &source, err),
        },
        Mode::Symbols => match jvm::javac::parse(&source) {
            Ok(unit) => {
                let (table, errors) = jvm::javac::enter::enter_cp(&unit, &extra_classpath);
                eprint!("{}", jvm::javac::render_diagnostics(&errors, &source, input));
                print!("{}", table.dump());
            }
            Err(err) => fail(input, &source, err),
        },
        Mode::AstRaw => match jvm::javac::parse(&source) {
            Ok(unit) => println!("{unit:#?}"),
            Err(err) => fail(input, &source, err),
        },
        Mode::Check => {
            // Los avisos de `-Xlint` (vacíos si no se pasó el flag) van junto a los errores; solo los
            // errores hacen fallar (`exit 1`).
            let warnings =
                jvm::javac::lint_cp(&source, &extra_classpath, &lint_set).unwrap_or_default();
            match jvm::javac::check_cp(&source, &extra_classpath) {
                Ok(errors) => {
                    let mut all = errors.clone();
                    all.extend(warnings);
                    if all.is_empty() {
                        println!("javac: {input} sin errores");
                    } else {
                        eprint!("{}", jvm::javac::render_diagnostics(&all, &source, input));
                        if !errors.is_empty() {
                            process::exit(1);
                        }
                    }
                }
                Err(err) => fail(input, &source, err),
            }
        }
        // Baja el azúcar (tras atribuir, que es de donde saca los tipos) y dibuja el AST resultante.
        Mode::Desugar => match jvm::javac::parse(&source) {
            Ok(mut unit) => {
                let (mut table, _errors) = jvm::javac::enter::enter_cp(&unit, &extra_classpath);
                jvm::javac::attribute::attribute(&mut unit, &table);
                jvm::javac::desugar::desugar(&mut unit, &mut table);
                print!("{}", jvm::javac::ast_view::tree(&unit));
            }
            Err(err) => fail(input, &source, err),
        },
        // Compila y **escribe un `.class` por clase**, cada uno con el nombre de **su** clase (no el
        // del archivo), en el mismo directorio que el fuente. Una unidad con varios tipos, o con
        // clases sintéticas (`C$1` del `switch`-enum), produce varios archivos.
        Mode::Emit => {
            // Los avisos de `-Xlint` se imprimen antes de emitir (no impiden la emisión).
            let warnings =
                jvm::javac::lint_cp(&source, &extra_classpath, &lint_set).unwrap_or_default();
            if !warnings.is_empty() {
                eprint!("{}", jvm::javac::render_diagnostics(&warnings, &source, input));
            }
            match jvm::javac::compile_cp(&source, &extra_classpath) {
            Ok(classes) => {
                for (internal, bytes) in &classes {
                    // El nombre de archivo es el último segmento del nombre interno (`com/foo/A$B`
                    // → `A$B.class`); el paquete no se traduce a subdirectorios.
                    let simple = internal.rsplit('/').next().unwrap_or(internal);
                    let out = std::path::Path::new(input).with_file_name(format!("{simple}.class"));
                    if let Err(err) = fs::write(&out, bytes) {
                        eprintln!("javac: no se pudo escribir {}: {err}", out.display());
                        process::exit(1);
                    }
                    println!("javac: escrito {} ({} bytes)", out.display(), bytes.len());
                }
            }
            Err(err) => fail(input, &source, err),
            }
        }
    }
}

/// Imprime los tokens en el orden en que aparecen en el fuente, como una tabla alineada:
/// índice, posición `línea:col`, [`TokenKind`](jvm::javac::token::TokenKind) y lexema. Los
/// anchos de columna se calculan de los propios datos para que todo quede a plomo.
fn print_token_table(tokens: &[jvm::javac::token::Token]) {
    let digits = |n: u32| n.to_string().len();
    let idx_w = tokens.len().to_string().len().max(1);
    let line_w = tokens.iter().map(|t| digits(t.line)).max().unwrap_or(1);
    let col_w = tokens.iter().map(|t| digits(t.col)).max().unwrap_or(1);
    let pos_w = (line_w + 1 + col_w).max("línea:col".chars().count());
    let kind_w = tokens
        .iter()
        .map(|t| format!("{:?}", t.kind).chars().count())
        .max()
        .unwrap_or(4)
        .max("KIND".len());

    println!("{:>idx_w$}  {:<pos_w$}  {:<kind_w$}  {}", "#", "línea:col", "KIND", "LEXEMA");
    for (i, t) in tokens.iter().enumerate() {
        let pos = format!("{:>line_w$}:{:<col_w$}", t.line, t.col);
        let lexeme = if t.text.is_empty() { "<eof>" } else { &t.text };
        let kind = format!("{:?}", t.kind);
        println!("{:>idx_w$}  {pos:<pos_w$}  {kind:<kind_w$}  {lexeme}", i + 1);
    }
}

enum Mode {
    Tokens,
    Symbols,
    Check,
    Desugar,
    Emit,
    AstTree,
    AstRaw,
}

fn fail(input: &str, source: &str, err: jvm::javac::Error) -> ! {
    eprint!("{}", jvm::javac::render_diagnostics(std::slice::from_ref(&err), source, input));
    process::exit(1);
}
