//! Decodes the `StackMapTable` attribute (JVM spec §4.7.4): a list of stack map
//! frames, one per branch target, used by the bytecode verifier. This file
//! parses the attribute and maps each entry to its dedicated frame class.

pub mod append_frame;
pub mod chop_frame;
pub mod full_frame;
pub mod same_frame;
pub mod same_frame_extended;
pub mod same_locals_1_stack_item;
pub mod same_locals_1_stack_item_extended;

use super::reader::{ClassReader, ParseError};
use crate::jvm::class_file::ClassFile;
use append_frame::AppendFrame;
use chop_frame::ChopFrame;
use full_frame::FullFrame;
use same_frame::SameFrame;
use same_frame_extended::SameFrameExtended;
use same_locals_1_stack_item::SameLocals1StackItem;
use same_locals_1_stack_item_extended::SameLocals1StackItemExtended;

/// One stack map frame. Each variant is decoded by its own dedicated class.
pub enum StackMapFrame {
    Same(SameFrame),
    SameLocals1StackItem(SameLocals1StackItem),
    SameLocals1StackItemExtended(SameLocals1StackItemExtended),
    Chop(ChopFrame),
    SameFrameExtended(SameFrameExtended),
    Append(AppendFrame),
    Full(FullFrame),
}

impl StackMapFrame {
    /// Prints the frame as javap does. Takes `cf` because some frames carry a
    /// `verification_type_info` whose `Object` type resolves through the pool.
    pub fn print(&self, cf: &ClassFile) {
        match self {
            StackMapFrame::Same(frame) => frame.print(),
            StackMapFrame::SameLocals1StackItem(frame) => frame.print(cf),
            StackMapFrame::SameLocals1StackItemExtended(frame) => frame.print(cf),
            StackMapFrame::Chop(frame) => frame.print(),
            StackMapFrame::SameFrameExtended(frame) => frame.print(),
            StackMapFrame::Append(frame) => frame.print(cf),
            StackMapFrame::Full(frame) => frame.print(cf),
        }
    }

    /// A normalized view of this frame for the verifier: the `offset_delta` plus the
    /// delta operation on the previous frame's type state. Hides the compact/extended
    /// encodings (`same` vs `same_extended`, etc.) behind one shape, and computes the
    /// chop count, so the verifier doesn't need to know the byte-level frame types.
    pub fn delta(&self) -> FrameDelta<'_> {
        match self {
            StackMapFrame::Same(f) => FrameDelta::Same { offset_delta: f.offset_delta },
            StackMapFrame::SameFrameExtended(f) => FrameDelta::Same { offset_delta: f.offset_delta },
            StackMapFrame::SameLocals1StackItem(f) => FrameDelta::SameLocals1 {
                offset_delta: (f.frame_type - 64) as u16,
                stack: &f.stack,
            },
            StackMapFrame::SameLocals1StackItemExtended(f) => FrameDelta::SameLocals1 {
                offset_delta: f.offset_delta,
                stack: &f.stack,
            },
            StackMapFrame::Chop(f) => FrameDelta::Chop {
                offset_delta: f.offset_delta,
                chopped: (251 - f.frame_type) as usize,
            },
            StackMapFrame::Append(f) => FrameDelta::Append {
                offset_delta: f.offset_delta,
                locals: &f.locals,
            },
            StackMapFrame::Full(f) => FrameDelta::Full {
                offset_delta: f.offset_delta,
                locals: &f.locals,
                stack: &f.stack,
            },
        }
    }
}

/// A stack-map frame normalized to `(offset_delta, delta operation)` — the form the
/// verifier consumes. Each variant says how to derive this frame's type state from
/// the previous one; the verifier turns the `VerificationTypeInfo`s into its own
/// types. The `offset_delta` is always the resolved value (compact forms decoded).
pub enum FrameDelta<'a> {
    /// Same locals as the previous frame, empty operand stack.
    Same { offset_delta: u16 },
    /// Same locals, operand stack of exactly one item.
    SameLocals1 { offset_delta: u16, stack: &'a VerificationTypeInfo },
    /// Drop the last `chopped` locals, empty operand stack.
    Chop { offset_delta: u16, chopped: usize },
    /// Append `locals` to the previous frame's, empty operand stack.
    Append { offset_delta: u16, locals: &'a [VerificationTypeInfo] },
    /// Explicit, complete locals and operand stack.
    Full { offset_delta: u16, locals: &'a [VerificationTypeInfo], stack: &'a [VerificationTypeInfo] },
}

impl FrameDelta<'_> {
    /// The resolved `offset_delta`, regardless of variant.
    pub fn offset_delta(&self) -> u16 {
        match self {
            FrameDelta::Same { offset_delta }
            | FrameDelta::SameLocals1 { offset_delta, .. }
            | FrameDelta::Chop { offset_delta, .. }
            | FrameDelta::Append { offset_delta, .. }
            | FrameDelta::Full { offset_delta, .. } => *offset_delta,
        }
    }
}

