package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * ChaCha20's nonce and block counter.
 *
 * <p>The nonce is **exactly twelve bytes** and there is no leeway: ChaCha20 builds its state with a
 * 96-bit nonce, so one of another length does not describe a possible configuration.
 *
 * <p>The counter is stored as an `int` and interpreted **unsigned**: ChaCha20's state treats it as an
 * unsigned 32-bit integer, so a counter of `-1` is block 4294967295 and not an error. That is why the
 * constructor does not validate it.
 */
public final class ChaCha20ParameterSpec implements AlgorithmParameterSpec {

    /** The length the algorithm requires. */
    private static final int NONCE_LEN = 12;

    private final byte[] nonce;
    private final int counter;

    /**
     * @throws NullPointerException if the nonce is null
     * @throws IllegalArgumentException if it is not twelve bytes long
     */
    public ChaCha20ParameterSpec(byte[] nonce, int counter) {
        if (nonce == null) {
            throw new NullPointerException("the nonce cannot be null");
        }
        if (nonce.length != NONCE_LEN) {
            throw new IllegalArgumentException(
                    "a ChaCha20 nonce is " + NONCE_LEN + " bytes, not " + nonce.length);
        }
        this.nonce = IvParameterSpec.copy(nonce, 0, NONCE_LEN);
        this.counter = counter;
    }

    /** A copy of the nonce. */
    public byte[] getNonce() {
        return IvParameterSpec.copy(this.nonce, 0, this.nonce.length);
    }

    /** The block counter, unsigned. See the class's note. */
    public int getCounter() {
        return this.counter;
    }
}
