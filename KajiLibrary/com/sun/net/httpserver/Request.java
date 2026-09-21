package com.sun.net.httpserver;

import java.net.URI;
import java.util.List;

/**
 * The part of an {@link HttpExchange} that is only read: method, URI and headers.
 *
 * <h2>What separating this from the exchange is for</h2>
 *
 * <p>For being able to <strong>rewrite a request without touching the connection</strong>.
 * {@link #with} returns a view with one header changed, and that is enough for a filter to
 * normalize, add or correct something before the handler sees it -- with no way of writing the
 * response by accident from there.
 *
 * <p>It is also what the predicates of {@link HttpHandlers#handleOrElse} receive: choosing a
 * handler is a decision that only looks at the request, and giving it the whole exchange would
 * be giving it too much.
 */
public interface Request {

    /** The requested URI. */
    URI getRequestURI();

    /** The HTTP method. */
    String getRequestMethod();

    /** The request's headers. */
    Headers getRequestHeaders();

    /**
     * A view of this request with {@code headerName} set to {@code headerValues}.
     *
     * <p>The original request does not change: what is returned is another view. It is what allows
     * a filter to adjust what the handler sees with no effect on anything else.
     */
    default Request with(String headerName, List<String> headerValues) {
        Request original = this;
        Headers combined = new Headers(original.getRequestHeaders());
        combined.put(headerName, headerValues);
        return new RewrittenRequest(original, combined);
    }
}
