package javax.management;

/**
 * An MBean that emits notifications.
 *
 * <p>Its shortcoming is in {@link #removeNotificationListener}: it removes <b>all</b> the
 * registrations of that listener, unable to tell filter or handback apart.
 * {@link NotificationEmitter} exists precisely to fix that, and is what is worth implementing
 * today.
 */
public interface NotificationBroadcaster {

    /**
     * Registers a listener.
     *
     * @param filter if it is {@code null}, all pass
     * @param handback opaque object that comes back on every delivery
     */
    void addNotificationListener(NotificationListener listener, NotificationFilter filter,
                                 Object handback) throws IllegalArgumentException;

    /**
     * Removes all the registrations of the listener.
     *
     * @throws ListenerNotFoundException if it was not registered
     */
    void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException;

    /** Which notifications this MBean may emit. */
    MBeanNotificationInfo[] getNotificationInfo();
}
