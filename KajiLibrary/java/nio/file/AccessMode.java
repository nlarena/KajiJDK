package java.nio.file;

// Los modos que se le pueden preguntar a `FileSystemProvider.checkAccess`.
//
// **KajiJDK contesta dos de los tres.** `stat` de `jdk.internal.io.Fs` trae las banderas de lectura y
// escritura pero **no** la de ejecucion: no hay bit de ejecucion en la respuesta del nativo.
// `checkAccess(EXECUTE)` levanta `UnsupportedOperationException` y `Files.isExecutable` directamente
// no existe -- devolver `false` seria decir "no se puede ejecutar" cuando la verdad es "no se".
public enum AccessMode {

    /** Se puede leer. */
    READ,

    /** Se puede escribir. */
    WRITE,

    /** Se puede ejecutar. */
    EXECUTE
}
