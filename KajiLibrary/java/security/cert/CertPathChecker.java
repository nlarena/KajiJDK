package java.security.cert;

// Una comprobacion que se aplica a cada certificado de un camino, uno por uno.
//
// Es el mecanismo de extension de la validacion: quien necesite una regla que PKIX no trae —una
// politica interna, una lista propia— la escribe aca en vez de reimplementar la validacion entera.
//
// `init(boolean forward)` no es un detalle. Un camino se puede recorrer desde el sujeto hacia la
// raiz o al reves, y hay comprobaciones que solo tienen sentido en un sentido —las que necesitan
// saber quien firmo antes de mirar al firmado, por ejemplo—. Por eso el checker declara con
// `isForwardCheckingSupported()` que sabe hacer, e `init` le dice cual le toco esta vez.
public interface CertPathChecker {

    // Prepara el checker y le dice en que sentido se va a recorrer. Se llama antes de la primera
    // comprobacion, y sirve tambien para resetear el estado entre usos.
    void init(boolean forward) throws CertPathValidatorException;

    // Si sabe comprobar en el sentido sujeto -> raiz.
    boolean isForwardCheckingSupported();

    // Comprueba un certificado. **Sin valor de retorno**: si no lanza, paso. Es el mismo contrato
    // que `Certificate.verify`, y trae el mismo riesgo: un `catch` vacio aca acepta cualquier cosa.
    void check(Certificate cert) throws CertPathValidatorException;
}
