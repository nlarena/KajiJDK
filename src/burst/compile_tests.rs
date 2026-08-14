//! End-to-end tests of [`compile`][super::compile]: **bytecode in, machine code out, mapped W^X and
//! actually executed**, with the answers checked against what the JLS says a JVM must produce.
//!
//! Encoding tests (`x64`) prove the bytes are the instructions we meant. These prove the
//! *instructions we meant* implement Java. The distinction matters most for the three traps in the
//! module docs of [`compile`][super::compile] — every one of them is a case where a plausible
//! 64-bit translation runs perfectly and returns a wrong number, so nothing short of executing it
//! and comparing against the JLS value would notice.

use std::sync::atomic::{AtomicU64, Ordering};

use super::compile::{
    compile as compile_with_poll, CompiledCode, Ineligible, Outcome, Status, MAX_SWITCH_CASES,
};
use super::exec_mem::ExecMem;

/// The safepoint poll word these tests compile against. **Never written**: every test here asks
/// "does this bytecode compute what the JLS says", and a poll firing mid-loop would answer a
/// different question. The one test that exercises the poll compiles against a word of its own,
/// so nothing in this file can perturb anything else in it — `cargo test` runs these in parallel
/// threads of one process.
static POLL: AtomicU64 = AtomicU64::new(0);

/// [`super::compile::compile`] against [`POLL`], so the programs below read as bytecode.
fn compile(
    code: &[u8],
    max_locals: usize,
    int_const: &dyn Fn(u16) -> Option<i32>,
) -> Result<CompiledCode, Ineligible> {
    compile_with_poll(code, max_locals, int_const, &|_| None, &POLL as *const _ as usize)
}

/// Compiles `code`, maps it, calls it with `locals`, and reports `Some(result)` / `None` for a
/// deopt. `constants` resolves an `ldc` index (tests that use none pass an empty slice).
fn run_with(code: &[u8], locals: &[i32], constants: &[(u16, i32)]) -> Option<i32> {
    let compiled = compile(code, locals.len(), &|index| {
        constants.iter().find(|(i, _)| *i == index).map(|(_, v)| *v)
    })
    .expect("the test programs are all inside the compiled subset");
    call(&compiled, locals)
}

/// The invocation half of [`run_with`], split out so a test can compile once and call many times.
fn call(compiled: &CompiledCode, locals: &[i32]) -> Option<i32> {
    match call_at(compiled, locals, 0).0 {
        Outcome::Returned(v) => Some(v),
        _ => None,
    }
}

/// [`call`] entering at `entry_pc` and reporting the full [`Outcome`] alongside the locals buffer
/// as native code left it — which is what an on-stack exit hands the interpreter.
fn call_at(compiled: &CompiledCode, locals: &[i32], entry_pc: i64) -> (Outcome, Vec<i64>) {
    let mem = ExecMem::from_code(&compiled.code).expect("map the code W^X");
    // `Value::Int(v) as i64` is the interpreter's marshalling, and it sign-extends -- which is
    // exactly the normalisation invariant the generated code relies on for its inputs. The extra
    // trailing slot keeps `as_mut_ptr` non-dangling for a zero-local program.
    let mut buffer: Vec<i64> = locals.iter().map(|&v| v as i64).collect();
    buffer.push(0);
    // SAFETY: `compile` emits exactly one `extern "system" fn(*mut i64, i64) -> i64` at offset 0,
    // built from `x64::Frame`, restoring every non-volatile register and ending in `ret`. The
    // buffer is a live, initialised `[i64]` at least `locals.len()` long, which is the marshalling
    // contract (`touched_locals` indices are all `< max_locals == locals.len()`), and `entry_pc`
    // is 0 or one of the code's own `osr_entries` at every call site below.
    let f: extern "system" fn(*mut i64, i64) -> i64 = unsafe { mem.as_fn() };
    let raw = f(buffer.as_mut_ptr(), entry_pc);
    (Status::unpack(raw), buffer)
}

// Bytecode shorthands, so the programs below read as bytecode rather than as hex.
const ICONST_0: u8 = 0x03;
const ICONST_1: u8 = 0x04;
const BIPUSH: u8 = 0x10;
const SIPUSH: u8 = 0x11;
const LDC: u8 = 0x12;
const ILOAD_0: u8 = 0x1a;
const ILOAD_1: u8 = 0x1b;
const ILOAD_2: u8 = 0x1c;
const ISTORE_0: u8 = 0x3b;
const ISTORE_1: u8 = 0x3c;
const ISTORE_2: u8 = 0x3d;
const POP: u8 = 0x57;
const DUP: u8 = 0x59;
const SWAP: u8 = 0x5f;
const IADD: u8 = 0x60;
const ISUB: u8 = 0x64;
const IMUL: u8 = 0x68;
const IDIV: u8 = 0x6c;
const IREM: u8 = 0x70;
const INEG: u8 = 0x74;
const ISHL: u8 = 0x78;
const ISHR: u8 = 0x7a;
const IUSHR: u8 = 0x7c;
const IAND: u8 = 0x7e;
const IOR: u8 = 0x80;
const IXOR: u8 = 0x82;
const IINC: u8 = 0x84;
const IF_ICMPGE: u8 = 0xa2;
const GOTO: u8 = 0xa7;
const IRETURN: u8 = 0xac;

/// `iload_0; iload_1; <op>; ireturn` — the shape almost every test below wants.
fn binop(op: u8, a: i32, b: i32) -> Option<i32> {
    run_with(&[ILOAD_0, ILOAD_1, op, IRETURN], &[a, b], &[])
}

// ---------------------------------------------------------------------------------------------
// The basics, first: if these are wrong nothing below means anything.
// ---------------------------------------------------------------------------------------------

#[test]
fn constants_and_locals_round_trip() {
    assert_eq!(run_with(&[ICONST_0, IRETURN], &[], &[]), Some(0));
    assert_eq!(run_with(&[0x02, IRETURN], &[], &[]), Some(-1)); // iconst_m1
    assert_eq!(run_with(&[BIPUSH, 0x80, IRETURN], &[], &[]), Some(-128));
    assert_eq!(run_with(&[SIPUSH, 0x80, 0x00, IRETURN], &[], &[]), Some(-32768));
    assert_eq!(run_with(&[SIPUSH, 0x7f, 0xff, IRETURN], &[], &[]), Some(32767));
    // ldc of an Integer, resolved at compile time and baked in as an immediate.
    assert_eq!(run_with(&[LDC, 7, IRETURN], &[], &[(7, 900_000)]), Some(900_000));
    assert_eq!(run_with(&[LDC, 7, IRETURN], &[], &[(7, i32::MIN)]), Some(i32::MIN));
    // iload/istore of a negative value: the marshalling and the slots must both sign-extend, or
    // the value comes back with garbage in its top half.
    assert_eq!(run_with(&[ILOAD_2, ISTORE_0, ILOAD_0, IRETURN], &[0, 0, -7], &[]), Some(-7));
}

