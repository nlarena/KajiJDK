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
/// El bootclasspath de este arnés, y **el mismo** que usa `run-headless`.
///
/// Estaba en `boot/` a secas, o sea que estos tests cargaban un `java.lang.String` distinto del que
/// carga la VM de verdad: `run-headless` bootea con `KajiLibrary` primero —la biblioteca propia, la
/// fuente de verdad— y `boot/` como relleno para lo que ahí falte. Dos imágenes distintas quieren
/// decir que un test del JIT puede pasar contra una clase que ningún programa real va a ver, que es
/// la forma exacta de un test que parece probar algo y no lo prueba.
///
/// Un solo lugar y no nueve copias, por la razón de siempre: nueve copias son ocho oportunidades de
/// que la próxima diverja en silencio.
fn boot_class_path() -> Vec<PathBuf> {
    vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")]
}

/// Una fixture que toca `String` da lo mismo por este arnés que por la VM de verdad.
///
/// # Qué se estaba escapando
///
/// Este arnés arrancaba desde `boot/` a secas y `run-headless` desde `KajiLibrary` con `boot/` de
/// relleno. O sea que los tests del JIT cargaban **otro** `java.lang.String` que el que corre
/// cualquier programa real, y nada lo comprobaba. Un test que pasa contra una clase que ningún
/// programa va a ver es la definición de un test que parece probar algo y no lo prueba — la familia
/// FZ-003…FZ-007.
///
/// # Por qué `String` y no cualquier clase
///
/// Porque es donde las dos imágenes más se pueden separar sin que se note: `KajiLibrary` es el
/// superset y su `String` tiene el pool de internado que `FZ-008` arregló. La fixture toca la
/// **identidad** de dos literales iguales, que es exactamente lo que ese arreglo cambió, más
/// `length`, `charAt` y `equals`, que leen el arreglo de respaldo.
///
/// El valor está horneado y es el que imprime `java KjBootStr`, como el resto de este archivo.
///
/// # Lo que este test **no** custodia, medido
///
/// No custodia el bootclasspath. Se plantó el sabotaje —volver a `boot/` a secas— y este test
/// **siguió pasando**: `boot/java/lang/String.class` son 407 bytes contra 33612 de `KajiLibrary`,
/// pero los métodos que la fixture toca los intrinsifica el intérprete, así que el archivo de clase
/// casi no participa. Un stub y la biblioteca real dan lo mismo.
///
/// Lo que sí custodia el arranque son los conteos de métodos escaneados de
/// `getstatic_of_an_int_agrees_with_the_interpreter` y `a_loop_before_a_monitorenter_still_runs_natively`:
/// con `boot/` solo dan uno menos cada uno, y los dos fallan. Queda anotado para que nadie lea este
/// test como la garantía que no es.
#[test]
fn una_fixture_de_string_da_lo_mismo_que_el_jdk_real() {
    const ESPERADO: i32 = -713_034_848;
    let (sin_jit, _, _) = run("java/KjBootStr.class", false);
    let (con_jit, _, _) = run("java/KjBootStr.class", true);
    assert_eq!(sin_jit, ESPERADO, "el intérprete no coincide con `java KjBootStr`");
    assert_eq!(con_jit, sin_jit, "el JIT calcula otra cosa que el intérprete");
}

