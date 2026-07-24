//! An **8-byte-aligned atomic byte region** — the substrate for the H4 relaxed Java Memory
//! Model. Object fields live here and are read/written **atomically with explicit ordering**:
//!
//! - **non-`volatile`** fields → `Relaxed`: lock-free and racy (no happens-before), but *sound*
//!   (an atomic access is never a data race, and never tears at its own width). The value still
//!   propagates via cache coherence, so a spin-on-flag eventually sees it.
//! - **`volatile`** fields → `Acquire` (read) / `Release` (write): a release publishes everything
//!   written before it, and a matching acquire sees all of it — the JMM's visibility guarantee.
//!
//! This is *the* sound way to relax field access off the lock in Rust: plain concurrent reads and
//! writes of shared memory are undefined behaviour, but atomic ones are not. Alignment matters —
//! `AtomicU32` needs a 4-aligned address and `AtomicU64` an 8-aligned one — so the backing store is
//! 8-aligned (a `Box<[u64]>` viewed as bytes) and the layout puts `long`/`double` on 8-aligned
//! offsets. Soundness (no data races, no tearing, correct Acquire/Release publication) is checked
//! under **Miri** (see the tests).

use std::cell::UnsafeCell;
use std::sync::atomic::{AtomicU16, AtomicU32, AtomicU64, AtomicU8, Ordering};

/// A fixed-capacity, 8-aligned region of bytes with atomic `u32`/`u64` slot access.
///
/// The backing store is a slice of `UnsafeCell<u64>` — one 8-byte cell per slot. Interior
/// mutability (via `UnsafeCell::get`) is what lets a shared `&self` hand out a *writable* raw
/// pointer soundly (deriving `*mut` from `as_ptr()` would only carry read permission, which Miri's
/// Stacked Borrows rejects). Making each cell a `u64` — rather than a `u8` as in [`EdenArena`] —
/// matters for atomics: a `u32` field sits at a 4-aligned offset, so it lands wholly *within* one
/// 8-byte cell's provenance, and a `u64` field is exactly one cell. No atomic access ever straddles
/// a cell boundary, so each stays inside the provenance `get()` grants.
///
/// [`EdenArena`]: super::eden_arena::EdenArena
pub struct AtomicRegion {
    /// One 8-byte cell per slot; the base is `u64`-aligned. Fixed size — never reallocates, so a
    /// byte's address is stable for the region's life.
    cells: Box<[UnsafeCell<u64>]>,
    /// Capacity in bytes (`cells.len() * 8`).
    bytes: usize,
}

// SAFETY: every shared access goes through the atomic accessors below (or happens at a
// stop-the-world safepoint, where a single thread has exclusive access). Atomic accesses to the
// same location never form a data race; distinct fields are distinct locations. The backing slice
// never reallocates, so the raw pointers the accessors derive stay valid.
unsafe impl Sync for AtomicRegion {}

impl AtomicRegion {
    /// A zeroed region of at least `bytes` bytes, 8-aligned.
    pub fn new(bytes: usize) -> Self {
        let ncells = bytes.div_ceil(8);
        let mut v = Vec::with_capacity(ncells);
        v.resize_with(ncells, || UnsafeCell::new(0u64));
        AtomicRegion { cells: v.into_boxed_slice(), bytes: ncells * 8 }
    }

    /// Capacity in bytes.
    pub fn capacity(&self) -> usize {
        self.bytes
    }

    /// A raw pointer to byte `offset`, carrying the provenance of the enclosing 8-byte cell. The
    /// caller must keep the access (`u32`/`u64`) within that cell — guaranteed for 4-aligned `u32`
    /// and 8-aligned `u64` offsets, which never straddle a cell.
    #[inline]
    fn byte_ptr(&self, offset: usize) -> *mut u8 {
        debug_assert!(offset < self.bytes);
        let cell = offset / 8;
        let within = offset % 8;
        // `get()` yields a `*mut u64` with write provenance over the whole cell; step into it.
        unsafe { (self.cells[cell].get() as *mut u8).add(within) }
    }

