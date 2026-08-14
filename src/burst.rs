//! `burst` — the optimizer (roadmap Phase F). This is milestone **F3, the JIT**.
//!
//! Four pieces. `burst` still does not depend on `crate::jvm` — the interpreter reaches *in*, never
//! the other way round — so the naive interpreter stays the correctness oracle it is meant to be:
//!
//! - [`x64`] — a small x86-64 assembler that emits into a `Vec<u8>`: the frame-local integer
//!   subset (moves, arithmetic, shifts, compares, branches with forward labels, prologue/epilogue).
//! - [`exec_mem`] — W^X executable memory: pages are allocated **RW**, written, flipped to **RX**,
//!   and only then called through a typed function pointer. Windows-only (`VirtualAlloc` &
//!   friends), hence the `cfg` — `x64` itself is pure byte emission and builds anywhere, so its
//!   encoding tests still run on a Linux/WSL build.
//! - [`compile`] (**step 2**) — the bytecode → native compiler: which methods qualify, how the
//!   operand stack becomes frame slots, the three `int`-semantics traps, and the deopt protocol.
//! - [`code_cache`] (**step 2**) — who owns compiled code, the invocation counter, and the
//!   marshalling convention the interpreter crosses on.
//!
//! # What the JIT is allowed to run on
//!
//! `green`: yes. One OS thread, and while native code runs no interpreter opcode executes, so a GC
//! cannot happen underneath it. `os-gil`: yes — the calling thread holds the GIL for the whole
//! native call, which is the same guarantee. **`os` (parallel): no.** A compiled method cannot poll
//! a safepoint, so a long loop inside one would stall a stop-the-world handshake for every other
//! thread; the JIT is simply switched off there until back-edge safepoint polling exists.
//!
//! That asymmetry is a gift rather than a limitation. The project already validates every change
//! against the `green ≡ os-gil ≡ os` oracle; with the JIT **on** in green and **off** in `os`, that
//! oracle *is* the differential test the roadmap asks `burst` for — if compiled code ever computed
//! something the interpreter does not, the existing suite says so.
//!
//! # ABI: Microsoft x64 (not System V)
//!
//! Every function this module builds follows the **Windows x64 calling convention**, because that
//! is what `extern "system"` means on this target and it is what a Rust caller will use:
//!
//! | | |
//! |---|---|
//! | integer/pointer args | `RCX`, `RDX`, `R8`, `R9` (in that order); the rest on the stack |
//! | integer return | `RAX` |
//! | volatile (scratch) | `RAX`, `RCX`, `RDX`, `R8`–`R11` — free to clobber |
//! | non-volatile | `RBX`, `RBP`, `RDI`, `RSI`, `RSP`, `R12`–`R15` — **must** be saved/restored |
//! | shadow space | the **caller** reserves 32 bytes above the return address for the callee to
//!   spill `RCX`/`RDX`/`R8`/`R9` into, even if it passes fewer args |
//! | alignment | `RSP` is 16-byte aligned **at the `call`**, so a function sees `RSP % 16 == 8` on
//!   entry (the return address the `call` just pushed) |
//!
//! This is where everyone trips: System V passes in `RDI`/`RSI`/`RDX`/`RCX`/`R8`/`R9`, treats
//! `RDI`/`RSI` as volatile, and has no shadow space. Emitting a System V frame here would appear
//! to work for leaf functions and corrupt the caller's stack the moment we make a call.
//! [`x64::Frame`] encodes the whole rule set (see its docs).

pub mod x64;

#[cfg(windows)]
pub mod exec_mem;

pub mod code_cache;
pub mod compile;

/// End-to-end tests: functions assembled by [`x64`], mapped by [`exec_mem`], and **actually run**.
/// Windows-only, since they need real executable memory.
#[cfg(all(test, windows))]
mod tests;

/// End-to-end tests of the *compiler*: bytecode in, machine code out, mapped and **executed**,
/// with the three `int`-semantics traps checked against the values the JLS mandates. Windows-only
/// for the same reason as [`tests`].
#[cfg(all(test, windows))]
mod compile_tests;

/// The **differential tests**: real Java workloads run through the whole VM twice — once with the
/// JIT on, once with it off — asserting the same answer. Windows-only (there is nothing to compare
/// against elsewhere: the JIT is inert).
#[cfg(all(test, windows))]
mod jit_tests;
