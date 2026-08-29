//! `athrow` (0xbf): throw an exception and **unwind** the call stack until a handler
//! catches it. An `impl JVM` method (it drives the whole frame stack, like
//! the invokes), dispatched from `step()`.
//!
//! The search: in each frame, from the top down, look at the running method's
//! exception table for a row whose `[start_pc, end_pc)` covers the current pc and
//! whose `catch_type` matches the thrown class (a subtype, via `is_subtype`). A
//! match installs the handler in that frame; no match pops the frame and retries the
//! caller. Empty stack → the exception was never caught.

use super::{class_operations, objects_operations};
use super::{Exec, Step};
use crate::jvm::interpreter::frame::Value;
use crate::jvm::interpreter::metaspace::MethodId;
use crate::jvm::interpreter::strings;

impl Exec<'_> {
    pub(super) fn athrow(&mut self) -> Step {
        // The exception object sits on top of the current frame's operand stack.
        // Throwing `null` is itself a NullPointerException.
        match self.top().pop() {
            Value::Reference(0) => self.throw_exception("java/lang/NullPointerException"),
            Value::Reference(offset) => self.unwind_with(offset),
            _ => panic!("athrow: expected an exception reference on the stack"),
        }
    }

    /// Throws an exception the **VM synthesizes itself** — the *implicit* exceptions
    /// raised by faults (a null receiver, an out-of-bounds index, a bad cast, …)
    /// rather than an explicit `athrow`. Loads/prepares `exc_class`, allocates an
    /// instance, and unwinds to its handler. Called from the faulting opcodes.
    pub(super) fn throw_exception(&mut self, exc_class: &str) -> Step {
        // Prepare the class so its mirror exists (the object header's class_id), then
        // allocate the exception instance — like a `new` the VM does on your behalf.
        class_operations::load_class(&mut self.shared.metaspace, &mut self.shared.heap, exc_class);
        let offset = objects_operations::allocate(&mut self.shared.metaspace, &mut self.shared.heap, exc_class);
        self.unwind_with(offset)
    }

    /// Unwinds the call stack for the exception object at heap `offset`: tries each
    /// frame top-down for a matching handler, popping the ones that don't catch it,
    /// until one does (jump into its `catch`) or the stack empties (uncaught).
    fn unwind_with(&mut self, exception: usize) -> Step {
        self.fire_exception(exception); // JVMTI (hito I): la excepción empieza a desenrollar
        let exc_class = self
            .shared.metaspace
            .class_name_at_mirror(self.shared.heap.read_u32(exception) as usize)
            .expect("throw: cannot resolve the thrown object's class")
            .to_string();

        // Record the call stack into the exception's `backtrace` field before we start popping
        // frames (both implicit faults and explicit `throw` funnel through here). `exception` may
        // move if interning triggers a GC, so `capture_backtrace` returns its current location.
        let exception = self.capture_backtrace(exception, &exc_class);

        loop {
            let method = self.frame().method();
            let pc = self.frame().pc();
            if let Some(handler_pc) = self.find_handler(method, pc, &exc_class) {
                // Caught: clear the operand stack, leave only the exception, and jump
                // to the handler — execution resumes inside the `catch`.
                let frame = self.top();
                frame.clear_stack();
                frame.push(Value::Reference(exception));
                frame.jump(handler_pc);
                return Step::Continue;
            }
            // Not handled here. If this frame is a `call_java` boundary (a `<clinit>` or intrinsic
            // callback the VM drove), stop: park the exception on the caller's operand stack
            // instead of unwinding through the code that made the call. `call_java` observes it.
            let top_index = self.running.frames.len() - 1;
            if self.running.exception_floor.last() == Some(&top_index) {
                self.pop_frame(); // drop the synthetic boundary frame; the VM takes over
                self.park_exception(exception); // onto the caller's stack, where the GC can see it
                return Step::Continue;
            }
            if self.running.frames.len() == 1 {
                // Uncaught (JVMS §2.10): nothing above this frame can catch it. Offer it to the
                // thread's `UncaughtExceptionHandler` first; if there is none (or it blew up),
                // print `Exception in thread "..." <toString>` + the captured stack trace.
                //
                // Note the order: the dispatch runs *before* the last frame is popped, because
                // that frame's operand stack is the only GC root left that can hold the exception
                // across the allocations running Java does. Then the thread ends. `Step::Return`
                // here is exactly a thread's final return: the scheduler marks a worker Terminated
                // (waking its joiners), and ends the program when it's the main thread.
                if let Some(exception) = self.dispatch_uncaught(exception) {
                    self.report_uncaught(exception, &exc_class);
                }
                self.pop_frame();
                return Step::Return(None);
            }
            // Otherwise discard the frame (releasing its monitor if it ran a synchronized
            // method) and try the caller.
            self.pop_frame();
        }
    }

    /// Hands an uncaught `exception` to the handler that should see it: this thread's
    /// `UncaughtExceptionHandler` if it has one, else `Thread`'s static default. Returns `None`
    /// when a handler ran — reporting is its job now — and `Some(exception)`, at its possibly
    /// **moved** location, when the VM must fall back to printing its own report.
    ///
    /// Called with the thread's bottom frame still on the stack, and that is load-bearing: the
    /// exception is parked on that frame's operand stack, a root the collector scans and forwards,
    /// so everything below is free to allocate — `main`'s `Thread` object is built on demand here,
    /// and the handler itself is arbitrary Java.
    ///
    /// A handler that throws is deliberately swallowed: it unwinds only as far as the `call_java`
    /// boundary (which parks it on the operand stack), we drop it, and the default report goes
    /// out for the *original* exception. A broken handler cannot silence the failure it was meant
    /// to report, and cannot re-enter this path either.
    fn dispatch_uncaught(&mut self, exception: usize) -> Option<usize> {
        // Park the exception before anything else can allocate.
        let frame = self.top();
        frame.clear_stack();
        frame.push(Value::Reference(exception));

        // The `Thread` argument the handler receives. For `main` this is where its object gets
        // fabricated (an allocation — hence the parking above).
        self.thread_current();
        let handler_at = objects_operations::field_offset(
            &mut self.shared.metaspace,
            "java/lang/Thread",
            "uncaughtExceptionHandler",
        );
        let mut handler = self.shared.heap.read_u32(self.current_thread_obj() + handler_at) as usize;
        if handler == 0 {
            // No per-thread handler: fall back to the process-wide default, read straight out of
            // `Thread`'s mirror. (`static_reference` can allocate that mirror, so `handler` is
            // read *after* it — and the thread object is re-read below.)
            handler = class_operations::static_reference(
                &mut self.shared.metaspace,
                &mut self.shared.heap,
                "java/lang/Thread",
                "defaultUncaughtExceptionHandler",
            );
        }
        let exception = self.parked_exception(exception);
        if handler == 0 {
            return Some(exception); // nobody wants it → the VM's own report
        }
        let args = vec![Value::Reference(self.current_thread_obj()), Value::Reference(exception)];
        // `void`, so there is no result to read — but `call_virtual` reports a *throw* through
        // parking it, and that we do care about.
        let _ = self.call_virtual(handler, "uncaughtException", "(Ljava/lang/Thread;Ljava/lang/Throwable;)V", args);
        if self.take_parked_exception().is_some() {
            return Some(self.parked_exception(exception));
        }
        None
    }

    /// The exception parked on the current frame's operand stack, read back at its **current**
    /// location — a GC that ran in the meantime forwarded it. `fallback` covers the impossible
    /// case of an empty stack.
    fn parked_exception(&mut self, fallback: usize) -> usize {
        match self.top().stack().last() {
            Some(Value::Reference(offset)) => *offset,
            _ => fallback,
        }
    }

    /// The current thread's `Thread` object (`0` when it has none — only `main`, and only before
    /// anything asks for it).
    fn current_thread_obj(&self) -> usize {
        self.shared.threads.get(self.running.current).map_or(0, |t| t.thread_obj)
    }

    /// Prints the uncaught-exception report to the program's console: the JVMS-shaped header
    /// (thread name + class + detail message) and the backtrace captured at throw time. All
    /// reads are plain field reads — no Java re-entry with an empty stack.
    fn report_uncaught(&mut self, exception: usize, exc_class: &str) {
        use std::fmt::Write;
        let thread_name = self.current_thread_name();
        let dotted = exc_class.replace('/', ".");
        let msg_off =
            objects_operations::field_offset(&mut self.shared.metaspace, exc_class, "message");
        let msg_ref = self.shared.heap.read_u32(exception + msg_off) as usize;
        let header = if msg_ref == 0 {
            dotted
        } else {
            format!("{dotted}: {}", strings::read(&self.shared.heap, msg_ref))
        };
        let _ = writeln!(self.shared.console, "Exception in thread \"{thread_name}\" {header}");
        let bt_off =
            objects_operations::field_offset(&mut self.shared.metaspace, exc_class, "backtrace");
        let bt_ref = self.shared.heap.read_u32(exception + bt_off) as usize;
        if bt_ref != 0 {
            let _ = writeln!(self.shared.console, "{}", strings::read(&self.shared.heap, bt_ref));
        }
    }

    /// The current thread's Java name ("main" for the entry thread, whose `Thread` object is
    /// lazily built and may not exist yet).
    fn current_thread_name(&mut self) -> String {
        let obj = self.current_thread_obj();
        if obj == 0 {
            return "main".to_string();
        }
        let name_off =
            objects_operations::field_offset(&mut self.shared.metaspace, "java/lang/Thread", "name");
        let name_ref = self.shared.heap.read_u32(obj + name_off) as usize;
        if name_ref == 0 {
            "main".to_string()
        } else {
            strings::read(&self.shared.heap, name_ref)
        }
    }

    /// Parks `exception` for the caller to re-deliver: pushes it onto the **current frame's operand
    /// stack** and raises the flag.
    ///
    /// The operand stack is the parking spot precisely because it is a **GC root** — `gc::roots`
    /// walks `threads[*].frames`, and reaching a safepoint syncs those frames into the thread's
    /// slot, so a collection that moves the throwable forwards this reference like any other.
    /// Keeping the offset in `RunningCtx` instead put it somewhere no collector path can see.
    ///
    /// Pushing *below* whatever the frame is already holding is safe: the interpreter only ever
    /// addresses the operand stack relative to its top, and every consumer takes the parked value
    /// back off before the frame runs another instruction ([`Self::take_pending_throw`]), so the
    /// push and the pop cancel. A frame that catches clears its stack anyway.
    pub(super) fn park_exception(&mut self, exception: usize) {
        assert!(
            !self.running.frames.is_empty(),
            "park_exception: no frame to park on — the exception would have nowhere rooted to live"
        );
        debug_assert!(!self.running.parked_exception, "park_exception: one is already parked");
        self.top().push(Value::Reference(exception));
        self.running.parked_exception = true;
    }

    /// Whether an exception is parked waiting to be re-delivered. The question a caller of
    /// [`Exec::call_java`] has to ask before believing a `None` means "returned void".
    pub(super) fn threw(&self) -> bool {
        self.running.parked_exception
    }

    /// Reads the parked throwable **without unparking it**, so it stays rooted on the operand stack
    /// while the VM decides what to do with it.
    pub(super) fn peek_parked_exception(&mut self) -> usize {
        assert!(self.running.parked_exception, "peek_parked_exception: nothing is parked");
        match self.top().stack().last() {
            Some(Value::Reference(offset)) => *offset,
            other => panic!("peek_parked_exception: the parked slot held {other:?}, not a reference"),
        }
    }

    /// Swaps the parked throwable for another one, keeping the slot rooted the whole time — what
    /// JVMS §5.5 needs when a non-`Error` initializer failure gets wrapped in
    /// `ExceptionInInitializerError`.
    pub(super) fn replace_parked_exception(&mut self, exception: usize) {
        assert!(self.running.parked_exception, "replace_parked_exception: nothing is parked");
        self.top().pop();
        self.top().push(Value::Reference(exception));
    }

    /// Takes the parked exception back off the operand stack, or `None` if nothing is parked.
    pub(super) fn take_parked_exception(&mut self) -> Option<usize> {
        if !self.running.parked_exception {
            return None;
        }
        self.running.parked_exception = false;
        match self.top().pop() {
            Value::Reference(offset) => Some(offset),
            other => panic!("take_parked_exception: the parked slot held {other:?}, not a reference"),
        }
    }

    /// If an exception unwound out of a `call_java` boundary and is parked, take it and deliver it
    /// into the **current** frame (the code that made the call) — resuming the ordinary unwind, now
    /// with no floor in the way. Returns the throw `Step` for the opcode to return, or `None` if
    /// nothing is pending.
    pub(super) fn take_pending_throw(&mut self) -> Option<Step> {
        let exception = self.take_parked_exception()?;
        Some(self.unwind_with(exception))
    }

    /// Loads and allocates a bare exception/error instance of `class` (no `<init>` run) — the same
    /// object the VM synthesizes for an implicit fault, used here to build the wrappers that class
    /// initialization failures need.
    pub(super) fn new_exception_object(&mut self, class: &str) -> usize {
        class_operations::load_class(&mut self.shared.metaspace, &mut self.shared.heap, class);
        objects_operations::allocate(&mut self.shared.metaspace, &mut self.shared.heap, class)
    }

    /// The binary name of the class of the throwable at heap `offset`.
    pub(super) fn exception_class_name(&self, offset: usize) -> String {
        self.shared.metaspace
            .class_name_at_mirror(self.shared.heap.read_u32(offset) as usize)
            .expect("exception object has a class")
            .to_string()
    }

    /// Renders and stores the current call stack into the exception's `backtrace` field (a
    /// pre-formatted `"\tat pkg.Class.method"` text, innermost frame first). Interning the text
    /// allocates, which may trigger a moving GC, so we **park the exception reference on the
    /// operand stack** (a GC root the collector scans and forwards) across the intern and read it
    /// back at its possibly-new location. Returns that up-to-date offset.
    fn capture_backtrace(&mut self, exception: usize, exc_class: &str) -> usize {
        let trace = self.render_backtrace();
        self.top().push(Value::Reference(exception));
        let interned = strings::intern(&mut self.shared.metaspace, &mut self.shared.heap, &trace);
        let exception = match self.top().pop() {
            Value::Reference(offset) => offset,
            _ => exception,
        };
        // Every thrown object is a Throwable subtype (the verifier guarantees it), so it inherits
        // the `backtrace` field; no further allocation happens before the write, so both offsets
        // stay valid.
        let off = objects_operations::field_offset(&mut self.shared.metaspace, exc_class, "backtrace");
        self.shared.heap.write_u32(exception + off, interned as u32);
        exception
    }

    /// The stack-trace text: one `"\tat pkg.Class.method"` line per live frame, top of stack
    /// (innermost call) first — the order a real trace prints.
    fn render_backtrace(&self) -> String {
        let mut trace = String::new();
        for (i, frame) in self.running.frames.iter().rev().enumerate() {
            let method = frame.method();
            let class = self.shared.metaspace.class_of(method).replace('/', ".");
            let name = self.shared.metaspace.name(method);
            if i > 0 {
                trace.push('\n');
            }
            trace.push_str("\tat ");
            trace.push_str(&class);
            trace.push('.');
            trace.push_str(name);
        }
        trace
    }

    /// Searches `method`'s exception table for a handler covering `pc` whose
    /// `catch_type` matches `exc_class`. `catch_type == 0` catches anything (a
    /// `finally`/catch-all). Returns the handler pc when one applies.
    fn find_handler(&mut self, method: MethodId, pc: usize, exc_class: &str) -> Option<usize> {
        let class = self.shared.metaspace.class_of(method).to_string();
        let pc = pc as u16;
        // Snapshot the rows so we don't hold a borrow on the metaspace while
        // resolving catch types and running `is_subtype`.
        let rows: Vec<(u16, u16, u16, u16)> = self
            .shared.metaspace
            .exception_table(method)
            .iter()
            .map(|e| (e.start_pc, e.end_pc, e.handler_pc, e.catch_type))
            .collect();
        for (start, end, handler, catch_type) in rows {
            if pc < start || pc >= end {
                continue; // pc outside this try range
            }
            if catch_type == 0 {
                return Some(handler as usize); // catch-all (finally)
            }
            let catch_name = self
                .shared.metaspace
                .get(&class)
                .and_then(|cf| cf.class_name(catch_type))
                .map(str::to_string);
            if let Some(name) = catch_name {
                if class_operations::is_subtype(&mut self.shared.metaspace, exc_class, &name) {
                    return Some(handler as usize);
                }
            }
        }
        None
    }
}
