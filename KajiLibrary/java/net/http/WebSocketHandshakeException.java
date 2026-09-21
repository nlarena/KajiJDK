package java.net.http;

import java.io.IOException;

/**
 * The server refused the switch to WebSocket.
 *
 * <p>What is notable is {@link #getResponse}: <strong>it carries the HTTP response inside</strong>.
 * A WebSocket handshake starts out as a normal HTTP request, and when it fails the server answers
 * with a status and a body that usually explain why — a {@code 401}, a {@code 404}, an unsupported
 * subprotocol. Without the response all of that would be lost, leaving only "it could not".
 *
 * @since 11
 */
public final class WebSocketHandshakeException extends IOException {

    private static final long serialVersionUID = 1L;

    private final transient HttpResponse<?> response;

    /** With the response the server gave. */
    public WebSocketHandshakeException(HttpResponse<?> response) {
        this.response = response;
    }

    /** The HTTP response of the refusal. */
    public HttpResponse<?> getResponse() {
        return this.response;
    }

    /**
     * Sets the cause and returns <strong>this</strong> class, not {@link Throwable}.
     *
     * <p>It is a covariant return type, and it is for chaining without casting: {@code throw new
     * WebSocketHandshakeException(r).initCause(e);} compiles because the type that comes back is
     * already the right one.
     */
    public WebSocketHandshakeException initCause(Throwable cause) {
        super.initCause(cause);
        return this;
    }
}
