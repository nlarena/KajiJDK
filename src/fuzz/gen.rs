//! The **generator** (level 2.3): a typed AST, a deterministic PRNG, and an emitter.
//!
//! This module is most of the fuzzer. The loop, the executor and the oracle are all a page of code
//! each; the difficulty of differential fuzzing is concentrated here, in four properties that a
//! generated program must have *by construction* — because none of them can be checked after the
//! fact without throwing the program away.
//!
//! # The four properties, and how each is guaranteed
//!
//! ## 1. Type-correct
//!
//! Not "generate and hope `javac` agrees": a [`Outcome::CompileError`](super::Outcome::CompileError)
//! is a bug in *this file*, and a campaign full of them tests nothing. So generation is driven from
//! a type context ([`Scope`]) that knows which locals exist and what type each one has, and
//! [`Gen::expr`] is always asked for a specific [`Ty`]. A variable is only ever read at the type it
//! was declared with; an operator only ever gets operands it accepts.
//!
//! The one asymmetry worth naming is the shift: in Java `long << int` is legal and yields `long`,
//! so the shift *amount* is generated as an `int` subexpression on both sides. That is also where
//! the interesting bugs are — see property 4.
//!
//! ## 2. Deterministic
//!
//! The vocabulary is `int`, `long`, `float`, `double`, locals, `if`/`else`, counted `for`, and calls
//! to static methods in the same class. There is deliberately no way to express `hashCode`,
//! `System.nanoTime`, object identity or iteration order. This is enforced by *absence*: the AST has
//! no node that could produce a non-reproducible value, so it is not a rule anybody has to remember.
//!
//! **Floating point is on the deterministic side of that line, and it is worth saying why**, since
//! "floats are non-deterministic" is folklore that was once true here. Java has been strictly IEEE
//! since 17 — JEP 306 removed `strictfp` by making every expression behave as if it had it — so a
//! `double` computation has exactly one right answer, on every platform, in every order. This VM
//! honours that on both engines: the interpreter is Rust `f32`/`f64`, and `burst::compile` uses
//! scalar SSE with `MXCSR`'s round-to-nearest-even and never touches the 80-bit x87 stack, which
//! was the one thing that ever made this genuinely ambiguous. What *is* still non-deterministic is
//! the NaN **payload** — so the grammar never observes one; see [`emit_classifier`], which collapses
//! every NaN to a single code.
//!
//! ## 3. Terminating
//!
//! Three rules, all structural:
//!
//! - the only *counted* loop is `for (int i = 0; i < K; i++)` with **K a literal**, never an
//!   expression. A `while` is allowed too, but only carrying a **guard counter**:
//!   `while (g++ < K && cond)`, with `K` a literal and the increment in the condition itself. The
//!   guard is tested **first**, so it runs at most `K` times whatever `cond` does — and because
//!   `continue` re-tests the condition, it increments the guard too. That is what keeps `while`
//!   inside the same structural argument as `for` instead of trading it for a timeout;
//! - the loop variable is added to the scope as **readable but not assignable**, so a body cannot
//!   reset the counter and run forever. This is the rule that is easy to forget and fatal to omit;
//! - method `k` may only call methods `0..k`, so the call graph is a DAG and recursion is
//!   unrepresentable.
//!
//! Those three make termination certain but not *fast*: nested loops around a call chain multiply,
//! and `5^3` iterations of a body that calls a method that does the same is how a generator
//! accidentally writes a benchmark. So there is a fourth, quantitative rule: a **cost budget**
//! ([`Gen::budget`]). Every construct costs, entering a loop of bound `K` divides the remaining
//! budget by `K`, and a call is only emitted if the callee's already-known static cost fits in what
//! is left. The estimate is exact for this grammar, so [`Program::estimated_cost`] is an upper
//! bound on the work a run can do.
//!
//! ## 4. Total
//!
//! `run()` wraps the body in `try`/`catch` and returns a distinctive marker per exception class —
//! the workaround for FZ-001, where this VM's `run-headless` reports only `-> None` and never the
//! exception's class. See [`marks`] for the choice of values and the argument that a collision
//! cannot manufacture a false finding.
//!
//! # The bias toward edges
//!
//! Constants are not drawn uniformly from `i32`. They come from a loaded pool ([`INT_POOL`],
//! [`LONG_POOL`], [`FLOAT_POOL`], [`DOUBLE_POOL`]) built out of the values where this project's
//! hand-found bugs actually lived: `Long.MIN_VALUE / -1`, shifts with a count at or past the word
//! size, `-1 >>> 1`. A uniform `i32` would hit `32` about once every four billion draws; the pool
//! hits it constantly. The floating pools are the same idea aimed at IEEE's own edges — both zeros,
//! both infinities, a NaN, the subnormal boundary, and the four magnitudes that sit exactly on an
//! `int` or `long` saturation boundary.
//!
//! # Arrays, and what they cost
//!
//! `newarray`, `iaload`/`iastore`, `arraylength`, out-of-range indices and negative lengths are in
//! the grammar. The valuable part is not the reads — it is that **an array access does not throw in
//! compiled code, it deopts**: the JIT's `iaload` emits a bounds guard that leaves native code and
//! hands the pc back to the interpreter, which then raises the exception the compiled code
//! declined to. So [`marks::BOUNDS`] arriving from both engines is a statement about a boundary
//! crossing, not just about a comparison.
//!
//! They cost more JIT coverage than floating point did, and the cost is measured rather than
//! guessed — see [`GenConfig::wide_array_elements`] and
//! `fuzz::campaigns::jit_coverage::what_each_grammar_setting_costs_in_jit_coverage`.
//!
//! # Floating point, and the one thing it cost
//!
//! `float` and `double` are in the grammar. Three of their corners are the point of having them:
//!
//! - **NaN in comparisons.** `fcmpl` and `fcmpg` differ *only* on NaN, which makes a mix-up
//!   invisible on every other input and fatal on that one;
//! - **the narrowing conversions.** `f2i`, `f2l`, `d2i` and `d2l` are saturating and NaN-aware per
//!   JLS §5.1.3, where x86's `cvtt*2si` answers the integer-indefinite value for all three of the
//!   cases that matters. The JIT refuses any method containing one rather than get it wrong, so
//!   **the interpreter is the only implementation that runs**, and nothing has ever checked it
//!   against a real JDK. See [`GenConfig::fp_narrowing`];
//! - **`frem` / `drem`**, which the JIT compiles to an unconditional deopt on purpose — so a
//!   generated `%` on a `double` is a boundary crossing in the middle of native code.
//!
//! What it cost is the **result channel**: the executor observes one `int`, and the obvious way to
//! turn a `double` into one is the very conversion under test. [`emit_classifier`] is the answer,
//! and it states its own limitation.
//!
//! # Objects, and the one thing that is actually being tested
//!
//! `new`, `getfield`, `putfield`, `invokevirtual`, `invokeinterface`, `checkcast` and `instanceof`
//! are in the grammar, over a **fixed hierarchy** ([`ObjClass`]) the program carries with it: four
//! classes under one interface, with an `int`, a `long` and a **reference** field.
//! A local declared as the interface reaches the last of those — same inline cache, but resolved by
//! searching an itable instead of indexing a vtable slot. All five instructions are inside the JIT's
//! subset, which makes this the first stage that costs no coverage by construction rather
//! than by measurement — though it is measured anyway, in
//! `fuzz::campaigns::jit_coverage::what_each_grammar_setting_costs_in_jit_coverage`.
//!
//! What the stage is *for* is the **inline cache**. The JIT gives every dispatched call site a
//! cell holding the class it last saw and the body it bound to, and a guard comparing the
//! receiver's class against it. Two things can happen there and they are opposites:
//!
//! - the guard **holds**, and the call is a direct one inside native code;
//! - the guard **fails**, and the site *deopts*: native code is left, the interpreter's locals,
//!   operand stack and pc are reconstructed from the compiled frame, and execution resumes.
//!
//! That second path is the critical one, and nothing generated could reach it before. It needs one
//! bytecode call site whose receiver's class changes between executions of that same site — which
//! in this grammar means a reassignment inside a loop over a name declared outside it — and rolling
//! that shape out of the ordinary distribution would happen a few times in a thousand seeds. So it
//! is **planted**: [`Gen::dispatch_shape`] puts a monomorphic site, a polymorphic one, or one of
//! each at the head of the entry method, and folds what they compute into the method's result so
//! the work is actually observable. Both halves of that sentence are FZ-004's lesson applied before
//! the fact rather than after.
//!
//! ## Determinism, given that objects have identity
//!
//! Property 2 says the grammar cannot express a non-reproducible value, and an object's identity is
//! exactly such a value. The rule is not "never allocate one" — it is that identity is never
//! **observed**: there is no node that compares two references, no `hashCode`, no `toString`, and
//! [`Ty`] has no reference type, so an identity has nowhere to flow. What survives of an object is
//! its fields and which body a call dispatched to, both of which are functions of the program
//! alone. That is enforced by absence, like the rest of property 2, and checked by
//! `nothing_non_deterministic_can_be_expressed`.
//!
//! # What is out of scope in this version
//!
//! Written down because an unstated scope is indistinguishable from an oversight:
//!
//! | left out | why |
//! |---|---|
//! | multi-dimensional arrays | `multianewarray` is outside the JIT's subset "and not narrowly" (`burst::compile`); a one-dimensional array already reaches every guard worth reaching |
//! | `null` **arrays** | the deopt path is covered by a `null` *receiver* ([`Stmt::NewObject`]), which reaches the same guard from a place the grammar can already express. An array variable that may be `null` needs a reference type in [`Ty`], and objects deliberately avoided that |
//! | NaN **payloads** | the one corner of IEEE that Java leaves implementation-defined; [`emit_classifier`] collapses every NaN to one code deliberately |
//! | locals of type `char`, `byte`, `short`, `boolean` | the narrowing *conversions* are generated ([`NarrowTy`]), which is where the truncation bugs are; declaring locals of those types multiplies the type context without adding a failure mode |
//! | `do`/`while`, labelled `break`/`continue` | the loop forms that are generated carry a structural termination argument ([`Stmt::While`]); a label lets control leave a loop the argument was written about |
//! | recursion | see property 3 |
//! | **races** a deterministic program cannot have | threads themselves are generated ([`Stmt::Fork`]), but the shape is rigid on purpose so the answer stays fixed, and a shape that is deterministic can only expose a race that **breaks** determinism. That is the right class for a VM bug (a stale reference under GC gives a different answer) and not for a program-level interleaving, which the grammar declines to express |

use std::fmt::Write as _;

use super::{Program, Seed};

/// The integers a generated program returns instead of letting an exception escape.
///
/// # Why these values
///
/// This VM does not surface an exception's class through `run-headless` (FZ-001), so a program that
/// throws is indistinguishable from any other program that throws. The workaround is for the
/// program to catch itself and encode *which* exception it caught in the `int` it returns — which
/// only works if a marker cannot be confused with an ordinary result.
///
/// The values sit in a `0x5AFE_00nn` band: large, odd-looking, and nowhere near anything
/// [`INT_POOL`] contains or that one operation on pool constants lands on.
///
/// # The collision argument
///
/// A marker collision is possible in principle — the body could legitimately compute
/// `0x5AFE_0001`. It is worth being precise about what that would cost, because the answer is
/// reassuring: **a collision cannot manufacture a false finding.** Both paths run the same source,
/// so if the body honestly computes a marker value it computes it on both sides and the oracle sees
/// agreement. The only damage is the reverse — one path returning the marker value honestly while
/// the other throws that exception — which would *mask* one real divergence, on one exact value.
/// That is a risk worth taking for an exact integer comparison on both sides.
pub mod marks {
    /// `ArithmeticException`: division or remainder by zero.
    pub const ARITHMETIC: i32 = 0x5AFE_0001u32 as i32;
    /// `ArrayIndexOutOfBoundsException`: an index outside `[0, length)`.
    pub const BOUNDS: i32 = 0x5AFE_0002u32 as i32;
    /// `NullPointerException`: a `getfield`, `putfield` or `invokevirtual` on a `null` receiver.
    ///
    /// Reachable since objects entered the grammar, and worth more than it looks: in compiled code
    /// none of the three *throws*, they **deopt** — the null check leaves native code and hands the
    /// pc back to the interpreter, which raises what the compiled code declined to. So this marker
    /// arriving from both engines is a statement about a boundary crossing, exactly as
    /// [`BOUNDS`] is.
    pub const NULL: i32 = 0x5AFE_0003u32 as i32;
    /// `ClassCastException`. Reserved; the grammar has no `checkcast` — every object local is
    /// declared at the base type and never narrowed.
    pub const CLASS_CAST: i32 = 0x5AFE_0004u32 as i32;
    /// `StackOverflowError`. Reserved; the call graph is a DAG, so it should be unreachable.
    pub const STACK_OVERFLOW: i32 = 0x5AFE_0005u32 as i32;
    /// `NegativeArraySizeException`: `new int[-1]`.
    ///
    /// It gets its own marker rather than falling into [`OTHER`] because it and [`BOUNDS`] are the
    /// two ways an array allocation can fail, and a campaign that could not tell them apart would
    /// read "the two engines both threw something" as agreement — which is exactly the shape of
    /// the bug worth catching here, an engine that checks the length where the other checks the
    /// index.
    pub const NEGATIVE_SIZE: i32 = 0x5AFE_0006u32 as i32;
    /// Anything else that reached the outermost `catch (Throwable)`.
    pub const OTHER: i32 = 0x5AFE_000Fu32 as i32;

    /// A program whose only observable was `"a" == "a"`, and it was `false` — **a bug**.
    ///
    /// Since **FZ-008** (2026-08-29) `strings::intern` is a real JLS §3.10.5 pool — one instance
    /// per literal, `malloc_old`ed, a GC root and pinned out of `gc::compact` — so a conforming
    /// answer here is [`STRING_IDENTITY_TRUE`], and this marker now means the pool was bypassed or
    /// the literal moved. **The same sentence was written here once before and was not true**: the
    /// pool had not landed, and the probe that should have said so was being folded away by `javac`
    /// (FZ-009). It is true now, and the tests that hold it are
    /// `jvm::interpreter::gc::tests::a_pooled_literal_*`. It used to be the *expected* answer on
    /// this VM,
    /// suppressed by an entry in the oracle's known-divergence list; that entry is gone, and the
    /// pair is now reported like any other divergence — see [`super::super::oracle`].
    ///
    /// It stays a distinct marker rather than a plain `0` because the oracle can only match on what
    /// a program returns, and `Returned(0)` vs `Returned(1)` is far too broad a shape to say
    /// anything about — whether the verdict wanted is "suppress" or, as now, "report precisely
    /// this".
    pub const STRING_IDENTITY_FALSE: i32 = 0x5AFE_0010u32 as i32;
    /// The same program, where it was `true` — what a real JDK returns, and what this VM returns
    /// too now that literals are interned.
    pub const STRING_IDENTITY_TRUE: i32 = 0x5AFE_0011u32 as i32;
}

// ---------------------------------------------------------------------------------------------
// The PRNG
// ---------------------------------------------------------------------------------------------

/// SplitMix64: ten lines, deterministic, and good enough that consecutive seeds produce unrelated
/// streams — which matters, because a campaign walks seeds `0, 1, 2, …` and a weaker generator
/// (a plain LCG, say) would hand back programs that differ only in their last constant.
///
/// Written out rather than pulled in: this project has already turned down `nom` and `byteorder`
/// for the same reason, and a PRNG whose exact output is part of the reproduction contract is
/// precisely the thing not to leave to a version bump.
#[derive(Clone, Debug)]
pub struct Rng {
    state: u64,
}

impl Rng {
    /// The stream for one seed. Same seed, same stream, forever — that is the whole contract.
    pub fn from_seed(seed: Seed) -> Rng {
        Rng { state: seed.0 }
    }

    fn next_u64(&mut self) -> u64 {
        self.state = self.state.wrapping_add(0x9E37_79B9_7F4A_7C15);
        let mut z = self.state;
        z = (z ^ (z >> 30)).wrapping_mul(0xBF58_476D_1CE4_E5B9);
        z = (z ^ (z >> 27)).wrapping_mul(0x94D0_49BB_1331_11EB);
        z ^ (z >> 31)
    }

    /// A value in `0..n`. Uses the high bits through a widening multiply — cheaper than rejection
    /// sampling and with a bias below one part in `2^32`, which is far below anything a fuzzer's
    /// distribution decisions would notice.
    pub fn below(&mut self, n: u32) -> u32 {
        debug_assert!(n > 0, "below(0) has no answer");
        ((self.next_u64() as u128 * n as u128) >> 64) as u32
    }

    /// `true` with probability `num / den`.
    pub fn chance(&mut self, num: u32, den: u32) -> bool {
        self.below(den) < num
    }

    /// One of `xs`. Panics on an empty slice, which would always be a bug in the caller's grammar.
    pub fn pick<'a, T>(&mut self, xs: &'a [T]) -> &'a T {
        &xs[self.below(xs.len() as u32) as usize]
    }
}

// ---------------------------------------------------------------------------------------------
// The constant pools
// ---------------------------------------------------------------------------------------------

/// Where `int` bugs live. Not a uniform draw: every value here is either a boundary of the type, a
/// boundary of a shift count, or a value this project has already been bitten by.
pub const INT_POOL: &[i32] = &[
    i32::MIN,
    i32::MIN + 1,
    -65,
    -64,
    -33,
    -32,
    -31,
    -2,
    -1,
    0,
    1,
    2,
    3,
    7,
    31,
    32,
    33,
    63,
    64,
    65,
    255,
    256,
    65535,
    65536,
    i32::MAX - 1,
    i32::MAX,
];

/// The same for `long`. `Long.MIN_VALUE` earns its place on its own: `Long.MIN_VALUE / -1` is the
/// one division in Java that overflows instead of throwing, and this project found it by hand.
pub const LONG_POOL: &[i64] = &[
    i64::MIN,
    i64::MIN + 1,
    i32::MIN as i64,
    -65,
    -64,
    -33,
    -32,
    -1,
    0,
    1,
    2,
    31,
    32,
    33,
    63,
    64,
    65,
    i32::MAX as i64,
    i32::MAX as i64 + 1,
    4294967296,
    i64::MAX - 1,
    i64::MAX,
];

/// Where `float` bugs live, **as bit patterns**.
///
/// Bits rather than `f32` for two reasons, and both of them are structural rather than stylistic:
///
/// - [`Expr`] derives `Eq`, which `f32` does not implement. The reducer compares expressions for
///   equality constantly (to know whether a shrink changed anything), so an `Expr` that cannot be
///   `Eq` would cost far more than storing four bytes;
/// - `0.0` and `-0.0` are `==` to each other and `NaN` is `==` to nothing, *including itself*.
///   Those are exactly the values this pool exists to reach, and a representation in which the
///   pool cannot tell its own entries apart is a representation that would silently deduplicate
///   the two most interesting ones.
///
/// The entries: both zeros, both infinities, a NaN, both ends of the subnormal range, the
/// normal-number boundary, `MAX_VALUE`, and the four magnitudes that sit exactly on an `int` or
/// `long` saturation boundary — because [`Expr::Cast`] to an integral type is where JLS §5.1.3
/// disagrees with what x86 does, and a conversion that is never handed a boundary never tests it.
pub const FLOAT_POOL: &[u32] = &[
    0x0000_0000, // 0.0
    0x8000_0000, // -0.0
    0x3F80_0000, // 1.0
    0xBF80_0000, // -1.0
    0x4000_0000, // 2.0
    0x3F00_0000, // 0.5
    0x4040_0000, // 3.0
    0x3DCC_CCCD, // 0.1 — not exact in binary, which is the point
    0x7FC0_0000, // NaN (the canonical quiet one)
    0x7F80_0000, // +Infinity
    0xFF80_0000, // -Infinity
    0x0000_0001, // Float.MIN_VALUE, the smallest subnormal
    0x8000_0001, // -Float.MIN_VALUE
    0x007F_FFFF, // the largest subnormal
    0x0080_0000, // Float.MIN_NORMAL
    0x7F7F_FFFF, // Float.MAX_VALUE
    0xFF7F_FFFF, // -Float.MAX_VALUE
    0x4B00_0000, // 8388608.0 = 2^23, where consecutive floats stop being consecutive integers
    0x4EFF_FFFF, // 2147483520.0, the largest float below Integer.MAX_VALUE
    0x4F00_0000, // 2147483648.0 — one past Integer.MAX_VALUE, so `(int)` must saturate
    0xCF00_0000, // -2147483648.0 = Integer.MIN_VALUE exactly
    0x5F00_0000, // 9.223372E18 — one past Long.MAX_VALUE, so `(long)` must saturate
    0xDF00_0000, // -9.223372E18 = Long.MIN_VALUE exactly
];

/// The same for `double`, and for the same reasons. The `int` and `long` saturation boundaries are
/// representable *exactly* here, which `float` cannot manage — so this pool is the one that pins
/// `(int) 2147483647.0` against `(int) 2147483648.0`, a pair a `float` literal cannot even express.
pub const DOUBLE_POOL: &[u64] = &[
    0x0000_0000_0000_0000, // 0.0
    0x8000_0000_0000_0000, // -0.0
    0x3FF0_0000_0000_0000, // 1.0
    0xBFF0_0000_0000_0000, // -1.0
    0x4000_0000_0000_0000, // 2.0
    0x3FE0_0000_0000_0000, // 0.5
    0x4008_0000_0000_0000, // 3.0
    0x3FB9_9999_9999_999A, // 0.1
    0x7FF8_0000_0000_0000, // NaN
    0x7FF0_0000_0000_0000, // +Infinity
    0xFFF0_0000_0000_0000, // -Infinity
    0x0000_0000_0000_0001, // Double.MIN_VALUE
    0x8000_0000_0000_0001, // -Double.MIN_VALUE
    0x000F_FFFF_FFFF_FFFF, // the largest subnormal
    0x0010_0000_0000_0000, // Double.MIN_NORMAL
    0x7FEF_FFFF_FFFF_FFFF, // Double.MAX_VALUE
    0xFFEF_FFFF_FFFF_FFFF, // -Double.MAX_VALUE
    0x4330_0000_0000_0000, // 2^52, where consecutive doubles stop being consecutive integers
    0x41DF_FFFF_FFC0_0000, // 2147483647.0 = Integer.MAX_VALUE exactly
    0x41E0_0000_0000_0000, // 2147483648.0 — one past it, where `(int)` must saturate
    0xC1E0_0000_0000_0000, // -2147483648.0 = Integer.MIN_VALUE exactly
    0x43DF_FFFF_FFFF_FFFF, // the largest double below Long.MAX_VALUE
    0x43E0_0000_0000_0000, // 9.223372036854776E18 — one past it, where `(long)` must saturate
    0xC3E0_0000_0000_0000, // -9.223372036854776E18 = Long.MIN_VALUE exactly
];

/// The magnitude ladder [`Expr::Classify`] walks a `double` down. See [`emit_classifier`] for why a
/// comparison ladder is the channel and a cast is not.
///
/// Ordered, and covering the range from `-MAX_VALUE` to `MAX_VALUE` with the saturation boundaries
/// on it — a probe the pool can land on *exactly* is a probe that reports an exact answer, and
/// arithmetic on pool constants lands on pool constants far more often than chance would suggest.
const DOUBLE_PROBES: &[u64] = &[
    0xFFEF_FFFF_FFFF_FFFF, // -Double.MAX_VALUE
    0xC3E0_0000_0000_0000, // -9.223372036854776E18
    0xC1E0_0000_0000_0000, // -2147483648.0
    0xBFF0_0000_0000_0000, // -1.0
    0x8000_0000_0000_0001, // -Double.MIN_VALUE
    0x0000_0000_0000_0001, // Double.MIN_VALUE
    0x0010_0000_0000_0000, // Double.MIN_NORMAL
    0x3FF0_0000_0000_0000, // 1.0
    0x4000_0000_0000_0000, // 2.0
    0x4008_0000_0000_0000, // 3.0
    0x41DF_FFFF_FFC0_0000, // 2147483647.0
    0x41E0_0000_0000_0000, // 2147483648.0
    0x43E0_0000_0000_0000, // 9.223372036854776E18
    0x7FEF_FFFF_FFFF_FFFF, // Double.MAX_VALUE
];

/// The same ladder for `float`.
const FLOAT_PROBES: &[u32] = &[
    0xFF7F_FFFF, // -Float.MAX_VALUE
    0xDF00_0000, // -9.223372E18
    0xCF00_0000, // -2147483648.0
    0xBF80_0000, // -1.0
    0x8000_0001, // -Float.MIN_VALUE
    0x0000_0001, // Float.MIN_VALUE
    0x0080_0000, // Float.MIN_NORMAL
    0x3F80_0000, // 1.0
    0x4000_0000, // 2.0
    0x4040_0000, // 3.0
    0x4EFF_FFFF, // 2147483520.0
    0x4F00_0000, // 2147483648.0
    0x5F00_0000, // 9.223372E18
    0x7F7F_FFFF, // Float.MAX_VALUE
];

// ---------------------------------------------------------------------------------------------
// The AST
// ---------------------------------------------------------------------------------------------

/// The types in the grammar. [`Scope`] stays a flat list and [`Gen::expr`] stays total: for every
/// `Ty` there is always *some* way to produce a value, and [`Gen::leaf`] is the proof.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum Ty {
    Int,
    Long,
    Float,
    Double,
}

impl Ty {
    fn keyword(self) -> &'static str {
        match self {
            Ty::Int => "int",
            Ty::Long => "long",
            Ty::Float => "float",
            Ty::Double => "double",
        }
    }

    /// `float` and `double`. The single most consulted predicate in this file once IEEE arrived:
    /// bitwise operators, shifts, `~` and the zero-guard are all integral-only, and each of them
    /// would be a `javac` rejection — i.e. a generator bug — if it ever reached a floating operand.
    pub fn is_fp(self) -> bool {
        matches!(self, Ty::Float | Ty::Double)
    }
}

/// Binary operators that take two operands of the same type and give that type back.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum BinOp {
    Add,
    Sub,
    Mul,
    Div,
    Rem,
    And,
    Or,
    Xor,
}

impl BinOp {
    fn symbol(self) -> &'static str {
        match self {
            BinOp::Add => "+",
            BinOp::Sub => "-",
            BinOp::Mul => "*",
            BinOp::Div => "/",
            BinOp::Rem => "%",
            BinOp::And => "&",
            BinOp::Or => "|",
            BinOp::Xor => "^",
        }
    }

    /// Whether a zero right-hand side throws. The generator uses this to decide when to reach for a
    /// zero guard, and the reducer uses it to avoid shrinking a divisor into an exception.
    ///
    /// **Integral division only.** `1.0 / 0.0` is `Infinity` and `0.0 % 0.0` is `NaN`; neither
    /// throws anything, and both are values this fuzzer very much wants to see. So every caller
    /// pairs this with [`Ty::is_fp`] — see [`Gen::expr`].
    pub fn traps_on_zero(self) -> bool {
        matches!(self, BinOp::Div | BinOp::Rem)
    }

    /// Whether this operator accepts operands of `ty`. `&`, `|` and `^` are integral-only in Java;
    /// handing one a `double` is a compile error, which property 1 exists to make unreachable.
    pub fn accepts(self, ty: Ty) -> bool {
        match self {
            BinOp::And | BinOp::Or | BinOp::Xor => !ty.is_fp(),
            _ => true,
        }
    }
}

/// The three shifts. Separate from [`BinOp`] because their operand types differ: the amount is
/// always an `int`, whatever the value being shifted is.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum ShiftOp {
    Left,
    /// Arithmetic right shift, `>>`.
    Right,
    /// Logical right shift, `>>>` — where `-1 >>> 1` lives.
    Unsigned,
}

impl ShiftOp {
    fn symbol(self) -> &'static str {
        match self {
            ShiftOp::Left => "<<",
            ShiftOp::Right => ">>",
            ShiftOp::Unsigned => ">>>",
        }
    }
}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum CmpOp {
    Lt,
    Le,
    Gt,
    Ge,
    Eq,
    Ne,
}

impl CmpOp {
    fn symbol(self) -> &'static str {
        match self {
            CmpOp::Lt => "<",
            CmpOp::Le => "<=",
            CmpOp::Gt => ">",
            CmpOp::Ge => ">=",
            CmpOp::Eq => "==",
            CmpOp::Ne => "!=",
        }
    }
}

/// One of the four classes of the **fixed hierarchy** a program carries when it uses objects.
///
/// # Why the hierarchy is fixed and the *use* is generated
///
/// The dimension that matters for this stage is not the shape of the class graph — it is **how many
/// distinct receiver classes reach one call site, and in what order**. That is a property of the
/// code around the call, not of the classes, so the classes are a constant and the interesting
/// variation goes where it pays: which class each `new` picks, how often the receiver is reassigned,
/// what the constructor argument computes to.
///
/// The four cover the three ways a `invokevirtual` can resolve:
///
/// | class | `v()` | `w()` | what it is for |
/// |---|---|---|---|
/// | `B` | own | own | the base; the receiver when nothing is subclassed |
/// | `S0` | overrides | inherits | the ordinary override |
/// | `S1` | overrides | overrides | a fully overriding subclass |
/// | `S2` | inherits | inherits | **no override at all** — the vtable slot must carry `B`'s body down |
///
/// `S2` is the one that is easy to leave out and worth having: a vtable built by copying only the
/// methods a class *declares* passes every test that uses `S0` and `S1`, and fails only here.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum ObjClass {
    Base,
    S0,
    S1,
    S2,
}

impl ObjClass {
    /// The suffix appended to the program's class name. The classes are named per program
    /// (`Fz7B`, `Fz7S0`, …) rather than with fixed names, because the executor compiles every
    /// program into the **same working directory**: a globally-named `B.class` from one seed would
    /// still be sitting there when the next seed ran, and the day the hierarchy stops being a
    /// constant that becomes a stale-class bug nobody would look for.
    fn suffix(self) -> &'static str {
        match self {
            ObjClass::Base => "B",
            ObjClass::S0 => "S0",
            ObjClass::S1 => "S1",
            ObjClass::S2 => "S2",
        }
    }
}

/// The two instance fields every object in the hierarchy has.
///
/// Both are **primitive**, and that is a constraint rather than a simplification: a `putfield` of a
/// *reference* needs the GC's write barrier, which this JIT tier cannot emit (`burst::compile`
/// answers `Ineligible` for one). A reference field would therefore take every method that touched
/// one straight out of the compiled arm — FZ-004 again, wearing the write barrier's hat.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum Field {
    /// `int a` — the only width **inside the JIT's subset**: `burst::compile`'s resolver answers
    /// an offset for a non-volatile `int` instance field and refuses everything else.
    A,
    /// `long b` — outside it, and gated behind [`GenConfig::wide_fields`] for exactly the reason
    /// [`GenConfig::wide_array_elements`] is gated.
    B,
}

impl Field {
    /// The field of this type, if the hierarchy has one — `int a`, or `long b` when the wide half
    /// is enabled. `None` for the floating types: the hierarchy has no floating half, and inventing
    /// one to satisfy a read would widen the stage past what it is for.
    fn of_ty(ty: Ty, wide: bool) -> Option<Field> {
        match ty {
            Ty::Int => Some(Field::A),
            Ty::Long if wide => Some(Field::B),
            _ => None,
        }
    }

    fn name(self) -> &'static str {
        match self {
            Field::A => "a",
            Field::B => "b",
        }
    }

    pub fn ty(self) -> Ty {
        match self {
            Field::A => Ty::Int,
            Field::B => Ty::Long,
        }
    }
}

/// A virtual method of the hierarchy. Nullary on purpose: argument marshalling is already covered
/// by [`Expr::Call`], and what this node exists for is the **dispatch**, which takes no arguments
/// to get wrong.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum VMethod {
    /// `int v()`.
    V,
    /// `long w()`.
    W,
}

impl VMethod {
    fn name(self) -> &'static str {
        match self {
            VMethod::V => "v",
            VMethod::W => "w",
        }
    }

    pub fn ty(self) -> Ty {
        match self {
            VMethod::V => Ty::Int,
            VMethod::W => Ty::Long,
        }
    }
}

/// An expression of a known [`Ty`].
///
/// Every variant carries enough to answer [`Expr::ty`] without a symbol table, which is what lets
/// the reducer rewrite a subtree and immediately know whether the result still type-checks.
/// The exception an explicit `throw` raises.
///
/// Only classes the total wrapper already catches by name, so a `throw` lands on a **mark** the
/// two engines can compare. Throwing something the wrapper only caught as `Throwable` would still
/// terminate, but it would collapse four distinguishable outcomes into one.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum ThrownExc {
    Arithmetic,
    Bounds,
    NegativeSize,
    NullPointer,
}

impl ThrownExc {
    fn class(self) -> &'static str {
        match self {
            ThrownExc::Arithmetic => "ArithmeticException",
            ThrownExc::Bounds => "ArrayIndexOutOfBoundsException",
            ThrownExc::NegativeSize => "NegativeArraySizeException",
            ThrownExc::NullPointer => "NullPointerException",
        }
    }
}

/// The width an `int` can be truncated to and read back from: `i2b`, `i2s` and `i2c`.
///
/// These are **not** new [`Ty`]s, and that is the whole design. Java's binary numeric promotion
/// says `byte + byte` is an `int`, so a real `byte` in the lattice would break the one invariant
/// every operator here relies on — two operands of a type give that type back — and would put a
/// promotion rule in every arm. A narrowing is instead a **round trip**: `int` in, truncate,
/// sign- or zero-extend, `int` out. The interesting semantics survive (truncation, and `char`
/// extending with zero where `byte` and `short` extend with the sign) and the type context does
/// not multiply.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum NarrowTy {
    Byte,
    Short,
    /// The one that is not like the others: `i2c` **zero**-extends, so `(char) -1` is 65535 and not
    /// -1. A generator that only drew `byte` and `short` would never ask that question.
    Char,
}

impl NarrowTy {
    fn keyword(self) -> &'static str {
        match self {
            NarrowTy::Byte => "byte",
            NarrowTy::Short => "short",
            NarrowTy::Char => "char",
        }
    }
}

/// The literals a generated `String` can be. All **ASCII** on purpose: the emitter's modified-UTF-8
/// bug (#128) and the missing `String.valueOf([CII)` native (#226) are open, so a non-ASCII literal
/// would manufacture divergences that are already known and would drown the ones that are not. The
/// pool repeats no value by accident — two occurrences of the *same* literal is exactly what
/// [`StrProbe::Identity`] needs to ask its question.
pub const STRING_POOL: &[&str] = &["", "a", "ab", "abc", "kaji", "0", "true", "null"];

/// A `String`-valued expression.
///
/// Strings live **outside** [`Ty`] deliberately. `Ty` is the *arithmetic* lattice — every operator
/// in the grammar takes two operands of one `Ty` and gives that `Ty` back — and a `String` has no
/// arithmetic. Adding it there would put a guard on every operator, which is the cost
/// [`Expr::Classify`] already avoided for floating point by making the odd type reach the observed
/// `int` through one dedicated node instead of through the lattice.
#[derive(Clone, PartialEq, Eq, Debug)]
pub enum StrExpr {
    /// An index into [`STRING_POOL`].
    Lit(usize),
    /// `a + b`. Two literals concatenated is a **compile-time constant expression** (JLS §15.28),
    /// so `javac` folds it and interns the result — which makes `("a" + "b") == "ab"` true in real
    /// Java. That is a fact about the *compiler*, and this node is how the fuzzer gets to ask
    /// whether ours agrees.
    Concat(Box<StrExpr>, Box<StrExpr>),
    /// `new String(s)` — a distinct object with equal contents. It exists for
    /// [`StrProbe::Identity`]: `==` must be **false** here and **true** between two occurrences of
    /// the same literal (JLS §3.10.5).
    Fresh(Box<StrExpr>),
}

