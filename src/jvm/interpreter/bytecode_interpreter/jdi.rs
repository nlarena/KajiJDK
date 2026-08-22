//! **JDI** — Java Debug Interface (Hito I4): la **API cliente** de alto nivel sobre JDWP. Es la capa
//! de más arriba del stack JPDA (`JVMTI → JDWP → JDI → jdb`) y la **contraparte del [bridge](super::bridge)**:
//! donde el bridge está del lado de la VM (traduce eventos JVMTI a paquetes), la JDI está del lado del
//! **debugger** (habla JDWP con una VM remota y expone objetos tipados en vez de bytes).
//!
//! Reemplaza al cliente hecho a mano —armar/parsear paquetes a pelo, como haría un script suelto— por
//! *mirrors* del VM remoto: [`Vm`] (la conexión), [`Location`], [`EventRequest`] y [`Event`]. Un `jdb`
//! fiel se escribe sobre esto, igual que el `jdb` real de un JDK se escribe sobre `com.sun.jdi`.
//!
//! Depende **solo** del codec [`super::jdwp`] (puro) y de un transporte `Read + Write` —**no** toca la
//! VM ni el [`super::jvmti`]—, así que un cliente y la [`JdwpSession`](super::jdwp::JdwpSession) servidor
//! se pueden probar de punta a punta **en memoria**, sin socket.

use std::collections::VecDeque;
use std::io::{self, Read, Write};

use super::jdwp::{
    command_set, event_kind, event_request_command, frame_command, method_command,
    reference_type_command, thread_command, value_tag, vm_command, CommandPacket, Packet, Reader,
    ReplyPacket, Writer, EVENT_COMPOSITE, HANDSHAKE, MOD_LOCATION_ONLY, TYPE_TAG_CLASS,
};

/// El error de una respuesta JDWP truncada o malformada (data más corta de lo esperado).
fn short() -> io::Error {
    io::Error::new(io::ErrorKind::InvalidData, "respuesta JDWP corta o malformada")
}

/// Una **ubicación** de código: `(method, índice de bytecode)`. *Mirror* de `com.sun.jdi.Location`
/// (versión mínima: sin el tipo declarante, que necesitaría resolver `ReferenceType`).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Location {
    pub method: u64,
    pub index: u64,
}

/// Un **pedido de evento** ya registrado en la VM remota, con el `requestID` que ésta devolvió.
/// *Mirror* de `com.sun.jdi.request.EventRequest`.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct EventRequest {
    pub id: i32,
    pub kind: u8,
}

/// Un **evento** empujado por la VM (un `Composite` decodificado a algo tipado). *Mirror* de los
/// `com.sun.jdi.event.*`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Event {
    /// La VM arrancó (evento automático al conectar): trae el hilo inicial, sin ubicación.
    VmStart { request_id: i32, thread: u64 },
    Breakpoint { request_id: i32, thread: u64, location: Location },
    SingleStep { request_id: i32, thread: u64, location: Location },
    MethodEntry { request_id: i32, thread: u64, location: Location },
    MethodExit { request_id: i32, thread: u64, location: Location },
    Exception { request_id: i32, thread: u64, location: Location, exception: u64 },
    /// Un evento que no modelamos (otra `eventKind`): se conserva el mínimo para no perderlo.
    Other { request_id: i32, kind: u8 },
}

/// Un **valor** leído de la VM remota — *mirror* de `com.sun.jdi.Value`. El `float`/`double` se
/// reconstruyen desde sus bits (como los serializó el servidor).
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum Value {
    Int(i32),
    Long(i64),
    Float(f32),
    Double(f64),
    Object(u64),
}

/// Un **frame** de la pila remota — *mirror* de `com.sun.jdi.StackFrame`: su `id` opaco (que se le pasa
/// de vuelta a `GetValues`) y su ubicación.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct StackFrame {
    pub id: u64,
    pub location: Location,
}

/// Una **clase** remota — *mirror* de `com.sun.jdi.ReferenceType`: su `referenceTypeID`, su firma JVM
/// (`LAdd;`) y su estado.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ClassInfo {
    pub id: u64,
    pub signature: String,
    pub status: i32,
}

