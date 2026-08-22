//! **JDWP** — Java Debug Wire Protocol (Hito I3): el **protocolo de cable** entre el debugger
//! (front-end) y la VM (debuggee). Es la capa del medio del stack JPDA: `JVMTI → JDWP → JDI → jdb`.
//!
//! Este módulo es el **codec**: el formato de paquetes y la serialización tipada de su *data*. Es
//! **puro** —no hace I/O ni toca la VM—, así que se testea de punta a punta. Encima irán el
//! **transporte** (un socket TCP; la VM como servidor `dt_socket`), los **command handlers** (los
//! ~15 *command sets*) y el **bridge JVMTI↔JDWP** (que traduce un evento de [`super::jvmti`] a un
//! *composite event packet* y un comando entrante a llamadas al `JvmtiEnv`).
//!
//! ## El protocolo, en dos piezas
//!
//! **1. Handshake** — antes de nada, los dos peers intercambian 14 bytes ASCII ([`HANDSHAKE`]).
//!
//! **2. Paquetes** — todo lo demás son *paquetes* con un header de **11 bytes**:
//!
//! ```text
//!   length (u32, incluye el header)  ·  id (u32, para casar reply↔command)  ·  flags (u8)
//!   luego 2 bytes:  command-set (u8) + command (u8)   [si flags & 0x80 == 0 → COMANDO]
//!                   error-code (u16)                   [si flags & 0x80 != 0 → RESPUESTA]
//!   luego (length − 11) bytes de data, específica del comando.
//! ```

use std::io::{self, Read, Write};
use std::net::TcpListener;

/// El handshake inicial: cliente y servidor intercambian **exactamente** estos 14 bytes antes de
/// cualquier paquete (el cliente los manda, el servidor los repite).
pub const HANDSHAKE: &[u8] = b"JDWP-Handshake";

/// El bit de `flags` que marca un paquete como **respuesta** (si está apagado, es un comando).
const FLAG_REPLY: u8 = 0x80;

/// El header de un paquete: siempre 11 bytes (length + id + flags + 2 bytes de discriminante).
const HEADER_LEN: usize = 11;

/// Un paquete de **comando** — un peer le pide algo al otro (`command_set`/`command` lo identifican).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct CommandPacket {
    pub id: u32,
    pub command_set: u8,
    pub command: u8,
    pub data: Vec<u8>,
}

/// Un paquete de **respuesta** — la contestación a un comando, con **su mismo `id`** y un código de
/// error (`0` = sin error).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ReplyPacket {
    pub id: u32,
    pub error: u16,
    pub data: Vec<u8>,
}

/// Un paquete, comando o respuesta (se distinguen por el bit `FLAG_REPLY`).
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Packet {
    Command(CommandPacket),
    Reply(ReplyPacket),
}

impl Packet {
    /// Serializa el paquete a bytes (header de 11 + data), listo para el socket.
    pub fn encode(&self) -> Vec<u8> {
        let (id, flags, discriminant, data): (u32, u8, [u8; 2], &[u8]) = match self {
            Packet::Command(c) => (c.id, 0, [c.command_set, c.command], &c.data),
            Packet::Reply(r) => (r.id, FLAG_REPLY, r.error.to_be_bytes(), &r.data),
        };
        let len = (HEADER_LEN + data.len()) as u32;
        let mut out = Vec::with_capacity(len as usize);
        out.extend_from_slice(&len.to_be_bytes());
        out.extend_from_slice(&id.to_be_bytes());
        out.push(flags);
        out.extend_from_slice(&discriminant);
        out.extend_from_slice(data);
        out
    }

    /// Parsea **un** paquete del frente de `bytes`. Devuelve `None` si el buffer todavía no tiene el
    /// paquete entero (header incompleto, o `length` mayor que lo disponible) — el llamador reintenta
    /// tras leer más del socket. El header dice el `length` total, así que el framing es exacto.
    pub fn decode(bytes: &[u8]) -> Option<Packet> {
        if bytes.len() < HEADER_LEN {
            return None;
        }
        let len = u32::from_be_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]) as usize;
        if len < HEADER_LEN || bytes.len() < len {
            return None;
        }
        let id = u32::from_be_bytes([bytes[4], bytes[5], bytes[6], bytes[7]]);
        let flags = bytes[8];
        let data = bytes[HEADER_LEN..len].to_vec();
        if flags & FLAG_REPLY != 0 {
            let error = u16::from_be_bytes([bytes[9], bytes[10]]);
            Some(Packet::Reply(ReplyPacket { id, error, data }))
        } else {
            Some(Packet::Command(CommandPacket { id, command_set: bytes[9], command: bytes[10], data }))
        }
    }

    /// Cuántos bytes ocupa el paquete que arranca en `bytes`, si ya está completo (para consumir del
    /// buffer del socket lo justo tras un [`decode`](Self::decode)).
    pub fn framed_len(bytes: &[u8]) -> Option<usize> {
        if bytes.len() < 4 {
            return None;
        }
        let len = u32::from_be_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]) as usize;
        (len >= HEADER_LEN && bytes.len() >= len).then_some(len)
    }
}

