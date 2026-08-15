//! The heap — the JVM's region for dynamically-allocated objects and arrays
//! (JVMS §2.5.3). Modelled the faithful way: a flat, growable **byte arena**.
//! Objects will be laid out into these raw bytes, a reference will be a byte
//! offset into them, and a bump cursor will allocate by advancing through them.
//!
//! "Montículo" here is a *heap of memory* — an unstructured pile of bytes with no
//! imposed shape — not the binary-heap data structure (they only share the name).
//!
//! This is the base: the byte region and its sizing. The allocator (a bump cursor
//! + `alloc`) and the object layout come on top of it next.

use super::eden_arena::EdenArena;
use std::collections::HashSet;
use std::sync::atomic::Ordering;
use std::sync::Mutex;

/// Initial size of the heap's byte region, in bytes. Arbitrary — the region
/// grows (or shrinks) on demand via [`HeapService::resize`], but never past [`DEFAULT_MAX_HEAP`].
const DEFAULT_SIZE: usize = 1024;

/// **Maximum** heap byte-region size (`JVM_GC_MAX_HEAP`, default 16 MiB). The backing `Vec` is
/// **pre-reserved to this capacity** at startup, so it never reallocates while growing (`Vec`
/// only reallocates when `len` would exceed `capacity`). That keeps every byte's **address
/// stable** for the VM's life — the invariant the H3 W2 TLABs rely on (a raw pointer into Eden
/// must stay valid across an unrelated `Old` growth). Growing past it is a controlled
/// "heap exhausted" panic, never undefined behaviour.
const DEFAULT_MAX_HEAP: usize = 16 * 1024 * 1024;

/// Bytes reserved at offset 0, never handed out — the **null page**. A reference
/// is a heap offset and `null` is offset `0`, so offset `0` must not name a real
/// object (else an object at 0 would be indistinguishable from `null`, and
/// `getClass()` on it — which returns its class_id, the mirror offset — would look
/// null). The first real allocation starts at `NULL_PAGE`. One header's worth keeps
/// the layout aligned.
const NULL_PAGE: usize = 8;

/// Which **generation** an object belongs to. New objects are born `Young` (in Eden);
/// once they survive enough minor collections they are *tenured* to `Old`. The split
/// is what makes collection generational — `Young` is collected often and cheaply,
/// `Old` rarely. (Phase 1 records it; the copying minor collector that acts on it
/// comes next.)
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum Gen {
    Young,
    Old,
}

/// A region of the arena, by address. The young generation is split into **Eden**
/// (where objects are born) and two **survivor** spaces (`S0`/`S1`, the copy
/// collector's from/to halves); **Old** is the tenured region above them.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum Region {
    Eden,
    Survivor0,
    Survivor1,
    Old,
}

/// A live allocation the heap is tracking: where its block starts, how big it is, and
/// its generation + **age** (the number of minor collections it has survived — the
/// tenuring counter). The size lets the GC sweep know a block's *extent*; the gen/age
/// drive promotion.
#[derive(Clone, Copy)]
pub struct Allocation {
    pub offset: usize,
    pub size: usize,
    pub gen: Gen,
    pub age: u8,
}

/// Default byte size of **Eden** — where new objects are allocated. Env:
/// `JVM_GC_EDEN_SIZE`.
const DEFAULT_EDEN_SIZE: usize = 256;

/// Default byte size of **each** survivor space (`S0`, `S1`). Env:
/// `JVM_GC_SURVIVOR_SIZE`.
const DEFAULT_SURVIVOR_SIZE: usize = 64;

/// A reclaimed, currently-free run of bytes — a hole left by a swept object,
/// available for reuse by a later `malloc`. The free list is the set of these.
#[derive(Clone, Copy)]
struct FreeBlock {
    offset: usize,
    size: usize,
}

/// The heap: a flat block of bytes, with no structure imposed on it, into which
/// objects and arrays are allocated.
pub struct HeapService {
    /// The raw byte region for survivors and Old. Objects live here as bytes; a reference is an
    /// offset into this vector. **Eden** is served separately by [`Self::eden`]; its byte range
    /// `[NULL_PAGE, eden_end)` in this vector is unused (H3 W2c).
    memory: Vec<u8>,
    /// **Eden** — a lock-free bump arena (H3 W2c). New objects allocate here without the VM lock
    /// (`UnsafeCell` bytes, atomic cursor; Miri-verified in `eden_arena`). Addresses are Eden's
    /// absolute heap offsets minus `NULL_PAGE` (arena-local). A minor GC evacuates survivors and
    /// `reset`s it. Overflow falls back to Old.
    eden: EdenArena,
    /// Which survivor space (`0` = `S0`, `1` = `S1`) is the current **to-space** — the
    /// half a minor GC copies survivors *into*. The other is the **from-space** (part
    /// of the collection set). They swap roles after each minor.
    to_survivor: u8,
    /// Bump pointer within the current to-survivor space (reset on each swap).
    survivor_cursor: usize,
    /// **Old** bump pointer: next free byte in Old `[old_start, …)`, which grows the
    /// arena. Promotions and Old allocations (mirrors, Eden overflow) land here; the
    /// free list reclaims Old holes after a major collection.
    old_cursor: usize,
    /// Allocation log: a record per block handed out and not yet freed. The bump heap
    /// is untyped bytes with no object boundaries, so this is the GC's only view of
    /// "what's allocated". Freed/evacuated blocks leave the log.
    objects: Vec<Allocation>,
    /// Free list for the **Old** generation: holes reclaimed by the major sweep,
    /// reused first-fit by Old allocation. (Young is copy-collected — no free list.)
    free_list: Vec<FreeBlock>,
    /// Byte size of Eden, and of each survivor space — fix the region boundaries.
    eden_size: usize,
    survivor_size: usize,
    /// **Remembered set**: the Old objects that hold a reference into the young
    /// generation, recorded by the write barrier ([`HeapService::record_reference_store`]). A
    /// minor GC scans just these for Old→young roots instead of all of Old — the
    /// generational shortcut. (Mirrors hold young statics too but are always scanned, so
    /// they stay out of this set.)
    remembered: HashSet<usize>,
    /// **Per-thread pending Eden log** (H3 W2b), indexed by thread slot. Eden `malloc`s record
    /// here — thread-local under the future lock-free allocation (W2c), so concurrent allocation
    /// doesn't contend on the shared `objects` log. Drained into `objects` by
    /// [`HeapService::commit_pending`] at every GC entry (so the collector sees them) — the GC's
    /// own bookkeeping is untouched. `Old` allocations (rare: mirrors) still log straight to
    /// `objects`.
    pending: Vec<Mutex<Vec<Allocation>>>,
    /// The thread slot whose Eden `malloc`s land in `pending[current_thread]`. Set by the driver
    /// on each context switch ([`activate`]); under the GIL/`.write()` model exactly one thread
    /// allocates at a time, so a shared index is correct until W2c makes it truly thread-local.
    current_thread: usize,
}