impl StrExpr {
    fn size(&self) -> usize {
        match self {
            StrExpr::Lit(_) => 1,
            StrExpr::Fresh(a) => 1 + a.size(),
            StrExpr::Concat(a, b) => 1 + a.size() + b.size(),
        }
    }
}

/// How a [`StrExpr`] becomes the single `int` the executor observes.
#[derive(Clone, PartialEq, Eq, Debug)]
pub enum StrProbe {
    /// `s.length()` — in **UTF-16 code units**, which is what a Java `String` is measured in.
    Length,
    /// `a.equals(b) ? 1 : 0` — equality by contents.
    Identity(Box<StrExpr>),
    /// `a == b ? 1 : 0` — equality by **reference**.
    ///
    /// This is the probe that earns the stage, and it took two tries to make it earn anything.
    /// `"a" == "a"` spent time on the oracle's known-divergence list as an accepted difference and
    /// turned out to be a non-conformance with JLS §3.10.5: literals must be interned. The
    /// suppression was removed **on the strength of a claim that the interning table had landed**,
    /// and it had not — `strings::intern` still `malloc`s a fresh object per `ldc`, which its own
    /// module header says plainly.
    ///
    /// The campaign did not catch that, because this probe used to be emitted **inline** and
    /// `javac` folds `("a" == "a")` to `true` before the VM sees a single `ldc`. So the check ran
    /// against the compiler's constant folder and reported agreement. It goes through `ssame` now
    /// — see [`emit_str_probe`] — and finds it: FZ-008.
    Same(Box<StrExpr>),
}

impl StrProbe {
    fn size(&self) -> usize {
        match self {
            StrProbe::Length => 1,
            StrProbe::Identity(b) | StrProbe::Same(b) => 1 + b.size(),
        }
    }
}

#[derive(Clone, PartialEq, Eq, Debug)]
pub enum Expr {
    IntLit(i32),
    LongLit(i64),
    /// A `float` constant, **by bit pattern** — see [`FLOAT_POOL`] for why not an `f32`.
    FloatLit(u32),
    /// A `double` constant, by bit pattern.
    DoubleLit(u64),
    /// A local or parameter. The [`Ty`] is carried on the node so the reducer never needs the
    /// scope to answer a type question.
    Var(String, Ty),
    Neg(Box<Expr>),
    Not(Box<Expr>),
    Bin(BinOp, Box<Expr>, Box<Expr>),
    /// `value <op> amount`, where `amount` is always `int`.
    Shift(ShiftOp, Box<Expr>, Box<Expr>),
    /// `(to) inner` — the only conversion in the grammar, and both directions are interesting:
    /// widening is sign extension, narrowing is truncation.
    Cast(Ty, Box<Expr>),
    /// `cond ? then : otherwise`, with both arms of the node's type. This is how a comparison
    /// becomes a value, since the grammar has no `boolean` locals.
    Ternary(Box<Cond>, Box<Expr>, Box<Expr>),
    /// A call to a method of the enclosing class, by index into [`Program::methods`].
    Call(usize, Vec<Expr>, Ty),
    /// **The floating-point result channel**: an IEEE fingerprint of a `float` or `double`, as an
    /// `int`, computed with comparisons only. The inner expression is always floating; the node is
    /// always `int`. See [`emit_classifier`] for the whole argument.
    Classify(Box<Expr>),
    /// `a[i]` — the array by name, the *element* type, and an `int` index. The index is an
    /// arbitrary expression on purpose: an index outside `[0, length)` is an
    /// `ArrayIndexOutOfBoundsException`, which the total wrapper turns into [`marks::BOUNDS`] and
    /// which both engines must agree about.
    ArrayLoad(String, Ty, Box<Expr>),
    /// `a.length`. Always `int`, and the one array operation that cannot throw — unless the array
    /// is null, which this grammar cannot express.
    ArrayLength(String),
    /// `(byte) e` / `(short) e` / `(char) e`, of an `int`, read back as an `int`. Always `int`:
    /// Java promotes the result the moment it is used, and modelling that promotion is exactly the
    /// complexity [`NarrowTy`] exists to avoid.
    Narrow(NarrowTy, Box<Expr>),
    /// A `String` question answered as an `int` — the only way a string reaches the observed
    /// value. Always `int`, whatever the strings inside it are.
    Str(StrProbe, Box<StrExpr>),
    /// `o.a` — a `getfield` on the object local named here. Throws a `NullPointerException` when
    /// the receiver is `null`, which in compiled code is a **deopt**, not a throw.
    Field(String, Field),
    /// `o.c.a` / `o.c.b` — a read **through** the reference field: a `getfield` of a reference,
    /// then a `getfield` of a primitive on whatever it landed on.
    ///
    /// Depth one and no deeper: a second hop exercises the same two opcodes.
    ///
    /// **The constructor initialises `c` to `this`**, so a chain on a fresh object reads fine. That
    /// was not the first design — leaving the field `null` made the null receiver free, which is a
    /// deopt on the compiled arm and looked like a bonus. It was measured instead of assumed:
    /// **41 of 80 seeds died on an exception marker** against 9 for the default grammar, because
    /// every chain threw on warm-up iteration 1. That is FZ-005 exactly, so the receiver is live by
    /// default and `null` is reached the way everything else in this grammar is reached — by a
    /// statement that puts it there ([`Stmt::RefStore`] with `None`).
    ThroughRef(String, Field),
    /// `o.v()` — an `invokevirtual` on the object local named here.
    ///
    /// **The point of this whole stage.** The JIT gives every dispatched call site an inline cache:
    /// a cell holding the class it saw last and the body it bound to, plus a guard comparing the
    /// receiver's class against it. A site whose receiver never changes takes the guard every time
    /// and never leaves native code; a site whose receiver rotates fails the guard and **deopts**,
    /// reconstructing the interpreter's locals, operand stack and pc from the compiled frame. The
    /// two live one line apart in a planted shape — see [`Gen::dispatch_shape`].
    Virtual(String, VMethod),
}

impl Expr {
    pub fn ty(&self) -> Ty {
        match self {
            Expr::IntLit(_) => Ty::Int,
            Expr::LongLit(_) => Ty::Long,
            Expr::FloatLit(_) => Ty::Float,
            Expr::DoubleLit(_) => Ty::Double,
            Expr::Var(_, ty) => *ty,
            Expr::Neg(inner) | Expr::Not(inner) => inner.ty(),
            Expr::Bin(_, left, _) => left.ty(),
            Expr::Shift(_, value, _) => value.ty(),
            Expr::Cast(to, _) => *to,
            Expr::Ternary(_, then, _) => then.ty(),
            Expr::Call(_, _, ty) => *ty,
            // The same type as the field it ends on: the reference hop is plumbing, the read is
            // what produces the value.
            Expr::ThroughRef(_, field) => field.ty(),
            Expr::Classify(_)
            | Expr::ArrayLength(_)
            | Expr::Str(_, _)
            | Expr::Narrow(_, _) => Ty::Int,
            Expr::ArrayLoad(_, elem, _) => *elem,
            Expr::Field(_, field) => field.ty(),
            Expr::Virtual(_, method) => method.ty(),
        }
    }

    /// The neutral value of a type — what the reducer replaces a subtree with.
    ///
    /// `0.0` and not `-0.0`: the reducer's order is on *bit patterns* (see
    /// [`super::reduce::weight`]), and `-0.0` has the sign bit set, so it is the larger of the two.
    /// Picking it here would make "replace this subtree with zero" a step the reducer refuses.
    pub fn zero(ty: Ty) -> Expr {
        match ty {
            Ty::Int => Expr::IntLit(0),
            Ty::Long => Expr::LongLit(0),
            Ty::Float => Expr::FloatLit(0),
            Ty::Double => Expr::DoubleLit(0),
        }
    }

    /// Node count, for measuring that the reducer really shrank something.
    pub fn size(&self) -> usize {
        match self {
            Expr::IntLit(_)
            | Expr::LongLit(_)
            | Expr::FloatLit(_)
            | Expr::DoubleLit(_)
            | Expr::ArrayLength(_)
            | Expr::Field(_, _)
            | Expr::ThroughRef(_, _)
            | Expr::Virtual(_, _)
            | Expr::Var(_, _) => 1,
            Expr::Neg(inner)
            | Expr::Not(inner)
            | Expr::Cast(_, inner)
            | Expr::Classify(inner)
            | Expr::ArrayLoad(_, _, inner) => 1 + inner.size(),
            Expr::Bin(_, a, b) | Expr::Shift(_, a, b) => 1 + a.size() + b.size(),
            Expr::Str(probe, s) => 1 + probe.size() + s.size(),
            Expr::Narrow(_, a) => 1 + a.size(),
            Expr::Ternary(c, a, b) => 1 + c.size() + a.size() + b.size(),
            Expr::Call(_, args, _) => 1 + args.iter().map(Expr::size).sum::<usize>(),
        }
    }

    /// Whether this expression classifies a value of `ty` anywhere inside it. The emitter needs it
    /// to decide which of the two classifier helpers a program actually has to carry: emitting
    /// both unconditionally would put `float` and `double` in the text of every program, including
    /// the ones the reducer has just finished stripping all the floating point out of.
    fn classifies(&self, ty: Ty) -> bool {
        match self {
            Expr::Classify(a) => a.ty() == ty || a.classifies(ty),
            // A string subtree holds no floating expression in this stage, so it can never keep a
            // `float` alive that the reducer just stripped.
            Expr::Str(_, _) => false,
            // A narrowing does: its operand is an `int` expression, and an `int` expression may be
            // a classifier over a `double`.
            Expr::Narrow(_, a) => a.classifies(ty),
            Expr::IntLit(_)
            | Expr::LongLit(_)
            | Expr::FloatLit(_)
            | Expr::DoubleLit(_)
            | Expr::ArrayLength(_)
            | Expr::Field(_, _)
            | Expr::ThroughRef(_, _)
            | Expr::Virtual(_, _)
            | Expr::Var(_, _) => false,
            Expr::Neg(a) | Expr::Not(a) | Expr::Cast(_, a) | Expr::ArrayLoad(_, _, a) => {
                a.classifies(ty)
            }
            Expr::Bin(_, a, b) | Expr::Shift(_, a, b) => a.classifies(ty) || b.classifies(ty),
            Expr::Ternary(c, a, b) => {
                c.classifies(ty) || a.classifies(ty) || b.classifies(ty)
            }
            Expr::Call(_, args, _) => args.iter().any(|a| a.classifies(ty)),
        }
    }
}

/// A boolean, which exists only to be the condition of an `if` or a `?:`. Kept out of [`Expr`] so
/// that `Expr` stays mono-typed over `{int, long}` and no generation path can accidentally hand a
/// `boolean` to `+`.
#[derive(Clone, PartialEq, Eq, Debug)]
pub enum Cond {
    /// Both sides have the same [`Ty`]; the emitter does not need to know which.
    Cmp(CmpOp, Expr, Expr),
    And(Box<Cond>, Box<Cond>),
    Or(Box<Cond>, Box<Cond>),
    Not(Box<Cond>),
}

impl Cond {
    pub fn size(&self) -> usize {
        match self {
            Cond::Cmp(_, a, b) => 1 + a.size() + b.size(),
            Cond::And(a, b) | Cond::Or(a, b) => 1 + a.size() + b.size(),
            Cond::Not(a) => 1 + a.size(),
        }
    }

    fn classifies(&self, ty: Ty) -> bool {
        match self {
            Cond::Cmp(_, a, b) => a.classifies(ty) || b.classifies(ty),
            Cond::And(a, b) | Cond::Or(a, b) => a.classifies(ty) || b.classifies(ty),
            Cond::Not(a) => a.classifies(ty),
        }
    }
}

/// One arm of a [`Stmt::Switch`].
#[derive(Clone, PartialEq, Eq, Debug)]
pub struct SwitchArm {
    pub label: i32,
    pub body: Block,
    /// Whether the arm ends in `break`.
    ///
    /// **`false` is the point of the whole construct.** Without the `break`, control falls into the
    /// next arm — and fall-through is a property of the *source* that neither `tableswitch` nor
    /// `lookupswitch` encodes: the opcode holds a jump table, and it is the **compiler** that lays
    /// the arms out so that one runs into the next. So this is the one thing in the stage that
    /// tests the emitter and the engine at once, and the one an implementation cannot get right by
    /// accident.
    pub breaks: bool,
}

#[derive(Clone, PartialEq, Eq, Debug)]
pub enum Stmt {
    /// `<ty> <name> = <init>;`
    Declare { name: String, ty: Ty, init: Expr },
    /// `<name> = <expr>;` — `name` must be an assignable local of `expr`'s type.
    Assign { name: String, ty: Ty, expr: Expr },
    If { cond: Cond, then: Block, otherwise: Block },
    /// `switch (selector) { case l: … }`.
    ///
    /// The labels decide which opcode `javac` emits: consecutive ones become a **`tableswitch`**
    /// (a jump table indexed by the value) and scattered ones a **`lookupswitch`** (a sorted search).
    /// Both are inside the JIT's subset, which makes this the first construct of the stage that
    /// *adds* compiled coverage instead of costing it — the exact opposite of the narrowing that
    /// shares its milestone.
    Switch { selector: Expr, arms: Vec<SwitchArm>, default: Option<Block> },
    /// `int g = 0; while (g++ < limit && cond) { … }`.
    ///
    /// The guard is not decoration: it is what lets an **arbitrary** condition into the grammar
    /// without giving up termination. `limit` is a literal and `guard` goes into the body's scope
    /// **not assignable**, the same two rules that make the counted `for` safe.
    While { guard: String, limit: i32, cond: Cond, body: Block },
    /// `break;` — only ever generated inside a loop body.
    Break,
    /// `continue;` — likewise. It re-tests the condition, which is where the guard increments, so
    /// it cannot be used to spin forever.
    Continue,
    /// `throw new X();` — the one way out of a method that is not a `return`.
    Throw(ThrownExc),
    /// `for (int <var> = 0; <var> < <bound>; <var>++)`. `bound` is a literal, never an expression,
    /// and `var` is never assignable inside `body` — the two rules that make termination structural.
    For { var: String, bound: i32, body: Block },
    /// `<elem>[] <name> = new <elem>[<len>];`
    ///
    /// `len` is a **literal**, for the same reason a loop bound is: the JIT's inline allocation
    /// path has a size ceiling and the interpreter has a heap, and a length that came out of the
    /// expression grammar could be `Integer.MAX_VALUE`. It is allowed to be negative — that is a
    /// `NegativeArraySizeException`, i.e. [`marks::NEGATIVE_SIZE`], and one of the two failures an
    /// allocation can have.
    NewArray { name: String, elem: Ty, len: i32 },
    /// `<array>[<index>] = <value>;`
    ///
    /// The one statement in the grammar that can mutate something the *caller* could observe — and
    /// it cannot, because an array is always a fresh local of the method that declared it. That is
    /// load-bearing: FZ-004's warm-up loop calls the entry method 40 times and relies on a
    /// generated method being **pure**, so an array that outlived a call would silently make the
    /// 40 iterations compute 40 different things.
    ArrayStore { array: String, elem: Ty, index: Expr, value: Expr },
    /// `<class>B <name> = new <class><cls>(<arg>);`, or `= null;` when `cls` is `None`.
    ///
    /// The **declared type is always the base**, whatever is constructed. That is what makes the
    /// calls on it `invokevirtual` rather than a statically-bound `invokespecial`: a local declared
    /// as its own exact class gives `javac` enough to bind the call itself, and the inline cache
    /// this stage exists to test would never be consulted.
    ///
    /// `None` is a `null` receiver, and it is a real path rather than a curiosity: `getfield`,
    /// `putfield` and `invokevirtual` on `null` all **deopt** out of compiled code rather than
    /// throwing in it. It is drawn rarely — see [`GenConfig::null_share`] and FZ-005 for what
    /// happens to a campaign when programs die before the JIT has looked at them.
    /// `iface` decides the **declared** type of the new local: the interface or the base class.
    /// The value is the same either way; what changes is which opcode a call on it becomes.
    NewObject { name: String, class: Option<ObjClass>, arg: Expr, iface: bool },
    /// `int t = (o instanceof …S1) ? 1 : 0;` or `int v = (((…S1) o).a);` — the **type test** and
    /// the **cast**, the same question asked the two ways the JVM asks it.
    ///
    /// `instanceof` never throws — `null instanceof X` is `false` by JLS §15.20.2, not a
    /// `NullPointerException` — while a `checkcast` to the wrong class throws
    /// `ClassCastException`, which is the one code in [`marks`] that was written down and
    /// unreachable until this node existed. In compiled code the failing cast is a **deopt** rather
    /// than a throw, so the two arms reach the same answer by different routes.
    ///
    /// **A statement rather than an expression**, and for a reason that is about the emitter and
    /// not about Java: naming the target class needs the program's prefix, which `emit_stmt` has
    /// and `emit_expr` does not — threading it through 33 call sites to save a local would have
    /// been a wide change for no coverage. The result lands in an ordinary local, so the rest of
    /// the grammar reads it with the machinery that already exists.
    TypeProbe { name: String, obj: String, class: ObjClass, cast: Option<Field> },
    /// `o.c = p;` or `o.c = null;` — a **`putfield` of a reference**.
    ///
    /// The only statement in this grammar that builds an edge from one heap object to another, and
    /// the reason it is worth its own knob: an Old object holding a young pointer is exactly what
    /// the GC's **write barrier** and remembered set exist for, and it is the shape of the field in
    /// FZ-002's report. Nothing else the generator emits can construct one.
    ///
    /// `None` writes `null`, which is not a filler case: it is how a chain that read fine on one
    /// iteration reads `null` on the next.
    RefStore { obj: String, value: Option<String> },
    /// **The parallel site.** `K` worker threads, each computing one `int` into **its own** slot of
    /// a shared array, joined, and reduced in a fixed order into [`Fork::acc`].
    ///
    /// The shape is rigid, and it has to be: property 2 says a generated program is deterministic,
    /// and concurrency is the one thing in this grammar that could break it. Three independent
    /// reasons keep it:
    ///
    /// 1. every worker writes **only** its own slot, so there is no write race to have an order;
    /// 2. every `join` happens before **any** read of the array, so nothing is read in flight;
    /// 3. the final reduction walks the array in index order, so **which thread finished first
    ///    cannot be observed**.
    ///
    /// A fourth rule keeps it comparable rather than merely deterministic: each worker catches its
    /// own exceptions into a marker in its slot. An exception escaping a worker thread is legal and
    /// deterministic, but it prints an uncaught report that the two sides classify differently —
    /// the reference JDK reads its own stderr and would answer `Threw` where we answer `Returned`.
    /// That is a divergence manufactured by the harness, not found by it.
    Fork {
        /// The `int[]`, one slot per worker.
        slots: String,
        /// The `…W[]` holding the threads.
        threads: String,
        /// The loop variable. One name for all three loops: each `for` scopes its own.
        counter: String,
        /// Where the joined slots reduce to. An ordinary `int` local afterwards.
        acc: String,
        /// The two `int`s handed to every worker **at construction**, so they are read in the
        /// enclosing scope before any thread starts.
        args: (Expr, Expr),
        /// One body per worker, over `k`, `a`, `b` and what it declares itself — never over the
        /// enclosing method's locals, which the worker object cannot see.
        bodies: Vec<ForkBody>,
    },
    /// `<name> = new <class><cls>(<arg>);` — the same, on a name that already exists.
    ///
    /// Separate from [`Stmt::NewObject`] because only this one can appear **inside a loop over a
    /// name declared outside it**, which is the only way to make a single bytecode call site see
    /// more than one receiver class. Every polymorphic site in a generated program comes from here.
    SetObject { name: String, class: Option<ObjClass>, arg: Expr },
    /// `<obj>.<field> = <value>;` — a `putfield`, of a primitive.
    FieldStore { obj: String, field: Field, value: Expr },
}

impl Stmt {
    /// Whether this statement always leaves its block — `break`, `continue`, `throw`, or an `if`
    /// or `switch` whose every path does.
    ///
    /// Java rejects a statement that cannot be reached (JLS 14.21), so anything after one of these
    /// is a compile error rather than a program. This is a **conservative approximation** of that
    /// rule, not an implementation of it: the real one is famously fiddly, and getting it wrong in
    /// the cautious direction costs a slightly smaller program, while getting it wrong in the other
    /// direction costs an unusable seed. Both are visible; only one is silent.
    fn completes_abruptly(&self) -> bool {
        match self {
            Stmt::Break | Stmt::Continue | Stmt::Throw(_) => true,
            // Both arms, and an `else` that exists: with no `else`, the `if` can always fall
            // through by taking the empty branch.
            Stmt::If { then, otherwise, .. } => {
                !otherwise.is_empty()
                    && then.last().is_some_and(Stmt::completes_abruptly)
                    && otherwise.last().is_some_and(Stmt::completes_abruptly)
            }
            // A `break` inside an arm leaves the **switch**, not the block, so it does not count
            // here; only a `continue` or a `throw` does. And without a `default` the whole
            // statement can be skipped.
            Stmt::Switch { arms, default, .. } => {
                let leaves = |b: &Block| {
                    matches!(b.last(), Some(Stmt::Continue | Stmt::Throw(_)))
                        || b.last().is_some_and(|s| {
                            !matches!(s, Stmt::Break) && s.completes_abruptly()
                        })
                };
                default.as_ref().is_some_and(leaves) && arms.iter().all(|a| leaves(&a.body))
            }
            _ => false,
        }
    }

    pub fn size(&self) -> usize {
        match self {
            Stmt::Declare { init, .. } => 1 + init.size(),
            Stmt::Assign { expr, .. } => 1 + expr.size(),
            Stmt::If { cond, then, otherwise } => 1 + cond.size() + then.size() + otherwise.size(),
            Stmt::Break | Stmt::Continue | Stmt::Throw(_) => 1,
            Stmt::While { cond, body, .. } => 1 + cond.size() + body.size(),
            Stmt::Switch { selector, arms, default } => {
                1 + selector.size()
                    + arms.iter().map(|a| a.body.size()).sum::<usize>()
                    + default.as_ref().map_or(0, Block::size)
            }
            Stmt::For { body, .. } => 1 + body.size(),
            Stmt::NewArray { .. } | Stmt::RefStore { .. } | Stmt::TypeProbe { .. } => 1,
            Stmt::ArrayStore { index, value, .. } => 1 + index.size() + value.size(),
            Stmt::NewObject { arg, .. } | Stmt::SetObject { arg, .. } => 1 + arg.size(),
            Stmt::FieldStore { value, .. } => 1 + value.size(),
            Stmt::Fork { args, bodies, .. } => {
                1 + args.0.size()
                    + args.1.size()
                    + bodies
                        .iter()
                        .map(|w| {
                            w.result.size() + w.block.iter().map(Stmt::size).sum::<usize>()
                        })
                        .sum::<usize>()
            }
        }
    }

    fn classifies(&self, ty: Ty) -> bool {
        match self {
            Stmt::Declare { init, .. } => init.classifies(ty),
            Stmt::Assign { expr, .. } => expr.classifies(ty),
            Stmt::If { cond, then, otherwise } => {
                cond.classifies(ty)
                    || then.iter().any(|s| s.classifies(ty))
                    || otherwise.iter().any(|s| s.classifies(ty))
            }
            Stmt::Break | Stmt::Continue | Stmt::Throw(_) => false,
            Stmt::While { cond, body, .. } => {
                cond.classifies(ty) || body.iter().any(|s| s.classifies(ty))
            }
            Stmt::Switch { selector, arms, default } => {
                selector.classifies(ty)
                    || arms.iter().any(|a| a.body.iter().any(|s| s.classifies(ty)))
                    || default.iter().flatten().any(|s| s.classifies(ty))
            }
            Stmt::For { body, .. } => body.iter().any(|s| s.classifies(ty)),
            Stmt::NewArray { .. } | Stmt::RefStore { .. } | Stmt::TypeProbe { .. } => false,
            Stmt::ArrayStore { index, value, .. } => {
                index.classifies(ty) || value.classifies(ty)
            }
            Stmt::NewObject { arg, .. } | Stmt::SetObject { arg, .. } => arg.classifies(ty),
            Stmt::FieldStore { value, .. } => value.classifies(ty),
            Stmt::Fork { args, bodies, .. } => {
                args.0.classifies(ty)
                    || args.1.classifies(ty)
                    || bodies.iter().any(|w| {
                        w.result.classifies(ty) || w.block.iter().any(|s| s.classifies(ty))
                    })
            }
        }
    }
}

/// A sequence of statements. Not a scope in its own right for the emitter's purposes — Java's
/// block scoping does the right thing for free, and the generator's [`Scope`] mirrors it.
pub type Block = Vec<Stmt>;

trait BlockExt {
    fn size(&self) -> usize;
}

impl BlockExt for Block {
    fn size(&self) -> usize {
        self.iter().map(Stmt::size).sum()
    }
}

/// One `static` method of the generated class.
#[derive(Clone, PartialEq, Eq, Debug)]
pub struct Method {
    pub name: String,
    pub params: Vec<(String, Ty)>,
    pub returns: Ty,
    pub body: Block,
    /// The value returned after `body` runs. Always present, so a method can never fall off its
    /// end — which `javac` rejects, and which would be a generator bug of exactly the kind
    /// property 1 exists to make impossible.
    pub result: Expr,
    /// Upper bound on the statements one call executes, including everything it calls. Computed at
    /// generation time and carried, because recomputing it needs the whole method table.
    pub cost: u64,
}

impl Method {
    pub fn size(&self) -> usize {
        self.body.size() + self.result.size()
    }

    fn classifies(&self, ty: Ty) -> bool {
        self.body.iter().any(|s| s.classifies(ty)) || self.result.classifies(ty)
    }
}

/// A complete generated class: some helper methods, a body, and the total wrapper around it.
#[derive(Clone, PartialEq, Eq, Debug)]
pub struct JavaProgram {
    pub class: String,
    /// Method `k` may only call methods `0..k`. The DAG that makes recursion unrepresentable.
    pub methods: Vec<Method>,
    /// The last method is the one `run()` calls. Held separately from `methods` so the reducer
    /// cannot delete it.
    pub entry: Method,
    /// How many times `run()` calls the entry method before returning. See [`GenConfig::warmup`] —
    /// this is what makes the JIT arm of a campaign actually be the JIT.
    pub warmup: i32,
}

impl JavaProgram {
    /// Whether any [`StrProbe::Same`] survives, i.e. whether `ssame` has to be emitted.
    ///
    /// Emitted on demand for the same reason the classifiers are: a helper nobody calls is a
    /// method the reducer cannot drop and a reader has to rule out.
    fn compares_strings(&self) -> bool {
        fn in_expr(e: &Expr) -> bool {
            match e {
                Expr::Str(StrProbe::Same(_), _) => true,
                Expr::Str(_, _) => false,
                Expr::Neg(a)
                | Expr::Not(a)
                | Expr::Cast(_, a)
                | Expr::Narrow(_, a)
                | Expr::Classify(a)
                | Expr::ArrayLoad(_, _, a) => in_expr(a),
                Expr::Bin(_, a, b) | Expr::Shift(_, a, b) => in_expr(a) || in_expr(b),
                Expr::Ternary(c, a, b) => in_cond(c) || in_expr(a) || in_expr(b),
                Expr::Call(_, args, _) => args.iter().any(in_expr),
                _ => false,
            }
        }
        fn in_cond(c: &Cond) -> bool {
            match c {
                Cond::Cmp(_, a, b) => in_expr(a) || in_expr(b),
                Cond::And(a, b) | Cond::Or(a, b) => in_cond(a) || in_cond(b),
                Cond::Not(a) => in_cond(a),
            }
        }
        fn in_block(b: &Block) -> bool {
            b.iter().any(|s| match s {
                Stmt::Declare { init, .. } => in_expr(init),
                Stmt::Assign { expr, .. } => in_expr(expr),
                Stmt::If { cond, then, otherwise } => {
                    in_cond(cond) || in_block(then) || in_block(otherwise)
                }
                Stmt::While { cond, body, .. } => in_cond(cond) || in_block(body),
                Stmt::For { body, .. } => in_block(body),
                Stmt::Switch { selector, arms, default } => {
                    in_expr(selector)
                        || arms.iter().any(|a| in_block(&a.body))
                        || default.as_ref().is_some_and(|d| in_block(d))
                }
                Stmt::ArrayStore { index, value, .. } => in_expr(index) || in_expr(value),
                Stmt::NewObject { arg, .. } | Stmt::SetObject { arg, .. } => in_expr(arg),
                Stmt::FieldStore { value, .. } => in_expr(value),
                // A worker body cannot contain one — `Scope::foreign` forbids it, because `ssame`
                // is emitted unqualified and does not resolve from another class.
                Stmt::Fork { args, .. } => in_expr(&args.0) || in_expr(&args.1),
                Stmt::NewArray { .. }
                | Stmt::RefStore { .. }
                | Stmt::TypeProbe { .. }
                | Stmt::Break
                | Stmt::Continue
                | Stmt::Throw(_) => false,
            })
        }
        let methods = self.methods.iter().chain(std::iter::once(&self.entry));
        methods.into_iter().any(|m| in_block(&m.body) || in_expr(&m.result))
    }

    /// The bodies of the program's [`Stmt::Fork`], if it has one.
    ///
    /// Found by walking rather than stored, and there is **at most one** — the site is planted
    /// once, like the dispatch probes. That is not a limitation waiting to be lifted: the worker
    /// class carries the bodies in a `switch` over `k`, so two forks would need two classes, and
    /// the reason to want a second one (more shapes) is served by more workers in the one there is.
    fn fork_bodies(&self) -> Option<&[ForkBody]> {
        fn find(block: &Block) -> Option<&[ForkBody]> {
            for stmt in block {
                let found = match stmt {
                    Stmt::Fork { bodies, .. } => return Some(bodies),
                    Stmt::If { then, otherwise, .. } => find(then).or_else(|| find(otherwise)),
                    Stmt::For { body, .. } | Stmt::While { body, .. } => find(body),
                    Stmt::Switch { arms, default, .. } => arms
                        .iter()
                        .find_map(|a| find(&a.body))
                        .or_else(|| default.as_ref().and_then(|b| find(b))),
                    _ => None,
                };
                if found.is_some() {
                    return found;
                }
            }
            None
        }
        find(&self.entry.body).or_else(|| self.methods.iter().find_map(|m| find(&m.body)))
    }

    /// Total AST nodes. The number a reduction test watches go down.
    pub fn size(&self) -> usize {
        self.entry.size() + self.methods.iter().map(Method::size).sum::<usize>()
    }

    /// An upper bound on the statements a run executes. Guaranteed finite by construction; see
    /// property 3 in the module docs.
    pub fn estimated_cost(&self) -> u64 {
        self.entry.cost * self.warmup.max(0) as u64
    }

    /// The cost of **one** call of the entry method — what the generator's budget bounds. The
    /// warm-up multiplies it, deliberately.
    pub fn cost_per_call(&self) -> u64 {
        self.entry.cost
    }

    /// Whether any method classifies a value of `ty`, i.e. whether the emitted class needs that
    /// classifier helper.
    fn classifies(&self, ty: Ty) -> bool {
        self.entry.classifies(ty) || self.methods.iter().any(|m| m.classifies(ty))
    }

    /// What the object hierarchy this program carries has to contain — see [`ObjUse`].
    fn obj_use(&self) -> ObjUse {
        let mut used = ObjUse::default();
        for method in self.methods.iter().chain(std::iter::once(&self.entry)) {
            for stmt in &method.body {
                scan_stmt(stmt, &mut used);
            }
            scan_expr(&method.result, &mut used);
        }
        used
    }
}

/// How much of the hierarchy a program actually touches.
///
/// Derived from the program, never from the configuration, and the difference matters twice over:
///
/// - the reducer can delete the last statement that read a `long` field, and the emitted class
///   should lose the field with it — otherwise a "minimal" case still carries a `long b` nobody
///   reads, and the human reading it has to work out that it is irrelevant;
/// - a `long` field is **outside the JIT's subset**, and it does not merely fail to compile
///   *itself*: `burst::compile` inlines the `invokespecial` of a constructor into its caller, so a
///   constructor that writes a `long` would take every method containing a `new` out of the
///   compiled arm with it. Emitting the field only when something reads it means a program with
///   [`GenConfig::wide_fields`] off is int-only all the way down, hierarchy included.
#[derive(Clone, Copy, Default, PartialEq, Eq, Debug)]
struct ObjUse {
    /// Anything at all — whether the four classes are emitted.
    any: bool,
    /// A `long` field or the `long`-returning virtual, i.e. whether `b` and `w()` exist.
    wide: bool,
    /// Whether the reference field [`REF_FIELD`] is used, and therefore emitted.
    ///
    /// On the same principle as `wide`: a field nobody reads is one the reducer cannot delete and a
    /// human reading a minimal case has to rule out. It costs more than `b` does, besides — a
    /// `putfield` of a reference takes its method out of the JIT's compiled subset entirely.
    refs: bool,
}

/// The **reference** field of the hierarchy: `…B c`.
///
/// A constant rather than a third [`Field`] variant, and that is deliberate: every consumer of
/// `Field` asks it for a [`Ty`], and `Ty` has no reference type — objects live in this grammar
/// without their identity ever being observable, which is what keeps property 2 (determinism) true
/// by absence rather than by rule. A `Field::C` would have made `Field::ty` a lie in one case and
/// the compiler would not have caught it.
const REF_FIELD: &str = "c";

fn scan_stmt(stmt: &Stmt, used: &mut ObjUse) {
    match stmt {
        Stmt::Break | Stmt::Continue | Stmt::Throw(_) => {}
        Stmt::While { cond, body, .. } => {
            scan_cond(cond, used);
            for st in body {
                scan_stmt(st, used);
            }
        }
        Stmt::Switch { selector, arms, default } => {
            scan_expr(selector, used);
            for st in arms.iter().flat_map(|a| a.body.iter()).chain(default.iter().flatten()) {
                scan_stmt(st, used);
            }
        }
        Stmt::Declare { init, .. } => scan_expr(init, used),
        Stmt::Assign { expr, .. } => scan_expr(expr, used),
        Stmt::If { cond, then, otherwise } => {
            scan_cond(cond, used);
            then.iter().chain(otherwise).for_each(|s| scan_stmt(s, used));
        }
        Stmt::For { body, .. } => body.iter().for_each(|s| scan_stmt(s, used)),
        Stmt::NewArray { .. } => {}
        // Names two object locals and writes the reference field, so the hierarchy is used and `c`
        // has to be emitted.
        Stmt::RefStore { .. } => {
            used.any = true;
            used.refs = true;
        }
        Stmt::TypeProbe { class, cast, .. } => {
            used.any = true;
            // Naming a subclass in a cast or a test is a use of the hierarchy even when nothing is
            // constructed, and a `long` field read through the cast pulls the wide half in.
            let _ = class;
            used.wide |= *cast == Some(Field::B);
        }
        Stmt::ArrayStore { index, value, .. } => {
            scan_expr(index, used);
            scan_expr(value, used);
        }
        Stmt::NewObject { arg, .. } | Stmt::SetObject { arg, .. } => {
            used.any = true;
            scan_expr(arg, used);
        }
        Stmt::FieldStore { field, value, .. } => {
            used.any = true;
            used.wide |= *field == Field::B;
            scan_expr(value, used);
        }
        // The bodies **are** scanned, and getting this wrong would be silent: the hierarchy is
        // emitted only when something uses it, so a worker that allocates a `…B` while this arm
        // looked only at the constructor arguments would produce a program naming a class the file
        // does not declare — on the threaded seeds alone.
        Stmt::Fork { args, bodies, .. } => {
            scan_expr(&args.0, used);
            scan_expr(&args.1, used);
            for worker in bodies {
                worker.block.iter().for_each(|s| scan_stmt(s, used));
                scan_expr(&worker.result, used);
            }
        }
    }
}

