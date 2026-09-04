package com.sun.net.httpserver;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** El manejador que devuelve {@link HttpHandlers#of}. De paquete, como en el JDK. */
final class ManejadorFijo implements HttpHandler {

    private final int codigo;
    private final Headers encabezados;
    private final byte[] cuerpo;

    ManejadorFijo(int codigo, Headers encabezados, String cuerpo) {
        this.codigo = codigo;
        this.encabezados = encabezados;
        this.cuerpo = cuerpo.getBytes(StandardCharsets.UTF_8);
    }

    public void handle(HttpExchange exchange) throws IOException {
        // El cuerpo del pedido se descarta enteramente y a proposito: lo que quede sin leer se
        // queda en la conexion y descoloca al pedido siguiente si el cliente la reusa.
        exchange.getRequestBody().readAllBytes();
        for (Map.Entry<String, List<String>> e : this.encabezados.entrySet()) {
            exchange.getResponseHeaders().put(e.getKey(), e.getValue());
        }
        // Un cuerpo vacio se manda como "sin cuerpo" (-1) y no como largo cero: son dos respuestas
        // distintas, y con `0` el cliente espera un cuerpo por trozos que nunca llega.
        if (this.cuerpo.length == 0) {
            exchange.sendResponseHeaders(this.codigo, -1);
            exchange.close();
            return;
        }
        exchange.sendResponseHeaders(this.codigo, this.cuerpo.length);
        OutputStream out = exchange.getResponseBody();
        try {
            out.write(this.cuerpo);
        } finally {
            exchange.close();
        }
    }
}
