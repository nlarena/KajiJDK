//! `invokespecial` (0xb7): constructors (`<init>`), `private` methods and
//! `super.m()` — calls that are *statically* bound (never overridden). An
//! `impl JVM` method, dispatched from `step()`.

use super::{JVM, Step};
use crate::jvm::interpreter::frame::Value;
use crate::jvm::interpreter::metaspace::MetaspaceService;

impl JVM {
    /// `invokespecial` (0xb7): the constructor/super call. Like `invokestatic`, but
    /// the receiver (the object the call runs *on*) sits under the arguments on the
    /// stack and becomes the callee's local 0 (`this`). If the target class can't be
    /// loaded — `java.lang.Object.<init>` isn't on our classpath — we treat the call
    /// as a no-op: pop the receiver (and any args) and move on. That's enough to let
    /// a constructor chain bottom out at `Object.<init>` without it existing.
    pub(super) fn invokespecial(&mut self) -> Step {
        let caller = self.frame().method();
        let pc = self.frame().pc();
        let cp_index = {
            let code = self.metaspace.code(caller);
            u16::from_be_bytes([code[pc + 1], code[pc + 2]])
        };
        let caller_class = self.metaspace.class_of(caller).to_string();

        // The descriptor (from the caller's pool) tells us how many operands to
        // move/drop — available even when the callee's class can't be resolved.
        let descriptor = {
            let cf = self.metaspace.get(&caller_class).expect("caller class is loaded");
            let (_, _, d) = cf.methodref_target(cp_index).expect("invokespecial: bad methodref");
            d.to_string()
        };
        let arg_count = MetaspaceService::descriptor_arg_count(&descriptor);
        let total = arg_count + 1; // + the receiver

        match self.metaspace.resolve_call(&caller_class, cp_index) {
            // The constructor has a body: push a frame with [receiver, args...] as
            // its leading locals, just like invokestatic but receiver-first.
            Some(callee) => {
                let max_locals = self.metaspace.max_locals(callee);
                let mut locals = Vec::with_capacity(total);
                {
                    let frame = self.top();
                    for _ in 0..total {
                        locals.push(frame.pop());
                    }
                    locals.reverse();
                    // No advance: the caller's pc stays at the invoke; the callee's
                    // `return` advances it (so unwinding lands on the right pc).
                }
                // Slot widths: the receiver (1) then each parameter (long/double = 2).
                let mut widths = vec![1];
                widths.extend(MetaspaceService::param_slot_widths(&descriptor));
                // A `private synchronized` method (or a synchronized `super.m()`) locks its
                // receiver (`this`, the leading local). Constructors can't be synchronized.
                let lock = self.metaspace.is_synchronized(callee).then(|| match locals[0] {
                    Value::Reference(offset) => offset,
                    _ => panic!("synchronized instance method: receiver is not a reference"),
                });
                self.push_frame_locked(callee, max_locals, locals, &widths, lock)
            }
            // Unresolvable (e.g. Object.<init>): no frame is pushed, so this *is* the
            // whole instruction — drop the receiver + args and advance past it here.
            None => {
                let frame = self.top();
                for _ in 0..total {
                    frame.pop();
                }
                frame.advance(3);
                Step::Continue
            }
        }
    }
}
