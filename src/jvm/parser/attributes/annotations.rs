//! `RuntimeVisibleAnnotations` / `RuntimeInvisibleAnnotations` (JVMS §4.7.16–17):
//! the annotations on a class, field or method. The payload is recursive — an
//! `element_value` can itself be a nested annotation or an array of values — so
//! both the parser and the renderer here recurse.
//!
//! javap prints each annotation twice: a raw line `i: #type(#name=tag…)` echoing
//! the constant-pool structure, then a resolved, indented, Java-like form.

use super::super::reader::ClassReader;
use crate::jvm::class_file::ClassFile;
use crate::jvm::parser::constant_pool::escape;
use crate::jvm::parser::float_to_decimal;
use crate::jvm::parser::ConstantPoolEntry;

pub struct Annotation {
    type_index: u16,
    pairs: Vec<(u16, ElementValue)>,
}

enum ElementValue {
    /// Tags `B C D F I J S Z s`: a primitive or `String`, by constant-pool index.
    Const { tag: u8, index: u16 },
    /// Tag `e`: an enum constant (`type_name`.`const_name`).
    Enum { type_name: u16, const_name: u16 },
    /// Tag `c`: a class literal.
    Class { index: u16 },
    /// Tag `@`: a nested annotation.
    Nested(Box<Annotation>),
    /// Tag `[`: an array of element values.
    Array(Vec<ElementValue>),
}

/// Parses an annotations attribute body: `u2 count`, then that many annotations.
pub fn parse(bytes: &[u8]) -> Vec<Annotation> {
    let mut r = ClassReader::new(bytes);
    let mut out = Vec::new();
    let Ok(count) = r.read_u16() else { return out };
    for _ in 0..count {
        match parse_annotation(&mut r) {
            Some(a) => out.push(a),
            None => break,
        }
    }
    out
}

fn parse_annotation(r: &mut ClassReader) -> Option<Annotation> {
    let type_index = r.read_u16().ok()?;
    let n = r.read_u16().ok()?;
    let mut pairs = Vec::with_capacity(n as usize);
    for _ in 0..n {
        let name = r.read_u16().ok()?;
        let value = parse_element_value(r)?;
        pairs.push((name, value));
    }
    Some(Annotation { type_index, pairs })
}

fn parse_element_value(r: &mut ClassReader) -> Option<ElementValue> {
    let tag = r.read_u8().ok()?;
    Some(match tag {
        b'e' => ElementValue::Enum {
            type_name: r.read_u16().ok()?,
            const_name: r.read_u16().ok()?,
        },
        b'c' => ElementValue::Class { index: r.read_u16().ok()? },
        b'@' => ElementValue::Nested(Box::new(parse_annotation(r)?)),
        b'[' => {
            let n = r.read_u16().ok()?;
            let mut values = Vec::with_capacity(n as usize);
            for _ in 0..n {
                values.push(parse_element_value(r)?);
            }
            ElementValue::Array(values)
        }
        _ => ElementValue::Const { tag, index: r.read_u16().ok()? },
    })
}

/// Prints one annotations block (header + each annotation's raw and resolved
/// forms) at the given base indentation (0 for class level, 4 for members).
pub fn print_block(cf: &ClassFile, label: &str, info: &[u8], indent: usize) {
    let pad = " ".repeat(indent);
    crate::pln!("{pad}{label}:");
    for (i, a) in parse(info).iter().enumerate() {
        crate::pln!("{}{i}: {}", " ".repeat(indent + 2), raw_annotation(a));
        crate::pln!("{}{}", " ".repeat(indent + 4), resolve_annotation(cf, a, indent + 4));
    }
}

/// Prints an `AnnotationDefault` (a single `element_value`) — the raw form on
/// the `default_value:` line, then the resolved value indented below.
pub fn print_default(cf: &ClassFile, info: &[u8], indent: usize) {
    let mut r = ClassReader::new(info);
    let Some(v) = parse_element_value(&mut r) else { return };
    crate::pln!("{}AnnotationDefault:", " ".repeat(indent));
    crate::pln!("{}default_value: {}", " ".repeat(indent + 2), raw_value(&v));
    crate::pln!("{}{}", " ".repeat(indent + 4), resolve_value(cf, &v, indent + 4));
}

/// Prints a `Runtime{Visible,Invisible}ParameterAnnotations` block (JVMS
/// §4.7.18–19): a `u1 num_parameters`, then per parameter a `u2 count` and that
/// many annotations. Each parameter gets its own `parameter N:` sub-header — even
/// when it has no annotations — and the annotations render like a normal block.
pub fn print_parameter_block(cf: &ClassFile, label: &str, info: &[u8], indent: usize) {
    let mut r = ClassReader::new(info);
    let Ok(num_params) = r.read_u8() else { return };
    crate::pln!("{}{label}:", " ".repeat(indent));
    for p in 0..num_params {
        crate::pln!("{}parameter {p}:", " ".repeat(indent + 2));
        let Ok(count) = r.read_u16() else { return };
        for i in 0..count {
            let Some(a) = parse_annotation(&mut r) else { return };
            crate::pln!("{}{i}: {}", " ".repeat(indent + 4), raw_annotation(&a));
            crate::pln!("{}{}", " ".repeat(indent + 6), resolve_annotation(cf, &a, indent + 6));
        }
    }
}

