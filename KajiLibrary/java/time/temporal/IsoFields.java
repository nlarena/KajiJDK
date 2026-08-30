package java.time.temporal;

// KajiLibrary's java.time.temporal.IsoFields — the ISO-8601 fields that the calendar year does not
// give you directly: the quarter, and the *week-based year*.
//
// The week-based year is the interesting one. ISO-8601 says a week belongs to the year that owns
// its THURSDAY, so a week never straddles two week-based years — which means the first days of
// January can belong to the previous week-based year, and the last days of December to the next.
// 2016-01-01 (a Friday) is week 53 of week-based-year 2015. That is not an edge case to paper
// over; it is the definition.
//
// The JDK models these as constants of two private inner enums (`Field` and `Unit`); ours are
// package-private top-level classes in this file, because a nested type doesn't resolve
// (finding #101).
public final class IsoFields {

    public static final TemporalField DAY_OF_QUARTER = new IsoField(0, "DayOfQuarter");

    public static final TemporalField QUARTER_OF_YEAR = new IsoField(1, "QuarterOfYear");

    public static final TemporalField WEEK_OF_WEEK_BASED_YEAR = new IsoField(2, "WeekOfWeekBasedYear");

    public static final TemporalField WEEK_BASED_YEAR = new IsoField(3, "WeekBasedYear");

    public static final TemporalUnit WEEK_BASED_YEARS = new IsoUnit(0, "WeekBasedYears");

    public static final TemporalUnit QUARTER_YEARS = new IsoUnit(1, "QuarterYears");

    private IsoFields() {
    }
}

// The four ISO fields, discriminated by `kind` — the shape a private inner enum would have given
// us. 0 = day-of-quarter, 1 = quarter-of-year, 2 = week-of-week-based-year, 3 = week-based-year.
final class IsoField implements TemporalField {

    private final int kind;
    private final String name;

    IsoField(int kind, String name) {
        this.kind = kind;
        this.name = name;
    }

    public long getFrom(TemporalAccessor temporal) {
        long result = 0L;
        if (this.kind == 0) {
            result = (long) IsoWeek.dayOfQuarter(temporal);
        } else if (this.kind == 1) {
            // Months 1-3 → 1, 4-6 → 2, 7-9 → 3, 10-12 → 4.
            result = (long) ((temporal.get(ChronoField.MONTH_OF_YEAR) + 2) / 3);
        } else if (this.kind == 2) {
            result = (long) IsoWeek.weekOfWeekBasedYear(temporal);
        } else {
            result = (long) IsoWeek.weekBasedYear(temporal);
        }
        return result;
    }

    // Las cuatro cosas que `TemporalField` pide para describir el campo: que cuenta, dentro de que,
    // que valores admite, y como se lo ajusta. Los cuatro campos ISO se derivan de la fecha, asi que
    // sus unidades salen de `ChronoUnit` y de las dos que este mismo archivo define.

    public TemporalUnit getBaseUnit() {
        if (this.kind == 0) {
            return ChronoUnit.DAYS;                      // dia del trimestre
        }
        if (this.kind == 1) {
            return IsoFields.QUARTER_YEARS;              // trimestre del año
        }
        if (this.kind == 2) {
            return ChronoUnit.WEEKS;                     // semana del año-de-semanas
        }
        return IsoFields.WEEK_BASED_YEARS;               // año-de-semanas
    }

    public TemporalUnit getRangeUnit() {
        if (this.kind == 0) {
            return IsoFields.QUARTER_YEARS;
        }
        if (this.kind == 1) {
            return ChronoUnit.YEARS;
        }
        if (this.kind == 2) {
            return IsoFields.WEEK_BASED_YEARS;
        }
        return ChronoUnit.FOREVER;
    }

    public ValueRange range() {
        if (this.kind == 0) {
            // Un trimestre tiene entre 90 y 92 dias: el primero del año bisiesto tiene 91.
            return ValueRange.of(1L, 90L, 92L);
        }
        if (this.kind == 1) {
            return ValueRange.of(1L, 4L);
        }
        if (this.kind == 2) {
            // Un año ISO tiene 52 semanas, y 53 los años largos.
            return ValueRange.of(1L, 52L, 53L);
        }
        return ChronoField.YEAR.range();
    }

