package javax.net.ssl;

import java.security.Principal;
import java.security.cert.Certificate;

/**
 * What two ends negotiated once and can reuse many times.
 *
 * <h2>Why a session is not a connection</h2>
 *
 * <p>It is the central distinction of this type. The complete handshake is expensive --asymmetric
 * cryptography, several round trips-- and an application opens and closes connections all the time.
 * The session keeps what was agreed (the suite, the master secret, the certificates) so that a new
 * connection can <em>resume</em> it with an abbreviated handshake. Many connections, one session.
 *
 * <p>Hence {@link #invalidate} closes nothing: it only forbids future connections from resuming it.
 *
 * <h2>The value store</h2>
 *
 * <p>{@link #putValue} and company let application data hang from it, and that is useful precisely
 * because the session outlives the connection: it is where to put something valid for all the
 * connections with that peer. A value implementing {@link SSLSessionBindingListener} finds out when
 * it goes in and out.
 */
public interface SSLSession {

    /** The identifier the server gave it. */
    byte[] getId();

    /** The context managing it, or {@code null} if it is in none. */
    SSLSessionContext getSessionContext();

    /** When it was created, in milliseconds since the epoch. */
    long getCreationTime();

    /** When it was last used. It is what the context looks at to expire it. */
    long getLastAccessedTime();

    /**
     * Forbids resuming it.
     *
     * <p>It does not close the connections already using it: those go on. What it prevents is a new
     * connection saving itself the complete handshake.
     */
    void invalidate();

    /** Whether it can still be resumed. */
    boolean isValid();

    /** Keeps an application value. */
    void putValue(String name, Object value);

    /** The value kept with that name, or {@code null}. */
    Object getValue(String name);

    /** Removes a value. */
    void removeValue(String name);

    /** The names of the kept values. */
    String[] getValueNames();

    /**
     * The certificates the peer presented.
     *
     * @throws SSLPeerUnverifiedException if the peer did not authenticate — which may happen with a
     *     perfectly valid session, because encrypting and authenticating are different things
     */
    Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException;

    /** The certificates that were presented, or {@code null} if none was presented. */
    Certificate[] getLocalCertificates();

    /**
     * The peer's certificates, in the old {@code javax.security.cert} type.
     *
     * @deprecated that package became obsolete; use {@link #getPeerCertificates}
     */
    @Deprecated(since = "9")
    default javax.security.cert.X509Certificate[] getPeerCertificateChain()
            throws SSLPeerUnverifiedException {
        throw new UnsupportedOperationException(
                "this session does not support the obsolete javax.security.cert type");
    }

    /**
     * Who the peer is.
     *
     * @throws SSLPeerUnverifiedException if it did not authenticate
     */
    Principal getPeerPrincipal() throws SSLPeerUnverifiedException;

    /** Who we presented ourselves as, or {@code null}. */
    Principal getLocalPrincipal();

    /** The agreed cipher suite. */
    String getCipherSuite();

    /** The agreed protocol version. */
    String getProtocol();

    /** The peer's name as it was asked for, neither resolved nor verified. */
    String getPeerHost();

    /** The peer's port. */
    int getPeerPort();

    /**
     * The largest buffer needed for a network record.
     *
     * <p>It is larger than {@link #getApplicationBufferSize}: a TLS record carries a header,
     * padding and a MAC besides the data. Whoever uses an {@link SSLEngine} sizes with these two
     * numbers and not by guessing, or eats a {@code BUFFER_OVERFLOW} at the worst moment.
     */
    int getPacketBufferSize();

    /** The largest data it can deliver at once. */
    int getApplicationBufferSize();
}
