package java.security.spec;

import java.security.DEREncodable;

// Una clave **privada** codificada segun el `PrivateKeyInfo` de PKCS#8.
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
