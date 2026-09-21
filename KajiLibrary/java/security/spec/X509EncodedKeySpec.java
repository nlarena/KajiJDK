package java.security.spec;

import java.security.DEREncodable;

// A **public** key encoded as X.509's `SubjectPublicKeyInfo`.
//
// The pair with `PKCS8EncodedKeySpec` is not symmetric by accident: X.509 is the format of keys
// that are published and PKCS#8 that of keys that are not. That they are distinct types and not one
// type with a format string is what keeps a private key from getting in where a public one is
// expected.
public class X509EncodedKeySpec extends EncodedKeySpec implements DEREncodable {

    public X509EncodedKeySpec(byte[] encodedKey) {
        super(encodedKey);
    }

    public X509EncodedKeySpec(byte[] encodedKey, String algorithm) {
        super(encodedKey, algorithm);
    }

    @Override
    public byte[] getEncoded() {
        return super.getEncoded();
    }

    // "X.509", always. `final` because the format is what defines this class: a subclass that
    // changed it would be lying about what it holds.
    @Override
    public final String getFormat() {
        return "X.509";
    }
}