/// Byte offset of the **mark word** inside an object header `[class_id | mark]`.
/// The GC sets it during the mark phase; it's 0 (unmarked) the rest of the time.
const MARK_OFFSET: usize = 4;

/// Where the heap's bytes actually are, for a caller that must reach them without going through
/// an accessor — today exactly one: the JIT ([`HeapService::jit_bases`]).
///
/// Both bases are **biased**, i.e. each is the address that heap offset `0` *would* have in its
/// buffer, so `base + offset` is the address of any offset that buffer serves. That is what lets
/// compiled code do the whole translation in a compare and an `add`; it also means neither number
/// is a pointer to anything and neither should ever be dereferenced as one.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub struct JitBases {
    /// Biased base for offsets **below** [`eden_end`][JitBases::eden_end] — Eden's arena.
    pub eden: usize,
    /// Biased base for every other offset — the survivor spaces and Old, in `memory`.
    pub other: usize,
    /// The first offset not served by Eden.
    pub eden_end: usize,
    /// One past the largest offset the heap can ever hand out (the pre-reserved capacity).
    pub max_offset: usize,
    /// Machine address of **Eden's bump cursor** — the word a compiled `new` reserves through, with
    /// a `lock xadd` that is the same operation [`EdenArena::alloc`]'s `fetch_add` is. Boxed at the
    /// arena, so this address outlives every move of the heap.
    pub eden_cursor: usize,
    /// Eden's capacity in bytes: the bound the arena checks a reservation against, and therefore
    /// the bound compiled code must check it against too.
    pub eden_capacity: usize,
    /// The **null page**: the heap offset of arena-local byte 0. A reservation at arena-local `l`
    /// is the heap offset `l + null_page`, which is the reference the program sees.
    pub null_page: usize,
}

impl HeapService {
    /// A heap sized to [`DEFAULT_SIZE`], zero-filled, with the cursor past the
    /// reserved null page (so no real allocation ever lands at offset 0 = `null`).
    /// The generational region sizes come from the environment (Eden / survivor),
    /// falling back to the defaults.
    pub fn new() -> Self {
        let eden_size = env_usize("JVM_GC_EDEN_SIZE", DEFAULT_EDEN_SIZE);
        let survivor_size = env_usize("JVM_GC_SURVIVOR_SIZE", DEFAULT_SURVIVOR_SIZE);
        // Old starts above Eden + both survivors; to-survivor is S0 to begin with.
        let old_start = NULL_PAGE + eden_size + 2 * survivor_size;
        // Pre-reserve the whole heap capacity so the backing `Vec` never reallocates as `Old`
        // grows — byte addresses stay stable for the VM's life (H3 W2a, the base TLABs need).
        let initial = DEFAULT_SIZE.max(old_start);
        let max_heap = env_usize("JVM_GC_MAX_HEAP", DEFAULT_MAX_HEAP).max(initial);
        let mut memory = Vec::with_capacity(max_heap);
        memory.resize(initial, 0);
        HeapService {
            memory,
            eden: EdenArena::new(eden_size),
            to_survivor: 0,
            survivor_cursor: NULL_PAGE + eden_size, // start of S0
            old_cursor: old_start,
            objects: Vec::new(),
            free_list: Vec::new(),
            eden_size,
            survivor_size,
            remembered: HashSet::new(),
            pending: vec![Mutex::new(Vec::new())], // thread 0 (main); grows as workers spawn
            current_thread: 0,
        }
    }

    /// Selects which thread's pending Eden log subsequent `malloc`s record into. Called by the
    /// driver on each context switch. Grows the per-thread log vector to cover new thread slots.
    pub fn set_alloc_thread(&mut self, idx: usize) {
        if idx >= self.pending.len() {
            self.pending.resize_with(idx + 1, || Mutex::new(Vec::new()));
        }
        self.current_thread = idx;
    }

    /// Ensures a pending-log slot exists for thread `idx` (called before a thread starts
    /// allocating lock-free, since `set_alloc_thread`'s growth happens under the lock).
    pub fn ensure_alloc_slot(&mut self, idx: usize) {
        if idx >= self.pending.len() {
            self.pending.resize_with(idx + 1, || Mutex::new(Vec::new()));
        }
    }

    /// Drains every thread's pending Eden log into the shared `objects` log. Called at each GC
    /// entry (via [`Self::parked`]) so the collector's wholesale view is complete before it runs.
    pub fn commit_pending(&mut self) {
        for i in 0..self.pending.len() {
            let mut drained = std::mem::take(&mut *self.pending[i].lock().unwrap());
            self.objects.append(&mut drained);
        }
    }

    // --- write barrier / remembered set -----------------------------------------

    /// The **one gateway** for storing a reference into a field or array slot: writes
    /// the pointer *and* runs the write barrier, atomically. Every reference store goes
    /// through here, so the barrier can't be bypassed or forgotten — which is the whole
    /// point of the heap having a single owner.
    pub fn store_reference(&mut self, holder: usize, slot: usize, value: usize) {
        self.write_u32(slot, value as u32);
        self.record_reference_store(holder, value);
    }

