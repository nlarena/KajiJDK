//! `jdb` — depurador **interactivo** sobre la KajiVM (Hito I1).
//!
//! Es el front-end del stack de depuración, construido sobre el JVMTI mínimo (I0): un
//! [`JvmtiAgent`] cuyos callbacks `breakpoint`/`single_step` **frenan la ejecución** y abren un
//! prompt de comandos. Arranca detenido antes del primer opcode (single-step encendido); desde el
//! prompt se navega:
//!
//! ```text
//!   step | s        ejecutar un opcode
//!   cont | c        continuar hasta el próximo breakpoint
//!   break <pc>      poner un breakpoint en el método actual, offset <pc>
//!   clear <pc>      quitarlo
//!   where | w | bt  la traza de la pila (método @pc por frame)
//!   locals | l      volcar los locales del frame actual (por slot)
//!   print <slot>    un local por su número de slot
//!   help | h | ?    esta ayuda
//!   quit | q        salir
//! ```
//!
//! Uso:  `jdb <File.class> <method> [intArg ...]`  (método estático sobre `int`, como `jvm-step`).
//!
//! El «jdb pragmático»: todo **in-process**, sin JDWP — el prompt corre *dentro* del callback del
//! agente, inspeccionando/controlando la VM por el [`JvmtiEnv`] y devolviendo el control (`step`/
//! `cont`) para que `step()` siga. El «jdb fiel» por protocolo es I3–I4.

use std::io::{self, BufRead, Write};
use std::path::{Path, PathBuf};
use std::process::ExitCode;

use jvm::jvm::class_file::ClassFile;
use jvm::jvm::interpreter::bytecode_interpreter::jvmti::{
    Capabilities, JvmtiAgent, JvmtiEnv, ThreadId,
};
use jvm::jvm::interpreter::bytecode_interpreter::{Step, JVM};
use jvm::jvm::interpreter::frame::{Frame, Value};
use jvm::jvm::interpreter::metaspace::{MetaspaceService, MethodId};

const HELP: &str = "\
  step | s        ejecutar un opcode
  cont | c        continuar hasta el próximo breakpoint
  break <pc>      breakpoint en el método actual, offset <pc>
  clear <pc>      quitar el breakpoint
  where | w | bt  la traza de la pila
  locals | l      los locales del frame actual (por slot)
  print <slot>    un local por su slot
  help | h | ?    esta ayuda
  quit | q        salir
";

/// Lo que un comando le dice al bucle del prompt.
#[derive(Debug, PartialEq, Eq)]
enum Ctl {
    /// Seguir pidiendo comandos (el comando fue de inspección/config).
    Stay,
    /// Devolver el control a la VM — continuar (hasta el próximo breakpoint).
    Resume,
    /// Devolver el control — ejecutar un opcode (el single-step queda encendido).
    Step,
}

/// El depurador: un agente JVMTI cuyos callbacks abren el prompt.
struct Jdb;

