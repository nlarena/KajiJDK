package javax.security.auth.kerberos;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Date;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import javax.security.auth.RefreshFailedException;
import javax.security.auth.Refreshable;

/**
 * KajiLibrary's javax.security.auth.kerberos.KerberosTicket -- a Kerberos ticket.
 *
 * <p>What the KDC gives a client to talk to a service: the ticket's bytes --encrypted with the
 * service's key, so the client cannot read them--, the session key, and the metadata that is
 * readable: whom it is for, until when it is valid, what can be done with it.
 *
 * <h2>The flags</h2>
 *
 * <p>They are thirty-two bits of RFC 4120 and this class exposes the seven that matter to user
 * code: {@link #isForwardable}, {@link #isForwarded}, {@link #isProxiable}, {@link #isProxy},
 * {@link #isPostdated}, {@link #isRenewable} and {@link #isInitial}. {@link #getFlags} gives the
 * full array, always at least thirty-two long, padded with falses if a shorter one was given.
 *
 * <h2>{@link #isCurrent} looks only at the expiry</h2>
 *
 * <p>A ticket is current if it is not destroyed and its end time has not passed. The start time
 * does not count: a postdated ticket that has not started yet is reported current, as in the JDK.
 *
 * <h2>{@link #refresh}</h2>
 *
 * <p>Renewing is asking the KDC for a new ticket with this one, and KajiJDK does not talk to a KDC:
 * a renewable ticket fails to renew with {@link RefreshFailedException} saying why. A non-renewable
 * or destroyed one fails earlier, with the same message as the JDK.
 *
 * <h2>It is destroyed</h2>
 *
 * <p>{@link #destroy} erases the session key and the ticket's bytes. Afterwards, the key and the
 * bytes throw {@link IllegalStateException}; the metadata --client, dates, flags-- return null or
 * false, which is what a ticket that no longer exists can say about itself.
 */
public class KerberosTicket implements Destroyable, Refreshable, Serializable {

    private static final long serialVersionUID = 7395334370157380539L;

    /** How many flags a ticket has. */
    private static final int NUM_FLAGS = 32;

    /** The positions of the seven exposed flags, in RFC 4120's order. */
    private static final int FORWARDABLE_TICKET_FLAG = 1;
    private static final int FORWARDED_TICKET_FLAG = 2;
    private static final int PROXIABLE_TICKET_FLAG = 3;
    private static final int PROXY_TICKET_FLAG = 4;
    private static final int POSTDATED_TICKET_FLAG = 6;
    private static final int RENEWABLE_TICKET_FLAG = 8;
    private static final int INITIAL_TICKET_FLAG = 9;

    /** The ticket's bytes, or null if it was destroyed. */
    private byte[] asn1Encoding;

    /** The session key, or null if it was destroyed. */
    private EncryptionKey sessionKey;

    /** The flags, or null if it was destroyed. */
    private boolean[] flags;

    /** When the client authenticated, or null. */
    private Date authTime;

    /** From when it is valid, or null. */
    private Date startTime;

    /** Until when it is valid. */
    private Date endTime;

    /** Until when it can be renewed, or null. */
    private Date renewTill;

    /** Whom it is for. */
    private KerberosPrincipal client;

    /** For which service. */
    private KerberosPrincipal server;

    /** From which addresses it can be used, or null. */
    private InetAddress[] clientAddresses;

    /** Whether it was already erased. */
    private transient boolean destroyed = false;

    /**
     * A ticket with everything. The arrays and the dates are copied.
     *
     * @param flags the flags; null is none, and a shorter array is padded with falses
     * @throws IllegalArgumentException if the bytes, the client, the server, the key or the end
     *     time is null
     */
    public KerberosTicket(byte[] asn1Encoding, KerberosPrincipal client, KerberosPrincipal server,
                          byte[] sessionKey, int keyType, boolean[] flags, Date authTime,
                          Date startTime, Date endTime, Date renewTill,
                          InetAddress[] clientAddresses) {
        if (sessionKey == null) {
            throw new IllegalArgumentException("Session key for ticket cannot be null");
        }
        if (asn1Encoding == null) {
            throw new IllegalArgumentException("ASN.1 encoding of ticket cannot be null");
        }
        if (client == null) {
            throw new IllegalArgumentException("Client name in ticket cannot be null");
        }
        if (server == null) {
            throw new IllegalArgumentException("Server name in ticket cannot be null");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("End time for ticket validity cannot be null");
        }
        this.asn1Encoding = asn1Encoding.clone();
        this.client = client;
        this.server = server;
        this.sessionKey = new EncryptionKey(sessionKey, keyType);
        if (flags == null) {
            this.flags = new boolean[NUM_FLAGS];
        } else if (flags.length >= NUM_FLAGS) {
            this.flags = flags.clone();
        } else {
            this.flags = new boolean[NUM_FLAGS];
            System.arraycopy(flags, 0, this.flags, 0, flags.length);
        }
        this.authTime = copy(authTime);
        this.startTime = copy(startTime);
        this.endTime = copy(endTime);
        this.renewTill = copy(renewTill);
        this.clientAddresses = clientAddresses == null ? null : clientAddresses.clone();
    }

