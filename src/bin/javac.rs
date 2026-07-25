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
    let args: Vec<String> = env::args().collect();
    let (mode, path) = match args.get(1).map(String::as_str) {
        Some("--tokens") => (Mode::Tokens, args.get(2)),
        Some("--symbols") => (Mode::Symbols, args.get(2)),
        Some("--check") => (Mode::Check, args.get(2)),
        Some("--desugar") => (Mode::Desugar, args.get(2)),
        Some("--emit") => (Mode::Emit, args.get(2)),
        Some("--ast-raw") => (Mode::AstRaw, args.get(2)),
        Some(_) => (Mode::AstTree, args.get(1)),
        None => (Mode::AstTree, None),
    };
    let Some(input) = path else {
        eprintln!("uso: javac [--tokens | --symbols | --check | --desugar | --emit | --ast-raw] <archivo.java>");
        process::exit(2);
    };

    let source = fs::read_to_string(input).unwrap_or_else(|err| {
        eprintln!("javac: no se pudo leer {input}: {err}");
        process::exit(1);
    });

    match mode {
        Mode::Tokens => match jvm::javac::lexer::tokenize(&source) {
            Ok(tokens) => print_token_table(&tokens),
            Err(err) => fail(input, err),
        },
        Mode::AstTree => match jvm::javac::parse(&source) {
            Ok(unit) => print!("{}", jvm::javac::ast_view::tree(&unit)),
            Err(err) => fail(input, err),
        },
        Mode::Symbols => match jvm::javac::parse(&source) {
            Ok(unit) => {
                let (table, errors) = jvm::javac::enter::enter(&unit);
                for err in &errors {
                    eprintln!("{input}: {err}");
                }
                print!("{}", table.dump());
            }
            Err(err) => fail(input, err),
        },
        Mode::AstRaw => match jvm::javac::parse(&source) {
            Ok(unit) => println!("{unit:#?}"),
            Err(err) => fail(input, err),
        },
        Mode::Check => match jvm::javac::check(&source) {
            Ok(errors) if errors.is_empty() => println!("javac: {input} sin errores"),
            Ok(errors) => {
                for err in &errors {
                    eprintln!("{input}:{err}");
                }
                process::exit(1);
            }
            Err(err) => fail(input, err),
        },
        // Baja el azúcar (tras atribuir, que es de donde saca los tipos) y dibuja el AST resultante.
        Mode::Desugar => match jvm::javac::parse(&source) {
            Ok(mut unit) => {
                let (mut table, _errors) = jvm::javac::enter::enter(&unit);
                jvm::javac::attribute::attribute(&mut unit, &table);
                jvm::javac::desugar::desugar(&mut unit, &mut table);
                print!("{}", jvm::javac::ast_view::tree(&unit));
            }
            Err(err) => fail(input, err),
        },
        // Compila y **escribe un `.class` por clase**, cada uno con el nombre de **su** clase (no el
        // del archivo), en el mismo directorio que el fuente. Una unidad con varios tipos, o con
        // clases sintéticas (`C$1` del `switch`-enum), produce varios archivos.
        Mode::Emit => match jvm::javac::compile(&source) {
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
            Err(err) => fail(input, err),
        },
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

fn fail(input: &str, err: jvm::javac::Error) -> ! {
    eprintln!("{input}:{err}");
    process::exit(1);
}