    /// The **write barrier** (private — reached only via [`HeapService::store_reference`]):
    /// if an **Old** object comes to hold a **young** pointer, it's recorded in the
    /// remembered set so the next minor GC treats it as a root. Without this, a young
    /// object reachable only from Old would be wrongly collected.
    fn record_reference_store(&mut self, holder: usize, value: usize) {
        if value != 0 && self.gen_of(holder) == Gen::Old && self.gen_of(value) == Gen::Young {
            self.remembered.insert(holder);
        }
    }

    /// The remembered Old holders (snapshot), for the minor collector to scan as roots.
    pub fn remembered(&self) -> Vec<usize> {
        self.remembered.iter().copied().collect()
    }

    /// Replaces the remembered set — the minor collector rebuilds it after a collection
    /// (a holder stays remembered only if it still points into the young generation),
    /// and a major collection recomputes it from scratch.
    pub fn set_remembered(&mut self, set: HashSet<usize>) {
        self.remembered = set;
    }

    // --- generational region boundaries (by address) ----------------------------

    /// Where Eden ends and the first survivor space begins.
    fn eden_end(&self) -> usize {
        NULL_PAGE + self.eden_size
    }

    /// The first byte of the **Old** generation — above Eden and the two survivors.
    /// Everything at or above this offset is tenured; everything below is young.
    pub fn old_start(&self) -> usize {
        self.eden_end() + 2 * self.survivor_size
    }

    /// The generation an offset falls in, by address: below [`HeapService::old_start`] is
    /// `Young`, at or above is `Old`.
    pub fn gen_of(&self, offset: usize) -> Gen {
        if offset < self.old_start() {
            Gen::Young
        } else {
            Gen::Old
        }
    }

    /// The arena region an offset falls in (Eden / `S0` / `S1` / Old) — for the
    /// visualizer and the collector.
    pub fn region_of(&self, offset: usize) -> Region {
        let s0 = self.eden_end();
        let s1 = s0 + self.survivor_size;
        if offset < s0 {
            Region::Eden
        } else if offset < s1 {
            Region::Survivor0
        } else if offset < self.old_start() {
            Region::Survivor1
        } else {
            Region::Old
        }
    }

    /// The current size of the byte region, in bytes.
    pub fn size(&self) -> usize {
        self.memory.len()
    }

    /// The **machine address** of heap offset `offset`, for the one consumer that needs a raw
    /// pointer rather than an offset: the JIT, which bakes the address of an `int` static into an
    /// instruction stream (`burst::compile`'s `getstatic`).
    ///
    /// `None` unless the whole `size`-byte span lies in `memory` **outside Eden**, and that
    /// exclusion is the point rather than an omission. Eden's bytes live in a different buffer
    /// ([`Self::eden`]) *and* Eden is evacuated by every minor collection, so no address in it can
    /// be baked into anything. The intended caller only ever asks about a `Class<…>` mirror, which
    /// `malloc_old` puts in Old and `gc::compact` pins there.
    ///
    /// The address is stable for the life of the VM, and so is the bounds check, for two separate
    /// reasons that are both worth naming because a caller bakes the answer into machine code and
    /// never asks again:
    ///
    ///  - `memory` is **pre-reserved to the maximum heap** at startup and therefore never
    ///    reallocates (see [`DEFAULT_MAX_HEAP`]), so `as_ptr()` is a constant;
    ///  - the only caller of [`Self::resize`] is `malloc_old`'s growth path, so `memory.len()` is
    ///    **monotonically non-decreasing**. A major collection moves `old_cursor` down but never
    ///    shrinks the region ([`Self::reset_after_compaction`]), so an offset that was in bounds
    ///    once stays in bounds.
    ///
    /// What this function does *not* and cannot promise is that the object at `offset` stays alive
    /// or stays put — that is the caller's to establish, and for pinned mirrors it is
    /// `gc::compact`'s pinned set.
    pub fn address_of(&self, offset: usize, size: usize) -> Option<usize> {
        let end = offset.checked_add(size)?;
        if offset < self.eden_end() || end > self.memory.len() {
            return None;
        }
        Some(self.memory.as_ptr() as usize + offset)
    }

    /// The two **bases** that turn a heap *offset* into a machine address, plus where the boundary
    /// between them is and how far the heap can ever reach — everything the JIT needs to emit a
    /// field or array read without calling back into the VM.
    ///
    /// It is two bases and not one because the heap is two buffers: Eden's bytes live in
    /// [`Self::eden`] (a fixed-size arena) and everything else in [`Self::memory`] (a `Vec`
    /// pre-reserved to the maximum heap). Both are **biased** by construction, so the compiled
    /// sequence is a comparison and one `add` — see `burst::compile`'s `heap_address`.
    ///
    /// Every number here is stable for the VM's life, and the reasons are the same two that make
    /// [`Self::address_of`] safe to bake in, now applied to the base rather than to one offset:
    /// neither buffer ever reallocates (the `Vec` is pre-reserved past its maximum, the arena is
    /// fixed at construction), and the region boundaries are set once from the environment.
    /// **Liveness is still not promised** — that a given offset names a live object is the caller's
    /// to establish, and for compiled code it is the fact that no collection can run while native
    /// code is on the stack.
    pub fn jit_bases(&self) -> JitBases {
        JitBases {
            // The arena is addressed from its own byte 0, which is heap offset `NULL_PAGE`; biasing
            // the base by that is what lets compiled code add the *heap* offset unmodified.
            eden: self.eden.base_address().wrapping_sub(NULL_PAGE),
            other: self.memory.as_ptr() as usize,
            eden_end: self.eden_end(),
            // The `Vec`'s capacity is the maximum the region can ever grow to (`resize` panics past
            // it), so no offset it hands out can reach this. Old is the highest region, so this
            // bounds every one of them.
            max_offset: self.memory.capacity(),
            eden_cursor: self.eden.cursor_address(),
            eden_capacity: self.eden.capacity(),
            null_page: NULL_PAGE,
        }
    }

