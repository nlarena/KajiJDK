package java.time;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.IsoChronology;

// KajiLibrary's java.time.LocalDate — a date without time or zone, on the ISO-8601 calendar
// (proleptic Gregorian). Immutable value type. The heart is the epoch-day ↔ (year, month, day)
// conversion (toEpochDay / ofEpochDay), the classic java.time algorithm — everything else is
// layered on it. Implements Temporal, TemporalAdjuster and Comparable. A KajiLibrary subset
// (toString/parse, more fields/units, from(TemporalAccessor) deferred).
// NO declara `Comparable<LocalDate>`: hereda `Comparable<ChronoLocalDate>` de `ChronoLocalDate`
// (#276), y una clase no puede implementar dos parametrizaciones de la misma interfaz. Es tambien
// lo que hace el JDK, y por la misma razon.
public final class LocalDate implements Temporal, TemporalAdjuster, ChronoLocalDate {

    private static final long DAYS_0000_TO_1970 = 719528L;

    private final int year;
    private final int month;
    private final int day;

    private LocalDate(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public static LocalDate of(int year, int month, int day) {
        return new LocalDate(year, month, day);
    }

    public static LocalDate now() {
        return LocalDate.ofEpochDay(System.currentTimeMillis() / 86400000L);
    }

    // Private helper: the JDK exposes leap-year testing on Year/IsoChronology, not as a static on
    // LocalDate (LocalDate only has the instance isLeapYear() below).
    private static boolean isLeapYear(long year) {
        return ((year & 3L) == 0) && ((year % 100L != 0) || (year % 400L == 0));
    }

    // --- the epoch-day conversion (the algorithmic centrepiece) ---

    public long toEpochDay() {
        long y = this.year;
        long m = this.month;
        long total = 0;
        total = total + 365L * y;
        if (y >= 0) {
            total = total + (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400;
        } else {
            total = total - (y / -4 - y / -100 + y / -400);
        }
        total = total + (367L * m - 362L) / 12L;
        total = total + (this.day - 1);
        if (m > 2) {
            total = total - 1;
            if (!LocalDate.isLeapYear(this.year)) {
                total = total - 1;
            }
        }
        return total - DAYS_0000_TO_1970;
    }

    public static LocalDate ofEpochDay(long epochDay) {
        long zeroDay = epochDay + DAYS_0000_TO_1970;
        zeroDay = zeroDay - 60;
        long adjust = 0;
        if (zeroDay < 0) {
            long adjustCycles = (zeroDay + 1) / 146097 - 1;
            adjust = adjustCycles * 400;
            zeroDay = zeroDay + (-adjustCycles * 146097);
        }
        long yearEst = (400 * zeroDay + 591) / 146097;
        long doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        if (doyEst < 0) {
            yearEst = yearEst - 1;
            doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        }
        yearEst = yearEst + adjust;
        int marchDoy0 = (int) doyEst;
        int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
        int month = (marchMonth0 + 2) % 12 + 1;
        int dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
        yearEst = yearEst + marchMonth0 / 10;
        return new LocalDate((int) yearEst, month, dom);
    }

    // --- accessors ---

    public int getYear() {
        return this.year;
    }

    public int getMonthValue() {
        return this.month;
    }

    public int getDayOfMonth() {
        return this.day;
    }

    public Month getMonth() {
        return Month.of(this.month);
    }

    public DayOfWeek getDayOfWeek() {
        long r = (this.toEpochDay() + 3) % 7;
        if (r < 0) {
            r = r + 7;
        }
        return DayOfWeek.of((int) r + 1);
    }

    public int getDayOfYear() {
        return (int) (this.toEpochDay() - LocalDate.of(this.year, 1, 1).toEpochDay()) + 1;
    }

    public boolean isLeapYear() {
        return LocalDate.isLeapYear(this.year);
    }

    public int lengthOfMonth() {
        return Month.of(this.month).length(this.isLeapYear());
    }

    public int lengthOfYear() {
        return this.isLeapYear() ? 366 : 365;
    }

    // --- arithmetic ---

    public LocalDate plusDays(long daysToAdd) {
        return LocalDate.ofEpochDay(this.toEpochDay() + daysToAdd);
    }

    public LocalDate minusDays(long daysToSubtract) {
        return this.plusDays(-daysToSubtract);
    }

    public LocalDate plusWeeks(long weeksToAdd) {
        return this.plusDays(weeksToAdd * 7L);
    }

    public LocalDate minusWeeks(long weeksToSubtract) {
        return this.plusDays(-weeksToSubtract * 7L);
    }

    public LocalDate plusMonths(long monthsToAdd) {
        long monthCount = this.year * 12L + (this.month - 1);
        long calcMonths = monthCount + monthsToAdd;
        int newYear = (int) LocalDate.floorDiv(calcMonths, 12);
        int newMonth = (int) LocalDate.floorMod(calcMonths, 12) + 1;
        int newDay = this.day;
        int monthLen = Month.of(newMonth).length(LocalDate.isLeapYear(newYear));
        if (newDay > monthLen) {
            newDay = monthLen;
        }
        return new LocalDate(newYear, newMonth, newDay);
    }

    public LocalDate minusMonths(long monthsToSubtract) {
        return this.plusMonths(-monthsToSubtract);
    }

    public LocalDate plusYears(long yearsToAdd) {
        return this.plusMonths(yearsToAdd * 12L);
    }

    public LocalDate minusYears(long yearsToSubtract) {
        return this.plusMonths(-yearsToSubtract * 12L);
    }

    // --- comparison ---

    /**
     * El orden natural. La firma toma {@link ChronoLocalDate} y no {@code LocalDate} porque es la
     * que declara la interfaz -- y es la que declara el JDK-: una clase no puede implementar
     * {@code Comparable} dos veces con parametros distintos.
     *
     * <p>Con otro {@code LocalDate} compara por campos, que es mas barato que ir al dia epocal.
     * Con una fecha de otro calendario cae al orden general: dia epocal y, si empatan, el id de la
     * cronologia -- el desempate que evita que dos fechas que no son iguales comparen 0.
     */
    @Override
    public int compareTo(ChronoLocalDate other) {
        if (other instanceof LocalDate) {
            LocalDate that = (LocalDate) other;
            if (this.year != that.year) {
                return this.year - that.year;
            }
            if (this.month != that.month) {
                return this.month - that.month;
            }
            return this.day - that.day;
        }
        long mine = this.toEpochDay();
        long theirs = other.toEpochDay();
        if (mine < theirs) {
            return -1;
        }
        if (mine > theirs) {
            return 1;
        }
        Chronology chrono = this.getChronology();
        Chronology otherChrono = other.getChronology();
        return chrono.getId().compareTo(otherChrono.getId());
    }

    public boolean isBefore(LocalDate other) {
        return this.compareTo(other) < 0;
    }

    public boolean isAfter(LocalDate other) {
        return this.compareTo(other) > 0;
    }

    // --- Temporal ---

    public boolean isSupported(TemporalField field) {
        return field == ChronoField.DAY_OF_MONTH || field == ChronoField.MONTH_OF_YEAR
            || field == ChronoField.YEAR || field == ChronoField.EPOCH_DAY
            || field == ChronoField.DAY_OF_WEEK || field == ChronoField.DAY_OF_YEAR;
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.DAY_OF_MONTH) {
            return this.day;
        }
        if (field == ChronoField.MONTH_OF_YEAR) {
            return this.month;
        }
        if (field == ChronoField.YEAR) {
            return this.year;
        }
        if (field == ChronoField.EPOCH_DAY) {
            return this.toEpochDay();
        }
        if (field == ChronoField.DAY_OF_WEEK) {
            return this.getDayOfWeek().getValue();
        }
        if (field == ChronoField.DAY_OF_YEAR) {
            return this.getDayOfYear();
        }
        throw new IllegalArgumentException();
    }

    public boolean isSupported(TemporalUnit unit) {
        return unit == ChronoUnit.DAYS || unit == ChronoUnit.WEEKS
            || unit == ChronoUnit.MONTHS || unit == ChronoUnit.YEARS;
    }

    public Temporal with(TemporalField field, long newValue) {
        if (field == ChronoField.DAY_OF_MONTH) {
            return new LocalDate(this.year, this.month, (int) newValue);
        }
        if (field == ChronoField.MONTH_OF_YEAR) {
            return new LocalDate(this.year, (int) newValue, this.day);
        }
        if (field == ChronoField.YEAR) {
            return new LocalDate((int) newValue, this.month, this.day);
        }
        if (field == ChronoField.EPOCH_DAY) {
            return LocalDate.ofEpochDay(newValue);
        }
        throw new IllegalArgumentException();
    }

    public Temporal plus(long amountToAdd, TemporalUnit unit) {
        if (unit == ChronoUnit.DAYS) {
            return this.plusDays(amountToAdd);
        }
        if (unit == ChronoUnit.WEEKS) {
            return this.plusWeeks(amountToAdd);
        }
        if (unit == ChronoUnit.MONTHS) {
            return this.plusMonths(amountToAdd);
        }
        if (unit == ChronoUnit.YEARS) {
            return this.plusYears(amountToAdd);
        }
        throw new IllegalArgumentException();
    }

    public Temporal minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        LocalDate end = (LocalDate) endExclusive;
        long daysDiff = end.toEpochDay() - this.toEpochDay();
        if (unit == ChronoUnit.DAYS) {
            return daysDiff;
        }
        if (unit == ChronoUnit.WEEKS) {
            return daysDiff / 7L;
        }
        throw new IllegalArgumentException();
    }

