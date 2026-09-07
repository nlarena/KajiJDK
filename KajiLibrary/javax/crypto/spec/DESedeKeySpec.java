package javax.crypto.spec;

import java.security.InvalidKeyException;
import java.security.spec.KeySpec;

/**
 * A Triple DES key: twenty-four bytes, that is, three DES keys placed one after another.
 *
 * <p>It has no `isWeak`, and the absence is deliberate: Triple DES's weakness is not in the
 * individual keys but in two of the three being equal --with that it degenerates into plain DES-- and
 * that is not what {@link DESKeySpec}'s `isWeak` measures. Checking only the three separately would
 * give false comfort.
 */
public class DESedeKeySpec implements KeySpec {

    /** The bytes a Triple DES key takes up. */
    public static final int DES_EDE_KEY_LEN = 24;

    private final byte[] key;

    /**
     * @throws InvalidKeyException if the array has fewer than twenty-four bytes
     * @throws NullPointerException if it is null
     */
    public DESedeKeySpec(byte[] key) throws InvalidKeyException {
        this(key, 0);
    }

    /**
     * The key is the twenty-four bytes starting at `offset`.
     *
     * @throws InvalidKeyException if fewer than twenty-four bytes are left from `offset`
     * @throws NullPointerException if the array is null
     */
    public DESedeKeySpec(byte[] key, int offset) throws InvalidKeyException {
        if (key == null) {
            throw new NullPointerException("the key cannot be null");
        }
        if (key.length - offset < DES_EDE_KEY_LEN) {
            throw new InvalidKeyException(
                    "a Triple DES key is " + DES_EDE_KEY_LEN + " bytes from the offset");
        }
        this.key = IvParameterSpec.copy(key, offset, DES_EDE_KEY_LEN);
    }

    /** A copy of the twenty-four bytes. */
    public byte[] getKey() {
        return IvParameterSpec.copy(this.key, 0, DES_EDE_KEY_LEN);
    }

    /**
     * Whether the three DES keys inside have their parity bits set.
     *
     * @throws InvalidKeyException if fewer than twenty-four bytes are left from `offset`
     * @throws NullPointerException if the array is null
     */
    public static boolean isParityAdjusted(byte[] key, int offset) throws InvalidKeyException {
        if (key == null) {
            throw new NullPointerException("the key cannot be null");
        }
        if (key.length - offset < DES_EDE_KEY_LEN) {
            throw new InvalidKeyException(
                    "a Triple DES key is " + DES_EDE_KEY_LEN + " bytes from the offset");
        }
        return DESKeySpec.isParityAdjusted(key, offset)
                && DESKeySpec.isParityAdjusted(key, offset + 8)
                && DESKeySpec.isParityAdjusted(key, offset + 16);
    }
}
