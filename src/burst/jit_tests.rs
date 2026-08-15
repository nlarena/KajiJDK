//! **Differential tests**: real Java workloads run through the whole VM twice — once with the JIT
//! on, once with it off — asserting the same answer both times, and asserting that the "on" run
//! really did compile and enter native code.
//!
//! That second half is the part it is easy to leave out and easy to be wrong about. A differential
//! test where the treatment arm silently compiled nothing passes trivially and proves nothing, so
//! every test here reads [`JitStats`] and pins the counts.
//!
//! These sit beside the two other layers of evidence, and the three answer different questions:
//!
//! | layer | question |
//! |---|---|
//! | `x64` encoding tests | are the bytes the instructions we meant? |
//! | [`compile_tests`][super::compile_tests] | do those instructions implement the JLS? |
//! | here | does the *whole VM* compute the same thing with the JIT as without it? |
//!
//! And a fourth, which costs nothing extra: the JIT is on in `green`/`os-gil` and off in `os`, so
//! the project's existing `green ≡ os-gil ≡ os` oracle — every workload in the suite, not just
//! these three — is now also a JIT differential test.

use std::path::PathBuf;

use super::code_cache::JitStats;
use crate::jvm::class_file::ClassFile;
use crate::jvm::interpreter::bytecode_interpreter::execute_counting_with_jit;
use crate::jvm::interpreter::frame::{Frame, Value};
use crate::jvm::interpreter::metaspace::MetaspaceService;

/// Runs `Class.run()I` on the green engine with the JIT forced on or off, and hands back the
/// result, the opcode count and the JIT's counters.
///
/// The JIT is switched programmatically rather than through `JVM_JIT=0`: `cargo test` runs its
/// tests in threads of a single process, so a test that set an environment variable would be
/// setting it for every other test running at that moment.
fn run(class_file: &str, jit: bool) -> (i32, usize, JitStats) {
    run_tuned(class_file, jit, None)
}

/// [`run`] with the **operand-stack register cache** (F3 step 10) forced to a size — `None` leaves
/// it at whatever `JVM_JIT_REGS` says, which by default is all of it.
///
/// Switched programmatically for the same reason the JIT itself is: the two arms have to be the
/// same binary at the same addresses, or this machine's code-layout noise would be indistinguishable
/// from the effect, and an environment variable set from a test would leak into every other test in
/// the process.
fn run_tuned(class_file: &str, jit: bool, regs: Option<u32>) -> (i32, usize, JitStats) {
    use crate::jvm::interpreter::bytecode_interpreter::execute_counting_tuned;
    let mut metaspace = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
    let class = ClassFile::from_path(class_file).expect("load class");
    let name = class.class_name(class.this_class).unwrap().to_string();
    metaspace.add(name.clone(), class);
    let entry = metaspace.resolve_method(&name, "run", "()I").expect("run()");
    let max_locals = metaspace.max_locals(entry);
    let frame = Frame::new(entry, max_locals, Vec::new());
    let (value, steps, stats) = execute_counting_tuned(metaspace, frame, Some(jit), regs, |_| {});
    match value {
        Some(Value::Int(v)) => (v, steps, stats),
        other => panic!("expected an int result, got {other:?}"),
    }
}

/// The differential assertion itself: same workload, both arms, same answer — and the JIT arm
/// really compiled something. `expected` is what a real `java` of JDK 25 prints for the same class
/// file, so the pair is pinned to the JLS and not merely to itself.
fn differential(class_file: &str, expected: i32) -> JitStats {
    let (off, off_steps, off_stats) = run(class_file, false);
    let (on, on_steps, on_stats) = run(class_file, true);
    assert_eq!(off, expected, "{class_file}: the interpreter disagrees with the real JDK");
    assert_eq!(on, off, "{class_file}: the JIT computes something else than the interpreter");
    assert_eq!(off_stats, JitStats::default(), "{class_file}: JVM_JIT=0 must compile nothing at all");
    assert!(on_stats.compiled > 0, "{class_file}: nothing was compiled, so nothing was tested");
    assert!(on_stats.native_calls > 0, "{class_file}: compiled but never entered");
    // A compiled call is one opcode from the interpreter's point of view (the invoke) instead of
    // the callee's whole body, so the JIT arm must execute strictly fewer opcodes. This is the
    // cheapest possible proof that native code did the work rather than merely existing.
    assert!(on_steps < off_steps, "{class_file}: {on_steps} opcodes with the JIT vs {off_steps} without");
    on_stats
}

#[test]
fn every_opcode_in_the_subset_agrees_with_the_interpreter() {
    // JtOps drives every arm of the compiler: all four constant forms, both local forms, all of
    // the arithmetic, bits and shifts, every conditional branch on both operand shapes, `dup`, a
    // back-edge, and a divisor that varies. 12214432 is what `java JtOps` prints.
    let stats = differential("java/JtOps.class", 12_214_432);
    // Eight helpers, every one of them inside the subset; `run` itself is not (it is full of
    // invokes). Since F3 step 3 `run`'s own loop makes it hot from the inside, so it is scanned
    // once and refused once — where before it was never looked at at all.
    assert_eq!(stats.compiled, 8, "all eight helpers should compile");
    assert_eq!(stats.rejected, 1, "`run` is now scanned, from its back-edge, and refused");
    assert_eq!(stats.deopts, 0, "no divisor is ever zero here");
    assert_eq!(stats.unmarshallable, 0, "every local of a static int method is an Int");
}

#[test]
fn the_three_int_semantics_traps_hold_end_to_end() {
    // The same three traps `compile_tests` checks at machine-code level, asked through the whole
    // VM in Java: 32-bit wraparound, 5-bit shift counts (and `>>>` being logical), and division —
    // including `MIN_VALUE / -1` and a zero divisor. 16383 = all fourteen observations held, and
    // is what `java JtSem` prints.
    let stats = differential("java/JtSem.class", 16_383);
    // Ten helpers — and, since **step 8**, `warm` itself: the invokes that kept it out are now
    // expanded into it. The answer is unchanged, which is the whole claim; what moved is only how
    // much of the program native code does.
    assert_eq!(stats.compiled, 11, "the ten helpers, and `warm` now that its calls are inlined");
    // The two `x / 0` calls each deopt: native code refuses, the interpreter re-runs the method
    // from the start and throws ArithmeticException, and the Java `catch` sees it. That the score
    // includes those two bits is the proof that a deopt is invisible to the program.
    assert_eq!(stats.deopts, 2, "one deopt per division by zero");
}

#[test]
fn a_hot_method_with_an_inner_loop_agrees_with_the_interpreter() {
    // The measurement workload, checked for correctness so `bench_jit` can never drift from a
    // program that computes something else. 832880 is what `java JtLoop` prints.
    let stats = differential("java/JtLoop.class", 832_880);
    assert_eq!(stats.compiled, 1, "only `mix` is in the subset");
    // Before step 3 this was `3000 - THRESHOLD + 1`: the first 32 calls were interpreted while
    // the invocation counter climbed. Now `mix`'s own 300-iteration loop pushes it over the
    // threshold **during its first call**, so that call finishes in native code (entered
    // on-stack) and every later one enters at the top: 1 + 2999.
    assert_eq!(stats.native_calls, 3000);
    assert_eq!(stats.osr_entries, 1, "exactly one of them was the on-stack entry");
    assert_eq!(stats.safepoint_exits, 0, "nothing raises the poll here");
}

#[test]
fn the_benchmark_workloads_that_are_still_controls_are_unaffected() {
    // The `Bm*` workloads that are still this milestone's **zero-effect controls**, and the reason
    // each of them is one. After step 9 there are only two left, and the honest statement of where
    // they stand is:
    //
    //  - `BmInvoke`: `bmFib` calls itself, and `run` calls `bmFib`. Step 8 expands calls, but not
    //    a **recursive** one — a callee already on the inline path would expand for ever, and the
    //    cycle check refuses it by identity. So both methods stay out.
    //  - `BmArray`: every `iastore` is inside `run`, which begins `new int[1024]`. That is
    //    `newarray`, **not** `new` — step 7 compiles the object allocation and not the array one,
    //    because an array's size is a runtime value and its zeroing therefore a loop rather than a
    //    run of stores. So it stays a control, on a narrower reason than before.
    //
    // Two workloads used to be here and are not any more. Step 5 took `BmVirtual` (its three `f`
    // overrides are `aload_0; getfield; …; ireturn`), and **step 9 took `BmField`** — the last one
    // the JIT had never been able to touch at all. See
    // [`bmvirtual_is_the_workload_step_5_took_from_the_controls`] and
    // [`bmfield_is_the_workload_step_9_took_from_the_controls`].
    //
    // Pinning it here means the measurement table cannot quietly become a comparison of two
    // identical runs: if a future change makes any of these compile, this test says so first.
    for (class_file, expected) in [("java/BmInvoke.class", 252_624), ("java/BmArray.class", 615_180)] {
        let (off, off_steps, _) = run(class_file, false);
        let (on, on_steps, stats) = run(class_file, true);
        assert_eq!(off, expected, "{class_file}");
        assert_eq!(on, expected, "{class_file}");
        assert_eq!(on_steps, off_steps, "{class_file}: a control must execute the same opcodes");
        assert_eq!(stats.compiled, 0, "{class_file} must compile nothing (it is a control)");
        assert_eq!(stats.native_calls, 0, "{class_file}");
    }

}

#[test]
fn bmfield_is_the_workload_step_9_took_from_the_controls() {
    // **The last `Bm*` workload the JIT had never touched**, and it took four steps to get to it.
    //
    // Each one removed a different blocker and none of them was enough on its own. Step 6 made the
    // `putfield`s in its inner loop safe to compile; step 7 made the `new` inline; step 8 made the
    // `invokespecial BmCell.<init>` expandable. And after all three, `run` was still refused — at
    // its **outer loop header** (pc 4), with `Ineligible::TypeMismatch`, one instruction before any
    // of that mattered. Local 2 holds the `BmCell`, and the header is reached with an `int` in that
    // slot on the way in and a reference across the back-edge: `javac`'s ordinary "the slot is dead
    // here" shape, which had nothing to do with fields, allocation or calls. Step 9 is the step
    // that joins those two into `Kind::Conflict` instead of giving up, and this is the test that
    // says so.
    //
    // It has therefore **stopped being a control**, which is a change to the measurement harness
    // and not only to the compiler: `bench_jit` lists it as a treatment now, and its arms are no
    // longer two runs of the same work. 973376 is what `java BmField` prints.
    let stats = differential("java/BmField.class", 973_376);
    // `run`, plus `java.lang.Object.<init>` — the bare `return` step 7 put in the subset, which
    // every `new BmCell(...)` reaches. `BmCell.<init>` is expanded into `run` rather than compiled
    // on its own: once `run` is native, nothing interpreted calls it any more.
    assert_eq!(stats.compiled, 2, "`run` itself, and `java.lang.Object.<init>`");
    // `run` is entered once and loops 800 times inside it, so the only way in is **on-stack** —
    // the same shape as `BmLoop`, and the reason the back-edge counter had to exist first.
    assert!(stats.osr_entries >= 1, "`run` is entered in the middle of its outer loop");
    assert_eq!(stats.deopts, 0, "nothing here divides by zero or dereferences null");
    // The workload allocates 800 objects, so Eden fills repeatedly and native code hands the method
    // back each time it does. Those are `alloc_exits`, not deopts: a capacity condition that clears
    // on its own, which is exactly why the two are counted apart.
    assert!(stats.alloc_exits > 0, "800 allocations do fill Eden");
}

