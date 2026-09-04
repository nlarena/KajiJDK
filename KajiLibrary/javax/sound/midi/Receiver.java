package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.Receiver -- recibe mensajes MIDI.
 *
 * <p>Un solo metodo util. Cualquier cosa que consuma MIDI --un sintetizador, un puerto de salida, un
 * grabador-- es un receptor.
 *
 * <h2>La marca de tiempo</h2>
 *
 * <p>El segundo argumento de {@link #send} son microsegundos <b>segun el reloj del dispositivo</b>,
 * no desde el epoch. Sirve para entregar un mensaje con adelanto y que suene exactamente cuando
 * corresponde, en lugar de depender de cuando el hilo llegue a mandarlo.
 *
 * <p>-1 significa "ya": sin tiempo, se procesa apenas llega.
 *
 * <p>No todos los receptores respetan la marca. El estandar no obliga, y muchos la ignoran.
 *
 * <p>Es {@link AutoCloseable}, y hay que cerrarlo: un receptor abierto mantiene tomado su dispositivo.
 */
public interface Receiver extends AutoCloseable {

    /**
     * Entrega un mensaje.
     *
     * @param timeStamp microsegundos del reloj del dispositivo, o -1 para "ya"
     * @throws IllegalStateException si ya se cerro
     */
    void send(MidiMessage message, long timeStamp);

    /** Cierra. Se puede llamar mas de una vez. */
    void close();
}
