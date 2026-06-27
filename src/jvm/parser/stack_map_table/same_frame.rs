//! `same_frame` (frame_type 0–63): at this branch target the operand stack is
//! empty and the locals are unchanged from the previous frame. The frame-type
//! byte *is* the `offset_delta`, so there are no extra bytes to read.

pub struct SameFrame {
    pub offset_delta: u16,
}

impl SameFrame {
    /// For a `same_frame`, the frame-type byte (0–63) equals the offset delta.
    pub fn parse(frame_type: u8) -> Self {
        SameFrame { offset_delta: frame_type as u16 }
    }

    /// Rendered exactly as javap does: `frame_type = N /* same */`.
    pub fn print(&self) {
        crate::pln!("        frame_type = {} /* same */", self.offset_delta);
    }
}
