package java.security.interfaces;

import java.math.BigInteger;
import java.security.PrivateKey;

// A DSA private key: the secret exponent x.
//
// That `getX()` exists in the API is a legacy of when keys were assumed to live in memory. A key in
// hardware cannot implement it without ceasing to be in hardware, and that is why modern code signs
// by asking the key to sign instead of taking x out of it.
public interface DSAPrivateKey extends DSAKey, PrivateKey {

    long serialVersionUID = 7776497482533790279L;

    // The private exponent x.
    BigInteger getX();

    // See `DSAPublicKey.getParams()`: it breaks the inheritance tie between `DSAKey` and
    // `AsymmetricKey`, and returns null by default for the same reason.
    @Override
    default DSAParams getParams() {
        return null;
    }
}
