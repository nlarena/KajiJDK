//! End-to-end tests for `burst`: functions assembled byte by byte, made executable, **called from
//! Rust**, and compared against the same computation written in Rust.
//!
//! The encoding round-trips in [`x64`][super::x64]'s own test module prove the bytes match the
//! Intel manual. These prove the other half: that those bytes, laid into W^X memory and entered
//! through an `extern "system"` pointer, actually compute the right thing under the Microsoft x64
//! ABI. An assembler can pass either set of tests alone and still be broken — an operand-order slip
//! survives round-trip tests it does not cover, and a wrong ModRM can execute correctly by luck on
//! one input.
//!
//! Windows-only, because [`ExecMem`] is.

use super::exec_mem::ExecMem;
use super::x64::{Asm, Cond, Frame, Mem, Reg};

/// Assembles, resolves labels, and maps the result as executable memory.
///
/// The returned [`ExecMem`] owns the pages, so it must be kept alive for as long as any function
/// pointer derived from it is callable — every test below binds it to a local that outlives the
/// calls.
fn build(emit: impl FnOnce(&mut Asm)) -> ExecMem {
    let mut a = Asm::new();
    emit(&mut a);
    let code = a.finish().expect("label resolution failed");
    ExecMem::from_code(&code).expect("could not map executable memory")
}

// ---------------------------------------------------------------------------------------------
// The basics: arguments in, result out.
// ---------------------------------------------------------------------------------------------

/// `fn(x) -> x`, routed through a stack local so the whole frame is exercised: prologue, an
/// RSP-relative store, an RSP-relative load, epilogue.
#[test]
fn identity_through_a_stack_local() {
    let f = Frame::new(1, &[]);
    let mem = build(|a| {
        f.prologue(a);
        a.mov_mr(f.local(0), f.arg(0));
        a.mov_rm(Reg::Rax, f.local(0));
        f.epilogue(a);
    });
    // SAFETY: the emitted code is a balanced Microsoft x64 frame (`Frame` guarantees the pushes,
    // the 16-byte-aligned `sub`, and the mirrored epilogue), reads one integer argument from RCX,
    // returns it in RAX, touches only its own frame, and saves no non-volatile register because it
    // writes none. That is exactly `extern "system" fn(i64) -> i64`.
    let ident = unsafe { mem.as_fn1() };
    for x in [0i64, 1, -1, 42, i64::MIN, i64::MAX] {
        assert_eq!(ident(x), x);
    }
}

/// `fn(a, b) -> a + b`, i.e. RCX + RDX. If this module had used the System V order, `a` would be
/// read from RDI and the result would be garbage.
#[test]
fn two_arguments_are_read_from_rcx_and_rdx() {
    let f = Frame::new(0, &[]);
    let mem = build(|a| {
        f.prologue(a);
        a.mov_rr(Reg::Rax, f.arg(0));
        a.add_rr(Reg::Rax, f.arg(1));
        f.epilogue(a);
    });
    // SAFETY: balanced frame, two integer arguments in RCX/RDX, result in RAX, only volatile
    // registers written, no memory accessed beyond its own frame.
    let add = unsafe { mem.as_fn2() };
    for (x, y) in [(0i64, 0i64), (1, 2), (-5, 3), (i32::MAX as i64, i32::MAX as i64)] {
        assert_eq!(add(x, y), x + y);
    }
}

/// All four register arguments, spilled to locals and summed back. RCX, RDX, R8 and R9 — the last
/// two need `REX.R` on every move, so a dropped REX here would sum the wrong registers.
#[test]
fn four_arguments_spilled_to_locals_and_summed() {
    let f = Frame::new(4, &[]);
    let mem = build(|a| {
        f.prologue(a);
        for i in 0..4u32 {
            a.mov_mr(f.local(i), f.arg(i as usize));
        }
        a.xor_rr(Reg::Rax, Reg::Rax);
        for i in 0..4u32 {
            a.add_rm(Reg::Rax, f.local(i));
        }
        f.epilogue(a);
    });
    // SAFETY: balanced frame, four integer arguments in RCX/RDX/R8/R9 spilled into slots the
    // prologue reserved, result in RAX, no non-volatile register written.
    let sum4 = unsafe { mem.as_fn4() };
    assert_eq!(sum4(1, 2, 3, 4), 10);
    assert_eq!(sum4(-1, -2, -3, -4), -10);
    assert_eq!(sum4(1_000_000, 20, 300, 4), 1_000_324);
}

