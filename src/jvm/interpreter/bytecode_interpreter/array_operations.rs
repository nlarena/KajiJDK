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

/// The `length` word sits right after the object header. **Public** because the JIT emits the
/// same load: `burst::compile`'s `arraylength` and `iaload` read the length from here, and the two
/// must agree to the byte or compiled code reads someone else's word.
pub const LENGTH_OFFSET: usize = HEADER_SIZE;
/// An array's header is the object header plus the `length` word; elements follow. Public for the
/// same reason as [`LENGTH_OFFSET`].
pub const ARRAY_HEADER_SIZE: usize = HEADER_SIZE + 4;

/// The width of one element of the array class `array_class` (e.g. `"[I"` → 4). Public so the JIT
/// can be told the stride of an `int[]` rather than assuming it — the single source of truth is
/// [`element_width`], and this is the door to it.
pub fn array_element_width(array_class: &str) -> usize {
    element_width(&array_class[1..])
}

/// Allocates an array of `count` elements of the class `array_class` (`"[I"`,
/// `"[Ljava/lang/String;"`) — the reflective counterpart of `newarray`/`anewarray`.
///
/// It exists for `java.lang.reflect.Array.newArray`, which is the only way to build an array
/// whose element type is known solely at run time. `Collection.toArray(T[])` needs exactly that:
/// the caller hands in a `String[0]` precisely so it gets a `String[]` back, and there is no
/// bytecode that allocates "an array of whatever class this mirror names".
///
/// Same `Err(OUT_OF_MEMORY)` contract as the opcodes: exhaustion is a throwable condition, not a
/// VM panic.
pub fn allocate_array_of_class(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    array_class: &str,
    count: usize,
) -> Result<usize, &'static str> {
    let elem_size = array_element_width(array_class);
    allocate_array(metaspace, heap, array_class, count, elem_size)
}

// The implicit exceptions array opcodes can raise; returned as `Err` for the
// dispatch loop to throw (`throw_exception`).
const NULL_POINTER: &str = "java/lang/NullPointerException";
const ARRAY_INDEX: &str = "java/lang/ArrayIndexOutOfBoundsException";
const NEGATIVE_SIZE: &str = "java/lang/NegativeArraySizeException";
/// Thrown when the heap can't fit the array (JVMS §6.3) — exhaustion is recoverable
/// for the *bytecode* allocation opcodes, which surface it via `throw_exception`.
const OUT_OF_MEMORY: &str = "java/lang/OutOfMemoryError";
const ARRAY_STORE: &str = "java/lang/ArrayStoreException";

/// `newarray` (0xbc): allocate a **primitive** array. `atype` names the element
/// type; we model the int-category primitives, each with its faithful element width
/// (so a `byte[10]` is 10 bytes, not 40). The array's class is its descriptor.
pub fn newarray(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    frame: &mut Frame,
    atype: u8,
) -> Result<(), &'static str> {
    let (array_class, elem_size) = primitive_array_class(atype)
        .unwrap_or_else(|| panic!("newarray: unknown primitive atype {atype}"));
    let count = pop_count(frame)?; // negative length → NegativeArraySizeException
    let offset = allocate_array(metaspace, heap, array_class, count, elem_size)?; // full heap → OOM
    frame.push(Value::Reference(offset));
    Ok(())
}

/// `anewarray` (0xbd): allocate a **reference** array. `cp_index` names the *element*
/// class; the array's class is `"[L<element>;"` — unless the element is itself an array,
/// whose name is already a descriptor: then it's just `"[" + element` (`new long[n][]`
/// names element `[J` → array class `[[J`, not `[L[J;`), matching the descriptors the
/// constant pool and `checkcast`/`instanceof` use for nested arrays.
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
    let array_class = reference_array_class(&element);
    let count = pop_count(frame)?; // negative length → NegativeArraySizeException
    // A reference element is one heap offset wide.
    let offset = allocate_array(metaspace, heap, &array_class, count, SLOT_SIZE)?; // full heap → OOM
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

    let offset = allocate_multi(metaspace, heap, &array_class, &counts)?; // full heap → OOM
    frame.push(Value::Reference(offset));
    Ok(())
}

