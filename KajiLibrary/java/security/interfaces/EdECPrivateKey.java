package java.security.interfaces;

import java.security.PrivateKey;
import java.security.spec.NamedParameterSpec;
import java.util.Optional;

// An Edwards private key: the seed from which the scalar and the nonce are derived.
public interface EdECPrivateKey extends EdECKey, PrivateKey {

    // The seed, or empty if the key does not let it out. `Optional` and not null because here "I
    // cannot give you this" is a legitimate and frequent answer —a key in hardware— and deserves to
    // be said without the caller having to remember to check.
    Optional<byte[]> getBytes();

    // Ver `ECPublicKey.getParams()`.
    @Override
    default NamedParameterSpec getParams() {
        return null;
    }
}