    /// Records an object **compiled code has already allocated in Eden** in this thread's pending
    /// log — the one part of an allocation native code cannot do for itself.
    ///
    /// An Eden allocation is four things: reserve the bytes (an atomic bump), zero them, write the
    /// `class_id` header, and *log the object* so the collector can find it. Compiled code does the
    /// first three inline; the fourth is a `Mutex<Vec<Allocation>>` push, which is not something an
    /// instruction stream can do. So it is **deferred**: the compiled `new` writes `(offset, size)`
    /// into a flat array in the caller's buffer and the JIT trampoline replays it through here the
    /// instant native code returns.
    ///
    /// Deferring it is sound for the same single reason everything else in this tier is: the log is
    /// drained only by [`Self::commit_pending`], which is called only from the interpreter's GC
    /// entry (`Exec::parked`), and **no collection can run while native code is on this thread's
    /// stack**. So there is no moment at which a collector could look at the heap and not see one
    /// of these objects — the window between the bump and this call contains no GC by construction.
    ///
    /// `size` must be the object's *logical* size (header plus fields), not the 8-byte-rounded
    /// stride the arena bumps by: that is what the interpreter's own `malloc` logs, and the minor
    /// collector copies exactly `size` bytes when it evacuates.
    pub fn log_jit_allocation(&self, offset: usize, size: usize) {
        self.pending[self.current_thread]
            .lock()
            .unwrap()
            .push(Allocation { offset, size, gen: Gen::Young, age: 0 });
    }

    /// Bytes handed out so far across all three regions (Eden + the live survivor +
    /// Old) — the heap's occupancy, for the GC triggers and the visualizer.
    pub fn used(&self) -> usize {
        let eden = self.eden.used();
        let survivor = self.survivor_cursor - self.to_survivor_start();
        let old = self.old_cursor - self.old_start();
        eden + survivor + old
    }

    /// Whether `offset` lands in Eden — and if so, its **arena-local** address (offset minus
    /// `NULL_PAGE`). Eden bytes live in [`Self::eden`]; everything else in `memory`. Every heap
    /// byte accessor routes through this (H3 W2c).
    fn in_eden(&self, offset: usize) -> Option<usize> {
        if (NULL_PAGE..self.eden_end()).contains(&offset) {
            Some(offset - NULL_PAGE)
        } else {
            None
        }
    }

    /// The whole occupied arena `memory[..old_cursor]` — for tooling/inspection. Old
    /// is the highest region, so its cursor bounds everything (Eden/survivors sit
    /// below `old_start`, possibly with gaps).
    pub fn bytes(&self) -> &[u8] {
        &self.memory[..self.old_cursor]
    }

    // --- generational allocation cursors ----------------------------------------

    /// The start offset of the current to-survivor space.
    fn to_survivor_start(&self) -> usize {
        self.eden_end() + self.to_survivor as usize * self.survivor_size
    }

    /// The region (`S0`/`S1`) that is the current **from-space** — the survivor half a
    /// minor GC evacuates *out of* (the other half being the to-space).
    pub fn from_survivor_region(&self) -> Region {
        if self.to_survivor == 0 {
            Region::Survivor1
        } else {
            Region::Survivor0
        }
    }

    /// Whether an offset is in the minor GC's **collection set** — Eden or the current
    /// from-survivor. These are the objects a minor collection evacuates; everything
    /// else (the to-survivor and Old) is left in place.
    pub fn in_collection_set(&self, offset: usize) -> bool {
        let r = self.region_of(offset);
        r == Region::Eden || r == self.from_survivor_region()
    }

    /// Eden's high-water usage in bytes — what the safepoint checks to decide a minor
    /// collection is due.
    pub fn eden_used(&self) -> usize {
        self.eden.used()
    }

    /// Eden's capacity in bytes.
    pub fn eden_capacity(&self) -> usize {
        self.eden_size
    }

    /// Grows or shrinks the region to `new_size` bytes — zero-filling the new space when
    /// growing, dropping the tail when shrinking. **Never reallocates**: the capacity was
    /// pre-reserved to the max heap (H3 W2a), so `new_size` stays `<= capacity` and the buffer's
    /// address is stable. Exceeding it is a controlled "heap exhausted" panic, not a realloc.
    pub fn resize(&mut self, new_size: usize) {
        assert!(
            new_size <= self.memory.capacity(),
            "heap exhausted: needed {new_size} B but JVM_GC_MAX_HEAP caps the region at {} B",
            self.memory.capacity()
        );
        self.memory.resize(new_size, 0);
    }

    /// Allocates `n` bytes for a **new** object and returns its start offset. New
    /// objects are born in **Eden** (a pure bump — no free list); if Eden is full,
    /// they overflow to **Old**. Logged so the GC can enumerate them.
    pub fn malloc(&mut self, n: usize) -> usize {
        // Bump the lock-free Eden arena (W2c); its `alloc` reserves + zeroes the bytes. The
        // arena-local address maps to the absolute heap offset by adding `NULL_PAGE`.
        if let Some(local) = self.eden.alloc(n) {
            let offset = local + NULL_PAGE;
            // Record in this thread's pending log (W2b) — committed to `objects` at the next GC.
            self.pending[self.current_thread]
                .lock()
                .unwrap()
                .push(Allocation { offset, size: n, gen: Gen::Young, age: 0 });
            offset
        } else {
            self.malloc_old(n) // Eden full → Old (until a minor frees Eden)
        }
    }

