package javax.crypto;

import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Lo que un proveedor tiene que escribir para ofrecer un cifrado.
 *
 * <h2>Por que separado de {@link Cipher}</h2>
 *
 * <p>Porque son dos audiencias distintas. {@link Cipher} es la cara que ve quien cifra: tiene
 * sobrecargas para cada comodidad --arreglos, porciones de arreglos, {@link ByteBuffer}-- y no le
 * pide nada a nadie. Esto es la cara que ve quien implementa el algoritmo, y esa quiere ser lo mas
 * chica posible. Cada sobrecarga que se le agregara aca seria trabajo repetido en cada proveedor.
 *
 * <h2>Cuales tienen cuerpo</h2>
 *
 * <p>Las de {@link ByteBuffer} y las de envolver claves. Las primeras porque se pueden escribir una
 * sola vez sacando los bytes del buffer y llamando a la version de arreglos --eso es lo que hace la
 * implementacion de aca--; las segundas porque no todo cifrado sabe envolver claves, y las que no
 * saben tiran {@link UnsupportedOperationException}.
 *
 * <h2>El modo y el relleno</h2>
 *
 * <p>{@link #engineSetMode} y {@link #engineSetPadding} se llaman una sola vez, al construir, y
 * salen de partir el nombre que se le paso a {@link Cipher#getInstance}. Un proveedor puede
 * rechazarlos: no todo algoritmo tiene modos, y un cifrado de flujo no tiene relleno.
 *
 * @since 1.4
 */
public abstract class CipherSpi {

    /** Uno. */
    public CipherSpi() {
    }

    /**
     * Fija el modo de operacion.
     *
     * @param mode el modo, como {@code "CBC"}
     * @throws NoSuchAlgorithmException si el proveedor no tiene ese modo
     */
    protected abstract void engineSetMode(String mode) throws NoSuchAlgorithmException;

    /**
     * Fija el relleno.
     *
     * @param padding el relleno, como {@code "PKCS5Padding"}
     * @throws NoSuchPaddingException si el proveedor no tiene ese relleno
     */
    protected abstract void engineSetPadding(String padding) throws NoSuchPaddingException;

    /**
     * Cuanto mide un bloque.
     *
     * @return el tamano en bytes, o cero si no es un cifrado por bloques
     */
    protected abstract int engineGetBlockSize();

    /**
     * Cuanto va a salir si ahora se entregan esos bytes y se termina.
     *
     * <p>Puede pasarse, nunca quedarse corto: sirve para reservar el arreglo de salida.
     *
     * @param inputLen cuantos bytes se van a entregar
     * @return el tamano en bytes
     */
    protected abstract int engineGetOutputSize(int inputLen);

    /**
     * El vector de inicializacion.
     *
     * @return una copia del vector, o {@code null} si no hay
     */
    protected abstract byte[] engineGetIV();

    /**
     * Los parametros con que quedo configurado.
     *
     * <p>Importa cuando el cifrador genero alguno solo --un vector de inicializacion al azar, por
     * ejemplo--: es la unica forma de que el que descifra sepa cual uso.
     *
     * @return los parametros, o {@code null} si no usa ninguno
     */
    protected abstract AlgorithmParameters engineGetParameters();

    /**
     * Lo configura.
     *
     * @param opmode que va a hacer
     * @param key con que clave
     * @param random de donde sacar lo que haya que sortear
     * @throws InvalidKeyException si la clave no sirve para este cifrado
     */
    protected abstract void engineInit(int opmode, Key key, SecureRandom random)
            throws InvalidKeyException;

    /**
     * Lo configura con parametros.
     *
     * @param opmode que va a hacer
     * @param key con que clave
     * @param params los parametros
     * @param random de donde sacar lo que haya que sortear
     * @throws InvalidKeyException si la clave no sirve para este cifrado
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     */
    protected abstract void engineInit(int opmode, Key key, AlgorithmParameterSpec params,
            SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException;

    /**
     * Lo configura con parametros ya codificados.
     *
     * @param opmode que va a hacer
     * @param key con que clave
     * @param params los parametros
     * @param random de donde sacar lo que haya que sortear
     * @throws InvalidKeyException si la clave no sirve para este cifrado
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     */
    protected abstract void engineInit(int opmode, Key key, AlgorithmParameters params,
            SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException;

    /**
     * Entrega datos y devuelve lo que salga.
     *
     * @param input los datos
     * @param inputOffset desde donde
     * @param inputLen cuantos
     * @return lo que salio, o {@code null} si no salio nada
     */
    protected abstract byte[] engineUpdate(byte[] input, int inputOffset, int inputLen);

    /**
     * Entrega datos y escribe lo que salga en el arreglo dado.
     *
     * @param input los datos
     * @param inputOffset desde donde
     * @param inputLen cuantos
     * @param output donde escribir
     * @param outputOffset desde donde escribir
     * @return cuantos bytes se escribieron
     * @throws ShortBufferException si el arreglo de salida no alcanza
     */
    protected abstract int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output,
            int outputOffset) throws ShortBufferException;

    /**
     * Lo mismo, con buffers.
     *
     * <p>La implementacion de aca saca los bytes del buffer de entrada, llama a la version de
     * arreglos y los pone en el de salida. Un proveedor que pueda trabajar sobre el buffer sin
     * copiar --uno que hable con la maquina directamente-- deberia redefinirlo.
     *
     * @param input de donde leer; queda consumido
     * @param output donde escribir
     * @return cuantos bytes se escribieron
     * @throws ShortBufferException si en el buffer de salida no entra
     * @throws NullPointerException si alguno de los dos es {@code null}
     * @throws IllegalArgumentException si son el mismo buffer
     * @throws java.nio.ReadOnlyBufferException si el de salida es de solo lectura
     */
    protected int engineUpdate(ByteBuffer input, ByteBuffer output) throws ShortBufferException {
        final byte[] entrada = sacar(input, output);
        return poner(output, engineUpdate(entrada, 0, entrada.length));
    }

    /**
     * Entrega los ultimos datos y termina.
     *
     * @param input los datos, o {@code null}
     * @param inputOffset desde donde
     * @param inputLen cuantos
     * @return lo que salio
     * @throws IllegalBlockSizeException si lo entregado no mide un multiplo del bloque
     * @throws BadPaddingException si el relleno no cierra
     */
    protected abstract byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen)
            throws IllegalBlockSizeException, BadPaddingException;

    /**
     * Entrega los ultimos datos, termina, y escribe en el arreglo dado.
     *
     * @param input los datos, o {@code null}
     * @param inputOffset desde donde
     * @param inputLen cuantos
     * @param output donde escribir
     * @param outputOffset desde donde escribir
     * @return cuantos bytes se escribieron
     * @throws ShortBufferException si el arreglo de salida no alcanza
     * @throws IllegalBlockSizeException si lo entregado no mide un multiplo del bloque
     * @throws BadPaddingException si el relleno no cierra
     */
    protected abstract int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output,
            int outputOffset)
            throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;

    /**
     * Lo mismo, con buffers.
     *
     * @param input de donde leer; queda consumido
     * @param output donde escribir
     * @return cuantos bytes se escribieron
     * @throws ShortBufferException si en el buffer de salida no entra
     * @throws IllegalBlockSizeException si lo entregado no mide un multiplo del bloque
     * @throws BadPaddingException si el relleno no cierra
     * @throws NullPointerException si alguno de los dos es {@code null}
     * @throws IllegalArgumentException si son el mismo buffer
     * @throws java.nio.ReadOnlyBufferException si el de salida es de solo lectura
     */
    protected int engineDoFinal(ByteBuffer input, ByteBuffer output)
            throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        final byte[] entrada = sacar(input, output);
        return poner(output, engineDoFinal(entrada, 0, entrada.length));
    }

    /**
     * Cifra una clave.
     *
     * <p>Envolver una clave no es lo mismo que cifrar sus bytes: la clave se codifica primero, y el
     * proveedor puede hacerlo sin que el material salga nunca a memoria --que es todo el punto
     * cuando la clave vive adentro de un dispositivo--.
     *
     * @param key la clave a envolver
     * @return la clave cifrada
     * @throws IllegalBlockSizeException si la clave codificada no mide un multiplo del bloque
     * @throws InvalidKeyException si la clave no se puede codificar
     * @throws UnsupportedOperationException si este cifrado no sabe envolver claves
     */
    protected byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        throw new UnsupportedOperationException();
    }

    /**
     * Descifra una clave.
     *
     * @param wrappedKey la clave cifrada
     * @param wrappedKeyAlgorithm para que algoritmo es la clave que sale
     * @param wrappedKeyType si es publica, privada o secreta
     * @return la clave
     * @throws InvalidKeyException si lo descifrado no es una clave de ese tipo
     * @throws NoSuchAlgorithmException si no hay con que reconstruirla
     * @throws UnsupportedOperationException si este cifrado no sabe envolver claves
     */
    protected Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType)
            throws InvalidKeyException, NoSuchAlgorithmException {
        throw new UnsupportedOperationException();
    }

    /**
     * Cuantos bits tiene esa clave.
     *
     * @param key la clave
     * @return el tamano en bits
     * @throws InvalidKeyException si la clave no sirve para este cifrado
     * @throws UnsupportedOperationException si este cifrado no sabe contestarlo
     */
    protected int engineGetKeySize(Key key) throws InvalidKeyException {
        throw new UnsupportedOperationException();
    }

    /**
     * Entrega datos que van autenticados pero no cifrados.
     *
     * <p>Solo tiene sentido en un cifrado autenticado. Sirve para lo que tiene que viajar a la vista
     * --una cabecera, un numero de secuencia-- pero igual protegido contra cambios.
     *
     * @param src los datos
     * @param offset desde donde
     * @param len cuantos
     * @throws UnsupportedOperationException si este cifrado no es autenticado
     */
    protected void engineUpdateAAD(byte[] src, int offset, int len) {
        throw new UnsupportedOperationException();
    }

    /**
     * Lo mismo, con un buffer.
     *
     * @param src los datos; queda consumido
     * @throws UnsupportedOperationException si este cifrado no es autenticado
     */
    protected void engineUpdateAAD(ByteBuffer src) {
        throw new UnsupportedOperationException();
    }

    /**
     * Comprueba los dos buffers y saca los bytes del de entrada, que queda consumido.
     *
     * <p>Se lo consume antes de saber si la salida va a entrar. Es lo que hace el JDK y no un
     * descuido: quien atrape una {@link ShortBufferException} tiene que volver a armar la entrada,
     * no reintentar con el mismo buffer.
     */
    private static byte[] sacar(ByteBuffer input, ByteBuffer output) {
        if (input == null || output == null) {
            throw new NullPointerException("los buffers no pueden ser nulos");
        }
        if (input == output) {
            throw new IllegalArgumentException("input and output buffers must not be the same");
        }
        if (output.isReadOnly()) {
            throw new java.nio.ReadOnlyBufferException();
        }
        final byte[] entrada = new byte[input.remaining()];
        input.get(entrada);
        return entrada;
    }

    /** Pone lo que salio en el buffer de salida. */
    private static int poner(ByteBuffer output, byte[] salida) throws ShortBufferException {
        if (salida == null || salida.length == 0) {
            return 0;
        }
        if (output.remaining() < salida.length) {
            throw new ShortBufferException(
                    "output buffer too small: " + output.remaining() + " < " + salida.length);
        }
        output.put(salida);
        return salida.length;
    }
}
