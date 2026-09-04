package com.sun.net.httpserver;

import java.net.InetSocketAddress;

import javax.net.ssl.SSLParameters;

/**
 * Los parametros de TLS de <strong>una</strong> conexion entrante, para que
 * {@link HttpsConfigurator#configure} los ajuste.
 *
 * <p>Trae {@link #getClientAddress}, que es lo que hace util a todo el mecanismo: la configuracion
 * puede depender de quien se esta conectando. Sin ese dato esto seria un objeto de configuracion
 * global y no haria falta pasarlo por conexion.
 *
 * <p>Los setters propios existen para el caso simple —cambiar solo las suites, solo los protocolos—
 * sin tener que construir un {@link SSLParameters} entero. {@link #setSSLParameters} es la via
 * completa, y la que el configurador por omision usa.
 */
public abstract class HttpsParameters {

    private String[] cipherSuites;
    private String[] protocols;
    private boolean wantClientAuth;
    private boolean needClientAuth;

    /** Para las implementaciones. */
    protected HttpsParameters() {
    }

    /** El configurador que dio origen a esta conexion. */
    public abstract HttpsConfigurator getHttpsConfigurator();

    /** De donde viene el cliente; ver la nota de la clase. */
    public abstract InetSocketAddress getClientAddress();

    /** Aplica una configuracion completa. */
    public abstract void setSSLParameters(SSLParameters params);

    /** Las suites fijadas, o {@code null}. */
    public String[] getCipherSuites() {
        return this.cipherSuites == null ? null : this.cipherSuites.clone();
    }

    /** Fija las suites. */
    public void setCipherSuites(String[] cipherSuites) {
        this.cipherSuites = cipherSuites == null ? null : cipherSuites.clone();
    }

    /** Los protocolos fijados, o {@code null}. */
    public String[] getProtocols() {
        return this.protocols == null ? null : this.protocols.clone();
    }

    /** Fija los protocolos. */
    public void setProtocols(String[] protocols) {
        this.protocols = protocols == null ? null : protocols.clone();
    }

    /** Si se pide certificado de cliente sin exigirlo. */
    public boolean getWantClientAuth() {
        return this.wantClientAuth;
    }

    /** Lo pide sin exigirlo. */
    public void setWantClientAuth(boolean wantClientAuth) {
        this.wantClientAuth = wantClientAuth;
    }

    /** Si se exige certificado de cliente. */
    public boolean getNeedClientAuth() {
        return this.needClientAuth;
    }

    /** Lo exige: sin certificado, no hay conexion. */
    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }
}
