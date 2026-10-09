package javax.net.ssl;

import java.io.IOException;

/**
 * Something failed in the SSL/TLS layer.
 *
 * <p>It is an {@link IOException} and not something separate, and that is a design decision with a
 * practical consequence: whoever writes over a secure socket does not have to learn a new
 * hierarchy. A {@code catch (IOException)} that already existed keeps working, and whoever wants to
 * tell the cryptographic failure from the network cut catches this one.
 *
 * <p>Its four subclasses say <em>at which stage</em> it broke: see {@link SSLHandshakeException},
 * {@link SSLKeyException}, {@link SSLPeerUnverifiedException} and {@link SSLProtocolException}.
 * (The note said three and listed four.)
 */
public class SSLException extends IOException {

    private static final long serialVersionUID = 4511006460650708967L;

    /** With a message. */
    public SSLException(String reason) {
        super(reason);
    }

    /** With a message and the underlying cause. */
    public SSLException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Wrapping what really failed. */
    public SSLException(Throwable cause) {
        super(cause);
    }
}
