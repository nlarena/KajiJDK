//! `Record` (JVMS §4.7.30): for a record class, the list of its components.
//! A `record_component_info` is a name + descriptor + its own attributes — the
//! same shape as a field, so each component is decoded into a [`MemberInfo`]
//! (with no access flags) and rendered like a field.

use crate::jvm::parser::attribute;
use crate::jvm::parser::{ClassReader, MemberInfo};

/// Parses the attribute body: `u2 count`, then for each component a name and
/// descriptor index followed by its own attribute list.
pub fn parse(bytes: &[u8]) -> Vec<MemberInfo> {
    let mut r = ClassReader::new(bytes);
    let mut out = Vec::new();
    let Ok(count) = r.read_u16() else { return out };
    for _ in 0..count {
        let (Ok(name_index), Ok(descriptor_index)) = (r.read_u16(), r.read_u16()) else {
            break;
        };
        let Ok(attributes) = attribute::parse_attributes(&mut r) else {
            break;
        };
        out.push(MemberInfo { access_flags: 0, name_index, descriptor_index, attributes });
    }
    out
}
