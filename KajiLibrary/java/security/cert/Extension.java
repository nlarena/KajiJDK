package java.security.cert;

import java.io.IOException;
import java.io.OutputStream;

// An X.509 extension seen one at a time: its OID, whether it is critical and its value.
//
// It is newer than `X509Extension` (Java 7) and the difference is in the unit: that one describes
// the object that **has** extensions, this one describes **one** loose extension. It was needed
// when APIs appeared that receive extensions separately, such as the ones sent in an OCSP query.
public interface Extension {

    // The OID in dotted notation.
    String getId();

    // Whether it is critical: whoever does not understand it has to reject the object that carries
    // it.
    boolean isCritical();

    // The DER value of the extension, already without the wrapping of the OCTET STRING.
    byte[] getValue();

    // Writes the complete extension —OID, criticality and value— encoded in DER.
    void encode(OutputStream out) throws IOException;
}
