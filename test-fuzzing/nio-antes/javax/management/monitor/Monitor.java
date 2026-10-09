package javax.management.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.monitor.Monitor -- the base of the three monitors.
 *
 * <p>A monitor reads an attribute of one or several MBeans every so often and notifies when
 * something happens. This class holds the part that does not depend on <b>what</b> is watched: the
 * list of observed objects, the period, the thread that wakes up, and the machinery for reporting
 * errors only once.
 *
 * <h2>The thread is a daemon</h2>
 *
 * <p>The other way round from {@code javax.management.timer.Timer}: a started monitor does
 * <b>not</b> keep the virtual machine from finishing. It makes sense in both cases and by the same
 * criterion -- a clock exists so that something happens and losing it is losing work; a monitor
 * exists to observe, and observing while the program is closing is of no use to anyone.
 *
 * <h2>Errors are reported once</h2>
 *
 * <p>The {@code alreadyNotified*} fields and the four {@code *_NOTIFIED} flags are exactly that: a
 * record, per observed object, of which errors have already been reported. Without them, a monitor
 * pointing at an MBean that does not exist would send one notice per period forever.
 *
 * <p>The flags are cleared when the condition is fixed, so an MBean that disappears and comes back
 * produces exactly two notices and not one or a thousand.
 *
 * <h2>The protected fields</h2>
 *
 * <p>Almost all the internal state is {@code protected} and not private. It is not a decision of
 * this library: the class predates generics and the JDK's subclasses touch them directly. They are
 * replicated as they are so that a subclass written against the JDK can compile against this.
 */
