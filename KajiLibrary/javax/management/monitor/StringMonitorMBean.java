package javax.management.monitor;

import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.monitor.StringMonitorMBean -- the management of the string
 * monitor.
 *
 * <p>The simplest of the three: it compares a text attribute with a fixed string and notifies when
 * the result of the comparison <b>changes</b>.
 *
 * <p>The key word is changes. It does not notify while it matches, it notifies when it
 * <b>starts</b> matching; and the same on the other side. It is the same hysteresis idea as in
 * {@link GaugeMonitorMBean}, applied to something with only two states: an attribute that says OK
 * for an hour produces one notice, not three thousand six hundred.
 *
 * <p>Hence the two flags being independent and both starting off. The common thing is to turn only
 * one on: {@link #setNotifyDiffer} to watch for something ceasing to be right, or
 * {@link #setNotifyMatch} to wait for it to reach a state.
 */
public interface StringMonitorMBean extends MonitorMBean {

    /** The value read from the first observed object. */
    String getDerivedGauge();

    /** When it was read. */
    long getDerivedGaugeTimeStamp();

    /** The value read from that observed object. */
    String getDerivedGauge(ObjectName object);

    /** When it was read, for that observed object. */
    long getDerivedGaugeTimeStamp(ObjectName object);

    /** What it is compared with. */
    String getStringToCompare();

    /**
     * See {@link #getStringToCompare}.
     *
     * @throws IllegalArgumentException if it is null
     */
    void setStringToCompare(String value) throws IllegalArgumentException;

    /** Whether it notifies when it starts matching. */
    boolean getNotifyMatch();

    /** Ver {@link #getNotifyMatch}. */
    void setNotifyMatch(boolean value);

    /** Whether it notifies when it stops matching. */
    boolean getNotifyDiffer();

    /** Ver {@link #getNotifyDiffer}. */
    void setNotifyDiffer(boolean value);
}
