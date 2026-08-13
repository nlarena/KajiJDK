//! The bytecode interpreter: a dispatch loop that *executes* a method's `code[]`,
//! one opcode at a time. Named after HotSpot's `bytecodeInterpreter.cpp`, whose
//! core is the same idea — a giant `switch (opcode)` over the raw bytes.
//!
//! Note the split from [`crate::jvm::opcode`]: that module decodes bytes into
//! `Instruction`s for *javap* (it renders text). Here we never go through the
//! mnemonic string — we `match` on the raw opcode byte and run it.
//!
//! The machine is **single-steppable**: [`JVM::step`] runs exactly one
//! opcode and reports whether to continue. [`execute`] drives `step` in a loop;
//! the `jvm-step` visualizer drives it one keypress at a time.
//!
//! Method calls use a **stack of frames** (the call stack). Frames don't own
//! their bytecode — they hold a `MethodId` into the **metaspace**, which the
//! interpreter owns and resolves to `code[]` on demand. `invokestatic` resolves
//! (loading the callee's class if needed) and pushes a frame; `ireturn` pops one,
//! handing the result down to the caller.

use std::fmt::Write as _;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, Mutex, RwLock};
use std::thread;
use std::time::Duration;

use super::frame::{Frame, Value};
use super::gc;
use super::heap::HeapService;
use super::metaspace::{InitState, MetaspaceService, MethodId};
use super::strings;

/// Opcode implementations grouped by family, dispatched from [`JVM::step`].
pub mod arithmetic_operations;
pub mod array_operations;
pub mod bifurcation_operations;
pub mod class_operations;
pub mod comparison_operations;
pub mod conversion_operations;
pub mod stack_operations;
pub mod objects_operations;
pub mod variable_operations;

/// Spins the implementing class a `LambdaMetafactory` call site produces (via the `.class` writer).
pub mod lambda_factory;

/// The four invoke opcodes — one module each. Unlike the per-family helpers above
/// (which act on a single `&mut Frame`), these drive the whole call stack, so each
/// contributes an `impl JVM` method that `step()` dispatches to.
mod invokeinterface;
mod invokespecial;
mod invokestatic;
mod invokevirtual;

/// `invokedynamic` — the odd one out: it takes free functions rather than an
/// `impl JVM` method, because it never pushes a frame (the bootstrap methods are
/// intrinsics, so the call site's value is produced in Rust).
pub mod invokedynamic;

/// `athrow` + stack unwinding — also an `impl JVM` method (it walks the
/// frame stack), in its own module.
mod athrow;

/// What [`JVM::step`] reports after running one opcode.
pub enum Step {
    /// Keep going — fetch the next opcode from the current frame.
    Continue,
    /// The entry method returned: `Some(value)` for an `ireturn`, `None` for a
    /// void `return`. The program is done.
    Return(Option<Value>),
}

/// A thread's scheduling state. The scheduler only runs `Runnable` threads; every other
/// state is a kind of parked. The distinctions mirror `java.lang.Thread.State` — they say
/// *why* a thread is parked, not *how* — because that is what `Thread.getState()` reports
/// and what a stop-the-world handshake (removing the GIL) will need to ask.
///
/// The state names the **reason**, not the mechanism: `sleep`, `join` and a contended
/// `monitorenter` all park the same way, but they are `TimedWaiting`, `Waiting` and
/// `Blocked` respectively. That information used to live *beside* the status (in
/// `sleep_until`/`joining_on`); folding it into the state is the point of this enum.
///
/// `NEW` (created, not started) has no representation here on purpose: a [`GreenThread`]
/// only exists once it has started, so `getState()` derives `NEW` from the *absence* of a
/// scheduler slot rather than from a state the scheduler could never be in.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum ThreadStatus {
    /// Running, or ready to run — the only state the scheduler will dispatch.
    Runnable,
    /// Waiting to acquire a **contended monitor** (`synchronized`). Java's `BLOCKED`.
    Blocked,
    /// Parked in `wait()` or `join()` with **no deadline**, until notified/woken.
    Waiting,
    /// Parked with a **deadline**: `sleep(ms)`, or `wait(ms)` — returns even without a
    /// notify once the time passes. Java's `TIMED_WAITING`.
    TimedWaiting,
    /// The thread finished (its `run()` returned). Terminal.
    Terminated,
}

/// The execution **substrate** for Java threads — an application parameter
/// (`JVM_THREADS`), read once at VM startup like the `JVM_GC_*` knobs.
///
/// - `Green` (default): the cooperative scheduler on a single OS thread (`step`
///   round-robins at opcode granularity). Deterministic and single-steppable — what
///   the `jvm-step` visualizer needs.
/// - `OsGil`: each `java.lang.Thread` runs on a real `std::thread`, with a **GIL** (one
///   global interpreter lock) serializing opcode execution. Correct but not parallel.
///   Blocking is real `park`/`unpark`.
/// - `OsParallel`: real OS threads **without** the GIL → true parallelism (H3, *in
///   progress*). Selected by `JVM_THREADS=os`. Today it still shares the `os-gil` engine
///   (one global lock per opcode), so it runs and is oracle-correct but is **not yet
///   actually parallel**; the plan *shrinks* that lock — safepoints, TLABs, fine-grained
///   locks — rather than removing it in one shot, so the mode stays validated at every step.
///
/// `OsGil` and `OsParallel` are the close pair: both run on real `std::thread`s and share
/// the whole OS substrate, differing only in whether the world-lock is engaged. `Green` is
/// the outlier — a different scheduler entirely.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ThreadMode {
    Green,
    OsGil,
    OsParallel,
}

impl ThreadMode {
    /// Reads `JVM_THREADS`: `os-gil` → OS threads + GIL; `os` → OS threads without the GIL
    /// (the in-progress parallel engine); anything else → green (the default while the OS
    /// substrate stabilises).
    pub fn from_env() -> Self {
        match std::env::var("JVM_THREADS").ok().as_deref().map(str::trim) {
            Some(v) if v.eq_ignore_ascii_case("os-gil") => ThreadMode::OsGil,
            Some(v) if v.eq_ignore_ascii_case("os") => ThreadMode::OsParallel,
            _ => ThreadMode::Green,
        }
    }

    /// True for the substrates that run on real `std::thread`s — so blocking uses real
    /// `park`/`unpark` — as opposed to the single-threaded cooperative `Green`. Both
    /// `OsGil` and `OsParallel` qualify; they differ only in the world-lock.
    pub fn uses_os_threads(self) -> bool {
        matches!(self, ThreadMode::OsGil | ThreadMode::OsParallel)
    }
}

/// An object's **monitor** — its lock + wait-set for `synchronized`/`wait`/`notify`.
/// Cooperative: no OS mutex, just bookkeeping. `owner` holds it (reentrant via `count`);
/// `blocked` are contenders waiting to *acquire*; `waiting` are threads parked in
/// `wait()` until a `notify` moves them to `blocked` to re-acquire.
#[derive(Default)]
pub struct Monitor {
    owner: Option<usize>,
    count: usize,
    blocked: Vec<usize>,
    waiting: Vec<usize>,
}

/// One **green thread**: a Java thread modelled as its own call stack, scheduled
/// cooperatively by the VM onto the single OS thread (the way early JVMs — and Java 21's
/// virtual threads — multiplex threads in user space). The *currently running* thread's
/// stack lives in [`JVM::frames`]; a parked thread keeps its stack here and its
/// slot's `frames` is empty (they swap on a context switch).
pub struct GreenThread {
    pub id: usize,
    pub status: ThreadStatus,
    pub frames: Vec<Frame>,
    /// Heap offset of the `java.lang.Thread` object this green thread runs (so `join`
    /// can map a `Thread` reference back to its thread). `0` for the entry/`main` thread.
    pub thread_obj: usize,
    /// Set while a thread is returning from `wait()`: the monitor it must **re-acquire**
    /// (and the recursion count to restore) before resuming past the `wait()` call.
    pub wait_reacquire: Option<(usize, usize)>,
    /// Set while blocked in `join`: the index of the thread whose termination we await.
    pub joining_on: Option<usize>,
    /// Set while in `sleep`: the logical step (opcode clock) at which to wake.
    pub sleep_until: Option<usize>,
    /// Set when the thread was **interrupted out of** an interruptible block
    /// (`sleep`/`join`/`wait`): on resume it throws `InterruptedException` instead of
    /// continuing. See the resume check at the top of [`JVM::run_one`].
    pub interrupt_pending: bool,
    /// The pc of the blocking call the thread is parked at. An interrupt throws
    /// `InterruptedException` *from this pc*, not from the instruction after the call —
    /// the exception table covers the call, so throwing past it would miss the handler.
    pub block_call_pc: usize,
    /// **OS mode only.** Handle to this thread's `std::thread`, so other threads can
    /// `unpark` it when they make it runnable (monitor release, notify, join-wake,
    /// sleeper-wake). `None` in green mode and for the main thread (driven directly).
    pub os_handle: Option<thread::Thread>,
    /// **OS mode only.** Whether a real OS thread has already been launched for this
    /// slot — the driver spawns one per new `Thread.start()` slot exactly once.
    pub os_spawned: bool,
    /// **`os` (parallel) mode only.** Set by a `Runnable` thread that has parked itself at a
    /// GC safepoint (its frames synced to this slot). The GC coordinator waits until every
    /// non-terminated thread is *safe* (`status != Runnable || at_safepoint`) before moving
    /// objects, then unparks the safepoint-parked ones. See `os_parallel_loop`.
    pub at_safepoint: bool,
    /// `LockSupport.unpark` **permit** (binary): set when unparked while *not* parked, so the next
    /// `park()` returns immediately instead of blocking — the token semantics that makes an
    /// unpark-before-park not get lost. `park()` consumes it.
    pub park_permit: bool,
    /// Whether this thread is currently blocked in `LockSupport.park()` (as opposed to a monitor
    /// `wait` or `sleep`). `unpark` uses it to decide between waking the thread and just leaving a
    /// permit.
    pub parked: bool,
}


/// A read-only snapshot of one thread for the visualizer: its id, scheduling state,
/// the method it's currently in, and whether it's the running thread.
pub struct ThreadView {
    pub id: usize,
    pub status: &'static str,
    pub method: String,
    pub current: bool,
}

/// A program execution in progress: the **metaspace** (the loaded classes and
/// their bytecode) plus the **call stack** — a stack of frames, one per in-flight
/// method call. The frame on top is the one currently executing.
/// The **per-thread execution context**: the running thread's call stack plus its slot
/// index. In green mode it lives inside the owner [`JVM`]; in OS mode **each thread owns its
/// own** `RunningCtx` (H3 1c-ii) and pairs it with a `SharedVm` lock guard per opcode via
/// [`Exec`]. Every opcode handler reaches it as `self.running.frames`. Frames live in the
/// thread's slot between turns and are `activate`d into here to run; 1d will let the
/// frame-local opcodes run on this context **without** taking the shared lock at all.
#[derive(Default)]
struct RunningCtx {
    /// Index into `threads` of the thread currently running. Its slot's own `frames` is
    /// empty while it runs — the live stack is here in `frames`.
    current: usize,
    /// The running thread's call stack (bottom → top). Swapped with the slot on a context switch.
    frames: Vec<Frame>,
    /// **Code cache** (H3 1d): a copy of the current top frame's bytecode, refreshed lazily
    /// by [`Exec::sync_code_cache`] whenever the top method changes (invoke/return/activate/
    /// unwind — all detected by `code_method` mismatch). Lets a frame-local opcode read its
    /// bytecode from thread-local memory instead of the shared `metaspace`.
    code: Vec<u8>,
    /// The `MethodId` whose bytecode currently lives in `code` (`None` before the first fill).
    code_method: Option<MethodId>,
    /// Floors for exception unwinding: while a `call_java`-driven nested execution runs (a
    /// `<clinit>`, an intrinsic callback), its synthetic base frame index sits here. An exception
    /// that reaches a floor with no handler stops there — surfacing to the VM via
    /// `pending_exception` — instead of tearing through the code that made the call. A stack,
    /// because such calls nest.
    exception_floor: Vec<usize>,
    /// An exception that unwound out of a `call_java` boundary (heap offset of the throwable),
    /// waiting for the caller to re-deliver it — e.g. a `<clinit>` failure re-thrown by the
    /// opcode that triggered initialization.
    pending_exception: Option<usize>,
}

/// The **shared VM state** — everything that is *not* private to a single thread. In OS mode
/// it lives behind one `Arc<Mutex<SharedVm>>` (H3 1c-ii), locked per opcode; the H3 endgame
/// (see `docs/H3_ownership.md`) puts each field behind its own lock in the order `metaspace <
/// heap < monitors < registry < console < gc`, so threads serialise only on the structure they
/// actually touch. Grouping the fields into one type now — behaviour identical while the single
/// `Mutex` still stands in — is the boundary the split needs.
struct SharedVm {
    metaspace: MetaspaceService,
    /// All green threads (the **registry**). `threads[running.current]` is the running one
    /// (its `frames` empty — the live stack is in `running.frames`); the rest keep their stacks.
    threads: Vec<GreenThread>,
    /// Monotonic id for the next spawned thread (the scheduler's internal id).
    next_thread_id: usize,
    /// Counter behind `Thread.nextThreadNum()` — the id a `Thread` object is stamped with
    /// at construction. Separate from `next_thread_id` because it advances at *construction*
    /// (every `new Thread()`), not at `start`. `main`'s lazily-built object takes id 0.
    java_thread_counter: i64,
    /// Object monitors for `synchronized`, keyed by the lock object's heap offset.
    /// Created lazily on first `monitorenter`.
    monitors: std::collections::HashMap<usize, Monitor>,
    /// Resolved **dynamic constants** (condy), keyed by the class and pool index that
    /// named them. Caching is not an optimisation here: a condy is a *constant*, so it
    /// must be computed once — and the same one is routinely shared, as when every case
    /// label of an enum `switch` points at the one `ClassDesc` describing the enum.
    condy: std::collections::HashMap<(String, u16), Value>,
    /// Dynamic constants currently being computed. A condy's arguments can be other
    /// condys, so the resolution is a graph walk — and a constant that (directly or not)
    /// refers to itself would otherwise recurse until the Rust stack gives out. This
    /// turns that into a diagnosable error, which is what the JVMS calls for.
    condy_in_progress: std::collections::HashSet<(String, u16)>,
    heap: HeapService,
    /// Everything the program has printed via native methods (e.g.
    /// `PrintStream.println`). Buffered here so tooling can show it persistently —
    /// the step visualizer clears the screen each frame, which would wipe raw stdout.
    console: String,
    /// The GC's policy (fragmentation knobs + automatic-trigger settings), read from
    /// the environment once at startup. Tunable per run via the `JVM_GC_*` variables.
    gc_policy: gc::GcPolicy,
    /// Logical clock: opcodes executed so far. The allocation-rate trigger measures
    /// "time" in opcodes (we have no wall clock), and a safepoint is polled per step.
    steps: usize,
    /// The clock / used-bytes snapshot taken at the **last** collection — the
    /// baseline the automatic triggers compare against (so a GC that frees nothing
    /// doesn't re-fire every opcode).
    last_gc_step: usize,
    last_gc_used: usize,
    /// Set by `System.gc()`; honoured at the next safepoint (the explicit trigger).
    gc_requested: bool,
    /// Threading substrate (green vs `os-gil`), read once from `JVM_THREADS` at startup.
    mode: ThreadMode,
    /// **OS mode only.** Raised when the main thread returns: worker OS threads see it
    /// at the top of their loop and exit (mirrors the green scheduler abandoning workers
    /// when `main` ends).
    halt: bool,
    /// Set by `System.exit(status)` — the VM's **exit status**, and the fact that the program
    /// was terminated deliberately rather than by `main` returning. Every driver reads it: the
    /// green scheduler stops even when a *worker* called `exit`, and an OS-mode main thread woken
    /// by the `halt` above returns this status as the program's result. `None` = no `exit` call.
    exit_status: Option<i32>,
    /// **`os` parallel driver only.** When set, [`Exec::safepoint`] *defers* collection to the
    /// driver's stop-the-world handshake instead of collecting inline. The serialised paths
    /// (green, `os-gil`, and the `os` serialised fallback) leave it `false` and collect inline.
    gc_by_driver: bool,
}

// H3 *ownership* border (see `docs/H3_ownership.md` §1). The two fields below are the split:
// `shared` (guarded as a whole by the GIL today; per-field locks at 1c/1d) and `running` (the
// per-thread execution context, owned by each OS thread once the GIL is gone). Every opcode
// handler still reaches them as `self.shared.<x>` / `self.running.<x>`.
pub struct JVM {
    /// Shared VM state (heap, metaspace, monitors, registry, GC bookkeeping…). See [`SharedVm`].
    shared: SharedVm,
    /// The **per-thread execution context** (running thread's stack + index). See [`RunningCtx`].
    running: RunningCtx,
}

/// The interpreter **view**: a borrow of the shared state plus the running thread's context.
/// Every opcode handler and scheduler method lives here (`impl Exec`), reaching state as
/// `self.shared.<x>` / `self.running.<x>` exactly as before. Splitting the *receiver* from the
/// owner ([`JVM`]) is the H3 1c step (see `docs/H3_ownership.md`): today both borrows come from
/// one owned `JVM`, but the shape lets an OS thread later combine its **own** `RunningCtx` with a
/// **shared** `SharedVm` guard — the actual GIL removal — without touching a single handler body.
pub struct Exec<'a> {
    shared: &'a mut SharedVm,
    running: &'a mut RunningCtx,
}

impl JVM {
    /// Starts an interpreter whose call stack holds just the `entry` frame, run
    /// against `metaspace` (which it takes ownership of, to resolve calls), with a
    /// fresh empty heap.
    pub fn new(metaspace: MetaspaceService, entry: Frame) -> Self {
        JVM {
            // The entry method runs on thread 0 (`main`); its stack is the active one,
            // so thread 0's own `frames` slot starts empty.
            running: RunningCtx { current: 0, frames: vec![entry], ..Default::default() },
            shared: SharedVm {
                metaspace,
                threads: vec![GreenThread {
                    id: 0,
                    status: ThreadStatus::Runnable,
                    frames: Vec::new(),
                    thread_obj: 0, // the entry/main thread has no `Thread` object
                    wait_reacquire: None,
                    joining_on: None,
                    sleep_until: None,
                    interrupt_pending: false,
                    block_call_pc: 0,
                    os_handle: None,
                    os_spawned: true, // the main thread is driven by execute_os_gil's own thread
                    at_safepoint: false,
                    park_permit: false,
                    parked: false,
                }],
                next_thread_id: 1,
                java_thread_counter: 1, // main's lazily-built object takes 0; first `new Thread()` takes 1
                monitors: std::collections::HashMap::new(),
                condy: std::collections::HashMap::new(),
                condy_in_progress: std::collections::HashSet::new(),
                heap: HeapService::new(),
                console: String::new(),
                gc_policy: gc::GcPolicy::from_env(),
                steps: 0,
                last_gc_step: 0,
                last_gc_used: 0,
                gc_requested: false,
                mode: ThreadMode::from_env(),
                halt: false,
                exit_status: None,
                gc_by_driver: false,
            },
        }
    }

