//! The **differential fuzzer**: generate programs nobody wrote, run each one down two paths that
//! *must* agree, and shout when they don't.
//!
//! # Why this project can afford one
//!
//! The hard part of fuzzing is never generating input — it is knowing whether the answer is wrong.
//! A generated program returns `847`; is that right? Nobody computed the expected value, so there
//! is nothing to compare against. That is the **oracle problem**, and it is what stops most
//! projects from fuzzing anything more interesting than a parser.
//!
//! This VM already solved it three times over, by accident:
//!
//! | pair | what it pins |
//! |---|---|
//! | `JVM_JIT=0` vs the JIT | the interpreter against the native compiler |
//! | `green` ≡ `os-gil` ≡ `os` | the three threading substrates against each other |
//! | this VM vs the real `java` | us against the reference implementation |
//!
//! None of them needs to know the right answer — only that two roads arrive at the same place. A
//! fuzzer is the industrial version of what the suite already does by hand: the workloads in
//! `java/` are cases somebody *thought of*, and this explores the ones nobody did.
//!
//! # The shape (level 0 — this file)
//!
//! Four verbs, and every deeper level is a refinement of one of them:
//!
//! ```text
//! for each seed:
//!     program = generate(seed)
//!     a       = run(program, left)
//!     b       = run(program, right)
//!     if oracle says a != b:
//!         minimal = reduce(program, "still diverges")
//!         report(minimal)
//! ```
//!
//! ## The one decision that shapes everything else
//!
//! **A [`Program`] is a data structure, not text.** Text is produced at the very end, by
//! [`Program::to_java`]. That is not a stylistic preference: the [`Reducer`] has to *manipulate* a
//! failing program — delete a statement, shrink a constant toward zero, drop a parameter — and any
//! cut through text yields something that no longer compiles. Reduction over a tree stays valid by
//! construction, and reduction is what turns "the fuzzer found something odd" into a case small
//! enough that a human will read it.
//!
//! ## The predicate belongs to the loop, not to the reducer
//!
//! [`campaign`] hands the reducer a closure it built itself, out of the same runner and oracle that
//! found the divergence. A reducer that defined its own notion of "still fails" could happily
//! minimise a program into one that fails for a *different* reason — the classic way a shrinker
//! turns a real bug into a fictional one.
//!
//! # The levels
//!
//! | level | module | what it refines |
//! |---|---|---|
//! | 0 | this file | the loop and the four contracts |
//! | 2.1 | [`exec`] | [`Runner`] — compile once, run out of process, with a timeout |
//! | 2.2 | [`oracle`] | [`Oracle`] — exact comparison, plus what must *not* be compared |
//! | 2.3 | [`gen`] | [`Generator`] — a typed AST that is total, terminating and reproducible |
//! | 2.4 | [`reduce`] | [`Reducer`] — structural shrinking to a local minimum |
//! | — | [`campaigns`] | the four of them wired together, and the campaigns worth running |
//!
//! # The third verdict
//!
//! Level 0 originally had two: the paths agree, or they do not. Building the oracle turned up a
//! third case that is neither, and pretending otherwise is how a campaign lies to you. A program
//! that does not compile, or that does not terminate on *either* side, says nothing about the VM —
//! it is a broken promise from the generator. [`Verdict::Unusable`] is that case, and
//! [`Report::unusable`] counts it, so "no divergences" can be read alongside "and it actually ran
//! something".

pub mod campaigns;
pub mod exec;
pub mod gen;
pub mod oracle;
pub mod reduce;

use std::fmt;

/// The seed a whole program is generated from. Two runs with the same seed must produce
/// byte-identical source: reproducibility is what makes a finding worth reporting, and it is cheap
/// to keep only if it is stated up front.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub struct Seed(pub u64);

/// A way of running a program. None of these are hypothetical — each is a configuration the VM
/// already supports and the suite already exercises, which is why the runner is the cheap half of
/// this tool.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum Path {
    /// The interpreter with the JIT switched off (`JVM_JIT=0`).
    Interpreter,
    /// Green threads with the JIT on.
    Jit,
    /// Real OS threads behind the GIL.
    OsGil,
    /// Real OS threads without the GIL. The JIT is off here by design.
    OsParallel,
    /// The reference implementation: a real `java` from an installed JDK.
    ReferenceJdk,
}

