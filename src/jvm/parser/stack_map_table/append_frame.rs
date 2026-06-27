//! `append_frame` (frame_type 252–254): the operand stack is empty and
//! `k = frame_type - 251` new locals are appended to the previous frame's.
//! Carries an explicit `u2` offset_delta followed by `k` `verification_type_info`
//! entries describing each appended local.

use super::super::reader::{ClassReader, ParseError};
use super::VerificationTypeInfo;
use crate::jvm::class_file::ClassFile;

pub struct AppendFrame {
    /// Raw frame-type byte (252–254); javap prints it verbatim. The number of
    /// appended locals is `frame_type - 251`.
    frame_type: u8,
    pub offset_delta: u16,
    pub locals: Vec<VerificationTypeInfo>,
}

impl AppendFrame {
    /// Reads the `u2` offset_delta, then `k = frame_type - 251` type entries.
    pub fn parse(frame_type: u8, reader: &mut ClassReader) -> Result<Self, ParseError> {
        let offset_delta = reader.read_u16()?;
        let k = (frame_type - 251) as usize;
        let mut locals = Vec::with_capacity(k);
        for _ in 0..k {
            locals.push(VerificationTypeInfo::parse(reader)?);
        }
        Ok(AppendFrame { frame_type, offset_delta, locals })
    }

    /// Rendered exactly as javap does, e.g.:
    /// ```text
    ///         frame_type = 253 /* append */
    ///           offset_delta = 4
    ///           locals = [ int, int ]
    /// ```
    pub fn print(&self, cf: &ClassFile) {
        crate::pln!("        frame_type = {} /* append */", self.frame_type);
        crate::pln!("          offset_delta = {}", self.offset_delta);
        let items: Vec<String> = self.locals.iter().map(|l| l.display(cf)).collect();
        crate::pln!("          locals = [ {} ]", items.join(", "));
    }
}