/// Constructor de la **data** de un paquete: escribe los tipos JDWP en **big-endian** (el orden de
/// red). Los *ids* (object/thread/method/…) van a 8 bytes —el tamaño que anunciamos en
/// `VirtualMachine.IDSizes`—; un string es `u32` de longitud + los bytes UTF-8.
#[derive(Default)]
pub struct Writer {
    buf: Vec<u8>,
}

impl Writer {
    pub fn new() -> Self {
        Writer { buf: Vec::new() }
    }
    pub fn byte(&mut self, v: u8) -> &mut Self {
        self.buf.push(v);
        self
    }
    pub fn boolean(&mut self, v: bool) -> &mut Self {
        self.buf.push(v as u8);
        self
    }
    pub fn int(&mut self, v: i32) -> &mut Self {
        self.buf.extend_from_slice(&v.to_be_bytes());
        self
    }
    pub fn long(&mut self, v: i64) -> &mut Self {
        self.buf.extend_from_slice(&v.to_be_bytes());
        self
    }
    /// Un *id* JDWP (object/thread/method/reference-type…), 8 bytes.
    pub fn id(&mut self, v: u64) -> &mut Self {
        self.buf.extend_from_slice(&v.to_be_bytes());
        self
    }
    /// Un string JDWP: longitud `u32` + los bytes UTF-8 (sin terminador).
    pub fn string(&mut self, s: &str) -> &mut Self {
        self.buf.extend_from_slice(&(s.len() as u32).to_be_bytes());
        self.buf.extend_from_slice(s.as_bytes());
        self
    }
    /// Devuelve la data acumulada (y deja el writer vacío, reusable). Cierra la cadena del builder.
    pub fn finish(&mut self) -> Vec<u8> {
        std::mem::take(&mut self.buf)
    }
}

/// Lector de la **data** de un paquete: un cursor sobre `&[u8]` que va sacando tipos JDWP. Cada
/// lectura devuelve `None` si no quedan bytes (data corta/malformada), sin paniquear.
pub struct Reader<'a> {
    data: &'a [u8],
    pos: usize,
}

impl<'a> Reader<'a> {
    pub fn new(data: &'a [u8]) -> Self {
        Reader { data, pos: 0 }
    }
    fn take(&mut self, n: usize) -> Option<&'a [u8]> {
        let end = self.pos.checked_add(n)?;
        let slice = self.data.get(self.pos..end)?;
        self.pos = end;
        Some(slice)
    }
    pub fn byte(&mut self) -> Option<u8> {
        self.take(1).map(|b| b[0])
    }
    pub fn boolean(&mut self) -> Option<bool> {
        self.byte().map(|b| b != 0)
    }
    pub fn int(&mut self) -> Option<i32> {
        self.take(4).map(|b| i32::from_be_bytes([b[0], b[1], b[2], b[3]]))
    }
    pub fn long(&mut self) -> Option<i64> {
        self.take(8).map(|b| i64::from_be_bytes(b.try_into().unwrap()))
    }
    pub fn id(&mut self) -> Option<u64> {
        self.take(8).map(|b| u64::from_be_bytes(b.try_into().unwrap()))
    }
    pub fn string(&mut self) -> Option<String> {
        let len = self.take(4).map(|b| u32::from_be_bytes([b[0], b[1], b[2], b[3]]) as usize)?;
        let bytes = self.take(len)?;
        Some(String::from_utf8_lossy(bytes).into_owned())
    }
    /// ¿Quedan bytes sin leer? (Para chequear que un comando consumió toda su data.)
    pub fn remaining(&self) -> usize {
        self.data.len().saturating_sub(self.pos)
    }
}

// ---- Command handlers ----------------------------------------------------------------------
//
// Un handler toma un `CommandPacket` y produce un `ReplyPacket`. La parte de **protocolo** (parsear
// la data, armar la respuesta) es pura y vive acá, contra una `JdwpSession` que lleva el estado del
// lado debugger. La parte que **toca la VM de verdad** (setear el breakpoint en el intérprete, leer
// valores de un frame) la cablea el bridge JVMTI↔JDWP más adelante — acá se registra la intención.