impl fmt::Display for Path {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(match self {
            Path::Interpreter => "interpreter",
            Path::Jit => "jit",
            Path::OsGil => "os-gil",
            Path::OsParallel => "os-parallel",
            Path::ReferenceJdk => "reference jdk",
        })
    }
}

/// How a run ended. `Timeout` is a first-class outcome rather than an error because a generated
/// program *can* fail to terminate: a fuzzer that treats every hang as a bug never finishes its
/// first iteration, and one that treats it as noise misses real infinite loops.
#[derive(Clone, PartialEq, Eq, Debug)]
pub enum Outcome {
    /// The entry method returned this value.
    Returned(i32),
    /// A Java exception escaped, named by its binary class name.
    Threw(String),
    /// The run exceeded its time budget.
    Timeout,
    /// The source did not compile. Not a VM finding at all but a **generator** bug — a campaign
    /// that cannot tell the two apart chases its own tail — so it gets its own variant.
    CompileError(String),
    /// The VM itself died — a panic, an abort, a non-zero exit. Always a finding.
    Crashed(String),
}

/// Everything a run may be judged on. Deliberately small: anything not listed here is something the
/// generator must not let a program depend on (identity hash codes, wall-clock time, iteration
/// order), because the oracle would then be comparing noise and every campaign would drown in it.
#[derive(Clone, PartialEq, Eq, Debug)]
pub struct Observation {
    pub outcome: Outcome,
    pub stdout: String,
}

/// A program the generator produced. Abstract so the reducer can work on the tree while the runner
/// only ever sees the text.
pub trait Program: Clone {
    /// The source to compile. Must be deterministic: same program, same bytes, every time.
    fn to_java(&self) -> String;
    /// The class whose entry method the runner invokes.
    fn class_name(&self) -> &str;

    /// Whether `value` is an **exception marker** rather than a computed result.
    ///
    /// A program that answers a marker did not compute anything: it threw, and what came back is
    /// the wrapper's code for *which* exception. That is a legitimate outcome — the two sides can
    /// agree on it, and agreeing on a thrown class is worth something — but it is **not** the same
    /// as agreeing on arithmetic, and a campaign whose seeds mostly answer markers is testing far
    /// less than its seed count suggests.
    ///
    /// It lives on the trait rather than in the campaign loop because only the generator knows what
    /// its markers are, and it defaults to `false` so a `Program` that has no notion of them —
    /// every hand-written fixture in the tests — needs to say nothing.
    fn is_marker(&self, _value: i32) -> bool {
        false
    }
}

/// Turns a seed into a program. Most of the difficulty of this whole tool lives behind this one
/// method: the programs must be type-correct (or `javac` rejects them), deterministic (or the
/// oracle compares noise) and terminating (or every run is a timeout).
pub trait Generator {
    type Program: Program;
    fn generate(&mut self, seed: Seed) -> Self::Program;
}

/// Compiles a program and runs it down one path.
pub trait Runner<P: Program> {
    fn run(&mut self, program: &P, path: Path) -> Observation;
}

/// Decides whether two observations of the same program disagree.
///
/// This is where a **known-divergence allowlist** belongs: differences that are real and *expected*,
/// where this VM answers something a real JDK does not and is still right to. An oracle that does
/// not know them reports the same non-bug forever, which is how a fuzzing campaign becomes
/// something nobody reads.
///
/// The list is currently **empty**, and staying that way is part of the job. Its one entry — string
/// interning, `"a" == "a"` false here and true on a real JDK — was retired on the claim that the
/// pool had landed. **It had not**, and the suppression's removal was right for the wrong reason:
/// what it was hiding was a live non-conformance (FZ-008), fixed on 2026-08-29. That is the whole
/// argument for keeping the list empty — a suppression that outlives its reason hides exactly the
/// bug it was written around, and this one nearly did. See [`oracle`] for the fixture that keeps
/// the mechanism proven while there is nothing real in it.
pub trait Oracle {
    fn verdict(&self, left: &Observation, right: &Observation) -> Verdict;
}

