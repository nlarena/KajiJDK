//! The **native bridge** — the escape hatch from bytecode to real native code.
//!
//! A `native` method has no `Code`; the interpreter, instead of pushing a frame,
//! calls in here. This is where the JVM reaches the outside world (I/O, the OS) —
//! the things Java can't do itself. In a real JVM these are C/C++ via JNI; ours are
//! Rust functions matched by `(class, name, descriptor)`.
//!
//! Right now there's one: `PrintStream.println(int)`, so `System.out.println(n)`
//! prints for real — the wall the whole interpreter has been building toward.

use std::cell::RefCell;
use std::fmt::Write;

use crate::jvm::class_file::ClassFile;

use super::apt::AptContext;
use super::bytecode_interpreter::annotation_factory::{self, Element};
use crate::jvm::parser::attributes::annotations::{self, ResolvedAnnotation};
use super::bytecode_interpreter::objects_operations::{
    allocate_old, field_offset, HEADER_SIZE, SLOT_SIZE,
};
use super::bytecode_interpreter::{array_operations, class_operations, objects_operations};
use super::frame::Value;
use super::heap::HeapService;
use super::metaspace::MetaspaceService;
use super::strings;

// --- APT fase 4: el canal lateral del Filer -------------------------------------------------------
//
// El `Filer` de un procesador de anotaciones fabrica archivos fuente en tiempo de ejecución. Cada
// `createSourceFile(name)` que hace el procesador crea un `StringWriter` que recibe el texto
// generado, y necesita quedar **registrado** para que el round loop lo drene una vez que el
// procesador retorna. La bytecode no puede escribir directamente en estado del compilador, así que
// `KajiFiler.nativeRegisterSourceFile` empuja acá `(nombre, offset del StringWriter en el heap)`.
//
// Es un `thread_local` a propósito: el intérprete verde corre en un solo hilo, y cada test de
// `cargo test` (que corren en paralelo, un hilo por test) ve su propio Filer sin pisarse con otro.

/// Lo que un procesador registró vía su `Filer`: los archivos fuente pendientes, en orden de
/// creación, cada uno como `(nombre, writer_ref)` donde `writer_ref` es el offset en el heap del
/// `StringWriter` que acumula su texto.
#[derive(Default)]
pub struct FilerState {
    pub pending: Vec<(String, u32)>,
}

/// Un proceso hijo lanzado por `ProcessBuilder.start()`: el `Child` de Rust y su codigo de salida
/// una vez que se supo.
///
/// La salida se **memoiza** porque `wait()` de Rust solo se puede llamar una vez -- la segunda
/// devuelve error, y el contrato de `Process.waitFor()` es que se puede llamar todas las veces que
/// uno quiera y siempre da lo mismo.
struct ProcState {
    hijo: std::process::Child,
    salida: Option<i32>,
    /// Si el hijo se lanzo con `redirectErrorStream`, o sea que su error tiene que salir por el
    /// mismo flujo que su salida.
    ///
    /// La union se hace **al leer** y no al lanzar, y eso es una limitacion que conviene tener
    /// escrita: `Stdio::piped()` dos veces crea **dos tuberias distintas**, y desde Rust no hay forma
    /// de duplicar un descriptor para que las dos puntas sean la misma. La primera version duplicaba
    /// el destino y por eso el error nunca aparecia en la salida.
    ///
    /// Lo que se pierde con unir al leer es el **entrelazado**: se entrega toda la salida y despues
    /// todo el error, en vez de mezclados en el orden en que el hijo los escribio. El contenido esta
    /// completo; el orden entre los dos flujos no se preserva. Se dice aca y en el javadoc de
    /// `ProcessBuilder.redirectErrorStream`.
    unir_error: bool,
}



/// La costura cruda con los sockets del sistema.
///
/// `std::net` cubre casi todo lo que hace falta, pero **tres cosas que `java.net` promete no las
/// expone**, y ninguna se puede rodear desde arriba:
///
///  - **Atar antes de conectar.** `TcpStream::connect` toma destino y nada mas; los constructores
///    `Socket(host, port, localAddr, localPort)` prometen elegir tambien la punta local. Es la misma
///    capacidad que necesita `InetAddress.isReachable(NetworkInterface, ...)` para probar **por esa
///    placa** y no por la que el sistema elija.
///  - **El TTL de una conexion saliente**, que es el otro parametro de ese `isReachable`.
///  - **Mandar un byte fuera de banda** (`sendUrgentData`), que no es escribir en el flujo: es una
///    bandera del protocolo.
///
/// Se declara a mano, como `GetDiskFreeSpaceExW` mas abajo, y **no se inventa nada**: son las
/// llamadas de siempre --`socket`, `bind`, `connect`, `setsockopt`, `send`-- con los numeros que
/// cada sistema les da. Lo unico que cambia entre plataformas son esos numeros y el tipo del
/// descriptor, asi que cada `cfg` define su tabla y sus envoltorios, y la logica de arriba es una
/// sola.
///
/// El socket que sale de aca se le entrega a `std` con `from_raw_socket`/`from_raw_fd`: desde ese
/// momento lo administra `TcpStream` --lo cierra al soltarlo-- y el resto del archivo no se entera
/// de que nacio distinto.
#[cfg(windows)]
mod crudo {
    use std::os::windows::io::{AsRawSocket, FromRawSocket};

    pub const AF_INET: u16 = 2;
    pub const AF_INET6: u16 = 23;
    const SOCK_STREAM: i32 = 1;
    const IPPROTO_TCP: i32 = 6;
    const IPPROTO_IP: i32 = 0;
    const IP_TTL: i32 = 4;
    const MSG_OOB: i32 = 1;

    type Descriptor = usize;
    const INVALIDO: Descriptor = usize::MAX;

    extern "system" {
        fn socket(af: i32, tipo: i32, protocolo: i32) -> Descriptor;
        fn bind(s: Descriptor, nombre: *const u8, largo: i32) -> i32;
        fn connect(s: Descriptor, nombre: *const u8, largo: i32) -> i32;
        fn setsockopt(s: Descriptor, nivel: i32, opcion: i32, valor: *const u8, largo: i32) -> i32;
        fn send(s: Descriptor, buf: *const u8, largo: i32, banderas: i32) -> i32;
        fn closesocket(s: Descriptor) -> i32;
    }

    /// Un socket TCP de esa familia, o `None`.
    ///
    /// Antes de la primera llamada se fuerza a que `std` arranque Winsock: en Windows nada de esta
    /// biblioteca funciona hasta que alguien llamo a `WSAStartup`, y `std` lo hace la primera vez
    /// que crea un socket suyo. Abrir y soltar un UDP es la forma mas corta de pedirselo sin
    /// replicar la estructura `WSADATA`, que es lo unico que se ganaria haciendolo a mano.
    pub fn nuevo(familia: u16) -> Option<Descriptor> {
        static ARRANQUE: std::sync::Once = std::sync::Once::new();
        ARRANQUE.call_once(|| {
            let _ = std::net::UdpSocket::bind("127.0.0.1:0");
        });
        let s = unsafe { socket(familia as i32, SOCK_STREAM, IPPROTO_TCP) };
        if s == INVALIDO {
            None
        } else {
            Some(s)
        }
    }

    pub fn atar(s: Descriptor, dir: &[u8]) -> bool {
        unsafe { bind(s, dir.as_ptr(), dir.len() as i32) == 0 }
    }

    pub fn conectar(s: Descriptor, dir: &[u8]) -> bool {
        unsafe { connect(s, dir.as_ptr(), dir.len() as i32) == 0 }
    }

    pub fn poner_ttl(s: Descriptor, ttl: u32) -> bool {
        let v = ttl.to_ne_bytes();
        unsafe { setsockopt(s, IPPROTO_IP, IP_TTL, v.as_ptr(), v.len() as i32) == 0 }
    }

    pub fn cerrar(s: Descriptor) {
        unsafe {
            closesocket(s);
        }
    }

    /// Le entrega el socket a `std`, que desde ahora lo administra.
    pub fn adoptar(s: Descriptor) -> std::net::TcpStream {
        unsafe { std::net::TcpStream::from_raw_socket(s as std::os::windows::io::RawSocket) }
    }

    /// Un byte **fuera de banda** por un flujo que ya existe.
    pub fn fuera_de_banda(flujo: &std::net::TcpStream, b: u8) -> bool {
        let s = flujo.as_raw_socket() as Descriptor;
        unsafe { send(s, &b, 1, MSG_OOB) == 1 }
    }
}

#[cfg(not(windows))]
mod crudo {
    use std::os::fd::{AsRawFd, FromRawFd};

    pub const AF_INET: u16 = 2;
    pub const AF_INET6: u16 = 10;
    const SOCK_STREAM: i32 = 1;
    const IPPROTO_TCP: i32 = 6;
    const IPPROTO_IP: i32 = 0;
    // El numero de `IP_TTL` **no** es el mismo que en Windows: alla es 4 y aca 2. Es el unico de la
    // tabla que no coincide, y por eso esta escrito dos veces en vez de compartirse.
    const IP_TTL: i32 = 2;
    const MSG_OOB: i32 = 1;

    type Descriptor = i32;
    const INVALIDO: Descriptor = -1;

    extern "C" {
        fn socket(af: i32, tipo: i32, protocolo: i32) -> Descriptor;
        fn bind(s: Descriptor, nombre: *const u8, largo: u32) -> i32;
        fn connect(s: Descriptor, nombre: *const u8, largo: u32) -> i32;
        fn setsockopt(s: Descriptor, nivel: i32, opcion: i32, valor: *const u8, largo: u32) -> i32;
        fn send(s: Descriptor, buf: *const u8, largo: usize, banderas: i32) -> isize;
        fn close(s: Descriptor) -> i32;
    }

    pub fn nuevo(familia: u16) -> Option<Descriptor> {
        let s = unsafe { socket(familia as i32, SOCK_STREAM, IPPROTO_TCP) };
        if s == INVALIDO {
            None
        } else {
            Some(s)
        }
    }

    pub fn atar(s: Descriptor, dir: &[u8]) -> bool {
        unsafe { bind(s, dir.as_ptr(), dir.len() as u32) == 0 }
    }

    pub fn conectar(s: Descriptor, dir: &[u8]) -> bool {
        unsafe { connect(s, dir.as_ptr(), dir.len() as u32) == 0 }
    }

    pub fn poner_ttl(s: Descriptor, ttl: u32) -> bool {
        let v = ttl.to_ne_bytes();
        unsafe { setsockopt(s, IPPROTO_IP, IP_TTL, v.as_ptr(), v.len() as u32) == 0 }
    }

    pub fn cerrar(s: Descriptor) {
        unsafe {
            close(s);
        }
    }

    pub fn adoptar(s: Descriptor) -> std::net::TcpStream {
        unsafe { std::net::TcpStream::from_raw_fd(s) }
    }

    pub fn fuera_de_banda(flujo: &std::net::TcpStream, b: u8) -> bool {
        let s = flujo.as_raw_fd();
        unsafe { send(s, &b, 1, MSG_OOB) == 1 }
    }
}

/// La direccion en la forma que espera el sistema: `sockaddr_in` o `sockaddr_in6`.
///
/// Los dos empiezan igual --familia y puerto-- y de ahi se separan. La familia va en el orden de la
/// maquina y el puerto en el de la red; **no es un descuido**, es como estan definidos, y
/// confundirlos da un puerto al reves que se nota recien al conectar.
fn como_sockaddr(dir: &std::net::SocketAddr) -> (Vec<u8>, u16) {
    match dir {
        std::net::SocketAddr::V4(a) => {
            let mut b = vec![0u8; 16];
            b[0..2].copy_from_slice(&crudo::AF_INET.to_ne_bytes());
            b[2..4].copy_from_slice(&a.port().to_be_bytes());
            b[4..8].copy_from_slice(&a.ip().octets());
            (b, crudo::AF_INET)
        }
        std::net::SocketAddr::V6(a) => {
            let mut b = vec![0u8; 28];
            b[0..2].copy_from_slice(&crudo::AF_INET6.to_ne_bytes());
            b[2..4].copy_from_slice(&a.port().to_be_bytes());
            b[4..8].copy_from_slice(&a.flowinfo().to_be_bytes());
            b[8..24].copy_from_slice(&a.ip().octets());
            b[24..28].copy_from_slice(&a.scope_id().to_ne_bytes());
            (b, crudo::AF_INET6)
        }
    }
}

/// La primera direccion a la que resuelve `host:puerto`, o `None`.
fn resolver(host: &str, puerto: u16) -> Option<std::net::SocketAddr> {
    use std::net::ToSocketAddrs;
    (host, puerto).to_socket_addrs().ok().and_then(|mut a| a.next())
}

/// Conecta a `remoto` saliendo por `local`, opcionalmente con ese TTL.
///
/// **Bloquea**, y por eso nadie la llama desde el hilo del interprete: los dos que la usan
/// --`connectFromStart` y `reachableStart`-- la corren en un hilo del sistema aparte y contestan
/// por el casillero. Ver la nota de los codigos de error.
///
/// `local` con puerto cero y direccion comodin es lo mismo que no atar, salvo que el sistema ya
/// sabe por que placa va a salir, que es justamente lo que se le esta pidiendo.
fn conectar_desde(
    remoto: std::net::SocketAddr,
    local: std::net::SocketAddr,
    ttl: u32,
) -> Option<std::net::TcpStream> {
    let (dir_remota, familia) = como_sockaddr(&remoto);
    let (dir_local, familia_local) = como_sockaddr(&local);
    if familia != familia_local {
        // Atar una punta IPv4 a una conexion IPv6 no es un caso raro que valga la pena rodear: es
        // una peticion contradictoria, y el que llama tiene que enterarse.
        return None;
    }
    let s = crudo::nuevo(familia)?;
    if !crudo::atar(s, &dir_local) {
        crudo::cerrar(s);
        return None;
    }
    if ttl > 0 && !crudo::poner_ttl(s, ttl) {
        crudo::cerrar(s);
        return None;
    }
    if !crudo::conectar(s, &dir_remota) {
        crudo::cerrar(s);
        return None;
    }
    Some(crudo::adoptar(s))
}

/// Un socket TCP abierto, del lado de la VM.
///
/// Un `Socket` de Java es un `handle` --un indice en [`SOCKETS`]-- y nada mas. El estado vive aca
/// porque un socket **es** estado entre llamadas: su par, sus tiempos de espera, si ya se cerro una
/// de sus mitades. No hay forma de representarlo con operaciones de una sola vez.
enum SockState {
    /// Una conexion establecida.
    Stream(std::net::TcpStream),
    /// Un socket a la escucha.
    Listener(std::net::TcpListener),
    /// Un socket de datagramas.
    ///
    /// Guarda de quien vino el ultimo paquete recibido porque **un datagrama y su remitente son un
    /// solo dato**: `DatagramPacket` los quiere juntos, y un nativo que devuelve un entero no puede
    /// devolver los dos. Se lee con `udpSenderAddress`/`udpSenderPort` justo despues de recibir, y
    /// para que ese par sea atomico el lado Java recibe adentro de un `synchronized`.
    Datagram {
        sock: std::net::UdpSocket,
        ultimo: Option<(String, u16)>,
    },
}

/// Los sockets abiertos, indexados por handle.
///
/// Es un `Mutex` global y **no** un `thread_local` como [`PROCS`], y la diferencia importa: un
/// socket se usa desde otro hilo del que lo creo todo el tiempo --un servidor acepta en uno y
/// atiende en otro-- asi que una tabla por hilo lo perderia. Con `JVM_THREADS=os`, donde los hilos
/// de Java son hilos del sistema, eso dejaria de ser teorico.
///
/// **Las entradas no se reciclan**, por lo mismo que en `PROCS`: un handle viejo apunta a `None` o
/// al socket que siempre fue, nunca a uno nuevo. Reciclar indices ahorraria memoria irrelevante a
/// cambio del peor error posible de encontrar.
static SOCKETS: std::sync::Mutex<Vec<Option<SockState>>> = std::sync::Mutex::new(Vec::new());

/// Las respuestas que todavia no llegaron, indexadas por id.
///
/// Es la tabla de las dos operaciones que **tienen** que usar el `connect` bloqueante del sistema
/// --la sonda de alcance y el connect con punta local elegida-- porque ninguna de las dos se puede
/// hacer sin bloquear. Cada casillero lo llena un hilo del sistema cuando su `connect` termina, y el
/// lado Java lo mira con `answerPoll` hasta que aparezca.
///
/// Un `i32` y no algo mas rico porque las dos respuestas son un entero y quien pregunta ya sabe cual
/// esta esperando: la sonda contesta 1 o 0, y el connect contesta el handle o -1.
///
/// **Las entradas no se reciclan**, por lo mismo que en [`SOCKETS`].
#[allow(clippy::type_complexity)]
static SONDAS: std::sync::Mutex<Vec<Option<std::sync::Arc<std::sync::Mutex<Option<i32>>>>>> =
    std::sync::Mutex::new(Vec::new());

/// Guarda un casillero vacio y devuelve su id.
fn nuevo_casillero() -> (i32, std::sync::Arc<std::sync::Mutex<Option<i32>>>) {
    let casillero = std::sync::Arc::new(std::sync::Mutex::new(None));
    let mut t = SONDAS.lock().unwrap();
    t.push(Some(casillero.clone()));
    ((t.len() - 1) as i32, casillero)
}

/// Guarda ese socket y devuelve su handle.
fn guardar_socket(s: SockState) -> i32 {
    let mut t = SOCKETS.lock().unwrap();
    t.push(Some(s));
    (t.len() - 1) as i32
}

/// Corre `f` sobre el flujo de ese handle, o devuelve `si_no` si el handle no nombra uno.
///
/// **Solo para operaciones instantaneas.** Tiene la tabla tomada mientras corre `f`, asi que una
/// operacion que espere ahi adentro le cierra la puerta a todos los demas sockets de la VM. Lo que
/// puede esperar --leer, escribir, aceptar-- saca antes su propio duplicado con [`tomar_flujo`] o
/// [`tomar_escucha`] y suelta la tabla.
fn con_flujo<T>(h: i32, si_no: T, f: impl FnOnce(&mut std::net::TcpStream) -> T) -> T {
    let mut t = SOCKETS.lock().unwrap();
    match t.get_mut(h as usize).and_then(|e| e.as_mut()) {
        Some(SockState::Stream(s)) => f(s),
        _ => si_no,
    }
}

/// Un duplicado del flujo de ese handle, **con la tabla ya soltada**.
///
/// `try_clone` no copia el socket: da otro descriptor sobre el mismo. Sirve para sacar de la tabla
/// lo que hace falta y devolver el candado antes de tocar la red, que es lo que evita que un hilo
/// leyendo deje sin sockets al resto de la VM.
fn tomar_flujo(h: i32) -> Option<std::net::TcpStream> {
    let t = SOCKETS.lock().unwrap();
    let d = match t.get(h as usize).and_then(|e| e.as_ref()) {
        Some(SockState::Stream(s)) => s.try_clone().ok(),
        _ => None,
    };
    if let Some(d) = &d {
        // **El duplicado no hereda el modo.** En Windows "no bloqueante" es una propiedad del
        // descriptor y no del socket, asi que el duplicado nace bloqueante aunque el original no lo
        // sea, y una lectura sobre el cuelga la VM entera. Costo un rato encontrarlo: el original
        // estaba bien puesto y el sintoma aparecia igual.
        let _ = d.set_nonblocking(true);
    }
    d
}

/// Entra o sale de un grupo multicast. Es una sola funcion porque las dos operaciones tienen que
/// resolver exactamente lo mismo --el grupo, la placa, y si son v4 o v6-- y separarlas duplicaria
/// esa resolucion, que es donde estan todos los casos raros.
///
/// La `interfaz` vacia significa "la que elija el sistema": `0.0.0.0` en v4 y el indice 0 en v6.
fn membresia(h: i32, grupo: &str, interfaz: &str, entrar: bool) -> bool {
    use std::net::{IpAddr, Ipv4Addr};
    let Some(sock) = tomar_datagrama(h) else {
        return false;
    };
    let Ok(dir) = grupo.parse::<IpAddr>() else {
        return false;
    };
    match dir {
        IpAddr::V4(g) => {
            let placa = interfaz.parse::<Ipv4Addr>().unwrap_or(Ipv4Addr::UNSPECIFIED);
            if entrar {
                sock.join_multicast_v4(&g, &placa).is_ok()
            } else {
                sock.leave_multicast_v4(&g, &placa).is_ok()
            }
        }
        IpAddr::V6(g) => {
            // En v6 la placa se nombra por indice, no por direccion. Sin uno, cero: "la que elija
            // el sistema", que es lo mismo que el `0.0.0.0` de v4.
            let indice = interfaz.parse::<u32>().unwrap_or(0);
            if entrar {
                sock.join_multicast_v6(&g, indice).is_ok()
            } else {
                sock.leave_multicast_v6(&g, indice).is_ok()
            }
        }
    }
}

/// Un duplicado del socket de datagramas de ese handle, con la tabla ya soltada. Ver
/// [`tomar_flujo`].
fn tomar_datagrama(h: i32) -> Option<std::net::UdpSocket> {
    let t = SOCKETS.lock().unwrap();
    let d = match t.get(h as usize).and_then(|e| e.as_ref()) {
        Some(SockState::Datagram { sock, .. }) => sock.try_clone().ok(),
        _ => None,
    };
    if let Some(d) = &d {
        // Ver [`tomar_flujo`]: el duplicado nace bloqueante en Windows.
        let _ = d.set_nonblocking(true);
    }
    d
}

/// Un duplicado del escucha de ese handle, con la tabla ya soltada. Ver [`tomar_flujo`].
fn tomar_escucha(h: i32) -> Option<std::net::TcpListener> {
    let t = SOCKETS.lock().unwrap();
    let d = match t.get(h as usize).and_then(|e| e.as_ref()) {
        Some(SockState::Listener(l)) => l.try_clone().ok(),
        _ => None,
    };
    if let Some(d) = &d {
        // Ver [`tomar_flujo`]: el duplicado nace bloqueante en Windows.
        let _ = d.set_nonblocking(true);
    }
    d
}

thread_local! {
    /// Los procesos hijo de este hilo, indexados por handle. **Las entradas no se reciclan**: un
    /// handle viejo apunta a `None` o al proceso que siempre fue, nunca a uno nuevo. Reciclar
    /// indices ahorraria memoria irrelevante a cambio del peor error posible de encontrar --dos
    /// `Process` de Java refiriendose al mismo hijo--.
    static PROCS: RefCell<Vec<Option<ProcState>>> = const { RefCell::new(Vec::new()) };
}

thread_local! {
    /// El Filer **armado** en este hilo, o `None` si no hay ninguno corriendo. Los nativos del
    /// Filer solo registran cuando está armado, así que una llamada suelta a `createSourceFile`
    /// fuera de una ronda de procesamiento no rompe nada (se descarta).
    static FILER: RefCell<Option<FilerState>> = const { RefCell::new(None) };
}

/// **Arma** el canal del Filer en este hilo — llamar antes de correr un procesador. Reemplaza
/// cualquier estado previo por uno vacío.
pub fn install_filer() {
    FILER.with(|f| *f.borrow_mut() = Some(FilerState::default()));
}

/// Los `StringWriter` que el Filer registró, como offsets del heap — **raíces del GC que sostiene
/// la VM**. El canal sobrevive a los frames que los crearon (el compilador lo drena *después* de
/// que el procesador retorna), así que ninguna pila los mantiene vivos: sin esto, una colección
/// que mueve objetos dejaría cada offset apuntando a donde el writer *solía* estar, y el texto
/// generado se recuperaría vacío.
pub fn filer_roots() -> Vec<usize> {
    FILER.with(|f| {
        f.borrow()
            .as_ref()
            .map(|state| state.pending.iter().map(|&(_, r)| r as usize).collect())
            .unwrap_or_default()
    })
}