    /// Borrows this owner as an interpreter [`Exec`] **view** — the receiver every opcode
    /// handler and scheduler method runs on. Both borrows come from one owner today; the OS
    /// parallel driver will instead pair a thread-local `RunningCtx` with a shared `SharedVm`.
    pub fn exec(&mut self) -> Exec<'_> {
        Exec { shared: &mut self.shared, running: &mut self.running }
    }
}

impl Exec<'_> {
    /// What the program has printed so far (via native methods), for tooling.
    pub fn console(&self) -> &str {
        &self.shared.console
    }

    /// The heap, for tooling that wants to show its contents.
    pub fn heap(&self) -> &HeapService {
        &self.shared.heap
    }

    /// Runs `f` with **every** thread stack visible to it. The GC's roots span all
    /// threads, but the running thread's stack lives in `self.running.frames`; this parks it
    /// into its slot so the whole set is in `self.shared.threads`, runs `f` over them, then
    /// re-activates. Every GC entry point goes through here.
    fn parked<R>(&mut self, f: impl FnOnce(&MetaspaceService, &mut HeapService, &mut [GreenThread]) -> R) -> R {
        // Every GC enters here: flush the per-thread pending Eden logs (W2b) into the shared
        // `objects` log first, so the collector's wholesale view is complete.
        self.shared.heap.commit_pending();
        std::mem::swap(&mut self.running.frames, &mut self.shared.threads[self.running.current].frames);
        let result = f(&self.shared.metaspace, &mut self.shared.heap, &mut self.shared.threads);
        std::mem::swap(&mut self.running.frames, &mut self.shared.threads[self.running.current].frames);
        result
    }

    /// Runs a GC **mark** phase over the live state (frames + mirrors) and returns
    /// what came out live vs garbage. Mark-only for now — nothing is freed; the
    /// visualizer triggers this (on `espacio`) to *show* reachability.
    pub fn gc_mark(&mut self) -> gc::MarkReport {
        let (_keys, refs) = self.condy_roots();
        self.parked(|m, h, t| gc::mark(m, h, t, &refs))
    }

    /// Runs a full GC cycle — mark **and sweep**: reclaims every unreachable object
    /// into the heap's free list. Returns the report (its `garbage` = what was freed).
    pub fn gc_sweep(&mut self) -> gc::MarkReport {
        let (_keys, refs) = self.condy_roots();
        self.parked(|m, h, t| gc::sweep(m, h, t, &refs))
    }

    /// The GC compaction policy in effect (read from the environment at startup),
    /// for tooling that evaluates the fragmentation rule.
    pub fn gc_policy(&self) -> &gc::GcPolicy {
        &self.shared.gc_policy
    }

    /// Runs a **mark-compact**: relocates live objects into one contiguous run and
    /// fixes the references to them. Returns what moved / how much was reclaimed.
    pub fn gc_compact(&mut self) -> gc::CompactReport {
        let (keys, mut refs) = self.condy_roots();
        let report = self.parked(|m, h, t| gc::compact(m, h, t, &mut refs));
        self.restore_condy_roots(&keys, &refs);
        self.remap_monitor_keys(&report.relocations);
        self.prune_dead_monitors();
        report
    }

    /// Relocate the object-monitor map through a GC `forward` map (old offset → new).
    /// Monitors are keyed by the lock object's heap offset, which a *moving* collection
    /// (minor evacuation / compaction) changes — without this a `synchronized`/`wait` on a
    /// relocated object would lose its monitor. (Frame monitors and `wait_reacquire` are
    /// remapped inside the collector itself; this fixes the map the collector can't see.)
    fn remap_monitor_keys(&mut self, forward: &std::collections::HashMap<usize, usize>) {
        if forward.is_empty() {
            return;
        }
        let monitors = std::mem::take(&mut self.shared.monitors);
        self.shared.monitors = monitors
            .into_iter()
            .map(|(obj, mon)| (forward.get(&obj).copied().unwrap_or(obj), mon))
            .collect();
    }

    /// The condy cache's live references, as parallel `(key, offset)` lists. A resolved dynamic
    /// constant lives for the program but is reachable only from this VM table — not any frame or
    /// mirror — so a moving GC must be told to keep it alive and to remap it. Non-reference
    /// constants (e.g. an `int` condy) are neither a root nor movable, so they're skipped.
    fn condy_roots(&self) -> (Vec<(String, u16)>, Vec<usize>) {
        let mut keys = Vec::new();
        let mut refs = Vec::new();
        for (key, value) in &self.shared.condy {
            if let Value::Reference(offset) = value {
                if *offset != 0 {
                    keys.push(key.clone());
                    refs.push(*offset);
                }
            }
        }
        (keys, refs)
    }

    /// Writes the GC-remapped condy references back into the cache (the collector rewrote the
    /// `refs` list in place through its forwarding map).
    fn restore_condy_roots(&mut self, keys: &[(String, u16)], refs: &[usize]) {
        for (key, &offset) in keys.iter().zip(refs) {
            self.shared.condy.insert(key.clone(), Value::Reference(offset));
        }
    }

    /// Drop monitors whose lock object is no longer allocated (it was collected), so a
    /// later allocation reusing that offset can't inherit a stale monitor.
    fn prune_dead_monitors(&mut self) {
        let live: std::collections::HashSet<usize> =
            self.shared.heap.allocations().iter().map(|a| a.offset).collect();
        self.shared.monitors.retain(|obj, _| live.contains(obj));
    }

    /// Runs a **minor** collection: the young generation's copying collector — evacuate
    /// Eden's survivors to a survivor space (or promote them), recycle Eden. Cheap and
    /// frequent; the visualizer can trigger it, and the safepoint runs it when Eden fills.
    pub fn gc_minor(&mut self) -> gc::MinorReport {
        let tenure = self.shared.gc_policy.tenure;
        let (keys, mut refs) = self.condy_roots();
        let report = self.parked(|m, h, t| gc::minor(m, h, t, tenure, &mut refs));
        self.restore_condy_roots(&keys, &refs);
        self.remap_monitor_keys(&report.relocations);
        self.prune_dead_monitors();
        report
    }

    /// Flags an explicit collection request (`System.gc()`), serviced at the next
    /// safepoint — not run inline, exactly like the real VM defers it.
    pub fn request_gc(&mut self) {
        self.shared.gc_requested = true;
    }

    /// A **safepoint**: the point between opcodes where the VM is allowed to collect.
    /// Polls the triggers — an explicit `System.gc()` first, then the automatic
    /// causes (out-of-space / occupancy / allocation-rate) — and runs a cycle if one
    /// fires. This is the single place "when does the GC run" is decided.
    fn safepoint(&mut self) {
        // `os` (parallel) mode: the collector *moves* objects, so it can't run while sibling
        // threads execute lock-free. The parallel driver owns GC — it runs a stop-the-world
        // handshake (`coordinate_gc`) instead — so here we defer. `os-gil`/green collect inline
        // (safe: the single lock / one OS thread already serialises everything).
        if self.shared.gc_by_driver {
            return;
        }
        self.collect_at_safepoint();
    }

    /// Whether a collection is due — the same triggers `collect_at_safepoint` acts on. The `os`
    /// parallel driver polls this after each shared opcode to decide whether to stop the world.
    fn needs_collection(&self) -> bool {
        self.shared.heap.eden_used() * 10 >= self.shared.heap.eden_capacity() * 9
            || self.shared.gc_requested
            || self
                .shared
                .gc_policy
                .auto_cause(
                    self.shared.heap.used(),
                    self.shared.steps,
                    self.shared.last_gc_used,
                    self.shared.last_gc_step,
                )
                .is_some()
    }

    /// The actual collection triggers, factored out of [`Self::safepoint`] so the `os` parallel
    /// driver can run the *same* logic under its stop-the-world handshake.
    fn collect_at_safepoint(&mut self) {
        // Young generation first: a (near-)full Eden triggers a cheap minor collection.
        // Always on — the copying collector is correct over any program state (the
        // gate the old `JVM_GC_AUTO` guarded was about an incomplete mark, long fixed).
        if self.shared.heap.eden_used() * 10 >= self.shared.heap.eden_capacity() * 9 {
            let tenure = self.shared.gc_policy.tenure;
            let (keys, mut refs) = self.condy_roots();
            let report = self.parked(|m, h, t| gc::minor(m, h, t, tenure, &mut refs));
            self.restore_condy_roots(&keys, &refs);
            self.remap_monitor_keys(&report.relocations); // young objects moved → fix monitor keys
            self.prune_dead_monitors();
            let _ = writeln!(
                self.shared.console,
                "[gc] minor: {} copiados, {} promovidos · recuperó {}B",
                report.copied, report.promoted, report.reclaimed
            );
        }

        // Then the major (Old) triggers: explicit `System.gc()` or the automatic causes.
        let cause = if self.shared.gc_requested {
            Some(gc::GcCause::Explicit)
        } else {
            self.shared.gc_policy.auto_cause(
                self.shared.heap.used(),
                self.shared.steps,
                self.shared.last_gc_used,
                self.shared.last_gc_step,
            )
        };
        if let Some(cause) = cause {
            self.collect(cause);
        }
        // Opt-in (`JVM_GC_VERIFY`) post-GC consistency check: assert no reference dangles. Runs
        // inside `parked` so the running thread's stack is in its slot and the scan spans every
        // frame. Off by default (a full-heap walk); a stress/CI safety net for moving-GC remap bugs.
        if self.shared.gc_policy.verify {
            self.parked(|m, h, t| gc::verify_heap(m, h, t));
        }
    }

    /// Runs one collection cycle: mark-and-sweep, then compact if the heap is
    /// fragmented enough ([`gc::should_compact`]). Resets the trigger baselines and
    /// logs a line (visible in the visualizer's output panel).
    fn collect(&mut self, cause: gc::GcCause) {
        let before = self.shared.heap.used();
        let policy = self.shared.gc_policy;
        // A full collection is generational: a minor first (evacuate/promote the young),
        // then the major over Old (sweep, and compact if fragmented). All over every
        // thread's roots, so it runs inside `parked`.
        let (keys, mut refs) = self.condy_roots();
        let (live, garbage, compacted, minor_reloc, compact_reloc) = self.parked(|m, h, t| {
            // `refs` (the condy roots) is threaded through and rewritten in place by each moving
            // phase, so it stays current: minor evacuates+remaps, sweep keeps it alive, compact
            // remaps again. Written back to the cache below.
            let minor = gc::minor(m, h, t, policy.tenure, &mut refs);
            // Soft references are given back only under **memory pressure**: a collection the
            // heap asked for (occupancy / out-of-space / allocation rate) clears them, an
            // explicit `System.gc()` does not — see `gc::SoftPolicy`.
            let report = if cause == gc::GcCause::Explicit {
                gc::sweep(m, h, t, &refs)
            } else {
                gc::sweep_under_pressure(m, h, t, &refs)
            };
            let (compacted, compact_reloc) = if gc::should_compact(h, &policy) {
                let c = gc::compact(m, h, t, &mut refs);
                (c.reclaimed, c.relocations)
            } else {
                (0, std::collections::HashMap::new())
            };
            (report.live.len(), report.garbage.len(), compacted, minor.relocations, compact_reloc)
        });
        self.restore_condy_roots(&keys, &refs);
        // Objects moved (minor evacuation, then compaction) → relocate the monitor map keys,
        // applied minor-then-compact (the composition), then drop monitors on dead objects.
        self.remap_monitor_keys(&minor_reloc);
        self.remap_monitor_keys(&compact_reloc);
        self.prune_dead_monitors();
        let after = self.shared.heap.used();

        self.shared.gc_requested = false;
        self.shared.last_gc_used = after;
        self.shared.last_gc_step = self.shared.steps;

        let _ = writeln!(
            self.shared.console,
            "[gc] {cause:?}: {live} vivos, {garbage} basura · used {before}B → {after}B{}",
            if compacted > 0 { format!(" (compactó {compacted}B)") } else { String::new() },
        );
    }

    /// The mirror index as `(Class ID, class name, offset)` rows, for a visualizer
    /// labelling the heap with which class's mirror sits at which offset.
    pub fn class_objects(&self) -> Vec<(&str, &str, usize)> {
        self.shared.metaspace.class_object_offsets()
    }

    /// The current (top) frame — read by the visualizer to show the live state.
    pub fn frame(&self) -> &Frame {
        self.running.frames.last().expect("no frame on the call stack")
    }

    /// The current frame's program counter.
    pub fn pc(&self) -> usize {
        self.frame().pc()
    }

    /// How deep the call stack is (1 = just the entry method).
    pub fn depth(&self) -> usize {
        self.running.frames.len()
    }

    /// The bytecode of the current (top) frame — served from the [`RunningCtx`] code cache,
    /// kept current by [`Self::sync_code_cache`] (called at the top of each `run_one`, and on
    /// every frame change via the `code_method` check).
    pub fn current_code(&self) -> &[u8] {
        &self.running.code
    }

    /// Refreshes the code cache if the top frame's method changed since the last fill. O(1) in
    /// the common case (same method — just a `MethodId` compare); copies the bytecode only on an
    /// actual frame change (invoke/return/activate/unwind). This is what lets the H3 1d
    /// frame-local fast path read opcodes from thread-local memory, never touching `metaspace`.
    fn sync_code_cache(&mut self) {
        let method = self.frame().method();
        if self.running.code_method != Some(method) {
            self.running.code = self.shared.metaspace.code(method).to_vec();
            self.running.code_method = Some(method);
        }
    }

    /// The whole call stack (bottom → top), for a visualizer that shows several
    /// frames at once.
    pub fn frames(&self) -> &[Frame] {
        &self.running.frames
    }

    /// A snapshot of every green thread (id, state, current method, which is running) —
    /// so the visualizer can show the cooperative scheduling. The running thread's live
    /// stack is in `self.running.frames`; parked threads keep theirs in their slot.
    pub fn thread_views(&self) -> Vec<ThreadView> {
        self.shared.threads
            .iter()
            .enumerate()
            .map(|(i, t)| {
                let stack = if i == self.running.current { &self.running.frames } else { &t.frames };
                let method = stack
                    .last()
                    .map(|f| self.shared.metaspace.name(f.method()).to_string())
                    .unwrap_or_else(|| "—".to_string());
                ThreadView {
                    id: t.id,
                    status: match t.status {
                        ThreadStatus::Runnable => "runnable",
                        ThreadStatus::Blocked => "blocked",
                        ThreadStatus::Waiting => "waiting",
                        ThreadStatus::TimedWaiting => "timed_waiting",
                        ThreadStatus::Terminated => "terminated",
                    },
                    method,
                    current: i == self.running.current,
                }
            })
            .collect()
    }

    /// The bytecode of an arbitrary frame (not just the top), via the metaspace.
    pub fn code_of(&self, frame: &Frame) -> &[u8] {
        self.shared.metaspace.code(frame.method())
    }

    /// A frame's method name, for labelling its panel.
    pub fn method_name_of(&self, frame: &Frame) -> &str {
        self.shared.metaspace.name(frame.method())
    }

    /// Mutable access to the top frame, for the opcode helpers.
    fn top(&mut self) -> &mut Frame {
        self.running.frames.last_mut().expect("no frame on the call stack")
    }

    /// Reads the signed 2-byte branch offset that follows the current opcode.
    fn branch_offset(&self) -> i16 {
        let frame = self.frame();
        let code = self.shared.metaspace.code(frame.method());
        let pc = frame.pc();
        i16::from_be_bytes([code[pc + 1], code[pc + 2]])
    }

    /// Reads the signed **4-byte** branch offset that follows a wide branch
    /// (`goto_w`, 0xc8) — the same role as [`branch_offset`](Self::branch_offset),
    /// with the range the 16-bit form can't express.
    fn wide_branch_offset(&self) -> i32 {
        let frame = self.frame();
        let code = self.shared.metaspace.code(frame.method());
        let pc = frame.pc();
        i32::from_be_bytes([code[pc + 1], code[pc + 2], code[pc + 3], code[pc + 4]])
    }

    /// One scheduler tick: run a single opcode of the **current** thread, then hand
    /// the CPU to the next runnable thread (round-robin, cooperative). The program ends
    /// when the entry thread (`main`, id 0) returns. A worker thread that returns is
    /// marked `Terminated` and skipped thereafter. This is the green-thread scheduler;
    /// `run_one` does the actual opcode, and class init (`ensure_initialized`) drives
    /// `run_one` directly so a `<clinit>` runs to completion without yielding.
    pub fn step(&mut self) -> Step {
        if let Step::Return(value) = self.run_one() {
            // `System.exit` ends the **VM**, not just the calling thread: report its status as
            // the program's result no matter which thread ran it (a worker's ordinary return
            // would only terminate that thread — see below).
            if self.shared.exit_status.is_some() {
                return Step::Return(value);
            }
            // The current thread's last frame returned.
            if self.running.current == 0 {
                return Step::Return(value); // the main thread finished → program result
            }
            let finished = self.running.current;
            // Mark terminated and wake anyone blocked in `join` on it.
            self.on_thread_terminated(finished);
        }
        self.wake_sleepers();
        // Cooperative context switch: pick the next runnable thread.
        match self.next_runnable() {
            Some(next) if next != self.running.current => {
                self.switch_to(next);
                Step::Continue
            }
            Some(_) => Step::Continue, // only the current thread is runnable
            None => Step::Return(None), // nothing left to run
        }
    }

    /// Round-robin from `current`: the index of the next `Runnable` thread (or the
    /// current one if it's the only runnable; `None` if none are).
    fn next_runnable(&self) -> Option<usize> {
        let n = self.shared.threads.len();
        // Offsets 1..=n cover every thread, ending at `current` itself — so others are
        // preferred (fairness) and the current thread is the last resort.
        (1..=n)
            .map(|off| (self.running.current + off) % n)
            .find(|&i| self.shared.threads[i].status == ThreadStatus::Runnable)
    }

    /// Context switch: park the running thread's stack into its slot and load the
    /// target thread's stack into the active `frames` (a pair of swaps — the active
    /// stack always lives in `self.running.frames`).
    fn switch_to(&mut self, next: usize) {
        std::mem::swap(&mut self.running.frames, &mut self.shared.threads[self.running.current].frames);
        self.running.current = next;
        std::mem::swap(&mut self.running.frames, &mut self.shared.threads[self.running.current].frames);
    }

    /// Park thread `idx` — the single "block" primitive, the mirror of [`make_runnable`].
    /// `status` says *why* it parks (`Blocked`/`Waiting`/`TimedWaiting`), which is the
    /// distinction `getState()` reports and a stop-the-world handshake will read. Every
    /// place that transitions a thread *out of* `Runnable` (short of terminating) goes
    /// through here, so "who is parked, and why" has one authority instead of being
    /// scattered across the blocking opcodes.
    ///
    /// It only records the state: the actual `park` in OS mode happens in the driver loop
    /// when it sees the thread is no longer runnable, exactly as before.
    fn block(&mut self, idx: usize, status: ThreadStatus) {
        debug_assert!(
            matches!(status, ThreadStatus::Blocked | ThreadStatus::Waiting | ThreadStatus::TimedWaiting),
            "block() is for parked states; Runnable goes through make_runnable, Terminated through on_thread_terminated",
        );
        self.shared.threads[idx].status = status;
    }

    /// Mark thread `idx` runnable — the single "wake" primitive. In OS mode it also
    /// `unpark`s the thread's `std::thread` (it may be parked waiting for exactly this);
    /// in green mode the round-robin scheduler will simply pick it up. Every place that
    /// transitions a thread *to* `Runnable` goes through here so the unpark can't be missed.
    fn make_runnable(&mut self, idx: usize) {
        self.shared.threads[idx].status = ThreadStatus::Runnable;
        if self.shared.mode.uses_os_threads() {
            if let Some(handle) = &self.shared.threads[idx].os_handle {
                handle.unpark();
            }
        }
    }

    /// **OS mode.** Load thread `idx` as the running one: set `current` and swap its saved
    /// stack into the active `self.running.frames` (the inverse of [`Self::deactivate`]). Every
    /// opcode handler then touches `self.running.frames`/`self.running.current` exactly as in green mode.
    fn activate(&mut self, idx: usize) {
        self.running.current = idx;
        self.shared.heap.set_alloc_thread(idx); // this thread's Eden mallocs log to its own pending (W2b)
        std::mem::swap(&mut self.running.frames, &mut self.shared.threads[idx].frames);
    }

    /// **OS mode.** Park thread `idx`'s stack back into its slot after running an opcode,
    /// so the slot holds the full stack between turns (and the GC, via `parked`, can walk it).
    fn deactivate(&mut self, idx: usize) {
        std::mem::swap(&mut self.running.frames, &mut self.shared.threads[idx].frames);
    }

    /// Mark thread `idx` terminated and wake anyone blocked in `join` on it. Shared by the
    /// green scheduler ([`Self::step`]) and the OS driver loop.
    fn on_thread_terminated(&mut self, idx: usize) {
        self.shared.threads[idx].status = ThreadStatus::Terminated;
        let joiners: Vec<usize> = (0..self.shared.threads.len())
            .filter(|&i| self.shared.threads[i].joining_on == Some(idx))
            .collect();
        for w in joiners {
            self.shared.threads[w].joining_on = None;
            self.make_runnable(w);
        }
    }

    /// **OS mode.** Unpark every thread with a live OS handle — used on `halt` so parked
    /// workers wake, see the halt flag, and exit instead of leaking.
    fn unpark_all(&self) {
        for t in &self.shared.threads {
            if let Some(handle) = &t.os_handle {
                handle.unpark();
            }
        }
    }

    /// `System.exit(status)` (JLS §12.8): terminate the **whole VM** right now, from whichever
    /// thread called it. Not a return and not a throw — the JVM does *not* unwind: no `finally`
    /// runs, no `catch` sees anything, no caller resumes. So we simply **drop this thread's whole
    /// frame stack** and report `Step::Return(Some(status))`, which every driver already reads as
    /// "this stack is done"; `exit_status` (+ `halt` in OS mode) carries the same decision to the
    /// other threads, so the *program* result is `status` even when a worker called `exit`.
    ///
    /// Out of scope: **shutdown hooks**. They would have to run Java code on the termination path
    /// — the one path that must not execute bytecode — so `exit` here is unconditionally final.
    /// For the same reason, calling `exit` from inside a VM-driven callback (a `<clinit>`, a
    /// `MethodHandle` intrinsic) is not supported: those drive `run_one` against a frame floor
    /// this bypasses.
    pub(super) fn vm_exit(&mut self, status: i32) -> Step {
        self.shared.exit_status = Some(status);
        self.shared.halt = true; // OS mode: workers see it at the top of their loop and exit
        // No `pop_frame` loop: that would release monitors and run the unwind bookkeeping, which
        // is exactly the "orderly shutdown" `exit` is defined *not* to do.
        self.running.frames.clear();
        if self.shared.mode.uses_os_threads() {
            self.unpark_all(); // wake parked threads (including main) so they see `halt`
        }
        Step::Return(Some(Value::Int(status)))
    }

    /// Spawns a green thread for `Thread.start()`: it runs the receiver's `run()`
    /// (virtual dispatch on the receiver's class), parked `Runnable` until the scheduler
    /// picks it. `start()` itself returns immediately to the caller.
    /// Whether the `Thread` object `receiver` already has a scheduler slot — i.e. it was
    /// started at some point. The slot outlives the thread (a `Terminated` slot stays), so
    /// this is `true` for a running, blocked *or* finished thread — all the cases where a
    /// second `start()` is illegal. The absence of a slot is exactly the `NEW` state.
    fn already_started(&self, receiver: usize) -> bool {
        receiver != 0 && self.shared.threads.iter().any(|t| t.thread_obj == receiver)
    }

    /// `Thread.currentThread()`: the running thread's `Thread` object. A spawned thread
    /// already has one (its slot's `thread_obj`); the **main** thread doesn't, so its object
    /// is built lazily on first ask — name `"main"`, id `0`. Because `thread_obj` is a GC
    /// root, the object we store here survives collections (that is *why* it became a root).
    fn thread_current(&mut self) -> usize {
        let current = self.running.current;
        if self.shared.threads[current].thread_obj != 0 {
            return self.shared.threads[current].thread_obj;
        }
        // Main's object: allocate a bare `Thread` (its `<init>` is *not* run — main was
        // never `new`ed), then stamp the two fields `<init>` would have set.
        class_operations::load_class(&mut self.shared.metaspace, &mut self.shared.heap, "java/lang/Thread");
        let obj = objects_operations::allocate(&mut self.shared.metaspace, &mut self.shared.heap, "java/lang/Thread");
        let name = strings::intern(&mut self.shared.metaspace, &mut self.shared.heap, "main");
        let name_at = obj + objects_operations::field_offset(&mut self.shared.metaspace, "java/lang/Thread", "name");
        self.shared.heap.store_reference(obj, name_at, name);
        // `tid` (a long) is left 0 by `allocate`'s zeroing — main is thread 0.
        self.shared.threads[current].thread_obj = obj;
        obj
    }

    /// `Thread.nextThreadNum()`: hand out the next construction-time thread id.
    fn next_java_thread_num(&mut self) -> i64 {
        let id = self.shared.java_thread_counter;
        self.shared.java_thread_counter += 1;
        id
    }

    /// `Thread.interrupt0()`: the VM half of interruption — **wake** the target if it is
    /// parked in an *interruptible* block, so it can throw `InterruptedException`. The flag
    /// itself was already set on the object by the Java `interrupt()`; this only handles
    /// waking, and does nothing for a thread that isn't blocked (it will see the flag by
    /// polling) or has no slot (`NEW`, already flagged on the object).
    ///
    /// Only `Waiting`/`TimedWaiting` are interruptible: `sleep`, `join`, and `wait`. A
    /// `Blocked` thread (contending for a monitor) is **not** interruptible — Java's
    /// `synchronized` entry can't be interrupted (that's what `lockInterruptibly` is for).
    ///
    /// **The notify/interrupt race.** For a `wait`, `notify` and `interrupt` compete for
    /// the same thread. The GIL serialises them, and keying off the *status* resolves it
    /// with no lost notification:
    /// - notify **first** → the thread is already `Blocked` (moved to the blocked-set), so
    ///   this sees `Blocked` and does nothing: it returns from `wait` normally, having
    ///   consumed the notification, with its interrupt flag still set (a later poll sees it).
    /// - interrupt **first** → the thread leaves the wait-set here, so a subsequent
    ///   `notify` targets some *other* waiter. Nothing is lost.
    ///
    /// A thread can never both consume a notification *and* throw, so the notification is
    /// never dropped. (Removing the GIL would make this need explicit atomicity.)
    fn thread_interrupt(&mut self, target_obj: usize) {
        // Set the flag first, so it's observable in *every* state (a NEW thread with no slot,
        // a running thread that will poll, a blocked thread about to throw).
        self.set_interrupt_flag(target_obj);

        let idx = match self.shared.threads.iter().position(|t| t.thread_obj == target_obj && target_obj != 0) {
            Some(i) => i,
            None => return, // no slot (NEW / terminated already gone): flag on the object is enough
        };
        if !matches!(self.shared.threads[idx].status, ThreadStatus::Waiting | ThreadStatus::TimedWaiting) {
            return; // Runnable / Blocked / Terminated: not an interruptible park
        }
        // Pull it out of whatever it's parked in. For `wait`, keep `wait_reacquire` so the
        // resume re-acquires the monitor *before* throwing; just remove it from the wait-set.
        self.shared.threads[idx].sleep_until = None;
        self.shared.threads[idx].joining_on = None;
        if let Some((obj, _)) = self.shared.threads[idx].wait_reacquire {
            if let Some(mon) = self.shared.monitors.get_mut(&obj) {
                mon.waiting.retain(|&w| w != idx);
            }
        }
        self.shared.threads[idx].interrupt_pending = true;
        self.make_runnable(idx); // green: reschedulable; OS: also `unpark`s the std::thread
    }

    /// Writes the `interrupted` boolean field of a `Thread` object.
    fn write_interrupt_flag(&mut self, thread_obj: usize, value: bool) {
        if thread_obj != 0 {
            let at =
                thread_obj + objects_operations::field_offset(&mut self.shared.metaspace, "java/lang/Thread", "interrupted");
            self.shared.heap.write_u32(at, value as u32);
        }
    }

    /// Sets the interrupt flag on a `Thread` object (the object half of `interrupt()`).
    fn set_interrupt_flag(&mut self, thread_obj: usize) {
        self.write_interrupt_flag(thread_obj, true);
    }

    /// Clears the interrupt flag on thread `idx`'s object — done when the throw of
    /// `InterruptedException` consumes it (JLS).
    fn clear_interrupt_flag(&mut self, idx: usize) {
        let obj = self.shared.threads[idx].thread_obj;
        self.write_interrupt_flag(obj, false);
    }

    fn spawn_thread(&mut self, receiver: usize) {
        let runtime_class = self
            .shared.metaspace
            .class_name_at_mirror(self.shared.heap.read_u32(receiver) as usize)
            .expect("Thread.start: receiver has no class")
            .to_string();
        let slot = self
            .shared.metaspace
            .vtable_slot("java/lang/Thread", "run", "()V")
            .expect("Thread.run vtable slot");
        let run = self.shared.metaspace.vtable_method(&runtime_class, slot).expect("run() method");
        let max_locals = self.shared.metaspace.max_locals(run);
        let frame = Frame::for_call(run, max_locals, vec![Value::Reference(receiver)], &[1]);
        let id = self.shared.next_thread_id;
        self.shared.next_thread_id += 1;
        self.shared.threads.push(GreenThread {
            id,
            status: ThreadStatus::Runnable,
            frames: vec![frame],
            thread_obj: receiver,
            wait_reacquire: None,
            joining_on: None,
            sleep_until: None,
            interrupt_pending: false,
            block_call_pc: 0,
            os_handle: None,
            os_spawned: false, // the OS driver launches this slot's std::thread on the next tick
            at_safepoint: false,
            park_permit: false,
            parked: false,
        });
    }

    /// Core monitor **acquire**, shared by the `monitorenter` opcode and synchronized-method
    /// entry. Tries to make the current thread own `obj`'s monitor: succeeds (returning
    /// `true`) if the monitor is free or already this thread's (reentrant — just bumps the
    /// count); otherwise parks the thread in the monitor's blocked-set, marks it `Blocked`,
    /// and returns `false`. The *caller* decides what "blocked" means for its opcode (the
    /// pc rewind / operand restore that makes the operation retry when rescheduled).
    fn acquire_monitor(&mut self, obj: usize) -> bool {
        let current = self.running.current;
        let acquired = {
            let mon = self.shared.monitors.entry(obj).or_default();
            match mon.owner {
                None => {
                    mon.owner = Some(current);
                    mon.count = 1;
                    true
                }
                Some(o) if o == current => {
                    mon.count += 1;
                    true
                }
                Some(_) => {
                    if !mon.blocked.contains(&current) {
                        mon.blocked.push(current);
                    }
                    false
                }
            }
        };
        if !acquired {
            self.block(current, ThreadStatus::Blocked); // waiting for a contended monitor
        }
        acquired
    }

    /// Core monitor **release**, shared by the `monitorexit` opcode and synchronized-method
    /// exit. Drops one level of the current thread's ownership of `obj`'s monitor (reentrant
    /// — the monitor frees only at count 0); on freeing it, wakes one blocked contender so it
    /// can retry its acquire. A no-op if the current thread doesn't actually own it.
    fn release_monitor(&mut self, obj: usize) {
        let current = self.running.current;
        let wake = {
            let mon = self.shared.monitors.entry(obj).or_default();
            if mon.owner == Some(current) && mon.count > 0 {
                mon.count -= 1;
                if mon.count == 0 {
                    mon.owner = None;
                    (!mon.blocked.is_empty()).then(|| mon.blocked.remove(0))
                } else {
                    None
                }
            } else {
                None
            }
        };
        if let Some(idx) = wake {
            self.make_runnable(idx);
        }
    }

    /// Whether the **current** thread owns `obj`'s monitor (with a live count). This is the
    /// JVMS/JLS gate for `IllegalMonitorStateException`: a thread may only `monitorexit`, or
    /// `wait`/`notify`/`notifyAll`, on a monitor it actually holds.
    fn owns_monitor(&self, obj: usize) -> bool {
        self.shared.monitors
            .get(&obj)
            .map_or(false, |m| m.owner == Some(self.running.current) && m.count > 0)
    }

    /// `monitorenter` (0xc2): acquire the lock object's monitor, or **block** if another
    /// thread holds it. On block, the pc stays at the opcode and the objectref on the stack,
    /// so the thread retries when rescheduled — woken by the owner's `monitorexit`.
    fn monitor_enter(&mut self) -> Step {
        let obj = match self.frame().stack().last() {
            Some(Value::Reference(0)) => return self.throw_exception("java/lang/NullPointerException"),
            Some(Value::Reference(o)) => *o,
            _ => panic!("monitorenter: expected an object reference on the stack"),
        };
        if !self.acquire_monitor(obj) {
            return Step::Continue; // blocked: pc stays at the opcode, objectref on the stack
        }
        self.top().pop(); // consume the objectref
        self.top().advance(1);
        Step::Continue
    }

    /// `monitorexit` (0xc3): release the monitor (reentrant — frees only at count 0) and
    /// wake one blocked contender so it can retry `monitorenter`.
    fn monitor_exit(&mut self) -> Step {
        let obj = match self.top().pop() {
            Value::Reference(0) => return self.throw_exception("java/lang/NullPointerException"),
            Value::Reference(o) => o,
            _ => panic!("monitorexit: expected an object reference on the stack"),
        };
        // A thread can only release a monitor it owns (JVMS §6.5 monitorexit).
        if !self.owns_monitor(obj) {
            return self.throw_exception("java/lang/IllegalMonitorStateException");
        }
        self.release_monitor(obj);
        self.top().advance(1);
        Step::Continue
    }

    /// The most frames a thread's call stack may hold before the VM throws
    /// `StackOverflowError` (JVMS §6.3). Every `invoke*` funnels through
    /// [`Self::push_frame_locked`], so this one bound covers all Java-to-Java calls;
    /// [`Self::call_java`] applies it to VM-pushed synthetic frames too. 2000 is far more
    /// than any legitimate call chain in this project needs, yet small enough that an
    /// infinite recursion overflows (and the test runs) quickly. Throwing never pushes a
    /// frame (`throw_exception` allocates a bare object and unwinds), so the throw itself
    /// cannot re-overflow.
    const MAX_FRAMES: usize = 2000;

    /// Builds and pushes the callee's frame, **taking its monitor first** when `lock` is
    /// `Some` — a `synchronized` method, whose lock object is the receiver (instance) or the
    /// `Class` mirror (static). The frame remembers the monitor so `pop_frame` releases it on
    /// every exit. If the monitor is contended, *nothing* is pushed: the popped `operands`
    /// are restored to the caller's stack and the thread is parked, so the scheduler reruns
    /// this same invoke — and retries the acquire — once the thread is woken. `lock` is
    /// `None` for the common, unsynchronized case (a plain push, no monitor work).
    fn push_frame_locked(
        &mut self,
        callee: MethodId,
        max_locals: usize,
        operands: Vec<Value>,
        widths: &[usize],
        lock: Option<usize>,
    ) -> Step {
        // Depth limit first (JVMS §6.3) — before any monitor work, so an overflow can't
        // acquire a lock it would then leak. The popped operands are simply discarded:
        // the unwind clears the caller's stack anyway on its way to the handler.
        if self.running.frames.len() >= Self::MAX_FRAMES {
            return self.throw_exception("java/lang/StackOverflowError");
        }
        if let Some(obj) = lock {
            if !self.acquire_monitor(obj) {
                // Contended: undo the operand pop so the invoke replays cleanly on retry.
                // `operands` is in stack order (receiver first, then args), so re-pushing in
                // order restores the caller's stack exactly. pc is still at the invoke.
                for value in operands {
                    self.top().push(value);
                }
                return Step::Continue;
            }
        }
        let mut frame = Frame::for_call(callee, max_locals, operands, widths);
        if let Some(obj) = lock {
            frame.set_monitor(obj);
        }
        self.running.frames.push(frame);
        Step::Continue
    }

    /// Pops the current (top) frame, **releasing its monitor first** if it ran a
    /// `synchronized` method. Returns the popped frame (so callers can read e.g.
    /// `is_synthetic`).
    ///
    /// All frame removal — normal `return` *and* exception unwind — funnels through here so
    /// a synchronized method's lock can never leak: there is no `monitorexit` opcode to drop
    /// it, so the VM must, on whichever exit path the frame leaves by.
    ///
    /// Performance note: this puts a monitor check on the `return` path, which *every* call
    /// traverses though the overwhelming majority are not synchronized — we pay one branch
    /// (`frame.monitor().is_some()`) per return to keep the release in a single, unbypassable
    /// place. A production VM keeps the synchronized path off the hot return path (and uses
    /// biased/thin locks); here we trade a little speed for one obvious release site.
    fn pop_frame(&mut self) -> Option<Frame> {
        let popped = self.running.frames.pop();
        if let Some(obj) = popped.as_ref().and_then(Frame::monitor) {
            self.release_monitor(obj);
        }
        popped
    }

    /// `Object.wait()`: release the monitor **fully** (saving the recursion count), park
    /// the thread in the monitor's wait-set, and yield. The thread resumes past the
    /// `wait()` call only after a `notify` moves it to the blocked-set and it
    /// re-acquires the monitor (see the re-acquire check in `run_one`). Releasing the
    /// monitor wakes one blocked contender. (Assumes the caller holds the monitor.)
    fn monitor_wait(&mut self, obj: usize, timeout: Option<i64>) -> Step {
        // `wait()` requires holding the monitor (JLS 17.2) — else IllegalMonitorState.
        if !self.owns_monitor(obj) {
            return self.throw_exception("java/lang/IllegalMonitorStateException");
        }
        let current = self.running.current;
        let (saved, wake) = {
            let mon = self.shared.monitors.entry(obj).or_default();
            let saved = mon.count;
            mon.owner = None;
            mon.count = 0;
            mon.waiting.push(current);
            let wake = (!mon.blocked.is_empty()).then(|| mon.blocked.remove(0));
            (saved, wake)
        };
        if let Some(idx) = wake {
            self.make_runnable(idx);
        }
        // Timed `wait(ms)`: a deadline (opcode-ticks in green; real time in OS mode) after
        // which the wait returns even without a `notify`. `expire_timed_block` then pulls
        // the thread out of the wait-set so the re-acquire path resumes it (a self-notify).
        // `wait(0)` / `wait()` is an indefinite wait (no deadline) — so a deadline is what
        // tells `Waiting` from `TimedWaiting`, exactly as Java distinguishes them.
        self.shared.threads[current].block_call_pc = self.frame().pc(); // for a possible interrupt throw
        let timed = matches!(timeout, Some(ms) if ms > 0);
        if timed {
            let ms = timeout.expect("timed implies Some") as usize;
            self.shared.threads[current].sleep_until = Some(self.shared.steps + ms);
        }
        self.block(current, if timed { ThreadStatus::TimedWaiting } else { ThreadStatus::Waiting });
        self.shared.threads[current].wait_reacquire = Some((obj, saved));
        self.advance_past_call(); // resume *after* wait() once the monitor is re-acquired
        Step::Continue
    }

    /// `Object.notify()` / `notifyAll()`: move one (or all) parked waiters from the
    /// monitor's wait-set to its blocked-set — they'll re-acquire the monitor once the
    /// notifier releases it. The notifier keeps holding the monitor here.
    fn monitor_notify(&mut self, obj: usize, all: bool) -> Step {
        // `notify`/`notifyAll` also require holding the monitor (JLS 17.2).
        if !self.owns_monitor(obj) {
            return self.throw_exception("java/lang/IllegalMonitorStateException");
        }
        let woken: Vec<usize> = {
            let mon = self.shared.monitors.entry(obj).or_default();
            if all {
                mon.waiting.drain(..).collect()
            } else if mon.waiting.is_empty() {
                Vec::new()
            } else {
                vec![mon.waiting.remove(0)]
            }
        };
        for idx in woken {
            self.shared.monitors.entry(obj).or_default().blocked.push(idx);
            // Notified, but not runnable yet: it must re-acquire the monitor first, so it
            // is contending for a lock — `Blocked`, like any other monitor contender.
            self.block(idx, ThreadStatus::Blocked);
        }
        self.advance_past_call();
        Step::Continue
    }

    /// `Thread.join()`: block the current thread until `target_obj`'s green thread
    /// terminates (woken in `step` when that thread ends). If it already finished — or
    /// was never `start`ed — `join` returns at once.
    fn thread_join(&mut self, target_obj: usize) -> Step {
        let current = self.running.current;
        // Grab the call site *before* advancing, in case an interrupt has to throw from it.
        self.shared.threads[current].block_call_pc = self.frame().pc();
        self.advance_past_call(); // resume after join() (now, or once the target ends)
        let target = self.shared.threads.iter().position(|t| t.thread_obj == target_obj && target_obj != 0);
        if let Some(idx) = target {
            if self.shared.threads[idx].status != ThreadStatus::Terminated {
                // `join()` with no timeout is an indefinite wait on another thread, not
                // monitor contention — `Waiting`, not `Blocked`. (This was `Blocked`.)
                self.block(current, ThreadStatus::Waiting);
                self.shared.threads[current].joining_on = Some(idx);
            }
        }
        Step::Continue
    }

    /// `Thread.getState()`: the scheduler's state for the thread whose `Thread` object is
    /// `thread_obj`, mapped to the matching `Thread.State` constant (returns its heap
    /// offset). The mapping is the whole point of the `ThreadStatus` enum carrying the
    /// *reason* a thread is parked — the translation is now one-to-one.
    ///
    /// `NEW` is **derived**: a `Thread` that was created but never started has no scheduler
    /// slot, so the absence of a slot *is* how the VM says `NEW`.
    fn thread_get_state(&mut self, thread_obj: usize) -> usize {
        let constant = match self.shared.threads.iter().find(|t| t.thread_obj == thread_obj && thread_obj != 0) {
            None => "NEW", // created but not started — no slot exists yet
            Some(t) => match t.status {
                ThreadStatus::Runnable => "RUNNABLE",
                ThreadStatus::Blocked => "BLOCKED",
                ThreadStatus::Waiting => "WAITING",
                ThreadStatus::TimedWaiting => "TIMED_WAITING",
                ThreadStatus::Terminated => "TERMINATED",
            },
        };
        // The constant objects don't exist until `Thread$State.<clinit>` has run.
        self.ensure_initialized("java/lang/Thread$State");
        class_operations::static_reference(
            &mut self.shared.metaspace,
            &mut self.shared.heap,
            "java/lang/Thread$State",
            constant,
        )
    }

    /// `Thread.sleep(ms)`: park the current thread until `ms` opcode-ticks pass (our
    /// clock is the opcode count — there's no wall clock). Other threads run meanwhile;
    /// the sleeper is woken in `step` once the clock reaches its wake time.
    fn thread_sleep(&mut self, ms: i64) -> Step {
        let current = self.running.current;
        // Remember the call site so an interrupt throws `InterruptedException` from here,
        // not from the (advanced) instruction after the call.
        self.shared.threads[current].block_call_pc = self.frame().pc();
        // `sleep(ms)` has a deadline — `TimedWaiting`, not `Blocked`. (This was `Blocked`.)
        self.block(current, ThreadStatus::TimedWaiting);
        self.shared.threads[current].sleep_until = Some(self.shared.steps + ms.max(0) as usize);
        self.advance_past_call();
        Step::Continue
    }

    /// `LockSupport.park()`: block the current thread until unparked — unless it already holds a
    /// **permit** (an `unpark` that arrived first), which it consumes and returns at once. `park`
    /// and `unpark` both escalate to the write path, so they're serialised: the permit check and
    /// the block happen atomically, so an unpark can't be lost between them. Spurious wakeups are
    /// allowed (a caller loops on its condition), which is what makes this safe to build AQS on.
    fn thread_park(&mut self) -> Step {
        let current = self.running.current;
        if self.shared.threads[current].park_permit {
            self.shared.threads[current].park_permit = false;
            self.advance_past_call();
            return Step::Continue;
        }
        self.shared.threads[current].block_call_pc = self.frame().pc();
        self.shared.threads[current].parked = true;
        self.block(current, ThreadStatus::Waiting);
        self.advance_past_call();
        Step::Continue
    }

    /// `LockSupport.unpark(thread)`: hand `thread_obj` a permit. If it's parked in `park()`, wake
    /// it; otherwise store the permit so its next `park()` returns immediately. `unpark(null)` and
    /// unparking a thread with no slot (not started / terminated) are no-ops.
    fn thread_unpark(&mut self, thread_obj: usize) {
        if thread_obj == 0 {
            return;
        }
        let idx = match self.shared.threads.iter().position(|t| t.thread_obj == thread_obj) {
            Some(i) => i,
            None => return,
        };
        if self.shared.threads[idx].parked {
            self.shared.threads[idx].parked = false;
            self.make_runnable(idx);
        } else {
            self.shared.threads[idx].park_permit = true;
        }
    }

    /// A timed block (`Thread.sleep` or `Object.wait(ms)`) reached its deadline: clear it
    /// and make the thread runnable. For a timed `wait`, also pull the thread out of its
    /// monitor's wait-set so the re-acquire path resumes it — the deadline acting as a
    /// self-`notify`. (A plain `sleep` has no monitor and just becomes runnable.)
    fn expire_timed_block(&mut self, idx: usize) {
        self.shared.threads[idx].sleep_until = None;
        if self.shared.threads[idx].status == ThreadStatus::Waiting {
            if let Some((obj, _)) = self.shared.threads[idx].wait_reacquire {
                if let Some(mon) = self.shared.monitors.get_mut(&obj) {
                    mon.waiting.retain(|&w| w != idx);
                }
            }
        }
        self.make_runnable(idx);
    }

    /// Wakes any thread whose timed block (`sleep` or `wait(ms)`) has come due. If *every*
    /// thread is parked on a deadline (no one to advance the opcode clock), the clock jumps
    /// to the earliest wake time so the program can't deadlock on `sleep`/`wait` alone.
    fn wake_sleepers(&mut self) {
        let any_runnable = self.shared.threads.iter().any(|t| t.status == ThreadStatus::Runnable);
        if !any_runnable {
            if let Some(earliest) =
                self.shared.threads.iter().filter_map(|t| t.sleep_until).min()
            {
                self.shared.steps = self.shared.steps.max(earliest);
            }
        }
        let now = self.shared.steps;
        let due: Vec<usize> = self
            .shared.threads
            .iter()
            .enumerate()
            .filter(|(_, t)| matches!(t.sleep_until, Some(at) if now >= at))
            .map(|(i, _)| i)
            .collect();
        for i in due {
            self.expire_timed_block(i);
        }
    }

    /// Runs the single opcode at the current frame's pc and reports what's next — the
    /// dispatch loop body, with no scheduling (so `<clinit>` and `step` can both use it).
    fn run_one(&mut self) -> Step {
        // The VM is at a safepoint between opcodes — poll the GC triggers first.
        self.shared.steps += 1;
        self.safepoint();

        // A thread returning from `wait()` (notified, now scheduled) must re-acquire its
        // monitor before running the instruction after the `wait()` call. If it can't
        // yet, it blocks and retries — exactly like `monitorenter`.
        if let Some((obj, saved)) = self.shared.threads[self.running.current].wait_reacquire {
            let current = self.running.current;
            let acquired = {
                let mon = self.shared.monitors.entry(obj).or_default();
                match mon.owner {
                    None => {
                        mon.owner = Some(current);
                        mon.count = saved;
                        true
                    }
                    _ => {
                        if !mon.blocked.contains(&current) {
                            mon.blocked.push(current);
                        }
                        false
                    }
                }
            };
            if acquired {
                self.shared.threads[current].wait_reacquire = None;
            } else {
                self.block(current, ThreadStatus::Blocked); // still contending for the monitor
                return Step::Continue; // can't re-acquire yet — yield and retry
            }
        }

        // A thread interrupted out of a `sleep`/`join`/`wait` throws `InterruptedException`
        // now — *after* any monitor re-acquire above (a `wait` must hold its lock again
        // before it can throw, JLS 17.2). The pc is rewound to the blocking call so the
        // handler search hits the `try` that wraps it; throwing also **clears** the
        // interrupt status (JLS: the flag is consumed by the throw).
        if self.shared.threads[self.running.current].interrupt_pending {
            let current = self.running.current;
            self.shared.threads[current].interrupt_pending = false;
            self.clear_interrupt_flag(current);
            let call_pc = self.shared.threads[current].block_call_pc;
            self.top().jump(call_pc);
            return self.throw_exception("java/lang/InterruptedException");
        }

        // Keep the code cache current with the top frame before dispatch — a no-op compare
        // unless a frame change (invoke/return/unwind) landed us in a different method.
        self.sync_code_cache();
        let opcode = self.current_code()[self.pc()];
        match opcode {
            // iadd / isub / imul — integer arithmetic
            0x60 => {
                arithmetic_operations::iadd(self.top());
                self.top().advance(1);
                Step::Continue
            }
            0x64 => {
                arithmetic_operations::isub(self.top());
                self.top().advance(1);
                Step::Continue
            }
            0x68 => {
                arithmetic_operations::imul(self.top());
                self.top().advance(1);
                Step::Continue
            }

            // iload_0..iload_3
            0x1a..=0x1d => {
                let slot = (opcode - 0x1a) as usize;
                variable_operations::iload(self.top(), slot);
                self.top().advance(1);
                Step::Continue
            }

            // istore_0..istore_3
            0x3b..=0x3e => {
                let slot = (opcode - 0x3b) as usize;
                variable_operations::istore(self.top(), slot);
                self.top().advance(1);
                Step::Continue
            }

            // aload_0..aload_3 — load a reference local (e.g. `this`)
            0x2a..=0x2d => {
                let slot = (opcode - 0x2a) as usize;
                variable_operations::aload(self.top(), slot);
                self.top().advance(1);
                Step::Continue
            }

            // Indexed load/store forms — the local slot is the next byte (2-byte
            // opcodes), used for slots >= 4: iload/istore (int), aload/astore (ref).
            0x15 => {
                let slot = self.current_code()[self.pc() + 1] as usize;
                variable_operations::iload(self.top(), slot);
                self.top().advance(2);
                Step::Continue
            }
            0x36 => {
                let slot = self.current_code()[self.pc() + 1] as usize;
                variable_operations::istore(self.top(), slot);
                self.top().advance(2);
                Step::Continue
            }
            0x19 => {
                let slot = self.current_code()[self.pc() + 1] as usize;
                variable_operations::aload(self.top(), slot);
                self.top().advance(2);
                Step::Continue
            }
            0x3a => {
                let slot = self.current_code()[self.pc() + 1] as usize;
                variable_operations::astore(self.top(), slot);
                self.top().advance(2);
                Step::Continue
            }

            // wide (0xc4): a **prefix**, not an instruction — it re-runs the opcode
            // that follows with a 16-bit local index, the only way to address the
            // slots past 255 in a method with more than 256 locals. Because every
            // handler above takes `slot: usize`, widening changes nothing but the
            // decoding: the same `iload`/`istore`/`iinc` run, and only the number of
            // bytes the index was written in (and so the instruction length) differs.
            0xc4 => {
                let pc = self.pc();
                let wide = variable_operations::wide_operands(self.current_code(), pc);
                match wide.op {
                    // The four load widths all just move a `Value` onto the stack —
                    // int/long/float/double share one handler, exactly as the narrow
                    // forms do; the verifier is what tells the types apart.
                    0x15..=0x18 => variable_operations::iload(self.top(), wide.slot),
                    0x19 => variable_operations::aload(self.top(), wide.slot),
                    0x36..=0x39 => variable_operations::istore(self.top(), wide.slot),
                    0x3a => variable_operations::astore(self.top(), wide.slot),
                    0x84 => variable_operations::iinc(self.top(), wide.slot, wide.delta),
                    // `wide ret` (0xa9) is the only other form JVMS allows, and
                    // subroutines are rejected outright by the verifier (§4.9.1 bans
                    // `jsr`/`ret` from class files of version 50.0+), so getting here
                    // means unverified bytecode reached the interpreter.
                    other => panic!("wide: unsupported opcode 0x{other:02x} after the prefix"),
                }
                self.top().advance(wide.length);
                Step::Continue
            }

            // astore_0..astore_3 — store a reference into a local
            0x4b..=0x4e => {
                let slot = (opcode - 0x4b) as usize;
                variable_operations::astore(self.top(), slot);
                self.top().advance(1);
                Step::Continue
            }

            // Operand-stack manipulation: pop/dup/swap family (0x57..0x5f). All
            // category-aware (a long/double counts as two slots).
            0x57..=0x5f => {
                let frame = self.top();
                match opcode {
                    0x57 => stack_operations::pop(frame),
                    0x58 => stack_operations::pop2(frame),
                    0x59 => stack_operations::dup(frame),
                    0x5a => stack_operations::dup_x1(frame),
                    0x5b => stack_operations::dup_x2(frame),
                    0x5c => stack_operations::dup2(frame),
                    0x5d => stack_operations::dup2_x1(frame),
                    0x5e => stack_operations::dup2_x2(frame),
                    _ => stack_operations::swap(frame), // 0x5f
                }
                self.top().advance(1);
                Step::Continue
            }

            // aconst_null (0x01): push the null reference (offset 0).
            0x01 => {
                self.top().push(Value::Reference(0));
                self.top().advance(1);
                Step::Continue
            }

            // ldc (0x12, 1-byte index) / ldc_w (0x13, 2-byte index): load a constant
            // — a String literal (materialised on the heap) or an int.
            0x12 => {
                let pc = self.frame().pc();
                let cp_index = self.current_code()[pc + 1] as u16;
                self.ldc(cp_index);
                self.top().advance(2);
                Step::Continue
            }
            0x13 => {
                let pc = self.frame().pc();
                let cp_index = {
                    let code = self.current_code();
                    u16::from_be_bytes([code[pc + 1], code[pc + 2]])
                };
                self.ldc(cp_index);
                self.top().advance(3);
                Step::Continue
            }

            // iconst_m1..iconst_5
            0x02..=0x08 => {
                let value = opcode as i32 - 0x03;
                variable_operations::iconst(self.top(), value);
                self.top().advance(1);
                Step::Continue
            }

            // bipush (0x10): push the signed byte operand as an int.
            0x10 => {
                let value = self.current_code()[self.pc() + 1] as i8 as i32;
                variable_operations::iconst(self.top(), value);
                self.top().advance(2);
                Step::Continue
            }
            // sipush (0x11): push the signed short operand as an int.
            0x11 => {
                let pc = self.pc();
                let value = {
                    let code = self.current_code();
                    i16::from_be_bytes([code[pc + 1], code[pc + 2]]) as i32
                };
                variable_operations::iconst(self.top(), value);
                self.top().advance(3);
                Step::Continue
            }

            // --- long (category-2): the first non-int primitive ------------------
            // lconst_0 / lconst_1 → push the long 0 / 1.
            0x09 | 0x0a => {
                variable_operations::lconst(self.top(), (opcode - 0x09) as i64);
                self.top().advance(1);
                Step::Continue
            }
            // ldc2_w → push a long constant from the pool.
            0x14 => {
                let pc = self.frame().pc();
                let cp_index = {
                    let code = self.current_code();
                    u16::from_be_bytes([code[pc + 1], code[pc + 2]])
                };
                self.ldc2_w(cp_index);
                self.top().advance(3);
                Step::Continue
            }
            // lload_0..lload_3 / lload — load a long local (reuses iload: moving a
            // `Value` is type-agnostic). The slot's high half (index+1) is unused.
            0x1e..=0x21 => {
                let slot = (opcode - 0x1e) as usize;
                variable_operations::iload(self.top(), slot);
                self.top().advance(1);
                Step::Continue
            }
            0x16 => {
                let slot = self.current_code()[self.pc() + 1] as usize;
                variable_operations::iload(self.top(), slot);
                self.top().advance(2);
                Step::Continue
            }
            // lstore_0..lstore_3 / lstore — store a long into a local (reuses istore).
            0x3f..=0x42 => {
                let slot = (opcode - 0x3f) as usize;
                variable_operations::istore(self.top(), slot);
                self.top().advance(1);
                Step::Continue
            }
            0x37 => {
                let slot = self.current_code()[self.pc() + 1] as usize;
                variable_operations::istore(self.top(), slot);
                self.top().advance(2);
                Step::Continue
            }
            // ladd / lsub / lmul — long arithmetic.
            0x61 => {
                arithmetic_operations::ladd(self.top());
                self.top().advance(1);
                Step::Continue
            }
            0x65 => {
                arithmetic_operations::lsub(self.top());
                self.top().advance(1);
                Step::Continue
            }
            0x69 => {
                arithmetic_operations::lmul(self.top());
                self.top().advance(1);
                Step::Continue
            }
            // lreturn → return the long on top (reuses ireturn — it pops any Value).
            0xad => self.ireturn(),

            // --- double (category-2): mirrors long, payload is f64 ---------------
            // dconst_0 / dconst_1 → push the double 0.0 / 1.0.
            0x0e | 0x0f => {
                variable_operations::dconst(self.top(), (opcode - 0x0e) as f64);
                self.top().advance(1);
                Step::Continue
            }
            // dload_0..dload_3 / dload — load a double local (reuses iload).
            0x26..=0x29 => {
                let slot = (opcode - 0x26) as usize;
                variable_operations::iload(self.top(), slot);
                self.top().advance(1);
                Step::Continue
            }
            0x18 => {
                let slot = self.current_code()[self.pc() + 1] as usize;
                variable_operations::iload(self.top(), slot);
                self.top().advance(2);
                Step::Continue
            }
            // dstore_0..dstore_3 / dstore — store a double into a local (reuses istore).
            0x47..=0x4a => {
                let slot = (opcode - 0x47) as usize;
                variable_operations::istore(self.top(), slot);
                self.top().advance(1);
                Step::Continue
            }
            0x39 => {
                let slot = self.current_code()[self.pc() + 1] as usize;
                variable_operations::istore(self.top(), slot);
                self.top().advance(2);
                Step::Continue
            }
            // dadd / dsub / dmul — double arithmetic.
            0x63 => {
                arithmetic_operations::dadd(self.top());
                self.top().advance(1);
                Step::Continue
            }
            0x67 => {
                arithmetic_operations::dsub(self.top());
                self.top().advance(1);
                Step::Continue
            }
            0x6b => {
                arithmetic_operations::dmul(self.top());
                self.top().advance(1);
                Step::Continue
            }
            // dreturn → return the double on top (reuses ireturn — it pops any Value).
            0xaf => self.ireturn(),

            // --- float (category-1): one slot, 4 bytes, payload f32 --------------
            // fconst_0 / fconst_1 / fconst_2 → push 0.0f / 1.0f / 2.0f.
            0x0b | 0x0c | 0x0d => {
                variable_operations::fconst(self.top(), (opcode - 0x0b) as f32);
                self.top().advance(1);
                Step::Continue
            }
            // fload_0..fload_3 / fload — load a float local (reuses iload; 1 slot).
            0x22..=0x25 => {
                let slot = (opcode - 0x22) as usize;
                variable_operations::iload(self.top(), slot);
                self.top().advance(1);
                Step::Continue
            }
            0x17 => {
                let slot = self.current_code()[self.pc() + 1] as usize;
                variable_operations::iload(self.top(), slot);
                self.top().advance(2);
                Step::Continue
            }
            // fstore_0..fstore_3 / fstore — store a float into a local (reuses istore).
            0x43..=0x46 => {
                let slot = (opcode - 0x43) as usize;
                variable_operations::istore(self.top(), slot);
                self.top().advance(1);
                Step::Continue
            }
            0x38 => {
                let slot = self.current_code()[self.pc() + 1] as usize;
                variable_operations::istore(self.top(), slot);
                self.top().advance(2);
                Step::Continue
            }
            // fadd / fsub / fmul — float arithmetic.
            0x62 => {
                arithmetic_operations::fadd(self.top());
                self.top().advance(1);
                Step::Continue
            }
            0x66 => {
                arithmetic_operations::fsub(self.top());
                self.top().advance(1);
                Step::Continue
            }
            0x6a => {
                arithmetic_operations::fmul(self.top());
                self.top().advance(1);
                Step::Continue
            }
            // freturn → return the float on top (reuses ireturn — it pops any Value).
            0xae => self.ireturn(),

            // iinc (0x84): in-place `int` local increment (index + signed const).
            0x84 => {
                let pc = self.pc();
                let slot = self.current_code()[pc + 1] as usize;
                let delta = self.current_code()[pc + 2] as i8 as i32;
                variable_operations::iinc(self.top(), slot, delta);
                self.top().advance(3);
                Step::Continue
            }
            // Integer division / remainder — may throw ArithmeticException on /0.
            0x6c | 0x6d | 0x70 | 0x71 => {
                let result = match opcode {
                    0x6c => arithmetic_operations::idiv(self.top()),
                    0x6d => arithmetic_operations::ldiv(self.top()),
                    0x70 => arithmetic_operations::irem(self.top()),
                    _ => arithmetic_operations::lrem(self.top()), // 0x71
                };
                match result {
                    Ok(()) => {
                        self.top().advance(1);
                        Step::Continue
                    }
                    Err(exc) => self.throw_exception(exc),
                }
            }
            // The rest of arithmetic — float/double div & rem, negation, shifts,
            // bitwise — none of which can throw.
            0x6e | 0x6f | 0x72 | 0x73 | 0x74..=0x83 => {
                let frame = self.top();
                match opcode {
                    0x6e => arithmetic_operations::fdiv(frame),
                    0x6f => arithmetic_operations::ddiv(frame),
                    0x72 => arithmetic_operations::frem(frame),
                    0x73 => arithmetic_operations::drem(frame),
                    0x74 => arithmetic_operations::ineg(frame),
                    0x75 => arithmetic_operations::lneg(frame),
                    0x76 => arithmetic_operations::fneg(frame),
                    0x77 => arithmetic_operations::dneg(frame),
                    0x78 => arithmetic_operations::ishl(frame),
                    0x79 => arithmetic_operations::lshl(frame),
                    0x7a => arithmetic_operations::ishr(frame),
                    0x7b => arithmetic_operations::lshr(frame),
                    0x7c => arithmetic_operations::iushr(frame),
                    0x7d => arithmetic_operations::lushr(frame),
                    0x7e => arithmetic_operations::iand(frame),
                    0x7f => arithmetic_operations::land(frame),
                    0x80 => arithmetic_operations::ior(frame),
                    0x81 => arithmetic_operations::lor(frame),
                    0x82 => arithmetic_operations::ixor(frame),
                    _ => arithmetic_operations::lxor(frame), // 0x83
                }
                self.top().advance(1);
                Step::Continue
            }

            // --- numeric conversions (i2l..i2s, 0x85..0x93) ----------------------
            0x85..=0x93 => {
                let frame = self.top();
                match opcode {
                    0x85 => conversion_operations::i2l(frame),
                    0x86 => conversion_operations::i2f(frame),
                    0x87 => conversion_operations::i2d(frame),
                    0x88 => conversion_operations::l2i(frame),
                    0x89 => conversion_operations::l2f(frame),
                    0x8a => conversion_operations::l2d(frame),
                    0x8b => conversion_operations::f2i(frame),
                    0x8c => conversion_operations::f2l(frame),
                    0x8d => conversion_operations::f2d(frame),
                    0x8e => conversion_operations::d2i(frame),
                    0x8f => conversion_operations::d2l(frame),
                    0x90 => conversion_operations::d2f(frame),
                    0x91 => conversion_operations::i2b(frame),
                    0x92 => conversion_operations::i2c(frame),
                    _ => conversion_operations::i2s(frame), // 0x93
                }
                self.top().advance(1);
                Step::Continue
            }

            // invokestatic / ireturn: the call-stack opcodes (own methods below).
            0xb8 => self.invokestatic(),
            0xac => self.ireturn(),
            // areturn (0xb0): return a *reference*. `ireturn` is type-agnostic — it
            // hands back whatever `Value` is on top — so it serves every typed return.
            0xb0 => self.ireturn(),

            // invokespecial / return: constructor calls and void returns.
            0xb7 => self.invokespecial(),
            0xb1 => self.return_void(),

            // invokevirtual / invokeinterface: dynamically-dispatched instance calls.
            0xb6 => self.invokevirtual(),
            0xb9 => self.invokeinterface(),

            // invokedynamic (0xba): `ba idx1 idx2 00 00` — 5 bytes, the trailing two
            // always zero. Unlike its siblings it pushes no frame: the bootstrap
            // methods are VM intrinsics, so the call site's value is produced directly
            // and the pc simply steps over the instruction.
            0xba => {
                let pc = self.frame().pc();
                let cp_index = {
                    let code = self.current_code();
                    u16::from_be_bytes([code[pc + 1], code[pc + 2]])
                };
                self.invokedynamic(cp_index);
                self.top().advance(5);
                Step::Continue
            }

            // athrow: throw an exception, unwinding the call stack to a handler.
            0xbf => self.athrow(),

            // nop (0x00): do nothing but step over itself. `javac` doesn't emit it,
            // but it is legal bytecode — obfuscators and instrumentation tools use it
            // as padding, and the switch alignment rules make it easy to synthesise.
            0x00 => {
                self.top().advance(1);
                Step::Continue
            }

            // goto (0xa7) / if_icmpgt (0xa3): branches. Read the signed 2-byte
            // offset, then let the branch family jump or fall through — it manages
            // the pc itself, so no blind advance here.
            0xa7 => {
                let offset = self.branch_offset();
                bifurcation_operations::goto(self.top(), offset);
                Step::Continue
            }
            // goto_w (0xc8): the same jump reading a 4-byte offset, for targets
            // farther than ±32 KB — out of reach of the 2-byte form.
            0xc8 => {
                let offset = self.wide_branch_offset();
                bifurcation_operations::goto_w(self.top(), offset);
                Step::Continue
            }
            0xa3 => {
                let offset = self.branch_offset();
                bifurcation_operations::if_icmpgt(self.top(), offset);
                Step::Continue
            }
            // if_icmpeq/ne/lt/ge/le (0x9f/0xa0/0xa1/0xa2/0xa4): the rest of the
            // two-int comparison branches (0xa3 if_icmpgt is just above).
            0x9f => {
                let offset = self.branch_offset();
                bifurcation_operations::if_icmpeq(self.top(), offset);
                Step::Continue
            }
            0xa0 => {
                let offset = self.branch_offset();
                bifurcation_operations::if_icmpne(self.top(), offset);
                Step::Continue
            }
            0xa1 => {
                let offset = self.branch_offset();
                bifurcation_operations::if_icmplt(self.top(), offset);
                Step::Continue
            }
            0xa2 => {
                let offset = self.branch_offset();
                bifurcation_operations::if_icmpge(self.top(), offset);
                Step::Continue
            }
            0xa4 => {
                let offset = self.branch_offset();
                bifurcation_operations::if_icmple(self.top(), offset);
                Step::Continue
            }
            // ifeq (0x99) / ifne (0x9a): branch on a single int vs 0 — what a Java
            // `if (booleanExpr)` (e.g. an `instanceof`) compiles to.
            0x99 => {
                let offset = self.branch_offset();
                bifurcation_operations::ifeq(self.top(), offset);
                Step::Continue
            }
            0x9a => {
                let offset = self.branch_offset();
                bifurcation_operations::ifne(self.top(), offset);
                Step::Continue
            }
            // iflt / ifge / ifgt / ifle (0x9b..0x9e): branch on an int vs 0 — the
            // forms a `lcmp`/`fcmp`/`dcmp` verdict feeds into.
            0x9b => {
                let offset = self.branch_offset();
                bifurcation_operations::iflt(self.top(), offset);
                Step::Continue
            }
            0x9c => {
                let offset = self.branch_offset();
                bifurcation_operations::ifge(self.top(), offset);
                Step::Continue
            }
            0x9d => {
                let offset = self.branch_offset();
                bifurcation_operations::ifgt(self.top(), offset);
                Step::Continue
            }
            0x9e => {
                let offset = self.branch_offset();
                bifurcation_operations::ifle(self.top(), offset);
                Step::Continue
            }
            // lcmp / fcmpl / fcmpg / dcmpl / dcmpg (0x94..0x98): pop two long/float/
            // double and push the int verdict (1/0/-1) for a following `if<cond>`.
            0x94..=0x98 => {
                let frame = self.top();
                match opcode {
                    0x94 => comparison_operations::lcmp(frame),
                    0x95 => comparison_operations::fcmpl(frame),
                    0x96 => comparison_operations::fcmpg(frame),
                    0x97 => comparison_operations::dcmpl(frame),
                    _ => comparison_operations::dcmpg(frame), // 0x98
                }
                self.top().advance(1);
                Step::Continue
            }
            // if_acmpeq (0xa5) / if_acmpne (0xa6): branch on reference identity.
            0xa5 => {
                let offset = self.branch_offset();
                bifurcation_operations::if_acmpeq(self.top(), offset);
                Step::Continue
            }
            0xa6 => {
                let offset = self.branch_offset();
                bifurcation_operations::if_acmpne(self.top(), offset);
                Step::Continue
            }
            // ifnull (0xc6) / ifnonnull (0xc7): branch on a reference vs null.
            0xc6 => {
                let offset = self.branch_offset();
                bifurcation_operations::ifnull(self.top(), offset);
                Step::Continue
            }
            0xc7 => {
                let offset = self.branch_offset();
                bifurcation_operations::ifnonnull(self.top(), offset);
                Step::Continue
            }

            // new (0xbb): allocate an object — delegated to the class/object family.
            // Read the u2 class index, let class_operations allocate it (touching
            // both the metaspace and the heap), then advance past the 3-byte op.
            0xbb => {
                let method = self.frame().method();
                let pc = self.frame().pc();
                let cp_index = {
                    let code = self.shared.metaspace.code(method);
                    u16::from_be_bytes([code[pc + 1], code[pc + 2]])
                };
                self.initialize_class_at(cp_index); // first active use: run <clinit>
                if let Some(step) = self.take_pending_throw() {
                    return step; // <clinit> failed → throw instead of allocating
                }
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                // A full heap returns Err → the VM throws a catchable OutOfMemoryError.
                match class_operations::new(&mut self.shared.metaspace, &mut self.shared.heap, frame, cp_index) {
                    Ok(()) => {
                        self.top().advance(3);
                        Step::Continue
                    }
                    Err(exc) => self.throw_exception(exc),
                }
            }

            // getstatic (0xb2) / putstatic (0xb3): read/write a *static* field in the
            // class's mirror (no receiver — located by class, not by an objectref).
            0xb2 => {
                let pc = self.frame().pc();
                let cp_index = {
                    let code = self.current_code();
                    u16::from_be_bytes([code[pc + 1], code[pc + 2]])
                };
                self.initialize_field_owner_at(cp_index); // first active use: run <clinit>
                if let Some(step) = self.take_pending_throw() {
                    return step; // <clinit> failed → throw instead of touching the static field
                }
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                class_operations::getstatic(&mut self.shared.metaspace, &mut self.shared.heap, frame, cp_index);
                self.top().advance(3);
                Step::Continue
            }
            0xb3 => {
                let pc = self.frame().pc();
                let cp_index = {
                    let code = self.current_code();
                    u16::from_be_bytes([code[pc + 1], code[pc + 2]])
                };
                self.initialize_field_owner_at(cp_index); // first active use: run <clinit>
                if let Some(step) = self.take_pending_throw() {
                    return step; // <clinit> failed → throw instead of touching the static field
                }
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                class_operations::putstatic(&mut self.shared.metaspace, &mut self.shared.heap, frame, cp_index);
                self.top().advance(3);
                Step::Continue
            }

            // getfield (0xb4) / putfield (0xb5): read/write an object's field on the
            // heap. Read the u2 FieldRef index, let the object family resolve the
            // field's offset and do the access, then step past the 3-byte op.
            0xb4 => {
                let pc = self.frame().pc();
                let cp_index = {
                    let code = self.current_code();
                    u16::from_be_bytes([code[pc + 1], code[pc + 2]])
                };
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                // A null receiver makes the field op return Err → the VM throws a NPE.
                match objects_operations::getfield(&mut self.shared.metaspace, &mut self.shared.heap, frame, cp_index) {
                    Ok(()) => {
                        self.top().advance(3);
                        Step::Continue
                    }
                    Err(exc) => self.throw_exception(exc),
                }
            }
            0xb5 => {
                let pc = self.frame().pc();
                let cp_index = {
                    let code = self.current_code();
                    u16::from_be_bytes([code[pc + 1], code[pc + 2]])
                };
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                match objects_operations::putfield(&mut self.shared.metaspace, &mut self.shared.heap, frame, cp_index) {
                    Ok(()) => {
                        self.top().advance(3);
                        Step::Continue
                    }
                    Err(exc) => self.throw_exception(exc),
                }
            }

            // arraylength (0xbe): push an array's length (null array → NPE).
            0xbe => {
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::arraylength(&self.shared.heap, frame);
                self.after_array_op(r, 1)
            }
            // Array loads — push an element: iaload(int) / baload(byte,bool) /
            // caload(char) / saload(short) / aaload(reference). Null array → NPE,
            // out-of-range index → ArrayIndexOutOfBoundsException.
            0x2e => {
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::iaload(&self.shared.heap, frame);
                self.after_array_op(r, 1)
            }
            0x33 => {
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::baload(&self.shared.heap, frame);
                self.after_array_op(r, 1)
            }
            0x34 => {
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::caload(&self.shared.heap, frame);
                self.after_array_op(r, 1)
            }
            0x35 => {
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::saload(&self.shared.heap, frame);
                self.after_array_op(r, 1)
            }
            0x32 => {
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::aaload(&self.shared.heap, frame);
                self.after_array_op(r, 1)
            }
            // laload / faload / daload (0x2f/0x30/0x31): long/float/double elements.
            0x2f => {
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::laload(&self.shared.heap, frame);
                self.after_array_op(r, 1)
            }
            0x30 => {
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::faload(&self.shared.heap, frame);
                self.after_array_op(r, 1)
            }
            0x31 => {
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::daload(&self.shared.heap, frame);
                self.after_array_op(r, 1)
            }
            // Array stores — pop value into an element: iastore(int) /
            // bastore(byte,bool) / castore(char) / sastore(short) / aastore(ref).
            // lastore / fastore / dastore (0x50/0x51/0x52): long/float/double elements.
            0x50 => {
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::lastore(&mut self.shared.heap, frame);
                self.after_array_op(r, 1)
            }
            0x51 => {
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::fastore(&mut self.shared.heap, frame);
                self.after_array_op(r, 1)
            }
            0x52 => {
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::dastore(&mut self.shared.heap, frame);
                self.after_array_op(r, 1)
            }
            0x4f => {
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::iastore(&mut self.shared.heap, frame);
                self.after_array_op(r, 1)
            }
            0x54 => {
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::bastore(&mut self.shared.heap, frame);
                self.after_array_op(r, 1)
            }
            0x55 => {
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::castore(&mut self.shared.heap, frame);
                self.after_array_op(r, 1)
            }
            0x56 => {
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::sastore(&mut self.shared.heap, frame);
                self.after_array_op(r, 1)
            }
            0x53 => {
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::aastore(&mut self.shared.metaspace, &mut self.shared.heap, frame);
                self.after_array_op(r, 1)
            }

            // instanceof (0xc1) / checkcast (0xc0): runtime type checks against the
            // Class at the u2 index — both 3-byte ops, dispatched to the type family.
            0xc1 => {
                let pc = self.frame().pc();
                let cp_index = {
                    let code = self.current_code();
                    u16::from_be_bytes([code[pc + 1], code[pc + 2]])
                };
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                class_operations::instanceof(&mut self.shared.metaspace, &self.shared.heap, frame, cp_index);
                self.top().advance(3);
                Step::Continue
            }
            0xc0 => {
                let pc = self.frame().pc();
                let cp_index = {
                    let code = self.current_code();
                    u16::from_be_bytes([code[pc + 1], code[pc + 2]])
                };
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                // A bad cast returns Err → the VM throws ClassCastException.
                match class_operations::checkcast(&mut self.shared.metaspace, &self.shared.heap, frame, cp_index) {
                    Ok(()) => {
                        self.top().advance(3);
                        Step::Continue
                    }
                    Err(exc) => self.throw_exception(exc),
                }
            }

            // newarray (0xbc) / anewarray (0xbd): allocate an array on the heap.
            // newarray takes a 1-byte element type (atype) → 2-byte op; anewarray
            // takes a u2 Class index for the element type → 3-byte op.
            0xbc => {
                let pc = self.frame().pc();
                let atype = self.current_code()[pc + 1];
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::newarray(&mut self.shared.metaspace, &mut self.shared.heap, frame, atype);
                self.after_array_op(r, 2) // negative size → NegativeArraySizeException
            }
            0xbd => {
                let pc = self.frame().pc();
                let cp_index = {
                    let code = self.current_code();
                    u16::from_be_bytes([code[pc + 1], code[pc + 2]])
                };
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::anewarray(&mut self.shared.metaspace, &mut self.shared.heap, frame, cp_index);
                self.after_array_op(r, 3)
            }
            // multianewarray (0xc5): allocate a multidimensional array — a u2 Class
            // index naming the *array* type (`[[I`, not the element type as in
            // anewarray) plus a u1 dimension count → 4-byte op. Only that many levels
            // are built, so `new int[3][]` leaves the inner slots null.
            0xc5 => {
                let pc = self.frame().pc();
                let (cp_index, dimensions) = {
                    let code = self.current_code();
                    (u16::from_be_bytes([code[pc + 1], code[pc + 2]]), code[pc + 3])
                };
                let frame = self.running.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::multianewarray(
                    &mut self.shared.metaspace,
                    &mut self.shared.heap,
                    frame,
                    cp_index,
                    dimensions,
                );
                self.after_array_op(r, 4) // negative size → NegativeArraySizeException
            }

            // tableswitch (0xaa) / lookupswitch (0xab): pop the int key and jump to its
            // case (or `default`). Variable-length, so the target is computed from the
            // table; the frame's pc still points at the switch (offsets are relative).
            0xaa | 0xab => {
                let method = self.frame().method();
                let pc = self.frame().pc();
                let key = match self.top().pop() {
                    Value::Int(v) => v,
                    other => panic!("switch expected an int key, found {other:?}"),
                };
                let target = bifurcation_operations::switch_target(self.shared.metaspace.code(method), pc, key);
                self.top().jump(target);
                Step::Continue
            }

            // monitorenter (0xc2) / monitorexit (0xc3): acquire/release an object's
            // monitor for `synchronized` — cooperative blocking via the scheduler.
            0xc2 => self.monitor_enter(),
            0xc3 => self.monitor_exit(),

            other => todo!("opcode 0x{other:02x} not implemented yet"),
        }
    }

    /// `return` (0xb1): end a `void` method (a constructor here). Pop the frame; if
    /// it was the entry the program is done (with no value), else the caller resumes
    /// — nothing is handed back, unlike `ireturn`.
    fn return_void(&mut self) -> Step {
        let popped = self.pop_frame();
        if self.running.frames.is_empty() {
            return Step::Return(None);
        }
        // A synthetic `<clinit>` frame wasn't reached via an invoke, so the caller's
        // pc must NOT advance — the instruction that triggered init resumes as-is.
        if !popped.is_some_and(|f| f.is_synthetic()) {
            self.advance_past_call();
        }
        Step::Continue
    }

    /// Ensures `class` is **initialized** before its first active use (JVMS §5.5):
    /// runs its `<clinit>` exactly once, *after* its superclass and the superinterfaces
    /// that declare default methods are initialized (see below, and JVMS §5.5). The
    /// `<clinit>` runs synchronously — pushed as a synthetic frame and stepped to
    /// completion — so it finishes before the triggering instruction proceeds.
    /// `InProgress` short-circuits re-entrant uses (a class touching itself mid-init).
    fn ensure_initialized(&mut self, class: &str) {
        match self.shared.metaspace.init_state(class) {
            InitState::Done | InitState::InProgress => return,
            InitState::Erroneous => {
                // A prior initialization threw: the class is permanently unusable — every active
                // use now fails with NoClassDefFoundError (JVMS §5.5).
                let ncdfe = self.new_exception_object("java/lang/NoClassDefFoundError");
                self.running.pending_exception = Some(ncdfe);
                return;
            }
            InitState::NotStarted => {}
        }
        self.shared.metaspace.set_init_state(class, InitState::InProgress);

        // Superclass first — initializing Dog initializes Animal (then Object). If the superclass
        // fails to initialize, this class can't either; it becomes erroneous and the failure
        // propagates unchanged.
        if let Some(superclass) = self.shared.metaspace.superclass_name(class) {
            self.ensure_initialized(&superclass);
            if self.running.pending_exception.is_some() {
                self.shared.metaspace.set_init_state(class, InitState::Erroneous);
                return;
            }
        }

        // Then the superinterfaces that declare **default methods** — and only those (JVMS §5.5).
        // Merely implementing an interface is not an active use of it: an interface holding just
        // constants and abstract methods stays uninitialized until someone reads one of its own
        // non-constant static fields. A default method, though, is code the instance can run, so
        // the interface has to be initialized alongside the class. Symmetrically, initializing an
        // *interface* initializes none of its superinterfaces — `default_method_superinterfaces`
        // returns nothing for one. A failure propagates exactly like the superclass's.
        for iface in self.shared.metaspace.default_method_superinterfaces(class) {
            self.ensure_initialized(&iface);
            if self.running.pending_exception.is_some() {
                self.shared.metaspace.set_init_state(class, InitState::Erroneous);
                return;
            }
        }

        // Run the class's `<clinit>` (if it has one) to completion. It is the
        // argument-less, result-less case of [`Self::call_java`] — the same VM-pushed
        // frame driven by the same nested loop. If it completes abruptly, mark the class
        // erroneous and, unless the exception is already an Error, wrap it in
        // ExceptionInInitializerError (JVMS §5.5) before it reaches the triggering code.
        if let Some(clinit) = self.shared.metaspace.resolve_method(class, "<clinit>", "()V") {
            self.call_java(clinit, Vec::new(), &[]);
            if let Some(exc) = self.running.pending_exception.take() {
                self.shared.metaspace.set_init_state(class, InitState::Erroneous);
                let exc_class = self.exception_class_name(exc);
                let is_error = class_operations::is_subtype(
                    &mut self.shared.metaspace,
                    &exc_class,
                    "java/lang/Error",
                );
                let delivered = if is_error {
                    exc
                } else {
                    self.new_exception_object("java/lang/ExceptionInInitializerError")
                };
                self.running.pending_exception = Some(delivered);
                return;
            }
        }
        self.shared.metaspace.set_init_state(class, InitState::Done);
    }

    /// Runs a Java method **from inside the VM** and hands back its result.
    ///
    /// This is the general form of what class initialization has always done: push a
    /// frame the VM owns and drive it to completion with a nested `run_one` loop, so the
    /// call finishes before the instruction that triggered it proceeds. `<clinit>` was
    /// just the special case with no arguments and no result.
    ///
    /// It matters well beyond initialization, because it is what stops the VM's
    /// intrinsics from being *terminal*. A native that can only compute and return has to
    /// reimplement anything it needs from the library — which is how `String.valueOf`
    /// ended up half-written in Rust. With this, an intrinsic can call back into Java:
    /// the natural way for `toString()` on a record component, `String.valueOf(Object)`
    /// in concatenation, and a real `ConstantBootstraps.invoke`.
    ///
    /// The frame is marked **synthetic**, so the returning opcode knows there is no call
    /// instruction to step over in the caller (the caller is a native, not an `invoke`).
    ///
    /// Returns the method's result, or `None` for a `void` method.
    pub(super) fn call_java(
        &mut self,
        method: MethodId,
        args: Vec<Value>,
        widths: &[usize],
    ) -> Option<Value> {
        // VM-pushed synthetic frames count against the same depth limit (JVMS §6.3).
        // `call_java` reports failure through `pending_exception` (see the return below),
        // so the overflow surfaces the same way any exception escaping the callee would —
        // as a bare object, like the NoClassDefFoundError in `ensure_initialized`.
        if self.running.frames.len() >= Self::MAX_FRAMES {
            let soe = self.new_exception_object("java/lang/StackOverflowError");
            self.running.pending_exception = Some(soe);
            return None;
        }
        let max_locals = self.shared.metaspace.max_locals(method);
        let base = self.running.frames.len();
        // The caller's stack is where a returning value lands, so its depth before the
        // call is what tells us afterwards whether anything came back.
        let depth_before = self.running.frames.last().map_or(0, |f| f.stack().len());

        let mut frame = Frame::for_call(method, max_locals, args, widths);
        frame.mark_synthetic();
        self.running.frames.push(frame);
        // Mark this call's boundary: an exception that reaches `base` with no handler stops there
        // (see `unwind_with`) and surfaces via `pending_exception` rather than unwinding further.
        self.running.exception_floor.push(base);

        // Drive it on the *current* thread — `run_one`, not `step`, so the scheduler
        // can't interleave another thread in the middle of a VM-initiated call.
        while self.running.frames.len() > base {
            self.run_one();
        }
        self.running.exception_floor.pop();

        // The callee threw and it unwound out to our boundary: there is no return value, and the
        // exception stays pending for our caller to re-deliver.
        if self.running.pending_exception.is_some() {
            return None;
        }

        let grew = self.running.frames.last().is_some_and(|f| f.stack().len() > depth_before);
        grew.then(|| self.top().pop())
    }

    /// Calls a method **on an object**, dispatched by its runtime class — the virtual
    /// call an intrinsic needs when the answer depends on user code.
    ///
    /// Wraps the two shapes a callee can take: a native goes to the bridge, anything else
    /// gets a frame via [`Self::call_java`]. `None` means the receiver's class has no such
    /// method, which is a legitimate answer rather than an error — our `java.lang.Object`
    /// declares no `equals`, so "no slot" *is* how a class says it inherits identity
    /// comparison.
    pub(super) fn call_virtual(
        &mut self,
        receiver: usize,
        name: &str,
        descriptor: &str,
        args: Vec<Value>,
    ) -> Option<Value> {
        let runtime = self.shared.metaspace.class_name_at_mirror(self.shared.heap.read_u32(receiver) as usize)?;
        let runtime = runtime.to_string();
        let slot = self.shared.metaspace.vtable_slot(&runtime, name, descriptor)?;
        let callee = self.shared.metaspace.vtable_method(&runtime, slot)?;

        let mut operands = vec![Value::Reference(receiver)];
        operands.extend(args);

        if self.shared.metaspace.is_native(callee) {
            let class = self.shared.metaspace.class_of(callee).to_string();
            return crate::jvm::interpreter::natives::dispatch(
                &class,
                name,
                descriptor,
                &operands,
                &mut self.shared.metaspace,
                &mut self.shared.heap,
                &mut self.shared.console,
            );
        }
        let mut widths = vec![1]; // the receiver, then each parameter at its own width
        widths.extend(MetaspaceService::param_slot_widths(descriptor));
        self.call_java(callee, operands, &widths)
    }

    /// `ldc`/`ldc_w`: resolve the constant at `cp_index` in the current method's pool
    /// and push it. A `String` literal is materialised as a heap String and pushed as
    /// a reference; an `Integer` is pushed as an int.
    fn ldc(&mut self, cp_index: u16) {
        let caller = self.shared.metaspace.class_of(self.frame().method()).to_string();

        // A String literal → materialise it on the heap, push the reference.
        let text = self.shared.metaspace.get(&caller).and_then(|cf| cf.string_constant(cp_index)).map(str::to_string);
        if let Some(text) = text {
            let offset = strings::intern(&mut self.shared.metaspace, &mut self.shared.heap, &text);
            self.top().push(Value::Reference(offset));
            return;
        }

        // An int constant → push it directly.
        if let Some(value) = self.shared.metaspace.get(&caller).and_then(|cf| cf.integer_constant(cp_index)) {
            self.top().push(Value::Int(value));
            return;
        }

        // A float constant (category-1, so it comes through `ldc`, not `ldc2_w`).
        if let Some(value) = self.shared.metaspace.get(&caller).and_then(|cf| cf.float_constant(cp_index)) {
            self.top().push(Value::Float(value));
            return;
        }

        // A **class literal** (`Foo.class`) → push the class's `Class<…>` mirror.
        //
        // Resolution *loads and prepares* the class but must **not initialize** it: a
        // class literal is not an "active use" (JVMS §5.5), so `Foo.class` alone must not
        // run `Foo.<clinit>`. `load_class` stops at preparation, which is exactly right.
        //
        // The mirror is cached by Class ID, so the same literal evaluated twice yields the
        // *same* reference — which is what makes `Foo.class == Foo.class` hold.
        let class_name =
            self.shared.metaspace.get(&caller).and_then(|cf| cf.class_name(cp_index)).map(str::to_string);
        if let Some(class_name) = class_name {
            // An **array** class literal (`int[].class`) names a class that has no
            // `.class` file, so loading it can't prepare a mirror. It gets the same
            // synthetic, header-only mirror `anewarray` builds — the array type's
            // identity lives in its descriptor, not in a file.
            let mirror = if class_name.starts_with('[') {
                array_operations::array_class_mirror(
                    &mut self.shared.metaspace,
                    &mut self.shared.heap,
                    &class_name,
                )
            } else {
                class_operations::load_class(&mut self.shared.metaspace, &mut self.shared.heap, &class_name);
                self.shared.metaspace.class_mirror(&class_name).unwrap_or_else(|| {
                    // Pushing a null mirror would be a silently wrong answer.
                    panic!("ldc: class '{class_name}' loaded but has no Class mirror")
                })
            };
            self.top().push(Value::Reference(mirror));
            return;
        }

        // A **MethodType** constant → materialise a `MethodType` carrying the descriptor. `javac`
        // never emits an `ldc` of one (they live in the pool only as bootstrap arguments), so this
        // path is reached only through hand-written class files — the `.class` writer.
        let method_type = self
            .shared.metaspace
            .get(&caller)
            .and_then(|cf| cf.method_type_descriptor(cp_index))
            .map(str::to_string);
        if let Some(descriptor) = method_type {
            let object = self.materialize_method_type(&descriptor);
            self.top().push(Value::Reference(object));
            return;
        }

        // A **MethodHandle** constant → materialise a `MethodHandle` naming its target (kind, class,
        // member, descriptor). Same "hand-written only" story as `MethodType`.
        let method_handle = self.shared.metaspace.get(&caller).and_then(|cf| cf.method_handle(cp_index)).map(
            |h| (h.kind.to_byte() as i32, h.class.to_string(), h.name.to_string(), h.descriptor.to_string()),
        );
        if let Some((kind, class, name, descriptor)) = method_handle {
            let object = self.materialize_method_handle(kind, &class, &name, &descriptor);
            self.top().push(Value::Reference(object));
            return;
        }

        panic!(
            "ldc: unsupported constant at #{cp_index} (String/Integer/Float/Class/MethodType/\
             MethodHandle modelled; Dynamic still pending — see docs/invokedynamic-ruta.md)"
        );
    }

    /// `ldc2_w` (0x14): load a category-2 constant — a `long` or a `double` — and
    /// push it. The pool index points at the `Long`/`Double` entry.
    fn ldc2_w(&mut self, cp_index: u16) {
        let caller = self.shared.metaspace.class_of(self.frame().method()).to_string();
        if let Some(value) = self.shared.metaspace.get(&caller).and_then(|cf| cf.long_constant(cp_index)) {
            self.top().push(Value::Long(value));
        } else if let Some(value) =
            self.shared.metaspace.get(&caller).and_then(|cf| cf.double_constant(cp_index))
        {
            self.top().push(Value::Double(value));
        } else {
            panic!("ldc2_w: unsupported constant at #{cp_index} (only Long/Double modelled)");
        }
    }

    /// Initializes the class named by the `Class` constant at `cp_index` in the
    /// current method's pool — the trigger for `new`.
    fn initialize_class_at(&mut self, cp_index: u16) {
        let caller = self.shared.metaspace.class_of(self.frame().method()).to_string();
        let class = self.shared.metaspace.get(&caller).and_then(|cf| cf.class_name(cp_index)).map(str::to_string);
        if let Some(class) = class {
            self.ensure_initialized(&class);
        }
    }

    /// Initializes the class that owns the field at `cp_index` (a `Fieldref`) — the
    /// trigger for `getstatic`/`putstatic`.
    fn initialize_field_owner_at(&mut self, cp_index: u16) {
        let caller = self.shared.metaspace.class_of(self.frame().method()).to_string();
        let owner = self
            .shared.metaspace
            .get(&caller)
            .and_then(|cf| cf.fieldref_target(cp_index))
            .map(|(class, _, _)| class.to_string());
        if let Some(owner) = owner {
            self.ensure_initialized(&owner);
        }
    }

    /// `ireturn` (0xac): end the current method with the int on top of its stack.
    /// Pop the returning frame; if it was the entry method the program is done,
    /// otherwise the value lands on the caller's operand stack and it resumes.
    fn ireturn(&mut self) -> Step {
        let value = self.top().pop();
        let popped = self.pop_frame();
        if self.running.frames.is_empty() {
            return Step::Return(Some(value));
        }
        // Same rule as `return_void`: a frame the VM pushed itself wasn't reached through
        // an invoke, so there is no call instruction to step over — advancing would move
        // the caller's pc by the width of whatever opcode happens to sit there.
        if !popped.is_some_and(|f| f.is_synthetic()) {
            self.advance_past_call();
        }
        self.top().push(value);
        Step::Continue
    }

    /// Advances the (now-top) caller past the invoke that called the just-returned
    /// method. The invoke instructions deliberately *don't* advance the caller's pc
    /// when they push a callee — they leave it pointing *at* the invoke, so an
    /// exception thrown in the callee unwinds to the right pc. A normal return is
    /// where that pc finally steps over the call. Length: `invokeinterface` is 5
    /// bytes, the other invokes 3.
    fn advance_past_call(&mut self) {
        let method = self.frame().method();
        let pc = self.frame().pc();
        let opcode = self.shared.metaspace.code(method)[pc];
        let length = if opcode == 0xb9 { 5 } else { 3 };
        self.top().advance(length);
    }

    /// Finishes an array opcode that may have faulted: on success advance `length`
    /// bytes and continue; on a fault throw the implicit exception (NPE / index out
    /// of bounds / negative size) the helper signalled.
    fn after_array_op(&mut self, result: Result<(), &'static str>, length: usize) -> Step {
        match result {
            Ok(()) => {
                self.top().advance(length);
                Step::Continue
            }
            Err(exc) => self.throw_exception(exc),
        }
    }
}