/// Un **método** remoto — *mirror* de `com.sun.jdi.Method`: su `methodID` (el que la VM usa para un
/// breakpoint), nombre, descriptor y flags de acceso.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MethodInfo {
    pub id: u64,
    pub name: String,
    pub signature: String,
    pub mod_bits: i32,
}

// ---- helpers puros (encode/decode de la data, sin I/O) -------------------------------------------

/// Arma la **data** de un `EventRequest.Set`: `eventKind · suspendPolicy · modifiers`. Un breakpoint
/// lleva un modificador `LocationOnly` con su posición; un pedido sin ubicación (single-step, method
/// entry/exit, exception) va con cero modificadores.
pub fn encode_event_set(kind: u8, suspend_policy: u8, location: Option<Location>) -> Vec<u8> {
    let mut w = Writer::new();
    w.byte(kind);
    w.byte(suspend_policy);
    match location {
        Some(loc) => {
            w.int(1); // un modificador
            w.byte(MOD_LOCATION_ONLY);
            w.byte(TYPE_TAG_CLASS);
            w.id(0); // classID (la VM no numera clases todavía)
            w.id(loc.method);
            w.long(loc.index as i64);
        }
        None => {
            w.int(0);
        }
    }
    w.finish()
}

/// Decodifica el cuerpo de un `Event.Composite` a `(suspendPolicy, eventos)`. Devuelve `None` si la
/// data está corta/malformada. Cada evento tiene su layout según la `eventKind` (los de ubicación
/// llevan `thread + location`; la excepción suma el objeto lanzado y la *catch location*).
pub fn decode_composite(data: &[u8]) -> Option<(u8, Vec<Event>)> {
    let mut r = Reader::new(data);
    let policy = r.byte()?;
    let count = r.int()?;
    let mut events = Vec::with_capacity(count.max(0) as usize);
    for _ in 0..count {
        let kind = r.byte()?;
        let request_id = r.int()?;
        let event = match kind {
            event_kind::VM_START => {
                let thread = r.id()?;
                Event::VmStart { request_id, thread }
            }
            event_kind::SINGLE_STEP
            | event_kind::BREAKPOINT
            | event_kind::METHOD_ENTRY
            | event_kind::METHOD_EXIT => {
                let thread = r.id()?;
                let location = read_location(&mut r)?;
                located_event(kind, request_id, thread, location)
            }
            event_kind::EXCEPTION => {
                let thread = r.id()?;
                let location = read_location(&mut r)?;
                let _exc_tag = r.byte()?; // tag del objeto ('L')
                let exception = r.id()?;
                let _catch = read_location(&mut r)?; // catch location (la ignoramos por ahora)
                Event::Exception { request_id, thread, location, exception }
            }
            _ => Event::Other { request_id, kind },
        };
        events.push(event);
    }
    Some((policy, events))
}

/// Decodifica un **valor tipado** JDWP (`tag` + bytes). `None` si el tag no se reconoce o falta data.
pub fn decode_value(r: &mut Reader) -> Option<Value> {
    let value = match r.byte()? {
        value_tag::INT => Value::Int(r.int()?),
        value_tag::LONG => Value::Long(r.long()?),
        value_tag::FLOAT => Value::Float(f32::from_bits(r.int()? as u32)),
        value_tag::DOUBLE => Value::Double(f64::from_bits(r.long()? as u64)),
        value_tag::OBJECT => Value::Object(r.id()?),
        _ => return None,
    };
    Some(value)
}

/// Lee una `location` JDWP: `typeTag · classID · methodID · index`.
fn read_location(r: &mut Reader) -> Option<Location> {
    let _tag = r.byte()?;
    let _class = r.id()?;
    let method = r.id()?;
    let index = r.long()? as u64;
    Some(Location { method, index })
}

/// Construye el `Event` de una `eventKind` con ubicación.
fn located_event(kind: u8, request_id: i32, thread: u64, location: Location) -> Event {
    match kind {
        event_kind::BREAKPOINT => Event::Breakpoint { request_id, thread, location },
        event_kind::SINGLE_STEP => Event::SingleStep { request_id, thread, location },
        event_kind::METHOD_ENTRY => Event::MethodEntry { request_id, thread, location },
        event_kind::METHOD_EXIT => Event::MethodExit { request_id, thread, location },
        _ => Event::Other { request_id, kind },
    }
}

