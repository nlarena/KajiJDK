package javax.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * Converts between the two shapes of a symmetric key.
 *
 * <h2>The two shapes</h2>
 *
 * <p>The opaque one --{@link SecretKey}-- may live inside a device and refuse to be looked at. The
 * transparent one --{@link KeySpec}-- is material the program assembles or reads from a file. This
 * factory is the only road between the two.
 *
 * <h2>The commonest use</h2>
 *
 * <p>Deriving a key from a password: a {@link javax.crypto.spec.PBEKeySpec} goes in --password,
 * salt, iteration count-- and a {@link SecretKey} comes out. The iteration count is what makes
 * trying passwords expensive, and that is why it is not chosen by eye.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>The machinery works in full, but no registered provider offers symmetric key factories, so
 * {@link #getInstance} throws {@link NoSuchAlgorithmException} for any name. Registering a provider
 * of one's own makes it work.
 *
 * @since 1.4
 */
public class SecretKeyFactory {

    private final SecretKeyFactorySpi spi;
    private final Provider provider;
    private final String algorithm;

    /**
     * One around that implementation.
     *
     * @param keyFacSpi the implementation
     * @param provider whose it is
     * @param algorithm the name it was asked for by
     */
    protected SecretKeyFactory(SecretKeyFactorySpi keyFacSpi, Provider provider,
            String algorithm) {
        this.spi = keyFacSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    /**
     * One for that algorithm.
     *
     * @param algorithm the algorithm
     * @return the engine
     * @throws NoSuchAlgorithmException if no provider has it
     * @throws NullPointerException if the algorithm is {@code null}
     */
    public static final SecretKeyFactory getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            final Provider.Service s = provs[i].getService("SecretKeyFactory", algorithm);
            if (s != null) {
                return build(s, algorithm);
            }
        }
        throw new NoSuchAlgorithmException(algorithm + " SecretKeyFactory not available");
    }

    /**
     * One from that provider, named.
     *
     * @param algorithm the algorithm
     * @param provider the provider's name
     * @return the engine
     * @throws NoSuchAlgorithmException if that provider does not have it
     * @throws NoSuchProviderException if there is no provider by that name
     * @throws IllegalArgumentException if the provider's name is {@code null} or empty
     */
    public static final SecretKeyFactory getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        final Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(algorithm, p);
    }

    /**
     * One from that provider.
     *
     * @param algorithm the algorithm
     * @param provider the provider
     * @return the engine
     * @throws NoSuchAlgorithmException if that provider does not have it
     * @throws IllegalArgumentException if the provider is {@code null}
     */
    public static final SecretKeyFactory getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider.Service s = provider.getService("SecretKeyFactory", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                    "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return build(s, algorithm);
    }

    /**
     * Whose the implementation is.
     *
     * @return the provider
     */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * Which algorithm it makes keys for.
     *
     * @return the algorithm
     */
    public final String getAlgorithm() {
        return this.algorithm;
    }

    /**
     * Assembles a key out of its description.
     *
     * @param keySpec the description
     * @return the key
     * @throws InvalidKeySpecException if the description is no good for this algorithm
     */
    public final SecretKey generateSecret(KeySpec keySpec) throws InvalidKeySpecException {
        return this.spi.engineGenerateSecret(keySpec);
    }

    /**
     * Describes a key.
     *
     * @param key the key
     * @param keySpec which description is wanted
     * @return the description
     * @throws InvalidKeySpecException if the key cannot be described that way
     */
    public final KeySpec getKeySpec(SecretKey key, Class<?> keySpec)
            throws InvalidKeySpecException {
        return this.spi.engineGetKeySpec(key, keySpec);
    }

    /**
     * Converts a key from another provider into one of this one's.
     *
     * @param key the key
     * @return this provider's equivalent key
     * @throws InvalidKeyException if it cannot be converted
     */
    public final SecretKey translateKey(SecretKey key) throws InvalidKeyException {
        return this.spi.engineTranslateKey(key);
    }

    private static SecretKeyFactory build(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        final Object o = s.newInstance(null);
        if (!(o instanceof SecretKeyFactorySpi)) {
            throw new NoSuchAlgorithmException(
                    "class configured for SecretKeyFactory is not a SecretKeyFactorySpi: " + s.getClassName());
        }
        return new SecretKeyFactory((SecretKeyFactorySpi) o, s.getProvider(), algorithm);
    }
}
