package java.security.interfaces;

import java.security.InvalidParameterException;
import java.security.SecureRandom;

/**
 * KajiLibrary's java.security.interfaces.DSAKeyPairGenerator -- the extra configuration a DSA key
 * generator accepts.
 *
 * <p>It is implemented by a {@link java.security.KeyPairGenerator} that generates DSA, and it
 * exists because DSA has a configuration that does not fit in "how many bits": the <b>domain
 * parameters</b> p, q and g, which are shared among many keys and are expensive to generate.
 *
 * <h2>Why there are two ways to initialize</h2>
 *
 * <p>Generating domain parameters means searching for large primes with a relation between them,
 * and that takes seconds or minutes. That is why normal practice is to <b>reuse</b> them: an
 * organization generates one set and all its keys share it. The two ways cover the two cases:
 *
 * <ul>
 *   <li>{@link #initialize(DSAParams, SecureRandom)} -- I already have them, use these.
 *   <li>{@link #initialize(int, boolean, SecureRandom)} -- generate new ones of that size, or take
 *       them from the precomputed ones the provider ships.
 * </ul>
 *
 * <p>Sharing domain parameters does <b>not</b> weaken the keys: p, q and g are public and are in
 * the certificate. What is never shared is the private key x, which comes from randomness.
 *
 * <h2>The source of randomness</h2>
 *
 * <p>Both methods take it, and in both it is used for the same thing as in any key generator:
 * choosing the private key. This note said it is not optional, and that the API does not allow
 * generating without saying where the randomness comes from; the JDK documents {@code random} as
 * "can be null" in both methods.
 *
 * <p><b>This library ships no DSA generator</b>: the interface is here so that a provider that
 * writes one fits in, just as {@code X509Certificate} is here without any certificate parser.
 */
public interface DSAKeyPairGenerator {

    /**
     * Uses these domain parameters.
     *
     * @param params the p, q and g already computed
     * @param random where the private key comes from; the JDK allows {@code null}
     * @throws InvalidParameterException if the generator does not accept those parameters
     */
    void initialize(DSAParams params, SecureRandom random) throws InvalidParameterException;

    /**
     * Generates or takes domain parameters of that size.
     *
     * @param modlen    the bits of p
     * @param genParams whether to <b>generate</b> new parameters, or take the precomputed ones the
     *     provider ships. Generating is expensive; taking them is instant and no less secure,
     *     because the parameters are public. A provider that has no precomputed ones for that size
     *     and receives false has to refuse instead of generating anyway: the caller asked not to
     *     wait
     * @param random    where the private key comes from; the JDK allows {@code null}
     * @throws InvalidParameterException if the size does not suit it, or if precomputed parameters
     *     it does not have were requested
     */
    void initialize(int modlen, boolean genParams, SecureRandom random)
        throws InvalidParameterException;
}
