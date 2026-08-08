package java.time.temporal;

// KajiLibrary's java.time.temporal.TemporalField — a field of a date/time, such as year or
// hour-of-day. `ChronoField` is the standard enum of these. A KajiLibrary subset (the JDK also
// has range/getBaseUnit/getRangeUnit/adjustInto/rangeRefinedBy).
public interface TemporalField {

    // The value of this field read from `temporal` (delegates to temporal.getLong(this)).
    long getFrom(TemporalAccessor temporal);

    boolean isSupportedBy(TemporalAccessor temporal);

    boolean isDateBased();

    boolean isTimeBased();
}
