package java.security.cert;

import java.io.IOException;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// A criterion for choosing X.509 certificates of a `CertStore`.
//
// It is an object of accumulable criteria: conditions are set one after another and `match` returns
// true only if the certificate meets **all** of them. A newly created selector has no condition, so
// it accepts any X.509 certificate —and that is the right default, because a selector is a search
// filter and not a security check—.
//
// That a certificate passes the selector **says nothing about whether it is to be trusted**. The
// selector verifies no signatures and no chains; it only compares fields. It is the step before the
// validation, not a substitute for it.
//
// ===============================================================================================
// WHICH CRITERIA ARE THERE AND WHICH ARE NOT
// ===============================================================================================
//
// The rule that was followed is simple: a criterion is declared only if `match` can really apply
// it. A setter whose criterion `match` ignored would be worse than its absence, because it would
// return true for certificates that do not meet it —and here that means choosing the wrong
// certificate—.
//
// **They are all there**, and `match` applies them all: exact certificate, issuer, subject, serial
// number, currency at a date, currency of the private key, exact public key, OID of the algorithm
// of the public key, KeyUsage, ExtendedKeyUsage, SubjectKeyIdentifier, AuthorityKeyIdentifier,
// BasicConstraints, policies, alternative names, name constraints and `pathToNames`.
//
// The last three arrived with `GeneralNameValue` and `NameConstraints`, which are the two pieces
// that were missing. It is worth saying how the two that sound similar differ, because they look at
// opposite things of the certificate:
//
//   - **`setNameConstraints`** puts the constraints **of the caller** and applies them to the names
//     **of the certificate**: "bring me one whose subject and whose alternative names fall inside
//     here".
//   - **`setPathToNames`** does the opposite: they are names of the caller that are checked against
//     the NameConstraints extension **of the certificate itself**, that is, "bring me a CA that can
//     issue for these names". A certificate without that extension restricts nothing and always
//     passes.
//
// `setMatchAllSubjectAltNames` governs the alternative names and now it **does** change the result:
// at true --the default-- the certificate has to bring every name asked for; at false bringing one
// is enough for it.
public class X509CertSelector implements CertSelector {

    private static final String OID_SUBJECT_KEY_ID = "2.5.29.14";
    private static final String OID_AUTHORITY_KEY_ID = "2.5.29.35";
    private static final String OID_CERT_POLICIES = "2.5.29.32";
    private static final String OID_PRIVATE_KEY_USAGE = "2.5.29.16";
    private static final String OID_SUBJECT_ALT_NAME = "2.5.29.17";
    private static final String OID_NAME_CONSTRAINTS = "2.5.29.30";
    // The "any use" of ExtendedKeyUsage: a certificate that carries it satisfies any demand of
    // extended use.
    private static final String OID_ANY_EXTENDED_KEY_USAGE = "2.5.29.37.0";

    private X509Certificate x509Cert;
    private BigInteger serialNumber;
    private byte[] subjectKeyID;
    private byte[] authorityKeyID;
    private Date certificateValid;
    private Date privateKeyValid;
    // The alternative names are kept twice for the same reason as the issuers of `X509CRLSelector`:
    // one is what the caller set and the other is what it is compared with.
    private List<List<?>> subjectAlternativeNames;
    private List<GeneralNameValue> subjectAlternativeGeneralNames;
    private List<List<?>> pathToNames;
    private List<GeneralNameValue> pathToGeneralNames;
    private byte[] nameConstraintsBytes;
    private NameConstraints nameConstraints;
    private javax.security.auth.x500.X500Principal issuer;
    private javax.security.auth.x500.X500Principal subject;
    private String subjectPublicKeyAlgID;
    private PublicKey subjectPublicKey;
    private byte[] subjectPublicKeyBytes;
    private boolean[] keyUsage;
    private Set<String> keyPurposeSet;
    private boolean matchAllSubjectAltNames = true;
    // -1 is "I do not care"; -2 is "it has to be of an end entity"; >= 0 is the minimum length.
    private int basicConstraints = -1;
    private Set<String> policySet;

    // A selector with no criterion: it accepts any X.509 certificate.
    public X509CertSelector() {
    }

    // It demands **this** exact certificate. It is the strongest criterion of all and makes the
    // others redundant.
    public void setCertificate(X509Certificate cert) {
        this.x509Cert = cert;
    }

    public X509Certificate getCertificate() {
        return this.x509Cert;
    }

    // It demands this issuer. Null removes the criterion.
    //
    // The **instance** that was passed is kept, not a copy: `X500Principal` is immutable, and the
    // JDK does the same —`getIssuer()` returns the same object—.
    public void setIssuer(javax.security.auth.x500.X500Principal issuer) {
        this.issuer = issuer;
    }

