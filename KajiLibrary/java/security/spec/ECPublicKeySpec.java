package java.security.spec;

// An EC public key in the clear: the point W = d*G, plus the domain parameters.
//
// Unlike `X509EncodedKeySpec`, here the key is **open**: the point can be seen. That makes it
// useful for building a key without going through DER, and it is also why the parameters have to
// come with it: a point without a curve identifies no key.
public class ECPublicKeySpec implements KeySpec {

    private final ECPoint w;
    private final ECParameterSpec params;

    public ECPublicKeySpec(ECPoint w, ECParameterSpec params) {
        if (w == null) {
            throw new NullPointerException("w is null");
        }
        if (params == null) {
            throw new NullPointerException("params is null");
        }
        // Infinity is the identity of the group: as a public key it would mean d = 0, that is, no
        // key at all. Rejecting it here keeps it from reaching a protocol where the result would be
        // a constant shared secret.
        if (w == ECPoint.POINT_INFINITY) {
            throw new IllegalArgumentException("w is ECPoint.POINT_INFINITY");
        }
        this.w = w;
        this.params = params;
    }

    // The public point.
    public ECPoint getW() {
        return this.w;
    }

    public ECParameterSpec getParams() {
        return this.params;
    }
}
