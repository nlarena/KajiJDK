//! Class/object opcodes — the family that creates and manipulates heap objects:
//! `new` (allocate an instance), `getfield`/`putfield` (instance fields), and the
//! object invokes (`invokespecial`/`invokevirtual`/`invokeinterface`).
//!
//! Skeleton stage: `load_class` does the part we already have (parsing into the
//! metaspace); `new` and the rest wait on the object layout (size from fields) and
//! the heap's byte-writers (to lay out the header + fields).

use super::objects_operations::{self, HEADER_SIZE, SLOT_SIZE};
use crate::jvm::interpreter::frame::{Frame, Value};
use crate::jvm::interpreter::heap::HeapService;
use crate::jvm::interpreter::metaspace::MetaspaceService;

/// Ensures the class `name` is loaded, HotSpot style:
///  - **Loading**: parse it into the metaspace (idempotent — cached after first).
///  - **Preparation** (still to come): allocate its Class object on the *heap*,
///    sized by its static fields, to hold the statics — which is why loading now
///    takes the heap. Needs the field layout + the heap byte-writers.
pub fn load_class(metaspace: &mut MetaspaceService, heap: &mut HeapService, name: &str) {
    // Loading: parse the class into the metaspace (idempotent — cached after the
    // first time it's seen).
    metaspace.get_or_load(name);

    // Preparation is *deduplicated* by the class's identity: a class is prepared
    // exactly once. If it already has a Class ID (UUID), it's been prepared —
    // ignore. Otherwise this is the first sight: mint its id now. (Correct as long
    // as `load_class` stays the only place a class's id is minted.)
    if metaspace.has_class_id(name) {
        return;
    }
    // First sight: mint the class's identity UUID. We hold onto it because the
    // mirror index is keyed by this id, not by the name.
    let uuid = metaspace.class_id(name).to_string();

    // Size the mirror = header + the static fields' slots (width-aware: a `long`/
    // `double` static takes two). `malloc` returns zeroed memory, so each static
    // starts at its default value (0 / null) — exactly what Preparation prescribes.
    let static_slots = match metaspace.get(name) {
        // Fold the single placement rule so the mirror is sized with the same 8-alignment
        // padding `static_slot` uses for offsets (a per-field sum would miss the padding).
        Some(class) => {
            let mut slots = 0;
            for f in class.fields.iter().filter(|f| f.is_static()) {
                slots = objects_operations::place_field(slots, class.utf8(f.descriptor_index).unwrap_or("")).1;
            }
            slots
        }
        None => return, // the class couldn't be loaded; nothing to prepare
    };
    let size = HEADER_SIZE + static_slots * SLOT_SIZE;
    // Mirrors are permanent roots (a loaded class outlives any object) and are pinned
    // by the GC — so allocate them straight into the **Old** generation, never Eden.
    let offset = heap.malloc_old(size);

    // Register the mirror's offset under the Class ID, so getstatic/putstatic (and
    // the GC) reach the statics through the class's stable identity.
    metaspace.set_class_object(&uuid, offset);

    // A `Class<…>` mirror is itself an instance of `java.lang.Class`, so its header's
    // `class_id` points at `java.lang.Class`'s mirror — that's what makes
    // `getClass()` + `invokevirtual` on a Class object dispatch correctly.
    // `java.lang.Class`'s own mirror is, recursively, an instance of itself.
    let class_mirror = if name == "java/lang/Class" {
        offset
    } else {
        load_class(metaspace, heap, "java/lang/Class");
        let class_uuid = metaspace.class_id("java/lang/Class").to_string();
        metaspace.class_object(&class_uuid).unwrap_or(0)
    };
    heap.write_u32(offset, class_mirror as u32);
}

