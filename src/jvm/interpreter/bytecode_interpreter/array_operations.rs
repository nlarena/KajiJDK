//! Array opcodes — allocating arrays on the heap. An array *is* an object, but with
//! a `length` word after the header and a homogeneous, indexed payload of fixed-size
//! slots:
//!
//! ```text
//! [ class_id(4) | mark(4) | length(4) | elem0(4) | elem1(4) | ... ]
//!   └──── object header ───┘   ┌── one 4-byte slot per element ──┘
//! ```
//!
//! Every slot is the same width (4 bytes) whatever the element type — for a
//! primitive array it holds the value, for a reference array it holds a reference
//! (a heap offset, `0` = null). That fixed width is *why* object arrays store
//! references and not the objects inline: subclasses have different sizes, so you
//! can't pack variable-size objects into fixed slots. So `new Dog[3]` reserves three
//! null slots — the `Dog`s are allocated separately and their references stored.
//!
//! The array carries its own **array class** (`"[I"`, `"[LDog;"`), whose descriptor
//! encodes the element kind — needed so `aaload` pushes a reference (not an int) and
//! so the GC knows which slots to trace.

use super::objects_operations::{HEADER_SIZE, SLOT_SIZE};
use crate::jvm::interpreter::frame::{Frame, Value};
use crate::jvm::interpreter::heap::HeapService;
use crate::jvm::interpreter::metaspace::MetaspaceService;

/// The `length` word sits right after the object header.
const LENGTH_OFFSET: usize = HEADER_SIZE;
/// An array's header is the object header plus the `length` word; elements follow.
const ARRAY_HEADER_SIZE: usize = HEADER_SIZE + 4;

// The implicit exceptions array opcodes can raise; returned as `Err` for the
// dispatch loop to throw (`throw_exception`).
const NULL_POINTER: &str = "java/lang/NullPointerException";
const ARRAY_INDEX: &str = "java/lang/ArrayIndexOutOfBoundsException";
const NEGATIVE_SIZE: &str = "java/lang/NegativeArraySizeException";

/// `newarray` (0xbc): allocate a **primitive** array. `atype` names the element
/// type; we model the int-category primitives, each with its faithful element width
/// (so a `byte[10]` is 10 bytes, not 40). The array's class is its descriptor.
pub fn newarray(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    frame: &mut Frame,
    atype: u8,
) -> Result<(), &'static str> {
    let (array_class, elem_size) = match atype {
        4 => ("[Z", 1),  // T_BOOLEAN (stored as a byte)
        5 => ("[C", 2),  // T_CHAR
        6 => ("[F", 4),  // T_FLOAT
        7 => ("[D", 8),  // T_DOUBLE (category-2: 8-byte elements)
        8 => ("[B", 1),  // T_BYTE
        9 => ("[S", 2),  // T_SHORT
        10 => ("[I", 4), // T_INT
        11 => ("[J", 8), // T_LONG (category-2)
        _ => panic!("newarray: unknown primitive atype {atype}"),
    };
    let count = pop_count(frame)?; // negative length → NegativeArraySizeException
    let offset = allocate_array(metaspace, heap, array_class, count, elem_size);
    frame.push(Value::Reference(offset));
    Ok(())
}

/// `anewarray` (0xbd): allocate a **reference** array. `cp_index` names the *element*
/// class; the array's class is `"[L<element>;"`. The slots start as null.
pub fn anewarray(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    frame: &mut Frame,
    cp_index: u16,
) -> Result<(), &'static str> {
    let caller = metaspace.class_of(frame.method()).to_string();
    let element = metaspace
        .get(&caller)
        .and_then(|cf| cf.class_name(cp_index))
        .expect("anewarray: cp_index does not point to a Class constant")
        .to_string();
    let array_class = format!("[L{element};");
    let count = pop_count(frame)?; // negative length → NegativeArraySizeException
    // A reference element is one heap offset wide.
    let offset = allocate_array(metaspace, heap, &array_class, count, SLOT_SIZE);
    frame.push(Value::Reference(offset));
    Ok(())
}

