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

use super::class_operations;
use crate::jvm::interpreter::frame::{Frame, Value};
use crate::jvm::interpreter::heap::HeapService;
use crate::jvm::interpreter::metaspace::{MetaspaceService, MethodId};
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

/// The heap size of an instance of `name`.
///
/// `[header | inherited fields | own fields]`, one slot per field — except for
/// `java.lang.String`, whose characters do not live in fields at all. The VM lays them out
/// inline after a length word (see [`crate::jvm::interpreter::strings`]), so an instance with
/// no characters is still bigger than its header.
///
/// Sizing it correctly matters even though `new` cannot know how long the string will be: the
/// object it produces is what a String constructor runs on, and it must be a *readable empty
/// String* rather than a header with nothing behind it. Getting this wrong does not fail at the
/// allocation, it fails later and elsewhere, as a read past the end of the heap block.
fn instance_size(metaspace: &mut MetaspaceService, name: &str) -> usize {
    if name == "java/lang/String" {
        return crate::jvm::interpreter::strings::STRING_HEADER;
    }
    HEADER_SIZE + instance_field_slots(metaspace, name) * SLOT_SIZE
}

/// Allocates a fresh, zero-initialised instance of `name` on the heap and returns
/// its offset (the object reference). Lays out `[header | inherited fields | own
/// fields]`: the size counts the instance fields of `name` **and every
/// superclass** (inheritance — a `Dog` carries `Animal`'s fields too), each one
/// slot wide. `malloc` zeroes the fields (their default values); the header's
/// `class_id` is filled with the class's `Class<…>` mirror offset, so the object
/// knows what it is.
pub fn allocate(metaspace: &mut MetaspaceService, heap: &mut HeapService, name: &str) -> usize {
    let size = instance_size(metaspace, name);
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

/// Fallible twin of [`allocate`] for the `new` **opcode**: `None` when the heap is
/// exhausted (neither Eden nor Old within `JVM_GC_MAX_HEAP` can fit the instance),
/// so the opcode can throw a catchable `java.lang.OutOfMemoryError` (JVMS §6.3)
/// instead of panicking the VM. Internal VM allocations (exception objects, interned
/// strings, mirrors, `Thread` objects) keep the panicking [`allocate`] — their failure
/// paths aren't cleanly recoverable mid-operation, so exhaustion there stays fatal.
pub fn try_allocate(metaspace: &mut MetaspaceService, heap: &mut HeapService, name: &str) -> Option<usize> {
    let size = instance_size(metaspace, name);
    let offset = heap.try_malloc(size)?;
    let uuid = metaspace.class_id(name).to_string();
    let class_id = metaspace.class_object(&uuid).unwrap_or(0) as u32;
    heap.write_u32(offset, class_id);
    Some(offset)
}

/// Like [`allocate`], but in the **Old** generation (`malloc_old`) — for objects the VM builds
/// while holding *young* references in Rust locals it can't root (a condy's target `MethodHandle`,
/// its `Object[]` args). Old allocation never triggers a minor GC and Old objects don't move, so
/// those Rust-held references stay valid across the build. Callers must store references into the
/// resulting object with the write barrier ([`HeapService::store_reference`]) so an Old→young
/// pointer is remembered.
pub fn allocate_old(metaspace: &mut MetaspaceService, heap: &mut HeapService, name: &str) -> usize {
    let size = instance_size(metaspace, name);
    let offset = heap.malloc_old(size);
    let uuid = metaspace.class_id(name).to_string();
    let class_id = metaspace.class_object(&uuid).unwrap_or(0) as u32;
    heap.write_u32(offset, class_id);
    offset
}

/// `Object.clone()`'s copy step: allocates a fresh instance of `class` and copies every
/// instance field of `source` into it verbatim — the **shallow** copy of JLS §10.7 (reference
/// fields copy the reference, so original and clone share the pointees). The Cloneable check
/// happens at the call site (the invoke interception, which can throw); this is just the copy.
///
/// Old-allocated (see [`allocate_old`]): `source` is held in a Rust local across this
/// allocation, and an Eden allocation could trigger a minor GC that moves it. Old allocation
/// never GCs and Old objects don't move, so `source` stays valid for the whole copy. Reference
/// fields go in through the write barrier ([`HeapService::store_reference`]) so the clone's
/// Old→young pointers land in the remembered set.
pub fn clone_instance(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    source: usize,
    class: &str,
) -> usize {
    let clone = allocate_old(metaspace, heap, class);
    // Fold the single placement rule over the super-first layout — the same walk the
    // field offsets use, so every field (and its alignment padding) lands where it came from.
    let mut slots = 0;
    for (descriptor, _) in layout_fields_mut(metaspace, class) {
        let (start, next) = place_field(slots, &descriptor);
        let at = HEADER_SIZE + start * SLOT_SIZE;
        match descriptor.as_bytes().first() {
            // Category-2 (long/double): one 8-byte copy.
            Some(b'J') | Some(b'D') => {
                let value = heap.read_u64(source + at);
                heap.write_u64(clone + at, value);
            }
            // Reference: copy the pointer through the barrier gateway (shallow — shared pointee).
            Some(b'L') | Some(b'[') => {
                let value = heap.read_u32(source + at) as usize;
                heap.store_reference(clone, clone + at, value);
            }
            // Everything else is one 4-byte slot.
            _ => {
                let value = heap.read_u32(source + at);
                heap.write_u32(clone + at, value);
            }
        }
        slots = next;
    }
    clone
}

// ---- F0 quickening: the per-call-site field cache ------------------------------------------
//
// A `getfield`/`putfield` at a given `(method, pc)` always names the **same** constant-pool
// entry, and a class's layout is fixed once the class is loaded — so resolving that site is a
// pure function computed once and reused. Without the cache every execution paid three or four
// `String` allocations plus a full rebuild of the class's super-first field list (a fresh
// `Vec<(String, String)>` walking the whole superclass chain) just to fold it back down to one
// offset. Here that whole resolution collapses to a single indexed atomic load of a `Copy` word
// living in the method's own [`MethodId`]-indexed body (`MetaspaceService::field_site`).

/// How to read/write the bytes of a field — the descriptor's first byte, decided **once** at
/// resolution so the hot path never touches the descriptor string again.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum FieldKind {
    /// `Z`/`B`/`C`/`S`/`I` — one 4-byte slot read back as an `Int`.
    Int,
    /// `J` — category-2, 8 bytes.
    Long,
    /// `D` — category-2, 8 bytes, `f64` bits.
    Double,
    /// `F` — 4 bytes, `f32` bits.
    Float,
    /// `L…;`/`[…` — 4 bytes read back as a `Reference`.
    Reference,
}

