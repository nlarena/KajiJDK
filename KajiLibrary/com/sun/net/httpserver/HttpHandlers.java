package com.sun.net.httpserver;

import java.util.function.Predicate;

/**
 * Dos fabricas para armar manejadores sin escribir una clase.
 *
 * <p>Cubren los dos casos que aparecen todo el tiempo y no merecen un tipo propio: devolver una
 * respuesta fija, y elegir entre dos manejadores mirando el pedido.
 *
 * <p>{@link #handleOrElse} se compone: el {@code fallback} puede ser otro {@code handleOrElse}, y
 * eso arma un enrutador sin ningun registro ni tabla.
 */
public final class HttpHandlers {

    private HttpHandlers() {
    }

    /**
     * Si {@code handlerTest} acepta el pedido lo atiende {@code handler}; si no,
     * {@code fallbackHandler}.
     *
     * <p>El predicado recibe un {@link Request} y no el intercambio entero, que es deliberado:
     * elegir manejador es una decision que solo mira el pedido, y darle acceso a la respuesta
     * invitaria a escribirla desde ahi.
     *
     * @throws NullPointerException si alguno es {@code null}
     */
    public static HttpHandler handleOrElse(Predicate<Request> handlerTest, HttpHandler handler,
            HttpHandler fallbackHandler) {
        if (handlerTest == null) {
            throw new NullPointerException("handlerTest");
        }
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        if (fallbackHandler == null) {
            throw new NullPointerException("fallbackHandler");
        }
        return new ManejadorCondicional(handlerTest, handler, fallbackHandler);
    }

    /**
     * Un manejador que siempre contesta lo mismo.
     *
     * <p>Sirve para un {@code 404}, un {@code 301} o un endpoint de salud. El cuerpo se manda en
     * UTF-8, y un cuerpo vacio produce una respuesta sin cuerpo — no una de largo cero, que es otra
     * cosa.
     *
     * @throws IllegalArgumentException si el codigo no esta entre {@code 100} y {@code 599}
     * @throws NullPointerException si faltan los encabezados o el cuerpo
     */
    public static HttpHandler of(int statusCode, Headers headers, String body) {
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("codigo fuera de rango: "
                    + String.valueOf(statusCode));
        }
        if (headers == null) {
            throw new NullPointerException("headers");
        }
        if (body == null) {
            throw new NullPointerException("body");
        }
        return new ManejadorFijo(statusCode, Headers.of(headers), body);
    }
}