/// A `verification_type_info` (JVM spec §4.7.4): the verifier's type for one
/// local variable or operand-stack slot. A 1-byte tag, with two tags carrying
/// an extra `u2`.
pub enum VerificationTypeInfo {
    Top,
    Integer,
    Float,
    Long,
    Double,
    Null,
    UninitializedThis,
    /// Tag 7: a reference whose class is `constant_pool[cpool_index]`.
    Object { cpool_index: u16 },
    /// Tag 8: a not-yet-initialized object created by the `new` at `offset`.
    Uninitialized { offset: u16 },
}

impl VerificationTypeInfo {
    pub fn parse(reader: &mut ClassReader) -> Result<Self, ParseError> {
        let tag = reader.read_u8()?;
        Ok(match tag {
            0 => VerificationTypeInfo::Top,
            1 => VerificationTypeInfo::Integer,
            2 => VerificationTypeInfo::Float,
            // Spec quirk: tag 3 is Double and tag 4 is Long (not in numeric order).
            3 => VerificationTypeInfo::Double,
            4 => VerificationTypeInfo::Long,
            5 => VerificationTypeInfo::Null,
            6 => VerificationTypeInfo::UninitializedThis,
            7 => VerificationTypeInfo::Object { cpool_index: reader.read_u16()? },
            8 => VerificationTypeInfo::Uninitialized { offset: reader.read_u16()? },
            other => return Err(ParseError::BadVerificationType(other)),
        })
    }

    /// The text javap prints for this type, e.g. `int` or `class java/lang/String`.
    pub fn display(&self, cf: &ClassFile) -> String {
        match self {
            VerificationTypeInfo::Top => "top".to_string(),
            VerificationTypeInfo::Integer => "int".to_string(),
            VerificationTypeInfo::Float => "float".to_string(),
            VerificationTypeInfo::Long => "long".to_string(),
            VerificationTypeInfo::Double => "double".to_string(),
            VerificationTypeInfo::Null => "null".to_string(),
            // javap renders ITEM_UninitializedThis simply as `this`.
            VerificationTypeInfo::UninitializedThis => "this".to_string(),
            VerificationTypeInfo::Object { cpool_index } => {
                // Array class names (descriptors beginning with `[`) are quoted.
                let name = cf.class_name(*cpool_index).unwrap_or("?");
                if name.starts_with('[') {
                    format!("class \"{name}\"")
                } else {
                    format!("class {name}")
                }
            }
            VerificationTypeInfo::Uninitialized { offset } => format!("uninitialized {offset}"),
        }
    }
}

/// The decoded `StackMapTable`: its list of frames.
pub struct StackMapTable {
    pub frames: Vec<StackMapFrame>,
}

/// Parses a `StackMapTable` attribute body. Fails on a frame type we don't
/// decode yet, so callers skip printing rather than show a partial table.
pub fn parse(bytes: &[u8]) -> Result<StackMapTable, ParseError> {
    let mut reader = ClassReader::new(bytes);
    let number_of_entries = reader.read_u16()?;
    let mut frames = Vec::with_capacity(number_of_entries as usize);
    for _ in 0..number_of_entries {
        frames.push(parse_frame(&mut reader)?);
    }
    Ok(StackMapTable { frames })
}

fn parse_frame(reader: &mut ClassReader) -> Result<StackMapFrame, ParseError> {
    let frame_type = reader.read_u8()?;
    match frame_type {
        0..=63 => Ok(StackMapFrame::Same(SameFrame::parse(frame_type))),
        64..=127 => Ok(StackMapFrame::SameLocals1StackItem(
            SameLocals1StackItem::parse(frame_type, reader)?,
        )),
        247 => Ok(StackMapFrame::SameLocals1StackItemExtended(
            SameLocals1StackItemExtended::parse(reader)?,
        )),
        248..=250 => Ok(StackMapFrame::Chop(ChopFrame::parse(frame_type, reader)?)),
        251 => Ok(StackMapFrame::SameFrameExtended(SameFrameExtended::parse(reader)?)),
        252..=254 => Ok(StackMapFrame::Append(AppendFrame::parse(frame_type, reader)?)),
        255 => Ok(StackMapFrame::Full(FullFrame::parse(reader)?)),
        other => Err(ParseError::UnsupportedStackMapFrame(other)),
    }
}
