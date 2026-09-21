package java.rmi;

/**
 * KajiLibrary's java.rmi.ConnectException -- The server could not be reached.
 *
 * <p>The connection could not be <b>established</b>: nobody is listening, or the firewall cut it.
 * Different from {@link ConnectIOException}, which is when the connection was established and then
 * failed.
 *
 * <p>The difference matters for retrying: this one usually means the server is not up, and retrying
 * straight away will not change anything.
 */
public class ConnectException extends RemoteException {

    private static final long serialVersionUID = 4863550261346652506L;

    /** @param s the message */
    public ConnectException(String s) {
        super(s);
    }

    /**
     * @param s the message
     * @param ex the cause
     */
    public ConnectException(String s, Exception ex) {
        super(s, ex);
    }
}
