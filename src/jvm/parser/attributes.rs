//! Decoders for `.class` attributes beyond the core ones already handled inline
//! (`Code`, `LineNumberTable`, `SourceFile`) and the `StackMapTable` (which has
//! its own module). Each non-trivial attribute gets its own module here.

pub mod annotations;
pub mod bootstrap_methods;
pub mod constant_value;
pub mod exceptions;
pub mod inner_classes;
pub mod local_variables;
pub mod method_parameters;
pub mod module;
pub mod nest;
pub mod record;
pub mod signature;
