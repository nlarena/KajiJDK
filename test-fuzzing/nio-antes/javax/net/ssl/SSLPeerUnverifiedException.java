package javax.net.ssl;

/**
 * Whoever is on the other side could not be verified.
 *
 * <p>The session may exist all the same: SSL admits anonymous suites, and with those there is
 * encryption but no identity. That is why asking an {@link SSLSession} for the peer's certificate
 * may fail even though everything else works — encrypting and authenticating are two different
 * things, and this exception is the place where that distinction becomes visible.
 */
public class SSLPeerUnverifiedException extends SSLException {

    private static final long serialVersionUID = -8919512675153181392L;

    /** With a message. */
    public SSLPeerUnverifiedException(String reason) {
        super(reason);
    }

    /** With a message and the underlying cause. */
    public SSLPeerUnverifiedException(String message, Throwable cause) {
        super(message, cause);
    }
}
