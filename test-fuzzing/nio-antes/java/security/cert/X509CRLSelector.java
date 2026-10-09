package java.security.cert;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashSet;

// A criterion for choosing CRLs of a `CertStore`.
//
// Just like `X509CertSelector`: the criteria accumulate and `match` demands all of them. A newly
// created selector accepts any X.509 CRL.
//
// The criterion that is used most —and the one that is forgotten most— is the date. Without it, the
// store can return an expired CRL, and an expired CRL says nothing: it is a photograph from before
// the revocation that is being looked for. `setDateAndTime` is what demands that the CRL cover the
// moment of interest.
//
// The CRL numbers serve for the same thing from another angle: each CRL of an issuer carries a
// number that grows, so asking for a minimum is asking for "a newer one than the one I have
// already". It is the defence against an attacker who controls the network serving an old but still
// valid CRL.
//
// ===============================================================================================
// THE TWO SETS OF ISSUERS
// ===============================================================================================
//
// The issuer criterion is kept **twice**, and it is not redundancy: it is the JDK's shape and it
// has to be reproduced because the two accessors return different things.
//
//   - `issuerX500Principals` is what `match` uses: X.500 names in canonical form, which is the only
//     way of comparing two DNs without getting it wrong.
//   - `issuerNames` keeps what the caller **set**, as it is: a `String` goes on being the same
//     `String`, and an `X500Principal` or a `byte[]` are kept as bytes. `getIssuerNames()` returns
//     that, with which a collection can have `String`s and `byte[]`s mixed.
//
// Both are set and removed together, always: a selector with one and without the other would filter
// wrongly. `setIssuerNames(null)` and `setIssuerNames(empty list)` do the same thing —they leave
// both at null, that is, with no criterion— and that is of the JDK too.
public class X509CRLSelector implements CRLSelector {

    private static final String OID_CRL_NUMBER = "2.5.29.20";

    private BigInteger minCRL;
    private BigInteger maxCRL;
    private Date dateAndTime;
    private X509Certificate certChecking;
    private HashSet<Object> issuerNames;
    private HashSet<javax.security.auth.x500.X500Principal> issuerX500Principals;

    // A selector with no criteria: it accepts any X.509 CRL.
    public X509CRLSelector() {
    }

    // It demands that the CRL be of one of these issuers. null or empty removes the criterion.
    public void setIssuers(java.util.Collection<javax.security.auth.x500.X500Principal> issuers) {
        if (issuers == null || issuers.isEmpty()) {
            this.issuerNames = null;
            this.issuerX500Principals = null;
            return;
        }
        this.issuerX500Principals =
            new HashSet<javax.security.auth.x500.X500Principal>(issuers);
        this.issuerNames = new HashSet<Object>();
        java.util.Iterator<javax.security.auth.x500.X500Principal> it =
            this.issuerX500Principals.iterator();
        while (it.hasNext()) {
            this.issuerNames.add(it.next().getEncoded());
        }
    }