/// `multianewarray` (0xc5): allocate a **multidimensional** array. `cp_index` names the
/// *array* type itself (unlike `anewarray`, which names the element type) — e.g. `[[I`
/// for `new int[3][4]` — and `dimensions` says how many levels to actually build.
///
/// The key rule is that **only `dimensions` levels are materialised**, even when the
/// descriptor is deeper: `new int[3][]` is `dimensions = 1` over `[[I`, so it allocates
/// the outer array of 3 slots and leaves them `null`. That is why the dimension count
/// is an operand at all instead of being derived from the descriptor.
pub fn multianewarray(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    frame: &mut Frame,
    cp_index: u16,
    dimensions: u8,
) -> Result<(), &'static str> {
    let caller = metaspace.class_of(frame.method()).to_string();
    let array_class = metaspace
        .get(&caller)
        .and_then(|cf| cf.class_name(cp_index))
        .expect("multianewarray: cp_index does not point to a Class constant")
        .to_string();

    // The counts were pushed outermost-first, so the *last* dimension is on top and
    // popping yields them backwards.
    let mut counts: Vec<i32> = (0..dimensions).map(|_| pop_int(frame)).collect();
    counts.reverse();

    // Every count is validated *before* anything is allocated: a negative length in a
    // later dimension must not leave a half-built array on the heap.
    if counts.iter().any(|&n| n < 0) {
        return Err(NEGATIVE_SIZE);
    }
    let counts: Vec<usize> = counts.into_iter().map(|n| n as usize).collect();

    let offset = allocate_multi(metaspace, heap, &array_class, &counts);
    frame.push(Value::Reference(offset));
    Ok(())
}

/// Builds one level of a multidimensional array and, while dimensions remain, each of
/// its children — the recursion that makes `new int[2][3]` two `[I` arrays hanging off
/// one `[[I`, rather than a single flat block. Java has no true rectangular arrays:
/// every level is a real object, which is exactly why the rows can be replaced
/// individually (and why `a[0].length` need not equal `a[1].length`).
///
/// Returns the offset of the level it allocated.
fn allocate_multi(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    array_class: &str,
    counts: &[usize],
) -> usize {
    let component = &array_class[1..]; // strip one `[` → this level's element descriptor
    let count = counts[0];
    // Levels above the innermost hold *references* to their sub-arrays; only the
    // innermost level we actually build holds raw elements at their true width.
    let elem_size = if counts.len() == 1 { element_width(component) } else { SLOT_SIZE };
    let offset = allocate_array(metaspace, heap, array_class, count, elem_size);

    if counts.len() > 1 {
        for i in 0..count {
            let child = allocate_multi(metaspace, heap, component, &counts[1..]);
            let at = offset + ARRAY_HEADER_SIZE + i * SLOT_SIZE;
            // Reference store → through the barrier gateway, never a raw `write_u32`:
            // these are exactly the `old→young` pointers the remembered set must catch.
            heap.store_reference(offset, at, child);
        }
    }
    offset
}

/// The element width of a component descriptor: the faithful primitive widths (so a
/// `byte[]` row is one byte per element), and one reference slot for anything that is
/// itself an object or an array.
fn element_width(component: &str) -> usize {
    match component.as_bytes().first() {
        Some(b'Z' | b'B') => 1,
        Some(b'C' | b'S') => 2,
        Some(b'I' | b'F') => 4,
        Some(b'J' | b'D') => 8,
        _ => SLOT_SIZE, // `L…;` or `[…` — a reference, null until something is stored
    }
}

/// Lays out and `malloc`s an array of `count` elements (each `elem_size` bytes) of
/// class `array_class`, writes its header + length, and **returns its offset** — the
/// caller decides whether that reference goes on the operand stack (the one-dimensional
/// opcodes) or into a parent array's slot (`multianewarray`'s recursion). The element
/// bytes stay zeroed — `0` for primitives, `null` for references.
fn allocate_array(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    array_class: &str,
    count: usize,
    elem_size: usize,
) -> usize {
    let mirror = array_class_mirror(metaspace, heap, array_class);
    let size = ARRAY_HEADER_SIZE + count * elem_size;
    let offset = heap.malloc(size);
    heap.write_u32(offset, mirror as u32); // class_id → the array class's mirror
    heap.write_u32(offset + LENGTH_OFFSET, count as u32); // length (in elements)
    offset
}

/// Ensures the synthetic **array class** `array_class` has a `Class<…>` mirror, and
/// returns its offset. Array classes have no `.class` file and no static fields, so
/// the mirror is just a header — it exists to give the array type an identity (its
/// descriptor encodes the element kind). Idempotent, like `load_class`'s dedup.
pub fn array_class_mirror(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    array_class: &str,
) -> usize {
    let uuid = metaspace.class_id(array_class).to_string();
    if let Some(offset) = metaspace.class_object(&uuid) {
        return offset;
    }
    let offset = heap.malloc(HEADER_SIZE); // header-only: no statics
    metaspace.set_class_object(&uuid, offset);
    offset
}

/// `arraylength` (0xbe): pop an array reference, push its `length`. A null array is
/// a NullPointerException.
pub fn arraylength(heap: &HeapService, frame: &mut Frame) -> Result<(), &'static str> {
    let array = pop_array_ref(frame)?;
    let length = heap.read_u32(array + LENGTH_OFFSET);
    frame.push(Value::Int(length as i32));
    Ok(())
}

