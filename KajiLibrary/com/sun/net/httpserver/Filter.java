package com.sun.net.httpserver;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Codigo que corre alrededor de un {@link HttpHandler}: registro, autenticacion, compresion.
 *
 * <h2>La cadena, y por que el filtro recibe el eslabon siguiente</h2>
 *
 * <p>{@link #doFilter} recibe un {@link Chain} y decide si lo invoca. Eso es mas que un
 * <em>antes</em> y un <em>despues</em>: un filtro que <strong>no</strong> llama a la cadena corta
 * el pedido ahi mismo, que es como se rechaza sin llegar al manejador. Y el codigo que va despues de
 * la llamada corre con la respuesta ya escrita, que es donde se mide cuanto tardo.
 *
 * <p>Es la misma figura que un decorador, con la diferencia de que la composicion la arma el
 * servidor a partir de una lista y no el programador anidando objetos.
 */
public abstract class Filter {

    /** Para las implementaciones. */
    protected Filter() {
    }

    /**
     * Lo que queda de la cadena: los filtros que siguen y, al final, el manejador.
     *
     * <p>Es un objeto con estado —sabe por donde va— y por eso <strong>no se reusa</strong>: cada
     * pedido arma la suya. Guardarse una y llamarla dos veces recorreria la cola desde donde quedo.
     */
    public static class Chain {

        private final List<Filter> filtros;
        private final HttpHandler handler;
        private int siguiente;

        public Chain(List<Filter> filters, HttpHandler handler) {
            this.filtros = filters;
            this.handler = handler;
        }

        /**
         * Sigue con el proximo filtro, o con el manejador si no quedan.
         *
         * <p>Devolver de aca significa que la respuesta ya se genero.
         */
        public void doFilter(HttpExchange exchange) throws IOException {
            if (this.siguiente < this.filtros.size()) {
                Filter f = this.filtros.get(this.siguiente);
                this.siguiente = this.siguiente + 1;
                f.doFilter(exchange, this);
            } else {
                this.handler.handle(exchange);
            }
        }
    }

    /**
     * Envuelve el resto de la cadena.
     *
     * <p>Llamar a {@link Chain#doFilter} sigue; no llamarlo corta el pedido, y ahi este filtro es
     * responsable de escribir la respuesta.
     */
    public abstract void doFilter(HttpExchange exchange, Chain chain) throws IOException;

    /** Para que aparezca en un log o en un listado. */
    public abstract String description();

    /**
     * Un filtro que corre {@code operation} <strong>antes</strong> del manejador.
     *
     * <p>Recibe el {@link HttpExchange} entero, asi que puede escribir la respuesta — pero si lo
     * hace, el manejador corre igual despues. Para cortar hay que escribir el filtro a mano.
     */
    public static Filter beforeHandler(String description, Consumer<HttpExchange> operation) {
        return new FiltroSimple(description, operation, null, null);
    }

    /**
     * Un filtro que corre {@code operation} <strong>despues</strong> del manejador.
     *
     * <p>La respuesta ya se escribio, asi que aca se puede mirar el codigo y cuanto salio, pero ya
     * no se puede cambiar.
     */
    public static Filter afterHandler(String description, Consumer<HttpExchange> operation) {
        return new FiltroSimple(description, null, operation, null);
    }

    /**
     * Un filtro que reescribe el pedido antes de que lo vea el manejador.
     *
     * <p>Distinto de {@link #beforeHandler}: aquel mira y opera sobre el intercambio, este devuelve
     * un {@link Request} <em>distinto</em>. Es lo que permite normalizar un encabezado sin que el
     * manejador se entere de que hubo una correccion.
     */
    public static Filter adaptRequest(String description, UnaryOperator<Request> requestOperator) {
        return new FiltroSimple(description, null, null, requestOperator);
    }
}
