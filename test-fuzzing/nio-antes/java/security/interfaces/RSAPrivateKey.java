package java.security.interfaces;

import java.math.BigInteger;
import java.security.PrivateKey;

// An RSA private key in its minimal form: the pair (n, d).
public interface RSAPrivateKey extends PrivateKey, RSAKey {

    long serialVersionUID = 5187144804936595022L;

    // The private exponent. A key in hardware cannot implement it without ceasing to be protected,
    // so code that calls it is tied to keys in memory.
    BigInteger getPrivateExponent();
}
