package java.rmi;

/**
 * KajiLibrary's java.rmi.AlreadyBoundException -- that name is already taken in the registry.
 *
 * <p>It comes out of {@code bind}, which refuses to overwrite. {@code rebind} is the one that does
 * overwrite, and that is why it does not throw this.
 *
 * <p>That they are two different operations is on purpose: starting the same server twice by
 * mistake is easy, and with {@code bind} the second one fails instead of silently stealing the
 * first one's clients.
 *
 * <p>It does not inherit from {@link RemoteException}: it is not a network problem but one of the
 * registry's contents, and the call arrived perfectly well.
 */
public class AlreadyBoundException extends Exception {

    private static final long serialVersionUID = 9218657361741657110L;

    /** With no detail. */
    public AlreadyBoundException() {
        super();
    }

    /** @param s the name that was already there */
    public AlreadyBoundException(String s) {
        super(s);
    }
}
