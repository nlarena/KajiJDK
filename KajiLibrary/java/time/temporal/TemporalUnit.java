package java.time.temporal;

// KajiLibrary's java.time.temporal.TemporalUnit — a unit of date/time, such as days or hours.
// `ChronoUnit` is the standard enum of these. A KajiLibrary subset (the JDK also has addTo and
// getDuration).
public interface TemporalUnit {

    // How many of this unit lie between two temporals (exclusive of the end).
    long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive);

    boolean isSupportedBy(Temporal temporal);

    boolean isDateBased();

    boolean isTimeBased();

    boolean isDurationEstimated();
}
