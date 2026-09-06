package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Dos partes terminan con la misma clave sin que esa clave haya viajado.
 *
 * <h2>Como puede ser</h2>
 *
 * <p>Cada parte manda su mitad publica y la combina con su mitad privada. La aritmetica esta armada
 * para que las dos combinaciones den lo mismo, y para que a quien mira pasar los dos mensajes
 * publicos no le alcance para calcularlo. Es la unica forma de que dos maquinas que nunca hablaron
 * compartan un secreto por un canal que cualquiera puede leer.
 *
 * <h2>Lo que no da</h2>
 *
 * <p>No dice con quien se acordo. Alguien en el medio puede acordar una clave con cada lado y
 * traducir entre las dos sin que ninguno se entere. Por eso un acuerdo de claves va siempre
 * acompanado de algo que autentique al otro: un certificado, una firma, una clave conocida de
 * antemano.
 *
 * <h2>{@link #generateSecret(String)}</h2>
 *
 * <p>El secreto crudo no sirve como clave: su distribucion no es uniforme y usarlo directo es un
 * error conocido. Esta version lo pasa por donde corresponda antes de entregarlo, y es la que hay
 * que usar.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>La maquinaria funciona entera, pero ningun proveedor registrado ofrece acuerdos de claves, asi
 * que {@link #getInstance} tira {@link NoSuchAlgorithmException} para cualquier nombre. Registrar un
 * proveedor propio lo hace andar.
 *
 * @since 1.4
 */
public class KeyAgreement {

    private final KeyAgreementSpi spi;
    private final Provider provider;
    private final String algorithm;

    /**
     * Uno alrededor de esa implementacion.
     *
     * @param keyAgreeSpi la implementacion
     * @param provider de quien es
     * @param algorithm con que nombre se lo pidio
     */
    protected KeyAgreement(KeyAgreementSpi keyAgreeSpi, Provider provider, String algorithm) {
        this.spi = keyAgreeSpi;
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
     * @return el motor
     * @throws NoSuchAlgorithmException si ningun proveedor lo tiene
     * @throws NullPointerException si el algoritmo es {@code null}
     */
    public static final KeyAgreement getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            final Provider.Service s = provs[i].getService("KeyAgreement", algorithm);
            if (s != null) {
                return armar(s, algorithm);
            }
        }
        throw new NoSuchAlgorithmException(algorithm + " KeyAgreement not available");
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
    public static final KeyAgreement getInstance(String algorithm, String provider)
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
    public static final KeyAgreement getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider.Service s = provider.getService("KeyAgreement", algorithm);
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
     * Lo configura con la mitad privada propia.
     *
     * @param key la clave privada
     * @throws InvalidKeyException si la clave no sirve
     */
    public final void init(Key key) throws InvalidKeyException {
        init(key, new SecureRandom());
    }

    /**
     * Lo configura, diciendo de donde sacar el azar.
     *
     * @param key la clave privada
     * @param random de donde sacar el azar
     * @throws InvalidKeyException si la clave no sirve
     */
    public final void init(Key key, SecureRandom random) throws InvalidKeyException {
        this.spi.engineInit(key, random);
    }

    /**
     * Lo configura con parametros.
     *
     * @param key la clave privada
     * @param params los parametros
     * @throws InvalidKeyException si la clave no sirve
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     */
    public final void init(Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        init(key, params, new SecureRandom());
    }

    /**
     * Lo configura con parametros, diciendo de donde sacar el azar.
     *
     * @param key la clave privada
     * @param params los parametros
     * @param random de donde sacar el azar
     * @throws InvalidKeyException si la clave no sirve
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     */
    public final void init(Key key, AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        this.spi.engineInit(key, params, random);
    }

    /**
     * Combina la mitad publica de otro participante.
     *
     * @param key la clave publica del otro
     * @param lastPhase si es el ultimo
     * @return la clave intermedia, o {@code null} si no hay
     * @throws InvalidKeyException si la clave no sirve
     * @throws IllegalStateException si no se lo configuro
     */
    public final Key doPhase(Key key, boolean lastPhase)
            throws InvalidKeyException, IllegalStateException {
        return this.spi.engineDoPhase(key, lastPhase);
    }

    /**
     * El secreto acordado, crudo.
     *
     * @return el secreto
     * @throws IllegalStateException si faltan fases
     */
    public final byte[] generateSecret() throws IllegalStateException {
        return this.spi.engineGenerateSecret();
    }

    /**
     * El secreto acordado, crudo, escrito en el arreglo dado.
     *
     * @param sharedSecret donde escribirlo
     * @param offset desde donde
     * @return cuantos bytes se escribieron
     * @throws IllegalStateException si faltan fases
     * @throws ShortBufferException si el arreglo no alcanza
     */
    public final int generateSecret(byte[] sharedSecret, int offset)
            throws IllegalStateException, ShortBufferException {
        return this.spi.engineGenerateSecret(sharedSecret, offset);
    }

    /**
     * El secreto acordado, ya convertido en clave de ese algoritmo.
     *
     * @param algorithm para que algoritmo
     * @return la clave
     * @throws IllegalStateException si faltan fases
     * @throws NoSuchAlgorithmException si no hay como armar una clave de ese algoritmo
     * @throws InvalidKeyException si el secreto no da para una clave de ese algoritmo
     */
    public final SecretKey generateSecret(String algorithm)
            throws IllegalStateException, NoSuchAlgorithmException, InvalidKeyException {
        return this.spi.engineGenerateSecret(algorithm);
    }

    private static KeyAgreement armar(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        final Object o = s.newInstance(null);
        if (!(o instanceof KeyAgreementSpi)) {
            throw new NoSuchAlgorithmException(
                    "class configured for KeyAgreement is not a KeyAgreementSpi: " + s.getClassName());
        }
        return new KeyAgreement((KeyAgreementSpi) o, s.getProvider(), algorithm);
    }
}
