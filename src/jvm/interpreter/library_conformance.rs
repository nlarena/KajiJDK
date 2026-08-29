//! Pruebas de que **la biblioteca que la VM carga** cumple las reglas del lenguaje.
//!
//! Es una categoría propia y vale separarla. Los tests de `gc.rs` preguntan si el intérprete hace
//! lo que el bytecode dice; éstos preguntan algo distinto: si `KajiLibrary` —que es lo que la VM
//! carga de verdad, porque gana en el bootclasspath— se comporta como la JLS obliga a que se
//! comporte una biblioteca estándar. Un defecto de esta clase no rompe ninguna instrucción: rompe
//! una **garantía**, y por eso pasa desapercibido hasta que alguien la usa.
//!
//! El caso que motivó el módulo (#275) es exactamente esa forma: las cachés de wrapper habían
//! estado bien —vivían en `boot/java/lang/Integer.class`— y se perdieron cuando el `Integer` de
//! KajiLibrary tomó su lugar. Nada lo notó, porque **ninguna prueba comparaba dos boxeos con
//! `==`**. El arreglo sin la prueba se vuelve a perder por el mismo camino.
//!
//! Cada prueba corre un probe de `java/` con `KajiLibrary` **primero** en el bootclasspath, que es
//! el orden real de `run-headless`, y compara contra un número que el JDK real produce con la
//! misma fuente. Ese cotejo es lo que hace al probe un oráculo y no una opinión.

#[cfg(test)]
mod tests {
    use crate::jvm::class_file::ClassFile;
    use crate::jvm::interpreter::bytecode_interpreter::execute;
    use crate::jvm::interpreter::frame::{Frame, Value};
    use crate::jvm::interpreter::metaspace::MetaspaceService;
    use std::path::PathBuf;

    /// Corre `run()I` de un probe de `java/` con **KajiLibrary primero** en el bootclasspath.
    ///
    /// El orden importa y es el del `run-headless`: KajiLibrary es la biblioteca que se desarrolla
    /// y la que tiene que correr, con `boot/` sólo de relleno para lo que todavía vive nada más
    /// que ahí. Un probe de conformidad que se midiera contra `boot/` mediría la biblioteca
    /// equivocada — que es, literalmente, el finding #246.
    fn run_probe(class_file: &str) -> i32 {
        let mut metaspace = MetaspaceService::new(
            vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")],
            vec![PathBuf::from("java")],
        );
        let class = ClassFile::from_path(class_file).expect("load class");
        let name = class.class_name(class.this_class).unwrap().to_string();
        metaspace.add(name.clone(), class);
        let entry = metaspace.resolve_method(&name, "run", "()I").expect("run()");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());
        match execute(metaspace, frame) {
            Some(Value::Int(v)) => v,
            other => panic!("se esperaba un int, salió {other:?}"),
        }
    }

    /// #275 — JLS §5.1.7: boxear un valor entre −128 y 127 devuelve la **misma** referencia.
    ///
    /// Dieciocho propiedades, un bit cada una, y las que piden `!=` valen tanto como las que piden
    /// `==`: una caché que cubriera *todos* los valores haría que el código que compara con `==`
    /// pareciera andar acá y se rompiera contra un JDK real. Se comprueban los dos caminos (por
    /// `valueOf` y por autoboxing), los cuatro bordes exactos (−128, 127, −129, 128) y los seis
    /// wrappers, incluido `Byte` —cuyo rango entero entra en la caché, así que `valueOf` nunca
    /// aloca— y `Boolean`, cuyas dos instancias son constantes.
    ///
    /// **262143 es el número que imprime el JDK 21 corriendo la misma fuente.** Eso es lo que hace
    /// del probe un oráculo: no se eligió el valor que da hoy, se eligió el que tiene que dar.
    /// **`#[ignore]` a propósito, y por una razón que hay que sacar cuando corresponda:** el
    /// arreglo de las cachés vive **sin commitear** en el árbol de trabajo (`Integer.java`,
    /// `Long.java`, `Short.java`, `Byte.java`, `Character.java`), así que sobre `HEAD` este test
    /// falla — se comprobó, y falla, que es justamente lo que lo hace una guarda de verdad y no
    /// una que pasa por casualidad. Con el arreglo aplicado da 262143.
    ///
    /// Se deja escrito acá en vez de esperar, porque el finding que lo motiva es *"se arregló una
    /// vez y se perdió porque nadie lo probaba"*: la prueba que llega después del arreglo llega
    /// tarde por definición. **Sacar el `#[ignore]` cuando los cinco wrappers estén commiteados.**
    #[ignore = "el arreglo de #275 todavia no esta commiteado; ver el comentario"]
    #[test]
    fn the_wrapper_caches_the_language_requires_are_there() {
        assert_eq!(run_probe("java/WrapCacheProbe.class"), 262143);
    }
}
