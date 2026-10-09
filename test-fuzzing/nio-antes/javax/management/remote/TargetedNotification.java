package javax.management.remote;

import java.io.Serializable;
import javax.management.Notification;

/**
 * KajiLibrary's javax.management.remote.TargetedNotification -- a notification with the number of
 * the listener it goes to.
 *
 * <p>It exists for a network reason. A remote client registers several listeners and the server
 * sends it the notifications in batches; without this number, the filter and the MBean's name
 * would have to be sent too so that the client knew whom to deliver each one to.
 *
 * <p>The server assigns the number when the client registers the listener, and it only makes sense
 * inside that connection.
 *
 * <p>Both fields are {@code final} in fact but the class does not promise immutability: it is a
 * transport object, not a value.
 */
public class TargetedNotification implements Serializable {

    private static final long serialVersionUID = 7676132089779300926L;

    /** The notification. */
    private final Notification notif;

    /** Which listener it goes to. */
    private final Integer id;

    /**
     * @throws IllegalArgumentException if either of the two is null
     */
    public TargetedNotification(Notification notification, Integer listenerID) {
        if (notification == null) {
            throw new IllegalArgumentException("Invalid notification: null");
        }
        if (listenerID == null) {
            throw new IllegalArgumentException("Invalid listener ID: null");
        }
        this.notif = notification;
        this.id = listenerID;
    }

    /** The notification. */
    public Notification getNotification() {
        return this.notif;
    }

    /** The listener's number. */
    public Integer getListenerID() {
        return this.id;
    }

    /** The two things between braces. */
    @Override
    public String toString() {
        return "{" + this.notif + ", " + this.id + "}";
    }
}
