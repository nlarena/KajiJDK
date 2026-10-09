package java.net.http;

import java.io.IOException;

/**
 * The timeout of an HTTP request ran out.
 *
 * <p>It is an {@link IOException} because for whoever receives it that is what it is: the operation
 * did not complete. What it adds over a plain one is that <strong>the timeout was set by the
 * caller</strong> — it is not that the network failed, it is that it took longer than it was
 * allowed. The difference matters when deciding whether to retry.
 *
 * <p>Its subclass {@link HttpConnectTimeoutException} separates the most useful case: running out
 * <em>while connecting</em>.
 *
 * @since 11
 */
public class HttpTimeoutException extends IOException {

    private static final long serialVersionUID = 981344271622632951L;

    /** With a message. */
    public HttpTimeoutException(String message) {
        super(message);
    }
}
