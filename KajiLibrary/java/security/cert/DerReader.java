package java.security.cert;

import java.io.IOException;

// A minimal DER reader, only for the three structures this package really needs to read.
//
// ===============================================================================================
// WHY THIS EXISTS AND HOW FAR IT GOES
// ===============================================================================================
//
// Almost all of `java.security.cert` can be honest without parsing anything: `X509Certificate` is
// abstract, the selectors compare what the certificate returns already, and the factories delegate
// to a provider. But three **concrete** methods of the API are left whose contract is literally
// "read a little bit of DER":
//
//   - `PolicyQualifierInfo`, which decodes a SEQUENCE with an OID in front.
//   - `X509Certificate.getExtendedKeyUsage()`, which decodes a SEQUENCE OF OID.
//   - `X509CRLEntry.getRevocationReason()`, which decodes an ENUMERATED.
//
// All three are **encoding, not cryptography**: there is no decision of trust here. A bug in this
// file produces a badly read OID or an exception, never a signature that is accepted without being
// verified. That is why it can be written without breaking the rule of the house, unlike what would
// happen with a complete certificate parser or with a comparator of X.500 names —where getting it
// wrong **is** a hole, and that is why they are not there.
//
// Since `javax.security.auth.x500.X500Principal` exists in this library a fourth case was added:
// **walking** to the `issuer` or `subject` field and cutting out its bytes. It is worth saying why
// that does not cross the limit above. This file does not interpret the name —it does not parse it,
// it does not compare it, it does not canonicalise it—: it counts fields of a SEQUENCE and returns
// a stretch. The one that understands that stretch is `X500Principal`, which brings its own decoder
// and its own canonical form. Separating it like that is what makes the risky part —comparing two
// names— be in a single place and tested.
//
// What this reader still does not do, and therefore what is not declared in the package:
// `GeneralName` —and with it, the lists of alternative names and the name constraints— and anything
// with an indefinite length.
//
// The rules of DER that matter for not accepting ambiguous encodings are checked: indefinite length
// forbidden, lengths in minimal form, OID components with no leading zeroes.
final class DerReader {

    private final byte[] buf;
    private int pos;
    private final int end;

    DerReader(byte[] buf, int from, int len) {
        this.buf = buf;
        this.pos = from;
        this.end = from + len;
    }

    boolean hasMore() {
        return this.pos < this.end;
    }

    int position() {
        return this.pos;
    }

    int limit() {
        return this.end;
    }

    // It reads the tag byte.
    int readTag() throws IOException {
        if (this.pos >= this.end) {
            throw new IOException("truncated DER: the tag is missing");
        }
        int t = this.buf[this.pos] & 0xff;
        this.pos = this.pos + 1;
        // The long-form tags (the low five bits at 1) do not appear in anything this package reads,
        // and accepting them without knowing how to decode them would be worse than rejecting them.
        if ((t & 0x1f) == 0x1f) {
            throw new IOException("DER: long-form tag not supported");
        }
        return t;
    }

    // It reads the length field and returns how many bytes of content follow.
    int readLength() throws IOException {
        if (this.pos >= this.end) {
            throw new IOException("truncated DER: the length is missing");
        }
        int b0 = this.buf[this.pos] & 0xff;
        this.pos = this.pos + 1;
        if (b0 < 0x80) {
            return b0;
        }
        // 0x80 is the indefinite length of BER. DER forbids it, and accepting it would open the
        // door to the same value having two encodings.
        if (b0 == 0x80) {
            throw new IOException("DER: indefinite length not allowed");
        }
        int n = b0 & 0x7f;
        // More than four bytes of length does not fit in an int, and nothing read here comes close.
        if (n > 4) {
            throw new IOException("DER: largo demasiado grande");
        }
        int v = 0;
        int i = 0;
        while (i < n) {
            if (this.pos >= this.end) {
                throw new IOException("DER truncado: largo incompleto");
            }
            v = (v << 8) | (this.buf[this.pos] & 0xff);
            this.pos = this.pos + 1;
            i = i + 1;
        }
        if (v < 0) {
            throw new IOException("DER: largo demasiado grande");
        }
        return v;
    }

