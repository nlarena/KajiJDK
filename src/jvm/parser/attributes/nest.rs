//! `NestHost` (JVMS §4.7.28) and `NestMembers` (§4.7.29): the Java 11 nest-based
//! access-control attributes. The nest host names the class that hosts the nest;
//! the host class lists its nest members. Both reference `Class` constants.

use crate::jvm::parser::ClassReader;

/// The host class's constant-pool `Class` index from a `NestHost` body.
pub fn host(bytes: &[u8]) -> Option<u16> {
    let mut r = ClassReader::new(bytes);
    r.read_u16().ok()
}

/// The member classes' constant-pool `Class` indices from a `NestMembers` body
/// (`u2 count` then that many `u2` indices).
pub fn members(bytes: &[u8]) -> Vec<u16> {
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
