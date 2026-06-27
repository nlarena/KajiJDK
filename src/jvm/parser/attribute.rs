//! Generic `attribute_info` (JVM spec §4.7): a named blob whose body format
//! depends on the name (`Code`, `SourceFile`, `LineNumberTable`, …). We keep the
//! raw bytes here; interpreting specific attributes (like `Code`'s bytecode)
//! comes later, at the interpreter stage.

use super::reader::{ClassReader, ParseError};

/// One `attribute_info`: a name index plus a length-prefixed raw byte body.
#[derive(Debug, Clone)]
pub struct AttributeInfo {
    /// Constant-pool index of the attribute's name (a `Utf8`, e.g. "Code").
    pub name_index: u16,
    /// The raw attribute body, stored verbatim (its length is a `u4` in the file).
    pub info: Vec<u8>,
}

/// Reads a `u2 attributes_count` followed by that many `attribute_info`.
/// Reusable for class-, field- and method-level attributes (all share this shape).
pub fn parse_attributes(reader: &mut ClassReader) -> Result<Vec<AttributeInfo>, ParseError> {
    let count = reader.read_u16()?;
    let mut attributes = Vec::with_capacity(count as usize);
    for _ in 0..count {
        attributes.push(parse_attribute(reader)?);
    }
    Ok(attributes)
}

fn parse_attribute(reader: &mut ClassReader) -> Result<AttributeInfo, ParseError> {
    let name_index = reader.read_u16()?;
    let length = reader.read_u32()? as usize;
    let info = reader.read_bytes(length)?.to_vec();
    Ok(AttributeInfo { name_index, info })
}
