package javax.crypto.spec;

import java.security.spec.KeySpec;
import java.util.Arrays;
import javax.crypto.SecretKey;

/**
 * A symmetric key that is, literally, an array of bytes and an algorithm name.
 *
 * <p>It is both a {@link KeySpec} and a {@link SecretKey}, and that double nature is its reason for
 * being: it can be passed to a `SecretKeyFactory` to be translated, or used directly in a cipher. It
 * serves the algorithms whose key material **has no structure** --AES, HmacSHA256--; for the ones
 * that do have it, like DES with its parity bit, there is a class of their own.
 *
 * <p><strong>It validates nothing.</strong> Neither the length nor the contents: a three-byte
 * `SecretKeySpec` for AES is built without complaint and fails only when a cipher uses it. It is on
 * purpose and it is what the JDK does -- this class does not know which algorithms exist nor which
 * lengths they accept, and pretending it did would lead to rejecting valid keys of an algorithm it
 * does not know.
 */
public class SecretKeySpec implements KeySpec, SecretKey {

    private static final long serialVersionUID = 6577238317307289933L;

    private final byte[] key;
    private final String algorithm;

    /**
     * @throws IllegalArgumentException if the key is null or empty, or the algorithm is null
     */
    public SecretKeySpec(byte[] key, String algorithm) {
        if (key == null) {
            throw new IllegalArgumentException("the key cannot be null");
        }
        if (key.length == 0) {
            throw new IllegalArgumentException("the key cannot be empty");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("the algorithm cannot be null");
        }
        this.key = IvParameterSpec.copy(key, 0, key.length);
        this.algorithm = algorithm;
    }

    /**
     * The key is `len` bytes starting at `offset`.
     *
     * @throws IllegalArgumentException if the key is null or empty, if the algorithm is null, or if
     *     the array is shorter than `offset + len`
     * @throws ArrayIndexOutOfBoundsException if `offset` or `len` are negative
     */
    public SecretKeySpec(byte[] key, int offset, int len, String algorithm) {
        if (key == null) {
            throw new IllegalArgumentException("the key cannot be null");
        }
        if (key.length == 0) {
            throw new IllegalArgumentException("the key cannot be empty");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("the algorithm cannot be null");
        }
        if (offset < 0 || len < 0) {
            throw new ArrayIndexOutOfBoundsException("negative offset or length");
        }
        if (key.length - offset < len) {
            throw new IllegalArgumentException("the key is shorter than offset + len");
        }
        if (len == 0) {
            throw new IllegalArgumentException("the key cannot be empty");
        }
        this.key = IvParameterSpec.copy(key, offset, len);
        this.algorithm = algorithm;
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    /** Always `"RAW"`: the bytes are the key, with no encoding in between. */
    public String getFormat() {
        return "RAW";
    }

    /** A copy of the key material. */
    public byte[] getEncoded() {
        return IvParameterSpec.copy(this.key, 0, this.key.length);
    }

    /**
     * The algorithm's name **in lower case** plus the bytes.
     *
     * <p>The lower case is not a detail: `equals` compares the algorithm case-insensitively --`"AES"`
     * and `"aes"` name the same algorithm-- and a `hashCode` that did tell them apart would put two
     * equal keys in different buckets.
     */
    public int hashCode() {
        int h = 0;
        for (int i = 1; i < this.key.length; i++) {
            h = h + (this.key[i] * i);
        }
        return h ^ this.algorithm.toLowerCase().hashCode();
    }

    /**
     * Equality by algorithm --case-insensitively-- and by the bytes.
     *
     * <p>The comparison of the bytes is **constant time**: it always walks both arrays whole instead
     * of stopping at the first difference. Comparing keys with a short circuit leaks how many leading
     * bytes match to anyone who can measure the time, which is how a key is broken by dint of
     * comparisons.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SecretKey)) {
            return false;
        }
        SecretKey other = (SecretKey) obj;
        if (!this.algorithm.equalsIgnoreCase(other.getAlgorithm())) {
            return false;
        }
        byte[] theirs = other.getEncoded();
        if (theirs == null) {
            return false;
        }
        boolean same = this.key.length == theirs.length;
        int n = this.key.length < theirs.length ? this.key.length : theirs.length;
        int diff = 0;
        for (int i = 0; i < n; i++) {
            diff = diff | (this.key[i] ^ theirs[i]);
        }
        Arrays.fill(theirs, (byte) 0);
        return same && diff == 0;
    }
}
