//! Spins the class a `LambdaMetafactory` call site produces — a **real** class implementing the
//! functional interface, the way the JDK's metafactory does (with ASM), rather than the VM
//! shortcut that used to dispatch a synthetic object straight to the implementation.
//!
//! For a call site `name:(captures)Interface` with an implementation handle and the SAM's own
//! descriptor, it emits (via the `.class` writer):
//!
//! ```text
//! class <synthetic> implements <Interface> {
//!     <cap fields, one per captured value>
//!     <synthetic>(<captures>) { super(); this.cap0 = …; … }   // stores the captures
//!     <sam>(<args>) { return <impl>(this.cap0, …, args…); }    // captures lead, then args
//! }
//! ```
//!
//! Only `REF_invokeStatic` (kind 6) implementations are emitted — every lambda body javac
//! produces is a static `lambda$…`, and a `Class::staticMethod` reference is static too. Instance
//! method references (kind 5) would load the captured receiver and `invokevirtual`; not yet.

use crate::javac::class_writer::{ClassFile, FieldInfo, MethodInfo};

const ACC_PUBLIC: u16 = 0x0001;
const ACC_SUPER: u16 = 0x0020;
const ACC_FINAL: u16 = 0x0010;
const ACC_PRIVATE: u16 = 0x0002;

/// Generates the `.class` bytes for a lambda's implementing class. `synthetic` is the (unique)
/// class name; `interface`/`sam_name`/`sam_descriptor` describe the method it overrides;
/// `impl_*` name the static implementation to forward to; `captures` are the captured values'
/// descriptors, which become the leading fields and the constructor's parameters.
#[allow(clippy::too_many_arguments)]
pub fn generate_lambda_class(
    synthetic: &str,
    interface: &str,
    sam_name: &str,
    sam_descriptor: &str,
    impl_class: &str,
    impl_name: &str,
    impl_descriptor: &str,
    captures: &[String],
) -> Vec<u8> {
    let mut cf = ClassFile::new();
    cf.access_flags = ACC_PUBLIC | ACC_SUPER | ACC_FINAL;
    cf.this_class = cf.pool.class(synthetic);
    cf.super_class = cf.pool.class("java/lang/Object");
    cf.interfaces = vec![cf.pool.class(interface)];

    // One field per capture: `cap0`, `cap1`, … at their own descriptors.
    let field_names: Vec<String> = (0..captures.len()).map(|i| format!("cap{i}")).collect();
    for (name, descriptor) in field_names.iter().zip(captures) {
        let name_index = cf.pool.utf8(name);
        let descriptor_index = cf.pool.utf8(descriptor);
        cf.fields.push(FieldInfo {
            access_flags: ACC_PRIVATE | ACC_FINAL,
            name_index,
            descriptor_index,
            ..Default::default() // a spun capture field carries no annotations/signature/constant
        });
    }

    let object_init = cf.pool.methodref("java/lang/Object", "<init>", "()V");
    let field_refs: Vec<u16> = field_names
        .iter()
        .zip(captures)
        .map(|(name, descriptor)| cf.pool.fieldref(synthetic, name, descriptor))
        .collect();

    // --- Constructor: super(), then store each capture from its parameter into its field. ---
    let ctor_descriptor = format!("({})V", captures.concat());
    let mut ctor = Vec::new();
    ctor.push(0x2a); // aload_0
    push_u16(&mut ctor, 0xb7, object_init); // invokespecial Object.<init>
    let mut slot = 1u8; // param slots start after `this`
    for (field_ref, descriptor) in field_refs.iter().zip(captures) {
        ctor.push(0x2a); // aload_0
        ctor.push(load_opcode(descriptor));
        ctor.push(slot);
        push_u16(&mut ctor, 0xb5, *field_ref); // putfield cap<i>
        slot += slot_width(descriptor);
    }
    ctor.push(0xb1); // return
    let ctor_name = cf.pool.utf8("<init>");
    let ctor_desc = cf.pool.utf8(&ctor_descriptor);
    cf.methods.push(MethodInfo {
        access_flags: ACC_PUBLIC,
        name_index: ctor_name,
        descriptor_index: ctor_desc,
        max_stack: 2, // aload_0 + one value
        max_locals: slot as u16,
        code: ctor,
        stack_map: None,
        exceptions: Vec::new(),
        ..Default::default() // spun code: only `Code`, no debug/annotation attributes
    });

    // --- SAM method: load captures (from fields) then arguments, forward to the impl, return. ---
    let (sam_params, sam_return) = split_descriptor(sam_descriptor);
    let impl_ref = cf.pool.methodref(impl_class, impl_name, impl_descriptor);
    let mut sam = Vec::new();
    let mut depth = 0u16;
    for (field_ref, descriptor) in field_refs.iter().zip(captures) {
        sam.push(0x2a); // aload_0
        push_u16(&mut sam, 0xb4, *field_ref); // getfield cap<i>
        depth += slot_width(descriptor) as u16;
    }
    let mut arg_slot = 1u8; // after `this`
    for descriptor in &sam_params {
        sam.push(load_opcode(descriptor));
        sam.push(arg_slot);
        arg_slot += slot_width(descriptor);
        depth += slot_width(descriptor) as u16;
    }
    push_u16(&mut sam, 0xb8, impl_ref); // invokestatic <impl>
    sam.push(return_opcode(&sam_return));
    let sam_name_index = cf.pool.utf8(sam_name);
    let sam_desc_index = cf.pool.utf8(sam_descriptor);
    cf.methods.push(MethodInfo {
        access_flags: ACC_PUBLIC,
        name_index: sam_name_index,
        descriptor_index: sam_desc_index,
        // Peak stack is every capture + argument on the stack at the call, plus the transient
        // `aload_0` while fetching a field. `+1` covers that transient; harmless if slack.
        max_stack: depth + 1,
        max_locals: arg_slot as u16,
        code: sam,
        stack_map: None,
        exceptions: Vec::new(),
        ..Default::default() // spun code: only `Code`, no debug/annotation attributes
    });

    cf.to_bytes()
}