/// Runs the entry method to completion, returning its result. Thin driver over
/// [`JVM::step`] — the same loop the visualizer runs, minus the pausing.
pub fn execute(metaspace: MetaspaceService, entry: Frame) -> Option<Value> {
    match ThreadMode::from_env() {
        // Cooperative green threads on this one OS thread (the original engine).
        ThreadMode::Green => {
            let mut interp = JVM::new(metaspace, entry);
            loop {
                if let Step::Return(value) = interp.exec().step() {
                    return value;
                }
            }
        }
        // Real OS threads serialised by a GIL.
        ThreadMode::OsGil => execute_os_gil(metaspace, entry),
        // Real OS threads without the GIL (H3, in progress — still shares the os-gil engine).
        ThreadMode::OsParallel => execute_os_parallel(metaspace, entry),
    }
}

/// `os-gil` substrate: the program runs under a **GIL** — the shared VM state lives behind one
/// `Arc<Mutex<SharedVm>>`, locked per opcode (each OS thread owns its own `RunningCtx`). The
/// main thread drives the loop on *this* OS thread; each `Thread.start()` launches a real
/// `std::thread` that competes for the same lock. Only the lock holder mutates shared state, so
/// the heap, monitors and GC stay correct with no extra synchronisation — the lock *is* the
/// stop-the-world. Releasing it for the frame-local opcodes (+ a real STW handshake for GC) is
/// the remaining H3 step (1d).
pub(crate) fn execute_os_gil(metaspace: MetaspaceService, entry: Frame) -> Option<Value> {
    run_os_threaded(metaspace, entry, ThreadMode::OsGil)
}

