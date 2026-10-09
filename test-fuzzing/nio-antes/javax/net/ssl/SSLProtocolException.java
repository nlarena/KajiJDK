package javax.net.ssl;

/**
 * An error in the protocol itself.
 *
 * <p>What arrived is not valid SSL/TLS: a malformed message, a field out of range, an impossible
 * order. Different from {@link SSLHandshakeException}, where the messages were correct and what
 * failed was the agreement.
 */
public class SSLProtocolException extends SSLException {

    private static final long serialVersionUID = 5445067063799134928L;

    /** With a message. */
    public SSLProtocolException(String reason) {
        super(reason);
    }

    /** With a message and the underlying cause. */
    public SSLProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
