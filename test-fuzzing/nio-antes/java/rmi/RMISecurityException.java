package java.rmi;

/**
 * KajiLibrary's java.rmi.RMISecurityException -- deprecated since 1.2.
 *
 * <p>Nobody throws it any more: where this used to come out, a plain {@link SecurityException}
 * comes out now. It is kept only so old code compiles.
 *
 * <p>The second constructor takes two strings and it was never clear what the second one meant; it
 * does not matter any more either.
 */
@Deprecated
public class RMISecurityException extends SecurityException {

    private static final long serialVersionUID = -8433406075740433514L;

    /** @param name the message */
    @Deprecated
    public RMISecurityException(String name) {
        super(name);
    }

    /**
     * @param name the message
     * @param arg unused
     */
    @Deprecated
    public RMISecurityException(String name, String arg) {
        this(name);
    }
}
