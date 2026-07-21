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
use std::sync::{Arc, Mutex};
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

/// The four invoke opcodes — one module each. Unlike the per-family helpers above
/// (which act on a single `&mut Frame`), these drive the whole call stack, so each
/// contributes an `impl JVM` method that `step()` dispatches to.
mod invokeinterface;
mod invokespecial;
mod invokestatic;
mod invokevirtual;

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

/// A thread's scheduling state. The scheduler only runs `Runnable` threads. `Blocked` =
/// waiting to acquire a contended monitor; `Waiting` = parked in `wait()` until notified.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum ThreadStatus {
    Runnable,
    Blocked,
    Waiting,
    Terminated,
}

/// The execution **substrate** for Java threads — an application parameter
/// (`JVM_THREADS`), read once at VM startup like the `JVM_GC_*` knobs.
///
/// - `Green` (default): the cooperative scheduler on a single OS thread (`step`
///   round-robins at opcode granularity). Deterministic and single-steppable — what
///   the `jvm-step` visualizer needs.
/// - `Os`: each `java.lang.Thread` runs on a real `std::thread`, with a **GIL** (one
///   global interpreter lock) serializing opcode execution. Correct but not yet
///   parallel — removing the GIL is the next milestone. Blocking is real `park`/`unpark`.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ThreadMode {
    Green,
    Os,
}