/// The oracle's answer.
#[derive(Clone, PartialEq, Eq, Debug)]
pub enum Verdict {
    /// The two paths arrived at the same place.
    Agree,
    /// They did not; the string says how, for the report.
    Differ(String),
    /// The run says nothing about the system under test — the source did not compile, or neither
    /// side terminated. Both are broken promises from the **generator**, and both let the loop move
    /// on, but they must never be recorded as `Agree`: a generator that emits nothing but rejected
    /// source would otherwise report a flawless campaign. [`Report::unusable`] counts them.
    Unusable(String),
}

/// Shrinks a failing program while `still_fails` keeps holding.
///
/// The predicate is supplied by [`campaign`] and never built here — see the module docs.
pub trait Reducer<P: Program> {
    fn reduce(&mut self, program: P, still_fails: &mut dyn FnMut(&P) -> bool) -> P;
}

/// A reducer that reduces nothing — the level-0 stand-in, and a useful control: what it reports is
/// the raw generated program, which is exactly what a campaign without shrinking looks like.
pub struct NoReduction;

impl<P: Program> Reducer<P> for NoReduction {
    fn reduce(&mut self, program: P, _still_fails: &mut dyn FnMut(&P) -> bool) -> P {
        program
    }
}

/// One finding: the seed that produced it, the reduced source, and both sides of the disagreement.
#[derive(Clone, Debug)]
pub struct Divergence {
    pub seed: Seed,
    pub source: String,
    pub left: (Path, Observation),
    pub right: (Path, Observation),
    pub reason: String,
}

impl fmt::Display for Divergence {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        writeln!(f, "divergence on seed {}: {}", self.seed.0, self.reason)?;
        writeln!(f, "  {} -> {:?}", self.left.0, self.left.1.outcome)?;
        writeln!(f, "  {} -> {:?}", self.right.0, self.right.1.outcome)?;
        write!(f, "{}", self.source)
    }
}

/// What a campaign found.
#[derive(Clone, Debug, Default)]
pub struct Report {
    pub seeds_run: u64,
    pub divergences: Vec<Divergence>,
    /// Runs the oracle refused to judge ([`Verdict::Unusable`]). Kept apart from the divergences
    /// because it measures a different thing: **the health of the generator**, not of the VM. A
    /// campaign whose `unusable` count is large is not finding nothing, it is testing nothing.
    pub unusable: u64,
    /// The same, broken down by reason, so the number above is actionable.
    pub unusable_reasons: std::collections::BTreeMap<String, u64>,
    /// Seeds whose answer was an **exception marker** rather than a computed value.
    ///
    /// A different measurement from [`Report::unusable`] and the reason it is counted apart: an
    /// unusable seed never ran, a marked one ran and **threw**, which the oracle is right to accept
    /// as agreement. What it is not is coverage. This number exists because FZ-005 was exactly this
    /// failure — 46% of seeds dying before the JIT ever looked at them, invisible in a report that
    /// only counted usable seeds and divergences.
    pub marked: u64,
}

impl Report {
    /// The share of seeds that actually exercised the VM. The one number that says whether a
    /// campaign was worth running.
    pub fn usable_fraction(&self) -> f64 {
        if self.seeds_run == 0 {
            return 0.0;
        }
        1.0 - (self.unusable as f64) / (self.seeds_run as f64)
    }

    /// The share of seeds that **threw** instead of computing something. The companion to
    /// [`Report::usable_fraction`], and the one that says how much of a clean report is real.
    pub fn marked_fraction(&self) -> f64 {
        if self.seeds_run == 0 {
            return 0.0;
        }
        (self.marked as f64) / (self.seeds_run as f64)
    }
}

