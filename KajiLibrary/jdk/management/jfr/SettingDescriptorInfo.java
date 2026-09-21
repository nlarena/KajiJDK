package jdk.management.jfr;

import java.util.Collections;

import javax.management.openmbean.CompositeData;

/**
 * A {@link jdk.jfr.SettingDescriptor} seen from the other side of a JMX connection.
 *
 * <p>The same datum, without the annotations:
 * {@link jdk.jfr.SettingDescriptor#getAnnotationElements} has no equivalent here because an
 * annotation cannot be represented as an open type.
 *
 * <p>What does survive is what a console needs in order to draw a configuration form: name, label,
 * description, type and default value.
 *
 * @since 9
 */
public final class SettingDescriptorInfo {

    private final String name;
    private final String label;
    private final String description;
    private final String typeName;
    private final String contentType;
    private final String defaultValue;

    SettingDescriptorInfo(final String name, final String label, final String description,
            final String typeName, final String contentType, final String defaultValue) {
        this.name = name;
        this.label = label;
        this.description = description;
        this.typeName = typeName;
        this.contentType = contentType;
        this.defaultValue = defaultValue;
    }

    /**
     * The name of the setting.
     *
     * @return the value
     */
    public String getName() {
        return name;
    }

    /**
     * The readable name.
     *
     * @return the value
     */
    public String getLabel() {
        return label;
    }

    /**
     * What the setting does.
     *
     * @return the value
     */
    public String getDescription() {
        return description;
    }

    /**
     * The name of the type of the setting.
     *
     * @return the value
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * The name of the annotation that gives the value meaning, or {@code null}.
     *
     * @return the value
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * The value it starts with.
     *
     * @return the value
     */
    public String getDefaultValue() {
        return defaultValue;
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
    public static SettingDescriptorInfo from(final CompositeData cd) {
        if (cd == null) {
            return null;
        }
        throw new IllegalArgumentException(
                "rebuilding a SettingDescriptorInfo needs the support of open types "
                + "of JFR, which this library does not implement");
    }

    /** {@inheritDoc} */
    public String toString() {
        final StringBuilder sb = new StringBuilder("SettingDescriptorInfo{");
        sb.append("name=").append(String.valueOf(name));
        sb.append(", ");
        sb.append("label=").append(String.valueOf(label));
        sb.append(", ");
        sb.append("description=").append(String.valueOf(description));
        sb.append(", ");
        sb.append("typeName=").append(String.valueOf(typeName));
        sb.append(", ");
        sb.append("contentType=").append(String.valueOf(contentType));
        sb.append(", ");
        sb.append("defaultValue=").append(String.valueOf(defaultValue));
        return sb.append('}').toString();
    }
}
