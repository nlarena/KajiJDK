package com.sun.net.httpserver;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * It decides how TLS is configured on each incoming connection of an {@link HttpsServer}.
 *
 * <h2>Why it is a class with an overridable method and not a configuration object</h2>
 *
 * <p>Because the configuration may depend <strong>on who connects</strong>.
 * {@link #configure} receives {@link HttpsParameters} that already bring the client's address,
 * so a certificate may be required of some and not of others, or the suites restricted by
 * origin. A fixed object would not allow that.
 *
 * <p>The default implementation applies the context's default parameters, which is what is
 * reasonable when there is no need to distinguish.
 */
public class HttpsConfigurator {

    private final SSLContext context;

    /**
     * @throws NullPointerException if the context is {@code null}
     */
    public HttpsConfigurator(SSLContext context) {
        if (context == null) {
            throw new NullPointerException("context");
        }
        this.context = context;
    }

    /** The context that provides the credentials and the trust policy. */
    public SSLContext getSSLContext() {
        return this.context;
    }

    /**
     * It adjusts an incoming connection's parameters.
     *
     * <p>By default it gives it the context's. Whoever overrides it has to call
     * {@link HttpsParameters#setSSLParameters} or the connection is left unconfigured.
     */
    public void configure(HttpsParameters params) {
        params.setSSLParameters(getSSLContext().getDefaultSSLParameters());
    }
}
