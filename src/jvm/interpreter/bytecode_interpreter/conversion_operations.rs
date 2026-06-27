//! The numeric **conversion** opcodes — `i2l`/`l2i`/`f2d`/… (JVMS §6.5). Each one
//! pops a value of one numeric type, converts it, and pushes the other type. They
//! split into three groups: *widening* (no loss, e.g. `i2l`), *narrowing* (may lose
//! magnitude/precision, e.g. `l2i`, `d2i`), and the `int`→sub-`int` truncations
//! (`i2b`/`i2c`/`i2s`, which still produce an `int`).
//!
//! Rust's `as` cast matches the JVM exactly here, including the tricky float→integer
//! rule: it **saturates** (NaN → 0, out-of-range → the type's MIN/MAX), which is
//! precisely what `f2i`/`d2i`/`f2l`/`d2l` require — so the narrowing casts are direct.

use crate::jvm::interpreter::frame::{Frame, Value};

// --- widening (int → long/float/double, long → float/double, float → double) -----

/// `i2l` (0x85): `int` → `long` (sign-extended).
pub fn i2l(frame: &mut Frame) {
    let v = pop_int(frame);
    frame.push(Value::Long(v as i64));
}

/// `i2f` (0x86): `int` → `float`.
pub fn i2f(frame: &mut Frame) {
    let v = pop_int(frame);
    frame.push(Value::Float(v as f32));
}

/// `i2d` (0x87): `int` → `double`.
pub fn i2d(frame: &mut Frame) {
    let v = pop_int(frame);
    frame.push(Value::Double(v as f64));
}

/// `l2f` (0x89): `long` → `float` (may lose precision).
pub fn l2f(frame: &mut Frame) {
    let v = pop_long(frame);
    frame.push(Value::Float(v as f32));
}

/// `l2d` (0x8a): `long` → `double` (may lose precision).
pub fn l2d(frame: &mut Frame) {
    let v = pop_long(frame);
    frame.push(Value::Double(v as f64));
}

/// `f2d` (0x8d): `float` → `double` (exact).
pub fn f2d(frame: &mut Frame) {
    let v = pop_float(frame);
    frame.push(Value::Double(v as f64));
}

// --- narrowing (long → int, float → int/long, double → int/long/float) -----------

/// `l2i` (0x88): `long` → `int` (keeps the low 32 bits).
pub fn l2i(frame: &mut Frame) {
    let v = pop_long(frame);
    frame.push(Value::Int(v as i32));
}

/// `f2i` (0x8b): `float` → `int` (toward zero; NaN → 0, overflow → MIN/MAX).
pub fn f2i(frame: &mut Frame) {
    let v = pop_float(frame);
    frame.push(Value::Int(v as i32));
}

/// `f2l` (0x8c): `float` → `long` (toward zero; saturating like `f2i`).
pub fn f2l(frame: &mut Frame) {
    let v = pop_float(frame);
    frame.push(Value::Long(v as i64));
}

/// `d2i` (0x8e): `double` → `int` (toward zero; saturating).
pub fn d2i(frame: &mut Frame) {
    let v = pop_double(frame);
    frame.push(Value::Int(v as i32));
}

/// `d2l` (0x8f): `double` → `long` (toward zero; saturating).
pub fn d2l(frame: &mut Frame) {
    let v = pop_double(frame);
    frame.push(Value::Long(v as i64));
}

/// `d2f` (0x90): `double` → `float` (may lose precision/magnitude).
pub fn d2f(frame: &mut Frame) {
    let v = pop_double(frame);
    frame.push(Value::Float(v as f32));
}

// --- int → sub-int (truncate to byte/char/short, result is still an `int`) -------

/// `i2b` (0x91): truncate to a signed `byte`, sign-extend back to `int`.
pub fn i2b(frame: &mut Frame) {
    let v = pop_int(frame);
    frame.push(Value::Int(v as i8 as i32));
}

/// `i2c` (0x92): truncate to a `char` (unsigned 16-bit), zero-extend back to `int`.
pub fn i2c(frame: &mut Frame) {
    let v = pop_int(frame);
    frame.push(Value::Int(v as u16 as i32));
}

/// `i2s` (0x93): truncate to a signed `short`, sign-extend back to `int`.
pub fn i2s(frame: &mut Frame) {
    let v = pop_int(frame);
    frame.push(Value::Int(v as i16 as i32));
}

// --- typed pops (the verifier guarantees the operand's type, so a mismatch is a bug)

fn pop_int(frame: &mut Frame) -> i32 {
    match frame.pop() {
        Value::Int(v) => v,
        other => panic!("conversion: expected an int, found {other:?}"),
    }
}

fn pop_long(frame: &mut Frame) -> i64 {
    match frame.pop() {
        Value::Long(v) => v,
        other => panic!("conversion: expected a long, found {other:?}"),
    }
}

fn pop_float(frame: &mut Frame) -> f32 {
    match frame.pop() {
        Value::Float(v) => v,
        other => panic!("conversion: expected a float, found {other:?}"),
    }
}

fn pop_double(frame: &mut Frame) -> f64 {
    match frame.pop() {
        Value::Double(v) => v,
        other => panic!("conversion: expected a double, found {other:?}"),
    }
}