/// `iaload` (0x2e): pop an index and an array reference, push the int element.
pub fn iaload(heap: &HeapService, frame: &mut Frame) -> Result<(), &'static str> {
    let index = pop_int(frame);
    let array = pop_array_ref(frame)?;
    let raw = heap.read_u32(element_offset(heap, array, index, 4)?);
    frame.push(Value::Int(raw as i32));
    Ok(())
}

/// `iastore` (0x4f): pop value, index and array reference; write the int element.
pub fn iastore(heap: &mut HeapService, frame: &mut Frame) -> Result<(), &'static str> {
    let value = pop_int(frame);
    let index = pop_int(frame);
    let array = pop_array_ref(frame)?;
    let at = element_offset(heap, array, index, 4)?;
    heap.write_u32(at, value as u32);
    Ok(())
}

/// `laload` (0x2f): read a `long` element (8 bytes, category-2).
pub fn laload(heap: &HeapService, frame: &mut Frame) -> Result<(), &'static str> {
    let index = pop_int(frame);
    let array = pop_array_ref(frame)?;
    let raw = heap.read_u64(element_offset(heap, array, index, 8)?);
    frame.push(Value::Long(raw as i64));
    Ok(())
}

/// `lastore` (0x50): write a `long` element (8 bytes).
pub fn lastore(heap: &mut HeapService, frame: &mut Frame) -> Result<(), &'static str> {
    let value = pop_long(frame);
    let index = pop_int(frame);
    let array = pop_array_ref(frame)?;
    let at = element_offset(heap, array, index, 8)?;
    heap.write_u64(at, value as u64);
    Ok(())
}

/// `daload` (0x31): read a `double` element (8 bytes, via f64 bits).
pub fn daload(heap: &HeapService, frame: &mut Frame) -> Result<(), &'static str> {
    let index = pop_int(frame);
    let array = pop_array_ref(frame)?;
    let raw = heap.read_u64(element_offset(heap, array, index, 8)?);
    frame.push(Value::Double(f64::from_bits(raw)));
    Ok(())
}

/// `dastore` (0x52): write a `double` element (8 bytes).
pub fn dastore(heap: &mut HeapService, frame: &mut Frame) -> Result<(), &'static str> {
    let value = pop_double(frame);
    let index = pop_int(frame);
    let array = pop_array_ref(frame)?;
    let at = element_offset(heap, array, index, 8)?;
    heap.write_u64(at, value.to_bits());
    Ok(())
}

/// `faload` (0x30): read a `float` element (4 bytes, via f32 bits, category-1).
pub fn faload(heap: &HeapService, frame: &mut Frame) -> Result<(), &'static str> {
    let index = pop_int(frame);
    let array = pop_array_ref(frame)?;
    let raw = heap.read_u32(element_offset(heap, array, index, 4)?);
    frame.push(Value::Float(f32::from_bits(raw)));
    Ok(())
}

/// `fastore` (0x51): write a `float` element (4 bytes).
pub fn fastore(heap: &mut HeapService, frame: &mut Frame) -> Result<(), &'static str> {
    let value = pop_float(frame);
    let index = pop_int(frame);
    let array = pop_array_ref(frame)?;
    let at = element_offset(heap, array, index, 4)?;
    heap.write_u32(at, value.to_bits());
    Ok(())
}

/// `baload` (0x33): read a `byte`/`boolean` element (1 byte) and **sign-extend** it
/// to int — `byte` is signed, so a stored `0xFE` reads back as `-2`.
pub fn baload(heap: &HeapService, frame: &mut Frame) -> Result<(), &'static str> {
    let index = pop_int(frame);
    let array = pop_array_ref(frame)?;
    let raw = heap.read_u8(element_offset(heap, array, index, 1)?);
    frame.push(Value::Int(raw as i8 as i32));
    Ok(())
}

/// `bastore` (0x54): write the low byte of an int into a `byte`/`boolean` element.
pub fn bastore(heap: &mut HeapService, frame: &mut Frame) -> Result<(), &'static str> {
    let value = pop_int(frame);
    let index = pop_int(frame);
    let array = pop_array_ref(frame)?;
    let at = element_offset(heap, array, index, 1)?;
    heap.write_u8(at, value as u8);
    Ok(())
}

/// `caload` (0x34): read a `char` element (2 bytes) and **zero-extend** it — `char`
/// is an unsigned 16-bit value.
pub fn caload(heap: &HeapService, frame: &mut Frame) -> Result<(), &'static str> {
    let index = pop_int(frame);
    let array = pop_array_ref(frame)?;
    let raw = heap.read_u16(element_offset(heap, array, index, 2)?);
    frame.push(Value::Int(raw as i32)); // u16 → i32 zero-extends
    Ok(())
}

