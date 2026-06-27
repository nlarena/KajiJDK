//! Branch opcodes — the control-flow family (`goto`, the `if_*` conditionals).
//! Unlike the arithmetic and variable families, these don't (only) touch the
//! operand stack: they change the **program counter**, making it *jump* instead
//! of advance. `step()` reads the 2-byte signed offset and hands it here; the
//! function decides whether to jump (to `pc + offset`) or fall through.

use crate::jvm::interpreter::frame::{Frame, Value};

/// `goto` (0xa7): unconditional jump to `pc + offset`. The offset is relative to
/// the goto's own pc — which is where the frame still points — so a negative
/// offset jumps *backward*, the way a loop's back-edge returns to its condition.
pub fn goto(frame: &mut Frame, offset: i16) {
    jump_to(frame, offset);
}

/// `if_icmpgt` (0xa3): pop `b` then `a` (the top is the second operand) and jump
/// if `a > b`; otherwise fall through past this 3-byte branch. The pop order
/// matters — the comparison is `a > b` in the order the values were pushed,
/// exactly like `isub` computes `a - b`.
pub fn if_icmpgt(frame: &mut Frame, offset: i16) {
    if_icmp(frame, offset, |a, b| a > b);
}

/// `if_icmpeq` (0x9f): jump if `a == b`.
pub fn if_icmpeq(frame: &mut Frame, offset: i16) {
    if_icmp(frame, offset, |a, b| a == b);
}

/// `if_icmpne` (0xa0): jump if `a != b`.
pub fn if_icmpne(frame: &mut Frame, offset: i16) {
    if_icmp(frame, offset, |a, b| a != b);
}

/// `if_icmplt` (0xa1): jump if `a < b`.
pub fn if_icmplt(frame: &mut Frame, offset: i16) {
    if_icmp(frame, offset, |a, b| a < b);
}

/// `if_icmpge` (0xa2): jump if `a >= b`.
pub fn if_icmpge(frame: &mut Frame, offset: i16) {
    if_icmp(frame, offset, |a, b| a >= b);
}

/// `if_icmple` (0xa4): jump if `a <= b`.
pub fn if_icmple(frame: &mut Frame, offset: i16) {
    if_icmp(frame, offset, |a, b| a <= b);
}

/// The shared shape of the `if_icmp*` family: pop two ints (`b` on top, `a` below),
/// jump to `offset` if `test(a, b)` holds, else fall through past the 3-byte op.
fn if_icmp(frame: &mut Frame, offset: i16, test: impl Fn(i32, i32) -> bool) {
    let b = pop_int(frame);
    let a = pop_int(frame);
    if test(a, b) {
        jump_to(frame, offset);
    } else {
        frame.advance(3);
    }
}

/// `ifeq` (0x99): pop a single int and jump if it's `== 0`, else fall through. The
/// compare-with-zero form `if_icmp*` uses against an implicit 0 — this is what a
/// Java `if (booleanExpr)` compiles to (the expr leaves 0/1 on the stack).
pub fn ifeq(frame: &mut Frame, offset: i16) {
    if pop_int(frame) == 0 {
        jump_to(frame, offset);
    } else {
        frame.advance(3);
    }
}

/// `ifne` (0x9a): the complement of `ifeq` — pop an int and jump if it's `!= 0`.
pub fn ifne(frame: &mut Frame, offset: i16) {
    if pop_int(frame) != 0 {
        jump_to(frame, offset);
    } else {
        frame.advance(3);
    }
}

/// `iflt` (0x9b): pop an int and jump if it's `< 0`. These compare-with-zero forms
/// are how the JVM branches on a `lcmp`/`fcmp`/`dcmp` result (which is `-1`/`0`/`1`),
/// i.e. what `if (longA < longB)` compiles to: `lcmp; iflt`.
pub fn iflt(frame: &mut Frame, offset: i16) {
    if pop_int(frame) < 0 {
        jump_to(frame, offset);
    } else {
        frame.advance(3);
    }
}

/// `ifge` (0x9c): pop an int and jump if it's `>= 0`.
pub fn ifge(frame: &mut Frame, offset: i16) {
    if pop_int(frame) >= 0 {
        jump_to(frame, offset);
    } else {
        frame.advance(3);
    }
}

/// `ifgt` (0x9d): pop an int and jump if it's `> 0`.
pub fn ifgt(frame: &mut Frame, offset: i16) {
    if pop_int(frame) > 0 {
        jump_to(frame, offset);
    } else {
        frame.advance(3);
    }
}

