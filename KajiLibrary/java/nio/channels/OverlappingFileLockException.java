package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.OverlappingFileLockException — Se pidio un candado sobre una region de archivo que **ya** esta candada por esta misma VM, o hay
 * otro pedido en curso sobre ella.
 *
 * <p>Es dentro de la misma VM a proposito: los candados de archivo son del proceso ante el sistema
 * operativo, asi que dos partes del mismo programa no se bloquean entre si --se pisan--. Esta
 * excepcion es lo que convierte ese pisarse silencioso en un error visible.
 */
public class OverlappingFileLockException extends IllegalStateException {

    private static final long serialVersionUID = 1000000019L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public OverlappingFileLockException() {
        super();
    }
}
