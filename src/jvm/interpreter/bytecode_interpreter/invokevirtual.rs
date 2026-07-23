//! `invokevirtual` (0xb6): the normal, **dynamically-dispatched** instance call —
//! the method run depends on the receiver's runtime class (polymorphism), resolved
//! through the vtable. An `impl JVM` method, dispatched from `step()`.

use super::{JVM, Step};
use crate::jvm::interpreter::frame::Value;
use crate::jvm::interpreter::metaspace::MetaspaceService;
use crate::jvm::interpreter::natives;

impl JVM {
    /// `invokevirtual` (0xb6): a **dynamically-dispatched** instance call. The
    /// method that runs depends on the receiver's *runtime* class, not the static
    /// type at the call site. We read the slot from the static type's vtable, then
    /// index the *receiver's* vtable at that slot — same slot, overridden entry.
    pub(super) fn invokevirtual(&mut self) -> Step {
        let caller = self.frame().method();
        let pc = self.frame().pc();
        let cp_index = {
            let code = self.metaspace.code(caller);
            u16::from_be_bytes([code[pc + 1], code[pc + 2]])
        };
        let caller_class = self.metaspace.class_of(caller).to_string();

        // The methodref names the *static* type, method name and descriptor.
        let (static_class, name, descriptor) = {
            let cf = self.metaspace.get(&caller_class).expect("caller class is loaded");
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
        let mirror_offset = self.heap.read_u32(receiver) as usize;
        let runtime_class = self
            .metaspace
            .class_name_at_mirror(mirror_offset)
            .expect("invokevirtual: could not resolve the receiver's class")
            .to_string();

        // Slot from the static type; method from the runtime type's table. This *is*
        // the dynamic dispatch: a `Dog` and an `Animal` share the slot, differ in it.
        // A missing method is a NoSuchMethodError (linkage), not a VM crash.
        let slot = match self.metaspace.vtable_slot(&static_class, &name, &descriptor) {
            Some(slot) => slot,
            None => return self.throw_exception("java/lang/NoSuchMethodError"),
        };
        let callee = match self.metaspace.vtable_method(&runtime_class, slot) {
            Some(callee) => callee,
            None => return self.throw_exception("java/lang/NoSuchMethodError"),
        };

        // `Thread.start()` / `Thread.join()`: scheduler operations — handled here, not
        // via the native bridge, because they touch the thread list / block the caller.
        if self.metaspace.class_of(callee) == "java/lang/Thread" && descriptor == "()V" {
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
        if self.metaspace.class_of(callee) == "java/lang/Thread"
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
        if self.metaspace.class_of(callee) == "java/lang/Thread"
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
        if self.metaspace.class_of(callee) == "java/lang/Object" {
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

        // A native method has no bytecode: dispatch it to the native bridge with the
        // popped [receiver, args...], push its result, and step past the call (no
        // frame, so nothing returns to advance the pc — we do it here).
        if self.metaspace.is_native(callee) {
            let native_class = self.metaspace.class_of(callee).to_string();
            let result = natives::dispatch(
                &native_class,
                &name,
                &descriptor,
                &locals,
                &mut self.metaspace,
                &mut self.heap,
                &mut self.console,
            );
            if let Some(value) = result {
                self.top().push(value);
            }
            self.advance_past_call();
            return Step::Continue;
        }

        let max_locals = self.metaspace.max_locals(callee);
        // Slot widths: the receiver (1) then each parameter (`long`/`double` = 2).
        let mut widths = vec![1];
        widths.extend(MetaspaceService::param_slot_widths(&descriptor));
        // A `synchronized` instance method locks its receiver (`this`); otherwise no lock.
        let lock = self.metaspace.is_synchronized(callee).then_some(receiver);
        self.push_frame_locked(callee, max_locals, locals, &widths, lock)
    }
}
