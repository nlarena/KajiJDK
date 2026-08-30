//! The **oracle** (level 2.2): given two [`Observation`]s of the same program, decide whether the
//! two paths arrived at the same place.
//!
//! # Why this is not `left == right`
//!
//! It looks like one line of code and it is not, because three of the five [`Outcome`]s say
//! something about the *tool* rather than about the system under test, and an oracle that cannot
//! tell those apart produces a report nobody reads:
//!
//! | pair | verdict | why |
//! |---|---|---|
//! | `CompileError` / `CompileError` | [`Verdict::Unusable`] | the **generator** emitted source `javac` rejects. Not a VM finding, and reporting it as one sends a human to read a program that never ran |
//! | `Crashed` on either side | [`Verdict::Differ`] | a VM panic is a finding *by itself*, even when both sides panic: two crashes are still two crashes, and nothing about "they agree" is reassuring here |
//! | `Timeout` / a value | [`Verdict::Differ`] | one side finished and the other did not — that is a real infinite loop on one engine, which is exactly the kind of thing this tool exists to catch |
//! | `Timeout` / `Timeout` | [`Verdict::Unusable`] | the generated program does not terminate. The generator promises it always does, so this is a broken promise, not a broken VM |
//! | anything else | exact comparison of [`Outcome`] | |
//!
//! The distinction between `Unusable` and `Agree` is the whole point of the third variant: both let
//! the loop move on, but only one of them means *"the two paths coincide on a value"*. Collapsing
//! them would let a generator that emits nothing but broken source report a flawless campaign.
//!
//! # What is deliberately *not* compared
//!
//! - **`stdout`.** The two sides do not even print the same shape: `run-headless` prints its own
//!   one-line report, a real `java` prints whatever the program's `main` printed. Comparing them
//!   would flag every single run.
//! - **Exception messages.** Already dropped in [`super::exec`]; two implementations may word the
//!   same failure differently and both be right. Only the class name survives, and see
//!   [`ExactOracle::verdict`] for the one case where even that cannot be compared.
//!
//! # The known-divergence list
//!
//! Some differences are real, understood, and *expected*: a difference of **implementation** rather
//! than of execution, where this VM answers something a real JDK does not and is still right to. An
//! oracle that does not know them reports the same non-bug forever, which is how a fuzzing campaign
//! becomes something nobody reads.
//!
//! The list is **consultable** ([`ExactOracle::known`]) and every entry carries its justification,
//! because a list of exceptions without arguments is an elegant way of hiding bugs.
//!
//! ## It is empty — and the reason written here used to be wrong
//!
//! [`KNOWN`] has no entries. It held exactly one: string interning — `"a" == "a"` is `false` here
//! and `true` on a real JDK, because `strings::intern` allocates a fresh String per execution and
//! there is no pool to consult.
//!
//! **This section used to say F3 made that entry false, and that `strings::intern` was "now a real
//! JLS §3.10.5 string pool: one instance per literal, `malloc_old`ed, a GC root, and pinned out of
//! `gc::compact`". None of that is true.** Measured 2026-08-29: `String x = "a"; String y = "a";
//! x == y` answers **false** in this VM and **true** on the reference JDK, on all three substrates
//! and with the JIT either way. The module header of `jvm::interpreter::strings` says so in its own
//! words — "No interning/dedup yet (each `ldc` makes a fresh object)" — and `intern_units` is a
//! bare `heap.malloc`.
//!
//! So removing the entry was right, but **not for the reason given**: it was not that the bug had
//! been fixed, it was that a suppression was hiding a live non-conformance. The list stays empty
//! and the difference is a finding — FZ-008 — rather than an accepted divergence. What made the
//! wrong reason survive is that the campaign agreed with it: the probe was emitted inline and
//! `javac` folded it away, so nothing ever asked the VM. See FZ-009.
//!
//! An empty list is not a dead mechanism. [`ExactOracle::classify`] and its both-directions rule
//! stay exercised by the tests below against a fixture entry, so on the day a second real divergence
//! is understood, the thing it gets added to is already known to work.
//!
//! ## How an entry recognises itself
//!
//! An [`Oracle`] sees two observations and nothing else — not the source. So an entry cannot match
//! on "the program compared two strings"; it has to match on what the program *reports*. That is
//! what the marker vocabulary in [`super::gen::marks`] is for: a generated program is total, and it
//! tells the oracle what it observed through the integer it returns. An entry matches one reserved
//! pair and *only* that pair — a matcher broad enough to swallow `Returned(0)` vs `Returned(1)`
//! would swallow half the real bugs with it. That narrowness is also what made the interning entry
//! cheap to retire: it named the exact pair it hid, so what it was costing was legible.

