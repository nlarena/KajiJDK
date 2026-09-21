package jdk.jfr;

import java.time.Duration;

/**
 * The settings of an event inside a recording, with a chainable interface.
 *
 * <h2>Why it is an abstract class with {@code final} methods</h2>
 *
 * <p>Because every convenient method --{@link #withThreshold}, {@link #withStackTrace} and
 * company-- is the same thing: a call to {@link #with} with the name of the setting and its
 * formatted value. The only thing that changes between implementations is {@code with}, and it is
 * the only abstract one.
 *
 * <p>That the others are {@code final} is not rigidity: it is what guarantees that
 * {@code withThreshold(Duration.ofMillis(20))} means exactly
 * {@code with("threshold", "20 ms")} in any implementation. If a subclass could redefine them, two
 * recordings configured the same could behave differently.
 *
 * <h2>It is chained and applied at the end</h2>
 *
 * <p>Each method returns the same object, so one writes
 * {@code r.enable("jdk.CPULoad").withPeriod(Duration.ofSeconds(1)).withStackTrace()}. The settings
 * go on accumulating and the recording takes them when it starts.
 *
 * @since 9
 */
public abstract class EventSettings {

    /** For the subclasses. */
    protected EventSettings() {
    }

    /**
     * It records the stack of calls.
     *
     * @return this same object
     */
    public final EventSettings withStackTrace() {
        return with(StackTrace.NAME, "true");
    }

    /**
     * It does not record the stack of calls.
     *
     * @return this same object
     */
    public final EventSettings withoutStackTrace() {
        return with(StackTrace.NAME, "false");
    }

    /**
     * It takes the threshold away: every event is recorded, however long it lasts.
     *
     * <p>It is {@code "0 ns"} and not an empty string: the setting goes on existing with a value
     * that discards nothing, which is different from having no setting.
     *
     * @return this same object
     */
    public final EventSettings withoutThreshold() {
        return with(Threshold.NAME, "0 ns");
    }

    /**
     * How often a periodic event is emitted.
     *
     * @param duration the period
     * @return this same object
     * @throws NullPointerException if it is {@code null}
     */
    public final EventSettings withPeriod(final Duration duration) {
        return with(Period.NAME, nanos(duration));
    }

    /**
     * The minimum duration for recording the event.
     *
     * @param duration the threshold
     * @return this same object
     * @throws NullPointerException if it is {@code null}
     */
    public final EventSettings withThreshold(final Duration duration) {
        return with(Threshold.NAME, nanos(duration));
    }

    /**
     * In nanoseconds, which is the unit JFR understands with no ambiguity.
     *
     * <p>It is formatted here and not in each caller so that the format is a single one: a
     * configuration file written by this API and one written by hand have to be able to be read the
     * same.
     */
    private static String nanos(final Duration d) {
        if (d == null) {
            throw new NullPointerException("duration");
        }
        return d.toNanos() + " ns";
    }

    /**
     * Any setting, by name.
     *
     * <p>It is the only method an implementation has to write, and the only road for the settings
     * of an event's own, which by definition have no convenient method.
     *
     * @param name the name of the setting
     * @param value the value
     * @return this same object
     */
    public abstract EventSettings with(String name, String value);
}
