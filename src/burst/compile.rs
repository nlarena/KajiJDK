//! The **bytecode → native compiler**: milestone F3, step 2.
//!
//! Step 1 built the machine that writes x86-64 ([`x64`][crate::burst::x64]) and the W^X pages to
//! run it from ([`exec_mem`][crate::burst::exec_mem]). This module is the part that decides *what*
//! to write: it takes a method's `code[]` and produces either machine code or a reason the method
//! is not eligible.
//!
//! # The subset, and why it is drawn there
//!
//! Only methods whose **entire** body lies in this whitelist are compiled:
//!
//! | group | opcodes |
//! |---|---|
//! | constants | `iconst_m1`…`iconst_5`, `bipush`, `sipush`, `ldc`/`ldc_w` **of an `Integer`** |
//! | locals | `iload`, `iload_0..3`, `istore`, `istore_0..3`, `iinc`, and all three under `wide` |
//! | arithmetic | `iadd`, `isub`, `imul`, `idiv`, `irem`, `ineg` |
//! | bits & shifts | `iand`, `ior`, `ixor`, `ishl`, `ishr`, `iushr` |
//! | control flow | `if_icmp<cond>`, `if<cond>`, `goto`, `tableswitch`, `lookupswitch` |
//! | stack | `nop`, `pop`, `pop2`, `dup`, `dup_x1`, `dup_x2`, `dup2`, `dup2_x1`, `dup2_x2`, `swap` |
//! | heap (read-only) | `getstatic` **of an `int`, in an already-initialised class** |
//! | exit | `ireturn` |
//!
//! Anything else — one single byte — makes the method permanently ineligible. That is the whole
//! safety argument: such a method touches **no field it can write, no array, no reference, no
//! other method**. Everything but `getstatic` is a *pure function of its `int` locals*, and
//! `getstatic` is a *pure read* — see below. Nothing any of it does is observable except the
//! return value, which is what makes both the marshalling convention and the deopt protocol below
//! sound.
//!
//! # Step 4: what widened the subset, and what each addition had to prove
//!
//! ## `wide` (`iload`/`istore`/`iinc` with a 16-bit index, `iinc` with a 16-bit constant)
//!
//! Purely frame-local — the same three instructions with a wider operand field, and `javac` emits
//! the wide `iinc` for something as ordinary as `x += 256`. Nothing about the purity argument
//! changes; the only new thing to get right is the 16-bit *signed* `iinc` constant, which still
//! goes through the same `add`/`movsxd` pair as the narrow form (trap 1).
//!
//! ## The rest of the stack shuffles
//!
//! `nop`, `pop2` and the five remaining `dup` forms. The operand stack is a **compile-time**
//! notion here (position `k` is frame slot `k`), so these are permutations of slots: `nop`,
//! `pop2` and `pop` emit nothing at all, and the `dup` family emits only `mov`s.
//!
//! Their JVMS definitions are *shape-dependent* — `dup2` duplicates either two category-1 values
//! or one category-2 value, and there is no opcode-level way to tell which. **In this subset the
//! question never arises**: every opcode that pushes pushes an `int`, so every operand-stack slot
//! holds a category-1 value, always. The category-1 reading is therefore not a guess but the only
//! possible one. The moment a category-2 value can reach the operand stack — the `long` step —
//! every one of these six opcodes has to be revisited, which is exactly why the guarantee is
//! written down here rather than assumed.
//!
//! ## `tableswitch` / `lookupswitch`
//!
//! Still perfectly pure — a multi-way `goto` on an `int`. Three details do the work: the 0–3 bytes
//! of **padding** that align the operand table to a 4-byte boundary *of the code array*, the
//! branch offsets being 4 bytes and relative to the **opcode** (not to the table), and the
//! `default` arm, which is a branch target like any other and must be walked by the scan.
//!
//! Both compile to a **compare chain** (`cmp`/`je` per case, then `jmp default`) rather than to a
//! jump table. A table would need a rip-relative data island inside the code page, and this
//! assembler deliberately has no encoding for one — the same reason the OSR entry dispatch is a
//! compare chain. So the chain is O(cases) where a table is O(1); the answer for now is
//! correctness plus a cap ([`MAX_SWITCH_CASES`]) that keeps the emitted chain bounded, and a real
//! jump table is a later step with an assembler change in front of it.
//!
//! ## `getstatic` of an `int` — a heap read that does not cost the purity argument
//!
//! A `getstatic` is a **read**. Re-executing the method re-reads the same location and gets the
//! same value, because in `green`/`os-gil` — the only substrates where the JIT is on — no other
//! thread runs a single opcode while a native frame is on this thread's stack. So deopt-by-restart
//! stays valid, and so does the OSR contract.
//!
//! What it does need is an **address that cannot move**, since the compiler bakes one in as an
//! immediate. Two facts make that true, and both are properties of code elsewhere rather than of
//! this module:
//!
//! 1. A class's statics live in its `Class<…>` mirror, which is allocated with `malloc_old` and is
//!    in the **pinned set** that `gc::compact` refuses to relocate. So the mirror's *heap offset*
//!    is fixed for the life of the VM.
//! 2. The heap's byte region is a `Vec` **pre-reserved to the maximum heap size at startup**, so
//!    it never reallocates while growing and every offset's *address* is stable too.
//!
//! And it needs the class to be **initialised**: the resolver is only allowed to answer with an
//! address once the declaring class's `<clinit>` has run, because compiled code has no way to
//! trigger one. A method reading a static of a class that is not yet initialised simply does not
//! compile — and since ineligibility is cached forever, that is a deliberate loss (the method may
//! well be compilable later) traded for not putting a class-init barrier in native code.
//!
//! The load is `movsxd r, dword [addr]`: a static `int` occupies 4 bytes in the mirror (the
//! interpreter's `putstatic` writes it with `write_u32`), and sign-extending it on the way in is
//! what re-establishes the normalisation invariant below.
//!
//! ## `putstatic` — deliberately left out
//!
//! A `putstatic` is the first **observable side effect** the subset would have contained, and it
//! breaks deopt-by-restart outright: a method that stores and *then* meets a zero divisor would,
//! on re-execution, apply the store twice. Two ways out were considered.
//!
//! *Compile it only in methods with no restart-style deopt site* (today: no `idiv`/`irem`) is
//! sound and easy to check. *Turn those deopts into resume-at-pc* is not available: the resume
//! mechanism the safepoint poll uses requires an **empty operand stack**, and a zero divisor
//! happens mid-expression with operands live.
//!
//! The first rule would work, but it buys very little and costs the one-line invariant that makes
//! this whole tier reviewable — "compiled code writes nothing observable" would become "compiled
//! code writes nothing observable *unless* it cannot deopt", and every future guard (a class-init
//! check, a type guard, an allocation) would have to re-derive its interaction with that
//! exception. The census says `putstatic` is the *first* blocker in 6 methods of 682, all of them
//! `<clinit>`s that are also full of things outside the subset. So it stays out until compiled
//! code has a real deoptimisation mechanism (reconstructing an interpreter frame at a pc, operand
//! stack included) rather than a restart.
//!
//! ## `long` arithmetic — out, and blocked on the calling convention rather than on effort
//!
//! `lload`/`ladd`/`lshl`/… map beautifully onto x86-64: they are native 64-bit operations, they
//! need no normalisation (the whole register *is* the value), and x86 masks a 64-bit shift count
//! to 6 bits exactly as JLS §15.19 does for `long`. The work is in the slot mapping — a `long` is
//! **category 2**, so it occupies two local slots and two operand-stack slots, which every index
//! calculation in both passes would have to account for, and it is also what makes the six `dup`
//! forms above ambiguous again.
//!
//! But none of that is what stops it. **`lreturn` does not fit the return protocol.** The packed
//! `RAX = (status << 32) | value` below spends the high half on the status, leaving 32 bits for
//! the result — enough for every `int`, and not enough for any `long`. Adding `long` therefore
//! means changing the boundary itself (a second return register, or an out-pointer, and with it
//! the marshalling contract, the OSR write-back and every `unsafe` block that crosses it), which
//! is a step of its own rather than a widening of this one.
//!
//! ## Constant-folded shift counts — out, for now
//!
//! `x >> 7` currently materialises the 7 into a stack slot, loads it into `CL` and masks it, where
//! `sar r, 7` would do. The peephole itself is easy and safe (the count must be pushed by the
//! immediately preceding instruction, and that pc must not be a branch target — and a shift can
//! never be an OSR entry, since those have an empty operand stack). What it needs is a
//! **shift-by-immediate encoding in the assembler**, which does not exist yet; and the win is two
//! instructions against an operand stack that lives entirely in memory, which is the thing
//! register allocation is about to change anyway. Better done on top of that than under it.
//!
//! `ldc`/`ldc_w` restricted to `CONSTANT_Integer` is the one addition to the brief's list, and it
//! is not a widening of the safety argument: the constant is read from the pool **at compile time**
//! and baked into the instruction stream as an immediate, so the generated code never touches the
//! constant pool at all. Without it the subset cannot express an `int` outside `[-32768, 32767]`,
//! which excludes essentially every real loop bound — `BmLoop`'s own `i < 900000` compiles to
//! `ldc #7`. A `ldc` naming anything else (a `String`, a `float`, a class literal) is rejected like
//! any other unsupported opcode.
//!
//! # The three semantics traps
//!
//! The emitter works in 64-bit registers; a Java `int` is 32-bit with wraparound. Three places
//! where the difference is not academic, and what this module does about each:
//!
//! ## 1. The normalisation invariant
//!
//! **Every `int` this code manipulates — in a register, in a stack slot, in a local slot — is the
//! sign-extension of its 32-bit value.** Equivalently: `v == (v as i32) as i64`, always.
//!
//! It is established on the way in (the interpreter marshals `Value::Int(v)` as `v as i64`, which
//! sign-extends) and re-established after every operation that can break it. Exactly three kinds
//! can: `iadd`/`isub`/`imul`/`ineg`/`iinc` (the 64-bit result may exceed 32 bits) and `ishl` (bits
//! shift up past bit 31) and `iushr` (which deliberately breaks it, then repairs it). Each is
//! followed by `movsxd r, r32` — "take the low 32 bits, sign-extend" — which *is* the JLS's
//! wraparound: `Integer.MAX_VALUE + 1` computes `0x8000_0000` in 64 bits and `movsxd` turns that
//! into `Integer.MIN_VALUE`.
//!
//! `iand`/`ior`/`ixor` need no repair, and that is a proof rather than an assumption: if both
//! operands are sign-extended, all 33 top bits of each are copies of bit 31, so the bitwise result
//! also has all 33 top bits equal — i.e. it is sign-extended too. `ishr` (arithmetic) likewise maps
//! `[-2^31, 2^31)` into itself. `movsxd` is emitted after `ishr` anyway: three bytes to make the
//! invariant hold *unconditionally* after every arithmetic opcode, rather than by case analysis.
//!
//! ## 2. Shift counts
//!
//! Java masks an `int` shift count to **5 bits** (JLS §15.19): `x << 33 == x << 1`. x86 masks to
//! 5 bits for 32-bit operands but to **6** for 64-bit ones — so a 64-bit `shl` by 33 shifts by 33.
//! Every shift here therefore emits an explicit `and rcx, 31` before the shift. The masking is in
//! the instruction stream, not implied by an operand size.
//!
//! `iushr` has a second half: it is a **logical** shift of the *32-bit* value. Applied to a
//! sign-extended `-1` (`0xFFFF_FFFF_FFFF_FFFF`), a 64-bit `shr` by 1 yields `0x7FFF_FFFF_FFFF_FFFF`,
//! whose low 32 bits are `0xFFFF_FFFF` = `-1` — not `Integer.MAX_VALUE`. So `iushr` first
//! *zero*-extends the low 32 bits (`mov eax, dword [slot]`), shifts, and then re-canonicalises with
//! `movsxd`.
//!
//! ## 3. Division
//!
//! `INT_MIN / -1` overflows a 32-bit `idiv` and raises `#DE`. Doing the division in **64 bits**
//! removes the problem instead of branching around it: the quotient `2^31` is representable, and
//! the mandatory `movsxd` truncates it to `INT_MIN` — exactly the wraparound JLS §15.17.2
//! prescribes. (`INT_MIN % -1` likewise yields `0`.)
//!
//! A **zero divisor** is the other `#DE`, and on Windows it arrives as a structured exception, not
//! a `java.lang.ArithmeticException`. Rather than emit exception machinery in native code, the
//! compiler emits an explicit `cmp rcx, 0; je deopt` — see below.
//!
//! # Deopt: the protocol, and why it is sound
//!
//! Because the subset is side-effect free, native code that meets a condition it cannot handle may
//! simply **abandon the attempt** and let the interpreter run the method from the beginning.
//! Re-execution is indistinguishable from a first execution: nothing observable was written. (The
//! locals buffer *is* written — `istore`/`iinc` write through it — but it is scratch memory owned
//! by the caller and refilled from the interpreter frame on every entry. The interpreter's own
//! `Frame` is never touched.)
//!
//! The compiled function is
//!
//! ```text
//!   extern "system" fn(locals: *mut i64, entry_pc: i64) -> i64
//! ```
//!
//! and packs **status and value into the one return register**:
//!
//! ```text
//!   RAX = (status << 32) | (value as u32)
//!   status 0 = returned normally, value is the `ireturn` operand
//!   status 1 = deopt, value meaningless
//!   status 2 = safepoint, value is the **bytecode pc** to resume interpreting at
//! ```
//!
//! One register, no out-pointer, and the common case costs one instruction: `ireturn` is
//! `mov eax, dword [slot]`, whose zero-extension simultaneously loads the value and writes status
//! 0 into the high half. Deopt is `mov rax, 1<<32`. See [`Status`].
//!
//! `idiv`/`irem` by zero is the only deopt site. The protocol exists so the next guards (a
//! class-init check, a type guard) can be added without changing the interpreter side of the
//! boundary.
//!
//! # OSR and the safepoint poll: entering and leaving in the middle
//!
//! Step 3 adds the two halves of one mechanism — crossing the boundary at a **loop header**
//! instead of only at a method's entry and exit.
//!
//! ## The simplification that makes it tractable
//!
//! Both directions happen **only at back-edge targets whose operand-stack depth is 0**. The scan
//! already recomputes that depth at every pc, so the set is known at compile time (it is
//! [`CompiledCode::osr_entries`]). A `while`/`for` loop's back-edge always lands on an empty
//! stack, so this covers essentially every real loop — and it means the state to transfer, in
//! *both* directions, is only **the locals buffer plus a bytecode pc**. No operand stack is ever
//! reconstructed. A back-edge whose target has a non-empty stack is simply not an entry and not a
//! poll site; a method with no eligible back-edge behaves exactly as it did in step 2.
//!
//! Nothing has to be marshalled *out* on either exit, either: `istore`/`iinc` write straight
//! through to `[rbx + 8i]`, so by the time native code leaves, the caller's buffer already holds
//! the current locals. The interpreter copies them back into its `Frame` — see
//! [`code_cache`][super::code_cache].
//!
//! ## Entering (OSR)
//!
//! The second ABI argument (`RDX`) is the **bytecode pc to start at**; `0` means "the beginning",
//! which is what an ordinary invocation passes. When the method has entry points the prologue
//! emits one `cmp rdx, <pc>; je <that pc>` per entry and then falls through to pc 0. A compare
//! chain rather than a jump table because the count is one or two in practice (one per loop), the
//! whole chain runs once per *entry to native code* rather than per iteration, and a table would
//! need a rip-relative data island in the code page that this assembler deliberately has no
//! encoding for.
//!
//! ## Leaving (the safepoint poll)
//!
//! At each of those same pcs — i.e. at the top of every eligible loop header, which is executed
//! exactly once per iteration — the code checks a **poll word** and, if it is non-zero, returns
//! `status = 2` with that pc. The interpreter then resumes the method *interpreted* from there and
//! reaches its safepoint by the ordinary path; nothing about the GC has to be reachable from
//! native code. When the method next gets hot again it re-enters by OSR.
//!
//! Putting the poll at the loop **header** rather than at the back-edge itself is what keeps it to
//! one site per loop and needs no branch inversion: every back-edge to that header passes through
//! it, and the state there is exactly the state the entry contract already describes.
//!
//! The poll word's address is baked into the instruction stream as an immediate, so it **must not
//! move**. See [`code_cache::JitCache::poll_word`][super::code_cache::JitCache::poll_word] for
//! where it lives and why that address is stable.
//!
//! # Frame and stack mapping
//!
//! - **Locals** stay in the caller's buffer: `RBX` holds the incoming pointer and local `i` is
//!   `[rbx + 8i]`. No copy in or out — a compiled method returns its value, never its locals.
//! - **The operand stack** is *not* dynamic. Its depth is statically known at every pc (that is
//!   what a `StackMapTable` records, and [`scan`] recomputes it from the control-flow graph rather
//!   than trusting the attribute), so stack position `k` is simply the native frame slot `k`:
//!   `[rsp + 32 + 8k]`. Pushes and pops become nothing at all — only the compiler's idea of the
//!   current depth changes.
//!
//! A first tier deliberately keeps operands in memory rather than in registers: every slot is in
//! L1 and store-to-load forwarding covers the round trip, while a register-allocated operand stack
//! would have to negotiate with `idiv`'s fixed `RDX:RAX` and the shifts' fixed `CL`. Register
//! allocation is the next step, not this one.

