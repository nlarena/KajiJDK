//! The arithmetic opcodes — the `iadd`/`isub`/`imul`/`idiv`/… family. Each one is
//! a small function that works purely on the operand stack: pop the operand(s),
//! compute, push the result. They never touch the pc or control flow — `step()`
//! in the parent module matches the opcode byte, calls the function here, then
//! advances the pc — so they stay this simple.

use crate::jvm::interpreter::frame::{Frame, Value};

/// `iadd` (0x60): pop the two top ints (`b` first, then `a`) and push `a + b`.
/// The addition wraps on overflow — JVM int arithmetic is defined modulo 2^32.
///
/// This is the worked example; every other binary int op has the same shape,
/// only the operator (and, for non-commutative ones, the pop order) changes.
pub fn iadd(frame: &mut Frame) {
    let b = pop_int(frame);
    let a = pop_int(frame);
    frame.push(Value::Int(a.wrapping_add(b)));
}

/// `isub` (0x64): pop `b` then `a`, push `a - b` (wrapping). The pop order
/// matters — `b` is the top, `a` below, so the result is `a - b`; swap them and
/// you'd compute `b - a`.
pub fn isub(frame: &mut Frame) {
    let b = pop_int(frame);
    let a = pop_int(frame);
    frame.push(Value::Int(a.wrapping_sub(b)));
}

/// `imul` (0x68): pop `b` then `a`, push `a * b` (wrapping). Multiplication is
/// commutative, so the pop order doesn't change the result here.
pub fn imul(frame: &mut Frame) {
    let b = pop_int(frame);
    let a = pop_int(frame);
    frame.push(Value::Int(a.wrapping_mul(b)));
}

// --- your turn -------------------------------------------------------------
// Still to do, same shape: idiv (0x6c): a / b, irem (0x70): a % b. Division needs
// care — the JVM rounds toward zero, and INT_MIN / -1 overflows.

/// `ladd` (0x61): pop two `long`s and push `a + b` (wrapping, modulo 2^64). The
/// `long` mirror of `iadd`; `lsub`/`lmul` follow the same shape.
pub fn ladd(frame: &mut Frame) {
    let b = pop_long(frame);
    let a = pop_long(frame);
    frame.push(Value::Long(a.wrapping_add(b)));
}

/// `lsub` (0x65): pop `b` then `a`, push `a - b` (wrapping).
pub fn lsub(frame: &mut Frame) {
    let b = pop_long(frame);
    let a = pop_long(frame);
    frame.push(Value::Long(a.wrapping_sub(b)));
}

/// `lmul` (0x69): pop two `long`s, push `a * b` (wrapping).
pub fn lmul(frame: &mut Frame) {
    let b = pop_long(frame);
    let a = pop_long(frame);
    frame.push(Value::Long(a.wrapping_mul(b)));
}

/// `dadd` (0x63): pop two `double`s and push `a + b`. The IEEE-754 mirror of `iadd`/
/// `ladd`; `dsub`/`dmul` follow the same shape. (No wrapping — floats don't.)
pub fn dadd(frame: &mut Frame) {
    let b = pop_double(frame);
    let a = pop_double(frame);
    frame.push(Value::Double(a + b));
}

/// `dsub` (0x67): pop `b` then `a`, push `a - b`.
pub fn dsub(frame: &mut Frame) {
    let b = pop_double(frame);
    let a = pop_double(frame);
    frame.push(Value::Double(a - b));
}

/// `dmul` (0x6b): pop two `double`s, push `a * b`.
pub fn dmul(frame: &mut Frame) {
    let b = pop_double(frame);
    let a = pop_double(frame);
    frame.push(Value::Double(a * b));
}

/// `fadd` (0x62): pop two `float`s and push `a + b`. The category-1 float twin of
/// `dadd`; `fsub`/`fmul` follow the same shape.
pub fn fadd(frame: &mut Frame) {
    let b = pop_float(frame);
    let a = pop_float(frame);
    frame.push(Value::Float(a + b));
}

/// `fsub` (0x66): pop `b` then `a`, push `a - b`.
pub fn fsub(frame: &mut Frame) {
    let b = pop_float(frame);
    let a = pop_float(frame);
    frame.push(Value::Float(a - b));
}

