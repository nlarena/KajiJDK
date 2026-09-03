package java.security.cert;

import java.security.PublicKey;

// El resultado de construir un camino PKIX: el camino, mas todo lo que devuelve una validacion.
//
// Hereda del resultado de validacion en vez de ser un tipo aparte, y eso dice algo: construir un
// camino **incluye** validarlo. Un constructor que devolviera cadenas armadas pero sin verificar
// seria peor que inutil, porque el nombre invitaria a confiar en ellas.
//
// El camino que sale de aca **no incluye el ancla**. Es facil de olvidar y cambia los indices: la
// cadena va del sujeto hasta el certificado emitido por la raiz, y la raiz misma esta en
// `getTrustAnchor()`.
public class PKIXCertPathBuilderResult extends PKIXCertPathValidatorResult
        implements CertPathBuilderResult {

    private final CertPath certPath;

    public PKIXCertPathBuilderResult(CertPath certPath, TrustAnchor trustAnchor,
                                     PolicyNode policyTree, PublicKey subjectPublicKey) {
        super(trustAnchor, policyTree, subjectPublicKey);
        if (certPath == null) {
            throw new NullPointerException("certPath must be non-null");
        }
        this.certPath = certPath;
    }

    // El camino construido y validado, sin el ancla.
    @Override
    public CertPath getCertPath() {
        return this.certPath;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PKIXCertPathBuilderResult: [\n");
        sb.append("  Certification Path: " + this.certPath + "\n");
        sb.append("  Trust Anchor: " + this.getTrustAnchor().toString() + "\n");
        sb.append("  Policy Tree: " + String.valueOf(this.getPolicyTree()) + "\n");
        sb.append("  Subject Public Key: " + this.getPublicKey() + "\n");
        sb.append("]");
        return sb.toString();
    }
}
