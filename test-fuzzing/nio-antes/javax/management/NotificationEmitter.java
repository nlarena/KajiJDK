package javax.management;

/**
 * The {@link NotificationBroadcaster} that can remove one specific registration.
 *
 * <p>The difference matters when the same listener registered several times with different filters:
 * with the inherited method they all go, with this one exactly one goes.
 */
public interface NotificationEmitter extends NotificationBroadcaster {

    /**
     * Removes the registration that matches on all three: listener, filter and handback.
     *
     * <p>The filter and the handback are compared by reference identity in the JDK's practice, not
     * by {@code equals}.
     *
     * @throws ListenerNotFoundException if there is none like that
     */
    void removeNotificationListener(NotificationListener listener, NotificationFilter filter,
                                    Object handback) throws ListenerNotFoundException;
}
