//! `BootstrapMethods` (JVMS §4.7.23): a class-level attribute listing the
//! bootstrap methods referenced by `invokedynamic`/`Dynamic` constant-pool
//! entries. Each entry is a `MethodHandle` constant-pool index plus a list of
//! static-argument constant-pool indices.

use super::super::reader::ClassReader;

pub struct BootstrapMethod {
    /// Constant-pool index of the bootstrap method's `MethodHandle`.
    pub method_ref: u16,
    /// Constant-pool indices of the static bootstrap arguments.
    pub arguments: Vec<u16>,
}

/// Parses the attribute body: `u2 count`, then for each method a `u2`
/// MethodHandle ref, a `u2` argument count, and that many `u2` argument indices.
pub fn parse(bytes: &[u8]) -> Vec<BootstrapMethod> {
    let mut r = ClassReader::new(bytes);
    let mut methods = Vec::new();
    let Ok(count) = r.read_u16() else { return methods };
    for _ in 0..count {
        let Ok(method_ref) = r.read_u16() else { break };
        let Ok(num_args) = r.read_u16() else { break };
        let mut arguments = Vec::with_capacity(num_args as usize);
        for _ in 0..num_args {
            match r.read_u16() {
                Ok(a) => arguments.push(a),
                Err(_) => break,
            }
        }
        methods.push(BootstrapMethod { method_ref, arguments });
    }
    methods
}
