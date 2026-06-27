//! Local-variable opcodes — the `iload`/`istore` family. These move ints between
//! the local-variable slots and the operand stack: `iload` copies a local *onto*
//! the stack, `istore` pops the stack *into* a local. Like the arithmetic ops,
//! each is a small function over `&mut Frame`; `step()` works out the slot from
//! the opcode and calls in here.
//!
//! Also home (for now) to `iconst`, the small-constant push — strictly a
//! different family (it produces a literal, it doesn't touch a local), but
//! grouped here until it earns its own module.

use crate::jvm::interpreter::frame::{Frame, Value};

/// `iload` (0x1a..0x1d = iload_0..iload_3, and the generic 0x15 iload): read
/// local variable `slot` and push its value onto the operand stack.
///
/// The load and the push are two separate statements on purpose:
/// `frame.push(frame.load(slot))` won't compile, because `load` borrows `frame`
/// as `&self` while `push` needs `&mut self`, and the two borrows would overlap.
/// Binding the value first lets the read finish before the write begins.
pub fn iload(frame: &mut Frame, slot: usize) {
    let value = frame.load(slot);
    frame.push(value);
}

/// `iconst_m1..iconst_5` (0x02..0x08): push the small constant baked into the
/// opcode onto the operand stack. Unlike `iload`, the value isn't read from
/// anywhere — it's a literal embedded in the program (the caller computes it as
/// `value = opcode - 0x03`). The simplest producer opcode: just a push.
pub fn iconst(frame: &mut Frame, value: i32) {
    frame.push(Value::Int(value));
}

/// `lconst_0`/`lconst_1` (0x09/0x0a): push the `long` constant `0` or `1`. The
/// `long` twin of `iconst`. `lload`/`lstore` reuse `iload`/`istore` — moving a
/// `Value` is type-agnostic; only the verifier distinguishes int from long.
pub fn lconst(frame: &mut Frame, value: i64) {
    frame.push(Value::Long(value));
}

/// `dconst_0`/`dconst_1` (0x0e/0x0f): push the `double` constant `0.0` or `1.0`.
pub fn dconst(frame: &mut Frame, value: f64) {
    frame.push(Value::Double(value));
}

/// `fconst_0`/`fconst_1`/`fconst_2` (0x0b/0x0c/0x0d): push `0.0f`/`1.0f`/`2.0f`.
pub fn fconst(frame: &mut Frame, value: f32) {
    frame.push(Value::Float(value));
}

/// `iinc` (0x84): add the signed constant `delta` to local int variable `slot`,
/// in place — it never touches the operand stack. This is what `i++` / `i += k`
/// on an `int` local compiles to (a single instruction, no load/add/store).
pub fn iinc(frame: &mut Frame, slot: usize, delta: i32) {
    match frame.load(slot) {
        Value::Int(v) => frame.store(slot, Value::Int(v.wrapping_add(delta))),
        other => panic!("iinc: local {slot} is not an int, found {other:?}"),
    }
}

/// `istore_0..istore_3` (0x3b..0x3e, and the generic 0x36): pop the top int off
/// the operand stack and write it into local variable `slot`. The mirror of
/// `iload` — where iload reads a local *onto* the stack, istore moves the stack's
/// top *into* a local, shrinking the stack. This is how a Java `=` assignment
/// lands its value in a variable.
pub fn istore(frame: &mut Frame, slot: usize) {
    let value = frame.pop();
    frame.store(slot, value);
}

/// `aload_0..aload_3` (0x2a..0x2d, and the generic 0x19): the reference-typed twin
/// of `iload` — read local `slot` and push it. Mechanically identical (a `Value`
/// is a `Value`); the JVM keeps a separate opcode only so the verifier can tell a
/// reference from an int. `aload_0` is the canonical "load `this`" at the start of
/// an instance method or constructor.
pub fn aload(frame: &mut Frame, slot: usize) {
    let value = frame.load(slot);
    frame.push(value);
}

/// `astore_0..astore_3` (0x4b..0x4e, and the generic 0x3a): the reference-typed
/// twin of `istore` — pop a reference off the stack into local `slot`. This is how
/// `Dog d = new Dog()` lands the new object's reference in its local.
pub fn astore(frame: &mut Frame, slot: usize) {
    let value = frame.pop();
    frame.store(slot, value);
}
