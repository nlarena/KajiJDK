package jdk.jfr;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * It manufactures types of event at run time, without a Java class existing for them.
 *
 * <h2>What it is for</h2>
 *
 * <p>For a bridge. A rules engine, a scripting language or an application server knows at run time
 * which events it wants to emit, and it cannot have a class written for each one: the names and the
 * fields come from a configuration that is read on starting.
 *
 * <p>This factory puts the type together from {@link ValueDescriptor} and {@link AnnotationElement}
 * --the same metadata that would be taken out of a class-- and generates the class underneath.
 *
 * <h2>How the fields are filled</h2>
 *
 * <p>With {@link Event#set(int, Object)}, by index. An event manufactured like this has no Java
 * fields to assign anything to, and the index is that of the list of descriptors it was created
 * with. It is the reason why {@code Event.set} exists.
 *
 * <h2>State in this VM</h2>
 *
 * <p>Manufacturing the type means <strong>generating a class</strong> and defining it at run time,
 * which needs support from the VM that this library does not have. {@link #create} fails saying so,
 * instead of returning a factory that afterwards manufactures no events.
 *
 * @since 9
 */
public final class EventFactory {

    private static final String NOT_THERE =
            "manufacturing a type of event generates and defines a class at run time, which "
            + "this VM does not support yet";

    private EventFactory() {
    }

    /**
     * A factory for a type of event with those fields and those annotations.
     *
     * @param annotationElements the annotations of the type
     * @param fields the fields
     * @return the factory
     * @throws NullPointerException if either of the two is {@code null}
     * @throws UnsupportedOperationException in this VM; see the note of the class
     */
    public static EventFactory create(final List<AnnotationElement> annotationElements,
            final List<ValueDescriptor> fields) {
        Objects.requireNonNull(annotationElements, "annotationElements");
        Objects.requireNonNull(fields, "fields");
        // They are copied and validated before failing: if some day there is support, the error of
        // a repeated field has to come out here and not on the first event emitted.
        final List<String> seen = new ArrayList<String>();
        for (final ValueDescriptor v : fields) {
            if (seen.contains(v.getName())) {
                throw new IllegalArgumentException("there are two fields called " + v.getName());
            }
            seen.add(v.getName());
        }
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * A new event of this type.
     *
     * @return the event
     * @throws UnsupportedOperationException in this VM; see the note of the class
     */
    public Event newEvent() {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * The type this factory manufactures.
     *
     * @return the type
     * @throws UnsupportedOperationException in this VM; see the note of the class
     */
    public EventType getEventType() {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It registers the type, so that JFR can record it.
     *
     * @throws UnsupportedOperationException in this VM; see the note of the class
     */
    public void register() {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * It takes the type out of the register.
     *
     * @throws UnsupportedOperationException in this VM; see the note of the class
     */
    public void unregister() {
        throw new UnsupportedOperationException(NOT_THERE);
    }
}