fn scan_cond(cond: &Cond, used: &mut ObjUse) {
    match cond {
        Cond::Cmp(_, a, b) => {
            scan_expr(a, used);
            scan_expr(b, used);
        }
        Cond::And(a, b) | Cond::Or(a, b) => {
            scan_cond(a, used);
            scan_cond(b, used);
        }
        Cond::Not(a) => scan_cond(a, used),
    }
}

fn scan_expr(expr: &Expr, used: &mut ObjUse) {
    match expr {
        Expr::IntLit(_)
        | Expr::LongLit(_)
        | Expr::FloatLit(_)
        | Expr::DoubleLit(_)
        | Expr::Var(_, _)
        | Expr::ArrayLength(_) => {}
        Expr::Field(_, field) => {
            used.any = true;
            used.wide |= *field == Field::B;
        }
        Expr::ThroughRef(_, field) => {
            used.any = true;
            used.refs = true;
            used.wide |= *field == Field::B;
        }
        Expr::Virtual(_, method) => {
            used.any = true;
            used.wide |= *method == VMethod::W;
        }
        Expr::Neg(a) | Expr::Not(a) | Expr::Cast(_, a) | Expr::Classify(a) => scan_expr(a, used),
        // A string subtree names no local: its leaves are literals from the pool.
        Expr::Str(_, _) => {}
        Expr::Narrow(_, a) => scan_expr(a, used),
        Expr::ArrayLoad(_, _, a) => scan_expr(a, used),
        Expr::Bin(_, a, b) | Expr::Shift(_, a, b) => {
            scan_expr(a, used);
            scan_expr(b, used);
        }
        Expr::Ternary(c, a, b) => {
            scan_cond(c, used);
            scan_expr(a, used);
            scan_expr(b, used);
        }
        Expr::Call(_, args, _) => args.iter().for_each(|a| scan_expr(a, used)),
    }
}

impl Program for JavaProgram {
    fn class_name(&self) -> &str {
        &self.class
    }

    /// A value is a marker when it is one of [`marks`]'s codes. They are deliberately far from
    /// anything arithmetic produces by accident (`0x5AFE_000n`), so this is an exact test and not
    /// a heuristic.
    ///
    /// With the wrapper catching **per iteration**, a marker in the returned value is now the
    /// accumulator of *some* iteration having thrown rather than the whole program having stopped
    /// — so this is checked against the accumulator's own seed value too: a program whose every
    /// iteration threw returns `31·…·MARK`, not `MARK`. What it still catches exactly is the
    /// warm-up of one, which is the shape of the seeds that do no work.
    fn is_marker(&self, value: i32) -> bool {
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

    fn to_java(&self) -> String {
        let mut out = String::new();
        let _ = writeln!(out, "public class {} {{", self.class);
        if self.classifies(Ty::Float) {
            emit_classifier(&mut out, Ty::Float);
        }
        if self.classifies(Ty::Double) {
            emit_classifier(&mut out, Ty::Double);
        }
        if self.compares_strings() {
            emit_same_helper(&mut out);
        }
        for method in &self.methods {
            emit_method(&mut out, method, &self.class);
        }
        emit_method(&mut out, &self.entry, &self.class);
        // The total wrapper (property 4). `Throwable` last, so the specific catches win.
        //
        // The **warm-up loop** inside the `try` is what makes the interpreter/JIT pairing mean
        // anything at all; see [`GenConfig::warmup`] for the measurement that put it here. The
        // accumulator, rather than simply keeping the last result, is so that a wrong answer on
        // *any* iteration reaches the return value: with a deopt in play, the iteration that runs
        // native and the iteration that runs last need not be the same one.
        // **The catch is inside the loop, and that is the difference between a program that runs
        // and one that stops on its first bad iteration.**
        //
        // Wrapped around the whole loop — which is how this was written until 2026-08-29 — one
        // throw on warm-up iteration 1 ends the program, and what comes back is a bare marker. The
        // rest of the iterations never happen, so the JIT never crosses its threshold and the
        // entry method's arithmetic is never exercised. That is FZ-005's shape, and it was
        // measured rather than suspected: with reference fields on, **41 of 80 seeds** died that
        // way. Per iteration, a throw becomes a *value* in the accumulator and the loop keeps
        // going, so a program that throws every time still returns something that depends on every
        // iteration — and one that throws once is barely dented.
        //
        // Property 4 (totality) is unchanged: every path out of the loop body is caught, and the
        // markers are the same ones.
        let _ = writeln!(out, "    static int run() {{");
        let _ = writeln!(out, "        int acc = 0;");
        let _ = writeln!(out, "        for (int w = 0; w < {}; w++) {{", self.warmup);
        let _ = writeln!(out, "            int r;");
        let _ = writeln!(out, "            try {{ r = {}(); }}", self.entry.name);
        let _ = writeln!(
            out,
            "            catch (ArithmeticException e) {{ r = {}; }}",
            marks::ARITHMETIC
        );
        let _ = writeln!(
            out,
            "            catch (ArrayIndexOutOfBoundsException e) {{ r = {}; }}",
            marks::BOUNDS
        );
        let _ = writeln!(
            out,
            "            catch (NegativeArraySizeException e) {{ r = {}; }}",
            marks::NEGATIVE_SIZE
        );
        let _ =
            writeln!(out, "            catch (NullPointerException e) {{ r = {}; }}", marks::NULL);
        // `marks::CLASS_CAST` was written down long before anything could produce it: no node in
        // the grammar could fail a cast. `Stmt::TypeProbe` can, so the code stops being decoration
        // — and without this catch it would arrive as `OTHER`, which is the same answer a bug in
        // any other construct gives.
        let _ = writeln!(
            out,
            "            catch (ClassCastException e) {{ r = {}; }}",
            marks::CLASS_CAST
        );
        let _ = writeln!(out, "            catch (Throwable t) {{ r = {}; }}", marks::OTHER);
        let _ = writeln!(out, "            acc = ((acc * 31) + r);");
        let _ = writeln!(out, "        }}");
        let _ = writeln!(out, "        return acc;");
        let _ = writeln!(out, "    }}");
        // `main` exists because the two sides are invoked differently: `run-headless` calls `run`
        // directly, a real `java` needs an entry point that prints what it got.
        let _ =
            writeln!(out, "    public static void main(String[] a) {{ System.out.println(run()); }}");
        let _ = writeln!(out, "}}");
        // The hierarchy goes **beside** the public class, not inside it. A nested class would be
        // `Fz7$B` on disk, and this file has no reason to find out how well every path that loads a
        // class handles the `$`; several top-level classes in one file are ordinary Java, and
        // `run-headless` puts the class file's own directory on the application class path, so both
        // engines find them the same way.
        let used = self.obj_use();
        if used.any {
            emit_hierarchy(&mut out, &self.class, used.wide, used.refs);
        }
        if let Some(bodies) = self.fork_bodies() {
            emit_worker_class(&mut out, &self.class, bodies);
        }
        out
    }
}

/// The worker class of [`Stmt::Fork`], as Java: a `Thread` subclass whose `run` computes one
/// `int` into its own slot.
///
/// **A named subclass rather than a lambda**, and that is a decision rather than a habit: a lambda
/// compiles to `invokedynamic` plus a bootstrap through `LambdaMetafactory`, which would make every
/// concurrent program depend on that machinery being right. This level is about threads; mixing in
/// the one part of the class file with the most moving parts would make any finding ambiguous
/// between the two.
///
/// **One class per program, with a `switch` over `k`**, because the workers differ only in what
/// they compute. The `default` arm is not decoration: `javac` requires the local to be definitely
/// assigned on every path, and a `switch` over `int` has no exhaustiveness to lean on.
fn emit_worker_class(out: &mut String, prefix: &str, bodies: &[ForkBody]) {
    let _ = writeln!(out, "class {prefix}W extends Thread {{");
    let _ = writeln!(out, "    int[] s;");
    let _ = writeln!(out, "    int k;");
    let _ = writeln!(out, "    int a;");
    let _ = writeln!(out, "    int b;");
    let _ = writeln!(out, "    {prefix}W(int[] s, int k, int a, int b) {{");
    let _ = writeln!(out, "        this.s = s; this.k = k; this.a = a; this.b = b;");
    let _ = writeln!(out, "    }}");
    let _ = writeln!(out, "    public void run() {{");
    let _ = writeln!(out, "        int v;");
    // The per-worker total wrapper. Without it an exception escaping a worker is still
    // deterministic, but it reaches the two sides differently — a real `java` prints its own
    // uncaught report and the runner reads the class off stderr, where ours returns a value — and
    // the campaign would report a divergence it manufactured itself.
    let _ = writeln!(out, "        try {{");
    let _ = writeln!(out, "            switch (k) {{");
    for (i, worker) in bodies.iter().enumerate() {
        // Braces around every arm, always. A `switch` arm is not a scope of its own, so two arms
        // that each declare a local would collide — and the collision would depend on what the
        // generator happened to name things, which is the worst kind of intermittent.
        let _ = writeln!(out, "                case {i}: {{");
        for stmt in &worker.block {
            emit_stmt(out, stmt, 5, prefix);
        }
        let mut e = String::new();
        emit_expr(&mut e, &worker.result);
        let _ = writeln!(out, "                    v = {e};");
        let _ = writeln!(out, "                }} break;");
    }
    let _ = writeln!(out, "                default: v = 0; break;");
    let _ = writeln!(out, "            }}");
    let _ = writeln!(out, "        }} catch (Throwable t) {{ v = {}; }}", marks::OTHER);
    let _ = writeln!(out, "        s[k] = v;");
    let _ = writeln!(out, "    }}");
    let _ = writeln!(out, "}}");
}

/// One worker's body: a block, then the `int` it leaves in its slot.
///
/// **A block and not just an expression**, and the difference is the whole point of the level. An
/// expression of `int` arithmetic allocates nothing, so K threads of it put *no* pressure on the
/// heap — and the bug this level exists to hunt (FZ-002: a stale reference under a collection with
/// real parallelism) needs threads that allocate while other threads hold references. A block
/// brings `new`, arrays, the object hierarchy and loops inside the worker, which is the difference
/// between running K threads and running K threads that make the collector work.
///
/// The hierarchy is reachable from here precisely because it is emitted **beside** the public class
/// rather than nested in it: `…B` is a top-level name, so a worker resolves it the same way the
/// entry method does. What it still cannot reach are the program's own statics, which are emitted
/// unqualified — see [`Scope::foreign`].
#[derive(Clone, PartialEq, Eq, Debug)]
pub struct ForkBody {
    pub block: Block,
    pub result: Expr,
}

/// The four classes of [`ObjClass`], as Java.
///
/// The bodies are constants — the variation this stage cares about is *which class reaches which
/// call site*, not what the callee computes — but the constructor's argument is a generated
/// expression, so the field values are as varied as anything else in the grammar.
///
/// `wide` is [`ObjUse::wide`]: with it off there is no `long` anywhere in the hierarchy, including
/// in the constructor, which is what keeps a `new` from taking its **caller** out of the JIT's
/// subset along with it.
fn emit_hierarchy(out: &mut String, prefix: &str, wide: bool, refs: bool) {
    // The interface exists for one opcode. `invokeinterface` walks the same inline cache as
    // `invokevirtual` but resolves differently: a virtual call takes a slot fixed at the call site
    // and indexes the receiver's vtable, while an interface call has no stable slot — each
    // interface numbers its own methods — so it searches by signature. Declaring only `v()` is
    // deliberate: an interface-typed local can then *only* dispatch, which is exactly the shape
    // that isolates the itable lookup.
    let _ = writeln!(out, "interface {prefix}I {{ int v(); }}");
    let _ = writeln!(out, "class {prefix}B implements {prefix}I {{");
    let _ = writeln!(out, "    int a;");
    if refs {
        let _ = writeln!(out, "    {prefix}B {REF_FIELD};");
    }
    if wide {
        let _ = writeln!(out, "    long b;");
        let _ = writeln!(
            out,
            "    {prefix}B(int s) {{ this.a = s; this.b = (s * 1000003L);{} }}",
            if refs { " this.c = this;" } else { "" }
        );
    } else {
        let _ = writeln!(
            out,
            "    {prefix}B(int s) {{ this.a = s;{} }}",
            if refs { " this.c = this;" } else { "" }
        );
    }
    // `public` because the interface declares it: an interface method is implicitly public, and an
    // implementation may not reduce its visibility (JLS §8.4.8.3). `w()` is not in the interface
    // and stays package-private.
    let _ = writeln!(out, "    public int v() {{ return (a + 1); }}");
    if wide {
        let _ = writeln!(out, "    long w() {{ return (b - 1L); }}");
    }
    let _ = writeln!(out, "}}");

    let _ = writeln!(out, "class {prefix}S0 extends {prefix}B {{");
    let _ = writeln!(out, "    {prefix}S0(int s) {{ super(s); }}");
    let _ = writeln!(out, "    public int v() {{ return (a * 3); }}");
    let _ = writeln!(out, "}}");

    let _ = writeln!(out, "class {prefix}S1 extends {prefix}B {{");
    let _ = writeln!(out, "    {prefix}S1(int s) {{ super(s); }}");
    let _ = writeln!(out, "    public int v() {{ return (a - 7); }}");
    if wide {
        let _ = writeln!(out, "    long w() {{ return (b * 2L); }}");
    }
    let _ = writeln!(out, "}}");

    // Overrides nothing. The one subclass that fails a vtable built by copying only the methods a
    // class *declares*, and passes every test written with the other two.
    let _ = writeln!(out, "class {prefix}S2 extends {prefix}B {{");
    let _ = writeln!(out, "    {prefix}S2(int s) {{ super(s); }}");
    let _ = writeln!(out, "}}");
}

/// The name of the classifier helper for a floating type. Not `m<k>`, so it cannot collide with a
/// generated method or disturb the numbering [`Malformed::MisnamedMethod`] checks.
fn classifier_name(ty: Ty) -> &'static str {
    match ty {
        Ty::Float => "fcls",
        _ => "dcls",
    }
}

/// **The floating-point result channel**, emitted as a Java method.
///
/// # The problem it solves
///
/// The executor can observe exactly one thing: the `int` that `run()` returns (see
/// [`super::exec`]). So a `double` a generated program computed has to become an `int` somehow, and
/// the obvious way — `(int) d` — is *precisely the conversion under test*. JLS §5.1.3 makes it
/// saturating and NaN-aware (NaN → `0`, out of range → `MIN`/`MAX`) where x86's `cvttsd2si` answers
/// the "integer indefinite" value for all three cases, and the JIT refuses any method containing
/// one rather than get it silently wrong. A channel built on it would map `NaN`, `-1.0E300` and
/// `Integer.MIN_VALUE` onto the same `int` and call that agreement.
///
/// # What was looked for first, and is not there
///
/// `Float.floatToRawIntBits` / `Double.doubleToRawLongBits` are the honest answer: they hand back
/// the exact bit pattern, NaN payload included, and nothing about them is under test. **This VM
/// does not have them.** `KajiLibrary/java/lang/Float.java` has no bit accessor at all, and
/// `Double.java` declares `public static native long doubleToLongBits(double)` whose native is
/// **not registered anywhere in `src/jvm/interpreter/natives.rs`**. Worse, the asymmetry is silent
/// in the wrong direction: the reference `javac` compiles the call against the *real* JDK's
/// `java.lang.Double`, so the source is accepted, and only this VM fails at run time — which the
/// total wrapper would swallow into [`marks::OTHER`] and the oracle would report as a divergence
/// on every single seed. So it is not merely unavailable; using it would poison the campaign.
///
/// # The channel that is left: comparisons
///
/// A comparison is not a conversion. `d != d`, `d == 0.0`, `d < c` are IEEE-defined, exact, and
/// produce a `boolean` directly — no bits are reinterpreted and nothing saturates. So the
/// classifier is a ladder of them:
///
/// - **NaN** first, because it is the value every other test lies about: `d != d` is true only for
///   NaN, and after that branch every remaining comparison behaves like a total order;
/// - **both zeros**, separated by `1.0 / d`, which is `+Infinity` for `0.0` and `-Infinity` for
///   `-0.0`. That division is itself one of the things worth testing (`0.0 == -0.0` is true while
///   `1/0.0 != 1/-0.0`), and it lives here on purpose rather than being hidden;
/// - **both infinities**, found by comparing against `MAX_VALUE` — the largest finite value, so
///   `d > MAX_VALUE` is true for `+Infinity` and nothing else;
/// - **a magnitude ladder** ([`DOUBLE_PROBES`]) over everything finite, accumulated three-valued
///   (`<`, `==`, `>`) so that a value landing *exactly* on a probe is distinguished exactly.
///
/// # The limitation, stated plainly
///
/// The ladder is **coarse between its probes**. Two finite results that differ by one ulp in a
/// magnitude no probe separates classify identically, and this channel will call them equal. That
/// is a real blind spot and there is no cheap way to close it without arithmetic on the value —
/// which would put the operators under test back into the channel.
///
/// Three things narrow it, and they are why the trade is acceptable:
///
/// 1. the probes include every saturation boundary and every "round" pool value, and arithmetic on
///    pool constants lands on pool constants far more often than chance suggests;
/// 2. the grammar *also* generates `(int)`/`(long)` of a floating expression as an ordinary
///    integral subexpression, which reaches the return value through the exact `int` channel and
///    resolves the integer part of a result to the last bit;
/// 3. floating comparisons appear in `if` and `?:` conditions on their own, where the pool's
///    constants are the probes and `fcmpl`/`fcmpg` is what is really being asked.
/// `ssame` — reference equality of two `String`s, behind a call.
///
/// The call is the whole point. See [`emit_str_probe`]: `javac` constant-folds a `==` between two
/// literals, so the only way to ask the **VM** whether it interns them is to hand both sides over
/// as parameters, where the folder cannot reach.
fn emit_same_helper(out: &mut String) {
    let _ = writeln!(out, "    static int ssame(String p, String q) {{ return (p == q) ? 1 : 0; }}");
}

fn emit_classifier(out: &mut String, ty: Ty) {
    let name = classifier_name(ty);
    let kw = ty.keyword();
    let lit = |out: &mut String, index: usize| match ty {
        Ty::Float => emit_float_lit(out, FLOAT_PROBES[index]),
        _ => emit_double_lit(out, DOUBLE_PROBES[index]),
    };
    let (probes, one, zero, max) = match ty {
        Ty::Float => (FLOAT_PROBES.len(), "1.0f", "0.0f", 0x7F7F_FFFFu64),
        _ => (DOUBLE_PROBES.len(), "1.0", "0.0", 0x7FEF_FFFF_FFFF_FFFF),
    };
    let _ = writeln!(out, "    static int {name}({kw} d) {{");
    let _ = writeln!(out, "        if (d != d) {{ return 1; }}");
    let _ = writeln!(
        out,
        "        if (d == {zero}) {{ if (({one} / d) < {zero}) {{ return 3; }} return 2; }}"
    );
    out.push_str("        if (d > ");
    match ty {
        Ty::Float => emit_float_lit(out, max as u32),
        _ => emit_double_lit(out, max),
    }
    let _ = writeln!(out, ") {{ return 4; }}");
    out.push_str("        if (d < ");
    match ty {
        Ty::Float => emit_float_lit(out, (max as u32) | 0x8000_0000),
        _ => emit_double_lit(out, max | 0x8000_0000_0000_0000),
    }
    let _ = writeln!(out, ") {{ return 5; }}");
    let _ = writeln!(out, "        int h = 6;");
    for index in 0..probes {
        out.push_str("        h = ((h * 3) + ((d < ");
        lit(out, index);
        out.push_str(") ? 0 : ((d == ");
        lit(out, index);
        let _ = writeln!(out, ") ? 1 : 2)));");
    }
    let _ = writeln!(out, "        return h;");
    let _ = writeln!(out, "    }}");
}

fn emit_method(out: &mut String, method: &Method, prefix: &str) {
    let params = method
        .params
        .iter()
        .map(|(name, ty)| format!("{} {}", ty.keyword(), name))
        .collect::<Vec<_>>()
        .join(", ");
    let _ = writeln!(out, "    static {} {}({}) {{", method.returns.keyword(), method.name, params);
    for stmt in &method.body {
        emit_stmt(out, stmt, 2, prefix);
    }
    let mut result = String::new();
    emit_expr(&mut result, &method.result);
    let _ = writeln!(out, "        return {result};");
    let _ = writeln!(out, "    }}");
}

fn indent(out: &mut String, depth: usize) {
    for _ in 0..depth {
        out.push_str("    ");
    }
}

fn emit_stmt(out: &mut String, stmt: &Stmt, depth: usize, prefix: &str) {
    indent(out, depth);
    match stmt {
        Stmt::Fork { slots, threads, counter, acc, args, bodies } => {
            let k = bodies.len();
            let (mut a, mut b) = (String::new(), String::new());
            emit_expr(&mut a, &args.0);
            emit_expr(&mut b, &args.1);
            // The arguments are evaluated **here**, in the enclosing scope, and handed over at
            // construction: nothing a worker runs can read a local that is still changing.
            let _ = writeln!(out, "int[] {slots} = new int[{k}];");
            indent(out, depth);
            let _ = writeln!(out, "{prefix}W[] {threads} = new {prefix}W[{k}];");
            indent(out, depth);
            let _ = writeln!(
                out,
                "for (int {counter} = 0; {counter} < {k}; {counter}++) {{ \
                 {threads}[{counter}] = new {prefix}W({slots}, {counter}, {a}, {b}); \
                 {threads}[{counter}].start(); }}"
            );
            indent(out, depth);
            // Every join before any read. `InterruptedException` is caught and ignored rather than
            // declared: nothing here interrupts, and a `throws` would have to be threaded through
            // the entry method and the wrapper for a case that cannot arise.
            let _ = writeln!(
                out,
                "try {{ for (int {counter} = 0; {counter} < {k}; {counter}++) {{ \
                 {threads}[{counter}].join(); }} }} catch (InterruptedException e) {{ }}"
            );
            indent(out, depth);
            let _ = writeln!(out, "int {acc} = 0;");
            indent(out, depth);
            // Index order, so which thread finished first is not observable. `31 *` and not `+`:
            // addition is commutative, and two slots that swapped values would sum the same — the
            // reduction has to be able to *tell*, or it is not evidence of anything.
            let _ = writeln!(
                out,
                "for (int {counter} = 0; {counter} < {k}; {counter}++) {{ \
                 {acc} = (({acc} * 31) + {slots}[{counter}]); }}"
            );
        }
        Stmt::Declare { name, ty, init } => {
            let mut e = String::new();
            emit_expr(&mut e, init);
            let _ = writeln!(out, "{} {name} = {e};", ty.keyword());
        }
        Stmt::Assign { name, expr, .. } => {
            let mut e = String::new();
            emit_expr(&mut e, expr);
            let _ = writeln!(out, "{name} = {e};");
        }
        Stmt::If { cond, then, otherwise } => {
            let mut c = String::new();
            emit_cond(&mut c, cond);
            let _ = writeln!(out, "if ({c}) {{");
            for s in then {
                emit_stmt(out, s, depth + 1, prefix);
            }
            indent(out, depth);
            if otherwise.is_empty() {
                let _ = writeln!(out, "}}");
            } else {
                let _ = writeln!(out, "}} else {{");
                for s in otherwise {
                    emit_stmt(out, s, depth + 1, prefix);
                }
                indent(out, depth);
                let _ = writeln!(out, "}}");
            }
        }
        Stmt::Break => out.push_str("break;\n"),
        Stmt::Continue => out.push_str("continue;\n"),
        Stmt::Throw(exc) => {
            let _ = writeln!(out, "throw new {}();", exc.class());
        }
        Stmt::While { guard, limit, cond, body } => {
            let mut c = String::new();
            emit_cond(&mut c, cond);
            let _ = writeln!(out, "int {guard} = 0;");
            indent(out, depth);
            // The guard first, so short-circuiting cannot skip the increment.
            let _ = writeln!(out, "while ({guard}++ < {limit} && ({c})) {{");
            for st in body {
                emit_stmt(out, st, depth + 1, prefix);
            }
            indent(out, depth);
            let _ = writeln!(out, "}}");
        }
        Stmt::Switch { selector, arms, default } => {
            let mut sel = String::new();
            emit_expr(&mut sel, selector);
            let _ = writeln!(out, "switch ({sel}) {{");
            for arm in arms {
                indent(out, depth);
                let _ = writeln!(out, "case {}: {{", arm.label);
                for st in &arm.body {
                    emit_stmt(out, st, depth + 1, prefix);
                }
                indent(out, depth);
                out.push_str("}\n");
                if arm.breaks {
                    indent(out, depth);
                    out.push_str("break;\n");
                }
            }
            if let Some(body) = default {
                indent(out, depth);
                let _ = writeln!(out, "default: {{");
                for st in body {
                    emit_stmt(out, st, depth + 1, prefix);
                }
                indent(out, depth);
                out.push_str("}\n");
            }
            indent(out, depth);
            let _ = writeln!(out, "}}");
        }
        Stmt::For { var, bound, body } => {
            let _ = writeln!(out, "for (int {var} = 0; {var} < {bound}; {var}++) {{");
            for s in body {
                emit_stmt(out, s, depth + 1, prefix);
            }
            indent(out, depth);
            let _ = writeln!(out, "}}");
        }
        Stmt::NewArray { name, elem, len } => {
            let kw = elem.keyword();
            let _ = writeln!(out, "{kw}[] {name} = new {kw}[{len}];");
        }
        Stmt::ArrayStore { array, index, value, .. } => {
            let mut i = String::new();
            emit_expr(&mut i, index);
            let mut v = String::new();
            emit_expr(&mut v, value);
            let _ = writeln!(out, "{array}[{i}] = {v};");
        }
        // Declared at the **base** type whatever is constructed — see [`Stmt::NewObject`]. The
        // argument of a `null` is not emitted, and the generator makes it a literal zero so that
        // nothing the reducer could shrink is invisible in the source.
        Stmt::NewObject { name, class, arg, iface } => {
            let declared = if *iface { "I" } else { "B" };
            match class {
                Some(cls) => {
                    let mut e = String::new();
                    emit_expr(&mut e, arg);
                    let _ = writeln!(
                        out,
                        "{prefix}{declared} {name} = new {prefix}{}({e});",
                        cls.suffix()
                    );
                }
                None => {
                    let _ = writeln!(out, "{prefix}{declared} {name} = null;");
                }
            }
        }
        Stmt::SetObject { name, class, arg } => match class {
            Some(cls) => {
                let mut e = String::new();
                emit_expr(&mut e, arg);
                let _ = writeln!(out, "{name} = new {prefix}{}({e});", cls.suffix());
            }
            None => {
                let _ = writeln!(out, "{name} = null;");
            }
        },
        Stmt::FieldStore { obj, field, value } => {
            let mut v = String::new();
            emit_expr(&mut v, value);
            let _ = writeln!(out, "{obj}.{} = {v};", field.name());
        }
        Stmt::TypeProbe { name, obj, class, cast } => {
            let target = format!("{prefix}{}", class.suffix());
            let _ = match cast {
                // The cast, which can fail: `ClassCastException` when `obj` is not a `target`.
                Some(field) => {
                    writeln!(out, "int {name} = ((({target}) {obj}).{});", field.name())
                }
                // The test, which cannot: `false` covers both "wrong class" and `null`.
                None => writeln!(out, "int {name} = ({obj} instanceof {target}) ? 1 : 0;"),
            };
        }
        Stmt::RefStore { obj, value } => {
            let _ = match value {
                Some(name) => writeln!(out, "{obj}.{REF_FIELD} = {name};"),
                None => writeln!(out, "{obj}.{REF_FIELD} = null;"),
            };
        }
    }
}

/// Emits an `int` literal.
///
/// `Integer.MIN_VALUE` is written by name rather than as `-2147483648`, and that is not decoration:
/// the JLS lets the decimal `2147483648` appear *only* as the direct operand of unary minus, so the
/// moment an emitter wraps a subexpression in parentheses — which this one does everywhere, to keep
/// precedence out of the picture — `(-2147483648)` is fine but `-(2147483648)` is a compile error.
/// Naming the constant sidesteps the whole question. `javac` folds it to an `ldc`, so nothing about
/// the class file depends on `java.lang.Integer` being loadable.
fn emit_int_lit(out: &mut String, value: i32) {
    match value {
        i32::MIN => out.push_str("Integer.MIN_VALUE"),
        i32::MAX => out.push_str("Integer.MAX_VALUE"),
        v => {
            let _ = write!(out, "{v}");
        }
    }
}

fn emit_long_lit(out: &mut String, value: i64) {
    match value {
        i64::MIN => out.push_str("Long.MIN_VALUE"),
        i64::MAX => out.push_str("Long.MAX_VALUE"),
        v => {
            let _ = write!(out, "{v}L");
        }
    }
}

/// Emits a `float` literal from its bit pattern, so that what the program computes is exactly what
/// the pool chose.
///
/// # Why not `Float.NaN` and `Float.POSITIVE_INFINITY`
///
/// Because `KajiLibrary/java/lang/Float.java` does not declare them, and the reference `javac`
/// compiles against the *real* JDK — so the source would be accepted and this VM would then fail to
/// resolve the field at run time, on every seed. `0.0f / 0.0f` and `1.0f / 0.0f` are compile-time
/// constant expressions (JLS §15.28), which `javac` folds into an `ldc` of exactly the value
/// wanted, so the class file is identical to the one a named constant would have produced and
/// nothing depends on `java.lang.Float` being loadable at all. The same argument the `int` pool
/// already makes for writing `Integer.MIN_VALUE` by name, run in the opposite direction.
///
/// # Why the decimal form round-trips
///
/// Rust's `{:?}` for `f32`/`f64` prints the shortest decimal that reads back as the same value, and
/// Java's literal grammar accepts every form it produces (`1e30`, `-0.0`, `2147483600.0`). A
/// `float` literal is never "too small" or "too large" for `float` here, because it came *from* a
/// `float`.
fn emit_float_lit(out: &mut String, bits: u32) {
    let value = f32::from_bits(bits);
    if value.is_nan() {
        out.push_str("(0.0f / 0.0f)");
    } else if value == f32::INFINITY {
        out.push_str("(1.0f / 0.0f)");
    } else if value == f32::NEG_INFINITY {
        out.push_str("(-1.0f / 0.0f)");
    } else {
        let _ = write!(out, "{value:?}f");
    }
}

/// The same for `double`. See [`emit_float_lit`] for the argument.
fn emit_double_lit(out: &mut String, bits: u64) {
    let value = f64::from_bits(bits);
    if value.is_nan() {
        out.push_str("(0.0 / 0.0)");
    } else if value == f64::INFINITY {
        out.push_str("(1.0 / 0.0)");
    } else if value == f64::NEG_INFINITY {
        out.push_str("(-1.0 / 0.0)");
    } else {
        let _ = write!(out, "{value:?}");
    }
}

/// Every compound expression is parenthesised. Verbose, and worth it: precedence is the one thing
/// an emitter gets subtly wrong, and a precedence bug does not show up as a compile error — it
/// shows up as a program that computes something other than what the AST says, which would make
/// every finding suspect.
/// A string question, emitted as the `int` it answers. The ternaries are how a `boolean` becomes
/// a value at all: the grammar has no `boolean` locals, the same reason [`Expr::Ternary`] exists.
fn emit_str_probe(out: &mut String, probe: &StrProbe, value: &StrExpr) {
    match probe {
        StrProbe::Length => {
            emit_str(out, value);
            out.push_str(".length()");
        }
        // The whole ternary is parenthesised, not just its condition. Without the outer pair,
        // `x * s.equals(t) ? 1 : 0` parses as `(x * s.equals(t)) ? 1 : 0` — a multiplication of an
        // int by a boolean — and `javac` rejects the program. Every node in this grammar
        // parenthesises itself for exactly this reason.
        StrProbe::Identity(other) => {
            out.push('(');
            emit_str(out, value);
            out.push_str(".equals(");
            emit_str(out, other);
            out.push_str(") ? 1 : 0)");
        }
        // **Through a helper, not inline.** Written as `("a" == "a")` the comparison never
        // reaches the VM: `javac` folds it to `true` at compile time and emits no `ldc` at all
        // (checked with `javap`: the whole `if` becomes an `iinc`). The probe that documents
        // itself as the one earning this stage was measuring the **compiler's constant folder**,
        // and the campaign reported agreement over a check the VM never ran. Passing both sides as
        // **parameters** is what the folder cannot see through.
        StrProbe::Same(other) => {
            out.push_str("ssame(");
            emit_str(out, value);
            out.push_str(", ");
            emit_str(out, other);
            out.push(')');
        }
    }
}

/// A `String`-valued expression. Every form is parenthesised: `a + b` inside a `.length()` would
/// otherwise bind the call to `b` alone.
fn emit_str(out: &mut String, value: &StrExpr) {
    match value {
        StrExpr::Lit(i) => {
            let _ = write!(out, "\"{}\"", STRING_POOL[*i % STRING_POOL.len()]);
        }
        StrExpr::Concat(a, b) => {
            out.push('(');
            emit_str(out, a);
            out.push_str(" + ");
            emit_str(out, b);
            out.push(')');
        }
        StrExpr::Fresh(a) => {
            out.push_str("new String(");
            emit_str(out, a);
            out.push(')');
        }
    }
}