// ---- la conexión (mirror de VirtualMachine) -----------------------------------------------------

/// La conexión a una VM remota — *mirror* de `com.sun.jdi.VirtualMachine`. Genérica sobre el transporte
/// (`Read + Write`): un `TcpStream` en producción, un stream en memoria en los tests. Lleva el contador
/// de `id` de paquete, el buffer de bytes a medio leer, y una **cola de eventos** ya recibidos (los
/// `Composite` que llegaron mientras esperábamos la respuesta de un comando se guardan acá).
pub struct Vm<S: Read + Write> {
    stream: S,
    next_id: u32,
    buf: Vec<u8>,
    events: VecDeque<Event>,
}

impl<S: Read + Write> Vm<S> {
    /// Se conecta a la VM remota: hace el **handshake del lado cliente** (manda [`HANDSHAKE`], espera el
    /// eco) sobre un stream ya abierto (TCP conectado, etc.).
    pub fn attach(mut stream: S) -> io::Result<Self> {
        stream.write_all(HANDSHAKE)?;
        stream.flush()?;
        let mut echo = [0u8; HANDSHAKE.len()];
        stream.read_exact(&mut echo)?;
        if echo != *HANDSHAKE {
            return Err(io::Error::new(io::ErrorKind::InvalidData, "handshake JDWP inválido"));
        }
        Ok(Vm { stream, next_id: 1, buf: Vec::new(), events: VecDeque::new() })
    }

    /// `VirtualMachine.Version` — la descripción de la VM remota (el primer string de la respuesta).
    pub fn version(&mut self) -> io::Result<String> {
        let id = self.send(command_set::VIRTUAL_MACHINE, vm_command::VERSION, Vec::new())?;
        let reply = self.recv_reply(id)?;
        Reader::new(&reply.data)
            .string()
            .ok_or_else(|| io::Error::new(io::ErrorKind::InvalidData, "Version sin descripción"))
    }

    /// `VirtualMachine.Resume` — suelta la VM (sigue hasta el próximo evento que suspenda).
    pub fn resume(&mut self) -> io::Result<()> {
        let id = self.send(command_set::VIRTUAL_MACHINE, vm_command::RESUME, Vec::new())?;
        self.recv_reply(id)?;
        Ok(())
    }

    /// `VirtualMachine.Suspend` — frena la VM.
    pub fn suspend(&mut self) -> io::Result<()> {
        let id = self.send(command_set::VIRTUAL_MACHINE, vm_command::SUSPEND, Vec::new())?;
        self.recv_reply(id)?;
        Ok(())
    }

    /// Pide un **breakpoint** en `location` con la política de suspensión dada (mirror de
    /// `EventRequestManager.createBreakpointRequest`). Devuelve el `EventRequest` con su `requestID`.
    pub fn set_breakpoint(&mut self, location: Location, suspend_policy: u8) -> io::Result<EventRequest> {
        self.event_request(event_kind::BREAKPOINT, suspend_policy, Some(location))
    }

    /// Pide un **single-step** (un evento antes de cada opcode) — mirror de `createStepRequest`.
    pub fn set_step_request(&mut self, suspend_policy: u8) -> io::Result<EventRequest> {
        self.event_request(event_kind::SINGLE_STEP, suspend_policy, None)
    }

    /// El envío común de un `EventRequest.Set` y la lectura de su `requestID`.
    fn event_request(
        &mut self,
        kind: u8,
        suspend_policy: u8,
        location: Option<Location>,
    ) -> io::Result<EventRequest> {
        let data = encode_event_set(kind, suspend_policy, location);
        let id = self.send(command_set::EVENT_REQUEST, event_request_command::SET, data)?;
        let reply = self.recv_reply(id)?;
        let req_id = Reader::new(&reply.data)
            .int()
            .ok_or_else(|| io::Error::new(io::ErrorKind::InvalidData, "Set sin requestID"))?;
        Ok(EventRequest { id: req_id, kind })
    }

    /// `EventRequest.Clear` — da de baja un pedido de evento.
    pub fn clear(&mut self, request: EventRequest) -> io::Result<()> {
        let mut w = Writer::new();
        w.byte(request.kind);
        w.int(request.id);
        let id = self.send(command_set::EVENT_REQUEST, event_request_command::CLEAR, w.finish())?;
        self.recv_reply(id)?;
        Ok(())
    }

