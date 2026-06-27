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
use super::{JVM, Step};
use crate::jvm::interpreter::frame::Value;
use crate::jvm::interpreter::metaspace::MethodId;

impl JVM {
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
        class_operations::load_class(&mut self.metaspace, &mut self.heap, exc_class);
        let offset = objects_operations::allocate(&mut self.metaspace, &mut self.heap, exc_class);
        self.unwind_with(offset)
    }

    /// Unwinds the call stack for the exception object at heap `offset`: tries each
    /// frame top-down for a matching handler, popping the ones that don't catch it,
    /// until one does (jump into its `catch`) or the stack empties (uncaught).
    fn unwind_with(&mut self, exception: usize) -> Step {
        let exc_class = self
            .metaspace
            .class_name_at_mirror(self.heap.read_u32(exception) as usize)
            .expect("throw: cannot resolve the thrown object's class")
            .to_string();

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
            // Not handled in this frame — discard it (releasing its monitor if it ran a
            // synchronized method) and try the caller.
            self.pop_frame();
            if self.frames.is_empty() {
                panic!("uncaught exception: {exc_class}");
            }
        }
    }

    /// Searches `method`'s exception table for a handler covering `pc` whose
    /// `catch_type` matches `exc_class`. `catch_type == 0` catches anything (a
    /// `finally`/catch-all). Returns the handler pc when one applies.
    fn find_handler(&mut self, method: MethodId, pc: usize, exc_class: &str) -> Option<usize> {
        let class = self.metaspace.class_of(method).to_string();
        let pc = pc as u16;
        // Snapshot the rows so we don't hold a borrow on the metaspace while
        // resolving catch types and running `is_subtype`.
        let rows: Vec<(u16, u16, u16, u16)> = self
            .metaspace
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
                .metaspace
                .get(&class)
                .and_then(|cf| cf.class_name(catch_type))
                .map(str::to_string);
            if let Some(name) = catch_name {
                if class_operations::is_subtype(&mut self.metaspace, exc_class, &name) {
                    return Some(handler as usize);
                }
            }
        }
        None
    }
}