#[test]
fn bmloop_was_the_workload_this_step_existed_for() {
    // `BmLoop` used to be a control *by accident of the trigger*: its body is the ideal shape for
    // this JIT, but `run()` is entered exactly once and loops 900 000 times inside it, which an
    // invocation counter can never see. Step 3's back-edge counter sees it, and OSR gets into it.
    //
    // The assertions are the ones the measurement needs before any timing means anything: it
    // compiles, it is entered **on-stack** (not through a call — there is no call), and the whole
    // 900 000-iteration loop then runs natively, which shows up as the interpreter executing three
    // orders of magnitude fewer opcodes for the same answer.
    let stats = differential("java/BmLoop.class", 161_265);
    assert_eq!(stats.compiled, 1, "`run` itself, and there is nothing else in the file");
    assert_eq!(stats.osr_entries, 1, "entered once, in the middle, and never left");
    assert_eq!(stats.native_calls, 1);
    assert_eq!(stats.deopts, 0);
    assert_eq!(stats.safepoint_exits, 0);

    let (_, off_steps, _) = run("java/BmLoop.class", false);
    let (_, on_steps, _) = run("java/BmLoop.class", true);
    assert!(off_steps > 17_000_000, "the interpreted arm really does run the whole loop");
    assert!(on_steps < 1_000, "{on_steps} opcodes: the loop is not being interpreted at all");
}

// =============================================================================================
// Step 3: on-stack replacement and the safepoint poll, through the whole VM.
// =============================================================================================

#[test]
fn every_shape_of_loop_agrees_with_the_interpreter() {
    // `OsJit` is step 3's coverage file: a loop entered once and iterated 300 000 times, a loop
    // left from the middle, a loop that never runs a single iteration, two- and three-deep nests,
    // a loop that cannot be compiled at all, and a loop whose body deopts halfway. 4706660 is what
    // `java OsJit` prints.
    let stats = differential("java/OsJit.class", 4_706_660);
    // `longLoop`, `earlyExit`, `nested`, `triple`, `divLoop`. Not `neverEnters` (its back-edge is
    // never taken and it is called once, so nothing ever counts it hot), not `uncompilable`, not
    // `deopting` or `run` (no loop, one call each).
    assert_eq!(stats.compiled, 5);
    assert_eq!(stats.rejected, 1, "`uncompilable` is scanned once, from its back-edge, and refused");
    // Every one of the five was reached **on-stack**: not one of them is called often enough for
    // the invocation counter to matter, and three of them are called exactly once.
    assert_eq!(stats.osr_entries, 5);
    // Six native calls: those five, plus `earlyExit`'s second call, which by then enters at the
    // top like any ordinary compiled call.
    assert_eq!(stats.native_calls, 6);
    assert_eq!(stats.deopts, 1, "`divLoop` gives up at the zero divisor and the interpreter throws");
    assert_eq!(stats.safepoint_exits, 0, "nothing raises the poll here");
    assert_eq!(stats.unmarshallable, 0);
}

/// Runs `OsJit` with the JIT on and `raise` deciding what to do with this run's poll word.
fn with_poll(raise: impl FnOnce(std::sync::Arc<std::sync::atomic::AtomicU64>)) -> (i32, JitStats) {
    with_poll_on("java/OsJit.class", raise)
}

/// [`with_poll`] for any workload.
fn with_poll_on(
    class_file: &str,
    raise: impl FnOnce(std::sync::Arc<std::sync::atomic::AtomicU64>),
) -> (i32, JitStats) {
    use crate::jvm::interpreter::bytecode_interpreter::execute_counting_with_jit_and_poll;
    let mut metaspace = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
    let class = ClassFile::from_path(class_file).expect("load class");
    let name = class.class_name(class.this_class).unwrap().to_string();
    metaspace.add(name.clone(), class);
    let entry = metaspace.resolve_method(&name, "run", "()I").expect("run()");
    let max_locals = metaspace.max_locals(entry);
    let frame = Frame::new(entry, max_locals, Vec::new());
    let (value, _, stats) = execute_counting_with_jit_and_poll(metaspace, frame, Some(true), raise);
    match value {
        Some(Value::Int(v)) => (v, stats),
        other => panic!("expected an int result, got {other:?}"),
    }
}

#[test]
fn a_raised_poll_pulls_every_compiled_loop_back_into_the_interpreter() {
    // The poll held **up for the whole run**. Every on-stack entry then runs exactly one
    // iteration and leaves, reporting the loop header it stopped at; the interpreter writes the
    // locals back, resumes there, and re-enters at the next back-edge. So the program crosses the
    // boundary tens of thousands of times instead of five, and still computes the same number —
    // which is the entire claim the state transfer makes, asked in the least forgiving way
    // available.
    use std::sync::atomic::Ordering;
    let (value, stats) = with_poll(|poll| poll.store(1, Ordering::Release));
    assert_eq!(value, 4_706_660, "the answer must not depend on when the poll fires");
    assert!(stats.safepoint_exits > 10_000, "only {} exits — was the poll ever seen?", stats.safepoint_exits);
    // The counters have to add up: every on-stack entry ended one of exactly three ways — it
    // polled out, it deopted, or it ran the method to its `ireturn`. With the poll held up the
    // third can happen at most once per compiled method (the iteration that leaves the loop), so
    // the residue is a handful rather than a third of the total.
    //
    // The *number of deopts* is deliberately not pinned here. Holding the poll up makes the
    // iterations alternate between the two engines, so whether `divLoop`'s zero divisor is
    // reached by native code or by the interpreter is a property of that alternation rather than
    // of the program — and either way the answer above is the same, which is the point.
    let finished = stats.osr_entries - stats.safepoint_exits - stats.deopts;
    assert!(finished <= stats.compiled, "{finished} entries ran to an `ireturn`, with {} compiled", stats.compiled);
}

#[test]
fn the_poll_may_be_raised_and_lowered_while_the_program_runs() {
    // The same thing asked as a race: a sibling OS thread flips the poll word as fast as it can
    // for as long as the program runs, so entries, exits and full-speed loops interleave
    // arbitrarily. Nothing about the answer may depend on that interleaving.
    use std::sync::atomic::{AtomicBool, Ordering};
    use std::sync::Arc;

    let done = Arc::new(AtomicBool::new(false));
    let stop = Arc::clone(&done);
    let flipper = std::sync::Mutex::new(None);
    let (value, stats) = with_poll(|poll| {
        let handle = std::thread::spawn(move || {
            while !stop.load(Ordering::Acquire) {
                poll.store(1, Ordering::Release);
                std::thread::yield_now();
                poll.store(0, Ordering::Release);
            }
        });
        *flipper.lock().unwrap() = Some(handle);
    });
    done.store(true, Ordering::Release);
    flipper.lock().unwrap().take().expect("the flipper was started").join().expect("flipper");

    assert_eq!(value, 4_706_660);
    // Not asserted: how many exits happened. That is genuinely up to the scheduler — the point of
    // this test is the *value*, and that it survives being interrupted at unpredictable moments.
    assert!(stats.osr_entries > 0, "the run must at least have entered native code");
}

// =============================================================================================
// The measurement.
// =============================================================================================

/// One timed run: the class is loaded and resolved *outside* the timer, so what is measured is
/// execution and nothing else.
fn timed(class_file: &str, jit: bool) -> (i32, usize, JitStats, std::time::Duration) {
    let mut metaspace = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
    let class = ClassFile::from_path(class_file).expect("load class");
    let name = class.class_name(class.this_class).unwrap().to_string();
    metaspace.add(name.clone(), class);
    let entry = metaspace.resolve_method(&name, "run", "()I").expect("run()");
    let max_locals = metaspace.max_locals(entry);
    let frame = Frame::new(entry, max_locals, Vec::new());
    let start = std::time::Instant::now();
    let (value, steps, stats) = execute_counting_with_jit(metaspace, frame, Some(jit));
    let elapsed = start.elapsed();
    match value {
        Some(Value::Int(v)) => (v, steps, stats, elapsed),
        other => panic!("expected an int result, got {other:?}"),
    }
}

fn median(sorted: &[std::time::Duration]) -> std::time::Duration {
    sorted[sorted.len() / 2]
}

