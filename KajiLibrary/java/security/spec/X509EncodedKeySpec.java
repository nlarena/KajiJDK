package java.security.spec;

import java.security.DEREncodable;

// Una clave **publica** codificada segun el `SubjectPublicKeyInfo` de X.509.
//
// El par con `PKCS8EncodedKeySpec` no es simetrico por casualidad: X.509 es el formato de las
// claves que se publican y PKCS#8 el de las que no. Que sean tipos distintos y no un solo tipo con
// un string de formato es lo que impide que una privada entre por donde se espera una publica.
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

    // "X.509", siempre. `final` porque el formato es lo que define a esta clase: una subclase que
    // lo cambiara estaria mintiendo sobre que contiene.
    @Override
    public final String getFormat() {
        return "X.509";
    }
}
