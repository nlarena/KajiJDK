//! The JVM: loads and executes Java bytecode.

pub mod class_file;
pub mod inflate;
pub mod interpreter;
pub mod jimage;
pub mod modules;
pub mod opcode;
pub mod parser;
pub mod uuid;
pub mod verifier;
