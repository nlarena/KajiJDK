package javax.management.monitor;

import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.monitor.GaugeMonitorMBean -- the management of the gauge
 * monitor.
 *
 * <p>A gauge goes up and down --memory use, number of connections-- and that is why it has
 * <b>two</b> thresholds instead of one. The two together are a hysteresis band, and that is the
 * whole idea:
 *
 * <ul>
 *   <li>on crossing the high threshold it notifies, and it does <b>not notify again</b> until the
 *       value drops below the low threshold;
 *   <li>on dropping below the low one it notifies, and it does not notify again until it rises
 *       above the high one.
 * </ul>
 *
 * <p>Without that band, a value oscillating around a single threshold would produce one notice per
 * reading. With it, a value trembling at the edge produces exactly one. It is the difference
 * between a useful alarm and one that gets ignored.
 *
 * <p>That is why {@link #setThresholds} sets both together and there is no setter for each: with
 * separate setters there would be an instant in which the high one is below the low one, and in
 * that instant the band means nothing.
 */
public interface GaugeMonitorMBean extends MonitorMBean {

    /** The value computed for the first observed object. */
    Number getDerivedGauge();

    /** When it was computed. */
    long getDerivedGaugeTimeStamp();

    /** The value computed for that observed object. */
    Number getDerivedGauge(ObjectName object);

    /** When it was computed, for that observed object. */
    long getDerivedGaugeTimeStamp(ObjectName object);

    /** The high threshold. */
    Number getHighThreshold();

    /** The low one. */
    Number getLowThreshold();

    /**
     * Sets both. See the class note about why they go together.
     *
     * @throws IllegalArgumentException if either is null, if they are of different types, or if the
     *     high one is lower than the low one
     */
    void setThresholds(Number highValue, Number lowValue) throws IllegalArgumentException;

    /** Whether it notifies on crossing the high one. */
    boolean getNotifyHigh();

    /** Ver {@link #getNotifyHigh}. */
    void setNotifyHigh(boolean value);

    /** Whether it notifies on dropping below the low one. */
    boolean getNotifyLow();

    /** Ver {@link #getNotifyLow}. */
    void setNotifyLow(boolean value);

    /** Whether the difference with the previous reading is compared. */
    boolean getDifferenceMode();

    /** Ver {@link #getDifferenceMode}. */
    void setDifferenceMode(boolean value);
}
