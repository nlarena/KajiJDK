package javax.management.monitor;

import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.monitor.CounterMonitorMBean -- the management of the counter
 * monitor.
 *
 * <p>A counter only goes up. The monitor notifies when it reaches the threshold, and then it has to
 * decide what to do so as not to notify on every reading: that is what the <b>offset</b> and the
 * <b>modulus</b> are for, the two pieces that make this class useful.
 *
 * <ul>
 *   <li>the <b>offset</b> shifts the threshold upwards after each firing. With threshold 100 and
 *       offset 100 it notifies at 100, at 200, at 300: it is the way to say "tell me every
 *       hundred" without reconfiguring anything. With offset 0 it notifies once and never again;
 *   <li>the <b>modulus</b> is the value at which the counter goes back to zero. A 32-bit counter
 *       that wraps would look as if it had gone down, and without this the monitor could not tell a
 *       wrap from a reset. On detecting one, the threshold goes back to {@link #getInitThreshold}.
 * </ul>
 *
 * <p>Hence there being <b>two</b> thresholds: the initial one, which is what was configured, and
 * the current one, which the offset has been shifting. {@link #getThreshold} returns the current
 * one.
 *
 * <p>Difference mode changes what is compared: instead of the value, the subtraction from the
 * previous reading. It is what turns an accumulated counter into a rate.
 */
public interface CounterMonitorMBean extends MonitorMBean {

    /** The value computed for the first observed object. */
    Number getDerivedGauge();

    /** When it was computed. */
    long getDerivedGaugeTimeStamp();

    /** The current threshold of the first observed object. See the class note. */
    Number getThreshold();

    /**
     * Changes the threshold, and with it the initial one.
     *
     * @throws IllegalArgumentException if it is null or negative
     */
    void setThreshold(Number value) throws IllegalArgumentException;

    /** The value computed for that observed object. */
    Number getDerivedGauge(ObjectName object);

    /** When it was computed, for that observed object. */
    long getDerivedGaugeTimeStamp(ObjectName object);

    /** The current threshold of that observed object. */
    Number getThreshold(ObjectName object);

    /** The configured threshold, before the offset shifted it. */
    Number getInitThreshold();

    /**
     * See {@link #getInitThreshold}.
     *
     * @throws IllegalArgumentException if it is null or negative
     */
    void setInitThreshold(Number value) throws IllegalArgumentException;

    /** How much the threshold shifts after each firing; 0 not to shift it. */
    Number getOffset();

    /**
     * See {@link #getOffset}.
     *
     * @throws IllegalArgumentException if it is null or negative
     */
    void setOffset(Number value) throws IllegalArgumentException;

    /** The value at which the counter wraps around; 0 if it does not wrap. */
    Number getModulus();

    /**
     * See {@link #getModulus}.
     *
     * @throws IllegalArgumentException if it is null or negative
     */
    void setModulus(Number value) throws IllegalArgumentException;

    /** Whether it notifies on reaching the threshold. */
    boolean getNotify();

    /** Ver {@link #getNotify}. */
    void setNotify(boolean value);

    /** Whether the difference with the previous reading is compared. See the class note. */
    boolean getDifferenceMode();

    /** Ver {@link #getDifferenceMode}. */
    void setDifferenceMode(boolean value);
}
