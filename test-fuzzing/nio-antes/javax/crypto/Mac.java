package javax.crypto;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;

/**
 * A message authentication code: a digest with a key.
 *
 * <h2>What it proves</h2>
 *
 * <p>That the message did not change and that it was written by somebody who has the key. A plain
 * digest proves the first but not the second: anybody who changes the message can recompute the
 * digest.
 *
 * <p>What it does not prove is which of the two wrote it. Both parties share the same key, so
 * neither can prove to a third party that it was the other. That needs a signature.
 *
 * <h2>How it is compared</h2>
 *
 * <p>Comparing the two arrays byte by byte with a loop that stops at the first different byte leaks
 * how many bytes matched, and with that the right code can be guessed one byte at a time. The
 * comparison has to look at every byte always, the way
 * {@link java.security.MessageDigest#isEqual} does.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>The machinery works in full, but no registered provider offers authentication codes, so
 * {@link #getInstance} throws {@link NoSuchAlgorithmException} for any name. Registering a provider
 * of one's own makes it work.
 *
 * @since 1.4
 */
public class Mac implements Cloneable {

    private MacSpi spi;
    private final Provider provider;
    private final String algorithm;
    private boolean initialized;

    /**
     * One around that implementation.
     *
     * @param macSpi the implementation
     * @param provider whose it is
     * @param algorithm the name it was asked for by
     */
    protected Mac(MacSpi macSpi, Provider provider, String algorithm) {
        this.spi = macSpi;
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
     * @return the authentication code
     * @throws NoSuchAlgorithmException if no provider has it
     * @throws NullPointerException if the algorithm is {@code null}
     */
    public static final Mac getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            final Provider.Service s = provs[i].getService("Mac", algorithm);
            if (s != null) {
                return build(s, algorithm);
            }
        }
        throw new NoSuchAlgorithmException(algorithm + " Mac not available");
    }

    /**
     * One from that provider, named.
     *
     * @param algorithm the algorithm
     * @param provider the provider's name
     * @return the authentication code
     * @throws NoSuchAlgorithmException if that provider does not have it
     * @throws NoSuchProviderException if there is no provider by that name
     * @throws IllegalArgumentException if the provider's name is {@code null} or empty
     */
    public static final Mac getInstance(String algorithm, String provider)
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
     * @return the authentication code
     * @throws NoSuchAlgorithmException if that provider does not have it
     * @throws IllegalArgumentException if the provider is {@code null}
     */
    public static final Mac getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider.Service s = provider.getService("Mac", algorithm);
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
     * How large what comes out is.
     *
     * @return the size in bytes
     */
    public final int getMacLength() {
        return this.spi.engineGetMacLength();
    }

    /**
     * Configures it.
     *
     * @param key the key
     * @throws InvalidKeyException if the key is no good
     */
    public final void init(Key key) throws InvalidKeyException {
        try {
            this.spi.engineInit(key, null);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException("init() failed", e);
        }
        this.initialized = true;
    }

    /**
     * Configures it with parameters.
     *
     * @param key the key
     * @param params the parameters
     * @throws InvalidKeyException if the key is no good
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     */
    public final void init(Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        this.spi.engineInit(key, params);
        this.initialized = true;
    }

    /**
     * Hands over one byte.
     *
     * @param input the byte
     * @throws IllegalStateException if it has not been configured
     */
    public final void update(byte input) throws IllegalStateException {
        check();
        this.spi.engineUpdate(input);
    }

    /**
     * Hands over data.
     *
     * @param input the data
     * @throws IllegalStateException if it has not been configured
     */
    public final void update(byte[] input) throws IllegalStateException {
        check();
        if (input != null) {
            this.spi.engineUpdate(input, 0, input.length);
        }
    }

    /**
     * Hands over part of an array.
     *
     * @param input the data
     * @param offset from where
     * @param len how many
     * @throws IllegalStateException if it has not been configured
     * @throws IllegalArgumentException if the slice is not right
     */
    public final void update(byte[] input, int offset, int len) throws IllegalStateException {
        check();
        if (input == null) {
            return;
        }
        if (offset < 0 || len < 0 || len > input.length - offset) {
            throw new IllegalArgumentException("Bad arguments");
        }
        this.spi.engineUpdate(input, offset, len);
    }

    /**
     * Hands over whatever is left in the buffer.
     *
     * @param input the data; it is left consumed
     * @throws IllegalStateException if it has not been configured
     */
    public final void update(ByteBuffer input) {
        check();
        if (input == null) {
            throw new IllegalArgumentException("Buffer must not be null");
        }
        this.spi.engineUpdate(input);
    }

    /**
     * Finishes and returns the code.
     *
     * <p>After this it is ready for another message with the same key: it does not have to be
     * configured again.
     *
     * @return the code
     * @throws IllegalStateException if it has not been configured
     */
    public final byte[] doFinal() throws IllegalStateException {
        check();
        final byte[] r = this.spi.engineDoFinal();
        this.spi.engineReset();
        return r;
    }

    /**
     * Finishes and writes the code into the given array.
     *
     * @param output where to write it
     * @param outOffset from where
     * @throws ShortBufferException if the array is not big enough
     * @throws IllegalStateException if it has not been configured
     */
    public final void doFinal(byte[] output, int outOffset)
            throws ShortBufferException, IllegalStateException {
        check();
        if (output == null || outOffset < 0) {
            throw new IllegalArgumentException("Bad arguments");
        }
        final int length = getMacLength();
        if (output.length - outOffset < length) {
            throw new ShortBufferException(
                    "Cannot store MAC in output buffer: " + length + " bytes needed");
        }
        final byte[] r = doFinal();
        System.arraycopy(r, 0, output, outOffset, r.length);
    }

    /**
     * Hands over the last data, finishes, and returns the code.
     *
     * @param input the data
     * @return the code
     * @throws IllegalStateException if it has not been configured
     */
    public final byte[] doFinal(byte[] input) throws IllegalStateException {
        check();
        update(input);
        return doFinal();
    }

    /**
     * Leaves it ready for another message with the same key.
     *
     * <p>It does not wipe the key: that would be configuring it again.
     */
    public final void reset() {
        this.spi.engineReset();
    }

    /**
     * A copy with the same state.
     *
     * <p>It is for computing the code of two messages that start the same without walking the common
     * part twice.
     *
     * @return the copy
     * @throws CloneNotSupportedException if the implementation cannot be copied
     */
    @Override
    public final Object clone() throws CloneNotSupportedException {
        final Mac copy = (Mac) super.clone();
        copy.spi = (MacSpi) this.spi.clone();
        return copy;
    }

    private void check() {
        if (!this.initialized) {
            throw new IllegalStateException("MAC not initialized");
        }
    }

    private static Mac build(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        final Object o = s.newInstance(null);
        if (!(o instanceof MacSpi)) {
            throw new NoSuchAlgorithmException(
                    "class configured for Mac is not a MacSpi: " + s.getClassName());
        }
        return new Mac((MacSpi) o, s.getProvider(), algorithm);
    }
}
