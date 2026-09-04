package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.FileLockInterruptionException — El hilo fue interrumpido mientras esperaba adquirir un candado de archivo.
 *
 * <p>A diferencia de `ClosedByInterruptException`, el canal **no** se cierra: esperar un candado
 * no deja nada a medias, asi que alcanza con abandonar la espera.
 */
public class FileLockInterruptionException extends java.io.IOException {

    private static final long serialVersionUID = 1000000009L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public FileLockInterruptionException() {
        super();
    }
}