    // It consumes the content of a value and returns where it starts.
    int skip(int len) throws IOException {
        int from = this.pos;
        if (len < 0 || this.pos + len > this.end) {
            throw new IOException("DER truncado: contenido incompleto");
        }
        this.pos = this.pos + len;
        return from;
    }

    // It consumes the next complete value and returns {startOfTheTlv, totalLength, tag}.
    //
    // It is what is needed for cutting out a field **with its header**: a `Name` in DER can only be
    // decoded again if it comes whole, with its SEQUENCE in front.
    int[] nextTlv() throws IOException {
        int start = this.pos;
        int tag = readTag();
        int len = readLength();
        skip(len);
        return new int[] {start, this.pos - start, tag};
    }

    // It checks that the tag is the expected one and returns the length of the content.
    int expect(int tag) throws IOException {
        int t = readTag();
        if (t != tag) {
            throw new IOException("DER: the tag expected was 0x"
                + Integer.toHexString(tag) + " and what came was 0x" + Integer.toHexString(t));
        }
        return readLength();
    }

    // It decodes an OBJECT IDENTIFIER into its dotted notation.
    //
    // The first sub-identifier encodes **two** arcs together as 40*a1 + a2. The trick exists
    // because a1 can only be worth 0, 1 or 2, so there is room to spare; the price is that for a1 =
    // 2 the second arc has no ceiling, and that is why the cut of a1 cannot be made by dividing by
    // 40 without looking at the range.
    String readOid(int from, int len) throws IOException {
        if (len <= 0) {
            throw new IOException("DER: OID vacio");
        }
        int i = from;
        int to = from + len;
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        while (i < to) {
            long v = 0;
            int bytes = 0;
            // A component that starts with 0x80 would have a leading zero: DER demands the shortest
            // encoding, so that is invalid and not simply redundant.
            if ((this.buf[i] & 0xff) == 0x80) {
                throw new IOException("DER: OID component with non-minimal encoding");
            }
            while (true) {
                if (i >= to) {
                    throw new IOException("DER: OID truncado");
                }
                int x = this.buf[i] & 0xff;
                i = i + 1;
                bytes = bytes + 1;
                // Nine groups of seven bits already overshoot a long: we cut before overflowing.
                if (bytes > 9) {
                    throw new IOException("DER: OID component too big");
                }
                v = (v << 7) | (x & 0x7f);
                if ((x & 0x80) == 0) {
                    break;
                }
            }
            if (first) {
                long a1;
                long a2;
                if (v < 40) {
                    a1 = 0;
                    a2 = v;
                } else if (v < 80) {
                    a1 = 1;
                    a2 = v - 40;
                } else {
                    a1 = 2;
                    a2 = v - 80;
                }
                sb.append(a1);
                sb.append('.');
                sb.append(a2);
                first = false;
            } else {
                sb.append('.');
                sb.append(v);
            }
        }
        return sb.toString();
    }

    // It copies a stretch of the buffer.
    byte[] copy(int from, int len) {
        byte[] c = new byte[len];
        System.arraycopy(this.buf, from, c, 0, len);
        return c;
    }

    // It reads a signed INTEGER, in two's complement and big-endian, as DER encodes it.
    java.math.BigInteger readInteger(int from, int len) throws IOException {
        if (len <= 0) {
            throw new IOException("DER: INTEGER vacio");
        }
        return new java.math.BigInteger(copy(from, len));
    }