#[test]
fn arithmetic_and_bitwise_agree_with_java() {
    assert_eq!(binop(IADD, 20, 22), Some(42));
    assert_eq!(binop(ISUB, 20, 22), Some(-2));
    assert_eq!(binop(IMUL, -6, 7), Some(-42));
    assert_eq!(binop(IDIV, -7, 2), Some(-3)); // Java truncates toward zero, unlike a floor divide
    assert_eq!(binop(IREM, -7, 2), Some(-1)); // ...so the remainder takes the dividend's sign
    assert_eq!(binop(IREM, 7, -2), Some(1));
    assert_eq!(binop(IAND, 0x0F0F, 0x00FF), Some(0x000F));
    assert_eq!(binop(IOR, 0x0F0F, 0x00FF), Some(0x0FFF));
    assert_eq!(binop(IXOR, 0x0F0F, 0x00FF), Some(0x0FF0));
    assert_eq!(run_with(&[ILOAD_0, INEG, IRETURN], &[42], &[]), Some(-42));
}

#[test]
fn bitwise_operations_preserve_the_normalisation_invariant() {
    // The claim in the module docs: a bitwise op on two sign-extended operands is sign-extended,
    // so no `movsxd` is emitted after one. If that were false the result would come back with a
    // corrupt top half, and the very next `ireturn` (a 32-bit load) would hide it -- so the test
    // feeds the result into a *comparison*, which reads all 64 bits.
    //
    //  0: iload_0  1: iload_1  2: iand  3: iconst_0  4: if_icmpge 9   7: iconst_1  8: ireturn
    //  9: iconst_0 10: ireturn      -- i.e. 1 when the result is negative, 0 when it is not.
    let code = [ILOAD_0, ILOAD_1, IAND, ICONST_0, IF_ICMPGE, 0x00, 0x05, ICONST_1, IRETURN, ICONST_0, IRETURN];
    assert_eq!(run_with(&code, &[-1, -1], &[]), Some(1), "-1 & -1 must compare as negative");
    assert_eq!(run_with(&code, &[-1, 0x7FFF_FFFF], &[]), Some(0));
}

#[test]
fn comparisons_are_signed() {
    // if_icmpge with a negative operand: an unsigned condition code would rank -1 above 1.
    //  0: iload_0  1: iload_1  2: if_icmpge 7   5: iconst_1  6: ireturn   7: iconst_0  8: ireturn
    let code = [ILOAD_0, ILOAD_1, IF_ICMPGE, 0x00, 0x05, ICONST_1, IRETURN, ICONST_0, IRETURN];
    assert_eq!(run_with(&code, &[-1, 1], &[]), Some(1), "-1 < 1");
    assert_eq!(run_with(&code, &[1, -1], &[]), Some(0), "1 >= -1");
    assert_eq!(run_with(&code, &[i32::MIN, i32::MAX], &[]), Some(1));
    assert_eq!(run_with(&code, &[5, 5], &[]), Some(0));
}

#[test]
fn stack_shuffles() {
    // dup: iload_0; dup; iadd; ireturn  == 2a
    assert_eq!(run_with(&[ILOAD_0, DUP, IADD, IRETURN], &[21], &[]), Some(42));
    // pop: iload_0; iload_1; pop; ireturn == a  (the popped value must not be the one returned)
    assert_eq!(run_with(&[ILOAD_0, ILOAD_1, POP, IRETURN], &[42, 99], &[]), Some(42));
    // swap: iload_0; iload_1; swap; isub; ireturn == b - a
    assert_eq!(run_with(&[ILOAD_0, ILOAD_1, SWAP, ISUB, IRETURN], &[10, 3], &[]), Some(-7));
}

#[test]
fn a_loop_runs_its_back_edge() {
    // int acc = 0; for (int i = local0; i < local1; i++) acc += i; return acc;
    //  0: iconst_0        acc = 0
    //  1: istore_2
    //  2: iload_0         i (already in local 0)
    //  3: iload_1
    //  4: if_icmpge 16
    //  7: iload_2
    //  8: iload_0
    //  9: iadd
    // 10: istore_2
    // 11: iinc 0, 1
    // 14: goto 2       (offset -12, a real backward fixup)
    // 17: iload_2
    // 18: ireturn
    let code = [
        ICONST_0, ISTORE_2, ILOAD_0, ILOAD_1, IF_ICMPGE, 0x00, 0x0d, ILOAD_2, ILOAD_0, IADD,
        ISTORE_2, IINC, 0x00, 0x01, GOTO, 0xff, 0xf4, ILOAD_2, IRETURN,
    ];
    assert_eq!(run_with(&code, &[0, 10, 0], &[]), Some(45)); // 0+1+...+9
    assert_eq!(run_with(&code, &[0, 0, 0], &[]), Some(0)); // the loop never runs
    assert_eq!(run_with(&code, &[-3, 3, 0], &[]), Some(-3)); // -3-2-1+0+1+2
}

// ---------------------------------------------------------------------------------------------
// TRAP 1: the normalisation invariant. Every case here overflows 32 bits, and every case would
// come back with the un-wrapped 64-bit answer if the `movsxd` were missing.
// ---------------------------------------------------------------------------------------------

#[test]
fn integer_arithmetic_wraps_at_32_bits() {
    assert_eq!(binop(IADD, i32::MAX, 1), Some(i32::MIN), "MAX + 1 == MIN (JLS 15.18.2)");
    assert_eq!(binop(ISUB, i32::MIN, 1), Some(i32::MAX), "MIN - 1 == MAX");
    assert_eq!(binop(IMUL, 65536, 65536), Some(0), "2^16 * 2^16 == 0");
    assert_eq!(binop(IMUL, i32::MAX, 2), Some(-2));
    assert_eq!(binop(IMUL, i32::MIN, -1), Some(i32::MIN));
    // -MIN_VALUE is MIN_VALUE (JLS 15.15.4); in 64 bits `neg` would answer 2^31.
    assert_eq!(run_with(&[ILOAD_0, INEG, IRETURN], &[i32::MIN], &[]), Some(i32::MIN));
    // iinc overflows too -- it is the one local write that computes.
    assert_eq!(run_with(&[IINC, 0x00, 0x01, ILOAD_0, IRETURN], &[i32::MAX], &[]), Some(i32::MIN));
    assert_eq!(run_with(&[IINC, 0x00, 0x80, ILOAD_0, IRETURN], &[i32::MIN], &[]), Some(i32::MAX - 127));
}

#[test]
fn an_overflowed_value_is_still_normalised_for_the_next_operation() {
    // The invariant is not only about what `ireturn` sees. `MAX + 1` overflows to MIN, and the
    // *comparison* that follows must see a negative number -- if the 64-bit 2^31 survived, it
    // would compare as positive and this returns the wrong branch.
    // iload_0; iconst_1; iadd; iconst_0; if_icmpge +7 -> 0 (non-negative) else 1 (negative)
    let code = [ILOAD_0, ICONST_1, IADD, ICONST_0, IF_ICMPGE, 0x00, 0x05, ICONST_1, IRETURN, ICONST_0, IRETURN];
    assert_eq!(run_with(&code, &[i32::MAX], &[]), Some(1), "MAX + 1 must be negative afterwards");
    assert_eq!(run_with(&code, &[5], &[]), Some(0));
    // The same for a multiply feeding a divide: 2^16 * 2^16 is 0, so this must be a 0/x, not
    // 2^32/x.
    assert_eq!(run_with(&[ILOAD_0, ILOAD_0, IMUL, ILOAD_1, IDIV, IRETURN], &[65536, 3], &[]), Some(0));
}

