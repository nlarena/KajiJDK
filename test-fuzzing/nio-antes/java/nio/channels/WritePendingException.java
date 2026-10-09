package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.WritePendingException — writing was asked for over an asynchronous
 * channel that has a write under way already.
 */
public class WritePendingException extends IllegalStateException {

    private static final long serialVersionUID = 1000000024L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public WritePendingException() {
        super();
    }
}
