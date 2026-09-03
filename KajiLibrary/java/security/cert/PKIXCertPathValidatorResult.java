package java.security.cert;

import java.security.PublicKey;

// Lo que queda cuando una validacion PKIX salio bien: en que ancla termino confiando, el arbol de
// politicas, y la clave publica del sujeto.
//
// El ancla es el dato que casi siempre se ignora y casi siempre importa. Que la cadena valide dice
// poco por si solo: lo que hay que mirar es **contra que raiz** valido, porque una cadena que
// termina en una CA que no esperabas es exactamente el ataque. Por eso el resultado la devuelve en
// vez de tragarsela.
public class PKIXCertPathValidatorResult implements CertPathValidatorResult {

    private final TrustAnchor trustAnchor;
    private final PolicyNode policyTree;
    private final PublicKey subjectPublicKey;

    // El arbol de politicas puede ser null —significa que no hay politicas que sostener— pero el
    // ancla y la clave no: sin ellas el resultado no diria nada.
    public PKIXCertPathValidatorResult(TrustAnchor trustAnchor, PolicyNode policyTree,
                                       PublicKey subjectPublicKey) {
        if (subjectPublicKey == null) {
            throw new NullPointerException("subjectPublicKey must be non-null");
        }
        if (trustAnchor == null) {
            throw new NullPointerException("trustAnchor must be non-null");
        }
        this.trustAnchor = trustAnchor;
        this.policyTree = policyTree;
        this.subjectPublicKey = subjectPublicKey;
    }

    // El ancla en la que termino la cadena.
    public TrustAnchor getTrustAnchor() {
        return this.trustAnchor;
    }

    // La raiz del arbol de politicas validas, o null si no hay.
    public PolicyNode getPolicyTree() {
        return this.policyTree;
    }

    // La clave publica del certificado que se estaba validando.
    public PublicKey getPublicKey() {
        return this.subjectPublicKey;
    }

    // Copia superficial, y alcanza: los tres campos son inmutables o de solo lectura.
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PKIXCertPathValidatorResult: [\n");
        sb.append("  Trust Anchor: " + this.trustAnchor.toString() + "\n");
        sb.append("  Policy Tree: " + String.valueOf(this.policyTree) + "\n");
        sb.append("  Subject Public Key: " + this.subjectPublicKey + "\n");
        sb.append("]");
        return sb.toString();
    }
}
