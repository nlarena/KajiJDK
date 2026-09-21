package javax.naming.ldap;

import java.util.EventObject;

/**
 * The event that wraps an {@link UnsolicitedNotification}.
 *
 * <p>{@link #dispatch} is on the event's side and not the listener's, and that is the
 * {@code javax.naming} event pattern: the event knows which listener method it corresponds to, so
 * whoever hands it out does not need an {@code if} per event type.
 */
public class UnsolicitedNotificationEvent extends EventObject {

    private static final long serialVersionUID = -2382603380799883705L;

    private final UnsolicitedNotification notice;

    /**
     * @param src who emitted it
     * @param notice the notification
     */
    public UnsolicitedNotificationEvent(Object src, UnsolicitedNotification notice) {
        super(src);
        this.notice = notice;
    }

    /** The notification. */
    public UnsolicitedNotification getNotification() {
        return this.notice;
    }

    /** Hands it to the listener. */
    public void dispatch(UnsolicitedNotificationListener listener) {
        listener.notificationReceived(this);
    }
}