    // The same, with the name written in RFC 2253.
    //
    // The text is parsed here and **is not kept**: what is left is the name, and
    // `getIssuerAsString()` returns its canonical form, not what was written. It is different from
    // `TrustAnchor`, which does keep the original; the difference is the JDK's and is worth keeping
    // in mind.
    public void setIssuer(String issuerDN) throws IOException {
        this.issuer = issuerDN == null ? null : principalOf(issuerDN);
    }

    // The same, with the DER of the `Name`.
    public void setIssuer(byte[] issuerDN) throws IOException {
        this.issuer = issuerDN == null ? null : principalOf(issuerDN);
    }

    public javax.security.auth.x500.X500Principal getIssuer() {
        return this.issuer;
    }

    // The issuer of the criterion in RFC 2253, or null if there is none.
    public String getIssuerAsString() {
        return this.issuer == null ? null : this.issuer.getName();
    }

    // The issuer of the criterion as the DER of its `Name`, or null if there is none. It is a copy.
    //
    // It declares `IOException` because the JDK declares it —there the name is kept in another form
    // and has to be encoded—; here it is kept encoded already and it never gets to be thrown.
    public byte[] getIssuerAsBytes() throws IOException {
        return this.issuer == null ? null : this.issuer.getEncoded();
    }

    // It demands this subject. Null removes the criterion. Everything said for the issuer holds.
    public void setSubject(javax.security.auth.x500.X500Principal subject) {
        this.subject = subject;
    }

    public void setSubject(String subjectDN) throws IOException {
        this.subject = subjectDN == null ? null : principalOf(subjectDN);
    }

    public void setSubject(byte[] subjectDN) throws IOException {
        this.subject = subjectDN == null ? null : principalOf(subjectDN);
    }

    public javax.security.auth.x500.X500Principal getSubject() {
        return this.subject;
    }

    public String getSubjectAsString() {
        return this.subject == null ? null : this.subject.getName();
    }

    public byte[] getSubjectAsBytes() throws IOException {
        return this.subject == null ? null : this.subject.getEncoded();
    }

    // `X500Principal` rejects with `IllegalArgumentException` and these methods promise
    // `IOException`. The messages are the JDK's: it tells the badly written name apart from the DER
    // that is not a name.
    private static javax.security.auth.x500.X500Principal principalOf(String dn)
            throws IOException {
        try {
            return new javax.security.auth.x500.X500Principal(dn);
        } catch (IllegalArgumentException e) {
            throw new IOException("Incorrect AVA format", e);
        }
    }

    private static javax.security.auth.x500.X500Principal principalOf(byte[] dn)
            throws IOException {
        try {
            return new javax.security.auth.x500.X500Principal(dn);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid name", e);
        }
    }

    /**
     * It demands that the certificate bring this alternative name. Several can be asked for; see
     * {@link #setMatchAllSubjectAltNames} for whether all of them are needed or one is enough.
     *
     * <p>The type is the number of the `CHOICE` of `GeneralName`: 1 rfc822Name, 2 dNSName, 4
     * directoryName, 6 URI, 7 iPAddress, 8 registeredID. The other three --0, 3 and 5-- have no
     * agreed text form and are rejected; for those there is the overload that takes the DER.
     *
     * @throws IOException if the type has no text form or the name is not valid for it
     */
    public void addSubjectAlternativeName(int type, String name) throws IOException {
        addAlternativeName(GeneralNameValue.ofString(type, name), type, name);
    }

    /**
     * The same, with the DER of the name.
     *
     * <p><b>Without the context tag</b>: a bare IA5String for the text types, a `Name` for
     * directoryName, an OCTET STRING for iPAddress. It is what the JDK expects and it is easy to
     * get wrong, because in a certificate the same name travels **with** its tag.
     */
    public void addSubjectAlternativeName(int type, byte[] name) throws IOException {
        addAlternativeName(GeneralNameValue.ofValueDer(type, name), type, copy(name));
    }

    private void addAlternativeName(GeneralNameValue parsed, int type, Object raw) {
        if (this.subjectAlternativeNames == null) {
            this.subjectAlternativeNames = new ArrayList<List<?>>();
            this.subjectAlternativeGeneralNames = new ArrayList<GeneralNameValue>();
        }
        List<Object> entry = new ArrayList<Object>();
        entry.add(Integer.valueOf(type));
        entry.add(raw);
        this.subjectAlternativeNames.add(Collections.unmodifiableList(entry));
        this.subjectAlternativeGeneralNames.add(parsed);
    }

