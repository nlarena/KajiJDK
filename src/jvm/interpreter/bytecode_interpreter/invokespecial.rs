//! `invokespecial` (0xb7): constructors (`<init>`), `private` methods and
//! `super.m()` — calls that are *statically* bound (never overridden). An
//! `impl JVM` method, dispatched from `step()`.

use super::call_site::{CallSite, SiteKind};
use super::{Exec, Step, Widths};
use crate::jvm::interpreter::frame::Value;
use crate::jvm::interpreter::metaspace::{Intrinsic, MethodId, MetaspaceService};

impl Exec<'_> {
    /// `invokespecial` (0xb7): the constructor/super call. Like `invokestatic`, but
    /// the receiver (the object the call runs *on*) sits under the arguments on the
    /// stack and becomes the callee's local 0 (`this`). If the target class can't be
    /// loaded — `java.lang.Object.<init>` isn't on our classpath — we treat the call
    /// as a no-op: pop the receiver (and any args) and move on. That's enough to let
    /// a constructor chain bottom out at `Object.<init>` without it existing.
    ///
    /// Statically bound means the whole decision is a property of the site, so it is resolved
    /// once into the call-site cache (F0 quickening) — including the "no target" case, which
    /// caches the operand count it must drop.
    pub(super) fn invokespecial(&mut self) -> Step {
        let caller = self.frame().method();
        let pc = self.frame().pc();

        let site = match CallSite::unpack(self.shared.metaspace.call_site(caller, pc)) {
            Some(site) => site,
            None => self.resolve_special_site(caller, pc),
        };

        let callee = match site.kind {
            SiteKind::Direct(callee) => callee,
            // Unresolvable (e.g. Object.<init>): no frame is pushed, so this *is* the
            // whole instruction — drop the receiver + args and advance past it here.
            _ => {
                let frame = self.top();
                for _ in 0..site.arg_count + 1 {
                    frame.pop();
                }
                frame.advance(3);
                return Step::Continue;
            }
        };

        // `super.clone()` resolving to `Object.clone` (JLS §10.7): the statically-bound
        // call an override makes to get the field-copied object. Object.clone is native
        // (no Code to frame) and must be able to throw CloneNotSupportedException, so it
        // runs in the interpreter — same interception as invokevirtual's (see
        // `object_clone`). Pop the receiver and hand it over. Which method that is was decided
        // when `Object.clone`'s body was resolved, so this is a tag compare, not three string ones.
        if self.shared.metaspace.intrinsic(callee) == Intrinsic::ObjectClone {
            let receiver = match self.top().pop() {
                Value::Reference(0) => return self.throw_exception("java/lang/NullPointerException"),
                Value::Reference(offset) => offset,
                _ => panic!("invokespecial clone: receiver is not an object reference"),
            };
            return self.object_clone(receiver);
        }

        // The constructor has a body: push a frame with [receiver, args...] as
        // its leading locals, just like invokestatic but receiver-first.
        let max_locals = self.shared.metaspace.max_locals(callee);
        let total = self.shared.metaspace.arg_count(callee) + 1; // + the receiver
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
        // A `private synchronized` method (or a synchronized `super.m()`) locks its
        // receiver (`this`, the leading local). Constructors can't be synchronized.
        let lock = self.shared.metaspace.is_synchronized(callee).then(|| match locals[0] {
            Value::Reference(offset) => offset,
            _ => panic!("synchronized instance method: receiver is not a reference"),
        });
        // Slot widths: the receiver (1) then each parameter (long/double = 2) — the callee's own
        // precomputed table, so no descriptor re-parse per call.
        self.push_frame_locked(callee, max_locals, locals, Widths::OfCallee { receiver: true }, lock)
    }

    /// The cold half of [`Self::invokespecial`]: read the constant-pool index, resolve the
    /// `Methodref`, and record the outcome — a resolved [`SiteKind::Direct`], or
    /// [`SiteKind::NoTarget`] with the operand count to drop when the class can't be loaded.
    ///
    /// The "no target" case is cached too, and safely so: it is not a *linkage error* being
    /// papered over but this VM's standing decision that a constructor chain may bottom out at a
    /// `java.lang.Object.<init>` that isn't on the classpath. Its operand count comes from the
    /// call site's own descriptor, which is available even with no callee to ask.
    fn resolve_special_site(&mut self, caller: MethodId, pc: usize) -> CallSite {
        let cp_index = {
            let code = self.shared.metaspace.code(caller);
            u16::from_be_bytes([code[pc + 1], code[pc + 2]])
        };
        let caller_class = self.shared.metaspace.class_of(caller).to_string();
        // The descriptor tells us how many operands to move/drop — available even when the
        // callee's class can't be resolved.
        let descriptor = {
            let cf = self.shared.metaspace.get(&caller_class).expect("caller class is loaded");
            let (_, _, d) = cf.methodref_target(cp_index).expect("invokespecial: bad methodref");
            d.to_string()
        };
        let kind = match self.shared.metaspace.resolve_call(&caller_class, cp_index) {
            Some(callee) => SiteKind::Direct(callee),
            None => SiteKind::NoTarget,
        };
        let site = CallSite {
            kind,
            arg_count: MetaspaceService::descriptor_arg_count(&descriptor),
            initialized: false, // `invokespecial` never initializes — the bit is unused here.
        };
        self.shared.metaspace.set_call_site(caller, pc, site.pack());
        site
    }
}
