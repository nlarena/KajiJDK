package javax.naming.ldap;

import java.io.IOException;

/**
 * Asks the server to return the results sorted.
 *
 * <h2>Why sort on the server side</h2>
 *
 * <p>Because combined with {@link PagedResultsControl} it is the only way for paging to mean
 * something: without a stable order, "page 2" is not a concept -- two searches may return the
 * same entries in a different order.
 *
 * <p>And because the server knows each attribute's comparison rules; sorting on the client would
 * use Java's, which are not the same. See {@link SortKey}.
 */
public final class SortControl extends BasicControl {

    private static final long serialVersionUID = -1965961680233330744L;

    /** The OID of this control. */
    public static final String OID = "1.2.840.113556.1.4.473";

    /** Ascending by that attribute. */
    public SortControl(String sortBy, boolean criticality) throws IOException {
        this(new SortKey[] { new SortKey(sortBy) }, criticality);
    }

    /** Ascending by those attributes, in that order of priority. */
    public SortControl(String[] sortBy, boolean criticality) throws IOException {
        this(keys(sortBy), criticality);
    }

    /** With full control over direction and comparison rule. */
    public SortControl(SortKey[] sortBy, boolean criticality) throws IOException {
        super(OID, criticality, encode(sortBy));
    }

    private static SortKey[] keys(String[] names) {
        SortKey[] out = new SortKey[names.length];
        for (int i = 0; i < names.length; i++) {
            out[i] = new SortKey(names[i]);
        }
        return out;
    }

    /**
     * {@code SEQUENCE OF SEQUENCE { attributeType, [0] orderingRule OPTIONAL,
     * [1] reverseOrder DEFAULT FALSE }}, from RFC 2891.
     *
     * <p>The context tags {@code 0x80} and {@code 0x81} are not decoration: since both fields are
     * optional, position cannot tell which one is present, so the RFC tells them apart by tag. (An
     * earlier note said by position.)
     */
    private static byte[] encode(SortKey[] keys) {
        java.io.ByteArrayOutputStream body = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < keys.length; i++) {
            SortKey k = keys[i];
            java.io.ByteArrayOutputStream keyBytes = new java.io.ByteArrayOutputStream();
            write(keyBytes, 0x04, utf8(k.getAttributeID()));
            if (k.getMatchingRuleID() != null) {
                write(keyBytes, 0x80, utf8(k.getMatchingRuleID()));
            }
            if (!k.isAscending()) {
                write(keyBytes, 0x81, new byte[] { (byte) 0xFF });
            }
            write(body, 0x30, keyBytes.toByteArray());
        }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        write(out, 0x30, body.toByteArray());
        return out.toByteArray();
    }

    private static byte[] utf8(String s) {
        return s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void write(java.io.ByteArrayOutputStream out, int tag, byte[] data) {
        out.write(tag);
        if (data.length < 0x80) {
            out.write(data.length);
        } else {
            // Long form: how many bytes the length takes, and then the length.
            int n = data.length;
            int bytes = n < 0x100 ? 1 : (n < 0x10000 ? 2 : 3);
            out.write(0x80 | bytes);
            for (int i = bytes - 1; i >= 0; i--) {
                out.write((n >> (8 * i)) & 0xFF);
            }
        }
        out.write(data, 0, data.length);
    }
}
