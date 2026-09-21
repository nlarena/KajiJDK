package java.security.cert;

import java.util.Collection;
import java.util.Set;

// A `CertPathChecker` with what PKIX adds: knowing which extensions it can process.
//
// The second thing is what justifies the class. During the validation a count is kept of which
// critical extensions were left unprocessed; if at the end of the certificate any is left, it has
// to be rejected. That is why `check` receives the set of pending OIDs and the checker **takes out
// of it** the ones it attended: it is how it tells the validator that that extension is no longer a
// reason for rejection. A checker that processes a critical extension and forgets to take it out
// makes the validation fail; one that takes out one it did not process dismantles the guarantee of
// the critical extensions.
public abstract class PKIXCertPathChecker implements CertPathChecker, Cloneable {

    protected PKIXCertPathChecker() {
    }

    public abstract void init(boolean forward) throws CertPathValidatorException;

    public abstract boolean isForwardCheckingSupported();

    // The OIDs of the extensions this checker knows how to process, or null if none.
    public abstract Set<String> getSupportedExtensions();

    // It checks the certificate and takes out of `unresolvedCritExts` the OIDs it attended.
    public abstract void check(Certificate cert, Collection<String> unresolvedCritExts)
        throws CertPathValidatorException;

    // The version with no set, which comes from `CertPathChecker`. It passes an **immutable** empty
    // set, and that choice shows: a checker that tries to take an OID out of there blows up with
    // `UnsupportedOperationException` instead of failing silently. It is right, because calling
    // this version means that nobody is keeping the count of the critical extensions, and a checker
    // that depends on that count has to find out.
    @Override
    public void check(Certificate cert) throws CertPathValidatorException {
        this.check(cert, java.util.Collections.<String>emptySet());
    }

    // A shallow copy. A checker with mutable state —almost all of them have it, because they
    // accumulate along the path— has to override it: if not, two concurrent validations step on
    // each other.
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }
}
