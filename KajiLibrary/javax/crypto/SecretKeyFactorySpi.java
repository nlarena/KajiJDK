package javax.crypto;

import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * Lo que un proveedor tiene que escribir para ofrecer una fabrica de claves simetricas.
 *
 * <h2>Las dos formas de una clave</h2>
 *
 * <p>La opaca --{@link SecretKey}-- puede vivir adentro de un dispositivo y no dejarse mirar. La
 * transparente --{@link KeySpec}-- es material que el programa arma o lee de un archivo. Esta
 * fabrica es el unico camino entre las dos, y por eso es lo que se usa para derivar una clave de una
 * contrasena: {@link javax.crypto.spec.PBEKeySpec} entra, {@link SecretKey} sale.
 *
 * <h2>{@link #engineTranslateKey}</h2>
 *
 * <p>Convierte una clave de otro proveedor a una de este. Sirve para usar una clave que llego de
 * afuera con un proveedor que solo sabe trabajar con las suyas, sin exportar el material.
 *
 * @since 1.4
 */
public abstract class SecretKeyFactorySpi {

    /** Uno. */
    public SecretKeyFactorySpi() {
    }

    /**
     * Arma una clave a partir de su descripcion.
     *
     * @param keySpec la descripcion
     * @return la clave
     * @throws InvalidKeySpecException si la descripcion no sirve para este algoritmo
     */
    protected abstract SecretKey engineGenerateSecret(KeySpec keySpec)
            throws InvalidKeySpecException;

    /**
     * Describe una clave.
     *
     * @param key la clave
     * @param keySpec que descripcion se quiere
     * @return la descripcion
     * @throws InvalidKeySpecException si la clave no se puede describir asi
     */
    protected abstract KeySpec engineGetKeySpec(SecretKey key, Class<?> keySpec)
            throws InvalidKeySpecException;

    /**
     * Convierte una clave de otro proveedor a una de este.
     *
     * @param key la clave
     * @return la clave equivalente de este proveedor
     * @throws InvalidKeyException si no se la puede convertir
     */
    protected abstract SecretKey engineTranslateKey(SecretKey key) throws InvalidKeyException;
}