public abstract class Monitor extends NotificationBroadcasterSupport
    implements MonitorMBean, MBeanRegistration {

    /** How much {@link #alreadyNotifieds} grows by. */
    protected static final int capacityIncrement = 16;

    /** How many observed objects there are. */
    protected int elementCount = 0;

    /** The flags of the <b>first</b> one, for compatibility with the single-object version. */
    protected int alreadyNotified = 0;

    /** The flags of each observed object, in the same order as the list. */
    protected int[] alreadyNotifieds = new int[capacityIncrement];

    /** The agent it was registered in, or null. */
    protected MBeanServer server;

    /** Clears all the flags. */
    protected static final int RESET_FLAGS_ALREADY_NOTIFIED = 0;

    /** It was already reported that the MBean is not there. */
    protected static final int OBSERVED_OBJECT_ERROR_NOTIFIED = 1;

    /** It was already reported that the attribute does not exist. */
    protected static final int OBSERVED_ATTRIBUTE_ERROR_NOTIFIED = 2;

    /** It was already reported that the attribute is of another type. */
    protected static final int OBSERVED_ATTRIBUTE_TYPE_ERROR_NOTIFIED = 4;

    /** It was already reported that something threw. */
    protected static final int RUNTIME_ERROR_NOTIFIED = 8;

    /** A label for the diagnostic messages. */
    protected String dbgTag = getClass().getName();

    /** The observed objects, in registration order. */
    private final List<ObjectName> observedObjects = new ArrayList<ObjectName>();

    /** The attribute read from all of them. */
    private String observedAttribute = null;

    /** How often, in milliseconds. */
    private long granularityPeriod = 10000;

    /** Null while it is stopped. */
    private Timer engine;

    /** The name it was registered under, or null. */
    private ObjectName objectName;

    /** The sequence number of the notices. */
    private long sequence = 1;

    /** A stopped monitor with no observed objects. */
    public Monitor() {
    }

    // ---- MBeanRegistration -----------------------------------------------------------------

    /** It keeps the agent: it is where the attributes will be read from. */
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        this.server = server;
        this.objectName = name;
        return name;
    }

    /** Nothing to do. */
    public void postRegister(Boolean registrationDone) {
    }

    /** Stops the monitor: without an agent it cannot read anything. */
    public void preDeregister() throws Exception {
        stop();
    }

    /** Releases the agent. */
    public void postDeregister() {
        this.server = null;
        this.objectName = null;
    }

    // ---- what the subclasses define -------------------------------------------------------

    /** Starts. Each concrete monitor validates its own before calling {@link #startPolling}. */
    public abstract void start();

    /** Stops. */
    public abstract void stop();

    // ---- observed objects ------------------------------------------------------------------

    /**
     * The first of the list.
     *
     * @deprecated see {@link MonitorMBean#getObservedObject}
     */
    @Deprecated
    public synchronized ObjectName getObservedObject() {
        if (this.observedObjects.isEmpty()) {
            return null;
        }
        return this.observedObjects.get(0);
    }

    /**
     * Replaces the whole list with that one.
     *
     * @throws IllegalArgumentException if it is null
     * @deprecated see {@link MonitorMBean#setObservedObject}
     */
    @Deprecated
    public synchronized void setObservedObject(ObjectName object) throws IllegalArgumentException {
        if (object == null) {
            throw new IllegalArgumentException("Null observed object");
        }
        for (ObjectName old : this.observedObjects) {
            forgetObserved(old);
        }
        this.observedObjects.clear();
        this.observedObjects.add(object);
        createObserved(object);
        resetCounts();
    }

    /**
     * Adds one.
     *
     * <p>Adding the same one twice does nothing: the list is a set in practice, and notifying twice
     * about the same MBean is of no use to anyone.
     *
     * @throws IllegalArgumentException if it is null
     */
    public synchronized void addObservedObject(ObjectName object) throws IllegalArgumentException {
        if (object == null) {
            throw new IllegalArgumentException("Null observed object");
        }
        if (this.observedObjects.contains(object)) {
            return;
        }
        this.observedObjects.add(object);
        createObserved(object);
        resetCounts();
    }

    /** Removes it. If it was not there, does nothing. */
    public synchronized void removeObservedObject(ObjectName object) {
        if (this.observedObjects.remove(object)) {
            forgetObserved(object);
            resetCounts();
        }
    }

    /** Whether that one is there. */
    public synchronized boolean containsObservedObject(ObjectName object) {
        return this.observedObjects.contains(object);
    }

    /** All of them, in registration order. A copy. */
    public synchronized ObjectName[] getObservedObjects() {
        return this.observedObjects.toArray(new ObjectName[this.observedObjects.size()]);
    }

    /** The attribute read from all of them. */
    public synchronized String getObservedAttribute() {
        return this.observedAttribute;
    }

    /**
     * See {@link #getObservedAttribute}.
     *
     * @throws IllegalArgumentException if it is null
     */
    public void setObservedAttribute(String attribute) throws IllegalArgumentException {
        if (attribute == null) {
            throw new IllegalArgumentException("Null observed attribute");
        }
        synchronized (this) {
            this.observedAttribute = attribute;
            resetCounts();
        }
    }

    /** Every how many milliseconds it reads. */
    public synchronized long getGranularityPeriod() {
        return this.granularityPeriod;
    }

    /**
     * See {@link #getGranularityPeriod}.
     *
     * @throws IllegalArgumentException if it is not positive
     */
    public synchronized void setGranularityPeriod(long period) throws IllegalArgumentException {
        if (period <= 0) {
            throw new IllegalArgumentException("Nonpositive granularity period");
        }
        this.granularityPeriod = period;
        if (this.engine != null) {
            // Reschedule with the new period; otherwise the change would not show until the next
            // start, which is exactly what nobody expects from a setter.
            stopPolling();
            startPolling();
        }
    }

    /** Whether it is observing. */
    public synchronized boolean isActive() {
        return this.engine != null;
    }

    // ---- for the subclasses ---------------------------------------------------------------

    /**
     * The value computed for that observed object, without committing to a type.
     *
     * <p>Package-private and returning {@code Object} on purpose: each concrete monitor redefines
     * it with <b>its</b> type --{@code Number} in the numeric ones, {@code String} in the string
     * one-- and that covariant redefinition is what makes each one's public method the typed one.
     *
     * <p>Without this one, the redefinition would not be a redefinition but a new method, and the
     * bridge the compiler generates --a public {@code Object getDerivedGauge(ObjectName)} in each
     * subclass-- would not exist. That bridge is part of the API reflection sees, so the
     * declaration has to be there.
     */
    synchronized Object getDerivedGauge(ObjectName object) {
        return null;
    }

    /**
     * Starts the thread that wakes up every period.
     *
     * <p>The subclasses' {@code start()} call it after validating their own. If it was already
     * active it does nothing.
     */
    synchronized void startPolling() {
        if (this.engine != null) {
            return;
        }
        // Daemon: see the class note.
        this.engine = new Timer("monitor-mbean", true);
        this.engine.schedule(new Tick(), this.granularityPeriod, this.granularityPeriod);
    }

    /** Stops the thread. The {@code stop()} methods call it. */
    synchronized void stopPolling() {
        if (this.engine == null) {
            return;
        }
        this.engine.cancel();
        this.engine = null;
    }

    /**
     * One reading of all the observed objects.
     *
     * <p>It runs on the monitor's thread. Each subclass decides what to do with the value.
     */
    synchronized void poll() {
        int i = 0;
        while (i < this.observedObjects.size()) {
            ObjectName name = this.observedObjects.get(i);
            try {
                if (this.server == null) {
                    // Without an agent there is nowhere to read from. It is not an error of the
                    // observed MBean.
                    i = i + 1;
                    continue;
                }
                Object value = this.server.getAttribute(name, this.observedAttribute);
                clearFlag(i, OBSERVED_OBJECT_ERROR_NOTIFIED | OBSERVED_ATTRIBUTE_ERROR_NOTIFIED);
                onValue(name, i, value);
            } catch (javax.management.InstanceNotFoundException e) {
                notifyOnce(i, OBSERVED_OBJECT_ERROR_NOTIFIED,
                    MonitorNotification.OBSERVED_OBJECT_ERROR, name,
                    "The observed object is not registered");
            } catch (javax.management.AttributeNotFoundException e) {
                notifyOnce(i, OBSERVED_ATTRIBUTE_ERROR_NOTIFIED,
                    MonitorNotification.OBSERVED_ATTRIBUTE_ERROR, name,
                    "The observed attribute is not contained in the observed object");
            } catch (Exception e) {
                notifyOnce(i, RUNTIME_ERROR_NOTIFIED, MonitorNotification.RUNTIME_ERROR, name,
                    "An error occurred while reading the observed attribute: " + e);
            }
            i = i + 1;
        }
    }

    /** What to do with the value read. Each concrete monitor defines it. */
    abstract void onValue(ObjectName name, int index, Object value);

    /**
     * Tells the concrete monitor that there is a new observed object.
     *
     * <p>The per-object state is created <b>when it is added</b> and not on the first reading. It
     * shows from outside: just added, {@code getDerivedGauge()} already answers an initial value
     * instead of null. It is what the JDK does and it makes sense -- "I have not read anything of
     * this one yet" and "this one does not exist" are two different answers.
     */
    abstract void createObserved(ObjectName name);

    /** Tells it that one was removed: that object's state is discarded. */
    abstract void forgetObserved(ObjectName name);

    /**
     * Sends a notice if that error has not been reported yet for that observed object.
     *
     * <p>See the class note about why only once.
     */
    void notifyOnce(int index, int flag, String type, ObjectName name, String message) {
        if ((flagsAt(index) & flag) != 0) {
            return;
        }
        setFlag(index, flag);
        send(type, name, message, null, null);
    }

    /** Sends a firing notice, with no flags in between. */
    void send(String type, ObjectName name, String message, Object derivedGauge, Object trigger) {
        long seq = this.sequence;
        this.sequence = this.sequence + 1;
        sendNotification(new MonitorNotification(type, this, seq, System.currentTimeMillis(),
            message, name, this.observedAttribute, derivedGauge, trigger));
    }

    /** The flags of that observed object. */
    int flagsAt(int index) {
        if (index < 0 || index >= this.alreadyNotifieds.length) {
            return 0;
        }
        return this.alreadyNotifieds[index];
    }

    /** Turns a flag on. */
    void setFlag(int index, int flag) {
        ensureCapacity(index);
        this.alreadyNotifieds[index] = this.alreadyNotifieds[index] | flag;
        if (index == 0) {
            this.alreadyNotified = this.alreadyNotifieds[0];
        }
    }

    /** Turns flags off. */
    void clearFlag(int index, int flags) {
        ensureCapacity(index);
        this.alreadyNotifieds[index] = this.alreadyNotifieds[index] & ~flags;
        if (index == 0) {
            this.alreadyNotified = this.alreadyNotifieds[0];
        }
    }

    /** Trims the flag record to the size of the list and clears it. */
    private void resetCounts() {
        this.elementCount = this.observedObjects.size();
        ensureCapacity(this.elementCount);
        int i = 0;
        while (i < this.alreadyNotifieds.length) {
            this.alreadyNotifieds[i] = RESET_FLAGS_ALREADY_NOTIFIED;
            i = i + 1;
        }
        this.alreadyNotified = RESET_FLAGS_ALREADY_NOTIFIED;
    }

    /** Grows the record by {@link #capacityIncrement}. */
    private void ensureCapacity(int index) {
        if (index < this.alreadyNotifieds.length) {
            return;
        }
        int size = this.alreadyNotifieds.length;
        while (size <= index) {
            size = size + capacityIncrement;
        }
        int[] bigger = new int[size];
        System.arraycopy(this.alreadyNotifieds, 0, bigger, 0, this.alreadyNotifieds.length);
        this.alreadyNotifieds = bigger;
    }

    /** What runs on the monitor's thread every period. */
    private final class Tick extends TimerTask {

        public void run() {
            poll();
        }
    }
}
