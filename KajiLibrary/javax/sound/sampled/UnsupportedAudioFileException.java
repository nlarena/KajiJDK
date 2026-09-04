package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.UnsupportedAudioFileException -- ese archivo no se puede leer.
 *
 * <p>Ningun lector registrado reconocio el formato. No dice que el archivo este roto: dice que nadie
 * sabe leerlo.
 *
 * <p>La diferencia con {@link java.io.IOException} importa al diagnosticar: aquella significa que no
 * se pudo leer el archivo, esta que se leyo y no se entendio.
 */
public class UnsupportedAudioFileException extends Exception {

    private static final long serialVersionUID = -139127412623160368L;

    /** Sin detalle. */
    public UnsupportedAudioFileException() {
        super();
    }

    /** Con mensaje. */
    public UnsupportedAudioFileException(String message) {
        super(message);
    }
}
