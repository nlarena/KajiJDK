//! The `jvm` crate library: the class-file parser, the javap-style printers, the
//! pretty visualizer and the bytecode interpreter. The binaries are thin shells
//! over this — `jvm` (disassembly, `src/main.rs`) and `jvm-step` (step-by-step
//! execution, `src/bin/jvm-step.rs`) — so both can share this one code base.

/// Prints a line of javap output, trimming trailing whitespace — javap drops any
/// trailing spaces on every line (e.g. a string constant ending in a space, or
/// an empty flag list), so all javap-faithful output goes through this.
#[macro_export]
macro_rules! pln {
    () => { println!() };
    ($($arg:tt)*) => {{ println!("{}", format!($($arg)*).trim_end()); }};
}

pub mod javap;
pub mod jvm;
pub mod pretty_class_visualizer;
