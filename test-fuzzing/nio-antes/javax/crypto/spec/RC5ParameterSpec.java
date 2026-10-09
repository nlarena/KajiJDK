package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

/**
 * RC5's parameters: version, rounds, word size and, if the mode calls for it, an IV.
 *
 * <p>RC5 is a **family** of ciphers and not a single one: changing the word size and the rounds gives
 * different algorithms, incompatible with each other. That is why these three numbers go in the
 * parameters and are not fixed in the algorithm.
 *
 * <p>The IV is **two words** long, not a fixed length: with 32-bit words that is eight bytes and with
 * 64-bit words sixteen. Hence the validation depending on `wordSize`, which is what tells it apart
 * from {@link RC2ParameterSpec}'s.
 */
public class RC5ParameterSpec implements AlgorithmParameterSpec {

    private final int version;
    private final int rounds;
    private final int wordSize;
    private final byte[] iv;

    /** With no IV: for the modes that do not use one. `wordSize` is in bits. */
    public RC5ParameterSpec(int version, int rounds, int wordSize) {
        this.version = version;
        this.rounds = rounds;
        this.wordSize = wordSize;
        this.iv = null;
    }

    /**
     * @throws IllegalArgumentException if the IV is null or is not two words long
     */
    public RC5ParameterSpec(int version, int rounds, int wordSize, byte[] iv) {
        this(version, rounds, wordSize, iv, 0);
    }

    /**
     * The IV is the two words starting at `offset`.
     *
     * @throws IllegalArgumentException if the IV is null or fewer than two words are left from
     *     `offset`
     */
    public RC5ParameterSpec(int version, int rounds, int wordSize, byte[] iv, int offset) {
        if (iv == null) {
            throw new IllegalArgumentException("the IV cannot be null");
        }
        int len = (wordSize / 8) * 2;
        if (iv.length - offset < len) {
            throw new IllegalArgumentException(
                    "an RC5 IV is two words (" + len + " bytes) from the offset");
        }
        this.version = version;
        this.rounds = rounds;
        this.wordSize = wordSize;
        this.iv = IvParameterSpec.copy(iv, offset, len);
    }

    /** The algorithm's version. */
    public int getVersion() {
        return this.version;
    }

    /** How many rounds. */
    public int getRounds() {
        return this.rounds;
    }

    /** The word size, in bits. */
    public int getWordSize() {
        return this.wordSize;
    }

    /** A copy of the IV, or null if it has none. */
    public byte[] getIV() {
        return this.iv == null ? null : IvParameterSpec.copy(this.iv, 0, this.iv.length);
    }

    /** Equality by the three numbers and the IV. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RC5ParameterSpec)) {
            return false;
        }
        RC5ParameterSpec other = (RC5ParameterSpec) obj;
        return this.version == other.version
                && this.rounds == other.rounds
                && this.wordSize == other.wordSize
                && Arrays.equals(this.iv, other.iv);
    }

    public int hashCode() {
        int h = 0;
        if (this.iv != null) {
            for (int i = 0; i < this.iv.length; i++) {
                h = h + this.iv[i] * i;
            }
        }
        return h + this.version + this.rounds + this.wordSize;
    }
}
