package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Genera claves simetricas.
 *
 * <h2>Por que no alcanza con bytes al azar</h2>
 *
 * <p>Para muchos algoritmos alcanzaria. Para otros no: DES y DESede tienen bits de paridad y claves
 * debiles que hay que descartar, y una clave para un algoritmo con estructura tiene que caer en un
 * rango. Sortear bytes y llamarlos clave produciria claves invalidas cada tanto, y --peor-- claves
 * validas pero debiles.
 *
 * <h2>Configurarlo es optativo</h2>
 *
 * <p>Sin configurar, usa el tamano de omision del algoritmo, que es el recomendado. Obligar a
 * elegirlo seria peor: es como se terminan escribiendo claves de 512 bits en programas que nadie
 * volvio a mirar.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>La maquinaria funciona entera, pero ningun proveedor registrado ofrece generadores de claves,
 * asi que {@link #getInstance} tira {@link NoSuchAlgorithmException} para cualquier nombre.
 * Registrar un proveedor propio lo hace andar.
 *
 * @since 1.4
 */
public class KeyGenerator {

    private final KeyGeneratorSpi spi;
    private final Provider provider;
    private final String algorithm;

    /**
     * Uno alrededor de esa implementacion.
     *
     * @param keyGenSpi la implementacion
     * @param provider de quien es
     * @param algorithm con que nombre se lo pidio
     */
    protected KeyGenerator(KeyGeneratorSpi keyGenSpi, Provider provider, String algorithm) {
        this.spi = keyGenSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    /**
     * Para que algoritmo genera claves.
     *
     * @return el algoritmo
     */
    public final String getAlgorithm() {
        return this.algorithm;
    }

    /**
     * Uno para ese algoritmo.
     *
     * @param algorithm el algoritmo
     * @return el motor
     * @throws NoSuchAlgorithmException si ningun proveedor lo tiene
     * @throws NullPointerException si el algoritmo es {@code null}
     */
    public static final KeyGenerator getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            final Provider.Service s = provs[i].getService("KeyGenerator", algorithm);
            if (s != null) {
                return armar(s, algorithm);
            }
        }
        throw new NoSuchAlgorithmException(algorithm + " KeyGenerator not available");
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
    public static final KeyGenerator getInstance(String algorithm, String provider)
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
    public static final KeyGenerator getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider.Service s = provider.getService("KeyGenerator", algorithm);
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
     * Lo configura con el tamano de omision.
     *
     * @param random de donde sacar el azar
     */
    public final void init(SecureRandom random) {
        this.spi.engineInit(random);
    }

    /**
     * Lo configura con parametros.
     *
     * @param params los parametros
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     */
    public final void init(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
        init(params, new SecureRandom());
    }

    /**
     * Lo configura con parametros, diciendo de donde sacar el azar.
     *
     * @param params los parametros
     * @param random de donde sacar el azar
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     */
    public final void init(AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidAlgorithmParameterException {
        this.spi.engineInit(params, random);
    }

    /**
     * Lo configura con un tamano.
     *
     * @param keysize el tamano en bits
     * @throws java.security.InvalidParameterException si ese tamano no sirve
     */
    public final void init(int keysize) {
        init(keysize, new SecureRandom());
    }

    /**
     * Lo configura con un tamano, diciendo de donde sacar el azar.
     *
     * @param keysize el tamano en bits
     * @param random de donde sacar el azar
     * @throws java.security.InvalidParameterException si ese tamano no sirve
     */
    public final void init(int keysize, SecureRandom random) {
        this.spi.engineInit(keysize, random);
    }

    /**
     * Genera una clave.
     *
     * @return la clave
     */
    public final SecretKey generateKey() {
        return this.spi.engineGenerateKey();
    }

    private static KeyGenerator armar(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        final Object o = s.newInstance(null);
        if (!(o instanceof KeyGeneratorSpi)) {
            throw new NoSuchAlgorithmException(
                    "class configured for KeyGenerator is not a KeyGeneratorSpi: " + s.getClassName());
        }
        return new KeyGenerator((KeyGeneratorSpi) o, s.getProvider(), algorithm);
    }
}
