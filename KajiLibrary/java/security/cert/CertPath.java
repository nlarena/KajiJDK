package java.security.cert;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

// A chain of certificates: from the one of interest upwards, in order.
//
// The order is part of the type and not a convention: the first is the certificate of the subject,
// each one is signed by the next, and the last is usually the one that is compared against a trust
// anchor. An unordered list would be a set of certificates, not a path, and validating a path is
// exactly following that chain.
//
// Just like `Certificate`, the equality is by contents —the same type and the same list— and
// everything that requires cryptography is abstract. This class validates nothing; validating is
// the work of `CertPathValidator`, which does not exist in this library.
public abstract class CertPath implements Serializable {

    private final String type;

    protected CertPath(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    // The names of the supported encodings, with the preferred one first.
    public abstract Iterator<String> getEncodings();

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CertPath)) {
            return false;
        }
        CertPath o = (CertPath) other;
        if (!this.type.equals(o.getType())) {
            return false;
        }
        return this.getCertificates().equals(o.getCertificates());
    }

    @Override
    public int hashCode() {
        return this.type.hashCode() * 31 + this.getCertificates().hashCode();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(this.type);
        b.append(" Cert Path: length = ");
        List<? extends Certificate> cs = this.getCertificates();
        b.append(cs.size());
        b.append(".\n[\n");
        int i = 0;
        while (i < cs.size()) {
            Certificate c = cs.get(i);
            b.append("=========================================================Certificate ");
            b.append(i + 1);
            b.append("start.\n");
            b.append(c.toString());
            b.append("\n=========================================================Certificate ");
            b.append(i + 1);
            b.append("end.\n\n\n");
            i = i + 1;
        }
        b.append("\n]");
        return b.toString();
    }

    public abstract byte[] getEncoded() throws CertificateEncodingException;

    public abstract byte[] getEncoded(String encoding) throws CertificateEncodingException;

    // The certificates, from the subject towards the root. Immutable.
    public abstract List<? extends Certificate> getCertificates();

    // A KajiLibrary subset, for the same reason as in `Certificate`: without `CertificateFactory`
    // there is no way of rebuilding the path when deserialising.
    protected Object writeReplace() throws ObjectStreamException {
        throw new java.io.NotSerializableException(
            "java.security.cert.CertPath: no CertificateFactory available to restore it");
    }
}