    /**
     * It demands these alternative names. Each element is a list of two: the type as an
     * {@code Integer} and the name as a {@code String} or {@code byte[]}. Null or empty removes the
     * criterion.
     *
     * <p>Both sets are built complete before either is assigned: if an element in the middle is
     * wrong, the selector is left as it was and not halfway.
     *
     * @throws IOException if some element does not have the expected shape
     */
    public void setSubjectAlternativeNames(java.util.Collection<List<?>> names) throws IOException {
        if (names == null || names.isEmpty()) {
            this.subjectAlternativeNames = null;
            this.subjectAlternativeGeneralNames = null;
            return;
        }
        List<List<?>> raw = new ArrayList<List<?>>();
        List<GeneralNameValue> parsed = new ArrayList<GeneralNameValue>();
        Iterator<List<?>> it = names.iterator();
        while (it.hasNext()) {
            List<?> entry = it.next();
            if (entry == null || entry.size() != 2) {
                throw new IOException("name list entry must be of size 2");
            }
            if (!(entry.get(0) instanceof Integer)) {
                throw new IOException("expected an Integer name type");
            }
            int type = ((Integer) entry.get(0)).intValue();
            Object value = entry.get(1);
            List<Object> entryCopy = new ArrayList<Object>();
            entryCopy.add(Integer.valueOf(type));
            if (value instanceof String) {
                parsed.add(GeneralNameValue.ofString(type, (String) value));
                entryCopy.add(value);
            } else if (value instanceof byte[]) {
                parsed.add(GeneralNameValue.ofValueDer(type, (byte[]) value));
                entryCopy.add(copy((byte[]) value));
            } else {
                throw new IOException("name not byte array or String");
            }
            raw.add(Collections.unmodifiableList(entryCopy));
        }
        this.subjectAlternativeNames = raw;
        this.subjectAlternativeGeneralNames = parsed;
    }

    /**
     * The alternative names of the criterion, or null if there are none. A copy: the {@code
     * byte[]}s go cloned, so touching what comes out of here does not change the criterion.
     */
    public java.util.Collection<List<?>> getSubjectAlternativeNames() {
        return copyOfNameList(this.subjectAlternativeNames);
    }

    /**
     * It demands that the certificate be able to issue for these names, according to **its own**
     * NameConstraints extension. Null or empty removes the criterion.
     *
     * <p>It is the criterion for looking for a CA, not an end-entity certificate: the question it
     * answers is "does this CA have the right to sign something called like this?". A certificate
     * without the extension restricts nothing and always passes.
     *
     * @throws IOException if some element does not have the expected shape
     */
    public void setPathToNames(java.util.Collection<List<?>> names) throws IOException {
        if (names == null || names.isEmpty()) {
            this.pathToNames = null;
            this.pathToGeneralNames = null;
            return;
        }
        X509CertSelector tmp = new X509CertSelector();
        tmp.setSubjectAlternativeNames(names);
        this.pathToNames = tmp.subjectAlternativeNames;
        this.pathToGeneralNames = tmp.subjectAlternativeGeneralNames;
    }

    /** It adds a name to the criterion of {@link #setPathToNames}. */
    public void addPathToName(int type, String name) throws IOException {
        addPath(GeneralNameValue.ofString(type, name), type, name);
    }

    /** The same, with the DER of the name and without its context tag. */
    public void addPathToName(int type, byte[] name) throws IOException {
        addPath(GeneralNameValue.ofValueDer(type, name), type, copy(name));
    }

    private void addPath(GeneralNameValue parsed, int type, Object raw) {
        if (this.pathToNames == null) {
            this.pathToNames = new ArrayList<List<?>>();
            this.pathToGeneralNames = new ArrayList<GeneralNameValue>();
        }
        List<Object> entry = new ArrayList<Object>();
        entry.add(Integer.valueOf(type));
        entry.add(raw);
        this.pathToNames.add(Collections.unmodifiableList(entry));
        this.pathToGeneralNames.add(parsed);
    }

    /** The names of {@link #setPathToNames}, or null if there are none. A copy. */
    public java.util.Collection<List<?>> getPathToNames() {
        return copyOfNameList(this.pathToNames);
    }

    /**
     * It demands that the subject and the alternative names of the certificate fall inside these
     * constraints. The argument is the DER of the **value** of the NameConstraints extension.
     *
     * <p>It is parsed here: badly formed constraints are an error of the caller, and keeping them
     * without looking would leave a criterion that says it restricts and does not.
     *
     * @throws IOException if the DER is not a well formed NameConstraints extension
     */
    public void setNameConstraints(byte[] bytes) throws IOException {
        if (bytes == null) {
            this.nameConstraintsBytes = null;
            this.nameConstraints = null;
            return;
        }
        this.nameConstraints = NameConstraints.of(bytes);
        this.nameConstraintsBytes = copy(bytes);
    }

    /** A copy of the DER of the constraints, or null if there are none. */
    public byte[] getNameConstraints() {
        return copy(this.nameConstraintsBytes);
    }

