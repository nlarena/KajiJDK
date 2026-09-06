package javax.crypto;

import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Encrypts and decrypts.
 *
 * <h2>The name carries three things</h2>
 *
 * <p>{@code "AES/CBC/PKCS5Padding"} is not an algorithm but three decisions. The algorithm says how
 * a block is transformed; the mode says how the blocks are chained to each other; the padding says
 * how the last one is completed. All three are necessary and none is a detail: the same AES in ECB
 * mode lets the patterns of the original text show through, and in CBC it does not.
 *
 * <p>The algorithm alone may be given, and then the provider chooses the other two. It is better not
 * to: what it chooses depends on the provider, and a program that works on one machine may encrypt
 * differently on another.
 *
 * <h2>{@link #update} and {@link #doFinal}</h2>
 *
 * <p>They are separate because a block cipher cannot hand anything over until it has a whole block,
 * and because the padding can only be applied once it is known that no more is coming. That is why
 * {@link #update} usually returns fewer bytes than it received --sometimes none-- and
 * {@link #doFinal} returns the rest.
 *
 * <p>Nothing is encrypted until {@link #doFinal} returns. Keeping what {@link #update} gave and not
 * calling {@link #doFinal} produces a truncated message, not a shorter one.
 *
 * <h2>Wrapping keys</h2>
 *
 * <p>{@link #wrap} and {@link #unwrap} exist apart from encrypting bytes because a key may live
 * inside a device and refuse to be exported. Wrapping it in there is the only thing that can be done
 * with it; taking its bytes out to encrypt them cannot.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>The machinery works in full: {@link #getInstance} really searches among the registered
 * providers, builds the cipher, sets its mode and padding, and delegates everything else to it. What
 * is missing is a provider offering ciphers --see the note on {@code java.security}'s provider: a
 * service that cannot be met is not registered-- so {@link #getInstance} throws
 * {@link NoSuchAlgorithmException} for any name. Registering a provider of one's own makes it work.
 *
 * <p>The only cipher that can be built out of the box is {@link NullCipher}, which encrypts nothing
 * and exists precisely for that.
 *
 * @since 1.4
 */
public class Cipher {

    /** That it will encrypt. */
    public static final int ENCRYPT_MODE = 1;

    /** That it will decrypt. */
    public static final int DECRYPT_MODE = 2;

    /** That it will wrap a key. */
    public static final int WRAP_MODE = 3;

    /** That it will unwrap a key. */
    public static final int UNWRAP_MODE = 4;

    /** That what was unwrapped is a public key. */
    public static final int PUBLIC_KEY = 1;

    /** That what was unwrapped is a private key. */
    public static final int PRIVATE_KEY = 2;

    /** That what was unwrapped is a symmetric key. */
    public static final int SECRET_KEY = 3;

    private final CipherSpi spi;
    private final Provider provider;
    private final String transformation;

    /** Whether it has been configured. Without this, encrypting would give junk instead of an error. */
    boolean initialized;

    /**
     * Whether it can be used without being configured.
     *
     * <p>{@link NullCipher} turns it on, having nothing to configure. It is separate from
     * {@link #initialized} because {@link #toString} has to go on saying it is not initialized,
     * which is what the JDK says.
     */
    boolean unconfigured;

    /**
     * One around that implementation.
     *
     * <p>It is protected and not public because the normal way in is {@link #getInstance}: whoever
     * calls this directly is assembling a cipher by hand, and that only makes sense for
     * {@link NullCipher} or for a subclass of one's own.
     *
     * @param cipherSpi the implementation
     * @param provider whose it is
     * @param transformation the name it was asked for by
     */
    protected Cipher(CipherSpi cipherSpi, Provider provider, String transformation) {
        this.spi = cipherSpi;
        this.provider = provider;
        this.transformation = transformation;
    }

    /**
     * A cipher for that transformation.
     *
     * @param transformation the algorithm, or the algorithm with its mode and its padding
     * @return the cipher
     * @throws NoSuchAlgorithmException if the name is malformed, or if no provider has it
     * @throws NoSuchPaddingException if none has that padding
     */
    public static final Cipher getInstance(String transformation)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        final String[] parts = split(transformation);
        final Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            final Cipher c = build(provs[i], parts, transformation);
            if (c != null) {
                return c;
            }
        }
        throw new NoSuchAlgorithmException(
                "Cannot find any provider supporting " + transformation);
    }

    /**
     * A cipher from that provider, named.
     *
     * @param transformation the algorithm, or the algorithm with its mode and its padding
     * @param provider the provider's name
     * @return the cipher
     * @throws NoSuchAlgorithmException if the name is malformed, or if that provider does not have it
     * @throws NoSuchProviderException if there is no provider by that name
     * @throws NoSuchPaddingException if that provider does not have that padding
     * @throws IllegalArgumentException if the provider's name is {@code null} or empty
     */
    public static final Cipher getInstance(String transformation, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("Missing provider");
        }
        final Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("No such provider: " + provider);
        }
        return getInstance(transformation, p);
    }

    /**
     * A cipher from that provider.
     *
     * @param transformation the algorithm, or the algorithm with its mode and its padding
     * @param provider the provider
     * @return the cipher
     * @throws NoSuchAlgorithmException if the name is malformed, or if that provider does not have it
     * @throws NoSuchPaddingException if that provider does not have that padding
     * @throws IllegalArgumentException if the provider is {@code null}
     */
    public static final Cipher getInstance(String transformation, Provider provider)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        if (provider == null) {
            throw new IllegalArgumentException("Missing provider");
        }
        final String[] parts = split(transformation);
        final Cipher c = build(provider, parts, transformation);
        if (c != null) {
            return c;
        }
        throw new NoSuchAlgorithmException(
                "No such algorithm: " + transformation + " for provider " + provider.getName());
    }

    /**
     * Whose the implementation is.
     *
     * @return the provider, or {@code null} if it comes from none
     */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * The name it was asked for by.
     *
     * @return the transformation, or {@code null} if it was not asked for by name
     */
    public final String getAlgorithm() {
        return this.transformation;
    }

    /**
     * How large a block is.
     *
     * @return the size in bytes, or zero if it is not a block cipher
     */
    public final int getBlockSize() {
        return this.spi.engineGetBlockSize();
    }

    /**
     * How much will come out if those bytes are handed over now and it finishes.
     *
     * <p>It may overshoot: this is what has to be reserved, and what gets written may be less.
     *
     * @param inputLen how many bytes will be handed over
     * @return the size in bytes
     * @throws IllegalStateException if it has not been configured
     * @throws IllegalArgumentException if the length is negative
     */
    public final int getOutputSize(int inputLen) {
        checkConfigured();
        if (inputLen < 0) {
            throw new IllegalArgumentException("Input size must be equal to or greater than zero");
        }
        return this.spi.engineGetOutputSize(inputLen);
    }

    /**
     * The initialization vector.
     *
     * @return a copy of the vector, or {@code null} if there is none
     */
    public final byte[] getIV() {
        return this.spi.engineGetIV();
    }

    /**
     * The parameters it ended up configured with.
     *
     * <p>It is how whoever decrypts learns what whoever encrypted generated at random.
     *
     * @return the parameters, or {@code null} if it uses none
     */
    public final AlgorithmParameters getParameters() {
        return this.spi.engineGetParameters();
    }

    /**
     * The exemption mechanism applied to it.
     *
     * @return always {@code null}: there is no export policy to apply, see
     *     {@link ExemptionMechanism}
     */
    public final ExemptionMechanism getExemptionMechanism() {
        return null;
    }

    /**
     * Configures it.
     *
     * @param opmode what it will do
     * @param key with which key
     * @throws InvalidKeyException if the key is no good for this cipher
     * @throws IllegalArgumentException if the operation mode is not one of the four
     */
    public final void init(int opmode, Key key) throws InvalidKeyException {
        init(opmode, key, new SecureRandom());
    }

    /**
     * Configures it, saying where to take the randomness from.
     *
     * @param opmode what it will do
     * @param key with which key
     * @param random where to take whatever has to be drawn from
     * @throws InvalidKeyException if the key is no good for this cipher
     * @throws IllegalArgumentException if the operation mode is not one of the four
     */
    public final void init(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
        checkMode(opmode);
        this.initialized = false;
        this.spi.engineInit(opmode, key, random);
        this.initialized = true;
    }

    /**
     * Configures it with parameters.
     *
     * @param opmode what it will do
     * @param key with which key
     * @param params the parameters
     * @throws InvalidKeyException if the key is no good for this cipher
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     * @throws IllegalArgumentException if the operation mode is not one of the four
     */
    public final void init(int opmode, Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        init(opmode, key, params, new SecureRandom());
    }

    /**
     * Configures it with parameters, saying where to take the randomness from.
     *
     * @param opmode what it will do
     * @param key with which key
     * @param params the parameters
     * @param random where to take whatever has to be drawn from
     * @throws InvalidKeyException if the key is no good for this cipher
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     * @throws IllegalArgumentException if the operation mode is not one of the four
     */
    public final void init(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        checkMode(opmode);
        this.initialized = false;
        this.spi.engineInit(opmode, key, params, random);
        this.initialized = true;
    }

    /**
     * Configures it with already encoded parameters.
     *
     * @param opmode what it will do
     * @param key with which key
     * @param params the parameters
     * @throws InvalidKeyException if the key is no good for this cipher
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     * @throws IllegalArgumentException if the operation mode is not one of the four
     */
    public final void init(int opmode, Key key, AlgorithmParameters params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        init(opmode, key, params, new SecureRandom());
    }

    /**
     * Configures it with already encoded parameters, saying where to take the randomness from.
     *
     * @param opmode what it will do
     * @param key with which key
     * @param params the parameters
     * @param random where to take whatever has to be drawn from
     * @throws InvalidKeyException if the key is no good for this cipher
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     * @throws IllegalArgumentException if the operation mode is not one of the four
     */
    public final void init(int opmode, Key key, AlgorithmParameters params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        checkMode(opmode);
        this.initialized = false;
        this.spi.engineInit(opmode, key, params, random);
        this.initialized = true;
    }

    /**
     * Configures it with a certificate's public key.
     *
     * @param opmode what it will do
     * @param certificate the certificate
     * @throws InvalidKeyException if the certificate's key is no good for this cipher
     * @throws IllegalArgumentException if the operation mode is not one of the four
     */
    public final void init(int opmode, Certificate certificate) throws InvalidKeyException {
        init(opmode, certificate, new SecureRandom());
    }

    /**
     * Configures it with a certificate's public key, saying where to take the randomness from.
     *
     * @param opmode what it will do
     * @param certificate the certificate
     * @param random where to take whatever has to be drawn from
     * @throws InvalidKeyException if the certificate's key is no good for this cipher
     * @throws IllegalArgumentException if the operation mode is not one of the four
     */
    public final void init(int opmode, Certificate certificate, SecureRandom random)
            throws InvalidKeyException {
        if (certificate == null) {
            throw new NullPointerException("certificate");
        }
        final PublicKey pk = certificate.getPublicKey();
        init(opmode, pk, random);
    }

    /**
     * Hands over data and returns whatever comes out.
     *
     * @param input the data
     * @return what came out, or {@code null} if nothing did
     * @throws IllegalStateException if it has not been configured
     * @throws IllegalArgumentException if the data is {@code null}
     */
    public final byte[] update(byte[] input) {
        checkConfigured();
        if (input == null) {
            throw new IllegalArgumentException("Null input buffer");
        }
        return this.spi.engineUpdate(input, 0, input.length);
    }

    /**
     * Hands over part of an array and returns whatever comes out.
     *
     * @param input the data
     * @param inputOffset from where
     * @param inputLen how many
     * @return what came out, or {@code null} if nothing did
     * @throws IllegalStateException if it has not been configured
     * @throws IllegalArgumentException if the slice is not right
     */
    public final byte[] update(byte[] input, int inputOffset, int inputLen) {
        checkConfigured();
        checkSlice(input, inputOffset, inputLen);
        return this.spi.engineUpdate(input, inputOffset, inputLen);
    }

    /**
     * Hands over data and writes whatever comes out into the given array.
     *
     * @param input the data
     * @param inputOffset from where
     * @param inputLen how many
     * @param output where to write
     * @return how many bytes were written
     * @throws IllegalStateException if it has not been configured
     * @throws ShortBufferException if the output array is not big enough
     */
    public final int update(byte[] input, int inputOffset, int inputLen, byte[] output)
            throws ShortBufferException {
        return update(input, inputOffset, inputLen, output, 0);
    }

    /**
     * Hands over data and writes whatever comes out into the given array, from that position.
     *
     * @param input the data
     * @param inputOffset from where
     * @param inputLen how many
     * @param output where to write
     * @param outputOffset from where to write
     * @return how many bytes were written
     * @throws IllegalStateException if it has not been configured
     * @throws ShortBufferException if the output array is not big enough
     */
    public final int update(byte[] input, int inputOffset, int inputLen, byte[] output,
            int outputOffset) throws ShortBufferException {
        checkConfigured();
        checkSlice(input, inputOffset, inputLen);
        if (output == null || outputOffset < 0) {
            throw new IllegalArgumentException("Bad arguments");
        }
        return this.spi.engineUpdate(input, inputOffset, inputLen, output, outputOffset);
    }

    /**
     * The same, with buffers.
     *
     * @param input where to read from; it is left consumed
     * @param output where to write
     * @return how many bytes were written
     * @throws IllegalStateException if it has not been configured
     * @throws ShortBufferException if it does not fit in the output buffer
     */
    public final int update(ByteBuffer input, ByteBuffer output) throws ShortBufferException {
        checkConfigured();
        return this.spi.engineUpdate(input, output);
    }

    /**
     * Finishes without handing anything else over.
     *
     * @return what was left, or {@code null} if nothing was
     * @throws IllegalStateException if it has not been configured
     * @throws IllegalBlockSizeException if what was handed over is not a multiple of the block
     * @throws BadPaddingException if the padding does not close
     */
    public final byte[] doFinal() throws IllegalBlockSizeException, BadPaddingException {
        checkConfigured();
        return this.spi.engineDoFinal(null, 0, 0);
    }

    /**
     * Finishes and writes what was left into the given array.
     *
     * @param output where to write
     * @param outputOffset from where to write
     * @return how many bytes were written
     * @throws IllegalStateException if it has not been configured
     * @throws IllegalBlockSizeException if what was handed over is not a multiple of the block
     * @throws ShortBufferException if the output array is not big enough
     * @throws BadPaddingException if the padding does not close
     */
    public final int doFinal(byte[] output, int outputOffset)
            throws IllegalBlockSizeException, ShortBufferException, BadPaddingException {
        checkConfigured();
        if (output == null || outputOffset < 0) {
            throw new IllegalArgumentException("Bad arguments");
        }
        return this.spi.engineDoFinal(null, 0, 0, output, outputOffset);
    }

    /**
     * Hands over the last data and finishes.
     *
     * @param input the data
     * @return what came out
     * @throws IllegalStateException if it has not been configured
     * @throws IllegalBlockSizeException if what was handed over is not a multiple of the block
     * @throws BadPaddingException if the padding does not close
     */
    public final byte[] doFinal(byte[] input)
            throws IllegalBlockSizeException, BadPaddingException {
        checkConfigured();
        if (input == null) {
            throw new IllegalArgumentException("Null input buffer");
        }
        return this.spi.engineDoFinal(input, 0, input.length);
    }

    /**
     * Hands over part of an array and finishes.
     *
     * @param input the data
     * @param inputOffset from where
     * @param inputLen how many
     * @return what came out
     * @throws IllegalStateException if it has not been configured
     * @throws IllegalBlockSizeException if what was handed over is not a multiple of the block
     * @throws BadPaddingException if the padding does not close
     */
    public final byte[] doFinal(byte[] input, int inputOffset, int inputLen)
            throws IllegalBlockSizeException, BadPaddingException {
        checkConfigured();
        checkSlice(input, inputOffset, inputLen);
        return this.spi.engineDoFinal(input, inputOffset, inputLen);
    }

    /**
     * Hands over the last data, finishes, and writes into the given array.
     *
     * @param input the data
     * @param inputOffset from where
     * @param inputLen how many
     * @param output where to write
     * @return how many bytes were written
     * @throws IllegalStateException if it has not been configured
     * @throws ShortBufferException if the output array is not big enough
     * @throws IllegalBlockSizeException if what was handed over is not a multiple of the block
     * @throws BadPaddingException if the padding does not close
     */
    public final int doFinal(byte[] input, int inputOffset, int inputLen, byte[] output)
            throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        return doFinal(input, inputOffset, inputLen, output, 0);
    }

    /**
     * The same, writing from that position.
     *
     * @param input the data
     * @param inputOffset from where
     * @param inputLen how many
     * @param output where to write
     * @param outputOffset from where to write
     * @return how many bytes were written
     * @throws IllegalStateException if it has not been configured
     * @throws ShortBufferException if the output array is not big enough
     * @throws IllegalBlockSizeException if what was handed over is not a multiple of the block
     * @throws BadPaddingException if the padding does not close
     */
    public final int doFinal(byte[] input, int inputOffset, int inputLen, byte[] output,
            int outputOffset)
            throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        checkConfigured();
        checkSlice(input, inputOffset, inputLen);
        if (output == null || outputOffset < 0) {
            throw new IllegalArgumentException("Bad arguments");
        }
        return this.spi.engineDoFinal(input, inputOffset, inputLen, output, outputOffset);
    }

    /**
     * The same, with buffers.
     *
     * @param input where to read from; it is left consumed
     * @param output where to write
     * @return how many bytes were written
     * @throws IllegalStateException if it has not been configured
     * @throws ShortBufferException if it does not fit in the output buffer
     * @throws IllegalBlockSizeException if what was handed over is not a multiple of the block
     * @throws BadPaddingException if the padding does not close
     */
    public final int doFinal(ByteBuffer input, ByteBuffer output)
            throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        checkConfigured();
        return this.spi.engineDoFinal(input, output);
    }

    /**
     * Wraps a key.
     *
     * @param key the key to wrap
     * @return the encrypted key
     * @throws IllegalStateException if it has not been configured
     * @throws IllegalBlockSizeException if the encoded key is not a multiple of the block
     * @throws InvalidKeyException if the key cannot be encoded
     */
    public final byte[] wrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        checkConfigured();
        return this.spi.engineWrap(key);
    }

    /**
     * Unwraps a key.
     *
     * @param wrappedKey the encrypted key
     * @param wrappedKeyAlgorithm which algorithm the resulting key is for
     * @param wrappedKeyType whether it is public, private or secret
     * @return the key
     * @throws IllegalStateException if it has not been configured
     * @throws InvalidKeyException if what was decrypted is not a key of that type
     * @throws NoSuchAlgorithmException if there is nothing to rebuild it with
     */
    public final Key unwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType)
            throws InvalidKeyException, NoSuchAlgorithmException {
        checkConfigured();
        if (wrappedKeyType != PUBLIC_KEY && wrappedKeyType != PRIVATE_KEY
                && wrappedKeyType != SECRET_KEY) {
            throw new InvalidParameterException("Invalid key type");
        }
        return this.spi.engineUnwrap(wrappedKey, wrappedKeyAlgorithm, wrappedKeyType);
    }

    /**
     * What the longest key usable with that algorithm is.
     *
     * @param transformation the algorithm
     * @return {@link Integer#MAX_VALUE}: there is no policy file limiting anything
     * @throws NoSuchAlgorithmException if the name is malformed
     * @throws NullPointerException if the name is {@code null}
     */
    public static final int getMaxAllowedKeyLength(String transformation)
            throws NoSuchAlgorithmException {
        if (transformation == null) {
            throw new NullPointerException("null transformation");
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Which parameters are the strongest usable with that algorithm.
     *
     * @param transformation the algorithm
     * @return {@code null}: there is no policy file limiting anything
     * @throws NoSuchAlgorithmException if the name is malformed
     * @throws NullPointerException if the name is {@code null}
     */
    public static final AlgorithmParameterSpec getMaxAllowedParameterSpec(String transformation)
            throws NoSuchAlgorithmException {
        if (transformation == null) {
            throw new NullPointerException("null transformation");
        }
        return null;
    }

    /**
     * Hands over data that is authenticated but not encrypted.
     *
     * @param src the data
     * @throws IllegalStateException if it has not been configured
     * @throws IllegalArgumentException if the data is {@code null}
     * @throws UnsupportedOperationException if this cipher is not authenticated
     */
    public final void updateAAD(byte[] src) {
        checkConfigured();
        if (src == null) {
            throw new IllegalArgumentException("src buffer is null");
        }
        this.spi.engineUpdateAAD(src, 0, src.length);
    }

    /**
     * The same, with part of an array.
     *
     * @param src the data
     * @param offset from where
     * @param len how many
     * @throws IllegalStateException if it has not been configured
     * @throws IllegalArgumentException if the slice is not right
     * @throws UnsupportedOperationException if this cipher is not authenticated
     */
    public final void updateAAD(byte[] src, int offset, int len) {
        checkConfigured();
        checkSlice(src, offset, len);
        this.spi.engineUpdateAAD(src, offset, len);
    }

    /**
     * The same, with a buffer.
     *
     * @param src the data; it is left consumed
     * @throws IllegalStateException if it has not been configured
     * @throws IllegalArgumentException if the buffer is {@code null}
     * @throws UnsupportedOperationException if this cipher is not authenticated
     */
    public final void updateAAD(ByteBuffer src) {
        checkConfigured();
        if (src == null) {
            throw new IllegalArgumentException("src buffer is null");
        }
        this.spi.engineUpdateAAD(src);
    }

    /**
     * For reading while debugging.
     *
     * @return the transformation, whether it is configured, and which provider it came from
     */
    @Override
    public String toString() {
        return "Cipher." + this.transformation + ", mode: "
                + (this.initialized ? "initialized" : "not initialized")
                + ", algorithm from: "
                + (this.provider == null ? "(no provider)" : this.provider.getName());
    }

    /**
     * Splits the name into algorithm, mode and padding.
     *
     * <p>One part or three; two or four is a malformed name. It is answered with
     * {@link NoSuchAlgorithmException} and not with {@link IllegalArgumentException} because from
     * the outside it is the same thing: something that does not exist was asked for.
     */
    private static String[] split(String transformation) throws NoSuchAlgorithmException {
        if (transformation == null || transformation.isEmpty()) {
            throw new NoSuchAlgorithmException("No transformation given");
        }
        final String[] parts = transformation.split("/", -1);
        if (parts.length != 1 && parts.length != 3) {
            throw new NoSuchAlgorithmException(
                    "Invalid transformation format: " + transformation);
        }
        if (parts.length == 1) {
            return new String[] {parts[0].trim(), null, null};
        }
        return new String[] {parts[0].trim(), parts[1].trim(), parts[2].trim()};
    }

    /**
     * Searches that provider and builds the cipher, or returns {@code null} if it does not have it.
     *
     * <p>Four names are tried, from the most specific to the least: a provider may register the
     * service under the whole transformation --because it has an implementation tuned for that
     * combination-- or under the algorithm alone, and let the mode and the padding be set separately.
     */
    private static Cipher build(Provider p, String[] parts, String transformation)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        final String alg = parts[0];
        final String mode = parts[1];
        final String padding = parts[2];
        Provider.Service s = null;
        boolean setMode = false;
        boolean setPadding = false;
        if (mode != null) {
            s = p.getService("Cipher", alg + "/" + mode + "/" + padding);
            if (s == null) {
                s = p.getService("Cipher", alg + "/" + mode);
                setPadding = s != null;
            }
            if (s == null) {
                s = p.getService("Cipher", alg + "//" + padding);
                setMode = s != null;
            }
        }
        if (s == null) {
            s = p.getService("Cipher", alg);
            setMode = mode != null;
            setPadding = padding != null;
        }
        if (s == null) {
            return null;
        }
        final Object o = s.newInstance(null);
        if (!(o instanceof CipherSpi)) {
            throw new NoSuchAlgorithmException(
                    "class configured for Cipher is not a CipherSpi: " + s.getClassName());
        }
        final CipherSpi spi = (CipherSpi) o;
        if (setMode) {
            spi.engineSetMode(mode);
        }
        if (setPadding) {
            spi.engineSetPadding(padding);
        }
        return new Cipher(spi, s.getProvider(), transformation);
    }

    private void checkConfigured() {
        if (!this.initialized && !this.unconfigured) {
            throw new IllegalStateException("Cipher not initialized");
        }
    }

    private static void checkMode(int opmode) {
        if (opmode != ENCRYPT_MODE && opmode != DECRYPT_MODE && opmode != WRAP_MODE
                && opmode != UNWRAP_MODE) {
            throw new IllegalArgumentException("Invalid operation mode: " + opmode);
        }
    }

    private static void checkSlice(byte[] input, int offset, int len) {
        if (input == null || offset < 0 || len < 0 || len > input.length - offset) {
            throw new IllegalArgumentException("Bad arguments");
        }
    }
}
