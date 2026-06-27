//! `same_frame_extended` (frame_type 251): same meaning as `same_frame` (locals
//! unchanged, empty operand stack), but the jump is too far for the compact
//! 0–63 range, so the `offset_delta` comes as an explicit `u2`.

use super::super::reader::{ClassReader, ParseError};

pub struct SameFrameExtended {
    pub offset_delta: u16,
}

impl SameFrameExtended {
    pub fn parse(reader: &mut ClassReader) -> Result<Self, ParseError> {
        Ok(SameFrameExtended { offset_delta: reader.read_u16()? })
    }

    /// Rendered exactly as javap does, e.g.:
    /// ```text
    ///         frame_type = 251 /* same_frame_extended */
    ///           offset_delta = 164
    /// ```
    pub fn print(&self) {
        crate::pln!("        frame_type = 251 /* same_frame_extended */");
        crate::pln!("          offset_delta = {}", self.offset_delta);
    }
}
