package javax.security.auth.kerberos;

import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

/**
 * KajiLibrary's javax.security.auth.kerberos.KerberosKey -- una clave de largo plazo de un principal.
 *
 * <p>Es lo que hay en un keytab: la clave de un servicio, con su principal y su numero de version.
 * Se distingue de {@link EncryptionKey} --que es solo material-- en que sabe de quien es.
 *
 * <h2>El constructor con contrasena</h2>
 *
 * <p>El JDK deriva la clave de una contrasena con el algoritmo <i>string-to-key</i> del tipo pedido,
 * que necesita DES o AES. KajiLibrary no tiene esos cifradores, asi que ese constructor lanza
 * {@link IllegalArgumentException} con el mismo mensaje que el JDK usa para un algoritmo que no
 * conoce. Es una omision declarada y no una clave inventada: una derivada mal seria peor que
 * ninguna.
 *
 * <h2>Se destruye</h2>
 *
 * <p>Igual que {@link EncryptionKey}: despues de {@link #destroy} todo lo que pregunte por la clave
 * --incluso el principal y la version-- lanza {@link IllegalStateException}.
 */
public class KerberosKey implements SecretKey {

    private static final long serialVersionUID = -4625402278148246993L;

    /** De quien es, o null si no se sabe. */
    private KerberosPrincipal principal;

    /** El numero de version en el keytab. */
    private final int versionNum;

    /** El material. */
    private EncryptionKey key;

    /** Si ya se borro. */
    private transient boolean destroyed = false;

    /**
     * Con esos bytes. El arreglo se copia.
     *
     * @param principal de quien es, o null
     * @param versionNum el numero de version; 0 si no se sabe
     * @throws NullPointerException si los bytes son null
     */
    public KerberosKey(KerberosPrincipal principal, byte[] keyBytes, int keyType, int versionNum) {
        this.principal = principal;
        this.versionNum = versionNum;
        this.key = new EncryptionKey(keyBytes, keyType);
    }

    /**
     * Derivada de una contrasena. Ver la nota de la clase: en KajiLibrary siempre falla.
     *
     * @param algorithm {@code "DES"}, {@code "DESede"}, {@code "AES128"}, {@code "AES256"},
     *     {@code "ArcFourHmac"}, o null por {@code "DES"}
     * @throws NullPointerException si el principal es null
     * @throws IllegalArgumentException siempre, porque no hay cifradores con que derivarla
     */
    public KerberosKey(KerberosPrincipal principal, char[] password, String algorithm) {
        if (principal == null) {
            throw new NullPointerException("principal == null");
        }
        String name = algorithm == null ? "DES" : algorithm;
        throw new IllegalArgumentException("Algorithm " + name + " not supported");
    }

    /**
     * De quien es, o null.
     *
     * @throws IllegalStateException si esta destruida
     */
    public final KerberosPrincipal getPrincipal() {
        checkAlive();
        return this.principal;
    }

    /**
     * El numero de version.
     *
     * @throws IllegalStateException si esta destruida
     */
    public final int getVersionNumber() {
        checkAlive();
        return this.versionNum;
    }

    /**
     * El numero de tipo.
     *
     * @throws IllegalStateException si esta destruida
     */
    public final int getKeyType() {
        checkAlive();
        return this.key.getKeyType();
    }

    /**
     * Como se llama el tipo.
     *
     * @throws IllegalStateException si esta destruida
     */
    @Override
    public final String getAlgorithm() {
        checkAlive();
        return this.key.getAlgorithm();
    }

    /**
     * Siempre {@code "RAW"}.
     *
     * @throws IllegalStateException si esta destruida
     */
    @Override
    public final String getFormat() {
        checkAlive();
        return this.key.getFormat();
    }

    /**
     * Los bytes. Una copia.
     *
     * @throws IllegalStateException si esta destruida
     */
    @Override
    public final byte[] getEncoded() {
        checkAlive();
        return this.key.getEncoded();
    }

    /** Borra la clave. Destruir dos veces no hace nada. */
    @Override
    public void destroy() throws DestroyFailedException {
        if (!this.destroyed) {
            this.key.destroy();
            this.principal = null;
            this.destroyed = true;
        }
    }

    /** Si ya se borro. */
    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    /** El principal, la version y el tipo; nunca los bytes. */
    @Override
    public String toString() {
        if (this.destroyed) {
            return "Destroyed KerberosKey";
        }
        return "KerberosKey: principal " + this.principal + ", version " + this.versionNum
            + ", key " + this.key.toString().substring("EncryptionKey: ".length());
    }

    /** Una destruida vale 17. */
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
     * Iguales si tienen el mismo principal, version, tipo y bytes; una destruida solo es igual a si
     * misma.
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

    /** Lanza si ya se destruyo. */
    private void checkAlive() {
        if (this.destroyed) {
            throw new IllegalStateException("This key is no longer valid");
        }
    }
}
