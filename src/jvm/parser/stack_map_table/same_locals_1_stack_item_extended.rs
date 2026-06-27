//! `same_locals_1_stack_item_frame_extended` (frame_type 247): same meaning as
//! `same_locals_1_stack_item` (locals unchanged, exactly one stack item), but
//! the jump is too far for the compact 64–127 range, so the `offset_delta`
//! comes as an explicit `u2` before the single `verification_type_info`.

use super::super::reader::{ClassReader, ParseError};
use super::VerificationTypeInfo;
use crate::jvm::class_file::ClassFile;

pub struct SameLocals1StackItemExtended {
    pub offset_delta: u16,
    pub stack: VerificationTypeInfo,
}

impl SameLocals1StackItemExtended {
    /// Reads the explicit `u2` offset_delta, then one `verification_type_info`.
    pub fn parse(reader: &mut ClassReader) -> Result<Self, ParseError> {
        Ok(SameLocals1StackItemExtended {
            offset_delta: reader.read_u16()?,
            stack: VerificationTypeInfo::parse(reader)?,
        })
    }

    /// Rendered exactly as javap does, e.g.:
    /// ```text
    ///         frame_type = 247 /* same_locals_1_stack_item_frame_extended */
    ///           offset_delta = 69
    ///           stack = [ class java/lang/String ]
    /// ```
    pub fn print(&self, cf: &ClassFile) {
        crate::pln!("        frame_type = 247 /* same_locals_1_stack_item_frame_extended */");
        crate::pln!("          offset_delta = {}", self.offset_delta);
        crate::pln!("          stack = [ {} ]", self.stack.display(cf));
    }
}
