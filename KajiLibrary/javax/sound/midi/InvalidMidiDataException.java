package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.InvalidMidiDataException -- those bytes are not valid MIDI.
 *
 * <p>The constructors and {@code setMessage}s of the messages throw it, and so do the file readers.
 *
 * <p>It is checked, and so building a MIDI message forces a {@code try}. It is awkward and rightly
 * so: an invalid status byte silently accepted turns into a device that hangs or a file nobody else
 * can read.
 */
public class InvalidMidiDataException extends Exception {

    private static final long serialVersionUID = 2780771756789932067L;

    /** Without detail. */
    public InvalidMidiDataException() {
        super();
    }

    /** With a message. */
    public InvalidMidiDataException(String message) {
        super(message);
    }
}