    // The same, with the names as `String`s in RFC 2253 or as `byte[]`s with the DER of the `Name`.
    //
    // Both sets are built **complete before** either is assigned: if an element in the middle is
    // badly formed, the selector is left as it was and not halfway with a criterion that filters
    // too little.
    public void setIssuerNames(java.util.Collection<?> names) throws IOException {
        if (names == null || names.isEmpty()) {
            this.issuerNames = null;
            this.issuerX500Principals = null;
            return;
        }
        HashSet<Object> raw = new HashSet<Object>();
        HashSet<javax.security.auth.x500.X500Principal> parsed =
            new HashSet<javax.security.auth.x500.X500Principal>();
        java.util.Iterator<?> it = names.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof String) {
                raw.add(o);
                parsed.add(principalOf((String) o));
            } else if (o instanceof byte[]) {
                byte[] b = (byte[]) o;
                byte[] copyOf = new byte[b.length];
                System.arraycopy(b, 0, copyOf, 0, b.length);
                raw.add(copyOf);
                parsed.add(principalOf(copyOf));
            } else {
                throw new IOException("name not byte array or String");
            }
        }
        this.issuerNames = raw;
        this.issuerX500Principals = parsed;
    }

    // It adds an issuer to the criterion.
    public void addIssuer(javax.security.auth.x500.X500Principal issuer) {
        add(issuer.getEncoded(), issuer);
    }

    // It adds an issuer written in RFC 2253.
    //
    // Discouraged in the JDK in favour of `addIssuer`, and with reason: the text is kept as it is
    // and it is `getIssuerNames()` that afterwards returns it without canonicalising. The criterion
    // itself **is** canonicalised —what is compared is the `X500Principal`—, so the filtering is
    // right all the same.
    public void addIssuerName(String name) throws IOException {
        add(name, principalOf(name));
    }

    // It adds an issuer from the DER of its `Name`.
    public void addIssuerName(byte[] name) throws IOException {
        byte[] copyOf = new byte[name.length];
        System.arraycopy(name, 0, copyOf, 0, name.length);
        add(copyOf, principalOf(copyOf));
    }

    private void add(Object rawBytes, javax.security.auth.x500.X500Principal name) {
        if (this.issuerNames == null) {
            this.issuerNames = new HashSet<Object>();
            this.issuerX500Principals = new HashSet<javax.security.auth.x500.X500Principal>();
        }
        this.issuerNames.add(rawBytes);
        this.issuerX500Principals.add(name);
    }

    // The constructor of `X500Principal` rejects with `IllegalArgumentException`, but these methods
    // promise `IOException`. It is translated instead of letting the other escape: whoever calls
    // `addIssuerName` expects a badly written name to be a declared error.
    private static javax.security.auth.x500.X500Principal principalOf(String name)
            throws IOException {
        try {
            return new javax.security.auth.x500.X500Principal(name);
        } catch (IllegalArgumentException e) {
            throw new IOException("Incorrect AVA format", e);
        }
    }

    private static javax.security.auth.x500.X500Principal principalOf(byte[] name)
            throws IOException {
        try {
            return new javax.security.auth.x500.X500Principal(name);
        } catch (IllegalArgumentException e) {
            throw new IOException("Incorrect AVA format", e);
        }
    }

    // The issuers of the criterion, or null if there are none. Immutable: the criterion is changed
    // through the setters.
    public java.util.Collection<javax.security.auth.x500.X500Principal> getIssuers() {
        if (this.issuerX500Principals == null) {
            return null;
        }
        return java.util.Collections.unmodifiableCollection(this.issuerX500Principals);
    }

    // The issuers **as they were set**, or null if there are none: `String`s and `byte[]`s mixed.
    //
    // It is a copy and the `byte[]`s go cloned, so touching what comes out of here does not change
    // the criterion. Unlike `getIssuers()`, the collection itself is modifiable —also of the JDK—.
    public java.util.Collection<Object> getIssuerNames() {
        if (this.issuerNames == null) {
            return null;
        }
        HashSet<Object> copyOf = new HashSet<Object>();
        java.util.Iterator<Object> it = this.issuerNames.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof byte[]) {
                byte[] b = (byte[]) o;
                byte[] c = new byte[b.length];
                System.arraycopy(b, 0, c, 0, b.length);
                copyOf.add(c);
            } else {
                copyOf.add(o);
            }
        }
        return copyOf;
    }

    // The minimum CRL number. It is how "a newer one than this" is asked for.
    public void setMinCRLNumber(BigInteger minCRL) {
        this.minCRL = minCRL;
    }

    public BigInteger getMinCRL() {
        return this.minCRL;
    }

    // The maximum CRL number. It serves for reconstructing the state at a moment in the past.
    public void setMaxCRLNumber(BigInteger maxCRL) {
        this.maxCRL = maxCRL;
    }

    public BigInteger getMaxCRL() {
        return this.maxCRL;
    }

    // It demands that the CRL cover this instant: that its `thisUpdate` not be later and its
    // `nextUpdate` not be earlier. Both ends **count**.
    public void setDateAndTime(Date dateAndTime) {
        if (dateAndTime == null) {
            this.dateAndTime = null;
        } else {
            this.dateAndTime = new Date(dateAndTime.getTime());
        }
    }

    public Date getDateAndTime() {
        if (this.dateAndTime == null) {
            return null;
        }
        return new Date(this.dateAndTime.getTime());
    }

    // The certificate whose state is being found out.
    //
    // It is not a criterion: `match` **does not look at it**, and that is of the JDK. It is there
    // so that a `CertStore` provider knows what to point the search at —for example, following the
    // CRL distribution points extension of the certificate— without having to guess it.
    public void setCertificateChecking(X509Certificate cert) {
        this.certChecking = cert;
    }

    public X509Certificate getCertificateChecking() {
        return this.certChecking;
    }

    // Whether the CRL meets all the criteria that were set.
    @Override
    public boolean match(CRL crl) {
        if (!(crl instanceof X509CRL)) {
            return false;
        }
        X509CRL xcrl = (X509CRL) crl;

        // The issuer goes first: it is the criterion that decides whether this CRL even talks about
        // the certificates of interest. It is compared by `X500Principal`, that is, by canonical
        // form.
        if (this.issuerX500Principals != null) {
            if (!this.issuerX500Principals.contains(xcrl.getIssuerX500Principal())) {
                return false;
            }
        }

        if (this.minCRL != null || this.maxCRL != null) {
            byte[] ext = xcrl.getExtensionValue(OID_CRL_NUMBER);
            // A CRL with no number cannot satisfy a criterion about the number. Saying no is the
            // safe side: accepting it would let through precisely the old CRL the criterion wanted
            // to discard.
            if (ext == null) {
                return false;
            }
            BigInteger num;
            try {
                byte[] value = DerReader.unwrapOctetString(ext);
                DerReader d = new DerReader(value, 0, value.length);
                int len = d.expect(DerReader.TAG_INTEGER);
                int from = d.skip(len);
                num = d.readInteger(from, len);
            } catch (IOException e) {
                return false;
            }
            if (this.minCRL != null && num.compareTo(this.minCRL) < 0) {
                return false;
            }
            if (this.maxCRL != null && num.compareTo(this.maxCRL) > 0) {
                return false;
            }
        }

        if (this.dateAndTime != null) {
            Date thisUpdate = xcrl.getThisUpdate();
            Date nextUpdate = xcrl.getNextUpdate();
            // Without `nextUpdate` the CRL does not say until when it is valid, so it cannot be
            // asserted that it covers the instant asked for.
            if (nextUpdate == null) {
                return false;
            }
            if (this.dateAndTime.after(nextUpdate) || this.dateAndTime.before(thisUpdate)) {
                return false;
            }
        }
        return true;
    }

    // A copy the store can keep. The date and the two sets are copied; the rest is immutable.
    // Sharing the sets would be the classic bug: the store keeps the copy and a later `add` over
    // the original would change its criterion.
    @Override
    public Object clone() {
        try {
            X509CRLSelector copyOf = (X509CRLSelector) super.clone();
            copyOf.dateAndTime = this.getDateAndTime();
            if (this.issuerNames != null) {
                copyOf.issuerNames = new HashSet<Object>(this.issuerNames);
                copyOf.issuerX500Principals =
                    new HashSet<javax.security.auth.x500.X500Principal>(this.issuerX500Principals);
            }
            return copyOf;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }

    // The format is not specified; the names of the fields are the JDK's.
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("X509CRLSelector: [\n");
        if (this.issuerNames != null) {
            sb.append("  IssuerNames:\n");
            java.util.Iterator<Object> it = this.issuerNames.iterator();
            while (it.hasNext()) {
                Object o = it.next();
                sb.append("    " + (o instanceof byte[] ? "(DER)" : o.toString()) + "\n");
            }
        }
        if (this.minCRL != null) {
            sb.append("  minCRLNumber: " + this.minCRL.toString() + "\n");
        }
        if (this.maxCRL != null) {
            sb.append("  maxCRLNumber: " + this.maxCRL.toString() + "\n");
        }
        if (this.dateAndTime != null) {
            sb.append("  dateAndTime: " + this.dateAndTime.toString() + "\n");
        }
        if (this.certChecking != null) {
            sb.append("  Certificate being checked: " + this.certChecking.toString() + "\n");
        }
        sb.append("]");
        return sb.toString();
    }
}
