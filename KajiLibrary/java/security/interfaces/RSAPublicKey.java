package java.security.interfaces;

import java.math.BigInteger;
import java.security.PublicKey;

// Una clave publica RSA: el par (n, e).
public interface RSAPublicKey extends PublicKey, RSAKey {

    long serialVersionUID = -8727434096241101194L;

    // El exponente publico, casi siempre 65537.
    BigInteger getPublicExponent();
}
