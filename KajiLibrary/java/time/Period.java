package java.time;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoUnit;

// KajiLibrary's java.time.Period — a date-based amount of years, months and days (each independent,
// unlike Duration's normalised seconds). Immutable. Implements TemporalAmount so `date.plus(period)`
// works. A KajiLibrary subset (the JDK adds between/parse/toString/multipliedBy).
public final class Period implements TemporalAmount, java.time.chrono.ChronoPeriod, Serializable {

    private final int years;
    private final int months;
    private final int days;

    private Period(int years, int months, int days) {
        this.years = years;
        this.months = months;
        this.days = days;
    }

    /** The period of zero length. */
    public static final Period ZERO = new Period(0, 0, 0);

    /**
     * The period between two dates, in years, months and days.
     *
     * <p>**The three fields are independent and that is the point of the class.** Between the 31st of
     * January and the 1st of March there is "1 month and 1 day", not a number of days: how many days
     * it is depends on whether the year is a leap one. That is why a `Period` cannot be converted to
     * a `Duration` without a reference date, and why both classes exist.
     *
     * <p>The computation takes the whole months first and then the days left over, which is what
     * makes `start.plus(between(start, end)).equals(end)` hold. Done the other way round --days
     * first-- that equality breaks in the months of different length.
     */
    public static Period between(LocalDate startDateInclusive, LocalDate endDateExclusive) {
        if (startDateInclusive == null || endDateExclusive == null) {
            throw new NullPointerException();
        }
        long totalMonths = (long) endDateExclusive.getYear() * 12L
                + (endDateExclusive.getMonthValue() - 1)
                - ((long) startDateInclusive.getYear() * 12L
                        + (startDateInclusive.getMonthValue() - 1));
        int days = endDateExclusive.getDayOfMonth() - startDateInclusive.getDayOfMonth();
        if (totalMonths > 0 && days < 0) {
            // The target's day of the month fell earlier: the last month was not completed. One is
            // taken off and the days are counted from the date already advanced by those months.
            totalMonths = totalMonths - 1;
            LocalDate advanced = startDateInclusive.plusMonths(totalMonths);
            days = (int) (endDateExclusive.toEpochDay() - advanced.toEpochDay());
        } else if (totalMonths < 0 && days > 0) {
            totalMonths = totalMonths + 1;
            days = days - endDateExclusive.lengthOfMonth();
        }
        return Period.of((int) (totalMonths / 12L), (int) (totalMonths % 12L), days);
    }

    /**
     * The period equivalent to `amount`.
     *
     * @throws java.time.DateTimeException if `amount` uses units other than years, months or days
     */
    public static Period from(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        if (amount instanceof Period) {
            return (Period) amount;
        }
        int y = 0;
        int m = 0;
        int d = 0;
        List<TemporalUnit> units = amount.getUnits();
        int i = 0;
        while (i < units.size()) {
            TemporalUnit u = units.get(i);
            long v = amount.get(u);
            if (u == ChronoUnit.YEARS) {
                y = (int) v;
            } else if (u == ChronoUnit.MONTHS) {
                m = (int) v;
            } else if (u == ChronoUnit.DAYS) {
                d = (int) v;
            } else if (v != 0L) {
                throw new java.time.DateTimeException("Unit must be Years, Months or Days, but was " + u);
            }
            i = i + 1;
        }
        return Period.of(y, m, d);
    }

    public static Period of(int years, int months, int days) {
        return new Period(years, months, days);
    }

    public static Period ofYears(int years) {
        return new Period(years, 0, 0);
    }

    public static Period ofMonths(int months) {
        return new Period(0, months, 0);
    }

    public static Period ofWeeks(int weeks) {
        return new Period(0, 0, weeks * 7);
    }

    public static Period ofDays(int days) {
        return new Period(0, 0, days);
    }

    public int getYears() {
        return this.years;
    }

    public int getMonths() {
        return this.months;
    }

    public int getDays() {
        return this.days;
    }

    public boolean isZero() {
        return this.years == 0 && this.months == 0 && this.days == 0;
    }

    public boolean isNegative() {
        return this.years < 0 || this.months < 0 || this.days < 0;
    }

    public Period plusYears(long yearsToAdd) {
        return new Period((int) (this.years + yearsToAdd), this.months, this.days);
    }

    public Period plusMonths(long monthsToAdd) {
        return new Period(this.years, (int) (this.months + monthsToAdd), this.days);
    }

    public Period plusDays(long daysToAdd) {
        return new Period(this.years, this.months, (int) (this.days + daysToAdd));
    }

    public Period minusYears(long yearsToSubtract) {
        return this.plusYears(-yearsToSubtract);
    }

    public Period minusMonths(long monthsToSubtract) {
        return this.plusMonths(-monthsToSubtract);
    }

    public Period minusDays(long daysToSubtract) {
        return this.plusDays(-daysToSubtract);
    }

    // Roll excess months into years (13 months → 1 year, 1 month); days are left alone.
    public Period normalized() {
        long totalMonths = this.years * 12L + this.months;
        return new Period((int) (totalMonths / 12), (int) (totalMonths % 12), this.days);
    }

