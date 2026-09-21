package jdk.jfr;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A metadata annotation, described as <strong>data</strong> instead of as a Java annotation.
 *
 * <h2>Why it has to be described as data</h2>
 *
 * <p>For two different reasons and both matter.
 *
 * <p>The first one: an event can be manufactured at run time with {@link EventFactory}, without a
 * Java class existing for it. There there is nowhere to put an annotation, and yet the event has to
 * be able to carry its label and its description. This class is how they are put on it.
 *
 * <p>The second one: a recording is read in <strong>another</strong> process, which may not have on
 * its classpath the annotations the one that recorded used. By keeping the name and the values
 * instead of the annotation itself, the one who reads can show them all the same.
 *
 * <h2>What can be kept inside</h2>
 *
 * <p>Only simple types: the primitives, {@code String}, {@code Class}, enumerations, and arrays of
 * those. There are no nested annotations as a value. It is the same restriction the format of the
 * recording imposes, and that is why it is here and not further down.
 *
 * <h2>The meta-annotations travel as well</h2>
 *
 * <p>{@link #getAnnotationElements} returns the annotations <strong>of the annotation</strong>. It
 * is what allows whoever reads to know that {@code @Timespan} is a {@link ContentType} and
 * therefore that the number that accompanies it is a duration -- without having the {@code
 * Timespan} class at hand.
 *
 * @since 9
 */
public final class AnnotationElement {

    private final Class<? extends Annotation> type;
    private final List<ValueDescriptor> descriptors;
    private final List<Object> values_;
    private final Map<String, Object> byName;

    /**
     * An annotation with several values, given by member name.
     *
     * @param annotationType the type of the annotation
     * @param values the values, by member name
     * @throws NullPointerException if either of the two is {@code null}
     * @throws IllegalArgumentException if a name is not a member of the annotation, if a member
     * with no default value is missing, or if a value is not of an allowed type
     */
    public AnnotationElement(final Class<? extends Annotation> annotationType,
            final Map<String, Object> values) {
        this.type = Objects.requireNonNull(annotationType, "annotationType");
        Objects.requireNonNull(values, "values");

        final List<ValueDescriptor> ds = new ArrayList<ValueDescriptor>();
        final List<Object> vs = new ArrayList<Object>();
        final Map<String, Object> map = new LinkedHashMap<String, Object>();

        // The members of the annotation are walked, not the keys of the map: that way the order of
        // the values is that of the declaration and not that of the map the caller passed, which
        // may be any one at all.
        for (final Method m : annotationType.getDeclaredMethods()) {
            final String member = m.getName();
            Object v = values.get(member);
            if (v == null) {
                v = m.getDefaultValue();
                if (v == null) {
                    throw new IllegalArgumentException(
                            "the value of the member " + member + " of " + annotationType
                            + " is missing");
                }
            }
            check(v, member);
            ds.add(new ValueDescriptor(m.getReturnType(), member));
            vs.add(v);
            map.put(member, v);
        }
        for (final String key : values.keySet()) {
            if (!map.containsKey(key)) {
                throw new IllegalArgumentException(
                        key + " is not a member of " + annotationType);
            }
        }

        this.descriptors = Collections.unmodifiableList(ds);
        this.values_ = Collections.unmodifiableList(vs);
        this.byName = Collections.unmodifiableMap(map);
    }

    /**
     * An annotation with a single member, the one called {@code value}.
     *
     * @param annotationType the type of the annotation
     * @param value the value
     * @throws NullPointerException if either of the two is {@code null}
     * @throws IllegalArgumentException if the annotation has no {@code value} member or the value
     * is not of an allowed type
     */
    public AnnotationElement(final Class<? extends Annotation> annotationType, final Object value) {
        this(annotationType, Collections.singletonMap("value",
                Objects.requireNonNull(value, "value")));
    }

    /**
     * An annotation with no values, or with every member at its default value.
     *
     * @param annotationType the type of the annotation
     * @throws NullPointerException if it is {@code null}
     * @throws IllegalArgumentException if some member has no default value
     */
    public AnnotationElement(final Class<? extends Annotation> annotationType) {
        this(annotationType, Collections.<String, Object>emptyMap());
    }

    /**
     * The types the format of the recording knows how to keep.
     *
     * <p>Rejecting here and not later matters: a value of an unsupported type discovered when
     * writing the file would ruin a recording that has already begun.
     */
    private static void check(final Object v, final String member) {
        Class<?> c = v.getClass();
        if (c.isArray()) {
            c = c.getComponentType();
        }
        if (c == Byte.class || c == Short.class || c == Integer.class || c == Long.class
                || c == Float.class || c == Double.class || c == Character.class
                || c == Boolean.class || c == String.class || c == Class.class
                || c.isPrimitive() || c.isEnum()) {
            return;
        }
        throw new IllegalArgumentException(
                "the value of the member " + member + " is of a type that cannot be kept in a "
                + "recording: " + c.getName());
    }

    /**
     * The values, in the order in which the annotation declares its members.
     *
     * @return the values
     */
    public List<Object> getValues() {
        return values_;
    }

    /**
     * The members of the annotation, described.
     *
     * <p>They correspond position by position with {@link #getValues}.
     *
     * @return the descriptors
     */
    public List<ValueDescriptor> getValueDescriptors() {
        return descriptors;
    }

    /**
     * The annotations <strong>this</strong> annotation carries.
     *
     * <p>The ones of {@code java.lang.annotation} --{@code @Retention}, {@code @Target} and
     * company-- are skipped because they describe how the annotation works in Java and say nothing
     * about the event. They are of no use to the one who reads the recording.
     *
     * @return the meta-annotations
     */
    public List<AnnotationElement> getAnnotationElements() {
        final List<AnnotationElement> out = new ArrayList<AnnotationElement>();
        for (final Annotation a : type.getAnnotations()) {
            final Class<? extends Annotation> t = a.annotationType();
            if (t.getName().startsWith("java.lang.annotation.")) {
                continue;
            }
            out.add(from(a));
        }
        return Collections.unmodifiableList(out);
    }

    /** It puts an element together from a live annotation, reading its members by reflection. */
    private static AnnotationElement from(final Annotation a) {
        final Map<String, Object> vals = new LinkedHashMap<String, Object>();
        for (final Method m : a.annotationType().getDeclaredMethods()) {
            try {
                vals.put(m.getName(), m.invoke(a));
            } catch (final ReflectiveOperationException e) {
                // A member that cannot be read should not make the others disappear: it is left out
                // and the element is left with what could be read.
                continue;
            }
        }
        return new AnnotationElement(a.annotationType(), vals);
    }

    /**
     * The complete name of the type of the annotation.
     *
     * @return the name
     */
    public String getTypeName() {
        return type.getName();
    }

    /**
     * The value of that member.
     *
     * @param name the name of the member
     * @return the value
     * @throws IllegalArgumentException if the annotation does not have that member
     */
    public Object getValue(final String name) {
        Objects.requireNonNull(name, "name");
        if (!byName.containsKey(name)) {
            throw new IllegalArgumentException(
                    name + " is not a member of " + type.getName());
        }
        return byName.get(name);
    }

    /**
     * Whether the annotation has that member.
     *
     * @param name the name of the member
     * @return whether it has it
     */
    public boolean hasValue(final String name) {
        return byName.containsKey(Objects.requireNonNull(name, "name"));
    }

    /**
     * The meta-annotation of that type this annotation carries, if it carries it.
     *
     * <p>The signature is that of the JDK, with an unbounded type parameter: the result is not tied
     * to the argument and the conversion cannot be checked. It is an oddity of the API that is
     * reproduced as it is.
     *
     * @param <A> the expected type
     * @param annotationType the type of the meta-annotation
     * @return the annotation, or {@code null} if it is not there
     */
    @SuppressWarnings("unchecked")
    public final <A> A getAnnotation(final Class<? extends Annotation> annotationType) {
        Objects.requireNonNull(annotationType, "annotationType");
        return (A) type.getAnnotation(annotationType);
    }

    /**
     * The numeric identifier of the type of this annotation.
     *
     * <p>It is valid inside this run of the VM and not outside; see {@code Types}.
     *
     * @return the identifier
     */
    public long getTypeId() {
        return Types.id(type);
    }
}
