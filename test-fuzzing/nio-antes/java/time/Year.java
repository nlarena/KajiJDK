package java.time;

import java.io.Serializable;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

// KajiLibrary's java.time.Year — a year on the ISO calendar, e.g. 2026. Immutable value type.
// Implements Temporal, TemporalAdjuster and Comparable. A KajiLibrary subset.
public final class Year implements Temporal, TemporalAdjuster, Comparable<Year>, Serializable {

    private final int year;

    private Year(int year) {
        this.year = year;
    }

    /** The earliest representable year. */
    public static final int MIN_VALUE = -999999999;

    /** The latest. */
    public static final int MAX_VALUE = 999999999;

    /** The year `temporal` holds. */
    public static Year from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof Year) {
            return (Year) temporal;
        }
        return Year.of(temporal.get(ChronoField.YEAR));
    }

    /** The year `clock` reads. The testable form of `now()`. */
    public static Year now(java.time.Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        return Year.of(LocalDate.now(clock).getYear());
    }

    /** The year in that zone, right now. */
    public static Year now(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return Year.of(LocalDate.now(zone).getYear());
    }

    public static Year of(int isoYear) {
        return new Year(isoYear);
    }

    public static Year now() {
        return Year.of(LocalDate.now().getYear());
    }

    public static boolean isLeap(long year) {
        return ((year & 3L) == 0) && ((year % 100L != 0) || (year % 400L == 0));
    }

    public int getValue() {
        return this.year;
    }

    public boolean isLeap() {
        return Year.isLeap(this.year);
    }

    public int length() {
        return this.isLeap() ? 366 : 365;
    }

    /**
     * Whether that month-and-day exists in this year.
     *
     * <p>The only case in which it does not is the 29th of February of a common year -- and that is
     * exactly what the method exists for.
     */
    public boolean isValidMonthDay(MonthDay monthDay) {
        return monthDay != null && monthDay.isValidYear(this.getValue());
    }

    /**
     * Day number `dayOfYear` of this year.
     *
     * @throws java.time.DateTimeException if the day does not exist -- the 366th in a common year
     */
    public LocalDate atDay(int dayOfYear) {
        return LocalDate.ofYearDay(this.getValue(), dayOfYear);
    }

    /** This year with that month-and-day. */
    public LocalDate atMonthDay(MonthDay monthDay) {
        if (monthDay == null) {
            throw new NullPointerException("monthDay");
        }
        return monthDay.atYear(this.getValue());
    }

    /** This year with that month. */
    public YearMonth atMonth(int month) {
        return YearMonth.of(this.getValue(), month);
    }

    public YearMonth atMonth(Month month) {
        if (month == null) {
            throw new NullPointerException("month");
        }
        return YearMonth.of(this.getValue(), month.getValue());
    }

    /**
     * This year formatted.
     *
     * @throws java.time.DateTimeException if it cannot be formatted
     */
    public String format(java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        return formatter.format(this);
    }

    /**
     * This year plus `amountToAdd` units.
     *
     * @throws java.time.DateTimeException if the unit is not a year-based one
     */
    public Year plus(long amountToAdd, java.time.temporal.TemporalUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (unit == ChronoUnit.YEARS) {
            return this.plusYears(amountToAdd);
        }
        if (unit == ChronoUnit.DECADES) {
            return this.plusYears(amountToAdd * 10L);
        }
        if (unit == ChronoUnit.CENTURIES) {
            return this.plusYears(amountToAdd * 100L);
        }
        if (unit == ChronoUnit.MILLENNIA) {
            return this.plusYears(amountToAdd * 1000L);
        }
        if (unit == ChronoUnit.ERAS) {
            // An ISO era is every year of one sign: adding one takes year `y` to `1-y`.
            long era = this.getLong(ChronoField.ERA);
            return (Year) this.with(ChronoField.ERA, era + amountToAdd);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    public Year minus(long amountToSubtract, java.time.temporal.TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    /** This year with `field` set to `newValue`. */
    public Year with(java.time.temporal.TemporalField field, long newValue) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (field == ChronoField.YEAR) {
            return Year.of((int) ChronoField.YEAR.checkValidValue(newValue));
        }
        if (field == ChronoField.YEAR_OF_ERA) {
            ChronoField.YEAR_OF_ERA.checkValidValue(newValue);
            return Year.of(this.getValue() < 1 ? (int) (1L - newValue) : (int) newValue);
        }
        if (field == ChronoField.ERA) {
            ChronoField.ERA.checkValidValue(newValue);
            // Changing era reflects the year about 1: year 5 of the earlier era is -4.
            long yoe = this.getLong(ChronoField.YEAR_OF_ERA);
            return Year.of(newValue == 0L ? (int) (1L - yoe) : (int) yoe);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
    }

    public Year plusYears(long yearsToAdd) {
        return new Year((int) (this.year + yearsToAdd));
    }

    public Year minusYears(long yearsToSubtract) {
        return this.plusYears(-yearsToSubtract);
    }

    public boolean isBefore(Year other) {
        return this.year < other.year;
    }

    public boolean isAfter(Year other) {
        return this.year > other.year;
    }

    public int compareTo(Year other) {
        return this.year - other.year;
    }

    // --- Temporal ---

    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return field == ChronoField.YEAR || field == ChronoField.YEAR_OF_ERA
                || field == ChronoField.ERA;
        }
        return field != null && field.isSupportedBy(this);
    }

    /**
     * That field's value.
     *
     * <p>`ERA` and `YEAR_OF_ERA` are the same year said another way --the era, and the count within
     * it-- so they are derived with nothing to decide. They used to be missing, and that is why a
     * formatter with `G` or with `yyyy` in a calendar with eras met a rejection where there was
     * information to spare.
     */
    public long getLong(TemporalField field) {
        if (field == ChronoField.YEAR) {
            return (long) this.year;
        }
        if (field == ChronoField.YEAR_OF_ERA) {
            return (long) (this.year >= 1 ? this.year : 1 - this.year);
        }
        if (field == ChronoField.ERA) {
            return (long) (this.year >= 1 ? 1 : 0);
        }
        if (field != null && !(field instanceof ChronoField)) {
            return field.getFrom(this);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
    }

    public boolean isSupported(TemporalUnit unit) {
        return unit == ChronoUnit.YEARS || unit == ChronoUnit.DECADES
            || unit == ChronoUnit.CENTURIES || unit == ChronoUnit.MILLENNIA;
    }

    public Temporal with(TemporalField field, long newValue) {
        if (field == ChronoField.YEAR) {
            return new Year((int) newValue);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
    }

    public Temporal plus(long amountToAdd, TemporalUnit unit) {
        if (unit == ChronoUnit.YEARS) {
            return this.plusYears(amountToAdd);
        }
        if (unit == ChronoUnit.DECADES) {
            return this.plusYears(amountToAdd * 10L);
        }
        if (unit == ChronoUnit.CENTURIES) {
            return this.plusYears(amountToAdd * 100L);
        }
        if (unit == ChronoUnit.MILLENNIA) {
            return this.plusYears(amountToAdd * 1000L);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    public Temporal minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        Year end = (Year) endExclusive;
        long yearsDiff = end.year - this.year;
        if (unit == ChronoUnit.YEARS) {
            return yearsDiff;
        }
        if (unit == ChronoUnit.DECADES) {
            return yearsDiff / 10L;
        }
        if (unit == ChronoUnit.CENTURIES) {
            return yearsDiff / 100L;
        }
        if (unit == ChronoUnit.MILLENNIA) {
            return yearsDiff / 1000L;
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.YEAR, this.year);
    }

    public String toString() {
        return Integer.toString(this.year);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Year) {
            return this.year == ((Year) obj).year;
        }
        return false;
    }

    public int hashCode() {
        return this.year;
    }

    // --- generic Temporal-typed conveniences ---

    public int get(TemporalField field) {
        return (int) this.getLong(field);
    }

    public Year plus(TemporalAmount amount) {
        return (Year) amount.addTo(this);
    }

    public Year minus(TemporalAmount amount) {
        return (Year) amount.subtractFrom(this);
    }

    public Year with(TemporalAdjuster adjuster) {
        return (Year) adjuster.adjustInto(this);
    }

    public static Year parse(CharSequence text) {
        String s = text.toString();
        int i = 0;
        int sign = 1;
        char c0 = s.charAt(0);
        if (c0 == '+') {
            i = 1;
        } else if (c0 == '-') {
            sign = -1;
            i = 1;
        }
        int year = 0;
        for (int k = i; k < s.length(); k = k + 1) {
            year = year * 10 + (s.charAt(k) - '0');
        }
        return Year.of(sign * year);
    }

    /**
     * It reads `text` with that formatter.
     *
     * <p>The one that decides which fields are there is the formatter; this class only says
     * **which of them it wants**, by passing its own `from`. That is why a pattern that brings no year
     * fails here and not when the result is used.
     *
     * @throws java.time.format.DateTimeParseException if the text does not fit the pattern, or what
     *     fits is not enough for a year
     */
    public static Year parse(CharSequence text, java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        // Bound to a local: chaining through an interface-typed intermediate gets lost (#108).
        java.time.temporal.TemporalQuery<Year> queryOf = Year::from;
        return formatter.parse(text, queryOf);
    }
}
