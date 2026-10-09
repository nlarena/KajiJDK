package jdk.jfr.consumer;

import java.util.Collections;
import java.util.List;

import jdk.jfr.Configuration;
import jdk.jfr.EventType;

/**
 * The notice that the metadata of a stream of events changed.
 *
 * <h2>Why the metadata change in the middle of a stream</h2>
 *
 * <p>Because classes are loaded when they are needed. An event defined in a library that is loaded
 * ten minutes after starting does not exist in the initial metadata, and without this notice
 * whoever consumes the stream would have no way of finding out that a new type appeared.
 *
 * <p>They may also disappear, if the code that defined them is unloaded.
 *
 * <p>{@link #getAddedEventTypes} and {@link #getRemovedEventTypes} are the delta;
 * {@link #getEventTypes} is the complete state after the change. The delta is what serves for
 * reacting, the complete state for the one who connects halfway through.
 *
 * @since 16
 */
public final class MetadataEvent {

    private final List<EventType> all;
    private final List<EventType> added;
    private final List<EventType> removed;
    private final List<Configuration> configurations;

    MetadataEvent(List<EventType> all, List<EventType> added, List<EventType> removed,
            List<Configuration> configurations) {
        this.all = Collections.unmodifiableList(all);
        this.added = Collections.unmodifiableList(added);
        this.removed = Collections.unmodifiableList(removed);
        this.configurations = Collections.unmodifiableList(configurations);
    }

    /**
     * Every type of event there is after the change.
     *
     * @return the types
     */
    public final List<EventType> getEventTypes() {
        return all;
    }

    /**
     * The types that appeared.
     *
     * @return the added types
     */
    public final List<EventType> getAddedEventTypes() {
        return added;
    }

    /**
     * The types that disappeared.
     *
     * @return the removed types
     */
    public final List<EventType> getRemovedEventTypes() {
        return removed;
    }

    /**
     * The available configurations.
     *
     * @return the configurations
     */
    public List<Configuration> getConfigurations() {
        return configurations;
    }
}
