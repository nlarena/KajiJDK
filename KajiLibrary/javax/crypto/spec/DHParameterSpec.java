package javax.crypto.spec;

import java.math.BigInteger;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Diffie-Hellman's public parameters: the prime `p`, the generator `g` and, optionally, the length of
 * the private exponent.
 *
 * <p>`l` at zero means "no restriction", which is different from "zero bits": the two-argument
 * constructor leaves it that way. There is no way to ask for a zero-bit exponent, and there is none
 * because it would make no sense.
 *
 * <p>It does not validate that `p` is prime. Checking it is expensive --a probabilistic test on a
 * two-thousand-bit number-- and the JDK does not do it either: whoever generates the parameters is
 * responsible for that.
 */
public class DHParameterSpec implements AlgorithmParameterSpec {

    private final BigInteger p;
    private final BigInteger g;
    private final int l;

    /** With no restriction on the length of the private exponent. */
    public DHParameterSpec(BigInteger p, BigInteger g) {
        this.p = p;
        this.g = g;
        this.l = 0;
    }

    /** With the private exponent limited to `l` bits. */
    public DHParameterSpec(BigInteger p, BigInteger g, int l) {
        this.p = p;
        this.g = g;
        this.l = l;
    }

    /** The prime. */
    public BigInteger getP() {
        return this.p;
    }

    /** The generator. */
    public BigInteger getG() {
        return this.g;
    }

    /** The length of the private exponent in bits, or zero if there is no restriction. */
    public int getL() {
        return this.l;
    }
}