    private static java.util.Collection<List<?>> copyOfNameList(List<List<?>> names) {
        if (names == null) {
            return null;
        }
        List<List<?>> out = new ArrayList<List<?>>();
        int i = 0;
        while (i < names.size()) {
            List<?> entry = names.get(i);
            List<Object> one = new ArrayList<Object>();
            one.add(entry.get(0));
            Object value = entry.get(1);
            one.add(value instanceof byte[] ? copy((byte[]) value) : value);
            out.add(Collections.unmodifiableList(one));
            i = i + 1;
        }
        return out;
    }

    // It demands this serial number. By itself it **does not identify** a certificate: the serial
    // is unique per issuer, so without also fixing the issuer this can bring certificates of other
    // CAs.
    public void setSerialNumber(BigInteger serial) {
        this.serialNumber = serial;
    }

    public BigInteger getSerialNumber() {
        return this.serialNumber;
    }

    // The SubjectKeyIdentifier it has to have, as the **DER of the KeyIdentifier** —that is, a
    // complete OCTET STRING with its tag and its length, not the bare bytes of the identifier—. It
    // is what the contract of the JDK says and it is easy to get wrong.
    public void setSubjectKeyIdentifier(byte[] subjectKeyID) {
        this.subjectKeyID = copy(subjectKeyID);
    }

    public byte[] getSubjectKeyIdentifier() {
        return copy(this.subjectKeyID);
    }

    // The AuthorityKeyIdentifier, with the same format: the complete DER of the extension already
    // unwrapped from the outer OCTET STRING.
    public void setAuthorityKeyIdentifier(byte[] authorityKeyID) {
        this.authorityKeyID = copy(authorityKeyID);
    }

    public byte[] getAuthorityKeyIdentifier() {
        return copy(this.authorityKeyID);
    }

    // It demands that the certificate be current at this date. Null removes the criterion.
    public void setCertificateValid(Date certValid) {
        if (certValid == null) {
            this.certificateValid = null;
        } else {
            this.certificateValid = new Date(certValid.getTime());
        }
    }

    public Date getCertificateValid() {
        if (this.certificateValid == null) {
            return null;
        }
        return new Date(this.certificateValid.getTime());
    }

    // It demands that the **private key** of the certificate was current at this date, according to
    // the PrivateKeyUsagePeriod extension. Null removes the criterion.
    //
    // It is not the same as `setCertificateValid`, and the difference is the reason the extension
    // exists: a signing key stops being able to sign before its certificate expires, so that the
    // old signatures can go on being verified afterwards. The certificate goes on being valid; the
    // key can no longer produce new signatures.
    //
    // A certificate **without** the extension passes the criterion. It is the JDK's and it has to
    // be said because it sounds the wrong way round: with no extension there is no declared period,
    // that is, the key was not limited.
    public void setPrivateKeyValid(Date privateKeyValid) {
        if (privateKeyValid == null) {
            this.privateKeyValid = null;
        } else {
            this.privateKeyValid = new Date(privateKeyValid.getTime());
        }
    }

    public Date getPrivateKeyValid() {
        if (this.privateKeyValid == null) {
            return null;
        }
        return new Date(this.privateKeyValid.getTime());
    }

    // The OID of the algorithm of the public key: "1.2.840.113549.1.1.1" for RSA. It is validated
    // to be a well formed OID at the moment of setting it, not when it is used.
    public void setSubjectPublicKeyAlgID(String oid) throws IOException {
        if (oid == null) {
            this.subjectPublicKeyAlgID = null;
        } else {
            DerReader.validateOid(oid);
            this.subjectPublicKeyAlgID = oid;
        }
    }

    public String getSubjectPublicKeyAlgID() {
        return this.subjectPublicKeyAlgID;
    }

    // It demands exactly this public key. It is compared by the encoding, not by identity: two
    // different objects with the same bytes are the same key.
    public void setSubjectPublicKey(PublicKey key) {
        if (key == null) {
            this.subjectPublicKey = null;
            this.subjectPublicKeyBytes = null;
        } else {
            this.subjectPublicKey = key;
            this.subjectPublicKeyBytes = key.getEncoded();
        }
    }

    /**
     * The same, with the key in its encoded form -- a `SubjectPublicKeyInfo` of X.509.
     *
     * <p>The DER is parsed here and not when it is used: a badly formed key is an error of the
     * caller and they have to be told at the moment, not left with a criterion that afterwards
     * matches nothing.
     *
     * <p>What {@link #getSubjectPublicKey()} returns after this is not the same class the JDK would
     * return; see {@code EncodedPublicKey} for the difference and its reason.
     *
     * @throws IOException if the DER is not a well formed SubjectPublicKeyInfo
     */
    public void setSubjectPublicKey(byte[] key) throws IOException {
        if (key == null) {
            this.subjectPublicKey = null;
            this.subjectPublicKeyBytes = null;
            return;
        }
        this.subjectPublicKey = EncodedPublicKey.of(key);
        this.subjectPublicKeyBytes = copy(key);
    }

    public PublicKey getSubjectPublicKey() {
        return this.subjectPublicKey;
    }

