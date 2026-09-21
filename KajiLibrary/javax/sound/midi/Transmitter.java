package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.Transmitter -- produces MIDI messages and gives them to a
 * {@link Receiver}.
 *
 * <p>The other end: a keyboard, an input port, a sequencer playing. It has no method to "read" -- a
 * receiver is connected to it and the transmitter pushes.
 *
 * <p>It is push and not pull because MIDI is real time: if the program had to ask, the latency
 * would depend on how often it asks.
 *
 * <p>A transmitter has <b>a single</b> receiver. {@link #setReceiver} replaces the previous one, it
 * does not add. To fan out to several, a receiver of one's own that forwards has to be put in.
 *
 * <p>It is {@link AutoCloseable}, and it has to be closed.
 */
public interface Transmitter extends AutoCloseable {

    /** Whom to deliver to. It replaces the previous one; see the class note. */
    void setReceiver(Receiver receiver);

    /** Whom it delivers to, or null. */
    Receiver getReceiver();

    /** Closes. It can be called more than once. */
    void close();
}