fn emit_expr(out: &mut String, expr: &Expr) {
    match expr {
        Expr::IntLit(v) => emit_int_lit(out, *v),
        Expr::LongLit(v) => emit_long_lit(out, *v),
        Expr::FloatLit(bits) => emit_float_lit(out, *bits),
        Expr::DoubleLit(bits) => emit_double_lit(out, *bits),
        Expr::Var(name, _) => out.push_str(name),
        // The space after the operator is load-bearing. `Neg(IntLit(-31))` without it emits
        // `(--31)`, which Java parses as a pre-decrement of a literal and rejects — a compile
        // error that only appears once a negative constant is drawn from the pool, i.e. constantly.
        Expr::Neg(inner) => {
            out.push_str("(- ");
            emit_expr(out, inner);
            out.push(')');
        }
        Expr::Not(inner) => {
            out.push_str("(~ ");
            emit_expr(out, inner);
            out.push(')');
        }
        Expr::Bin(op, a, b) => {
            out.push('(');
            emit_expr(out, a);
            let _ = write!(out, " {} ", op.symbol());
            emit_expr(out, b);
            out.push(')');
        }
        Expr::Shift(op, value, amount) => {
            out.push('(');
            emit_expr(out, value);
            let _ = write!(out, " {} ", op.symbol());
            emit_expr(out, amount);
            out.push(')');
        }
        Expr::Cast(to, inner) => {
            let _ = write!(out, "(({})", to.keyword());
            emit_expr(out, inner);
            out.push(')');
        }
        Expr::Ternary(cond, then, otherwise) => {
            out.push('(');
            emit_cond(out, cond);
            out.push_str(" ? ");
            emit_expr(out, then);
            out.push_str(" : ");
            emit_expr(out, otherwise);
            out.push(')');
        }
        Expr::Call(index, args, _) => {
            let _ = write!(out, "m{index}(");
            for (i, arg) in args.iter().enumerate() {
                if i > 0 {
                    out.push_str(", ");
                }
                emit_expr(out, arg);
            }
            out.push(')');
        }
        Expr::Classify(inner) => {
            let _ = write!(out, "{}(", classifier_name(inner.ty()));
            emit_expr(out, inner);
            out.push(')');
        }
        Expr::Str(probe, value) => emit_str_probe(out, probe, value),
        // Two pairs of parentheses, both load-bearing: the inner one so the cast takes the whole
        // operand rather than binding tighter than the expression's own operator, the outer one so
        // the promoted result sits where any other `int` would.
        Expr::Narrow(to, inner) => {
            let _ = write!(out, "(({}) ", to.keyword());
            emit_expr(out, inner);
            out.push(')');
        }
        // No parentheses of their own: `a[i]` and `a.length` are *primary* expressions, the same
        // grammatical class as a variable name, so there is no precedence to get wrong. The
        // subscript is a fresh bracket pair, which is a parenthesis by another name.
        Expr::ArrayLoad(name, _, index) => {
            let _ = write!(out, "{name}[");
            emit_expr(out, index);
            out.push(']');
        }
        Expr::ArrayLength(name) => {
            let _ = write!(out, "{name}.length");
        }
        // Primary expressions too, for the same reason `a.length` is: a field access and a method
        // invocation bind tighter than every operator, so there is no precedence to protect.
        Expr::Field(name, field) => {
            let _ = write!(out, "{name}.{}", field.name());
        }
        Expr::ThroughRef(name, field) => {
            let _ = write!(out, "{name}.{REF_FIELD}.{}", field.name());
        }
        Expr::Virtual(name, method) => {
            let _ = write!(out, "{name}.{}()", method.name());
        }
    }
}

