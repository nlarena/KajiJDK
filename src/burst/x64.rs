//! A minimal — but *correct* — x86-64 assembler, emitting into a `Vec<u8>`.
//!
//! Only the frame-local integer subset a first JIT tier needs: moves, the ALU group, `imul`/`idiv`,
//! compares, `setcc`, branches with forward labels, `push`/`pop`, and a prologue/epilogue that
//! follows the Microsoft x64 ABI (see [`Frame`] and the [module docs][crate::burst]).
//!
//! # Encoding, in the order the bytes come out
//!
//! ```text
//! [REX] opcode [ModRM] [SIB] [disp] [imm]
//! ```
//!
//! - **REX** = `0100 W R X B`, i.e. `0x40 | W<<3 | R<<2 | X<<1 | B`.
//!   `W` = 64-bit operand size; `R` extends `ModRM.reg`; `X` extends `SIB.index`; `B` extends
//!   `ModRM.rm` (or `SIB.base`, or the register baked into the opcode, as in `push r15`).
//!   Only *one* REX byte may appear, and it must sit immediately before the opcode.
//! - **ModRM** = `mod<<6 | reg<<3 | rm`. `mod == 0b11` means "`rm` is a register"; otherwise `rm`
//!   names a memory operand. `reg` is either the second register operand or an opcode extension
//!   (the `/0`…`/7` digit in Intel's tables).
//!
//! Three encoding traps are handled centrally in `Asm::modrm_mem`, because getting any of them
//! wrong produces bytes that decode to a *different, plausible* instruction:
//!
//! 1. `rm == 0b100` in a memory form does **not** mean RSP — it means "a SIB byte follows". So
//!    `[rsp+d]` and `[r12+d]` (both low-3 = 4) always need SIB `0x24` = scale 1, index = none,
//!    base = the low 3 bits.
//! 2. `rm == 0b101` with `mod == 0b00` does **not** mean `[rbp]` — it means RIP-relative `disp32`.
//!    So `[rbp+0]` and `[r13+0]` must be encoded as `mod == 0b01` with an explicit zero `disp8`.
//! 3. Registers R8–R15 need `REX.B`/`REX.R`. This is the single most common source of silent
//!    corruption: dropping REX turns `mov r8, rcx` into `mov rax, rcx`, which will "work" until it
//!    doesn't. Every emitter here goes through one `Asm::rex` helper, and the tests exercise
//!    R8–R15 explicitly.
//!
//! # Operand size: everything is 64-bit
//!
//! This step emits `REX.W` (64-bit) forms only, with two escape hatches ([`Asm::movsxd_rr`] /
//! [`Asm::movsxd_rm`] and [`Asm::mov_mr32`]) so a future bytecode compiler can keep a Java `int`
//! **sign-extended in a 64-bit register** and truncate on store. That is a deliberate choice, not
//! laziness: 64-bit `idiv` of `INT_MIN / -1` computes `2^31` and truncates to `0x8000_0000` —
//! exactly the wrap-around JLS §15.17.2 requires — whereas 32-bit `idiv` *faults* (`#DE`) on that
//! input and would need a branch around it.

/// A 64-bit general-purpose register. The discriminant is the architectural register number: the
/// low 3 bits go into ModRM/SIB/opcode, and bit 3 becomes the matching REX bit.
#[derive(Clone, Copy, PartialEq, Eq, Debug, Hash)]
#[repr(u8)]
pub enum Reg {
    Rax = 0,
    Rcx = 1,
    Rdx = 2,
    Rbx = 3,
    Rsp = 4,
    Rbp = 5,
    Rsi = 6,
    Rdi = 7,
    R8 = 8,
    R9 = 9,
    R10 = 10,
    R11 = 11,
    R12 = 12,
    R13 = 13,
    R14 = 14,
    R15 = 15,
}

impl Reg {
    /// The architectural register number, 0–15.
    pub fn code(self) -> u8 {
        self as u8
    }

    /// The low 3 bits — what actually fits in a ModRM/SIB field or an opcode's `+rd`.
    pub fn low3(self) -> u8 {
        (self as u8) & 7
    }

    /// R8–R15: the high bit that must be carried by a REX prefix.
    pub fn is_extended(self) -> bool {
        (self as u8) >= 8
    }

    /// Volatile (caller-saved) under the Microsoft x64 ABI — free for generated code to clobber.
    /// Everything else (`RBX`, `RBP`, `RDI`, `RSI`, `RSP`, `R12`–`R15`) must be restored on exit.
    pub fn is_volatile(self) -> bool {
        matches!(self, Reg::Rax | Reg::Rcx | Reg::Rdx | Reg::R8 | Reg::R9 | Reg::R10 | Reg::R11)
    }
}

/// Integer/pointer argument registers, in order, under the **Microsoft x64** ABI.
///
/// System V uses `RDI, RSI, RDX, RCX, R8, R9` instead; mixing the two is the classic
/// "works on Linux, garbage on Windows" bug, so the order lives here once.
pub const ARG_REGS: [Reg; 4] = [Reg::Rcx, Reg::Rdx, Reg::R8, Reg::R9];

/// Registers the callee must preserve under the Microsoft x64 ABI. `RSP` is preserved too, but by
/// construction (balanced prologue/epilogue) rather than by save/restore.
pub const NON_VOLATILE: [Reg; 8] =
    [Reg::Rbx, Reg::Rbp, Reg::Rdi, Reg::Rsi, Reg::R12, Reg::R13, Reg::R14, Reg::R15];

/// A memory operand: `[base + disp]`. No index/scale — a frame-local tier only ever addresses the
/// stack frame at a constant offset, and leaving indexing out keeps `REX.X` provably zero.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub struct Mem {
    pub base: Reg,
    pub disp: i32,
}

impl Mem {
    /// `[base + disp]`.
    pub fn at(base: Reg, disp: i32) -> Mem {
        Mem { base, disp }
    }
}

/// A condition code — the `tttn` nibble shared by `Jcc` (`0F 80+cc`) and `SETcc` (`0F 90+cc`).
///
/// The signed set (`L`/`Ge`/`Le`/`G`) reads SF/OF; the unsigned set (`B`/`Ae`/`Be`/`A`) reads CF.
/// Java's `if_icmplt` and friends are *signed*, so `L`/`Ge`/`Le`/`G` are the ones a bytecode
/// compiler wants — picking `B`/`A` there would compare `-1` as greater than `1`.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
#[repr(u8)]
pub enum Cond {
    /// overflow
    O = 0,
    /// no overflow
    No = 1,
    /// unsigned `<`
    B = 2,
    /// unsigned `>=`
    Ae = 3,
    /// equal
    E = 4,
    /// not equal
    Ne = 5,
    /// unsigned `<=`
    Be = 6,
    /// unsigned `>`
    A = 7,
    /// sign set
    S = 8,
    /// sign clear
    Ns = 9,
    /// signed `<`
    L = 12,
    /// signed `>=`
    Ge = 13,
    /// signed `<=`
    Le = 14,
    /// signed `>`
    G = 15,
}

impl Cond {
    /// The `tttn` nibble added to the `Jcc`/`SETcc` base opcode.
    pub fn tttn(self) -> u8 {
        self as u8
    }

    /// The condition that is true exactly when `self` is false — `jcc L; ...` becomes
    /// `j<inverse> skip` when a bytecode compiler needs to fall through instead of branch.
    pub fn inverse(self) -> Cond {
        match self {
            Cond::O => Cond::No,
            Cond::No => Cond::O,
            Cond::B => Cond::Ae,
            Cond::Ae => Cond::B,
            Cond::E => Cond::Ne,
            Cond::Ne => Cond::E,
            Cond::Be => Cond::A,
            Cond::A => Cond::Be,
            Cond::S => Cond::Ns,
            Cond::Ns => Cond::S,
            Cond::L => Cond::Ge,
            Cond::Ge => Cond::L,
            Cond::Le => Cond::G,
            Cond::G => Cond::Le,
        }
    }
}

