package javax.security.auth.kerberos;

import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

/**
 * KajiLibrary's javax.security.auth.kerberos.EncryptionKey -- una clave de Kerberos, con su tipo.
 *
 * <p>Son los bytes y el numero de tipo de cifrado; {@link #getAlgorithm} traduce el numero a nombre.
 * No sabe cifrar: es el <b>material</b>, y quien cifra es otro.
 *
 * <h2>Se destruye</h2>
 *
 * <p>{@link #destroy} borra los bytes y deja el objeto inservible: todo lo que pregunte por la clave
 * lanza {@link IllegalStateException}. No es un capricho: una clave de Kerberos en memoria es una
 * contrasena en memoria, y el programa que termino de usarla tiene que poder asegurarse de que ya no
 * esta. Un objeto destruido no es igual a nada mas que a si mismo.
 */
public final class EncryptionKey implements SecretKey {

    private static final long serialVersionUID = 9L;

    /** Los bytes, o null una vez destruida. */
    private byte[] keyBytes;

    /** El numero de tipo. */
    private final int keyType;

    /** Si ya se borro. */
    private transient boolean destroyed = false;

    /**
     * Con esos bytes y ese tipo. El arreglo se copia.
     *
     * @throws NullPointerException si los bytes son null
     */
    public EncryptionKey(byte[] keyBytes, int keyType) {
        this.keyBytes = keyBytes.clone();
        this.keyType = keyType;
    }

    /**
     * El numero de tipo.
     *
     * @throws IllegalStateException si esta destruida
     */
    public int getKeyType() {
        checkAlive();
        return this.keyType;
    }

    /**
     * Como se llama el tipo: {@code "aes128-cts-hmac-sha1-96"}, {@code "des-cbc-md5"}...
     *
     * @throws IllegalStateException si esta destruida
     */
    @Override
    public String getAlgorithm() {
        checkAlive();
        return EncryptionTypes.algorithmName(this.keyType);
    }

    /**
     * Siempre {@code "RAW"}.
     *
     * @throws IllegalStateException si esta destruida
     */
    @Override
    public String getFormat() {
        checkAlive();
        return "RAW";
    }

    /**
     * Los bytes. Una copia.
     *
     * @throws IllegalStateException si esta destruida
     */
    @Override
    public byte[] getEncoded() {
        checkAlive();
        return this.keyBytes.clone();
    }

    /** Borra los bytes. Ver la nota de la clase. Destruir dos veces no hace nada. */
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

    /** Si ya se borro. */
    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    /** El tipo y el largo; nunca los bytes. */
    @Override
    public String toString() {
        if (this.destroyed) {
            return "Destroyed EncryptionKey";
        }
        return "EncryptionKey: keyType=" + this.keyType + ", " + this.keyBytes.length + "-byte key";
    }

    /** Una destruida vale 17. */
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

    /** Iguales si tienen el mismo tipo y los mismos bytes; una destruida solo es igual a si misma. */
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

    /** Lanza si ya se destruyo. */
    private void checkAlive() {
        if (this.destroyed) {
            throw new IllegalStateException("This key is no longer valid");
        }
    }
}
