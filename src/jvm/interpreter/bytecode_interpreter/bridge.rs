//! **Bridge JVMTI↔JDWP** (Hito I3, cierre): la pieza que hace que un breakpoint pedido por protocolo
//! **frene de verdad** y que un evento de la VM **llegue al cliente**. Une los dos módulos vecinos —el
//! back-end [`super::jvmti`] y el protocolo [`super::jdwp`]— que hasta acá no se conocían.
//!
//! El bridge **es un [`JvmtiAgent`]** (como el `Jdb` in-process de I1), pero en vez de abrir un prompt
//! en consola habla JDWP por un stream:
//!
//! - **hacia arriba (VM → cliente)**: cuando la VM le dispara un callback (`breakpoint`, …), arma un
//!   *Composite event packet* y lo **empuja** por el stream.
//! - **hacia abajo (cliente → VM)**: mientras el evento **suspende** al hilo, el bridge **sirve
//!   comandos** del stream —los despacha a la [`JdwpSession`] y **aplica sus efectos a la VM** por el
//!   [`JvmtiEnv`]: un `EventRequest.Set` de breakpoint se vuelve un `set_breakpoint` real— hasta que el
//!   cliente manda `VirtualMachine.Resume`, y ahí devuelve el control para que `step()` siga.
//!
//! El mismo bucle de servicio corre en `vm_init` (la fase inicial de configuración: el cliente pone sus
//! breakpoints y hace el primer `Resume`) y tras cada evento. Es genérico sobre `Read + Write`, así que
//! se testea entero en memoria —sin socket— con un stream de ida y vuelta.
//!
//! ## El truco del borrow, otra vez
//!
//! El bridge sostiene `session` y `stream` como campos **propios**; los callbacks reciben el `env` como
//! parámetro. No hay conflicto: escribe su stream y muta su sesión (campos del agente) mientras lee/
//! escribe el `env` (que la VM le pasa destructurando `self` — el agente ⊥ los campos del env).

use std::collections::HashSet;
use std::io::{self, Read, Write};

use super::debug_info::VmSnapshot;
use super::jdwp::{
    class_type_command, command_set, error_code, event_kind, frame_command, method_command,
    object_command, reference_type_command, suspend_policy, thread_command, thread_group_command,
    thread_status, value_tag, vm_command, CommandPacket, EventRequest, JdwpSession, Packet, Reader,
    ReplyPacket, Writer, CLASS_STATUS_READY, EVENT_COMPOSITE, REF_TYPE_TAG_CLASS, TYPE_TAG_CLASS,
};
use super::jvmti::{JvmtiAgent, JvmtiEnv, ObjRef, ThreadId};
use super::super::frame::Value;
use super::super::metaspace::MethodId;

/// JDWP reserva el id `0` para **null**, pero nuestros `MethodId` arrancan en `0` (el método de entrada
/// suele ser el `0`). Así que exponemos `MethodId + 1` como *methodID* JDWP y lo des-mapeamos al
/// recibirlo. Sin esto un cliente real (jdb/IntelliJ) muestra el método como `<obsolete>` y `line=-1`.
fn to_jdwp_method(vm_method: u64) -> u64 {
    vm_method + 1
}
/// La inversa de [`to_jdwp_method`].
fn from_jdwp_method(jdwp_method: u64) -> u64 {
    jdwp_method.saturating_sub(1)
}

/// Arma el *Composite event packet* de **un** evento con ubicación (breakpoint / single-step / method
/// entry): `suspendPolicy · events=1 · [eventKind · requestID · thread · location]`, donde la
/// ubicación es `typeTag · classID · methodID · index`. Es **puro** (no toca la VM ni el stream), por
/// eso se testea suelto. `out_id` es el id del paquete saliente (la VM lo genera).
fn composite_located(
    out_id: u32,
    suspend_pol: u8,
    kind: u8,
    request_id: i32,
    thread: ThreadId,
    class_id: u64,
    method: MethodId,
    location: u32,
) -> Packet {
    let mut w = Writer::new();
    w.byte(suspend_pol)
        .int(1) // un solo evento en el sobre
        .byte(kind)
        .int(request_id)
        .id(thread as u64)
        // location: typeTag + classID + methodID + index
        .byte(TYPE_TAG_CLASS)
        .id(class_id)
        .id(to_jdwp_method(method as u64))
        .long(location as i64);
    Packet::Command(CommandPacket {
        id: out_id,
        command_set: command_set::EVENT,
        command: EVENT_COMPOSITE,
        data: w.finish(),
    })
}

/// Arma el *Composite* de una **excepción**: como el de ubicación, más el objeto lanzado y la ubicación
/// del *catch* (acá `(0,0)` = «no encontrado / uncaught», que es lo que el intérprete sabe al lanzar).
fn composite_exception(
    out_id: u32,
    suspend_pol: u8,
    request_id: i32,
    thread: ThreadId,
    class_id: u64,
    method: MethodId,
    location: u32,
    exception: ObjRef,
) -> Packet {
    let mut w = Writer::new();
    w.byte(suspend_pol)
        .int(1)
        .byte(event_kind::EXCEPTION)
        .int(request_id)
        .id(thread as u64)
        // throw location
        .byte(TYPE_TAG_CLASS)
        .id(class_id)
        .id(to_jdwp_method(method as u64))
        .long(location as i64)
        // el objeto excepción (tag 'L' = objeto)
        .byte(b'L')
        .id(exception as u64)
        // catch location: (0,0) = sin handler conocido
        .byte(TYPE_TAG_CLASS)
        .id(0)
        .id(0)
        .long(0);
    Packet::Command(CommandPacket {
        id: out_id,
        command_set: command_set::EVENT,
        command: EVENT_COMPOSITE,
        data: w.finish(),
    })
}

/// Arma el *Composite* de un **field watchpoint** (`FIELD_ACCESS` o `FIELD_MODIFICATION`): la ubicación
/// de código, el campo `(refTypeTag · typeID · fieldID)`, el objeto receptor tageado, y —para la
/// modificación— el nuevo valor. `class_id_of_method` es el classID de la ubicación; `(field_class_id,
/// field_id)` identifican el campo.
#[allow(clippy::too_many_arguments)]
fn composite_field(
    out_id: u32,
    suspend_pol: u8,
    kind: u8,
    request_id: i32,
    thread: ThreadId,
    class_id_of_method: u64,
    method: MethodId,
    location: u32,
    field_class_id: u64,
    field_id: u64,
    object: ObjRef,
    new_value: Option<Value>,
) -> Packet {
    let mut w = Writer::new();
    w.byte(suspend_pol).int(1).byte(kind).int(request_id).id(thread as u64);
    // ubicación de código donde se toca el campo
    w.byte(TYPE_TAG_CLASS).id(class_id_of_method).id(to_jdwp_method(method as u64)).long(location as i64);
    // el campo: refTypeTag + typeID + fieldID
    w.byte(TYPE_TAG_CLASS).id(field_class_id).id(field_id);
    // el objeto receptor (taggedObjectID): tag 'L' + objectID (0 para un campo estático)
    w.byte(value_tag::OBJECT).id(object as u64);
    // FIELD_MODIFICATION: el valor que se va a escribir (tageado)
    if let Some(value) = new_value {
        write_tagged_value(&mut w, value);
    }
    Packet::Command(CommandPacket {
        id: out_id,
        command_set: command_set::EVENT,
        command: EVENT_COMPOSITE,
        data: w.finish(),
    })
}

// ---- introspección: comandos JDWP respaldados por el `env` (Hito I5) ----------------------------
//
// Estos comandos (leer la pila, los locales, los hilos) necesitan datos **de la VM**, no solo del
// protocolo — así que se atienden acá, donde hay un `JvmtiEnv`, y no en la `JdwpSession` pura. El
// dispatcher `reply_for` los intercepta y delega el resto (Version, EventRequest, Resume…) a la sesión.

/// Escribe una `location` JDWP (`typeTag · classID · methodID · index`). El `class_id` es el
/// `referenceTypeID` de la clase del método (0 solo si no está en el snapshot — un cliente real lo
/// rechazaría, pero para métodos capturados siempre es no-cero).
fn write_location(w: &mut Writer, class_id: u64, method: MethodId, index: u32) {
    w.byte(TYPE_TAG_CLASS);
    w.id(class_id);
    w.id(to_jdwp_method(method as u64));
    w.long(index as i64);
}

/// Escribe un **valor tipado** JDWP: el `tag` + los bytes del valor. Un `float`/`double` va por sus
/// bits (preservando el patrón) sobre `int`/`long`.
fn write_tagged_value(w: &mut Writer, value: Value) {
    match value {
        Value::Int(n) => {
            w.byte(value_tag::INT);
            w.int(n);
        }
        Value::Long(n) => {
            w.byte(value_tag::LONG);
            w.long(n);
        }
        Value::Float(f) => {
            w.byte(value_tag::FLOAT);
            w.int(f.to_bits() as i32);
        }
        Value::Double(d) => {
            w.byte(value_tag::DOUBLE);
            w.long(d.to_bits() as i64);
        }
        Value::Reference(r) => {
            w.byte(value_tag::OBJECT);
            w.id(r as u64);
        }
    }
}

