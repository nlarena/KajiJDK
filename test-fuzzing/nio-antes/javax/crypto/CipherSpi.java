package javax.crypto;

import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

/**
 * What a provider has to write in order to offer a cipher.
 *
 * <h2>Why it is separate from {@link Cipher}</h2>
 *
 * <p>Because they are two different audiences. {@link Cipher} is the face whoever encrypts sees: it
 * has overloads for every convenience --arrays, slices of arrays, {@link ByteBuffer}-- and asks
 * nothing of anybody. This is the face whoever implements the algorithm sees, and that one wants to
 * be as small as possible. Every overload added here would be work repeated in every provider.
 *
 * <h2>Which ones have a body</h2>
 *
 * <p>The {@link ByteBuffer} ones and the key-wrapping ones. The first because they can be written
 * once by taking the bytes out of the buffer and calling the array version --which is what the
 * implementation here does; the second because not every cipher knows how to wrap keys, and the ones
 * that do not throw {@link UnsupportedOperationException}.
 *
 * <h2>The mode and the padding</h2>
 *
 * <p>{@link #engineSetMode} and {@link #engineSetPadding} are called once, at construction, and come
 * out of splitting the name handed to {@link Cipher#getInstance}. A provider may refuse them: not
 * every algorithm has modes, and a stream cipher has no padding.
 *
 * @since 1.4
 */
public abstract class CipherSpi {

    /** One. */
    public CipherSpi() {
    }

    /**
     * Sets the operation mode.
     *
     * @param mode the mode, such as {@code "CBC"}
     * @throws NoSuchAlgorithmException if the provider does not have that mode
     */
    protected abstract void engineSetMode(String mode) throws NoSuchAlgorithmException;

    /**
     * Sets the padding.
     *
     * @param padding the padding, such as {@code "PKCS5Padding"}
     * @throws NoSuchPaddingException if the provider does not have that padding
     */
    protected abstract void engineSetPadding(String padding) throws NoSuchPaddingException;

    /**
     * How large a block is.
     *
     * @return the size in bytes, or zero if it is not a block cipher
     */
    protected abstract int engineGetBlockSize();

    /**
     * How much will come out if those bytes are handed over now and it finishes.
     *
     * <p>It may overshoot, never fall short: it is for reserving the output array.
     *
     * @param inputLen how many bytes will be handed over
     * @return the size in bytes
     */
    protected abstract int engineGetOutputSize(int inputLen);

    /**
     * The initialization vector.
     *
     * @return a copy of the vector, or {@code null} if there is none
     */
    protected abstract byte[] engineGetIV();

    /**
     * The parameters it ended up configured with.
     *
     * <p>It matters when the cipher generated some by itself --a random initialization vector, for
     * instance: it is the only way for whoever decrypts to know which it used.
     *
     * @return the parameters, or {@code null} if it uses none
     */
    protected abstract AlgorithmParameters engineGetParameters();

    /**
     * Configures it.
     *
     * @param opmode what it will do
     * @param key with which key
     * @param random where to take whatever has to be drawn from
     * @throws InvalidKeyException if the key is no good for this cipher
     */
    protected abstract void engineInit(int opmode, Key key, SecureRandom random)
            throws InvalidKeyException;

    /**
     * Configures it with parameters.
     *
     * @param opmode what it will do
     * @param key with which key
     * @param params the parameters
     * @param random where to take whatever has to be drawn from
     * @throws InvalidKeyException if the key is no good for this cipher
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     */
    protected abstract void engineInit(int opmode, Key key, AlgorithmParameterSpec params,
            SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException;

    /**
     * Configures it with already encoded parameters.
     *
     * @param opmode what it will do
     * @param key with which key
     * @param params the parameters
     * @param random where to take whatever has to be drawn from
     * @throws InvalidKeyException if the key is no good for this cipher
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     */
    protected abstract void engineInit(int opmode, Key key, AlgorithmParameters params,
            SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException;

    /**
     * Hands over data and returns whatever comes out.
     *
     * @param input the data
     * @param inputOffset from where
     * @param inputLen how many
     * @return what came out, or {@code null} if nothing did
     */
    protected abstract byte[] engineUpdate(byte[] input, int inputOffset, int inputLen);

    /**
     * Hands over data and writes whatever comes out into the given array.
     *
     * @param input the data
     * @param inputOffset from where
     * @param inputLen how many
     * @param output where to write
     * @param outputOffset from where to write
     * @return how many bytes were written
     * @throws ShortBufferException if the output array is not big enough
     */
    protected abstract int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output,
            int outputOffset) throws ShortBufferException;

    /**
     * The same, with buffers.
     *
     * <p>The implementation here takes the bytes out of the input buffer, calls the array version
     * and puts them into the output one. A provider that can work on the buffer without copying
     * --one that talks to the machine directly-- should override it.
     *
     * @param input where to read from; it is left consumed
     * @param output where to write
     * @return how many bytes were written
     * @throws ShortBufferException if it does not fit in the output buffer
     * @throws NullPointerException if either of the two is {@code null}
     * @throws IllegalArgumentException if they are the same buffer
     * @throws java.nio.ReadOnlyBufferException if the output one is read-only
     */
    protected int engineUpdate(ByteBuffer input, ByteBuffer output) throws ShortBufferException {
        final byte[] in = take(input, output);
        return put(output, engineUpdate(in, 0, in.length));
    }

    /**
     * Hands over the last data and finishes.
     *
     * @param input the data, or {@code null}
     * @param inputOffset from where
     * @param inputLen how many
     * @return what came out
     * @throws IllegalBlockSizeException if what was handed over is not a multiple of the block
     * @throws BadPaddingException if the padding does not close
     */
    protected abstract byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen)
            throws IllegalBlockSizeException, BadPaddingException;