/// `new` (0xbb): allocate an instance of the class named at `cp_index` (resolved
/// against the current frame's class) and push a reference to it. Loads the class
/// first, sizes the object from its field layout, `malloc`s it, writes the header
/// `[class_id | mark]`, and pushes the offset as a `Value::Reference`.
///
/// `Err` when the heap is exhausted — the dispatch loop throws it as a catchable
/// `java.lang.OutOfMemoryError` (JVMS §6.3) instead of panicking the VM.
pub fn new(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    frame: &mut Frame,
    cp_index: u16,
) -> Result<(), &'static str> {
    // Resolution: `cp_index` is a `Class` entry in the *current* class's pool —
    // resolve it to the target's binary name (e.g. "Point"). The caller's class is
    // already loaded (we're running its bytecode), so the lookup can't miss.
    let caller_class = metaspace.class_of(frame.method()).to_string();
    let class_name = metaspace
        .get(&caller_class)
        .and_then(|cf| cf.class_name(cp_index))
        .expect("new: cp_index does not point to a Class constant")
        .to_string();

    // Loading (+ Preparation): bring the target class into the metaspace before we
    // can lay out an instance of it — we need its fields to know the object's size.
    check_access(metaspace, &caller_class, &class_name);
    load_class(metaspace, heap, &class_name);

    // Allocate the instance on the heap and push its reference. Per the JVM, `new`
    // only allocates (zeroed fields); the following `dup`/`invokespecial <init>`
    // run the constructor. A `None` here is true heap exhaustion (the GC already had
    // its chances at earlier safepoints and nothing more is reclaimable) → OOM.
    let Some(offset) = objects_operations::try_allocate(metaspace, heap, &class_name) else {
        return Err("java/lang/OutOfMemoryError");
    };
    frame.push(Value::Reference(offset));
    Ok(())
}

/// Enforces the JPMS access rule at a **resolution site** (JVMS §5.4.4).
///
/// Every place the interpreter turns a constant-pool reference into a class is a place the
/// rule applies — `new`, the `invoke*`s, the field accesses, `checkcast`. Checking only one
/// of them would read like encapsulation while leaving the others open, so the check lives
/// here and every site calls it.
///
/// A VM booted from directories has no module graph and this always passes; see
/// [`MetaspaceService::can_access`].
pub fn check_access(metaspace: &MetaspaceService, caller: &str, target: &str) {
    if !metaspace.can_access(caller, target) {
        panic!(
            "IllegalAccessError: '{caller}' no puede acceder a '{target}'              (el módulo no lo lee, o el paquete no está exportado)"
        );
    }
}

/// `getstatic` (0xb2): push the value of a *static* field. Unlike `getfield`, there
/// is no receiver — the value lives in the class's `Class<…>` mirror on the heap,
/// located by class (not by an object reference on the stack).
pub fn getstatic(metaspace: &mut MetaspaceService, heap: &mut HeapService, frame: &mut Frame, cp_index: u16) {
    let caller = metaspace.class_of(frame.method()).to_string();
    let (named, field, descriptor) = {
        let cf = metaspace.get(&caller).expect("caller class is loaded");
        let (c, n, d) = cf.fieldref_target(cp_index).expect("getstatic: bad FieldRef");
        (c.to_string(), n.to_string(), d.to_string())
    };
    check_access(metaspace, &caller, &named);
    let at = static_slot(metaspace, heap, &named, &field);
    let value = match descriptor.as_bytes().first() {
        Some(b'J') => Value::Long(heap.read_u64(at) as i64),
        Some(b'D') => Value::Double(f64::from_bits(heap.read_u64(at))),
        Some(b'F') => Value::Float(f32::from_bits(heap.read_u32(at))),
        Some(b'L') | Some(b'[') => Value::Reference(heap.read_u32(at) as usize),
        _ => Value::Int(heap.read_u32(at) as i32),
    };
    frame.push(value);
}

