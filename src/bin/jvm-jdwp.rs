//! `jvm-jdwp` — la KajiVM como **servidor de depuración** JDWP (Hito I3, end-to-end).
//!
//! Corre un método como `jvm-step`/`jdb`, pero en vez del prompt in-process de I1 **expone la VM por
//! un socket**: es el equivalente a arrancar un JDK con
//! `-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=<port>`. Un cliente JDWP (un `jdb`
//! real, un IDE) se conecta, hace el handshake, pone breakpoints y maneja la ejecución por protocolo.
//!
//! El armado es el stack de I3 de punta a punta:
//!
//! ```text
//!   TcpListener::bind → accept → jdwp::handshake → JVM::attach_agent(JdwpBridge sobre el socket)
//!                                                        │
//!    vm_init del bridge sirve la config inicial (Set breakpoints + Resume) y suelta la VM;
//!    el loop de step() dispara los callbacks del bridge, que empujan eventos y sirven comandos.
//! ```
//!
//! Uso:  `jvm-jdwp <File.class> <method> [--port <N>] [intArg ...]`   (default port 5005).
//!
//! La VM arranca **suspendida** (server `suspend=y`): `attach_agent` corre `vm_init`, que bloquea
//! sirviendo comandos hasta el primer `VirtualMachine.Resume` del cliente.

use std::net::TcpListener;
use std::path::{Path, PathBuf};
use std::process::ExitCode;

use jvm::jvm::class_file::ClassFile;
use jvm::jvm::interpreter::bytecode_interpreter::bridge::JdwpBridge;
use jvm::jvm::interpreter::bytecode_interpreter::debug_info::VmSnapshot;
use jvm::jvm::interpreter::bytecode_interpreter::jdwp;
use jvm::jvm::interpreter::bytecode_interpreter::jvmti::Capabilities;
use jvm::jvm::interpreter::bytecode_interpreter::{Step, JVM};
use jvm::jvm::interpreter::frame::{Frame, Value};
use jvm::jvm::interpreter::metaspace::MetaspaceService;

/// Los argumentos ya parseados: el puerto (sacado del `--port N`) y el resto posicional.
struct Args {
    port: u16,
    positional: Vec<String>,
}

/// Saca un `--port <N>` de la línea (default 5005); lo demás queda posicional, en orden.
fn parse_args(raw: Vec<String>) -> Result<Args, String> {
    let mut port = 5005u16;
    let mut positional = Vec::new();
    let mut it = raw.into_iter();
    while let Some(arg) = it.next() {
        if arg == "--port" {
            let n = it.next().ok_or("--port necesita un número")?;
            port = n.parse().map_err(|_| format!("puerto inválido: {n}"))?;
        } else {
            positional.push(arg);
        }
    }
    Ok(Args { port, positional })
}