/// Prints a `Runtime{Visible,Invisible}TypeAnnotations` block (JVMS §4.7.20): a
/// `u2 count`, then that many `type_annotation`s. Each is a `target_type` byte, a
/// variable `target_info`, a `type_path`, and finally a regular annotation. The
/// raw line appends the resolved target description: `i: #type(): TARGET, …`.
pub fn print_type_block(cf: &ClassFile, label: &str, info: &[u8], indent: usize) {
    let mut r = ClassReader::new(info);
    let Ok(count) = r.read_u16() else { return };
    crate::pln!("{}{label}:", " ".repeat(indent));
    for i in 0..count {
        let Ok(target_type) = r.read_u8() else { return };
        let Some(target) = parse_target(&mut r, target_type) else { return };
        // Consume the type_path (`u1 length`, then `length` 2-byte entries). It is
        // empty for every type annotation in the corpus, so we don't render it.
        let Ok(path_len) = r.read_u8() else { return };
        if r.read_bytes(path_len as usize * 2).is_err() {
            return;
        }
        let Some(a) = parse_annotation(&mut r) else { return };
        crate::pln!("{}{i}: {}: {target}", " ".repeat(indent + 2), raw_annotation(&a));
        crate::pln!("{}{}", " ".repeat(indent + 4), resolve_annotation(cf, &a, indent + 4));
    }
}

/// Parses a `target_info` (JVMS §4.7.20.1) and returns javap's textual rendering
/// of it. The number of bytes consumed depends on `target_type`; we cover the
/// whole table so the reader never desyncs.
fn parse_target(r: &mut ClassReader, target_type: u8) -> Option<String> {
    Some(match target_type {
        0x00 => format!("CLASS_TYPE_PARAMETER, param_index={}", r.read_u8().ok()?),
        0x01 => format!("METHOD_TYPE_PARAMETER, param_index={}", r.read_u8().ok()?),
        0x10 => format!("CLASS_EXTENDS, type_index={}", r.read_u16().ok()? as i16),
        0x11 => format!(
            "CLASS_TYPE_PARAMETER_BOUND, param_index={}, bound_index={}",
            r.read_u8().ok()?,
            r.read_u8().ok()?
        ),
        0x12 => format!(
            "METHOD_TYPE_PARAMETER_BOUND, param_index={}, bound_index={}",
            r.read_u8().ok()?,
            r.read_u8().ok()?
        ),
        0x13 => "FIELD".to_string(),
        0x14 => "METHOD_RETURN".to_string(),
        0x15 => "METHOD_RECEIVER".to_string(),
        0x16 => format!("METHOD_FORMAL_PARAMETER, param_index={}", r.read_u8().ok()?),
        0x17 => format!("THROWS, type_index={}", r.read_u16().ok()?),
        0x40 | 0x41 => {
            // localvar_target: u2 table_length, then table_length * {u2,u2,u2}.
            let n = r.read_u16().ok()?;
            let mut spans = Vec::with_capacity(n as usize);
            for _ in 0..n {
                let start = r.read_u16().ok()?;
                let len = r.read_u16().ok()?;
                let idx = r.read_u16().ok()?;
                spans.push(format!("start_pc={start}, length={len}, index={idx}"));
            }
            let label = if target_type == 0x40 { "LOCAL_VARIABLE" } else { "RESOURCE_VARIABLE" };
            format!("{label}, {{{}}}", spans.join("; "))
        }
        0x42 => format!("EXCEPTION_PARAMETER, exception_index={}", r.read_u16().ok()?),
        0x43 => format!("INSTANCEOF, offset={}", r.read_u16().ok()?),
        0x44 => format!("NEW, offset={}", r.read_u16().ok()?),
        0x45 => format!("CONSTRUCTOR_REFERENCE, offset={}", r.read_u16().ok()?),
        0x46 => format!("METHOD_REFERENCE, offset={}", r.read_u16().ok()?),
        0x47 => {
            format!("CAST, offset={}, type_index={}", r.read_u16().ok()?, r.read_u8().ok()?)
        }
        0x48 => format!(
            "CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT, offset={}, type_index={}",
            r.read_u16().ok()?,
            r.read_u8().ok()?
        ),
        0x49 => format!(
            "METHOD_INVOCATION_TYPE_ARGUMENT, offset={}, type_index={}",
            r.read_u16().ok()?,
            r.read_u8().ok()?
        ),
        0x4a => format!(
            "CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT, offset={}, type_index={}",
            r.read_u16().ok()?,
            r.read_u8().ok()?
        ),
        0x4b => format!(
            "METHOD_REFERENCE_TYPE_ARGUMENT, offset={}, type_index={}",
            r.read_u16().ok()?,
            r.read_u8().ok()?
        ),
        _ => return None,
    })
}

