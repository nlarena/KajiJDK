package java.security;

import java.io.Serializable;
import java.security.cert.CertPath;
import java.util.Date;

// A signed timestamp: when something was signed, according to whoever attests it.
//
// It exists because of a concrete problem: certificates expire and are revoked, and without a
// timestamp a signature made when the certificate was valid becomes indistinguishable from one made
// afterwards. With the stamp, the question becomes "was the certificate valid **at that moment**?",
// which can be answered years later.
//
// That the date comes accompanied by a `CertPath` is what makes it useful: a date alone can be
// written by anybody. The path is that of the time stamping authority that signed it. This class
// **keeps** the two data and verifies neither — verifying the signature of the stamping requires
// cryptography this library does not have.
public final class Timestamp implements Serializable {

    private final Date timestamp;
    private final CertPath signerCertPath;

    public Timestamp(Date timestamp, CertPath signerCertPath) {
        if (timestamp == null || signerCertPath == null) {
            throw new NullPointerException();
        }
        // It is copied: `Date` is mutable, and a timestamp the caller can move after building it is
        // of no use at all.
        this.timestamp = new Date(timestamp.getTime());
        this.signerCertPath = signerCertPath;
    }

    public Date getTimestamp() {
        return new Date(this.timestamp.getTime());
    }

    public CertPath getSignerCertPath() {
        return this.signerCertPath;
    }

    @Override
    public int hashCode() {
        return this.timestamp.hashCode() * 31 + this.signerCertPath.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Timestamp)) {
            return false;
        }
        Timestamp that = (Timestamp) obj;
        return this.timestamp.equals(that.timestamp)
            && this.signerCertPath.equals(that.signerCertPath);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("(");
        b.append("timestamp: ");
        b.append(this.timestamp);
        java.util.List<? extends java.security.cert.Certificate> cs =
            this.signerCertPath.getCertificates();
        if (!cs.isEmpty()) {
            b.append("TSA: ");
            java.security.cert.Certificate c0 = cs.get(0);
            b.append(c0.toString());
        } else {
            b.append("TSA: <empty>");
        }
        b.append(")");
        return b.toString();
    }
}
