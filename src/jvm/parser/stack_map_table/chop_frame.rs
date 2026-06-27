//! `chop_frame` (frame_type 248–250): the operand stack is empty and the last
//! `k = 251 - frame_type` locals of the previous frame are removed ("chopped").
//! Carries an explicit `u2` offset_delta and no `verification_type_info` — it
//! only says *how many* locals to drop, not their types.

use super::super::reader::{ClassReader, ParseError};

pub struct ChopFrame {
    /// Raw frame-type byte (248–250); javap prints it verbatim. The number of
    /// chopped locals is `251 - frame_type`.
    pub frame_type: u8,
    pub offset_delta: u16,
}

impl ChopFrame {
    pub fn parse(frame_type: u8, reader: &mut ClassReader) -> Result<Self, ParseError> {
        Ok(ChopFrame {
            frame_type,
            offset_delta: reader.read_u16()?,
        })
    }

    /// Rendered exactly as javap does, e.g.:
    /// ```text
    ///         frame_type = 250 /* chop */
    ///           offset_delta = 14
    /// ```
    pub fn print(&self) {
        crate::pln!("        frame_type = {} /* chop */", self.frame_type);
        crate::pln!("          offset_delta = {}", self.offset_delta);
    }
}