/// `os` substrate: real OS threads **without** the GIL — true parallelism (H3).
///
/// *In progress.* Today it runs on the **same engine** as [`execute_os_gil`] (the shared state
/// behind one `Arc<Mutex<SharedVm>>`, locked per opcode), so it is correct and oracle-validated
/// but **not yet actually parallel**. This is deliberate: the plan is to *shrink* that lock —
/// release it for frame-local opcodes, add a safepoint handshake + TLABs, then fine-grained
/// per-structure locks — not to remove it in one flag day. Running under the shared engine now
/// gives the mode a place to stand and keeps every step diffable against `green`/`os-gil`/`java`.
pub(crate) fn execute_os_parallel(metaspace: MetaspaceService, entry: Frame) -> Option<Value> {
    run_os_parallel(metaspace, entry)
}

/// Shared setup + driver for both OS-threaded substrates (`os-gil` and `os`). They differ
/// only in the `mode` tag today; that tag is where the lock-shrinking work will branch.
///
/// H3 1c-ii: only the **`SharedVm`** is shared (behind one `Arc<Mutex<SharedVm>>`); each OS
/// thread owns a thread-local [`RunningCtx`] and pairs it with a lock guard per opcode via
/// [`Exec`]. Still one lock per opcode (serialised, oracle-green) — 1d releases it for the
/// frame-local opcodes that touch only the thread-local context.
fn run_os_threaded(metaspace: MetaspaceService, entry: Frame, mode: ThreadMode) -> Option<Value> {
    debug_assert!(mode.uses_os_threads(), "run_os_threaded needs an OS-threaded mode");
    let mut jvm = JVM::new(metaspace, entry);
    jvm.shared.mode = mode; // force the OS substrate regardless of the env (e.g. in tests)
    // The main thread runs the loop on *this* OS thread; record its handle so workers can
    // `unpark` it (e.g. when a join target finishes) instead of waiting out the poll.
    jvm.shared.threads[0].os_handle = Some(thread::current());
    // OS-mode invariant: between turns every thread's stack lives in *its slot*; each OS thread
    // `activate`s it into its own local `RunningCtx` to run. `JVM::new` follows the green
    // convention (main's entry frame in the owner's active `running`, slot 0 empty), so move it
    // into slot 0 before handing the *shared* state off to the threads (the owner is dropped).
    std::mem::swap(&mut jvm.running.frames, &mut jvm.shared.threads[0].frames);
    let shared = Arc::new(Mutex::new(jvm.shared));
    os_thread_loop(&shared, 0)
}

