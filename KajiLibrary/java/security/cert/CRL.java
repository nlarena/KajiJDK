package java.security.cert;

// Una lista de certificados revocados.
//
// La clase es minima a proposito: lo unico que toda CRL sabe hacer, sea del formato que sea, es
// decir si un certificado esta en ella. Todo lo demas —emisor, fechas, extensiones— es especifico
// de X.509 y vive en `X509CRL`.
//
// El modelo de las CRLs tiene un problema estructural que conviene tener presente: son una foto con
// fecha. Entre que se emite una y se emite la siguiente, una revocacion no se ve. Esa ventana es la
// razon por la que existe OCSP, y por la que `PKIXRevocationChecker` deja elegir cual se prefiere.
public abstract class CRL {

    private final String type;

    protected CRL(String type) {
        this.type = type;
    }

    // El tipo: "X.509". `final` porque lo fija el constructor, y una subclase que mintiera sobre el
    // haria que el codigo que despacha por tipo eligiera el parser equivocado.
    public final String getType() {
        return this.type;
    }

    @Override
    public abstract String toString();

    // Si este certificado figura como revocado en esta lista.
    public abstract boolean isRevoked(Certificate cert);
}
