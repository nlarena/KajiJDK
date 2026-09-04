package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.NotYetConnectedException — Se leyo o escribio en un canal que todavia no se conecto.
 */
public class NotYetConnectedException extends IllegalStateException {

    private static final long serialVersionUID = 1000000018L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public NotYetConnectedException() {
        super();
    }
}
