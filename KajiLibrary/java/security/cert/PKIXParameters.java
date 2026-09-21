package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// The parameters of a PKIX validation: against which anchors, with which date, with which rules.
//
// It is the class where the whole policy of the validation is configured, and therefore where the
// mistakes that matter are made. The defaults are chosen on the safe side and turning them off is
// easy and silent:
//
//   - `revocationEnabled` starts at **true**. Turning it off makes a revoked certificate validate
//     all the same. It is the line that appears most often in code that "fixed" a connectivity
//     problem.
//   - `policyQualifiersRejected` starts at **true**: a critical policy qualifier the validator does
//     not process makes it fail. Setting it to false forces the caller to process them themselves,
//     and hardly anybody does.
//   - `date` at null means **now**. Fixing it serves for verifying an old signature, and there it
//     is legitimate; fixing it in the past so that an expired certificate passes is like not
//     validating.
//
// The other thing that matters is that the object is **mutable** and the validator keeps it: that
// is why it implements `clone()`, that is why `getTrustAnchors()` returns an immutable set, and
// that is why the lists are copied on the way in and on the way out. Without that, changing the
// parameters halfway through a validation would change the rules while it runs.
//
// The constructor that takes a `KeyStore` is a convenience with a filter worth keeping in mind: it
// only looks at the **trusted certificate** entries, not at the private key ones. A store that has
// the key of the server and nothing else contributes no anchor, and the result is not an empty
// object but an `InvalidAlgorithmParameterException` —what has to be done with an empty set of
// anchors is to fail, not to validate against nothing—.
public class PKIXParameters implements CertPathParameters {

    private Set<TrustAnchor> unmodTrustAnchors;
    private Date date;
    private List<PKIXCertPathChecker> certPathCheckers;
    private String sigProvider;
    private boolean revocationEnabled = true;
    private Set<String> unmodInitialPolicies;
    private boolean explicitPolicyRequired = false;
    private boolean policyMappingInhibited = false;
    private boolean anyPolicyInhibited = false;
    private boolean policyQualifiersRejected = true;
    private List<CertStore> certStores;
    private CertSelector certSelector;

    // The set of anchors cannot be empty: with no anchor there is nowhere to end the chain, and a
    // validation that cannot end is not a validation.
    public PKIXParameters(Set<TrustAnchor> trustAnchors)
            throws InvalidAlgorithmParameterException {
        setTrustAnchors(trustAnchors);
        this.unmodInitialPolicies = Collections.emptySet();
        this.certPathCheckers = new ArrayList<PKIXCertPathChecker>();
        this.certStores = new ArrayList<CertStore>();
    }

    // The anchors come from the trusted certificate entries of the store.
    //
    // The private key entries are skipped on purpose: the certificate that accompanies a key of
    // one's own is one's identity, not a CA to trust. Putting it in as an anchor is like signing
    // one's own certificates and believing oneself.
    public PKIXParameters(java.security.KeyStore keystore)
            throws java.security.KeyStoreException, InvalidAlgorithmParameterException {
        if (keystore == null) {
            throw new NullPointerException("the keystore parameter must be non-null");
        }
        Set<TrustAnchor> anchors = new HashSet<TrustAnchor>();
        java.util.Enumeration<String> alias = keystore.aliases();
        while (alias.hasMoreElements()) {
            String a = alias.nextElement();
            if (keystore.isCertificateEntry(a)) {
                java.security.cert.Certificate c = keystore.getCertificate(a);
                if (c instanceof X509Certificate) {
                    anchors.add(new TrustAnchor((X509Certificate) c, null));
                }
            }
        }
        // Without `setTrustAnchors` a store with no trusted certificates would give an object that
        // looks valid and validates nothing. It is the one that throws if the set was left empty.
        setTrustAnchors(anchors);
        this.unmodInitialPolicies = Collections.emptySet();
        this.certPathCheckers = new ArrayList<PKIXCertPathChecker>();
        this.certStores = new ArrayList<CertStore>();
    }

    // An immutable copy of the anchors. The copy is defensive in both directions: whoever passed
    // them cannot take one out afterwards, and whoever receives them cannot add one.
    public Set<TrustAnchor> getTrustAnchors() {
        return this.unmodTrustAnchors;
    }

