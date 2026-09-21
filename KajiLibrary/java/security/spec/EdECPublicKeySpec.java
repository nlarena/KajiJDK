package java.security.spec;

// An Edwards public key: the curve by name, plus the point.
//
// The parameters are a `NamedParameterSpec` and not an `ECParameterSpec` on purpose: for Edwards
// curves there is no offer to describe the curve by hand. Ed25519 and Ed448 are the only two, their
// parameters are fixed, and giving no way to write others is what keeps anyone from signing over a
// badly chosen curve.
public final class EdECPublicKeySpec implements KeySpec {

    private final NamedParameterSpec params;
    private final EdECPoint point;

    public EdECPublicKeySpec(NamedParameterSpec params, EdECPoint point) {
        if (params == null) {
            throw new NullPointerException("params must not be null");
        }
        if (point == null) {
            throw new NullPointerException("point must not be null");
        }
        this.params = params;
        this.point = point;
    }

    public NamedParameterSpec getParams() {
        return this.params;
    }

    public EdECPoint getPoint() {
        return this.point;
    }
}
