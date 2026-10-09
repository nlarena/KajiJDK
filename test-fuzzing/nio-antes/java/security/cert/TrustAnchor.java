package java.security.cert;

import java.security.PublicKey;

// A trust anchor: where a chain ends and faith begins.
//
// It is the most important object of the whole validation, and it is worth saying precisely why. A
// chain of certificates does not validate "by itself": each certificate is verified with the key of
// the next, and that recursion has to stop at something that is accepted **without verifying**.
// That is the anchor. All the security of PKIX rests on the set of anchors being the right one; one
// CA too many in that list can sign a certificate for any name and the validation will say yes.
//
// An anchor need not be a certificate. The name of the CA and its public key are enough, and in
// fact that is the most honest: the only thing needed in order to close the chain is the key. That
// the JDK lets an `X509Certificate` through is a convenience —the name and the key are taken out of
// it— and does not mean that that certificate is validated: **the anchor is never verified**, and
// its date is not looked at either.
//
// The three constructors say the same thing in three ways, and the order in which their arguments
// are looked at matters: the one that takes a `String` **parses** that name and rejects it if it is
// wrong, so no anchor is left with a name nobody validated.
//
// A detail of the JDK that is reproduced on purpose and that surprises: `getCAName()` does not
// always return the same as `getCA().getName()`. When the anchor was created from a `String`,
// **that text is kept as it is** —spaces included—; when it was created from an `X500Principal`,
// its `getName()` is kept, which is RFC 2253 without spaces. Comparing anchors by `getCAName()` is
// therefore a mistake; they have to be compared by `getCA()`, which does have a canonical form.
//
// A difference of behaviour noted on purpose: the JDK **validates** the DER of `nameConstraints`
// and throws `IllegalArgumentException` if it is badly formed. Here there is no parser of
// `NameConstraints`, so the bytes are kept without being looked at. It is not a decision of trust
// —this class does not apply the constraints, it only carries them— but it is an observable
// difference and it is written down.
public class TrustAnchor {

    private final X509Certificate trustedCert;
    private final PublicKey pubKey;
    private final byte[] ncBytes;
    private final javax.security.auth.x500.X500Principal caPrincipal;
    private final String caName;

    // An anchor from the self-signed certificate of the CA.
    //
    // `nameConstraints` may be null. When it is not, they are the name constraints imposed on
    // **this anchor from outside**, without appearing in its certificate: it serves for bounding a
    // CA to a subdomain even though it has not bounded itself.
    public TrustAnchor(X509Certificate trustedCert, byte[] nameConstraints) {
        if (trustedCert == null) {
            throw new NullPointerException("the trustedCert parameter must be non-null");
        }
        this.trustedCert = trustedCert;
        this.pubKey = null;
        // They are not derived from the certificate, and that is of the JDK: an anchor created this
        // way returns null in both accessors. Whoever wants the name takes it from
        // `getTrustedCert().getSubjectX500Principal()`, which is the subject —the anchor is the CA,
        // not the issuer of its own certificate—.
        this.caPrincipal = null;
        this.caName = null;
        this.ncBytes = copy(nameConstraints);
    }

    // An anchor from the name of the CA and its public key, with no certificate.
    //
    // It is the most honest of the three forms: the only thing needed in order to close a chain is
    // knowing whom to believe and with which key. The certificate of the CA contributes nothing
    // more —it is not verified and its date is not looked at—.
    public TrustAnchor(javax.security.auth.x500.X500Principal caPrincipal, PublicKey pubKey,
            byte[] nameConstraints) {
        if (caPrincipal == null || pubKey == null) {
            throw new NullPointerException();
        }
        this.trustedCert = null;
        this.caPrincipal = caPrincipal;
        this.caName = caPrincipal.getName();
        this.pubKey = pubKey;
        this.ncBytes = copy(nameConstraints);
    }

    // The same, with the name of the CA in RFC 2253.
    //
    // The name is parsed right here and both forms are kept. The order of the checks is the JDK's
    // and it shows: with both arguments null, the one that complains is `pubKey`, not `caName`.
    public TrustAnchor(String caName, PublicKey pubKey, byte[] nameConstraints) {
        if (pubKey == null) {
            throw new NullPointerException("the pubKey parameter must be non-null");
        }
        if (caName == null) {
            throw new NullPointerException("the caName parameter must be non-null");
        }
        if (caName.length() == 0) {
            throw new IllegalArgumentException("the caName "
                + "parameter must be a non-empty String");
        }
        // A badly formed name throws `IllegalArgumentException` from here: it is the constructor of
        // `X500Principal` that rejects it, and letting it through would be the anchor without
        // validation.
        this.caPrincipal = new javax.security.auth.x500.X500Principal(caName);
        this.pubKey = pubKey;
        // The **original** text is kept, not the canonical one. See the comment of the class.
        this.caName = caName;
        this.trustedCert = null;
        this.ncBytes = copy(nameConstraints);
    }

    private static byte[] copy(byte[] b) {
        if (b == null) {
            return null;
        }
        byte[] c = new byte[b.length];
        System.arraycopy(b, 0, c, 0, b.length);
        return c;
    }

    // The certificate of the trusted CA, or null if the anchor was created from a name and a key.
    // `final` in every accessor: a subclass that changed what they return would move the point at
    // which the chain stops being verified.
    public final X509Certificate getTrustedCert() {
        return this.trustedCert;
    }

    // The public key of the CA, or null if the anchor was created from a certificate —there the key
    // comes out of the certificate, not from here—.
    public final PublicKey getCAPublicKey() {
        return this.pubKey;
    }

    // The name of the CA, or null if the anchor was created from a certificate.
    public final javax.security.auth.x500.X500Principal getCA() {
        return this.caPrincipal;
    }

    // The name of the CA as text, or null if the anchor was created from a certificate.
    //
    // It is **not** necessarily `getCA().getName()`: see the comment of the class. To compare
    // anchors `getCA()` has to be used.
    public final String getCAName() {
        return this.caName;
    }

    // A copy of the name constraints in DER, or null if there are none.
    public final byte[] getNameConstraints() {
        return copy(this.ncBytes);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        if (this.pubKey != null) {
            sb.append("  Trusted CA Public Key: " + this.pubKey.toString() + "\n");
            sb.append("  Trusted CA Issuer Name: " + String.valueOf(this.caName) + "\n");
        } else {
            sb.append("  Trusted CA cert: " + this.trustedCert.toString() + "\n");
        }
        if (this.ncBytes != null) {
            sb.append("  Name Constraints: " + this.ncBytes.length + " bytes\n");
        }
        return sb.toString();
    }
}