/// Reescribe los offsets que el GC remapeó, en el mismo orden en que [`filer_roots`] los entregó.
pub fn remap_filer_roots(refs: &[usize]) {
    FILER.with(|f| {
        if let Some(state) = f.borrow_mut().as_mut() {
            for (entry, &offset) in state.pending.iter_mut().zip(refs) {
                entry.1 = offset as u32;
            }
        }
    });
}

/// **Desarma** el canal y devuelve todo lo que el procesador registró, en orden de creación. Si no
/// había Filer armado, devuelve un vector vacío.
pub fn drain_filer() -> Vec<(String, u32)> {
    FILER.with(|f| f.borrow_mut().take().map(|state| state.pending).unwrap_or_default())
}

/// Runs the native method `class.name descriptor` with `args` (slot 0 is the
/// receiver for an instance method), returning its result ([`NativeOutcome::Ran`] with `None` for
/// `void`), or [`NativeOutcome::Unimplemented`] when there is no bridge for that method.
/// `heap` lets a native read object memory (e.g. an object's header); anything the
/// method "prints" is appended to `out` — the program's stdout, which the caller
/// surfaces (the visualizer shows it; a headless run would flush it). `apt` is the
/// reified compiler model (APT fase 3), or `None` outside a processor run — the
/// `jdk/internal/apt/SymElement` natives read the symbol table through it.
pub fn dispatch(
    class: &str,
    name: &str,
    descriptor: &str,
    args: &[Value],
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    out: &mut String,
    apt: &mut Option<AptContext>,
) -> NativeOutcome {
    NativeOutcome::Ran(match (class, name, descriptor) {
        // --- I/O: PrintStream.println --------------------------------------------
        // The receiver is args[0]; the value follows. The native `write` the real
        // java.io chain bottoms out at.
        ("java/io/PrintStream", "println", "(I)V") => {
            if let Value::Int(n) = args[1] {
                let _ = writeln!(out, "{n}");
            }
            None
        }
        // println(String): the arg is a heap String reference; read its bytes back.
        ("java/io/PrintStream", "println", "(Ljava/lang/String;)V") => {
            let _ = writeln!(out, "{}", strings::read(heap, reference(&args[1])));
            None
        }
        // print(String) — the same channel as println but WITHOUT the trailing newline. It is the
        // single text seam the whole of PrintStream (print/println()/printf/format/append and the
        // byte writes) funnels through.
        ("java/io/PrintStream", "writeString", "(Ljava/lang/String;)V") => {
            let _ = write!(out, "{}", strings::read(heap, reference(&args[1])));
            None
        }
        // APT (fase 2): salida de un processor durante el round loop, vía este native estático — el
        // mínimo `Messager` delegando a Rust. `args[0]` es la referencia al String (método estático:
        // sin receptor). (Nota histórica: se agregó cuando `System.out.println` no compilaba en este
        // javac por un bug de carga transitiva —el tipo de `System.out` quedaba sin resolver—; ese
        // bug ya está arreglado y `System.out.println` compila, pero el native se conserva como el
        // canal directo `Messager`→stdout de un processor.)
        ("javax/annotation/processing/AptTrace", "trace", "(Ljava/lang/String;)V") => {
            let _ = writeln!(out, "{}", strings::read(heap, reference(&args[0])));
            None
        }

        // --- Sistema de archivos -------------------------------------------------
        //
        // Los seis nativos que le dan a Java acceso al disco. Son pocos a proposito: todo lo demas
        // --`File.getParent`, `Scanner`, `Formatter`-- se construye arriba de estos en Java, donde se
        // puede leer y probar. Lo que **no** se puede escribir en Java es esto, y solo esto.
        //
        // **El archivo se lee o escribe entero de una.** No hay descriptor abierto, ni posicion, ni
        // `close` que pueda faltar. Es una limitacion real --un archivo de un giga entra en memoria
        // dos veces-- y a cambio no hay ningun estado que se pueda quedar colgado, que es la clase de
        // error mas dificil de encontrar en una VM. Cuando haga falta streaming de verdad, la puerta
        // es agregar un handle aca abajo, no cambiar lo de arriba.
        //
        // Todos toman la ruta como `String` y no como `File`: el nativo no sabe nada de la clase, y
        // asi `java.io.File` puede cambiar sin tocar Rust.

        // Los bytes del archivo, o `null` si no se puede leer. Devolver null y no tirar es lo que
        // deja que el lado Java decida cual de las cuatro excepciones corresponde -- el nativo no
        // tiene con que distinguir "no existe" de "no se puede leer".
        ("jdk/internal/io/Fs", "readAllBytes", "(Ljava/lang/String;)[B") => {
            let ruta = strings::read(heap, reference(&args[0]));
            match std::fs::read(&ruta) {
                Ok(bytes) => {
                    let offset = match array_operations::allocate_array_of_class(
                        metaspace, heap, "[B", bytes.len(),
                    ) {
                        Ok(o) => o,
                        Err(_) => return NativeOutcome::Ran(Some(Value::Reference(0))),
                    };
                    for (i, &b) in bytes.iter().enumerate() {
                        heap.write_u8(offset + array_operations::ARRAY_HEADER_SIZE + i, b);
                    }
                    Some(Value::Reference(offset))
                }
                Err(_) => Some(Value::Reference(0)),
            }
        }
        // Escribe los bytes. `true` si se pudo. `append` decide si agrega o pisa.
        ("jdk/internal/io/Fs", "writeAllBytes", "(Ljava/lang/String;[BZ)Z") => {
            let ruta = strings::read(heap, reference(&args[0]));
            let arr = reference(&args[1]);
            let anexar = matches!(args[2], Value::Int(1));
            if arr == 0 {
                return NativeOutcome::Ran(Some(Value::Int(0)));
            }
            let n = heap.read_u32(arr + HEADER_SIZE) as usize;
            let mut bytes = Vec::with_capacity(n);
            for i in 0..n {
                bytes.push(heap.read_u8(arr + array_operations::ARRAY_HEADER_SIZE + i));
            }
            let r = if anexar {
                std::fs::OpenOptions::new().create(true).append(true).open(&ruta).and_then(|mut f| {
                    use std::io::Write;
                    f.write_all(&bytes)
                })
            } else {
                std::fs::write(&ruta, &bytes)
            };
            Some(Value::Int(if r.is_ok() { 1 } else { 0 }))
        }
        // Los metadatos, empaquetados en un `int` de banderas: 1 = existe, 2 = es archivo,
        // 4 = es directorio, 8 = se puede leer, 16 = se puede escribir.
        //
        // Van juntos y no en cinco nativos porque los cinco salen de **una sola** llamada al sistema:
        // preguntarlos por separado consultaria el disco cinco veces, y --peor-- podria dar respuestas
        // de momentos distintos si algo cambia en el medio.
        ("jdk/internal/io/Fs", "stat", "(Ljava/lang/String;)I") => {
            let ruta = strings::read(heap, reference(&args[0]));
            let mut banderas = 0i32;
            if let Ok(md) = std::fs::metadata(&ruta) {
                banderas |= 1;
                if md.is_file() {
                    banderas |= 2;
                }
                if md.is_dir() {
                    banderas |= 4;
                }
                banderas |= 8;
                if !md.permissions().readonly() {
                    banderas |= 16;
                }
            }
            Some(Value::Int(banderas))
        }
        // El tamaño en bytes, o 0 si no se puede saber -- que es lo que devuelve `File.length()`
        // para lo que no existe.
        ("jdk/internal/io/Fs", "size", "(Ljava/lang/String;)J") => {
            let ruta = strings::read(heap, reference(&args[0]));
            let n = std::fs::metadata(&ruta).map(|m| m.len()).unwrap_or(0);
            Some(Value::Long(n as i64))
        }
        // Las raices del sistema de archivos: `C:\\`, `D:\\`, ... en Windows; `/` en el resto.
        //
        // Es lo que le faltaba a `FileSystem.getRootDirectories()`, que devolvia una lista vacia. El
        // vacio era la respuesta mas debil que tenia: se lee como "no hay raices", que no es lo
        // mismo que "no las puedo enumerar".
        ("jdk/internal/io/Fs", "roots", "()[Ljava/lang/String;") => {
            let raices = raices_del_sistema();
            let arr = match array_operations::allocate_array_of_class(
                metaspace, heap, "[Ljava/lang/String;", raices.len(),
            ) {
                Ok(o) => o,
                Err(_) => return NativeOutcome::Ran(Some(Value::Reference(0))),
            };
            for (i, r) in raices.iter().enumerate() {
                // De a uno y escribiendo enseguida, como en `list`: internar puede disparar una
                // recoleccion, y guardar los offsets antes de tiempo dejaria referencias viejas.
                let sref = strings::intern(metaspace, heap, r) as u32;
                heap.write_u32(arr + array_operations::ARRAY_HEADER_SIZE + i * 4, sref);
            }
            Some(Value::Reference(arr))
        }
        // --- Espacio de volumen: `Fs.diskTotal/diskUsable/diskUnallocated` -------------------
        //
        // Los tres que `java.nio.file.FileStore` promete, y la costura que le faltaba a
        // `Files.getFileStore`. Devuelven **-1** cuando no se pudo averiguar, y eso es parte del
        // contrato de estos tres nativos: el lado Java lo traduce a la `IOException` que el
        // `FileStore` declara. Devolver cero seria peor -- un volumen con cero bytes libres es una
        // respuesta valida y muy distinta de "no se sabe".
        //
        // El camino se toma tal cual viene: la API de Windows acepta un archivo o un directorio
        // cualquiera y contesta por el volumen que lo contiene, que es exactamente lo que
        // `getFileStore(path)` quiere.
        ("jdk/internal/io/Fs", "diskTotal", "(Ljava/lang/String;)J") => {
            let ruta = strings::read(heap, reference(&args[0]));
            Some(Value::Long(espacio_de_volumen(&ruta).map(|(t, _, _)| t).unwrap_or(-1)))
        }
        ("jdk/internal/io/Fs", "diskUsable", "(Ljava/lang/String;)J") => {
            let ruta = strings::read(heap, reference(&args[0]));
            Some(Value::Long(espacio_de_volumen(&ruta).map(|(_, u, _)| u).unwrap_or(-1)))
        }
        ("jdk/internal/io/Fs", "diskUnallocated", "(Ljava/lang/String;)J") => {
            let ruta = strings::read(heap, reference(&args[0]));
            Some(Value::Long(espacio_de_volumen(&ruta).map(|(_, _, l)| l).unwrap_or(-1)))
        }
        // Borra un archivo o un directorio **vacio**. `true` si se pudo.
        //
        // Vacio a proposito: `File.delete()` no borra recursivamente, y un nativo que si lo hiciera
        // convertiria un `delete()` sobre el directorio equivocado en una perdida de datos.
        ("jdk/internal/io/Fs", "delete", "(Ljava/lang/String;)Z") => {
            let ruta = strings::read(heap, reference(&args[0]));
            let ok = if std::fs::metadata(&ruta).map(|m| m.is_dir()).unwrap_or(false) {
                std::fs::remove_dir(&ruta).is_ok()
            } else {
                std::fs::remove_file(&ruta).is_ok()
            };
            Some(Value::Int(if ok { 1 } else { 0 }))
        }
        // Los nombres **simples** de las entradas de un directorio, o `null` si no se pudo leer
        // (no existe, no es un directorio, sin permisos).
        //
        // Es el nativo que faltaba para que se pueda **recorrer** el disco y no solo tocar archivos
        // sueltos. Con el entran los nueve metodos de `java.nio.file` que enumeran --`list`, `walk`,
        // `find`, `walkFileTree`, los tres `newDirectoryStream`-- y los cinco `list`/`listFiles` de
        // `java.io.File`, que hasta ahora devolvian `null` siempre.
        //
        // Nombres simples y no rutas completas, como hace `File.list()`: quien quiera la ruta la
        // arma con el directorio que ya tiene, y devolverla armada obligaria al nativo a elegir un
        // separador y a decidir si normaliza -- dos decisiones que son del lado Java.
        //
        // El orden es el que da el sistema de archivos y **no se ordena**: el contrato dice
        // explicitamente que no hay garantia de orden, y ordenar aca haria que alguien se apoyara en
        // uno que otra plataforma no le va a dar.
        ("jdk/internal/io/Fs", "list", "(Ljava/lang/String;)[Ljava/lang/String;") => {
            let ruta = strings::read(heap, reference(&args[0]));
            let Ok(entradas) = std::fs::read_dir(&ruta) else {
                return NativeOutcome::Ran(Some(Value::Reference(0)));
            };
            let mut nombres: Vec<String> = Vec::new();
            for e in entradas.flatten() {
                nombres.push(e.file_name().to_string_lossy().into_owned());
            }
            let arr = match array_operations::allocate_array_of_class(
                metaspace, heap, "[Ljava/lang/String;", nombres.len(),
            ) {
                Ok(o) => o,
                Err(_) => return NativeOutcome::Ran(Some(Value::Reference(0))),
            };
            for (i, n) in nombres.iter().enumerate() {
                // Se internan de a uno y se escriben enseguida: `intern` puede disparar una
                // recoleccion, y guardar los offsets antes de tiempo dejaria referencias viejas.
                let sref = strings::intern(metaspace, heap, n) as u32;
                heap.write_u32(arr + array_operations::ARRAY_HEADER_SIZE + i * 4, sref);
            }
            Some(Value::Reference(arr))
        }
        // El camino **canonico**: resuelto, absoluto y sin enlaces. Es lo que contesta si dos rutas
        // distintas nombran el mismo archivo, que es la pregunta de `Files.isSameFile` y la unica
        // que no se puede contestar comparando cadenas -- en Windows `C:\A.TXT` y `C:.txt` son
        // el mismo archivo y no son la misma cadena.
        //
        // `null` si la ruta no existe: canonicalizar lo que no esta no tiene respuesta.
        ("jdk/internal/io/Fs", "canonical", "(Ljava/lang/String;)Ljava/lang/String;") => {
            let ruta = strings::read(heap, reference(&args[0]));
            match std::fs::canonicalize(&ruta) {
                Ok(p) => {
                    let texto = p.to_string_lossy().into_owned();
                    Some(Value::Reference(strings::intern(metaspace, heap, &texto)))
                }
                Err(_) => Some(Value::Reference(0)),
            }
        }
        // La fecha de ultima modificacion, en milisegundos desde la epoca; `Long.MIN_VALUE` si no
        // se pudo leer. Un centinela y no un cero: cero **es** una fecha valida (la epoca), y era
        // justamente la que se devolvia antes por no tener con que leer la de verdad.
        ("jdk/internal/io/Fs", "mtime", "(Ljava/lang/String;)J") => {
            let ruta = strings::read(heap, reference(&args[0]));
            let millis = std::fs::metadata(&ruta)
                .and_then(|m| m.modified())
                .ok()
                .map(|t| match t.duration_since(std::time::UNIX_EPOCH) {
                    Ok(d) => d.as_millis() as i64,
                    // Anterior a 1970: la resta va al reves y el resultado es negativo.
                    Err(e) => -(e.duration().as_millis() as i64),
                });
            Some(Value::Long(millis.unwrap_or(i64::MIN)))
        }
        // Fija la fecha de ultima modificacion, en milisegundos desde la epoca.
        ("jdk/internal/io/Fs", "setMtime", "(Ljava/lang/String;J)Z") => {
            let ruta = strings::read(heap, reference(&args[0]));
            let Value::Long(millis) = args[1] else {
                return NativeOutcome::Ran(Some(Value::Int(0)));
            };
            // `SystemTime` no admite negativos por resta desde `UNIX_EPOCH`, asi que una fecha
            // anterior a 1970 se arma restando en vez de sumando.
            let base = std::time::UNIX_EPOCH;
            let t = if millis >= 0 {
                base.checked_add(std::time::Duration::from_millis(millis as u64))
            } else {
                base.checked_sub(std::time::Duration::from_millis((-millis) as u64))
            };
            let ok = match (t, std::fs::File::options().write(true).open(&ruta)) {
                (Some(t), Ok(f)) => f.set_modified(t).is_ok(),
                _ => false,
            };
            Some(Value::Int(i32::from(ok)))
        }
        // Crea un directorio. `todos` decide si tambien los padres que falten.
        ("jdk/internal/io/Fs", "mkdir", "(Ljava/lang/String;Z)Z") => {
            let ruta = strings::read(heap, reference(&args[0]));
            let todos = matches!(args[1], Value::Int(1));
            let r = if todos {
                std::fs::create_dir_all(&ruta)
            } else {
                std::fs::create_dir(&ruta)
            };
            Some(Value::Int(if r.is_ok() { 1 } else { 0 }))
        }


        // --- TCP: `jdk/internal/net/Net` ------------------------------------------------------
        //
        // La costura que le faltaba a `java.net.Socket`, a `ServerSocket` y a todo lo que se apoya
        // en ellos. El diseno es el mismo que el de `Proc`: el nativo hace lo minimo y **no sabe
        // nada de las clases de Java**. Toma y devuelve cadenas, arreglos y enteros; quien sea
        // `Socket` es problema del lado Java, que puede cambiar sin tocar Rust.
        //
        // Los codigos de error se eligieron para que el lado Java pueda distinguir los casos que el
        // contrato distingue, y por eso no son todos -1:
        //
        //   - `connect`/`listen`/`accept` devuelven **-1** si no se pudo. El nativo no distingue
        //     "rechazado" de "no hay ruta", asi que el lado Java arma la `IOException` con lo unico
        //     que sabe con certeza: a donde intento conectarse.
        //   - `read` devuelve **-1** en fin de flujo y **-2** si vencio el tiempo de espera. Son dos
        //     cosas distintas --una conexion cerrada y una que sigue viva pero callada-- y un solo
        //     centinela haria que `SocketTimeoutException` fuera indistinguible del EOF.
        //   - `read` y `accept` devuelven **-3** para "todavia no hay nada", y ese codigo es la
        //     pieza que hace que TCP funcione en esta VM.
        //
        // **Por que ningun nativo de aca bloquea.** Los hilos de Java de esta VM comparten un
        // interprete: el verde los multiplexa sobre un solo hilo del sistema, y los dos modos con
        // hilos del sistema todavia serializan la ejecucion con un candado global. Un nativo que se
        // quede esperando adentro --un `accept` que espera a que alguien conecte-- no deja correr a
        // **ningun** otro hilo de Java, y el que iba a conectar es justamente uno de ellos: la VM se
        // cuelga entera. Es un abrazo mortal, no una lentitud.
        //
        // Por eso todos los sockets se ponen en modo **no bloqueante** apenas se crean, y lo que
        // antes esperaba ahora contesta -3 en el acto. La espera se hace del lado Java, con un
        // `Thread.sleep` corto entre intentos: dormir **si** es una operacion que esta VM sabe
        // manejar --suelta el interprete y deja correr a los demas-- asi que el hilo que espera no
        // le impide a nadie avanzar. De regalo, esa espera del lado Java es la que le permite a
        // `ServerSocket.accept` respetar `setSoTimeout`, cosa que un `accept` bloqueante del sistema
        // no daba.

        // Conecta. `timeoutMs` en cero significa sin limite.
        ("jdk/internal/net/Net", "connect", "(Ljava/lang/String;II)I") => {
            let host = strings::read(heap, reference(&args[0]));
            let puerto = entero(&args[1]) as u16;
            let espera = entero(&args[2]);
            let r = if espera > 0 {
                // Con plazo hay que resolver el nombre primero: `connect_timeout` toma una direccion
                // ya resuelta, no un nombre. Se prueba la primera que resuelva, que es lo que hace
                // `Socket` cuando el nombre tiene varias.
                use std::net::ToSocketAddrs;
                match (host.as_str(), puerto).to_socket_addrs().ok().and_then(|mut a| a.next()) {
                    Some(dir) => std::net::TcpStream::connect_timeout(
                        &dir,
                        std::time::Duration::from_millis(espera as u64),
                    ),
                    None => Err(std::io::Error::new(std::io::ErrorKind::Other, "sin direccion")),
                }
            } else {
                std::net::TcpStream::connect((host.as_str(), puerto))
            };
            match r {
                Ok(s) => {
                    // Desde aca en adelante el socket no espera: ver la nota de arriba.
                    let _ = s.set_nonblocking(true);
                    Some(Value::Int(guardar_socket(SockState::Stream(s))))
                }
                Err(_) => Some(Value::Int(-1)),
            }
        }
        // Ata y escucha. Un puerto cero deja que el sistema elija uno, que es lo que
        // `ServerSocket(0)` promete; el lado Java lo lee despues con `localPort`.
        ("jdk/internal/net/Net", "listen", "(Ljava/lang/String;II)I") => {
            let host = strings::read(heap, reference(&args[0]));
            let puerto = entero(&args[1]) as u16;
            match std::net::TcpListener::bind((host.as_str(), puerto)) {
                Ok(l) => {
                    let _ = l.set_nonblocking(true);
                    Some(Value::Int(guardar_socket(SockState::Listener(l))))
                }
                Err(_) => Some(Value::Int(-1)),
            }
        }
        // Acepta una conexion **sin esperar**: -3 si todavia no hay nadie, -1 si el handle no es un
        // escucha o si el sistema fallo. Quien quiera esperar, espera del lado Java.
        ("jdk/internal/net/Net", "accept", "(I)I") => {
            let h = entero(&args[0]);
            let Some(escucha) = tomar_escucha(h) else {
                return NativeOutcome::Ran(Some(Value::Int(-1)));
            };
            match escucha.accept() {
                Ok((s, _)) => {
                    // El socket aceptado hereda el modo del escucha en algunos sistemas y en otros
                    // no; ponerlo siempre es la unica forma de que no dependa del sistema.
                    let _ = s.set_nonblocking(true);
                    Some(Value::Int(guardar_socket(SockState::Stream(s))))
                }
                Err(e) if e.kind() == std::io::ErrorKind::WouldBlock => Some(Value::Int(-3)),
                Err(_) => Some(Value::Int(-1)),
            }
        }
        // Lee. Devuelve cuantos bytes puso, -1 en fin de flujo, -2 si vencio el plazo.
        ("jdk/internal/net/Net", "read", "(I[BII)I") => {
            use std::io::Read;
            let h = entero(&args[0]);
            let arr = reference(&args[1]);
            let off = entero(&args[2]) as usize;
            let len = entero(&args[3]) as usize;
            if arr == 0 {
                return NativeOutcome::Ran(Some(Value::Int(-1)));
            }
            let mut buf = vec![0u8; len];
            let Some(mut flujo) = tomar_flujo(h) else {
                return NativeOutcome::Ran(Some(Value::Int(-1)));
            };
            let n = match flujo.read(&mut buf) {
                Ok(0) => -1,
                Ok(n) => n as i32,
                // Nada que leer todavia. No es fin de flujo --la conexion sigue viva-- y no es un
                // plazo vencido, porque el plazo lo cuenta el lado Java.
                Err(e)
                    if e.kind() == std::io::ErrorKind::WouldBlock
                        || e.kind() == std::io::ErrorKind::TimedOut =>
                {
                    -3
                }
                Err(_) => -1,
            };
            if n > 0 {
                for i in 0..n as usize {
                    heap.write_u8(arr + array_operations::ARRAY_HEADER_SIZE + off + i, buf[i]);
                }
            }
            Some(Value::Int(n))
        }
        // Escribe. `true` si se pudo escribir **todo**: una escritura parcial sobre un socket es una
        // escritura fallida desde el punto de vista de quien llama.
        ("jdk/internal/net/Net", "write", "(I[BII)Z") => {
            use std::io::Write;
            let h = entero(&args[0]);
            let arr = reference(&args[1]);
            let off = entero(&args[2]) as usize;
            let len = entero(&args[3]) as usize;
            if arr == 0 {
                return NativeOutcome::Ran(Some(Value::Int(0)));
            }
            let mut bytes = Vec::with_capacity(len);
            for i in 0..len {
                bytes.push(heap.read_u8(arr + array_operations::ARRAY_HEADER_SIZE + off + i));
            }
            let Some(mut flujo) = tomar_flujo(h) else {
                return NativeOutcome::Ran(Some(Value::Int(0)));
            };
            // Un socket no bloqueante puede aceptar solo una parte de lo que se le da cuando el
            // buffer de salida del sistema esta lleno, asi que hay que insistir con lo que quedo.
            // La espera es de este lado y no del de Java porque una escritura parcial no es un
            // estado que se pueda devolver: `write` promete todo o nada, y a mitad de camino no hay
            // nada honesto que contestar. El buffer se llena solo si el par no lee, y entonces el
            // milisegundo de espera entre intentos es lo unico que se puede hacer igual.
            let mut escrito = 0usize;
            let ok = loop {
                if escrito == bytes.len() {
                    break flujo.flush().is_ok();
                }
                match flujo.write(&bytes[escrito..]) {
                    Ok(0) => break false,
                    Ok(n) => escrito += n,
                    Err(e) if e.kind() == std::io::ErrorKind::WouldBlock => {
                        std::thread::sleep(std::time::Duration::from_millis(1));
                    }
                    Err(_) => break false,
                }
            };
            Some(Value::Int(i32::from(ok)))
        }
        // Cierra. La entrada se saca de la tabla, y ahi Rust cierra el descriptor al soltarla.
        ("jdk/internal/net/Net", "close", "(I)V") => {
            let h = entero(&args[0]);
            let mut t = SOCKETS.lock().unwrap();
            if let Some(e) = t.get_mut(h as usize) {
                *e = None;
            }
            None
        }
        ("jdk/internal/net/Net", "shutdownIn", "(I)Z") => {
            let h = entero(&args[0]);
            let ok = con_flujo(h, false, |s| s.shutdown(std::net::Shutdown::Read).is_ok());
            Some(Value::Int(i32::from(ok)))
        }
        ("jdk/internal/net/Net", "shutdownOut", "(I)Z") => {
            let h = entero(&args[0]);
            let ok = con_flujo(h, false, |s| s.shutdown(std::net::Shutdown::Write).is_ok());
            Some(Value::Int(i32::from(ok)))
        }
        // El puerto local. Sirve para las dos formas --un flujo y un escucha-- porque los dos lo
        // tienen, y `ServerSocket(0)` lo necesita para saber que puerto le dio el sistema.
        ("jdk/internal/net/Net", "localPort", "(I)I") => {
            let h = entero(&args[0]);
            let t = SOCKETS.lock().unwrap();
            let p = match t.get(h as usize).and_then(|e| e.as_ref()) {
                Some(SockState::Stream(s)) => s.local_addr().map(|a| a.port() as i32).unwrap_or(-1),
                Some(SockState::Listener(l)) => {
                    l.local_addr().map(|a| a.port() as i32).unwrap_or(-1)
                }
                Some(SockState::Datagram { sock, .. }) => {
                    sock.local_addr().map(|a| a.port() as i32).unwrap_or(-1)
                }
                None => -1,
            };
            Some(Value::Int(p))
        }
        ("jdk/internal/net/Net", "localAddress", "(I)Ljava/lang/String;") => {
            let h = entero(&args[0]);
            let dir = {
                let t = SOCKETS.lock().unwrap();
                match t.get(h as usize).and_then(|e| e.as_ref()) {
                    Some(SockState::Stream(s)) => s.local_addr().ok().map(|a| a.ip().to_string()),
                    Some(SockState::Listener(l)) => l.local_addr().ok().map(|a| a.ip().to_string()),
                    Some(SockState::Datagram { sock, .. }) => {
                        sock.local_addr().ok().map(|a| a.ip().to_string())
                    }
                    None => None,
                }
            };
            match dir {
                Some(d) => Some(Value::Reference(strings::intern(metaspace, heap, &d))),
                None => Some(Value::Reference(0)),
            }
        }
        ("jdk/internal/net/Net", "remotePort", "(I)I") => {
            let h = entero(&args[0]);
            Some(Value::Int(con_flujo(h, -1, |s| {
                s.peer_addr().map(|a| a.port() as i32).unwrap_or(-1)
            })))
        }
        ("jdk/internal/net/Net", "remoteAddress", "(I)Ljava/lang/String;") => {
            let h = entero(&args[0]);
            let dir = con_flujo(h, None, |s| s.peer_addr().ok().map(|a| a.ip().to_string()));
            match dir {
                Some(d) => Some(Value::Reference(strings::intern(metaspace, heap, &d))),
                None => Some(Value::Reference(0)),
            }
        }
        // Cero significa **sin limite**, como en `Socket.setSoTimeout`. `Duration::ZERO` no sirve
        // para eso: en Rust un plazo de cero es un error, y el "sin limite" es `None`.
        //
        // La opcion se pone igual, pero **no es la que hace cumplir el plazo**: sobre un socket no
        // bloqueante el sistema contesta "todavia no" en el acto y nunca llega a vencer nada. Quien
        // cuenta el tiempo es el lado Java, que es el que sabe cuando empezo a esperar.
        ("jdk/internal/net/Net", "setSoTimeout", "(II)Z") => {
            let h = entero(&args[0]);
            let ms = entero(&args[1]);
            let d = if ms > 0 {
                Some(std::time::Duration::from_millis(ms as u64))
            } else {
                None
            };
            let ok = con_flujo(h, false, |s| s.set_read_timeout(d).is_ok());
            Some(Value::Int(i32::from(ok)))
        }
        ("jdk/internal/net/Net", "setTcpNoDelay", "(IZ)Z") => {
            let h = entero(&args[0]);
            let on = matches!(args[1], Value::Int(1));
            let ok = con_flujo(h, false, |s| s.set_nodelay(on).is_ok());
            Some(Value::Int(i32::from(ok)))
        }

        // La prueba de alcance de `InetAddress.isReachable`, en tres partes.
        //
        // **Que se prueba.** Un TCP al puerto 7 --el de `echo`, donde casi nunca hay nadie-- tomando
        // **el rechazo como respuesta**: el RST lo manda el host, asi que un "conexion rechazada"
        // prueba que esta vivo tanto como un "conectado". El silencio es el unico `false`. Es el
        // camino de reserva del JDK cuando no puede mandar un ICMP, que es lo normal: un ping crudo
        // necesita permisos que un proceso comun no tiene.
        //
        // **Por que son tres nativos y no uno.** `connect_timeout` no sirve: en Windows un rechazo
        // llega por el conjunto de excepciones del `select` y no por el de escritura, asi que Rust
        // lo reporta como `TimedOut` --se comprobo-- y el rechazo, que es justamente la respuesta
        // que mas prueba, quedaria indistinguible del silencio. El `connect` bloqueante **si** lo
        // distingue, pero bloquea, y ya se sabe lo que pasa cuando un nativo de aca bloquea.
        //
        // Asi que el `connect` bloqueante corre en un hilo del sistema aparte --uno de Rust, no de
        // Java: no ejecuta bytecode, solo escribe un entero-- y el lado Java pregunta con
        // `answerPoll` hasta que conteste o se acabe el plazo. Misma forma que `accept` y que
        // `read`: el -3 es "todavia no se sabe".
        //
        // El mismo mecanismo lo usa `connectFromStart`, que es el `connect` que ata la punta local
        // antes de salir. Bloquea por el mismo motivo --no hay forma de atar y conectar sin un
        // `connect` de verdad-- y se contesta por el mismo casillero.
        //
        // `local` vacia significa "por donde el sistema quiera" y `ttl` cero "el que venga por
        // omision": son los dos casos que `isReachable(null, 0, plazo)` pide, y los unicos en los que
        // no hace falta el socket crudo.
        ("jdk/internal/net/Net", "reachableStart", "(Ljava/lang/String;Ljava/lang/String;I)I") => {
            let host = strings::read(heap, reference(&args[0]));
            let local = strings::read(heap, reference(&args[1]));
            let ttl = entero(&args[2]).max(0) as u32;
            let (id, casillero) = nuevo_casillero();
            std::thread::spawn(move || {
                let vivo = if local.is_empty() && ttl == 0 {
                    // Sin placa ni TTL que elegir, `std` alcanza y no hace falta bajar al crudo.
                    match std::net::TcpStream::connect((host.as_str(), 7u16)) {
                        Ok(_) => true,
                        Err(e) => matches!(
                            e.kind(),
                            std::io::ErrorKind::ConnectionRefused
                                | std::io::ErrorKind::ConnectionReset
                        ),
                    }
                } else {
                    // Con placa o con TTL hay que armar el socket a mano. Aca **no** se puede
                    // distinguir el rechazo del silencio --`conectar_desde` devuelve `None` para los
                    // dos-- asi que una prueba por una placa concreta solo afirma que **llego**.
                    // Es menos de lo que dice la de un parametro, y esta dicho en el javadoc.
                    let remoto = resolver(&host, 7);
                    let salida = if local.is_empty() {
                        // El comodin de la familia del destino: no elige placa, pero deja poner TTL.
                        remoto.map(|r| match r {
                            std::net::SocketAddr::V4(_) => {
                                "0.0.0.0:0".parse::<std::net::SocketAddr>().unwrap()
                            }
                            std::net::SocketAddr::V6(_) => {
                                "[::]:0".parse::<std::net::SocketAddr>().unwrap()
                            }
                        })
                    } else {
                        resolver(&local, 0)
                    };
                    match (remoto, salida) {
                        (Some(r), Some(l)) => conectar_desde(r, l, ttl).is_some(),
                        _ => false,
                    }
                };
                *casillero.lock().unwrap() = Some(i32::from(vivo));
            });
            Some(Value::Int(id))
        }
        // Conecta atando primero la punta local. Devuelve el id del casillero; la respuesta es el
        // handle del socket, o -1.
        ("jdk/internal/net/Net", "connectFromStart", "(Ljava/lang/String;ILjava/lang/String;I)I") => {
            let host = strings::read(heap, reference(&args[0]));
            let puerto = entero(&args[1]) as u16;
            let local = strings::read(heap, reference(&args[2]));
            let puerto_local = entero(&args[3]) as u16;
            let (id, casillero) = nuevo_casillero();
            std::thread::spawn(move || {
                let remoto = resolver(&host, puerto);
                let salida = if local.is_empty() {
                    remoto.map(|r| match r {
                        std::net::SocketAddr::V4(_) => std::net::SocketAddr::from((
                            std::net::Ipv4Addr::UNSPECIFIED,
                            puerto_local,
                        )),
                        std::net::SocketAddr::V6(_) => std::net::SocketAddr::from((
                            std::net::Ipv6Addr::UNSPECIFIED,
                            puerto_local,
                        )),
                    })
                } else {
                    resolver(&local, puerto_local)
                };
                let r = match (remoto, salida) {
                    (Some(r), Some(l)) => match conectar_desde(r, l, 0) {
                        Some(s) => {
                            // Igual que en `connect`: de aca en adelante el socket no espera.
                            let _ = s.set_nonblocking(true);
                            guardar_socket(SockState::Stream(s))
                        }
                        None => -1,
                    },
                    _ => -1,
                };
                *casillero.lock().unwrap() = Some(r);
            });
            Some(Value::Int(id))
        }
        // La respuesta, o **-3** si todavia no llego. Que significa depende de quien pregunte: la
        // sonda de alcance contesta 1 o 0, el connect contesta el handle o -1.
        ("jdk/internal/net/Net", "answerPoll", "(I)I") => {
            let id = entero(&args[0]) as usize;
            let t = SONDAS.lock().unwrap();
            let r = match t.get(id).and_then(|e| e.as_ref()) {
                Some(c) => match *c.lock().unwrap() {
                    Some(v) => v,
                    None => -3,
                },
                // Un casillero que no existe es un error del que llama, no una espera eterna.
                None => -1,
            };
            Some(Value::Int(r))
        }
        // Suelta el casillero. El hilo que quedo colgado del `connect` termina solo y escribe en un
        // casillero que ya no mira nadie, que es todo lo que puede hacer de malo.
        ("jdk/internal/net/Net", "answerFree", "(I)V") => {
            let id = entero(&args[0]) as usize;
            let mut t = SONDAS.lock().unwrap();
            if let Some(e) = t.get_mut(id) {
                *e = None;
            }
            None
        }
        // Un byte **fuera de banda**. No es escribir en el flujo: va con una bandera del protocolo,
        // y el que lo recibe lo ve por un camino aparte.
        ("jdk/internal/net/Net", "sendUrgent", "(II)Z") => {
            let h = entero(&args[0]);
            let b = entero(&args[1]) as u8;
            let ok = match tomar_flujo(h) {
                Some(f) => crudo::fuera_de_banda(&f, b),
                None => false,
            };
            Some(Value::Int(i32::from(ok)))
        }
        // --- UDP: la otra mitad de `jdk/internal/net/Net` ------------------------------------
        //
        // Mismas reglas que TCP: nada bloquea, y "todavia no llego nada" es **-3**. Un datagrama no
        // tiene fin de flujo --no hay conexion que cerrar-- asi que aca el -1 es siempre un error de
        // verdad y no hay -2.

        // Ata un socket de datagramas. Puerto cero: lo elige el sistema, y se lee con `localPort`.
        ("jdk/internal/net/Net", "udpBind", "(Ljava/lang/String;I)I") => {
            let host = strings::read(heap, reference(&args[0]));
            let puerto = entero(&args[1]) as u16;
            match std::net::UdpSocket::bind((host.as_str(), puerto)) {
                Ok(s) => {
                    let _ = s.set_nonblocking(true);
                    // La difusion se pide explicitamente en casi todos los sistemas, y el lado Java
                    // promete que `setBroadcast(true)` --su valor por omision-- funciona.
                    let _ = s.set_broadcast(true);
                    Some(Value::Int(guardar_socket(SockState::Datagram { sock: s, ultimo: None })))
                }
                Err(_) => Some(Value::Int(-1)),
            }
        }
        // Manda un datagrama. `true` solo si salio entero: un datagrama partido no es un datagrama.
        ("jdk/internal/net/Net", "udpSend", "(ILjava/lang/String;I[BII)Z") => {
            let h = entero(&args[0]);
            let host = strings::read(heap, reference(&args[1]));
            let puerto = entero(&args[2]) as u16;
            let arr = reference(&args[3]);
            let off = entero(&args[4]) as usize;
            let len = entero(&args[5]) as usize;
            if arr == 0 {
                return NativeOutcome::Ran(Some(Value::Int(0)));
            }
            let mut bytes = Vec::with_capacity(len);
            for i in 0..len {
                bytes.push(heap.read_u8(arr + array_operations::ARRAY_HEADER_SIZE + off + i));
            }
            let ok = match tomar_datagrama(h) {
                Some(s) => s.send_to(&bytes, (host.as_str(), puerto)).map(|n| n == len).unwrap_or(false),
                None => false,
            };
            Some(Value::Int(i32::from(ok)))
        }
        // Recibe un datagrama. Devuelve cuantos bytes puso, **-3** si todavia no llego nada, -1 si
        // fallo. Deja anotado el remitente para `udpSenderAddress`/`udpSenderPort`.
        ("jdk/internal/net/Net", "udpReceive", "(I[BII)I") => {
            let h = entero(&args[0]);
            let arr = reference(&args[1]);
            let off = entero(&args[2]) as usize;
            let len = entero(&args[3]) as usize;
            if arr == 0 {
                return NativeOutcome::Ran(Some(Value::Int(-1)));
            }
            let Some(sock) = tomar_datagrama(h) else {
                return NativeOutcome::Ran(Some(Value::Int(-1)));
            };
            let mut buf = vec![0u8; len];
            match sock.recv_from(&mut buf) {
                Ok((n, dir)) => {
                    for i in 0..n.min(len) {
                        heap.write_u8(arr + array_operations::ARRAY_HEADER_SIZE + off + i, buf[i]);
                    }
                    let mut t = SOCKETS.lock().unwrap();
                    if let Some(SockState::Datagram { ultimo, .. }) =
                        t.get_mut(h as usize).and_then(|e| e.as_mut())
                    {
                        *ultimo = Some((dir.ip().to_string(), dir.port()));
                    }
                    Some(Value::Int(n.min(len) as i32))
                }
                Err(e) if e.kind() == std::io::ErrorKind::WouldBlock => Some(Value::Int(-3)),
                Err(_) => Some(Value::Int(-1)),
            }
        }
        // De quien vino el ultimo datagrama recibido. `null` si todavia no se recibio ninguno.
        ("jdk/internal/net/Net", "udpSenderAddress", "(I)Ljava/lang/String;") => {
            let h = entero(&args[0]);
            let dir = {
                let t = SOCKETS.lock().unwrap();
                match t.get(h as usize).and_then(|e| e.as_ref()) {
                    Some(SockState::Datagram { ultimo, .. }) => ultimo.clone(),
                    _ => None,
                }
            };
            match dir {
                Some((d, _)) => Some(Value::Reference(strings::intern(metaspace, heap, &d))),
                None => Some(Value::Reference(0)),
            }
        }
        ("jdk/internal/net/Net", "udpSenderPort", "(I)I") => {
            let h = entero(&args[0]);
            let t = SOCKETS.lock().unwrap();
            let p = match t.get(h as usize).and_then(|e| e.as_ref()) {
                Some(SockState::Datagram { ultimo, .. }) => {
                    ultimo.as_ref().map(|(_, p)| *p as i32).unwrap_or(-1)
                }
                _ => -1,
            };
            Some(Value::Int(p))
        }
        // Entra a un grupo multicast. `interfaz` vacia significa "la que elija el sistema".
        ("jdk/internal/net/Net", "udpJoin", "(ILjava/lang/String;Ljava/lang/String;)Z") => {
            let h = entero(&args[0]);
            let grupo = strings::read(heap, reference(&args[1]));
            let interfaz = strings::read(heap, reference(&args[2]));
            Some(Value::Int(i32::from(membresia(h, &grupo, &interfaz, true))))
        }
        ("jdk/internal/net/Net", "udpLeave", "(ILjava/lang/String;Ljava/lang/String;)Z") => {
            let h = entero(&args[0]);
            let grupo = strings::read(heap, reference(&args[1]));
            let interfaz = strings::read(heap, reference(&args[2]));
            Some(Value::Int(i32::from(membresia(h, &grupo, &interfaz, false))))
        }
        // El limite de saltos de los paquetes multicast que salgan de aca.
        ("jdk/internal/net/Net", "udpSetTtl", "(II)Z") => {
            let h = entero(&args[0]);
            let ttl = entero(&args[1]);
            let ok = match tomar_datagrama(h) {
                Some(s) => s.set_multicast_ttl_v4(ttl as u32).is_ok(),
                None => false,
            };
            Some(Value::Int(i32::from(ok)))
        }
        // --- Procesos hijo: `jdk/internal/proc/Proc` -----------------------------
        //
        // La costura que faltaba para `ProcessBuilder.start()`. Hasta ahora la VM no sabía lanzar
        // procesos, y por eso `start()` y `startPipeline()` quedaban sin declarar --un `Process` que
        // no representa ningún proceso no es un miembro que se pueda escribir--.
        //
        // El diseño es el mismo que el de `Fs`: el nativo hace lo mínimo y **no sabe nada de las
        // clases de Java**. Toma y devuelve cadenas, arreglos y enteros; quién sea `Process` o
        // `ProcessBuilder` es problema del lado Java, que puede cambiar sin tocar Rust.
        //
        // A diferencia de `Fs`, acá **sí hay handle**: un proceso es estado que vive entre llamadas
        // --su salida, sus tuberías, su código de salida-- y no hay forma de representarlo con
        // operaciones de una sola vez. El handle es un índice en una tabla por hilo; las entradas no
        // se reciclan, así que un handle viejo nunca apunta a un proceso nuevo (el error que sí sería
        // difícil de encontrar).
        //
        // Los modos de redirección son los tres que `ProcessBuilder.Redirect` distingue de verdad:
        // 0 = tubería, 1 = heredar, 2 = descartar, 3 = archivo (la ruta va en el arreglo de rutas).

        // Lanza el proceso. Devuelve el handle, o -1 si no se pudo (ejecutable inexistente, permisos).
        // Devolver -1 y no tirar es lo que deja que el lado Java arme la `IOException` con el mensaje
        // que corresponde -- el nativo no distingue "no existe" de "no se puede ejecutar".
        ("jdk/internal/proc/Proc", "spawn",
         "([Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;[IZ)I") => {
            let cmd = leer_arreglo_de_cadenas(heap, reference(&args[0]));
            if cmd.is_empty() {
                return NativeOutcome::Ran(Some(Value::Int(-1)));
            }
            let dir_ref = reference(&args[1]);
            let env = leer_arreglo_de_cadenas(heap, reference(&args[2]));
            let rutas = leer_arreglo_de_cadenas_con_nulos(heap, reference(&args[3]));
            let modos = leer_arreglo_de_int(heap, reference(&args[4]));
            let unir_error = matches!(args[5], Value::Int(1));

            let mut c = std::process::Command::new(&cmd[0]);
            c.args(&cmd[1..]);
            if dir_ref != 0 {
                c.current_dir(strings::read(heap, dir_ref));
            }
            // Un `env` no nulo **reemplaza** el entorno entero, como `ProcessBuilder.environment()`:
            // el mapa que el llamador manipuló es el entorno del hijo, no un agregado al nuestro.
            if !env.is_empty() {
                c.env_clear();
                let mut i = 0;
                while i + 1 < env.len() {
                    c.env(&env[i], &env[i + 1]);
                    i += 2;
                }
            }
            let modo = |k: usize| -> i32 { modos.get(k).copied().unwrap_or(0) };
            let ruta = |k: usize| -> Option<String> { rutas.get(k).cloned().flatten() };
            c.stdin(redireccion_entrada(modo(0), ruta(0)));
            c.stdout(redireccion_salida(modo(1), ruta(1)));
            // Con `redirectErrorStream` el error va a la misma tubería que la salida, y eso se hace
            // duplicando el destino de stdout -- no se puede clonar el pipe desde acá, así que el
            // lado Java lee un solo flujo y `getErrorStream()` devuelve uno vacío, tal como el JDK.
            if unir_error {
                // Su propia tuberia, y la union se hace al leer -- ver `unir_error` en `ProcState`.
                // Si la salida no es tuberia (archivo, heredada), se manda al mismo destino, que ahi
                // si funciona: dos escritores al mismo archivo o a la misma consola se mezclan solos.
                c.stderr(if modo(1) == 0 {
                    std::process::Stdio::piped()
                } else {
                    redireccion_salida(modo(1), ruta(1))
                });
            } else {
                c.stderr(redireccion_salida(modo(2), ruta(2)));
            }
            match c.spawn() {
                Ok(hijo) => Some(Value::Int(PROCS.with(|t| {
                    let mut t = t.borrow_mut();
                    t.push(Some(ProcState { hijo, salida: None, unir_error }));
                    (t.len() - 1) as i32
                }))),
                Err(_) => Some(Value::Int(-1)),
            }
        }
        // Espera a que termine y devuelve su código de salida. Si el handle no vale, -1.
        ("jdk/internal/proc/Proc", "waitFor", "(I)I") => {
            let h = entero(&args[0]) as usize;
            Some(Value::Int(PROCS.with(|t| {
                let mut t = t.borrow_mut();
                match t.get_mut(h).and_then(|e| e.as_mut()) {
                    Some(p) => {
                        if let Some(c) = p.salida {
                            return c;
                        }
                        match p.hijo.wait() {
                            Ok(st) => {
                                let c = st.code().unwrap_or(-1);
                                p.salida = Some(c);
                                c
                            }
                            Err(_) => -1,
                        }
                    }
                    None => -1,
                }
            })))
        }
        // El código de salida si ya terminó. `i32::MIN` es el centinela de "sigue corriendo": es lo
        // que le permite al lado Java tirar `IllegalThreadStateException`, que es lo que el contrato
        // pide, en vez de bloquearse.
        ("jdk/internal/proc/Proc", "exitValue", "(I)I") => {
            let h = entero(&args[0]) as usize;
            Some(Value::Int(PROCS.with(|t| {
                let mut t = t.borrow_mut();
                match t.get_mut(h).and_then(|e| e.as_mut()) {
                    Some(p) => {
                        if let Some(c) = p.salida {
                            return c;
                        }
                        match p.hijo.try_wait() {
                            Ok(Some(st)) => {
                                let c = st.code().unwrap_or(-1);
                                p.salida = Some(c);
                                c
                            }
                            _ => i32::MIN,
                        }
                    }
                    None => i32::MIN,
                }
            })))
        }
        ("jdk/internal/proc/Proc", "isAlive", "(I)Z") => {
            let h = entero(&args[0]) as usize;
            Some(Value::Int(PROCS.with(|t| {
                let mut t = t.borrow_mut();
                match t.get_mut(h).and_then(|e| e.as_mut()) {
                    Some(p) => {
                        if p.salida.is_some() {
                            return 0;
                        }
                        match p.hijo.try_wait() {
                            Ok(Some(st)) => {
                                p.salida = Some(st.code().unwrap_or(-1));
                                0
                            }
                            Ok(None) => 1,
                            Err(_) => 0,
                        }
                    }
                    None => 0,
                }
            })))
        }
        // Mata el proceso. `forzar` se acepta y no cambia nada en Windows, donde no hay una señal
        // "amable" -- se dice acá y se documenta del lado Java en vez de fingir dos comportamientos.
        ("jdk/internal/proc/Proc", "destroy", "(IZ)V") => {
            let h = entero(&args[0]) as usize;
            PROCS.with(|t| {
                let mut t = t.borrow_mut();
                if let Some(p) = t.get_mut(h).and_then(|e| e.as_mut()) {
                    let _ = p.hijo.kill();
                }
            });
            None
        }
        ("jdk/internal/proc/Proc", "pid", "(I)J") => {
            let h = entero(&args[0]) as usize;
            Some(Value::Long(PROCS.with(|t| {
                let t = t.borrow();
                t.get(h).and_then(|e| e.as_ref()).map(|p| i64::from(p.hijo.id())).unwrap_or(-1)
            })))
        }
        // Escribe en la entrada estándar del hijo. `true` si se pudo.
        ("jdk/internal/proc/Proc", "writeIn", "(I[BII)Z") => {
            use std::io::Write;
            let h = entero(&args[0]) as usize;
            let arr = reference(&args[1]);
            let off = entero(&args[2]) as usize;
            let len = entero(&args[3]) as usize;
            if arr == 0 {
                return NativeOutcome::Ran(Some(Value::Int(0)));
            }
            let mut bytes = Vec::with_capacity(len);
            for i in 0..len {
                bytes.push(heap.read_u8(arr + array_operations::ARRAY_HEADER_SIZE + off + i));
            }
            Some(Value::Int(PROCS.with(|t| {
                let mut t = t.borrow_mut();
                match t.get_mut(h).and_then(|e| e.as_mut()).and_then(|p| p.hijo.stdin.as_mut()) {
                    Some(w) => i32::from(w.write_all(&bytes).and_then(|_| w.flush()).is_ok()),
                    None => 0,
                }
            })))
        }
        // Cierra la entrada del hijo, que es como se le dice "no viene más".
        ("jdk/internal/proc/Proc", "closeIn", "(I)V") => {
            let h = entero(&args[0]) as usize;
            PROCS.with(|t| {
                let mut t = t.borrow_mut();
                if let Some(p) = t.get_mut(h).and_then(|e| e.as_mut()) {
                    p.hijo.stdin = None; // al soltarlo se cierra
                }
            });
            None
        }
        // Lee de la salida del hijo. Devuelve cuántos bytes puso, o -1 en fin de flujo. Bloquea, que
        // es lo que un `InputStream` promete.
        ("jdk/internal/proc/Proc", "readOut", "(I[B)I") => {
            let h = entero(&args[0]) as usize;
            let arr = reference(&args[1]);
            leer_de_hijo(heap, h, arr, true)
        }
        ("jdk/internal/proc/Proc", "readErr", "(I[B)I") => {
            let h = entero(&args[0]) as usize;
            let arr = reference(&args[1]);
            leer_de_hijo(heap, h, arr, false)
        }

        // --- Introspection / identity (things Java can't read of itself) ---------
        // getClass(): the receiver's header `class_id` *is* its Class<…> mirror.
        ("java/lang/Object", "getClass", "()Ljava/lang/Class;") => {
            Some(Value::Reference(heap.read_u32(reference(&args[0])) as usize))
        }
        // hashCode() (identity): se calcula una vez y se guarda en el encabezado, para que
        // **no cambie** cuando el recolector mueva el objeto (#302). Antes era el offset a secas.
        ("java/lang/Object", "hashCode", "()I") => {
            Some(Value::Int(heap.identity_hash(reference(&args[0]))))
        }
        // Throwable.toString(): "pkg.Class" or "pkg.Class: message". Reads the receiver's runtime
        // class name (Java has no Class.getName() yet) and the `message` field, then interns the
        // text. Called virtually, so a subclass instance (e.g. NullPointerException) reports its
        // own class name.
        ("java/lang/Throwable", "toString", "()Ljava/lang/String;") => {
            let this_ref = reference(&args[0]);
            let class_id = heap.read_u32(this_ref) as usize;
            let internal = metaspace
                .class_name_at_mirror(class_id)
                .unwrap_or("java/lang/Throwable")
                .to_string();
            let dotted = internal.replace('/', ".");
            let msg_off = field_offset(metaspace, &internal, "message");
            let msg_ref = heap.read_u32(this_ref + msg_off) as usize;
            let text = if msg_ref == 0 {
                dotted
            } else {
                format!("{dotted}: {}", strings::read(heap, msg_ref))
            };
            Some(Value::Reference(strings::intern(metaspace, heap, &text)))
        }
        // System.identityHashCode(Object): the same, as a static.
        ("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I") => {
            Some(Value::Int(heap.identity_hash(reference(&args[0]))))
        }
        // System.nanoTime(): a monotonic timer with an arbitrary origin (elapsed since the first
        // call in this process). Backs scheduling delays; never goes backwards.
        ("java/lang/System", "nanoTime", "()J") => {
            use std::sync::OnceLock;
            use std::time::Instant;
            static START: OnceLock<Instant> = OnceLock::new();
            let start = START.get_or_init(Instant::now);
            Some(Value::Long(start.elapsed().as_nanos() as i64))
        }
        // System.mapLibraryName(name): the platform's file name for a native library, e.g.
        // `foo` -> `foo.dll` on Windows, `libfoo.dylib` on macOS, `libfoo.so` elsewhere. Native
        // because the mapping is a property of the host, not of bytecode. (KajiJDK loads no native
        // libraries -- see load/loadLibrary -- but the name it *would* look for is still well-defined.)
        ("java/lang/System", "mapLibraryName", "(Ljava/lang/String;)Ljava/lang/String;") => {
            let name = strings::read(heap, reference(&args[0]));
            let mapped = if cfg!(windows) {
                format!("{name}.dll")
            } else if cfg!(target_os = "macos") {
                format!("lib{name}.dylib")
            } else {
                format!("lib{name}.so")
            };
            Some(Value::Reference(strings::intern(metaspace, heap, &mapped)))
        }
        // System.setIn0/setOut0/setErr0(stream): the native seams behind setIn/setOut/setErr. They
        // exist because `in`/`out`/`err` are `public static final`: bytecode cannot reassign a final
        // field, but native code writes the slot directly (exactly how the JDK does it). A reference
        // static is a single slot holding a heap offset, written like `putstatic` (no barrier: the
        // mirror's statics are scanned as GC roots).
        ("java/lang/System", "setIn0", "(Ljava/io/InputStream;)V")
        | ("java/lang/System", "setOut0", "(Ljava/io/PrintStream;)V")
        | ("java/lang/System", "setErr0", "(Ljava/io/PrintStream;)V") => {
            let field = match name {
                "setIn0" => "in",
                "setOut0" => "out",
                _ => "err",
            };
            let at = class_operations::static_slot(metaspace, heap, "java/lang/System", field);
            heap.write_u32(at, reference(&args[0]) as u32);
            None
        }

        // Runtime.availableProcessors(): how many CPUs the VM can actually use — the host's
        // parallelism, which only the OS can answer. Falls back to 1 (the value the JVMS
        // permits when the count is unavailable) rather than failing.
        ("java/lang/Runtime", "availableProcessors", "()I") => Some(Value::Int(
            std::thread::available_parallelism().map_or(1, |n| n.get() as i32),
        )),
        // Runtime.gc(): a hint. KajiJDK's GC runs on its own schedule; an explicit request is a
        // no-op (a correct answer -- gc() has never been a guarantee) rather than a forced cycle.
        ("java/lang/Runtime", "gc", "()V") => None,
        // Memory accounting: KajiJDK does not expose the heap's real figures, so it reports a fixed,
        // plausible budget -- enough for callers that only compare or log these.
        ("java/lang/Runtime", "maxMemory", "()J") => Some(Value::Long(256 * 1024 * 1024)),
        ("java/lang/Runtime", "totalMemory", "()J") => Some(Value::Long(64 * 1024 * 1024)),
        ("java/lang/Runtime", "freeMemory", "()J") => Some(Value::Long(32 * 1024 * 1024)),

        // --- Math (would map to CPU instructions under a JIT) --------------------
        ("java/lang/Math", "abs", "(I)I") => Some(Value::Int(int(&args[0]).abs())),
        ("java/lang/Math", "max", "(II)I") => Some(Value::Int(int(&args[0]).max(int(&args[1])))),
        ("java/lang/Math", "min", "(II)I") => Some(Value::Int(int(&args[0]).min(int(&args[1])))),

        // --- Integer bit ops (popcnt / lzcnt) -----------------------------------
        ("java/lang/Integer", "bitCount", "(I)I") => {
            Some(Value::Int(int(&args[0]).count_ones() as i32))
        }
        ("java/lang/Integer", "numberOfLeadingZeros", "(I)I") => {
            Some(Value::Int(int(&args[0]).leading_zeros() as i32))
        }

        // --- Arrays: System.arraycopy -------------------------------------------
        // La copia a granel entre arrays. Dos cosas que parecen detalle y no lo son:
        //
        // 1. **El ancho del elemento sale de la CLASE del array**, no de una suposición. Un
        //    `char[]` mide dos bytes por elemento y un `long[]` ocho; copiar todo con paso de
        //    cuatro no solo escribe basura, además se **sale del array por el final** — que es
        //    de dónde salían los pánicos "range end index N out of range for slice of length
        //    N-4" en cuanto algo copiaba texto.
        //
        // 2. **El solapamiento**. El contrato dice que se comporta como si el origen se copiara
        //    primero a un buffer temporal, así que con el mismo array y rangos que se pisan la
        //    dirección del recorrido decide el resultado. Un `delete` de un `StringBuilder` es
        //    exactamente ese caso, y hacia abajo funcionaba por casualidad.
        ("java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V") => {
            use super::bytecode_interpreter::array_operations::{
                array_element_width, ARRAY_HEADER_SIZE,
            };
            let (src, src_pos) = (reference(&args[0]), int(&args[1]) as usize);
            let (dst, dst_pos) = (reference(&args[2]), int(&args[3]) as usize);
            let length = int(&args[4]) as usize;
            // Un origen o un destino nulos son un `NullPointerException` (JLS §11.5), no un
            // invariante roto de la VM: hasta acá esto entraba en pánico y se llevaba puesto el
            // programa entero por un null que Java define como atrapable.
            if src == 0 || dst == 0 {
                return NativeOutcome::Lanza("java/lang/NullPointerException".to_string());
            }
            let Some(class) =
                metaspace.class_name_at_mirror(heap.read_u32(src) as usize).map(str::to_string)
            else {
                // Lo que llegó no es un array. `ArrayStoreException` es lo que la especificación
                // pide para eso, y sigue siendo un error del programa y no de la máquina.
                return NativeOutcome::Lanza("java/lang/ArrayStoreException".to_string());
            };
            let width = array_element_width(&class);
            let from = src + ARRAY_HEADER_SIZE + src_pos * width;
            let to = dst + ARRAY_HEADER_SIZE + dst_pos * width;
            // Un array de referencias se copia slot a slot y **por la puerta del barrier**: un
            // `write_u8` dejaría al GC sin enterarse de la referencia nueva.
            let of_references = matches!(class.as_bytes().get(1), Some(b'L' | b'['));
            if of_references {
                let slots: Vec<usize> =
                    (0..length).map(|i| heap.read_u32(from + i * width) as usize).collect();
                for (i, value) in slots.into_iter().enumerate() {
                    heap.store_reference(dst, to + i * width, value);
                }
            } else if to > from {
                for i in (0..length * width).rev() {
                    let byte = heap.read_u8(from + i);
                    heap.write_u8(to + i, byte);
                }
            } else {
                for i in 0..length * width {
                    let byte = heap.read_u8(from + i);
                    heap.write_u8(to + i, byte);
                }
            }
            None
        }

        // --- Serializacion: las dos preguntas que la reflexion no contesta -------------------
        //
        // `ObjectStreamClass.hasStaticInitializer(Class)` -- si la clase declara `<clinit>`.
        //
        // Entra en el `serialVersionUID` calculado (el bit `0x08` de los modificadores de la forma
        // canonica), y **no hay manera de averiguarlo por reflexion**: `getDeclaredMethods` filtra
        // `<clinit>` a proposito, aca y en el JDK. Por eso el JDK tambien lo resuelve con un nativo
        // y no con reflexion; sin el, el UID sale bien para las clases sin bloque estatico y mal
        // para las demas, que son la mayoria.
        //
        // La respuesta sale del archivo de clase, que es donde vive el dato: un metodo llamado
        // `<clinit>`. Una primitiva o un arreglo no tienen archivo detras, y contestan `false`.
        ("java/io/ObjectStreamClass", "hasStaticInitializer", "(Ljava/lang/Class;)Z") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            let tiene = if name.starts_with('[') || is_primitive_name(&name) {
                false
            } else {
                metaspace
                    .get_or_load(&name)
                    .map(|cf| {
                        cf.methods
                            .iter()
                            .any(|m| cf.utf8(m.name_index) == Some("<clinit>"))
                    })
                    .unwrap_or(false)
            };
            Some(Value::Int(tiene as i32))
        }
        // `ObjectStreamClass.allocateInstance(Class)` -- una instancia con todos sus campos en el
        // valor por defecto y **sin correr ningun constructor**.
        //
        // Es la unica pieza de la deserializacion que no se puede escribir en Java. Reconstruir un
        // objeto no es construirlo: los campos vienen del flujo, y correr el constructor de la
        // clase ejecutaria sus efectos --validaciones, contadores, registros en tablas globales--
        // por un objeto que no se esta creando sino leyendo. La especificacion de serializacion lo
        // dice al reves de como suena: del constructor **solo** corre el de la primera superclase
        // no serializable, y de ahi para abajo nada.
        //
        // Devuelve `null` --y no un objeto a medias-- para lo que no se puede instanciar: una
        // interfaz, una abstracta, un arreglo o una primitiva. El que llama lo convierte en la
        // excepcion que corresponda; el nativo no puede tirar.
        ("java/io/ObjectStreamClass", "allocateInstance", "(Ljava/lang/Class;)Ljava/lang/Object;") => {
            const ACC_INTERFACE: u16 = 0x0200;
            const ACC_ABSTRACT: u16 = 0x0400;
            let name = mirror_name(metaspace, reference(&args[0]));
            if name.starts_with('[') || is_primitive_name(&name) {
                return NativeOutcome::Ran(Some(Value::Reference(0)));
            }
            let instanciable = metaspace
                .get_or_load(&name)
                .map(|cf| cf.access_flags & (ACC_INTERFACE | ACC_ABSTRACT) == 0)
                .unwrap_or(false);
            if !instanciable {
                return NativeOutcome::Ran(Some(Value::Reference(0)));
            }
            // `try_allocate` y no `allocate`: quedarse sin heap deserializando es recuperable, y
            // el que llama lo ve como el `null` de arriba en vez de bajar la VM.
            let objeto = objects_operations::try_allocate(metaspace, heap, &name).unwrap_or(0);
            Some(Value::Reference(objeto))
        }

        // --- Class.isInstance: the subtype check, reusing is_subtype -------------
        // The receiver is a Class mirror; args[1] is the object to test. `null` is
        // never an instance.
        ("java/lang/Class", "isInstance", "(Ljava/lang/Object;)Z") => {
            let object = reference(&args[1]);
            if object == 0 {
                return NativeOutcome::Ran(Some(Value::Int(0)));
            }
            let target = metaspace.class_name_at_mirror(reference(&args[0])).map(str::to_string);
            let runtime =
                metaspace.class_name_at_mirror(heap.read_u32(object) as usize).map(str::to_string);
            let is = match (target, runtime) {
                (Some(t), Some(r)) => class_operations::is_subtype(metaspace, &r, &t),
                _ => false,
            };
            Some(Value::Int(is as i32))
        }
        // --- Class.isAnnotationPresent: JSR 175 reflection over §4.7.16 ----------
        // Both the receiver and the argument are Class mirrors, so both heap offsets key
        // `class_name_at_mirror`. The receiver's class file holds its `RuntimeVisibleAnnotations`
        // (only RUNTIME-retention annotations are written there — that's javac's job, so the
        // retention rule falls out for free); each entry names its annotation type by *descriptor*,
        // which is what the argument's name becomes as `L…;`. A mirror with no class file behind
        // it (a primitive, an array) has no attributes → false.
        //
        // Only *directly present* class-level annotations count: no @Inherited walk up the
        // superclass chain, and no field/method/parameter annotations (we have no Field/Method
        // model to hang them on). `getAnnotation` is deliberately absent — returning an annotation
        // *object* would mean synthesising a proxy class implementing the @interface, as the JDK
        // does; presence is the part of the API that needs no such object.
        // `annotationPresent0` es el nombre de la costura privada de KajiLibrary;
        // `isAnnotationPresent` el de la copia compilada de `boot/`. Los dos, mientras convivan.
        ("java/lang/Class", "annotationPresent0" | "isAnnotationPresent", "(Ljava/lang/Class;)Z") => {
            let this = metaspace
                .class_name_at_mirror(reference(&args[0]))
                .expect("Class.isAnnotationPresent: no class at this mirror")
                .to_string();
            let wanted = metaspace
                .class_name_at_mirror(reference(&args[1]))
                .map(|name| format!("L{name};"))
                .expect("Class.isAnnotationPresent: no class at the argument mirror");
            let present = metaspace
                .get_or_load(&this)
                .map(ClassFile::runtime_visible_annotation_types)
                .is_some_and(|types| types.contains(&wanted));
            Some(Value::Int(present as i32))
        }
        // --- Class.getAnnotation & friends: reflection that hands back the annotation as an OBJECT.
        // Where `annotationPresent0` answers *presence* by a descriptor compare, this materialises a
        // real instance of each @interface. For every entry in `RuntimeVisibleAnnotations`, the VM
        // spins a class implementing the @interface whose element methods return the values written
        // at the use site (falling back to the @interface's defaults), then allocates one — an
        // annotation object carries no instance fields, so allocation alone is a complete object and
        // no `<init>` needs to run. The Java side filters this array by type
        // (getAnnotation/getAnnotationsByType/…). No @Inherited walk: only *directly present*
        // class-level annotations, matching `isAnnotationPresent`.
        // Las anotaciones **del metodo**, no de su clase.
        //
        // El mecanismo es el mismo que el de `Class.declaredAnnotations0` --resolver el atributo y
        // fabricar una clase sintetica por anotacion-- solo que el atributo se busca en el
        // `method_info` en vez de en el `ClassFile`. Hizo falta cuando el compilador dejo de perder
        // las meta-anotaciones (finding #467): hasta entonces ningun `.class` de esta biblioteca
        // llevaba anotaciones de metodo, asi que `Method.getAnnotation` podia devolver null sin
        // mentir. Ahora si las lleva.
        ("java/lang/reflect/Method", "declaredAnnotations0", "()[Ljava/lang/annotation/Annotation;") => {
            let this = reference(&args[0]);
            let clazz_at = field_offset(metaspace, "java/lang/reflect/Method", "clazz");
            let owner = mirror_name(metaspace, heap.read_u32(this + clazz_at) as usize);
            let name_at = field_offset(metaspace, "java/lang/reflect/Method", "name");
            let name = strings::read(heap, heap.read_u32(this + name_at) as usize);
            let params_at = field_offset(metaspace, "java/lang/reflect/Method", "parameterTypes");
            let ret_at = field_offset(metaspace, "java/lang/reflect/Method", "returnType");
            let params = heap.read_u32(this + params_at) as usize;
            let mut descriptor = String::from("(");
            if params != 0 {
                let n = heap.read_u32(params + array_operations::LENGTH_OFFSET) as usize;
                for k in 0..n {
                    let at = params + array_operations::ARRAY_HEADER_SIZE + k * SLOT_SIZE;
                    let mirror = heap.read_u32(at) as usize;
                    descriptor.push_str(&descriptor_of(&mirror_name(metaspace, mirror)));
                }
            }
            descriptor.push(')');
            let ret = heap.read_u32(this + ret_at) as usize;
            descriptor.push_str(&descriptor_of(&mirror_name(metaspace, ret)));
            let objects = method_annotation_objects(metaspace, heap, &owner, &name, &descriptor);
            Some(Value::Reference(reference_array(
                metaspace,
                heap,
                "[Ljava/lang/annotation/Annotation;",
                &objects,
            )))
        }
        // Los `access_flags` **crudos** del class file, sin las correcciones que `getModifiers`
        // aplica a una clase anidada -- que ahi devuelve los del `InnerClasses` y no los del
        // encabezado. La diferencia importa justo para lo que este metodo se usa: decidir accesos.
        ("jdk/internal/reflect/Reflection", "getClassAccessFlags", "(Ljava/lang/Class;)I") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            let flags = metaspace.get_or_load(&name).map_or(0, |cf| cf.access_flags as i32);
            Some(Value::Int(flags))
        }
        ("java/lang/Class", "declaredAnnotations0", "()[Ljava/lang/annotation/Annotation;") => {
            let this = mirror_name(metaspace, reference(&args[0]));
            let objects = annotation_objects(metaspace, heap, &this);
            Some(Value::Reference(reference_array(
                metaspace,
                heap,
                "[Ljava/lang/annotation/Annotation;",
                &objects,
            )))
        }
        ("java/lang/Class", "descriptorString", "()Ljava/lang/String;") => {
            // The field descriptor of the class this mirror names. A **primitive** mirror is named
            // by its type name (`int`, …) → a one-letter descriptor; an array's internal name *is*
            // already a descriptor (`[I`, `[Ljava/lang/String;`); a class/interface name
            // (`java/lang/String`) becomes `L…;`.
            let name = metaspace
                .class_name_at_mirror(reference(&args[0]))
                .map(str::to_string)
                .expect("Class.descriptorString: no class at this mirror");
            let descriptor = match name.as_str() {
                "int" => "I".to_string(),
                "long" => "J".to_string(),
                "double" => "D".to_string(),
                "float" => "F".to_string(),
                "short" => "S".to_string(),
                "byte" => "B".to_string(),
                "char" => "C".to_string(),
                "boolean" => "Z".to_string(),
                "void" => "V".to_string(),
                n if n.starts_with('[') => n.to_string(),
                n => format!("L{n};"),
            };
            let offset = strings::intern(metaspace, heap, &descriptor);
            Some(Value::Reference(offset))
        }
        ("java/lang/Class", "getName", "()Ljava/lang/String;") => {
            // The JDK-format name of the class this mirror names. The receiver *is* the mirror,
            // so its heap offset keys `class_name_at_mirror` directly. Classes/interfaces get the
            // dotted binary name ("java.lang.String"); an array's internal name is already
            // descriptor-shaped, so it comes out in descriptor form with dots ("[I",
            // "[Ljava.lang.String;"). Both are just '/' → '.' on the internal name.
            let internal = metaspace
                .class_name_at_mirror(reference(&args[0]))
                .expect("Class.getName: no class at this mirror")
                .to_string();
            let dotted = internal.replace('/', ".");
            Some(Value::Reference(strings::intern(metaspace, heap, &dotted)))
        }
        ("java/lang/Class", "getSimpleName", "()Ljava/lang/String;") => {
            // The source-level simple name: the segment after the last '/' (package) and last
            // '$' (nesting). Arrays report the component's simple name plus "[]" per dimension
            // ("[I" → "int[]", "[Ljava/lang/String;" → "String[]"); a primitive mirror's
            // internal name is already its simple name ("int").
            let internal = metaspace
                .class_name_at_mirror(reference(&args[0]))
                .expect("Class.getSimpleName: no class at this mirror")
                .to_string();
            let dims = internal.bytes().take_while(|&b| b == b'[').count();
            let element = &internal[dims..];
            let element =
                element.strip_prefix('L').and_then(|e| e.strip_suffix(';')).unwrap_or(element);
            let base = match (dims, element) {
                (1.., "I") => "int",
                (1.., "J") => "long",
                (1.., "D") => "double",
                (1.., "F") => "float",
                (1.., "S") => "short",
                (1.., "B") => "byte",
                (1.., "C") => "char",
                (1.., "Z") => "boolean",
                _ => element.rsplit(|c| c == '/' || c == '$').next().unwrap_or(element),
            };
            let simple = format!("{base}{}", "[]".repeat(dims));
            Some(Value::Reference(strings::intern(metaspace, heap, &simple)))
        }
        // --- Class: la capa de METADATOS -----------------------------------------
        //
        // Todo lo que sigue sale del archivo de clase y de nada mas: flags, superclase,
        // interfaces, tipo componente. Es la mitad de `java.lang.Class` que no necesita el
        // modelo de objetos de `java.lang.reflect`.
        //
        // Los que el JDK declara `native` -- `isInstance`, `getSuperclass`, `isAssignableFrom`,
        // `isHidden` -- se llaman igual, para que la superficie publica coincida hasta el
        // modificador. El resto son costuras **privadas** con sufijo `0`: la logica que se puede
        // escribir en Java se escribe en Java, que es la misma regla que dejo a `String` con
        // cuatro costuras privadas y toda su API en el lenguaje.

        ("java/lang/Class", "name0", "()Ljava/lang/String;") => {
            // El nombre INTERNO, crudo, con barras: `java/lang/String`, `[I`, `int`. El lado
            // Java le pone los puntos para `getName` y lo corta por la barra para el paquete,
            // asi que la costura entrega la forma sin decidir nada.
            let internal = mirror_name(metaspace, reference(&args[0]));
            Some(Value::Reference(strings::intern(metaspace, heap, &internal)))
        }

        ("java/lang/Class", "getSuperclass", "()Ljava/lang/Class;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            // Un array hereda de Object; un primitivo no hereda de nada, y una interfaz tampoco
            // -- `Runnable.class.getSuperclass()` es null, no Object.
            let parent = if name.starts_with('[') {
                Some("java/lang/Object".to_string())
            } else if is_primitive_name(&name) {
                None
            } else {
                metaspace.get_or_load(&name).and_then(|cf| {
                    if cf.is_interface() || cf.super_class == 0 {
                        None
                    } else {
                        cf.class_name(cf.super_class).map(str::to_string)
                    }
                })
            };
            let mirror = match parent {
                Some(p) => mirror_for(metaspace, heap, &p),
                None => 0,
            };
            Some(Value::Reference(mirror))
        }

        ("java/lang/Class", "isAssignableFrom", "(Ljava/lang/Class;)Z") => {
            let target = mirror_name(metaspace, reference(&args[0]));
            let candidate = mirror_name(metaspace, reference(&args[1]));
            // La identidad primero, y es la unica respuesta posible entre primitivos: `int` no
            // participa de ninguna jerarquia, asi que solo es asignable a si mismo.
            let assignable = candidate == target
                || (!is_primitive_name(&target)
                    && !is_primitive_name(&candidate)
                    && class_operations::is_subtype(metaspace, &candidate, &target));
            Some(Value::Int(assignable as i32))
        }

        // No fabricamos clases ocultas (las de `Lookup.defineHiddenClass`), asi que ninguna lo es.
        ("java/lang/Class", "isHidden", "()Z") => Some(Value::Int(0)),

        ("java/lang/Class", "modifiers0", "()I") => {
            const PUBLIC: u16 = 0x0001;
            const FINAL: u16 = 0x0010;
            const SUPER: u16 = 0x0020;
            const ABSTRACT: u16 = 0x0400;
            let name = mirror_name(metaspace, reference(&args[0]));
            let flags = if is_primitive_name(&name) {
                PUBLIC | FINAL | ABSTRACT
            } else if name.starts_with('[') {
                // Un array toma la VISIBILIDAD de su componente y es siempre final y abstracto:
                // `String[]` es publico porque `String` lo es, y un componente package-private
                // hace package-private al array.
                let component = component_name(&name);
                let visibility = match component {
                    Some(ref c) if !is_primitive_name(c) && !c.starts_with('[') => {
                        metaspace.get_or_load(c).map(|cf| cf.access_flags & 0x0007).unwrap_or(PUBLIC)
                    }
                    _ => PUBLIC,
                };
                visibility | FINAL | ABSTRACT
            } else {
                // ACC_SUPER se saca: no es un modificador del lenguaje (la especificacion dice
                // que se ignore) y comparte bit con `synchronized`, asi que dejarlo haria que
                // `Modifier.toString` imprimiera una clase "synchronized".
                metaspace.get_or_load(&name).map(|cf| cf.access_flags).unwrap_or(0) & !SUPER
            };
            Some(Value::Int(flags as i32))
        }

        ("java/lang/Class", "isPrimitive0", "()Z") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            Some(Value::Int(is_primitive_name(&name) as i32))
        }

        ("java/lang/Class", "interfaces0", "()[Ljava/lang/Class;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            // Todo array implementa Cloneable y Serializable (JLS §10.7), y en ese orden; un
            // primitivo no implementa nada.
            let names: Vec<String> = if name.starts_with('[') {
                vec!["java/lang/Cloneable".to_string(), "java/io/Serializable".to_string()]
            } else if is_primitive_name(&name) {
                Vec::new()
            } else {
                metaspace
                    .get_or_load(&name)
                    .map(|cf| {
                        cf.interfaces
                            .iter()
                            .filter_map(|&i| cf.class_name(i).map(str::to_string))
                            .collect()
                    })
                    .unwrap_or_default()
            };
            let mut mirrors = Vec::with_capacity(names.len());
            for n in &names {
                mirrors.push(mirror_for(metaspace, heap, n));
            }
            Some(Value::Reference(reference_array(
                metaspace,
                heap,
                "[Ljava/lang/Class;",
                &mirrors,
            )))
        }

        ("java/lang/Class", "componentType0", "()Ljava/lang/Class;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            let mirror = match component_name(&name) {
                Some(component) => mirror_for(metaspace, heap, &component),
                None => 0, // no es un array
            };
            Some(Value::Reference(mirror))
        }

        ("java/lang/Class", "arrayType0", "()Ljava/lang/Class;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            let array_class = format!("[{}", descriptor_of(&name));
            let mirror = mirror_for(metaspace, heap, &array_class);
            Some(Value::Reference(mirror))
        }

        ("java/lang/Class", "forName0", "(Ljava/lang/String;Z)Ljava/lang/Class;") => {
            // El nombre llega en forma binaria con puntos ("java.lang.String") o ya como
            // descriptor de array ("[I", "[Ljava.lang.String;"). Devolver 0 y no panicar es
            // deliberado: el que no exista es una respuesta normal, y el lado Java la convierte
            // en ClassNotFoundException.
            //
            // La bandera es la mitad que faltaba, y no es un detalle: cargar una clase **no** es
            // inicializarla (JVMS §5.4 vs §5.5), y `forName(String)` promete las dos cosas. Sin eso
            // toda clase que se registra sola desde un bloque `static` —un driver de JDBC, un
            // proveedor de `spi`— se cargaba y no hacía nada, sin un solo error que mirar. Ver el
            // hallazgo #487.
            //
            // Un array no tiene `<clinit>` ni estáticos, así que la bandera no lo toca.
            let dotted = strings::read(heap, reference(&args[0]));
            let inicializar = matches!(args[1], Value::Int(1));
            let internal = dotted.replace('.', "/");
            if internal.starts_with('[') {
                return NativeOutcome::Ran(Some(Value::Reference(mirror_for(metaspace, heap, &internal))));
            }
            if metaspace.get_or_load(&internal).is_none() {
                return NativeOutcome::Ran(Some(Value::Reference(0)));
            }
            if inicializar {
                // El mirror lo toma el sitio de despacho después de correr el `<clinit>`; ver la
                // nota de `RanEInicializa`.
                return NativeOutcome::RanEInicializa(internal);
            }
            Some(Value::Reference(mirror_for(metaspace, heap, &internal)))
        }

        // --- Class: los ATRIBUTOS del archivo de clase ---------------------------
        //
        // Todo lo que sigue sale de atributos que `ClassFile` guarda en crudo y nadie leia:
        // `InnerClasses`, `EnclosingMethod`, `NestHost`, `NestMembers`, `PermittedSubclasses` y
        // `Record`. Son la unica fuente de la estructura que el LENGUAJE tiene y el archivo de
        // clase pierde: un `.class` no sabe que estaba adentro de otro -- los dos son archivos
        // sueltos con un `$` en el nombre --, y estos atributos son lo que reconstruye eso.

        // --- ClassLoader: definir y encontrar --------------------------------------

        ("java/lang/ClassLoader", "defineClass0",
            "(Ljava/lang/String;[BII)Ljava/lang/Class;") => {
            // Un `.class` que llega como bytes en vez de como archivo. Es la unica forma de
            // meter una clase en la VM sin pasar por el classpath, y es de lo que viven los
            // proxies dinamicos y los generadores de bytecode.
            // Estatica: los argumentos arrancan en 0, sin receptor delante.
            let bytes: Vec<u8> = {
                let array = reference(&args[1]);
                if array == 0 {
                    return NativeOutcome::Ran(Some(Value::Reference(0)));
                }
                let offset = int(&args[2]) as usize;
                let length = int(&args[3]) as usize;
                (0..length)
                    .map(|i| {
                        heap.read_u8(array + array_operations::ARRAY_HEADER_SIZE + offset + i)
                    })
                    .collect()
            };
            let Ok(class) = ClassFile::from_bytes(&bytes) else {
                return NativeOutcome::Ran(Some(Value::Reference(0)));
            };
            let Some(internal) = class.class_name(class.this_class).map(str::to_string) else {
                return NativeOutcome::Ran(Some(Value::Reference(0)));
            };
            // El nombre que el llamador dijo tiene que coincidir con el que el archivo dice; si
            // no, es un `NoClassDefFoundError` en el JDK y acá un cero que el lado Java traduce.
            let asked = reference(&args[0]);
            if asked != 0 {
                let dotted = strings::read(heap, asked);
                if dotted.replace('.', "/") != internal {
                    return NativeOutcome::Ran(Some(Value::Reference(0)));
                }
            }
            metaspace.add(internal.clone(), class);
            class_operations::load_class(metaspace, heap, &internal);
            Some(Value::Reference(mirror_for(metaspace, heap, &internal)))
        }

        ("java/lang/Class", "nestHost0", "()Ljava/lang/Class;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            let host = attribute_class(metaspace, &name, "NestHost", 0);
            let mirror = match host {
                Some(h) => mirror_for(metaspace, heap, &h),
                // Sin atributo, una clase es su propio nido. No es un valor por defecto
                // arbitrario: una clase de nivel superior sin anidados ES un nido de uno.
                None => reference(&args[0]),
            };
            Some(Value::Reference(mirror))
        }

        ("java/lang/Class", "nestMembers0", "()[Ljava/lang/Class;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            let members = attribute_class_list(metaspace, &name, "NestMembers");
            let mut mirrors = vec![reference(&args[0])];
            for m in &members {
                if *m != name {
                    mirrors.push(mirror_for(metaspace, heap, m));
                }
            }
            Some(Value::Reference(reference_array(metaspace, heap, "[Ljava/lang/Class;", &mirrors)))
        }

        ("java/lang/Class", "permittedSubclasses0", "()[Ljava/lang/Class;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            let permitted = attribute_class_list(metaspace, &name, "PermittedSubclasses");
            if !has_attribute(metaspace, &name, "PermittedSubclasses") {
                // Null y no un array vacio: "no es sellada" y "es sellada y no permite a nadie"
                // son cosas distintas, y `isSealed` se apoya en esa diferencia.
                return NativeOutcome::Ran(Some(Value::Reference(0)));
            }
            let mut mirrors = Vec::with_capacity(permitted.len());
            for p in &permitted {
                mirrors.push(mirror_for(metaspace, heap, p));
            }
            Some(Value::Reference(reference_array(metaspace, heap, "[Ljava/lang/Class;", &mirrors)))
        }

        ("java/lang/Class", "declaringClass0", "()Ljava/lang/Class;") => {
            // La clase que DECLARA a esta, o 0. Una local o anonima no tiene: su entrada de
            // `InnerClasses` deja el `outer_class_info` en cero, que es exactamente como el
            // archivo de clase distingue "anidada" de "declarada adentro de un metodo".
            let name = mirror_name(metaspace, reference(&args[0]));
            let outer = inner_class_entry(metaspace, &name).and_then(|e| e.1);
            let mirror = match outer {
                Some(o) => mirror_for(metaspace, heap, &o),
                None => 0,
            };
            Some(Value::Reference(mirror))
        }

        ("java/lang/Class", "enclosingClass0", "()Ljava/lang/Class;") => {
            // La que la ENCIERRA, que para una local o anonima es la del `EnclosingMethod` y
            // para una anidada es la que la declara.
            let name = mirror_name(metaspace, reference(&args[0]));
            let enclosing = attribute_class(metaspace, &name, "EnclosingMethod", 0)
                .or_else(|| inner_class_entry(metaspace, &name).and_then(|e| e.1));
            let mirror = match enclosing {
                Some(e) => mirror_for(metaspace, heap, &e),
                None => 0,
            };
            Some(Value::Reference(mirror))
        }

        ("java/lang/Class", "innerName0", "()Ljava/lang/String;") => {
            // El nombre simple que el FUENTE le dio, o 0 si es anonima. Es la unica forma exacta
            // de contestar `getSimpleName`, `isAnonymousClass` y `isMemberClass`: derivarlo del
            // `$` del nombre binario acierta casi siempre y falla con una clase de nivel superior
            // que de verdad se llame `A$B`.
            let name = mirror_name(metaspace, reference(&args[0]));
            match inner_class_entry(metaspace, &name).and_then(|e| e.2) {
                Some(simple) => {
                    Some(Value::Reference(strings::intern(metaspace, heap, &simple)))
                }
                None => Some(Value::Reference(0)),
            }
        }

        ("java/lang/Class", "isInnerClass0", "()Z") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            Some(Value::Int(inner_class_entry(metaspace, &name).is_some() as i32))
        }

        ("java/lang/Class", "hasEnclosingMethod0", "()Z") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            let present = attribute_body(metaspace, &name, "EnclosingMethod")
                .map(|b| b.len() >= 4 && (b[2] != 0 || b[3] != 0))
                .unwrap_or(false);
            Some(Value::Int(present as i32))
        }

        ("java/lang/Class", "enclosingMethodInfo0", "()[Ljava/lang/String;") => {
            // `{clase, nombre, descriptor}` del metodo que encierra a una clase local o anonima,
            // o 0. El atributo `EnclosingMethod` guarda la clase siempre y el metodo solo cuando
            // la clase nacio adentro de uno -- una anonima declarada en un inicializador de campo
            // tiene clase y no tiene metodo, y ese cero es informacion, no un hueco.
            let name = mirror_name(metaspace, reference(&args[0]));
            let indices = {
                let Some(body) = attribute_body(metaspace, &name, "EnclosingMethod") else {
                    return NativeOutcome::Ran(Some(Value::Reference(0)));
                };
                if body.len() < 4 {
                    return NativeOutcome::Ran(Some(Value::Reference(0)));
                }
                (
                    u16::from_be_bytes([body[0], body[1]]),
                    u16::from_be_bytes([body[2], body[3]]),
                )
            };
            if indices.1 == 0 {
                return NativeOutcome::Ran(Some(Value::Reference(0)));
            }
            let (owner, method, descriptor) = {
                let Some(class) = metaspace.get(&name) else {
                    return NativeOutcome::Ran(Some(Value::Reference(0)));
                };
                let Some(owner) = class.class_name(indices.0).map(str::to_string) else {
                    return NativeOutcome::Ran(Some(Value::Reference(0)));
                };
                let Some((m, d)) = class.name_and_type(indices.1) else {
                    return NativeOutcome::Ran(Some(Value::Reference(0)));
                };
                (owner, m.to_string(), d.to_string())
            };
            let parts = vec![
                strings::intern(metaspace, heap, &owner),
                strings::intern(metaspace, heap, &method),
                strings::intern(metaspace, heap, &descriptor),
            ];
            Some(Value::Reference(reference_array(
                metaspace,
                heap,
                "[Ljava/lang/String;",
                &parts,
            )))
        }

        ("java/lang/Class", "declaredClasses0", "()[Ljava/lang/Class;") => {
            // Las que ESTA declara: las entradas de su propio `InnerClasses` cuyo
            // `outer_class_info` es ella. La tabla trae tambien las clases anidadas que la clase
            // solo MENCIONA -- por eso hay que filtrar por el outer y no tomarla entera.
            let name = mirror_name(metaspace, reference(&args[0]));
            let declared = declared_inner_classes(metaspace, &name);
            let mut mirrors = Vec::with_capacity(declared.len());
            for d in &declared {
                mirrors.push(mirror_for(metaspace, heap, d));
            }
            Some(Value::Reference(reference_array(metaspace, heap, "[Ljava/lang/Class;", &mirrors)))
        }

        ("java/lang/Class", "recordComponents0",
            "()[Ljava/lang/reflect/RecordComponent;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            let Some(components) = record_components(metaspace, &name) else {
                return NativeOutcome::Ran(Some(Value::Reference(0))); // no es un record
            };
            class_operations::load_class(metaspace, heap, "java/lang/reflect/RecordComponent");
            let empty: Vec<usize> = vec![0; components.len()];
            let array =
                reference_array(metaspace, heap, "[Ljava/lang/reflect/RecordComponent;", &empty);
            let owner = reference(&args[0]);
            for (slot, (component, descriptor)) in components.into_iter().enumerate() {
                let Some(object) = objects_operations::try_allocate(
                    metaspace,
                    heap,
                    "java/lang/reflect/RecordComponent",
                ) else {
                    break;
                };
                let at = array_operations::ARRAY_HEADER_SIZE + slot * SLOT_SIZE;
                heap.store_reference(array, array + at, object);
                let interned = strings::intern(metaspace, heap, &component);
                let type_mirror = mirror_for(metaspace, heap, &internal_name_of(&descriptor));
                const RC: &str = "java/lang/reflect/RecordComponent";
                let clazz_at = field_offset(metaspace, RC, "clazz");
                let name_at = field_offset(metaspace, RC, "name");
                let type_at = field_offset(metaspace, RC, "type");
                heap.store_reference(object, object + clazz_at, owner);
                heap.store_reference(object, object + name_at, interned);
                heap.store_reference(object, object + type_at, type_mirror);
            }
            Some(Value::Reference(array))
        }

        ("java/lang/Class", "declaredConstructors0",
            "()[Ljava/lang/reflect/Constructor;") => {
            // Un constructor es un metodo llamado `<init>` y nada mas: la reflexion lo presenta
            // como otra cosa porque se invoca de otra forma -- aloca antes de correr --, pero en
            // el archivo de clase vive en la misma tabla que los demas.
            let name = mirror_name(metaspace, reference(&args[0]));
            let declared: Vec<(String, u16, Vec<String>)> =
                if name.starts_with('[') || is_primitive_name(&name) {
                    Vec::new()
                } else {
                    metaspace
                        .get_or_load(&name)
                        .map(|cf| {
                            cf.methods
                                .iter()
                                .filter_map(|m| {
                                    if cf.utf8(m.name_index)? != "<init>" {
                                        return None;
                                    }
                                    let d = cf.utf8(m.descriptor_index)?.to_string();
                                    Some((d, m.access_flags, declared_exceptions(cf, m)))
                                })
                                .collect()
                        })
                        .unwrap_or_default()
                };
            class_operations::load_class(metaspace, heap, "java/lang/reflect/Constructor");
            let empty: Vec<usize> = vec![0; declared.len()];
            let array =
                reference_array(metaspace, heap, "[Ljava/lang/reflect/Constructor;", &empty);
            let owner = reference(&args[0]);
            for (slot, (descriptor, flags, throws)) in declared.into_iter().enumerate() {
                let Some(object) = objects_operations::try_allocate(
                    metaspace,
                    heap,
                    "java/lang/reflect/Constructor",
                ) else {
                    break;
                };
                let at = array_operations::ARRAY_HEADER_SIZE + slot * SLOT_SIZE;
                heap.store_reference(array, array + at, object);

                let (parameters, _) = split_descriptor(&descriptor);
                let mut parameter_mirrors = Vec::with_capacity(parameters.len());
                for p in &parameters {
                    parameter_mirrors.push(mirror_for(metaspace, heap, p));
                }
                let parameter_array =
                    reference_array(metaspace, heap, "[Ljava/lang/Class;", &parameter_mirrors);
                let mut throws_mirrors = Vec::with_capacity(throws.len());
                for t in &throws {
                    throws_mirrors.push(mirror_for(metaspace, heap, t));
                }
                let throws_array =
                    reference_array(metaspace, heap, "[Ljava/lang/Class;", &throws_mirrors);

                const CTOR: &str = "java/lang/reflect/Constructor";
                let clazz_at = field_offset(metaspace, CTOR, "clazz");
                let params_at = field_offset(metaspace, CTOR, "parameterTypes");
                let throws_at = field_offset(metaspace, CTOR, "exceptionTypes");
                let mods_at = field_offset(metaspace, CTOR, "modifiers");
                let slot_at = field_offset(metaspace, CTOR, "slot");
                heap.store_reference(object, object + clazz_at, owner);
                heap.store_reference(object, object + params_at, parameter_array);
                heap.store_reference(object, object + throws_at, throws_array);
                heap.write_u32(object + mods_at, flags as u32);
                heap.write_u32(object + slot_at, slot as u32);
            }
            Some(Value::Reference(array))
        }

        ("java/lang/Class", "declaredMethods0", "()[Ljava/lang/reflect/Method;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            // `<init>` y `<clinit>` no son metodos para la reflexion: el primero sale por
            // `getDeclaredConstructors` y el segundo no sale por ningun lado, porque nadie
            // puede llamarlo.
            let declared: Vec<(String, String, u16, Vec<String>)> =
                if name.starts_with('[') || is_primitive_name(&name) {
                    Vec::new()
                } else {
                    metaspace
                        .get_or_load(&name)
                        .map(|cf| {
                            cf.methods
                                .iter()
                                .filter_map(|m| {
                                    let n = cf.utf8(m.name_index)?.to_string();
                                    if n == "<init>" || n == "<clinit>" {
                                        return None;
                                    }
                                    let d = cf.utf8(m.descriptor_index)?.to_string();
                                    Some((n, d, m.access_flags, declared_exceptions(cf, m)))
                                })
                                .collect()
                        })
                        .unwrap_or_default()
                };
            class_operations::load_class(metaspace, heap, "java/lang/reflect/Method");
            // El array primero y en Old, cada `Method` adentro apenas se crea: ver
            // `declaredFields0` para por que el orden inverso los dejaria sin raiz.
            let empty: Vec<usize> = vec![0; declared.len()];
            let array = reference_array(metaspace, heap, "[Ljava/lang/reflect/Method;", &empty);
            let owner = reference(&args[0]);
            for (slot, (method_name, descriptor, flags, throws)) in
                declared.into_iter().enumerate()
            {
                let Some(object) =
                    objects_operations::try_allocate(metaspace, heap, "java/lang/reflect/Method")
                else {
                    break;
                };
                let at = array_operations::ARRAY_HEADER_SIZE + slot * SLOT_SIZE;
                heap.store_reference(array, array + at, object);

                let (parameters, returns) = split_descriptor(&descriptor);
                let interned = strings::intern(metaspace, heap, &method_name);
                let return_mirror = mirror_for(metaspace, heap, &returns);
                let mut parameter_mirrors = Vec::with_capacity(parameters.len());
                for p in &parameters {
                    parameter_mirrors.push(mirror_for(metaspace, heap, p));
                }
                let parameter_array =
                    reference_array(metaspace, heap, "[Ljava/lang/Class;", &parameter_mirrors);
                let mut throws_mirrors = Vec::with_capacity(throws.len());
                for t in &throws {
                    throws_mirrors.push(mirror_for(metaspace, heap, t));
                }
                let throws_array =
                    reference_array(metaspace, heap, "[Ljava/lang/Class;", &throws_mirrors);

                let clazz_at = field_offset(metaspace, "java/lang/reflect/Method", "clazz");
                let name_at = field_offset(metaspace, "java/lang/reflect/Method", "name");
                let ret_at = field_offset(metaspace, "java/lang/reflect/Method", "returnType");
                let params_at =
                    field_offset(metaspace, "java/lang/reflect/Method", "parameterTypes");
                let throws_at =
                    field_offset(metaspace, "java/lang/reflect/Method", "exceptionTypes");
                let mods_at = field_offset(metaspace, "java/lang/reflect/Method", "modifiers");
                let slot_at = field_offset(metaspace, "java/lang/reflect/Method", "slot");
                heap.store_reference(object, object + clazz_at, owner);
                heap.store_reference(object, object + name_at, interned);
                heap.store_reference(object, object + ret_at, return_mirror);
                heap.store_reference(object, object + params_at, parameter_array);
                heap.store_reference(object, object + throws_at, throws_array);
                heap.write_u32(object + mods_at, flags as u32);
                heap.write_u32(object + slot_at, slot as u32);
            }
            Some(Value::Reference(array))
        }

        ("java/lang/Class", "declaredFields0", "()[Ljava/lang/reflect/Field;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            let declared: Vec<(String, String, u16)> = if name.starts_with('[')
                || is_primitive_name(&name)
            {
                Vec::new()
            } else {
                metaspace
                    .get_or_load(&name)
                    .map(|cf| {
                        cf.fields
                            .iter()
                            .filter_map(|f| {
                                let n = cf.utf8(f.name_index)?.to_string();
                                let d = cf.utf8(f.descriptor_index)?.to_string();
                                Some((n, d, f.access_flags))
                            })
                            .collect()
                    })
                    .unwrap_or_default()
            };
            class_operations::load_class(metaspace, heap, "java/lang/reflect/Field");
            // El array se aloca PRIMERO y en Old, y cada `Field` entra por el barrier apenas se
            // crea. Al reves -- juntar los `Field` en un `Vec` de Rust y armar el array despues --
            // los dejaria sin raiz: son objetos jovenes que solo un `Vec` de Rust conoce, y el
            // primer minor GC de la vuelta siguiente los moveria debajo de nuestros pies.
            let empty: Vec<usize> = vec![0; declared.len()];
            let array =
                reference_array(metaspace, heap, "[Ljava/lang/reflect/Field;", &empty);
            let owner = reference(&args[0]);
            for (slot, (field_name, descriptor, flags)) in declared.into_iter().enumerate() {
                let Some(object) =
                    objects_operations::try_allocate(metaspace, heap, "java/lang/reflect/Field")
                else {
                    break;
                };
                let at = array_operations::ARRAY_HEADER_SIZE + slot * SLOT_SIZE;
                heap.store_reference(array, array + at, object);
                let interned = strings::intern(metaspace, heap, &field_name);
                let type_mirror = mirror_for(metaspace, heap, &internal_name_of(&descriptor));
                let clazz_at = field_offset(metaspace, "java/lang/reflect/Field", "clazz");
                let name_at = field_offset(metaspace, "java/lang/reflect/Field", "name");
                let type_at = field_offset(metaspace, "java/lang/reflect/Field", "type");
                let mods_at = field_offset(metaspace, "java/lang/reflect/Field", "modifiers");
                let slot_at = field_offset(metaspace, "java/lang/reflect/Field", "slot");
                heap.store_reference(object, object + clazz_at, owner);
                heap.store_reference(object, object + name_at, interned);
                heap.store_reference(object, object + type_at, type_mirror);
                heap.write_u32(object + mods_at, flags as u32);
                heap.write_u32(object + slot_at, slot as u32);
            }
            Some(Value::Reference(array))
        }

        // --- reflective field access (java.lang.reflect.Field) -------------------------------
        // The typed raw seams the Field's Java layer calls after its own type-check/widening/boxing.
        // Each locates the field's slot from the Field object (its clazz/name/modifiers) and the
        // target: a static field lives on the owner's Class mirror, an instance field at the target's
        // offset. `getInt0` covers every 4-byte field (boolean/byte/short/char/int, and float via its
        // bit pattern); `getLong0` the 8-byte ones; `getReference0` the object fields. The width the
        // native reads is fixed per method — the Java side already picked the right one from the type.
        ("java/lang/reflect/Field", "getInt0", "(Ljava/lang/Object;)I") => {
            let (at, _) = field_slot_addr(metaspace, heap, reference(&args[0]), reference(&args[1]));
            Some(Value::Int(heap.read_u32(at) as i32))
        }
        ("java/lang/reflect/Field", "getLong0", "(Ljava/lang/Object;)J") => {
            let (at, _) = field_slot_addr(metaspace, heap, reference(&args[0]), reference(&args[1]));
            Some(Value::Long(heap.read_u64(at) as i64))
        }
        ("java/lang/reflect/Field", "getReference0", "(Ljava/lang/Object;)Ljava/lang/Object;") => {
            let (at, _) = field_slot_addr(metaspace, heap, reference(&args[0]), reference(&args[1]));
            Some(Value::Reference(heap.read_u32(at) as usize))
        }
        ("java/lang/reflect/Field", "setInt0", "(Ljava/lang/Object;I)V") => {
            let (at, _) = field_slot_addr(metaspace, heap, reference(&args[0]), reference(&args[1]));
            heap.write_u32(at, int(&args[2]) as u32);
            None
        }
        ("java/lang/reflect/Field", "setLong0", "(Ljava/lang/Object;J)V") => {
            let (at, _) = field_slot_addr(metaspace, heap, reference(&args[0]), reference(&args[1]));
            heap.write_u64(at, long(&args[2]) as u64);
            None
        }
        ("java/lang/reflect/Field", "setReference0", "(Ljava/lang/Object;Ljava/lang/Object;)V") => {
            let (at, holder) =
                field_slot_addr(metaspace, heap, reference(&args[0]), reference(&args[1]));
            // A reference store goes through the write barrier so the GC learns of the new edge.
            heap.store_reference(holder, at, reference(&args[2]));
            None
        }

        ("java/lang/Class", "getPrimitiveClass", "(Ljava/lang/String;)Ljava/lang/Class;") => {
            // The primitive type's `Class` mirror — `int.class` compiles to `getstatic
            // Integer.TYPE`, whose `<clinit>` calls this. A header-only mirror (like an array
            // class's), keyed and cached by the type name so `int.class == int.class`. Old-pinned:
            // a primitive mirror is permanent, like any `Class`.
            let type_name = strings::read(heap, reference(&args[0]));
            let uuid = metaspace.class_id(&type_name).to_string();
            let mirror = match metaspace.class_object(&uuid) {
                Some(offset) => offset,
                None => {
                    let offset = heap.malloc_old(HEADER_SIZE);
                    metaspace.set_class_object(&uuid, offset);
                    // The mirror is itself an instance of `java.lang.Class`, so its header's
                    // `class_id` points at `Class`'s mirror — that's what makes `invokevirtual`
                    // on it (`descriptorString`, `getClass`, …) dispatch correctly.
                    class_operations::load_class(metaspace, heap, "java/lang/Class");
                    let class_uuid = metaspace.class_id("java/lang/Class").to_string();
                    let class_mirror = metaspace.class_object(&class_uuid).unwrap_or(0);
                    heap.write_u32(offset, class_mirror as u32);
                    offset
                }
            };
            Some(Value::Reference(mirror))
        }

        // --- APT fase 3: el modelo de elementos reificado (ver `super::apt`) -----
        // El receptor es un `jdk/internal/apt/SymElement`: su campo `int sym` es el `SymbolId`
        // en la tabla de `javac` (viva en este mismo proceso, atada con `JVM::set_apt`). Leemos
        // ese id, indexamos la tabla y respondemos contra la tabla del compilador. Estos tres son
        // native (no intrínsecos): no corren `<clinit>` ni re-entran al intérprete —construyen un
        // objeto a mano, el mismo patrón que `materialize_method_type`—. Los que sí lo necesitan
        // (`getKind`/`getEnclosedElements`) los intercepta el intérprete antes de llegar acá.
        //
        // `getSimpleName`: el `Symbol.name` (`"Foo"`), envuelto en un `Name` (`SymName`) —el
        // contrato de `Element` devuelve `Name`, no `String`—.
        ("jdk/internal/apt/SymElement", "getSimpleName", "()Ljavax/lang/model/element/Name;") => {
            let sym = sym_of(&args[0], metaspace, heap);
            let text = apt_ref(apt).table().symbol(sym).name.clone();
            Some(Value::Reference(make_name(metaspace, heap, &text)))
        }
        // `getQualifiedName`: el *binary name* de un tipo con el `$` del anidamiento vuelto `.`
        // (`"a.b.Outer$Inner"` → `"a.b.Outer.Inner"`), también como un `Name`. Para un símbolo que
        // no es una clase (no debería pasar: sólo `TypeElement` lo declara) cae al nombre simple.
        ("jdk/internal/apt/SymElement", "getQualifiedName", "()Ljavax/lang/model/element/Name;") => {
            let sym = sym_of(&args[0], metaspace, heap);
            let symbol = apt_ref(apt).table().symbol(sym);
            let text = match &symbol.kind {
                crate::javac::symbol::SymbolKind::Class { binary, .. } => binary.replace('$', "."),
                _ => symbol.name.clone(),
            };
            Some(Value::Reference(make_name(metaspace, heap, &text)))
        }
        // `getEnclosingElement`: el `Symbol.owner` reificado —la clase de un miembro, el paquete de
        // un tipo top-level— vía `element_for`, así que hereda su **caché de identidad**: dos hijos
        // del mismo dueño devuelven el **mismo** objeto (`a.getEnclosingElement() ==
        // b.getEnclosingElement()`). `null` (referencia 0) si el símbolo no tiene dueño.
        ("jdk/internal/apt/SymElement", "getEnclosingElement", "()Ljavax/lang/model/element/Element;") => {
            let sym = sym_of(&args[0], metaspace, heap);
            let owner = apt_ref(apt).table().symbol(sym).owner;
            let element = match owner {
                Some(owner) => apt.as_mut().expect("AptContext atado").element_for(owner, metaspace, heap),
                None => 0,
            };
            Some(Value::Reference(element))
        }

        // El reloj de pared, en milisegundos desde la epoca. Declarado hace rato del lado
        // Java y sin implementar de este; lo destapo `java.util.Random`, cuyo constructor sin
        // argumentos se siembra de aca.
        // --- entropia del sistema operativo -------------------------------------
        //
        // The one thing a CSPRNG cannot compute: the seed. Everything above this line is
        // deterministic, so `SecureRandom` has to reach the OS, and this is the seam.
        //
        // Per platform, the *system* generator — not a library's own:
        //   - Windows: `BCryptGenRandom` with the system-preferred algorithm. `bcrypt` is the
        //     documented replacement for the deprecated `RtlGenRandom`, and it is what the JDK
        //     and the Rust standard library both call.
        //   - everything else: `/dev/urandom`, which never blocks once the pool is seeded and is
        //     the source the kernel itself recommends for everything after early boot.
        //
        // Returns whether it filled the buffer. **A partial read is a failure**, not a short
        // result: the caller cannot tell which bytes are real, and half a seed that looks like a
        // whole one is the worst outcome available here.
        ("java/security/OsEntropy", "fill0", "([B)Z") => {
            let arr = reference(&args[0]);
            if arr == 0 {
                return NativeOutcome::Ran(Some(Value::Int(0)));
            }
            let len = heap.read_u32(arr + array_operations::LENGTH_OFFSET) as usize;
            let mut bytes = vec![0u8; len];
            let ok = os_entropy(&mut bytes);
            if ok {
                for i in 0..len {
                    heap.write_u8(arr + array_operations::ARRAY_HEADER_SIZE + i, bytes[i]);
                }
            }
            Some(Value::Int(i32::from(ok)))
        }

        ("java/lang/System", "currentTimeMillis", "()J") => {
            let since = std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .map(|d| d.as_millis() as i64)
                .unwrap_or(0);
            Some(Value::Long(since))
        }

        // --- las propiedades del sistema ---------------------------------------
        //
        // Que propiedades existen es cosa de la implementacion, no de la especificacion: el JDK
        // documenta un conjunto minimo y cada plataforma agrega lo suyo. Estas son las que esta
        // VM puede responder de verdad; para cualquier otra clave devuelve `null`, que es
        // exactamente lo que el contrato dice para una clave ausente.
        ("java/lang/System", "getProperty0", "(Ljava/lang/String;)Ljava/lang/String;") => {
            let key = strings::read(heap, reference(&args[0]));
            let value = match key.as_str() {
                "java.version" | "java.specification.version" => Some("25"),
                "java.vm.specification.version" => Some("25"),
                "java.vm.name" | "java.vendor" => Some("KajiJDK"),
                "line.separator" => Some(if cfg!(windows) { "\r\n" } else { "\n" }),
                "file.separator" => Some(if cfg!(windows) { "\\" } else { "/" }),
                "path.separator" => Some(if cfg!(windows) { ";" } else { ":" }),
                "os.name" => Some(std::env::consts::OS),
                "os.arch" => Some(std::env::consts::ARCH),
                "native.encoding" | "file.encoding" => Some("UTF-8"),
                _ => None,
            };
            // `java.io.tmpdir` y `user.dir` salen del entorno, así que no entran en el `match` de
            // arriba —que devuelve `&'static str`— y se resuelven aparte.
            //
            // Sin `java.io.tmpdir` **no hay dónde crear un temporal**, y eso deja inservibles a
            // `Files.createTempFile`/`createTempDirectory` y a todo lo que se apoya en ellas: la
            // caída a `"."` que hacen no sirve, porque `user.dir` también faltaba y una ruta
            // relativa no se podía llevar a absoluta. Eran dos ausencias que se tapaban entre sí.
            let del_entorno: Option<String> = match key.as_str() {
                "java.io.tmpdir" => Some(std::env::temp_dir().to_string_lossy().into_owned()),
                "user.dir" => std::env::current_dir()
                    .ok()
                    .map(|p| p.to_string_lossy().into_owned()),
                _ => None,
            };
            if let Some(text) = del_entorno {
                return NativeOutcome::Ran(Some(Value::Reference(strings::intern(
                    metaspace, heap, &text,
                ))));
            }
            match value {
                Some(text) => Some(Value::Reference(strings::intern(metaspace, heap, text))),
                None => Some(Value::Reference(0)),
            }
        }

        // --- las costuras de bits de Double/Float -------------------------------
        //
        // Reinterpretar los bits de un flotante es lo unico que el bytecode NO puede hacer: no
        // hay opcode que lea un `double` como un `long` sin convertir el VALOR. De ahi que sean
        // nativas — y que lo sean en el JDK tambien, con estos mismos nombres.
        //
        // `raw` significa que un NaN se devuelve tal cual, con su carga util; la canonicalizacion
        // a 0x7ff8000000000000 la hace `doubleToLongBits`, que es Java y esta en la biblioteca.
        ("java/lang/Double", "doubleToRawLongBits" | "doubleToLongBits", "(D)J") => {
            Some(Value::Long(double(&args[0]).to_bits() as i64))
        }
        ("java/lang/Double", "longBitsToDouble", "(J)D") => {
            Some(Value::Double(f64::from_bits(long(&args[0]) as u64)))
        }
        ("java/lang/Float", "floatToRawIntBits" | "floatToIntBits", "(F)I") => {
            Some(Value::Int(float(&args[0]).to_bits() as i32))
        }
        ("java/lang/Float", "intBitsToFloat", "(I)F") => {
            Some(Value::Float(f32::from_bits(int(&args[0]) as u32)))
        }

        // --- String -------------------------------------------------------------
        // Dos nombres para la misma costura. `rawLength` es la de KajiLibrary, privada, con el
        // String como parametro explicito; `length` es la que declara la copia compilada de
        // `boot/`, publica y de instancia. El cuerpo es el mismo porque el receptor de una y el
        // primer argumento de la otra ocupan la MISMA posicion, `args[0]`. Se aceptan las dos
        // mientras los dos arboles convivan (ver COMPILER_FINDINGS sobre boot/ vs KajiLibrary).
        ("java/lang/String", "rawLength" | "length", "(Ljava/lang/String;)I" | "()I") => {
            // The receiver is a heap String; its length word holds the UTF-8 byte count.
            Some(Value::Int(strings::length(heap, reference(&args[0])) as i32))
        }
        // charAt(int): the i-th byte (ASCII; our String is UTF-8, fine for ASCII).
        ("java/lang/String", "rawCharAt" | "charAt", "(Ljava/lang/String;I)C" | "(I)C") => {
            Some(Value::Int(strings::char_at(heap, reference(&args[0]), int(&args[1]) as usize) as i32))
        }
        // equals(Object): true if the other is a String with the same text.
        // valueOf(char[], offset, count): the seam KajiLibrary builds every String through —
        // `StringBuilder.toString`, `substring`, `Writer.write(String)`'s inverse, etc. A `char[]`
        // stores UTF-16 code units two bytes wide (after the 12-byte array header); we slice
        // `[offset, offset+count)`, decode them, and intern the text as a fresh heap String.
        // Los tres que KajiLibrary ya NO declara nativos -- los implementa en Java sobre
        // `charAt`, que ademas es lo que arregla el hash de las cadenas no-ASCII. Siguen aca
        // porque la copia de `boot/` los declara nativos y la VM los carga a ella en los tests.
        ("java/lang/String", "equals", "(Ljava/lang/Object;)Z") => {
            let other = reference(&args[1]);
            let equal = other != 0 && strings::read(heap, reference(&args[0])) == strings::read(heap, other);
            Some(Value::Int(equal as i32))
        }
        ("java/lang/String", "hashCode", "()I") => {
            let text = strings::read(heap, reference(&args[0]));
            let hash = text.chars().flat_map(|c| { let mut b = [0u16; 2]; c.encode_utf16(&mut b).to_vec() })
                .fold(0i32, |h, u| h.wrapping_mul(31).wrapping_add(u as i32));
            Some(Value::Int(hash))
        }
        ("java/lang/String", "startsWith", "(Ljava/lang/String;)Z") => {
            let text = strings::read(heap, reference(&args[0]));
            let prefix = strings::read(heap, reference(&args[1]));
            Some(Value::Int(text.starts_with(&prefix) as i32))
        }
        ("java/lang/String", "rawValueOf" | "valueOf", "([CII)Ljava/lang/String;") => {
            const ARRAY_HEADER: usize = 12; // object header (8) + length word (4)
            let array = reference(&args[0]);
            let start = int(&args[1]) as usize;
            let count = int(&args[2]) as usize;
            let units: Vec<u16> =
                (0..count).map(|i| heap.read_u16(array + ARRAY_HEADER + (start + i) * 2)).collect();
            // **Allocated, not pooled**: this is a String the program *computes* out of a
            // `char[]`, so it is a distinct object even when its contents equal a literal — which
            // is what makes `new String("a") == "a"` false, as JLS 3.10.5 requires.
            //
            // By UNITS, not through a Rust `String`. Going through one would be lossy in a way
            // Java can observe: `from_utf16_lossy` turns an unpaired surrogate into U+FFFD, and a
            // `char[]` is allowed to hold one. `new String(chars).charAt(0)` must answer 0xD800
            // when that is what was put in.
            Some(Value::Reference(strings::allocate_units(metaspace, heap, &units)))
        }

        // The CAS primitive (H5) — the atomic root of every lock-free counter. Compare the
        // `value` field to `expectedValue` (args[1]); if equal, set it to `newValue` (args[2]) and
        // return `true`. In `os` mode an `invokevirtual` escalates to the write path, so this
        // read-compare-write is exclusive (atomic) and correct; the retry loops in the Java
        // `AtomicInteger` build every other operation (`incrementAndGet`, …) on top of it.
        ("java/util/concurrent/atomic/AtomicInteger", "compareAndSet", "(II)Z") => {
            let at = reference(&args[0])
                + field_offset(metaspace, "java/util/concurrent/atomic/AtomicInteger", "value");
            let matched = heap.read_u32(at) as i32 == int(&args[1]);
            if matched {
                heap.write_u32(at, int(&args[2]) as u32);
            }
            Some(Value::Int(matched as i32))
        }
        ("java/util/concurrent/atomic/AtomicLong", "compareAndSet", "(JJ)Z") => {
            let at = reference(&args[0])
                + field_offset(metaspace, "java/util/concurrent/atomic/AtomicLong", "value");
            let matched = heap.read_u64(at) as i64 == long(&args[1]);
            if matched {
                heap.write_u64(at, long(&args[2]) as u64);
            }
            Some(Value::Int(matched as i32))
        }
        ("java/util/concurrent/atomic/AtomicReference", "compareAndSet", "(Ljava/lang/Object;Ljava/lang/Object;)Z") => {
            // Reference CAS: compares by **identity** (heap offset). On success the store goes
            // through the write barrier (an Old holder pointing at a young value must be remembered).
            let object = reference(&args[0]);
            let at = object + field_offset(metaspace, "java/util/concurrent/atomic/AtomicReference", "value");
            let matched = heap.read_u32(at) as usize == reference(&args[1]);
            if matched {
                heap.store_reference(object, at, reference(&args[2]));
            }
            Some(Value::Int(matched as i32))
        }

        // --- APT fase 4: registro de un archivo fuente del Filer ----------------
        // `KajiFiler.createSourceFile(name)` acaba acá: args[0] es el `KajiFiler` receptor, args[1]
        // el nombre (una `String` del heap) y args[2] el `StringWriter` recién creado que recibirá
        // el texto. Guardamos `(nombre, offset del writer)` en el canal lateral del hilo; el round
        // loop lo drena con `drain_filer` y recupera el texto con `read_generated_text`. Si no hay
        // Filer armado (`install_filer` no corrió), el registro se descarta en silencio.
        ("javax/annotation/processing/KajiFiler", "nativeRegisterSourceFile", "(Ljava/lang/String;Ljava/io/StringWriter;)V") => {
            let name = strings::read(heap, reference(&args[1]));
            let writer_ref = reference(&args[2]) as u32;
            FILER.with(|f| {
                if let Some(state) = f.borrow_mut().as_mut() {
                    state.pending.push((name, writer_ref));
                }
            });
            None
        }

        // `Array.newArray(Class, int)`: un array cuyo tipo de elemento solo se conoce en runtime.
        //
        // No hay opcode para esto — `anewarray` lleva la clase en el constant pool — y es lo que
        // necesita `Collection.toArray(T[])`: el llamador pasa un `String[0]` justamente para
        // recibir un `String[]`, y el tipo del array sale del mirror, no del bytecode.
        //
        // El largo negativo ya lo rechaza `newInstance` del lado Java; esta puerta interna solo
        // ve pedidos bien formados, asi que aca no se repite el chequeo.
        ("java/lang/reflect/Array", "newArray", "(Ljava/lang/Class;I)Ljava/lang/Object;") => {
            let component = mirror_name(metaspace, reference(&args[0]));
            let count = int(&args[1]).max(0) as usize;
            let array_class = format!("[{}", descriptor_of(&component));
            let offset = super::bytecode_interpreter::array_operations::allocate_array_of_class(
                metaspace, heap, &array_class, count,
            )
            .expect("Array.newArray: el heap no alcanza");
            Some(Value::Reference(offset))
        }

        // El hermano de `newArray` para varias dimensiones. Mismo motivo para existir: no hay
        // opcode que aloque "un arreglo de N dimensiones de la clase que dice este mirror" --
        // `multianewarray` lleva la clase en el constant pool--, y `newInstance(Class, int...)` es
        // justamente el que la conoce recien corriendo.
        //
        // Las dimensiones ya vienen validadas del lado Java --ni cero ni mas de 255, ninguna
        // negativa-- igual que en `newArray`.
        ("java/lang/reflect/Array", "multiNewArray", "(Ljava/lang/Class;[I)Ljava/lang/Object;") => {
            let component = mirror_name(metaspace, reference(&args[0]));
            let dims = reference(&args[1]);
            let cuantas = heap.read_u32(dims + array_operations::LENGTH_OFFSET) as usize;
            let mut counts: Vec<usize> = Vec::with_capacity(cuantas);
            for i in 0..cuantas {
                let at = dims + array_operations::ARRAY_HEADER_SIZE + i * 4;
                counts.push(heap.read_u32(at) as i32 as usize);
            }
            // Un `[` por dimension: `newInstance(int.class, 2, 3)` es un `[[I`.
            let mut array_class = String::new();
            for _ in 0..cuantas {
                array_class.push('[');
            }
            array_class.push_str(&descriptor_of(&component));
            let offset = array_operations::allocate_multi(metaspace, heap, &array_class, &counts)
                .expect("Array.multiNewArray: el heap no alcanza");
            Some(Value::Reference(offset))
        }

        // --- `java.lang.reflect.Array`: leer y escribir un elemento -------------------------
        //
        // `newArray` (arriba) sabia **crear** un arreglo cuyo tipo se conoce recien en tiempo de
        // ejecucion, y no habia con que leerlo ni escribirlo: `getLength`, `get`, `set` y sus doce
        // variantes tipadas estaban declaradas `native` y sin puente, o sea que tiraban
        // `UnsatisfiedLinkError`. Un paquete que mide completo con miembros que no funcionan.
        //
        // **Los chequeos no estan aca sino del lado Java**, y es a proposito: el nulo, el indice
        // fuera de rango y el tipo que no corresponde tienen cada uno su excepcion con su mensaje, y
        // armarlas en Rust seria mover el diagnostico al lugar donde peor se lee. Es la misma
        // decision que ya estaba tomada en `newArray` ("esta puerta interna solo ve pedidos bien
        // formados"). Estas cinco puertas leen y escriben, nada mas.
        //
        // Son cinco y no diecisiete porque el **ancho** lo decide el tipo del arreglo, que la VM ya
        // sabe: un `getInt0` sobre un `byte[]` lee un byte y lo extiende con signo, sobre un
        // `char[]` lee dos sin signo. Lo que el lado Java elige es la **forma** en que quiere el
        // valor, y para eso alcanzan `int`, `long`, `float`, `double` y referencia.
        ("java/lang/reflect/Array", "length0", "(Ljava/lang/Object;)I") => {
            // -1 es "no es un arreglo": el lado Java lo convierte en `IllegalArgumentException`.
            let arr = reference(&args[0]);
            Some(Value::Int(match clase_de_arreglo(metaspace, heap, arr) {
                Some(_) => heap.read_u32(arr + array_operations::LENGTH_OFFSET) as i32,
                None => -1,
            }))
        }
        ("java/lang/reflect/Array", "getInt0", "(Ljava/lang/Object;I)I") => {
            let (arr, at, comp) = match posicion(metaspace, heap, &args) {
                Some(t) => t,
                None => return NativeOutcome::Ran(Some(Value::Int(0))),
            };
            let _ = arr;
            Some(Value::Int(match comp {
                // `boolean` y `byte` ocupan un byte; el primero es 0/1 y el segundo tiene signo.
                b'Z' => i32::from(heap.read_u8(at) != 0),
                b'B' => heap.read_u8(at) as i8 as i32,
                b'C' => heap.read_u16(at) as i32,
                b'S' => heap.read_u16(at) as i16 as i32,
                _ => heap.read_u32(at) as i32,
            }))
        }
        ("java/lang/reflect/Array", "getLong0", "(Ljava/lang/Object;I)J") => {
            let (_, at, _) = match posicion(metaspace, heap, &args) {
                Some(t) => t,
                None => return NativeOutcome::Ran(Some(Value::Long(0))),
            };
            Some(Value::Long(heap.read_u64(at) as i64))
        }
        ("java/lang/reflect/Array", "getFloat0", "(Ljava/lang/Object;I)F") => {
            let (_, at, _) = match posicion(metaspace, heap, &args) {
                Some(t) => t,
                None => return NativeOutcome::Ran(Some(Value::Float(0.0))),
            };
            Some(Value::Float(f32::from_bits(heap.read_u32(at))))
        }
        ("java/lang/reflect/Array", "getDouble0", "(Ljava/lang/Object;I)D") => {
            let (_, at, _) = match posicion(metaspace, heap, &args) {
                Some(t) => t,
                None => return NativeOutcome::Ran(Some(Value::Double(0.0))),
            };
            Some(Value::Double(f64::from_bits(heap.read_u64(at))))
        }
        ("java/lang/reflect/Array", "getRef0", "(Ljava/lang/Object;I)Ljava/lang/Object;") => {
            let (_, at, _) = match posicion(metaspace, heap, &args) {
                Some(t) => t,
                None => return NativeOutcome::Ran(Some(Value::Reference(0))),
            };
            Some(Value::Reference(heap.read_u32(at) as usize))
        }
        ("java/lang/reflect/Array", "setInt0", "(Ljava/lang/Object;II)V") => {
            let v = entero(&args[2]);
            if let Some((_, at, comp)) = posicion(metaspace, heap, &args) {
                match comp {
                    b'Z' => heap.write_u8(at, u8::from(v != 0)),
                    b'B' => heap.write_u8(at, v as u8),
                    b'C' | b'S' => heap.write_u16(at, v as u16),
                    _ => heap.write_u32(at, v as u32),
                }
            }
            None
        }
        ("java/lang/reflect/Array", "setLong0", "(Ljava/lang/Object;IJ)V") => {
            let Value::Long(v) = args[2] else {
                return NativeOutcome::Ran(None);
            };
            if let Some((_, at, _)) = posicion(metaspace, heap, &args) {
                heap.write_u64(at, v as u64);
            }
            None
        }
        ("java/lang/reflect/Array", "setFloat0", "(Ljava/lang/Object;IF)V") => {
            let Value::Float(v) = args[2] else {
                return NativeOutcome::Ran(None);
            };
            if let Some((_, at, _)) = posicion(metaspace, heap, &args) {
                heap.write_u32(at, v.to_bits());
            }
            None
        }
        ("java/lang/reflect/Array", "setDouble0", "(Ljava/lang/Object;ID)V") => {
            let Value::Double(v) = args[2] else {
                return NativeOutcome::Ran(None);
            };
            if let Some((_, at, _)) = posicion(metaspace, heap, &args) {
                heap.write_u64(at, v.to_bits());
            }
            None
        }
        ("java/lang/reflect/Array", "setRef0", "(Ljava/lang/Object;ILjava/lang/Object;)V") => {
            let v = reference(&args[2]);
            let arr = reference(&args[0]);
            if let Some((_, at, _)) = posicion(metaspace, heap, &args) {
                // Por `store_reference` y no `write_u32`: el heap tiene que enterarse de que una
                // referencia vieja pasa a apuntar a un objeto nuevo, o el recolector generacional
                // pierde el rastro.
                heap.store_reference(arr, at, v);
            }
            None
        }

        // Un `native` sin puente **no puede voltear el proceso**. Antes esto era un `panic!`, que
        // convertía "esta biblioteca declara un método que la VM todavía no implementa" —una
        // ausencia perfectamente normal mientras se construye un JDK— en la muerte del intérprete,
        // sin traza de Java y sin `catch` posible.
        //
        // `UnsatisfiedLinkError` es lo que la JVM real tira, y es lo correcto por partida doble: es
        // atrapable, así que una biblioteca puede degradar con elegancia; y es un `Error`, así que
        // nadie lo confunde con una condición del programa.
        _ => return NativeOutcome::Unimplemented,
    })
}

