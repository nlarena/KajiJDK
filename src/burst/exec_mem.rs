//! W^X executable memory on Windows, on top of hand-declared `kernel32` imports.
//!
//! # W^X ("write xor execute")
//!
//! A page is never simultaneously writable and executable. The type system enforces the sequence:
//!
//! ```text
//!   CodeBuf::new(cap)   -> PAGE_READWRITE     (writable, not executable)
//!   buf.write(bytes)    -> still RW
//!   buf.make_executable() -> PAGE_EXECUTE_READ (executable, not writable)  ... consumes the CodeBuf
//!   mem.as_fn::<F>()    -> callable
//! ```
//!
//! [`CodeBuf::make_executable`] takes `self` by value, so once the pages are RX there is no
//! `CodeBuf` left to write through. Allocating RWX would be one call shorter and would turn any
//! stray write into arbitrary code execution; it also trips W^X mitigations (Arbitrary Code Guard)
//! and forces some CPUs onto a slower path. The cost of doing it properly is one `VirtualProtect`.
//!
//! # No dependencies
//!
//! `VirtualAlloc` and friends are declared by hand in an `extern "system"` block, in the same
//! spirit as the rest of this project (which parses class files with its own byte cursor rather
//! than pulling in `nom`). `extern "system"` on `x86_64-pc-windows-*` *is* the Microsoft x64
//! calling convention — the same one [`crate::burst::x64::Frame`] emits, which is why a generated
//! function can be called through an `extern "system" fn` pointer with no thunk in between.

use std::ffi::c_void;
use std::io;

// -------------------------------------------------------------------------------------------
// Hand-declared kernel32 imports.
// -------------------------------------------------------------------------------------------

/// Reserve address space *and* back it with physical storage in one call. `MEM_RESERVE` alone
/// hands back address space that faults on touch; `MEM_COMMIT` alone only works on an
/// already-reserved range. For a single self-contained code block we want both.
const MEM_COMMIT: u32 = 0x0000_1000;
/// Reserve a range of the process's virtual address space.
const MEM_RESERVE: u32 = 0x0000_2000;
/// Release the *entire* reservation. Required to pass `dwSize == 0` and the original base address;
/// this is the counterpart of an allocation made with `MEM_RESERVE`.
const MEM_RELEASE: u32 = 0x0000_8000;

/// Readable and writable, **not** executable — the state in which code is assembled.
const PAGE_READWRITE: u32 = 0x04;
/// Readable and executable, **not** writable — the state in which code is run.
const PAGE_EXECUTE_READ: u32 = 0x20;

#[link(name = "kernel32")]
extern "system" {
    /// Returns the base address of the new region, or null on failure.
    fn VirtualAlloc(
        lpAddress: *mut c_void,
        dwSize: usize,
        flAllocationType: u32,
        flProtect: u32,
    ) -> *mut c_void;

    /// Changes the protection of a committed region. `lpflOldProtect` is **not** optional: passing
    /// null makes the call fail with `ERROR_NOACCESS`, even though the old value is not wanted.
    fn VirtualProtect(
        lpAddress: *mut c_void,
        dwSize: usize,
        flNewProtect: u32,
        lpflOldProtect: *mut u32,
    ) -> i32;

    /// Frees a region. With `MEM_RELEASE`, `dwSize` must be 0.
    fn VirtualFree(lpAddress: *mut c_void, dwSize: usize, dwFreeType: u32) -> i32;

    /// A pseudo-handle for the current process. Not a real handle, so it must not be closed.
    fn GetCurrentProcess() -> *mut c_void;

    /// Tells the OS that the given range now contains new code.
    fn FlushInstructionCache(
        hProcess: *mut c_void,
        lpBaseAddress: *const c_void,
        dwSize: usize,
    ) -> i32;
}

