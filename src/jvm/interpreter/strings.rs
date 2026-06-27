//! Minimal `java.lang.String` support — enough to load a string literal (`ldc`) and
//! print it. A real `String` is backed by a `byte[] value` field; we keep it simpler
//! and lay the UTF-8 bytes *inline* in the String object, so the text lives in the
//! heap like everything else:
//!
//! ```text
//! [ class_id(4) | mark(4) | length(4) | utf8 bytes... ]
//! ```
//!
//! No interning/dedup yet (each `ldc` makes a fresh object), so `"a" == "a"` would be
//! false here — fine for printing, a refinement for later.

use super::bytecode_interpreter::class_operations;
use super::bytecode_interpreter::objects_operations::HEADER_SIZE;
use super::heap::HeapService;
use super::metaspace::MetaspaceService;

/// The `length` word (in bytes of UTF-8) sits right after the object header.
const LENGTH_OFFSET: usize = HEADER_SIZE;
/// The UTF-8 payload starts after the header + length word.
const STRING_HEADER: usize = HEADER_SIZE + 4;

/// Allocates a `java.lang.String` on the heap holding `text`, and returns its offset.
/// Loads `String`'s mirror first so the header's `class_id` points at it (an `ldc`
/// of a string literal does exactly this — materialise a String for the constant).
pub fn intern(metaspace: &mut MetaspaceService, heap: &mut HeapService, text: &str) -> usize {
    class_operations::load_class(metaspace, heap, "java/lang/String");
    let uuid = metaspace.class_id("java/lang/String").to_string();
    let mirror = metaspace.class_object(&uuid).unwrap_or(0);

    let bytes = text.as_bytes();
    let offset = heap.malloc(STRING_HEADER + bytes.len());
    heap.write_u32(offset, mirror as u32);
    heap.write_u32(offset + LENGTH_OFFSET, bytes.len() as u32);
    for (i, &byte) in bytes.iter().enumerate() {
        heap.write_u8(offset + STRING_HEADER + i, byte);
    }
    offset
}

/// Reads the text of the `String` object at `offset` back out of the heap.
pub fn read(heap: &HeapService, offset: usize) -> String {
    let bytes = heap.read_bytes(offset + STRING_HEADER, length(heap, offset));
    String::from_utf8_lossy(bytes).into_owned()
}

/// The length (in UTF-8 bytes) of the `String` object at `offset`.
pub fn length(heap: &HeapService, offset: usize) -> usize {
    heap.read_u32(offset + LENGTH_OFFSET) as usize
}

/// The `i`-th byte of the `String` at `offset` — `charAt` for ASCII text.
pub fn char_at(heap: &HeapService, offset: usize, i: usize) -> u8 {
    heap.read_u8(offset + STRING_HEADER + i)
}
