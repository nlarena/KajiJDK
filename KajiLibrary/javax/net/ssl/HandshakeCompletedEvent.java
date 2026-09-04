package javax.net.ssl;

import java.security.Principal;
import java.security.cert.Certificate;
import java.util.EventObject;

/**
 * Termino un handshake sobre un {@link SSLSocket}.
 *
 * <p>Casi todos sus metodos delegan en la {@link SSLSession}, y eso no lo vuelve superfluo: lo que
 * aporta es <strong>congelar el momento</strong>. Un socket puede renegociar y cambiar de sesion, y
 * este evento sigue apuntando a la que se acababa de acordar cuando se lo emitio.
 */
public class HandshakeCompletedEvent extends EventObject {

    private static final long serialVersionUID = 7914963744257769778L;

    private final transient SSLSession session;

    public HandshakeCompletedEvent(SSLSocket sock, SSLSession s) {
        super(sock);
        this.session = s;
    }

    /** La sesion que quedo acordada. */
    public SSLSession getSession() {
        return this.session;
    }

    /** La suite de cifrado acordada. */
    public String getCipherSuite() {
        return this.session.getCipherSuite();
    }

    /** Los certificados que se presentaron, o {@code null}. */
    public Certificate[] getLocalCertificates() {
        return this.session.getLocalCertificates();
    }

    /**
     * Los certificados del par.
     *
     * @throws SSLPeerUnverifiedException si el par no se autentico
     */
    public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
        return this.session.getPeerCertificates();
    }

    /**
     * Los certificados del par, en el tipo obsoleto.
     *
     * @deprecated {@code javax.security.cert} quedo obsoleto; usar {@link #getPeerCertificates}
     */
    @Deprecated(since = "9")
    public javax.security.cert.X509Certificate[] getPeerCertificateChain()
            throws SSLPeerUnverifiedException {
        return this.session.getPeerCertificateChain();
    }

    /**
     * Quien es el par.
     *
     * @throws SSLPeerUnverifiedException si no se autentico
     */
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        return this.session.getPeerPrincipal();
    }

    /** Quien nos presentamos como, o {@code null}. */
    public Principal getLocalPrincipal() {
        return this.session.getLocalPrincipal();
    }

    /** El socket donde paso. */
    public SSLSocket getSocket() {
        return (SSLSocket) getSource();
    }
}
