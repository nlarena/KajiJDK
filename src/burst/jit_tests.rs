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
    let mut metaspace = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
    let class = ClassFile::from_path(class_file).expect("load class");
    let name = class.class_name(class.this_class).unwrap().to_string();
    metaspace.add(name.clone(), class);
    let entry = metaspace.resolve_method(&name, "run", "()I").expect("run()");
    let max_locals = metaspace.max_locals(entry);
    let frame = Frame::new(entry, max_locals, Vec::new());
    let (value, steps, stats) = execute_counting_with_jit(metaspace, frame, Some(jit));
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
    assert_eq!(stats.compiled, 10, "the ten helpers; `warm` itself is full of invokes");
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
    // Four of the five `Bm*` workloads remain this milestone's **zero-effect controls**, each for
    // its own reason: BmInvoke's `bmFib` calls itself, BmField touches fields, BmArray touches an
    // array, BmVirtual dispatches virtually — all outside the subset, in the inner method *and* in
    // the `run()` loop around it. Nothing here compiles, so both arms are literally the same
    // execution and whatever ratio they show in `bench_jit` is the noise floor.
    //
    // Pinning it here means the measurement table cannot quietly become a comparison of two
    // identical runs: if a future change makes any of these compile, this test says so first.
    for (class_file, expected) in [
        ("java/BmInvoke.class", 252_624),
        ("java/BmField.class", 973_376),
        ("java/BmArray.class", 615_180),
        ("java/BmVirtual.class", 861_237),
    ] {
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
    use crate::jvm::interpreter::bytecode_interpreter::execute_counting_with_jit_and_poll;
    let mut metaspace = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
    let class = ClassFile::from_path("java/OsJit.class").expect("load class");
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
//  2. **Zero-effect controls.** `BmField` and `BmInvoke` compile *nothing* — the control
//     assertions below prove it from the JIT's own counters, not from belief — so their two arms
//     are literally the same execution. Whatever ratio they show is the noise floor, and a
//     treatment's ratio is only evidence in so far as it exceeds it.
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

    // (class file, expected value, does it compile anything?)
    let workloads = [
        ("java/BmLoop.class", 161_265, true),
        ("java/JtLoop.class", 832_880, true),
        ("java/BmField.class", 973_376, false),
        ("java/BmInvoke.class", 252_624, false),
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
    for (class_file, expected, compiles) in workloads {
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
            // control's two arms are the same execution, which is what makes its ratio a noise
            // floor instead of an effect.
            assert_eq!(on_run.2.compiled > 0, compiles, "{short}: role does not match reality");
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
        match compiles {
            true => treatment_ratios.push((short, ratio)),
            false => control_ratios.push(ratio),
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
            if compiles { "treatment" } else { "control (compiles nothing)" }
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
///  - The `getstatic` resolver here is a **stub** that accepts any `int` static and hands back a
///    fixed address. At run time the same `getstatic` also requires the declaring class to be
///    *initialised* already, which this cannot know without a running VM — so the census is an
///    upper bound on the opcode subset rather than a prediction of what a given run compiles.
///  - `main` and other `void` methods can never compile (`return`, 0xb1, is not `ireturn`), so a
///    couple of rejections per file are structural and will never go away.
#[test]
#[ignore = "census: prints the compiled subset's coverage over the java/ corpus"]
fn subset_census() {
    use std::collections::BTreeMap;

    static CENSUS_POLL: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(0);
    let poll = &CENSUS_POLL as *const _ as usize;

    let mut files = 0usize;
    let mut methods = 0usize;
    let mut returns_int = 0usize;
    let mut compiled: Vec<String> = Vec::new();
    let mut reasons: BTreeMap<String, usize> = BTreeMap::new();

    let mut paths: Vec<_> = std::fs::read_dir("java")
        .expect("the java/ corpus")
        .filter_map(|e| e.ok().map(|e| e.path()))
        .filter(|p| p.extension().is_some_and(|e| e == "class"))
        .collect();
    paths.sort();

    for path in &paths {
        let Ok(class) = ClassFile::from_path(path.to_str().expect("utf-8 path")) else { continue };
        files += 1;
        let short = path.file_stem().and_then(|s| s.to_str()).unwrap_or("?").to_string();
        for member in &class.methods {
            let Some(code) = class.member_code(member) else { continue };
            methods += 1;
            // The **ceiling**. `ireturn` is the subset's only exit, so a method whose descriptor
            // does not return `int` (or `boolean`/`byte`/`short`/`char`, which `javac` also returns
            // with `ireturn`) cannot compile however wide the subset gets — constructors, `void`
            // methods and every method returning a reference are permanently out. Reporting the
            // count turns "77 of 710" into a coverage figure with a meaningful denominator.
            let descriptor = class.utf8(member.descriptor_index).unwrap_or("");
            let returns = descriptor.rsplit(')').next().unwrap_or("");
            if matches!(returns, "I" | "Z" | "B" | "S" | "C") {
                returns_int += 1;
            }
            let result = crate::burst::compile::compile(
                &code.code,
                code.max_locals as usize,
                &|index| class.integer_constant(index),
                &|index| {
                    // The stub: any `int` static resolves, to an address no code here will run.
                    match class.fieldref_target(index) {
                        Some((_, _, "I")) => Some(poll),
                        _ => None,
                    }
                },
                poll,
            );
            match result {
                Ok(_) => {
                    let name = class.utf8(member.name_index).unwrap_or("?");
                    compiled.push(format!("{short}.{name}"));
                }
                Err(crate::burst::compile::Ineligible::Opcode { pc, opcode }) => {
                    let mnemonic = crate::jvm::opcode::decode(&code.code, pc).mnemonic;
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
    }

    let mut ranked: Vec<_> = reasons.into_iter().collect();
    ranked.sort_by(|a, b| b.1.cmp(&a.1).then(a.0.cmp(&b.0)));

    eprintln!();
    eprintln!("F3 step 4 — compiled-subset census over {files} class files");
    eprintln!("methods with a Code attribute: {methods}");
    eprintln!("...of those, returning an int: {returns_int}   <- the ceiling: `ireturn` is the only exit");
    eprintln!(
        "methods that compile:          {}   ({:.0}% of the ceiling)",
        compiled.len(),
        100.0 * compiled.len() as f64 / returns_int.max(1) as f64
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
