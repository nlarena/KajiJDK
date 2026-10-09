package com.sun.net.httpserver;

import java.util.function.Predicate;

/**
 * Two factories for building handlers without writing a class.
 *
 * <p>They cover the two cases that appear all the time and do not deserve a type of their own:
 * returning a fixed response, and choosing between two handlers by looking at the request.
 *
 * <p>{@link #handleOrElse} composes: the {@code fallback} may be another {@code handleOrElse},
 * and that builds a router with no registry and no table.
 */
public final class HttpHandlers {

    private HttpHandlers() {
    }

    /**
     * If {@code handlerTest} accepts the request it is attended by {@code handler}; if not, by
     * {@code fallbackHandler}.
     *
     * <p>The predicate receives a {@link Request} and not the whole exchange, which is deliberate:
     * choosing a handler is a decision that only looks at the request, and giving it access to the
     * response would be an invitation to write it from there.
     *
     * @throws NullPointerException if any of them is {@code null}
     */
    public static HttpHandler handleOrElse(Predicate<Request> handlerTest, HttpHandler handler,
            HttpHandler fallbackHandler) {
        if (handlerTest == null) {
            throw new NullPointerException("handlerTest");
        }
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        if (fallbackHandler == null) {
            throw new NullPointerException("fallbackHandler");
        }
        return new ConditionalHandler(handlerTest, handler, fallbackHandler);
    }

    /**
     * A handler that always answers the same.
     *
     * <p>It serves for a {@code 404}, a {@code 301} or a health endpoint. The body is sent in
     * UTF-8, and an empty body produces a response with no body -- not one of length zero, which
     * is another thing.
     *
     * @throws IllegalArgumentException if the code is not between {@code 100} and {@code 599}
     * @throws NullPointerException if the headers or the body are missing
     */
    public static HttpHandler of(int statusCode, Headers headers, String body) {
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("code out of range: "
                    + String.valueOf(statusCode));
        }
        if (headers == null) {
            throw new NullPointerException("headers");
        }
        if (body == null) {
            throw new NullPointerException("body");
        }
        return new FixedHandler(statusCode, Headers.of(headers), body);
    }
}
