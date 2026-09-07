package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * The salt and the iteration count of a password-based encryption.
 *
 * <p>The two values are what makes a dictionary attack slow: the salt prevents precomputed tables and
 * the iterations make each attempt expensive. This class does not validate them --neither the salt's
 * length nor a minimum number of iterations-- because the reasonable value depends on the algorithm
 * and on the year, and putting a floor on it now would be a number that ages badly.
 */
public class PBEParameterSpec implements AlgorithmParameterSpec {

    private final byte[] salt;
    private final int iterationCount;
    private final AlgorithmParameterSpec paramSpec;

    /**
     * @throws NullPointerException if the salt is null
     */
    public PBEParameterSpec(byte[] salt, int iterationCount) {
        this(salt, iterationCount, null);
    }

    /**
     * With parameters for the underlying cipher --an AES's IV, for instance--.
     *
     * @throws NullPointerException if the salt is null
     */
    public PBEParameterSpec(byte[] salt, int iterationCount, AlgorithmParameterSpec paramSpec) {
        if (salt == null) {
            throw new NullPointerException("the salt cannot be null");
        }
        this.salt = IvParameterSpec.copy(salt, 0, salt.length);
        this.iterationCount = iterationCount;
        this.paramSpec = paramSpec;
    }

    /** A copy of the salt. */
    public byte[] getSalt() {
        return IvParameterSpec.copy(this.salt, 0, this.salt.length);
    }

    /** How many iterations. */
    public int getIterationCount() {
        return this.iterationCount;
    }

    /** The underlying cipher's parameters, or null if there are none. */
    public AlgorithmParameterSpec getParameterSpec() {
        return this.paramSpec;
    }
}