/// Los *command sets* (el primer byte del discriminante de un comando). Solo los que manejamos.
pub mod command_set {
    /// Comandos sobre la VM entera (versión, tamaños de id, suspend/resume, hilos…).
    pub const VIRTUAL_MACHINE: u8 = 1;
    /// Registrar/limpiar pedidos de evento (breakpoints, single-step, watchpoints…).
    pub const EVENT_REQUEST: u8 = 15;
    /// Comandos sobre un **tipo de referencia** (clase): firma, métodos, archivo fuente…
    pub const REFERENCE_TYPE: u8 = 2;
    /// Comandos sobre un **tipo clase** (`ClassType`): su superclase, escritura de estáticos…
    pub const CLASS_TYPE: u8 = 3;
    /// Comandos sobre un **método**: la tabla de líneas, la de variables…
    pub const METHOD: u8 = 6;
    /// Comandos sobre un **objeto** del heap (su tipo real, leer sus campos).
    pub const OBJECT_REFERENCE: u8 = 9;
    /// Comandos sobre un **hilo** (nombre, frames, cantidad de frames…).
    pub const THREAD_REFERENCE: u8 = 11;
    /// Comandos sobre un **grupo de hilos** (nombre, padre, hijos).
    pub const THREAD_GROUP_REFERENCE: u8 = 12;
    /// Comandos sobre un **frame** de la pila (leer/escribir sus locales).
    pub const STACK_FRAME: u8 = 16;
    /// **Eventos** — el único command-set que va **VM → cliente** (un `Composite` con los eventos que
    /// dispararon). El bridge JVMTI↔JDWP lo usa para empujar un breakpoint/step/exception.
    pub const EVENT: u8 = 64;
}

/// Los comandos de `VirtualMachine` que manejamos (segundo byte del discriminante).
pub mod vm_command {
    pub const VERSION: u8 = 1;
    pub const CLASSES_BY_SIGNATURE: u8 = 2;
    pub const ALL_CLASSES: u8 = 3;
    pub const ALL_THREADS: u8 = 4;
    pub const TOP_LEVEL_THREAD_GROUPS: u8 = 5;
    pub const DISPOSE: u8 = 6;
    pub const ID_SIZES: u8 = 7;
    pub const SUSPEND: u8 = 8;
    pub const RESUME: u8 = 9;
    pub const CAPABILITIES: u8 = 12;
    pub const CLASS_PATHS: u8 = 13;
    pub const CAPABILITIES_NEW: u8 = 17;
    /// Como `AllClasses` pero cada clase suma su firma genérica — **la que usa el JDI real** (Java 5+).
    pub const ALL_CLASSES_WITH_GENERIC: u8 = 20;
}

/// Los comandos de `ReferenceType` (una clase).
pub mod reference_type_command {
    pub const SIGNATURE: u8 = 1;
    pub const FIELDS: u8 = 4;
    pub const METHODS: u8 = 5;
    pub const SOURCE_FILE: u8 = 7;
    pub const INTERFACES: u8 = 10;
    /// Variantes **con firma genérica** — las que el JDI real emite en vez de las planas.
    pub const SIGNATURE_WITH_GENERIC: u8 = 13;
    pub const FIELDS_WITH_GENERIC: u8 = 14;
    pub const METHODS_WITH_GENERIC: u8 = 15;
}

/// Los comandos de `ClassType`.
pub mod class_type_command {
    pub const SUPERCLASS: u8 = 1;
}

/// Los comandos de `ObjectReference`.
pub mod object_command {
    /// El tipo real del objeto (para leer sus campos con el layout correcto).
    pub const REFERENCE_TYPE: u8 = 1;
    /// Leer campos de instancia del objeto.
    pub const GET_VALUES: u8 = 2;
}

/// Los comandos de `Method`.
pub mod method_command {
    pub const LINE_TABLE: u8 = 1;
    pub const VARIABLE_TABLE: u8 = 2;
    /// La variante con firma genérica — la que emite el JDI real para `locals`.
    pub const VARIABLE_TABLE_WITH_GENERIC: u8 = 5;
}

/// El `refTypeTag` de un tipo: `CLASS` (los otros son INTERFACE=2, ARRAY=3).
pub const REF_TYPE_TAG_CLASS: u8 = 1;

/// El *status* de una clase cargada: `VERIFIED | PREPARED | INITIALIZED` (los tres bits bajos).
pub const CLASS_STATUS_READY: i32 = 1 | 2 | 4;