    /// `VirtualMachine.AllThreads` — los hilos de la VM remota (mirror de `allThreads()`).
    pub fn all_threads(&mut self) -> io::Result<Vec<u64>> {
        let id = self.send(command_set::VIRTUAL_MACHINE, vm_command::ALL_THREADS, Vec::new())?;
        let reply = self.recv_reply(id)?;
        let mut r = Reader::new(&reply.data);
        let count = r.int().ok_or_else(short)?;
        let mut threads = Vec::with_capacity(count.max(0) as usize);
        for _ in 0..count {
            threads.push(r.id().ok_or_else(short)?);
        }
        Ok(threads)
    }

    /// `ThreadReference.Name` — el nombre de un hilo.
    pub fn thread_name(&mut self, thread: u64) -> io::Result<String> {
        let mut w = Writer::new();
        w.id(thread);
        let id = self.send(command_set::THREAD_REFERENCE, thread_command::NAME, w.finish())?;
        let reply = self.recv_reply(id)?;
        Reader::new(&reply.data).string().ok_or_else(short)
    }

    /// `ThreadReference.FrameCount` — cuántos frames tiene la pila del hilo.
    pub fn frame_count(&mut self, thread: u64) -> io::Result<i32> {
        let mut w = Writer::new();
        w.id(thread);
        let id = self.send(command_set::THREAD_REFERENCE, thread_command::FRAME_COUNT, w.finish())?;
        let reply = self.recv_reply(id)?;
        Reader::new(&reply.data).int().ok_or_else(short)
    }

    /// `ThreadReference.Frames` — la pila entera del hilo, de arriba (frame actual) hacia abajo (mirror
    /// de `frames()`).
    pub fn frames(&mut self, thread: u64) -> io::Result<Vec<StackFrame>> {
        let mut w = Writer::new();
        w.id(thread);
        w.int(0); // startFrame
        w.int(-1); // length = todos
        let id = self.send(command_set::THREAD_REFERENCE, thread_command::FRAMES, w.finish())?;
        let reply = self.recv_reply(id)?;
        let mut r = Reader::new(&reply.data);
        let count = r.int().ok_or_else(short)?;
        let mut frames = Vec::with_capacity(count.max(0) as usize);
        for _ in 0..count {
            let frame_id = r.id().ok_or_else(short)?;
            let location = read_location(&mut r).ok_or_else(short)?;
            frames.push(StackFrame { id: frame_id, location });
        }
        Ok(frames)
    }

    /// `StackFrame.GetValues` — lee un local del frame por su `slot` (mirror de `getValue`). Lleva el
    /// `thread` **y** el `frame` (un frameID solo tiene sentido con su hilo). El `sig` es el tag del tipo
    /// esperado (el servidor devuelve el valor tageado igual).
    pub fn get_value(&mut self, thread: u64, frame: u64, slot: i32, sig: u8) -> io::Result<Value> {
        let mut w = Writer::new();
        w.id(thread);
        w.id(frame);
        w.int(1); // un slot
        w.int(slot);
        w.byte(sig);
        let id = self.send(command_set::STACK_FRAME, frame_command::GET_VALUES, w.finish())?;
        let reply = self.recv_reply(id)?;
        let mut r = Reader::new(&reply.data);
        let _count = r.int().ok_or_else(short)?;
        decode_value(&mut r).ok_or_else(short)
    }

    /// `VirtualMachine.AllClasses` — todas las clases cargadas en la VM remota (mirror de `allClasses`).
    pub fn all_classes(&mut self) -> io::Result<Vec<ClassInfo>> {
        let id = self.send(command_set::VIRTUAL_MACHINE, vm_command::ALL_CLASSES, Vec::new())?;
        let reply = self.recv_reply(id)?;
        let mut r = Reader::new(&reply.data);
        let count = r.int().ok_or_else(short)?;
        let mut classes = Vec::with_capacity(count.max(0) as usize);
        for _ in 0..count {
            let _tag = r.byte().ok_or_else(short)?;
            let id = r.id().ok_or_else(short)?;
            let signature = r.string().ok_or_else(short)?;
            let status = r.int().ok_or_else(short)?;
            classes.push(ClassInfo { id, signature, status });
        }
        Ok(classes)
    }

