package javax.naming.ldap;

import java.io.IOException;

/**
 * Asks for the results in pages.
 *
 * <h2>Why it is needed</h2>
 *
 * <p>A search over a large directory can return hundreds of thousands of entries. Without paging,
 * the server sends them all and the client receives them all -- or the server cuts at its limit
 * and the client does not notice that half is missing.
 *
 * <h2>The cookie, which is how it works</h2>
 *
 * <p>Each response carries a {@link PagedResultsResponseControl} with a <em>cookie</em>: an opaque
 * piece of data that represents "where it was". To ask for the next page you have to send that
 * cookie back.
 *
 * <p>And from there the consequence that surprises: paging is <strong>stateful on the server
 * side</strong>, so you have to walk it to the end or release it explicitly -- per RFC 2696, a
 * request with a page size of 0 and the last cookie --, because abandoned pages take up resources
 * there until they expire. (An earlier note said to release it by sending an empty cookie; an
 * empty cookie starts a new paged search.)
 */
public final class PagedResultsControl extends BasicControl {

    private static final long serialVersionUID = 6684806685736844298L;

    /** The OID of this control. */
    public static final String OID = "1.2.840.113556.1.4.319";

    /**
     * The first page, of that size.
     *
     * @param pageSize how many entries per page; it is a request, the server may give fewer
     * @throws IOException if the control could not be encoded
     */
    public PagedResultsControl(int pageSize, boolean criticality) throws IOException {
        super(OID, criticality, encode(pageSize, null));
    }

    /**
     * The page that follows that cookie.
     *
     * @param cookie the one the previous response carried; {@code null} or empty starts from
     *     scratch
     * @throws IOException if the control could not be encoded
     */
    public PagedResultsControl(int pageSize, byte[] cookie, boolean criticality)
            throws IOException {
        super(OID, criticality, encode(pageSize, cookie));
    }

    /**
     * The control's value: a BER sequence with the size and the cookie.
     *
     * <p>It is encoded by hand because it is a two-field structure and bringing in a whole BER
     * encoder for this would be out of proportion. The form is {@code SEQUENCE { INTEGER size,
     * OCTET STRING cookie }}, as RFC 2696 defines it.
     */
    private static byte[] encode(int pageSize, byte[] cookie) {
        byte[] cookieBytes = cookie == null ? new byte[0] : cookie;
        byte[] sizeBytes = berInteger(pageSize);
        int contentLength = sizeBytes.length + 2 + cookieBytes.length;
        byte[] out = new byte[2 + contentLength];
        int i = 0;
        out[i++] = 0x30;                       // SEQUENCE
        out[i++] = (byte) contentLength;
        System.arraycopy(sizeBytes, 0, out, i, sizeBytes.length);
        i += sizeBytes.length;
        out[i++] = 0x04;                       // OCTET STRING
        out[i++] = (byte) cookieBytes.length;
        System.arraycopy(cookieBytes, 0, out, i, cookieBytes.length);
        return out;
    }

    /** A BER INTEGER, with the minimum number of bytes and in two's complement. */
    private static byte[] berInteger(int v) {
        int bytes = 1;
        int t = v;
        while (t > 127 || t < -128) {
            t = t >> 8;
            bytes++;
        }
        byte[] out = new byte[2 + bytes];
        out[0] = 0x02;
        out[1] = (byte) bytes;
        for (int i = 0; i < bytes; i++) {
            out[2 + i] = (byte) (v >> (8 * (bytes - 1 - i)));
        }
        return out;
    }
}