/// A branch target. Created by [`Asm::new_label`], referenced by any number of branches (before or
/// after its position is known), and pinned to the current offset by [`Asm::bind`].
#[derive(Clone, Copy, PartialEq, Eq, Debug, Hash)]
pub struct Label(usize);

/// Something the assembler refuses to encode. All of these are compiler bugs rather than runtime
/// conditions, but they are returned rather than panicked so a JIT can bail out to the interpreter
/// instead of taking the process down.
#[derive(Clone, PartialEq, Eq, Debug)]
pub enum AsmError {
    /// A branch referenced a label that was never [`bound`][Asm::bind].
    UnboundLabel(Label),
    /// The displacement did not fit in the `rel32` field (>= 2 GiB of code).
    BranchOutOfRange { from: usize, to: usize },
}

impl std::fmt::Display for AsmError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            AsmError::UnboundLabel(l) => write!(f, "label {} was never bound", l.0),
            AsmError::BranchOutOfRange { from, to } => {
                write!(f, "branch at {from} to {to} does not fit in rel32")
            }
        }
    }
}

impl std::error::Error for AsmError {}

/// A pending `rel32` patch: `site` is the offset of the 4 displacement bytes, `label` the target.
struct Fixup {
    site: usize,
    label: Label,
}

/// The assembler. Emits into an internal `Vec<u8>`; [`finish`][Asm::finish] resolves the labels and
/// hands back the finished machine code.
pub struct Asm {
    code: Vec<u8>,
    /// `labels[i]` is the bound offset of `Label(i)`, or `None` while still forward-referenced.
    labels: Vec<Option<usize>>,
    fixups: Vec<Fixup>,
}

impl Default for Asm {
    fn default() -> Self {
        Asm::new()
    }
}

// ---------------------------------------------------------------------------------------------
// Raw emission: bytes, REX, ModRM, SIB, displacements.
// ---------------------------------------------------------------------------------------------

impl Asm {
    /// An empty assembler.
    pub fn new() -> Asm {
        Asm { code: Vec::new(), labels: Vec::new(), fixups: Vec::new() }
    }

    /// The bytes emitted so far — branch displacements are still unpatched, so this is for
    /// inspection and encoding tests, not for execution. Use [`finish`][Asm::finish] for that.
    pub fn code(&self) -> &[u8] {
        &self.code
    }

    /// The current offset, i.e. the address the next instruction will start at.
    pub fn offset(&self) -> usize {
        self.code.len()
    }

    fn byte(&mut self, b: u8) {
        self.code.push(b);
    }

    fn imm32(&mut self, v: i32) {
        self.code.extend_from_slice(&v.to_le_bytes());
    }

    fn imm64(&mut self, v: i64) {
        self.code.extend_from_slice(&v.to_le_bytes());
    }

    /// Emits a REX prefix if one is needed.
    ///
    /// `w` selects 64-bit operands, `r` extends `ModRM.reg`, `b` extends `ModRM.rm`/`SIB.base`/the
    /// opcode register. `REX.X` is always 0 — no instruction here uses a SIB *index*.
    ///
    /// A REX byte of exactly `0x40` carries no information and is normally omitted, **except** for
    /// 8-bit register operands: the mere presence of any REX prefix reinterprets r/m8 encodings
    /// 4–7 as `SPL/BPL/SIL/DIL` instead of `AH/CH/DH/BH`. That is what `force` is for — see
    /// [`Asm::setcc`].
    fn rex(&mut self, w: bool, r: bool, b: bool, force: bool) {
        let byte = 0x40 | (u8::from(w) << 3) | (u8::from(r) << 2) | u8::from(b);
        if byte != 0x40 || force {
            self.byte(byte);
        }
    }

    /// `mod<<6 | reg<<3 | rm`. `reg` is either a register's low 3 bits or an opcode-extension digit.
    fn modrm(&mut self, md: u8, reg: u8, rm: u8) {
        self.byte((md << 6) | ((reg & 7) << 3) | (rm & 7));
    }

    /// ModRM for the register-direct form (`mod = 0b11`).
    fn modrm_reg(&mut self, reg: u8, rm: Reg) {
        self.modrm(0b11, reg, rm.low3());
    }

