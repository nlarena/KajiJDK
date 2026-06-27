//! `LocalVariableTable` (JVMS §4.7.13) and `LocalVariableTypeTable` (§4.7.14):
//! debug info emitted by `javac -g`, mapping local-variable slots to names and
//! a descriptor (Table) or generic signature (TypeTable) over bytecode ranges.
//! Both share the same five-`u2` row layout.

use crate::jvm::parser::ClassReader;

pub struct LocalVar {
    pub start_pc: u16,
    pub length: u16,
    pub name_index: u16,
    /// Descriptor index (`LocalVariableTable`) or signature index (TypeTable).
    pub type_index: u16,
    pub slot: u16,
}

pub fn parse(bytes: &[u8]) -> Vec<LocalVar> {
    let mut r = ClassReader::new(bytes);
    let mut out = Vec::new();
    let Ok(count) = r.read_u16() else { return out };
    for _ in 0..count {
        let (Ok(start_pc), Ok(length), Ok(name_index), Ok(type_index), Ok(slot)) =
            (r.read_u16(), r.read_u16(), r.read_u16(), r.read_u16(), r.read_u16())
        else {
            break;
        };
        out.push(LocalVar { start_pc, length, name_index, type_index, slot });
    }
    out
}
