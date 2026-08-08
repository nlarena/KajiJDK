package java.time;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;

// KajiLibrary's java.time.LocalDateTime — a date-and-time without a zone, composed of a LocalDate
// and a LocalTime. Immutable. Time arithmetic that crosses midnight carries into the date. A
// KajiLibrary subset (toString/parse and some conversions deferred).
public final class LocalDateTime implements Temporal, TemporalAdjuster, Comparable<LocalDateTime> {

    private static final long NANOS_PER_SECOND = 1000000000L;
    private static final long NANOS_PER_DAY = 86400L * NANOS_PER_SECOND;

    private final LocalDate date;
    private final LocalTime time;

    private LocalDateTime(LocalDate date, LocalTime time) {
        this.date = date;
        this.time = time;
    }

    public static LocalDateTime of(LocalDate date, LocalTime time) {
        return new LocalDateTime(date, time);
    }

    public static LocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute) {
        return new LocalDateTime(LocalDate.of(year, month, dayOfMonth), LocalTime.of(hour, minute));
    }

    public static LocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute, int second) {
        return new LocalDateTime(LocalDate.of(year, month, dayOfMonth), LocalTime.of(hour, minute, second));
    }

    public static LocalDateTime now() {
        long millis = System.currentTimeMillis();
        return new LocalDateTime(LocalDate.ofEpochDay(millis / 86400000L),
            LocalTime.ofNanoOfDay((millis % 86400000L) * 1000000L));
    }

    public LocalDate toLocalDate() {
        return this.date;
    }

    public LocalTime toLocalTime() {
        return this.time;
    }

    public int getYear() {
        return this.date.getYear();
    }

    public int getMonthValue() {
        return this.date.getMonthValue();
    }

    public Month getMonth() {
        return this.date.getMonth();
    }

    public int getDayOfMonth() {
        return this.date.getDayOfMonth();
    }

    public DayOfWeek getDayOfWeek() {
        return this.date.getDayOfWeek();
    }

    public int getHour() {
        return this.time.getHour();
    }

    public int getMinute() {
        return this.time.getMinute();
    }

    public int getSecond() {
        return this.time.getSecond();
    }

    public int getNano() {
        return this.time.getNano();
    }

    // --- date arithmetic (delegates to the date, keeps the time) ---

    public LocalDateTime plusDays(long days) {
        return new LocalDateTime(this.date.plusDays(days), this.time);
    }

    public LocalDateTime plusWeeks(long weeks) {
        return new LocalDateTime(this.date.plusWeeks(weeks), this.time);
    }

    public LocalDateTime plusMonths(long months) {
        return new LocalDateTime(this.date.plusMonths(months), this.time);
    }

    public LocalDateTime plusYears(long years) {
        return new LocalDateTime(this.date.plusYears(years), this.time);
    }

    public LocalDateTime minusDays(long days) {
        return this.plusDays(-days);
    }

    public LocalDateTime minusMonths(long months) {
        return this.plusMonths(-months);
    }

    public LocalDateTime minusYears(long years) {
        return this.plusYears(-years);
    }

    // --- time arithmetic (carries whole days into the date) ---

    private LocalDateTime plusNanosCarry(long nanosToAdd) {
        long newNanoOfDay = this.time.toNanoOfDay() + nanosToAdd;
        long dayCarry = LocalDateTime.floorDiv(newNanoOfDay, NANOS_PER_DAY);
        long nanoOfDay = LocalDateTime.floorMod(newNanoOfDay, NANOS_PER_DAY);
        return new LocalDateTime(this.date.plusDays(dayCarry), LocalTime.ofNanoOfDay(nanoOfDay));
    }

    public LocalDateTime plusHours(long hours) {
        return this.plusNanosCarry(hours * 3600000000000L);
    }

    public LocalDateTime plusMinutes(long minutes) {
        return this.plusNanosCarry(minutes * 60000000000L);
    }

    public LocalDateTime plusSeconds(long seconds) {
        return this.plusNanosCarry(seconds * NANOS_PER_SECOND);
    }

    public LocalDateTime plusNanos(long nanos) {
        return this.plusNanosCarry(nanos);
    }

    public LocalDateTime minusHours(long hours) {
        return this.plusNanosCarry(-hours * 3600000000000L);
    }

    public LocalDateTime minusMinutes(long minutes) {
        return this.plusNanosCarry(-minutes * 60000000000L);
    }

    public LocalDateTime minusSeconds(long seconds) {
        return this.plusNanosCarry(-seconds * NANOS_PER_SECOND);
    }

    // --- comparison ---

    public int compareTo(LocalDateTime other) {
        int cmp = this.date.compareTo(other.date);
        if (cmp != 0) {
            return cmp;
        }
        return this.time.compareTo(other.time);
    }

    public boolean isBefore(LocalDateTime other) {
        return this.compareTo(other) < 0;
    }

    public boolean isAfter(LocalDateTime other) {
        return this.compareTo(other) > 0;
    }

    // --- Temporal (route field/unit ops to the date or the time part) ---

    public boolean isSupported(TemporalField field) {
        return this.time.isSupported(field) || this.date.isSupported(field);
    }

    public long getLong(TemporalField field) {
        if (this.time.isSupported(field)) {
            return this.time.getLong(field);
        }
        return this.date.getLong(field);
    }

    public boolean isSupported(TemporalUnit unit) {
        return this.time.isSupported(unit) || this.date.isSupported(unit);
    }

    public Temporal with(TemporalField field, long newValue) {
        if (this.time.isSupported(field)) {
            return new LocalDateTime(this.date, (LocalTime) this.time.with(field, newValue));
        }
        return new LocalDateTime((LocalDate) this.date.with(field, newValue), this.time);
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
        if (unit == ChronoUnit.HOURS) {
            return this.plusHours(amountToAdd);
        }
        if (unit == ChronoUnit.MINUTES) {
            return this.plusMinutes(amountToAdd);
        }
        if (unit == ChronoUnit.SECONDS) {
            return this.plusSeconds(amountToAdd);
        }
        if (unit == ChronoUnit.NANOS) {
            return this.plusNanos(amountToAdd);
        }
        throw new IllegalArgumentException();
    }

    public Temporal minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        LocalDateTime end = (LocalDateTime) endExclusive;
        if (unit == ChronoUnit.DAYS) {
            return this.date.until(end.date, ChronoUnit.DAYS);
        }
        long totalNanos = (end.date.toEpochDay() - this.date.toEpochDay()) * NANOS_PER_DAY
            + (end.time.toNanoOfDay() - this.time.toNanoOfDay());
        if (unit == ChronoUnit.NANOS) {
            return totalNanos;
        }
        if (unit == ChronoUnit.SECONDS) {
            return totalNanos / NANOS_PER_SECOND;
        }
        throw new IllegalArgumentException();
    }

    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.EPOCH_DAY, this.date.toEpochDay());
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
        return a - LocalDateTime.floorDiv(a, b) * b;
    }

    // --- value-type methods (ISO-8601: <date>T<time>) ---

    public String toString() {
        return this.date.toString() + "T" + this.time.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LocalDateTime) {
            LocalDateTime o = (LocalDateTime) obj;
            return this.date.equals(o.date) && this.time.equals(o.time);
        }
        return false;
    }

    public int hashCode() {
        return this.date.hashCode() ^ this.time.hashCode();
    }

    // --- generic Temporal-typed conveniences ---

    public int get(TemporalField field) {
        return (int) this.getLong(field);
    }

    public LocalDateTime plus(TemporalAmount amount) {
        return (LocalDateTime) amount.addTo(this);
    }

    public LocalDateTime minus(TemporalAmount amount) {
        return (LocalDateTime) amount.subtractFrom(this);
    }

    public LocalDateTime with(TemporalAdjuster adjuster) {
        return (LocalDateTime) adjuster.adjustInto(this);
    }

    // Parses an ISO-8601 date-time (<date>T<time>), delegating to LocalDate/LocalTime.
    public static LocalDateTime parse(CharSequence text) {
        String s = text.toString();
        int t = -1;
        for (int i = 0; i < s.length(); i = i + 1) {
            if (s.charAt(i) == 'T') {
                t = i;
                break;
            }
        }
        LocalDate d = LocalDate.parse(s.substring(0, t));
        LocalTime tm = LocalTime.parse(s.substring(t + 1, s.length()));
        return LocalDateTime.of(d, tm);
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }
}
