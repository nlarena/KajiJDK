package java.security.cert;

// El resultado de construir un camino: el camino que se encontro.
//
// Construir incluye validar —un constructor que devolviera cadenas sin verificar no serviria para
// nada— y por eso `PKIXCertPathBuilderResult` extiende al resultado de validacion en vez de ser un
// tipo aparte.
public interface CertPathBuilderResult extends Cloneable {

    // El camino construido.
    CertPath getCertPath();

    Object clone();
}