/// Rounds `n` up to a whole number of 4 KiB pages (never 0 — a zero-byte `VirtualAlloc` fails).
///
/// `VirtualAlloc` rounds up internally too, but `VirtualProtect` operates on whole pages as well,
/// so rounding here keeps the length we pass to both calls identical to the length we actually own.
fn page_round_up(n: usize) -> usize {
    const PAGE: usize = 4096;
    n.max(1).div_ceil(PAGE) * PAGE
}

// -------------------------------------------------------------------------------------------
// The RAII owner.
// -------------------------------------------------------------------------------------------

/// Owns one `VirtualAlloc` reservation and releases it on drop. Private: the public types
/// ([`CodeBuf`], [`ExecMem`]) wrap it so that the *protection state* is part of the type, while the
/// lifetime of the pages is handled here, once.
struct Pages {
    ptr: *mut u8,
    len: usize,
}

impl Pages {
    /// Commits `len` bytes (rounded up to pages) with protection `protect`.
    fn alloc(len: usize, protect: u32) -> io::Result<Pages> {
        let len = page_round_up(len);
        // SAFETY: a null `lpAddress` asks the OS to choose the address, which is always legal.
        // `len` is non-zero and page-rounded. The call has no effect on any memory we already own;
        // on failure it returns null, which is checked immediately below, so nothing downstream
        // ever sees an invalid pointer.
        let ptr = unsafe { VirtualAlloc(std::ptr::null_mut(), len, MEM_COMMIT | MEM_RESERVE, protect) };
        if ptr.is_null() {
            return Err(io::Error::last_os_error());
        }
        Ok(Pages { ptr: ptr.cast::<u8>(), len })
    }

    /// Changes the protection of the whole reservation.
    fn protect(&self, new: u32) -> io::Result<()> {
        let mut old: u32 = 0;
        // SAFETY: `self.ptr`/`self.len` describe exactly the region this `Pages` owns and is still
        // committed (it is only released in `Drop`). `old` is a valid, initialised `u32` we own —
        // the API requires a non-null out-pointer here even when the previous value is unwanted.
        let ok = unsafe { VirtualProtect(self.ptr.cast::<c_void>(), self.len, new, &mut old) };
        if ok == 0 {
            return Err(io::Error::last_os_error());
        }
        Ok(())
    }

    /// Asks the OS to make the range coherent for instruction fetch.
    ///
    /// On x86-64 this is nearly a formality: the instruction cache is kept coherent with data
    /// writes in hardware, and the `VirtualProtect` above already serialises enough for the
    /// processor to see the new bytes. It is called anyway for two reasons. First, the Windows
    /// contract says to: Microsoft documents it as *required* after writing code, and a future
    /// Windows version (or a hypervisor, or an emulator such as the x86-on-ARM64 layer) is entitled
    /// to rely on it. Second, on every other architecture this project might one day target —
    /// ARM64 above all — I-cache coherency is **not** automatic, and code that skips the flush
    /// fails there in the worst possible way: intermittently, depending on cache pressure. Doing it
    /// unconditionally costs one syscall per compiled method.
    fn flush_icache(&self) -> io::Result<()> {
        // SAFETY: `GetCurrentProcess` returns a pseudo-handle that is always valid and needs no
        // release; the range is the one we own and have just finished writing.
        let ok = unsafe {
            FlushInstructionCache(GetCurrentProcess(), self.ptr.cast::<c_void>(), self.len)
        };
        if ok == 0 {
            return Err(io::Error::last_os_error());
        }
        Ok(())
    }
}

impl Drop for Pages {
    fn drop(&mut self) {
        // SAFETY: `self.ptr` is the exact base returned by `VirtualAlloc` (never offset, never
        // re-assigned) and has not been freed — `Pages` is not `Clone`/`Copy`, is moved rather than
        // duplicated between `CodeBuf` and `ExecMem`, and this is the only `VirtualFree` in the
        // module. `MEM_RELEASE` requires `dwSize == 0`, which is what is passed. The return value
        // is ignored deliberately: a failure here is unrecoverable and `drop` cannot report it.
        unsafe {
            VirtualFree(self.ptr.cast::<c_void>(), 0, MEM_RELEASE);
        }
    }
}

