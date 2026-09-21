package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.UnsupportedAudioFileException -- that file cannot be read.
 *
 * <p>No registered reader recognized the format. It does not say the file is broken: it says nobody
 * knows how to read it.
 *
 * <p>The difference from {@link java.io.IOException} matters when diagnosing: that one means the
 * file could not be read, this one that it was read and not understood.
 */
public class UnsupportedAudioFileException extends Exception {

    private static final long serialVersionUID = -139127412623160368L;

    /** Without detail. */
    public UnsupportedAudioFileException() {
        super();
    }

    /** With a message. */
    public UnsupportedAudioFileException(String message) {
        super(message);
    }
}