    /**
     * Hands over the last data, finishes, and writes into the given array.
     *
     * @param input the data, or {@code null}
     * @param inputOffset from where
     * @param inputLen how many
     * @param output where to write
     * @param outputOffset from where to write
     * @return how many bytes were written
     * @throws ShortBufferException if the output array is not big enough
     * @throws IllegalBlockSizeException if what was handed over is not a multiple of the block
     * @throws BadPaddingException if the padding does not close
     */
    protected abstract int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output,
            int outputOffset)
            throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;

    /**
     * The same, with buffers.
     *
     * @param input where to read from; it is left consumed
     * @param output where to write
     * @return how many bytes were written
     * @throws ShortBufferException if it does not fit in the output buffer
     * @throws IllegalBlockSizeException if what was handed over is not a multiple of the block
     * @throws BadPaddingException if the padding does not close
     * @throws NullPointerException if either of the two is {@code null}
     * @throws IllegalArgumentException if they are the same buffer
     * @throws java.nio.ReadOnlyBufferException if the output one is read-only
     */
    protected int engineDoFinal(ByteBuffer input, ByteBuffer output)
            throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        final byte[] in = take(input, output);
        return put(output, engineDoFinal(in, 0, in.length));
    }

    /**
     * Encrypts a key.
     *
     * <p>Wrapping a key is not the same as encrypting its bytes: the key is encoded first, and the
     * provider may do it without the material ever reaching memory --which is the whole point when
     * the key lives inside a device.
     *
     * @param key the key to wrap
     * @return the encrypted key
     * @throws IllegalBlockSizeException if the encoded key is not a multiple of the block
     * @throws InvalidKeyException if the key cannot be encoded
     * @throws UnsupportedOperationException if this cipher does not know how to wrap keys
     */
    protected byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        throw new UnsupportedOperationException();
    }

    /**
     * Decrypts a key.
     *
     * @param wrappedKey the encrypted key
     * @param wrappedKeyAlgorithm which algorithm the resulting key is for
     * @param wrappedKeyType whether it is public, private or secret
     * @return the key
     * @throws InvalidKeyException if what was decrypted is not a key of that type
     * @throws NoSuchAlgorithmException if there is nothing to rebuild it with
     * @throws UnsupportedOperationException if this cipher does not know how to wrap keys
     */
    protected Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType)
            throws InvalidKeyException, NoSuchAlgorithmException {
        throw new UnsupportedOperationException();
    }

    /**
     * How many bits that key has.
     *
     * @param key the key
     * @return the size in bits
     * @throws InvalidKeyException if the key is no good for this cipher
     * @throws UnsupportedOperationException if this cipher does not know how to answer
     */
    protected int engineGetKeySize(Key key) throws InvalidKeyException {
        throw new UnsupportedOperationException();
    }

    /**
     * Hands over data that is authenticated but not encrypted.
     *
     * <p>It only makes sense in an authenticated cipher. It is for what has to travel in the clear
     * --a header, a sequence number-- but protected against changes all the same.
     *
     * @param src the data
     * @param offset from where
     * @param len how many
     * @throws UnsupportedOperationException if this cipher is not authenticated
     */
    protected void engineUpdateAAD(byte[] src, int offset, int len) {
        throw new UnsupportedOperationException();
    }

    /**
     * The same, with a buffer.
     *
     * @param src the data; it is left consumed
     * @throws UnsupportedOperationException if this cipher is not authenticated
     */
    protected void engineUpdateAAD(ByteBuffer src) {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks the two buffers and takes the bytes out of the input one, which is left consumed.
     *
     * <p>It is consumed before it is known whether the output will fit. That is what the JDK does
     * and not an oversight: whoever catches a {@link ShortBufferException} has to build the input
     * again, not retry with the same buffer.
     */
    private static byte[] take(ByteBuffer input, ByteBuffer output) {
        if (input == null || output == null) {
            throw new NullPointerException("the buffers must not be null");
        }
        if (input == output) {
            throw new IllegalArgumentException("input and output buffers must not be the same");
        }
        if (output.isReadOnly()) {
            throw new java.nio.ReadOnlyBufferException();
        }
        final byte[] in = new byte[input.remaining()];
        input.get(in);
        return in;
    }

    /** Puts what came out into the output buffer. */
    private static int put(ByteBuffer output, byte[] out) throws ShortBufferException {
        if (out == null || out.length == 0) {
            return 0;
        }
        if (output.remaining() < out.length) {
            throw new ShortBufferException(
                    "output buffer too small: " + output.remaining() + " < " + out.length);
        }
        output.put(out);
        return out.length;
    }
}
