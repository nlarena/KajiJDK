package com.sun.net.httpserver;

import java.io.IOException;
import java.util.function.Predicate;

/** The handler {@link HttpHandlers#handleOrElse} returns. Package-private, as in the JDK. */
final class ConditionalHandler implements HttpHandler {

    private final Predicate<Request> test;
    private final HttpHandler ifTrue;
    private final HttpHandler otherwise;

    ConditionalHandler(Predicate<Request> test, HttpHandler ifTrue, HttpHandler otherwise) {
        this.test = test;
        this.ifTrue = ifTrue;
        this.otherwise = otherwise;
    }

    public void handle(HttpExchange exchange) throws IOException {
        if (this.test.test(exchange)) {
            this.ifTrue.handle(exchange);
        } else {
            this.otherwise.handle(exchange);
        }
    }
}
