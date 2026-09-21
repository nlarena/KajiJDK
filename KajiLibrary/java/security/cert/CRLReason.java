package java.security.cert;

// Why a certificate was revoked, according to the `CRLReason` code of RFC 5280.
//
// The order **is** the contract: the values are encoded in the CRL as their ordinal number, so
// moving one changes the meaning of the CRLs already issued. That is why `UNUSED` is in the middle
// and is not taken out: it is code 7, which was reserved and never came to be used.
//
// The distinction that matters most in practice is `KEY_COMPROMISE` against the rest: only that one
// says that the private key leaked, and therefore only that one invalidates backwards the
// signatures made before the revocation. With `SUPERSEDED` or `CESSATION_OF_OPERATION`, what was
// signed before goes on being worth something.
public enum CRLReason {

    // It was revoked without saying why.
    UNSPECIFIED,

    // The private key of the subject leaked. The only reason that invalidates backwards.
    KEY_COMPROMISE,

    // The private key of the CA leaked: everything that CA issued falls.
    CA_COMPROMISE,

    // Something of the subject changed —name, organisation— with no suspicion about the key.
    AFFILIATION_CHANGED,

    // There is a new certificate that replaces it.
    SUPERSEDED,

    // The subject stopped operating.
    CESSATION_OF_OPERATION,

    // A temporary suspension: it can become valid again. The only reversible reason.
    CERTIFICATE_HOLD,

    // Code 7, reserved and never used. It is here only so that the following ordinals land
    // where the RFC says they land.
    UNUSED,

    // It is taken out of the CRL: it only appears in delta CRLs, to lift a CERTIFICATE_HOLD.
    REMOVE_FROM_CRL,

    // A privilege was withdrawn from the subject.
    PRIVILEGE_WITHDRAWN,

    // The key of an attribute authority leaked.
    AA_COMPROMISE
}