use super::{Observation, Oracle, Outcome, Verdict};

/// A difference that is real, understood, and must not be reported again.
#[derive(Clone, Copy)]
pub struct KnownDivergence {
    /// The finding it is documented under, e.g. `"docs/fuzzer_findings.md#interning"`.
    pub id: &'static str,
    /// What the difference looks like.
    pub what: &'static str,
    /// Why it is legitimate. Never empty — see the module docs.
    pub why: &'static str,
    /// Recognises the pair, in the order the oracle was given it. The oracle also tries the
    /// mirrored pair, so a matcher only has to describe one direction.
    matches: fn(&Outcome, &Outcome) -> bool,
}

impl std::fmt::Debug for KnownDivergence {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("KnownDivergence").field("id", &self.id).field("what", &self.what).finish()
    }
}

/// The divergences this project has already decided are not bugs.
///
/// **Empty on purpose.** The one entry it ever held — string interning, from the table in
/// `docs/fuzzer_findings.md` — stopped being true when F3 hito 3 gave `strings::intern` a real
/// JLS §3.10.5 pool, and a suppression whose reason has expired hides the very bug it was written
/// around. See the module docs. Adding to it means writing a `why` that survives being read aloud.
pub const KNOWN: &[KnownDivergence] = &[];

/// Compares two observations outcome by outcome, with the rules in the module docs and a
/// known-divergence list on top.
#[derive(Clone, Debug)]
pub struct ExactOracle {
    known: &'static [KnownDivergence],
}

impl Default for ExactOracle {
    fn default() -> ExactOracle {
        ExactOracle::new()
    }
}

impl ExactOracle {
    /// An oracle that knows everything this project has written down.
    pub fn new() -> ExactOracle {
        ExactOracle { known: KNOWN }
    }

    /// An oracle that knows nothing — for tests that want to see the raw comparison, and for the
    /// day somebody wants to re-audit an entry by watching how much it was hiding. That audit is
    /// how the interning entry was retired.
    ///
    /// While [`KNOWN`] is empty this is indistinguishable from [`ExactOracle::new`], so it proves
    /// nothing on its own: what it is *for* is being the other half of a comparison against an
    /// oracle that was handed a list.
    pub fn without_known_divergences() -> ExactOracle {
        ExactOracle { known: &[] }
    }

    /// The list, so a campaign report can print what it chose to ignore.
    pub fn known(&self) -> &'static [KnownDivergence] {
        self.known
    }

    /// The entry that explains this pair, if any. Tried in both directions so an entry does not
    /// have to be written twice.
    pub fn classify(&self, left: &Outcome, right: &Outcome) -> Option<&'static KnownDivergence> {
        self.known.iter().find(|k| (k.matches)(left, right) || (k.matches)(right, left))
    }
}

