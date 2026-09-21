package java.rmi;

/**
 * KajiLibrary's java.rmi.MarshalException -- writing the arguments failed.
 *
 * <p>It is on the <b>sending</b> side: the call's arguments, or the method's identifier, could not
 * be serialised. Its mirror is {@link UnmarshalException}, on the receiving side.
 *
 * <p>It has a consequence worth keeping in mind: since the call never went out whole, the remote
 * method <b>did not run</b>. It is one of the few RMI exceptions of which that can be asserted.
 */
public class MarshalException extends RemoteException {

    private static final long serialVersionUID = 6223554758134037936L;

    /** @param s the message */
    public MarshalException(String s) {
        super(s);
    }

    /**
     * @param s the message
     * @param ex the cause
     */
    public MarshalException(String s, Exception ex) {
        super(s, ex);
    }
}
