package com.sun.nio.sctp;

/**
 * A {@link NotificationHandler} that hands each notification to the method of its type.
 *
 * <h2>What problem it resolves, and why it is a class and not an interface</h2>
 *
 * <p>Without this, every handler starts with the same chain of {@code instanceof}: asking
 * whether the notification is an association change, whether it is an address change, whether
 * it is a failed send. This class writes it once -- the <strong>overloading</strong> does the
 * handing out -- and each subclass overrides only the types that are of interest to it.
 *
 * <p>It is a class with bodies and not an interface with {@code default} because the five
 * methods have to exist with an implementation that does nothing: whoever attends to a single
 * kind of notification should not have to write four empty methods.
 *
 * <p>They all return {@link HandlerResult#CONTINUE} by default, which is the safe answer: to go
 * on waiting for the message the program asked for. A handler that wants to cut off has to say
 * so.
 *
 * @param <T> the context object that travels from the {@code receive}
 */
public class AbstractNotificationHandler<T> implements NotificationHandler<T> {

    /** For the subclasses. */
    protected AbstractNotificationHandler() {
    }

    /**
     * What attends to a notification that is of none of the four known types.
     *
     * <p>It exists for the same reason as a {@code default} in a {@code switch}: the protocol may
     * grow, and a new notification has to fall somewhere.
     */
    public HandlerResult handleNotification(Notification notification, T attachment) {
        return HandlerResult.CONTINUE;
    }

    /** The association changed state. */
    public HandlerResult handleNotification(AssociationChangeNotification notification, T attachment) {
        return HandlerResult.CONTINUE;
    }

    /** An address of the peer changed state. */
    public HandlerResult handleNotification(PeerAddressChangeNotification notification, T attachment) {
        return HandlerResult.CONTINUE;
    }

    /** A message could not be delivered and came back. */
    public HandlerResult handleNotification(SendFailedNotification notification, T attachment) {
        return HandlerResult.CONTINUE;
    }

    /** The peer started closing the association. */
    public HandlerResult handleNotification(ShutdownNotification notification, T attachment) {
        return HandlerResult.CONTINUE;
    }
}
