//! The **code cache**: who owns compiled methods, who counts invocations, and how the interpreter
//! crosses into native code and back.
//!
//! # One owner per thread, for both the counter and the code
//!
//! [`ExecMem`][crate::burst::exec_mem::ExecMem] is neither `Send` nor `Sync` (step 1 left it that
//! way deliberately — it holds a raw pointer, and sharing generated code across threads is a
//! decision that deserves its own justification). So the cache is **per thread**, and it holds the
//! invocation counters too, rather than putting them on `MethodBody` in the shared metaspace.
//!
//! That is a deviation from the obvious design, and the reason is that splitting the two would put
//! the *decision* under the GIL and the *code* outside it: thread A's counter could trip while
//! thread B owns the only compiled copy, and "is this method compiled?" would have two answers
//! depending on who asks. One owner, one table, one answer. The cost is that each OS thread warms
//! up its own copy of a hot method — real, but bounded (this tier compiles small methods) and
//! irrelevant to the substrate the JIT actually runs on today (`green` has exactly one).
//!
//! # The hotness counter
//!
//! One counter per method, bumped by **two** events: entering it (a Java-to-Java call) and taking
//! a **back-edge** inside it. At [`JitCache::THRESHOLD`] the method is scanned once; the outcome —
//! compiled, or ineligible — is recorded forever, so a method outside the subset (the
//! overwhelming majority) costs one scan in its whole lifetime and a single `HashMap` probe per
//! call thereafter.
//!
//! The two events share a counter deliberately. They are the same question ("is this method worth
//! compiling?") asked from the two directions a method can be hot in, and a method that is hot
//! *both* ways should not have to cross two thresholds independently. Before step 3 only the
//! first was counted, which had a visible consequence: a method entered *once* that then loops a
//! million times was never compiled. `java/BmLoop.run` is exactly such a method, and it is the
//! workload this step exists for.
//!
//! # On-stack replacement
//!
//! A compiled method has, besides its ordinary entry, one entry point per **loop header** — see
//! [`compile`][super::compile] for which loops qualify and why. [`JitCache::run_osr`] enters at
//! one of them, and the same call may come back three ways: the method returned, it deopted, or
//! its **safepoint poll** fired and it wants to be interpreted from a given pc. Going *in* the
//! state is still only the locals buffer and a pc, which is what keeps the entry contract small.
//!
//! # Coming back part-way: [`ResumeState`]
//!
//! Coming *out* is where step 6 changed everything. A deopt used to carry nothing: the interpreter
//! re-ran the method from its first byte, which is indistinguishable from a first execution only
//! while compiled code writes nothing observable — the restriction that kept `putfield`, `iastore`
//! and `putstatic` out of the subset. Now both non-return exits carry a whole interpreter state —
//! locals, **operand stack**, and the pc — and the interpreter *continues* from it. The two exits
//! are one mechanism with two reasons, so they share one table
//! ([`CompiledCode::resume_sites`]) and one reconstruction ([`JitCache::resume_state`]).
//!
//! # Crossing the boundary
//!
//! The interpreter's locals are `Vec<Value>` — a tagged enum whose layout is not this module's
//! business. So the crossing **marshals**: the caller copies the locals the compiled code actually
//! reads (its [`CompiledCode::touched_locals`]) into a flat `[i64]` scratch buffer, calls, and uses
//! the result. Cost is O(touched locals) per invocation, amortised over the loop inside — which is
//! precisely where a JIT wins.
//!
//! That buffer is **longer than the locals**: its tail is where a deopt spills the live operand
//! stack, since the native frame slots holding it die with the frame. [`CompiledCode::buffer_slots`]
//! is the length the contract requires and [`JitCache::install`] is where it is sized.
//!
//! **Every kind of value crosses now**, and each is one 64-bit word: an `int` sign-extended, a
//! **reference** as its heap offset, a `long` whole, a `float` as its 32-bit IEEE pattern
//! zero-extended, and a `double` as its 64-bit pattern.
//!
//! Two of those are worth stating because they are the surprises. **A `long` needs no special
//! handling on this side at all**: the interpreter keeps a `Value::Long` in one local slot and as
//! one entry on its operand stack, and so does the compiled side, so the two layouts are the same
//! layout rather than two that agree — what the category-2 rules cost is confined to the compiler's
//! index arithmetic (see `Kind::Long`). And **a float crosses as bits, not as a number**: compiled
//! code never holds one in a floating-point register across an instruction boundary, so there is no
//! second bank for this boundary to know about (see `Kind::Float`).
//!
//! What can still abandon a call is a slot the compiler could not type at all — which, now that
//! every descriptor form has a kind, means only a descriptor that could not be parsed. The compiled
//! code provably never *reads* such a slot, so it is a fallback rather than a correctness guard.
//!
//! # The tag, and which direction needs it
//!
//! An `i64` in the scratch buffer does not say which of the five kinds it is, and they are not
//! interchangeable at the far end: an offset put back into a frame as a `Value::Int` is a live
//! object the collector can no longer see *or relocate*, an `int` put back as a `Value::Reference`
//! is a pointer made of arithmetic, a `long` put back as either is read at the wrong width, and a
//! `float` put back as an `int` is its exponent and mantissa read as a number. None of them fails
//! where the mistake is.
//!
//! Going **in**, the tag is not needed: the compiler derived its entry types from the method's own
//! descriptor, so a disagreement with the frame would mean the descriptor was wrong — the
//! verifier's business. Coming **out**, it is needed and it is carried: [`JitValue`] pairs each
//! value with its kind, taken from [`CompiledCode::returns`] for a return and from
//! [`CompiledCode::resume_sites`] — the type map at that exact pc — for everything a
//! [`ResumeState`] contains, operand stack included.
//!
//! Nothing is marshalled back on a call that **returns**: its only observable effect on the
//! interpreter is the value, so the frame it was built from is discarded untouched. A call that
//! stops part-way is the other case, and that is what [`ResumeState`] is for.

use std::collections::HashMap;
use std::sync::atomic::AtomicU64;
use std::sync::Arc;

use super::compile::{CompiledCode, Ineligible, Kind, Outcome, ResumeSite, Status};

/// A value crossing back from native code, with its **kind** attached.
///
/// The bits are the same whichever kind it is — a boundary word is 64 bits and carries an `int`
/// sign-extended, a heap offset, or a whole `long` (see [`Status`]). What differs is what the
/// interpreter must build out of them, and getting that wrong is the one mistake in this milestone
/// that does not fail where the bug is: an offset stored as a `Value::Int` is a live object the
/// collector can no longer see. So the kind travels *with* the value rather than being re-derived
/// at the far end —
/// from the descriptor for a return ([`CompiledCode::returns`]), from the type map for a
/// local or an operand ([`CompiledCode::resume_sites`]).
/// `Eq` is deliberately absent, exactly as it is on the interpreter's own `Value`: a `Float`
/// wraps an `f32`, which is only `PartialEq` because `NaN != NaN`. Nothing uses one as a map key,
/// so `PartialEq` is all the equality this type is entitled to.
#[derive(Clone, Copy, PartialEq, Debug)]
pub enum JitValue {
    Int(i32),
    /// A heap **offset**, `0` for `null`.
    Reference(usize),
    /// A `long` — the whole 64-bit word, which is what a buffer slot is.
    ///
    /// It has no analogue of the `int`'s normalisation invariant and needs none: a `long` fills the
    /// slot, so there is no upper half to have got out of step with a lower one. Category-2 costs
    /// this side of the boundary nothing at all — the interpreter's `Value::Long` is one entry on
    /// its operand stack and one local slot, and so is this.
    Long(i64),
    /// A `float`, built from the **low 32 bits** of the boundary word — see [`Kind::Float`], which
    /// is where the "a float travels as its bit pattern" convention is stated.
    Float(f32),
    /// A `double`, built from all 64.
    Double(f64),
}

