package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

/**
 * What a provider has to write in order to offer a key agreement.
 *
 * <h2>What it solves</h2>
 *
 * <p>Two parties that never spoke end up with the same secret key, without that key ever having
 * travelled. Each sends its public part and combines the one it receives with its own private part;
 * the arithmetic makes both combinations come out the same, and makes watching the two public
 * messages go by not enough to work it out.
 *
 * <h2>The phases</h2>
 *
 * <p>{@link #engineDoPhase} is called once per participant other than oneself, and the last one is
 * marked with {@code lastPhase}. There are several because the agreement generalizes to more than
 * two parties; with two, which is the normal case, there is a single phase and it is the last.
 *
 * @since 1.4
 */
public abstract class KeyAgreementSpi {

    /** One. */
    public KeyAgreementSpi() {
    }

    /**
     * Configures it with one's own private part.
     *
     * @param key the private key
     * @param random where to take the randomness from
     * @throws InvalidKeyException if the key is no good
     */
    protected abstract void engineInit(Key key, SecureRandom random) throws InvalidKeyException;

    /**
     * Configures it with parameters.
     *
     * @param key the private key
     * @param params the parameters
     * @param random where to take the randomness from
     * @throws InvalidKeyException if the key is no good
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     */
    protected abstract void engineInit(Key key, AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException;

    /**
     * Combines another participant's public part.
     *
     * @param key the other one's public key
     * @param lastPhase whether it is the last participant
     * @return the intermediate key, or {@code null} if there is none
     * @throws InvalidKeyException if the key is no good
     * @throws IllegalStateException if it has not been configured
     */
    protected abstract Key engineDoPhase(Key key, boolean lastPhase)
            throws InvalidKeyException, IllegalStateException;

    /**
     * The agreed secret.
     *
     * @return the secret
     * @throws IllegalStateException if phases are missing
     */
    protected abstract byte[] engineGenerateSecret() throws IllegalStateException;

    /**
     * The agreed secret, written into the given array.
     *
     * @param sharedSecret where to write it
     * @param offset from where
     * @return how many bytes were written
     * @throws IllegalStateException if phases are missing
     * @throws ShortBufferException if the array is not big enough
     */
    protected abstract int engineGenerateSecret(byte[] sharedSecret, int offset)
            throws IllegalStateException, ShortBufferException;

    /**
     * The agreed secret, already turned into a key of that algorithm.
     *
     * <p>It is not the same as taking the raw bytes: the secret of an agreement has a distribution
     * that is not uniform, and using it straight as a key is a known mistake. This puts it through
     * whatever it has to go through before handing it over.
     *
     * @param algorithm for which algorithm
     * @return the key
     * @throws IllegalStateException if phases are missing
     * @throws NoSuchAlgorithmException if there is no way to assemble a key of that algorithm
     * @throws InvalidKeyException if the secret is not enough for a key of that algorithm
     */
    protected abstract SecretKey engineGenerateSecret(String algorithm)
            throws IllegalStateException, NoSuchAlgorithmException, InvalidKeyException;
}
