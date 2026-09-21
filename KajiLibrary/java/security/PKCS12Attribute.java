package java.security;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

// An attribute of PKCS#12: an OID and a set of values, kept in their DER form.
//
// ===============================================================================================
// WHY THIS CLASS CAN BE WRITTEN AND HARDLY ANY OTHER OF THIS PACKAGE CAN
// ===============================================================================================
//
// The rest of `java.security` that is missing is blocked by two things this VM does not have:
// entropy of the operating system (`SecureRandom`, and with it the fifteen signatures that name it)
// and some provider that knows RSA or ECDSA. This attribute needs neither of the two: it is **only
// encoding**. An OID, a SET of values, DER. It can be implemented whole and for real, so it is
// implemented whole.
//
// ===============================================================================================
// THE SHAPE
// ===============================================================================================
//
//     SEQUENCE { OBJECT IDENTIFIER, SET OF ANY }
//
// The object is **immutable and is defined by its bytes**: `equals` and `hashCode` compare the DER,
// not the name/value pair. It is the only definition that holds up, because two different encodings
// of the same text are different attributes for whoever signs them afterwards.
//
// The text constructor decides the type of each value by its shape: if it is a string of
// hexadecimal pairs separated by colons --and **at least two** pairs are needed, a loose "01" does
// not count-- it goes as an OCTET STRING; if not, as a UTF8String. A value between square brackets
// and separated by ", " is a list of several.
//
// Careful with an oddity inherited from the JDK that was copied on purpose: the hexadecimal pairs
// go through a {@link BigInteger}, so **the leading zeroes are lost**. "00:01" is encoded as the
// single byte 01 and on rereading it comes back as "01". It is surprising, but changing it would
// give bytes different from the JDK's for the same input, and these bytes end up inside signed
// things.
//
// ===============================================================================================
// WHAT IT DOES NOT DECODE, AND WHY IT IS AN EXCEPTION AND NOT AN ANSWER
// ===============================================================================================
//
// {@link #PKCS12Attribute(byte[])} **rejects** an attribute whose value is a UTCTime or a
// GeneralizedTime. It is not that we do not know how to read the date: the JDK converts those
// values to {@code java.util.Date} and returns its {@code toString()}, and the {@code
// Date.toString()} of this library is different on purpose (it prints the milliseconds, because
// here there is no time zone with which to build an honest wall clock; see java/util/Date.java).
//
// That is, the same bytes would give a different {@code getValue()} here than in any other JVM.
// Between silently returning a value that matches nobody's and failing loudly in the constructor,
// it fails. Whoever runs into it finds out at the moment; the other road does not show until
// something is compared against the value of a real JVM. All the rest of the types --the strings,
// the OIDs, the integers, the booleans, the OCTET STRINGs and the fallback hexadecimal for the odd
// tags-- is decoded just as in the JDK.
public final class PKCS12Attribute implements KeyStore.Entry.Attribute {

    // At least two pairs: it is what the `+` of the group in the JDK says, and it is what separates
    // a hexadecimal value from a text of two characters that happen to be hex digits.
    private static final String HEX_PAIRS = "^[0-9a-fA-F]{2}(:[0-9a-fA-F]{2})+$";

    private static final int TAG_BOOLEAN = 0x01;
    private static final int TAG_INTEGER = 0x02;
    private static final int TAG_OCTET_STRING = 0x04;
    private static final int TAG_OID = 0x06;
    private static final int TAG_UTF8 = 0x0c;
    private static final int TAG_NUMERIC = 0x12;
    private static final int TAG_PRINTABLE = 0x13;
    private static final int TAG_T61 = 0x14;
    private static final int TAG_IA5 = 0x16;
    private static final int TAG_UTC_TIME = 0x17;
    private static final int TAG_GENERALIZED_TIME = 0x18;
    private static final int TAG_VISIBLE = 0x1a;
    private static final int TAG_GENERAL = 0x1b;
    private static final int TAG_BMP = 0x1e;
    private static final int TAG_SEQUENCE = 0x30;
    private static final int TAG_SET = 0x31;

    private String name;

    private String value;

    private final byte[] encoded;

    // -1 marks "not computed yet". A real attribute can have hash 0 and nothing happens: it would
    // be recomputed one time too many, which is cheap, and it never gives an incorrect answer.
    private int hashValue = -1;

