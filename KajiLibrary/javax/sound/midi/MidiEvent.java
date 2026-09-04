package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.MidiEvent -- un mensaje MIDI con su momento.
 *
 * <p>Un {@link MidiMessage} mas un instante en <b>pulsos</b>. Es lo que va adentro de un {@link Track};
 * un mensaje suelto no tiene tiempo, y por eso no se puede secuenciar solo.
 *
 * <p>El pulso es una unidad relativa: cuanto dura depende de la resolucion de la {@link Sequence} y del
 * tempo actual. Ver {@link Sequence} sobre las dos formas de contar.
 *
 * <p>El mensaje no se puede cambiar; el pulso si. Es lo que permite mover un evento en el tiempo sin
 * reconstruirlo -- aunque un {@link Track} ordena por pulso, asi que cambiarlo despues de agregarlo
 * deja la pista desordenada.
 */
public class MidiEvent {

    /** Que mensaje. */
    private final MidiMessage message;

    /** En que pulso. */
    private long tick;

    /**
     * @param message el mensaje
     * @param tick en que pulso
     */
    public MidiEvent(MidiMessage message, long tick) {
        this.message = message;
        this.tick = tick;
    }

    /** El mensaje. */
    public MidiMessage getMessage() {
        return this.message;
    }

    /** Lo mueve en el tiempo. Ver la nota de la clase. */
    public void setTick(long tick) {
        this.tick = tick;
    }

    /** En que pulso. */
    public long getTick() {
        return this.tick;
    }
}
