package java.security.interfaces;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;

// A Montgomery-curve public key: the u coordinate, without v.
public interface XECPublicKey extends XECKey, PublicKey {

    BigInteger getU();

    // Here the clash between `XECKey` and `AsymmetricKey` is between two methods with **the same
    // signature**, not covariant ones: it has to be declared anyway, because one is abstract and
    // the other default.
    @Override
    default AlgorithmParameterSpec getParams() {
        return null;
    }
}
