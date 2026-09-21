package javax.naming.ldap;

import java.io.IOException;

/**
 * What the server answers to a {@link PagedResultsControl}: how many entries there are and where
 * it was.
 *
 * <p>{@link #getCookie} is what has to be sent back to ask for the next page. An
 * <strong>empty</strong> cookie means there is no more -- it is the end of the walk, not an error.
 *
 * <p>{@link #getResultSize} often comes as zero: the estimated total is optional in RFC 2696 and
 * computing it may cost the server as much as the whole search. Better not to rely on it.
 */
public final class PagedResultsResponseControl extends BasicControl {

    private static final long serialVersionUID = -8819778744844514666L;

    /** The OID of this control. */
    public static final String OID = "1.2.840.113556.1.4.319";

    private final int resultSize;
    private final byte[] cookie;

    /**
     * Built by the provider from what arrived.
     *
     * @throws IOException if the value could not be decoded
     */
    public PagedResultsResponseControl(String id, boolean criticality, byte[] value)
            throws IOException {
        super(id == null ? OID : id, criticality, value);
        int[] pos = new int[] { 0 };
        byte[] data = value == null ? new byte[0] : value;
        if (data.length == 0) {
            this.resultSize = 0;
            this.cookie = new byte[0];
            return;
        }
        // SEQUENCE { INTEGER size, OCTET STRING cookie } -- the same form
        // `PagedResultsControl` builds, read the other way.
        expect(data, pos, 0x30);
        length(data, pos);
        expect(data, pos, 0x02);
        int nSize = length(data, pos);
        int size = 0;
        for (int i = 0; i < nSize; i++) {
            size = (size << 8) | (data[pos[0]++] & 0xFF);
        }
        this.resultSize = size;
        expect(data, pos, 0x04);
        int nCookie = length(data, pos);
        byte[] c = new byte[nCookie];
        System.arraycopy(data, pos[0], c, 0, nCookie);
        this.cookie = c;
    }

    private static void expect(byte[] b, int[] pos, int tag) throws IOException {
        if (pos[0] >= b.length || (b[pos[0]] & 0xFF) != tag) {
            throw new IOException("the paged results control does not have the expected form");
        }
        pos[0]++;
    }

    /**
     * A BER length: the short form, or the long form with any number of length bytes (they are not
     * checked against overflow). An earlier note said only a one-byte long form was read.
     */
    private static int length(byte[] b, int[] pos) throws IOException {
        if (pos[0] >= b.length) {
            throw new IOException("the paged results control is truncated");
        }
        int n = b[pos[0]++] & 0xFF;
        if (n < 0x80) {
            return n;
        }
        int bytes = n & 0x7F;
        int out = 0;
        for (int i = 0; i < bytes; i++) {
            out = (out << 8) | (b[pos[0]++] & 0xFF);
        }
        return out;
    }

    /** The estimated total, or {@code 0} if the server did not report it. */
    public int getResultSize() {
        return this.resultSize;
    }

    /** Where it was; empty means there are no more pages. */
    public byte[] getCookie() {
        return this.cookie.length == 0 ? null : this.cookie.clone();
    }
}
