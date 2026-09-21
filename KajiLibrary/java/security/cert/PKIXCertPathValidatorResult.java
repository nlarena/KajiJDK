package java.security.cert;

import java.security.PublicKey;

// What is left when a PKIX validation went well: which anchor it ended up trusting, the policy
// tree, and the public key of the subject.
//
// The anchor is the datum that is almost always ignored and almost always matters. That the chain
// validates says little by itself: what has to be looked at is **against which root** it validated,
// because a chain that ends in a CA you were not expecting is exactly the attack. That is why the
// result returns it instead of swallowing it.
public class PKIXCertPathValidatorResult implements CertPathValidatorResult {

    private final TrustAnchor trustAnchor;
    private final PolicyNode policyTree;
    private final PublicKey subjectPublicKey;

    // The policy tree may be null —it means that there are no policies to sustain— but the anchor
    // and the key may not: without them the result would say nothing.
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

    // The anchor the chain ended in.
    public TrustAnchor getTrustAnchor() {
        return this.trustAnchor;
    }

    // The root of the tree of valid policies, or null if there is none.
    public PolicyNode getPolicyTree() {
        return this.policyTree;
    }

    // The public key of the certificate that was being validated.
    public PublicKey getPublicKey() {
        return this.subjectPublicKey;
    }

    // A shallow copy, and it is enough: the three fields are immutable or read-only.
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
