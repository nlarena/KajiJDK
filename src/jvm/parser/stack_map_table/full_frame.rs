//! `full_frame` (frame_type 255): the explicit catch-all, used when no compact
//! form fits. It restates everything: the `u2` offset_delta, the entire locals
//! array, and the entire operand stack — each as a `u2` count followed by that
//! many `verification_type_info` entries.

use super::super::reader::{ClassReader, ParseError};
use super::VerificationTypeInfo;
use crate::jvm::class_file::ClassFile;

pub struct FullFrame {
    pub offset_delta: u16,
    pub locals: Vec<VerificationTypeInfo>,
    pub stack: Vec<VerificationTypeInfo>,
}

impl FullFrame {
    pub fn parse(reader: &mut ClassReader) -> Result<Self, ParseError> {
        let offset_delta = reader.read_u16()?;
        let locals = parse_type_list(reader)?;
        let stack = parse_type_list(reader)?;
        Ok(FullFrame { offset_delta, locals, stack })
    }

    /// Rendered exactly as javap does, e.g.:
    /// ```text
    ///         frame_type = 255 /* full_frame */
    ///           offset_delta = 0
    ///           locals = [ class Full, int, int, int ]
    ///           stack = [ int, int ]
    /// ```
    pub fn print(&self, cf: &ClassFile) {
        crate::pln!("        frame_type = 255 /* full_frame */");
        crate::pln!("          offset_delta = {}", self.offset_delta);
        crate::pln!("          locals = {}", format_type_list(&self.locals, cf));
        crate::pln!("          stack = {}", format_type_list(&self.stack, cf));
    }
}

/// Reads a `u2` count followed by that many `verification_type_info` entries.
fn parse_type_list(reader: &mut ClassReader) -> Result<Vec<VerificationTypeInfo>, ParseError> {
    let count = reader.read_u16()?;
    let mut list = Vec::with_capacity(count as usize);
    for _ in 0..count {
        list.push(VerificationTypeInfo::parse(reader)?);
    }
    Ok(list)
}

/// Formats a type list the way javap does: `[ a, b, c ]`, and `[ ]` when empty
/// (a space after `[`, `, ` between items, a space before `]`).
fn format_type_list(items: &[VerificationTypeInfo], cf: &ClassFile) -> String {
    if items.is_empty() {
        return "[]".to_string(); // javap renders an empty list without inner spaces
    }
    let mut out = String::from("[");
    for (i, item) in items.iter().enumerate() {
        out.push_str(if i == 0 { " " } else { ", " });
        out.push_str(&item.display(cf));
    }
    out.push_str(" ]");
    out
}
