package java.security;

import java.io.Serializable;
import java.util.ArrayList;

// An identity of the **old** key management system, obsolete since 1.2.
//
// It was replaced by `KeyStore` plus `java.security.cert`, and for good reasons: this API mixed in
// a single object the name, the public key, the certificates that back it and the scope where it
// lives, with an odd notion of equality —two identities are equal if they have the same full name
// **or** the same short name and the same key— that is easy to read wrongly.
//
// It is implemented because it is still in the signatures of the JDK and because its structural
// part is entirely honest: there is not a single cryptographic operation in this class. What it
// does have is an **invariant worth having**: the public key and the certificates cannot contradict
// each other. `addCertificate` rejects a certificate whose key is not the identity's, and
// `setPublicKey` throws the old certificates away instead of leaving them talking about a key that
// is no longer the one. Without that, an identity could assert one key and exhibit certificates of
// another.
@Deprecated
public abstract class Identity implements Principal, Serializable {

    private String name;

    private PublicKey publicKey;

    // Free information about the identity. Package-private in the JDK.
    String info = "No further information available.";

    // The scope it belongs to, or null if it is a top-level one.
    IdentityScope scope;

    private final ArrayList<Certificate> certs = new ArrayList<Certificate>();

    // Only for deserialising. The name is overwritten when reading the stream.
    protected Identity() {
        this("restoring...");
    }

    // An identity inside a scope. It is registered in the scope when it is built: if the scope has
    // an identity with that name or with that key already, the registration fails and this identity
    // does not get as far as existing half-made.
    public Identity(String name, IdentityScope scope) throws KeyManagementException {
        this(name);
        if (scope != null) {
            scope.addIdentity(this);
        }
        this.scope = scope;
    }

    public Identity(String name) {
        this.name = name;
    }

    @Override
    public final String getName() {
        return this.name;
    }

    public final IdentityScope getScope() {
        return this.scope;
    }

    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    // It changes the public key and **erases the certificates**. See the header: a certificate
    // talks about a concrete key, and leaving it after changing the key would turn it into a false
    // assertion.
    public void setPublicKey(PublicKey key) throws KeyManagementException {
        this.publicKey = key;
        this.certs.clear();
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getInfo() {
        return this.info;
    }

    // It adds a certificate. If the identity has a public key already, that of the certificate has
    // to be the same; if it does not have one, it adopts it.
    public void addCertificate(Certificate certificate) throws KeyManagementException {
        if (this.publicKey != null) {
            if (!sameKey(this.publicKey, certificate.getPublicKey())) {
                throw new KeyManagementException("public key different from cert public key");
            }
        } else {
            this.publicKey = certificate.getPublicKey();
        }
        this.certs.add(certificate);
    }

    // It compares two keys by their encoding and not by `equals`.
    //
    // It is on purpose: two different implementations of `PublicKey` that represent the same key
    // are not `equals` to each other —each provider has its class— but they encode the same bytes.
    // If they were compared by identity of object, a certificate issued by another provider would
    // be rejected for no reason.
    private static boolean sameKey(PublicKey a, PublicKey b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }
        byte[] ea = a.getEncoded();
        byte[] eb = b.getEncoded();
        if (ea == null || eb == null) {
            return false;
        }
        return MessageDigest.isEqual(ea, eb);
    }

    public void removeCertificate(Certificate certificate) throws KeyManagementException {
        if (!this.certs.contains(certificate)) {
            throw new KeyManagementException("certificate not registered");
        }
        this.certs.remove(certificate);
    }

    // A copy of the array of certificates.
    public Certificate[] certificates() {
        Certificate[] a = new Certificate[this.certs.size()];
        int i = 0;
        while (i < this.certs.size()) {
            a[i] = this.certs.get(i);
            i = i + 1;
        }
        return a;
    }

    // `final` because the contract of equality of this class is odd and a subclass cannot be
    // allowed to change it: first it tries the full name, and if it does not match it delegates to
    // `identityEquals`, which a subclass **can** refine.
    @Override
    public final boolean equals(Object identity) {
        if (identity == this) {
            return true;
        }
        if (!(identity instanceof Identity)) {
            return false;
        }
        Identity other = (Identity) identity;
        if (this.fullName().equals(other.fullName())) {
            return true;
        }
        return this.identityEquals(other);
    }

    // Equality by short name plus key. A subclass can adjust it; `equals` cannot.
    protected boolean identityEquals(Identity identity) {
        if (!this.name.equalsIgnoreCase(identity.name)) {
            return false;
        }
        if ((this.publicKey == null) != (identity.publicKey == null)) {
            return false;
        }
        if (this.publicKey != null) {
            return this.publicKey.equals(identity.publicKey);
        }
        return true;
    }

    // The name qualified by the scope. Package-private, as in the JDK.
    String fullName() {
        if (this.scope != null) {
            return this.name + "." + this.scope.getName();
        }
        return this.name;
    }

    @Override
    public String toString() {
        if (this.scope != null) {
            return this.name + "[" + this.scope.getName() + "]";
        }
        return this.name;
    }

    // The long form: key, certificates and free information.
    public String toString(boolean detailed) {
        String out = this.toString();
        if (!detailed) {
            return out;
        }
        out = out + "\n";
        out = out + this.printKeys();
        out = out + "\n" + this.printCertificates();
        if (this.info != null) {
            out = out + "\n\t" + this.info;
        } else {
            out = out + "\n\tno additional information available.";
        }
        return out;
    }

    String printKeys() {
        if (this.publicKey != null) {
            return "\tpublic key initialized";
        }
        return "\tno public key";
    }

    String printCertificates() {
        if (this.certs.isEmpty()) {
            return "\tno certificates";
        }
        StringBuilder b = new StringBuilder();
        b.append("\tcertificates: \n");
        int i = 1;
        int k = 0;
        while (k < this.certs.size()) {
            b.append("\tcertificate ");
            b.append(i);
            b.append("\t");
            b.append(this.certs.get(k).toString());
            b.append("\n");
            i = i + 1;
            k = k + 1;
        }
        return b.toString();
    }

    @Override
    public int hashCode() {
        return this.fullName().hashCode();
    }
}