fn emit_cond(out: &mut String, cond: &Cond) {
    match cond {
        Cond::Cmp(op, a, b) => {
            out.push('(');
            emit_expr(out, a);
            let _ = write!(out, " {} ", op.symbol());
            emit_expr(out, b);
            out.push(')');
        }
        Cond::And(a, b) => {
            out.push('(');
            emit_cond(out, a);
            out.push_str(" && ");
            emit_cond(out, b);
            out.push(')');
        }
        Cond::Or(a, b) => {
            out.push('(');
            emit_cond(out, a);
            out.push_str(" || ");
            emit_cond(out, b);
            out.push(')');
        }
        Cond::Not(a) => {
            out.push_str("(!");
            emit_cond(out, a);
            out.push(')');
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Generation
// ---------------------------------------------------------------------------------------------

/// One name in scope.
#[derive(Clone, Debug)]
struct Local {
    name: String,
    ty: Ty,
    /// `false` for a `for` counter. The single most important flag in this file: a counter that can
    /// be assigned is a program that can loop forever, and no amount of care elsewhere recovers it.
    assignable: bool,
    /// `Some(element type)` when this name is an **array**, `None` when it is a scalar.
    ///
    /// One list rather than two, so that every existing lookup — the checker's `scope`, the
    /// shadowing rule, the block cloning — keeps working unchanged. The flag is what keeps the two
    /// namespaces apart: an array must never be offered where a scalar of its element type is
    /// wanted, because `int[] + 1` is not a program.
    array_of: Option<Ty>,
    /// The array's declared length, carried so that [`Gen::index_expr`] can generate an index that
    /// is actually **in range for this array**.
    ///
    /// It is not decoration, and FZ-005 is the reason it exists. The first version generated an
    /// index in `0..max_array_len` without knowing which array it was for, so a "small" index into
    /// a length-2 array was out of range about as often as not — and a program that throws on its
    /// **first** warm-up iteration never reaches `JitCache::THRESHOLD`, so the JIT never sees it at
    /// all. Measured: 46% of seeds died that way and JIT coverage halved.
    array_len: i32,
    /// `true` when the local's **declared** type is the interface rather than the base class.
    ///
    /// The value is the same object either way; what changes is what the *call site* can do with
    /// it. Through the interface only `v()` is reachable — no field, and no `w()`, because an
    /// interface declares neither — and that restriction is enforced by keeping these names out of
    /// [`Scope::objects`] entirely, so every existing use stays correct without a guard.
    object_iface: bool,
    /// `true` when this name is an **object** — static type `<class>B`, whatever class the value
    /// currently is.
    ///
    /// A third flag on the same flat list, for the reason `array_of` is one: every existing lookup
    /// (the checker's scope, the shadowing rule, the block cloning) keeps working unchanged. What
    /// it must do is keep the namespaces apart in *both* directions — an object offered where a
    /// scalar is wanted emits `o0 + 1`, and a scalar offered as a receiver emits `v3.v()`. Both are
    /// `javac` rejections, i.e. generator bugs, so [`Scope::readable`] and [`Scope::assignable`]
    /// filter on it and [`Scope::objects`] is the only way in.
    object: bool,
}

impl Local {
    fn scalar(name: String, ty: Ty) -> Local {
        Local {
            name,
            ty,
            assignable: true,
            array_of: None,
            array_len: 0,
            object: false,
            object_iface: false,
        }
    }

    /// A worker's field: readable, **never assignable**.
    ///
    /// This is load-bearing, and the campaign proved it within minutes of the bodies becoming
    /// blocks. `k` is the worker's slot index, and the last thing the worker does is `s[k] = v`.
    /// Let a body assign to `k` and two of the three reasons [`Stmt::Fork`] is deterministic stop
    /// being true at once: two workers can land on the same slot (a write race with an order), and
    /// an index outside the array throws **out of the try**, escaping the thread — which the
    /// reference JDK reports as an uncaught exception and this VM does not, a divergence the
    /// harness manufactured rather than found.
    ///
    /// `a` and `b` are frozen for a smaller reason of the same kind: they are the only channel from
    /// the enclosing scope, and a body that rewrote them would be describing a different program
    /// than the one the constructor arguments say it is.
    fn worker_field(name: &str) -> Local {
        Local { assignable: false, ..Local::scalar(name.to_string(), Ty::Int) }
    }

    /// An object local. `ty` is a placeholder that nothing may read: every consumer of a scalar
    /// type filters `object` out first, and [`Scope::objects`] hands back names only.
    fn object(name: String) -> Local {
        Local {
            name,
            ty: Ty::Int,
            assignable: true,
            array_of: None,
            array_len: 0,
            object: true,
            object_iface: false,
        }
    }

    /// An object local **declared as the interface**. Same value, narrower call site: only `v()`.
    fn object_via_interface(name: String) -> Local {
        Local { object_iface: true, ..Local::object(name) }
    }
}

/// The type context generation runs in.
#[derive(Clone, Debug, Default)]
pub struct Scope {
    locals: Vec<Local>,
    /// `true` when this scope lives inside **another class** — today, a [`Stmt::Fork`] worker body.
    ///
    /// What it forbids is everything the program emits **unqualified**: a call to one of its static
    /// helpers ([`Expr::Call`]) and a call to a classifier ([`Expr::Classify`], emitted as
    /// `fcls`/`dcls`). Both are perfectly good expressions in the class that declares them and
    /// neither resolves from outside it, so this is a `javac` error rather than a wrong answer —
    /// and one that would only ever appear on the seeds that generate threads, which is the worst
    /// kind of intermittent.
    ///
    /// A property of the *scope* and not a flag on the generator, so it cannot be left switched on
    /// after the worker bodies are done.
    foreign: bool,
}

impl Scope {
    fn readable(&self, ty: Ty) -> Vec<&str> {
        self.locals
            .iter()
            .filter(|l| l.ty == ty && l.array_of.is_none() && !l.object)
            .map(|l| l.name.as_str())
            .collect()
    }

    fn assignable(&self, ty: Ty) -> Vec<&str> {
        self.locals
            .iter()
            .filter(|l| l.ty == ty && l.assignable && l.array_of.is_none() && !l.object)
            .map(|l| l.name.as_str())
            .collect()
    }

    /// The arrays in scope, as `(name, element type)`. An array is never assignable as a whole:
    /// there is no `a = b` between arrays in this grammar, so two names can never alias, and
    /// "which array did that store land in" is answered by reading the source.
    fn arrays(&self) -> Vec<(&str, Ty, i32)> {
        self.locals
            .iter()
            .filter_map(|l| l.array_of.map(|elem| (l.name.as_str(), elem, l.array_len)))
            .collect()
    }

    /// The object locals in scope, by name. Their static type is always the base class, so there
    /// is nothing else to carry.
    fn objects(&self) -> Vec<&str> {
        // Interface-typed names are **not** here, and that is the whole enforcement mechanism: an
        // interface declares no field and no `w()`, so every consumer of this list — field reads,
        // field stores, the wide call — stays correct without learning about the distinction.
        self.locals
            .iter()
            .filter(|l| l.object && !l.object_iface)
            .map(|l| l.name.as_str())
            .collect()
    }

    /// The object names whose **declared** type is the interface. Only one thing may be done with
    /// them, and it is the point: call `v()`, which compiles to `invokeinterface`.
    fn iface_objects(&self) -> Vec<&str> {
        self.locals
            .iter()
            .filter(|l| l.object_iface)
            .map(|l| l.name.as_str())
            .collect()
    }

    /// The arrays whose elements are of `elem` — for the places that need a value of a known type
    /// out of one.
    fn arrays_of(&self, elem: Ty) -> Vec<(&str, i32)> {
        self.locals
            .iter()
            .filter(|l| l.array_of == Some(elem))
            .map(|l| (l.name.as_str(), l.array_len))
            .collect()
    }
}

/// The knobs. Every one of them is a bound on something, because a generator without bounds is a
/// generator that eventually emits a program nobody can run.
#[derive(Clone, Copy, Debug)]
pub struct GenConfig {
    /// Helper methods, on top of the entry method.
    pub max_methods: usize,
    /// Parameters each helper may take.
    pub max_params: usize,
    /// Statements in one block, before nesting.
    pub max_stmts: usize,
    /// How deeply expressions nest. The cost of this one is exponential in the node count.
    pub max_expr_depth: u32,
    /// How deeply blocks nest (`if` and `for` together).
    pub max_block_depth: u32,
    /// The largest literal a `for` bound may take.
    pub max_loop_bound: i32,
    /// The execution-cost ceiling for the entry method; see property 3. The *whole program* costs
    /// this times [`GenConfig::warmup`].
    pub budget: u64,
    /// Out of 100, how often a type drawn by [`Gen::any_ty`] is `float` or `double` (split evenly
    /// between the two).
    ///
    /// A knob rather than a constant because floating point changes what the *pairings* mean, not
    /// just what the programs look like — see [`GenConfig::fp_narrowing`]. Zero reproduces the
    /// integral-only grammar exactly, which is what makes "did floats cost us JIT coverage?" a
    /// question with a measurable answer.
    pub fp_share: u32,
    /// Whether `(int)` / `(long)` of a floating expression may be generated.
    ///
    /// # Why this is a knob and not simply on
    ///
    /// `f2i`, `f2l`, `d2i` and `d2l` are **deliberately outside the JIT's subset**
    /// (`burst::compile`: JLS §5.1.3 makes them saturating and NaN-aware where x86's `cvtt*2si`
    /// answers the integer-indefinite value for NaN *and* both overflows, so each needs a
    /// compare-and-branch sequence nobody has written yet). The JIT does not compile them wrong —
    /// it **refuses the whole method**.
    ///
    /// So a narrowing conversion has opposite effects on the two pairings that matter:
    ///
    /// - against [`Path::ReferenceJdk`](super::Path::ReferenceJdk) it is the single most valuable
    ///   construct in this stage. The interpreter's `conversion_operations::f2i` is the only
    ///   implementation that ever runs, and nothing has checked it against a real JDK;
    /// - against [`Path::Jit`](super::Path::Jit) it is *poison*: every method carrying one becomes
    ///   `Ineligible`, both arms run the interpreter, and the campaign quietly turns back into
    ///   FZ-004.
    ///
    /// Hence the knob, and hence `fuzz::campaigns::jit_coverage` measuring both settings rather
    /// than assuming either.
    pub fp_narrowing: bool,
    /// Out of 100, how often a new object local is declared as the **interface** instead of the
    /// base class.
    ///
    /// The one construct that reaches `invokeinterface`, which resolves by **itable** — a search by
    /// signature — where `invokevirtual` indexes a vtable slot fixed at the call site. Same inline
    /// cache, genuinely different lookup.
    pub interface_share: u32,
    /// Out of 100, how often a loop is a `while` instead of a counted `for`.
    ///
    /// Safe to leave on: the guard counter keeps the structural termination argument intact (see
    /// the module header), so a `while` costs a bounded number of trips exactly like a `for`. What
    /// it buys is a loop whose condition is an **arbitrary expression** re-evaluated every trip,
    /// which a counted `for` cannot express.
    pub while_share: u32,
    /// Out of 100, how often a statement *inside a loop* is a `break` or a `continue`.
    ///
    /// Both only ever cut iterations short, so neither can defeat the guard — and `continue` in
    /// particular re-tests the condition, which is where the guard increments.
    pub jump_share: u32,
    /// Out of 100, how often a statement is an explicit `throw`.
    ///
    /// Zero by default, and the reason is measurable rather than cautious: `athrow` is in
    /// `burst::compile`, but as a **deopt** — the compiled code jumps out to the interpreter when
    /// it fires. So a program that throws on most runs is a program whose interesting half is
    /// interpreted on both sides of the JIT pairing, which is FZ-004's shape. Against the reference
    /// JDK it costs nothing and exercises the handler search.
    pub throw_share: u32,
    /// Out of 100, how often a statement is a `switch`.
    ///
    /// Unlike the other two knobs of this milestone, turning this one up **helps** the JIT pairing:
    /// `tableswitch` and `lookupswitch` are both inside `burst::compile`'s subset, padding included.
    /// It is the construct that pays for the narrowing's cost.
    pub switch_share: u32,
    /// Out of 100, how often an `int` expression is a **narrowing round trip** (`i2b`/`i2s`/`i2c`).
    ///
    /// Its own knob, separate from anything else in this stage, because it pulls the two pairings
    /// in **opposite** directions — the same tension [`GenConfig::fp_narrowing`] documents:
    ///
    /// - against [`Path::ReferenceJdk`](super::Path::ReferenceJdk) it is the point. Truncation and
    ///   the sign/zero-extension split (`(char) -1` is 65535, `(byte) -1` is -1) are implemented
    ///   only in `conversion_operations`, and nothing has ever diffed them against a real JDK;
    /// - against [`Path::Jit`](super::Path::Jit) it is **poison**. `0x91`–`0x93` do not appear in
    ///   `burst::compile`'s opcode scan at all, so a method carrying one is refused whole — the
    ///   compiled arm never sees it, and the campaign quietly measures the interpreter twice.
    pub narrowing_share: u32,
    /// Out of 100, how often an `int` expression is a **string probe** instead of arithmetic.
    ///
    /// A knob rather than a constant, and defaulting to **zero**, for the same reason
    /// [`GenConfig::fp_share`] is one: strings change what the *pairings* mean. Against
    /// [`Path::ReferenceJdk`](super::Path::ReferenceJdk) they are the point — interning, concat
    /// folding and `equals` are all compiler-and-runtime behaviour nobody has diffed. Against
    /// [`Path::Jit`](super::Path::Jit) they are inert weight: no string opcode is in the JIT's
    /// subset, so every probe is arithmetic the compiled arm does not get to see. Zero reproduces
    /// the grammar without strings exactly, which is what makes "what did strings cost us in JIT
    /// coverage?" a question with a measurable answer instead of a guess.
    pub string_share: u32,
    /// Out of 100, how often a statement is an array `new` or an array store.
    ///
    /// Drawn *before* the ordinary statement roll rather than as extra faces on it, so that setting
    /// it to zero leaves the rest of the distribution exactly as it was.
    pub array_share: u32,
    /// Whether an array may have an element type other than `int`.
    ///
    /// The exact counterpart of [`GenConfig::fp_narrowing`], and for the same reason: `iaload` and
    /// `iastore` are the **only element widths inside the JIT's subset**. `laload`, `faload`,
    /// `daload` and their storing twins are not listed in `burst::compile`'s opcode scan at all, so
    /// a `long[]` or a `double[]` does not compile *badly* — it refuses the whole method, exactly
    /// as a narrowing conversion does.
    ///
    /// So the same split: against a real `java` a `double[]` is worth having, and against the JIT
    /// it is a method that silently stopped being compiled. See
    /// `fuzz::campaigns::jit_coverage::what_each_grammar_setting_costs_in_jit_coverage` for what it
    /// actually costs, which is not what one would guess.
    pub wide_array_elements: bool,
    /// The largest literal length a `new` may take, and the ceiling on an in-range index.
    ///
    /// Small on purpose, twice over: the JIT only allocates inline below a byte ceiling
    /// (`burst::compile::MAX_INLINE_ARRAY_BYTES`), and `newarray` costs a zeroing loop that the
    /// budget has to pay for. A length of 200 inside a nest of loops is how a generator writes a
    /// benchmark by accident.
    pub max_array_len: i32,
    /// Out of 100, how often a statement is an object `new`, a reassignment or a `putfield`.
    ///
    /// Drawn *after* the array roll and before the ordinary one, so that a share of zero consumes
    /// no randomness at all and reproduces the array grammar byte for byte.
    pub object_share: u32,
    /// Whether the hierarchy has a `long` field and a `long`-returning virtual.
    ///
    /// The third knob of this shape, after [`GenConfig::fp_narrowing`] and
    /// [`GenConfig::wide_array_elements`]. `getfield` and `putfield` are in the JIT's subset **for
    /// a non-volatile `int` instance field and nothing else**, so a method that reads `b` or calls
    /// `w()` is refused. That much is a fact about `burst::compile`'s resolver.
    ///
    /// # The measurement that changed this default
    ///
    /// The fear was worse than the refusal, and specific: `burst::compile` **inlines** the
    /// `invokespecial` of a constructor into its caller, so a constructor writing a `long` looked
    /// like it would take every method containing a `new` out of the compiled arm with it. That
    /// would be a large effect — the compiled-method count collapsing toward the objects-off row —
    /// and it is not what
    /// `fuzz::campaigns::jit_coverage::what_each_grammar_setting_costs_in_jit_coverage` found:
    ///
    /// | | entered native | methods compiled | deopts |
    /// |---|---|---|---|
    /// | no objects at all | 57/80 | 121 | 18 |
    /// | objects, `int` fields | 67/80 | 405 | 754 |
    /// | objects, **`long` fields** | 71/80 | 401 | 802 |
    ///
    /// The rows are different *populations* — each configuration generates its own 80 programs —
    /// so a difference of four programs is not attributable to anything. But the predicted effect
    /// was a collapse toward the first row, and 401 is not that. The constructor is not poisoning
    /// its callers, so the knob defaults **on**: against a real `java` the `long` half is worth
    /// having, exactly as [`GenConfig::wide_array_elements`] is.
    ///
    /// The field is still absent from the emitted source when nothing reads it — see [`ObjUse`] —
    /// so a reduced case never carries a `long` a human then has to rule out.
    pub wide_fields: bool,
    /// Out of 100, how often an object `new` is a plain `null` instead.
    ///
    /// A `null` receiver is a genuinely valuable path — `getfield`, `putfield` and `invokevirtual`
    /// on one all **deopt** out of compiled code rather than throwing inside it — and it is drawn
    /// rarely for exactly the reason [`Gen::index_expr`] draws an out-of-range index rarely. FZ-005
    /// measured what happens otherwise: a program that throws on warm-up iteration 1 never reaches
    /// `JitCache::THRESHOLD`, so the JIT never sees it, and the campaign compares the interpreter
    /// against itself while reporting nothing wrong.
    ///
    /// It is never drawn inside the planted [`Gen::dispatch_shape`], which has to survive.
    pub null_share: u32,
    /// Whether the entry method gets a planted monomorphic and/or polymorphic call site.
    ///
    /// # Why this is planted rather than left to chance
    ///
    /// A polymorphic **site** is not a polymorphic *program*: it needs one bytecode call site whose
    /// receiver's class changes between executions of that same site, which in this grammar means a
    /// reassignment inside a loop over a name declared outside it. Rolling that shape out of the
    /// ordinary statement distribution would happen a few times in a thousand seeds.
    ///
    /// FZ-004 and FZ-005 were both the same mistake — assuming coverage that was not there — found
    /// by measuring rather than by reasoning. Planting the shape is that lesson applied before the
    /// fact, and the knob exists so the coverage campaign can still measure what it costs.
    pub dispatch_probe: bool,
    /// Out of 100, how often a statement is a **reference-field store** and an object read goes
    /// **through** the reference field.
    ///
    /// **Off by default, and it pulls in opposite directions on the two pairings** — the same shape
    /// as [`GenConfig::narrowing_share`], for the same kind of reason. A `putfield` of a reference
    /// is answered `Ineligible` by `burst::compile`, and the refusal is **per method**: one of
    /// these anywhere in the entry method and the whole thing runs interpreted on *both* arms of an
    /// interpreter-versus-JIT campaign, which is FZ-004 wearing the write barrier's hat.
    ///
    /// Against the reference JDK and against `os-parallel` compared with itself it is the opposite:
    /// the edge it creates is the one the collector's barrier is written for, and no other
    /// construct in the grammar can make one.
    pub ref_field_share: u32,
    /// Out of 100, how often an object statement is a **type probe** — `instanceof` or a cast.
    ///
    /// **On by default**, unlike the other two object knobs of this level, and the reason is the
    /// one that decides every share in this config: `checkcast` and `instanceof` are **inside**
    /// `burst::compile`'s subset — answered against one exact class, with a deopt for anything else
    /// — so they add coverage instead of costing it. The same argument that keeps `switch_share` on
    /// and `narrowing_share` off.
    pub cast_share: u32,
    /// How many worker threads the planted parallel site spawns. `0` turns the site off.
    ///
    /// **Off by default**, and the reason is the one this whole level had to solve first: a
    /// concurrent program is only usable to a *differential* oracle if its answer is fixed, and
    /// [`Stmt::Fork`] buys that with a shape rigid enough that most of the interesting interleaving
    /// is unobservable on purpose. It is worth running deliberately —
    /// `fuzz::campaigns::os_parallel_agrees_with_itself_on_deterministic_programs` is where it
    /// earns its keep — and it is not worth paying for on every seed of every other campaign,
    /// where a thread is by far the most expensive thing the grammar can ask for.
    pub workers: usize,
    /// How many times `run()` calls the entry method before returning.
    ///
    /// # Why this is not 1
    ///
    /// Because it was, and the measurement said no. `JitCache::THRESHOLD` is **32**: a method is
    /// only compiled after that many invocations, or on-stack once a loop has gone round enough
    /// times. A program whose `run()` calls the body once crosses neither threshold, and then
    /// `JVM_JIT=0` and `JVM_JIT` unset are *the same engine*. `fuzz::campaigns::jit_coverage`
    /// measured it: with `warmup: 1`, **7 of 60** generated programs ever entered native code. The
    /// interpreter-vs-JIT campaign was comparing the interpreter against itself 88% of the time,
    /// and a clean report meant almost nothing. Written up as FZ-004.
    ///
    /// 40 clears the threshold with margin. It is safe to repeat the call because a generated
    /// method is **pure** — no fields, no arrays, no statics, nothing carried between calls — so
    /// every iteration computes the same value, and the program's meaning is unchanged.
    pub warmup: i32,
}

impl Default for GenConfig {
    fn default() -> GenConfig {
        GenConfig {
            max_methods: 3,
            max_params: 3,
            max_stmts: 5,
            max_expr_depth: 3,
            max_block_depth: 2,
            max_loop_bound: 5,
            // 3000 statement-executions per call, times `warmup` calls, is a few milliseconds in
            // the interpreter — small enough that a campaign is dominated by process startup rather
            // than by the programs themselves.
            budget: 3000,
            // A third of the draws floating. High enough that most programs touch IEEE somewhere,
            // low enough that the integral grammar — where every bug this project has found by
            // hand actually lived — keeps most of the seeds.
            fp_share: 34,
            fp_narrowing: true,
            // On by default, and low: it is the one addition of this milestone that costs the
            // compiled arm nothing, so there is no reason to hide it behind a flag.
            switch_share: 12,
            interface_share: 50,
            while_share: 30,
            jump_share: 15,
            throw_share: 0,
            // Zero for the same reason `string_share` is: it costs JIT coverage, and the pairing
            // that gains from it is not the one the standing campaigns run.
            narrowing_share: 0,
            // Zero by default: strings are the newest arm and the one pairing that gains from
            // them is not the one the standing campaigns run. `jit_coverage` turns it on to
            // measure the cost, and the reference-JDK campaign is where it earns its keep.
            string_share: 0,
            array_share: 22,
            wide_array_elements: true,
            max_array_len: 6,
            // Objects are the newest arm of the grammar and the one with the most ways to cost JIT
            // coverage, so the share is a little under the arrays' and every setting below is the
            // JIT-friendly one until `fuzz::campaigns::jit_coverage` says otherwise.
            object_share: 18,
            wide_fields: true,
            null_share: 4,
            dispatch_probe: true,
            ref_field_share: 0,
            cast_share: 25,
            workers: 0,
            warmup: 40,
        }
    }
}

/// The generator. Holds no state between programs other than the configuration: a [`Seed`] is the
/// entire input, which is what makes a finding reproducible from one number.
#[derive(Clone, Debug, Default)]
pub struct JavaGenerator {
    pub config: GenConfig,
}

impl JavaGenerator {
    pub fn new(config: GenConfig) -> JavaGenerator {
        JavaGenerator { config }
    }
}

impl super::Generator for JavaGenerator {
    type Program = JavaProgram;

    fn generate(&mut self, seed: Seed) -> JavaProgram {
        Gen::new(self.config, seed).program(seed)
    }
}

/// Generation state for one program.
struct Gen {
    config: GenConfig,
    rng: Rng,
    /// How many `switch` arms enclose the statement being generated.
    ///
    /// Needed because `break` binds to the innermost enclosing **loop or switch**: inside an arm it
    /// leaves the `switch`, not the loop. Emitting one there would make [`SwitchArm::breaks`] lie
    /// about its own arm, so inside an arm only `continue` is drawn — which binds to the loop
    /// whatever is in between.
    switch_depth: u32,
    /// How many loops enclose the statement being generated. `break` and `continue` are only legal
    /// inside one, and this is the cheapest way to know — the scope tracks *names*, not control
    /// flow, so it cannot answer the question.
    loop_depth: u32,
    /// The static cost of each already-generated method, indexed the same as
    /// [`JavaProgram::methods`]. A call is only emitted when the callee fits the remaining budget.
    costs: Vec<u64>,
    /// Signatures of the methods generated so far, for choosing a callee and typing its arguments.
    signatures: Vec<(Vec<Ty>, Ty)>,
    /// Fresh-name counter, so no generated name can shadow another.
    next_name: u32,
    /// What is left of the execution budget at the point being generated. Divided on entering a
    /// loop, consulted before emitting a call.
    budget: u64,
}

impl Gen {
    fn new(config: GenConfig, seed: Seed) -> Gen {
        Gen {
            config,
            rng: Rng::from_seed(seed),
            switch_depth: 0,
            loop_depth: 0,
            costs: Vec::new(),
            signatures: Vec::new(),
            next_name: 0,
            budget: config.budget,
        }
    }

    fn fresh(&mut self, prefix: &str) -> String {
        let name = format!("{prefix}{}", self.next_name);
        self.next_name += 1;
        name
    }

    /// A type, weighted by [`GenConfig::fp_share`]. With a share of zero this is the old coin flip
    /// between `int` and `long`, byte for byte.
    fn any_ty(&mut self) -> Ty {
        let share = self.config.fp_share.min(100);
        if share > 0 && self.rng.below(100) < share {
            if self.rng.chance(1, 2) {
                Ty::Float
            } else {
                Ty::Double
            }
        } else if self.rng.chance(1, 2) {
            Ty::Int
        } else {
            Ty::Long
        }
    }

    /// A floating type, for the operand of a classifier or a narrowing conversion.
    fn any_fp_ty(&mut self) -> Ty {
        if self.rng.chance(1, 2) {
            Ty::Float
        } else {
            Ty::Double
        }
    }

    fn program(&mut self, seed: Seed) -> JavaProgram {
        let helper_count = self.rng.below(self.config.max_methods as u32 + 1) as usize;
        let mut methods = Vec::with_capacity(helper_count);
        for index in 0..helper_count {
            // Helpers get a slice of the budget so a chain of them cannot add up past it.
            self.budget = self.config.budget / 4;
            let method = self.method(index);
            self.costs.push(method.cost);
            self.signatures
                .push((method.params.iter().map(|(_, t)| *t).collect(), method.returns));
            methods.push(method);
        }
        self.budget = self.config.budget;
        let entry = self.entry_method(methods.len());
        JavaProgram { class: format!("Fz{}", seed.0), methods, entry, warmup: self.config.warmup }
    }

    /// A helper method: any return type, some parameters.
    fn method(&mut self, index: usize) -> Method {
        let returns = self.any_ty();
        let param_count = self.rng.below(self.config.max_params as u32 + 1) as usize;
        let mut scope = Scope::default();
        let mut params = Vec::with_capacity(param_count);
        for i in 0..param_count {
            let ty = self.any_ty();
            let name = format!("p{i}");
            params.push((name.clone(), ty));
            scope.locals.push(Local::scalar(name, ty));
        }
        self.finish_method(format!("m{index}"), params, returns, scope, false)
    }

    /// The method `run()` calls. Always returns `int` — that is the only thing the executor can
    /// observe — and takes no parameters, since nobody would have anything to pass.
    fn entry_method(&mut self, index: usize) -> Method {
        // `true`: the planted dispatch shape goes in the **entry** method and nowhere else. It is
        // the one method `run()` calls 40 times, so it is the one guaranteed to cross
        // `JitCache::THRESHOLD` and actually be compiled.
        self.finish_method(format!("m{index}"), Vec::new(), Ty::Int, Scope::default(), true)
    }

    fn finish_method(
        &mut self,
        name: String,
        params: Vec<(String, Ty)>,
        returns: Ty,
        mut scope: Scope,
        plant: bool,
    ) -> Method {
        let planting = plant && self.config.object_share > 0 && self.config.dispatch_probe;
        let (mut body, mut accumulators) =
            if planting { self.dispatch_shape(&mut scope) } else { (Vec::new(), Vec::new()) };
        // The parallel site goes in the **entry** method only (`plant`), and once: the worker class
        // carries the bodies in a `switch` over `k`, so a second site would need a second class.
        if plant && self.config.workers > 0 {
            let (stmts, acc) = self.parallel_site(&mut scope);
            body.extend(stmts);
            accumulators.push(acc);
        }
        self.budget = self.budget.saturating_sub(self.block_cost(&body));
        body.extend(self.block(&mut scope, 0));

        let mut result = self.expr(&scope, returns, 0);
        // **The accumulators are folded into the result on purpose.**
        //
        // Without this the planted shape would be coverage that never reaches the observable: the
        // entry method's result is generated independently, so it would read the dispatch
        // accumulator only by luck, and a seed where it did not would execute every virtual call,
        // miss the inline cache exactly as intended, deopt — and return a number that could not
        // possibly differ. That is FZ-004's mistake in miniature (work the campaign believed it was
        // measuring and was not), so the dependency is built rather than hoped for.
        //
        // `31 *` and not a bare `+`: addition is commutative, so two accumulators that swapped
        // values would sum the same. It is the same shift-and-add the warm-up loop in
        // [`JavaProgram::to_java`] uses, for the same reason.
        for (name, ty) in accumulators {
            let read = Expr::Var(name, ty);
            // `(int)` of a `long` is `l2i`, a plain truncation and inside the JIT's subset — not
            // one of the saturating conversions [`GenConfig::fp_narrowing`] is careful about.
            let as_int = if ty == Ty::Int { read } else { Expr::Cast(Ty::Int, Box::new(read)) };
            result = Expr::Bin(
                BinOp::Add,
                Box::new(Expr::Bin(BinOp::Mul, Box::new(result), Box::new(Expr::IntLit(31)))),
                Box::new(as_int),
            );
        }
        let cost = self.block_cost(&body) + self.expr_cost(&result);
        Method { name, params, returns, body, result, cost }
    }

    // -- objects, fields and dispatch ----------------------------------------------------------

    /// The planted inline-cache probe: a monomorphic site, a polymorphic one, or both.
    ///
    /// Returns the statements and the **accumulators** they write, which [`Gen::finish_method`]
    /// folds into the method's result so that what the sites compute is actually observable.
    fn dispatch_shape(&mut self, scope: &mut Scope) -> (Vec<Stmt>, Vec<(String, Ty)>) {
        let mut stmts = Vec::new();
        let mut accs = Vec::new();
        let roll = self.rng.below(3);
        if roll != 1 {
            let (s, acc) = self.monomorphic_site(scope);
            stmts.extend(s);
            accs.push(acc);
        }
        if roll != 0 {
            let (s, acc) = self.polymorphic_site(scope);
            stmts.extend(s);
            accs.push(acc);
        }
        (stmts, accs)
    }

    /// **The parallel site.** `K` workers over disjoint slots, joined, reduced in index order.
    ///
    /// Returns the statement and the accumulator, which [`Gen::finish_method`] folds into the
    /// method's result for the same reason the dispatch probes' accumulators are folded: a site
    /// whose value never reaches the observable is coverage that cannot fail, which is FZ-004's
    /// mistake in miniature.
    fn parallel_site(&mut self, scope: &mut Scope) -> (Vec<Stmt>, (String, Ty)) {
        let k = self.config.workers;
        let slots = self.fresh("s");
        let threads = self.fresh("t");
        let counter = self.fresh("i");
        let acc = self.fresh("p");

        // Evaluated in the enclosing scope, before anything starts.
        let args = (self.expr(scope, Ty::Int, 1), self.expr(scope, Ty::Int, 1));

        // The bodies see the worker's three fields and nothing else — a separate scope, which is
        // what makes "a worker cannot read the enclosing method" true by construction rather than
        // by remembering.
        let bodies = (0..k)
            .map(|_| {
                // A fresh scope per worker: one `case` arm cannot see another's declarations.
                let mut worker = Scope {
                    locals: ["k", "a", "b"].into_iter().map(Local::worker_field).collect(),
                    foreign: true,
                };
                let block = self.block(&mut worker, 0);
                self.budget = self.budget.saturating_sub(self.block_cost(&block));
                let result = self.expr(&worker, Ty::Int, 1);
                ForkBody { block, result }
            })
            .collect();

        scope.locals.push(Local::scalar(acc.clone(), Ty::Int));
        let stmt = Stmt::Fork { slots, threads, counter, acc: acc.clone(), args, bodies };
        (vec![stmt], (acc, Ty::Int))
    }

    /// A call site whose receiver never changes: the inline cache is filled on the first execution
    /// and its guard holds for every one after, so native code is never left.
    fn monomorphic_site(&mut self, scope: &mut Scope) -> (Vec<Stmt>, (String, Ty)) {
        let class = *self.rng.pick(OBJ_CLASSES);
        let method = self.virtual_method();
        let ty = method.ty();
        let bound = 2 + self.rng.below(3) as i32;
        let arg = self.ctor_arg(scope);

        let obj = self.fresh("o");
        let acc = self.fresh("v");
        let counter = self.fresh("i");
        scope.locals.push(Local::object(obj.clone()));
        scope.locals.push(Local::scalar(acc.clone(), ty));

        let site = Stmt::Assign {
            name: acc.clone(),
            ty,
            expr: Expr::Bin(
                BinOp::Add,
                Box::new(Expr::Var(acc.clone(), ty)),
                Box::new(Expr::Virtual(obj.clone(), method)),
            ),
        };
        let stmts = vec![
            // La sonda plantada se declara por la clase base a proposito: su medicion ya
            // existe y cambiarle el tipo declarado le cambiaria el significado.
            Stmt::NewObject { name: obj, class: Some(class), arg, iface: false },
            Stmt::Declare { name: acc.clone(), ty, init: Expr::zero(ty) },
            Stmt::For { var: counter, bound, body: vec![site] },
        ];
        (stmts, (acc, ty))
    }

    /// A call site whose receiver rotates between two classes on alternate iterations, after
    /// starting as a third.
    ///
    /// Three classes over the site's life rather than two, because the first execution only *fills*
    /// the cache — it cannot miss. Starting at the base means the first guarded execution is
    /// already a miss, instead of the site spending an iteration establishing the answer it is
    /// about to contradict.
    fn polymorphic_site(&mut self, scope: &mut Scope) -> (Vec<Stmt>, (String, Ty)) {
        let (first, second) = self.two_classes();
        // Through the interface the same probe becomes an **itable** site, and a far more
        // interesting one: this is the only place where a receiver is guaranteed to change class
        // on every iteration of a loop hot enough to be compiled, so the inline cache's guard
        // fails every time instead of settling. Ordinary generation reassigns an interface-typed
        // name in about 2% of programs, which is indistinguishable from never.
        let iface = self.config.interface_share > 0
            && self.rng.below(100) < self.config.interface_share;
        // The interface declares `v()` and nothing else, so the wide half is not on offer here.
        let method = if iface { VMethod::V } else { self.virtual_method() };
        let ty = method.ty();
        let bound = 2 + self.rng.below(3) as i32;
        let initial = self.ctor_arg(scope);
        let left = self.ctor_arg(scope);
        let right = self.ctor_arg(scope);

        let obj = self.fresh(if iface { "q" } else { "o" });
        let acc = self.fresh("v");
        let counter = self.fresh("i");
        scope.locals.push(if iface {
            Local::object_via_interface(obj.clone())
        } else {
            Local::object(obj.clone())
        });
        scope.locals.push(Local::scalar(acc.clone(), ty));

        // `(i & 1) == 0` — the receiver alternates, so the guard fails on every iteration after the
        // first of each class.
        let rotate = Stmt::If {
            cond: Cond::Cmp(
                CmpOp::Eq,
                Expr::Bin(
                    BinOp::And,
                    Box::new(Expr::Var(counter.clone(), Ty::Int)),
                    Box::new(Expr::IntLit(1)),
                ),
                Expr::IntLit(0),
            ),
            then: vec![Stmt::SetObject { name: obj.clone(), class: Some(first), arg: left }],
            otherwise: vec![Stmt::SetObject { name: obj.clone(), class: Some(second), arg: right }],
        };
        let site = Stmt::Assign {
            name: acc.clone(),
            ty,
            expr: Expr::Bin(
                BinOp::Add,
                Box::new(Expr::Var(acc.clone(), ty)),
                Box::new(Expr::Virtual(obj.clone(), method)),
            ),
        };
        let stmts = vec![
            Stmt::NewObject { name: obj, class: Some(ObjClass::Base), arg: initial, iface },
            Stmt::Declare { name: acc.clone(), ty, init: Expr::zero(ty) },
            Stmt::For { var: counter, bound, body: vec![rotate, site] },
        ];
        (stmts, (acc, ty))
    }

    /// Two **distinct** subclasses. Distinct is the whole point: the same class twice is a
    /// monomorphic site written the long way.
    fn two_classes(&mut self) -> (ObjClass, ObjClass) {
        let subs = [ObjClass::S0, ObjClass::S1, ObjClass::S2];
        let first = self.rng.below(3) as usize;
        let second = (first + 1 + self.rng.below(2) as usize) % 3;
        (subs[first], subs[second])
    }

    /// `v()` unless the hierarchy has a `long` half to call into.
    fn virtual_method(&mut self) -> VMethod {
        if self.config.wide_fields && self.rng.chance(1, 3) {
            VMethod::W
        } else {
            VMethod::V
        }
    }

    fn any_field(&mut self) -> Field {
        if self.config.wide_fields && self.rng.chance(1, 3) {
            Field::B
        } else {
            Field::A
        }
    }

    /// The `int` a constructor is handed. Shallow: the field values want to be varied, not the
    /// expression that produced them, and the budget has a whole method left to pay for.
    fn ctor_arg(&mut self, scope: &Scope) -> Expr {
        self.expr(scope, Ty::Int, self.config.max_expr_depth.saturating_sub(1))
    }

    /// Which class a `new` builds, and its argument — or `None` for a `null`, whose argument is a
    /// literal zero because the emitter does not write it (see [`Stmt::NewObject`]).
    fn new_object(&mut self, scope: &Scope) -> (Option<ObjClass>, Expr) {
        if self.config.null_share > 0 && self.rng.below(100) < self.config.null_share {
            return (None, Expr::IntLit(0));
        }
        let class = *self.rng.pick(OBJ_CLASSES);
        (Some(class), self.ctor_arg(scope))
    }

    /// A `new`, a reassignment or a `putfield`. `None` when nothing applies, so [`Gen::stmt`] falls
    /// through to the ordinary roll rather than emitting nothing — the shape [`Gen::array_stmt`]
    /// already has.
    fn object_stmt(&mut self, scope: &mut Scope) -> Option<Stmt> {
        let existing = scope.objects().len();
        // **Above the `new` branch**, because a type probe constructs nothing: all it needs is a
        // receiver that already exists. Left below it the probe was reachable only 60% of the time
        // and a share of 40 produced it in 19 of 200 programs — the same mistake the reference
        // store made, and the reason every share in this config gets measured rather than assumed.
        if existing > 0
            && self.config.cast_share > 0
            && self.rng.below(100) < self.config.cast_share
        {
            let plain: Vec<String> = scope.objects().into_iter().map(str::to_string).collect();
            let obj = self.rng.pick(&plain).clone();
            let class = *self.rng.pick(&[ObjClass::Base, ObjClass::S0, ObjClass::S1, ObjClass::S2]);
            // Half tests, half casts. The test can never fail and the cast can, and both are worth
            // generating: one exercises the guard, the other the guard *and* the path out of it.
            let cast = if self.rng.chance(1, 2) { Some(Field::A) } else { None };
            let name = self.fresh("y");
            scope.locals.push(Local::scalar(name.clone(), Ty::Int));
            return Some(Stmt::TypeProbe { name, obj, class, cast });
        }
        // The first draw in a scope has to be a `new`: there is nothing to reassign or store into.
        if existing == 0 || self.rng.chance(2, 5) {
            let (class, arg) = self.new_object(scope);
            let iface = self.config.interface_share > 0
                && self.rng.below(100) < self.config.interface_share;
            let name = self.fresh(if iface { "q" } else { "o" });
            scope.locals.push(if iface {
                Local::object_via_interface(name.clone())
            } else {
                Local::object(name.clone())
            });
            return Some(Stmt::NewObject { name, class, arg, iface });
        }
        // **Before the other two, not after.** Left at the end it was reachable only through two
        // prior coin flips, so a share of 45 produced the store in 15% of programs — the knob would
        // not have meant what it says, and this is the one construct the whole level is about.
        if self.config.ref_field_share > 0 && self.rng.below(100) < self.config.ref_field_share {
            let plain: Vec<String> = scope.objects().into_iter().map(str::to_string).collect();
            let obj = self.rng.pick(&plain).clone();
            // `null` a third of the time. Not filler: it is how a chain that read fine on one
            // iteration reads `null` on the next, which is the deopt the compiled arm has to
            // survive — and, on the interpreted one, an ordinary `NullPointerException`.
            let value =
                if self.rng.chance(1, 3) { None } else { Some(self.rng.pick(&plain).clone()) };
            return Some(Stmt::RefStore { obj, value });
        }
        if self.rng.chance(1, 2) {
            // Reassignment is offered on interface-typed names too: `q = new S1(…)` is
            // type-correct, and a receiver whose class changes is exactly what makes the inline
            // cache's guard fire instead of hitting.
            let names: Vec<String> = scope
                .objects()
                .into_iter()
                .chain(scope.iface_objects())
                .map(str::to_string)
                .collect();
            let name = self.rng.pick(&names).clone();
            let (class, arg) = self.new_object(scope);
            return Some(Stmt::SetObject { name, class, arg });
        }
        // A field store needs a declared type that *has* the field, so only the plain names — and
        // `existing > 0` above already proved there is one.
        let plain: Vec<String> = scope.objects().into_iter().map(str::to_string).collect();
        let obj = self.rng.pick(&plain).clone();
        let field = self.any_field();
        let value = self.expr(scope, field.ty(), 1);
        Some(Stmt::FieldStore { obj, field, value })
    }

    /// `o.a` or `o.v()` — a field read or a dispatched call, when one can produce `ty`.
    ///
    /// `None` for `float` and `double`: the hierarchy has no floating half, and inventing one would
    /// buy nothing the arithmetic grammar does not already cover far better.
    fn object_read(&mut self, scope: &Scope, ty: Ty) -> Option<Expr> {
        // Through an interface only `v()` exists, so this is the only shape offered — and it is
        // the one that compiles to `invokeinterface`.
        if ty == Ty::Int {
            let via_iface = scope.iface_objects();
            if !via_iface.is_empty() && self.rng.chance(2, 3) {
                let name = (*self.rng.pick(&via_iface)).to_string();
                return Some(Expr::Virtual(name, VMethod::V));
            }
        }
        let names = scope.objects();
        if names.is_empty() {
            return None;
        }
        // The chained read. Offered before the plain one so the knob's share is the share of
        // *object reads that hop*, which is what the census measures.
        if self.config.ref_field_share > 0
            && self.rng.below(100) < self.config.ref_field_share
        {
            if let Some(field) = Field::of_ty(ty, self.config.wide_fields) {
                let name = (*self.rng.pick(&names)).to_string();
                return Some(Expr::ThroughRef(name, field));
            }
        }
        let (field, method) = match ty {
            Ty::Int => (Field::A, VMethod::V),
            Ty::Long if self.config.wide_fields => (Field::B, VMethod::W),
            _ => return None,
        };
        let name = (*self.rng.pick(&names)).to_string();
        // Half and half. The call is the interesting one, but a `getfield` right beside it is what
        // makes the two comparable when a campaign reports something.
        Some(if self.rng.chance(1, 2) {
            Expr::Field(name, field)
        } else {
            Expr::Virtual(name, method)
        })
    }

    /// Statements are **charged as they are generated**, not checked afterwards. Consulting the
    /// budget only at call sites (the first version of this) leaves the multiplication that
    /// actually matters unaccounted for: five statements, each a loop of five, each containing five
    /// statements is already 125× before a single method call is involved. Paying for each
    /// statement out of a running budget bounds the whole tree instead of one construct in it.
    fn block(&mut self, scope: &mut Scope, depth: u32) -> Block {
        let count = self.rng.below(self.config.max_stmts as u32 + 1) as usize;
        let mut block = Vec::with_capacity(count);
        for _ in 0..count {
            if self.budget == 0 {
                break;
            }
            if let Some(stmt) = self.stmt(scope, depth) {
                self.budget = self.budget.saturating_sub(self.stmt_cost(&stmt));
                let leaves = stmt.completes_abruptly();
                block.push(stmt);
                // Nothing after a statement that always leaves: it would be unreachable, and
                // `javac` rejects that. This is where an `if` with two abrupt arms gets caught —
                // the arms are legal on their own and only the statement *after* the `if` is not.
                if leaves {
                    return block;
                }
            }
        }
        if let Some(last) = self.terminator(depth) {
            block.push(last);
        }
        block
    }

    /// A statement that ends a block: `break`, `continue` or `throw`.
    ///
    /// **Only ever last, and never at depth 0.** Java rejects an unreachable statement, so one of
    /// these anywhere but the end of its block is a compile error — which is exactly what happened
    /// when they were drawn like ordinary statements: usable seeds fell from 100% to 78%, every
    /// loss reading `error: unreachable statement`. And depth 0 is a *method body*, whose emitted
    /// form ends in `return <result>;`: a terminator there would make the `return` unreachable
    /// instead.
    fn terminator(&mut self, depth: u32) -> Option<Stmt> {
        if depth == 0 {
            return None;
        }
        if self.loop_depth > 0
            && self.config.jump_share > 0
            && self.rng.below(100) < self.config.jump_share
        {
            // Inside a `switch` arm only `continue` is safe; see [`Gen::switch_depth`].
            let can_break = self.switch_depth == 0;
            return Some(if can_break && self.rng.chance(1, 2) {
                Stmt::Break
            } else {
                Stmt::Continue
            });
        }
        if self.config.throw_share > 0 && self.rng.below(100) < self.config.throw_share {
            let exc = *self.rng.pick(&[
                ThrownExc::Arithmetic,
                ThrownExc::Bounds,
                ThrownExc::NegativeSize,
                ThrownExc::NullPointer,
            ]);
            return Some(Stmt::Throw(exc));
        }
        None
    }

    fn stmt(&mut self, scope: &mut Scope, depth: u32) -> Option<Stmt> {
        let can_nest = depth < self.config.max_block_depth;
        // Arrays are drawn before the main roll rather than as extra faces on it, so that turning
        // `array_share` down does not quietly reweight everything else. At a share of zero the
        // roll below is exactly the one that was there before.
        if self.config.array_share > 0 && self.rng.below(100) < self.config.array_share {
            if let Some(stmt) = self.array_stmt(scope) {
                return Some(stmt);
            }
        }
        // And objects after them, on the same terms: a share of zero draws nothing, so it leaves
        // both the array grammar and the roll below exactly as they were.
        if self.config.object_share > 0 && self.rng.below(100) < self.config.object_share {
            if let Some(stmt) = self.object_stmt(scope) {
                return Some(stmt);
            }
        }
        // The weights are the grammar's shape: mostly straight-line arithmetic, because that is
        // where an interpreter and a JIT have the most surface to disagree on, with enough control
        // flow to build the loops the JIT actually compiles.
        let roll = self.rng.below(if can_nest { 10 } else { 6 });
        match roll {
            0..=3 => {
                let ty = self.any_ty();
                let name = self.fresh("v");
                let init = self.expr(scope, ty, 0);
                scope.locals.push(Local::scalar(name.clone(), ty));
                Some(Stmt::Declare { name, ty, init })
            }
            4..=5 => {
                let ty = self.any_ty();
                let targets: Vec<String> =
                    scope.assignable(ty).into_iter().map(str::to_string).collect();
                if targets.is_empty() {
                    return None;
                }
                let name = self.rng.pick(&targets).clone();
                let expr = self.expr(scope, ty, 0);
                Some(Stmt::Assign { name, ty, expr })
            }
            6..=7 if self.config.switch_share > 0
                && self.rng.below(100) < self.config.switch_share =>
            {
                self.switch_stmt(scope, depth)
            }
            6..=7 => {
                let ty = self.any_ty();
                let cond = self.cond(scope, ty, 0);
                let mut then_scope = scope.clone();
                let then = self.block(&mut then_scope, depth + 1);
                let otherwise = if self.rng.chance(1, 2) {
                    let mut else_scope = scope.clone();
                    self.block(&mut else_scope, depth + 1)
                } else {
                    Vec::new()
                };
                Some(Stmt::If { cond, then, otherwise })
            }
            _ => {
                let bound = 1 + self.rng.below(self.config.max_loop_bound as u32) as i32;
                // The budget is what stops `for` inside `for` inside a call from turning into a
                // benchmark. Dividing it here is the whole mechanism, and a `while` divides by its
                // guard limit for exactly the same reason.
                let outer = self.budget;
                self.budget = (self.budget / bound.max(1) as u64).max(1);
                let counted = self.config.while_share == 0
                    || self.rng.below(100) >= self.config.while_share;
                let var = self.fresh(if counted { "i" } else { "g" });
                let mut body_scope = scope.clone();
                // Readable, *not* assignable. See [`Local::assignable`]. The `while` guard carries
                // the same rule: a body that could reset it would be a body that never has to stop.
                body_scope.locals.push(Local {
                    name: var.clone(),
                    ty: Ty::Int,
                    assignable: false,
                    array_of: None,
                    array_len: 0,
                    object: false,
                    object_iface: false,
                });
                self.loop_depth += 1;
                let body = self.block(&mut body_scope, depth + 1);
                self.loop_depth -= 1;
                self.budget = outer;
                if counted {
                    return Some(Stmt::For { var, bound, body });
                }
                let cond_ty = self.any_ty();
                let cond = self.cond(scope, cond_ty, 0);
                Some(Stmt::While { guard: var, limit: bound, cond, body })
            }
        }
    }

    /// A `new` or a store. `None` when neither is available, which lets [`Gen::stmt`] fall through
    /// to the ordinary roll instead of emitting nothing.
    fn array_stmt(&mut self, scope: &mut Scope) -> Option<Stmt> {
        let existing = scope.arrays().len();
        // A store needs somewhere to store *to*, so the first draw in a scope is always a `new`.
        // After that the two are worth roughly the same, except that a scope full of arrays nobody
        // writes to is a scope where every load reads the zeros `newarray` left behind — which
        // tests the zeroing loop and nothing else.
        if existing == 0 || self.rng.chance(2, 5) {
            let elem = self.array_elem_ty();
            let len = self.array_len();
            let name = self.fresh("a");
            scope.locals.push(Local {
                name: name.clone(),
                ty: elem,
                assignable: false,
                array_of: Some(elem),
                array_len: len,
                object: false,
                object_iface: false,
            });
            return Some(Stmt::NewArray { name, elem, len });
        }
        let arrays = scope.arrays();
        let (name, elem, len) = *self.rng.pick(&arrays);
        let name = name.to_string();
        let index = self.index_expr(scope, len);
        let value = self.expr(scope, elem, 1);
        Some(Stmt::ArrayStore { array: name, elem, index, value })
    }

    /// A `switch`, dense or scattered.
    ///
    /// The choice between the two is not cosmetic: consecutive labels are what make `javac` emit a
    /// `tableswitch` and scattered ones a `lookupswitch`, and those are **different opcodes with
    /// different decoders** — the second one searches. A generator that only ever produced one
    /// shape would leave half of the pair untested and look exactly like one that tested both.
    ///
    /// The selector is masked rather than left arbitrary. An unbounded `int` expression would miss
    /// every label essentially always, so the arms would be dead code and the whole construct would
    /// reduce to "evaluate the selector, run `default`".
    fn switch_stmt(&mut self, scope: &Scope, depth: u32) -> Option<Stmt> {
        let count = 2 + self.rng.below(3) as usize;
        let dense = self.rng.chance(1, 2);
        let (labels, mask): (Vec<i32>, i32) = if dense {
            let base = self.rng.below(3) as i32 - 1;
            ((0..count).map(|i| base + i as i32).collect(), 3)
        } else {
            // Spread wide enough that `javac` cannot fold them into a table, and inside the mask so
            // the selector still lands on one now and then.
            let pool = [0i32, 5, 17, 44, 99, 126];
            let mut picked: Vec<i32> = Vec::new();
            while picked.len() < count {
                let candidate = *self.rng.pick(&pool);
                if !picked.contains(&candidate) {
                    picked.push(candidate);
                }
            }
            (picked, 127)
        };
        let inner = self.expr(scope, Ty::Int, 1);
        let selector =
            Expr::Bin(BinOp::And, Box::new(inner), Box::new(Expr::IntLit(mask)));
        let arms = labels
            .into_iter()
            .map(|label| {
                let mut arm_scope = scope.clone();
                self.switch_depth += 1;
                let body = self.block(&mut arm_scope, depth + 1);
                self.switch_depth -= 1;
                // An arm whose body already leaves — `continue`, or a `throw` — neither breaks nor
                // falls through, and the `break` after it would be **unreachable**, which `javac`
                // rejects. So the flag is not drawn in that case, it is forced.
                let leaves = matches!(
                    body.last(),
                    Some(Stmt::Break | Stmt::Continue | Stmt::Throw(_))
                );
                SwitchArm {
                    label,
                    body,
                    // Fall-through a third of the time. Rare enough that a `switch` still usually
                    // reads like one, common enough that a campaign of any size meets it.
                    breaks: !leaves && !self.rng.chance(1, 3),
                }
            })
            .collect();
        let default = if self.rng.chance(1, 2) {
            let mut default_scope = scope.clone();
            Some(self.block(&mut default_scope, depth + 1))
        } else {
            None
        };
        Some(Stmt::Switch { selector, arms, default })
    }

    /// A string question. The probe is drawn first because it decides how many string operands
    /// the node needs, and `Same` is weighted like the others rather than kept rare: reference
    /// identity is the property this stage exists to check.
    fn str_probe(&mut self, depth: u32) -> Expr {
        let value = self.str_expr(depth);
        let probe = match self.rng.below(3) {
            0 => StrProbe::Length,
            1 => StrProbe::Identity(Box::new(self.str_expr(depth))),
            _ => StrProbe::Same(Box::new(self.str_expr(depth))),
        };
        Expr::Str(probe, Box::new(value))
    }

    /// A `String`-valued subtree. Depth is charged the same way arithmetic charges it, so a string
    /// cannot be the one construct that escapes [`GenConfig::max_expr_depth`].
    fn str_expr(&mut self, depth: u32) -> StrExpr {
        if depth >= self.config.max_expr_depth {
            return StrExpr::Lit(self.rng.below(STRING_POOL.len() as u32) as usize);
        }
        match self.rng.below(6) {
            0 => StrExpr::Concat(
                Box::new(self.str_expr(depth + 1)),
                Box::new(self.str_expr(depth + 1)),
            ),
            1 => StrExpr::Fresh(Box::new(self.str_expr(depth + 1))),
            _ => StrExpr::Lit(self.rng.below(STRING_POOL.len() as u32) as usize),
        }
    }

    /// The element type of a new array. `int` most of the time, and deliberately so: `iaload` and
    /// `iastore` are the only element widths **inside the JIT's subset** (`laload`, `faload`,
    /// `daload` and their storing twins are not), so an `int[]` is the one that gets compiled and
    /// the others are worth having for the reference-JDK pairing.
    fn array_elem_ty(&mut self) -> Ty {
        if !self.config.wide_array_elements || self.rng.chance(3, 5) {
            return Ty::Int;
        }
        let ty = self.any_ty();
        // `fp_share` still governs whether IEEE appears at all, including inside an array.
        if ty.is_fp() && self.config.fp_share == 0 {
            Ty::Long
        } else {
            ty
        }
    }

    /// A literal array length. Small, because the JIT's inline allocation has a byte ceiling and
    /// the interpreter has a heap; and occasionally **negative**, which is the other way an
    /// allocation fails ([`marks::NEGATIVE_SIZE`]).
    fn array_len(&mut self) -> i32 {
        match self.rng.below(25) {
            0 => 0,
            1 => -(1 + self.rng.below(3) as i32),
            _ => 1 + self.rng.below(self.config.max_array_len as u32) as i32,
        }
    }

    /// An index into an array of length `len`.
    ///
    /// # The balance FZ-005 forced
    ///
    /// An index outside `[0, length)` is the most valuable thing an array grammar can produce: the
    /// JIT's `iaload` does not *throw*, it **deopts** — it emits a bounds guard that leaves native
    /// code and hands the pc back to the interpreter, which then raises the exception the compiled
    /// code declined to. Testing that boundary crossing is most of the point of having arrays.
    ///
    /// But it can only be tested from *inside compiled code*, and compiled code needs 32
    /// invocations to exist. A program that throws on warm-up iteration 1 dies before the JIT has
    /// looked at it, and then the out-of-range access tested the interpreter twice.
    ///
    /// The first version of this function generated an index in `0..max_array_len` **without
    /// knowing which array it was for**, so an index into a length-2 array missed about half the
    /// time. Measured: 46% of seeds died on a marker and the JIT arm's coverage halved, from 73/80
    /// to 40/80. That is FZ-005, and it is FZ-004 wearing a different hat.
    ///
    /// So the shape now: `len` is consulted, an in-range index is the common case, and the misses
    /// come from the two arms that cannot be in range by construction. Rare per access, and still
    /// dozens of seeds per campaign.
    fn index_expr(&mut self, scope: &Scope, len: i32) -> Expr {
        // A zero- or negative-length array has no index that is in range, so there is no choice to
        // make here and no point pretending otherwise.
        if len <= 0 {
            return Expr::IntLit(0);
        }
        match self.rng.below(20) {
            0..=15 => Expr::IntLit(self.rng.below(len as u32) as i32),
            16..=18 => {
                let names = scope.readable(Ty::Int);
                if names.is_empty() {
                    return Expr::IntLit(0);
                }
                Expr::Var((*self.rng.pick(&names)).to_string(), Ty::Int)
            }
            _ => self.expr(scope, Ty::Int, self.config.max_expr_depth.saturating_sub(1)),
        }
    }

    fn cond(&mut self, scope: &Scope, ty: Ty, depth: u32) -> Cond {
        if depth >= 2 {
            let left = self.expr(scope, ty, 1);
            let right = self.expr(scope, ty, 1);
            return Cond::Cmp(*self.rng.pick(CMP_OPS), left, right);
        }
        match self.rng.below(6) {
            0..=3 => {
                let left = self.expr(scope, ty, 1);
                let right = self.expr(scope, ty, 1);
                Cond::Cmp(*self.rng.pick(CMP_OPS), left, right)
            }
            4 => {
                let a = self.cond(scope, ty, depth + 1);
                let b = self.cond(scope, ty, depth + 1);
                if self.rng.chance(1, 2) {
                    Cond::And(Box::new(a), Box::new(b))
                } else {
                    Cond::Or(Box::new(a), Box::new(b))
                }
            }
            _ => Cond::Not(Box::new(self.cond(scope, ty, depth + 1))),
        }
    }

    /// The heart of property 1: this always returns an expression of exactly `ty`.
    ///
    /// Every arm below is gated on what Java actually allows at `ty`. The three that floating point
    /// took away — `&`/`|`/`^`, the shifts and `~` — fall back to another arm rather than being
    /// skipped, so the weights stay roughly the shape the integral grammar had.
    fn expr(&mut self, scope: &Scope, ty: Ty, depth: u32) -> Expr {
        if depth >= self.config.max_expr_depth {
            return self.leaf(scope, ty);
        }
        // Drawn before the roll, like the array and object *statements* are, and for the same
        // reason: at a share of zero this consumes no randomness and the grammar below is byte for
        // byte the one that was there before objects existed.
        if self.config.object_share > 0 && self.rng.below(100) < self.config.object_share {
            if let Some(read) = self.object_read(scope, ty) {
                return read;
            }
        }
        // Same treatment, same reason. A string question only answers `int`, so the share is spent
        // only where it can be honoured — otherwise a high share would silently do nothing on a
        // `long` subtree and the knob would not mean what it says.
        // Not in a worker body: the `Same` probe compiles to a call to `ssame`, emitted
        // unqualified on the program's own class, which does not resolve from another one — see
        // [`Scope::foreign`].
        if ty == Ty::Int
            && !scope.foreign
            && self.config.string_share > 0
            && self.rng.below(100) < self.config.string_share
        {
            return self.str_probe(depth);
        }
        if ty == Ty::Int
            && self.config.narrowing_share > 0
            && self.rng.below(100) < self.config.narrowing_share
        {
            let to = *self.rng.pick(&[NarrowTy::Byte, NarrowTy::Short, NarrowTy::Char]);
            return Expr::Narrow(to, Box::new(self.expr(scope, Ty::Int, depth + 1)));
        }
        match self.rng.below(17) {
            0..=3 => self.leaf(scope, ty),
            4..=6 => {
                let op = *self.rng.pick(if ty.is_fp() { FP_BIN_OPS } else { BIN_OPS });
                let left = self.expr(scope, ty, depth + 1);
                let right = self.expr(scope, ty, depth + 1);
                // Half the integral divisions are guarded, half are not. Both halves matter: an
                // unguarded one reaches `ArithmeticException` (and `Long.MIN_VALUE / -1`, which
                // does *not* throw), and a guarded one lets the rest of the program keep running
                // instead of every seed collapsing into the same marker.
                //
                // A floating division is **never** guarded: `1.0 / 0.0` is `Infinity` and
                // `0.0 % 0.0` is `NaN`, both of which are values worth reaching, and neither
                // throws anything for the guard to prevent.
                let right = if !ty.is_fp() && op.traps_on_zero() && self.rng.chance(1, 2) {
                    guard_zero(right)
                } else {
                    right
                };
                Expr::Bin(op, Box::new(left), Box::new(right))
            }
            7..=8 if !ty.is_fp() => {
                let op = *self.rng.pick(SHIFT_OPS);
                let value = self.expr(scope, ty, depth + 1);
                // Always an `int`, whatever `ty` is — `long << int` is legal Java and yields
                // `long`. Drawing the amount from the pool is what makes counts of 31/32/33/63/64
                // common instead of astronomically rare.
                let amount = self.expr(scope, Ty::Int, depth + 1);
                Expr::Shift(op, Box::new(value), Box::new(amount))
            }
            9 => {
                let inner = self.expr(scope, ty, depth + 1);
                // `~` is integral-only. `fneg`/`dneg` are not arithmetic negation but a sign-bit
                // flip, which is why `- 0.0` is `-0.0` and `- NaN` is still a NaN — worth having.
                if ty.is_fp() || self.rng.chance(1, 2) {
                    Expr::Neg(Box::new(inner))
                } else {
                    Expr::Not(Box::new(inner))
                }
            }
            10 => self.conversion(scope, ty, depth),
            11 => {
                let cmp_ty = self.any_ty();
                let cond = self.cond(scope, cmp_ty, 1);
                let then = self.expr(scope, ty, depth + 1);
                let otherwise = self.expr(scope, ty, depth + 1);
                Expr::Ternary(Box::new(cond), Box::new(then), Box::new(otherwise))
            }
            // The classifier is the only way a floating *value* reaches the `int` the executor
            // observes without going through a conversion that is itself under test. It is
            // therefore not an optional flourish: without it, a whole seed's floating arithmetic
            // could be invisible. See [`emit_classifier`].
            12 if ty == Ty::Int && self.config.fp_share > 0 && !scope.foreign => {
                let fp = self.any_fp_ty();
                Expr::Classify(Box::new(self.expr(scope, fp, depth + 1)))
            }
            13..=14 if self.config.array_share > 0 => self.array_read(scope, ty, depth),
            _ => self.call_or_leaf(scope, ty, depth),
        }
    }

    /// `a[i]`, or `a.length` when an `int` is wanted. Falls back to [`Gen::call_or_leaf`] when no
    /// array of the right element type is in scope — the same totality argument every other arm
    /// makes.
    fn array_read(&mut self, scope: &Scope, ty: Ty, depth: u32) -> Expr {
        // `a.length` is the one array read that cannot throw, which makes it the one that keeps a
        // program *alive* long enough for the rest of its arithmetic to be observed.
        if ty == Ty::Int && self.rng.chance(1, 3) {
            let arrays = scope.arrays();
            if !arrays.is_empty() {
                return Expr::ArrayLength(self.rng.pick(&arrays).0.to_string());
            }
        }
        let names = scope.arrays_of(ty);
        if names.is_empty() {
            // No array of this element type, but an `int` can still come out of any array at all.
            let arrays = scope.arrays();
            if ty == Ty::Int && !arrays.is_empty() {
                return Expr::ArrayLength(self.rng.pick(&arrays).0.to_string());
            }
            return self.call_or_leaf(scope, ty, depth);
        }
        let (name, len) = *self.rng.pick(&names);
        let name = name.to_string();
        let index = self.index_expr(scope, len);
        Expr::ArrayLoad(name, ty, Box::new(index))
    }

    /// A cast that produces `ty`. Which source types are on offer is where the interesting
    /// asymmetry lives.
    fn conversion(&mut self, scope: &Scope, ty: Ty, depth: u32) -> Expr {
        let from = if ty.is_fp() {
            // Widening from an integral type (`i2f`, `i2d`, `l2f`, `l2d`) or across the two
            // floating widths (`f2d`, `d2f`). All six are in the JIT's subset.
            let sources: &[Ty] = match ty {
                Ty::Float => &[Ty::Int, Ty::Long, Ty::Double],
                _ => &[Ty::Int, Ty::Long, Ty::Float],
            };
            *self.rng.pick(sources)
        } else if self.config.fp_share > 0 && self.config.fp_narrowing && self.rng.chance(1, 2) {
            // `f2i` / `f2l` / `d2i` / `d2l` — JLS §5.1.3, and the reason
            // [`GenConfig::fp_narrowing`] exists.
            self.any_fp_ty()
        } else {
            match ty {
                Ty::Int => Ty::Long,
                _ => Ty::Int,
            }
        };
        Expr::Cast(ty, Box::new(self.expr(scope, from, depth + 1)))
    }

    /// A call to an already-generated method of the right return type, if one exists and fits the
    /// remaining budget. Otherwise a leaf — the fallback is what keeps [`Gen::expr`] total.
    fn call_or_leaf(&mut self, scope: &Scope, ty: Ty, depth: u32) -> Expr {
        // Nothing this program declares is in view from another class — see [`Scope::foreign`].
        if scope.foreign {
            return self.leaf(scope, ty);
        }
        let candidates: Vec<usize> = (0..self.signatures.len())
            .filter(|&i| self.signatures[i].1 == ty && self.costs[i] < self.budget)
            .collect();
        if candidates.is_empty() {
            return self.leaf(scope, ty);
        }
        let index = *self.rng.pick(&candidates);
        let param_types = self.signatures[index].0.clone();
        // Charge the call before generating its arguments, so a second call in the same expression
        // sees a budget that already accounts for the first.
        self.budget = self.budget.saturating_sub(self.costs[index]);
        let args = param_types.iter().map(|&t| self.expr(scope, t, depth + 1)).collect();
        Expr::Call(index, args, ty)
    }

    /// A variable or a constant. Variables are preferred when any exist, because a program made of
    /// literals is one `javac` constant-folds into nothing and neither engine ever executes.
    fn leaf(&mut self, scope: &Scope, ty: Ty) -> Expr {
        let names = scope.readable(ty);
        if !names.is_empty() && self.rng.chance(3, 5) {
            let name = (*self.rng.pick(&names)).to_string();
            return Expr::Var(name, ty);
        }
        match ty {
            Ty::Int => Expr::IntLit(*self.rng.pick(INT_POOL)),
            Ty::Long => Expr::LongLit(*self.rng.pick(LONG_POOL)),
            Ty::Float => Expr::FloatLit(*self.rng.pick(FLOAT_POOL)),
            Ty::Double => Expr::DoubleLit(*self.rng.pick(DOUBLE_POOL)),
        }
    }

    // -- the cost model (property 3) ----------------------------------------------------------

    fn block_cost(&self, block: &Block) -> u64 {
        block.iter().map(|s| self.stmt_cost(s)).sum()
    }

    fn stmt_cost(&self, stmt: &Stmt) -> u64 {
        match stmt {
            Stmt::Break | Stmt::Continue | Stmt::Throw(_) => 1,
            // Charged like a `for` of the same bound: the guard is what makes `limit` an honest
            // upper bound on the trip count.
            Stmt::While { limit, cond, body, .. } => {
                1 + cond.size() as u64 + (*limit).max(1) as u64 * self.block_cost(body)
            }
            // **Every** arm is charged, not the average one. Fall-through means a single entry can
            // run several of them, and a budget that assumed one would be wrong exactly on the
            // shapes this construct exists to generate.
            Stmt::Switch { selector, arms, default } => {
                1 + self.expr_cost(selector)
                    + arms.iter().map(|a| self.block_cost(&a.body)).sum::<u64>()
                    + default.as_ref().map_or(0, |b| self.block_cost(b))
            }
            Stmt::Declare { init, .. } => 1 + self.expr_cost(init),
            Stmt::Assign { expr, .. } => 1 + self.expr_cost(expr),
            Stmt::If { cond, then, otherwise } => {
                // The worst case takes the larger arm; an `if` never runs both.
                1 + self.cond_cost(cond)
                    + self.block_cost(then).max(self.block_cost(otherwise))
            }
            Stmt::For { bound, body, .. } => {
                1 + (*bound).max(0) as u64 * (1 + self.block_cost(body))
            }
            // A `newarray` is not one statement's worth of work: the VM zeroes the storage, so the
            // length is the cost. Charging only `1` here is how a loop nest full of allocations
            // would slip past a budget that thought it had bounded everything.
            Stmt::NewArray { len, .. } => 1 + (*len).max(0) as u64,
            // Two allocations per worker (the thread object and its OS thread), the body, and
            // three loops over the workers. Priced generously and on purpose: a thread is the most
            // expensive thing this grammar can ask for, and property 3 bounds *work*, not
            // statements.
            Stmt::Fork { args, bodies, .. } => {
                let k = bodies.len() as u64;
                1 + self.expr_cost(&args.0)
                    + self.expr_cost(&args.1)
                    + 3 * k
                    + bodies
                        .iter()
                        .map(|w| 8 + self.block_cost(&w.block) + self.expr_cost(&w.result))
                        .sum::<u64>()
            }
            Stmt::ArrayStore { index, value, .. } => {
                1 + self.expr_cost(index) + self.expr_cost(value)
            }
            // An allocation plus a constructor chain, charged as three rather than one for the
            // reason `newarray` is charged for its length: a `new` inside a loop nest is real work,
            // and a budget that priced it at one statement would have bounded nothing.
            Stmt::NewObject { arg, .. } | Stmt::SetObject { arg, .. } => {
                3 + self.expr_cost(arg)
            }
            Stmt::FieldStore { value, .. } => 1 + self.expr_cost(value),
            Stmt::RefStore { .. } => 1,
            // A `checkcast`/`instanceof` plus, for the cast, a `getfield`.
            Stmt::TypeProbe { cast, .. } => 1 + usize::from(cast.is_some()) as u64,
        }
    }

    fn cond_cost(&self, cond: &Cond) -> u64 {
        match cond {
            Cond::Cmp(_, a, b) => 1 + self.expr_cost(a) + self.expr_cost(b),
            Cond::And(a, b) | Cond::Or(a, b) => 1 + self.cond_cost(a) + self.cond_cost(b),
            Cond::Not(a) => 1 + self.cond_cost(a),
        }
    }

    fn expr_cost(&self, expr: &Expr) -> u64 {
        match expr {
            Expr::IntLit(_) | Expr::LongLit(_) | Expr::FloatLit(_) | Expr::DoubleLit(_) => 1,
            Expr::Var(_, _) => 1,
            // The classifier's body is fixed: four special-case branches and one comparison per
            // probe. Charging it honestly is what keeps a program that classifies inside a nest of
            // loops from silently costing thirty times what the budget was told.
            Expr::Classify(a) => CLASSIFY_COST + self.expr_cost(a),
            // A concat allocates and a compare walks the contents; both are cheap next to the
            // classifier, and neither loops.
            Expr::Str(probe, value) => (probe.size() + value.size()) as u64,
            Expr::Narrow(_, a) => 1 + self.expr_cost(a),
            Expr::ArrayLength(_) => 1,
            Expr::ArrayLoad(_, _, index) => 1 + self.expr_cost(index),
            // Two `getfield`s instead of one, and priced as such.
            Expr::Field(_, _) => 1,
            Expr::ThroughRef(_, _) => 2,
            // The dispatch plus the callee's one-expression body.
            Expr::Virtual(_, _) => 2,
            Expr::Neg(a) | Expr::Not(a) | Expr::Cast(_, a) => 1 + self.expr_cost(a),
            Expr::Bin(_, a, b) | Expr::Shift(_, a, b) => 1 + self.expr_cost(a) + self.expr_cost(b),
            Expr::Ternary(c, a, b) => {
                1 + self.cond_cost(c) + self.expr_cost(a).max(self.expr_cost(b))
            }
            Expr::Call(index, args, _) => {
                let callee = self.costs.get(*index).copied().unwrap_or(0);
                1 + callee + args.iter().map(|a| self.expr_cost(a)).sum::<u64>()
            }
        }
    }
}

/// `(d == 0 ? 1 : d)` — a divisor that cannot be zero, at the same type.
///
/// Integral only. A floating divisor needs no guard (see [`Gen::expr`]), and calling this on one
/// would suppress `Infinity` and `NaN` — two of the three results this stage exists to reach.
fn guard_zero(divisor: Expr) -> Expr {
    let ty = divisor.ty();
    debug_assert!(!ty.is_fp(), "a floating divisor must not be guarded — see Gen::expr");
    let (zero, one) = match ty {
        Ty::Long => (Expr::LongLit(0), Expr::LongLit(1)),
        _ => (Expr::IntLit(0), Expr::IntLit(1)),
    };
    Expr::Ternary(
        Box::new(Cond::Cmp(CmpOp::Eq, divisor.clone(), zero)),
        Box::new(one),
        Box::new(divisor),
    )
}

/// Statements charged for one [`Expr::Classify`]: four special-case branches plus one comparison
/// per probe, which is what [`emit_classifier`] writes.
const CLASSIFY_COST: u64 = 4 + DOUBLE_PROBES.len() as u64;

const BIN_OPS: &[BinOp] = &[
    BinOp::Add,
    BinOp::Sub,
    BinOp::Mul,
    BinOp::Div,
    BinOp::Rem,
    BinOp::And,
    BinOp::Or,
    BinOp::Xor,
];

/// The operators a floating operand accepts. `&`, `|` and `^` are integral-only in Java; `%` is
/// **not** — `frem`/`drem` are real instructions with a real definition (`a - (b * trunc(a/b))`),
/// and the JIT compiles them to an unconditional deopt on purpose, which makes them one of the
/// more interesting things in this list.
const FP_BIN_OPS: &[BinOp] =
    &[BinOp::Add, BinOp::Sub, BinOp::Mul, BinOp::Div, BinOp::Rem];
const SHIFT_OPS: &[ShiftOp] = &[ShiftOp::Left, ShiftOp::Right, ShiftOp::Unsigned];
const CMP_OPS: &[CmpOp] =
    &[CmpOp::Lt, CmpOp::Le, CmpOp::Gt, CmpOp::Ge, CmpOp::Eq, CmpOp::Ne];
/// The base included: a receiver that is exactly the declared type still goes through
/// `invokevirtual` and still fills an inline cache, and a hierarchy whose base is never instantiated
/// would never test that.
const OBJ_CLASSES: &[ObjClass] =
    &[ObjClass::Base, ObjClass::S0, ObjClass::S1, ObjClass::S2];

// ---------------------------------------------------------------------------------------------
// Well-formedness
// ---------------------------------------------------------------------------------------------

/// Why a program is not valid Java. The reducer gates every candidate on this: rather than proving
/// each individual cut safe — a proof that has to be redone for every new transform — it cuts
/// freely and throws away anything that stops type-checking. Cheaper to write and much harder to
/// get subtly wrong.
#[derive(Clone, PartialEq, Eq, Debug)]
pub enum Malformed {
    /// A worker body that does not produce an `int`. The slot array is `int[]`, so nothing else
    /// can be stored, and a `long` would take the whole worker out of the JIT's subset besides.
    ForkBodyIsNotInt(Ty),
    /// A parallel site with no workers. It would still compile and still be deterministic — and it
    /// would spawn no threads at all, which is the one thing the construct exists to do.
    ForkWithoutWorkers,
    /// A field read, a field store or a `w()` through a name whose declared type is the interface.
    /// An interface declares neither, so this is a `javac` error rather than a wrong answer — and
    /// catching it here keeps a reducer candidate from being spent on it.
    FieldThroughInterface(String),
    /// A `switch` selector that is not an `int`. Java allows `String` and `enum` too; this
    /// grammar does not generate either, so anything else is a generator bug.
    SwitchOnNonInt(Ty),
    /// [`Expr::Narrow`] was handed something that is not an `int`. `i2b`/`i2s`/`i2c` take an `int`
    /// and nothing else; a `long` would need `l2i` first, which is a different node.
    NarrowOnNonInt(Ty),
    /// A name that is not in scope at the point it is read.
    UnboundVariable(String),
    /// A name read at a type other than the one it was declared with.
    WrongType { name: String, declared: Ty, used: Ty },
    /// A binary operator whose two operands are not the same type.
    MixedOperands(BinOp),
    /// `&`, `|`, `^`, a shift or `~` applied to a `float` or a `double`. Every one of them is a
    /// `javac` rejection, i.e. a bug in this file — see [`BinOp::accepts`].
    IntegralOperatorOnFloat(&'static str),
    /// [`Expr::Classify`] handed something that is not floating. The node's whole reason to exist
    /// is the floating result channel; on an `int` it would emit a call to a helper the class does
    /// not carry.
    ClassifyOnNonFloat(Ty),
    /// An array name that is not in scope — or that is in scope as a *scalar*. The two are the same
    /// failure from the emitter's point of view (`v3[0]` on an `int` does not compile), and the
    /// reducer produces both constantly by deleting the `new` that declared one.
    NotAnArray(String),
    /// An element read or written at a type other than the array's. `long[] a; a[0] = 1;` is
    /// actually legal Java by widening, but the AST says the reducer broke an invariant — and a
    /// reducer that silently changes what a program means is worse than one that gives up.
    ArrayElementType { name: String, declared: Ty, used: Ty },
    /// An index that is not an `int`. Java would accept a `short` or a `char`; this grammar has
    /// neither, so anything but `int` here is a bug.
    BadArrayIndex(Ty),
    /// A comparison between an `int` and a `long`. (Java would widen; the AST says the reducer
    /// broke an invariant, and a reducer that silently changes semantics is worse than one that
    /// gives up.)
    MixedComparison,
    /// A ternary whose arms are different types.
    MixedTernary,
    /// A call to a method that does not exist, or one that would make the call graph cyclic.
    BadCall(usize),
    /// A call whose argument list does not match the callee's parameters.
    BadArguments(usize),
    /// A `for` whose bound is not positive — which would be a loop that never runs, and more to
    /// the point a sign that a shrink went somewhere it should not.
    BadLoopBound(i32),
    /// A method whose `result` is not of its declared return type.
    WrongReturnType(String),
    /// An assignment to a `for` counter — the one way this grammar could stop terminating.
    AssignedLoopCounter(String),
    /// Method `k` is not called `mk`. The emitter writes a call site as `m<index>`, so the name and
    /// the position have to agree; the reducer can delete a method, and renumbering is exactly the
    /// kind of bookkeeping that is worth having checked rather than trusted.
    MisnamedMethod { position: usize, name: String },
    /// A name used as a receiver that is not in scope, or is in scope as a scalar or an array. The
    /// reducer produces this constantly by deleting the `new` that declared an object — the exact
    /// counterpart of [`Malformed::NotAnArray`].
    NotAnObject(String),
    /// A `putfield` whose value is not the field's type. Java would widen an `int` into a `long`
    /// field; the AST says the reducer broke an invariant, and a reducer that silently changes what
    /// a program means is worse than one that gives up.
    FieldValueType { field: Field, used: Ty },
    /// A constructor argument that is not an `int`. The hierarchy has one constructor shape, so
    /// anything else here is a shrink that went somewhere it should not.
    BadConstructorArgument(Ty),
}

impl JavaProgram {
    /// Checks everything [`Malformed`] lists. Returns the first problem, if any.
    pub fn well_formed(&self) -> Result<(), Malformed> {
        for (index, method) in self.methods.iter().enumerate() {
            if method.name != format!("m{index}") {
                return Err(Malformed::MisnamedMethod {
                    position: index,
                    name: method.name.clone(),
                });
            }
            check_method(method, &self.methods[..index])?;
        }
        let last = self.methods.len();
        if self.entry.name != format!("m{last}") {
            return Err(Malformed::MisnamedMethod {
                position: last,
                name: self.entry.name.clone(),
            });
        }
        if self.entry.returns != Ty::Int || !self.entry.params.is_empty() {
            return Err(Malformed::WrongReturnType(self.entry.name.clone()));
        }
        // A warm-up of zero is a program whose body never runs at all — the emitted `run()` would
        // return the accumulator's initial value and the campaign would compare nothing.
        if self.warmup <= 0 {
            return Err(Malformed::BadLoopBound(self.warmup));
        }
        check_method(&self.entry, &self.methods)?;
        Ok(())
    }
}

fn check_method(method: &Method, visible: &[Method]) -> Result<(), Malformed> {
    let mut scope: Vec<Local> = method
        .params
        .iter()
        .map(|(name, ty)| Local::scalar(name.clone(), *ty))
        .collect();
    check_block(&method.body, &mut scope, visible)?;
    check_expr(&method.result, &scope, visible)?;
    if method.result.ty() != method.returns {
        return Err(Malformed::WrongReturnType(method.name.clone()));
    }
    Ok(())
}

fn check_block(
    block: &Block,
    scope: &mut Vec<Local>,
    visible: &[Method],
) -> Result<(), Malformed> {
    for stmt in block {
        match stmt {
            Stmt::Fork { acc, args, bodies, .. } => {
                // The constructor arguments belong to the **enclosing** scope and are ordinary
                // expressions there.
                for arg in [&args.0, &args.1] {
                    check_expr(arg, scope, visible)?;
                    if arg.ty() != Ty::Int {
                        return Err(Malformed::BadConstructorArgument(arg.ty()));
                    }
                }
                // The bodies belong to a **different class**, and this is where that is enforced.
                // Their scope is exactly the worker's three fields — a body naming an enclosing
                // local would be a `javac` error, not a wrong answer — and `visible` is empty,
                // because the program's static helpers are emitted unqualified and a worker cannot
                // resolve them from outside the class that declares them.
                for worker in bodies {
                    // A **fresh** scope per worker, because each `case` arm gets its own braces:
                    // sharing one would let arm 1 read what arm 0 declared, which compiles here
                    // and not in Java.
                    let mut names: Vec<Local> =
                        ["k", "a", "b"].into_iter().map(Local::worker_field).collect();
                    check_block(&worker.block, &mut names, &[])?;
                    check_expr(&worker.result, &names, &[])?;
                    if worker.result.ty() != Ty::Int {
                        return Err(Malformed::ForkBodyIsNotInt(worker.result.ty()));
                    }
                }
                if bodies.is_empty() {
                    return Err(Malformed::ForkWithoutWorkers);
                }
                // Only the accumulator survives into the enclosing scope; the array, the thread
                // array and the loop variable are the shape's own plumbing.
                scope.push(Local::scalar(acc.clone(), Ty::Int));
            }
            Stmt::Declare { name, ty, init } => {
                check_expr(init, scope, visible)?;
                if init.ty() != *ty {
                    return Err(Malformed::WrongType {
                        name: name.clone(),
                        declared: *ty,
                        used: init.ty(),
                    });
                }
                scope.push(Local::scalar(name.clone(), *ty));
            }
            Stmt::Assign { name, ty, expr } => {
                let local = scope
                    .iter()
                    .rev()
                    .find(|l| &l.name == name)
                    .ok_or_else(|| Malformed::UnboundVariable(name.clone()))?;
                if !local.assignable {
                    return Err(Malformed::AssignedLoopCounter(name.clone()));
                }
                // An object local carries `Ty::Int` as a placeholder, so without this an
                // `o0 = <int>` would type-check here and emit source `javac` rejects. The generator
                // cannot build one — [`Scope::assignable`] filters objects out — but the checker's
                // job is to hold for programs the *reducer* built, and it is a cheap thing to be
                // wrong about.
                if local.object || local.array_of.is_some() {
                    return Err(Malformed::WrongType {
                        name: name.clone(),
                        declared: local.ty,
                        used: *ty,
                    });
                }
                if local.ty != *ty || expr.ty() != *ty {
                    return Err(Malformed::WrongType {
                        name: name.clone(),
                        declared: local.ty,
                        used: expr.ty(),
                    });
                }
                check_expr(expr, scope, visible)?;
            }
            Stmt::If { cond, then, otherwise } => {
                check_cond(cond, scope, visible)?;
                let mut inner = scope.clone();
                check_block(then, &mut inner, visible)?;
                let mut inner = scope.clone();
                check_block(otherwise, &mut inner, visible)?;
            }
            // `break`/`continue` outside a loop is a `javac` error, so a generator bug shows up as
            // an unusable seed rather than as a wrong answer. Nothing to check here.
            Stmt::Break | Stmt::Continue | Stmt::Throw(_) => {}
            Stmt::While { guard, limit, cond, body } => {
                if *limit <= 0 {
                    return Err(Malformed::BadLoopBound(*limit));
                }
                check_cond(cond, scope, visible)?;
                let mut inner = scope.clone();
                inner.push(Local {
                    name: guard.clone(),
                    ty: Ty::Int,
                    // Not assignable, for the same reason the `for` counter is not: a body that
                    // could reset it would be a body that never has to stop.
                    assignable: false,
                    array_of: None,
                    array_len: 0,
                    object: false,
                    object_iface: false,
                });
                check_block(body, &mut inner, visible)?;
            }
            Stmt::Switch { selector, arms, default } => {
                check_expr(selector, scope, visible)?;
                if selector.ty() != Ty::Int {
                    return Err(Malformed::SwitchOnNonInt(selector.ty()));
                }
                // Each arm gets its own copy of the scope: the braces around an arm's body are a
                // real block, so a local declared in one is not in scope in the next.
                for arm in arms {
                    let mut inner = scope.clone();
                    check_block(&arm.body, &mut inner, visible)?;
                }
                if let Some(body) = default {
                    let mut inner = scope.clone();
                    check_block(body, &mut inner, visible)?;
                }
            }
            Stmt::For { var, bound, body } => {
                if *bound <= 0 {
                    return Err(Malformed::BadLoopBound(*bound));
                }
                let mut inner = scope.clone();
                inner.push(Local {
                    name: var.clone(),
                    ty: Ty::Int,
                    assignable: false,
                    array_of: None,
                    array_len: 0,
                    object: false,
                    object_iface: false,
                });
                check_block(body, &mut inner, visible)?;
            }
            // A negative length is *not* checked: `new int[-1]` is legal Java that throws at run
            // time, and it is one of the two failures this grammar wants to observe.
            Stmt::NewArray { name, elem, .. } => {
                scope.push(Local {
                    name: name.clone(),
                    ty: *elem,
                    assignable: false,
                    array_of: Some(*elem),
                    array_len: 0,
                    object: false,
                    object_iface: false,
                });
            }
            Stmt::ArrayStore { array, elem, index, value } => {
                let declared = array_element(scope, array)?;
                if declared != *elem || value.ty() != *elem {
                    return Err(Malformed::ArrayElementType {
                        name: array.clone(),
                        declared,
                        used: value.ty(),
                    });
                }
                check_expr(index, scope, visible)?;
                if index.ty() != Ty::Int {
                    return Err(Malformed::BadArrayIndex(index.ty()));
                }
                check_expr(value, scope, visible)?;
            }
            Stmt::NewObject { name, arg, iface, .. } => {
                check_expr(arg, scope, visible)?;
                if arg.ty() != Ty::Int {
                    return Err(Malformed::BadConstructorArgument(arg.ty()));
                }
                // Carrying `iface` into the scope is what makes the restriction checkable at all.
                // Forget it here and `q.a` type-checks, which would let a reducer candidate be
                // counted as valid and then be rejected by `javac` — a spent process spawn
                // reported as a healthy candidate.
                scope.push(if *iface {
                    Local::object_via_interface(name.clone())
                } else {
                    Local::object(name.clone())
                });
            }
            Stmt::SetObject { name, arg, .. } => {
                object_local(scope, name)?;
                check_expr(arg, scope, visible)?;
                if arg.ty() != Ty::Int {
                    return Err(Malformed::BadConstructorArgument(arg.ty()));
                }
            }
            Stmt::TypeProbe { name, obj, cast, .. } => {
                // The receiver has to be a name whose declared type is the class: an interface can
                // be tested but the cast reads a field, and the two share this arm.
                object_local(scope, obj)?;
                if scope.iter().any(|l| l.name == *obj && l.object_iface) {
                    return Err(Malformed::FieldThroughInterface(obj.clone()));
                }
                // Always an `int`, whichever half it is: `instanceof` is folded to 1/0 and a
                // `long` field read through a cast would not fit the local this declares.
                if *cast == Some(Field::B) {
                    return Err(Malformed::ForkBodyIsNotInt(Ty::Long));
                }
                scope.push(Local::scalar(name.clone(), Ty::Int));
            }
            Stmt::RefStore { obj, value } => {
                // Both sides are object names of the **class** type: the field is declared `…B`,
                // and an interface-typed name could neither hold it nor be assigned into it.
                for name in std::iter::once(obj).chain(value.as_ref()) {
                    object_local(scope, name)?;
                    if scope.iter().any(|l| l.name == *name && l.object_iface) {
                        return Err(Malformed::FieldThroughInterface(name.clone()));
                    }
                }
            }
            Stmt::FieldStore { obj, field, value } => {
                object_local(scope, obj)?;
                check_expr(value, scope, visible)?;
                if value.ty() != field.ty() {
                    return Err(Malformed::FieldValueType { field: *field, used: value.ty() });
                }
            }
        }
    }
    Ok(())
}

/// That `name` is in scope **as an object**, or why it is not.
fn object_local(scope: &[Local], name: &str) -> Result<(), Malformed> {
    scope
        .iter()
        .rev()
        .find(|l| l.name == name)
        .filter(|l| l.object)
        .map(|_| ())
        .ok_or_else(|| Malformed::NotAnObject(name.to_string()))
}

/// The element type of the array called `name`, or why it is not one.
fn array_element(scope: &[Local], name: &str) -> Result<Ty, Malformed> {
    scope
        .iter()
        .rev()
        .find(|l| l.name == name)
        .and_then(|l| l.array_of)
        .ok_or_else(|| Malformed::NotAnArray(name.to_string()))
}

fn check_cond(cond: &Cond, scope: &[Local], visible: &[Method]) -> Result<(), Malformed> {
    match cond {
        Cond::Cmp(_, a, b) => {
            check_expr(a, scope, visible)?;
            check_expr(b, scope, visible)?;
            if a.ty() != b.ty() {
                return Err(Malformed::MixedComparison);
            }
            Ok(())
        }
        Cond::And(a, b) | Cond::Or(a, b) => {
            check_cond(a, scope, visible)?;
            check_cond(b, scope, visible)
        }
        Cond::Not(a) => check_cond(a, scope, visible),
    }
}

fn check_expr(expr: &Expr, scope: &[Local], visible: &[Method]) -> Result<(), Malformed> {
    match expr {
        Expr::IntLit(_) | Expr::LongLit(_) | Expr::FloatLit(_) | Expr::DoubleLit(_) => Ok(()),
        Expr::Var(name, ty) => {
            let local = scope
                .iter()
                .rev()
                .find(|l| &l.name == name)
                .ok_or_else(|| Malformed::UnboundVariable(name.clone()))?;
            // An array or an object read as a scalar emits `a0 + 1` or `o0 + 1`, neither of which
            // compiles. The reducer reaches this by replacing a subtree with a variable that
            // happens to share a name.
            if local.array_of.is_some() || local.object {
                return Err(Malformed::WrongType {
                    name: name.clone(),
                    declared: local.ty,
                    used: *ty,
                });
            }
            if local.ty != *ty {
                return Err(Malformed::WrongType {
                    name: name.clone(),
                    declared: local.ty,
                    used: *ty,
                });
            }
            Ok(())
        }
        Expr::Neg(a) => check_expr(a, scope, visible),
        Expr::Not(a) => {
            check_expr(a, scope, visible)?;
            if a.ty().is_fp() {
                return Err(Malformed::IntegralOperatorOnFloat("~"));
            }
            Ok(())
        }
        Expr::Cast(_, a) => check_expr(a, scope, visible),
        // Nothing to check: a string subtree names no local and has no type to get wrong.
        Expr::Str(_, _) => Ok(()),
        Expr::Narrow(_, a) => {
            check_expr(a, scope, visible)?;
            if a.ty() != Ty::Int {
                return Err(Malformed::NarrowOnNonInt(a.ty()));
            }
            Ok(())
        }
        Expr::Classify(a) => {
            check_expr(a, scope, visible)?;
            if !a.ty().is_fp() {
                return Err(Malformed::ClassifyOnNonFloat(a.ty()));
            }
            Ok(())
        }
        Expr::ArrayLoad(name, elem, index) => {
            let declared = array_element(scope, name)?;
            if declared != *elem {
                return Err(Malformed::ArrayElementType {
                    name: name.clone(),
                    declared,
                    used: *elem,
                });
            }
            check_expr(index, scope, visible)?;
            if index.ty() != Ty::Int {
                return Err(Malformed::BadArrayIndex(index.ty()));
            }
            Ok(())
        }
        Expr::ArrayLength(name) => array_element(scope, name).map(|_| ()),
        // A field read needs a name whose *declared* type has fields, which an interface does not.
        // Both read a field, so both need a name whose *declared* type has one — which an
        // interface does not.
        Expr::Field(name, _) | Expr::ThroughRef(name, _) => {
            object_local(scope, name)?;
            match scope.iter().find(|l| l.name == *name) {
                Some(l) if l.object_iface => Err(Malformed::FieldThroughInterface(name.clone())),
                _ => Ok(()),
            }
        }
        Expr::Virtual(name, method) => {
            object_local(scope, name)?;
            match scope.iter().find(|l| l.name == *name) {
                // The interface declares `v()` and nothing else.
                Some(l) if l.object_iface && *method != VMethod::V => {
                    Err(Malformed::FieldThroughInterface(name.clone()))
                }
                _ => Ok(()),
            }
        }
        Expr::Bin(op, a, b) => {
            check_expr(a, scope, visible)?;
            check_expr(b, scope, visible)?;
            if a.ty() != b.ty() {
                return Err(Malformed::MixedOperands(*op));
            }
            if !op.accepts(a.ty()) {
                return Err(Malformed::IntegralOperatorOnFloat(op.symbol()));
            }
            Ok(())
        }
        Expr::Shift(op, value, amount) => {
            check_expr(value, scope, visible)?;
            check_expr(amount, scope, visible)?;
            // Both operands are integral: there is no `fshl`, and `javac` rejects `1.0 << 2`.
            if value.ty().is_fp() {
                return Err(Malformed::IntegralOperatorOnFloat(op.symbol()));
            }
            // The amount must be `int` on both `int` and `long` shifts — see property 1.
            if amount.ty() != Ty::Int {
                return Err(Malformed::MixedComparison);
            }
            Ok(())
        }
        Expr::Ternary(cond, then, otherwise) => {
            check_cond(cond, scope, visible)?;
            check_expr(then, scope, visible)?;
            check_expr(otherwise, scope, visible)?;
            if then.ty() != otherwise.ty() {
                return Err(Malformed::MixedTernary);
            }
            Ok(())
        }
        Expr::Call(index, args, ty) => {
            let callee = visible.get(*index).ok_or(Malformed::BadCall(*index))?;
            if callee.returns != *ty {
                return Err(Malformed::BadCall(*index));
            }
            if callee.params.len() != args.len() {
                return Err(Malformed::BadArguments(*index));
            }
            for (arg, (_, param_ty)) in args.iter().zip(&callee.params) {
                check_expr(arg, scope, visible)?;
                if arg.ty() != *param_ty {
                    return Err(Malformed::BadArguments(*index));
                }
            }
            Ok(())
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::fuzz::Generator as _;

    fn gen() -> JavaGenerator {
        JavaGenerator::default()
    }

    fn program(seed: u64) -> JavaProgram {
        gen().generate(Seed(seed))
    }

    // -- the PRNG -----------------------------------------------------------------------------

    #[test]
    fn the_same_seed_gives_the_same_stream() {
        let mut a = Rng::from_seed(Seed(12345));
        let mut b = Rng::from_seed(Seed(12345));
        for _ in 0..1000 {
            assert_eq!(a.next_u64(), b.next_u64());
        }
    }

    #[test]
    fn consecutive_seeds_give_unrelated_streams() {
        // The property a campaign depends on: it walks seeds 0, 1, 2, …, and a generator whose
        // adjacent streams correlate would hand back programs differing only in a last constant.
        let first: Vec<u64> = (0..8).map(|s| Rng::from_seed(Seed(s)).next_u64()).collect();
        let mut sorted = first.clone();
        sorted.sort_unstable();
        sorted.dedup();
        assert_eq!(sorted.len(), first.len(), "eight consecutive seeds, eight distinct first draws");
        // And they must not simply increase with the seed, which an LCG would.
        assert!(
            first.windows(2).any(|w| w[0] > w[1]),
            "a stream that only grows with the seed is not a stream, got {first:?}"
        );
    }

    #[test]
    fn below_stays_in_range_and_covers_it() {
        let mut rng = Rng::from_seed(Seed(7));
        let mut seen = [false; 5];
        for _ in 0..500 {
            let v = rng.below(5);
            assert!(v < 5, "below(5) returned {v}");
            seen[v as usize] = true;
        }
        assert!(seen.iter().all(|&s| s), "every value in range must be reachable");
    }

    #[test]
    fn seed_zero_is_a_usable_seed() {
        // A campaign starts at 0, and a PRNG whose zero state is absorbing would silently make the
        // first program of every campaign degenerate.
        let mut rng = Rng::from_seed(Seed(0));
        let draws: Vec<u64> = (0..4).map(|_| rng.next_u64()).collect();
        assert!(draws.iter().all(|&d| d != 0), "got {draws:?}");
    }

    // -- reproducibility ----------------------------------------------------------------------

    #[test]
    fn the_same_seed_gives_byte_identical_source() {
        for seed in [0, 1, 42, 9999, u64::MAX] {
            let a = program(seed).to_java();
            let b = program(seed).to_java();
            assert_eq!(a, b, "seed {seed} must be reproducible byte for byte");
        }
    }

    #[test]
    fn different_seeds_give_different_programs() {
        let sources: Vec<String> = (0..40).map(program).map(|p| p.to_java()).collect();
        let mut unique = sources.clone();
        unique.sort();
        unique.dedup();
        assert!(
            unique.len() > 35,
            "40 seeds produced only {} distinct programs — the generator is barely exploring",
            unique.len()
        );
    }

    // -- property 1: type-correct by construction ---------------------------------------------

    #[test]
    fn every_generated_program_is_well_formed() {
        for seed in 0..500 {
            let p = program(seed);
            if let Err(problem) = p.well_formed() {
                panic!("seed {seed} produced {problem:?}\n{}", p.to_java());
            }
        }
    }

    #[test]
    fn well_formed_actually_rejects_things() {
        // A checker that never says no proves nothing about the 500 programs above.
        let mut p = program(3);
        p.entry.result = Expr::Var("nosuchvariable".to_string(), Ty::Int);
        assert!(matches!(p.well_formed(), Err(Malformed::UnboundVariable(_))));

        let mut p = program(3);
        p.entry.result = Expr::LongLit(0);
        assert!(matches!(p.well_formed(), Err(Malformed::WrongReturnType(_))));

        let mut p = program(3);
        p.entry.result =
            Expr::Bin(BinOp::Add, Box::new(Expr::IntLit(1)), Box::new(Expr::LongLit(1)));
        assert!(matches!(p.well_formed(), Err(Malformed::MixedOperands(_))));

        let mut p = program(3);
        p.entry.body.push(Stmt::For { var: "iz".to_string(), bound: 0, body: vec![] });
        assert!(matches!(p.well_formed(), Err(Malformed::BadLoopBound(0))));

        let mut p = program(3);
        p.entry.body.push(Stmt::For {
            var: "iz".to_string(),
            bound: 3,
            body: vec![Stmt::Assign {
                name: "iz".to_string(),
                ty: Ty::Int,
                expr: Expr::IntLit(0),
            }],
        });
        assert!(
            matches!(p.well_formed(), Err(Malformed::AssignedLoopCounter(_))),
            "resetting the counter is how this grammar would stop terminating"
        );
    }

    // -- property 2: deterministic ------------------------------------------------------------

    #[test]
    fn nothing_non_deterministic_can_be_expressed() {
        // Enforced by absence, so the test is on the emitted text: if any of these ever appear, a
        // node was added to the AST that should not have been.
        //
        // `float` and `double` were on this list and are deliberately off it: Java has been
        // strictly IEEE since JEP 306 removed `strictfp`, so a floating computation has exactly one
        // right answer everywhere. What stays banned is everything that depends on *this* run — the
        // clock, a hash, an identity, an iteration order. See the module docs.
        let banned = [
            "hashCode",
            "currentTimeMillis",
            "nanoTime",
            "Random",
            "identityHashCode",
            "System.getenv",
            "System.getProperty",
            "Thread",
            "HashMap",
            "HashSet",
            "toString",
            // The NaN payload is the one part of IEEE Java does not pin down, so no generated
            // program may ever look at bits. It would also not resolve on this VM — see
            // `emit_classifier`.
            "ToRawIntBits",
            "ToLongBits",
            "ToIntBits",
            "bitsToDouble",
            "bitsToFloat",
            // Neither of these fields exists in `KajiLibrary/java/lang/Float.java`, so a program
            // naming one compiles against the reference JDK and fails on this VM alone.
            "Float.NaN",
            "Double.NaN",
            "POSITIVE_INFINITY",
            "NEGATIVE_INFINITY",
        ];
        for seed in 0..200 {
            let source = program(seed).to_java();
            for needle in banned {
                assert!(
                    !source.contains(needle),
                    "seed {seed} emitted {needle:?}, which the oracle cannot compare"
                );
            }
            // `new` deserves better than a substring ban now that arrays and objects are both in
            // the grammar.
            //
            // Objects *do* have identity, and identity is the one thing two runs of a program need
            // not agree on — so the rule cannot be "never allocate one". It is the sharper claim
            // that identity is never **observed**: the AST has no node that compares two
            // references, no `hashCode`, no `toString`, and [`Ty`] has no reference type, so there
            // is nothing an identity could flow into. What is left of an object is its fields and
            // which body a call dispatched to, both of which are functions of the program alone.
            //
            // The check that stays is therefore about *shape*: everything allocated is either a
            // primitive array or one of the four classes this file emits, so no third kind of
            // object can appear without this test being revisited.
            let class = format!("Fz{seed}");
            for allocation in source.split("new ").skip(1) {
                let ok = ["int[", "long[", "float[", "double["]
                    .iter()
                    .any(|k| allocation.starts_with(k))
                    || OBJ_CLASSES.iter().any(|c| {
                        allocation.starts_with(&format!("{class}{}(", c.suffix()))
                    });
                assert!(
                    ok,
                    "seed {seed} allocated something that is neither a primitive array nor a \
                     class of the hierarchy: new {}",
                    &allocation[..allocation.len().min(24)]
                );
            }
        }
    }

    // -- floating point (stage 1) --------------------------------------------------------------

    #[test]
    fn a_float_share_of_zero_gives_the_integral_grammar_back() {
        // The knob has to be a real switch, not a bias: `jit_coverage` measures "what did floating
        // point cost us?" by comparing the two settings, and that measurement only means something
        // if one of them is the grammar that was there before. `fp_share` gates the *narrowing
        // conversion* as well as the type draw — a `(int) someDouble` at share zero would be a
        // `double` sneaking in through the one door that does not consult the share.
        let integral = GenConfig { fp_share: 0, ..GenConfig::default() };
        for seed in 0..200 {
            let source = JavaGenerator::new(integral).generate(Seed(seed)).to_java();
            for needle in ["float", "double", "fcls", "dcls"] {
                assert!(!source.contains(needle), "seed {seed} emitted {needle:?} at fp_share 0");
            }
        }
    }

    #[test]
    fn floating_point_actually_shows_up() {
        let mut fp = 0;
        let mut classified = 0;
        let mut narrowed = 0;
        for seed in 0..200 {
            let source = program(seed).to_java();
            fp += usize::from(source.contains("float ") || source.contains("double "));
            classified += usize::from(source.contains("fcls(") || source.contains("dcls("));
            narrowed += usize::from(source.contains("((int)") || source.contains("((long)"));
        }
        assert!(fp > 100, "only {fp}/200 programs touched IEEE at all");
        assert!(classified > 20, "only {classified}/200 programs used the result channel");
        assert!(narrowed > 20, "only {narrowed}/200 programs narrowed anything");
    }

    #[test]
    fn the_float_pools_reach_every_ieee_corner() {
        // The pools are the whole bias argument for this stage, and a pool that quietly lost its
        // NaN would leave `fcmpl` versus `fcmpg` — the one place they differ — untested forever.
        let floats: Vec<f32> = FLOAT_POOL.iter().map(|&b| f32::from_bits(b)).collect();
        assert!(floats.iter().any(|f| f.is_nan()), "no NaN in FLOAT_POOL");
        assert!(floats.contains(&f32::INFINITY), "no +Infinity in FLOAT_POOL");
        assert!(floats.contains(&f32::NEG_INFINITY), "no -Infinity in FLOAT_POOL");
        assert!(floats.iter().any(|f| *f == 0.0 && f.is_sign_negative()), "no -0.0");
        assert!(floats.iter().any(|f| f.is_subnormal()), "no subnormal");
        let doubles: Vec<f64> = DOUBLE_POOL.iter().map(|&b| f64::from_bits(b)).collect();
        assert!(doubles.iter().any(|d| d.is_nan()), "no NaN in DOUBLE_POOL");
        assert!(doubles.iter().any(|d| *d == 0.0 && d.is_sign_negative()), "no -0.0");
        assert!(doubles.iter().any(|d| d.is_subnormal()), "no subnormal");
        // The saturation boundaries, which are the reason `(int)` is generated at all.
        assert!(doubles.contains(&2147483647.0), "Integer.MAX_VALUE is not a probe");
        assert!(doubles.contains(&2147483648.0), "one past it is where `(int)` must saturate");
        // Every pool entry must be distinct *as bits*, which is the point of storing bits: 0.0 and
        // -0.0 are `==` and would collapse under any value-based dedup.
        let mut bits = FLOAT_POOL.to_vec();
        bits.sort_unstable();
        bits.dedup();
        assert_eq!(bits.len(), FLOAT_POOL.len(), "FLOAT_POOL has a duplicate");
        let mut bits = DOUBLE_POOL.to_vec();
        bits.sort_unstable();
        bits.dedup();
        assert_eq!(bits.len(), DOUBLE_POOL.len(), "DOUBLE_POOL has a duplicate");
    }

    #[test]
    fn every_float_literal_round_trips_through_the_emitter() {
        // The emitter writes a decimal; `javac` reads it back. If the two disagree by one ulp, every
        // finding involving that constant is a lie — so the round trip is checked here rather than
        // trusted, using the same parser Java's grammar agrees with on these forms.
        for &b in FLOAT_POOL.iter().chain(FLOAT_PROBES) {
            let mut out = String::new();
            emit_float_lit(&mut out, b);
            let value = f32::from_bits(b);
            if value.is_nan() {
                assert_eq!(out, "(0.0f / 0.0f)");
                continue;
            }
            if value.is_infinite() {
                assert!(out.ends_with("1.0f / 0.0f)"), "got {out}");
                continue;
            }
            let text = out.strip_suffix('f').unwrap_or(&out);
            let parsed: f32 = text.parse().expect(&out);
            assert_eq!(parsed.to_bits(), b, "{out} does not read back as the value it was written from");
        }
        for &b in DOUBLE_POOL.iter().chain(DOUBLE_PROBES) {
            let mut out = String::new();
            emit_double_lit(&mut out, b);
            let value = f64::from_bits(b);
            if value.is_nan() || value.is_infinite() {
                assert!(out.contains("/ 0.0)"), "got {out}");
                continue;
            }
            let parsed: f64 = out.parse().expect(&out);
            assert_eq!(parsed.to_bits(), b, "{out} does not read back as the value it was written from");
        }
    }

    #[test]
    fn the_classifier_is_emitted_exactly_when_it_is_called() {
        // Both directions are failures with very different symptoms: a call without a declaration
        // is a compile error on every seed that draws one, and a declaration without a call puts
        // `float` into the text of programs the reducer has just finished stripping it out of.
        let mut with = 0;
        for seed in 0..200 {
            let source = program(seed).to_java();
            for ty in [Ty::Float, Ty::Double] {
                let name = classifier_name(ty);
                let declared = source.contains(&format!("static int {name}("));
                let calls =
                    source.matches(&format!("{name}(")).count() - usize::from(declared);
                assert_eq!(
                    declared,
                    calls > 0,
                    "seed {seed}: {name} declared={declared}, called {calls} times"
                );
                with += usize::from(declared);
            }
        }
        assert!(with > 0, "no seed in 200 used the floating result channel at all");
    }

    #[test]
    fn no_integral_only_operator_ever_reaches_a_floating_operand() {
        // `1.0 & 2.0`, `1.0 << 2` and `~1.0` are all `javac` rejections, i.e. generator bugs. This
        // is checked on the AST rather than the text because `&` and `|` also appear inside `&&`
        // and `||`, which are perfectly legal on the *conditions* a floating comparison produces.
        fn walk(e: &Expr, seed: u64) {
            match e {
                Expr::Bin(op, a, b) => {
                    assert!(op.accepts(a.ty()), "seed {seed}: {op:?} on {:?}", a.ty());
                    walk(a, seed);
                    walk(b, seed);
                }
                Expr::Shift(_, v, n) => {
                    assert!(!v.ty().is_fp(), "seed {seed}: shift of a {:?}", v.ty());
                    walk(v, seed);
                    walk(n, seed);
                }
                Expr::Not(a) => {
                    assert!(!a.ty().is_fp(), "seed {seed}: `~` on a {:?}", a.ty());
                    walk(a, seed);
                }
                Expr::Neg(a) | Expr::Cast(_, a) => walk(a, seed),
                Expr::Str(_, _) => {}
                Expr::Narrow(_, a) => {
                    assert_eq!(a.ty(), Ty::Int, "seed {seed}: narrowing of a {:?}", a.ty());
                    walk(a, seed);
                }
                Expr::Classify(a) => {
                    assert!(a.ty().is_fp(), "seed {seed}: classify of a {:?}", a.ty());
                    walk(a, seed);
                }
                Expr::Ternary(_, a, b) => {
                    walk(a, seed);
                    walk(b, seed);
                }
                Expr::Call(_, args, _) => args.iter().for_each(|a| walk(a, seed)),
                _ => {}
            }
        }
        for seed in 0..300 {
            let p = program(seed);
            for m in p.methods.iter().chain([&p.entry]) {
                walk(&m.result, seed);
            }
        }
    }

    #[test]
    fn well_formed_rejects_the_new_ways_to_be_wrong() {
        // Wrapped in a cast because the entry method returns `int` and the return-type check would
        // otherwise fire first, hiding the rule actually under test.
        let mut p = program(3);
        p.entry.result = Expr::Cast(
            Ty::Int,
            Box::new(Expr::Bin(
                BinOp::And,
                Box::new(Expr::DoubleLit(0)),
                Box::new(Expr::DoubleLit(0)),
            )),
        );
        assert!(
            matches!(p.well_formed(), Err(Malformed::IntegralOperatorOnFloat(_))),
            "`double & double` must be rejected"
        );

        let mut p = program(3);
        p.entry.result = Expr::Cast(
            Ty::Int,
            Box::new(Expr::Shift(
                ShiftOp::Left,
                Box::new(Expr::DoubleLit(0)),
                Box::new(Expr::IntLit(1)),
            )),
        );
        assert!(matches!(p.well_formed(), Err(Malformed::IntegralOperatorOnFloat(_))));

        let mut p = program(3);
        p.entry.result = Expr::Classify(Box::new(Expr::IntLit(1)));
        assert!(matches!(p.well_formed(), Err(Malformed::ClassifyOnNonFloat(Ty::Int))));
    }

    // -- arrays (stage 2) ----------------------------------------------------------------------

    /// Stage 5, and the property that matters is not that the constructs appear but that
    /// **`break`/`continue` only ever appear inside a loop** — outside one they are a `javac`
    /// error, so a generator bug there would show up as a wave of unusable seeds rather than as a
    /// finding. The rest is the usual on/off contract.
    #[test]
    fn stage_five_generates_loops_jumps_and_throws_where_they_are_legal() {
        fn scan(
            block: &Block,
            depth: u32,
            whiles: &mut usize,
            jumps: &mut usize,
            throws: &mut usize,
            stray: &mut usize,
        ) {
            for stmt in block {
                match stmt {
                    Stmt::While { body, .. } => {
                        *whiles += 1;
                        scan(body, depth + 1, whiles, jumps, throws, stray);
                    }
                    Stmt::For { body, .. } => scan(body, depth + 1, whiles, jumps, throws, stray),
                    Stmt::Break | Stmt::Continue => {
                        *jumps += 1;
                        if depth == 0 {
                            *stray += 1;
                        }
                    }
                    Stmt::Throw(_) => *throws += 1,
                    Stmt::If { then, otherwise, .. } => {
                        scan(then, depth, whiles, jumps, throws, stray);
                        scan(otherwise, depth, whiles, jumps, throws, stray);
                    }
                    Stmt::Switch { arms, default, .. } => {
                        for arm in arms {
                            scan(&arm.body, depth, whiles, jumps, throws, stray);
                        }
                        if let Some(b) = default {
                            scan(b, depth, whiles, jumps, throws, stray);
                        }
                    }
                    _ => {}
                }
            }
        }

        // El `switch` tambien apagado, y no para que pase el test: un brazo que corta emite su
        // propio `break;`, asi que con switches encendidos esa aguja deja de ser la firma de los
        // saltos. Apagar un eje para poder afirmar algo del otro es lo que mantiene la afirmacion.
        let off = GenConfig {
            while_share: 0,
            jump_share: 0,
            throw_share: 0,
            switch_share: 0,
            ..GenConfig::default()
        };
        for seed in 0..200 {
            let source = JavaGenerator::new(off).generate(Seed(seed)).to_java();
            for needle in ["while (", "break;", "continue;", "throw new"] {
                assert!(!source.contains(needle), "seed {seed} emitio {needle} con las perillas en 0");
            }
        }

        let on = GenConfig {
            while_share: 40,
            jump_share: 25,
            throw_share: 20,
            ..GenConfig::default()
        };
        let (mut whiles, mut jumps, mut throws, mut stray) = (0, 0, 0, 0);
        for seed in 0..300 {
            let program = JavaGenerator::new(on).generate(Seed(seed));
            scan(&program.entry.body, 0, &mut whiles, &mut jumps, &mut throws, &mut stray);
            for m in &program.methods {
                scan(&m.body, 0, &mut whiles, &mut jumps, &mut throws, &mut stray);
            }
        }
        assert!(whiles > 10, "while en {whiles} lugares");
        assert!(jumps > 10, "break/continue en {jumps} lugares");
        assert!(throws > 10, "throw en {throws} lugares");
        assert_eq!(stray, 0, "{stray} break/continue fuera de un bucle");
    }

    /// A `switch` has three properties worth pinning, and none of them is "it appears".
    ///
    /// **Dense and scattered both**, because the label layout is what decides whether `javac` emits
    /// a `tableswitch` or a `lookupswitch`, and those are different opcodes with different
    /// decoders. **Fall-through**, because it is the one thing here the opcodes do not encode — the
    /// compiler lays the arms out so control runs from one into the next. And **arms that are
    /// actually reached**, since a selector that never matches a label turns the whole construct
    /// into `default` with extra steps.
    ///
    /// Read off the tree rather than the text: `break` is easy to grep for and easy to be wrong
    /// about, and the tree is what the reducer manipulates anyway.
    #[test]
    fn a_switch_is_generated_dense_scattered_and_falling_through() {
        fn scan(block: &Block, dense: &mut usize, sparse: &mut usize, falls: &mut usize) {
            for stmt in block {
                match stmt {
                    Stmt::Switch { arms, default, .. } => {
                        let mut labels: Vec<i32> = arms.iter().map(|a| a.label).collect();
                        labels.sort_unstable();
                        let consecutive = labels.windows(2).all(|w| w[1] == w[0] + 1);
                        if consecutive {
                            *dense += 1;
                        } else {
                            *sparse += 1;
                        }
                        if arms.iter().any(|a| !a.breaks) {
                            *falls += 1;
                        }
                        for arm in arms {
                            scan(&arm.body, dense, sparse, falls);
                        }
                        if let Some(body) = default {
                            scan(body, dense, sparse, falls);
                        }
                    }
                    Stmt::If { then, otherwise, .. } => {
                        scan(then, dense, sparse, falls);
                        scan(otherwise, dense, sparse, falls);
                    }
                    Stmt::For { body, .. } => scan(body, dense, sparse, falls),
                    _ => {}
                }
            }
        }

        let off = GenConfig { switch_share: 0, ..GenConfig::default() };
        for seed in 0..200 {
            let source = JavaGenerator::new(off).generate(Seed(seed)).to_java();
            assert!(!source.contains("switch ("), "seed {seed} emitted a switch at share 0");
        }

        let on = GenConfig { switch_share: 45, ..GenConfig::default() };
        let (mut dense, mut sparse, mut falls) = (0, 0, 0);
        for seed in 0..300 {
            let program = JavaGenerator::new(on).generate(Seed(seed));
            scan(&program.entry.body, &mut dense, &mut sparse, &mut falls);
            for m in &program.methods {
                scan(&m.body, &mut dense, &mut sparse, &mut falls);
            }
        }
        assert!(dense > 10, "switch denso (tableswitch) en {dense} lugares");
        assert!(sparse > 10, "switch disperso (lookupswitch) en {sparse} lugares");
        assert!(falls > 10, "fall-through en {falls} lugares");
    }

    /// Same contract as the string test, and the same reason. A narrowing that never appears is
    /// a campaign measuring nothing, and it would look exactly like a campaign that passed.
    #[test]
    fn a_narrowing_share_turns_the_round_trips_on_and_off() {
        let none = GenConfig { narrowing_share: 0, ..GenConfig::default() };
        for seed in 0..200 {
            let source = JavaGenerator::new(none).generate(Seed(seed)).to_java();
            for needle in ["(byte)", "(short)", "(char)"] {
                assert!(!source.contains(needle), "seed {seed} emitted {needle} at share 0");
            }
        }
        let lots = GenConfig { narrowing_share: 60, ..GenConfig::default() };
        let (mut byte, mut short, mut ch) = (0, 0, 0);
        for seed in 0..300 {
            let source = JavaGenerator::new(lots).generate(Seed(seed)).to_java();
            if source.contains("(byte)") {
                byte += 1;
            }
            if source.contains("(short)") {
                short += 1;
            }
            // The one worth counting separately: `i2c` zero-extends where the other two carry the
            // sign, so a run without it has not asked the question this stage is for.
            if source.contains("(char)") {
                ch += 1;
            }
        }
        assert!(byte > 10, "(byte) en {byte}/300 seeds");
        assert!(short > 10, "(short) en {short}/300 seeds");
        assert!(ch > 10, "(char) en {ch}/300 seeds");
    }

    /// The counterpart of the array test, and the reason it exists: a campaign that reports zero
    /// divergences over a grammar that never produced the construct is FZ-004 with a different
    /// disguise. This pins that `string_share` both **fires** and **stays off** when it is zero.
    #[test]
    fn a_string_share_turns_strings_on_and_off() {
        // Off: not a single quoted literal outside the fixed `main(String[] a)` signature.
        let none = GenConfig { string_share: 0, ..GenConfig::default() };
        for seed in 0..200 {
            let source = JavaGenerator::new(none).generate(Seed(seed)).to_java();
            let body = source.replace("main(String[] a)", "");
            assert!(!body.contains('\"'), "seed {seed} emitted a string literal at share 0");
        }
        // On: the three shapes the stage exists to exercise all appear across a few hundred seeds.
        let lots = GenConfig { string_share: 60, ..GenConfig::default() };
        let (mut literal, mut concat, mut fresh, mut equals, mut same) = (0, 0, 0, 0, 0);
        for seed in 0..300 {
            let source = JavaGenerator::new(lots).generate(Seed(seed)).to_java();
            let body = source.replace("main(String[] a)", "");
            if body.contains('\"') { literal += 1; }
            if body.contains("\" + \"") { concat += 1; }
            if body.contains("new String(") { fresh += 1; }
            if body.contains(".equals(") { equals += 1; }
            if body.contains(" == \"") || body.contains("\") == ") { same += 1; }
        }
        assert!(literal > 100, "literales en {literal}/300 seeds");
        assert!(concat > 10, "concatenaciones en {concat}/300");
        assert!(fresh > 10, "new String en {fresh}/300");
        assert!(equals > 10, "equals en {equals}/300");
        assert!(same > 10, "identidad en {same}/300");
    }

    /// `interface_share` both fires and stays off — and, when it fires, produces the *call*, not
    /// just the declaration.
    ///
    /// The declaration alone would be worthless: the whole point is a call site whose receiver's
    /// **declared** type is the interface, because that is the only thing that decides whether
    /// `javac` writes `invokevirtual` or `invokeinterface`. The class-file side of the same claim
    /// is `fuzz::campaigns::jit_coverage::interface_calls_actually_reach_the_class_file`; this half
    /// is the one that runs without a toolchain.
    #[test]
    fn an_interface_share_turns_interface_dispatch_on_and_off() {
        // Off: no interface-typed declaration anywhere. The interface *type* is still emitted with
        // the hierarchy — it costs nothing and keeps the shape of the source stable — so what is
        // asserted is that nothing is ever declared with it.
        let none = GenConfig { interface_share: 0, ..GenConfig::default() };
        for seed in 0..200 {
            let source = JavaGenerator::new(none).generate(Seed(seed)).to_java();
            let prefix = source
                .split_once("interface ")
                .map(|(_, rest)| rest.split_once('I').expect("hierarchy prefix").0.to_string())
                .expect("the hierarchy always declares the interface");
            assert!(
                !source.contains(&format!("{prefix}I q")),
                "seed {seed} declared an interface-typed local at share 0"
            );
        }

        // On: declared, called, and reassigned — the third one is what makes an inline cache miss
        // instead of settling, so it is worth knowing it happens rather than hoping.
        // Interface locals are the `q`-prefixed names, but the counter behind [`Gen::fresh`] is
        // shared across every prefix, so they are `q7`/`q13` and not `q0`/`q1`. Reading them out of
        // the source is what keeps this test measuring the calls rather than the numbering.
        fn names(source: &str) -> Vec<String> {
            source
                .match_indices("I q")
                .map(|(i, _)| {
                    let rest = &source[i + 2..];
                    let end = rest.find(|c: char| !c.is_ascii_digit() && c != 'q').unwrap_or(1);
                    rest[..end].to_string()
                })
                .collect()
        }

        let lots = GenConfig { interface_share: 60, ..GenConfig::default() };
        let (mut declared, mut called, mut reassigned) = (0, 0, 0);
        for seed in 0..300 {
            let source = JavaGenerator::new(lots).generate(Seed(seed)).to_java();
            if source.contains("I q") {
                declared += 1;
            }
            if names(&source).iter().any(|n| source.contains(&format!("{n}.v()"))) {
                called += 1;
            }
            // `> 1` and not `>= 1`: the declaration itself reads `…I q7 = new …`, so one
            // occurrence is the declaration and only a second one is a reassignment.
            if names(&source)
                .iter()
                .any(|n| source.matches(&format!("{n} = new ")).count() > 1)
            {
                reassigned += 1;
            }
        }
        println!("declarados {declared}/300, llamados {called}/300, reasignados {reassigned}/300");
        assert!(declared > 100, "locales de interfaz en {declared}/300 seeds");
        assert!(called > 30, "llamadas por interfaz en {called}/300 seeds");
        assert!(reassigned > 5, "reasignaciones de un local de interfaz en {reassigned}/300");
    }

    /// The parallel site is generated, complete, and **off** when the knob is zero.
    ///
    /// "0 divergences" over a construct that never appeared is the failure this project has now
    /// hit twice (FZ-004, FZ-005), and threads are the easiest place yet to hit it a third time: a
    /// concurrent campaign that never spawned a thread looks exactly like one that did and found
    /// nothing. So every piece of the shape is asserted separately — a `start()` without a `join()`
    /// would be a race rather than a probe, and a `join()` without a reduction would be a thread
    /// whose answer nobody reads.
    #[test]
    fn the_parallel_site_is_whole_when_it_is_on_and_absent_when_it_is_off() {
        const K: usize = 4;

        // Off: not a thread anywhere. `Thread` alone would be too weak a needle — it is a word —
        // so the pieces that only this construct emits are checked too.
        let none = GenConfig { workers: 0, ..GenConfig::default() };
        for seed in 0..120 {
            let source = JavaGenerator::new(none).generate(Seed(seed)).to_java();
            for needle in ["extends Thread", ".start()", ".join()", "W(int[] s"] {
                assert!(!source.contains(needle), "seed {seed} emitted {needle:?} at workers 0");
            }
        }

        // On: every piece, on every seed. Not "most" — the site is *planted*, not rolled, so a
        // seed without it is a bug and an assertion that tolerated one would hide it.
        let lots = GenConfig { workers: K, ..GenConfig::default() };
        for seed in 0..120 {
            let program = JavaGenerator::new(lots).generate(Seed(seed));
            assert!(program.well_formed().is_ok(), "seed {seed}: {:?}", program.well_formed());
            let source = program.to_java();
            for needle in [
                "extends Thread",
                ".start();",
                ".join();",
                "catch (InterruptedException e) { }",
                &format!("new int[{K}]"),
                &format!("case {}: {{", K - 1),
                "s[k] = v;",
            ] {
                assert!(source.contains(needle), "seed {seed} is missing {needle:?}");
            }
        }

        // **The number this level actually turns on.** A worker that only does `int` arithmetic
        // allocates nothing, so K threads of it put no pressure at all on the collector — and the
        // bug the level exists to hunt (FZ-002: a stale reference under a collection with real
        // parallelism) cannot happen without threads that allocate while others hold references.
        // Counted over seeds rather than required on each, because allocation is share-driven.
        let mut allocating = 0;
        for seed in 0..120 {
            let source = JavaGenerator::new(lots).generate(Seed(seed)).to_java();
            let Some((_, worker)) = source.split_once("extends Thread {") else {
                panic!("seed {seed}: no worker class");
            };
            if worker.contains("new ") {
                allocating += 1;
            }
        }
        assert!(
            allocating > 60,
            "only {allocating}/120 seeds allocate inside a worker — K threads of pure arithmetic \
             make the collector do nothing, which is the one thing this level is for"
        );

        // And the half that decides whether any of it means anything: the reduced value has to
        // reach the number the executor observes. A site whose result is computed and dropped is
        // coverage that cannot fail — FZ-004's mistake, and the reason `finish_method` folds the
        // accumulator into the result rather than hoping the generator reads it.
        for seed in 0..40 {
            let program = JavaGenerator::new(lots).generate(Seed(seed));
            let Some(Stmt::Fork { acc, .. }) =
                program.entry.body.iter().find(|s| matches!(s, Stmt::Fork { .. }))
            else {
                panic!("seed {seed}: the entry method has no parallel site");
            };
            let mut result = String::new();
            emit_expr(&mut result, &program.entry.result);
            assert!(
                result.contains(acc.as_str()),
                "seed {seed}: the threads' result never reaches the return value: {result}"
            );
        }
    }

    /// The total wrapper catches **per iteration**, not around the whole warm-up loop.
    ///
    /// This is one line of emission and it was worth 5 to 41 seeds out of 80. Around the loop, one
    /// throw on iteration 1 ends the program: the remaining iterations never run, the JIT never
    /// crosses its threshold, and the campaign compares two engines on a program that did nothing.
    /// Measured across the whole census, moving the catch inside took every configuration to
    /// 78–80 of 80 entering native code, from 58–75.
    ///
    /// Pinned structurally rather than by the count, because the count is the *consequence*: what
    /// has to hold is that the `try` is inside the `for` and that the caught value reaches the
    /// accumulator. A wrapper that caught per iteration and then discarded `r` would score the same
    /// on coverage and observe nothing.
    #[test]
    fn the_total_wrapper_catches_once_per_iteration_and_keeps_going() {
        let source = JavaGenerator::default().generate(Seed(4)).to_java();
        let run = source
            .split_once("static int run() {")
            .expect("the entry wrapper")
            .1
            .split_once("\n    }")
            .expect("its closing brace")
            .0;

        let for_at = run.find("for (int w = 0;").expect("the warm-up loop");
        let try_at = run.find("try {").expect("the try");
        assert!(
            for_at < try_at,
            "the `try` is outside the loop again, so one throw ends the program:\n{run}"
        );
        assert!(
            run.contains("acc = ((acc * 31) + r);"),
            "the caught value never reaches the accumulator, so a throw is unobservable:\n{run}"
        );
        // And the marks are still all of them: totality (property 4) is what makes the executor's
        // single `int` enough, and a catch that missed one would let an exception escape instead.
        for needle in [
            "catch (ArithmeticException e)",
            "catch (ArrayIndexOutOfBoundsException e)",
            "catch (NegativeArraySizeException e)",
            "catch (NullPointerException e)",
            "catch (Throwable t)",
        ] {
            assert!(run.contains(needle), "the wrapper lost {needle:?}:\n{run}");
        }
    }

    /// `cast_share` turns the type probes on and off, and when on both shapes appear.
    ///
    /// Both, separately: `instanceof` can never fail, so on its own it exercises the guard and
    /// never the path out of it — which is the half that becomes a **deopt** in compiled code and
    /// a `ClassCastException` in the interpreter. A census that only counted "a type probe
    /// appeared" would be satisfied by the harmless half.
    #[test]
    fn a_cast_share_turns_the_type_probes_on_and_off() {
        let none = GenConfig { cast_share: 0, ..GenConfig::default() };
        for seed in 0..150 {
            let source = JavaGenerator::new(none).generate(Seed(seed)).to_java();
            for needle in [" instanceof ", ") o", "int y"] {
                let _ = needle;
            }
            assert!(!source.contains(" instanceof "), "seed {seed} emitted instanceof at share 0");
        }

        let lots = GenConfig { cast_share: 40, ..GenConfig::default() };
        let (mut test, mut cast, mut both) = (0, 0, 0);
        for seed in 0..200 {
            let program = JavaGenerator::new(lots).generate(Seed(seed));
            assert!(program.well_formed().is_ok(), "seed {seed}: {:?}", program.well_formed());
            let source = program.to_java();
            let t = source.contains(" instanceof ");
            // A cast reads a field through a parenthesised class name: `(((Fz0S1) o5).a)`.
            let c = source.contains(") o") && source.contains(").a);");
            if t { test += 1; }
            if c { cast += 1; }
            if t && c { both += 1; }
        }
        println!("instanceof {test}/200, cast {cast}/200, ambos {both}/200");
        // **The bar is what the knob can actually deliver, measured.** At a share of 40 both land
        // near 27/200, and raising the share does not move it much: the ceiling is set by how often
        // an *object statement* is drawn at all, which is `object_share`'s business and not this
        // knob's. A floor of 15 catches the construct disappearing without pretending the knob
        // controls something it does not.
        assert!(test > 15, "instanceof en {test}/200");
        assert!(cast > 15, "cast en {cast}/200");

        // And the catch that makes the failing cast *observable* rather than lumped in with
        // everything else: `marks::CLASS_CAST` was in the table and unreachable until now.
        let source = JavaGenerator::new(lots).generate(Seed(0)).to_java();
        assert!(
            source.contains(&format!("catch (ClassCastException e) {{ r = {}; }}", marks::CLASS_CAST)),
            "a failing cast would arrive as OTHER, indistinguishable from any other bug"
        );
    }

    /// `ref_field_share` turns the reference field on and off, and when it is on all three shapes
    /// appear — the store, the chained read, and the `null` store.
    ///
    /// The three are asserted separately because they fail separately. A **store** with no read is
    /// an edge nobody observes; a **read** with no store is a chain that is always `null`, which
    /// tests the deopt and nothing else; and without the **`null` store** the receiver of a chained
    /// read is only ever null on a fresh object, so the transition live-then-null never happens.
    #[test]
    fn a_ref_field_share_turns_the_reference_field_on_and_off() {
        // Off: the field is not even declared, so nothing can name it.
        let none = GenConfig { ref_field_share: 0, ..GenConfig::default() };
        for seed in 0..200 {
            let source = JavaGenerator::new(none).generate(Seed(seed)).to_java();
            for needle in ["B c;", ".c = ", ".c."] {
                assert!(!source.contains(needle), "seed {seed} emitted {needle:?} at share 0");
            }
        }

        let lots = GenConfig { ref_field_share: 45, ..GenConfig::default() };
        let (mut store, mut read, mut declared, mut null_store) = (0, 0, 0, 0);
        for seed in 0..200 {
            let program = JavaGenerator::new(lots).generate(Seed(seed));
            assert!(program.well_formed().is_ok(), "seed {seed}: {:?}", program.well_formed());
            // The constructor's own `this.c = this;` is removed first. Counting it would have made
            // this number 116/200 the moment the initialiser landed, which is the same measurement
            // bug as counting a declaration as a reassignment.
            let source = program.to_java().replace("this.c = this;", "");
            if source.contains("B c;") { declared += 1; }
            if source.contains(".c = ") { store += 1; }
            if source.contains(".c.") { read += 1; }
            if source.contains(".c = null;") { null_store += 1; }
        }
        println!(
            "campo {declared}/200, store {store}/200, read {read}/200, null {null_store}/200"
        );
        assert!(declared > 80, "el campo se declara en {declared}/200");
        assert!(store > 25, "`putfield` de referencia en {store}/200");
        assert!(read > 60, "lectura encadenada en {read}/200");
        assert!(null_store > 5, "store de null en {null_store}/200");

        // And the half that keeps the field honest: it is declared **only** when something uses it.
        // A field nobody reads is one the reducer cannot delete and a human has to rule out.
        for seed in 0..200 {
            let source = JavaGenerator::new(lots).generate(Seed(seed)).to_java();
            if source.contains("B c;") {
                assert!(
                    source.replace("this.c = this;", "").contains(".c"),
                    "seed {seed} declares the field and never touches it"
                );
            }
        }
    }

    #[test]
    fn an_array_share_of_zero_gives_the_grammar_without_arrays_back() {
        // Objects off as well, and not to make the test pass: `new ` and a bracket are the
        // *arrays'* signature, and with objects on they are no longer only that. Turning one axis
        // off to make a claim about the other is what keeps the claim about arrays.
        let flat = GenConfig {
            array_share: 0,
            object_share: 0,
            dispatch_probe: false,
            ..GenConfig::default()
        };
        for seed in 0..200 {
            let source = JavaGenerator::new(flat).generate(Seed(seed)).to_java();
            // Not `[`: `main(String[] a)` has one, and always did.
            for needle in ["new ", ".length"] {
                assert!(!source.contains(needle), "seed {seed} emitted {needle:?} at array_share 0");
            }
        }
    }

    #[test]
    fn arrays_actually_show_up_in_every_shape_that_matters() {
        let (mut allocated, mut stored, mut loaded, mut length) = (0, 0, 0, 0);
        let (mut negative, mut empty) = (0, 0);
        for seed in 0..300 {
            let p = program(seed);
            let source = p.to_java();
            let mut lens = Vec::new();
            collect_array_lengths(&p, &mut lens);
            // Counted on the AST, not as `" = new "` in the text. That substring used to mean "an
            // array was allocated" and stopped meaning it the moment objects joined the grammar:
            // it now matches `Fz7B o0 = new Fz7S1(3)` too, so a version of this file that emitted
            // no arrays at all would have gone on passing.
            allocated += usize::from(!lens.is_empty());
            stored += usize::from(count_array_stores(&p) > 0);
            loaded += usize::from(count_array_loads(&p) > 0);
            length += usize::from(source.contains(".length"));
            negative += usize::from(lens.iter().any(|&l| l < 0));
            empty += usize::from(lens.contains(&0));
        }
        // The thresholds are below what the grammar actually produces, measured: 252 allocate, 123
        // store, 87 read and 77 ask for a length. Objects cost the last three about a sixth
        // between them (149, 92 and one more allocation with `object_share: 0`), which is the
        // expression grammar being shared out rather than anything going wrong — but it is written
        // down here so the next arm that takes a bite out of arrays has to say so too.
        assert!(allocated > 100, "only {allocated}/300 programs allocated an array");
        assert!(stored > 40, "only {stored}/300 programs stored into one");
        assert!(loaded > 60, "only {loaded}/300 programs read one");
        assert!(length > 20, "only {length}/300 programs asked for a length");
        // The two allocation failures. Neither is common, and neither may be absent: they are the
        // whole reason `marks::NEGATIVE_SIZE` exists.
        assert!(negative > 5, "only {negative}/300 programs tried a negative length");
        assert!(empty > 5, "only {empty}/300 programs allocated a zero-length array");
    }

    fn count_array_stores(p: &JavaProgram) -> usize {
        fn in_block(b: &Block) -> usize {
            b.iter()
                .map(|s| match s {
                    Stmt::ArrayStore { .. } => 1,
                    Stmt::If { then, otherwise, .. } => in_block(then) + in_block(otherwise),
                    Stmt::For { body, .. } => in_block(body),
                    _ => 0,
                })
                .sum()
        }
        p.methods.iter().chain([&p.entry]).map(|m| in_block(&m.body)).sum()
    }

    /// Counted on the AST, because `[` in the text is also `String[] a` in `main`'s signature.
    fn count_array_loads(p: &JavaProgram) -> usize {
        fn in_expr(e: &Expr) -> usize {
            match e {
                Expr::ArrayLoad(_, _, i) => 1 + in_expr(i),
                Expr::Neg(a) | Expr::Not(a) | Expr::Cast(_, a) | Expr::Classify(a) => in_expr(a),
                Expr::Bin(_, a, b) | Expr::Shift(_, a, b) | Expr::Ternary(_, a, b) => {
                    in_expr(a) + in_expr(b)
                }
                Expr::Call(_, args, _) => args.iter().map(in_expr).sum(),
                _ => 0,
            }
        }
        fn in_block(b: &Block) -> usize {
            b.iter()
                .map(|s| match s {
                    Stmt::Declare { init, .. } => in_expr(init),
                    Stmt::Assign { expr, .. } => in_expr(expr),
                    Stmt::If { then, otherwise, .. } => in_block(then) + in_block(otherwise),
                    Stmt::Switch { selector, arms, default } => {
                        in_expr(selector)
                            + arms.iter().map(|x| in_block(&x.body)).sum::<usize>()
                            + default.as_ref().map_or(0, in_block)
                    }
                    Stmt::While { body, .. } => in_block(body),
                    Stmt::Break | Stmt::Continue | Stmt::Throw(_) => 0,
                    Stmt::For { body, .. } => in_block(body),
                    Stmt::ArrayStore { index, value, .. } => in_expr(index) + in_expr(value),
                    Stmt::NewObject { arg, .. } | Stmt::SetObject { arg, .. } => in_expr(arg),
                    Stmt::FieldStore { value, .. } => in_expr(value),
                    Stmt::Fork { args, bodies, .. } => {
                        in_expr(&args.0)
                            + in_expr(&args.1)
                            + bodies
                                .iter()
                                .map(|w| in_expr(&w.result) + in_block(&w.block))
                                .sum::<usize>()
                    }
                    Stmt::NewArray { .. } | Stmt::RefStore { .. } | Stmt::TypeProbe { .. } => 0,
                })
                .sum()
        }
        p.methods
            .iter()
            .chain([&p.entry])
            .map(|m| in_block(&m.body) + in_expr(&m.result))
            .sum()
    }

    fn collect_array_lengths(p: &JavaProgram, out: &mut Vec<i32>) {
        fn walk(block: &Block, out: &mut Vec<i32>) {
            for stmt in block {
                match stmt {
                    Stmt::NewArray { len, .. } => out.push(*len),
                    Stmt::If { then, otherwise, .. } => {
                        walk(then, out);
                        walk(otherwise, out);
                    }
                    Stmt::For { body, .. } => walk(body, out),
                    _ => {}
                }
            }
        }
        for m in p.methods.iter().chain([&p.entry]) {
            walk(&m.body, out);
        }
    }

    #[test]
    fn an_array_length_is_always_a_literal_and_always_small() {
        // The same rule a loop bound lives by, for the same reason plus one: the JIT only allocates
        // inline below a byte ceiling, and a length out of the expression grammar could be
        // `Integer.MAX_VALUE` — which is a heap exhaustion on one engine and something else on the
        // other, a divergence about the machine rather than about the VM.
        let max = GenConfig::default().max_array_len;
        for seed in 0..300 {
            let mut lens = Vec::new();
            collect_array_lengths(&program(seed), &mut lens);
            for len in lens {
                assert!(len >= -3 && len <= max, "seed {seed} allocated a length of {len}");
            }
        }
    }

    #[test]
    fn well_formed_rejects_the_array_mistakes_the_reducer_will_make() {
        // Every one of these is a candidate `Pass::DeleteStatement` produces the moment it removes
        // a `new`, so the checker is what stands between the reducer and a campaign of compile
        // errors.
        let mut p = program(3);
        p.entry.result = Expr::ArrayLoad("a99".into(), Ty::Int, Box::new(Expr::IntLit(0)));
        assert!(matches!(p.well_formed(), Err(Malformed::NotAnArray(_))));

        let mut p = program(3);
        p.entry.result = Expr::ArrayLength("a99".into());
        assert!(matches!(p.well_formed(), Err(Malformed::NotAnArray(_))));

        // An array declared as `long[]` but read as `int[]`.
        let mut p = program(3);
        p.entry.body = vec![Stmt::NewArray { name: "a0".into(), elem: Ty::Long, len: 2 }];
        p.entry.result = Expr::ArrayLoad("a0".into(), Ty::Int, Box::new(Expr::IntLit(0)));
        assert!(matches!(p.well_formed(), Err(Malformed::ArrayElementType { .. })));

        // A `long` index. Java has no such subscript.
        let mut p = program(3);
        p.entry.body = vec![Stmt::NewArray { name: "a0".into(), elem: Ty::Int, len: 2 }];
        p.entry.result = Expr::ArrayLoad("a0".into(), Ty::Int, Box::new(Expr::LongLit(0)));
        assert!(matches!(p.well_formed(), Err(Malformed::BadArrayIndex(Ty::Long))));

        // And the reverse confusion: an array name used where a scalar is wanted. `a0 + 1` is not
        // a program, and the reducer reaches this by substituting a variable of the same name.
        let mut p = program(3);
        p.entry.body = vec![Stmt::NewArray { name: "a0".into(), elem: Ty::Int, len: 2 }];
        p.entry.result = Expr::Var("a0".into(), Ty::Int);
        assert!(matches!(p.well_formed(), Err(Malformed::WrongType { .. })));
    }

    #[test]
    fn an_array_never_outlives_the_call_that_made_it() {
        // The purity argument FZ-004's warm-up loop rests on: `run()` calls the entry method 40
        // times and needs all 40 to compute the same thing. An array reachable from one call to
        // the next would break that silently — so every `new` must be a *local declaration*, and
        // the class must have no fields at all.
        for seed in 0..200 {
            let source = program(seed).to_java();
            for line in source.lines() {
                let trimmed = line.trim();
                let is_field = trimmed.starts_with("static ")
                    && !trimmed.contains('(')
                    && trimmed.ends_with(';');
                assert!(!is_field, "seed {seed} declared a static field: {trimmed}");
            }
        }
    }

    #[test]
    fn a_floating_divisor_is_never_zero_guarded() {
        // `1.0 / 0.0` is `Infinity` and `0.0 % 0.0` is `NaN`. Guarding either would suppress the
        // two results this stage exists to reach, and neither throws anything a guard would save.
        //
        // Matched on the exact shape `guard_zero` builds, not on "is a ternary": a ternary can
        // perfectly well be a divisor by chance, and a test that called that a failure would be
        // testing the RNG.
        fn is_zero_guard(e: &Expr) -> bool {
            let Expr::Ternary(cond, then, otherwise) = e else {
                return false;
            };
            let Cond::Cmp(CmpOp::Eq, left, right) = &**cond else {
                return false;
            };
            *right == Expr::zero(left.ty()) && **then != Expr::zero(left.ty()) && left == &**otherwise
        }
        for seed in 0..300 {
            let p = program(seed);
            for m in p.methods.iter().chain([&p.entry]) {
                let mut stack = vec![&m.result];
                while let Some(e) = stack.pop() {
                    if let Expr::Bin(op, _, b) = e {
                        assert!(
                            !(op.traps_on_zero() && e.ty().is_fp() && is_zero_guard(b)),
                            "seed {seed} guarded a floating divisor"
                        );
                    }
                    match e {
                        Expr::Neg(a) | Expr::Not(a) | Expr::Cast(_, a) | Expr::Classify(a) => {
                            stack.push(a)
                        }
                        Expr::Bin(_, a, b) | Expr::Shift(_, a, b) | Expr::Ternary(_, a, b) => {
                            stack.push(a);
                            stack.push(b);
                        }
                        Expr::Call(_, args, _) => stack.extend(args.iter()),
                        _ => {}
                    }
                }
            }
        }
    }

    // -- property 3: it terminates ------------------------------------------------------------

    fn check_bounds(p: &JavaProgram) {
        fn walk(block: &Block) {
            for stmt in block {
                match stmt {
                    Stmt::For { bound, body, .. } => {
                        assert!(*bound > 0, "a bound of {bound} is not a loop that ends");
                        walk(body);
                    }
                    // The guard makes the argument for a `while` the same as for a `for`, so the
                    // check has to be the same too. Leaving it out would quietly stop testing
                    // property 3 for the only loop whose condition is arbitrary.
                    Stmt::While { limit, body, .. } => {
                        assert!(*limit > 0, "a guard limit of {limit} is not a loop that ends");
                        walk(body);
                    }
                    Stmt::Switch { arms, default, .. } => {
                        for arm in arms {
                            walk(&arm.body);
                        }
                        if let Some(b) = default {
                            walk(b);
                        }
                    }
                    Stmt::If { then, otherwise, .. } => {
                        walk(then);
                        walk(otherwise);
                    }
                    _ => {}
                }
            }
        }
        for m in &p.methods {
            walk(&m.body);
        }
        walk(&p.entry.body);
    }

    #[test]
    fn loop_bounds_are_always_positive_literals() {
        for seed in 0..300 {
            check_bounds(&program(seed));
        }
    }

    #[test]
    fn a_loop_counter_is_never_assigned() {
        // The rule that is easy to forget and fatal to omit.
        for seed in 0..300 {
            let p = program(seed);
            assert!(
                !matches!(p.well_formed(), Err(Malformed::AssignedLoopCounter(_))),
                "seed {seed} assigned a loop counter"
            );
        }
    }

    fn callees(method: &Method) -> Vec<usize> {
        fn in_expr(e: &Expr, out: &mut Vec<usize>) {
            match e {
                Expr::Call(i, args, _) => {
                    out.push(*i);
                    args.iter().for_each(|a| in_expr(a, out));
                }
                Expr::Neg(a)
                | Expr::Not(a)
                | Expr::Cast(_, a)
                | Expr::Classify(a)
                | Expr::ArrayLoad(_, _, a) => in_expr(a, out),
                Expr::Bin(_, a, b) | Expr::Shift(_, a, b) => {
                    in_expr(a, out);
                    in_expr(b, out);
                }
                Expr::Ternary(c, a, b) => {
                    in_cond(c, out);
                    in_expr(a, out);
                    in_expr(b, out);
                }
                _ => {}
            }
        }
        fn in_cond(c: &Cond, out: &mut Vec<usize>) {
            match c {
                Cond::Cmp(_, a, b) => {
                    in_expr(a, out);
                    in_expr(b, out);
                }
                Cond::And(a, b) | Cond::Or(a, b) => {
                    in_cond(a, out);
                    in_cond(b, out);
                }
                Cond::Not(a) => in_cond(a, out),
            }
        }
        fn in_block(b: &Block, out: &mut Vec<usize>) {
            for s in b {
                match s {
                    Stmt::Declare { init, .. } => in_expr(init, out),
                    Stmt::Assign { expr, .. } => in_expr(expr, out),
                    Stmt::If { cond, then, otherwise } => {
                        in_cond(cond, out);
                        in_block(then, out);
                        in_block(otherwise, out);
                    }
                    Stmt::Switch { selector, arms, default } => {
                        in_expr(selector, out);
                        for arm in arms {
                            in_block(&arm.body, out);
                        }
                        if let Some(body) = default {
                            in_block(body, out);
                        }
                    }
                    Stmt::While { cond, body, .. } => {
                        in_cond(cond, out);
                        in_block(body, out);
                    }
                    Stmt::Break | Stmt::Continue | Stmt::Throw(_) => {}
                    Stmt::RefStore { .. } | Stmt::TypeProbe { .. } => {}
                    Stmt::For { body, .. } => in_block(body, out),
                    Stmt::ArrayStore { index, value, .. } => {
                        in_expr(index, out);
                        in_expr(value, out);
                    }
                    Stmt::NewObject { arg, .. } | Stmt::SetObject { arg, .. } => {
                        in_expr(arg, out)
                    }
                    Stmt::FieldStore { value, .. } => in_expr(value, out),
                    // Only the constructor arguments: a worker body cannot contain a call at all,
                    // because it is emitted inside another class where an unqualified static name
                    // does not resolve — `check_block` enforces it with an empty `visible` list.
                    Stmt::Fork { args, .. } => {
                        in_expr(&args.0, out);
                        in_expr(&args.1, out);
                    }
                    Stmt::NewArray { .. } => {}
                }
            }
        }
        let mut out = Vec::new();
        in_block(&method.body, &mut out);
        in_expr(&method.result, &mut out);
        out
    }

    #[test]
    fn no_method_can_call_itself_or_anything_after_it() {
        // The DAG that makes recursion unrepresentable. Checked on the AST rather than the text,
        // because "m3 does not contain the substring m3" is a much weaker statement.
        for seed in 0..300 {
            let p = program(seed);
            for (index, method) in p.methods.iter().enumerate() {
                for callee in callees(method) {
                    assert!(callee < index, "seed {seed}: m{index} calls m{callee}");
                }
            }
        }
    }

    #[test]
    fn the_cost_budget_is_respected() {
        let config = GenConfig::default();
        for seed in 0..300 {
            let cost = program(seed).cost_per_call();
            assert!(
                cost <= config.budget * 2,
                "seed {seed} estimated {cost} statement-executions against a budget of {}",
                config.budget
            );
        }
    }

    // -- property 4: total --------------------------------------------------------------------

    #[test]
    fn every_program_catches_everything_and_returns_a_marker() {
        for seed in 0..50 {
            let source = program(seed).to_java();
            assert!(source.contains("static int run() {"), "seed {seed}");
            assert!(
                source.contains(&format!("w < {}; w++", GenConfig::default().warmup)),
                "seed {seed} has no warm-up loop, so the JIT arm would be the interpreter"
            );
            assert!(
                source.contains(&format!(
                    "catch (ArithmeticException e) {{ r = {}; }}",
                    marks::ARITHMETIC
                )),
                "seed {seed}"
            );
            // The two array failures need their own catches, or both collapse into `OTHER` and a
            // campaign cannot tell "the index was out of range" from "the length was negative" —
            // which is exactly the disagreement worth catching between two engines.
            assert!(
                source.contains(&format!(
                    "catch (ArrayIndexOutOfBoundsException e) {{ r = {}; }}",
                    marks::BOUNDS
                )),
                "seed {seed}"
            );
            assert!(
                source.contains(&format!(
                    "catch (NegativeArraySizeException e) {{ r = {}; }}",
                    marks::NEGATIVE_SIZE
                )),
                "seed {seed}"
            );
            assert!(
                source.contains(&format!("catch (Throwable t) {{ r = {}; }}", marks::OTHER)),
                "seed {seed} must have a last-resort catch or an escaping throw is unobservable"
            );
        }
    }

    #[test]
    fn markers_are_distinct_and_far_from_the_constant_pool() {
        let all = [
            marks::ARITHMETIC,
            marks::BOUNDS,
            marks::NULL,
            marks::CLASS_CAST,
            marks::STACK_OVERFLOW,
            marks::NEGATIVE_SIZE,
            marks::OTHER,
            marks::STRING_IDENTITY_FALSE,
            marks::STRING_IDENTITY_TRUE,
        ];
        let mut sorted = all.to_vec();
        sorted.sort_unstable();
        sorted.dedup();
        assert_eq!(sorted.len(), all.len(), "two markers that collide are one lost finding");
        for m in all {
            assert!(
                !INT_POOL.contains(&m),
                "{m} is drawable as a literal, which weakens the marker"
            );
        }
    }

    // -- the emitter --------------------------------------------------------------------------

    #[test]
    fn the_class_name_matches_the_seed_and_is_a_java_identifier() {
        let p = program(77);
        assert_eq!(p.class_name(), "Fz77");
        assert!(p.to_java().contains("public class Fz77 {"));
    }

    #[test]
    fn main_prints_run_so_the_reference_jdk_has_something_to_say() {
        // The two sides are invoked differently — `run-headless` calls `run`, `java` calls `main` —
        // so the program has to serve both.
        let source = program(5).to_java();
        assert!(
            source.contains("public static void main(String[] a) { System.out.println(run()); }")
        );
    }

    #[test]
    fn the_extreme_int_literal_is_written_by_name_not_as_a_decimal() {
        // `-(2147483648)` is a compile error; `Integer.MIN_VALUE` is not. Since every compound
        // expression is parenthesised, the decimal form is not safe here.
        let mut out = String::new();
        emit_int_lit(&mut out, i32::MIN);
        assert_eq!(out, "Integer.MIN_VALUE");
        let mut out = String::new();
        emit_expr(&mut out, &Expr::Neg(Box::new(Expr::IntLit(i32::MIN))));
        assert_eq!(out, "(- Integer.MIN_VALUE)");
        let mut out = String::new();
        emit_long_lit(&mut out, i64::MIN);
        assert_eq!(out, "Long.MIN_VALUE");
    }

    #[test]
    fn long_literals_carry_their_suffix() {
        let mut out = String::new();
        emit_expr(&mut out, &Expr::LongLit(4294967296));
        assert_eq!(out, "4294967296L", "without the L this is an int literal out of range");
    }

    #[test]
    fn a_negated_negative_literal_does_not_become_a_pre_decrement() {
        // `(--31)` is a pre-decrement of a literal and a compile error; `(- -31)` is arithmetic.
        let mut out = String::new();
        emit_expr(&mut out, &Expr::Neg(Box::new(Expr::IntLit(-31))));
        assert_eq!(out, "(- -31)");
        for seed in 0..300 {
            let source = program(seed).to_java();
            assert!(!source.contains("--"), "seed {seed} emitted a `--`");
        }
    }

    #[test]
    fn every_compound_expression_is_parenthesised() {
        // Precedence bugs do not show up as compile errors — they show up as a program that
        // computes something other than what the AST says, which makes every finding suspect.
        let e = Expr::Bin(
            BinOp::Mul,
            Box::new(Expr::Bin(BinOp::Add, Box::new(Expr::IntLit(1)), Box::new(Expr::IntLit(2)))),
            Box::new(Expr::IntLit(3)),
        );
        let mut out = String::new();
        emit_expr(&mut out, &e);
        assert_eq!(out, "((1 + 2) * 3)");
    }

    #[test]
    fn a_shift_amount_must_be_an_int_even_on_a_long() {
        let mut p = program(1);
        p.entry.result = Expr::Cast(
            Ty::Int,
            Box::new(Expr::Shift(
                ShiftOp::Left,
                Box::new(Expr::LongLit(1)),
                Box::new(Expr::LongLit(2)),
            )),
        );
        assert!(p.well_formed().is_err(), "`long << long` is out of this grammar on purpose");
    }

    #[test]
    fn the_edge_pool_is_what_gets_drawn_from() {
        // The whole bias argument: `32`, `63`, `Integer.MIN_VALUE` should be everywhere, and a
        // uniform i32 would produce them essentially never.
        let mut hits = 0;
        for seed in 0..200 {
            let source = program(seed).to_java();
            if source.contains("Integer.MIN_VALUE")
                || source.contains("Long.MIN_VALUE")
                || source.contains(" 32")
                || source.contains(" 63")
                || source.contains(" 64")
            {
                hits += 1;
            }
        }
        assert!(hits > 100, "only {hits}/200 programs touched an edge constant");
    }

    /// A generated program, printed, so a human can see what the grammar actually produces.
    /// `cargo test --release --lib fuzz::gen::tests::show -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn show() {
        for seed in 0..3 {
            let p = program(seed);
            println!("==== seed {seed} (cost {}, {} nodes) ====", p.estimated_cost(), p.size());
            println!("{}", p.to_java());
        }
    }
    // -- objects, fields and dispatch (stage 3) ------------------------------------------------

    #[test]
    fn an_object_share_of_zero_gives_the_grammar_without_objects_back() {
        let flat = GenConfig { object_share: 0, dispatch_probe: false, ..GenConfig::default() };
        for seed in 0..200 {
            let source = JavaGenerator::new(flat).generate(Seed(seed)).to_java();
            // The hierarchy is emitted only when something uses it, so its absence is the whole
            // claim: no classes, and therefore no `getfield`, no `putfield` and no dispatch.
            for needle in ["class Fz", ".v()", ".w()", "extends"] {
                let hit = source.matches(needle).count();
                // `class Fz<seed> {` is the program's own class, which is always there.
                let expected = usize::from(needle == "class Fz");
                assert_eq!(
                    hit, expected,
                    "seed {seed} emitted {needle:?} {hit} times at object_share 0"
                );
            }
        }
    }

    #[test]
    fn the_probe_plants_a_call_site_in_every_program() {
        // The point of planting rather than rolling: this is 300/300, not a share.
        for seed in 0..300 {
            let p = program(seed);
            assert!(
                !planted_sites(&p.entry.body).is_empty(),
                "seed {seed} has no planted call site:\n{}",
                p.to_java()
            );
        }
    }

    #[test]
    fn both_a_monomorphic_and_a_polymorphic_site_are_reachable() {
        // The *pair* is what an inline cache is tested by: a guard that always holds and a guard
        // that always fails, on the same shape of code. One without the other proves half of it.
        let (mut mono, mut poly, mut both) = (0, 0, 0);
        for seed in 0..300 {
            let p = program(seed);
            let sites = planted_sites(&p.entry.body);
            let m = sites.iter().filter(|s| !s.rotates()).count();
            let q = sites.iter().filter(|s| s.rotates()).count();
            mono += usize::from(m > 0);
            poly += usize::from(q > 0);
            both += usize::from(m > 0 && q > 0);
        }
        // Two thirds of the seeds each, a third with both — the three faces of the roll in
        // `dispatch_shape`. The thresholds are loose enough to survive a reweighting and tight
        // enough that dropping either shape fails.
        assert!(mono > 150, "only {mono}/300 programs have a monomorphic site");
        assert!(poly > 150, "only {poly}/300 programs have a polymorphic site");
        assert!(both > 50, "only {both}/300 programs have both at once");
    }

    #[test]
    fn a_polymorphic_site_rotates_between_two_different_classes_and_never_to_null() {
        // Two *distinct* classes, or it is a monomorphic site written the long way and the cache
        // never misses. And never a `null`: the planted site has to survive all 40 warm-up calls,
        // because a program that throws before `JitCache::THRESHOLD` is never compiled at all
        // (FZ-005), and an uncompiled probe measures nothing.
        let mut checked = 0;
        for seed in 0..300 {
            let p = program(seed);
            for site in planted_sites(&p.entry.body) {
                assert!(site.initial.is_some(), "seed {seed}: a planted site starts at null");
                if !site.rotates() {
                    continue;
                }
                let Some(Stmt::If { then, otherwise, .. }) = site.body.first() else {
                    panic!("seed {seed}: a rotating site with no `if`");
                };
                let (
                    Some(Stmt::SetObject { class: a, name: left, .. }),
                    Some(Stmt::SetObject { class: b, name: right, .. }),
                ) = (then.first(), otherwise.first())
                else {
                    panic!("seed {seed}: a rotating site whose arms do not reassign");
                };
                assert_eq!(left, right, "seed {seed} rotates two different names");
                assert_eq!(left, site.receiver, "seed {seed} rotates something else");
                assert_ne!(a, b, "seed {seed} rotates between one class and itself");
                assert!(a.is_some() && b.is_some(), "seed {seed} rotates through a null");
                checked += 1;
            }
        }
        assert!(checked > 150, "only {checked} rotating sites in 300 programs");
    }

    #[test]
    fn what_the_planted_sites_compute_reaches_the_result() {
        // The trap this exists for: a planted site that runs, misses its cache, deopts — and
        // writes an accumulator nobody reads, so no divergence in any of it could change the one
        // `int` the executor observes. That is FZ-004's mistake in miniature, which is why
        // `finish_method` folds the accumulators in rather than hoping the result happens to
        // mention them.
        for seed in 0..300 {
            let p = program(seed);
            let mut reads = Vec::new();
            collect_var_reads(&p.entry.result, &mut reads);
            for site in planted_sites(&p.entry.body) {
                assert!(
                    reads.contains(site.acc),
                    "seed {seed}: the site writing {} never reaches the result:\n{}",
                    site.acc,
                    p.to_java()
                );
            }
        }
    }

    #[test]
    fn a_class_that_overrides_nothing_is_emitted_and_used() {
        // `S2` declares no method at all, so a vtable built by copying only what a class declares
        // leaves its slots empty. Every other subclass in the hierarchy hides that.
        let mut used = 0;
        for seed in 0..300 {
            let source = program(seed).to_java();
            let s2 = format!("class Fz{seed}S2 extends Fz{seed}B");
            assert!(source.contains(&s2), "seed {seed} has no S2");
            let body = &source[source.find(&s2).unwrap()..];
            let body = &body[..body.find("\n}").unwrap()];
            assert!(!body.contains("int v()"), "seed {seed}: S2 overrides v()");
            assert!(!body.contains("long w()"), "seed {seed}: S2 overrides w()");
            used += usize::from(source.contains(&format!("new Fz{seed}S2(")));
        }
        assert!(used > 100, "only {used}/300 programs instantiate the class that overrides nothing");
    }

    #[test]
    fn the_hierarchy_carries_a_long_only_when_something_reads_one() {
        // Not a cosmetic saving. `burst::compile` resolves a `getfield`/`putfield` only for a
        // non-volatile `int` instance field, and it **inlines** the `invokespecial` of a
        // constructor into its caller — so a constructor that wrote a `long` would make every
        // method containing a `new` ineligible. With `wide_fields` off the hierarchy has to be
        // int-only all the way down, the constructor included.
        let narrow_cfg = GenConfig { wide_fields: false, ..GenConfig::default() };
        for seed in 0..120 {
            let narrow = JavaGenerator::new(narrow_cfg).generate(Seed(seed)).to_java();
            assert!(!narrow.contains("long b;"), "seed {seed} carries a long field it cannot read");
            assert!(!narrow.contains("long w()"), "seed {seed} carries a long virtual");
            assert!(!narrow.contains("1000003L"), "seed {seed}: the constructor writes a long");
        }
        // And with the knob on — the default since the coverage measurement — most programs do
        // reach it, but not all: `ObjUse` still keeps the field out of the ones that never read it.
        let carried =
            (0..120).filter(|&seed| program(seed).to_java().contains("long b;")).count();
        assert!(carried > 40, "only {carried}/120 default programs reach the long half");
        assert!(carried < 120, "every program carries the long field — ObjUse is not filtering");
    }

    #[test]
    fn null_receivers_are_rare_and_present() {
        // The balance FZ-005 forced, in its third setting. A `null` receiver is a real deopt path
        // — `getfield`, `putfield` and `invokevirtual` on one all leave native code rather than
        // throwing inside it — and a program that dies on warm-up iteration 1 never reaches
        // `JitCache::THRESHOLD`. So: present, and nowhere near common.
        //
        // That the *planted* sites never contain one is asserted where the shape is recognised
        // precisely, in `a_polymorphic_site_rotates_between_two_different_classes_and_never_to_null`.
        // Trying to assert it here as well meant matching any `if` inside any `for`, which also
        // matches what the ordinary grammar rolls — a recogniser loose enough to report the
        // generator working correctly as a failure.
        let mut with_null = 0;
        for seed in 0..300 {
            with_null += usize::from(count_nulls(&program(seed)) > 0);
        }
        assert!(with_null > 5, "only {with_null}/300 programs build a null receiver");
        assert!(with_null < 150, "{with_null}/300 programs build one — too many to warm up");
    }

    #[test]
    fn well_formed_rejects_the_object_things_it_should() {
        // The reducer reaches every one of these by deleting the statement that declared an object.
        let mut p = program(7);
        p.entry.result = Expr::Field("nope".to_string(), Field::A);
        assert!(matches!(p.well_formed(), Err(Malformed::NotAnObject(_))));

        let mut p = program(7);
        p.entry.result = Expr::Bin(
            BinOp::Add,
            Box::new(Expr::Virtual("nope".to_string(), VMethod::V)),
            Box::new(Expr::IntLit(1)),
        );
        assert!(matches!(p.well_formed(), Err(Malformed::NotAnObject(_))));

        // An object read as a scalar — `o0 + 1`, which does not compile.
        let mut p = program(7);
        let obj = p
            .entry
            .body
            .iter()
            .find_map(|s| match s {
                Stmt::NewObject { name, .. } => Some(name.clone()),
                _ => None,
            })
            .expect("the probe plants one");
        p.entry.result = Expr::Var(obj.clone(), Ty::Int);
        assert!(matches!(p.well_formed(), Err(Malformed::WrongType { .. })));

        // And an object *assigned* a scalar — `o0 = 1;`. The generator cannot build this, since
        // `Scope::assignable` filters objects out; the checker rejects it anyway, because an object
        // local carries `Ty::Int` as a placeholder and the plain type comparison would let it past.
        let mut p = program(7);
        p.entry.body.push(Stmt::Assign {
            name: obj.clone(),
            ty: Ty::Int,
            expr: Expr::IntLit(1),
        });
        assert!(matches!(p.well_formed(), Err(Malformed::WrongType { .. })));

        // A `putfield` of the wrong width. Java would widen; the AST says a shrink went somewhere
        // it should not have.
        let mut p = program(7);
        p.entry.body.push(Stmt::FieldStore {
            obj: obj.clone(),
            field: Field::A,
            value: Expr::LongLit(1),
        });
        assert!(matches!(p.well_formed(), Err(Malformed::FieldValueType { .. })));

        let mut p = program(7);
        p.entry.body.push(Stmt::SetObject {
            name: obj,
            class: Some(ObjClass::S0),
            arg: Expr::DoubleLit(0),
        });
        assert!(matches!(p.well_formed(), Err(Malformed::BadConstructorArgument(Ty::Double))));
    }

    /// One planted call site, as [`Gen::dispatch_shape`] wrote it.
    struct PlantedSite<'a> {
        /// The class the receiver starts as. Never `None` — see the rotation test.
        initial: Option<ObjClass>,
        receiver: &'a String,
        acc: &'a String,
        /// The body of the loop the site sits in.
        body: &'a Block,
    }

    impl PlantedSite<'_> {
        /// Whether the loop reassigns the receiver, which is what makes the site polymorphic.
        fn rotates(&self) -> bool {
            reassigns(self.body, self.receiver)
        }
    }

    /// The planted sites, read off the **head** of a method body.
    ///
    /// The head is the only place they can be — [`Gen::finish_method`] puts the shape in front of
    /// everything it generates — and reading them from there is what makes this a recogniser for
    /// *the emitter* rather than a pattern. The looser version of this function (any `for`
    /// containing a virtual call) matched what the ordinary grammar rolls as well, and reported
    /// two correct programs as failures before it was tightened.
    fn planted_sites(body: &Block) -> Vec<PlantedSite<'_>> {
        let mut out = Vec::new();
        let mut at = 0;
        // Never more than two: `dispatch_shape` plants a monomorphic site, a polymorphic one, or
        // one of each.
        while out.len() < 2 && at + 3 <= body.len() {
            let (
                Stmt::NewObject { name: receiver, class: initial, .. },
                Stmt::Declare { name: acc, .. },
                Stmt::For { body: loop_body, .. },
            ) = (&body[at], &body[at + 1], &body[at + 2])
            else {
                break;
            };
            // The loop has to end in `acc = acc + receiver.m()`, or this is three statements that
            // merely start the same way.
            let Some(Stmt::Assign { name, expr: Expr::Bin(BinOp::Add, _, right), .. }) =
                loop_body.last()
            else {
                break;
            };
            let Expr::Virtual(called_on, _) = right.as_ref() else { break };
            if name != acc || called_on != receiver {
                break;
            }
            out.push(PlantedSite { initial: *initial, receiver, acc, body: loop_body });
            at += 3;
        }
        out
    }

    fn reassigns(block: &Block, name: &str) -> bool {
        block.iter().any(|s| match s {
            Stmt::SetObject { name: target, .. } => target == name,
            Stmt::If { then, otherwise, .. } => reassigns(then, name) || reassigns(otherwise, name),
            Stmt::For { body, .. } => reassigns(body, name),
            _ => false,
        })
    }

    fn collect_var_reads(e: &Expr, out: &mut Vec<String>) {
        match e {
            Expr::Var(name, _) => out.push(name.clone()),
            Expr::Neg(a) | Expr::Not(a) | Expr::Cast(_, a) | Expr::Classify(a) => {
                collect_var_reads(a, out)
            }
            Expr::ArrayLoad(_, _, a) => collect_var_reads(a, out),
            Expr::Bin(_, a, b) | Expr::Shift(_, a, b) => {
                collect_var_reads(a, out);
                collect_var_reads(b, out);
            }
            Expr::Ternary(_, a, b) => {
                collect_var_reads(a, out);
                collect_var_reads(b, out);
            }
            Expr::Call(_, args, _) => args.iter().for_each(|a| collect_var_reads(a, out)),
            _ => {}
        }
    }

    fn count_nulls(p: &JavaProgram) -> usize {
        fn in_block(b: &Block) -> usize {
            b.iter()
                .map(|s| match s {
                    Stmt::NewObject { class, .. } | Stmt::SetObject { class, .. } => {
                        usize::from(class.is_none())
                    }
                    Stmt::If { then, otherwise, .. } => in_block(then) + in_block(otherwise),
                    Stmt::For { body, .. } => in_block(body),
                    _ => 0,
                })
                .sum()
        }
        p.methods.iter().chain([&p.entry]).map(|m| in_block(&m.body)).sum()
    }
}

#[cfg(test)]
mod against_javac {
    //! The test the whole "type-correct by construction" claim rests on: real `javac`, real
    //! programs. Ignored because it needs a JDK on the machine, like everything else in
    //! [`crate::fuzz::exec`].
    //!
    //! `cargo test --release --lib fuzz::gen::against_javac -- --ignored --nocapture`

    use super::*;
    use crate::fuzz::exec::{ProcessRunner, Toolchain};
    use crate::fuzz::{Generator as _, Outcome, Path, Runner as _};
    use std::time::Duration;

    #[test]
    #[ignore]
    fn nothing_the_generator_emits_is_rejected_by_javac() {
        const SEEDS: u64 = 120;
        let workdir = std::env::temp_dir().join("kaji-fuzz-gen");
        let mut runner =
            ProcessRunner::new(Toolchain::detect(), &workdir, Duration::from_secs(30));
        let mut generator = JavaGenerator::default();

        let mut rejected = Vec::new();
        for seed in 0..SEEDS {
            let program = generator.generate(Seed(seed));
            // Any path compiles the same source; the interpreter is the cheapest to reach.
            if let Outcome::CompileError(diagnostics) = runner.run(&program, Path::Interpreter).outcome
            {
                rejected.push((seed, diagnostics));
            }
        }
        if !rejected.is_empty() {
            let (seed, diagnostics) = &rejected[0];
            panic!(
                "{}/{SEEDS} generated programs were rejected by javac — every one of them is a bug \
                 in the generator, not in the VM.\nfirst was seed {seed}:\n{diagnostics}\n{}",
                rejected.len(),
                generator.generate(Seed(*seed)).to_java()
            );
        }
    }
}
