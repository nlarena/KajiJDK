package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Lo que un proveedor tiene que escribir para ofrecer un acuerdo de claves.
 *
 * <h2>Que resuelve</h2>
 *
 * <p>Dos partes que nunca hablaron terminan con la misma clave secreta, sin que esa clave haya
 * viajado. Cada una manda su parte publica y combina la que recibe con su parte privada; la
 * aritmetica hace que las dos combinaciones den lo mismo, y que a quien mira pasar los dos mensajes
 * publicos no le alcance para calcularlo.
 *
 * <h2>Las fases</h2>
 *
 * <p>{@link #engineDoPhase} se llama una vez por cada participante que no sea uno mismo, y la
 * ultima se marca con {@code lastPhase}. Son varias porque el acuerdo se generaliza a mas de dos
 * partes; con dos, que es el caso normal, hay una sola fase y es la ultima.
 *
 * @since 1.4
 */
public abstract class KeyAgreementSpi {

    /** Uno. */
    public KeyAgreementSpi() {
    }

    /**
     * Lo configura con la parte privada propia.
     *
     * @param key la clave privada
     * @param random de donde sacar el azar
     * @throws InvalidKeyException si la clave no sirve
     */
    protected abstract void engineInit(Key key, SecureRandom random) throws InvalidKeyException;

    /**
     * Lo configura con parametros.
     *
     * @param key la clave privada
     * @param params los parametros
     * @param random de donde sacar el azar
     * @throws InvalidKeyException si la clave no sirve
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     */
    protected abstract void engineInit(Key key, AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException;

    /**
     * Combina la parte publica de otro participante.
     *
     * @param key la clave publica del otro
     * @param lastPhase si es el ultimo participante
     * @return la clave intermedia, o {@code null} si no hay
     * @throws InvalidKeyException si la clave no sirve
     * @throws IllegalStateException si no se lo configuro
     */
    protected abstract Key engineDoPhase(Key key, boolean lastPhase)
            throws InvalidKeyException, IllegalStateException;

    /**
     * El secreto acordado.
     *
     * @return el secreto
     * @throws IllegalStateException si faltan fases
     */
    protected abstract byte[] engineGenerateSecret() throws IllegalStateException;

    /**
     * El secreto acordado, escrito en el arreglo dado.
     *
     * @param sharedSecret donde escribirlo
     * @param offset desde donde
     * @return cuantos bytes se escribieron
     * @throws IllegalStateException si faltan fases
     * @throws ShortBufferException si el arreglo no alcanza
     */
    protected abstract int engineGenerateSecret(byte[] sharedSecret, int offset)
            throws IllegalStateException, ShortBufferException;

    /**
     * El secreto acordado, ya convertido en clave de ese algoritmo.
     *
     * <p>No es lo mismo que tomar los bytes crudos: el secreto de un acuerdo tiene una distribucion
     * que no es uniforme, y usarlo directo como clave es un error conocido. Esto lo pasa por donde
     * corresponda antes de entregarlo.
     *
     * @param algorithm para que algoritmo
     * @return la clave
     * @throws IllegalStateException si faltan fases
     * @throws NoSuchAlgorithmException si no hay como armar una clave de ese algoritmo
     * @throws InvalidKeyException si el secreto no da para una clave de ese algoritmo
     */
    protected abstract SecretKey engineGenerateSecret(String algorithm)
            throws IllegalStateException, NoSuchAlgorithmException, InvalidKeyException;
}