    /** This period's calendar: ISO, the only one `Period` models. */
    public java.time.chrono.IsoChronology getChronology() {
        return java.time.chrono.IsoChronology.INSTANCE;
    }

    /** This period with other years, leaving months and days as they are. */
    public Period withYears(int years) {
        return years == this.years ? this : Period.of(years, this.months, this.days);
    }

    public Period withMonths(int months) {
        return months == this.months ? this : Period.of(this.years, months, this.days);
    }

    public Period withDays(int days) {
        return days == this.days ? this : Period.of(this.years, this.months, days);
    }

    /**
     * This period plus `amountToAdd`, **field by field**.
     *
     * <p>Years are added to years and days to days: there is no conversion between units, because
     * none exists. Adding "1 month" to "30 days" gives "1 month and 30 days", not "60 days".
     *
     * @throws java.time.DateTimeException if `amountToAdd` uses other units
     */
    public Period plus(TemporalAmount amountToAdd) {
        Period other = Period.from(amountToAdd);
        return Period.of(this.years + other.years, this.months + other.months, this.days + other.days);
    }

    public Period minus(TemporalAmount amountToSubtract) {
        Period other = Period.from(amountToSubtract);
        return Period.of(this.years - other.years, this.months - other.months, this.days - other.days);
    }

    /** Each field multiplied by `scalar`. */
    public Period multipliedBy(int scalar) {
        if (scalar == 1 || this.isZero()) {
            return this;
        }
        return Period.of(this.years * scalar, this.months * scalar, this.days * scalar);
    }

    /** Each field with its sign flipped. */
    public Period negated() {
        return this.multipliedBy(-1);
    }

    public long toTotalMonths() {
        return this.years * 12L + this.months;
    }

    // --- TemporalAmount ---

    public long get(TemporalUnit unit) {
        if (unit == ChronoUnit.YEARS) {
            return this.years;
        }
        if (unit == ChronoUnit.MONTHS) {
            return this.months;
        }
        if (unit == ChronoUnit.DAYS) {
            return this.days;
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    public List<TemporalUnit> getUnits() {
        List<TemporalUnit> units = new ArrayList<TemporalUnit>();
        units.add(ChronoUnit.YEARS);
        units.add(ChronoUnit.MONTHS);
        units.add(ChronoUnit.DAYS);
        return units;
    }

    public Temporal addTo(Temporal temporal) {
        Temporal result = temporal;
        long totalMonths = this.toTotalMonths();
        if (totalMonths != 0) {
            result = result.plus(totalMonths, ChronoUnit.MONTHS);
        }
        if (this.days != 0) {
            result = result.plus(this.days, ChronoUnit.DAYS);
        }
        return result;
    }

    public Temporal subtractFrom(Temporal temporal) {
        Temporal result = temporal;
        long totalMonths = this.toTotalMonths();
        if (totalMonths != 0) {
            result = result.minus(totalMonths, ChronoUnit.MONTHS);
        }
        if (this.days != 0) {
            result = result.minus(this.days, ChronoUnit.DAYS);
        }
        return result;
    }

    // ISO-8601: PnYnMnD (P0D for zero); omits zero components.
    public String toString() {
        if (this.years == 0 && this.months == 0 && this.days == 0) {
            return "P0D";
        }
        StringBuilder buf = new StringBuilder("P");
        if (this.years != 0) {
            buf.append(Integer.toString(this.years));
            buf.append("Y");
        }
        if (this.months != 0) {
            buf.append(Integer.toString(this.months));
            buf.append("M");
        }
        if (this.days != 0) {
            buf.append(Integer.toString(this.days));
            buf.append("D");
        }
        return buf.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Period) {
            Period o = (Period) obj;
            return this.years == o.years && this.months == o.months && this.days == o.days;
        }
        return false;
    }

    public int hashCode() {
        return this.years + ((this.months << 8) | (this.months >>> 24)) + ((this.days << 16) | (this.days >>> 16));
    }

    // Parses P[nY][nM][nW][nD] (each component optionally signed; weeks fold into days).
    public static Period parse(CharSequence text) {
        String s = text.toString();
        int i = 1;
        int years = 0;
        int months = 0;
        int days = 0;
        while (i < s.length()) {
            int sign = 1;
            if (s.charAt(i) == '-') {
                sign = -1;
                i = i + 1;
            } else if (s.charAt(i) == '+') {
                i = i + 1;
            }
            int nStart = i;
            while (i < s.length() && isDigit(s.charAt(i))) {
                i = i + 1;
            }
            int val = sign * parseDigits(s, nStart, i);
            char u = s.charAt(i);
            i = i + 1;
            if (u == 'Y') {
                years = val;
            } else if (u == 'M') {
                months = val;
            } else if (u == 'W') {
                days = days + val * 7;
            } else if (u == 'D') {
                days = days + val;
            }
        }
        return Period.of(years, months, days);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static int parseDigits(String s, int from, int to) {
        int v = 0;
        for (int k = from; k < to; k = k + 1) {
            v = v * 10 + (s.charAt(k) - '0');
        }
        return v;
    }
}
