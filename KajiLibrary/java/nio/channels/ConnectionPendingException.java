package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.ConnectionPendingException — Se pidio conectar un canal que ya tiene una conexion en curso sin terminar.
 */
public class ConnectionPendingException extends IllegalStateException {

    private static final long serialVersionUID = 1000000008L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public ConnectionPendingException() {
        super();
    }
}
