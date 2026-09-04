package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.AlreadyConnectedException — Se quiso conectar un canal que ya estaba conectado.
 */
public class AlreadyConnectedException extends IllegalStateException {

    private static final long serialVersionUID = 1000000002L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public AlreadyConnectedException() {
        super();
    }
}
