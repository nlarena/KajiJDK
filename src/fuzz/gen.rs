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
//! - the only loop is `for (int i = 0; i < K; i++)` with **K a literal**, never an expression;
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
//! # What is out of scope in this version
//!
//! Written down because an unstated scope is indistinguishable from an oversight:
//!
//! | left out | why |
//! |---|---|
//! | objects, `new`, fields, virtual calls | needs the heap and the GC in the comparison; a large step, and object *identity* is a determinism hazard. The valuable half is the **inline cache** — a monomorphic site that always hits next to a polymorphic one that misses and deopts — and nothing generated tests it today |
//! | multi-dimensional arrays | `multianewarray` is outside the JIT's subset "and not narrowly" (`burst::compile`); a one-dimensional array already reaches every guard worth reaching |
//! | `null` arrays | an `NullPointerException` from `arraylength` is a real JIT deopt path, but a `null` in the grammar needs a reference type in [`Ty`], which is the objects step |
//! | strings | `jvm::interpreter::strings` is being given its interning table **right now**, so the expected answer to a string comparison is the thing in flux. Generating them today would manufacture divergences nobody could classify. The first thing to add once that lands |
//! | NaN **payloads** | the one corner of IEEE that Java leaves implementation-defined; [`emit_classifier`] collapses every NaN to one code deliberately |
//! | `char`, `byte`, `short`, `boolean` locals | narrowing conversions are a real bug source, but they multiply the type context before it has earned it |
//! | `while`, `do`, `break`, `continue`, labels | none of them can be bounded structurally the way a counted `for` can |
//! | recursion | see property 3 |
//! | `switch`, ternary chains on non-`int` conditions, `instanceof` | grammar breadth, no new failure mode |
//! | explicit `throw` | the total wrapper already exercises every path a real exception takes |
//! | threads | the whole reason FZ-002 wants a fuzzer, and a level of its own |

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
    /// `NullPointerException`. Reserved; objects are out of scope in this version.
    pub const NULL: i32 = 0x5AFE_0003u32 as i32;
    /// `ClassCastException`. Reserved.
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
    /// Reserved for the day strings enter the grammar. Since F3 hito 3, `strings::intern` is a real
    /// JLS §3.10.5 pool (one instance per literal, `malloc_old`ed, a GC root, pinned out of
    /// `gc::compact`), so a conforming answer here is [`STRING_IDENTITY_TRUE`] and this marker means
    /// the pool was bypassed or the literal moved. It used to be the *expected* answer on this VM,
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

/// An expression of a known [`Ty`].
///
/// Every variant carries enough to answer [`Expr::ty`] without a symbol table, which is what lets
/// the reducer rewrite a subtree and immediately know whether the result still type-checks.
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
            Expr::Classify(_) | Expr::ArrayLength(_) => Ty::Int,
            Expr::ArrayLoad(_, elem, _) => *elem,
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
            | Expr::Var(_, _) => 1,
            Expr::Neg(inner)
            | Expr::Not(inner)
            | Expr::Cast(_, inner)
            | Expr::Classify(inner)
            | Expr::ArrayLoad(_, _, inner) => 1 + inner.size(),
            Expr::Bin(_, a, b) | Expr::Shift(_, a, b) => 1 + a.size() + b.size(),
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
            Expr::IntLit(_)
            | Expr::LongLit(_)
            | Expr::FloatLit(_)
            | Expr::DoubleLit(_)
            | Expr::ArrayLength(_)
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

#[derive(Clone, PartialEq, Eq, Debug)]
pub enum Stmt {
    /// `<ty> <name> = <init>;`
    Declare { name: String, ty: Ty, init: Expr },
    /// `<name> = <expr>;` — `name` must be an assignable local of `expr`'s type.
    Assign { name: String, ty: Ty, expr: Expr },
    If { cond: Cond, then: Block, otherwise: Block },
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
}