// -------------------------------------------------------------------------------------------
// State 1: writable.
// -------------------------------------------------------------------------------------------

/// A **writable, non-executable** block of pages: the buffer machine code is assembled into.
///
/// Turn it into runnable code with [`make_executable`][CodeBuf::make_executable], which consumes
/// it — that is the W^X transition, and it is one-way.
pub struct CodeBuf {
    pages: Pages,
    written: usize,
}

impl CodeBuf {
    /// A zero-filled `PAGE_READWRITE` block with room for at least `capacity` bytes.
    pub fn new(capacity: usize) -> io::Result<CodeBuf> {
        Ok(CodeBuf { pages: Pages::alloc(capacity, PAGE_READWRITE)?, written: 0 })
    }

    /// Total capacity in bytes (the page-rounded allocation, so usually more than requested).
    pub fn capacity(&self) -> usize {
        self.pages.len
    }

    /// Bytes written so far.
    pub fn len(&self) -> usize {
        self.written
    }

    /// Whether nothing has been written yet.
    pub fn is_empty(&self) -> bool {
        self.written == 0
    }

    /// Appends `bytes`. Fails with `WriteZero` rather than overflowing the allocation.
    pub fn write(&mut self, bytes: &[u8]) -> io::Result<()> {
        if bytes.len() > self.capacity() - self.written {
            return Err(io::Error::new(
                io::ErrorKind::WriteZero,
                format!(
                    "code buffer overflow: {} more bytes into {} free",
                    bytes.len(),
                    self.capacity() - self.written
                ),
            ));
        }
        // SAFETY: the bounds check above guarantees `[written, written + bytes.len())` lies inside
        // the region we own, so the destination range is valid for writes. Source and destination
        // cannot overlap: `bytes` is a caller-owned slice and these pages are freshly allocated,
        // reachable only through `&mut self`, and never handed out as a slice. The pages are
        // currently `PAGE_READWRITE`, which is an invariant of this type.
        unsafe {
            std::ptr::copy_nonoverlapping(
                bytes.as_ptr(),
                self.pages.ptr.add(self.written),
                bytes.len(),
            );
        }
        self.written += bytes.len();
        Ok(())
    }

    /// Flips the pages to `PAGE_EXECUTE_READ`, flushes the instruction cache, and hands back the
    /// executable view.
    ///
    /// Consuming `self` is what makes W^X a type-level guarantee rather than a convention: no
    /// writable handle to these pages survives the call. If `VirtualProtect` fails, the `CodeBuf`
    /// is dropped along with the error and the pages are released — there is no half-flipped state.
    pub fn make_executable(self) -> io::Result<ExecMem> {
        self.pages.protect(PAGE_EXECUTE_READ)?;
        self.pages.flush_icache()?;
        Ok(ExecMem { pages: self.pages, len: self.written })
    }
}

// -------------------------------------------------------------------------------------------
// State 2: executable.
// -------------------------------------------------------------------------------------------

/// An **executable, read-only** block of pages holding finished machine code.
///
/// Neither `Send` nor `Sync` (it holds a raw pointer), which is the conservative default; sharing
/// compiled code across threads will need an explicit, justified `unsafe impl` once there is a
/// code cache to share.
pub struct ExecMem {
    pages: Pages,
    len: usize,
}

impl ExecMem {
    /// The whole pipeline in one call: allocate RW, copy `code` in, flip to RX, flush.
    pub fn from_code(code: &[u8]) -> io::Result<ExecMem> {
        let mut buf = CodeBuf::new(code.len())?;
        buf.write(code)?;
        buf.make_executable()
    }

    /// The base address — the entry point of the first function in the block.
    pub fn as_ptr(&self) -> *const u8 {
        self.pages.ptr.cast_const()
    }

