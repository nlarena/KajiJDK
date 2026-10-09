package com.sun.nio.sctp;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * A message could not be delivered and came back.
 *
 * <p>The notable thing is {@link #buffer}: <strong>the message comes back whole</strong>, not
 * only the notice that it failed. It is what allows it to be retried over another address or
 * over another stream without having had to keep it beforehand -- and the reason this
 * notification is not simply an error code.
 */
public abstract class SendFailedNotification implements Notification {

    /** For the SCTP implementations. */
    protected SendFailedNotification() {
    }

    /** The association it was tried to send over. */
    public abstract Association association();

    /** The address it was tried to send to. */
    public abstract SocketAddress address();

    /** The message that could not be delivered, whole. */
    public abstract ByteBuffer buffer();

    /** The error code the stack gave. */
    public abstract int errorCode();

    /** The stream it was tried to send over. */
    public abstract int streamNumber();
}
