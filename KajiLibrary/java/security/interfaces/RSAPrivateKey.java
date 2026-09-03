package java.security.interfaces;

import java.math.BigInteger;
import java.security.PrivateKey;

// Una clave privada RSA en su forma minima: el par (n, d).
public interface RSAPrivateKey extends PrivateKey, RSAKey {

    long serialVersionUID = 5187144804936595022L;

    // El exponente privado. Una clave en hardware no puede implementarlo sin dejar de estar
    // protegida, asi que el codigo que lo llama esta atado a claves en memoria.
    BigInteger getPrivateExponent();
}
