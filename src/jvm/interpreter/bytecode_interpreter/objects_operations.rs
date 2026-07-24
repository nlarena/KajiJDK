//! Generic object operations — laying out a class's fields in memory and
//! allocating a fresh object on the heap for them.
//!
//! This is the **generic builder**: given a class, it computes the object's field
//! layout (size + per-field slot) and `malloc`s a brand-new object with its header.
//! Instance creation (`new`) uses it directly — every `new Point()` *is* a distinct
//! object, so allocating a fresh one each time is exactly right.
//!
//! Loading a `Class<…>` is the **special** case and does *not* live here: a class's
//! mirror must be unique (one per program), so allocating it must be deduplicated.
//! That guard lives in [`super::class_operations::load_class`], which consults the
//! metaspace's class-object index and only calls into this builder on first load.

use crate::jvm::interpreter::frame::{Frame, Value};
use crate::jvm::interpreter::heap::HeapService;
use crate::jvm::interpreter::metaspace::MetaspaceService;
use std::sync::atomic::Ordering;

/// Bytes for one field slot — 4 bytes. Most types take one slot; the **category-2**
/// types (`long`/`double`) take two (8 bytes) — see [`field_slots`].
pub const SLOT_SIZE: usize = 4;

/// How many 4-byte slots a field of this `descriptor` occupies: two for the
/// category-2 types (`long` = `J`, `double` = `D`), one for everything else. This is
/// what makes the object layout width-aware — a `long` field shifts the ones after it.
pub fn field_slots(descriptor: &str) -> usize {
    match descriptor.as_bytes().first() {
        Some(b'J') | Some(b'D') => 2,
        _ => 1,
    }
}

/// The object header size, in bytes: `[class_id: u32 | mark: u32]`. Every heap
/// object (instance or `Class<…>` mirror) starts with it, which also guarantees a
/// non-zero footprint, so distinct objects get distinct offsets.
pub const HEADER_SIZE: usize = 8;

/// The exception a field access raises on a null receiver. Field ops return this as
/// `Err` and the dispatch loop turns it into a thrown object (`throw_exception`).
const NULL_POINTER: &str = "java/lang/NullPointerException";

/// Allocates a fresh, zero-initialised instance of `name` on the heap and returns
/// its offset (the object reference). Lays out `[header | inherited fields | own
/// fields]`: the size counts the instance fields of `name` **and every
/// superclass** (inheritance — a `Dog` carries `Animal`'s fields too), each one
/// slot wide. `malloc` zeroes the fields (their default values); the header's
/// `class_id` is filled with the class's `Class<…>` mirror offset, so the object
/// knows what it is.
pub fn allocate(metaspace: &mut MetaspaceService, heap: &mut HeapService, name: &str) -> usize {
    let slots = instance_field_slots(metaspace, name);
    let size = HEADER_SIZE + slots * SLOT_SIZE;
    let offset = heap.malloc(size);

    // Header: point `class_id` at the class's mirror (its `Class<…>` offset), so an
    // object resolves back to its class. The mirror index is keyed by Class ID, so
    // resolve the name to its id first. NOTE: a heap offset isn't GC-stable (A5
    // will move objects); the class's durable identity is its UUID in the
    // metaspace. We store the offset here for now because it's concrete and lets
    // the visualizer show the link. The `mark` word stays 0 (already zeroed).
    let uuid = metaspace.class_id(name).to_string();
    let class_id = metaspace.class_object(&uuid).unwrap_or(0) as u32;
    heap.write_u32(offset, class_id);

    offset
}

