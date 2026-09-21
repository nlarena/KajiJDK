package com.sun.net.httpserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

import com.sun.net.httpserver.spi.HttpServerProvider;

/**
 * A small HTTP server, which comes with the JDK.
 *
 * <h2>What it is and what it is not</h2>
 *
 * <p>It is enough to expose an endpoint, serve a few files or raise a mock in a test, without
 * bringing a whole container. It does not claim to be that: it has no servlets, no sessions, no
 * configurable connection pool.
 *
 * <h2>The executor, which is the decision that matters most</h2>
 *
 * <p>By default it is {@code null}, and that means that <strong>every request is attended on a
 * single thread</strong>, one after another. It works for a test and it is a trap anywhere
 * else: one slow handler blocks all the others. Giving it a pool with {@link #setExecutor} is
 * the first thing to do for real use.
 *
 * <h2>How the contexts are resolved</h2>
 *
 * <p>By longest prefix. With {@code /} and {@code /api} registered, a request to {@code /api/x}
 * goes to the second. It is what allows one to have a general handler and more specific
 * exceptions without ordering them by hand.
 *
 * <h2>With no provider installed</h2>
 *
 * <p>The {@link #create}s delegate to {@link HttpServerProvider}, which is looked up by
 * {@link java.util.ServiceLoader}. This VM brings none, so they throw
 * {@link UnsupportedOperationException} with the reason. The mechanism is complete: what is
 * missing is somebody to register in it.
 */
public abstract class HttpServer {

    /** For the implementations. */
    protected HttpServer() {
    }

    /** An unbound server; {@link #bind} has to be called on it. */
    public static HttpServer create() throws IOException {
        return HttpServerProvider.provider().createHttpServer(null, 0);
    }

    /**
     * Bound to {@code addr}, with that number of connections waiting.
     *
     * @param backlog {@code 0} or less leaves the system's value
     */
    public static HttpServer create(InetSocketAddress addr, int backlog) throws IOException {
        return HttpServerProvider.provider().createHttpServer(addr, backlog);
    }

    /**
     * Bound, with a context and its filters already set.
     *
     * <p>The shortcut for the common case: create, register a path and start in a single line.
     *
     * @throws NullPointerException if the path or the handler are missing
     * @throws IllegalArgumentException if the path is not absolute
     */
    public static HttpServer create(InetSocketAddress addr, int backlog, String path,
            HttpHandler handler, Filter... filters) throws IOException {
        if (path == null) {
            throw new NullPointerException("path");
        }
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        HttpServer s = create(addr, backlog);
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
     * It binds the server to an address.
     *
     * @throws java.net.BindException if the port is already taken
     */
    public abstract void bind(InetSocketAddress addr, int backlog) throws IOException;

    /**
     * It starts attending, on a separate thread.
     *
     * <p>It does not block, which is the other half of why the executor matters: the caller goes
     * on with its own business and does not learn whether there is one thread or twenty
     * attending.
     */
    public abstract void start();

    /**
     * Who runs the handlers; {@code null} goes back to the single thread.
     *
     * <p>See the class note: leaving it at {@code null} is the default configuration and almost
     * never the one that is wanted.
     */
    public abstract void setExecutor(Executor executor);

    /** The executor that was set, or {@code null}. */
    public abstract Executor getExecutor();

    /**
     * It stops attending, waiting up to {@code delay} seconds for the requests under way.
     *
     * <p>The requests that are still open after that term are cut off. A {@code 0} cuts
     * everything off at once.
     */
    public abstract void stop(int delay);

    /**
     * It registers a path with its handler.
     *
     * @throws IllegalArgumentException if the path is invalid or was already registered
     */
    public abstract HttpContext createContext(String path, HttpHandler handler);

    /**
     * It registers a path with no handler yet.
     *
     * <p>It serves in order to configure filters and authenticator first and set the handler
     * afterwards, with {@link HttpContext#setHandler}. A request that arrives before that gives
     * an error.
     */
    public abstract HttpContext createContext(String path);

    /** It removes the path. */
    public abstract void removeContext(String path) throws IllegalArgumentException;

    /** It removes that context. */
    public abstract void removeContext(HttpContext context);

    /**
     * The address it listens at.
     *
     * <p>It is worth consulting even though one has chosen the port: with port {@code 0} the
     * system chooses it, and this is the only way of knowing which one it got.
     */
    public abstract InetSocketAddress getAddress();
}
