package javax.crypto;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Un codigo de autenticacion de mensaje: un resumen con clave.
 *
 * <h2>Que prueba</h2>
 *
 * <p>Que el mensaje no cambio y que lo escribio alguien que tiene la clave. Un resumen a secas prueba
 * lo primero pero no lo segundo: cualquiera que cambie el mensaje puede recalcular el resumen.
 *
 * <p>Lo que no prueba es cual de los dos lo escribio. Las dos partes comparten la misma clave, asi
 * que ninguna puede demostrarle a un tercero que fue la otra. Para eso hace falta una firma.
 *
 * <h2>Como se compara</h2>
 *
 * <p>Comparando los dos arreglos byte a byte con un bucle que corta al primer byte distinto se filtra
 * cuantos bytes coincidieron, y con eso se puede adivinar el codigo correcto uno a uno. La
 * comparacion tiene que mirar todos los bytes siempre, como hace
 * {@link java.security.MessageDigest#isEqual}.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>La maquinaria funciona entera, pero ningun proveedor registrado ofrece codigos de
 * autenticacion, asi que {@link #getInstance} tira {@link NoSuchAlgorithmException} para cualquier
 * nombre. Registrar un proveedor propio lo hace andar.
 *
 * @since 1.4
 */
public class Mac implements Cloneable {

    private MacSpi spi;
    private final Provider provider;
    private final String algorithm;
    private boolean initialized;

    /**
     * Uno alrededor de esa implementacion.
     *
     * @param macSpi la implementacion
     * @param provider de quien es
     * @param algorithm con que nombre se lo pidio
     */
    protected Mac(MacSpi macSpi, Provider provider, String algorithm) {
        this.spi = macSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    /**
     * Con que nombre se lo pidio.
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
     * @return el codigo de autenticacion
     * @throws NoSuchAlgorithmException si ningun proveedor lo tiene
     * @throws NullPointerException si el algoritmo es {@code null}
     */
    public static final Mac getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            final Provider.Service s = provs[i].getService("Mac", algorithm);
            if (s != null) {
                return armar(s, algorithm);
            }
        }
        throw new NoSuchAlgorithmException(algorithm + " Mac not available");
    }

    /**
     * Uno de ese proveedor, nombrado.
     *
     * @param algorithm el algoritmo
     * @param provider el nombre del proveedor
     * @return el codigo de autenticacion
     * @throws NoSuchAlgorithmException si ese proveedor no lo tiene
     * @throws NoSuchProviderException si no hay un proveedor con ese nombre
     * @throws IllegalArgumentException si el nombre del proveedor es {@code null} o vacio
     */
    public static final Mac getInstance(String algorithm, String provider)
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
     * @return el codigo de autenticacion
     * @throws NoSuchAlgorithmException si ese proveedor no lo tiene
     * @throws IllegalArgumentException si el proveedor es {@code null}
     */
    public static final Mac getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider.Service s = provider.getService("Mac", algorithm);
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
     * Cuanto mide lo que sale.
     *
     * @return el tamano en bytes
     */
    public final int getMacLength() {
        return this.spi.engineGetMacLength();
    }

    /**
     * Lo configura.
     *
     * @param key la clave
     * @throws InvalidKeyException si la clave no sirve
     */
    public final void init(Key key) throws InvalidKeyException {
        try {
            this.spi.engineInit(key, null);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException("init() failed", e);
        }
        this.initialized = true;
    }

    /**
     * Lo configura con parametros.
     *
     * @param key la clave
     * @param params los parametros
     * @throws InvalidKeyException si la clave no sirve
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     */
    public final void init(Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        this.spi.engineInit(key, params);
        this.initialized = true;
    }

    /**
     * Entrega un byte.
     *
     * @param input el byte
     * @throws IllegalStateException si no se lo configuro
     */
    public final void update(byte input) throws IllegalStateException {
        comprobar();
        this.spi.engineUpdate(input);
    }

    /**
     * Entrega datos.
     *
     * @param input los datos
     * @throws IllegalStateException si no se lo configuro
     */
    public final void update(byte[] input) throws IllegalStateException {
        comprobar();
        if (input != null) {
            this.spi.engineUpdate(input, 0, input.length);
        }
    }

    /**
     * Entrega parte de un arreglo.
     *
     * @param input los datos
     * @param offset desde donde
     * @param len cuantos
     * @throws IllegalStateException si no se lo configuro
     * @throws IllegalArgumentException si la porcion no esta bien
     */
    public final void update(byte[] input, int offset, int len) throws IllegalStateException {
        comprobar();
        if (input == null) {
            return;
        }
        if (offset < 0 || len < 0 || len > input.length - offset) {
            throw new IllegalArgumentException("Bad arguments");
        }
        this.spi.engineUpdate(input, offset, len);
    }

    /**
     * Entrega lo que quede en el buffer.
     *
     * @param input los datos; queda consumido
     * @throws IllegalStateException si no se lo configuro
     */
    public final void update(ByteBuffer input) {
        comprobar();
        if (input == null) {
            throw new IllegalArgumentException("Buffer must not be null");
        }
        this.spi.engineUpdate(input);
    }

    /**
     * Termina y devuelve el codigo.
     *
     * <p>Despues de esto queda listo para otro mensaje con la misma clave: no hay que volver a
     * configurarlo.
     *
     * @return el codigo
     * @throws IllegalStateException si no se lo configuro
     */
    public final byte[] doFinal() throws IllegalStateException {
        comprobar();
        final byte[] r = this.spi.engineDoFinal();
        this.spi.engineReset();
        return r;
    }

    /**
     * Termina y escribe el codigo en el arreglo dado.
     *
     * @param output donde escribirlo
     * @param outOffset desde donde
     * @throws ShortBufferException si el arreglo no alcanza
     * @throws IllegalStateException si no se lo configuro
     */
    public final void doFinal(byte[] output, int outOffset)
            throws ShortBufferException, IllegalStateException {
        comprobar();
        if (output == null || outOffset < 0) {
            throw new IllegalArgumentException("Bad arguments");
        }
        final int largo = getMacLength();
        if (output.length - outOffset < largo) {
            throw new ShortBufferException(
                    "Cannot store MAC in output buffer: " + largo + " bytes needed");
        }
        final byte[] r = doFinal();
        System.arraycopy(r, 0, output, outOffset, r.length);
    }

    /**
     * Entrega los ultimos datos, termina, y devuelve el codigo.
     *
     * @param input los datos
     * @return el codigo
     * @throws IllegalStateException si no se lo configuro
     */
    public final byte[] doFinal(byte[] input) throws IllegalStateException {
        comprobar();
        update(input);
        return doFinal();
    }

    /**
     * Lo deja listo para otro mensaje con la misma clave.
     *
     * <p>No borra la clave: eso seria volver a configurarlo.
     */
    public final void reset() {
        this.spi.engineReset();
    }

    /**
     * Una copia con el mismo estado.
     *
     * <p>Sirve para calcular el codigo de dos mensajes que empiezan igual sin recorrer dos veces la
     * parte comun.
     *
     * @return la copia
     * @throws CloneNotSupportedException si la implementacion no se puede copiar
     */
    @Override
    public final Object clone() throws CloneNotSupportedException {
        final Mac copia = (Mac) super.clone();
        copia.spi = (MacSpi) this.spi.clone();
        return copia;
    }

    private void comprobar() {
        if (!this.initialized) {
            throw new IllegalStateException("MAC not initialized");
        }
    }

    private static Mac armar(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        final Object o = s.newInstance(null);
        if (!(o instanceof MacSpi)) {
            throw new NoSuchAlgorithmException(
                    "class configured for Mac is not a MacSpi: " + s.getClassName());
        }
        return new Mac((MacSpi) o, s.getProvider(), algorithm);
    }
}
