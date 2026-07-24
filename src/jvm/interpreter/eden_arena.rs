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
    cursor: AtomicUsize,
}

impl EdenArena {
    /// A zeroed arena of at least `size` bytes.
    pub fn new(size: usize) -> Self {
        EdenArena { region: AtomicRegion::new(size), cursor: AtomicUsize::new(0) }
    }

    /// Total capacity in bytes.
    pub fn capacity(&self) -> usize {
        self.region.capacity()
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
}
