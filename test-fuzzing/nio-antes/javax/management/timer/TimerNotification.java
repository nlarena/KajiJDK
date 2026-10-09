package javax.management.timer;

import javax.management.Notification;

/**
 * KajiLibrary's javax.management.timer.TimerNotification -- what a {@link Timer} sends.
 *
 * <p>It adds a single field to {@link Notification}: the identifier of the <b>registration</b> that
 * produced it. It is not the sequence number and the two must not be confused: the sequence number
 * changes on every send, the identifier is the same in every firing of a periodic notification. It
 * is the one that serves to tie what arrives to what was asked for, and to remove it.
 */
public class TimerNotification extends Notification {

    private static final long serialVersionUID = 1798492029603825750L;

    /** The registration's identifier; see the class note. */
    private Integer notificationID;

    /**
     * @param type the type whoever registered the notification chose
     * @param source the {@link Timer} that sends it
     * @param sequenceNumber changes on every send
     * @param id the registration's identifier, constant between firings
     */
    public TimerNotification(String type, Object source, long sequenceNumber, long timeStamp,
                             String msg, Integer id) {
        super(type, source, sequenceNumber, timeStamp, msg);
        this.notificationID = id;
    }

    /** The registration's identifier. See the class note. */
    public Integer getNotificationID() {
        return this.notificationID;
    }
}
