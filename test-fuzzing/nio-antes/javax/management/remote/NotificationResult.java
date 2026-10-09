package javax.management.remote;

import java.io.Serializable;

/**
 * KajiLibrary's javax.management.remote.NotificationResult -- a batch of notifications and where to
 * go on.
 *
 * <p>It is the answer to a request for pending notifications. It brings the notifications and
 * <b>two</b> sequence numbers, and the difference between them is the only thing to understand
 * about this class:
 *
 * <ul>
 *   <li>{@link #getNextSequenceNumber} is where to ask from next time;
 *   <li>{@link #getEarliestSequenceNumber} is the oldest the server still keeps.
 * </ul>
 *
 * <p>If the client asked from a number <b>lower</b> than that one, it lost notifications: the
 * server discarded them because its buffer filled up while the client was not asking. It is how
 * the loss is detected, and it is what triggers a
 * {@link JMXConnectionNotification#NOTIFS_LOST}.
 */
public class NotificationResult implements Serializable {

    private static final long serialVersionUID = 1191800228721395279L;

    /** The oldest one still there. */
    private final long earliestSequenceNumber;

    /** Where to go on. */
    private final long nextSequenceNumber;

    /** This batch's. */
    private final TargetedNotification[] targetedNotifications;

    /**
     * @throws IllegalArgumentException if the array is null
     */
    public NotificationResult(long earliestSequenceNumber, long nextSequenceNumber,
                              TargetedNotification[] targetedNotifications) {
        if (targetedNotifications == null) {
            throw new IllegalArgumentException("Notifications null");
        }
        this.earliestSequenceNumber = earliestSequenceNumber;
        this.nextSequenceNumber = nextSequenceNumber;
        this.targetedNotifications = targetedNotifications;
    }

    /** The oldest one the server still keeps. See the class note. */
    public long getEarliestSequenceNumber() {
        return this.earliestSequenceNumber;
    }

    /** Where to ask from next time. */
    public long getNextSequenceNumber() {
        return this.nextSequenceNumber;
    }

    /** This batch's. */
    public TargetedNotification[] getTargetedNotifications() {
        return this.targetedNotifications;
    }

    /** The two numbers and how many notifications came. */
    @Override
    public String toString() {
        return "NotificationResult: earliest=" + getEarliestSequenceNumber()
            + "; next=" + getNextSequenceNumber()
            + "; nnotifs=" + this.targetedNotifications.length;
    }
}