impl ThreadMode {
    /// Reads `JVM_THREADS` (`os` → real OS threads + GIL; anything else → green).
    /// Defaults to `Green` while the OS substrate stabilises.
    pub fn from_env() -> Self {
        match std::env::var("JVM_THREADS").ok().as_deref().map(str::trim) {
            Some("os") | Some("OS") | Some("Os") => ThreadMode::Os,
            _ => ThreadMode::Green,
        }
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
    /// **OS mode only.** Handle to this thread's `std::thread`, so other threads can
    /// `unpark` it when they make it runnable (monitor release, notify, join-wake,
    /// sleeper-wake). `None` in green mode and for the main thread (driven directly).
    pub os_handle: Option<thread::Thread>,
    /// **OS mode only.** Whether a real OS thread has already been launched for this
    /// slot — the driver spawns one per new `Thread.start()` slot exactly once.
    pub os_spawned: bool,
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
pub struct JVM {
    metaspace: MetaspaceService,
    /// The **currently running** thread's call stack (its slot in `threads` is empty
    /// while it runs; they swap on a context switch). Keeping the active stack here
    /// means every opcode handler touches `self.frames` exactly as before — threading
    /// only adds the scheduler around it.
    frames: Vec<Frame>,
    /// All green threads. `threads[current]` is the running one (its `frames` empty —
    /// the live stack is in `self.frames`); the rest are parked with their stacks.
    threads: Vec<GreenThread>,
    /// Index of the running thread in `threads` (thread 0 is the entry/`main` thread).
    current: usize,
    /// Monotonic id for the next spawned thread.
    next_thread_id: usize,
    /// Object monitors for `synchronized`, keyed by the lock object's heap offset.
    /// Created lazily on first `monitorenter`.
    monitors: std::collections::HashMap<usize, Monitor>,
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
    /// Threading substrate (green vs OS+GIL), read once from `JVM_THREADS` at startup.
    mode: ThreadMode,
    /// **OS mode only.** Raised when the main thread returns: worker OS threads see it
    /// at the top of their loop and exit (mirrors the green scheduler abandoning workers
    /// when `main` ends).
    halt: bool,
}

impl JVM {
    /// Starts an interpreter whose call stack holds just the `entry` frame, run
    /// against `metaspace` (which it takes ownership of, to resolve calls), with a
    /// fresh empty heap.
    pub fn new(metaspace: MetaspaceService, entry: Frame) -> Self {
        JVM {
            metaspace,
            // The entry method runs on thread 0 (`main`); its stack is the active one,
            // so thread 0's own `frames` slot starts empty.
            frames: vec![entry],
            threads: vec![GreenThread {
                id: 0,
                status: ThreadStatus::Runnable,
                frames: Vec::new(),
                thread_obj: 0, // the entry/main thread has no `Thread` object
                wait_reacquire: None,
                joining_on: None,
                sleep_until: None,
                os_handle: None,
                os_spawned: true, // the main thread is driven by execute_os' own thread
            }],
            current: 0,
            next_thread_id: 1,
            monitors: std::collections::HashMap::new(),
            heap: HeapService::new(),
            console: String::new(),
            gc_policy: gc::GcPolicy::from_env(),
            steps: 0,
            last_gc_step: 0,
            last_gc_used: 0,
            gc_requested: false,
            mode: ThreadMode::from_env(),
            halt: false,
        }
    }

    /// What the program has printed so far (via native methods), for tooling.
    pub fn console(&self) -> &str {
        &self.console
    }

    /// The heap, for tooling that wants to show its contents.
    pub fn heap(&self) -> &HeapService {
        &self.heap
    }

    /// Runs `f` with **every** thread stack visible to it. The GC's roots span all
    /// threads, but the running thread's stack lives in `self.frames`; this parks it
    /// into its slot so the whole set is in `self.threads`, runs `f` over them, then
    /// re-activates. Every GC entry point goes through here.
    fn parked<R>(&mut self, f: impl FnOnce(&MetaspaceService, &mut HeapService, &mut [GreenThread]) -> R) -> R {
        std::mem::swap(&mut self.frames, &mut self.threads[self.current].frames);
        let result = f(&self.metaspace, &mut self.heap, &mut self.threads);
        std::mem::swap(&mut self.frames, &mut self.threads[self.current].frames);
        result
    }

    /// Runs a GC **mark** phase over the live state (frames + mirrors) and returns
    /// what came out live vs garbage. Mark-only for now — nothing is freed; the
    /// visualizer triggers this (on `espacio`) to *show* reachability.
    pub fn gc_mark(&mut self) -> gc::MarkReport {
        self.parked(|m, h, t| gc::mark(m, h, t))
    }

    /// Runs a full GC cycle — mark **and sweep**: reclaims every unreachable object
    /// into the heap's free list. Returns the report (its `garbage` = what was freed).
    pub fn gc_sweep(&mut self) -> gc::MarkReport {
        self.parked(|m, h, t| gc::sweep(m, h, t))
    }

    /// The GC compaction policy in effect (read from the environment at startup),
    /// for tooling that evaluates the fragmentation rule.
    pub fn gc_policy(&self) -> &gc::GcPolicy {
        &self.gc_policy
    }

    /// Runs a **mark-compact**: relocates live objects into one contiguous run and
    /// fixes the references to them. Returns what moved / how much was reclaimed.
    pub fn gc_compact(&mut self) -> gc::CompactReport {
        let report = self.parked(|m, h, t| gc::compact(m, h, t));
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
        let monitors = std::mem::take(&mut self.monitors);
        self.monitors = monitors
            .into_iter()
            .map(|(obj, mon)| (forward.get(&obj).copied().unwrap_or(obj), mon))
            .collect();
    }

    /// Drop monitors whose lock object is no longer allocated (it was collected), so a
    /// later allocation reusing that offset can't inherit a stale monitor.
    fn prune_dead_monitors(&mut self) {
        let live: std::collections::HashSet<usize> =
            self.heap.allocations().iter().map(|a| a.offset).collect();
        self.monitors.retain(|obj, _| live.contains(obj));
    }

    /// Runs a **minor** collection: the young generation's copying collector — evacuate
    /// Eden's survivors to a survivor space (or promote them), recycle Eden. Cheap and
    /// frequent; the visualizer can trigger it, and the safepoint runs it when Eden fills.
    pub fn gc_minor(&mut self) -> gc::MinorReport {
        let tenure = self.gc_policy.tenure;
        let report = self.parked(|m, h, t| gc::minor(m, h, t, tenure));
        self.remap_monitor_keys(&report.relocations);
        self.prune_dead_monitors();
        report
    }

    /// Flags an explicit collection request (`System.gc()`), serviced at the next
    /// safepoint — not run inline, exactly like the real VM defers it.
    pub fn request_gc(&mut self) {
        self.gc_requested = true;
    }

    /// A **safepoint**: the point between opcodes where the VM is allowed to collect.
    /// Polls the triggers — an explicit `System.gc()` first, then the automatic
    /// causes (out-of-space / occupancy / allocation-rate) — and runs a cycle if one
    /// fires. This is the single place "when does the GC run" is decided.
    fn safepoint(&mut self) {
        // Young generation first: a (near-)full Eden triggers a cheap minor collection.
        // Always on — the copying collector is correct over any program state (the
        // gate the old `JVM_GC_AUTO` guarded was about an incomplete mark, long fixed).
        if self.heap.eden_used() * 10 >= self.heap.eden_capacity() * 9 {
            let tenure = self.gc_policy.tenure;
            let report = self.parked(|m, h, t| gc::minor(m, h, t, tenure));
            self.remap_monitor_keys(&report.relocations); // young objects moved → fix monitor keys
            self.prune_dead_monitors();
            let _ = writeln!(
                self.console,
                "[gc] minor: {} copiados, {} promovidos · recuperó {}B",
                report.copied, report.promoted, report.reclaimed
            );
        }

        // Then the major (Old) triggers: explicit `System.gc()` or the automatic causes.
        let cause = if self.gc_requested {
            Some(gc::GcCause::Explicit)
        } else {
            self.gc_policy.auto_cause(
                self.heap.used(),
                self.steps,
                self.last_gc_used,
                self.last_gc_step,
            )
        };
        if let Some(cause) = cause {
            self.collect(cause);
        }
    }

    /// Runs one collection cycle: mark-and-sweep, then compact if the heap is
    /// fragmented enough ([`gc::should_compact`]). Resets the trigger baselines and
    /// logs a line (visible in the visualizer's output panel).
    fn collect(&mut self, cause: gc::GcCause) {
        let before = self.heap.used();
        let policy = self.gc_policy;
        // A full collection is generational: a minor first (evacuate/promote the young),
        // then the major over Old (sweep, and compact if fragmented). All over every
        // thread's roots, so it runs inside `parked`.
        let (live, garbage, compacted, minor_reloc, compact_reloc) = self.parked(|m, h, t| {
            let minor = gc::minor(m, h, t, policy.tenure);
            let report = gc::sweep(m, h, t);
            let (compacted, compact_reloc) = if gc::should_compact(h, &policy) {
                let c = gc::compact(m, h, t);
                (c.reclaimed, c.relocations)
            } else {
                (0, std::collections::HashMap::new())
            };
            (report.live.len(), report.garbage.len(), compacted, minor.relocations, compact_reloc)
        });
        // Objects moved (minor evacuation, then compaction) → relocate the monitor map keys,
        // applied minor-then-compact (the composition), then drop monitors on dead objects.
        self.remap_monitor_keys(&minor_reloc);
        self.remap_monitor_keys(&compact_reloc);
        self.prune_dead_monitors();
        let after = self.heap.used();

        self.gc_requested = false;
        self.last_gc_used = after;
        self.last_gc_step = self.steps;

        let _ = writeln!(
            self.console,
            "[gc] {cause:?}: {live} vivos, {garbage} basura · used {before}B → {after}B{}",
            if compacted > 0 { format!(" (compactó {compacted}B)") } else { String::new() },
        );
    }

    /// The mirror index as `(Class ID, class name, offset)` rows, for a visualizer
    /// labelling the heap with which class's mirror sits at which offset.
    pub fn class_objects(&self) -> Vec<(&str, &str, usize)> {
        self.metaspace.class_object_offsets()
    }

    /// The current (top) frame — read by the visualizer to show the live state.
    pub fn frame(&self) -> &Frame {
        self.frames.last().expect("no frame on the call stack")
    }

    /// The current frame's program counter.
    pub fn pc(&self) -> usize {
        self.frame().pc()
    }

    /// How deep the call stack is (1 = just the entry method).
    pub fn depth(&self) -> usize {
        self.frames.len()
    }

    /// The bytecode of the current (top) frame, resolved through the metaspace.
    /// The visualizer disassembles this to draw the instruction window.
    pub fn current_code(&self) -> &[u8] {
        self.metaspace.code(self.frame().method())
    }

    /// The whole call stack (bottom → top), for a visualizer that shows several
    /// frames at once.
    pub fn frames(&self) -> &[Frame] {
        &self.frames
    }

    /// A snapshot of every green thread (id, state, current method, which is running) —
    /// so the visualizer can show the cooperative scheduling. The running thread's live
    /// stack is in `self.frames`; parked threads keep theirs in their slot.
    pub fn thread_views(&self) -> Vec<ThreadView> {
        self.threads
            .iter()
            .enumerate()
            .map(|(i, t)| {
                let stack = if i == self.current { &self.frames } else { &t.frames };
                let method = stack
                    .last()
                    .map(|f| self.metaspace.name(f.method()).to_string())
                    .unwrap_or_else(|| "—".to_string());
                ThreadView {
                    id: t.id,
                    status: match t.status {
                        ThreadStatus::Runnable => "runnable",
                        ThreadStatus::Blocked => "blocked",
                        ThreadStatus::Waiting => "waiting",
                        ThreadStatus::Terminated => "terminated",
                    },
                    method,
                    current: i == self.current,
                }
            })
            .collect()
    }

    /// The bytecode of an arbitrary frame (not just the top), via the metaspace.
    pub fn code_of(&self, frame: &Frame) -> &[u8] {
        self.metaspace.code(frame.method())
    }

    /// A frame's method name, for labelling its panel.
    pub fn method_name_of(&self, frame: &Frame) -> &str {
        self.metaspace.name(frame.method())
    }

    /// Mutable access to the top frame, for the opcode helpers.
    fn top(&mut self) -> &mut Frame {
        self.frames.last_mut().expect("no frame on the call stack")
    }

    /// Reads the signed 2-byte branch offset that follows the current opcode.
    fn branch_offset(&self) -> i16 {
        let frame = self.frame();
        let code = self.metaspace.code(frame.method());
        let pc = frame.pc();
        i16::from_be_bytes([code[pc + 1], code[pc + 2]])
    }

    /// Reads the signed **4-byte** branch offset that follows a wide branch
    /// (`goto_w`, 0xc8) — the same role as [`branch_offset`](Self::branch_offset),
    /// with the range the 16-bit form can't express.
    fn wide_branch_offset(&self) -> i32 {
        let frame = self.frame();
        let code = self.metaspace.code(frame.method());
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
            // The current thread's last frame returned.
            if self.current == 0 {
                return Step::Return(value); // the main thread finished → program result
            }
            let finished = self.current;
            // Mark terminated and wake anyone blocked in `join` on it.
            self.on_thread_terminated(finished);
        }
        self.wake_sleepers();
        // Cooperative context switch: pick the next runnable thread.
        match self.next_runnable() {
            Some(next) if next != self.current => {
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
        let n = self.threads.len();
        // Offsets 1..=n cover every thread, ending at `current` itself — so others are
        // preferred (fairness) and the current thread is the last resort.
        (1..=n)
            .map(|off| (self.current + off) % n)
            .find(|&i| self.threads[i].status == ThreadStatus::Runnable)
    }

    /// Context switch: park the running thread's stack into its slot and load the
    /// target thread's stack into the active `frames` (a pair of swaps — the active
    /// stack always lives in `self.frames`).
    fn switch_to(&mut self, next: usize) {
        std::mem::swap(&mut self.frames, &mut self.threads[self.current].frames);
        self.current = next;
        std::mem::swap(&mut self.frames, &mut self.threads[self.current].frames);
    }

    /// Mark thread `idx` runnable — the single "wake" primitive. In OS mode it also
    /// `unpark`s the thread's `std::thread` (it may be parked waiting for exactly this);
    /// in green mode the round-robin scheduler will simply pick it up. Every place that
    /// transitions a thread *to* `Runnable` goes through here so the unpark can't be missed.
    fn make_runnable(&mut self, idx: usize) {
        self.threads[idx].status = ThreadStatus::Runnable;
        if self.mode == ThreadMode::Os {
            if let Some(handle) = &self.threads[idx].os_handle {
                handle.unpark();
            }
        }
    }

    /// **OS mode.** Load thread `idx` as the running one: set `current` and swap its saved
    /// stack into the active `self.frames` (the inverse of [`Self::deactivate`]). Every
    /// opcode handler then touches `self.frames`/`self.current` exactly as in green mode.
    fn activate(&mut self, idx: usize) {
        self.current = idx;
        std::mem::swap(&mut self.frames, &mut self.threads[idx].frames);
    }

    /// **OS mode.** Park thread `idx`'s stack back into its slot after running an opcode,
    /// so the slot holds the full stack between turns (and the GC, via `parked`, can walk it).
    fn deactivate(&mut self, idx: usize) {
        std::mem::swap(&mut self.frames, &mut self.threads[idx].frames);
    }

    /// Mark thread `idx` terminated and wake anyone blocked in `join` on it. Shared by the
    /// green scheduler ([`Self::step`]) and the OS driver loop.
    fn on_thread_terminated(&mut self, idx: usize) {
        self.threads[idx].status = ThreadStatus::Terminated;
        let joiners: Vec<usize> = (0..self.threads.len())
            .filter(|&i| self.threads[i].joining_on == Some(idx))
            .collect();
        for w in joiners {
            self.threads[w].joining_on = None;
            self.make_runnable(w);
        }
    }

    /// **OS mode.** Unpark every thread with a live OS handle — used on `halt` so parked
    /// workers wake, see the halt flag, and exit instead of leaking.
    fn unpark_all(&self) {
        for t in &self.threads {
            if let Some(handle) = &t.os_handle {
                handle.unpark();
            }
        }
    }

    /// Spawns a green thread for `Thread.start()`: it runs the receiver's `run()`
    /// (virtual dispatch on the receiver's class), parked `Runnable` until the scheduler
    /// picks it. `start()` itself returns immediately to the caller.
    fn spawn_thread(&mut self, receiver: usize) {
        let runtime_class = self
            .metaspace
            .class_name_at_mirror(self.heap.read_u32(receiver) as usize)
            .expect("Thread.start: receiver has no class")
            .to_string();
        let slot = self
            .metaspace
            .vtable_slot("java/lang/Thread", "run", "()V")
            .expect("Thread.run vtable slot");
        let run = self.metaspace.vtable_method(&runtime_class, slot).expect("run() method");
        let max_locals = self.metaspace.max_locals(run);
        let frame = Frame::for_call(run, max_locals, vec![Value::Reference(receiver)], &[1]);
        let id = self.next_thread_id;
        self.next_thread_id += 1;
        self.threads.push(GreenThread {
            id,
            status: ThreadStatus::Runnable,
            frames: vec![frame],
            thread_obj: receiver,
            wait_reacquire: None,
            joining_on: None,
            sleep_until: None,
            os_handle: None,
            os_spawned: false, // the OS driver launches this slot's std::thread on the next tick
        });
    }

    /// Core monitor **acquire**, shared by the `monitorenter` opcode and synchronized-method
    /// entry. Tries to make the current thread own `obj`'s monitor: succeeds (returning
    /// `true`) if the monitor is free or already this thread's (reentrant — just bumps the
    /// count); otherwise parks the thread in the monitor's blocked-set, marks it `Blocked`,
    /// and returns `false`. The *caller* decides what "blocked" means for its opcode (the
    /// pc rewind / operand restore that makes the operation retry when rescheduled).
    fn acquire_monitor(&mut self, obj: usize) -> bool {
        let current = self.current;
        let acquired = {
            let mon = self.monitors.entry(obj).or_default();
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
            self.threads[current].status = ThreadStatus::Blocked;
        }
        acquired
    }

    /// Core monitor **release**, shared by the `monitorexit` opcode and synchronized-method
    /// exit. Drops one level of the current thread's ownership of `obj`'s monitor (reentrant
    /// — the monitor frees only at count 0); on freeing it, wakes one blocked contender so it
    /// can retry its acquire. A no-op if the current thread doesn't actually own it.
    fn release_monitor(&mut self, obj: usize) {
        let current = self.current;
        let wake = {
            let mon = self.monitors.entry(obj).or_default();
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
        self.monitors
            .get(&obj)
            .map_or(false, |m| m.owner == Some(self.current) && m.count > 0)
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
        self.frames.push(frame);
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
        let popped = self.frames.pop();
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
        let current = self.current;
        let (saved, wake) = {
            let mon = self.monitors.entry(obj).or_default();
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
        self.threads[current].status = ThreadStatus::Waiting;
        self.threads[current].wait_reacquire = Some((obj, saved));
        // Timed `wait(ms)`: a deadline (opcode-ticks in green; real time in OS mode) after
        // which the wait returns even without a `notify`. `expire_timed_block` then pulls
        // the thread out of the wait-set so the re-acquire path resumes it (a self-notify).
        // `wait(0)` / `wait()` is an indefinite wait (no deadline).
        if let Some(ms) = timeout {
            if ms > 0 {
                self.threads[current].sleep_until = Some(self.steps + ms as usize);
            }
        }
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
            let mon = self.monitors.entry(obj).or_default();
            if all {
                mon.waiting.drain(..).collect()
            } else if mon.waiting.is_empty() {
                Vec::new()
            } else {
                vec![mon.waiting.remove(0)]
            }
        };
        for idx in woken {
            self.monitors.entry(obj).or_default().blocked.push(idx);
            self.threads[idx].status = ThreadStatus::Blocked;
        }
        self.advance_past_call();
        Step::Continue
    }

    /// `Thread.join()`: block the current thread until `target_obj`'s green thread
    /// terminates (woken in `step` when that thread ends). If it already finished — or
    /// was never `start`ed — `join` returns at once.
    fn thread_join(&mut self, target_obj: usize) -> Step {
        self.advance_past_call(); // resume after join() (now, or once the target ends)
        let target = self.threads.iter().position(|t| t.thread_obj == target_obj && target_obj != 0);
        if let Some(idx) = target {
            if self.threads[idx].status != ThreadStatus::Terminated {
                let current = self.current;
                self.threads[current].status = ThreadStatus::Blocked;
                self.threads[current].joining_on = Some(idx);
            }
        }
        Step::Continue
    }

    /// `Thread.sleep(ms)`: park the current thread until `ms` opcode-ticks pass (our
    /// clock is the opcode count — there's no wall clock). Other threads run meanwhile;
    /// the sleeper is woken in `step` once the clock reaches its wake time.
    fn thread_sleep(&mut self, ms: i64) -> Step {
        let current = self.current;
        self.threads[current].status = ThreadStatus::Blocked;
        self.threads[current].sleep_until = Some(self.steps + ms.max(0) as usize);
        self.advance_past_call();
        Step::Continue
    }

    /// A timed block (`Thread.sleep` or `Object.wait(ms)`) reached its deadline: clear it
    /// and make the thread runnable. For a timed `wait`, also pull the thread out of its
    /// monitor's wait-set so the re-acquire path resumes it — the deadline acting as a
    /// self-`notify`. (A plain `sleep` has no monitor and just becomes runnable.)
    fn expire_timed_block(&mut self, idx: usize) {
        self.threads[idx].sleep_until = None;
        if self.threads[idx].status == ThreadStatus::Waiting {
            if let Some((obj, _)) = self.threads[idx].wait_reacquire {
                if let Some(mon) = self.monitors.get_mut(&obj) {
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
        let any_runnable = self.threads.iter().any(|t| t.status == ThreadStatus::Runnable);
        if !any_runnable {
            if let Some(earliest) =
                self.threads.iter().filter_map(|t| t.sleep_until).min()
            {
                self.steps = self.steps.max(earliest);
            }
        }
        let now = self.steps;
        let due: Vec<usize> = self
            .threads
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
        self.steps += 1;
        self.safepoint();

        // A thread returning from `wait()` (notified, now scheduled) must re-acquire its
        // monitor before running the instruction after the `wait()` call. If it can't
        // yet, it blocks and retries — exactly like `monitorenter`.
        if let Some((obj, saved)) = self.threads[self.current].wait_reacquire {
            let current = self.current;
            let acquired = {
                let mon = self.monitors.entry(obj).or_default();
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
                self.threads[current].wait_reacquire = None;
            } else {
                self.threads[current].status = ThreadStatus::Blocked;
                return Step::Continue; // can't re-acquire yet — yield and retry
            }
        }

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
                    let code = self.metaspace.code(method);
                    u16::from_be_bytes([code[pc + 1], code[pc + 2]])
                };
                self.initialize_class_at(cp_index); // first active use: run <clinit>
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                class_operations::new(&mut self.metaspace, &mut self.heap, frame, cp_index);
                self.top().advance(3);
                Step::Continue
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
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                class_operations::getstatic(&mut self.metaspace, &mut self.heap, frame, cp_index);
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
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                class_operations::putstatic(&mut self.metaspace, &mut self.heap, frame, cp_index);
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
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                // A null receiver makes the field op return Err → the VM throws a NPE.
                match objects_operations::getfield(&mut self.metaspace, &mut self.heap, frame, cp_index) {
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
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                match objects_operations::putfield(&mut self.metaspace, &mut self.heap, frame, cp_index) {
                    Ok(()) => {
                        self.top().advance(3);
                        Step::Continue
                    }
                    Err(exc) => self.throw_exception(exc),
                }
            }

            // arraylength (0xbe): push an array's length (null array → NPE).
            0xbe => {
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::arraylength(&self.heap, frame);
                self.after_array_op(r, 1)
            }
            // Array loads — push an element: iaload(int) / baload(byte,bool) /
            // caload(char) / saload(short) / aaload(reference). Null array → NPE,
            // out-of-range index → ArrayIndexOutOfBoundsException.
            0x2e => {
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::iaload(&self.heap, frame);
                self.after_array_op(r, 1)
            }
            0x33 => {
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::baload(&self.heap, frame);
                self.after_array_op(r, 1)
            }
            0x34 => {
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::caload(&self.heap, frame);
                self.after_array_op(r, 1)
            }
            0x35 => {
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::saload(&self.heap, frame);
                self.after_array_op(r, 1)
            }
            0x32 => {
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::aaload(&self.heap, frame);
                self.after_array_op(r, 1)
            }
            // laload / faload / daload (0x2f/0x30/0x31): long/float/double elements.
            0x2f => {
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::laload(&self.heap, frame);
                self.after_array_op(r, 1)
            }
            0x30 => {
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::faload(&self.heap, frame);
                self.after_array_op(r, 1)
            }
            0x31 => {
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::daload(&self.heap, frame);
                self.after_array_op(r, 1)
            }
            // Array stores — pop value into an element: iastore(int) /
            // bastore(byte,bool) / castore(char) / sastore(short) / aastore(ref).
            // lastore / fastore / dastore (0x50/0x51/0x52): long/float/double elements.
            0x50 => {
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::lastore(&mut self.heap, frame);
                self.after_array_op(r, 1)
            }
            0x51 => {
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::fastore(&mut self.heap, frame);
                self.after_array_op(r, 1)
            }
            0x52 => {
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::dastore(&mut self.heap, frame);
                self.after_array_op(r, 1)
            }
            0x4f => {
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::iastore(&mut self.heap, frame);
                self.after_array_op(r, 1)
            }
            0x54 => {
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::bastore(&mut self.heap, frame);
                self.after_array_op(r, 1)
            }
            0x55 => {
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::castore(&mut self.heap, frame);
                self.after_array_op(r, 1)
            }
            0x56 => {
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::sastore(&mut self.heap, frame);
                self.after_array_op(r, 1)
            }
            0x53 => {
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::aastore(&mut self.heap, frame);
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
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                class_operations::instanceof(&mut self.metaspace, &self.heap, frame, cp_index);
                self.top().advance(3);
                Step::Continue
            }
            0xc0 => {
                let pc = self.frame().pc();
                let cp_index = {
                    let code = self.current_code();
                    u16::from_be_bytes([code[pc + 1], code[pc + 2]])
                };
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                // A bad cast returns Err → the VM throws ClassCastException.
                match class_operations::checkcast(&mut self.metaspace, &self.heap, frame, cp_index) {
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
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::newarray(&mut self.metaspace, &mut self.heap, frame, atype);
                self.after_array_op(r, 2) // negative size → NegativeArraySizeException
            }
            0xbd => {
                let pc = self.frame().pc();
                let cp_index = {
                    let code = self.current_code();
                    u16::from_be_bytes([code[pc + 1], code[pc + 2]])
                };
                let frame = self.frames.last_mut().expect("no frame on the call stack");
                let r = array_operations::anewarray(&mut self.metaspace, &mut self.heap, frame, cp_index);
                self.after_array_op(r, 3)
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
                let target = bifurcation_operations::switch_target(self.metaspace.code(method), pc, key);
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
        if self.frames.is_empty() {
            return Step::Return(None);
        }
        // A synthetic `<clinit>` frame wasn't reached via an invoke, so the caller's
        // pc must NOT advance — the instruction that triggered init resumes as-is.
        if !popped.map_or(false, |f| f.is_synthetic()) {
            self.advance_past_call();
        }
        Step::Continue
    }

    /// Ensures `class` is **initialized** before its first active use (JVMS §5.5):
    /// runs its `<clinit>` exactly once, *after* its superclass is initialized. The
    /// `<clinit>` runs synchronously — pushed as a synthetic frame and stepped to
    /// completion — so it finishes before the triggering instruction proceeds.
    /// `InProgress` short-circuits re-entrant uses (a class touching itself mid-init).
    fn ensure_initialized(&mut self, class: &str) {
        match self.metaspace.init_state(class) {
            InitState::Done | InitState::InProgress => return,
            InitState::NotStarted => {}
        }
        self.metaspace.set_init_state(class, InitState::InProgress);

        // Superclass first — initializing Dog initializes Animal (then Object).
        if let Some(superclass) = self.metaspace.superclass_name(class) {
            self.ensure_initialized(&superclass);
        }

        // Run the class's `<clinit>` (if it has one) to completion.
        if let Some(clinit) = self.metaspace.resolve_method(class, "<clinit>", "()V") {
            let max_locals = self.metaspace.max_locals(clinit);
            let base = self.frames.len();
            self.frames.push(Frame::new_synthetic(clinit, max_locals));
            // Drive `<clinit>` to completion on the *current* thread — `run_one`, not
            // `step`, so the scheduler doesn't interleave other threads mid-init.
            while self.frames.len() > base {
                self.run_one();
            }
        }
        self.metaspace.set_init_state(class, InitState::Done);
    }

    /// `ldc`/`ldc_w`: resolve the constant at `cp_index` in the current method's pool
    /// and push it. A `String` literal is materialised as a heap String and pushed as
    /// a reference; an `Integer` is pushed as an int.
    fn ldc(&mut self, cp_index: u16) {
        let caller = self.metaspace.class_of(self.frame().method()).to_string();

        // A String literal → materialise it on the heap, push the reference.
        let text = self.metaspace.get(&caller).and_then(|cf| cf.string_constant(cp_index)).map(str::to_string);
        if let Some(text) = text {
            let offset = strings::intern(&mut self.metaspace, &mut self.heap, &text);
            self.top().push(Value::Reference(offset));
            return;
        }

        // An int constant → push it directly.
        if let Some(value) = self.metaspace.get(&caller).and_then(|cf| cf.integer_constant(cp_index)) {
            self.top().push(Value::Int(value));
            return;
        }

        // A float constant (category-1, so it comes through `ldc`, not `ldc2_w`).
        if let Some(value) = self.metaspace.get(&caller).and_then(|cf| cf.float_constant(cp_index)) {
            self.top().push(Value::Float(value));
            return;
        }

        panic!("ldc: unsupported constant at #{cp_index} (only String/Integer/Float modelled)");
    }

    /// `ldc2_w` (0x14): load a category-2 constant — a `long` or a `double` — and
    /// push it. The pool index points at the `Long`/`Double` entry.
    fn ldc2_w(&mut self, cp_index: u16) {
        let caller = self.metaspace.class_of(self.frame().method()).to_string();
        if let Some(value) = self.metaspace.get(&caller).and_then(|cf| cf.long_constant(cp_index)) {
            self.top().push(Value::Long(value));
        } else if let Some(value) =
            self.metaspace.get(&caller).and_then(|cf| cf.double_constant(cp_index))
        {
            self.top().push(Value::Double(value));
        } else {
            panic!("ldc2_w: unsupported constant at #{cp_index} (only Long/Double modelled)");
        }
    }

    /// Initializes the class named by the `Class` constant at `cp_index` in the
    /// current method's pool — the trigger for `new`.
    fn initialize_class_at(&mut self, cp_index: u16) {
        let caller = self.metaspace.class_of(self.frame().method()).to_string();
        let class = self.metaspace.get(&caller).and_then(|cf| cf.class_name(cp_index)).map(str::to_string);
        if let Some(class) = class {
            self.ensure_initialized(&class);
        }
    }

    /// Initializes the class that owns the field at `cp_index` (a `Fieldref`) — the
    /// trigger for `getstatic`/`putstatic`.
    fn initialize_field_owner_at(&mut self, cp_index: u16) {
        let caller = self.metaspace.class_of(self.frame().method()).to_string();
        let owner = self
            .metaspace
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
        self.pop_frame();
        if self.frames.is_empty() {
            return Step::Return(Some(value));
        }
        self.advance_past_call();
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
        let opcode = self.metaspace.code(method)[pc];
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
                if let Step::Return(value) = interp.step() {
                    return value;
                }
            }
        }
        // Real OS threads serialised by a GIL.
        ThreadMode::Os => execute_os(metaspace, entry),
    }
}

/// OS-thread substrate: the program runs under a **GIL** — the whole VM lives behind one
/// `Arc<Mutex<JVM>>`. The main thread drives the loop on *this* OS thread; each
/// `Thread.start()` launches a real `std::thread` that competes for the same lock. Only
/// the GIL holder mutates VM state, so the heap, monitors and GC stay correct with no
/// extra synchronisation — the GIL *is* the stop-the-world. Removing it for true
/// parallelism (fine-grained locks + a real STW handshake) is the next milestone.
pub(crate) fn execute_os(metaspace: MetaspaceService, entry: Frame) -> Option<Value> {
    let mut jvm = JVM::new(metaspace, entry);
    jvm.mode = ThreadMode::Os; // force OS mode regardless of the env (e.g. in tests)
    // The main thread runs the loop on *this* OS thread; record its handle so workers can
    // `unpark` it (e.g. when a join target finishes) instead of waiting out the poll.
    jvm.threads[0].os_handle = Some(thread::current());
    // OS-mode invariant: between turns every thread's stack lives in *its slot* (so
    // `activate`/`deactivate` swap it into the shared `frames` to run). `JVM::new` follows
    // the green convention (main's entry frame in the active `frames`, slot 0 empty), so
    // move it into slot 0 once before the loop.
    std::mem::swap(&mut jvm.frames, &mut jvm.threads[0].frames);
    let gil = Arc::new(Mutex::new(jvm));
    os_thread_loop(&gil, 0)
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

/// One thread slot's run loop on its own OS thread. Acquires the GIL only to run a single
/// opcode, then yields so siblings can run; **parks** when blocked/waiting (woken by
/// [`JVM::make_runnable`]'s `unpark`). Returns the thread's result — the program result for
/// the main thread (`idx == 0`), ignored for workers.
fn os_thread_loop(gil: &Arc<Mutex<JVM>>, idx: usize) -> Option<Value> {
    loop {
        let tick = {
            let mut vm = gil.lock().unwrap();
            if vm.halt {
                return None; // main has finished — workers exit
            }
            match vm.threads[idx].status {
                ThreadStatus::Terminated => return None,
                ThreadStatus::Blocked | ThreadStatus::Waiting => os_block_tick(&vm, idx),
                ThreadStatus::Runnable => {
                    vm.activate(idx);
                    let step = vm.run_one();
                    vm.deactivate(idx);
                    spawn_pending(&mut vm, gil); // launch OS threads for new Thread.start() slots
                    vm.wake_sleepers();
                    if let Step::Return(value) = step {
                        vm.on_thread_terminated(idx);
                        if idx == 0 {
                            vm.halt = true; // program done → release the workers
                            vm.unpark_all();
                        }
                        OsTick::Done(value)
                    } else {
                        os_block_tick(&vm, idx) // may have blocked us (monitor/wait/join/sleep)
                    }
                }
            }
        }; // GIL released here

        match tick {
            OsTick::Done(value) => return value,
            OsTick::Yield => thread::yield_now(), // let a sibling grab the GIL
            OsTick::Park => thread::park_timeout(Duration::from_millis(50)),
            OsTick::Sleep(ms) => {
                thread::sleep(Duration::from_millis(ms));
                let mut vm = gil.lock().unwrap();
                vm.expire_timed_block(idx); // sleep done, or timed wait expired → re-acquire
            }
        }
    }
}

/// Classify a thread that didn't (or couldn't) run this turn: still runnable → yield;
/// sleeping → real sleep of the remaining ticks-as-millis (capped); otherwise park.
fn os_block_tick(vm: &JVM, idx: usize) -> OsTick {
    match vm.threads[idx].status {
        ThreadStatus::Runnable => OsTick::Yield,
        // Both a `sleep` (Blocked) and a timed `wait(ms)` (Waiting) carry a deadline →
        // a real-time sleep; an indefinite `wait()`/monitor block parks until unparked.
        ThreadStatus::Blocked | ThreadStatus::Waiting => match vm.threads[idx].sleep_until {
            Some(at) => OsTick::Sleep((at.saturating_sub(vm.steps)).min(200) as u64),
            None => OsTick::Park,
        },
        ThreadStatus::Terminated => OsTick::Park,
    }
}

/// Launch a real `std::thread` for every slot that doesn't have one yet (each
/// `Thread.start()` pushes a slot; this turns it into an OS thread exactly once). Runs
/// while the caller holds the GIL, so the handle is recorded before the child can run.
fn spawn_pending(vm: &mut JVM, gil: &Arc<Mutex<JVM>>) {
    let pending: Vec<usize> = (0..vm.threads.len()).filter(|&i| !vm.threads[i].os_spawned).collect();
    for i in pending {
        vm.threads[i].os_spawned = true;
        let child_gil = Arc::clone(gil);
        let handle = thread::spawn(move || {
            os_thread_loop(&child_gil, i);
        });
        // Keep the Thread handle for `unpark`; detach the JoinHandle (workers exit on halt).
        vm.threads[i].os_handle = Some(handle.thread().clone());
    }
}
