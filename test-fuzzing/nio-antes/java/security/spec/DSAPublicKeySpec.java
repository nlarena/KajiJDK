package java.security.spec;

import java.math.BigInteger;

// A DSA public key in the clear: y, plus the parameters p, q, g.
//
// The parameters come loose instead of in a `DSAParameterSpec` because of the API's age, not by
// design: this class is from JDK 1.2, and `DSAParameterSpec` is a sibling from the same release,
// not an older class it could have used.
public class DSAPublicKeySpec implements KeySpec {

    private final BigInteger y;
    private final BigInteger p;
    private final BigInteger q;
    private final BigInteger g;

    public DSAPublicKeySpec(BigInteger y, BigInteger p, BigInteger q, BigInteger g) {
        this.y = y;
        this.p = p;
        this.q = q;
        this.g = g;
    }

    // The public value y = g^x mod p.
    public BigInteger getY() {
        return this.y;
    }

    public BigInteger getP() {
        return this.p;
    }

    public BigInteger getQ() {
        return this.q;
    }

    public BigInteger getG() {
        return this.g;
    }
}
