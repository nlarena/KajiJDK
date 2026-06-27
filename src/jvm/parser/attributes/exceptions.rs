//! `Exceptions` (JVMS §4.7.5): the checked exceptions a method may throw — its
//! `throws` clause. The body is a `u2` count followed by that many `u2`
//! constant-pool indices to `Class` entries.

use super::super::reader::ClassReader;

/// The `Class` constant-pool indices of the declared checked exceptions.
pub fn parse(bytes: &[u8]) -> Vec<u16> {
    let mut r = ClassReader::new(bytes);
    let mut out = Vec::new();
    let Ok(count) = r.read_u16() else { return out };
    for _ in 0..count {
        match r.read_u16() {
            Ok(i) => out.push(i),
            Err(_) => break,
        }
    }
    out
}
