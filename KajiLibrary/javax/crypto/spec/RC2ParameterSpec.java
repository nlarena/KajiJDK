package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

/**
 * RC2's parameters: the key's effective bits and, if the mode calls for it, an eight-byte IV.
 *
 * <p>The "effective bits" are a peculiarity of RC2 worth understanding: the key may be of whatever
 * length, but the algorithm expands it to a strength declared separately. A 128-bit key with
 * `effectiveKeyBits` at 40 is a 40-bit key -- the number above is the one that rules.
 *
 * <p>It has `equals` and `hashCode` of its own, which most of this package's classes do not. It is
 * not an oversight in the others: it is that RC2 is used in formats where two sets of parameters have
 * to be compared to decide whether they describe the same cipher.
 */
public class RC2ParameterSpec implements AlgorithmParameterSpec {

    private static final int IV_LEN = 8;

    private final int effectiveKeyBits;
    private final byte[] iv;

    /** With no IV: for the modes that do not use one. */
    public RC2ParameterSpec(int effectiveKeyBits) {
        this.effectiveKeyBits = effectiveKeyBits;
        this.iv = null;
    }

    /**
     * @throws IllegalArgumentException if the IV is null or has fewer than eight bytes
     */
    public RC2ParameterSpec(int effectiveKeyBits, byte[] iv) {
        this(effectiveKeyBits, iv, 0);
    }

    /**
     * The IV is the eight bytes starting at `offset`.
     *
     * @throws IllegalArgumentException if the IV is null or fewer than eight bytes are left from
     *     `offset`
     */
    public RC2ParameterSpec(int effectiveKeyBits, byte[] iv, int offset) {
        if (iv == null) {
            throw new IllegalArgumentException("the IV cannot be null");
        }
        if (iv.length - offset < IV_LEN) {
            throw new IllegalArgumentException(
                    "an RC2 IV is " + IV_LEN + " bytes from the offset");
        }
        this.effectiveKeyBits = effectiveKeyBits;
        this.iv = IvParameterSpec.copy(iv, offset, IV_LEN);
    }

    /** The effective bits. See the class's note. */
    public int getEffectiveKeyBits() {
        return this.effectiveKeyBits;
    }

    /** A copy of the IV, or null if it has none. */
    public byte[] getIV() {
        return this.iv == null ? null : IvParameterSpec.copy(this.iv, 0, IV_LEN);
    }

    /** Equality by effective bits and IV. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RC2ParameterSpec)) {
            return false;
        }
        RC2ParameterSpec other = (RC2ParameterSpec) obj;
        return this.effectiveKeyBits == other.effectiveKeyBits
                && Arrays.equals(this.iv, other.iv);
    }

    public int hashCode() {
        int h = 0;
        if (this.iv != null) {
            for (int i = 0; i < this.iv.length; i++) {
                h = h + this.iv[i] * i;
            }
        }
        return h + this.effectiveKeyBits;
    }
}
