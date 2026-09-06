package javax.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * Convierte entre las dos formas de una clave simetrica.
 *
 * <h2>Las dos formas</h2>
 *
 * <p>La opaca --{@link SecretKey}-- puede vivir adentro de un dispositivo y no dejarse mirar. La
 * transparente --{@link KeySpec}-- es material que el programa arma o lee de un archivo. Esta
 * fabrica es el unico camino entre las dos.
 *
 * <h2>El uso mas comun</h2>
 *
 * <p>Derivar una clave de una contrasena: entra un
 * {@link javax.crypto.spec.PBEKeySpec} --contrasena, sal, cantidad de vueltas-- y sale una
 * {@link SecretKey}. La cantidad de vueltas es lo que hace que probar contrasenas cueste caro, y por
 * eso no se elige a ojo.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>La maquinaria funciona entera, pero ningun proveedor registrado ofrece fabricas de claves
 * simetricas, asi que {@link #getInstance} tira {@link NoSuchAlgorithmException} para cualquier
 * nombre. Registrar un proveedor propio lo hace andar.
 *
 * @since 1.4
 */
public class SecretKeyFactory {

    private final SecretKeyFactorySpi spi;
    private final Provider provider;
    private final String algorithm;

    /**
     * Una alrededor de esa implementacion.
     *
     * @param keyFacSpi la implementacion
     * @param provider de quien es
     * @param algorithm con que nombre se la pidio
     */
    protected SecretKeyFactory(SecretKeyFactorySpi keyFacSpi, Provider provider,
            String algorithm) {
        this.spi = keyFacSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    /**
     * Uno para ese algoritmo.
     *
     * @param algorithm el algoritmo
     * @return el motor
     * @throws NoSuchAlgorithmException si ningun proveedor lo tiene
     * @throws NullPointerException si el algoritmo es {@code null}
     */
    public static final SecretKeyFactory getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            final Provider.Service s = provs[i].getService("SecretKeyFactory", algorithm);
            if (s != null) {
                return armar(s, algorithm);
            }
        }
        throw new NoSuchAlgorithmException(algorithm + " SecretKeyFactory not available");
    }

    /**
     * Uno de ese proveedor, nombrado.
     *
     * @param algorithm el algoritmo
     * @param provider el nombre del proveedor
     * @return el motor
     * @throws NoSuchAlgorithmException si ese proveedor no lo tiene
     * @throws NoSuchProviderException si no hay un proveedor con ese nombre
     * @throws IllegalArgumentException si el nombre del proveedor es {@code null} o vacio
     */
    public static final SecretKeyFactory getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        final Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(algorithm, p);
    }

    /**
     * Uno de ese proveedor.
     *
     * @param algorithm el algoritmo
     * @param provider el proveedor
     * @return el motor
     * @throws NoSuchAlgorithmException si ese proveedor no lo tiene
     * @throws IllegalArgumentException si el proveedor es {@code null}
     */
    public static final SecretKeyFactory getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider.Service s = provider.getService("SecretKeyFactory", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                    "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return armar(s, algorithm);
    }

    /**
     * De quien es la implementacion.
     *
     * @return el proveedor
     */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * Para que algoritmo fabrica claves.
     *
     * @return el algoritmo
     */
    public final String getAlgorithm() {
        return this.algorithm;
    }

    /**
     * Arma una clave a partir de su descripcion.
     *
     * @param keySpec la descripcion
     * @return la clave
     * @throws InvalidKeySpecException si la descripcion no sirve para este algoritmo
     */
    public final SecretKey generateSecret(KeySpec keySpec) throws InvalidKeySpecException {
        return this.spi.engineGenerateSecret(keySpec);
    }

    /**
     * Describe una clave.
     *
     * @param key la clave
     * @param keySpec que descripcion se quiere
     * @return la descripcion
     * @throws InvalidKeySpecException si la clave no se puede describir asi
     */
    public final KeySpec getKeySpec(SecretKey key, Class<?> keySpec)
            throws InvalidKeySpecException {
        return this.spi.engineGetKeySpec(key, keySpec);
    }

    /**
     * Convierte una clave de otro proveedor a una de este.
     *
     * @param key la clave
     * @return la clave equivalente de este proveedor
     * @throws InvalidKeyException si no se la puede convertir
     */
    public final SecretKey translateKey(SecretKey key) throws InvalidKeyException {
        return this.spi.engineTranslateKey(key);
    }

    private static SecretKeyFactory armar(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        final Object o = s.newInstance(null);
        if (!(o instanceof SecretKeyFactorySpi)) {
            throw new NoSuchAlgorithmException(
                    "class configured for SecretKeyFactory is not a SecretKeyFactorySpi: " + s.getClassName());
        }
        return new SecretKeyFactory((SecretKeyFactorySpi) o, s.getProvider(), algorithm);
    }
}
