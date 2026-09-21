package java.security.spec;

import java.math.BigInteger;

// The elliptic curve itself: its coefficients a and b over a field, plus —optionally— the seed it
// was generated from. Over GF(p) the equation is y^2 = x^3 + a*x + b; over GF(2^m) it is
// y^2 + x*y = x^3 + a*x^2 + b. (This note gave only the first, as if it held for both fields.)
//
// The seed is not decorative. Standard curves are derived from a public value passed through a
// hash, and publishing it is what lets anyone recompute a and b and check that they were not picked
// by hand to hide a weakness. Its being optional reflects that many curves simply do not publish
// it.
//
// `equals` does **not** compare the seed, and that is right: two curves with the same a, b and
// field are the same curve even if one says how it was generated and the other does not. The seed
// is provenance, not identity.
//
// Validating a and b is only a range check, not a check that the curve is non-singular. This note
// said that check is cheap over GF(p) but not over GF(2^m); it is cheap over both —4a^3 + 27b^2 !=
// 0 over GF(p), b != 0 over GF(2^m)— and the JDK does it over neither. A descriptor describes;
// whoever operates on the curve decides whether to accept it. The range check is also narrower than
// the JDK's: over GF(p) it rejects a coefficient that is not below p but, unlike the JDK, not a
// negative one.
public class EllipticCurve {

    private final ECField field;
    private final BigInteger a;
    private final BigInteger b;
    private final byte[] seed;

    public EllipticCurve(ECField field, BigInteger a, BigInteger b) {
        this(field, a, b, null);
    }

    public EllipticCurve(ECField field, BigInteger a, BigInteger b, byte[] seed) {
        if (field == null) {
            throw new NullPointerException("field is null");
        }
        if (a == null) {
            throw new NullPointerException("first coefficient is null");
        }
        if (b == null) {
            throw new NullPointerException("second coefficient is null");
        }
        // The coefficients are field elements, so they have to fit in it. Since "fitting" is said
        // differently for each field, the concrete type is asked; a field of another class is not
        // validated because there is no way to know what it means there.
        if (field instanceof ECFieldFp) {
            BigInteger p = ((ECFieldFp) field).getP();
            if (p.compareTo(a) != 1) {
                throw new IllegalArgumentException("first coefficient is too large");
            }
            if (p.compareTo(b) != 1) {
                throw new IllegalArgumentException("second coefficient is too large");
            }
        } else if (field instanceof ECFieldF2m) {
            int m = ((ECFieldF2m) field).getM();
            if (a.bitLength() > m) {
                throw new IllegalArgumentException("first coefficient is too large");
            }
            if (b.bitLength() > m) {
                throw new IllegalArgumentException("second coefficient is too large");
            }
        }
        this.field = field;
        this.a = a;
        this.b = b;
        this.seed = copyOf(seed);
    }

    private static byte[] copyOf(byte[] b) {
        if (b == null) {
            return null;
        }
        byte[] c = new byte[b.length];
        System.arraycopy(b, 0, c, 0, b.length);
        return c;
    }

    public ECField getField() {
        return this.field;
    }

    public BigInteger getA() {
        return this.a;
    }

    public BigInteger getB() {
        return this.b;
    }

    // A copy of the seed, or null if the curve does not say how it was generated.
    public byte[] getSeed() {
        return copyOf(this.seed);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof EllipticCurve) {
            EllipticCurve other = (EllipticCurve) obj;
            return this.field.equals(other.field)
                && this.a.equals(other.a)
                && this.b.equals(other.b);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = this.field.hashCode();
        h = h * 31 + this.a.hashCode();
        h = h * 31 + this.b.hashCode();
        return h;
    }
}
