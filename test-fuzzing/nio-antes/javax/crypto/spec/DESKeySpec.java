package javax.crypto.spec;

import java.security.InvalidKeyException;
import java.security.spec.KeySpec;

/**
 * A DES key: eight bytes, of which only fifty-six bits are key.
 *
 * <p>The eighth bit of each byte is a **parity** bit and DES ignores it. Hence the two static
 * methods, which are the only interesting thing about this class:
 *
 * <ul>
 * <li>{@link #isParityAdjusted} says whether the parity bits are properly set --odd parity per
 *     byte--. An unadjusted key is still usable; the bit only served to detect transmission errors in
 *     nineteen-seventies hardware.</li>
 * <li>{@link #isWeak} says whether it is one of the sixteen keys DES documents as weak or
 *     semi-weak. A weak key makes encrypting twice give back the original text, and a semi-weak one
 *     forms pairs where one decrypts what the other encrypted. **That does matter**, and that is why
 *     the method exists: they are keys to reject, not to warn about.
 *     <p>Mind how it compares: byte by byte, **with the parity bit**. A weak key with the parity
 *     wrongly set is not recognized, even though to DES it is the same key. See the note in the
 *     implementation.</li>
 * </ul>
 */
public class DESKeySpec implements KeySpec {

    /** The bytes a DES key takes up. */
    public static final int DES_KEY_LEN = 8;

