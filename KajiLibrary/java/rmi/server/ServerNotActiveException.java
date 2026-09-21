package java.rmi.server;

/**
 * {@link RemoteServer#getClientHost} was asked for outside the handling of a remote call.
 *
 * <p>It is not a server error but a misplaced question: "who is the client" only has an answer
 * while one is being served. Asking it from another thread, or before the call arrives, makes no
 * sense — and returning {@code null} would have hidden that.
 */
public class ServerNotActiveException extends Exception {

    private static final long serialVersionUID = 4687940720827538231L;

    /** With no detail. */
    public ServerNotActiveException() {
        super();
    }

    /** With a message. */
    public ServerNotActiveException(String s) {
        super(s);
    }
}
