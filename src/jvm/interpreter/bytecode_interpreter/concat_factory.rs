//! Spins the class a `StringConcatFactory` call site produces — **la misma jugada que
//! [`lambda_factory`][super::lambda_factory] hace para los lambdas**, aplicada a la concatenación.
//!
//! # Por qué existe
//!
//! `makeConcatWithConstants` se resolvía calculando en Rust: se renderizaba cada argumento y se
//! devolvía un `String` nativo. Eso funciona y es **rápido interpretado**, pero deja al opcode sin
//! bytecode — y sin bytecode no hay nada que el JIT pueda compilar. Como la concatenación es el
//! `invokedynamic` más común y lejos (5897 sitios contra 426 de `metafactory` en este corpus), ese
//! atajo era el techo del compilador sobre todo el código que concatena.
//!
//! Lo que se emite acá es la secuencia que `javac` emitía **antes de Java 9**:
//!
//! ```text
//! class <synthetic> {
//!     static String concat(<args>) {
//!         return new StringBuilder().append(…).append(…)….toString();
//!     }
//! }
//! ```
//!
//! Bytecode ordinario: una alocación y una cadena de llamadas, que es exactamente lo que este JIT
//! sabe compilar. El indy pasa de ser un intrínseco a ser **una llamada estática**.
//!
//! **El precio, dicho de frente**: interpretada, esta secuencia es más lenta que el intrínseco de
//! Rust — son N llamadas reales contra un bucle nativo. Se cambia velocidad del intérprete por
//! *compilabilidad*, que es la misma jugada del JDK real (ahí `StringConcatFactory` produce una
//! cadena de `MethodHandle` que HotSpot inlinea).

use crate::javac::class_writer::{ClassFile, MethodInfo};

const ACC_PUBLIC: u16 = 0x0001;
const ACC_STATIC: u16 = 0x0008;
const ACC_SUPER: u16 = 0x0020;
const ACC_FINAL: u16 = 0x0010;

const SB: &str = "java/lang/StringBuilder";

/// La sobrecarga de `StringBuilder.append` que le corresponde a un parámetro de descriptor `d`.
///
/// **Acá vive toda la semántica de tipos de esta clase.** `byte` y `short` van por `(I)` porque no
/// tienen sobrecarga propia y la promoción es la que manda la JLS; un `char` va por `(C)` y **no**
/// por `(I)`, que es la diferencia entre imprimir `'A'` y imprimir `65`; y cualquier referencia que
/// no sea `String` va por `(Ljava/lang/Object;)`, que es la que llama a `toString()` — incluidos los
/// arreglos, que no tienen sobrecarga propia.
fn append_descriptor(d: &str) -> &'static str {
    match d {
        "I" | "B" | "S" => "(I)Ljava/lang/StringBuilder;",
        "C" => "(C)Ljava/lang/StringBuilder;",
        "Z" => "(Z)Ljava/lang/StringBuilder;",
        "J" => "(J)Ljava/lang/StringBuilder;",
        "F" => "(F)Ljava/lang/StringBuilder;",
        "D" => "(D)Ljava/lang/StringBuilder;",
        "Ljava/lang/String;" => "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
        _ => "(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
    }
}

/// El opcode de carga y el ancho en slots de un parámetro.
fn load_of(d: &str) -> (u8, u8) {
    match d {
        "J" => (0x16, 2), // lload
        "D" => (0x18, 2), // dload
        "F" => (0x17, 1), // fload
        "I" | "B" | "S" | "C" | "Z" => (0x15, 1), // iload
        _ => (0x19, 1),   // aload
    }
}

fn push_u16(code: &mut Vec<u8>, opcode: u8, operand: u16) {
    code.push(opcode);
    code.extend_from_slice(&operand.to_be_bytes());
}