/// Builds one level of a multidimensional array and, while dimensions remain, each of
/// its children — the recursion that makes `new int[2][3]` two `[I` arrays hanging off
/// one `[[I`, rather than a single flat block. Java has no true rectangular arrays:
/// every level is a real object, which is exactly why the rows can be replaced
/// individually (and why `a[0].length` need not equal `a[1].length`).
///
/// Returns the offset of the level it allocated. `Err(OUT_OF_MEMORY)` if the heap
/// can't fit a level; a partially-built outer array is simply abandoned (never
/// pushed → unreachable → garbage for the next collection).
pub fn allocate_multi(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    array_class: &str,
    counts: &[usize],
) -> Result<usize, &'static str> {
    let component = &array_class[1..]; // strip one `[` → this level's element descriptor
    let count = counts[0];
    // Levels above the innermost hold *references* to their sub-arrays; only the
    // innermost level we actually build holds raw elements at their true width.
    let elem_size = if counts.len() == 1 { element_width(component) } else { SLOT_SIZE };
    let offset = allocate_array(metaspace, heap, array_class, count, elem_size)?;

    if counts.len() > 1 {
        for i in 0..count {
            let child = allocate_multi(metaspace, heap, component, &counts[1..])?;
            let at = offset + ARRAY_HEADER_SIZE + i * SLOT_SIZE;
            // Reference store → through the barrier gateway, never a raw `write_u32`:
            // these are exactly the `old→young` pointers the remembered set must catch.
            heap.store_reference(offset, at, child);
        }
    }
    Ok(offset)
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
///
/// `Err(OUT_OF_MEMORY)` when the heap is exhausted (`try_malloc` fails): the array
/// opcodes are the *bytecode* allocation sites, where exhaustion must surface as a
/// catchable `java.lang.OutOfMemoryError` rather than a VM panic. (The mirror
/// allocation stays on the panicking path — it's a fixed 8-byte header.)
fn allocate_array(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    array_class: &str,
    count: usize,
    elem_size: usize,
) -> Result<usize, &'static str> {
    let mirror = array_class_mirror(metaspace, heap, array_class);
    let size = ARRAY_HEADER_SIZE + count * elem_size;
    let offset = heap.try_malloc(size).ok_or(OUT_OF_MEMORY)?;
    heap.write_u32(offset, mirror as u32); // class_id → the array class's mirror
    heap.write_u32(offset + LENGTH_OFFSET, count as u32); // length (in elements)
    Ok(offset)
}

/// Allocates a `java.lang.Object[]` holding `elements` (all references) — the VM uses this to
/// hand a bootstrap method its `Object... args`. Primitive values would need boxing first; the
/// current callers (condy static arguments) pass references only.
///
/// Allocated in **Old** (`malloc_old`): the caller holds the element references in a Rust `Vec`
/// it can't root, so a minor GC here would leave them stale — Old allocation triggers none and
/// Old objects don't move. Elements go in through the write barrier so the Old→young pointers are
/// remembered and a later minor GC updates them.
pub fn build_object_array(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    elements: &[Value],
) -> usize {
    let mirror = array_class_mirror(metaspace, heap, "[Ljava/lang/Object;");
    let offset = heap.malloc_old(ARRAY_HEADER_SIZE + elements.len() * SLOT_SIZE);
    heap.write_u32(offset, mirror as u32);
    heap.write_u32(offset + LENGTH_OFFSET, elements.len() as u32);
    for (i, value) in elements.iter().enumerate() {
        let reference = match value {
            Value::Reference(r) => *r,
            other => panic!("build_object_array: only references are supported, got {other:?}"),
        };
        heap.store_reference(offset, offset + ARRAY_HEADER_SIZE + i * SLOT_SIZE, reference);
    }
    offset
}

