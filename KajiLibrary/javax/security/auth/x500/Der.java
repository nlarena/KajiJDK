package javax.security.auth.x500;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's javax.security.auth.x500.Der -- the X.501 name in its encoded form.
 *
 * <p>The whole structure is three levels of nesting and it is as well to have it at hand:
 *
 * <pre>
 *   Name  ::= SEQUENCE OF RelativeDistinguishedName
 *   RDN   ::= SET OF AttributeTypeAndValue
 *   ATV   ::= SEQUENCE { type OBJECT IDENTIFIER, value ANY }
 * </pre>
 *
 * <p>The SET in the middle is the surprising one: a step of the name can have **several** pairs,
 * and that is why it is a set and not a value. In practice it almost always has one.
 *
 * <p><strong>The order is the reverse of the text's.</strong> The DER lists the steps from the
 * general to the particular --country first, common name last-- and the text the other way round.
 * Reversing it here is all that separates a correct name from one that looks fine and chains wrong.
 */
final class Der {

    private Der() {
    }

    static final int SEQUENCE = 0x30;
    static final int SET = 0x31;
    static final int OID = 0x06;
    static final int PRINTABLE = 0x13;
    static final int UTF8 = 0x0c;
    static final int IA5 = 0x16;
    static final int T61 = 0x14;
    static final int BMP = 0x1e;
    static final int UNIVERSAL = 0x1c;

    // ---- reading --------------------------------------------------------------------------------

    /** The steps of the name, **already turned around** to the text's order. */
    static X500Principal.Rdn[] readName(byte[] der) throws IOException {
        Cursor c = new Cursor(der, 0, der.length);
        Cursor seq = c.descend(SEQUENCE);
        List<X500Principal.Rdn> rdns = new ArrayList<X500Principal.Rdn>();
        while (seq.hasMore()) {
            rdns.add(readRdn(seq.descend(SET)));
        }
        if (c.hasMore()) {
            throw new IOException("extra bytes after the Name");
        }
        // From the DER's order to the text's.
        X500Principal.Rdn[] out = new X500Principal.Rdn[rdns.size()];
        int i = 0;
        while (i < out.length) {
            out[i] = rdns.get(out.length - 1 - i);
            i = i + 1;
        }
        return out;
    }

    private static X500Principal.Rdn readRdn(Cursor set) throws IOException {
        List<String> types = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        while (set.hasMore()) {
            Cursor atv = set.descend(SEQUENCE);
            types.add(readOid(atv));
            values.add(readAttributeValue(atv.restOfValue()));
            if (atv.hasMore()) {
                throw new IOException("extra bytes in an AttributeTypeAndValue");
            }
        }
        if (types.isEmpty()) {
            throw new IOException("an empty RDN");
        }
        return new X500Principal.Rdn(types.toArray(new String[types.size()]),
                values.toArray(new String[values.size()]));
    }

    private static String readOid(Cursor c) throws IOException {
        byte[] body = c.readBody(OID);
        if (body.length == 0) {
            throw new IOException("empty OID");
        }
        StringBuilder sb = new StringBuilder();
        // The first byte carries **two** arcs: `40*a + b`. It is the only irregularity of the OID
        // encoding, and it comes from the first arc only being able to be 0, 1 or 2.
        int first = body[0] & 0xff;
        sb.append(first / 40).append('.').append(first % 40);
        long acc = 0;
        int i = 1;
        while (i < body.length) {
            int b = body[i] & 0xff;
            acc = (acc << 7) | (long) (b & 0x7f);
            if ((b & 0x80) == 0) {
                sb.append('.').append(acc);
                acc = 0;
            }
            i = i + 1;
        }
        if (acc != 0) {
            throw new IOException("truncated OID");
        }
        return sb.toString();
    }

