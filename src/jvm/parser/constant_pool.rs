//! The constant pool: the class file's symbol table (JVM spec §4.4).

use std::fmt;

use super::reader::{ClassReader, ParseError};

/// One constant pool entry. The leading `tag` byte decides the variant.
#[derive(Debug, Clone)]
pub enum ConstantPoolEntry {
    Utf8(String),                                                     // tag 1
    Integer(i32),                                                     // tag 3
    Float(f32),                                                       // tag 4
    Long(i64),                                                        // tag 5  (2 slots)
    Double(f64),                                                      // tag 6  (2 slots)
    Class { name_index: u16 },                                        // tag 7
    String { string_index: u16 },                                    // tag 8
    FieldRef { class_index: u16, name_and_type_index: u16 },          // tag 9
    MethodRef { class_index: u16, name_and_type_index: u16 },         // tag 10
    InterfaceMethodRef { class_index: u16, name_and_type_index: u16 },// tag 11
    NameAndType { name_index: u16, descriptor_index: u16 },           // tag 12
    MethodHandle { reference_kind: u8, reference_index: u16 },        // tag 15
    MethodType { descriptor_index: u16 },                            // tag 16
    Dynamic { bootstrap_method_attr_index: u16, name_and_type_index: u16 },        // tag 17
    InvokeDynamic { bootstrap_method_attr_index: u16, name_and_type_index: u16 },  // tag 18
    Module { name_index: u16 },                                      // tag 19
    Package { name_index: u16 },                                     // tag 20

    /// Unused slot that follows a Long or Double (they occupy two slots). Keeps
    /// the vector indices aligned with the spec's 1-based indices.
    Tombstone,
}

/// `javap`-style one-line rendering of an entry. Unresolved: shows raw indices
/// (`#n`) rather than the names they point at (that needs an index lookup,
/// which comes later).
impl fmt::Display for ConstantPoolEntry {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        use ConstantPoolEntry::*;
        match self {
            Utf8(s) => write!(f, "{:<18} {}", "Utf8", escape(s)),
            Integer(v) => write!(f, "{:<18} {v}", "Integer"),
            // javap suffixes the wide/float literals by type: `l`, `f`, `d`, and
            // formats float/double with Java's `toString` rules.
            Float(v) => write!(f, "{:<18} {}f", "Float", super::float_to_decimal::java_float(*v)),
            Long(v) => write!(f, "{:<18} {v}l", "Long"),
            Double(v) => write!(f, "{:<18} {}d", "Double", super::float_to_decimal::java_double(*v)),
            Class { name_index } => write!(f, "{:<18} #{name_index}", "Class"),
            String { string_index } => write!(f, "{:<18} #{string_index}", "String"),
            FieldRef { class_index, name_and_type_index } => {
                write!(f, "{:<18} #{class_index}.#{name_and_type_index}", "Fieldref")
            }
            MethodRef { class_index, name_and_type_index } => {
                write!(f, "{:<18} #{class_index}.#{name_and_type_index}", "Methodref")
            }
            InterfaceMethodRef { class_index, name_and_type_index } => {
                write!(f, "{:<18} #{class_index}.#{name_and_type_index}", "InterfaceMethodref")
            }
            NameAndType { name_index, descriptor_index } => {
                write!(f, "{:<18} #{name_index}:#{descriptor_index}", "NameAndType")
            }
            MethodHandle { reference_kind, reference_index } => {
                write!(f, "{:<18} {reference_kind}:#{reference_index}", "MethodHandle")
            }
            MethodType { descriptor_index } => write!(f, "{:<18} #{descriptor_index}", "MethodType"),
            Dynamic { bootstrap_method_attr_index, name_and_type_index } => {
                write!(f, "{:<18} #{bootstrap_method_attr_index}:#{name_and_type_index}", "Dynamic")
            }
            InvokeDynamic { bootstrap_method_attr_index, name_and_type_index } => {
                write!(f, "{:<18} #{bootstrap_method_attr_index}:#{name_and_type_index}", "InvokeDynamic")
            }
            // javap's constant-pool printer has no label for the module-system
            // tags (Module/Package) and falls through to `Unknown`.
            Module { name_index } => write!(f, "{:<18} #{name_index}", "Unknown"),
            Package { name_index } => write!(f, "{:<18} #{name_index}", "Unknown"),
            Tombstone => write!(f, "(large-entry continuation)"),
        }
    }
}

