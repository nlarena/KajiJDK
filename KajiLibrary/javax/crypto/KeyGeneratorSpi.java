package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

/**
 * What a provider has to write in order to offer a symmetric key generator.
 *
 * <h2>Why random bytes are not enough</h2>
 *
 * <p>For many algorithms they would be, and there the generator is little more than a wrapper around
 * the random generator. For others they are not: DES and DESede have parity bits and weak keys that
 * have to be discarded, and a key for an algorithm with structure --curve keys, for instance-- has
 * to fall in a range. Drawing bytes and calling them a key would produce invalid keys now and then.
 *
 * <h2>The three ways of configuring it</h2>
 *
 * <p>By size, by parameters, or by nothing. The last is not an oversight: nearly every algorithm has
 * a recommended size, and choosing it by default is better than forcing every program to know which
 * it is --which is how 512-bit keys end up being written in 2026.
 *
 * @since 1.4
 */
public abstract class KeyGeneratorSpi {

    /** One. */
    public KeyGeneratorSpi() {
    }

    /**
     * Configures it with the default size.
     *
     * @param random where to take the randomness from
     */
    protected abstract void engineInit(SecureRandom random);

    /**
     * Configures it with parameters.
     *
     * @param params the parameters
     * @param random where to take the randomness from
     * @throws InvalidAlgorithmParameterException if the parameters are no good
     */
    protected abstract void engineInit(AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidAlgorithmParameterException;

    /**
     * Configures it with a size.
     *
     * @param keysize the size in bits
     * @param random where to take the randomness from
     * @throws InvalidParameterException if that size is no good
     */
    protected abstract void engineInit(int keysize, SecureRandom random);

    /**
     * Generates a key.
     *
     * @return the key
     */
    protected abstract SecretKey engineGenerateKey();
}
