package java.nio.file;

// Las opciones estandar para abrir un archivo.
//
// **Cuales entiende KajiJDK.** El modelo de archivo de esta VM es "todo de una": `Fs.readAllBytes`
// y `Fs.writeAllBytes(path, bytes, append)`. Sobre eso, `Files.newOutputStream` y compania honran
// `READ`, `WRITE`, `APPEND`, `CREATE`, `CREATE_NEW` y `TRUNCATE_EXISTING`, que son las que se
// pueden expresar con ese unico parametro `append` mas un `stat` previo.
//
// Las otras cuatro --`DELETE_ON_CLOSE`, `SPARSE`, `SYNC`, `DSYNC`-- **no se pueden honrar y se
// rechazan** con `UnsupportedOperationException` en vez de ignorarse: `SYNC` sin sincronizar de
// verdad es exactamente la clase de promesa falsa que hace perder datos, y aceptarla en silencio
// seria peor que no ofrecerla. Las constantes existen igual porque son parte del enum.
public enum StandardOpenOption implements OpenOption {

    /** Abrir para leer. */
    READ,

    /** Abrir para escribir. */
    WRITE,

    /** Escribir siempre al final de lo que ya hay. */
    APPEND,

    /** Si ya existe y se abre para escribir, dejarlo en cero bytes. */
    TRUNCATE_EXISTING,

    /** Crearlo si no existe. */
    CREATE,

    /** Crearlo, y fallar si ya existia. */
    CREATE_NEW,

    /** Borrarlo al cerrar. KajiJDK no la soporta. */
    DELETE_ON_CLOSE,

    /** Pedirle al sistema que lo guarde disperso. KajiJDK no la soporta. */
    SPARSE,

    /** Sincronizar contenido y metadatos con el disco en cada escritura. KajiJDK no la soporta. */
    SYNC,

    /** Sincronizar solo el contenido. KajiJDK no la soporta. */
    DSYNC
}
