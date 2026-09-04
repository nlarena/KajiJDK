package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.InterruptedByTimeoutException — Una operacion asincronica se abandono porque vencio su plazo.
 *
 * <p>Dice que el plazo vencio, no que la operacion fallara: puede haber quedado a medias del otro
 * lado. Es la diferencia entre 'no paso' y 'no se si paso', y el que llama tiene que tratarla como
 * lo segundo.
 */
public class InterruptedByTimeoutException extends java.io.IOException {

    private static final long serialVersionUID = 1000000013L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public InterruptedByTimeoutException() {
        super();
    }
}