    /// ModRM (+ SIB, + displacement) for `[base + disp]`, handling the three traps documented at
    /// the top of this module.
    fn modrm_mem(&mut self, reg: u8, m: Mem) {
        let rm = m.base.low3();
        // rm == 4 (RSP, R12) is the "SIB follows" escape, so those bases always need a SIB byte.
        let need_sib = rm == 4;
        // rm == 5 (RBP, R13) has no mod=00 form — that slot means RIP-relative — so a zero
        // displacement has to be spelled out as an explicit disp8.
        let (md, disp_len) = if m.disp == 0 && rm != 5 {
            (0b00, 0)
        } else if i8::try_from(m.disp).is_ok() {
            (0b01, 1)
        } else {
            (0b10, 4)
        };
        self.modrm(md, reg, rm);
        if need_sib {
            // scale = 00 (×1), index = 100 (none), base = 100 (the low 3 bits of RSP/R12; which of
            // the two it is comes from REX.B, already emitted by the caller).
            self.byte(0b00_100_100);
        }
        match disp_len {
            1 => self.byte(m.disp as u8),
            4 => self.imm32(m.disp),
            _ => {}
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Data movement.
// ---------------------------------------------------------------------------------------------

impl Asm {
    /// `mov dst, imm` (64-bit).
    ///
    /// Picks the shorter of the two encodings, deterministically:
    /// - `imm` fits in a signed 32-bit value → `REX.W + C7 /0 id` (7 bytes), which sign-extends;
    /// - otherwise → `REX.W + B8+rd io` (10 bytes), the `movabs` form with a full 64-bit immediate.
    pub fn mov_ri(&mut self, dst: Reg, imm: i64) {
        if let Ok(small) = i32::try_from(imm) {
            self.rex(true, false, dst.is_extended(), false);
            self.byte(0xC7);
            self.modrm_reg(0, dst);
            self.imm32(small);
        } else {
            self.rex(true, false, dst.is_extended(), false);
            self.byte(0xB8 + dst.low3());
            self.imm64(imm);
        }
    }

    /// `movabs dst, imm` — always the 10-byte `REX.W + B8+rd io` form, even for small immediates.
    /// A JIT needs this when it must patch the constant later and therefore needs a fixed length.
    pub fn movabs(&mut self, dst: Reg, imm: i64) {
        self.rex(true, false, dst.is_extended(), false);
        self.byte(0xB8 + dst.low3());
        self.imm64(imm);
    }

    /// `mov dst, src` (64-bit) — `REX.W + 89 /r`, i.e. `MOV r/m64, r64`, so `src` goes in the
    /// `reg` field and `dst` in `rm`. Reversed from the `8B` form; mixing the two silently swaps
    /// the operands, which is why there is a round-trip test for it.
    pub fn mov_rr(&mut self, dst: Reg, src: Reg) {
        self.rex(true, src.is_extended(), dst.is_extended(), false);
        self.byte(0x89);
        self.modrm_reg(src.low3(), dst);
    }

    /// `mov dst, [base+disp]` — `REX.W + 8B /r` (`MOV r64, r/m64`).
    pub fn mov_rm(&mut self, dst: Reg, src: Mem) {
        self.rex(true, dst.is_extended(), src.base.is_extended(), false);
        self.byte(0x8B);
        self.modrm_mem(dst.low3(), src);
    }

    /// `mov [base+disp], src` — `REX.W + 89 /r` (`MOV r/m64, r64`).
    pub fn mov_mr(&mut self, dst: Mem, src: Reg) {
        self.rex(true, src.is_extended(), dst.base.is_extended(), false);
        self.byte(0x89);
        self.modrm_mem(src.low3(), dst);
    }

    /// `mov [base+disp], src32` — the same `89 /r` opcode **without** `REX.W`, storing only the
    /// low 32 bits. This is how a Java `int` gets written back to a 4-byte slot after 64-bit
    /// arithmetic: the store *is* the wrap-to-32-bits the JLS mandates.
    pub fn mov_mr32(&mut self, dst: Mem, src: Reg) {
        self.rex(false, src.is_extended(), dst.base.is_extended(), false);
        self.byte(0x89);
        self.modrm_mem(src.low3(), dst);
    }

    /// `movsxd dst, src32` — `REX.W + 63 /r`, sign-extending the low 32 bits of `src` into the
    /// full 64-bit `dst`. The inverse of [`mov_mr32`]: it re-canonicalises a Java `int` after a
    /// truncating operation.
    ///
    /// [`mov_mr32`]: Asm::mov_mr32
    pub fn movsxd_rr(&mut self, dst: Reg, src: Reg) {
        self.rex(true, dst.is_extended(), src.is_extended(), false);
        self.byte(0x63);
        self.modrm_reg(dst.low3(), src);
    }

    /// `movsxd dst, dword [base+disp]` — loads a 32-bit slot sign-extended into 64 bits.
    pub fn movsxd_rm(&mut self, dst: Reg, src: Mem) {
        self.rex(true, dst.is_extended(), src.base.is_extended(), false);
        self.byte(0x63);
        self.modrm_mem(dst.low3(), src);
    }

    /// `movzx dst, src8` — `REX.W + 0F B6 /r`, zero-extending the low byte. Pairs with
    /// [`setcc`][Asm::setcc] to materialise a Java `boolean`.
    pub fn movzx_rr8(&mut self, dst: Reg, src: Reg) {
        self.rex(true, dst.is_extended(), src.is_extended(), false);
        self.byte(0x0F);
        self.byte(0xB6);
        self.modrm_reg(dst.low3(), src);
    }

    /// `mov dst32, src32` — `89 /r` **without** `REX.W`.
    ///
    /// The point is the side effect: on x86-64 *every* write to a 32-bit register zero-extends
    /// into the full 64-bit one. So this is the two-byte "take the low 32 bits, zero-extend" —
    /// the exact inverse of [`movsxd_rr`][Asm::movsxd_rr], and what `iushr` needs before a
    /// logical shift (a sign-extended `-1` must become `0x0000_0000_FFFF_FFFF`, or `-1 >>> 1`
    /// answers `-1` instead of `Integer.MAX_VALUE`).
    pub fn mov_rr32(&mut self, dst: Reg, src: Reg) {
        self.rex(false, src.is_extended(), dst.is_extended(), false);
        self.byte(0x89);
        self.modrm_reg(src.low3(), dst);
    }

    /// `mov dst32, dword [base+disp]` — `8B /r` without `REX.W`: loads 4 bytes and
    /// **zero-extends** them into the 64-bit `dst`. The load-from-memory twin of
    /// [`mov_rr32`][Asm::mov_rr32].
    pub fn mov_rm32(&mut self, dst: Reg, src: Mem) {
        self.rex(false, dst.is_extended(), src.base.is_extended(), false);
        self.byte(0x8B);
        self.modrm_mem(dst.low3(), src);
    }
}

// ---------------------------------------------------------------------------------------------
// The ALU group. ADD/OR/AND/SUB/XOR/CMP share one encoding family, parameterised by opcode
// (register forms) or by the ModRM `/digit` (immediate forms).
// ---------------------------------------------------------------------------------------------

/// The `/digit` opcode extensions of the `80`/`81`/`83` immediate group (Intel Vol. 2, "Group 1").
const ALU_ADD: u8 = 0;
const ALU_OR: u8 = 1;
const ALU_AND: u8 = 4;
const ALU_SUB: u8 = 5;
const ALU_XOR: u8 = 6;
const ALU_CMP: u8 = 7;

impl Asm {
    /// `op dst, src` for the `r/m64, r64` opcode `op` (`01` add, `09` or, `21` and, `29` sub,
    /// `31` xor, `39` cmp). `src` lands in `reg`, `dst` in `rm`.
    fn alu_rr(&mut self, op: u8, dst: Reg, src: Reg) {
        self.rex(true, src.is_extended(), dst.is_extended(), false);
        self.byte(op);
        self.modrm_reg(src.low3(), dst);
    }

    /// `op dst, [base+disp]` for the `r64, r/m64` opcode (the `r/m64, r64` opcode + 2).
    fn alu_rm(&mut self, op: u8, dst: Reg, src: Mem) {
        self.rex(true, dst.is_extended(), src.base.is_extended(), false);
        self.byte(op + 2);
        self.modrm_mem(dst.low3(), src);
    }

    /// `op dst, imm` — `83 /digit ib` when the immediate fits in a sign-extended byte (4 bytes
    /// total), else `81 /digit id` (7 bytes).
    fn alu_ri(&mut self, digit: u8, dst: Reg, imm: i32) {
        self.rex(true, false, dst.is_extended(), false);
        if let Ok(small) = i8::try_from(imm) {
            self.byte(0x83);
            self.modrm_reg(digit, dst);
            self.byte(small as u8);
        } else {
            self.byte(0x81);
            self.modrm_reg(digit, dst);
            self.imm32(imm);
        }
    }

    /// `add dst, src`.
    pub fn add_rr(&mut self, dst: Reg, src: Reg) {
        self.alu_rr(0x01, dst, src);
    }

    /// `add dst, [base+disp]`.
    pub fn add_rm(&mut self, dst: Reg, src: Mem) {
        self.alu_rm(0x01, dst, src);
    }

    /// `add dst, imm`.
    pub fn add_ri(&mut self, dst: Reg, imm: i32) {
        self.alu_ri(ALU_ADD, dst, imm);
    }

    /// `sub dst, src`.
    pub fn sub_rr(&mut self, dst: Reg, src: Reg) {
        self.alu_rr(0x29, dst, src);
    }

    /// `sub dst, [base+disp]`.
    pub fn sub_rm(&mut self, dst: Reg, src: Mem) {
        self.alu_rm(0x29, dst, src);
    }

    /// `sub dst, imm`.
    pub fn sub_ri(&mut self, dst: Reg, imm: i32) {
        self.alu_ri(ALU_SUB, dst, imm);
    }

    /// `cmp lhs, rhs` — sets the flags from `lhs - rhs` without storing the result.
    pub fn cmp_rr(&mut self, lhs: Reg, rhs: Reg) {
        self.alu_rr(0x39, lhs, rhs);
    }

    /// `cmp lhs, [base+disp]`.
    pub fn cmp_rm(&mut self, lhs: Reg, rhs: Mem) {
        self.alu_rm(0x39, lhs, rhs);
    }

    /// `cmp lhs, imm`.
    pub fn cmp_ri(&mut self, lhs: Reg, imm: i32) {
        self.alu_ri(ALU_CMP, lhs, imm);
    }

    /// `and dst, src`.
    pub fn and_rr(&mut self, dst: Reg, src: Reg) {
        self.alu_rr(0x21, dst, src);
    }

    /// `and dst, imm`.
    pub fn and_ri(&mut self, dst: Reg, imm: i32) {
        self.alu_ri(ALU_AND, dst, imm);
    }

    /// `or dst, src`.
    pub fn or_rr(&mut self, dst: Reg, src: Reg) {
        self.alu_rr(0x09, dst, src);
    }

    /// `and dst, [base+disp]`.
    pub fn and_rm(&mut self, dst: Reg, src: Mem) {
        self.alu_rm(0x21, dst, src);
    }

    /// `or dst, src`.
    pub fn or_rm(&mut self, dst: Reg, src: Mem) {
        self.alu_rm(0x09, dst, src);
    }

    /// `xor dst, [base+disp]`.
    pub fn xor_rm(&mut self, dst: Reg, src: Mem) {
        self.alu_rm(0x31, dst, src);
    }

    /// `or dst, imm`.
    pub fn or_ri(&mut self, dst: Reg, imm: i32) {
        self.alu_ri(ALU_OR, dst, imm);
    }

    /// `xor dst, src`. `xor r, r` is the idiomatic (and shortest) way to zero a register.
    pub fn xor_rr(&mut self, dst: Reg, src: Reg) {
        self.alu_rr(0x31, dst, src);
    }

    /// `xor dst, imm`.
    pub fn xor_ri(&mut self, dst: Reg, imm: i32) {
        self.alu_ri(ALU_XOR, dst, imm);
    }

    /// `neg dst` — `REX.W + F7 /3`, two's-complement negation.
    pub fn neg(&mut self, dst: Reg) {
        self.rex(true, false, dst.is_extended(), false);
        self.byte(0xF7);
        self.modrm_reg(3, dst);
    }
}

// ---------------------------------------------------------------------------------------------
// Shifts. All by `CL`, all 64-bit (`REX.W + D3 /digit`).
// ---------------------------------------------------------------------------------------------

/// `/digit` extensions of the `D0`–`D3` shift group (Intel Vol. 2, "Group 2").
const SHIFT_SHL: u8 = 4;
const SHIFT_SHR: u8 = 5;
const SHIFT_SAR: u8 = 7;

impl Asm {
    /// `<shift> dst, cl` — `REX.W + D3 /digit`.
    ///
    /// # The masking trap
    ///
    /// x86 masks the shift count to the **low 5 bits for 32-bit** operands and to the **low 6
    /// bits for 64-bit** ones. Java masks `int` shifts to 5 bits *always* (JLS §15.19), so these
    /// 64-bit forms are **not** a drop-in for Java semantics: `x << 33` would shift by 33 here
    /// and by 1 in Java. A compiler using them must `and cl, 31` first — which is what
    /// [`crate::burst::compile`] does, explicitly, rather than relying on the operand size.
    fn shift_cl(&mut self, digit: u8, dst: Reg) {
        self.rex(true, false, dst.is_extended(), false);
        self.byte(0xD3);
        self.modrm_reg(digit, dst);
    }

    /// `shl dst, cl` — left shift, zeroes shifted in.
    pub fn shl_cl(&mut self, dst: Reg) {
        self.shift_cl(SHIFT_SHL, dst);
    }

    /// `shr dst, cl` — **logical** right shift, zeroes shifted in. Java's `>>>` over a value
    /// that has first been zero-extended to 32 bits (see [`Asm::mov_rr32`]).
    pub fn shr_cl(&mut self, dst: Reg) {
        self.shift_cl(SHIFT_SHR, dst);
    }

    /// `sar dst, cl` — **arithmetic** right shift, the sign bit shifted in. Java's `>>`.
    pub fn sar_cl(&mut self, dst: Reg) {
        self.shift_cl(SHIFT_SAR, dst);
    }
}

// ---------------------------------------------------------------------------------------------
// Multiply and divide.
// ---------------------------------------------------------------------------------------------

impl Asm {
    /// `imul dst, src` — `REX.W + 0F AF /r`.
    ///
    /// **Note the operand order is the opposite of `add`/`sub`**: here `dst` goes in the `reg`
    /// field and `src` in `rm`, because the opcode is the `r64, r/m64` form. Swapping them is
    /// invisible for `imul rax, rax` and wrong everywhere else.
    pub fn imul_rr(&mut self, dst: Reg, src: Reg) {
        self.rex(true, dst.is_extended(), src.is_extended(), false);
        self.byte(0x0F);
        self.byte(0xAF);
        self.modrm_reg(dst.low3(), src);
    }

    /// `imul dst, [base+disp]`.
    pub fn imul_rm(&mut self, dst: Reg, src: Mem) {
        self.rex(true, dst.is_extended(), src.base.is_extended(), false);
        self.byte(0x0F);
        self.byte(0xAF);
        self.modrm_mem(dst.low3(), src);
    }

    /// `imul dst, src, imm` — the three-operand form: `6B /r ib` for a byte-sized immediate,
    /// else `69 /r id`.
    pub fn imul_rri(&mut self, dst: Reg, src: Reg, imm: i32) {
        self.rex(true, dst.is_extended(), src.is_extended(), false);
        if let Ok(small) = i8::try_from(imm) {
            self.byte(0x6B);
            self.modrm_reg(dst.low3(), src);
            self.byte(small as u8);
        } else {
            self.byte(0x69);
            self.modrm_reg(dst.low3(), src);
            self.imm32(imm);
        }
    }

    /// `cqo` — `REX.W + 99`. Sign-extends RAX into **RDX:RAX**, the 128-bit dividend
    /// [`idiv`][Asm::idiv] implicitly consumes. Forgetting it leaves stale bits in RDX and yields
    /// a garbage quotient (or a `#DE` when the quotient overflows 64 bits).
    pub fn cqo(&mut self) {
        self.rex(true, false, false, false);
        self.byte(0x99);
    }

    /// `cdq` — bare `99`. The 32-bit sibling of [`cqo`][Asm::cqo]: sign-extends EAX into EDX:EAX.
    /// Provided for completeness; this module's arithmetic is 64-bit, so [`cqo`][Asm::cqo] is the
    /// one to pair with [`idiv`][Asm::idiv].
    pub fn cdq(&mut self) {
        self.byte(0x99);
    }

    /// `idiv divisor` — `REX.W + F7 /7`.
    ///
    /// Fully implicit on both sides: the dividend is **RDX:RAX** (so [`cqo`][Asm::cqo] must run
    /// first), the quotient lands in **RAX** and the remainder in **RDX**. Both are clobbered, so
    /// nothing live may sit in RDX across this instruction. A zero divisor raises `#DE`, which on
    /// Windows surfaces as an `EXCEPTION_INT_DIVIDE_BY_ZERO` — a real JIT must emit an explicit
    /// zero check to throw `ArithmeticException` instead.
    pub fn idiv(&mut self, divisor: Reg) {
        self.rex(true, false, divisor.is_extended(), false);
        self.byte(0xF7);
        self.modrm_reg(7, divisor);
    }
}

// ---------------------------------------------------------------------------------------------
// Control flow: labels, branches, calls, stack.
// ---------------------------------------------------------------------------------------------

impl Asm {
    /// A fresh, unbound label. Branching to it before [`bind`][Asm::bind] is the forward-reference
    /// case: the displacement is written as a placeholder and patched by
    /// [`finish`][Asm::finish].
    pub fn new_label(&mut self) -> Label {
        self.labels.push(None);
        Label(self.labels.len() - 1)
    }

    /// Pins `label` to the current offset. Panics if it was already bound — that is always a
    /// compiler bug (two basic blocks claiming the same name), and silently accepting it would
    /// send half the branches to the wrong place.
    pub fn bind(&mut self, label: Label) {
        assert!(self.labels[label.0].is_none(), "label {} bound twice", label.0);
        self.labels[label.0] = Some(self.code.len());
    }

    /// Reserves 4 placeholder bytes for a `rel32` and records the patch site.
    fn rel32_to(&mut self, label: Label) {
        self.fixups.push(Fixup { site: self.code.len(), label });
        self.imm32(0);
    }

    /// `jmp label` — always the `E9 cd` (`rel32`) form.
    ///
    /// No branch shortening: a 5-byte `jmp` is emitted even when the 2-byte `EB cb` form would
    /// reach. Shortening a branch moves every later instruction, which invalidates offsets already
    /// recorded for *other* branches — an iterative relaxation pass. That is a size optimisation,
    /// not a correctness one, so it is deliberately left out of step 1.
    pub fn jmp(&mut self, label: Label) {
        self.byte(0xE9);
        self.rel32_to(label);
    }

    /// `jcc label` — `0F 80+tttn cd`.
    pub fn jcc(&mut self, cond: Cond, label: Label) {
        self.byte(0x0F);
        self.byte(0x80 + cond.tttn());
        self.rel32_to(label);
    }

    /// `setcc dst8` — `0F 90+tttn /0`, writing 0 or 1 into the **low byte** of `dst` and leaving
    /// the upper 56 bits untouched (so pair it with [`movzx_rr8`][Asm::movzx_rr8], or zero the
    /// register first).
    ///
    /// This is the one place a bare `0x40` REX prefix is meaningful: for an 8-bit operand,
    /// encodings 4–7 mean `AH/CH/DH/BH` *without* REX and `SPL/BPL/SIL/DIL` *with* it. Emitting
    /// `setne sil` without the prefix would write into `DH` — a different register entirely.
    pub fn setcc(&mut self, cond: Cond, dst: Reg) {
        let needs_bare_rex = matches!(dst, Reg::Rsp | Reg::Rbp | Reg::Rsi | Reg::Rdi);
        self.rex(false, false, dst.is_extended(), needs_bare_rex);
        self.byte(0x0F);
        self.byte(0x90 + cond.tttn());
        self.modrm_reg(0, dst);
    }

    /// `call target` — `FF /2`, an indirect call through a register. 64-bit is the default
    /// operand size for near calls, so no `REX.W`.
    ///
    /// The caller is responsible for the ABI around it: 32 bytes of shadow space reserved and
    /// `RSP` 16-byte aligned at this instruction. [`Frame`] arranges both.
    pub fn call_r(&mut self, target: Reg) {
        self.rex(false, false, target.is_extended(), false);
        self.byte(0xFF);
        self.modrm_reg(2, target);
    }

    /// `push reg` — `50+rd`. 64-bit is the default operand size, so REX carries only `B`.
    pub fn push(&mut self, reg: Reg) {
        self.rex(false, false, reg.is_extended(), false);
        self.byte(0x50 + reg.low3());
    }

    /// `pop reg` — `58+rd`.
    pub fn pop(&mut self, reg: Reg) {
        self.rex(false, false, reg.is_extended(), false);
        self.byte(0x58 + reg.low3());
    }

    /// `ret` — `C3`, the near return.
    pub fn ret(&mut self) {
        self.byte(0xC3);
    }

    /// `int3` — `CC`. Useful as inter-function padding: an accidental fall-through traps
    /// immediately instead of decoding whatever bytes follow.
    pub fn int3(&mut self) {
        self.byte(0xCC);
    }

    /// `nop` — `90`.
    pub fn nop(&mut self) {
        self.byte(0x90);
    }

    /// Resolves every branch and returns the finished machine code.
    ///
    /// The displacement of a `rel32` is measured from the **end of the instruction**, which is
    /// exactly `site + 4` — the byte after the displacement field. Both forward and backward
    /// branches are patched here rather than eagerly, so there is a single implementation of that
    /// arithmetic and therefore a single place for it to be wrong.
    pub fn finish(mut self) -> Result<Vec<u8>, AsmError> {
        for fixup in &self.fixups {
            let target = self.labels[fixup.label.0].ok_or(AsmError::UnboundLabel(fixup.label))?;
            let next_ip = fixup.site + 4;
            let delta = target as i64 - next_ip as i64;
            let rel = i32::try_from(delta)
                .map_err(|_| AsmError::BranchOutOfRange { from: next_ip, to: target })?;
            self.code[fixup.site..fixup.site + 4].copy_from_slice(&rel.to_le_bytes());
        }
        self.fixups.clear();
        Ok(self.code)
    }
}

// ---------------------------------------------------------------------------------------------
// The frame: prologue, epilogue, local slots.
// ---------------------------------------------------------------------------------------------

/// A Microsoft x64 stack frame for a generated function.
///
/// # Layout (higher addresses at the top)
///
/// ```text
///   ...caller's frame...
///   [rbp+16.. ]  caller's shadow space (32 bytes; our incoming args may be spilled here)
///   [rbp+8   ]  return address
///   [rbp+0   ]  saved RBP                 <- rbp
///   [rbp-8.. ]  saved non-volatile registers, in `saved` order
///               ---- sub rsp, frame_bytes ----
///   [rsp+32.. ]  local slots, 8 bytes each: local 0 at [rsp+32], local 1 at [rsp+40], ...
///   [rsp+0..32]  our outgoing shadow space                          <- rsp
/// ```
///
/// # Why locals are addressed from RSP, not RBP
///
/// `RSP` does not move inside the body (nothing is pushed after the prologue), so `[rsp+k]` is a
/// constant offset — and, unlike an RBP-relative offset, it does **not** depend on how many
/// non-volatile registers happened to be saved. `RBP` is still set up as a conventional frame
/// pointer so a debugger can walk the chain.
///
/// # Alignment
///
/// The ABI guarantees `RSP % 16 == 0` *at the `call`*, so a callee sees `RSP % 16 == 8` on entry
/// (the return address). Each `push` subtracts 8, so after `k` pushes `RSP ≡ 8 - 8k (mod 16)`;
/// `frame_bytes` is then chosen to bring it back to 0. Getting this wrong does not crash *here* —
/// it crashes inside some callee that uses an aligned SSE store, arbitrarily far away.
///
/// # Shadow space
///
/// 32 bytes are reserved unconditionally, even for leaf functions that never call anything. It
/// costs one immediate in the `sub` and means a call can be emitted later without revisiting the
/// prologue.
///
/// # Known gap
///
/// No unwind data is registered (`RtlAddFunctionTable`). Windows x64 uses table-driven unwinding,
/// so a structured exception propagating *through* one of these frames cannot be unwound. Fine for
/// self-contained leaf code; a prerequisite before generated code can call back into the runtime
/// and have exceptions cross the boundary.
pub struct Frame {
    /// Number of 8-byte local slots.
    pub locals: u32,
    /// Non-volatile registers this function uses and therefore must save.
    pub saved: Vec<Reg>,
    /// Bytes subtracted from RSP after the pushes: shadow space + locals + alignment padding.
    frame_bytes: i32,
}

impl Frame {
    /// A frame with `locals` 8-byte slots, saving `saved` (which should list only registers from
    /// [`NON_VOLATILE`] that the body actually writes).
    pub fn new(locals: u32, saved: &[Reg]) -> Frame {
        // One push for RBP, plus one per saved register.
        let pushes = 1 + saved.len() as u32;
        let need = 32 + 8 * locals;
        let mut frame_bytes = need.next_multiple_of(16);
        // After `pushes` pushes, RSP ≡ 8 - 8*pushes (mod 16). An odd count already leaves it at 0,
        // so a multiple of 16 keeps it there; an even count leaves it at 8 and needs 8 more.
        if pushes.is_multiple_of(2) {
            frame_bytes += 8;
        }
        Frame { locals, saved: saved.to_vec(), frame_bytes: frame_bytes as i32 }
    }

    /// Total bytes subtracted from RSP by the prologue's `sub`.
    pub fn frame_bytes(&self) -> i32 {
        self.frame_bytes
    }

    /// The memory operand for local slot `i`, i.e. `[rsp + 32 + 8i]` — above our outgoing shadow
    /// space so a `call` cannot scribble on it. Panics on an out-of-range slot.
    pub fn local(&self, i: u32) -> Mem {
        assert!(i < self.locals, "local {i} out of range (frame has {})", self.locals);
        Mem::at(Reg::Rsp, 32 + 8 * i as i32)
    }

    /// The register holding incoming integer argument `i` (0-based). Panics beyond the fourth —
    /// arguments 5+ arrive on the stack, which this step does not implement.
    pub fn arg(&self, i: usize) -> Reg {
        assert!(i < ARG_REGS.len(), "argument {i} is passed on the stack, which is not supported yet");
        ARG_REGS[i]
    }

    /// Emits `push rbp; mov rbp, rsp; push <saved>...; sub rsp, frame_bytes`.
    pub fn prologue(&self, a: &mut Asm) {
        a.push(Reg::Rbp);
        a.mov_rr(Reg::Rbp, Reg::Rsp);
        for &r in &self.saved {
            a.push(r);
        }
        if self.frame_bytes != 0 {
            a.sub_ri(Reg::Rsp, self.frame_bytes);
        }
    }

    /// Emits `add rsp, frame_bytes; pop <saved in reverse>...; pop rbp; ret`.
    ///
    /// The pops must mirror the prologue exactly and in reverse — an unbalanced stack returns to
    /// whatever happens to sit at the wrong slot, which is an immediate wild jump.
    pub fn epilogue(&self, a: &mut Asm) {
        if self.frame_bytes != 0 {
            a.add_ri(Reg::Rsp, self.frame_bytes);
        }
        for &r in self.saved.iter().rev() {
            a.pop(r);
        }
        a.pop(Reg::Rbp);
        a.ret();
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Assembles `f` and compares the raw bytes (no label resolution) against `expected`.
    fn enc(f: impl FnOnce(&mut Asm)) -> Vec<u8> {
        let mut a = Asm::new();
        f(&mut a);
        a.code().to_vec()
    }

    // -- Round-trip encoding tests -------------------------------------------------------------
    //
    // Every expectation below cites Intel SDM Vol. 2 opcode + ModRM reasoning. These catch the
    // errors execution can mask by luck (e.g. a swapped ModRM.reg/rm that is a no-op when both
    // operands are RAX).

    #[test]
    fn mov_rr_operand_order() {
        // MOV r/m64, r64 = REX.W + 89 /r, with the *source* in ModRM.reg.
        // mov rax, rcx -> REX.W=48, 89, ModRM = 11 001 000 = C8.
        assert_eq!(enc(|a| a.mov_rr(Reg::Rax, Reg::Rcx)), vec![0x48, 0x89, 0xC8]);
        // mov rcx, rax -> ModRM = 11 000 001 = C1. Different bytes: the order is not symmetric.
        assert_eq!(enc(|a| a.mov_rr(Reg::Rcx, Reg::Rax)), vec![0x48, 0x89, 0xC1]);
        // mov rbp, rsp -> ModRM = 11 100 101 = E5.
        assert_eq!(enc(|a| a.mov_rr(Reg::Rbp, Reg::Rsp)), vec![0x48, 0x89, 0xE5]);
    }

    #[test]
    fn rex_b_and_r_for_extended_regs() {
        // mov r8, rcx: dst is rm -> REX.B. REX = 0x48|0x01 = 49.
        assert_eq!(enc(|a| a.mov_rr(Reg::R8, Reg::Rcx)), vec![0x49, 0x89, 0xC8]);
        // mov rcx, r8: src is reg -> REX.R. REX = 0x48|0x04 = 4C, ModRM = 11 000 001 = C1.
        assert_eq!(enc(|a| a.mov_rr(Reg::Rcx, Reg::R8)), vec![0x4C, 0x89, 0xC1]);
        // mov r15, r8: both -> REX = 0x48|0x04|0x01 = 4D, ModRM = 11 000 111 = C7.
        assert_eq!(enc(|a| a.mov_rr(Reg::R15, Reg::R8)), vec![0x4D, 0x89, 0xC7]);
        // push/pop use REX.B only (64-bit is the default operand size, so no REX.W).
        assert_eq!(enc(|a| a.push(Reg::Rbp)), vec![0x55]);
        assert_eq!(enc(|a| a.push(Reg::R15)), vec![0x41, 0x57]);
        assert_eq!(enc(|a| a.pop(Reg::R15)), vec![0x41, 0x5F]);
        assert_eq!(enc(|a| a.pop(Reg::Rbp)), vec![0x5D]);
    }

    #[test]
    fn mov_immediates() {
        // Small immediate -> REX.W + C7 /0 id, ModRM = 11 000 000 = C0.
        assert_eq!(enc(|a| a.mov_ri(Reg::Rax, 5)), vec![0x48, 0xC7, 0xC0, 5, 0, 0, 0]);
        // Same for R8, with REX.B: 49 C7 C0.
        assert_eq!(enc(|a| a.mov_ri(Reg::R8, 5)), vec![0x49, 0xC7, 0xC0, 5, 0, 0, 0]);
        // Negative fits in a sign-extended imm32.
        assert_eq!(enc(|a| a.mov_ri(Reg::Rax, -1)), vec![0x48, 0xC7, 0xC0, 0xFF, 0xFF, 0xFF, 0xFF]);
        // Too large -> REX.W + B8+rd io (movabs).
        assert_eq!(
            enc(|a| a.mov_ri(Reg::R8, 0x1122_3344_5566_7788)),
            vec![0x49, 0xB8, 0x88, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11]
        );
        // movabs forces the 10-byte form even for a small value.
        assert_eq!(enc(|a| a.movabs(Reg::Rax, 1)), vec![0x48, 0xB8, 1, 0, 0, 0, 0, 0, 0, 0]);
    }

    #[test]
    fn memory_operands_sib_and_rbp_traps() {
        // mov rax, [rsp+0x20]: rm=100 forces SIB 24; disp8 -> mod=01, ModRM = 01 000 100 = 44.
        assert_eq!(
            enc(|a| a.mov_rm(Reg::Rax, Mem::at(Reg::Rsp, 0x20))),
            vec![0x48, 0x8B, 0x44, 0x24, 0x20]
        );
        // mov [rsp+0x20], r9: source in ModRM.reg -> REX.R (4C), ModRM = 01 001 100 = 4C.
        assert_eq!(
            enc(|a| a.mov_mr(Mem::at(Reg::Rsp, 0x20), Reg::R9)),
            vec![0x4C, 0x89, 0x4C, 0x24, 0x20]
        );
        // [r12+0]: low3 = 100 -> SIB needed, and REX.B distinguishes it from RSP. disp 0 -> mod=00.
        assert_eq!(
            enc(|a| a.mov_rm(Reg::Rax, Mem::at(Reg::R12, 0))),
            vec![0x49, 0x8B, 0x04, 0x24]
        );
        // [r13+0]: low3 = 101 has no mod=00 form (that slot is RIP-relative), so an explicit
        // zero disp8 with mod=01. ModRM = 01 000 101 = 45.
        assert_eq!(
            enc(|a| a.mov_rm(Reg::Rax, Mem::at(Reg::R13, 0))),
            vec![0x49, 0x8B, 0x45, 0x00]
        );
        // Same trap for RBP itself.
        assert_eq!(enc(|a| a.mov_rm(Reg::Rax, Mem::at(Reg::Rbp, 0))), vec![0x48, 0x8B, 0x45, 0x00]);
        // A base with no trap and disp 0 -> mod=00, no SIB, no displacement.
        assert_eq!(enc(|a| a.mov_rm(Reg::Rax, Mem::at(Reg::Rcx, 0))), vec![0x48, 0x8B, 0x01]);
        // Large displacement -> mod=10 and a full disp32.
        assert_eq!(
            enc(|a| a.mov_rm(Reg::Rax, Mem::at(Reg::Rcx, 0x1234))),
            vec![0x48, 0x8B, 0x81, 0x34, 0x12, 0x00, 0x00]
        );
        // 32-bit store: identical, minus REX.W. Here that leaves REX = 0x40, which carries no
        // information for a 32-bit operand, so it is omitted entirely.
        assert_eq!(
            enc(|a| a.mov_mr32(Mem::at(Reg::Rcx, 0), Reg::Rax)),
            vec![0x89, 0x01]
        );
    }

    #[test]
    fn alu_forms() {
        // add rax, rcx -> REX.W + 01 /r, ModRM = 11 001 000 = C8.
        assert_eq!(enc(|a| a.add_rr(Reg::Rax, Reg::Rcx)), vec![0x48, 0x01, 0xC8]);
        // sub rax, rcx -> 29 /r.
        assert_eq!(enc(|a| a.sub_rr(Reg::Rax, Reg::Rcx)), vec![0x48, 0x29, 0xC8]);
        // cmp rax, rcx -> 39 /r.
        assert_eq!(enc(|a| a.cmp_rr(Reg::Rax, Reg::Rcx)), vec![0x48, 0x39, 0xC8]);
        // xor rax, rax -> 31 /r, ModRM = 11 000 000 = C0. The canonical zeroing idiom.
        assert_eq!(enc(|a| a.xor_rr(Reg::Rax, Reg::Rax)), vec![0x48, 0x31, 0xC0]);
        // sub rsp, 0x28 -> 83 /5 ib, ModRM = 11 101 100 = EC.
        assert_eq!(enc(|a| a.sub_ri(Reg::Rsp, 0x28)), vec![0x48, 0x83, 0xEC, 0x28]);
        // add rsp, 0x28 -> 83 /0 ib, ModRM = 11 000 100 = C4.
        assert_eq!(enc(|a| a.add_ri(Reg::Rsp, 0x28)), vec![0x48, 0x83, 0xC4, 0x28]);
        // cmp rcx, 10 -> 83 /7 ib, ModRM = 11 111 001 = F9.
        assert_eq!(enc(|a| a.cmp_ri(Reg::Rcx, 10)), vec![0x48, 0x83, 0xF9, 0x0A]);
        // An immediate too big for imm8 -> the 81 /digit id form.
        assert_eq!(
            enc(|a| a.add_ri(Reg::Rax, 0x1000)),
            vec![0x48, 0x81, 0xC0, 0x00, 0x10, 0x00, 0x00]
        );
        // add rax, [rsp+0x20] -> the r64,r/m64 opcode 03.
        assert_eq!(
            enc(|a| a.add_rm(Reg::Rax, Mem::at(Reg::Rsp, 0x20))),
            vec![0x48, 0x03, 0x44, 0x24, 0x20]
        );
        // neg r8 -> REX.W|B + F7 /3, ModRM = 11 011 000 = D8.
        assert_eq!(enc(|a| a.neg(Reg::R8)), vec![0x49, 0xF7, 0xD8]);
    }

    #[test]
    fn mul_div_forms() {
        // imul rax, rcx -> REX.W + 0F AF /r with dst in ModRM.reg: ModRM = 11 000 001 = C1.
        // Note this is the mirror image of `add rax, rcx` (ModRM C8) -- different operand order.
        assert_eq!(enc(|a| a.imul_rr(Reg::Rax, Reg::Rcx)), vec![0x48, 0x0F, 0xAF, 0xC1]);
        // imul r9, r10 -> REX = 0x48|R|B = 4D, ModRM = 11 001 010 = CA.
        assert_eq!(enc(|a| a.imul_rr(Reg::R9, Reg::R10)), vec![0x4D, 0x0F, 0xAF, 0xCA]);
        // imul rax, rcx, 3 -> 6B /r ib.
        assert_eq!(enc(|a| a.imul_rri(Reg::Rax, Reg::Rcx, 3)), vec![0x48, 0x6B, 0xC1, 0x03]);
        // imul rax, rcx, 0x1000 -> 69 /r id.
        assert_eq!(
            enc(|a| a.imul_rri(Reg::Rax, Reg::Rcx, 0x1000)),
            vec![0x48, 0x69, 0xC1, 0x00, 0x10, 0x00, 0x00]
        );
        // cqo -> REX.W + 99; cdq -> bare 99.
        assert_eq!(enc(|a| a.cqo()), vec![0x48, 0x99]);
        assert_eq!(enc(|a| a.cdq()), vec![0x99]);
        // idiv rcx -> REX.W + F7 /7, ModRM = 11 111 001 = F9.
        assert_eq!(enc(|a| a.idiv(Reg::Rcx)), vec![0x48, 0xF7, 0xF9]);
        // idiv r15 -> REX.W|B = 49, ModRM = 11 111 111 = FF.
        assert_eq!(enc(|a| a.idiv(Reg::R15)), vec![0x49, 0xF7, 0xFF]);
    }

    #[test]
    fn setcc_needs_a_bare_rex_for_the_new_byte_registers() {
        // sete al -> 0F 94 /0, ModRM = 11 000 000 = C0. No REX: encoding 0 is AL either way.
        assert_eq!(enc(|a| a.setcc(Cond::E, Reg::Rax)), vec![0x0F, 0x94, 0xC0]);
        // setne sil -> rm encoding 6 means DH without REX and SIL with it, so a bare 40 is
        // mandatory here even though it sets no extension bit.
        assert_eq!(enc(|a| a.setcc(Cond::Ne, Reg::Rsi)), vec![0x40, 0x0F, 0x95, 0xC6]);
        // setl r10b -> REX.B = 41, ModRM = 11 000 010 = C2.
        assert_eq!(enc(|a| a.setcc(Cond::L, Reg::R10)), vec![0x41, 0x0F, 0x9C, 0xC2]);
        // movzx rax, al -> REX.W + 0F B6 /r.
        assert_eq!(enc(|a| a.movzx_rr8(Reg::Rax, Reg::Rax)), vec![0x48, 0x0F, 0xB6, 0xC0]);
    }

    #[test]
    fn shift_group_is_the_cl_form() {
        // shl rax, cl -> REX.W + D3 /4, ModRM = 11 100 000 = E0.
        assert_eq!(enc(|a| a.shl_cl(Reg::Rax)), vec![0x48, 0xD3, 0xE0]);
        // shr rax, cl -> /5, ModRM = 11 101 000 = E8. Distinct from sar: the two differ only in
        // the digit, and swapping them is invisible for non-negative values.
        assert_eq!(enc(|a| a.shr_cl(Reg::Rax)), vec![0x48, 0xD3, 0xE8]);
        // sar rax, cl -> /7, ModRM = 11 111 000 = F8.
        assert_eq!(enc(|a| a.sar_cl(Reg::Rax)), vec![0x48, 0xD3, 0xF8]);
        // sar r9, cl -> REX.W|B = 49, ModRM = 11 111 001 = F9.
        assert_eq!(enc(|a| a.sar_cl(Reg::R9)), vec![0x49, 0xD3, 0xF9]);
    }

    #[test]
    fn thirty_two_bit_moves_drop_rex_w() {
        // mov eax, ecx -> 89 /r with no REX at all (the 0x40 byte carries nothing here).
        assert_eq!(enc(|a| a.mov_rr32(Reg::Rax, Reg::Rcx)), vec![0x89, 0xC8]);
        // ...but an extended register still needs its REX bit.
        assert_eq!(enc(|a| a.mov_rr32(Reg::R8, Reg::Rcx)), vec![0x41, 0x89, 0xC8]);
        // mov eax, dword [rsp+0x20] -> 8B /r, SIB 24, disp8. Same bytes as the 64-bit load
        // minus the 48 prefix -- which is exactly the zero-extension we are after.
        assert_eq!(
            enc(|a| a.mov_rm32(Reg::Rax, Mem::at(Reg::Rsp, 0x20))),
            vec![0x8B, 0x44, 0x24, 0x20]
        );
        assert_eq!(
            enc(|a| a.mov_rm(Reg::Rax, Mem::at(Reg::Rsp, 0x20))),
            vec![0x48, 0x8B, 0x44, 0x24, 0x20]
        );
    }

    #[test]
    fn bitwise_memory_forms() {
        // and rax, [rsp+0x20] -> the r64,r/m64 opcode 23 (= 21 + 2).
        assert_eq!(
            enc(|a| a.and_rm(Reg::Rax, Mem::at(Reg::Rsp, 0x20))),
            vec![0x48, 0x23, 0x44, 0x24, 0x20]
        );
        // or rax, [rsp+0x20] -> 0B.
        assert_eq!(
            enc(|a| a.or_rm(Reg::Rax, Mem::at(Reg::Rsp, 0x20))),
            vec![0x48, 0x0B, 0x44, 0x24, 0x20]
        );
        // xor rax, [rsp+0x20] -> 33.
        assert_eq!(
            enc(|a| a.xor_rm(Reg::Rax, Mem::at(Reg::Rsp, 0x20))),
            vec![0x48, 0x33, 0x44, 0x24, 0x20]
        );
    }

    #[test]
    fn movsxd_and_misc() {
        // movsxd rax, eax -> REX.W + 63 /r, ModRM = 11 000 000 = C0.
        assert_eq!(enc(|a| a.movsxd_rr(Reg::Rax, Reg::Rax)), vec![0x48, 0x63, 0xC0]);
        // movsxd r8, dword [rsp+0x20].
        assert_eq!(
            enc(|a| a.movsxd_rm(Reg::R8, Mem::at(Reg::Rsp, 0x20))),
            vec![0x4C, 0x63, 0x44, 0x24, 0x20]
        );
        // call rax -> FF /2, no REX.W (64-bit is the default for near calls).
        assert_eq!(enc(|a| a.call_r(Reg::Rax)), vec![0xFF, 0xD0]);
        // call r8 -> REX.B only.
        assert_eq!(enc(|a| a.call_r(Reg::R8)), vec![0x41, 0xFF, 0xD0]);
        assert_eq!(enc(|a| a.ret()), vec![0xC3]);
        assert_eq!(enc(|a| a.int3()), vec![0xCC]);
        assert_eq!(enc(|a| a.nop()), vec![0x90]);
    }

    // -- Label resolution ----------------------------------------------------------------------

    #[test]
    fn forward_branch_displacement_is_from_end_of_instruction() {
        let mut a = Asm::new();
        let l = a.new_label();
        a.jmp(l); // 5 bytes: E9 + rel32 at offset 1..5
        a.nop(); // offset 5
        a.nop(); // offset 6
        a.bind(l); // target = 7
        let code = a.finish().unwrap();
        // rel = target - (site + 4) = 7 - 5 = 2.
        assert_eq!(code, vec![0xE9, 0x02, 0x00, 0x00, 0x00, 0x90, 0x90]);
    }

    #[test]
    fn backward_branch_displacement_is_negative() {
        let mut a = Asm::new();
        let top = a.new_label();
        a.bind(top); // target = 0
        a.nop(); // offset 0
        // `jcc` is a two-byte opcode (0F 85) at offsets 1..3, so the rel32 occupies 3..7 and the
        // instruction ends at 7 -- the displacement is measured from there, not from the opcode.
        a.jcc(Cond::Ne, top);
        let code = a.finish().unwrap();
        // rel = 0 - 7 = -7 = F9 FF FF FF.
        assert_eq!(code, vec![0x90, 0x0F, 0x85, 0xF9, 0xFF, 0xFF, 0xFF]);
    }

    #[test]
    fn two_branches_to_one_label_both_get_patched() {
        let mut a = Asm::new();
        let l = a.new_label();
        a.jmp(l); // 0..5, rel32 at 1
        a.jmp(l); // 5..10, rel32 at 6
        a.bind(l); // target = 10
        let code = a.finish().unwrap();
        assert_eq!(i32::from_le_bytes(code[1..5].try_into().unwrap()), 5); // 10 - 5
        assert_eq!(i32::from_le_bytes(code[6..10].try_into().unwrap()), 0); // 10 - 10
    }

    #[test]
    fn unbound_label_is_an_error_not_a_wild_jump() {
        let mut a = Asm::new();
        let l = a.new_label();
        a.jmp(l);
        assert_eq!(a.finish(), Err(AsmError::UnboundLabel(l)));
    }

    #[test]
    #[should_panic(expected = "bound twice")]
    fn binding_a_label_twice_panics() {
        let mut a = Asm::new();
        let l = a.new_label();
        a.bind(l);
        a.bind(l);
    }

    // -- Frame ---------------------------------------------------------------------------------

    #[test]
    fn frame_keeps_rsp_16_byte_aligned() {
        // Entry: RSP ≡ 8 (mod 16). Each push subtracts 8. The `sub` must land back on 0.
        for locals in 0..8u32 {
            for nsaved in 0..5usize {
                let saved = &NON_VOLATILE[..nsaved];
                let f = Frame::new(locals, saved);
                let pushes = 1 + nsaved as i64; // rbp + saved
                let rsp = 8 - 8 * pushes - f.frame_bytes() as i64;
                assert_eq!(rsp.rem_euclid(16), 0, "locals={locals} nsaved={nsaved}");
                // ...and the frame is still big enough for shadow space plus every local.
                assert!(f.frame_bytes() >= 32 + 8 * locals as i32);
            }
        }
    }

    #[test]
    fn local_slots_sit_above_the_shadow_space() {
        let f = Frame::new(3, &[]);
        assert_eq!(f.local(0), Mem::at(Reg::Rsp, 32));
        assert_eq!(f.local(1), Mem::at(Reg::Rsp, 40));
        assert_eq!(f.local(2), Mem::at(Reg::Rsp, 48));
    }

    #[test]
    #[should_panic(expected = "out of range")]
    fn local_out_of_range_panics() {
        Frame::new(2, &[]).local(2);
    }

    #[test]
    fn ms_x64_argument_registers() {
        let f = Frame::new(0, &[]);
        assert_eq!(f.arg(0), Reg::Rcx);
        assert_eq!(f.arg(1), Reg::Rdx);
        assert_eq!(f.arg(2), Reg::R8);
        assert_eq!(f.arg(3), Reg::R9);
        // Not the System V order -- if this ever reads RDI/RSI, the ABI got crossed.
        assert_ne!(f.arg(0), Reg::Rdi);
    }

    #[test]
    fn prologue_and_epilogue_are_balanced() {
        let f = Frame::new(2, &[Reg::Rbx, Reg::R12]);
        let mut a = Asm::new();
        f.prologue(&mut a);
        let pro = a.code().to_vec();
        let mut a2 = Asm::new();
        f.epilogue(&mut a2);
        let epi = a2.code().to_vec();
        // push rbp; mov rbp,rsp; push rbx; push r12; sub rsp, imm
        assert_eq!(&pro[..3], &[0x55, 0x48, 0x89]);
        assert_eq!(pro[4], 0x53); // push rbx
        assert_eq!(&pro[5..7], &[0x41, 0x54]); // push r12 (REX.B + 50+4)
        // add rsp, imm; pop r12; pop rbx; pop rbp; ret -- an exact mirror of the pushes.
        assert_eq!(epi[epi.len() - 5..], [0x41, 0x5C, 0x5B, 0x5D, 0xC3][..]);
        assert_eq!(&epi[..3], &[0x48, 0x83, 0xC4]); // add rsp, imm8
    }

    #[test]
    fn cond_inverse_is_an_involution() {
        for c in [
            Cond::O, Cond::No, Cond::B, Cond::Ae, Cond::E, Cond::Ne, Cond::Be, Cond::A, Cond::S,
            Cond::Ns, Cond::L, Cond::Ge, Cond::Le, Cond::G,
        ] {
            assert_ne!(c.inverse(), c);
            assert_eq!(c.inverse().inverse(), c);
        }
    }

    #[test]
    fn register_classification_matches_the_ms_x64_abi() {
        for r in ARG_REGS {
            assert!(r.is_volatile(), "{r:?} is an argument register and must be volatile");
        }
        for r in NON_VOLATILE {
            assert!(!r.is_volatile(), "{r:?} must be callee-saved");
        }
        assert!(Reg::R8.is_extended() && !Reg::Rdi.is_extended());
        assert_eq!(Reg::R13.low3(), Reg::Rbp.low3());
        assert_eq!(Reg::R12.low3(), Reg::Rsp.low3());
    }
}