/// Los comandos de `ThreadReference`.
pub mod thread_command {
    pub const NAME: u8 = 1;
    pub const STATUS: u8 = 4;
    pub const THREAD_GROUP: u8 = 5;
    pub const FRAMES: u8 = 6;
    pub const FRAME_COUNT: u8 = 7;
}

/// El estado de un hilo (`ThreadReference.Status`).
pub mod thread_status {
    pub const RUNNING: i32 = 1;
    /// El bit de que el hilo está suspendido por el debugger.
    pub const SUSPEND_STATUS_SUSPENDED: i32 = 1;
}

/// Los comandos de `ThreadGroupReference`.
pub mod thread_group_command {
    pub const NAME: u8 = 1;
    pub const PARENT: u8 = 2;
    pub const CHILDREN: u8 = 3;
}

/// Los comandos de `StackFrame`.
pub mod frame_command {
    pub const GET_VALUES: u8 = 1;
}

/// Los *tags* de un valor JDWP (el byte que precede a un valor tipado). Solo los que emitimos.
pub mod value_tag {
    pub const INT: u8 = b'I';
    pub const LONG: u8 = b'J';
    pub const FLOAT: u8 = b'F';
    pub const DOUBLE: u8 = b'D';
    /// Un objeto (referencia). En JDWP el tag exacto depende del tipo real; usamos el genérico `L`.
    pub const OBJECT: u8 = b'L';
}

/// Los comandos de `EventRequest`.
pub mod event_request_command {
    pub const SET: u8 = 1;
    pub const CLEAR: u8 = 2;
}

/// El comando de `Event` (VM → cliente): `Composite`, el sobre que agrupa los eventos disparados.
pub const EVENT_COMPOSITE: u8 = 100;

/// El *typeTag* de una ubicación/tipo: `CLASS`. (Lo escribe el `LocationOnly` y el `Composite`.)
pub const TYPE_TAG_CLASS: u8 = 1;

/// Las *suspend policies* de un evento: a quién frena cuando dispara.
pub mod suspend_policy {
    /// No frena a nadie (el evento se empuja y la VM sigue).
    pub const NONE: u8 = 0;
    /// Frena solo al hilo del evento.
    pub const EVENT_THREAD: u8 = 1;
    /// Frena la VM entera.
    pub const ALL: u8 = 2;
}

/// Códigos de error de una respuesta (`0` = sin error).
pub mod error_code {
    pub const NONE: u16 = 0;
    /// No hay información pedida (p.ej. una clase sin atributo `SourceFile`, o un método sin
    /// `LineNumberTable`).
    pub const ABSENT_INFORMATION: u16 = 101;
    /// Un argumento del comando es inválido/estaba corto.
    pub const ILLEGAL_ARGUMENT: u16 = 103;
    /// El comando (o su command-set) no está implementado.
    pub const NOT_IMPLEMENTED: u16 = 99;
}

/// Las clases de evento (`eventKind`), como en `EventRequest.Set`. Espejan las de [`super::jvmti`].
pub mod event_kind {
    pub const SINGLE_STEP: u8 = 1;
    pub const BREAKPOINT: u8 = 2;
    pub const EXCEPTION: u8 = 4;
    pub const FIELD_ACCESS: u8 = 20;
    pub const FIELD_MODIFICATION: u8 = 21;
    pub const METHOD_ENTRY: u8 = 40;
    pub const METHOD_EXIT: u8 = 41;
    /// La VM arrancó — el evento **automático** (requestID 0) que el VM manda al conectarse el
    /// debugger, con el hilo inicial. Un cliente real lo usa para fijar su «hilo actual».
    pub const VM_START: u8 = 90;
}

/// El *modKind* de un modificador `LocationOnly` (acota un evento a una posición de código). El lado
/// servidor lo parsea en `event_request_set`; el cliente JDI ([`super::jdi`]) lo escribe al pedir un
/// breakpoint.
pub const MOD_LOCATION_ONLY: u8 = 7;

/// El *modKind* de un modificador `FieldOnly` (acota un field watchpoint a un `(referenceType, fieldID)`).
pub const MOD_FIELD_ONLY: u8 = 9;

/// Un pedido de evento registrado por el cliente (`EventRequest.Set`), con el `id` que le devolvimos.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct EventRequest {
    pub id: i32,
    pub kind: u8,
    pub suspend_policy: u8,
    /// Para un `BREAKPOINT`: la posición `(methodID, índice de bytecode)` de su modificador
    /// `LocationOnly`. `None` para eventos sin ubicación (method entry/exit, exception…).
    pub location: Option<(u64, u64)>,
    /// Para un `FIELD_ACCESS`/`FIELD_MODIFICATION`: el `(referenceTypeID, fieldID)` de su modificador
    /// `FieldOnly`.
    pub field: Option<(u64, u64)>,
}