    /// Bytes of actual code (not the page-rounded allocation).
    pub fn len(&self) -> usize {
        self.len
    }

    /// Whether the block holds no code.
    pub fn is_empty(&self) -> bool {
        self.len == 0
    }

    /// Reads the code back for inspection (tests, disassembly). Read-only: the pages are RX.
    pub fn code(&self) -> &[u8] {
        // SAFETY: `ptr` is valid and readable for `len <= pages.len` bytes, `u8` has no alignment
        // requirement and no invalid bit patterns, and the borrow ties the slice's lifetime to
        // `self`, so the pages outlive it. The region is `PAGE_EXECUTE_READ`, i.e. readable.
        unsafe { std::slice::from_raw_parts(self.pages.ptr, self.len) }
    }

    /// Reinterprets the entry point as a function pointer of type `F`.
    ///
    /// # Safety
    ///
    /// The caller guarantees that:
    ///
    /// - `F` is a bare function-pointer type (`extern "system" fn(..) -> ..`), not a closure or a
    ///   `Fn` trait object. The size assert below catches the grossest violations, but a wide
    ///   pointer is not the only way to get this wrong.
    /// - `F`'s signature matches the **Microsoft x64** ABI contract the emitted code actually
    ///   implements: argument count and widths, return register, and the promise that every
    ///   non-volatile register is restored. A mismatch corrupts the caller's registers or stack
    ///   and the damage surfaces far from here.
    /// - The code at the entry point is a complete, correctly terminated function.
    ///
    /// The returned pointer must not outlive `self`; borrowing it from `&self` enforces that only
    /// as long as the caller does not copy it out (function pointers are `Copy`).
    pub unsafe fn as_fn<F: Copy>(&self) -> F {
        assert_eq!(
            std::mem::size_of::<F>(),
            std::mem::size_of::<*const u8>(),
            "F must be a thin function pointer"
        );
        // SAFETY: the size assert rules out wide pointers, and the caller has promised `F` is a
        // function-pointer type with a signature matching the emitted code. `transmute_copy` reads
        // `size_of::<F>()` bytes out of the pointer value itself (not out of the code pages).
        unsafe { std::mem::transmute_copy::<*const u8, F>(&self.as_ptr()) }
    }

    /// Convenience wrapper for `extern "system" fn() -> i64`.
    ///
    /// # Safety
    ///
    /// As [`as_fn`][ExecMem::as_fn]: the code must really be a zero-argument function returning an
    /// integer in RAX and preserving the non-volatile registers.
    pub unsafe fn as_fn0(&self) -> extern "system" fn() -> i64 {
        // SAFETY: forwarded to the caller by this function's own `# Safety` contract.
        unsafe { self.as_fn() }
    }

    /// Convenience wrapper for `extern "system" fn(i64) -> i64` (argument in RCX).
    ///
    /// # Safety
    ///
    /// As [`as_fn`][ExecMem::as_fn].
    pub unsafe fn as_fn1(&self) -> extern "system" fn(i64) -> i64 {
        // SAFETY: forwarded to the caller by this function's own `# Safety` contract.
        unsafe { self.as_fn() }
    }

    /// Convenience wrapper for `extern "system" fn(i64, i64) -> i64` (RCX, RDX).
    ///
    /// # Safety
    ///
    /// As [`as_fn`][ExecMem::as_fn].
    pub unsafe fn as_fn2(&self) -> extern "system" fn(i64, i64) -> i64 {
        // SAFETY: forwarded to the caller by this function's own `# Safety` contract.
        unsafe { self.as_fn() }
    }

    /// Convenience wrapper for `extern "system" fn(i64, i64, i64) -> i64` (RCX, RDX, R8).
    ///
    /// # Safety
    ///
    /// As [`as_fn`][ExecMem::as_fn].
    pub unsafe fn as_fn3(&self) -> extern "system" fn(i64, i64, i64) -> i64 {
        // SAFETY: forwarded to the caller by this function's own `# Safety` contract.
        unsafe { self.as_fn() }
    }

