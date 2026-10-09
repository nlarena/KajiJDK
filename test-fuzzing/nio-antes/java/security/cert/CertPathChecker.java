package java.security.cert;

// A check that is applied to each certificate of a path, one at a time.
//
// It is the extension mechanism of the validation: whoever needs a rule PKIX does not bring —an
// internal policy, a list of one's own— writes it here instead of reimplementing the whole
// validation.
//
// `init(boolean forward)` is not a detail. A path can be walked from the subject towards the root
// or the other way round, and there are checks that only make sense in one direction —the ones that
// need to know who signed before looking at the signed one, for example—. That is why the checker
// declares with `isForwardCheckingSupported()` what it knows how to do, and `init` tells it which
// one it got this time.
public interface CertPathChecker {

    // It prepares the checker and tells it in which direction the walk is going to be. It is called
    // before the first check, and it also serves for resetting the state between uses.
    void init(boolean forward) throws CertPathValidatorException;

    // Whether it knows how to check in the subject -> root direction.
    boolean isForwardCheckingSupported();

    // It checks a certificate. **With no return value**: if it does not throw, it passed. It is the
    // same contract as `Certificate.verify`, and it brings the same risk: an empty `catch` here
    // accepts anything.
    void check(Certificate cert) throws CertPathValidatorException;
}
