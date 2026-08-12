package java.time.temporal;

// KajiLibrary's java.time.temporal.ChronoField — the standard TemporalFields the value types
// read/write. Each carries whether it's date-based or time-based; getFrom/isSupportedBy delegate
// to the temporal. A KajiLibrary subset (the JDK has ~30 fields plus a ValueRange and base/range
// units per field).
public enum ChronoField implements TemporalField {

    NANO_OF_SECOND(false, true),
    SECOND_OF_MINUTE(false, true),
    MINUTE_OF_HOUR(false, true),
    HOUR_OF_DAY(false, true),
    DAY_OF_WEEK(true, false),
    DAY_OF_MONTH(true, false),
    DAY_OF_YEAR(true, false),
    EPOCH_DAY(true, false),
    MONTH_OF_YEAR(true, false),
    YEAR(true, false);

    private final boolean dateBased;
    private final boolean timeBased;

    ChronoField(boolean dateBased, boolean timeBased) {
        this.dateBased = dateBased;
        this.timeBased = timeBased;
    }

    public long getFrom(TemporalAccessor temporal) {
        return temporal.getLong(this);
    }

    public boolean isSupportedBy(TemporalAccessor temporal) {
        return temporal.isSupported(this);
    }

    public boolean isDateBased() {
        return this.dateBased;
    }

    public boolean isTimeBased() {
        return this.timeBased;
    }
}
