package javax.crypto;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;

/**
 * An exemption mechanism: what allowed keys longer than could be exported to be used.
 *
 * <h2>Where it comes from</h2>
 *
 * <p>From when exporting strong cryptography was restricted by law. A product could go over the
 * limit if it also kept, alongside the message, a block letting an authority recover it: key escrow,
 * key recovery, or deliberate weakening. That block is what {@link #genExemptionBlob} generates.
 *
 * <p>{@link #isCryptoAllowed} was the question {@link Cipher} asked before letting a long key be
 * used: it answered yes only once the blob had been generated for that key.
 *
 * <h2>Today</h2>
 *
 * <p>The restrictions were lifted and the JDK ships no mechanism. The machinery stayed because
 * removing it would break programs that name it, and because a deployment with rules of its own
 * could register one.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>The machinery works in full, but no registered provider offers exemption mechanisms --the JDK
 * does not either-- so {@link #getInstance} throws {@link NoSuchAlgorithmException} for any name. In
 * this library there is also no policy to apply: {@link Cipher#getMaxAllowedKeyLength} limits
 * nothing and {@link Cipher#getExemptionMechanism} always gives {@code null}.
 *
 * @since 1.4
 */
public class ExemptionMechanism {

    private final ExemptionMechanismSpi spi;
    private final Provider provider;
    private final String mechanism;

    private Key key;
    private boolean generated;

    /**
     * One around that implementation.
     *
     * @param exmechSpi the implementation
     * @param provider whose it is
     * @param mechanism the name it was asked for by
     */
    protected ExemptionMechanism(ExemptionMechanismSpi exmechSpi, Provider provider,
            String mechanism) {
        this.spi = exmechSpi;
        this.provider = provider;
        this.mechanism = mechanism;
    }

    /**
     * What it is called.
     *
     * @return the mechanism's name
     */
    public final String getName() {
        return this.mechanism;
    }

    /**
     * One with that name.
     *
     * @param algorithm the mechanism's name
     * @return the mechanism
     * @throws NoSuchAlgorithmException if no provider has it
     * @throws NullPointerException if the name is {@code null}
     */
    public static final ExemptionMechanism getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            final Provider.Service s = provs[i].getService("ExemptionMechanism", algorithm);
            if (s != null) {
                return build(s, algorithm);
            }
        }
        throw new NoSuchAlgorithmException(algorithm + " ExemptionMechanism not available");
    }

    /**
     * One from that provider, named.
     *
     * @param algorithm the mechanism's name
     * @param provider the provider's name
     * @return the mechanism
     * @throws NoSuchAlgorithmException if that provider does not have it
     * @throws NoSuchProviderException if there is no provider by that name
     * @throws IllegalArgumentException if the provider's name is {@code null} or empty
     */
    public static final ExemptionMechanism getInstance(String algorithm, String provider)
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
     * @param algorithm the mechanism's name
     * @param provider the provider
     * @return the mechanism
     * @throws NoSuchAlgorithmException if that provider does not have it
     * @throws IllegalArgumentException if the provider is {@code null}
     */
    public static final ExemptionMechanism getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider.Service s = provider.getService("ExemptionMechanism", algorithm);
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
     * Whether that key already has its exemption blob generated.
     *
     * <p>It is the question that decides whether it may be used. It answers yes only when the blob
     * was generated for that same key: one generated for another does not exempt this one.
     *
     * @param key the key
     * @return true if the blob has already been generated for it
     * @throws ExemptionMechanismException if it cannot be answered
     */
    public final boolean isCryptoAllowed(Key key) throws ExemptionMechanismException {
        return this.generated && this.key != null && this.key.equals(key);
    }

    /**
     * How large the blob will be.
     *
     * @param inputLen how large the input is
     * @return the size in bytes
     * @throws IllegalStateException if it has not been configured
     */
    public final int getOutputSize(int inputLen) throws IllegalStateException {
        check();
        return this.spi.engineGetOutputSize(inputLen);
    }

    /**
     * Configures it.
     *
     * @param key the key
     * @throws InvalidKeyException if the key is no good
     * @throws ExemptionMechanismException if anything else goes wrong
     */
    public final void init(Key key) throws InvalidKeyException, ExemptionMechanismException {
        this.generated = false;
        this.spi.engineInit(key);
        this.key = key;
    }

    /**
     * Configures it with parameters.
     *
     * @param key the key
     * @param params the parameters
     * @throws InvalidKeyException if the key is no good
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     * @throws ExemptionMechanismException if anything else goes wrong
     */
    public final void init(Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException,
            ExemptionMechanismException {
        this.generated = false;
        this.spi.engineInit(key, params);
        this.key = key;
    }

    /**
     * Configures it with already encoded parameters.
     *
     * @param key the key
     * @param params the parameters
     * @throws InvalidKeyException if the key is no good
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     * @throws ExemptionMechanismException if anything else goes wrong
     */
    public final void init(Key key, AlgorithmParameters params)
            throws InvalidKeyException, InvalidAlgorithmParameterException,
            ExemptionMechanismException {
        this.generated = false;
        this.spi.engineInit(key, params);
        this.key = key;
    }

    /**
     * Generates the blob.
     *
     * @return the blob
     * @throws IllegalStateException if it has not been configured
     * @throws ExemptionMechanismException if anything goes wrong
     */
    public final byte[] genExemptionBlob()
            throws IllegalStateException, ExemptionMechanismException {
        check();
        final byte[] r = this.spi.engineGenExemptionBlob();
        this.generated = true;
        return r;
    }

    /**
     * Generates the blob into the given array.
     *
     * @param output where to write it
     * @return how many bytes were written
     * @throws IllegalStateException if it has not been configured
     * @throws ShortBufferException if the array is not big enough
     * @throws ExemptionMechanismException if anything goes wrong
     */
    public final int genExemptionBlob(byte[] output)
            throws IllegalStateException, ShortBufferException, ExemptionMechanismException {
        return genExemptionBlob(output, 0);
    }

    /**
     * Generates the blob into the given array, from that position.
     *
     * @param output where to write it
     * @param outputOffset from where
     * @return how many bytes were written
     * @throws IllegalStateException if it has not been configured
     * @throws ShortBufferException if the array is not big enough
     * @throws ExemptionMechanismException if anything goes wrong
     */
    public final int genExemptionBlob(byte[] output, int outputOffset)
            throws IllegalStateException, ShortBufferException, ExemptionMechanismException {
        check();
        final int n = this.spi.engineGenExemptionBlob(output, outputOffset);
        this.generated = true;
        return n;
    }

    private void check() {
        if (this.key == null) {
            throw new IllegalStateException("ExemptionMechanism not initialized");
        }
    }

    private static ExemptionMechanism build(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        final Object o = s.newInstance(null);
        if (!(o instanceof ExemptionMechanismSpi)) {
            throw new NoSuchAlgorithmException(
                    "class configured for ExemptionMechanism is not an ExemptionMechanismSpi: "
                            + s.getClassName());
        }
        return new ExemptionMechanism((ExemptionMechanismSpi) o, s.getProvider(), algorithm);
    }
}
