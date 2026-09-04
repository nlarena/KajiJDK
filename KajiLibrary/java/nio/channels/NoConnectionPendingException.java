package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.NoConnectionPendingException — Se quiso terminar de conectar un canal que nunca empezo a conectarse.
 */
public class NoConnectionPendingException extends IllegalStateException {

    private static final long serialVersionUID = 1000000014L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public NoConnectionPendingException() {
        super();
    }
}
