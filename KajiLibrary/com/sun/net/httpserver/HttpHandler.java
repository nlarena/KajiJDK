package com.sun.net.httpserver;

import java.io.IOException;

/**
 * What attends a request: the only piece whoever uses this server always writes.
 *
 * <p>A single operation, with no return value, and it is not poverty of design: the response is
 * not <em>returned</em> but <em>written</em> into the {@link HttpExchange}. That is what allows
 * one to answer with a stream that does not fit in memory, or to start sending before knowing
 * how much it is going to measure.
 *
 * <p>The price of that freedom is that the closing is up to whoever writes: not closing the
 * exchange leaves the connection held.
 */
public interface HttpHandler {

    /**
     * It attends a request and writes the response.
     *
     * <p>It has to call {@link HttpExchange#sendResponseHeaders} before writing the body, and
     * close the exchange when it finishes.
     */
    void handle(HttpExchange exchange) throws IOException;
}