/// El id de hilo que exponemos por JDWP cuando no hay registro real (tests). JDWP reserva el id `0`
/// para **null** —un cliente real (el JDI) lo rechaza—, así que el hilo interno `0` se mapea a `1`.
/// Con registro real, cada hilo `vm_id` se expone como `vm_id + 1` por la misma razón.
const JDWP_THREAD_ID: u64 = 1;

/// El nombre visible de un hilo dado su id JDWP: el `main` (vm_id `0`) o `Thread-N` para el resto.
/// Mapea de vuelta el `+1` que JDWP le suma a cada id interno.
fn thread_display_name(jdwp_id: u64) -> String {
    let vm_id = jdwp_id.saturating_sub(1);
    if vm_id == 0 {
        "main".to_string()
    } else {
        format!("Thread-{vm_id}")
    }
}

/// Escribe la lista de ids de hilo (`vm_id + 1`) del registro real, o degrada al `main` si el `env`
/// no trae registro (tests que fabrican un `env` sin hilos).
fn write_thread_ids(w: &mut Writer, env: &JvmtiEnv) {
    let ids = env.thread_ids();
    if ids.is_empty() {
        w.int(1);
        w.id(JDWP_THREAD_ID);
    } else {
        w.int(ids.len() as i32);
        for &id in ids {
            w.id(id as u64 + 1); // JDWP reserva el 0 para null (ver JDWP_THREAD_ID)
        }
    }
}

/// `VirtualMachine.AllThreads` — enumera **todos** los hilos vivos del registro de la VM.
fn all_threads(env: &JvmtiEnv) -> Vec<u8> {
    let mut w = Writer::new();
    write_thread_ids(&mut w, env);
    w.finish()
}

/// `ThreadReference.Name` — el nombre del hilo pedido (`main` o `Thread-N`).
fn thread_name(data: &[u8]) -> Vec<u8> {
    let mut r = Reader::new(data);
    let jdwp_id = r.id().unwrap_or(JDWP_THREAD_ID);
    let mut w = Writer::new();
    w.string(&thread_display_name(jdwp_id));
    w.finish()
}

/// `ThreadReference.FrameCount` — cuántos frames tiene la pila del hilo del evento.
fn thread_frame_count(env: &JvmtiEnv) -> Vec<u8> {
    let mut w = Writer::new();
    w.int(env.frame_count() as i32);
    w.finish()
}

/// `ThreadReference.Frames` — la porción `[start, start+length)` de la pila (de arriba hacia abajo),
/// cada frame como `frameID · location`. El `frameID` es la **profundidad** (`0` = tope), lo que
/// `StackFrame.GetValues` usa para volver a ubicar el frame. `length = -1` pide todos.
fn thread_frames(env: &JvmtiEnv, snapshot: &VmSnapshot, data: &[u8]) -> Result<Vec<u8>, u16> {
    let mut r = Reader::new(data);
    let _thread = r.id().ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let start = r.int().ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let length = r.int().ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let trace = env.stack_trace(); // (método, pc), tope primero
    let total = trace.len() as i32;
    let start = start.clamp(0, total);
    let count = if length < 0 { total - start } else { length.min(total - start) };
    let mut w = Writer::new();
    w.int(count);
    for depth in start..(start + count) {
        let (method, index) = trace[depth as usize];
        let class_id = snapshot.class_id_of_method(method as u64).unwrap_or(0);
        w.id(depth as u64); // frameID = profundidad
        write_location(&mut w, class_id, method, index);
    }
    Ok(w.finish())
}

/// `StackFrame.GetValues` — lee los locales pedidos del frame `frameID` (= profundidad). Cada pedido es
/// `(slot, sigByte)`; devuelve `values · [valor tipado]`. Un slot inexistente se contesta como `int 0`.
fn stack_frame_get_values(env: &JvmtiEnv, data: &[u8]) -> Result<Vec<u8>, u16> {
    let mut r = Reader::new(data);
    // La request lleva threadID **y** frameID (un frame solo tiene sentido con su hilo).
    let _thread = r.id().ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let frame_id = r.id().ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let slots = r.int().ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let depth = frame_id as usize;
    let mut w = Writer::new();
    w.int(slots);
    for _ in 0..slots {
        let slot = r.int().ok_or(error_code::ILLEGAL_ARGUMENT)?;
        let _sig = r.byte().ok_or(error_code::ILLEGAL_ARGUMENT)?;
        match env.local(depth, slot as u16) {
            Some(value) => write_tagged_value(&mut w, value),
            None => {
                w.byte(value_tag::INT);
                w.int(0);
            }
        }
    }
    Ok(w.finish())
}

// ---- resolución: comandos JDWP respaldados por el snapshot (Hito I5b) ----------------------------
//
// Estos comandos leen la **metadata** capturada al attachear (clases, métodos, tablas de líneas), no la
// pila viva — así un cliente traduce `Add.java:12` a la `(methodID, índice)` de un breakpoint.

/// `VirtualMachine.AllClasses` — todas las clases del snapshot con su firma y estado.
fn all_classes(snapshot: &VmSnapshot) -> Vec<u8> {
    let mut w = Writer::new();
    w.int(snapshot.classes().len() as i32);
    for class in snapshot.classes() {
        w.byte(REF_TYPE_TAG_CLASS);
        w.id(class.id);
        w.string(&class.signature);
        w.int(CLASS_STATUS_READY);
    }
    w.finish()
}

/// `VirtualMachine.ClassesBySignature` — las clases cuya firma JVM (`LAdd;`) casa la pedida.
fn classes_by_signature(snapshot: &VmSnapshot, data: &[u8]) -> Result<Vec<u8>, u16> {
    let mut r = Reader::new(data);
    let signature = r.string().ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let matches: Vec<_> = snapshot.classes().iter().filter(|c| c.signature == signature).collect();
    let mut w = Writer::new();
    w.int(matches.len() as i32);
    for class in matches {
        w.byte(REF_TYPE_TAG_CLASS);
        w.id(class.id);
        w.int(CLASS_STATUS_READY);
    }
    Ok(w.finish())
}

/// `ReferenceType.Signature` — la firma JVM de una clase por su `referenceTypeID`.
fn reference_type_signature(snapshot: &VmSnapshot, data: &[u8]) -> Result<Vec<u8>, u16> {
    let id = Reader::new(data).id().ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let class = snapshot.class(id).ok_or(error_code::ILLEGAL_ARGUMENT)?;
    Ok(Writer::new().string(&class.signature).finish())
}

/// `ReferenceType.Methods` — los métodos de una clase: `(methodID, nombre, descriptor, modBits)`.
fn reference_type_methods(snapshot: &VmSnapshot, data: &[u8]) -> Result<Vec<u8>, u16> {
    let id = Reader::new(data).id().ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let class = snapshot.class(id).ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let mut w = Writer::new();
    w.int(class.methods.len() as i32);
    for method in &class.methods {
        w.id(to_jdwp_method(method.id));
        w.string(&method.name);
        w.string(&method.signature);
        w.int(method.mod_bits as i32);
    }
    Ok(w.finish())
}

/// `ReferenceType.Fields[WithGeneric]` — los campos de una clase: `(fieldID, nombre, descriptor,
/// (genérico), modBits)`. El cliente los usa para resolver `Watched.value` → fieldID de un watchpoint.
fn reference_type_fields(snapshot: &VmSnapshot, data: &[u8], with_generic: bool) -> Result<Vec<u8>, u16> {
    let id = Reader::new(data).id().ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let class = snapshot.class(id).ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let mut w = Writer::new();
    w.int(class.fields.len() as i32);
    for field in &class.fields {
        w.id(field.id);
        w.string(&field.name);
        w.string(&field.signature);
        if with_generic {
            w.string(""); // genericSignature
        }
        w.int(field.mod_bits as i32);
    }
    Ok(w.finish())
}

/// `ReferenceType.Interfaces` — las interfaces que implementa una clase. No las modelamos → lista vacía
/// (un cliente que resuelve un campo las recorre; vacío está bien para clases sin interfaces).
fn reference_type_interfaces(_snapshot: &VmSnapshot, _data: &[u8]) -> Result<Vec<u8>, u16> {
    Ok(Writer::new().int(0).finish())
}

/// `ClassType.Superclass` — el `referenceTypeID` de la superclase (o `0` para `Object`/la raíz, o si la
/// superclase no está en el snapshot). Un cliente lo usa para caminar la jerarquía al resolver un campo.
fn class_type_superclass(snapshot: &VmSnapshot, data: &[u8]) -> Result<Vec<u8>, u16> {
    let id = Reader::new(data).id().ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let class = snapshot.class(id).ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let super_id = class
        .super_name
        .as_deref()
        .and_then(|binary| {
            let sig = super::debug_info::binary_to_signature(binary);
            snapshot.classes().iter().find(|c| c.signature == sig)
        })
        .map(|c| c.id)
        .unwrap_or(0);
    Ok(Writer::new().id(super_id).finish())
}