use std::collections::BTreeSet;

use super::x64::{Asm, AsmError, Cond, Label, Mem, Reg};

/// The register holding the caller's locals buffer for the whole body. Callee-saved, so the
/// prologue/epilogue pair saves and restores it; `RCX` (where the ABI delivers it) is needed as
/// the shift count register.
const LOCALS: Reg = Reg::Rbx;

/// The register holding the **safepoint poll word's address** for the whole body, loaded once in
/// the prologue. Callee-saved, and only saved (and only loaded) when the method has at least one
/// poll site — keeping the address in a register turns each poll into `mov` + `cmp` + `jcc`
/// instead of re-materialising a 10-byte `movabs` immediate on every loop iteration.
const POLL: Reg = Reg::Rsi;

/// Scratch. Never live across an instruction boundary — every bytecode opcode loads what it needs,
/// computes, and stores back to a slot.
const T0: Reg = Reg::Rax;
/// Second scratch. Also the *mandatory* shift-count register (`shl/shr/sar` read `CL`) and the
/// divisor register for `idiv`.
const T1: Reg = Reg::Rcx;
/// Third scratch. Clobbered by `cqo`/`idiv` (which write the remainder there), so nothing may be
/// live in it across a division.
const T2: Reg = Reg::Rdx;

/// How a native call ended — the decoded form of the packed return value (see the module docs).
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum Outcome {
    /// Ran to an `ireturn`; this is the method's result.
    Returned(i32),
    /// Gave up (today: a zero divisor). The interpreter must run the method itself; because the
    /// subset writes nothing observable, doing so from the *start* — or from the pc the native
    /// code was entered at — is indistinguishable from never having tried.
    Deopt,
    /// The safepoint poll fired. The locals buffer holds the current locals and this is the
    /// bytecode pc to resume interpreting at.
    Safepoint(u32),
}

/// The status half of the packed return value (see the module docs).
pub struct Status;

impl Status {
    /// Bits the status occupies: the high half of `RAX`.
    pub const SHIFT: u32 = 32;
    /// The method ran to an `ireturn`; the low 32 bits are the result.
    pub const OK: i64 = 0;
    /// The method gave up; the interpreter must run it itself.
    pub const DEOPT: i64 = 1;
    /// The poll fired; the low 32 bits are the bytecode pc to resume at.
    pub const SAFEPOINT: i64 = 2;

