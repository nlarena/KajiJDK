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
//! its **safepoint poll** fired and it wants to be interpreted from a given pc. The state crossing
//! the boundary in both directions is only the locals buffer and a pc, which is what makes the
//! whole thing small enough to be obviously right.
//!
//! # Crossing the boundary
//!
//! The interpreter's locals are `Vec<Value>` — a tagged enum whose layout is not this module's
//! business. So the crossing **marshals**: the caller copies the locals the compiled code actually
//! reads (its [`CompiledCode::touched_locals`]) into a flat `[i64]` scratch buffer, calls, and uses
//! the result. Cost is O(touched locals) per invocation, amortised over the loop inside — which is
//! precisely where a JIT wins.
//!
//! The marshalling is also a **guard**: a local that is not a `Value::Int` cannot be marshalled, so
//! the call is simply abandoned and the interpreter runs the method. That is what makes an instance
//! method safe to compile — slot 0 holds a `Value::Reference`, and if the body ever reads it the
//! call falls back instead of reinterpreting a heap offset as an `int`.
//!
//! Nothing is marshalled **back**. A compiled method is a pure function of its locals: its only
//! observable effect is the value it returns, so the interpreter frame it was built from is
//! discarded untouched. That is also what makes deopt free — see [`compile`][super::compile].

use std::collections::HashMap;
use std::sync::atomic::AtomicU64;
use std::sync::Arc;

use super::compile::{CompiledCode, Ineligible, Outcome, Status};

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
    /// Compiled. `locals` is the callee's `max_locals`, i.e. how long the scratch buffer must be.
    Compiled {
        native: native::Native,
        touched: Vec<u16>,
        locals: usize,
        /// The bytecode pcs this code may be entered at on-stack (its loop headers), ascending.
        osr_entries: Vec<u32>,
        /// Cleared for good the first time an OSR entry **deopts**.
        ///
        /// A deopt from a loop header is safe — nothing observable was written, so the interpreter
        /// simply carries on from that same pc — but it is also *reproducible*: the native code
        /// would run the same iterations and give up in the same place on the next attempt, while
        /// the interpreter creeps forward one iteration per try. That is quadratic work for no
        /// progress, so the first one closes the door. The ordinary entry is untouched: a deopt
        /// there re-runs the whole method, which converges.
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

/// How an on-stack entry ended. The two ways native code can hand control back *without* the
/// method being over are folded into the one type the interpreter matches on.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum OsrResult {
    /// The method ran to its `ireturn`; this is its result and its frame is finished.
    Returned(i32),
    /// The safepoint poll fired. Write the locals back
    /// ([`osr_writeback`][JitCache::osr_writeback]) and resume **interpreting** at this pc.
    Safepoint(u32),
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
}

/// One thread's compiled methods, invocation counters and scratch buffer.
///
/// Keyed by an opaque `usize` — the interpreter's `MethodId`. `burst` does not depend on
/// `crate::jvm`, so the key stays a number.
pub struct JitCache {
    enabled: bool,
    threshold: u32,
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