/// `ReferenceType.SourceFile` — el nombre del archivo fuente de una clase (o `ABSENT_INFORMATION`).
fn reference_type_source_file(snapshot: &VmSnapshot, data: &[u8]) -> Result<Vec<u8>, u16> {
    let id = Reader::new(data).id().ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let class = snapshot.class(id).ok_or(error_code::ILLEGAL_ARGUMENT)?;
    match &class.source_file {
        Some(name) => Ok(Writer::new().string(name).finish()),
        None => Err(error_code::ABSENT_INFORMATION),
    }
}

/// `Method.LineTable` — `start · end · [(índice de bytecode, línea)]`. La pieza que mapea línea de
/// fuente ↔ posición de código para un breakpoint por línea.
fn method_line_table(snapshot: &VmSnapshot, data: &[u8]) -> Result<Vec<u8>, u16> {
    let mut r = Reader::new(data);
    let _class = r.id().ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let method_id = from_jdwp_method(r.id().ok_or(error_code::ILLEGAL_ARGUMENT)?);
    let method = snapshot.method(method_id).ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let mut w = Writer::new();
    w.long(0); // start: el primer índice de código
    w.long(method.code_len as i64); // end: el largo del código
    w.int(method.line_table.len() as i32);
    for &(index, line) in &method.line_table {
        w.long(index as i64);
        w.int(line);
    }
    Ok(w.finish())
}

// ---- variantes "WithGeneric" + negociación, para el JDI real (Hito I5, fidelidad) ----------------
//
// El JDI de referencia (el que usan `jdb` e IntelliJ) anuncia Java 5+ por nuestra versión JDWP 1.8, así
// que emite las variantes **con firma genérica** de varios comandos —y consulta capabilities, class
// paths y estado de hilos al attachear—. Modelamos lo mínimo para que un debugger real se conecte.

/// `VirtualMachine.AllClassesWithGeneric` — como [`all_classes`] pero con la firma genérica (vacía, no
/// modelamos genéricos) de cada clase.
fn all_classes_with_generic(snapshot: &VmSnapshot) -> Vec<u8> {
    let mut w = Writer::new();
    w.int(snapshot.classes().len() as i32);
    for class in snapshot.classes() {
        w.byte(REF_TYPE_TAG_CLASS);
        w.id(class.id);
        w.string(&class.signature);
        w.string(""); // genericSignature
        w.int(CLASS_STATUS_READY);
    }
    w.finish()
}

/// `ReferenceType.SignatureWithGeneric` — la firma JVM + la genérica (vacía).
fn reference_type_signature_with_generic(snapshot: &VmSnapshot, data: &[u8]) -> Result<Vec<u8>, u16> {
    let id = Reader::new(data).id().ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let class = snapshot.class(id).ok_or(error_code::ILLEGAL_ARGUMENT)?;
    Ok(Writer::new().string(&class.signature).string("").finish())
}

/// `ReferenceType.MethodsWithGeneric` — los métodos con su firma genérica (vacía) intercalada.
fn reference_type_methods_with_generic(snapshot: &VmSnapshot, data: &[u8]) -> Result<Vec<u8>, u16> {
    let id = Reader::new(data).id().ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let class = snapshot.class(id).ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let mut w = Writer::new();
    w.int(class.methods.len() as i32);
    for method in &class.methods {
        w.id(to_jdwp_method(method.id));
        w.string(&method.name);
        w.string(&method.signature);
        w.string(""); // genericSignature
        w.int(method.mod_bits as i32);
    }
    Ok(w.finish())
}

/// `VirtualMachine.Capabilities` — 7 booleanos. Los dos primeros son `canWatchFieldModification` y
/// `canWatchFieldAccess`, en **verdadero** (soportamos field watchpoints); el resto en falso. Sin esto
/// un cliente real (jdb) rechaza `watch` con «not supported on the target VM».
fn capabilities() -> Vec<u8> {
    let mut w = Writer::new();
    w.boolean(true); // canWatchFieldModification
    w.boolean(true); // canWatchFieldAccess
    for _ in 0..5 {
        w.boolean(false);
    }
    w.finish()
}

/// `VirtualMachine.CapabilitiesNew` — 32 booleanos; los dos primeros (field watch) en verdadero, el
/// resto en falso.
fn capabilities_new() -> Vec<u8> {
    let mut w = Writer::new();
    w.boolean(true); // canWatchFieldModification
    w.boolean(true); // canWatchFieldAccess
    for _ in 0..30 {
        w.boolean(false);
    }
    w.finish()
}

/// `VirtualMachine.ClassPaths` — el directorio base y las listas de classpath (vacías).
fn class_paths() -> Vec<u8> {
    let mut w = Writer::new();
    w.string("."); // baseDir
    w.int(0); // classpaths
    w.int(0); // bootclasspaths
    w.finish()
}

/// El id del único grupo de hilos que exponemos (no-cero; el `0` es null en JDWP). El cliente lo pide
/// para agrupar los hilos —`jdb` explota si un hilo no tiene grupo—.
const JDWP_GROUP_ID: u64 = 0xC0FFEE;

/// `VirtualMachine.TopLevelThreadGroups` — un único grupo raíz (`main`).
fn top_level_thread_groups() -> Vec<u8> {
    let mut w = Writer::new();
    w.int(1); // un grupo
    w.id(JDWP_GROUP_ID);
    w.finish()
}

/// `ThreadReference.Status` — el único hilo, corriendo y **suspendido** por el debugger.
fn thread_status_reply() -> Vec<u8> {
    let mut w = Writer::new();
    w.int(thread_status::RUNNING);
    w.int(thread_status::SUSPEND_STATUS_SUSPENDED);
    w.finish()
}

/// `ThreadReference.ThreadGroup` — el hilo pertenece a nuestro único grupo.
fn thread_group() -> Vec<u8> {
    let mut w = Writer::new();
    w.id(JDWP_GROUP_ID);
    w.finish()
}

/// `ThreadGroupReference.Name` — el nombre del grupo raíz.
fn thread_group_name() -> Vec<u8> {
    let mut w = Writer::new();
    w.string("main");
    w.finish()
}

/// `ThreadGroupReference.Parent` — es raíz, no tiene padre (id `0` = null).
fn thread_group_parent() -> Vec<u8> {
    let mut w = Writer::new();
    w.id(0);
    w.finish()
}

/// `ThreadGroupReference.Children` — el grupo raíz contiene **todos** los hilos y ningún subgrupo.
fn thread_group_children(env: &JvmtiEnv) -> Vec<u8> {
    let mut w = Writer::new();
    write_thread_ids(&mut w, env); // childThreads
    w.int(0); // childGroups
    w.finish()
}

/// `Method.VariableTable[WithGeneric]` — las variables locales de un método: `argCnt · slots ·
/// [codeIndex · nombre · descriptor · (genérico) · length · slot]`. Vacío si el `.class` no trae
/// `LocalVariableTable` (compilado sin `javac -g`) → `jdb locals` degrada a «no locals», no rompe.
fn method_variable_table(snapshot: &VmSnapshot, data: &[u8], with_generic: bool) -> Result<Vec<u8>, u16> {
    let mut r = Reader::new(data);
    let _class = r.id().ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let method_id = from_jdwp_method(r.id().ok_or(error_code::ILLEGAL_ARGUMENT)?);
    let method = snapshot.method(method_id).ok_or(error_code::ILLEGAL_ARGUMENT)?;
    let mut w = Writer::new();
    w.int(method.arg_slots as i32);
    w.int(method.variables.len() as i32);
    for var in &method.variables {
        w.long(var.start_pc as i64);
        w.string(&var.name);
        w.string(&var.signature);
        if with_generic {
            w.string(""); // genericSignature: no modelamos genéricos
        }
        w.int(var.length as i32);
        w.int(var.slot as i32);
    }
    Ok(w.finish())
}

// ---- inspección de objetos: comandos respaldados por el heap (Hito I5) ---------------------------

/// `ObjectReference.ReferenceType` — el tipo real de un objeto: `refTypeTag · typeID`. Lee la clase del
/// objeto en el heap (por el `env`) y la mapea al `referenceTypeID` del snapshot (`0` si no se resuelve).
fn object_reference_type(env: &JvmtiEnv, snapshot: &VmSnapshot, data: &[u8]) -> Result<Vec<u8>, u16> {
    let object = Reader::new(data).id().ok_or(error_code::ILLEGAL_ARGUMENT)? as usize;
    let class_id = env
        .object_class_name(object)
        .and_then(|name| {
            let sig = super::debug_info::binary_to_signature(name);
            snapshot.classes().iter().find(|c| c.signature == sig)
        })
        .map(|c| c.id)
        .unwrap_or(0);
    Ok(Writer::new().byte(REF_TYPE_TAG_CLASS).id(class_id).finish())
}

