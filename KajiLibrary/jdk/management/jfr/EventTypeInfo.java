package jdk.management.jfr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.management.openmbean.CompositeData;

/**
 * A {@link jdk.jfr.EventType} seen from the other side of a JMX connection.
 *
 * <p>It brings what is needed in order to <strong>choose</strong> what to record: name, label,
 * categories and the settings it admits.
 *
 * <p>What it does not bring are the fields of the event ({@code getFields}). It is coherent with
 * what it is for: a remote console uses this to put together the configuration screen, and the
 * fields only matter when reading the recording, which is done with the file at hand.
 *
 * @since 9
 */
public final class EventTypeInfo {

    private final String name;
    private final String label;
    private final String description;
    private final long id;
    private final List<String> categoryNames;
    private final List<SettingDescriptorInfo> settingDescriptors;

    EventTypeInfo(final String name, final String label, final String description,
            final long id, final List<String> categoryNames,
            final List<SettingDescriptorInfo> settingDescriptors) {
        this.name = name;
        this.label = label;
        this.description = description;
        this.id = id;
        this.categoryNames = Collections.unmodifiableList(new ArrayList<String>(categoryNames));
        this.settingDescriptors = Collections.unmodifiableList(
                new ArrayList<SettingDescriptorInfo>(settingDescriptors));
    }

    /**
     * The name of the type of event.
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
     * What this event records.
     *
     * @return the value
     */
    public String getDescription() {
        return description;
    }

    /**
     * The identifier of the type.
     *
     * @return the value
     */
    public long getId() {
        return id;
    }

    /**
     * The categories, from the most general to the most specific.
     *
     * @return the value
     */
    public List<String> getCategoryNames() {
        return categoryNames;
    }

    /**
     * The settings this type of event admits.
     *
     * @return the value
     */
    public List<SettingDescriptorInfo> getSettingDescriptors() {
        return settingDescriptors;
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
    public static EventTypeInfo from(final CompositeData cd) {
        if (cd == null) {
            return null;
        }
        throw new IllegalArgumentException(
                "rebuilding an EventTypeInfo needs the support of open types of JFR, which"
                + " this library does not implement");
    }

    /** {@inheritDoc} */
    public String toString() {
        final StringBuilder sb = new StringBuilder("EventTypeInfo{");
        sb.append("name=").append(String.valueOf(name));
        sb.append(", ");
        sb.append("label=").append(String.valueOf(label));
        sb.append(", ");
        sb.append("description=").append(String.valueOf(description));
        sb.append(", ");
        sb.append("id=").append(String.valueOf(id));
        sb.append(", ");
        sb.append("categoryNames=").append(String.valueOf(categoryNames));
        sb.append(", ");
        sb.append("settingDescriptors=").append(String.valueOf(settingDescriptors));
        return sb.append('}').toString();
    }
}
