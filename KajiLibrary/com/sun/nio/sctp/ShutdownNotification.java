package com.sun.nio.sctp;

/**
 * The peer started closing the association in an orderly way.
 *
 * <p>In an orderly way means that what was already in flight is delivered all the same: SCTP
 * separates the close from the loss, and this notification is the close's. The loss arrives as
 * an {@link AssociationChangeNotification} with {@code COMM_LOST}.
 */
public abstract class ShutdownNotification implements Notification {

    /** For the SCTP implementations. */
    protected ShutdownNotification() {
    }

    /** The association that is being closed. */
    public abstract Association association();
}