    /// A cache configured from the environment.
    ///
    /// - `JVM_JIT=0` (or `off`/`false`) disables the JIT completely: no counters, no scans, no
    ///   native code. **The default is on.** This is the switch the differential tests and any
    ///   bisection use — with it off the VM is bit-for-bit the interpreter it was before.
    /// - `JVM_JIT_THRESHOLD=<n>` overrides [`JitCache::THRESHOLD`].
    pub fn from_env() -> Self {
        let enabled = !matches!(
            std::env::var("JVM_JIT").ok().as_deref().map(str::trim),
            Some("0" | "off" | "false" | "no")
        );
        let threshold = std::env::var("JVM_JIT_THRESHOLD")
            .ok()
            .and_then(|v| v.trim().parse::<u32>().ok())
            .unwrap_or(Self::THRESHOLD);
        JitCache {
            enabled,
            threshold,
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
        // The scratch buffer must cover every slot the code can address. `max(1)` because an empty
        // `Vec`'s `as_mut_ptr` is dangling, and a dangling pointer is not worth reasoning about
        // even when nothing dereferences it.
        self.scratch.resize(self.scratch.len().max(max_locals).max(1), 0);
        self.entries.insert(
            key,
            Entry::Compiled {
                native,
                touched: compiled.touched_locals,
                locals: max_locals,
                osr_entries: compiled.osr_entries,
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
    /// `local(i)` yields the interpreter's local slot `i` as an `i64` — `Some(v as i64)` for a
    /// `Value::Int(v)`, and **`None` for anything else**, which abandons the call.
    ///
    /// Returns `Some(result)` when native code ran to an `ireturn`, and `None` in every case where
    /// the interpreter must run the method itself: not compiled, a local that could not be
    /// marshalled, or a deopt. All three are safe for the same reason — the compiled subset writes
    /// nothing observable, so re-executing from the start is indistinguishable from never having
    /// tried.
    pub fn run(&mut self, key: usize, local: impl Fn(u16) -> Option<i64>) -> Option<i32> {
        match self.enter(key, 0, false, local)? {
            OsrResult::Returned(value) => Some(value),
            // A poll that fires during an ordinary call is simply ignored: the state at a loop
            // header of a method entered at its start is a state the interpreter can reproduce by
            // re-running the method, so this is the deopt path, and `enter` has already counted it
            // as one. Not a case that arises today (nothing raises the poll while `green`/`os-gil`
            // hold the world), but the answer has to be *some* correct answer rather than a panic.
            OsrResult::Safepoint(_) => None,
        }
    }

    /// Enters `key`'s compiled code **at a loop header**, marshalling its locals through `local`.
    ///
    /// `entry_pc` must be one of the code's own entry points; anything else answers `None` rather
    /// than entering, because the entry dispatch would otherwise fall through and run the method
    /// from its start with mid-loop locals. `None` also covers every other reason not to enter —
    /// not compiled, OSR closed, an unmarshallable local, a deopt — and all of them mean the same
    /// thing to the caller: **keep interpreting at this pc**, which is safe because the compiled
    /// subset writes nothing observable and the interpreter's own frame has not been touched.
    ///
    /// On [`OsrResult::Safepoint`] the caller must copy the locals back with
    /// [`osr_writeback`][JitCache::osr_writeback] and resume at the pc it carries.
    pub fn run_osr(
        &mut self,
        key: usize,
        entry_pc: u32,
        local: impl Fn(u16) -> Option<i64>,
    ) -> Option<OsrResult> {
        match self.entries.get(&key) {
            Some(Entry::Compiled { osr_entries, osr_open, .. })
                if *osr_open && osr_entries.contains(&entry_pc) => {}
            _ => return None,
        }
        let result = self.enter(key, entry_pc, true, local)?;
        if let OsrResult::Safepoint(_) = result {
            self.stats.safepoint_exits += 1;
        }
        Some(result)
    }

    /// The locals the compiled code for `key` may have written, paired with their current values
    /// in the marshalling buffer — what [`OsrResult::Safepoint`] leaves behind for the interpreter
    /// to put back into its frame.
    ///
    /// It is exactly `touched_locals`, and that is the whole correctness argument: every slot the
    /// code can write is a slot it also declared, so writing all of them back is neither too few
    /// (a written slot cannot be missing) nor too many (an unwritten one is copied back unchanged,
    /// having been marshalled *in* from that same frame moments earlier).
    pub fn osr_writeback(&self, key: usize) -> impl Iterator<Item = (u16, i64)> + '_ {
        let touched: &[u16] = match self.entries.get(&key) {
            Some(Entry::Compiled { touched, .. }) => touched,
            _ => &[],
        };
        touched.iter().map(|&i| (i, self.scratch[i as usize]))
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
    ) -> Option<OsrResult> {
        if !self.enabled {
            return None;
        }
        let Some(Entry::Compiled { native, touched, locals, .. }) = self.entries.get(&key) else {
            return None;
        };
        debug_assert!(self.scratch.len() >= *locals, "the scratch buffer was sized at install time");
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
        // SAFETY: `scratch` is a live, initialised `Vec<i64>` of at least `locals` elements (sized
        // in `install`, and every index in `touched` is `< max_locals` by construction — `compile`
        // rejects a local index at or past `max_locals`). The pointer is valid for the duration of
        // the call and is not aliased: the compiled code is the only thing running. `entry_pc` is
        // 0 (the ordinary entry) or, from `run_osr`, checked to be one of this code's own entry
        // points — the two values the entry dispatch is built for.
        let raw = unsafe { native.call(self.scratch.as_mut_ptr(), entry_pc as i64) };
        match Status::unpack(raw) {
            Outcome::Returned(value) => Some(OsrResult::Returned(value)),
            Outcome::Safepoint(pc) => Some(OsrResult::Safepoint(pc)),
            Outcome::Deopt => {
                self.stats.deopts += 1;
                // A deopt out of an on-stack entry closes OSR for this method for good — see
                // `Entry::Compiled::osr_open` for why re-entering would be quadratic. Keyed off
                // *how* it was entered, not off the pc: a loop header can perfectly well be pc 0.
                if on_stack {
                    if let Some(Entry::Compiled { osr_open, .. }) = self.entries.get_mut(&key) {
                        *osr_open = false;
                    }
                }
                None
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// `iload_0; iload_1; iadd; ireturn` — the smallest thing with locals in it.
    #[cfg(windows)]
    fn add_two(c: &JitCache) -> CompiledCode {
        super::super::compile::compile(&[0x1a, 0x1b, 0x60, 0xac], 2, &|_| None, &|_| None, c.poll_address())
            .unwrap()
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
        super::super::compile::compile(&code, 2, &|_| None, &|_| None, c.poll_address()).unwrap()
    }

    fn cache() -> JitCache {
        let mut c = JitCache::from_env();
        c.set_enabled(true);
        c
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
        assert_eq!(c.run(7, |_| Some(0)), None);
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
        assert_eq!(c.run(7, |i| Some(i as i64 * 10 + 1)), Some(1 + 11));
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
        assert_eq!(c.run(7, |i| (i != 1).then_some(3)), None);
        assert_eq!(c.stats().unmarshallable, 1);
        assert_eq!(c.stats().native_calls, 0);
    }

    #[cfg(windows)]
    #[test]
    fn division_by_zero_deopts_instead_of_faulting() {
        // iload_0; iload_1; idiv; ireturn — the one deopt site this tier has. Without the emitted
        // zero check this call would raise #DE, which on Windows is a structured exception that
        // would take the process down rather than throw ArithmeticException.
        let mut c = cache();
        for _ in 0..c.threshold {
            c.on_entry(7);
        }
        let code =
            super::super::compile::compile(&[0x1a, 0x1b, 0x6c, 0xac], 2, &|_| None, &|_| None, c.poll_address())
                .unwrap();
        c.install(7, Ok(code), 2);
        assert_eq!(c.run(7, |i| Some(if i == 0 { 100 } else { 7 })), Some(14));
        assert_eq!(c.run(7, |i| Some(if i == 0 { 100 } else { 0 })), None);
        assert_eq!(c.stats().deopts, 1);
        assert_eq!(c.stats().native_calls, 2);
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
        let out = c.run_osr(7, 2, |i| Some(if i == 0 { 5 } else { 9 }));
        assert_eq!(out, Some(OsrResult::Returned(9)));
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
        assert_eq!(c.run_osr(7, 7, |_| Some(0)), None);
        assert_eq!(c.stats().native_calls, 0, "nothing must have been entered");
    }

    #[cfg(windows)]
    #[test]
    fn the_poll_brings_the_method_back_with_its_locals_and_a_pc() {
        let mut c = warm_loop();
        // Raise the poll *before* entering: the first time the loop comes round to its header the
        // check fires, so exactly one iteration runs natively.
        c.poll_word().store(1, std::sync::atomic::Ordering::Release);
        let out = c.run_osr(7, 2, |i| Some(if i == 0 { 5 } else { 1_000_000 }));
        assert_eq!(out, Some(OsrResult::Safepoint(2)), "it must come back at the loop header");
        assert_eq!(c.stats().safepoint_exits, 1);
        // And the state it comes back with is the state the interpreter has to resume from: local
        // 0 advanced by exactly the one iteration that ran.
        let written: Vec<(u16, i64)> = c.osr_writeback(7).collect();
        assert_eq!(written, vec![(0, 6), (1, 1_000_000)]);

        // Lower it again and the very same code runs the loop to the end — the poll is a
        // condition, not a mode.
        c.poll_word().store(0, std::sync::atomic::Ordering::Release);
        let out = c.run_osr(7, 2, |i| Some(if i == 0 { 6 } else { 9 }));
        assert_eq!(out, Some(OsrResult::Returned(9)));
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
        // pc 0 (its own header) with a zero divisor. The first attempt deopts; after that the
        // method must stop being offered for on-stack entry, or the interpreter and the JIT would
        // take turns making one iteration of progress each.
        //
        //  0: iload_0; iload_1; idiv; istore_0   <- header (depth 0)
        //  4: goto -4 -> 0
        let mut c = cache();
        for _ in 0..c.threshold {
            c.on_back_edge(7);
        }
        let code = super::super::compile::compile(
            &[0x1a, 0x1b, 0x6c, 0x3b, 0xa7, 0xff, 0xfc],
            2,
            &|_| None,
            &|_| None,
            c.poll_address(),
        )
        .unwrap();
        assert_eq!(code.osr_entries, vec![0]);
        c.install(7, Ok(code), 2);
        assert!(c.watches_back_edges(7));
        assert_eq!(c.run_osr(7, 0, |i| Some(if i == 0 { 10 } else { 0 })), None);
        assert_eq!(c.stats().deopts, 1);
        assert!(!c.watches_back_edges(7), "OSR is closed after a deopt from a loop header");
        assert_eq!(c.run_osr(7, 0, |i| Some(if i == 0 { 10 } else { 2 })), None, "and stays closed");
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
}
