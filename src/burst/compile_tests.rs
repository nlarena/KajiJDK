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
    CompiledCode, Environment, Heap, Ineligible, Instance, Kind, Method, Outcome, ResumeSite,
    Status, MAX_SWITCH_CASES,
};
use super::exec_mem::ExecMem;

/// [`super::compile::compile`] with everything a program here does not use filled in: a `static`
/// method returning an `int`, no constants beyond `int_const`, no statics, no fields, and no heap.
/// `heap` is the one that is worth naming — a [`Heap::default`] has `max_offset == 0`, which means
/// "this VM told the compiler nothing about its heap", so any opcode that would dereference a
/// reference is refused rather than emitted against a base address of zero.
fn compile_with_poll(
    code: &[u8],
    max_locals: usize,
    int_const: &dyn Fn(u16) -> Option<i32>,
    poll_word: usize,
) -> Result<CompiledCode, Ineligible> {
    compile_shaped(code, max_locals, "()I", true, int_const, Heap::default(), poll_word)
}

/// [`compile_with_poll`] for a method whose *shape* matters: the descriptor is what fixes the kind
/// of every entry local and of the exit, and the [`Heap`] is what makes a heap read compilable at
/// all.
fn compile_shaped(
    code: &[u8],
    max_locals: usize,
    descriptor: &str,
    is_static: bool,
    int_const: &dyn Fn(u16) -> Option<i32>,
    heap: Heap,
    poll_word: usize,
) -> Result<CompiledCode, Ineligible> {
    super::compile::compile(
        &Method { unit: 0, code, max_locals, descriptor, is_static, has_handlers: false },
        &Environment {
            // One body, so the unit is the root's and carries no information.
            int_const: &|_, index| int_const(index),
            static_int: &|_, _| None,
            int_field: &|_, _, _| None,
            instance: &|_, _| None,
            invoke: &|_, _, _| None,
            heap,
            poll_word,
        },
    )
}

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
    compile_with_poll(code, max_locals, int_const, &POLL as *const _ as usize)
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
    // **The buffer's second half.** Since F3 step 6 a deopt spills the live operand stack into the
    // slots past the locals, so the contract is `buffer_slots` long, not `max_locals` long — a
    // buffer sized the old way would be written past its end by the very first null check. The one
    // extra slot keeps `as_mut_ptr` non-dangling for a zero-local, zero-stack program.
    buffer.resize(buffer.len().max(compiled.buffer_slots as usize) + 1, 0);
    // SAFETY: `compile` emits exactly one `extern "system" fn(*mut i64, i64) -> i64` at offset 0,
    // built from `x64::Frame`, restoring every non-volatile register and ending in `ret`. The
    // buffer is a live, initialised `[i64]` at least `locals.len()` long, which is the marshalling
    // contract (`touched_locals` indices are all `< max_locals == locals.len()`), and `entry_pc`
    // is 0 or one of the code's own `osr_entries` at every call site below.
    let f: extern "system" fn(*mut i64, i64) -> i64 = unsafe { mem.as_fn() };
    let raw = f(buffer.as_mut_ptr(), entry_pc);
    (Status::unpack(raw), buffer)
}

/// The resume site at `pc` — what the interpreter would be handed if native code stopped there.
fn site_at(compiled: &CompiledCode, pc: u32) -> &ResumeSite {
    compiled.resume_sites.iter().find(|site| site.pc == pc).expect("a resume site at this pc")
}

/// The operand stack a deopt at `pc` spilled, read out of the buffer `call_at` handed back. The
/// values are bottom-first, which is push order.
fn spilled(compiled: &CompiledCode, buffer: &[i64], pc: u32) -> Vec<i64> {
    let base = compiled.stack_base as usize;
    (0..site_at(compiled, pc).stack.len()).map(|k| buffer[base + k]).collect()
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
    let err = compile(&[ILOAD_0, 0xb6, 0x00, 0x02, IRETURN], 1, &|_| None).unwrap_err();
    assert_eq!(err, Ineligible::Opcode { pc: 1, opcode: 0xb6 }); // invokevirtual: a *call*, never in
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
        compile_with_poll(&SUM_LOOP, 3, &|_| None, &TEST_POLL as *const _ as usize).unwrap();

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
    run_with(&shuffle_program(values, op, result_depth), &[0], &[]).expect("the shuffles never deopt")
}