// The **JIT measurement**, run with:
//
//     cargo test --release --lib bench_jit -- --ignored --nocapture
//
// ------------------------------------------------------------------------------------------
// **Why this table can be read at all.** `bench_baseline`'s protocol (see `gc.rs`) exists
// because the dominant noise on this machine is *code layout*: any edit relinks the crate and
// swings a workload by ±3–12%, which is larger than most honest effects. Every rule there is a
// way of separating the change from the relayout.
//
// Here the two arms are **not two binaries**. The JIT is a runtime flag, so both arms are the
// same machine code at the same addresses in the same process — there is no relayout between
// them to separate out. That removes the largest error term outright, and it is the reason this
// measurement is worth more than the usual before/after. What remains is ordinary run-to-run
// noise, and the rest of the protocol still applies to it:
//
//  1. **Mirrored order** (off·on / on·off, alternating) so run position, thermal drift and
//     background load fall on both arms equally.
//  2. **Zero-effect controls.** `BmInvoke` compiles *nothing* (its only hot method is recursive,
//     which the inliner refuses by identity) and `BmArray` compiles nothing either (its hot method
//     begins with `newarray`). The control assertions below pin those counts from the JIT's own
//     counters rather than from belief, so "these arms do the same work" stays a checked claim.
//     Whatever ratio they show is the noise floor, and a treatment's ratio is only evidence in so
//     far as it exceeds it.
//
//     **Step 9 cost this table a control.** `BmField` was one for six steps and is now a treatment:
//     its `run` was refused at its outer loop header for a dead slot two edges typed differently,
//     and that is exactly what the type lattice's top now absorbs. Two controls is thin, so
//     `BmArray` was promoted from "also a control, for a different reason" to a pinned one.
//  3. **Median and minimum.** An interrupted run can only be slower, so the minimum is the
//     least-perturbed sample; when median and minimum disagree, the spread is the story.
//
// `BmLoop` is the headline of step 3 and it deserves its own note, because until step 3 it was a
// *control*. Its body is entirely inside the compiled subset — it is the ideal shape for this
// JIT — but `run()` is entered exactly **once** and loops 900 000 times inside it, which an
// invocation counter can never see, so nothing was compiled and its two arms were the same
// execution. With a back-edge counter and on-stack replacement it compiles, is entered in the
// middle of its loop, and finishes there. `JtLoop` is the same arithmetic and the same 900 000
// iterations re-shaped into 3 000 calls of 300, i.e. the shape the *previous* trigger could
// already see; keeping both in the table is what separates "OSR works" from "the JIT works".
#[test]
#[ignore = "benchmark: prints the JIT table, asserts no timing"]
fn bench_jit() {
    const PAIRS: usize = 7; // 1 warm-up pair (discarded) + 6 measured

    // (class file, expected value, role). `None` is a **treatment** — it must compile something.
    // `Some(n)` is a **control**: it must compile exactly `n` methods, and every one of them must
    // be incapable of moving the measurement. The count rather than a boolean is what lets a
    // workload whose *arms are not literally identical* still serve as a noise floor — a compiled
    // method that does nothing is still a control, but only if nothing else quietly joins it.
    let workloads = [
        ("java/BmLoop.class", 161_265, None),
        ("java/JtLoop.class", 832_880, None),
        // Step 5's treatment, and the one that measures something different from the other two:
        // its compiled methods are *small* (`aload_0; getfield; …; ireturn`), so what is being
        // timed is the **boundary** — marshal, call, unpack — against an interpreted body of a
        // handful of opcodes, rather than a long loop that pays the crossing once.
        ("java/BmVirtual.class", 861_237, None),
        // Step 6's treatments, written when `BmField` was still a control: the same arithmetic with
        // the inner loop hoisted into a method the trigger could see. `JdArray` pays the boundary
        // once per 1024 array writes; `JdField` once per 500 field writes, on a receiver that has
        // to be marshalled every time. They stay in the table — now that `BmField` itself compiles,
        // the three together separate "the writes are fast" from "the whole loop is native".
        ("java/JdArray.class", 649_216, None),
        ("java/JdField.class", 685_184, None),
        // **Step 9's treatment, and the headline of this step**: `BmField` was a control in every
        // table before this one. Its hot method is the only workload here that allocates inside the
        // loop being measured, so its "on" arm is also the only one whose time includes collection
        // — which is what makes it diagnostic rather than merely one more win.
        ("java/BmField.class", 973_376, None),
        // The controls, with the exact count of what each compiles. Both are zero, and both for a
        // reason the JIT states rather than one this table assumes: `BmInvoke`'s hot method is
        // recursive (the inliner refuses it by identity) and `BmArray`'s begins with `newarray`.
        ("java/BmInvoke.class", 252_624, Some(0)),
        ("java/BmArray.class", 615_180, Some(0)),
    ];

    eprintln!();
    eprintln!("F3 JIT — green, median of {} mirrored pairs (1 warm-up discarded)", PAIRS - 1);
    eprintln!(
        "{:<10} {:>9} {:>12} {:>12} {:>8} {:>10} {:>10} {:>7}  role",
        "workload", "compiled", "JIT off", "JIT on", "median", "off min", "on min", "min"
    );
    eprintln!("{}", "-".repeat(104));

    let mut control_ratios = Vec::new();
    let mut treatment_ratios = Vec::new();
    for (class_file, expected, control) in workloads {
        let short = class_file.trim_start_matches("java/").trim_end_matches(".class");
        let (mut off_times, mut on_times) = (Vec::new(), Vec::new());
        let mut compiled = 0;
        for pair in 0..PAIRS {
            // Mirrored: off·on, on·off, off·on, ... so neither arm always runs first.
            let first_is_off = pair % 2 == 0;
            let first = timed(class_file, !first_is_off);
            let second = timed(class_file, first_is_off);
            let (off_run, on_run) = if first_is_off { (first, second) } else { (second, first) };
            assert_eq!(off_run.0, expected, "{short}: wrong result with the JIT off");
            assert_eq!(on_run.0, expected, "{short}: wrong result with the JIT on");
            assert_eq!(off_run.2, JitStats::default(), "{short}: the JIT-off arm must do nothing");
            // The control assertion, from the JIT's own counters rather than from belief: a
            // control compiles exactly the methods it is declared to (none of which can move the
            // measurement), which is what makes its ratio a noise floor instead of an effect.
            match control {
                Some(n) => assert_eq!(on_run.2.compiled, n, "{short}: a control's compiled count"),
                None => assert!(on_run.2.compiled > 0, "{short}: a treatment must compile something"),
            }
            compiled = on_run.2.compiled;
            if pair > 0 {
                off_times.push(off_run.3);
                on_times.push(on_run.3);
            }
        }
        off_times.sort();
        on_times.sort();
        let (off, on) = (median(&off_times), median(&on_times));
        let (off_min, on_min) = (off_times[0], on_times[0]);
        let ratio = off.as_nanos() as f64 / on.as_nanos() as f64;
        let min_ratio = off_min.as_nanos() as f64 / on_min.as_nanos() as f64;
        match control {
            None => treatment_ratios.push((short, ratio)),
            Some(_) => control_ratios.push(ratio),
        }
        eprintln!(
            "{:<10} {:>9} {:>11.1?} {:>12.1?} {:>7.2}x {:>10.1?} {:>10.1?} {:>6.2}x  {}",
            short,
            compiled,
            off,
            on,
            ratio,
            off_min,
            on_min,
            min_ratio,
            match control {
                None => "treatment",
                Some(0) => "control (compiles nothing)",
                Some(_) => "control (compiles only no-ops)",
            }
        );
    }

    // The normalisation. A control's two arms are the *same* execution, so its ratio is pure
    // noise; dividing a treatment's ratio by the controls' median states the speedup net of
    // whatever the machine was doing while the numbers were collected.
    control_ratios.sort_by(f64::total_cmp);
    let noise = control_ratios[control_ratios.len() / 2];
    eprintln!();
    eprintln!("control noise floor (median of the controls' off/on ratio): {noise:.3}x");
    for (short, ratio) in treatment_ratios {
        eprintln!("{short} raw speedup: {ratio:.2}x — normalised: {:.2}x", ratio / noise);
    }
    eprintln!();
}

/// The **user-facing switch**, checked against the environment rather than the programmatic
/// override every other test here uses.
///
/// `#[ignore]` and run explicitly, once per setting, because its expectation *is* the environment
/// — and a test that mutated `JVM_JIT` itself would be mutating it for every other test sharing
/// the process. Both directions:
///
/// ```text
///                     cargo test --lib the_env_var -- --ignored --nocapture
///     JVM_JIT=0       cargo test --lib the_env_var -- --ignored --nocapture
/// ```
#[test]
#[ignore = "reads JVM_JIT from the environment; run it explicitly, once per setting"]
fn the_env_var_is_the_user_facing_switch() {
    let setting = std::env::var("JVM_JIT").unwrap_or_else(|_| "<unset>".to_string());
    // `None` = respect the environment, which is exactly what `execute` does for a real run.
    let (value, _, stats) = {
        let mut metaspace = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let class = ClassFile::from_path("java/JtLoop.class").expect("load class");
        let name = class.class_name(class.this_class).unwrap().to_string();
        metaspace.add(name.clone(), class);
        let entry = metaspace.resolve_method(&name, "run", "()I").expect("run()");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());
        execute_counting_with_jit(metaspace, frame, None)
    };
    eprintln!(
        "JVM_JIT={setting} -> compiled {}, native calls {}",
        stats.compiled, stats.native_calls
    );
    // Whichever way the switch is set, the program's answer is the same. That is the whole point
    // of it being a switch and not a mode.
    assert_eq!(value, Some(Value::Int(832_880)), "the result must not depend on the JIT");
    match setting.as_str() {
        "0" | "off" | "false" | "no" => {
            assert_eq!(stats, JitStats::default(), "JVM_JIT={setting} must switch the JIT off entirely");
        }
        _ => assert!(stats.compiled > 0, "the default is on"),
    }
}

// =============================================================================================
// Step 4: the opcodes that widened the subset, through the whole VM.
// =============================================================================================

#[test]
fn the_wide_prefix_agrees_with_the_interpreter() {
    // `WdWide.bump` is a `wide iinc` in both directions and at both extremes of a signed 16-bit
    // constant; `WdWide.deep` has 264 local slots and does its arithmetic in the ones past 255, so
    // every access to them is a `wide iload`/`wide istore`. -3390500 is what `java WdWide` prints.
    //
    // The failure this really guards against is not an arithmetic one: a `wide iinc` is **6** bytes
    // and a `wide iload` is 4, so a decoder that got either length wrong would resynchronise in the
    // middle of the next instruction and emit something arbitrary — which is exactly the kind of
    // bug that produces a plausible-looking wrong number rather than a crash.
    let stats = differential("java/WdWide.class", -3_390_500);
    assert_eq!(stats.compiled, 2, "`bump` and `deep`");
    assert_eq!(stats.rejected, 1, "`run` is scanned from its back-edge and refused (invokes)");
    assert_eq!(stats.unmarshallable, 0, "every one of `deep`'s 264 slots holds an Int");
    assert_eq!(stats.deopts, 0);
}

#[test]
fn both_switch_opcodes_agree_with_the_interpreter() {
    // `tableswitch` and `lookupswitch`, at several alignments (so the 0–3 padding bytes vary), with
    // negative `low`s, with `Integer.MIN_VALUE`/`MAX_VALUE` as keys, with arms that fall through,
    // with a `continue` that makes a switch arm a back-edge, and with keys that miss every case so
    // the `default` arm is taken. 301975 is what `java WdSwitch` prints.
    let stats = differential("java/WdSwitch.class", 301_975);
    assert_eq!(stats.compiled, 5, "all five helpers");
    assert_eq!(stats.rejected, 1, "`run` itself");
    assert_eq!(stats.deopts, 0);
}

