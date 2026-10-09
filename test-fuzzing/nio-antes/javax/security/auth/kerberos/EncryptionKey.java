package javax.security.auth.kerberos;

import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

/**
 * KajiLibrary's javax.security.auth.kerberos.EncryptionKey -- a Kerberos key, with its type.
 *
 * <p>It is the bytes and the encryption type number; {@link #getAlgorithm} translates the number to
 * a name. It cannot encrypt: it is the <b>material</b>, and whoever encrypts is someone else.
 *
 * <h2>It is destroyed</h2>
 *
 * <p>{@link #destroy} erases the bytes and leaves the object unusable: everything that asks about
 * the key throws {@link IllegalStateException}. It is not a whim: a Kerberos key in memory is a
 * password in memory, and the program that finished using it has to be able to make sure it is
 * gone. A destroyed object is equal to nothing but itself.
 */
public final class EncryptionKey implements SecretKey {

    private static final long serialVersionUID = 9L;

    /** The bytes, or null once destroyed. */
    private byte[] keyBytes;

    /** The type number. */
    private final int keyType;

    /** Whether it was already erased. */
    private transient boolean destroyed = false;

    /**
     * With those bytes and that type. The array is copied.
     *
     * @throws NullPointerException if the bytes are null
     */
    public EncryptionKey(byte[] keyBytes, int keyType) {
        this.keyBytes = keyBytes.clone();
        this.keyType = keyType;
    }

    /**
     * The type number.
     *
     * @throws IllegalStateException if it is destroyed
     */
    public int getKeyType() {
        checkAlive();
        return this.keyType;
    }

    /**
     * What the type is called: {@code "aes128-cts-hmac-sha1-96"}, {@code "des-cbc-md5"}...
     *
     * @throws IllegalStateException if it is destroyed
     */
    @Override
    public String getAlgorithm() {
        checkAlive();
        return EncryptionTypes.algorithmName(this.keyType);
    }

    /**
     * Always {@code "RAW"}.
     *
     * @throws IllegalStateException if it is destroyed
     */
    @Override
    public String getFormat() {
        checkAlive();
        return "RAW";
    }

    /**
     * The bytes. A copy.
     *
     * @throws IllegalStateException if it is destroyed
     */
    @Override
    public byte[] getEncoded() {
        checkAlive();
        return this.keyBytes.clone();
    }

    /** Erases the bytes. See the class note. Destroying twice does nothing. */
    @Override
    public void destroy() throws DestroyFailedException {
        if (!this.destroyed) {
            if (this.keyBytes != null) {
                Arrays.fill(this.keyBytes, (byte) 0);
                this.keyBytes = null;
            }
            this.destroyed = true;
        }
    }

    /** Whether it was already erased. */
    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    /** The type and the length; never the bytes. */
    @Override
    public String toString() {
        if (this.destroyed) {
            return "Destroyed EncryptionKey";
        }
        return "EncryptionKey: keyType=" + this.keyType + ", " + this.keyBytes.length + "-byte key";
    }

    /** A destroyed one is 17. */
    @Override
    public int hashCode() {
        int result = 17;
        if (this.destroyed) {
            return result;
        }
        result = 37 * result + Arrays.hashCode(this.keyBytes);
        result = 37 * result + this.keyType;
        return result;
    }

    /**
     * Equal if they have the same type and the same bytes; a destroyed one is only equal to itself.
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof EncryptionKey)) {
            return false;
        }
        EncryptionKey that = (EncryptionKey) other;
        if (this.destroyed || that.destroyed) {
            return false;
        }
        return this.keyType == that.keyType && Arrays.equals(this.keyBytes, that.keyBytes);
    }

    /** Throws if it was already destroyed. */
    private void checkAlive() {
        if (this.destroyed) {
            throw new IllegalStateException("This key is no longer valid");
        }
    }
}
