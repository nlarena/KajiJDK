package java.security.cert;

// The result of building a path: the path that was found.
//
// Building includes validating —a builder that returned unverified chains would be of no use— and
// that is why `PKIXCertPathBuilderResult` extends the validation result instead of being a separate
// type.
public interface CertPathBuilderResult extends Cloneable {

    // The built path.
    CertPath getCertPath();

    Object clone();
}
