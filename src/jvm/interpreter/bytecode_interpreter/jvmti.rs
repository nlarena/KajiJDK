//! **JVMTI mínimo** (Hito I0): la interfaz de servicios de depuración que la VM *expone*.
//!
//! Es el back-end del stack JPDA (JVMTI → JDWP → JDI → `jdb`). Modelo **push**: el agente
//! registra callbacks ([`JvmtiAgent`]) y la VM se los **llama** en medio de la ejecución
//! (`breakpoint`, `single_step`), pasándole un [`JvmtiEnv`] para que vuelva a inspeccionar/controlar
//! la VM.
//!
//! ## El handle acotado y el borrow checker
//!
//! El callback necesita re-entrar a la VM mientras esta está en medio de un `step()`. La VM **posee**
//! al agente, así que llamarlo pasándole la VM sería un doble `&mut self`. La salida —sin `unsafe`— es
//! que [`JvmtiEnv`] toma `&mut` de **solo** los campos debuggeables (la pila, la tabla de breakpoints,
//! el flag de single-step), **no** del `agent`: al disparar un evento la VM **destructura `self` en
//! campos disjuntos** (`agent` ⊥ los campos del `env`), y el compilador prueba que son distintos. Que
//! el `env` sea **acotado** no es solo encapsulación: es *lo que hace posible* el destructure.

use std::collections::HashSet;

use super::super::frame::{Frame, Value};
use super::super::heap::HeapService;
use super::super::metaspace::{MetaspaceService, MethodId};

/// El id de un hilo (green thread) — su `GreenThread::id`.
pub type ThreadId = usize;

/// El **offset en el heap** de un objeto (una referencia). `0` es `null`.
pub type ObjRef = usize;

/// Qué eventos quiere el agente. **Gatea el overhead**: con el cap apagado, la VM ni mira el evento
/// (el "pagás por lo que activás" de JVMTI).
#[derive(Default, Clone, Copy)]
pub struct Capabilities {
    pub breakpoint: bool,
    pub single_step: bool,
    pub method_entry: bool,
    pub method_exit: bool,
    pub exception: bool,
    pub field_access: bool,
    pub field_modification: bool,
}

impl Capabilities {
    /// ¿Hay algún evento habilitado? El *fast-path* por-opcode de `step()` lo consulta primero.
    pub fn any(&self) -> bool {
        self.breakpoint
            || self.single_step
            || self.method_entry
            || self.method_exit
            || self.exception
            || self.field_access
            || self.field_modification
    }
}

/// Los **field watchpoints** activos, por `(clase, nombre de campo)`. Separa acceso (lectura,
/// `getfield`/`getstatic`) de modificación (escritura, `putfield`/`putstatic`), como JVMTI. La clase es
/// la nombrada en el *fieldref* del bytecode.
#[derive(Default)]
pub struct FieldWatchTable {
    access: HashSet<(String, String)>,
    modification: HashSet<(String, String)>,
}

impl FieldWatchTable {
    pub fn watch_access(&mut self, class: &str, field: &str) {
        self.access.insert((class.to_string(), field.to_string()));
    }
    pub fn unwatch_access(&mut self, class: &str, field: &str) {
        self.access.remove(&(class.to_string(), field.to_string()));
    }
    pub fn watches_access(&self, class: &str, field: &str) -> bool {
        self.access.contains(&(class.to_string(), field.to_string()))
    }
    pub fn watch_modification(&mut self, class: &str, field: &str) {
        self.modification.insert((class.to_string(), field.to_string()));
    }
    pub fn unwatch_modification(&mut self, class: &str, field: &str) {
        self.modification.remove(&(class.to_string(), field.to_string()));
    }
    pub fn watches_modification(&self, class: &str, field: &str) -> bool {
        self.modification.contains(&(class.to_string(), field.to_string()))
    }
    pub fn is_empty(&self) -> bool {
        self.access.is_empty() && self.modification.is_empty()
    }
}

/// Los breakpoints activos, por `(método, offset de bytecode)`.
#[derive(Default)]
pub struct BreakpointTable {
    set: HashSet<(MethodId, u32)>,
}

