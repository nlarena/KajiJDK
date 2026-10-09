package com.sun.nio.sctp;

import java.net.SocketAddress;

/**
 * An address of the peer changed state.
 *
 * <h2>Why it exists: multihoming</h2>
 *
 * <p>An SCTP end may have <strong>several addresses</strong> at once, and the association goes
 * on being alive while some one of them works. That is what gives it fault tolerance without
 * reconnecting -- and it is also what has to be notified, because the set of addresses changes
 * while the association is open.
 *
 * <p>One of them is the <em>primary</em>: the one that is used by default.
 * {@link AddressChangeEvent} includes the change of primary for that reason.
 */
public abstract class PeerAddressChangeNotification implements Notification {

    /** What happened to the address. */
    public enum AddressChangeEvent {

        /** It became available again. */
        ADDR_AVAILABLE,
        /** It stopped answering. */
        ADDR_UNREACHABLE,
        /** The peer took it out of the association. */
        ADDR_REMOVED,
        /** The peer added it to the association. */
        ADDR_ADDED,
        /** It became the primary one. */
        ADDR_MADE_PRIMARY,
        /** It was confirmed to be reachable. */
        ADDR_CONFIRMED
    }

    /** For the SCTP implementations. */
    protected PeerAddressChangeNotification() {
    }

    /** The address that changed. */
    public abstract SocketAddress address();

    /** The association it belongs to. */
    public abstract Association association();

    /** Which of the six events it was. */
    public abstract AddressChangeEvent event();
}
