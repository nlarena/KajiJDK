//! A **lock-free bump allocator** for Eden (H3 W2c) — the `unsafe` heart of TLAB allocation.
//!
//! Concurrent threads reserve **disjoint** byte ranges with a single atomic `fetch_add`, then
//! read and write them through an [`AtomicRegion`] — every access is an atomic op, so even a
//! `getfield` racing a `putfield` on the same object (the H4 relaxed memory model) is free of
//! data-race UB, not merely of *overlap*. Plain (non-`volatile`) field access uses `Relaxed`
//! ordering (lock-free, no happens-before); `volatile` fields use the `*_ordered` accessors
//! (`Acquire`/`Release`) for publication. Category-2 `volatile` (`long`/`double`) fields ride a
//! real `AtomicU64` — no tearing — which is why the layout 8-aligns them (`place_field`).
//!
//! Because each `fetch_add` hands out a unique `[off, off+n)` and the backing store never
//! reallocates, a block's address is stable for the arena's life. Soundness (no data races, no
//! aliasing, correct publication) is checked under **Miri** here and in [`AtomicRegion`].

use super::atomic_region::AtomicRegion;
use std::sync::atomic::{AtomicUsize, Ordering};

/// A fixed-capacity arena that bump-allocates lock-free. Bytes live in an [`AtomicRegion`] so many
/// threads can atomically read/write their (disjoint, or even shared) ranges concurrently.
pub struct EdenArena {
    /// The backing store — 8-aligned, atomic byte/word access. Fixed size — **never reallocates**,
    /// so a byte's address is stable for the arena's whole life.
    region: AtomicRegion,
    /// Bump cursor. `fetch_add` reserves disjoint ranges lock-free.
    ///
    /// **Boxed, and that is load-bearing rather than incidental.** The JIT bakes this word's
    /// machine address into an instruction stream (see [`Self::cursor_address`]) and never asks
    /// again, so the address has to survive everything that can happen to the arena afterwards —
    /// including the enclosing `HeapService` being *moved*, which it is (a `JVM` is built and
    /// returned by value). A cursor stored inline would move with it; a boxed one does not, for
    /// exactly the reason [`AtomicRegion::base_address`] is stable. The extra indirection costs one
    /// pointer load on the allocation path, against a word that is in L1 by definition.
    cursor: Box<AtomicUsize>,
}

impl EdenArena {
    /// A zeroed arena of at least `size` bytes.
    pub fn new(size: usize) -> Self {
        EdenArena { region: AtomicRegion::new(size), cursor: Box::new(AtomicUsize::new(0)) }
    }

    /// Total capacity in bytes.
    pub fn capacity(&self) -> usize {
        self.region.capacity()
    }

    /// The **machine address** of arena-local byte 0 — see
    /// [`AtomicRegion::base_address`][super::atomic_region::AtomicRegion::base_address]. The JIT
    /// bakes it in so compiled code can read an Eden object's field without going through the
    /// accessors; the arena never reallocates, so the address is good for the VM's life.
    pub fn base_address(&self) -> usize {
        self.region.base_address()
    }

    /// The **machine address** of the bump cursor itself — what a compiled `new` does its
    /// `lock xadd` on, so that native code reserves Eden bytes through the very same word, and with
    /// the very same semantics, as [`Self::alloc`]'s `fetch_add`.
    ///
    /// Stable for the arena's whole life: the cursor is boxed (see the field), so no move of the
    /// arena — or of the `HeapService` around it — relocates the word. `reset` writes through
    /// `get_mut` to this same address.
    ///
    /// The two constants a caller needs alongside it are [`Self::capacity`] and the **8-byte
    /// rounding** `alloc` applies to the request: a reservation of `n` bytes bumps the cursor by
    /// `(n + 7) & !7` and fails when the value it read back exceeds `capacity - bump`. Emitting
    /// anything else here would let compiled code and the interpreter disagree about where the next
    /// object starts.
    pub fn cursor_address(&self) -> usize {
        &*self.cursor as *const AtomicUsize as usize
    }

    /// Bytes handed out so far (clamped — the cursor may sit past the end after an overflow).
    pub fn used(&self) -> usize {
        self.cursor.load(Ordering::Relaxed).min(self.region.capacity())
    }