    /** Whom it is for; null if it was destroyed. */
    public final KerberosPrincipal getClient() {
        return this.client;
    }

    /** For which service; null if it was destroyed. */
    public final KerberosPrincipal getServer() {
        return this.server;
    }

    /**
     * The session key. A new object each time, equal to the earlier ones.
     *
     * @throws IllegalStateException if it is destroyed
     */
    public final SecretKey getSessionKey() {
        checkAlive();
        return new EncryptionKey(this.sessionKey.getEncoded(), this.sessionKey.getKeyType());
    }

    /**
     * The session key's type.
     *
     * @throws IllegalStateException if it is destroyed
     */
    public final int getSessionKeyType() {
        checkAlive();
        return this.sessionKey.getKeyType();
    }

    /**
     * Whether a forwardable ticket for another host can be asked for with it. False if destroyed.
     */
    public final boolean isForwardable() {
        return flag(FORWARDABLE_TICKET_FLAG);
    }

    /** Whether it was obtained by forwarding another. */
    public final boolean isForwarded() {
        return flag(FORWARDED_TICKET_FLAG);
    }

    /** Whether a ticket for another host can be asked for with it. */
    public final boolean isProxiable() {
        return flag(PROXIABLE_TICKET_FLAG);
    }

    /** Whether it is for another host. */
    public final boolean isProxy() {
        return flag(PROXY_TICKET_FLAG);
    }

    /** Whether it starts being valid in the future. */
    public final boolean isPostdated() {
        return flag(POSTDATED_TICKET_FLAG);
    }

    /** Whether it can be renewed. It is the flag, not that there is a renewal time limit. */
    public final boolean isRenewable() {
        return flag(RENEWABLE_TICKET_FLAG);
    }

    /** Whether it was obtained with the password and not with another ticket. */
    public final boolean isInitial() {
        return flag(INITIAL_TICKET_FLAG);
    }

    /** The flags. A copy; null if it was destroyed. */
    public final boolean[] getFlags() {
        return this.flags == null ? null : this.flags.clone();
    }

    /** When the client authenticated; null if not known or destroyed. */
    public final Date getAuthTime() {
        return copy(this.authTime);
    }

    /** From when it is valid; if not given, the authentication time. Null if destroyed. */
    public final Date getStartTime() {
        return copy(this.startTime == null ? this.authTime : this.startTime);
    }

    /** Until when it is valid; null if destroyed. */
    public final Date getEndTime() {
        return copy(this.endTime);
    }

    /** Until when it can be renewed; null if not renewable or destroyed. */
    public final Date getRenewTill() {
        return copy(this.renewTill);
    }

    /** From which addresses it can be used. A copy; null if not restricted or destroyed. */
    public final InetAddress[] getClientAddresses() {
        return this.clientAddresses == null ? null : this.clientAddresses.clone();
    }

    /**
     * The ticket's bytes. A copy.
     *
     * @throws IllegalStateException if it is destroyed
     */
    public final byte[] getEncoded() {
        checkAlive();
        return this.asn1Encoding.clone();
    }

    /** Whether it is not destroyed and not expired. See the class note. */
    @Override
    public boolean isCurrent() {
        return !this.destroyed && System.currentTimeMillis() <= this.endTime.getTime();
    }

    /**
     * Tries to renew it. See the class note: in KajiJDK it always fails.
     *
     * @throws RefreshFailedException always
     */
    @Override
    public void refresh() throws RefreshFailedException {
        if (this.destroyed) {
            throw new RefreshFailedException("A destroyed ticket cannot be renewd.");
        }
        if (!isRenewable()) {
            throw new RefreshFailedException("This ticket is not renewable");
        }
        throw new RefreshFailedException("Failed to renew Kerberos Ticket for client " + this.client
            + " and server " + this.server + " - KajiJDK has no KDC client to renew it with");
    }

