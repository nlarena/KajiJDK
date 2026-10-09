package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * An initialization vector.
 *
 * <p>The IV is **copied on the way in and on the way out**, and it is one of the few times the
 * defensive copy is not debatable: an IV someone could change after configuring the cipher would
 * leave two parts of the same program believing they use the same one, which is a silent way of
 * breaking the encryption.
 */
public class IvParameterSpec implements AlgorithmParameterSpec {

    private final byte[] iv;

    /**
     * @throws NullPointerException if `iv` is null
     */
    public IvParameterSpec(byte[] iv) {
        if (iv == null) {
            throw new NullPointerException("the IV cannot be null");
        }
        this.iv = copy(iv, 0, iv.length);
    }

    /**
     * The IV is `len` bytes starting at `offset`.
     *
     * @throws IllegalArgumentException if the array is shorter than `offset + len`
     * @throws ArrayIndexOutOfBoundsException if `offset` or `len` are negative
     */
    public IvParameterSpec(byte[] iv, int offset, int len) {
        if (iv == null) {
            throw new IllegalArgumentException("the IV cannot be null");
        }
        if (offset < 0 || len < 0) {
            throw new ArrayIndexOutOfBoundsException("negative offset or length");
        }
        if (iv.length - offset < len) {
            throw new IllegalArgumentException("the IV is shorter than offset + len");
        }
        this.iv = copy(iv, offset, len);
    }

    static byte[] copy(byte[] src, int offset, int len) {
        byte[] out = new byte[len];
        System.arraycopy(src, offset, out, 0, len);
        return out;
    }

    /** A copy of the IV. */
    public byte[] getIV() {
        return copy(this.iv, 0, this.iv.length);
    }
}