/// Everything a field access needs to know about its site, resolved once: **where** the field
/// sits in the object, **how wide** it is, and whether it is `volatile` (which the H4 lock-free
/// read path turns into `Acquire`/`Release`). `Copy` and packable into one `u64`.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
struct FieldSite {
    /// Byte offset of the field inside the object (header included) — what [`field_offset`] returns.
    offset: usize,
    kind: FieldKind,
    volatile: bool,
}

impl FieldSite {
    /// Packs the site into the cache's `u64`: `[offset: 32 | kind: 3 | volatile: 1 | present: 1]`.
    /// Bit 0 is always set, so the cache's zero-initialised cell is an unambiguous "unresolved".
    fn pack(self) -> u64 {
        1 | ((self.volatile as u64) << 1) | ((self.kind as u64) << 2) | ((self.offset as u64) << 32)
    }

    /// The inverse of [`Self::pack`] — `None` for the unresolved sentinel (`0`). This is the whole
    /// hot path: a shift and a mask, no allocation and no hashing.
    fn unpack(bits: u64) -> Option<FieldSite> {
        if bits & 1 == 0 {
            return None;
        }
        let kind = match (bits >> 2) & 0b111 {
            1 => FieldKind::Long,
            2 => FieldKind::Double,
            3 => FieldKind::Float,
            4 => FieldKind::Reference,
            _ => FieldKind::Int,
        };
        Some(FieldSite { offset: (bits >> 32) as usize, kind, volatile: bits & 0b10 != 0 })
    }
}

/// The access width a field `descriptor` implies — the same first-byte test the uncached code
/// did inline, hoisted to resolution time.
fn field_kind(descriptor: &str) -> FieldKind {
    match descriptor.as_bytes().first() {
        Some(b'J') => FieldKind::Long,
        Some(b'D') => FieldKind::Double,
        Some(b'F') => FieldKind::Float,
        Some(b'L') | Some(b'[') => FieldKind::Reference,
        _ => FieldKind::Int,
    }
}

