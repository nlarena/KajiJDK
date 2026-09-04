package com.sun.net.httpserver;

import java.net.URI;

/**
 * La vista que devuelve {@link Request#with}: el pedido original con otros encabezados.
 *
 * <p>De paquete, igual que en el JDK, que la escribe como una anonima adentro del {@code default}.
 * Aca es una clase con nombre porque nuestro compilador no soporta una anonima en un inicializador
 * (#499) y porque asi se puede documentar que hace.
 */
final class RequestReescrito implements Request {

    private final Request original;
    private final Headers headers;

    RequestReescrito(Request original, Headers headers) {
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