/// El estado del lado **debugger** de una conexión JDWP: el contador de `requestID`, los pedidos de
/// evento vivos y si la VM está suspendida. `handle` despacha un comando a su respuesta **sin tocar
/// la VM** (eso lo hace el bridge, que lee esta sesión).
pub struct JdwpSession {
    next_request_id: i32,
    requests: Vec<EventRequest>,
    suspended: bool,
}

impl Default for JdwpSession {
    fn default() -> Self {
        JdwpSession { next_request_id: 1, requests: Vec::new(), suspended: false }
    }
}

impl JdwpSession {
    pub fn new() -> Self {
        Self::default()
    }
    /// Los pedidos de evento vivos (el bridge los consulta para saber qué breakpoints setear).
    pub fn requests(&self) -> &[EventRequest] {
        &self.requests
    }
    /// ¿El cliente pidió suspender la VM?
    pub fn is_suspended(&self) -> bool {
        self.suspended
    }

    /// Despacha un comando entrante a su respuesta. Un `(command_set, command)` no cubierto responde
    /// con `NOT_IMPLEMENTED` (como haría una VM que no soporta ese comando).
    pub fn handle(&mut self, cmd: &CommandPacket) -> ReplyPacket {
        use command_set::*;
        let result = match (cmd.command_set, cmd.command) {
            (VIRTUAL_MACHINE, vm_command::VERSION) => Ok(self.vm_version()),
            (VIRTUAL_MACHINE, vm_command::ID_SIZES) => Ok(self.vm_id_sizes()),
            (VIRTUAL_MACHINE, vm_command::SUSPEND) => {
                self.suspended = true;
                Ok(Vec::new())
            }
            (VIRTUAL_MACHINE, vm_command::RESUME) => {
                self.suspended = false;
                Ok(Vec::new())
            }
            (EVENT_REQUEST, event_request_command::SET) => self.event_request_set(&cmd.data),
            (EVENT_REQUEST, event_request_command::CLEAR) => self.event_request_clear(&cmd.data),
            _ => Err(error_code::NOT_IMPLEMENTED),
        };
        match result {
            Ok(data) => ReplyPacket { id: cmd.id, error: error_code::NONE, data },
            Err(error) => ReplyPacket { id: cmd.id, error, data: Vec::new() },
        }
    }

    /// `VirtualMachine.Version` — descripción + versión del protocolo + versión/nombre de la VM.
    fn vm_version(&self) -> Vec<u8> {
        Writer::new()
            .string("KajiVM JDWP (Hito I3)")
            .int(1) // jdwpMajor
            .int(8) // jdwpMinor
            .string("25")
            .string("KajiVM")
            .finish()
    }

    /// `VirtualMachine.IDSizes` — el tamaño (bytes) de cada id. Todos a 8 (nuestro `Writer::id`).
    fn vm_id_sizes(&self) -> Vec<u8> {
        Writer::new()
            .int(8) // fieldIDSize
            .int(8) // methodIDSize
            .int(8) // objectIDSize
            .int(8) // referenceTypeIDSize
            .int(8) // frameIDSize
            .finish()
    }

    /// `EventRequest.Set` — registra un pedido de evento y devuelve su `requestID`. Parsea `eventKind`,
    /// `suspendPolicy` y los **modificadores**; del `LocationOnly` (el de un breakpoint) saca la
    /// posición. Otros modKind cortan el parseo (primer corte).
    fn event_request_set(&mut self, data: &[u8]) -> Result<Vec<u8>, u16> {
        let mut r = Reader::new(data);
        let kind = r.byte().ok_or(error_code::ILLEGAL_ARGUMENT)?;
        let suspend_policy = r.byte().ok_or(error_code::ILLEGAL_ARGUMENT)?;
        let modifiers = r.int().ok_or(error_code::ILLEGAL_ARGUMENT)?;
        let mut location = None;
        let mut field = None;
        for _ in 0..modifiers {
            match r.byte().ok_or(error_code::ILLEGAL_ARGUMENT)? {
                MOD_LOCATION_ONLY => {
                    let _tag = r.byte().ok_or(error_code::ILLEGAL_ARGUMENT)?;
                    let _class = r.id().ok_or(error_code::ILLEGAL_ARGUMENT)?;
                    let method = r.id().ok_or(error_code::ILLEGAL_ARGUMENT)?;
                    let index = r.long().ok_or(error_code::ILLEGAL_ARGUMENT)? as u64;
                    location = Some((method, index));
                }
                MOD_FIELD_ONLY => {
                    let ref_type = r.id().ok_or(error_code::ILLEGAL_ARGUMENT)?;
                    let field_id = r.id().ok_or(error_code::ILLEGAL_ARGUMENT)?;
                    field = Some((ref_type, field_id));
                }
                // Otros modificadores todavía no se parsean: se corta (el requestID igual se aloca).
                _ => break,
            }
        }
        let id = self.next_request_id;
        self.next_request_id += 1;
        self.requests.push(EventRequest { id, kind, suspend_policy, location, field });
        Ok(Writer::new().int(id).finish())
    }

