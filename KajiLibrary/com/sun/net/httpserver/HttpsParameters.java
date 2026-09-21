package com.sun.net.httpserver;

import java.net.InetSocketAddress;

import javax.net.ssl.SSLParameters;

/**
 * The TLS parameters of <strong>one</strong> incoming connection, for
 * {@link HttpsConfigurator#configure} to adjust them.
 *
 * <p>It brings {@link #getClientAddress}, which is what makes the whole mechanism useful: the
 * configuration may depend on who is connecting. Without that datum this would be a global
 * configuration object and there would be no need to pass it per connection.
 *
 * <p>The setters of its own exist for the simple case -- changing only the suites, only the
 * protocols -- without having to build a whole {@link SSLParameters}.
 * {@link #setSSLParameters} is the complete way, and the one the default configurator uses.
 */
public abstract class HttpsParameters {

    private String[] cipherSuites;
    private String[] protocols;
    private boolean wantClientAuth;
    private boolean needClientAuth;

    /** For the implementations. */
    protected HttpsParameters() {
    }

    /** The configurator this connection came from. */
    public abstract HttpsConfigurator getHttpsConfigurator();

    /** Where the client comes from; see the class note. */
    public abstract InetSocketAddress getClientAddress();

    /** It applies a complete configuration. */
    public abstract void setSSLParameters(SSLParameters params);

    /** The suites that were fixed, or {@code null}. */
    public String[] getCipherSuites() {
        return this.cipherSuites == null ? null : this.cipherSuites.clone();
    }

    /** It fixes the suites. */
    public void setCipherSuites(String[] cipherSuites) {
        this.cipherSuites = cipherSuites == null ? null : cipherSuites.clone();
    }

    /** The protocols that were fixed, or {@code null}. */
    public String[] getProtocols() {
        return this.protocols == null ? null : this.protocols.clone();
    }

    /** It fixes the protocols. */
    public void setProtocols(String[] protocols) {
        this.protocols = protocols == null ? null : protocols.clone();
    }

    /** Whether a client certificate is asked for without requiring it. */
    public boolean getWantClientAuth() {
        return this.wantClientAuth;
    }

    /** It asks for it without requiring it. */
    public void setWantClientAuth(boolean wantClientAuth) {
        this.wantClientAuth = wantClientAuth;
    }

    /** Whether a client certificate is required. */
    public boolean getNeedClientAuth() {
        return this.needClientAuth;
    }

    /** It requires it: with no certificate, there is no connection. */
    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }
}
