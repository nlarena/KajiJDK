package javax.management.timer;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.Vector;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.timer.Timer -- the clock that sends notifications.
 *
 * <p>It is an MBean registered in an agent and asked for notices at a given date, with or without
 * repetition. It serves so that a JMX client can schedule something <b>on the agent's side</b>
 * without leaving a thread of its own waiting on the other side of the network.
 *
 * <h2>Past dates</h2>
 *
 * <p>It is the part of the behaviour that cannot be guessed. A notification registered for a date
 * that has already passed --because the clock was stopped, or because it was registered with an
 * old date-- is resolved on start according to {@link #getSendPastNotifications}:
 *
 * <ul>
 *   <li>if it is false, the one-shot one is <b>dropped</b> without notice and removed from the
 *       table; the periodic one runs its date forward to the first one in the future;
 *   <li>if it is true, it is sent <b>once</b> and then follows the same path.
 * </ul>
 *
 * <p>The one-shot one that gets dropped is never scheduled, so it <b>cannot</b> come out.
 *
 * <p>It is sent once and not once per missed repetition: a clock stopped for two days with a
 * one-minute period would fire almost three thousand notices at once, and none of them would be
 * of any use. For the same reason the repetitions that were skipped are <b>not</b> subtracted
 * from {@code nbOccurences}: what was asked for was "tell me this many times", not "this many
 * clock slots".
 *
 * <p>That is a deliberate divergence from the JDK, and worth knowing. The JDK's
 * {@code sendPastNotifications} loops while the date is in the past: with the flag on it sends
 * <b>one notification per missed occurrence</b> and decrements {@code nbOccurences} on each,
 * and with the flag off it only advances the date (removing the one-shot one). (An earlier note
 * said the JDK has a race there between scheduling the overdue notification and removing it;
 * reading the JDK 25 sources, it does not -- the whole loop runs before any alarm is started.)
 *
 * <h2>The thread is not a daemon</h2>
 *
 * <p>While the clock is active, its thread keeps the virtual machine alive. It is on purpose and
 * has to be known: a program that starts a {@link Timer} and does not stop it <b>does not
 * finish</b>. The alternative --a daemon thread-- would lose notices just when the program is
 * shutting down, which is when they usually matter.
 *
 * <h2>Fixed rate versus fixed delay</h2>
 *
 * <p>With {@code fixedRate} true the next date is computed from the <b>previously scheduled</b>
 * one, so the average rate is kept even if one firing comes out late. With false it is computed
 * from the moment it went out, so a delay is carried along. The first serves for sampling over
 * time; the second, for leaving a guaranteed gap between two heavy tasks.
 */
public class Timer extends NotificationBroadcasterSupport implements TimerMBean, MBeanRegistration {

    /** One second, in milliseconds. */
    public static final long ONE_SECOND = 1000;

    /** One minute. */
    public static final long ONE_MINUTE = 60 * ONE_SECOND;

    /** One hour. */
    public static final long ONE_HOUR = 60 * ONE_MINUTE;

    /** One day. */
    public static final long ONE_DAY = 24 * ONE_HOUR;

    /** One week. */
    public static final long ONE_WEEK = 7 * ONE_DAY;

    /** The type declared in {@link #getNotificationInfo}. */
    private static final String NOTIFICATION_CLASS = "javax.management.timer.TimerNotification";

    /** The registrations, in registration order. */
    private final Map<Integer, Registration> table = new LinkedHashMap<Integer, Registration>();

    /** The next identifier. It goes back to 1 when the table is left empty. */
    private int nextId = 1;

    /** The sequence number, which advances on every <b>send</b>. */
    private long sequence = 1;

    /** See the class note about past dates. */
    private boolean sendPastNotifications = false;

    /** Null while the clock is stopped. */
    private java.util.Timer engine;

    /** The agent it is registered in, or null. */
    private MBeanServer server;

    /** The name it was registered under, or null. */
    private ObjectName objectName;

    /** A stopped clock with no registrations. */
    public Timer() {
    }

    // ---- MBeanRegistration -----------------------------------------------------------------

    /** It keeps the agent and the name; it starts nothing. */
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        this.server = server;
        this.objectName = name;
        return name;
    }

    /** Nothing to do. */
    public void postRegister(Boolean registrationDone) {
    }

    /** Nothing to do: stopping early would leave notices unsent if the unregistration failed. */
    public void preDeregister() throws Exception {
    }

    /** Stops the clock: there is no longer anyone to notify. */
    public void postDeregister() {
        stop();
    }

    /**
     * What this MBean sends.
     *
     * <p>The types come from the registrations there are <b>right now</b>, sorted. They cannot be a
     * fixed list because whoever registers chooses them, not this class.
     */
    public synchronized MBeanNotificationInfo[] getNotificationInfo() {
        TreeSet<String> types = new TreeSet<String>();
        for (Registration r : this.table.values()) {
            types.add(r.type);
        }
        String[] asArray = types.toArray(new String[types.size()]);
        return new MBeanNotificationInfo[] {
            new MBeanNotificationInfo(asArray, NOTIFICATION_CLASS, "Notification sent by Timer MBean")
        };
    }

    // ---- starting and stopping ----------------------------------------------------------

    /**
     * Starts the clock.
     *
     * <p>This is where past dates are resolved; see the class note. If it was already active it
     * does nothing, not even reschedule.
     */
    public synchronized void start() {
        if (this.engine != null) {
            return;
        }
        this.engine = new java.util.Timer("timer-mbean", false);
        long now = System.currentTimeMillis();
        List<Registration> all = new ArrayList<Registration>(this.table.values());
        for (Registration r : all) {
            if (r.date.getTime() > now) {
                schedule(r);
            } else {
                catchUp(r, now);
            }
        }
        if (this.table.isEmpty()) {
            this.nextId = 1;
        }
    }

    /**
     * Stops the clock.
     *
     * <p>The registrations stay: a later {@link #start} schedules them again, and those left
     * overdue go through the past-date rule.
     */
    public synchronized void stop() {
        if (this.engine == null) {
            return;
        }
        for (Registration r : this.table.values()) {
            if (r.task != null) {
                r.task.cancel();
                r.task = null;
            }
        }
        this.engine.cancel();
        this.engine = null;
    }

    // ---- registration and removal ---------------------------------------------------------

    /**
     * Ver {@link TimerMBean#addNotification(String, String, Object, Date, long, long, boolean)}.
     */
    public synchronized Integer addNotification(String type, String message, Object userData,
                                                Date date, long period, long nbOccurences,
                                                boolean fixedRate) throws IllegalArgumentException {
        if (date == null) {
            throw new IllegalArgumentException("Timer notification date cannot be null");
        }
        if (period < 0) {
            throw new IllegalArgumentException("Negative period");
        }
        if (nbOccurences < 0) {
            throw new IllegalArgumentException("Negative number of occurrences");
        }
        Integer id = Integer.valueOf(this.nextId);
        this.nextId = this.nextId + 1;
        Registration r = new Registration();
        r.id = id;
        r.type = type;
        r.message = message;
        r.userData = userData;
        // A copy: whoever registered may keep the `Date` and mutate it.
        r.date = new Date(date.getTime());
        r.period = period;
        r.occurrences = nbOccurences;
        r.fixedRate = fixedRate;
        this.table.put(id, r);
        if (this.engine != null) {
            long now = System.currentTimeMillis();
            if (r.date.getTime() > now) {
                schedule(r);
            } else {
                catchUp(r, now);
            }
        }
        return id;
    }

    /** Ver {@link TimerMBean}. */
    public synchronized Integer addNotification(String type, String message, Object userData,
                                                Date date, long period, long nbOccurences)
        throws IllegalArgumentException {
        return addNotification(type, message, userData, date, period, nbOccurences, false);
    }

    /** Ver {@link TimerMBean}. */
    public synchronized Integer addNotification(String type, String message, Object userData,
                                                Date date, long period)
        throws IllegalArgumentException {
        return addNotification(type, message, userData, date, period, 0, false);
    }

    /** Ver {@link TimerMBean}. */
    public synchronized Integer addNotification(String type, String message, Object userData,
                                                Date date) throws IllegalArgumentException {
        return addNotification(type, message, userData, date, 0, 0, false);
    }

    /** Ver {@link TimerMBean#removeNotification}. */
    public synchronized void removeNotification(Integer id) throws InstanceNotFoundException {
        Registration r = (id == null) ? null : this.table.get(id);
        if (r == null) {
            throw new InstanceNotFoundException("Timer notification " + id + " does not exist");
        }
        drop(r);
    }

    /** Ver {@link TimerMBean#removeNotifications}. */
    public synchronized void removeNotifications(String type) throws InstanceNotFoundException {
        List<Registration> hits = new ArrayList<Registration>();
        for (Registration r : this.table.values()) {
            if (r.type == null ? type == null : r.type.equals(type)) {
                hits.add(r);
            }
        }
        if (hits.isEmpty()) {
            throw new InstanceNotFoundException("No timer notification of type " + type);
        }
        for (Registration r : hits) {
            drop(r);
        }
    }

    /** Ver {@link TimerMBean#removeAllNotifications}. */
    public synchronized void removeAllNotifications() {
        for (Registration r : new ArrayList<Registration>(this.table.values())) {
            drop(r);
        }
    }

    // ---- queries ---------------------------------------------------------------------------

    /** How many registrations there are. */
    public synchronized int getNbNotifications() {
        return this.table.size();
    }

    /** The identifiers of all of them, in registration order. */
    public synchronized Vector<Integer> getAllNotificationIDs() {
        return new Vector<Integer>(this.table.keySet());
    }

    /** Those of that type; empty if there are none. */
    public synchronized Vector<Integer> getNotificationIDs(String type) {
        Vector<Integer> found = new Vector<Integer>();
        for (Registration r : this.table.values()) {
            if (r.type == null ? type == null : r.type.equals(type)) {
                found.add(r.id);
            }
        }
        return found;
    }

    /** The type, or null if that identifier does not exist. */
    public synchronized String getNotificationType(Integer id) {
        Registration r = lookup(id);
        return (r == null) ? null : r.type;
    }

    /** The message, or null. */
    public synchronized String getNotificationMessage(Integer id) {
        Registration r = lookup(id);
        return (r == null) ? null : r.message;
    }

    /** The attached data, or null. */
    public synchronized Object getNotificationUserData(Integer id) {
        Registration r = lookup(id);
        return (r == null) ? null : r.userData;
    }

    /** The next firing date, or null. A copy, so that it is not moved from outside. */
    public synchronized Date getDate(Integer id) {
        Registration r = lookup(id);
        return (r == null) ? null : new Date(r.date.getTime());
    }

    /** The period, or null. */
    public synchronized Long getPeriod(Integer id) {
        Registration r = lookup(id);
        return (r == null) ? null : Long.valueOf(r.period);
    }

    /** The firings left, or null. See the name in {@link TimerMBean#getNbOccurences}. */
    public synchronized Long getNbOccurences(Integer id) {
        Registration r = lookup(id);
        return (r == null) ? null : Long.valueOf(r.occurrences);
    }

    /** Whether it counts from the original date, or null. */
    public synchronized Boolean getFixedRate(Integer id) {
        Registration r = lookup(id);
        return (r == null) ? null : Boolean.valueOf(r.fixedRate);
    }

    /** See the class note about past dates. */
    public boolean getSendPastNotifications() {
        return this.sendPastNotifications;
    }

    /** Ver {@link #getSendPastNotifications}. */
    public void setSendPastNotifications(boolean value) {
        this.sendPastNotifications = value;
    }

    /** Whether it is running. */
    public boolean isActive() {
        return this.engine != null;
    }

    /** Whether there is no registration at all. */
    public synchronized boolean isEmpty() {
        return this.table.isEmpty();
    }

    // ---- internals -------------------------------------------------------------------------

    /** The lookup all the getters use; null is a valid answer. */
    private Registration lookup(Integer id) {
        return (id == null) ? null : this.table.get(id);
    }

    /** Removes a registration and cancels whatever it had scheduled. */
    private void drop(Registration r) {
        if (r.task != null) {
            r.task.cancel();
            r.task = null;
        }
        this.table.remove(r.id);
        if (this.table.isEmpty()) {
            this.nextId = 1;
        }
    }

    /** Schedules the next firing of a registration whose date is already in the future. */
    private void schedule(Registration r) {
        if (this.engine == null) {
            return;
        }
        Alarm alarm = new Alarm(r);
        r.task = alarm;
        this.engine.schedule(alarm, new Date(r.date.getTime()));
    }

    /**
     * Resolves a registration whose date has passed. See the class note.
     *
     * <p>When it has to be sent, it is <b>scheduled for now</b> instead of being sent on the spot:
     * the notice goes out through the clock's thread like any other, and not inside {@link #start}
     * on the thread of whoever started it. The difference shows --a slow listener would hang the
     * start-- and it is what makes an overdue firing and a normal one behave the same.
     *
     * @param now the instant compared against, taken only once by the caller
     */
    private void catchUp(Registration r, long now) {
        if (this.sendPastNotifications) {
            r.date = new Date(now);
            schedule(r);
            return;
        }
        if (r.period == 0) {
            // One-shot and not sent: there is nothing left to schedule.
            drop(r);
            return;
        }
        r.date = new Date(advance(r.date.getTime(), r.period, now));
        schedule(r);
    }

    /**
     * Runs a date forward in whole periods until it passes {@code now}.
     *
     * <p>The repetitions that were skipped are <b>not</b> subtracted from the occurrences; see the
     * class note for why.
     */
    private static long advance(long from, long period, long now) {
        long next = from;
        while (next <= now) {
            next = next + period;
        }
        return next;
    }

    /** Sends a registration's notice. */
    private void fire(Registration r) {
        long seq = this.sequence;
        this.sequence = this.sequence + 1;
        TimerNotification n = new TimerNotification(
            r.type, this, seq, System.currentTimeMillis(), r.message, r.id);
        n.setUserData(r.userData);
        sendNotification(n);
    }

    /**
     * What runs on the clock's thread when the date arrives.
     *
     * <p>It takes the {@link Timer}'s lock just like the public methods, so a firing does not cross
     * with a registration or a removal halfway through.
     */
    private final class Alarm extends TimerTask {

        private final Registration registration;

        Alarm(Registration registration) {
            this.registration = registration;
        }

        public void run() {
            synchronized (Timer.this) {
                Registration r = this.registration;
                // It may have been removed between being scheduled and the time arriving.
                if (Timer.this.table.get(r.id) != r) {
                    return;
                }
                Timer.this.fire(r);
                if (r.occurrences > 0) {
                    r.occurrences = r.occurrences - 1;
                    if (r.occurrences == 0) {
                        Timer.this.drop(r);
                        return;
                    }
                }
                if (r.period == 0) {
                    Timer.this.drop(r);
                    return;
                }
                long now = System.currentTimeMillis();
                long base = r.fixedRate ? r.date.getTime() : now;
                long next = base + r.period;
                if (next <= now) {
                    // The clock was behind: skip to the next future slot.
                    next = advance(next, r.period, now);
                }
                r.date = new Date(next);
                Timer.this.schedule(r);
            }
        }
    }

    /** A registration: what was asked for plus what is left to do. */
    private static final class Registration {
        private Integer id;
        private String type;
        private String message;
        private Object userData;
        private Date date;
        private long period;
        private long occurrences;
        private boolean fixedRate;
        private TimerTask task;
    }
}