    /// The exact `i64` a deopting function returns — `mov rax, <this>`.
    pub const DEOPT_VALUE: i64 = Status::DEOPT << Status::SHIFT;

    /// The exact `i64` a safepoint exit at bytecode `pc` returns.
    pub const fn safepoint_value(pc: u32) -> i64 {
        (Status::SAFEPOINT << Status::SHIFT) | pc as i64
    }

    /// Decodes a returned `i64`.
    ///
    /// This is the *only* place the packing is interpreted, so the encoding above and the
    /// decoding here cannot drift apart. An unknown status decodes as [`Outcome::Deopt`]: no
    /// emitted code produces one, and "the interpreter runs it" is the answer that is safe for
    /// every possible state.
    pub fn unpack(raw: i64) -> Outcome {
        match raw >> Status::SHIFT {
            Status::OK => Outcome::Returned(raw as i32),
            Status::SAFEPOINT => Outcome::Safepoint(raw as u32),
            _ => Outcome::Deopt,
        }
    }
}

/// Why a method will never be compiled. Every variant is a *permanent* property of the bytecode,
/// so the answer is cached once and the scan never repeated.
#[derive(Clone, PartialEq, Eq, Debug)]
pub enum Ineligible {
    /// An opcode outside the whitelist (the overwhelmingly common answer).
    Opcode { pc: usize, opcode: u8 },
    /// `ldc`/`ldc_w` naming something that is not a `CONSTANT_Integer`.
    NonIntegerConstant { pc: usize, index: u16 },
    /// A `getstatic` the resolver would not give an address for: the field is not a static `int`,
    /// or its declaring class is not initialised yet. Unlike every other variant this one is a
    /// property of the *VM's state* as well as of the bytecode, so caching it means a method that
    /// would compile after the class initialises never gets a second look. That is the deliberate
    /// price of keeping a class-init barrier out of native code — see the module docs.
    UnresolvedStatic { pc: usize, index: u16 },
    /// A branch target outside the code array, or an instruction whose operand bytes run off
    /// the end.
    OutOfRange { pc: usize },
    /// Two paths reach the same pc with different operand-stack depths. Legal bytecode never does
    /// this (JVMS §4.10.1), so it means the walk lost track — bail rather than guess.
    StackMismatch { pc: usize, seen: u16, found: u16 },
    /// An opcode wanted more operands than the statically-known depth provides.
    StackUnderflow { pc: usize },
    /// Instructions overlap — a branch landed *inside* another instruction. Impossible from
    /// `javac`, possible from a hand-written or hostile class file.
    OverlappingInstructions { pc: usize },
    /// A local index at or beyond `max_locals`.
    LocalOutOfRange { pc: usize, slot: usize },
    /// Past the size this tier is willing to compile (code length or operand-stack depth).
    TooBig,
    /// The assembler refused to encode something — a compiler bug, surfaced rather than panicked.
    Assembler(AsmError),
}

impl std::fmt::Display for Ineligible {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Ineligible::Opcode { pc, opcode } => write!(f, "opcode 0x{opcode:02x} at {pc} is outside the compiled subset"),
            Ineligible::NonIntegerConstant { pc, index } => write!(f, "ldc #{index} at {pc} is not an integer constant"),
            Ineligible::UnresolvedStatic { pc, index } => {
                write!(f, "getstatic #{index} at {pc} is not an int static of an initialised class")
            }
            Ineligible::OutOfRange { pc } => write!(f, "instruction or branch at {pc} leaves the code array"),
            Ineligible::StackMismatch { pc, seen, found } => {
                write!(f, "pc {pc} is reached with stack depth {seen} and {found}")
            }
            Ineligible::StackUnderflow { pc } => write!(f, "operand stack underflow at {pc}"),
            Ineligible::OverlappingInstructions { pc } => write!(f, "an instruction boundary falls inside another at {pc}"),
            Ineligible::LocalOutOfRange { pc, slot } => write!(f, "local {slot} at {pc} is past max_locals"),
            Ineligible::TooBig => write!(f, "method is larger than this tier compiles"),
            Ineligible::Assembler(e) => write!(f, "assembler: {e}"),
        }
    }
}

impl std::error::Error for Ineligible {}

/// Finished machine code plus what the caller needs to invoke it. Deliberately *not* an
/// [`ExecMem`][crate::burst::exec_mem::ExecMem]: mapping pages is Windows-only, while everything
/// here is plain byte emission, so the compiler and its tests build and run anywhere.
#[derive(Clone, PartialEq, Eq, Debug)]
pub struct CompiledCode {
    /// The machine code, labels resolved — ready for `ExecMem::from_code`.
    pub code: Vec<u8>,
    /// The local slots the body actually reads or writes, ascending. **The marshalling contract**:
    /// the caller must fill exactly these before the call, and may leave every other slot alone —
    /// the generated code provably never reads them.
    ///
    /// This is what lets an instance method with an unused `this` compile: slot 0 holds a
    /// `Value::Reference` the caller could not marshal, but if it is not in this list nothing ever
    /// looks at it.
    pub touched_locals: Vec<u16>,
    /// Number of 8-byte operand-stack slots the native frame reserves (the method's true maximum
    /// depth, recomputed here rather than read from `max_stack`).
    pub stack_slots: u32,
    /// **Loop headers**: the bytecode pcs this code may be entered at on-stack, and the same pcs
    /// at which it polls the safepoint word. Ascending, and always the target of some backward
    /// branch with an operand-stack depth of 0 (see the module docs). Empty for a method with no
    /// loop, or whose only loops carry operands across their back-edge — such a method compiles
    /// and runs exactly as it did before OSR existed.
    pub osr_entries: Vec<u32>,
}

/// The largest method this tier compiles, in bytes of bytecode. A first-tier JIT exists for hot
/// *loops*, which are small; the bound keeps compile time and code size bounded by construction
/// and makes the `Vec`s the scan allocates (one entry per code byte) trivially cheap.
const MAX_CODE_LEN: usize = 4096;

/// The deepest operand stack this tier maps to frame slots. `javac` emits single digits for the
/// arithmetic this subset covers; the bound just stops a pathological class file from asking for
/// a megabyte-deep frame.
const MAX_STACK_SLOTS: u16 = 64;

/// The most cases this tier will turn into a compare chain. A `tableswitch`/`lookupswitch` with
/// more is [`Ineligible::TooBig`] rather than a kilobyte of `cmp`/`je` — the honest answer while
/// the switch has no jump table (see the module docs). A method of [`MAX_CODE_LEN`] bytes cannot
/// hold much more than a thousand cases anyway, so this bites only on the pathological end.
pub(super) const MAX_SWITCH_CASES: usize = 256;

// ---------------------------------------------------------------------------------------------
// Pass 1: the scan. Decide eligibility, and recover the operand-stack depth at every pc.
// ---------------------------------------------------------------------------------------------

/// What one whitelisted instruction does, as far as the scan is concerned.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum Flow {
    /// Falls through to the next instruction and nothing else.
    Next,
    /// Falls through *or* branches to the target.
    Branch(usize),
    /// Jumps unconditionally; does not fall through.
    Goto(usize),
    /// A `tableswitch`/`lookupswitch`: jumps to one of many targets and does not fall through.
    /// The targets are **not** carried here — there can be hundreds of them and `Insn` is `Copy`,
    /// so both the scan and the emitter re-read them with [`switch_layout`], which is a pure
    /// function of `(code, pc)` and therefore cannot disagree with itself between the two passes.
    Switch,
    /// Leaves the method.
    Return,
}

/// One decoded instruction.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
struct Insn {
    /// Length in bytes. `u16` rather than `u8` because a `tableswitch` is as long as its table —
    /// bounded only by [`MAX_CODE_LEN`].
    len: u16,
    /// Operands consumed from the stack.
    pops: u16,
    /// Values left on the stack.
    pushes: u16,
    flow: Flow,
}

/// Whether control can reach the byte after this instruction. Written once because both passes ask
/// — the scan to decide whether running off the end is possible, the emitter to decide whether it
/// owes the next instruction a bridging `jmp` — and a disagreement between them would be code that
/// runs into the wrong place.
fn falls_through(flow: Flow) -> bool {
    matches!(flow, Flow::Next | Flow::Branch(_))
}

/// The result of the scan: everything pass 2 needs, and proof that pass 2 can run at all.
struct Scan {
    /// Operand-stack depth on entry to each reachable instruction; `None` for a byte that is not
    /// a reachable instruction start.
    depth: Vec<Option<u16>>,
    /// Reachable instruction starts, ascending.
    order: Vec<usize>,
    /// Decoded form of each reachable instruction, parallel to `order`.
    insns: Vec<Insn>,
    /// Deepest the operand stack ever gets.
    max_depth: u16,
    /// Local slots read or written anywhere in the body.
    touched: BTreeSet<u16>,
    /// Targets of **backward** branches whose operand-stack depth is 0 — the loop headers that
    /// become OSR entry points and safepoint poll sites. A `BTreeSet` so the order is the pc
    /// order, which is what the entry dispatch and the interpreter's lookup both want.
    osr: BTreeSet<usize>,
}

/// What [`switch_layout`] recovers from a `tableswitch`/`lookupswitch`: its **total length** in
/// bytes, the pc of its `default` arm, and its `(match value, target pc)` pairs in table order.
type SwitchLayout = (u16, usize, Vec<(i32, usize)>);

