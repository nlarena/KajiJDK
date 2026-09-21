package java.security.cert;

import java.io.IOException;
import java.math.BigInteger;
import java.security.DEREncodable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// An X.509 v3 certificate (RFC 5280).
//
// ===============================================================================================
// WHY DECLARING IT WHOLE IS HONEST
// ===============================================================================================
//
// This class is **abstract**, and that changes everything. It does not parse a certificate and does
// not verify a signature: it declares **what can be asked** of something that is a certificate
// already. It is a contract, and a complete contract is exactly what is needed for the rest of the
// package —the selectors, the validators, the trust anchors— to be writable without inventing
// anything.
//
// This library brings **no subclass**: there is no DER parser of certificates and no RSA and no
// ECDSA. Whoever wants a real certificate has to bring an implementation. What this class promises
// is only the shape.
//
// ===============================================================================================
// THE X.500 NAMES
// ===============================================================================================
//
// `getSubjectX500Principal()` and `getIssuerX500Principal()` were out for a while, with the
// argument that comparing X.500 names wrongly is how a chain is forged and that there was nowhere
// to do it properly. Now `javax.security.auth.x500.X500Principal` exists —with its DER decoder, its
// RFC 2253 parser and its canonical form, all tested against the JDK—, so the argument fell: the
// risky part lives in a single place and these two methods only use it.
//
// What they do is the same thing the JDK does: parse the bytes of `getEncoded()` as far as the
// corresponding field and build an `X500Principal` with that stretch. `getIssuerDN()` is **not**
// used: going through the `toString` of just any `Principal` and reparsing it is precisely the
// confusion of names one wanted to avoid. If the DER cannot be read a `RuntimeException` is thrown,
// which is what the JDK does —the method declares no exceptions and there is no honest way of
// returning a name all the same—.
//
// `getSubjectAlternativeNames()` and `getIssuerAlternativeNames()` are there, but returning `null`
// like the base class of the JDK: they are concrete methods whose contract is "the subclasses know,
// I do not". Decoding `GeneralName` is still not there, and that is why no subclass here overrides
// them.
//
// `getExtendedKeyUsage()` really decodes: its extension is a SEQUENCE OF OID and nothing else, with
// no decision of trust inside. See `DerReader` for exactly where the limit was put.
public abstract class X509Certificate extends Certificate implements X509Extension, DEREncodable {

    private static final String OID_EXTENDED_KEY_USAGE = "2.5.29.37";

    // They are remembered because parsing the whole certificate at every call would be expensive
    // and because the JDK returns the same instance twice in a row, something there is code that
    // compares with `==`.
    private javax.security.auth.x500.X500Principal issuerX500;
    private javax.security.auth.x500.X500Principal subjectX500;

    protected X509Certificate() {
        super("X.509");
    }

    // It checks that the certificate is current **now**. With no return value: if it does not
    // throw, it is current. It is the same contract as `verify` and the same risk of swallowing it
    // with an empty catch.
    public abstract void checkValidity()
        throws CertificateExpiredException, CertificateNotYetValidException;

    // The same, but at a given date. It serves for verifying an old signature: the right question
    // there is not whether the certificate is valid today but whether it was valid when it signed.
    public abstract void checkValidity(Date date)
        throws CertificateExpiredException, CertificateNotYetValidException;

    // The version: 1, 2 or 3. A v1 certificate has no extensions, so it has neither
    // BasicConstraints nor KeyUsage; treating it as a CA because "it does not say it is not" is a
    // classic mistake.
    public abstract int getVersion();

    // The serial number. Unique **per issuer**, not absolutely: the pair (issuer, serial) is what
    // identifies a certificate, and that is why the CRLs and the selectors always ask for both.
    public abstract BigInteger getSerialNumber();

    // The issuer as a `Principal`.
    //
    // This method is discouraged in the JDK in favour of `getIssuerX500Principal()`, and with
    // reason: the `Principal` it returns is of an internal class and comparing two names by their
    // `toString` is not reliable. The replacement is here —see the comment of the class, which
    // explains that `X500Principal` exists now— and this one stays for the signatures that name it.
    public abstract Principal getIssuerDN();

    // The subject as a `Principal`. The same holds as for `getIssuerDN()`.
    public abstract Principal getSubjectDN();

    // The issuer as an X.500 name, which is the form with which comparison **is** possible.
    //
    // It is the replacement of `getIssuerDN()` and the difference is not cosmetic: two equal X.500
    // names can be written differently —upper case, spaces, order of escaping— and only the
    // canonical form of `X500Principal` gives them as equal. Chaining a certificate with its issuer
    // by comparing texts is exactly the mistake that lets somebody else's certificate through.
    public javax.security.auth.x500.X500Principal getIssuerX500Principal() {
        if (this.issuerX500 == null) {
            this.issuerX500 = name(false);
        }
        return this.issuerX500;
    }

