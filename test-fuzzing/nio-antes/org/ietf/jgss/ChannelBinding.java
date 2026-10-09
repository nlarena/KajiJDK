package org.ietf.jgss;

import java.net.InetAddress;

/**
 * KajiLibrary's org.ietf.jgss.ChannelBinding -- it ties the authentication to the channel it
 * travels on.
 *
 * <p>It is the defence against the man in the middle who limits himself to <b>forwarding</b>.
 * Without this, somebody can sit in the middle, pass the tokens from one side to the other without
 * touching them, and keep the channel: the two ends authenticate each other correctly, but each one
 * is talking to the intermediary.
 *
 * <p>With this, the two parties put into the exchange a description of the channel --the addresses,
 * and whatever the application wants to add-- and if they do not match, the authentication fails
 * with {@link GSSException#BAD_BINDINGS}. The intermediary cannot make them match because the
 * addresses each end sees are its own.
 *
 * <p>The application data are the most useful of the three fields, and at the same time the least
 * used: that is where, for example, the digest of the TLS certificate of the channel goes. It is
 * what ties the authentication to <b>that</b> connection and not to any one between the same two
 * machines.
 *
 * <p>The object is immutable: the three fields are fixed on construction and there are no setters.
 * It makes sense for something that takes part in a security decision -- if it could be changed
 * after being passed, what was verified would not be what was asked for.
 */
public class ChannelBinding {

    private final InetAddress initiator;

    private final InetAddress acceptor;

    private final byte[] appData;

    /**
     * With the two addresses.
     *
     * @param initAddr the one of the initiator, or null if one does not want to tie to it
     * @param acceptAddr the one of the acceptor, or null
     * @param appData whatever the application wants to add, or null
     */
    public ChannelBinding(InetAddress initAddr, InetAddress acceptAddr, byte[] appData) {
        this.initiator = initAddr;
        this.acceptor = acceptAddr;
        this.appData = copyOf(appData);
    }

    /** Only with the application data. */
    public ChannelBinding(byte[] appData) {
        this(null, null, appData);
    }

    /** The address of the initiator, or null. */
    public InetAddress getInitiatorAddress() {
        return this.initiator;
    }

    /** The one of the acceptor, or null. */
    public InetAddress getAcceptorAddress() {
        return this.acceptor;
    }

    /** The application data, or null. A copy. */
    public byte[] getApplicationData() {
        return copyOf(this.appData);
    }

    /**
     * Equal if the three things match.
     *
     * <p>The data are compared byte by byte, not by identity: two labels with the same contents tie
     * to the same channel, which is the only thing that matters here.
     */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ChannelBinding)) {
            return false;
        }
        ChannelBinding that = (ChannelBinding) other;
        if (!sameAddress(this.initiator, that.initiator)) {
            return false;
        }
        if (!sameAddress(this.acceptor, that.acceptor)) {
            return false;
        }
        if (this.appData == null || that.appData == null) {
            return this.appData == that.appData;
        }
        if (this.appData.length != that.appData.length) {
            return false;
        }
        int i = 0;
        while (i < this.appData.length) {
            if (this.appData[i] != that.appData[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** Coherent with {@link #equals}. */
    public int hashCode() {
        if (this.initiator != null) {
            return this.initiator.hashCode();
        }
        if (this.acceptor != null) {
            return this.acceptor.hashCode();
        }
        if (this.appData == null) {
            return 1;
        }
        int result = 1;
        int i = 0;
        while (i < this.appData.length) {
            result = 31 * result + this.appData[i];
            i = i + 1;
        }
        return result;
    }

    /** Two addresses that may be null. */
    private static boolean sameAddress(InetAddress a, InetAddress b) {
        return (a == null) ? b == null : a.equals(b);
    }

    /** A defensive copy of an array that may be null. */
    private static byte[] copyOf(byte[] data) {
        if (data == null) {
            return null;
        }
        byte[] copy = new byte[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return copy;
    }
}