/// Decodes a `tableswitch` (0xaa) or `lookupswitch` (0xab) at `pc` into its **total length**, its
/// `default` target, and its `(match, target)` pairs — everything both passes need.
///
/// Three things here are easy to get wrong and are the whole reason this is one shared function:
///
///  - **Padding.** The operand table starts at the next multiple of 4 *counted from the start of
///    the code array*, so the 0–3 pad bytes depend on `pc`, not on anything local.
///  - **Offsets are 4 bytes and relative to the opcode**, like every other Java branch (and unlike
///    x86's rel32, which is relative to the next instruction).
///  - **`default` is a target too.** Forgetting it in the scan would leave a reachable pc
///    undecoded and un-labelled; forgetting it in the emitter would fall off the end of the chain.
///
/// Being a pure function of `(code, pc)` is load-bearing: the scan and the emitter each call it
/// and must agree, and this is why they cannot fail to.
fn switch_layout(code: &[u8], pc: usize) -> Result<SwitchLayout, Ineligible> {
    let out_of_range = || Ineligible::OutOfRange { pc };
    // The pad is measured from the byte *after* the opcode, to the next 4-byte boundary.
    let base = pc + 1 + (4 - ((pc + 1) % 4)) % 4;
    let word = |at: usize| -> Result<i32, Ineligible> {
        let b = code.get(at..at + 4).ok_or_else(out_of_range)?;
        Ok(i32::from_be_bytes([b[0], b[1], b[2], b[3]]))
    };
    let target = |offset: i32| -> Result<usize, Ineligible> {
        let t = pc as isize + offset as isize;
        match t >= 0 && (t as usize) < code.len() {
            true => Ok(t as usize),
            false => Err(out_of_range()),
        }
    };

    let default = target(word(base)?)?;
    let mut pairs = Vec::new();
    let end = match code[pc] {
        0xaa => {
            let (low, high) = (word(base + 4)?, word(base + 8)?);
            // JVMS §6.5 requires `low <= high`; a class file that says otherwise is malformed, and
            // the subtraction below would underflow on it.
            if low > high {
                return Err(out_of_range());
            }
            // In `i64`, so `high - low + 1` cannot overflow for `low = i32::MIN, high = i32::MAX`.
            let count = (high as i64 - low as i64 + 1) as usize;
            if count > MAX_SWITCH_CASES {
                return Err(Ineligible::TooBig);
            }
            for k in 0..count {
                // `low + k` stays in range: `k < count` means `low + k <= high <= i32::MAX`.
                pairs.push((low + k as i32, target(word(base + 12 + 4 * k)?)?));
            }
            base + 12 + 4 * count
        }
        _ => {
            let npairs = word(base + 4)?;
            if npairs < 0 || npairs as usize > MAX_SWITCH_CASES {
                return Err(match npairs < 0 {
                    true => out_of_range(),
                    false => Ineligible::TooBig,
                });
            }
            for k in 0..npairs as usize {
                pairs.push((word(base + 8 + 8 * k)?, target(word(base + 12 + 8 * k)?)?));
            }
            base + 8 + 8 * npairs as usize
        }
    };
    // `MAX_CODE_LEN` keeps this inside `u16`, but say so rather than assume it.
    let len = u16::try_from(end - pc).map_err(|_| Ineligible::TooBig)?;
    Ok((len, default, pairs))
}

/// Decodes the instruction at `pc`, rejecting anything outside the whitelist.
///
/// `int_const` resolves a constant-pool index to an `i32`, returning `None` for every other
/// constant kind — that is how a `String` or class-literal `ldc` is rejected without this module
/// knowing what a constant pool is. `static_int` does the same job for `getstatic`: an address for
/// an `int` static of an already-initialised class, `None` for everything else.
fn decode(
    code: &[u8],
    pc: usize,
    max_locals: usize,
    int_const: &dyn Fn(u16) -> Option<i32>,
    static_int: &dyn Fn(u16) -> Option<usize>,
) -> Result<(Insn, Option<i32>), Ineligible> {
    let op = code[pc];
    // Every arm below indexes `code[pc + k]`; check the whole instruction fits first, once.
    let need = |n: usize| -> Result<(), Ineligible> {
        match pc + n <= code.len() {
            true => Ok(()),
            false => Err(Ineligible::OutOfRange { pc }),
        }
    };
    let local = |slot: usize| -> Result<u16, Ineligible> {
        match slot < max_locals && slot <= u16::MAX as usize {
            true => Ok(slot as u16),
            false => Err(Ineligible::LocalOutOfRange { pc, slot }),
        }
    };
    // A branch's 2-byte operand is signed and relative to the *opcode*, not to the next
    // instruction (unlike x86's rel32 — the two conventions differ, which is exactly why the
    // translation happens through named labels rather than arithmetic).
    let target = |code: &[u8]| -> Result<usize, Ineligible> {
        let offset = i16::from_be_bytes([code[pc + 1], code[pc + 2]]) as isize;
        let t = pc as isize + offset;
        match t >= 0 && (t as usize) < code.len() {
            true => Ok(t as usize),
            false => Err(Ineligible::OutOfRange { pc }),
        }
    };
    let simple = |len: u16, pops: u16, pushes: u16| Insn { len, pops, pushes, flow: Flow::Next };
    // A `wide`'s local index is the two bytes after the wrapped opcode.
    let wide_index = |code: &[u8]| u16::from_be_bytes([code[pc + 2], code[pc + 3]]) as usize;

    let insn = match op {
        // --- constants ---------------------------------------------------------------------
        // iconst_m1 (0x02) .. iconst_5 (0x08): the value is the opcode minus iconst_0.
        0x02..=0x08 => return Ok((simple(1, 0, 1), Some(op as i32 - 0x03))),
        0x10 => {
            need(2)?;
            return Ok((simple(2, 0, 1), Some(code[pc + 1] as i8 as i32)));
        }
        0x11 => {
            need(3)?;
            return Ok((simple(3, 0, 1), Some(i16::from_be_bytes([code[pc + 1], code[pc + 2]]) as i32)));
        }
        // ldc / ldc_w restricted to CONSTANT_Integer — resolved *now*, so the generated code
        // carries an immediate and never looks at a constant pool.
        0x12 | 0x13 => {
            let (len, index) = match op {
                0x12 => {
                    need(2)?;
                    (2u16, code[pc + 1] as u16)
                }
                _ => {
                    need(3)?;
                    (3u16, u16::from_be_bytes([code[pc + 1], code[pc + 2]]))
                }
            };
            let value = int_const(index).ok_or(Ineligible::NonIntegerConstant { pc, index })?;
            return Ok((simple(len, 0, 1), Some(value)));
        }

        // --- locals ------------------------------------------------------------------------
        0x1a..=0x1d => {
            local((op - 0x1a) as usize)?;
            simple(1, 0, 1)
        }
        0x15 => {
            need(2)?;
            local(code[pc + 1] as usize)?;
            simple(2, 0, 1)
        }
        0x3b..=0x3e => {
            local((op - 0x3b) as usize)?;
            simple(1, 1, 0)
        }
        0x36 => {
            need(2)?;
            local(code[pc + 1] as usize)?;
            simple(2, 1, 0)
        }
        0x84 => {
            need(3)?;
            local(code[pc + 1] as usize)?;
            simple(3, 0, 0)
        }

        // --- wide: the same three instructions with a 16-bit operand field -------------------
        // Only these three forms. A `wide aload`/`lstore`/… is refused like the narrow opcode it
        // wraps would be — reported as 0xc4 rather than as the inner byte, so the invariant
        // "`Ineligible::Opcode { pc, opcode }` names `code[pc]`" holds for every rejection.
        0xc4 => {
            need(2)?;
            match code[pc + 1] {
                0x15 => {
                    need(4)?;
                    local(wide_index(code))?;
                    simple(4, 0, 1)
                }
                0x36 => {
                    need(4)?;
                    local(wide_index(code))?;
                    simple(4, 1, 0)
                }
                0x84 => {
                    need(6)?;
                    local(wide_index(code))?;
                    simple(6, 0, 0)
                }
                _ => return Err(Ineligible::Opcode { pc, opcode: op }),
            }
        }

        // --- getstatic of an int ------------------------------------------------------------
        // Resolved **now**, to an address baked in as an immediate — so the generated code never
        // touches a constant pool, a metaspace or a class-init state. See the module docs for why
        // that address cannot move and why the class has to be initialised already.
        0xb2 => {
            need(3)?;
            let index = u16::from_be_bytes([code[pc + 1], code[pc + 2]]);
            static_int(index).ok_or(Ineligible::UnresolvedStatic { pc, index })?;
            simple(3, 0, 1)
        }

        // --- arithmetic, bits and shifts ---------------------------------------------------
        // iadd isub imul idiv irem / ishl ishr iushr iand ior ixor: two operands, one result.
        0x60 | 0x64 | 0x68 | 0x6c | 0x70 | 0x78 | 0x7a | 0x7c | 0x7e | 0x80 | 0x82 => simple(1, 2, 1),
        0x74 => simple(1, 1, 1), // ineg

        // --- stack -------------------------------------------------------------------------
        // Every one of these is read as its **category-1** form, which in this subset is not a
        // choice: nothing pushes anything but an `int`. See the module docs — this is the exact
        // assumption the `long` step has to come back and re-examine.
        0x00 => simple(1, 0, 0), // nop
        0x57 => simple(1, 1, 0), // pop
        0x58 => simple(1, 2, 0), // pop2
        0x59 => simple(1, 1, 2), // dup
        0x5a => simple(1, 2, 3), // dup_x1
        0x5b => simple(1, 3, 4), // dup_x2
        0x5c => simple(1, 2, 4), // dup2
        0x5d => simple(1, 3, 5), // dup2_x1
        0x5e => simple(1, 4, 6), // dup2_x2
        0x5f => simple(1, 2, 2), // swap

        // --- control flow ------------------------------------------------------------------
        0x99..=0x9e => {
            need(3)?;
            Insn { len: 3, pops: 1, pushes: 0, flow: Flow::Branch(target(code)?) }
        }
        0x9f..=0xa4 => {
            need(3)?;
            Insn { len: 3, pops: 2, pushes: 0, flow: Flow::Branch(target(code)?) }
        }
        0xa7 => {
            need(3)?;
            Insn { len: 3, pops: 0, pushes: 0, flow: Flow::Goto(target(code)?) }
        }
        // tableswitch / lookupswitch: pop the key, jump to one of many places, never fall through.
        // `switch_layout` does all the decoding *and* all the validation, so a malformed table is
        // an `Ineligible` here rather than a surprise in pass 2.
        0xaa | 0xab => {
            let (len, _, _) = switch_layout(code, pc)?;
            Insn { len, pops: 1, pushes: 0, flow: Flow::Switch }
        }
        0xac => Insn { len: 1, pops: 1, pushes: 0, flow: Flow::Return }, // ireturn

        _ => return Err(Ineligible::Opcode { pc, opcode: op }),
    };
    Ok((insn, None))
}