impl Jdb {
    /// Interpreta un comando contra el `env` (el frame/pila del punto de parada). Devuelve qué hacer
    /// y el texto a mostrar. **Puro y testeable**: no hace I/O, solo toca el `env` y arma la salida.
    fn command(&mut self, line: &str, env: &mut JvmtiEnv, method: MethodId, _loc: u32) -> (Ctl, String) {
        let mut it = line.split_whitespace();
        let parse_u32 = |it: &mut std::str::SplitWhitespace| it.next().and_then(|s| s.parse::<u32>().ok());
        match it.next().unwrap_or("") {
            "" => (Ctl::Stay, String::new()),
            "step" | "s" => {
                env.set_single_step(true);
                (Ctl::Step, String::new())
            }
            "cont" | "c" => {
                env.set_single_step(false);
                (Ctl::Resume, String::new())
            }
            "where" | "w" | "bt" => {
                let mut out = String::new();
                for (i, (m, pc)) in env.stack_trace().iter().enumerate() {
                    out.push_str(&format!("  #{i}  {} @{pc}\n", env.method_name(*m)));
                }
                (Ctl::Stay, out)
            }
            "locals" | "l" => {
                let mut out = String::new();
                let mut slot = 0u16;
                while let Some(v) = env.local(0, slot) {
                    out.push_str(&format!("  slot {slot} = {}\n", fmt_value(v)));
                    slot += 1;
                }
                if out.is_empty() {
                    out.push_str("  (sin locales)\n");
                }
                (Ctl::Stay, out)
            }
            "print" | "p" => match parse_u32(&mut it).map(|n| n as u16) {
                Some(slot) => match env.local(0, slot) {
                    Some(v) => (Ctl::Stay, format!("  slot {slot} = {}\n", fmt_value(v))),
                    None => (Ctl::Stay, format!("  slot {slot}: no existe\n")),
                },
                None => (Ctl::Stay, "  uso: print <slot>\n".into()),
            },
            "break" | "b" => match parse_u32(&mut it) {
                Some(pc) => {
                    env.set_breakpoint(method, pc);
                    (Ctl::Stay, format!("  breakpoint puesto @{pc} (método actual)\n"))
                }
                None => (Ctl::Stay, "  uso: break <pc>\n".into()),
            },
            "clear" => match parse_u32(&mut it) {
                Some(pc) => {
                    env.clear_breakpoint(method, pc);
                    (Ctl::Stay, format!("  quitado el breakpoint @{pc}\n"))
                }
                None => (Ctl::Stay, "  uso: clear <pc>\n".into()),
            },
            "help" | "h" | "?" => (Ctl::Stay, HELP.into()),
            "quit" | "q" => std::process::exit(0),
            other => (Ctl::Stay, format!("  comando desconocido: `{other}` (probá `help`)\n")),
        }
    }

    /// El bucle del prompt: lee comandos hasta que uno devuelve el control a la VM (`step`/`cont`).
    /// Corre **dentro** del callback del agente, con la VM detenida en `(method, loc)`.
    fn prompt(&mut self, env: &mut JvmtiEnv, method: MethodId, loc: u32) {
        let stdin = io::stdin();
        loop {
            print!("({} @{loc}) ", env.method_name(method));
            let _ = io::stdout().flush();
            let mut line = String::new();
            if stdin.lock().read_line(&mut line).unwrap_or(0) == 0 {
                std::process::exit(0); // EOF (Ctrl-D) → salir
            }
            let (ctl, out) = self.command(line.trim(), env, method, loc);
            print!("{out}");
            let _ = io::stdout().flush();
            match ctl {
                Ctl::Stay => continue,
                Ctl::Resume | Ctl::Step => return,
            }
        }
    }
}

impl JvmtiAgent for Jdb {
    fn vm_init(&mut self, env: &mut JvmtiEnv) {
        // Arrancar **detenido** antes del primer opcode del método de entrada.
        env.set_single_step(true);
        println!("jdb — depurador sobre la KajiVM (I1). `help` para los comandos.");
    }
    fn breakpoint(&mut self, env: &mut JvmtiEnv, _t: ThreadId, m: MethodId, loc: u32) {
        println!("\n● breakpoint: {} @{loc}", env.method_name(m));
        self.prompt(env, m, loc);
    }
    fn single_step(&mut self, env: &mut JvmtiEnv, _t: ThreadId, m: MethodId, loc: u32) {
        self.prompt(env, m, loc);
    }
}

/// Un [`Value`] como texto compacto para el prompt.
fn fmt_value(v: Value) -> String {
    match v {
        Value::Int(n) => format!("int {n}"),
        Value::Long(n) => format!("long {n}"),
        Value::Float(f) => format!("float {f}"),
        Value::Double(d) => format!("double {d}"),
        Value::Reference(0) => "null".to_string(),
        Value::Reference(off) => format!("ref @{off}"),
    }
}