/// **The loop.** Generate, run twice, compare, shrink, record.
///
/// Stops once `stop_after` findings have accumulated, so an afternoon's campaign does not spend
/// itself re-finding one broken opcode a thousand times.
pub fn campaign<G, R, O, D>(
    generator: &mut G,
    runner: &mut R,
    oracle: &O,
    reducer: &mut D,
    paths: (Path, Path),
    seeds: impl IntoIterator<Item = Seed>,
    stop_after: usize,
) -> Report
where
    G: Generator,
    R: Runner<G::Program>,
    O: Oracle,
    D: Reducer<G::Program>,
{
    let (left_path, right_path) = paths;
    let mut report = Report::default();

    for seed in seeds {
        report.seeds_run += 1;
        let program = generator.generate(seed);

        let left = runner.run(&program, left_path);
        let right = runner.run(&program, right_path);
        if let Outcome::Returned(v) = left.outcome {
            if program.is_marker(v) {
                report.marked += 1;
            }
        }
        let reason = match oracle.verdict(&left, &right) {
            Verdict::Differ(reason) => reason,
            Verdict::Agree => continue,
            Verdict::Unusable(why) => {
                report.unusable += 1;
                *report.unusable_reasons.entry(why).or_default() += 1;
                continue;
            }
        };

        // The predicate the reducer must preserve, built here from the same runner and oracle that
        // just disagreed — so "smaller" can never quietly become "fails differently".
        let mut still_fails = |candidate: &G::Program| {
            let a = runner.run(candidate, left_path);
            let b = runner.run(candidate, right_path);
            matches!(oracle.verdict(&a, &b), Verdict::Differ(_))
        };
        let minimal = reducer.reduce(program, &mut still_fails);

        report.divergences.push(Divergence {
            seed,
            source: minimal.to_java(),
            left: (left_path, left),
            right: (right_path, right),
            reason,
        });
        if report.divergences.len() >= stop_after {
            break;
        }
    }
    report
}