impl JitValue {
    /// Reads one 64-bit buffer word as `kind`.
    ///
    /// An `int` is truncated to its low 32 bits, which is exact rather than lossy: the
    /// normalisation invariant makes every `int` in a slot the sign-extension of its value, so the
    /// upper half is a copy of bit 31 and dropping it loses nothing. A **reference** is a heap
    /// offset, read whole — it is never negative, and since the boundary stopped squeezing a value
    /// through the low half of `RAX` there is no 32-bit window left to truncate it to.
    fn of(kind: Kind, raw: i64) -> Option<JitValue> {
        match kind {
            Kind::Int => Some(JitValue::Int(raw as i32)),
            Kind::Reference => Some(JitValue::Reference(raw as usize)),
            Kind::Long => Some(JitValue::Long(raw)),
            // **A float is bits, not a number, until exactly here.** Compiled code carries the
            // 32-bit IEEE pattern in an ordinary 64-bit slot (zero-extended) and only ever puts it
            // in an SSE register for the length of one arithmetic opcode; this is the one place it
            // becomes an `f32` again. `from_bits` is bit-exact and NaN-preserving, which matters:
            // Java distinguishes NaN payloads no more than this does, but a conversion through a
            // numeric type would quiet a signalling NaN and change the bits.
            Kind::Float => Some(JitValue::Float(f32::from_bits(raw as u32))),
            Kind::Double => Some(JitValue::Double(f64::from_bits(raw as u64))),
            // **The high half of a category-2 local**, and the third reason to leave a slot alone.
            //
            // JVMS §2.6.1 makes it unreadable, so it holds no value, and neither side ever wrote
            // one there: the interpreter's `lstore` puts a whole `Value::Long` in the *low* slot,
            // and so does compiled code. What the interpreter's frame happens to hold at `n + 1` is
            // therefore whatever it held before the `long` arrived — a real, well-typed `Value` —
            // and leaving it is both safe for the collector and correct, because the slot is dead
            // by the same argument `Conflict` is: nothing can read it, since every read is an
            // equality check against a kind this is not.
            Kind::Cat2High => None,
            // **The two kinds that are not values, and the one place `None` is an answer rather
            // than a failure**: a local the caller is told to leave alone.
            //
            // `Opaque` — the type map proved compiled code cannot have written this slot on any
            // path to here, so the interpreter's own value is current.
            //
            // `Conflict` (step 9) — compiled code may have written it, as an `int` down one path
            // and a reference down another, and there is no static answer to which. So the frame
            // keeps the `Value` it already held: safe for the collector whatever it is, and correct
            // because a conflicted slot is provably dead (nothing can read it before storing to
            // it — see `ResumeSite::locals`). The cost is one object kept alive slightly too long.
            Kind::Opaque | Kind::Conflict => None,
        }
    }

    /// The same read, for a kind that is **known to name a value** — every operand-stack position
    /// and every slot of an inlined frame, both guaranteed by [`compile`]'s rebuildability check
    /// ([`Ineligible::Unrebuildable`][crate::burst::compile::Ineligible::Unrebuildable]).
    ///
    /// This exists so those two call sites have no `None` arm to invent a fallback for. The old
    /// shape returned `Option` everywhere and its callers answered a `None` with "the JIT declined"
    /// — which, after native code had already run and already mutated the heap, would restart the
    /// method from its first byte and apply every write twice. There is no path to that now: the
    /// site could not have been installed, and if the invariant were ever broken this stops loudly
    /// instead of quietly re-running a method.
    fn of_value(kind: Kind, raw: i64) -> JitValue {
        match kind {
            // **The one kind that names a value here and no value in [`JitValue::of`]**, because
            // the two callers are asking different questions. An inlined frame does not exist yet,
            // so its high slot has to be written with *something*; the emitted call zeroed it and
            // nothing since has touched it, so `Value::Int(0)` is not a stand-in but literally what
            // is in that slot — and it is what `Frame::reset_for_call` would have put there. The
            // root frame's high slot is a different case (there is an existing `Value` to keep),
            // which is why `of` skips it instead.
            Kind::Cat2High => JitValue::Int(0),
            _ => match JitValue::of(kind, raw) {
                Some(value) => value,
                None => {
                    unreachable!("compile refuses a resume site whose operands or inlined frames are untypable")
                }
            },
        }
    }
}

// ---------------------------------------------------------------------------------------------
// The executable-memory half, which only exists on Windows.
// ---------------------------------------------------------------------------------------------

/// A mapped, executable compiled method.
///
/// Windows-only, because [`exec_mem`][crate::burst::exec_mem] is: `VirtualAlloc`/`VirtualProtect`
/// have no portable stand-in. The rest of `burst` — the assembler and the whole bytecode compiler —
/// is plain byte emission and builds anywhere, so the stub below keeps *this* module portable too:
/// on any other platform mapping simply fails and every method is recorded ineligible, which is the
/// same state a `JVM_JIT=0` run is in.
#[cfg(windows)]
mod native {
    use super::super::exec_mem::ExecMem;
    use std::io;

    /// Executable pages holding one compiled method.
    pub struct Native(ExecMem);

    impl Native {
        /// Maps `code` W^X (allocate RW, copy, flip to RX) and hands back the callable view.
        pub fn map(code: &[u8]) -> io::Result<Native> {
            ExecMem::from_code(code).map(Native)
        }

        /// Calls the compiled method.
        ///
        /// # Safety
        ///
        /// `locals` must point to an initialised, writable `[i64]` at least as long as the highest
        /// local index the compiled code names — i.e. the caller must have honoured the marshalling
        /// contract in [`CompiledCode::touched_locals`][super::CompiledCode::touched_locals]. The
        /// code itself is trusted to be what [`compile`][super::super::compile::compile] produced:
        /// an `extern "system" fn(*mut i64, i64) -> i64` that preserves every non-volatile
        /// register. `entry_pc` must be `0` or one of the code's own
        /// [`osr_entries`][super::CompiledCode::osr_entries] — any other value would fall through
        /// the entry dispatch and run the method from its start with mid-method locals.
        pub unsafe fn call(&self, locals: *mut i64, entry_pc: i64) -> i64 {
            // SAFETY: `compile` emits exactly one function per block, entered at offset 0, built
            // from `x64::Frame` (Microsoft x64 prologue/epilogue, every saved register restored,
            // terminated by `ret`), taking its pointer argument in RCX and its entry pc in RDX and
            // returning the packed status/value in RAX. That is this signature. The caller's own
            // `# Safety` clause covers the pointer and the entry pc.
            let f: extern "system" fn(*mut i64, i64) -> i64 = unsafe { self.0.as_fn() };
            f(locals, entry_pc)
        }
    }
}

/// Portable stand-in: there is no way to make pages executable, so nothing is ever compiled.
#[cfg(not(windows))]
mod native {
    use std::io;

    /// A compiled method that cannot exist on this platform.
    pub struct Native(());

    impl Native {
        /// Always fails: `burst` can *emit* x86-64 anywhere, but only Windows can map it.
        pub fn map(_code: &[u8]) -> io::Result<Native> {
            Err(io::Error::new(io::ErrorKind::Unsupported, "executable memory is Windows-only"))
        }

        /// Unreachable: no `Native` is ever constructed off Windows.
        ///
        /// # Safety
        ///
        /// Vacuous — there is no value of this type to call it on.
        pub unsafe fn call(&self, _locals: *mut i64, _entry_pc: i64) -> i64 {
            unreachable!("no method is ever compiled on this platform")
        }
    }
}

/// A method the cache has finished thinking about.
enum Entry {
    /// Seen `n` times, not yet warm enough to be worth scanning.
    Cold(u32),
    /// Scanned and rejected — an opcode outside the subset, or the mapping failed. Permanent: the
    /// bytecode of a resolved method never changes, so nothing can make this answer stale.
    Rejected,
    /// Compiled. `slots` is how long the scratch buffer must be — the callee's `max_locals`
    /// followed by room for its deepest operand stack, which a resume spills into.
    Compiled {
        native: native::Native,
        touched: Vec<u16>,
        slots: usize,
        /// Where operand-stack position 0 lands in the buffer: the callee's `max_locals`.
        stack_base: usize,
        /// **Where a returned value lands** in the buffer — see [`CompiledCode::result_base`]. Read
        /// on [`Outcome::Returned`] and only when the method returns something.
        result_base: usize,
        /// Where this code's **allocation log** starts in the buffer, and how many records it
        /// holds — `0` for a method that contains no `new`, which carries no log at all. See
        /// [`CompiledCode::alloc_base`].
        alloc_base: usize,
        alloc_records: usize,
        /// The bytecode pcs this code may be entered at on-stack (its loop headers), ascending.
        osr_entries: Vec<u32>,
        /// **The resume map**: every point native code can hand a half-finished method back at,
        /// with the kinds needed to turn the buffer's bare `i64`s into `Value`s, and — since step 8
        /// — the frames inlining removed. Keyed by [`ResumeSite::key`]. See [`ResumeSite`].
        resume: Vec<ResumeSite>,
        /// How many interpreter frames one deopt out of this code can produce. See
        /// [`CompiledCode::frame_depth`]; [`JitCache::frames_needed`] is what the interpreter asks.
        frame_depth: usize,
        /// Whether this method's descriptor returns a reference — i.e. what the 32 bits of an
        /// [`Outcome::Returned`] mean.
        returns: Kind,
        /// Whether this method returns `void`, in which case an [`Outcome::Returned`] carries no
        /// value at all and its 32 bits mean nothing.
        returns_void: bool,
        /// Cleared for good the first time an OSR entry **deopts**.
        ///
        /// Not a correctness rule any more — a deopt now hands back a pc and a state, so the
        /// interpreter continues from exactly where native code stopped and every attempt makes
        /// progress. It is a *cost* rule: a guard that fails immediately on entry (a receiver that
        /// is always null, a divisor that is always zero) would otherwise have every back-edge pay
        /// for a full marshal-and-enter to be told the same thing again. The ordinary entry is
        /// untouched.
        osr_open: bool,
    },
}

