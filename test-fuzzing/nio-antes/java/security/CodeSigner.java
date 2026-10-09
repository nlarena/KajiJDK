package java.security;

import java.io.Serializable;
import java.security.cert.CertPath;

// Who signed: their chain of certificates and, if there was one, the timestamp of the signature.
//
// The timestamp is optional and that optionality matters. Without it the signature is only worth
// something while the signer's certificate is still in force; with it it goes on being worth
// something afterwards, because it can be checked that at the moment of signing the certificate was
// fine. That is why `getTimestamp()` returning null is not a detail: it is a signature with an
// expiry date.
//
// Just like `Timestamp`, this class keeps and compares; it verifies nothing.
public final class CodeSigner implements Serializable {

    private final CertPath signerCertPath;
    private final Timestamp timestamp;

    public CodeSigner(CertPath signerCertPath, Timestamp timestamp) {
        if (signerCertPath == null) {
            throw new NullPointerException();
        }
        this.signerCertPath = signerCertPath;
        this.timestamp = timestamp;
    }

    public CertPath getSignerCertPath() {
        return this.signerCertPath;
    }

    // The timestamp, or null if the signature was not stamped.
    public Timestamp getTimestamp() {
        return this.timestamp;
    }

    @Override
    public int hashCode() {
        if (this.timestamp == null) {
            return this.signerCertPath.hashCode();
        }
        return this.signerCertPath.hashCode() ^ this.timestamp.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CodeSigner)) {
            return false;
        }
        CodeSigner that = (CodeSigner) obj;
        if (!this.signerCertPath.equals(that.signerCertPath)) {
            return false;
        }
        // Signed without a stamp and signed with a stamp are different things, even if the signer
        // is the same: only one of the two survives the expiry of the certificate.
        if (this.timestamp == null) {
            return that.timestamp == null;
        }
        return this.timestamp.equals(that.timestamp);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("(");
        java.util.List<? extends java.security.cert.Certificate> cs =
            this.signerCertPath.getCertificates();
        b.append("Signer: ");
        if (!cs.isEmpty()) {
            java.security.cert.Certificate c0 = cs.get(0);
            b.append(c0.toString());
        } else {
            b.append("<empty>");
        }
        if (this.timestamp != null) {
            b.append("timestamp: ");
            b.append(this.timestamp.toString());
        }
        b.append(")");
        return b.toString();
    }
}
