//! Spins the class an **annotation instance** is: a real class implementing the `@interface`, the
//! way the JDK builds one (there via a `Proxy` + `AnnotationInvocationHandler`; here, as with
//! lambdas, via the `.class` writer). `Class.getAnnotation` needs an OBJECT whose type is the
//! annotation interface and whose element methods return the values written at the use site — this
//! is what produces it.
//!
//! An annotation's values are all compile-time constants, so unlike the lambda factory there are no
//! captures and no constructor parameters: each element method just **bakes its value in** (an
//! `ldc`, a `getstatic` for an enum constant, an array built inline) and returns it. The class also
//! carries `annotationType()` (returning the `@interface`'s `Class`) and a default constructor;
//! `equals`/`hashCode`/`toString` are inherited from `Object` (identity), which satisfies the
//! `Annotation` interface but is not the value-based equality the spec asks for — a documented
//! limitation of this subset. Nested annotations as element values are not baked (they would need a
//! second spun instance); such an element returns `null`.
//!
//! ```text
//! class <synthetic> implements <@interface> {
//!     <synthetic>() { super(); }
//!     <each element>() { return <baked value>; }
//!     Class annotationType() { return <@interface>.class; }
//! }
//! ```

use crate::javac::class_writer::{ClassFile, MethodInfo};
use crate::jvm::parser::attributes::annotations::ResolvedValue;

const ACC_PUBLIC: u16 = 0x0001;
const ACC_SUPER: u16 = 0x0020;
const ACC_FINAL: u16 = 0x0010;

/// One element of the annotation: the accessor's `name`, its return `descriptor`, and the resolved
/// `value` to bake in (the use-site value, or the `@interface`'s default).
pub struct Element {
    pub name: String,
    pub descriptor: String,
    pub value: ResolvedValue,
}

/// Generate the `.class` bytes for the class an instance of `annotation_interface` is. `synthetic`
/// is the unique class name; `elements` are its accessors with the values to return.
pub fn generate_annotation_class(
    synthetic: &str,
    annotation_interface: &str,
    elements: &[Element],
) -> Vec<u8> {
    let mut cf = ClassFile::new();
    cf.access_flags = ACC_PUBLIC | ACC_SUPER | ACC_FINAL;
    cf.this_class = cf.pool.class(synthetic);
    cf.super_class = cf.pool.class("java/lang/Object");
    cf.interfaces = vec![cf.pool.class(annotation_interface)];

    // --- default constructor: super(); return; ---
    let object_init = cf.pool.methodref("java/lang/Object", "<init>", "()V");
    let mut ctor = Vec::new();
    ctor.push(0x2a); // aload_0
    push_u16(&mut ctor, 0xb7, object_init); // invokespecial Object.<init>
    ctor.push(0xb1); // return
    let ctor_name = cf.pool.utf8("<init>");
    let ctor_desc = cf.pool.utf8("()V");
    cf.methods.push(MethodInfo {
        access_flags: ACC_PUBLIC,
        name_index: ctor_name,
        descriptor_index: ctor_desc,
        max_stack: 1,
        max_locals: 1,
        code: ctor,
        stack_map: None,
        exceptions: Vec::new(),
        ..Default::default()
    });

    // --- one accessor per element, returning its baked value ---
    for e in elements {
        let (_params, ret) = split_return(&e.descriptor);
        let mut code = Vec::new();
        let peak = emit_typed(&mut code, &mut cf.pool, &ret, &e.value);
        code.push(return_opcode(&ret));
        let name_index = cf.pool.utf8(&e.name);
        let desc_index = cf.pool.utf8(&e.descriptor);
        cf.methods.push(MethodInfo {
            access_flags: ACC_PUBLIC,
            name_index,
            descriptor_index: desc_index,
            max_stack: peak.max(1),
            max_locals: 1, // just `this`; elements take no parameters
            code,
            stack_map: None,
            exceptions: Vec::new(),
            ..Default::default()
        });
    }

    // --- annotationType(): return the @interface's Class ---
    let mut at = Vec::new();
    let iface_class = cf.pool.class(annotation_interface);
    push_u16(&mut at, 0x13, iface_class); // ldc_w <@interface>.class
    at.push(0xb0); // areturn
    let at_name = cf.pool.utf8("annotationType");
    let at_desc = cf.pool.utf8("()Ljava/lang/Class;");
    cf.methods.push(MethodInfo {
        access_flags: ACC_PUBLIC,
        name_index: at_name,
        descriptor_index: at_desc,
        max_stack: 1,
        max_locals: 1,
        code: at,
        stack_map: None,
        exceptions: Vec::new(),
        ..Default::default()
    });

    cf.to_bytes()
}

