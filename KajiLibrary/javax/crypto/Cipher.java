package javax.crypto;

import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Cifra y descifra.
 *
 * <h2>El nombre lleva tres cosas</h2>
 *
 * <p>{@code "AES/CBC/PKCS5Padding"} no es un algoritmo sino tres decisiones. El algoritmo dice como
 * se transforma un bloque; el modo dice como se encadenan los bloques entre si; el relleno dice como
 * se completa el ultimo. Las tres son necesarias y ninguna es un detalle: el mismo AES en modo ECB
 * deja ver los patrones del texto original, y en CBC no.
 *
 * <p>Se puede dar solo el algoritmo, y entonces el proveedor elige los otros dos. Conviene no
 * hacerlo: lo que elija depende del proveedor, y un programa que funciona en una maquina puede
 * cifrar distinto en otra.
 *
 * <h2>{@link #update} y {@link #doFinal}</h2>
 *
 * <p>Estan separados porque un cifrado por bloques no puede entregar nada hasta tener un bloque
 * entero, y porque el relleno solo se puede aplicar cuando se sabe que ya no viene mas. Por eso
 * {@link #update} suele devolver menos bytes de los que recibe --a veces ninguno-- y {@link #doFinal}
 * devuelve el resto.
 *
 * <p>Nada esta cifrado hasta que {@link #doFinal} vuelve. Quedarse con lo que dio {@link #update} y
 * no llamar a {@link #doFinal} produce un mensaje truncado, no un mensaje mas corto.
 *
 * <h2>Envolver claves</h2>
 *
 * <p>{@link #wrap} y {@link #unwrap} existen aparte de cifrar bytes porque una clave puede vivir
 * adentro de un dispositivo y no dejarse exportar. Envolverla ahi adentro es lo unico que se puede
 * hacer con ella; sacar sus bytes para cifrarlos no.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>La maquinaria funciona entera: {@link #getInstance} busca de verdad entre los proveedores
 * registrados, arma el cifrador, le fija modo y relleno, y todo lo demas se le delega. Lo que no hay
 * es un proveedor que ofrezca cifrados --ver la nota del proveedor de {@code java.security}: no se
 * registra un servicio que no se puede cumplir--, asi que {@link #getInstance} tira
 * {@link NoSuchAlgorithmException} para cualquier nombre. Registrar un proveedor propio lo hace
 * andar.
 *
 * <p>El unico cifrador que se puede construir de fabrica es {@link NullCipher}, que no cifra nada y
 * existe justamente para eso.
 *
 * @since 1.4
 */
public class Cipher {

    /** Que va a cifrar. */
    public static final int ENCRYPT_MODE = 1;

    /** Que va a descifrar. */
    public static final int DECRYPT_MODE = 2;

    /** Que va a envolver una clave. */
    public static final int WRAP_MODE = 3;

    /** Que va a desenvolver una clave. */
    public static final int UNWRAP_MODE = 4;

    /** Que lo desenvuelto es una clave publica. */
    public static final int PUBLIC_KEY = 1;

    /** Que lo desenvuelto es una clave privada. */
    public static final int PRIVATE_KEY = 2;

    /** Que lo desenvuelto es una clave simetrica. */
    public static final int SECRET_KEY = 3;

    private final CipherSpi spi;
    private final Provider provider;
    private final String transformation;

    /** Si ya se lo configuro. Sin esto, cifrar daria basura en vez de un error. */
    boolean initialized;

    /**
     * Si se lo puede usar sin configurar.
     *
     * <p>Lo prende {@link NullCipher}, que no tiene nada que configurar. Va aparte de
     * {@link #initialized} porque {@link #toString} tiene que seguir diciendo que no esta
     * configurado, que es lo que dice el JDK.
     */
    boolean sinConfigurar;

    /**
     * Uno alrededor de esa implementacion.
     *
     * <p>Es protegido y no publico porque el camino normal es {@link #getInstance}: quien lo llama
     * directamente esta armando un cifrador a mano, y eso solo tiene sentido para
     * {@link NullCipher} o para una subclase propia.
     *
     * @param cipherSpi la implementacion
     * @param provider de quien es
     * @param transformation el nombre con que se lo pidio
     */
    protected Cipher(CipherSpi cipherSpi, Provider provider, String transformation) {
        this.spi = cipherSpi;
        this.provider = provider;
        this.transformation = transformation;
    }

    /**
     * Un cifrador para esa transformacion.
     *
     * @param transformation el algoritmo, o el algoritmo con su modo y su relleno
     * @return el cifrador
     * @throws NoSuchAlgorithmException si el nombre esta mal formado, o si ningun proveedor lo tiene
     * @throws NoSuchPaddingException si ninguno tiene ese relleno
     */
    public static final Cipher getInstance(String transformation)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        final String[] partes = partir(transformation);
        final Provider[] provs = Security.getProviders();
        for (int i = 0; i < provs.length; i++) {
            final Cipher c = armar(provs[i], partes, transformation);
            if (c != null) {
                return c;
            }
        }
        throw new NoSuchAlgorithmException(
                "Cannot find any provider supporting " + transformation);
    }

    /**
     * Un cifrador de ese proveedor, nombrado.
     *
     * @param transformation el algoritmo, o el algoritmo con su modo y su relleno
     * @param provider el nombre del proveedor
     * @return el cifrador
     * @throws NoSuchAlgorithmException si el nombre esta mal formado, o si ese proveedor no lo tiene
     * @throws NoSuchProviderException si no hay un proveedor con ese nombre
     * @throws NoSuchPaddingException si ese proveedor no tiene ese relleno
     * @throws IllegalArgumentException si el nombre del proveedor es {@code null} o vacio
     */
    public static final Cipher getInstance(String transformation, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("Missing provider");
        }
        final Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("No such provider: " + provider);
        }
        return getInstance(transformation, p);
    }

    /**
     * Un cifrador de ese proveedor.
     *
     * @param transformation el algoritmo, o el algoritmo con su modo y su relleno
     * @param provider el proveedor
     * @return el cifrador
     * @throws NoSuchAlgorithmException si el nombre esta mal formado, o si ese proveedor no lo tiene
     * @throws NoSuchPaddingException si ese proveedor no tiene ese relleno
     * @throws IllegalArgumentException si el proveedor es {@code null}
     */
    public static final Cipher getInstance(String transformation, Provider provider)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        if (provider == null) {
            throw new IllegalArgumentException("Missing provider");
        }
        final String[] partes = partir(transformation);
        final Cipher c = armar(provider, partes, transformation);
        if (c != null) {
            return c;
        }
        throw new NoSuchAlgorithmException(
                "No such algorithm: " + transformation + " for provider " + provider.getName());
    }

    /**
     * De quien es la implementacion.
     *
     * @return el proveedor, o {@code null} si no viene de ninguno
     */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * Con que nombre se lo pidio.
     *
     * @return la transformacion, o {@code null} si no se lo pidio con un nombre
     */
    public final String getAlgorithm() {
        return this.transformation;
    }

    /**
     * Cuanto mide un bloque.
     *
     * @return el tamano en bytes, o cero si no es un cifrado por bloques
     */
    public final int getBlockSize() {
        return this.spi.engineGetBlockSize();
    }

    /**
     * Cuanto va a salir si ahora se entregan esos bytes y se termina.
     *
     * <p>Puede pasarse: lo que hay que reservar es esto, y lo que se escribe puede ser menos.
     *
     * @param inputLen cuantos bytes se van a entregar
     * @return el tamano en bytes
     * @throws IllegalStateException si no se lo configuro
     * @throws IllegalArgumentException si el largo es negativo
     */
    public final int getOutputSize(int inputLen) {
        comprobarConfigurado();
        if (inputLen < 0) {
            throw new IllegalArgumentException("Input size must be equal to or greater than zero");
        }
        return this.spi.engineGetOutputSize(inputLen);
    }

    /**
     * El vector de inicializacion.
     *
     * @return una copia del vector, o {@code null} si no hay
     */
    public final byte[] getIV() {
        return this.spi.engineGetIV();
    }

    /**
     * Los parametros con que quedo configurado.
     *
     * <p>Es como se entera el que descifra de lo que el que cifro genero al azar.
     *
     * @return los parametros, o {@code null} si no usa ninguno
     */
    public final AlgorithmParameters getParameters() {
        return this.spi.engineGetParameters();
    }

    /**
     * El mecanismo de exencion que se le aplico.
     *
     * @return siempre {@code null}: no hay politica de exportacion que aplicar, ver
     *     {@link ExemptionMechanism}
     */
    public final ExemptionMechanism getExemptionMechanism() {
        return null;
    }

    /**
     * Lo configura.
     *
     * @param opmode que va a hacer
     * @param key con que clave
     * @throws InvalidKeyException si la clave no sirve para este cifrado
     * @throws IllegalArgumentException si el modo de operacion no es uno de los cuatro
     */
    public final void init(int opmode, Key key) throws InvalidKeyException {
        init(opmode, key, new SecureRandom());
    }

    /**
     * Lo configura, diciendo de donde sacar el azar.
     *
     * @param opmode que va a hacer
     * @param key con que clave
     * @param random de donde sacar lo que haya que sortear
     * @throws InvalidKeyException si la clave no sirve para este cifrado
     * @throws IllegalArgumentException si el modo de operacion no es uno de los cuatro
     */
    public final void init(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
        comprobarModo(opmode);
        this.initialized = false;
        this.spi.engineInit(opmode, key, random);
        this.initialized = true;
    }

    /**
     * Lo configura con parametros.
     *
     * @param opmode que va a hacer
     * @param key con que clave
     * @param params los parametros
     * @throws InvalidKeyException si la clave no sirve para este cifrado
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     * @throws IllegalArgumentException si el modo de operacion no es uno de los cuatro
     */
    public final void init(int opmode, Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        init(opmode, key, params, new SecureRandom());
    }

    /**
     * Lo configura con parametros, diciendo de donde sacar el azar.
     *
     * @param opmode que va a hacer
     * @param key con que clave
     * @param params los parametros
     * @param random de donde sacar lo que haya que sortear
     * @throws InvalidKeyException si la clave no sirve para este cifrado
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     * @throws IllegalArgumentException si el modo de operacion no es uno de los cuatro
     */
    public final void init(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        comprobarModo(opmode);
        this.initialized = false;
        this.spi.engineInit(opmode, key, params, random);
        this.initialized = true;
    }

    /**
     * Lo configura con parametros ya codificados.
     *
     * @param opmode que va a hacer
     * @param key con que clave
     * @param params los parametros
     * @throws InvalidKeyException si la clave no sirve para este cifrado
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     * @throws IllegalArgumentException si el modo de operacion no es uno de los cuatro
     */
    public final void init(int opmode, Key key, AlgorithmParameters params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        init(opmode, key, params, new SecureRandom());
    }

    /**
     * Lo configura con parametros ya codificados, diciendo de donde sacar el azar.
     *
     * @param opmode que va a hacer
     * @param key con que clave
     * @param params los parametros
     * @param random de donde sacar lo que haya que sortear
     * @throws InvalidKeyException si la clave no sirve para este cifrado
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     * @throws IllegalArgumentException si el modo de operacion no es uno de los cuatro
     */
    public final void init(int opmode, Key key, AlgorithmParameters params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        comprobarModo(opmode);
        this.initialized = false;
        this.spi.engineInit(opmode, key, params, random);
        this.initialized = true;
    }

    /**
     * Lo configura con la clave publica de un certificado.
     *
     * @param opmode que va a hacer
     * @param certificate el certificado
     * @throws InvalidKeyException si la clave del certificado no sirve para este cifrado
     * @throws IllegalArgumentException si el modo de operacion no es uno de los cuatro
     */
    public final void init(int opmode, Certificate certificate) throws InvalidKeyException {
        init(opmode, certificate, new SecureRandom());
    }

    /**
     * Lo configura con la clave publica de un certificado, diciendo de donde sacar el azar.
     *
     * @param opmode que va a hacer
     * @param certificate el certificado
     * @param random de donde sacar lo que haya que sortear
     * @throws InvalidKeyException si la clave del certificado no sirve para este cifrado
     * @throws IllegalArgumentException si el modo de operacion no es uno de los cuatro
     */
    public final void init(int opmode, Certificate certificate, SecureRandom random)
            throws InvalidKeyException {
        if (certificate == null) {
            throw new NullPointerException("certificate");
        }
        final PublicKey pk = certificate.getPublicKey();
        init(opmode, pk, random);
    }

    /**
     * Entrega datos y devuelve lo que salga.
     *
     * @param input los datos
     * @return lo que salio, o {@code null} si no salio nada
     * @throws IllegalStateException si no se lo configuro
     * @throws IllegalArgumentException si los datos son {@code null}
     */
    public final byte[] update(byte[] input) {
        comprobarConfigurado();
        if (input == null) {
            throw new IllegalArgumentException("Null input buffer");
        }
        return this.spi.engineUpdate(input, 0, input.length);
    }

    /**
     * Entrega parte de un arreglo y devuelve lo que salga.
     *
     * @param input los datos
     * @param inputOffset desde donde
     * @param inputLen cuantos
     * @return lo que salio, o {@code null} si no salio nada
     * @throws IllegalStateException si no se lo configuro
     * @throws IllegalArgumentException si la porcion no esta bien
     */
    public final byte[] update(byte[] input, int inputOffset, int inputLen) {
        comprobarConfigurado();
        comprobarPorcion(input, inputOffset, inputLen);
        return this.spi.engineUpdate(input, inputOffset, inputLen);
    }

    /**
     * Entrega datos y escribe lo que salga en el arreglo dado.
     *
     * @param input los datos
     * @param inputOffset desde donde
     * @param inputLen cuantos
     * @param output donde escribir
     * @return cuantos bytes se escribieron
     * @throws IllegalStateException si no se lo configuro
     * @throws ShortBufferException si el arreglo de salida no alcanza
     */
    public final int update(byte[] input, int inputOffset, int inputLen, byte[] output)
            throws ShortBufferException {
        return update(input, inputOffset, inputLen, output, 0);
    }

    /**
     * Entrega datos y escribe lo que salga en el arreglo dado, desde esa posicion.
     *
     * @param input los datos
     * @param inputOffset desde donde
     * @param inputLen cuantos
     * @param output donde escribir
     * @param outputOffset desde donde escribir
     * @return cuantos bytes se escribieron
     * @throws IllegalStateException si no se lo configuro
     * @throws ShortBufferException si el arreglo de salida no alcanza
     */
    public final int update(byte[] input, int inputOffset, int inputLen, byte[] output,
            int outputOffset) throws ShortBufferException {
        comprobarConfigurado();
        comprobarPorcion(input, inputOffset, inputLen);
        if (output == null || outputOffset < 0) {
            throw new IllegalArgumentException("Bad arguments");
        }
        return this.spi.engineUpdate(input, inputOffset, inputLen, output, outputOffset);
    }

    /**
     * Lo mismo, con buffers.
     *
     * @param input de donde leer; queda consumido
     * @param output donde escribir
     * @return cuantos bytes se escribieron
     * @throws IllegalStateException si no se lo configuro
     * @throws ShortBufferException si en el buffer de salida no entra
     */
    public final int update(ByteBuffer input, ByteBuffer output) throws ShortBufferException {
        comprobarConfigurado();
        return this.spi.engineUpdate(input, output);
    }

    /**
     * Termina sin entregar nada mas.
     *
     * @return lo que quedaba, o {@code null} si no quedaba nada
     * @throws IllegalStateException si no se lo configuro
     * @throws IllegalBlockSizeException si lo entregado no mide un multiplo del bloque
     * @throws BadPaddingException si el relleno no cierra
     */
    public final byte[] doFinal() throws IllegalBlockSizeException, BadPaddingException {
        comprobarConfigurado();
        return this.spi.engineDoFinal(null, 0, 0);
    }

    /**
     * Termina y escribe lo que quedaba en el arreglo dado.
     *
     * @param output donde escribir
     * @param outputOffset desde donde escribir
     * @return cuantos bytes se escribieron
     * @throws IllegalStateException si no se lo configuro
     * @throws IllegalBlockSizeException si lo entregado no mide un multiplo del bloque
     * @throws ShortBufferException si el arreglo de salida no alcanza
     * @throws BadPaddingException si el relleno no cierra
     */
    public final int doFinal(byte[] output, int outputOffset)
            throws IllegalBlockSizeException, ShortBufferException, BadPaddingException {
        comprobarConfigurado();
        if (output == null || outputOffset < 0) {
            throw new IllegalArgumentException("Bad arguments");
        }
        return this.spi.engineDoFinal(null, 0, 0, output, outputOffset);
    }

    /**
     * Entrega los ultimos datos y termina.
     *
     * @param input los datos
     * @return lo que salio
     * @throws IllegalStateException si no se lo configuro
     * @throws IllegalBlockSizeException si lo entregado no mide un multiplo del bloque
     * @throws BadPaddingException si el relleno no cierra
     */
    public final byte[] doFinal(byte[] input)
            throws IllegalBlockSizeException, BadPaddingException {
        comprobarConfigurado();
        if (input == null) {
            throw new IllegalArgumentException("Null input buffer");
        }
        return this.spi.engineDoFinal(input, 0, input.length);
    }

    /**
     * Entrega parte de un arreglo y termina.
     *
     * @param input los datos
     * @param inputOffset desde donde
     * @param inputLen cuantos
     * @return lo que salio
     * @throws IllegalStateException si no se lo configuro
     * @throws IllegalBlockSizeException si lo entregado no mide un multiplo del bloque
     * @throws BadPaddingException si el relleno no cierra
     */
    public final byte[] doFinal(byte[] input, int inputOffset, int inputLen)
            throws IllegalBlockSizeException, BadPaddingException {
        comprobarConfigurado();
        comprobarPorcion(input, inputOffset, inputLen);
        return this.spi.engineDoFinal(input, inputOffset, inputLen);
    }

    /**
     * Entrega los ultimos datos, termina, y escribe en el arreglo dado.
     *
     * @param input los datos
     * @param inputOffset desde donde
     * @param inputLen cuantos
     * @param output donde escribir
     * @return cuantos bytes se escribieron
     * @throws IllegalStateException si no se lo configuro
     * @throws ShortBufferException si el arreglo de salida no alcanza
     * @throws IllegalBlockSizeException si lo entregado no mide un multiplo del bloque
     * @throws BadPaddingException si el relleno no cierra
     */
    public final int doFinal(byte[] input, int inputOffset, int inputLen, byte[] output)
            throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        return doFinal(input, inputOffset, inputLen, output, 0);
    }

    /**
     * Lo mismo, escribiendo desde esa posicion.
     *
     * @param input los datos
     * @param inputOffset desde donde
     * @param inputLen cuantos
     * @param output donde escribir
     * @param outputOffset desde donde escribir
     * @return cuantos bytes se escribieron
     * @throws IllegalStateException si no se lo configuro
     * @throws ShortBufferException si el arreglo de salida no alcanza
     * @throws IllegalBlockSizeException si lo entregado no mide un multiplo del bloque
     * @throws BadPaddingException si el relleno no cierra
     */
    public final int doFinal(byte[] input, int inputOffset, int inputLen, byte[] output,
            int outputOffset)
            throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        comprobarConfigurado();
        comprobarPorcion(input, inputOffset, inputLen);
        if (output == null || outputOffset < 0) {
            throw new IllegalArgumentException("Bad arguments");
        }
        return this.spi.engineDoFinal(input, inputOffset, inputLen, output, outputOffset);
    }

    /**
     * Lo mismo, con buffers.
     *
     * @param input de donde leer; queda consumido
     * @param output donde escribir
     * @return cuantos bytes se escribieron
     * @throws IllegalStateException si no se lo configuro
     * @throws ShortBufferException si en el buffer de salida no entra
     * @throws IllegalBlockSizeException si lo entregado no mide un multiplo del bloque
     * @throws BadPaddingException si el relleno no cierra
     */
    public final int doFinal(ByteBuffer input, ByteBuffer output)
            throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        comprobarConfigurado();
        return this.spi.engineDoFinal(input, output);
    }

    /**
     * Envuelve una clave.
     *
     * @param key la clave a envolver
     * @return la clave cifrada
     * @throws IllegalStateException si no se lo configuro
     * @throws IllegalBlockSizeException si la clave codificada no mide un multiplo del bloque
     * @throws InvalidKeyException si la clave no se puede codificar
     */
    public final byte[] wrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        comprobarConfigurado();
        return this.spi.engineWrap(key);
    }

    /**
     * Desenvuelve una clave.
     *
     * @param wrappedKey la clave cifrada
     * @param wrappedKeyAlgorithm para que algoritmo es la clave que sale
     * @param wrappedKeyType si es publica, privada o secreta
     * @return la clave
     * @throws IllegalStateException si no se lo configuro
     * @throws InvalidKeyException si lo descifrado no es una clave de ese tipo
     * @throws NoSuchAlgorithmException si no hay con que reconstruirla
     */
    public final Key unwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType)
            throws InvalidKeyException, NoSuchAlgorithmException {
        comprobarConfigurado();
        if (wrappedKeyType != PUBLIC_KEY && wrappedKeyType != PRIVATE_KEY
                && wrappedKeyType != SECRET_KEY) {
            throw new InvalidParameterException("Invalid key type");
        }
        return this.spi.engineUnwrap(wrappedKey, wrappedKeyAlgorithm, wrappedKeyType);
    }

    /**
     * Cual es la clave mas larga que se puede usar con ese algoritmo.
     *
     * @param transformation el algoritmo
     * @return {@link Integer#MAX_VALUE}: no hay archivo de politica que limite nada
     * @throws NoSuchAlgorithmException si el nombre esta mal formado
     * @throws NullPointerException si el nombre es {@code null}
     */
    public static final int getMaxAllowedKeyLength(String transformation)
            throws NoSuchAlgorithmException {
        if (transformation == null) {
            throw new NullPointerException("null transformation");
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Que parametros son los mas fuertes que se pueden usar con ese algoritmo.
     *
     * @param transformation el algoritmo
     * @return {@code null}: no hay archivo de politica que limite nada
     * @throws NoSuchAlgorithmException si el nombre esta mal formado
     * @throws NullPointerException si el nombre es {@code null}
     */
    public static final AlgorithmParameterSpec getMaxAllowedParameterSpec(String transformation)
            throws NoSuchAlgorithmException {
        if (transformation == null) {
            throw new NullPointerException("null transformation");
        }
        return null;
    }

    /**
     * Entrega datos que van autenticados pero no cifrados.
     *
     * @param src los datos
     * @throws IllegalStateException si no se lo configuro
     * @throws IllegalArgumentException si los datos son {@code null}
     * @throws UnsupportedOperationException si este cifrado no es autenticado
     */
    public final void updateAAD(byte[] src) {
        comprobarConfigurado();
        if (src == null) {
            throw new IllegalArgumentException("src buffer is null");
        }
        this.spi.engineUpdateAAD(src, 0, src.length);
    }

    /**
     * Lo mismo, con parte de un arreglo.
     *
     * @param src los datos
     * @param offset desde donde
     * @param len cuantos
     * @throws IllegalStateException si no se lo configuro
     * @throws IllegalArgumentException si la porcion no esta bien
     * @throws UnsupportedOperationException si este cifrado no es autenticado
     */
    public final void updateAAD(byte[] src, int offset, int len) {
        comprobarConfigurado();
        comprobarPorcion(src, offset, len);
        this.spi.engineUpdateAAD(src, offset, len);
    }

    /**
     * Lo mismo, con un buffer.
     *
     * @param src los datos; queda consumido
     * @throws IllegalStateException si no se lo configuro
     * @throws IllegalArgumentException si el buffer es {@code null}
     * @throws UnsupportedOperationException si este cifrado no es autenticado
     */
    public final void updateAAD(ByteBuffer src) {
        comprobarConfigurado();
        if (src == null) {
            throw new IllegalArgumentException("src buffer is null");
        }
        this.spi.engineUpdateAAD(src);
    }

    /**
     * Para leer al depurar.
     *
     * @return la transformacion, si esta configurado, y de que proveedor salio
     */
    @Override
    public String toString() {
        return "Cipher." + this.transformation + ", mode: "
                + (this.initialized ? "initialized" : "not initialized")
                + ", algorithm from: "
                + (this.provider == null ? "(no provider)" : this.provider.getName());
    }

    /**
     * Parte el nombre en algoritmo, modo y relleno.
     *
     * <p>Una sola parte o tres; dos o cuatro es un nombre mal formado. Se contesta con
     * {@link NoSuchAlgorithmException} y no con {@link IllegalArgumentException} porque desde afuera
     * es lo mismo: se pidio algo que no existe.
     */
    private static String[] partir(String transformation) throws NoSuchAlgorithmException {
        if (transformation == null || transformation.isEmpty()) {
            throw new NoSuchAlgorithmException("No transformation given");
        }
        final String[] partes = transformation.split("/", -1);
        if (partes.length != 1 && partes.length != 3) {
            throw new NoSuchAlgorithmException(
                    "Invalid transformation format: " + transformation);
        }
        if (partes.length == 1) {
            return new String[] {partes[0].trim(), null, null};
        }
        return new String[] {partes[0].trim(), partes[1].trim(), partes[2].trim()};
    }

    /**
     * Busca en ese proveedor y arma el cifrador, o devuelve {@code null} si no lo tiene.
     *
     * <p>Se prueban cuatro nombres, de mas especifico a menos: un proveedor puede registrar el
     * servicio con la transformacion entera --porque tiene una implementacion afinada para esa
     * combinacion-- o solo con el algoritmo, y dejar que el modo y el relleno se le pidan aparte.
     */
    private static Cipher armar(Provider p, String[] partes, String transformation)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        final String alg = partes[0];
        final String modo = partes[1];
        final String relleno = partes[2];
        Provider.Service s = null;
        boolean ponerModo = false;
        boolean ponerRelleno = false;
        if (modo != null) {
            s = p.getService("Cipher", alg + "/" + modo + "/" + relleno);
            if (s == null) {
                s = p.getService("Cipher", alg + "/" + modo);
                ponerRelleno = s != null;
            }
            if (s == null) {
                s = p.getService("Cipher", alg + "//" + relleno);
                ponerModo = s != null;
            }
        }
        if (s == null) {
            s = p.getService("Cipher", alg);
            ponerModo = modo != null;
            ponerRelleno = relleno != null;
        }
        if (s == null) {
            return null;
        }
        final Object o = s.newInstance(null);
        if (!(o instanceof CipherSpi)) {
            throw new NoSuchAlgorithmException(
                    "class configured for Cipher is not a CipherSpi: " + s.getClassName());
        }
        final CipherSpi spi = (CipherSpi) o;
        if (ponerModo) {
            spi.engineSetMode(modo);
        }
        if (ponerRelleno) {
            spi.engineSetPadding(relleno);
        }
        return new Cipher(spi, s.getProvider(), transformation);
    }

    private void comprobarConfigurado() {
        if (!this.initialized && !this.sinConfigurar) {
            throw new IllegalStateException("Cipher not initialized");
        }
    }

    private static void comprobarModo(int opmode) {
        if (opmode != ENCRYPT_MODE && opmode != DECRYPT_MODE && opmode != WRAP_MODE
                && opmode != UNWRAP_MODE) {
            throw new IllegalArgumentException("Invalid operation mode: " + opmode);
        }
    }

    private static void comprobarPorcion(byte[] input, int offset, int len) {
        if (input == null || offset < 0 || len < 0 || len > input.length - offset) {
            throw new IllegalArgumentException("Bad arguments");
        }
    }
}
