package javax.management.monitor;

import javax.management.Notification;
import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.monitor.MonitorNotification -- what a monitor sends.
 *
 * <p>The ten types fall into two groups worth telling apart from the start:
 *
 * <ul>
 *   <li>five of <b>error</b> --{@code jmx.monitor.error.*}--, which say the monitor could not
 *       observe: the MBean is not there, the attribute does not exist, the attribute is of another
 *       type, the threshold is not valid, or something threw;
 *   <li>five of <b>firing</b>, which are what the monitor exists for: the counter passed the
 *       threshold, the gauge went up or down, the string started or stopped matching.
 * </ul>
 *
 * <p>The error ones are sent <b>only once</b> until the condition changes. It is the right thing: a
 * monitor observing an MBean that does not exist, with a one-second period, would send one notice
 * per second forever.
 *
 * <p>{@link #getDerivedGauge} is the value the monitor computed --which is not always the
 * attribute: in difference mode it is the subtraction from the previous reading-- and {@link
 * #getTrigger} is what it was compared against. The two together are what explains why it fired.
 *
 * <p>It has no public constructor: the monitor builds them. See the equivalent note in
 * {@code javax.management.timer.TimerNotification}, which does have one, and for the opposite
 * reason -- there the application chooses the notice's content and here it does not.
 */
public class MonitorNotification extends Notification {

    private static final long serialVersionUID = -4608189663661929204L;

    /** The observed MBean is not registered. */
    public static final String OBSERVED_OBJECT_ERROR = "jmx.monitor.error.mbean";

    /** The observed attribute does not exist. */
    public static final String OBSERVED_ATTRIBUTE_ERROR = "jmx.monitor.error.attribute";

    /** The attribute is of a type this monitor cannot watch. */
    public static final String OBSERVED_ATTRIBUTE_TYPE_ERROR = "jmx.monitor.error.type";

    /** The threshold does not fit the attribute's type. */
    public static final String THRESHOLD_ERROR = "jmx.monitor.error.threshold";

    /** Something threw while observing. */
    public static final String RUNTIME_ERROR = "jmx.monitor.error.runtime";

    /** The counter reached the threshold. */
    public static final String THRESHOLD_VALUE_EXCEEDED = "jmx.monitor.counter.threshold";

    /** The gauge crossed the high threshold. */
    public static final String THRESHOLD_HIGH_VALUE_EXCEEDED = "jmx.monitor.gauge.high";

    /** The gauge crossed the low threshold. */
    public static final String THRESHOLD_LOW_VALUE_EXCEEDED = "jmx.monitor.gauge.low";

    /** The string started matching. */
    public static final String STRING_TO_COMPARE_VALUE_MATCHED = "jmx.monitor.string.matches";

    /** The string stopped matching. */
    public static final String STRING_TO_COMPARE_VALUE_DIFFERED = "jmx.monitor.string.differs";

    /** Which of the observed objects fired. */
    private final ObjectName observedObject;

    /** Which attribute of it. */
    private final String observedAttribute;

    /** The value the monitor computed. */
    private final Object derivedGauge;

    /** What it was compared against. */
    private final Object trigger;

    /** Package-private: the monitor builds them. See the class note. */
    MonitorNotification(String type, Object source, long sequenceNumber, long timeStamp, String msg,
                        ObjectName observedObject, String observedAttribute, Object derivedGauge,
                        Object trigger) {
        super(type, source, sequenceNumber, timeStamp, msg);
        this.observedObject = observedObject;
        this.observedAttribute = observedAttribute;
        this.derivedGauge = derivedGauge;
        this.trigger = trigger;
    }

    /** The MBean that fired the notice. */
    public ObjectName getObservedObject() {
        return this.observedObject;
    }

    /** The attribute that was being watched. */
    public String getObservedAttribute() {
        return this.observedAttribute;
    }

    /** The computed value. See the class note: it is not always the attribute. */
    public Object getDerivedGauge() {
        return this.derivedGauge;
    }

    /** What it was compared against: the threshold, or the string. */
    public Object getTrigger() {
        return this.trigger;
    }
}