    public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
        if (!this.isSupportedBy(temporal)) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + this.name);
        }
        return this.range();
    }

    public <R extends Temporal> R adjustInto(R temporal, long newValue) {
        // Se ajusta **por diferencia**: se calcula cuanto hay que moverse en la unidad base y se
        // suma. Poner el campo directamente pediria reconstruir la fecha desde el calendario ISO de
        // semanas, que es una cuenta aparte; moverse la cantidad justa da el mismo resultado.
        long actual = this.getFrom(temporal);
        this.range().checkValidValue(newValue, this);
        return (R) temporal.plus(newValue - actual, this.getBaseUnit());
    }

    public boolean isSupportedBy(TemporalAccessor temporal) {
        // Every one of the four is derived from the calendar date, so the date fields it is
        // computed from are exactly what has to be there.
        boolean ok = temporal.isSupported(ChronoField.DAY_OF_YEAR)
                && temporal.isSupported(ChronoField.MONTH_OF_YEAR)
                && temporal.isSupported(ChronoField.YEAR);
        if (ok && this.kind >= 2) {
            ok = temporal.isSupported(ChronoField.DAY_OF_WEEK);
        }
        return ok;
    }

    public boolean isDateBased() {
        return true;
    }

    public boolean isTimeBased() {
        return false;
    }

    public String toString() {
        return this.name;
    }
}

// The two ISO units. 0 = week-based years, 1 = quarter years.
final class IsoUnit implements TemporalUnit {

    private final int kind;
    private final String name;

    IsoUnit(int kind, String name) {
        this.kind = kind;
        this.name = name;
    }

    /**
     * Cuanto dura, **estimado**: un año-de-semanas es el año medio, un trimestre su cuarta parte.
     *
     * <p>Las dos son estimaciones y `isDurationEstimated` lo dice: un año ISO tiene 52 o 53 semanas
     * segun cual sea, y los trimestres tienen 90, 91 o 92 dias.
     */
    public java.time.Duration getDuration() {
        if (this.kind == 0) {
            return java.time.Duration.ofSeconds(31556952L);        // año-de-semanas
        }
        return java.time.Duration.ofSeconds(31556952L / 4L);       // trimestre
    }

    /** Devuelve `temporal` mas `amount` de esta unidad. */
    public <R extends Temporal> R addTo(R temporal, long amount) {
        if (this.kind == 0) {
            // Sumar años-de-semanas: se mueve el campo del año-de-semanas, que es lo unico que
            // conserva la semana y el dia de la semana. Sumar 52 semanas no sirve -- se desfasa en
            // los años de 53.
            long actual = IsoFields.WEEK_BASED_YEAR.getFrom(temporal);
            return (R) IsoFields.WEEK_BASED_YEAR.adjustInto(temporal, actual + amount);
        }
        return (R) temporal.plus(amount * 3L, ChronoUnit.MONTHS);   // un trimestre son tres meses
    }

    public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
        long result = 0L;
        if (this.kind == 0) {
            // Whole week-based years between the two dates: the difference of the week-based
            // years, minus one if the second hasn't reached the first's week-of-year yet.
            long years = (long) (IsoWeek.weekBasedYear(temporal2Exclusive)
                    - IsoWeek.weekBasedYear(temporal1Inclusive));
            int w1 = IsoWeek.weekOfWeekBasedYear(temporal1Inclusive);
            int w2 = IsoWeek.weekOfWeekBasedYear(temporal2Exclusive);
            int d1 = temporal1Inclusive.get(ChronoField.DAY_OF_WEEK);
            int d2 = temporal2Exclusive.get(ChronoField.DAY_OF_WEEK);
            if (years > 0L && (w2 < w1 || (w2 == w1 && d2 < d1))) {
                years = years - 1L;
            } else if (years < 0L && (w2 > w1 || (w2 == w1 && d2 > d1))) {
                years = years + 1L;
            }
            result = years;
        } else {
            // A quarter is exactly three months, so this is months/3 truncated toward zero —
            // which is what `until(MONTHS)` already gives.
            result = temporal1Inclusive.until(temporal2Exclusive, ChronoUnit.MONTHS) / 3L;
        }
        return result;
    }

    public boolean isSupportedBy(Temporal temporal) {
        return temporal.isSupported(ChronoField.EPOCH_DAY);
    }

    public boolean isDateBased() {
        return true;
    }

    public boolean isTimeBased() {
        return false;
    }

    public boolean isDurationEstimated() {
        // Both vary in length (a week-based year is 52 or 53 weeks; a quarter, 90 to 92 days).
        return true;
    }

    public String toString() {
        return this.name;
    }
}

