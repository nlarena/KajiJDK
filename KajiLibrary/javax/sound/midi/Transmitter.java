package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.Transmitter -- produce mensajes MIDI y se los da a un
 * {@link Receiver}.
 *
 * <p>El otro extremo: un teclado, un puerto de entrada, un secuenciador reproduciendo. No tiene metodo
 * para "leer" -- se le conecta un receptor y el transmisor empuja.
 *
 * <p>Es empuje y no tiro porque MIDI es en tiempo real: si el programa tuviera que preguntar, la
 * latencia dependeria de cada cuanto pregunta.
 *
 * <p>Un transmisor tiene <b>un solo</b> receptor. {@link #setReceiver} reemplaza al anterior, no
 * agrega. Para repartir a varios hay que poner un receptor propio que reenvie.
 *
 * <p>Es {@link AutoCloseable}, y hay que cerrarlo.
 */
public interface Transmitter extends AutoCloseable {

    /** A quien entregarle. Reemplaza al anterior; ver la nota de la clase. */
    void setReceiver(Receiver receiver);

    /** A quien le entrega, o null. */
    Receiver getReceiver();

    /** Cierra. Se puede llamar mas de una vez. */
    void close();
}
