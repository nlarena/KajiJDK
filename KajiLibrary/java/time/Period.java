package java.time;

import java.util.List;
import java.util.ArrayList;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoUnit;

// KajiLibrary's java.time.Period — a date-based amount of years, months and days (each independent,
// unlike Duration's normalised seconds). Immutable. Implements TemporalAmount so `date.plus(period)`
// works. A KajiLibrary subset (the JDK adds between/parse/toString/multipliedBy).
public final class Period implements TemporalAmount {

    private final int years;
    private final int months;
    private final int days;

    private Period(int years, int months, int days) {
        this.years = years;
        this.months = months;
        this.days = days;
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
        throw new IllegalArgumentException();
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
