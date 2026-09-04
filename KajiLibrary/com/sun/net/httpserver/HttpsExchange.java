package com.sun.net.httpserver;

import javax.net.ssl.SSLSession;

/**
 * Un {@link HttpExchange} que llego por TLS.
 *
 * <p>Lo unico que agrega es {@link #getSSLSession}, y con eso alcanza: es como el manejador averigua
 * que certificado presento el cliente y que suite se acordo. Sin eso, la autenticacion por
 * certificado de cliente seria invisible desde arriba — el servidor la habria verificado y el
 * manejador no tendria forma de saber quien es.
 */
public abstract class HttpsExchange extends HttpExchange {

    /** Para las implementaciones. */
    protected HttpsExchange() {
    }

    /** La sesion TLS de esta conexion. */
    public abstract SSLSession getSSLSession();
}