#[test]
fn getstatic_of_an_int_agrees_with_the_interpreter() {
    // The first opcode in the subset that reads the heap. `WdStatic` reads statics out of three
    // different mirrors (its own class, its superclass, an unrelated class), reads one that
    // **changes between calls** (so a compiler that folded the value in would be caught), and
    // allocates hard enough between the calls to force collections — which a baked-in address must
    // survive. 246189 is what `java WdStatic` prints.
    let stats = differential("java/WdStatic.class", 246_189);
    // `own`, `inherited`, `far`, `mutable`, `mixed`. Not `notAnInt` (a `String` static, so the
    // resolver refuses it), not `churn` (an array), not `run` (invokes).
    assert_eq!(stats.compiled, 5);
    assert_eq!(stats.rejected, 3, "`notAnInt`, `churn` and `run`, each scanned once");
    assert_eq!(stats.deopts, 0);
    assert_eq!(stats.unmarshallable, 0);
}

// =============================================================================================
// Step 8: calls, by inlining.
// =============================================================================================

#[test]
fn the_whole_new_pattern_agrees_with_the_interpreter() {
    // **The shape step 8 exists for.** `javac` never emits a bare `new`, so step 7's inline
    // allocation bought nothing until a call could be expanded; `JiNew` is `new JiCell(i, i + 1)`
    // in a loop, which is that pattern complete. It needs the step's three pieces at once — an
    // `invokespecial` with a receiver and arguments, a callee (`<init>`) whose two `putfield`s can
    // **deopt**, and `<init>`'s own `super()`, which is a second level of expansion. 160000 is what
    // `java JiNew` prints.
    let stats = differential("java/JiNew.class", 160_000);
    // `run` — which no earlier step could touch — and `JiCell.<init>`, which is called often enough
    // to become hot in its own right and compiles as a root like any other method.
    assert_eq!(stats.compiled, 2, "`run` and `JiCell.<init>`");
    assert_eq!(stats.rejected, 0);
    // Nothing here fails a guard: the receiver of every `putfield` is the object the `new` two
    // instructions earlier produced, and it is never null.
    assert_eq!(stats.deopts, 0);
    // The loop allocates, so it leaves native code whenever Eden or the excursion's allocation log
    // fills and re-enters at the header — `Status::ALLOC`, which is deliberately not a deopt.
    assert!(stats.alloc_exits > 0, "an allocating loop must hand the method back sometimes");
    assert!(stats.osr_entries > 0, "`run` is entered once and is reached from its own back-edge");
}

#[test]
fn a_deopt_from_inside_an_inlined_callee_agrees_with_the_interpreter() {
    // **Virtual frames, asked end to end.** `div` is expanded into `step`, so where the interpreter
    // has two frames native code has none — and every eighth call divides by zero, which `div`
    // cannot do. The deopt therefore has to hand back *two* interpreter frames: `step` parked at
    // the invoke with its argument already gone, and `div` at the `idiv` that could not run.
    //
    // What happens next is the part that needs no new machinery, and that is the claim: the
    // interpreter re-executes the `idiv`, throws `ArithmeticException`, unwinds it through `div`
    // and `step` — neither has a handler — and `run`'s `catch` sees it. 130250 is what
    // `java JiDeopt` prints, and it counts both arms, so a frame lost or a pc misreported moves it.
    let stats = differential("java/JiDeopt.class", 130_250);
    assert_eq!(stats.compiled, 2, "`step`, with `div` inlined into it, and `div` on its own");
    // 50 of the 400 calls divide by zero; the ones before `step` got hot are interpreted.
    assert_eq!(stats.deopts, 46, "one deopt per zero divisor reached in native code");
}

#[test]
fn a_write_inside_an_inlined_callee_is_applied_exactly_once() {
    // **The order rule, across the inline boundary.** Step 6's rule — every guard before its
    // instruction's first observable effect, nothing after the effect may deopt — has to keep
    // holding when the instruction is inside an expanded callee, and this is the shape that would
    // catch it failing: `poke` writes a field and *then* meets a guard it fails every fourth call.
    //
    // A deopt must therefore report the `iaload`, which is **past** the write, so the interpreter
    // resumes after it and the field is incremented once in total. `box.v` counts every call,
    // failures included, and is folded into the answer — so re-running the write would print 500
    // pokes instead of 400 and the number would move. 50000 is what `java JiOrder` prints.
    let stats = differential("java/JiOrder.class", 59_300);
    // `step`, with `poke` expanded into it. Not `run`, which builds the array with `newarray` —
    // still outside the subset, which is what makes `step` the root of the compilation.
    assert_eq!(stats.compiled, 1, "`step`, with `poke` inlined");
    assert_eq!(stats.rejected, 1, "`run`, for its `newarray`");
    // 100 of the 400 calls index past the end; the ones before `step` got hot are interpreted.
    assert_eq!(stats.deopts, 93, "one deopt per index out of range reached in native code");
}

#[test]
fn a_rebuilt_virtual_frame_hands_references_back_as_references() {
    // **The one mistake in this milestone that does not fail where the bug is**, asked of the frames
    // inlining removed. A heap offset put back into a rebuilt frame as a `Value::Int` is a live
    // object the collector can no longer see or relocate; an `int` put back as a reference is a
    // pointer made of arithmetic.
    //
    // Making it observable needs a deopt the program **survives** — a frame that throws immediately
    // never reads its locals again, so a mistagged local nothing reads is a mistake no test can
    // see. `at` therefore catches its own exception: the deopt rebuilds its frame mid-`try`, the
    // interpreter re-executes the array read, `at`'s own handler catches, and execution carries on
    // *inside the rebuilt frame*, where it reads `c` — a reference local that was live across all
    // of it. 11900 is what `java JiRef` prints.
    //
    // It is also the answer to the other half of the handler question: an inlined callee may have
    // an exception table of its own. Native code throws nothing, so no handler can fire while it
    // runs; when one does fire it fires in an ordinary interpreter frame, which is what a deopt
    // rebuilds, and this tier needs to know nothing about it.
    let stats = differential("java/JiRef.class", 11_900);
    assert_eq!(stats.compiled, 1, "`outer`, with `at` inlined");
    assert_eq!(stats.rejected, 1, "`run`, for its `newarray`");
    assert_eq!(stats.deopts, 93, "one deopt per index out of range reached in native code");
}

#[test]
fn nested_inlining_rebuilds_every_frame_in_the_chain() {
    // **Three bodies in one compiled function**, which is the depth limit — `outer` expands
    // `middle`, which expands `inner`. A guard inside `inner` therefore hands back three frames,
    // and the one worth testing is the middle: simultaneously a callee (it was inlined) and a
    // caller (it is parked at an invoke of its own).
    //
    // Each of the three leaves an operand *below* the call's arguments — the `7`, the `100` — so
    // this is also where "a frame in the middle of a call has already given its arguments away" is
    // pinned: a rebuilt frame that kept them would push each argument twice and the arithmetic
    // after the call would read the wrong operands. `inner` catches its own exception so that
    // arithmetic actually runs. 134200 is what `java JiNest` prints.
    let stats = differential("java/JiNest.class", 134_200);
    assert_eq!(stats.compiled, 1, "`outer`, with `middle` and `inner` expanded into it");
    assert_eq!(stats.rejected, 1, "`run`, for its `newarray`");
    assert_eq!(stats.deopts, 93, "one deopt per index out of range reached in native code");
}

#[test]
fn recursion_is_cut_by_the_cycle_check_rather_than_expanded() {
    // **The bound that is not a depth bound.** A method that calls itself would inline into itself
    // without end, and the honest reason to stop is identity rather than arithmetic: a callee whose
    // `Unit` is already on the path from the root is refused. `JiRec` has both shapes — `down`
    // calling itself, and `mutualA`/`mutualB` calling each other, which no per-method check would
    // see and which is caught two expansions in.
    //
    // The assertion that matters is the one that is easy to overlook: this test **terminates**.
    // 3900 is what `java JiRec` prints.
    let stats = differential("java/JiRec.class", 3_900);
    // `sum`, a leaf that inlines nowhere and compiles as it always did. The other four are refused
    // — the three recursive ones for the cycle, and `run` because it calls them.
    assert_eq!(stats.compiled, 1, "`sum`");
    assert_eq!(stats.rejected, 4, "`down`, `mutualA`, `mutualB` and `run`");
    assert_eq!(stats.deopts, 0);
}

// =============================================================================================
// Step 9: the type lattice's top, through the whole VM.
//
// The compiler-level statement of this step is in `compile_tests`; what these add is the half that
// only a whole VM can check. A conflicted slot is one the write-back deliberately **does not
// touch**, so the interpreter frame keeps a value native code may have overwritten — and that frame
// is a GC root the instant it is interpreted again. "Nobody reads it" and "the collector survives
// it" are two different claims, and the second one needs a real heap.
// =============================================================================================

#[test]
fn a_dead_slot_that_two_edges_type_differently_no_longer_costs_the_method() {
    // `JmDead` is the three Java-expressible halves of step 9 in one file — the merge (`dead`), the
    // re-type (`retyped`), and a **deopt at a pc where a slot is conflicted** (`guarded`). Every
    // one of them is `javac`'s ordinary "an object allocated inside a loop" shape, which is why
    // this step is worth more than its census line suggests: before it, no such method compiled.
    //
    // 854257 is what `java JmDead` prints.
    let stats = differential("java/JmDead.class", 854_257);
    // `dead`, `retyped` and `guarded` — every one of which step 8 refused with
    // `Ineligible::TypeMismatch`, and none of which has anything else wrong with it — plus
    // `JmCell.<init>`, which is hot in its own right because each of the three calls it. (Not
    // `java.lang.Object.<init>`: `JmCell.<init>` expands it inline, so the interpreter stops
    // reaching it and its counter stops climbing.)
    assert_eq!(stats.compiled, 4, "the three conflicted methods, and `JmCell.<init>`");
    // `trapped` is the one refusal: it allocates its array with `newarray`, which is outside the
    // subset. `run` is entered once and has no loop, so no counter ever makes it warm enough to be
    // looked at at all.
    assert_eq!(stats.rejected, 1, "`trapped`, for its `newarray`");
    // **The deopt that matters.** `guarded` walks off the end of a 64-element array, and the bounds
    // check that stops it is emitted *before* the object is stored into the conflicted slot — so
    // the frame handed back is one in which that slot holds the *previous* iteration's object,
    // written by nobody and read by nobody. Exactly one, at exactly the 65th iteration.
    assert_eq!(stats.deopts, 1, "the out-of-range index reaches native code and gives up there");
    // And the allocation pressure is real, which is what makes this a GC test and not only a type
    // test: 65 excursions ended because Eden had no room left for the next `new JmCell`.
    assert!(stats.alloc_exits > 20, "{} alloc exits: Eden really does fill", stats.alloc_exits);
}

