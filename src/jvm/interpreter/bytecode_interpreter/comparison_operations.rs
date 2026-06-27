//! The numeric **comparison** opcodes — `lcmp`/`fcmpl`/`fcmpg`/`dcmpl`/`dcmpg`
//! (JVMS §6.5). Unlike `if_icmp*` (which compare two `int`s and branch directly),
//! these compare two `long`/`float`/`double`s and push an **`int` verdict**:
//! `1` if `a > b`, `0` if `a == b`, `-1` if `a < b`. A following `if<cond>` then
//! branches on that verdict — so `if (longA < longB)` is `lcmp; iflt`.
//!
//! The `l` vs `g` split (`fcmpl`/`fcmpg`) is only about **NaN**: floats/doubles are
//! unordered with NaN, so `fcmpl` yields `-1` when either operand is NaN and `fcmpg`
//! yields `1`. The compiler picks whichever makes the NaN case fail the source
//! comparison (e.g. `a < b` with a NaN must be false).

use std::cmp::Ordering;

use crate::jvm::interpreter::frame::{Frame, Value};

/// `lcmp` (0x94): pop two `long`s, push `1`/`0`/`-1` for `a > / == / < b`. No NaN —
/// `long` is totally ordered.
pub fn lcmp(frame: &mut Frame) {
    let b = pop_long(frame);
    let a = pop_long(frame);
    frame.push(Value::Int(verdict(a.cmp(&b))));
}

/// `fcmpl` (0x95): pop two `float`s, push the verdict; **NaN → `-1`**.
pub fn fcmpl(frame: &mut Frame) {
    let b = pop_float(frame);
    let a = pop_float(frame);
    frame.push(Value::Int(a.partial_cmp(&b).map_or(-1, verdict)));
}

/// `fcmpg` (0x96): like `fcmpl`, but **NaN → `1`**.
pub fn fcmpg(frame: &mut Frame) {
    let b = pop_float(frame);
    let a = pop_float(frame);
    frame.push(Value::Int(a.partial_cmp(&b).map_or(1, verdict)));
}

/// `dcmpl` (0x97): pop two `double`s, push the verdict; **NaN → `-1`**.
pub fn dcmpl(frame: &mut Frame) {
    let b = pop_double(frame);
    let a = pop_double(frame);
    frame.push(Value::Int(a.partial_cmp(&b).map_or(-1, verdict)));
}

/// `dcmpg` (0x98): like `dcmpl`, but **NaN → `1`**.
pub fn dcmpg(frame: &mut Frame) {
    let b = pop_double(frame);
    let a = pop_double(frame);
    frame.push(Value::Int(a.partial_cmp(&b).map_or(1, verdict)));
}

/// Maps an `Ordering` to the JVM's `1`/`0`/`-1` comparison verdict.
fn verdict(ordering: Ordering) -> i32 {
    match ordering {
        Ordering::Greater => 1,
        Ordering::Equal => 0,
        Ordering::Less => -1,
    }
}

fn pop_long(frame: &mut Frame) -> i64 {
    match frame.pop() {
        Value::Long(v) => v,
        other => panic!("compare: expected a long, found {other:?}"),
    }
}

fn pop_float(frame: &mut Frame) -> f32 {
    match frame.pop() {
        Value::Float(v) => v,
        other => panic!("compare: expected a float, found {other:?}"),
    }
}

fn pop_double(frame: &mut Frame) -> f64 {
    match frame.pop() {
        Value::Double(v) => v,
        other => panic!("compare: expected a double, found {other:?}"),
    }
}
