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
            // callback the VM drove), stop: surface the exception to the VM via `pending_exception`
            // instead of unwinding through the code that made the call. `call_java` observes it.
            let top_index = self.running.frames.len() - 1;
            if self.running.exception_floor.last() == Some(&top_index) {
                self.running.pending_exception = Some(exception);
                self.pop_frame(); // drop the synthetic boundary frame; the VM takes over
                return Step::Continue;
            }
            // Otherwise discard the frame (releasing its monitor if it ran a synchronized
            // method) and try the caller.
            self.pop_frame();
            if self.running.frames.is_empty() {
                // Uncaught (JVMS §2.10): print `Exception in thread "..." <toString>` + the
                // captured stack trace, then terminate the thread. `Step::Return` here is
                // exactly a thread's final return: the scheduler marks a worker Terminated
                // (waking its joiners), and ends the program when it's the main thread.
                self.report_uncaught(exception, &exc_class);
                return Step::Return(None);
            }
        }
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
        let obj = self
            .shared
            .threads
            .get(self.running.current)
            .map(|t| t.thread_obj)
            .unwrap_or(0);
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

    /// If an exception unwound out of a `call_java` boundary and is waiting in
    /// `pending_exception`, take it and deliver it into the **current** frame (the code that made
    /// the call) — resuming the ordinary unwind, now with no floor in the way. Returns the throw
    /// `Step` for the opcode to return, or `None` if nothing is pending.
    pub(super) fn take_pending_throw(&mut self) -> Option<Step> {
        let exception = self.running.pending_exception.take()?;
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