/// `array.clone()`'s copy step (JLS §10.7: every array is Cloneable, so there is no opt-in
/// check): allocates a new array of `source`'s runtime array class and length, and copies the
/// elements verbatim — shallow, like the instance copy (a reference array copies references).
///
/// Old-allocated (`malloc_old`, see [`build_object_array`]): `source` is held in a Rust local
/// across the allocation, and an Eden allocation could trigger a minor GC that moves it.
/// Reference elements go in through the write barrier so the clone's Old→young pointers are
/// remembered; primitive payloads copy byte-for-byte (elements have their faithful widths —
/// a `byte[]` row is one byte per element).
pub fn clone_array(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    source: usize,
) -> usize {
    let array_class = metaspace
        .class_name_at_mirror(heap.read_u32(source) as usize)
        .expect("clone_array: receiver is not an array")
        .to_string();
    let length = heap.read_u32(source + LENGTH_OFFSET) as usize;
    let component = &array_class[1..];
    let elem_size = element_width(component);

    let mirror = array_class_mirror(metaspace, heap, &array_class);
    let offset = heap.malloc_old(ARRAY_HEADER_SIZE + length * elem_size);
    heap.write_u32(offset, mirror as u32);
    heap.write_u32(offset + LENGTH_OFFSET, length as u32);

    if matches!(component.as_bytes().first(), Some(b'L') | Some(b'[')) {
        // Reference elements: each pointer through the barrier gateway.
        for i in 0..length {
            let at = ARRAY_HEADER_SIZE + i * SLOT_SIZE;
            let value = heap.read_u32(source + at) as usize;
            heap.store_reference(offset, offset + at, value);
        }
    } else {
        // Primitive payload: a raw byte-for-byte copy of `length * elem_size` bytes.
        for b in 0..length * elem_size {
            let value = heap.read_u8(source + ARRAY_HEADER_SIZE + b);
            heap.write_u8(offset + ARRAY_HEADER_SIZE + b, value);
        }
    }
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
    // Header-only (no statics), and **Old-pinned like every Class mirror** (see the class and
    // primitive mirrors): the metaspace's mirror index and every array header's `class_id` hold
    // this offset as a plain number the GC doesn't rewrite — an Eden mirror would move (or die,
    // if only garbage pointed at it) on the first minor GC and leave them dangling at a slot the
    // collector then hands to a different mirror.
    let offset = heap.malloc_old(HEADER_SIZE);
    metaspace.set_class_object(&uuid, offset);
    // Y el header **se escribe**: un mirror es el mismo una instancia de `java.lang.Class`, asi que
    // su `class_id` tiene que apuntar al mirror de `Class`. El de una clase normal lo hacia; el de
    // un array quedaba en 0, o sea un objeto sin clase. Se notaba recien al **operar sobre el
    // mirror** —`array.getClass().getName()`, o un `checkcast` a `Class`—, que terminaba en "could
    // not resolve the object's class from its header". Que `getClass()` devolviera algo no-nulo
    // tapaba el hueco: lo que devolvia era un objeto a medio construir (#262).
    super::class_operations::load_class(metaspace, heap, "java/lang/Class");
    let class_uuid = metaspace.class_id("java/lang/Class").to_string();
    let class_mirror = metaspace.class_object(&class_uuid).unwrap_or(0);
    heap.write_u32(offset, class_mirror as u32);
    offset
}

/// The array class a `newarray`'s `atype` operand names, and the width of one of its elements —
/// JVMS §6.5's table, which is the same one [`newarray`] switches on. Public so the JIT reads the
/// *same* table rather than a copy of it: an `atype` the two decode differently is an array whose
/// header says one type and whose elements are laid out for another.
pub fn primitive_array_class(atype: u8) -> Option<(&'static str, usize)> {
    Some(match atype {
        4 => ("[Z", 1),
        5 => ("[C", 2),
        6 => ("[F", 4),
        7 => ("[D", 8),
        8 => ("[B", 1),
        9 => ("[S", 2),
        10 => ("[I", 4),
        11 => ("[J", 8),
        _ => return None,
    })
}

/// The array class a `anewarray`'s **element** class name implies — `Dog` → `[LDog;`, but `[J` →
/// `[[J`, since an element that is itself an array already carries a descriptor. The same two lines
/// [`anewarray`] runs, extracted so the JIT cannot drift from it.
pub fn reference_array_class(element: &str) -> String {
    match element.starts_with('[') {
        true => format!("[{element}"),
        false => format!("[L{element};"),
    }
}

