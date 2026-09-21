package java.security.cert;

// The result of validating a path, when the validation went well.
//
// That there is a result and not a boolean is the important thing: a successful validation produces
// data that are needed afterwards —the anchor it ended up trusting, the public key of the subject—
// and throwing them away would force them to be recomputed. A **failed** validation does not return
// this: it throws `CertPathValidatorException`.
public interface CertPathValidatorResult extends Cloneable {

    Object clone();
}