/// What the interpreter should do about the call it is holding.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum Decision {
    /// Interpret it. Either the JIT is off, the method is not warm yet, or it was rejected.
    Interpret,
    /// The method just crossed the threshold: fetch its bytecode and call [`JitCache::install`].
    Compile,
    /// There is native code — call [`JitCache::run`].
    Ready,
}

/// **The interpreter state native code left behind**, ready to be poured back into a `Frame`.
///
/// This is what "a real deopt" means in one type: not "give up and re-run the method", but "here
/// are the locals, here is the operand stack, here is the pc — carry on". It is built by
/// [`JitCache::resume_state`] out of the caller's buffer and the compiler's type map for that exact
/// pc, so every value arrives tagged as the `int` or the reference it is.
#[derive(Clone, PartialEq, Debug)]
pub struct ResumeState {
    /// The bytecode pc **the frame the interpreter is holding** resumes at. **The instruction there
    /// has not run** — see the write/pc rule in [`compile`][super::compile].
    ///
    /// When [`inlined`][ResumeState::inlined] is non-empty this is the pc of an *invoke*, and the
    /// frames below are what that invoke would have created.
    pub pc: u32,
    /// The locals compiled code may have written, and only those: a slot absent from this list is
    /// one the type map proved untouched, whose value in the interpreter's own frame is current.
    pub locals: Vec<(u16, JitValue)>,
    /// The operand stack at `pc`, **bottom-first** — push them in this order. Already trimmed of
    /// the arguments of the call this frame is in the middle of, if any.
    pub stack: Vec<JitValue>,
    /// **The frames inlining removed**, outermost first: whole interpreter frames to build and push
    /// on top of the one above. Empty unless native code stopped inside an expanded callee.
    pub inlined: Vec<VirtualState>,
}

/// One frame [`ResumeState`] asks the interpreter to build from nothing — the run-time twin of
/// [`VirtualFrame`][super::compile::VirtualFrame].
///
/// Everything here is complete rather than differential, and that is the difference from the root
/// frame: there is no existing frame whose untouched slots could be left alone, so `locals` is every
/// slot of the method, in order, already tagged.
#[derive(Clone, PartialEq, Debug)]
pub struct VirtualState {
    /// The method this frame runs — the interpreter's own `MethodId`, as [`VirtualFrame::unit`][super::compile::VirtualFrame::unit].
    pub unit: usize,
    /// The bytecode pc in that method. The instruction there has not run.
    pub pc: u32,
    /// Every local slot, slot `0` first. `locals.len()` is the method's `max_locals`.
    pub locals: Vec<JitValue>,
    /// The operand stack, bottom-first.
    pub stack: Vec<JitValue>,
}

/// How a crossing into native code ended. The two ways native code can hand control back *without*
/// the method being over are folded into the one type the interpreter matches on, and since step 6
/// they carry the same thing: a whole interpreter state.
#[derive(Clone, PartialEq, Debug)]
pub enum OsrResult {
    /// The method ran to one of its exits and its frame is finished. `Some` is the value an
    /// `ireturn`/`areturn` handed back; `None` is a `void` method's `return`, which hands back
    /// nothing and whose caller must therefore push nothing.
    Returned(Option<JitValue>),
    /// The safepoint poll fired. Rebuild the frame from this state and resume **interpreting**;
    /// the thread then reaches its safepoint by the ordinary path.
    Safepoint(ResumeState),
    /// Native code met something it cannot do — a zero divisor, a null receiver, an index out of
    /// range. Rebuild the frame from this state and resume interpreting: the interpreter
    /// re-executes the instruction at `pc` and raises the proper exception. It never re-runs what
    /// native code already did, which is what makes a **write** inside compiled code safe.
    Deopt(ResumeState),
}

/// Counters, for tests and for the measurement harness. Every one of them is a *fact about a run*,
/// not a timing: they are what makes "the benchmark actually compiled something" checkable instead
/// of assumed.
#[derive(Clone, Copy, Default, PartialEq, Eq, Debug)]
pub struct JitStats {
    /// Methods successfully compiled and mapped.
    pub compiled: usize,
    /// Methods scanned and permanently refused (or whose mapping failed).
    pub rejected: usize,
    /// Calls that entered native code.
    pub native_calls: usize,
    /// Native calls that came back with [`Status::DEOPT`] — the interpreter re-ran the method.
    pub deopts: usize,
    /// Calls that never entered native code because a local could not be marshalled (a
    /// non-`Int` `Value` in a slot the compiled code reads).
    pub unmarshallable: usize,
    /// Native calls entered **on-stack**, at a loop header rather than at the method's start.
    /// A subset of `native_calls`.
    pub osr_entries: usize,
    /// Native calls that came back because the **safepoint poll** fired: the locals were written
    /// back to the interpreter's frame and the method resumed interpreted.
    pub safepoint_exits: usize,
    /// Native calls that came back because a `new` could not take its fast path — Eden was full, or
    /// the excursion's allocation log was. Counted apart from `deopts` because it is a capacity
    /// condition that clears by itself, not a guard the method keeps failing.
    pub alloc_exits: usize,
    /// **Interpreter frames rebuilt that inlining had removed**, summed over every exit — see
    /// [`ResumeState::inlined`].
    ///
    /// It is the only statistic that distinguishes an exit *from inside an expansion* from an exit
    /// out of the root's own body, and that is exactly what group 3's two stages needed a number
    /// for: a poll at an inlined loop header, and a miss at an inline cache, both leave through a
    /// site whose state is a whole call chain. A test that only checked the answer would pass just
    /// as well against a compilation that never expanded anything.
    pub virtual_frames: usize,
}

/// One thread's compiled methods, invocation counters and scratch buffer.
///
/// Keyed by an opaque `usize` — the interpreter's `MethodId`. `burst` does not depend on
/// `crate::jvm`, so the key stays a number.
pub struct JitCache {
    enabled: bool,
    threshold: u32,
    /// **How many operand-stack positions the compiler keeps in registers** (F3 step 10) — see
    /// [`compile_with_regs`][super::compile::compile_with_regs]. `0` turns the allocator off and
    /// makes the emitter produce exactly what step 9 produced.
    ///
    /// It is a *runtime* setting, not a `cfg`, and that is the whole point: both arms of the
    /// measurement are then the same binary at the same addresses, so this machine's ±3–12%
    /// code-layout noise cannot masquerade as an effect. Same reasoning, same shape, as `enabled`.
    regs: u32,
    entries: HashMap<usize, Entry>,
    /// The marshalling buffer, grown once to the largest `max_locals` seen and reused thereafter —
    /// a compiled call must not allocate, or the allocator would be the thing being measured.
    scratch: Vec<i64>,
    /// The **safepoint poll word** — see [`JitCache::poll_word`] for why it is shaped like this.
    poll: Arc<AtomicU64>,
    stats: JitStats,
}

impl Default for JitCache {
    /// Reads the environment — see [`JitCache::from_env`]. `Default` is what
    /// `RunningCtx::default()` calls when an OS thread builds its execution context, so this is
    /// the constructor that matters in practice.
    fn default() -> Self {
        JitCache::from_env()
    }
}

impl JitCache {
    /// Invocations before a method is scanned. Low enough that a benchmark's inner method is
    /// compiled almost immediately, high enough that the one-off calls of class initialisation and
    /// start-up never pay for a scan. Override with `JVM_JIT_THRESHOLD`.
    pub const THRESHOLD: u32 = 32;

    /// **What `JVM_JIT` says**, read exactly as [`from_env`][JitCache::from_env] reads it — and the
    /// only place that decision is written down.
    ///
    /// It is `pub` for one reason: a *harness* has to be able to say which engine it just measured.
    /// `bench_baseline` used to print "interpreter baseline" unconditionally, which stopped being
    /// true the moment the JIT's default became on, and a header that has to be re-derived from a
    /// second reading of the same variable is a header that will drift again.
    pub fn enabled_by_env() -> bool {
        !matches!(
            std::env::var("JVM_JIT").ok().as_deref().map(str::trim),
            Some("0" | "off" | "false" | "no")
        )
    }

