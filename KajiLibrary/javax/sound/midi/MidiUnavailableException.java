package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.MidiUnavailableException -- el dispositivo existe pero no se puede
 * usar ahora.
 *
 * <p>El equivalente MIDI de {@code javax.sound.sampled.LineUnavailableException}, y la misma
 * distincion: no dice que el sistema no soporte lo que se pide, dice que en este momento el recurso
 * esta tomado.
 *
 * <p>Con MIDI pasa mas seguido que con audio: un puerto MIDI fisico suele admitir un solo programa a
 * la vez.
 */
public class MidiUnavailableException extends Exception {

    private static final long serialVersionUID = 6093809578628944323L;

    /** Sin detalle. */
    public MidiUnavailableException() {
        super();
    }

    /** Con mensaje. */
    public MidiUnavailableException(String message) {
        super(message);
    }
}