/// `ObjectReference.GetValues` — lee los campos de instancia pedidos de un objeto. Para cada `fieldID`,
/// busca el campo en la clase real del objeto (nombre/offset del snapshot) y lee su valor del heap. Un
/// campo que no se resuelve (sin heap, o clase/campo desconocido) se contesta como `int 0`.
fn object_reference_get_values(env: &JvmtiEnv, snapshot: &VmSnapshot, data: &[u8]) -> Result<Vec<u8>, u16> {
    let mut r = Reader::new(data);
    let object = r.id().ok_or(error_code::ILLEGAL_ARGUMENT)? as usize;
    let count = r.int().ok_or(error_code::ILLEGAL_ARGUMENT)?;
    // La clase real del objeto (para ubicar los campos por su fieldID).
    let class = env.object_class_name(object).and_then(|name| {
        let sig = super::debug_info::binary_to_signature(name);
        snapshot.classes().iter().find(|c| c.signature == sig)
    });
    let mut w = Writer::new();
    w.int(count);
    for _ in 0..count {
        let field_id = r.id().ok_or(error_code::ILLEGAL_ARGUMENT)?;
        let value = class
            .and_then(|c| c.fields.iter().find(|f| f.id == field_id))
            .and_then(|f| env.read_object_field(object, f.offset, &f.signature));
        match value {
            Some(v) => write_tagged_value(&mut w, v),
            None => {
                w.byte(value_tag::INT);
                w.int(0);
            }
        }
    }
    Ok(w.finish())
}

/// El **dispatcher** del bridge: atiende los comandos que necesitan la VM —el `env` (pila/valores/heap) o
/// el `snapshot` (clases/métodos/líneas)— y delega el resto a la [`JdwpSession`] pura (Version, IDSizes,
/// EventRequest, Suspend/Resume).
pub(crate) fn reply_for(
    session: &mut JdwpSession,
    cmd: &CommandPacket,
    env: &JvmtiEnv,
    snapshot: &VmSnapshot,
) -> ReplyPacket {
    use command_set::*;
    let result: Result<Vec<u8>, u16> = match (cmd.command_set, cmd.command) {
        // Pila / valores (respaldados por el `env`).
        (VIRTUAL_MACHINE, vm_command::ALL_THREADS) => Ok(all_threads(env)),
        (THREAD_REFERENCE, thread_command::NAME) => Ok(thread_name(&cmd.data)),
        (THREAD_REFERENCE, thread_command::FRAMES) => thread_frames(env, snapshot, &cmd.data),
        (THREAD_REFERENCE, thread_command::FRAME_COUNT) => Ok(thread_frame_count(env)),
        (STACK_FRAME, frame_command::GET_VALUES) => stack_frame_get_values(env, &cmd.data),
        // Resolución de clases / métodos / líneas (respaldados por el `snapshot`).
        (VIRTUAL_MACHINE, vm_command::ALL_CLASSES) => Ok(all_classes(snapshot)),
        (VIRTUAL_MACHINE, vm_command::ALL_CLASSES_WITH_GENERIC) => Ok(all_classes_with_generic(snapshot)),
        (VIRTUAL_MACHINE, vm_command::CLASSES_BY_SIGNATURE) => classes_by_signature(snapshot, &cmd.data),
        (REFERENCE_TYPE, reference_type_command::SIGNATURE) => reference_type_signature(snapshot, &cmd.data),
        (REFERENCE_TYPE, reference_type_command::SIGNATURE_WITH_GENERIC) => reference_type_signature_with_generic(snapshot, &cmd.data),
        (REFERENCE_TYPE, reference_type_command::METHODS) => reference_type_methods(snapshot, &cmd.data),
        (REFERENCE_TYPE, reference_type_command::METHODS_WITH_GENERIC) => reference_type_methods_with_generic(snapshot, &cmd.data),
        (REFERENCE_TYPE, reference_type_command::FIELDS) => reference_type_fields(snapshot, &cmd.data, false),
        (REFERENCE_TYPE, reference_type_command::FIELDS_WITH_GENERIC) => reference_type_fields(snapshot, &cmd.data, true),
        (REFERENCE_TYPE, reference_type_command::INTERFACES) => reference_type_interfaces(snapshot, &cmd.data),
        (CLASS_TYPE, class_type_command::SUPERCLASS) => class_type_superclass(snapshot, &cmd.data),
        (OBJECT_REFERENCE, object_command::REFERENCE_TYPE) => object_reference_type(env, snapshot, &cmd.data),
        (OBJECT_REFERENCE, object_command::GET_VALUES) => object_reference_get_values(env, snapshot, &cmd.data),
        (REFERENCE_TYPE, reference_type_command::SOURCE_FILE) => reference_type_source_file(snapshot, &cmd.data),
        (METHOD, method_command::LINE_TABLE) => method_line_table(snapshot, &cmd.data),
        (METHOD, method_command::VARIABLE_TABLE) => method_variable_table(snapshot, &cmd.data, false),
        (METHOD, method_command::VARIABLE_TABLE_WITH_GENERIC) => method_variable_table(snapshot, &cmd.data, true),
        // Negociación / entorno que el JDI real consulta al attachear.
        (VIRTUAL_MACHINE, vm_command::CAPABILITIES) => Ok(capabilities()),
        (VIRTUAL_MACHINE, vm_command::CAPABILITIES_NEW) => Ok(capabilities_new()),
        (VIRTUAL_MACHINE, vm_command::CLASS_PATHS) => Ok(class_paths()),
        (VIRTUAL_MACHINE, vm_command::TOP_LEVEL_THREAD_GROUPS) => Ok(top_level_thread_groups()),
        (VIRTUAL_MACHINE, vm_command::DISPOSE) => Ok(Vec::new()),
        (THREAD_REFERENCE, thread_command::STATUS) => Ok(thread_status_reply()),
        (THREAD_REFERENCE, thread_command::THREAD_GROUP) => Ok(thread_group()),
        (THREAD_GROUP_REFERENCE, thread_group_command::NAME) => Ok(thread_group_name()),
        (THREAD_GROUP_REFERENCE, thread_group_command::PARENT) => Ok(thread_group_parent()),
        (THREAD_GROUP_REFERENCE, thread_group_command::CHILDREN) => Ok(thread_group_children(env)),
        // Todo lo demás es protocolo puro: lo resuelve la sesión (sin tocar la VM).
        _ => return session.handle(cmd),
    };
    match result {
        Ok(data) => ReplyPacket { id: cmd.id, error: error_code::NONE, data },
        Err(error) => ReplyPacket { id: cmd.id, error, data: Vec::new() },
    }
}

/// Arma el *Composite* del evento **VM_START** (automático, `requestID 0`): le dice al cliente que la
/// VM arrancó **suspendida**, con su hilo inicial. Un debugger real lo espera para fijar su hilo actual.
fn composite_vm_start(out_id: u32, thread: u64) -> Packet {
    let mut w = Writer::new();
    w.byte(suspend_policy::ALL);
    w.int(1);
    w.byte(event_kind::VM_START);
    w.int(0); // requestID 0 = evento automático
    w.id(thread);
    Packet::Command(CommandPacket {
        id: out_id,
        command_set: command_set::EVENT,
        command: EVENT_COMPOSITE,
        data: w.finish(),
    })
}

/// El **bridge**: un [`JvmtiAgent`] que traduce entre la VM y un cliente JDWP a través de `stream`.
/// Genérico sobre el transporte (`Read + Write`) para poder testearlo sin red.
pub struct JdwpBridge<S: Read + Write> {
    /// El estado de protocolo del lado debugger (requestIDs, pedidos de evento vivos).
    session: JdwpSession,
    /// El transporte hacia el cliente (un `TcpStream`, o un stream en memoria en los tests).
    stream: S,
    /// Buffer de bytes leídos que todavía no forman un paquete entero (se preserva entre suspends).
    buf: Vec<u8>,
    /// El id del próximo paquete que la VM **manda** (los eventos son comandos VM→cliente).
    next_out_id: u32,
    /// Los breakpoints que ya reflejamos en la VM, para diffear contra los pedidos de la sesión y
    /// aplicar solo el delta (altas/bajas) al `env`.
    applied: HashSet<(u64, u64)>,
    /// Los field watchpoints ya reflejados en la VM: `(es_modificación, clase, campo)`. Se diffean
    /// contra los pedidos `FIELD_ACCESS`/`FIELD_MODIFICATION` de la sesión.
    applied_field_watches: HashSet<(bool, String, String)>,
    /// La metadata de depuración (clases/métodos/líneas/campos) capturada al attachear — lo que sirve la
    /// resolución de breakpoints por línea y de field watchpoints. Vacío si el bridge se armó sin snapshot.
    snapshot: VmSnapshot,
}

