package javax.net.ssl;

import java.security.Principal;
import java.security.cert.Certificate;
import java.util.EventObject;

/**
 * A handshake finished over an {@link SSLSocket}.
 *
 * <p>Almost all its methods delegate to the {@link SSLSession}, and that does not make it
 * superfluous: what it contributes is <strong>freezing the moment</strong>. A socket can
 * renegotiate and change session, and this event keeps pointing at the one just agreed when it was
 * emitted.
 */
public class HandshakeCompletedEvent extends EventObject {

    private static final long serialVersionUID = 7914963744257769778L;

    private final transient SSLSession session;

    public HandshakeCompletedEvent(SSLSocket sock, SSLSession s) {
        super(sock);
        this.session = s;
    }

    /** The session that was agreed. */
    public SSLSession getSession() {
        return this.session;
    }

    /** The agreed cipher suite. */
    public String getCipherSuite() {
        return this.session.getCipherSuite();
    }

    /** The certificates that were presented, or {@code null}. */
    public Certificate[] getLocalCertificates() {
        return this.session.getLocalCertificates();
    }

    /**
     * The peer's certificates.
     *
     * @throws SSLPeerUnverifiedException if the peer did not authenticate
     */
    public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
        return this.session.getPeerCertificates();
    }

    /**
     * The peer's certificates, in the obsolete type.
     *
     * @deprecated {@code javax.security.cert} became obsolete; use {@link #getPeerCertificates}
     */
    @Deprecated(since = "9")
    public javax.security.cert.X509Certificate[] getPeerCertificateChain()
            throws SSLPeerUnverifiedException {
        return this.session.getPeerCertificateChain();
    }

    /**
     * Who the peer is.
     *
     * @throws SSLPeerUnverifiedException if it did not authenticate
     */
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        return this.session.getPeerPrincipal();
    }

    /** Who we presented ourselves as, or {@code null}. */
    public Principal getLocalPrincipal() {
        return this.session.getLocalPrincipal();
    }

    /** The socket where it happened. */
    public SSLSocket getSocket() {
        return (SSLSocket) getSource();
    }
}
