package com.sun.security.jgss;

/**
 * An entry of authorization data of a Kerberos 5 ticket.
 *
 * <p>A ticket's `AuthorizationData` field is a list of (type, bytes) pairs that the KDC puts in
 * and that the service interprets. The content depends on the type --RFC 4120's `AD-IF-RELEVANT`,
 * Windows's `PAC`-- and this class does not look at it: it hands over the raw bytes and leaves
 * the interpretation to whoever knows.
 *
 * <p>It is immutable, and the `byte[]`s are copied on the way in and on the way out. Without the
 * copy, whoever received the entry could modify the authorization data of an already validated
 * ticket, which is exactly what a datum of authorization cannot allow.
 *
 * @see InquireType#KRB5_GET_AUTHZ_DATA
 */
public final class AuthorizationDataEntry {

    private final int type;
    private final byte[] data;

    /**
     * An entry with that type and those data.
     *
     * @param type the type number, of those RFC 4120 registers
     * @param data the bytes; they are copied
     */
    public AuthorizationDataEntry(int type, byte[] data) {
        this.type = type;
        this.data = data.clone();
    }

    /** The type number. */
    public int getType() {
        return this.type;
    }

    /** A copy of the bytes. */
    public byte[] getData() {
        return this.data.clone();
    }

    public String toString() {
        return "AuthorizationDataEntry: type=" + this.type + ", data=" + this.data.length
                + " bytes";
    }
}
