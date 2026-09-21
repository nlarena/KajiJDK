package jdk.jfr.consumer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jdk.jfr.Timespan;
import jdk.jfr.Timestamp;
import jdk.jfr.ValueDescriptor;

/**
 * An object read from a recording: fields with a name and with no Java class.
 *
 * <h2>Why it is not a normal object</h2>
 *
 * <p>Because the process that reads a recording almost never has the classes of the one that wrote
 * it. It may be another machine, another version, another program entirely. Deserialising to typed
 * objects would demand having those classes on the classpath, which is precisely what cannot be
 * asked for.
 *
 * <p>The way out is this one: the data are left as name-value pairs, with the
 * {@link ValueDescriptor descriptors} beside them explaining what each one is. They are accessed by
 * name and the type is put by the one who reads, who is the one that knows what they expect.
 *
 * <h2>The typed getters are not sugar</h2>
 *
 * <p>{@link #getInt} could be {@code (int) getValue(...)} and it is not: it does the conversion by
 * <strong>widening</strong>. A field recorded as a {@code short} is read with {@code getInt} with
 * no problem, which is what is needed when the one who reads does not know with what width it was
 * recorded -- and they do not know, because that depends on the version of the JDK that produced
 * the file.
 *
 * <p>{@link #getDuration} and {@link #getInstant} go further: they read a {@code long} and
 * interpret it according to the {@link Timespan} or {@link Timestamp} annotation of the field.
 * Without that, the one who reads would have to know in what unit each field of each event was
 * recorded.
 *
 * <h2>State in this VM</h2>
 *
 * <p>This whole class is real and works: given the descriptors and the values, the typed reading,
 * the conversion of units and {@link #toString} do what they say.
 *
 * <p>What there is not is anywhere to take those values from, because {@link RecordingFile} cannot
 * read the binary format. With the reading of the file written, this class works without touching
 * it.
 *
 * @since 9
 */
public class RecordedObject {

    private final List<ValueDescriptor> descriptors;
    private final Object[] values;

    RecordedObject(final List<ValueDescriptor> descriptors, final Object[] values) {
        this.descriptors = Collections.unmodifiableList(
                new ArrayList<ValueDescriptor>(descriptors));
        this.values = values.clone();
    }

    /**
     * Whether the object has a field with that name.
     *
     * <p>It accepts names with dots in order to reach a nested field, such as {@code
     * "thread.javaName"}: an event has objects inside it and this is the way of walking them
     * without taking them out one by one.
     *
     * @param name the name
     * @return whether it exists
     */
    public boolean hasField(final String name) {
        Objects.requireNonNull(name, "name");
        final int dot = name.indexOf('.');
        if (dot < 0) {
            return indexOf(name) >= 0;
        }
        final int i = indexOf(name.substring(0, dot));
        if (i < 0) {
            return false;
        }
        final Object v = values[i];
        return v instanceof RecordedObject
                && ((RecordedObject) v).hasField(name.substring(dot + 1));
    }

    /**
     * The value of that field, unconverted.
     *
     * <p>It accepts names with dots, just like {@link #hasField}.
     *
     * @param <T> the expected type; it is not checked
     * @param name the name
     * @return the value
     * @throws IllegalArgumentException if there is no field with that name
     */
    @SuppressWarnings("unchecked")
    public final <T> T getValue(final String name) {
        return (T) raw(name);
    }

    private Object raw(final String name) {
        Objects.requireNonNull(name, "name");
        final int dot = name.indexOf('.');
        if (dot < 0) {
            final int i = indexOf(name);
            if (i < 0) {
                throw new IllegalArgumentException("there is no field called " + name);
            }
            return values[i];
        }
        final Object v = raw(name.substring(0, dot));
        if (!(v instanceof RecordedObject)) {
            throw new IllegalArgumentException(
                    "the field " + name.substring(0, dot) + " is not an object");
        }
        return ((RecordedObject) v).raw(name.substring(dot + 1));
    }