/// Qué pasó al intentar correr un `native`.
///
/// Existe porque `Option<Value>` ya estaba ocupado distinguiendo un resultado de un `void`, y hacen
/// falta **tres** respuestas y no dos: devolvió algo, no devolvió nada, y no había qué correr.
pub enum NativeOutcome {
    /// Corrió; `None` si es `void`.
    Ran(Option<Value>),
    /// Corrió, **y esa clase tiene que quedar inicializada** antes de seguir. El valor que se
    /// devuelve es el mirror de esa clase, y se toma **después** de inicializar.
    ///
    /// Existe por un solo nativo, `Class.forName0`, y por una razón que no se puede resolver de
    /// otra manera: cargar una clase no es inicializarla (JVMS §5.4 vs §5.5), pero `forName`
    /// promete las dos cosas. Correr un `<clinit>` es empujar un marco y drivearlo hasta el final
    /// —lo hace `ensure_initialized`, que vive en el intérprete— y un nativo no tiene al intérprete
    /// a mano: recibe el metaspace y el heap, no el `&mut self`.
    ///
    /// Así que el nativo **pide** y el sitio de despacho **hace**. La alternativa era darle al
    /// nativo acceso al intérprete entero, que es mucho más de lo que necesita para esto.
    ///
    /// **Por qué no trae el valor ya calculado.** Porque entre calcularlo y usarlo corre un
    /// `<clinit>` entero, que aloca, y alocar puede disparar el GC. Una referencia guardada mientras
    /// tanto en una variable de Rust **no es una raíz** para el recolector: quedaría apuntando a un
    /// objeto que se movió o se juntó. No se llegó a ver el síntoma --y no hace falta verlo, es la
    /// clase de error que aparece una vez cada mil corridas-- así que el mirror se pide con
    /// [`mirror_de_clase`] recién cuando la inicialización terminó, que es el único momento en que
    /// ya no queda nada por correr en el medio.
    RanEInicializa(String),
    /// El nativo detectó una condición que en Java **es una excepción**, y pide lanzarla.
    ///
    /// El sitio de despacho la lanza con `throw_exception`, así que se ve desde Java como cualquier
    /// otra: se puede atrapar, y no se lleva puesta la máquina virtual.
    ///
    /// Existe porque un nativo tiene condiciones de error que la especificación define como
    /// excepciones —un `arraycopy` con un origen nulo es un `NullPointerException` (JLS §11.5)— y
    /// hasta acá lo único que podía hacer era entrar en pánico. Un pánico es correcto para un
    /// invariante roto de la VM; no lo es para un programa Java que pasó un null.
    Lanza(String),
    /// No hay puente nativo para ese método.
    Unimplemented,
}

