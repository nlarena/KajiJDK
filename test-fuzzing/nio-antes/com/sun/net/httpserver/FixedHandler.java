package com.sun.net.httpserver;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** The handler {@link HttpHandlers#of} returns. Package-private, as in the JDK. */
final class FixedHandler implements HttpHandler {

    private final int code;
    private final Headers headers;
    private final byte[] body;

    FixedHandler(int code, Headers headers, String body) {
        this.code = code;
        this.headers = headers;
        this.body = body.getBytes(StandardCharsets.UTF_8);
    }

    public void handle(HttpExchange exchange) throws IOException {
        // The request's body is discarded entirely and on purpose: whatever is left unread stays
                // in the connection and throws the next request out if the client reuses it.
        exchange.getRequestBody().readAllBytes();
        for (Map.Entry<String, List<String>> e : this.headers.entrySet()) {
            exchange.getResponseHeaders().put(e.getKey(), e.getValue());
        }
        // An empty body is sent as "no body" (-1) and not as length zero: they are two different
                // responses, and with `0` the client waits for a chunked body that never arrives.
        if (this.body.length == 0) {
            exchange.sendResponseHeaders(this.code, -1);
            exchange.close();
            return;
        }
        exchange.sendResponseHeaders(this.code, this.body.length);
        OutputStream out = exchange.getResponseBody();
        try {
            out.write(this.body);
        } finally {
            exchange.close();
        }
    }
}
