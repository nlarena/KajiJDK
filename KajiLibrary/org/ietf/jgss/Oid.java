package org.ietf.jgss;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's org.ietf.jgss.Oid -- an object identifier, from the world of ASN.1.
 *
 * <p>A list of numbers that names something uniquely and globally: {@code 1.2.840.113554.1.2.2} is
 * Kerberos v5. Registration bodies share out the tree, and that is why no agreements between the
 * parties are needed for two implementations to refer to the same thing.
 *
 * <h2>Two rules that are not guessed</h2>
 *
 * <p>At least <b>two</b> arcs are needed, and the first one can only be 0, 1 or 2. It is not a
 * whim: the DER encoding puts the first two arcs into a single subidentifier as {@code 40 * first +
 * second}, and that only works out if the first one is small. That is why {@code "1"} and {@code
 * "3.1"} are rejected and {@code "0.0"} is accepted.
 *
 * <h2>The encoding</h2>
 *
 * <p>{@link #getDER} returns the <b>complete</b> TLV --tag {@code 0x06}, length, and contents-- and
 * not only the contents. Each subidentifier goes in base 128, with the high bit on in every byte
 * but the last; that way a large arc takes up what it needs and there is no fixed length.
 *
 * <p>{@link #hashCode} comes from those bytes. The concrete value is not the same as the JDK's
 * --its own comes from an internal class of its own-- and it does not have to be: the only thing
 * the contract asks is that two equal ones coincide, and that holds because two equal OIDs have the
 * same encoding.
 */
public class Oid {

    /** The ASN.1 tag of an object identifier. */
    private static final byte TAG = 0x06;

    /** The arcs, in order. */
    private final int[] arcs;

    /** The complete TLV, calculated once on construction. */
    private final byte[] der;

    /**
     * From the dotted form.
     *
     * @throws GSSException with {@link GSSException#FAILURE} if it is not a valid OID
     */
    public Oid(String strOid) throws GSSException {
        if (strOid == null) {
            throw new GSSException(GSSException.FAILURE, 0,
                "Improperly formatted Object Identifier String - null");
        }
        int[] parsed = parse(strOid);
        if (parsed == null) {
            throw new GSSException(GSSException.FAILURE, 0,
                "Improperly formatted Object Identifier String - " + strOid);
        }
        this.arcs = parsed;
        this.der = encode(parsed);
    }

    /**
     * From a stream with the DER encoding.
     *
     * @throws GSSException if the bytes are not an OID
     */
    public Oid(InputStream derOid) throws GSSException {
        if (derOid == null) {
            throw new GSSException(GSSException.FAILURE, 0, "Null DER stream");
        }
        byte[] bytes;
        try {
            bytes = readTlv(derOid);
        } catch (IOException e) {
            throw new GSSException(GSSException.FAILURE, 0,
                "Could not read the DER encoding: " + e.getMessage());
        }
        this.arcs = decode(bytes);
        this.der = bytes;
    }

    /**
     * From the complete DER encoding.
     *
     * @throws GSSException if the bytes are not an OID
     */
    public Oid(byte[] data) throws GSSException {
        if (data == null) {
            throw new GSSException(GSSException.FAILURE, 0, "Null DER encoding");
        }
        byte[] copy = new byte[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        this.arcs = decode(copy);
        this.der = copy;
    }

    /** The dotted form. */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < this.arcs.length) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(this.arcs[i]);
            i = i + 1;
        }
        return sb.toString();
    }

    /** Equality by arcs. */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Oid)) {
            return false;
        }
        int[] theirs = ((Oid) other).arcs;
        if (theirs.length != this.arcs.length) {
            return false;
        }
        int i = 0;
        while (i < this.arcs.length) {
            if (this.arcs[i] != theirs[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /**
     * The complete TLV. A copy, so that nobody modifies it.
     *
     * @throws GSSException never in this implementation; it is in the signature because the JDK
     *     encodes lazily and there it can fail
     */
    public byte[] getDER() throws GSSException {
        byte[] copy = new byte[this.der.length];
        System.arraycopy(this.der, 0, copy, 0, this.der.length);
        return copy;
    }

    /**
     * Whether this OID is in that set.
     *
     * <p>It is what is used to ask "does it support this mechanism", which is the most common
     * operation of the type and the reason why this method exists instead of leaving the loop to
     * the caller.
     */
    public boolean containedIn(Oid[] oids) {
        int i = 0;
        while (i < oids.length) {
            if (this.equals(oids[i])) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /** Over the encoded bytes; see the note of the class. */
    public int hashCode() {
        int result = 1;
        int i = 0;
        while (i < this.der.length) {
            result = 31 * result + this.der[i];
            i = i + 1;
        }
        return result;
    }

    /**
     * Like the string constructor, but with no checked exception.
     *
     * <p>It exists for the {@code NT_*} constants of {@link GSSName}: a field initialiser of an
     * interface cannot catch anything, and those OIDs are literals of this library that cannot
     * fail. It is package-private on purpose -- it is not part of the API and nobody outside sees
     * it.
     *
     * @return null if the string is not an OID, which for a literal from here means a typing
     *     mistake
     */
    static Oid literal(String strOid) {
        try {
            return new Oid(strOid);
        } catch (GSSException e) {
            return null;
        }
    }

    // ---- inside ----------------------------------------------------------------------------

    /** The arcs of a dotted string, or null if it is not valid. See the two rules of the class. */
    private static int[] parse(String text) {
        if (text.length() == 0) {
            return null;
        }
        String[] parts = text.split("\\.", -1);
        if (parts.length < 2) {
            return null;
        }
        int[] out = new int[parts.length];
        int i = 0;
        while (i < parts.length) {
            if (parts[i].length() == 0) {
                return null;
            }
            int value = 0;
            int j = 0;
            while (j < parts[i].length()) {
                char c = parts[i].charAt(j);
                if (c < '0' || c > '9') {
                    return null;
                }
                value = value * 10 + (c - '0');
                if (value < 0) {
                    return null; // it overflowed int
                }
                j = j + 1;
            }
            out[i] = value;
            i = i + 1;
        }
        if (out[0] > 2) {
            return null;
        }
        // With a first arc of 0 or 1, the second cannot go past 39: the two go into one byte.
        if (out[0] < 2 && out[1] > 39) {
            return null;
        }
        return out;
    }

    /** The TLV of those arcs. */
    private static byte[] encode(int[] arcs) {
        List<Byte> content = new ArrayList<Byte>();
        appendBase128(content, arcs[0] * 40 + arcs[1]);
        int i = 2;
        while (i < arcs.length) {
            appendBase128(content, arcs[i]);
            i = i + 1;
        }
        byte[] out = new byte[2 + content.size()];
        out[0] = TAG;
        // The length fits in one byte as long as it is under 128, which is the case of any real
        // OID.
        out[1] = (byte) content.size();
        int j = 0;
        while (j < content.size()) {
            out[2 + j] = content.get(j).byteValue();
            j = j + 1;
        }
        return out;
    }

    /** An arc in base 128, with the high bit on except in the last byte. */
    private static void appendBase128(List<Byte> out, int value) {
        int shift = 28;
        boolean started = false;
        while (shift > 0) {
            int part = (value >>> shift) & 0x7f;
            if (part != 0 || started) {
                out.add(Byte.valueOf((byte) (part | 0x80)));
                started = true;
            }
            shift = shift - 7;
        }
        out.add(Byte.valueOf((byte) (value & 0x7f)));
    }

    /** The arcs of a TLV. */
    private static int[] decode(byte[] tlv) throws GSSException {
        if (tlv.length < 3 || tlv[0] != TAG) {
            throw new GSSException(GSSException.FAILURE, 0, "Not a DER Object Identifier");
        }
        int length = tlv[1] & 0xff;
        if (length > 127 || length != tlv.length - 2) {
            throw new GSSException(GSSException.FAILURE, 0, "Malformed Object Identifier length");
        }
        List<Integer> arcs = new ArrayList<Integer>();
        int i = 2;
        // The first subidentifier carries the first two arcs; see the note of the class.
        int first = readBase128(tlv, i);
        int consumed = base128Length(tlv, i);
        if (consumed < 0) {
            throw new GSSException(GSSException.FAILURE, 0, "Truncated Object Identifier");
        }
        i = i + consumed;
        if (first < 80) {
            arcs.add(Integer.valueOf(first / 40));
            arcs.add(Integer.valueOf(first % 40));
        } else {
            arcs.add(Integer.valueOf(2));
            arcs.add(Integer.valueOf(first - 80));
        }
        while (i < tlv.length) {
            int value = readBase128(tlv, i);
            int used = base128Length(tlv, i);
            if (used < 0) {
                throw new GSSException(GSSException.FAILURE, 0, "Truncated Object Identifier");
            }
            arcs.add(Integer.valueOf(value));
            i = i + used;
        }
        int[] out = new int[arcs.size()];
        int k = 0;
        while (k < arcs.size()) {
            out[k] = arcs.get(k).intValue();
            k = k + 1;
        }
        return out;
    }

    /** The value of the arc that starts at `from`. */
    private static int readBase128(byte[] data, int from) {
        int value = 0;
        int i = from;
        while (i < data.length) {
            value = (value << 7) | (data[i] & 0x7f);
            if ((data[i] & 0x80) == 0) {
                return value;
            }
            i = i + 1;
        }
        return value;
    }

    /** How many bytes that arc takes up, or -1 if it is cut short before finishing. */
    private static int base128Length(byte[] data, int from) {
        int i = from;
        while (i < data.length) {
            if ((data[i] & 0x80) == 0) {
                return i - from + 1;
            }
            i = i + 1;
        }
        return -1;
    }

    /** It reads a complete TLV from the stream: tag, length and contents. */
    private static byte[] readTlv(InputStream in) throws IOException {
        int tag = in.read();
        int length = in.read();
        if (tag < 0 || length < 0) {
            throw new IOException("truncated DER encoding");
        }
        byte[] out = new byte[2 + length];
        out[0] = (byte) tag;
        out[1] = (byte) length;
        int read = 0;
        while (read < length) {
            int n = in.read(out, 2 + read, length - read);
            if (n < 0) {
                throw new IOException("truncated DER encoding");
            }
            read = read + n;
        }
        return out;
    }
}
