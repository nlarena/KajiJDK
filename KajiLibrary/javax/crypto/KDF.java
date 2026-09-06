package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Deriva claves de otro material.
 *
 * <h2>Por que hay que derivar</h2>
 *
 * <p>Porque lo que se tiene casi nunca sirve como clave tal cual. El secreto que sale de un acuerdo
 * no tiene distribucion uniforme --hay valores mas probables que otros--; una contrasena tiene
 * poquisima entropia; y de un solo secreto suelen hacer falta varias claves distintas, una para cada
 * direccion y otra para autenticar. Derivar convierte una cosa en la otra, y ademas hace que las
 * claves derivadas no se puedan relacionar entre si: tener una no ayuda a encontrar las demas.
 *
 * <h2>Los dos juegos de parametros</h2>
 *
 * <p>Los de la funcion van en {@link #getInstance(String, KDFParameters)} y se fijan una vez; los de
 * cada derivacion van en {@link #deriveKey} y cambian en cada llamada. Separarlos es lo que permite
 * armar la funcion una vez y derivar muchas claves distintas de ella.
 *
 * <h2>{@link #deriveData}</h2>
 *
 * <p>Devuelve bytes en vez de una clave, para lo que no es una clave: un vector de inicializacion,
 * una sal, un identificador. Sale del mismo lugar y con las mismas garantias.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>La maquinaria funciona entera, pero ningun proveedor registrado ofrece funciones de derivacion,
 * asi que {@link #getInstance} tira {@link NoSuchAlgorithmException} para cualquier nombre.
 * Registrar un proveedor propio lo hace andar.
 *
 * @since 24
 */
public final class KDF {

    private final KDFSpi spi;
    private final Provider provider;
    private final String algorithm;

    private KDF(KDFSpi spi, Provider provider, String algorithm) {
        this.spi = spi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    /**
     * Con que nombre se la pidio.
     *
     * @return el algoritmo
     */
    public String getAlgorithm() {
        return this.algorithm;
    }

    /**
     * De quien es la implementacion.
     *
     * @return el nombre del proveedor
     */
    public String getProviderName() {
        return this.provider.getName();
    }

    /**
     * Los parametros con que se la armo.
     *
     * @return los parametros, o {@code null} si no se le dio ninguno
     */
    public KDFParameters getParameters() {
        return this.spi.engineGetParameters();
    }

    /**
     * Una para ese algoritmo.
     *
     * @param algorithm el algoritmo
     * @return la funcion
     * @throws NoSuchAlgorithmException si ningun proveedor lo tiene
     * @throws NullPointerException si el algoritmo es {@code null}
     */
    public static KDF getInstance(String algorithm) throws NoSuchAlgorithmException {
        try {
            return conParametros(algorithm, null, null, null);
        } catch (InvalidAlgorithmParameterException e) {
            // Sin parametros no hay parametros que rechazar.
            throw new NoSuchAlgorithmException(e.getMessage());
        }
    }

    /**
     * Una de ese proveedor, nombrado.
     *
     * @param algorithm el algoritmo
     * @param provider el nombre del proveedor
     * @return la funcion
     * @throws NoSuchAlgorithmException si ese proveedor no lo tiene
     * @throws NoSuchProviderException si no hay un proveedor con ese nombre
     */
    public static KDF getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        try {
            return getInstance(algorithm, null, provider);
        } catch (InvalidAlgorithmParameterException e) {
            throw new NoSuchAlgorithmException(e.getMessage());
        }
    }

    /**
     * Una de ese proveedor.
     *
     * @param algorithm el algoritmo
     * @param provider el proveedor
     * @return la funcion
     * @throws NoSuchAlgorithmException si ese proveedor no lo tiene
     */
    public static KDF getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        try {
            return getInstance(algorithm, null, provider);
        } catch (InvalidAlgorithmParameterException e) {
            throw new NoSuchAlgorithmException(e.getMessage());
        }
    }

    /**
     * Una para ese algoritmo, con esos parametros.
     *
     * @param algorithm el algoritmo
     * @param kdfParameters los parametros de la funcion, o {@code null}
     * @return la funcion
     * @throws NoSuchAlgorithmException si ningun proveedor lo tiene
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     */
    public static KDF getInstance(String algorithm, KDFParameters kdfParameters)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        return conParametros(algorithm, kdfParameters, null, null);
    }

    /**
     * Una de ese proveedor, nombrado, con esos parametros.
     *
     * @param algorithm el algoritmo
     * @param kdfParameters los parametros de la funcion, o {@code null}
     * @param provider el nombre del proveedor
     * @return la funcion
     * @throws NoSuchAlgorithmException si ese proveedor no lo tiene
     * @throws NoSuchProviderException si no hay un proveedor con ese nombre
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     */
    public static KDF getInstance(String algorithm, KDFParameters kdfParameters, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidAlgorithmParameterException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        final Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(algorithm, kdfParameters, p);
    }

    /**
     * Una de ese proveedor, con esos parametros.
     *
     * @param algorithm el algoritmo
     * @param kdfParameters los parametros de la funcion, o {@code null}
     * @param provider el proveedor
     * @return la funcion
     * @throws NoSuchAlgorithmException si ese proveedor no lo tiene
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     */
    public static KDF getInstance(String algorithm, KDFParameters kdfParameters, Provider provider)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        return conParametros(algorithm, kdfParameters, provider, null);
    }

    /**
     * Deriva una clave.
     *
     * @param alg para que algoritmo es la clave
     * @param derivationSpec los parametros de esta derivacion
     * @return la clave
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     * @throws NoSuchAlgorithmException si no hay como armar una clave de ese algoritmo
     * @throws NullPointerException si el algoritmo es {@code null}
     */
    public SecretKey deriveKey(String alg, AlgorithmParameterSpec derivationSpec)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        if (alg == null) {
            throw new NullPointerException("the algorithm must not be null");
        }
        return this.spi.engineDeriveKey(alg, derivationSpec);
    }

    /**
     * Deriva bytes.
     *
     * @param derivationSpec los parametros de esta derivacion
     * @return los bytes
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     */
    public byte[] deriveData(AlgorithmParameterSpec derivationSpec)
            throws InvalidAlgorithmParameterException {
        return this.spi.engineDeriveData(derivationSpec);
    }

    /**
     * Busca el servicio y arma la funcion.
     *
     * <p>Los parametros de la funcion viajan como parametro de construccion del servicio, que es
     * para lo que ese argumento existe en {@link Provider.Service#newInstance}.
     */
    private static KDF conParametros(String algorithm, KDFParameters params, Provider unico,
            String noUsado)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider[] provs = unico == null
                ? Security.getProviders() : new Provider[] {unico};
        for (int i = 0; i < provs.length; i++) {
            final Provider.Service s = provs[i].getService("KDF", algorithm);
            if (s != null) {
                final Object o = s.newInstance(params);
                if (!(o instanceof KDFSpi)) {
                    throw new NoSuchAlgorithmException(
                            "class configured for KDF is not a KDFSpi: " + s.getClassName());
                }
                return new KDF((KDFSpi) o, s.getProvider(), algorithm);
            }
        }
        throw new NoSuchAlgorithmException(algorithm + " KDF not available");
    }
}