/// Push a value known to have field descriptor `desc` onto the stack; returns the peak stack it
/// uses. Descriptor-driven so that an EMPTY array still knows its component type.
fn emit_typed(
    code: &mut Vec<u8>,
    pool: &mut crate::javac::class_writer::ConstantPool,
    desc: &str,
    value: &ResolvedValue,
) -> u16 {
    match desc.as_bytes()[0] {
        b'Z' | b'B' | b'C' | b'S' | b'I' => {
            push_u16(code, 0x13, pool.integer(as_int(value))); // ldc_w
            1
        }
        b'J' => {
            push_u16(code, 0x14, pool.long(as_long(value))); // ldc2_w
            2
        }
        b'F' => {
            push_u16(code, 0x13, pool.float(as_float(value)));
            1
        }
        b'D' => {
            push_u16(code, 0x14, pool.double(as_double(value)));
            2
        }
        b'[' => emit_array(code, pool, &desc[1..], value),
        b'L' => emit_ref(code, pool, desc, value),
        _ => {
            code.push(0x01); // aconst_null — unknown descriptor, should not happen
            1
        }
    }
}

/// A reference-typed element: `String`, `Class`, an enum constant, or (unsupported) a nested
/// annotation → `null`.
fn emit_ref(
    code: &mut Vec<u8>,
    pool: &mut crate::javac::class_writer::ConstantPool,
    desc: &str,
    value: &ResolvedValue,
) -> u16 {
    match value {
        ResolvedValue::Str(s) => {
            push_u16(code, 0x13, pool.string(s)); // ldc_w
        }
        ResolvedValue::Class { descriptor } => emit_class_literal(code, pool, descriptor),
        ResolvedValue::Enum { type_descriptor, const_name } => {
            let internal = strip_l(type_descriptor);
            let field = pool.fieldref(internal, const_name, type_descriptor);
            push_u16(code, 0xb2, field); // getstatic E.CONST
        }
        // A nested annotation would need its own spun instance; not baked in this subset.
        ResolvedValue::Nested(_) => code.push(0x01), // aconst_null
        // The descriptor says reference but the value is something else — be safe.
        _ => {
            let _ = desc;
            code.push(0x01);
        }
    }
    1
}

/// Build an array whose component descriptor is `component`, filling it with `value`'s items.
fn emit_array(
    code: &mut Vec<u8>,
    pool: &mut crate::javac::class_writer::ConstantPool,
    component: &str,
    value: &ResolvedValue,
) -> u16 {
    let items: &[ResolvedValue] = match value {
        ResolvedValue::Array(v) => v,
        _ => &[], // descriptor says array but value isn't one: emit an empty array
    };
    // length
    push_u16(code, 0x13, pool.integer(items.len() as i32)); // ldc_w len
    // allocate
    match component.as_bytes()[0] {
        b'L' | b'[' => {
            let comp_class = if component.starts_with('L') {
                strip_l(component).to_string()
            } else {
                component.to_string() // an array component's CONSTANT_Class name is the descriptor
            };
            let c = pool.class(&comp_class);
            push_u16(code, 0xbd, c); // anewarray
        }
        prim => {
            code.push(0xbc); // newarray
            code.push(atype(prim));
        }
    }
    // fill
    let store = arraystore_opcode(component.as_bytes()[0]);
    let mut value_peak = 1u16;
    for (i, item) in items.iter().enumerate() {
        code.push(0x59); // dup (arrayref)
        push_u16(code, 0x13, pool.integer(i as i32)); // ldc_w index
        let p = emit_typed(code, pool, component, item);
        value_peak = value_peak.max(p);
        code.push(store);
    }
    // peak: arrayref + dup(arrayref) + index + value  (value_peak covers long/double = 2)
    3 + value_peak
}

fn emit_class_literal(
    code: &mut Vec<u8>,
    pool: &mut crate::javac::class_writer::ConstantPool,
    descriptor: &str,
) {
    match descriptor.as_bytes()[0] {
        b'L' => {
            let c = pool.class(strip_l(descriptor));
            push_u16(code, 0x13, c); // ldc_w <T>.class
        }
        b'[' => {
            let c = pool.class(descriptor);
            push_u16(code, 0x13, c);
        }
        prim => {
            // A primitive class literal is the wrapper's static TYPE field.
            let wrapper = wrapper_of(prim);
            let field = pool.fieldref(wrapper, "TYPE", "Ljava/lang/Class;");
            push_u16(code, 0xb2, field); // getstatic Integer.TYPE, …
        }
    }
}

// ---- value coercions (annotation constants live as Integer/Long/… in the pool) ----

fn as_int(v: &ResolvedValue) -> i32 {
    match v {
        ResolvedValue::Int(x) => *x,
        ResolvedValue::Short(x) => *x as i32,
        ResolvedValue::Byte(x) => *x as i32,
        ResolvedValue::Char(x) => *x as i32,
        ResolvedValue::Bool(x) => *x as i32,
        _ => 0,
    }
}

fn as_long(v: &ResolvedValue) -> i64 {
    match v {
        ResolvedValue::Long(x) => *x,
        _ => 0,
    }
}

