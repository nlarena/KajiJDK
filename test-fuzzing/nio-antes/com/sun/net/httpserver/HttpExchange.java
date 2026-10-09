package com.sun.net.httpserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * A request and its response, seen as a single object.
 *
 * <h2>The order is a contract, not a suggestion</h2>
 *
 * <p>Read the request's body, then {@link #sendResponseHeaders}, then write the response's
 * body, then {@link #close}. Going outside that order does not give a clear error: it gives a
 * header that arrives after the body, or a connection that is left hanging.
 *
 * <h2>{@code sendResponseHeaders}' two numbers</h2>
 *
 * <p>The second parameter has three different meanings according to the value, and it is the
 * easiest thing to get wrong in this whole API:
 *
 * <ul>
 * <li><strong>positive</strong> -- the body's exact length. Writing more or less than that
 *     breaks the response;</li>
 * <li><strong>zero</strong> -- there is a body but how much is not known; it is sent in
 *     chunks;</li>
 * <li><strong>{@code -1}</strong> -- there is no body. A {@code 204} or a {@code 304} needs it,
 *     and using zero there would leave the client waiting for a body that never arrives.</li>
 * </ul>
 *
 * <p>It is {@link AutoCloseable} from Java 21 on, so the right form is a {@code try} with
 * resources: closing releases the two streams and the connection, and not closing leaves it
 * held until it expires.
 */
public abstract class HttpExchange implements AutoCloseable, Request {

    /** For the implementations. */
    protected HttpExchange() {
    }

    /** The request's headers, read-only. */
    public abstract Headers getRequestHeaders();

    /**
     * The response's headers, mutable.
     *
     * <p>They have to be filled in <strong>before</strong> {@link #sendResponseHeaders}:
     * afterwards they have already travelled and modifying them does nothing.
     */
    public abstract Headers getResponseHeaders();

    /** The requested URI. */
    public abstract URI getRequestURI();

    /** The HTTP method, in upper case. */
    public abstract String getRequestMethod();

    /** The context that caught this request. */
    public abstract HttpContext getHttpContext();

    /**
     * It closes the exchange.
     *
     * <p>It does not declare {@code IOException}, and that is deliberate: closing has to be able
     * to go in a {@code finally} without forcing another {@code try} to be nested.
     */
    public abstract void close();

    /**
     * The request's body.
     *
     * <p>It has to be read to the end -- or closed -- even though it is of no interest: whatever
     * is left unread stays in the connection and throws the next request out if the client reuses
     * it.
     */
    public abstract InputStream getRequestBody();

    /**
     * The response's body.
     *
     * <p>It only serves after {@link #sendResponseHeaders}.
     */
    public abstract OutputStream getResponseBody();

    /**
     * It sends the code and the headers; see the class note about {@code responseLength}.
     *
     * @param rCode the HTTP code
     * @param responseLength positive the exact length, {@code 0} unknown, {@code -1} no body
     */
    public abstract void sendResponseHeaders(int rCode, long responseLength) throws IOException;

    /** Where the request came from. */
    public abstract InetSocketAddress getRemoteAddress();

    /** The code already sent, or {@code -1} if none has been sent yet. */
    public abstract int getResponseCode();

    /** The local address it came in through. */
    public abstract InetSocketAddress getLocalAddress();

    /** The protocol's version, such as {@code "HTTP/1.1"}. */
    public abstract String getProtocol();

    /**
     * An attribute of <strong>this</strong> request.
     *
     * <p>Different from {@link HttpContext#getAttributes}, which is shared between them all: this
     * lives as long as the exchange lives, and it is where a filter leaves something for the
     * handler.
     */
    public abstract Object getAttribute(String name);

    /** It sets an attribute of this request. */
    public abstract void setAttribute(String name, Object value);

    /**
     * It replaces the two streams.
     *
     * <p>It is how a filter wraps the body -- compressing it, counting it, encrypting it --
     * without the handler learning. Either of the two may be {@code null} to leave it as it
     * was.
     */
    public abstract void setStreams(InputStream i, OutputStream o);

    /**
     * Who sent the request, or {@code null} if the context had no authenticator.
     *
     * <p>It is never {@code null} when it did have one: a request that reached the handler with
     * an authenticator set is, by construction, an authenticated request.
     */
    public abstract HttpPrincipal getPrincipal();
}