/// The `int` payload of an argument (a verifier-guaranteed `Int`).
// --- El vecindario de `java.lang.Class`: nombres, mirrors y descriptores -------------------------
//
// Un mirror es una identidad, no un objeto con campos: la VM guarda uno por clase cargada, y
// tambien por cada clase array sintetica y por cada primitivo (que no tienen archivo de clase
// ninguno). Estas cuatro funciones son la traduccion entre las tres formas en que un tipo se
// nombra -- nombre interno (`java/lang/String`, `[I`, `int`), descriptor (`Ljava/lang/String;`) y
// mirror -- y existen para que las nativas de arriba no la repitan cada una a su manera.

// --- Los atributos de clase que `ClassFile` guarda en crudo -------------------------------------
//
// `ClassFile` parsea el cuerpo de unos pocos atributos y deja los demas como bytes. Estos
// lectores son la otra mitad: cada uno conoce el formato de §4.7 del que le toca y nada mas.

/// El cuerpo crudo del atributo `wanted` de la clase `name`, si lo tiene.
/// Los elementos de un `String[]` del heap, como cadenas de Rust. Un `null` en el arreglo se lee
/// como cadena vacia; para distinguir el nulo de la cadena vacia esta [`leer_arreglo_de_cadenas_con_nulos`].
fn leer_arreglo_de_cadenas(heap: &mut HeapService, arr: usize) -> Vec<String> {
    if arr == 0 {
        return Vec::new();
    }
    let n = heap.read_u32(arr + HEADER_SIZE) as usize;
    let mut out = Vec::with_capacity(n);
    for i in 0..n {
        let elem = heap.read_u32(arr + array_operations::ARRAY_HEADER_SIZE + i * 4) as usize;
        out.push(if elem == 0 { String::new() } else { strings::read(heap, elem) });
    }
    out
}