    /// `EventRequest.Clear` — saca un pedido por `(eventKind, requestID)`.
    fn event_request_clear(&mut self, data: &[u8]) -> Result<Vec<u8>, u16> {
        let mut r = Reader::new(data);
        let _kind = r.byte().ok_or(error_code::ILLEGAL_ARGUMENT)?;
        let id = r.int().ok_or(error_code::ILLEGAL_ARGUMENT)?;
        self.requests.retain(|req| req.id != id);
        Ok(Vec::new())
    }
}

// ---- Transporte (socket) -------------------------------------------------------------------
//
// La VM es el **servidor** `dt_socket` (server=y): escucha, acepta una conexión, hace el handshake y
// sirve paquetes. El core (`handshake`/`serve`) es genérico sobre `Read`/`Write` —no sabe de sockets—
// para poder testearlo con un buffer en memoria; `listen` es la capa de TCP.

/// El **handshake** del lado servidor: lee 14 bytes, verifica que sean [`HANDSHAKE`] y los repite.
/// Falla si no matchean (o si el stream corta antes).
pub fn handshake<R: Read, W: Write>(reader: &mut R, writer: &mut W) -> io::Result<()> {
    let mut buf = [0u8; HANDSHAKE.len()];
    reader.read_exact(&mut buf)?;
    if buf != *HANDSHAKE {
        return Err(io::Error::new(io::ErrorKind::InvalidData, "handshake JDWP inválido"));
    }
    writer.write_all(HANDSHAKE)?;
    writer.flush()
}

/// El **loop de servicio** tras el handshake: acumula bytes del `reader`, extrae los paquetes
/// **completos** (framing por el `length` del header), despacha cada **comando** a `session` y escribe
/// su respuesta. Termina en EOF (el cliente cerró). Los paquetes de respuesta entrantes se ignoran
/// (un cliente manda comandos). El bridge JVMTI↔JDWP —que además **empuja** eventos— va aparte.
pub fn serve<R: Read, W: Write>(
    reader: &mut R,
    writer: &mut W,
    session: &mut JdwpSession,
) -> io::Result<()> {
    let mut buf: Vec<u8> = Vec::new();
    let mut chunk = [0u8; 4096];
    loop {
        // Despachar todos los paquetes ya completos en el buffer.
        while let Some(len) = Packet::framed_len(&buf) {
            let packet = Packet::decode(&buf).expect("framed_len ⇒ hay un paquete entero");
            buf.drain(..len);
            if let Packet::Command(cmd) = packet {
                let reply = session.handle(&cmd);
                writer.write_all(&Packet::Reply(reply).encode())?;
                writer.flush()?;
            }
        }
        // Leer más.
        let n = reader.read(&mut chunk)?;
        if n == 0 {
            return Ok(()); // EOF
        }
        buf.extend_from_slice(&chunk[..n]);
    }
}