fn as_float(v: &ResolvedValue) -> f32 {
    match v {
        ResolvedValue::Float(x) => *x,
        _ => 0.0,
    }
}

fn as_double(v: &ResolvedValue) -> f64 {
    match v {
        ResolvedValue::Double(x) => *x,
        _ => 0.0,
    }
}

/// Strip a `Lpkg/Name;` field descriptor to the internal name `pkg/Name`.
fn strip_l(descriptor: &str) -> &str {
    descriptor.strip_prefix('L').and_then(|s| s.strip_suffix(';')).unwrap_or(descriptor)
}

fn wrapper_of(prim: u8) -> &'static str {
    match prim {
        b'I' => "java/lang/Integer",
        b'J' => "java/lang/Long",
        b'F' => "java/lang/Float",
        b'D' => "java/lang/Double",
        b'S' => "java/lang/Short",
        b'B' => "java/lang/Byte",
        b'C' => "java/lang/Character",
        b'Z' => "java/lang/Boolean",
        _ => "java/lang/Void",
    }
}

fn atype(prim: u8) -> u8 {
    match prim {
        b'Z' => 4,
        b'C' => 5,
        b'F' => 6,
        b'D' => 7,
        b'B' => 8,
        b'S' => 9,
        b'I' => 10,
        b'J' => 11,
        _ => 10,
    }
}

fn arraystore_opcode(component: u8) -> u8 {
    match component {
        b'J' => 0x50, // lastore
        b'F' => 0x51, // fastore
        b'D' => 0x52, // dastore
        b'L' | b'[' => 0x53, // aastore
        b'B' | b'Z' => 0x54, // bastore
        b'C' => 0x55, // castore
        b'S' => 0x56, // sastore
        _ => 0x4f, // iastore (I)
    }
}

fn return_opcode(descriptor: &str) -> u8 {
    match descriptor.as_bytes()[0] {
        b'J' => 0xad, // lreturn
        b'F' => 0xae, // freturn
        b'D' => 0xaf, // dreturn
        b'L' | b'[' => 0xb0, // areturn
        _ => 0xac, // ireturn
    }
}

/// Appends `opcode` then a big-endian `u16` operand.
fn push_u16(code: &mut Vec<u8>, opcode: u8, operand: u16) {
    code.push(opcode);
    code.extend_from_slice(&operand.to_be_bytes());
}

/// Split `"()ret"` into (params-ignored, ret). Element accessors take no parameters.
fn split_return(descriptor: &str) -> ((), String) {
    let ret = descriptor.rsplit(')').next().unwrap_or(descriptor).to_string();
    ((), ret)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::jvm::class_file::ClassFile as ParsedClass;

    #[test]
    fn generates_a_parseable_annotation_class() {
        // Mimics @Deprecated: String since() default "", boolean forRemoval() default false.
        let elements = vec![
            Element {
                name: "since".to_string(),
                descriptor: "()Ljava/lang/String;".to_string(),
                value: ResolvedValue::Str("9".to_string()),
            },
            Element {
                name: "forRemoval".to_string(),
                descriptor: "()Z".to_string(),
                value: ResolvedValue::Bool(true),
            },
        ];
        let bytes = generate_annotation_class("Host$$Anno$0", "java/lang/Deprecated", &elements);
        let parsed = ParsedClass::from_bytes(&bytes).expect("spun annotation class must parse");
        assert_eq!(parsed.class_name(parsed.this_class), Some("Host$$Anno$0"));
        assert_eq!(parsed.interfaces.len(), 1);
        for m in ["<init>", "since", "forRemoval", "annotationType"] {
            assert!(
                parsed.methods.iter().any(|mi| parsed.utf8(mi.name_index) == Some(m)),
                "missing method {m}"
            );
        }
    }

    #[test]
    fn bakes_an_enum_and_an_array() {
        // e.g. @Retention: RetentionPolicy value(); and a String[] element.
        let elements = vec![
            Element {
                name: "value".to_string(),
                descriptor: "()Ljava/lang/annotation/RetentionPolicy;".to_string(),
                value: ResolvedValue::Enum {
                    type_descriptor: "Ljava/lang/annotation/RetentionPolicy;".to_string(),
                    const_name: "RUNTIME".to_string(),
                },
            },
            Element {
                name: "names".to_string(),
                descriptor: "()[Ljava/lang/String;".to_string(),
                value: ResolvedValue::Array(vec![
                    ResolvedValue::Str("a".to_string()),
                    ResolvedValue::Str("b".to_string()),
                ]),
            },
        ];
        let bytes = generate_annotation_class("Host$$Anno$1", "Marker", &elements);
        let parsed = ParsedClass::from_bytes(&bytes).expect("spun annotation class must parse");
        assert!(parsed.methods.iter().any(|mi| parsed.utf8(mi.name_index) == Some("value")));
        assert!(parsed.methods.iter().any(|mi| parsed.utf8(mi.name_index) == Some("names")));
    }
}