/// Lo mismo, pero conservando la diferencia entre `null` y `""`. La necesitan las rutas de
/// redireccion: `null` quiere decir "sin archivo" y `""` seria una ruta vacia, que es otra cosa.
fn leer_arreglo_de_cadenas_con_nulos(heap: &mut HeapService, arr: usize) -> Vec<Option<String>> {
    if arr == 0 {
        return Vec::new();
    }
    let n = heap.read_u32(arr + HEADER_SIZE) as usize;
    let mut out = Vec::with_capacity(n);
    for i in 0..n {
        let elem = heap.read_u32(arr + array_operations::ARRAY_HEADER_SIZE + i * 4) as usize;
        out.push(if elem == 0 { None } else { Some(strings::read(heap, elem)) });
    }
    out
}

/// Los elementos de un `int[]` del heap.
fn leer_arreglo_de_int(heap: &mut HeapService, arr: usize) -> Vec<i32> {
    if arr == 0 {
        return Vec::new();
    }
    let n = heap.read_u32(arr + HEADER_SIZE) as usize;
    let mut out = Vec::with_capacity(n);
    for i in 0..n {
        out.push(heap.read_u32(arr + array_operations::ARRAY_HEADER_SIZE + i * 4) as i32);
    }
    out
}

/// El `int` de un `Value`, o 0.
fn entero(v: &Value) -> i32 {
    match v {
        Value::Int(n) => *n,
        _ => 0,
    }
}