#[test]
fn a_conflicted_slot_does_not_change_what_the_program_computes() {
    // The same file with the JIT **on**, twice, against a run with it off: what a conflicted slot
    // must not do is change an answer. `JmDead.run` folds all three methods into one number, so a
    // write-back that guessed `Int` for `c` (a heap offset handed over as an integer) or
    // `Reference` (an integer handed over as a pointer) would show up here as a wrong sum, an
    // exception, or a collector that trips over a root it cannot follow.
    //
    // Determinism is part of the claim: the deopt in `guarded` happens at a fixed iteration, so two
    // JIT runs of the same file must agree with each other as well as with the interpreter.
    let (off, _, off_stats) = run("java/JmDead.class", false);
    let (first, _, _) = run("java/JmDead.class", true);
    let (second, _, _) = run("java/JmDead.class", true);
    assert_eq!(off_stats, JitStats::default(), "the JVM_JIT=0 arm must compile nothing");
    assert_eq!(off, 854_257);
    assert_eq!(first, off);
    assert_eq!(second, off);
}

// =============================================================================================
// Step 10: the operand stack in registers, through the whole VM.
//
// The compiler-level statement of this step is in `compile_tests`, where every program can be
// compiled at **every** cache size and the emitted state inspected directly. What these add is the
// half only a whole VM checks: that a real Java program — with a real heap, real exceptions and a
// real interpreter picking up where native code left off — computes the same number whether its
// operands lived in R8-R15 or in frame slots.
//
// Every test here runs **three** arms: the interpreter, the JIT with the cache off (`regs = 0`,
// which emits exactly what step 9 emitted), and the JIT with it on. Three rather than two, because
// two would not distinguish "the register allocator is right" from "the register allocator never
// ran" — and the third arm is also the honest control for the measurement.
// =============================================================================================

/// The three-arm assertion: interpreter, JIT with the cache off, JIT with the cache on, all equal
/// to what a real `java` of JDK 25 prints. Hands back the counters of the **cache-on** arm.
fn differential_regs(class_file: &str, expected: i32) -> JitStats {
    let (off, off_steps, off_stats) = run_tuned(class_file, false, None);
    let (slots, slot_steps, slot_stats) = run_tuned(class_file, true, Some(0));
    let (cached, cached_steps, cached_stats) = run_tuned(class_file, true, Some(super::compile::CACHE_REGS));
    assert_eq!(off, expected, "{class_file}: the interpreter disagrees with the real JDK");
    assert_eq!(off_stats, JitStats::default(), "{class_file}: the JIT-off arm must compile nothing");
    assert_eq!(slots, off, "{class_file}: JVM_JIT_REGS=0 computes something else than the interpreter");
    assert_eq!(cached, off, "{class_file}: the register cache computes something else than the interpreter");
    // The two JIT arms must not merely agree on the answer — they must have done the same *work*,
    // opcode for opcode and deopt for deopt. A register allocator that quietly refused a method, or
    // deopted where the slot version did not, would still return the right number.
    assert_eq!(slot_steps, cached_steps, "{class_file}: the two arms interpreted different amounts");
    assert_eq!(slot_stats, cached_stats, "{class_file}: the two arms have different JIT counters");
    assert!(cached_stats.compiled > 0, "{class_file}: nothing was compiled, so nothing was tested");
    assert!(cached_stats.native_calls > 0, "{class_file}: compiled but never entered");
    assert!(cached_steps < off_steps, "{class_file}: native code did none of the work");
    cached_stats
}

#[test]
fn expressions_deeper_than_the_cache_agree_with_the_interpreter() {
    // `JgDeep` is written against the *edge* of the cache rather than inside it: `deep` needs a
    // stack ten deep (two positions past the eight registers), and `mixed` puts `idiv`, `irem`,
    // `ishl` and `iushr` — the four instructions with implicit fixed registers — at exactly the
    // boundary, with the dividend in the last cached position and the divisor in the first frame
    // slot. 279403 is what `java JgDeep` prints.
    let stats = differential_regs("java/JgDeep.class", 279_403);
    assert_eq!(stats.compiled, 4, "`deep`, `mixed`, `shallow` and `run`");
    assert_eq!(stats.deopts, 0, "no divisor is ever zero here");
}

#[test]
fn a_deopt_with_the_operand_stack_in_registers_rebuilds_it_exactly() {
    // The delicate half of step 10, asked of a whole VM. `JgGuard` deopts on a zero divisor, on an
    // index past the end of an array and on a null receiver, each of them under a stack **nine**
    // deep — eight registers and one frame slot. The interpreter re-executes the guarded
    // instruction out of the spilled stack, so a spill that named the wrong register would re-read
    // a non-zero divisor or an in-range index and never throw at all, and the `catch` arms would
    // stop contributing to the score. 385935 is what `java JgGuard` prints.
    let stats = differential_regs("java/JgGuard.class", 385_935);
    // Every deopt is a guard that fired with a live operand stack, and there are hundreds of them:
    // this is not a shape that has to be argued about, it is one the run walks into repeatedly.
    assert!(stats.deopts > 500, "{} deopts: the guards really do fire", stats.deopts);
}

#[test]
fn a_deopt_inside_inlined_code_spills_every_frames_registers() {
    // Step 8's virtual frames meeting step 10's registers: `outer` inlines `inner`, `middle`
    // inlines `outer` which inlines `inner`, and the divisor is zero on some calls. Each frame's
    // operands live in that frame's own registers — the regions are disjoint, so the caller's are
    // still intact when the callee's guard fires — and the stub walks the chain spilling each.
    // 70961 is what `java JgInline` prints.
    let stats = differential_regs("java/JgInline.class", 70_961);
    assert!(stats.deopts > 500, "{} deopts out of inlined bodies", stats.deopts);
}

#[test]
fn merges_with_a_non_empty_operand_stack_need_no_reconciliation() {
    // The claim this step does *not* have to pay for: because an operand's home is a function of
    // its position and the depth at a pc is already single-valued, two arms of a ternary leave
    // their result in the same place without a spill at the branch or a reload at the join.
    // `JgEdge` puts such a merge at a cached position, at the last cached one, and one past the end
    // of the cache — plus a loop around two of them, so the shape is met on-stack as well as from
    // the top. 148321 is what `java JgEdge` prints.
    let stats = differential_regs("java/JgEdge.class", 148_321);
    assert!(stats.osr_entries > 0, "`both` is entered on-stack, with the cache live");
    assert_eq!(stats.deopts, 0, "nothing here can fail a guard");
}

// =============================================================================================
// The coverage census.
// =============================================================================================