    /// **Lock-free** object allocation for the W2c `.read()` path: bumps the Eden arena, writes the
    /// class-id header, and logs into thread `idx`'s pending slot — all through `&self`, so many
    /// threads allocate concurrently. `None` if Eden is full (the caller escalates to the locked
    /// Old path). `idx`'s pending slot must already exist (`ensure_alloc_slot`).
    ///
    /// Soundness: `eden.alloc` reserves `[local, local+size)` exclusively for this call, and the
    /// header lands only in that fresh, *unpublished* region — no other thread reads or writes it
    /// until we return and the reference is pushed. The arena's lock-free writes are Miri-verified.
    pub fn alloc_object_lockfree(&self, size: usize, class_id: u32, idx: usize) -> Option<usize> {
        let local = self.eden.alloc(size)?;
        // SAFETY: `[local, local+4)` is within our fresh, exclusive reservation — no aliasing.
        unsafe { self.eden.write_u32(local, class_id) };
        let offset = local + NULL_PAGE;
        self.pending[idx]
            .lock()
            .unwrap()
            .push(Allocation { offset, size, gen: Gen::Young, age: 0 });
        Some(offset)
    }

    /// **Lock-free** array allocation for the W2c `.read()` path — like [`Self::alloc_object_lockfree`]
    /// but also writes the array **length** field, which sits at `HEADER_SIZE` (8), right after the
    /// `[class_id | mark]` header.
    pub fn alloc_array_lockfree(&self, size: usize, class_id: u32, length: u32, idx: usize) -> Option<usize> {
        let local = self.eden.alloc(size)?;
        // SAFETY: `[local, local+size)` is our fresh, exclusive reservation — no aliasing.
        unsafe {
            self.eden.write_u32(local, class_id); // header class_id (mark stays 0)
            self.eden.write_u32(local + MARK_OFFSET + 4, length); // length at HEADER_SIZE (= 8)
        }
        let offset = local + NULL_PAGE;
        self.pending[idx]
            .lock()
            .unwrap()
            .push(Allocation { offset, size, gen: Gen::Young, age: 0 });
        Some(offset)
    }

    /// Fallible [`Self::malloc`] for **bytecode** allocations (`new` / `newarray` /
    /// `anewarray` / `multianewarray`): same Eden-then-Old policy, but when the request
    /// fits neither Eden nor Old within the pre-reserved max heap it returns `None`
    /// instead of the "heap exhausted" panic — the opcode turns that into a catchable
    /// `java.lang.OutOfMemoryError` (JVMS §6.3). Internal VM allocations (interned
    /// strings, mirrors, promotions) keep the panicking `malloc`/`malloc_old` path.
    pub fn try_malloc(&mut self, n: usize) -> Option<usize> {
        if let Some(local) = self.eden.alloc(n) {
            let offset = local + NULL_PAGE;
            self.pending[self.current_thread]
                .lock()
                .unwrap()
                .push(Allocation { offset, size: n, gen: Gen::Young, age: 0 });
            return Some(offset);
        }
        if self.can_alloc_old(n) {
            Some(self.malloc_old(n))
        } else {
            None // truly exhausted: Old would have to grow past JVM_GC_MAX_HEAP
        }
    }

    /// Whether an **Old** allocation of `n` bytes can be satisfied without growing the
    /// region past its pre-reserved max capacity: a free-list hole big enough, or a
    /// bump that stays within `JVM_GC_MAX_HEAP`. Mirrors [`Self::bump_old`]'s two paths,
    /// so `can_alloc_old(n) == true` guarantees `malloc_old(n)` won't hit the
    /// "heap exhausted" panic in [`Self::resize`].
    pub fn can_alloc_old(&self, n: usize) -> bool {
        self.free_list.iter().any(|b| b.size >= n)
            || self.old_cursor.checked_add(n).is_some_and(|end| end <= self.memory.capacity())
    }

    /// Allocates `n` bytes directly in the **Old** generation and logs it as `Old`.
    /// Used for permanent objects (`Class<…>` mirrors) and Eden overflow — anything
    /// that should skip the young generation.
    pub fn malloc_old(&mut self, n: usize) -> usize {
        let offset = self.bump_old(n);
        self.objects.push(Allocation { offset, size: n, gen: Gen::Old, age: 0 });
        offset
    }

    /// Raw **Old** allocation — a first-fit free-list reuse or a bump of `old_cursor`
    /// (growing the arena) — **without** logging. The minor collector uses it for
    /// promotions and logs the moved object itself.
    fn bump_old(&mut self, n: usize) -> usize {
        if let Some(i) = self.free_list.iter().position(|b| b.size >= n) {
            let block = self.free_list.remove(i);
            if block.size > n {
                self.free_list.push(FreeBlock { offset: block.offset + n, size: block.size - n });
            }
            self.memory[block.offset..block.offset + n].fill(0);
            return block.offset;
        }
        let offset = self.old_cursor;
        self.old_cursor += n;
        if self.old_cursor > self.memory.len() {
            self.resize(self.old_cursor);
        }
        offset
    }

    /// Raw **to-survivor** allocation — a bump within the current to-space, or `None`
    /// if it's full (the minor collector then promotes the object to Old instead).
    /// Not logged; the collector rebuilds the object log.
    fn bump_survivor(&mut self, n: usize) -> Option<usize> {
        let end = self.to_survivor_start() + self.survivor_size;
        if self.survivor_cursor + n <= end {
            let offset = self.survivor_cursor;
            self.survivor_cursor += n;
            Some(offset)
        } else {
            None
        }
    }

    /// Copies a live young object's `size` bytes to a freshly-evacuated `dest`
    /// (survivor or Old) during a minor GC. Source and destination regions are
    /// disjoint, so this never clobbers.
    pub fn evacuate_block(&mut self, from: usize, dest: usize, size: usize) {
        // `dest` is always in survivor/Old (`memory`); `from` may be Eden (the arena) or Old.
        // A cross-buffer move (Eden → memory) copies byte by byte; an in-`memory` move uses the
        // fast `copy_within`.
        match self.in_eden(from) {
            Some(a) => {
                for i in 0..size {
                    self.memory[dest + i] = unsafe { self.eden.read_u8(a + i) };
                }
            }
            None => self.memory.copy_within(from..from + size, dest),
        }
    }

