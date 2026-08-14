//! The execution state of a single method call: a **handle to its method**, its
//! **program counter**, its **operand stack** and its **local variables**. The
//! JVM is a stack machine, so instructions push/pop `Value`s on the operand stack
//! and read/write the locals by slot index. Each method invocation gets its own
//! `Frame`; the interpreter keeps a *stack* of them (the call stack).
//!
//! Note the frame does **not** own its bytecode: it references the method by a
//! [`MethodId`] index into the metaspace, the way the class file references
//! everything by constant-pool index. The bytecode is owned once, in the
//! metaspace, and shared by every frame of that method.

use super::metaspace::MethodId;

/// A value living on the operand stack or in a local slot. `Int` is the scalar
/// case; `Reference` is an object handle — a **byte offset into the heap** (the
/// JVM's `reference` type, modelled the way we model the heap: as a position, not
/// a pointer). `Long`/`Double` are **category-2** types (logically two JVM slots,
/// held in one `Value`; the high-half local index is left unused — the compiler
/// indexes the next variable past it).
///
/// `Eq` is intentionally absent: `Double` wraps an `f64`, which is only `PartialEq`
/// (NaN ≠ NaN). Nothing uses `Value` as a map key, so `PartialEq` is enough.
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum Value {
    Int(i32),
    /// A reference to a heap object: its starting offset, or `0` for `null`.
    Reference(usize),
    /// A `long` — 64-bit signed. Category-2: spans two JVM slots (we use one entry).
    Long(i64),
    /// A `double` — 64-bit IEEE-754. Category-2, like `long`.
    Double(f64),
    /// A `float` — 32-bit IEEE-754. **Category-1** (one slot, 4 bytes), so unlike
    /// `long`/`double` it needs no high-half slot and fits the 4-byte heap accessors.
    Float(f32),
}

/// One method invocation's context. A fresh `Frame` is created per call.
pub struct Frame {
    /// Handle to this call's method body in the metaspace — resolve it to the
    /// bytecode with `MetaspaceService::code`. Many frames can share one method.
    method: MethodId,
    /// Program counter: the byte offset of the next opcode to run *in this frame*.
    /// Per-frame, so a caller's position survives while a callee runs (JVMS gives
    /// every frame its own "pc register").
    pc: usize,
    /// Operand stack: instructions push operands here and pop their results.
    stack: Vec<Value>,
    /// Local variables, indexed by slot. The method arguments occupy the first
    /// slots (slot 0 is `this` for instance methods); the rest are scratch.
    locals: Vec<Value>,
    /// `true` for a frame the VM pushed itself — a `<clinit>` run during class
    /// initialization — rather than one from an `invoke`. On return, a synthetic
    /// frame must *not* advance the caller's pc (the triggering instruction resumes).
    synthetic: bool,
    /// If this frame runs a `synchronized` method, the heap offset of the object whose
    /// monitor it holds — `this` for an instance method, the `Class` mirror for a static
    /// one. `None` for an ordinary method. The VM releases this monitor when the frame is
    /// popped (there is no `monitorexit` opcode to do it). See `JVM::pop_frame`.
    monitor: Option<usize>,
}

impl Frame {
    /// Builds a frame for the method `method` with `max_locals` slots, the leading
    /// ones initialised to the call `args`, the rest left as `Int(0)`, pc at 0.
    pub fn new(method: MethodId, max_locals: usize, args: Vec<Value>) -> Self {
        let mut locals = args;
        locals.resize(max_locals, Value::Int(0));
        Frame { method, pc: 0, stack: Vec::new(), locals, synthetic: false, monitor: None }
    }

