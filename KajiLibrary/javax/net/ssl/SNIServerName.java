package javax.net.ssl;

import java.util.Arrays;

/**
 * A server name of the SNI extension: its type and its bytes.
 *
 * <h2>What problem SNI solves</h2>
 *
 * <p>One of ordering. At the same IP address there may be many sites, each with its certificate,
 * and the server has to choose which to send <strong>before</strong> the client has said anything
 * about the application — the certificate travels at the start of the handshake, long before any
 * {@code Host:} header. SNI is the client saying which site it comes for, in the first message.
 *
 * <p>The type is an integer from the IANA registry and today there is only one, {@link
 * StandardConstants#SNI_HOST_NAME}. The class is abstract all the same, so that adding another
 * breaks nothing.
 */
public abstract class SNIServerName {

    private final int type;
    private final byte[] encoded;

    /**
     * @throws IllegalArgumentException if the type does not fit in an unsigned byte
     */
    protected SNIServerName(int type, byte[] encoded) {
        if (type < 0 || type > 255) {
            throw new IllegalArgumentException("type out of range: " + String.valueOf(type));
        }
        if (encoded == null) {
            throw new NullPointerException("encoded");
        }
        this.type = type;
        this.encoded = encoded.clone();
    }

    /** The type of name. */
    public final int getType() {
        return this.type;
    }

    /** A copy of the bytes; the internal array is not lent out. */
    public final byte[] getEncoded() {
        return this.encoded.clone();
    }

    /**
     * On the type and the bytes.
     *
     * <p>It is {@code final} in practice although it does not carry the word: two names with the
     * same type and the same bytes <strong>are</strong> the same name, and a subclass that
     * redefined it would break the lookup by equality the server does.
     */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof SNIServerName) {
            SNIServerName o = (SNIServerName) other;
            return this.type == o.type && Arrays.equals(this.encoded, o.encoded);
        }
        return false;
    }

    public int hashCode() {
        return 31 * (17 + this.type) + Arrays.hashCode(this.encoded);
    }

    /** The type and the bytes in hexadecimal. */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.type == StandardConstants.SNI_HOST_NAME) {
            sb.append("host_name: ");
        } else {
            sb.append("type=(").append(String.valueOf(this.type)).append("): ");
        }
        for (int i = 0; i < this.encoded.length; i++) {
            int b = this.encoded[i] & 0xFF;
            if (b < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(b));
        }
        return sb.toString();
    }
}
