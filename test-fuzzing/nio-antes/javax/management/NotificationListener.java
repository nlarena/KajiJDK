package javax.management;

import java.util.EventListener;

/**
 * The one that receives an MBean's notifications.
 *
 * <p>The second argument of {@link #handleNotification} is the key to the design and usually goes
 * unnoticed: it is the object the listener handed over when registering, and it comes back as it
 * was. With that, one listener can serve twenty different sources and know which is which without
 * keeping a map of its own or registering twenty objects.
 */
public interface NotificationListener extends EventListener {

    /**
     * A notification arrives.
     *
     * <p>It is called, in general, on one of the emitter's threads: blocking here slows down the
     * MBean that notifies.
     *
     * @param handback the object handed over when registering, or {@code null}
     */
    void handleNotification(Notification notification, Object handback);
}