    /// Atomic `u8` load — any offset is 1-aligned, so always valid.
    ///
    /// # Safety
    /// `offset` in bounds; the byte only ever accessed as a `u8` while shared.
    #[inline]
    pub unsafe fn load_u8(&self, offset: usize, order: Ordering) -> u8 {
        unsafe { (*AtomicU8::from_ptr(self.byte_ptr(offset))).load(order) }
    }

    /// Atomic `u8` store.
    ///
    /// # Safety
    /// As [`Self::load_u8`].
    #[inline]
    pub unsafe fn store_u8(&self, offset: usize, value: u8, order: Ordering) {
        unsafe { (*AtomicU8::from_ptr(self.byte_ptr(offset))).store(value, order) }
    }

    /// Atomic `u16` load.
    ///
    /// # Safety
    /// `offset` must be 2-aligned and in bounds; the 2-byte slot only ever accessed as a `u16`.
    #[inline]
    pub unsafe fn load_u16(&self, offset: usize, order: Ordering) -> u16 {
        debug_assert_eq!(offset % 2, 0, "u16 atomic needs a 2-aligned offset");
        unsafe { (*AtomicU16::from_ptr(self.byte_ptr(offset).cast())).load(order) }
    }

    /// Atomic `u16` store.
    ///
    /// # Safety
    /// As [`Self::load_u16`].
    #[inline]
    pub unsafe fn store_u16(&self, offset: usize, value: u16, order: Ordering) {
        debug_assert_eq!(offset % 2, 0, "u16 atomic needs a 2-aligned offset");
        unsafe { (*AtomicU16::from_ptr(self.byte_ptr(offset).cast())).store(value, order) }
    }

    /// Atomic `u32` load with the given ordering.
    ///
    /// # Safety
    /// `offset` must be 4-aligned and in bounds, and the 4-byte slot must only ever be accepted as
    /// a `u32` (not aliased as a different atomic width) while shared.
    #[inline]
    pub unsafe fn load_u32(&self, offset: usize, order: Ordering) -> u32 {
        debug_assert_eq!(offset % 4, 0, "u32 atomic needs a 4-aligned offset");
        unsafe { (*AtomicU32::from_ptr(self.byte_ptr(offset).cast())).load(order) }
    }

    /// Atomic `u32` store with the given ordering.
    ///
    /// # Safety
    /// As [`Self::load_u32`].
    #[inline]
    pub unsafe fn store_u32(&self, offset: usize, value: u32, order: Ordering) {
        debug_assert_eq!(offset % 4, 0, "u32 atomic needs a 4-aligned offset");
        unsafe { (*AtomicU32::from_ptr(self.byte_ptr(offset).cast())).store(value, order) }
    }

    /// Atomic `u64` load (a `long`/`double` slot) — no tearing.
    ///
    /// # Safety
    /// `offset` must be **8-aligned** and in bounds, and the 8-byte slot only ever accessed as a
    /// `u64` while shared.
    #[inline]
    pub unsafe fn load_u64(&self, offset: usize, order: Ordering) -> u64 {
        debug_assert_eq!(offset % 8, 0, "u64 atomic needs an 8-aligned offset");
        unsafe { (*AtomicU64::from_ptr(self.byte_ptr(offset).cast())).load(order) }
    }

    /// Atomic `u64` store (a `long`/`double` slot) — no tearing.
    ///
    /// # Safety
    /// As [`Self::load_u64`].
    #[inline]
    pub unsafe fn store_u64(&self, offset: usize, value: u64, order: Ordering) {
        debug_assert_eq!(offset % 8, 0, "u64 atomic needs an 8-aligned offset");
        unsafe { (*AtomicU64::from_ptr(self.byte_ptr(offset).cast())).store(value, order) }
    }

    /// Copy `len` bytes from `[offset, offset+len)` into an owned `Vec`, byte-by-byte (a
    /// contiguous `&[u8]` can't span cells). Each byte is a `Relaxed` atomic load, so this is
    /// sound even if another thread is atomically touching neighbouring bytes.
    ///
    /// # Safety
    /// The range must be in bounds.
    pub unsafe fn read_bytes(&self, offset: usize, len: usize) -> Vec<u8> {
        (0..len).map(|i| unsafe { self.load_u8(offset + i, Ordering::Relaxed) }).collect()
    }

