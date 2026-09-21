package javax.net.ssl;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;

/**
 * Produces the {@link TrustManager}s that decide whom to trust.
 *
 * <p>It is useful because writing an {@link X509TrustManager} by hand is at once unnecessary and
 * dangerous: validating a certificate chain has more cases than one remembers --expiry, incomplete
 * chain, revocation, usage constraints-- and each one left out is a hole. This factory delivers the
 * provider's implementation, which already covers them.
 *
 * <p>Initializing it with {@code null} uses the system's default trust store, which is what one
 * wants almost always.
 */
public class TrustManagerFactory {

    private final TrustManagerFactorySpi factorySpi;
    private final Provider provider;
    private final String algorithm;

    /** The default algorithm: the {@code ssl.TrustManagerFactory.algorithm} property. */
    public static final String getDefaultAlgorithm() {
        String a = Security.getProperty("ssl.TrustManagerFactory.algorithm");
        return a == null ? "PKIX" : a;
    }

    /** For providers. */
    protected TrustManagerFactory(TrustManagerFactorySpi factorySpi, Provider provider,
            String algorithm) {
        this.factorySpi = factorySpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    /** This factory's algorithm. */
    public final String getAlgorithm() {
        return this.algorithm;
    }

    /**
     * From the first provider that offers that algorithm.
     *
     * @throws NoSuchAlgorithmException if none offers it
     */
    public static final TrustManagerFactory getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("algorithm");
        }
        Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            Provider.Service s = provs[i].getService("TrustManagerFactory", algorithm);
            if (s != null) {
                return build(s, provs[i], algorithm);
            }
        }
        throw new NoSuchAlgorithmException(algorithm + " TrustManagerFactory not available");
    }

    /**
     * From a named provider.
     *
     * @throws NoSuchProviderException if there is no provider with that name
     */
    public static final TrustManagerFactory getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(algorithm, p);
    }

    /**
     * From a concrete provider.
     *
     * @throws NoSuchAlgorithmException if that provider does not offer it
     */
    public static final TrustManagerFactory getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("algorithm");
        }
        Provider.Service s = provider.getService("TrustManagerFactory", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(algorithm + " TrustManagerFactory not available");
        }
        return build(s, provider, algorithm);
    }

    private static TrustManagerFactory build(Provider.Service s, Provider p, String algorithm)
            throws NoSuchAlgorithmException {
        try {
            Object spi = s.newInstance(null);
            if (!(spi instanceof TrustManagerFactorySpi)) {
                throw new NoSuchAlgorithmException(
                        "the provider did not return a TrustManagerFactorySpi for " + algorithm);
            }
            return new TrustManagerFactory((TrustManagerFactorySpi) spi, p, algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw e;
        } catch (Exception e) {
            throw new NoSuchAlgorithmException(
                    algorithm + " TrustManagerFactory not available", e);
        }
    }

    /** The provider that produced it. */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * From a store of trusted certificates; {@code null} uses the system's.
     *
     * <p>No password, and that is not an omission: a trust store keeps public certificates. There
     * is nothing secret to unlock.
     */
    public final void init(KeyStore ks) throws KeyStoreException {
        this.factorySpi.engineInit(ks);
    }

    /** From parameters; see {@link CertPathTrustManagerParameters}. */
    public final void init(ManagerFactoryParameters spec)
            throws InvalidAlgorithmParameterException {
        this.factorySpi.engineInit(spec);
    }

    /**
     * The trust managers.
     *
     * @throws IllegalStateException if {@code init} was not called first
     */
    public final TrustManager[] getTrustManagers() {
        return this.factorySpi.engineGetTrustManagers();
    }
}
