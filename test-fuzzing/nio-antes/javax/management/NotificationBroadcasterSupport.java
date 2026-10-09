package javax.management;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * The ready-made implementation of {@link NotificationEmitter}: it is extended or delegated to.
 *
 * <p>Three decisions of this class that are not visible in the signature and are worth knowing:
 *
 * <ul>
 *   <li><b>The list is copy-on-write.</b> It is what lets {@code sendNotification} walk it without
 *       taking the lock while another thread registers or removes listeners. With an ordinary list
 *       you would have to choose between holding the lock during delivery --and being at the mercy
 *       of a slow or reentrant listener-- or copying on every send.
 *   <li><b>The filter is evaluated on the sending thread, delivery may go on another.</b> It is on
 *       purpose: filtering is cheap and discards; dispatching is the expensive part. With the
 *       {@code Executor} of the constructor, delivery leaves the emitter's thread; without it, it
 *       goes on the same one.
 *   <li><b>Filter and handback are compared by identity</b> ({@code ==}), not by {@code equals}. It
 *       is what the JDK does and has to be respected: two equal but distinct handbacks are two
 *       different registrations.
 * </ul>
 */
public class NotificationBroadcasterSupport implements NotificationEmitter {

    /** Shared: always returning the same empty array avoids one allocation per query. */
    private static final MBeanNotificationInfo[] NO_INFO = new MBeanNotificationInfo[0];

    /** Runs the task on the calling thread; it is the behaviour without an {@code Executor}. */
    private static class SameThread implements Executor {
        public void execute(Runnable r) {
            r.run();
        }
    }

    private static final Executor SAME_THREAD = new SameThread();

    /** A registration: the listener/filter/handback triple, which is the unit that gets removed. */
    private static class Registration {
        final NotificationListener listener;
        final NotificationFilter filter;
        final Object handback;

        Registration(NotificationListener listener, NotificationFilter filter, Object handback) {
            this.listener = listener;
            this.filter = filter;
            this.handback = handback;
        }
    }

    private final List<Registration> registrations = new CopyOnWriteArrayList<Registration>();
    private final Executor executor;
    private final MBeanNotificationInfo[] info;

    /** Delivers on the emitter's thread and without declaring which notifications it emits. */
    public NotificationBroadcasterSupport() {
        this(null, (MBeanNotificationInfo[]) null);
    }

    /** Delivers through the {@code Executor}; if it is {@code null}, on the emitter's thread. */
    public NotificationBroadcasterSupport(Executor executor) {
        this(executor, (MBeanNotificationInfo[]) null);
    }

    /** Declares which notifications it emits; delivers on the emitter's thread. */
    public NotificationBroadcasterSupport(MBeanNotificationInfo... info) {
        this(null, info);
    }

    /**
     * The full one.
     *
     * <p>The array is copied on the way in and on the way out of {@link #getNotificationInfo}: it
     * is the only way for what the MBean declares not to change behind the back of whoever queried
     * it.
     */
    public NotificationBroadcasterSupport(Executor executor, MBeanNotificationInfo... info) {
        this.executor = (executor == null) ? SAME_THREAD : executor;
        if (info == null || info.length == 0) {
            this.info = NO_INFO;
        } else {
            MBeanNotificationInfo[] copy = new MBeanNotificationInfo[info.length];
            System.arraycopy(info, 0, copy, 0, info.length);
            this.info = copy;
        }
    }

    /**
     * @param listener cannot be {@code null}
     * @param filter {@code null} means "all"
     * @throws IllegalArgumentException if the listener is {@code null}
     */
    public void addNotificationListener(NotificationListener listener, NotificationFilter filter,
                                        Object handback) {
        if (listener == null) {
            throw new IllegalArgumentException("The listener cannot be null");
        }
        registrations.add(new Registration(listener, filter, handback));
    }

    /**
     * Removes <b>all</b> the registrations of that listener, with any filter and handback.
     *
     * @throws ListenerNotFoundException if there was none
     */
    public void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
        boolean any = false;
        // A copy is walked because `registrations` is copy-on-write and its iterator has no remove.
        for (Registration r : registrations.toArray(new Registration[0])) {
            if (r.listener == listener) {
                registrations.remove(r);
                any = true;
            }
        }
        if (!any) {
            throw new ListenerNotFoundException("The listener was not registered");
        }
    }

    /**
     * Removes <b>one</b> registration: the one that matches on all three by identity.
     *
     * <p>If the same triple was registered twice, this call removes only one. It is like that in
     * the JDK and is consistent with {@code add} not deduplicating.
     *
     * @throws ListenerNotFoundException if there is none that matches
     */
    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter,
                                           Object handback) throws ListenerNotFoundException {
        for (Registration r : registrations) {
            if (r.listener == listener && r.filter == filter && r.handback == handback) {
                registrations.remove(r);
                return;
            }
        }
        throw new ListenerNotFoundException(
            "No registration with that listener, filter and handback");
    }

    /** What this emitter declares it may emit; defensive copy. */
    public MBeanNotificationInfo[] getNotificationInfo() {
        if (info.length == 0) {
            return NO_INFO;
        }
        MBeanNotificationInfo[] copy = new MBeanNotificationInfo[info.length];
        System.arraycopy(info, 0, copy, 0, info.length);
        return copy;
    }

    /**
     * Sends the notification to the listeners whose filter lets it through.
     *
     * <p>If a filter throws, the exception comes out of here unwrapped and without having delivered
     * to the remaining listeners: it is what the JDK does, and hiding it would make a broken filter
     * look like a filter that denies.
     */
    public void sendNotification(Notification notification) {
        if (notification == null) {
            return;
        }
        for (final Registration r : registrations) {
            NotificationFilter f = r.filter;
            if (f != null && !f.isNotificationEnabled(notification)) {
                continue;
            }
            final Notification n = notification;
            executor.execute(new Runnable() {
                public void run() {
                    handleNotification(r.listener, n, r.handback);
                }
            });
        }
    }

    /**
     * The extension point: by default it calls the listener, and it is redefined to wrap the
     * delivery --for example to catch whatever the listener throws so that it does not bring the
     * emitter down.
     */
    protected void handleNotification(NotificationListener listener, Notification notif,
                                      Object handback) {
        listener.handleNotification(notif, handback);
    }
}