/// Wraps a name in quotes the way javap does in `// ` comments: when it is not a
/// plain binary name — i.e. when it contains any character outside
/// `[A-Za-z0-9/_$]`. This covers array descriptors (`[LX;`) and dotted names
/// (module names, `package-info`, …).
pub fn comment_quote(name: &str) -> String {
    let needs_quote = name
        .chars()
        .any(|c| !(c.is_alphanumeric() || matches!(c, '/' | '_' | '$')));
    if needs_quote {
        format!("\"{name}\"")
    } else {
        name.to_string()
    }
}

/// Escapes a Utf8/String value the way javap does: the named C-style escapes
/// plus `\uXXXX` for other control characters.
pub fn escape(s: &str) -> String {
    let mut out = String::with_capacity(s.len());
    for c in s.chars() {
        match c {
            '\t' => out.push_str("\\t"),
            '\n' => out.push_str("\\n"),
            '\r' => out.push_str("\\r"),
            '\u{8}' => out.push_str("\\b"),
            '\u{c}' => out.push_str("\\f"),
            '"' => out.push_str("\\\""),
            '\'' => out.push_str("\\'"),
            '\\' => out.push_str("\\\\"),
            // Control characters: C0 (< 0x20) plus DEL and the C1 block
            // (0x7F..=0x9F) — javap escapes these as `\uXXXX` too.
            c if (c as u32) < 0x20 || (0x7f..=0x9f).contains(&(c as u32)) => {
                out.push_str(&format!("\\u{:04x}", c as u32))
            }
            c => out.push(c),
        }
    }
    out
}

/// Parses the whole constant pool and returns the list of entries.
///
/// `constant_pool_count` is the value from the header: the pool has
/// `count - 1` logical entries, and is **1-indexed** (entry #1 is `entries[0]`).
pub fn parse(
    reader: &mut ClassReader,
    constant_pool_count: u16,
) -> Result<Vec<ConstantPoolEntry>, ParseError> {
    let mut entries = Vec::new();
    let mut index = 1; // the spec numbers entries from 1

    while index < constant_pool_count {
        let tag = reader.read_u8()?;

        let entry = match tag {
            1 => {
                let length = reader.read_u16()? as usize;
                let raw = reader.read_bytes(length)?;
                ConstantPoolEntry::Utf8(decode_modified_utf8(raw)?)
            }
            3 => ConstantPoolEntry::Integer(reader.read_u32()? as i32),
            4 => ConstantPoolEntry::Float(f32::from_bits(reader.read_u32()?)),
            5 => ConstantPoolEntry::Long(read_u64(reader)? as i64),
            6 => ConstantPoolEntry::Double(f64::from_bits(read_u64(reader)?)),
            7 => ConstantPoolEntry::Class { name_index: reader.read_u16()? },
            8 => ConstantPoolEntry::String { string_index: reader.read_u16()? },
            9 => ConstantPoolEntry::FieldRef {
                class_index: reader.read_u16()?,
                name_and_type_index: reader.read_u16()?,
            },
            10 => ConstantPoolEntry::MethodRef {
                class_index: reader.read_u16()?,
                name_and_type_index: reader.read_u16()?,
            },
            11 => ConstantPoolEntry::InterfaceMethodRef {
                class_index: reader.read_u16()?,
                name_and_type_index: reader.read_u16()?,
            },
            12 => ConstantPoolEntry::NameAndType {
                name_index: reader.read_u16()?,
                descriptor_index: reader.read_u16()?,
            },
            15 => ConstantPoolEntry::MethodHandle {
                reference_kind: reader.read_u8()?,
                reference_index: reader.read_u16()?,
            },
            16 => ConstantPoolEntry::MethodType { descriptor_index: reader.read_u16()? },
            17 => ConstantPoolEntry::Dynamic {
                bootstrap_method_attr_index: reader.read_u16()?,
                name_and_type_index: reader.read_u16()?,
            },
            18 => ConstantPoolEntry::InvokeDynamic {
                bootstrap_method_attr_index: reader.read_u16()?,
                name_and_type_index: reader.read_u16()?,
            },
            19 => ConstantPoolEntry::Module { name_index: reader.read_u16()? },
            20 => ConstantPoolEntry::Package { name_index: reader.read_u16()? },
            other => return Err(ParseError::BadConstantTag(other)),
        };

        // Long and Double take TWO slots: store the entry, then a Tombstone, and
        // advance the index by two so following indices stay correct.
        let is_wide = matches!(entry, ConstantPoolEntry::Long(_) | ConstantPoolEntry::Double(_));
        entries.push(entry);
        index += 1;
        if is_wide {
            entries.push(ConstantPoolEntry::Tombstone);
            index += 1;
        }
    }

    Ok(entries)
}

