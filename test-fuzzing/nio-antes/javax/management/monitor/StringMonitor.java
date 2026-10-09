package javax.management.monitor;

import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanNotificationInfo;
import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.monitor.StringMonitor -- watches a text attribute.
 *
 * <p>The logic is in {@link StringMonitorMBean}: it notifies on the <b>change</b> of the
 * comparison, not while it lasts. Here is the per-observed-object state --whether the last reading
 * matched or not-- which is all that is needed to detect the change.
 *
 * <p>The comparison is exact {@code equals}: case included, without trimming spaces. It is right
 * for what it is used for --states like {@code STARTED} or {@code FAILED}-- and it has to be known,
 * because one extra space in the attribute makes the monitor never fire.
 *
 * <p>The first value read also counts as a change: a monitor started on an attribute that already
 * does not match notifies on the first reading. That is what is wanted -- otherwise you would have
 * to wait for the value to change twice to learn about a state that was already wrong.
 */
public class StringMonitor extends Monitor implements StringMonitorMBean {

    /** The last thing seen of each observed object. */
    private final Map<ObjectName, Watched> state = new HashMap<ObjectName, Watched>();

    /** What it is compared with. It starts at the empty string, not at null. */
    private String stringToCompare = "";

    /** Whether it notifies when it starts matching. */
    private boolean notifyMatch = false;

    /** Whether it notifies when it stops matching. */
    private boolean notifyDiffer = false;

    /** A stopped monitor, comparing against the empty string. */
    public StringMonitor() {
    }

    /** Starts observing. */
    public synchronized void start() {
        startPolling();
    }

    /** Stops it. */
    public synchronized void stop() {
        stopPolling();
    }

    /** The value read from the first observed object. */
    public synchronized String getDerivedGauge() {
        return getDerivedGauge(getObservedObject());
    }

    /** When it was read. */
    public synchronized long getDerivedGaugeTimeStamp() {
        return getDerivedGaugeTimeStamp(getObservedObject());
    }

    /** The value read from that observed object, or null if it was never read. */
    public synchronized String getDerivedGauge(ObjectName object) {
        Watched w = this.state.get(object);
        return (w == null) ? null : w.derivedGauge;
    }

    /** When it was read; 0 if never. */
    public synchronized long getDerivedGaugeTimeStamp(ObjectName object) {
        Watched w = this.state.get(object);
        return (w == null) ? 0 : w.timestamp;
    }

    /** What it is compared with. */
    public synchronized String getStringToCompare() {
        return this.stringToCompare;
    }

    /**
     * See {@link #getStringToCompare}.
     *
     * <p>Changing it resets everyone's state: with another string, the next reading is a change
     * even if the attribute has not moved.
     *
     * @throws IllegalArgumentException if it is null
     */
    public synchronized void setStringToCompare(String value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Null string to compare");
        }
        this.stringToCompare = value;
        for (Watched w : this.state.values()) {
            w.hasCompared = false;
        }
    }

    /** Whether it notifies when it starts matching. */
    public synchronized boolean getNotifyMatch() {
        return this.notifyMatch;
    }

    /** Ver {@link #getNotifyMatch}. */
    public synchronized void setNotifyMatch(boolean value) {
        this.notifyMatch = value;
    }

    /** Whether it notifies when it stops matching. */
    public synchronized boolean getNotifyDiffer() {
        return this.notifyDiffer;
    }

    /** Ver {@link #getNotifyDiffer}. */
    public synchronized void setNotifyDiffer(boolean value) {
        this.notifyDiffer = value;
    }

    /**
     * The common errors plus the string's two firings.
     *
     * <p>Without {@code THRESHOLD_ERROR}: there is no threshold here that could be wrong.
     */
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types = {
            MonitorNotification.RUNTIME_ERROR,
            MonitorNotification.OBSERVED_OBJECT_ERROR,
            MonitorNotification.OBSERVED_ATTRIBUTE_ERROR,
            MonitorNotification.OBSERVED_ATTRIBUTE_TYPE_ERROR,
            MonitorNotification.STRING_TO_COMPARE_VALUE_MATCHED,
            MonitorNotification.STRING_TO_COMPARE_VALUE_DIFFERED,
        };
        return new MBeanNotificationInfo[] {
            new MBeanNotificationInfo(types, "javax.management.monitor.MonitorNotification",
                "Notifications sent by the StringMonitor MBean")
        };
    }

    /**
     * Initial state of a new observed object.
     *
     * <p><b>Deliberate divergence.</b> The JDK creates an initial value of type {@code Integer}
     * here --the same one for all three monitors-- and then {@code getDerivedGauge()} casts it to
     * {@code String}, so asking a just-configured and still unread {@code StringMonitor} <b>throws
     * {@code ClassCastException}</b>. It was checked against the JDK 25 (and again while
     * translating this: the JDK throws, this one answers null).
     *
     * <p>Here the initial value is left at null, which means what it says --nothing was read-- and
     * does not break. Replicating the bug would add nothing: it is a value no program wants to
     * receive, and the exception comes out of a place unrelated to what the program asked for.
     */
    synchronized void createObserved(ObjectName name) {
        Watched w = new Watched();
        w.timestamp = System.currentTimeMillis();
        this.state.put(name, w);
    }

    /** It forgets about it. */
    synchronized void forgetObserved(ObjectName name) {
        this.state.remove(name);
    }

    /** A reading: compares and notifies only if the result changed. */
    synchronized void onValue(ObjectName name, int index, Object value) {
        if (!(value instanceof String)) {
            notifyOnce(index, OBSERVED_ATTRIBUTE_TYPE_ERROR_NOTIFIED,
                MonitorNotification.OBSERVED_ATTRIBUTE_TYPE_ERROR, name,
                "The observed attribute type is not a string");
            return;
        }
        clearFlag(index, OBSERVED_ATTRIBUTE_TYPE_ERROR_NOTIFIED);
        String reading = (String) value;
        Watched w = this.state.get(name);
        if (w == null) {
            w = new Watched();
            this.state.put(name, w);
        }
        w.derivedGauge = reading;
        w.timestamp = System.currentTimeMillis();
        boolean matches = this.stringToCompare.equals(reading);
        // The first reading counts as a change; see the class note.
        boolean changed = !w.hasCompared || w.matched != matches;
        w.hasCompared = true;
        w.matched = matches;
        if (!changed) {
            return;
        }
        if (matches && this.notifyMatch) {
            send(MonitorNotification.STRING_TO_COMPARE_VALUE_MATCHED, name,
                "The observed attribute has matched the string to compare",
                reading, this.stringToCompare);
        } else if (!matches && this.notifyDiffer) {
            send(MonitorNotification.STRING_TO_COMPARE_VALUE_DIFFERED, name,
                "The observed attribute has differed from the string to compare",
                reading, this.stringToCompare);
        }
    }

    /** The last thing seen of an observed object. */
    private static final class Watched {
        private String derivedGauge = null;
        private long timestamp = 0;
        private boolean hasCompared = false;
        private boolean matched = false;
    }
}
