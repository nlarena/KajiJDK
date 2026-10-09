package javax.management.monitor;

import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanNotificationInfo;
import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.monitor.CounterMonitor -- watches a counter.
 *
 * <p>The logic is explained in {@link CounterMonitorMBean}: threshold, offset and modulus. Here is
 * the per-observed-object state, which is what keeps two MBeans watched by the same monitor from
 * stepping on each other -- each has its own shifted threshold and its own previous reading.
 *
 * <p>It only works with integers. A {@code Double} or {@code Float} attribute produces a
 * {@link MonitorNotification#OBSERVED_ATTRIBUTE_TYPE_ERROR} and not an approximate comparison: a
 * counter that advances in fractions is not a counter, and comparing it with an integer threshold
 * would give firings that depend on rounding.
 */
public class CounterMonitor extends Monitor implements CounterMonitorMBean {

    /** What the monitor knows about each observed object. */
    private final Map<ObjectName, Counted> state = new HashMap<ObjectName, Counted>();

    /** The configured threshold. */
    private Number initThreshold = Integer.valueOf(0);

    /** How much it shifts after each firing. */
    private Number offset = Integer.valueOf(0);

    /** The value at which the counter wraps around. */
    private Number modulus = Integer.valueOf(0);

    /** Whether it notifies. */
    private boolean notify = false;

    /** Whether the difference is compared. */
    private boolean differenceMode = false;

    /** A stopped monitor, with everything at zero. */
    public CounterMonitor() {
    }

    /** Starts observing. */
    public synchronized void start() {
        startPolling();
    }

    /** Stops it. The shifted thresholds stay as they were. */
    public synchronized void stop() {
        stopPolling();
    }

    /** The value computed for the first observed object. */
    public synchronized Number getDerivedGauge() {
        return getDerivedGauge(getObservedObject());
    }

    /** When it was computed. */
    public synchronized long getDerivedGaugeTimeStamp() {
        return getDerivedGaugeTimeStamp(getObservedObject());
    }

    /** The value computed for that observed object, or null if it was never read. */
    public synchronized Number getDerivedGauge(ObjectName object) {
        Counted c = this.state.get(object);
        return (c == null) ? null : c.derivedGauge;
    }

    /** When it was computed; 0 if never. */
    public synchronized long getDerivedGaugeTimeStamp(ObjectName object) {
        Counted c = this.state.get(object);
        return (c == null) ? 0 : c.timestamp;
    }

    /**
     * The <b>current</b> threshold of that observed object; the initial one if it has not been
     * shifted yet.
     *
     * @return null if that object is not observed
     */
    public synchronized Number getThreshold(ObjectName object) {
        Counted c = this.state.get(object);
        if (c == null) {
            return null;
        }
        return (c.threshold == null) ? this.initThreshold : c.threshold;
    }

    /** The current threshold of the first observed object. */
    public synchronized Number getThreshold() {
        return getThreshold(getObservedObject());
    }

    /**
     * Changes the threshold. It also resets the shifted ones: a new threshold starts from scratch
     * for everyone.
     *
     * @throws IllegalArgumentException if it is null or negative
     */
    public synchronized void setThreshold(Number value) throws IllegalArgumentException {
        setInitThreshold(value);
    }

    /** The configured threshold. */
    public synchronized Number getInitThreshold() {
        return this.initThreshold;
    }

    /**
     * See {@link #getInitThreshold}.
     *
     * @throws IllegalArgumentException if it is null or negative
     */
    public synchronized void setInitThreshold(Number value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Null threshold");
        }
        if (value.longValue() < 0) {
            throw new IllegalArgumentException("Negative threshold");
        }
        this.initThreshold = value;
        for (Counted c : this.state.values()) {
            c.threshold = null;
            c.notified = false;
        }
    }

    /** How much the threshold shifts after each firing. */
    public synchronized Number getOffset() {
        return this.offset;
    }

    /**
     * See {@link #getOffset}.
     *
     * @throws IllegalArgumentException if it is null or negative
     */
    public synchronized void setOffset(Number value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Null offset");
        }
        if (value.longValue() < 0) {
            throw new IllegalArgumentException("Negative offset");
        }
        this.offset = value;
    }

    /** The value at which the counter wraps around. */
    public synchronized Number getModulus() {
        return this.modulus;
    }

    /**
     * See {@link #getModulus}.
     *
     * @throws IllegalArgumentException if it is null or negative
     */
    public synchronized void setModulus(Number value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Null modulus");
        }
        if (value.longValue() < 0) {
            throw new IllegalArgumentException("Negative modulus");
        }
        this.modulus = value;
    }

    /** Whether it notifies on reaching the threshold. */
    public synchronized boolean getNotify() {
        return this.notify;
    }

    /** Ver {@link #getNotify}. */
    public synchronized void setNotify(boolean value) {
        this.notify = value;
    }

    /** Whether the difference with the previous reading is compared. */
    public synchronized boolean getDifferenceMode() {
        return this.differenceMode;
    }

    /** Ver {@link #getDifferenceMode}. */
    public synchronized void setDifferenceMode(boolean value) {
        this.differenceMode = value;
    }

    /** The five common errors plus the counter's own firing. */
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types = {
            MonitorNotification.RUNTIME_ERROR,
            MonitorNotification.OBSERVED_OBJECT_ERROR,
            MonitorNotification.OBSERVED_ATTRIBUTE_ERROR,
            MonitorNotification.OBSERVED_ATTRIBUTE_TYPE_ERROR,
            MonitorNotification.THRESHOLD_ERROR,
            MonitorNotification.THRESHOLD_VALUE_EXCEEDED,
        };
        return new MBeanNotificationInfo[] {
            new MBeanNotificationInfo(types, "javax.management.monitor.MonitorNotification",
                "Notifications sent by the CounterMonitor MBean")
        };
    }

    /** Initial state of a new observed object; see {@link Monitor#createObserved}. */
    synchronized void createObserved(ObjectName name) {
        Counted c = new Counted();
        c.derivedGauge = Integer.valueOf(0);
        c.timestamp = System.currentTimeMillis();
        this.state.put(name, c);
    }

    /** It forgets about it. */
    synchronized void forgetObserved(ObjectName name) {
        this.state.remove(name);
    }

    /** A reading: computes the derived value and decides whether to fire. */
    synchronized void onValue(ObjectName name, int index, Object value) {
        if (!(value instanceof Number) || value instanceof Double || value instanceof Float) {
            notifyOnce(index, OBSERVED_ATTRIBUTE_TYPE_ERROR_NOTIFIED,
                MonitorNotification.OBSERVED_ATTRIBUTE_TYPE_ERROR, name,
                "The observed attribute type is not an integer");
            return;
        }
        clearFlag(index, OBSERVED_ATTRIBUTE_TYPE_ERROR_NOTIFIED);
        long reading = ((Number) value).longValue();
        Counted c = this.state.get(name);
        if (c == null) {
            c = new Counted();
            this.state.put(name, c);
        }
        long derived = reading;
        if (this.differenceMode) {
            derived = c.hasPrevious ? (reading - c.previous) : 0;
            long mod = this.modulus.longValue();
            // With a modulus, a negative difference is a wrap of the counter and not a step back.
            if (derived < 0 && mod > 0) {
                derived = derived + mod;
            }
        } else {
            long mod = this.modulus.longValue();
            // Without difference mode, the wrap is detected because the value went down.
            if (mod > 0 && c.hasPrevious && reading < c.previous) {
                c.threshold = null;
                c.notified = false;
            }
        }
        c.previous = reading;
        c.hasPrevious = true;
        c.derivedGauge = Long.valueOf(derived);
        c.timestamp = System.currentTimeMillis();

        long threshold = (c.threshold == null) ? this.initThreshold.longValue()
            : c.threshold.longValue();
        if (threshold <= 0 && this.initThreshold.longValue() == 0) {
            // Threshold 0 configured: there is nothing to watch.
            return;
        }
        if (derived < threshold) {
            return;
        }
        if (this.notify && !c.notified) {
            send(MonitorNotification.THRESHOLD_VALUE_EXCEEDED, name,
                "The observed attribute has reached the threshold",
                c.derivedGauge, Long.valueOf(threshold));
        }
        long step = this.offset.longValue();
        if (step > 0) {
            // It is shifted until it passes the current value: if the counter jumped several
            // offsets at once, there is no point leaving the threshold behind and firing on every
            // following reading.
            long moved = threshold;
            while (moved <= derived) {
                moved = moved + step;
            }
            c.threshold = Long.valueOf(moved);
            c.notified = false;
        } else {
            // Without an offset it notifies once and the threshold is never moved again.
            c.notified = true;
        }
    }

    /** What the monitor remembers about each observed object. */
    private static final class Counted {
        private Number derivedGauge = null;
        private long timestamp = 0;
        private long previous = 0;
        private boolean hasPrevious = false;
        private Number threshold = null;
        private boolean notified = false;
    }
}
