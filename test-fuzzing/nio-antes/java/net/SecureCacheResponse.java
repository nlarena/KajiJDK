package java.net;

import java.security.Principal;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

/**
 * A response taken from the cache that originally came over a secure connection.
 *
 * <h2>Why a separate type is needed</h2>
 *
 * <p>Because serving from the cache loses the channel. An ordinary {@link CacheResponse} hands over
 * the bytes and nothing more, and then code that decided something by looking at the server's
 * certificate —or the agreed cipher suite— <strong>cannot repeat that decision</strong> when the
 * response comes from disk.
 *
 * <p>This class is what keeps the cache from silently degrading security: it also stores what was
 * known about the channel at the moment the response was obtained, and gives it back. The information
 * is <em>historical</em> —it describes the original connection, not a current one— and that is
 * exactly the distinction to bear in mind when using it.
 *
 * @since 1.5
 */
public abstract class SecureCacheResponse extends CacheResponse {

    /** For the cache implementations. */
    public SecureCacheResponse() {
    }

    /** The cipher suite that had been agreed. */
    public abstract String getCipherSuite();

    /**
     * The certificates that had been presented, or {@code null} if none were.
     *
     * <p>The list goes from one's own towards the root CA, which is the protocol's order.
     */
    public abstract List<Certificate> getLocalCertificateChain();

    /**
     * The certificates the server had presented.
     *
     * @throws SSLPeerUnverifiedException if it had not authenticated — this can happen with a
     *     perfectly valid connection, because encrypting and authenticating are different things
     */
    public abstract List<Certificate> getServerCertificateChain()
            throws SSLPeerUnverifiedException;

    /**
     * Who the server was.
     *
     * @throws SSLPeerUnverifiedException if it had not authenticated
     */
    public abstract Principal getPeerPrincipal() throws SSLPeerUnverifiedException;

    /** Who one had presented as, or {@code null}. */
    public abstract Principal getLocalPrincipal();

    /**
     * The original session, if the cache kept it.
     *
     * <p>An {@link Optional} and with a body: it arrived after the rest of the class, and an old
     * cache has no reason to have kept the whole session. Empty means "I do not have it", which is a
     * legitimate answer and different from "there was none".
     *
     * @since 12
     */
    public Optional<SSLSession> getSSLSession() {
        return Optional.empty();
    }
}
