package com.sun.net.httpserver;

import javax.net.ssl.SSLSession;

/**
 * An {@link HttpExchange} that arrived over TLS.
 *
 * <p>The only thing it adds is {@link #getSSLSession}, and with that it is enough: it is how
 * the handler finds out what certificate the client presented and what suite was agreed.
 * Without that, client certificate authentication would be invisible from above -- the server
 * would have verified it and the handler would have no way of knowing who it is.
 */
public abstract class HttpsExchange extends HttpExchange {

    /** For the implementations. */
    protected HttpsExchange() {
    }

    /** This connection's TLS session. */
    public abstract SSLSession getSSLSession();
}
