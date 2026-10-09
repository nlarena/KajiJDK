package javax.crypto.spec;

import java.math.BigInteger;
import java.security.spec.KeySpec;

/**
 * A Diffie-Hellman private key: the exponent `x` plus the parameters it is used with.
 *
 * <p>The same note as {@link DHPublicKeySpec} applies about why the parameters travel inside.
 */
public class DHPrivateKeySpec implements KeySpec {

    private final BigInteger x;
    private final BigInteger p;
    private final BigInteger g;

    /** The private exponent `x`, with its prime and its generator. */
    public DHPrivateKeySpec(BigInteger x, BigInteger p, BigInteger g) {
        this.x = x;
        this.p = p;
        this.g = g;
    }

    /** The private exponent. */
    public BigInteger getX() {
        return this.x;
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