// ---------------------------------------------------------------------------------------------
// TRAP 2: shift counts are masked to 5 bits, and `>>>` is logical over 32 bits.
// ---------------------------------------------------------------------------------------------

#[test]
fn shift_counts_are_masked_to_five_bits() {
    // JLS 15.19: only the low 5 bits of the count are used for an int shift. x86 masks a *64-bit*
    // shift to 6 bits, so without the explicit `and cl, 31` these would shift by 33 and answer 0.
    assert_eq!(binop(ISHL, 1, 33), Some(2), "1 << 33 == 1 << 1");
    assert_eq!(binop(ISHL, 1, 32), Some(1), "1 << 32 == 1 << 0");
    assert_eq!(binop(ISHL, 1, -1), Some(i32::MIN), "a negative count masks to 31");
    assert_eq!(binop(ISHR, -1, 33), Some(-1));
    assert_eq!(binop(ISHR, 1024, 33), Some(512));
    assert_eq!(binop(IUSHR, -1, 33), Some(i32::MAX));
    assert_eq!(binop(IUSHR, 256, 32 + 4), Some(16));
}

#[test]
fn unsigned_right_shift_is_logical_over_32_bits() {
    // The headline case. The operand arrives sign-extended (0xFFFF_FFFF_FFFF_FFFF); a 64-bit
    // logical shift of *that* leaves the low 32 bits at 0xFFFF_FFFF, i.e. -1 -- so `iushr` must
    // zero-extend the low half first.
    assert_eq!(binop(IUSHR, -1, 1), Some(i32::MAX), "-1 >>> 1 == Integer.MAX_VALUE");
    assert_eq!(binop(IUSHR, -1, 31), Some(1));
    assert_eq!(binop(IUSHR, -1, 0), Some(-1), "a zero count must not change the value");
    assert_eq!(binop(IUSHR, i32::MIN, 1), Some(0x4000_0000));
    assert_eq!(binop(IUSHR, -16, 4), Some(0x0FFF_FFFF));
    // ...and `>>` keeps the sign, which is the whole difference between the two.
    assert_eq!(binop(ISHR, -1, 1), Some(-1));
    assert_eq!(binop(ISHR, -16, 4), Some(-1));
    assert_eq!(binop(ISHR, i32::MIN, 31), Some(-1));
}

#[test]
fn a_left_shift_that_overflows_still_normalises() {
    // Bits shifted past bit 31 must be gone, and the result must still be a sign-extended int:
    // 1 << 31 is Integer.MIN_VALUE, and the comparison that follows must see it as negative.
    assert_eq!(binop(ISHL, 1, 31), Some(i32::MIN));
    assert_eq!(binop(ISHL, 0x0001_0000, 16), Some(0));
    let code = [ILOAD_0, ILOAD_1, ISHL, ICONST_0, IF_ICMPGE, 0x00, 0x05, ICONST_1, IRETURN, ICONST_0, IRETURN];
    assert_eq!(run_with(&code, &[1, 31], &[]), Some(1), "1 << 31 must be negative afterwards");
}

// ---------------------------------------------------------------------------------------------
// TRAP 3: division.
// ---------------------------------------------------------------------------------------------

#[test]
fn min_value_divided_by_minus_one_wraps_instead_of_faulting() {
    // JLS 15.17.2: the quotient is MIN_VALUE itself. A 32-bit `idiv` raises #DE here (the quotient
    // does not fit in 32 bits); doing the division in 64 bits makes 2^31 representable and the
    // mandatory `movsxd` performs the required truncation.
    assert_eq!(binop(IDIV, i32::MIN, -1), Some(i32::MIN));
    // JLS 15.17.3: and the remainder is 0.
    assert_eq!(binop(IREM, i32::MIN, -1), Some(0));
    assert_eq!(binop(IDIV, i32::MIN, 1), Some(i32::MIN));
    assert_eq!(binop(IDIV, i32::MAX, -1), Some(-i32::MAX));
}

#[test]
fn division_by_zero_deopts_rather_than_faulting() {
    // The one deopt site this tier has. Without the emitted `cmp/je`, `idiv` raises #DE, which on
    // Windows is a structured exception -- it would take the process down, not throw
    // ArithmeticException. `None` here means "the interpreter must run this method", which is
    // what turns it into a proper Java exception.
    assert_eq!(binop(IDIV, 1, 0), None);
    assert_eq!(binop(IREM, 1, 0), None);
    assert_eq!(binop(IDIV, 0, 0), None);
    assert_eq!(binop(IDIV, i32::MIN, 0), None);
    // A non-zero divisor on the same compiled code still works -- the guard is a branch, not a
    // refusal to compile.
    assert_eq!(binop(IDIV, 10, 3), Some(3));
}

#[test]
fn a_deopt_leaves_the_frame_exactly_as_a_normal_return_does() {
    // Both exits go through one epilogue, so an unbalanced stack on the deopt path would corrupt
    // the caller. Alternating the two exits many times would blow up the Rust stack (or return to
    // garbage) if they disagreed by so much as one push.
    let compiled = compile(&[ILOAD_0, ILOAD_1, IDIV, IRETURN], 2, &|_| None).unwrap();
    for i in 0..200i32 {
        assert_eq!(call(&compiled, &[100, 0]), None);
        assert_eq!(call(&compiled, &[100, 4]), Some(25), "iteration {i}");
    }
}

// ---------------------------------------------------------------------------------------------
// A model check over the whole subset, on values chosen to sit on the edges.
// ---------------------------------------------------------------------------------------------

#[test]
fn every_binary_operator_matches_a_rust_model_on_the_edge_values() {
    // Rust's `i32` arithmetic is the same two's-complement machine Java specifies, so
    // `wrapping_*` / `rotate`-free shift-by-mask is a faithful model — as long as *it* is written
    // with the same care the compiler is being tested for (`wrapping_add`, not `+`; an explicit
    // `& 31`, not the raw count; `as u32 >>` for `>>>`).
    let edges = [
        0, 1, -1, 2, -2, 7, -7, 31, 32, 33, 255, -255, 65536, i32::MAX, i32::MIN, i32::MAX - 1,
        i32::MIN + 1, 0x5555_5555u32 as i32, 0x7FFF_0001,
    ];
    /// A bytecode opcode paired with the Rust expression that models it.
    type Modelled = (u8, fn(i32, i32) -> Option<i32>);
    let ops: [Modelled; 11] = [
        (IADD, |a, b| Some(a.wrapping_add(b))),
        (ISUB, |a, b| Some(a.wrapping_sub(b))),
        (IMUL, |a, b| Some(a.wrapping_mul(b))),
        (IDIV, |a, b| (b != 0).then(|| a.wrapping_div(b))),
        (IREM, |a, b| (b != 0).then(|| a.wrapping_rem(b))),
        (IAND, |a, b| Some(a & b)),
        (IOR, |a, b| Some(a | b)),
        (IXOR, |a, b| Some(a ^ b)),
        (ISHL, |a, b| Some(a.wrapping_shl(b as u32 & 31))),
        (ISHR, |a, b| Some(a.wrapping_shr(b as u32 & 31))),
        (IUSHR, |a, b| Some(((a as u32) >> (b as u32 & 31)) as i32)),
    ];
    for (op, model) in ops {
        let compiled = compile(&[ILOAD_0, ILOAD_1, op, IRETURN], 2, &|_| None).unwrap();
        for &a in &edges {
            for &b in &edges {
                assert_eq!(call(&compiled, &[a, b]), model(a, b), "op 0x{op:02x} on ({a}, {b})");
            }
        }
    }
}

