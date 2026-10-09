package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.MidiEvent -- a MIDI message with its moment.
 *
 * <p>A {@link MidiMessage} plus an instant in <b>ticks</b>. It is what goes inside a {@link Track};
 * a loose message has no time, and that is why it cannot be sequenced on its own.
 *
 * <p>The tick is a relative unit: how long it lasts depends on the {@link Sequence}'s resolution
 * and on the current tempo. See {@link Sequence} on the two ways of counting.
 *
 * <p>The message cannot be changed; the tick can. It is what allows moving an event in time without
 * rebuilding it -- although a {@link Track} sorts by tick, so changing it after adding it leaves
 * the track out of order.
 */
public class MidiEvent {

    /** Which message. */
    private final MidiMessage message;

    /** At which tick. */
    private long tick;

    /**
     * @param message the message
     * @param tick at which tick
     */
    public MidiEvent(MidiMessage message, long tick) {
        this.message = message;
        this.tick = tick;
    }

    /** The message. */
    public MidiMessage getMessage() {
        return this.message;
    }

    /** Moves it in time. See the class note. */
    public void setTick(long tick) {
        this.tick = tick;
    }

    /** At which tick. */
    public long getTick() {
        return this.tick;
    }
}
