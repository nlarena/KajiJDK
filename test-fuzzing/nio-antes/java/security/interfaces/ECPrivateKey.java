package java.security.interfaces;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.spec.ECParameterSpec;

// An elliptic-curve private key: the scalar s.
public interface ECPrivateKey extends PrivateKey, ECKey {

    long serialVersionUID = -7896394956925609184L;

    // The private scalar.
    BigInteger getS();

    // See `ECPublicKey.getParams()`: same clash, same resolution.
    @Override
    default ECParameterSpec getParams() {
        return null;
    }
}
