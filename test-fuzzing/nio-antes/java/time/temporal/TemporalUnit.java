package java.time.temporal;

// KajiLibrary's java.time.temporal.TemporalUnit — a unit of date/time, such as days or hours.
// `ChronoUnit` is the standard enum of these.
public interface TemporalUnit {

    /**
     * It returns `temporal` plus `amount` of this unit.
     *
     * <p>The return type repeats the parameter's so the result keeps the concrete type: adding days
     * to a `LocalDate` returns a `LocalDate`.
     */
    <R extends Temporal> R addTo(R temporal, long amount);

    /**
     * How long this unit lasts.
     *
     * <p>For the **estimated** units --months, years-- it is an average, and that is why
     * `isDurationEstimated` exists: using this value for exact arithmetic over those units gives a
     * wrong result.
     */
    java.time.Duration getDuration();

    // How many of this unit lie between two temporals (exclusive of the end).
    long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive);

    boolean isSupportedBy(Temporal temporal);

    boolean isDateBased();

    boolean isTimeBased();

    boolean isDurationEstimated();
}
