package java.security.spec;

import java.security.DEREncodable;

// A **private** key encoded as PKCS#8's `PrivateKeyInfo`.
public class PKCS8EncodedKeySpec extends EncodedKeySpec implements DEREncodable {

    public PKCS8EncodedKeySpec(byte[] encodedKey) {
        super(encodedKey);
    }

    public PKCS8EncodedKeySpec(byte[] encodedKey, String algorithm) {
        super(encodedKey, algorithm);
    }

    @Override
    public byte[] getEncoded() {
        return super.getEncoded();
    }

    @Override
    public final String getFormat() {
        return "PKCS#8";
    }
}
