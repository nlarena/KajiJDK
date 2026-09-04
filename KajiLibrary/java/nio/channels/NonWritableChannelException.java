package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.NonWritableChannelException — Se escribio en un canal que no se abrio para escritura.
 */
public class NonWritableChannelException extends IllegalStateException {

    private static final long serialVersionUID = 1000000016L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public NonWritableChannelException() {
        super();
    }
}
