//! `invokeinterface` (0xb9): dynamic dispatch through an *interface* reference.
//! Like `invokevirtual`, but the static type (an interface) has no stable vtable
//! slots, so the signature is resolved in the receiver's own table. An
//! `impl JVM` method, dispatched from `step()`.

use super::objects_operations::HEADER_SIZE;
use super::{JVM, Step};
use crate::jvm::interpreter::frame::Value;
use crate::jvm::interpreter::heap::HeapService;
use crate::jvm::interpreter::metaspace::MetaspaceService;

/// Reads a lambda object's captured values back out, at the widths their descriptors
/// imply. They were written in order right after the header when the call site ran.
fn read_captures(heap: &HeapService, object: usize, descriptors: &[String]) -> Vec<Value> {
    let mut at = object + HEADER_SIZE;
    descriptors
        .iter()
        .map(|descriptor| {
            let value = match descriptor.as_bytes().first() {
                Some(b'J') => Value::Long(heap.read_u64(at) as i64),
                Some(b'D') => Value::Double(f64::from_bits(heap.read_u64(at))),
                Some(b'F') => Value::Float(f32::from_bits(heap.read_u32(at))),
                Some(b'L' | b'[') => Value::Reference(heap.read_u32(at) as usize),
                _ => Value::Int(heap.read_u32(at) as i32),
            };
            at += capture_bytes(descriptor);
            value
        })
        .collect()
}

/// How many **heap bytes** a captured value occupies in the lambda object — 8 for a
/// category-2 primitive, 4 otherwise. Only for walking the object's layout.
fn capture_bytes(descriptor: &str) -> usize {
    match descriptor.as_bytes().first() {
        Some(b'J' | b'D') => 8,
        _ => 4,
    }
}

/// How many **local-variable slots** a captured value occupies in the callee's frame —
/// 2 for a category-2 primitive, 1 otherwise.
///
/// Deliberately separate from [`capture_bytes`]: the two answer different questions
/// (heap layout vs. frame slots) and their numbers differ. Sharing one function silently
/// placed the interface method's own arguments four slots too far along, past
/// `max_locals`, where `Frame::for_call` drops them.
fn capture_slots(descriptor: &str) -> usize {
    match descriptor.as_bytes().first() {
        Some(b'J' | b'D') => 2,
        _ => 1,
    }
}

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

        // A lambda object has no itable: its class is synthetic, minted by the call site
        // that produced it. This is where the shortcut pays off — instead of a generated
        // class forwarding to the implementation, the dispatch jumps there directly,
        // prepending the values the lambda captured. Those captures are the
        // implementation's *leading* parameters, ahead of the interface method's own.
        if let Some(shape) = self.lambdas.get(&runtime_class) {
            let implementation = shape.implementation;
            let capture_descriptors = shape.captures.clone();
            let mut operands = read_captures(&self.heap, receiver, &capture_descriptors);
            let mut widths: Vec<usize> =
                capture_descriptors.iter().map(|d| capture_slots(d)).collect();
            // The receiver itself is dropped: the implementation is a plain static, it
            // never sees the object that stood in for the interface.
            operands.extend(locals.into_iter().skip(1));
            widths.extend(MetaspaceService::param_slot_widths(&descriptor));

            let max_locals = self.metaspace.max_locals(implementation);
            return self.push_frame_locked(implementation, max_locals, operands, &widths, None);
        }

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