/// The program [`shuffle`] runs, split out so step 10 can compile the same bytes at every cache
/// size instead of only at the default one.
fn shuffle_program(values: &[i32], op: u8, result_depth: usize) -> Vec<u8> {
    let mut code = vec![ICONST_0, ISTORE_0]; // acc = 0
    for &v in values {
        code.extend([BIPUSH, v as u8]);
    }
    code.push(op);
    for _ in 0..result_depth {
        code.extend([ILOAD_0, BIPUSH, 10, IMUL, IADD, ISTORE_0]);
    }
    code.extend([ILOAD_0, IRETURN]);
    code
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
    // `wide lload`, `wide fload`, `wide fstore`, `wide dstore`, `wide ret` — the prefix is only
    // accepted in front of the five instructions the subset has (`iload`/`aload`/`istore`/`astore`/
    // `iinc`). Reported as 0xc4 rather than as the inner byte, so `Ineligible::Opcode { pc, opcode }`
    // always names `code[pc]`.
    for inner in [0x16u8, 0x17, 0x1e, 0x38, 0x39, 0xa9] {
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
    let compiled = super::compile::compile(
        &Method { unit: 0, code: &code, max_locals: 0, descriptor: "()I", is_static: true, has_handlers: false },
        &Environment {
            int_const: &|_, _| None,
            static_int: &|_, index| match index {
                1 => Some(address(0)),
                2 => Some(address(2)),
                _ => None,
            },
            int_field: &|_, _, _| None,
            instance: &|_, _| None,
            invoke: &|_, _, _| None,
            heap: Heap::default(),
            poll_word: &POLL as *const _ as usize,
        },
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

// ---------------------------------------------------------------------------------------------
// Step 5: references. The type map, the two-armed heap address, and the guards that deopt.
// ---------------------------------------------------------------------------------------------

const ACONST_NULL: u8 = 0x01;
const ALOAD_0: u8 = 0x2a;
const ALOAD_1: u8 = 0x2b;
const ASTORE_1: u8 = 0x4c;
const IALOAD: u8 = 0x2e;
const IF_ACMPEQ: u8 = 0xa5;
const IFNULL: u8 = 0xc6;
const ARETURN: u8 = 0xb0;
const GETFIELD: u8 = 0xb4;
const ARRAYLENGTH: u8 = 0xbe;
const IASTORE: u8 = 0x4f;
const PUTSTATIC: u8 = 0xb3;
const PUTFIELD: u8 = 0xb5;

/// A stand-in for the VM's heap: **two** buffers and the numbers that decide which of them an
/// offset belongs to, laid out exactly as `HeapService` lays its own out (Eden below `eden_end`,
/// everything else above). Two really are needed — one buffer would let a compiler that ignored
/// the split pass every test here, which is the mistake this shape exists to catch.
struct FakeHeap {
    eden: Vec<u8>,
    other: Vec<u8>,
    /// Eden's bump cursor, boxed for the same reason the real arena boxes its own: a compiled `new`
    /// bakes this word's address in, so it must not move when the `FakeHeap` does.
    cursor: Box<std::sync::atomic::AtomicUsize>,
}

impl FakeHeap {
    /// Offset 0 is `null` and the first eight bytes are never handed out, as in the real heap.
    const NULL_PAGE: usize = 8;
    /// Eden's size, and therefore where the boundary between the two buffers sits.
    const EDEN_SIZE: usize = 256;
    const EDEN_END: u32 = (Self::NULL_PAGE + Self::EDEN_SIZE) as u32;
    const CAPACITY: usize = 4096;

    fn new() -> FakeHeap {
        FakeHeap {
            eden: vec![0; Self::EDEN_SIZE],
            other: vec![0; Self::CAPACITY],
            cursor: Box::new(std::sync::atomic::AtomicUsize::new(0)),
        }
    }

    /// The addresses and layout constants, biased exactly as `HeapService::jit_bases` biases them.
    fn bases(&self) -> Heap {
        Heap {
            eden_base: self.eden.as_ptr() as usize - Self::NULL_PAGE,
            other_base: self.other.as_ptr() as usize,
            eden_end: Self::EDEN_END,
            max_offset: Self::CAPACITY,
            eden_cursor: &*self.cursor as *const _ as usize,
            eden_capacity: Self::EDEN_SIZE,
            null_page: Self::NULL_PAGE as u32,
            array_length: 8,    // the `length` word, right after the object header
            int_array_data: 12, // ...and the elements right after that
            int_element: 4,
        }
    }

    /// Eden's cursor, as the arena's `used()` reports it — how many bytes have been handed out.
    fn eden_used(&self) -> usize {
        self.cursor.load(Ordering::Relaxed)
    }

    /// Writes a 4-byte little-endian word at a heap **offset**, routing it to the buffer that
    /// offset belongs to — the same two-armed decision the emitted code makes.
    fn write(&mut self, offset: usize, value: i32) {
        let bytes = value.to_le_bytes();
        match offset < Self::EDEN_END as usize {
            true => self.eden[offset - Self::NULL_PAGE..][..4].copy_from_slice(&bytes),
            false => self.other[offset..][..4].copy_from_slice(&bytes),
        }
    }

    /// Reads back a 4-byte little-endian word at a heap **offset** — the inverse of
    /// [`FakeHeap::write`], and what a test of a *write* opcode checks against.
    fn read(&self, offset: usize) -> i32 {
        let bytes: [u8; 4] = match offset < Self::EDEN_END as usize {
            true => self.eden[offset - Self::NULL_PAGE..][..4].try_into().expect("four bytes"),
            false => self.other[offset..][..4].try_into().expect("four bytes"),
        };
        i32::from_le_bytes(bytes)
    }

    /// Lays out an `int[]` of `values` at `offset`: `[class_id | mark | length | elements…]`.
    fn array(&mut self, offset: usize, values: &[i32]) {
        self.write(offset, 0x5a5a_5a5a); // a class id, never read by compiled code
        self.write(offset + 8, values.len() as i32);
        for (i, &v) in values.iter().enumerate() {
            self.write(offset + 12 + 4 * i, v);
        }
    }
}

/// Compiles a program that reads the heap: `field` is the byte offset every `getfield` resolves to.
fn compile_heap(
    code: &[u8],
    max_locals: usize,
    descriptor: &str,
    heap: Heap,
    field: u32,
) -> Result<CompiledCode, Ineligible> {
    compile_instance(code, max_locals, descriptor, true, heap, field)
}

/// [`compile_heap`] that can also compile an **instance** method, i.e. one whose slot 0 is `this`.
fn compile_instance(
    code: &[u8],
    max_locals: usize,
    descriptor: &str,
    is_static: bool,
    heap: Heap,
    field: u32,
) -> Result<CompiledCode, Ineligible> {
    super::compile::compile(
        &Method { unit: 0, code, max_locals, descriptor, is_static, has_handlers: false },
        &Environment {
            int_const: &|_, _| None,
            static_int: &|_, _| None,
            int_field: &|_, _, _| Some(field),
            instance: &|_, _| None,
            invoke: &|_, _, _| None,
            heap,
            poll_word: &POLL as *const _ as usize,
        },
    )
}

#[test]
fn a_reference_travels_through_a_local_and_back_out() {
    // `aload_0; areturn` — the whole of step 5's new exit in two bytes. The offset goes in as a
    // local and comes back as the low 32 bits of the protocol word, zero-extended: a heap offset
    // is never negative, and this is the test that would fail if it were sign-extended.
    let heap = FakeHeap::new();
    let code = [ALOAD_0, ARETURN];
    let compiled =
        compile_heap(&code, 1, "(Ljava/lang/Object;)Ljava/lang/Object;", heap.bases(), 0).unwrap();
    assert!(compiled.returns_reference, "the descriptor says a reference comes back");
    for offset in [0, 8, 264, 4000] {
        assert_eq!(call_at(&compiled, &[offset], 0).0, Outcome::Returned(offset));
    }
    // ...and `aconst_null; areturn` is the same thing with the offset 0, which is `null`.
    let null =
        compile_heap(&[ACONST_NULL, ARETURN], 0, "()Ljava/lang/String;", heap.bases(), 0).unwrap();
    assert_eq!(call_at(&null, &[], 0).0, Outcome::Returned(0));
}

#[test]
fn getfield_reads_the_right_four_bytes_in_both_halves_of_the_heap() {
    // The same compiled code, run against a receiver in **Eden** and one in Old — the two arms of
    // the address computation, which a compiler that knew about only one base would fail on
    // whichever it did not know about.
    let mut heap = FakeHeap::new();
    heap.write(16 + 12, -5); // an Eden object at 16, field at +12
    heap.write(16 + 16, 999_999); // ...and its neighbour, which must never be the answer
    heap.write(1000 + 12, 77); // an Old object at 1000, same field offset
    heap.write(1000 + 16, 999_999);

    let code = [ALOAD_0, GETFIELD, 0x00, 0x01, IRETURN];
    let compiled = compile_heap(&code, 1, "(LCell;)I", heap.bases(), 12).unwrap();
    assert_eq!(call_at(&compiled, &[16], 0).0, Outcome::Returned(-5), "the Eden arm");
    assert_eq!(call_at(&compiled, &[1000], 0).0, Outcome::Returned(77), "the other arm");
    // A `null` receiver gives up rather than faulting: the interpreter re-runs and throws.
    assert_eq!(call_at(&compiled, &[0], 0).0, Outcome::Deopt(1), "at the `getfield`, unexecuted");
}

#[test]
fn a_field_is_read_as_a_sign_extended_int_not_as_eight_bytes() {
    // Two adjacent fields, the upper one deliberately non-zero: a 64-bit load would drag it in.
    // And the value itself is negative, so the load has to *sign*-extend — the normalisation
    // invariant every arithmetic opcode assumes of its inputs.
    let mut heap = FakeHeap::new();
    heap.write(1000 + 8, i32::MIN);
    heap.write(1000 + 12, -1);
    let code = [ALOAD_0, GETFIELD, 0x00, 0x01, ICONST_1, IADD, IRETURN];
    let compiled = compile_heap(&code, 1, "(LCell;)I", heap.bases(), 8).unwrap();
    // MIN + 1, computed in 64 bits from a correctly sign-extended operand.
    assert_eq!(call_at(&compiled, &[1000], 0).0, Outcome::Returned(i32::MIN + 1));
}

#[test]
fn arraylength_and_iaload_read_and_check_their_bounds() {
    let mut heap = FakeHeap::new();
    heap.array(24, &[10, 20, 30]); // in Eden
    heap.array(2000, &[-1, i32::MIN, 7, 0, 5]); // in Old

    let length =
        compile_heap(&[ALOAD_0, ARRAYLENGTH, IRETURN], 1, "([I)I", heap.bases(), 0).unwrap();
    assert_eq!(call_at(&length, &[24], 0).0, Outcome::Returned(3));
    assert_eq!(call_at(&length, &[2000], 0).0, Outcome::Returned(5));
    assert_eq!(call_at(&length, &[0], 0).0, Outcome::Deopt(1), "a null array deopts");

    let at =
        compile_heap(&[ALOAD_0, ILOAD_1, IALOAD, IRETURN], 2, "([II)I", heap.bases(), 0).unwrap();
    assert_eq!(call_at(&at, &[24, 0], 0).0, Outcome::Returned(10));
    assert_eq!(call_at(&at, &[24, 2], 0).0, Outcome::Returned(30));
    assert_eq!(call_at(&at, &[2000, 1], 0).0, Outcome::Returned(i32::MIN), "elements sign-extend");
    assert_eq!(call_at(&at, &[2000, 4], 0).0, Outcome::Returned(5));
    // The three ways out. A negative index is the one a single unsigned comparison would let
    // through, because the index arrives *sign*-extended into 64 bits.
    assert_eq!(call_at(&at, &[24, 3], 0).0, Outcome::Deopt(2), "one past the end");
    assert_eq!(call_at(&at, &[24, -1], 0).0, Outcome::Deopt(2), "before the start");
    assert_eq!(call_at(&at, &[24, i32::MIN], 0).0, Outcome::Deopt(2), "and the extreme of that");
    assert_eq!(call_at(&at, &[0, 0], 0).0, Outcome::Deopt(2), "a null array");
}

#[test]
fn the_reference_comparisons_are_identity_and_nothing_more() {
    let heap = FakeHeap::new();
    // `if_acmpeq +5; iconst_0; ireturn; iconst_1; ireturn` — 1 when the two are the same object.
    let same = [ALOAD_0, ALOAD_1, IF_ACMPEQ, 0x00, 0x05, ICONST_0, IRETURN, ICONST_1, IRETURN];
    let compiled =
        compile_heap(&same, 2, "(Ljava/lang/Object;Ljava/lang/Object;)I", heap.bases(), 0).unwrap();
    assert_eq!(call_at(&compiled, &[1000, 1000], 0).0, Outcome::Returned(1));
    assert_eq!(call_at(&compiled, &[1000, 1004], 0).0, Outcome::Returned(0));
    assert_eq!(
        call_at(&compiled, &[0, 0], 0).0,
        Outcome::Returned(1),
        "null is a value like any other"
    );

    // `ifnull +6` on the same shape.
    let is_null = [ALOAD_0, IFNULL, 0x00, 0x05, ICONST_0, IRETURN, ICONST_1, IRETURN];
    let compiled = compile_heap(&is_null, 1, "(Ljava/lang/Object;)I", heap.bases(), 0).unwrap();
    assert_eq!(call_at(&compiled, &[0], 0).0, Outcome::Returned(1));
    assert_eq!(call_at(&compiled, &[8], 0).0, Outcome::Returned(0));
}

#[test]
fn the_type_map_refuses_to_confuse_an_int_with_a_reference() {
    let heap = FakeHeap::new();
    // An `aload` of an `int` argument...
    let err =
        compile_heap(&[ALOAD_0, ARETURN], 1, "(I)Ljava/lang/Object;", heap.bases(), 0).unwrap_err();
    assert_eq!(err, Ineligible::WrongType { pc: 0 });
    // ...and the same in the other direction, through an *instance* method's slot 0, which is
    // `this`. This is the case that was previously caught only at run time, by a marshalling
    // failure; it is now caught before a byte is emitted.
    let err = compile_instance(&[ILOAD_0, IRETURN], 1, "()I", false, heap.bases(), 0).unwrap_err();
    assert_eq!(err, Ineligible::WrongType { pc: 0 }, "`iload_0` of `this`");
    // An `iadd` of a reference, and an `iaload` whose operands are the right way round in the
    // bytecode but the wrong way round in the map.
    let err =
        compile_heap(&[ALOAD_0, ICONST_1, IADD, IRETURN], 1, "(LA;)I", heap.bases(), 0).unwrap_err();
    assert_eq!(err, Ineligible::WrongType { pc: 2 });
    let err = compile_heap(&[ILOAD_0, ALOAD_1, IALOAD, IRETURN], 2, "(I[I)I", heap.bases(), 0)
        .unwrap_err();
    assert_eq!(err, Ineligible::WrongType { pc: 2 }, "the array must be *under* the index");
}

#[test]
fn the_exit_must_agree_with_the_descriptor() {
    let heap = FakeHeap::new();
    // `areturn` in a method that returns an `int`, and `ireturn` in one that returns a reference:
    // both are unverifiable bytecode, and both are the mistake that would hand the interpreter a
    // heap offset labelled `int`.
    let err = compile_heap(&[ALOAD_0, ARETURN], 1, "(LA;)I", heap.bases(), 0).unwrap_err();
    assert_eq!(err, Ineligible::WrongType { pc: 1 });
    let err =
        compile_heap(&[ICONST_1, IRETURN], 1, "()Ljava/lang/Object;", heap.bases(), 0).unwrap_err();
    assert_eq!(err, Ineligible::WrongType { pc: 1 });
    // A method that returns something this tier has no exit for (`void`, `long`, `double`,
    // `float`) cannot compile at all, because neither exit is legal in it.
    for descriptor in ["()V", "()J", "()D", "()F"] {
        let err = compile_heap(&[ICONST_1, IRETURN], 0, descriptor, heap.bases(), 0).unwrap_err();
        assert_eq!(err, Ineligible::WrongType { pc: 1 }, "{descriptor}");
    }
}

// ---------------------------------------------------------------------------------------------
// Step 9: the top of the lattice. A local two paths type differently is `Kind::Conflict`, which
// costs the method nothing unless somebody **reads** it.
//
// Until this step the type map merged by equality, so `javac`'s most ordinary shape — one slot
// reused for two disjoint scopes, dead at the point where the scopes meet — refused the whole
// method. The group below is the whole of the new rule, stated as programs:
//
//   * carrying a conflicted slot across a merge compiles;
//   * *reading* one does not, in any of the three ways a slot can be read;
//   * a `store` re-types it, and everything after the store is ordinary again;
//   * the operand stack gets none of this, because a resume site cannot skip an operand;
//   * a body with exception handlers gets none of it either, because this walk does not follow
//     the edges into a handler and therefore cannot prove the slot dead;
//   * and the write-back says *nothing* about a conflicted slot, which is the half of the design
//     that has to answer to the collector rather than to the verifier.
// ---------------------------------------------------------------------------------------------

const IFEQ: u8 = 0x99;

/// The program every test in this group varies: a branch that leaves slot 1 holding a **reference**
/// on one path and an **`int`** on the other, with `tail` emitted at the merge.
///
/// ```text
///  0: iload_0; ifeq -> 8      slot 1 arrives as a reference (the descriptor says so)
///  4: aload_1; astore_1       ...and is still one on this path
///  6: iconst_0; istore_1      ...but is an int on the fall-through
///  8: <tail>                  <- the merge. Slot 1 is `Conflict` from here.
/// ```
fn merge_then(tail: &[u8]) -> Vec<u8> {
    let mut code = vec![ILOAD_0, IFEQ, 0x00, 0x07, ALOAD_1, ASTORE_1, ICONST_0, ISTORE_1];
    code.extend_from_slice(tail);
    code
}

#[test]
fn a_dead_slot_two_paths_type_differently_no_longer_costs_the_method() {
    // The case this step exists for, and the one `BmField.run` is made of. Nothing reads slot 1
    // after the merge, so there is nothing for the disagreement to break: it compiles, and it runs.
    let heap = FakeHeap::new();
    let compiled =
        compile_heap(&merge_then(&[ICONST_1, IRETURN]), 2, "(ILjava/lang/Object;)I", heap.bases(), 0)
            .unwrap();
    // Both ways through the branch, and both give the same answer because the slot is dead.
    assert_eq!(call_at(&compiled, &[0, 264], 0).0, Outcome::Returned(1));
    assert_eq!(call_at(&compiled, &[1, 264], 0).0, Outcome::Returned(1));
}

#[test]
fn reading_a_conflicted_slot_is_still_refused() {
    // **The rule that turns "conflicted" into "dead".** A conflicted slot matches neither `Int` nor
    // `Reference`, so every one of the three ways to read a local refuses it — and it is *that*
    // refusal, not an assumption about `javac`, that licenses the write-back to leave the
    // interpreter's stale value in place.
    let heap = FakeHeap::new();
    for (tail, what) in [
        (vec![ILOAD_1, IRETURN], "iload"),
        (vec![IINC, 0x01, 0x01, ICONST_1, IRETURN], "iinc"),
    ] {
        let err = compile_heap(&merge_then(&tail), 2, "(ILjava/lang/Object;)I", heap.bases(), 0)
            .unwrap_err();
        assert_eq!(err, Ineligible::WrongType { pc: 8 }, "{what}");
    }
    // The reference read is the one that matters most: it is the shape where a wrong answer would
    // hand the program a pointer made of whatever the other path's `int` happened to be.
    let code = merge_then(&[ALOAD_1, ARETURN]);
    let err =
        compile_heap(&code, 2, "(ILjava/lang/Object;)Ljava/lang/Object;", heap.bases(), 0).unwrap_err();
    assert_eq!(err, Ineligible::WrongType { pc: 8 }, "aload");

    // **And the read does not have to be at the merge.** This is the check that the walk is a real
    // fixed point rather than one visit per pc: the first edge to arrive at pc 8 carries an `int`,
    // so pc 11's `iload_1` is walked once and *accepted*, and only the second edge — the one that
    // makes the slot conflict — is what must send the analysis back through the tail and refuse it.
    // Delete the re-propagation and this is the assertion that fails; the two above would not.
    let err = compile_heap(
        &merge_then(&[GOTO, 0x00, 0x03, ILOAD_1, IRETURN]),
        2,
        "(ILjava/lang/Object;)I",
        heap.bases(),
        0,
    )
    .unwrap_err();
    assert_eq!(err, Ineligible::WrongType { pc: 11 }, "a read three instructions downstream");
}

#[test]
fn a_store_re_types_a_conflicted_slot_and_the_read_after_it_is_ordinary() {
    // `Conflict` is the top of the lattice, and a `store` is what brings a slot all the way back
    // down. So the *second* scope `javac` gave this slot compiles like any other code — which is
    // what makes the feature worth having rather than merely safe.
    let heap = FakeHeap::new();
    let compiled = compile_heap(
        &merge_then(&[BIPUSH, 42, ISTORE_1, ILOAD_1, IRETURN]),
        2,
        "(ILjava/lang/Object;)I",
        heap.bases(),
        0,
    )
    .unwrap();
    assert_eq!(call_at(&compiled, &[0, 264], 0).0, Outcome::Returned(42));
    assert_eq!(call_at(&compiled, &[1, 264], 0).0, Outcome::Returned(42));
}

#[test]
fn two_paths_that_disagree_about_an_operand_are_still_refused() {
    // **The operand stack gets no lattice**, and the reason is the write-back rather than the type
    // system: a local is handed back by name, so a conflicted one can be skipped, but an operand's
    // *position is its identity* and a resume site owes the interpreter a value for every one of
    // them. It is also a shape no verifier would have passed, so refusing it costs nothing real.
    //
    //  0: iload_0; ifeq -> 8
    //  4: aload_1; goto -> 9     one path leaves a reference on the stack
    //  8: iconst_0               the other leaves an int
    //  9: pop; iconst_1; ireturn <- the merge, at depth 1 with two answers
    let heap = FakeHeap::new();
    let code = [
        ILOAD_0, IFEQ, 0x00, 0x07, // 0: ifeq -> 8
        ALOAD_1, GOTO, 0x00, 0x04, // 4: goto -> 9
        ICONST_0,  // 8
        POP, ICONST_1, IRETURN, // 9
    ];
    let err = compile_heap(&code, 2, "(ILjava/lang/Object;)I", heap.bases(), 0).unwrap_err();
    assert_eq!(err, Ineligible::TypeMismatch { pc: 9 });
}

#[test]
fn a_body_with_exception_handlers_refuses_a_conflict_rather_than_carrying_one() {
    // **The one place this analysis declines to trust the verifier.** "A conflicted slot is dead"
    // is proved by this walk covering every edge the interpreter can take out of a resume site —
    // and the edges into an exception handler are exactly the ones it does not follow (the same gap
    // that costs a handler method its OSR entries). So a body with handlers keeps step 8's answer,
    // which is the answer every method got before this step.
    let heap = FakeHeap::new();
    let code = merge_then(&[ICONST_1, IRETURN]);
    let handlers = |has_handlers| {
        super::compile::compile(
            &Method {
                unit: 0,
                code: &code,
                max_locals: 2,
                descriptor: "(ILjava/lang/Object;)I",
                is_static: true,
                has_handlers,
            },
            &Environment {
                int_const: &|_, _| None,
                static_int: &|_, _| None,
                int_field: &|_, _, _| None,
                instance: &|_, _| None,
                invoke: &|_, _, _| None,
                heap: heap.bases(),
                poll_word: &POLL as *const _ as usize,
            },
        )
    };
    assert!(handlers(false).is_ok(), "without handlers the conflict is carried");
    assert_eq!(handlers(true).unwrap_err(), Ineligible::TypeMismatch { pc: 8 });
}

#[test]
fn the_write_back_says_nothing_at_all_about_a_conflicted_slot() {
    // **The half of step 9 that answers to the collector.** `BmField`'s shape in miniature: a loop
    // whose body `astore`s a reference into a slot that arrives at the header as an `int` (every
    // non-argument slot starts as `Value::Int(0)`). The header is a resume site, so the map there
    // is what the interpreter would be handed — and for slot 1 it must say `Conflict`, which
    // `JitCache::resume_state` turns into *no write at all*: the frame keeps the `Value` it had.
    //
    // The alternative to saying `Conflict` is the one unrecoverable mistake in this milestone.
    // Calling it `Int` would hand the interpreter a heap offset labelled as an integer — a live
    // object the collector can no longer see or relocate. Calling it `Reference` would hand it a
    // pointer made of arithmetic. There is no third answer that is a value, which is why the
    // answer is silence.
    //
    //  0: iconst_0; istore_2                     n = 0
    //  2: iload_2; iconst_1; if_icmpge -> 15     <- the header, stack empty
    //  7: aload_0; astore_1                      slot 1 becomes a reference
    //  9: iinc 2, 1; goto -> 2
    // 15: iload_2; ireturn
    let heap = FakeHeap::new();
    let code = [
        ICONST_0, ISTORE_2, // 0
        ILOAD_2, ICONST_1, IF_ICMPGE, 0x00, 0x0b, // 4: if_icmpge +11 -> 15
        ALOAD_0, ASTORE_1, // 7
        IINC, 0x02, 0x01, // 9
        GOTO, 0xff, 0xf6, // 12: goto -10 -> 2
        ILOAD_2, IRETURN, // 15
    ];
    let compiled = compile_heap(&code, 3, "(Ljava/lang/Object;)I", heap.bases(), 0).unwrap();
    assert_eq!(compiled.osr_entries, vec![2], "one loop, one header");
    assert_eq!(compiled.touched_locals, vec![0, 1, 2]);
    assert_eq!(
        site_at(&compiled, 2).locals,
        vec![Kind::Reference, Kind::Conflict, Kind::Int],
        "slot 1 is an int on the way in and a reference on the back-edge: neither, and therefore silence"
    );
    // And it runs, both through the front door and entered on-stack at the header — the entry that
    // hands compiled code a slot 1 it must not read.
    assert_eq!(call_at(&compiled, &[264, 0, 0], 0).0, Outcome::Returned(1));
    assert_eq!(call_at(&compiled, &[264, 0, 0], 2).0, Outcome::Returned(1));
}

#[test]
fn a_conflict_inside_an_inlined_callee_refuses_the_compilation() {
    // **The frames inlining removed get no skip, and this is the test that says so out loud.**
    //
    // The root frame exists already, so a conflicted slot in it can be left alone. An *inlined*
    // frame does not exist at all until a deopt materialises it, so every one of its slots has to
    // be written — and `Conflict` is precisely the kind that has nothing to write. There is no
    // third answer, so the whole compilation is refused, which is the same answer step 8 gave every
    // method with a conflict anywhere in it.
    //
    // It matters that this is a **compile-time** refusal rather than a run-time one. Before step 9
    // the equivalent case was a `debug_assert!(false)` inside `JitCache::resume_state` whose
    // release behaviour was to return `None` — "the JIT declined", *after* native code had run and
    // written to the heap, which would have made the interpreter re-run the method and apply every
    // one of those writes twice. A compilation that is never installed cannot do that.
    //
    // The callee is the same program as the group above, plus a guard: an `idiv` after the merge,
    // which is a resume site at a pc where slot 2 is conflicted.
    let heap = FakeHeap::new();
    let conflicted = [
        0x1a, 0x99, 0x00, 0x05, // 0: iload_0; ifeq +5 -> 6
        ALOAD_1, 0x4d, // 4: aload_1; astore_2
        BIPUSH, 100, ILOAD_0, IDIV, // 6: bipush 100; iload_0; idiv   <- the guard
        IRETURN, // 10
    ];
    // The same callee with no branch in it at all: one resume site, no conflict, ordinary inlining.
    let plain = [BIPUSH, 100, ILOAD_0, IDIV, IRETURN];
    // `iload_0; aload_1; invokestatic <callee>; ireturn`.
    let caller = [ILOAD_0, ALOAD_1, 0xb8, 0x00, 0x01, IRETURN];

    let inlining = |callee: &[u8], callee_locals: usize| {
        super::compile::compile(
            &Method {
                unit: 0,
                code: &caller,
                max_locals: 2,
                descriptor: "(ILjava/lang/Object;)I",
                is_static: true,
                has_handlers: false,
            },
            &Environment {
                int_const: &|_, _| None,
                static_int: &|_, _| None,
                int_field: &|_, _, _| None,
                instance: &|_, _| None,
                invoke: &|_, _, _| {
                    Some(super::compile::Callee {
                        method: Method {
                            unit: 1,
                            code: callee,
                            max_locals: callee_locals,
                            descriptor: "(ILjava/lang/Object;)I",
                            is_static: true,
                            has_handlers: false,
                        },
                        arg_slots: 2,
                    })
                },
                heap: heap.bases(),
                poll_word: &POLL as *const _ as usize,
            },
        )
    };

    // The control first, so the refusal below is about the conflict and not about inlining: the
    // same caller, the same call, a callee that deopts — and it compiles and runs.
    let ok = inlining(&plain, 2).expect("a callee with a guard and no conflict inlines");
    assert_eq!(call_at(&ok, &[4, 264], 0).0, Outcome::Returned(25));
    assert!(matches!(call_at(&ok, &[0, 264], 0).0, Outcome::Deopt(_)), "a zero divisor still deopts");

    // And with the conflict, at the *site*, the compilation is refused whole.
    let err = inlining(&conflicted, 3).unwrap_err();
    assert!(matches!(err, Ineligible::Unrebuildable { .. }), "{err:?}");
}

#[test]
fn the_write_back_map_names_a_kind_for_every_touched_local_at_every_loop_header() {
    // The map that leaves the compiler. This loop carries a reference (the array) and two `int`s
    // across its back-edge, so the write-back at that header must say `Reference, Int, Int` — in
    // `touched_locals` order, which is slot order.
    //
    //  0: iconst_0; istore_1                                   acc = 0
    //  2: iconst_0; istore_2                                    i  = 0
    //  4: iload_2; aload_0; arraylength; if_icmpge -> 22        <- the header, stack empty
    // 10: iload_1; aload_0; iload_2; iaload; iadd; istore_1
    // 16: iinc 2, 1
    // 19: goto -> 4
    // 22: iload_1; ireturn
    let code = [
        ICONST_0, ISTORE_1, // 0
        ICONST_0, ISTORE_2, // 2
        ILOAD_2, ALOAD_0, ARRAYLENGTH, 0xa2, 0x00, 0x0f, // 7: if_icmpge +15 -> 22
        ILOAD_1, ALOAD_0, ILOAD_2, IALOAD, IADD, ISTORE_1, // 10
        IINC, 0x02, 0x01, // 16
        GOTO, 0xff, 0xf1, // 19: goto -15 -> 4
        ILOAD_1, IRETURN, // 22
    ];
    let mut heap = FakeHeap::new();
    heap.array(24, &[3, 4, 5]);
    let compiled = compile_heap(&code, 3, "([I)I", heap.bases(), 0).unwrap();
    assert_eq!(compiled.osr_entries, vec![4], "one loop, one header");
    assert_eq!(compiled.touched_locals, vec![0, 1, 2]);
    assert_eq!(site_at(&compiled, 4).locals, vec![Kind::Reference, Kind::Int, Kind::Int]);
    assert!(site_at(&compiled, 4).stack.is_empty(), "a loop header carries no operands");
    // The same map at the two *deopt* sites this body also has — where the operand stack is not
    // empty, and is the half a restart never had to describe. At the `arraylength` (pc 6) the
    // index is under the array; at the `iaload` (pc 13) the running total is under both.
    assert_eq!(site_at(&compiled, 6).stack, vec![Kind::Int, Kind::Reference]);
    assert_eq!(site_at(&compiled, 13).stack, vec![Kind::Int, Kind::Reference, Kind::Int]);
    assert!(!compiled.returns_reference);
    // And it runs, over an array in Eden.
    assert_eq!(call_at(&compiled, &[24, 0, 0], 0).0, Outcome::Returned(12));
}

#[test]
fn a_slot_the_map_cannot_type_is_left_alone_rather_than_written_back() {
    // Local 0 is a `long` — `Kind::Opaque`, a kind this tier has no value for. The method never
    // reads it (it could not), but it *writes* it, so it is a touched local; and at the loop
    // header, before that write can have happened, the map still calls it `Opaque`. The write-back
    // must then say nothing at all about it, because the interpreter's own `Value::Long` is the
    // current one.
    //
    // The store is *after* the loop, deliberately. A store **inside** it would make the header
    // disagree with itself — `Opaque` on the way in, `Int` across the back-edge — which since step
    // 9 joins to `Kind::Conflict` rather than refusing the method. Both are written back the same
    // way (not at all), but they are different claims and this test is about the first: `Opaque`
    // says compiled code *never wrote the slot*, which is why the `long` in it is still intact.
    //
    //  0: iload_2; ifeq -> 10                    <- header (a back-edge target, stack empty)
    //  4: iinc 2, -1; goto -> 0
    // 10: iconst_1; istore_0                     slot 0 (the `long`'s low half) becomes an int
    // 12: iload_2; ireturn
    let heap = FakeHeap::new();
    let code = [
        ILOAD_2, 0x99, 0x00, 0x09, // 0: ifeq +9 -> 10
        IINC, 0x02, 0xff, // 4: iinc 2, -1
        GOTO, 0xff, 0xf9, // 7: goto -7 -> 0
        ICONST_1, ISTORE_0, // 10
        ILOAD_2, IRETURN, // 12
    ];
    let compiled = compile_heap(&code, 3, "(JI)I", heap.bases(), 0).unwrap();
    assert_eq!(compiled.osr_entries, vec![0]);
    assert_eq!(compiled.touched_locals, vec![0, 2]);
    assert_eq!(
        site_at(&compiled, 0).locals,
        vec![Kind::Opaque, Kind::Int],
        "the `long`'s slot is unwritten here, and saying so is what protects it"
    );
}

#[test]
fn a_heap_this_tier_cannot_address_puts_every_reference_read_out_of_reach() {
    // Not a property of the method: a VM whose heap could grow past the 32 bits a reference has to
    // cross the boundary in simply gets no compiled method that returns or dereferences one. The
    // same answer covers the portable configuration, where no heap is supplied at all.
    let mut heap = FakeHeap::new().bases();
    heap.max_offset = u32::MAX as usize + 1;
    let err = compile_heap(&[ALOAD_0, ARETURN], 1, "(LA;)LA;", heap, 0).unwrap_err();
    assert_eq!(err, Ineligible::HeapOutOfReach { pc: 1 });
    let err = compile_heap(&[ALOAD_0, ARRAYLENGTH, IRETURN], 1, "([I)I", heap, 0).unwrap_err();
    assert_eq!(err, Ineligible::HeapOutOfReach { pc: 1 });
    // ...and an `aload`/`astore`/`if_acmp` program that never *dereferences* anything still
    // compiles, because none of it depends on where the heap is.
    let same = [ALOAD_0, ALOAD_1, IF_ACMPEQ, 0x00, 0x05, ICONST_0, IRETURN, ICONST_1, IRETURN];
    assert!(compile_heap(&same, 2, "(LA;LA;)I", heap, 0).is_ok());
}

#[test]
fn a_method_with_exception_handlers_still_compiles_but_is_never_entered_on_stack() {
    // The one place the type map is *not* sound is a pc the walk cannot reach: a handler is
    // entered by an exception edge, so the interpreter can arrive at a loop header having run code
    // this analysis never looked at — with, say, a reference in a slot the map calls an `int`.
    //
    // It is not hypothetical. The subset's own deopts send the interpreter back to run the method
    // itself, and *that* execution can throw and be caught right here.
    //
    // The answer is surgical rather than total: the method still compiles (an **ordinary** entry
    // starts at pc 0 with the descriptor's state, and native code throws nothing, so no handler can
    // fire while it runs), but it is offered no on-stack entry point and therefore polls nowhere.
    //
    //  0: iconst_0; istore_0
    //  2: iload_0; bipush 10; if_icmpge -> 14     <- a loop header in every other respect
    //  8: iinc 0, 1; goto -> 2
    // 14: iload_0; ireturn
    let code = [
        ICONST_0, ISTORE_0, // 0
        ILOAD_0, BIPUSH, 0x0a, 0xa2, 0x00, 0x09, // 5: if_icmpge +9 -> 14
        IINC, 0x00, 0x01, // 8
        GOTO, 0xff, 0xf7, // 11: goto -9 -> 2
        ILOAD_0, IRETURN, // 14
    ];
    let handlers = |has_handlers| {
        super::compile::compile(
            &Method { unit: 0, code: &code, max_locals: 1, descriptor: "()I", is_static: true, has_handlers },
            &Environment {
                int_const: &|_, _| None,
                static_int: &|_, _| None,
                int_field: &|_, _, _| None,
                instance: &|_, _| None,
                invoke: &|_, _, _| None,
                heap: Heap::default(),
                poll_word: &POLL as *const _ as usize,
            },
        )
        .unwrap()
    };
    assert_eq!(handlers(false).osr_entries, vec![2], "without a handler, an ordinary loop header");
    let guarded = handlers(true);
    assert!(guarded.osr_entries.is_empty(), "with one, no on-stack entry and no poll site");
    // ...and therefore no resume site either, since this body has no deopt guard in it. A method
    // with handlers that *did* would still get one: a deopt is an edge the forward analysis
    // followed, so the state at that pc is the state the map says. Only an on-stack **entry** is
    // an edge it cannot see.
    assert!(guarded.resume_sites.is_empty());
    // ...and it still computes the right answer through its ordinary entry.
    assert_eq!(call_at(&guarded, &[0], 0).0, Outcome::Returned(10));
}

// ---------------------------------------------------------------------------------------------
// Step 6: the real deopt, and the three writes it made safe.
//
// Until this step compiled code wrote nothing observable, and that was the entire argument for
// deopt-by-restart: re-running a method that has written nothing is indistinguishable from never
// having run it. Resuming instead of restarting is what let `putfield`, `iastore` and `putstatic`
// in — and it brings one new rule with it, which the tests below are mostly about:
//
//   **A deopt names an instruction that has not run.** Every guard of an instruction is emitted
//   before that instruction's first observable effect, and nothing after the effect can deopt. So
//   the interpreter resuming at that pc executes it exactly once, and the pair "native attempt +
//   interpreted resume" applies each write once in total.
//
// Both halves are checked: a guard that fires leaves the heap untouched, and a deopt *after* a
// write reports a pc past it.
// ---------------------------------------------------------------------------------------------

/// [`compile_instance`] with a working `getstatic`/`putstatic` resolver: every index resolves to
/// `address`, which the tests below point at a cell they can read back.
fn compile_static(
    code: &[u8],
    max_locals: usize,
    descriptor: &str,
    heap: Heap,
    address: usize,
) -> Result<CompiledCode, Ineligible> {
    super::compile::compile(
        &Method { unit: 0, code, max_locals, descriptor, is_static: true, has_handlers: false },
        &Environment {
            int_const: &|_, _| None,
            static_int: &|_, _| Some(address),
            int_field: &|_, _, _| Some(0),
            instance: &|_, _| None,
            invoke: &|_, _, _| None,
            heap,
            poll_word: &POLL as *const _ as usize,
        },
    )
}

#[test]
fn a_deopt_hands_back_the_operand_stack_it_was_holding() {
    // `iload_0; iload_1; iload_2; idiv; iadd; ireturn` — `a + b / c`. The division at pc 3 gives up
    // with **three values live**, and the first of them is not one of its own operands: it is the
    // `a` that the `iadd` two instructions later will need. A restart could ignore all three; a
    // resume cannot, and this is the shape the safepoint poll's empty-stack contract could never
    // describe.
    let code = [ILOAD_0, ILOAD_1, ILOAD_2, IDIV, IADD, IRETURN];
    let compiled = compile(&code, 3, &|_| None).unwrap();
    assert_eq!(compiled.stack_base, 3, "the spill starts past the three locals");
    assert_eq!(compiled.buffer_slots, 6, "...and is three slots deep");
    assert_eq!(site_at(&compiled, 3).stack, vec![Kind::Int, Kind::Int, Kind::Int]);

    assert_eq!(call_at(&compiled, &[10, 30, 3], 0).0, Outcome::Returned(20));
    let (outcome, buffer) = call_at(&compiled, &[10, 30, 0], 0);
    assert_eq!(outcome, Outcome::Deopt(3), "the pc of the `idiv`, which has not run");
    assert_eq!(spilled(&compiled, &buffer, 3), vec![10, 30, 0], "bottom-first, as the JVMS stack is");
    // The locals were already current — nothing writes them here — but the buffer must not have
    // been disturbed by the spill either, or a resume would restore the wrong ones.
    assert_eq!(&buffer[..3], &[10, 30, 0]);
}

#[test]
fn a_reference_on_the_operand_stack_comes_back_as_a_reference() {
    // The mistake that does not fail where it is made. `x[i] + y[i]` leaves **an `int` under a
    // reference under an `int`** when the second `iaload` finds its index out of range, so the
    // resume site has to say `Int, Reference, Int` — get the middle one wrong and the interpreter
    // rebuilds a heap offset as a `Value::Int`, which is a live object the collector can no longer
    // see or relocate.
    //
    //  0: aload_0; iload_2; iaload      x[i]
    //  3: aload_1; iload_2; iaload      y[i]   <- pc 5 is the guarded one
    //  6: iadd; ireturn
    let mut heap = FakeHeap::new();
    heap.array(24, &[10, 20, 30, 40]); // x, in Eden
    heap.array(2000, &[1, 2]); // y, in Old, and shorter
    let code = [ALOAD_0, ILOAD_2, IALOAD, ALOAD_1, ILOAD_2, IALOAD, IADD, IRETURN];
    let compiled = compile_instance(&code, 3, "([I[II)I", true, heap.bases(), 0).unwrap();
    assert_eq!(site_at(&compiled, 5).stack, vec![Kind::Int, Kind::Reference, Kind::Int]);
    assert_eq!(call_at(&compiled, &[24, 2000, 1], 0).0, Outcome::Returned(22));
    // Index 3 is inside `x` and past the end of `y`, so the first read succeeds and the second is
    // the one that gives up — with `x[3]` and the *reference* `y` both live.
    let (outcome, buffer) = call_at(&compiled, &[24, 2000, 3], 0);
    assert_eq!(outcome, Outcome::Deopt(5));
    assert_eq!(spilled(&compiled, &buffer, 5), vec![40, 2000, 3]);
}

#[test]
fn putstatic_writes_four_bytes_at_the_baked_in_address() {
    // A static `int` occupies four bytes in its class's mirror (the interpreter's own `putstatic`
    // writes it with `write_u32`), so an eight-byte store would take the neighbouring static with
    // it. The neighbour here is a recognisable pattern for exactly that reason.
    let cell: Box<[i32; 2]> = Box::new([0, 0x5A5A_5A5A]);
    let address = cell.as_ptr() as usize;
    // `getstatic #1; iload_0; iadd; dup; putstatic #1; ireturn` — read, add, write back, and return
    // what was written, so one program checks the load and the store against each other.
    let code = [GETSTATIC, 0x00, 0x01, ILOAD_0, IADD, DUP, PUTSTATIC, 0x00, 0x01, IRETURN];
    let compiled = compile_static(&code, 1, "(I)I", Heap::default(), address).unwrap();
    assert!(compiled.resume_sites.is_empty(), "neither static opcode can fail");
    assert_eq!(call_at(&compiled, &[7], 0).0, Outcome::Returned(7));
    assert_eq!(cell[0], 7);
    assert_eq!(call_at(&compiled, &[-9], 0).0, Outcome::Returned(-2));
    assert_eq!(cell[0], -2, "the store truncates to 32 bits, and the load sign-extends back");
    assert_eq!(cell[1], 0x5A5A_5A5A, "the neighbouring static must be untouched");
}

#[test]
fn putfield_writes_the_right_four_bytes_in_both_halves_of_the_heap() {
    // The write twin of `getfield_reads_the_right_four_bytes_in_both_halves_of_the_heap`: the same
    // two-armed address computation, the same four-byte width, and the same null check — except
    // that here the null check is load-bearing in a way it never was for a read. A `getfield` that
    // deopted after doing its work would merely be wasteful; a `putfield` that did would apply the
    // store twice.
    //
    //  0: aload_0; iload_1; putfield #1     (a static method, so slot 0 is the argument)
    //  5: iconst_0; ireturn
    let mut heap = FakeHeap::new();
    heap.write(16 + 16, 0x1234_5678); // the neighbour of the Eden object's field
    heap.write(1000 + 16, 0x1234_5678); // ...and of the Old one's
    let bases = heap.bases();
    let code = [ALOAD_0, ILOAD_1, PUTFIELD, 0x00, 0x01, ICONST_0, IRETURN];
    let compiled = compile_instance(&code, 2, "(LCell;I)I", true, bases, 12).unwrap();

    assert_eq!(call_at(&compiled, &[16, -5], 0).0, Outcome::Returned(0), "the Eden arm");
    assert_eq!(heap.read(16 + 12), -5);
    assert_eq!(call_at(&compiled, &[1000, 77], 0).0, Outcome::Returned(0), "the other arm");
    assert_eq!(heap.read(1000 + 12), 77);
    assert_eq!(heap.read(16 + 16), 0x1234_5678, "four bytes, not eight");
    assert_eq!(heap.read(1000 + 16), 0x1234_5678);

    // And the guard: a null receiver deopts **at the `putfield` itself**, which is the pc the
    // interpreter will re-execute — so nothing may have been written by then.
    let before = (heap.read(16 + 12), heap.read(1000 + 12));
    assert_eq!(call_at(&compiled, &[0, 999], 0).0, Outcome::Deopt(2));
    assert_eq!((heap.read(16 + 12), heap.read(1000 + 12)), before, "a guard writes nothing");
}

#[test]
fn iastore_checks_its_bounds_before_it_writes_anything() {
    //  0: aload_0; iload_1; iload_2; iastore
    //  4: iconst_0; ireturn
    let mut heap = FakeHeap::new();
    heap.array(24, &[10, 20, 30]); // in Eden
    heap.array(2000, &[1, 2, 3, 4, 5]); // in Old
    let bases = heap.bases();
    let code = [ALOAD_0, ILOAD_1, ILOAD_2, IASTORE, ICONST_0, IRETURN];
    let compiled = compile_instance(&code, 3, "([III)I", true, bases, 0).unwrap();

    assert_eq!(call_at(&compiled, &[24, 1, -7], 0).0, Outcome::Returned(0));
    assert_eq!(heap.read(24 + 12 + 4), -7, "element 1 of the Eden array");
    assert_eq!(heap.read(24 + 12), 10, "and its neighbours are untouched");
    assert_eq!(heap.read(24 + 12 + 8), 30);
    assert_eq!(call_at(&compiled, &[2000, 4, i32::MIN], 0).0, Outcome::Returned(0));
    assert_eq!(heap.read(2000 + 12 + 16), i32::MIN, "the last element of the Old array");
    // The array's own `length` word sits immediately before element 0 — a stride or header
    // constant that is off by one element would overwrite it, and nothing else here would notice.
    assert_eq!(heap.read(2000 + 8), 5);

    // The four guards. Each reports the pc of the `iastore`, which has not run, and each leaves
    // every element exactly as it was.
    let snapshot: Vec<i32> = (0..3).map(|k| heap.read(24 + 12 + 4 * k)).collect();
    for (locals, why) in [
        ([24, 3, 99], "one past the end"),
        ([24, -1, 99], "before the start"),
        ([24, i32::MIN, 99], "and the extreme of that"),
        ([0, 0, 99], "a null array"),
    ] {
        assert_eq!(call_at(&compiled, &locals, 0).0, Outcome::Deopt(3), "{why}");
    }
    let after: Vec<i32> = (0..3).map(|k| heap.read(24 + 12 + 4 * k)).collect();
    assert_eq!(after, snapshot, "not one of the four guards may have written an element");
}

#[test]
fn a_deopt_after_a_write_reports_a_pc_past_it() {
    // **The ordering rule, stated as a test.** `cell.a = v; return 100 / d;` — the store at pc 2 is
    // applied, and then the division at pc 8 gives up. The pc that comes back is 8, not 2: the
    // interpreter resumes *after* the write and therefore does not repeat it, which is the whole
    // reason a write may sit in front of a deopt site at all.
    //
    // Reported as an assertion about the *pc* rather than only about the heap because that is the
    // thing a future opcode could get wrong. A guard emitted after its own store would still write
    // the field once here, and be a latent double-write for whoever resumed at it.
    //
    //  0: aload_0; iload_1; putfield #1
    //  5: bipush 100; iload_2; idiv
    //  9: ireturn
    let mut heap = FakeHeap::new();
    let bases = heap.bases();
    let code = [ALOAD_0, ILOAD_1, PUTFIELD, 0x00, 0x01, BIPUSH, 100, ILOAD_2, IDIV, IRETURN];
    let compiled = compile_instance(&code, 3, "(LCell;II)I", true, bases, 12).unwrap();
    assert_eq!(
        compiled.resume_sites.iter().map(|s| s.pc).collect::<Vec<_>>(),
        vec![2, 8],
        "both the store and the division are guarded pcs, and they are different pcs"
    );

    assert_eq!(call_at(&compiled, &[1000, 42, 4], 0).0, Outcome::Returned(25));
    assert_eq!(heap.read(1000 + 12), 42);

    // Now the pair that only a resuming deopt can survive: the write lands, and *then* the method
    // gives up. One call, one write of the field — and the pc says the interpreter will pick up at
    // the division, not at the store.
    heap.write(1000 + 12, 0);
    let (outcome, buffer) = call_at(&compiled, &[1000, 7, 0], 0);
    assert_eq!(outcome, Outcome::Deopt(8), "past the `putfield`, at the `idiv`");
    assert_eq!(heap.read(1000 + 12), 7, "the store happened, exactly once");
    assert_eq!(spilled(&compiled, &buffer, 8), vec![100, 0], "the division's own two operands");
}

#[test]
fn a_loop_of_array_writes_deopts_where_it_ran_off_the_end() {
    // The shape the milestone's brief asks for by name: a loop that writes an array and gives up on
    // an index past the end. What matters is not that it deopts but *where* — the elements it did
    // write must be written once, and the interpreter must take over at the iteration native code
    // stopped in rather than at the top of the loop.
    //
    //  0: iconst_0; istore_2                                          j = 0
    //  2: iload_2                                                     <- the loop header
    //  4: if_icmpge -> 21
    //  7: aload_0; iload_2; aload_0; iload_2; iaload; iconst_1; iadd; iastore
    // 15: iinc 2, 1
    // 18: goto -> 2
    // 21: iload_2; ireturn
    let code = [
        ICONST_0, ISTORE_2, // 0
        ILOAD_2, ILOAD_1, IF_ICMPGE, 0x00, 0x11, // 2: if_icmpge +17 -> 21
        ALOAD_0, ILOAD_2, ALOAD_0, ILOAD_2, IALOAD, ICONST_1, IADD, IASTORE, // 7..14
        IINC, 0x02, 0x01, // 15
        GOTO, 0xff, 0xf0, // 18: goto -16 -> 2
        ILOAD_2, IRETURN, // 21
    ];
    let mut heap = FakeHeap::new();
    heap.array(2000, &[5, 6, 7]);
    let bases = heap.bases();
    let compiled = compile_instance(&code, 3, "([II)I", true, bases, 0).unwrap();
    assert_eq!(compiled.osr_entries, vec![2], "the loop header");

    // In range: every element moves by exactly one.
    assert_eq!(call_at(&compiled, &[2000, 3, 0], 0).0, Outcome::Returned(3));
    assert_eq!((0..3).map(|k| heap.read(2000 + 12 + 4 * k)).collect::<Vec<_>>(), vec![6, 7, 8]);

    // Past the end: the three elements move by one *each*, once, and the deopt names the read of
    // the fourth element — with `j` already at 3 in the locals buffer, which is what makes the
    // interpreter throw from the right iteration instead of re-running the first three.
    let (outcome, buffer) = call_at(&compiled, &[2000, 9, 0], 0);
    assert_eq!(outcome, Outcome::Deopt(11), "the `iaload` of the fourth element is the first guard");
    assert_eq!((0..3).map(|k| heap.read(2000 + 12 + 4 * k)).collect::<Vec<_>>(), vec![7, 8, 9]);
    assert_eq!(buffer[2], 3, "local 2 (`j`) is where native code left it");
}

// ---------------------------------------------------------------------------------------------
// Step 7: `new` — the allocation fast path, its two exits, and the log that keeps the GC informed.
// ---------------------------------------------------------------------------------------------

const NEW: u8 = 0xbb;
const RETURN: u8 = 0xb1;

/// The class every `new` below allocates: three `int` fields past the 8-byte header, so the object
/// is 20 bytes logical and the arena's 8-byte rounding bumps by 24. Both numbers matter, and the
/// tests check them apart: 20 is what the *collector* is told (it copies exactly that many bytes)
/// and 24 is what the *cursor* moves by.
const CELL: Instance = Instance { size: 20, class_id: 0xabc };

/// [`compile_instance`] for a program that allocates: every `new` resolves to `instance`, and every
/// field to `+8` — the first `int` past the header.
fn compile_alloc(
    code: &[u8],
    max_locals: usize,
    descriptor: &str,
    heap: Heap,
    instance: Instance,
) -> Result<CompiledCode, Ineligible> {
    super::compile::compile(
        &Method { unit: 0, code, max_locals, descriptor, is_static: true, has_handlers: false },
        &Environment {
            int_const: &|_, _| None,
            static_int: &|_, _| None,
            int_field: &|_, _, _| Some(8),
            instance: &|_, _| Some(instance),
            invoke: &|_, _, _| None,
            heap,
            poll_word: &POLL as *const _ as usize,
        },
    )
}

/// The `(offset, size)` records a run left in the allocation log, read out of the buffer exactly as
/// `JitCache::enter` reads them.
fn logged(compiled: &CompiledCode, buffer: &[i64]) -> Vec<(usize, usize)> {
    let base = compiled.alloc_base as usize;
    let count = buffer[base] as usize;
    (0..count).map(|r| (buffer[base + 1 + 2 * r] as usize, buffer[base + 2 + 2 * r] as usize)).collect()
}

#[test]
fn new_bumps_eden_writes_the_header_and_logs_the_object() {
    // The whole of the fast path, one clause at a time, against what
    // `objects_operations::allocate` would have produced for the same class.
    let mut heap = FakeHeap::new();
    // Eden is deliberately *dirty*: the collector recycles the arena without wiping it, so zeroing
    // is the compiled code's job and a test against a pre-zeroed buffer would prove nothing.
    heap.eden.fill(0xAB);
    let bases = heap.bases();

    // new #1; astore_1; aload_1; areturn
    let code = [NEW, 0x00, 0x01, ASTORE_1, ALOAD_1, ARETURN];
    let compiled = compile_alloc(&code, 2, "()LCell;", bases, CELL).unwrap();
    assert_eq!(compiled.alloc_records, super::compile::ALLOC_LOG_RECORDS, "the method allocates");
    assert_eq!(compiled.alloc_base, compiled.stack_base + compiled.stack_slots);

    let (outcome, buffer) = call_at(&compiled, &[0, 0], 0);
    // The first object starts at arena-local 0, i.e. the heap offset `NULL_PAGE` — and it is a
    // *reference*, which is what `areturn` hands back.
    let object = FakeHeap::NULL_PAGE;
    assert_eq!(outcome, Outcome::Returned(object as i32));
    assert!(compiled.returns_reference);
    // The cursor moved by the **rounded** stride, not by the logical size.
    assert_eq!(heap.eden_used(), 24);
    // The header is byte-for-byte the interpreter's: `class_id` in the first word, `mark` zero.
    assert_eq!(heap.read(object), CELL.class_id as i32);
    assert_eq!(heap.read(object + 4), 0, "the mark word");
    // ...and every field is at its JVMS default, out of a buffer that was full of 0xAB.
    for field in [8, 12, 16] {
        assert_eq!(heap.read(object + field), 0, "field at +{field}");
    }
    // The log the trampoline replays: one object, at its heap offset, with its **logical** size.
    assert_eq!(logged(&compiled, &buffer), vec![(object, CELL.size as usize)]);
}

#[test]
fn a_fresh_object_is_an_eden_offset_the_field_opcodes_can_use() {
    // The reference a `new` produces has to be indistinguishable from one the interpreter made —
    // which for this tier means one thing above all: `heap_address` must route it to the **Eden**
    // base. Get the null page wrong by eight and every field access lands in the other buffer.
    let mut heap = FakeHeap::new();
    heap.eden.fill(0xAB);
    let bases = heap.bases();

    // new #1; dup; bipush 42; putfield #1; getfield #1; ireturn   (the field resolves to +8)
    let code =
        [NEW, 0x00, 0x01, DUP, BIPUSH, 42, PUTFIELD, 0x00, 0x01, GETFIELD, 0x00, 0x01, IRETURN];
    let compiled = compile_alloc(&code, 0, "()I", bases, CELL).unwrap();
    let (outcome, _) = call_at(&compiled, &[], 0);
    assert_eq!(outcome, Outcome::Returned(42), "written and read back through the fresh reference");
    assert_eq!(heap.read(FakeHeap::NULL_PAGE + 8), 42, "and the bytes really are in Eden");
}

#[test]
fn a_full_eden_leaves_through_the_alloc_exit_at_the_new() {
    // Eden is 256 bytes and each object takes 24, so the eleventh allocation is the one that does
    // not fit. What happens then is *not* a deopt and *not* an exception: the method stops at the
    // `new`, which has not run, and the interpreter allocates — and may collect, with no native
    // frame anywhere on the stack. That is the whole safety argument for inline allocation.
    //
    //  0: iconst_0; istore_0                     n = 0
    //  2: iload_0; bipush 20; if_icmpge -> 18    <- the loop header
    //  8: new #1; pop
    // 12: iinc 0, 1
    // 15: goto -> 2
    // 18: iload_0; ireturn
    let code = [
        ICONST_0, ISTORE_0, // 0
        ILOAD_0, BIPUSH, 20, IF_ICMPGE, 0x00, 0x0d, // 2: +13 -> 18
        NEW, 0x00, 0x01, POP, // 8
        IINC, 0x00, 0x01, // 12
        GOTO, 0xff, 0xf3, // 15: -13 -> 2
        ILOAD_0, IRETURN, // 18
    ];
    let heap = FakeHeap::new();
    let bases = heap.bases();
    let compiled = compile_alloc(&code, 1, "()I", bases, CELL).unwrap();

    let (outcome, buffer) = call_at(&compiled, &[0], 0);
    assert_eq!(outcome, Outcome::AllocFailed(8), "at the `new`, which has not run");
    // Ten objects fit (240 of 256 bytes) and every one of them was logged.
    let records = logged(&compiled, &buffer);
    assert_eq!(records.len(), 10);
    assert_eq!(buffer[0], 10, "local 0 (`n`) counted exactly the allocations that succeeded");
    for (r, &(offset, size)) in records.iter().enumerate() {
        assert_eq!(offset, FakeHeap::NULL_PAGE + 24 * r, "object {r}");
        assert_eq!(size, CELL.size as usize);
        assert!(offset < FakeHeap::EDEN_END as usize, "every one of them is an Eden offset");
    }
    // The cursor is left **past the end** — exactly what `EdenArena::alloc` does when it fails, so
    // the interpreter, re-executing this same `new`, fails Eden too and falls to Old by its own
    // path. Anything else here would be the two allocators disagreeing about how full Eden is.
    assert!(heap.eden_used() > FakeHeap::EDEN_SIZE, "cursor at {}", heap.eden_used());
}

#[test]
fn the_allocation_log_has_a_bottom_and_reaching_it_is_an_alloc_exit() {
    // The other capacity condition, isolated from Eden's: with an object small enough that Eden
    // could hold thousands, the *log* is what fills — and it must leave the same way, at a `new`
    // that has not run, with every object it did allocate recorded.
    let mut heap = FakeHeap::new();
    heap.eden = vec![0; 8 * 4096];
    let mut bases = heap.bases();
    bases.eden_base = heap.eden.as_ptr() as usize - FakeHeap::NULL_PAGE;
    bases.eden_capacity = 8 * 4096;
    bases.eden_end = (FakeHeap::NULL_PAGE + 8 * 4096) as u32;
    bases.max_offset = FakeHeap::NULL_PAGE + 8 * 4096;

    //  0: iconst_0; istore_0
    //  2: iload_0; sipush 1000; if_icmpge -> 19
    //  9: new #1; pop
    // 13: iinc 0, 1
    // 16: goto -> 2
    // 19: iload_0; ireturn
    let code = [
        ICONST_0, ISTORE_0, //
        ILOAD_0, SIPUSH, 0x03, 0xe8, IF_ICMPGE, 0x00, 0x0d, // 2: +13 -> 19
        NEW, 0x00, 0x01, POP, // 9
        IINC, 0x00, 0x01, // 13
        GOTO, 0xff, 0xf2, // 16: -14 -> 2
        ILOAD_0, IRETURN, // 19
    ];
    let tiny = Instance { size: 8, class_id: 7 };
    let records = super::compile::ALLOC_LOG_RECORDS as usize;
    let compiled = compile_alloc(&code, 1, "()I", bases, tiny).unwrap();

    let (outcome, buffer) = call_at(&compiled, &[0], 0);
    assert_eq!(outcome, Outcome::AllocFailed(9), "the `new` that found the log full");
    assert_eq!(buffer[compiled.alloc_base as usize], records as i64);
    assert_eq!(logged(&compiled, &buffer).len(), records);
    // Nothing was reserved for the allocation that did not happen: the log check comes **first**,
    // before the bump, so a refused `new` leaves Eden exactly as it found it.
    assert_eq!(heap.eden_used(), 8 * records);
}

#[test]
fn an_allocation_this_tier_cannot_do_inline_is_refused_rather_than_escaped() {
    let heap = FakeHeap::new();
    let code = [NEW, 0x00, 0x01, ARETURN];
    // An object too big to zero with a straight run of stores.
    let big = Instance { size: 1024, class_id: 1 };
    assert_eq!(
        compile_alloc(&code, 0, "()LCell;", heap.bases(), big).unwrap_err(),
        Ineligible::AllocOutOfReach { pc: 0 }
    );
    // An object bigger than Eden itself — it could never fit, so there is no fast path to emit.
    let huge = Instance { size: 264, class_id: 1 };
    assert_eq!(
        compile_alloc(&code, 0, "()LCell;", heap.bases(), huge).unwrap_err(),
        Ineligible::AllocOutOfReach { pc: 0 }
    );
    // A VM that supplied no Eden cursor at all — the `Heap::default` posture, which is what keeps a
    // forgotten field from becoming an address of zero.
    let mut blind = heap.bases();
    blind.eden_cursor = 0;
    assert_eq!(
        compile_alloc(&code, 0, "()LCell;", blind, CELL).unwrap_err(),
        Ineligible::AllocOutOfReach { pc: 0 }
    );
    // And a class the resolver will not answer for — the class-initialisation case, which is what
    // this looks like from inside the compiler.
    let refused = super::compile::compile(
        &Method { unit: 0, code: &code, max_locals: 0, descriptor: "()LCell;", is_static: true, has_handlers: false },
        &Environment {
            int_const: &|_, _| None,
            static_int: &|_, _| None,
            int_field: &|_, _, _| None,
            instance: &|_, _| None,
            invoke: &|_, _, _| None,
            heap: heap.bases(),
            poll_word: &POLL as *const _ as usize,
        },
    );
    assert_eq!(refused.unwrap_err(), Ineligible::UnresolvedClass { pc: 0, index: 1 });
}

#[test]
fn a_void_method_with_an_allocation_returns_nothing_and_still_logs() {
    // Step 7's two halves in one method, which is the shape a constructor has: it allocates, it
    // writes, and it hands nothing back.
    let heap = FakeHeap::new();
    let code = [NEW, 0x00, 0x01, DUP, BIPUSH, 9, PUTFIELD, 0x00, 0x01, POP, RETURN];
    let compiled = compile_alloc(&code, 0, "()V", heap.bases(), CELL).unwrap();
    assert!(compiled.returns_void);
    let (outcome, buffer) = call_at(&compiled, &[], 0);
    // `Status::OK`, and the value half is meaningless — the caller pushes nothing.
    assert!(matches!(outcome, Outcome::Returned(_)));
    assert_eq!(logged(&compiled, &buffer), vec![(FakeHeap::NULL_PAGE, CELL.size as usize)]);
    assert_eq!(heap.read(FakeHeap::NULL_PAGE + 8), 9);
}

// =============================================================================================
// Step 10: the operand stack in registers.
//
// The allocator is a **total function of an operand's position** — native slot `s` lives in
// `CACHE[s]` when `s < regs`, and in its frame slot otherwise. So there is no register state for two
// paths to disagree about and nothing to spill at a branch; what has to be checked instead is the
// *boundary*, where one operand of an instruction is a register and the other is memory, and where
// `idiv`'s implicit RDX:RAX and the shifts' CL meet a cached operand.
//
// Almost every test here therefore runs its program at **every** cache size, from 0 — which emits
// byte-for-byte what step 9 emitted — to the whole cache. Sweeping the size is the same thing as
// sweeping the boundary through the program, which is what a single fixed size would miss, and it
// is also what makes each of these a differential test between the two arms rather than a
// self-consistency check.
// =============================================================================================

use super::compile::{compile_with_regs, CACHE_REGS};

/// Every cache size the compiler will emit, "off" included.
fn every_cache_size() -> impl Iterator<Item = u32> {
    0..=CACHE_REGS
}

/// [`compile`] at a chosen cache size, against [`POLL`] and no heap.
fn compile_at(
    code: &[u8],
    max_locals: usize,
    regs: u32,
) -> Result<CompiledCode, Ineligible> {
    compile_with_regs(
        &Method { unit: 0, code, max_locals, descriptor: "()I", is_static: true, has_handlers: false },
        &Environment {
            int_const: &|_, _| None,
            static_int: &|_, _| None,
            int_field: &|_, _, _| None,
            instance: &|_, _| None,
            invoke: &|_, _, _| None,
            heap: Heap::default(),
            poll_word: &POLL as *const _ as usize,
        },
        regs,
    )
}

/// Runs `code` at **every** cache size and asserts all of them agree, then hands back the answer.
/// `why` names the program, because "Some(3) != Some(4)" says nothing about which of nine
/// compilations was the odd one.
fn agrees_at_every_cache_size(why: &str, code: &[u8], locals: &[i32]) -> Option<i32> {
    let mut first = None;
    for regs in every_cache_size() {
        let compiled = compile_at(code, locals.len(), regs)
            .unwrap_or_else(|e| panic!("{why}: refused at regs={regs}: {e}"));
        let answer = call(&compiled, locals);
        match regs {
            0 => first = Some(answer),
            _ => assert_eq!(Some(answer), first, "{why}: regs={regs} disagrees with regs=0, locals {locals:?}"),
        }
    }
    first.expect("the sweep always starts at regs=0")
}

/// `iload 0; iload 1; …; iload n-1` — one push per local, so the stack ends `n` deep and a long
/// enough program has positions past the end of the cache.
fn push_locals(n: usize) -> Vec<u8> {
    (0..n).flat_map(|i| [0x15u8, i as u8]).collect()
}

/// `push_locals(n); <ops>; ireturn` — the right-associated fold a parenthesised Java expression
/// compiles to, and the only shape that makes the operand stack deep.
fn fold_program(n: usize, ops: &[u8]) -> Vec<u8> {
    let mut code = push_locals(n);
    code.extend_from_slice(ops);
    code.push(IRETURN);
    code
}

/// **The oracle for [`fold_program`]**: the same stack machine written out here, with Java's `int`
/// semantics spelled out — wrapping arithmetic, a shift count masked to five bits, a division that
/// truncates toward zero. `None` where the JLS would throw.
///
/// A second implementation on purpose. An expectation taken from the thing under test cannot fail.
fn fold_expected(values: &[i32], ops: &[u8]) -> Option<i32> {
    let mut stack: Vec<i32> = values.to_vec();
    for &op in ops {
        let (rhs, lhs) = (stack.pop()?, stack.pop()?);
        stack.push(match op {
            IADD => lhs.wrapping_add(rhs),
            ISUB => lhs.wrapping_sub(rhs),
            IMUL => lhs.wrapping_mul(rhs),
            IDIV | IREM if rhs == 0 => return None,
            IDIV => lhs.wrapping_div(rhs),
            IREM => lhs.wrapping_rem(rhs),
            ISHL => ((lhs as u32) << (rhs as u32 & 31)) as i32,
            ISHR => lhs >> (rhs as u32 & 31),
            IUSHR => ((lhs as u32) >> (rhs as u32 & 31)) as i32,
            IAND => lhs & rhs,
            IOR => lhs | rhs,
            IXOR => lhs ^ rhs,
            other => panic!("fold_expected has no rule for opcode 0x{other:02x}"),
        });
    }
    stack.pop()
}

#[test]
fn an_expression_deeper_than_the_cache_computes_the_same_thing_at_every_size() {
    // Twelve operands, four past the eight registers, folded with a **non-commutative** operation:
    // `isub` is what turns a mis-mapped position into a wrong number rather than the same number
    // reached differently. The values are distinct, so exchanging any two of them shows.
    let values: Vec<i32> = (0..12).map(|k| (1 << k) + k).collect();
    for depth in 2..=12usize {
        let vals = &values[..depth];
        let ops = vec![ISUB; depth - 1];
        let code = fold_program(depth, &ops);
        let why = format!("isub fold of depth {depth}");
        assert_eq!(agrees_at_every_cache_size(&why, &code, vals), fold_expected(vals, &ops), "{why}");
    }
}

#[test]
fn the_fixed_register_instructions_meet_the_cache_at_every_boundary() {
    // `idiv`/`irem` consume and clobber RDX:RAX implicitly and the shifts read CL — the three
    // registers a cache would otherwise have to move an operand out of, per site, with the deopt
    // state to match. None of them is in `CACHE`, so the claim is that **nothing has to happen**,
    // and this is the program that would notice if something did.
    //
    // The fold puts the fixed-register instruction at the deepest point and then folds all the way
    // back down, so every operand below it stays live across it. Sweeping the depth *and* the cache
    // size walks the instruction's own two operands through "both cached", "cached over spilled"
    // and "both spilled", and walks the boundary through the operands underneath it as well.
    let values: Vec<i32> = (0..10).map(|k| 3 * k + 7).collect();
    for &fixed in &[IDIV, IREM, ISHL, ISHR, IUSHR] {
        for depth in 2..=10usize {
            let vals = &values[..depth];
            let mut ops = vec![fixed];
            ops.extend(vec![ISUB; depth - 2]);
            let code = fold_program(depth, &ops);
            let why = format!("0x{fixed:02x} at depth {depth}");
            assert_eq!(agrees_at_every_cache_size(&why, &code, vals), fold_expected(vals, &ops), "{why}");
        }
    }
}

#[test]
fn the_whole_mixed_arithmetic_subset_agrees_at_every_cache_size() {
    // One program using every binary opcode in the subset over a stack that starts ten deep, so
    // each operation happens at a different position and no two of them meet the boundary the same
    // way. Four seeds, including one that makes the intermediate values wrap.
    let ops = [IDIV, IADD, ISHL, IXOR, IMUL, IUSHR, IOR, IREM, IAND];
    for seed in [1i32, -7, 1_000_003, i32::MIN / 3] {
        let values: Vec<i32> =
            (0..10).map(|k: i32| seed.wrapping_mul(k + 1).wrapping_add(k * k + 1)).collect();
        let code = fold_program(values.len(), &ops);
        let why = format!("mixed fold, seed {seed}");
        assert_eq!(agrees_at_every_cache_size(&why, &code, &values), fold_expected(&values, &ops), "{why}");
    }
}

#[test]
fn a_deopt_spills_the_registers_that_belong_to_its_pc() {
    // **The delicate half of the step.** A stub has to materialise the operand stack of the pc it
    // fires at, and since step 10 most of those operands are in R8-R15 rather than in frame slots.
    // The values are all distinct, so a spill that named the wrong register — or the right registers
    // in the wrong order — is a different vector rather than a permutation of the same one.
    //
    // Ten operands are live at the division, so at every cache size some are registers and some are
    // slots; at `regs = 8` the boundary falls between positions 7 and 8.
    let values: Vec<i32> = (1..=10).map(|k| 1000 + k).collect();
    let mut zeroed = values.clone();
    *zeroed.last_mut().unwrap() = 0; // the divisor `idiv` pops first
    let ops = [IDIV, ISUB, ISUB, ISUB, ISUB, ISUB, ISUB, ISUB, ISUB];
    let code = fold_program(10, &ops);
    let div_pc = 20u32; // past ten two-byte `iload`s
    let want: Vec<i64> = zeroed.iter().map(|&v| v as i64).collect();

    for regs in every_cache_size() {
        let compiled = compile_at(&code, 10, regs).unwrap();
        assert_eq!(site_at(&compiled, div_pc).stack.len(), 10, "regs={regs}: ten operands are live");
        // The good case first, so the deopt below is about the guard and not about the program.
        assert_eq!(call(&compiled, &values), fold_expected(&values, &ops), "regs={regs}");
        let (outcome, buffer) = call_at(&compiled, &zeroed, 0);
        assert_eq!(outcome, Outcome::Deopt(div_pc), "regs={regs}: at the `idiv`, which has not run");
        assert_eq!(
            spilled(&compiled, &buffer, div_pc),
            want,
            "regs={regs}: the spilled stack must be the stack the instruction was entered with"
        );
        assert_eq!(&buffer[..10], &want[..], "regs={regs}: the locals were not disturbed by the spill");
    }
}

#[test]
fn a_guard_never_writes_a_home_register_before_it_can_still_fire() {
    // **The order rule in its register form**, and the one place getting it wrong would be worse
    // than a wrong number. `iaload` turns the array reference into a machine address and *then*
    // checks the index — two more `jcc`s after the reference has been consumed — so a guard phase
    // that computed into the operand's own register would leave the stub spilling an address where
    // a heap offset belongs. The interpreter would rebuild that as a `Value::Reference`, which is a
    // pointer made of arithmetic.
    //
    // The array is at position 1 and the index at position 2, with an operand underneath, at every
    // cache size.
    let mut heap = FakeHeap::new();
    heap.array(24, &[10, 20, 30, 40]);
    let code = [ILOAD_2, ALOAD_0, ILOAD_1, IALOAD, IADD, IRETURN];
    for regs in every_cache_size() {
        let compiled = compile_with_regs(
            &Method {
                unit: 0,
                code: &code,
                max_locals: 3,
                descriptor: "([III)I",
                is_static: true,
                has_handlers: false,
            },
            &Environment {
                int_const: &|_, _| None,
                static_int: &|_, _| None,
                int_field: &|_, _, _| None,
                instance: &|_, _| None,
                invoke: &|_, _, _| None,
                heap: heap.bases(),
                poll_word: &POLL as *const _ as usize,
            },
            regs,
        )
        .unwrap();
        assert_eq!(call_at(&compiled, &[24, 2, 5], 0).0, Outcome::Returned(35), "regs={regs}");
        let (outcome, buffer) = call_at(&compiled, &[24, 9, 5], 0);
        assert_eq!(outcome, Outcome::Deopt(3), "regs={regs}: at the `iaload`");
        assert_eq!(
            spilled(&compiled, &buffer, 3),
            vec![5, 24, 9],
            "regs={regs}: the array comes back as the heap offset it was, not as an address"
        );
    }
}

#[test]
fn every_heap_opcode_agrees_at_every_cache_size_and_every_depth() {
    // **The test a sabotage asked for.** The heap opcodes are the ones that compute an *address*
    // into scratch and then need their value operand, so which scratch register that value is
    // loaded into matters — and it only matters when the value is in a frame slot rather than in a
    // register of its own. A putfield whose value sat in RAX, over the address, would be a wild
    // four-byte store; and at the default cache size, with a shallow stack, that operand is always
    // cached and the bug is invisible.
    //
    // So this sweeps **two** axes: the cache size, and the depth of the stack the heap opcodes sit
    // on top of. Together they put every operand of every heap opcode on both sides of the
    // boundary. The padding underneath is folded back down with `isub`, so it is not enough for it
    // to survive — it has to survive in order.
    //
    // The receiver is in Eden and the array in the other half, so the two-armed address computation
    // is exercised in both directions at every depth as well.
    const OBJ: usize = 40;
    const ARR: usize = 2000;
    for pad in 0..9usize {
        let mut code: Vec<u8> = (0..pad).flat_map(|k| [BIPUSH, (k + 1) as u8]).collect();
        code.extend_from_slice(&[ALOAD_0, 0x1d, PUTFIELD, 0x00, 0x01]); // obj.f = local 3
        code.extend_from_slice(&[ALOAD_0, GETFIELD, 0x00, 0x01]); // ...and read it back
        code.extend_from_slice(&[ALOAD_1, ILOAD_2, 0x1d, IASTORE]); // xs[local 2] = local 3
        code.extend_from_slice(&[ALOAD_1, ILOAD_2, IALOAD, IADD]); // ...and read that back
        code.extend_from_slice(&[ALOAD_1, ARRAYLENGTH, IADD]);
        code.extend_from_slice(&[0x1d, PUTSTATIC, 0x00, 0x01, GETSTATIC, 0x00, 0x01, IADD]);
        code.extend(vec![ISUB; pad]);
        code.push(IRETURN);

        for regs in every_cache_size() {
            let mut heap = FakeHeap::new();
            heap.array(ARR, &[0; 6]);
            heap.write(OBJ + 12, 0x5A5A_5A5A); // the field's neighbour, never to be touched
            let cell: Box<[i32; 2]> = Box::new([0, 0x1234_5678]);
            let address = cell.as_ptr() as usize;
            let bases = heap.bases();
            let compiled = compile_with_regs(
                &Method {
                    unit: 0,
                    code: &code,
                    max_locals: 4,
                    descriptor: "(LCell;[III)I",
                    is_static: true,
                    has_handlers: false,
                },
                &Environment {
                    int_const: &|_, _| None,
                    static_int: &|_, _| Some(address),
                    int_field: &|_, _, _| Some(8),
                    instance: &|_, _| None,
                    invoke: &|_, _, _| None,
                    heap: bases,
                    poll_word: &POLL as *const _ as usize,
                },
                regs,
            )
            .unwrap_or_else(|e| panic!("pad={pad} regs={regs}: {e}"));

            let (index, value) = (3i32, -77i32);
            let why = format!("pad={pad} regs={regs}");
            // obj.f + xs[index] + xs.length + the static, all of which are `value` except the length.
            let core = 3 * value + 6;
            let expected = (1..=pad as i32).rev().fold(core, |acc, v| v.wrapping_sub(acc));
            assert_eq!(
                call_at(&compiled, &[OBJ as i32, ARR as i32, index, value], 0).0,
                Outcome::Returned(expected),
                "{why}"
            );
            // ...and the three writes really landed, four bytes wide, where they belong.
            assert_eq!(heap.read(OBJ + 8), value, "{why}: the field");
            assert_eq!(heap.read(OBJ + 12), 0x5A5A_5A5A, "{why}: four bytes, not eight");
            assert_eq!(heap.read(ARR + 12 + 4 * index as usize), value, "{why}: the array element");
            assert_eq!(heap.read(ARR + 12), 0, "{why}: the element before it is untouched");
            assert_eq!(cell[0], value, "{why}: the static");
            assert_eq!(cell[1], 0x1234_5678, "{why}: and its neighbour");
        }
    }
}

#[test]
fn a_merge_with_a_non_empty_stack_needs_no_spill_and_no_reload() {
    // Two paths into one pc with operands live across the join. The allocator has nothing to
    // reconcile — a home is a function of the position, and the depth at the merge is single-valued
    // because the scan already made it so — but "nothing to reconcile" is only true if both arms
    // really do leave the value in the same place, which is what this executes.
    //
    // The merge is buried under `pad` operands so it lands at depths 1 through 10, i.e. below, on
    // and above the boundary; and the `pad` operands are folded back down with `isub`, so their
    // order matters too.
    for pad in 0..10usize {
        let mut code = push_locals(pad);
        let (a, b) = (pad as u8, (pad + 1) as u8);
        code.extend_from_slice(&[0x15, a, IFEQ, 0x00, 0x0b]);
        code.extend_from_slice(&[0x15, a, 0x15, b, IMUL, GOTO, 0x00, 0x08]);
        code.extend_from_slice(&[0x15, a, 0x15, b, ISUB]);
        code.extend(vec![ISUB; pad]);
        code.push(IRETURN);
        for &(x, y) in &[(0i32, 7i32), (3, 7), (-4, 9)] {
            let mut locals: Vec<i32> = (0..pad as i32).map(|k| 100 + k).collect();
            locals.extend([x, y]);
            let branch = match x == 0 {
                true => x.wrapping_sub(y),
                false => x.wrapping_mul(y),
            };
            let expected =
                (0..pad as i32).map(|k| 100 + k).rev().fold(branch, |acc, v| v.wrapping_sub(acc));
            let why = format!("merge at depth {} with x={x}", pad + 1);
            assert_eq!(agrees_at_every_cache_size(&why, &code, &locals), Some(expected), "{why}");
        }
    }
}

#[test]
fn every_stack_shuffle_permutes_the_same_way_at_every_cache_size() {
    // The shuffles are permutations of *homes*, and a home is now sometimes a register. Their
    // correctness argument was always the order of the moves; this says the argument survived the
    // change of addressing mode — `dup2_x2` included, whose backup slots are its own result and can
    // therefore straddle the boundary.
    //
    // Sweeping the cache size *is* sweeping the boundary through the affected region: `dup2_x2`
    // works over six positions, so `regs = 0..=8` places the register/slot split at every one of
    // them in turn. `shuffle` drains the whole resulting stack into one number, so one comparison
    // checks the entire permutation — here against the interpreter's own implementation.
    for (values, op, depth) in [
        (&[1, 2][..], NOP, 2usize),
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
        let code = shuffle_program(values, op, depth);
        let why = format!("shuffle 0x{op:02x} on {values:?}");
        let expected = shuffle_interpreted(values, op);
        assert_eq!(agrees_at_every_cache_size(&why, &code, &[0]), Some(expected), "{why}");
    }
}

#[test]
fn the_cache_really_is_registers_and_the_off_arm_really_is_not() {
    // The test that keeps every other one in this section from being vacuous. If `regs` were
    // ignored the whole step would be a no-op and every "agrees at every cache size" assertion
    // above would pass trivially.
    //
    // The prologue is where it shows without ambiguity. `push rbp; mov rbp, rsp; push rbx` is what
    // every compilation without a loop starts with; a cached one whose stack reaches past the four
    // volatile registers then pushes `r12`-`r15` (`41 54`, `41 55`, `41 56`, `41 57`) and an
    // uncached one goes straight to `sub rsp`.
    const HEAD: [u8; 5] = [0x55, 0x48, 0x89, 0xE5, 0x53]; // push rbp; mov rbp, rsp; push rbx
    let deep = fold_program(10, &vec![ISUB; 9]);
    let off = compile_at(&deep, 10, 0).unwrap();
    let on = compile_at(&deep, 10, CACHE_REGS).unwrap();
    assert!(off.code.starts_with(&HEAD) && on.code.starts_with(&HEAD));
    assert_eq!(
        &on.code[HEAD.len()..HEAD.len() + 8],
        &[0x41, 0x54, 0x41, 0x55, 0x41, 0x56, 0x41, 0x57],
        "a ten-deep cached method saves the callee-saved half of the cache"
    );
    assert_ne!(off.code[HEAD.len()], 0x41, "with the cache off nothing extra is saved");
    // The point of the step: fewer bytes, because most of the loads and stores are gone.
    assert!(on.code.len() < off.code.len(), "{} cached vs {} in slots", on.code.len(), off.code.len());
    // A method that never reaches past the volatile half pays nothing at all for the feature.
    let shallow = compile_at(&[ILOAD_0, ILOAD_1, IADD, IRETURN], 2, CACHE_REGS).unwrap();
    assert!(shallow.code.starts_with(&HEAD));
    assert_ne!(shallow.code[HEAD.len()], 0x41, "four operands reach only R8-R11, which are volatile");
    // And whatever the cache does with the operands, the *contract* is unchanged: the frame, the
    // buffer and the resume map are about state, not about storage.
    assert_eq!(off.stack_slots, on.stack_slots);
    assert_eq!(off.buffer_slots, on.buffer_slots);
    assert_eq!(off.resume_sites, on.resume_sites);
}

#[test]
fn a_deopt_inside_an_inlined_callee_spills_both_frames_at_every_cache_size() {
    // Step 8's virtual frames meeting step 10's registers. A body's operands are addressed by their
    // **native** slot index, and [`plan`] gives each body a disjoint slice of those — so the
    // caller's registers and the callee's are disjoint too, which is what lets the callee run
    // without disturbing the caller's live stack and lets the stub walk the chain spilling each
    // frame from its own.
    //
    // The caller keeps two operands live under the invoke and the callee stacks four of its own on
    // top; sweeping the cache size moves the boundary through the callee's stack and then into the
    // caller's, which is the case that would break if the two frames shared a mapping.
    let heap = FakeHeap::new();
    //  0: iload_0  1: iload_1  2: iload_2  3: iload 3  5: invokestatic #1  8: iadd  9: iadd  10: ireturn
    let caller = [ILOAD_0, ILOAD_1, ILOAD_2, 0x15, 3, 0xb8, 0x00, 0x01, IADD, IADD, IRETURN];
    //  0: iload_0  1: iload_1  2: iload_0  3: iload_1  4: idiv  5: isub  6: iadd  7: ireturn
    let callee = [ILOAD_0, ILOAD_1, ILOAD_0, ILOAD_1, IDIV, ISUB, IADD, IRETURN];

    for regs in every_cache_size() {
        let compiled = compile_with_regs(
            &Method {
                unit: 0,
                code: &caller,
                max_locals: 4,
                descriptor: "(IIII)I",
                is_static: true,
                has_handlers: false,
            },
            &Environment {
                int_const: &|_, _| None,
                static_int: &|_, _| None,
                int_field: &|_, _, _| None,
                instance: &|_, _| None,
                invoke: &|_, _, _| Some(super::compile::Callee {
                    method: Method {
                        unit: 1,
                        code: &callee,
                        max_locals: 2,
                        descriptor: "(II)I",
                        is_static: true,
                        has_handlers: false,
                    },
                    arg_slots: 2,
                }),
                heap: heap.bases(),
                poll_word: &POLL as *const _ as usize,
            },
            regs,
        )
        .unwrap();
        // p + (q - p/q) with p = 20, q = 4 is 19, on top of the caller's 100 + 200.
        assert_eq!(call_at(&compiled, &[100, 200, 20, 4], 0).0, Outcome::Returned(319), "regs={regs}");

        // q = 0: the callee's `idiv` gives up, and the whole chain has to come back.
        let (outcome, buffer) = call_at(&compiled, &[100, 200, 20, 0], 0);
        let Outcome::Deopt(key) = outcome else { panic!("regs={regs}: expected a deopt, got {outcome:?}") };
        let site = compiled.resume_sites.iter().find(|s| s.key == key).expect("a site for the key");
        assert_eq!(site.pc, 5, "regs={regs}: the root resumes at the invoke");
        assert_eq!(site.inlined.len(), 1, "regs={regs}: one frame to rebuild");
        assert_eq!(site.stack.len(), 2, "regs={regs}: the caller's operands, its arguments removed");
        assert_eq!(
            (0..2).map(|k| buffer[compiled.stack_base as usize + k]).collect::<Vec<_>>(),
            vec![100, 200],
            "regs={regs}: the caller's stack, out of the caller's own registers"
        );
        let frame = &site.inlined[0];
        assert_eq!(frame.pc, 4, "regs={regs}: the callee resumes at its own `idiv`");
        assert_eq!(
            frame.stack.iter().map(|&(slot, _)| buffer[slot as usize]).collect::<Vec<_>>(),
            vec![20, 0, 20, 0],
            "regs={regs}: the callee's stack, out of the callee's"
        );
        assert_eq!(
            frame.locals.iter().map(|&(slot, _)| buffer[slot as usize]).collect::<Vec<_>>(),
            vec![20, 0],
            "regs={regs}: the arguments the call wrote into the callee's locals"
        );
    }
}

#[test]
fn a_loop_entered_on_stack_starts_with_an_empty_cache() {
    // Why the cache needs no transfer protocol of its own: an on-stack entry lands at a loop header,
    // and this tier only ever offers one whose operand stack is **empty**. There is no live operand
    // to hand across the boundary in either direction — at an entry or at a poll exit — so the
    // registers may hold whatever the caller left in them and the first push overwrites them.
    //
    //  0: iconst_0; istore_0                      i = 0
    //  2: iload_0; iload_1; if_icmpge -> 13       <- the header, stack empty
    //  7: iinc 0, 1     10: goto -> 2     13: iload_0; ireturn
    let code = [
        ICONST_0, ISTORE_0, //
        ILOAD_0, ILOAD_1, IF_ICMPGE, 0x00, 0x09, //
        IINC, 0x00, 0x01, //
        GOTO, 0xff, 0xf8, //
        ILOAD_0, IRETURN,
    ];
    for regs in every_cache_size() {
        let compiled = compile_at(&code, 2, regs).unwrap();
        assert_eq!(compiled.osr_entries, vec![2], "regs={regs}");
        assert!(site_at(&compiled, 2).stack.is_empty(), "regs={regs}: nothing is live at a header");
        assert_eq!(call_at(&compiled, &[0, 50], 0).0, Outcome::Returned(50), "regs={regs}: from the top");
        assert_eq!(call_at(&compiled, &[30, 50], 2).0, Outcome::Returned(50), "regs={regs}: on-stack");
    }
}