/// `putstatic` (0xb3): pop a value and write it into a *static* field's slot in the
/// class's mirror. The mirror of `getstatic` — and the building block of `<clinit>`
/// (a `static int X = 42` compiles to a `putstatic`).
pub fn putstatic(metaspace: &mut MetaspaceService, heap: &mut HeapService, frame: &mut Frame, cp_index: u16) {
    let caller = metaspace.class_of(frame.method()).to_string();
    let (named, field) = {
        let cf = metaspace.get(&caller).expect("caller class is loaded");
        let (c, n, _d) = cf.fieldref_target(cp_index).expect("putstatic: bad FieldRef");
        (c.to_string(), n.to_string())
    };
    check_access(metaspace, &caller, &named);
    let at = static_slot(metaspace, heap, &named, &field);
    // The value's type decides the width (a `long`/`double` is 8 bytes); floats and
    // ints store their 4-byte bit pattern.
    match frame.pop() {
        Value::Long(v) => heap.write_u64(at, v as u64),
        Value::Double(v) => heap.write_u64(at, v.to_bits()),
        Value::Float(v) => heap.write_u32(at, v.to_bits()),
        Value::Int(v) => heap.write_u32(at, v as u32),
        Value::Reference(r) => heap.write_u32(at, r as u32),
    }
}

/// Reads a static **reference** field by name from `class`'s mirror, returning the heap
/// offset it holds. The class must already be **initialized** (its `<clinit>` run), or the
/// field still holds its default (`0`/null). For natives that hand back a constant an
/// enum's `<clinit>` created — e.g. a `java.lang.Thread$State` value from `getState()`.
pub fn static_reference(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    class: &str,
    field: &str,
) -> usize {
    let at = static_slot(metaspace, heap, class, field);
    heap.read_u32(at) as usize
}

/// Resolves a static field to its **absolute heap offset** in the declaring class's
/// mirror. Finds the class that declares the field (statics are reached through the
/// declaring class, possibly a superclass of the named one), makes sure its mirror
/// exists (`load_class`), then offsets past the header by the field's index among
/// that class's own static fields. Each class keeps its own statics — no flattening.
pub(crate) fn static_slot(metaspace: &mut MetaspaceService, heap: &mut HeapService, named_class: &str, field: &str) -> usize {
    let declaring = static_declaring_class(metaspace, named_class, field)
        .expect("getstatic/putstatic: static field not found in the class or its superclasses");

    // Ensure the declaring class's mirror is allocated (Preparation), then locate it.
    load_class(metaspace, heap, &declaring);
    let uuid = metaspace.class_id(&declaring).to_string();
    let mirror = metaspace
        .class_object(&uuid)
        .expect("static_slot: mirror must exist after load_class");

    // Slots occupied by the declaring class's own static fields *before* `field` —
    // width-aware, so a `long`/`double` static counts as two slots.
    let slots = metaspace
        .get(&declaring)
        .map(|cf| {
            let mut acc = 0;
            for f in cf.fields.iter().filter(|f| f.is_static()) {
                let (start, next) = objects_operations::place_field(acc, cf.utf8(f.descriptor_index).unwrap_or(""));
                if cf.utf8(f.name_index) == Some(field) {
                    return start;
                }
                acc = next;
            }
            panic!("static_slot: field vanished from its declaring class");
        })
        .expect("static_slot: declaring class not loaded");

    mirror + HEADER_SIZE + slots * SLOT_SIZE
}

