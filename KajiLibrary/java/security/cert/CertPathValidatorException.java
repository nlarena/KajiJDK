package java.security.cert;

import java.io.Serializable;
import java.security.GeneralSecurityException;

// The validation of a certification path failed, and **where** and **why**.
//
// It is the most informative exception of the package and with reason: when a TLS chain does not
// validate, knowing that it failed is not enough —one has to know in which link and whether it was
// because of the date, the signature or the revocation. That is why it carries the path, the index
// of the certificate that caused it and a reason.
//
// The reason is an **interface** and not a closed enum, and that is the design decision that
// matters: `BasicReason` covers what holds for any PKI, `PKIXReason` adds what is specific to PKIX,
// and a validator of another type can contribute its own without this class having to be touched.
// An enum would have frozen the list in JDK 1.5.
//
// The index is -1 when it is not known which certificate was the guilty one, and that is the only
// legal way of saying it: the real indices are positions inside the path, and the constructor
// checks it.
public class CertPathValidatorException extends GeneralSecurityException {

    private static final long serialVersionUID = -3083180014971893139L;

    // The reason it failed. It is `Serializable` because it travels inside the exception, which is
    // too; it declares no method because its only purpose is to be an identifiable constant.
    public interface Reason extends Serializable {
    }

    // The reasons that hold for any PKI, not only PKIX.
    public enum BasicReason implements Reason {

        // It failed, but it is not known why. It is the default of every constructor that does not
        // receive a reason: saying "I do not know" is right, inventing a reason is not.
        UNSPECIFIED,

        // The certificate expired.
        EXPIRED,

        // The certificate has not come into force yet.
        NOT_YET_VALID,

        // The issuer revoked it.
        REVOKED,

        // It could not be found out whether it is revoked. It is **different** from "it is not
        // revoked" and confusing the two is the classic mistake: accepting a certificate whose
        // state could not be consulted is exactly what an attacker who blocks the OCSP wants to
        // happen.
        UNDETERMINED_REVOCATION_STATUS,

        // The signature does not validate.
        INVALID_SIGNATURE,

        // The algorithm is forbidden by policy: it is not that the signature is wrong, it is that
        // that algorithm is no longer accepted.
        ALGORITHM_CONSTRAINED
    }

    private final CertPath certPath;
    private final int index;
    private final Reason reason;

    public CertPathValidatorException() {
        this((String) null, null);
    }

    public CertPathValidatorException(String msg) {
        this(msg, null);
    }

    // It takes the message from the cause, just like the rest of the hierarchy.
    public CertPathValidatorException(Throwable cause) {
        this((cause == null ? null : cause.toString()), cause);
    }

    public CertPathValidatorException(String msg, Throwable cause) {
        this(msg, cause, null, -1);
    }

    public CertPathValidatorException(String msg, Throwable cause, CertPath certPath, int index) {
        this(msg, cause, certPath, index, BasicReason.UNSPECIFIED);
    }

    public CertPathValidatorException(String msg, Throwable cause, CertPath certPath, int index,
                                      Reason reason) {
        super(msg, cause);
        // An index with no path points at nothing: if there is no path the index has to be -1.
        if (certPath == null && index != -1) {
            throw new IllegalArgumentException();
        }
        if (index < -1 || (certPath != null && index >= certPath.getCertificates().size())) {
            throw new IndexOutOfBoundsException();
        }
        if (reason == null) {
            throw new NullPointerException("reason can't be null");
        }
        this.certPath = certPath;
        this.index = index;
        this.reason = reason;
    }

    // The path that failed, or null if it was not given.
    public CertPath getCertPath() {
        return this.certPath;
    }

    // The position of the guilty certificate inside the path, or -1 if it is not known.
    public int getIndex() {
        return this.index;
    }

    // Never null: in the worst case it is `BasicReason.UNSPECIFIED`.
    public Reason getReason() {
        return this.reason;
    }
}