    // The bits of KeyUsage the certificate has to have **on**. A false at position i demands
    // nothing; only the trues are conditions.
    //
    // And there is an asymmetry of the JDK worth knowing: a certificate **without** a KeyUsage
    // extension always passes, because it restricts nothing. The criterion filters certificates
    // that declare uses and do not include the ones asked for, not certificates that declare
    // nothing.
    public void setKeyUsage(boolean[] keyUsage) {
        if (keyUsage == null) {
            this.keyUsage = null;
        } else {
            boolean[] c = new boolean[keyUsage.length];
            System.arraycopy(keyUsage, 0, c, 0, keyUsage.length);
            this.keyUsage = c;
        }
    }

    public boolean[] getKeyUsage() {
        if (this.keyUsage == null) {
            return null;
        }
        boolean[] c = new boolean[this.keyUsage.length];
        System.arraycopy(this.keyUsage, 0, c, 0, this.keyUsage.length);
        return c;
    }

    // The OIDs of ExtendedKeyUsage the certificate has to have. An **empty set is treated as
    // null**: it means "no criterion", not "no use at all".
    //
    // Just as with KeyUsage: a certificate without the extension passes. And one that carries
    // anyExtendedKeyUsage (2.5.29.37.0) too, because that OID means exactly "I serve for
    // everything".
    public void setExtendedKeyUsage(Set<String> keyPurposeSet) throws IOException {
        if (keyPurposeSet == null || keyPurposeSet.isEmpty()) {
            this.keyPurposeSet = null;
        } else {
            Set<String> copyOf = new HashSet<String>(keyPurposeSet);
            Iterator<String> it = copyOf.iterator();
            while (it.hasNext()) {
                DerReader.validateOid(it.next());
            }
            this.keyPurposeSet = Collections.unmodifiableSet(copyOf);
        }
    }

    public Set<String> getExtendedKeyUsage() {
        return this.keyPurposeSet;
    }

    // Whether **all** the alternative names have to be demanded or one is enough.
    //
    // This note used to say that the criterion this flag governs was not implemented here and that
    // the flag was kept without changing what `match` returns. It is implemented: see the note of
    // the class and `matchSubjectAltNames`. The default is true, just as in the JDK.
    public void setMatchAllSubjectAltNames(boolean matchAllNames) {
        this.matchAllSubjectAltNames = matchAllNames;
    }

    public boolean getMatchAllSubjectAltNames() {
        return this.matchAllSubjectAltNames;
    }

    // The restriction of BasicConstraints. The three ranges mean different things:
    //
    //   -1  no criterion (the default).
    //   -2  the certificate has to be of an **end entity**, that is, not a CA.
    //   >=0 it has to be a CA whose maximum chain length is at least this number.
    //
    // The -2 is the one that serves for "do not bring me CAs", and confusing it with -1 makes the
    // filter not filter.
    public void setBasicConstraints(int minMaxPathLen) {
        if (minMaxPathLen < -2) {
            throw new IllegalArgumentException("basic constraints less than -2");
        }
        this.basicConstraints = minMaxPathLen;
    }

    public int getBasicConstraints() {
        return this.basicConstraints;
    }

    // The policy OIDs the certificate has to declare. It is enough for it to have **one** of the
    // ones in the set.
    //
    // The empty set is not the same as null: empty demands that the certificate have the policy
    // extension, whichever it is; null removes the criterion.
    public void setPolicy(Set<String> certPolicySet) throws IOException {
        if (certPolicySet == null) {
            this.policySet = null;
        } else {
            Set<String> copyOf = new HashSet<String>(certPolicySet);
            Iterator<String> it = copyOf.iterator();
            while (it.hasNext()) {
                DerReader.validateOid(it.next());
            }
            this.policySet = Collections.unmodifiableSet(copyOf);
        }
    }

    public Set<String> getPolicy() {
        return this.policySet;
    }

