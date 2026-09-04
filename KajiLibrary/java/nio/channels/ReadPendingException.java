package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.ReadPendingException — Se pidio leer sobre un canal asincronico que ya tiene una lectura en curso.
 */
public class ReadPendingException extends IllegalStateException {

    private static final long serialVersionUID = 1000000020L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public ReadPendingException() {
        super();
    }
}
