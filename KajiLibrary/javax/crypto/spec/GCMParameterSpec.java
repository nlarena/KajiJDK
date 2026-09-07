package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * GCM's IV and authentication tag length.
 *
 * <p>`tLen` is **in bits**, not in bytes, and it is the classic mistake with this class: passing 16
 * instead of 128 configures a two-byte tag, which can be guessed. That is why the constructor rejects
 * negatives, although it cannot reject a 16 -- it is a legal value for other uses and the class does
 * not know which one is its own.
 */
public class GCMParameterSpec implements AlgorithmParameterSpec {

    private final int tLen;
    private final byte[] iv;

    /**
     * @throws IllegalArgumentException if `tLen` is negative or the IV is null
     */
    public GCMParameterSpec(int tLen, byte[] iv) {
        if (iv == null) {
            throw new IllegalArgumentException("the IV cannot be null");
        }
        if (tLen < 0) {
            throw new IllegalArgumentException("the tag length cannot be negative");
        }
        this.tLen = tLen;
        this.iv = IvParameterSpec.copy(iv, 0, iv.length);
    }

    /**
     * The IV is `len` bytes starting at `offset`.
     *
     * @throws IllegalArgumentException if `tLen` is negative, if the IV is null, or if the array is
     *     shorter than `offset + len`
     */
    public GCMParameterSpec(int tLen, byte[] iv, int offset, int len) {
        if (iv == null) {
            throw new IllegalArgumentException("the IV cannot be null");
        }
        if (tLen < 0) {
            throw new IllegalArgumentException("the tag length cannot be negative");
        }
        if (offset < 0 || len < 0) {
            throw new IllegalArgumentException("negative offset or length");
        }
        if (iv.length - offset < len) {
            throw new IllegalArgumentException("the IV is shorter than offset + len");
        }
        this.tLen = tLen;
        this.iv = IvParameterSpec.copy(iv, offset, len);
    }

    /** The tag length, **in bits**. */
    public int getTLen() {
        return this.tLen;
    }

    /** A copy of the IV. */
    public byte[] getIV() {
        return IvParameterSpec.copy(this.iv, 0, this.iv.length);
    }
}
