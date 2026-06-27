//! `ConstantValue` (JVMS §4.7.2): a static field's compile-time constant. The
//! body is a single `u2` index into the constant pool, pointing at an
//! `Integer`/`Long`/`Float`/`Double`/`String` entry that holds the value.

use super::super::reader::ClassReader;

/// The constant-pool index the `ConstantValue` body points to.
pub fn index(bytes: &[u8]) -> Option<u16> {
    let mut r = ClassReader::new(bytes);
    r.read_u16().ok()
}