    // The subject as an X.500 name. The same holds as for `getIssuerX500Principal()`.
    public javax.security.auth.x500.X500Principal getSubjectX500Principal() {
        if (this.subjectX500 == null) {
            this.subjectX500 = name(true);
        }
        return this.subjectX500;
    }

    private javax.security.auth.x500.X500Principal name(boolean subject) {
        try {
            return new javax.security.auth.x500.X500Principal(
                DerReader.certificateName(this.getEncoded(), subject));
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(subject ? "Could not parse subject" : "Could not parse issuer");
        } catch (IOException e) {
            throw new RuntimeException(subject ? "Could not parse subject" : "Could not parse issuer");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(subject ? "Could not parse subject" : "Could not parse issuer");
        }
    }

    // The alternative names of the subject (SubjectAltName extension), or null if there are none.
    //
    // It returns null and does not throw: it is a **concrete** method of the base class whose
    // contract is "the subclass that knows how to decode `GeneralName` should override it". The JDK
    // does exactly this same thing. Returning an empty list would be wrong: empty and absent are
    // different things —empty would mean that the certificate serves for no name—.
    //
    // For a server certificate **this is the method that matters**, not the CN of the subject:
    // since RFC 6125 the name of the host is looked for here and the CN was left as a historical
    // fallback.
    public java.util.Collection<List<?>> getSubjectAlternativeNames()
            throws CertificateParsingException {
        return null;
    }

    // The alternative names of the issuer. The same contract as the one above.
    public java.util.Collection<List<?>> getIssuerAlternativeNames()
            throws CertificateParsingException {
        return null;
    }

    // From when it is valid.
    public abstract Date getNotBefore();

    // Until when it is valid.
    public abstract Date getNotAfter();

    // The signed part of the certificate: everything except the signature itself. It is over these
    // bytes that one has to verify, and that is why the method exists instead of letting everybody
    // cut the DER themselves.
    public abstract byte[] getTBSCertificate() throws CertificateEncodingException;

    // The bits of the signature.
    public abstract byte[] getSignature();

    // The name of the signature algorithm: "SHA256withRSA".
    public abstract String getSigAlgName();

    // The OID of the signature algorithm. It is the **authoritative** datum: the name depends on
    // which table of OIDs the implementation has and can be null or odd for algorithms it does not
    // know.
    public abstract String getSigAlgOID();

    // The parameters of the signature algorithm in DER, or null if it carries none. For RSASSA-PSS
    // it is not optional: that is where the hash and the length of the salt live.
    public abstract byte[] getSigAlgParams();

    // The unique identifier of the issuer (v2+), or null. It is practically not used.
    public abstract boolean[] getIssuerUniqueID();

    // The unique identifier of the subject (v2+), or null.
    public abstract boolean[] getSubjectUniqueID();

    // The KeyUsage extension as bits, or null if it is not there.
    //
    // The order of the bits is that of the RFC: 0 digitalSignature, 1 nonRepudiation, 2
    // keyEncipherment, 3 dataEncipherment, 4 keyAgreement, 5 keyCertSign, 6 cRLSign, 7
    // encipherOnly, 8 decipherOnly. The one that matters for a chain is 5: without it, the
    // certificate cannot sign other certificates even if BasicConstraints says it is a CA.
    public abstract boolean[] getKeyUsage();

    // The chain length restriction of BasicConstraints, or -1 if the certificate **is not a CA**.
    //
    // The return value mixes two things and has to be read carefully: -1 means "it is not a CA";
    // `Integer.MAX_VALUE` means "it is a CA with no limit of length"; any other number is the
    // limit. Taking the -1 for "it is a CA with length zero" is exactly the opposite of what it
    // says.
    public abstract int getBasicConstraints();

    // The OIDs of ExtendedKeyUsage, or null if the extension is not there.
    //
    // The distinction between null and an empty list matters: null is "the certificate does not
    // restrict what it serves for", empty is "it serves for nothing". They are opposites.
    //
    // It really decodes because the extension is a SEQUENCE OF OBJECT IDENTIFIER and nothing else.
    public List<String> getExtendedKeyUsage() throws CertificateParsingException {
        byte[] ext = this.getExtensionValue(OID_EXTENDED_KEY_USAGE);
        if (ext == null) {
            return null;
        }
        try {
            byte[] value = DerReader.unwrapOctetString(ext);
            DerReader d = new DerReader(value, 0, value.length);
            int len = d.expect(DerReader.TAG_SEQUENCE);
            int end = d.position() + len;
            if (end > value.length) {
                throw new IOException("DER truncado: SEQUENCE incompleto");
            }
            List<String> oids = new ArrayList<String>();
            DerReader inner = new DerReader(value, d.position(), len);
            while (inner.hasMore()) {
                int oidLen = inner.expect(DerReader.TAG_OID);
                int from = inner.skip(oidLen);
                oids.add(inner.readOid(from, oidLen));
            }
            return java.util.Collections.unmodifiableList(oids);
        } catch (IOException e) {
            throw new CertificateParsingException(e.toString(), e);
        }
    }
}