impl BreakpointTable {
    pub fn insert(&mut self, method: MethodId, location: u32) {
        self.set.insert((method, location));
    }
    pub fn remove(&mut self, method: MethodId, location: u32) {
        self.set.remove(&(method, location));
    }
    pub fn contains(&self, method: MethodId, location: u32) -> bool {
        self.set.contains(&(method, location))
    }
    pub fn is_empty(&self) -> bool {
        self.set.is_empty()
    }
}

/// El handle **acotado** que el callback usa para volver a entrar a la VM (introspección + control).
/// Sostiene `&mut` de **solo** el estado debuggeable —la pila del hilo del evento, la tabla de
/// breakpoints y el flag de single-step—; **no** incluye al `agent` (por eso la VM puede
/// destructurarse en campos disjuntos) ni el scheduler. Vive solo durante el callback.
pub struct JvmtiEnv<'a> {
    frames: &'a mut Vec<Frame>,
    breakpoints: &'a mut BreakpointTable,
    single_step: &'a mut bool,
    /// Los field watchpoints activos — otro campo **disjunto** del `agent`, para que el callback pueda
    /// poner/sacar watches (el bridge lo hace al recibir un `EventRequest` de `FIELD_ACCESS`).
    field_watches: &'a mut FieldWatchTable,
    /// El **heap**, solo lectura, para inspeccionar objetos (la clase real de un objeto y sus campos).
    /// `None` cuando el `env` se fabrica sin heap (tests que no inspeccionan objetos). Otro campo
    /// disjunto del `agent`.
    heap: Option<&'a HeapService>,
    /// Los **ids de todos los hilos** del registro de la VM (para `VirtualMachine.AllThreads`). Vacío
    /// si el `env` se fabrica sin registro (tests). Otro campo disjunto.
    thread_ids: &'a [ThreadId],
    /// El *Method Area* — solo lectura, para resolver `MethodId` → nombre y bytecode (nombres en el
    /// `where`, disassembly en el `list`). Es un campo **disjunto** del `agent`, así que entra en el
    /// destructure sin romper el préstamo.
    metaspace: &'a MetaspaceService,
}

impl<'a> JvmtiEnv<'a> {
    /// Construye un `env` sobre los pedazos debuggeables. Normalmente lo arma la VM al disparar un
    /// evento; es `pub` para que una herramienta de depuración (o un test) pueda fabricar uno.
    pub fn new(
        frames: &'a mut Vec<Frame>,
        breakpoints: &'a mut BreakpointTable,
        single_step: &'a mut bool,
        field_watches: &'a mut FieldWatchTable,
        heap: Option<&'a HeapService>,
        thread_ids: &'a [ThreadId],
        metaspace: &'a MetaspaceService,
    ) -> Self {
        JvmtiEnv { frames, breakpoints, single_step, field_watches, heap, thread_ids, metaspace }
    }

    /// Los ids de todos los hilos vivos del registro de la VM (para `VirtualMachine.AllThreads` y
    /// `ThreadGroupReference.Children`). Vacío si el `env` se fabricó sin registro (tests).
    pub fn thread_ids(&self) -> &[ThreadId] {
        self.thread_ids
    }

    // ---- introspección (lectura) ----

    /// La traza de la pila del hilo del evento, de arriba (frame actual) hacia abajo: `(método, pc)`.
    pub fn stack_trace(&self) -> Vec<(MethodId, u32)> {
        self.frames.iter().rev().map(|f| (f.method(), f.pc() as u32)).collect()
    }

    /// El valor de un local del frame a `depth` niveles del tope (`0` = frame actual), o `None` si el
    /// frame o el slot no existen.
    pub fn local(&self, depth: usize, slot: u16) -> Option<Value> {
        let idx = self.frames.len().checked_sub(1 + depth)?;
        self.frames.get(idx)?.locals().get(slot as usize).copied()
    }

    /// Cuántos frames tiene la pila del hilo del evento.
    pub fn frame_count(&self) -> usize {
        self.frames.len()
    }

    /// El nombre de un método (`Clase.metodo`), para etiquetar la traza y el prompt.
    pub fn method_name(&self, method: MethodId) -> &str {
        self.metaspace.name(method)
    }

