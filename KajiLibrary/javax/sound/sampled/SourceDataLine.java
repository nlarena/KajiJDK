package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.SourceDataLine -- una linea a la que se le escribe audio.
 *
 * <p>El nombre confunde y vale aclararlo de una: es la <b>salida</b>. Se llama "fuente" porque es la
 * fuente de datos <b>del mezclador</b>, no del programa. La entrada, la que captura, es
 * {@link TargetDataLine}.
 *
 * <p>{@link #write} bloquea hasta que entra todo lo que se le paso, y eso es lo que marca el ritmo: el
 * programa avanza a la velocidad a la que el dispositivo consume. Es la forma correcta de reproducir
 * sin acumular retraso ni cortar.
 *
 * <p>Escribe siempre cuadros enteros; un largo que no sea multiplo del cuadro lanza
 * {@link IllegalArgumentException}.
 */
public interface SourceDataLine extends DataLine {

    /**
     * Abre con ese formato y ese tamano de bufer.
     *
     * <p>El bufer es una <b>sugerencia</b>: el dispositivo puede darle otro. Un bufer chico baja la
     * latencia y sube el riesgo de cortes.
     *
     * @throws LineUnavailableException si el recurso no esta disponible
     * @throws IllegalArgumentException si no soporta ese formato
     * @throws IllegalStateException si ya estaba abierta
     */
    void open(AudioFormat format, int bufferSize) throws LineUnavailableException;

    /** Idem, con el bufer que el dispositivo prefiera. */
    void open(AudioFormat format) throws LineUnavailableException;

    /**
     * Escribe audio. Bloquea; ver la nota de la clase.
     *
     * @param len tiene que ser multiplo del tamano de cuadro
     * @return cuantos bytes se escribieron
     * @throws IllegalArgumentException si el largo no es multiplo del cuadro
     */
    int write(byte[] b, int off, int len);
}