/// The locals an instruction reads or writes, if any.
///
/// Only ever called on an instruction [`decode`] has already accepted, so the operand bytes it
/// indexes are known to be there — and the `wide` arm is known to wrap one of the three forms
/// with a local index.
fn touched_local(code: &[u8], pc: usize) -> Option<u16> {
    match code[pc] {
        0x1a..=0x1d => Some((code[pc] - 0x1a) as u16),
        0x3b..=0x3e => Some((code[pc] - 0x3b) as u16),
        0x15 | 0x36 | 0x84 => Some(code[pc + 1] as u16),
        0xc4 => Some(u16::from_be_bytes([code[pc + 2], code[pc + 3]])),
        _ => None,
    }
}

/// Walks the control-flow graph from pc 0, decoding as it goes.
///
/// The operand-stack depth is **recomputed**, not read from the `StackMapTable`: the attribute is
/// optional, can be stale, and is exactly the kind of input a JIT must not trust. Re-deriving it
/// costs one pass and turns "the class file said so" into "two paths agree" — a disagreement is an
/// [`Ineligible::StackMismatch`] and the method is simply never compiled.
fn scan(
    code: &[u8],
    max_locals: usize,
    int_const: &dyn Fn(u16) -> Option<i32>,
    static_int: &dyn Fn(u16) -> Option<usize>,
) -> Result<Scan, Ineligible> {
    if code.is_empty() || code.len() > MAX_CODE_LEN {
        return Err(Ineligible::TooBig);
    }
    let mut depth: Vec<Option<u16>> = vec![None; code.len()];
    let mut decoded: Vec<Option<Insn>> = vec![None; code.len()];
    let mut touched = BTreeSet::new();
    let mut max_depth = 0u16;

    let mut work = vec![(0usize, 0u16)];
    while let Some((pc, entry)) = work.pop() {
        // Reached before? Then the depths must agree — that agreement is the whole verification.
        if let Some(seen) = depth[pc] {
            if seen != entry {
                return Err(Ineligible::StackMismatch { pc, seen, found: entry });
            }
            continue;
        }
        depth[pc] = Some(entry);

        let (insn, _) = decode(code, pc, max_locals, int_const, static_int)?;
        decoded[pc] = Some(insn);
        if let Some(slot) = touched_local(code, pc) {
            touched.insert(slot);
        }

        if entry < insn.pops {
            return Err(Ineligible::StackUnderflow { pc });
        }
        let after = entry - insn.pops + insn.pushes;
        max_depth = max_depth.max(entry).max(after);
        if max_depth > MAX_STACK_SLOTS {
            return Err(Ineligible::TooBig);
        }

        let fallthrough = pc + insn.len as usize;
        match insn.flow {
            Flow::Next => work.push((fallthrough, after)),
            Flow::Branch(t) => {
                work.push((t, after));
                work.push((fallthrough, after));
            }
            Flow::Goto(t) => work.push((t, after)),
            // Every arm *and* the default — a `default` that never got walked would be a reachable
            // pc with no depth, no label and no emitted code.
            Flow::Switch => {
                let (_, default, pairs) = switch_layout(code, pc)?;
                work.push((default, after));
                work.extend(pairs.into_iter().map(|(_, t)| (t, after)));
            }
            Flow::Return => {}
        }
        // A fall-through (or the instruction after a branch) that leaves the code array means the
        // method can run off its own end — never true of verified bytecode, always a reject here.
        if falls_through(insn.flow) && fallthrough >= code.len() {
            return Err(Ineligible::OutOfRange { pc });
        }
    }

    // Collect the reachable starts in address order and check they *tile*: no instruction may
    // begin inside another. A branch into the middle of an instruction would otherwise be
    // decoded twice, at two different boundaries, and pass 2 would emit both.
    let order: Vec<usize> = (0..code.len()).filter(|&pc| depth[pc].is_some()).collect();
    let insns: Vec<Insn> = order.iter().map(|&pc| decoded[pc].expect("reachable implies decoded")).collect();
    for (i, &pc) in order.iter().enumerate() {
        let end = pc + insns[i].len as usize;
        if let Some(&next) = order.get(i + 1) {
            if end > next {
                return Err(Ineligible::OverlappingInstructions { pc });
            }
        }
    }

    // The loop headers. A branch is a back-edge when its target precedes it; the target is
    // *eligible* when the operand stack is empty there, because that is the only state the
    // transfer contract can describe with a locals buffer and a pc alone. `depth[t]` is `Some`
    // for every branch target (the walk reached it), and equals the depth the branch leaves
    // behind — the scan's own agreement check is what makes those two the same number.
    let mut osr = BTreeSet::new();
    for (i, &pc) in order.iter().enumerate() {
        // A `switch` back-edge is a loop header like any other — a `continue` inside a `switch`
        // inside a loop is exactly that shape — so its arms are considered too.
        let targets: Vec<usize> = match insns[i].flow {
            Flow::Branch(t) | Flow::Goto(t) => vec![t],
            Flow::Switch => {
                let (_, default, pairs) = switch_layout(code, pc)?;
                std::iter::once(default).chain(pairs.into_iter().map(|(_, t)| t)).collect()
            }
            Flow::Next | Flow::Return => continue,
        };
        for target in targets {
            if target < pc && depth[target] == Some(0) {
                osr.insert(target);
            }
        }
    }

    Ok(Scan { depth, order, insns, max_depth, touched, osr })
}

// ---------------------------------------------------------------------------------------------
// Pass 2: emission.
// ---------------------------------------------------------------------------------------------