    // A GeneralizedTime in milliseconds since the epoch.
    //
    // **Only** the form DER forces is accepted: `YYYYMMDDHHMMSSZ`, with the fraction of a second
    // optional and always in UTC. BER also allows the seconds to be omitted and a time offset to be
    // written; that is rejected on purpose, because accepting two encodings of the same instant is
    // just what DER exists to avoid and because a conforming certificate never uses them.
    //
    // The counting of days is Howard Hinnant's, with the year shifted so that February is left at
    // the end: that way the leap day is the last of the cycle and does not have to be treated
    // apart. Everything in integers, without going through any date class.
    static long generalizedTime(byte[] buf, int from, int len) throws IOException {
        if (len < 15) {
            throw new IOException("DER: GeneralizedTime demasiado corto");
        }
        String s = new String(buf, from, len, java.nio.charset.StandardCharsets.US_ASCII);
        if (s.charAt(s.length() - 1) != 'Z') {
            throw new IOException("DER: GeneralizedTime with no Z");
        }
        int year = digitsAt(s, 0, 4);
        int month = digitsAt(s, 4, 2);
        int day = digitsAt(s, 6, 2);
        int hour = digitsAt(s, 8, 2);
        int minute = digitsAt(s, 10, 2);
        int second = digitsAt(s, 12, 2);
        if (month < 1 || month > 12 || day < 1 || day > 31 || hour > 23 || minute > 59
                || second > 60) {
            throw new IOException("DER: GeneralizedTime out of range: " + s);
        }
        long millis = 0;
        // The fraction, if it is there, goes between the seconds and the Z. Up to three digits are
        // read: more precision than a millisecond does not fit in what this method returns.
        if (s.length() > 15) {
            if (s.charAt(14) != '.' && s.charAt(14) != ',') {
                throw new IOException("DER: GeneralizedTime with data left over: " + s);
            }
            int i = 15;
            int scale = 100;
            while (i < s.length() - 1) {
                char c = s.charAt(i);
                if (c < '0' || c > '9') {
                    throw new IOException("DER: non-numeric fraction: " + s);
                }
                if (scale > 0) {
                    millis = millis + (c - '0') * scale;
                    scale = scale / 10;
                }
                i = i + 1;
            }
        }
        long y = year;
        if (month <= 2) {
            y = y - 1;
        }
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153 * (month + (month > 2 ? -3 : 9)) + 2) / 5 + day - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        long days = era * 146097 + doe - 719468;
        return ((days * 24 + hour) * 60 + minute) * 60000L + second * 1000L + millis;
    }

    private static int digitsAt(String s, int from, int count) throws IOException {
        int v = 0;
        int i = 0;
        while (i < count) {
            char c = s.charAt(from + i);
            if (c < '0' || c > '9') {
                throw new IOException("DER: a digit was expected in \"" + s + "\"");
            }
            v = v * 10 + (c - '0');
            i = i + 1;
        }
        return v;
    }

    // Universal tags that are used in this package.
    static final int TAG_INTEGER = 0x02;
    static final int TAG_ENUMERATED = 0x0a;
    static final int TAG_OID = 0x06;
    static final int TAG_OCTET_STRING = 0x04;
    static final int TAG_SEQUENCE = 0x30;

    // It checks that a string is a valid OID in dotted notation, with the same rules the JDK uses
    // when building one.
    //
    // The three rules are not arbitrary and come from how an OID is encoded in DER: the first
    // sub-identifier keeps the first two arcs together as 40*a1 + a2, and that only closes if a1 is
    // worth 0, 1 or 2 —and, when it is worth 0 or 1, if a2 stays below 40—. An OID of a single arc
    // simply cannot be encoded.
    static void validateOid(String oid) throws IOException {
        if (oid == null) {
            throw new NullPointerException("oid is null");
        }
        int parts = 0;
        int i = 0;
        int len = oid.length();
        long first = -1;
        long second = -1;
        while (i <= len) {
            int cut = oid.indexOf('.', i);
            if (cut < 0) {
                cut = len;
            }
            if (cut == i) {
                throw new IOException(
                    "ObjectIdentifier() -- Invalid format: componente vacio en \"" + oid + "\"");
            }
            long v = 0;
            int j = i;
            while (j < cut) {
                char c = oid.charAt(j);
                if (c < '0' || c > '9') {
                    throw new IOException(
                        "ObjectIdentifier() -- Invalid format: \"" + oid + "\"");
                }
                v = v * 10 + (c - '0');
                // It cuts before overflowing; no real OID comes close to this order.
                if (v > 0x7fffffffL) {
                    throw new IOException(
                        "ObjectIdentifier() -- componente demasiado grande en \"" + oid + "\"");
                }
                j = j + 1;
            }
            if (parts == 0) {
                first = v;
            } else if (parts == 1) {
                second = v;
            }
            parts = parts + 1;
            i = cut + 1;
        }
        if (parts < 2) {
            throw new IOException("ObjectIdentifier() -- Must be at least two oid components ");
        }
        if (first > 2) {
            throw new IOException("ObjectIdentifier() -- First oid component is invalid ");
        }
        if (first < 2 && second > 39) {
            throw new IOException("ObjectIdentifier() -- Second oid component is invalid ");
        }
    }

    // It unwraps the OCTET STRING the value of an X.509 extension travels in and returns its
    // content. `getExtensionValue` always hands over that wrapping, never the bare value.
    static byte[] unwrapOctetString(byte[] ext) throws IOException {
        DerReader d = new DerReader(ext, 0, ext.length);
        int len = d.expect(TAG_OCTET_STRING);
        int from = d.skip(len);
        if (d.hasMore()) {
            throw new IOException("DER: extra data after the OCTET STRING");
        }
        return d.copy(from, len);
    }

    // The bytes of the `issuer` or of the `subject` of an X.509 certificate, with their header.
    //
    //   Certificate     ::= SEQUENCE { tbsCertificate, signatureAlgorithm, signatureValue }
    //   TBSCertificate  ::= SEQUENCE { [0] version DEFAULT v1, serialNumber, signature,
    //                                  issuer, validity, subject, ... }
    //
    // The only optional field there is before the issuer is the version, and it comes with an
    // explicit tag `[0]` (0xa0), so it is told apart from an INTEGER without ambiguity. From there
    // on the positions are fixed and counting is enough.
    static byte[] certificateName(byte[] der, boolean subject) throws IOException {
        if (der == null) {
            throw new IOException("the certificate has no encoding");
        }
        DerReader outer = new DerReader(der, 0, der.length);
        int certLen = outer.expect(TAG_SEQUENCE);
        int certStart = outer.position();
        DerReader cert = new DerReader(der, certStart, certLen);
        int tbsLen = cert.expect(TAG_SEQUENCE);
        DerReader tbs = new DerReader(der, cert.position(), tbsLen);

        int[] field = tbs.nextTlv();
        // The version is optional: if the first field is not the `[0]`, we were standing on the
        // serial already and nothing extra has to be consumed.
        if (field[2] == 0xa0) {
            field = tbs.nextTlv();
        }
        if (field[2] != TAG_INTEGER) {
            throw new IOException("DER: the serial number was expected");
        }
        field = tbs.nextTlv();
        if (field[2] != TAG_SEQUENCE) {
            throw new IOException("DER: the signature algorithm was expected");
        }
        int[] issuer = tbs.nextTlv();
        if (issuer[2] != TAG_SEQUENCE) {
            throw new IOException("DER: the name of the issuer was expected");
        }
        if (!subject) {
            return tbs.copy(issuer[0], issuer[1]);
        }
        field = tbs.nextTlv();
        if (field[2] != TAG_SEQUENCE) {
            throw new IOException("DER: the validity period was expected");
        }
        int[] subj = tbs.nextTlv();
        if (subj[2] != TAG_SEQUENCE) {
            throw new IOException("DER: the name of the subject was expected");
        }
        return tbs.copy(subj[0], subj[1]);
    }

    // The bytes of the `issuer` of a CRL, with their header.
    //
    //   CertificateList ::= SEQUENCE { tbsCertList, signatureAlgorithm, signatureValue }
    //   TBSCertList     ::= SEQUENCE { version OPTIONAL, signature, issuer, thisUpdate, ... }
    //
    // Unlike the certificate, here the optional version does **not** carry an explicit tag: it is a
    // bare INTEGER. That is why it is decided by looking at the tag and not by counting: if the
    // first field is an INTEGER it is the version, and if it is a SEQUENCE it is the signature
    // algorithm already.
    static byte[] crlName(byte[] der) throws IOException {
        if (der == null) {
            throw new IOException("the CRL has no encoding");
        }
        DerReader outer = new DerReader(der, 0, der.length);
        int listLen = outer.expect(TAG_SEQUENCE);
        DerReader list = new DerReader(der, outer.position(), listLen);
        int tbsLen = list.expect(TAG_SEQUENCE);
        DerReader tbs = new DerReader(der, list.position(), tbsLen);

        int[] field = tbs.nextTlv();
        if (field[2] == TAG_INTEGER) {
            field = tbs.nextTlv();
        }
        if (field[2] != TAG_SEQUENCE) {
            throw new IOException("DER: the signature algorithm was expected");
        }
        int[] issuer = tbs.nextTlv();
        if (issuer[2] != TAG_SEQUENCE) {
            throw new IOException("DER: the name of the issuer was expected");
        }
        return tbs.copy(issuer[0], issuer[1]);
    }

    // The pairs (OID, value) of an X.500 `Name`, in the order of the DER.
    //
    //   Name ::= SEQUENCE OF RelativeDistinguishedName
    //   RelativeDistinguishedName ::= SET OF AttributeTypeAndValue
    //   AttributeTypeAndValue ::= SEQUENCE { type OBJECT IDENTIFIER, value ANY }
    //
    // It exists for the two rules inherited from NameConstraints: the CN is also checked as a DNS
    // name and the EMAILADDRESS as a mail address. Both need to look inside the name, and
    // `X500Principal` does not allow it -- it only hands over the whole text.
    //
    // A value that is not of a string type is skipped instead of breaking: an odd attribute should
    // not stop the ones that are understood from being read.
    static java.util.List<String[]> attributesOf(byte[] nameDer) throws IOException {
        java.util.List<String[]> out = new java.util.ArrayList<String[]>();
        DerReader outer = new DerReader(nameDer, 0, nameDer.length);
        int nameLen = outer.expect(TAG_SEQUENCE);
        DerReader name = new DerReader(nameDer, outer.position(), nameLen);
        while (name.hasMore()) {
            int setLen = name.expect(0x31);
            DerReader rdn = new DerReader(nameDer, name.position(), setLen);
            name.skip(setLen);
            while (rdn.hasMore()) {
                int avaLen = rdn.expect(TAG_SEQUENCE);
                DerReader ava = new DerReader(nameDer, rdn.position(), avaLen);
                rdn.skip(avaLen);
                int oidLen = ava.expect(TAG_OID);
                int oidAt = ava.skip(oidLen);
                String oid = ava.readOid(oidAt, oidLen);
                int tag = ava.readTag();
                int len = ava.readLength();
                int at = ava.skip(len);
                String text = stringValue(nameDer, tag, at, len);
                if (text != null) {
                    out.add(new String[] {oid, text});
                }
            }
        }
        return out;
    }

    // The text of an attribute value, or null if its tag is not that of a string.
    //
    // UTF8String goes in UTF-8 and the others in ASCII. BMPString and UniversalString --UTF-16 and
    // UTF-32-- are skipped: they appear almost only in old certificates and reading them wrongly
    // would give a name that is not the right one.
    private static String stringValue(byte[] buf, int tag, int at, int len) {
        if (tag == 0x0c) {
            return new String(buf, at, len, java.nio.charset.StandardCharsets.UTF_8);
        }
        if (tag == 0x13 || tag == 0x16 || tag == 0x14 || tag == 0x12) {
            return new String(buf, at, len, java.nio.charset.StandardCharsets.US_ASCII);
        }
        return null;
    }
}
