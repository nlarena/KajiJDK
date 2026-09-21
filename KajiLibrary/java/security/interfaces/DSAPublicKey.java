package java.security.interfaces;

import java.math.BigInteger;
import java.security.PublicKey;

// A DSA public key: y = g^x mod p, where x is the private one.
//
// The default `getParams()` resolves a real inheritance clash. `DSAKey.getParams()` returns
// `DSAParams` and `AsymmetricKey.getParams()` —which arrives through `PublicKey`— returns
// `AlgorithmParameterSpec`; since `DSAParams` extends `AlgorithmParameterSpec`, the `DSAKey` method
// is a valid covariant override of the other, but Java does not pick it by itself when the two
// arrive through different branches. The default breaks the tie by declaring which one wins.
public interface DSAPublicKey extends DSAKey, PublicKey {

    long serialVersionUID = 1234526332779022332L;

    // The public value y.
    BigInteger getY();

    // Returns null by default, like `AsymmetricKey.getParams()`: it means "this key does not say
    // what its parameters are". Being default and not abstract keeps implementations older than
    // `AsymmetricKey` compiling.
    @Override
    default DSAParams getParams() {
        return null;
    }
}