    /// Zero `[offset, offset+len)` with `Relaxed` atomic byte stores — used to clear a freshly
    /// bump-allocated block to its default (0/null) field values.
    ///
    /// # Safety
    /// The range must be in bounds and reserved by (exclusive to) the caller.
    pub unsafe fn zero(&self, offset: usize, len: usize) {
        for i in 0..len {
            unsafe { self.store_u8(offset + i, 0, Ordering::Relaxed) };
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::Arc;

    #[test]
    fn roundtrips_all_widths() {
        let r = AtomicRegion::new(64);
        unsafe {
            r.store_u8(0, 0xab, Ordering::Relaxed);
            r.store_u16(2, 0xbeef, Ordering::Relaxed);
            r.store_u32(4, 0xdead_beef, Ordering::Relaxed);
            r.store_u64(8, 0x0123_4567_89ab_cdef, Ordering::Relaxed);
            assert_eq!(r.load_u8(0, Ordering::Relaxed), 0xab);
            assert_eq!(r.load_u16(2, Ordering::Relaxed), 0xbeef);
            assert_eq!(r.load_u32(4, Ordering::Relaxed), 0xdead_beef);
            assert_eq!(r.load_u64(8, Ordering::Relaxed), 0x0123_4567_89ab_cdef);
            // read_bytes reflects the little-endian byte image; zero clears a range.
            assert_eq!(r.read_bytes(4, 4), 0xdead_beefu32.to_le_bytes().to_vec());
            r.zero(4, 4);
            assert_eq!(r.load_u32(4, Ordering::Relaxed), 0);
        }
    }

    /// The **publication** guarantee (`volatile`): a `Release` store publishes everything written
    /// before it, and a matching `Acquire` load sees all of it. Under Miri, if the orderings were
    /// wrong the reader could observe the flag set but the data stale — Miri's weak-memory model
    /// would surface it. Run under `cargo +nightly miri test`.
    #[test]
    fn release_acquire_publishes_prior_writes() {
        const DATA: usize = 0; // non-volatile payload
        const FLAG: usize = 8; // volatile flag (8-aligned so we can reuse it as u64 too, but use u32)
        let r = Arc::new(AtomicRegion::new(16));

        let w = Arc::clone(&r);
        let writer = std::thread::spawn(move || {
            unsafe {
                w.store_u32(DATA, 42, Ordering::Relaxed); // prior write
                w.store_u32(FLAG, 1, Ordering::Release); // publish
            }
        });
        let reader = Arc::clone(&r);
        let observed = std::thread::spawn(move || {
            // Spin until the flag is published (Acquire), then read the data.
            while unsafe { reader.load_u32(FLAG, Ordering::Acquire) } == 0 {
                std::hint::spin_loop();
            }
            unsafe { reader.load_u32(DATA, Ordering::Relaxed) }
        });

        writer.join().unwrap();
        // The Acquire saw the Release, so the prior `data = 42` is visible — never the stale 0.
        assert_eq!(observed.join().unwrap(), 42);
    }

    /// **No tearing** on a `long`/`double` (`u64`) slot: one thread flips it between two values
    /// whose halves differ; the reader must only ever observe a whole value, never a mix.
    #[test]
    fn u64_slot_never_tears() {
        const A: u64 = 0x0000_0000_0000_0000;
        const B: u64 = 0xffff_ffff_ffff_ffff;
        let r = Arc::new(AtomicRegion::new(8));

        let w = Arc::clone(&r);
        let writer = std::thread::spawn(move || {
            for i in 0..100u64 {
                unsafe { w.store_u64(0, if i % 2 == 0 { A } else { B }, Ordering::Relaxed) };
            }
        });
        let reader = Arc::clone(&r);
        let checker = std::thread::spawn(move || {
            for _ in 0..100 {
                let v = unsafe { reader.load_u64(0, Ordering::Relaxed) };
                assert!(v == A || v == B, "torn u64: {v:#018x}");
            }
        });
        writer.join().unwrap();
        checker.join().unwrap();
    }
}