// The arithmetic, kept out of the field/unit classes so both can share it and so the rules are
// readable on their own.
final class IsoWeek {

    private IsoWeek() {
    }

    // Day-of-year of the first day of each quarter, minus one: the value to subtract from
    // day-of-year to get day-of-quarter. Built here rather than in a `static final int[]` so the
    // lookup can't be caught by a constant-folding gap; the branch is cheap and explicit.
    static int quarterStart(int quarter, boolean leap) {
        int start = 0;
        if (quarter == 2) {
            start = 90;
        } else if (quarter == 3) {
            start = 181;
        } else if (quarter == 4) {
            start = 273;
        }
        if (leap && quarter > 1) {
            start = start + 1;
        }
        return start;
    }

    static boolean isLeap(long year) {
        return (year % 4L == 0L) && ((year % 100L != 0L) || (year % 400L == 0L));
    }

    static int dayOfQuarter(TemporalAccessor temporal) {
        int doy = temporal.get(ChronoField.DAY_OF_YEAR);
        int quarter = (temporal.get(ChronoField.MONTH_OF_YEAR) + 2) / 3;
        boolean leap = IsoWeek.isLeap(temporal.getLong(ChronoField.YEAR));
        return doy - IsoWeek.quarterStart(quarter, leap);
    }

    // The day-of-week of January 1st of `year`, as 0=Monday..6=Sunday. This is the whole ISO week
    // calendar in one expression: the year's day-count mod 7, accumulated over the leap rule.
    static int jan1Dow(long year) {
        long y = year - 1L;
        long days = 365L * y + y / 4L - y / 100L + y / 400L;
        // Day 0 of the proleptic ISO calendar (0001-01-01) is a Monday. Floor-mod by hand:
        // `%` truncates toward zero, so a proleptic year before 1 would land on a negative index.
        long dow = days % 7L;
        if (dow < 0L) {
            dow = dow + 7L;
        }
        return (int) dow;
    }

    // 52 or 53. A year is long when it starts on a Thursday, or is a leap year starting on a
    // Wednesday — the two ways to fit a 53rd Thursday.
    static int weeksInYear(long year) {
        int jan1 = IsoWeek.jan1Dow(year);
        int weeks = 52;
        if (jan1 == 3) {
            weeks = 53;
        } else if (jan1 == 2 && IsoWeek.isLeap(year)) {
            weeks = 53;
        }
        return weeks;
    }

    // The ISO week number, and the year that owns it. Both come from the same intermediate, so
    // they're computed together and the callers pick a half.
    //
    //   week = (dayOfYear - dayOfWeek + 10) / 7
    //
    // reads as: shift the date to the Thursday of its week, then count sevens. week 0 means the
    // Thursday fell in the previous year; week 53 on a short year means it fell in the next.
    static int rawWeek(TemporalAccessor temporal) {
        int doy = temporal.get(ChronoField.DAY_OF_YEAR);
        int dow = temporal.get(ChronoField.DAY_OF_WEEK);
        return (doy - dow + 10) / 7;
    }

    static int weekOfWeekBasedYear(TemporalAccessor temporal) {
        long year = temporal.getLong(ChronoField.YEAR);
        int week = IsoWeek.rawWeek(temporal);
        int result = week;
        if (week < 1) {
            result = IsoWeek.weeksInYear(year - 1L);
        } else if (week > IsoWeek.weeksInYear(year)) {
            result = 1;
        }
        return result;
    }

    static int weekBasedYear(TemporalAccessor temporal) {
        long year = temporal.getLong(ChronoField.YEAR);
        int week = IsoWeek.rawWeek(temporal);
        long result = year;
        if (week < 1) {
            result = year - 1L;
        } else if (week > IsoWeek.weeksInYear(year)) {
            result = year + 1L;
        }
        return (int) result;
    }
}