/// Decodes a Utf8 constant from Java's **modified UTF-8** (JVMS §4.4.7), which
/// differs from standard UTF-8 in two ways:
/// - the null code point (U+0000) is encoded as the two bytes `C0 80` (so a
///   string never contains an embedded `00` byte);
/// - supplementary code points (> U+FFFF) are *not* encoded as a 4-byte
///   sequence; they are first split into a UTF-16 surrogate pair, and each
///   surrogate is then encoded as its own 3-byte sequence (this is CESU-8).
///
/// We decode into a normal Rust `String` (real Unicode scalar values), which is
/// what `javap` ultimately prints. Lone/unpaired surrogates (malformed) are
/// rejected, since a Rust `char` cannot represent them.
fn decode_modified_utf8(bytes: &[u8]) -> Result<String, ParseError> {
    let mut out = String::with_capacity(bytes.len());
    let mut i = 0;
    while i < bytes.len() {
        let b = bytes[i];
        if b & 0x80 == 0 {
            // 1-byte form: 0xxxxxxx (U+0001..U+007F).
            out.push(b as char);
            i += 1;
        } else if b & 0xE0 == 0xC0 {
            // 2-byte form: 110xxxxx 10xxxxxx (U+0000, U+0080..U+07FF).
            let b2 = *bytes.get(i + 1).ok_or(ParseError::BadUtf8)?;
            let cp = (((b as u32) & 0x1F) << 6) | ((b2 as u32) & 0x3F);
            out.push(char::from_u32(cp).ok_or(ParseError::BadUtf8)?);
            i += 2;
        } else if b & 0xF0 == 0xE0 {
            // 3-byte form: 1110xxxx 10xxxxxx 10xxxxxx.
            let b2 = *bytes.get(i + 1).ok_or(ParseError::BadUtf8)?;
            let b3 = *bytes.get(i + 2).ok_or(ParseError::BadUtf8)?;
            let cp = (((b as u32) & 0x0F) << 12) | (((b2 as u32) & 0x3F) << 6) | ((b3 as u32) & 0x3F);
            // A high surrogate (D800..DBFF) followed by a low surrogate (DC00..
            // DFFF) is a CESU-8 pair — combine them into one supplementary char.
            if (0xD800..=0xDBFF).contains(&cp) {
                if let (Some(&0xED), Some(_), Some(_)) =
                    (bytes.get(i + 3), bytes.get(i + 4), bytes.get(i + 5))
                {
                    let (c4, c5, c6) = (bytes[i + 3], bytes[i + 4], bytes[i + 5]);
                    let low = (((c4 as u32) & 0x0F) << 12)
                        | (((c5 as u32) & 0x3F) << 6)
                        | ((c6 as u32) & 0x3F);
                    if (0xDC00..=0xDFFF).contains(&low) {
                        let combined = 0x10000 + ((cp - 0xD800) << 10) + (low - 0xDC00);
                        out.push(char::from_u32(combined).ok_or(ParseError::BadUtf8)?);
                        i += 6;
                        continue;
                    }
                }
            }
            out.push(char::from_u32(cp).ok_or(ParseError::BadUtf8)?);
            i += 3;
        } else {
            return Err(ParseError::BadUtf8);
        }
    }
    Ok(out)
}

/// Reads a big-endian 8-byte unsigned integer (for Long/Double constants).
fn read_u64(reader: &mut ClassReader) -> Result<u64, ParseError> {
    let bytes = reader.read_bytes(8)?;
    Ok(u64::from_be_bytes(bytes.try_into().unwrap()))
}