    /// A cache configured from the environment.
    ///
    /// - `JVM_JIT=0` (or `off`/`false`) disables the JIT completely: no counters, no scans, no
    ///   native code. **The default is on.** This is the switch the differential tests and any
    ///   bisection use — with it off the VM is bit-for-bit the interpreter it was before.
    /// - `JVM_JIT_THRESHOLD=<n>` overrides [`JitCache::THRESHOLD`].
    /// - `JVM_JIT_REGS=0` (or `off`/`false`/`no`) turns off the **operand-stack register cache**
    ///   (F3 step 10), leaving the operand stack entirely in frame slots as it was through step 9.
    ///   A number sets how many positions are cached, clamped to
    ///   [`CACHE_REGS`][super::compile::CACHE_REGS]. **The default is all of them.** This is the
    ///   switch the step-10 measurement flips, and it is a runtime one so that both arms are the
    ///   same binary — see [`JitCache::regs`].
    pub fn from_env() -> Self {
        let enabled = Self::enabled_by_env();
        let threshold = std::env::var("JVM_JIT_THRESHOLD")
            .ok()
            .and_then(|v| v.trim().parse::<u32>().ok())
            .unwrap_or(Self::THRESHOLD);
        let regs = match std::env::var("JVM_JIT_REGS").ok().as_deref().map(str::trim) {
            None => super::compile::CACHE_REGS,
            Some("off" | "false" | "no") => 0,
            // An unparseable value is read as "off" rather than as "the default": a typo in a
            // measurement's environment must not silently measure the treatment arm twice.
            Some(v) => v.parse::<u32>().unwrap_or(0).min(super::compile::CACHE_REGS),
        };
        JitCache {
            enabled,
            threshold,
            regs,
            entries: HashMap::new(),
            scratch: Vec::new(),
            poll: Arc::new(AtomicU64::new(0)),
            stats: JitStats::default(),
        }
    }

    /// The **safepoint poll word**: the 8-byte location every compiled loop header checks, whose
    /// address is baked into the instruction stream as an immediate. Non-zero means "leave native
    /// code at the next loop header"; the method then comes back as
    /// [`OsrResult::Safepoint`] and is resumed by the interpreter, which reaches the real
    /// safepoint by its ordinary path. Handed out as an `Arc` so whichever part of the VM owns a
    /// stop-the-world handshake can raise it.
    ///
    /// **The address must not move, and this shape is what guarantees it.** Three candidates were
    /// weighed:
    ///
    /// - The `os` driver's own `gc_pending` — *rejected*. It is an `Arc<AtomicBool>` built fresh
    ///   inside `run_os_parallel`, so there is a different one per VM run and none at all in
    ///   `green`/`os-gil`; and it is one byte, so the 8-byte load the poll wants would read past
    ///   it, into an allocation whose remaining bytes belong to nobody.
    /// - A field of `JitCache` (or of `RunningCtx`) — *rejected*. Both are moved after
    ///   construction (`RunningCtx` is built on an OS thread's stack and passed around by
    ///   reference; a `JitCache` is moved into it), so an interior address would be stale the
    ///   moment the owner moved. A stale pointer here is not a wrong answer, it is a silent read
    ///   of unrelated memory.
    /// - This: an `Arc<AtomicU64>`. The allocation is on the heap and **never moves**; it is
    ///   8 bytes and naturally aligned, so the plain 64-bit load in the generated code is a
    ///   single, tear-free access; and because the `Arc` is owned by the same `JitCache` that owns
    ///   the code, the word provably outlives every instruction that reads it. One word per
    ///   thread, and no process-wide state — so one VM's safepoint can never drag another VM's
    ///   compiled loops out of native code, which matters in a test binary that runs many at once.
    pub fn poll_word(&self) -> Arc<AtomicU64> {
        Arc::clone(&self.poll)
    }

    /// The poll word's address, for [`compile`][super::compile::compile] to bake in. Stable for
    /// as long as this cache — and therefore any code it holds — exists; see
    /// [`poll_word`][JitCache::poll_word].
    pub fn poll_address(&self) -> usize {
        Arc::as_ptr(&self.poll) as usize
    }

    /// Forces the JIT on or off regardless of the environment.
    ///
    /// The differential tests use this rather than `JVM_JIT=0`: `cargo test` runs its tests in
    /// threads of one process, so a test that mutated the environment would be mutating it for
    /// every other test running at that moment. The env var is the *user-facing* switch; this is
    /// the same switch reached programmatically.
    pub fn set_enabled(&mut self, enabled: bool) {
        self.enabled = enabled;
    }

    /// Whether native code may be produced or entered at all.
    pub fn enabled(&self) -> bool {
        self.enabled
    }

    /// Forces the size of the **operand-stack register cache** regardless of `JVM_JIT_REGS`, the
    /// same way [`set_enabled`][JitCache::set_enabled] does for the JIT itself — and for the same
    /// reason: `cargo test` runs its tests in threads of one process, so the measurement cannot
    /// mutate the environment to switch arms.
    pub fn set_cache_regs(&mut self, regs: u32) {
        self.regs = regs.min(super::compile::CACHE_REGS);
    }

    /// How many operand-stack positions this cache compiles into registers.
    pub fn cache_regs(&self) -> u32 {
        self.regs
    }

    /// This cache's counters.
    pub fn stats(&self) -> JitStats {
        self.stats
    }

    /// Records one entry to `key` and says what to do about it.
    ///
    /// The counter lives here, so this is the *only* place hotness is decided. A `Compile` answer
    /// is handed out exactly once per method: the entry is flipped to `Rejected` first, and
    /// [`install`][JitCache::install] promotes it only on success — so a failed compile is a
    /// decision, not a retry loop.
    pub fn on_entry(&mut self, key: usize) -> Decision {
        if !self.enabled {
            return Decision::Interpret;
        }
        match self.entries.entry(key).or_insert(Entry::Cold(0)) {
            Entry::Compiled { .. } => Decision::Ready,
            Entry::Rejected => Decision::Interpret,
            Entry::Cold(n) => {
                *n += 1;
                if *n < self.threshold {
                    return Decision::Interpret;
                }
                // Warm. Claim the scan by settling the entry pessimistically: whatever happens
                // next, this method is never scanned twice.
                self.entries.insert(key, Entry::Rejected);
                self.stats.rejected += 1;
                Decision::Compile
            }
        }
    }

    /// Records one **back-edge** taken inside `key` and says what to do about it — the OSR half of
    /// [`on_entry`][JitCache::on_entry], and deliberately the same counter and the same threshold
    /// (see the module docs). `Ready` means "there may be an entry point at this pc"; ask
    /// [`run_osr`][JitCache::run_osr].
    pub fn on_back_edge(&mut self, key: usize) -> Decision {
        self.on_entry(key)
    }

    /// Whether back-edges in `key` are worth reporting at all.
    ///
    /// The interpreter caches this answer alongside its bytecode cache, so the common case — a
    /// method that will never be compiled, looping — costs one boolean test per back-edge instead
    /// of a hash probe. It is safe to cache because every `false` here is **permanent**: a
    /// rejected method stays rejected, and a compiled method's set of entry points never grows.
    /// (`true` may go stale in the other direction, which only costs a probe; the back-edge hook
    /// re-reads it whenever the cache's answer becomes final.)
    pub fn watches_back_edges(&self, key: usize) -> bool {
        if !self.enabled {
            return false;
        }
        match self.entries.get(&key) {
            None | Some(Entry::Cold(_)) => true,
            Some(Entry::Rejected) => false,
            Some(Entry::Compiled { osr_entries, osr_open, .. }) => *osr_open && !osr_entries.is_empty(),
        }
    }

    /// Records the outcome of the scan [`on_entry`][JitCache::on_entry] asked for.
    ///
    /// `max_locals` is the callee's, and sizes the scratch buffer. A compile error, or a failure to
    /// map the pages, leaves the method permanently interpreted — the pessimistic entry
    /// `on_entry` already wrote.
    pub fn install(&mut self, key: usize, result: Result<CompiledCode, Ineligible>, max_locals: usize) {
        let Ok(compiled) = result else { return };
        let Ok(native) = native::Native::map(&compiled.code) else { return };
        debug_assert_eq!(compiled.stack_base as usize, max_locals, "the buffer layout is the callee's");
        // The scratch buffer must cover every slot the code can address — since step 6 that is the
        // locals **and** the operand slots a deopt spills past them. `max(1)` because an empty
        // `Vec`'s `as_mut_ptr` is dangling, and a dangling pointer is not worth reasoning about
        // even when nothing dereferences it.
        let slots = compiled.buffer_slots as usize;
        self.scratch.resize(self.scratch.len().max(slots).max(1), 0);
        self.entries.insert(
            key,
            Entry::Compiled {
                native,
                touched: compiled.touched_locals,
                slots,
                stack_base: compiled.stack_base as usize,
                result_base: compiled.result_base as usize,
                alloc_base: compiled.alloc_base as usize,
                alloc_records: compiled.alloc_records as usize,
                osr_entries: compiled.osr_entries,
                resume: compiled.resume_sites,
                frame_depth: compiled.frame_depth as usize,
                returns: compiled.returns,
                returns_void: compiled.returns_void,
                osr_open: true,
            },
        );
        // `on_entry` counted this method as rejected before handing out the scan; it turned out
        // fine, so take the pessimism back. Saturating because the two are only paired by
        // convention — a caller that installed without asking would otherwise underflow a counter.
        self.stats.rejected = self.stats.rejected.saturating_sub(1);
        self.stats.compiled += 1;
    }

