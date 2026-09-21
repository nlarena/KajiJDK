package jdk.jfr.consumer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import jdk.jfr.EventType;
import jdk.jfr.ValueDescriptor;

/**
 * An event read from a recording.
 *
 * <p>It is a {@link RecordedObject} with four extra accessors, one for each field every event
 * carries: {@link #getStartTime}, {@link #getEndTime}, {@link #getThread} and
 * {@link #getStackTrace}. The other fields, the ones whoever wrote the event defined, are read by
 * name with the typed getters of the base class.
 *
 * <p>{@link #getEndTime} is not a recorded field: it comes from adding the duration to the
 * beginning. It is so because what is kept in the file is the beginning and the duration, and
 * keeping the end as well would be a redundant datum in each one of millions of events.
 *
 * @since 9
 */
public final class RecordedEvent extends RecordedObject {

    private final EventType type;

    RecordedEvent(EventType type, List<ValueDescriptor> descriptors, Object[] values) {
        super(descriptors, values);
        this.type = type;
    }

    /**
     * The stack of calls of the point where it was emitted.
     *
     * @return the stack, or {@code null} if it was not recorded
     */
    public RecordedStackTrace getStackTrace() {
        return getValue("stackTrace");
    }

    /**
     * The thread that emitted it.
     *
     * @return the thread, or {@code null} if it was not recorded
     */
    public RecordedThread getThread() {
        return getValue("eventThread");
    }

    /**
     * The type of the event.
     *
     * @return the type
     */
    public EventType getEventType() {
        return type;
    }

    /**
     * When it began.
     *
     * @return the moment
     */
    public Instant getStartTime() {
        return getInstant("startTime");
    }

    /**
     * When it ended.
     *
     * <p>Calculated: the beginning plus the duration.
     *
     * @return the moment
     */
    public Instant getEndTime() {
        return getStartTime().plus(getDuration());
    }

    /**
     * How long it lasted.
     *
     * @return the duration
     */
    public Duration getDuration() {
        return getDuration("duration");
    }

    /**
     * The fields of the event.
     *
     * @return the descriptors
     */
    public List<ValueDescriptor> getFields() {
        return type.getFields();
    }
}