/// `fmul` (0x6a): pop two `float`s, push `a * b`.
pub fn fmul(frame: &mut Frame) {
    let b = pop_float(frame);
    let a = pop_float(frame);
    frame.push(Value::Float(a * b));
}

/// Pops the top of the operand stack as an `f32`. Shared by the `float` ops above.
fn pop_float(frame: &mut Frame) -> f32 {
    match frame.pop() {
        Value::Float(v) => v,
        other => panic!("expected a float on the operand stack, found {other:?}"),
    }
}

// --- division & remainder ---------------------------------------------------
// Integer `/` and `%` by zero throw `ArithmeticException`, so these return a
// `Result` the dispatch loop turns into a thrown object. `wrapping_div`/`_rem`
// handle the one overflow case (`MIN / -1`) the way the JVM does (wrap, no trap).

/// The exception integer division/remainder by zero raises.
const DIV_BY_ZERO: &str = "java/lang/ArithmeticException";

/// `idiv` (0x6c): `a / b`, rounded toward zero. `b == 0` → `ArithmeticException`.
pub fn idiv(frame: &mut Frame) -> Result<(), &'static str> {
    let b = pop_int(frame);
    let a = pop_int(frame);
    if b == 0 {
        return Err(DIV_BY_ZERO);
    }
    frame.push(Value::Int(a.wrapping_div(b)));
    Ok(())
}

/// `irem` (0x70): `a % b` (sign follows the dividend). `b == 0` → `ArithmeticException`.
pub fn irem(frame: &mut Frame) -> Result<(), &'static str> {
    let b = pop_int(frame);
    let a = pop_int(frame);
    if b == 0 {
        return Err(DIV_BY_ZERO);
    }
    frame.push(Value::Int(a.wrapping_rem(b)));
    Ok(())
}

/// `ldiv` (0x6d): `long` division. `b == 0` → `ArithmeticException`.
pub fn ldiv(frame: &mut Frame) -> Result<(), &'static str> {
    let b = pop_long(frame);
    let a = pop_long(frame);
    if b == 0 {
        return Err(DIV_BY_ZERO);
    }
    frame.push(Value::Long(a.wrapping_div(b)));
    Ok(())
}

/// `lrem` (0x71): `long` remainder. `b == 0` → `ArithmeticException`.
pub fn lrem(frame: &mut Frame) -> Result<(), &'static str> {
    let b = pop_long(frame);
    let a = pop_long(frame);
    if b == 0 {
        return Err(DIV_BY_ZERO);
    }
    frame.push(Value::Long(a.wrapping_rem(b)));
    Ok(())
}

/// `fdiv` (0x6e): IEEE float division (`/0` → ±∞ or NaN, never throws).
pub fn fdiv(frame: &mut Frame) {
    let b = pop_float(frame);
    let a = pop_float(frame);
    frame.push(Value::Float(a / b));
}

/// `frem` (0x72): float remainder — `a - (a/b truncated)·b`, which is Rust's `%`
/// (truncated, sign of the dividend), *not* IEEE remainder.
pub fn frem(frame: &mut Frame) {
    let b = pop_float(frame);
    let a = pop_float(frame);
    frame.push(Value::Float(a % b));
}

/// `ddiv` (0x6f): IEEE double division.
pub fn ddiv(frame: &mut Frame) {
    let b = pop_double(frame);
    let a = pop_double(frame);
    frame.push(Value::Double(a / b));
}

/// `drem` (0x73): double remainder (truncated, like `frem`).
pub fn drem(frame: &mut Frame) {
    let b = pop_double(frame);
    let a = pop_double(frame);
    frame.push(Value::Double(a % b));
}

// --- negation ---------------------------------------------------------------

/// `ineg` (0x74): `-a` (wrapping — `-INT_MIN` is `INT_MIN`).
pub fn ineg(frame: &mut Frame) {
    let a = pop_int(frame);
    frame.push(Value::Int(a.wrapping_neg()));
}

/// `lneg` (0x75): `-a` for a `long` (wrapping).
pub fn lneg(frame: &mut Frame) {
    let a = pop_long(frame);
    frame.push(Value::Long(a.wrapping_neg()));
}

/// `fneg` (0x76): `-a` for a `float`.
pub fn fneg(frame: &mut Frame) {
    let a = pop_float(frame);
    frame.push(Value::Float(-a));
}