    /// Runs `key`'s compiled code, marshalling its locals through `local`.
    ///
    /// `local(i)` yields the interpreter's local slot `i` as an `i64` — `v as i64` for a
    /// `Value::Int(v)`, the offset for a `Value::Reference`, and **`None` for anything else**
    /// (a `double`, a `float`), which abandons the call. A `long` marshals like any other value:
    /// one slot, all 64 bits.
    ///
    /// `Some(Returned(v))` when native code ran the method to its exit; `Some(Safepoint(state))` or
    /// `Some(Deopt(state))` when it stopped part-way, in which case the caller must **rebuild its
    /// callee frame from `state` and interpret from `state.pc`** rather than from the method's
    /// first byte; and `None` when native code was never entered at all (not compiled, or a local
    /// that could not be marshalled), where interpreting from the start is right because nothing
    /// happened.
    ///
    /// Before step 6 the two middle cases were also `None` — the interpreter re-ran the method from
    /// the beginning, which was sound only because the compiled subset wrote nothing observable.
    /// Resuming instead of restarting is exactly what lifted that restriction.
    ///
    /// `allocated(offset, size)` is called **once for every object native code allocated**, in
    /// allocation order, before this returns — see [`JitCache::enter`]. It is not optional and it is
    /// not conditional on the outcome: a deopt has allocated just as much as a return has.
    pub fn run(
        &mut self,
        key: usize,
        local: impl Fn(u16) -> Option<i64>,
        allocated: impl FnMut(usize, usize),
    ) -> Option<OsrResult> {
        self.enter(key, 0, false, local, allocated)
    }

    /// Enters `key`'s compiled code **at a loop header**, marshalling its locals through `local`.
    ///
    /// `entry_pc` must be one of the code's own entry points; anything else answers `None` rather
    /// than entering, because the entry dispatch would otherwise fall through and run the method
    /// from its start with mid-loop locals. `None` also covers every other reason not to enter —
    /// not compiled, OSR closed, an unmarshallable local — and all of them mean the same thing to
    /// the caller: **keep interpreting at this pc**, which is safe because native code was never
    /// entered and the interpreter's own frame has not been touched.
    ///
    /// On [`OsrResult::Safepoint`] or [`OsrResult::Deopt`] the caller must rebuild this frame from
    /// the state it carries and resume at its pc.
    pub fn run_osr(
        &mut self,
        key: usize,
        entry_pc: u32,
        local: impl Fn(u16) -> Option<i64>,
        allocated: impl FnMut(usize, usize),
    ) -> Option<OsrResult> {
        match self.entries.get(&key) {
            Some(Entry::Compiled { osr_entries, osr_open, .. })
                if *osr_open && osr_entries.contains(&entry_pc) => {}
            _ => return None,
        }
        let result = self.enter(key, entry_pc, true, local, allocated)?;
        if let OsrResult::Safepoint(_) = result {
            self.stats.safepoint_exits += 1;
        }
        Some(result)
    }

    /// **The reconstruction**: the interpreter state compiled code for `key` left in the buffer when
    /// it stopped at `pc`.
    ///
    /// Two halves, and each has its own correctness argument.
    ///
    /// The **locals** are `touched_locals`, which is one half: every slot the code can write is a
    /// slot it also declared, so nothing written can be missing. The other half is the **kind**,
    /// which is why this takes a `pc` — whether slot 3 is an `int` or a reference is a property of
    /// *where in the method* execution stopped, and the answer comes from the type map the compiler
    /// computed for exactly this pc. A slot the map calls [`Kind::Opaque`] or [`Kind::Conflict`] is
    /// **skipped**, not written — the first because compiled code provably never touched it, the
    /// second because it touched it two different ways and the slot is dead either way. In both
    /// cases the interpreter's own `Value` survives, which is what keeps the frame a well-typed GC
    /// root without this side having to know which of the two reasons applied.
    ///
    /// The **operand stack** has no such escape hatch: an operand cannot be skipped, since its
    /// position is its identity. It is read bottom-first out of the slots past the locals, where the
    /// deopt stub spilled it, and every position has a kind (nothing in the subset can push a value
    /// it cannot name).
    ///
    /// **This cannot fail, and that is a property rather than a convenience.** It is called only
    /// after native code has returned — after it has written to the heap and after its allocations
    /// have been replayed — and at that point there is no such thing as "never mind". Handing the
    /// caller a `None` there would make it interpret the method from its first byte and apply every
    /// one of those writes a second time, which is exactly the bug step 6 removed.
    ///
    /// So the two ways it used to be able to fail were closed rather than handled:
    ///
    ///  - an operand or an inlined frame with an untypable kind is refused at **compile** time
    ///    ([`Ineligible::Unrebuildable`][crate::burst::compile::Ineligible::Unrebuildable]), so such
    ///    a compilation is never installed;
    ///  - `key` and `site_key` name an entry this cache installed and a site that entry declared —
    ///    the site key is an immediate *this compiler wrote into the stub that just returned it*.
    ///    A miss is memory corruption or a compiler bug, and the honest response to either is to
    ///    stop, not to quietly re-run a method that has already had its effects.
    fn resume_state(&self, key: usize, site_key: u32) -> ResumeState {
        let Some(Entry::Compiled { touched, resume, stack_base, .. }) = self.entries.get(&key) else {
            unreachable!("native code for {key} returned, so {key} is an installed compilation");
        };
        let Some(site) = resume.iter().find(|site| site.key == site_key) else {
            unreachable!("native code returned {site_key}, which is not one of its resume sites");
        };
        let locals = touched
            .iter()
            .zip(&site.locals)
            .filter_map(|(&i, &kind)| JitValue::of(kind, self.scratch[i as usize]).map(|value| (i, value)))
            .collect();
        let stack = site
            .stack
            .iter()
            .enumerate()
            .map(|(k, &kind)| JitValue::of_value(kind, self.scratch[stack_base + k]))
            .collect();
        // **The frames inlining removed** (step 8). Each carries the buffer slot of every value it
        // needs, so this is a read rather than a layout calculation — where a frame's locals and
        // operands live was decided once, by the compiler, and is not re-derived here.
        let inlined = site
            .inlined
            .iter()
            .map(|frame| {
                let read = |&(slot, kind): &(u32, Kind)| JitValue::of_value(kind, self.scratch[slot as usize]);
                VirtualState {
                    unit: frame.unit,
                    pc: frame.pc,
                    locals: frame.locals.iter().map(read).collect(),
                    stack: frame.stack.iter().map(read).collect(),
                }
            })
            .collect();
        ResumeState { pc: site.pc, locals, stack, inlined }
    }

    /// [`Self::resume_state`] plus the one thing every caller wants counted: **how many frames
    /// inlining had removed** at this site. Every exit that hands a state back goes through here,
    /// so [`JitStats::virtual_frames`] cannot drift from what was actually rebuilt.
    fn resumed(&mut self, key: usize, site_key: u32) -> ResumeState {
        let state = self.resume_state(key, site_key);
        self.stats.virtual_frames += state.inlined.len();
        state
    }

    /// **How many interpreter frames a deopt out of `key` can produce** — 1 for a method with
    /// nothing inlined. See [`CompiledCode::frame_depth`] for why the caller must check it against
    /// its own frame limit *before* entering: inlining hides the invokes it expanded, and with them
    /// the depth checks the interpreter would have made at each one.
    ///
    /// A method that is not compiled needs none, which is the answer that makes the caller's check
    /// harmless when the JIT has nothing to offer.
    pub fn frames_needed(&self, key: usize) -> usize {
        match self.entries.get(&key) {
            Some(Entry::Compiled { frame_depth, .. }) => *frame_depth,
            _ => 0,
        }
    }

