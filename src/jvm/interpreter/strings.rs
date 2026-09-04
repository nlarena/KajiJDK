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
//! # Interning (JLS §3.10.5)
//!
//! A literal is a reference to a **pooled** instance: two literals with the same contents, in the
//! same class or in different ones, are the *same* reference, so `"a" == "a"` is `true`. What the
//! program **computes** — a runtime concatenation, a `new String(…)`, `String.valueOf` — is a
//! distinct object, and that is not a detail of the same rule but the other half of it: a pool that
//! swallowed computed strings would make `new String("a") == "a"` answer `true`, which is just as
//! wrong in the other direction.
//!
//! Hence two entry points that look alike and must not be confused: [`intern`] for a literal,
//! [`allocate`] for everything else.
//!
//! # Which side of the line a caller falls on
//!
//! The rule is **not** "three callers, the rest allocate", which is what this paragraph used to
//! say and what the callers never matched. It is the one the reference implementation actually
//! follows, and it was read off `java` 25 rather than reasoned about:
//!
//! > A string that *is* a symbol — an entry of some class file's constant pool — is **pooled**. A
//! > string the VM **computes**, character by character, is not.
//!
//! Every measurement lines up with it. `String.class.getName() == "java.lang.String"` is `true` on
//! `java` 25, and so is `field.getName() == "fld"` and `int[].class.getName() == "[I"`: those names
//! come straight off a `Symbol`, and HotSpot interns them. `getSimpleName() == "String"` is
//! `false`, `descriptorString() == descriptorString()` is `false`, and
//! `e.toString() == e.toString()` is `false`: each of those is *built* — a substring, a
//! concatenation — and none of them is in the table.
//!
//! So [`intern`] is for the constant-pool side: `ldc` of a `String` constant, the `ConstantValue`
//! of a static `String` field (JVMS §5.4.2), a `String` static argument of a bootstrap method — the
//! three places a pool *entry* becomes an object — plus the reflective names that are pool entries
//! read back out (`Class.name0`, `innerName0`, a `Method`/`Field`/`RecordComponent` name, the
//! `MethodHandle` owner/name/descriptor triple) and `String.intern()` itself, which is the one
//! place a program may add to the table on purpose.
//!
//! [`allocate`] is for everything the VM builds: a concatenation, a `new String(…)`,
//! `String.valueOf`, `Throwable.toString`, `Class.getSimpleName`/`descriptorString`,
//! `System.mapLibraryName`, a file name from `Fs.list`, a socket address, a rendered stack trace.
//! Two reasons, and either alone decides it: `java` answers `false` to the identity question for
//! all of them, and the pool is **permanent** — a pooled string is a GC root, pinned out of the
//! compactor, alive for the life of the VM. Pooling `Fs.list`'s output is not a subtle semantic
//! slip, it is a directory listing that can never be freed.
//!
//! **The pool is allocated in Old, is a GC root and is pinned in `gc::compact`.** All three are
//! required and for different reasons: between two `ldc`s of the same literal nothing else refers
//! to it, so without the root the first collection frees it; and a literal that moved would leave
//! every reference to it dangling, with no second copy of the identity to restore. This was FZ-008,
//! and it was open for a long time behind a probe the compiler folded away (FZ-009).

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

/// The **pooled** `String` for `text` — the same object every time (JLS §3.10.5).
///
/// For a *literal* only. A computed String goes through [`allocate`]; see the module docs for why
/// the distinction is the rule rather than an optimisation.
pub fn intern(metaspace: &mut MetaspaceService, heap: &mut HeapService, text: &str) -> usize {
    // `encode_utf16` is the whole conversion: a scalar above U+FFFF becomes its surrogate pair,
    // which is exactly how Java counts it.
    let units: Vec<u16> = text.encode_utf16().collect();
    intern_units(metaspace, heap, &units)
}

/// [`intern`] for units that come from Java rather than from Rust — see [`allocate_units`] for why
/// the pair exists.
pub fn intern_units(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    units: &[u16],
) -> usize {
    if let Some(offset) = metaspace.interned_string(units) {
        return offset;
    }
    // **Old, not Eden.** A literal outlives every collection by definition, so putting it in the
    // young generation would mean copying it on every minor for the lifetime of the program — and
    // it has to be pinned anyway, which the young generation has no notion of.
    let offset = allocate_in(metaspace, heap, units, Generation::Old);
    metaspace.set_interned_string(units.to_vec(), offset);
    offset
}

/// A **fresh** `java.lang.String` holding `text` — never pooled.
///
/// Everything the program computes lands here. Sharing one of these with a literal would make
/// `new String("a") == "a"` answer `true`.
pub fn allocate(metaspace: &mut MetaspaceService, heap: &mut HeapService, text: &str) -> usize {
    let units: Vec<u16> = text.encode_utf16().collect();
    allocate_units(metaspace, heap, &units)
}

/// Where a `String` object is built, for both entry points.
fn allocate_in(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    units: &[u16],
    generation: Generation,
) -> usize {
    class_operations::load_class(metaspace, heap, "java/lang/String");
    let uuid = metaspace.class_id("java/lang/String").to_string();
    let mirror = metaspace.class_object(&uuid).unwrap_or(0);
    let size = STRING_HEADER + units.len() * UNIT_SIZE;
    let offset = match generation {
        Generation::Young => heap.malloc(size),
        Generation::Old => heap.malloc_old(size),
    };
    heap.write_u32(offset, mirror as u32);
    heap.write_u32(offset + LENGTH_OFFSET, units.len() as u32);
    for (i, &unit) in units.iter().enumerate() {
        heap.write_u16(offset + STRING_HEADER + i * UNIT_SIZE, unit);
    }
    offset
}

/// Which half of the heap a new `String` goes in.
enum Generation {
    Young,
    Old,
}

/// A fresh `java.lang.String` holding these UTF-16 code units verbatim.
///
/// The one to call when the units come from Java rather than from Rust. [`allocate`] cannot serve
/// that case: its argument is a Rust `String`, and a Rust `String` is well-formed UTF-8 by
/// construction, so an **unpaired surrogate** cannot survive the trip through it. Java's are not
/// so restricted -- a `char[]` may hold a lone `0xD800` and `String.valueOf` must keep it -- so
/// the units are written straight through here, with no encoding step to lose them in.
pub fn allocate_units(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    units: &[u16],
) -> usize {
    allocate_in(metaspace, heap, units, Generation::Young)
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
