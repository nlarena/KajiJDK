package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.NonWritableChannelException — a channel that was not opened for
 * writing was written to.
 */
public class NonWritableChannelException extends IllegalStateException {

    private static final long serialVersionUID = 1000000016L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public NonWritableChannelException() {
        super();
    }
}
