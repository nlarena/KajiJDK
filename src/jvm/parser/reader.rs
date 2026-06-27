//! Sequential reader for the `.class` binary format.
//!
//! A `.class` file is a flat, **big-endian** binary with a fixed *order* of
//! sections (JVM spec §4.1) but **no fixed offsets**: most sections are
//! variable-length, so you must parse from the start, in order, to know where
//! the next one begins. That is why [`ClassReader`] is a one-way cursor.
//!
//! Layout of a `ClassFile`, in the exact order it appears on disk:
//!
//! | # | Field | Type | Meaning |
//! |---|-------|------|---------|
//! | 1 | `magic` | u4 | Signature, always `0xCAFEBABE` |
//! | 2 | `minor_version` / `major_version` | u2, u2 | Class file format version (65 = Java 21) |
//! | 3 | `constant_pool_count` + `constant_pool[]` | u2 + entries | Symbol table (names, types, literals); 1-indexed |
//! | 4 | `access_flags` | u2 | `public`/`final`/`abstract`/`interface`… of the class |
//! | 5 | `this_class` | u2 | Constant-pool index -> this class's name |
//! | 6 | `super_class` | u2 | Constant-pool index -> superclass (0 only for `Object`) |
//! | 7 | `interfaces_count` + `interfaces[]` | u2 + u2[] | Constant-pool indices -> implemented interfaces |
//! | 8 | `fields_count` + `fields[]` | u2 + field_info[] | Field declarations |
//! | 9 | `methods_count` + `methods[]` | u2 + method_info[] | Method declarations (bytecode lives here, in the `Code` attribute) |
//! | 10 | `attributes_count` + `attributes[]` | u2 + attribute_info[] | Class-level attributes (`SourceFile`, …) |
//!
//! We parse these top to bottom. For now this module exposes only the raw
//! `read_*` primitives; the higher-level structs that consume them come next.

use std::fmt;

/// Parse error. We'll extend it as the parser grows.
#[derive(Debug)]
pub enum ParseError {
    /// Asked for more bytes than remain. `needed` bytes from offset `at`.
    UnexpectedEof { needed: usize, at: usize },
    /// Magic wasn't 0xCAFEBABE.
    BadMagic(u32),
    /// Unknown constant pool tag byte.
    BadConstantTag(u8),
    /// A Utf8 constant held invalid (modified) UTF-8.
    BadUtf8,
    /// A `StackMapTable` frame type we don't decode yet.
    UnsupportedStackMapFrame(u8),
    /// A `verification_type_info` tag outside the 0–8 range (JVM spec §4.7.4).
    BadVerificationType(u8),
    /// The file couldn't be read from disk.
    Io(std::io::Error),
}

impl fmt::Display for ParseError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ParseError::UnexpectedEof { needed, at } => {
                write!(f, "unexpected EOF: needed {needed} bytes at offset {at}")
            }
            ParseError::BadMagic(found) => {
                write!(f, "invalid magic: {found:#010X} (expected 0xCAFEBABE)")
            }
            ParseError::BadConstantTag(tag) => write!(f, "unknown constant pool tag: {tag}"),
            ParseError::BadUtf8 => write!(f, "invalid modified UTF-8 in a Utf8 constant"),
            ParseError::UnsupportedStackMapFrame(t) => {
                write!(f, "unsupported StackMapTable frame type: {t}")
            }
            ParseError::BadVerificationType(t) => {
                write!(f, "invalid verification_type_info tag: {t}")
            }
            ParseError::Io(e) => write!(f, "I/O error: {e}"),
        }
    }
}

impl std::error::Error for ParseError {}

/// Lets `?` turn a file-read `io::Error` into a `ParseError` automatically.
impl From<std::io::Error> for ParseError {
    fn from(e: std::io::Error) -> Self {
        ParseError::Io(e)
    }
}

/// Cursor over the bytes of a `.class` file. The format is **big-endian**.
///
/// Keeps a `position` index that advances on every read. All reads return a
/// `Result` because they can run out of bytes.
///
/// The JVM spec names these types `u1`/`u2`/`u4` (1/2/4 bytes). Here the methods
/// are named after the Rust type they return:
/// - `read_u8`  -> 1 byte  (spec `u1`)
/// - `read_u16` -> 2 bytes (spec `u2`)
/// - `read_u32` -> 4 bytes (spec `u4`)
pub struct ClassReader<'a> {
    bytes: &'a [u8],
    position: usize,
}

impl<'a> ClassReader<'a> {
    pub fn new(bytes: &'a [u8]) -> Self {
        ClassReader { bytes, position: 0 }
    }

    /// Reads `count` raw bytes and advances the cursor. Building block for the
    /// `read_u*` methods.
    pub fn read_bytes(&mut self, count: usize) -> Result<&'a [u8], ParseError> {
        let start = self.position;   // where I start
        let end = start + count;     // where reading would end

        if end > self.bytes.len() {  // past the end of the slice?
            return Err(ParseError::UnexpectedEof { needed: count, at: start });
        }

        self.position = end;         // advance the cursor (that's why it's &mut self)
        Ok(&self.bytes[start..end])  // return the byte window wrapped in Ok
    }

    /// Reads a single unsigned byte (JVM spec `u1`).
    pub fn read_u8(&mut self) -> Result<u8, ParseError> {
        let bytes = self.read_bytes(1)?; // `?`: on error, return the Err right away
        Ok(bytes[0])                     // the slice's only byte
    }

    /// Reads a 16-bit big-endian unsigned integer (JVM spec `u2`).
    pub fn read_u16(&mut self) -> Result<u16, ParseError> {
        let bytes = self.read_bytes(2)?;
        // big-endian: bytes[0] is the most significant. from_be_bytes assembles it.
        Ok(u16::from_be_bytes([bytes[0], bytes[1]]))
    }

    /// Reads a 32-bit big-endian unsigned integer (JVM spec `u4`).
    pub fn read_u32(&mut self) -> Result<u32, ParseError> {
        let bytes = self.read_bytes(4)?;
        Ok(u32::from_be_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]))
    }
}

#[cfg(test)] // only compiled under `cargo test`
mod tests {
    use super::*; // bring ClassReader and ParseError into scope

    #[test]
    fn reads_sample_class_header() {
        let bytes = std::fs::read("java/Sample.class").expect("missing java/Sample.class");
        let mut reader = ClassReader::new(&bytes);
        assert_eq!(reader.read_u32().unwrap(), 0xCAFE_BABE); // magic
        assert_eq!(reader.read_u16().unwrap(), 0); // minor version
        assert_eq!(reader.read_u16().unwrap(), 65); // major version (Java 21)
    }

    #[test]
    fn running_out_of_bytes_errors() {
        let data = [0x01u8, 0x02]; // only 2 bytes
        let mut reader = ClassReader::new(&data);
        assert!(reader.read_u32().is_err()); // asking for 4 -> UnexpectedEof
    }
}
