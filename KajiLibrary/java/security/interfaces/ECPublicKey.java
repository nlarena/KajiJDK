package java.security.interfaces;

import java.security.PublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;

// An elliptic-curve public key: the point W = d*G.
public interface ECPublicKey extends PublicKey, ECKey {

    long serialVersionUID = -3314988629879632826L;

    // The public point.
    ECPoint getW();

    // Resolves the clash between `ECKey.getParams()` (abstract, returns `ECParameterSpec`) and
    // `AsymmetricKey.getParams()` (default, returns `AlgorithmParameterSpec`), which arrive through
    // different branches. Returns null for the same reason as `AsymmetricKey`'s: it means "this key
    // does not say what they are".
    @Override
    default ECParameterSpec getParams() {
        return null;
    }
}