/// Appends `opcode` followed by a big-endian `u16` operand (a constant-pool index).
fn push_u16(code: &mut Vec<u8>, opcode: u8, operand: u16) {
    code.push(opcode);
    code.extend_from_slice(&operand.to_be_bytes());
}

/// The typed load opcode for a value of this field descriptor (`iload`/`lload`/…).
fn load_opcode(descriptor: &str) -> u8 {
    match descriptor.as_bytes()[0] {
        b'J' => 0x16, // lload
        b'F' => 0x17, // fload
        b'D' => 0x18, // dload
        b'L' | b'[' => 0x19, // aload
        _ => 0x15, // iload (I, S, B, C, Z)
    }
}

/// The typed return opcode for this return descriptor.
fn return_opcode(descriptor: &str) -> u8 {
    match descriptor.as_bytes()[0] {
        b'J' => 0xad, // lreturn
        b'F' => 0xae, // freturn
        b'D' => 0xaf, // dreturn
        b'L' | b'[' => 0xb0, // areturn
        b'V' => 0xb1, // return
        _ => 0xac, // ireturn
    }
}

/// Slots a value of this descriptor occupies (`long`/`double` = 2, everything else = 1).
fn slot_width(descriptor: &str) -> u8 {
    match descriptor.as_bytes()[0] {
        b'J' | b'D' => 2,
        _ => 1,
    }
}

/// Splits a method descriptor `"(params)ret"` into the list of parameter descriptors and the
/// return descriptor.
fn split_descriptor(descriptor: &str) -> (Vec<String>, String) {
    let bytes = descriptor.as_bytes();
    let mut i = 1; // skip '('
    let mut params = Vec::new();
    while bytes[i] != b')' {
        let start = i;
        while bytes[i] == b'[' {
            i += 1; // array dimensions
        }
        if bytes[i] == b'L' {
            while bytes[i] != b';' {
                i += 1;
            }
        }
        i += 1; // the primitive char or the ';'
        params.push(descriptor[start..i].to_string());
    }
    (params, descriptor[i + 1..].to_string()) // past ')'
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::jvm::class_file::ClassFile as ParsedClass;

    #[test]
    fn splits_a_method_descriptor() {
        let (params, ret) = split_descriptor("(I)I");
        assert_eq!(params, vec!["I"]);
        assert_eq!(ret, "I");
        let (params, ret) = split_descriptor("(ILjava/lang/String;[J)V");
        assert_eq!(params, vec!["I", "Ljava/lang/String;", "[J"]);
        assert_eq!(ret, "V");
        let (params, ret) = split_descriptor("()Ljava/lang/Object;");
        assert!(params.is_empty());
        assert_eq!(ret, "Ljava/lang/Object;");
    }

    #[test]
    fn generates_a_parseable_lambda_class() {
        // A capturing lambda `int a -> a + n` over one int: implements `Op`, one capture field,
        // SAM `apply(I)I` forwarding to a static `Host.lambda$0(II)I`.
        let bytes = generate_lambda_class(
            "Host$$Lambda$0",
            "Op",
            "apply",
            "(I)I",
            "Host",
            "lambda$0",
            "(II)I",
            &["I".to_string()],
        );
        // The VM's own parser must accept what the writer emitted.
        let parsed = ParsedClass::from_bytes(&bytes).expect("generated lambda class must parse");
        assert_eq!(parsed.class_name(parsed.this_class), Some("Host$$Lambda$0"));
        // It implements the interface and has the constructor + SAM.
        assert_eq!(parsed.interfaces.len(), 1);
        assert!(parsed.methods.iter().any(|m| parsed.utf8(m.name_index) == Some("apply")));
        assert!(parsed.methods.iter().any(|m| parsed.utf8(m.name_index) == Some("<init>")));
        assert_eq!(parsed.fields.len(), 1); // one capture
    }
}
