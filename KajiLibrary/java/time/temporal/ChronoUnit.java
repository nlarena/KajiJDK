package java.time.temporal;

// KajiLibrary's java.time.temporal.ChronoUnit — the standard TemporalUnits, from NANOS to
// FOREVER. Each carries whether it's date-based or time-based; the operations delegate to the
// temporal (between → temporal1.until(temporal2, this), isSupportedBy → temporal.isSupported(this)).
// Each also carries its duration: exact for the time-based ones, **estimated** for the date-based
// ones -- a month is 30.4368 days on average, and `isDurationEstimated` is what warns that that
// number is no good for exact arithmetic.
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

    /**
     * How long this unit lasts.
     *
     * <p>The date-based ones are **estimated**: the year is 365.2425 days --the Gregorian average--
     * and the month a twelfth of that. It is not the value to use for adding months to a date; for
     * that there is `LocalDate.plusMonths`, which respects the real lengths.
     * `isDurationEstimated` tells them apart.
     */
    public java.time.Duration getDuration() {
        if (this == NANOS) {
            return java.time.Duration.ofNanos(1L);
        }
        if (this == MICROS) {
            return java.time.Duration.ofNanos(1000L);
        }
        if (this == MILLIS) {
            return java.time.Duration.ofNanos(1000000L);
        }
        if (this == SECONDS) {
            return java.time.Duration.ofSeconds(1L);
        }
        if (this == MINUTES) {
            return java.time.Duration.ofSeconds(60L);
        }
        if (this == HOURS) {
            return java.time.Duration.ofSeconds(3600L);
        }
        if (this == HALF_DAYS) {
            return java.time.Duration.ofSeconds(43200L);
        }
        if (this == DAYS) {
            return java.time.Duration.ofSeconds(86400L);
        }
        if (this == WEEKS) {
            return java.time.Duration.ofSeconds(7L * 86400L);
        }
        if (this == MONTHS) {
            return java.time.Duration.ofSeconds(31556952L / 12L);
        }
        if (this == YEARS) {
            return java.time.Duration.ofSeconds(31556952L);
        }
        if (this == DECADES) {
            return java.time.Duration.ofSeconds(31556952L * 10L);
        }
        if (this == CENTURIES) {
            return java.time.Duration.ofSeconds(31556952L * 100L);
        }
        if (this == MILLENNIA) {
            return java.time.Duration.ofSeconds(31556952L * 1000L);
        }
        if (this == ERAS) {
            return java.time.Duration.ofSeconds(31556952L * 1000000000L);
        }
        return java.time.Duration.ofSeconds(Long.MAX_VALUE, 999999999L);   // FOREVER
    }

    /** It returns `temporal` plus `amount` of this unit. */
    public <R extends Temporal> R addTo(R temporal, long amount) {
        return (R) temporal.plus(amount, this);
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
