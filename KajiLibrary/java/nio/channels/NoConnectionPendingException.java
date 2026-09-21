package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.NoConnectionPendingException — finishing the connection of a
 * channel that never started connecting was attempted.
 */
public class NoConnectionPendingException extends IllegalStateException {

    private static final long serialVersionUID = 1000000014L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public NoConnectionPendingException() {
        super();
    }
}
