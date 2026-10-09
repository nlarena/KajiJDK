package javax.crypto.spec;

import java.math.BigInteger;
import java.security.spec.KeySpec;

/**
 * A Diffie-Hellman public key: the value `y` plus the parameters `p` and `g` it was computed with.
 *
 * <p>The parameters go inside the key and not separately because a DH key **means nothing without
 * them**: the same `y` with another prime is another key. It is the difference from RSA, where the
 * modulus already comes in the key.
 */
public class DHPublicKeySpec implements KeySpec {

    private final BigInteger y;
    private final BigInteger p;
    private final BigInteger g;

    /** The public value `y`, with its prime and its generator. */
    public DHPublicKeySpec(BigInteger y, BigInteger p, BigInteger g) {
        this.y = y;
        this.p = p;
        this.g = g;
    }

    /** The public value. */
    public BigInteger getY() {
        return this.y;
    }

    /** The prime. */
    public BigInteger getP() {
        return this.p;
    }

    /** The generator. */
    public BigInteger getG() {
        return this.g;
    }
}
