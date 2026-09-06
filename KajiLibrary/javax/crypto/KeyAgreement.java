package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Two parties end up with the same key without that key ever having travelled.
 *
 * <h2>How that can be</h2>
 *
 * <p>Each party sends its public half and combines it with its private half. The arithmetic is built
 * so that both combinations come out the same, and so that watching the two public messages go by is
 * not enough to work it out. It is the only way for two machines that never spoke to share a secret
 * over a channel anybody can read.
 *
 * <h2>What it does not give</h2>
 *
 * <p>It does not say whom the agreement was with. Somebody in the middle can agree a key with each
 * side and translate between the two without either noticing. That is why a key agreement always
 * comes with something that authenticates the other side: a certificate, a signature, a key known in
 * advance.
 *
 * <h2>{@link #generateSecret(String)}</h2>
 *
 * <p>The raw secret is no good as a key: its distribution is not uniform and using it straight is a
 * known mistake. This version puts it through whatever it has to go through before handing it over,
 * and it is the one to use.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>The machinery works in full, but no registered provider offers key agreements, so
 * {@link #getInstance} throws {@link NoSuchAlgorithmException} for any name. Registering a provider
 * of one's own makes it work.
 *
 * @since 1.4
 */
public class KeyAgreement {

    private final KeyAgreementSpi spi;
    private final Provider provider;
    private final String algorithm;

    /**
     * One around that implementation.
     *
     * @param keyAgreeSpi the implementation
     * @param provider whose it is
     * @param algorithm the name it was asked for by
     */
    protected KeyAgreement(KeyAgreementSpi keyAgreeSpi, Provider provider, String algorithm) {
        this.spi = keyAgreeSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    /**
     * The name it was asked for by.
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
    public static final KeyAgreement getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            final Provider.Service s = provs[i].getService("KeyAgreement", algorithm);
            if (s != null) {
                return build(s, algorithm);
            }
        }
        throw new NoSuchAlgorithmException(algorithm + " KeyAgreement not available");
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
    public static final KeyAgreement getInstance(String algorithm, String provider)
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
    public static final KeyAgreement getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider.Service s = provider.getService("KeyAgreement", algorithm);
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
     * Configures it with one's own private half.
     *
     * @param key the private key
     * @throws InvalidKeyException if the key is no good
     */
    public final void init(Key key) throws InvalidKeyException {
        init(key, new SecureRandom());
    }

    /**
     * Configures it, saying where to take the randomness from.
     *
     * @param key the private key
     * @param random where to take the randomness from
     * @throws InvalidKeyException if the key is no good
     */
    public final void init(Key key, SecureRandom random) throws InvalidKeyException {
        this.spi.engineInit(key, random);
    }

    /**
     * Configures it with parameters.
     *
     * @param key the private key
     * @param params the parameters
     * @throws InvalidKeyException if the key is no good
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     */
    public final void init(Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        init(key, params, new SecureRandom());
    }

    /**
     * Configures it with parameters, saying where to take the randomness from.
     *
     * @param key the private key
     * @param params the parameters
     * @param random where to take the randomness from
     * @throws InvalidKeyException if the key is no good
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     */
    public final void init(Key key, AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        this.spi.engineInit(key, params, random);
    }

    /**
     * Combines another participant's public half.
     *
     * @param key the other one's public key
     * @param lastPhase whether it is the last one
     * @return the intermediate key, or {@code null} if there is none
     * @throws InvalidKeyException if the key is no good
     * @throws IllegalStateException if it has not been configured
     */
    public final Key doPhase(Key key, boolean lastPhase)
            throws InvalidKeyException, IllegalStateException {
        return this.spi.engineDoPhase(key, lastPhase);
    }

    /**
     * The agreed secret, raw.
     *
     * @return the secret
     * @throws IllegalStateException if phases are missing
     */
    public final byte[] generateSecret() throws IllegalStateException {
        return this.spi.engineGenerateSecret();
    }

    /**
     * The agreed secret, raw, written into the given array.
     *
     * @param sharedSecret where to write it
     * @param offset from where
     * @return how many bytes were written
     * @throws IllegalStateException if phases are missing
     * @throws ShortBufferException if the array is not big enough
     */
    public final int generateSecret(byte[] sharedSecret, int offset)
            throws IllegalStateException, ShortBufferException {
        return this.spi.engineGenerateSecret(sharedSecret, offset);
    }

    /**
     * The agreed secret, already turned into a key of that algorithm.
     *
     * @param algorithm for which algorithm
     * @return the key
     * @throws IllegalStateException if phases are missing
     * @throws NoSuchAlgorithmException if there is no way to assemble a key of that algorithm
     * @throws InvalidKeyException if the secret is not enough for a key of that algorithm
     */
    public final SecretKey generateSecret(String algorithm)
            throws IllegalStateException, NoSuchAlgorithmException, InvalidKeyException {
        return this.spi.engineGenerateSecret(algorithm);
    }

    private static KeyAgreement build(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        final Object o = s.newInstance(null);
        if (!(o instanceof KeyAgreementSpi)) {
            throw new NoSuchAlgorithmException(
                    "class configured for KeyAgreement is not a KeyAgreementSpi: " + s.getClassName());
        }
        return new KeyAgreement((KeyAgreementSpi) o, s.getProvider(), algorithm);
    }
}
