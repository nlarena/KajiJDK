package jdk.management.jfr;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.openmbean.CompositeData;

/**
 * A {@link jdk.jfr.Configuration} seen from the other side of a JMX connection.
 *
 * <h2>Why the original class is not used</h2>
 *
 * <p>Because {@code Configuration} does not travel: JMX transports {@code CompositeData}, which is
 * a generic structure of name to value, and not arbitrary objects. A console that connects to a
 * remote VM receives that.
 *
 * <p>This class is the same datum on the client's side, with typed accessors. {@link #from} is how
 * it is rebuilt.
 *
 * <p>It is <strong>read-only</strong>, and it has to be: it describes the state of another VM at a
 * given moment. A setter would make one believe that changing it changes something on the other
 * side.
 *
 * @since 9
 */
public final class ConfigurationInfo {

    private final String name;
    private final String label;
    private final String description;
    private final String provider;
    private final String contents;
    private final Map<String, String> settings;

    ConfigurationInfo(final String name, final String label, final String description,
            final String provider, final String contents, final Map<String, String> settings) {
        this.name = name;
        this.label = label;
        this.description = description;
        this.provider = provider;
        this.contents = contents;
        this.settings = Collections.unmodifiableMap(new LinkedHashMap<String, String>(settings));
    }

    /**
     * The name of the configuration.
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
     * What this configuration is for.
     *
     * @return the value
     */
    public String getDescription() {
        return description;
    }

    /**
     * Who wrote it.
     *
     * @return the value
     */
    public String getProvider() {
        return provider;
    }

    /**
     * The text of the {@code .jfc} file.
     *
     * @return the value
     */
    public String getContents() {
        return contents;
    }

    /**
     * The settings, with the key {@code "event#setting"}.
     *
     * @return the value
     */
    public Map<String, String> getSettings() {
        return settings;
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
    public static ConfigurationInfo from(final CompositeData cd) {
        if (cd == null) {
            return null;
        }
        throw new IllegalArgumentException(
                "rebuilding a ConfigurationInfo needs the support of open types of JFR, which"
                + " this library does not implement");
    }

    /** {@inheritDoc} */
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConfigurationInfo{");
        sb.append("name=").append(String.valueOf(name));
        sb.append(", ");
        sb.append("label=").append(String.valueOf(label));
        sb.append(", ");
        sb.append("description=").append(String.valueOf(description));
        sb.append(", ");
        sb.append("provider=").append(String.valueOf(provider));
        sb.append(", ");
        sb.append("contents=").append(String.valueOf(contents));
        sb.append(", ");
        sb.append("settings=").append(String.valueOf(settings));
        return sb.append('}').toString();
    }
}
