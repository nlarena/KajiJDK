package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.MidiUnavailableException -- the device exists but cannot be used
 * now.
 *
 * <p>The MIDI equivalent of {@code javax.sound.sampled.LineUnavailableException}, and the same
 * distinction: it does not say the system does not support what is asked, it says that right now
 * the resource is taken.
 *
 * <p>With MIDI it happens more often than with audio: a physical MIDI port usually admits a single
 * program at a time.
 */
public class MidiUnavailableException extends Exception {

    private static final long serialVersionUID = 6093809578628944323L;

    /** Without detail. */
    public MidiUnavailableException() {
        super();
    }

    /** With a message. */
    public MidiUnavailableException(String message) {
        super(message);
    }
}
