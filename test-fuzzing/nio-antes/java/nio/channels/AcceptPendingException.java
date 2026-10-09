package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.AcceptPendingException — accepting a connection was asked for over
 * a channel that has an acceptance under way already.
 *
 * <p>An asynchronous channel admits **one** acceptance operation at a time: the second one is not
 * queued, it is refused. Queueing them silently would make the order in which they complete depend
 * on details the caller does not control.
 */
public class AcceptPendingException extends IllegalStateException {

    private static final long serialVersionUID = 1000000000L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public AcceptPendingException() {
        super();
    }
}
