package javax.crypto;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;

/**
 * What a provider has to write in order to offer a message authentication code.
 *
 * <h2>What a MAC is and what it is not</h2>
 *
 * <p>It is a digest with a key. A plain digest proves the message did not change, but anybody can
 * recompute it after changing it; with a key, only whoever has the key can compute it. That is what
 * makes it a proof of origin and not only of integrity.
 *
 * <p>What it is not: a signature. Both parties share the same key, so neither can prove to a third
 * party that the other wrote the message --it could have written it itself.
 *
 * <h2>{@link #clone}</h2>
 *
 * <p>It exists for the same reason as in a digest: being able to save the state after a common
 * prefix and carry on down two different paths without recomputing it. A provider that does not
 * allow it inherits {@link Object}'s behaviour, which throws.
 *
 * @since 1.4
 */
public abstract class MacSpi {

    /** One. */
    public MacSpi() {
    }

    /**
     * How large what comes out is.
     *
     * @return the size in bytes
     */
    protected abstract int engineGetMacLength();

    /**
     * Configures it.
     *
     * @param key the key
     * @param params the parameters, or {@code null}
     * @throws InvalidKeyException if the key is no good
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     */
    protected abstract void engineInit(Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException;

    /**
     * Hands over one byte.
     *
     * @param input the byte
     */
    protected abstract void engineUpdate(byte input);

    /**
     * Hands over data.
     *
     * @param input the data
     * @param offset from where
     * @param len how many
     */
    protected abstract void engineUpdate(byte[] input, int offset, int len);

    /**
     * Hands over whatever is left in the buffer.
     *
     * @param input the data; it is left consumed
     */
    protected void engineUpdate(ByteBuffer input) {
        if (input == null) {
            throw new NullPointerException("input");
        }
        if (!input.hasRemaining()) {
            return;
        }
        if (input.hasArray()) {
            final byte[] a = input.array();
            final int from = input.arrayOffset() + input.position();
            final int howMany = input.remaining();
            engineUpdate(a, from, howMany);
            input.position(input.limit());
            return;
        }
        final byte[] copy = new byte[input.remaining()];
        input.get(copy);
        engineUpdate(copy, 0, copy.length);
    }

    /**
     * Finishes and returns the code.
     *
     * @return the code
     */
    protected abstract byte[] engineDoFinal();

    /** Goes back to the state it had after being configured. */
    protected abstract void engineReset();

    /**
     * A copy with the same state.
     *
     * @return the copy
     * @throws CloneNotSupportedException if this one cannot be copied
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
