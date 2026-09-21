package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.util.Set;

// The parameters of a **building** of a PKIX path: the same ones as the validation, plus the
// maximum length.
//
// The maximum length is not an optimisation, it is a cut for security. Building a path is a search,
// and with no ceiling that search may not end: a set of cross certificates —very common between CAs
// that sign each other— produces cycles, and a few are enough for the space of possible paths to
// explode. The default of 5 comes from the fact that in practice no real chain goes past three or
// four links.
//
// The -1 means "no limit" and has to be read like that and not as "zero": it is the only negative
// value accepted, and that is why the setter rejects -2 instead of treating it as another "no
// limit".
//
public class PKIXBuilderParameters extends PKIXParameters {

    private int maxPathLength = 5;

    // `targetConstraints` may be null, but it is better not to leave it: with no criterion for the
    // certificate at the end, the builder does not know which way to look.
    public PKIXBuilderParameters(Set<TrustAnchor> trustAnchors, CertSelector targetConstraints)
            throws InvalidAlgorithmParameterException {
        super(trustAnchors);
        super.setTargetCertConstraints(targetConstraints);
    }

    // The same, with the anchors taken from a store. See `PKIXParameters(KeyStore)` for which
    // entries are looked at and which are not.
    public PKIXBuilderParameters(java.security.KeyStore keystore, CertSelector targetConstraints)
            throws java.security.KeyStoreException, InvalidAlgorithmParameterException {
        super(keystore);
        super.setTargetCertConstraints(targetConstraints);
    }

    // The maximum length of the chain, not counting the anchor. -1 removes the limit; 0 forces the
    // anchor to have directly signed the certificate being looked for.
    public void setMaxPathLength(int maxPathLength) {
        if (maxPathLength < -1) {
            throw new InvalidParameterException("the maximum path "
                + "length parameter can not be less than -1");
        }
        this.maxPathLength = maxPathLength;
    }

    public int getMaxPathLength() {
        return this.maxPathLength;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        sb.append(super.toString());
        sb.append("  Maximum Path Length: " + this.maxPathLength + "\n");
        sb.append("]\n");
        return sb.toString();
    }
}
