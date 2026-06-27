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

/// The byte offset of field `field` *within an object* of class `named_class`.
/// `named_class` is the class named in the `FieldRef`, which may not be the one
/// that *declares* the field (an inherited field is accessed through the subclass),
/// so we first locate the declaring class by walking up. Superclass fields come
/// first in the layout — so an inherited field sits at the same offset in a subclass
/// instance — then the declaring class's own fields, in declaration order.
/// Offset = header + (super slots + own slots before `field`) — width-aware, so a
/// `long`/`double` field counts as two slots and shifts the ones after it.
pub fn field_offset(metaspace: &mut MetaspaceService, named_class: &str, field: &str) -> usize {
    let declaring = declaring_class(metaspace, named_class, field)
        .expect("field_offset: field not found in the class or its superclasses");

    // Everything declared above the declaring class is laid out before its fields.
    let super_name = metaspace
        .get_or_load(&declaring)
        .and_then(|cf| cf.class_name(cf.super_class).map(|s| s.to_string()));
    let super_slots = super_name.map_or(0, |s| instance_field_slots(metaspace, &s));

    // Slots occupied by the declaring class's own instance fields *before* `field`.
    let own_slots = metaspace
        .get_or_load(&declaring)
        .map(|cf| {
            let mut slots = 0;
            for f in cf.fields.iter().filter(|f| !f.is_static()) {
                if cf.utf8(f.name_index) == Some(field) {
                    return slots;
                }
                slots += field_slots(cf.utf8(f.descriptor_index).unwrap_or(""));
            }
            panic!("field_offset: field vanished from its declaring class");
        })
        .expect("field_offset: declaring class not loadable");

    HEADER_SIZE + (super_slots + own_slots) * SLOT_SIZE
}

/// Finds the class that actually *declares* `field`, starting at `start` and
/// walking up the superclass chain — the resolution `getfield`/`putfield` do, since
/// a `FieldRef` may name a subclass that only inherits the field.
fn declaring_class(metaspace: &mut MetaspaceService, start: &str, field: &str) -> Option<String> {
    let mut current = Some(start.to_string());
    while let Some(class_name) = current.take() {
        let (declares, super_name) = match metaspace.get_or_load(&class_name) {
            Some(cf) => (
                cf.fields
                    .iter()
                    .filter(|f| !f.is_static())
                    .any(|f| cf.utf8(f.name_index) == Some(field)),
                cf.class_name(cf.super_class).map(|s| s.to_string()),
            ),
            None => return None,
        };
        if declares {
            return Some(class_name);
        }
        current = super_name;
    }
    None
}

/// The total instance-field **slots** of `name` plus those of every superclass,
/// walking up the chain until a super can't be loaded (e.g. `java.lang.Object`,
/// which isn't on our classpath). Width-aware — a `long`/`double` field counts as
/// two slots — so this is the object's size in slots (inherited fields included).
fn instance_field_slots(metaspace: &mut MetaspaceService, name: &str) -> usize {
    let mut slots = 0;
    let mut current = Some(name.to_string());
    while let Some(class_name) = current.take() {
        // Extract what we need under the borrow, then drop it before the next hop.
        let (own, super_name) = match metaspace.get_or_load(&class_name) {
            Some(class) => (
                class
                    .fields
                    .iter()
                    .filter(|f| !f.is_static())
                    .map(|f| field_slots(class.utf8(f.descriptor_index).unwrap_or("")))
                    .sum::<usize>(),
                class.class_name(class.super_class).map(|s| s.to_string()),
            ),
            None => break, // class not loadable → stop climbing
        };
        slots += own;
        current = super_name;
    }
    slots
}