#[test]
fn a_method_that_uses_many_stack_slots_and_locals_still_works() {
    // Deep-ish operand stack (each `iload` pushes before anything pops) plus a local past the
    // single-byte opcodes, so the wide `iload`/`istore` forms and the higher frame slots are
    // exercised together. ((l0 + l1) + (l2 + l3)) * l4 with l4 read through `iload 4`.
    let code = [
        ILOAD_0, ILOAD_1, IADD, ILOAD_2, 0x1d, IADD, IADD, 0x15, 0x04, IMUL, ISTORE_1, ILOAD_1,
        IRETURN,
    ];
    assert_eq!(run_with(&code, &[1, 2, 3, 4, 10], &[]), Some(100));
    assert_eq!(compile(&code, 5, &|_| None).unwrap().touched_locals, vec![0, 1, 2, 3, 4]);
}

#[test]
fn an_ineligible_method_is_reported_not_compiled() {
    // The safety net the whole design rests on: one opcode outside the subset and there is no
    // native code at all, for any part of the method.
    let err = compile(&[ILOAD_0, 0xb4, 0x00, 0x02, IRETURN], 1, &|_| None).unwrap_err();
    assert_eq!(err, Ineligible::Opcode { pc: 1, opcode: 0xb4 }); // getfield
    assert!(err.to_string().contains("outside the compiled subset"));
}

// ---------------------------------------------------------------------------------------------
// Step 3: entering in the middle (OSR) and leaving in the middle (the safepoint poll).
//
// The same summing loop as `a_loop_runs_its_back_edge`, now entered *at its header* with the
// accumulator and the induction variable already part-way along. Every assertion here is a
// comparison against the value the *whole* loop would have produced, which is the only thing that
// distinguishes a correct on-stack entry from one that quietly restarted at pc 0.
// ---------------------------------------------------------------------------------------------

/// `int acc = local2; for (int i = local0; i < local1; i++) acc += i; return acc;`
///
/// ```text
///  0: iconst_0; istore_2                     acc = 0        (skipped by an on-stack entry)
///  2: iload_0; iload_1; if_icmpge -> 17      <- the loop header, operand stack empty
///  7: iload_2; iload_0; iadd; istore_2       acc += i
/// 11: iinc 0, 1
/// 14: goto -> 2                              the back-edge
/// 17: iload_2; ireturn
/// ```
const SUM_LOOP: [u8; 19] = [
    ICONST_0, ISTORE_2, ILOAD_0, ILOAD_1, IF_ICMPGE, 0x00, 0x0d, ILOAD_2, ILOAD_0, IADD, ISTORE_2,
    IINC, 0x00, 0x01, GOTO, 0xff, 0xf4, ILOAD_2, IRETURN,
];

#[test]
fn a_loop_header_is_reported_as_the_entry_point() {
    let compiled = compile(&SUM_LOOP, 3, &|_| None).unwrap();
    assert_eq!(compiled.osr_entries, vec![2]);
}

#[test]
fn entering_at_a_loop_header_resumes_rather_than_restarts() {
    let compiled = compile(&SUM_LOOP, 3, &|_| None).unwrap();
    // From the start: 0+1+...+9 = 45.
    assert_eq!(call(&compiled, &[0, 10, 0]), Some(45));
    // On-stack at pc 2 with i = 7 and acc = 21 (= 0+1+...+6, where the interpreter would have got
    // to): 21 + 7 + 8 + 9 = 45, the same answer arrived at from the middle. Entering at pc 0
    // instead would answer 45 as well *by accident* — so the discriminating case is the next one.
    assert_eq!(call_at(&compiled, &[7, 10, 21], 2).0, Outcome::Returned(45));
    // The discriminating case: `acc` starts at 1000. An entry at pc 0 would run `iconst_0;
    // istore_2` and throw it away.
    assert_eq!(call_at(&compiled, &[7, 10, 1000], 2).0, Outcome::Returned(1024));
    // And the induction variable really is honoured: entering with i already past the bound must
    // fall straight out of the loop.
    assert_eq!(call_at(&compiled, &[10, 10, 1000], 2).0, Outcome::Returned(1000));
}

#[test]
fn a_back_edge_that_carries_an_operand_is_compiled_but_not_entered() {
    // The one loop shape step 3 deliberately declines: a value live on the operand stack across
    // the back-edge. Transferring *that* state would mean rebuilding an operand stack on both
    // sides of the boundary, which is the complexity the whole "depth 0 only" rule exists to
    // avoid — so the header is not an entry point and not a poll site.
    //
    // The method still **compiles and runs**, unchanged. Ineligibility for on-stack entry is not
    // ineligibility for compilation, and that distinction is the entire cost of the rule.
    //
    //  0: iconst_5                     the carried operand — never popped until the end
    //  1: iconst_0; istore_0           i = 0
    //  3: iload_0; sipush 100; if_icmpge -> 16     <- header, but at depth 1
    // 10: iinc 0, 1
    // 13: goto -> 3
    // 16: ireturn                      returns the 5 that has been on the stack throughout
    let code = [
        0x08, ICONST_0, ISTORE_0, ILOAD_0, SIPUSH, 0x00, 0x64, IF_ICMPGE, 0x00, 0x09, IINC, 0x00,
        0x01, GOTO, 0xff, 0xf6, IRETURN,
    ];
    let compiled = compile(&code, 1, &|_| None).unwrap();
    assert!(compiled.osr_entries.is_empty(), "depth 1 at the header: not a transfer point");
    assert_eq!(call(&compiled, &[0]), Some(5), "and it still runs the loop and returns");
    // Nothing to dispatch on, so even a pc that *is* an instruction boundary is ignored and the
    // method runs from its start — which is precisely why the cache refuses to pass one.
    assert_eq!(call_at(&compiled, &[0], 3).0, Outcome::Returned(5));
}

#[test]
fn a_method_with_no_loop_ignores_the_entry_argument() {
    // Nothing to dispatch on, so the second argument is simply unread — an ordinary call passes 0
    // and the register is free. Checked because the argument is RDX, which the body also uses as
    // scratch: if the dispatch ever read it *after* an instruction ran, this is where it shows.
    let compiled = compile(&[ILOAD_0, ILOAD_1, IADD, IRETURN], 2, &|_| None).unwrap();
    assert!(compiled.osr_entries.is_empty());
    assert_eq!(call_at(&compiled, &[3, 4], 0).0, Outcome::Returned(7));
    assert_eq!(call_at(&compiled, &[3, 4], 999).0, Outcome::Returned(7));
}