/// What an OS thread does after one turn under the GIL — decided while holding the lock,
/// then acted on after releasing it (so we never block/sleep with the GIL held).
enum OsTick {
    /// This thread's stack returned — its result (program result for `main`).
    Done(Option<Value>),
    /// Ran an opcode and stayed runnable: let a sibling grab the GIL, then loop.
    Yield,
    /// Blocked on a monitor / join / wait: park until `unpark` (poll-capped as a backstop).
    Park,
    /// Sleeping: in OS mode `Thread.sleep` is **real wall time** (the opcode clock can stall
    /// when every thread is blocked), capped so tests stay quick.
    Sleep(u64),
}

/// One thread's run loop on its own OS thread. Owns a thread-local [`RunningCtx`]; locks the
/// shared state only to run a single opcode (pairing the local context with the guard via
/// [`Exec`]), then yields so siblings can run; **parks** when blocked/waiting (woken by
/// `make_runnable`'s `unpark`). Returns the thread's result — the program result for the main
/// thread (`idx == 0`), ignored for workers.
fn os_thread_loop(shared_arc: &Arc<Mutex<SharedVm>>, idx: usize) -> Option<Value> {
    // This OS thread's own execution context. Frames live in the shared slot between turns and
    // are `activate`d into here to run; the shared state is the only thing behind the lock.
    let mut running = RunningCtx::default();
    loop {
        let tick = {
            let mut guard = shared_arc.lock().unwrap();
            if guard.halt {
                // Main has finished — workers exit. When the halt came from `System.exit`,
                // main itself lands here too (a worker raised it), so hand back the status:
                // it is the program's result. A plain main-returned halt leaves it `None`.
                return guard.exit_status.map(Value::Int);
            }
            match guard.threads[idx].status {
                ThreadStatus::Terminated => return None,
                ThreadStatus::Blocked | ThreadStatus::Waiting | ThreadStatus::TimedWaiting => {
                    os_block_tick(&guard, idx)
                }
                ThreadStatus::Runnable => {
                    // Run one opcode with the local context bound to the shared guard, then drop
                    // the view so the guard is free for the scheduler bookkeeping below.
                    let step = {
                        let mut ex = Exec { running: &mut running, shared: &mut guard };
                        ex.activate(idx);
                        let s = ex.run_one();
                        ex.deactivate(idx);
                        s
                    };
                    spawn_pending(&mut guard, shared_arc); // OS threads for new Thread.start() slots
                    Exec { running: &mut running, shared: &mut guard }.wake_sleepers();
                    if let Step::Return(value) = step {
                        Exec { running: &mut running, shared: &mut guard }.on_thread_terminated(idx);
                        if idx == 0 {
                            guard.halt = true; // program done → release the workers
                            Exec { running: &mut running, shared: &mut guard }.unpark_all();
                        }
                        OsTick::Done(value)
                    } else {
                        os_block_tick(&guard, idx) // may have blocked us (monitor/wait/join/sleep)
                    }
                }
            }
        }; // lock released here

        match tick {
            OsTick::Done(value) => return value,
            OsTick::Yield => thread::yield_now(), // let a sibling grab the lock
            OsTick::Park => thread::park_timeout(Duration::from_millis(50)),
            OsTick::Sleep(ms) => {
                thread::sleep(Duration::from_millis(ms));
                let mut guard = shared_arc.lock().unwrap();
                // sleep done, or timed wait expired → re-acquire
                Exec { running: &mut running, shared: &mut guard }.expire_timed_block(idx);
            }
        }
    }
}

