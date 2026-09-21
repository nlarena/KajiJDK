package jdk.jfr;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The description of a field of an event: its name, its type and its annotations.
 *
 * <h2>What it adds over a reflection {@code Field}</h2>
 *
 * <p>That it does not need the field to exist. An event manufactured with {@link EventFactory} has
 * no Java class, and its fields are exactly these descriptors and nothing else.
 *
 * <p>And that on the other side, reading a recording, it is the only description there is: the
 * process that reads does not have the classes of the one that recorded.
 *
 * <h2>The accessors that look redundant</h2>
 *
 * <p>{@link #getLabel}, {@link #getDescription} and {@link #getContentType} come from the
 * annotations and could be asked for with {@link #getAnnotation}. They are apart because they are
 * the three a tool consults for <strong>each</strong> field when showing a table, and doing it by
 * the generic road would be a search by type in every cell.
 *
 * <p>{@link #getContentType} has a further twist: it does not return an annotation but the name of
 * the annotation that is in turn marked with {@link ContentType}. That is, it looks at the
 * meta-annotations. That is why it cannot be resolved with a direct {@code getAnnotation}.
 *
 * <h2>The arrays</h2>
 *
 * <p>{@link #getTypeName} of a {@code String[]} returns {@code "java.lang.String"} and
 * {@link #isArray} returns {@code true}. The condition of being an array goes apart from the name
 * and not stuck to it, which is what allows the one who reads to use the same type for the loose
 * field and for the array.
 *
 * @since 9
 */
public final class ValueDescriptor {

    private final String typeName;
    private final long typeId;
    private final boolean array;
    private final String name;
    private final List<AnnotationElement> annotations;

    /**
     * A field whose type <strong>has no Java class</strong>.
     *
     * <p>It is not API. It is needed for the {@code stackTrace} field every event carries, which is
     * of type {@code jdk.types.StackTrace} -- a type of the format of the recording and not a
     * class.
     */
    ValueDescriptor(final String typeName, final String name,
            final List<AnnotationElement> annotations) {
        this.typeName = typeName;
        this.typeId = Types.idOfName(typeName);
        this.array = false;
        this.name = name;
        this.annotations = Collections.unmodifiableList(
                new ArrayList<AnnotationElement>(annotations));
    }

    /**
     * A field with that type and that name, with no annotations.
     *
     * @param type the type
     * @param name the name
     * @throws NullPointerException if either is {@code null}
     * @throws IllegalArgumentException if the name is not a valid Java identifier
     */
    public ValueDescriptor(final Class<?> type, final String name) {
        this(type, name, Collections.<AnnotationElement>emptyList());
    }

    /**
     * A field with that type, that name and those annotations.
     *
     * @param type the type
     * @param name the name
     * @param annotations the annotations
     * @throws NullPointerException if any is {@code null}
     * @throws IllegalArgumentException if the name is not a valid Java identifier
     */
    public ValueDescriptor(final Class<?> type, final String name,
            final List<AnnotationElement> annotations) {
        Objects.requireNonNull(type, "type");
        this.name = Objects.requireNonNull(name, "name");
        Objects.requireNonNull(annotations, "annotations");
        if (!isIdentifier(name)) {
            throw new IllegalArgumentException(
                    "the name of a field has to be a valid Java identifier: " + name);
        }
        this.typeName = Types.name(type);
        this.typeId = Types.id(type);
        this.array = type.isArray();
        this.annotations = Collections.unmodifiableList(
                new ArrayList<AnnotationElement>(annotations));
    }

    private static boolean isIdentifier(final String s) {
        if (s.length() == 0 || !Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * The readable name of the field, from its {@link Label}.
     *
     * @return the label, or {@code null} if it has none
     */
    public String getLabel() {
        return text(Label.class);
    }

    /**
     * The name of the field.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * The explanation of the field, from its {@link Description}.
     *
     * @return the description, or {@code null} if it has none
     */
    public String getDescription() {
        return text(Description.class);
    }

    /**
     * The name of the annotation that gives the value its meaning, if it has one.
     *
     * <p>It returns, for example, {@code "jdk.jfr.Timespan"}: not the annotation but its name, and
     * not any one but the one that is in turn marked with {@link ContentType}.
     *
     * @return the name of the content type, or {@code null} if the field has none
     */
    public String getContentType() {
        for (final AnnotationElement a : annotations) {
            for (final AnnotationElement meta : a.getAnnotationElements()) {
                if (ContentType.class.getName().equals(meta.getTypeName())) {
                    return a.getTypeName();
                }
            }
        }
        return null;
    }

    /**
     * The name of the type of the field; for an array, that of its component.
     *
     * @return the name of the type
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * The numeric identifier of the type of the field.
     *
     * @return the identifier
     */
    public long getTypeId() {
        return typeId;
    }

    /**
     * Whether the field is an array.
     *
     * @return whether it is
     */
    public boolean isArray() {
        return array;
    }

    /**
     * The annotation of that type the field carries, if it carries it.
     *
     * @param <A> the type of the annotation
     * @param annotationType the type of the annotation
     * @return the annotation, or {@code null}
     */
    public <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
        Objects.requireNonNull(annotationType, "annotationType");
        for (final AnnotationElement a : annotations) {
            if (a.getTypeName().equals(annotationType.getName())) {
                return a.<A>getAnnotation(annotationType);
            }
        }
        return null;
    }

    /**
     * The annotations of the field.
     *
     * @return the annotations
     */
    public List<AnnotationElement> getAnnotationElements() {
        return annotations;
    }

    /**
     * The fields the type of this field has, if it is composite.
     *
     * <p>Empty for the simple types, which are the enormous majority of the fields of an event. The
     * format admits composite types --a network address with its host and its port-- and this is
     * how they are walked.
     *
     * @return the fields, or an empty list
     */
    public List<ValueDescriptor> getFields() {
        return Collections.emptyList();
    }

    /** The value of an annotation with a single {@code value} member of type text. */
    private String text(final Class<? extends Annotation> t) {
        for (final AnnotationElement a : annotations) {
            if (a.getTypeName().equals(t.getName()) && a.hasValue("value")) {
                final Object v = a.getValue("value");
                return v == null ? null : v.toString();
            }
        }
        return null;
    }
}
