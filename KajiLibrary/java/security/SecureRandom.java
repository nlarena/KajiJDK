package java.security;

import java.util.Random;

/**
 * KajiLibrary's java.security.SecureRandom -- el generador del que salen las claves.
 *
 * <p>Hereda de {@link Random} y esa herencia es historica, no conceptual: lo unico que comparten es
 * la forma. Un {@code Random} es un generador <b>predecible</b> -- misma semilla, misma serie -- y
 * es lo correcto para una simulacion o un juego. Un {@code SecureRandom} promete lo contrario, y
 * usar el primero donde hace falta el segundo es el error de seguridad mas comun que existe: el
 * codigo anda, las pruebas pasan, y las claves son adivinables.
 *
 * <h2>Que hay detras aca</h2>
 *
 * <p>El proveedor de fabrica registra un solo algoritmo, {@code "OS-PRNG"}, que es un pase directo
 * al generador del sistema operativo -- {@code BCryptGenRandom} en Windows, {@code /dev/urandom} en
 * el resto. Ver {@code OsPrngSpi} para por que se eligio un pase directo y no un DRBG propio.
 *
 * <p><b>Diferencia anotada con el JDK</b>: alla {@code getInstance("SHA1PRNG")} y
 * {@code getInstance("DRBG")} funcionan. Aca lanzan {@code NoSuchAlgorithmException}, porque esta
 * biblioteca no implementa esos dos algoritmos y devolver otro con ese nombre seria mentir sobre
 * que construccion esta corriendo. Lo que si funciona, y es lo que casi todo el mundo usa, es
 * {@code new SecureRandom()}: toma el primero que haya, que aca es el del sistema.
 *
 * <h2>Tres detalles del contrato que se olvidan</h2>
 *
 * <ol>
 *   <li>{@code setSeed} <b>agrega</b> entropia, no la reemplaza. Dos generadores con la misma
 *       semilla <b>no</b> dan la misma serie, al reves que en {@link Random}. Contar con eso para
 *       reproducir una corrida es un error.
 *   <li>{@code generateSeed} no es {@code nextBytes}: el primero entrega entropia para sembrar a
 *       otro generador y el segundo entrega salida. Ver {@link SecureRandomSpi}.
 *   <li>{@code setSeed(long)} con cero <b>no hace nada</b>. Tiene que ser asi porque el constructor
 *       de {@link Random} llama a {@code setSeed} antes de que este objeto tenga su generador
 *       armado; sin esa salida, construir un {@code SecureRandom} reventaria.
 * </ol>
 */
public class SecureRandom extends Random {

    private static final long serialVersionUID = 4940670005562187L;

    private final SecureRandomSpi secureRandomSpi;
    private final Provider provider;
    private String algorithm;