/// A constant returned via the 10-byte `movabs` form — the value does not fit in the sign-extended
/// `imm32` encoding, so this checks the immediate actually made it into the instruction stream.
#[test]
fn large_immediate_uses_movabs() {
    const K: i64 = 0x0123_4567_89AB_CDEF;
    let f = Frame::new(0, &[]);
    let mem = build(|a| {
        f.prologue(a);
        a.mov_ri(Reg::Rax, K);
        f.epilogue(a);
    });
    // SAFETY: balanced frame, no arguments read, constant returned in RAX, nothing else touched.
    let k = unsafe { mem.as_fn0() };
    assert_eq!(k(), K);
}

// ---------------------------------------------------------------------------------------------
// Arithmetic.
// ---------------------------------------------------------------------------------------------

/// `fn(x) -> ((x + 10) * 3 - 4) / 2`, mixing immediates, the three-operand `imul`, and `idiv`.
///
/// `idiv` is the fiddly one: the dividend is the implicit 128-bit `RDX:RAX`, so `cqo` has to
/// sign-extend RAX into RDX first, and the divisor has to live somewhere other than RDX.
#[test]
fn arithmetic_with_constants_including_idiv() {
    let f = Frame::new(0, &[]);
    let mem = build(|a| {
        f.prologue(a);
        a.mov_rr(Reg::Rax, f.arg(0));
        a.add_ri(Reg::Rax, 10);
        a.imul_rri(Reg::Rax, Reg::Rax, 3);
        a.sub_ri(Reg::Rax, 4);
        a.mov_ri(Reg::R8, 2); // divisor parked in a volatile register that `cqo` will not clobber
        a.cqo();
        a.idiv(Reg::R8);
        f.epilogue(a);
    });
    // SAFETY: balanced frame, one argument in RCX, result in RAX; RAX/RDX/R8 are volatile and the
    // divisor is a non-zero constant, so `idiv` cannot fault.
    let g = unsafe { mem.as_fn1() };
    for x in [0i64, 1, 7, -1, -13, 100, -100, 1_000_000] {
        // Rust's `/` truncates toward zero, which is exactly `idiv`'s rounding (and the JLS's).
        assert_eq!(g(x), ((x + 10) * 3 - 4) / 2, "x = {x}");
    }
}

/// Division and remainder, with the divisor arriving in RDX — which `cqo` is about to overwrite.
/// Moving it to R8 first is mandatory; forgetting to would divide by the sign extension.
#[test]
fn idiv_produces_quotient_in_rax_and_remainder_in_rdx() {
    let f = Frame::new(0, &[]);
    let build_div = |want_remainder: bool| {
        build(|a| {
            f.prologue(a);
            a.mov_rr(Reg::R8, f.arg(1)); // rescue the divisor before `cqo` clobbers RDX
            a.mov_rr(Reg::Rax, f.arg(0));
            a.cqo();
            a.idiv(Reg::R8);
            if want_remainder {
                a.mov_rr(Reg::Rax, Reg::Rdx);
            }
            f.epilogue(a);
        })
    };
    let qmem = build_div(false);
    let rmem = build_div(true);
    // SAFETY: balanced frames, two integer arguments in RCX/RDX, result in RAX, only volatile
    // registers written. Every divisor used below is non-zero, so `idiv` cannot raise #DE.
    let quot = unsafe { qmem.as_fn2() };
    // SAFETY: as above.
    let rem = unsafe { rmem.as_fn2() };
    for (n, d) in [(7i64, 2i64), (-7, 2), (7, -2), (-7, -2), (100, 10), (1, 3), (0, 5)] {
        assert_eq!(quot(n, d), n / d, "{n} / {d}");
        assert_eq!(rem(n, d), n % d, "{n} % {d}");
    }
    // The case that motivates staying in 64-bit registers: 32-bit `idiv` faults on INT_MIN / -1
    // because the quotient does not fit in 32 bits. In 64 bits it is just an ordinary division.
    assert_eq!(quot(i32::MIN as i64, -1), -(i32::MIN as i64));
}

