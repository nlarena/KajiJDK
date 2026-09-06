package javax.crypto;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Lo que un proveedor tiene que escribir para ofrecer un mecanismo de exencion.
 *
 * <h2>De donde sale esto</h2>
 *
 * <p>De cuando exportar criptografia fuerte estaba restringido. Un producto podia usar claves mas
 * largas de lo permitido si ademas guardaba, junto al mensaje, un bloque que le permitiera a una
 * autoridad recuperarlo. Eso es el bloque de exencion: deposito de claves, recuperacion de claves, o
 * debilitamiento deliberado.
 *
 * <p>Hoy no se usa. Las restricciones se levantaron y el JDK no trae ningun mecanismo; la maquinaria
 * quedo porque sacarla romperia programas que la nombran.
 *
 * @since 1.4
 */
public abstract class ExemptionMechanismSpi {

    /** Uno. */
    public ExemptionMechanismSpi() {
    }

    /**
     * Cuanto va a medir el bloque.
     *
     * @param inputLen cuanto mide la entrada
     * @return el tamano en bytes
     */
    protected abstract int engineGetOutputSize(int inputLen);

    /**
     * Lo configura.
     *
     * @param key la clave
     * @throws InvalidKeyException si la clave no sirve
     * @throws ExemptionMechanismException si algo mas sale mal
     */
    protected abstract void engineInit(Key key)
            throws InvalidKeyException, ExemptionMechanismException;

    /**
     * Lo configura con parametros.
     *
     * @param key la clave
     * @param params los parametros
     * @throws InvalidKeyException si la clave no sirve
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     * @throws ExemptionMechanismException si algo mas sale mal
     */
    protected abstract void engineInit(Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException,
            ExemptionMechanismException;

    /**
     * Lo configura con parametros ya codificados.
     *
     * @param key la clave
     * @param params los parametros
     * @throws InvalidKeyException si la clave no sirve
     * @throws InvalidAlgorithmParameterException si los parametros no sirven
     * @throws ExemptionMechanismException si algo mas sale mal
     */
    protected abstract void engineInit(Key key, AlgorithmParameters params)
            throws InvalidKeyException, InvalidAlgorithmParameterException,
            ExemptionMechanismException;

    /**
     * Genera el bloque.
     *
     * @return el bloque
     * @throws ExemptionMechanismException si algo sale mal
     */
    protected abstract byte[] engineGenExemptionBlob() throws ExemptionMechanismException;

    /**
     * Genera el bloque en el arreglo dado.
     *
     * @param output donde escribirlo
     * @param outputOffset desde donde
     * @return cuantos bytes se escribieron
     * @throws ShortBufferException si el arreglo no alcanza
     * @throws ExemptionMechanismException si algo sale mal
     */
    protected abstract int engineGenExemptionBlob(byte[] output, int outputOffset)
            throws ShortBufferException, ExemptionMechanismException;
}
