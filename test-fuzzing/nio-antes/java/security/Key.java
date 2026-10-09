package java.security;

import java.io.Serializable;

// A cryptographic key, seen as something **opaque**.
//
// The three methods are all that can be asked without opening the key, and they are chosen so that
// a key that lives in a card or in an HSM can go on being a `Key`: it says which algorithm it is
// of, and —if at all— with which format it lets itself be exported. `getEncoded()` may legitimately
// return **null**, and that is not an error: it means "this key does not leave here". Code that
// assumes it is never null breaks precisely with the best protected keys.
public interface Key extends Serializable {

    // The serialisation identifier is part of the public contract: it fixes the format with which a
    // key travels between VMs, and changing it breaks everything already serialised.
    long serialVersionUID = 6603384152749567654L;

    // The name of the algorithm: "RSA", "DSA", "AES".
    String getAlgorithm();

    // The name of the format of `getEncoded()` —"X.509", "PKCS#8"— or null if it does not let
    // itself be exported.
    String getFormat();

    // The encoded key, or null if it does not let itself be exported.
    byte[] getEncoded();
}
