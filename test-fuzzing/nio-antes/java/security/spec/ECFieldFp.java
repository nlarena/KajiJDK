package java.security.spec;

import java.math.BigInteger;

// A prime field GF(p): the integers modulo a prime.
//
// The class **does not check that `p` is prime**, and that is the JDK's contract, not an omission
// here: testing the primality of a 256-bit number in a constructor called for every key would be a
// cost nobody asked for. Whoever builds the field is responsible for it being prime.
public class ECFieldFp implements ECField {

    private final BigInteger p;

    public ECFieldFp(BigInteger p) {
        if (p == null) {
            throw new NullPointerException("p is null");
        }
        if (p.signum() != 1) {
            throw new IllegalArgumentException("p is not positive");
        }
        this.p = p;
    }

    // The size of `p` in bits, which is what an element of the field takes.
    public int getFieldSize() {
        return this.p.bitLength();
    }

    public BigInteger getP() {
        return this.p;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ECFieldFp)) {
            return false;
        }
        return this.p.equals(((ECFieldFp) obj).p);
    }

    @Override
    public int hashCode() {
        return this.p.hashCode();
    }
}
