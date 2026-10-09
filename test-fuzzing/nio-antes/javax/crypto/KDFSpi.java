package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

/**
 * What a provider has to write in order to offer a key derivation function.
 *
 * <h2>What deriving is for</h2>
 *
 * <p>Because what one has is nearly never usable as a key as it is. The secret that comes out of an
 * agreement is not uniformly distributed; a password has very little entropy; and from a single
 * secret several different keys are usually needed --one per direction, another for the MAC.
 * Deriving is what turns one thing into the other without the derived keys being relatable to each
 * other.
 *
 * <h2>Why the constructor takes parameters</h2>
 *
 * <p>Because the function's are fixed once and hold for every derivation; each derivation's go in
 * {@link #engineDeriveKey}. Separating them is what allows the function to be built once and used
 * many times.
 *
 * @since 24
 */
public abstract class KDFSpi {

    /**
     * One with those parameters.
     *
     * @param kdfParameters the function's parameters, or {@code null}
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     */
    protected KDFSpi(KDFParameters kdfParameters) throws InvalidAlgorithmParameterException {
    }

    /**
     * The parameters it was built with.
     *
     * @return the parameters, or {@code null}
     */
    protected abstract KDFParameters engineGetParameters();

    /**
     * Derives a key.
     *
     * @param alg which algorithm the key is for
     * @param kdfParameterSpec this derivation's parameters
     * @return the key
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     * @throws NoSuchAlgorithmException if there is no way to assemble a key of that algorithm
     */
    protected abstract SecretKey engineDeriveKey(String alg,
            AlgorithmParameterSpec kdfParameterSpec)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException;

    /**
     * Derives bytes.
     *
     * @param kdfParameterSpec this derivation's parameters
     * @return the bytes
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     */
    protected abstract byte[] engineDeriveData(AlgorithmParameterSpec kdfParameterSpec)
            throws InvalidAlgorithmParameterException;
}
