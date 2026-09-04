package com.sun.net.httpserver;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.spi.HttpServerProvider;

/**
 * Un {@link HttpServer} sobre TLS.
 *
 * <h2>El paso que falta y no se puede olvidar</h2>
 *
 * <p>Crear el servidor no alcanza: sin {@link #setHttpsConfigurator} no hay contexto TLS, y
 * cualquier conexion entrante falla. Es intencional —no hay una configuracion de TLS por omision
 * que sea razonable— pero es tambien el error mas comun con esta clase, porque el servidor arranca
 * sin quejarse y falla recien cuando alguien se conecta.
 */
public abstract class HttpsServer extends HttpServer {

    /** Para las implementaciones. */
    protected HttpsServer() {
    }

    /** Un servidor sin ligar; hay que llamarle {@link #bind} y ponerle el configurador. */
    public static HttpsServer create() throws IOException {
        return HttpServerProvider.provider().createHttpsServer(null, 0);
    }

    /** Ligado a {@code addr}. */
    public static HttpsServer create(InetSocketAddress addr, int backlog) throws IOException {
        return HttpServerProvider.provider().createHttpsServer(addr, backlog);
    }

    /**
     * Ligado, con un contexto y sus filtros ya puestos.
     *
     * @throws NullPointerException si falta la ruta o el manejador
     */
    public static HttpsServer create(InetSocketAddress addr, int backlog, String path,
            HttpHandler handler, Filter... filters) throws IOException {
        if (path == null) {
            throw new NullPointerException("path");
        }
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        HttpsServer s = create(addr, backlog);
        HttpContext c = s.createContext(path, handler);
        for (int i = 0; i < filters.length; i++) {
            if (filters[i] == null) {
                throw new NullPointerException("un filtro es null");
            }
            c.getFilters().add(filters[i]);
        }
        return s;
    }

    /**
     * Pone el configurador de TLS.
     *
     * @throws NullPointerException si es {@code null}
     */
    public abstract void setHttpsConfigurator(HttpsConfigurator config);

    /** El configurador, o {@code null} si todavia no se puso. */
    public abstract HttpsConfigurator getHttpsConfigurator();
}
