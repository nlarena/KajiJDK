//! The `Code` attribute (JVM spec §4.7.3): a method's bytecode plus its frame
//! sizes, exception table and nested attributes. Parsed on demand from the raw
//! bytes that the generic `AttributeInfo` stored.

use super::attribute::{self, AttributeInfo};
use super::reader::{ClassReader, ParseError};

/// One row of a method's exception table (a `try`/`catch` range).
#[derive(Debug, Clone)]
pub struct ExceptionTableEntry {
    pub start_pc: u16,
    pub end_pc: u16,
    pub handler_pc: u16,
    /// Constant-pool index of the caught exception's `Class` (0 = any, `finally`).
    pub catch_type: u16,
}

/// The decoded body of a method's `Code` attribute.
#[derive(Debug, Clone)]
pub struct Code {
    /// Max depth of the operand stack this method needs.
    pub max_stack: u16,
    /// Number of local variable slots (includes `this` and the arguments).
    pub max_locals: u16,
    /// The raw bytecode: opcodes + operands, still to be disassembled.
    pub code: Vec<u8>,
    pub exception_table: Vec<ExceptionTableEntry>,
    /// Nested attributes (`LineNumberTable`, `StackMapTable`, …).
    pub attributes: Vec<AttributeInfo>,
}

/// Parses a `Code` attribute body from its raw bytes.
pub fn parse(bytes: &[u8]) -> Result<Code, ParseError> {
    let mut reader = ClassReader::new(bytes);

    let max_stack = reader.read_u16()?;
    let max_locals = reader.read_u16()?;

    let code_length = reader.read_u32()? as usize;
    let code = reader.read_bytes(code_length)?.to_vec();

    let exception_table_length = reader.read_u16()?;
    let mut exception_table = Vec::with_capacity(exception_table_length as usize);
    for _ in 0..exception_table_length {
        // Struct fields evaluate in source order → read in the spec's order.
        exception_table.push(ExceptionTableEntry {
            start_pc: reader.read_u16()?,
            end_pc: reader.read_u16()?,
            handler_pc: reader.read_u16()?,
            catch_type: reader.read_u16()?,
        });
    }

    let attributes = attribute::parse_attributes(&mut reader)?;

    Ok(Code {
        max_stack,
        max_locals,
        code,
        exception_table,
        attributes,
    })
}
