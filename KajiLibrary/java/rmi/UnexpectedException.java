package java.rmi;

/**
 * KajiLibrary's java.rmi.UnexpectedException -- the remote method threw something it did not
 * declare.
 *
 * <p>A remote method declares its checked exceptions, and the client stub can only propagate them
 * if they are in the signature. If the server sends a checked one that is <b>not</b> declared
 * --because the server was compiled against another version of the interface-- the stub cannot
 * throw it without breaking the compiler, and wraps it here.
 *
 * <p>In practice it almost always means the same thing: client and server have different versions
 * of the remote interface.
 */
public class UnexpectedException extends RemoteException {

    private static final long serialVersionUID = 1800467484195073863L;

    /** @param s the message */
    public UnexpectedException(String s) {
        super(s);
    }

    /**
     * @param s the message
     * @param ex the one that arrived without being declared
     */
    public UnexpectedException(String s, Exception ex) {
        super(s, ex);
    }
}
