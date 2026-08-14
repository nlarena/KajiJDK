//! `invokeinterface` (0xb9): dynamic dispatch through an *interface* reference.
//! Like `invokevirtual`, but the static type (an interface) has no stable vtable
//! slots, so the signature is resolved in the receiver's own table. An
//! `impl JVM` method, dispatched from `step()`.

use super::call_site::{CallSite, SiteKind};
use super::{Exec, Step, Widths};
use crate::jvm::interpreter::frame::Value;
use crate::jvm::interpreter::metaspace::{MethodId, MetaspaceService, SignatureId};

impl Exec<'_> {
    /// `invokeinterface` (0xb9): dynamic dispatch through an *interface* reference.
    /// Like [`JVM::invokevirtual`], but the static type is an interface,
    /// which has no vtable with stable slots (a class implements several interfaces,
    /// each numbering its methods independently). So instead of taking a slot from
    /// the static type, we resolve the signature directly in the *receiver's* own
    /// table — our stand-in for HotSpot's itable. The opcode is also **5 bytes** (a
    /// u2 index, then a historical `count` byte and a reserved `0`).
    ///
    /// Having no stable slot is also what limits the F0 cache here: what the site can hold is the
    /// **interned signature** to search for ([`SignatureId`]) plus the operand count — enough to
    /// drop the three `String`s per call, not enough to skip the search itself.
    pub(super) fn invokeinterface(&mut self) -> Step {
        let caller = self.frame().method();
        let pc = self.frame().pc();

        let site = match CallSite::unpack(self.shared.metaspace.call_site(caller, pc)) {
            Some(site) => site,
            None => self.resolve_interface_site(caller, pc),
        };
        let signature = match site.kind {
            SiteKind::Signature(signature) => signature,
            _ => unreachable!("invokeinterface only ever records a signature site"),
        };

        // Pop [receiver, args...]. No advance — the caller's pc stays at the invoke
        // (5 bytes here); the callee's `return` advances it, so unwinding lands on
        // the right pc.
        let total = site.arg_count + 1;
        let mut locals = Vec::with_capacity(total);
        {
            let frame = self.top();
            for _ in 0..total {
                locals.push(frame.pop());
            }
            locals.reverse();
        }

        // Receiver's runtime class from its header. A null receiver is a NPE.
        let receiver = match locals[0] {
            Value::Reference(0) => return self.throw_exception("java/lang/NullPointerException"),
            Value::Reference(offset) => offset,
            _ => panic!("invokeinterface: receiver is not an object reference"),
        };
        let mirror_offset = self.shared.heap.read_u32(receiver) as usize;

        // A lambda produced by a call site is now an ordinary instance of a **spun** class that
        // implements the interface (see `lambda_factory`), so there's no special case: its SAM is
        // found in the receiver's own table like any other implementation.
        //
        // No stable interface slot — find the signature in the receiver's own table. That search
        // walks the receiver's *merged* table, so an interface `default` method inherited from a
        // superinterface is found exactly where an override would be. A class that doesn't
        // implement the method ⇒ NoSuchMethodError (linkage).
        let callee =
            match self.shared.metaspace.vtable_method_at_mirror_by_signature(mirror_offset, signature) {
                Some(callee) => callee,
                None => return self.throw_exception("java/lang/NoSuchMethodError"),
            };

        let max_locals = self.shared.metaspace.max_locals(callee);
        // A `synchronized` implementation locks its receiver (`this`); otherwise no lock.
        let lock = self.shared.metaspace.is_synchronized(callee).then_some(receiver);
        // Slot widths: the receiver (1) then each parameter (`long`/`double` = 2), off the
        // callee's own precomputed table.
        self.push_frame_locked(callee, max_locals, locals, Widths::OfCallee { receiver: true }, lock)
    }

    /// The cold half of [`Self::invokeinterface`]: read the `InterfaceMethodRef`, intern the
    /// `(name, descriptor)` it names, and record that plus the operand count. The interface it
    /// names is *not* recorded — the lookup never uses it, since the receiver's own table is
    /// what's searched. Interning cannot fail, so this site never throws.
    fn resolve_interface_site(&mut self, caller: MethodId, pc: usize) -> CallSite {
        let cp_index = {
            let code = self.shared.metaspace.code(caller);
            u16::from_be_bytes([code[pc + 1], code[pc + 2]])
        };
        let caller_class = self.shared.metaspace.class_of(caller).to_string();
        let (name, descriptor) = {
            let cf = self.shared.metaspace.get(&caller_class).expect("caller class is loaded");
            let (_, n, d) =
                cf.methodref_target(cp_index).expect("invokeinterface: bad InterfaceMethodRef");
            (n.to_string(), d.to_string())
        };
        let signature: SignatureId = self.shared.metaspace.intern_signature(&name, &descriptor);
        let site = CallSite {
            kind: SiteKind::Signature(signature),
            arg_count: MetaspaceService::descriptor_arg_count(&descriptor),
            initialized: false, // `invokeinterface` never initializes — the bit is unused here.
        };
        self.shared.metaspace.set_call_site(caller, pc, site.pack());
        site
    }
}