impl<S: Read + Write> JdwpBridge<S> {
    /// Crea el bridge sobre un stream **ya handshakeado** (el handshake lo hace la capa de transporte,
    /// [`super::jdwp::handshake`], antes de atachear el agente), **sin** metadata de resolución.
    pub fn new(stream: S) -> Self {
        Self::with_snapshot(stream, VmSnapshot::default())
    }

    /// Como [`new`](Self::new) pero con un [`VmSnapshot`] ya capturado (clases/métodos/líneas), para
    /// que el cliente pueda resolver breakpoints por línea. Lo usa el servidor `jvm-jdwp`, que captura
    /// la metadata del metaspace antes de mover la VM.
    pub fn with_snapshot(stream: S, snapshot: VmSnapshot) -> Self {
        JdwpBridge {
            session: JdwpSession::new(),
            stream,
            buf: Vec::new(),
            next_out_id: 1,
            applied: HashSet::new(),
            applied_field_watches: HashSet::new(),
            snapshot,
        }
    }

    /// Toma el próximo id saliente (y lo avanza).
    fn out_id(&mut self) -> u32 {
        let id = self.next_out_id;
        self.next_out_id += 1;
        id
    }

    /// Reconcilia la VM con los pedidos de la sesión: aplica al `env` el **delta** de breakpoints
    /// (altas y bajas) y prende el single-step si hay algún pedido de `SINGLE_STEP`. Corre tras cada
    /// comando servido, así un `Set`/`Clear` toca la VM de inmediato.
    fn sync_vm(&mut self, env: &mut JvmtiEnv) {
        let want: HashSet<(u64, u64)> = self
            .session
            .requests()
            .iter()
            .filter(|r| r.kind == event_kind::BREAKPOINT)
            .filter_map(|r| r.location)
            .collect();
        // La sesión guarda el methodID en espacio JDWP; el env usa el MethodId de la VM → des-mapeamos.
        for &(m, i) in want.difference(&self.applied) {
            env.set_breakpoint(from_jdwp_method(m) as MethodId, i as u32);
        }
        for &(m, i) in self.applied.difference(&want) {
            env.clear_breakpoint(from_jdwp_method(m) as MethodId, i as u32);
        }
        self.applied = want;

        let stepping = self.session.requests().iter().any(|r| r.kind == event_kind::SINGLE_STEP);
        env.set_single_step(stepping);

        // Field watchpoints: los pedidos `FIELD_*` (por `(classId, fieldId)`) → `(clase, campo)` reales.
        let want_fields: HashSet<(bool, String, String)> = self
            .session
            .requests()
            .iter()
            .filter_map(|r| {
                let is_mod = match r.kind {
                    event_kind::FIELD_MODIFICATION => true,
                    event_kind::FIELD_ACCESS => false,
                    _ => return None,
                };
                let (class_id, field_id) = r.field?;
                let (class, field) = self.snapshot.field_name(class_id, field_id)?;
                Some((is_mod, class.to_string(), field.to_string()))
            })
            .collect();
        for (is_mod, class, field) in want_fields.difference(&self.applied_field_watches) {
            if *is_mod {
                env.set_field_modification_watch(class, field);
            } else {
                env.set_field_access_watch(class, field);
            }
        }
        for (is_mod, class, field) in self.applied_field_watches.difference(&want_fields) {
            if *is_mod {
                env.clear_field_modification_watch(class, field);
            } else {
                env.clear_field_access_watch(class, field);
            }
        }
        self.applied_field_watches = want_fields;
    }

    /// El **bucle de servicio**: sirve comandos del cliente —despachándolos a la sesión, escribiendo la
    /// respuesta y aplicando sus efectos a la VM— hasta que llega un `VirtualMachine.Resume` (devuelve
    /// el control) o el stream corta (EOF → el cliente se fue, se deja correr la VM). Es lo que corre
    /// mientras un evento tiene suspendido al hilo, y también en `vm_init` (la config inicial).
    fn serve_until_resume(&mut self, env: &mut JvmtiEnv) -> io::Result<()> {
        let mut chunk = [0u8; 4096];
        loop {
            while let Some(len) = Packet::framed_len(&self.buf) {
                let packet = Packet::decode(&self.buf).expect("framed_len ⇒ hay un paquete entero");
                self.buf.drain(..len);
                let Packet::Command(cmd) = packet else { continue };
                let is_resume =
                    (cmd.command_set, cmd.command) == (command_set::VIRTUAL_MACHINE, vm_command::RESUME);
                let reply = reply_for(&mut self.session, &cmd, env, &self.snapshot);
                self.stream.write_all(&Packet::Reply(reply).encode())?;
                self.stream.flush()?;
                self.sync_vm(env);
                if is_resume {
                    return Ok(());
                }
            }
            let n = self.stream.read(&mut chunk)?;
            if n == 0 {
                return Ok(()); // EOF: el cliente cerró → dejamos correr la VM
            }
            self.buf.extend_from_slice(&chunk[..n]);
        }
    }

    /// Los pedidos de evento de `kind` cuya ubicación casa `(method, location)` — para saber qué
    /// `requestID`(s) mandar en el sobre y con qué política suspender.
    fn matching(&self, kind: u8, method: MethodId, location: u32) -> Vec<EventRequest> {
        // La sesión guarda el methodID en espacio JDWP (el que mandó el cliente); el callback trae el
        // MethodId de la VM → lo mapeamos para casar.
        let jdwp_method = to_jdwp_method(method as u64);
        self.session
            .requests()
            .iter()
            .filter(|r| r.kind == kind && r.location == Some((jdwp_method, location as u64)))
            .cloned()
            .collect()
    }

    /// Empuja un `Composite` armado por `build` (con el requestID y la política del pedido que casó) y,
    /// si la política suspende, entra al bucle de servicio hasta el `Resume`. Sin pedido que case no hay
    /// a quién avisar: no se manda nada.
    fn deliver(
        &mut self,
        requests: Vec<EventRequest>,
        env: &mut JvmtiEnv,
        build: impl Fn(u32, u8, i32) -> Packet,
    ) -> io::Result<()> {
        let Some(req) = requests.first() else { return Ok(()) };
        let out_id = self.out_id();
        let packet = build(out_id, req.suspend_policy, req.id);
        self.stream.write_all(&packet.encode())?;
        self.stream.flush()?;
        if req.suspend_policy != suspend_policy::NONE {
            self.serve_until_resume(env)?;
        }
        Ok(())
    }

    /// Empuja el `Composite` de un field watchpoint (acceso si `new_value` es `None`, modificación si no):
    /// mapea `(clase, campo)` → `(classID, fieldID)`, busca los pedidos `FIELD_*` que casan y delega en
    /// [`deliver`](Self::deliver).
    #[allow(clippy::too_many_arguments)]
    fn deliver_field(
        &mut self,
        env: &mut JvmtiEnv,
        thread: ThreadId,
        method: MethodId,
        location: u32,
        class: &str,
        field: &str,
        object: ObjRef,
        new_value: Option<Value>,
    ) {
        let thread = thread + 1; // JDWP reserva el id 0 para null
        let Some((field_class_id, field_id)) = self.snapshot.field_id(class, field) else { return };
        let class_id_of_method = self.snapshot.class_id_of_method(method as u64).unwrap_or(0);
        let kind = if new_value.is_some() {
            event_kind::FIELD_MODIFICATION
        } else {
            event_kind::FIELD_ACCESS
        };
        let reqs: Vec<EventRequest> = self
            .session
            .requests()
            .iter()
            .filter(|r| r.kind == kind && r.field == Some((field_class_id, field_id)))
            .cloned()
            .collect();
        let _ = self.deliver(reqs, env, move |out_id, pol, id| {
            composite_field(
                out_id,
                pol,
                kind,
                id,
                thread,
                class_id_of_method,
                method,
                location,
                field_class_id,
                field_id,
                object,
                new_value,
            )
        });
    }
}

impl<S: Read + Write> JvmtiAgent for JdwpBridge<S> {
    /// La VM arrancó: el cliente configura sus `EventRequest` (breakpoints) y hace el primer `Resume`.
    /// Si el stream ya trae esos comandos, se sirven acá; si algo falla en el I/O, se sigue sin
    /// depurar (no queremos tumbar la VM por un socket).
    fn vm_init(&mut self, env: &mut JvmtiEnv) {
        // Evento VM_START (automático): le avisa al cliente que la VM arrancó suspendida, con su hilo
        // inicial — un debugger real lo necesita para fijar su «hilo actual» antes de `cont`/`where`.
        let out_id = self.out_id();
        let start = composite_vm_start(out_id, JDWP_THREAD_ID);
        if self.stream.write_all(&start.encode()).and_then(|_| self.stream.flush()).is_err() {
            return;
        }
        let _ = self.serve_until_resume(env);
    }

