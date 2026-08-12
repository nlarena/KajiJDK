package java.time;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

// KajiLibrary's java.time.YearMonth — a year-and-month, e.g. 2026-08, without a day. Immutable.
// Implements Temporal, TemporalAdjuster and Comparable. A KajiLibrary subset.
public final class YearMonth implements Temporal, TemporalAdjuster, Comparable<YearMonth> {

    private final int year;
    private final int month;

    private YearMonth(int year, int month) {
        this.year = year;
        this.month = month;
    }

    public static YearMonth of(int year, int month) {
        return new YearMonth(year, month);
    }

    public static YearMonth now() {
        LocalDate today = LocalDate.now();
        return new YearMonth(today.getYear(), today.getMonthValue());
    }

    public int getYear() {
        return this.year;
    }

    public int getMonthValue() {
        return this.month;
    }

    public Month getMonth() {
        return Month.of(this.month);
    }

    public boolean isLeapYear() {
        return Year.isLeap(this.year);
    }

    public int lengthOfMonth() {
        return this.getMonth().length(this.isLeapYear());
    }

    public int lengthOfYear() {
        return this.isLeapYear() ? 366 : 365;
    }

    public YearMonth plusMonths(long monthsToAdd) {
        long total = this.year * 12L + (this.month - 1) + monthsToAdd;
        int newYear = (int) YearMonth.floorDiv(total, 12);
        int newMonth = (int) YearMonth.floorMod(total, 12) + 1;
        return new YearMonth(newYear, newMonth);
    }

    public YearMonth plusYears(long yearsToAdd) {
        return new YearMonth((int) (this.year + yearsToAdd), this.month);
    }

    public YearMonth minusMonths(long monthsToSubtract) {
        return this.plusMonths(-monthsToSubtract);
    }

    public YearMonth minusYears(long yearsToSubtract) {
        return this.plusYears(-yearsToSubtract);
    }

    public LocalDate atDay(int dayOfMonth) {
        return LocalDate.of(this.year, this.month, dayOfMonth);
    }

    public LocalDate atEndOfMonth() {
        return LocalDate.of(this.year, this.month, this.lengthOfMonth());
    }

    public boolean isBefore(YearMonth other) {
        return this.compareTo(other) < 0;
    }

    public boolean isAfter(YearMonth other) {
        return this.compareTo(other) > 0;
    }

    public int compareTo(YearMonth other) {
        if (this.year != other.year) {
            return this.year - other.year;
        }
        return this.month - other.month;
    }

    // --- Temporal ---

    public boolean isSupported(TemporalField field) {
        return field == ChronoField.YEAR || field == ChronoField.MONTH_OF_YEAR;
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.YEAR) {
            return this.year;
        }
        if (field == ChronoField.MONTH_OF_YEAR) {
            return this.month;
        }
        throw new IllegalArgumentException();
    }

    public boolean isSupported(TemporalUnit unit) {
        return unit == ChronoUnit.MONTHS || unit == ChronoUnit.YEARS
            || unit == ChronoUnit.DECADES || unit == ChronoUnit.CENTURIES;
    }

    public Temporal with(TemporalField field, long newValue) {
        if (field == ChronoField.YEAR) {
            return new YearMonth((int) newValue, this.month);
        }
        if (field == ChronoField.MONTH_OF_YEAR) {
            return new YearMonth(this.year, (int) newValue);
        }
        throw new IllegalArgumentException();
    }

    public Temporal plus(long amountToAdd, TemporalUnit unit) {
        if (unit == ChronoUnit.MONTHS) {
            return this.plusMonths(amountToAdd);
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
        throw new IllegalArgumentException();
    }

    public Temporal minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        YearMonth end = (YearMonth) endExclusive;
        long monthsDiff = (end.year * 12L + (end.month - 1)) - (this.year * 12L + (this.month - 1));
        if (unit == ChronoUnit.MONTHS) {
            return monthsDiff;
        }
        if (unit == ChronoUnit.YEARS) {
            return monthsDiff / 12L;
        }
        throw new IllegalArgumentException();
    }

    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.MONTH_OF_YEAR, this.month).with(ChronoField.YEAR, this.year);
    }

    private static long floorDiv(long a, long b) {
        long q = a / b;
        if ((a % b != 0) && ((a ^ b) < 0)) {
            q = q - 1;
        }
        return q;
    }

    private static long floorMod(long a, long b) {
        return a - YearMonth.floorDiv(a, b) * b;
    }

    // ISO-8601: uuuu-MM (year padded to 4 digits; no '+' prefix past 9999, unlike LocalDate)
    public String toString() {
        StringBuilder buf = new StringBuilder();
        int absYear;
        if (this.year < 0) {
            absYear = -this.year;
        } else {
            absYear = this.year;
        }
        if (absYear < 1000) {
            if (this.year < 0) {
                String t = Integer.toString(this.year - 10000);
                buf.append("-");
                buf.append(t.substring(2, t.length()));
            } else {
                String t = Integer.toString(this.year + 10000);
                buf.append(t.substring(1, t.length()));
            }
        } else {
            buf.append(Integer.toString(this.year));
        }
        if (this.month < 10) {
            buf.append("-0");
        } else {
            buf.append("-");
        }
        buf.append(Integer.toString(this.month));
        return buf.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof YearMonth) {
            YearMonth o = (YearMonth) obj;
            return this.year == o.year && this.month == o.month;
        }
        return false;
    }

    public int hashCode() {
        return this.year ^ (this.month << 27);
    }

    // --- generic Temporal-typed conveniences ---

    public int get(TemporalField field) {
        return (int) this.getLong(field);
    }

    public YearMonth plus(TemporalAmount amount) {
        return (YearMonth) amount.addTo(this);
    }

    public YearMonth minus(TemporalAmount amount) {
        return (YearMonth) amount.subtractFrom(this);
    }

    public YearMonth with(TemporalAdjuster adjuster) {
        return (YearMonth) adjuster.adjustInto(this);
    }

    public static YearMonth parse(CharSequence text) {
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
        int yStart = i;
        while (i < s.length() && s.charAt(i) != '-') {
            i = i + 1;
        }
        int year = sign * parseDigits(s, yStart, i);
        i = i + 1;
        int month = parseDigits(s, i, i + 2);
        return YearMonth.of(year, month);
    }

    private static int parseDigits(String s, int from, int to) {
        int v = 0;
        for (int k = from; k < to; k = k + 1) {
            v = v * 10 + (s.charAt(k) - '0');
        }
        return v;
    }
}
