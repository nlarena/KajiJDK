package java.lang.classfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// La fábrica de la API: de acá salen los `ClassModel` y acá viven las constantes del formato.
//
// Lo que KajiLibrary implementa de esta interfaz es la **lectura**: `of`, `withOptions`, `parse` y
// las constantes. Lo que NO está, y por qué:
//
//   - `build`, `buildTo`, `buildModule`, `buildModuleTo` y `transformClass` — escriben un `.class`
//     nuevo. Escribir exige el paquete `java.lang.classfile.instruction` entero (una clase por forma
//     de instrucción) y la generación de `StackMapTable`; nada de eso está. Un `build` que devolviera
//     bytes sin mapa de pila produciría clases que la JVM real rechaza con `VerifyError`, que es
//     exactamente el tipo de mentira que este proyecto no admite.
//   - `verify` — devuelve la lista de errores de verificación del bytecode. Nuestro parseo valida la
//     ESTRUCTURA del archivo (§4.1 a §4.7) y falla al leer si está mal, pero no verifica el flujo de
//     tipos de §4.10. Devolver una lista vacía sería afirmar que la clase verifica, y no lo sabemos.
//   - `latestMajorVersion`/`latestMinorVersion` sí están: son datos, no comportamiento.
//
// Las opciones (`ClassFile.Option` y sus enums anidados) tampoco están, salvo la interfaz marcadora:
// todas gobiernan al escritor.
public interface ClassFile {

    /** Una instancia con las opciones por omisión. */
    public static ClassFile of() {
        return new jdk.internal.classfile.impl.ClassFileImpl();
    }

    /** Una instancia con estas opciones. */
    public static ClassFile of(Option... options) {
        return new jdk.internal.classfile.impl.ClassFileImpl().withOptions(options);
    }

    /** La misma instancia con estas opciones encima. */
    ClassFile withOptions(Option... options);

    /**
     * Lee un `.class`. Valida el magic, las versiones, el pool entero y la estructura de campos,
     * métodos y atributos; si algo no cierra tira `IllegalArgumentException` (o la
     * `ConstantPoolException` que hereda de ella) en vez de devolver un modelo a medio armar.
     */
    ClassModel parse(byte[] bytes);

    /** Lee el `.class` que está en `path`. */
    default ClassModel parse(Path path) throws IOException {
        return parse(Files.readAllBytes(path));
    }

    /** La versión mayor más nueva que esta implementación conoce. */
    public static int latestMajorVersion() {
        return JAVA_25_VERSION;
    }

    /** La versión menor más nueva que esta implementación conoce. */
    public static int latestMinorVersion() {
        return 0;
    }

    /**
     * Una opción de lectura o de escritura. Es una interfaz marcadora; en el JDK sus
     * implementaciones son los enums anidados de `ClassFile`, que acá no están porque todos
     * gobiernan al escritor.
     */
    public interface Option {
    }

    /** `0xCAFEBABE`, los primeros cuatro bytes de todo `.class` (JVMS §4.1). */
    public static final int MAGIC_NUMBER = 0xCAFEBABE;

