package javax.management.monitor;

import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.monitor.MonitorMBean -- the management interface common to the
 * three monitors.
 *
 * <p>A monitor is an MBean that watches <b>other</b> MBeans: it reads an attribute every so often
 * and notifies when something happens. This interface is the part that does not depend on what is
 * being watched: whom, which attribute, how often, and starting and stopping.
 *
 * <h2>The two {@code ObservedObject}, singular and plural</h2>
 *
 * <p>{@link #getObservedObject} and {@link #setObservedObject} are from version 1, when a monitor
 * watched <b>one</b>. When watching several was added, they were kept: the getter returns the first
 * of the list and the setter replaces the whole list with one. They are deprecated and still work,
 * which is the only combination that does not break old code.
 *
 * <p>The observed attribute is <b>a single one</b> for all the observed objects. It is a real
 * limitation and it shows in use: to watch two different attributes you need two monitors.
 */
public interface MonitorMBean {

    /** Starts observing. */
    void start();

    /** Stops it. The configuration stays. */
    void stop();

    /**
     * Adds an MBean to observe.
     *
     * @throws IllegalArgumentException if it is null
     */
    void addObservedObject(ObjectName object) throws IllegalArgumentException;

    /** Removes it. If it was not there, does nothing. */
    void removeObservedObject(ObjectName object);

    /** Whether that one is in the list. */
    boolean containsObservedObject(ObjectName object);

    /** All the observed objects. */
    ObjectName[] getObservedObjects();

    /**
     * The first of the list.
     *
     * @deprecated see the class note; use {@link #getObservedObjects}
     */
    @Deprecated
    ObjectName getObservedObject();

    /**
     * Replaces the whole list with that one.
     *
     * @deprecated see the class note; use {@link #addObservedObject}
     */
    @Deprecated
    void setObservedObject(ObjectName object);

    /** The attribute that is read. See the class note: it is a single one. */
    String getObservedAttribute();

    /**
     * See {@link #getObservedAttribute}.
     *
     * @throws IllegalArgumentException if it is null
     */
    void setObservedAttribute(String attribute);

    /** Every how many milliseconds it reads. */
    long getGranularityPeriod();

    /**
     * See {@link #getGranularityPeriod}.
     *
     * @throws IllegalArgumentException if it is not positive: a period of 0 would be a tight loop
     */
    void setGranularityPeriod(long period) throws IllegalArgumentException;

    /** Whether it is observing. */
    boolean isActive();
}