    /// Builds a call frame, placing `args` into the leading locals with **category-2
    /// gaps**: a `long`/`double` argument occupies two slots, so the next argument
    /// lands one slot further along. `slot_widths[i]` is `args[i]`'s width (the
    /// receiver, for an instance call, is width 1). The rest start as `Int(0)`.
    pub fn for_call(
        method: MethodId,
        max_locals: usize,
        args: Vec<Value>,
        slot_widths: &[usize],
    ) -> Self {
        let mut frame =
            Frame { method, pc: 0, stack: Vec::new(), locals: Vec::new(), synthetic: false, monitor: None };
        frame.reset_for_call(method, max_locals, args, slot_widths);
        frame
    }

    /// Re-arms a **pooled** frame for a new call: exactly what [`Frame::for_call`] builds, but
    /// written into the buffers this frame already owns. That is the whole point of the frame
    /// pool (Fase F): `locals` and `stack` keep the capacity they grew during the previous call,
    /// so a call costs no `Vec` allocation at all (and its `return` no free).
    ///
    /// Every field is (re)set here — nothing is inherited from the previous occupant. In
    /// particular `locals` is refilled from index 0 through `max_locals`, so **every slot the
    /// frame will ever expose is written before it can be read**: `load`/`store` index `locals`,
    /// which after this call is exactly `max_locals` fresh entries. `stack` starts empty, as a
    /// method's operand stack must.
    pub fn reset_for_call(
        &mut self,
        method: MethodId,
        max_locals: usize,
        args: Vec<Value>,
        slot_widths: &[usize],
    ) {
        self.method = method;
        self.pc = 0;
        self.synthetic = false;
        self.monitor = None;
        self.stack.clear();
        self.locals.clear();
        self.locals.resize(max_locals, Value::Int(0));
        let mut index = 0;
        for (arg, &width) in args.into_iter().zip(slot_widths) {
            if index < self.locals.len() {
                self.locals[index] = arg;
            }
            index += width;
        }
    }

    /// **Scrubs a retired frame** so it may sit in the thread's frame pool: empties `locals` and
    /// the operand stack (keeping their capacity — that capacity *is* the pool's payoff) and
    /// resets the per-call flags, including the `monitor` its caller has just released.
    ///
    /// This is the frame pool's correctness rule, and it is not an optimisation detail: a frame
    /// that has just returned is full of `Value::Reference`s into the heap, and once it leaves the
    /// call stack the collector no longer scans it (the GC's roots are `threads[*].frames`, and the
    /// pool is deliberately not among them — see `RunningCtx::frame_pool`). Keeping those
    /// references around would be a leak at best — pinning dead objects alive — and a *stale
    /// reference* at worst: a moving collection would never remap them, so reusing the frame would
    /// hand the new call a pointer to wherever that object used to live. So: nothing survives the
    /// trip through the pool.
    ///
    /// `clear()` sets the length to zero without touching the backing buffer (`Value` is `Copy`,
    /// so there is nothing to drop). That is sufficient, and deliberately so: the only ways to see
    /// a `Value` in a frame — `load`, `stack()`, `locals()`, `operands_mut()`,
    /// `remap_references()` — all go through the `Vec`'s *length*, so a scrubbed frame yields no
    /// values to anyone, the collector included; and [`Frame::reset_for_call`] rewrites every slot
    /// it re-exposes before the new call can read it. Zeroing the spare capacity as well would
    /// cost O(capacity) per return and change nothing observable.
    pub fn scrub(&mut self) {
        self.stack.clear();
        self.locals.clear();
        self.pc = 0;
        self.synthetic = false;
        self.monitor = None;
    }

    /// Whether this frame holds no values at all — the invariant every frame in the pool must
    /// satisfy (see [`Frame::scrub`]). Used by the pool's assertions and its tests.
    pub fn is_scrubbed(&self) -> bool {
        self.stack.is_empty() && self.locals.is_empty() && self.monitor.is_none()
    }

    /// A frame for a VM-run `<clinit>` (no arguments). Marked synthetic so its
    /// `return` doesn't advance the instruction that triggered initialization.
    pub fn new_synthetic(method: MethodId, max_locals: usize) -> Self {
        let mut frame = Frame::new(method, max_locals, Vec::new());
        frame.synthetic = true;
        frame
    }