fn main() -> ExitCode {
    let args: Vec<String> = std::env::args().skip(1).collect();
    if args.len() < 2 {
        eprintln!("uso: jdb <File.class> <method> [intArg ...]");
        return ExitCode::FAILURE;
    }
    let path = &args[0];
    let method_name = &args[1];
    let call_args: Vec<Value> = args[2..].iter().map(|a| Value::Int(a.parse().unwrap_or(0))).collect();

    // Parseo de la clase + descriptor del método pedido (el CLI da solo el nombre).
    let class_file = match ClassFile::from_path(path) {
        Ok(cf) => cf,
        Err(e) => {
            eprintln!("jdb: no se pudo leer '{path}': {e}");
            return ExitCode::FAILURE;
        }
    };
    let class_name = match class_file.class_name(class_file.this_class) {
        Some(name) => name.to_string(),
        None => {
            eprintln!("jdb: no se pudo leer el nombre de la clase de {path}");
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
            eprintln!("jdb: el método '{method_name}' no está en {path}");
            return ExitCode::FAILURE;
        }
    };

    // Metaspace: bootstrap `boot/` + el directorio de la clase de entrada.
    let app: Vec<PathBuf> = Path::new(path).parent().map(PathBuf::from).into_iter().collect();
    let mut metaspace = MetaspaceService::new(vec![PathBuf::from("boot")], app);
    metaspace.add(class_name.clone(), class_file);
    let entry = match metaspace.resolve_method(&class_name, method_name, &descriptor) {
        Some(id) => id,
        None => {
            eprintln!("jdb: el método '{method_name}' no tiene Code (¿abstracto o nativo?)");
            return ExitCode::FAILURE;
        }
    };
    let max_locals = metaspace.max_locals(entry);
    let mut jvm = JVM::new(metaspace, Frame::new(entry, max_locals, call_args));

    // Atachear el depurador: quiere breakpoint + single_step; `vm_init` prende el single-step para
    // arrancar detenido.
    jvm.attach_agent(
        Box::new(Jdb),
        Capabilities { breakpoint: true, single_step: true, ..Default::default() },
    );

    // Bucle de ejecución: `step()` corre un opcode y, por los ganchos JVMTI, entra al prompt cuando
    // toca. Termina cuando el método de entrada retorna.
    loop {
        match jvm.step() {
            Step::Return(value) => {
                println!("\n▪ el programa terminó → {value:?}");
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
    fn control_commands_drive_the_step_and_breakpoints() {
        use jvm::jvm::interpreter::bytecode_interpreter::jvmti::{BreakpointTable, FieldWatchTable};
        let mut frames = vec![Frame::new(0, 2, vec![Value::Int(9), Value::Int(4)])];
        let mut bp = BreakpointTable::default();
        let mut ss = false;
        let mut fw = FieldWatchTable::default();
        let ms = MetaspaceService::new(vec![], vec![]);
        let mut env = JvmtiEnv::new(&mut frames, &mut bp, &mut ss, &mut fw, None, &[], &ms);
        let mut jdb = Jdb;

        // `step` → devuelve Step y deja el single-step encendido.
        assert_eq!(jdb.command("step", &mut env, 0, 0).0, Ctl::Step);
        // `break 7` → registra el breakpoint (método 0, pc 7).
        assert_eq!(jdb.command("break 7", &mut env, 0, 0).0, Ctl::Stay);
        // `cont` → Resume y apaga el single-step.
        assert_eq!(jdb.command("cont", &mut env, 0, 0).0, Ctl::Resume);
        drop(env);
        assert!(bp.contains(0, 7), "`break 7` puso el breakpoint");
        assert!(!ss, "`cont` apagó el single-step");
    }

    #[test]
    fn inspection_commands_read_the_frame() {
        use jvm::jvm::interpreter::bytecode_interpreter::jvmti::{BreakpointTable, FieldWatchTable};
        let mut frames = vec![Frame::new(0, 2, vec![Value::Int(9), Value::Int(4)])];
        let mut bp = BreakpointTable::default();
        let mut ss = false;
        let mut fw = FieldWatchTable::default();
        let ms = MetaspaceService::new(vec![], vec![]);
        let mut env = JvmtiEnv::new(&mut frames, &mut bp, &mut ss, &mut fw, None, &[], &ms);
        let mut jdb = Jdb;

        let (ctl, out) = jdb.command("print 0", &mut env, 0, 0);
        assert_eq!(ctl, Ctl::Stay);
        assert!(out.contains("slot 0 = int 9"), "{out}");
        let (_, locals) = jdb.command("locals", &mut env, 0, 0);
        assert!(locals.contains("slot 0 = int 9") && locals.contains("slot 1 = int 4"), "{locals}");
        let (_, unknown) = jdb.command("frobnicate", &mut env, 0, 0);
        assert!(unknown.contains("desconocido"), "{unknown}");
    }
}
