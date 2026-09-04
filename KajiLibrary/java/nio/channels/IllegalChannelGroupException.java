package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.IllegalChannelGroupException — Se combinaron un canal y un grupo que no pertenecen al mismo proveedor.
 *
 * <p>Es `IllegalArgumentException` y no de estado porque el error esta en **el argumento**: el
 * grupo que se paso no sirve para este canal, y no hay momento en que sirviera.
 */
public class IllegalChannelGroupException extends IllegalArgumentException {

    private static final long serialVersionUID = 1000000011L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public IllegalChannelGroupException() {
        super();
    }
}
