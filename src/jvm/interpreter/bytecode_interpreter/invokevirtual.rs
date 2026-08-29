//! `invokevirtual` (0xb6): the normal, **dynamically-dispatched** instance call —
//! the method run depends on the receiver's runtime class (polymorphism), resolved
//! through the vtable. An `impl JVM` method, dispatched from `step()`.

use super::call_site::{CallSite, SiteKind};
use super::objects_operations::{self, HEADER_SIZE, SLOT_SIZE};
use super::{array_operations, class_operations, Exec, Step, Widths};
use crate::jvm::interpreter::frame::Value;
use crate::jvm::interpreter::metaspace::{Intrinsic, MethodId, MetaspaceService};
use crate::jvm::interpreter::{natives, strings};

impl Exec<'_> {
    /// `invokevirtual` (0xb6): a **dynamically-dispatched** instance call. The
    /// method that runs depends on the receiver's *runtime* class, not the static
    /// type at the call site. We read the slot from the static type's vtable, then
    /// index the *receiver's* vtable at that slot — same slot, overridden entry.
    ///
    /// That split is exactly what the F0 call-site cache can and cannot hold: the **slot** comes
    /// from the call site's static type and is cached; the `vtable_method(runtime_class, slot)`
    /// that follows depends on the receiver and is redone every call. Caching *that* — a
    /// monomorphic inline cache keyed on the receiver's class — is Hito F2, and
    /// [`SiteKind::Vtable`] is shaped to make room for it.
    pub(super) fn invokevirtual(&mut self) -> Step {
        let caller = self.frame().method();
        let pc = self.frame().pc();
        let cached = CallSite::unpack(self.shared.metaspace.call_site(caller, pc));

        // Cold site only: read the methodref, which names the *static* type, method name and
        // descriptor. A warm site never touches the constant pool — nor allocates these three
        // `String`s, which used to be paid on every single virtual call.
        let cold = match cached {
            Some(_) => None,
            None => Some(self.virtual_methodref(caller, pc)),
        };
        let arg_count = match (&cached, &cold) {
            (Some(site), _) => site.arg_count,
            (None, Some((_, _, descriptor))) => MetaspaceService::descriptor_arg_count(descriptor),
            (None, None) => unreachable!("a cold site always reads its methodref"),
        };

        // Pop [receiver, args...] off the caller (receiver sits under the args). No
        // advance — the caller's pc stays at the invoke; the callee's `return`
        // advances it (so an exception unwinds to the right pc).
        let total = arg_count + 1;
        let mut locals = Vec::with_capacity(total);
        {
            let frame = self.top();
            for _ in 0..total {
                locals.push(frame.pop());
            }
            locals.reverse();
        }

        // The receiver's *runtime* class comes from the `class_id` in its header
        // (the mirror offset). A null receiver is a NullPointerException — checked before any
        // resolution, exactly as before, so a null receiver still wins over a linkage error.
        let receiver = match locals[0] {
            Value::Reference(0) => return self.throw_exception("java/lang/NullPointerException"),
            Value::Reference(offset) => offset,
            _ => panic!("invokevirtual: receiver is not an object reference"),
        };

        let kind = match (cached, cold) {
            (Some(site), _) => site.kind,
            (None, Some(reference)) => match self.resolve_virtual_kind(&reference) {
                Ok(kind) => {
                    let site = CallSite { kind, arg_count, initialized: false };
                    self.shared.metaspace.set_call_site(caller, pc, site.pack());
                    kind
                }
                // A linkage error is thrown, never cached: it is not a resolved site, and the
                // class that would satisfy it may still load later.
                Err(error) => return self.throw_exception(error),
            },
            (None, None) => unreachable!("a cold site always reads its methodref"),
        };

        let callee = match kind {
            // `array.clone()` (JLS §10.7): javac emits an invokevirtual whose owner is the *array
            // type* itself (`"[I".clone:()Ljava/lang/Object;`). Array classes are synthetic — no
            // class file, no vtable — so normal resolution can't even start. Intercept before it:
            // every array is Cloneable, so this always succeeds with a shallow element copy.
            SiteKind::ArrayClone => {
                let clone =
                    array_operations::clone_array(&mut self.shared.metaspace, &mut self.shared.heap, receiver);
                self.top().push(Value::Reference(clone));
                self.advance_past_call();
                return Step::Continue;
            }
            // `MethodHandle.invoke` / `invokeExact`: **signature-polymorphic** (JVMS §2.9.3). The
            // call site's descriptor is the *real* one, so normal vtable resolution — which
            // expects the declared `(Object...)Object` — would fail. Intercepted before it, and
            // the `arg_count` the cache holds is the call site's own, which is the whole point.
            SiteKind::MethodHandleInvoke => return self.invoke_method_handle(receiver, &locals[1..]),
            // `MethodHandle.invokeWithArguments(Object[])`: a *regular* method (fixed descriptor),
            // but spreading the array and dispatching is a VM operation. Read the elements and
            // invoke the handle with them — `ConstantBootstraps.invoke` (now Java) is built on it.
            SiteKind::MethodHandleInvokeWithArguments => {
                let array = match locals[1] {
                    Value::Reference(0) => return self.throw_exception("java/lang/NullPointerException"),
                    Value::Reference(offset) => offset,
                    _ => panic!("invokeWithArguments: argument is not an array"),
                };
                let elements = self.read_reference_array(array);
                return self.invoke_method_handle(receiver, &elements);
            }
            // A `private` instance method reached by `invokevirtual` — which `javac` emits for a
            // same-class private call via nestmate access (Java 11+). It has no vtable slot and
            // isn't overridable, so per JVMS §6.5 the resolved private method *is* the selected
            // one: dispatched directly on the declaring class, no receiver's-table lookup.
            SiteKind::Direct(callee) => callee,
            // Slot from the static type (cached); method from the runtime type's table (not —
            // it's the receiver's). This *is* the dynamic dispatch: a `Dog` and an `Animal` share
            // the slot and differ in it. A missing method is a NoSuchMethodError (linkage).
            SiteKind::Vtable(slot) => {
                let mirror_offset = self.shared.heap.read_u32(receiver) as usize;
                // **The inline cache's one observation** (milestone F2). The word is already in
                // hand — it is the header this dispatch is about to index a vtable with — so
                // recording it costs one `Relaxed` store, and it is the entire profile the JIT
                // gets. By the time a method is compiled this site has run at least as many times
                // as the invocation counter demanded, so a site that really is monomorphic has
                // written the same value every one of them. See `MethodBody::receiver_classes`.
                self.shared.metaspace.set_receiver_class(caller, pc, mirror_offset as u32);
                match self.shared.metaspace.vtable_method_at_mirror(mirror_offset, slot) {
                    Some(callee) => callee,
                    None => return self.throw_exception("java/lang/NoSuchMethodError"),
                }
            }
            SiteKind::Signature(_) | SiteKind::NoTarget => {
                unreachable!("invokevirtual never records an interface or targetless site")
            }
        };

        // The methods the VM intercepts instead of running. The chain of `class_of(callee) == …
        // && name == … && descriptor == …` this replaces ran on every virtual call; the answer is
        // a property of the selected method's body, so it is decided once at its resolution (see
        // `Intrinsic`) and read here as a `Copy` tag. A subclass that *overrides*, say,
        // `Thread.start()` has its own body and so is `Intrinsic::None` — the same behaviour the
        // `class_of(callee)` test gave.
        match self.shared.metaspace.intrinsic(callee) {
            // `Thread.start()` / `Thread.join()`: scheduler operations — handled here, not
            // via the native bridge, because they touch the thread list / block the caller.
            Intrinsic::ThreadStart => {
                // A thread can only be started once. A slot for this `Thread` object
                // already existing means it was started before — even if it has since
                // terminated, since the slot persists. (JLS: restarting is illegal.)
                if self.already_started(receiver) {
                    return self.throw_exception("java/lang/IllegalThreadStateException");
                }
                self.spawn_thread(receiver);
                self.advance_past_call();
                return Step::Continue;
            }
            Intrinsic::ThreadJoin => return self.thread_join(receiver),
            // `Thread.interrupt()`: set the receiver's interrupt flag and wake it if it's parked
            // in an interruptible block. Handled here (not the native bridge) because it touches
            // the thread list and scheduler.
            Intrinsic::ThreadInterrupt => {
                self.thread_interrupt(receiver);
                self.advance_past_call();
                return Step::Continue;
            }
            // `Thread.getState()`: reads the scheduler's authoritative state and hands back the
            // matching `Thread.State` constant. Handled here (not the native bridge) because it
            // must *initialize* the `State` enum first — its `<clinit>` is what creates the
            // constant objects — which only the interpreter can drive.
            Intrinsic::ThreadGetState => {
                let state = self.thread_get_state(receiver);
                self.top().push(Value::Reference(state));
                self.advance_past_call();
                return Step::Continue;
            }
            // `Object.wait()` / `notify()` / `notifyAll()`: monitor signalling. Handled here
            // (not the native bridge) because they suspend/wake threads via the scheduler.
            Intrinsic::ObjectWait => return self.monitor_wait(receiver, None),
            Intrinsic::ObjectWaitTimed => {
                // `wait(long ms)`: the timeout is the long arg popped under the receiver.
                let ms = match locals.get(1) {
                    Some(Value::Long(v)) => *v,
                    _ => 0,
                };
                return self.monitor_wait(receiver, Some(ms));
            }
            Intrinsic::ObjectNotify => return self.monitor_notify(receiver, false),
            Intrinsic::ObjectNotifyAll => return self.monitor_notify(receiver, true),
            // `Object.clone()` (JLS §10.7): handled here, not via the native bridge, because the
            // Cloneable opt-in check must be able to *throw* (CloneNotSupportedException), which a
            // bridge native can't — only the interpreter can unwind. Reached only when the
            // receiver's class doesn't override clone (the vtable resolved to Object's); an
            // override runs as a normal frame, and its `super.clone()` lands in invokespecial's
            // twin interception.
            Intrinsic::ObjectClone => return self.object_clone(receiver),
            // APT fase 3 (capas 4-5): los dos accesores de `SymElement` que el bridge no puede
            // atender —`getKind` corre el `<clinit>` de un enum, `getEnclosedElements` re-entra al
            // intérprete—. Se resuelven acá y su resultado (un offset del heap) se empuja como una
            // referencia, igual que cualquier native interceptado. Ver `super::apt`.
            Intrinsic::SymElementGetKind => {
                let kind = self.sym_element_kind(receiver);
                self.top().push(Value::Reference(kind));
                self.advance_past_call();
                return Step::Continue;
            }
            Intrinsic::SymElementGetEnclosedElements => {
                let list = self.sym_element_enclosed(receiver);
                self.top().push(Value::Reference(list));
                self.advance_past_call();
                return Step::Continue;
            }
            _ => {}
        }

        // A native method has no bytecode: dispatch it to the native bridge with the
        // popped [receiver, args...], push its result, and step past the call (no
        // frame, so nothing returns to advance the pc — we do it here). The name and descriptor
        // are the selected method's own, which are the call site's: a vtable slot holds one
        // signature across the whole hierarchy, and a private target resolved on both.
        if self.shared.metaspace.is_native(callee) {
            let (native_class, name, descriptor) = {
                let m = &self.shared.metaspace;
                (m.class_of(callee).to_string(), m.name(callee).to_string(), m.descriptor(callee).to_string())
            };
            let result = natives::dispatch(
                &native_class,
                &name,
                &descriptor,
                &locals,
                &mut self.shared.metaspace,
                &mut self.shared.heap,
                &mut self.shared.console,
                &mut self.shared.apt,
            );
            if let Some(value) = result {
                self.top().push(value);
            }
            self.advance_past_call();
            return Step::Continue;
        }

        let max_locals = self.shared.metaspace.max_locals(callee);
        // A `synchronized` instance method locks its receiver (`this`); otherwise no lock.
        let lock = self.shared.metaspace.is_synchronized(callee).then_some(receiver);
        // Slot widths: the receiver (1) then each parameter (`long`/`double` = 2) — read off the
        // callee's precomputed table instead of re-parsing its descriptor into a fresh `Vec`.
        self.push_frame_locked(callee, max_locals, locals, Widths::OfCallee { receiver: true }, lock)
    }

    /// The `(static class, name, descriptor)` an `invokevirtual`'s `Methodref` names — the only
    /// part of a call site that still needs the constant pool, and so read **once**, when the
    /// site is cold.
    fn virtual_methodref(&mut self, caller: MethodId, pc: usize) -> (String, String, String) {
        let cp_index = {
            let code = self.shared.metaspace.code(caller);
            u16::from_be_bytes([code[pc + 1], code[pc + 2]])
        };
        let caller_class = self.shared.metaspace.class_of(caller).to_string();
        let cf = self.shared.metaspace.get(&caller_class).expect("caller class is loaded");
        let (c, n, d) = cf.methodref_target(cp_index).expect("invokevirtual: bad methodref");
        let (c, n, d) = (c.to_string(), n.to_string(), d.to_string());
        // Accesibilidad (JPMS): se chequea la **referencia simbólica** —la clase que nombra el
        // pool—, no la del receptor en runtime: es la resolución, no el despacho.
        class_operations::check_access(&self.shared.metaspace, &caller_class, &c);
        (c, n, d)
    }

    /// Classifies a cold `invokevirtual` site into the [`SiteKind`] it will keep for good: the
    /// three interceptions that must happen *before* resolution (an array's `clone`, and the two
    /// `MethodHandle` entry points whose call-site descriptor is the real one), then the JVMS
    /// §6.5 private/nestmate rule, then the ordinary vtable slot of the static type.
    ///
    /// `Err` is the linkage error to throw — the caller neither caches nor retries it.
    fn resolve_virtual_kind(
        &mut self,
        (static_class, name, descriptor): &(String, String, String),
    ) -> Result<SiteKind, &'static str> {
        if static_class.starts_with('[') && name == "clone" && descriptor == "()Ljava/lang/Object;" {
            return Ok(SiteKind::ArrayClone);
        }
        if static_class == "java/lang/invoke/MethodHandle" {
            match name.as_str() {
                "invoke" | "invokeExact" => return Ok(SiteKind::MethodHandleInvoke),
                "invokeWithArguments" => return Ok(SiteKind::MethodHandleInvokeWithArguments),
                _ => {}
            }
        }
        if self.shared.metaspace.method_is_private(static_class, name, descriptor) {
            return match self.shared.metaspace.resolve_method(static_class, name, descriptor) {
                Some(callee) => Ok(SiteKind::Direct(callee)),
                None => Err("java/lang/NoSuchMethodError"),
            };
        }
        match self.shared.metaspace.vtable_slot(static_class, name, descriptor) {
            Some(slot) => Ok(SiteKind::Vtable(slot)),
            None => Err("java/lang/NoSuchMethodError"),
        }
    }

    /// Invoke a `MethodHandle` (`invoke`/`invokeExact`). Reads the target off the handle on the
    /// heap — `owner` (a `Class` mirror), `name`, `descriptor`, `kind` — and dispatches by the
    /// reference kind (JVMS Table 5.4.3.5-A):
    /// - **6 `invokeStatic`** / **7 `invokeSpecial`**: the exact resolved method. Static takes just
    ///   `args`; special takes `[receiver, args…]`.
    /// - **5 `invokeVirtual`**: `[receiver, args…]`, dispatched through the receiver's vtable.
    /// - **8 `newInvokeSpecial`**: a constructor — allocate the object, run `<init>` on it, and the
    ///   handle's result is the new object (not the `void` return).
    ///
    /// For the frame-pushing kinds the callee's result lands on this caller's stack and the pc
    /// steps past the call, exactly as a normal invoke. Field kinds (1–4) aren't modelled yet.
    fn invoke_method_handle(&mut self, handle: usize, args: &[Value]) -> Step {
        let mh = "java/lang/invoke/MethodHandle";
        let owner_off = objects_operations::field_offset(&mut self.shared.metaspace, mh, "owner");
        let name_off = objects_operations::field_offset(&mut self.shared.metaspace, mh, "name");
        let desc_off = objects_operations::field_offset(&mut self.shared.metaspace, mh, "descriptor");
        let kind_off = objects_operations::field_offset(&mut self.shared.metaspace, mh, "kind");

        let owner_mirror = self.shared.heap.read_u32(handle + owner_off) as usize;
        let name_ref = self.shared.heap.read_u32(handle + name_off) as usize;
        let desc_ref = self.shared.heap.read_u32(handle + desc_off) as usize;
        let kind = self.shared.heap.read_u32(handle + kind_off) as i32;

        let owner_class = self
            .shared.metaspace
            .class_name_at_mirror(owner_mirror)
            .expect("MethodHandle.invoke: owner field is not a Class mirror")
            .to_string();
        let target_name = self.text_of(name_ref);
        let target_desc = self.text_of(desc_ref);
        self.ensure_initialized(&owner_class);
        if let Some(step) = self.take_pending_throw() {
            return step; // <clinit> failed → throw instead of invoking the handle target
        }

        // kind 8 (`newInvokeSpecial`): construct. Allocate, run `<init>` to completion, hand back
        // the new object — which is the handle's result, *not* the constructor's `void`.
        if kind == 8 {
            let object = objects_operations::allocate(&mut self.shared.metaspace, &mut self.shared.heap, &owner_class);
            let ctor = match self.shared.metaspace.resolve_method(&owner_class, "<init>", &target_desc) {
                Some(ctor) => ctor,
                None => return self.throw_exception("java/lang/NoSuchMethodError"),
            };
            let mut widths = vec![1];
            widths.extend(MetaspaceService::param_slot_widths(&target_desc));
            let mut init_args = vec![Value::Reference(object)];
            init_args.extend(args.iter().cloned());
            self.call_java(ctor, init_args, &widths);
            self.top().push(Value::Reference(object));
            self.advance_past_call();
            return Step::Continue;
        }

        // kinds 5/7 take a receiver (`args[0]`); kind 6 doesn't. Virtual (5) dispatches through the
        // receiver's runtime vtable; special (7) and static (6) call the exact resolved method.
        let callee = if kind == 5 {
            let receiver = match args.first() {
                Some(Value::Reference(0)) => return self.throw_exception("java/lang/NullPointerException"),
                Some(Value::Reference(offset)) => *offset,
                _ => panic!("MethodHandle.invoke (virtual): first argument is not a receiver"),
            };
            let runtime = self
                .shared.metaspace
                .class_name_at_mirror(self.shared.heap.read_u32(receiver) as usize)
                .expect("MethodHandle.invoke: receiver has no class")
                .to_string();
            let slot = match self.shared.metaspace.vtable_slot(&owner_class, &target_name, &target_desc) {
                Some(slot) => slot,
                None => return self.throw_exception("java/lang/NoSuchMethodError"),
            };
            match self.shared.metaspace.vtable_method(&runtime, slot) {
                Some(callee) => callee,
                None => return self.throw_exception("java/lang/NoSuchMethodError"),
            }
        } else {
            match self.shared.metaspace.resolve_method(&owner_class, &target_name, &target_desc) {
                Some(callee) => callee,
                None => return self.throw_exception("java/lang/NoSuchMethodError"),
            }
        };

        // A native target (e.g. a handle on `String.length`) has no bytecode: run the bridge with
        // `args` (`[receiver, …]` for 5/7, just the arguments for 6) and push its result.
        if self.shared.metaspace.is_native(callee) {
            let native_class = self.shared.metaspace.class_of(callee).to_string();
            let result = natives::dispatch(
                &native_class,
                &target_name,
                &target_desc,
                args,
                &mut self.shared.metaspace,
                &mut self.shared.heap,
                &mut self.shared.console,
                &mut self.shared.apt,
            );
            if let Some(value) = result {
                self.top().push(value);
            }
            self.advance_past_call();
            return Step::Continue;
        }

        let max_locals = self.shared.metaspace.max_locals(callee);
        // Static (6): the arguments are the locals. Virtual/special (5/7): a receiver precedes them.
        let mut widths = Vec::new();
        if kind != 6 {
            widths.push(1);
        }
        widths.extend(MetaspaceService::param_slot_widths(&target_desc));
        self.push_frame_locked(callee, max_locals, args.to_vec(), Widths::Slice(&widths), None)
    }

    /// `Object.clone()` (JLS §10.7), shared by the invokevirtual and invokespecial (`super.clone()`)
    /// interceptions: enforce the **Cloneable opt-in** on the receiver's *runtime* class — throw
    /// CloneNotSupportedException if it doesn't implement the marker — then shallow-copy. Arrays
    /// (reachable via a `super.clone()`-style path only in theory, but cheap to honor) are always
    /// cloneable. Pushes the clone and steps past the call, like any intercepted native.
    pub(super) fn object_clone(&mut self, receiver: usize) -> Step {
        let class = self
            .shared.metaspace
            .class_name_at_mirror(self.shared.heap.read_u32(receiver) as usize)
            .expect("clone: receiver has no class")
            .to_string();
        let clone = if class.starts_with('[') {
            array_operations::clone_array(&mut self.shared.metaspace, &mut self.shared.heap, receiver)
        } else {
            // The opt-in: a class that doesn't implement Cloneable refuses to clone (§10.7).
            // This is *why* clone is intercepted here instead of living in the native bridge —
            // a bridge native returns a value; only the interpreter can throw and unwind.
            if !class_operations::is_subtype(&mut self.shared.metaspace, &class, "java/lang/Cloneable") {
                return self.throw_exception("java/lang/CloneNotSupportedException");
            }
            objects_operations::clone_instance(&mut self.shared.metaspace, &mut self.shared.heap, receiver, &class)
        };
        self.top().push(Value::Reference(clone));
        self.advance_past_call();
        Step::Continue
    }

    /// Reads a reference array (an `Object[]`) into a `Vec<Value>` of its elements — the spread a
    /// `MethodHandle.invokeWithArguments` needs. Layout: `[class_id | mark | length | elements…]`.
    fn read_reference_array(&self, array: usize) -> Vec<Value> {
        let length = self.shared.heap.read_u32(array + HEADER_SIZE) as usize;
        let base = array + HEADER_SIZE + SLOT_SIZE;
        (0..length)
            .map(|i| Value::Reference(self.shared.heap.read_u32(base + i * SLOT_SIZE) as usize))
            .collect()
    }

    /// Materialises a `MethodHandle` object on the heap from a resolved handle reference — its
    /// `owner` (the declaring class's mirror), `name`, `descriptor` and `kind`. This is what the
    /// VM hands a bootstrap method (a condy's target handle), and — later — what `ldc` of a
    /// `MethodHandle` constant will build. Returns the object offset.
    ///
    pub(super) fn materialize_method_handle(
        &mut self,
        kind: i32,
        class: &str,
        name: &str,
        descriptor: &str,
    ) -> usize {
        // The owner must be loaded so it has a `Class` mirror (which `invoke` reads to name it).
        class_operations::load_class(&mut self.shared.metaspace, &mut self.shared.heap, class);
        let owner_mirror = self.shared.metaspace.class_mirror(class).unwrap_or(0);

        // Old-allocated (see `allocate_old`): the handle is held in a Rust local across the arg
        // resolution and array build that follow, so it must not move under a minor GC. Its
        // reference fields go in through the write barrier so Old→young pointers are remembered.
        let mh = "java/lang/invoke/MethodHandle";
        class_operations::load_class(&mut self.shared.metaspace, &mut self.shared.heap, mh);
        let handle = objects_operations::allocate_old(&mut self.shared.metaspace, &mut self.shared.heap, mh);

        let name_ref = strings::intern(&mut self.shared.metaspace, &mut self.shared.heap, name);
        let desc_ref = strings::intern(&mut self.shared.metaspace, &mut self.shared.heap, descriptor);
        let owner_off = objects_operations::field_offset(&mut self.shared.metaspace, mh, "owner");
        let name_off = objects_operations::field_offset(&mut self.shared.metaspace, mh, "name");
        let desc_off = objects_operations::field_offset(&mut self.shared.metaspace, mh, "descriptor");
        let kind_off = objects_operations::field_offset(&mut self.shared.metaspace, mh, "kind");
        self.shared.heap.store_reference(handle, handle + owner_off, owner_mirror);
        self.shared.heap.store_reference(handle, handle + name_off, name_ref);
        self.shared.heap.store_reference(handle, handle + desc_off, desc_ref);
        self.shared.heap.write_u32(handle + kind_off, kind as u32);
        handle
    }

    /// Materialises a `MethodType` object from a method descriptor — what `ldc` of a
    /// `CONSTANT_MethodType` constant produces. Old-allocated with the descriptor stored through
    /// the write barrier, matching [`Self::materialize_method_handle`]'s GC discipline.
    pub(super) fn materialize_method_type(&mut self, descriptor: &str) -> usize {
        let mt = "java/lang/invoke/MethodType";
        class_operations::load_class(&mut self.shared.metaspace, &mut self.shared.heap, mt);
        let object = objects_operations::allocate_old(&mut self.shared.metaspace, &mut self.shared.heap, mt);
        let desc_ref = strings::intern(&mut self.shared.metaspace, &mut self.shared.heap, descriptor);
        let desc_off = objects_operations::field_offset(&mut self.shared.metaspace, mt, "descriptor");
        self.shared.heap.store_reference(object, object + desc_off, desc_ref);
        object
    }
}