/// Genera los bytes de la clase que concatena. `recipe` es la receta de
/// `makeConcatWithConstants` (texto literal con marcadores), `params` los descriptores de los
/// argumentos del call site y `constants` las constantes que los marcadores empalman.
pub fn generate_concat_class(
    synthetic: &str,
    recipe: &str,
    params: &[String],
    constants: &[String],
    tag_arg: char,
    tag_const: char,
) -> Vec<u8> {
    let mut cf = ClassFile::new();
    cf.access_flags = ACC_PUBLIC | ACC_SUPER | ACC_FINAL;
    cf.this_class = cf.pool.class(synthetic);
    cf.super_class = cf.pool.class("java/lang/Object");

    let sb_class = cf.pool.class(SB);
    let sb_init = cf.pool.methodref(SB, "<init>", "()V");
    let sb_to_string = cf.pool.methodref(SB, "toString", "()Ljava/lang/String;");
    let append_string = cf.pool.methodref(SB, "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");

    let mut code = Vec::new();
    push_u16(&mut code, 0xbb, sb_class); // new StringBuilder
    code.push(0x59); // dup
    push_u16(&mut code, 0xb7, sb_init); // invokespecial <init>()V

    // Los slots de los parámetros, calculados de una vez: el método es `static`, así que el
    // primero es el 0.
    let mut slots = Vec::with_capacity(params.len());
    let mut next = 0u8;
    for d in params {
        slots.push(next);
        next += load_of(d).1;
    }

    // Un `ldc_w` + `append(String)` por corrida de texto literal, y una carga + `append` por
    // marcador. Se usa `ldc_w` (0x13) y no `ldc` (0x12) porque el índice del pool puede pasar de
    // 255 en una receta larga, y un truncado ahí cargaría otra constante.
    let mut literal = String::new();
    let flush = |code: &mut Vec<u8>, cf: &mut ClassFile, literal: &mut String| {
        if !literal.is_empty() {
            let idx = cf.pool.string(literal);
            push_u16(code, 0x13, idx);
            push_u16(code, 0xb6, append_string);
            literal.clear();
        }
    };
    let (mut next_arg, mut next_const) = (0usize, 0usize);
    for ch in recipe.chars() {
        if ch == tag_arg {
            flush(&mut code, &mut cf, &mut literal);
            let d = &params[next_arg];
            let (op, _) = load_of(d);
            code.push(op);
            code.push(slots[next_arg]);
            let m = cf.pool.methodref(SB, "append", append_descriptor(d));
            push_u16(&mut code, 0xb6, m);
            next_arg += 1;
        } else if ch == tag_const {
            flush(&mut code, &mut cf, &mut literal);
            let idx = cf.pool.string(&constants[next_const]);
            push_u16(&mut code, 0x13, idx);
            push_u16(&mut code, 0xb6, append_string);
            next_const += 1;
        } else {
            literal.push(ch);
        }
    }
    flush(&mut code, &mut cf, &mut literal);
    push_u16(&mut code, 0xb6, sb_to_string);
    code.push(0xb0); // areturn

    let descriptor = format!("({})Ljava/lang/String;", params.concat());
    let name_index = cf.pool.utf8("concat");
    let descriptor_index = cf.pool.utf8(&descriptor);
    cf.methods.push(MethodInfo {
        access_flags: ACC_PUBLIC | ACC_STATIC,
        name_index,
        descriptor_index,
        // El pico es el `StringBuilder` más el argumento más ancho encima: 1 + 2. El `new`/`dup`
        // usa 2. Cuatro cubre los dos con holgura, y la holgura acá no cuesta nada.
        max_stack: 4,
        max_locals: next.max(1) as u16,
        code,
        stack_map: None,
        exceptions: Vec::new(),
        ..Default::default()
    });
    cf.to_bytes()
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::jvm::class_file::ClassFile as Parsed;

    /// La clase que se emite tiene que **parsear con nuestro propio lector** — que es el mismo que
    /// va a cargarla en la VM. Un generador que produce bytes que sólo él entiende no sirve.
    #[test]
    fn la_clase_que_concatena_parsea_y_tiene_la_forma_esperada() {
        // `"a" + x + "b"` con `x:int` → receta "a\u{1}b", sin constantes empalmadas.
        let bytes = generate_concat_class(
            "Kaji$$concat0",
            "a\u{1}b",
            &["I".to_string()],
            &[],
            '\u{1}',
            '\u{2}',
        );
        let parsed = Parsed::from_bytes(&bytes).expect("la clase spun debe parsear");
        assert_eq!(parsed.class_name(parsed.this_class), Some("Kaji$$concat0"));
        assert_eq!(parsed.methods.len(), 1, "sólo `concat`");
        let m = &parsed.methods[0];
        assert_eq!(parsed.utf8(m.name_index), Some("concat"));
        assert_eq!(parsed.utf8(m.descriptor_index), Some("(I)Ljava/lang/String;"));
        assert!(m.is_static(), "el call site la llama con invokestatic");
        let code = parsed.member_code(m).expect("tiene cuerpo");
        // Empieza alocando y termina devolviendo una referencia.
        assert_eq!(code.code[0], 0xbb, "new");
        assert_eq!(*code.code.last().unwrap(), 0xb0, "areturn");
        // Y el argumento se carga con `iload`, no con `aload`: el descriptor manda.
        assert!(code.code.contains(&0x15), "iload del `int`");
    }

    /// **El caso que separa a `char` de `int`**, que es el error clásico de esta familia: un `char`
    /// tiene que ir por `append(C)` y no por `append(I)`, o `'A'` se imprime como `65`. Se afirma
    /// sobre el descriptor elegido y no sobre el resultado, porque el resultado sólo se ve
    /// ejecutando.
    #[test]
    fn cada_tipo_elige_su_sobrecarga_de_append() {
        assert_eq!(append_descriptor("C"), "(C)Ljava/lang/StringBuilder;");
        assert_eq!(append_descriptor("I"), "(I)Ljava/lang/StringBuilder;");
        // `byte` y `short` no tienen sobrecarga propia: promueven a `int`, como manda la JLS.
        assert_eq!(append_descriptor("B"), "(I)Ljava/lang/StringBuilder;");
        assert_eq!(append_descriptor("S"), "(I)Ljava/lang/StringBuilder;");
        assert_eq!(append_descriptor("Z"), "(Z)Ljava/lang/StringBuilder;");
        assert_eq!(append_descriptor("J"), "(J)Ljava/lang/StringBuilder;");
        // Un `String` va por su propia sobrecarga; cualquier otra referencia —arreglos incluidos—
        // por la de `Object`, que es la que termina llamando a `toString()`.
        assert_eq!(append_descriptor("Ljava/lang/String;"), "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
        assert_eq!(append_descriptor("Ljava/lang/Integer;"), "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
        assert_eq!(append_descriptor("[I"), "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
    }
}
