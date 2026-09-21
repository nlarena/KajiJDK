package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.Receiver -- receives MIDI messages.
 *
 * <p>A single useful method. Anything that consumes MIDI --a synthesizer, an output port, a
 * recorder-- is a receiver.
 *
 * <h2>The timestamp</h2>
 *
 * <p>The second argument of {@link #send} is microseconds <b>by the device's clock</b>, not since
 * the epoch. It serves to deliver a message ahead of time and have it sound exactly when it should,
 * instead of depending on when the thread gets round to sending it.
 *
 * <p>-1 means "now": without a time, it is processed as soon as it arrives.
 *
 * <p>Not all receivers honour the stamp. The standard does not require it, and many ignore it.
 *
 * <p>It is {@link AutoCloseable}, and it has to be closed: an open receiver keeps its device taken.
 */
public interface Receiver extends AutoCloseable {

    /**
     * Delivers a message.
     *
     * @param timeStamp microseconds of the device's clock, or -1 for "now"
     * @throws IllegalStateException if it was already closed
     */
    void send(MidiMessage message, long timeStamp);

    /** Closes. It can be called more than once. */
    void close();
}
