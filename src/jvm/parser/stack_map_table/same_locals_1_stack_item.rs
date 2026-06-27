//! `same_locals_1_stack_item` (frame_type 64–127): the locals are unchanged
//! from the previous frame and the operand stack holds **exactly one** item.
//! The `offset_delta` is `frame_type - 64`, and the frame is followed by a
//! single `verification_type_info` describing that one stack item.

use super::super::reader::{ClassReader, ParseError};
use super::VerificationTypeInfo;
use crate::jvm::class_file::ClassFile;

pub struct SameLocals1StackItem {
    /// The raw frame-type byte (64–127). javap prints this verbatim; the actual
    /// `offset_delta` is `frame_type - 64`.
    pub frame_type: u8,
    pub stack: VerificationTypeInfo,
}

impl SameLocals1StackItem {
    /// Stores the raw `frame_type`, then reads one `verification_type_info`.
    pub fn parse(frame_type: u8, reader: &mut ClassReader) -> Result<Self, ParseError> {
        Ok(SameLocals1StackItem {
            frame_type,
            stack: VerificationTypeInfo::parse(reader)?,
        })
    }

    /// Rendered exactly as javap does, e.g.:
    /// ```text
    ///         frame_type = 65 /* same_locals_1_stack_item */
    ///           stack = [ class java/lang/String ]
    /// ```
    pub fn print(&self, cf: &ClassFile) {
        crate::pln!(
            "        frame_type = {} /* same_locals_1_stack_item */",
            self.frame_type
        );
        crate::pln!("          stack = [ {} ]", self.stack.display(cf));
    }
}