/// The **machine address** of the primitive static named by `cp_index` in `caller`'s constant pool,
/// and **which kind it is** — what the JIT bakes into a compiled `getstatic`/`putstatic` (see
/// [`burst::compile`][crate::burst::compile]).
///
/// Read-only in every sense, and that is the whole design: it takes `&MetaspaceService` and
/// `&HeapService`, so it cannot load a class, mint a Class ID, allocate a mirror or run a
/// `<clinit>` — all of which [`static_slot`] happily does. Compilation must not have side effects
/// on the VM's state, and the type signature is what enforces it rather than a comment.
///
/// `None` — "do not compile this method" — unless **all five** hold:
///
///  1. the constant-pool entry is a `FieldRef` whose descriptor is one this tier has a
///     representation for — `I`, `F`, `J`, `D`, or (group 2) a **reference**, `L…;` or `[…`. That
///     is not a formality: it fixes the **width** of the access, and they are different
///     instructions. `I`, `F` and a reference are 4-byte slots (sign-extending, zero-extending and
///     zero-extending respectively — a heap offset is unsigned and the interpreter's own
///     `getstatic` reads one with `read_u32(at) as usize`); `J` and `D` are 8, read and written
///     whole, and it is `place_field` that has already aligned them to an even slot pair so the 8
///     bytes are contiguous and start where this address says.
///
///     A reference static is answered for so that a compiled **`getstatic`** can read one. Writing
///     it stays out — a reference store owes the collector's write barrier — but that refusal is
///     the compiler's (`burst::compile::writable`), not this resolver's, so the read is not lost
///     with the write. Note that the *slot* is what is pinned here, not what it holds: the value is
///     loaded at run time, so a young object the collector later moves is no more a problem for
///     compiled code than it is for the interpreter;
///  2. some class in the named class's superclass chain — all of them already **loaded** — declares
///     it `static`;
///  3. that declaring class is **initialised** (`InitState::Done`). Compiled code cannot trigger a
///     `<clinit>`, so a static whose class has not run one yet would be read at its default value.
///     A method refused for this reason is refused permanently, which is the deliberate cost noted
///     in `burst::compile`'s docs;
///  4. its `Class<…>` mirror has been prepared, i.e. the mirror offset exists;
///  5. the mirror is somewhere [`HeapService::address_of`] will vouch for — outside Eden and inside
///     the byte region. It always is: `load_class` allocates mirrors with `malloc_old` and
///     `gc::compact` keeps them in its **pinned** set, so the offset never moves; the byte region
///     is pre-reserved to the max heap and never reallocates, so the address never moves either.
///     Those two facts together are what make baking the address in sound at all.
pub fn jit_static_field(
    metaspace: &MetaspaceService,
    heap: &HeapService,
    caller: &str,
    cp_index: u16,
) -> Option<(usize, crate::burst::compile::Kind)> {
    let (named, field, descriptor) = metaspace.get(caller)?.fieldref_target(cp_index)?;
    // The descriptor decides both halves of the answer: which kind the compiler's type map carries,
    // and how many bytes the emitted access touches. `place_field` gives a `J` two slots and starts
    // it on an even one, so its eight bytes are the pair beginning at the address computed below.
    let (kind, width) = match descriptor {
        "I" => (crate::burst::compile::Kind::Int, SLOT_SIZE),
        "F" => (crate::burst::compile::Kind::Float, SLOT_SIZE),
        "J" => (crate::burst::compile::Kind::Long, 2 * SLOT_SIZE),
        "D" => (crate::burst::compile::Kind::Double, 2 * SLOT_SIZE),
        // A reference static: one 4-byte slot holding a heap **offset**, exactly as `putstatic`
        // writes it (`heap.write_u32(at, r as u32)`). Every other descriptor — `B`, `C`, `S`, `Z`
        // — is refused, not because it could not be read but because `place_field`/`write_u32`
        // give it a width this pair has never agreed on.
        d if d.starts_with('L') || d.starts_with('[') => (crate::burst::compile::Kind::Reference, SLOT_SIZE),
        _ => return None,
    };
    let declaring = static_declaring_class_read(metaspace, named, field)?;
    if metaspace.init_state(&declaring) != crate::jvm::interpreter::metaspace::InitState::Done {
        return None;
    }
    let mirror = metaspace.class_object(metaspace.class_id_read(&declaring)?)?;

    // The field's byte offset inside the mirror: past the header, then the slots its declaring
    // class's own static fields occupy before it — the same width-aware walk `static_slot` does,
    // because the two must agree to the byte or compiled code reads the wrong static.
    let class = metaspace.get(&declaring)?;
    let mut acc = 0;
    let mut start = None;
    for f in class.fields.iter().filter(|f| f.is_static()) {
        let (at, next) = objects_operations::place_field(acc, class.utf8(f.descriptor_index).unwrap_or(""));
        if class.utf8(f.name_index) == Some(field) {
            start = Some(at);
            break;
        }
        acc = next;
    }
    Some((heap.address_of(mirror + HEADER_SIZE + start? * SLOT_SIZE, width)?, kind))
}

