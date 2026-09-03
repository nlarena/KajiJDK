package java.security.cert;

import java.util.Set;

// Lo que un objeto X.509 con extensiones —certificado, CRL, entrada de CRL— sabe decir sobre ellas.
//
// La distincion entre criticas y no criticas es **la** regla de seguridad de las extensiones, y
// esta en el nombre de los metodos: una extension critica que quien valida no entiende obliga a
// rechazar el objeto entero. No es una recomendacion. Es lo que permite que una CA agregue una
// restriccion nueva sabiendo que ningun cliente viejo la va a ignorar en silencio.
//
// `hasUnsupportedCriticalExtension()` existe justamente para preguntar eso de una sola vez.
public interface X509Extension {

    // Si hay alguna extension critica que esta implementacion no sabe procesar. Si da true, el
    // objeto **no** se debe usar.
    boolean hasUnsupportedCriticalExtension();

    // Los OIDs de las extensiones criticas, o null si no hay ninguna.
    Set<String> getCriticalExtensionOIDs();

    // Los OIDs de las no criticas, o null si no hay ninguna.
    Set<String> getNonCriticalExtensionOIDs();

    // El valor DER de una extension por su OID, o null si no esta. Son los bytes del OCTET STRING
    // que envuelve al valor, sin desenvolver: quien pregunta ya sabe que espera adentro.
    byte[] getExtensionValue(String oid);
}
