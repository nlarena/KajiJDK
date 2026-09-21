package com.sun.net.httpserver;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Code that runs around an {@link HttpHandler}: logging, authentication, compression.
 *
 * <h2>The chain, and why the filter receives the next link</h2>
 *
 * <p>{@link #doFilter} receives a {@link Chain} and decides whether to invoke it. That is more
 * than a <em>before</em> and an <em>after</em>: a filter that does <strong>not</strong> call
 * the chain cuts the request off right there, which is how it is rejected without reaching the
 * handler. And the code that goes after the call runs with the response already written, which
 * is where how long it took is measured.
 *
 * <p>It is the same figure as a decorator, with the difference that the composition is built by
 * the server from a list and not by the programmer nesting objects.
 */
public abstract class Filter {

    /** For the implementations. */
    protected Filter() {
    }

    /**
     * What is left of the chain: the filters that follow and, at the end, the handler.
     *
     * <p>It is an object with state -- it knows where it is -- and that is why it is <strong>not
     * reused</strong>: each request builds its own. Keeping one and calling it twice would walk
     * the tail from where it was left.
     */
    public static class Chain {

        private final List<Filter> filters;
        private final HttpHandler handler;
        private int next;

        public Chain(List<Filter> filters, HttpHandler handler) {
            this.filters = filters;
            this.handler = handler;
        }

        /**
         * It goes on with the next filter, or with the handler if none are left.
         *
         * <p>Returning from here means that the response has already been generated.
         */
        public void doFilter(HttpExchange exchange) throws IOException {
            if (this.next < this.filters.size()) {
                Filter f = this.filters.get(this.next);
                this.next = this.next + 1;
                f.doFilter(exchange, this);
            } else {
                this.handler.handle(exchange);
            }
        }
    }

    /**
     * It wraps the rest of the chain.
     *
     * <p>Calling {@link Chain#doFilter} goes on; not calling it cuts the request off, and there
     * this filter is responsible for writing the response.
     */
    public abstract void doFilter(HttpExchange exchange, Chain chain) throws IOException;

    /** So that it appears in a log or in a listing. */
    public abstract String description();

    /**
     * A filter that runs {@code operation} <strong>before</strong> the handler.
     *
     * <p>It receives the whole {@link HttpExchange}, so it may write the response -- but if it
     * does, the handler runs afterwards all the same. In order to cut off, the filter has to be
     * written by hand.
     */
    public static Filter beforeHandler(String description, Consumer<HttpExchange> operation) {
        return new SimpleFilter(description, operation, null, null);
    }

    /**
     * A filter that runs {@code operation} <strong>after</strong> the handler.
     *
     * <p>The response has already been written, so here the code and how much came out may be
     * looked at, but they can no longer be changed.
     */
    public static Filter afterHandler(String description, Consumer<HttpExchange> operation) {
        return new SimpleFilter(description, null, operation, null);
    }

    /**
     * A filter that rewrites the request before the handler sees it.
     *
     * <p>Different from {@link #beforeHandler}: that one looks at and operates on the exchange,
     * this one returns a <em>different</em> {@link Request}. It is what allows a header to be
     * normalized without the handler learning that there was a correction.
     */
    public static Filter adaptRequest(String description, UnaryOperator<Request> requestOperator) {
        return new SimpleFilter(description, null, null, requestOperator);
    }
}
