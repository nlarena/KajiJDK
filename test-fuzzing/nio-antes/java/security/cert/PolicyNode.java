package java.security.cert;

import java.util.Iterator;
import java.util.Set;

// A node of the policy tree the PKIX validation produces.
//
// Certificate policies are the part of PKIX hardly anybody uses, and it is worth saying what they
// are for: a CA can declare under which rules it issued a certificate —how much verification of
// identity it did, for example— and whoever validates can demand that the whole chain sustain a
// given policy from end to end. The tree is the result of that sum: each level corresponds to a
// certificate of the path, and the branches that survive are the policies that hold for the whole
// chain.
//
// Every method is read-only and the tree is built by the validator: there is no way of assembling
// it by hand from the public API, and that is intentional.
public interface PolicyNode {

    // The parent node, or null if it is the root.
    PolicyNode getParent();

    // The children. The iterator is immutable.
    Iterator<? extends PolicyNode> getChildren();

    // The depth: 0 at the root, and it coincides with the position in the path.
    int getDepth();

    // The OID of the policy this node represents.
    String getValidPolicy();

    // The associated qualifiers: legal text, URLs of the declaration of practices of the CA.
    Set<? extends PolicyQualifierInfo> getPolicyQualifiers();

    // The OIDs a child could have in order to follow this branch.
    Set<String> getExpectedPolicies();

    // Whether the policy extension of the certificate of this level came marked critical.
    boolean isCritical();
}
