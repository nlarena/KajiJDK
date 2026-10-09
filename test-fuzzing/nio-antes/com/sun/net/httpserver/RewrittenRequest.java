package com.sun.net.httpserver;

import java.net.URI;

/**
 * The view {@link Request#with} returns: the original request with other headers.
 *
 * <p>Package-private, just as in the JDK, which writes it as an anonymous one inside the
 * {@code default}. Here it is a named class because our compiler does not support an anonymous
 * one in an initializer (#499) and because that way what it does can be documented.
 */
final class RewrittenRequest implements Request {

    private final Request original;
    private final Headers headers;

    RewrittenRequest(Request original, Headers headers) {
        this.original = original;
        this.headers = headers;
    }

    public URI getRequestURI() {
        return this.original.getRequestURI();
    }

    public String getRequestMethod() {
        return this.original.getRequestMethod();
    }

    public Headers getRequestHeaders() {
        return this.headers;
    }
}
