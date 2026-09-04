package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.WritePendingException — Se pidio escribir sobre un canal asincronico que ya tiene una escritura en curso.
 */
public class WritePendingException extends IllegalStateException {

    private static final long serialVersionUID = 1000000024L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public WritePendingException() {
        super();
    }
}