#[test]
fn the_poll_leaves_at_a_loop_header_with_the_locals_already_written_back() {
    // A poll word of this test's own, so nothing else in the file (or in the test binary) can see
    // it set. `compile` bakes its address in as an immediate — which is the whole reason the
    // address has to be something that cannot move.
    static TEST_POLL: AtomicU64 = AtomicU64::new(0);
    let compiled =
        compile_with_poll(&SUM_LOOP, 3, &|_| None, &|_| None, &TEST_POLL as *const _ as usize)
            .unwrap();

    // Unset: the loop runs to the end, exactly as the shared-poll compilation above does.
    assert_eq!(call_at(&compiled, &[0, 10, 0], 0).0, Outcome::Returned(45));

    // Set: the first time control comes back round to the header, native code leaves — reporting
    // the header's pc and having already written the locals through to the caller's buffer. One
    // iteration ran (i went 0 -> 1, acc 0 -> 0), which is the progress guarantee that keeps an
    // interpreter/JIT ping-pong from spinning: an on-stack entry lands *past* the poll.
    TEST_POLL.store(1, Ordering::Release);
    let (outcome, locals) = call_at(&compiled, &[0, 10, 0], 2);
    assert_eq!(outcome, Outcome::Safepoint(2));
    assert_eq!(locals[0], 1, "i advanced by the one iteration that ran");
    assert_eq!(locals[2], 0, "acc += 0");
    // Resuming from what it handed back, repeatedly, must reach the same answer the uninterrupted
    // loop does — this is the interpreter's job in miniature, minus the interpreting.
    let (mut i, mut acc) = (locals[0] as i32, locals[2] as i32);
    for _ in 0..8 {
        let (outcome, locals) = call_at(&compiled, &[i, 10, acc], 2);
        assert_eq!(outcome, Outcome::Safepoint(2));
        i = locals[0] as i32;
        acc = locals[2] as i32;
    }
    assert_eq!((i, acc), (9, 36), "eight more single iterations: i = 9, acc = 1+...+8");
    TEST_POLL.store(0, Ordering::Release);
    assert_eq!(call_at(&compiled, &[i, 10, acc], 2).0, Outcome::Returned(45), "and it lands on 45");

    // An *ordinary* entry to the same code is unaffected by the second argument being a pc: with
    // the poll down it runs start to finish.
    assert_eq!(call_at(&compiled, &[0, 10, 0], 0).0, Outcome::Returned(45));
}

#[test]
fn nested_loops_each_get_their_own_entry_point_and_poll() {
    // for (i = 0; i < l0; i++) for (j = 0; j < l1; j++) acc++;  -> l0 * l1
    // Locals: 0 = the outer bound, 1 = the inner bound, 2 = acc, 3 = i, 4 = j.
    let code = [
        ICONST_0, ISTORE_2, // 0: acc = 0
        ICONST_0, 0x3e, // 2: istore_3 — i = 0
        0x1d, ILOAD_0, IF_ICMPGE, 0x00, 0x1b, // 4: iload_3; iload_0; if_icmpge +27 -> 33
        ICONST_0, 0x36, 0x04, // 9: iconst_0; istore 4 — j = 0
        0x15, 0x04, ILOAD_1, IF_ICMPGE, 0x00, 0x0c, // 12: iload 4; iload_1; if_icmpge +12 -> 27
        IINC, 0x02, 0x01, // 18: acc++
        IINC, 0x04, 0x01, // 21: j++
        GOTO, 0xff, 0xf4, // 24: goto -12 -> 12
        IINC, 0x03, 0x01, // 27: i++
        GOTO, 0xff, 0xe6, // 30: goto -26 -> 4
        ILOAD_2, IRETURN, // 33
    ];
    let compiled = compile(&code, 5, &|_| None).unwrap();
    assert_eq!(compiled.osr_entries, vec![4, 12], "one per loop, in pc order");
    assert_eq!(call(&compiled, &[3, 4, 0, 0, 0]), Some(12));
    // Entering at the *inner* header mid-flight: i = 1, j = 2, acc = 6 of a 3x4 grid means
    // 2 remaining inner iterations, then 1 whole outer iteration of 4 -> 6 + 2 + 4 = 12.
    assert_eq!(call_at(&compiled, &[3, 4, 6, 1, 2], 12).0, Outcome::Returned(12));
    // And at the outer header: i = 2 of 3, acc = 8 -> one more row of 4.
    assert_eq!(call_at(&compiled, &[3, 4, 8, 2, 0], 4).0, Outcome::Returned(12));
}

// =============================================================================================
// Step 4: the opcodes that widened the subset.
//
// These sit one level below `jit_tests`: no VM, no class file — bytecode straight into the
// compiler, mapped W^X, executed, and the answer compared against what the JVMS says. For the
// stack shuffles that comparison is against the **interpreter's own implementation** of the same
// opcode rather than against a number written out by hand, which makes them differential tests in
// the strict sense even though no Java source can produce them (see the note on `shuffle`).
// =============================================================================================

const NOP: u8 = 0x00;
const POP2: u8 = 0x58;
const DUP_X1: u8 = 0x5a;
const DUP_X2: u8 = 0x5b;
const DUP2: u8 = 0x5c;
const DUP2_X1: u8 = 0x5d;
const DUP2_X2: u8 = 0x5e;
const TABLESWITCH: u8 = 0xaa;
const LOOKUPSWITCH: u8 = 0xab;
const GETSTATIC: u8 = 0xb2;
const WIDE: u8 = 0xc4;

/// Pushes `values` (bottom-first), runs the single stack opcode `op`, then **drains the whole
/// operand stack into one `int`**: `acc = acc * 10 + top`, repeatedly, until the stack is empty.
/// With single-digit values the returned number *is* the resulting stack read from the top down,
/// so one comparison checks the entire permutation rather than just its top.
///
/// The drain is five instructions per value — `iload_0; bipush 10; imul; iadd; istore_0`, applied
/// to a stack that already has the value underneath — and every one of them is in the subset, so
/// the program under test is the shuffle and nothing else.
fn shuffle(values: &[i32], op: u8, result_depth: usize) -> i32 {
    let mut code = vec![ICONST_0, ISTORE_0]; // acc = 0
    for &v in values {
        code.extend([BIPUSH, v as u8]);
    }
    code.push(op);
    for _ in 0..result_depth {
        code.extend([ILOAD_0, BIPUSH, 10, IMUL, IADD, ISTORE_0]);
    }
    code.extend([ILOAD_0, IRETURN]);
    run_with(&code, &[0], &[]).expect("the shuffles never deopt")
}

