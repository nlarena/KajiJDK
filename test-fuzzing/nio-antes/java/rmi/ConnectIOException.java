package java.rmi;

/**
 * KajiLibrary's java.rmi.ConnectIOException -- the connection's I/O failed.
 *
 * <p>The connection was established and then something broke along the way. Unlike
 * {@link ConnectException}, here the server exists and answers, so retrying can make sense.
 */
public class ConnectIOException extends RemoteException {

    private static final long serialVersionUID = -8087809532704668744L;

    /** @param s the message */
    public ConnectIOException(String s) {
        super(s);
    }

    /**
     * @param s the message
     * @param ex the cause
     */
    public ConnectIOException(String s, Exception ex) {
        super(s, ex);
    }
}
