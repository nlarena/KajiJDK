package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.TargetDataLine -- una linea de la que se lee audio.
 *
 * <p>La <b>entrada</b>: el microfono, la linea de captura. Se llama "destino" porque es el destino de
 * los datos <b>del mezclador</b>; ver {@link SourceDataLine} sobre esta nomenclatura al reves.
 *
 * <p>Hay que llamar {@link DataLine#start} despues de abrir: abrir reserva el dispositivo, arrancar
 * empieza a llenar el bufer. Una linea abierta y no arrancada no captura nada, y es el error mas comun
 * al grabar por primera vez.
 *
 * <p>Y hay que leer a tiempo: el bufer es circular y se pisa. Lo que no se lea antes de que de la
 * vuelta se pierde, sin aviso.
 */
public interface TargetDataLine extends DataLine {

    /**
     * Abre con ese formato y ese tamano de bufer.
     *
     * <p>Aca el bufer decide cuanto se puede tardar en leer sin perder audio; ver la nota de la clase.
     *
     * @throws LineUnavailableException si el recurso no esta disponible
     * @throws IllegalArgumentException si no soporta ese formato
     * @throws IllegalStateException si ya estaba abierta
     */
    void open(AudioFormat format, int bufferSize) throws LineUnavailableException;

    /** Idem, con el bufer que el dispositivo prefiera. */
    void open(AudioFormat format) throws LineUnavailableException;

    /**
     * Lee audio capturado. Bloquea hasta juntar {@code len} bytes.
     *
     * @param len tiene que ser multiplo del tamano de cuadro
     * @return cuantos bytes se leyeron
     * @throws IllegalArgumentException si el largo no es multiplo del cuadro
     */
    int read(byte[] b, int off, int len);
}
