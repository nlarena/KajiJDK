//! Level 2 assembled: generator + executor + oracle + reducer, wired into
//! [`campaign`](super::campaign).
//!
//! Everything below this line is a convenience. The four pieces are independent by design and the
//! loop takes them as arguments; this module just spares every caller from repeating the same eight
//! lines, and gives the `#[ignore]`d end-to-end tests one place to live.
//!
//! # Which pairings are worth running
//!
//! | pair | what it pins | cost |
//! |---|---|---|
//! | [`Path::Interpreter`] vs [`Path::Jit`] | the JIT against the interpreter, which is the VM's own correctness oracle | two of our processes |
//! | [`Path::Jit`] vs [`Path::ReferenceJdk`] | this VM against a real `java` | needs the known-divergence list to be honest |
//! | [`Path::Jit`] vs [`Path::OsGil`] | the threading substrates against each other | cheap, but this grammar has no threads yet, so it can only find engine differences |
//!
//! The first is the one to run by default: it is the pairing where a disagreement is unambiguously
//! a bug in *this* project, with no reference-implementation judgement calls in the way.

use std::path::PathBuf;
use std::time::Duration;

use super::exec::{ProcessRunner, Toolchain};
use super::gen::{GenConfig, JavaGenerator};
use super::oracle::ExactOracle;
use super::reduce::StructuralReducer;
use super::{campaign, Path, Report, Seed};

/// The four pieces, assembled.
pub struct Campaign {
    pub generator: JavaGenerator,
    pub runner: ProcessRunner,
    pub oracle: ExactOracle,
    pub reducer: StructuralReducer,
}

impl Campaign {
    /// A campaign against the toolchain on this machine.
    ///
    /// The time budget is per *run*, not per seed, and it has to clear the slowest thing a
    /// generated program can legitimately do (a few thousand statements through the interpreter,
    /// plus JVM startup) by a wide margin — a budget set too tight turns every slow-but-correct
    /// program into a `Timeout` on one side and a value on the other, which the oracle is obliged
    /// to report as a divergence. That failure mode looks exactly like a real finding, so it is
    /// worth over-provisioning.
    pub fn detect(workdir: impl Into<PathBuf>, budget: Duration) -> Campaign {
        Campaign {
            generator: JavaGenerator::default(),
            runner: ProcessRunner::new(Toolchain::detect(), workdir, budget),
            oracle: ExactOracle::new(),
            reducer: StructuralReducer::default(),
        }
    }

    /// Narrows the grammar. Useful for a first campaign against a pairing nobody has tried, where
    /// small programs make any finding readable even before the reducer runs.
    pub fn with_config(mut self, config: GenConfig) -> Campaign {
        self.generator.config = config;
        self
    }

    pub fn run(&mut self, paths: (Path, Path), seeds: u64, stop_after: usize) -> Report {
        campaign(
            &mut self.generator,
            &mut self.runner,
            &self.oracle,
            &mut self.reducer,
            paths,
            (0..seeds).map(Seed),
            stop_after,
        )
    }
}

