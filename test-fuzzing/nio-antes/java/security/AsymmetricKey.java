package java.security;

import java.security.spec.AlgorithmParameterSpec;

// The half of an asymmetric pair: what `PublicKey` and `PrivateKey` have in common.
//
// It was added late (JDK 22) and not out of tidiness: what it contributes is `getParams()`, and the
// point is that in an asymmetric algorithm the parameters of the domain —the curve, the group— are
// part of the identity of the key and until then each family exposed them with a method of its own
// in its specific interface. Here it is asked once, without knowing which family the key is of.
public interface AsymmetricKey extends Key, DEREncodable {

    // The associated parameters, or null if the key has none.
    //
    // Default and not abstract because there are implementations older than this method that do not
    // write it: for them the right answer is "I do not know", and null is how that is said.
    default AlgorithmParameterSpec getParams() {
        return null;
    }
}
