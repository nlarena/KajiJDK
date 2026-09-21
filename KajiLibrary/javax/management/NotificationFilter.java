package javax.management;

import java.io.Serializable;

/**
 * Decides, on the emitter's side, which notifications are worth the trip.
 *
 * <p>It is {@code Serializable} for the same reason as {@link QueryExp}: over a remote connection
 * the filter travels to the agent and is evaluated there. Filtering on the client would be
 * filtering after paying the cost the filter exists to avoid.
 */
public interface NotificationFilter extends Serializable {

    /** Whether this notification is delivered to the listener. */
    boolean isNotificationEnabled(Notification notification);
}
