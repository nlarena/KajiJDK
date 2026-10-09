package com.sun.net.httpserver;

import java.util.List;
import java.util.Map;

/**
 * The union between a path and who attends it, with everything that is hung around it.
 *
 * <h2>Why {@code createContext} returns this and not {@code void}</h2>
 *
 * <p>Because registering the path is only the beginning: afterwards filters have to be set on
 * it, perhaps an authenticator, perhaps shared attributes. Returning the context allows all
 * that to be done without a second registration and without the server having to expose a
 * method for each configurable thing.
 *
 * <p>{@link #getAttributes} is a mutable map shared by every request of this path: it is where
 * the state a handler needs between requests goes, and for that very reason it has to be
 * synchronized if it is written.
 */
public abstract class HttpContext {

    /** For the implementations. */
    protected HttpContext() {
    }

    /** Who attends this path. */
    public abstract HttpHandler getHandler();

    /**
     * It changes who attends.
     *
     * @throws IllegalArgumentException if there was already one -- it is fixed only once
     */
    public abstract void setHandler(HttpHandler h);

    /** The path, always absolute and beginning with {@code /}. */
    public abstract String getPath();

    /** The server it lives in. */
    public abstract HttpServer getServer();

    /** The shared attributes; mutable, and shared between requests. */
    public abstract Map<String, Object> getAttributes();

    /**
     * The list of filters, mutable.
     *
     * <p>It is modified by adding elements to it, not by replacing it. The order is the order of
     * execution.
     */
    public abstract List<Filter> getFilters();

    /**
     * It sets the authenticator and returns the one that was there, or {@code null}.
     *
     * @return the previous one
     */
    public abstract Authenticator setAuthenticator(Authenticator auth);

    /** The current authenticator, or {@code null}. */
    public abstract Authenticator getAuthenticator();
}