    public PKCS12Attribute(String name, String value) {
        if (name == null || value == null) {
            throw new NullPointerException();
        }

        byte[] oid;
        try {
            oid = oidToDer(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Incorrect format: name", e);
        }
        this.name = name;

        // The square brackets mark a list. The length is looked at before indexing because "["
        // alone measures 1 and would be the first and the last character at once.
        int len = value.length();
        String[] values;
        if (len > 1 && value.charAt(0) == '[' && value.charAt(len - 1) == ']') {
            values = value.substring(1, len - 1).split(", ");
        } else {
            values = new String[] { value };
        }
        this.value = value;

        try {
            this.encoded = encode(oid, values);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Incorrect format: value", e);
        }
    }

    /**
     * It is cloned on the way in and on the way out: the attribute is immutable and a `byte[]`
     * shared with whoever built it would not be.
     */
    public PKCS12Attribute(byte[] encoded) {
        if (encoded == null) {
            throw new NullPointerException();
        }
        this.encoded = encoded.clone();
        try {
            parseDer(this.encoded);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Incorrect format: encoded", e);
        }
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public byte[] getEncoded() {
        return encoded.clone();
    }

    /**
     * The identity is the bytes, not the name/value pair: two different DERs that read the same as
     * text are still different attributes for whoever is going to sign them.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PKCS12Attribute)) {
            return false;
        }
        return java.util.Arrays.equals(encoded, ((PKCS12Attribute) obj).encoded);
    }

    @Override
    public int hashCode() {
        int h = hashValue;
        if (h == -1) {
            hashValue = h = java.util.Arrays.hashCode(encoded);
        }
        return h;
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }

    // ---- encoding -------------------------------------------------------------------------

    private static byte[] encode(byte[] oid, String[] values) {
        Buf content = new Buf();
        for (int i = 0; i < values.length; i++) {
            String v = values[i];
            if (Pattern.matches(HEX_PAIRS, v)) {
                // Through BigInteger, just like the JDK: it is what makes "00:01" lose the zero.
                byte[] bytes = new BigInteger(v.replace(":", ""), 16).toByteArray();
                if (bytes.length > 0 && bytes[0] == 0) {
                    byte[] slice = new byte[bytes.length - 1];
                    System.arraycopy(bytes, 1, slice, 0, slice.length);
                    bytes = slice;
                }
                content.tlv(TAG_OCTET_STRING, bytes);
            } else {
                content.tlv(TAG_UTF8, v.getBytes(StandardCharsets.UTF_8));
            }
        }

        Buf attr = new Buf();
        attr.tlv(TAG_OID, oid);
        attr.tlv(TAG_SET, content.bytes());

        Buf outside = new Buf();
        outside.tlv(TAG_SEQUENCE, attr.bytes());
        return outside.bytes();
    }

    /**
     * An OID in dotted text to its DER content (with no tag and no length).
     *
     * <p>The first two arcs go together in a single number, {@code 40*a + b}. It is not a
     * capricious compression: since the first arc can only be worth 0, 1 or 2, and with 0 or 1 the
     * second does not go past 39, the sum can be undone without ambiguity. With arc 2 the second
     * has no ceiling, and that is why the combined number can need several bytes.
     */
    private static byte[] oidToDer(String oid) {
        String[] parts = oid.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("OID with fewer than two arcs: " + oid);
        }
        long[] arcs = new long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            arcs[i] = arc(parts[i]);
        }
        if (arcs[0] > 2) {
            throw new IllegalArgumentException("first arc outside 0..2: " + oid);
        }
        if (arcs[0] < 2 && arcs[1] > 39) {
            throw new IllegalArgumentException("second arc outside 0..39: " + oid);
        }

        Buf b = new Buf();
        b.base128(arcs[0] * 40 + arcs[1]);
        for (int i = 2; i < arcs.length; i++) {
            b.base128(arcs[i]);
        }
        return b.bytes();
    }

