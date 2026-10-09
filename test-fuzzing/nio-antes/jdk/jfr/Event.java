package jdk.jfr;

/**
 * The class every event of one's own in JFR inherits from.
 *
 * <h2>How it is used</h2>
 *
 * <p>It is inherited from, public fields are added to it --each field is a datum of the event-- and
 * it is emitted:
 *
 * <pre>{@code
 * class Request extends Event {
 *     @Label("Path") String path;
 *     @Label("Bytes") @DataAmount long bytes;
 * }
 *
 * Request e = new Request();
 * e.begin();
 * ... do the work ...
 * e.path = path;
 * e.end();
 * if (e.shouldCommit()) { e.commit(); }
 * }</pre>
 *
 * <h2>Why the methods are empty</h2>
 *
 * <p>Because that is how they are in the JDK, and it is not an omission: the VM <strong>rewrites
 * the bytecode</strong> of each subclass when it loads it, replacing these calls by the code that
 * writes the event into the buffer. The method one sees here never runs.
 *
 * <p>That explains two things that are otherwise odd. That they are all {@code final}: if a
 * subclass redefined them, the rewriting would have nowhere to hook onto. And that an event in a
 * program with no JFR active costs literally nothing -- the empty bodies are inlined away and not
 * even the call is left.
 *
 * <p>In this library the classes are the same and the rewriting does not happen, so the events are
 * not recorded. {@link #isEnabled} and {@link #shouldCommit} answer {@code false}, which is the
 * correct answer: there is nothing listening.
 *
 * <h2>The pair {@code shouldCommit} / {@code commit}</h2>
 *
 * <p>Asking before emitting is not an optional optimisation. Filling the fields of an event may
 * cost --formatting a string, walking a structure-- and {@code shouldCommit} answers whether that
 * work is going to be of any use, looking at the threshold and the configured filters.
 *
 * @since 9
 */
public abstract class Event extends jdk.internal.event.Event {

    /** For the subclasses. */
    protected Event() {
    }

    /**
     * It marks the beginning of the event and starts its stopwatch.
     *
     * <p>It is not needed for an event with no duration: if it is not called, the event is left
     * with duration zero and the time mark is put by {@link #commit}.
     */
    public final void begin() {
    }

    /**
     * It marks the end of the event and stops its stopwatch.
     *
     * <p>Separated from {@link #commit} so that the measured duration is that of the work and does
     * not include whatever it costs to fill the fields of the event afterwards.
     */
    public final void end() {
    }

    /**
     * It emits the event.
     *
     * <p>If {@link #end} was not called, it calls it on its own.
     */
    public final void commit() {
    }

    /**
     * Whether somebody is recording this type of event.
     *
     * @return {@code false} in this library, because there is no recorder
     */
    public final boolean isEnabled() {
        return false;
    }

    /**
     * Whether this event would pass the configured filters --threshold included-- and therefore
     * whether it is worth finishing putting it together.
     *
     * @return {@code false} in this library, because there is no recorder
     */
    public final boolean shouldCommit() {
        return false;
    }

    /**
     * It sets a field by its index, for the events put together at run time with
     * {@link EventFactory}.
     *
     * <p>An event made by hand has fields with a name and they are assigned to directly; one
     * manufactured dynamically has no Java fields, and this is the only way of filling it.
     *
     * @param index the index of the field, in the order in which they were declared
     * @param value the value
     */
    public final void set(int index, Object value) {
    }
}
