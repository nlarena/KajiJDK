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
public final class LocalDateTime implements Temporal, TemporalAdjuster,
        Comparable<LocalDateTime>, java.time.chrono.ChronoLocalDateTime {

    private static final long NANOS_PER_SECOND = 1000000000L;
    private static final long NANOS_PER_DAY = 86400L * NANOS_PER_SECOND;

    private final LocalDate date;
    private final LocalTime time;

    private LocalDateTime(LocalDate date, LocalTime time) {
        this.date = date;
        this.time = time;
    }

    /** La fecha y hora mas temprana representable. */
    public static final LocalDateTime MIN = LocalDateTime.of(LocalDate.MIN, LocalTime.MIN);

    /** La mas tardia. */
    public static final LocalDateTime MAX = LocalDateTime.of(LocalDate.MAX, LocalTime.MAX);

    /** La fecha y hora que `temporal` tiene. */
    public static LocalDateTime from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof LocalDateTime) {
            return (LocalDateTime) temporal;
        }
        return LocalDateTime.of(LocalDate.from(temporal), LocalTime.from(temporal));
    }

    /** La que marca `clock`. La forma testeable de `now()`. */
    public static LocalDateTime now(java.time.Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        return LocalDateTime.ofInstant(clock.instant(), clock.getZone());
    }

    /** La de esa zona, ahora. */
    public static LocalDateTime now(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return LocalDateTime.ofInstant(Instant.now(), zone);
    }

    /**
     * La fecha y hora local de ese instante en esa zona.
     *
     * <p>Pierde el desplazamiento a proposito: un `LocalDateTime` es "las 15:30 del martes" sin
     * decir dónde, y por eso dos instantes distintos pueden dar el mismo -- uno de cada lado del
     * cambio de horario de verano.
     */
    public static LocalDateTime ofInstant(Instant instant, ZoneId zone) {
        if (instant == null || zone == null) {
            throw new NullPointerException();
        }
        ZoneOffset offset = zone.getRules().getOffset(instant);
        return LocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), offset);
    }

    /**
     * La fecha y hora local de ese segundo de epoca con ese desplazamiento.
     *
     * @throws java.time.DateTimeException si `nanoOfSecond` cae fuera de [0, 999999999]
     */
    public static LocalDateTime ofEpochSecond(long epochSecond, int nanoOfSecond, ZoneOffset offset) {
        if (offset == null) {
            throw new NullPointerException("offset");
        }
        ChronoField.NANO_OF_SECOND.checkValidValue((long) nanoOfSecond);
        long segsLocales = epochSecond + offset.getTotalSeconds();
        long dia = Math.floorDiv(segsLocales, 86400L);
        int segsDelDia = (int) Math.floorMod(segsLocales, 86400L);
        return LocalDateTime.of(LocalDate.ofEpochDay(dia),
                LocalTime.of(segsDelDia / 3600, (segsDelDia / 60) % 60, segsDelDia % 60,
                        nanoOfSecond));
    }

    /** Con el mes como enum. */
    public static LocalDateTime of(int year, Month month, int dayOfMonth, int hour, int minute) {
        if (month == null) {
            throw new NullPointerException("month");
        }
        return LocalDateTime.of(year, month.getValue(), dayOfMonth, hour, minute);
    }

    public static LocalDateTime of(int year, Month month, int dayOfMonth, int hour, int minute,
            int second) {
        if (month == null) {
            throw new NullPointerException("month");
        }
        return LocalDateTime.of(year, month.getValue(), dayOfMonth, hour, minute, second);
    }

    public static LocalDateTime of(int year, Month month, int dayOfMonth, int hour, int minute,
            int second, int nanoOfSecond) {
        if (month == null) {
            throw new NullPointerException("month");
        }
        return LocalDateTime.of(year, month.getValue(), dayOfMonth, hour, minute, second,
                nanoOfSecond);
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

    /**
     * The full form, down to the nanosecond. {@code LocalTime} already had its four-argument
     * {@code of}; what was missing was the bridge, and {@code ZonedDateTime.of(int x7, ZoneId)}
     * -- the only caller -- could not compile without it (#266).
     */
    public static LocalDateTime of(int year, int month, int dayOfMonth, int hour, int minute,
            int second, int nanoOfSecond) {
        return new LocalDateTime(LocalDate.of(year, month, dayOfMonth),
            LocalTime.of(hour, minute, second, nanoOfSecond));
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

    /** El dia del año, de 1 a 365 o 366. */
    public int getDayOfYear() {
        return this.date.getDayOfYear();
    }

    // ---- los `with*`, campo a campo -------------------------------------------------------------
    //
    // Los siete delegan en la mitad que corresponde y rearman el par. Un `LocalDateTime` es
    // exactamente una fecha mas una hora, y esa separacion es lo que hace que no haya nada mas.

    public LocalDateTime withYear(int year) {
        return LocalDateTime.of(this.date.withYear(year), this.time);
    }

    public LocalDateTime withMonth(int month) {
        return LocalDateTime.of(this.date.withMonth(month), this.time);
    }

    public LocalDateTime withDayOfMonth(int dayOfMonth) {
        return LocalDateTime.of(this.date.withDayOfMonth(dayOfMonth), this.time);
    }

    public LocalDateTime withDayOfYear(int dayOfYear) {
        return LocalDateTime.of(LocalDate.ofYearDay(this.date.getYear(), dayOfYear), this.time);
    }

    public LocalDateTime withHour(int hour) {
        return LocalDateTime.of(this.date, this.time.withHour(hour));
    }

    public LocalDateTime withMinute(int minute) {
        return LocalDateTime.of(this.date, this.time.withMinute(minute));
    }

    public LocalDateTime withSecond(int second) {
        return LocalDateTime.of(this.date, this.time.withSecond(second));
    }

    public LocalDateTime withNano(int nanoOfSecond) {
        return LocalDateTime.of(this.date, this.time.withNano(nanoOfSecond));
    }

    /**
     * Truncada a un multiplo de `unit`, contando desde la medianoche.
     *
     * <p>La fecha no se toca: truncar a horas deja el mismo dia con la hora redondeada hacia abajo.
     */
    public LocalDateTime truncatedTo(java.time.temporal.TemporalUnit unit) {
        return LocalDateTime.of(this.date, this.time.truncatedTo(unit));
    }

    public LocalDateTime minusNanos(long nanos) {
        return this.plusNanos(-nanos);
    }

    public LocalDateTime minusWeeks(long weeks) {
        return this.plusWeeks(-weeks);
    }

    /** Esta fecha y hora con ese desplazamiento. */
    public java.time.OffsetDateTime atOffset(ZoneOffset offset) {
        if (offset == null) {
            throw new NullPointerException("offset");
        }
        return java.time.OffsetDateTime.of(this, offset);
    }

    /** Esta fecha y hora en esa zona. */
    public ZonedDateTime atZone(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return ZonedDateTime.of(this, zone);
    }

    /**
     * Esta fecha y hora mas `amountToAdd` unidades.
     *
     * <p>Las unidades de tiempo van a la hora --y arrastran el dia si desbordan-- y las de fecha a
     * la fecha. Es la separacion que la clase tiene por dentro, expuesta.
     */
    public LocalDateTime plus(long amountToAdd, java.time.temporal.TemporalUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (unit == ChronoUnit.NANOS) {
            return this.plusNanos(amountToAdd);
        }
        if (unit == ChronoUnit.MICROS) {
            return this.plusNanos(amountToAdd * 1000L);
        }
        if (unit == ChronoUnit.MILLIS) {
            return this.plusNanos(amountToAdd * 1000000L);
        }
        if (unit == ChronoUnit.SECONDS) {
            return this.plusSeconds(amountToAdd);
        }
        if (unit == ChronoUnit.MINUTES) {
            return this.plusMinutes(amountToAdd);
        }
        if (unit == ChronoUnit.HOURS) {
            return this.plusHours(amountToAdd);
        }
        if (unit == ChronoUnit.HALF_DAYS) {
            return this.plusHours(amountToAdd * 12L);
        }
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

    public LocalDateTime minus(long amountToSubtract, java.time.temporal.TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    /** Con `field` puesto en `newValue`: los de hora van a la hora, los de fecha a la fecha. */
    public LocalDateTime with(java.time.temporal.TemporalField field, long newValue) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (field instanceof ChronoField) {
            ChronoField cf = (ChronoField) field;
            if (cf.isTimeBased()) {
                return LocalDateTime.of(this.date, this.time.with(field, newValue));
            }
            return LocalDateTime.of(this.date.with(field, newValue), this.time);
        }
        return (LocalDateTime) field.adjustInto(this, newValue);
    }

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
