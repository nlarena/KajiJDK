package javax.management.timer;

import java.util.Date;
import java.util.Vector;
import javax.management.InstanceNotFoundException;

/**
 * KajiLibrary's javax.management.timer.TimerMBean -- the clock's management interface.
 *
 * <p>It is a standard MBean, so this interface <b>is</b> the remote API: every method here can be
 * called from a JMX console without knowing anything about the class that implements it.
 *
 * <h2>Why it returns wrappers and {@code Vector}</h2>
 *
 * <p>The getters return {@code Integer}, {@code Long}, {@code Boolean} and not primitives because
 * they have to be able to answer <b>null</b>: asking about an identifier that does not exist is not
 * an error, and with a {@code long} a sentinel value would have to be invented.
 * {@code getNbNotifications} is primitive, because "how many are there" always has an answer.
 *
 * <p>The {@code Vector}s are from 1998 and nobody would choose them today, but an MBean's return
 * type is part of the protocol: changing it for {@code List} breaks every client compiled against
 * the original. It is the same reason the typo in {@link #getNbOccurences} survives.
 */
public interface TimerMBean {

    /** Starts the clock. If it was already active it does nothing. */
    void start();

    /** Stops it. The registrations are <b>not</b> lost: they hold for the next start. */
    void stop();

    /**
     * Registers a notification.
     *
     * @param period milliseconds between repetitions; 0 means once only
     * @param nbOccurences how many times; 0 means forever
     * @param fixedRate true to count from the original date, false from each send
     * @return the identifier it is queried or removed by
     * @throws IllegalArgumentException if the date is null, or the period or the occurrences are
     *     negative
     */
    Integer addNotification(String type, String message, Object userData, Date date, long period,
                            long nbOccurences, boolean fixedRate) throws IllegalArgumentException;

    /** The same, with a fixed-delay clock. */
    Integer addNotification(String type, String message, Object userData, Date date, long period,
                            long nbOccurences) throws IllegalArgumentException;

    /** The same, repeating forever. */
    Integer addNotification(String type, String message, Object userData, Date date, long period)
        throws IllegalArgumentException;

    /** The same, once only. */
    Integer addNotification(String type, String message, Object userData, Date date)
        throws IllegalArgumentException;

    /**
     * Removes a registration.
     *
     * @throws InstanceNotFoundException if that identifier does not exist
     */
    void removeNotification(Integer id) throws InstanceNotFoundException;

    /**
     * Removes all of that type.
     *
     * @throws InstanceNotFoundException if there is none of that type
     */
    void removeNotifications(String type) throws InstanceNotFoundException;

    /** Removes them all. */
    void removeAllNotifications();

    /** How many registrations there are. */
    int getNbNotifications();

    /** The identifiers of all of them. */
    Vector<Integer> getAllNotificationIDs();

    /** Those of that type; empty if there are none, which is not an error. */
    Vector<Integer> getNotificationIDs(String type);

    /** The type of that registration, or null if it does not exist. */
    String getNotificationType(Integer id);

    /** Its message, or null. */
    String getNotificationMessage(Integer id);

    /** Its attached data, or null. */
    Object getNotificationUserData(Integer id);

    /** Its next firing date, or null. */
    Date getDate(Integer id);

    /** Its period in milliseconds, or null. */
    Long getPeriod(Integer id);

    /**
     * How many firings it has left, or null.
     *
     * <p>The name is misspelled --it would be "occurrences"-- and stayed that way: fixing it would
     * change the management API and break the clients.
     */
    Long getNbOccurences(Integer id);

    /** Whether it counts from the original date, or null. */
    Boolean getFixedRate(Integer id);

    /** Whether the overdue ones are sent on start. */
    boolean getSendPastNotifications();

    /** Ver {@link #getSendPastNotifications}. */
    void setSendPastNotifications(boolean value);

    /** Whether it is running. */
    boolean isActive();

    /** Whether there is no registration at all. */
    boolean isEmpty();
}