    /// `VirtualMachine.ClassesBySignature` — los `referenceTypeID` de las clases con esa firma JVM
    /// (mirror de `classesByName`, pero por firma `L…;`).
    pub fn classes_by_signature(&mut self, signature: &str) -> io::Result<Vec<u64>> {
        let mut w = Writer::new();
        w.string(signature);
        let id = self.send(command_set::VIRTUAL_MACHINE, vm_command::CLASSES_BY_SIGNATURE, w.finish())?;
        let reply = self.recv_reply(id)?;
        let mut r = Reader::new(&reply.data);
        let count = r.int().ok_or_else(short)?;
        let mut ids = Vec::with_capacity(count.max(0) as usize);
        for _ in 0..count {
            let _tag = r.byte().ok_or_else(short)?;
            ids.push(r.id().ok_or_else(short)?);
            let _status = r.int().ok_or_else(short)?;
        }
        Ok(ids)
    }

    /// `ReferenceType.Methods` — los métodos declarados por una clase (mirror de `methods()`).
    pub fn methods(&mut self, class_id: u64) -> io::Result<Vec<MethodInfo>> {
        let mut w = Writer::new();
        w.id(class_id);
        let id = self.send(command_set::REFERENCE_TYPE, reference_type_command::METHODS, w.finish())?;
        let reply = self.recv_reply(id)?;
        let mut r = Reader::new(&reply.data);
        let count = r.int().ok_or_else(short)?;
        let mut methods = Vec::with_capacity(count.max(0) as usize);
        for _ in 0..count {
            let method_id = r.id().ok_or_else(short)?;
            let name = r.string().ok_or_else(short)?;
            let signature = r.string().ok_or_else(short)?;
            let mod_bits = r.int().ok_or_else(short)?;
            methods.push(MethodInfo { id: method_id, name, signature, mod_bits });
        }
        Ok(methods)
    }

    /// `Method.LineTable` — los pares `(índice de bytecode, línea)` de un método (mirror de
    /// `allLineLocations`). La pieza para poner un breakpoint por línea.
    pub fn line_table(&mut self, class_id: u64, method_id: u64) -> io::Result<Vec<(u64, i32)>> {
        let mut w = Writer::new();
        w.id(class_id);
        w.id(method_id);
        let id = self.send(command_set::METHOD, method_command::LINE_TABLE, w.finish())?;
        let reply = self.recv_reply(id)?;
        let mut r = Reader::new(&reply.data);
        let _start = r.long().ok_or_else(short)?;
        let _end = r.long().ok_or_else(short)?;
        let count = r.int().ok_or_else(short)?;
        let mut lines = Vec::with_capacity(count.max(0) as usize);
        for _ in 0..count {
            let index = r.long().ok_or_else(short)? as u64;
            let line = r.int().ok_or_else(short)?;
            lines.push((index, line));
        }
        Ok(lines)
    }

    /// Espera y devuelve el **próximo evento** que la VM empuje (mirror de `EventQueue.remove`). Drena
    /// primero los eventos ya encolados; si no hay, lee del stream hasta que llegue un `Composite`.
    pub fn next_event(&mut self) -> io::Result<Event> {
        loop {
            if let Some(event) = self.events.pop_front() {
                return Ok(event);
            }
            match self.read_packet()? {
                Packet::Command(cmd) => self.stash_events(cmd),
                Packet::Reply(_) => {} // una respuesta perdida mientras esperábamos eventos: se ignora
            }
        }
    }

    // ---- transporte ----

    /// Manda un comando y devuelve el `id` de paquete que le asignó (para casar la respuesta).
    fn send(&mut self, command_set: u8, command: u8, data: Vec<u8>) -> io::Result<u32> {
        let id = self.next_id;
        self.next_id += 1;
        let packet = Packet::Command(CommandPacket { id, command_set, command, data });
        self.stream.write_all(&packet.encode())?;
        self.stream.flush()?;
        Ok(id)
    }

