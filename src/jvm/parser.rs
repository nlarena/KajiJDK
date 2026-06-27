//! Parsing of the `.class` binary format.

pub mod attribute;
pub mod attributes;
pub mod code;
pub mod float_to_decimal;
pub mod constant_pool;
pub mod member;
pub mod printers;
pub mod reader;
pub mod stack_map_table;

// Re-export the most-used items so callers can write `jvm::parser::X`
// instead of the longer `jvm::parser::<submodule>::X`.
pub use attribute::AttributeInfo;
pub use code::Code;
pub use constant_pool::ConstantPoolEntry;
pub use member::MemberInfo;
pub use reader::{ClassReader, ParseError};