    /**
     * El primer generador que ofrezca algun proveedor instalado.
     *
     * @throws ProviderException si no hay ninguno. El JDK garantiza que siempre hay uno; aca ese
     *     uno es el del sistema operativo, y si el sistema no puede dar entropia no hay nada
     *     razonable que devolver.
     */
    public SecureRandom() {
        // `super(0)` y no `super()`: el constructor de Random llama a setSeed, que esta
        // sobrescrito, y todavia no hay `secureRandomSpi`. Con cero, el override no hace nada.
        super(0);
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = firstSecureRandom(provs[i]);
            if (s != null) {
                try {
                    this.secureRandomSpi = (SecureRandomSpi) s.newInstance(null);
                    this.provider = provs[i];
                    this.algorithm = s.getAlgorithm();
                    return;
                } catch (NoSuchAlgorithmException e) {
                    // Un proveedor que anuncia un servicio y no lo puede construir no descalifica a
                    // los que vienen despues.
                    i = i + 1;
                    continue;
                }
            }
            i = i + 1;
        }
        throw new ProviderException("no SecureRandom implementation is installed");
    }

    /**
     * Idem, sembrado con esos bytes.
     *
     * <p>La semilla <b>agrega</b>: dos generadores construidos con la misma no dan la misma serie.
     */
    public SecureRandom(byte[] seed) {
        this();
        this.secureRandomSpi.engineSetSeed(seed);
    }

    /** El constructor para quien trae su propia implementacion. */
    protected SecureRandom(SecureRandomSpi secureRandomSpi, Provider provider) {
        super(0);
        this.secureRandomSpi = secureRandomSpi;
        this.provider = provider;
        this.algorithm = null;
    }

    private static Provider.Service firstSecureRandom(Provider p) {
        java.util.Iterator<Provider.Service> it = p.getServices().iterator();
        while (it.hasNext()) {
            Provider.Service s = it.next();
            if ("SecureRandom".equals(s.getType())) {
                return s;
            }
        }
        return null;
    }

    /**
     * El generador de ese algoritmo, del primer proveedor que lo ofrezca.
     *
     * @throws NoSuchAlgorithmException si ningun proveedor lo ofrece
     */
    public static SecureRandom getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("SecureRandom", algorithm);
            if (s != null) {
                return build(s, algorithm, provs[i]);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(algorithm + " SecureRandom not available");
    }

    /**
     * Idem, exigiendo ese proveedor.
     *
     * @throws NoSuchProviderException si no hay un proveedor instalado con ese nombre
     */
    public static SecureRandom getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        if (provider == null || provider.length() == 0) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(algorithm, p);
    }

    /** Idem, con la instancia del proveedor en vez de su nombre. */
    public static SecureRandom getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider.Service s = provider.getService("SecureRandom", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return build(s, algorithm, provider);
    }

    /**
     * El generador de ese algoritmo configurado con esos parametros.
     *
     * <p>Los parametros solo los entiende un DRBG. El generador de fabrica de esta biblioteca no lo
     * es, asi que aca esta sobrecarga siempre termina en {@code NoSuchAlgorithmException}: el
     * servicio existe pero no acepta parametros, y ese es el error que corresponde.
     */
    public static SecureRandom getInstance(String algorithm, SecureRandomParameters params)
            throws NoSuchAlgorithmException {
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("SecureRandom", algorithm);
            if (s != null) {
                return buildWithParams(s, algorithm, provs[i], params);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(algorithm + " SecureRandom not available");
    }

    /** Idem, exigiendo ese proveedor por nombre. */
    public static SecureRandom getInstance(String algorithm, SecureRandomParameters params,
            String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        if (provider == null || provider.length() == 0) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(algorithm, params, p);
    }

    /** Idem, con la instancia del proveedor. */
    public static SecureRandom getInstance(String algorithm, SecureRandomParameters params,
            Provider provider) throws NoSuchAlgorithmException {
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider.Service s = provider.getService("SecureRandom", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return buildWithParams(s, algorithm, provider, params);
    }

    private static SecureRandom build(Provider.Service s, String algorithm, Provider p)
            throws NoSuchAlgorithmException {
        SecureRandom sr = new SecureRandom((SecureRandomSpi) s.newInstance(null), p);
        sr.algorithm = algorithm;
        return sr;
    }

    private static SecureRandom buildWithParams(Provider.Service s, String algorithm, Provider p,
            SecureRandomParameters params) throws NoSuchAlgorithmException {
        Object o = s.newInstance(params);
        if (!(o instanceof SecureRandomSpi)) {
            throw new NoSuchAlgorithmException(algorithm + " does not accept parameters");
        }
        SecureRandom sr = new SecureRandom((SecureRandomSpi) o, p);
        sr.algorithm = algorithm;
        return sr;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    /** El nombre del algoritmo, o {@code "unknown"} si se construyo con un SPI a mano. */
    public String getAlgorithm() {
        return this.algorithm == null ? "unknown" : this.algorithm;
    }

    @Override
    public String toString() {
        return this.secureRandomSpi.toString();
    }

    /** Los parametros con los que se creo, o null si no tiene. */
    public SecureRandomParameters getParameters() {
        return this.secureRandomSpi.engineGetParameters();
    }

    /** Agrega esa semilla. Ver la nota de la clase: agrega, no reemplaza. */
    public void setSeed(byte[] seed) {
        if (seed == null) {
            throw new NullPointerException("seed is null");
        }
        this.secureRandomSpi.engineSetSeed(seed);
    }

    /**
     * Agrega los ocho bytes de ese entero como semilla.
     *
     * <p>Con cero no hace nada, y no es un capricho: el constructor de {@link Random} llama a este
     * metodo antes de que el generador exista. Ver la nota de la clase.
     */
    @Override
    public void setSeed(long seed) {
        if (seed != 0) {
            this.secureRandomSpi.engineSetSeed(longToByteArray(seed));
        }
    }

    private static byte[] longToByteArray(long l) {
        byte[] out = new byte[8];
        int i = 0;
        while (i < 8) {
            out[i] = (byte) l;
            l = l >> 8;
            i = i + 1;
        }
        return out;
    }

    /** Llena el arreglo con la salida del generador. */
    @Override
    public void nextBytes(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        this.secureRandomSpi.engineNextBytes(bytes);
    }

    /**
     * Idem, con parametros por llamada.
     *
     * @throws UnsupportedOperationException si el generador no es un DRBG
     */
    public void nextBytes(byte[] bytes, SecureRandomParameters params) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        this.secureRandomSpi.engineNextBytes(bytes, params);
    }

    /**
     * Los `numBits` bits de abajo, sacados del generador.
     *
     * <p>Es el metodo que {@link Random} llama desde {@code nextInt}, {@code nextLong} y compañia,
     * y por eso alcanza con sobrescribirlo para que **todos** ellos pasen a ser criptograficos. Es
     * `final`: una subclase que lo cambiara podria devolver bits predecibles sin que nada mas de la
     * clase se entere.
     */
    @Override
    protected final int next(int numBits) {
        int numBytes = (numBits + 7) / 8;
        byte[] b = new byte[numBytes];
        int next = 0;
        this.nextBytes(b);
        int i = 0;
        while (i < numBytes) {
            next = (next << 8) + (b[i] & 0xFF);
            i = i + 1;
        }
        return next >>> (numBytes * 8 - numBits);
    }

    /**
     * Bytes de entropia, del generador por omision.
     *
     * <p>Es estatico y por lo tanto no dice de que generador salen: usa el mismo que
     * {@code new SecureRandom()}. Para sembrar algo propio conviene el de instancia.
     */
    public static byte[] getSeed(int numBytes) {
        return new SecureRandom().generateSeed(numBytes);
    }

    /** Bytes de <b>entropia</b>, no de salida. Ver la nota de {@link SecureRandomSpi}. */
    public byte[] generateSeed(int numBytes) {
        if (numBytes < 0) {
            throw new IllegalArgumentException("numBytes cannot be negative");
        }
        return this.secureRandomSpi.engineGenerateSeed(numBytes);
    }

    /**
     * El generador mas fuerte que haya.
     *
     * <p>En el JDK se elige con la propiedad de seguridad {@code securerandom.strongAlgorithms}.
     * Aca no hay archivo de configuracion que leer (ver {@link Security}), y el unico generador de
     * fabrica es el del sistema operativo -- que es justamente el que esa propiedad nombraria --,
     * asi que devuelve el mismo que {@code new SecureRandom()}.
     *
     * @throws NoSuchAlgorithmException si no hay ninguno instalado
     */
    public static SecureRandom getInstanceStrong() throws NoSuchAlgorithmException {
        try {
            return new SecureRandom();
        } catch (ProviderException e) {
            throw new NoSuchAlgorithmException("no strong SecureRandom is available", e);
        }
    }

    /**
     * Resiembra el estado interno con entropia nueva.
     *
     * @throws UnsupportedOperationException si el generador no tiene estado interno que resembrar
     *     -- que es el caso del pase directo al sistema, ver {@code OsPrngSpi}
     */
    public void reseed() {
        this.secureRandomSpi.engineReseed(null);
    }

    /**
     * Idem, con parametros.
     *
     * @throws UnsupportedOperationException si el generador no es un DRBG
     */
    public void reseed(SecureRandomParameters params) {
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        this.secureRandomSpi.engineReseed(params);
    }
}