    /**
     * The value of an attribute, as text.
     *
     * <p>`PrintableString`, `IA5String`, `UTF8String`, `TeletexString` and `UniversalString` are
     * decoded as UTF-8, and `BMPString` as UTF-16 with two bytes per character. A type that is none
     * of those **is not made up**: its hexadecimal form is returned with `#` in front, which is
     * exactly what the JDK shows.
     *
     * <p>The note said the five were read the same because they are bytes. For the first three that
     * holds; the JDK reads `TeletexString` as Latin-1 (a lone {@code 0xE9} is an e with an acute
     * accent, here it is U+FFFD) and does not decode `UniversalString` at all: it shows it in
     * hexadecimal, whereas here its four bytes per character come out as three NULs and the letter.
     */
    static String readAttributeValue(byte[] der) throws IOException {
        if (der.length < 2) {
            throw new IOException("truncated attribute value");
        }
        Cursor c = new Cursor(der, 0, der.length);
        int tag = c.peekTag();
        if (tag == PRINTABLE || tag == UTF8 || tag == IA5 || tag == T61 || tag == UNIVERSAL) {
            byte[] body = c.readBody(tag);
            return new String(body, java.nio.charset.StandardCharsets.UTF_8);
        }
        if (tag == BMP) {
            byte[] body = c.readBody(tag);
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i + 1 < body.length) {
                sb.append((char) (((body[i] & 0xff) << 8) | (body[i + 1] & 0xff)));
                i = i + 2;
            }
            return sb.toString();
        }
        return "#" + toHex(der);
    }

    /**
     * Reads **one** DER value from a stream and returns its bytes, leaving the stream just after
     * it.
     */
    static byte[] readOneValue(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int tag = is.read();
        if (tag < 0) {
            throw new IOException("empty stream");
        }
        out.write(tag);
        int first = is.read();
        if (first < 0) {
            throw new IOException("truncated length");
        }
        out.write(first);
        int len;
        if ((first & 0x80) == 0) {
            len = first;
        } else {
            int n = first & 0x7f;
            if (n == 0 || n > 4) {
                throw new IOException("indefinite or too large length");
            }
            len = 0;
            int i = 0;
            while (i < n) {
                int b = is.read();
                if (b < 0) {
                    throw new IOException("truncated length");
                }
                out.write(b);
                len = (len << 8) | b;
                i = i + 1;
            }
        }
        int readCount = 0;
        while (readCount < len) {
            int b = is.read();
            if (b < 0) {
                throw new IOException("truncated value");
            }
            out.write(b);
            readCount = readCount + 1;
        }
        return out.toByteArray();
    }

    // ---- escritura ------------------------------------------------------------------------------

    /** The name in DER, **turning** the steps **around** to the DER's order. */
    static byte[] writeName(X500Principal.Rdn[] rdns) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        int i = rdns.length - 1;
        while (i >= 0) {
            byte[] rdn = writeRdn(rdns[i]);
            body.write(rdn, 0, rdn.length);
            i = i - 1;
        }
        return wrap(SEQUENCE, body.toByteArray());
    }

    private static byte[] writeRdn(X500Principal.Rdn rdn) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        int i = 0;
        while (i < rdn.types.length) {
            ByteArrayOutputStream atv = new ByteArrayOutputStream();
            byte[] oid = writeOid(rdn.types[i]);
            atv.write(oid, 0, oid.length);
            byte[] val = writeValue(rdn.values[i]);
            atv.write(val, 0, val.length);
            byte[] onePrincipal = wrap(SEQUENCE, atv.toByteArray());
            body.write(onePrincipal, 0, onePrincipal.length);
            i = i + 1;
        }
        return wrap(SET, body.toByteArray());
    }

    private static byte[] writeOid(String oid) {
        String[] arcs = splitArcs(oid);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // The first two arcs go together in one byte, just as when reading.
        out.write(Integer.parseInt(arcs[0]) * 40 + Integer.parseInt(arcs[1]));
        int i = 2;
        while (i < arcs.length) {
            writeBase128(out, Long.parseLong(arcs[i]));
            i = i + 1;
        }
        return wrap(OID, out.toByteArray());
    }

    // Base 128 with the high bit marking "more follows", and the last byte unmarked.
    private static void writeBase128(ByteArrayOutputStream out, long v) {
        if (v == 0) {
            out.write(0);
            return;
        }
        byte[] tmp = new byte[10];
        int n = 0;
        long x = v;
        while (x > 0) {
            tmp[n] = (byte) (x & 0x7f);
            x = x >>> 7;
            n = n + 1;
        }
        int i = n - 1;
        while (i >= 0) {
            out.write(i > 0 ? (tmp[i] | 0x80) : tmp[i]);
            i = i - 1;
        }
    }

    /**
     * The value with the **narrowest** string type that can represent it.
     *
     * <p>`PrintableString` if it fits --letters, digits and a handful of signs-- and `UTF8String`
     * if not. Choosing the narrowest is not stinginess: it is what makes the DER we emit the same
     * as the one the JDK emits for the same name, and that is what allows comparing certificates
     * byte by byte.
     *
     * <p>The note said it was the same as any other implementation's. Not always: the JDK writes
     * {@code EMAILADDRESS} and {@code DC} as `IA5String`, and here {@code EMAILADDRESS=a@b} comes
     * out as `UTF8String`, so those names do not compare byte by byte with the JDK's.
     */
    static byte[] writeValue(String value) {
        // A value that stayed in hexadecimal form is raw DER: it is returned as is.
        if (value.length() > 1 && value.charAt(0) == '#') {
            byte[] rawBytes = fromHex(value.substring(1, value.length()));
            if (rawBytes != null) {
                return rawBytes;
            }
        }
        if (isPrintable(value)) {
            return wrap(PRINTABLE, value.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        }
        return wrap(UTF8, value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    // The `PrintableString` set (X.680): it is small and includes neither `@` nor `_`, which is why
    // an email address always ends up in UTF8String or IA5String.
    private static boolean isPrintable(String s) {
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || " '()+,-./:=?".indexOf(c) >= 0;
            if (!ok) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    // ---- utilities ------------------------------------------------------------------------------

    private static byte[] wrap(int tag, byte[] body) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        int n = body.length;
        if (n < 128) {
            out.write(n);
        } else {
            // Long form: one byte with the number of bytes of the length, and then the length.
            int bytes = n < 256 ? 1 : (n < 65536 ? 2 : (n < 16777216 ? 3 : 4));
            out.write(0x80 | bytes);
            int i = bytes - 1;
            while (i >= 0) {
                out.write((n >>> (8 * i)) & 0xff);
                i = i - 1;
            }
        }
        out.write(body, 0, body.length);
        return out.toByteArray();
    }

    static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < b.length) {
            int v = b[i] & 0xff;
            sb.append(Character.forDigit(v >> 4, 16)).append(Character.forDigit(v & 0xf, 16));
            i = i + 1;
        }
        return sb.toString().toUpperCase();
    }

    private static byte[] fromHex(String h) {
        if (h.length() == 0 || h.length() % 2 != 0) {
            return null;
        }
        byte[] out = new byte[h.length() / 2];
        int i = 0;
        while (i < out.length) {
            int a = Character.digit(h.charAt(2 * i), 16);
            int b = Character.digit(h.charAt(2 * i + 1), 16);
            if (a < 0 || b < 0) {
                return null;
            }
            out[i] = (byte) ((a << 4) | b);
            i = i + 1;
        }
        return out;
    }

    private static String[] splitArcs(String oid) {
        List<String> out = new ArrayList<String>();
        int from = 0;
        int i = 0;
        while (i <= oid.length()) {
            if (i == oid.length() || oid.charAt(i) == '.') {
                out.add(oid.substring(from, i));
                from = i + 1;
            }
            i = i + 1;
        }
        return out.toArray(new String[out.size()]);
    }

    /**
     * A cursor over a stretch of DER.
     *
     * <p>It exists so that reading a nested structure is not index arithmetic: `descend(tag)`
     * returns a cursor over the **content** and advances the outer one, so the nesting of the code
     * follows that of the data.
     */
    private static final class Cursor {
        private final byte[] b;
        private int pos;
        private final int end;

        Cursor(byte[] b, int from, int end) {
            this.b = b;
            this.pos = from;
            this.end = end;
        }

        boolean hasMore() {
            return this.pos < this.end;
        }

        int peekTag() throws IOException {
            if (!this.hasMore()) {
                throw new IOException("truncated DER");
            }
            return this.b[this.pos] & 0xff;
        }

        Cursor descend(int expectedTag) throws IOException {
            int[] r = this.header(expectedTag);
            Cursor inner = new Cursor(this.b, r[0], r[0] + r[1]);
            this.pos = r[0] + r[1];
            return inner;
        }

        byte[] readBody(int expectedTag) throws IOException {
            int[] r = this.header(expectedTag);
            byte[] out = new byte[r[1]];
            int i = 0;
            while (i < out.length) {
                out[i] = this.b[r[0] + i];
                i = i + 1;
            }
            this.pos = r[0] + r[1];
            return out;
        }

        byte[] restOfValue() throws IOException {
            int from = this.pos;
            // The value is measured and **consumed whole** --header and body--, and its bytes are
            // returned with the header on: whoever receives it needs the tag to know what kind of
            // string it is.
            //
            // The `pos = body + len` is not superfluous: `header` leaves the cursor at the **start
            // of the body**, which is what `descend` wants. Without this line only the header was
            // returned and the cursor was left out of place, and the error came out much later as
            // "value runs past its range" on a datum that was fine.
            int[] r = this.header(-1);
            this.pos = r[0] + r[1];
            byte[] out = new byte[this.pos - from];
            int i = 0;
            while (i < out.length) {
                out[i] = this.b[from + i];
                i = i + 1;
            }
            return out;
        }

        // {position of the body, length}. `expectedTag` at -1 accepts any.
        private int[] header(int expectedTag) throws IOException {
            if (this.pos + 1 >= this.end) {
                throw new IOException("truncated DER");
            }
            int tag = this.b[this.pos] & 0xff;
            if (expectedTag >= 0 && tag != expectedTag) {
                throw new IOException("expected tag 0x"
                        + Integer.toHexString(expectedTag) + " but got 0x"
                        + Integer.toHexString(tag));
            }
            int p = this.pos + 1;
            int first = this.b[p] & 0xff;
            p = p + 1;
            int len;
            if ((first & 0x80) == 0) {
                len = first;
            } else {
                int n = first & 0x7f;
                // The indefinite form (`n == 0`) does not exist in DER, only in BER. A length of
                // more than four bytes does not fit in an `int` and no name needs it.
                if (n == 0 || n > 4) {
                    throw new IOException("indefinite or too large length");
                }
                len = 0;
                int i = 0;
                while (i < n) {
                    if (p >= this.end) {
                        throw new IOException("truncated length");
                    }
                    len = (len << 8) | (this.b[p] & 0xff);
                    p = p + 1;
                    i = i + 1;
                }
            }
            if (len < 0 || p + len > this.end) {
                throw new IOException("value runs past its range");
            }
            this.pos = p;
            return new int[] {p, len};
        }
    }
}
