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

use std::collections::HashSet;

/// Initial size of the heap's byte region, in bytes. Arbitrary — the region
/// grows (or shrinks) on demand via [`HeapService::resize`].
const DEFAULT_SIZE: usize = 1024;

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
    /// The raw byte region. Objects live here as bytes; a reference is an offset
    /// into this vector.
    memory: Vec<u8>,
    /// **Eden** bump pointer: next free byte in Eden `[NULL_PAGE, eden_end)`. New
    /// objects allocate here; a minor GC evacuates the survivors and **resets** this
    /// to the floor, so Eden is reused over and over (the generational allocator's
    /// fast path). Overflow falls back to Old.
    eden_cursor: usize,
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
}

/// Byte offset of the **mark word** inside an object header `[class_id | mark]`.
/// The GC sets it during the mark phase; it's 0 (unmarked) the rest of the time.
const MARK_OFFSET: usize = 4;

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
        HeapService {
            memory: vec![0; DEFAULT_SIZE.max(old_start)],
            eden_cursor: NULL_PAGE,
            to_survivor: 0,
            survivor_cursor: NULL_PAGE + eden_size, // start of S0
            old_cursor: old_start,
            objects: Vec::new(),
            free_list: Vec::new(),
            eden_size,
            survivor_size,
            remembered: HashSet::new(),
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

    /// Bytes handed out so far across all three regions (Eden + the live survivor +
    /// Old) — the heap's occupancy, for the GC triggers and the visualizer.
    pub fn used(&self) -> usize {
        let eden = self.eden_cursor - NULL_PAGE;
        let survivor = self.survivor_cursor - self.to_survivor_start();
        let old = self.old_cursor - self.old_start();
        eden + survivor + old
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
        self.eden_cursor - NULL_PAGE
    }

    /// Eden's capacity in bytes.
    pub fn eden_capacity(&self) -> usize {
        self.eden_size
    }

    /// Grows or shrinks the region to `new_size` bytes — zero-filling the new
    /// space when growing, dropping the tail when shrinking.
    pub fn resize(&mut self, new_size: usize) {
        self.memory.resize(new_size, 0);
    }

    /// Allocates `n` bytes for a **new** object and returns its start offset. New
    /// objects are born in **Eden** (a pure bump — no free list); if Eden is full,
    /// they overflow to **Old**. Logged so the GC can enumerate them.
    pub fn malloc(&mut self, n: usize) -> usize {
        if self.eden_cursor + n <= self.eden_end() {
            let offset = self.eden_cursor;
            self.eden_cursor += n;
            self.memory[offset..offset + n].fill(0);
            self.objects.push(Allocation { offset, size: n, gen: Gen::Young, age: 0 });
            offset
        } else {
            self.malloc_old(n) // Eden overflow → Old (until a minor frees Eden)
        }
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
        self.memory.copy_within(from..from + size, dest);
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
        self.eden_cursor = NULL_PAGE; // Eden is now empty — reuse it
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

    /// Writes a 32-bit value at `offset`, little-endian. The primitive every object
    /// field/header write goes through: the heap is just bytes, so a `u32` lands as
    /// its 4 LE bytes.
    pub fn write_u32(&mut self, offset: usize, value: u32) {
        self.memory[offset..offset + 4].copy_from_slice(&value.to_le_bytes());
    }

    /// Reads a 32-bit little-endian value at `offset` — the inverse of
    /// [`HeapService::write_u32`].
    pub fn read_u32(&self, offset: usize) -> u32 {
        u32::from_le_bytes(self.memory[offset..offset + 4].try_into().unwrap())
    }

    /// Writes a 64-bit value at `offset`, little-endian — for **category-2** values
    /// (`long`/`double`), which occupy two 4-byte slots (8 bytes). Independent of the
    /// 4-byte accessors: the heap is flat bytes, so an 8-byte write at any offset is
    /// fine (no alignment requirement — we copy bytes).
    pub fn write_u64(&mut self, offset: usize, value: u64) {
        self.memory[offset..offset + 8].copy_from_slice(&value.to_le_bytes());
    }

    /// Reads a 64-bit little-endian value at `offset` — the inverse of
    /// [`HeapService::write_u64`].
    pub fn read_u64(&self, offset: usize) -> u64 {
        u64::from_le_bytes(self.memory[offset..offset + 8].try_into().unwrap())
    }

    /// Writes a single byte — for `byte[]`/`boolean[]` elements (1 byte wide).
    pub fn write_u8(&mut self, offset: usize, value: u8) {
        self.memory[offset] = value;
    }

    /// Reads a single byte. The caller sign/zero-extends as the element type wants.
    pub fn read_u8(&self, offset: usize) -> u8 {
        self.memory[offset]
    }

    /// Writes a 16-bit little-endian value — for `char[]`/`short[]` elements.
    pub fn write_u16(&mut self, offset: usize, value: u16) {
        self.memory[offset..offset + 2].copy_from_slice(&value.to_le_bytes());
    }

    /// Reads a 16-bit little-endian value. The caller sign-extends (`short`) or
    /// zero-extends (`char`).
    pub fn read_u16(&self, offset: usize) -> u16 {
        u16::from_le_bytes(self.memory[offset..offset + 2].try_into().unwrap())
    }

    /// Borrows `len` raw bytes at `offset` — e.g. a `String`'s UTF-8 payload.
    pub fn read_bytes(&self, offset: usize, len: usize) -> &[u8] {
        &self.memory[offset..offset + len]
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
    fn classifies_allocations_into_generations_by_region() {
        // Defaults: Eden 256, survivors 64 each → old starts at 8 + 256 + 128 = 392.
        let mut heap = HeapService::new();
        assert_eq!(heap.old_start(), 392);

        let a = heap.malloc(16);
        assert_eq!(heap.region_of(a), Region::Eden);
        assert_eq!(heap.gen_of(a), Gen::Young);
        assert_eq!(heap.allocations()[0].gen, Gen::Young);

        // An object too big for Eden overflows straight to Old.
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
