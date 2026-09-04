package com.sun.net.httpserver;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * El filtro que devuelven las tres fabricas de {@link Filter}.
 *
 * <p>De paquete: nadie deberia construirlo salvo por esas fabricas, que son las que garantizan que
 * exactamente una de las tres operaciones esta puesta. Una sola clase para los tres casos en vez de
 * tres, porque la diferencia entre ellos es una linea.
 */
final class FiltroSimple extends Filter {

    private final String descripcion;
    private final Consumer<HttpExchange> antes;
    private final Consumer<HttpExchange> despues;
    private final UnaryOperator<Request> adaptador;

    FiltroSimple(String descripcion, Consumer<HttpExchange> antes,
            Consumer<HttpExchange> despues, UnaryOperator<Request> adaptador) {
        if (descripcion == null) {
            throw new NullPointerException("description");
        }
        this.descripcion = descripcion;
        this.antes = antes;
        this.despues = despues;
        this.adaptador = adaptador;
    }

    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        if (this.antes != null) {
            this.antes.accept(exchange);
        }
        if (this.adaptador != null) {
            // El resultado se descarta a proposito: el JDK aplica el adaptador y sigue con el
            // intercambio original, porque el `Request` reescrito no es un `HttpExchange` y no hay
            // donde meterlo en la cadena. Queda dicho para que no parezca un olvido.
            this.adaptador.apply(exchange);
        }
        chain.doFilter(exchange);
        if (this.despues != null) {
            this.despues.accept(exchange);
        }
    }

    public String description() {
        return this.descripcion;
    }
}
