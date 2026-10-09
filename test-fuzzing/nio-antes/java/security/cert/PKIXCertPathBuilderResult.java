package java.security.cert;

import java.security.PublicKey;

// The result of building a PKIX path: the path, plus everything a validation returns.
//
// It inherits from the validation result instead of being a separate type, and that says something:
// building a path **includes** validating it. A builder that returned assembled but unverified
// chains would be worse than useless, because the name would invite trust in them.
//
// The path that comes out of here **does not include the anchor**. It is easy to forget and it
// changes the indices: the chain goes from the subject to the certificate issued by the root, and
// the root itself is in `getTrustAnchor()`.
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

    // The built and validated path, without the anchor.
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
