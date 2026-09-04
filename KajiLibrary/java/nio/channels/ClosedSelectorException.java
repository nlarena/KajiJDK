package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.ClosedSelectorException — Se uso un selector cerrado.
 */
public class ClosedSelectorException extends IllegalStateException {

    private static final long serialVersionUID = 1000000007L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public ClosedSelectorException() {
        super();
    }
}
