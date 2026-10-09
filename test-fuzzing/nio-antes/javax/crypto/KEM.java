package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Key encapsulation: how a secret is agreed with somebody who only published their public key.
 *
 * <h2>How it differs from encrypting</h2>
 *
 * <p>In that the secret is not chosen, it is generated. Encrypting a key with RSA forces whoever
 * encrypts to choose what to encrypt, and that choice is one of the historical sources of mistakes:
 * badly done padding, predictable values, the same key twice. Here whoever encapsulates chooses
 * nothing: they ask for a secret and get the secret and its encapsulation.
 *
 * <p>The other difference is that the two parties do not have to talk. With a key agreement
 * --{@link KeyAgreement}-- both have to send their half; here the recipient's public key is enough,
 * and it may have been published years ago.
 *
 * <h2>Why now</h2>
 *
 * <p>Because it is the shape the algorithms that resist a quantum computer take. The classical key
 * agreements lean on problems such a computer would solve; the ones replacing them are naturally
 * expressed as encapsulation, not as agreement.
 *
 * <h2>The sizes</h2>
 *
 * <p>{@link Encapsulator#secretSize} and {@link Encapsulator#encapsulationSize} are known before
 * anything is done. That is what allows exactly the right memory to be reserved and fixed-size
 * message protocols to be built, which are easier to analyse.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>The machinery works in full, but no registered provider offers key encapsulation, so
 * {@link #getInstance} throws {@link NoSuchAlgorithmException} for any name. Registering a provider
 * of one's own makes it work.
 *
 * @since 21
 */
public final class KEM {

    private final KEMSpi spi;
    private final Provider provider;
    private final String algorithm;

    private KEM(KEMSpi spi, Provider provider, String algorithm) {
        this.spi = spi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    /**
     * A freshly generated secret, together with what has to be sent to the other side for them to
     * recover it.
     *
     * @param key the secret, already as a key
     * @param encapsulation what is sent to the other side
     * @param params whatever parameters are needed to recover it, or {@code null}
     * @since 21
     */
    public static final class Encapsulated {

        private final SecretKey key;
        private final byte[] encapsulation;
        private final byte[] params;

        /**
         * One.
         *
         * @param key the secret
         * @param encapsulation what is sent to the other side
         * @param params the parameters, or {@code null}
         * @throws NullPointerException if the key or the encapsulation are {@code null}
         */
        public Encapsulated(SecretKey key, byte[] encapsulation, byte[] params) {
            if (key == null) {
                throw new NullPointerException("key");
            }
            if (encapsulation == null) {
                throw new NullPointerException("encapsulation");
            }
            this.key = key;
            this.encapsulation = encapsulation;
            this.params = params;
        }

        /**
         * The secret.
         *
         * @return the key
         */
        public SecretKey key() {
            return this.key;
        }

        /**
         * What has to be sent to the other side.
         *
         * @return the encapsulation
         */
        public byte[] encapsulation() {
            return this.encapsulation.clone();
        }

        /**
         * Whatever parameters are needed to recover it.
         *
         * @return the parameters, or {@code null}
         */
        public byte[] params() {
            return this.params == null ? null : this.params.clone();
        }
    }

    /** The side that generates the secret. */
    public static final class Encapsulator {

        private final KEMSpi.EncapsulatorSpi spi;
        private final String providerName;

        Encapsulator(KEMSpi.EncapsulatorSpi spi, String providerName) {
            this.spi = spi;
            this.providerName = providerName;
        }

        /**
         * Whose the implementation is.
         *
         * @return the provider's name
         */
        public String providerName() {
            return this.providerName;
        }

        /**
         * Generates a secret and its encapsulation.
         *
         * @return the secret and the encapsulation
         */
        public Encapsulated encapsulate() {
            return encapsulate(0, secretSize(), "Generic");
        }

        /**
         * The same, keeping only part of the secret.
         *
         * <p>It is for when several keys come out of one secret: it is encapsulated once and each
         * piece goes to a different use.
         *
         * @param from from which byte
         * @param to up to which byte, exclusive
         * @param algorithm which algorithm the resulting key is for
         * @return the secret and the encapsulation
         * @throws IndexOutOfBoundsException if the range does not fall inside the secret
         * @throws NullPointerException if the algorithm is {@code null}
         */
        public Encapsulated encapsulate(int from, int to, String algorithm) {
            checkRange(from, to, secretSize(), algorithm);
            return this.spi.engineEncapsulate(from, to, algorithm);
        }

        /**
         * How large the secret is.
         *
         * @return the size in bytes
         */
        public int secretSize() {
            return this.spi.engineSecretSize();
        }

        /**
         * How large the encapsulation is.
         *
         * @return the size in bytes
         */
        public int encapsulationSize() {
            return this.spi.engineEncapsulationSize();
        }
    }

    /** The side that recovers the secret. */
    public static final class Decapsulator {

        private final KEMSpi.DecapsulatorSpi spi;
        private final String providerName;

        Decapsulator(KEMSpi.DecapsulatorSpi spi, String providerName) {
            this.spi = spi;
            this.providerName = providerName;
        }

        /**
         * Whose the implementation is.
         *
         * @return the provider's name
         */
        public String providerName() {
            return this.providerName;
        }

        /**
         * Recovers the whole secret.
         *
         * @param encapsulation what the other side sent
         * @return the key
         * @throws DecapsulateException if it could not be recovered
         */
        public SecretKey decapsulate(byte[] encapsulation) throws DecapsulateException {
            return decapsulate(encapsulation, 0, secretSize(), "Generic");
        }

        /**
         * Recovers part of the secret.
         *
         * @param encapsulation what the other side sent
         * @param from from which byte
         * @param to up to which byte, exclusive
         * @param algorithm which algorithm the resulting key is for
         * @return the key
         * @throws DecapsulateException if it could not be recovered
         * @throws IndexOutOfBoundsException if the range does not fall inside the secret
         * @throws NullPointerException if the encapsulation or the algorithm are {@code null}
         * @throws IllegalArgumentException if the encapsulation is not the size it has to be
         */
        public SecretKey decapsulate(byte[] encapsulation, int from, int to, String algorithm)
                throws DecapsulateException {
            if (encapsulation == null) {
                throw new NullPointerException("encapsulation");
            }
            checkRange(from, to, secretSize(), algorithm);
            if (encapsulation.length != encapsulationSize()) {
                throw new IllegalArgumentException("Invalid encapsulation size");
            }
            return this.spi.engineDecapsulate(encapsulation, from, to, algorithm);
        }

        /**
         * How large the secret is.
         *
         * @return the size in bytes
         */
        public int secretSize() {
            return this.spi.engineSecretSize();
        }

        /**
         * How large the encapsulation is.
         *
         * @return the size in bytes
         */
        public int encapsulationSize() {
            return this.spi.engineEncapsulationSize();
        }
    }

    /**
     * One for that algorithm.
     *
     * @param algorithm the algorithm
     * @return the mechanism
     * @throws NoSuchAlgorithmException if no provider has it
     * @throws NullPointerException if the algorithm is {@code null}
     */
    public static KEM getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            final KEM k = build(provs[i], algorithm);
            if (k != null) {
                return k;
            }
        }
        throw new NoSuchAlgorithmException(algorithm + " KEM not available");
    }

    /**
     * One from that provider.
     *
     * @param algorithm the algorithm
     * @param provider the provider
     * @return the mechanism
     * @throws NoSuchAlgorithmException if that provider does not have it
     */
    public static KEM getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final KEM k = build(provider, algorithm);
        if (k != null) {
            return k;
        }
        throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
    }

    /**
     * One from that provider, named.
     *
     * @param algorithm the algorithm
     * @param provider the provider's name
     * @return the mechanism
     * @throws NoSuchAlgorithmException if that provider does not have it
     * @throws NoSuchProviderException if there is no provider by that name
     */
    public static KEM getInstance(String algorithm, String provider)
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
     * An encapsulator for that public key.
     *
     * @param publicKey the other one's public key
     * @return the encapsulator
     * @throws InvalidKeyException if the key is no good
     */
    public Encapsulator newEncapsulator(PublicKey publicKey) throws InvalidKeyException {
        try {
            return newEncapsulator(publicKey, null, null);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(e.getMessage());
        }
    }

    /**
     * The same, saying where to take the randomness from.
     *
     * @param publicKey the other one's public key
     * @param secureRandom where to take the randomness from, or {@code null}
     * @return the encapsulator
     * @throws InvalidKeyException if the key is no good
     */
    public Encapsulator newEncapsulator(PublicKey publicKey, SecureRandom secureRandom)
            throws InvalidKeyException {
        try {
            return newEncapsulator(publicKey, null, secureRandom);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(e.getMessage());
        }
    }

    /**
     * The same, with parameters.
     *
     * @param publicKey the other one's public key
     * @param spec the parameters, or {@code null}
     * @param secureRandom where to take the randomness from, or {@code null}
     * @return the encapsulator
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     * @throws InvalidKeyException if the key is no good
     * @throws NullPointerException if the key is {@code null}
     */
    public Encapsulator newEncapsulator(PublicKey publicKey, AlgorithmParameterSpec spec,
            SecureRandom secureRandom)
            throws InvalidAlgorithmParameterException, InvalidKeyException {
        if (publicKey == null) {
            throw new NullPointerException("input key is null");
        }
        return new Encapsulator(this.spi.engineNewEncapsulator(publicKey, spec, secureRandom),
                this.provider.getName());
    }

    /**
     * A decapsulator for that private key.
     *
     * @param privateKey one's own private key
     * @return the decapsulator
     * @throws InvalidKeyException if the key is no good
     */
    public Decapsulator newDecapsulator(PrivateKey privateKey) throws InvalidKeyException {
        try {
            return newDecapsulator(privateKey, null);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(e.getMessage());
        }
    }

    /**
     * The same, with parameters.
     *
     * @param privateKey one's own private key
     * @param spec the parameters, or {@code null}
     * @return the decapsulator
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     * @throws InvalidKeyException if the key is no good
     * @throws NullPointerException if the key is {@code null}
     */
    public Decapsulator newDecapsulator(PrivateKey privateKey, AlgorithmParameterSpec spec)
            throws InvalidAlgorithmParameterException, InvalidKeyException {
        if (privateKey == null) {
            throw new NullPointerException("input key is null");
        }
        return new Decapsulator(this.spi.engineNewDecapsulator(privateKey, spec),
                this.provider.getName());
    }

    /**
     * The name it was asked for by.
     *
     * @return the algorithm
     */
    public String getAlgorithm() {
        return this.algorithm;
    }

    private static KEM build(Provider p, String algorithm) throws NoSuchAlgorithmException {
        final Provider.Service s = p.getService("KEM", algorithm);
        if (s == null) {
            return null;
        }
        final Object o = s.newInstance(null);
        if (!(o instanceof KEMSpi)) {
            throw new NoSuchAlgorithmException(
                    "class configured for KEM is not a KEMSpi: " + s.getClassName());
        }
        return new KEM((KEMSpi) o, s.getProvider(), algorithm);
    }

    /** The range asked for has to fall inside the secret, and the algorithm cannot be missing. */
    static void checkRange(int from, int to, int size, String algorithm) {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        if (from < 0 || from > to || to > size) {
            throw new IndexOutOfBoundsException("from: " + from + ", to: " + to + ", size: " + size);
        }
    }
}
