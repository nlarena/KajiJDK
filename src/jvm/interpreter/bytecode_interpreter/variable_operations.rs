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

/// The operands of a `wide`-prefixed instruction, already decoded.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct WideOp {
    /// The opcode the prefix widens (`iload`, `istore`, `iinc`, …).
    pub op: u8,
    /// The local slot, read as **16 bits** instead of the usual 8.
    pub slot: usize,
    /// The signed increment — only meaningful when `op` is `iinc` (0x84), `0` otherwise.
    pub delta: i32,
    /// Total instruction length including the prefix: 6 for `wide iinc`, 4 otherwise.
    pub length: usize,
}

/// Decodes a `wide` (0xc4) instruction at `pc`.
///
/// `wide` is **not an instruction, it's a prefix**: it re-runs the following opcode
/// reading a 16-bit local index instead of an 8-bit one, which is the only way a
/// method with more than 256 locals can address the slots past 255. The layout is
/// `c4 <op> idx1 idx2`, plus two bytes of signed constant when the wrapped opcode is
/// `iinc`: `c4 84 idx1 idx2 const1 const2`.
///
/// Widening is purely a *decoding* concern: the handlers above all take `slot: usize`
/// and never learn how many bytes it was written in, so the same `iload`/`istore`/
/// `iinc` run either way. Kept as a pure function (like `switch_target`) so the
/// byte-level decoding can be tested without standing up an interpreter.
pub fn wide_operands(code: &[u8], pc: usize) -> WideOp {
    let op = code[pc + 1];
    let slot = u16::from_be_bytes([code[pc + 2], code[pc + 3]]) as usize;
    if op == 0x84 {
        let delta = i16::from_be_bytes([code[pc + 4], code[pc + 5]]) as i32;
        WideOp { op, slot, delta, length: 6 }
    } else {
        WideOp { op, slot, delta: 0, length: 4 }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A widened load reads its slot from two bytes, so it reaches past 255 — the
    /// whole point of the prefix — and the instruction is 4 bytes long.
    #[test]
    fn wide_load_decodes_a_16_bit_slot() {
        // wide iload #300
        let code = [0xc4, 0x15, 0x01, 0x2c];
        let w = wide_operands(&code, 0);
        assert_eq!(w, WideOp { op: 0x15, slot: 300, delta: 0, length: 4 });
    }

    /// `wide iinc` is the odd one: it carries a **signed 16-bit** constant after the
    /// index, making it 6 bytes rather than 4. Decoding it as 4 would desynchronise
    /// the program counter for the rest of the method.
    #[test]
    fn wide_iinc_decodes_a_signed_delta_and_is_six_bytes() {
        // wide iinc #300, -2
        let code = [0xc4, 0x84, 0x01, 0x2c, 0xff, 0xfe];
        let w = wide_operands(&code, 0);
        assert_eq!(w, WideOp { op: 0x84, slot: 300, delta: -2, length: 6 });
    }

    /// The prefix is what distinguishes the forms: the same slot number encoded
    /// narrow would be a different, shorter instruction. Decoding starts at `pc`, so
    /// a `wide` sitting mid-method decodes exactly like one at offset 0.
    #[test]
    fn wide_decodes_at_an_offset_into_the_method() {
        let code = [0x00, 0x00, 0xc4, 0x3a, 0xff, 0xff];
        let w = wide_operands(&code, 2);
        assert_eq!(w.op, 0x3a); // astore
        assert_eq!(w.slot, 65_535); // the widest slot the form can address
        assert_eq!(w.length, 4);
    }
}
