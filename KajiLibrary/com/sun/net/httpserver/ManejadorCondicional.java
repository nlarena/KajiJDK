package com.sun.net.httpserver;

import java.io.IOException;
import java.util.function.Predicate;

/** El manejador que devuelve {@link HttpHandlers#handleOrElse}. De paquete, como en el JDK. */
final class ManejadorCondicional implements HttpHandler {

    private final Predicate<Request> prueba;
    private final HttpHandler siPasa;
    private final HttpHandler siNo;

    ManejadorCondicional(Predicate<Request> prueba, HttpHandler siPasa, HttpHandler siNo) {
        this.prueba = prueba;
        this.siPasa = siPasa;
        this.siNo = siNo;
    }

    public void handle(HttpExchange exchange) throws IOException {
        if (this.prueba.test(exchange)) {
            this.siPasa.handle(exchange);
        } else {
            this.siNo.handle(exchange);
        }
    }
}