/// `putfield` (0xb5): pop a value and an object reference, and write the value into
/// the object's field on the heap. The field is named by `cp_index` (a `FieldRef`
/// in the current method's class); its byte offset inside the object comes from the
/// layout via [`field_offset`].
pub fn putfield(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    frame: &mut Frame,
    cp_index: u16,
) -> Result<(), &'static str> {
    let caller = metaspace.class_of(frame.method()).to_string();
    let (declaring, field) = {
        let cf = metaspace.get(&caller).expect("caller class is loaded");
        let (c, n, _d) = cf.fieldref_target(cp_index).expect("putfield: bad FieldRef");
        (c.to_string(), n.to_string())
    };
    let field_off = field_offset(metaspace, &declaring, &field);

    // Stack shape is [objectref, value]: pop the value first, then the receiver. The
    // *value's* type tells us the width to write — a `long` is 8 bytes, an int or
    // reference 4 — so we don't need to re-read the descriptor here.
    let value = frame.pop();
    let object = match frame.pop() {
        Value::Reference(0) => return Err(NULL_POINTER), // null receiver → NPE
        Value::Reference(offset) => offset,
        _ => panic!("putfield: expected an object reference under the value"),
    };
    match value {
        Value::Long(v) => heap.write_u64(object + field_off, v as u64),
        Value::Double(v) => heap.write_u64(object + field_off, v.to_bits()),
        Value::Float(v) => heap.write_u32(object + field_off, v.to_bits()),
        Value::Int(v) => heap.write_u32(object + field_off, v as u32),
        // Reference store → the single barrier gateway (write + remember, can't bypass).
        Value::Reference(r) => heap.store_reference(object, object + field_off, r),
    }
    Ok(())
}

/// `getfield` (0xb4): pop an object reference and push the value of one of its
/// fields, read from the heap. Mirror of [`putfield`]. The field descriptor decides
/// how to read the bytes back — a reference field yields a `Reference`, anything
/// else an `Int`.
pub fn getfield(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    frame: &mut Frame,
    cp_index: u16,
) -> Result<(), &'static str> {
    let caller = metaspace.class_of(frame.method()).to_string();
    let (named, field, descriptor) = {
        let cf = metaspace.get(&caller).expect("caller class is loaded");
        let (c, n, d) = cf.fieldref_target(cp_index).expect("getfield: bad FieldRef");
        (c.to_string(), n.to_string(), d.to_string())
    };
    let field_off = field_offset(metaspace, &named, &field);

    let object = match frame.pop() {
        Value::Reference(0) => return Err(NULL_POINTER), // null receiver → NPE
        Value::Reference(offset) => offset,
        _ => panic!("getfield: expected an object reference"),
    };
    // The descriptor decides the width to read: a `long` is 8 bytes (category-2), a
    // reference or int 4. (`double` would also be 8, once it's in the value model.)
    let value = match descriptor.as_bytes().first() {
        Some(b'J') => Value::Long(heap.read_u64(object + field_off) as i64),
        Some(b'D') => Value::Double(f64::from_bits(heap.read_u64(object + field_off))),
        Some(b'F') => Value::Float(f32::from_bits(heap.read_u32(object + field_off))),
        Some(b'L') | Some(b'[') => Value::Reference(heap.read_u32(object + field_off) as usize),
        _ => Value::Int(heap.read_u32(object + field_off) as i32),
    };
    frame.push(value);
    Ok(())
}

/// Places a field of `descriptor` at the running slot count, returning `(start_slot,
/// next_count)`. Category-2 fields (`long`/`double`) are **8-byte aligned**: their start
/// slot is rounded up to even — the offset `HEADER_SIZE + even*4` is then 8-aligned —
/// inserting one padding slot when the running count is odd. `AtomicU64` (the H4 memory
/// model) needs that 8-alignment to read/write a `long`/`double` field atomically without
/// tearing; category-1 fields are unaffected. This is the **single** placement rule — every
/// layout consumer (offset, size, and the GC's reference walk) folds over it, so they can't
/// disagree about where a field lands.
pub fn place_field(slots: usize, descriptor: &str) -> (usize, usize) {
    let width = field_slots(descriptor);
    let start = if width == 2 { (slots + 1) & !1 } else { slots };
    (start, start + width)
}

/// The super-first list of `(descriptor, name)` non-static instance fields of `class` and
/// every loadable superclass — the object's field layout order (a `Dog` carries `Animal`'s
/// fields first, then its own in declaration order). Write variant: loads classes on demand.
fn layout_fields_mut(metaspace: &mut MetaspaceService, class: &str) -> Vec<(String, String)> {
    let mut chain: Vec<Vec<(String, String)>> = Vec::new();
    let mut current = Some(class.to_string());
    while let Some(name) = current.take() {
        match metaspace.get_or_load(&name) {
            Some(cf) => {
                current = cf.class_name(cf.super_class).map(|s| s.to_string());
                chain.push(own_instance_fields(cf));
            }
            None => break, // reached Object (off classpath) — chain ends
        }
    }
    chain.into_iter().rev().flatten().collect()
}