impl Oracle for ExactOracle {
    fn verdict(&self, left: &Observation, right: &Observation) -> Verdict {
        let (a, b) = (&left.outcome, &right.outcome);

        // A panic outranks everything, including the other side's compile error: the rule is
        // "`Crashed` on either side is always a finding", and an ordering that let some other
        // condition win would quietly turn "always" into "usually".
        if let Outcome::Crashed(why) = a {
            return Verdict::Differ(format!("left crashed: {why}"));
        }
        if let Outcome::Crashed(why) = b {
            return Verdict::Differ(format!("right crashed: {why}"));
        }

        // Nothing ran, so there is nothing to say about the VM. One-sided is just as unusable as
        // two-sided — the source is the same source, so a compile error is a fact about the
        // generator whichever side reports it.
        match (a, b) {
            (Outcome::CompileError(d), _) | (_, Outcome::CompileError(d)) => {
                let first = d.lines().next().unwrap_or("").trim();
                return Verdict::Unusable(format!("generator emitted source javac rejects: {first}"));
            }
            (Outcome::Timeout, Outcome::Timeout) => {
                return Verdict::Unusable(
                    "both paths timed out — the generated program does not terminate".to_string(),
                );
            }
            _ => {}
        }

        if a == b {
            return Verdict::Agree;
        }

        // Both threw, but this VM cannot name its exception (FZ-001), so all that is really
        // observable is *that* it threw. Manufacturing a divergence out of "" vs
        // "java.lang.ArithmeticException" would report the tool's blind spot as a VM bug.
        if let (Outcome::Threw(x), Outcome::Threw(y)) = (a, b) {
            if x.is_empty() || y.is_empty() {
                return Verdict::Agree;
            }
        }

        match self.classify(a, b) {
            Some(_) => Verdict::Agree,
            None => Verdict::Differ(format!("{a:?} vs {b:?}")),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::fuzz::gen::marks;

    // -- the fixture ---------------------------------------------------------------------------
    //
    // `KNOWN` is empty (see the module docs), so the suppression machinery has nothing real to
    // exercise it. Deleting its tests along with the entry would leave `classify`, the
    // both-directions rule and the narrowness discipline unproven until somebody added the next
    // entry — which is exactly the moment you want them already proven. So the mechanism is tested
    // against a divergence invented here. It cannot leak: no oracle outside this module is ever
    // built from `FIXTURE`, so what these values mean is decided entirely by the tests below.

    const FIXTURE_LEFT: i32 = 0x0BAD_0001u32 as i32;
    const FIXTURE_RIGHT: i32 = 0x0BAD_0002u32 as i32;

    fn fixture_pair(ours: &Outcome, theirs: &Outcome) -> bool {
        matches!(
            (ours, theirs),
            (Outcome::Returned(FIXTURE_LEFT), Outcome::Returned(FIXTURE_RIGHT))
        )
    }

    const FIXTURE: &[KnownDivergence] = &[KnownDivergence {
        id: "test fixture — not a real divergence",
        what: "a pair of values that mean nothing outside this module",
        why: "it exists to prove the list still suppresses, matches both directions, and stays \
               narrow. A real entry has to argue its case; this one only argues for itself.",
        matches: fixture_pair,
    }];

    /// An oracle that knows the fixture and nothing else. Built by hand rather than through a
    /// constructor: `ExactOracle` should not grow public API for a test.
    fn seeded() -> ExactOracle {
        ExactOracle { known: FIXTURE }
    }

    fn obs(outcome: Outcome) -> Observation {
        Observation { outcome, stdout: String::new() }
    }

    fn verdict(a: Outcome, b: Outcome) -> Verdict {
        ExactOracle::new().verdict(&obs(a), &obs(b))
    }

    #[test]
    fn two_equal_values_agree() {
        assert_eq!(verdict(Outcome::Returned(7), Outcome::Returned(7)), Verdict::Agree);
    }

    #[test]
    fn two_different_values_differ() {
        assert!(matches!(
            verdict(Outcome::Returned(7), Outcome::Returned(8)),
            Verdict::Differ(_)
        ));
    }

    #[test]
    fn a_compile_error_on_both_sides_is_unusable_not_agreement() {
        let v = verdict(
            Outcome::CompileError("Foo.java:3: error: bad".into()),
            Outcome::CompileError("Foo.java:3: error: bad".into()),
        );
        match v {
            Verdict::Unusable(why) => assert!(why.contains("generator"), "got {why}"),
            other => panic!("a compile error is a generator bug, not agreement: {other:?}"),
        }
        assert_ne!(
            verdict(
                Outcome::CompileError("x".into()),
                Outcome::CompileError("x".into())
            ),
            Verdict::Agree,
            "it must never be confused with 'the two paths coincide on a value'"
        );
    }

    #[test]
    fn a_one_sided_compile_error_is_also_unusable() {
        // Both sides compile the *same* source, so this only happens when the tool is confused —
        // either way it says nothing about the VM.
        assert!(matches!(
            verdict(Outcome::CompileError("x".into()), Outcome::Returned(1)),
            Verdict::Unusable(_)
        ));
        assert!(matches!(
            verdict(Outcome::Returned(1), Outcome::CompileError("x".into())),
            Verdict::Unusable(_)
        ));
    }

    #[test]
    fn a_crash_on_either_side_is_always_a_divergence() {
        assert!(matches!(
            verdict(Outcome::Crashed("panicked at a.rs".into()), Outcome::Returned(1)),
            Verdict::Differ(_)
        ));
        assert!(matches!(
            verdict(Outcome::Returned(1), Outcome::Crashed("panicked at a.rs".into())),
            Verdict::Differ(_)
        ));
    }

    #[test]
    fn two_crashes_are_still_a_divergence_even_though_they_match() {
        let same = Outcome::Crashed("panicked at src/jvm/x.rs:1:1".to_string());
        assert!(
            matches!(verdict(same.clone(), same), Verdict::Differ(_)),
            "a VM panic is a finding by itself; 'both panicked identically' is not reassuring"
        );
    }

    #[test]
    fn a_crash_outranks_a_compile_error() {
        assert!(matches!(
            verdict(Outcome::Crashed("panicked".into()), Outcome::CompileError("x".into())),
            Verdict::Differ(_)
        ));
    }

    #[test]
    fn a_timeout_against_a_value_is_a_divergence() {
        assert!(matches!(verdict(Outcome::Timeout, Outcome::Returned(3)), Verdict::Differ(_)));
        assert!(matches!(verdict(Outcome::Returned(3), Outcome::Timeout), Verdict::Differ(_)));
    }

    #[test]
    fn a_timeout_on_both_sides_is_unusable() {
        match verdict(Outcome::Timeout, Outcome::Timeout) {
            Verdict::Unusable(why) => assert!(why.contains("terminate"), "got {why}"),
            other => panic!("expected unusable, got {other:?}"),
        }
    }

    #[test]
    fn exception_classes_are_compared_when_both_sides_know_them() {
        assert_eq!(
            verdict(
                Outcome::Threw("java.lang.ArithmeticException".into()),
                Outcome::Threw("java.lang.ArithmeticException".into())
            ),
            Verdict::Agree
        );
        assert!(matches!(
            verdict(
                Outcome::Threw("java.lang.ArithmeticException".into()),
                Outcome::Threw("java.lang.NullPointerException".into())
            ),
            Verdict::Differ(_)
        ));
    }

    #[test]
    fn a_nameless_throw_from_this_vm_is_not_a_divergence_against_a_named_one() {
        // FZ-001: `run-headless` reports `-> None` and nothing more. All that is observable is
        // *that* it threw, and reporting the tool's blind spot as a VM bug would be a lie.
        assert_eq!(
            verdict(
                Outcome::Threw(String::new()),
                Outcome::Threw("java.lang.ArithmeticException".into())
            ),
            Verdict::Agree
        );
    }

    #[test]
    fn a_throw_against_a_value_is_a_divergence_even_nameless() {
        assert!(matches!(
            verdict(Outcome::Threw(String::new()), Outcome::Returned(0)),
            Verdict::Differ(_)
        ));
    }

    #[test]
    fn a_seeded_entry_is_recognised_in_both_directions() {
        // The mechanism, on the fixture. `KNOWN` is empty, so this is the only place the
        // suppression path runs at all — and it has to keep running, or the next real entry gets
        // added to code nothing has executed since the interning entry was retired.
        let ours = Outcome::Returned(FIXTURE_LEFT);
        let theirs = Outcome::Returned(FIXTURE_RIGHT);
        let oracle = seeded();
        assert_eq!(oracle.verdict(&obs(ours.clone()), &obs(theirs.clone())), Verdict::Agree);
        assert_eq!(
            oracle.verdict(&obs(theirs), &obs(ours)),
            Verdict::Agree,
            "order must not matter: an entry describes one direction and the oracle mirrors it"
        );
    }

    #[test]
    fn without_the_list_the_same_pair_is_reported() {
        // The other half of the test above, on the same pair: with the list, `Agree`; without it,
        // `Differ`. That contrast is the claim — not that a bare oracle reports things, which it
        // would do anyway now that `KNOWN` is empty.
        let bare = ExactOracle::without_known_divergences();
        let v = bare.verdict(
            &obs(Outcome::Returned(FIXTURE_LEFT)),
            &obs(Outcome::Returned(FIXTURE_RIGHT)),
        );
        assert!(matches!(v, Verdict::Differ(_)), "the list is what suppresses it, nothing else");
    }

    #[test]
    fn a_seeded_entry_matches_its_pair_and_nothing_else() {
        // Narrowness is the property that made the interning entry safe to retire — it named the
        // exact pair it hid. An entry that leaked past its pair would take real findings with it.
        let oracle = seeded();
        let pair = (Outcome::Returned(FIXTURE_LEFT), Outcome::Returned(FIXTURE_RIGHT));
        assert!(oracle.classify(&pair.0, &pair.1).is_some());
        assert!(oracle.classify(&Outcome::Returned(FIXTURE_LEFT), &Outcome::Returned(9)).is_none());
        assert!(oracle.classify(&Outcome::Returned(0), &Outcome::Returned(1)).is_none());
        assert!(oracle.classify(&Outcome::Timeout, &Outcome::Returned(FIXTURE_RIGHT)).is_none());
    }

    #[test]
    fn the_known_list_is_consultable_and_every_entry_argues_its_case() {
        // Consultable: a campaign report has to be able to print what the oracle chose to ignore,
        // and "nothing" is a printable answer. The discipline below is vacuous while `KNOWN` is
        // empty, so it is also run over the fixture — otherwise the day an entry is added is the
        // day this assertion runs for the first time.
        let ids = |o: &ExactOracle| o.known().iter().map(|k| k.id).collect::<Vec<_>>();
        assert_eq!(
            ids(&ExactOracle::new()),
            KNOWN.iter().map(|k| k.id).collect::<Vec<_>>(),
            "`known()` must hand back the shipped list, not a filtered view of it"
        );
        assert_eq!(ids(&seeded()), vec![FIXTURE[0].id], "and the list the oracle was built with");

        let oracle = ExactOracle::new();

        for entry in oracle.known().iter().chain(FIXTURE) {
            assert!(!entry.id.is_empty(), "an entry must say where it is documented");
            assert!(!entry.why.is_empty(), "{}: a suppression without a reason hides bugs", entry.id);
            assert!(!entry.what.is_empty(), "{}: an entry must say what it matches", entry.id);
        }
    }

    #[test]
    fn the_shipped_oracle_suppresses_nothing() {
        // The list is empty on purpose (module docs). Asserting it keeps an entry from being added
        // back without the argument that has to come with it.
        assert!(KNOWN.is_empty(), "an entry was added — did its `why` get written down?");
    }

    #[test]
    fn string_identity_is_a_finding_now_that_this_vm_interns() {
        // The retired entry, inverted. F3 hito 3 made `strings::intern` a real JLS §3.10.5 pool —
        // one instance per literal, `malloc_old`ed, a GC root, pinned out of `gc::compact` — so
        // `"a" == "a"` is `true` here as it is on a real JDK. A program reporting
        // `STRING_IDENTITY_FALSE` against a JDK's `STRING_IDENTITY_TRUE` is therefore a genuine
        // interning regression, and the oracle must say so instead of nodding it through.
        let ours = Outcome::Returned(marks::STRING_IDENTITY_FALSE);
        let theirs = Outcome::Returned(marks::STRING_IDENTITY_TRUE);
        assert!(
            matches!(verdict(ours.clone(), theirs.clone()), Verdict::Differ(_)),
            "the interning suppression outlived its reason; it must not still be here"
        );
        assert!(
            matches!(verdict(theirs, ours), Verdict::Differ(_)),
            "and it must not be hiding in the mirrored direction either"
        );
        assert!(
            ExactOracle::new()
                .classify(
                    &Outcome::Returned(marks::STRING_IDENTITY_FALSE),
                    &Outcome::Returned(marks::STRING_IDENTITY_TRUE)
                )
                .is_none(),
            "no entry may claim this pair"
        );
    }

    #[test]
    fn the_known_list_does_not_swallow_ordinary_boolean_results() {
        // The trap every entry has to avoid, and the reason the marker vocabulary exists: a string
        // comparison really does show up as 0 vs 1 in the source, and a matcher written on
        // `Returned(0)` / `Returned(1)` would hide half the real bugs with it. True of the shipped
        // (empty) list, and true of a seeded one.
        assert!(matches!(verdict(Outcome::Returned(0), Outcome::Returned(1)), Verdict::Differ(_)));
        assert!(matches!(
            seeded().verdict(&obs(Outcome::Returned(0)), &obs(Outcome::Returned(1))),
            Verdict::Differ(_)
        ));
    }
}
