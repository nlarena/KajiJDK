package javax.net.ssl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;

/**
 * The factory of everything else in this package.
 *
 * <h2>What a context brings together</h2>
 *
 * <p>Three things that are loose until then: the own credentials ({@link KeyManager}), the trust
 * policy ({@link TrustManager}) and the source of randomness. With the three configured, the
 * context produces sockets, engines and factories that come with that configuration inside.
 *
 * <p>It is what allows two different policies in the same program --a connection to an internal
 * service with its own CA, another to the Internet with the public CAs-- which the global
 * configuration through system properties does not allow.
 *
 * <h2>Without a TLS provider installed</h2>
 *
 * <p>{@link #getInstance} throws {@link NoSuchAlgorithmException}, which is the right answer and
 * not a disguised shortcoming: there is no provider offering that protocol. It is the same
 * criterion {@code MessageDigest} follows in this library. Whoever wants TLS registers a provider
 * with {@link Security#addProvider} and this starts working without touching a line here.
 */
public class SSLContext {

    private static SSLContext theDefault;

    private final SSLContextSpi contextSpi;
    private final Provider provider;
    private final String protocol;

    /** For providers. */
    protected SSLContext(SSLContextSpi contextSpi, Provider provider, String protocol) {
        this.contextSpi = contextSpi;
        this.provider = provider;
        this.protocol = protocol;
    }

    /**
     * The default context, already initialized.
     *
     * @throws NoSuchAlgorithmException if no provider offers the default protocol
     */
    public static synchronized SSLContext getDefault() throws NoSuchAlgorithmException {
        if (theDefault == null) {
            theDefault = getInstance("Default");
        }
        return theDefault;
    }

    /**
     * Changes the default context.
     *
     * @throws NullPointerException if it is {@code null}
     */
    public static synchronized void setDefault(SSLContext context) {
        if (context == null) {
            throw new NullPointerException("context");
        }
        theDefault = context;
    }

    /**
     * The context of that protocol, from the first provider that offers it.
     *
     * @throws NoSuchAlgorithmException if none offers it
     */
    public static SSLContext getInstance(String protocol) throws NoSuchAlgorithmException {
        if (protocol == null) {
            throw new NullPointerException("protocol");
        }
        Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            Provider.Service s = provs[i].getService("SSLContext", protocol);
            if (s != null) {
                return build(s, provs[i], protocol);
            }
        }
        throw new NoSuchAlgorithmException(protocol + " SSLContext not available");
    }

    /**
     * From a named provider.
     *
     * @throws NoSuchProviderException if there is no provider with that name
     */
    public static SSLContext getInstance(String protocol, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(protocol, p);
    }

    /**
     * From a concrete provider.
     *
     * @throws NoSuchAlgorithmException if that provider does not offer the protocol
     */
    public static SSLContext getInstance(String protocol, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (protocol == null) {
            throw new NullPointerException("protocol");
        }
        Provider.Service s = provider.getService("SSLContext", protocol);
        if (s == null) {
            throw new NoSuchAlgorithmException(protocol + " SSLContext not available");
        }
        return build(s, provider, protocol);
    }

    private static SSLContext build(Provider.Service s, Provider p, String protocol)
            throws NoSuchAlgorithmException {
        try {
            Object spi = s.newInstance(null);
            if (!(spi instanceof SSLContextSpi)) {
                throw new NoSuchAlgorithmException(
                        "the provider did not return an SSLContextSpi for " + protocol);
            }
            return new SSLContext((SSLContextSpi) spi, p, protocol);
        } catch (NoSuchAlgorithmException e) {
            throw e;
        } catch (Exception e) {
            throw new NoSuchAlgorithmException(protocol + " SSLContext not available", e);
        }
    }

    /** This context's protocol. */
    public final String getProtocol() {
        return this.protocol;
    }

    /** The provider that produced it. */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * Configures the three sources.
     *
     * <p>Any of the three may be {@code null}, and then the provider's default is used. With {@code
     * null} for the trust managers, that means the CAs the system already had — which is what one
     * wants almost always, and it is as well to know that is what happens.
     */
    public final void init(KeyManager[] km, TrustManager[] tm, SecureRandom random)
            throws KeyManagementException {
        this.contextSpi.engineInit(km, tm, random);
    }

    /** The client socket factory with this configuration. */
    public final SSLSocketFactory getSocketFactory() {
        return this.contextSpi.engineGetSocketFactory();
    }

    /** The server socket factory with this configuration. */
    public final SSLServerSocketFactory getServerSocketFactory() {
        return this.contextSpi.engineGetServerSocketFactory();
    }

    /** An engine without peer data. */
    public final SSLEngine createSSLEngine() {
        return this.contextSpi.engineCreateSSLEngine();
    }

    /** An engine with the suggested peer, which enables resuming a session and sending SNI. */
    public final SSLEngine createSSLEngine(String peerHost, int peerPort) {
        return this.contextSpi.engineCreateSSLEngine(peerHost, peerPort);
    }

    /** The server-side sessions. */
    public final SSLSessionContext getServerSessionContext() {
        return this.contextSpi.engineGetServerSessionContext();
    }

    /** The client-side sessions. */
    public final SSLSessionContext getClientSessionContext() {
        return this.contextSpi.engineGetClientSessionContext();
    }

    /** This context's default parameters. */
    public final SSLParameters getDefaultSSLParameters() {
        return this.contextSpi.engineGetDefaultSSLParameters();
    }

    /** Everything this context supports, whether enabled or not. */
    public final SSLParameters getSupportedSSLParameters() {
        return this.contextSpi.engineGetSupportedSSLParameters();
    }
}
