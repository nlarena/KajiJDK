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

    /** El año y mes que `temporal` tiene. */
    public static YearMonth from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof YearMonth) {
            return (YearMonth) temporal;
        }
        return YearMonth.of(temporal.get(ChronoField.YEAR),
                temporal.get(ChronoField.MONTH_OF_YEAR));
    }

    /** El que marca `clock`. La forma testeable de `now()`. */
    public static YearMonth now(java.time.Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        LocalDate d = LocalDate.now(clock);
        return YearMonth.of(d.getYear(), d.getMonthValue());
    }

    /** El de esa zona, ahora. */
    public static YearMonth now(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        LocalDate d = LocalDate.now(zone);
        return YearMonth.of(d.getYear(), d.getMonthValue());
    }

    /** Con el mes como enum. */
    public static YearMonth of(int year, Month month) {
        if (month == null) {
            throw new NullPointerException("month");
        }
        return YearMonth.of(year, month.getValue());
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

    /**
     * Si ese dia del mes existe en este año y mes.
     *
     * <p>Distingue el 29 de febrero de un año bisiesto del de uno comun, que es para lo que sirve.
     */
    public boolean isValidDay(int dayOfMonth) {
        return dayOfMonth >= 1 && dayOfMonth <= this.lengthOfMonth();
    }

    /** Este año y mes con otro año; el mes queda igual. */
    public YearMonth withYear(int year) {
        ChronoField.YEAR.checkValidValue((long) year);
        return YearMonth.of(year, this.getMonthValue());
    }

    /** Con otro mes; el año queda igual. */
    public YearMonth withMonth(int month) {
        ChronoField.MONTH_OF_YEAR.checkValidValue((long) month);
        return YearMonth.of(this.getYear(), month);
    }

    /** Formateado. */
    public String format(java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        return formatter.format(this);
    }

    /**
     * Este año y mes mas `amountToAdd` unidades.
     *
     * @throws java.time.DateTimeException si la unidad no es de mes o de año
     */
    public YearMonth plus(long amountToAdd, java.time.temporal.TemporalUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
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
        if (unit == ChronoUnit.MILLENNIA) {
            return this.plusYears(amountToAdd * 1000L);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    public YearMonth minus(long amountToSubtract, java.time.temporal.TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    /** Con `field` puesto en `newValue`. */
    public YearMonth with(java.time.temporal.TemporalField field, long newValue) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (field == ChronoField.YEAR) {
            return this.withYear((int) ChronoField.YEAR.checkValidValue(newValue));
        }
        if (field == ChronoField.MONTH_OF_YEAR) {
            return this.withMonth((int) ChronoField.MONTH_OF_YEAR.checkValidValue(newValue));
        }
        if (field == ChronoField.PROLEPTIC_MONTH) {
            long delta = newValue - (this.getYear() * 12L + this.getMonthValue() - 1);
            return this.plusMonths(delta);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
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