/// The resolved [`FieldSite`] of the field access at `(method, pc)`, from the cache on a hit and
/// by full resolution (loading classes as needed) on a miss — which then fills the cache. Write
/// path: `&mut MetaspaceService`, so it can never fail the way the read path can. `op` only
/// names the opcode in the panic message for a malformed `FieldRef`.
fn resolve_field_site(
    metaspace: &mut MetaspaceService,
    method: MethodId,
    pc: usize,
    cp_index: u16,
    op: &str,
) -> FieldSite {
    if let Some(site) = FieldSite::unpack(metaspace.field_site(method, pc)) {
        return site;
    }
    let caller = metaspace.class_of(method).to_string();
    let (named, field, descriptor) = {
        let cf = metaspace.get(&caller).expect("caller class is loaded");
        let Some((c, n, d)) = cf.fieldref_target(cp_index) else { panic!("{op}: bad FieldRef") };
        (c.to_string(), n.to_string(), d.to_string())
    };
    // Accesibilidad (JPMS): el sitio se resuelve una sola vez y queda cacheado, así que el
    // chequeo va acá — antes de fijar el offset en la caché de F0.
    class_operations::check_access(metaspace, &caller, &named);
    // `field_offset` loads the whole superclass chain, so the volatility walk right after it
    // (read-only, `get`) always sees every class it needs.
    let offset = field_offset(metaspace, &named, &field);
    let site =
        FieldSite { offset, kind: field_kind(&descriptor), volatile: field_is_volatile_read(metaspace, &named, &field) };
    metaspace.set_field_site(method, pc, site.pack());
    site
}

