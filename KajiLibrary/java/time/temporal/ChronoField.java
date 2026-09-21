package java.time.temporal;

// KajiLibrary's java.time.temporal.ChronoField -- the standard `TemporalField`s, all thirty.
//
// Each is described by **four** things, and everything else follows from them: what it counts (the
// base unit), within what it counts it (the range unit), which values it allows, and whether it is
// date-based or time-based. `MINUTE_OF_DAY` counts minutes within a day, allows 0..1439, and is
// time-based.
//
// The declaration order is the JDK's, and that is not cosmetic: `values()` and `ordinal()` are
// observable, so reordering them would be a difference in behaviour.
//
// **The two pairs that get confused.** `HOUR_OF_AMPM` runs 0..11 and `CLOCK_HOUR_OF_AMPM` runs 1..12
// --a clock has no "0 o'clock", it has "12"--; likewise `HOUR_OF_DAY` (0..23) against
// `CLOCK_HOUR_OF_DAY` (1..24). Choosing the wrong one gives an error of one hour twice a day, which
// is exactly the kind of error that survives a careless test.
public enum ChronoField implements TemporalField {

    NANO_OF_SECOND(ChronoUnit.NANOS, ChronoUnit.SECONDS, 0L, 999999999L, false, true),
    NANO_OF_DAY(ChronoUnit.NANOS, ChronoUnit.DAYS, 0L, 86399999999999L, false, true),
    MICRO_OF_SECOND(ChronoUnit.MICROS, ChronoUnit.SECONDS, 0L, 999999L, false, true),
    MICRO_OF_DAY(ChronoUnit.MICROS, ChronoUnit.DAYS, 0L, 86399999999L, false, true),
    MILLI_OF_SECOND(ChronoUnit.MILLIS, ChronoUnit.SECONDS, 0L, 999L, false, true),
    MILLI_OF_DAY(ChronoUnit.MILLIS, ChronoUnit.DAYS, 0L, 86399999L, false, true),
    SECOND_OF_MINUTE(ChronoUnit.SECONDS, ChronoUnit.MINUTES, 0L, 59L, false, true),
    SECOND_OF_DAY(ChronoUnit.SECONDS, ChronoUnit.DAYS, 0L, 86399L, false, true),
    MINUTE_OF_HOUR(ChronoUnit.MINUTES, ChronoUnit.HOURS, 0L, 59L, false, true),
    MINUTE_OF_DAY(ChronoUnit.MINUTES, ChronoUnit.DAYS, 0L, 1439L, false, true),
    HOUR_OF_AMPM(ChronoUnit.HOURS, ChronoUnit.HALF_DAYS, 0L, 11L, false, true),
    CLOCK_HOUR_OF_AMPM(ChronoUnit.HOURS, ChronoUnit.HALF_DAYS, 1L, 12L, false, true),
    HOUR_OF_DAY(ChronoUnit.HOURS, ChronoUnit.DAYS, 0L, 23L, false, true),
    CLOCK_HOUR_OF_DAY(ChronoUnit.HOURS, ChronoUnit.DAYS, 1L, 24L, false, true),
    AMPM_OF_DAY(ChronoUnit.HALF_DAYS, ChronoUnit.DAYS, 0L, 1L, false, true),
    DAY_OF_WEEK(ChronoUnit.DAYS, ChronoUnit.WEEKS, 1L, 7L, true, false),
    ALIGNED_DAY_OF_WEEK_IN_MONTH(ChronoUnit.DAYS, ChronoUnit.WEEKS, 1L, 7L, true, false),
    ALIGNED_DAY_OF_WEEK_IN_YEAR(ChronoUnit.DAYS, ChronoUnit.WEEKS, 1L, 7L, true, false),
    // The small maximum is 28 --a non-leap February-- and the large one 31. `range()` gives the
    // general range; `rangeRefinedBy` over a concrete date refines it.
    DAY_OF_MONTH(ChronoUnit.DAYS, ChronoUnit.MONTHS, 1L, 28L, 31L, true, false),
    DAY_OF_YEAR(ChronoUnit.DAYS, ChronoUnit.YEARS, 1L, 365L, 366L, true, false),
    EPOCH_DAY(ChronoUnit.DAYS, ChronoUnit.FOREVER, -365243219162L, 365241780471L, true, false),
    ALIGNED_WEEK_OF_MONTH(ChronoUnit.WEEKS, ChronoUnit.MONTHS, 1L, 4L, 5L, true, false),
    ALIGNED_WEEK_OF_YEAR(ChronoUnit.WEEKS, ChronoUnit.YEARS, 1L, 53L, true, false),
    MONTH_OF_YEAR(ChronoUnit.MONTHS, ChronoUnit.YEARS, 1L, 12L, true, false),
    PROLEPTIC_MONTH(ChronoUnit.MONTHS, ChronoUnit.FOREVER, -11999999988L, 11999999999L, true, false),
    YEAR_OF_ERA(ChronoUnit.YEARS, ChronoUnit.FOREVER, 1L, 999999999L, 1000000000L, true, false),
    YEAR(ChronoUnit.YEARS, ChronoUnit.FOREVER, -999999999L, 999999999L, true, false),
    ERA(ChronoUnit.ERAS, ChronoUnit.FOREVER, 0L, 1L, true, false),
    // The last two are the only ones that are **neither date-based nor time-based**, and that is why
    // they carry `false` in both flags. They are measured in seconds, which invites marking them as
    // time-based --and so they were-- but that is what they are not: a `LocalTime` can answer neither
    // of them. One needs date, time and zone at once; the other is the offset itself, which is not an
    // instant within the day. Marking them time-based made `LocalTime.isSupported(OFFSET_SECONDS)` say
    // yes and then `getLong` throw, which is exactly the contradiction `isSupported` exists to
    // prevent.
    INSTANT_SECONDS(ChronoUnit.SECONDS, ChronoUnit.FOREVER, Long.MIN_VALUE, Long.MAX_VALUE, false, false),
    // +-18 hours: the maximum the specification allows for a zone offset.
    OFFSET_SECONDS(ChronoUnit.SECONDS, ChronoUnit.FOREVER, -64800L, 64800L, false, false);