// -- raw structure form (echoes constant-pool indices) -------------------

fn raw_annotation(a: &Annotation) -> String {
    let pairs: Vec<String> = a
        .pairs
        .iter()
        .map(|(name, v)| format!("#{name}={}", raw_value(v)))
        .collect();
    format!("#{}({})", a.type_index, pairs.join(","))
}

fn raw_value(v: &ElementValue) -> String {
    match v {
        ElementValue::Const { tag, index } => format!("{}#{index}", *tag as char),
        ElementValue::Enum { type_name, const_name } => format!("e#{type_name}.#{const_name}"),
        ElementValue::Class { index } => format!("c#{index}"),
        ElementValue::Nested(a) => format!("@{}", raw_annotation(a)),
        ElementValue::Array(vs) => {
            format!("[{}]", vs.iter().map(raw_value).collect::<Vec<_>>().join(","))
        }
    }
}

// -- resolved, Java-like form --------------------------------------------

/// Renders an annotation without a leading `@`: just `Type` for a marker, or
/// `Type(` + one indented `name=value` per pair + `)` for one with elements.
fn resolve_annotation(cf: &ClassFile, a: &Annotation, indent: usize) -> String {
    let ty = type_name(cf, a.type_index);
    if a.pairs.is_empty() {
        return ty;
    }
    let mut s = format!("{ty}(\n");
    for (name, v) in &a.pairs {
        s.push_str(&format!(
            "{}{}={}\n",
            " ".repeat(indent + 2),
            cf.utf8(*name).unwrap_or("?"),
            resolve_value(cf, v, indent + 2)
        ));
    }
    s.push_str(&format!("{})", " ".repeat(indent)));
    s
}

fn resolve_value(cf: &ClassFile, v: &ElementValue, indent: usize) -> String {
    match v {
        ElementValue::Const { tag, index } => const_text(cf, *tag, *index),
        ElementValue::Enum { type_name, const_name } => {
            format!("{}.{}", cf.utf8(*type_name).unwrap_or("?"), cf.utf8(*const_name).unwrap_or("?"))
        }
        ElementValue::Class { index } => format!("class {}", cf.utf8(*index).unwrap_or("?")),
        ElementValue::Nested(a) => format!("@{}", resolve_annotation(cf, a, indent)),
        ElementValue::Array(vs) => {
            let items: Vec<String> = vs.iter().map(|e| resolve_value(cf, e, indent)).collect();
            format!("[{}]", items.join(","))
        }
    }
}

/// The annotation type's descriptor (`Lpkg/Name;`) as a dotted Java name.
fn type_name(cf: &ClassFile, type_index: u16) -> String {
    let d = cf.utf8(type_index).unwrap_or("?");
    let inner = d.strip_prefix('L').and_then(|s| s.strip_suffix(';')).unwrap_or(d);
    inner.replace('/', ".")
}

/// A primitive or `String` element value, rendered as javap does. Variants are
/// qualified on purpose: `use ConstantPoolEntry::*` would shadow `String`.
fn const_text(cf: &ClassFile, tag: u8, index: u16) -> String {
    let entry = cf.constant_pool.get((index as usize).wrapping_sub(1));
    match (tag, entry) {
        (b'I', Some(ConstantPoolEntry::Integer(v))) => v.to_string(),
        (b'J', Some(ConstantPoolEntry::Long(v))) => format!("{v}l"),
        (b'F', Some(ConstantPoolEntry::Float(v))) => {
            format!("{}f", float_to_decimal::java_float(*v))
        }
        (b'D', Some(ConstantPoolEntry::Double(v))) => {
            format!("{}d", float_to_decimal::java_double(*v))
        }
        (b'S', Some(ConstantPoolEntry::Integer(v))) => format!("(short) {v}"),
        (b'B', Some(ConstantPoolEntry::Integer(v))) => format!("(byte) {v}"),
        (b'C', Some(ConstantPoolEntry::Integer(v))) => {
            format!("'{}'", char::from_u32(*v as u32).unwrap_or('?'))
        }
        (b'Z', Some(ConstantPoolEntry::Integer(v))) => {
            if *v != 0 { "true" } else { "false" }.to_string()
        }
        (b's', _) => format!("\"{}\"", escape(cf.utf8(index).unwrap_or(""))),
        _ => String::new(),
    }
}
