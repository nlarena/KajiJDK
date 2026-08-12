//! `invokevirtual` (0xb6): the normal, **dynamically-dispatched** instance call —
//! the method run depends on the receiver's runtime class (polymorphism), resolved
//! through the vtable. An `impl JVM` method, dispatched from `step()`.

use super::objects_operations::{self, HEADER_SIZE, SLOT_SIZE};
use super::{array_operations, class_operations, Exec, Step};
use crate::jvm::interpreter::frame::Value;
use crate::jvm::interpreter::metaspace::MetaspaceService;
use crate::jvm::interpreter::{natives, strings};

impl Exec<'_> {
    /// `invokevirtual` (0xb6): a **dynamically-dispatched** instance call. The
    /// method that runs depends on the receiver's *runtime* class, not the static
    /// type at the call site. We read the slot from the static type's vtable, then
    /// index the *receiver's* vtable at that slot — same slot, overridden entry.
    pub(super) fn invokevirtual(&mut self) -> Step {
        let caller = self.frame().method();
        let pc = self.frame().pc();
        let cp_index = {
            let code = self.shared.metaspace.code(caller);
            u16::from_be_bytes([code[pc + 1], code[pc + 2]])
        };
        let caller_class = self.shared.metaspace.class_of(caller).to_string();

        // The methodref names the *static* type, method name and descriptor.
        let (static_class, name, descriptor) = {
            let cf = self.shared.metaspace.get(&caller_class).expect("caller class is loaded");
            let (c, n, d) = cf.methodref_target(cp_index).expect("invokevirtual: bad methodref");
            (c.to_string(), n.to_string(), d.to_string())
        };
        let arg_count = MetaspaceService::descriptor_arg_count(&descriptor);

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
        // (the mirror offset). A null receiver is a NullPointerException.
        let receiver = match locals[0] {
            Value::Reference(0) => return self.throw_exception("java/lang/NullPointerException"),
            Value::Reference(offset) => offset,
            _ => panic!("invokevirtual: receiver is not an object reference"),
        };

        // `array.clone()` (JLS §10.7): javac emits an invokevirtual whose owner is the *array
        // type* itself (`"[I".clone:()Ljava/lang/Object;`). Array classes are synthetic — no
        // class file, no vtable — so normal resolution can't even start. Intercept before it:
        // every array is Cloneable, so this always succeeds with a shallow element copy.
        if static_class.starts_with('[') && name == "clone" && descriptor == "()Ljava/lang/Object;" {
            let clone = array_operations::clone_array(&mut self.shared.metaspace, &mut self.shared.heap, receiver);
            self.top().push(Value::Reference(clone));
            self.advance_past_call();
            return Step::Continue;
        }

        // `MethodHandle.invoke` / `invokeExact`: **signature-polymorphic** (JVMS §2.9.3). The
        // call site's descriptor is the *real* one, so normal vtable resolution — which expects
        // the declared `(Object...)Object` — would fail. Intercept before it: `locals` already
        // holds `[handle, args...]`, popped per the call-site descriptor.
        if static_class == "java/lang/invoke/MethodHandle"
            && matches!(name.as_str(), "invoke" | "invokeExact")
        {
            return self.invoke_method_handle(receiver, &locals[1..]);
        }

        // `MethodHandle.invokeWithArguments(Object[])`: a *regular* method (fixed descriptor), but
        // spreading the array and dispatching is a VM operation. Read the elements and invoke the
        // handle with them — the primitive `ConstantBootstraps.invoke` (now Java) is built on this.
        if static_class == "java/lang/invoke/MethodHandle" && name == "invokeWithArguments" {
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
        // isn't overridable, so per JVMS §6.5 the resolved private method *is* the selected one:
        // dispatch it directly on the declaring class, skipping the receiver's-table lookup.
        let callee = if self.shared.metaspace.method_is_private(&static_class, &name, &descriptor) {
            match self.shared.metaspace.resolve_method(&static_class, &name, &descriptor) {
                Some(callee) => callee,
                None => return self.throw_exception("java/lang/NoSuchMethodError"),
            }
        } else {
            let mirror_offset = self.shared.heap.read_u32(receiver) as usize;
            let runtime_class = self
                .shared.metaspace
                .class_name_at_mirror(mirror_offset)
                .expect("invokevirtual: could not resolve the receiver's class")
                .to_string();

            // Slot from the static type; method from the runtime type's table. This *is*
            // the dynamic dispatch: a `Dog` and an `Animal` share the slot, differ in it.
            // A missing method is a NoSuchMethodError (linkage), not a VM crash.
            let slot = match self.shared.metaspace.vtable_slot(&static_class, &name, &descriptor) {
                Some(slot) => slot,
                None => return self.throw_exception("java/lang/NoSuchMethodError"),
            };
            match self.shared.metaspace.vtable_method(&runtime_class, slot) {
                Some(callee) => callee,
                None => return self.throw_exception("java/lang/NoSuchMethodError"),
            }
        };

        // `Thread.start()` / `Thread.join()`: scheduler operations — handled here, not
        // via the native bridge, because they touch the thread list / block the caller.
        if self.shared.metaspace.class_of(callee) == "java/lang/Thread" && descriptor == "()V" {
            match name.as_str() {
                "start" => {
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
                "join" => return self.thread_join(receiver),
                _ => {}
            }
        }

        // `Thread.interrupt()`: set the receiver's interrupt flag and wake it if it's parked
        // in an interruptible block. Handled here (not the native bridge) because it touches
        // the thread list and scheduler.
        if self.shared.metaspace.class_of(callee) == "java/lang/Thread"
            && name == "interrupt"
            && descriptor == "()V"
        {
            self.thread_interrupt(receiver);
            self.advance_past_call();
            return Step::Continue;
        }

        // `Thread.getState()`: reads the scheduler's authoritative state and hands back the
        // matching `Thread.State` constant. Handled here (not the native bridge) because it
        // must *initialize* the `State` enum first — its `<clinit>` is what creates the
        // constant objects — which only the interpreter can drive.
        if self.shared.metaspace.class_of(callee) == "java/lang/Thread"
            && name == "getState"
            && descriptor == "()Ljava/lang/Thread$State;"
        {
            let state = self.thread_get_state(receiver);
            self.top().push(Value::Reference(state));
            self.advance_past_call();
            return Step::Continue;
        }

        // `Object.wait()` / `notify()` / `notifyAll()`: monitor signalling. Handled here
        // (not the native bridge) because they suspend/wake threads via the scheduler.
        if self.shared.metaspace.class_of(callee) == "java/lang/Object" {
            match (name.as_str(), descriptor.as_str()) {
                ("wait", "()V") => return self.monitor_wait(receiver, None),
                ("wait", "(J)V") => {
                    // `wait(long ms)`: the timeout is the long arg popped under the receiver.
                    let ms = match locals.get(1) {
                        Some(Value::Long(v)) => *v,
                        _ => 0,
                    };
                    return self.monitor_wait(receiver, Some(ms));
                }
                ("notify", "()V") => return self.monitor_notify(receiver, false),
                ("notifyAll", "()V") => return self.monitor_notify(receiver, true),
                _ => {}
            }
        }

        // `Object.clone()` (JLS §10.7): handled here, not via the native bridge, because the
        // Cloneable opt-in check must be able to *throw* (CloneNotSupportedException), which a
        // bridge native can't — only the interpreter can unwind. Reached only when the receiver's
        // class doesn't override clone (the vtable resolved to Object's); an override runs as a
        // normal frame, and its `super.clone()` lands in invokespecial's twin interception.
        if self.shared.metaspace.class_of(callee) == "java/lang/Object"
            && name == "clone"
            && descriptor == "()Ljava/lang/Object;"
        {
            return self.object_clone(receiver);
        }

        // A native method has no bytecode: dispatch it to the native bridge with the
        // popped [receiver, args...], push its result, and step past the call (no
        // frame, so nothing returns to advance the pc — we do it here).
        if self.shared.metaspace.is_native(callee) {
            let native_class = self.shared.metaspace.class_of(callee).to_string();
            let result = natives::dispatch(
                &native_class,
                &name,
                &descriptor,
                &locals,
                &mut self.shared.metaspace,
                &mut self.shared.heap,
                &mut self.shared.console,
            );
            if let Some(value) = result {
                self.top().push(value);
            }
            self.advance_past_call();
            return Step::Continue;
        }

        let max_locals = self.shared.metaspace.max_locals(callee);
        // Slot widths: the receiver (1) then each parameter (`long`/`double` = 2).
        let mut widths = vec![1];
        widths.extend(MetaspaceService::param_slot_widths(&descriptor));
        // A `synchronized` instance method locks its receiver (`this`); otherwise no lock.
        let lock = self.shared.metaspace.is_synchronized(callee).then_some(receiver);
        self.push_frame_locked(callee, max_locals, locals, &widths, lock)
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
        self.push_frame_locked(callee, max_locals, args.to_vec(), &widths, None)
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
