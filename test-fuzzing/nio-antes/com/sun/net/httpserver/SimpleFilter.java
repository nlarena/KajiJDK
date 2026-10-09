package com.sun.net.httpserver;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * The filter {@link Filter}'s three factories return.
 *
 * <p>Package-private: nobody should build it except through those factories, which are the ones
 * that guarantee that exactly one of the three operations is set. A single class for the three
 * cases instead of three, because the difference between them is one line.
 */
final class SimpleFilter extends Filter {

    private final String description;
    private final Consumer<HttpExchange> before;
    private final Consumer<HttpExchange> after;
    private final UnaryOperator<Request> adapter;

    SimpleFilter(String description, Consumer<HttpExchange> before,
            Consumer<HttpExchange> after, UnaryOperator<Request> adapter) {
        if (description == null) {
            throw new NullPointerException("description");
        }
        this.description = description;
        this.before = before;
        this.after = after;
        this.adapter = adapter;
    }

    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        if (this.before != null) {
            this.before.accept(exchange);
        }
        if (this.adapter != null) {
            // The result is discarded on purpose: the JDK applies the adapter and goes on with the
                        // original exchange, because the rewritten `Request` is not an
                        // `HttpExchange` and there is nowhere to put it in the chain. It is said so
                        // that it does not look like an oversight.
            this.adapter.apply(exchange);
        }
        chain.doFilter(exchange);
        if (this.after != null) {
            this.after.accept(exchange);
        }
    }

    public String description() {
        return this.description;
    }
}