    /// Lee paquetes hasta la **respuesta** con el `id` esperado; los `Composite` que lleguen en el medio
    /// se **encolan** como eventos (no se pierden).
    fn recv_reply(&mut self, id: u32) -> io::Result<ReplyPacket> {
        loop {
            match self.read_packet()? {
                Packet::Reply(reply) if reply.id == id => return Ok(reply),
                Packet::Reply(_) => {} // respuesta de otro id (no debería pasar en secuencial): se ignora
                Packet::Command(cmd) => self.stash_events(cmd),
            }
        }
    }

    /// Si el comando entrante es un `Event.Composite`, decodifica sus eventos y los encola.
    fn stash_events(&mut self, cmd: CommandPacket) {
        if cmd.command_set == command_set::EVENT && cmd.command == EVENT_COMPOSITE {
            if let Some((_policy, events)) = decode_composite(&cmd.data) {
                self.events.extend(events);
            }
        }
    }

    /// Lee **un** paquete completo del stream (acumulando en `buf` hasta que el framing lo complete).
    fn read_packet(&mut self) -> io::Result<Packet> {
        let mut chunk = [0u8; 4096];
        loop {
            if let Some(len) = Packet::framed_len(&self.buf) {
                let packet = Packet::decode(&self.buf).expect("framed_len ⇒ hay un paquete entero");
                self.buf.drain(..len);
                return Ok(packet);
            }
            let n = self.stream.read(&mut chunk)?;
            if n == 0 {
                return Err(io::Error::new(io::ErrorKind::UnexpectedEof, "la VM cerró la conexión"));
            }
            self.buf.extend_from_slice(&chunk[..n]);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use super::super::jdwp::{suspend_policy, JdwpSession};

    /// Un **servidor JDWP en memoria**, síncrono: cada vez que el cliente *escribe* (un comando), lo
    /// atendemos con la `JdwpSession` real y dejamos la respuesta lista para que el cliente la *lea*.
    /// Así se prueba el cliente JDI contra el servidor real sin sockets ni hilos. (No empuja eventos:
    /// eso lo hace el bridge, que necesita una VM; los eventos se prueban aparte.)
    struct Loopback {
        session: JdwpSession,
        outbox: VecDeque<u8>,
        inbox: Vec<u8>,
        handshook: bool,
    }
    impl Loopback {
        fn new() -> Self {
            Loopback {
                session: JdwpSession::new(),
                outbox: VecDeque::new(),
                inbox: Vec::new(),
                handshook: false,
            }
        }
    }
    impl Write for Loopback {
        fn write(&mut self, bytes: &[u8]) -> io::Result<usize> {
            self.inbox.extend_from_slice(bytes);
            if !self.handshook {
                if self.inbox.len() < HANDSHAKE.len() {
                    return Ok(bytes.len());
                }
                let head: Vec<u8> = self.inbox.drain(..HANDSHAKE.len()).collect();
                assert_eq!(head, HANDSHAKE, "el cliente debe mandar el handshake primero");
                self.outbox.extend(HANDSHAKE);
                self.handshook = true;
            }
            while let Some(len) = Packet::framed_len(&self.inbox) {
                let packet = Packet::decode(&self.inbox).unwrap();
                self.inbox.drain(..len);
                if let Packet::Command(cmd) = packet {
                    let reply = self.session.handle(&cmd);
                    self.outbox.extend(Packet::Reply(reply).encode());
                }
            }
            Ok(bytes.len())
        }
        fn flush(&mut self) -> io::Result<()> {
            Ok(())
        }
    }
    impl Read for Loopback {
        fn read(&mut self, out: &mut [u8]) -> io::Result<usize> {
            let n = out.len().min(self.outbox.len());
            for slot in out.iter_mut().take(n) {
                *slot = self.outbox.pop_front().unwrap();
            }
            Ok(n)
        }
    }

    /// Un stream de lectura pura: sirve bytes precargados (para inyectar un `Composite` y probar
    /// `next_event`). La escritura se descarta.
    struct ReadOnly {
        data: std::io::Cursor<Vec<u8>>,
    }
    impl Read for ReadOnly {
        fn read(&mut self, b: &mut [u8]) -> io::Result<usize> {
            self.data.read(b)
        }
    }
    impl Write for ReadOnly {
        fn write(&mut self, b: &[u8]) -> io::Result<usize> {
            Ok(b.len())
        }
        fn flush(&mut self) -> io::Result<()> {
            Ok(())
        }
    }

    #[test]
    fn encode_set_round_trips_through_the_server_parser() {
        // Lo que el cliente arma para un breakpoint debe ser lo que el servidor entiende.
        let data = encode_event_set(event_kind::BREAKPOINT, suspend_policy::ALL, Some(Location { method: 5, index: 12 }));
        let mut session = JdwpSession::new();
        let reply = session.handle(&CommandPacket {
            id: 1,
            command_set: command_set::EVENT_REQUEST,
            command: event_request_command::SET,
            data,
        });
        assert_eq!(reply.error, 0);
        assert_eq!(session.requests().len(), 1);
        let req = &session.requests()[0];
        assert_eq!(req.kind, event_kind::BREAKPOINT);
        assert_eq!(req.location, Some((5, 12)), "el servidor sacó la posición del LocationOnly");
    }

    #[test]
    fn decode_composite_parses_a_breakpoint_event() {
        // Un Composite con un breakpoint (mismo layout que arma el bridge).
        let mut w = Writer::new();
        w.byte(suspend_policy::ALL);
        w.int(1);
        w.byte(event_kind::BREAKPOINT);
        w.int(7); // requestID
        w.id(3); // thread
        w.byte(TYPE_TAG_CLASS);
        w.id(0); // class
        w.id(5); // method
        w.long(12); // index
        let (policy, events) = decode_composite(&w.finish()).unwrap();
        assert_eq!(policy, suspend_policy::ALL);
        assert_eq!(events.len(), 1);
        assert_eq!(
            events[0],
            Event::Breakpoint { request_id: 7, thread: 3, location: Location { method: 5, index: 12 } }
        );
    }

    #[test]
    fn the_client_drives_a_real_session_in_memory() {
        // Cliente JDI ↔ JdwpSession servidor, todo en memoria (attach + comandos + reply).
        let mut vm = Vm::attach(Loopback::new()).unwrap();
        assert_eq!(vm.version().unwrap(), "KajiVM JDWP (Hito I3)");

        let req = vm.set_breakpoint(Location { method: 5, index: 0 }, suspend_policy::ALL).unwrap();
        assert_eq!(req.id, 1, "primer requestID");
        assert_eq!(req.kind, event_kind::BREAKPOINT);

        let step = vm.set_step_request(suspend_policy::ALL).unwrap();
        assert_eq!(step.id, 2);

        vm.clear(req).unwrap();
        vm.resume().unwrap(); // no debe colgar: reply vacío
    }

    #[test]
    fn next_event_yields_typed_events_from_a_composite() {
        // Un stream que, tras el handshake, entrega un Composite con dos eventos.
        let mut bytes = Vec::new();
        bytes.extend(HANDSHAKE);
        let mut w = Writer::new();
        w.byte(suspend_policy::ALL);
        w.int(2);
        // evento 1: single-step
        w.byte(event_kind::SINGLE_STEP);
        w.int(1);
        w.id(0);
        w.byte(TYPE_TAG_CLASS);
        w.id(0);
        w.id(5);
        w.long(0);
        // evento 2: breakpoint
        w.byte(event_kind::BREAKPOINT);
        w.int(2);
        w.id(0);
        w.byte(TYPE_TAG_CLASS);
        w.id(0);
        w.id(5);
        w.long(3);
        let composite =
            Packet::Command(CommandPacket { id: 9, command_set: command_set::EVENT, command: EVENT_COMPOSITE, data: w.finish() });
        bytes.extend(composite.encode());

        let mut vm = Vm::attach(ReadOnly { data: std::io::Cursor::new(bytes) }).unwrap();
        // el primer next_event decodifica el Composite y devuelve el 1º evento; el 2º sale de la cola.
        match vm.next_event().unwrap() {
            Event::SingleStep { request_id, location, .. } => {
                assert_eq!(request_id, 1);
                assert_eq!(location, Location { method: 5, index: 0 });
            }
            other => panic!("esperaba single-step, vino {other:?}"),
        }
        match vm.next_event().unwrap() {
            Event::Breakpoint { request_id, location, .. } => {
                assert_eq!(request_id, 2);
                assert_eq!(location, Location { method: 5, index: 3 });
            }
            other => panic!("esperaba breakpoint, vino {other:?}"),
        }
    }
}