    /// El bytecode de un método, para desensamblar (`list`).
    pub fn code(&self, method: MethodId) -> &[u8] {
        self.metaspace.code(method)
    }

    /// El **nombre de la clase real** de un objeto del heap (`Value::Reference(offset)`), o `None` si es
    /// null / no hay heap / no se puede resolver. Lee el header del objeto (offset del mirror `Class`) y
    /// lo mapea a un nombre en el metaspace.
    pub fn object_class_name(&self, object: usize) -> Option<&str> {
        if object == 0 {
            return None;
        }
        let mirror = self.heap?.read_u32(object) as usize;
        self.metaspace.class_name_at_mirror(mirror)
    }

    /// Lee el **valor de un campo de instancia** de un objeto: `heap[object + field_offset]`, decidiendo
    /// el ancho/tipo por el primer byte del `descriptor` (como `getfield`). `None` sin heap.
    pub fn read_object_field(&self, object: usize, field_offset: usize, descriptor: &str) -> Option<Value> {
        let heap = self.heap?;
        let addr = object + field_offset;
        let value = match descriptor.as_bytes().first() {
            Some(b'J') => Value::Long(heap.read_u64(addr) as i64),
            Some(b'D') => Value::Double(f64::from_bits(heap.read_u64(addr))),
            Some(b'F') => Value::Float(f32::from_bits(heap.read_u32(addr))),
            Some(b'L') | Some(b'[') => Value::Reference(heap.read_u32(addr) as usize),
            _ => Value::Int(heap.read_u32(addr) as i32),
        };
        Some(value)
    }

    // ---- control (escritura) ----

    /// Pone un breakpoint en `(método, offset)`.
    pub fn set_breakpoint(&mut self, method: MethodId, location: u32) {
        self.breakpoints.insert(method, location);
    }
    /// Lo saca.
    pub fn clear_breakpoint(&mut self, method: MethodId, location: u32) {
        self.breakpoints.remove(method, location);
    }
    /// Prende/apaga el single-step (un evento antes de **cada** opcode).
    pub fn set_single_step(&mut self, on: bool) {
        *self.single_step = on;
    }

    /// Vigila la **lectura** de un campo (`class.field`) — dispara `field_access` en cada
    /// `getfield`/`getstatic` de ese campo.
    pub fn set_field_access_watch(&mut self, class: &str, field: &str) {
        self.field_watches.watch_access(class, field);
    }
    pub fn clear_field_access_watch(&mut self, class: &str, field: &str) {
        self.field_watches.unwatch_access(class, field);
    }
    /// Vigila la **escritura** de un campo — dispara `field_modification` en cada `putfield`/`putstatic`.
    pub fn set_field_modification_watch(&mut self, class: &str, field: &str) {
        self.field_watches.watch_modification(class, field);
    }
    pub fn clear_field_modification_watch(&mut self, class: &str, field: &str) {
        self.field_watches.unwatch_modification(class, field);
    }
}