/// A campaign report as a human reads it: the health of the generator first, because a campaign
/// with a bad `unusable` rate found nothing for a reason that has nothing to do with the VM.
pub fn describe(report: &Report, paths: (Path, Path)) -> String {
    use std::fmt::Write as _;
    let mut out = String::new();
    let _ = writeln!(out, "{} vs {}", paths.0, paths.1);
    let _ = writeln!(
        out,
        "  {} seeds, {} usable ({:.0}%), {} divergences",
        report.seeds_run,
        report.seeds_run - report.unusable,
        report.usable_fraction() * 100.0,
        report.divergences.len()
    );
    for (why, count) in &report.unusable_reasons {
        let _ = writeln!(out, "  unusable x{count}: {why}");
    }
    for divergence in &report.divergences {
        let _ = writeln!(out, "---\n{divergence}");
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    fn workdir(name: &str) -> PathBuf {
        std::env::temp_dir().join(format!("kaji-fuzz-{name}"))
    }

    /// `FUZZ_SEEDS` if it is set, otherwise the smoke-test default. One environment variable so a
    /// campaign can be widened without editing the test that runs it — the same knob
    /// [`a_long_campaign_over_every_pairing`] already used.
    fn seed_count(default: u64) -> u64 {
        std::env::var("FUZZ_SEEDS").ok().and_then(|s| s.parse().ok()).unwrap_or(default)
    }

    /// **The campaign.** Ignored because it compiles and spawns thousands of processes; this is the
    /// one to run by hand.
    ///
    /// `cargo build --release && cargo test --release --lib fuzz::campaigns -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn the_interpreter_and_the_jit_agree_on_generated_programs() {
        let paths = (Path::Interpreter, Path::Jit);
        let mut it = Campaign::detect(workdir("campaign"), Duration::from_secs(25));
        let report = it.run(paths, seed_count(120), 10);
        println!("{}", describe(&report, paths));
        println!(
            "reducer: {} cuts accepted, {} candidates run, {} rejected for free",
            it.reducer.steps, it.reducer.candidates_tried, it.reducer.candidates_rejected_unchecked
        );
        assert!(
            report.usable_fraction() > 0.9,
            "a campaign that cannot compile or cannot finish its own programs tests nothing: {}",
            describe(&report, paths)
        );
        assert!(
            report.divergences.is_empty(),
            "the JIT and the interpreter must agree:\n{}",
            describe(&report, paths)
        );
    }

    /// The same programs against a real `java`. Kept separate because a disagreement here needs a
    /// judgement call — the reference implementation is right by definition, but some differences
    /// are known and legitimate, which is what the oracle's list is for.
    #[test]
    #[ignore]
    fn this_vm_and_the_reference_jdk_agree_on_generated_programs() {
        let paths = (Path::Jit, Path::ReferenceJdk);
        let mut it = Campaign::detect(workdir("campaign-ref"), Duration::from_secs(25));
        let report = it.run(paths, seed_count(120), 10);
        println!("{}", describe(&report, paths));
        assert!(
            report.divergences.is_empty(),
            "this VM disagrees with the reference implementation:\n{}",
            describe(&report, paths)
        );
    }

    /// An afternoon's campaign rather than a smoke test: more seeds, a wider grammar, every
    /// pairing. `FUZZ_SEEDS` sets the count so the same test serves both a ten-minute run and an
    /// overnight one.
    ///
    /// `FUZZ_SEEDS=1000 cargo test --release --lib fuzz::campaigns::tests::a_long_campaign -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn a_long_campaign_over_every_pairing() {
        let seeds = seed_count(300);
        // Wider than the default: more helpers to call, deeper expressions, more statements. The
        // budget goes up with them, because the point of a long campaign is to reach shapes the
        // smoke tests cannot.
        let wide = GenConfig {
            max_methods: 5,
            max_params: 4,
            max_stmts: 7,
            max_expr_depth: 4,
            max_block_depth: 3,
            max_loop_bound: 6,
            budget: 6_000,
            // Everything the grammar gains from here on keeps its default share, so a new
            // construct joins the long campaign the day it lands instead of the day somebody
            // remembers to add a field.
            ..GenConfig::default()
        };
        let pairings = [
            (Path::Interpreter, Path::Jit),
            (Path::Jit, Path::ReferenceJdk),
            (Path::Jit, Path::OsGil),
            (Path::Jit, Path::OsParallel),
        ];
        let mut findings = Vec::new();
        for paths in pairings {
            let mut it = Campaign::detect(workdir("long"), Duration::from_secs(30))
                .with_config(wide);
            let report = it.run(paths, seeds, 5);
            println!("{}", describe(&report, paths));
            if !report.divergences.is_empty() {
                findings.push(describe(&report, paths));
            }
        }
        assert!(findings.is_empty(), "{}", findings.join("\n"));
    }

    /// Green threads against OS threads behind the GIL. The grammar has no threads in it yet, so
    /// this can only catch an engine difference — but it is nearly free to run alongside the others.
    #[test]
    #[ignore]
    fn the_threading_substrates_agree_on_generated_programs() {
        let paths = (Path::Jit, Path::OsGil);
        let mut it = Campaign::detect(workdir("campaign-threads"), Duration::from_secs(25));
        let report = it.run(paths, 60, 10);
        println!("{}", describe(&report, paths));
        assert!(report.divergences.is_empty(), "{}", describe(&report, paths));
    }
}

