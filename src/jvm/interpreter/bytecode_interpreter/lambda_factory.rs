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

use crate::javac::class_writer::{ClassFile, ConstantPool, FieldInfo, MethodInfo};

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
    // El *kind* del `MethodHandle` de la implementacion (JVMS §4.4.8). Decide con que opcode se
    // reenvia: `6` es `invokestatic`, `5` `invokevirtual`, `7` `invokespecial`, `9`
    // `invokeinterface`. Finding #552.
    impl_kind: u8,
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
    let (impl_params, impl_return) = split_descriptor(impl_descriptor);
    // **A donde va cada valor.** Un handle de instancia (kinds 5, 7 y 9) recibe el receptor primero
    // y el descriptor de la implementacion **no** lo incluye, asi que se antepone. Finding #552.
    let instancia = matches!(impl_kind, 5 | 7 | 9);
    let mut destinos: Vec<String> = Vec::new();
    if instancia {
        destinos.push(format!("L{impl_class};"));
    }
    destinos.extend(impl_params.iter().cloned());
    // **De donde sale cada uno**: primero las capturas, despues los argumentos del SAM.
    let fuentes: Vec<String> = captures.iter().chain(sam_params.iter()).cloned().collect();
    let adaptable = fuentes.len() == destinos.len();

    let mut sam = Vec::new();
    let mut depth = 0u16;
    let mut arg_slot = 1u8; // despues de `this`
    // Una **referencia a constructor** (`ArrayList::new`, kind 8) aloca primero y deja una copia
    // debajo de los argumentos: el `<init>` no devuelve nada, asi que lo que queda en la pila para
    // el `areturn` es esa copia. Finding #552.
    if impl_kind == 8 {
        let c = cf.pool.class(impl_class);
        push_u16(&mut sam, 0xbb, c); // new
        sam.push(0x59); // dup
        depth += 2;
    }
    for (i, fuente) in fuentes.iter().enumerate() {
        if i < field_refs.len() {
            sam.push(0x2a); // aload_0
            push_u16(&mut sam, 0xb4, field_refs[i]); // getfield cap<i>
        } else {
            sam.push(load_opcode(fuente));
            sam.push(arg_slot);
            arg_slot += slot_width(fuente);
        }
        depth += slot_width(fuente) as u16;
        // El descriptor del SAM esta **borrado** (`Function.apply` dice `(Object)Object`), asi que
        // un primitivo que la implementacion pide hay que desempaquetarlo, y uno que entrega hay que
        // empaquetarlo. Sin esto, `BiFunction<Integer, Long, Integer> f = C::suma` con
        // `suma(int, long)` reventaba con *"conversion: expected a long, found Reference"*.
        if adaptable {
            adaptar(&mut sam, &mut cf.pool, fuente, &destinos[i]);
        }
    }
    // **El reenvio va con el opcode que pida el kind.** Antes era siempre `invokestatic`, y un
    // `assert` cortaba lo demas: no corria aca **ninguna** referencia a un metodo de instancia
    // (`String::length`, kind 5) ni **ninguna** lambda que tocara `this` (kind 7).
    match impl_kind {
        5 => push_u16(&mut sam, 0xb6, impl_ref), // invokevirtual
        7 | 8 => push_u16(&mut sam, 0xb7, impl_ref), // invokespecial (el 8 es sobre el `new` de arriba)
        9 => {
            // `invokeinterface` lleva dos bytes mas: la cuenta de argumentos --el receptor
            // incluido-- y un cero (JVMS §6.5).
            let cuenta: u8 = 1 + impl_params.iter().map(|d| slot_width(d)).sum::<u8>();
            push_u16(&mut sam, 0xb9, impl_ref);
            sam.push(cuenta);
            sam.push(0);
        }
        _ => push_u16(&mut sam, 0xb8, impl_ref), // invokestatic
    }
    // Y el retorno, por lo mismo: `String::length` devuelve `int` donde `Function.apply` declara
    // `Object`. Sin empaquetarlo, el `checkcast Integer` del **llamador** encontraba un `int`.
    adaptar(&mut sam, &mut cf.pool, &impl_return, &sam_return);
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
/// Escribe la conversion que lleva un valor de `fuente` a `destino`, si hace falta (§5.3): el
/// **boxeo** de un primitivo que va a una referencia, y el **desempaquetado** --con su `checkcast`
/// previo, porque el descriptor del SAM esta borrado-- de una referencia que va a un primitivo.
///
/// Entre dos referencias no se escribe nada: esta clase no se verifica y el despacho es por el
/// objeto real. Entre dos primitivos tampoco: el emisor ya los dejo del ancho que corresponde.
/// Finding #552.
fn adaptar(code: &mut Vec<u8>, pool: &mut ConstantPool, fuente: &str, destino: &str) {
    let primitivo = |d: &str| !d.starts_with('L') && !d.starts_with('[');
    match (primitivo(fuente), primitivo(destino)) {
        (true, false) => {
            if let Some((envoltorio, _)) = envoltorio_de(fuente) {
                let desc = format!("({fuente})L{envoltorio};");
                let m = pool.methodref(envoltorio, "valueOf", &desc);
                push_u16(code, 0xb8, m); // invokestatic Envoltorio.valueOf
            }
        }
        (false, true) => {
            if let Some((envoltorio, metodo)) = envoltorio_de(destino) {
                let c = pool.class(envoltorio);
                push_u16(code, 0xc0, c); // checkcast Envoltorio
                let desc = format!("(){destino}");
                let m = pool.methodref(envoltorio, metodo, &desc);
                push_u16(code, 0xb6, m); // invokevirtual Envoltorio.xxxValue
            }
        }
        _ => {}
    }
}

/// El envoltorio de un primitivo y el nombre de su metodo de desempaquetado.
fn envoltorio_de(descriptor: &str) -> Option<(&'static str, &'static str)> {
    Some(match descriptor {
        "I" => ("java/lang/Integer", "intValue"),
        "J" => ("java/lang/Long", "longValue"),
        "D" => ("java/lang/Double", "doubleValue"),
        "F" => ("java/lang/Float", "floatValue"),
        "S" => ("java/lang/Short", "shortValue"),
        "B" => ("java/lang/Byte", "byteValue"),
        "C" => ("java/lang/Character", "charValue"),
        "Z" => ("java/lang/Boolean", "booleanValue"),
        _ => return None,
    })
}

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
            6, // invokestatic: reenvia al estatico `Host.lambda$0`
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
