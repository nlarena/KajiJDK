package java.security.spec;

import java.math.BigInteger;

// The ECC domain parameters: the curve, the generator point, the order of that generator and the
// cofactor.
//
// The four together are what two parties have to share for a public key to mean the same on both
// sides. An EC key without these parameters cannot be interpreted: the same pair (x, y) is a valid
// point on infinitely many different curves.
//
// The cofactor h = |E| / n is not a bookkeeping detail. It is the reason small-subgroup attacks
// exist: if h > 1, a point an attacker sends can live in a subgroup of small order and leak the
// private key modulo that order. That is why the value travels with the parameters instead of being
// deduced. This note also said that serious curves have it at 1; the NIST prime curves do, but
// Curve25519 has 8 and Ed448 has 4, and they defend by other means (see `XECPrivateKeySpec`).
public class ECParameterSpec implements AlgorithmParameterSpec {

    private final EllipticCurve curve;
    private final ECPoint g;
    private final BigInteger n;
    private final int h;

    public ECParameterSpec(EllipticCurve curve, ECPoint g, BigInteger n, int h) {
        if (curve == null) {
            throw new NullPointerException("curve is null");
        }
        if (g == null) {
            throw new NullPointerException("generator is null");
        }
        if (n == null) {
            throw new NullPointerException("order is null");
        }
        if (n.signum() != 1) {
            throw new IllegalArgumentException("n is not positive");
        }
        if (h <= 0) {
            throw new IllegalArgumentException("h is not positive");
        }
        this.curve = curve;
        this.g = g;
        this.n = n;
        this.h = h;
    }

    public EllipticCurve getCurve() {
        return this.curve;
    }

    public ECPoint getGenerator() {
        return this.g;
    }

    // The order of the generator: the smallest n such that n*G is the point at infinity.
    public BigInteger getOrder() {
        return this.n;
    }

    public int getCofactor() {
        return this.h;
    }
}