/// `neg` and the bitwise group, checked against Rust's operators.
#[test]
fn negate_and_bitwise_ops() {
    let f = Frame::new(0, &[]);
    let neg = build(|a| {
        f.prologue(a);
        a.mov_rr(Reg::Rax, f.arg(0));
        a.neg(Reg::Rax);
        f.epilogue(a);
    });
    let bits = build(|a| {
        f.prologue(a);
        a.mov_rr(Reg::Rax, f.arg(0));
        a.and_rr(Reg::Rax, f.arg(1));
        a.or_ri(Reg::Rax, 1);
        a.xor_ri(Reg::Rax, 0xF0);
        f.epilogue(a);
    });
    // SAFETY: balanced frames, arguments in RCX/RDX, result in RAX, only volatile registers used.
    let n = unsafe { neg.as_fn1() };
    // SAFETY: as above.
    let b = unsafe { bits.as_fn2() };
    for x in [0i64, 1, -1, 12345, i32::MIN as i64] {
        assert_eq!(n(x), -x);
    }
    for (x, y) in [(0xFFi64, 0x0Fi64), (0, 0), (-1, 0x1234), (0xAAAA, 0x5555)] {
        assert_eq!(b(x, y), ((x & y) | 1) ^ 0xF0);
    }
}

// ---------------------------------------------------------------------------------------------
// Branches.
// ---------------------------------------------------------------------------------------------

/// `fn(a, b) -> max(a, b)` — one forward conditional branch and one forward unconditional branch,
/// both patched by `finish()`. Uses the **signed** condition, as Java's `if_icmp*` require.
#[test]
fn conditional_branch_computes_max() {
    let f = Frame::new(0, &[]);
    let mem = build(|a| {
        let b_bigger = a.new_label();
        let done = a.new_label();
        f.prologue(a);
        a.cmp_rr(f.arg(0), f.arg(1));
        a.jcc(Cond::L, b_bigger); // a < b (signed)
        a.mov_rr(Reg::Rax, f.arg(0));
        a.jmp(done);
        a.bind(b_bigger);
        a.mov_rr(Reg::Rax, f.arg(1));
        a.bind(done);
        f.epilogue(a);
    });
    // SAFETY: balanced frame, two integer arguments in RCX/RDX, result in RAX, only volatile
    // registers written; every branch target is inside this function's own code.
    let max = unsafe { mem.as_fn2() };
    for (x, y) in [(1i64, 2i64), (2, 1), (0, 0), (-5, 3), (3, -5), (-5, -5), (i64::MIN, i64::MAX)] {
        assert_eq!(max(x, y), x.max(y), "max({x}, {y})");
    }
}

/// `setcc` writes a boolean into a register's low byte. `RSI` is deliberately chosen: as an 8-bit
/// operand its encoding means `DH` unless a REX prefix is present, so this only returns the right
/// answer if the bare `0x40` prefix was emitted. It is also non-volatile, so the frame must save it.
#[test]
fn setcc_into_sil_needs_the_bare_rex_prefix() {
    let f = Frame::new(0, &[Reg::Rsi]);
    let mem = build(|a| {
        f.prologue(a);
        a.cmp_rr(f.arg(0), f.arg(1));
        a.setcc(Cond::L, Reg::Rsi); // setl sil
        a.movzx_rr8(Reg::Rax, Reg::Rsi);
        f.epilogue(a);
    });
    // SAFETY: balanced frame, two integer arguments in RCX/RDX, result in RAX. RSI is the only
    // non-volatile register written and the frame's `saved` list restores it.
    let lt = unsafe { mem.as_fn2() };
    for (x, y) in [(1i64, 2i64), (2, 1), (0, 0), (-1, 0), (0, -1), (i64::MIN, i64::MAX)] {
        assert_eq!(lt(x, y), i64::from(x < y), "{x} < {y}");
    }
}