/// **The self-comparison loop.** Generate, run the *same* program `repeats` times on **one** path,
/// and check that every run landed in the same place.
///
/// # Why this exists when three pairings already do
///
/// Every other campaign asks "do two engines agree?", and to ask that it has to have two engines.
/// This one asks "does **one** engine agree with itself?", and that is a different question with a
/// property the others do not have: **it needs no reference at all**. It cannot tell you the right
/// answer, and it does not have to — a program whose result is fixed by construction that answers
/// two different things is a finding no matter which answer was right.
///
/// That is exactly the shape of FZ-002, and of the `os-parallel` stale-reference bug behind it: a
/// heisenbug that appears in roughly one run of ten, on a program whose correct output nobody
/// disputes. Against a *pairing*, such a bug is a coin flip on both sides at once; against
/// repetition, every extra run is another chance to catch it and the cost is linear.
///
/// # The reducer, and why its predicate is deliberately more sensitive than the campaign's
///
/// A greedy shrinker over a **flaky** predicate is unsound in a specific, silent way: a candidate
/// that happens not to reproduce this time is judged "no longer failing", so the reducer keeps the
/// cut — and the thing it just deleted may be precisely what made the program race. Left alone,
/// the minimal case is a program that does not reproduce anything.
///
/// There is no way to make that sound without a deterministic predicate, which is the one thing
/// this campaign does not have. What can be done is to stop the reducer from being *less* likely to
/// see the bug than the detector was, which is the failure that turns a real finding into a
/// mystery: `reduce_repeats` is `repeats * 2`. It buys probability, not certainty, and the shrink
/// is best-effort by construction — stated here rather than discovered later.
pub fn repetition_campaign<G, R, O, D>(
    generator: &mut G,
    runner: &mut R,
    oracle: &O,
    reducer: &mut D,
    path: Path,
    repeats: usize,
    seeds: impl IntoIterator<Item = Seed>,
    stop_after: usize,
) -> Report
where
    G: Generator,
    R: Runner<G::Program>,
    O: Oracle,
    D: Reducer<G::Program>,
{
    assert!(repeats >= 2, "one run cannot disagree with itself");
    let reduce_repeats = repeats * 2;
    let mut report = Report::default();

    for seed in seeds {
        report.seeds_run += 1;
        let program = generator.generate(seed);

        // The first run is the baseline every later one is compared against. Comparing each run
        // with its predecessor instead would let a value drift A, B, B and be called agreement
        // twice out of three.
        let first = runner.run(&program, path);
        if let Outcome::Returned(v) = first.outcome {
            if program.is_marker(v) {
                report.marked += 1;
            }
        }
        let mut found: Option<(usize, Observation, String)> = None;
        let mut unusable: Option<String> = None;

        for repeat in 1..repeats {
            let again = runner.run(&program, path);
            match oracle.verdict(&first, &again) {
                Verdict::Agree => continue,
                Verdict::Differ(reason) => {
                    found = Some((repeat, again, reason));
                    break;
                }
                // Unusable is a property of the *program*, not of this run, so one is enough to
                // disqualify the seed — and it must not be left to look like agreement.
                Verdict::Unusable(why) => {
                    unusable = Some(why);
                    break;
                }
            }
        }

        let Some((repeat, again, reason)) = found else {
            if let Some(why) = unusable {
                report.unusable += 1;
                *report.unusable_reasons.entry(why).or_default() += 1;
            }
            continue;
        };

        let mut still_fails = |candidate: &G::Program| {
            let baseline = runner.run(candidate, path);
            (1..reduce_repeats).any(|_| {
                let again = runner.run(candidate, path);
                matches!(oracle.verdict(&baseline, &again), Verdict::Differ(_))
            })
        };
        let minimal = reducer.reduce(program, &mut still_fails);

        report.divergences.push(Divergence {
            seed,
            source: minimal.to_java(),
            left: (path, first),
            right: (path, again),
            reason: format!("run 1 vs run {}: {reason}", repeat + 1),
        });
        if report.divergences.len() >= stop_after {
            break;
        }
    }
    report
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A program that is just a number, so the loop can be exercised without a real generator.
    #[derive(Clone)]
    struct Toy(i32);

    impl Program for Toy {
        fn to_java(&self) -> String {
            format!("class Toy {{ static int run() {{ return {}; }} }}", self.0)
        }
        fn class_name(&self) -> &str {
            "Toy"
        }
    }

    struct FromSeed;
    impl Generator for FromSeed {
        type Program = Toy;
        fn generate(&mut self, seed: Seed) -> Toy {
            Toy(seed.0 as i32)
        }
    }

    /// Agrees with itself everywhere except on programs whose value is divisible by `bug_every`,
    /// where the JIT path is off by one — a planted bug, so the loop has something to find.
    struct Planted {
        bug_every: i32,
        runs: usize,
    }
    impl Runner<Toy> for Planted {
        fn run(&mut self, program: &Toy, path: Path) -> Observation {
            self.runs += 1;
            let mut value = program.0;
            if path == Path::Jit && program.0 % self.bug_every == 0 {
                value += 1;
            }
            Observation { outcome: Outcome::Returned(value), stdout: String::new() }
        }
    }

    /// Diverges at or above a threshold — the shape a one-step shrinker can actually walk down,
    /// unlike a modulus, where the very first step lands on a value that agrees and the shrink stops
    /// at once. Which is the honest lesson: a greedy reducer finds a **local** minimum.
    struct PlantedAbove {
        threshold: i32,
        runs: usize,
    }
    impl Runner<Toy> for PlantedAbove {
        fn run(&mut self, program: &Toy, path: Path) -> Observation {
            self.runs += 1;
            let mut value = program.0;
            if path == Path::Jit && program.0 >= self.threshold {
                value += 1;
            }
            Observation { outcome: Outcome::Returned(value), stdout: String::new() }
        }
    }

    struct Exact;
    impl Oracle for Exact {
        fn verdict(&self, left: &Observation, right: &Observation) -> Verdict {
            if left == right {
                Verdict::Agree
            } else {
                Verdict::Differ(format!("{:?} vs {:?}", left.outcome, right.outcome))
            }
        }
    }

    /// Shrinks toward zero one step at a time, re-checking the predicate — the smallest thing that
    /// is still a reducer, here to prove the loop hands over a predicate that actually works.
    struct TowardZero;
    impl Reducer<Toy> for TowardZero {
        fn reduce(&mut self, program: Toy, still_fails: &mut dyn FnMut(&Toy) -> bool) -> Toy {
            let mut best = program;
            while best.0 != 0 {
                let candidate = Toy(best.0 - best.0.signum());
                if !still_fails(&candidate) {
                    break;
                }
                best = candidate;
            }
            best
        }
    }

    /// A runner that answers the truth except on every `flake_every`-th call, where it is off by
    /// one — a **non-deterministic** engine, which is the thing no pairing can see because both
    /// sides of a pairing get the same coin.
    ///
    /// The counter is per runner and not per program on purpose: what is being modelled is an
    /// engine that misbehaves once in a while, not a program that is wrong.
    struct Flaky {
        flake_every: usize,
        calls: usize,
    }
    impl Runner<Toy> for Flaky {
        fn run(&mut self, program: &Toy, _path: Path) -> Observation {
            self.calls += 1;
            let value =
                if self.flake_every > 0 && self.calls % self.flake_every == 0 {
                    program.0 + 1
                } else {
                    program.0
                };
            Observation { outcome: Outcome::Returned(value), stdout: String::new() }
        }
    }

    fn seeds(range: std::ops::Range<u64>) -> Vec<Seed> {
        range.map(Seed).collect()
    }

    #[test]
    fn a_campaign_over_agreeing_paths_finds_nothing() {
        // `bug_every` is larger than any value generated, so the two paths agree everywhere.
        let report = campaign(
            &mut FromSeed,
            &mut Planted { bug_every: 1000, runs: 0 },
            &Exact,
            &mut NoReduction,
            (Path::Interpreter, Path::Jit),
            seeds(1..20),
            1,
        );
        assert_eq!(report.seeds_run, 19);
        assert!(report.divergences.is_empty(), "nothing was planted, so nothing may be reported");
    }

    #[test]
    fn a_campaign_finds_a_planted_divergence_and_records_both_sides() {
        let report = campaign(
            &mut FromSeed,
            &mut Planted { bug_every: 7, runs: 0 },
            &Exact,
            &mut NoReduction,
            (Path::Interpreter, Path::Jit),
            seeds(1..20),
            1,
        );
        let found = report.divergences.first().expect("the planted bug must be found");
        assert_eq!(found.seed, Seed(7), "the first multiple of 7 is where it should trip");
        assert_eq!(found.left.1.outcome, Outcome::Returned(7));
        assert_eq!(found.right.1.outcome, Outcome::Returned(8));
        assert!(found.source.contains("return 7;"), "a finding carries source, not just numbers");
    }

    #[test]
    fn the_loop_stops_once_it_has_enough_findings() {
        let report = campaign(
            &mut FromSeed,
            &mut Planted { bug_every: 2, runs: 0 },
            &Exact,
            &mut NoReduction,
            (Path::Interpreter, Path::Jit),
            seeds(1..100),
            3,
        );
        assert_eq!(report.divergences.len(), 3);
        assert_eq!(report.seeds_run, 6, "seeds 2, 4 and 6 — it stops instead of running all 99");
    }

    #[test]
    fn the_reducer_gets_a_predicate_that_really_re_runs_both_paths() {
        // Diverges at 5 and above, so walking 18 down one step at a time must stop exactly at 5.
        let mut runner = PlantedAbove { threshold: 5, runs: 0 };
        let report = campaign(
            &mut FromSeed,
            &mut runner,
            &Exact,
            &mut TowardZero,
            (Path::Interpreter, Path::Jit),
            vec![Seed(18)],
            1,
        );
        let found = report.divergences.first().expect("18 is above the threshold");
        assert!(
            found.source.contains("return 5;"),
            "the reducer should have walked 18 down to the smallest still-failing value, got: {}",
            found.source
        );
        assert!(runner.runs > 2, "a real predicate re-runs both paths for every candidate");
    }

    /// The control, and the one that matters most: an engine that is deterministic must produce
    /// **no** finding, however many times it is asked. A repetition campaign that reported
    /// something here would report something forever, on every seed, and be worthless.
    #[test]
    fn a_deterministic_engine_never_disagrees_with_itself() {
        let report = repetition_campaign(
            &mut FromSeed,
            &mut Flaky { flake_every: 0, calls: 0 },
            &Exact,
            &mut NoReduction,
            Path::OsParallel,
            5,
            seeds(1..30),
            1,
        );
        assert_eq!(report.seeds_run, 29);
        assert!(report.divergences.is_empty(), "nothing flaked, so nothing may be reported");
        assert_eq!(report.unusable, 0);
    }

    /// And the other half: a flake **is** caught, with no reference implementation anywhere in the
    /// picture. This is the property the whole campaign shape exists for — it does not know the
    /// right answer and does not need to.
    #[test]
    fn a_flaky_engine_is_caught_without_any_reference_to_compare_against() {
        // Every 3rd call is wrong, so within 5 runs of the first seed there is certainly one.
        let report = repetition_campaign(
            &mut FromSeed,
            &mut Flaky { flake_every: 3, calls: 0 },
            &Exact,
            &mut NoReduction,
            Path::OsParallel,
            5,
            seeds(1..5),
            1,
        );
        assert_eq!(report.divergences.len(), 1, "a flake must be a finding");
        let found = &report.divergences[0];
        assert_eq!(found.left.0, Path::OsParallel, "both sides are the same path, by construction");
        assert_eq!(found.right.0, Path::OsParallel);
        assert!(
            found.reason.starts_with("run 1 vs run "),
            "the report must say *which* run disagreed: {}",
            found.reason
        );
    }

    /// A flake rarer than the campaign's own repeat count is missed, and that is not a defect to
    /// hide: it is the shape of the instrument. Detection is probabilistic, `repeats` is the knob,
    /// and writing the limit down is what stops a clean report from being read as proof.
    #[test]
    fn a_flake_rarer_than_the_repeat_count_is_missed_which_is_the_instruments_shape() {
        let report = repetition_campaign(
            &mut FromSeed,
            &mut Flaky { flake_every: 50, calls: 0 },
            &Exact,
            &mut NoReduction,
            Path::OsParallel,
            3,
            seeds(1..5),
            1,
        );
        assert!(
            report.divergences.is_empty(),
            "3 runs per seed cannot see a 1-in-50 flake, and pretending otherwise would be worse"
        );
    }

    /// The reducer must be **more** sensitive than the detector, not less. A shrinker whose
    /// predicate gets fewer chances than the campaign did will call a candidate fixed because it
    /// got lucky, keep the cut, and hand back a minimal case that reproduces nothing.
    ///
    /// What is counted is the number that decides that: **how many runs a candidate gets before it
    /// is declared clean**. The predicate stops at the first disagreement, so a *failing* candidate
    /// is cheap; the one that has to be paid for is the candidate that looks fine, and the contract
    /// is that it looks fine only after `repeats * 2` tries.
    #[test]
    fn a_candidate_is_only_declared_clean_after_twice_the_detectors_runs() {
        thread_local! {
            /// Every call ever, which is what drives the flake. Kept apart from the measurement
            /// below on purpose: one counter for both would let the reducer's reset re-arm the
            /// flake, and the candidate would never look clean.
            static CALLS: std::cell::Cell<usize> = const { std::cell::Cell::new(0) };
            /// Runs since the reducer started asking.
            static MEASURED: std::cell::Cell<usize> = const { std::cell::Cell::new(0) };
        }
        /// Wrong on its **second** call and honest ever after: enough for the detector to find
        /// something, and then nothing for the reducer to find — which is the case being measured.
        struct FlakyOnce;
        impl Runner<Toy> for FlakyOnce {
            fn run(&mut self, program: &Toy, _path: Path) -> Observation {
                let n = CALLS.with(|c| {
                    c.set(c.get() + 1);
                    c.get()
                });
                MEASURED.with(|c| c.set(c.get() + 1));
                let value = if n == 2 { program.0 + 1 } else { program.0 };
                Observation { outcome: Outcome::Returned(value), stdout: String::new() }
            }
        }
        struct CountingReducer {
            runs_to_declare_clean: usize,
        }
        impl Reducer<Toy> for CountingReducer {
            fn reduce(&mut self, program: Toy, still_fails: &mut dyn FnMut(&Toy) -> bool) -> Toy {
                MEASURED.with(|c| c.set(0));
                assert!(!still_fails(&program), "the candidate must look clean here");
                self.runs_to_declare_clean = MEASURED.with(|c| c.get());
                program
            }
        }

        const REPEATS: usize = 4;
        let mut reducer = CountingReducer { runs_to_declare_clean: 0 };
        let report = repetition_campaign(
            &mut FromSeed,
            &mut FlakyOnce,
            &Exact,
            &mut reducer,
            Path::OsParallel,
            REPEATS,
            seeds(1..2),
            1,
        );
        assert_eq!(report.divergences.len(), 1, "the flake on run 2 must be found");
        assert_eq!(
            reducer.runs_to_declare_clean,
            REPEATS * 2,
            "a candidate declared clean on fewer runs than the detector had is how a flaky \
             shrink deletes the cause and reports a minimal case that reproduces nothing"
        );
    }
}
