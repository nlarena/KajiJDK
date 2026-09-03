package java.nio.file.attribute;

// El juego de atributos que todo sistema de archivos deberia poder contestar: tres marcas de
// tiempo, cuatro preguntas de tipo, el tamaño y una clave de identidad.
//
// **KajiJDK declara la interfaz pero no la implementa, y conviene decir por que.** De los nueve
// miembros, `stat` de `jdk.internal.io.Fs` solo puede contestar tres --`isRegularFile`,
// `isDirectory` y (con `size`) `size`--. Los otros seis no tienen de donde salir: no hay nativo que
// devuelva fecha de modificacion, de acceso ni de creacion, ni que distinga un enlace simbolico, ni
// que entregue el numero de inodo que seria `fileKey()`. Una implementacion tendria que devolver
// `FileTime.fromMillis(0)` y `false`, que son respuestas plausibles y **falsas** -- justo lo que
// esta biblioteca no hace. Asi que `Files.readAttributes` no existe, y esta interfaz queda como el
// tipo que la firma necesita nombrar.
public interface BasicFileAttributes {

    /** La ultima vez que se modifico el contenido. */
    FileTime lastModifiedTime();

    /** La ultima vez que se leyo. */
    FileTime lastAccessTime();

    /** Cuando se creo. */
    FileTime creationTime();

    /** Si es un archivo comun. */
    boolean isRegularFile();

    /** Si es un directorio. */
    boolean isDirectory();

    /** Si es un enlace simbolico. */
    boolean isSymbolicLink();

    /** Si no es ninguna de las tres cosas anteriores. */
    boolean isOther();

    /** El tamaño en bytes. */
    long size();

    /**
     * Una clave que identifica al archivo, o `null` si el sistema no puede darla.
     *
     * <p>`null` es una respuesta **valida** por spec, no un hueco: es lo que corresponde cuando no
     * hay algo como el inodo de POSIX.
     */
    Object fileKey();
}
