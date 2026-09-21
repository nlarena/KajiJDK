package javax.net.ssl;

import java.util.Collections;
import java.util.List;

/**
 * An {@link SSLSession} that also reports what modern TLS negotiates and the original interface did
 * not foresee.
 *
 * <p>It is an abstract class and not new methods on the interface for the usual reason: adding them
 * to {@code SSLSession} would have broken whoever implemented it. The ones with a body here return
 * the empty value or throw {@link UnsupportedOperationException} depending on whether "nothing" is
 * a sensible answer.
 */
public abstract class ExtendedSSLSession implements SSLSession {

    public ExtendedSSLSession() {
    }

    /** The signature algorithms this end accepts, in order of preference. */
    public abstract String[] getLocalSupportedSignatureAlgorithms();

    /** The ones the peer declared, or {@code null} if it declared none. */
    public abstract String[] getPeerSupportedSignatureAlgorithms();

    /**
     * The SNI names the client asked for.
     *
     * <p>Empty by default, which is right: having asked for none is normal and not an error.
     *
     * @throws UnsupportedOperationException if the implementation does not support it
     */
    public List<SNIServerName> getRequestedServerNames() {
        throw new UnsupportedOperationException("this session does not report requested SNI names");
    }

    /**
     * The OCSP responses stapled to the handshake.
     *
     * <p>Empty by default. Stapling the revocation response to the handshake saves the client from
     * having to look it up on its own — another connection, another point of failure, and a leak of
     * whom it is connecting to.
     */
    public List<byte[]> getStatusResponses() {
        return Collections.<byte[]>emptyList();
    }

    /**
     * Derives a key from the session's secret.
     *
     * @throws UnsupportedOperationException if the implementation does not support it
     */
    public javax.crypto.SecretKey exportKeyingMaterialKey(String keyAlg, String label,
            byte[] context, int length) throws SSLKeyException {
        throw new UnsupportedOperationException("this session does not export keying material");
    }

    /**
     * The same, as raw bytes.
     *
     * @throws UnsupportedOperationException if the implementation does not support it
     */
    public byte[] exportKeyingMaterialData(String label, byte[] context, int length)
            throws SSLKeyException {
        throw new UnsupportedOperationException("this session does not export keying material");
    }
}