/// La redireccion de la **entrada** segun el modo: 0 tuberia, 1 heredar, 2 descartar, 3 archivo.
///
/// Un archivo que no se puede abrir cae a `null()` en vez de hacer fallar el lanzamiento, porque el
/// contrato de `ProcessBuilder` para una entrada ilegible es que el hijo vea fin de archivo -- no que
/// el `start()` explote.
fn redireccion_entrada(modo: i32, ruta: Option<String>) -> std::process::Stdio {
    match modo {
        1 => std::process::Stdio::inherit(),
        2 => std::process::Stdio::null(),
        3 => match ruta.and_then(|r| std::fs::File::open(r).ok()) {
            Some(f) => std::process::Stdio::from(f),
            None => std::process::Stdio::null(),
        },
        _ => std::process::Stdio::piped(),
    }
}

/// La redireccion de una **salida**. Modos: 0 tuberia, 1 heredar, 2 descartar, 3 archivo (pisando),
/// 4 archivo (agregando al final).
///
/// El 3 y el 4 son dos modos y no un modo con bandera porque `ProcessBuilder.Redirect` los distingue
/// como dos tipos, `WRITE` y `APPEND`, y la diferencia es observable: uno borra lo que habia.
fn redireccion_salida(modo: i32, ruta: Option<String>) -> std::process::Stdio {
    match modo {
        1 => std::process::Stdio::inherit(),
        2 => std::process::Stdio::null(),
        3 => match ruta.and_then(|r| std::fs::File::create(r).ok()) {
            Some(f) => std::process::Stdio::from(f),
            None => std::process::Stdio::null(),
        },
        4 => match ruta.and_then(|r| {
            std::fs::OpenOptions::new().create(true).append(true).open(r).ok()
        }) {
            Some(f) => std::process::Stdio::from(f),
            None => std::process::Stdio::null(),
        },
        _ => std::process::Stdio::piped(),
    }
}