/// `putfield` (0xb5): pop a value and an object reference, and write the value into
/// the object's field on the heap. The field is named by `cp_index` (a `FieldRef`
/// in the current method's class); its byte offset inside the object comes from the
/// site cache ([`resolve_field_site`], which folds the layout on the first execution only).
pub fn putfield(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    frame: &mut Frame,
    cp_index: u16,
) -> Result<(), &'static str> {
    let field_off = resolve_field_site(metaspace, frame.method(), frame.pc(), cp_index, "putfield").offset;

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
/// fields, read from the heap. Mirror of [`putfield`]. The field's [`FieldKind`] — resolved
/// once with its offset — decides how to read the bytes back: a reference field yields a
/// `Reference`, a `long`/`double` reads 8 bytes, anything else an `Int`.
pub fn getfield(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    frame: &mut Frame,
    cp_index: u16,
) -> Result<(), &'static str> {
    let site = resolve_field_site(metaspace, frame.method(), frame.pc(), cp_index, "getfield");

    let object = match frame.pop() {
        Value::Reference(0) => return Err(NULL_POINTER), // null receiver → NPE
        Value::Reference(offset) => offset,
        _ => panic!("getfield: expected an object reference"),
    };
    let at = object + site.offset;
    let value = match site.kind {
        FieldKind::Long => Value::Long(heap.read_u64(at) as i64),
        FieldKind::Double => Value::Double(f64::from_bits(heap.read_u64(at))),
        FieldKind::Float => Value::Float(f32::from_bits(heap.read_u32(at))),
        FieldKind::Reference => Value::Reference(heap.read_u32(at) as usize),
        FieldKind::Int => Value::Int(heap.read_u32(at) as i32),
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
/// The `bool` is **completeness**: `true` when the walk ran off the top of the hierarchy (a class
/// declaring no superclass), `false` when it stopped early because a named superclass wasn't
/// loaded — in which case the list is missing that prefix and every offset folded from it is too
/// small. Only a *complete* layout is worth caching (see [`resolve_field_site_read`]).
fn layout_fields_ref(metaspace: &MetaspaceService, class: &str) -> (Vec<(String, String)>, bool) {
    let mut chain: Vec<Vec<(String, String)>> = Vec::new();
    let mut current = Some(class.to_string());
    let mut complete = true;
    while let Some(name) = current.take() {
        let Some(cf) = metaspace.get(&name) else {
            complete = false;
            break;
        };
        current = cf.class_name(cf.super_class).map(|s| s.to_string());
        chain.push(own_instance_fields(cf));
    }
    (chain.into_iter().rev().flatten().collect(), complete)
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
    try_field_offset(metaspace, named_class, field)
        .expect("field_offset: field not found in the class or its superclasses")
}

/// The fallible half of [`field_offset`]: `None` when neither the class nor any superclass
/// declares `field`.
///
/// The distinction is not stylistic. A caller that reads a field the *bytecode* named requires
/// it to exist, and a missing one is a linkage failure worth the panic. A caller that reads a
/// field by a name the **VM** chose — `backtrace`, say — is asking the library to cooperate with
/// an implementation detail, and a library that declares no such field is not malformed. That
/// second kind must degrade, not die (COMPILER_FINDINGS #227).
pub fn try_field_offset(
    metaspace: &mut MetaspaceService,
    named_class: &str,
    field: &str,
) -> Option<usize> {
    field_offset_in(&layout_fields_mut(metaspace, named_class), field)
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

/// Read-path twin of [`resolve_field_site`]: the site cache on a hit, a read-only resolution
/// (`get`, never loading) on a miss — `None` when that resolution can't complete, which is
/// exactly today's escalation condition.
///
/// It **also fills** the cache, with `&MetaspaceService`, because the cells are atomic. That's
/// the point: a workload that only ever runs `getfield` on the W3 path (os-parallel readers)
/// would otherwise never populate a single site and stay on the slow path forever. The race is
/// benign — a site's resolution is a pure function of `(method, pc)` and the class layouts, so
/// every thread that fills a cell writes the *same* word, and a `u64` store is indivisible, so no
/// reader can observe a half-written entry.
///
/// The **completeness** guard is what keeps that sound: if the walk stopped at an unloaded
/// superclass the offsets are wrong (the missing prefix shifts them), so we use the value for
/// this one escalating access but never cache it. Concretely, a `getfield` on a `null` receiver
/// resolves the site *before* it discovers the null — without the guard it could immortalise a
/// bad offset from a class whose chain wasn't loaded yet.
fn resolve_field_site_read(
    metaspace: &MetaspaceService,
    method: MethodId,
    pc: usize,
    cp_index: u16,
) -> Option<FieldSite> {
    if let Some(site) = FieldSite::unpack(metaspace.field_site(method, pc)) {
        return Some(site);
    }
    let caller = metaspace.class_of(method);
    let (named, field, descriptor) = metaspace.get(caller)?.fieldref_target(cp_index)?;
    class_operations::check_access(metaspace, caller, named);
    let (layout, complete) = layout_fields_ref(metaspace, named);
    let offset = field_offset_in(&layout, field)?;
    let site =
        FieldSite { offset, kind: field_kind(descriptor), volatile: field_is_volatile_read(metaspace, named, field) };
    if complete {
        metaspace.set_field_site(method, pc, site.pack());
    }
    Some(site)
}

/// The **JIT's** field resolver: the byte offset of the `getfield`/`putfield` at `(method, pc)`
/// inside its receiver **and what kind of value is there**, or `None` unless the field is a
/// non-`volatile` instance field of a primitive type.
///
/// It is [`resolve_field_site_read`] plus one translation, and **both of the conditions it used to
/// add are gone** — group 2 of the F3-JIT widening removed them, each for its own reason:
///
///  - **Every kind, references included.** The kind decides the *width* and the *extension* of the
///    emitted access — four bytes sign-extending for an `int`, four zero-extending for a `float` or
///    a **reference**, eight for a `long` or a `double` — so it is handed back rather than left to
///    be inferred, which is why the answer is a pair and not just an offset. A reference field used
///    to be refused here because a compiled `putfield` of one would need the GC write barrier; the
///    barrier is still owed, and since F3-H3 the compiler *pays* it — the emitted store records the
///    `(holder, value)` pair and the JIT trampoline replays it through
///    `HeapService::replay_jit_reference_store` (see `burst::compile`'s `putfield` arm and
///    `CompiledCode::barrier_base`). This resolver answers what the field *is*; the compiler decides
///    what has to happen around writing it.
///  - **`volatile` too, now** — for reads *and* for primitive writes. VOLATILE-REVISIT-OS-PARALLEL.
///    The argument is **not** x86-TSO. It is that the JIT runs only on `green` (one OS thread) and
///    `os-gil` (the thread holds the one global lock for the whole opcode, the native call
///    included), so while compiled code is on this stack **no other thread executes a single
///    opcode**. There is no concurrency for an `Acquire`/`Release` to order and no observer for an
///    8-byte `long` access to tear in front of, so a plain `mov` is not merely permitted, it is the
///    same execution. Enabling the JIT on `os-parallel` **invalidates this** and every volatile
///    access here must then be revisited — grep the marker above.
///
/// Read-only (`&MetaspaceService`), like every other resolver the compiler is handed — compiling
/// must not load a class or run a `<clinit>`. It does *fill* the resolved-site cache, which is a
/// pure function of `(method, pc)` and therefore not a change to the VM's state in any sense the
/// program can observe.
pub fn jit_field_site(
    metaspace: &MetaspaceService,
    method: MethodId,
    pc: usize,
    cp_index: u16,
) -> Option<(u32, crate::burst::compile::Kind)> {
    let site = resolve_field_site_read(metaspace, method, pc, cp_index)?;
    let kind = match site.kind {
        FieldKind::Int => crate::burst::compile::Kind::Int,
        FieldKind::Long => crate::burst::compile::Kind::Long,
        FieldKind::Float => crate::burst::compile::Kind::Float,
        FieldKind::Double => crate::burst::compile::Kind::Double,
        FieldKind::Reference => crate::burst::compile::Kind::Reference,
    };
    Some((u32::try_from(site.offset).ok()?, kind))
}

/// Read-only `getfield`. `Some(())` = read the field and pushed it (ran concurrently); `None` =
/// escalate to the write path (null receiver or an unloaded class). On escalation the operand
/// stack is left exactly as it was (the popped receiver is pushed back).
pub fn getfield_read(
    metaspace: &MetaspaceService,
    heap: &HeapService,
    frame: &mut Frame,
    cp_index: u16,
) -> Option<()> {
    let site = resolve_field_site_read(metaspace, frame.method(), frame.pc(), cp_index)?;
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
    let volatile = site.volatile;
    let at = object + site.offset;
    let value = match site.kind {
        FieldKind::Long => Value::Long(read_u64_field(heap, at, volatile) as i64),
        FieldKind::Double => Value::Double(f64::from_bits(read_u64_field(heap, at, volatile))),
        FieldKind::Float => Value::Float(f32::from_bits(read_u32_field(heap, at, volatile))),
        FieldKind::Reference => Value::Reference(read_u32_field(heap, at, volatile) as usize),
        FieldKind::Int => Value::Int(read_u32_field(heap, at, volatile) as i32),
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
    let site = resolve_field_site_read(metaspace, frame.method(), frame.pc(), cp_index)?;

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

    let order = if site.volatile { Ordering::Release } else { Ordering::Relaxed };
    let at = object + site.offset;
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
    field_offset_in(&layout_fields_ref(metaspace, named_class).0, field)
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
    total_slots(&layout_fields_ref(metaspace, name).0)
}

/// What the JIT bakes into a compiled `new`: the instance's `(size, class_id)` for the class named
/// at `cp_index` in `caller`'s constant pool — see `burst::compile::Instance`.
///
/// Read-only in every sense, and that is the design, exactly as for
/// [`jit_static_field`][super::class_operations::jit_static_field]: it takes
/// `&MetaspaceService`, so it cannot load a class, mint a Class ID, allocate a mirror or run a
/// `<clinit>`. Compilation must not have side effects on the VM's state, and the signature is what
/// enforces that rather than a comment.
///
/// `None` — "do not compile this method" — unless **all four** hold:
///
///  1. the constant-pool entry is a `Class` whose name resolves;
///  2. that class is **initialised** (`InitState::Done`). `new` is a first active use, so an
///     uninitialised class would have to run its `<clinit>` — and compiled code cannot run
///     anything. This is the same requirement `getstatic` has, refused for the same reason, and
///     cached just as permanently;
///  3. its Class ID is minted (i.e. it is *prepared*), so the layout below is the real one;
///  4. its `Class<…>` mirror exists, and its offset fits the `u32` the header word is. The mirror is
///     `malloc_old`ed and pinned against `gc::compact`, so that offset never moves — which is what
///     makes baking it in sound.
///
/// The size is the **logical** one — `[header | inherited fields | own fields]`, each field at its
/// width-aware slot — computed by the very same walk [`allocate`] uses, because compiled code and
/// the interpreter must not come to disagree about how big a `Point` is.
pub fn jit_instance(metaspace: &MetaspaceService, caller: &str, cp_index: u16) -> Option<(u32, u32)> {
    let class_name = metaspace.get(caller)?.class_name(cp_index)?.to_string();
    if metaspace.init_state(&class_name) != crate::jvm::interpreter::metaspace::InitState::Done {
        return None;
    }
    let uuid = metaspace.class_id_read(&class_name)?;
    let class_id = u32::try_from(metaspace.class_object(uuid)?).ok()?;
    let slots = instance_field_slots_read(metaspace, &class_name);
    let size = u32::try_from(HEADER_SIZE + slots * SLOT_SIZE).ok()?;
    Some((size, class_id))
}

/// Read-path (lock-free) `AtomicInteger`/`AtomicLong.compareAndSet` (H5, widened like W3): when the
/// receiver is an **Eden** object, do a real atomic `compare_exchange` on its `value` field under
/// the shared `.read()` lock — no VM write lock. `Some(())` = did the CAS and pushed the boolean;
/// `None` = escalate to the locked native (an Old receiver, or the class isn't loaded). The stack
/// top is `[receiver, expected, new]`; on escalation it's left untouched. (`AtomicReference` is
/// never intercepted — its store needs the GC write barrier, which the write path owns.)
pub fn atomic_cas_read(
    metaspace: &MetaspaceService,
    heap: &HeapService,
    frame: &mut Frame,
    class: &str,
    is_long: bool,
) -> Option<()> {
    let field_off = field_offset_read(metaspace, class, "value")?;
    // Peek `[receiver, expected, new]` (owned copies) so the stack borrow ends before we mutate it.
    let (receiver, expected, new) = {
        let stack = frame.stack();
        let top = stack.len();
        if top < 3 {
            return None;
        }
        let receiver = match stack[top - 3] {
            Value::Reference(0) => return None, // null → escalate (the native throws NPE)
            Value::Reference(offset) => offset,
            _ => return None,
        };
        (receiver, stack[top - 2], stack[top - 1])
    };
    let at = receiver + field_off;
    let swapped = if is_long {
        let (Value::Long(expected), Value::Long(new)) = (expected, new) else { return None };
        heap.cas_u64_eden(at, expected as u64, new as u64)? // None (Old) → escalate
    } else {
        let (Value::Int(expected), Value::Int(new)) = (expected, new) else { return None };
        heap.cas_u32_eden(at, expected as u32, new as u32)?
    };
    // The CAS ran (Eden): drop the three arguments and push the boolean result.
    frame.pop();
    frame.pop();
    frame.pop();
    frame.push(Value::Int(swapped as i32));
    Some(())
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

    /// Pack → unpack is the identity for every `(offset, kind, volatile)` a field site can hold,
    /// and the zero cell reads back as "unresolved". The F0 field cache rests entirely on this:
    /// a site that unpacked to a different offset or width would read the *wrong bytes* of an
    /// object — silently, with no other test in the suite positioned to notice.
    #[test]
    fn field_site_packing_round_trips() {
        assert_eq!(FieldSite::unpack(0), None, "the zero cell is the unresolved sentinel");

        let kinds =
            [FieldKind::Int, FieldKind::Long, FieldKind::Double, FieldKind::Float, FieldKind::Reference];
        for kind in kinds {
            // 0 and a full 32-bit offset bracket the field's range; 8 and 12 are the first two
            // real field offsets (`HEADER_SIZE` = 8, one slot = 4).
            for offset in [0usize, 8, 12, 0xffff_ffff] {
                for volatile in [false, true] {
                    let site = FieldSite { offset, kind, volatile };
                    let packed = site.pack();
                    assert_ne!(packed & 1, 0, "a packed site always sets the present bit");
                    assert_eq!(
                        FieldSite::unpack(packed),
                        Some(site),
                        "round-trip of {site:?} through {packed:#018x}"
                    );
                }
            }
        }

        // The descriptor's first byte is what picks the kind — the test the hot path no longer runs.
        assert_eq!(field_kind("J"), FieldKind::Long);
        assert_eq!(field_kind("D"), FieldKind::Double);
        assert_eq!(field_kind("F"), FieldKind::Float);
        assert_eq!(field_kind("Ljava/lang/String;"), FieldKind::Reference);
        assert_eq!(field_kind("[I"), FieldKind::Reference);
        for primitive in ["Z", "B", "C", "S", "I"] {
            assert_eq!(field_kind(primitive), FieldKind::Int, "{primitive} is a 4-byte int slot");
        }
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