    /// Whether this is a VM-pushed `<clinit>` frame (see [`Frame::new_synthetic`]).
    pub fn is_synthetic(&self) -> bool {
        self.synthetic
    }

    /// Marks a frame as VM-pushed after it was built with arguments — the general case
    /// [`Frame::new_synthetic`] covers only for the argument-less `<clinit>`.
    pub fn mark_synthetic(&mut self) {
        self.synthetic = true;
    }

    /// Records that this frame holds `obj`'s monitor (a `synchronized` method) — set
    /// just after the frame is built, once the monitor has been acquired.
    pub fn set_monitor(&mut self, obj: usize) {
        self.monitor = Some(obj);
    }

    /// The object whose monitor this frame holds, if it runs a `synchronized` method —
    /// the monitor the VM must release when the frame is popped.
    pub fn monitor(&self) -> Option<usize> {
        self.monitor
    }

    /// Pushes a value onto the operand stack.
    pub fn push(&mut self, value: Value) {
        self.stack.push(value);
    }

    /// Pops the top of the operand stack. Well-formed bytecode never underflows
    /// (the verifier guarantees it), so a missing value is a bug, not bad input.
    pub fn pop(&mut self) -> Value {
        self.stack.pop().expect("operand stack underflow")
    }

    /// Reads local variable `slot`.
    pub fn load(&self, slot: usize) -> Value {
        self.locals[slot]
    }

    /// Writes local variable `slot`.
    pub fn store(&mut self, slot: usize, value: Value) {
        self.locals[slot] = value;
    }

    /// Read-only view of the operand stack (bottom → top), for tooling such as
    /// the step visualizer.
    pub fn stack(&self) -> &[Value] {
        &self.stack
    }

    /// Mutable access to the operand stack — for the stack-manipulation opcodes
    /// (`dup`/`pop`/`swap`…), which insert and remove at positions the plain
    /// `push`/`pop` can't reach.
    pub fn operands_mut(&mut self) -> &mut Vec<Value> {
        &mut self.stack
    }

    /// Empties the operand stack. When an exception is caught, the JVM clears the
    /// handler frame's stack and leaves only the exception reference on it.
    pub fn clear_stack(&mut self) {
        self.stack.clear();
    }

    /// Read-only view of the local-variable slots, for tooling.
    pub fn locals(&self) -> &[Value] {
        &self.locals
    }

    /// Rewrites every reference in this frame (operand stack + locals) through
    /// `remap` — the GC compactor calls it so roots follow objects it relocated.
    /// `remap` maps an old heap offset to its new one (identity if unchanged).
    pub fn remap_references(&mut self, remap: impl Fn(usize) -> usize) {
        for value in self.stack.iter_mut().chain(self.locals.iter_mut()) {
            if let Value::Reference(offset) = value {
                *offset = remap(*offset);
            }
        }
        // A `synchronized` method's lock object can move too — keep the release target
        // (used by `pop_frame`) pointing at the relocated object.
        if let Some(obj) = self.monitor {
            self.monitor = Some(remap(obj));
        }
    }

    /// This frame's method handle — resolve its bytecode via `MetaspaceService::code`.
    pub fn method(&self) -> MethodId {
        self.method
    }

    /// The program counter — byte offset of the next opcode in this frame.
    pub fn pc(&self) -> usize {
        self.pc
    }

    /// Advances the pc past an instruction `n` bytes long.
    pub fn advance(&mut self, n: usize) {
        self.pc += n;
    }

    /// Jumps the pc to an absolute target. Used by branch opcodes (`goto`,
    /// `if_*`): unlike `advance`, which steps forward by one instruction, this
    /// sets the pc to a computed destination (which may be *backward*, as a loop's
    /// back-edge).
    pub fn jump(&mut self, target: usize) {
        self.pc = target;
    }
}
