package java.security.cert;

import java.math.BigInteger;
import java.security.DEREncodable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Date;
import java.util.Set;

// An X.509 revocation list (RFC 5280): which certificates of this issuer stopped being valid.
//
// A CRL is **a signed object**, just like a certificate, and that is why it has `verify`. That is
// not a detail: an unverified CRL is a list anybody could have written, and accepting it has the
// opposite effect to the one sought —an attacker who can inject false CRLs can revoke legitimate
// certificates, or hand over an old CRL that does not yet list the certificate they stole—. The
// contract of `verify` is the same as in `Certificate`: **it returns nothing, it throws if it
// fails**.
//
// The two dates are the other sensitive point. `thisUpdate` says when the photograph is from and
// `nextUpdate` when the next one is promised; a CRL whose `nextUpdate` has passed is an expired CRL
// and using it is like checking nothing. That `getNextUpdate()` can return null —the field is
// optional— makes that check easy to forget.
//
// `getRevokedCertificate(X509Certificate)` is the one to use, and not the one that takes only the
// serial: **it compares the issuers first**. Two different CAs can issue the same serial, so
// looking for a serial in the wrong CRL is faster and wrong. The one that takes a `BigInteger` goes
// on existing because the JDK has it, but there the caller takes responsibility for having checked
// the issuer.
public abstract class X509CRL extends CRL implements X509Extension, DEREncodable {

    // It is remembered for the same reason as in `X509Certificate`: parsing at every call would be
    // expensive and the JDK returns the same instance twice in a row.
    private javax.security.auth.x500.X500Principal issuerX500;

    protected X509CRL() {
        super("X.509");
    }

    // The issuer of the CRL as an X.500 name.
    //
    // It is read from the DER —`TBSCertList.issuer`— and not from `getIssuerDN()`, for the same
    // reason as in `X509Certificate`: going through the text of just any `Principal` and reparsing
    // it is the confusion of names this method exists to avoid. `RuntimeException` if the DER
    // cannot be read, just as in the JDK.
    public javax.security.auth.x500.X500Principal getIssuerX500Principal() {
        if (this.issuerX500 == null) {
            try {
                this.issuerX500 = new javax.security.auth.x500.X500Principal(
                    DerReader.crlName(this.getEncoded()));
            } catch (CRLException e) {
                throw new RuntimeException("Could not parse issuer");
            } catch (java.io.IOException e) {
                throw new RuntimeException("Could not parse issuer");
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Could not parse issuer");
            }
        }
        return this.issuerX500;
    }

    // The entry of this certificate, or null if this CRL does not revoke it.
    //
    // The issuer check goes **before** looking at the serial, and if it does not match null is
    // returned without getting as far as searching: it is not an optimisation, it is that the right
    // answer there is "this CRL does not talk about this certificate". An indirect CRL —one that
    // revokes certificates of several CAs— is not contemplated, just as in the JDK: for that one
    // would have to look at the `certificateIssuer` of each entry, and that field is an extension
    // this class does not decode.
    public X509CRLEntry getRevokedCertificate(X509Certificate certificate) {
        javax.security.auth.x500.X500Principal certIssuer = certificate.getIssuerX500Principal();
        if (!certIssuer.equals(this.getIssuerX500Principal())) {
            return null;
        }
        return this.getRevokedCertificate(certificate.getSerialNumber());
    }

    // Equality by encoding, just as in `Certificate`.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof X509CRL)) {
            return false;
        }
        try {
            byte[] a = this.getEncoded();
            byte[] b = ((X509CRL) other).getEncoded();
            if (a.length != b.length) {
                return false;
            }
            int i = 0;
            while (i < a.length) {
                if (a[i] != b[i]) {
                    return false;
                }
                i = i + 1;
            }
            return true;
        } catch (CRLException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int h = 0;
        try {
            byte[] a = this.getEncoded();
            int i = 0;
            while (i < a.length) {
                h = h * 31 + a[i];
                i = i + 1;
            }
        } catch (CRLException e) {
            return 0;
        }
        return h;
    }

    public abstract byte[] getEncoded() throws CRLException;

    // It verifies the signature of the CRL. If it does not throw, the signature is valid.
    public abstract void verify(PublicKey key)
        throws CRLException, NoSuchAlgorithmException, InvalidKeyException,
               NoSuchProviderException, SignatureException;

    public abstract void verify(PublicKey key, String sigProvider)
        throws CRLException, NoSuchAlgorithmException, InvalidKeyException,
               NoSuchProviderException, SignatureException;

    // Just as in `Certificate`: the variant with a `Provider` arrived afterwards and its base
    // implementation throws so as not to force the subclasses that already existed. Inventing a
    // verification here would be the hole.
    public void verify(PublicKey key, Provider sigProvider)
            throws CRLException, NoSuchAlgorithmException, InvalidKeyException,
                   SignatureException {
        throw new UnsupportedOperationException();
    }

    // The version: 1 or 2. Only the v2 ones have extensions, and therefore only they can be delta
    // or indirect.
    public abstract int getVersion();

    // Who signed the CRL. See the note of the class about why the modern variant is not there.
    public abstract Principal getIssuerDN();

    // When this photograph is from.
    public abstract Date getThisUpdate();

    // When the next one is promised, or null if it does not say. If it has passed, the CRL is
    // expired.
    public abstract Date getNextUpdate();

    // The entry of that serial, or null if it is not revoked. The caller has to have checked that
    // the issuer of the CRL is that of the certificate: this class cannot do it for them.
    public abstract X509CRLEntry getRevokedCertificate(BigInteger serialNumber);

    // Every entry, or null if the CRL is empty. **Null and not an empty set**: it is what the JDK
    // does and confusing them is an NPE waiting.
    public abstract Set<? extends X509CRLEntry> getRevokedCertificates();

    // The signed part of the CRL.
    public abstract byte[] getTBSCertList() throws CRLException;

    public abstract byte[] getSignature();

    public abstract String getSigAlgName();

    public abstract String getSigAlgOID();

    public abstract byte[] getSigAlgParams();
}