/// `castore` (0x55): write the low 2 bytes of an int into a `char` element.
pub fn castore(heap: &mut HeapService, frame: &mut Frame) -> Result<(), &'static str> {
    let value = pop_int(frame);
    let index = pop_int(frame);
    let array = pop_array_ref(frame)?;
    let at = element_offset(heap, array, index, 2)?;
    heap.write_u16(at, value as u16);
    Ok(())
}

/// `saload` (0x35): read a `short` element (2 bytes) and **sign-extend** it.
pub fn saload(heap: &HeapService, frame: &mut Frame) -> Result<(), &'static str> {
    let index = pop_int(frame);
    let array = pop_array_ref(frame)?;
    let raw = heap.read_u16(element_offset(heap, array, index, 2)?);
    frame.push(Value::Int(raw as i16 as i32));
    Ok(())
}

/// `sastore` (0x56): write the low 2 bytes of an int into a `short` element.
pub fn sastore(heap: &mut HeapService, frame: &mut Frame) -> Result<(), &'static str> {
    let value = pop_int(frame);
    let index = pop_int(frame);
    let array = pop_array_ref(frame)?;
    let at = element_offset(heap, array, index, 2)?;
    heap.write_u16(at, value as u16);
    Ok(())
}

/// `aaload` (0x32): pop an index and an array reference, push the *reference*
/// element. The slot holds an object offset (or 0 = null), so we push a `Reference`.
pub fn aaload(heap: &HeapService, frame: &mut Frame) -> Result<(), &'static str> {
    let index = pop_int(frame);
    let array = pop_array_ref(frame)?;
    let raw = heap.read_u32(element_offset(heap, array, index, SLOT_SIZE)?);
    frame.push(Value::Reference(raw as usize));
    Ok(())
}

/// `aastore` (0x53): pop a reference value, an index and an array reference; store
/// the reference (the target object's offset) into the slot. The stored *value* may
/// be null (a valid element); only a null *array* is a NullPointerException.
pub fn aastore(heap: &mut HeapService, frame: &mut Frame) -> Result<(), &'static str> {
    let value = pop_ref(frame);
    let index = pop_int(frame);
    let array = pop_array_ref(frame)?;
    let at = element_offset(heap, array, index, SLOT_SIZE)?;
    // Reference store → the single barrier gateway (write + remember, can't bypass).
    heap.store_reference(array, at, value as usize);
    Ok(())
}

/// The heap offset of element `index` in `array`, given the element width
/// `elem_size`. Bounds-checked against the stored length: out of range is an
/// `ArrayIndexOutOfBoundsException`.
fn element_offset(heap: &HeapService, array: usize, index: i32, elem_size: usize) -> Result<usize, &'static str> {
    let length = heap.read_u32(array + LENGTH_OFFSET) as i32;
    if index < 0 || index >= length {
        return Err(ARRAY_INDEX);
    }
    Ok(array + ARRAY_HEADER_SIZE + (index as usize) * elem_size)
}

/// Pops an array length off the stack. A negative length is a
/// `NegativeArraySizeException`.
fn pop_count(frame: &mut Frame) -> Result<usize, &'static str> {
    match frame.pop() {
        Value::Int(n) if n >= 0 => Ok(n as usize),
        Value::Int(_) => Err(NEGATIVE_SIZE),
        other => panic!("array length must be an int, found {other:?}"),
    }
}

/// Pops an `int` off the stack (an array index or element value).
fn pop_int(frame: &mut Frame) -> i32 {
    match frame.pop() {
        Value::Int(n) => n,
        other => panic!("expected an int, found {other:?}"),
    }
}

/// Pops a `long`/`float`/`double` element value off the stack (for the typed stores).
fn pop_long(frame: &mut Frame) -> i64 {
    match frame.pop() {
        Value::Long(v) => v,
        other => panic!("expected a long, found {other:?}"),
    }
}

fn pop_float(frame: &mut Frame) -> f32 {
    match frame.pop() {
        Value::Float(v) => v,
        other => panic!("expected a float, found {other:?}"),
    }
}

fn pop_double(frame: &mut Frame) -> f64 {
    match frame.pop() {
        Value::Double(v) => v,
        other => panic!("expected a double, found {other:?}"),
    }
}

/// Pops a reference off the stack as a heap offset (used for the *value* of an
/// `aastore`, where null is a legitimate thing to store).
fn pop_ref(frame: &mut Frame) -> usize {
    match frame.pop() {
        Value::Reference(offset) => offset,
        other => panic!("expected a reference, found {other:?}"),
    }
}

/// Pops the *array* reference an access operates on; a null array (offset 0) is a
/// `NullPointerException`.
fn pop_array_ref(frame: &mut Frame) -> Result<usize, &'static str> {
    match frame.pop() {
        Value::Reference(0) => Err(NULL_POINTER),
        Value::Reference(offset) => Ok(offset),
        other => panic!("expected an array reference, found {other:?}"),
    }
}
