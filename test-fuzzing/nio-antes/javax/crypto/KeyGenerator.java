package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Generates symmetric keys.
 *
 * <h2>Why random bytes are not enough</h2>
 *
 * <p>For many algorithms they would be. For others they are not: DES and DESede have parity bits and
 * weak keys that have to be discarded, and a key for an algorithm with structure has to fall in a
 * range. Drawing bytes and calling them a key would produce invalid keys now and then, and --worse--
 * valid but weak ones.
 *
 * <h2>Configuring it is optional</h2>
 *
 * <p>Unconfigured, it uses the algorithm's default size, which is the recommended one. Forcing the
 * choice would be worse: it is how 512-bit keys end up written in programs nobody looked at again.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>The machinery works in full, but no registered provider offers key generators, so
 * {@link #getInstance} throws {@link NoSuchAlgorithmException} for any name. Registering a provider
 * of one's own makes it work.
 *
 * @since 1.4
 */
public class KeyGenerator {

    private final KeyGeneratorSpi spi;
    private final Provider provider;
    private final String algorithm;

    /**
     * One around that implementation.
     *
     * @param keyGenSpi the implementation
     * @param provider whose it is
     * @param algorithm the name it was asked for by
     */
    protected KeyGenerator(KeyGeneratorSpi keyGenSpi, Provider provider, String algorithm) {
        this.spi = keyGenSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    /**
     * Which algorithm it generates keys for.
     *
     * @return the algorithm
     */
    public final String getAlgorithm() {
        return this.algorithm;
    }

    /**
     * One for that algorithm.
     *
     * @param algorithm the algorithm
     * @return the engine
     * @throws NoSuchAlgorithmException if no provider has it
     * @throws NullPointerException if the algorithm is {@code null}
     */
    public static final KeyGenerator getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            final Provider.Service s = provs[i].getService("KeyGenerator", algorithm);
            if (s != null) {
                return build(s, algorithm);
            }
        }
        throw new NoSuchAlgorithmException(algorithm + " KeyGenerator not available");
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
    public static final KeyGenerator getInstance(String algorithm, String provider)
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
    public static final KeyGenerator getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider.Service s = provider.getService("KeyGenerator", algorithm);
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
     * Configures it with the default size.
     *
     * @param random where to take the randomness from
     */
    public final void init(SecureRandom random) {
        this.spi.engineInit(random);
    }

    /**
     * Configures it with parameters.
     *
     * @param params the parameters
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     */
    public final void init(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
        init(params, new SecureRandom());
    }

    /**
     * Configures it with parameters, saying where to take the randomness from.
     *
     * @param params the parameters
     * @param random where to take the randomness from
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     */
    public final void init(AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidAlgorithmParameterException {
        this.spi.engineInit(params, random);
    }

    /**
     * Configures it with a size.
     *
     * @param keysize the size in bits
     * @throws java.security.InvalidParameterException if that size is no good
     */
    public final void init(int keysize) {
        init(keysize, new SecureRandom());
    }

    /**
     * Configures it with a size, saying where to take the randomness from.
     *
     * @param keysize the size in bits
     * @param random where to take the randomness from
     * @throws java.security.InvalidParameterException if that size is no good
     */
    public final void init(int keysize, SecureRandom random) {
        this.spi.engineInit(keysize, random);
    }

    /**
     * Generates a key.
     *
     * @return the key
     */
    public final SecretKey generateKey() {
        return this.spi.engineGenerateKey();
    }

    private static KeyGenerator build(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        final Object o = s.newInstance(null);
        if (!(o instanceof KeyGeneratorSpi)) {
            throw new NoSuchAlgorithmException(
                    "class configured for KeyGenerator is not a KeyGeneratorSpi: " + s.getClassName());
        }
        return new KeyGenerator((KeyGeneratorSpi) o, s.getProvider(), algorithm);
    }
}