/// Read-only twin of [`layout_fields_mut`] (uses `get`; stops at the first unloaded super).
fn layout_fields_ref(metaspace: &MetaspaceService, class: &str) -> Vec<(String, String)> {
    let mut chain: Vec<Vec<(String, String)>> = Vec::new();
    let mut current = Some(class.to_string());
    while let Some(name) = current.take() {
        let Some(cf) = metaspace.get(&name) else { break };
        current = cf.class_name(cf.super_class).map(|s| s.to_string());
        chain.push(own_instance_fields(cf));
    }
    chain.into_iter().rev().flatten().collect()
}

/// A class's own non-static fields as `(descriptor, name)`, in declaration order.
fn own_instance_fields(cf: &crate::jvm::class_file::ClassFile) -> Vec<(String, String)> {
    cf.fields
        .iter()
        .filter(|f| !f.is_static())
        .map(|f| {
            (
                cf.utf8(f.descriptor_index).unwrap_or("").to_string(),
                cf.utf8(f.name_index).unwrap_or("").to_string(),
            )
        })
        .collect()
}

/// The byte offset of `field` within an object laid out as `fields` (super-first). Folds the
/// single [`place_field`] rule, so padding is accounted for. Returns the **last** matching
/// occurrence: a subclass field shadows a superclass one of the same name, and in super-first
/// order the subclass's comes last — matching Java's resolution (nearest declaring class).
/// `None` if no field of that name is present.
fn field_offset_in(fields: &[(String, String)], field: &str) -> Option<usize> {
    let mut slots = 0;
    let mut found = None;
    for (descriptor, name) in fields {
        let (start, next) = place_field(slots, descriptor);
        if name == field {
            found = Some(HEADER_SIZE + start * SLOT_SIZE);
        }
        slots = next;
    }
    found
}

/// Total instance-field slots of a super-first field list, padding included.
fn total_slots(fields: &[(String, String)]) -> usize {
    let mut slots = 0;
    for (descriptor, _) in fields {
        slots = place_field(slots, descriptor).1;
    }
    slots
}

/// The byte offset of field `field` *within an object* of class `named_class`. `named_class`
/// is the class named in the `FieldRef`, which may only *inherit* the field; the layout puts
/// superclass fields first, so folding [`place_field`] over the super-first field list lands
/// each field (and its 8-alignment padding) exactly where the object stores it.
pub fn field_offset(metaspace: &mut MetaspaceService, named_class: &str, field: &str) -> usize {
    field_offset_in(&layout_fields_mut(metaspace, named_class), field)
        .expect("field_offset: field not found in the class or its superclasses")
}

/// The total instance-field **slots** of `name` plus those of every superclass — the object's
/// size in slots (inherited fields included). Folds the super-first layout with [`place_field`],
/// so it counts the same 8-alignment padding the field offsets use (a per-class sum would miss
/// padding that straddles a superclass boundary — parity is order-sensitive).
fn instance_field_slots(metaspace: &mut MetaspaceService, name: &str) -> usize {
    total_slots(&layout_fields_mut(metaspace, name))
}

// ---- Read-only twins for the H3 W3 parallel `.read()` path ----------------------------------
// These read a field with only `&MetaspaceService`/`&HeapService`, so many threads can run
// `getfield` concurrently under a shared read lock (heap reads are already `&self`). They use
// `get` (never load a class): at runtime an instantiated object's classes are always loaded, so
// the read path succeeds; if a class on the path somehow isn't loaded they return `None` and the
// driver **escalates** to the write path (which resolves it).

