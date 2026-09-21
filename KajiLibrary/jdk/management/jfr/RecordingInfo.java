package jdk.management.jfr;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.openmbean.CompositeData;

/**
 * A {@link jdk.jfr.Recording} seen from the other side of a JMX connection.
 *
 * <h2>The times and the durations are numbers</h2>
 *
 * <p>{@link #getStartTime} returns a {@code long} in milliseconds and not an {@code Instant};
 * {@link #getMaxAge} a {@code long} and not a {@code Duration}. It is not carelessness: the open
 * types of JMX are the primitives, {@code String} and little else, and an {@code Instant} is not
 * among them.
 *
 * <p>The same with {@link #getState}, which is the <strong>name</strong> of the constant of
 * {@link jdk.jfr.RecordingState} and not the constant: an enumeration does not travel either.
 *
 * <p>Converting them on the client's side is the job of whoever consumes this, and it is the
 * counterpart of the protocol not depending on the two ends having the same classes.
 *
 * @since 9
 */
public final class RecordingInfo {

    private final String name;
    private final long id;
    private final boolean dumpOnExit;
    private final long maxAge;
    private final long maxSize;
    private final String state;
    private final long startTime;
    private final long stopTime;
    private final Map<String, String> settings;
    private final String destination;
    private final long size;
    private final boolean toDisk;
    private final long duration;

    RecordingInfo(final String name, final long id, final boolean dumpOnExit,
            final long maxAge, final long maxSize, final String state, final long startTime,
            final long stopTime, final Map<String, String> settings, final String destination,
            final long size, final boolean toDisk, final long duration) {
        this.name = name;
        this.id = id;
        this.dumpOnExit = dumpOnExit;
        this.maxAge = maxAge;
        this.maxSize = maxSize;
        this.state = state;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.settings = Collections.unmodifiableMap(new LinkedHashMap<String, String>(settings));
        this.destination = destination;
        this.size = size;
        this.toDisk = toDisk;
        this.duration = duration;
    }

    /**
     * The name of the recording.
     *
     * @return the value
     */
    public String getName() {
        return name;
    }

    /**
     * The identifier of the recording.
     *
     * @return the value
     */
    public long getId() {
        return id;
    }

    /**
     * Whether it dumps when the VM ends.
     *
     * @return the value
     */
    public boolean getDumpOnExit() {
        return dumpOnExit;
    }

    /**
     * The maximum age of the data, in milliseconds; zero is no limit.
     *
     * @return the value
     */
    public long getMaxAge() {
        return maxAge;
    }

    /**
     * The maximum size in bytes; zero is no limit.
     *
     * @return the value
     */
    public long getMaxSize() {
        return maxSize;
    }

    /**
     * The state, with the name of the constant of
     * {@link jdk.jfr.RecordingState}.
     *
     * @return the value
     */
    public String getState() {
        return state;
    }

    /**
     * When it started, in milliseconds since the epoch; zero if it did not start.
     *
     * @return the value
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * When it stopped, in milliseconds since the epoch; zero if it did not stop.
     *
     * @return the value
     */
    public long getStopTime() {
        return stopTime;
    }

    /**
     * The settings of the recording.
     *
     * @return the value
     */
    public Map<String, String> getSettings() {
        return settings;
    }

    /**
     * Where it dumps, or {@code null}.
     *
     * @return the value
     */
    public String getDestination() {
        return destination;
    }

    /**
     * How much it takes up, in bytes.
     *
     * @return the value
     */
    public long getSize() {
        return size;
    }

    /**
     * Whether it writes to disk while it records.
     *
     * @return the value
     */
    public boolean isToDisk() {
        return toDisk;
    }

    /**
     * How long it is going to last, in milliseconds; zero is no limit.
     *
     * @return the value
     */
    public long getDuration() {
        return duration;
    }

    /**
     * It rebuilds the object from its open form.
     *
     * <p>It is the road by which this datum arrives from a remote VM: what travels over JMX is a
     * generic {@link CompositeData} and this turns it back.
     *
     * @param cd the open form, or {@code null}
     * @return the object, or {@code null} if {@code cd} was {@code null}
     * @throws IllegalArgumentException if it does not have the expected shape
     */
    public static RecordingInfo from(final CompositeData cd) {
        if (cd == null) {
            return null;
        }
        throw new IllegalArgumentException(
                "rebuilding a RecordingInfo needs the support of open types of JFR, which"
                + " this library does not implement");
    }

    /** {@inheritDoc} */
    public String toString() {
        final StringBuilder sb = new StringBuilder("RecordingInfo{");
        sb.append("name=").append(String.valueOf(name));
        sb.append(", ");
        sb.append("id=").append(String.valueOf(id));
        sb.append(", ");
        sb.append("dumpOnExit=").append(String.valueOf(dumpOnExit));
        sb.append(", ");
        sb.append("maxAge=").append(String.valueOf(maxAge));
        sb.append(", ");
        sb.append("maxSize=").append(String.valueOf(maxSize));
        sb.append(", ");
        sb.append("state=").append(String.valueOf(state));
        sb.append(", ");
        sb.append("startTime=").append(String.valueOf(startTime));
        sb.append(", ");
        sb.append("stopTime=").append(String.valueOf(stopTime));
        sb.append(", ");
        sb.append("settings=").append(String.valueOf(settings));
        sb.append(", ");
        sb.append("destination=").append(String.valueOf(destination));
        sb.append(", ");
        sb.append("size=").append(String.valueOf(size));
        sb.append(", ");
        sb.append("toDisk=").append(String.valueOf(toDisk));
        sb.append(", ");
        sb.append("duration=").append(String.valueOf(duration));
        return sb.append('}').toString();
    }
}
