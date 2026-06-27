//! `MethodParameters` (JVMS §4.7.24): the formal parameter names and flags of a
//! method. The body is a `u1` count followed by, per parameter, a `u2` name
//! index (0 = no name) and `u2` access flags (`final`/`synthetic`/`mandated`).

use crate::jvm::parser::ClassReader;

pub struct MethodParameter {
    pub name_index: u16,
    pub access_flags: u16,
}

pub fn parse(bytes: &[u8]) -> Vec<MethodParameter> {
    let mut r = ClassReader::new(bytes);
    let mut out = Vec::new();
    let Ok(count) = r.read_u8() else { return out }; // note: a single byte
    for _ in 0..count {
        let (Ok(name_index), Ok(access_flags)) = (r.read_u16(), r.read_u16()) else {
            break;
        };
        out.push(MethodParameter { name_index, access_flags });
    }
    out
}

/// The flag keywords javap shows in the `Flags` column.
pub fn flag_names(flags: u16) -> String {
    let mut v = Vec::new();
    if flags & 0x0010 != 0 {
        v.push("final");
    }
    if flags & 0x1000 != 0 {
        v.push("synthetic");
    }
    if flags & 0x8000 != 0 {
        v.push("mandated");
    }
    v.join(" ")
}