    // The sixteen problematic keys, with the parity bits set as the standard publishes them. The
    // first four are weak and the remaining twelve form the six semi-weak pairs.
    private static final byte[][] PROBLEMATIC = {
        { (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
          (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01 },
        { (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE,
          (byte) 0xFE, (byte) 0xFE, (byte) 0xFE, (byte) 0xFE },
        { (byte) 0xE0, (byte) 0xE0, (byte) 0xE0, (byte) 0xE0,
          (byte) 0xF1, (byte) 0xF1, (byte) 0xF1, (byte) 0xF1 },
        { (byte) 0x1F, (byte) 0x1F, (byte) 0x1F, (byte) 0x1F,
          (byte) 0x0E, (byte) 0x0E, (byte) 0x0E, (byte) 0x0E },
        { (byte) 0x01, (byte) 0xFE, (byte) 0x01, (byte) 0xFE,
          (byte) 0x01, (byte) 0xFE, (byte) 0x01, (byte) 0xFE },
        { (byte) 0xFE, (byte) 0x01, (byte) 0xFE, (byte) 0x01,
          (byte) 0xFE, (byte) 0x01, (byte) 0xFE, (byte) 0x01 },
        { (byte) 0x1F, (byte) 0xE0, (byte) 0x1F, (byte) 0xE0,
          (byte) 0x0E, (byte) 0xF1, (byte) 0x0E, (byte) 0xF1 },
        { (byte) 0xE0, (byte) 0x1F, (byte) 0xE0, (byte) 0x1F,
          (byte) 0xF1, (byte) 0x0E, (byte) 0xF1, (byte) 0x0E },
        { (byte) 0x01, (byte) 0xE0, (byte) 0x01, (byte) 0xE0,
          (byte) 0x01, (byte) 0xF1, (byte) 0x01, (byte) 0xF1 },
        { (byte) 0xE0, (byte) 0x01, (byte) 0xE0, (byte) 0x01,
          (byte) 0xF1, (byte) 0x01, (byte) 0xF1, (byte) 0x01 },
        { (byte) 0x1F, (byte) 0xFE, (byte) 0x1F, (byte) 0xFE,
          (byte) 0x0E, (byte) 0xFE, (byte) 0x0E, (byte) 0xFE },
        { (byte) 0xFE, (byte) 0x1F, (byte) 0xFE, (byte) 0x1F,
          (byte) 0xFE, (byte) 0x0E, (byte) 0xFE, (byte) 0x0E },
        { (byte) 0x01, (byte) 0x1F, (byte) 0x01, (byte) 0x1F,
          (byte) 0x01, (byte) 0x0E, (byte) 0x01, (byte) 0x0E },
        { (byte) 0x1F, (byte) 0x01, (byte) 0x1F, (byte) 0x01,
          (byte) 0x0E, (byte) 0x01, (byte) 0x0E, (byte) 0x01 },
        { (byte) 0xE0, (byte) 0xFE, (byte) 0xE0, (byte) 0xFE,
          (byte) 0xF1, (byte) 0xFE, (byte) 0xF1, (byte) 0xFE },
        { (byte) 0xFE, (byte) 0xE0, (byte) 0xFE, (byte) 0xE0,
          (byte) 0xFE, (byte) 0xF1, (byte) 0xFE, (byte) 0xF1 },
    };

    private final byte[] key;

    /**
     * @throws InvalidKeyException if the array has fewer than eight bytes
     * @throws NullPointerException if it is null
     */
    public DESKeySpec(byte[] key) throws InvalidKeyException {
        this(key, 0);
    }

    /**
     * The key is the eight bytes starting at `offset`.
     *
     * @throws InvalidKeyException if fewer than eight bytes are left from `offset`
     * @throws NullPointerException if the array is null
     */
    public DESKeySpec(byte[] key, int offset) throws InvalidKeyException {
        if (key == null) {
            throw new NullPointerException("the key cannot be null");
        }
        if (key.length - offset < DES_KEY_LEN) {
            throw new InvalidKeyException(
                    "a DES key is " + DES_KEY_LEN + " bytes from the offset");
        }
        this.key = IvParameterSpec.copy(key, offset, DES_KEY_LEN);
    }

    /** A copy of the eight bytes. */
    public byte[] getKey() {
        return IvParameterSpec.copy(this.key, 0, DES_KEY_LEN);
    }

    /**
     * Whether the parity bits are set: each byte has an **odd** number of ones.
     *
     * @throws InvalidKeyException if fewer than eight bytes are left from `offset`
     * @throws NullPointerException if the array is null
     */
    public static boolean isParityAdjusted(byte[] key, int offset) throws InvalidKeyException {
        requireEight(key, offset);
        for (int i = offset; i < offset + DES_KEY_LEN; i++) {
            int ones = 0;
            int b = key[i] & 0xFF;
            for (int bit = 0; bit < 8; bit++) {
                ones = ones + ((b >> bit) & 1);
            }
            if (ones % 2 == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether it is one of the sixteen weak or semi-weak keys. See the class's note.
     *
     * @throws InvalidKeyException if fewer than eight bytes are left from `offset`
     * @throws NullPointerException if the array is null
     */
    public static boolean isWeak(byte[] key, int offset) throws InvalidKeyException {
        requireEight(key, offset);
        for (int i = 0; i < PROBLEMATIC.length; i++) {
            boolean same = true;
            for (int j = 0; j < DES_KEY_LEN && same; j++) {
                // WHOLE bytes are compared, parity bit included, and that is what JDK 25 does --
                // measured. One would expect the opposite: DES ignores the parity bit, so 0x00 and
                // 0x01 are the same key to the algorithm and the all-zeros one should be as weak as
                // the all-ones one. `isWeak` answers `false` for the zeros one and `true` for the
                // ones one.
                //
                // It is a JDK decision and not an oversight: the standard's list publishes the
                // sixteen keys WITH their parity adjusted, and the method answers for that list and
                // not for the equivalence class. The practical consequence is that `isWeak` is not
                // enough on its own -- the parity has to be adjusted before asking.
                if (key[offset + j] != PROBLEMATIC[i][j]) {
                    same = false;
                }
            }
            if (same) {
                return true;
            }
        }
        return false;
    }

    private static void requireEight(byte[] key, int offset) throws InvalidKeyException {
        if (key == null) {
            throw new NullPointerException("the key cannot be null");
        }
        if (key.length - offset < DES_KEY_LEN) {
            throw new InvalidKeyException(
                    "a DES key is " + DES_KEY_LEN + " bytes from the offset");
        }
    }
}
