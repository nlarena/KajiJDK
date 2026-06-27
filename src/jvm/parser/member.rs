//! `field_info` (§4.5) and `method_info` (§4.6) have the *identical* layout, so
//! one `MemberInfo` type serves both: access flags + name + descriptor + the
//! member's own attributes.

use super::attribute::{self, AttributeInfo};
use super::reader::{ClassReader, ParseError};

/// A field or a method (same structure for both in the class file format).
#[derive(Debug, Clone)]
pub struct MemberInfo {
    /// Member access flags (different bit meanings than the class ones:
    /// `ACC_STATIC`, `ACC_PRIVATE`, …).
    pub access_flags: u16,
    /// Constant-pool index of the name (`Utf8`, e.g. "value", "main", "<init>").
    pub name_index: u16,
    /// Constant-pool index of the descriptor (`Utf8`, e.g. "I", "(II)I").
    pub descriptor_index: u16,
    /// Attributes attached to this member (a field's `ConstantValue`, a method's
    /// `Code`, etc.).
    pub attributes: Vec<AttributeInfo>,
}

impl MemberInfo {
    /// Whether `ACC_STATIC` is set — the member belongs to the *class* (one shared
    /// copy) rather than to each *instance*. Static fields are laid out into the
    /// class's `Class<…>` object; static methods are invoked without a receiver.
    pub fn is_static(&self) -> bool {
        self.access_flags & 0x0008 != 0
    }

    /// Whether `ACC_PRIVATE` is set. Private methods aren't dynamically dispatched
    /// (they're called with `invokespecial`, not `invokevirtual`), so they don't
    /// get a virtual-table slot.
    pub fn is_private(&self) -> bool {
        self.access_flags & 0x0002 != 0
    }

    /// Whether `ACC_NATIVE` is set — the method is implemented in native code, so it
    /// has no `Code` attribute. The interpreter dispatches it to a native bridge
    /// instead of running bytecode.
    pub fn is_native(&self) -> bool {
        self.access_flags & 0x0100 != 0
    }

    /// Whether `ACC_SYNCHRONIZED` (0x0020) is set. For a *method*, this is the whole
    /// signal that it is `synchronized`: there are **no** `monitorenter`/`monitorexit`
    /// opcodes in the body (unlike a `synchronized` *block*), so the VM must take the
    /// object's monitor on entry — `this`, or the `Class` mirror for a `static` method —
    /// and release it on every exit path. (The same bit means `ACC_SUPER` on a *class*.)
    pub fn is_synchronized(&self) -> bool {
        self.access_flags & 0x0020 != 0
    }
}

/// Reads a `u2` count followed by that many `field_info`/`method_info`.
pub fn parse_members(reader: &mut ClassReader) -> Result<Vec<MemberInfo>, ParseError> {
    let count = reader.read_u16()?;
    let mut members = Vec::with_capacity(count as usize);
    for _ in 0..count {
        members.push(parse_member(reader)?);
    }
    Ok(members)
}

fn parse_member(reader: &mut ClassReader) -> Result<MemberInfo, ParseError> {
    let access_flags = reader.read_u16()?;
    let name_index = reader.read_u16()?;
    let descriptor_index = reader.read_u16()?;
    // A member's attributes use the very same "count + N" reader as the class's.
    let attributes = attribute::parse_attributes(reader)?;
    Ok(MemberInfo {
        access_flags,
        name_index,
        descriptor_index,
        attributes,
    })
}
