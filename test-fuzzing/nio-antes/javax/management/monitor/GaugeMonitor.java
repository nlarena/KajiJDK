package javax.management.monitor;

import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanNotificationInfo;
import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.monitor.GaugeMonitor -- watches a value that goes up and down.
 *
 * <p>The hysteresis band is explained in {@link GaugeMonitorMBean}. Here is the per-observed-object
 * state: which of the two sides of the band each one is on, which is the only thing that has to be
 * remembered so as not to repeat notices.
 *
 * <p>Unlike {@link CounterMonitor}, this one does accept values with a decimal point: a temperature
 * or load gauge is naturally fractional. The comparison is made in {@code double} when either
 * threshold is one, and in {@code long} when both are integers -- so that an integer gauge does not
 * drag along the representation error of floating point.
 */
public class GaugeMonitor extends Monitor implements GaugeMonitorMBean {

    /** Which side of the band each observed object is on. */
    private final Map<ObjectName, Gauged> state = new HashMap<ObjectName, Gauged>();

    /** The high threshold. */
    private Number highThreshold = Integer.valueOf(0);

    /** The low one. */
    private Number lowThreshold = Integer.valueOf(0);

    /** Whether it notifies on crossing the high one. */
    private boolean notifyHigh = false;

    /** Whether it notifies on dropping below the low one. */
    private boolean notifyLow = false;

    /** Whether the difference is compared. */
    private boolean differenceMode = false;

    /** A stopped monitor, with both thresholds at zero. */
    public GaugeMonitor() {
    }

    /** Starts observing. */
    public synchronized void start() {
        startPolling();
    }

    /** Stops it. */
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
        Gauged g = this.state.get(object);
        return (g == null) ? null : g.derivedGauge;
    }

    /** When it was computed; 0 if never. */
    public synchronized long getDerivedGaugeTimeStamp(ObjectName object) {
        Gauged g = this.state.get(object);
        return (g == null) ? 0 : g.timestamp;
    }

    /** The high threshold. */
    public synchronized Number getHighThreshold() {
        return this.highThreshold;
    }

    /** The low one. */
    public synchronized Number getLowThreshold() {
        return this.lowThreshold;
    }

    /**
     * Sets both. See {@link GaugeMonitorMBean#setThresholds} about why they go together.
     *
     * @throws IllegalArgumentException if either is null, if they are of different types, or if the
     *     high one is lower than the low one
     */
    public synchronized void setThresholds(Number highValue, Number lowValue)
        throws IllegalArgumentException {
        if (highValue == null || lowValue == null) {
            throw new IllegalArgumentException("Null threshold value");
        }
        if (!highValue.getClass().equals(lowValue.getClass())) {
            throw new IllegalArgumentException("Different type threshold values");
        }
        if (highValue.doubleValue() < lowValue.doubleValue()) {
            throw new IllegalArgumentException("High threshold less than low threshold");
        }
        this.highThreshold = highValue;
        this.lowThreshold = lowValue;
        for (Gauged g : this.state.values()) {
            g.aboveHigh = false;
            g.belowLow = false;
        }
    }

    /** Whether it notifies on crossing the high one. */
    public synchronized boolean getNotifyHigh() {
        return this.notifyHigh;
    }

    /** Ver {@link #getNotifyHigh}. */
    public synchronized void setNotifyHigh(boolean value) {
        this.notifyHigh = value;
    }

    /** Whether it notifies on dropping below the low one. */
    public synchronized boolean getNotifyLow() {
        return this.notifyLow;
    }

    /** Ver {@link #getNotifyLow}. */
    public synchronized void setNotifyLow(boolean value) {
        this.notifyLow = value;
    }

    /** Whether the difference with the previous reading is compared. */
    public synchronized boolean getDifferenceMode() {
        return this.differenceMode;
    }

    /** Ver {@link #getDifferenceMode}. */
    public synchronized void setDifferenceMode(boolean value) {
        this.differenceMode = value;
    }

    /** The five common errors plus the gauge's two firings. */
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types = {
            MonitorNotification.RUNTIME_ERROR,
            MonitorNotification.OBSERVED_OBJECT_ERROR,
            MonitorNotification.OBSERVED_ATTRIBUTE_ERROR,
            MonitorNotification.OBSERVED_ATTRIBUTE_TYPE_ERROR,
            MonitorNotification.THRESHOLD_ERROR,
            MonitorNotification.THRESHOLD_HIGH_VALUE_EXCEEDED,
            MonitorNotification.THRESHOLD_LOW_VALUE_EXCEEDED,
        };
        return new MBeanNotificationInfo[] {
            new MBeanNotificationInfo(types, "javax.management.monitor.MonitorNotification",
                "Notifications sent by the GaugeMonitor MBean")
        };
    }

    /** Initial state of a new observed object; see {@link Monitor#createObserved}. */
    synchronized void createObserved(ObjectName name) {
        Gauged g = new Gauged();
        g.derivedGauge = Integer.valueOf(0);
        g.timestamp = System.currentTimeMillis();
        this.state.put(name, g);
    }

    /** It forgets about it. */
    synchronized void forgetObserved(ObjectName name) {
        this.state.remove(name);
    }

    /** A reading: computes the derived value and looks at which side of the band it fell on. */
    synchronized void onValue(ObjectName name, int index, Object value) {
        if (!(value instanceof Number)) {
            notifyOnce(index, OBSERVED_ATTRIBUTE_TYPE_ERROR_NOTIFIED,
                MonitorNotification.OBSERVED_ATTRIBUTE_TYPE_ERROR, name,
                "The observed attribute type is not a number");
            return;
        }
        clearFlag(index, OBSERVED_ATTRIBUTE_TYPE_ERROR_NOTIFIED);
        double reading = ((Number) value).doubleValue();
        Gauged g = this.state.get(name);
        if (g == null) {
            g = new Gauged();
            this.state.put(name, g);
        }
        double derived = reading;
        if (this.differenceMode) {
            derived = g.hasPrevious ? (reading - g.previous) : 0;
        }
        g.previous = reading;
        g.hasPrevious = true;
        g.derivedGauge = Double.valueOf(derived);
        g.timestamp = System.currentTimeMillis();

        double high = this.highThreshold.doubleValue();
        double low = this.lowThreshold.doubleValue();
        if (derived >= high) {
            // On crossing upwards the low side is cleared: the band is left armed for the next
            // descent.
            g.belowLow = false;
            if (!g.aboveHigh) {
                g.aboveHigh = true;
                if (this.notifyHigh) {
                    send(MonitorNotification.THRESHOLD_HIGH_VALUE_EXCEEDED, name,
                        "The observed attribute has exceeded the high threshold",
                        g.derivedGauge, this.highThreshold);
                }
            }
        } else if (derived <= low) {
            g.aboveHigh = false;
            if (!g.belowLow) {
                g.belowLow = true;
                if (this.notifyLow) {
                    send(MonitorNotification.THRESHOLD_LOW_VALUE_EXCEEDED, name,
                        "The observed attribute has exceeded the low threshold",
                        g.derivedGauge, this.lowThreshold);
                }
            }
        }
        // Inside the band nothing happens, which is exactly what the band exists for.
    }

    /** What the monitor remembers about each observed object. */
    private static final class Gauged {
        private Number derivedGauge = null;
        private long timestamp = 0;
        private double previous = 0;
        private boolean hasPrevious = false;
        private boolean aboveHigh = false;
        private boolean belowLow = false;
    }
}
