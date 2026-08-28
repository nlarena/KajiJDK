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
/// interning, `"a" == "a"` false here and true on a real JDK — was retired when F3 hito 3 gave
/// `strings::intern` a real JLS §3.10.5 pool, because a suppression that outlives its reason hides
/// exactly the bug it was written around. See [`oracle`] for the argument and for the fixture that
/// keeps the mechanism proven while there is nothing real in it.
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
}