/// El **agente de depuración**: los callbacks que la VM le llama (modelo *push* de JVMTI). Cada uno
/// recibe un [`JvmtiEnv`] para volver a inspeccionar/controlar la VM. Los métodos tienen *default*
/// vacío: un agente implementa solo los que le interesan.
pub trait JvmtiAgent {
    /// La VM arrancó y el agente se atacheó — el momento de poner los breakpoints iniciales.
    fn vm_init(&mut self, _env: &mut JvmtiEnv) {}
    /// A punto de ejecutar el opcode en `(method, location)`, y hay un breakpoint ahí.
    fn breakpoint(&mut self, _env: &mut JvmtiEnv, _thread: ThreadId, _method: MethodId, _location: u32) {}
    /// A punto de ejecutar un opcode con el single-step activo.
    fn single_step(&mut self, _env: &mut JvmtiEnv, _thread: ThreadId, _method: MethodId, _location: u32) {}
    /// Se **entró** a `method` (se apiló su frame por un `invoke`). No dispara para el método de
    /// entrada (su frame se apila antes de atachear) ni para un `<clinit>` sintético.
    fn method_entry(&mut self, _env: &mut JvmtiEnv, _thread: ThreadId, _method: MethodId) {}
    /// Se está por **salir** de `method` (su frame se va a popear) —por `return` **o** por unwind de
    /// excepción—. No dispara para un `<clinit>` sintético.
    fn method_exit(&mut self, _env: &mut JvmtiEnv, _thread: ThreadId, _method: MethodId) {}
    /// Se **lanzó** una excepción (el objeto `exception`) en `(method, location)`, antes de buscarle
    /// handler. Dispara tanto para un `throw` explícito como para una excepción implícita del VM.
    fn exception(
        &mut self,
        _env: &mut JvmtiEnv,
        _thread: ThreadId,
        _exception: ObjRef,
        _method: MethodId,
        _location: u32,
    ) {
    }
    /// Se está por **leer** un campo vigilado (`class.field`) en `(method, location)`. `object` es el
    /// receptor (`0` para un campo estático). Dispara antes de ejecutar el `getfield`/`getstatic`.
    fn field_access(
        &mut self,
        _env: &mut JvmtiEnv,
        _thread: ThreadId,
        _method: MethodId,
        _location: u32,
        _class: &str,
        _field: &str,
        _object: ObjRef,
    ) {
    }
    /// Se está por **escribir** un campo vigilado, con `new_value`. `object` es el receptor (`0` si es
    /// estático). Dispara antes de ejecutar el `putfield`/`putstatic`.
    fn field_modification(
        &mut self,
        _env: &mut JvmtiEnv,
        _thread: ThreadId,
        _method: MethodId,
        _location: u32,
        _class: &str,
        _field: &str,
        _object: ObjRef,
        _new_value: Value,
    ) {
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn the_breakpoint_table_and_capabilities_work() {
        let mut bp = BreakpointTable::default();
        assert!(bp.is_empty());
        bp.insert(3, 7);
        assert!(bp.contains(3, 7));
        assert!(!bp.contains(3, 8));
        bp.remove(3, 7);
        assert!(!bp.contains(3, 7) && bp.is_empty());

        let mut caps = Capabilities::default();
        assert!(!caps.any(), "sin capabilities, el fast-path corta");
        caps.breakpoint = true;
        assert!(caps.any());
        // los watchpoints de campo también prenden el fast-path
        let mut only_fields = Capabilities::default();
        only_fields.field_modification = true;
        assert!(only_fields.any());
    }

    #[test]
    fn the_field_watch_table_separates_access_from_modification() {
        let mut fw = FieldWatchTable::default();
        assert!(fw.is_empty());
        fw.watch_modification("Watched", "value");
        assert!(fw.watches_modification("Watched", "value"));
        assert!(!fw.watches_access("Watched", "value"), "vigilar escritura no vigila lectura");
        fw.watch_access("Watched", "value");
        assert!(fw.watches_access("Watched", "value"));
        fw.unwatch_modification("Watched", "value");
        assert!(!fw.watches_modification("Watched", "value"));
        assert!(!fw.is_empty(), "todavía queda el de acceso");
    }

    #[test]
    fn the_env_reads_locals_and_writes_control() {
        // Un frame de `method = 5` con dos locales `[42, 7]`.
        let mut frames = vec![Frame::new(5, 2, vec![Value::Int(42), Value::Int(7)])];
        let mut bp = BreakpointTable::default();
        let mut ss = false;
        let mut fw = FieldWatchTable::default();
        let ms = MetaspaceService::new(vec![], vec![]); // vacío: el test no toca nombres/bytecode
        let mut env = JvmtiEnv::new(&mut frames, &mut bp, &mut ss, &mut fw, None, &[], &ms);
        // lectura
        assert_eq!(env.local(0, 0), Some(Value::Int(42)));
        assert_eq!(env.local(0, 1), Some(Value::Int(7)));
        assert_eq!(env.local(0, 9), None, "slot inexistente");
        assert_eq!(env.local(9, 0), None, "depth inexistente");
        assert_eq!(env.stack_trace(), vec![(5, 0)]);
        // control
        env.set_breakpoint(5, 12);
        env.set_single_step(true);
        env.set_field_modification_watch("Watched", "value");
        drop(env);
        assert!(bp.contains(5, 12));
        assert!(ss);
        assert!(fw.watches_modification("Watched", "value"));
    }
}