/// Lee de la salida o del error de un hijo al `byte[]` dado. Devuelve cuantos bytes puso, o -1 en fin
/// de flujo -- que es exactamente el contrato de `InputStream.read(byte[])`.
fn leer_de_hijo(heap: &mut HeapService, h: usize, arr: usize, es_salida: bool) -> Option<Value> {
    use std::io::Read;
    if arr == 0 {
        return Some(Value::Int(-1));
    }
    let cap = heap.read_u32(arr + HEADER_SIZE) as usize;
    let mut buf = vec![0u8; cap];
    let leidos = PROCS.with(|t| {
        let mut t = t.borrow_mut();
        let Some(p) = t.get_mut(h).and_then(|e| e.as_mut()) else {
            return -1i32;
        };
        // Con `redirectErrorStream`, `getErrorStream()` esta vacio --como en el JDK-- y la salida
        // entrega primero todo stdout y despues todo stderr.
        if p.unir_error && !es_salida {
            return -1;
        }
        let r = if es_salida {
            p.hijo.stdout.as_mut().map(|s| s.read(&mut buf))
        } else {
            p.hijo.stderr.as_mut().map(|s| s.read(&mut buf))
        };
        let n = match r {
            // Cero bytes de una tuberia **es** fin de flujo: `read` solo devuelve 0 cuando el otro
            // extremo se cerro. Devolver 0 haria que el lado Java girara en vacio para siempre.
            Some(Ok(0)) => -1,
            Some(Ok(n)) => n as i32,
            _ => -1,
        };
        if n < 0 && es_salida && p.unir_error {
            // La salida se agoto: se sigue por el error, que es lo que hace que los dos lleguen por
            // `getInputStream()`.
            return match p.hijo.stderr.as_mut().map(|s| s.read(&mut buf)) {
                Some(Ok(0)) => -1,
                Some(Ok(m)) => m as i32,
                _ => -1,
            };
        }
        n
    });
    if leidos > 0 {
        for i in 0..leidos as usize {
            heap.write_u8(arr + array_operations::ARRAY_HEADER_SIZE + i, buf[i]);
        }
    }
    Some(Value::Int(leidos))
}

fn attribute_body<'a>(
    metaspace: &'a mut MetaspaceService,
    name: &str,
    wanted: &str,
) -> Option<&'a [u8]> {
    let class = metaspace.get_or_load(name)?;
    class
        .attributes
        .iter()
        .find(|a| class.utf8(a.name_index) == Some(wanted))
        .map(|a| a.info.as_slice())
}

fn has_attribute(metaspace: &mut MetaspaceService, name: &str, wanted: &str) -> bool {
    attribute_body(metaspace, name, wanted).is_some()
}

/// The absolute heap offset of the slot a `Field` object names, and the object that HOLDS it (for
/// the write barrier). A static field's slot lives on the owner's `Class` mirror; an instance
/// field's at the target object's offset. Reads the Field's own `clazz`/`name`/`modifiers`.
/// El nombre de la clase que declara el campo descripto por `field_obj`, **solo si el campo es
/// estático**; `None` para uno de instancia (o para un `Field` nulo).
///
/// Existe para que el intérprete pueda correrle el `<clinit>` antes de dejar que los accesores
/// nativos toquen el slot: leer o escribir un estático **por reflexión** es un uso activo de la
/// clase igual que un `getstatic` (JLS §12.4.1), y sin esto el campo se lee en su valor por defecto
/// mientras nadie haya tocado la clase por la vía normal. Ver el finding #361.
pub(crate) fn static_field_owner(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    field_obj: usize,
) -> Option<String> {
    if field_obj == 0 {
        return None;
    }
    const F: &str = "java/lang/reflect/Field";
    let mods_off = field_offset(metaspace, F, "modifiers");
    if heap.read_u32(field_obj + mods_off) & 0x0008 == 0 {
        return None;
    }
    let clazz_off = field_offset(metaspace, F, "clazz");
    let clazz_mirror = heap.read_u32(field_obj + clazz_off) as usize;
    metaspace.class_name_at_mirror(clazz_mirror).map(str::to_string)
}

fn field_slot_addr(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    field_obj: usize,
    target: usize,
) -> (usize, usize) {
    const F: &str = "java/lang/reflect/Field";
    let clazz_off = field_offset(metaspace, F, "clazz");
    let name_off = field_offset(metaspace, F, "name");
    let mods_off = field_offset(metaspace, F, "modifiers");
    let clazz_mirror = heap.read_u32(field_obj + clazz_off) as usize;
    let owner = metaspace
        .class_name_at_mirror(clazz_mirror)
        .expect("Field.clazz has no class")
        .to_string();
    let name_ref = heap.read_u32(field_obj + name_off) as usize;
    let name = strings::read(heap, name_ref);
    let mods = heap.read_u32(field_obj + mods_off);
    if mods & 0x0008 != 0 {
        // static: the slot is on the owner's Class mirror.
        let at = class_operations::static_slot(metaspace, heap, &owner, &name);
        let uuid = metaspace.class_id(&owner).to_string();
        let holder = metaspace.class_object(&uuid).unwrap_or(0);
        (at, holder)
    } else {
        // instance: the slot is at the target object's field offset.
        let at = target + field_offset(metaspace, &owner, &name);
        (at, target)
    }
}

/// Materialise every `RuntimeVisibleAnnotation` on class `this` as an object, returning their heap
/// offsets — backs `Class.declaredAnnotations0`. Each is an instance of a class the VM spins
/// implementing the @interface (see `annotation_factory`). Since annotation objects carry no
/// instance fields, `allocate` alone yields a complete object; the constructor (just `super()`)
/// need not run, which is what lets this stay a plain native without re-entering the interpreter.
/// Las anotaciones de un **metodo**, como objetos en el heap.
///
/// Comparte todo con [`annotation_objects`] salvo de donde sale el atributo: del `method_info` del
/// metodo con esa firma en vez de del `ClassFile`. La clase sintetica se nombra con la clase, el
/// metodo y la ranura, para que dos metodos anotados con la misma anotacion no compartan una que
/// tenga valores distintos.
fn method_annotation_objects(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    owner: &str,
    name: &str,
    descriptor: &str,
) -> Vec<usize> {
    let resolved: Vec<ResolvedAnnotation> = match metaspace.get_or_load(owner) {
        Some(cf) => cf
            .methods
            .iter()
            .find(|m| {
                cf.utf8(m.name_index) == Some(name)
                    && cf.utf8(m.descriptor_index) == Some(descriptor)
            })
            .and_then(|m| {
                m.attributes
                    .iter()
                    .find(|a| cf.utf8(a.name_index) == Some("RuntimeVisibleAnnotations"))
                    .map(|a| annotations::resolve(cf, &a.info))
            })
            .unwrap_or_default(),
        None => Vec::new(),
    };
    // El nombre sintetico lleva la firma saneada: `(` y `/` no pueden ir en un nombre de clase.
    let tag: String = format!("{name}{descriptor}")
        .chars()
        .map(|c| if c.is_ascii_alphanumeric() { c } else { '_' })
        .collect();
    spin_annotation_objects(metaspace, heap, &resolved, &format!("{owner}$$MAnno${tag}"))
}

fn annotation_objects(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    this: &str,
) -> Vec<usize> {
    // Resolve the attribute into an owned tree FIRST, so the class-file borrow ends before we begin
    // mutating the metaspace (spinning + loading the synthetic classes).
    let resolved: Vec<ResolvedAnnotation> = match metaspace.get_or_load(this) {
        Some(cf) => cf
            .attributes
            .iter()
            .find(|a| cf.utf8(a.name_index) == Some("RuntimeVisibleAnnotations"))
            .map(|a| annotations::resolve(cf, &a.info))
            .unwrap_or_default(),
        None => Vec::new(),
    };
    spin_annotation_objects(metaspace, heap, &resolved, &format!("{this}$$Anno"))
}