/// Classify a thread that didn't (or couldn't) run this turn: still runnable → yield;
/// sleeping → real sleep of the remaining ticks-as-millis (capped); otherwise park.
fn os_block_tick(shared: &SharedVm, idx: usize) -> OsTick {
    match shared.threads[idx].status {
        ThreadStatus::Runnable => OsTick::Yield,
        // The state *is* the answer now: only a deadline state (`sleep`/`wait(ms)`) sleeps
        // for its remaining ticks-as-millis. Before, the driver peeked at `sleep_until` to
        // guess timed-ness because `sleep` and `wait(ms)` both hid under `Blocked`/`Waiting`.
        ThreadStatus::TimedWaiting => match shared.threads[idx].sleep_until {
            Some(at) => OsTick::Sleep((at.saturating_sub(shared.steps)).min(200) as u64),
            None => OsTick::Park,
        },
        // An indefinite block — monitor contention or `wait()`/`join()` — parks until unparked.
        ThreadStatus::Blocked | ThreadStatus::Waiting => OsTick::Park,
        ThreadStatus::Terminated => OsTick::Park,
    }
}

/// Launch a real `std::thread` for every slot that doesn't have one yet (each
/// `Thread.start()` pushes a slot; this turns it into an OS thread exactly once). Runs
/// while the caller holds the GIL, so the handle is recorded before the child can run.
fn spawn_pending(shared: &mut SharedVm, shared_arc: &Arc<Mutex<SharedVm>>) {
    let pending: Vec<usize> = (0..shared.threads.len()).filter(|&i| !shared.threads[i].os_spawned).collect();
    for i in pending {
        shared.threads[i].os_spawned = true;
        let child = Arc::clone(shared_arc);
        let handle = thread::spawn(move || {
            os_thread_loop(&child, i);
        });
        // Keep the Thread handle for `unpark`; detach the JoinHandle (workers exit on halt).
        shared.threads[i].os_handle = Some(handle.thread().clone());
    }
}

