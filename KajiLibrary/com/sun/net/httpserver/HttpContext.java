package com.sun.net.httpserver;

import java.util.List;
import java.util.Map;

/**
 * La union entre una ruta y quien la atiende, con todo lo que se le cuelga alrededor.
 *
 * <h2>Por que {@code createContext} devuelve esto y no {@code void}</h2>
 *
 * <p>Porque registrar la ruta es solo el principio: despues hay que ponerle filtros, quizas un
 * autenticador, quizas atributos compartidos. Devolver el contexto deja hacer todo eso sin un
 * segundo registro y sin que el servidor tenga que exponer un metodo por cada cosa configurable.
 *
 * <p>{@link #getAttributes} es un mapa mutable y compartido por todos los pedidos de esta ruta: es
 * donde va el estado que un manejador necesita entre pedidos, y por eso mismo hay que sincronizarlo
 * si se escribe.
 */
public abstract class HttpContext {

    /** Para las implementaciones. */
    protected HttpContext() {
    }

    /** Quien atiende esta ruta. */
    public abstract HttpHandler getHandler();

    /**
     * Cambia quien atiende.
     *
     * @throws IllegalArgumentException si ya habia uno — se fija una sola vez
     */
    public abstract void setHandler(HttpHandler h);

    /** La ruta, siempre absoluta y empezando con {@code /}. */
    public abstract String getPath();

    /** El servidor donde vive. */
    public abstract HttpServer getServer();

    /** Los atributos compartidos; mutable, y compartido entre pedidos. */
    public abstract Map<String, Object> getAttributes();

    /**
     * La lista de filtros, mutable.
     *
     * <p>Se modifica agregandole elementos, no reemplazandola. El orden es el de ejecucion.
     */
    public abstract List<Filter> getFilters();

    /**
     * Pone el autenticador y devuelve el que estaba, o {@code null}.
     *
     * @return el anterior
     */
    public abstract Authenticator setAuthenticator(Authenticator auth);

    /** El autenticador actual, o {@code null}. */
    public abstract Authenticator getAuthenticator();
}