/// Escucha en `addr` (p.ej. `"127.0.0.1:5005"`), acepta **una** conexión de debugger, hace el
/// handshake y la sirve hasta que cierre. La VM como servidor `dt_socket`. (El `JdwpSession` acá
/// resuelve el protocolo; conectarlo a una VM corriendo —para que un breakpoint frene de verdad y
/// los eventos se empujen— es el bridge JVMTI↔JDWP, la próxima pieza.)
pub fn listen(addr: &str, session: &mut JdwpSession) -> io::Result<()> {
    let listener = TcpListener::bind(addr)?;
    let (mut reader, _peer) = listener.accept()?;
    let mut writer = reader.try_clone()?;
    handshake(&mut reader, &mut writer)?;
    serve(&mut reader, &mut writer, session)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn the_handshake_is_the_14_ascii_bytes() {
        assert_eq!(HANDSHAKE, b"JDWP-Handshake");
        assert_eq!(HANDSHAKE.len(), 14);
    }

    #[test]
    fn a_command_packet_round_trips_through_encode_decode() {
        let cmd = Packet::Command(CommandPacket {
            id: 0xDEAD_BEEF,
            command_set: 1, // VirtualMachine
            command: 1,     // Version
            data: vec![1, 2, 3, 4, 5],
        });
        let bytes = cmd.encode();
        // header (11) + data (5)
        assert_eq!(bytes.len(), 16);
        assert_eq!(u32::from_be_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]), 16);
        assert_eq!(bytes[8] & FLAG_REPLY, 0, "es un comando, no una respuesta");
        assert_eq!(Packet::framed_len(&bytes), Some(16));
        assert_eq!(Packet::decode(&bytes), Some(cmd));
    }

    #[test]
    fn a_reply_packet_round_trips_and_carries_its_error() {
        let reply = Packet::Reply(ReplyPacket { id: 7, error: 0, data: vec![0xAA, 0xBB] });
        let bytes = reply.encode();
        assert_ne!(bytes[8] & FLAG_REPLY, 0, "el bit de reply está prendido");
        assert_eq!(Packet::decode(&bytes), Some(reply));

        let err = Packet::Reply(ReplyPacket { id: 8, error: 113, data: vec![] });
        assert_eq!(Packet::decode(&err.encode()), Some(err), "un reply de error round-trippea");
    }

    #[test]
    fn decode_waits_for_a_full_packet() {
        let bytes = Packet::Command(CommandPacket { id: 1, command_set: 1, command: 1, data: vec![9, 9, 9] }).encode();
        assert_eq!(Packet::decode(&bytes[..5]), None, "header incompleto → None");
        assert_eq!(Packet::decode(&bytes[..bytes.len() - 1]), None, "falta 1 byte de data → None");
        assert!(Packet::decode(&bytes).is_some(), "paquete completo → Some");
    }

    #[test]
    fn the_typed_writer_and_reader_round_trip() {
        let data = Writer::new()
            .byte(0x42)
            .boolean(true)
            .int(-1_000)
            .long(1 << 40)
            .id(0x0123_4567_89AB_CDEF)
            .string("Clase.metodo")
            .finish();

        let mut r = Reader::new(&data);
        assert_eq!(r.byte(), Some(0x42));
        assert_eq!(r.boolean(), Some(true));
        assert_eq!(r.int(), Some(-1_000));
        assert_eq!(r.long(), Some(1 << 40));
        assert_eq!(r.id(), Some(0x0123_4567_89AB_CDEF));
        assert_eq!(r.string().as_deref(), Some("Clase.metodo"));
        assert_eq!(r.remaining(), 0, "se consumió toda la data");
        // más allá del final: None, sin panic.
        assert_eq!(r.int(), None);
    }

    // ---- command handlers ----

    fn command(cs: u8, c: u8, data: Vec<u8>) -> CommandPacket {
        CommandPacket { id: 1, command_set: cs, command: c, data }
    }

    #[test]
    fn vm_version_replies_with_the_version_fields() {
        let mut s = JdwpSession::new();
        let reply = s.handle(&command(command_set::VIRTUAL_MACHINE, 1, vec![]));
        assert_eq!(reply.error, error_code::NONE);
        let mut r = Reader::new(&reply.data);
        assert_eq!(r.string().as_deref(), Some("KajiVM JDWP (Hito I3)"));
        assert_eq!(r.int(), Some(1)); // jdwpMajor
        assert_eq!(r.int(), Some(8)); // jdwpMinor
        assert_eq!(r.string().as_deref(), Some("25"));
        assert_eq!(r.string().as_deref(), Some("KajiVM"));
    }

    #[test]
    fn vm_id_sizes_are_all_eight() {
        let mut s = JdwpSession::new();
        let reply = s.handle(&command(command_set::VIRTUAL_MACHINE, 7, vec![]));
        let mut r = Reader::new(&reply.data);
        for _ in 0..5 {
            assert_eq!(r.int(), Some(8));
        }
        assert_eq!(r.remaining(), 0);
    }

    #[test]
    fn event_request_set_registers_a_breakpoint_and_hands_out_ids() {
        let mut s = JdwpSession::new();
        // eventKind BREAKPOINT · suspendPolicy ALL(2) · 1 modificador LocationOnly (tag, class, method, index).
        let data = Writer::new()
            .byte(event_kind::BREAKPOINT)
            .byte(2)
            .int(1)
            .byte(MOD_LOCATION_ONLY)
            .byte(1) // typeTag CLASS
            .id(0xAA) // classID
            .id(0x5) // methodID
            .long(12) // índice
            .finish();
        let reply = s.handle(&command(command_set::EVENT_REQUEST, 1, data));
        assert_eq!(reply.error, error_code::NONE);
        assert_eq!(Reader::new(&reply.data).int(), Some(1), "primer requestID = 1");
        assert_eq!(s.requests().len(), 1);
        let req = &s.requests()[0];
        assert_eq!(req.kind, event_kind::BREAKPOINT);
        assert_eq!(req.location, Some((0x5, 12)), "sacó la posición del modificador LocationOnly");

        // Un segundo Set (un method-entry, sin location) → requestID 2.
        let m = Writer::new().byte(event_kind::METHOD_ENTRY).byte(0).int(0).finish();
        let reply2 = s.handle(&command(command_set::EVENT_REQUEST, 1, m));
        assert_eq!(Reader::new(&reply2.data).int(), Some(2));
        assert_eq!(s.requests()[1].location, None);
    }

    #[test]
    fn event_request_clear_removes_the_request() {
        let mut s = JdwpSession::new();
        let set = Writer::new().byte(event_kind::METHOD_ENTRY).byte(0).int(0).finish();
        s.handle(&command(command_set::EVENT_REQUEST, 1, set));
        assert_eq!(s.requests().len(), 1);
        let clear = Writer::new().byte(event_kind::METHOD_ENTRY).int(1).finish();
        let reply = s.handle(&command(command_set::EVENT_REQUEST, 2, clear));
        assert_eq!(reply.error, error_code::NONE);
        assert!(s.requests().is_empty());
    }

    #[test]
    fn suspend_and_resume_toggle_and_an_unknown_command_is_not_implemented() {
        let mut s = JdwpSession::new();
        assert!(!s.is_suspended());
        s.handle(&command(command_set::VIRTUAL_MACHINE, 8, vec![])); // Suspend
        assert!(s.is_suspended());
        s.handle(&command(command_set::VIRTUAL_MACHINE, 9, vec![])); // Resume
        assert!(!s.is_suspended());
        let reply = s.handle(&command(0x77, 0x77, vec![]));
        assert_eq!(reply.error, error_code::NOT_IMPLEMENTED);
        assert!(reply.data.is_empty());
    }

    // ---- transporte (sin red: Cursor de entrada + Vec de salida) ----

    #[test]
    fn the_transport_handshakes_and_serves_commands_over_a_stream() {
        // Entrada: el handshake + dos comandos (Version y IDSizes) pegados —el framing los separa—.
        let mut input = Vec::new();
        input.extend_from_slice(HANDSHAKE);
        input.extend_from_slice(&Packet::Command(command(command_set::VIRTUAL_MACHINE, 1, vec![])).encode());
        input.extend_from_slice(&Packet::Command(command(command_set::VIRTUAL_MACHINE, 7, vec![])).encode());
        let mut reader = std::io::Cursor::new(input);
        let mut writer: Vec<u8> = Vec::new();
        let mut session = JdwpSession::new();

        handshake(&mut reader, &mut writer).unwrap();
        serve(&mut reader, &mut writer, &mut session).unwrap();

        // Salida: el eco del handshake + las dos respuestas, en orden.
        assert_eq!(&writer[..HANDSHAKE.len()], HANDSHAKE);
        let mut rest = &writer[HANDSHAKE.len()..];
        // 1ª respuesta: Version.
        let len1 = Packet::framed_len(rest).unwrap();
        match Packet::decode(rest).unwrap() {
            Packet::Reply(r) => assert_eq!(r.error, error_code::NONE),
            _ => panic!("esperaba un reply"),
        }
        rest = &rest[len1..];
        // 2ª respuesta: IDSizes (5 × 8).
        match Packet::decode(rest).unwrap() {
            Packet::Reply(r) => {
                let mut rd = Reader::new(&r.data);
                for _ in 0..5 {
                    assert_eq!(rd.int(), Some(8));
                }
            }
            _ => panic!("esperaba un reply"),
        }
    }

    #[test]
    fn a_bad_handshake_is_rejected() {
        let mut reader = std::io::Cursor::new(b"NOT-A-HANDSHK!".to_vec()); // 14 bytes, no matchea
        let mut writer: Vec<u8> = Vec::new();
        assert!(handshake(&mut reader, &mut writer).is_err());
        assert!(writer.is_empty(), "no se contesta un handshake inválido");
    }
}
