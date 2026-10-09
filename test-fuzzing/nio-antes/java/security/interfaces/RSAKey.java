package java.security.interfaces;

import java.math.BigInteger;
import java.security.spec.AlgorithmParameterSpec;

// What every RSA key has: the modulus.
//
// The modulus is public on both sides of the pair —it is in the public key as well as in the
// private one— and it is the only thing that can be asked for without knowing which of the two it
// is. It is also what fixes the "size" of the key: `getModulus().bitLength()` is what people call
// RSA-2048.
public interface RSAKey {

    BigInteger getModulus();

    // The algorithm parameters, for RSASSA-PSS. Null by default: most RSA keys carry none, and
    // implementations older than this method do not write it.
    default AlgorithmParameterSpec getParams() {
        return null;
    }
}