// ---------------------------------------------------------------------------------------------
// Loops: a backward branch plus a forward one.
// ---------------------------------------------------------------------------------------------

/// `fn(n) -> 1 + 2 + ... + n`, with the accumulator and induction variable in stack locals.
///
/// Exercises both branch directions at once: the exit test is a forward reference (patched after
/// the body is emitted) and the loop back-edge is a backward one (the label is already bound).
#[test]
fn loop_sums_one_to_n() {
    let f = Frame::new(2, &[]); // local 0 = i, local 1 = acc
    let mem = build(|a| {
        let top = a.new_label();
        let done = a.new_label();
        f.prologue(a);
        a.xor_rr(Reg::Rax, Reg::Rax);
        a.mov_mr(f.local(1), Reg::Rax); // acc = 0
        a.mov_ri(Reg::Rax, 1);
        a.mov_mr(f.local(0), Reg::Rax); // i = 1

        a.bind(top);
        a.mov_rm(Reg::Rax, f.local(0));
        a.cmp_rr(Reg::Rax, f.arg(0));
        a.jcc(Cond::G, done); // forward: exit when i > n
        a.mov_rm(Reg::Rdx, f.local(1));
        a.add_rr(Reg::Rdx, Reg::Rax);
        a.mov_mr(f.local(1), Reg::Rdx); // acc += i
        a.add_ri(Reg::Rax, 1);
        a.mov_mr(f.local(0), Reg::Rax); // i += 1
        a.jmp(top); // backward

        a.bind(done);
        a.mov_rm(Reg::Rax, f.local(1));
        f.epilogue(a);
    });
    // SAFETY: balanced frame, one integer argument in RCX (never written), result in RAX. Only
    // RAX/RDX (volatile) and the two frame slots the prologue reserved are written; both branch
    // targets are inside this function.
    let sum = unsafe { mem.as_fn1() };
    for n in [0i64, 1, 2, 3, 10, 100, 1000, 12345] {
        assert_eq!(sum(n), n * (n + 1) / 2, "sum(1..={n})");
    }
    // n <= 0 must fall straight through the exit test, not wrap around.
    assert_eq!(sum(-1), 0);
    assert_eq!(sum(i64::MIN), 0);
}

/// Iterative factorial, accumulating in `RBX` — a non-volatile register, so the frame has to save
/// and restore it. If the save were missing, the corruption would land in the *Rust* caller.
#[test]
fn loop_computes_factorial_in_a_callee_saved_register() {
    let f = Frame::new(0, &[Reg::Rbx]);
    let mem = build(|a| {
        let top = a.new_label();
        let done = a.new_label();
        f.prologue(a);
        a.mov_ri(Reg::Rbx, 1); // acc
        a.mov_ri(Reg::Rdx, 1); // i

        a.bind(top);
        a.cmp_rr(Reg::Rdx, f.arg(0));
        a.jcc(Cond::G, done);
        a.imul_rr(Reg::Rbx, Reg::Rdx);
        a.add_ri(Reg::Rdx, 1);
        a.jmp(top);

        a.bind(done);
        a.mov_rr(Reg::Rax, Reg::Rbx);
        f.epilogue(a);
    });
    // SAFETY: balanced frame, one integer argument in RCX, result in RAX. RBX is the only
    // non-volatile register written and the frame's `saved` list restores it before `ret`.
    let fact = unsafe { mem.as_fn1() };
    let mut expected = 1i64;
    for n in 0..=20i64 {
        if n > 0 {
            expected *= n;
        }
        assert_eq!(fact(n), expected, "{n}!");
    }
    assert_eq!(fact(0), 1);
    assert_eq!(fact(-3), 1); // empty loop
}

