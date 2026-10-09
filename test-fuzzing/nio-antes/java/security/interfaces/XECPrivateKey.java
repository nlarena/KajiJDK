package java.security.interfaces;

import java.security.PrivateKey;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Optional;

// A Montgomery-curve private key: the scalar, if the key lets it out.
public interface XECPrivateKey extends XECKey, PrivateKey {

    // The private scalar, or empty if it does not leave where it is.
    Optional<byte[]> getScalar();

    // Ver `XECPublicKey.getParams()`.
    @Override
    default AlgorithmParameterSpec getParams() {
        return null;
    }
}
