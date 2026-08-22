//! `jdi-attach` — un cliente de depuración **sobre la API JDI** (Hito I4), del lado del debugger.
//!
//! Se conecta a una VM que expone JDWP (nuestro [`jvm-jdwp`], o cualquier servidor `dt_socket`),
//! attachea con [`Vm`], pide un single-step y **maneja la ejecución por el cable** imprimiendo cada
//! evento tipado que la VM le empuja — hasta que el programa remoto termina. Es la contraparte de alto
//! nivel del `jdb` in-process de I1: acá el front-end vive en **otro proceso** y no sabe nada de bytes
//! de protocolo, solo de *mirrors* (`Vm`/`Location`/`Event`). Reemplaza al cliente hecho a mano.
//!
//! Uso:  `jdi-attach [--port <N>] [host]`   (default `127.0.0.1:5005`).

use std::io::{self, ErrorKind};
use std::net::TcpStream;
use std::process::ExitCode;

use jvm::jvm::interpreter::bytecode_interpreter::jdi::{Event, Location, Vm};
use jvm::jvm::interpreter::bytecode_interpreter::jdwp::suspend_policy;

fn main() -> ExitCode {
    // Args: [--port N] [--break <line>] [host].
    let mut port = 5005u16;
    let mut host = "127.0.0.1".to_string();
    let mut break_line: Option<i32> = None;
    let mut it = std::env::args().skip(1);
    while let Some(arg) = it.next() {
        match arg.as_str() {
            "--port" => match it.next().and_then(|n| n.parse().ok()) {
                Some(p) => port = p,
                None => {
                    eprintln!("jdi-attach: --port necesita un número");
                    return ExitCode::FAILURE;
                }
            },
            "--break" => match it.next().and_then(|n| n.parse().ok()) {
                Some(line) => break_line = Some(line),
                None => {
                    eprintln!("jdi-attach: --break necesita un número de línea");
                    return ExitCode::FAILURE;
                }
            },
            other => host = other.to_string(),
        }
    }
    let addr = format!("{host}:{port}");

    let stream = match TcpStream::connect(&addr) {
        Ok(s) => s,
        Err(e) => {
            eprintln!("jdi-attach: no se pudo conectar a {addr}: {e}");
            return ExitCode::FAILURE;
        }
    };
    let mut vm = match Vm::attach(stream) {
        Ok(vm) => vm,
        Err(e) => {
            eprintln!("jdi-attach: handshake fallido: {e}");
            return ExitCode::FAILURE;
        }
    };
    println!("jdi-attach: conectado a {addr}");

    match vm.version() {
        Ok(v) => println!("jdi-attach: VM remota → {v:?}"),
        Err(e) => {
            eprintln!("jdi-attach: Version falló: {e}");
            return ExitCode::FAILURE;
        }
    }

    // Modo breakpoint-por-línea (I5b) o single-step (I5a), según `--break`.
    if let Some(line) = break_line {
        match resolve_and_break(&mut vm, line) {
            Ok(true) => {}
            Ok(false) => return ExitCode::FAILURE, // no se encontró la línea (ya se avisó)
            Err(e) => {
                eprintln!("jdi-attach: resolución del breakpoint falló: {e}");
                return ExitCode::FAILURE;
            }
        }
    } else {
        match vm.set_step_request(suspend_policy::ALL) {
            Ok(r) => println!("jdi-attach: single-step pedido (requestID {})", r.id),
            Err(e) => {
                eprintln!("jdi-attach: no se pudo pedir el single-step: {e}");
                return ExitCode::FAILURE;
            }
        }
    }
    if let Err(e) = vm.resume() {
        eprintln!("jdi-attach: Resume inicial falló: {e}");
        return ExitCode::FAILURE;
    }

    // Bucle de eventos: por cada evento que la VM empuja, lo mostramos y reanudamos. Cuando el programa
    // remoto termina, la VM cierra el socket y `next_event` devuelve EOF.
    let mut count = 0usize;
    loop {
        match vm.next_event() {
            Ok(Event::VmStart { thread, .. }) => {
                // Evento automático de arranque: la VM está suspendida antes del primer opcode. La
                // soltamos para que corra hasta el próximo evento (single-step o breakpoint).
                println!("jdi-attach: VM iniciada (hilo {thread}), soltando…");
                if let Err(e) = vm.resume() {
                    eprintln!("jdi-attach: Resume tras VMStart falló: {e}");
                    return ExitCode::FAILURE;
                }
            }
            Ok(event) => {
                count += 1;
                let thread = match event {
                    Event::SingleStep { request_id, thread, location } => {
                        println!(
                            "jdi-attach: [step #{count}] req={request_id} thread={thread} method={} index={}",
                            location.method, location.index
                        );
                        thread
                    }
                    Event::Breakpoint { request_id, thread, location } => {
                        println!("jdi-attach: [breakpoint] req={request_id} @ method={} index={}", location.method, location.index);
                        thread
                    }
                    Event::Exception { thread, location, exception, .. } => {
                        println!("jdi-attach: [exception] obj={exception} @ method={} index={}", location.method, location.index);
                        thread
                    }
                    other => {
                        println!("jdi-attach: [evento] {other:?}");
                        0
                    }
                };
                // I5a: con la VM parada, inspeccionamos la pila y los locales por la API JDI.
                if let Ok(frames) = vm.frames(thread) {
                    let depths: Vec<String> = frames
                        .iter()
                        .map(|f| format!("#{} m{}@{}", f.id, f.location.method, f.location.index))
                        .collect();
                    print!("jdi-attach:   pila [{}]", depths.join(" "));
                    // los locales del frame tope (slots 0 y 1, como int).
                    if let Some(top) = frames.first() {
                        let mut locals = Vec::new();
                        for slot in 0..2 {
                            if let Ok(v) = vm.get_value(thread, top.id, slot, b'I') {
                                locals.push(format!("slot{slot}={v:?}"));
                            }
                        }
                        if !locals.is_empty() {
                            print!("  locales {{{}}}", locals.join(", "));
                        }
                    }
                    println!();
                }
                if let Err(e) = vm.resume() {
                    eprintln!("jdi-attach: Resume falló: {e}");
                    return ExitCode::FAILURE;
                }
            }
            Err(e) if e.kind() == ErrorKind::UnexpectedEof => {
                println!("jdi-attach: la VM remota terminó (tras {count} eventos)");
                return ExitCode::SUCCESS;
            }
            Err(e) => {
                eprintln!("jdi-attach: error leyendo eventos: {e}");
                return ExitCode::FAILURE;
            }
        }
    }
}

