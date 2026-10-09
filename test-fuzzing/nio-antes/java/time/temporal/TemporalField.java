package java.time.temporal;

// KajiLibrary's java.time.temporal.TemporalField -- a field of a date or a time, such as the year or
// the hour of the day. `ChronoField` is the standard enum of these.
//
// The interface describes the field from **two sides**, and that distinction is what organises the
// whole package:
//
//   - the **base unit** is what the field counts (the minute of the day counts minutes);
//   - the **range unit** is within what it counts them (within a day).
//
// Almost everything else follows from that pair: the range of valid values, whether the field is
// date-based or time-based, and how it is adjusted over a `Temporal`.
public interface TemporalField {

    /** What this field counts. The minute of the day counts `MINUTES`. */
    TemporalUnit getBaseUnit();

    /** Within what it counts them. The minute of the day is counted within `DAYS`. */
    TemporalUnit getRangeUnit();

    /**
     * The range of values the field allows **in general**.
     *
     * <p>It is the range without looking at any concrete date, and that is why `DAY_OF_MONTH` gives
     * 1..28/31: the maximum depends on the month, and here there is no month yet. For a given date's
     * range there is {@link #rangeRefinedBy(TemporalAccessor)}.
     */
    ValueRange range();

    /**
     * The range of values for **that** temporal.
     *
     * <p>It is the refined version of {@link #range()}: over a leap year's February,
     * `DAY_OF_MONTH` returns 1..29 and not 1..31.
     */
    ValueRange rangeRefinedBy(TemporalAccessor temporal);

    // The value of this field read from `temporal` (delegates to temporal.getLong(this)).
    long getFrom(TemporalAccessor temporal);

    boolean isSupportedBy(TemporalAccessor temporal);

    /**
     * It returns `temporal` with this field set to `newValue`.
     *
     * <p>The return type repeats the parameter's so the result keeps the concrete type: adjusting a
     * `LocalDate` returns a `LocalDate`, not a `Temporal` that has to be cast.
     */
    <R extends Temporal> R adjustInto(R temporal, long newValue);

    boolean isDateBased();

    boolean isTimeBased();

    /**
     * The field's name in that region.
     *
     * <p>It returns `toString()` for any region: this library does not carry the localisation data
     * for field names. It is documented instead of faked.
     */
    default String getDisplayName(java.util.Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        return this.toString();
    }

    /**
     * It resolves this field during parsing, out of the fields already read.
     *
     * <p>`null` --the default-- means "I know of no special way of resolving myself": the parser
     * takes the generic path. Only a field that knows how to derive itself from others overrides
     * it.
     */
    default TemporalAccessor resolve(java.util.Map<TemporalField, Long> fieldValues,
            TemporalAccessor partialTemporal, java.time.format.ResolverStyle resolverStyle) {
        return null;
    }
}