    /// **Lock-free** allocation of `n` zeroed bytes. `Some(offset)` on success; `None` if Eden is
    /// full (the caller falls back to the locked Old path). Safe to call from `&self` on many
    /// threads at once — the reserved range is exclusive to this call. The bump is rounded up to
    /// **8 bytes** so every object stays 8-aligned: that keeps `u32` fields 4-aligned and
    /// `long`/`double` fields (on even slots) 8-aligned — the alignment atomic access needs.
    pub fn alloc(&self, n: usize) -> Option<usize> {
        let bump = (n + 7) & !7; // pad the *stride* to 8; the object still uses its logical `n`
        let offset = self.cursor.fetch_add(bump, Ordering::Relaxed);
        // Overflow (including the wrap-guard `offset + bump` overflowing usize) → out of Eden. The
        // cursor is left past the end, so subsequent allocations also fail until `reset`.
        if offset > self.region.capacity() || bump > self.region.capacity() - offset {
            return None;
        }
        // SAFETY: `[offset, offset+n)` was reserved by *this* `fetch_add`; no other reservation
        // overlaps it. Clear the logical object to its default (0/null) field values; the padding
        // up to `bump` is slack between objects.
        unsafe { self.region.zero(offset, n) };
        Some(offset)
    }

    // ---- Plain (`Relaxed`) accessors: object headers, non-`volatile` fields, array elements,
    //      string bytes. Every one is atomic, so it never races a concurrent access on the VM. ----

    /// Write one byte (`Relaxed`).
    ///
    /// # Safety
    /// `offset` must be in bounds.
    pub unsafe fn write_u8(&self, offset: usize, value: u8) {
        unsafe { self.region.store_u8(offset, value, Ordering::Relaxed) };
    }

    /// Read one byte (`Relaxed`).
    ///
    /// # Safety
    /// `offset` must be in bounds.
    pub unsafe fn read_u8(&self, offset: usize) -> u8 {
        unsafe { self.region.load_u8(offset, Ordering::Relaxed) }
    }

    /// Write a little-endian `u32` (`Relaxed`) — a header word or non-`volatile` int/ref field.
    ///
    /// # Safety
    /// `[offset, offset+4)` in bounds and 4-aligned.
    pub unsafe fn write_u32(&self, offset: usize, value: u32) {
        unsafe { self.region.store_u32(offset, value, Ordering::Relaxed) };
    }

    /// Read a little-endian `u32` (`Relaxed`).
    ///
    /// # Safety
    /// `[offset, offset+4)` in bounds and 4-aligned.
    pub unsafe fn read_u32(&self, offset: usize) -> u32 {
        unsafe { self.region.load_u32(offset, Ordering::Relaxed) }
    }

    /// Write a little-endian `u16` (`Relaxed`) — a `char` array element.
    ///
    /// # Safety
    /// `[offset, offset+2)` in bounds and 2-aligned.
    pub unsafe fn write_u16(&self, offset: usize, value: u16) {
        unsafe { self.region.store_u16(offset, value, Ordering::Relaxed) };
    }

    /// Read a little-endian `u16` (`Relaxed`).
    ///
    /// # Safety
    /// `[offset, offset+2)` in bounds and 2-aligned.
    pub unsafe fn read_u16(&self, offset: usize) -> u16 {
        unsafe { self.region.load_u16(offset, Ordering::Relaxed) }
    }

    /// Write a little-endian `u64` (a non-`volatile` `long`/`double` field, or a `long[]` element)
    /// as **two `Relaxed` `u32` halves**. Only needs 4-alignment, so it serves `long[]` elements
    /// too; the trade-off is that a concurrent racing access may observe a torn value — which the
    /// JVM spec explicitly permits for *non-volatile* category-2. Volatile fields use
    /// [`Self::store_u64_ordered`] (a real `AtomicU64`, no tearing).
    ///
    /// # Safety
    /// `[offset, offset+8)` in bounds and 4-aligned.
    pub unsafe fn write_u64(&self, offset: usize, value: u64) {
        unsafe {
            self.region.store_u32(offset, value as u32, Ordering::Relaxed);
            self.region.store_u32(offset + 4, (value >> 32) as u32, Ordering::Relaxed);
        }
    }

