package javax.naming.ldap;

import java.io.IOException;

import javax.naming.NamingException;

/**
 * What the server answers to a {@link SortControl}: whether it could sort, and if not, why.
 *
 * <h2>Why you have to look at it</h2>
 *
 * <p>Because a <strong>non-critical</strong> {@link SortControl} the server cannot honour is
 * ignored, and the results arrive unsorted. Without checking this response there is no way to
 * tell "sorted" from "could not".
 *
 * <p>{@link #getAttributeID} says <em>which</em> attribute caused trouble, which is the only thing
 * that allows fixing it: the typical case is asking to sort by an attribute the server has no
 * ordering for, or that does not exist in its schema.
 */
public final class SortResponseControl extends BasicControl {

    private static final long serialVersionUID = 5142939176006310877L;

    /** The OID of this control. */
    public static final String OID = "1.2.840.113556.1.4.474";

    private final int resultCode;
    private final String badAttrId;

    /**
     * Built by the provider from what arrived.
     *
     * @throws IOException if the value could not be decoded
     */
    public SortResponseControl(String id, boolean criticality, byte[] value) throws IOException {
        super(id == null ? OID : id, criticality, value);
        byte[] data = value == null ? new byte[0] : value;
        if (data.length < 5) {
            this.resultCode = 0;
            this.badAttrId = null;
            return;
        }
        // SEQUENCE { sortResult ENUMERATED, attributeType [0] OPTIONAL }
        int i = 0;
        if ((data[i] & 0xFF) != 0x30) {
            throw new IOException("the sort control does not have the expected form");
        }
        i += 2;
        if ((data[i] & 0xFF) != 0x0A) {
            throw new IOException("the sort result is missing");
        }
        i++;
        int n = data[i++] & 0xFF;
        int code = 0;
        for (int k = 0; k < n; k++) {
            code = (code << 8) | (data[i++] & 0xFF);
        }
        this.resultCode = code;
        if (i < data.length && (data[i] & 0xFF) == 0x80) {
            i++;
            int length = data[i++] & 0xFF;
            this.badAttrId = new String(data, i, length,
                    java.nio.charset.StandardCharsets.UTF_8);
        } else {
            this.badAttrId = null;
        }
    }

    /** Whether the server sorted; {@code false} means the results come unsorted. */
    public boolean isSorted() {
        return this.resultCode == 0;
    }

    /** The LDAP result code; {@code 0} is success. */
    public int getResultCode() {
        return this.resultCode;
    }

    /** The attribute that caused trouble, or {@code null} if the server did not say. */
    public String getAttributeID() {
        return this.badAttrId;
    }

    /**
     * The error as an exception, or {@code null} if it went well.
     *
     * <p>Returning it instead of throwing it is deliberate: sorting may have failed and the results
     * still be usable. The caller decides whether that invalidates everything.
     */
    public NamingException getException() {
        if (this.resultCode == 0) {
            return null;
        }
        NamingException e = new NamingException(
                "could not sort; LDAP code " + String.valueOf(this.resultCode)
                + (this.badAttrId == null ? "" : ", atributo " + this.badAttrId));
        return e;
    }
}