    private int indexOf(final String name) {
        for (int i = 0; i < descriptors.size(); i++) {
            if (descriptors.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /** The descriptor of that field, in order to read its annotations. */
    private ValueDescriptor descriptor(final String name) {
        final int dot = name.indexOf('.');
        if (dot < 0) {
            final int i = indexOf(name);
            return i < 0 ? null : descriptors.get(i);
        }
        final Object v = raw(name.substring(0, dot));
        return v instanceof RecordedObject
                ? ((RecordedObject) v).descriptor(name.substring(dot + 1)) : null;
    }

    /**
     * The fields of this object.
     *
     * @return the descriptors
     */
    public List<ValueDescriptor> getFields() {
        return descriptors;
    }

    /**
     * The boolean value of that field.
     *
     * @param name the name
     * @return the value
     * @throws IllegalArgumentException if the field does not exist or is not boolean
     */
    public final boolean getBoolean(final String name) {
        final Object v = raw(name);
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue();
        }
        throw notA(name, "boolean", v);
    }

    /**
     * The value of that field as a {@code byte}.
     *
     * @param name the name
     * @return the value
     * @throws IllegalArgumentException if the field does not exist or does not fit in a {@code
     *     byte}
     */
    public final byte getByte(final String name) {
        return (byte) integer(name, Byte.MIN_VALUE, Byte.MAX_VALUE, "byte");
    }

    /**
     * The value of that field as a {@code char}.
     *
     * @param name the name
     * @return the value
     * @throws IllegalArgumentException if the field does not exist or is not a character
     */
    public final char getChar(final String name) {
        final Object v = raw(name);
        if (v instanceof Character) {
            return ((Character) v).charValue();
        }
        throw notA(name, "char", v);
    }

    /**
     * The value of that field as a {@code short}.
     *
     * @param name the name
     * @return the value
     * @throws IllegalArgumentException if the field does not exist or does not fit in a {@code
     *     short}
     */
    public final short getShort(final String name) {
        return (short) integer(name, Short.MIN_VALUE, Short.MAX_VALUE, "short");
    }

    /**
     * The value of that field as an {@code int}.
     *
     * @param name the name
     * @return the value
     * @throws IllegalArgumentException if the field does not exist or does not fit in an {@code
     *     int}
     */
    public final int getInt(final String name) {
        return (int) integer(name, Integer.MIN_VALUE, Integer.MAX_VALUE, "int");
    }

    /**
     * The value of that field as a {@code long}.
     *
     * @param name the name
     * @return the value
     * @throws IllegalArgumentException if the field does not exist or is not an integer
     */
    public final long getLong(final String name) {
        return integer(name, Long.MIN_VALUE, Long.MAX_VALUE, "long");
    }

    /**
     * It reads an integer of any width and checks that it fits in the requested one.
     *
     * <p>Widening is the normal case --a field recorded as a {@code short} is read with {@code
     * getInt}-- and that is why it is not rejected. What is rejected is <strong>narrowing</strong>
     * a value that does not fit: returning the truncated one would be a wrong number with no
     * warning, which is exactly what one does not want on the side of the one who reads a
     * recording.
     */
    private long integer(final String name, final long min, final long max, final String asWhat) {
        final Object v = raw(name);
        final long l;
        if (v instanceof Byte) {
            l = ((Byte) v).byteValue();
        } else if (v instanceof Short) {
            l = ((Short) v).shortValue();
        } else if (v instanceof Integer) {
            l = ((Integer) v).intValue();
        } else if (v instanceof Long) {
            l = ((Long) v).longValue();
        } else if (v instanceof Character) {
            l = ((Character) v).charValue();
        } else {
            throw notA(name, asWhat, v);
        }
        if (l < min || l > max) {
            throw new IllegalArgumentException(
                    "the value of the field " + name + " does not fit in a " + asWhat + ": " + l);
        }
        return l;
    }

    /**
     * The value of that field as a {@code float}.
     *
     * @param name the name
     * @return the value
     * @throws IllegalArgumentException if the field does not exist or is not numeric
     */
    public final float getFloat(final String name) {
        final Object v = raw(name);
        if (v instanceof Number) {
            return ((Number) v).floatValue();
        }
        throw notA(name, "float", v);
    }

    /**
     * The value of that field as a {@code double}.
     *
     * @param name the name
     * @return the value
     * @throws IllegalArgumentException if the field does not exist or is not numeric
     */
    public final double getDouble(final String name) {
        final Object v = raw(name);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        throw notA(name, "double", v);
    }

    /**
     * The value of that field as text.
     *
     * @param name the name
     * @return the value, or {@code null} if the field is empty
     * @throws IllegalArgumentException if the field does not exist or is not text
     */
    public final String getString(final String name) {
        final Object v = raw(name);
        if (v == null || v instanceof String) {
            return (String) v;
        }
        throw notA(name, "String", v);
    }

    /**
     * The value of that field as a duration, interpreting its unit.
     *
     * <p>The unit comes from the {@link Timespan} annotation of the field. With no annotation
     * nanoseconds are assumed, which is what that annotation has by default.
     *
     * @param name the name
     * @return the duration
     * @throws IllegalArgumentException if the field does not exist or is not numeric
     */
    public final Duration getDuration(final String name) {
        final long v = integer(name, Long.MIN_VALUE, Long.MAX_VALUE, "Duration");
        final ValueDescriptor d = descriptor(name);
        final Timespan t = d == null ? null : d.getAnnotation(Timespan.class);
        final String unit = t == null ? Timespan.NANOSECONDS : t.value();
        if (Timespan.SECONDS.equals(unit)) {
            return Duration.ofSeconds(v);
        }
        if (Timespan.MILLISECONDS.equals(unit)) {
            return Duration.ofMillis(v);
        }
        if (Timespan.MICROSECONDS.equals(unit)) {
            return Duration.ofNanos(v * 1000L);
        }
        // TICKS included: without the frequency of the clock of the machine that recorded there is
        // no way of converting them, and treating them as nanoseconds is what the JDK does.
        return Duration.ofNanos(v);
    }

    /**
     * The value of that field as a moment, interpreting its unit.
     *
     * <p>The unit comes from the {@link Timestamp} annotation of the field.
     *
     * @param name the name
     * @return the moment
     * @throws IllegalArgumentException if the field does not exist or is not numeric
     */
    public final Instant getInstant(final String name) {
        final long v = integer(name, Long.MIN_VALUE, Long.MAX_VALUE, "Instant");
        final ValueDescriptor d = descriptor(name);
        final Timestamp t = d == null ? null : d.getAnnotation(Timestamp.class);
        final String unit = t == null ? Timestamp.MILLISECONDS_SINCE_EPOCH : t.value();
        if (Timestamp.TICKS.equals(unit)) {
            return Instant.ofEpochSecond(0L, v);
        }
        return Instant.ofEpochMilli(v);
    }

    /**
     * The value of that field as a recorded class.
     *
     * @param name the name
     * @return the class, or {@code null} if the field is empty
     * @throws IllegalArgumentException if the field does not exist or is not a class
     */
    public final RecordedClass getClass(final String name) {
        final Object v = raw(name);
        if (v == null || v instanceof RecordedClass) {
            return (RecordedClass) v;
        }
        throw notA(name, "RecordedClass", v);
    }

    /**
     * The value of that field as a recorded thread.
     *
     * @param name the name
     * @return the thread, or {@code null} if the field is empty
     * @throws IllegalArgumentException if the field does not exist or is not a thread
     */
    public final RecordedThread getThread(final String name) {
        final Object v = raw(name);
        if (v == null || v instanceof RecordedThread) {
            return (RecordedThread) v;
        }
        throw notA(name, "RecordedThread", v);
    }

    private static IllegalArgumentException notA(final String name, final String type,
            final Object v) {
        return new IllegalArgumentException("the field " + name + " cannot be read as " + type
                + "; it is " + (v == null ? "null" : v.getClass().getName()));
    }

    /**
     * The whole object, one field per line.
     *
     * <p>It is {@code final} because the format has to be the same for every recorded object:
     * whoever reads a dump of a recording should not have to know which subclass each thing was of.
     *
     * @return the text
     */
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append('{').append(System.lineSeparator());
        for (int i = 0; i < descriptors.size(); i++) {
            sb.append("  ").append(descriptors.get(i).getName()).append(" = ")
              .append(String.valueOf(values[i])).append(System.lineSeparator());
        }
        sb.append('}');
        return sb.toString();
    }
}