    /// Allocates `size` bytes for an **evacuated** survivor: in the to-survivor space
    /// if it fits, else promoted to Old. Returns `(dest, promoted)`. The minor
    /// collector calls this, then [`HeapService::evacuate_block`], then logs the new object.
    pub fn alloc_evacuation(&mut self, size: usize, promote: bool) -> (usize, bool) {
        if promote {
            return (self.bump_old(size), true);
        }
        match self.bump_survivor(size) {
            Some(dest) => (dest, false),
            None => (self.bump_old(size), true), // to-survivor full → promote
        }
    }

    /// Commits a finished **minor** collection: installs the new allocation log (Old
    /// objects, unchanged, plus the evacuated survivors/promotions), empties Eden, and
    /// swaps the survivor roles so the just-filled to-space becomes next cycle's
    /// from-space.
    pub fn reset_after_minor(&mut self, objects: Vec<Allocation>) {
        self.objects = objects;
        self.eden.reset(); // Eden is now empty — recycle the arena
        self.to_survivor = 1 - self.to_survivor; // swap from/to
        self.survivor_cursor = self.to_survivor_start(); // the new to-space is empty
    }

    /// Reclaims a block: drops it from the live set and returns its bytes to the
    /// free list (coalescing with adjacent free runs). This is what the GC **sweep**
    /// will call for each garbage object — once the mark phase is trustworthy (i.e.
    /// the transitive trace is in place; until then a roots-only mark would free
    /// objects that are live through another object's field).
    pub fn free(&mut self, offset: usize) {
        let Some(i) = self.objects.iter().position(|a| a.offset == offset) else {
            return; // not a tracked allocation (already freed, or never ours)
        };
        let size = self.objects.remove(i).size;
        self.free_list.push(FreeBlock { offset, size });
        self.coalesce();
    }

    /// Merges adjacent free blocks into single larger runs: sort by offset, then
    /// join any block that starts exactly where the previous one ends. Keeps
    /// fragmentation down so a later `malloc` can reuse a merged hole that the
    /// individual pieces would each have been too small for.
    fn coalesce(&mut self) {
        self.free_list.sort_by_key(|b| b.offset);
        let mut merged: Vec<FreeBlock> = Vec::with_capacity(self.free_list.len());
        for block in self.free_list.drain(..) {
            match merged.last_mut() {
                Some(prev) if prev.offset + prev.size == block.offset => prev.size += block.size,
                _ => merged.push(block),
            }
        }
        self.free_list = merged;
    }

    /// Every live allocation (start offset + size), in tracking order — the GC's
    /// view of "everything on the heap" (it has no other object table). The size
    /// is what the sweep needs to reclaim each block.
    pub fn allocations(&self) -> &[Allocation] {
        &self.objects
    }

    /// The lowest offset the **Old** generation can occupy — the major compactor packs
    /// relocated old objects from here up. (Young lives below it, copy-collected.)
    pub fn floor(&self) -> usize {
        self.old_start()
    }

    /// Moves a block's `size` bytes from `from` to `to` (overlap-safe). The GC
    /// compactor uses it to slide a live object down into the packed region; it
    /// doesn't touch the allocation log — the compactor commits the new layout with
    /// [`HeapService::reset_after_compaction`].
    pub fn relocate(&mut self, from: usize, to: usize, size: usize) {
        if from != to {
            self.memory.copy_within(from..from + size, to);
        }
    }

    /// Installs the layout after a **major** (Old) compaction: the new allocation log
    /// (young objects unchanged + the relocated old ones), the new Old high-water
    /// cursor, and an empty free list (compaction coalesces every Old hole into the
    /// single trailing free region above the cursor).
    pub fn reset_after_compaction(&mut self, objects: Vec<Allocation>, old_cursor: usize) {
        self.objects = objects;
        self.old_cursor = old_cursor;
        self.free_list.clear();
    }

    /// The current free list as `(offset, size)` holes, for tooling that wants to
    /// show reclaimed space.
    pub fn free_blocks(&self) -> Vec<(usize, usize)> {
        self.free_list.iter().map(|b| (b.offset, b.size)).collect()
    }

    /// Sets an object's **mark bit** (used by the GC's mark phase to flag it live).
    pub fn set_mark(&mut self, offset: usize) {
        self.write_u32(offset + MARK_OFFSET, 1);
    }

    /// Whether an object is currently marked (reachable, as of the last mark phase).
    pub fn is_marked(&self, offset: usize) -> bool {
        self.read_u32(offset + MARK_OFFSET) != 0
    }

    /// Clears the mark bit on every allocated object — the reset the mark phase runs
    /// first, so a fresh trace starts from a clean slate.
    pub fn clear_all_marks(&mut self) {
        for i in 0..self.objects.len() {
            let offset = self.objects[i].offset;
            self.write_u32(offset + MARK_OFFSET, 0);
        }
    }

    // Every byte accessor routes Eden offsets to the lock-free arena, everything else to `memory`
    // (H3 W2c). The `unsafe` arena calls are sound: while allocation is still serialized under the
    // VM lock, only one thread touches Eden at a time; the W2c concurrency step will re-justify
    // them (disjoint published-vs-fresh objects) with its own Miri-checked test.

    /// Writes a 32-bit value at `offset`, little-endian. The primitive every object
    /// field/header write goes through.
    pub fn write_u32(&mut self, offset: usize, value: u32) {
        match self.in_eden(offset) {
            Some(a) => unsafe { self.eden.write_u32(a, value) },
            None => self.memory[offset..offset + 4].copy_from_slice(&value.to_le_bytes()),
        }
    }

    /// Reads a 32-bit little-endian value at `offset` — the inverse of [`Self::write_u32`].
    pub fn read_u32(&self, offset: usize) -> u32 {
        match self.in_eden(offset) {
            Some(a) => unsafe { self.eden.read_u32(a) },
            None => u32::from_le_bytes(self.memory[offset..offset + 4].try_into().unwrap()),
        }
    }

