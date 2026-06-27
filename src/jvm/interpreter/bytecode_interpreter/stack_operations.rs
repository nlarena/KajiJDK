//! The operand-stack manipulation opcodes — `pop`/`dup`/`swap` and friends (JVMS
//! §6.5). They reshuffle the top of the stack regardless of *type*, but they are
//! **category-aware**: a `long`/`double` occupies two slots, so `dup2` duplicates
//! either two category-1 values or one category-2 value, etc.
//!
//! We hold a category-2 value as a *single* `Value`, so the JVM's "slots" and our
//! Vec "entries" differ — [`entries_for`] bridges the two. The whole `dup*` family
//! then collapses to one operation, [`dup_insert`], parameterised by how many slots
//! to duplicate and how many to skip past.

use crate::jvm::interpreter::frame::{Frame, Value};

/// `pop` (0x57): discard the top one-slot value.
pub fn pop(frame: &mut Frame) {
    pop_slots(frame.operands_mut(), 1);
}

/// `pop2` (0x58): discard the top two slots (two category-1 values, or one cat-2).
pub fn pop2(frame: &mut Frame) {
    pop_slots(frame.operands_mut(), 2);
}

/// `dup` (0x59): duplicate the top one-slot value.
pub fn dup(frame: &mut Frame) {
    dup_insert(frame.operands_mut(), 1, 0);
}

/// `dup_x1` (0x5a): duplicate the top value and insert the copy one slot down.
pub fn dup_x1(frame: &mut Frame) {
    dup_insert(frame.operands_mut(), 1, 1);
}

/// `dup_x2` (0x5b): duplicate the top value and insert the copy two slots down.
pub fn dup_x2(frame: &mut Frame) {
    dup_insert(frame.operands_mut(), 1, 2);
}

/// `dup2` (0x5c): duplicate the top two slots.
pub fn dup2(frame: &mut Frame) {
    dup_insert(frame.operands_mut(), 2, 0);
}

/// `dup2_x1` (0x5d): duplicate the top two slots, insert the copy one slot down.
pub fn dup2_x1(frame: &mut Frame) {
    dup_insert(frame.operands_mut(), 2, 1);
}

/// `dup2_x2` (0x5e): duplicate the top two slots, insert the copy two slots down.
pub fn dup2_x2(frame: &mut Frame) {
    dup_insert(frame.operands_mut(), 2, 2);
}

/// `swap` (0x5f): swap the top two category-1 values.
pub fn swap(frame: &mut Frame) {
    let stack = frame.operands_mut();
    let len = stack.len();
    stack.swap(len - 1, len - 2);
}

/// A value's slot width: category-2 (`long`/`double`) takes two, everything else one.
fn width(value: &Value) -> usize {
    match value {
        Value::Long(_) | Value::Double(_) => 2,
        _ => 1,
    }
}

/// How many entries from the top of `stack` add up to `slots` slots (0 → none).
fn entries_for(stack: &[Value], slots: usize) -> usize {
    if slots == 0 {
        return 0;
    }
    let mut acc = 0;
    let mut count = 0;
    for value in stack.iter().rev() {
        acc += width(value);
        count += 1;
        if acc >= slots {
            break;
        }
    }
    count
}

/// Removes the top `slots` slots' worth of entries.
fn pop_slots(stack: &mut Vec<Value>, slots: usize) {
    let n = entries_for(stack, slots);
    stack.truncate(stack.len() - n);
}

/// The shared engine of the `dup*` family: duplicate the top `dup_slots` slots and
/// reinsert the copy `skip_slots` slots further down. With `skip_slots = 0` the copy
/// lands right below the original (`dup`/`dup2`); otherwise it slides past that many
/// slots (`dup_x1`/`dup2_x2`/…). Category-2 values are handled for free, since
/// [`entries_for`] counts them as the two slots they are.
fn dup_insert(stack: &mut Vec<Value>, dup_slots: usize, skip_slots: usize) {
    let len = stack.len();
    let dup_n = entries_for(stack, dup_slots);
    let skip_n = entries_for(&stack[..len - dup_n], skip_slots);
    let insert_at = len - dup_n - skip_n;
    let group: Vec<Value> = stack[len - dup_n..].to_vec();
    for (offset, value) in group.into_iter().enumerate() {
        stack.insert(insert_at + offset, value);
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::jvm::interpreter::frame::Value::{Int, Long};

    #[test]
    fn dup2_duplicates_one_category2_or_two_category1() {
        // Form 2: a single long (category-2) on top is duplicated as one entry.
        let mut s = vec![Int(1), Long(99)];
        dup_insert(&mut s, 2, 0);
        assert_eq!(s, vec![Int(1), Long(99), Long(99)]);
        // Form 1: two category-1 values are duplicated as a pair.
        let mut s = vec![Int(1), Int(2)];
        dup_insert(&mut s, 2, 0);
        assert_eq!(s, vec![Int(1), Int(2), Int(1), Int(2)]);
    }

    #[test]
    fn dup_x1_inserts_the_copy_one_slot_down() {
        let mut s = vec![Int(2), Int(1)]; // …, v2, v1
        dup_insert(&mut s, 1, 1);
        assert_eq!(s, vec![Int(1), Int(2), Int(1)]); // …, v1, v2, v1
    }

    #[test]
    fn dup_x2_skips_a_category2_value() {
        // Form 2: …, long(cat-2), v1 → …, v1, long, v1.
        let mut s = vec![Long(7), Int(1)];
        dup_insert(&mut s, 1, 2);
        assert_eq!(s, vec![Int(1), Long(7), Int(1)]);
    }

    #[test]
    fn pop2_drops_one_category2_or_two_category1() {
        let mut s = vec![Int(1), Long(9)];
        pop_slots(&mut s, 2);
        assert_eq!(s, vec![Int(1)]); // the long was a single 2-slot entry
        let mut s = vec![Int(1), Int(2), Int(3)];
        pop_slots(&mut s, 2);
        assert_eq!(s, vec![Int(1)]); // two category-1 values dropped
    }
}
