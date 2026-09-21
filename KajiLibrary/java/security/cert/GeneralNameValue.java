package java.security.cert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// A `GeneralName` of X.509: one of nine ways of naming something.
//
// ===============================================================================================
// WHAT IT IS AND WHY IT LIVES IN A CLASS OF ITS OWN
// ===============================================================================================
//
//   GeneralName ::= CHOICE {
//       otherName                 [0] OtherName,
//       rfc822Name                [1] IA5String,
//       dNSName                   [2] IA5String,
//       x400Address               [3] ORAddress,
//       directoryName             [4] Name,
//       ediPartyName              [5] EDIPartyName,
//       uniformResourceIdentifier [6] IA5String,
//       iPAddress                 [7] OCTET STRING,
//       registeredID              [8] OBJECT IDENTIFIER }
//
// It appears in three places this package needs --SubjectAltName, IssuerAltName and
// NameConstraints-- and in all three the same thing is needed: reading it, writing it, comparing
// it, and deciding whether a name falls **inside** another. That last one is the one that matters
// and the one that is not obvious, so it has its own method with its own explanation (`contains`).
//
// ===============================================================================================
// WHAT IS UNDERSTOOD AND WHAT IS CARRIED
// ===============================================================================================
//
// Six of the nine forms are really understood: rfc822Name, dNSName, directoryName, URI, iPAddress
// and registeredID. The other three --otherName, x400Address and ediPartyName-- are kept as bytes
// and **are only compared by exact equality**. It is the honest thing: they are structures with
// semantics of their own that hardly anybody uses, and a `contains` invented for them would be
// saying that a name is inside a subtree without knowing it.
//
// That is why `ofString` rejects them, just as the JDK does: there is no agreed text form for
// them.
final class GeneralNameValue {

    static final int OTHER = 0;
    static final int RFC822 = 1;
    static final int DNS = 2;
    static final int X400 = 3;
    static final int DIRECTORY = 4;
    static final int EDI_PARTY = 5;
    static final int URI = 6;
    static final int IP = 7;
    static final int REGISTERED_ID = 8;

    private static final String OID_COMMON_NAME = "2.5.4.3";
    private static final String OID_EMAIL_ADDRESS = "1.2.840.113549.1.9.1";

    private final int type;
    // For the text forms (1, 2, 6, 8). Null in the others.
    private final String text;
    // For iPAddress (the address, or address+mask in a subtree) and for the three opaque ones.
    private final byte[] bytes;
    // For directoryName. The principal is kept and not the bytes because the comparison of X.500
    // names is by canonical form, not by encoding.
    private final javax.security.auth.x500.X500Principal dn;

    private GeneralNameValue(int type, String text, byte[] bytes,
            javax.security.auth.x500.X500Principal dn) {
        this.type = type;
        this.text = text;
        this.bytes = bytes;
        this.dn = dn;
    }

    int type() {
        return this.type;
    }

    /**
     * A name written as text, in the form that corresponds to its type.
     *
     * @throws IOException if the type has no text form, or if the text is not valid for it
     */
    static GeneralNameValue ofString(int type, String name) throws IOException {
        if (name == null) {
            throw new NullPointerException("name is null");
        }
        switch (type) {
            case RFC822:
                if (name.length() == 0) {
                    throw new IOException("RFC822Name must not be empty");
                }
                return new GeneralNameValue(type, name, null, null);
            case DNS:
                checkDns(name);
                return new GeneralNameValue(type, name, null, null);
            case URI:
                // It has to bring a scheme: without it there is no host to compare, and the host is
                // the only thing a name constraint looks at in a URI.
                if (name.indexOf(':') < 0) {
                    throw new IOException("URI name must include a scheme: " + name);
                }
                return new GeneralNameValue(type, name, null, null);
            case REGISTERED_ID:
                DerReader.validateOid(name);
                return new GeneralNameValue(type, name, null, null);
            case IP:
                return new GeneralNameValue(type, null, parseIp(name), null);
            case DIRECTORY:
                try {
                    return new GeneralNameValue(type, null, null,
                        new javax.security.auth.x500.X500Principal(name));
                } catch (IllegalArgumentException e) {
                    throw new IOException("Incorrect AVA format", e);
                }
            default:
                // otherName, x400Address, ediPartyName and any number out of range.
                throw new IOException("unable to parse String names of type " + type);
        }
    }