    // Whether the certificate meets **all** the criteria that were set.
    //
    // An object that is not an `X509Certificate` does not meet them: there is no way of asking it
    // anything this selector compares.
    @Override
    public boolean match(Certificate cert) {
        if (!(cert instanceof X509Certificate)) {
            return false;
        }
        X509Certificate xcert = (X509Certificate) cert;

        if (this.x509Cert != null && !this.x509Cert.equals(xcert)) {
            return false;
        }
        if (this.serialNumber != null && !this.serialNumber.equals(xcert.getSerialNumber())) {
            return false;
        }
        // The names are compared by `X500Principal`, that is, by canonical form. Comparing the
        // texts would accept somebody else's certificate whose name is written differently but
        // means the same, and would reject one's own for one space too many.
        if (this.issuer != null && !this.issuer.equals(xcert.getIssuerX500Principal())) {
            return false;
        }
        if (this.subject != null && !this.subject.equals(xcert.getSubjectX500Principal())) {
            return false;
        }
        if (!matchesPrivateKeyPeriod(xcert)) {
            return false;
        }
        if (!matchesAlternativeNames(xcert)) {
            return false;
        }
        if (!matchesNameConstraints(xcert)) {
            return false;
        }
        if (!matchesPathToNames(xcert)) {
            return false;
        }
        if (this.certificateValid != null) {
            try {
                xcert.checkValidity(this.certificateValid);
            } catch (CertificateException e) {
                return false;
            }
        }
        if (this.subjectPublicKeyBytes != null) {
            byte[] certKey = xcert.getPublicKey().getEncoded();
            if (!sameBytes(this.subjectPublicKeyBytes, certKey)) {
                return false;
            }
        }
        return matchesBasicConstraints(xcert)
            && matchesKeyUsage(xcert)
            && matchesExtendedKeyUsage(xcert)
            && matchesKeyId(xcert, OID_SUBJECT_KEY_ID, this.subjectKeyID)
            && matchesKeyId(xcert, OID_AUTHORITY_KEY_ID, this.authorityKeyID)
            && matchesKeyAlgId(xcert)
            && matchesPolicy(xcert);
    }