    /// Read a little-endian `u64` as two `Relaxed` `u32` halves (see [`Self::write_u64`]).
    ///
    /// # Safety
    /// `[offset, offset+8)` in bounds and 4-aligned.
    pub unsafe fn read_u64(&self, offset: usize) -> u64 {
        unsafe {
            let lo = self.region.load_u32(offset, Ordering::Relaxed) as u64;
            let hi = self.region.load_u32(offset + 4, Ordering::Relaxed) as u64;
            lo | (hi << 32)
        }
    }

    // ---- Ordered accessors for `volatile` fields (H4): `Acquire` reads, `Release` writes. ----

    /// A `volatile` 4-byte field read/write with explicit ordering (int/float/reference).
    ///
    /// # Safety
    /// `[offset, offset+4)` in bounds and 4-aligned.
    pub unsafe fn load_u32_ordered(&self, offset: usize, order: Ordering) -> u32 {
        unsafe { self.region.load_u32(offset, order) }
    }

    /// See [`Self::load_u32_ordered`].
    ///
    /// # Safety
    /// `[offset, offset+4)` in bounds and 4-aligned.
    pub unsafe fn store_u32_ordered(&self, offset: usize, value: u32, order: Ordering) {
        unsafe { self.region.store_u32(offset, value, order) };
    }

    /// A `volatile` `long`/`double` read/write — a real `AtomicU64`, so **no tearing**. Requires
    /// an 8-aligned offset, which volatile category-2 fields have (`place_field` → even slot).
    ///
    /// # Safety
    /// `[offset, offset+8)` in bounds and 8-aligned.
    pub unsafe fn load_u64_ordered(&self, offset: usize, order: Ordering) -> u64 {
        unsafe { self.region.load_u64(offset, order) }
    }

    /// See [`Self::load_u64_ordered`].
    ///
    /// # Safety
    /// `[offset, offset+8)` in bounds and 8-aligned.
    pub unsafe fn store_u64_ordered(&self, offset: usize, value: u64, order: Ordering) {
        unsafe { self.region.store_u64(offset, value, order) };
    }

    /// Lock-free **compare-and-set** of a `u32` field (H5): if it holds `expected`, set it to
    /// `new` and return `true`. `AcqRel` on success (acquire the read, release the write),
    /// `Acquire` on failure — volatile-CAS semantics.
    ///
    /// # Safety
    /// `[offset, offset+4)` in bounds and 4-aligned.
    pub unsafe fn cas_u32(&self, offset: usize, expected: u32, new: u32) -> bool {
        unsafe {
            self.region
                .compare_exchange_u32(offset, expected, new, Ordering::AcqRel, Ordering::Acquire)
                .is_ok()
        }
    }

    /// Lock-free compare-and-set of a `u64` field (`AtomicLong`). See [`Self::cas_u32`].
    ///
    /// # Safety
    /// `[offset, offset+8)` in bounds and 8-aligned.
    pub unsafe fn cas_u64(&self, offset: usize, expected: u64, new: u64) -> bool {
        unsafe {
            self.region
                .compare_exchange_u64(offset, expected, new, Ordering::AcqRel, Ordering::Acquire)
                .is_ok()
        }
    }

    /// Copy `len` bytes from `[offset, offset+len)` into an owned `Vec` (Relaxed, byte-by-byte).
    ///
    /// # Safety
    /// The range must be in bounds.
    pub unsafe fn read_bytes(&self, offset: usize, len: usize) -> Vec<u8> {
        unsafe { self.region.read_bytes(offset, len) }
    }

