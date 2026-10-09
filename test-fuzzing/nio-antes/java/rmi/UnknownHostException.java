package java.rmi;

/**
 * KajiLibrary's java.rmi.UnknownHostException -- the server's name could not be resolved.
 *
 * <p>DNS did not know what to do with the name. It is RMI's version of
 * {@link java.net.UnknownHostException} --and not the same class-- because it has to be a
 * {@link RemoteException} to be able to come out through a remote method's signature.
 */
public class UnknownHostException extends RemoteException {

    private static final long serialVersionUID = -8152710247442114228L;

    /** @param s the message */
    public UnknownHostException(String s) {
        super(s);
    }

    /**
     * @param s the message
     * @param ex the cause
     */
    public UnknownHostException(String s, Exception ex) {
        super(s, ex);
    }
}