    // The PrivateKeyUsagePeriod extension, if it is there, has to cover the date asked for.
    //
    //   PrivateKeyUsagePeriod ::= SEQUENCE {
    //       notBefore [0] GeneralizedTime OPTIONAL,
    //       notAfter  [1] GeneralizedTime OPTIONAL }
    //
    // Both fields are optional and both can be missing —an empty SEQUENCE, which according to the
    // RFC should not happen but is encoded all the same—; there the period limits nothing and the
    // certificate passes. An extension **present but unreadable** on the other hand does not pass:
    // it cannot be asserted that the key was current if what the certificate says is not
    // understood, and the safe side is to reject.
    private boolean matchesPrivateKeyPeriod(X509Certificate xcert) {
        if (this.privateKeyValid == null) {
            return true;
        }
        byte[] ext = xcert.getExtensionValue(OID_PRIVATE_KEY_USAGE);
        if (ext == null) {
            return true;
        }
        long when = this.privateKeyValid.getTime();
        try {
            byte[] value = DerReader.unwrapOctetString(ext);
            DerReader d = new DerReader(value, 0, value.length);
            int len = d.expect(DerReader.TAG_SEQUENCE);
            DerReader inner = new DerReader(value, d.position(), len);
            while (inner.hasMore()) {
                int tag = inner.readTag();
                int n = inner.readLength();
                int from = inner.skip(n);
                if (tag == 0x80) {
                    if (when < DerReader.generalizedTime(value, from, n)) {
                        return false;
                    }
                } else if (tag == 0x81) {
                    if (when > DerReader.generalizedTime(value, from, n)) {
                        return false;
                    }
                } else {
                    throw new IOException("DER: campo inesperado en PrivateKeyUsagePeriod");
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // The names asked for have to be in the SubjectAltName of the certificate.
    //
    // `matchAllSubjectAltNames` decides whether all of them are needed or one is enough. The
    // default is **all**, which is the restrictive side: a selector that brings a certificate for
    // matching only one of three names is not what whoever set the three was asking for.
    private boolean matchesAlternativeNames(X509Certificate xcert) {
        if (this.subjectAlternativeGeneralNames == null) {
            return true;
        }
        List<GeneralNameValue> certIssuer;
        try {
            certIssuer = alternativeNamesOf(xcert);
        } catch (IOException e) {
            // An unreadable SubjectAltName cannot be asserted to bring the names asked for.
            return false;
        }
        int i = 0;
        while (i < this.subjectAlternativeGeneralNames.size()) {
            boolean present = certIssuer.contains(this.subjectAlternativeGeneralNames.get(i));
            if (present && !this.matchAllSubjectAltNames) {
                return true;
            }
            if (!present && this.matchAllSubjectAltNames) {
                return false;
            }
            i = i + 1;
        }
        // With matchAll none failed; without matchAll none hit.
        return this.matchAllSubjectAltNames;
    }

    private static List<GeneralNameValue> alternativeNamesOf(X509Certificate xcert)
            throws IOException {
        List<GeneralNameValue> out = new ArrayList<GeneralNameValue>();
        byte[] ext = xcert.getExtensionValue(OID_SUBJECT_ALT_NAME);
        if (ext == null) {
            return out;
        }
        byte[] value = DerReader.unwrapOctetString(ext);
        DerReader d = new DerReader(value, 0, value.length);
        int len = d.expect(DerReader.TAG_SEQUENCE);
        DerReader list = new DerReader(value, d.position(), len);
        while (list.hasMore()) {
            int at = list.position();
            int[] one = list.nextTlv();
            out.add(GeneralNameValue.ofTagged(value, at, one[1]));
        }
        return out;
    }

    // The names of the certificate have to fall inside the constraints of the criterion.
    private boolean matchesNameConstraints(X509Certificate xcert) {
        if (this.nameConstraints == null) {
            return true;
        }
        return this.nameConstraints.verify(xcert);
    }

    // The other way round: the names of the criterion have to fall inside the constraints **of the
    // certificate**. A certificate without the extension restricts nothing and passes.
    private boolean matchesPathToNames(X509Certificate xcert) {
        if (this.pathToGeneralNames == null) {
            return true;
        }
        byte[] ext = xcert.getExtensionValue(OID_NAME_CONSTRAINTS);
        if (ext == null) {
            return true;
        }
        try {
            NameConstraints nc = NameConstraints.of(DerReader.unwrapOctetString(ext));
            return nc.verify(this.pathToGeneralNames);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean matchesBasicConstraints(X509Certificate xcert) {
        if (this.basicConstraints == -1) {
            return true;
        }
        int maxPathLen = xcert.getBasicConstraints();
        if (this.basicConstraints == -2) {
            // -1 of the certificate is "it is not a CA", which is just what is asked for.
            return maxPathLen == -1;
        }
        return maxPathLen >= this.basicConstraints;
    }

    private boolean matchesKeyUsage(X509Certificate xcert) {
        if (this.keyUsage == null) {
            return true;
        }
        boolean[] certKeyUsage = xcert.getKeyUsage();
        // With no extension there is no restriction to violate: it passes.
        if (certKeyUsage == null) {
            return true;
        }
        int i = 0;
        while (i < this.keyUsage.length) {
            if (this.keyUsage[i] && (i >= certKeyUsage.length || !certKeyUsage[i])) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    private boolean matchesExtendedKeyUsage(X509Certificate xcert) {
        if (this.keyPurposeSet == null || this.keyPurposeSet.isEmpty()) {
            return true;
        }
        List<String> uses;
        try {
            uses = xcert.getExtendedKeyUsage();
        } catch (CertificateParsingException e) {
            return false;
        }
        // With no extension, the certificate does not restrict what it serves for: it passes.
        if (uses == null) {
            return true;
        }
        if (uses.contains(OID_ANY_EXTENDED_KEY_USAGE)) {
            return true;
        }
        return uses.containsAll(this.keyPurposeSet);
    }

    // It compares a key identifier against that of the extension. The extension comes wrapped in an
    // OCTET STRING; what is compared is what is inside against what was set in the selector.
    private boolean matchesKeyId(X509Certificate xcert, String oid, byte[] expected) {
        if (expected == null) {
            return true;
        }
        byte[] ext = xcert.getExtensionValue(oid);
        if (ext == null) {
            return false;
        }
        try {
            return sameBytes(expected, DerReader.unwrapOctetString(ext));
        } catch (IOException e) {
            return false;
        }
    }

    // It takes the OID of the algorithm out of the `SubjectPublicKeyInfo` of the key.
    //
    // The structure is SEQUENCE { AlgorithmIdentifier SEQUENCE { OID, params OPTIONAL }, BIT STRING
    // }. Only getting as far as the first OID is needed, which is three steps of DER and no
    // decision of trust.
    private boolean matchesKeyAlgId(X509Certificate xcert) {
        if (this.subjectPublicKeyAlgID == null) {
            return true;
        }
        PublicKey k = xcert.getPublicKey();
        if (k == null) {
            return false;
        }
        byte[] enc = k.getEncoded();
        if (enc == null) {
            return false;
        }
        try {
            DerReader d = new DerReader(enc, 0, enc.length);
            int extLen = d.expect(DerReader.TAG_SEQUENCE);
            DerReader inner = new DerReader(enc, d.position(), extLen);
            int algLen = inner.expect(DerReader.TAG_SEQUENCE);
            DerReader alg = new DerReader(enc, inner.position(), algLen);
            int oidLen = alg.expect(DerReader.TAG_OID);
            int from = alg.skip(oidLen);
            return this.subjectPublicKeyAlgID.equals(alg.readOid(from, oidLen));
        } catch (IOException e) {
            return false;
        }
    }

    // It checks the policies of the certificate.
    //
    // The extension is SEQUENCE OF PolicyInformation, and each PolicyInformation is a SEQUENCE
    // whose first element is the OID of the policy. The rest of each entry —the qualifiers— is
    // skipped without being looked at, which is right: here only which OIDs it declares is of
    // interest.
    private boolean matchesPolicy(X509Certificate xcert) {
        if (this.policySet == null) {
            return true;
        }
        byte[] ext = xcert.getExtensionValue(OID_CERT_POLICIES);
        if (ext == null) {
            return false;
        }
        try {
            byte[] value = DerReader.unwrapOctetString(ext);
            DerReader d = new DerReader(value, 0, value.length);
            int len = d.expect(DerReader.TAG_SEQUENCE);
            DerReader inner = new DerReader(value, d.position(), len);
            List<String> oids = new ArrayList<String>();
            while (inner.hasMore()) {
                int infoLen = inner.expect(DerReader.TAG_SEQUENCE);
                DerReader info = new DerReader(value, inner.position(), infoLen);
                inner.skip(infoLen);
                int oidLen = info.expect(DerReader.TAG_OID);
                int from = info.skip(oidLen);
                oids.add(info.readOid(from, oidLen));
            }
            // The empty set asks only that the extension exist with some policy inside.
            if (this.policySet.isEmpty()) {
                return !oids.isEmpty();
            }
            Iterator<String> it = oids.iterator();
            while (it.hasNext()) {
                if (this.policySet.contains(it.next())) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    // A copy the `CertStore` can keep. The arrays and the date are copied; the sets are immutable
    // already.
    @Override
    public Object clone() {
        try {
            X509CertSelector copyOf = (X509CertSelector) super.clone();
            copyOf.subjectKeyID = copy(this.subjectKeyID);
            copyOf.authorityKeyID = copy(this.authorityKeyID);
            copyOf.subjectPublicKeyBytes = copy(this.subjectPublicKeyBytes);
            copyOf.keyUsage = this.getKeyUsage();
            copyOf.certificateValid = this.getCertificateValid();
            copyOf.privateKeyValid = this.getPrivateKeyValid();
            copyOf.nameConstraintsBytes = copy(this.nameConstraintsBytes);
            // The three lists are copied: the store keeps the clone, and a later `add` over the
            // original must not change its criterion halfway through a search.
            if (this.subjectAlternativeNames != null) {
                copyOf.subjectAlternativeNames =
                    new ArrayList<List<?>>(this.subjectAlternativeNames);
                copyOf.subjectAlternativeGeneralNames =
                    new ArrayList<GeneralNameValue>(this.subjectAlternativeGeneralNames);
            }
            if (this.pathToNames != null) {
                copyOf.pathToNames = new ArrayList<List<?>>(this.pathToNames);
                copyOf.pathToGeneralNames =
                    new ArrayList<GeneralNameValue>(this.pathToGeneralNames);
            }
            return copyOf;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }

    // A KajiLibrary subset: the JDK also prints the criteria that do not exist here, and uses its
    // internal hexadecimal dump for the arrays. The format is not specified; the structure and the
    // names of the fields that do exist are kept.
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("X509CertSelector: [\n");
        if (this.x509Cert != null) {
            sb.append("  Certificate: " + this.x509Cert.toString() + "\n");
        }
        if (this.serialNumber != null) {
            sb.append("  Serial Number: " + this.serialNumber.toString() + "\n");
        }
        if (this.issuer != null) {
            sb.append("  Issuer: " + this.issuer.getName() + "\n");
        }
        if (this.subject != null) {
            sb.append("  Subject: " + this.subject.getName() + "\n");
        }
        if (this.certificateValid != null) {
            sb.append("  Certificate Valid: " + this.certificateValid.toString() + "\n");
        }
        if (this.privateKeyValid != null) {
            sb.append("  Private Key Valid: " + this.privateKeyValid.toString() + "\n");
        }
        if (this.subjectPublicKeyAlgID != null) {
            sb.append("  Subject Public Key AlgID: " + this.subjectPublicKeyAlgID + "\n");
        }
        if (this.subjectPublicKey != null) {
            sb.append("  Subject Public Key: " + this.subjectPublicKey.toString() + "\n");
        }
        if (this.keyUsage != null) {
            sb.append("  Key Usage: " + this.keyUsage.length + " bits\n");
        }
        if (this.keyPurposeSet != null) {
            sb.append("  Extended Key Usage: " + this.keyPurposeSet.toString() + "\n");
        }
        if (this.subjectAlternativeNames != null) {
            sb.append("  Subject Alternative Names: " + this.subjectAlternativeNames.size()
                + " names\n");
        }
        if (this.pathToNames != null) {
            sb.append("  Path to Names: " + this.pathToNames.size() + " names\n");
        }
        if (this.nameConstraintsBytes != null) {
            sb.append("  Name Constraints: " + this.nameConstraintsBytes.length + " bytes\n");
        }
        sb.append("  Match All Subject Alt Names: " + this.matchAllSubjectAltNames + "\n");
        sb.append("  Basic Constraints: " + this.basicConstraints + "\n");
        if (this.policySet != null) {
            sb.append("  Policy: " + this.policySet.toString() + "\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private static byte[] copy(byte[] b) {
        if (b == null) {
            return null;
        }
        byte[] c = new byte[b.length];
        System.arraycopy(b, 0, c, 0, b.length);
        return c;
    }

    private static boolean sameBytes(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return a == b;
        }
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
    }
}