fn main() -> ExitCode {
    let args = match parse_args(std::env::args().skip(1).collect()) {
        Ok(a) => a,
        Err(e) => {
            eprintln!("jvm-jdwp: {e}");
            return ExitCode::FAILURE;
        }
    };
    if args.positional.len() < 2 {
        eprintln!("uso: jvm-jdwp <File.class> <method> [--port <N>] [intArg ...]");
        return ExitCode::FAILURE;
    }
    let path = &args.positional[0];
    let method_name = &args.positional[1];
    let call_args: Vec<Value> =
        args.positional[2..].iter().map(|a| Value::Int(a.parse().unwrap_or(0))).collect();

    // --- Resolver la clase + el método pedido (igual que jdb) ---
    let class_file = match ClassFile::from_path(path) {
        Ok(cf) => cf,
        Err(e) => {
            eprintln!("jvm-jdwp: no se pudo leer '{path}': {e}");
            return ExitCode::FAILURE;
        }
    };
    let class_name = match class_file.class_name(class_file.this_class) {
        Some(name) => name.to_string(),
        None => {
            eprintln!("jvm-jdwp: no se pudo leer el nombre de la clase de {path}");
            return ExitCode::FAILURE;
        }
    };
    let descriptor = class_file
        .methods
        .iter()
        .find(|m| class_file.utf8(m.name_index) == Some(method_name.as_str()))
        .and_then(|m| class_file.utf8(m.descriptor_index))
        .map(str::to_string);
    let descriptor = match descriptor {
        Some(d) => d,
        None => {
            eprintln!("jvm-jdwp: el método '{method_name}' no está en {path}");
            return ExitCode::FAILURE;
        }
    };

    let app: Vec<PathBuf> = Path::new(path).parent().map(PathBuf::from).into_iter().collect();
    let mut metaspace = MetaspaceService::new(vec![PathBuf::from("boot")], app);
    metaspace.add(class_name.clone(), class_file);
    let entry = match metaspace.resolve_method(&class_name, method_name, &descriptor) {
        Some(id) => id,
        None => {
            eprintln!("jvm-jdwp: el método '{method_name}' no tiene Code (¿abstracto o nativo?)");
            return ExitCode::FAILURE;
        }
    };
    let max_locals = metaspace.max_locals(entry);

    // Capturamos la metadata de depuración (clases/métodos/líneas) AHORA, con `&mut metaspace`
    // disponible — después la VM la mueve y queda inmutable. Es lo que deja resolver breakpoints por
    // línea (I5b): el `methodID` del snapshot es el mismo que la VM usa al correr.
    let snapshot = VmSnapshot::capture(&mut metaspace);

    let mut jvm = JVM::new(metaspace, Frame::new(entry, max_locals, call_args));

    // --- Transporte: escuchar, aceptar UNA conexión, handshake ---
    let addr = format!("127.0.0.1:{}", args.port);
    let listener = match TcpListener::bind(&addr) {
        Ok(l) => l,
        Err(e) => {
            eprintln!("jvm-jdwp: no se pudo escuchar en {addr}: {e}");
            return ExitCode::FAILURE;
        }
    };
    println!("jvm-jdwp: escuchando en {addr} (dt_socket, server=y, suspend=y) — esperando debugger…");
    let stream = match listener.accept() {
        Ok((s, peer)) => {
            println!("jvm-jdwp: debugger conectado desde {peer}");
            s
        }
        Err(e) => {
            eprintln!("jvm-jdwp: fallo al aceptar la conexión: {e}");
            return ExitCode::FAILURE;
        }
    };

    // El handshake usa dos handles (lector/escritor); un TcpStream lee y escribe sobre el mismo socket,
    // así que clonamos solo para el handshake y le damos el original al bridge (que lo usa a dos manos).
    let mut writer = match stream.try_clone() {
        Ok(w) => w,
        Err(e) => {
            eprintln!("jvm-jdwp: no se pudo clonar el socket: {e}");
            return ExitCode::FAILURE;
        }
    };
    let mut reader = stream;
    if let Err(e) = jdwp::handshake(&mut reader, &mut writer) {
        eprintln!("jvm-jdwp: handshake JDWP fallido: {e}");
        return ExitCode::FAILURE;
    }
    drop(writer); // el bridge lee y escribe por su propio handle
    println!("jvm-jdwp: handshake ok — cediendo el control al debugger");

    // --- Atachear el bridge: el evento frena la VM y viaja al cliente por el socket ---
    // `attach_agent` corre `vm_init`, que bloquea sirviendo la config inicial hasta el primer Resume.
    jvm.attach_agent(
        Box::new(JdwpBridge::with_snapshot(reader, snapshot)),
        Capabilities {
            breakpoint: true,
            single_step: true,
            exception: true,
            field_access: true,
            field_modification: true,
            ..Default::default()
        },
    );

    // El loop de ejecución: cada `step()` corre un opcode; por los ganchos JVMTI, el bridge empuja
    // eventos y sirve comandos cuando toca. Termina cuando el método de entrada retorna.
    loop {
        match jvm.step() {
            Step::Return(value) => {
                println!("jvm-jdwp: el programa terminó → {value:?}");
                return ExitCode::SUCCESS;
            }
            Step::Continue => {}
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parse_args_pulls_the_port_and_keeps_the_rest_in_order() {
        let a = parse_args(
            ["Add.class", "add", "--port", "6001", "3", "4"].map(String::from).to_vec(),
        )
        .unwrap();
        assert_eq!(a.port, 6001);
        assert_eq!(a.positional, vec!["Add.class", "add", "3", "4"]);
    }

    #[test]
    fn parse_args_defaults_the_port_to_5005() {
        let a = parse_args(["Add.class", "add"].map(String::from).to_vec()).unwrap();
        assert_eq!(a.port, 5005);
        assert_eq!(a.positional, vec!["Add.class", "add"]);
    }

    #[test]
    fn parse_args_rejects_a_bad_port() {
        assert!(parse_args(["A.class", "m", "--port", "abc"].map(String::from).to_vec()).is_err());
        assert!(parse_args(["A.class", "m", "--port"].map(String::from).to_vec()).is_err());
    }
}
