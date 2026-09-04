package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.IllegalSelectorException — Se quiso registrar un canal en un selector de otro proveedor.
 */
public class IllegalSelectorException extends IllegalArgumentException {

    private static final long serialVersionUID = 1000000012L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public IllegalSelectorException() {
        super();
    }
}
