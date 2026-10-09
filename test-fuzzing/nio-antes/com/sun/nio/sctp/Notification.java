package com.sun.nio.sctp;

/**
 * Something that happened to an association and is not a message.
 *
 * <h2>Why the protocol needs this and TCP does not</h2>
 *
 * <p>In TCP the connection's events are seen as effects: the socket closes, a read returns
 * {@code -1}. SCTP has more things to tell -- an address of the peer that stopped answering, a
 * message that could not be delivered, an association that restarted -- and none of them fits
 * in the data stream, because they are not data.
 *
 * <p>They go then over a separate channel, and since the {@code receive} is the only moment the
 * program looks at the channel, the notifications are delivered there, to a
 * {@link NotificationHandler}.
 */
public interface Notification {

    /** The association it happened to; it may be {@code null} if there was not one yet. */
    Association association();
}
