package java.rmi;

/**
 * KajiLibrary's java.rmi.ServerException -- the remote method threw an RMI exception.
 *
 * <p>It wraps a {@link RemoteException} that happened <b>on the server</b> while it ran the method.
 * The distinction from the others is <b>where it happened</b>: the others are transport problems,
 * this one says the transport worked and the problem was over there.
 *
 * <p>That it is a {@code RemoteException} wrapped in another is not redundant: the inner one may
 * come from a call the server made to a third party.
 */
public class ServerException extends RemoteException {

    private static final long serialVersionUID = -4775845313121906682L;

    /** @param s the message */
    public ServerException(String s) {
        super(s);
    }

    /**
     * @param s the message
     * @param ex the cause
     */
    public ServerException(String s, Exception ex) {
        super(s, ex);
    }
}
