package java.security;

import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

// The provider's face for a `KeyFactory`.
//
// The four methods are abstract: converting between the opaque and the transparent form of a key
// can only be done by whoever knows the algorithm. KajiLibrary brings no implementation.
public abstract class KeyFactorySpi {

    public KeyFactorySpi() {
    }

    protected abstract PublicKey engineGeneratePublic(KeySpec keySpec)
        throws InvalidKeySpecException;

    protected abstract PrivateKey engineGeneratePrivate(KeySpec keySpec)
        throws InvalidKeySpecException;

    protected abstract <T extends KeySpec> T engineGetKeySpec(Key key, Class<T> keySpec)
        throws InvalidKeySpecException;

    // Translates a key of another provider into one of this one.
    protected abstract Key engineTranslateKey(Key key) throws InvalidKeyException;
}
