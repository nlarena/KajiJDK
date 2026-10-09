package javax.management;

import java.util.EventObject;

/**
 * The notice an MBean emits when something happens.
 *
 * <p>It extends {@code EventObject} and therefore carries {@code source}, but with a twist worth
 * knowing: the emitter puts <b>the MBean object</b> there, and the MBean server <b>replaces it with
 * the {@link ObjectName}</b> before forwarding it. A listener registered through the agent sees an
 * {@code ObjectName} in {@code getSource()}; one registered directly against the MBean sees the
 * object. Hence {@code setSource} being public: it is not an oversight, it is the mechanism.
 *
 * <p>The other field that matters is the sequence number. It belongs to the <b>emitter</b>, not to
 * the world, and it serves so the receiver can detect gaps: if it gets 5 and 7, it knows it missed
 * 6. It is zero if the emitter does not keep count.
 */
public class Notification extends EventObject {

    private static final long serialVersionUID = -7516092053498031989L;

    /**
     * @serial the type, with the dotted convention
     */
    private String type;

    /**
     * @serial the emitter's sequence number
     */
    private long sequenceNumber;

    /**
     * @serial when it happened, in milliseconds
     */
    private long timeStamp;

    /**
     * @serial free data from the emitter
     */
    private Object userData = null;

    /**
     * @serial text to read
     */
    private String message = "";

    /**
     * @serial the source, which the agent may replace with the ObjectName
     */
    protected Object source = null;

    /** With the time taken from the clock. */
    public Notification(String type, Object source, long sequenceNumber) {
        super(source);
        this.source = source;
        this.type = type;
        this.sequenceNumber = sequenceNumber;
        this.timeStamp = System.currentTimeMillis();
    }

    /** With the time taken from the clock and a text. */
    public Notification(String type, Object source, long sequenceNumber, String message) {
        this(type, source, sequenceNumber);
        this.message = message;
    }

    /** With an explicit time: to replay events that already happened. */
    public Notification(String type, Object source, long sequenceNumber, long timeStamp) {
        super(source);
        this.source = source;
        this.type = type;
        this.sequenceNumber = sequenceNumber;
        this.timeStamp = timeStamp;
    }

    /** With an explicit time and a text. */
    public Notification(String type, Object source, long sequenceNumber, long timeStamp,
                        String message) {
        this(type, source, sequenceNumber, timeStamp);
        this.message = message;
    }

    /**
     * Changes the source.
     *
     * <p>Public because the MBean server uses it to put the {@link ObjectName} in place of the
     * object; see the class note.
     */
    public void setSource(Object source) {
        super.source = source;
        this.source = source;
    }

    /** The emitter's sequence number. */
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    /** Sets it; whoever forwards uses it. */
    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * The type, by convention dotted and from the general to the particular
     * ({@code jmx.attribute.change}), so that a prefix filter makes sense.
     */
    public String getType() {
        return type;
    }

    /** When it happened, in milliseconds since the epoch. */
    public long getTimeStamp() {
        return timeStamp;
    }

    /** Sets it. */
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /** The text; the empty string if none was given, never {@code null}. */
    public String getMessage() {
        return message;
    }

    /** The emitter's free data, or {@code null}. */
    public Object getUserData() {
        return userData;
    }

    /** Sets it. If it travels to a remote client it has to be serializable. */
    public void setUserData(Object userData) {
        this.userData = userData;
    }

    /** {@code class[source=...][type=...][message=...]}. */
    public String toString() {
        return super.toString() + "[type=" + type + "][message=" + message + "]";
    }
}