/// The same permutation asked of the **interpreter**, by running its own opcode implementation
/// over a `Frame` and draining the result the same way. This is what `shuffle`'s answer is
/// compared against: `javac` cannot emit `dup2_x2` for int-only code (the forms it does emit all
/// involve arrays or fields, which are outside the subset), so a whole-VM differential test of
/// these opcodes is not expressible in Java — but the two implementations can still be asked the
/// same question directly, which is the part that actually matters.
fn shuffle_interpreted(values: &[i32], op: u8) -> i32 {
    use crate::jvm::interpreter::bytecode_interpreter::stack_operations;
    use crate::jvm::interpreter::frame::{Frame, Value};

    let mut frame = Frame::new(0, 1, Vec::new());
    for &v in values {
        frame.push(Value::Int(v));
    }
    match op {
        NOP => {}
        POP => stack_operations::pop(&mut frame),
        POP2 => stack_operations::pop2(&mut frame),
        DUP => stack_operations::dup(&mut frame),
        DUP_X1 => stack_operations::dup_x1(&mut frame),
        DUP_X2 => stack_operations::dup_x2(&mut frame),
        DUP2 => stack_operations::dup2(&mut frame),
        DUP2_X1 => stack_operations::dup2_x1(&mut frame),
        DUP2_X2 => stack_operations::dup2_x2(&mut frame),
        SWAP => stack_operations::swap(&mut frame),
        other => panic!("not a stack opcode: 0x{other:02x}"),
    }
    frame.stack().iter().rev().fold(0, |acc, v| match v {
        Value::Int(x) => acc * 10 + x,
        other => panic!("the subset only ever holds ints: {other:?}"),
    })
}

#[test]
fn every_stack_shuffle_permutes_exactly_as_the_interpreter_does() {
    // Distinct single digits, so the drained number names every slot unambiguously — `[1, 2, 3, 4]`
    // is bottom-to-top, and `dup2_x2` turning it into `3 4 1 2 3 4` reads back as 432143.
    for (values, op, depth) in [
        (&[1, 2][..], NOP, 2),
        (&[1, 2][..], POP, 1),
        (&[1, 2, 3][..], POP2, 1),
        (&[1, 2][..], DUP, 3),
        (&[1, 2][..], DUP_X1, 3),
        (&[1, 2, 3][..], DUP_X2, 4),
        (&[1, 2][..], DUP2, 4),
        (&[1, 2, 3][..], DUP2_X1, 5),
        (&[1, 2, 3, 4][..], DUP2_X2, 6),
        (&[1, 2][..], SWAP, 2),
    ] {
        let compiled = shuffle(values, op, depth);
        let interpreted = shuffle_interpreted(values, op);
        assert_eq!(
            compiled, interpreted,
            "0x{op:02x} on {values:?}: compiled says {compiled}, the interpreter says {interpreted}"
        );
    }
}

#[test]
fn the_dup_family_matches_the_jvms_tables_by_inspection() {
    // The same permutations again, this time against numbers written out from JVMS 6.5 by hand.
    // Belt and braces on purpose: the test above proves the two *implementations* agree, which
    // would still pass if both were wrong in the same way. These constants come from the spec.
    assert_eq!(shuffle(&[1, 2], DUP, 3), 221, "..., v -> ..., v, v");
    assert_eq!(shuffle(&[1, 2], DUP_X1, 3), 212, "..., v2, v1 -> ..., v1, v2, v1");
    assert_eq!(shuffle(&[1, 2, 3], DUP_X2, 4), 3213, "..., v3, v2, v1 -> ..., v1, v3, v2, v1");
    assert_eq!(shuffle(&[1, 2], DUP2, 4), 2121, "..., v2, v1 -> ..., v2, v1, v2, v1");
    assert_eq!(shuffle(&[1, 2, 3], DUP2_X1, 5), 32132, "..., v3, v2, v1 -> ..., v2, v1, v3, v2, v1");
    assert_eq!(shuffle(&[1, 2, 3, 4], DUP2_X2, 6), 432143, "..., v4..v1 -> ..., v2, v1, v4, v3, v2, v1");
    assert_eq!(shuffle(&[1, 2], SWAP, 2), 12);
    assert_eq!(shuffle(&[1, 2, 3], POP2, 1), 1, "both category-1 values go");
    assert_eq!(shuffle(&[1, 2], NOP, 2), 21, "and `nop` really does nothing");
}

#[test]
fn a_stack_shuffle_reserves_the_slots_it_needs() {
    // `dup2_x2` leaves the stack two deeper than it found it, and its emitted code writes two slots
    // *above* the four it read (that is where it parks s2/s3 while it moves s0/s1 out of the way).
    // If the frame were sized from the entry depth rather than from the exit depth, those two
    // stores would land on someone else's memory — the return address, in the worst case.
    let code = [
        ICONST_1, 0x05, 0x06, 0x07, // depth 4: 1, 2, 3, 4
        DUP2_X2, // depth 6
        ISTORE_0, POP, POP, POP, POP, POP, // drain the six
        ILOAD_0, IRETURN,
    ];
    let compiled = compile(&code, 1, &|_| None).unwrap();
    assert_eq!(compiled.stack_slots, 6, "the frame is sized by the *deepest* the stack ever gets");
    assert_eq!(call(&compiled, &[0]), Some(4), "the top after the shuffle is v1 = 4");
}

// ---------------------------------------------------------------------------------------------
// `wide`.
// ---------------------------------------------------------------------------------------------

/// `wide iload <index>` / `wide istore <index>` — four bytes.
fn wide_local(op: u8, index: u16) -> [u8; 4] {
    let [hi, lo] = index.to_be_bytes();
    [WIDE, op, hi, lo]
}

/// `wide iinc <index>, <delta>` — six bytes, and the delta is a **signed** 16-bit value.
fn wide_iinc(index: u16, delta: i16) -> [u8; 6] {
    let ([hi, lo], [dh, dl]) = (index.to_be_bytes(), delta.to_be_bytes());
    [WIDE, IINC, hi, lo, dh, dl]
}

#[test]
fn wide_iload_and_istore_reach_a_local_past_255() {
    // Copy local 300 into local 299 and return it. Nothing here is expressible with the narrow
    // opcodes at all: their operand is a single byte.
    let mut code = Vec::new();
    code.extend(wide_local(0x15, 300)); // wide iload 300
    code.extend(wide_local(0x36, 299)); // wide istore 299
    code.extend(wide_local(0x15, 299)); // wide iload 299
    code.push(IRETURN);

    let mut locals = vec![0; 301];
    locals[300] = -12345;
    assert_eq!(run_with(&code, &locals, &[]), Some(-12345));

    // And the marshalling contract names exactly those two slots — a caller that filled 301 of
    // them would be doing 299 slots of pointless work per call.
    let compiled = compile(&code, 301, &|_| None).unwrap();
    assert_eq!(compiled.touched_locals, vec![299, 300]);
}

#[test]
fn wide_iinc_takes_a_16_bit_signed_constant_and_still_wraps() {
    // The reason `wide iinc` exists: `x += 300` does not fit the narrow form's signed byte.
    let mut code = wide_iinc(0, 300).to_vec();
    code.extend([ILOAD_0, IRETURN]);
    assert_eq!(run_with(&code, &[42], &[]), Some(342));

    // Both extremes of the 16-bit field, and the negative direction.
    let mut code = wide_iinc(0, i16::MAX).to_vec();
    code.extend(wide_iinc(0, i16::MIN));
    code.extend([ILOAD_0, IRETURN]);
    assert_eq!(run_with(&code, &[0], &[]), Some(-1), "32767 + (-32768)");

    // TRAP 1 is not suspended by the prefix: the result still wraps at 32 bits. In 64-bit
    // arithmetic without the `movsxd` this would answer 2147483947.
    let mut code = wide_iinc(0, 300).to_vec();
    code.extend([ILOAD_0, IRETURN]);
    assert_eq!(run_with(&code, &[i32::MAX], &[]), Some(i32::MIN + 299));
}

