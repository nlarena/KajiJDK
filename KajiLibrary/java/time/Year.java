package java.time;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

// KajiLibrary's java.time.Year — a year on the ISO calendar, e.g. 2026. Immutable value type.
// Implements Temporal, TemporalAdjuster and Comparable. A KajiLibrary subset.
public final class Year implements Temporal, TemporalAdjuster, Comparable<Year> {

    private final int year;

    private Year(int year) {
        this.year = year;
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
        return field == ChronoField.YEAR;
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.YEAR) {
            return this.year;
        }
        throw new IllegalArgumentException();
    }

    public boolean isSupported(TemporalUnit unit) {
        return unit == ChronoUnit.YEARS || unit == ChronoUnit.DECADES
            || unit == ChronoUnit.CENTURIES || unit == ChronoUnit.MILLENNIA;
    }

    public Temporal with(TemporalField field, long newValue) {
        if (field == ChronoField.YEAR) {
            return new Year((int) newValue);
        }
        throw new IllegalArgumentException();
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
        throw new IllegalArgumentException();
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
        throw new IllegalArgumentException();
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
}
