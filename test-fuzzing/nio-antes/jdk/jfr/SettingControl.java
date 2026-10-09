package jdk.jfr;

import java.util.Set;

/**
 * A setting of an event's own: how it is read, how it is written and how several are combined.
 *
 * <h2>Why {@link #combine} is needed</h2>
 *
 * <p>It is the part that is not obvious, and it is the reason why this class exists instead of a
 * simple pair of accessors.
 *
 * <p>There may be <strong>several recordings at a time</strong>, each with its configuration. If
 * one asks for a threshold of 10 ms and another for one of 100 ms, the event has to be emitted with
 * the threshold that satisfies both --10 ms, the more demanding one-- because otherwise the first
 * recording loses events it asked for.
 *
 * <p>{@link #combine} receives every requested value and returns the one that is going to be used.
 * For a threshold that is the minimum; for a boolean of "record the stack" it is the {@code true};
 * for something enumerated it may be something else. Only the one who defined the setting knows
 * which, and that is why they decide it.
 *
 * <h2>The value is always text</h2>
 *
 * <p>Because it has to be able to come from a configuration file, from {@code jcmd} and from the
 * API, and the only format that serves for the three is a string. Interpreting it is the job of the
 * subclass.
 *
 * @since 9
 */
public abstract class SettingControl {

    /** For the subclasses. */
    protected SettingControl() {
    }

    /**
     * The value that results from combining the ones every active recording asked for.
     *
     * <p>It is called every time a recording starts, stops and changes its settings.
     *
     * @param settingValues the requested values; never empty
     * @return the value to use
     */
    public abstract String combine(Set<String> settingValues);

    /**
     * It sets the effective value.
     *
     * <p>JFR calls it with the result of {@link #combine}, not the user.
     *
     * @param settingValue the value
     */
    public abstract void setValue(String settingValue);

    /**
     * The effective value.
     *
     * @return the value
     */
    public abstract String getValue();
}
