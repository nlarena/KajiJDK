package java.nio.file.attribute;

// Los cuatro bits heredados de DOS que Windows sigue guardando por archivo.
//
// KajiJDK no los puede leer: `stat` de `jdk.internal.io.Fs` devuelve existe/archivo/directorio/
// lectura/escritura y nada mas. La interfaz existe --es el tipo que devuelve
// `DosFileAttributeView.readAttributes()`-- pero no hay implementacion.
public interface DosFileAttributes extends BasicFileAttributes {

    /** Si esta marcado de solo lectura. */
    boolean isReadOnly();

    /** Si esta oculto. */
    boolean isHidden();

    /** Si tiene puesto el bit de archivado. */
    boolean isArchive();

    /** Si es un archivo de sistema. */
    boolean isSystem();
}