/// The **heap offset of the `Class<…>` mirror** of the class named at `cp_index` in `caller`'s
/// pool — what the JIT bakes into a `checkcast`, an `instanceof` and a class-literal `ldc`.
///
/// It is exactly the word [`objects_operations::allocate`] writes into an object's header
/// `class_id`, which is the whole point: the compiled guard compares the header against this
/// immediate, so the two sides are the same quantity resolved by the same lookup rather than two
/// derivations that could drift.
///
/// Read-only in every sense, like [`jit_static_field`]: `&MetaspaceService` cannot mint a Class ID,
/// load a class or allocate a mirror. So the answer is `None` for a class the interpreter has never
/// **prepared** — which refuses the method rather than preparing it, because a compilation that
/// allocates is a compilation that can collect. It also means an array class (`[I`, `[LFoo;`) is
/// answered for only once some array of it has actually been created, since that is when
/// `array_class_mirror` mints one.
///
/// **Why baking the offset in is sound**, and it is one fact rather than a hope: a mirror is
/// allocated with `malloc_old` and is in the **pinned** set `gc::compact` refuses to relocate
/// (`class_object_offsets`), so its offset is fixed for the life of the VM. It is the same fact
/// that lets `getstatic` bake in a static's address, and it is exactly the fact an interned
/// `String` does *not* have.
///
/// **Not [`jit_instance`][objects_operations::jit_instance]**, which answers the same `class_id`
/// but additionally demands the class be *initialised*: `new` is a first active use and a
/// `checkcast` is not, so requiring it here would refuse methods for a reason the JVMS has not got.
pub fn jit_class_mirror(metaspace: &MetaspaceService, caller: &str, cp_index: u16) -> Option<u32> {
    let name = metaspace.get(caller)?.class_name(cp_index)?;
    let uuid = metaspace.class_id_read(name)?;
    u32::try_from(metaspace.class_object(uuid)?).ok()
}

/// [`static_declaring_class`] without the loading — `None` where the other would have pulled a
/// superclass off the classpath. The read-only half of the pair, for [`jit_static_field`].
fn static_declaring_class_read(metaspace: &MetaspaceService, start: &str, field: &str) -> Option<String> {
    let mut current = Some(start.to_string());
    while let Some(class_name) = current.take() {
        let class = metaspace.get(&class_name)?;
        if class.fields.iter().filter(|f| f.is_static()).any(|f| class.utf8(f.name_index) == Some(field)) {
            return Some(class_name);
        }
        current = class.class_name(class.super_class).map(|s| s.to_string());
    }
    None
}

