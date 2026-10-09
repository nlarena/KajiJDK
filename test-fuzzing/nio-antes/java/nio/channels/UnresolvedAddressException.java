package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.UnresolvedAddressException — a socket address that was never
 * resolved was used.
 *
 * <p>An unresolved address is a name with no number: it can be built and passed, but it cannot be
 * used to connect. Failing here, and not inside the network stack, makes the error point at the
 * place where it can be fixed.
 */
public class UnresolvedAddressException extends IllegalArgumentException {

    private static final long serialVersionUID = 1000000022L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public UnresolvedAddressException() {
        super();
    }
}