/// **How wide is the subset?** — the metric step 4 exists to move, run with:
///
/// ```text
///     cargo test --lib subset_census -- --ignored --nocapture
/// ```
///
/// It offers every method of every class file in `java/` to [`compile`][super::compile::compile]
/// and tallies the answers. Two honesty notes about what the number means:
///
///  - The `getstatic`/`putstatic` resolver here is a **stub** that accepts any `int` static and
///    hands back a fixed address. At run time those opcodes also require the declaring class to be
///    *initialised* already, which this cannot know without a running VM — so the census is an
///    upper bound on the opcode subset rather than a prediction of what a given run compiles.
///  - Step 7 put `return` (0xb1) in the subset, so `void` methods — `<init>`, every setter, `main`
///    — are no longer structurally excluded and count towards the ceiling. What is still out is a
///    method returning a `long`, a `double` or a `float`: the packed return protocol has 32 bits
///    for a value, which is a limit of the boundary rather than of the opcode subset.
///  - Step 8 made a compilation span **more than one class file**, so the whole corpus (and
///    `boot/`, for `java.lang.Object.<init>`) is loaded before anything is offered. The `invoke`
///    stub is an upper bound in the same two ways the others are — it cannot know whether a class
///    is initialised or a method `synchronized` — and a slight *under*-count in one: it does not
///    walk the superclass chain, so an inherited target simply does not resolve.
///  - **Step 9 barely moves this number, and that is worth saying out loud.** Over this corpus the
///    dead-slot merge accounted for exactly *two* refusals, and joining them into `Kind::Conflict`
///    recovered one of them (the other then stops one step later, at an inlined callee that loops).
///    The census is a count of *methods*, and this step's value is not in the count: the one method
///    it recovered is `BmField.run`, the last of the milestone's measurement workloads the JIT had
///    never been able to touch, and its shape — an object allocated inside a loop — is the
///    commonest one in real Java there is. A corpus of small test programs simply does not contain
///    many loops that allocate.
#[test]
#[ignore = "census: prints the compiled subset's coverage over the java/ corpus"]
fn subset_census() {
    use std::collections::BTreeMap;

    static CENSUS_POLL: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(0);
    let poll = &CENSUS_POLL as *const _ as usize;
    // A heap that is *shaped* like the VM's — the layout constants matter, since they are what a
    // heap-reading opcode is compiled against — but whose bases are 1: nothing here is executed,
    // and a non-zero `max_offset` is what tells the compiler a heap exists at all.
    const CENSUS_HEAP: crate::burst::compile::Heap = crate::burst::compile::Heap {
        eden_base: 1,
        other_base: 1,
        eden_end: 264,
        max_offset: 16 * 1024 * 1024,
        // A cursor address that is *non-zero* — which is all the compiler asks, since nothing here
        // is executed — plus Eden's real shape, so a `new` is judged against the bounds it would
        // face in the VM. As with the two resolvers below, this makes the census an **upper bound**:
        // it cannot know whether a given class is initialised.
        eden_cursor: 1,
        eden_capacity: 256,
        null_page: 8,
        array_length: 8,
        int_array_data: 12,
        int_element: 4,
    };

    /// Every `.class` under `root`, recursively — `boot/` is a package tree, not a flat directory.
    fn walk(root: &std::path::Path, out: &mut Vec<std::path::PathBuf>) {
        let Ok(entries) = std::fs::read_dir(root) else { return };
        for entry in entries.filter_map(|e| e.ok()) {
            let path = entry.path();
            match path.is_dir() {
                true => walk(&path, out),
                false if path.extension().is_some_and(|e| e == "class") => out.push(path),
                false => {}
            }
        }
    }

    /// The operand-stack values a call to `descriptor` consumes, receiver included — the number the
    /// VM's `arg_count` would report. One per parameter, since this VM keeps a category-2 value in
    /// a single operand slot.
    fn census_arg_slots(descriptor: &str, receiver: bool) -> usize {
        let b = descriptor.as_bytes();
        let mut n = usize::from(receiver);
        let mut at = b.iter().position(|&c| c == b'(').map_or(b.len(), |i| i + 1);
        while at < b.len() && b[at] != b')' {
            if b[at] == b'[' {
                at += 1;
                continue; // a prefix, not a parameter of its own
            }
            at += match b[at] {
                b'L' => b[at..].iter().position(|&c| c == b';').map_or(1, |i| i + 1),
                _ => 1,
            };
            n += 1;
        }
        n
    }

    /// The class file a [`Unit`] belongs to.
    fn class_of<'c>(classes: &'c [(String, ClassFile)], units: &[(usize, usize)], unit: usize) -> &'c ClassFile {
        &classes[units[unit].0].1
    }

    /// One unit in the shape the compiler wants a body in.
    ///
    /// The `Code` attribute comes from `bodies` rather than from a fresh `member_code` call:
    /// parsing one hands back an **owned** value, so a body re-parsed here would be dropped while
    /// the compiler still held a `&[u8]` into it.
    fn shape<'c>(
        classes: &'c [(String, ClassFile)],
        units: &[(usize, usize)],
        bodies: &'c [crate::jvm::parser::Code],
        unit: usize,
    ) -> crate::burst::compile::Method<'c> {
        let class = class_of(classes, units, unit);
        let member = &class.methods[units[unit].1];
        let code = &bodies[unit];
        crate::burst::compile::Method {
            unit,
            code: &code.code,
            max_locals: code.max_locals as usize,
            descriptor: class.utf8(member.descriptor_index).unwrap_or(""),
            is_static: member.is_static(),
            has_handlers: !code.exception_table.is_empty(),
        }
    }

    // **The method table** (step 8). Inlining means a compilation spans more than one class file,
    // so the census can no longer work one file at a time: every bodied method of `java/` *and* of
    // `boot/` is loaded first and given a [`Unit`], and the resolvers below index that table.
    // `boot/` is in the table but not in the population — nothing there is censused, but a
    // constructor's `super()` has to be able to find `java.lang.Object.<init>` or no `<init>` would
    // ever inline.
    let (mut java_paths, mut boot_paths) = (Vec::new(), Vec::new());
    walk(std::path::Path::new("java"), &mut java_paths);
    walk(std::path::Path::new("boot"), &mut boot_paths);
    java_paths.sort();
    boot_paths.sort();

    let mut classes: Vec<(String, ClassFile)> = Vec::new();
    let mut units: Vec<(usize, usize)> = Vec::new(); // unit -> (class index, member index)
    let mut bodies: Vec<crate::jvm::parser::Code> = Vec::new(); // unit -> its parsed `Code`
    let mut population: Vec<usize> = Vec::new(); // the units drawn from `java/`, i.e. what is censused
    let mut by_name: BTreeMap<(String, String, String), usize> = BTreeMap::new();
    let mut files = 0usize;

    for (from_java, path) in
        java_paths.iter().map(|p| (true, p)).chain(boot_paths.iter().map(|p| (false, p)))
    {
        let Ok(class) = ClassFile::from_path(path.to_str().expect("utf-8 path")) else { continue };
        let Some(name) = class.class_name(class.this_class).map(str::to_string) else { continue };
        files += usize::from(from_java);
        let ci = classes.len();
        for (mi, member) in class.methods.iter().enumerate() {
            // `native`/`abstract`: no body to census and none to inline.
            let Some(body) = class.member_code(member) else { continue };
            let key = (
                name.clone(),
                class.utf8(member.name_index).unwrap_or("?").to_string(),
                class.utf8(member.descriptor_index).unwrap_or("").to_string(),
            );
            let unit = units.len();
            units.push((ci, mi));
            bodies.push(body);
            by_name.entry(key).or_insert(unit);
            if from_java {
                population.push(unit);
            }
        }
        classes.push((name, class));
    }

    let mut methods = 0usize;
    let mut returns_value = 0usize;
    let mut compiled: Vec<String> = Vec::new();
    let mut reasons: BTreeMap<String, usize> = BTreeMap::new();

    for &root in &population {
        let class = class_of(&classes, &units, root);
        let member = &class.methods[units[root].1];
        methods += 1;
        // The **ceiling**, and every step so far has moved it. It is the set of methods whose
        // *exit* this tier can express: step 5 added `areturn` (methods returning a reference)
        // and step 7 added `return` (methods returning `void` — `<init>` and every setter,
        // which together are a large fraction of any real corpus). What remains permanently
        // out is `long`/`double`/`float`, and that is a limit of the **boundary** — the packed
        // `RAX = (status << 32) | value` has 32 bits for the value — rather than of the subset.
        let descriptor = class.utf8(member.descriptor_index).unwrap_or("");
        let returns = descriptor.rsplit(')').next().unwrap_or("");
        if matches!(returns, "I" | "Z" | "B" | "S" | "C" | "V") || returns.starts_with(['L', '[']) {
            returns_value += 1;
        }
        let result = crate::burst::compile::compile(
            &shape(&classes, &units, &bodies, root),
            &crate::burst::compile::Environment {
                int_const: &|unit, index| class_of(&classes, &units, unit).integer_constant(index),
                static_int: &|unit, index| {
                    // The stub: any `int` static resolves, to an address no code here will run.
                    match class_of(&classes, &units, unit).fieldref_target(index) {
                        Some((_, _, "I")) => Some(poll),
                        _ => None,
                    }
                },
                // The same shape of stub for `getfield`/`putfield`: any `int` instance field
                // resolves, to an offset no code here will touch. Like the static stub it
                // cannot know about `volatile` or about a layout that is not loaded, so the
                // census stays an **upper bound** on what a running VM would accept.
                int_field: &|unit, _, index| {
                    match class_of(&classes, &units, unit).fieldref_target(index) {
                        Some((_, _, "I")) => Some(0),
                        _ => None,
                    }
                },
                // The same shape of stub again for `new`: every class resolves, to a small
                // instance and a plausible header word. A running VM additionally requires the
                // class to be *initialised*, which this cannot know — so, once more, an upper
                // bound rather than a prediction.
                instance: &|_, _| Some(crate::burst::compile::Instance { size: 16, class_id: 1 }),
                // And the same shape once more for the **call** (step 8): a `Methodref` naming a
                // method in the table resolves to that method's body, with no check that the class
                // is initialised or that the method is not `synchronized`, and no walk up the
                // superclass chain for an inherited target. The first two make this an upper bound
                // like every stub above; the third makes it a slight *under*-count, and is left as
                // the simpler thing because a direct hit is what `super()` and every constructor
                // call already are.
                invoke: &|unit, pc, index| {
                    let class = class_of(&classes, &units, unit);
                    let code = &bodies[unit].code;
                    if !matches!(code.get(pc), Some(0xb7 | 0xb8)) {
                        return None;
                    }
                    let (owner, name, desc) = class.methodref_target(index)?;
                    let target = *by_name.get(&(owner.to_string(), name.to_string(), desc.to_string()))?;
                    Some(crate::burst::compile::Callee {
                        method: shape(&classes, &units, &bodies, target),
                        arg_slots: census_arg_slots(desc, code[pc] == 0xb7),
                    })
                },
                heap: CENSUS_HEAP,
                poll_word: poll,
            },
        );
        match result {
            Ok(_) => {
                let short = classes[units[root].0].0.rsplit('/').next().unwrap_or("?");
                let name = class.utf8(member.name_index).unwrap_or("?");
                compiled.push(format!("{short}.{name}"));
            }
            Err(crate::burst::compile::Ineligible::Opcode { pc, opcode }) => {
                // The mnemonic is decoded from the **opcode byte** rather than by indexing this
                // method's code at `pc`. Step 8 is why: a refusal can now come from an inlined
                // callee's body, so its `pc` is a position in *that* code array and indexing this
                // one with it is out of bounds (or, worse, in bounds and wrong). The padding is
                // there so a variable-length opcode still decodes; only the mnemonic is read.
                let _ = pc;
                let mut one = [0u8; 32];
                one[0] = opcode;
                let mnemonic = crate::jvm::opcode::decode(&one, 0).mnemonic;
                *reasons.entry(format!("opcode {mnemonic} (0x{opcode:02x})")).or_default() += 1;
            }
            Err(other) => {
                // Everything that is not "an opcode outside the whitelist" is rare enough to
                // group by variant name rather than by its fields.
                let variant = format!("{other:?}");
                let head = variant.split([' ', '(']).next().unwrap_or("?");
                *reasons.entry(head.to_string()).or_default() += 1;
            }
        }
    }

    let mut ranked: Vec<_> = reasons.into_iter().collect();
    ranked.sort_by(|a, b| b.1.cmp(&a.1).then(a.0.cmp(&b.0)));

    eprintln!();
    eprintln!("F3 step 9 — compiled-subset census over {files} class files");
    eprintln!("methods with a Code attribute: {methods}");
    eprintln!(
        "...of those, with an expressible exit: {returns_value}   <- the ceiling: `ireturn`/`areturn`/`return`"
    );
    eprintln!(
        "methods that compile:          {}   ({:.0}% of the ceiling)",
        compiled.len(),
        100.0 * compiled.len() as f64 / returns_value.max(1) as f64
    );
    eprintln!();
    eprintln!("the twenty most common reasons for refusing the rest:");
    for (reason, count) in ranked.iter().take(20) {
        eprintln!("  {count:>5}  {reason}");
    }
    eprintln!();
    eprintln!("the {} methods that compile:", compiled.len());
    for name in &compiled {
        eprintln!("  {name}");
    }
    eprintln!();
}

#[test]
fn methods_outside_the_subset_are_scanned_once_and_never_again() {
    // The cost model. A hot method that cannot be compiled must pay for exactly one scan in its
    // whole lifetime — `BmInvoke.bmFib` is called ~250 000 times, so a per-call rescan would be
    // impossible to miss in the timings but easy to miss by reading the code.
    let (_, _, stats) = run("java/BmInvoke.class", true);
    // Two methods are now scanned: `bmFib` (from its invocation counter) and `run` (from the
    // back-edge counter of the loop that calls it). Each exactly once, ever — `bmFib` is called
    // ~250 000 times and `run` loops 6 000, so a per-event rescan would be impossible to miss in
    // the timings and easy to miss by reading the code.
    assert_eq!(stats.rejected, 2, "each is scanned exactly once, and refused");
    assert_eq!(stats.compiled, 0);
}

