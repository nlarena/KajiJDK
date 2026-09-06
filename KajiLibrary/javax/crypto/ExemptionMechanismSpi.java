package javax.crypto;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;

/**
 * What a provider has to write in order to offer an exemption mechanism.
 *
 * <h2>Where this comes from</h2>
 *
 * <p>From when exporting strong cryptography was restricted. A product could use keys longer than
 * allowed if it also kept, alongside the message, a block letting an authority recover it. That is
 * the exemption blob: key escrow, key recovery, or deliberate weakening.
 *
 * <p>Nobody uses it today. The restrictions were lifted and the JDK ships no mechanism; the
 * machinery stayed because removing it would break programs that name it.
 *
 * @since 1.4
 */
public abstract class ExemptionMechanismSpi {

    /** One. */
    public ExemptionMechanismSpi() {
    }

    /**
     * How large the blob will be.
     *
     * @param inputLen how large the input is
     * @return the size in bytes
     */
    protected abstract int engineGetOutputSize(int inputLen);

    /**
     * Configures it.
     *
     * @param key the key
     * @throws InvalidKeyException if the key is no good
     * @throws ExemptionMechanismException if anything else goes wrong
     */
    protected abstract void engineInit(Key key)
            throws InvalidKeyException, ExemptionMechanismException;

    /**
     * Configures it with parameters.
     *
     * @param key the key
     * @param params the parameters
     * @throws InvalidKeyException if the key is no good
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     * @throws ExemptionMechanismException if anything else goes wrong
     */
    protected abstract void engineInit(Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException,
            ExemptionMechanismException;

    /**
     * Configures it with already encoded parameters.
     *
     * @param key the key
     * @param params the parameters
     * @throws InvalidKeyException if the key is no good
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     * @throws ExemptionMechanismException if anything else goes wrong
     */
    protected abstract void engineInit(Key key, AlgorithmParameters params)
            throws InvalidKeyException, InvalidAlgorithmParameterException,
            ExemptionMechanismException;

    /**
     * Generates the blob.
     *
     * @return the blob
     * @throws ExemptionMechanismException if anything goes wrong
     */
    protected abstract byte[] engineGenExemptionBlob() throws ExemptionMechanismException;

    /**
     * Generates the blob into the given array.
     *
     * @param output where to write it
     * @param outputOffset from where
     * @return how many bytes were written
     * @throws ShortBufferException if the array is not big enough
     * @throws ExemptionMechanismException if anything goes wrong
     */
    protected abstract int engineGenExemptionBlob(byte[] output, int outputOffset)
            throws ShortBufferException, ExemptionMechanismException;
}