    /// The one crossing into native code: marshal, call, decode. Both [`run`][JitCache::run] and
    /// [`run_osr`][JitCache::run_osr] go through here so the marshalling contract, the counters and
    /// the `unsafe` block exist once.
    fn enter(
        &mut self,
        key: usize,
        entry_pc: u32,
        on_stack: bool,
        local: impl Fn(u16) -> Option<i64>,
        mut allocated: impl FnMut(usize, usize),
    ) -> Option<OsrResult> {
        if !self.enabled {
            return None;
        }
        let Some(Entry::Compiled {
            native,
            touched,
            slots,
            returns,
            returns_void,
            result_base,
            alloc_base,
            alloc_records,
            ..
        }) = self.entries.get(&key)
        else {
            return None;
        };
        let returns = *returns;
        let returns_void = *returns_void;
        let result_base = *result_base;
        let (alloc_base, alloc_records) = (*alloc_base, *alloc_records);
        debug_assert!(self.scratch.len() >= *slots, "the scratch buffer was sized at install time");
        for &i in touched {
            match local(i) {
                Some(v) => self.scratch[i as usize] = v,
                // A slot the code reads holds something that is not an `int` (an unused `this`,
                // say, that turned out to be used). Fall back rather than reinterpret it.
                None => {
                    self.stats.unmarshallable += 1;
                    return None;
                }
            }
        }
        self.stats.native_calls += 1;
        // Counted here rather than on the way out, so a deopt is still an entry: "how often did
        // the interpreter hand a running loop to native code" is the question this answers.
        self.stats.osr_entries += usize::from(on_stack);
        // The allocation log starts empty. A stale count from the previous excursion would replay
        // objects that no longer exist — and after a collection has recycled Eden, that is a
        // reference to nothing at all logged as live.
        if alloc_records > 0 {
            self.scratch[alloc_base] = 0;
        }
        // SAFETY: `scratch` is a live, initialised `Vec<i64>` of at least `locals` elements (sized
        // in `install`, and every index in `touched` is `< max_locals` by construction — `compile`
        // rejects a local index at or past `max_locals`). The pointer is valid for the duration of
        // the call and is not aliased: the compiled code is the only thing running. `entry_pc` is
        // 0 (the ordinary entry) or, from `run_osr`, checked to be one of this code's own entry
        // points — the two values the entry dispatch is built for.
        let raw = unsafe { native.call(self.scratch.as_mut_ptr(), entry_pc as i64) };
        // **Before anything else**, and on every outcome: replay what native code allocated into the
        // heap's pending log. This is the fourth quarter of an Eden allocation (see the `new` arm in
        // `compile`), deferred exactly as far as it can be and no further — the interpreter has not
        // run an opcode since the call returned, so no collection has had a chance to look at a heap
        // holding objects it does not know about.
        if alloc_records > 0 {
            let count = (self.scratch[alloc_base] as usize).min(alloc_records);
            debug_assert!(
                self.scratch[alloc_base] as usize <= alloc_records,
                "compiled code logged more allocations than the buffer holds"
            );
            for r in 0..count {
                let at = alloc_base + 1 + 2 * r;
                allocated(self.scratch[at] as usize, self.scratch[at + 1] as usize);
            }
        }
        match Status::unpack(raw) {
            // The value is in the buffer's **result slot**, not in the status word — that is the
            // boundary contract the `long` work rests on. The descriptor decided which kind it is,
            // back when the method was compiled; `JitValue::of` never sees `Opaque` here because a
            // method that returns a `float` or a `double` has no exit in the subset and never
            // compiled.
            Outcome::Returned => match returns_void {
                // A `void` method's `return` writes nothing to the result slot, so reading it would
                // hand the interpreter whatever the previous excursion left there.
                true => Some(OsrResult::Returned(None)),
                false => {
                    let raw = self.scratch[result_base];
                    Some(OsrResult::Returned(Some(JitValue::of_value(returns, raw))))
                }
            },
            Outcome::Safepoint(pc) => Some(OsrResult::Safepoint(self.resumed(key, pc))),
            Outcome::Deopt(pc) => {
                self.stats.deopts += 1;
                // A deopt out of an on-stack entry closes OSR for this method for good — see
                // `Entry::Compiled::osr_open`. Keyed off *how* it was entered, not off the pc: a
                // loop header can perfectly well be pc 0.
                if on_stack {
                    if let Some(Entry::Compiled { osr_open, .. }) = self.entries.get_mut(&key) {
                        *osr_open = false;
                    }
                }
                Some(OsrResult::Deopt(self.resumed(key, pc)))
            }
            // A `new` that could not take its fast path. The state contract is a deopt's, so the
            // caller is told the same thing; what is deliberately *not* done here is the two things
            // a real deopt does. It does not close on-stack entry — Eden fills once per collection
            // cycle and the log fills once per 256 objects, so closing OSR on either would retire
            // every allocating loop after one lap, for a condition that clears on its own. And it
            // does not count as a deopt: "this method keeps failing a guard" and "this loop keeps
            // filling Eden" are different facts, and a measurement that conflates them is worse than
            // one that omits them.
            Outcome::AllocFailed(pc) => {
                self.stats.alloc_exits += 1;
                Some(OsrResult::Deopt(self.resumed(key, pc)))
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Compiles `code` as a `static` method of `max_locals` slots with the descriptor `signature`,
    /// against `c`'s poll word. Every program in this file is `int`-only, so no heap is supplied —
    /// which also means a stray reference opcode would be refused rather than silently emitted.
    #[cfg(windows)]
    fn compiled(c: &JitCache, code: &[u8], max_locals: usize, signature: &str) -> CompiledCode {
        use super::super::compile::{Environment, Heap, Method};
        super::super::compile::compile(
            &Method { unit: 0, code, max_locals, descriptor: signature, is_static: true, has_handlers: false },
            &Environment {
                int_const: &|_, _| None,
                long_const: &|_, _| None,
                float_const: &|_, _| None,
                double_const: &|_, _| None,
                static_field: &|_, _| None,
                field: &|_, _, _| None,
                instance: &|_, _| None,
                array: &|_, _| None,
                invoke: &|_, _, _| None,
                heap: Heap::default(),
                class_mirror: &|_, _| None,
                poll_word: c.poll_address(),
            },
        )
        .unwrap()
    }

    /// `iload_0; iload_1; iadd; ireturn` — the smallest thing with locals in it.
    #[cfg(windows)]
    fn add_two(c: &JitCache) -> CompiledCode {
        compiled(c, &[0x1a, 0x1b, 0x60, 0xac], 2, "(II)I")
    }

    /// A counting loop over local 0, entered on-stack at its header:
    ///
    /// ```text
    ///  0: iconst_0; istore_0                      i = 0
    ///  2: iload_0; iload_1; if_icmpge -> 13       <- the loop header, stack empty
    ///  7: iinc 0, 1
    /// 10: goto -> 2                               the back-edge
    /// 13: iload_0; ireturn                        returns the trip count
    /// ```
    ///
    /// Local 1 is the trip count, so one compiled function serves both "runs to completion" and
    /// "runs long enough for the poll to fire".
    #[cfg(windows)]
    fn count_to(c: &JitCache) -> CompiledCode {
        let code = [
            0x03, 0x3b, // 0: iconst_0; istore_0
            0x1a, 0x1b, 0xa2, 0x00, 0x09, // 2: iload_0; iload_1; if_icmpge +9 -> 13
            0x84, 0x00, 0x01, // 7: iinc 0, 1
            0xa7, 0xff, 0xf8, // 10: goto -8 -> 2
            0x1a, 0xac, // 13: iload_0; ireturn
        ];
        compiled(c, &code, 2, "(II)I")
    }

    /// The allocation sink for the programs below, none of which contains a `new` — so it is not
    /// merely unused, it is *asserted* unused: a call would mean compiled code allocated something
    /// this test never asked for.
    fn no_allocations(offset: usize, size: usize) {
        panic!("this program allocates nothing, but native code logged ({offset}, {size})");
    }

    fn cache() -> JitCache {
        let mut c = JitCache::from_env();
        c.set_enabled(true);
        c
    }

    /// The ordinary "it ran to its `ireturn`" answer, so the assertions below read as values.
    fn returned(v: i32) -> OsrResult {
        OsrResult::Returned(Some(JitValue::Int(v)))
    }

    #[test]
    fn a_method_is_interpreted_until_it_is_warm() {
        let mut c = cache();
        for i in 1..c.threshold {
            assert_eq!(c.on_entry(7), Decision::Interpret, "call {i}");
        }
        assert_eq!(c.on_entry(7), Decision::Compile, "the {}th call is the warm one", c.threshold);
    }

    #[test]
    fn the_scan_is_claimed_exactly_once() {
        let mut c = cache();
        for _ in 0..c.threshold {
            c.on_entry(7);
        }
        // The compile was asked for and never answered: the method stays interpreted forever
        // rather than being rescanned on every subsequent call.
        for _ in 0..100 {
            assert_eq!(c.on_entry(7), Decision::Interpret);
        }
        assert_eq!(c.stats().rejected, 1);
        assert_eq!(c.stats().compiled, 0);
    }

    #[test]
    fn an_ineligible_method_is_refused_permanently() {
        let mut c = cache();
        for _ in 0..c.threshold {
            c.on_entry(7);
        }
        c.install(7, Err(Ineligible::TooBig), 0);
        for _ in 0..100 {
            assert_eq!(c.on_entry(7), Decision::Interpret);
        }
        assert_eq!(c.stats().rejected, 1);
    }

    #[test]
    fn disabling_the_jit_stops_everything() {
        let mut c = cache();
        c.set_enabled(false);
        for _ in 0..1000 {
            assert_eq!(c.on_entry(7), Decision::Interpret);
        }
        assert_eq!(c.run(7, |_| Some(0), no_allocations), None);
        assert_eq!(c.stats(), JitStats::default());
    }

    #[cfg(windows)]
    #[test]
    fn a_compiled_method_runs_and_marshals_only_what_it_reads() {
        let mut c = cache();
        for _ in 0..c.threshold {
            c.on_entry(7);
        }
        let code = add_two(&c);
        c.install(7, Ok(code), 2);
        assert_eq!(c.on_entry(7), Decision::Ready);
        assert_eq!(c.run(7, |i| Some(i as i64 * 10 + 1), no_allocations), Some(returned(1 + 11)));
        assert_eq!(c.stats().compiled, 1);
        assert_eq!(c.stats().rejected, 0);
        assert_eq!(c.stats().native_calls, 1);
    }

    #[cfg(windows)]
    #[test]
    fn a_local_that_cannot_be_marshalled_falls_back_to_the_interpreter() {
        let mut c = cache();
        for _ in 0..c.threshold {
            c.on_entry(7);
        }
        let code = add_two(&c);
        c.install(7, Ok(code), 2);
        // Slot 1 holds something that is not an int (a reference, a long, a double...).
        assert_eq!(c.run(7, |i| (i != 1).then_some(3), no_allocations), None);
        assert_eq!(c.stats().unmarshallable, 1);
        assert_eq!(c.stats().native_calls, 0);
    }

    #[cfg(windows)]
    #[test]
    fn division_by_zero_deopts_with_the_state_to_resume_from() {
        // iload_0; iload_1; idiv; ireturn. Without the emitted zero check this call would raise
        // #DE, which on Windows is a structured exception that would take the process down rather
        // than throw ArithmeticException.
        //
        // And what comes back is step 6 in one assertion: not "give up", but **pc 2 with both
        // operands on the stack** — exactly the state an interpreter needs to execute that `idiv`
        // itself and throw. Before this step the answer was a bare `None` and the whole method was
        // re-run from its first byte.
        let mut c = cache();
        for _ in 0..c.threshold {
            c.on_entry(7);
        }
        let code = compiled(&c, &[0x1a, 0x1b, 0x6c, 0xac], 2, "(II)I");
        c.install(7, Ok(code), 2);
        assert_eq!(c.run(7, |i| Some(if i == 0 { 100 } else { 7 }), no_allocations), Some(returned(14)));
        let out = c.run(7, |i| Some(if i == 0 { 100 } else { 0 }), no_allocations);
        assert_eq!(
            out,
            Some(OsrResult::Deopt(ResumeState {
                pc: 2,
                locals: vec![(0, JitValue::Int(100)), (1, JitValue::Int(0))],
                stack: vec![JitValue::Int(100), JitValue::Int(0)],
                inlined: Vec::new(),
            }))
        );
        assert_eq!(c.stats().deopts, 1);
        assert_eq!(c.stats().native_calls, 2);
    }

    #[cfg(windows)]
    #[test]
    fn a_conflicted_slot_is_absent_from_the_state_rather_than_guessed_at() {
        // **The write-back half of step 9, asked directly.** A local whose kind two paths disagree
        // about is `Kind::Conflict`, and what the interpreter must be handed for it is *nothing* —
        // no entry in `ResumeState::locals` at all — so that its frame keeps the `Value` it already
        // had. There is no third option that is a value: labelling the slot `Int` would hand back a
        // heap offset the collector can no longer see, and labelling it `Reference` would hand back
        // a pointer made of arithmetic. Both fail somewhere else, later, as corruption.
        //
        //  0: iload_0; ifeq -> 6      slot 2 keeps the `int` a fresh frame put there
        //  4: aload_1; astore_2       ...and holds a reference on the other path
        //  6: bipush 100; iload_0     <- the merge: slot 2 is `Conflict` from here
        //  9: idiv                    <- the deopt, with slot 2 still conflicted
        // 10: ireturn
        let mut c = cache();
        for _ in 0..c.threshold {
            c.on_entry(9);
        }
        let code = [
            0x1a, 0x99, 0x00, 0x05, // 0: iload_0; ifeq +5 -> 6
            0x2b, 0x4d, // 4: aload_1; astore_2
            0x10, 100, 0x1a, 0x6c, // 6: bipush 100; iload_0; idiv
            0xac, // 10: ireturn
        ];
        c.install(9, Ok(compiled(&c, &code, 3, "(ILjava/lang/Object;)I")), 3);
        // Local 0 is the zero divisor *and* the branch flag, so this call takes the path that never
        // writes slot 2 — and the assertion below is that the answer does not depend on which path
        // it took, because the map says `Conflict` either way.
        let out = c.run(9, |i| Some([0, 264, 4242][i as usize]), no_allocations);
        assert_eq!(
            out,
            Some(OsrResult::Deopt(ResumeState {
                pc: 9,
                // Slot 2 is **not here**, and that is the whole test. Slots 0 and 1 are, with the
                // kinds the map gives them — a reference still arriving as a `Reference`, which is
                // the assertion that would catch a lattice change that flattened everything to
                // "unknown" and stopped writing anything back at all.
                locals: vec![(0, JitValue::Int(0)), (1, JitValue::Reference(264))],
                stack: vec![JitValue::Int(100), JitValue::Int(0)],
                inlined: Vec::new(),
            }))
        );
    }

    // -- On-stack replacement and the safepoint poll -------------------------------------------

    /// A cache holding `count_to` at key 7, already compiled.
    #[cfg(windows)]
    fn warm_loop() -> JitCache {
        let mut c = cache();
        for _ in 0..c.threshold {
            c.on_back_edge(7);
        }
        let code = count_to(&c);
        c.install(7, Ok(code), 2);
        c
    }

    #[cfg(windows)]
    #[test]
    fn back_edges_make_a_method_hot_without_a_single_call() {
        // The whole point of step 3: `on_entry` is never called here, and the method still
        // compiles. This is `BmLoop.run`'s shape — entered once, hot only from the inside.
        let mut c = cache();
        for i in 1..c.threshold {
            assert_eq!(c.on_back_edge(7), Decision::Interpret, "back-edge {i}");
        }
        assert_eq!(c.on_back_edge(7), Decision::Compile);
        let code = count_to(&c);
        c.install(7, Ok(code), 2);
        assert_eq!(c.on_back_edge(7), Decision::Ready);
    }

    #[cfg(windows)]
    #[test]
    fn an_on_stack_entry_finishes_the_method_from_the_middle_of_its_loop() {
        let mut c = warm_loop();
        // Enter at the loop header with i already at 5 and the bound at 9. Native code must run
        // the remaining four iterations and return 9 — not restart from `i = 0`, which is the one
        // thing an entry-point mix-up would look like.
        let out = c.run_osr(7, 2, |i| Some(if i == 0 { 5 } else { 9 }), no_allocations);
        assert_eq!(out, Some(returned(9)));
        assert_eq!(c.stats().osr_entries, 1);
        assert_eq!(c.stats().native_calls, 1);
        assert_eq!(c.stats().safepoint_exits, 0);
    }

    #[cfg(windows)]
    #[test]
    fn only_a_real_entry_point_is_entered() {
        let mut c = warm_loop();
        // pc 7 is the `iinc` — a real instruction, but not a loop header, so not an entry point.
        // Falling through the dispatch would run the method from pc 0 and answer 9 instead.
        assert_eq!(c.run_osr(7, 7, |_| Some(0), no_allocations), None);
        assert_eq!(c.stats().native_calls, 0, "nothing must have been entered");
    }

    #[cfg(windows)]
    #[test]
    fn the_poll_brings_the_method_back_with_its_locals_and_a_pc() {
        let mut c = warm_loop();
        // Raise the poll *before* entering: the first time the loop comes round to its header the
        // check fires, so exactly one iteration runs natively.
        c.poll_word().store(1, std::sync::atomic::Ordering::Release);
        let out = c.run_osr(7, 2, |i| Some(if i == 0 { 5 } else { 1_000_000 }), no_allocations);
        // The state it comes back with is the state the interpreter has to resume from: local 0
        // advanced by exactly the one iteration that ran, the pc at the loop header, and an empty
        // operand stack — which is what a loop header being an entry point *means*.
        assert_eq!(
            out,
            Some(OsrResult::Safepoint(ResumeState {
                pc: 2,
                locals: vec![(0, JitValue::Int(6)), (1, JitValue::Int(1_000_000))],
                stack: Vec::new(),
                inlined: Vec::new(),
            })),
            "it must come back at the loop header"
        );
        assert_eq!(c.stats().safepoint_exits, 1);

        // Lower it again and the very same code runs the loop to the end — the poll is a
        // condition, not a mode.
        c.poll_word().store(0, std::sync::atomic::Ordering::Release);
        let out = c.run_osr(7, 2, |i| Some(if i == 0 { 6 } else { 9 }), no_allocations);
        assert_eq!(out, Some(returned(9)));
        assert_eq!(c.stats().safepoint_exits, 1);
    }

    #[cfg(windows)]
    #[test]
    fn the_poll_word_is_this_cache_s_own() {
        // Two caches must not be able to pull each other out of native code — the property that
        // makes many VMs in one process (a test binary, say) independent.
        let (a, b) = (cache(), cache());
        assert_ne!(a.poll_address(), b.poll_address());
        a.poll_word().store(1, std::sync::atomic::Ordering::Release);
        assert_eq!(b.poll_word().load(std::sync::atomic::Ordering::Acquire), 0);
    }

    #[cfg(windows)]
    #[test]
    fn a_deopt_out_of_a_loop_header_closes_osr_for_good() {
        // `iload_0; iload_1; idiv; istore_0; goto -> 0` — a loop whose body divides, entered at
        // pc 0 (its own header) with a zero divisor. The first attempt deopts and hands back a
        // resumable state; after that the method stops being offered for on-stack entry, so a
        // guard that fails on entry cannot make every back-edge pay for a marshal-and-enter.
        //
        //  0: iload_0; iload_1; idiv; istore_0   <- header (depth 0)
        //  4: goto -4 -> 0
        let mut c = cache();
        for _ in 0..c.threshold {
            c.on_back_edge(7);
        }
        let code = compiled(&c, &[0x1a, 0x1b, 0x6c, 0x3b, 0xa7, 0xff, 0xfc], 2, "(II)I");
        assert_eq!(code.osr_entries, vec![0]);
        // pc 0 is both a loop header *and* — through the `idiv` at pc 2 — a method with a deopt
        // site, so the resume map holds both, in pc order.
        assert_eq!(code.resume_sites.iter().map(|s| s.pc).collect::<Vec<_>>(), vec![0, 2]);
        c.install(7, Ok(code), 2);
        assert!(c.watches_back_edges(7));
        let out = c.run_osr(7, 0, |i| Some(if i == 0 { 10 } else { 0 }), no_allocations);
        assert_eq!(
            out,
            Some(OsrResult::Deopt(ResumeState {
                pc: 2,
                locals: vec![(0, JitValue::Int(10)), (1, JitValue::Int(0))],
                stack: vec![JitValue::Int(10), JitValue::Int(0)],
                inlined: Vec::new(),
            }))
        );
        assert_eq!(c.stats().deopts, 1);
        assert!(!c.watches_back_edges(7), "OSR is closed after a deopt from a loop header");
        assert_eq!(c.run_osr(7, 0, |i| Some(if i == 0 { 10 } else { 2 }), no_allocations), None, "and stays closed");
        assert_eq!(c.stats().native_calls, 1, "the second attempt never entered");
    }

    #[cfg(windows)]
    #[test]
    fn a_method_without_an_eligible_loop_is_never_watched_again() {
        let mut c = cache();
        for _ in 0..c.threshold {
            c.on_entry(7);
        }
        let code = add_two(&c);
        assert!(code.osr_entries.is_empty());
        c.install(7, Ok(code), 2);
        assert!(!c.watches_back_edges(7), "no entry point, so back-edges here are not worth a probe");
    }

    #[test]
    fn a_rejected_method_is_never_watched_again() {
        let mut c = cache();
        assert!(c.watches_back_edges(7), "an unseen method might still turn out to be hot");
        for _ in 0..c.threshold {
            c.on_back_edge(7);
        }
        c.install(7, Err(Ineligible::TooBig), 0);
        assert!(!c.watches_back_edges(7));
        c.set_enabled(false);
        assert!(!c.watches_back_edges(9), "and nothing at all is watched with the JIT off");
    }

    // -----------------------------------------------------------------------------------------
    // Step 7: the allocation log the trampoline replays, and the exit that is not a deopt.
    // -----------------------------------------------------------------------------------------

    /// A stand-in Eden: a byte buffer and a boxed cursor, shaped exactly as `HeapService` hands the
    /// real ones to the compiler. Boxed for the same reason the arena boxes its own — a compiled
    /// `new` bakes the cursor's address in.
    #[cfg(windows)]
    struct TestEden {
        bytes: Vec<u8>,
        cursor: Box<std::sync::atomic::AtomicUsize>,
    }

    #[cfg(windows)]
    impl TestEden {
        const NULL_PAGE: usize = 8;

        fn new(size: usize) -> TestEden {
            TestEden {
                bytes: vec![0; size],
                cursor: Box::new(std::sync::atomic::AtomicUsize::new(0)),
            }
        }

        fn heap(&self) -> super::super::compile::Heap {
            super::super::compile::Heap {
                eden_base: self.bytes.as_ptr() as usize - Self::NULL_PAGE,
                other_base: self.bytes.as_ptr() as usize,
                eden_end: (Self::NULL_PAGE + self.bytes.len()) as u32,
                max_offset: Self::NULL_PAGE + self.bytes.len(),
                eden_cursor: &*self.cursor as *const _ as usize,
                eden_capacity: self.bytes.len(),
                null_page: Self::NULL_PAGE as u32,
                array_length: 8,
                array_data: 12,
                int_element: 4,
            }
        }
    }

    /// `compiled` for a program that allocates: every `new` resolves to a 16-byte instance.
    #[cfg(windows)]
    fn compiled_alloc(c: &JitCache, code: &[u8], max_locals: usize, signature: &str, eden: &TestEden) -> CompiledCode {
        use super::super::compile::{Environment, Instance, Method};
        super::super::compile::compile(
            &Method { unit: 0, code, max_locals, descriptor: signature, is_static: true, has_handlers: false },
            &Environment {
                int_const: &|_, _| None,
                long_const: &|_, _| None,
                float_const: &|_, _| None,
                double_const: &|_, _| None,
                static_field: &|_, _| None,
                field: &|_, _, _| None,
                instance: &|_, _| Some(Instance { size: 16, class_id: 0x77 }),
                array: &|_, _| None,
                invoke: &|_, _, _| None,
                heap: eden.heap(),
                class_mirror: &|_, _| None,
                poll_word: c.poll_address(),
            },
        )
        .unwrap()
    }

    /// **The trampoline's third clause**: every object native code allocated is handed to the
    /// caller, exactly once, in allocation order — because until that happens the collector cannot
    /// see any of them.
    #[test]
    #[cfg(windows)]
    fn the_cache_hands_back_every_object_native_code_allocated() {
        let mut c = cache();
        let eden = TestEden::new(4096);
        // new #1; astore_0; new #1; astore_0; aload_0; areturn — two objects, one excursion.
        let code = [0xbb, 0, 1, 0x4b, 0xbb, 0, 1, 0x4b, 0x2a, 0xb0];
        let program = compiled_alloc(&c, &code, 1, "()LCell;", &eden);
        assert_eq!(program.alloc_records, super::super::compile::ALLOC_LOG_RECORDS);
        c.install(7, Ok(program), 1);

        let mut seen: Vec<(usize, usize)> = Vec::new();
        let out = c.run(7, |_| Some(0), |offset, size| seen.push((offset, size)));
        assert!(matches!(out, Some(OsrResult::Returned(Some(JitValue::Reference(_))))));
        assert_eq!(
            seen,
            vec![(TestEden::NULL_PAGE, 16), (TestEden::NULL_PAGE + 16, 16)],
            "both objects, in allocation order, with their logical sizes"
        );

        // And the count is **reset** between excursions: a second call must report its own two
        // objects, not four. A stale count here would replay references to objects that a
        // collection has since recycled — the one bookkeeping mistake that is silent.
        seen.clear();
        let _ = c.run(7, |_| Some(0), |offset, size| seen.push((offset, size)));
        assert_eq!(seen, vec![(TestEden::NULL_PAGE + 32, 16), (TestEden::NULL_PAGE + 48, 16)]);
    }

    /// A full Eden is **not** a deopt: it does not count as one and it does not close on-stack
    /// entry. Both matter for a real allocating loop, which fills Eden once per collection cycle
    /// and would otherwise be retired from native code after its first lap.
    #[test]
    #[cfg(windows)]
    fn a_full_eden_leaves_without_closing_on_stack_entry() {
        let mut c = cache();
        // Eden holds exactly two 16-byte objects, so the loop's third iteration cannot allocate.
        let eden = TestEden::new(32);
        //  0: iconst_0; istore_0
        //  2: iload_0; bipush 10; if_icmpge -> 18   <- the loop header
        //  8: new #1; pop
        // 12: iinc 0, 1
        // 15: goto -> 2
        // 18: iload_0; ireturn
        let code = [
            0x03, 0x3b, //
            0x1a, 0x10, 10, 0xa2, 0x00, 0x0d, // 2
            0xbb, 0x00, 0x01, 0x57, // 8
            0x84, 0x00, 0x01, // 12
            0xa7, 0xff, 0xf3, // 15
            0x1a, 0xac, // 18
        ];
        let program = compiled_alloc(&c, &code, 1, "()I", &eden);
        assert_eq!(program.osr_entries, vec![2], "the loop header");
        c.install(7, Ok(program), 1);

        let mut seen = 0usize;
        let out = c.run_osr(7, 2, |_| Some(0), |_, _| seen += 1);
        // The state contract is a deopt's — the interpreter resumes at the `new` that did not run.
        match out {
            Some(OsrResult::Deopt(state)) => assert_eq!(state.pc, 8),
            other => panic!("expected a resume at the `new`, got {other:?}"),
        }
        assert_eq!(seen, 2, "the two objects that did fit were still handed over");
        assert_eq!(c.stats().alloc_exits, 1);
        assert_eq!(c.stats().deopts, 0, "a full Eden is a capacity condition, not a failed guard");
        // The decisive one: on-stack entry is still open, so the loop can be re-entered as soon as
        // a collection has recycled Eden.
        assert!(c.watches_back_edges(7), "an alloc exit must not retire the loop");
    }

}
