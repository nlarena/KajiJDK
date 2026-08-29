//! Minimal `java.lang.String` support — enough to load a string literal (`ldc`) and
//! print it. A real `String` is backed by a `byte[] value` field; we keep it simpler
//! and lay the text *inline* in the String object, so it lives in the heap like
//! everything else:
//!
//! ```text
//! [ class_id(4) | mark(4) | length(4) | utf16 code units (2 bytes each)... ]
//! ```
//!
//! The units are **UTF-16**, because that is what a Java `String` is: `length()` counts code
//! units and `charAt` returns one, so a supplementary character is two of them. This used to be
//! UTF-8 bytes with the byte count as the length, which made every non-ASCII literal wrong in
//! both directions — `"ñ".length()` answered 2, and `charAt(0)` handed back `0xC3`, the first
//! byte of the encoding rather than the character (COMPILER_FINDINGS #229). The class reader
//! already decodes modified UTF-8 (surrogate pairs included) into real scalar values, so the
//! text arriving here is correct; only the storage was lossy.
//!
//! No interning/dedup yet (each `ldc` makes a fresh object), so `"a" == "a"` would be
//! false here — fine for printing, a refinement for later.

use super::bytecode_interpreter::class_operations;
use super::bytecode_interpreter::objects_operations::HEADER_SIZE;
use super::heap::HeapService;
use super::metaspace::MetaspaceService;

/// The `length` word (in UTF-16 code units) sits right after the object header.
const LENGTH_OFFSET: usize = HEADER_SIZE;
/// The payload starts after the header + length word. Public because it is also the size
/// of an *empty* String, which is what the `new` opcode must allocate for one.
pub const STRING_HEADER: usize = HEADER_SIZE + 4;
/// Bytes per UTF-16 code unit.
const UNIT_SIZE: usize = 2;
/// Object header (8) + the array's length word (4).
const ARRAY_HEADER: usize = HEADER_SIZE + 4;

/// Allocates a `java.lang.String` on the heap holding `text`, and returns its offset.
/// Loads `String`'s mirror first so the header's `class_id` points at it (an `ldc`
/// of a string literal does exactly this — materialise a String for the constant).
pub fn intern(metaspace: &mut MetaspaceService, heap: &mut HeapService, text: &str) -> usize {
    class_operations::load_class(metaspace, heap, "java/lang/String");
    let uuid = metaspace.class_id("java/lang/String").to_string();
    let mirror = metaspace.class_object(&uuid).unwrap_or(0);

    // `encode_utf16` is the whole conversion: a scalar above U+FFFF becomes its surrogate pair,
    // which is exactly how Java counts it.
    let units: Vec<u16> = text.encode_utf16().collect();
    intern_units(metaspace, heap, &units)
}

/// Allocates a `java.lang.String` holding these UTF-16 code units verbatim.
///
/// The one to call when the units come from Java rather than from Rust. `intern` cannot serve
/// that case: its argument is a Rust `String`, and a Rust `String` is well-formed UTF-8 by
/// construction, so an **unpaired surrogate** cannot survive the trip through it. Java's are not
/// so restricted -- a `char[]` may hold a lone `0xD800` and `String.valueOf` must keep it -- so
/// the units are written straight through here, with no encoding step to lose them in.
pub fn intern_units(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    units: &[u16],
) -> usize {
    class_operations::load_class(metaspace, heap, "java/lang/String");
    let uuid = metaspace.class_id("java/lang/String").to_string();
    let mirror = metaspace.class_object(&uuid).unwrap_or(0);
    let offset = heap.malloc(STRING_HEADER + units.len() * UNIT_SIZE);
    heap.write_u32(offset, mirror as u32);
    heap.write_u32(offset + LENGTH_OFFSET, units.len() as u32);
    for (i, &unit) in units.iter().enumerate() {
        heap.write_u16(offset + STRING_HEADER + i * UNIT_SIZE, unit);
    }
    offset
}

/// Reads the text of the `String` object at `offset` back out of the heap. A lone surrogate is
/// replaced rather than rejected — Java allows one to sit in a `String`, Rust's `String` cannot
/// hold it, and the only consumers of this are diagnostics and native bridges.
pub fn read(heap: &HeapService, offset: usize) -> String {
    let units: Vec<u16> = (0..length(heap, offset)).map(|i| char_at(heap, offset, i)).collect();
    String::from_utf16_lossy(&units)
}

/// The length of the `String` at `offset`, in **UTF-16 code units** — `String.length()`.
pub fn length(heap: &HeapService, offset: usize) -> usize {
    heap.read_u32(offset + LENGTH_OFFSET) as usize
}

/// The `i`-th UTF-16 code unit of the `String` at `offset` — `String.charAt`.
pub fn char_at(heap: &HeapService, offset: usize, i: usize) -> u16 {
    heap.read_u16(offset + STRING_HEADER + i * UNIT_SIZE)
}