// ===================== `os` parallel engine (H3 1d) =====================
// True parallelism: only `SharedVm` is locked, and the **frame-local** opcode subset runs
// lock-free on each thread's `RunningCtx` (+ code cache). The moving GC can't run while
// siblings execute lock-free, so it goes through a cooperative stop-the-world handshake
// (`coordinate_gc`): threads poll `gc_pending`, sync their frames to their slot, park; the
// coordinator collects once all are safe, then unparks them to reload their remapped frames.
// `os-gil` is untouched — it stays the serialised reference (and the fallback).

/// The outcome of one [`run_frame_local`] step.
enum FastStep {
    /// Ran a frame-local opcode; keep going lock-free.
    Continue,
    /// Ran a *backward* branch (a loop back-edge) — the caller polls the safepoint, then continues.
    BackEdge,
    /// The opcode is **not** in the frame-local subset — the caller runs it through the locked
    /// [`Exec::run_one`].
    NeedsShared,
}

/// Runs the opcode at the current frame's pc **iff** it is in the conservative frame-local
/// subset — pure int arithmetic / stack shuffles / int branches that touch only this thread's
/// own `RunningCtx` (stack, locals, pc, code cache), never shared state or object references.
/// Anything else returns [`FastStep::NeedsShared`]. Each arm mirrors the matching arm of
/// [`Exec::run_one`] exactly; the `os_parallel_matches_oracle` test guards that they agree.
///
/// **Safety of the classification:** misclassifying a shared opcode as frame-local would be a
/// bug (running it without the lock); misclassifying a frame-local opcode as shared merely runs
/// it under the lock (correct, just not parallel). So the set is deliberately *conservative*.
fn run_frame_local(ctx: &mut RunningCtx) -> FastStep {
    let pc = ctx.frames.last().expect("run_frame_local: no frame").pc();
    let op = ctx.code[pc];
    match op {
        // iconst_m1..iconst_5
        0x02..=0x08 => {
            let v = op as i32 - 0x03;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::iconst(f, v);
            f.advance(1);
            FastStep::Continue
        }
        // bipush / sipush
        0x10 => {
            let v = ctx.code[pc + 1] as i8 as i32;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::iconst(f, v);
            f.advance(2);
            FastStep::Continue
        }
        0x11 => {
            let v = i16::from_be_bytes([ctx.code[pc + 1], ctx.code[pc + 2]]) as i32;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::iconst(f, v);
            f.advance(3);
            FastStep::Continue
        }
        // iload_0..3 / iload
        0x1a..=0x1d => {
            let slot = (op - 0x1a) as usize;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::iload(f, slot);
            f.advance(1);
            FastStep::Continue
        }
        0x15 => {
            let slot = ctx.code[pc + 1] as usize;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::iload(f, slot);
            f.advance(2);
            FastStep::Continue
        }
        // istore_0..3 / istore
        0x3b..=0x3e => {
            let slot = (op - 0x3b) as usize;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::istore(f, slot);
            f.advance(1);
            FastStep::Continue
        }
        0x36 => {
            let slot = ctx.code[pc + 1] as usize;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::istore(f, slot);
            f.advance(2);
            FastStep::Continue
        }
        // iadd / isub / imul
        0x60 => {
            let f = ctx.frames.last_mut().unwrap();
            arithmetic_operations::iadd(f);
            f.advance(1);
            FastStep::Continue
        }
        0x64 => {
            let f = ctx.frames.last_mut().unwrap();
            arithmetic_operations::isub(f);
            f.advance(1);
            FastStep::Continue
        }
        0x68 => {
            let f = ctx.frames.last_mut().unwrap();
            arithmetic_operations::imul(f);
            f.advance(1);
            FastStep::Continue
        }
        // iinc
        0x84 => {
            let slot = ctx.code[pc + 1] as usize;
            let delta = ctx.code[pc + 2] as i8 as i32;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::iinc(f, slot, delta);
            f.advance(3);
            FastStep::Continue
        }
        // pop/pop2/dup*/swap — pure stack shuffles (value-agnostic, no deref)
        0x57..=0x5f => {
            let f = ctx.frames.last_mut().unwrap();
            match op {
                0x57 => stack_operations::pop(f),
                0x58 => stack_operations::pop2(f),
                0x59 => stack_operations::dup(f),
                0x5a => stack_operations::dup_x1(f),
                0x5b => stack_operations::dup_x2(f),
                0x5c => stack_operations::dup2(f),
                0x5d => stack_operations::dup2_x1(f),
                0x5e => stack_operations::dup2_x2(f),
                _ => stack_operations::swap(f),
            }
            f.advance(1);
            FastStep::Continue
        }
        // goto and the int comparison branches — the helper manages pc, so detect a *backward*
        // jump (a loop back-edge) to trigger a safepoint poll.
        0xa7 => branch(ctx, pc, bifurcation_operations::goto),
        0x9f => branch(ctx, pc, bifurcation_operations::if_icmpeq),
        0xa0 => branch(ctx, pc, bifurcation_operations::if_icmpne),
        0xa1 => branch(ctx, pc, bifurcation_operations::if_icmplt),
        0xa2 => branch(ctx, pc, bifurcation_operations::if_icmpge),
        0xa3 => branch(ctx, pc, bifurcation_operations::if_icmpgt),
        0xa4 => branch(ctx, pc, bifurcation_operations::if_icmple),
        0x99 => branch(ctx, pc, bifurcation_operations::ifeq),
        0x9a => branch(ctx, pc, bifurcation_operations::ifne),
        0x9b => branch(ctx, pc, bifurcation_operations::iflt),
        0x9c => branch(ctx, pc, bifurcation_operations::ifge),
        0x9d => branch(ctx, pc, bifurcation_operations::ifgt),
        0x9e => branch(ctx, pc, bifurcation_operations::ifle),

        // ---- W1: widen the frame-local set --------------------------------------------
        // aconst_null / lconst_0/1 — push a constant onto the stack.
        0x01 => {
            let f = ctx.frames.last_mut().unwrap();
            f.push(Value::Reference(0));
            f.advance(1);
            FastStep::Continue
        }
        0x09 | 0x0a => {
            let v = (op - 0x09) as i64;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::lconst(f, v);
            f.advance(1);
            FastStep::Continue
        }
        // aload_0..3 / aload — load a *reference* local (moves the offset value, no deref).
        0x2a..=0x2d => {
            let slot = (op - 0x2a) as usize;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::aload(f, slot);
            f.advance(1);
            FastStep::Continue
        }
        0x19 => {
            let slot = ctx.code[pc + 1] as usize;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::aload(f, slot);
            f.advance(2);
            FastStep::Continue
        }
        // astore_0..3 / astore — store a reference into a local.
        0x4b..=0x4e => {
            let slot = (op - 0x4b) as usize;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::astore(f, slot);
            f.advance(1);
            FastStep::Continue
        }
        0x3a => {
            let slot = ctx.code[pc + 1] as usize;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::astore(f, slot);
            f.advance(2);
            FastStep::Continue
        }
        // long / float / double add·sub·mul — none can throw.
        0x61 => arith1(ctx, arithmetic_operations::ladd),
        0x65 => arith1(ctx, arithmetic_operations::lsub),
        0x69 => arith1(ctx, arithmetic_operations::lmul),
        0x62 => arith1(ctx, arithmetic_operations::fadd),
        0x66 => arith1(ctx, arithmetic_operations::fsub),
        0x6a => arith1(ctx, arithmetic_operations::fmul),
        0x63 => arith1(ctx, arithmetic_operations::dadd),
        0x67 => arith1(ctx, arithmetic_operations::dsub),
        0x6b => arith1(ctx, arithmetic_operations::dmul),
        // float/double div & rem, negation, shifts, bitwise — none can throw. (Integer
        // idiv/irem/ldiv/lrem stay on the shared path: they may throw on /0.)
        0x6e | 0x6f | 0x72 | 0x73 | 0x74..=0x83 => {
            let f = ctx.frames.last_mut().unwrap();
            match op {
                0x6e => arithmetic_operations::fdiv(f),
                0x6f => arithmetic_operations::ddiv(f),
                0x72 => arithmetic_operations::frem(f),
                0x73 => arithmetic_operations::drem(f),
                0x74 => arithmetic_operations::ineg(f),
                0x75 => arithmetic_operations::lneg(f),
                0x76 => arithmetic_operations::fneg(f),
                0x77 => arithmetic_operations::dneg(f),
                0x78 => arithmetic_operations::ishl(f),
                0x79 => arithmetic_operations::lshl(f),
                0x7a => arithmetic_operations::ishr(f),
                0x7b => arithmetic_operations::lshr(f),
                0x7c => arithmetic_operations::iushr(f),
                0x7d => arithmetic_operations::lushr(f),
                0x7e => arithmetic_operations::iand(f),
                0x7f => arithmetic_operations::land(f),
                0x80 => arithmetic_operations::ior(f),
                0x81 => arithmetic_operations::lor(f),
                0x82 => arithmetic_operations::ixor(f),
                _ => arithmetic_operations::lxor(f), // 0x83
            }
            f.advance(1);
            FastStep::Continue
        }
        // numeric conversions i2l..i2s — pure stack transforms, none throw.
        0x85..=0x93 => {
            let f = ctx.frames.last_mut().unwrap();
            match op {
                0x85 => conversion_operations::i2l(f),
                0x86 => conversion_operations::i2f(f),
                0x87 => conversion_operations::i2d(f),
                0x88 => conversion_operations::l2i(f),
                0x89 => conversion_operations::l2f(f),
                0x8a => conversion_operations::l2d(f),
                0x8b => conversion_operations::f2i(f),
                0x8c => conversion_operations::f2l(f),
                0x8d => conversion_operations::f2d(f),
                0x8e => conversion_operations::d2i(f),
                0x8f => conversion_operations::d2l(f),
                0x90 => conversion_operations::d2f(f),
                0x91 => conversion_operations::i2b(f),
                0x92 => conversion_operations::i2c(f),
                _ => conversion_operations::i2s(f), // 0x93
            }
            f.advance(1);
            FastStep::Continue
        }
        // if_acmpeq / if_acmpne — reference-identity branches (compare two offsets).
        0xa5 => branch(ctx, pc, bifurcation_operations::if_acmpeq),
        0xa6 => branch(ctx, pc, bifurcation_operations::if_acmpne),

        // fconst_0/1/2 / dconst_0/1 — push a float/double constant.
        0x0b..=0x0d => {
            let v = (op - 0x0b) as f32;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::fconst(f, v);
            f.advance(1);
            FastStep::Continue
        }
        0x0e | 0x0f => {
            let v = (op - 0x0e) as f64;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::dconst(f, v);
            f.advance(1);
            FastStep::Continue
        }
        // Typed loads/stores for long/float/double — moving a `Value` is type-agnostic, so
        // these reuse `iload`/`istore` (as `run_one` does). `_n` forms: the slot is the low
        // nibble of `op - base`; indexed forms take the slot in the next byte.
        // lload_0..3 (0x1e-21) · fload_0..3 (0x22-25) · dload_0..3 (0x26-29)
        0x1e..=0x29 => {
            let slot = ((op - 0x1e) % 4) as usize;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::iload(f, slot);
            f.advance(1);
            FastStep::Continue
        }
        // lstore_0..3 (0x3f-42) · fstore_0..3 (0x43-46) · dstore_0..3 (0x47-4a)
        0x3f..=0x4a => {
            let slot = ((op - 0x3f) % 4) as usize;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::istore(f, slot);
            f.advance(1);
            FastStep::Continue
        }
        // lload / fload / dload (indexed)
        0x16..=0x18 => {
            let slot = ctx.code[pc + 1] as usize;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::iload(f, slot);
            f.advance(2);
            FastStep::Continue
        }
        // lstore / fstore / dstore (indexed)
        0x37..=0x39 => {
            let slot = ctx.code[pc + 1] as usize;
            let f = ctx.frames.last_mut().unwrap();
            variable_operations::istore(f, slot);
            f.advance(2);
            FastStep::Continue
        }

        _ => FastStep::NeedsShared,
    }
}