#[test]
fn a_wide_wrapping_anything_else_is_refused() {
    // `wide aload`, `wide lload`, `wide astore`, `wide fstore`, `wide ret` — the prefix is only
    // accepted in front of the three instructions the subset already has. Reported as 0xc4 rather
    // than as the inner byte, so `Ineligible::Opcode { pc, opcode }` always names `code[pc]`.
    for inner in [0x19u8, 0x1e, 0x3a, 0x38, 0xa9] {
        let mut code = wide_local(inner, 300).to_vec();
        code.push(IRETURN);
        assert_eq!(
            compile(&code, 301, &|_| None).unwrap_err(),
            Ineligible::Opcode { pc: 0, opcode: WIDE },
            "wide 0x{inner:02x} must be refused"
        );
    }
    // A local index past `max_locals` is refused through the wide form too.
    let mut code = wide_local(0x15, 300).to_vec();
    code.push(IRETURN);
    assert_eq!(
        compile(&code, 20, &|_| None).unwrap_err(),
        Ineligible::LocalOutOfRange { pc: 0, slot: 300 }
    );
}

#[test]
fn a_wide_instruction_is_measured_at_its_full_length() {
    // The one bug a value-checking test would miss. `wide iinc` is **6** bytes; if the decoder
    // called it 4, the scan would resynchronise on the delta's low byte and decode *that* as an
    // opcode. Here the delta is 0x0102, whose low byte 0x02 is `iconst_m1` — perfectly decodable,
    // so nothing would complain and the emitted code would simply be wrong.
    let mut code = wide_iinc(0, 0x0102).to_vec();
    code.extend([ILOAD_0, IRETURN]);
    let compiled = compile(&code, 1, &|_| None).unwrap();
    assert_eq!(compiled.stack_slots, 1, "one push, not two: the stray `iconst_m1` was never decoded");
    assert_eq!(call(&compiled, &[0]), Some(0x0102));
}

// ---------------------------------------------------------------------------------------------
// `tableswitch` / `lookupswitch`.
// ---------------------------------------------------------------------------------------------

/// Assembles `iload_0; <switch>; <arms>`, where arm `k` returns `10 * (k + 1)` and the `default`
/// arm returns 99. `kind` picks `tableswitch` (keys must then be contiguous) or `lookupswitch`.
///
/// The padding is computed the way the JVMS defines it — from the byte after the opcode to the next
/// multiple of four **counted from the start of the code array** — which is the whole reason this
/// is a function rather than a literal: put the opcode at a different pc and the layout changes.
fn switch_program(kind: u8, keys: &[i32]) -> Vec<u8> {
    let n = keys.len();
    let pc = 1usize; // the switch itself, right after `iload_0`
    let pad = (4 - ((pc + 1) % 4)) % 4;
    let header = 1 + pad + if kind == TABLESWITCH { 12 + 4 * n } else { 8 + 8 * n };
    let arm = |k: usize| (pc + header + 3 * k) as i32 - pc as i32; // each arm is `bipush v; ireturn`
    let default = (pc + header + 3 * n) as i32 - pc as i32;

    let mut code = vec![ILOAD_0, kind];
    code.extend(std::iter::repeat_n(0u8, pad));
    code.extend(default.to_be_bytes());
    match kind == TABLESWITCH {
        true => {
            code.extend(keys[0].to_be_bytes());
            code.extend(keys[n - 1].to_be_bytes());
            for k in 0..n {
                code.extend(arm(k).to_be_bytes());
            }
        }
        false => {
            code.extend((n as i32).to_be_bytes());
            for (k, &key) in keys.iter().enumerate() {
                code.extend(key.to_be_bytes());
                code.extend(arm(k).to_be_bytes());
            }
        }
    }
    for k in 0..=n {
        let value = if k == n { 99 } else { 10 * (k + 1) as i32 };
        code.extend([BIPUSH, value as u8, IRETURN]);
    }
    code
}

#[test]
fn a_tableswitch_selects_every_arm_and_its_default() {
    let keys: Vec<i32> = (0..5).collect();
    let code = switch_program(TABLESWITCH, &keys);
    for (k, &key) in keys.iter().enumerate() {
        assert_eq!(run_with(&code, &[key], &[]), Some(10 * (k as i32 + 1)), "case {key}");
    }
    // One below the first, one above the last, and something far away: all `default`.
    for key in [-1, 5, 1000, i32::MIN, i32::MAX] {
        assert_eq!(run_with(&code, &[key], &[]), Some(99), "key {key} must take the default");
    }
}

#[test]
fn a_tableswitch_with_a_negative_low_compares_signed() {
    // `low = -3`. A compare chain that used unsigned condition codes, or that computed
    // `key - low` in the wrong width, gets this wrong for every negative key and only for those.
    let keys: Vec<i32> = (-3..=1).collect();
    let code = switch_program(TABLESWITCH, &keys);
    for (k, &key) in keys.iter().enumerate() {
        assert_eq!(run_with(&code, &[key], &[]), Some(10 * (k as i32 + 1)), "case {key}");
    }
    assert_eq!(run_with(&code, &[-4], &[]), Some(99));
    assert_eq!(run_with(&code, &[2], &[]), Some(99));
}

#[test]
fn a_lookupswitch_handles_sparse_keys_including_both_extremes() {
    // `Integer.MIN_VALUE` and `MAX_VALUE` are the two keys most likely to expose a width or
    // signedness mistake, and a `lookupswitch` is the only opcode that can name both at once.
    let keys = [i32::MIN, -100, 0, 7, 1000, i32::MAX];
    let code = switch_program(LOOKUPSWITCH, &keys);
    for (k, &key) in keys.iter().enumerate() {
        assert_eq!(run_with(&code, &[key], &[]), Some(10 * (k as i32 + 1)), "case {key}");
    }
    for key in [i32::MIN + 1, -99, 1, 8, 999, i32::MAX - 1] {
        assert_eq!(run_with(&code, &[key], &[]), Some(99), "key {key} must take the default");
    }
}

#[test]
fn an_empty_lookupswitch_is_just_a_goto_to_its_default() {
    // `npairs = 0` is legal and javac emits it for `switch (x) { default: ... }`. The compare chain
    // is then empty and the whole instruction is one unconditional jump — a case worth pinning
    // because a loop written as `for k in 0..n-1` would silently drop the last arm of every switch
    // and *only* be visibly wrong here.
    let code = switch_program(LOOKUPSWITCH, &[]);
    for key in [-1, 0, 1, i32::MIN, i32::MAX] {
        assert_eq!(run_with(&code, &[key], &[]), Some(99));
    }
}

