package java.security.interfaces;

import java.math.BigInteger;
import java.security.PublicKey;

// An RSA public key: the pair (n, e).
public interface RSAPublicKey extends PublicKey, RSAKey {

    long serialVersionUID = -8727434096241101194L;

    // The public exponent, almost always 65537.
    BigInteger getPublicExponent();
}