// ---------------------------------------------------------------------------------------------
// REX: the extended registers, where encodings break.
// ---------------------------------------------------------------------------------------------

/// Routes all four arguments through `R12`–`R15`. Every one of those moves needs `REX.R`/`REX.B`,
/// and all four are callee-saved, so the frame must push and pop them in mirrored order.
#[test]
fn r8_through_r15_round_trip_values() {
    let f = Frame::new(0, &[Reg::R12, Reg::R13, Reg::R14, Reg::R15]);
    let mem = build(|a| {
        f.prologue(a);
        a.mov_rr(Reg::R12, f.arg(0));
        a.mov_rr(Reg::R13, f.arg(1));
        a.mov_rr(Reg::R14, f.arg(2)); // arg 2 is R8: extended -> extended
        a.mov_rr(Reg::R15, f.arg(3)); // arg 3 is R9
        a.imul_rr(Reg::R12, Reg::R13); // extended x extended
        a.add_rr(Reg::R12, Reg::R14);
        a.sub_rr(Reg::R12, Reg::R15);
        a.mov_rr(Reg::Rax, Reg::R12);
        f.epilogue(a);
    });
    // SAFETY: balanced frame, four integer arguments in RCX/RDX/R8/R9, result in RAX. R12-R15 are
    // the only non-volatile registers written and all four are in the frame's `saved` list.
    let g = unsafe { mem.as_fn4() };
    for (a0, b, c, d) in [(1i64, 2i64, 3i64, 4i64), (-1, -2, -3, -4), (100, 0, 7, 7), (5, 5, 5, 5)] {
        assert_eq!(g(a0, b, c, d), a0 * b + c - d, "{a0}*{b}+{c}-{d}");
    }
}

/// Memory operands based on `R12` and `R13` — the two bases with special ModRM handling:
/// `R12`'s low 3 bits are `100`, which forces a SIB byte; `R13`'s are `101`, which has no
/// zero-displacement form and needs an explicit `disp8` of 0. Both aliased to RSP here so the
/// loads must reproduce exactly what the RSP-relative stores wrote.
#[test]
fn extended_base_registers_address_memory_correctly() {
    let f = Frame::new(2, &[Reg::R12, Reg::R13]);
    let slot0 = f.local(0).disp;
    let slot1 = f.local(1).disp;
    let mem = build(|a| {
        f.prologue(a);
        a.mov_mr(f.local(0), f.arg(0)); // [rsp+32] = a
        a.mov_mr(f.local(1), f.arg(1)); // [rsp+40] = b
        a.mov_rr(Reg::R12, Reg::Rsp);
        a.mov_rr(Reg::R13, Reg::Rsp);
        a.mov_rm(Reg::Rax, Mem::at(Reg::R13, slot0)); // R13 base + disp8 (no SIB)
        a.add_rm(Reg::Rax, Mem::at(Reg::R12, slot1)); // R12 base + SIB + disp8
        a.add_ri(Reg::R13, slot1);
        a.add_rm(Reg::Rax, Mem::at(Reg::R13, 0)); // R13 base, disp 0 -> explicit zero disp8
        f.epilogue(a);
    });
    // SAFETY: balanced frame, two integer arguments in RCX/RDX, result in RAX. R12/R13 are the
    // only non-volatile registers written and both are saved by the frame. Every memory access is
    // to a slot the prologue reserved: R12/R13 are copies of RSP, which the body never moves.
    let g = unsafe { mem.as_fn2() };
    for (x, y) in [(1i64, 2i64), (-3, 9), (0, 0), (1 << 40, 1)] {
        assert_eq!(g(x, y), x + y + y, "{x} + 2*{y}");
    }
}

// ---------------------------------------------------------------------------------------------
// The full ABI contract: one generated function calling another.
// ---------------------------------------------------------------------------------------------

