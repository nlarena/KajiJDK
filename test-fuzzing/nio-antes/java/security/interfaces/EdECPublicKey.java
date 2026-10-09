package java.security.interfaces;

import java.security.PublicKey;
import java.security.spec.EdECPoint;
import java.security.spec.NamedParameterSpec;

// An Edwards public key: the point in compressed form.
public interface EdECPublicKey extends EdECKey, PublicKey {

    // The public point: the y coordinate plus the sign bit of x.
    EdECPoint getPoint();

    // See `ECPublicKey.getParams()`: it breaks the tie between `EdECKey` and `AsymmetricKey`.
    @Override
    default NamedParameterSpec getParams() {
        return null;
    }
}
