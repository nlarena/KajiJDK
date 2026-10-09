package java.rmi.server;

/**
 * The export failed because opening the port was not permitted.
 *
 * <p>A particular case of {@link ExportException}, kept separate so "could not" can be told from
 * "was not allowed": the first is fixed by retrying or changing port, the second is not.
 */
public class SocketSecurityException extends ExportException {

    private static final long serialVersionUID = -7622072999407781979L;

    /** With a message. */
    public SocketSecurityException(String s) {
        super(s);
    }

    /** With a message and the cause. */
    public SocketSecurityException(String s, Exception ex) {
        super(s, ex);
    }
}
