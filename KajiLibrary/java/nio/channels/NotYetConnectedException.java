package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.NotYetConnectedException — a channel that has not been connected
 * yet was read from or written to.
 */
public class NotYetConnectedException extends IllegalStateException {

    private static final long serialVersionUID = 1000000018L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public NotYetConnectedException() {
        super();
    }
}