    /// Convenience wrapper for `extern "system" fn(i64, i64, i64, i64) -> i64` (RCX, RDX, R8, R9).
    ///
    /// # Safety
    ///
    /// As [`as_fn`][ExecMem::as_fn].
    pub unsafe fn as_fn4(&self) -> extern "system" fn(i64, i64, i64, i64) -> i64 {
        // SAFETY: forwarded to the caller by this function's own `# Safety` contract.
        unsafe { self.as_fn() }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn page_rounding() {
        assert_eq!(page_round_up(0), 4096); // never zero: VirtualAlloc(0) fails
        assert_eq!(page_round_up(1), 4096);
        assert_eq!(page_round_up(4096), 4096);
        assert_eq!(page_round_up(4097), 8192);
    }

    #[test]
    fn write_then_read_back() {
        let mut buf = CodeBuf::new(16).unwrap();
        assert!(buf.is_empty());
        buf.write(&[0x48, 0x89, 0xC8]).unwrap();
        buf.write(&[0xC3]).unwrap();
        assert_eq!(buf.len(), 4);
        assert!(buf.capacity() >= 4096);
        let mem = buf.make_executable().unwrap();
        assert_eq!(mem.code(), &[0x48, 0x89, 0xC8, 0xC3]);
        assert_eq!(mem.len(), 4);
        assert!(!mem.is_empty());
        assert!(!mem.as_ptr().is_null());
    }

    #[test]
    fn overflow_is_refused_not_silently_truncated() {
        let mut buf = CodeBuf::new(1).unwrap();
        let cap = buf.capacity();
        let err = buf.write(&vec![0x90; cap + 1]).unwrap_err();
        assert_eq!(err.kind(), io::ErrorKind::WriteZero);
        // The failed write left nothing behind.
        assert_eq!(buf.len(), 0);
        // Exactly filling it is fine.
        buf.write(&vec![0x90; cap]).unwrap();
        assert_eq!(buf.len(), cap);
    }

    /// The minimum viable proof that the RW -> RX flip actually produces runnable code:
    /// `mov rax, rcx; ret` is the identity function under the Microsoft x64 ABI.
    #[test]
    fn executes_after_the_wx_flip() {
        let mem = ExecMem::from_code(&[0x48, 0x89, 0xC8, 0xC3]).unwrap();
        // SAFETY: the bytes are exactly `mov rax, rcx; ret` -- one integer argument read from RCX,
        // returned in RAX, no memory touched, no non-volatile register modified, and terminated by
        // a `ret`. That is precisely `extern "system" fn(i64) -> i64`.
        let f = unsafe { mem.as_fn1() };
        assert_eq!(f(42), 42);
        assert_eq!(f(-1), -1);
        assert_eq!(f(i64::MAX), i64::MAX);
    }

    /// Allocate, execute and drop many blocks: if `Drop` leaked the reservation or freed the wrong
    /// base, this would fail long before the loop ends.
    #[test]
    fn many_blocks_are_allocated_and_released() {
        for i in 0..256i64 {
            let mem = ExecMem::from_code(&[0x48, 0x89, 0xC8, 0xC3]).unwrap();
            // SAFETY: same identity function as above.
            let f = unsafe { mem.as_fn1() };
            assert_eq!(f(i), i);
        }
    }

    #[test]
    #[should_panic(expected = "thin function pointer")]
    fn fat_pointer_types_are_rejected() {
        let mem = ExecMem::from_code(&[0xC3]).unwrap();
        // SAFETY: this call is expected to panic on the size assert before any transmute or call
        // happens; `&[u8]` is a wide pointer, exactly what the assert exists to catch.
        let _: &[u8] = unsafe { mem.as_fn() };
    }
}