    public void setTrustAnchors(Set<TrustAnchor> trustAnchors)
            throws InvalidAlgorithmParameterException {
        if (trustAnchors == null) {
            throw new NullPointerException("the trustAnchors parameters must"
                + " be non-null");
        }
        if (trustAnchors.isEmpty()) {
            throw new InvalidAlgorithmParameterException("the trustAnchors "
                + "parameter must be non-empty");
        }
        // The type check is explicit because the `Set` can come raw: without this, an element that
        // is not a `TrustAnchor` would not blow up until the middle of the validation.
        Iterator<TrustAnchor> it = trustAnchors.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (!(o instanceof TrustAnchor)) {
                throw new ClassCastException("all elements of set must be "
                    + "of type java.security.cert.TrustAnchor");
            }
        }
        this.unmodTrustAnchors = Collections.unmodifiableSet(
            new HashSet<TrustAnchor>(trustAnchors));
    }

    // The OIDs of the policies the caller accepts. Empty means **any**, not none.
    public Set<String> getInitialPolicies() {
        return this.unmodInitialPolicies;
    }

    public void setInitialPolicies(Set<String> initialPolicies) {
        if (initialPolicies != null) {
            Iterator<String> it = initialPolicies.iterator();
            while (it.hasNext()) {
                Object o = it.next();
                if (!(o instanceof String)) {
                    throw new ClassCastException("all elements of set must be "
                        + "of type java.lang.String");
                }
            }
            this.unmodInitialPolicies = Collections.unmodifiableSet(
                new HashSet<String>(initialPolicies));
        } else {
            this.unmodInitialPolicies = Collections.emptySet();
        }
    }

    // Where to take certificates and CRLs that did not come in the path from. Null clears the list.
    public void setCertStores(List<CertStore> stores) {
        if (stores == null) {
            this.certStores = new ArrayList<CertStore>();
        } else {
            List<CertStore> copyOf = new ArrayList<CertStore>(stores);
            Iterator<CertStore> it = copyOf.iterator();
            while (it.hasNext()) {
                Object o = it.next();
                if (!(o instanceof CertStore)) {
                    throw new ClassCastException("all elements of list must be "
                        + "of type java.security.cert.CertStore");
                }
            }
            this.certStores = copyOf;
        }
    }

    public void addCertStore(CertStore store) {
        if (store != null) {
            this.certStores.add(store);
        }
    }

    public List<CertStore> getCertStores() {
        return Collections.unmodifiableList(new ArrayList<CertStore>(this.certStores));
    }

    // Whether revocation is checked. **It starts at true and turning it off is a security
    // decision**: a revoked certificate validates all the same from there on.
    public void setRevocationEnabled(boolean val) {
        this.revocationEnabled = val;
    }

    public boolean isRevocationEnabled() {
        return this.revocationEnabled;
    }

    // Whether the whole chain is required to sustain some explicit policy.
    public void setExplicitPolicyRequired(boolean val) {
        this.explicitPolicyRequired = val;
    }

    public boolean isExplicitPolicyRequired() {
        return this.explicitPolicyRequired;
    }

    // Whether the mapping of policies between different domains is forbidden.
    public void setPolicyMappingInhibited(boolean val) {
        this.policyMappingInhibited = val;
    }

    public boolean isPolicyMappingInhibited() {
        return this.policyMappingInhibited;
    }

    // Whether anyPolicy (2.5.29.32.0) is forbidden to satisfy the policy requirement. With
    // anyPolicy permitted, "any policy serves" and the whole mechanism stops restricting anything.
    public void setAnyPolicyInhibited(boolean val) {
        this.anyPolicyInhibited = val;
    }

    public boolean isAnyPolicyInhibited() {
        return this.anyPolicyInhibited;
    }

    // Whether a certificate with policy qualifiers the validator does not process is rejected. It
    // starts at true, which is the safe side.
    public void setPolicyQualifiersRejected(boolean qualifiersRejected) {
        this.policyQualifiersRejected = qualifiersRejected;
    }

    public boolean getPolicyQualifiersRejected() {
        return this.policyQualifiersRejected;
    }

    // The date the validation is made against, or null for "now". It is copied in both directions
    // because `Date` is mutable: without copying, whoever passed it could move it afterwards and
    // shift the moment the currency of the chain is checked against.
    //
    // The copy is made with `new Date(getTime())` and not with `clone()` because the
    // `java.util.Date` of this library does not implement `Cloneable` yet. The effect is the same.
    public Date getDate() {
        if (this.date == null) {
            return null;
        }
        return new Date(this.date.getTime());
    }

    public void setDate(Date date) {
        if (date != null) {
            this.date = new Date(date.getTime());
        } else {
            this.date = null;
        }
    }

    // The extra checkers. The list is copied and **so is each checker**: they have mutable state,
    // and sharing them would make two concurrent validations step on each other.
    public void setCertPathCheckers(List<PKIXCertPathChecker> checkers) {
        if (checkers != null) {
            List<PKIXCertPathChecker> copyOf = new ArrayList<PKIXCertPathChecker>();
            Iterator<PKIXCertPathChecker> it = checkers.iterator();
            while (it.hasNext()) {
                copyOf.add((PKIXCertPathChecker) it.next().clone());
            }
            this.certPathCheckers = copyOf;
        } else {
            this.certPathCheckers = new ArrayList<PKIXCertPathChecker>();
        }
    }

    public List<PKIXCertPathChecker> getCertPathCheckers() {
        List<PKIXCertPathChecker> copyOf = new ArrayList<PKIXCertPathChecker>();
        Iterator<PKIXCertPathChecker> it = this.certPathCheckers.iterator();
        while (it.hasNext()) {
            copyOf.add((PKIXCertPathChecker) it.next().clone());
        }
        return Collections.unmodifiableList(copyOf);
    }

    public void addCertPathChecker(PKIXCertPathChecker checker) {
        if (checker != null) {
            this.certPathCheckers.add((PKIXCertPathChecker) checker.clone());
        }
    }

    // The provider to verify the signatures with, or null for whichever is found.
    public String getSigProvider() {
        return this.sigProvider;
    }

    public void setSigProvider(String sigProvider) {
        this.sigProvider = sigProvider;
    }

    // The criterion the certificate at the end of the path has to meet, or null if none.
    public CertSelector getTargetCertConstraints() {
        if (this.certSelector != null) {
            return (CertSelector) this.certSelector.clone();
        }
        return null;
    }

    public void setTargetCertConstraints(CertSelector selector) {
        if (selector != null) {
            this.certSelector = (CertSelector) selector.clone();
        } else {
            this.certSelector = null;
        }
    }

    // A copy the validator can keep without the caller being able to change it afterwards. The
    // lists are copied; the anchors are immutable already.
    @Override
    public Object clone() {
        try {
            PKIXParameters copyOf = (PKIXParameters) super.clone();
            if (this.certStores != null) {
                copyOf.certStores = new ArrayList<CertStore>(this.certStores);
            }
            if (this.certPathCheckers != null) {
                copyOf.certPathCheckers = new ArrayList<PKIXCertPathChecker>();
                Iterator<PKIXCertPathChecker> it = this.certPathCheckers.iterator();
                while (it.hasNext()) {
                    copyOf.certPathCheckers.add((PKIXCertPathChecker) it.next().clone());
                }
            }
            return copyOf;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        if (this.unmodTrustAnchors != null) {
            sb.append("  Trust Anchors: " + this.unmodTrustAnchors.toString() + "\n");
        }
        if (this.unmodInitialPolicies != null) {
            if (this.unmodInitialPolicies.isEmpty()) {
                sb.append("  Initial Policy OIDs: any\n");
            } else {
                sb.append("  Initial Policy OIDs: ["
                    + this.unmodInitialPolicies.toString() + "]\n");
            }
        }
        sb.append("  Validity Date: " + String.valueOf(this.date) + "\n");
        sb.append("  Signature Provider: " + String.valueOf(this.sigProvider) + "\n");
        sb.append("  Default Revocation Enabled: " + this.revocationEnabled + "\n");
        sb.append("  Explicit Policy Required: " + this.explicitPolicyRequired + "\n");
        sb.append("  Policy Mapping Inhibited: " + this.policyMappingInhibited + "\n");
        sb.append("  Any Policy Inhibited: " + this.anyPolicyInhibited + "\n");
        sb.append("  Policy Qualifiers Rejected: " + this.policyQualifiersRejected + "\n");
        sb.append("  Target Cert Constraints: " + String.valueOf(this.certSelector) + "\n");
        if (this.certPathCheckers != null) {
            sb.append("  Certification Path Checkers: ["
                + this.certPathCheckers.toString() + "]\n");
        }
        if (this.certStores != null) {
            sb.append("  CertStores: [" + this.certStores.toString() + "]\n");
        }
        sb.append("]");
        return sb.toString();
    }
}