/// `ifle` (0x9e): pop an int and jump if it's `<= 0`.
pub fn ifle(frame: &mut Frame, offset: i16) {
    if pop_int(frame) <= 0 {
        jump_to(frame, offset);
    } else {
        frame.advance(3);
    }
}

/// `if_acmpeq` (0xa5): pop two **references** and jump if they're the *same* object
/// (equal heap offsets) — reference identity, not value equality. What `a == b`
/// compiles to for object operands.
pub fn if_acmpeq(frame: &mut Frame, offset: i16) {
    let b = frame.pop();
    let a = frame.pop();
    if a == b {
        jump_to(frame, offset);
    } else {
        frame.advance(3);
    }
}

/// `if_acmpne` (0xa6): the complement of `if_acmpeq` — jump if the two references are
/// *different* objects.
pub fn if_acmpne(frame: &mut Frame, offset: i16) {
    let b = frame.pop();
    let a = frame.pop();
    if a != b {
        jump_to(frame, offset);
    } else {
        frame.advance(3);
    }
}

/// `ifnull` (0xc6): pop a reference and jump if it's `null` (heap offset 0). What
/// `x == null` compiles to.
pub fn ifnull(frame: &mut Frame, offset: i16) {
    if matches!(frame.pop(), Value::Reference(0)) {
        jump_to(frame, offset);
    } else {
        frame.advance(3);
    }
}

/// `ifnonnull` (0xc7): the complement — jump if the popped reference is *not* `null`.
pub fn ifnonnull(frame: &mut Frame, offset: i16) {
    if !matches!(frame.pop(), Value::Reference(0)) {
        jump_to(frame, offset);
    } else {
        frame.advance(3);
    }
}

// --- your turn -------------------------------------------------------------
// The other conditionals are `if_icmpgt` with a different test (and the same
// jump-or-fall-through shape):
//   if_icmpge (0xa2): a >= b    if_icmplt (0xa1): a < b    if_icmple (0xa4): a <= b
//   if_icmpeq (0x9f): a == b    if_icmpne (0xa0): a != b
//   the rest of the compare-with-zero forms iflt..ifle (0x9b..0x9e) vs 0.

/// Jumps the frame to the branch target `pc + offset` (offset is relative to the
/// branch's own pc, where the frame's pc still points). Shared by every branch.
fn jump_to(frame: &mut Frame, offset: i16) {
    let target = (frame.pc() as i64 + offset as i64) as usize;
    frame.jump(target);
}

/// The absolute jump target of a `tableswitch` (0xaa) / `lookupswitch` (0xab) at `pc`
/// for the popped `key`: the matching case's offset, or `default`. Both are
/// variable-length — 0–3 bytes of alignment padding after the opcode, then 32-bit
/// big-endian offsets relative to `pc`. `tableswitch` holds a contiguous `[low, high]`
/// range (indexed directly); `lookupswitch` holds sorted `match → offset` pairs
/// (scanned for an equal key). `step()` pops the key and jumps here.
pub fn switch_target(code: &[u8], pc: usize, key: i32) -> usize {
    let mut i = pc + 1 + switch_padding(pc);
    let default = i32_at(code, i);
    i += 4;
    let offset = if code[pc] == 0xaa {
        let low = i32_at(code, i);
        let high = i32_at(code, i + 4);
        i += 8;
        if key < low || key > high {
            default
        } else {
            i32_at(code, i + (key - low) as usize * 4)
        }
    } else {
        let npairs = i32_at(code, i).max(0) as usize;
        i += 4;
        let mut found = default;
        for _ in 0..npairs {
            if i32_at(code, i) == key {
                found = i32_at(code, i + 4);
                break;
            }
            i += 8;
        }
        found
    };
    (pc as i64 + offset as i64) as usize
}

/// Bytes of padding after a switch opcode so its jump table is 4-byte aligned (from the
/// start of the code array).
fn switch_padding(pc: usize) -> usize {
    (4 - ((pc + 1) % 4)) % 4
}

/// Reads a big-endian 32-bit signed value at `i` (out-of-range bytes read as 0).
fn i32_at(code: &[u8], i: usize) -> i32 {
    let b = |k: usize| code.get(k).copied().unwrap_or(0) as i32;
    (b(i) << 24) | (b(i + 1) << 16) | (b(i + 2) << 8) | b(i + 3)
}

/// Pops the top of the operand stack as an `i32`. A reference would be a type
/// error the verifier rules out, so treat it as a bug.
fn pop_int(frame: &mut Frame) -> i32 {
    match frame.pop() {
        Value::Int(v) => v,
        other => panic!("expected an int on the operand stack, found {other:?}"),
    }
}