    /// Bounds-checked [`Self::read_u32`] — `None` if `offset` isn't a readable 4-byte word (out of
    /// range in Old/survivor, or past Eden). For diagnostics over *untrusted* offsets (e.g. a
    /// possibly-dangling reference in [`super::gc::verify_heap`]), where a raw read could panic.
    pub fn try_read_u32(&self, offset: usize) -> Option<u32> {
        match self.in_eden(offset) {
            Some(a) if a + 4 <= self.eden.capacity() => Some(unsafe { self.eden.read_u32(a) }),
            Some(_) => None,
            None if offset + 4 <= self.memory.len() => {
                Some(u32::from_le_bytes(self.memory[offset..offset + 4].try_into().unwrap()))
            }
            None => None,
        }
    }

    /// Writes a 64-bit value at `offset`, little-endian — for **category-2** values
    /// (`long`/`double`), 8 bytes wide.
    pub fn write_u64(&mut self, offset: usize, value: u64) {
        match self.in_eden(offset) {
            Some(a) => unsafe { self.eden.write_u64(a, value) },
            None => self.memory[offset..offset + 8].copy_from_slice(&value.to_le_bytes()),
        }
    }

    /// Reads a 64-bit little-endian value at `offset` — the inverse of [`Self::write_u64`].
    pub fn read_u64(&self, offset: usize) -> u64 {
        match self.in_eden(offset) {
            Some(a) => unsafe { self.eden.read_u64(a) },
            None => u64::from_le_bytes(self.memory[offset..offset + 8].try_into().unwrap()),
        }
    }

    // ---- H4 relaxed-JMM field access ------------------------------------------------------------
    // `getfield`/`putfield` use these to read/write fields with explicit memory ordering. A
    // **volatile** read is `Acquire`, a volatile write `Release` — the publication guarantee. For
    // an **Eden** object the access is a lock-free atomic op (no VM lock); an **Old** object falls
    // back to plain byte access, which is sound because Old field access always holds the RwLock
    // (`.read()` for reads, `.write()` for writes), and that lock already provides happens-before.

    /// `Acquire` read of a `u32` field (volatile int/float/reference). Eden → atomic `Acquire`;
    /// Old → plain read (the read lock orders it).
    pub fn read_u32_acquire(&self, offset: usize) -> u32 {
        match self.in_eden(offset) {
            Some(a) => unsafe { self.eden.load_u32_ordered(a, Ordering::Acquire) },
            None => u32::from_le_bytes(self.memory[offset..offset + 4].try_into().unwrap()),
        }
    }

    /// `Acquire` read of a `u64` field (volatile long/double — 8-aligned, so a real `AtomicU64`).
    pub fn read_u64_acquire(&self, offset: usize) -> u64 {
        match self.in_eden(offset) {
            Some(a) => unsafe { self.eden.load_u64_ordered(a, Ordering::Acquire) },
            None => u64::from_le_bytes(self.memory[offset..offset + 8].try_into().unwrap()),
        }
    }

    /// Lock-free write of a `u32` field **if the object is in Eden**: `true` on success, `false`
    /// if it's in Old (the caller escalates to the locked write path). `order` is `Release` for a
    /// volatile field, `Relaxed` otherwise — takes `&self`, so many threads can write disjoint
    /// young objects' fields without serializing on the VM write lock.
    pub fn write_u32_eden(&self, offset: usize, value: u32, order: Ordering) -> bool {
        match self.in_eden(offset) {
            Some(a) => {
                unsafe { self.eden.store_u32_ordered(a, value, order) };
                true
            }
            None => false,
        }
    }

    /// Lock-free write of a `u64` field (long/double) if the object is in Eden (see
    /// [`Self::write_u32_eden`]). Fields are 8-aligned, so this is a real `AtomicU64`.
    pub fn write_u64_eden(&self, offset: usize, value: u64, order: Ordering) -> bool {
        match self.in_eden(offset) {
            Some(a) => {
                unsafe { self.eden.store_u64_ordered(a, value, order) };
                true
            }
            None => false,
        }
    }

    /// Lock-free **compare-and-set** of a `u32` field if the object is in Eden (H5): `Some(swapped)`
    /// with the atomic CAS done, `None` if it's in Old (the caller escalates to the locked native).
    /// Takes `&self`, so concurrent CASes on young `AtomicInteger`s never touch the VM write lock.
    pub fn cas_u32_eden(&self, offset: usize, expected: u32, new: u32) -> Option<bool> {
        self.in_eden(offset).map(|a| unsafe { self.eden.cas_u32(a, expected, new) })
    }

    /// Lock-free compare-and-set of a `u64` field (an `AtomicLong`) if in Eden. See
    /// [`Self::cas_u32_eden`].
    pub fn cas_u64_eden(&self, offset: usize, expected: u64, new: u64) -> Option<bool> {
        self.in_eden(offset).map(|a| unsafe { self.eden.cas_u64(a, expected, new) })
    }

    /// Writes a single byte — for `byte[]`/`boolean[]` elements (1 byte wide).
    pub fn write_u8(&mut self, offset: usize, value: u8) {
        match self.in_eden(offset) {
            Some(a) => unsafe { self.eden.write_u8(a, value) },
            None => self.memory[offset] = value,
        }
    }

    /// Reads a single byte. The caller sign/zero-extends as the element type wants.
    pub fn read_u8(&self, offset: usize) -> u8 {
        match self.in_eden(offset) {
            Some(a) => unsafe { self.eden.read_u8(a) },
            None => self.memory[offset],
        }
    }

    /// Writes a 16-bit little-endian value — for `char[]`/`short[]` elements.
    pub fn write_u16(&mut self, offset: usize, value: u16) {
        match self.in_eden(offset) {
            Some(a) => unsafe { self.eden.write_u16(a, value) },
            None => self.memory[offset..offset + 2].copy_from_slice(&value.to_le_bytes()),
        }
    }