/// Resuelve una **línea de fuente** a `(methodID, índice)` recorriendo la metadata de la VM (I5b) —
/// `AllClasses → Methods → LineTable`— y pone un breakpoint ahí. `Ok(true)` si lo puso; `Ok(false)` si
/// la línea no existe (imprime las disponibles para orientar). Es la cadena que un `jdb`/IDE hace para
/// un breakpoint por línea, escrita sobre la API JDI.
fn resolve_and_break(vm: &mut Vm<TcpStream>, line: i32) -> io::Result<bool> {
    println!("jdi-attach: resolviendo un breakpoint en la línea {line}…");
    let mut target: Option<(String, Location)> = None;
    let mut available: Vec<String> = Vec::new();
    for class in vm.all_classes()? {
        for method in vm.methods(class.id)? {
            let lines = vm.line_table(class.id, method.id)?;
            if !lines.is_empty() {
                let nums: Vec<i32> = lines.iter().map(|(_, ln)| *ln).collect();
                available.push(format!("{}.{} líneas {nums:?}", class.signature, method.name));
            }
            for (index, ln) in lines {
                if ln == line && target.is_none() {
                    let label = format!("{}.{}", class.signature, method.name);
                    target = Some((label, Location { method: method.id, index }));
                }
            }
        }
    }
    match target {
        Some((label, loc)) => {
            let req = vm.set_breakpoint(loc, suspend_policy::ALL)?;
            println!(
                "jdi-attach: breakpoint en {label}:{line} → method={} index={} (requestID {})",
                loc.method, loc.index, req.id
            );
            Ok(true)
        }
        None => {
            eprintln!("jdi-attach: no hay línea {line} en las clases cargadas. Disponibles:");
            for entry in available {
                eprintln!("  {entry}");
            }
            Ok(false)
        }
    }
}
