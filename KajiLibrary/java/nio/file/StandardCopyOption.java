package java.nio.file;

// Las opciones estandar de `Files.copy` y `Files.move`.
//
// **KajiJDK honra `REPLACE_EXISTING` y rechaza las otras dos.** `COPY_ATTRIBUTES` pide llevarse las
// marcas de tiempo y los permisos, y no hay nativo que los lea ni los escriba; `ATOMIC_MOVE` pide
// una garantia que un mover hecho de copiar-y-borrar no puede dar. En los dos casos se levanta la
// excepcion que la spec ya prevee --`UnsupportedOperationException` y
// `AtomicMoveNotSupportedException`-- en vez de aceptarlas y no cumplirlas.
public enum StandardCopyOption implements CopyOption {

    /** Si el destino existe, pisarlo. */
    REPLACE_EXISTING,

    /** Copiar tambien los atributos. KajiJDK no la soporta. */
    COPY_ATTRIBUTES,

    /** Mover como una operacion atomica. KajiJDK no la soporta. */
    ATOMIC_MOVE
}
