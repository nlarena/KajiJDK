package java.security.spec;

import java.math.BigInteger;
import java.security.interfaces.DSAParams;

// The DSA domain parameters as a spec: p, q and g.
//
// It implements `AlgorithmParameterSpec` and `DSAParams` at once, and that is not redundancy: the
// first makes it passable to `AlgorithmParameters` and to generators, the second makes it passable
// where the parameters of a concrete DSA key are expected. It is the point where the world of "this
// is a description" and the world of "these are that key's parameters" meet.
//
// It does not validate that q divides p-1 nor that g generates the right subgroup. As in the rest
// of the package: it is a container, and the checks that matter cost modular exponentiations that a
// constructor should not do.
public class DSAParameterSpec implements AlgorithmParameterSpec, DSAParams {

    private final BigInteger p;
    private final BigInteger q;
    private final BigInteger g;

    public DSAParameterSpec(BigInteger p, BigInteger q, BigInteger g) {
        this.p = p;
        this.q = q;
        this.g = g;
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