    /// Reset the arena to empty — the minor collector recycles Eden. Takes `&mut self`, so it can
    /// only run when no thread holds a shared reference (a stop-the-world safepoint). Stale bytes
    /// stay until `alloc` re-zeroes them.
    pub fn reset(&mut self) {
        *self.cursor.get_mut() = 0;
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::Arc;

    #[test]
    fn single_thread_bumps_resets_and_reports_full() {
        let mut arena = EdenArena::new(64);
        assert_eq!(arena.alloc(16), Some(0));
        assert_eq!(arena.alloc(16), Some(16));
        assert_eq!(arena.used(), 32);
        // A request that doesn't fit fails — and (standard bump semantics) still advances the
        // cursor past the end, so Eden reads as full until the GC recycles it. This is fine: the
        // caller falls back to Old, and the minor collector `reset`s Eden shortly after.
        assert_eq!(arena.alloc(64), None);
        assert_eq!(arena.alloc(8), None);
        assert_eq!(arena.used(), 64); // clamped to capacity
        // The minor GC recycles Eden at a safepoint.
        arena.reset();
        assert_eq!(arena.alloc(8), Some(0));
    }

    #[test]
    fn write_then_read_roundtrips() {
        let arena = EdenArena::new(64);
        let off = arena.alloc(8).unwrap();
        unsafe {
            arena.write_u32(off, 0xdead_beef);
            arena.write_u8(off + 4, 0x42);
            assert_eq!(arena.read_u32(off), 0xdead_beef);
            assert_eq!(arena.read_u8(off + 4), 0x42);
        }
    }

    /// The soundness test — run under Miri (`cargo +nightly miri test`): four threads each
    /// allocate many blocks concurrently and write a thread-unique tag into every one. If the
    /// ranges ever overlapped (a bug in the bump allocator) two threads would write the same
    /// byte and Miri's data-race checker would fire; then we verify every block still holds its
    /// own tag (no lost/torn writes) and all ranges are disjoint and in bounds.
    #[test]
    fn concurrent_bump_is_race_free() {
        const THREADS: usize = 4;
        const PER_THREAD: usize = 50;
        const BLOCK: usize = 8;
        let arena = Arc::new(EdenArena::new(THREADS * PER_THREAD * BLOCK));

        let handles: Vec<_> = (0..THREADS)
            .map(|t| {
                let arena = Arc::clone(&arena);
                std::thread::spawn(move || {
                    let mut offsets = Vec::with_capacity(PER_THREAD);
                    for _ in 0..PER_THREAD {
                        let off = arena.alloc(BLOCK).expect("arena sized to fit exactly");
                        // Tag every byte of our block with our thread id — concurrent writes to
                        // disjoint ranges. Miri checks this doesn't race.
                        for i in 0..BLOCK {
                            unsafe { arena.write_u8(off + i, t as u8) };
                        }
                        offsets.push(off);
                    }
                    offsets
                })
            })
            .collect();

        let mut all: Vec<(usize, usize)> = Vec::new(); // (offset, thread)
        for (t, h) in handles.into_iter().enumerate() {
            for off in h.join().unwrap() {
                // Our tag survived (no other thread clobbered our block).
                for i in 0..BLOCK {
                    assert_eq!(unsafe { arena.read_u8(off + i) }, t as u8, "block was clobbered");
                }
                all.push((off, t));
            }
        }

        // Every allocated range is disjoint and in bounds.
        all.sort_unstable();
        for w in all.windows(2) {
            assert!(w[0].0 + BLOCK <= w[1].0, "overlapping allocations: {:?} {:?}", w[0], w[1]);
        }
        assert_eq!(all.len(), THREADS * PER_THREAD);
        assert!(all.last().unwrap().0 + BLOCK <= arena.capacity());
    }

    /// The **integrated** W2c soundness: while one thread allocates+writes *fresh* blocks, another
    /// reads *already-published* ones — the exact shape of a lock-free `new` racing a `getfield`
    /// in the running VM. Published and fresh blocks are disjoint, so under Miri no read ever
    /// aliases a concurrent write. Run under `cargo +nightly miri test`.
    #[test]
    fn concurrent_alloc_and_read_of_published_is_race_free() {
        const N: u32 = 40;
        const BLOCK: usize = 8;
        let arena = Arc::new(EdenArena::new(2 * N as usize * BLOCK)); // published + fresh
        // Publish N tagged blocks single-threaded.
        let published: Vec<usize> = (0..N)
            .map(|k| {
                let off = arena.alloc(BLOCK).unwrap();
                unsafe { arena.write_u32(off, k) };
                off
            })
            .collect();
        let published = Arc::new(published);

        // A: allocate + write fresh blocks. B: read the published blocks — concurrently.
        let a_arena = Arc::clone(&arena);
        let a = std::thread::spawn(move || {
            for _ in 0..N {
                if let Some(off) = a_arena.alloc(BLOCK) {
                    unsafe { a_arena.write_u32(off, 0xffff_ffff) };
                }
            }
        });
        let b_arena = Arc::clone(&arena);
        let b_pub = Arc::clone(&published);
        let b = std::thread::spawn(move || {
            b_pub.iter().map(|&off| unsafe { b_arena.read_u32(off) } as u64).sum::<u64>()
        });

        a.join().unwrap();
        let sum = b.join().unwrap();
        // The published tags survived untouched (A only wrote its own fresh blocks).
        assert_eq!(sum, (0..N as u64).sum());
    }

    /// **Miri harness for the rare os-parallel stale-reference bug** —
    /// `cargo +nightly miri test miri_os_parallel_heap`.
    ///
    /// The isolated `AtomicRegion`/`EdenArena` tests above only ever cover a single arena with no
    /// generational move and no reuse. The real bug lives in the *integrated* os-parallel heap, so
    /// this harness rebuilds that path in miniature and hands it to Miri's data-race detector to
    /// rule on the three low-level suspects recorded in the investigation:
    ///   1. the lock-free Eden bump cursor (`alloc`, `&self`, atomic `fetch_add`) vs. `reset`
    ///      (`&mut`, non-atomic `get_mut`) on the collector,
    ///   2. an object's header word read (`Relaxed`) while it is written during alloc/evacuation,
    ///   3. a *published* reference read across the Eden→Old move **and** the following Eden reuse.
    ///
    /// Shape mirrors `HeapService` behind `SharedVm`'s `RwLock`: `Heap { eden, old }` under one
    /// `RwLock`. Mutator threads take the **read** guard to allocate + tag objects lock-free
    /// (concurrent, disjoint Eden ranges) and to read back references from a shared root array; a
    /// coordinator takes the **write** guard for a stop-the-world collect that evacuates every
    /// rooted Eden object to Old (atomic Eden read → plain Old write — the H4 mixed-access case),
    /// remaps the roots, and `reset`s Eden so the next round reuses the same offsets. Root
    /// publication uses `Release`/`Acquire` — the ordered-visibility the real read/write locks
    /// carry from a header write to a later header read. Every tag word is unique per
    /// `(thread, round, slot)`, so a torn, zeroed, stale or aliased word trips an assertion even if
    /// Miri's race detector stays silent.
    #[test]
    fn miri_os_parallel_heap() {
        use std::sync::atomic::{AtomicU32, Ordering};
        use std::sync::{Arc, Barrier, RwLock};

        const THREADS: usize = 2;
        const ROUNDS: usize = 3;
        const SLOTS: usize = 2; // roots (live references) owned per thread
        const BLOCK: usize = 16; // bytes per object; word 0 is the header/tag

        // Reference encoding (like `heap.read_u32`'s `in_eden` routing): NULL, or a generation bit
        // plus the offset within that generation. EDEN_BIT keeps Eden offset 0 distinct from NULL.
        const NULL: u32 = 0;
        const EDEN_BIT: u32 = 0x4000_0000;
        const OLD_BIT: u32 = 0x8000_0000;
        const OFF_MASK: u32 = 0x3FFF_FFFF;

        struct Heap {
            eden: EdenArena,
            old: Vec<u8>,
            old_cursor: usize,
        }

        // Eden holds exactly one round's objects; the collect recycles it each round. Old grows by
        // one round's worth of survivors per round.
        let per_round = THREADS * SLOTS * BLOCK;
        let heap = Arc::new(RwLock::new(Heap {
            eden: EdenArena::new(per_round),
            old: vec![0u8; per_round * ROUNDS],
            old_cursor: 0,
        }));
        // Shared root array — models live array/field slots holding references. Slot `t*SLOTS+s` is
        // written only by thread `t` but read by everyone.
        let roots: Arc<Vec<AtomicU32>> =
            Arc::new((0..THREADS * SLOTS).map(|_| AtomicU32::new(NULL)).collect());
        let gate = Arc::new(Barrier::new(THREADS + 1)); // mutate | collect phases, per round

        // Unique non-zero header tag per (thread, round, slot); high byte 0x01 marks a valid tag.
        let tag_of = |t: usize, round: usize, s: usize| -> u32 {
            0x0100_0000 | ((t as u32) << 12) | ((round as u32) << 4) | (s as u32)
        };

        let handles: Vec<_> = (0..THREADS)
            .map(|t| {
                let heap = Arc::clone(&heap);
                let roots = Arc::clone(&roots);
                let gate = Arc::clone(&gate);
                std::thread::spawn(move || {
                    for round in 0..ROUNDS {
                        // ---- MUTATE: read guard, all mutators concurrent ----
                        {
                            let h = heap.read().unwrap();
                            for s in 0..SLOTS {
                                // lock-free `new`: fresh, disjoint Eden offset + header write.
                                let off = h.eden.alloc(BLOCK).expect("eden sized for one round");
                                let tag = tag_of(t, round, s);
                                unsafe { h.eden.write_u32(off, tag) };
                                // publish the reference (like `aastore` into a live slot).
                                roots[t * SLOTS + s]
                                    .store(EDEN_BIT | off as u32, Ordering::Release);
                            }
                            // read every published root and check its header — models another thread
                            // doing `aaload` + `getfield` on a shared reference, concurrently with
                            // the other mutators' allocations. A root may point into Eden (this
                            // round) or Old (a survivor of the prior collect); both are valid.
                            for (i, r) in roots.iter().enumerate() {
                                let rf = r.load(Ordering::Acquire);
                                if rf == NULL {
                                    continue;
                                }
                                let off = (rf & OFF_MASK) as usize;
                                let got = if rf & OLD_BIT != 0 {
                                    let b = &h.old[off..off + 4];
                                    u32::from_le_bytes([b[0], b[1], b[2], b[3]])
                                } else {
                                    unsafe { h.eden.read_u32(off) }
                                };
                                assert_eq!(
                                    got & 0xFF00_0000,
                                    0x0100_0000,
                                    "slot {i}: torn/stale/aliased header {got:#010x}"
                                );
                            }
                        }
                        gate.wait(); // mutators reached the safepoint → coordinator collects
                        gate.wait(); // collect done → next round reuses the reset Eden
                    }
                })
            })
            .collect();

        // ---- Coordinator: one stop-the-world collect per round (write guard) ----
        for _round in 0..ROUNDS {
            gate.wait(); // all mutators parked
            {
                let mut h = heap.write().unwrap();
                for i in 0..roots.len() {
                    let rf = roots[i].load(Ordering::Acquire);
                    if rf == NULL || rf & OLD_BIT != 0 {
                        continue; // null, or already evacuated in a prior round
                    }
                    let from = (rf & OFF_MASK) as usize;
                    let dest = h.old_cursor;
                    h.old_cursor += BLOCK;
                    for k in 0..BLOCK {
                        // atomic read from Eden → plain byte write to Old (the mixed-access case).
                        let b = unsafe { h.eden.read_u8(from + k) };
                        h.old[dest + k] = b;
                    }
                    roots[i].store(OLD_BIT | dest as u32, Ordering::Release);
                }
                h.eden.reset(); // recycle Eden; next round hands the same offsets back out
            }
            gate.wait(); // release mutators
        }
        for handle in handles {
            handle.join().unwrap();
        }

        // Every surviving root now resolves, from Old, to its owner's *last* published tag — no
        // remap was missed and no reused Eden offset leaked through.
        let h = heap.read().unwrap();
        for t in 0..THREADS {
            for s in 0..SLOTS {
                let rf = roots[t * SLOTS + s].load(Ordering::Acquire);
                assert!(rf & OLD_BIT != 0, "root ({t},{s}) was not evacuated to Old");
                let off = (rf & OFF_MASK) as usize;
                let b = &h.old[off..off + 4];
                let got = u32::from_le_bytes([b[0], b[1], b[2], b[3]]);
                assert_eq!(got, tag_of(t, ROUNDS - 1, s), "root ({t},{s}) lost its final tag");
            }
        }
    }
}