    private static long arc(String s) {
        if (s.isEmpty()) {
            throw new IllegalArgumentException("arco vacio");
        }
        long v = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("non-numeric arc: " + s);
            }
            v = v * 10 + (c - '0');
            // The ceiling is artificial but honest: with a `long` no more can be represented, and a
            // silent overflow would give an OID different from the one asked for.
            if (v > (1L << 56)) {
                throw new IllegalArgumentException("arco demasiado grande: " + s);
            }
        }
        return v;
    }

    // ---- reading ------------------------------------------------------------------------------

    private void parseDer(byte[] der) {
        Lec outer = new Lec(der, 0, der.length);
        Lec seq = outer.tlv(TAG_SEQUENCE);
        // On purpose it is NOT demanded that the SEQUENCE exhaust the array: the JDK accepts bytes
        // left over after the attribute and keeps them inside `encoded`, so a `getEncoded()`
        // returns them. Checked against JDK 25; being stricter here would make an attribute the
        // real JVM reads without complaining be rejected by ours.
        byte[] oid = seq.tlv(TAG_OID).rest();
        Lec set = seq.tlv(TAG_SET);
        // Inside the SEQUENCE it is demanded: there has to be exactly the OID and the SET. A third
        // element is rejected by the JDK too.
        seq.requireEnd();

        // It is counted first so that the array can be sized without a list in between.
        int n = 0;
        Lec count = set.copy();
        while (!count.end()) {
            count.skipTlv();
            n++;
        }
        // An empty SET is valid: the JDK accepts it and the value is left as "[]", which is what
        // `Arrays.toString` of an array with no elements gives. Rejecting it would be inventing a
        // rule.
        String[] values = new String[n];
        for (int i = 0; i < n; i++) {
            values[i] = valueOf(set);
        }

        this.name = derToOid(oid);
        this.value = n == 1 ? values[0] : listOf(values);
    }

    private static String valueOf(Lec l) {
        int tag = l.currentTag();
        Lec body = l.tlv(tag);
        byte[] data = body.rest();

        switch (tag) {
            case TAG_OCTET_STRING:
                return hexWithColons(data);
            case TAG_UTF8:
                return new String(data, StandardCharsets.UTF_8);
            case TAG_NUMERIC:
            case TAG_PRINTABLE:
            case TAG_T61:
            case TAG_IA5:
            case TAG_VISIBLE:
            case TAG_GENERAL:
                return new String(data, StandardCharsets.ISO_8859_1);
            case TAG_BMP:
                return new String(data, StandardCharsets.UTF_16BE);
            case TAG_OID:
                return derToOid(data);
            case TAG_INTEGER:
                if (data.length == 0) {
                    throw new IllegalArgumentException("INTEGER vacio");
                }
                return new BigInteger(data).toString();
            case TAG_BOOLEAN:
                if (data.length != 1) {
                    throw new IllegalArgumentException("BOOLEAN of length " + data.length);
                }
                return String.valueOf(data[0] != 0);
            case TAG_UTC_TIME:
            case TAG_GENERALIZED_TIME:
                // See the comment of the class: it is rejected instead of answering a text that
                // would not match that of any other JVM.
                throw new IllegalArgumentException(
                        "value of a time type (tag 0x" + Integer.toHexString(tag)
                                + "): this library gives it no text form, see PKCS12Attribute");
            default:
                // Just like the JDK: what is not recognised comes out as the hexadecimal of its
                // content.
                return hexWithColons(data);
        }
    }

    private static String listOf(String[] values) {
        StringBuilder s = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                s.append(", ");
            }
            s.append(values[i]);
        }
        return s.append(']').toString();
    }

    private static String hexWithColons(byte[] b) {
        StringBuilder s = new StringBuilder(b.length * 3);
        for (int i = 0; i < b.length; i++) {
            if (i > 0) {
                s.append(':');
            }
            s.append(Character.forDigit((b[i] >> 4) & 0xf, 16));
            s.append(Character.forDigit(b[i] & 0xf, 16));
        }
        return s.toString();
    }

    private static String derToOid(byte[] c) {
        if (c.length == 0) {
            throw new IllegalArgumentException("OID vacio");
        }
        StringBuilder s = new StringBuilder();
        int i = 0;
        long first = readBase128(c, i);
        i = endBase128(c, i);
        if (first < 40) {
            s.append('0').append('.').append(first);
        } else if (first < 80) {
            s.append('1').append('.').append(first - 40);
        } else {
            s.append('2').append('.').append(first - 80);
        }
        while (i < c.length) {
            s.append('.').append(readBase128(c, i));
            i = endBase128(c, i);
        }
        return s.toString();
    }

    private static long readBase128(byte[] c, int i) {
        long v = 0;
        while (true) {
            if (i >= c.length) {
                throw new IllegalArgumentException("OID truncado");
            }
            int b = c[i] & 0xff;
            v = (v << 7) | (b & 0x7f);
            if (v > (1L << 56)) {
                throw new IllegalArgumentException("arc too big in the OID");
            }
            if ((b & 0x80) == 0) {
                return v;
            }
            i++;
        }
    }

    private static int endBase128(byte[] c, int i) {
        while ((c[i] & 0x80) != 0) {
            i++;
            if (i >= c.length) {
                throw new IllegalArgumentException("OID truncado");
            }
        }
        return i + 1;
    }

    // ---- two minimal DER helpers -------------------------------------------------------------

    /** A `byte[]` that grows, with just enough for writing DER. */
    private static final class Buf {

        private byte[] a = new byte[64];

        private int n;

        void b2(int b) {
            if (n == a.length) {
                byte[] more = new byte[a.length * 2];
                System.arraycopy(a, 0, more, 0, n);
                a = more;
            }
            a[n++] = (byte) b;
        }

        void all(byte[] b) {
            for (int i = 0; i < b.length; i++) {
                b2(b[i]);
            }
        }

        /**
         * Tag, length and content.
         *
         * <p>The length goes in short form up to 127 and in long form from there on. DER does not
         * allow a choice: for a given length there is a single valid encoding, and that is why the
         * writer always uses the shortest that is enough.
         */
        void tlv(int tag, byte[] content) {
            b2(tag);
            int len = content.length;
            if (len < 128) {
                b2(len);
            } else {
                int octets = 0;
                for (int v = len; v != 0; v >>>= 8) {
                    octets++;
                }
                b2(0x80 | octets);
                for (int i = octets - 1; i >= 0; i--) {
                    b2((len >>> (i * 8)) & 0xff);
                }
            }
            all(content);
        }

        /** An integer in base 128, seven bits per byte, with the high bit on except in the last. */
        void base128(long v) {
            int octets = 1;
            for (long t = v >>> 7; t != 0; t >>>= 7) {
                octets++;
            }
            for (int i = octets - 1; i >= 0; i--) {
                int seven = (int) ((v >>> (i * 7)) & 0x7f);
                b2(i == 0 ? seven : (seven | 0x80));
            }
        }

        byte[] bytes() {
            byte[] r = new byte[n];
            System.arraycopy(a, 0, r, 0, n);
            return r;
        }
    }

    /** A window over the `byte[]` that is consumed. It copies nothing until it has to. */
    private static final class Lec {

        private final byte[] a;

        private int i;

        private final int end;

        Lec(byte[] a, int i, int end) {
            this.a = a;
            this.i = i;
            this.end = end;
        }

        Lec copy() {
            return new Lec(a, i, end);
        }

        boolean end() {
            return i >= end;
        }

        int currentTag() {
            if (i >= end) {
                throw new IllegalArgumentException("a tag was expected and no bytes were left");
            }
            return a[i] & 0xff;
        }

        /** It consumes a TLV of the tag asked for and returns a window over its content. */
        Lec tlv(int expected) {
            if (currentTag() != expected) {
                throw new IllegalArgumentException("the tag expected was 0x"
                        + Integer.toHexString(expected) + " and what came was 0x"
                        + Integer.toHexString(currentTag()));
            }
            i++;
            int len = len();
            if (len > end - i) {
                throw new IllegalArgumentException("largo " + len + " beyond the end");
            }
            Lec inside = new Lec(a, i, i + len);
            i += len;
            return inside;
        }

        void skipTlv() {
            currentTag();
            i++;
            int len = len();
            if (len > end - i) {
                throw new IllegalArgumentException("largo " + len + " beyond the end");
            }
            i += len;
        }

        private int len() {
            if (i >= end) {
                throw new IllegalArgumentException("largo truncado");
            }
            int b = a[i++] & 0xff;
            if (b < 128) {
                return b;
            }
            int octets = b & 0x7f;
            // The indefinite form (0x80) does not exist in DER, and more than four octets does not
            // fit in an int: both are invalid inputs, not cases we know how to handle.
            if (octets == 0 || octets > 4) {
                throw new IllegalArgumentException("largo mal formado");
            }
            int v = 0;
            for (int k = 0; k < octets; k++) {
                if (i >= end) {
                    throw new IllegalArgumentException("largo truncado");
                }
                v = (v << 8) | (a[i++] & 0xff);
            }
            if (v < 0) {
                throw new IllegalArgumentException("largo negativo");
            }
            return v;
        }

        void requireEnd() {
            if (i != end) {
                throw new IllegalArgumentException("sobran " + (end - i) + " bytes");
            }
        }

        byte[] rest() {
            byte[] r = new byte[end - i];
            System.arraycopy(a, i, r, 0, r.length);
            i = end;
            return r;
        }
    }
}