/// Read-only `getfield`. `Some(())` = read the field and pushed it (ran concurrently); `None` =
/// escalate to the write path (null receiver or an unloaded class). On escalation the operand
/// stack is left exactly as it was (the popped receiver is pushed back).
pub fn getfield_read(
    metaspace: &MetaspaceService,
    heap: &HeapService,
    frame: &mut Frame,
    cp_index: u16,
) -> Option<()> {
    let caller = metaspace.class_of(frame.method()).to_string();
    let cf = metaspace.get(&caller)?;
    let (named, field, descriptor) = {
        let (c, n, d) = cf.fieldref_target(cp_index)?;
        (c.to_string(), n.to_string(), d.to_string())
    };
    let field_off = field_offset_read(metaspace, &named, &field)?;
    let receiver = frame.pop();
    let object = match receiver {
        Value::Reference(0) => {
            frame.push(receiver); // null → restore stack and escalate (write path throws NPE)
            return None;
        }
        Value::Reference(offset) => offset,
        other => {
            frame.push(other);
            return None;
        }
    };
    // A `volatile` field reads with `Acquire`, so it sees everything published by the matching
    // `Release` write (H4). For an Eden object that's a real atomic acquire; for an Old object the
    // enclosing `.read()` lock already orders it. Non-volatile fields stay `Relaxed` (plain read).
    let volatile = field_is_volatile_read(metaspace, &named, &field);
    let at = object + field_off;
    let value = match descriptor.as_bytes().first() {
        Some(b'J') => Value::Long(read_u64_field(heap, at, volatile) as i64),
        Some(b'D') => Value::Double(f64::from_bits(read_u64_field(heap, at, volatile))),
        Some(b'F') => Value::Float(f32::from_bits(read_u32_field(heap, at, volatile))),
        Some(b'L') | Some(b'[') => Value::Reference(read_u32_field(heap, at, volatile) as usize),
        _ => Value::Int(read_u32_field(heap, at, volatile) as i32),
    };
    frame.push(value);
    Some(())
}

/// Reads a 4-byte field with the ordering its `volatile`-ness implies: `Acquire` if volatile,
/// plain `Relaxed` otherwise.
fn read_u32_field(heap: &HeapService, at: usize, volatile: bool) -> u32 {
    if volatile {
        heap.read_u32_acquire(at)
    } else {
        heap.read_u32(at)
    }
}

/// Reads an 8-byte (`long`/`double`) field — `Acquire` if volatile, plain otherwise.
fn read_u64_field(heap: &HeapService, at: usize, volatile: bool) -> u64 {
    if volatile {
        heap.read_u64_acquire(at)
    } else {
        heap.read_u64(at)
    }
}

/// Whether `field` — resolved from `named_class` up the superclass chain — is declared
/// `volatile`. Read-only (uses `get`, walking to the nearest declaring class, matching how
/// [`field_offset_in`] resolves a shadowed field). `false` if not found or a class isn't loaded;
/// that only ever *under*-orders an access the write path will redo, so it's safe.
fn field_is_volatile_read(metaspace: &MetaspaceService, named_class: &str, field: &str) -> bool {
    let mut current = Some(named_class.to_string());
    while let Some(name) = current.take() {
        let Some(cf) = metaspace.get(&name) else { break };
        for f in cf.fields.iter().filter(|f| !f.is_static()) {
            if cf.utf8(f.name_index) == Some(field) {
                return f.is_volatile();
            }
        }
        current = cf.class_name(cf.super_class).map(|s| s.to_string());
    }
    false
}

/// Read-path (lock-free) `putfield` for a **primitive** field of an **Eden** object: writes the
/// value atomically without the VM write lock — `Relaxed`, or `Release` if the field is
/// `volatile`. `Some(())` = wrote it (ran concurrently); `None` = **escalate** to the locked write
/// path — for a *reference* store (which needs the GC write barrier), an *Old* object, an unloaded
/// class, or a null receiver. On escalation the operand stack is restored to `[object, value]`.
pub fn putfield_read(
    metaspace: &MetaspaceService,
    heap: &HeapService,
    frame: &mut Frame,
    cp_index: u16,
) -> Option<()> {
    let caller = metaspace.class_of(frame.method()).to_string();
    let (declaring, field) = {
        let cf = metaspace.get(&caller)?;
        let (c, n, _d) = cf.fieldref_target(cp_index)?;
        (c.to_string(), n.to_string())
    };
    let field_off = field_offset_read(metaspace, &declaring, &field)?;

    // Stack is [object, value]: pop the value, then the receiver — restoring both on escalation.
    let value = frame.pop();
    let object = match frame.pop() {
        Value::Reference(0) => {
            frame.push(Value::Reference(0)); // null → restore and escalate (write path throws NPE)
            frame.push(value);
            return None;
        }
        Value::Reference(off) => off,
        other => {
            frame.push(other);
            frame.push(value);
            return None;
        }
    };

    let volatile = field_is_volatile_read(metaspace, &declaring, &field);
    let order = if volatile { Ordering::Release } else { Ordering::Relaxed };
    let at = object + field_off;
    let wrote = match value {
        // Reference store → the GC write barrier isn't lock-free yet → escalate.
        Value::Reference(_) => false,
        Value::Long(v) => heap.write_u64_eden(at, v as u64, order),
        Value::Double(v) => heap.write_u64_eden(at, v.to_bits(), order),
        Value::Float(v) => heap.write_u32_eden(at, v.to_bits(), order),
        Value::Int(v) => heap.write_u32_eden(at, v as u32, order),
    };
    if wrote {
        Some(())
    } else {
        // Old object or reference store → restore [object, value] and escalate to the write path.
        frame.push(Value::Reference(object));
        frame.push(value);
        None
    }
}

