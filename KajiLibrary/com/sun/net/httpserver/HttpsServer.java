package com.sun.net.httpserver;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.spi.HttpServerProvider;

/**
 * An {@link HttpServer} over TLS.
 *
 * <h2>The step that is missing and cannot be forgotten</h2>
 *
 * <p>Creating the server is not enough: with no {@link #setHttpsConfigurator} there is no TLS
 * context, and any incoming connection fails. It is intentional -- there is no default TLS
 * configuration that is reasonable -- but it is also the commonest mistake with this class,
 * because the server starts without complaining and fails only when somebody connects.
 */
public abstract class HttpsServer extends HttpServer {

    /** For the implementations. */
    protected HttpsServer() {
    }

    /** An unbound server; {@link #bind} has to be called on it and the configurator set. */
    public static HttpsServer create() throws IOException {
        return HttpServerProvider.provider().createHttpsServer(null, 0);
    }

    /** Bound to {@code addr}. */
    public static HttpsServer create(InetSocketAddress addr, int backlog) throws IOException {
        return HttpServerProvider.provider().createHttpsServer(addr, backlog);
    }

    /**
     * Bound, with a context and its filters already set.
     *
     * @throws NullPointerException if the path or the handler are missing
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
                throw new NullPointerException("a filter is null");
            }
            c.getFilters().add(filters[i]);
        }
        return s;
    }

    /**
     * It sets the TLS configurator.
     *
     * @throws NullPointerException if it is {@code null}
     */
    public abstract void setHttpsConfigurator(HttpsConfigurator config);

    /** The configurator, or {@code null} if it has not been set yet. */
    public abstract HttpsConfigurator getHttpsConfigurator();
}