fn run_tuned(class_file: &str, jit: bool, regs: Option<u32>) -> (i32, usize, JitStats) {
    use crate::jvm::interpreter::bytecode_interpreter::execute_counting_tuned;
    let mut metaspace = MetaspaceService::new(boot_class_path(), vec![PathBuf::from("java")]);
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
    // Eight helpers, every one of them inside the subset — **and `run` itself**, since group 3's
    // first stage. What used to refuse it was one of its callees looping: an inlined body with a
    // back-edge was refused outright, because only the root's headers polled. Now an inlined
    // header polls too, so the whole of `run` is one compilation.
    assert_eq!(stats.compiled, 9, "the eight helpers, and `run` now that a callee may loop");
    assert_eq!(stats.rejected, 0, "nothing is refused any more");
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
    // `mix`, and — since group 3's first stage — `run`, whose 3000-iteration loop calls it: `mix`
    // loops, so inlining it used to be refused outright and `run` with it.
    assert_eq!(stats.compiled, 2, "`mix`, and `run` now that its looping callee may be inlined");
    // **The count collapsed, and that is the feature.** Before this stage the interpreter executed
    // `run`'s 3000 invokes and entered native code at each one. Now `run` is itself compiled with
    // `mix` expanded inside it, so the whole nest is one excursion and the interpreter never
    // reaches the invoke at all.
    assert_eq!(stats.native_calls, 33);
    // Two on-stack entries, one per compiled method: each is made hot from inside its own loop and
    // so finishes its first call in native code.
    assert_eq!(stats.osr_entries, 2);
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
    // Three workloads used to be here and are not any more. Step 5 took `BmVirtual` (its three `f`
    // overrides are `aload_0; getfield; …; ireturn`), step 9 took `BmField` — the last one the JIT
    // had never been able to touch at all — and the **array-allocation step took `BmArray`**, whose
    // `run` begins `new int[1024]` and was held out by that one opcode alone. See
    // [`bmvirtual_is_the_workload_step_5_took_from_the_controls`],
    // [`bmfield_is_the_workload_step_9_took_from_the_controls`] and
    // [`bmarray_is_the_workload_the_array_step_took_from_the_controls`].
    //
    // So `BmInvoke` is the **last** control, and that is worth saying plainly: this milestone's
    // measurement table now has exactly one row whose two arms are the same interpreted run.
    //
    // Pinning it here means the table cannot quietly become a comparison of two identical runs: if
    // a future change makes it compile, this test says so first.
    for (class_file, expected) in [("java/BmInvoke.class", 252_624)] {
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
fn bmarray_is_the_workload_the_array_step_took_from_the_controls() {
    // **One opcode was the whole of it.** `BmArray.run` is `iaload`/`iastore` in a doubly nested
    // loop — the exact shape this tier has compiled since step 4 — behind a single `new int[1024]`
    // on its first line. A method is compiled whole or not at all, so that one `newarray` held the
    // other seventeen million opcodes out, and the workload stayed a zero-effect control through
    // six steps that had nothing to do with arrays.
    //
    // It is worth being precise about *why* an array allocation is a step of its own and an object
    // allocation was not, because "arrays too" sounds like a widening and is really a new shape:
    // `new`'s size is a compile-time constant, so its zeroing is a straight run of stores and its
    // Eden bounds check folds the stride into an immediate. An array's size is
    // `header + count * width` with `count` in a **register**, so the same three things become a
    // guard on the count, a zeroing *loop*, and a bounds check whose stride is computed at run
    // time. See `burst::compile::emit_array_alloc`.
    //
    // 615180 is what `java BmArray` prints.
    let stats = differential("java/BmArray.class", 615_180);
    assert_eq!(stats.compiled, 1, "`run`, and there is nothing else in the file");
    assert_eq!(stats.rejected, 0);
    // Entered exactly once, on-stack: `run` is called once and loops a million times inside it.
    assert_eq!(stats.osr_entries, 1);
    assert_eq!(stats.native_calls, 1, "one entry, and it never comes back until the method returns");
    // **Nothing leaves.** The single allocation is 1024 ints — 4108 bytes, over
    // `MAX_INLINE_ARRAY_BYTES` — so it is *not* allocated inline; it deopts... and that would be an
    // `alloc_exit`. It is not one, because the allocation happens **before** the loop, on the very
    // first call, when the method is still cold and interpreted. By the time the back-edge counter
    // makes `run` hot, the array already exists and the compiled entry is at the loop header.
    // That is the honest shape of this workload: the array step is what let it compile, and the
    // array allocation itself never runs natively in it.
    assert_eq!(stats.alloc_exits, 0);
    assert_eq!(stats.deopts, 0, "every index is in range and the array is never null");
    assert_eq!(stats.unmarshallable, 0);
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
    // `longLoop`, `earlyExit`, `nested`, `triple`, `divLoop` — **and `uncompilable`**, which is no
    // longer any such thing: its `int[] a = new int[16]` was the one opcode holding it out, and the
    // array-allocation step compiles it. Its name is now a fossil, and it is left alone on purpose:
    // renaming it would lose the record of a method that *was* refused for six steps and then was
    // not, which is exactly what a coverage file is for.
    //
    // Not `neverEnters` (its back-edge is never taken and it is called once, so nothing ever counts
    // it hot), not `deopting` or `run` (no loop, one call each).
    assert_eq!(stats.compiled, 6);
    assert_eq!(stats.rejected, 0, "nothing in this file is outside the subset any more");
    // Every one of the six was reached **on-stack**: not one of them is called often enough for
    // the invocation counter to matter, and three of them are called exactly once.
    assert_eq!(stats.osr_entries, 6);
    // Seven native calls: those six, plus `earlyExit`'s second call, which by then enters at the
    // top like any ordinary compiled call.
    assert_eq!(stats.native_calls, 7);
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
    let mut metaspace = MetaspaceService::new(boot_class_path(), vec![PathBuf::from("java")]);
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
    let mut metaspace = MetaspaceService::new(boot_class_path(), vec![PathBuf::from("java")]);
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
        let mut metaspace = MetaspaceService::new(boot_class_path(), vec![PathBuf::from("java")]);
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

/// **`ldc` of a `String` literal**, which the compiler refused until the string pool landed.
///
/// The reason it refused was about the VM and not the compiler: `strings::intern` allocated a fresh
/// `String` in Eden on every `ldc`, so there was no permanent offset to bake — and baking one would
/// have made `"a" == "a"` answer `true` in compiled code and `false` in the interpreter, which is
/// worse than a method that does not compile. FZ-008 gave the pool its three properties (one
/// instance per literal, in Old, a GC root, pinned out of `gc::compact`), which is exactly what an
/// immediate needs.
///
/// So what this asks is the thing that used to be impossible: that the two arms agree two `ldc`s of
/// one literal are **the same object**, and that a different literal is a different one. The
/// comparisons go through locals because `javac` folds `("a" == "a")` written inline and the VM
/// would never see it — the mistake that hid FZ-008 for as long as it did.
///
/// **What this cannot catch, and why it does not have to.** A Java program can only observe a
/// literal's *identity relation*, never its address, so a resolver that shifted every offset by a
/// constant would pass here — verified by trying it. That failure is unreachable rather than
/// untested: the resolver reads the very map `strings::intern` writes, so the compiled arm and the
/// interpreter cannot name different objects for one literal without the pool itself being wrong,
/// which its own tests cover. What *is* observable is the relation, and collapsing two literals
/// onto one offset does fail this test.
///
/// Nothing else about `String` is probed, and that is deliberate: this harness boots from `boot/`
/// rather than from `KajiLibrary`, so a probe touching `new String(…)` would be measuring which of
/// the two class libraries is on the path instead of what the compiler did with an `ldc`. Found by
/// writing the richer version first and watching it disagree with the same fixture run through
/// `run-headless`, which boots the other way.
#[test]
fn an_ldc_of_a_string_literal_agrees_with_the_interpreter() {
    // 19003336 is what `java LdcStr` prints.
    let stats = differential("java/LdcStr.class", 19_003_336);
    assert!(
        stats.compiled > 0,
        "nothing compiled, so this proved only that the interpreter agrees with itself"
    );
}

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
    // `bump` and `deep` — and, since group 3 raised the byte budget to two kilobytes, `run` too:
    // `deep` is a 264-slot method and expanding it was what used to exceed the old one.
    assert_eq!(stats.compiled, 3, "`bump`, `deep`, and `run` with both of them expanded into it");
    assert_eq!(stats.rejected, 0, "`run` is no longer refused");
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
    // **Two, not five, and the drop is the point.** `run` used to be refused (its callees loop) and
    // its 3000 invokes each made a helper hotter until all five compiled on their own. Since group
    // 3's first stage `run` compiles with the helpers expanded inside it, so the interpreter stops
    // executing those invokes and the helpers' own counters stop climbing: only the two that were
    // already hot when `run` was compiled have a compilation of their own.
    assert_eq!(stats.compiled, 2, "`run`, with the helpers inlined, plus the one already hot");
    assert_eq!(stats.rejected, 0, "`run` is no longer refused");
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
    // `own`, `inherited`, `far`, `mutable`, `mixed` — **and `churn`**, which the array-allocation
    // step added: it is `new int[16]` in a loop, and it was refused for that opcode alone.
    // Not `notAnInt` (a `String` static, so the resolver refuses it), not `run` (invokes).
    assert_eq!(stats.compiled, 6);
    // Tres y no dos desde que este arnés bootea con `KajiLibrary` en vez de `boot/` a secas (ver
    // [`boot_class_path`]): la biblioteca de verdad pone **un** método más en el camino de este
    // workload, y se lo escanea y se lo rechaza. Que sea rechazado y no compilado se ve en que
    // `compiled` no se movió, y que el JIT siga acertando se ve en que el diferencial de arriba
    // sigue dando 246189. Lo que cambió es cobertura, no corrección.
    //
    // El número es a propósito exacto y no un `>=`: depende de la imagen de arranque, así que si la
    // biblioteca crece este test tiene que fallar y que alguien lo mire.
    assert_eq!(stats.rejected, 3, "`notAnInt`, `run`, y uno de la biblioteca");
    assert_eq!(stats.deopts, 0);
    assert_eq!(stats.unmarshallable, 0);
    // `churn` is the reason this workload was written — it allocates hard enough to force
    // collections between the other calls — and now it allocates *natively*, 16-int arrays at a
    // time, until Eden or the excursion's log fills. Those exits are what says the compiled
    // `newarray` is really running here rather than merely having been emitted.
    assert!(stats.alloc_exits > 0, "`churn`'s arrays fill Eden and the method is handed back");
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
    // `step`, with `poke` expanded into it — and `run`, which builds the array and which the
    // array-allocation step brought in (it was refused for its `newarray` alone). `step` is still
    // the root of its own compilation: `run` calls it through an invoke the inliner declines, so
    // the deopt below still hands back the two-frame chain this test is about.
    assert_eq!(stats.compiled, 2, "`step` with `poke` inlined, and `run`");
    assert_eq!(stats.rejected, 0);
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
    // `outer`, with `at` inlined — and `run`, which the array-allocation step brought in: it was
    // refused for its `newarray` alone.
    assert_eq!(stats.compiled, 2, "`outer` with `at` inlined, and `run`");
    assert_eq!(stats.rejected, 0);
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
    // `outer`, with `middle` and `inner` expanded into it — and, since group 3 raised the depth
    // bound to four, `run` as well: its `newarray` was never the refusal, the chain below it was.
    assert_eq!(stats.compiled, 2, "`outer` with its two expansions, and `run`");
    assert_eq!(stats.rejected, 0);
    assert_eq!(stats.deopts, 93, "one deopt per index out of range reached in native code");
    // Each of those deopts is two frames deep inside the expansion, so the chain is rebuilt every
    // time — which is the thing this test is named for.
    assert_eq!(stats.virtual_frames, 187);
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
// Group 5: the opcodes that leave — `athrow`, and the monitors.
// =============================================================================================

#[test]
fn an_explicit_throw_is_caught_by_the_handler_of_the_frame_the_deopt_rebuilt() {
    // The whole of group 5's `athrow` claim, asked of a whole VM: native code contains no exception
    // handling at all, so a compiled `athrow` deopts at its own pc with the exception on the
    // reconstructed operand stack, and the interpreter re-executes it and unwinds.
    //
    // `JeThrow` walks the three shapes that fail differently (see the file): a throw caught in the
    // very frame the deopt rebuilt — which is the interaction worth checking, since a method with
    // an exception table compiles but gets no OSR and no polls — a throw that leaves the method
    // entirely, and a throw with both frames' operand stacks live, including a **reference** in the
    // caller's.
    //
    // Two things in that file exist so this cannot pass by luck, and they are worth knowing about
    // before editing it: the exception is an `IllegalStateException` rather than a
    // `RuntimeException` (so a deopt that handed back `null` would throw an NPE that escapes the
    // `catch` instead of being absorbed by it), and the outer arms compare the caught object's
    // **identity** against the field it came from (so a spill of the wrong register cannot pass as
    // "the right type"). 35825 is what `java JeThrow` of JDK 25 prints.
    let stats = differential_regs("java/JeThrow.class", 35_825);
    // **Four, and which four is the interesting part.** `caughtHere` and `propagates` are compiled
    // because they are called directly. `catcher` and `deepThrow` are compiled *and contain an
    // invoke*, so their compiling at all is the proof that the callee was **inlined** — a call this
    // tier cannot expand is an `Ineligible::Opcode` and the method is refused outright. So the
    // athrow inside `propagates` and the one inside `bang` are each reached in an expanded body,
    // with the caller's frame rebuilt above the callee's by the same deopt.
    //
    // `bang` and `use` are *not* in the count, and that is the ordinary shape rather than a
    // surprise: their invocation counters were incremented only while `deepThrow` was interpreted,
    // so they stop one short of the threshold on the very call that compiles their caller and are
    // never entered through the interpreter again.
    assert_eq!(stats.compiled, 4, "`caughtHere`, `propagates`, `catcher` and `deepThrow`");
    // **Every deopt here is an athrow, and that is arithmetic rather than a hope.** The only other
    // guard in the file is `100 / k` in `caughtHere`, which is reached only on the path where `k`
    // is not zero. So the count is the number of throws: over 4000 iterations, one per multiple of
    // 7 (571), of 5 (800), of 11 (363) and of 13 (307) — 2041, less the handful of early calls
    // made while each method's invocation counter was still climbing.
    assert!(stats.deopts > 2000, "{} deopts, and every one of them is an athrow", stats.deopts);
    // ...and the exception really did have to be materialised as a *reference*. Nothing here can
    // marshal badly on the way in, so a non-zero count would mean a slot the map could not type.
    assert_eq!(stats.unmarshallable, 0);
}

#[test]
fn a_loop_before_a_monitorenter_still_runs_natively() {
    // The shape group 5 exists for. `JeSync.loopThenSync` is a compilable loop followed by a
    // `synchronized` block whose body is deliberately **outside** the subset, and it compiles
    // anyway: the scan stops at the `monitorenter` (`Flow::Return`), so the code past the lock is
    // not part of the compilation and does not have to be expressible. Before group 5 the method
    // was refused at that byte and its loop was interpreted.
    //
    // 465141 is what `java JeSync` of JDK 25 prints.
    let stats = differential_regs("java/JeSync.class", 465_141);
    // Two methods reach native code and both of them stop at their monitor: 3000 calls each, every
    // one of which enters, runs its 64-iteration loop and then deopts. The equality is what pins
    // the behaviour — a `monitorenter` that fell through instead of deopting would show up here as
    // a count far below the number of entries, and one that refused the method would show up as
    // nothing compiled at all.
    assert_eq!(stats.compiled, 2, "`loopThenSync` and `syncSimple`");
    assert_eq!(stats.native_calls, stats.deopts, "every entry ends at the monitor, none returns");
    assert!(stats.deopts > 5000, "{} deopts: one per call, twice per iteration", stats.deopts);
    // `syncMethod` is the *other* exclusion and it is structural: `ACC_SYNCHRONIZED` is a flag, not
    // an opcode, the interpreter takes its monitor before the JIT's dispatch point is reached, and
    // that dispatch is gated on there being no monitor. So it is never offered, never scanned and
    // never refused — which is why it is absent from both counters rather than present in
    // `rejected`. `run` itself is the one refusal: its loop calls a method that loops.
    // Dos desde el cambio de imagen de arranque, y es **el mismo** `+1` que en
    // `getstatic_of_an_int_agrees_with_the_interpreter`: el método de biblioteca que ahora entra al
    // camino se escanea y se rechaza en los dos workloads. `syncMethod` sigue sin ser ofrecido, que
    // es lo que este aserto existe para decir.
    assert_eq!(stats.rejected, 2, "`run` y uno de la biblioteca; `syncMethod` sigue sin ofrecerse");
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
///    — are no longer structurally excluded and count towards the ceiling. The wide-types step then
///    added `lreturn`, `freturn` and `dreturn` at once, by moving the returned value out of the
///    status register and into the caller's buffer: once a result is 64 type-agnostic bits, **no
///    return type is a reason not to compile a method**, and the ceiling is simply every method
///    with a `Code` attribute whose descriptor parses.
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
///
/// # The split, and why a count of `Ineligible` variants is not an answer
///
/// Four of the variants this table tallies name a **position and an index** and say nothing about
/// the thing at it, so their counts add up causes that have nothing in common and no shared fix:
/// `UnresolvedField` covers a `char` field and a `long` one alike; `NonIntegerConstant` covers a
/// `String` and a `MethodHandle`. Three widenings' worth of work had already gone into that pile
/// without the pile ever being separated, so "how much is left" was unanswerable from this table.
///
/// The resolver *knows* — it is the one that read the descriptor — so it now writes down what it
/// saw on its way out ([`Refusals`]) and the census reads it back when the compilation it aborted
/// comes back as an error. That is a note kept **by the census's own stubs**, not a widening of
/// `Ineligible`: the compiler's error type stays a position and an index, which is all `burst` is
/// entitled to know (it has never had a constant pool and is not getting one for a report).
///
/// Two causes the split structurally cannot see, and both are said out loud in the output rather
/// than left as a silent zero: an **uninitialised declaring class** (the stubs accept
/// unconditionally — the same upper-bound caveat as everywhere else here), and **`volatile`**,
/// which stopped being a refusal at all in group 2.
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
        array_data: 12,
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

    /// **What kind a field of this descriptor holds**, as the census's `getfield`/`getstatic` stubs
    /// answer it — the same table the VM's own `jit_field_site`/`jit_static_field` use, restated
    /// here because the census reads class files with no VM behind them.
    ///
    /// `L…;` and `[…` are group 2's addition and they answer [`Kind::Reference`]. The four
    /// remaining primitives (`B`, `C`, `S`, `Z`) are still `None`: they are stored at widths this
    /// tier's loads have never agreed on, and the *real* resolver refuses them too, so accepting
    /// them here would make the census measure a compiler that does not exist.
    fn census_field_kind(descriptor: &str) -> Option<crate::burst::compile::Kind> {
        use crate::burst::compile::Kind;
        Some(match descriptor {
            "I" => Kind::Int,
            "F" => Kind::Float,
            "J" => Kind::Long,
            "D" => Kind::Double,
            d if d.starts_with('L') || d.starts_with('[') => Kind::Reference,
            _ => return None,
        })
    }

    /// **Why a field descriptor is not one this tier can hold**, in the resolver's own terms.
    ///
    /// [`census_field_kind`] answers `None` for exactly one reason, and it is worth naming rather
    /// than counting: `B`, `C`, `S` and `Z` are stored at 1 and 2 bytes, and this tier's loads and
    /// stores have no encoding for those widths ([`x64`][super::x64] can `mov` 4 and 8 bytes and
    /// nothing else). Everything else that reaches here is a malformed descriptor.
    fn census_field_cause(descriptor: &str) -> &'static str {
        match descriptor {
            "B" | "Z" => "byte/boolean — a 1-byte access this tier cannot encode",
            "C" | "S" => "char/short — a 2-byte access this tier cannot encode",
            _ => "descriptor is not a field type",
        }
    }

    /// The **constant-pool tag** at `index`, as a word — what a refused `ldc` was actually naming.
    ///
    /// `NonIntegerConstant` is the compiler's answer for "no resolver would take this", and it
    /// mixes causes that are not remotely alike: a `String` is refused because this VM has no
    /// interning table (see the module docs), while a `MethodHandle` or a `Dynamic` is refused
    /// because nothing in this tier could ever materialise one. The census can tell them apart
    /// because, unlike `burst`, it is holding the constant pool.
    ///
    /// The pool is **1-based** in the class file and 0-based in the `Vec`, exactly as every
    /// resolver on `ClassFile` handles it — getting that wrong reads the entry before the one the
    /// `ldc` named, which for a `String` is its `Utf8` and looks entirely plausible.
    fn census_constant_tag(class: &ClassFile, index: u16) -> &'static str {
        use crate::jvm::parser::constant_pool::ConstantPoolEntry as E;
        let Some(at) = index.checked_sub(1) else { return "index 0" };
        match class.constant_pool.get(at as usize) {
            Some(E::Utf8(_)) => "Utf8",
            Some(E::Integer(_)) => "Integer",
            Some(E::Float(_)) => "Float",
            Some(E::Long(_)) => "Long",
            Some(E::Double(_)) => "Double",
            Some(E::Class { .. }) => "Class",
            Some(E::String { .. }) => "String",
            Some(E::MethodHandle { .. }) => "MethodHandle",
            Some(E::MethodType { .. }) => "MethodType",
            Some(E::Dynamic { .. }) => "Dynamic",
            _ => "something else",
        }
    }

    /// **Why a constant of this tag is not one this tier can bake into the instruction stream.**
    /// One line per tag, because the reasons genuinely differ and only one of them is a decision
    /// this project could revisit tomorrow.
    fn census_constant_cause(tag: &str) -> &'static str {
        match tag {
            // The one that is a *VM* fact rather than a compiler one: `strings::intern` keeps no
            // table and allocates a fresh Eden `String` per execution, so there is no permanent
            // offset to bake — and baking one would make `"a" == "a"` true where the interpreter
            // says false. See the module docs.
            "String" => "the pool exists (FZ-008) but the compiler does not bake a literal yet",
            "MethodHandle" | "MethodType" | "Dynamic" => "nothing in this tier can materialise one",
            _ => "no resolver in the subset answers for it",
        }
    }

    /// A field descriptor with its package stripped — `Ljava/util/concurrent/Semaphore;` becomes
    /// `LSemaphore;`. Purely so the split table stays a table; the column is about the *kind* of
    /// thing the field holds, and the package never changes the answer.
    fn census_short_descriptor(descriptor: &str) -> String {
        match descriptor.rsplit('/').next() {
            Some(tail) if tail != descriptor => format!("L{tail}"),
            _ => descriptor.to_string(),
        }
    }

    /// **What a census stub said no to, and why** — the split the coarse `Ineligible` variants
    /// cannot carry.
    ///
    /// `UnresolvedStatic`, `UnresolvedField` and `NonIntegerConstant` each name a *position* and an
    /// *index* and nothing about the thing at it, so a tally of them mixes "a `char` field" with "a
    /// `String` constant" under one line. The resolver knows better — it is the one that read the
    /// descriptor — so it writes down what it saw on the way out, and the census reads it back when
    /// the compilation it aborted comes back as an error.
    ///
    /// **Why "the last refusal" is the right one**, and not a heuristic: every one of these
    /// resolvers is consulted through `.ok_or(…)?`, so a `None` from any of them ends the
    /// compilation on that line. There is no path on which a refused field is recorded and the
    /// compilation continues past it. The index is carried and checked against the error's anyway,
    /// so a mismatch degrades to "unattributed" rather than to a wrong answer.
    #[derive(Default)]
    struct Refusals {
        /// `(index, pool tag)` of the last constant no resolver would take.
        constant: Option<(u16, &'static str)>,
        /// `(index, descriptor)` of the last static the stub refused.
        static_field: Option<(u16, String)>,
        /// `(index, descriptor)` of the last instance field the stub refused.
        field: Option<(u16, String)>,
        /// The descriptor of the last field that resolved **as a reference**. `ReferenceWrite`
        /// carries no index at all — it is a rule of the compiler rather than of the resolver — so
        /// this is the only way to say which field it was about.
        reference: Option<String>,
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
    let (mut java_paths, mut lib_paths, mut boot_paths) = (Vec::new(), Vec::new(), Vec::new());
    walk(std::path::Path::new("java"), &mut java_paths);
    walk(std::path::Path::new("KajiLibrary"), &mut lib_paths);
    walk(std::path::Path::new("boot"), &mut boot_paths);
    java_paths.sort();
    lib_paths.sort();
    boot_paths.sort();

    let mut classes: Vec<(String, ClassFile)> = Vec::new();
    let mut units: Vec<(usize, usize)> = Vec::new(); // unit -> (class index, member index)
    let mut bodies: Vec<crate::jvm::parser::Code> = Vec::new(); // unit -> its parsed `Code`
    let mut population: Vec<usize> = Vec::new(); // the units drawn from `java/`, i.e. what is censused
    let mut by_name: BTreeMap<(String, String, String), usize> = BTreeMap::new();
    // Class name -> index in `classes`, so a target can be looked for **up the superclass chain**
    // the way the VM's own resolver does. Without it an inherited method — `Sub.m()` declared on
    // `Base` — is a miss here and refuses the caller, which is an under-count of the compiler
    // rather than a property of it.
    let mut by_class: BTreeMap<String, usize> = BTreeMap::new();
    let mut files = 0usize;

    for (from_java, path) in
        java_paths
            .iter()
            .map(|p| (true, p))
            .chain(lib_paths.iter().map(|p| (false, p)))
            .chain(boot_paths.iter().map(|p| (false, p)))
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
        by_class.entry(name.clone()).or_insert(ci);
        classes.push((name, class));
    }

    let mut methods = 0usize;
    let mut returns_value = 0usize;
    let mut compiled: Vec<String> = Vec::new();
    let mut reasons: BTreeMap<String, usize> = BTreeMap::new();
    // The **split**: the same refusals, attributed to what the resolver actually saw. Kept apart
    // from `reasons` so the coarse table stays comparable with every earlier step's.
    let mut split: BTreeMap<(&'static str, String), usize> = BTreeMap::new();
    let notes = std::cell::RefCell::new(Refusals::default());

    for &root in &population {
        let class = class_of(&classes, &units, root);
        let member = &class.methods[units[root].1];
        methods += 1;
        // The **ceiling**, and every step so far has moved it. It is the set of methods whose
        // *exit* this tier can express: step 5 added `areturn` (methods returning a reference),
        // step 7 added `return` (methods returning `void` — `<init>` and every setter, which
        // together are a large fraction of any real corpus), and the category-2 step added
        // `lreturn`, `freturn` and `dreturn`. Those last three came from changing the **boundary**
        // rather than the opcode set: the old packed `RAX = (status << 32) | value` had 32 bits for
        // a result and no `long` fitted, so moving the value into the caller's buffer is what let
        // `J` in — and once it was 64 type-agnostic bits, `F` and `D` cost nothing more. Nothing is
        // out of the ceiling any more: every descriptor this tier can parse has an exit.
        let descriptor = class.utf8(member.descriptor_index).unwrap_or("");
        let returns = descriptor.rsplit(')').next().unwrap_or("");
        if matches!(returns, "I" | "Z" | "B" | "S" | "C" | "V" | "J" | "F" | "D") || returns.starts_with(['L', '[']) {
            returns_value += 1;
        }
        // A fresh slate per method: what a *previous* method's resolver refused says nothing about
        // this one, and an attribution carried across would be worse than none.
        *notes.borrow_mut() = Refusals::default();
        let result = crate::burst::compile::compile(
            &shape(&classes, &units, &bodies, root),
            &crate::burst::compile::Environment {
                // The four constant resolvers, each of which **writes down what it refused**: a
                // `None` from all of them is the compiler's `NonIntegerConstant`, which names an
                // index and says nothing about what is at it. Recording the pool tag here is what
                // turns that one line into the split table below — and it is free, because the
                // pool is right there.
                int_const: &|unit, index| {
                    let class = class_of(&classes, &units, unit);
                    let answer = class.integer_constant(index);
                    if answer.is_none() {
                        notes.borrow_mut().constant = Some((index, census_constant_tag(class, index)));
                    }
                    answer
                },
                long_const: &|unit, index| {
                    let class = class_of(&classes, &units, unit);
                    let answer = class.long_constant(index);
                    if answer.is_none() {
                        notes.borrow_mut().constant = Some((index, census_constant_tag(class, index)));
                    }
                    answer
                },
                float_const: &|unit, index| {
                    let class = class_of(&classes, &units, unit);
                    let answer = class.float_constant(index).map(f32::to_bits);
                    if answer.is_none() {
                        notes.borrow_mut().constant = Some((index, census_constant_tag(class, index)));
                    }
                    answer
                },
                double_const: &|unit, index| {
                    let class = class_of(&classes, &units, unit);
                    let answer = class.double_constant(index).map(f64::to_bits);
                    if answer.is_none() {
                        notes.borrow_mut().constant = Some((index, census_constant_tag(class, index)));
                    }
                    answer
                },
                static_field: &|unit, index| {
                    // The stub: any static this tier has a representation for resolves, to an
                    // address no code here will run. The **kind** is what the descriptor says,
                    // because it is what fixes the width of the access and therefore what the
                    // census is counting.
                    //
                    // And when it does *not* resolve, the descriptor it read is written down. The
                    // compiler's `UnresolvedStatic` carries a pc and an index, which is exactly the
                    // information that does not distinguish "a `char` static" from "a `long` one"
                    // — and the whole point of the split is that those two answers had different
                    // fates in groups 1 and 4.
                    let (_, _, descriptor) = class_of(&classes, &units, unit).fieldref_target(index)?;
                    match census_field_kind(descriptor) {
                        Some(kind) => {
                            if kind == crate::burst::compile::Kind::Reference {
                                notes.borrow_mut().reference = Some(descriptor.to_string());
                            }
                            Some((poll, kind))
                        }
                        None => {
                            notes.borrow_mut().static_field = Some((index, descriptor.to_string()));
                            None
                        }
                    }
                },
                // The same shape of stub for `getfield`/`putfield`: any instance field of a kind
                // this tier represents resolves, to an offset no code here will touch. Like the
                // static stub it cannot know about a layout that is not loaded, so the census
                // stays an **upper bound** on what a running VM would accept. It *can* now stop
                // caring about `volatile`, which the real resolver no longer refuses either —
                // see `jit_field_site`, VOLATILE-REVISIT-OS-PARALLEL.
                field: &|unit, _, index| {
                    let (_, _, descriptor) = class_of(&classes, &units, unit).fieldref_target(index)?;
                    match census_field_kind(descriptor) {
                        Some(kind) => {
                            // A field that resolves *as a reference* is the one the compiler may
                            // still refuse, one line later, if the opcode writes it
                            // (`Ineligible::ReferenceWrite`). That variant carries no index — it is
                            // the compiler's rule, not the resolver's answer — so this is the only
                            // place the descriptor behind it can be captured.
                            if kind == crate::burst::compile::Kind::Reference {
                                notes.borrow_mut().reference = Some(descriptor.to_string());
                            }
                            Some((0, kind))
                        }
                        None => {
                            notes.borrow_mut().field = Some((index, descriptor.to_string()));
                            None
                        }
                    }
                },
                // The same shape of stub again for `new`: every class resolves, to a small
                // instance and a plausible header word. A running VM additionally requires the
                // class to be *initialised*, which this cannot know — so, once more, an upper
                // bound rather than a prediction.
                instance: &|_, _| Some(crate::burst::compile::Instance { size: 16, class_id: 1 }),
                // ...and for `newarray`/`anewarray`. The **element width is real** — decoded from
                // the `atype` through the interpreter's own table, and 4 for every reference array
                // — because the width is what the emitted arithmetic depends on and stubbing it
                // would make the census measure a different compiler. The mirror offset is a
                // plausible constant: a running VM requires the array class's `Class<…>` to exist
                // already, which this cannot know. Upper bound, exactly like the two stubs above.
                array: &|_, of| {
                    use crate::burst::compile::ArrayOf;
                    use crate::jvm::interpreter::bytecode_interpreter::array_operations;
                    let element = match of {
                        ArrayOf::Primitive(atype) => array_operations::primitive_array_class(atype)?.1,
                        ArrayOf::Reference(_) => 4,
                    };
                    Some(crate::burst::compile::ArrayType { class_id: 1, element: element as u32 })
                },
                // And the same shape once more for the **call** (step 8): a `Methodref` naming a
                // method in the table resolves to that method's body, with no check that the class
                // is initialised or that the method is not `synchronized`, and no walk up the
                // superclass chain for an inherited target. The first two make this an upper bound
                // like every stub above; the third makes it a slight *under*-count, and is left as
                // the simpler thing because a direct hit is what `super()` and every constructor
                // call already are.
                //
                // **Milestone F2 widens it to all four invokes.** A dispatched call
                // (`invokevirtual`, `invokeinterface`) is bound in a running VM to the class the
                // interpreter has actually seen at that site; the census has run nothing, so it
                // answers with the site's **static** owner and a stand-in mirror. That keeps it the
                // upper bound it has always been — a real VM additionally needs the site to have
                // executed, and needs the class it saw to be the one whose body is expanded — and
                // it is the same licence the `new`, `newarray` and field stubs already take.
                invoke: &|unit, pc, index| {
                    let class = class_of(&classes, &units, unit);
                    let code = &bodies[unit].code;
                    let op = *code.get(pc)?;
                    if !matches!(op, 0xb6..=0xb9) {
                        return None;
                    }
                    let (owner, name, desc) = class.methodref_target(index)?;
                    // Resolution walks **up** from the owner, as JVMS §5.4.3.3 does: `Sub.m()` may
                    // well be `Base.m()`. Bounded by the chain's length, and a class outside the
                    // corpus simply ends the walk — the census stays an upper bound on a *loaded*
                    // world, never a claim about one it cannot see.
                    let mut owner = owner.to_string();
                    let target = loop {
                        if let Some(&unit) = by_name.get(&(owner.clone(), name.to_string(), desc.to_string())) {
                            break unit;
                        }
                        let ci = *by_class.get(&owner)?;
                        let up = classes[ci].1.class_name(classes[ci].1.super_class)?;
                        if up == owner {
                            return None;
                        }
                        owner = up.to_string();
                    };
                    Some(crate::burst::compile::Callee {
                        method: shape(&classes, &units, &bodies, target),
                        arg_slots: census_arg_slots(desc, op != 0xb8),
                        guard: match op {
                            0xb6 | 0xb9 => crate::burst::compile::Guard::ExactClass(1),
                            _ => crate::burst::compile::Guard::Static,
                        },
                    })
                },
                heap: CENSUS_HEAP,
                // The `checkcast`/`instanceof`/`ldc Foo.class` stub: **any `CONSTANT_Class`**
                // resolves, to a plausible mirror offset no code here compares against. Asking
                // `class_name` rather than answering unconditionally is what keeps the two `ldc`
                // stubs apart, so each is counted against its own pool tag. A running VM
                // additionally requires the mirror to exist already, which this cannot know: upper
                // bound, exactly like the `new` and `newarray` stubs above.
                class_mirror: &|unit, index| class_of(&classes, &units, unit).class_name(index).map(|_| 1),
                // The `ldc "…"` stub, and an upper bound for the same reason: **any
                // `CONSTANT_String`** resolves, where a running VM would additionally require the
                // literal to be in the pool already. What it can be at all is FZ-008's doing — the
                // pool is one instance per literal, in Old, a GC root and pinned, so a literal has
                // a permanent address to bake.
                string_literal: &|unit, index| {
                    class_of(&classes, &units, unit).string_constant(index).map(|_| 1)
                },
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
                // ...and the three variants that *do* mix incompatible causes get a second line
                // saying which cause it was. The index in the note is checked against the error's
                // rather than assumed: an attribution that cannot be proved is reported as one that
                // was not made, which is the only honest degradation for a table like this.
                use crate::burst::compile::Ineligible as I;
                let note = notes.borrow();
                let detail = match other {
                    I::UnresolvedStatic { index, .. } => Some((
                        "UnresolvedStatic",
                        match &note.static_field {
                            Some((at, d)) if *at == index => {
                                format!("{:<26} {}", census_short_descriptor(d), census_field_cause(d))
                            }
                            _ => format!("#{index:<25} the fieldref itself does not resolve"),
                        },
                    )),
                    I::UnresolvedField { index, .. } => Some((
                        "UnresolvedField",
                        match &note.field {
                            Some((at, d)) if *at == index => {
                                format!("{:<26} {}", census_short_descriptor(d), census_field_cause(d))
                            }
                            _ => format!("#{index:<25} the fieldref itself does not resolve"),
                        },
                    )),
                    I::NonIntegerConstant { index, .. } => Some((
                        "NonIntegerConstant",
                        match &note.constant {
                            Some((at, tag)) if *at == index => {
                                format!("{:<26} {}", *tag, census_constant_cause(tag))
                            }
                            _ => format!("#{index:<25} unattributed"),
                        },
                    )),
                    I::ReferenceWrite { .. } => Some((
                        "ReferenceWrite",
                        match &note.reference {
                            Some(d) => format!(
                                "{:<26} {}",
                                census_short_descriptor(d),
                                "a reference field: needs the GC write barrier"
                            ),
                            None => "unattributed".to_string(),
                        },
                    )),
                    _ => None,
                };
                if let Some((variant, detail)) = detail {
                    *split.entry((variant, detail)).or_default() += 1;
                }
            }
        }
    }

    let mut ranked: Vec<_> = reasons.into_iter().collect();
    ranked.sort_by(|a, b| b.1.cmp(&a.1).then(a.0.cmp(&b.0)));

    eprintln!();
    eprintln!("F3 — compiled-subset census over {files} class files (wide types in: long, float, double)");
    eprintln!("methods with a Code attribute: {methods}");
    eprintln!(
        "...of those, with an expressible exit: {returns_value}   <- the ceiling: every return opcode"
    );
    eprintln!(
        "methods that compile:          {}   ({:.0}% of the ceiling)",
        compiled.len(),
        100.0 * compiled.len() as f64 / returns_value.max(1) as f64
    );
    eprintln!();
    eprintln!("every reason for refusing the rest, most common first:");
    for (reason, count) in &ranked {
        eprintln!("  {count:>5}  {reason}");
    }

    // **The split.** Four of the variants above name a position and an index and nothing about the
    // thing at it, so their counts mix causes with nothing in common: a `char` field and a `long`
    // one, a `String` constant and a `MethodHandle`. This is the same refusals, attributed.
    eprintln!();
    eprintln!("the four variants that mix causes, split by what the resolver actually saw:");
    // A fixed order with the **zeros printed**. A variant that has stopped happening is the most
    // valuable row in this table — it is what a widening was for — and leaving it out would make it
    // indistinguishable from one nobody instrumented.
    for variant in ["UnresolvedStatic", "UnresolvedField", "NonIntegerConstant", "ReferenceWrite"] {
        let mut rows: Vec<_> = split.iter().filter(|((v, _), _)| *v == variant).collect();
        rows.sort_by(|a, b| b.1.cmp(a.1).then(a.0.cmp(b.0)));
        let total: usize = rows.iter().map(|(_, n)| **n).sum();
        eprintln!("  {variant} — {total}");
        match rows.is_empty() {
            true => eprintln!("      (none left over this corpus)"),
            false => {
                for ((_, detail), count) in rows {
                    eprintln!("      {count:>4}  {detail}");
                }
            }
        }
    }
    // The two causes this table structurally **cannot** see, said plainly rather than left as a
    // silent zero. Both are properties of a *running* VM, and the census has none behind it:
    //
    //  - **an uninitialised declaring class.** `getstatic`, `putstatic`, `new` and an inlined call
    //    all require it, and the stubs above accept unconditionally. This is the same "upper bound"
    //    caveat the header carries, restated where it would otherwise be mistaken for a zero.
    //  - **`volatile`.** It stopped being a refusal at all in group 2 — the real resolver answers
    //    for a volatile field like any other, and the emitter uses a plain `mov` on the strength of
    //    the substrate rather than of x86-TSO. So its absence here is not blindness; there is
    //    nothing to see. (VOLATILE-REVISIT-OS-PARALLEL: that changes the day this tier runs on the
    //    parallel substrate.)
    eprintln!();
    eprintln!("  not visible to a static census, and not zero: a declaring class that is not");
    eprintln!("  initialised yet (every stub above accepts unconditionally). `volatile` is not");
    eprintln!("  in the list because it stopped being a refusal in group 2, not because it is");
    eprintln!("  invisible here.");
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
// Group 2: reference fields, `volatile`, `checkcast`/`instanceof`, and the class literal.
// =============================================================================================

#[test]
fn richer_references_agree_with_the_interpreter() {
    // `JxRich` is group 2's coverage file, and each of its methods is one question:
    // `chain`/`viaStatic` (a reference out of a `getfield`/`getstatic`, dereferenced again),
    // `nullable` (a null reference loaded and only tested — which must not deopt),
    // `readVi`/`bumpVi`/`readVl`/`setVl`/`readVstat`/`bumpVstat`/`readVr` (`volatile` at every
    // width the subset has, plus a reference **read**), `cast`/`isLeaf` (the exact class natively,
    // a genuine subtype and a failure by deopt) and `leafClass` (a pinned mirror by identity).
    // 353090 is what `java JxRich` of JDK 25 prints.
    let stats = differential("java/JxRich.class", 353_090);
    // All thirteen helpers, and nothing else: `run` allocates and invokes, and the four
    // constructors run twice each — far short of the threshold — so they are never even scanned.
    assert_eq!(stats.compiled, 13, "every helper in the file compiles");
    assert_eq!(stats.rejected, 1, "`run`, scanned once from its own back-edge and refused");
    assert_eq!(stats.unmarshallable, 0, "every local here is a reference, an int or a long");
    // **The deopt count is the assertion about `checkcast`, and it is exact.** Four calls per round
    // are not an exact class hit — `cast(sub)`, `cast(other)`, `isLeaf(sub)`, `isLeaf(other)` — and
    // every one of them after its method goes native must give up. `cast` is called three times a
    // round, so it is warm from round 10 (21 of its bad calls are still interpreted); `isLeaf` four
    // times, so it is warm from round 7 (15 interpreted). 800 - 21 + 800 - 15.
    //
    // What the number also says is what is **not** in it. `nullable` reads a reference field that
    // is `null` on half its calls and never deopts; `isLeaf(null)` answers `0` natively; and
    // `readVr(withNull)` returns `null` out of a compiled method. A compiler that treated a zero
    // reference as a guard failure would add 1200 to this count.
    assert_eq!(stats.deopts, 779 + 785, "one per non-exact cast or type test, and nothing else");
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
    // `pass`, and — since group 3's first stage — `run`: its callee loops, which used to refuse the
    // expansion outright, and the `newarray` it does is only an *exit*, never a refusal.
    assert_eq!(array.compiled, 2, "`pass`, and `run` now that a looping callee may be inlined");
    assert_eq!(array.rejected, 0, "`run` is no longer refused");
    assert_eq!(array.deopts, 0, "no index is ever out of range");
    // **Thirty-three, not a thousand.** The thousand invokes are inside `run`, which is now itself
    // compiled with `pass` expanded into it, so the interpreter never executes them.
    assert_eq!(array.native_calls, 33);
    assert_eq!(array.osr_entries, 2, "one per compiled method, each hot from inside its own loop");

    let field = differential("java/JdField.class", 685_184);
    // `churn`, plus `java.lang.Object.<init>` (a bare `return`, in the subset since step 7) which
    // every `new JdBox()` in `run` reaches — and, since group 3's first stage, `run` itself: it
    // allocates, but an allocation is an *exit* (`Status::ALLOC`), never a refusal, and what did
    // refuse it was `churn`'s loop.
    assert_eq!(field.compiled, 3, "`churn`, `Object.<init>`, and `run`");
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
    // `walk`, `carry`, `sameness`, `Object.<init>` (a bare `return`) — and, since group 3's first
    // stage, `run`, whose looping callees may now be expanded into it.
    assert_eq!(quiet.compiled, 5, "the four, and `run` now that a looping callee may be inlined");
    // `run` allocates, so it leaves through `Status::ALLOC` every time Eden's fast path or the
    // excursion's allocation log fills — and an allocation exit deliberately does *not* close
    // on-stack entry, so each one is followed by another entry at the loop header.
    assert_eq!(quiet.osr_entries, 41);
    assert_eq!(quiet.alloc_exits, 38, "`run`'s allocations, resumed and re-entered");
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
// Group 3: the calls that were not inlinable — a loop inside an expansion, and the inline cache.
// =============================================================================================

#[test]
fn an_inlined_loop_polls_and_hands_back_the_frame_it_removed() {
    // Stage 1's whole claim, asked end to end. `JcLoop.inner` loops, which until this stage refused
    // its expansion outright and every caller with it; now its header carries a poll, and a poll
    // taken there has to materialise the frame `inner` never had — plus the caller's, which in
    // `flat` is holding a live operand at the time (the first call's result). 433870 is what
    // `java JcLoop` of JDK 25 prints.
    use std::sync::atomic::Ordering;

    let quiet = differential("java/JcLoop.class", 433_870);
    // `inner`, `flat`, `nested`, `sameness`, `run` — and `java.lang.Object.<init>`, a bare `return`
    // reached by every `new JcLoop(…)`. `inner` compiles on its own account as well as being
    // expanded into the other two, because `run` calls the pair often enough to make it hot before
    // either of them is.
    assert!(quiet.compiled >= 4, "only {} compiled — was anything inlined?", quiet.compiled);
    assert_eq!(quiet.deopts, 0, "nothing here is null and no index is out of range");
    assert_eq!(quiet.unmarshallable, 0);

    // Now with the poll **held up for the whole run**, which is the interesting arm: every entry
    // into `flat` or `nested` reaches an inlined loop header, finds the poll set, and leaves
    // through a stub that has to spill two frames' operand stacks and name a resume site that no
    // bytecode pc of the root can name. Thousands of times.
    let (value, stats) = with_poll_on("java/JcLoop.class", |poll| poll.store(1, Ordering::Release));
    assert_eq!(value, 433_870, "the answer must not depend on when the poll fires");
    assert!(
        stats.safepoint_exits > 500,
        "only {} exits — did an inlined header ever poll?",
        stats.safepoint_exits
    );
    // **The sharp assertion.** A poll at a *root* loop header rebuilds nothing: the locals were
    // written through and the header's stack is empty. Only a poll inside an expansion produces a
    // frame that did not exist, so a non-zero count here is the proof that stage 1's mechanism
    // fired at all — without it this test would pass just as happily against a compiler that still
    // refused every looping callee.
    assert!(
        stats.virtual_frames > 100,
        "only {} frames rebuilt — did a poll ever fire *inside* an expansion?",
        stats.virtual_frames
    );
    // The identity checks inside `run` are what make this more than an arithmetic agreement: the
    // interpreter collects in the gaps between the poll firing and native code being re-entered,
    // so a reference handed back with the wrong tag would be a stale offset by the next round.
    assert_eq!(stats.unmarshallable, 0, "a reference must marshal, in both directions");
    assert_eq!(stats.deopts, 0, "a poll is not a guard failure");
}

#[test]
fn the_inline_cache_hits_a_monomorphic_site_and_deopts_a_drifting_one() {
    // Stage 2 (milestone F2), and the one failure mode it has: running the **wrong body**. `JcIc`
    // warms three dispatched sites with a `JcAlpha` — which is the class each guard is compiled
    // against — and then hands two of them a `JcBeta` that overrides both halves with different
    // arithmetic. A cache without a guard, or with a guard that does not deopt, computes
    // `JcAlpha`'s answer for a `JcBeta` and the sum moves. 624722 is what `java JcIc` of JDK 25
    // prints, and the interpreter arm is checked against it too.
    let stats = differential("java/JcIc.class", 624_722);
    // **The guard fires, in bulk.** The drifting half of the run is 40 rounds × 200 iterations at
    // each of two sites, and every one of those calls is a miss: the receiver is a `JcBeta` and the
    // baked class is `JcAlpha`. A count in the thousands is the proof that the compiled code really
    // did contain a class check rather than an unconditional expansion.
    assert!(stats.deopts > 5_000, "only {} deopts — did the guard ever miss?", stats.deopts);
    assert!(stats.compiled > 0);
    // **And it fires at the inner level too.** Most of those misses are at a site in the root's own
    // body, where a deopt rebuilds nothing. `relay` is the one that is not: its own receiver never
    // changes class, so `through` is expanded — and the call *inside* that expansion is guarded on
    // a receiver that does drift. A miss there has to materialise a frame that never existed, one
    // per entry into `relay` (the interpreter finishes the call from there), so this counts the
    // forty-odd entries in the drifting half. Zero would mean nothing was ever expanded at all.
    assert!(
        stats.virtual_frames > 20,
        "only {} frames rebuilt — did a guard inside an expansion ever miss?",
        stats.virtual_frames
    );
    assert_eq!(stats.unmarshallable, 0, "a receiver marshals as its heap offset");
}

#[test]
fn a_call_site_that_never_ran_no_longer_refuses_the_method() {
    // The **cold call site**. `JcCold.hot` loops two hundred times per call and never once takes the
    // branch that calls `cold`, so that site's F0 cache cell stays at zero — and a zero used to
    // refuse the whole method, because the cache is where the compiler learns what an invoke binds
    // to. A statically bound call needs nothing from the receiver, so the metaspace can answer it
    // read-only instead (see `MetaspaceService::resolved_call_readonly`), and this is the test that
    // says it does: without the fallback `hot` is refused, `run` is refused with it, and nothing is
    // compiled at all.
    //
    // 229973 is what `java JcCold` of JDK 25 prints. The second half of `run` then **takes** the
    // branch, so the body compiled for a site that had never executed is also the body that runs.
    let stats = differential("java/JcCold.class", 229_973);
    assert_eq!(stats.compiled, 2, "`hot`, and `run` with it expanded inside");
    assert_eq!(stats.rejected, 0, "a cold site is no longer a refusal");
    assert_eq!(stats.deopts, 0, "nothing here guards on anything");
}

#[test]
fn the_harness_shapes_go_through_the_inline_cache() {
    // The benchmark harness's own polymorphism (`BmShape`/`BmSq`/`BmCir`/`BmTri`), asked in a shape
    // this tier's subset can express — `BmVirtual.run` reads its receivers out of a `BmShape[]` and
    // `aaload` is outside the subset, so that method does not compile and F2 leaves the control
    // workload exactly where it was.
    //
    // 745120 is what `java JcShapes` of JDK 25 prints.
    let stats = differential("java/JcShapes.class", 745_120);
    // **Forty deopts, and the number is the whole result.** `steady` is monomorphic and is called
    // 40 × 300 times: not one of those misses. `rotate` is genuinely polymorphic — one call site,
    // three receiver classes, rotating — and is entered 40 times: each entry runs until the first
    // receiver that is not the baked class, deopts once, and is interpreted from there. So a
    // polymorphic site costs one deopt per entry and a monomorphic one costs none, which is
    // precisely the trade a monomorphic cache makes.
    assert_eq!(stats.deopts, 40, "one per entry into `rotate`; `steady` never misses");
    assert_eq!(stats.unmarshallable, 0);
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

    let mut metaspace = MetaspaceService::new(boot_class_path(), vec![PathBuf::from("java")]);
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
            long_const: &|_, _| None,
            float_const: &|_, _| None,
            double_const: &|_, _| None,
            static_field: &|_, _| None,
            field: &|_, _, _| None,
            instance: &|_, i| {
                objects_operations::jit_instance(&metaspace, "BmField", i)
                    .map(|(size, class_id)| Instance { size, class_id })
            },
            array: &|_, _| None,
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
                array_data: 12,
                int_element: 4,
            },
            class_mirror: &|_, _| None,
            string_literal: &|_, _| None,
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
        // The status word says only *that* it returned; the reference is in the result slot, which
        // is where the boundary contract puts every returned value.
        Outcome::Returned => buffer[compiled.result_base as usize] as usize,
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

    let mut metaspace = MetaspaceService::new(boot_class_path(), vec![PathBuf::from("java")]);
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




// ---------------------------------------------------------------------------------------------
// The `long` group (F3, category-2 types). Two workloads, and they ask different questions: does
// the arithmetic follow the JLS where it differs from `int`'s, and does the *value* survive every
// crossing of the boundary. Both expectations are what a real `java` of JDK 25 prints.
// ---------------------------------------------------------------------------------------------

#[test]
fn long_semantics_agree_with_the_interpreter_and_with_the_jls() {
    // Twenty observations, each one a place where carrying an `int`'s habits over to a `long`
    // gives a plausible wrong answer: a `movsxd` after the arithmetic, a 5-bit shift mask, an
    // unguarded `idiv`. 1048575 is what `java JwSem` prints.
    let stats = differential("java/JwSem.class", 1_048_575);
    // The two `x / 0` calls deopt, exactly as their `int` twins do in `JtSem`: native code refuses,
    // the interpreter re-runs the instruction and throws, and the Java `catch` sees it. That the
    // score includes those bits is the proof that a `long` deopt is invisible to the program.
    assert_eq!(stats.deopts, 2, "one deopt per division by zero, and none for MIN / -1");
    assert_eq!(stats.unmarshallable, 0, "a `long` local marshals like any other");
}

#[test]
fn a_long_survives_every_crossing_of_the_boundary() {
    // Twelve observations across the four crossings a `long` can make — a deopt with `long`s live
    // on the operand stack, a safepoint poll out of a compiled loop with a `long` accumulator,
    // 8-byte fields and statics, and on-stack entry into a loop already part-way along. Every
    // constant has bits in both halves, so a value reconstructed from one slot or spilled four
    // bytes wide is wrong rather than merely suspicious. 4095 is what `java JwState` prints.
    let stats = differential("java/JwState.class", 4_095);
    assert!(stats.osr_entries > 0, "the 200000-lap loops must be entered on-stack");
    assert_eq!(stats.deopts, 1, "the one zero divisor, and nothing else gives up");
}

#[test]
fn float_and_double_semantics_agree_with_the_interpreter_and_with_the_jls() {
    // Nineteen observations across the four places IEEE-754 behaves like nothing else in this
    // subset: single precision really being single (one prefix byte away from double), a NaN
    // comparing unordered (all `fcmpl`/`fcmpg` disagree about), a signed zero (invisible to `==`,
    // and what makes `fneg` a bit flip rather than a subtraction), and a division that answers
    // infinity where the integer arms deopt. 524287 is what `java JwFloat` prints.
    let stats = differential("java/JwFloat.class", 524_287);
    // **The `frem`/`drem` deopts, and nothing else gives up.** There is no SSE scalar remainder, so
    // those two opcodes compile to an unconditional deopt and the interpreter finishes the method —
    // which is why the score includes their bit at all. Every other floating-point operation here
    // ran natively.
    assert!(stats.deopts > 0, "frem/drem must be reaching their deopt");
    assert_eq!(stats.unmarshallable, 0, "a float and a double marshal like any other value");
}


// ---------------------------------------------------------------------------------------------
// Array allocation (`newarray` / `anewarray`). What is new here, and it is the whole of it: the
// size is an **operand**. Everything below is an observation that can only go wrong because of
// that — a count that is negative, a count too big to zero inline, a payload whose length is not a
// multiple of eight, and a length word that must carry the count the program asked for rather than
// the one the arena rounded to.
// ---------------------------------------------------------------------------------------------

#[test]
fn array_allocation_agrees_with_the_interpreter() {
    // `JaArray` asks all five at once and folds every answer into one number, so a single wrong
    // byte anywhere moves it. 549311 is what `java JaArray` prints.
    let stats = differential("java/JaArray.class", 549_311);
    // `fill`, `chars`, `bytes`, `refs`, `big`, `neg` — every method whose body is an allocation and
    // the arithmetic around it. Not `scanChars`/`scanBytes`/`countNulls` (`caload`, `baload` and
    // `aaload` are outside the subset), and not `run` (a `try`/`catch` around invokes).
    assert_eq!(stats.compiled, 6);
    assert_eq!(stats.rejected, 4, "the three scanners and `run`");
    // **The negative count is not a deopt**, and that is deliberate rather than incidental: the
    // guard rides the same stub as the Eden-full one, which reports `Status::ALLOC`. The two are
    // the same rebuilt state resumed at the same instruction, so what the interpreter does next is
    // identical; the difference is only which counter moves, and `ALLOC` is the one that does not
    // retire the enclosing loop. See `burst::compile`'s `newarray` arm.
    assert_eq!(stats.deopts, 0, "nothing here fails a guard that is about a *value*");
    // Two reasons to leave, and the count says which one dominates: `big`'s 1200-int array is 4812
    // bytes, over `MAX_INLINE_ARRAY_BYTES`, so **every** call to it after the 32 that made it hot
    // leaves — 368 of the 400 rounds — and `neg(-1)` adds the one negative count. That the number
    // is ~368 rather than ~400 is itself the statement that a method is entered only once hot.
    assert!(
        (360..=380).contains(&stats.alloc_exits),
        "one exit per round for `big`, plus the negative count: got {}",
        stats.alloc_exits
    );
    assert_eq!(stats.unmarshallable, 0, "an array reference marshals like any other reference");
}

// =============================================================================================
// Array allocation, against the **real** heap and the **real** collector.
//
// The differential above says the whole VM computes the same number with the JIT as without it.
// These say the things a differential structurally cannot: what the bytes in Eden look like
// immediately after native code wrote them, and whether the collector can see, type, size, move
// and remap an array it did not allocate.
// =============================================================================================

/// Compiles `iload_0; newarray <atype>; areturn` — a whole method whose only instruction that
/// matters is the allocation — against a real `HeapService`.
///
/// The `array` resolver is the VM's own, so this drives the real path: `array_class_mirror` must
/// already have been called for the class, or the resolver answers `None` and the method is refused
/// (which is checked at the call sites rather than assumed).
#[cfg(windows)]
fn compile_newarray(
    heap: &crate::jvm::interpreter::heap::HeapService,
    metaspace: &MetaspaceService,
    atype: u8,
    descriptor: &str,
) -> Result<crate::burst::compile::CompiledCode, crate::burst::compile::Ineligible> {
    use crate::burst::compile::{ArrayOf, ArrayType, Environment, Method};
    use crate::jvm::interpreter::bytecode_interpreter::array_operations;

    let bases = heap.jit_bases();
    let code = [0x1a, 0xbc, atype, 0xb0]; // iload_0; newarray atype; areturn
    crate::burst::compile::compile(
        &Method { unit: 0, code: &code, max_locals: 1, descriptor, is_static: true, has_handlers: false },
        &Environment {
            int_const: &|_, _| None,
            long_const: &|_, _| None,
            float_const: &|_, _| None,
            double_const: &|_, _| None,
            static_field: &|_, _| None,
            field: &|_, _, _| None,
            instance: &|_, _| None,
            array: &|_, of| {
                let ArrayOf::Primitive(atype) = of else { return None };
                let (class, _) = array_operations::primitive_array_class(atype)?;
                array_operations::jit_array_class(metaspace, class)
                    .map(|(class_id, element)| ArrayType { class_id, element })
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
                array_length: array_operations::LENGTH_OFFSET as u32,
                array_data: array_operations::ARRAY_HEADER_SIZE as u32,
                int_element: array_operations::array_element_width("[I") as u32,
            },
            class_mirror: &|_, _| None,
            string_literal: &|_, _| None,
            poll_word: &JIT_GC_POLL as *const _ as usize,
        },
    )
}

/// Runs `compiled` with `count` in local 0, replays its allocation log into `heap` exactly as the
/// trampoline does, and returns `(reference, logged_size)`. Panics unless the allocation took its
/// fast path — a `Status::ALLOC` here would mean the test measured the interpreter.
#[cfg(windows)]
fn run_newarray(
    compiled: &crate::burst::compile::CompiledCode,
    heap: &crate::jvm::interpreter::heap::HeapService,
    count: i64,
) -> (usize, usize) {
    use crate::burst::compile::{Outcome, Status};
    use crate::burst::exec_mem::ExecMem;

    let mem = ExecMem::from_code(&compiled.code).expect("map W^X");
    let mut buffer = vec![0i64; compiled.buffer_slots as usize + 1];
    buffer[0] = count; // local 0 — the count, marshalled as the interpreter marshals an `Int`
    // SAFETY: the same contract `JitCache::enter` satisfies — a live `[i64]` at least
    // `buffer_slots` long, and an entry pc of 0.
    let f: extern "system" fn(*mut i64, i64) -> i64 = unsafe { mem.as_fn() };
    let array = match Status::unpack(f(buffer.as_mut_ptr(), 0)) {
        Outcome::Returned => buffer[compiled.result_base as usize] as usize,
        other => panic!("the allocation should have taken its fast path, got {other:?}"),
    };
    let base = compiled.alloc_base as usize;
    assert_eq!(buffer[base], 1, "exactly one array was logged");
    assert_eq!(buffer[base + 1] as usize, array, "...and it is the one that came back");
    let size = buffer[base + 2] as usize;
    for r in 0..buffer[base] as usize {
        heap.log_jit_allocation(buffer[base + 1 + 2 * r] as usize, buffer[base + 2 + 2 * r] as usize);
    }
    (array, size)
}

/// **The zeroing test, and the only place the 8-byte rounding is observable.**
///
/// A `char[3]` is 18 logical bytes — `[class_id(4) | mark(4) | length(4) | 3 * 2]` — and the arena
/// reserves 24. Its three elements sit at byte 12, 14 and **16**, which is past `18 & !7 = 16`: so
/// a zeroing loop that rounded the wrong way would leave the last element holding whatever the
/// previous occupant of those bytes left behind, and every *other* element would still look right.
///
/// Making that observable needs Eden to be **dirty**, which is arranged rather than hoped for: the
/// front of the arena is filled with `0xFF` through the interpreter's own accessors and then
/// recycled by a collection with no live roots, which resets the cursor and leaves the bytes
/// exactly where they were. The next reservation lands on top of a known non-zero pattern.
#[test]
#[cfg(windows)]
fn a_compiled_array_is_zeroed_to_the_rounded_stride() {
    use crate::jvm::interpreter::bytecode_interpreter::array_operations::{
        self, ARRAY_HEADER_SIZE, LENGTH_OFFSET,
    };
    use crate::jvm::interpreter::gc;
    use crate::jvm::interpreter::heap::HeapService;

    let mut metaspace = MetaspaceService::new(boot_class_path(), vec![PathBuf::from("java")]);
    let mut heap = HeapService::new();
    // The mirror has to exist before anything is compiled — minting one allocates, and a
    // compilation may not. The refusal is checked rather than assumed.
    assert!(
        array_operations::jit_array_class(&metaspace, "[C").is_none(),
        "an array class with no mirror yet must not be allocatable by compiled code"
    );
    assert!(
        compile_newarray(&heap, &metaspace, 5, "(I)[C").is_err(),
        "...and a method containing that `newarray` must be refused outright"
    );
    array_operations::array_class_mirror(&mut metaspace, &mut heap, "[C");

    // ---- dirty Eden, then recycle it -------------------------------------------------------
    let scratch = heap.malloc(64);
    for b in 0..64 {
        heap.write_u8(scratch + b, 0xFF);
    }
    heap.commit_pending();
    let mut threads = vec![rooted_thread(vec![Frame::new(0, 1, vec![Value::Int(0)])])];
    gc::minor(&metaspace, &mut heap, &mut threads, 15, &mut []);
    assert_eq!(heap.read_u8(scratch), 0xFF, "recycling Eden resets the cursor, not the bytes");

    // ---- allocate a `char[3]` in native code -----------------------------------------------
    let compiled = compile_newarray(&heap, &metaspace, 5, "(I)[C").expect("`[C` has a mirror now");
    let (array, size) = run_newarray(&compiled, &heap, 3);
    assert_eq!(array, scratch, "the reservation lands exactly where the dirty bytes are");

    // **The length word carries the count the program asked for** — not the rounded stride, and
    // not a byte count. Three is the number `arraylength` has to answer.
    assert_eq!(heap.read_u32(array + LENGTH_OFFSET), 3);
    // The logical size — 12 + 6 — is what the collector copies and what the interpreter's own
    // `allocate_array` would have logged. Not 24.
    assert_eq!(size, ARRAY_HEADER_SIZE + 3 * 2, "the log carries the logical size, not the stride");
    // Every element is zero, **including the third**, which begins at byte 16 and would survive a
    // loop that stopped at `size & !7`.
    for i in 0..3 {
        assert_eq!(heap.read_u16(array + ARRAY_HEADER_SIZE + 2 * i), 0, "element {i}");
    }
    // And the slack up to the reserved stride is zero too. Nothing reads it, which is exactly why
    // it is worth pinning: it is the part of the claim no other test can notice being wrong.
    for b in ARRAY_HEADER_SIZE + 6..24 {
        assert_eq!(heap.read_u8(array + b), 0, "slack byte {b}");
    }
    // The bytes past the reservation are **still dirty**, which is the other half of the same
    // claim: the loop zeroed our block and not one byte more.
    assert_eq!(heap.read_u8(array + 24), 0xFF, "the next object's bytes are not ours to zero");
}

/// **The GC-integration test for arrays**, and the counterpart of
/// [`an_object_allocated_by_compiled_code_survives_a_minor_collection`]: an array native code
/// allocated must be indistinguishable, to the collector, from one the interpreter allocated —
/// *scannable* (its header names its array class), *evacuable* (its logged size covers the header,
/// the length word and the payload) and *remappable* (the root holding it is rewritten).
///
/// The **length** is the part an object cannot test: it is a word the collector never interprets
/// and copies blindly, so a logged size that omitted it — or a length written before the zeroing
/// rather than after — would come out the far side as a zero-length array, and every read of it
/// would then be an `ArrayIndexOutOfBoundsException` a long way from here.
#[test]
#[cfg(windows)]
fn an_array_allocated_by_compiled_code_survives_a_minor_collection() {
    use crate::jvm::interpreter::bytecode_interpreter::array_operations::{
        self, ARRAY_HEADER_SIZE, LENGTH_OFFSET,
    };
    use crate::jvm::interpreter::gc;
    use crate::jvm::interpreter::heap::HeapService;

    let mut metaspace = MetaspaceService::new(boot_class_path(), vec![PathBuf::from("java")]);
    let mut heap = HeapService::new();
    let class_id = array_operations::array_class_mirror(&mut metaspace, &mut heap, "[I") as u32;

    // ---- allocate an `int[4]` in native code -----------------------------------------------
    let bases = heap.jit_bases();
    let compiled = compile_newarray(&heap, &metaspace, 10, "(I)[I").expect("`[I` has a mirror");
    let (array, size) = run_newarray(&compiled, &heap, 4);
    // The compiler's answer must be the interpreter's: header, length word and four ints, which is
    // exactly what `allocate_array` lays out and therefore exactly what the collector will copy.
    assert_eq!(size, ARRAY_HEADER_SIZE + 4 * 4, "header, length word and four ints");
    assert_eq!(heap.read_u32(array), class_id, "the header compiled code wrote types the array");
    assert_eq!(heap.read_u32(array + LENGTH_OFFSET), 4);

    assert!(array >= bases.null_page && array < bases.eden_end, "allocated in Eden");
    assert!(!heap.allocations().iter().any(|a| a.offset == array), "not logged until committed");
    heap.commit_pending();
    let entry = heap.allocations().iter().find(|a| a.offset == array).expect("committed");
    assert_eq!(entry.size, size, "the collector will copy exactly this many bytes");

    // ---- elements written through the *interpreter's* accessor -----------------------------
    // So "intact" below is a claim about bytes rather than about the compiler agreeing with itself.
    for i in 0..4u32 {
        heap.write_u32(array + ARRAY_HEADER_SIZE + 4 * i as usize, 0x1000_0000 + i);
    }

    // ---- collect, with the reference held in a frame ---------------------------------------
    let mut threads = vec![rooted_thread(vec![Frame::new(0, 1, vec![Value::Reference(array)])])];
    let report = gc::minor(&metaspace, &mut heap, &mut threads, 15, &mut []);
    assert!(report.copied > 0, "something was evacuated");

    // ---- survived, moved, intact, and remapped ---------------------------------------------
    let moved = match threads[0].frames[0].locals()[0] {
        Value::Reference(offset) => offset,
        other => panic!("the root stopped being a reference: {other:?}"),
    };
    assert_ne!(moved, array, "a minor collection evacuates out of Eden");
    assert!(moved >= bases.eden_end, "...and the new home is outside Eden");
    assert_eq!(heap.read_u32(moved), class_id, "the header travelled with it");
    assert_eq!(heap.read_u32(moved + LENGTH_OFFSET), 4, "and so did the length word");
    for i in 0..4u32 {
        let at = moved + ARRAY_HEADER_SIZE + 4 * i as usize;
        assert_eq!(heap.read_u32(at), 0x1000_0000 + i, "element {i}");
    }
    assert!(heap.allocations().iter().any(|a| a.offset == moved && a.size == size));
    // Nothing dangles: the post-collection verifier walks every frame and every object slot.
    gc::verify_heap(&metaspace, &heap, &threads);
}

/// The counterpart, and the one that would fail silently in production: an **array** native code
/// allocated but the trampoline did not log is invisible to the collector, and the verifier says
/// so. The object twin of this test is not enough on its own — an array's log record is the one
/// whose `size` is computed at run time, so it is the one a refactor can get wrong without touching
/// a constant.
#[test]
#[cfg(windows)]
#[should_panic(expected = "DANGLING")]
fn an_unlogged_array_is_exactly_the_corruption_the_replay_prevents() {
    use crate::burst::compile::{Outcome, Status};
    use crate::burst::exec_mem::ExecMem;
    use crate::jvm::interpreter::bytecode_interpreter::array_operations;
    use crate::jvm::interpreter::gc;
    use crate::jvm::interpreter::heap::HeapService;

    let mut metaspace = MetaspaceService::new(boot_class_path(), vec![PathBuf::from("java")]);
    let mut heap = HeapService::new();
    array_operations::array_class_mirror(&mut metaspace, &mut heap, "[I");
    let compiled = compile_newarray(&heap, &metaspace, 10, "(I)[I").expect("`[I` has a mirror");

    // Run it and deliberately *drop* the log — the one line `JitCache::enter` must never lose.
    let mem = ExecMem::from_code(&compiled.code).expect("map W^X");
    let mut buffer = vec![0i64; compiled.buffer_slots as usize + 1];
    buffer[0] = 4;
    // SAFETY: as in `run_newarray`.
    let f: extern "system" fn(*mut i64, i64) -> i64 = unsafe { mem.as_fn() };
    let array = match Status::unpack(f(buffer.as_mut_ptr(), 0)) {
        Outcome::Returned => buffer[compiled.result_base as usize] as usize,
        other => panic!("the allocation should have taken its fast path, got {other:?}"),
    };
    assert_eq!(buffer[compiled.alloc_base as usize], 1, "native code did record it");

    let threads = vec![rooted_thread(vec![Frame::new(0, 1, vec![Value::Reference(array)])])];
    heap.commit_pending();
    gc::verify_heap(&metaspace, &heap, &threads);
}







/// **Argumentos `long` y `double` en callees que el JIT inlinea**, contra un `java` de verdad.
///
/// Los tests de `compile_tests.rs` prueban la copia con un `Environment` de mentira; éste la prueba
/// con el resolver real, el intérprete real y el layout de locales real — que es el único lugar
/// donde se ve si el JIT y `Frame::reset_for_call` acuerdan sobre en qué slot cae cada argumento.
///
/// El fixture tiene los cuatro casos que se pueden equivocar de forma distinta: un `long` **en el
/// medio** (el que corre al argumento siguiente), un `long` **primero**, dos categoría-2 seguidas, y
/// un método de instancia (donde el receptor ocupa el slot 0 y corre todo lo demás). Y uno de los
/// callees devuelve `long` y lo usa como `long`, no casteado a `int`: si la mitad alta se leyera del
/// slot de al lado, un cast a 32 bits no lo mostraría.
///
/// `1566736103` es lo que imprime `java JcCat2` con un JDK 25.
#[test]
fn argumentos_de_categoria_2_en_callees_inlineados_dan_lo_mismo_que_el_jdk_real() {
    differential("java/JcCat2.class", 1566736103);
}
