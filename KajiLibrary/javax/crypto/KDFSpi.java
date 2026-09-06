package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Lo que un proveedor tiene que escribir para ofrecer una funcion de derivacion de claves.
 *
 * <h2>Para que se deriva</h2>
 *
 * <p>Porque lo que se tiene casi nunca sirve como clave tal cual. El secreto que sale de un acuerdo
 * no tiene distribucion uniforme; una contrasena tiene poquisima entropia; y de un solo secreto
 * suelen hacer falta varias claves distintas --una para cada direccion, otra para el MAC--. Derivar
 * es lo que convierte una cosa en la otra sin que las claves derivadas se puedan relacionar entre
 * si.
 *
 * <h2>Por que el constructor lleva parametros</h2>
 *
 * <p>Porque los de la funcion se fijan una vez y valen para todas las derivaciones; los de cada
 * derivacion van en {@link #engineDeriveKey}. Separarlos es lo que permite armar la funcion una vez
 * y usarla muchas.
 *
 * @since 24
 */
public abstract class KDFSpi {

    /**
     * Uno con esos parametros.
     *
     * @param kdfParameters los parametros de la funcion, o {@code null}
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     */
    protected KDFSpi(KDFParameters kdfParameters) throws InvalidAlgorithmParameterException {
    }

    /**
     * Los parametros con que se lo armo.
     *
     * @return los parametros, o {@code null}
     */
    protected abstract KDFParameters engineGetParameters();

    /**
     * Deriva una clave.
     *
     * @param alg para que algoritmo es la clave
     * @param kdfParameterSpec los parametros de esta derivacion
     * @return la clave
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     * @throws NoSuchAlgorithmException si no hay como armar una clave de ese algoritmo
     */
    protected abstract SecretKey engineDeriveKey(String alg,
            AlgorithmParameterSpec kdfParameterSpec)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException;

    /**
     * Deriva bytes.
     *
     * @param kdfParameterSpec los parametros de esta derivacion
     * @return los bytes
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     */
    protected abstract byte[] engineDeriveData(AlgorithmParameterSpec kdfParameterSpec)
            throws InvalidAlgorithmParameterException;
}