/// What the JIT bakes into a compiled `newarray`/`anewarray`: the `(class_id, element_width)` of
/// the array class `array_class` — see `burst::compile::ArrayType`.
///
/// **Read-only, and that is the whole design constraint**, exactly as for
/// [`jit_instance`][super::objects_operations::jit_instance]: it takes `&MetaspaceService`, so it
/// cannot mint a Class ID and — crucially — cannot allocate the mirror. [`array_class_mirror`] does
/// both, and a compilation that allocates is a compilation that can collect, which would break the
/// one invariant the whole tier stands on.
///
/// So `None` — "do not compile this method" — unless **both** hold:
///
///  1. the array class already has a Class ID minted, and
///  2. its `Class<…>` mirror already exists, at an offset that fits the `u32` the header word is.
///
/// Both become true the first time the *interpreter* allocates an array of this class, which for a
/// method hot enough to be offered to this tier has almost always already happened. There is no
/// initialisation state to check, unlike `new`: an array class has no `<clinit>` and no statics.
pub fn jit_array_class(metaspace: &MetaspaceService, array_class: &str) -> Option<(u32, u32)> {
    let uuid = metaspace.class_id_read(array_class)?;
    let class_id = u32::try_from(metaspace.class_object(uuid)?).ok()?;
    Some((class_id, array_element_width(array_class) as u32))
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
///
/// Because arrays are **covariant** (`Dog[] <: Animal[]`), the static types can't
/// guarantee the store is sound — JVMS §6.5 requires the *dynamic* check: a non-null
/// value whose runtime class is not assignable to the array's element type is an
/// `ArrayStoreException` (returned as `Err` for the dispatch loop to throw).
pub fn aastore(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    frame: &mut Frame,
) -> Result<(), &'static str> {
    let value = pop_ref(frame);
    let index = pop_int(frame);
    let array = pop_array_ref(frame)?;
    let at = element_offset(heap, array, index, SLOT_SIZE)?;
    // null always stores fine; a real reference must be assignable to the element type.
    if value != 0 {
        let array_class = metaspace
            .class_name_at_mirror(heap.read_u32(array) as usize)
            .expect("aastore: array header does not point at a known class")
            .to_string();
        let value_class = metaspace
            .class_name_at_mirror(heap.read_u32(value) as usize)
            .expect("aastore: value header does not point at a known class")
            .to_string();
        // The element type is the array descriptor minus one `[`: `L<name>;` → the
        // class name, a nested `[…` → itself. (A primitive component can't reach
        // aastore in verified code — treat it as unassignable if it ever does.)
        let component = &array_class[1..];
        let element = match component.strip_prefix('L') {
            Some(name) => name.trim_end_matches(';'),
            None if component.starts_with('[') => component,
            None => return Err(ARRAY_STORE),
        };
        if !super::class_operations::is_subtype(metaspace, &value_class, element) {
            return Err(ARRAY_STORE);
        }
    }
    // Reference store → the single barrier gateway (write + remember, can't bypass).
    heap.store_reference(array, at, value);
    Ok(())
}

// ---- Read-only twins for the H3 W3 parallel `.read()` path ---------------------------------
// Array *loads* already read the heap through `&HeapService`, so they can run under a shared read
// lock. The catch is escalation: on a null array or an out-of-range index the op would throw, and
// the throw needs the write path — so these restore the operand stack and return `None` instead of
// mutating it, letting the driver re-run the op under `.write()` cleanly.

/// Read-only `arraylength`. `Some(())` = pushed the length; `None` = null array → escalate
/// (stack restored so the write path throws NPE).
pub fn arraylength_read(heap: &HeapService, frame: &mut Frame) -> Option<()> {
    let array_v = frame.pop();
    match array_v {
        Value::Reference(0) => {
            frame.push(array_v); // null → restore + escalate
            None
        }
        Value::Reference(off) => {
            frame.push(Value::Int(heap.read_u32(off + LENGTH_OFFSET) as i32));
            Some(())
        }
        _ => {
            frame.push(array_v);
            None
        }
    }
}

