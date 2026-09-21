package java.security.spec;

import java.math.BigInteger;

// A point of an Edwards curve, in RFC 8032's compressed form: the whole y coordinate, plus **a
// single bit** of x.
//
// That one bit is enough is the property that makes Ed25519 compact. The curve equation determines
// x^2 from y, so two candidates remain, x and -x; knowing whether x is even or odd picks one. An
// Ed25519 public key is therefore 32 bytes instead of 64.
//
// This class does not decompress: it does not compute x from y, because that needs a modular square
// root over the concrete curve, which is not here. It keeps the two pieces of data and returns
// them.
public final class EdECPoint {

    private final boolean xOdd;
    private final BigInteger y;

    public EdECPoint(boolean xOdd, BigInteger y) {
        if (y == null) {
            throw new NullPointerException("y must not be null");
        }
        this.xOdd = xOdd;
        this.y = y;
    }

    // Whether the x coordinate is odd: the bit that breaks the tie between x and -x.
    public boolean isXOdd() {
        return this.xOdd;
    }

    public BigInteger getY() {
        return this.y;
    }
}
