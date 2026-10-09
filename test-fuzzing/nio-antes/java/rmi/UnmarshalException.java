package java.rmi;

/**
 * KajiLibrary's java.rmi.UnmarshalException -- A failure reading what arrived.
 *
 * <p>The mirror of {@link MarshalException}: it is on the <b>receiving</b> side. Deserialisation
 * failed --typically because a class is missing, or because the version does not match--.
 *
 * <p>Unlike that one, here it is <b>not known</b> whether the remote method ran: if the failure was
 * in reading the result, it already ran.
 */
public class UnmarshalException extends RemoteException {

    private static final long serialVersionUID = 594380845140740218L;

    /** @param s the message */
    public UnmarshalException(String s) {
        super(s);
    }

    /**
     * @param s the message
     * @param ex the cause
     */
    public UnmarshalException(String s, Exception ex) {
        super(s, ex);
    }
}
