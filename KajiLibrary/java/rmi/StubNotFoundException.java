package java.rmi;

/**
 * KajiLibrary's java.rmi.StubNotFoundException -- The stub class is missing.
 *
 * <p>The stub class --the intermediary that turns a local call into a remote one-- was not found
 * when exporting an object or when receiving a reference.
 *
 * <p>It is an exception from another era. Since 1.5 stubs are generated on their own with
 * {@code java.lang.reflect.Proxy}, so in practice it only appears with code compiled with
 * {@code rmic}.
 */
public class StubNotFoundException extends RemoteException {

    private static final long serialVersionUID = -7088199405468872373L;

    /** @param s the message */
    public StubNotFoundException(String s) {
        super(s);
    }

    /**
     * @param s the message
     * @param ex the cause
     */
    public StubNotFoundException(String s, Exception ex) {
        super(s, ex);
    }
}
