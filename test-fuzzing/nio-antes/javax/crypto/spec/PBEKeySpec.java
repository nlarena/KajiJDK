package javax.crypto.spec;

import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * A password --and optionally its salt, iterations and key length-- for deriving a key.
 *
 * <p><strong>The password is a `char[]` and not a `String`, and that is this class's whole
 * idea.</strong> A `String` is immutable and lives in the pool until the collector picks it up: a
 * password there stays in memory for a time nobody controls, and shows up in a dump. An array can be
 * **wiped**, and {@link #clearPassword} is what does it.
 *
 * <p>That is why the constructor copies the array and `getPassword` returns another copy: the caller
 * can wipe its own straight away without breaking this object. And that is why `getPassword`
 * **throws** after `clearPassword` instead of returning zeros -- returning a blank password as if it
 * were valid is the kind of error that goes unnoticed until something is encrypted with the wrong
 * key.
 */
public class PBEKeySpec implements KeySpec {

    private char[] password;
    private final byte[] salt;
    private final int iterationCount;
    private final int keyLength;

    /**
     * The password alone. A null is taken as an empty password, which is what the JDK does.
     */
    public PBEKeySpec(char[] password) {
        this.password = password == null ? new char[0] : copy(password);
        this.salt = null;
        this.iterationCount = 0;
        this.keyLength = 0;
    }

    /**
     * With salt and iterations.
     *
     * @throws NullPointerException if the salt is null
     * @throws IllegalArgumentException if the salt is empty or the iterations are not positive
     */
    public PBEKeySpec(char[] password, byte[] salt, int iterationCount) {
        this(password, salt, iterationCount, 0, false);
    }

    /**
     * With salt, iterations and key length in bits.
     *
     * @throws NullPointerException if the salt is null
     * @throws IllegalArgumentException if the salt is empty, or if the iterations or the length are
     *     not positive
     */
    public PBEKeySpec(char[] password, byte[] salt, int iterationCount, int keyLength) {
        this(password, salt, iterationCount, keyLength, true);
    }

    private PBEKeySpec(char[] password, byte[] salt, int iterationCount, int keyLength,
            boolean withLength) {
        if (salt == null) {
            throw new NullPointerException("the salt cannot be null");
        }
        if (salt.length == 0) {
            throw new IllegalArgumentException("the salt cannot be empty");
        }
        if (iterationCount <= 0) {
            throw new IllegalArgumentException("the iterations have to be positive");
        }
        if (withLength && keyLength <= 0) {
            throw new IllegalArgumentException("the key length has to be positive");
        }
        this.password = password == null ? new char[0] : copy(password);
        this.salt = IvParameterSpec.copy(salt, 0, salt.length);
        this.iterationCount = iterationCount;
        this.keyLength = keyLength;
    }

    private static char[] copy(char[] src) {
        char[] out = new char[src.length];
        System.arraycopy(src, 0, out, 0, src.length);
        return out;
    }

    /**
     * Wipes the password from memory.
     *
     * <p>It is overwritten with zeros **before** the reference is dropped: dropping it alone would
     * leave the characters on the heap until the collector came by, which is exactly what this class
     * exists to avoid.
     */
    public final synchronized void clearPassword() {
        if (this.password != null) {
            Arrays.fill(this.password, (char) 0);
            this.password = null;
        }
    }

    /**
     * A copy of the password.
     *
     * @throws IllegalStateException if {@link #clearPassword} has already been called
     */
    public final synchronized char[] getPassword() {
        if (this.password == null) {
            throw new IllegalStateException("the password has already been wiped");
        }
        return copy(this.password);
    }

    /** A copy of the salt, or null if it has none. */
    public final byte[] getSalt() {
        return this.salt == null ? null : IvParameterSpec.copy(this.salt, 0, this.salt.length);
    }

    /** How many iterations, or zero if none were given. */
    public final int getIterationCount() {
        return this.iterationCount;
    }

    /** The key length in bits, or zero if none was given. */
    public final int getKeyLength() {
        return this.keyLength;
    }
}
