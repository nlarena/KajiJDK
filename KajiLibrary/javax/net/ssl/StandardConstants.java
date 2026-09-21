package javax.net.ssl;

/**
 * The protocol's standardized constants.
 *
 * <p>Today it has only one: the server name type of the SNI extension. It is a whole class for one
 * constant because the IANA registry may grow, and the place where the new one would go has to
 * exist beforehand.
 */
public final class StandardConstants {

    private StandardConstants() {
    }

    /**
     * The "host name" type of the SNI extension, which is {@code 0}.
     *
     * <p>SNI solves a concrete problem: the client has to say which site it connects to
     * <strong>before</strong> the server sends it a certificate, because at a single IP address
     * there may be many sites and each with its own. Without SNI, the server would have to choose
     * blindly.
     *
     * <p>See {@link SNIHostName}.
     */
    public static final int SNI_HOST_NAME = 0;
}