    private final TemporalUnit baseUnit;
    private final TemporalUnit rangeUnit;
    private final ValueRange range;
    private final boolean dateBased;
    private final boolean timeBased;

    ChronoField(TemporalUnit baseUnit, TemporalUnit rangeUnit, long min, long max,
            boolean dateBased, boolean timeBased) {
        this.baseUnit = baseUnit;
        this.rangeUnit = rangeUnit;
        this.range = ValueRange.of(min, max);
        this.dateBased = dateBased;
        this.timeBased = timeBased;
    }

    ChronoField(TemporalUnit baseUnit, TemporalUnit rangeUnit, long min, long maxSmallest,
            long maxLargest, boolean dateBased, boolean timeBased) {
        this.baseUnit = baseUnit;
        this.rangeUnit = rangeUnit;
        this.range = ValueRange.of(min, maxSmallest, maxLargest);
        this.dateBased = dateBased;
        this.timeBased = timeBased;
    }

    public TemporalUnit getBaseUnit() {
        return this.baseUnit;
    }

    public TemporalUnit getRangeUnit() {
        return this.rangeUnit;
    }

    public ValueRange range() {
        return this.range;
    }

    /**
     * This field's range **for that** temporal.
     *
     * <p>It asks the temporal, which is the one that can refine: `DAY_OF_MONTH` over a leap year's
     * February gives 1..29, not the general 1..28/31.
     */
    public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        return temporal.range(this);
    }

    public long getFrom(TemporalAccessor temporal) {
        return temporal.getLong(this);
    }

    public boolean isSupportedBy(TemporalAccessor temporal) {
        return temporal.isSupported(this);
    }

    public <R extends Temporal> R adjustInto(R temporal, long newValue) {
        return (R) temporal.with(this, newValue);
    }

    public boolean isDateBased() {
        return this.dateBased;
    }

    public boolean isTimeBased() {
        return this.timeBased;
    }

    /**
     * It checks that `value` is in the field's general range, and returns it.
     *
     * <p>It returns the value instead of a boolean on purpose: that way it chains into the expression
     * that uses it (`field.checkValidValue(v)`) and there is no way of forgetting to look at the
     * result.
     *
     * @throws java.time.DateTimeException if it is out of range
     */
    public long checkValidValue(long value) {
        return this.range.checkValidValue(value, this);
    }

    /**
     * The same, and that it fits in an `int` as well.
     *
     * @throws java.time.DateTimeException if it is out of range or does not fit in an `int`
     */
    public int checkValidIntValue(long value) {
        return this.range.checkValidIntValue(value, this);
    }

    /** The field's name. See `TemporalField.getDisplayName`'s note: it does not depend on the region. */
    public String getDisplayName(java.util.Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        return this.toString();
    }
}
