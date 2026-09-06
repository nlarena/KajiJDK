package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Encapsulacion de claves: como se acuerda un secreto con quien solo publico su clave publica.
 *
 * <h2>En que se diferencia de cifrar</h2>
 *
 * <p>En que el secreto no se elige, se genera. Cifrar una clave con RSA obliga a que el que cifra
 * elija que cifrar, y esa eleccion es una de las fuentes historicas de errores: relleno mal hecho,
 * valores predecibles, la misma clave dos veces. Aca el que encapsula no elige nada: pide un secreto
 * y recibe el secreto y su encapsulacion.
 *
 * <p>La otra diferencia es que no hace falta que las dos partes hablen. Con un acuerdo de claves
 * --{@link KeyAgreement}-- los dos tienen que mandar su mitad; aca alcanza con la clave publica del
 * destinatario, que puede estar publicada desde hace anos.
 *
 * <h2>Por que ahora</h2>
 *
 * <p>Porque es la forma que toman los algoritmos que resisten a una computadora cuantica. Los
 * acuerdos de claves clasicos se apoyan en problemas que esa computadora resolveria; los que
 * reemplazan a esos se expresan naturalmente como encapsulacion, no como acuerdo.
 *
 * <h2>Los tamanos</h2>
 *
 * <p>{@link Encapsulator#secretSize} y {@link Encapsulator#encapsulationSize} se saben antes de
 * hacer nada. Es lo que permite reservar la memoria justa y armar protocolos de mensajes de tamano
 * fijo, que son mas faciles de analizar.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>La maquinaria funciona entera, pero ningun proveedor registrado ofrece encapsulacion de claves,
 * asi que {@link #getInstance} tira {@link NoSuchAlgorithmException} para cualquier nombre.
 * Registrar un proveedor propio lo hace andar.
 *
 * @since 21
 */
public final class KEM {

    private final KEMSpi spi;
    private final Provider provider;
    private final String algorithm;

    private KEM(KEMSpi spi, Provider provider, String algorithm) {
        this.spi = spi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    /**
     * Un secreto recien generado, junto con lo que hay que mandarle al otro para que lo recupere.
     *
     * @param key el secreto, ya como clave
     * @param encapsulation lo que se le manda al otro
     * @param params los parametros que hagan falta para recuperarlo, o {@code null}
     * @since 21
     */
    public static final class Encapsulated {

        private final SecretKey key;
        private final byte[] encapsulation;
        private final byte[] params;

        /**
         * Uno.
         *
         * @param key el secreto
         * @param encapsulation lo que se le manda al otro
         * @param params los parametros, o {@code null}
         * @throws NullPointerException si la clave o la encapsulacion son {@code null}
         */
        public Encapsulated(SecretKey key, byte[] encapsulation, byte[] params) {
            if (key == null) {
                throw new NullPointerException("key");
            }
            if (encapsulation == null) {
                throw new NullPointerException("encapsulation");
            }
            this.key = key;
            this.encapsulation = encapsulation;
            this.params = params;
        }

        /**
         * El secreto.
         *
         * @return la clave
         */
        public SecretKey key() {
            return this.key;
        }

        /**
         * Lo que hay que mandarle al otro.
         *
         * @return la encapsulacion
         */
        public byte[] encapsulation() {
            return this.encapsulation.clone();
        }

        /**
         * Los parametros que hagan falta para recuperarlo.
         *
         * @return los parametros, o {@code null}
         */
        public byte[] params() {
            return this.params == null ? null : this.params.clone();
        }
    }

    /** El lado que genera el secreto. */
    public static final class Encapsulator {

        private final KEMSpi.EncapsulatorSpi spi;
        private final String providerName;

        Encapsulator(KEMSpi.EncapsulatorSpi spi, String providerName) {
            this.spi = spi;
            this.providerName = providerName;
        }

        /**
         * De quien es la implementacion.
         *
         * @return el nombre del proveedor
         */
        public String providerName() {
            return this.providerName;
        }

        /**
         * Genera un secreto y su encapsulacion.
         *
         * @return el secreto y la encapsulacion
         */
        public Encapsulated encapsulate() {
            return encapsulate(0, secretSize(), "Generic");
        }

        /**
         * Lo mismo, quedandose con una parte del secreto.
         *
         * <p>Sirve cuando de un mismo secreto salen varias claves: se encapsula una vez y cada
         * pedazo va a un uso distinto.
         *
         * @param from desde que byte
         * @param to hasta que byte, sin incluirlo
         * @param algorithm para que algoritmo es la clave que sale
         * @return el secreto y la encapsulacion
         * @throws IndexOutOfBoundsException si el rango no cae adentro del secreto
         * @throws NullPointerException si el algoritmo es {@code null}
         */
        public Encapsulated encapsulate(int from, int to, String algorithm) {
            comprobarRango(from, to, secretSize(), algorithm);
            return this.spi.engineEncapsulate(from, to, algorithm);
        }

        /**
         * Cuanto mide el secreto.
         *
         * @return el tamano en bytes
         */
        public int secretSize() {
            return this.spi.engineSecretSize();
        }

        /**
         * Cuanto mide la encapsulacion.
         *
         * @return el tamano en bytes
         */
        public int encapsulationSize() {
            return this.spi.engineEncapsulationSize();
        }
    }

    /** El lado que recupera el secreto. */
    public static final class Decapsulator {

        private final KEMSpi.DecapsulatorSpi spi;
        private final String providerName;

        Decapsulator(KEMSpi.DecapsulatorSpi spi, String providerName) {
            this.spi = spi;
            this.providerName = providerName;
        }

        /**
         * De quien es la implementacion.
         *
         * @return el nombre del proveedor
         */
        public String providerName() {
            return this.providerName;
        }

        /**
         * Recupera el secreto entero.
         *
         * @param encapsulation lo que mando el otro
         * @return la clave
         * @throws DecapsulateException si no se pudo recuperar
         */
        public SecretKey decapsulate(byte[] encapsulation) throws DecapsulateException {
            return decapsulate(encapsulation, 0, secretSize(), "Generic");
        }

        /**
         * Recupera una parte del secreto.
         *
         * @param encapsulation lo que mando el otro
         * @param from desde que byte
         * @param to hasta que byte, sin incluirlo
         * @param algorithm para que algoritmo es la clave que sale
         * @return la clave
         * @throws DecapsulateException si no se pudo recuperar
         * @throws IndexOutOfBoundsException si el rango no cae adentro del secreto
         * @throws NullPointerException si la encapsulacion o el algoritmo son {@code null}
         * @throws IllegalArgumentException si la encapsulacion no mide lo que tiene que medir
         */
        public SecretKey decapsulate(byte[] encapsulation, int from, int to, String algorithm)
                throws DecapsulateException {
            if (encapsulation == null) {
                throw new NullPointerException("encapsulation");
            }
            comprobarRango(from, to, secretSize(), algorithm);
            if (encapsulation.length != encapsulationSize()) {
                throw new IllegalArgumentException("Invalid encapsulation size");
            }
            return this.spi.engineDecapsulate(encapsulation, from, to, algorithm);
        }

        /**
         * Cuanto mide el secreto.
         *
         * @return el tamano en bytes
         */
        public int secretSize() {
            return this.spi.engineSecretSize();
        }

        /**
         * Cuanto mide la encapsulacion.
         *
         * @return el tamano en bytes
         */
        public int encapsulationSize() {
            return this.spi.engineEncapsulationSize();
        }
    }

    /**
     * Uno para ese algoritmo.
     *
     * @param algorithm el algoritmo
     * @return el mecanismo
     * @throws NoSuchAlgorithmException si ningun proveedor lo tiene
     * @throws NullPointerException si el algoritmo es {@code null}
     */
    public static KEM getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            final KEM k = armar(provs[i], algorithm);
            if (k != null) {
                return k;
            }
        }
        throw new NoSuchAlgorithmException(algorithm + " KEM not available");
    }

    /**
     * Uno de ese proveedor.
     *
     * @param algorithm el algoritmo
     * @param provider el proveedor
     * @return el mecanismo
     * @throws NoSuchAlgorithmException si ese proveedor no lo tiene
     */
    public static KEM getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        final KEM k = armar(provider, algorithm);
        if (k != null) {
            return k;
        }
        throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
    }

    /**
     * Uno de ese proveedor, nombrado.
     *
     * @param algorithm el algoritmo
     * @param provider el nombre del proveedor
     * @return el mecanismo
     * @throws NoSuchAlgorithmException si ese proveedor no lo tiene
     * @throws NoSuchProviderException si no hay un proveedor con ese nombre
     */
    public static KEM getInstance(String algorithm, String provider)
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
     * Un encapsulador para esa clave publica.
     *
     * @param publicKey la clave publica del otro
     * @return el encapsulador
     * @throws InvalidKeyException si la clave no sirve
     */
    public Encapsulator newEncapsulator(PublicKey publicKey) throws InvalidKeyException {
        try {
            return newEncapsulator(publicKey, null, null);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(e.getMessage());
        }
    }

    /**
     * Lo mismo, diciendo de donde sacar el azar.
     *
     * @param publicKey la clave publica del otro
     * @param secureRandom de donde sacar el azar, o {@code null}
     * @return el encapsulador
     * @throws InvalidKeyException si la clave no sirve
     */
    public Encapsulator newEncapsulator(PublicKey publicKey, SecureRandom secureRandom)
            throws InvalidKeyException {
        try {
            return newEncapsulator(publicKey, null, secureRandom);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(e.getMessage());
        }
    }

    /**
     * Lo mismo, con parametros.
     *
     * @param publicKey la clave publica del otro
     * @param spec los parametros, o {@code null}
     * @param secureRandom de donde sacar el azar, o {@code null}
     * @return el encapsulador
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     * @throws InvalidKeyException si la clave no sirve
     * @throws NullPointerException si la clave es {@code null}
     */
    public Encapsulator newEncapsulator(PublicKey publicKey, AlgorithmParameterSpec spec,
            SecureRandom secureRandom)
            throws InvalidAlgorithmParameterException, InvalidKeyException {
        if (publicKey == null) {
            throw new NullPointerException("input key is null");
        }
        return new Encapsulator(this.spi.engineNewEncapsulator(publicKey, spec, secureRandom),
                this.provider.getName());
    }

    /**
     * Un desencapsulador para esa clave privada.
     *
     * @param privateKey la clave privada propia
     * @return el desencapsulador
     * @throws InvalidKeyException si la clave no sirve
     */
    public Decapsulator newDecapsulator(PrivateKey privateKey) throws InvalidKeyException {
        try {
            return newDecapsulator(privateKey, null);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(e.getMessage());
        }
    }

    /**
     * Lo mismo, con parametros.
     *
     * @param privateKey la clave privada propia
     * @param spec los parametros, o {@code null}
     * @return el desencapsulador
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     * @throws InvalidKeyException si la clave no sirve
     * @throws NullPointerException si la clave es {@code null}
     */
    public Decapsulator newDecapsulator(PrivateKey privateKey, AlgorithmParameterSpec spec)
            throws InvalidAlgorithmParameterException, InvalidKeyException {
        if (privateKey == null) {
            throw new NullPointerException("input key is null");
        }
        return new Decapsulator(this.spi.engineNewDecapsulator(privateKey, spec),
                this.provider.getName());
    }

    /**
     * Con que nombre se lo pidio.
     *
     * @return el algoritmo
     */
    public String getAlgorithm() {
        return this.algorithm;
    }

    private static KEM armar(Provider p, String algorithm) throws NoSuchAlgorithmException {
        final Provider.Service s = p.getService("KEM", algorithm);
        if (s == null) {
            return null;
        }
        final Object o = s.newInstance(null);
        if (!(o instanceof KEMSpi)) {
            throw new NoSuchAlgorithmException(
                    "class configured for KEM is not a KEMSpi: " + s.getClassName());
        }
        return new KEM((KEMSpi) o, s.getProvider(), algorithm);
    }

    /** El rango pedido tiene que caer adentro del secreto, y el algoritmo no puede faltar. */
    static void comprobarRango(int from, int to, int tam, String algorithm) {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        if (from < 0 || from > to || to > tam) {
            throw new IndexOutOfBoundsException("from: " + from + ", to: " + to + ", size: " + tam);
        }
    }
}
