//! `InnerClasses` (JVMS §4.7.6): a class-level attribute listing every class
//! referenced by this class that is a member of (or otherwise nested in)
//! another class — named nested classes, anonymous and local classes. Each
//! entry pairs the inner class with its outer class and simple name (either of
//! which may be absent), plus the inner class's access flags.

use super::super::reader::ClassReader;

pub struct InnerClass {
    /// Constant-pool `Class` index of the inner class.
    pub inner_class_info: u16,
    /// Constant-pool `Class` index of the outer class, or 0 (local/anonymous).
    pub outer_class_info: u16,
    /// Constant-pool `Utf8` index of the simple name, or 0 (anonymous).
    pub inner_name: u16,
    /// The inner class's declared access flags.
    pub access_flags: u16,
}

/// Parses the attribute body: `u2 count`, then for each class four `u2` values
/// (inner class, outer class, inner name, access flags).
pub fn parse(bytes: &[u8]) -> Vec<InnerClass> {
    let mut r = ClassReader::new(bytes);
    let mut classes = Vec::new();
    let Ok(count) = r.read_u16() else { return classes };
    for _ in 0..count {
        let (Ok(inner_class_info), Ok(outer_class_info), Ok(inner_name), Ok(access_flags)) =
            (r.read_u16(), r.read_u16(), r.read_u16(), r.read_u16())
        else {
            break;
        };
        classes.push(InnerClass { inner_class_info, outer_class_info, inner_name, access_flags });
    }
    classes
}