/// Read-only array element load for the W3 read path (shared by `iaload`..`saload`). Pops the
/// index and array ref; on a null array or an out-of-range index it **restores the stack** and
/// returns `None` (escalate — the write path throws); otherwise reads the element via `read`.
pub fn array_load_read(
    heap: &HeapService,
    frame: &mut Frame,
    elem_size: usize,
    read: fn(&HeapService, usize) -> Value,
) -> Option<()> {
    let index_v = frame.pop();
    let array_v = frame.pop();
    let (index, array) = match (&index_v, &array_v) {
        (Value::Int(i), Value::Reference(a)) if *a != 0 => (*i, *a),
        _ => {
            frame.push(array_v); // restore (array below, index on top) and escalate
            frame.push(index_v);
            return None;
        }
    };
    let length = heap.read_u32(array + LENGTH_OFFSET) as i32;
    if index < 0 || index >= length {
        frame.push(array_v); // out of bounds → restore + escalate (write path throws AIOOBE)
        frame.push(index_v);
        return None;
    }
    frame.push(read(heap, array + ARRAY_HEADER_SIZE + (index as usize) * elem_size));
    Some(())
}

/// Lock-free array allocation for the W2c `.read()` path (shared by `newarray`/`anewarray`).
/// `None` = escalate (the array class's mirror isn't prepared yet, or Eden is full).
fn allocate_array_read(
    metaspace: &MetaspaceService,
    heap: &HeapService,
    array_class: &str,
    count: usize,
    elem_size: usize,
    idx: usize,
) -> Option<usize> {
    let uuid = metaspace.class_id_read(array_class)?; // array class not registered → escalate
    let mirror = metaspace.class_object(uuid)?; // mirror not created yet → escalate
    let size = ARRAY_HEADER_SIZE + count * elem_size;
    heap.alloc_array_lockfree(size, mirror as u32, count as u32, idx)
}

/// Read-only `newarray` (primitive array). `Some(())` = allocated + pushed; `None` = escalate
/// (negative length, unknown atype, unprepared class, or Eden full) with the stack restored.
pub fn newarray_read(
    metaspace: &MetaspaceService,
    heap: &HeapService,
    frame: &mut Frame,
    atype: u8,
    idx: usize,
) -> Option<()> {
    // The same table the write path and the JIT read — see [`primitive_array_class`]. An unknown
    // atype escalates here rather than panicking, which is what `run_one` does on this path.
    let (array_class, elem_size) = primitive_array_class(atype)?;
    let n = match frame.pop() {
        Value::Int(n) => n,
        other => {
            frame.push(other);
            return None;
        }
    };
    if n < 0 {
        frame.push(Value::Int(n)); // negative → escalate (write path throws NegativeArraySize)
        return None;
    }
    match allocate_array_read(metaspace, heap, array_class, n as usize, elem_size, idx) {
        Some(off) => {
            frame.push(Value::Reference(off));
            Some(())
        }
        None => {
            frame.push(Value::Int(n)); // restore + escalate
            None
        }
    }
}

/// Read-only `anewarray` (reference array). Same escalation rules as [`newarray_read`].
pub fn anewarray_read(
    metaspace: &MetaspaceService,
    heap: &HeapService,
    frame: &mut Frame,
    cp_index: u16,
    idx: usize,
) -> Option<()> {
    let array_class = {
        let caller = metaspace.class_of(frame.method());
        let element = metaspace.get(caller)?.class_name(cp_index)?;
        // Same naming as the write path and as the JIT — see [`reference_array_class`].
        reference_array_class(element)
    };
    let n = match frame.pop() {
        Value::Int(n) => n,
        other => {
            frame.push(other);
            return None;
        }
    };
    if n < 0 {
        frame.push(Value::Int(n));
        return None;
    }
    match allocate_array_read(metaspace, heap, &array_class, n as usize, SLOT_SIZE, idx) {
        Some(off) => {
            frame.push(Value::Reference(off));
            Some(())
        }
        None => {
            frame.push(Value::Int(n));
            None
        }
    }
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