#[test]
fn a_switch_arm_that_branches_backwards_is_a_loop_header() {
    // The back-edge of a loop expressed as a `tableswitch` arm rather than as a `goto`, which is
    // the shape a `continue` inside a `switch` inside a loop produces. The scan has to walk
    // *through* the switch to notice that pc 2 is a loop header at all.
    //
    //  0: iconst_0; istore_1          acc = 0
    //  2: iload_0                     <- the header, stack empty
    //  3: ifle +29 -> 32              while (n > 0)
    //  6: iinc 1, 1                   acc++
    //  9: iinc 0, -1                  n--
    // 12: iconst_0                    the switch key
    // 13: tableswitch (2 pad bytes), low 0, high 0: arm 0 -> 2 (backwards), default -> 32
    // 32: iload_1; ireturn
    let mut code = vec![
        ICONST_0, 0x3c, // 0: iconst_0; istore_1
        ILOAD_0, // 2: the loop header
        0x9e, 0x00, 0x1d, // 3: ifle +29 -> 32
        IINC, 0x01, 0x01, // 6: acc++
        IINC, 0x00, 0xff, // 9: n--
        ICONST_0, // 12: the switch key
        TABLESWITCH, 0x00, 0x00, // 13: opcode + 2 pad bytes (14 -> the next multiple of 4 is 16)
    ];
    code.extend((32i32 - 13).to_be_bytes()); // default -> 32
    code.extend(0i32.to_be_bytes()); // low
    code.extend(0i32.to_be_bytes()); // high
    code.extend((2i32 - 13).to_be_bytes()); // arm 0 -> 2, a back-edge
    assert_eq!(code.len(), 32, "the switch ends exactly where the epilogue starts");
    code.extend([ILOAD_1, IRETURN]); // 32

    let compiled = compile(&code, 2, &|_| None).unwrap();
    assert_eq!(compiled.osr_entries, vec![2], "the switch arm's target is a loop header");
    assert_eq!(call(&compiled, &[5, 0]), Some(5));
    // And the on-stack entry works there like any other: 3 iterations already done, 2 to go.
    assert_eq!(call_at(&compiled, &[2, 3], 2).0, Outcome::Returned(5));
}

#[test]
fn a_malformed_or_oversized_switch_is_refused() {
    // `low > high` — JVMS forbids it, and the entry-count subtraction would underflow.
    let mut code = vec![ILOAD_0, TABLESWITCH, 0x00, 0x00];
    code.extend(20i32.to_be_bytes()); // default
    code.extend(5i32.to_be_bytes()); // low
    code.extend(1i32.to_be_bytes()); // high — smaller
    code.extend([BIPUSH, 9, IRETURN]);
    assert_eq!(compile(&code, 1, &|_| None).unwrap_err(), Ineligible::OutOfRange { pc: 1 });

    // A negative `npairs`, likewise.
    let mut code = vec![ILOAD_0, LOOKUPSWITCH, 0x00, 0x00];
    code.extend(12i32.to_be_bytes());
    code.extend((-1i32).to_be_bytes());
    code.extend([BIPUSH, 9, IRETURN]);
    assert_eq!(compile(&code, 1, &|_| None).unwrap_err(), Ineligible::OutOfRange { pc: 1 });

    // More cases than the compare chain is willing to emit. Not a correctness limit — a code-size
    // one, and the honest answer until there is a jump table.
    let keys: Vec<i32> = (0..MAX_SWITCH_CASES as i32 + 1).collect();
    let code = switch_program(LOOKUPSWITCH, &keys);
    assert_eq!(compile(&code, 1, &|_| None).unwrap_err(), Ineligible::TooBig);
    // One fewer is fine — a 256-case chain compiles and still picks the right arm. (Only the first
    // dozen arms are checked by value: `switch_program`'s bodies are `bipush`, whose operand is a
    // signed byte, so `10 * (k + 1)` stops being representable long before case 255.)
    let keys: Vec<i32> = (0..MAX_SWITCH_CASES as i32).collect();
    let code = switch_program(LOOKUPSWITCH, &keys);
    assert!(compile(&code, 1, &|_| None).is_ok(), "{MAX_SWITCH_CASES} cases is the limit, not past it");
    for k in [0i32, 1, 7, 11] {
        assert_eq!(run_with(&code, &[k], &[]), Some(10 * (k + 1)), "case {k} of 256");
    }
    assert_eq!(run_with(&code, &[MAX_SWITCH_CASES as i32], &[]), Some(99), "one past the last case");
}

// ---------------------------------------------------------------------------------------------
// `getstatic`.
// ---------------------------------------------------------------------------------------------

#[test]
fn getstatic_reads_the_live_four_bytes_at_the_address_it_was_given() {
    use std::sync::atomic::AtomicI32;

    // Three neighbouring cells, so an off-by-one in the address would read the wrong one and be
    // caught rather than merely looking odd. `AtomicI32` because the compiled code reads these
    // while Rust also writes them; the accesses are 4 bytes wide on both sides.
    static CELLS: [AtomicI32; 3] = [AtomicI32::new(0), AtomicI32::new(0), AtomicI32::new(0)];
    let address = |i: usize| &CELLS[i] as *const _ as usize;

    // getstatic #1; getstatic #2; iadd; ireturn — with #1 and #2 the outer two cells.
    let code = [GETSTATIC, 0x00, 0x01, GETSTATIC, 0x00, 0x02, IADD, IRETURN];
    let compiled = compile_with_poll(
        &code,
        0,
        &|_| None,
        &|index| match index {
            1 => Some(address(0)),
            2 => Some(address(2)),
            _ => None,
        },
        &POLL as *const _ as usize,
    )
    .unwrap();

    CELLS[0].store(40, Ordering::Release);
    CELLS[1].store(999_999, Ordering::Release); // the neighbour: never part of any answer
    CELLS[2].store(2, Ordering::Release);
    assert_eq!(call(&compiled, &[]), Some(42));

    // A **live** read, not a value folded in at compile time: the same code, called again after
    // the cells changed, must report the new numbers.
    CELLS[0].store(-1, Ordering::Release);
    CELLS[2].store(-2, Ordering::Release);
    assert_eq!(call(&compiled, &[]), Some(-3));

    // And the load is sign-extending and exactly 4 bytes wide. A 64-bit load would drag in the
    // neighbouring cell; a zero-extending one would answer 4294967295 instead of -1.
    CELLS[0].store(-1, Ordering::Release);
    CELLS[2].store(0, Ordering::Release);
    assert_eq!(call(&compiled, &[]), Some(-1));
    CELLS[0].store(i32::MIN, Ordering::Release);
    CELLS[2].store(0, Ordering::Release);
    assert_eq!(call(&compiled, &[]), Some(i32::MIN), "the normalisation invariant holds on entry");
}

#[test]
fn a_getstatic_the_resolver_will_not_answer_for_is_refused() {
    // The resolver says `None` for a field that is not a static `int`, or whose declaring class is
    // not initialised yet. Either way the method is simply never compiled — compiled code has no
    // way to run a `<clinit>`, and no business reading a reference or a `long` slot as an `int`.
    let code = [GETSTATIC, 0x00, 0x07, IRETURN];
    assert_eq!(
        compile(&code, 0, &|_| None).unwrap_err(),
        Ineligible::UnresolvedStatic { pc: 0, index: 7 }
    );
    // And one byte of it inside an otherwise perfect method is enough, as for every other opcode.
    let code = [ILOAD_0, GETSTATIC, 0x00, 0x07, IADD, IRETURN];
    assert!(matches!(
        compile(&code, 1, &|_| None).unwrap_err(),
        Ineligible::UnresolvedStatic { pc: 1, index: 7 }
    ));
}
