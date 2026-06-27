//! `invokeinterface` (0xb9): dynamic dispatch through an *interface* reference.
//! Like `invokevirtual`, but the static type (an interface) has no stable vtable
//! slots, so the signature is resolved in the receiver's own table. An
//! `impl JVM` method, dispatched from `step()`.

use super::{JVM, Step};
use crate::jvm::interpreter::frame::Value;
use crate::jvm::interpreter::metaspace::MetaspaceService;

impl JVM {
    /// `invokeinterface` (0xb9): dynamic dispatch through an *interface* reference.
    /// Like [`JVM::invokevirtual`], but the static type is an interface,
    /// which has no vtable with stable slots (a class implements several interfaces,
    /// each numbering its methods independently). So instead of taking a slot from
    /// the static type, we resolve the signature directly in the *receiver's* own
    /// table — our stand-in for HotSpot's itable. The opcode is also **5 bytes** (a
    /// u2 index, then a historical `count` byte and a reserved `0`).
    pub(super) fn invokeinterface(&mut self) -> Step {
        let caller = self.frame().method();
        let pc = self.frame().pc();
        let cp_index = {
            let code = self.metaspace.code(caller);
            u16::from_be_bytes([code[pc + 1], code[pc + 2]])
        };
        let caller_class = self.metaspace.class_of(caller).to_string();

        // The InterfaceMethodRef gives the interface, method name and descriptor.
        let (_interface, name, descriptor) = {
            let cf = self.metaspace.get(&caller_class).expect("caller class is loaded");
            let (c, n, d) =
                cf.methodref_target(cp_index).expect("invokeinterface: bad InterfaceMethodRef");
            (c.to_string(), n.to_string(), d.to_string())
        };
        let arg_count = MetaspaceService::descriptor_arg_count(&descriptor);

        // Pop [receiver, args...]. No advance — the caller's pc stays at the invoke
        // (5 bytes here); the callee's `return` advances it, so unwinding lands on
        // the right pc.
        let total = arg_count + 1;
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
        let mirror_offset = self.heap.read_u32(receiver) as usize;
        let runtime_class = self
            .metaspace
            .class_name_at_mirror(mirror_offset)
            .expect("invokeinterface: could not resolve the receiver's class")
            .to_string();

        // No stable interface slot — find the signature in the receiver's own table.
        // A class that doesn't implement the method ⇒ NoSuchMethodError (linkage).
        let slot = match self.metaspace.vtable_slot(&runtime_class, &name, &descriptor) {
            Some(slot) => slot,
            None => return self.throw_exception("java/lang/NoSuchMethodError"),
        };
        let callee = match self.metaspace.vtable_method(&runtime_class, slot) {
            Some(callee) => callee,
            None => return self.throw_exception("java/lang/NoSuchMethodError"),
        };

        let max_locals = self.metaspace.max_locals(callee);
        // Slot widths: the receiver (1) then each parameter (`long`/`double` = 2).
        let mut widths = vec![1];
        widths.extend(MetaspaceService::param_slot_widths(&descriptor));
        // A `synchronized` implementation locks its receiver (`this`); otherwise no lock.
        let lock = self.metaspace.is_synchronized(callee).then_some(receiver);
        self.push_frame_locked(callee, max_locals, locals, &widths, lock)
    }
}
