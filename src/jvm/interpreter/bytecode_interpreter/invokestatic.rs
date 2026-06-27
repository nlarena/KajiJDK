//! `invokestatic` (0xb8): the call-stack opcode for `static` methods — no receiver,
//! the target fixed at link time. Lives as an `impl JVM` method (it drives
//! the whole call stack, not just one frame), dispatched from `step()`.

use super::{JVM, Step};
use crate::jvm::interpreter::metaspace::MetaspaceService;
use crate::jvm::interpreter::natives;

impl JVM {
    /// `invokestatic` (0xb8): resolve the target static method through the
    /// metaspace (loading its class if needed), move the caller's top-of-stack
    /// arguments into a fresh callee frame's leading locals, and push it.
    pub(super) fn invokestatic(&mut self) -> Step {
        let caller = self.frame().method();
        let pc = self.frame().pc();

        // Read the u2 constant-pool index that follows the opcode (the `00 07`).
        let cp_index = {
            let code = self.metaspace.code(caller);
            u16::from_be_bytes([code[pc + 1], code[pc + 2]])
        };

        // Resolve the call straight from that code, against the caller's class.
        // The metaspace reads the (already parsed) Methodref and caches the result
        // under (class, index) — the JVM's "resolved constant pool": the next time
        // this same `b8 00 07` runs it's a direct (class, #7) → MethodId lookup.
        let caller_class = self.metaspace.class_of(caller).to_string();
        // Resolution can fail — that's a *linkage error*, not a VM crash. If the
        // target class can't be loaded it's a NoClassDefFoundError; if the class is
        // there but the method isn't, a NoSuchMethodError. Both are thrown.
        let callee = match self.metaspace.resolve_call(&caller_class, cp_index) {
            Some(callee) => callee,
            None => {
                let target = self
                    .metaspace
                    .get(&caller_class)
                    .and_then(|cf| cf.methodref_target(cp_index))
                    .map(|(class, _, _)| class.to_string());
                let error = match target {
                    Some(class) if self.metaspace.get_or_load(&class).is_none() => {
                        "java/lang/NoClassDefFoundError"
                    }
                    _ => "java/lang/NoSuchMethodError",
                };
                return self.throw_exception(error);
            }
        };

        // First active use of the callee's class triggers its initialization.
        let callee_class = self.metaspace.class_of(callee).to_string();
        self.ensure_initialized(&callee_class);

        // `System.gc()`: an *explicit* GC request. Flag it and consume the call — it's
        // serviced at the next safepoint (the real VM also defers, never runs it
        // inline). No args, no return value.
        if callee_class == "java/lang/System" && self.metaspace.name(callee) == "gc" {
            self.request_gc();
            self.advance_past_call();
            return Step::Continue;
        }

        let arg_count = self.metaspace.arg_count(callee);
        let max_locals = self.metaspace.max_locals(callee);

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

        // `Thread.sleep(ms)`: park the current thread (scheduler op) — handled here, not
        // the native bridge, since it suspends the thread.
        if callee_class == "java/lang/Thread" && self.metaspace.name(callee) == "sleep" {
            let ms = match args.first() {
                Some(crate::jvm::interpreter::frame::Value::Long(v)) => *v,
                _ => 0,
            };
            return self.thread_sleep(ms);
        }

        // A native static (e.g. `Math.max`, `System.arraycopy`): no bytecode — run
        // the bridge with the args, push any result, and step past the call.
        if self.metaspace.is_native(callee) {
            let (name, descriptor) = {
                let cf = self.metaspace.get(&caller_class).expect("caller class is loaded");
                let (_, n, d) = cf.methodref_target(cp_index).expect("invokestatic: bad methodref");
                (n.to_string(), d.to_string())
            };
            let result = natives::dispatch(
                &callee_class,
                &name,
                &descriptor,
                &args,
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

        // Lay the arguments into the callee's locals by their slot widths, so a
        // `long`/`double` parameter occupies two slots and the next lands past it.
        let descriptor = self
            .metaspace
            .get(&caller_class)
            .and_then(|cf| cf.methodref_target(cp_index))
            .map(|(_, _, d)| d.to_string())
            .unwrap_or_default();
        let widths = MetaspaceService::param_slot_widths(&descriptor);
        // A `static synchronized` method locks the class's `Class` mirror (already
        // allocated — `ensure_initialized` ran above). Ordinary statics: no lock.
        let lock = self.metaspace.is_synchronized(callee).then(|| {
            self.metaspace
                .class_mirror(&callee_class)
                .expect("static synchronized: the Class mirror exists after initialization")
        });
        self.push_frame_locked(callee, max_locals, args, &widths, lock)
    }
}
