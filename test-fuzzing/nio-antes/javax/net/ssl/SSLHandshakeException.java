package javax.net.ssl;

/**
 * The handshake did not get to finish, so <strong>there was never a session</strong>.
 *
 * <p>It is the most informative failure of the four: it means the two ends could not agree. The
 * typical causes are that they share no cipher suite, that the certificate does not validate, or
 * that one asks for client authentication and the other does not have it.
 */
public class SSLHandshakeException extends SSLException {

    private static final long serialVersionUID = -5045881315018326890L;

    /** With a message. */
    public SSLHandshakeException(String reason) {
        super(reason);
    }

    /** With a message and the underlying cause. */
    public SSLHandshakeException(String message, Throwable cause) {
        super(message, cause);
    }
}