impl Stmt {
    pub fn size(&self) -> usize {
        match self {
            Stmt::Declare { init, .. } => 1 + init.size(),
            Stmt::Assign { expr, .. } => 1 + expr.size(),
            Stmt::If { cond, then, otherwise } => 1 + cond.size() + then.size() + otherwise.size(),
            Stmt::For { body, .. } => 1 + body.size(),
            Stmt::NewArray { .. } => 1,
            Stmt::ArrayStore { index, value, .. } => 1 + index.size() + value.size(),
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
            Stmt::For { body, .. } => body.iter().any(|s| s.classifies(ty)),
            Stmt::NewArray { .. } => false,
            Stmt::ArrayStore { index, value, .. } => {
                index.classifies(ty) || value.classifies(ty)
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
}

impl Program for JavaProgram {
    fn class_name(&self) -> &str {
        &self.class
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
        for method in &self.methods {
            emit_method(&mut out, method);
        }
        emit_method(&mut out, &self.entry);
        // The total wrapper (property 4). `Throwable` last, so the specific catches win.
        //
        // The **warm-up loop** inside the `try` is what makes the interpreter/JIT pairing mean
        // anything at all; see [`GenConfig::warmup`] for the measurement that put it here. The
        // accumulator, rather than simply keeping the last result, is so that a wrong answer on
        // *any* iteration reaches the return value: with a deopt in play, the iteration that runs
        // native and the iteration that runs last need not be the same one.
        let _ = writeln!(out, "    static int run() {{");
        let _ = writeln!(out, "        try {{");
        let _ = writeln!(out, "            int acc = 0;");
        let _ = writeln!(
            out,
            "            for (int w = 0; w < {}; w++) {{ acc = ((acc * 31) + {}()); }}",
            self.warmup, self.entry.name
        );
        let _ = writeln!(out, "            return acc;");
        let _ = writeln!(out, "        }}");
        let _ = writeln!(
            out,
            "        catch (ArithmeticException e) {{ return {}; }}",
            marks::ARITHMETIC
        );
        let _ = writeln!(
            out,
            "        catch (ArrayIndexOutOfBoundsException e) {{ return {}; }}",
            marks::BOUNDS
        );
        let _ = writeln!(
            out,
            "        catch (NegativeArraySizeException e) {{ return {}; }}",
            marks::NEGATIVE_SIZE
        );
        let _ =
            writeln!(out, "        catch (NullPointerException e) {{ return {}; }}", marks::NULL);
        let _ = writeln!(out, "        catch (Throwable t) {{ return {}; }}", marks::OTHER);
        let _ = writeln!(out, "    }}");
        // `main` exists because the two sides are invoked differently: `run-headless` calls `run`
        // directly, a real `java` needs an entry point that prints what it got.
        let _ =
            writeln!(out, "    public static void main(String[] a) {{ System.out.println(run()); }}");
        let _ = writeln!(out, "}}");
        out
    }
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

fn emit_method(out: &mut String, method: &Method) {
    let params = method
        .params
        .iter()
        .map(|(name, ty)| format!("{} {}", ty.keyword(), name))
        .collect::<Vec<_>>()
        .join(", ");
    let _ = writeln!(out, "    static {} {}({}) {{", method.returns.keyword(), method.name, params);
    for stmt in &method.body {
        emit_stmt(out, stmt, 2);
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

fn emit_stmt(out: &mut String, stmt: &Stmt, depth: usize) {
    indent(out, depth);
    match stmt {
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
                emit_stmt(out, s, depth + 1);
            }
            indent(out, depth);
            if otherwise.is_empty() {
                let _ = writeln!(out, "}}");
            } else {
                let _ = writeln!(out, "}} else {{");
                for s in otherwise {
                    emit_stmt(out, s, depth + 1);
                }
                indent(out, depth);
                let _ = writeln!(out, "}}");
            }
        }
        Stmt::For { var, bound, body } => {
            let _ = writeln!(out, "for (int {var} = 0; {var} < {bound}; {var}++) {{");
            for s in body {
                emit_stmt(out, s, depth + 1);
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
}

impl Local {
    fn scalar(name: String, ty: Ty) -> Local {
        Local { name, ty, assignable: true, array_of: None, array_len: 0 }
    }
}

/// The type context generation runs in.
#[derive(Clone, Debug, Default)]
pub struct Scope {
    locals: Vec<Local>,
}

impl Scope {
    fn readable(&self, ty: Ty) -> Vec<&str> {
        self.locals
            .iter()
            .filter(|l| l.ty == ty && l.array_of.is_none())
            .map(|l| l.name.as_str())
            .collect()
    }

    fn assignable(&self, ty: Ty) -> Vec<&str> {
        self.locals
            .iter()
            .filter(|l| l.ty == ty && l.assignable && l.array_of.is_none())
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
            array_share: 22,
            wide_array_elements: true,
            max_array_len: 6,
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
        self.finish_method(format!("m{index}"), params, returns, scope)
    }

    /// The method `run()` calls. Always returns `int` — that is the only thing the executor can
    /// observe — and takes no parameters, since nobody would have anything to pass.
    fn entry_method(&mut self, index: usize) -> Method {
        self.finish_method(format!("m{index}"), Vec::new(), Ty::Int, Scope::default())
    }

    fn finish_method(
        &mut self,
        name: String,
        params: Vec<(String, Ty)>,
        returns: Ty,
        mut scope: Scope,
    ) -> Method {
        let body = self.block(&mut scope, 0);
        let result = self.expr(&scope, returns, 0);
        let cost = self.block_cost(&body) + self.expr_cost(&result);
        Method { name, params, returns, body, result, cost }
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
                block.push(stmt);
            }
        }
        block
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
                // benchmark. Dividing it here is the whole mechanism.
                let outer = self.budget;
                self.budget = (self.budget / bound.max(1) as u64).max(1);
                let var = self.fresh("i");
                let mut body_scope = scope.clone();
                // Readable, *not* assignable. See [`Local::assignable`].
                body_scope.locals.push(Local {
                    name: var.clone(),
                    ty: Ty::Int,
                    assignable: false,
                    array_of: None,
                    array_len: 0,
                });
                let body = self.block(&mut body_scope, depth + 1);
                self.budget = outer;
                Some(Stmt::For { var, bound, body })
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
            12 if ty == Ty::Int && self.config.fp_share > 0 => {
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
            Stmt::ArrayStore { index, value, .. } => {
                1 + self.expr_cost(index) + self.expr_cost(value)
            }
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
            Expr::ArrayLength(_) => 1,
            Expr::ArrayLoad(_, _, index) => 1 + self.expr_cost(index),
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

// ---------------------------------------------------------------------------------------------
// Well-formedness
// ---------------------------------------------------------------------------------------------

/// Why a program is not valid Java. The reducer gates every candidate on this: rather than proving
/// each individual cut safe — a proof that has to be redone for every new transform — it cuts
/// freely and throws away anything that stops type-checking. Cheaper to write and much harder to
/// get subtly wrong.
#[derive(Clone, PartialEq, Eq, Debug)]
pub enum Malformed {
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
        }
    }
    Ok(())
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
            // An array read as a scalar emits `a0 + 1`, which does not compile. The reducer reaches
            // this by replacing a subtree with a variable that happens to share a name.
            if local.array_of.is_some() {
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
            // `new` deserves better than a substring ban, now that arrays are in the grammar.
            // What must stay impossible is allocating an **object**: an object has an identity,
            // and identity is the one thing two runs of the same program need not agree on. An
            // array allocation is safe here for a reason the grammar enforces rather than hopes
            // for — no node compares two references, so an array's identity is unobservable.
            for allocation in source.split("new ").skip(1) {
                assert!(
                    ["int[", "long[", "float[", "double["]
                        .iter()
                        .any(|k| allocation.starts_with(k)),
                    "seed {seed} allocated something that is not a primitive array: new {}",
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

    #[test]
    fn an_array_share_of_zero_gives_the_grammar_without_arrays_back() {
        let flat = GenConfig { array_share: 0, ..GenConfig::default() };
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
            allocated += usize::from(source.contains(" = new "));
            stored += usize::from(count_array_stores(&p) > 0);
            loaded += usize::from(count_array_loads(&p) > 0);
            length += usize::from(source.contains(".length"));
            let mut lens = Vec::new();
            collect_array_lengths(&p, &mut lens);
            negative += usize::from(lens.iter().any(|&l| l < 0));
            empty += usize::from(lens.contains(&0));
        }
        assert!(allocated > 100, "only {allocated}/300 programs allocated an array");
        assert!(stored > 40, "only {stored}/300 programs stored into one");
        assert!(loaded > 90, "only {loaded}/300 programs read one");
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
                    Stmt::For { body, .. } => in_block(body),
                    Stmt::ArrayStore { index, value, .. } => in_expr(index) + in_expr(value),
                    Stmt::NewArray { .. } => 0,
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
                    Stmt::For { body, .. } => in_block(body, out),
                    Stmt::ArrayStore { index, value, .. } => {
                        in_expr(index, out);
                        in_expr(value, out);
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
                    "catch (ArithmeticException e) {{ return {}; }}",
                    marks::ARITHMETIC
                )),
                "seed {seed}"
            );
            // The two array failures need their own catches, or both collapse into `OTHER` and a
            // campaign cannot tell "the index was out of range" from "the length was negative" —
            // which is exactly the disagreement worth catching between two engines.
            assert!(
                source.contains(&format!(
                    "catch (ArrayIndexOutOfBoundsException e) {{ return {}; }}",
                    marks::BOUNDS
                )),
                "seed {seed}"
            );
            assert!(
                source.contains(&format!(
                    "catch (NegativeArraySizeException e) {{ return {}; }}",
                    marks::NEGATIVE_SIZE
                )),
                "seed {seed}"
            );
            assert!(
                source.contains(&format!("catch (Throwable t) {{ return {}; }}", marks::OTHER)),
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