/// Compiles `code` — a method body — to native code, or explains why it cannot be.
///
/// `max_locals` bounds the local slots the body may name; `int_const` resolves a constant-pool
/// index to an integer constant (`None` for any other kind — see [`Ineligible::NonIntegerConstant`]).
///
/// `static_int` resolves a `getstatic`'s constant-pool index to the **absolute address** of an
/// `int` static, and answers `None` unless *all* of these hold: the field is static, its descriptor
/// is `I`, and its declaring class is already **initialised**. The address is baked into the
/// instruction stream, so it must not move — see the module docs for the two facts (mirrors are
/// pinned in Old; the heap's byte region never reallocates) that make it stable.
///
/// `poll_word` is the address of the 8-byte safepoint poll word, **baked into the instruction
/// stream as an immediate** — it must stay valid and unmoved for as long as this code can run.
///
/// The result is a function of signature `extern "system" fn(*mut i64, i64) -> i64` following the
/// [`Status`] protocol; see the module docs for the marshalling contract on the pointer and for
/// what the second argument (the entry pc) means.
pub fn compile(
    code: &[u8],
    max_locals: usize,
    int_const: &dyn Fn(u16) -> Option<i32>,
    static_int: &dyn Fn(u16) -> Option<usize>,
    poll_word: usize,
) -> Result<CompiledCode, Ineligible> {
    let scan = scan(code, max_locals, int_const, static_int)?;
    // `POLL` is saved only when something reads it, so a method without loop headers keeps
    // exactly the frame — and the prologue — it had before OSR existed.
    let saved: &[Reg] = match scan.osr.is_empty() {
        true => &[LOCALS],
        false => &[LOCALS, POLL],
    };
    let frame = super::x64::Frame::new(scan.max_depth as u32, saved);
    let mut a = Asm::new();

    // One label per reachable instruction start, so *any* branch target can be named without the
    // emitter reasoning about x86 displacements at all — `Asm::finish` resolves them.
    let mut labels: Vec<Option<Label>> = vec![None; code.len()];
    for &pc in &scan.order {
        labels[pc] = Some(a.new_label());
    }
    // A loop header gets two more names: `osr_labels[pc]` is the instruction itself (where an
    // on-stack entry lands, *past* the poll — so a re-entry always makes at least one iteration
    // of progress even against a poll word that is permanently set), and `exits[pc]` is the stub
    // that returns `Safepoint(pc)`.
    let mut osr_labels: Vec<Option<Label>> = vec![None; code.len()];
    let mut exits: Vec<Option<Label>> = vec![None; code.len()];
    for &pc in &scan.osr {
        osr_labels[pc] = Some(a.new_label());
        exits[pc] = Some(a.new_label());
    }
    let deopt = a.new_label();
    let epilogue = a.new_label();
    let mut deopt_used = false;

    frame.prologue(&mut a);
    // The ABI delivers the locals pointer in RCX; park it in the callee-saved register so RCX is
    // free to be the shift count (`shl` and friends can read the count from nowhere else).
    a.mov_rr(LOCALS, frame.arg(0));
    if !scan.osr.is_empty() {
        // The entry dispatch. `frame.arg(1)` is RDX, which is also `T2` (scratch, and `cqo`'s
        // output) — so it is consumed here, before a single body instruction can clobber it.
        a.mov_ri(POLL, poll_word as i64);
        for &pc in &scan.osr {
            a.cmp_ri(frame.arg(1), pc as i32);
            a.jcc(Cond::E, osr_labels[pc].expect("every loop header has an OSR label"));
        }
        // Anything else — in practice only 0, the ordinary invocation — falls through to pc 0.
    }

    // Operand stack position `k` -> native frame slot `k`. The closure exists so the mapping is
    // written down once; `frame.local` panics on an out-of-range slot, which the scan's
    // `max_depth` has already made impossible.
    let slot = |k: u16| -> Mem { frame.local(k as u32) };
    let lcl = |i: u16| -> Mem { Mem::at(LOCALS, 8 * i as i32) };

    for (i, &pc) in scan.order.iter().enumerate() {
        let insn = scan.insns[i];
        let d = scan.depth[pc].expect("reachable implies a known depth");
        a.bind(labels[pc].expect("every reachable start has a label"));
        // A loop header polls before its own instruction. Every back-edge to this pc branches to
        // the label just bound, so the poll runs exactly once per iteration; an OSR entry lands on
        // `osr_labels[pc]` just below it and therefore skips it.
        if scan.osr.contains(&pc) {
            a.mov_rm(T0, Mem::at(POLL, 0));
            a.cmp_ri(T0, 0);
            a.jcc(Cond::Ne, exits[pc].expect("every loop header has an exit stub"));
            a.bind(osr_labels[pc].expect("every loop header has an OSR label"));
        }
        let op = code[pc];

        match op {
            // --- constants: materialise the immediate, store it at the new top ---------------
            0x02..=0x08 | 0x10 | 0x11 | 0x12 | 0x13 => {
                let (_, value) = decode(code, pc, max_locals, int_const, static_int)?;
                let value = value.expect("the constant opcodes always decode a value");
                // A sign-extended i32 immediate: the normalisation invariant holds on entry to
                // the stack, by construction.
                a.mov_ri(T0, value as i64);
                a.mov_mr(slot(d), T0);
            }

            // --- iload / iload_0..3 ---------------------------------------------------------
            0x15 | 0x1a..=0x1d => {
                let i = if op == 0x15 { code[pc + 1] as u16 } else { (op - 0x1a) as u16 };
                a.mov_rm(T0, lcl(i));
                a.mov_mr(slot(d), T0);
            }

            // --- istore / istore_0..3 -------------------------------------------------------
            0x36 | 0x3b..=0x3e => {
                let i = if op == 0x36 { code[pc + 1] as u16 } else { (op - 0x3b) as u16 };
                a.mov_rm(T0, slot(d - 1));
                a.mov_mr(lcl(i), T0);
            }

            // --- iinc: local += constant, then re-normalise ----------------------------------
            // TRAP 1. `iinc` is the one local write that computes: `Integer.MAX_VALUE` plus one
            // must wrap, and in 64 bits it would not.
            0x84 => {
                let i = code[pc + 1] as u16;
                let delta = code[pc + 2] as i8 as i32;
                a.mov_rm(T0, lcl(i));
                a.add_ri(T0, delta);
                a.movsxd_rr(T0, T0);
                a.mov_mr(lcl(i), T0);
            }

            // --- wide iload / istore / iinc -------------------------------------------------
            // The same three instructions as above with a 16-bit local index (and, for `iinc`, a
            // 16-bit *signed* delta). `decode` has already established that the wrapped opcode is
            // one of these three and that the index is inside `max_locals`.
            0xc4 => {
                let i = u16::from_be_bytes([code[pc + 2], code[pc + 3]]);
                match code[pc + 1] {
                    0x15 => {
                        a.mov_rm(T0, lcl(i));
                        a.mov_mr(slot(d), T0);
                    }
                    0x36 => {
                        a.mov_rm(T0, slot(d - 1));
                        a.mov_mr(lcl(i), T0);
                    }
                    // wide iinc — TRAP 1 exactly as the narrow form: `add` then `movsxd`.
                    _ => {
                        let delta = i16::from_be_bytes([code[pc + 4], code[pc + 5]]) as i32;
                        a.mov_rm(T0, lcl(i));
                        a.add_ri(T0, delta);
                        a.movsxd_rr(T0, T0);
                        a.mov_mr(lcl(i), T0);
                    }
                }
            }

            // --- getstatic of an int ---------------------------------------------------------
            // A 32-bit **sign-extending** load from a baked-in address. Two things are load-bearing
            // and neither is obvious: the static occupies 4 bytes (the interpreter's `putstatic`
            // writes it with `write_u32`), so a 64-bit load would drag in the neighbouring slot;
            // and `movsxd` is what puts the value into the normalised form every other opcode here
            // assumes of its inputs. `T1` holds the address only for the one instruction that uses
            // it — nothing is live across an opcode boundary.
            0xb2 => {
                let index = u16::from_be_bytes([code[pc + 1], code[pc + 2]]);
                let address = static_int(index).ok_or(Ineligible::UnresolvedStatic { pc, index })?;
                a.mov_ri(T1, address as i64);
                a.movsxd_rm(T0, Mem::at(T1, 0));
                a.mov_mr(slot(d), T0);
            }

            // --- binary arithmetic: lhs at d-2, rhs at d-1, result overwrites lhs -------------
            // TRAP 1 again: add/sub/mul are exactly the operations that can leave the 32-bit
            // range, so each is followed by `movsxd`.
            0x60 | 0x64 | 0x68 => {
                a.mov_rm(T0, slot(d - 2));
                match op {
                    0x60 => a.add_rm(T0, slot(d - 1)),
                    0x64 => a.sub_rm(T0, slot(d - 1)),
                    _ => a.imul_rm(T0, slot(d - 1)),
                }
                a.movsxd_rr(T0, T0);
                a.mov_mr(slot(d - 2), T0);
            }

            // --- bitwise: no normalisation needed (see the module docs for the proof) --------
            0x7e | 0x80 | 0x82 => {
                a.mov_rm(T0, slot(d - 2));
                match op {
                    0x7e => a.and_rm(T0, slot(d - 1)),
                    0x80 => a.or_rm(T0, slot(d - 1)),
                    _ => a.xor_rm(T0, slot(d - 1)),
                }
                a.mov_mr(slot(d - 2), T0);
            }

            // --- ineg -----------------------------------------------------------------------
            // `-Integer.MIN_VALUE` is `Integer.MIN_VALUE`; in 64 bits `neg` would answer 2^31.
            0x74 => {
                a.mov_rm(T0, slot(d - 1));
                a.neg(T0);
                a.movsxd_rr(T0, T0);
                a.mov_mr(slot(d - 1), T0);
            }

            // --- shifts ---------------------------------------------------------------------
            // TRAP 2. The count is masked to 5 bits *explicitly* — a 64-bit shift would mask to
            // 6 and `x << 33` would not equal `x << 1`. `iushr` additionally zero-extends the
            // low 32 bits first, or `-1 >>> 1` answers -1 instead of Integer.MAX_VALUE.
            0x78 | 0x7a | 0x7c => {
                a.mov_rm(T1, slot(d - 1));
                a.and_ri(T1, 31);
                match op {
                    0x7c => a.mov_rm32(T0, slot(d - 2)), // iushr: zero-extend, then logical shift
                    _ => a.mov_rm(T0, slot(d - 2)),
                }
                match op {
                    0x78 => a.shl_cl(T0),
                    0x7a => a.sar_cl(T0),
                    _ => a.shr_cl(T0),
                }
                a.movsxd_rr(T0, T0);
                a.mov_mr(slot(d - 2), T0);
            }

            // --- idiv / irem ----------------------------------------------------------------
            // TRAP 3, both halves. A zero divisor would raise #DE (a Windows structured
            // exception, not an ArithmeticException), so it is checked and **deopted** — the
            // interpreter then runs the method from the start and throws properly. The
            // `INT_MIN / -1` overflow needs no check at all: in 64 bits the quotient 2^31 is
            // representable, and the `movsxd` truncates it to INT_MIN as JLS 15.17.2 requires.
            0x6c | 0x70 => {
                a.mov_rm(T1, slot(d - 1));
                a.cmp_ri(T1, 0);
                a.jcc(Cond::E, deopt);
                deopt_used = true;
                a.mov_rm(T0, slot(d - 2));
                a.cqo(); // sign-extend RAX into RDX:RAX -- idiv's dividend is the pair
                a.idiv(T1);
                let result = if op == 0x6c { T0 } else { T2 }; // quotient in RAX, remainder in RDX
                a.movsxd_rr(result, result);
                a.mov_mr(slot(d - 2), result);
            }

            // --- stack shuffles -------------------------------------------------------------
            // `nop`, `pop` and `pop2` emit nothing at all: the operand stack is a compile-time
            // notion here, so dropping a value is just a smaller depth for the next instruction.
            //
            // The rest are permutations of frame slots. Writing them out one by one rather than
            // through a generic permutation engine is deliberate: the operand slots are both the
            // sources *and* the destinations, so the order of the `mov`s is the correctness
            // argument, and it is different for each. Below, `b` is the bottom of the affected
            // region and `s0..sn` are the values found there on entry, bottom-first.
            //
            // (All six are the **category-1** readings. In this subset that is forced, not chosen
            // — nothing can push a category-2 value. See the module docs.)
            0x00 | 0x57 | 0x58 => {}
            // dup: [s0] -> [s0, s0]
            0x59 => {
                a.mov_rm(T0, slot(d - 1));
                a.mov_mr(slot(d), T0);
            }
            // dup_x1: [s0, s1] -> [s1, s0, s1]. Both sources must be in registers before the
            // first store, since every slot in the region is overwritten.
            0x5a => {
                let b = d - 2;
                a.mov_rm(T0, slot(b));
                a.mov_rm(T1, slot(b + 1));
                a.mov_mr(slot(b), T1);
                a.mov_mr(slot(b + 1), T0);
                a.mov_mr(slot(b + 2), T1);
            }
            // dup_x2: [s0, s1, s2] -> [s2, s0, s1, s2]
            0x5b => {
                let b = d - 3;
                a.mov_rm(T0, slot(b));
                a.mov_rm(T1, slot(b + 1));
                a.mov_rm(T2, slot(b + 2));
                a.mov_mr(slot(b), T2);
                a.mov_mr(slot(b + 1), T0);
                a.mov_mr(slot(b + 2), T1);
                a.mov_mr(slot(b + 3), T2);
            }
            // dup2: [s0, s1] -> [s0, s1, s0, s1]. The bottom two are already right where they
            // belong, so this is a pure copy upwards and clobbers no source.
            0x5c => {
                let b = d - 2;
                a.mov_rm(T0, slot(b));
                a.mov_rm(T1, slot(b + 1));
                a.mov_mr(slot(b + 2), T0);
                a.mov_mr(slot(b + 3), T1);
            }
            // dup2_x1: [s0, s1, s2] -> [s1, s2, s0, s1, s2]
            0x5d => {
                let b = d - 3;
                a.mov_rm(T0, slot(b));
                a.mov_rm(T1, slot(b + 1));
                a.mov_rm(T2, slot(b + 2));
                a.mov_mr(slot(b), T1);
                a.mov_mr(slot(b + 1), T2);
                a.mov_mr(slot(b + 2), T0);
                a.mov_mr(slot(b + 3), T1);
                a.mov_mr(slot(b + 4), T2);
            }
            // dup2_x2: [s0, s1, s2, s3] -> [s2, s3, s0, s1, s2, s3].
            //
            // Four live sources and three scratch registers, so this one spills — but it spills
            // *into its own result*: slots b+4 and b+5 are above the source region, they are
            // untouched on entry, and their final contents are s2 and s3. So writing them first
            // is simultaneously the backup and the answer, and nothing extra is needed.
            0x5e => {
                let b = d - 4;
                a.mov_rm(T0, slot(b + 2)); // s2
                a.mov_rm(T1, slot(b + 3)); // s3
                a.mov_mr(slot(b + 4), T0); // final top pair, and the only copy of s2/s3 from here
                a.mov_mr(slot(b + 5), T1);
                a.mov_rm(T0, slot(b)); // s0
                a.mov_rm(T1, slot(b + 1)); // s1
                a.mov_mr(slot(b + 2), T0);
                a.mov_mr(slot(b + 3), T1);
                a.mov_rm(T0, slot(b + 4)); // s2 back out of its own result slot
                a.mov_rm(T1, slot(b + 5)); // s3
                a.mov_mr(slot(b), T0);
                a.mov_mr(slot(b + 1), T1);
            }
            // swap: [s0, s1] -> [s1, s0]
            0x5f => {
                a.mov_rm(T0, slot(d - 2));
                a.mov_rm(T2, slot(d - 1));
                a.mov_mr(slot(d - 2), T2);
                a.mov_mr(slot(d - 1), T0);
            }

            // --- branches -------------------------------------------------------------------
            // Java's `if_icmp*` are *signed* comparisons, so the signed condition codes are the
            // only correct ones: `Cond::B` would rank -1 above 1.
            0x99..=0xa4 => {
                let cond = match op {
                    0x9f | 0x99 => Cond::E,
                    0xa0 | 0x9a => Cond::Ne,
                    0xa1 | 0x9b => Cond::L,
                    0xa2 | 0x9c => Cond::Ge,
                    0xa3 | 0x9d => Cond::G,
                    _ => Cond::Le,
                };
                match op {
                    0x9f..=0xa4 => {
                        a.mov_rm(T0, slot(d - 2));
                        a.cmp_rm(T0, slot(d - 1));
                    }
                    _ => {
                        a.mov_rm(T0, slot(d - 1));
                        a.cmp_ri(T0, 0);
                    }
                }
                let Flow::Branch(t) = insn.flow else { unreachable!("a conditional branch has a target") };
                a.jcc(cond, labels[t].expect("a reachable branch target has a label"));
            }
            0xa7 => {
                let Flow::Goto(t) = insn.flow else { unreachable!("goto has a target") };
                a.jmp(labels[t].expect("a reachable branch target has a label"));
            }

            // --- tableswitch / lookupswitch -------------------------------------------------
            // A compare chain: one `cmp`/`je` per case in table order, then an unconditional jump
            // to `default`. The key is loaded **once**, into T0, and `cmp reg, imm` leaves it
            // untouched, so the chain costs one load however long it is.
            //
            // `default` is emitted as a real `jmp` rather than left to fall through: a switch does
            // not fall through, and the next instruction in address order is usually the first arm
            // of the switch itself.
            //
            // A `tableswitch` whose arms are contiguous still gets the chain — see the module docs
            // for why there is no jump table yet. Correct and O(cases), rather than clever and
            // needing an assembler feature that does not exist.
            0xaa | 0xab => {
                let (_, default, pairs) = switch_layout(code, pc)?;
                a.mov_rm(T0, slot(d - 1));
                for (value, t) in pairs {
                    a.cmp_ri(T0, value);
                    a.jcc(Cond::E, labels[t].expect("a reachable switch arm has a label"));
                }
                a.jmp(labels[default].expect("the switch default is reachable and has a label"));
            }

            // --- ireturn --------------------------------------------------------------------
            // The whole OK path in one instruction: a 32-bit load zero-extends, which puts the
            // value in the low half and status 0 (`Status::OK`) in the high half at once.
            0xac => {
                a.mov_rm32(T0, slot(d - 1));
                a.jmp(epilogue);
            }

            _ => return Err(Ineligible::Opcode { pc, opcode: op }),
        }

        // Fall-through must land on the instruction emitted next. It always does for `javac`
        // output (reachable code is contiguous), but a gap — created by unreachable bytes between
        // two reachable instructions — would silently run the wrong code, so bridge it explicitly.
        let next_emitted = scan.order.get(i + 1).copied();
        if falls_through(insn.flow) && next_emitted != Some(pc + insn.len as usize) {
            let t = pc + insn.len as usize;
            a.jmp(labels[t].ok_or(Ineligible::OutOfRange { pc })?);
        }
    }

    // The safepoint exit stubs, one per loop header, parked here at the end of the function so a
    // taken poll costs the loop body nothing but the `jcc` — the stub itself never shares a cache
    // line with the loop. Each is two instructions: pack this pc with `Status::SAFEPOINT` and
    // leave through the shared epilogue. Nothing is marshalled out: `istore`/`iinc` wrote straight
    // through to the caller's buffer, so it is already current.
    for &pc in &scan.osr {
        a.bind(exits[pc].expect("every loop header has an exit stub"));
        a.mov_ri(T0, Status::safepoint_value(pc as u32));
        a.jmp(epilogue);
    }

    // The deopt block: hand back `status = DEOPT` and leave through the shared epilogue, so the
    // frame is torn down by exactly the same instructions on both exits.
    a.bind(deopt);
    if deopt_used {
        a.mov_ri(T0, Status::DEOPT_VALUE);
    }
    a.bind(epilogue);
    frame.epilogue(&mut a);

    let code = a.finish().map_err(Ineligible::Assembler)?;
    Ok(CompiledCode {
        code,
        touched_locals: scan.touched.into_iter().collect(),
        stack_slots: scan.max_depth as u32,
        osr_entries: scan.osr.iter().map(|&pc| pc as u32).collect(),
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A constant pool with no integers in it — the default for tests that use no `ldc`.
    fn no_constants(_: u16) -> Option<i32> {
        None
    }

    /// A poll word for the tests below, which read the emitted bytes but never run them. A real
    /// `static` rather than a fabricated integer, so the immediate `compile` bakes in is a genuine
    /// address — and one whose lifetime is the process, which is the property the emitted code
    /// depends on.
    static TEST_POLL: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(0);

    /// [`super::compile`] with the poll word filled in. Shadows the glob-imported name so the
    /// tests read exactly as they did before step 3 added the argument.
    fn compile(
        code: &[u8],
        max_locals: usize,
        int_const: &dyn Fn(u16) -> Option<i32>,
    ) -> Result<CompiledCode, Ineligible> {
        super::compile(code, max_locals, int_const, &|_| None, &TEST_POLL as *const _ as usize)
    }

    #[test]
    fn a_trivial_method_compiles() {
        // iconst_1; ireturn
        let c = compile(&[0x04, 0xac], 0, &no_constants).unwrap();
        assert!(!c.code.is_empty());
        assert_eq!(c.stack_slots, 1);
        assert!(c.touched_locals.is_empty());
    }

    #[test]
    fn one_foreign_opcode_disqualifies_the_whole_method() {
        // iload_0; putstatic #1; ireturn -- `putstatic` (0xb3) is deliberately outside the subset
        // (it is the one *observable* side effect, and deopt-by-restart could not survive it), so
        // the method is rejected outright rather than compiled up to that point.
        let err = compile(&[0x1a, 0xb3, 0x00, 0x01, 0xac], 1, &no_constants).unwrap_err();
        assert_eq!(err, Ineligible::Opcode { pc: 1, opcode: 0xb3 });
        // The same for a call, an instance-field access, an array access, a reference return, a
        // void return, an allocation, a monitor and a `wide` wrapping something unsupported.
        for op in [0xb8u8, 0xb5, 0xb4, 0x2e, 0xb0, 0xb1, 0xbb, 0xbf, 0xc2] {
            let err = compile(&[0x03, op, 0x00, 0x00, 0xac], 1, &no_constants).unwrap_err();
            assert_eq!(err, Ineligible::Opcode { pc: 1, opcode: op }, "0x{op:02x} must be rejected");
        }
        // `getstatic` (0xb2) is now *conditionally* in the subset, so its rejection has its own
        // shape: the resolver refused this index, and the whole method goes with it.
        let err = compile(&[0x1a, 0xb2, 0x00, 0x01, 0xac], 1, &no_constants).unwrap_err();
        assert_eq!(err, Ineligible::UnresolvedStatic { pc: 1, index: 1 });
    }

    #[test]
    fn ldc_is_accepted_only_for_integer_constants() {
        // ldc #1; ireturn, with #1 an Integer -> compiles.
        assert!(compile(&[0x12, 0x01, 0xac], 0, &|i| (i == 1).then_some(90_000)).is_ok());
        // The same bytes with #1 a String (the resolver answers None) -> rejected.
        let err = compile(&[0x12, 0x01, 0xac], 0, &no_constants).unwrap_err();
        assert_eq!(err, Ineligible::NonIntegerConstant { pc: 0, index: 1 });
    }

    #[test]
    fn only_the_locals_actually_used_are_in_the_marshalling_contract() {
        // iload_2; iload 5; iadd; istore_2; iinc 7, 1; iload_2; ireturn
        // Locals 0, 1, 3, 4, 6 are never named, so the caller need not (and must not have to)
        // marshal them -- that is what lets an unused `this` in slot 0 stay unmarshalled.
        let code = [0x1c, 0x15, 0x05, 0x60, 0x3d, 0x84, 0x07, 0x01, 0x1c, 0xac];
        let c = compile(&code, 8, &no_constants).unwrap();
        assert_eq!(c.touched_locals, vec![2, 5, 7]);
    }

    #[test]
    fn a_local_past_max_locals_is_refused() {
        let err = compile(&[0x15, 0x09, 0xac], 4, &no_constants).unwrap_err();
        assert_eq!(err, Ineligible::LocalOutOfRange { pc: 0, slot: 9 });
    }

    #[test]
    fn the_operand_stack_depth_is_recomputed_from_the_control_flow_graph() {
        // iconst_0; ifeq +5 (to the iconst_3); iconst_1; ireturn; iconst_3; ireturn
        //  0        1                          4          5        6         7
        // Both arms of the branch leave the stack at 0 before pushing their result, so the
        // deepest the stack ever gets is 1.
        let code = [0x03, 0x99, 0x00, 0x05, 0x04, 0xac, 0x06, 0xac];
        let c = compile(&code, 0, &no_constants).unwrap();
        assert_eq!(c.stack_slots, 1);
    }

    #[test]
    fn paths_that_disagree_about_the_stack_depth_are_refused() {
        // iconst_0; iconst_0; ifeq +4 (to pc 6); pop; iconst_1; ireturn
        //  0         1         2                  5    6         7
        // pc 6 is reached with depth 1 by the branch (one value left) and depth 0 by the
        // fall-through through `pop`. Verified bytecode never does this.
        let code = [0x03, 0x03, 0x99, 0x00, 0x04, 0x57, 0x04, 0xac];
        let err = compile(&code, 0, &no_constants).unwrap_err();
        assert!(matches!(err, Ineligible::StackMismatch { pc: 6, .. }), "{err:?}");
    }

    #[test]
    fn underflow_and_out_of_range_branches_are_refused() {
        // iadd with an empty stack.
        assert_eq!(compile(&[0x60, 0xac], 0, &no_constants).unwrap_err(), Ineligible::StackUnderflow { pc: 0 });
        // goto -100, off the front of the code array.
        let err = compile(&[0xa7, 0xff, 0x9c], 0, &no_constants).unwrap_err();
        assert_eq!(err, Ineligible::OutOfRange { pc: 0 });
        // A method whose last instruction falls through past the end.
        assert_eq!(compile(&[0x03], 0, &no_constants).unwrap_err(), Ineligible::OutOfRange { pc: 0 });
    }

    #[test]
    fn a_branch_into_the_middle_of_an_instruction_is_refused() {
        // A branch target that is not an instruction boundary of the linear decode. Both readings
        // happen to agree about the stack depth, so only the tiling check catches it — which is
        // exactly why that check exists. `javac` cannot produce this; a hand-written or hostile
        // class file can.
        //
        //  0: iconst_0            pushes 1
        //  1: ifeq  -> 6          pops 1; falls through to 4 and branches to 6
        //  4: sipush 0x0004       occupies 4, 5 and 6 -- so the branch lands *inside* it
        //  7: ireturn
        //
        // Decoded from 6, the byte 0x04 is `iconst_1`, which pushes 1 and reaches pc 7 at the
        // same depth the sipush leaves it. Everything is consistent; the instructions simply
        // overlap.
        let code = [0x03, 0x99, 0x00, 0x05, 0x11, 0x00, 0x04, 0xac];
        assert_eq!(
            compile(&code, 0, &no_constants).unwrap_err(),
            Ineligible::OverlappingInstructions { pc: 4 }
        );
    }

    #[test]
    fn an_oversized_method_is_refused() {
        let mut code = vec![0x00u8; MAX_CODE_LEN + 1];
        code[0] = 0x04;
        assert_eq!(compile(&code, 0, &no_constants).unwrap_err(), Ineligible::TooBig);
        assert_eq!(compile(&[], 0, &no_constants).unwrap_err(), Ineligible::TooBig);
    }

    #[test]
    fn the_status_packing_round_trips() {
        assert_eq!(Status::unpack(0), Outcome::Returned(0));
        assert_eq!(Status::unpack(42), Outcome::Returned(42));
        // A negative int is carried as its *zero-extended* 32-bit pattern, so the high half
        // stays free for the status -- that is why `ireturn` is a 32-bit load.
        assert_eq!(Status::unpack(0xFFFF_FFFF), Outcome::Returned(-1));
        assert_eq!(Status::unpack(0x8000_0000), Outcome::Returned(i32::MIN));
        assert_eq!(Status::unpack(Status::DEOPT_VALUE), Outcome::Deopt);
        assert_eq!(Status::unpack(Status::safepoint_value(0)), Outcome::Safepoint(0));
        assert_eq!(Status::unpack(Status::safepoint_value(4095)), Outcome::Safepoint(4095));
        // An unknown status is read as a deopt: no emitted code produces one, and "interpret it"
        // is the answer that is safe whatever the state.
        assert_eq!(Status::unpack(99 << Status::SHIFT), Outcome::Deopt);
    }

    #[test]
    fn a_loop_header_with_an_empty_stack_becomes_an_entry_point() {
        //  0: iconst_0        push 0
        //  1: istore_0        i = 0
        //  2: iload_0         <- the loop header, stack empty
        //  3: sipush 100
        //  6: if_icmpge +9    -> 15 (exit)
        //  9: iinc 0, 1
        // 12: goto -10        -> 2, the back-edge
        // 15: iload_0
        // 16: ireturn
        let code = [
            0x03, 0x3b, 0x1a, 0x11, 0x00, 0x64, 0xa2, 0x00, 0x09, 0x84, 0x00, 0x01, 0xa7, 0xff,
            0xf6, 0x1a, 0xac,
        ];
        let c = compile(&code, 1, &no_constants).unwrap();
        assert_eq!(c.osr_entries, vec![2], "the `goto`'s target is the one loop header");
    }

    #[test]
    fn a_back_edge_that_carries_operands_is_not_an_entry_point() {
        // The same loop, but with a value live on the operand stack across the back-edge:
        //  0: iconst_5        push 5          (the carried operand)
        //  1: iconst_0
        //  2: istore_0        i = 0
        //  3: iload_0         <- header, but the stack holds 1 value here
        //  4: sipush 100
        //  7: if_icmpge +9    -> 16
        // 10: iinc 0, 1
        // 13: goto -10        -> 3
        // 16: ireturn         returns the carried 5
        let code = [
            0x08, 0x03, 0x3b, 0x1a, 0x11, 0x00, 0x64, 0xa2, 0x00, 0x09, 0x84, 0x00, 0x01, 0xa7,
            0xff, 0xf6, 0xac,
        ];
        let c = compile(&code, 1, &no_constants).unwrap();
        assert!(c.osr_entries.is_empty(), "depth 1 at the header, so it is not a transfer point");
        // And it still compiles and runs as it always did — ineligibility for OSR is not
        // ineligibility for compilation.
        assert_eq!(c.stack_slots, 3);
    }

    #[test]
    fn nested_loops_give_one_entry_point_each() {
        //  0: iconst_0; istore_0                      i = 0
        //  2: iload_0; bipush 10; if_icmpge -> 28     <- outer header at 2
        //  8: iconst_0; istore_1                      j = 0
        // 10: iload_1; bipush 10; if_icmpge -> 22     <- inner header at 10
        // 16: iinc 1,1; goto -> 10                    the inner back-edge
        // 22: iinc 0,1; goto -> 2                     the outer back-edge
        // 28: iload_0; ireturn
        let code = [
            0x03, 0x3b, // 0: iconst_0; istore_0
            0x1a, 0x10, 0x0a, 0xa2, 0x00, 0x17, // 2: iload_0; bipush 10; if_icmpge +23 -> 28
            0x03, 0x3c, // 8: iconst_0; istore_1
            0x1b, 0x10, 0x0a, 0xa2, 0x00, 0x09, // 10: iload_1; bipush 10; if_icmpge +9 -> 22
            0x84, 0x01, 0x01, 0xa7, 0xff, 0xf7, // 16: iinc 1,1; goto -9 -> 10
            0x84, 0x00, 0x01, 0xa7, 0xff, 0xe9, // 22: iinc 0,1; goto -23 -> 2
            0x1a, 0xac, // 28: iload_0; ireturn
        ];
        let c = compile(&code, 2, &no_constants).unwrap();
        assert_eq!(c.osr_entries, vec![2, 10], "both headers, in pc order");
    }

    #[test]
    fn a_method_without_a_loop_pays_nothing_for_osr() {
        // No back-edge -> no entry points, no poll, and RSI is never saved: the emitted code is
        // byte-for-byte what step 2 produced.
        let c = compile(&[0x1a, 0x1b, 0x60, 0xac], 2, &no_constants).unwrap();
        assert!(c.osr_entries.is_empty());
        // `push rsi` is `56`; the prologue of a poll-free method must not contain it.
        assert!(!c.code.starts_with(&[0x55, 0x48, 0x89, 0xE5, 0x53, 0x56]), "RSI must not be saved");
    }

    #[test]
    fn the_deopt_block_is_only_emitted_when_something_can_deopt() {
        // No division -> no deopt site -> `mov rax, DEOPT_VALUE` (a 10-byte movabs) is absent.
        let plain = compile(&[0x1a, 0xac], 1, &no_constants).unwrap();
        let movabs = [0x48u8, 0xB8, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00];
        assert!(!plain.code.windows(movabs.len()).any(|w| w == movabs));
        // iload_0; iload_1; idiv; ireturn -> the guard and the deopt block are both there.
        let divides = compile(&[0x1a, 0x1b, 0x6c, 0xac], 2, &no_constants).unwrap();
        assert!(divides.code.windows(movabs.len()).any(|w| w == movabs));
    }
}
