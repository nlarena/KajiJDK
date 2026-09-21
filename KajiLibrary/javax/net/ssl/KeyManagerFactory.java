package javax.net.ssl;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;

/**
 * Produces the {@link KeyManager}s that present the own credentials.
 *
 * <p>The mirror of {@link TrustManagerFactory}: that one decides whom to trust, this one decides
 * what to present. The visible difference is that {@link #init(KeyStore, char[])}
 * <strong>does</strong> take a password — a key store keeps private keys, and unlocking them is the
 * whole point.
 */
public class KeyManagerFactory {

    private final KeyManagerFactorySpi factorySpi;
    private final Provider provider;
    private final String algorithm;

    /** The default algorithm: the {@code ssl.KeyManagerFactory.algorithm} property. */
    public static final String getDefaultAlgorithm() {
        String a = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        return a == null ? "SunX509" : a;
    }

    /** For providers. */
    protected KeyManagerFactory(KeyManagerFactorySpi factorySpi, Provider provider,
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
    public static final KeyManagerFactory getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("algorithm");
        }
        Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            Provider.Service s = provs[i].getService("KeyManagerFactory", algorithm);
            if (s != null) {
                return build(s, provs[i], algorithm);
            }
        }
        throw new NoSuchAlgorithmException(algorithm + " KeyManagerFactory not available");
    }

    /**
     * From a named provider.
     *
     * @throws NoSuchProviderException if there is no provider with that name
     */
    public static final KeyManagerFactory getInstance(String algorithm, String provider)
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
    public static final KeyManagerFactory getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("algorithm");
        }
        Provider.Service s = provider.getService("KeyManagerFactory", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(algorithm + " KeyManagerFactory not available");
        }
        return build(s, provider, algorithm);
    }

    private static KeyManagerFactory build(Provider.Service s, Provider p, String algorithm)
            throws NoSuchAlgorithmException {
        try {
            Object spi = s.newInstance(null);
            if (!(spi instanceof KeyManagerFactorySpi)) {
                throw new NoSuchAlgorithmException(
                        "the provider did not return a KeyManagerFactorySpi for " + algorithm);
            }
            return new KeyManagerFactory((KeyManagerFactorySpi) spi, p, algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw e;
        } catch (Exception e) {
            throw new NoSuchAlgorithmException(algorithm + " KeyManagerFactory not available", e);
        }
    }

    /** The provider that produced it. */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * From a key store and its password.
     *
     * <p>The password may be {@code null} if the keys are not protected separately; the array is
     * best cleared afterwards, because a {@code char[]} in memory lives until somebody overwrites
     * it.
     */
    public final void init(KeyStore ks, char[] password)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        this.factorySpi.engineInit(ks, password);
    }

    /** From parameters; see {@link KeyStoreBuilderParameters}. */
    public final void init(ManagerFactoryParameters spec)
            throws InvalidAlgorithmParameterException {
        this.factorySpi.engineInit(spec);
    }

    /**
     * The key managers, one per type.
     *
     * @throws IllegalStateException if {@code init} was not called first
     */
    public final KeyManager[] getKeyManagers() {
        return this.factorySpi.engineGetKeyManagers();
    }
}
