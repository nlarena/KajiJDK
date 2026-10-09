package jdk.jfr;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The type of an event: its name, its fields, its settings and its annotations.
 *
 * <h2>Where it comes from</h2>
 *
 * <p>From reading a class that inherits from {@link Event}: its instance fields are the fields of
 * the event, its annotations are the annotations of the event, and the methods marked with
 * {@link SettingDefinition} are its settings of its own.
 *
 * <p>It is the translation of "Java class" into "type of event", and it is what is kept in the
 * header of the recording so that whoever reads it later understands the bytes that follow.
 *
 * <h2>The four fields that appear without having been declared</h2>
 *
 * <p>{@link #getFields} always starts with {@code startTime}, {@code duration}, {@code eventThread}
 * and {@code stackTrace}, in that order, and then those of the class follow. They do not come from
 * the class: JFR puts them, and they are the ones that make it possible to put two events of
 * different types on the same line of time.
 *
 * <p>{@code stackTrace} is the only one whose type is not a Java class: it is
 * {@code jdk.types.StackTrace}, a type of the format of the recording.
 *
 * <h2>Which declared fields count</h2>
 *
 * <p>The <strong>instance</strong> ones that are neither {@code static} nor {@code transient} and
 * that are of a type JFR knows how to record: the eight primitives, {@code String}, {@code Thread}
 * and {@code Class}.
 *
 * <p>A field of any other type --an array, a collection, an {@code Object}-- is
 * <strong>ignored in silence</strong>. It is what the JDK does and it is surprising: the class
 * compiles, the field exists in Java, and it does not appear in the recording without anybody
 * warning.
 *
 * <h2>The three settings every event has</h2>
 *
 * <p>{@code threshold}, {@code stackTrace} and {@code enabled}, in that order, and then the ones of
 * its own. Their default values come from the corresponding annotations if they are there.
 *
 * <p><strong>{@code period} is not there</strong>, not even in an event annotated with
 * {@link Period}. It is what the JDK answers and it is easy to assume the other way round -- the
 * annotation exists, the setting does not appear in this list.
 *
 * <h2>State in this VM</h2>
 *
 * <p>Almost everything here is real and comes from reflection, and it is checked against the JDK
 * 25: the differential gives <strong>1 discrepancy out of 14</strong>.
 *
 * <p>{@link #isEnabled} cannot be real and answers {@code false}, which is the right thing with no
 * recorder.
 *
 * <p>The discrepancy that is left is #505: {@code Field.getAnnotations()} returns empty in this VM,
 * and that is why a declared field loses its {@link Label}, its {@link Description} and its content
 * type. The four implicit fields, which this class puts together by hand, do have them. It is fixed
 * on the side of the VM, with the native of field attributes that {@code Method} already has.
 *
 * @since 9
 */
public final class EventType {

    private static final Cache CACHE = new Cache();

    /**
     * The cache, with a name instead of anonymous because of #482: the bytecode generator does not
     * emit an anonymous class that is in the initialiser of a field.
     */
    private static final class Cache extends ClassValue<EventType> {
        protected EventType computeValue(final Class<?> type) {
            return new EventType(type);
        }
    }

    private final Class<?> cls;
    private final String name;
    private final List<ValueDescriptor> fields;
    private final Map<String, ValueDescriptor> byName;
    private final List<AnnotationElement> annotations;
    private final List<SettingDescriptor> settings;

    private EventType(final Class<?> cls) {
        this.cls = cls;

        final Name n = cls.getAnnotation(Name.class);
        this.name = n != null ? n.value() : cls.getName();

        final List<ValueDescriptor> cs = new ArrayList<ValueDescriptor>(implicitFields());
        for (final Field f : cls.getDeclaredFields()) {
            final int mod = f.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) {
                continue;
            }
            if (!isAllowedType(f.getType())) {
                // JFR ignores in silence the fields of a type it does not know how to record. It is
                // reproduced instead of failing: the class compiles all the same on the Java side,
                // and an event with an extra field that is not recorded is what the JDK delivers.
                continue;
            }
            cs.add(new ValueDescriptor(f.getType(), f.getName(),
                    fromAnnotations(f.getAnnotations())));
        }
        final Map<String, ValueDescriptor> map = new LinkedHashMap<String, ValueDescriptor>();
        for (final ValueDescriptor v : cs) {
            map.put(v.getName(), v);
        }
        this.fields = Collections.unmodifiableList(cs);
        this.byName = Collections.unmodifiableMap(map);

        this.annotations = Collections.unmodifiableList(fromAnnotations(cls.getAnnotations()));
        this.settings = Collections.unmodifiableList(buildSettings(cls));
    }

    private static List<AnnotationElement> fromAnnotations(final Annotation[] as) {
        final List<AnnotationElement> out = new ArrayList<AnnotationElement>();
        for (final Annotation a : as) {
            if (a.annotationType().getName().startsWith("java.lang.annotation.")) {
                continue;
            }
            final Map<String, Object> vals = new LinkedHashMap<String, Object>();
            for (final Method m : a.annotationType().getDeclaredMethods()) {
                try {
                    vals.put(m.getName(), m.invoke(a));
                } catch (final ReflectiveOperationException e) {
                    continue;
                }
            }
            out.add(new AnnotationElement(a.annotationType(), vals));
        }
        return out;
    }

    private static List<SettingDescriptor> buildSettings(final Class<?> cls) {
        final List<SettingDescriptor> out = new ArrayList<SettingDescriptor>();

        // The order is that of the JDK and not the alphabetical one nor that of the annotations:
        // threshold, stackTrace, enabled. Checked against the JDK 25.
        final Threshold th = cls.getAnnotation(Threshold.class);
        out.add(new SettingDescriptor(Threshold.NAME, "jdk.settings.Threshold", "Threshold",
                "Record event with duration above or equal to threshold",
                th == null ? "0 ns" : th.value(),
                Collections.<AnnotationElement>emptyList()));

        final StackTrace st = cls.getAnnotation(StackTrace.class);
        out.add(new SettingDescriptor(StackTrace.NAME, "jdk.settings.StackTrace", "Stack Trace",
                "Record stack traces", String.valueOf(st == null || st.value()),
                Collections.<AnnotationElement>emptyList()));

        final Enabled en = cls.getAnnotation(Enabled.class);
        out.add(new SettingDescriptor(Enabled.NAME, "jdk.settings.Enabled", "Enabled",
                "Record event", String.valueOf(en == null || en.value()),
                Collections.<AnnotationElement>emptyList()));

        // `period` is NOT there, not even in an event with @Period. It is what the JDK answers, and
        // it is surprising: the annotation exists and the setting does not appear in the list of
        // the type.
        for (final Method m : cls.getDeclaredMethods()) {
            if (m.getAnnotation(SettingDefinition.class) == null) {
                continue;
            }
            final Name nm = m.getAnnotation(Name.class);
            final Label lb = m.getAnnotation(Label.class);
            final Description ds = m.getAnnotation(Description.class);
            final Class<?> control = m.getParameterTypes().length == 1
                    ? m.getParameterTypes()[0] : SettingControl.class;
            out.add(new SettingDescriptor(nm != null ? nm.value() : m.getName(),
                    control.getName(), lb == null ? null : lb.value(),
                    ds == null ? null : ds.value(), initialValue(control),
                    fromAnnotations(m.getAnnotations())));
        }
        return out;
    }

    /**
     * The default value of a setting of one's own comes from <strong>instantiating its
     * control</strong> and asking it.
     *
     * <p>It is what the JDK does, and it is the only way: the default value is declared nowhere,
     * the control decides it in its constructor.
     */
    private static String initialValue(final Class<?> control) {
        try {
            final Object o = control.getDeclaredConstructor().newInstance();
            return o instanceof SettingControl ? ((SettingControl) o).getValue() : null;
        } catch (final ReflectiveOperationException e) {
            // A control with no accessible constructor does not invalidate the whole type of event:
            // the setting is left with no default value, which is different from not existing.
            return null;
        }
    }

    /**
     * The four fields every event has, whether or not they are declared in the class.
     *
     * <p>They go first and in this order. They do not come from the class: JFR puts them, and they
     * are the ones that make it possible to put two events of different types on the same line of
     * time.
     */
    private static List<ValueDescriptor> implicitFields() {
        final List<ValueDescriptor> out = new ArrayList<ValueDescriptor>(4);
        out.add(new ValueDescriptor(long.class, "startTime",
                Arrays.asList(new AnnotationElement(Label.class, "Start Time"),
                        new AnnotationElement(Timestamp.class, Timestamp.TICKS))));
        out.add(new ValueDescriptor(long.class, "duration",
                Arrays.asList(new AnnotationElement(Label.class, "Duration"),
                        new AnnotationElement(Timespan.class, Timespan.TICKS))));
        out.add(new ValueDescriptor(Thread.class, "eventThread",
                Arrays.asList(new AnnotationElement(Label.class, "Event Thread"),
                        new AnnotationElement(Description.class,
                                "Thread in which event was committed in"))));
        // The only one whose type is not a Java class: it is a type of the format of the recording.
        out.add(new ValueDescriptor("jdk.types.StackTrace", "stackTrace",
                Arrays.asList(new AnnotationElement(Label.class, "Stack Trace"),
                        new AnnotationElement(Description.class,
                                "Stack Trace starting from the method the event was committed in"))));
        return out;
    }

    /**
     * The types JFR knows how to record in a field of an event.
     *
     * <p>The eight primitives plus {@code String}, {@code Thread} and {@code Class}. Nothing else:
     * neither arrays, nor collections, nor {@code Object}. A field of another type is ignored in
     * silence.
     */
    private static boolean isAllowedType(final Class<?> t) {
        return t.isPrimitive() && t != void.class
                || t == String.class || t == Thread.class || t == Class.class;
    }

    /**
     * The type of event of that class.
     *
     * <p>Always the same instance for the same class: the reading by reflection is done once.
     *
     * @param eventClass the class of the event
     * @return its type
     * @throws NullPointerException if it is {@code null}
     */
    public static EventType getEventType(final Class<? extends Event> eventClass) {
        return CACHE.get(Objects.requireNonNull(eventClass, "eventClass"));
    }

    /**
     * The fields of the event, in the order in which the class declares them.
     *
     * @return the fields
     */
    public List<ValueDescriptor> getFields() {
        return fields;
    }

    /**
     * The field with that name.
     *
     * @param name the name
     * @return the field, or {@code null} if it does not exist
     */
    public ValueDescriptor getField(final String name) {
        return byName.get(Objects.requireNonNull(name, "name"));
    }

    /**
     * The name of the event in the recording.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * The readable name of the event.
     *
     * @return the label, or {@code null} if it has none
     */
    public String getLabel() {
        final Label l = cls.getAnnotation(Label.class);
        return l == null ? null : l.value();
    }

    /**
     * The numeric identifier of the type.
     *
     * @return the identifier
     */
    public long getId() {
        return Types.id(cls);
    }

    /**
     * The annotations of the event.
     *
     * @return the annotations
     */
    public List<AnnotationElement> getAnnotationElements() {
        return annotations;
    }

    /**
     * Whether somebody is recording this type of event right now.
     *
     * @return {@code false} in this library, because there is no recorder
     */
    public boolean isEnabled() {
        return false;
    }

    /**
     * The explanation of the event.
     *
     * @return the description, or {@code null} if it has none
     */
    public String getDescription() {
        final Description d = cls.getAnnotation(Description.class);
        return d == null ? null : d.value();
    }

    /**
     * The annotation of that type the event carries, if it carries it.
     *
     * @param <A> the type of the annotation
     * @param annotationType the type of the annotation
     * @return the annotation, or {@code null}
     */
    public <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
        return cls.getAnnotation(Objects.requireNonNull(annotationType, "annotationType"));
    }

    /**
     * The settings of the event: the three usual ones plus the ones of its own.
     *
     * <p>The note said "the four usual ones"; there are three --{@code threshold}, {@code
     * stackTrace} and {@code enabled}-- which is what the method builds and what the class javadoc
     * says. The four that do exist are the implicit <em>fields</em>, which are another thing.
     *
     * @return the settings
     */
    public List<SettingDescriptor> getSettingDescriptors() {
        return settings;
    }

    /**
     * The categories of the event, from its {@link Category}.
     *
     * <p>With no annotation it returns {@code ["Uncategorized"]}, which is what the JDK uses so
     * that an event with no category falls somewhere in the tree instead of not appearing.
     *
     * @return the categories, from the most general to the most specific
     */
    public List<String> getCategoryNames() {
        final Category c = cls.getAnnotation(Category.class);
        if (c == null || c.value().length == 0) {
            return Collections.singletonList("Uncategorized");
        }
        return Collections.unmodifiableList(Arrays.asList(c.value()));
    }
}
