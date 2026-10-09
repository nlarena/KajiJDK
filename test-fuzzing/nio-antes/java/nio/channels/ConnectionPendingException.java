package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.ConnectionPendingException — connecting was asked for over a
 * channel that has an unfinished connection under way already.
 */
public class ConnectionPendingException extends IllegalStateException {

    private static final long serialVersionUID = 1000000008L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public ConnectionPendingException() {
        super();
    }
}
