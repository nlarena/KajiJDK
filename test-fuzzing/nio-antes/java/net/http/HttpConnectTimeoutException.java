package java.net.http;

/**
 * The timeout ran out <strong>before connecting</strong>.
 *
 * <h2>Why it deserves its own type</h2>
 *
 * <p>Because it separates "did not get there" from "got there and was slow". If the connection was
 * never established, the server <strong>did not see the request</strong> — and then retrying is
 * safe even for a {@code POST}, which it would not be if the response had been lost after the
 * server processed it.
 *
 * <p>It is the only way this API gives to know that, which is why it is worth a type and not a
 * field.
 *
 * @since 11
 */
public class HttpConnectTimeoutException extends HttpTimeoutException {

    private static final long serialVersionUID = 321L + 11L;

    /** With a message. */
    public HttpConnectTimeoutException(String message) {
        super(message);
    }
}
