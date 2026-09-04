package com.sun.net.httpserver;

import java.io.IOException;

/**
 * Lo que atiende un pedido: la unica pieza que escribe siempre quien usa este servidor.
 *
 * <p>Una sola operacion, sin valor de retorno, y no es pobreza de diseno: la respuesta no se
 * <em>devuelve</em> sino que se <em>escribe</em> en el {@link HttpExchange}. Eso es lo que permite
 * responder con un flujo que no cabe en memoria, o empezar a mandar antes de saber cuanto va a
 * medir.
 *
 * <p>El precio de esa libertad es que el cierre queda a cargo de quien escribe: no cerrar el
 * intercambio deja la conexion tomada.
 */
public interface HttpHandler {

    /**
     * Atiende un pedido y escribe la respuesta.
     *
     * <p>Tiene que llamar a {@link HttpExchange#sendResponseHeaders} antes de escribir el cuerpo, y
     * cerrar el intercambio cuando termina.
     */
    void handle(HttpExchange exchange) throws IOException;
}
