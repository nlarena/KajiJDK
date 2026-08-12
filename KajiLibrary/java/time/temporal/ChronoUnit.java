package java.time.temporal;

// KajiLibrary's java.time.temporal.ChronoUnit — the standard TemporalUnits, from NANOS to
// FOREVER. Each carries whether it's date-based or time-based; the operations delegate to the
// temporal (between → temporal1.until(temporal2, this), isSupportedBy → temporal.isSupported(this)).
// A KajiLibrary subset (the JDK's ChronoUnit also carries an estimated Duration per unit).
public enum ChronoUnit implements TemporalUnit {

    NANOS(false, true),
    MICROS(false, true),
    MILLIS(false, true),
    SECONDS(false, true),
    MINUTES(false, true),
    HOURS(false, true),
    HALF_DAYS(false, true),
    DAYS(true, false),
    WEEKS(true, false),
    MONTHS(true, false),
    YEARS(true, false),
    DECADES(true, false),
    CENTURIES(true, false),
    MILLENNIA(true, false),
    ERAS(true, false),
    FOREVER(false, false);

    private final boolean dateBased;
    private final boolean timeBased;

    ChronoUnit(boolean dateBased, boolean timeBased) {
        this.dateBased = dateBased;
        this.timeBased = timeBased;
    }

    public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
        return temporal1Inclusive.until(temporal2Exclusive, this);
    }

    public boolean isSupportedBy(Temporal temporal) {
        return temporal.isSupported(this);
    }

    public boolean isDateBased() {
        return this.dateBased;
    }

    public boolean isTimeBased() {
        return this.timeBased;
    }

    // Estimated for date-based units (their length varies) and for FOREVER.
    public boolean isDurationEstimated() {
        return this.dateBased || this == ChronoUnit.FOREVER;
    }
}
