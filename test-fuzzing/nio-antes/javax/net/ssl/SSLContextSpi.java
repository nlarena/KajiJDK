package javax.net.ssl;

import java.security.KeyManagementException;
import java.security.SecureRandom;

/**
 * What a provider implements for an {@link SSLContext} to exist.
 *
 * <p>The last two methods have a body and the rest do not, and the difference says something: an
 * old provider did not know {@link SSLParameters}, so making them abstract would have broken it.
 * They arrive throwing {@link UnsupportedOperationException}, which is honest — the provider cannot
 * answer that question— and not a made-up value.
 */
public abstract class SSLContextSpi {

    public SSLContextSpi() {
    }

    /** Initializes with the sources of credentials, of trust and of randomness. */
    protected abstract void engineInit(KeyManager[] km, TrustManager[] tm, SecureRandom sr)
            throws KeyManagementException;

    /** This context's client socket factory. */
    protected abstract SSLSocketFactory engineGetSocketFactory();

    /** The server socket factory. */
    protected abstract SSLServerSocketFactory engineGetServerSocketFactory();

    /** An engine without peer data. */
    protected abstract SSLEngine engineCreateSSLEngine();

    /** An engine with the suggested peer, which enables resuming sessions and sending SNI. */
    protected abstract SSLEngine engineCreateSSLEngine(String host, int port);

    /** The server-side session context. */
    protected abstract SSLSessionContext engineGetServerSessionContext();

    /** The client-side session context. */
    protected abstract SSLSessionContext engineGetClientSessionContext();

    /**
     * The default parameters.
     *
     * @throws UnsupportedOperationException if the provider cannot report them
     */
    protected SSLParameters engineGetDefaultSSLParameters() {
        throw new UnsupportedOperationException(
                "this provider does not report its default parameters");
    }

    /**
     * The parameters it supports.
     *
     * @throws UnsupportedOperationException if the provider cannot report them
     */
    protected SSLParameters engineGetSupportedSSLParameters() {
        throw new UnsupportedOperationException(
                "this provider does not report the parameters it supports");
    }
}
