package java.security.cert;

// The failure reasons of PKIX's own (RFC 5280), the ones that make no sense outside it.
//
// They complement `BasicReason`, which covers what holds for any PKI. That they are two different
// enums implementing the same interface is exactly the point of the design of `Reason`: the list is
// extended without touching the exception.
public enum PKIXReason implements CertPathValidatorException.Reason {

    // The issuer of a certificate does not match the subject of the next: the chain is cut.
    NAME_CHAINING,

    // The certificate was used for something its KeyUsage extension does not allow. A server
    // certificate signing other certificates lands here.
    INVALID_KEY_USAGE,

    // The policy of the certificate is not acceptable according to the validation parameters.
    INVALID_POLICY,

    // The chain does not end in any known trust anchor: the "I do not know who signed this".
    NO_TRUST_ANCHOR,

    // There is an extension marked critical that the validator does not understand. Rejecting is
    // compulsory and not optional: "critical" means exactly "if you do not understand this, do not
    // accept".
    UNRECOGNIZED_CRIT_EXT,

    // A certificate of the chain signed another without being a CA.
    NOT_CA_CERT,

    // The chain is longer than the length constraint of some CA allows.
    PATH_TOO_LONG,

    // A name violates the name constraints of some CA of the chain.
    INVALID_NAME
}
