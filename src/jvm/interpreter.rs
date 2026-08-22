//! The execution engine: runs `.class` bytecode (as opposed to just
//! disassembling it). Starts minimal — integer arithmetic in a single method —
//! and grows from there.

pub mod apt;
pub mod bytecode_interpreter;
pub mod frame;
pub mod gc;
pub mod atomic_region;
pub mod eden_arena;
pub mod heap;
pub mod metaspace;
pub mod natives;
pub mod strings;