    /**
     * A name from the DER of its **value**, without the context tag: an IA5String for the text
     * ones, a `Name` for directoryName, an OCTET STRING for iPAddress.
     *
     * <p>It is the form {@code X509CertSelector.addSubjectAlternativeName(int, byte[])} expects,
     * and it is worth saying because the other one --with the tag on-- is the one you would expect.
     */
    static GeneralNameValue ofValueDer(int type, byte[] der) throws IOException {
        DerReader d = new DerReader(der, 0, der.length);
        int tag = d.readTag();
        int len = d.readLength();
        int at = d.skip(len);
        switch (type) {
            case RFC822:
            case DNS:
            case URI:
                if (tag != 0x16) {
                    throw new IOException("expected an IA5String for name type " + type);
                }
                return ofDerText(type, new String(der, at, len, StandardCharsets.US_ASCII));
            case REGISTERED_ID:
                if (tag != DerReader.TAG_OID) {
                    throw new IOException("expected an OBJECT IDENTIFIER");
                }
                return new GeneralNameValue(type, d.readOid(at, len), null, null);
            case IP:
                if (tag != DerReader.TAG_OCTET_STRING) {
                    throw new IOException("expected an OCTET STRING");
                }
                return new GeneralNameValue(type, null, d.copy(at, len), null);
            case DIRECTORY:
                if (tag != DerReader.TAG_SEQUENCE) {
                    throw new IOException("expected a Name");
                }
                return ofDirectory(der);
            default:
                return new GeneralNameValue(type, null, copyOf(der), null);
        }
    }

    /** A directoryName from the DER of its `Name`. */
    static GeneralNameValue ofDirectory(byte[] nameDer) throws IOException {
        try {
            return new GeneralNameValue(DIRECTORY, null, null,
                new javax.security.auth.x500.X500Principal(nameDer));
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid name", e);
        }
    }

    /**
     * A name read from a list --SubjectAltName or a subtree-- where it comes **with** its context
     * tag.
     *
     * @param at    where the complete TLV starts
     * @param total how much it takes, header included
     */
    static GeneralNameValue ofTagged(byte[] buf, int at, int total) throws IOException {
        DerReader d = new DerReader(buf, at, total);
        int tag = d.readTag();
        int len = d.readLength();
        int from = d.skip(len);
        int type = tag & 0x1f;
        switch (type) {
            case RFC822:
            case DNS:
            case URI:
                return ofDerText(type, new String(buf, from, len, StandardCharsets.US_ASCII));
            case REGISTERED_ID:
                return new GeneralNameValue(type, d.readOid(from, len), null, null);
            case IP:
                return new GeneralNameValue(type, null, d.copy(from, len), null);
            case DIRECTORY:
                // [4] is CONSTRUCTED and wraps the whole `Name`, so inside there is another
                // SEQUENCE.
                return ofDirectory(d.copy(from, len));
            default:
                return new GeneralNameValue(type, null, d.copy(at, total), null);
        }
    }