#[cfg(test)]
mod jit_coverage {
    //! Does the JIT actually run on what the generator emits?
    //!
    //! This is not a rhetorical question, and it is the single most important thing to check about
    //! the [`Path::Interpreter`] / [`Path::Jit`] pairing. A method is only compiled after
    //! `JitCache::THRESHOLD` (32) invocations, or on-stack at a loop header once the loop has gone
    //! round enough times. A generated program calls `run()` **once**. If nothing crosses either
    //! threshold, then `JVM_JIT=0` and `JVM_JIT` unset are running *the same engine*, the campaign
    //! is comparing the interpreter against itself, and a clean report means nothing at all.
    //!
    //! This is the same genre of tool bug as FZ-003: a campaign that looks like it is testing
    //! something and is not. So it gets measured rather than assumed, and the measurement is
    //! written down in `docs/fuzzer_findings/`.
    //!
    //! Running in this process rather than through [`ProcessRunner`] is what makes the measurement
    //! possible: `run-headless` never prints the JIT's counters, but
    //! [`execute_counting_tuned`][crate::jvm::interpreter::bytecode_interpreter::execute_counting_tuned]
    //! returns them. It is also very much faster, which is why the differential below can cover far
    //! more seeds than the process-spawning campaign — at the cost of the one thing the child
    //! process was for: a VM panic here takes the test down with it instead of being reported.

    use super::*;
    use crate::burst::code_cache::JitStats;
    use crate::fuzz::gen::{marks, JavaProgram};
    use crate::fuzz::{Generator as _, Program as _};
    use crate::jvm::class_file::ClassFile;
    use crate::jvm::interpreter::bytecode_interpreter::execute_counting_tuned;
    use crate::jvm::interpreter::frame::{Frame, Value};
    use crate::jvm::interpreter::metaspace::MetaspaceService;

    /// Compiles `program` with the reference `javac` and leaves the class file in `dir`.
    fn compile(program: &JavaProgram, dir: &std::path::Path) -> PathBuf {
        std::fs::create_dir_all(dir).expect("workdir");
        let source = dir.join(format!("{}.java", program.class_name()));
        std::fs::write(&source, program.to_java()).expect("write source");
        let out = std::process::Command::new(Toolchain::detect().javac)
            .arg("-d")
            .arg(dir)
            .arg(&source)
            .output()
            .expect("spawn javac");
        assert!(
            out.status.success(),
            "the generator emitted source javac rejects:\n{}",
            String::from_utf8_lossy(&out.stderr)
        );
        dir.join(format!("{}.class", program.class_name()))
    }

    /// Whether a returned value is one of the total wrapper's exception markers rather than a
    /// computed result — i.e. whether the program threw instead of finishing its warm-up.
    fn is_marker(value: i32) -> bool {
        [
            marks::ARITHMETIC,
            marks::BOUNDS,
            marks::NULL,
            marks::CLASS_CAST,
            marks::STACK_OVERFLOW,
            marks::NEGATIVE_SIZE,
            marks::OTHER,
        ]
        .contains(&value)
    }

