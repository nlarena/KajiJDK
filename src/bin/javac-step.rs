//! `javac-step`: visor paso a paso del **pipeline del compilador**. Corre las fases sobre un
//! `.java` y **pausa entre pasadas** — cada Enter avanza a la siguiente y muestra lo que
//! produjo:
//!
//! 1. **Lexer** → la lista de tokens.
//! 2. **Parser** → el AST (árbol).
//! 3. **Enter/MemberEnter** (pasada 1) → la tabla de símbolos + errores.
//! 4. **Attribute** (pasada 2) → el chequeo de tipos de los cuerpos + errores.
//!
//! Uso:  javac-step <archivo.java>

use std::io::{BufRead, Write};
use std::process::ExitCode;

use jvm::javac::token::Token;
use jvm::javac::Error;
use jvm::javac::{ast_view, attribute, enter, lexer, parser};

const CLEAR: &str = "\x1b[2J\x1b[3J\x1b[H";
const BOLD: &str = "\x1b[1m";
const YELLOW: &str = "\x1b[1;93m";
const GREEN: &str = "\x1b[92m";
const RED: &str = "\x1b[91m";
const DIM: &str = "\x1b[90m";
const RESET: &str = "\x1b[0m";

const STAGES: [&str; 4] = ["Lexer", "Parser", "Enter · pasada 1", "Attribute · pasada 2"];

fn main() -> ExitCode {
    enable_ansi();
    let Some(path) = std::env::args().nth(1) else {
        eprintln!("uso: javac-step <archivo.java>");
        return ExitCode::FAILURE;
    };
    let source = match std::fs::read_to_string(&path) {
        Ok(s) => s,
        Err(e) => {
            eprintln!("no se pudo leer {path}: {e}");
            return ExitCode::FAILURE;
        }
    };

    // Fase 1 — Lexer.
    screen(0, "texto .java → tokens");
    let tokens = match lexer::tokenize(&source) {
        Ok(t) => t,
        Err(e) => return fail(&path, e),
    };
    print_tokens(&tokens);
    pause();

    // Fase 2 — Parser.
    screen(1, "tokens → AST");
    let (mut unit, parse_errs) = parser::parse(tokens);
    print!("{}", ast_view::tree(&unit));
    print_errors(&path, "parser", &parse_errs);
    pause();

    // Fase 3 — Enter (pasada 1).
    screen(2, "declaraciones → tabla de símbolos + grafo tipado");
    let (table, errs1) = enter::enter(&unit);
    print!("{}", table.dump_table());
    print_errors(&path, "pasada 1", &errs1);
    pause();

    // Fase 4 — Attribute (pasada 2).
    screen(3, "resolver + tipar los cuerpos");
    let errs2 = attribute::attribute(&mut unit, &table);
    print_errors(&path, "pasada 2", &errs2);
    if errs1.is_empty() && errs2.is_empty() {
        println!("{GREEN}✓ sin errores semánticos{RESET}");
    }
    println!("\n{DIM}[fin del pipeline · Enter para salir]{RESET}  ");
    let _ = std::io::stdout().flush();
    wait_for_enter();
    ExitCode::SUCCESS
}

/// Limpia la pantalla y dibuja el breadcrumb de las 4 fases (hechas en verde, actual en
/// amarillo, pendientes en gris) + el título de la fase actual.
fn screen(current: usize, subtitle: &str) {
    print!("{CLEAR}");
    let crumbs: Vec<String> = STAGES
        .iter()
        .enumerate()
        .map(|(i, s)| {
            if i < current {
                format!("{GREEN}✓ {s}{RESET}")
            } else if i == current {
                format!("{YELLOW}▶ {s}{RESET}")
            } else {
                format!("{DIM}{s}{RESET}")
            }
        })
        .collect();
    println!("{}", crumbs.join(&format!("{DIM}  ›  {RESET}")));
    println!("\n{BOLD}Fase {}: {}{RESET}  {DIM}— {subtitle}{RESET}\n", current + 1, STAGES[current]);
}

fn print_tokens(tokens: &[Token]) {
    for (i, t) in tokens.iter().enumerate() {
        let lex = if t.text.is_empty() { "<eof>" } else { &t.text };
        println!("{:>3}  {DIM}{:>3}:{:<3}{RESET} {:?}  {lex}", i + 1, t.line, t.col, t.kind);
    }
}

fn print_errors(path: &str, fase: &str, errors: &[Error]) {
    if errors.is_empty() {
        println!("\n{GREEN}✓ {fase}: sin errores{RESET}");
    } else {
        println!("\n{RED}✗ {fase}: {} error(es){RESET}", errors.len());
        for e in errors {
            println!("  {RED}{path}:{e}{RESET}");
        }
    }
}

fn pause() {
    print!("\n{DIM}[Enter] siguiente pasada…{RESET}  ");
    let _ = std::io::stdout().flush();
    wait_for_enter();
}

fn fail(path: &str, e: Error) -> ExitCode {
    eprintln!("{RED}{path}:{e}{RESET}");
    ExitCode::FAILURE
}

fn wait_for_enter() {
    let mut line = String::new();
    let _ = std::io::stdin().lock().read_line(&mut line);
}

/// Habilita el procesamiento de secuencias ANSI en la consola clásica de Windows.
#[cfg(windows)]
fn enable_ansi() {
    const STD_OUTPUT_HANDLE: u32 = -11i32 as u32;
    const ENABLE_VIRTUAL_TERMINAL_PROCESSING: u32 = 0x0004;
    #[link(name = "kernel32")]
    extern "system" {
        fn GetStdHandle(n_std_handle: u32) -> *mut core::ffi::c_void;
        fn GetConsoleMode(h: *mut core::ffi::c_void, mode: *mut u32) -> i32;
        fn SetConsoleMode(h: *mut core::ffi::c_void, mode: u32) -> i32;
    }
    unsafe {
        let handle = GetStdHandle(STD_OUTPUT_HANDLE);
        let mut mode = 0u32;
        if GetConsoleMode(handle, &mut mode) != 0 {
            SetConsoleMode(handle, mode | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
        }
    }
}

#[cfg(not(windows))]
fn enable_ansi() {}
