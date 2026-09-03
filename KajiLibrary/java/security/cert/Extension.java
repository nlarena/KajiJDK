package java.security.cert;

import java.io.IOException;
import java.io.OutputStream;

// Una extension X.509 vista de a una: su OID, si es critica y su valor.
//
// Es mas nueva que `X509Extension` (Java 7) y la diferencia esta en la unidad: aquella describe al
// objeto que **tiene** extensiones, esta describe **una** extension suelta. Hizo falta cuando
// aparecieron APIs que reciben extensiones por separado, como las que se mandan en una consulta
// OCSP.
public interface Extension {

    // El OID en notacion de puntos.
    String getId();

    // Si es critica: quien no la entienda tiene que rechazar el objeto que la lleva.
    boolean isCritical();

    // El valor DER de la extension, ya sin el envoltorio del OCTET STRING.
    byte[] getValue();

    // Escribe la extension completa —OID, criticidad y valor— codificada en DER.
    void encode(OutputStream out) throws IOException;
}