/// A no-operand, non-throwing arithmetic op in the fast path: apply the helper to the top
/// frame and advance one byte.
fn arith1(ctx: &mut RunningCtx, op: fn(&mut Frame)) -> FastStep {
    let f = ctx.frames.last_mut().unwrap();
    op(f);
    f.advance(1);
    FastStep::Continue
}

/// A 2-byte-offset branch in the frame-local fast path: read the offset, run the branch (it
/// sets pc itself), and report a back-edge if it jumped backward (a loop).
fn branch(ctx: &mut RunningCtx, pc: usize, bf: fn(&mut Frame, i16)) -> FastStep {
    let off = i16::from_be_bytes([ctx.code[pc + 1], ctx.code[pc + 2]]);
    let f = ctx.frames.last_mut().unwrap();
    bf(f, off);
    if f.pc() < pc {
        FastStep::BackEdge
    } else {
        FastStep::Continue
    }
}

/// **Read-only** shared opcodes (H3 W3): these run under a `SharedVm` *read* lock, so many
/// threads execute them concurrently. `Some(step)` = handled under the read lock; `None` =
/// escalate to the write path ([`Exec::run_one`]). Only ops that are side-effect-free on `None`
/// belong here — they must leave shared state untouched and, on escalation, the operand stack too.
fn run_read_shared(shared: &SharedVm, ctx: &mut RunningCtx) -> Option<Step> {
    let pc = ctx.frames.last()?.pc();
    let op = *ctx.code.get(pc)?;
    match op {
        // getfield — read an object's field. `getfield_read` escalates (`None`) on a null
        // receiver or an unloaded class, restoring the stack; otherwise it reads and pushes.
        0xb4 => {
            let cp_index = u16::from_be_bytes([ctx.code[pc + 1], ctx.code[pc + 2]]);
            let frame = ctx.frames.last_mut().unwrap();
            objects_operations::getfield_read(&shared.metaspace, &shared.heap, frame, cp_index)?;
            frame.advance(3);
            Some(Step::Continue)
        }
        // putfield — write an object's field **lock-free** when it's a primitive on an Eden object
        // (`Relaxed`, or `Release` if volatile). `putfield_read` escalates (`None`, restoring the
        // stack) for a reference store (write barrier), an Old object, an unloaded class, or null.
        0xb5 => {
            let cp_index = u16::from_be_bytes([ctx.code[pc + 1], ctx.code[pc + 2]]);
            let frame = ctx.frames.last_mut().unwrap();
            objects_operations::putfield_read(&shared.metaspace, &shared.heap, frame, cp_index)?;
            frame.advance(3);
            Some(Step::Continue)
        }
        // invokevirtual — only `AtomicInteger`/`AtomicLong.compareAndSet` is handled here, as a
        // **lock-free CAS** on an Eden field (H5 widened, like W3's reads). Every other virtual
        // call escalates to the write path; so does a CAS on an Old receiver (the locked native).
        0xb6 => {
            let cp_index = u16::from_be_bytes([ctx.code[pc + 1], ctx.code[pc + 2]]);
            let method = ctx.frames.last()?.method();
            let is_long = {
                let caller = shared.metaspace.class_of(method);
                let (class, name, _descriptor) =
                    shared.metaspace.get(caller)?.methodref_target(cp_index)?;
                match (class, name) {
                    ("java/util/concurrent/atomic/AtomicInteger", "compareAndSet") => false,
                    ("java/util/concurrent/atomic/AtomicLong", "compareAndSet") => true,
                    _ => return None, // any other virtual call → escalate
                }
            };
            let class = if is_long {
                "java/util/concurrent/atomic/AtomicLong"
            } else {
                "java/util/concurrent/atomic/AtomicInteger"
            };
            let frame = ctx.frames.last_mut().unwrap();
            objects_operations::atomic_cas_read(&shared.metaspace, &shared.heap, frame, class, is_long)?;
            frame.advance(3);
            Some(Step::Continue)
        }
        // arraylength — push the array's length. Escalates on a null array.
        0xbe => {
            let frame = ctx.frames.last_mut().unwrap();
            array_operations::arraylength_read(&shared.heap, frame)?;
            frame.advance(1);
            Some(Step::Continue)
        }
        // new (0xbb) — allocate an object **lock-free** in Eden, but only for an already
        // *initialized* class (an uninitialized one must run `<clinit>`, a write). Escalates
        // otherwise, or if the class isn't prepared / Eden is full.
        0xbb => {
            let cp_index = u16::from_be_bytes([ctx.code[pc + 1], ctx.code[pc + 2]]);
            let method = ctx.frames.last()?.method();
            let caller = shared.metaspace.class_of(method).to_string();
            let class_name = shared.metaspace.get(&caller).and_then(|cf| cf.class_name(cp_index))?.to_string();
            if shared.metaspace.init_state(&class_name) != InitState::Done {
                return None; // not initialized → escalate (write path runs `<clinit>`)
            }
            let offset =
                objects_operations::allocate_read(&shared.metaspace, &shared.heap, &class_name, ctx.current)?;
            let frame = ctx.frames.last_mut().unwrap();
            frame.push(Value::Reference(offset));
            frame.advance(3);
            Some(Step::Continue)
        }
        // newarray (0xbc) / anewarray (0xbd) — allocate an array lock-free. Escalate on a negative
        // length, an unprepared array class, or a full Eden (`*_read` restore the stack).
        0xbc => {
            let idx = ctx.current;
            let atype = ctx.code[pc + 1];
            let frame = ctx.frames.last_mut().unwrap();
            array_operations::newarray_read(&shared.metaspace, &shared.heap, frame, atype, idx)?;
            frame.advance(2);
            Some(Step::Continue)
        }
        0xbd => {
            let idx = ctx.current;
            let cp_index = u16::from_be_bytes([ctx.code[pc + 1], ctx.code[pc + 2]]);
            let frame = ctx.frames.last_mut().unwrap();
            array_operations::anewarray_read(&shared.metaspace, &shared.heap, frame, cp_index, idx)?;
            frame.advance(3);
            Some(Step::Continue)
        }
        // iaload..saload — read an array element. Escalate on null array / out-of-bounds.
        0x2e..=0x35 => {
            let frame = ctx.frames.last_mut().unwrap();
            let h = &shared.heap;
            match op {
                0x2e => array_operations::array_load_read(h, frame, 4, |h, at| Value::Int(h.read_u32(at) as i32)),
                0x2f => array_operations::array_load_read(h, frame, 8, |h, at| Value::Long(h.read_u64(at) as i64)),
                0x30 => array_operations::array_load_read(h, frame, 4, |h, at| Value::Float(f32::from_bits(h.read_u32(at)))),
                0x31 => array_operations::array_load_read(h, frame, 8, |h, at| Value::Double(f64::from_bits(h.read_u64(at)))),
                0x32 => array_operations::array_load_read(h, frame, 4, |h, at| Value::Reference(h.read_u32(at) as usize)),
                0x33 => array_operations::array_load_read(h, frame, 1, |h, at| Value::Int(h.read_u8(at) as i8 as i32)),
                0x34 => array_operations::array_load_read(h, frame, 2, |h, at| Value::Int(h.read_u16(at) as i32)),
                _ => array_operations::array_load_read(h, frame, 2, |h, at| Value::Int(h.read_u16(at) as i16 as i32)), // 0x35 saload
            }?;
            frame.advance(1);
            Some(Step::Continue)
        }
        _ => None,
    }
}

/// `os` parallel substrate entry: set up the shared state (main's frame in slot 0), flip
/// `gc_by_driver` so `safepoint` defers to us, and run the parallel loop on this OS thread.
pub(crate) fn run_os_parallel(metaspace: MetaspaceService, entry: Frame) -> Option<Value> {
    let mut jvm = JVM::new(metaspace, entry);
    jvm.shared.mode = ThreadMode::OsParallel;
    jvm.shared.gc_by_driver = true; // GC goes through `coordinate_gc`, not inline `safepoint`
    jvm.shared.threads[0].os_handle = Some(thread::current());
    std::mem::swap(&mut jvm.running.frames, &mut jvm.shared.threads[0].frames);
    let shared = Arc::new(RwLock::new(jvm.shared));
    let gc_pending = Arc::new(AtomicBool::new(false));
    os_parallel_loop(&shared, &gc_pending, 0)
}

/// One thread's parallel run loop. Runs frame-local opcodes lock-free on its own `RunningCtx`;
/// takes the `SharedVm` lock only for shared opcodes and scheduling; and cooperates with the GC
/// stop-the-world via `gc_pending` + [`reach_safepoint`].
fn os_parallel_loop(
    shared_arc: &Arc<RwLock<SharedVm>>,
    gc_pending: &Arc<AtomicBool>,
    idx: usize,
) -> Option<Value> {
    let mut running = RunningCtx::default();
    // Load this thread's frames from its slot and fill the code cache (once, under the lock).
    {
        let mut g = shared_arc.write().unwrap();
        Exec { running: &mut running, shared: &mut g }.activate(idx);
        if !running.frames.is_empty() {
            Exec { running: &mut running, shared: &mut g }.sync_code_cache();
        }
    }
    loop {
        // Safepoint poll (lock-free): if a GC is pending, sync + park until it's done.
        if gc_pending.load(Ordering::Acquire) {
            reach_safepoint(shared_arc, gc_pending, &mut running, idx);
        }

        // Fast path: run the top opcode lock-free if it's frame-local.
        if !running.frames.is_empty() {
            match run_frame_local(&mut running) {
                FastStep::Continue | FastStep::BackEdge => continue,
                FastStep::NeedsShared => {}
            }
        }

        // W3 read path: a read-only shared opcode (e.g. `getfield`) runs under a **read** lock,
        // so sibling threads doing the same run concurrently. `None` → escalate to the write path.
        if !running.frames.is_empty() {
            let handled = run_read_shared(&shared_arc.read().unwrap(), &mut running);
            if handled.is_some() {
                continue;
            }
        }

        // Shared (write) path: one opcode / scheduling step under the write lock.
        let tick: OsTick = {
            let mut g = shared_arc.write().unwrap();
            if g.halt {
                return g.exit_status.map(Value::Int); // `System.exit` status, else a plain halt
            }
            match g.threads[idx].status {
                ThreadStatus::Terminated => return None,
                ThreadStatus::Blocked | ThreadStatus::Waiting | ThreadStatus::TimedWaiting => {
                    os_block_tick(&g, idx)
                }
                ThreadStatus::Runnable => {
                    // Reload our frames if a previous block deactivated them into the slot.
                    if running.frames.is_empty() {
                        std::mem::swap(&mut running.frames, &mut g.threads[idx].frames);
                        Exec { running: &mut running, shared: &mut g }.sync_code_cache();
                    }
                    let step = Exec { running: &mut running, shared: &mut g }.run_one();
                    spawn_pending_parallel(&mut g, shared_arc, gc_pending);
                    Exec { running: &mut running, shared: &mut g }.wake_sleepers();
                    // The op may have pushed/popped a frame (invoke/return) — re-sync the code
                    // cache to the new top method so the lock-free fast path reads the right
                    // bytecode next iteration. (Still under the lock, so `metaspace` is safe.)
                    if !running.frames.is_empty() {
                        Exec { running: &mut running, shared: &mut g }.sync_code_cache();
                    }
                    if let Step::Return(value) = step {
                        Exec { running: &mut running, shared: &mut g }.on_thread_terminated(idx);
                        if idx == 0 {
                            g.halt = true;
                            Exec { running: &mut running, shared: &mut g }.unpark_all();
                        }
                        return value;
                    }
                    if g.threads[idx].status == ThreadStatus::Runnable {
                        // Still running: GC due? Become the coordinator (frames stay local so
                        // `collect_at_safepoint`'s `parked()` can sync them). Else yield.
                        if (Exec { running: &mut running, shared: &mut g }).needs_collection() {
                            drop(g);
                            coordinate_gc(shared_arc, gc_pending, &mut running, idx);
                            continue;
                        }
                        OsTick::Yield
                    } else {
                        // Blocked/waiting/sleeping: sync our frames into the slot so a GC sees
                        // them while we're parked, then classify how to wait.
                        std::mem::swap(&mut running.frames, &mut g.threads[idx].frames);
                        os_block_tick(&g, idx)
                    }
                }
            }
        };

        match tick {
            OsTick::Done(value) => return value,
            OsTick::Yield => thread::yield_now(),
            OsTick::Park => thread::park_timeout(Duration::from_millis(50)),
            OsTick::Sleep(ms) => {
                thread::sleep(Duration::from_millis(ms));
                let mut g = shared_arc.write().unwrap();
                Exec { running: &mut running, shared: &mut g }.expire_timed_block(idx);
            }
        }
    }
}

/// Reach a GC safepoint: sync our frames into our slot (so the collector can walk and remap
/// them), mark ourselves safe, park until the coordinator clears `gc_pending`, then reload the
/// (possibly remapped) frames.
///
/// The first swap is **guarded** by `had_local_frames`: only deactivate into the slot when our
/// frames are actually local. A thread that entered here already deactivated (it blocked in
/// `join`/`wait`, so its frames are already in its slot and `running.frames` is empty) must LEAVE
/// them in the slot — an unconditional swap would pull them back out, empty the slot, and hide our
/// roots from the very collect we are stopping for (the collector scans slots, not our local
/// `running`). Symmetrically we only reload what we swapped out; if we never swapped, the frames
/// stay in the slot and the write-path reload (`if running.frames.is_empty()`) picks them up when
/// the thread next becomes `Runnable`.
fn reach_safepoint(
    shared_arc: &Arc<RwLock<SharedVm>>,
    gc_pending: &Arc<AtomicBool>,
    running: &mut RunningCtx,
    idx: usize,
) {
    let had_local_frames;
    {
        let mut g = shared_arc.write().unwrap();
        had_local_frames = !running.frames.is_empty();
        if had_local_frames {
            std::mem::swap(&mut running.frames, &mut g.threads[idx].frames);
        }
        g.threads[idx].at_safepoint = true;
    }
    while gc_pending.load(Ordering::Acquire) {
        thread::park();
    }
    let mut g = shared_arc.write().unwrap();
    if had_local_frames {
        std::mem::swap(&mut running.frames, &mut g.threads[idx].frames);
    }
    g.threads[idx].at_safepoint = false;
}

/// Stop-the-world coordinator: exactly one thread runs this per collection (won via
/// `compare_exchange` on `gc_pending`). It waits until every other thread is *safe*
/// (`status != Runnable`, i.e. blocked with frames in its slot, or `at_safepoint`), collects,
/// then clears the flag and unparks the safepointed threads to reload their remapped frames.
fn coordinate_gc(
    shared_arc: &Arc<RwLock<SharedVm>>,
    gc_pending: &Arc<AtomicBool>,
    running: &mut RunningCtx,
    idx: usize,
) {
    if gc_pending
        .compare_exchange(false, true, Ordering::AcqRel, Ordering::Acquire)
        .is_err()
    {
        // Another thread is already coordinating — reach the safepoint ourselves instead.
        reach_safepoint(shared_arc, gc_pending, running, idx);
        return;
    }
    loop {
        {
            let mut g = shared_arc.write().unwrap();
            let all_safe = g.threads.iter().enumerate().all(|(i, t)| {
                i == idx || t.status != ThreadStatus::Runnable || t.at_safepoint
            });
            if all_safe {
                // Everyone stopped and synced. Collect — our own frames are handled by
                // `parked()` inside; the others' are already in their slots.
                Exec { running: &mut *running, shared: &mut g }.collect_at_safepoint();
                gc_pending.store(false, Ordering::Release);
                for t in &g.threads {
                    if let Some(h) = &t.os_handle {
                        h.unpark();
                    }
                }
                return;
            }
        }
        thread::yield_now(); // let the stragglers reach their safepoint
    }
}

/// Like [`spawn_pending`] but launches the **parallel** loop for each new `Thread.start()` slot.
fn spawn_pending_parallel(
    shared: &mut SharedVm,
    shared_arc: &Arc<RwLock<SharedVm>>,
    gc_pending: &Arc<AtomicBool>,
) {
    let pending: Vec<usize> =
        (0..shared.threads.len()).filter(|&i| !shared.threads[i].os_spawned).collect();
    for i in pending {
        shared.threads[i].os_spawned = true;
        let child_shared = Arc::clone(shared_arc);
        let child_gc = Arc::clone(gc_pending);
        let handle = thread::spawn(move || {
            os_parallel_loop(&child_shared, &child_gc, i);
        });
        shared.threads[i].os_handle = Some(handle.thread().clone());
    }
}