    fn breakpoint(&mut self, env: &mut JvmtiEnv, thread: ThreadId, method: MethodId, location: u32) {
        let thread = thread + 1; // JDWP reserva el id 0 para null (ver JDWP_THREAD_ID)
        let class_id = self.snapshot.class_id_of_method(method as u64).unwrap_or(0);
        let reqs = self.matching(event_kind::BREAKPOINT, method, location);
        let _ = self.deliver(reqs, env, |out_id, pol, id| {
            composite_located(out_id, pol, event_kind::BREAKPOINT, id, thread, class_id, method, location)
        });
    }

    fn single_step(&mut self, env: &mut JvmtiEnv, thread: ThreadId, method: MethodId, location: u32) {
        let thread = thread + 1; // JDWP reserva el id 0 para null
        let class_id = self.snapshot.class_id_of_method(method as u64).unwrap_or(0);
        let reqs = self.matching(event_kind::SINGLE_STEP, method, location);
        // un pedido de single-step no lleva LocationOnly; si no casó por ubicación, tomamos cualquiera.
        let reqs = if reqs.is_empty() {
            self.session
                .requests()
                .iter()
                .filter(|r| r.kind == event_kind::SINGLE_STEP)
                .cloned()
                .collect()
        } else {
            reqs
        };
        let _ = self.deliver(reqs, env, |out_id, pol, id| {
            composite_located(out_id, pol, event_kind::SINGLE_STEP, id, thread, class_id, method, location)
        });
    }

    fn exception(
        &mut self,
        env: &mut JvmtiEnv,
        thread: ThreadId,
        exception: ObjRef,
        method: MethodId,
        location: u32,
    ) {
        let thread = thread + 1; // JDWP reserva el id 0 para null
        let class_id = self.snapshot.class_id_of_method(method as u64).unwrap_or(0);
        let reqs: Vec<EventRequest> = self
            .session
            .requests()
            .iter()
            .filter(|r| r.kind == event_kind::EXCEPTION)
            .cloned()
            .collect();
        let _ = self.deliver(reqs, env, |out_id, pol, id| {
            composite_exception(out_id, pol, id, thread, class_id, method, location, exception)
        });
    }

    fn field_access(
        &mut self,
        env: &mut JvmtiEnv,
        thread: ThreadId,
        method: MethodId,
        location: u32,
        class: &str,
        field: &str,
        object: ObjRef,
    ) {
        self.deliver_field(env, thread, method, location, class, field, object, None);
    }

