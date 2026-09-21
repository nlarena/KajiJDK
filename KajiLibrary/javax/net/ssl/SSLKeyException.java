package javax.net.ssl;

/**
 * There is something wrong with a key.
 *
 * <p>On the own side, not the peer's: the key could not be used, is malformed, or does not match
 * the certificate accompanying it. A problem with the peer's key arrives as {@link
 * SSLPeerUnverifiedException}.
 */
public class SSLKeyException extends SSLException {

    private static final long serialVersionUID = -8071664081190424597L;

    /** With a message. */
    public SSLKeyException(String reason) {
        super(reason);
    }

    /** With a message and the underlying cause. */
    public SSLKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