// =============================================================================================
// Step 5: references, through the whole VM.
// =============================================================================================

#[test]
fn bmvirtual_is_the_workload_step_5_took_from_the_controls() {
    // The measurement this step exists for, and the reason it is stated as a *lost control*: until
    // now `BmVirtual` compiled nothing at all, so its two arms were the same execution. Its three
    // `f` overrides are `aload_0; getfield k; …; ireturn` — one instance-field read each, which is
    // precisely what references bought — so they now compile and the polymorphic call site behind
    // them lands in native code three times out of three. 861237 is what `java BmVirtual` prints.
    let stats = differential("java/BmVirtual.class", 861_237);
    assert_eq!(stats.compiled, 3, "`BmSq.f`, `BmCir.f`, `BmTri.f` — one per subclass");
    assert_eq!(stats.rejected, 1, "`run` itself: an `aaload` and an `invokevirtual` per iteration");
    // 220 000 calls, of which the first 31 per method are interpreted while its counter climbs to
    // the threshold: 220000 - 3 * 31. Pinned to the digit because it is the proof that the
    // *polymorphic* site reaches native code on every one of its three receivers — a number that
    // came out a third of this would mean two of the three overrides were quietly falling back.
    assert_eq!(stats.native_calls, 220_000 - 3 * 31);
    assert_eq!(stats.deopts, 0, "no receiver is ever null");
    // Slot 0 is `this`, a `Value::Reference`, and it is marshalled — this counter being zero is
    // the whole of step 5 in one assertion. Before it, every one of these calls was refused here.
    assert_eq!(stats.unmarshallable, 0);
    assert_eq!(stats.osr_entries, 0, "`f` has no loop; it is hot from its invocation counter");
}

#[test]
fn references_agree_with_the_interpreter() {
    // `JrRef` is step 5's coverage file: `getfield` of a declared, an inherited and a
    // past-a-`long` field; `arraylength` and `iaload` in a loop and off both ends; all four
    // reference comparisons; and an `areturn` whose *identity* is checked. 604164 is what
    // `java JrRef` prints.
    let stats = differential("java/JrRef.class", 604_164);
    // `getOwn`, `getBase`, `getAfter`, `mix`, `nullField`, `sum`, `at`, `nullness`, `pick`.
    // Every one of them begins with an `aload` of a slot the previous step could not marshal.
    assert_eq!(stats.compiled, 9);
    // `run` is scanned once (from the back-edge of its own loop) and refused: it allocates.
    assert_eq!(stats.rejected, 1);
    // Three deopt sites, hit once per round for 400 rounds: a null receiver, an index of -1 and
    // an index of `length`. Each is a *restart* — the interpreter re-runs the method from its
    // first byte and throws the exception itself — so the count is exactly the number of attempts
    // made **after** the method in question went native, and the two methods warm at different
    // rates: `nullField` is called once per round, so its first 31 calls are interpreted and the
    // remaining 369 deopt; `at` is called three times per round (one in range, two out), so it is
    // warm from the middle of round 10 and 780 of its 800 bad calls deopt.
    assert_eq!(stats.deopts, 369 + 780);
    // The point of the whole step: not one call was abandoned because a local held a reference.
    assert_eq!(stats.unmarshallable, 0);
    // `sum` is the one method with a loop, so it is the one entered on-stack — with a reference
    // (the array) live in local 0 at the moment of entry.
    assert_eq!(stats.osr_entries, 1);
}

// =============================================================================================
// Step 6: the real deopt, and the writes it made safe.
// =============================================================================================

#[test]
fn writes_and_deopts_agree_with_the_interpreter() {
    // `JdWrite` is step 6's coverage file, and every write in it is deliberately
    // **non-idempotent** (`+ 1`, never `= 1`). That is what makes it a test rather than a
    // demonstration: deopt-by-restart would re-run the method from its first byte and apply each
    // write a second time, and the totals the program returns would say so. 832289 is what
    // `java JdWrite` prints.
    //
    // What it drives, one clause per danger the step had to answer:
    //
    //  - a deopt **mid-expression**, with operands live on the stack (`midExpression`);
    //  - a **`putfield` then a deopt** — fifty times, with the field checked to have moved by
    //    exactly fifty (`writeThenFail`), and the same shape through a **`putstatic`**;
    //  - a deopt with **references live in locals and on the operand stack** (`twoArrays`: the
    //    second array is under the index when the bounds check fails), with allocation between the
    //    rounds so a minor collection can fire while a rebuilt frame is a GC root;
    //  - a **loop of `iastore`s that runs off the end** (`bump`), with the array's total checked to
    //    have moved by exactly its length whatever number of iterations native code got through;
    //  - a deopt **caught by a handler in the rebuilt frame itself** (`guarded`), which is the one
    //    place a method with an exception table and a deopt site meet.
    let stats = differential("java/JdWrite.class", 832_289);
    // `midExpression`, `writeThenFail`, `staticThenFail`, `twoArrays`, `bump`, `guarded`, `scale`,
    // and — since step 7 put `return` in the subset — `java.lang.Object.<init>`, which is a bare
    // `return` and is reached by every allocation `run` makes.
    // Not `run` (it allocates), not `sum` (four calls and a four-iteration loop: never warm).
    assert_eq!(stats.compiled, 8);
    assert_eq!(stats.unmarshallable, 0, "an array and a receiver both marshal");
    // 1 + 50 + 50 + 50 + 1 + 1: every one of them a *resume*, not a restart.
    assert_eq!(stats.deopts, 153);
}

#[test]
fn the_write_workloads_are_the_measurement_bmfield_and_bmarray_cannot_be() {
    // `BmField` and `BmArray` are still controls after this step, and the reason has moved: their
    // hot method allocates (`new` / `newarray`), so the writes were never the only thing keeping
    // them out. `JdField` and `JdArray` are the same arithmetic with the inner loop hoisted into a
    // method of its own — the allocation stays in `run`, the writes move into the compiled subset —
    // which is exactly the relationship `JtLoop` has to `BmLoop`. They are what `bench_jit` times.
    //
    // 649216 and 685184 are what `java JdArray` and `java JdField` print.
    let array = differential("java/JdArray.class", 649_216);
    assert_eq!(array.compiled, 1, "`pass`; `run` allocates the array");
    assert_eq!(array.rejected, 1, "`run`, scanned once from its back-edge");
    assert_eq!(array.deopts, 0, "no index is ever out of range");
    // Every one of the thousand calls reaches native code, including the *first*: `pass`'s own
    // 1024-iteration loop takes it past the threshold from the inside, so that call finishes
    // on-stack and the other 999 enter at the top.
    assert_eq!(array.native_calls, 1000);
    assert_eq!(array.osr_entries, 1);

    let field = differential("java/JdField.class", 685_184);
    // `churn`, plus `java.lang.Object.<init>` (a bare `return`, in the subset since step 7) which
    // every `new JdBox()` in `run` reaches. `run` itself still allocates and is still refused.
    assert_eq!(field.compiled, 2, "`churn` and `Object.<init>`; `run` allocates the box");
    assert_eq!(field.deopts, 0, "no receiver is ever null");
    assert_eq!(field.unmarshallable, 0, "the receiver marshals as its heap offset");
}

#[test]
fn a_write_survives_a_deopt_exactly_once() {
    // The ordering rule, isolated: a deopt hands back the pc of an instruction that has **not**
    // run, so the interpreter executes it once and the total effect of the pair is one write.
    //
    // The whole-program differential above already pins this, but it pins it inside a five-digit
    // sum. Here the claim is checked directly against the interpreter, on the two methods that put
    // a write and a deopt in the same body, and — crucially — the JIT arm is asserted to have
    // *taken* those deopts. A run where the JIT quietly compiled nothing would agree with the
    // interpreter for the least interesting reason available.
    let (off, _, off_stats) = run("java/JdWrite.class", false);
    let (on, _, on_stats) = run("java/JdWrite.class", true);
    assert_eq!(on, off);
    assert_eq!(off_stats.deopts, 0, "the interpreter arm has no native code to give up in");
    assert!(on_stats.deopts > 150, "only {} deopts — was anything compiled?", on_stats.deopts);
}

#[test]
fn the_poll_hands_references_back_as_references() {
    // The write-back, asked in the least forgiving way available: the poll is held **up for the
    // whole run**, so every compiled loop runs one iteration and leaves, thousands of times, and
    // every one of those exits has to put two heap offsets and four `int`s back into an
    // interpreter frame with the right tags on them.
    //
    // A mistyped write-back cannot survive this. An offset written back as a `Value::Int` is
    // rejected by the interpreter's own `iaload`/`getfield` on the very next iteration; and even
    // if it were only carried, `run` allocates hard enough between the rounds that a minor
    // collection *moves* these objects while `walk` is half-interpreted — and the collector
    // relocates a frame slot only if the frame says it is a reference. 977804 is what
    // `java JrPoll` prints.
    use std::sync::atomic::Ordering;

    // First without touching the poll at all: both arms agree, and the compiled loops really are
    // entered on-stack with references in their locals.
    let quiet = differential("java/JrPoll.class", 977_804);
    assert_eq!(quiet.compiled, 4, "`walk`, `carry`, `sameness` and `Object.<init>` (a bare `return`)");
    assert_eq!(quiet.osr_entries, 2, "`walk` and `carry` are each entered in the middle of a loop");
    assert_eq!(quiet.safepoint_exits, 0, "nothing raises the poll in this arm");
    assert_eq!(quiet.unmarshallable, 0);

    // Then with the poll held up for the whole run, which turns those two on-stack entries into
    // thousands of one-iteration excursions — and therefore into thousands of write-backs.
    let (value, stats) = with_poll_on("java/JrPoll.class", |poll| poll.store(1, Ordering::Release));
    assert_eq!(value, 977_804, "the answer must not depend on when the poll fires");
    assert!(
        stats.safepoint_exits > 1_000,
        "only {} exits — was the poll ever seen?",
        stats.safepoint_exits
    );
    assert_eq!(stats.unmarshallable, 0, "a reference must marshal, in both directions");
    assert_eq!(stats.deopts, 0, "nothing here is null and no index is out of range");
}

// =============================================================================================
// Step 7: an object allocated by **native code**, against the real heap and the real collector.
// =============================================================================================

