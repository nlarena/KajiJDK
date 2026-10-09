package javax.security.auth.kerberos;

import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

/**
 * KajiLibrary's javax.security.auth.kerberos.KerberosKey -- a principal's long-term key.
 *
 * <p>It is what there is in a keytab: a service's key, with its principal and its version number.
 * It differs from {@link EncryptionKey} --which is only material-- in that it knows whose it is.
 *
 * <h2>The constructor with a password</h2>
 *
 * <p>The JDK derives the key from a password with the requested type's <i>string-to-key</i>
 * algorithm, which needs DES or AES. KajiLibrary does not have those ciphers, so that constructor
 * throws {@link IllegalArgumentException} with the same message the JDK uses for an algorithm it
 * does not know. It is a declared omission and not a made-up key: a badly derived one would be
 * worse than none.
 *
 * <h2>It is destroyed</h2>
 *
 * <p>Like {@link EncryptionKey}: after {@link #destroy} everything that asks about the key --even
 * the principal and the version-- throws {@link IllegalStateException}.
 */
public class KerberosKey implements SecretKey {

    private static final long serialVersionUID = -4625402278148246993L;

    /** Whose it is, or null if not known. */
    private KerberosPrincipal principal;

    /** The version number in the keytab. */
    private final int versionNum;

    /** The material. */
    private EncryptionKey key;

    /** Whether it was already erased. */
    private transient boolean destroyed = false;

    /**
     * With those bytes. The array is copied.
     *
     * @param principal whose it is, or null
     * @param versionNum the version number; 0 if not known
     * @throws NullPointerException if the bytes are null
     */
    public KerberosKey(KerberosPrincipal principal, byte[] keyBytes, int keyType, int versionNum) {
        this.principal = principal;
        this.versionNum = versionNum;
        this.key = new EncryptionKey(keyBytes, keyType);
    }

    /**
     * Derived from a password. See the class note: in KajiLibrary it always fails.
     *
     * @param algorithm {@code "DES"}, {@code "DESede"}, {@code "AES128"}, {@code "AES256"},
     *     {@code "ArcFourHmac"}, or null for {@code "DES"}
     * @throws NullPointerException if the principal is null
     * @throws IllegalArgumentException always, because there are no ciphers to derive it with
     */
    public KerberosKey(KerberosPrincipal principal, char[] password, String algorithm) {
        if (principal == null) {
            throw new NullPointerException("principal == null");
        }
        String name = algorithm == null ? "DES" : algorithm;
        throw new IllegalArgumentException("Algorithm " + name + " not supported");
    }

    /**
     * Whose it is, or null.
     *
     * @throws IllegalStateException if it is destroyed
     */
    public final KerberosPrincipal getPrincipal() {
        checkAlive();
        return this.principal;
    }

    /**
     * The version number.
     *
     * @throws IllegalStateException if it is destroyed
     */
    public final int getVersionNumber() {
        checkAlive();
        return this.versionNum;
    }

    /**
     * The type number.
     *
     * @throws IllegalStateException if it is destroyed
     */
    public final int getKeyType() {
        checkAlive();
        return this.key.getKeyType();
    }

    /**
     * What the type is called.
     *
     * @throws IllegalStateException if it is destroyed
     */
    @Override
    public final String getAlgorithm() {
        checkAlive();
        return this.key.getAlgorithm();
    }

    /**
     * Always {@code "RAW"}.
     *
     * @throws IllegalStateException if it is destroyed
     */
    @Override
    public final String getFormat() {
        checkAlive();
        return this.key.getFormat();
    }

    /**
     * The bytes. A copy.
     *
     * @throws IllegalStateException if it is destroyed
     */
    @Override
    public final byte[] getEncoded() {
        checkAlive();
        return this.key.getEncoded();
    }

    /** Erases the key. Destroying twice does nothing. */
    @Override
    public void destroy() throws DestroyFailedException {
        if (!this.destroyed) {
            this.key.destroy();
            this.principal = null;
            this.destroyed = true;
        }
    }

    /** Whether it was already erased. */
    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    /** The principal, the version and the type; never the bytes. */
    @Override
    public String toString() {
        if (this.destroyed) {
            return "Destroyed KerberosKey";
        }
        return "KerberosKey: principal " + this.principal + ", version " + this.versionNum
            + ", key " + this.key.toString().substring("EncryptionKey: ".length());
    }

    /** A destroyed one is 17. */
    @Override
    public int hashCode() {
        int result = 17;
        if (this.destroyed) {
            return result;
        }
        result = 37 * result + Arrays.hashCode(this.key.getEncoded());
        result = 37 * result + this.key.getKeyType();
        if (this.principal != null) {
            result = 37 * result + this.principal.hashCode();
        }
        return result * 37 + this.versionNum;
    }

    /**
     * Equal if they have the same principal, version, type and bytes; a destroyed one is only equal
     * to itself.
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof KerberosKey)) {
            return false;
        }
        KerberosKey that = (KerberosKey) other;
        if (this.destroyed || that.destroyed) {
            return false;
        }
        if (this.versionNum != that.versionNum || !this.key.equals(that.key)) {
            return false;
        }
        if (this.principal == null) {
            return that.principal == null;
        }
        return this.principal.equals(that.principal);
    }

    /** Throws if it was already destroyed. */
    private void checkAlive() {
        if (this.destroyed) {
            throw new IllegalStateException("This key is no longer valid");
        }
    }
}