    /**
     * The value as {@code getSubjectAlternativeNames()} returns it: a {@code String} for the text
     * forms, a {@code byte[]} for the others.
     */
    Object storedValue() {
        if (this.type == DIRECTORY) {
            return this.dn.getName();
        }
        if (this.text != null) {
            return this.text;
        }
        return copyOf(this.bytes);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GeneralNameValue)) {
            return false;
        }
        GeneralNameValue other = (GeneralNameValue) o;
        if (other.type != this.type) {
            return false;
        }
        if (this.type == DIRECTORY) {
            // By canonical form: two X.500 names written differently are the same name.
            return this.dn.equals(other.dn);
        }
        if (this.type == DNS || this.type == RFC822 || this.type == URI) {
            // Case does not count in a host name or in a mail domain.
            return this.text.equalsIgnoreCase(other.text);
        }
        if (this.text != null) {
            return this.text.equals(other.text);
        }
        return sameBytes(this.bytes, other.bytes);
    }

    @Override
    public int hashCode() {
        if (this.type == DIRECTORY) {
            return this.type * 31 + this.dn.hashCode();
        }
        if (this.type == DNS || this.type == RFC822 || this.type == URI) {
            return this.type * 31 + this.text.toLowerCase().hashCode();
        }
        if (this.text != null) {
            return this.type * 31 + this.text.hashCode();
        }
        int h = this.type;
        int i = 0;
        while (i < this.bytes.length) {
            h = h * 31 + this.bytes[i];
            i = i + 1;
        }
        return h;
    }

    @Override
    public String toString() {
        return this.type + ":" + this.storedValue();
    }

    /**
     * Whether <b>this</b> name, taken as the base of a subtree, contains {@code name}.
     *
     * <p>It is the operation that decides whether a certificate falls inside what its CA was
     * allowed, so each rule is written with its reason. A name of another type is never contained:
     * that is decided by {@code NameConstraints}, not by this method.
     */
    boolean contains(GeneralNameValue name) {
        if (name.type != this.type) {
            return false;
        }
        switch (this.type) {
            case DNS:
                return containsDns(this.text, name.text);
            case RFC822:
                return containsRfc822(this.text, name.text);
            case URI:
                return containsUri(this.text, name.text);
            case IP:
                return containsIp(this.bytes, name.bytes);
            case DIRECTORY:
                return containsDirectory(this.dn, name.dn);
            default:
                // registeredID and the three opaque ones: equality only. See the note of the class.
                return this.equals(name);
        }
    }

    // The cut goes at the dot, not just anywhere: `acme.com` contains `www.acme.com` but **not**
    // `xacme.com`. Without that condition, whoever registered `malacme.com` would be inside the
    // subtree of `acme.com`, which is exactly the hole the constraints avoid.
    private static boolean containsDns(String base, String name) {
        String b = base.toLowerCase();
        String n = name.toLowerCase();
        // A base with a leading dot comes from certificates that write subdomains that way. It is
        // not the form of the RFC for dNSName, but it is found, and reading it as a suffix is the
        // only thing it can mean.
        if (b.startsWith(".")) {
            return n.endsWith(b);
        }
        if (n.equals(b)) {
            return true;
        }
        return n.length() > b.length() && n.endsWith(b)
            && n.charAt(n.length() - b.length() - 1) == '.';
    }

    // Three forms, and all three different:
    //
    //   - `u@acme.com` (with an at sign) is a mailbox: only that one.
    //   - `.acme.com` (with a leading dot) is the subdomains: `u@sub.acme.com` yes, `u@acme.com`
    //   no.
    //   - `acme.com` (bare) is **that exact host**: `u@acme.com` yes, `u@sub.acme.com` no.
    //
    // The third is the one that surprises, because in dNSName the bare name does cover the
    // subdomains.
    private static boolean containsRfc822(String base, String name) {
        String b = base.toLowerCase();
        String n = name.toLowerCase();
        if (b.indexOf('@') >= 0) {
            return n.equals(b);
        }
        int atSign = n.indexOf('@');
        String host = atSign < 0 ? n : n.substring(atSign + 1);
        if (b.startsWith(".")) {
            // A host that ends in `.acme.com` is a subdomain; bare `acme.com` does not end that
            // way, and that is why it is left out -- which is what the form with the dot means.
            return host.endsWith(b);
        }
        return host.equals(b);
    }

    // Of a URI only the **host** is looked at: the path and the query do not say whose the name is.
    // With a leading dot it is the subdomains; without a dot it is the exact host --and here it
    // does not cover subdomains, unlike dNSName--.
    private static boolean containsUri(String base, String name) {
        String host = hostOf(name);
        if (host == null) {
            return false;
        }
        String b = base.toLowerCase();
        String h = host.toLowerCase();
        if (b.startsWith(".")) {
            return h.endsWith(b);
        }
        return h.equals(b);
    }

    // The host of a URI, without user or port. Null if it has no authority.
    private static String hostOf(String uri) {
        int scheme = uri.indexOf("://");
        if (scheme < 0) {
            return null;
        }
        int from = scheme + 3;
        int end = uri.length();
        int i = from;
        while (i < end) {
            char c = uri.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                end = i;
                break;
            }
            i = i + 1;
        }
        String authority = uri.substring(from, end);
        int atSign = authority.lastIndexOf('@');
        if (atSign >= 0) {
            authority = authority.substring(atSign + 1);
        }
        int colon = authority.lastIndexOf(':');
        if (colon >= 0 && authority.indexOf(']') < colon) {
            authority = authority.substring(0, colon);
        }
        return authority.length() == 0 ? null : authority;
    }

    // In a subtree the address comes with its mask stuck behind: 8 bytes for IPv4 and 32 for IPv6.
    // In a name it comes alone. The bits the mask lets through are compared.
    private static boolean containsIp(byte[] base, byte[] name) {
        if (name == null || base == null) {
            return false;
        }
        if (base.length == name.length) {
            return sameBytes(base, name);
        }
        if (base.length != name.length * 2) {
            return false;
        }
        int n = name.length;
        int i = 0;
        while (i < n) {
            if (((base[i] ^ name[i]) & base[n + i]) != 0) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    // The subtree is a **prefix** of the name in the order of the DER, which is the reverse order
    // to that of the RFC 2253 text. That is why here it is compared by suffix: `o=acme` contains
    // `cn=juan,o=acme`.
    //
    // It is compared over the canonical form --lower case, collapsed spaces-- because two X.500
    // names written differently are the same name, and the cut goes at the comma: in canonical form
    // a comma inside a value comes escaped, so a bare comma always separates.
    private static boolean containsDirectory(javax.security.auth.x500.X500Principal base,
            javax.security.auth.x500.X500Principal name) {
        String b = base.getName(javax.security.auth.x500.X500Principal.CANONICAL);
        String n = name.getName(javax.security.auth.x500.X500Principal.CANONICAL);
        if (b.length() == 0) {
            // The root of the directory contains them all.
            return true;
        }
        if (n.equals(b)) {
            return true;
        }
        return n.length() > b.length() && n.endsWith(b)
            && n.charAt(n.length() - b.length() - 1) == ',';
    }

    // A DNS name: at least one label, labels of letters, digits and hyphens, with no dot at the
    // start or at the end and no two in a row. It is what the JDK validates and that is why
    // `CN=Juan Perez` is not taken for a host name and `CN=x` is.
    private static void checkDns(String name) throws IOException {
        if (name.length() == 0) {
            throw new IOException("DNSName must not be null or empty");
        }
        int i = 0;
        int labelLen = 0;
        while (i < name.length()) {
            char c = name.charAt(i);
            if (c == '.') {
                if (labelLen == 0) {
                    throw new IOException("DNSName with an empty label: " + name);
                }
                labelLen = 0;
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-') {
                labelLen = labelLen + 1;
            } else {
                throw new IOException(
                    "DNSName components must consist of letters, digits, and hyphens");
            }
            i = i + 1;
        }
        if (labelLen == 0) {
            throw new IOException("DNSName with an empty label: " + name);
        }
    }

    /**
     * A text name read from a DER, without the validation of form.
     *
     * <p>The validation is for what the caller <b>writes</b>: a badly written name there is a
     * mistake of theirs and they have to be told. What is inside a certificate already, on the
     * other hand, has to be readable even if it is not entirely conforming -- rejecting it does not
     * fix it, it only leaves the certificate uncompared, which is worse.
     */
    private static GeneralNameValue ofDerText(int type, String text) {
        return new GeneralNameValue(type, text, null, null);
    }

    /**
     * Whether that text serves as a DNS name. It is used by the CN rule of {@code NameConstraints}.
     */
    static boolean looksLikeDns(String text) {
        try {
            checkDns(text);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * An IP address written as text, or an address with a mask for a subtree.
     *
     * <p><b>Noted difference with the JDK</b>: the JDK goes through {@code InetAddress}, which also
     * accepts the abbreviated forms of BSD --{@code "10.0.0"}, {@code "10.1"}-- where the last part
     * fills the bytes that are missing. Here the four parts are demanded. The difference is always
     * towards the safe side: what is rejected here was accepted there with a value hardly anybody
     * predicts rightly.
     */
    private static byte[] parseIp(String name) throws IOException {
        int slash = name.indexOf('/');
        if (slash >= 0) {
            byte[] dir = parseAddress(name.substring(0, slash));
            String tail = name.substring(slash + 1);
            byte[] mask;
            if (tail.indexOf('.') >= 0 || tail.indexOf(':') >= 0) {
                mask = parseAddress(tail);
            } else {
                mask = maskOfBits(dir.length, tail);
            }
            if (mask.length != dir.length) {
                throw new IOException("address and mask are of different families: " + name);
            }
            byte[] out = new byte[dir.length * 2];
            System.arraycopy(dir, 0, out, 0, dir.length);
            System.arraycopy(mask, 0, out, dir.length, mask.length);
            return out;
        }
        return parseAddress(name);
    }

    private static byte[] maskOfBits(int size, String bitCount) throws IOException {
        int n;
        try {
            n = Integer.parseInt(bitCount);
        } catch (NumberFormatException e) {
            throw new IOException("bad prefix length: " + bitCount);
        }
        if (n < 0 || n > size * 8) {
            throw new IOException("bad prefix length: " + bitCount);
        }
        byte[] m = new byte[size];
        int i = 0;
        while (i < size) {
            int inThisOne = n - i * 8;
            if (inThisOne >= 8) {
                m[i] = (byte) 0xff;
            } else if (inThisOne > 0) {
                m[i] = (byte) (0xff << (8 - inThisOne));
            }
            i = i + 1;
        }
        return m;
    }

    private static byte[] parseAddress(String s) throws IOException {
        if (s.indexOf(':') >= 0) {
            return parseIpv6(s);
        }
        String[] parts = split(s, '.');
        if (parts.length != 4) {
            throw new IOException("not an IPv4 address: " + s);
        }
        byte[] b = new byte[4];
        int i = 0;
        while (i < 4) {
            int v = numberAt(parts[i], 10, s);
            if (v < 0 || v > 255) {
                throw new IOException("IPv4 octet out of range: " + s);
            }
            b[i] = (byte) v;
            i = i + 1;
        }
        return b;
    }

    // IPv6 with `::` a single time. The last four bytes in decimal form are not accepted
    // (`::ffff:10.0.0.1`): that form has two encodings of the same value and contributes nothing
    // here.
    private static byte[] parseIpv6(String s) throws IOException {
        int doble = s.indexOf("::");
        if (doble != s.lastIndexOf("::")) {
            throw new IOException("more than one :: in " + s);
        }
        byte[] b = new byte[16];
        String izq = doble < 0 ? s : s.substring(0, doble);
        String der = doble < 0 ? "" : s.substring(doble + 2);
        String[] a = izq.length() == 0 ? new String[0] : split(izq, ':');
        String[] c = der.length() == 0 ? new String[0] : split(der, ':');
        if (doble < 0 && a.length != 8) {
            throw new IOException("not an IPv6 address: " + s);
        }
        if (a.length + c.length > 8) {
            throw new IOException("too many groups in " + s);
        }
        int i = 0;
        while (i < a.length) {
            writeGroup(b, i * 2, numberAt(a[i], 16, s));
            i = i + 1;
        }
        int j = 0;
        while (j < c.length) {
            writeGroup(b, 16 - (c.length - j) * 2, numberAt(c[j], 16, s));
            j = j + 1;
        }
        return b;
    }

    private static void writeGroup(byte[] b, int at, int v) {
        b[at] = (byte) (v >> 8);
        b[at + 1] = (byte) v;
    }

    private static int numberAt(String s, int base, String whole) throws IOException {
        if (s.length() == 0 || s.length() > (base == 16 ? 4 : 3)) {
            throw new IOException("bad address component in " + whole);
        }
        int v = 0;
        int i = 0;
        while (i < s.length()) {
            int d = Character.digit(s.charAt(i), base);
            if (d < 0) {
                throw new IOException("bad address component in " + whole);
            }
            v = v * base + d;
            i = i + 1;
        }
        return v;
    }

    private static String[] split(String s, char sep) {
        int n = 1;
        int i = 0;
        while (i < s.length()) {
            if (s.charAt(i) == sep) {
                n = n + 1;
            }
            i = i + 1;
        }
        String[] out = new String[n];
        int k = 0;
        int from = 0;
        i = 0;
        while (i <= s.length()) {
            if (i == s.length() || s.charAt(i) == sep) {
                out[k] = s.substring(from, i);
                k = k + 1;
                from = i + 1;
            }
            i = i + 1;
        }
        return out;
    }

    /** The OID of the `CN` attribute, for the rule inherited from {@code NameConstraints}. */
    static String commonNameOid() {
        return OID_COMMON_NAME;
    }

    /** The OID of the `EMAILADDRESS` attribute, likewise. */
    static String emailAddressOid() {
        return OID_EMAIL_ADDRESS;
    }

    private static byte[] copyOf(byte[] b) {
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