    /** `ACC_PUBLIC`. */
    public static final int ACC_PUBLIC = 0x0001;
    /** `ACC_PRIVATE`. */
    public static final int ACC_PRIVATE = 0x0002;
    /** `ACC_PROTECTED`. */
    public static final int ACC_PROTECTED = 0x0004;
    /** `ACC_STATIC`. */
    public static final int ACC_STATIC = 0x0008;
    /** `ACC_FINAL`. */
    public static final int ACC_FINAL = 0x0010;
    /** `ACC_SUPER` en una clase. */
    public static final int ACC_SUPER = 0x0020;
    /** `ACC_OPEN` en un módulo: el mismo bit que `ACC_SUPER`. */
    public static final int ACC_OPEN = 0x0020;
    /** `ACC_TRANSITIVE` en un `requires`: el mismo bit otra vez. */
    public static final int ACC_TRANSITIVE = 0x0020;
    /** `ACC_SYNCHRONIZED` en un método: el mismo bit otra vez. */
    public static final int ACC_SYNCHRONIZED = 0x0020;
    /** `ACC_STATIC_PHASE` en un `requires`. */
    public static final int ACC_STATIC_PHASE = 0x0040;
    /** `ACC_VOLATILE` en un campo. */
    public static final int ACC_VOLATILE = 0x0040;
    /** `ACC_BRIDGE` en un método: el mismo bit que `ACC_VOLATILE`. */
    public static final int ACC_BRIDGE = 0x0040;
    /** `ACC_TRANSIENT` en un campo. */
    public static final int ACC_TRANSIENT = 0x0080;
    /** `ACC_VARARGS` en un método: el mismo bit que `ACC_TRANSIENT`. */
    public static final int ACC_VARARGS = 0x0080;
    /** `ACC_NATIVE`. */
    public static final int ACC_NATIVE = 0x0100;
    /** `ACC_INTERFACE`. */
    public static final int ACC_INTERFACE = 0x0200;
    /** `ACC_ABSTRACT`. */
    public static final int ACC_ABSTRACT = 0x0400;
    /** `ACC_STRICT`. */
    public static final int ACC_STRICT = 0x0800;
    /** `ACC_SYNTHETIC`. */
    public static final int ACC_SYNTHETIC = 0x1000;
    /** `ACC_ANNOTATION`. */
    public static final int ACC_ANNOTATION = 0x2000;
    /** `ACC_ENUM`. */
    public static final int ACC_ENUM = 0x4000;
    /** `ACC_MANDATED`. */
    public static final int ACC_MANDATED = 0x8000;
    /** `ACC_MODULE`: el mismo bit que `ACC_MANDATED`, pero sólo válido en una clase. */
    public static final int ACC_MODULE = 0x8000;

    /** Versión mayor de Java 1.0/1.1. */
    public static final int JAVA_1_VERSION = 45;
    /** Versión mayor de Java 1.2. */
    public static final int JAVA_2_VERSION = 46;
    /** Versión mayor de Java 1.3. */
    public static final int JAVA_3_VERSION = 47;
    /** Versión mayor de Java 1.4. */
    public static final int JAVA_4_VERSION = 48;
    /** Versión mayor de Java 5. */
    public static final int JAVA_5_VERSION = 49;
    /** Versión mayor de Java 6. */
    public static final int JAVA_6_VERSION = 50;
    /** Versión mayor de Java 7. */
    public static final int JAVA_7_VERSION = 51;
    /** Versión mayor de Java 8. */
    public static final int JAVA_8_VERSION = 52;
    /** Versión mayor de Java 9. */
    public static final int JAVA_9_VERSION = 53;
    /** Versión mayor de Java 10. */
    public static final int JAVA_10_VERSION = 54;
    /** Versión mayor de Java 11. */
    public static final int JAVA_11_VERSION = 55;
    /** Versión mayor de Java 12. */
    public static final int JAVA_12_VERSION = 56;
    /** Versión mayor de Java 13. */
    public static final int JAVA_13_VERSION = 57;
    /** Versión mayor de Java 14. */
    public static final int JAVA_14_VERSION = 58;
    /** Versión mayor de Java 15. */
    public static final int JAVA_15_VERSION = 59;
    /** Versión mayor de Java 16. */
    public static final int JAVA_16_VERSION = 60;
    /** Versión mayor de Java 17. */
    public static final int JAVA_17_VERSION = 61;
    /** Versión mayor de Java 18. */
    public static final int JAVA_18_VERSION = 62;
    /** Versión mayor de Java 19. */
    public static final int JAVA_19_VERSION = 63;
    /** Versión mayor de Java 20. */
    public static final int JAVA_20_VERSION = 64;
    /** Versión mayor de Java 21. */
    public static final int JAVA_21_VERSION = 65;
    /** Versión mayor de Java 22. */
    public static final int JAVA_22_VERSION = 66;
    /** Versión mayor de Java 23. */
    public static final int JAVA_23_VERSION = 67;
    /** Versión mayor de Java 24. */
    public static final int JAVA_24_VERSION = 68;
    /** Versión mayor de Java 25. */
    public static final int JAVA_25_VERSION = 69;

    /** El `minor_version` que marca una clase de vista previa: `0xFFFF`. */
    public static final int PREVIEW_MINOR_VERSION = 0xFFFF;
}
