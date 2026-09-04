//! `invokestatic` (0xb8): the call-stack opcode for `static` methods — no receiver,
//! the target fixed at link time. Lives as an `impl JVM` method (it drives
//! the whole call stack, not just one frame), dispatched from `step()`.

use super::call_site::{CallSite, SiteKind};
use super::array_operations;
use super::class_operations;
use super::{Exec, Step, Widths};
use crate::jvm::interpreter::frame::Value;
use crate::jvm::interpreter::metaspace::{Intrinsic, MethodId};
use crate::jvm::interpreter::natives;
use crate::jvm::interpreter::strings;

impl Exec<'_> {
    /// `invokestatic` (0xb8): resolve the target static method through the
    /// metaspace (loading its class if needed), move the caller's top-of-stack
    /// arguments into a fresh callee frame's leading locals, and push it.
    ///
    /// The resolution is **quickened** (F0): the target of a given `b8` is fixed for the life of
    /// the site, so the first execution folds it into the method's call-site cache and every
    /// later one reads a `MethodId` out of a single indexed atomic load — no `String`, no
    /// `HashMap`, no intrinsic-name compares. See `super::call_site`.
    /// `Enum.valueOf(Class<E>, String)` (§8.9.3): la constante del enum con ese `name`. El
    /// `valueOf(String)` que javac genera en cada enum delega acá (mismo bytecode que javac).
    /// Sin reflexión: se inicializa la clase enum, se lee su `$VALUES` (el `E[]` sintético) y se
    /// compara el `name` de cada constante; si ninguna matchea, `IllegalArgumentException`.
    fn enum_value_of(&mut self, args: &[Value]) -> Step {
        use crate::jvm::interpreter::bytecode_interpreter::array_operations::{
            ARRAY_HEADER_SIZE, LENGTH_OFFSET,
        };
        use crate::jvm::interpreter::bytecode_interpreter::objects_operations::field_offset;
        use crate::jvm::interpreter::strings;

        let Value::Reference(class_ref) = args[0] else {
            return self.throw_exception("java/lang/IllegalArgumentException");
        };
        let enum_class = match self.shared.metaspace.class_name_at_mirror(class_ref) {
            Some(n) => n.to_string(),
            None => return self.throw_exception("java/lang/IllegalArgumentException"),
        };
        let name_ref = match args[1] {
            Value::Reference(0) => return self.throw_exception("java/lang/NullPointerException"),
            Value::Reference(r) => r,
            _ => return self.throw_exception("java/lang/IllegalArgumentException"),
        };
        let target = strings::read(&self.shared.heap, name_ref);

        // El `$VALUES` se puebla en el `<clinit>` del enum; garantizar que corrió.
        self.ensure_initialized(&enum_class);
        if let Some(step) = self.take_pending_throw() {
            return step;
        }

        let addr = class_operations::static_slot(
            &mut self.shared.metaspace,
            &mut self.shared.heap,
            &enum_class,
            "$VALUES",
        );
        let array = self.shared.heap.read_u32(addr) as usize;
        let len = self.shared.heap.read_u32(array + LENGTH_OFFSET) as usize;
        let name_off = field_offset(&mut self.shared.metaspace, &enum_class, "name");
        for i in 0..len {
            let elem = self.shared.heap.read_u32(array + ARRAY_HEADER_SIZE + i * 4) as usize;
            let elem_name = self.shared.heap.read_u32(elem + name_off) as usize;
            if strings::read(&self.shared.heap, elem_name) == target {
                self.top().push(Value::Reference(elem));
                self.advance_past_call();
                return Step::Continue;
            }
        }
        self.throw_exception("java/lang/IllegalArgumentException")
    }

    pub(super) fn invokestatic(&mut self) -> Step {
        let caller = self.frame().method();
        let pc = self.frame().pc();

        let mut site = match CallSite::unpack(self.shared.metaspace.call_site(caller, pc)) {
            Some(site) => site,
            // Cold site: resolve it the long way once, then record it.
            None => match self.resolve_static_site(caller, pc) {
                Ok(site) => site,
                Err(step) => return step, // a linkage error — thrown, not cached
            },
        };
        let callee = match site.kind {
            SiteKind::Direct(callee) => callee,
            _ => unreachable!("invokestatic sites are always statically bound"),
        };

        // Accesibilidad (JPMS): la clase dueña del método resuelto tiene que ser visible para
        // quien llama. Va acá, sobre el `callee` ya resuelto y antes de cualquier efecto.
        {
            let caller_class = self.shared.metaspace.class_of(caller).to_string();
            let callee_class = self.shared.metaspace.class_of(callee).to_string();
            class_operations::check_access(&self.shared.metaspace, &caller_class, &callee_class);
        }

        // First active use of the callee's class triggers its initialization. Once that class is
        // `Done` the check can never do anything again (JVMS §5.5 has no transition out of it),
        // so the site records the fact and stops asking. `Erroneous` deliberately never sets the
        // bit: it must keep throwing NoClassDefFoundError on every single use.
        if !site.initialized {
            if !self.shared.metaspace.declaring_class_initialized(callee) {
                let callee_class = self.shared.metaspace.class_of(callee).to_string();
                self.ensure_initialized(&callee_class);
                if let Some(step) = self.take_pending_throw() {
                    return step; // <clinit> failed → throw instead of entering the callee
                }
            }
            if self.shared.metaspace.declaring_class_initialized(callee) {
                site.initialized = true;
                self.shared.metaspace.set_call_site(caller, pc, site.pack());
            }
        }

        let arg_count = self.shared.metaspace.arg_count(callee);
        let max_locals = self.shared.metaspace.max_locals(callee);

        // Pop the arguments off the caller (top-of-stack is the *last* argument, so
        // reverse). The caller's pc is left *at* the invoke — the matching `return`
        // advances it past the call, so an exception thrown in the callee unwinds to
        // the correct pc in the caller.
        let mut args = Vec::with_capacity(arg_count);
        {
            let frame = self.top();
            for _ in 0..arg_count {
                args.push(frame.pop());
            }
            args.reverse();
        }

        // `Enum.valueOf(Class, String)` (§8.9.3): javac genera en cada enum un `valueOf(String)` que
        // delega en este método de `java.lang.Enum`. Lo interceptamos y lo resolvemos leyendo el
        // `$VALUES` de la clase enum (sin reflexión) en vez de correr el cuerpo (que en la boot lib es
        // un stub `return null`).
        if self.shared.metaspace.class_of(callee) == "java/lang/Enum"
            && self.shared.metaspace.name(callee) == "valueOf"
        {
            return self.enum_value_of(&args);
        }

        // The methods the VM intercepts instead of running. Which ones those are is a property of
        // the *callee* — decided once when its body was resolved (see `Intrinsic`) — so what used
        // to be a chain of ~7 class-name compares that every ordinary call failed is now one
        // jump table on a `Copy` tag.
        match self.shared.metaspace.intrinsic(callee) {
            // `System.gc()`: an *explicit* GC request. Flag it and consume the call — it's
            // serviced at the next safepoint (the real VM also defers, never runs it
            // inline). No args, no return value.
            Intrinsic::SystemGc => {
                self.request_gc();
                self.advance_past_call();
                return Step::Continue;
            }
            // `jdk.internal.vm.Stack.frames()`: los cuadros de la pila, de arriba hacia abajo, como
            // `"clase|metodo"`. Se resuelve aca y no en el puente de nativos porque el puente no ve
            // los frames -- y son justamente lo que hay que leer.
            //
            // Se saltea el cuadro del propio llamador de `frames()` **no**: se devuelve la pila tal
            // como esta, y quien la use decide cuantos niveles suyos descartar. Recortar aca
            // obligaria a adivinar cuantos frames de envoltorio puso el que llama, y ese numero no lo
            // sabe la VM.
            // `Reflection.getCallerClass()`: la clase del **llamador del llamador**.
            //
            // Los tres cuadros de arriba son, de la punta hacia abajo: el de `getCallerClass` no
            // --todavia no se empujo, esto corre en el sitio de llamada--, el del metodo que la
            // llamo, y el de quien llamo a ese. El que se busca es el tercero, o sea `len() - 2`.
            //
            // Devuelve null si no hay tanto: llamarla desde el metodo de entrada es legitimo y la
            // respuesta correcta ahi es "nadie", no una excepcion.
            Intrinsic::GetCallerClass => {
                let frames = self.frames();
                let mirror = if frames.len() < 2 {
                    0
                } else {
                    let m = frames[frames.len() - 2].method();
                    let owner = self.shared.metaspace.class_of(m).to_string();
                    class_operations::load_class(
                        &mut self.shared.metaspace,
                        &mut self.shared.heap,
                        &owner,
                    );
                    self.shared.metaspace.class_mirror(&owner).unwrap_or(0)
                };
                self.top().push(Value::Reference(mirror));
                self.advance_past_call();
                return Step::Continue;
            }
            Intrinsic::StackFrames => {
                let mut nombres: Vec<String> = Vec::with_capacity(self.frames().len());
                for f in self.frames().iter().rev() {
                    let m = f.method();
                    nombres.push(format!(
                        "{}|{}",
                        self.shared.metaspace.class_of(m),
                        self.shared.metaspace.name(m)
                    ));
                }
                let arr = match array_operations::allocate_array_of_class(
                    &mut self.shared.metaspace,
                    &mut self.shared.heap,
                    "[Ljava/lang/String;",
                    nombres.len(),
                ) {
                    Ok(o) => o,
                    Err(_) => 0,
                };
                if arr != 0 {
                    for (i, n) in nombres.iter().enumerate() {
                        // **Alocado, no agrupado**: `"clase|metodo"` es una cadena que este
                        // intrinseco *arma* con un `format!`, no una entrada del pool de
                        // constantes de nadie. Meterla en la tabla de literales la volveria
                        // inmortal y clavada en Old, y una traza de pila se pide en un bucle.
                        //
                        // `store_reference` y no `write_u32`: el `String` es joven y el array pudo
                        // caer en Old (`try_malloc` escala con Eden lleno), asi que hace falta la
                        // barrera de escritura para que el minor lo trate como raiz.
                        let sref = strings::allocate(
                            &mut self.shared.metaspace,
                            &mut self.shared.heap,
                            n,
                        );
                        self.shared.heap.store_reference(
                            arr,
                            arr + array_operations::ARRAY_HEADER_SIZE + i * 4,
                            sref,
                        );
                    }
                }
                self.top().push(Value::Reference(arr));
                self.advance_past_call();
                return Step::Continue;
            }
            // `System.exit(status)`: end the VM. Handled here and **not** in the native bridge for
            // a structural reason: `natives::dispatch` returns an `Option<Value>` — a value to
            // push — so it can only ever *continue* execution. Terminating means answering with a
            // `Step`, and only an interception on this side of the bridge (with `&mut self`, i.e.
            // the frame stack) can do that. `vm_exit` drops every frame and returns
            // `Step::Return(status)`, so nothing after the call runs — not the rest of the method,
            // not its `finally`, not the caller. See `Exec::vm_exit` for the semantics (and for
            // why shutdown hooks are out).
            Intrinsic::SystemExit => {
                let status = match args.first() {
                    Some(Value::Int(v)) => *v,
                    _ => 0,
                };
                return self.vm_exit(status);
            }
            // `String.valueOf(Object)`: the text of anything. Handled here rather than in the
            // native bridge for the same reason as the scheduler ops — it isn't a leaf. It
            // has to call the object's *own* `toString()`, which is a virtual call back into
            // user bytecode, and the bridge has no way to re-enter the interpreter.
            //
            // This is what `"x" + obj` needs: javac emits this call *before* the concatenation
            // call site, so the indy only ever sees Strings.
            Intrinsic::StringValueOfObject => {
                let object = match args.first() {
                    Some(Value::Reference(offset)) => *offset,
                    other => panic!("String.valueOf: expected a reference, found {other:?}"),
                };
                let text = self.text_of(object);
                // `String.valueOf` computes its result, so it is never the pooled instance.
                let offset = crate::jvm::interpreter::strings::allocate(
                    &mut self.shared.metaspace,
                    &mut self.shared.heap,
                    &text,
                );
                self.top().push(Value::Reference(offset));
                self.advance_past_call();
                return Step::Continue;
            }
            // `String.publish(built)`: how a String constructor hands back its result.
            //
            // Every other constructor answers by writing fields of `this`. A String cannot: its
            // characters sit inline and are sized when the object is allocated, and the `new`
            // opcode sizes an instance from its *declared* fields, of which `String` has none.
            // The object the constructor was handed therefore has room for nothing, and a heap
            // block does not grow in place. So the constructor builds a separate String and says
            // so here; the `return` that ends it rewrites the caller's references.
            //
            // `self.top()` is the constructor's own frame — `publish` is called from inside it —
            // so local 0 is the object `new` allocated, still untouched.
            Intrinsic::StringPublish => {
                let built = match args.first() {
                    Some(Value::Reference(offset)) => *offset,
                    other => panic!("String.publish: expected a reference, found {other:?}"),
                };
                let frame = self.top();
                let handed = match frame.load(0) {
                    Value::Reference(offset) => offset,
                    other => panic!("String.publish: local 0 is not the receiver, it is {other:?}"),
                };
                frame.set_published(handed, built);
                self.advance_past_call();
                return Step::Continue;
            }
            // `LockSupport.park()` / `unpark(Thread)`: the block/wake primitive AQS is built on.
            // Scheduler ops (they suspend/wake a thread), so handled here, not the native bridge.
            // `park`/`park(Object blocker)` block the current thread (the blocker is ignored);
            // `unpark` hands its `Thread` argument a permit.
            Intrinsic::LockSupportPark => return self.thread_park(),
            // `parkNanos(nanos)` and `parkNanos(blocker, nanos)`: the deadline is the **last**
            // argument in both, so it's read off the end rather than by position.
            Intrinsic::LockSupportParkNanos => {
                let nanos = match args.last() {
                    Some(Value::Long(n)) => *n,
                    _ => 0,
                };
                return self.thread_park_nanos(nanos);
            }
            Intrinsic::LockSupportUnpark => {
                let target = match args.first() {
                    Some(Value::Reference(offset)) => *offset,
                    _ => 0,
                };
                self.thread_unpark(target);
                self.advance_past_call();
                return Step::Continue;
            }
            // `Thread.sleep(ms)`: park the current thread (scheduler op) — handled here, not
            // the native bridge, since it suspends the thread.
            Intrinsic::ThreadSleep => {
                let ms = match args.first() {
                    Some(Value::Long(v)) => *v,
                    _ => 0,
                };
                return self.thread_sleep(ms);
            }
            // `Thread.yield()`: a cooperative scheduler already switches every opcode, so this is
            // a no-op beyond stepping past the call.
            Intrinsic::ThreadYield => {
                self.advance_past_call();
                return Step::Continue;
            }
            // `Thread.holdsLock(o)`: does the current thread own o's monitor?
            Intrinsic::ThreadHoldsLock => {
                let obj = match args.first() {
                    Some(Value::Reference(o)) => *o,
                    _ => 0,
                };
                let held = self.owns_monitor(obj);
                self.top().push(Value::Int(held as i32));
                self.advance_past_call();
                return Step::Continue;
            }
            // `Thread.currentThread()` / `Thread.nextThreadNum()`: scheduler reads — handled
            // here because they touch the thread list (and `currentThread` may allocate main's
            // Thread object), which the native bridge can't do.
            Intrinsic::ThreadCurrentThread => {
                let obj = self.thread_current();
                self.top().push(Value::Reference(obj));
                self.advance_past_call();
                return Step::Continue;
            }
            Intrinsic::ThreadNextThreadNum => {
                let id = self.next_java_thread_num();
                self.top().push(Value::Long(id));
                self.advance_past_call();
                return Step::Continue;
            }
            _ => {}
        }

        // A native static (e.g. `Math.max`, `System.arraycopy`): no bytecode — run
        // the bridge with the args, push any result, and step past the call. The name and
        // descriptor are the *resolved method's own* — which is what the call site named, since
        // resolution matches on both.
        if self.shared.metaspace.is_native(callee) {
            let (class, name, descriptor) = {
                let m = &self.shared.metaspace;
                (m.class_of(callee).to_string(), m.name(callee).to_string(), m.descriptor(callee).to_string())
            };
            // Alocar una instancia es un **uso activo** de su clase y le debe el `<clinit>`
            // (JVMS §5.5); el `new` de bytecode lo pide por su cuenta. `allocateInstance` aloca
            // desde el puente nativo, que solo tiene metaspace y heap y no puede empujar un frame
            // de Java, asi que la inicializacion se pide aca --el mismo arreglo que el acceso
            // reflexivo a un campo estatico necesito en #361--. Sin esto, un objeto deserializado
            // podia salir de una clase con sus estaticos todavia en el valor por defecto.
            if class == "java/io/ObjectStreamClass" && name == "allocateInstance" {
                if let Some(Value::Reference(mirror)) = args.first() {
                    if let Some(objetivo) =
                        self.shared.metaspace.class_name_at_mirror(*mirror).map(str::to_string)
                    {
                        self.ensure_initialized(&objetivo);
                        if let Some(step) = self.take_pending_throw() {
                            return step; // el <clinit> fallo: no se aloca nada
                        }
                    }
                }
            }
            let result = natives::dispatch(
                &class,
                &name,
                &descriptor,
                &args,
                &mut self.shared.metaspace,
                &mut self.shared.heap,
                &mut self.shared.console,
                &mut self.shared.apt,
            );
            let result = match result {
                natives::NativeOutcome::Ran(v) => v,
                natives::NativeOutcome::RanEInicializa(clase) => {
                    // El nativo cargó la clase y pide que quede inicializada. Correr el `<clinit>`
                    // acá y no adentro del nativo es lo que permite que sea un `<clinit>` de verdad
                    // —un marco empujado y drivado hasta el final— y no una imitación.
                    self.ensure_initialized(&clase);
                    if let Some(step) = self.take_pending_throw() {
                        return step; // el <clinit> falló: no hay valor que devolver
                    }
                    // El mirror, recién ahora: el `<clinit>` alocó y pudo haber corrido el GC.
                    Some(Value::Reference(natives::mirror_de_clase(
                        &mut self.shared.metaspace,
                        &mut self.shared.heap,
                        &clase,
                    )))
                }
                natives::NativeOutcome::Lanza(clase) => return self.throw_exception(&clase),
                natives::NativeOutcome::Unimplemented => {
                    return self.throw_exception("java/lang/UnsatisfiedLinkError")
                }
            };
            if let Some(value) = result {
                self.top().push(value);
            }
            self.advance_past_call();
            return Step::Continue;
        }

        // A `static synchronized` method locks the class's `Class` mirror. Ordinary statics: no
        // lock.
        //
        // The mirror has to be **prepared here** and not assumed: `ensure_initialized` above only
        // runs `<clinit>`, and a class without one --or one already `Done`, like the entry class of
        // a run that starts inside it-- never reaches `load_class` on this path. Same order as
        // `static_slot`: prepare first, look up second (COMPILER_FINDINGS #340).
        let lock = match self.shared.metaspace.is_synchronized(callee) {
            true => {
                let callee_class = self.shared.metaspace.class_of(callee).to_string();
                class_operations::load_class(
                    &mut self.shared.metaspace,
                    &mut self.shared.heap,
                    &callee_class,
                );
                Some(self.shared.metaspace.class_mirror(&callee_class).expect(
                    "static synchronized: the Class mirror exists after preparation",
                ))
            }
            false => None,
        };
        // The arguments go into the callee's locals by its own precomputed slot widths, so a
        // `long`/`double` parameter occupies two slots and the next lands past it.
        self.push_frame_locked(callee, max_locals, args, Widths::OfCallee { receiver: false }, lock)
    }

    /// The cold half of [`Self::invokestatic`]: read the `u2` constant-pool index that follows the
    /// opcode (the `00 07`), resolve the `Methodref` it names, and record the result in the
    /// call-site cache so no later execution of this `b8` repeats any of it.
    ///
    /// Resolution can fail — that's a *linkage error*, not a VM crash. If the target class can't
    /// be loaded it's a NoClassDefFoundError; if the class is there but the method isn't, a
    /// NoSuchMethodError. Both are thrown, and **neither is cached**: a failure is not a resolved
    /// site, and the same class may well load later.
    fn resolve_static_site(&mut self, caller: MethodId, pc: usize) -> Result<CallSite, Step> {
        let cp_index = {
            let code = self.shared.metaspace.code(caller);
            u16::from_be_bytes([code[pc + 1], code[pc + 2]])
        };
        let caller_class = self.shared.metaspace.class_of(caller).to_string();
        let callee = match self.shared.metaspace.resolve_call(&caller_class, cp_index) {
            Some(callee) => callee,
            None => {
                let target = self
                    .shared.metaspace
                    .get(&caller_class)
                    .and_then(|cf| cf.methodref_target(cp_index))
                    .map(|(class, _, _)| class.to_string());
                let error = match target {
                    Some(class) if self.shared.metaspace.get_or_load(&class).is_none() => {
                        "java/lang/NoClassDefFoundError"
                    }
                    _ => "java/lang/NoSuchMethodError",
                };
                return Err(self.throw_exception(error));
            }
        };
        let site = CallSite {
            kind: SiteKind::Direct(callee),
            arg_count: self.shared.metaspace.arg_count(callee),
            // Initialization is *not* part of resolution: the caller runs `ensure_initialized`
            // right after and upgrades this bit once the class reaches `Done`.
            initialized: false,
        };
        self.shared.metaspace.set_call_site(caller, pc, site.pack());
        Ok(site)
    }
}
