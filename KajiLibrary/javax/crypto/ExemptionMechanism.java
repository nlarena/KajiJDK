package javax.crypto;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Un mecanismo de exencion: lo que permitia usar claves mas largas de lo que se podia exportar.
 *
 * <h2>De donde sale</h2>
 *
 * <p>De cuando exportar criptografia fuerte estaba restringido por ley. Un producto podia pasarse
 * del limite si ademas guardaba, junto al mensaje, un bloque que le permitiera a una autoridad
 * recuperarlo: deposito de claves, recuperacion de claves, o debilitamiento deliberado. Ese bloque
 * es lo que genera {@link #genExemptionBlob}.
 *
 * <p>{@link #isCryptoAllowed} era la pregunta que {@link Cipher} hacia antes de dejar usar una clave
 * larga: contestaba que si solo despues de que el bloque estuviera generado para esa clave.
 *
 * <h2>Hoy</h2>
 *
 * <p>Las restricciones se levantaron y el JDK no trae ningun mecanismo. La maquinaria quedo porque
 * sacarla romperia programas que la nombran, y porque un despliegue con reglas propias podria
 * registrar el suyo.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>La maquinaria funciona entera, pero ningun proveedor registrado ofrece mecanismos de exencion
 * --tampoco el JDK--, asi que {@link #getInstance} tira {@link NoSuchAlgorithmException} para
 * cualquier nombre. En esta biblioteca no hay ademas politica que aplicar:
 * {@link Cipher#getMaxAllowedKeyLength} no limita nada y {@link Cipher#getExemptionMechanism} da
 * siempre {@code null}.
 *
 * @since 1.4
 */
public class ExemptionMechanism {

    private final ExemptionMechanismSpi spi;
    private final Provider provider;
    private final String mechanism;

    private Key clave;
    private boolean generado;

    /**
     * Uno alrededor de esa implementacion.
     *
     * @param exmechSpi la implementacion
     * @param provider de quien es
     * @param mechanism con que nombre se lo pidio
     */
    protected ExemptionMechanism(ExemptionMechanismSpi exmechSpi, Provider provider,
            String mechanism) {
        this.spi = exmechSpi;
        this.provider = provider;
        this.mechanism = mechanism;
    }

    /**
     * Como se llama.
     *
     * @return el nombre del mecanismo
     */
    public final String getName() {
        return this.mechanism;
    }

    /**
     * Uno con ese nombre.
     *
     * @param algorithm el nombre del mecanismo
     * @return el mecanismo
     * @throws NoSuchAlgorithmException si ningun proveedor lo tiene
     * @throws NullPointerException si el nombre es {@code null}
     */
    public static final ExemptionMechanism getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            final Provider.Service s = provs[i].getService("ExemptionMechanism", algorithm);
            if (s != null) {
                return armar(s, algorithm);
            }
        }
        throw new NoSuchAlgorithmException(algorithm + " ExemptionMechanism not available");
    }

    /**
     * Uno de ese proveedor, nombrado.
     *
     * @param algorithm el nombre del mecanismo
     * @param provider el nombre del proveedor
     * @return el mecanismo
     * @throws NoSuchAlgorithmException si ese proveedor no lo tiene
     * @throws NoSuchProviderException si no hay un proveedor con ese nombre
     * @throws IllegalArgumentException si el nombre del proveedor es {@code null} o vacio
     */
    public static final ExemptionMechanism getInstance(String algorithm, String provider)
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
     * @param algorithm el nombre del mecanismo
     * @param provider el proveedor
     * @return el mecanismo
     * @throws NoSuchAlgorithmException si ese proveedor no lo tiene
     * @throws IllegalArgumentException si el proveedor es {@code null}
     */
    public static final ExemptionMechanism getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider.Service s = provider.getService("ExemptionMechanism", algorithm);
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
     * Si esa clave ya tiene su bloque de exencion generado.
     *
     * <p>Es la pregunta que decide si se la puede usar. Contesta que si solo cuando el bloque se
     * genero para esa misma clave: uno generado para otra no exime a esta.
     *
     * @param key la clave
     * @return cierto si ya se genero el bloque para ella
     * @throws ExemptionMechanismException si no se puede contestar
     */
    public final boolean isCryptoAllowed(Key key) throws ExemptionMechanismException {
        return this.generado && this.clave != null && this.clave.equals(key);
    }

    /**
     * Cuanto va a medir el bloque.
     *
     * @param inputLen cuanto mide la entrada
     * @return el tamano en bytes
     * @throws IllegalStateException si no se lo configuro
     */
    public final int getOutputSize(int inputLen) throws IllegalStateException {
        comprobar();
        return this.spi.engineGetOutputSize(inputLen);
    }

    /**
     * Lo configura.
     *
     * @param key la clave
     * @throws InvalidKeyException si la clave no sirve
     * @throws ExemptionMechanismException si algo mas sale mal
     */
    public final void init(Key key) throws InvalidKeyException, ExemptionMechanismException {
        this.generado = false;
        this.spi.engineInit(key);
        this.clave = key;
    }

    /**
     * Lo configura con parametros.
     *
     * @param key la clave
     * @param params los parametros
     * @throws InvalidKeyException si la clave no sirve
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     * @throws ExemptionMechanismException si algo mas sale mal
     */
    public final void init(Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException,
            ExemptionMechanismException {
        this.generado = false;
        this.spi.engineInit(key, params);
        this.clave = key;
    }

    /**
     * Lo configura con parametros ya codificados.
     *
     * @param key la clave
     * @param params los parametros
     * @throws InvalidKeyException si la clave no sirve
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     * @throws ExemptionMechanismException si algo mas sale mal
     */
    public final void init(Key key, AlgorithmParameters params)
            throws InvalidKeyException, InvalidAlgorithmParameterException,
            ExemptionMechanismException {
        this.generado = false;
        this.spi.engineInit(key, params);
        this.clave = key;
    }

    /**
     * Genera el bloque.
     *
     * @return el bloque
     * @throws IllegalStateException si no se lo configuro
     * @throws ExemptionMechanismException si algo sale mal
     */
    public final byte[] genExemptionBlob()
            throws IllegalStateException, ExemptionMechanismException {
        comprobar();
        final byte[] r = this.spi.engineGenExemptionBlob();
        this.generado = true;
        return r;
    }

    /**
     * Genera el bloque en el arreglo dado.
     *
     * @param output donde escribirlo
     * @return cuantos bytes se escribieron
     * @throws IllegalStateException si no se lo configuro
     * @throws ShortBufferException si el arreglo no alcanza
     * @throws ExemptionMechanismException si algo sale mal
     */
    public final int genExemptionBlob(byte[] output)
            throws IllegalStateException, ShortBufferException, ExemptionMechanismException {
        return genExemptionBlob(output, 0);
    }

    /**
     * Genera el bloque en el arreglo dado, desde esa posicion.
     *
     * @param output donde escribirlo
     * @param outputOffset desde donde
     * @return cuantos bytes se escribieron
     * @throws IllegalStateException si no se lo configuro
     * @throws ShortBufferException si el arreglo no alcanza
     * @throws ExemptionMechanismException si algo sale mal
     */
    public final int genExemptionBlob(byte[] output, int outputOffset)
            throws IllegalStateException, ShortBufferException, ExemptionMechanismException {
        comprobar();
        final int n = this.spi.engineGenExemptionBlob(output, outputOffset);
        this.generado = true;
        return n;
    }

    private void comprobar() {
        if (this.clave == null) {
            throw new IllegalStateException("ExemptionMechanism not initialized");
        }
    }

    private static ExemptionMechanism armar(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        final Object o = s.newInstance(null);
        if (!(o instanceof ExemptionMechanismSpi)) {
            throw new NoSuchAlgorithmException(
                    "class configured for ExemptionMechanism is not an ExemptionMechanismSpi: "
                            + s.getClassName());
        }
        return new ExemptionMechanism((ExemptionMechanismSpi) o, s.getProvider(), algorithm);
    }
}
