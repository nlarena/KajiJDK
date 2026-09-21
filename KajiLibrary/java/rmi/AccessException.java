package java.rmi;

/**
 * KajiLibrary's java.rmi.AccessException -- the registry does not allow that.
 *
 * <p>It comes out of {@code bind}, {@code rebind} and {@code unbind} when the caller is not
 * authorised.
 *
 * <p>The rule is an old one and it surprises: an RMI registry accepts those three operations only
 * from the <b>same machine</b>. A remote client can look up and list, it cannot modify. It is the
 * only defence a registry has, which otherwise authenticates nobody.
 */
public class AccessException extends RemoteException {

    private static final long serialVersionUID = 6314925228044966088L;

    /** @param s the message */
    public AccessException(String s) {
        super(s);
    }

    /**
     * @param s the message
     * @param ex the cause
     */
    public AccessException(String s, Exception ex) {
        super(s, ex);
    }
}