    /// Reads a 16-bit little-endian value. The caller sign-extends (`short`) or zero-extends (`char`).
    pub fn read_u16(&self, offset: usize) -> u16 {
        match self.in_eden(offset) {
            Some(a) => unsafe { self.eden.read_u16(a) },
            None => u16::from_le_bytes(self.memory[offset..offset + 2].try_into().unwrap()),
        }
    }

    /// Reads `len` raw bytes at `offset` into an owned `Vec` — e.g. a `String`'s UTF-8 payload.
    /// (Owned, not borrowed: an Eden payload lives in the arena's `UnsafeCell`s, which can't hand
    /// out a plain `&[u8]`.)
    pub fn read_bytes(&self, offset: usize, len: usize) -> Vec<u8> {
        match self.in_eden(offset) {
            Some(a) => unsafe { self.eden.read_bytes(a, len) },
            None => self.memory[offset..offset + len].to_vec(),
        }
    }
}

/// Reads `key` from the environment as a `usize`, or returns `default` if it's unset
/// or doesn't parse — for the per-run generational region sizes.
fn env_usize(key: &str, default: usize) -> usize {
    std::env::var(key).ok().and_then(|v| v.trim().parse().ok()).unwrap_or(default)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn heap_buffer_address_is_stable_across_old_growth() {
        // W2a invariant: the backing `Vec` is pre-reserved to the max heap, so growing `Old`
        // never reallocates it — a byte's address is stable for the VM's life (the base TLABs
        // need). Grow `Old` far past the initial `DEFAULT_SIZE` and assert the pointer holds.
        let mut heap = HeapService::new();
        let ptr_before = heap.memory.as_ptr() as usize;
        let len_before = heap.memory.len();
        for _ in 0..2000 {
            heap.malloc_old(1024); // ~2 MiB of Old — well past the 1 KiB initial region
        }
        assert!(heap.memory.len() > len_before, "Old should have grown the region");
        assert_eq!(
            heap.memory.as_ptr() as usize,
            ptr_before,
            "the heap buffer must not reallocate (W2a): raw pointers into Eden would dangle"
        );
    }

    #[test]
    fn classifies_allocations_into_generations_by_region() {
        // Defaults: Eden 256, survivors 64 each → old starts at 8 + 256 + 128 = 392.
        let mut heap = HeapService::new();
        assert_eq!(heap.old_start(), 392);

        let a = heap.malloc(16);
        assert_eq!(heap.region_of(a), Region::Eden);
        assert_eq!(heap.gen_of(a), Gen::Young);
        heap.commit_pending(); // W2b: Eden mallocs go to the pending log; flush before inspecting
        assert_eq!(heap.allocations()[0].gen, Gen::Young);

        // An object too big for Eden overflows straight to Old (logged directly, not pending).
        let big = heap.malloc(heap.eden_capacity() + 8);
        assert!(big >= heap.old_start());
        assert_eq!(heap.region_of(big), Region::Old);
        assert_eq!(heap.allocations().last().unwrap().gen, Gen::Old);
    }

    #[test]
    fn malloc_bumps_eden_then_overflows_to_old() {
        let mut heap = HeapService::new();
        // New objects bump Eden, back-to-back from the null page.
        assert_eq!(heap.malloc(16), NULL_PAGE);
        assert_eq!(heap.malloc(8), NULL_PAGE + 16);
        assert_eq!(heap.malloc(8), NULL_PAGE + 24);
        // A request too big for Eden overflows to Old (the first old block) and grows
        // the arena to fit.
        let big = heap.malloc(4096);
        assert_eq!(big, heap.old_start());
        assert!(heap.size() >= heap.old_start() + 4096);
    }

    #[test]
    fn old_free_list_reuses_holes_before_growing() {
        let mut heap = HeapService::new();
        let _a = heap.malloc_old(16);
        let b = heap.malloc_old(16);
        let _c = heap.malloc_old(16);
        let high_water = heap.used();

        // Freeing the middle Old block and re-allocating its size reuses the exact
        // hole instead of bumping the Old cursor — the high-water mark doesn't move.
        heap.free(b);
        assert_eq!(heap.malloc_old(16), b);
        assert_eq!(heap.used(), high_water);
    }

    #[test]
    fn old_free_list_coalesces_adjacent_holes() {
        let mut heap = HeapService::new();
        let _a = heap.malloc_old(16);
        let b = heap.malloc_old(16);
        let c = heap.malloc_old(16);

        // Two adjacent freed Old blocks merge into one 32-byte run, reusable whole.
        heap.free(b);
        heap.free(c);
        assert_eq!(heap.free_blocks(), vec![(b, 32)]);
        assert_eq!(heap.malloc_old(32), b);
    }

    #[test]
    fn relocate_and_reset_compacts_the_old_layout() {
        let mut heap = HeapService::new();
        let a = heap.malloc_old(8);
        let _hole = heap.malloc_old(8); // middle block, to be dropped
        let c = heap.malloc_old(8);
        heap.write_u32(c, 0xabcd); // a recognisable payload to follow as it moves

        // Slide `c` down into the middle hole and install the packed Old layout: two
        // 8-byte blocks back-to-back, the trailing space reclaimed.
        let new_c = a + 8;
        heap.relocate(c, new_c, 8);
        heap.reset_after_compaction(
            vec![
                Allocation { offset: a, size: 8, gen: Gen::Old, age: 0 },
                Allocation { offset: new_c, size: 8, gen: Gen::Old, age: 0 },
            ],
            new_c + 8,
        );

        assert_eq!(heap.read_u32(new_c), 0xabcd); // payload followed the move
        assert_eq!(heap.used(), new_c + 8 - heap.old_start()); // old usage dropped
        assert!(heap.free_blocks().is_empty()); // compaction leaves no holes
    }
}