    // --- TemporalAdjuster ---

    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.EPOCH_DAY, this.toEpochDay());
    }

    // --- floor division/modulo (no java.lang.Math.floorDiv/floorMod in KajiLibrary yet) ---

    private static long floorDiv(long a, long b) {
        long q = a / b;
        if ((a % b != 0) && ((a ^ b) < 0)) {
            q = q - 1;
        }
        return q;
    }

    private static long floorMod(long a, long b) {
        return a - LocalDate.floorDiv(a, b) * b;
    }

    // --- value-type methods (ISO-8601) ---

    // ISO-8601: uuuu-MM-dd, with the year padded to at least 4 digits (a '+' prefix past 9999,
    // '-' when negative) — the same layout java.time uses.
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
            if (this.year > 9999) {
                buf.append("+");
            }
            buf.append(Integer.toString(this.year));
        }
        if (this.month < 10) {
            buf.append("-0");
        } else {
            buf.append("-");
        }
        buf.append(Integer.toString(this.month));
        if (this.day < 10) {
            buf.append("-0");
        } else {
            buf.append("-");
        }
        buf.append(Integer.toString(this.day));
        return buf.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LocalDate) {
            LocalDate o = (LocalDate) obj;
            return this.year == o.year && this.month == o.month && this.day == o.day;
        }
        return false;
    }

    public int hashCode() {
        int y = this.year;
        return (y & 0xFFFFF800) ^ ((y << 11) + (this.month << 6) + this.day);
    }

    public LocalDate withYear(int year) {
        return LocalDate.of(year, this.month, this.day);
    }

    public LocalDate withMonth(int month) {
        return LocalDate.of(this.year, month, this.day);
    }

    public LocalDate withDayOfMonth(int dayOfMonth) {
        return LocalDate.of(this.year, this.month, dayOfMonth);
    }

    public LocalDate withDayOfYear(int dayOfYear) {
        return LocalDate.ofEpochDay(LocalDate.of(this.year, 1, 1).toEpochDay() + dayOfYear - 1);
    }

    // --- generic Temporal-typed conveniences ---

    public int get(TemporalField field) {
        return (int) this.getLong(field);
    }

    public LocalDate plus(TemporalAmount amount) {
        return (LocalDate) amount.addTo(this);
    }

    public LocalDate minus(TemporalAmount amount) {
        return (LocalDate) amount.subtractFrom(this);
    }

    public LocalDate with(TemporalAdjuster adjuster) {
        return (LocalDate) adjuster.adjustInto(this);
    }

    // Combines this date with a time to make a LocalDateTime.
    public LocalDateTime atTime(LocalTime time) {
        return LocalDateTime.of(this, time);
    }

    public LocalDateTime atTime(int hour, int minute) {
        return LocalDateTime.of(this, LocalTime.of(hour, minute));
    }

    public LocalDateTime atTime(int hour, int minute, int second) {
        return LocalDateTime.of(this, LocalTime.of(hour, minute, second));
    }

    public LocalDateTime atTime(int hour, int minute, int second, int nanoOfSecond) {
        return LocalDateTime.of(this, LocalTime.of(hour, minute, second, nanoOfSecond));
    }

    public boolean isEqual(LocalDate other) {
        return this.compareTo(other) == 0;
    }

    // The Period between this date (inclusive) and `end` (exclusive), the java.time algorithm:
    // whole months first, then the remaining days, normalising the sign.
    public Period until(LocalDate end) {
        long totalMonths = (end.year * 12L + (end.month - 1)) - (this.year * 12L + (this.month - 1));
        int days = end.day - this.day;
        if (totalMonths > 0 && days < 0) {
            totalMonths = totalMonths - 1;
            LocalDate calcDate = this.plusMonths(totalMonths);
            days = (int) (end.toEpochDay() - calcDate.toEpochDay());
        } else if (totalMonths < 0 && days > 0) {
            totalMonths = totalMonths + 1;
            days = days - end.lengthOfMonth();
        }
        long years = totalMonths / 12;
        int months = (int) (totalMonths % 12);
        return Period.of((int) years, months, days);
    }

    // Parses an ISO-8601 date (uuuu-MM-dd, the year optionally signed and wider than 4 digits).
    public static LocalDate parse(CharSequence text) {
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
        i = i + 3;
        int day = parseDigits(s, i, i + 2);
        return LocalDate.of(year, month, day);
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    public IsoChronology getChronology() {
        return IsoChronology.INSTANCE;
    }

    private static int parseDigits(String s, int from, int to) {
        int v = 0;
        for (int k = from; k < to; k = k + 1) {
            v = v * 10 + (s.charAt(k) - '0');
        }
        return v;
    }
}