/// A generated function calls a second generated function through `call rXX`.
///
/// This is the test that exercises the Microsoft x64 rules that only bite at a call boundary:
///
/// - **shadow space** — the callee's prologue is free to spill into the 32 bytes above the return
///   address, so the caller must have reserved them. [`Frame`] always does.
/// - **16-byte alignment at the `call`** — the caller's frame arithmetic must leave `RSP % 16 == 0`
///   right before the `call` pushes the return address.
/// - **non-volatile preservation** — the caller parks a magic value in `RBX`, the callee overwrites
///   `RBX` and (via its own frame) restores it. The result is only correct if it really did.
#[test]
fn generated_code_calls_generated_code_under_the_ms_x64_abi() {
    const MAGIC: i64 = 0x1234_5678;

    // Callee: returns arg + 1, and clobbers RBX along the way (declaring it, so it is restored).
    let callee_frame = Frame::new(1, &[Reg::Rbx]);
    let callee = build(|a| {
        callee_frame.prologue(a);
        a.mov_ri(Reg::Rbx, -1); // trash the caller's RBX...
        a.mov_mr(callee_frame.local(0), Reg::Rbx); // ...and use its own frame slot
        a.mov_rr(Reg::Rax, callee_frame.arg(0));
        a.add_ri(Reg::Rax, 1);
        callee_frame.epilogue(a); // ...restored here
    });

    // Caller: fn(x, callee_ptr) -> callee(x) + MAGIC, with MAGIC held in RBX across the call.
    let caller_frame = Frame::new(0, &[Reg::Rbx, Reg::R14]);
    let caller = build(|a| {
        caller_frame.prologue(a);
        a.mov_ri(Reg::Rbx, MAGIC);
        a.mov_rr(Reg::R14, caller_frame.arg(1)); // callee pointer: R14 survives the call
        // arg 0 is already in RCX, which is exactly where the callee expects it.
        a.call_r(Reg::R14);
        a.add_rr(Reg::Rax, Reg::Rbx); // only correct if the callee restored RBX
        caller_frame.epilogue(a);
    });

    // SAFETY: balanced frames on both sides. The caller reserves 32 bytes of shadow space and
    // keeps RSP 16-byte aligned at the `call` (both guaranteed by `Frame`), and the callee is a
    // real function with the matching one-integer-argument signature, kept alive by `callee` for
    // the whole call. RBX and R14 are the only non-volatile registers written and both are saved.
    let f = unsafe { caller.as_fn2() };
    let callee_addr = callee.as_ptr() as i64;
    for x in [0i64, 1, -1, 1_000_000] {
        assert_eq!(f(x, callee_addr), x + 1 + MAGIC, "callee({x}) + MAGIC");
    }
}

/// Calling the same block repeatedly, and building many blocks, must be stable — a stack that
/// drifts by 8 bytes per call, or pages released too early, would show up here rather than as a
/// mystery crash later.
#[test]
fn repeated_calls_leave_the_stack_balanced() {
    let f = Frame::new(3, &[Reg::Rbx, Reg::Rsi, Reg::R15]);
    let mem = build(|a| {
        f.prologue(a);
        a.mov_mr(f.local(0), f.arg(0));
        a.mov_ri(Reg::Rbx, 7);
        a.mov_ri(Reg::Rsi, 11);
        a.mov_ri(Reg::R15, 13);
        a.mov_rm(Reg::Rax, f.local(0));
        a.add_rr(Reg::Rax, Reg::Rbx);
        a.add_rr(Reg::Rax, Reg::Rsi);
        a.add_rr(Reg::Rax, Reg::R15);
        f.epilogue(a);
    });
    // SAFETY: balanced frame, one integer argument in RCX, result in RAX; RBX/RSI/R15 are the only
    // non-volatile registers written and all three are in the frame's `saved` list. The only memory
    // touched is a slot the prologue reserved.
    let g = unsafe { mem.as_fn1() };
    let mut acc = 0i64;
    for i in 0..10_000i64 {
        acc = acc.wrapping_add(g(i));
    }
    let expected: i64 = (0..10_000i64).map(|i| i + 31).sum();
    assert_eq!(acc, expected);
}
