package java.rmi;

/**
 * KajiLibrary's java.rmi.NotBoundException -- that name is not in the registry.
 *
 * <p>The mirror of {@link AlreadyBoundException}: it comes out of {@code lookup} and {@code
 * unbind}.
 *
 * <p>It does not inherit from {@link RemoteException} either, for the same reason: the registry
 * answered fine, it just has nothing by that name. Telling them apart matters -- a client that
 * catches this one can wait and retry; one that catches a {@code RemoteException} has a network
 * problem.
 */
public class NotBoundException extends Exception {

    private static final long serialVersionUID = -1857741824849069317L;

    /** With no detail. */
    public NotBoundException() {
        super();
    }

    /** @param s the name that was not there */
    public NotBoundException(String s) {
        super(s);
    }
}