/// Finds the class that declares the *static* field `field`, walking up from `start`
/// through its superclasses (a `FieldRef` may name a subclass that inherits it).
fn static_declaring_class(metaspace: &mut MetaspaceService, start: &str, field: &str) -> Option<String> {
    let mut current = Some(start.to_string());
    while let Some(class_name) = current.take() {
        let (declares, super_name) = match metaspace.get_or_load(&class_name) {
            Some(cf) => (
                cf.fields
                    .iter()
                    .filter(|f| f.is_static())
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

/// `instanceof` (0xc1): pop a reference and push `1` if it's a **non-null** instance
/// of the class/interface at `cp_index` (a subtype of it), else `0`. `null` → `0`.
pub fn instanceof(metaspace: &mut MetaspaceService, heap: &HeapService, frame: &mut Frame, cp_index: u16) {
    let target = target_class(metaspace, frame, cp_index);
    let objref = match frame.pop() {
        Value::Reference(offset) => offset,
        _ => panic!("instanceof: expected an object reference"),
    };
    let result = if objref == 0 {
        0 // null is never an instance of anything
    } else {
        let runtime = runtime_class(metaspace, heap, objref);
        i32::from(is_subtype(metaspace, &runtime, &target))
    };
    frame.push(Value::Int(result));
}

/// `checkcast` (0xc0): if the reference is `null` or a subtype of the class at
/// `cp_index`, leave it on the stack unchanged; otherwise the cast fails with a
/// `ClassCastException` (returned as `Err` for the dispatch loop to throw).
pub fn checkcast(
    metaspace: &mut MetaspaceService,
    heap: &HeapService,
    frame: &mut Frame,
    cp_index: u16,
) -> Result<(), &'static str> {
    let target = target_class(metaspace, frame, cp_index);
    let objref = match frame.pop() {
        Value::Reference(offset) => offset,
        _ => panic!("checkcast: expected an object reference"),
    };
    if objref != 0 {
        let runtime = runtime_class(metaspace, heap, objref);
        if !is_subtype(metaspace, &runtime, &target) {
            return Err("java/lang/ClassCastException");
        }
    }
    frame.push(Value::Reference(objref)); // null or a valid cast: the ref stays
    Ok(())
}

/// Whether class `sub` is a subtype of `target`: the same class, a subclass (walking
/// the superclass chain), an implementer of `target` as an interface (directly or
/// transitively), or — for array types — by **array covariance**. The kernel of
/// `instanceof`/`checkcast`, of matching a thrown exception against a `catch` type,
/// and of the verifier's assignability.
pub fn is_subtype(metaspace: &mut MetaspaceService, sub: &str, target: &str) -> bool {
    if sub == target {
        return true;
    }

    // Array types: `sub` is `[…`. Every array is an `Object` (and `Cloneable`,
    // `Serializable`); and `[X ⊑ [Y` iff the components are subtypes — *covariance*.
    if let Some(sub_component) = sub.strip_prefix('[') {
        if matches!(
            target,
            "java/lang/Object" | "java/lang/Cloneable" | "java/io/Serializable"
        ) {
            return true;
        }
        return match target.strip_prefix('[') {
            Some(target_component) => match (component(sub_component), component(target_component)) {
                // Reference (or nested-array) components recurse through subtyping…
                (Some(s), Some(t)) => is_subtype(metaspace, &s, &t),
                // …primitive components must be identical (no subtyping among them).
                _ => sub_component == target_component,
            },
            None => false, // an array is not a subtype of any non-array class
        };
    }

    // Pull the direct interfaces and the superclass out under one borrow.
    let (interfaces, super_name) = match metaspace.get_or_load(sub) {
        Some(cf) => (
            cf.interfaces
                .iter()
                .filter_map(|&idx| cf.class_name(idx).map(str::to_string))
                .collect::<Vec<_>>(),
            cf.class_name(cf.super_class).map(str::to_string),
        ),
        None => return false, // can't load (e.g. Object's super) → chain ends
    };
    for iface in &interfaces {
        if is_subtype(metaspace, iface, target) {
            return true;
        }
    }
    match super_name {
        Some(s) => is_subtype(metaspace, &s, target),
        None => false,
    }
}

/// The "class name" of one array component descriptor, for subtype recursion: a
/// reference component `L<name>;` → `<name>`, a nested array `[…` → itself; a
/// primitive (`I`, `J`, …) → `None` (primitives have no subtyping).
fn component(descriptor: &str) -> Option<String> {
    if let Some(name) = descriptor.strip_prefix('L') {
        Some(name.trim_end_matches(';').to_string())
    } else if descriptor.starts_with('[') {
        Some(descriptor.to_string())
    } else {
        None
    }
}

/// Resolves the `Class` constant at `cp_index` (in the current method's pool) to its
/// binary name — the target type of an `instanceof`/`checkcast`.
fn target_class(metaspace: &MetaspaceService, frame: &Frame, cp_index: u16) -> String {
    let caller = metaspace.class_of(frame.method());
    metaspace
        .get(caller)
        .and_then(|cf| cf.class_name(cp_index))
        .expect("instanceof/checkcast: cp_index does not point to a Class constant")
        .to_string()
}

/// The runtime class name of the object at heap `offset`, via the `class_id` in its
/// header (the mirror offset) → the mirror index.
fn runtime_class(metaspace: &MetaspaceService, heap: &HeapService, offset: usize) -> String {
    let mirror = heap.read_u32(offset) as usize;
    metaspace
        .class_name_at_mirror(mirror)
        .expect("could not resolve the object's class from its header")
        .to_string()
}
