package javax.sound.sampled;

import java.io.IOException;

/**
 * KajiLibrary's javax.sound.sampled.Clip -- audio cargado entero en memoria.
 *
 * <p>La diferencia con {@link SourceDataLine} es de modelo, no de calidad: aquella recibe audio en
 * tandas mientras suena, esta lo tiene todo antes de empezar.
 *
 * <p>Eso es lo que permite lo unico que aquella no puede: <b>saltar a una posicion</b> y
 * <b>repetir</b>. Un clip es lo correcto para un efecto de sonido corto que se repite; un flujo largo
 * va por la otra.
 *
 * <p>El costo es la memoria: un clip ocupa el audio entero descomprimido. Un minuto de estereo de 16
 * bits a 44100 Hz son diez megabytes.
 *
 * <h2>{@link #loop} y los puntos de repeticion</h2>
 *
 * <p>{@link #setLoopPoints} marca el pedazo que se repite, en cuadros. {@code loop(n)} lo repite
 * {@code n} veces mas, y {@link #LOOP_CONTINUOUSLY} para siempre.
 *
 * <p>El detalle que se olvida: {@code loop(0)} es valido y significa "no repitas". No es lo mismo que
 * no llamar a {@code loop}, porque igual arranca la reproduccion.
 *
 * <p>Para cortar una repeticion infinita hay que llamar {@link DataLine#stop}, o
 * {@code loop(0)} para que termine la vuelta actual y pare.
 */
public interface Clip extends DataLine {

    /** Repetir para siempre. */
    int LOOP_CONTINUOUSLY = -1;

    /**
     * Carga audio desde un arreglo de bytes.
     *
     * @param offset desde donde
     * @param bufferSize cuantos bytes; tiene que ser multiplo del cuadro
     * @throws LineUnavailableException si el recurso no esta disponible
     * @throws IllegalArgumentException si el formato no se soporta o el largo no es multiplo del
     *     cuadro
     * @throws IllegalStateException si ya estaba abierto
     */
    void open(AudioFormat format, byte[] data, int offset, int bufferSize)
        throws LineUnavailableException;

    /**
     * Carga audio desde un flujo, hasta el final.
     *
     * @throws LineUnavailableException si el recurso no esta disponible
     * @throws IOException si no se pudo leer
     * @throws IllegalArgumentException si el formato no se soporta
     * @throws IllegalStateException si ya estaba abierto
     */
    void open(AudioInputStream stream) throws LineUnavailableException, IOException;

    /** Cuantos cuadros tiene. */
    int getFrameLength();

    /** Cuanto dura, en microsegundos. */
    long getMicrosecondLength();

    /** Salta a ese cuadro. */
    void setFramePosition(int frames);

    /** Salta a ese microsegundo; se redondea al cuadro mas cercano. */
    void setMicrosecondPosition(long microseconds);

    /**
     * Marca el pedazo que se repite.
     *
     * @param end el ultimo cuadro del pedazo; -1 significa hasta el final
     * @throws IllegalArgumentException si los puntos no son validos
     */
    void setLoopPoints(int start, int end);

    /**
     * Repite el pedazo marcado.
     *
     * <p>Ver la nota de la clase: {@code loop(0)} reproduce sin repetir.
     *
     * @param count cuantas veces mas, o {@link #LOOP_CONTINUOUSLY}
     */
    void loop(int count);
}