    /// `run()` on the green engine with the JIT forced on or off, plus the JIT's counters.
    fn execute(class_file: &std::path::Path, jit: bool) -> (Option<i32>, usize, JitStats) {
        let class = ClassFile::from_path(class_file.to_str().expect("utf-8 path")).expect("load");
        let name = class.class_name(class.this_class).unwrap().to_string();
        let mut metaspace = MetaspaceService::new(
            vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")],
            vec![class_file.parent().map(PathBuf::from).unwrap_or_default()],
        );
        metaspace.add(name.clone(), class);
        let entry = metaspace.resolve_method(&name, "run", "()I").expect("run()I");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());
        let (value, steps, stats) =
            execute_counting_tuned(metaspace, frame, Some(jit), None, |_| {});
        let value = match value {
            Some(Value::Int(v)) => Some(v),
            _ => None,
        };
        (value, steps, stats)
    }

    /// The measurement. Prints the share of generated programs on which the JIT compiles anything
    /// at all, and fails if that share is so low that the process-spawning campaign is theatre.
    ///
    /// `cargo test --release --lib fuzz::campaigns::jit_coverage::the_jit -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn the_jit_actually_compiles_something_on_generated_programs() {
        const SEEDS: u64 = 60;
        let dir = std::env::temp_dir().join("kaji-fuzz-jitcov");
        let mut generator = JavaGenerator::default();

        let (mut compiled_any, mut entered_any, mut osr_any) = (0, 0, 0);
        for seed in 0..SEEDS {
            let program = generator.generate(Seed(seed));
            let class_file = compile(&program, &dir);
            let (_, _, stats) = execute(&class_file, true);
            compiled_any += usize::from(stats.compiled > 0);
            entered_any += usize::from(stats.native_calls > 0);
            osr_any += usize::from(stats.osr_entries > 0);
        }
        println!(
            "of {SEEDS} generated programs: {compiled_any} compiled something, \
             {entered_any} entered native code, {osr_any} entered on-stack"
        );
        assert!(
            entered_any * 4 >= SEEDS as usize,
            "only {entered_any}/{SEEDS} programs ever entered native code — the \
             interpreter-vs-JIT campaign is comparing the interpreter against itself. Raise the \
             call counts the generator produces, or lower JVM_JIT_THRESHOLD for the JIT path."
        );
    }

    /// **FZ-005, as a regression test.** A program that throws on warm-up iteration 1 is never
    /// scanned by the JIT at all, and the signature that says so is `rejected == 0`.
    ///
    /// That last part is the whole diagnostic value. "The JIT did not run" has two causes with
    /// opposite fixes — it refused the method (`rejected` goes up: legitimate information about the
    /// subset) or it never saw the method (`rejected` stays at zero: a bug in the generator, which
    /// is producing programs that die before they are hot). Both look identical in
    /// `native_calls == 0`, which is why FZ-005 hid behind a green campaign for a whole stage.
    ///
    /// `cargo test --release --lib fuzz::campaigns::jit_coverage::a_program_that_throws -- --ignored`
    #[test]
    #[ignore]
    fn a_program_that_throws_before_the_threshold_is_never_even_scanned() {
        use crate::fuzz::gen::{Expr, Method, Stmt, Ty};

        // The exact minimal case in `docs/fuzzer_findings/FZ-005-arrays-mueren-antes-del-jit.md`,
        // built from the AST so the two halves cannot drift apart: `int[] a0 = new int[2];
        // return a0[<index>];`
        let at = |index: i32, class: &str| JavaProgram {
            class: class.to_string(),
            methods: Vec::new(),
            entry: Method {
                name: "m0".to_string(),
                params: Vec::new(),
                returns: Ty::Int,
                body: vec![Stmt::NewArray { name: "a0".into(), elem: Ty::Int, len: 2 }],
                result: Expr::ArrayLoad("a0".into(), Ty::Int, Box::new(Expr::IntLit(index))),
                cost: 4,
            },
            warmup: GenConfig::default().warmup,
        };
        let dir = std::env::temp_dir().join("kaji-fuzz-fz005");

        let out_of_range = at(5, "FzFive");
        assert!(out_of_range.well_formed().is_ok(), "the fixture must be valid Java");
        let (value, _, stats) = execute(&compile(&out_of_range, &dir), true);
        assert_eq!(value, Some(marks::BOUNDS), "it must throw, or it proves nothing");
        assert_eq!(stats.native_calls, 0, "one invocation cannot reach a threshold of 32");
        assert_eq!(
            stats.rejected, 0,
            "`rejected` must stay at zero: the JIT never *saw* this method, and a campaign that \
             cannot tell that from a refusal cannot tell a generator bug from a subset boundary"
        );

        // The identical program with an index the array actually has. One character apart, and it
        // is the difference between a campaign that tests the JIT and one that does not.
        let in_range = at(1, "FzOne");
        let (value, _, stats) = execute(&compile(&in_range, &dir), true);
        assert_eq!(value, Some(0), "a0[1] of a fresh int[2] is the zero `newarray` left there");
        assert!(
            stats.native_calls > 0,
            "the same program that survives its warm-up must reach native code: {stats:?}"
        );
    }

    /// What each grammar extension costs in JIT coverage — measured per configuration rather than
    /// assumed, which is the whole lesson of FZ-004.
    ///
    /// The one that matters is `fp_narrowing`. `f2i`/`f2l`/`d2i`/`d2l` are outside the JIT's subset
    /// on purpose (`burst::compile`, JLS §5.1.3), and the refusal is **per method**: one narrowing
    /// conversion anywhere in the entry method and the whole thing runs interpreted, on both arms
    /// of an interpreter-versus-JIT campaign. So the two settings are not two flavours of the same
    /// campaign — they are two different campaigns, and this prints the number that says so.
    ///
    /// `cargo test --release --lib fuzz::campaigns::jit_coverage::what_each -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn what_each_grammar_setting_costs_in_jit_coverage() {
        const SEEDS: u64 = 80;
        let dir = std::env::temp_dir().join("kaji-fuzz-jitcov-cfg");
        let base = GenConfig::default();
        let settings = [
            ("scalars, integral only ", GenConfig { fp_share: 0, array_share: 0, ..base }),
            ("scalars, with floats   ", GenConfig { array_share: 0, ..base }),
            ("arrays, no floats     ", GenConfig { fp_share: 0, ..base }),
            ("arrays of int only    ", GenConfig { wide_array_elements: false, ..base }),
            ("no narrowing conversion", GenConfig { fp_narrowing: false, ..base }),
            ("everything (the default)", base),
        ];
        let mut rows = Vec::new();
        for (label, config) in settings {
            let mut generator = JavaGenerator::new(config);
            let (mut entered, mut osr) = (0, 0);
            let (mut compiled, mut refused, mut deopts, mut threw) = (0, 0, 0, 0);
            for seed in 0..SEEDS {
                let program = generator.generate(Seed(seed));
                let class_file = compile(&program, &dir);
                let (value, _, stats) = execute(&class_file, true);
                entered += usize::from(stats.native_calls > 0);
                osr += usize::from(stats.osr_entries > 0);
                compiled += stats.compiled;
                refused += stats.rejected;
                deopts += stats.deopts;
                // A program that threw on its **first** warm-up iteration never reached
                // `JitCache::THRESHOLD`, so the JIT never even scanned it. That is a completely
                // different failure from "the JIT refused it", and telling the two apart is the
                // only way to know which one a grammar change actually caused.
                threw += usize::from(matches!(value, Some(v) if is_marker(v)));
            }
            // `rejected` is the number that actually answers the question, because the refusal is
            // per method: a program can enter native code through its classifier helper while the
            // method doing the floating arithmetic was turned away.
            println!(
                "{label}: {entered}/{SEEDS} entered native code, {osr} on-stack, \
                 {compiled} methods compiled, {refused} refused, {deopts} deopts,                  {threw} died on a marker"
            );
            rows.push((label, entered));
        }
        // Not an assertion about which is larger — that is the measurement, and pinning it would
        // turn a finding into a test failure. What must hold is that the integral grammar still
        // clears the FZ-004 floor, so a regression there is not blamed on floating point.
        let integral = rows[0].1;
        assert!(
            integral * 4 >= SEEDS as usize,
            "the integral grammar itself dropped to {integral}/{SEEDS} — FZ-004 again, and \
             floating point is not the cause"
        );
    }

    /// The same differential the process-spawning campaign runs, but in this process and therefore
    /// over far more seeds. Not a replacement — it cannot survive a VM panic, and a panic is a
    /// finding — but it is what a wide sweep can afford.
    #[test]
    #[ignore]
    fn the_jit_and_the_interpreter_agree_in_process() {
        let seeds: u64 = std::env::var("FUZZ_SEEDS")
            .ok()
            .and_then(|s| s.parse().ok())
            .unwrap_or(200);
        let dir = std::env::temp_dir().join("kaji-fuzz-inproc");
        let mut generator = JavaGenerator::default();

        let (mut disagreements, mut exercised) = (Vec::new(), 0);
        for seed in 0..seeds {
            let program = generator.generate(Seed(seed));
            let class_file = compile(&program, &dir);
            let (off, _, off_stats) = execute(&class_file, false);
            let (on, _, on_stats) = execute(&class_file, true);
            assert_eq!(off_stats, JitStats::default(), "seed {seed}: the JIT ran with it off");
            exercised += usize::from(on_stats.native_calls > 0);
            if off != on {
                disagreements.push(format!(
                    "seed {seed}: interpreter {off:?}, jit {on:?} ({on_stats:?})\n{}",
                    program.to_java()
                ));
            }
        }
        println!("{seeds} seeds, {exercised} of them entered native code");
        assert!(disagreements.is_empty(), "{}", disagreements.join("\n---\n"));
    }
}
