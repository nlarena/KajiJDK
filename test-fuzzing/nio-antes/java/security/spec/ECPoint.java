package java.security.spec;

import java.math.BigInteger;

// A point of an elliptic curve in affine coordinates.
//
// It deliberately does **not** know which curve it belongs to: the pair (x, y) only makes sense
// next to an `EllipticCurve`, and keeping them apart is what lets the same class serve for the
// generator of an `ECParameterSpec` and for the public key of an `ECPublicKeySpec` without
// duplicating the type.
//
// This class does no curve arithmetic —it neither adds points nor multiplies by scalars— and that
// is on purpose: it is a descriptor, not an ECC implementation. Adding two points requires knowing
// the curve, which is not here.
public class ECPoint {

    // The point at infinity: the identity of the group. It is represented with both coordinates
    // null because it **has no** affine coordinates; it is not (0, 0) nor any other concrete pair.
    // That absence is what forces `equals` and `hashCode` to treat it separately.
    public static final ECPoint POINT_INFINITY = new ECPoint();

    private final BigInteger x;
    private final BigInteger y;

    // Private constructor, only for POINT_INFINITY: it is the only legitimate way to have an
    // ECPoint with null coordinates.
    private ECPoint() {
        this.x = null;
        this.y = null;
    }

    public ECPoint(BigInteger x, BigInteger y) {
        if ((x == null) || (y == null)) {
            throw new NullPointerException("affine coordinate x or y is null");
        }
        this.x = x;
        this.y = y;
    }

    // The x coordinate, or null if this is the point at infinity.
    public BigInteger getAffineX() {
        return this.x;
    }

    // The y coordinate, or null if this is the point at infinity.
    public BigInteger getAffineY() {
        return this.y;
    }

    // Infinity is only equal to itself. The check goes first because comparing its null coordinates
    // against another point's would be an NPE, and because two infinities are always the same
    // instance: the constant.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (this == POINT_INFINITY) {
            return false;
        }
        if (obj instanceof ECPoint) {
            ECPoint other = (ECPoint) obj;
            return this.x.equals(other.x) && this.y.equals(other.y);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this == POINT_INFINITY) {
            return 0;
        }
        return this.x.hashCode() * 31 + this.y.hashCode();
    }
}