/// A `GreenThread` holding `frames` and nothing else — the shape `gc::minor` and `gc::verify_heap`
/// walk for roots.
#[cfg(windows)]
fn rooted_thread(frames: Vec<Frame>) -> crate::jvm::interpreter::bytecode_interpreter::GreenThread {
    use crate::jvm::interpreter::bytecode_interpreter::{GreenThread, ThreadStatus};
    GreenThread {
        id: 0,
        status: ThreadStatus::Runnable,
        frames,
        thread_obj: 0,
        wait_reacquire: None,
        joining_on: None,
        sleep_until: None,
        interrupt_pending: false,
        block_call_pc: 0,
        os_handle: None,
        os_spawned: false,
        at_safepoint: false,
        park_permit: false,
        parked: false,
    }
}

/// The constant-pool index of the `Class` entry naming `target` in `caller`'s pool — how the JIT's
/// `new` resolver is actually reached, so the test drives the real path rather than a fabricated
/// index.
#[cfg(windows)]
fn class_constant(metaspace: &MetaspaceService, caller: &str, target: &str) -> u16 {
    let class = metaspace.get(caller).expect("the caller is loaded");
    (1..class.constant_pool.len() as u16)
        .find(|&i| class.class_name(i) == Some(target))
        .expect("the caller's pool names the target class")
}

/// **The GC-integration test the whole of step 7 turns on**: an object that native code allocated
/// must be indistinguishable, to the collector, from one the interpreter allocated — *scannable*
/// (its header types it), *evacuable* (its size is known and its bytes move), and *remappable*
/// (every reference to it is rewritten).
///
/// It is built against the real `HeapService`, the real `MetaspaceService` and the real
/// `gc::minor`, because every cheaper version of this test checks the wrong thing: a fake heap
/// cannot tell you whether the pending log was written, and a mocked collector cannot tell you
/// whether the header word means what the mirror index thinks it means.
///
/// The sequence is exactly the one the VM performs:
///
///  1. compile a `new` against `heap.jit_bases()` and the real `objects_operations::jit_instance`;
///  2. run it, and replay its allocation log through `HeapService::log_jit_allocation` — the
///     trampoline's job, and the one part of an allocation compiled code cannot do itself;
///  3. `commit_pending`, which is what every GC entry does;
///  4. write a value into the object's field *through the interpreter's own accessor*, so the
///     "fields intact" check below is about bytes and not about the compiler agreeing with itself;
///  5. collect, with the reference held in a frame — the only thing keeping it alive;
///  6. check it survived, moved out of Eden, kept its field, and that the root was rewritten.
#[test]
#[cfg(windows)]
fn an_object_allocated_by_compiled_code_survives_a_minor_collection() {
    use crate::burst::compile::{Environment, Instance, Method, Outcome, Status};
    use crate::burst::exec_mem::ExecMem;
    use crate::jvm::interpreter::bytecode_interpreter::{class_operations, objects_operations};
    use crate::jvm::interpreter::gc;
    use crate::jvm::interpreter::heap::HeapService;
    use crate::jvm::interpreter::metaspace::InitState;

    let mut metaspace = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
    let mut heap = HeapService::new();
    // `BmField`'s pool names `BmCell`, which has two `int` fields — a real class, a real layout, a
    // real mirror. Loading it is what mints its Class ID and allocates that mirror in Old.
    for name in ["BmField", "BmCell"] {
        class_operations::load_class(&mut metaspace, &mut heap, name);
    }
    let index = class_constant(&metaspace, "BmField", "BmCell");
    // `new` is a first active use, so the JIT's resolver refuses a class that has not run its
    // `<clinit>` — which is what the class is in right now, loaded but not initialised. That
    // refusal is checked here rather than assumed, because it is the one precondition of an inline
    // allocation that is about the *VM's state* rather than about the bytecode.
    assert!(
        objects_operations::jit_instance(&metaspace, "BmField", index).is_none(),
        "an uninitialised class must not be allocated by compiled code"
    );
    metaspace.set_init_state("BmCell", InitState::Done);
    let (size, class_id) =
        objects_operations::jit_instance(&metaspace, "BmField", index).expect("BmCell resolves");

    // The compiler's answer must be the interpreter's. An object the two disagree about is either
    // the wrong number of bytes for the collector to copy or a header it cannot type — so the two
    // are compared here, against an object the *interpreter* built, before anything is compiled.
    let interpreted = objects_operations::allocate(&mut metaspace, &mut heap, "BmCell");
    assert_eq!(heap.read_u32(interpreted), class_id, "the same header word the interpreter writes");
    heap.commit_pending();
    let by_interpreter =
        heap.allocations().iter().find(|a| a.offset == interpreted).expect("logged").size;
    assert_eq!(by_interpreter, size as usize, "the same number of bytes");

    // ---- 1. compile `new BmCell; astore_0; aload_0; areturn` -------------------------------
    let bases = heap.jit_bases();
    let code = [0xbb, (index >> 8) as u8, index as u8, 0x4b, 0x2a, 0xb0];
    let compiled = crate::burst::compile::compile(
        &Method {
            unit: 0, code: &code,
            max_locals: 1,
            descriptor: "()LBmCell;",
            is_static: true,
            has_handlers: false,
        },
        &Environment {
            int_const: &|_, _| None,
            static_int: &|_, _| None,
            int_field: &|_, _, _| None,
            instance: &|_, i| {
                objects_operations::jit_instance(&metaspace, "BmField", i)
                    .map(|(size, class_id)| Instance { size, class_id })
            },
            invoke: &|_, _, _| None,
            heap: crate::burst::compile::Heap {
                eden_base: bases.eden,
                other_base: bases.other,
                eden_end: bases.eden_end as u32,
                max_offset: bases.max_offset,
                eden_cursor: bases.eden_cursor,
                eden_capacity: bases.eden_capacity,
                null_page: bases.null_page as u32,
                array_length: 8,
                int_array_data: 12,
                int_element: 4,
            },
            poll_word: &JIT_GC_POLL as *const _ as usize,
        },
    )
    .expect("`new` of an initialised class is inside the subset");

    // ---- 2. run it, and replay the log exactly as `JitCache::enter` does --------------------
    let mem = ExecMem::from_code(&compiled.code).expect("map W^X");
    let mut buffer = vec![0i64; compiled.buffer_slots as usize + 1];
    // SAFETY: the same contract `JitCache::enter` satisfies — a live `[i64]` at least
    // `buffer_slots` long, and an entry pc of 0.
    let f: extern "system" fn(*mut i64, i64) -> i64 = unsafe { mem.as_fn() };
    let raw = f(buffer.as_mut_ptr(), 0);
    let object = match Status::unpack(raw) {
        Outcome::Returned(v) => v as u32 as usize,
        other => panic!("the allocation should have taken its fast path, got {other:?}"),
    };
    let base = compiled.alloc_base as usize;
    assert_eq!(buffer[base], 1, "one object was logged");
    assert_eq!(buffer[base + 1] as usize, object, "...and it is the one that came back");
    assert_eq!(buffer[base + 2] as usize, size as usize);
    for r in 0..buffer[base] as usize {
        heap.log_jit_allocation(buffer[base + 1 + 2 * r] as usize, buffer[base + 2 + 2 * r] as usize);
    }

    // The object native code made is in Eden, is *young*, and — the whole point of the log — the
    // collector's own view of the heap contains it once `commit_pending` runs.
    assert!(object >= bases.null_page && object < bases.eden_end, "allocated in Eden");
    assert!(!heap.allocations().iter().any(|a| a.offset == object), "not logged until committed");
    heap.commit_pending();
    let entry = heap.allocations().iter().find(|a| a.offset == object).expect("committed");
    assert_eq!(entry.size, size as usize, "the collector will copy exactly this many bytes");

    // ---- 3–4. a field written through the *interpreter's* accessor -------------------------
    // 8 is the first `int` past the `[class_id | mark]` header — `BmCell.a`.
    heap.write_u32(object + 8, 0x1234_5678);
    assert_eq!(heap.read_u32(object), class_id, "the header compiled code wrote types the object");

    // ---- 5. collect, with the reference held in a frame ------------------------------------
    let mut threads = vec![rooted_thread(vec![Frame::new(0, 1, vec![Value::Reference(object)])])];
    let report = gc::minor(&metaspace, &mut heap, &mut threads, 15, &mut []);
    assert!(report.copied > 0, "something was evacuated");

    // ---- 6. survived, moved, intact, and remapped ------------------------------------------
    let moved = match threads[0].frames[0].locals()[0] {
        Value::Reference(offset) => offset,
        other => panic!("the root stopped being a reference: {other:?}"),
    };
    assert_ne!(moved, object, "a minor collection evacuates out of Eden");
    assert!(moved >= bases.eden_end, "...and the new home is outside Eden");
    assert_eq!(heap.read_u32(moved), class_id, "the header travelled with it");
    assert_eq!(heap.read_u32(moved + 8), 0x1234_5678, "and so did the field");
    assert!(heap.allocations().iter().any(|a| a.offset == moved && a.size == size as usize));
    // Nothing dangles: the post-collection verifier walks every frame and every object slot.
    gc::verify_heap(&metaspace, &heap, &threads);
}

/// The counterpart, and the one that would fail silently in production: an object native code
/// allocated but the trampoline **did not** log is invisible to the collector — and the verifier
/// says so. This is the test that gives the replay in `JitCache::enter` its teeth; without it, a
/// future refactor could drop the call and every other test here would still pass.
#[test]
#[cfg(windows)]
#[should_panic(expected = "DANGLING")]
fn an_unlogged_allocation_is_exactly_the_corruption_the_replay_prevents() {
    use crate::jvm::interpreter::bytecode_interpreter::class_operations;
    use crate::jvm::interpreter::gc;
    use crate::jvm::interpreter::heap::HeapService;

    let mut metaspace = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
    let mut heap = HeapService::new();
    class_operations::load_class(&mut metaspace, &mut heap, "BmCell");
    // An object in Eden that the collector's log does not contain. `malloc` records into the
    // per-thread *pending* log and `commit_pending` is what moves it into the collector's view —
    // so skipping that step is precisely the state a compiled `new` would leave behind if the
    // trampoline forgot to replay its records.
    let object = heap.malloc(24);
    let threads = vec![rooted_thread(vec![Frame::new(0, 1, vec![Value::Reference(object)])])];
    gc::verify_heap(&metaspace, &heap, &threads);
    // ...and with the log committed the very same heap is clean, which is what makes the panic
    // above about the missing record and nothing else.
    heap.commit_pending();
    gc::verify_heap(&metaspace, &heap, &threads);
}

/// The poll word the GC tests compile against; never raised.
#[cfg(windows)]
static JIT_GC_POLL: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(0);