/// Fabrica un objeto por anotacion resuelta, con una clase sintetica por ranura bajo `prefix`.
///
/// La clase es **estable**: reflexionar dos veces sobre lo mismo reusa la que ya se giro en vez de
/// acuñar una nueva en cada llamada.
fn spin_annotation_objects(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    resolved: &[ResolvedAnnotation],
    prefix: &str,
) -> Vec<usize> {
    let mut objects = Vec::with_capacity(resolved.len());
    for (i, ann) in resolved.iter().enumerate() {
        let iface = ann
            .type_descriptor
            .strip_prefix('L')
            .and_then(|s| s.strip_suffix(';'))
            .unwrap_or(&ann.type_descriptor)
            .to_string();
        let elements = annotation_elements(metaspace, &iface, ann);
        let synthetic = format!("{prefix}${i}");
        if metaspace.get(&synthetic).is_none() {
            let bytes = annotation_factory::generate_annotation_class(&synthetic, &iface, &elements);
            match ClassFile::from_bytes(&bytes) {
                Ok(class) => metaspace.add(synthetic.clone(), class),
                Err(_) => continue,
            }
        }
        class_operations::load_class(metaspace, heap, &synthetic);
        objects.push(objects_operations::allocate(metaspace, heap, &synthetic));
    }
    objects
}

/// The elements to give the factory for one annotation: every accessor the @interface declares,
/// paired with the value written at the use site or, absent that, the accessor's `AnnotationDefault`.
fn annotation_elements(
    metaspace: &mut MetaspaceService,
    iface: &str,
    ann: &ResolvedAnnotation,
) -> Vec<Element> {
    let cf = match metaspace.get_or_load(iface) {
        Some(cf) => cf,
        None => return Vec::new(),
    };
    let mut out = Vec::new();
    for m in &cf.methods {
        let (Some(name), Some(desc)) = (cf.utf8(m.name_index), cf.utf8(m.descriptor_index)) else {
            continue;
        };
        // Element accessors are non-static, parameterless, and not <init>/<clinit>.
        if m.is_static() || name.starts_with('<') || !desc.starts_with("()") {
            continue;
        }
        let value = if let Some((_, v)) = ann.elements.iter().find(|(n, _)| n.as_str() == name) {
            v.clone()
        } else if let Some(def) =
            m.attributes.iter().find(|a| cf.utf8(a.name_index) == Some("AnnotationDefault"))
        {
            match annotations::resolve_default(cf, &def.info) {
                Some(v) => v,
                None => continue,
            }
        } else {
            continue;
        };
        out.push(Element { name: name.to_string(), descriptor: desc.to_string(), value });
    }
    out
}

/// El nombre de clase que el atributo `wanted` nombra en el offset `at` de su cuerpo.
fn attribute_class(
    metaspace: &mut MetaspaceService,
    name: &str,
    wanted: &str,
    at: usize,
) -> Option<String> {
    let index = {
        let body = attribute_body(metaspace, name, wanted)?;
        if body.len() < at + 2 {
            return None;
        }
        u16::from_be_bytes([body[at], body[at + 1]])
    };
    let class = metaspace.get(name)?;
    class.class_name(index).map(str::to_string)
}

/// Los nombres de clase de un atributo con forma `u2 count` + `u2[] classes` -- que es la de
/// `NestMembers` y la de `PermittedSubclasses`.
fn attribute_class_list(
    metaspace: &mut MetaspaceService,
    name: &str,
    wanted: &str,
) -> Vec<String> {
    let indices = {
        let Some(body) = attribute_body(metaspace, name, wanted) else {
            return Vec::new();
        };
        if body.len() < 2 {
            return Vec::new();
        }
        let count = u16::from_be_bytes([body[0], body[1]]) as usize;
        (0..count)
            .filter_map(|i| {
                let at = 2 + i * 2;
                (at + 1 < body.len()).then(|| u16::from_be_bytes([body[at], body[at + 1]]))
            })
            .collect::<Vec<u16>>()
    };
    let Some(class) = metaspace.get(name) else {
        return Vec::new();
    };
    indices.iter().filter_map(|&i| class.class_name(i).map(str::to_string)).collect()
}

/// La entrada de `InnerClasses` que habla de la clase `name` MISMA, como
/// `(interna, externa, nombre simple)`. La externa es `None` para una local o anonima, y el
/// nombre simple es `None` para una anonima -- que es como el archivo de clase distingue los tres
/// casos sin decirlo con un flag.
fn inner_class_entry(
    metaspace: &mut MetaspaceService,
    name: &str,
) -> Option<(String, Option<String>, Option<String>)> {
    let entries = inner_classes(metaspace, name);
    entries.into_iter().find(|e| e.0 == name)
}

/// Toda la tabla `InnerClasses` de la clase `name`, decodificada.
fn inner_classes(
    metaspace: &mut MetaspaceService,
    name: &str,
) -> Vec<(String, Option<String>, Option<String>)> {
    let rows = {
        let Some(body) = attribute_body(metaspace, name, "InnerClasses") else {
            return Vec::new();
        };
        if body.len() < 2 {
            return Vec::new();
        }
        let count = u16::from_be_bytes([body[0], body[1]]) as usize;
        (0..count)
            .filter_map(|i| {
                let at = 2 + i * 8;
                if at + 5 >= body.len() {
                    return None;
                }
                Some((
                    u16::from_be_bytes([body[at], body[at + 1]]),
                    u16::from_be_bytes([body[at + 2], body[at + 3]]),
                    u16::from_be_bytes([body[at + 4], body[at + 5]]),
                ))
            })
            .collect::<Vec<(u16, u16, u16)>>()
    };
    let Some(class) = metaspace.get(name) else {
        return Vec::new();
    };
    rows.iter()
        .filter_map(|&(inner, outer, simple)| {
            let inner_name = class.class_name(inner)?.to_string();
            let outer_name = class.class_name(outer).map(str::to_string);
            let simple_name = class.utf8(simple).map(str::to_string);
            Some((inner_name, outer_name, simple_name))
        })
        .collect()
}

/// Las clases que `name` declara adentro: las entradas cuyo `outer_class_info` es ella.
fn declared_inner_classes(metaspace: &mut MetaspaceService, name: &str) -> Vec<String> {
    inner_classes(metaspace, name)
        .into_iter()
        .filter(|(inner, outer, _)| {
            inner != name && outer.as_deref() == Some(name)
        })
        .map(|(inner, _, _)| inner)
        .collect()
}

/// Los componentes de un record, como `(nombre, descriptor)`, o `None` si la clase no lo es.
fn record_components(
    metaspace: &mut MetaspaceService,
    name: &str,
) -> Option<Vec<(String, String)>> {
    let pairs = {
        let body = attribute_body(metaspace, name, "Record")?;
        if body.len() < 2 {
            return Some(Vec::new());
        }
        let count = u16::from_be_bytes([body[0], body[1]]) as usize;
        let mut at = 2;
        let mut out = Vec::with_capacity(count);
        for _ in 0..count {
            if at + 5 >= body.len() {
                break;
            }
            let name_index = u16::from_be_bytes([body[at], body[at + 1]]);
            let descriptor_index = u16::from_be_bytes([body[at + 2], body[at + 3]]);
            let attributes = u16::from_be_bytes([body[at + 4], body[at + 5]]) as usize;
            at += 6;
            // Cada componente puede traer sus propios atributos (`Signature`, anotaciones); se
            // saltean leyendo su largo, que es un u4 detras del indice del nombre.
            for _ in 0..attributes {
                if at + 5 >= body.len() {
                    break;
                }
                let length = u32::from_be_bytes([
                    body[at + 2],
                    body[at + 3],
                    body[at + 4],
                    body[at + 5],
                ]) as usize;
                at += 6 + length;
            }
            out.push((name_index, descriptor_index));
        }
        out
    };
    let class = metaspace.get(name)?;
    Some(
        pairs
            .iter()
            .filter_map(|&(n, d)| Some((class.utf8(n)?.to_string(), class.utf8(d)?.to_string())))
            .collect(),
    )
}

/// Los tipos que un metodo declara en su `throws`, leidos del atributo `Exceptions` (§4.7.5).
/// Vacio cuando no lo tiene, que es lo mismo que no declarar ninguno.
///
/// El atributo se parsea acá y no en `ClassFile` porque su cuerpo se guarda crudo: son dos
/// bytes de cuenta y dos por indice al pool, y este es el unico lector que hay.
fn declared_exceptions(class: &ClassFile, member: &crate::jvm::parser::MemberInfo) -> Vec<String> {
    for attribute in &member.attributes {
        if class.utf8(attribute.name_index) != Some("Exceptions") {
            continue;
        }
        let body = &attribute.info;
        if body.len() < 2 {
            return Vec::new();
        }
        let count = u16::from_be_bytes([body[0], body[1]]) as usize;
        let mut out = Vec::with_capacity(count);
        for i in 0..count {
            let at = 2 + i * 2;
            if at + 1 >= body.len() {
                break;
            }
            let index = u16::from_be_bytes([body[at], body[at + 1]]);
            if let Some(name) = class.class_name(index) {
                out.push(name.to_string());
            }
        }
        return out;
    }
    Vec::new()
}

/// Un descriptor de metodo partido en `(nombres internos de los parametros, del retorno)`.
/// `(Ljava/lang/String;[IJ)V` → `(["java/lang/String", "[I", "long"], "void")`.
fn split_descriptor(descriptor: &str) -> (Vec<String>, String) {
    let bytes = descriptor.as_bytes();
    let mut i = 1; // saltar el '('
    let mut parameters = Vec::new();
    while i < bytes.len() && bytes[i] != b')' {
        let start = i;
        while i < bytes.len() && bytes[i] == b'[' {
            i += 1;
        }
        if i < bytes.len() && bytes[i] == b'L' {
            while i < bytes.len() && bytes[i] != b';' {
                i += 1;
            }
        }
        i += 1;
        parameters.push(internal_name_of(&descriptor[start..i]));
    }
    let returns = if i + 1 <= descriptor.len() {
        internal_name_of(&descriptor[i + 1..])
    } else {
        "void".to_string()
    };
    (parameters, returns)
}

/// El nombre interno de la clase que un mirror nombra.
pub(super) fn mirror_name(metaspace: &MetaspaceService, mirror: usize) -> String {
    metaspace
        .class_name_at_mirror(mirror)
        .expect("Class: no hay ninguna clase en este mirror")
        .to_string()
}

/// Las nueve palabras que nombran un tipo primitivo. No hay archivo de clase detras de ninguna,
/// asi que el nombre **es** la identidad.
fn is_primitive_name(name: &str) -> bool {
    matches!(
        name,
        "int" | "long" | "double" | "float" | "short" | "byte" | "char" | "boolean" | "void"
    )
}

/// El descriptor de campo de un nombre interno: `int` → `I`, `java/lang/String` →
/// `Ljava/lang/String;`, y un array ya viene en forma de descriptor.
pub(super) fn descriptor_of(name: &str) -> String {
    match name {
        "int" => "I".to_string(),
        "long" => "J".to_string(),
        "double" => "D".to_string(),
        "float" => "F".to_string(),
        "short" => "S".to_string(),
        "byte" => "B".to_string(),
        "char" => "C".to_string(),
        "boolean" => "Z".to_string(),
        "void" => "V".to_string(),
        n if n.starts_with('[') => n.to_string(),
        n => format!("L{n};"),
    }
}

/// La vuelta de [`descriptor_of`]: el nombre interno que un descriptor de campo nombra.
fn internal_name_of(descriptor: &str) -> String {
    match descriptor.as_bytes().first() {
        Some(b'I') => "int".to_string(),
        Some(b'J') => "long".to_string(),
        Some(b'D') => "double".to_string(),
        Some(b'F') => "float".to_string(),
        Some(b'S') => "short".to_string(),
        Some(b'B') => "byte".to_string(),
        Some(b'C') => "char".to_string(),
        Some(b'Z') => "boolean".to_string(),
        Some(b'V') => "void".to_string(),
        Some(b'L') => descriptor[1..descriptor.len() - 1].to_string(),
        _ => descriptor.to_string(), // ya es un array
    }
}

/// El nombre interno del tipo COMPONENTE de una clase array, o `None` si no es un array. Una
/// dimension menos: `[[I` → `[I`, y recien `[I` → `int`.
fn component_name(array_class: &str) -> Option<String> {
    let inner = array_class.strip_prefix('[')?;
    Some(internal_name_of(inner))
}

/// El mirror de un nombre interno cualquiera, creandolo si hace falta. Las tres formas de tipo
/// llegan por caminos distintos: un array por su mirror sintetico, un primitivo por el suyo
/// (idem, pero indexado por la palabra clave) y una clase de verdad cargandola.
/// El mirror de esa clase, para quien lo necesite desde afuera de este módulo.
///
/// Lo usa el sitio de despacho de [`NativeOutcome::RanEInicializa`], que tiene que tomarlo después
/// de correr el `<clinit>` y no antes. Ver la nota de esa variante.
pub fn mirror_de_clase(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    name: &str,
) -> usize {
    mirror_for(metaspace, heap, name)
}

/// El nombre de clase de `arr` si es un arreglo, o `None` (null, o no es un arreglo).
fn clase_de_arreglo(
    metaspace: &MetaspaceService,
    heap: &HeapService,
    arr: usize,
) -> Option<String> {
    if arr == 0 {
        return None;
    }
    let nombre = metaspace.class_name_at_mirror(heap.read_u32(arr) as usize)?;
    if nombre.starts_with('[') {
        Some(nombre.to_string())
    } else {
        None
    }
}

/// Donde vive el elemento `args[1]` del arreglo `args[0]`, y de que tipo es su componente.
///
/// Devuelve `(arreglo, offset del elemento, primer byte del descriptor del componente)`. `None` si
/// el arreglo es nulo, no es un arreglo, o el indice esta fuera de rango -- los tres casos que el
/// lado Java ya chequeo, asi que aca solo evitan leer memoria que no es.
fn posicion(
    metaspace: &MetaspaceService,
    heap: &HeapService,
    args: &[Value],
) -> Option<(usize, usize, u8)> {
    let arr = reference(&args[0]);
    let idx = entero(&args[1]);
    let clase = clase_de_arreglo(metaspace, heap, arr)?;
    let largo = heap.read_u32(arr + array_operations::LENGTH_OFFSET) as i32;
    if idx < 0 || idx >= largo {
        return None;
    }
    let ancho = array_operations::array_element_width(&clase);
    let comp = clase.as_bytes()[1];
    Some((arr, arr + array_operations::ARRAY_HEADER_SIZE + idx as usize * ancho, comp))
}

fn mirror_for(metaspace: &mut MetaspaceService, heap: &mut HeapService, name: &str) -> usize {
    if name.starts_with('[') {
        return array_operations::array_class_mirror(metaspace, heap, name);
    }
    if is_primitive_name(name) {
        return primitive_mirror(metaspace, heap, name);
    }
    class_operations::load_class(metaspace, heap, name);
    let uuid = metaspace.class_id(name).to_string();
    metaspace.class_object(&uuid).unwrap_or(0)
}

/// El mirror de un primitivo por su palabra clave, creado la primera vez y cacheado despues --
/// que es lo que hace que `int.class == int.class`. Solo cabecera: un primitivo no tiene
/// estaticos, y como todo mirror va a **Old**, donde el GC no lo mueve.
fn primitive_mirror(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    keyword: &str,
) -> usize {
    let uuid = metaspace.class_id(keyword).to_string();
    if let Some(offset) = metaspace.class_object(&uuid) {
        return offset;
    }
    let offset = heap.malloc_old(HEADER_SIZE);
    metaspace.set_class_object(&uuid, offset);
    class_operations::load_class(metaspace, heap, "java/lang/Class");
    let class_uuid = metaspace.class_id("java/lang/Class").to_string();
    let class_mirror = metaspace.class_object(&class_uuid).unwrap_or(0);
    heap.write_u32(offset, class_mirror as u32);
    offset
}

/// Un array de referencias de la clase `array_class`, con los elementos ya adentro. En **Old** y
/// por el write barrier, por lo mismo que `build_object_array`: el llamador tiene las referencias
/// en un `Vec` de Rust que no es raiz de nada.
fn reference_array(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    array_class: &str,
    elements: &[usize],
) -> usize {
    let mirror = array_operations::array_class_mirror(metaspace, heap, array_class);
    let offset =
        heap.malloc_old(array_operations::ARRAY_HEADER_SIZE + elements.len() * SLOT_SIZE);
    heap.write_u32(offset, mirror as u32);
    heap.write_u32(offset + array_operations::LENGTH_OFFSET, elements.len() as u32);
    for (i, &value) in elements.iter().enumerate() {
        if value != 0 {
            heap.store_reference(
                offset,
                offset + array_operations::ARRAY_HEADER_SIZE + i * SLOT_SIZE,
                value,
            );
        }
    }
    offset
}

fn int(value: &Value) -> i32 {
    match value {
        Value::Int(n) => *n,
        other => panic!("native: expected an int argument, found {other:?}"),
    }
}

/// The `long` payload of an argument (a verifier-guaranteed `Long`).
fn long(value: &Value) -> i64 {
    match value {
        Value::Long(n) => *n,
        other => panic!("native: expected a long argument, found {other:?}"),
    }
}

/// The heap offset of a reference argument (a verifier-guaranteed `Reference`).
fn double(value: &Value) -> f64 {
    match value {
        Value::Double(d) => *d,
        other => panic!("se esperaba un double, llego {other:?}"),
    }
}

fn float(value: &Value) -> f32 {
    match value {
        Value::Float(f) => *f,
        other => panic!("se esperaba un float, llego {other:?}"),
    }
}

fn reference(value: &Value) -> usize {
    match value {
        Value::Reference(offset) => *offset,
        other => panic!("native: expected a reference argument, found {other:?}"),
    }
}

// --- APT fase 3: ayudantes de la reificación de `SymElement` -------------------------------------

/// El `SymbolId` que reifica un `SymElement` receptor: lee su campo `int sym`. Es el índice en la
/// tabla viva de `javac` que los native de arriba usan para releer el símbolo.
fn sym_of(receiver: &Value, metaspace: &mut MetaspaceService, heap: &HeapService) -> usize {
    let this = reference(receiver);
    let sym_offset = field_offset(metaspace, "jdk/internal/apt/SymElement", "sym");
    heap.read_u32(this + sym_offset) as usize
}

/// El [`AptContext`] atado (con `JVM::set_apt`). Panica si falta: un native de `SymElement` sólo se
/// alcanza durante una corrida de procesador, donde el contexto siempre está presente.
fn apt_ref(apt: &Option<AptContext>) -> &AptContext {
    apt.as_ref().expect("SymElement native sin un AptContext (usar JVM::set_apt)")
}

/// Construye un `jdk/internal/apt/SymName` (un `javax.lang.model.element.Name`) que envuelve `text`:
/// aloca el objeto y le escribe el `String` internado en su campo `value`. Alocado en **Old** (como
/// un `SymElement`): un `Name` es un handle que el procesador puede sostener a través de otras
/// alocaciones/GC. El `String` va por la barrera de escritura (`store_reference`), por si el Name
/// (Old) apunta a una `String` joven. Mismo patrón que `Exec::materialize_method_type`.
fn make_name(metaspace: &mut MetaspaceService, heap: &mut HeapService, text: &str) -> usize {
    const SYM_NAME: &str = "jdk/internal/apt/SymName";
    class_operations::load_class(metaspace, heap, SYM_NAME);
    let object = allocate_old(metaspace, heap, SYM_NAME);
    let value = strings::intern(metaspace, heap, text);
    let value_offset = field_offset(metaspace, SYM_NAME, "value");
    heap.store_reference(object, object + value_offset, value);
    object
}

/// Fills `out` with bytes from the operating system's own generator, reporting whether it could.
///
/// Two implementations, both the platform's documented system source:
///
///   - **Windows**: `BCryptGenRandom` with `BCRYPT_USE_SYSTEM_PREFERRED_RNG`, so no algorithm
///     handle has to be opened and closed. It is what replaced `RtlGenRandom`.
///   - **everything else**: `/dev/urandom`, read to the end of the buffer. It does not block once
///     the pool is seeded, which for any process that has reached this code is already true.
///
/// A short read is reported as a failure. The caller cannot tell which bytes came from the OS and
/// which are still zero, and half a seed that looks whole is worse than no seed at all.
fn os_entropy(out: &mut [u8]) -> bool {
    if out.is_empty() {
        return true;
    }
    #[cfg(windows)]
    {
        #[link(name = "bcrypt")]
        extern "system" {
            fn BCryptGenRandom(
                h_algorithm: *mut core::ffi::c_void,
                pb_buffer: *mut u8,
                cb_buffer: u32,
                dw_flags: u32,
            ) -> i32;
        }
        const USE_SYSTEM_PREFERRED_RNG: u32 = 0x0000_0002;
        // Chunked because the count is a u32 and a caller could ask for more than that.
        for chunk in out.chunks_mut(u32::MAX as usize) {
            let status = unsafe {
                BCryptGenRandom(
                    core::ptr::null_mut(),
                    chunk.as_mut_ptr(),
                    chunk.len() as u32,
                    USE_SYSTEM_PREFERRED_RNG,
                )
            };
            if status != 0 {
                return false;
            }
        }
        true
    }
    #[cfg(not(windows))]
    {
        use std::io::Read;
        let mut f = match std::fs::File::open("/dev/urandom") {
            Ok(f) => f,
            Err(_) => return false,
        };
        f.read_exact(out).is_ok()
    }
}


/// El espacio del volumen que contiene a `ruta`: (total, utilizable, sin asignar), en bytes.
///
/// Los tres que el formato de `FileStore` distingue, y **no son dos de lo mismo**: "utilizable" es
/// lo que este usuario puede escribir y "sin asignar" es lo que le queda al volumen. Con una cuota
/// puesta, el segundo es mayor que el primero; sin cuota son iguales. Devolverlos como uno solo
/// haria que un `getUsableSpace` mintiera justo en la maquina donde la cuota importa.
///
/// `None` si no se pudo averiguar. Es distinto de cero: cero bytes libres es una respuesta.
#[cfg(windows)]
fn espacio_de_volumen(ruta: &str) -> Option<(i64, i64, i64)> {
    #[link(name = "kernel32")]
    extern "system" {
        fn GetDiskFreeSpaceExW(
            lp_directory_name: *const u16,
            lp_free_bytes_available_to_caller: *mut u64,
            lp_total_number_of_bytes: *mut u64,
            lp_total_number_of_free_bytes: *mut u64,
        ) -> i32;
    }
    // UTF-16 terminado en cero, que es lo que la API `W` espera.
    let mut ancha: Vec<u16> = ruta.encode_utf16().collect();
    ancha.push(0);
    let mut disponible: u64 = 0;
    let mut total: u64 = 0;
    let mut libre: u64 = 0;
    let ok = unsafe {
        GetDiskFreeSpaceExW(ancha.as_ptr(), &mut disponible, &mut total, &mut libre)
    };
    if ok == 0 {
        return None;
    }
    Some((total as i64, disponible as i64, libre as i64))
}

/// Ver la version de Windows. Fuera de Windows no hay una forma portable de preguntar esto sin
/// dependencias, así que se contesta "no se sabe" -- que es lo que el lado Java traduce a
/// `IOException`, y no un cero que se leería como un disco lleno.
#[cfg(not(windows))]
fn espacio_de_volumen(_ruta: &str) -> Option<(i64, i64, i64)> {
    None
}

/// Las raices del sistema de archivos.
///
/// En Windows, las unidades que existen **ahora**: `GetLogicalDrives` devuelve un bit por letra, y
/// se pregunta en cada llamada en vez de guardarse -- un pendrive que se conecta agrega una raiz, y
/// una lista cacheada se quedaria vieja justo cuando alguien la mira para ver que hay conectado.
///
/// Fuera de Windows hay una sola raiz y es `/`. No hace falta preguntarle a nadie.
#[cfg(windows)]
fn raices_del_sistema() -> Vec<String> {
    #[link(name = "kernel32")]
    extern "system" {
        fn GetLogicalDrives() -> u32;
    }
    let mascara = unsafe { GetLogicalDrives() };
    let mut out = Vec::new();
    for i in 0..26u32 {
        if mascara & (1 << i) != 0 {
            out.push(format!("{}:\\", (b'A' + i as u8) as char));
        }
    }
    out
}

/// Ver la version de Windows.
#[cfg(not(windows))]
fn raices_del_sistema() -> Vec<String> {
    vec!["/".to_string()]
}