/// Read-only twin of [`field_offset`] — `None` if the field's declaring class isn't loaded
/// (its fields never appear in the partial layout list, so the fold finds no match → escalate).
fn field_offset_read(metaspace: &MetaspaceService, named_class: &str, field: &str) -> Option<usize> {
    field_offset_in(&layout_fields_ref(metaspace, named_class), field)
}

/// Read-only twin of [`allocate`] for the W2c lock-free `.read()` path: computes the instance
/// size and class-id header from `&MetaspaceService`, then allocates in Eden without the VM lock
/// (`heap.alloc_object_lockfree`). `None` = escalate to the write path (the class's id isn't
/// minted yet — i.e. not prepared — or Eden is full). `idx` is the allocating thread's slot.
pub fn allocate_read(
    metaspace: &MetaspaceService,
    heap: &HeapService,
    name: &str,
    idx: usize,
) -> Option<usize> {
    let uuid = metaspace.class_id_read(name)?; // not prepared → escalate
    let class_id = metaspace.class_object(uuid).unwrap_or(0) as u32;
    let slots = instance_field_slots_read(metaspace, name);
    let size = HEADER_SIZE + slots * SLOT_SIZE;
    heap.alloc_object_lockfree(size, class_id, idx)
}

/// Read-only twin of [`instance_field_slots`] (uses `get`; stops at any unloaded super).
fn instance_field_slots_read(metaspace: &MetaspaceService, name: &str) -> usize {
    total_slots(&layout_fields_ref(metaspace, name))
}

#[cfg(test)]
mod tests {
    use super::*;

    /// The 8-alignment rule: a `long`/`double` field always lands on an even slot, so its byte
    /// offset (`HEADER_SIZE + slot*4`, with `HEADER_SIZE` = 8) is 8-aligned — the precondition for
    /// `AtomicU64` field access (H4). A category-1 field before it may force a padding slot.
    #[test]
    fn category_two_fields_are_eight_aligned() {
        // Fresh object: an `int` at slot 0, then a `long`. The long can't start at slot 1 (offset
        // 12, only 4-aligned), so it pads to slot 2 (offset 16, 8-aligned).
        let (int_start, after_int) = place_field(0, "I");
        assert_eq!((int_start, after_int), (0, 1));
        let (long_start, after_long) = place_field(after_int, "J");
        assert_eq!(long_start, 2, "long padded past the odd slot");
        assert_eq!(after_long, 4);
        assert_eq!((HEADER_SIZE + long_start * SLOT_SIZE) % 8, 0, "long offset is 8-aligned");

        // A `long` already on an even slot needs no padding.
        assert_eq!(place_field(2, "D"), (2, 4));
        assert_eq!(place_field(0, "J"), (0, 2));
        // Category-1 fields never pad.
        assert_eq!(place_field(1, "I"), (1, 2));
        assert_eq!(place_field(3, "Ljava/lang/Object;"), (3, 4));
    }

    /// Every offset a fold produces for a category-2 field is 8-aligned, whatever the mix of
    /// preceding fields — the invariant the GC walk, size, and access all rely on.
    #[test]
    fn folded_long_offsets_stay_aligned() {
        // int, long, int, double, long — a deliberately awkward interleaving.
        let fields: Vec<(String, String)> = ["I", "J", "I", "D", "J"]
            .iter()
            .enumerate()
            .map(|(i, d)| ((*d).to_string(), format!("f{i}")))
            .collect();
        for (i, d) in ["I", "J", "I", "D", "J"].iter().enumerate() {
            if matches!(d.as_bytes()[0], b'J' | b'D') {
                let off = field_offset_in(&fields, &format!("f{i}")).unwrap();
                assert_eq!(off % 8, 0, "field f{i} ({d}) at offset {off} must be 8-aligned");
            }
        }
    }
}