    fn field_modification(
        &mut self,
        env: &mut JvmtiEnv,
        thread: ThreadId,
        method: MethodId,
        location: u32,
        class: &str,
        field: &str,
        object: ObjRef,
        new_value: Value,
    ) {
        self.deliver_field(env, thread, method, location, class, field, object, Some(new_value));
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::jvm::interpreter::bytecode_interpreter::jdwp::{
        error_code, event_request_command, Reader, HANDSHAKE,
    };
    use crate::jvm::interpreter::bytecode_interpreter::jvmti::{BreakpointTable, FieldWatchTable};
    use crate::jvm::interpreter::frame::{Frame, Value};
    use crate::jvm::interpreter::metaspace::MetaspaceService;
    use std::io::Cursor;

    /// Un stream dúplex de mentira: lee de un guion precargado, captura lo escrito. Alcanza para un
    /// intercambio de un solo sentido por vez (el cliente ya dejó sus comandos; la VM contesta/empuja).
    struct MockStream {
        input: Cursor<Vec<u8>>,
        output: Vec<u8>,
    }
    impl MockStream {
        fn new(script: Vec<u8>) -> Self {
            MockStream { input: Cursor::new(script), output: Vec::new() }
        }
    }
    impl Read for MockStream {
        fn read(&mut self, b: &mut [u8]) -> io::Result<usize> {
            self.input.read(b)
        }
    }
    impl Write for MockStream {
        fn write(&mut self, b: &[u8]) -> io::Result<usize> {
            self.output.extend_from_slice(b);
            Ok(b.len())
        }
        fn flush(&mut self) -> io::Result<()> {
            Ok(())
        }
    }

    fn cmd(cs: u8, c: u8, data: Vec<u8>) -> Vec<u8> {
        Packet::Command(CommandPacket { id: 1, command_set: cs, command: c, data }).encode()
    }

    /// `EventRequest.Set` de un breakpoint en `(method, index)` con la política dada.
    fn set_breakpoint_cmd(method: u64, index: u64, pol: u8) -> Vec<u8> {
        let data = Writer::new()
            .byte(event_kind::BREAKPOINT)
            .byte(pol)
            .int(1) // 1 modificador
            .byte(7) // MOD_LOCATION_ONLY
            .byte(TYPE_TAG_CLASS)
            .id(0) // classID
            .id(method)
            .long(index as i64)
            .finish();
        cmd(command_set::EVENT_REQUEST, event_request_command::SET, data)
    }

    /// `EventRequest.Set` de un field watchpoint con un modificador `FieldOnly` `(classID, fieldID)`.
    fn set_field_watch_cmd(kind: u8, class_id: u64, field_id: u64, pol: u8) -> Vec<u8> {
        let data = Writer::new()
            .byte(kind)
            .byte(pol)
            .int(1) // 1 modificador
            .byte(9) // MOD_FIELD_ONLY
            .id(class_id)
            .id(field_id)
            .finish();
        cmd(command_set::EVENT_REQUEST, event_request_command::SET, data)
    }

    fn resume_cmd() -> Vec<u8> {
        cmd(command_set::VIRTUAL_MACHINE, vm_command::RESUME, vec![])
    }

    /// Un `env` de juguete sobre un frame de `method` con dos locales, y sus piezas debuggeables.
    fn scratch() -> (Vec<Frame>, BreakpointTable, bool, FieldWatchTable, MetaspaceService) {
        (
            vec![Frame::new(5, 2, vec![Value::Int(9), Value::Int(4)])],
            BreakpointTable::default(),
            false,
            FieldWatchTable::default(),
            MetaspaceService::new(vec![], vec![]),
        )
    }

    /// Un `CommandPacket` suelto (para `reply_for`, que toma el paquete, no bytes).
    fn probe(cs: u8, c: u8, data: Vec<u8>) -> CommandPacket {
        CommandPacket { id: 1, command_set: cs, command: c, data }
    }

    #[test]
    fn introspection_reads_threads_frames_and_locals() {
        // Pila de dos frames: tope method=7 con locales [3, 4], abajo method=5 con [9].
        let mut frames = vec![
            Frame::new(5, 1, vec![Value::Int(9)]),
            Frame::new(7, 2, vec![Value::Int(3), Value::Int(4)]),
        ];
        let mut bp = BreakpointTable::default();
        let mut ss = false;
        let mut fw = FieldWatchTable::default();
        let ms = MetaspaceService::new(vec![], vec![]);
        let env = JvmtiEnv::new(&mut frames, &mut bp, &mut ss, &mut fw, None, &[], &ms);
        let mut session = JdwpSession::new();
        let snap = VmSnapshot::default();

        // VirtualMachine.AllThreads → 1 hilo, id 1 (el 0 es null en JDWP).
        let r = reply_for(&mut session, &probe(command_set::VIRTUAL_MACHINE, vm_command::ALL_THREADS, vec![]), &env, &snap);
        let mut rd = Reader::new(&r.data);
        assert_eq!(rd.int(), Some(1));
        assert_eq!(rd.id(), Some(1));

        // ThreadReference.Name → "main".
        let r = reply_for(&mut session, &probe(command_set::THREAD_REFERENCE, thread_command::NAME, vec![]), &env, &snap);
        assert_eq!(Reader::new(&r.data).string().as_deref(), Some("main"));

        // ThreadReference.FrameCount → 2.
        let r = reply_for(&mut session, &probe(command_set::THREAD_REFERENCE, thread_command::FRAME_COUNT, vec![]), &env, &snap);
        assert_eq!(Reader::new(&r.data).int(), Some(2));

        // ThreadReference.Frames (thread 0, start 0, length -1) → 2 frames, tope primero.
        let frames_data = Writer::new().id(0).int(0).int(-1).finish();
        let r = reply_for(&mut session, &probe(command_set::THREAD_REFERENCE, thread_command::FRAMES, frames_data), &env, &snap);
        let mut rd = Reader::new(&r.data);
        assert_eq!(rd.int(), Some(2), "cantidad de frames");
        // frame 0: frameID 0, location del tope (método VM 7 → methodID JDWP 8, index 0).
        assert_eq!(rd.id(), Some(0), "frameID = profundidad 0");
        assert_eq!(rd.byte(), Some(TYPE_TAG_CLASS));
        assert_eq!(rd.id(), Some(0), "classID");
        assert_eq!(rd.id(), Some(8), "methodID JDWP del tope (VM 7 + 1)");
        assert_eq!(rd.long(), Some(0), "index");
        // frame 1: frameID 1, método VM 5 → methodID JDWP 6.
        assert_eq!(rd.id(), Some(1));
        assert_eq!(rd.byte(), Some(TYPE_TAG_CLASS));
        assert_eq!(rd.id(), Some(0));
        assert_eq!(rd.id(), Some(6), "methodID JDWP del frame de abajo (VM 5 + 1)");
        assert_eq!(rd.long(), Some(0));

        // StackFrame.GetValues (thread 1, frame 0, slots 0 y 1) → int 3, int 4.
        let get_data = Writer::new().id(1).id(0).int(2).int(0).byte(value_tag::INT).int(1).byte(value_tag::INT).finish();
        let r = reply_for(&mut session, &probe(command_set::STACK_FRAME, frame_command::GET_VALUES, get_data), &env, &snap);
        let mut rd = Reader::new(&r.data);
        assert_eq!(rd.int(), Some(2), "cantidad de valores");
        assert_eq!(rd.byte(), Some(value_tag::INT));
        assert_eq!(rd.int(), Some(3), "slot 0 del tope");
        assert_eq!(rd.byte(), Some(value_tag::INT));
        assert_eq!(rd.int(), Some(4), "slot 1 del tope");
    }

    /// Un servidor JDWP en memoria **con introspección**: rutea cada comando por `reply_for` con un
    /// `env` real (frames/bp/ss/metaspace), así el cliente JDI se prueba contra el servidor completo.
    struct InspectLoopback {
        session: JdwpSession,
        frames: Vec<Frame>,
        bp: BreakpointTable,
        ss: bool,
        fw: FieldWatchTable,
        ms: MetaspaceService,
        snapshot: VmSnapshot,
        outbox: std::collections::VecDeque<u8>,
        inbox: Vec<u8>,
        handshook: bool,
    }
    impl io::Write for InspectLoopback {
        fn write(&mut self, bytes: &[u8]) -> io::Result<usize> {
            self.inbox.extend_from_slice(bytes);
            if !self.handshook {
                if self.inbox.len() < HANDSHAKE.len() {
                    return Ok(bytes.len());
                }
                let head: Vec<u8> = self.inbox.drain(..HANDSHAKE.len()).collect();
                assert_eq!(head, HANDSHAKE);
                self.outbox.extend(HANDSHAKE);
                self.handshook = true;
            }
            while let Some(len) = Packet::framed_len(&self.inbox) {
                let packet = Packet::decode(&self.inbox).unwrap();
                self.inbox.drain(..len);
                if let Packet::Command(cmd) = packet {
                    // borrows disjuntos: session ⊥ (frames/bp/ss/fw/ms) ⊥ snapshot, por campo.
                    let env =
                        JvmtiEnv::new(&mut self.frames, &mut self.bp, &mut self.ss, &mut self.fw, None, &[], &self.ms);
                    let reply = reply_for(&mut self.session, &cmd, &env, &self.snapshot);
                    self.outbox.extend(Packet::Reply(reply).encode());
                }
            }
            Ok(bytes.len())
        }
        fn flush(&mut self) -> io::Result<()> {
            Ok(())
        }
    }
    impl io::Read for InspectLoopback {
        fn read(&mut self, out: &mut [u8]) -> io::Result<usize> {
            let n = out.len().min(self.outbox.len());
            for slot in out.iter_mut().take(n) {
                *slot = self.outbox.pop_front().unwrap();
            }
            Ok(n)
        }
    }

    #[test]
    fn the_jdi_client_reads_frames_and_locals_end_to_end() {
        use crate::jvm::interpreter::bytecode_interpreter::jdi::{StackFrame, Value as JdiValue, Vm};

        let server = InspectLoopback {
            session: JdwpSession::new(),
            frames: vec![
                Frame::new(5, 1, vec![Value::Int(9)]),
                Frame::new(7, 2, vec![Value::Int(3), Value::Int(4)]),
            ],
            bp: BreakpointTable::default(),
            ss: false,
            fw: FieldWatchTable::default(),
            ms: MetaspaceService::new(vec![], vec![]),
            snapshot: VmSnapshot::default(),
            outbox: std::collections::VecDeque::new(),
            inbox: Vec::new(),
            handshook: false,
        };
        let mut vm = Vm::attach(server).unwrap();

        let threads = vm.all_threads().unwrap();
        assert_eq!(threads, vec![1], "el threadID es 1 (el 0 es null en JDWP)");
        assert_eq!(vm.thread_name(1).unwrap(), "main");
        assert_eq!(vm.frame_count(1).unwrap(), 2);

        let frames = vm.frames(1).unwrap();
        assert_eq!(frames.len(), 2);
        // El methodID que ve el cliente es el de la VM + 1 (JDWP reserva el 0).
        assert_eq!(frames[0], StackFrame { id: 0, location: super::super::jdi::Location { method: 8, index: 0 } });
        assert_eq!(frames[1].location.method, 6);

        // Los locales del frame tope (thread 1, frame 0): slot 0 = 3, slot 1 = 4.
        assert_eq!(vm.get_value(1, 0, 0, b'I').unwrap(), JdiValue::Int(3));
        assert_eq!(vm.get_value(1, 0, 1, b'I').unwrap(), JdiValue::Int(4));
    }

    #[test]
    fn the_jdi_client_resolves_a_line_breakpoint_end_to_end() {
        use crate::jvm::class_file::ClassFile;
        use crate::jvm::interpreter::bytecode_interpreter::debug_info::binary_to_signature;
        use crate::jvm::interpreter::bytecode_interpreter::jdi::Vm;
        use std::path::PathBuf;

        // Snapshot real de java/Add.class (cwd = raíz del repo en `cargo test`).
        let cf = match ClassFile::from_path("java/Add.class") {
            Ok(cf) => cf,
            Err(_) => return, // sin el fixture, el test no aplica
        };
        let name = cf.class_name(cf.this_class).unwrap().to_string();
        let mut ms = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        ms.add(name.clone(), cf);
        let real_add = ms.resolve_method(&name, "add", "(II)I").unwrap();
        let snapshot = VmSnapshot::capture(&mut ms);

        let server = InspectLoopback {
            session: JdwpSession::new(),
            frames: Vec::new(),
            bp: BreakpointTable::default(),
            ss: false,
            fw: FieldWatchTable::default(),
            ms,
            snapshot,
            outbox: std::collections::VecDeque::new(),
            inbox: Vec::new(),
            handshook: false,
        };
        let mut vm = Vm::attach(server).unwrap();

        // La cadena de resolución de un cliente real: firma → classID → methodID → line table.
        let class_ids = vm.classes_by_signature(&binary_to_signature(&name)).unwrap();
        assert_eq!(class_ids.len(), 1, "la clase resuelve por firma");
        let class_id = class_ids[0];
        let methods = vm.methods(class_id).unwrap();
        let add = methods.iter().find(|m| m.name == "add").expect("add está");
        // El methodID que ve el cliente es el de la VM + 1 (el 0 es null en JDWP).
        assert_eq!(add.id, real_add as u64 + 1, "el methodID del cliente = el de la VM mapeado");
        assert_eq!(add.signature, "(II)I");
        let lines = vm.line_table(class_id, add.id).unwrap();
        assert!(!lines.is_empty(), "add trae line table → se puede poner un breakpoint por línea");
    }

    #[test]
    fn reply_for_delegates_unknown_commands_to_the_session() {
        let (mut frames, mut bp, mut ss, mut fw, ms) = scratch();
        let env = JvmtiEnv::new(&mut frames, &mut bp, &mut ss, &mut fw, None, &[], &ms);
        let mut session = JdwpSession::new();
        // Version no lo maneja el env ni el snapshot: cae a la sesión pura.
        let r = reply_for(&mut session, &probe(command_set::VIRTUAL_MACHINE, vm_command::VERSION, vec![]), &env, &VmSnapshot::default());
        assert_eq!(r.error, error_code::NONE);
        assert_eq!(Reader::new(&r.data).string().as_deref(), Some("KajiVM JDWP (Hito I3)"));
    }

    #[test]
    fn the_located_composite_serializes_an_event() {
        // (out_id, policy, kind, requestID, thread, classID, methodID, index)
        let packet = composite_located(7, suspend_policy::ALL, event_kind::BREAKPOINT, 42, 3, 9, 5, 12);
        let Packet::Command(c) = &packet else { panic!("un evento es un comando VM→cliente") };
        assert_eq!(c.command_set, command_set::EVENT);
        assert_eq!(c.command, EVENT_COMPOSITE);
        let mut r = Reader::new(&c.data);
        assert_eq!(r.byte(), Some(suspend_policy::ALL));
        assert_eq!(r.int(), Some(1), "un evento en el sobre");
        assert_eq!(r.byte(), Some(event_kind::BREAKPOINT));
        assert_eq!(r.int(), Some(42), "requestID");
        assert_eq!(r.id(), Some(3), "thread");
        assert_eq!(r.byte(), Some(TYPE_TAG_CLASS));
        assert_eq!(r.id(), Some(9), "classID");
        assert_eq!(r.id(), Some(6), "methodID JDWP = método VM 5 + 1");
        assert_eq!(r.long(), Some(12), "index");
        assert_eq!(r.remaining(), 0);
    }

    #[test]
    fn vm_init_applies_a_breakpoint_request_and_resumes() {
        // El cliente ya dejó: Set breakpoint@(5,3) + Resume.
        let mut script = set_breakpoint_cmd(5, 3, suspend_policy::ALL);
        script.extend(resume_cmd());
        let (mut frames, mut bp, mut ss, mut fw, ms) = scratch();

        let mut bridge = JdwpBridge::new(MockStream::new(script));
        {
            let mut env = JvmtiEnv::new(&mut frames, &mut bp, &mut ss, &mut fw, None, &[], &ms);
            bridge.vm_init(&mut env);
        }

        // El breakpoint quedó puesto en la VM de verdad, y el single-step apagado (no se pidió).
        // El cliente pidió methodID 5 (JDWP); en la VM eso es el método 4 (from_jdwp_method).
        assert!(bp.contains(4, 3), "el Set del cliente se volvió un set_breakpoint real");
        assert!(!ss);
        // La salida arranca con el evento automático VM_START; lo salteamos.
        let out = &bridge.stream.output[..];
        let start_len = Packet::framed_len(out).unwrap();
        match Packet::decode(out).unwrap() {
            Packet::Command(c) => assert_eq!(c.command, EVENT_COMPOSITE, "el 1º paquete es el VM_START"),
            _ => panic!("esperaba el VM_START"),
        }
        // Luego se contestaron los dos comandos (Set → requestID, Resume → vacío).
        let out = &out[start_len..];
        let len1 = Packet::framed_len(out).unwrap();
        match Packet::decode(out).unwrap() {
            Packet::Reply(rep) => {
                assert_eq!(rep.error, error_code::NONE);
                assert_eq!(Reader::new(&rep.data).int(), Some(1), "requestID 1");
            }
            _ => panic!("esperaba el reply del Set"),
        }
        match Packet::decode(&out[len1..]).unwrap() {
            Packet::Reply(rep) => assert_eq!(rep.error, error_code::NONE),
            _ => panic!("esperaba el reply del Resume"),
        }
    }

    #[test]
    fn a_breakpoint_hit_pushes_a_composite_and_serves_until_resume() {
        // Config inicial: Set breakpoint@(5,0) + Resume. Después de reanudar, el guion trae otro Resume
        // (el que el cliente manda cuando ve el evento del breakpoint).
        let mut script = set_breakpoint_cmd(5, 0, suspend_policy::ALL);
        script.extend(resume_cmd());
        script.extend(resume_cmd());
        let (mut frames, mut bp, mut ss, mut fw, ms) = scratch();

        let mut bridge = JdwpBridge::new(MockStream::new(script));
        {
            let mut env = JvmtiEnv::new(&mut frames, &mut bp, &mut ss, &mut fw, None, &[], &ms);
            bridge.vm_init(&mut env); // consume Set + primer Resume
            let before = bridge.stream.output.len();

            // La VM pega el breakpoint en su método 4 (= el methodID JDWP 5 que pidió el cliente), loc 0.
            bridge.breakpoint(&mut env, 0, 4, 0);

            // Se empujó un Composite con el requestID del breakpoint, y se sirvió hasta el 2º Resume.
            let pushed = &bridge.stream.output[before..];
            match Packet::decode(pushed).unwrap() {
                Packet::Command(c) => {
                    assert_eq!(c.command_set, command_set::EVENT);
                    assert_eq!(c.command, EVENT_COMPOSITE);
                    let mut r = Reader::new(&c.data);
                    assert_eq!(r.byte(), Some(suspend_policy::ALL));
                    assert_eq!(r.int(), Some(1));
                    assert_eq!(r.byte(), Some(event_kind::BREAKPOINT));
                    assert_eq!(r.int(), Some(1), "requestID del breakpoint que casó");
                }
                _ => panic!("esperaba el Composite del evento"),
            }
        }
    }

    #[test]
    fn a_field_modification_watch_sets_and_fires_over_jdwp() {
        use crate::jvm::class_file::ClassFile;
        use crate::jvm::interpreter::bytecode_interpreter::debug_info::VmSnapshot;
        use std::path::PathBuf;

        let cf = match ClassFile::from_path("java/Watched.class") {
            Ok(cf) => cf,
            Err(_) => return, // sin el fixture, el test no aplica
        };
        let name = cf.class_name(cf.this_class).unwrap().to_string();
        let mut ms = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        ms.add(name.clone(), cf);
        let run = ms.resolve_method(&name, "run", "()I").unwrap();
        let snapshot = VmSnapshot::capture(&mut ms);
        let (class_id, field_id) = snapshot.field_id("Watched", "value").expect("Watched.value");

        // El cliente pide vigilar la escritura de Watched.value (por `(classID, fieldID)`), resume, y
        // —tras el evento— resume de nuevo.
        let mut script =
            set_field_watch_cmd(event_kind::FIELD_MODIFICATION, class_id, field_id, suspend_policy::ALL);
        script.extend(resume_cmd());
        script.extend(resume_cmd());

        let (mut frames, mut bp, mut ss, mut fw, ms2) = scratch();
        let mut bridge = JdwpBridge::with_snapshot(MockStream::new(script), snapshot);
        {
            let mut env = JvmtiEnv::new(&mut frames, &mut bp, &mut ss, &mut fw, None, &[], &ms2);
            bridge.vm_init(&mut env); // sirve el Set + Resume
        }
        assert!(fw.watches_modification("Watched", "value"), "el Set puso el watch en la VM");
        let before = bridge.stream.output.len();

        // La VM escribe `Watched.value = 7` sobre el objeto 42 (en el método run).
        {
            let mut env = JvmtiEnv::new(&mut frames, &mut bp, &mut ss, &mut fw, None, &[], &ms2);
            bridge.field_modification(&mut env, 0, run, 6, "Watched", "value", 42, Value::Int(7));
        }

        // Se empujó un Composite FIELD_MODIFICATION con el campo, el objeto y el valor a escribir.
        let pushed = &bridge.stream.output[before..];
        match Packet::decode(pushed).unwrap() {
            Packet::Command(c) => {
                assert_eq!(c.command, EVENT_COMPOSITE);
                let mut r = Reader::new(&c.data);
                assert_eq!(r.byte(), Some(suspend_policy::ALL));
                assert_eq!(r.int(), Some(1));
                assert_eq!(r.byte(), Some(event_kind::FIELD_MODIFICATION));
                let _req = r.int();
                let _thread = r.id();
                // location de código: typeTag + classID + methodID + index
                (r.byte(), r.id(), r.id(), r.long());
                // el campo: refTypeTag + classID + fieldID
                assert_eq!(r.byte(), Some(TYPE_TAG_CLASS));
                assert_eq!(r.id(), Some(class_id), "classID del campo");
                assert_eq!(r.id(), Some(field_id), "fieldID");
                // el objeto receptor (tageado)
                assert_eq!(r.byte(), Some(value_tag::OBJECT));
                assert_eq!(r.id(), Some(42), "el objeto");
                // el valor a escribir (tageado)
                assert_eq!(r.byte(), Some(value_tag::INT));
                assert_eq!(r.int(), Some(7), "valueToBe");
            }
            _ => panic!("esperaba el Composite del field modification"),
        }
    }

    #[test]
    fn clearing_a_request_removes_the_real_breakpoint() {
        // Set @(5,1) + Resume  →  luego Clear + Resume.
        let mut script = set_breakpoint_cmd(5, 1, suspend_policy::ALL);
        script.extend(resume_cmd());
        let clear = Writer::new().byte(event_kind::BREAKPOINT).int(1).finish();
        script.extend(cmd(command_set::EVENT_REQUEST, event_request_command::CLEAR, clear));
        script.extend(resume_cmd());
        let (mut frames, mut bp, mut ss, mut fw, ms) = scratch();

        let mut bridge = JdwpBridge::new(MockStream::new(script));
        {
            let mut env = JvmtiEnv::new(&mut frames, &mut bp, &mut ss, &mut fw, None, &[], &ms);
            bridge.vm_init(&mut env); // Set + Resume
        }
        // methodID JDWP 5 → método 4 en la VM (from_jdwp_method).
        assert!(bp.contains(4, 1), "primero está puesto");
        {
            let mut env = JvmtiEnv::new(&mut frames, &mut bp, &mut ss, &mut fw, None, &[], &ms);
            bridge.serve_until_resume(&mut env).unwrap(); // Clear + Resume
        }
        assert!(!bp.contains(4, 1), "el Clear lo sacó de la VM");
    }
}
