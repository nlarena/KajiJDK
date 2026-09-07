package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * What is needed to **generate** Diffie-Hellman parameters: how many bits the prime and how many the
 * exponent.
 *
 * <p>It is {@link DHParameterSpec}'s counterpart: this one describes parameters that do not exist
 * yet and that one the ones already computed. Hence this one being two integers and that one two
 * enormous numbers.
 */
public class DHGenParameterSpec implements AlgorithmParameterSpec {

    private final int primeSize;
    private final int exponentSize;

    /** The prime of `primeSize` bits and the exponent of `exponentSize`. */
    public DHGenParameterSpec(int primeSize, int exponentSize) {
        this.primeSize = primeSize;
        this.exponentSize = exponentSize;
    }

    /** The prime's size, in bits. */
    public int getPrimeSize() {
        return this.primeSize;
    }

    /** The exponent's size, in bits. */
    public int getExponentSize() {
        return this.exponentSize;
    }
}