/// `dneg` (0x77): `-a` for a `double`.
pub fn dneg(frame: &mut Frame) {
    let a = pop_double(frame);
    frame.push(Value::Double(-a));
}

// --- shifts (the shift amount is always an `int`, masked to the type's width) ---

/// `ishl` (0x78): `a << (s & 0x1f)`.
pub fn ishl(frame: &mut Frame) {
    let s = pop_int(frame);
    let a = pop_int(frame);
    frame.push(Value::Int(a.wrapping_shl(s as u32 & 0x1f)));
}

/// `ishr` (0x7a): arithmetic right shift `a >> (s & 0x1f)` (sign-extending).
pub fn ishr(frame: &mut Frame) {
    let s = pop_int(frame);
    let a = pop_int(frame);
    frame.push(Value::Int(a.wrapping_shr(s as u32 & 0x1f)));
}

/// `iushr` (0x7c): logical (zero-fill) right shift.
pub fn iushr(frame: &mut Frame) {
    let s = pop_int(frame);
    let a = pop_int(frame);
    frame.push(Value::Int(((a as u32).wrapping_shr(s as u32 & 0x1f)) as i32));
}

/// `lshl` (0x79): `long` left shift, amount masked to 6 bits.
pub fn lshl(frame: &mut Frame) {
    let s = pop_int(frame);
    let a = pop_long(frame);
    frame.push(Value::Long(a.wrapping_shl(s as u32 & 0x3f)));
}

/// `lshr` (0x7b): arithmetic `long` right shift.
pub fn lshr(frame: &mut Frame) {
    let s = pop_int(frame);
    let a = pop_long(frame);
    frame.push(Value::Long(a.wrapping_shr(s as u32 & 0x3f)));
}

/// `lushr` (0x7d): logical `long` right shift.
pub fn lushr(frame: &mut Frame) {
    let s = pop_int(frame);
    let a = pop_long(frame);
    frame.push(Value::Long(((a as u64).wrapping_shr(s as u32 & 0x3f)) as i64));
}

// --- bitwise (int and long) -------------------------------------------------

/// `iand` (0x7e): bitwise AND of two ints.
pub fn iand(frame: &mut Frame) {
    let b = pop_int(frame);
    let a = pop_int(frame);
    frame.push(Value::Int(a & b));
}

/// `ior` (0x80): bitwise OR.
pub fn ior(frame: &mut Frame) {
    let b = pop_int(frame);
    let a = pop_int(frame);
    frame.push(Value::Int(a | b));
}

/// `ixor` (0x82): bitwise XOR.
pub fn ixor(frame: &mut Frame) {
    let b = pop_int(frame);
    let a = pop_int(frame);
    frame.push(Value::Int(a ^ b));
}

/// `land` (0x7f): bitwise AND of two longs.
pub fn land(frame: &mut Frame) {
    let b = pop_long(frame);
    let a = pop_long(frame);
    frame.push(Value::Long(a & b));
}

/// `lor` (0x81): bitwise OR of two longs.
pub fn lor(frame: &mut Frame) {
    let b = pop_long(frame);
    let a = pop_long(frame);
    frame.push(Value::Long(a | b));
}

/// `lxor` (0x83): bitwise XOR of two longs.
pub fn lxor(frame: &mut Frame) {
    let b = pop_long(frame);
    let a = pop_long(frame);
    frame.push(Value::Long(a ^ b));
}

/// Pops the top of the operand stack as an `i32`. Shared by the int ops above.
/// A reference here would be a type error the verifier rules out, so it's a bug.
fn pop_int(frame: &mut Frame) -> i32 {
    match frame.pop() {
        Value::Int(v) => v,
        other => panic!("expected an int on the operand stack, found {other:?}"),
    }
}

/// Pops the top of the operand stack as an `i64`. Shared by the `long` ops above.
fn pop_long(frame: &mut Frame) -> i64 {
    match frame.pop() {
        Value::Long(v) => v,
        other => panic!("expected a long on the operand stack, found {other:?}"),
    }
}

/// Pops the top of the operand stack as an `f64`. Shared by the `double` ops above.
fn pop_double(frame: &mut Frame) -> f64 {
    match frame.pop() {
        Value::Double(v) => v,
        other => panic!("expected a double on the operand stack, found {other:?}"),
    }
}