    /** Erases the key and the bytes. See the class note. Destroying twice does nothing. */
    @Override
    public void destroy() throws DestroyFailedException {
        if (!this.destroyed) {
            Arrays.fill(this.asn1Encoding, (byte) 0);
            this.asn1Encoding = null;
            this.client = null;
            this.server = null;
            this.sessionKey.destroy();
            this.sessionKey = null;
            this.flags = null;
            this.authTime = null;
            this.startTime = null;
            this.endTime = null;
            this.renewTill = null;
            this.clientAddresses = null;
            this.destroyed = true;
        }
    }

    /** Whether it was already erased. */
    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    /** A readable dump: the bytes in hexadecimal, the principals, the flags and the dates. */
    @Override
    public String toString() {
        if (this.destroyed) {
            return "Destroyed KerberosTicket";
        }
        StringBuilder text = new StringBuilder();
        text.append("Ticket (hex) = \n").append(HexDump.dump(this.asn1Encoding)).append("\n");
        text.append("Client Principal = ").append(this.client).append("\n");
        text.append("Server Principal = ").append(this.server).append("\n");
        text.append("Session Key = ")
            .append(this.sessionKey.toString().substring("EncryptionKey: ".length())).append("\n");
        text.append("Forwardable Ticket ").append(isForwardable()).append("\n");
        text.append("Forwarded Ticket ").append(isForwarded()).append("\n");
        text.append("Proxiable Ticket ").append(isProxiable()).append("\n");
        text.append("Proxy Ticket ").append(isProxy()).append("\n");
        text.append("Postdated Ticket ").append(isPostdated()).append("\n");
        text.append("Renewable Ticket ").append(isRenewable()).append("\n");
        text.append("Initial Ticket ").append(isInitial()).append("\n");
        text.append("Auth Time = ").append(this.authTime).append("\n");
        text.append("Start Time = ").append(this.startTime).append("\n");
        text.append("End Time = ").append(this.endTime).append("\n");
        text.append("Renew Till = ").append(this.renewTill).append("\n");
        text.append("Client Addresses ");
        if (this.clientAddresses == null) {
            text.append(" Null ");
        } else {
            int i = 0;
            while (i < this.clientAddresses.length) {
                text.append("clientAddresses[").append(i).append("] = ")
                    .append(this.clientAddresses[i]);
                i = i + 1;
            }
            text.append("\n");
        }
        return text.toString();
    }

    /** A destroyed one is 17. */
    @Override
    public int hashCode() {
        int result = 17;
        if (this.destroyed) {
            return result;
        }
        result = result * 37 + Arrays.hashCode(this.asn1Encoding);
        result = result * 37 + this.endTime.hashCode();
        result = result * 37 + this.client.hashCode();
        result = result * 37 + this.server.hashCode();
        result = result * 37 + this.sessionKey.hashCode();
        if (this.authTime != null) {
            result = result * 37 + this.authTime.hashCode();
        }
        if (this.startTime != null) {
            result = result * 37 + this.startTime.hashCode();
        }
        if (this.renewTill != null) {
            result = result * 37 + this.renewTill.hashCode();
        }
        // Without addresses the array is null and its hash is zero, but the step is taken all the
        // same.
        result = result * 37 + Arrays.hashCode(this.clientAddresses);
        return result * 37 + Arrays.hashCode(this.flags);
    }

    /** Equal if everything matches; a destroyed one is only equal to itself. */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof KerberosTicket)) {
            return false;
        }
        KerberosTicket that = (KerberosTicket) other;
        if (this.destroyed || that.destroyed) {
            return false;
        }
        if (!Arrays.equals(this.asn1Encoding, that.asn1Encoding)
            || !this.endTime.equals(that.endTime)
            || !this.client.equals(that.client)
            || !this.server.equals(that.server)
            || !this.sessionKey.equals(that.sessionKey)
            || !Arrays.equals(this.flags, that.flags)
            || !Arrays.equals(this.clientAddresses, that.clientAddresses)) {
            return false;
        }
        return sameDate(this.authTime, that.authTime) && sameDate(this.startTime, that.startTime)
            && sameDate(this.renewTill, that.renewTill);
    }

    /** Flag number {@code index}; false if destroyed. */
    private boolean flag(int index) {
        return this.flags != null && this.flags[index];
    }

    /** Throws if it was already destroyed. */
    private void checkAlive() {
        if (this.destroyed) {
            throw new IllegalStateException("This ticket is no longer valid");
        }
    }

    /** A copy of the date, or null. */
    private static Date copy(Date date) {
        return date == null ? null : new Date(date.getTime());
    }

    /** Whether two dates that may be null are the same. */
    private static boolean sameDate(Date a, Date b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}
