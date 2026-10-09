package java.security.cert;

// A criterion for choosing certificates of a `CertStore`.
//
// The counterpart of `CRLSelector`, and it redeclares `clone()` for the same reason: the selector
// is mutable state the store appropriates, and it has to be able to copy it.
public interface CertSelector extends Cloneable {

    boolean match(Certificate cert);

    Object clone();
}
