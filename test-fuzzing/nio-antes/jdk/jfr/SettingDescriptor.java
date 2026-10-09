package jdk.jfr;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * The description of a <strong>setting</strong> of an event: {@code threshold}, {@code stackTrace}
 * or one of one's own.
 *
 * <h2>How it differs from a {@link ValueDescriptor}</h2>
 *
 * <p>A {@code ValueDescriptor} describes a <strong>datum</strong> of the event: something that is
 * recorded every time the event happens. A {@code SettingDescriptor} describes a
 * <strong>knob</strong>: something that is configured once and decides whether the event happens.
 *
 * <p>The confusion is easy because both have a name, a label, a description and a type. The
 * difference is in {@link #getDefaultValue}, which only makes sense for a knob -- a datum has no
 * default value, the event brings it.
 *
 * <h2>Why it has no public constructor</h2>
 *
 * <p>Because the settings are not invented on the side of the one who consults: they come from the
 * annotations of the event and from the methods marked with {@link SettingDefinition}. They are
 * obtained from {@link EventType#getSettingDescriptors}.
 *
 * @since 9
 */
public final class SettingDescriptor {

    private final String name;
    private final String typeName;
    private final long typeId;
    private final String label;
    private final String description;
    private final String defaultValue;
    private final List<AnnotationElement> annotations;

    /**
     * The label and the description are kept apart and do not come from the annotations, which is
     * what the JDK does: the usual settings have a label and an empty {@code
     * getAnnotationElements}. Checked against the JDK 25.
     */
    SettingDescriptor(final String name, final String typeName, final String label,
            final String description, final String defaultValue,
            final List<AnnotationElement> annotations) {
        this.name = name;
        this.typeName = typeName;
        this.typeId = Types.idOfName(typeName);
        this.label = label;
        this.description = description;
        this.defaultValue = defaultValue;
        this.annotations = Collections.unmodifiableList(annotations);
    }

    /**
     * The name of the setting, as it is written in a configuration.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * The readable name of the setting.
     *
     * @return the label, or {@code null} if it has none
     */
    public String getLabel() {
        return label;
    }

    /**
     * The explanation of the setting.
     *
     * @return the description, or {@code null} if it has none
     */
    public String getDescription() {
        return description;
    }

    /**
     * The name of the annotation that gives the value its meaning, if it has one.
     *
     * @return the name of the content type, or {@code null}
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
     * The name of the type of the setting.
     *
     * @return the name of the type
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * The numeric identifier of the type of the setting.
     *
     * @return the identifier
     */
    public long getTypeId() {
        return typeId;
    }

    /**
     * The annotation of that type the setting carries, if it carries it.
     *
     * @param <A> the type of the annotation
     * @param annotationType the type of the annotation
     * @return the annotation, or {@code null}
     */
    public <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
        for (final AnnotationElement a : annotations) {
            if (a.getTypeName().equals(annotationType.getName())) {
                return a.<A>getAnnotation(annotationType);
            }
        }
        return null;
    }

    /**
     * The annotations of the setting.
     *
     * @return the annotations
     */
    public List<AnnotationElement> getAnnotationElements() {
        return annotations;
    }

    /**
     * The value the setting starts with if nobody configures it.
     *
     * @return the default value
     */
    public String getDefaultValue() {
        return defaultValue;
    }
}
