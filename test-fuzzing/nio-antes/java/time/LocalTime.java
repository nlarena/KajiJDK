package java.time;

import java.io.Serializable;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;

// KajiLibrary's java.time.LocalTime — a time of day (hour/minute/second/nano) without date or zone.
// Immutable value type. Arithmetic wraps within a 24h day (via nano-of-day). Implements Temporal,
// TemporalAdjuster and Comparable. A KajiLibrary subset (toString/parse and some fields deferred).
public final class LocalTime implements Temporal, TemporalAdjuster, Comparable<LocalTime>, Serializable {

    private static final long NANOS_PER_SECOND = 1000000000L;
    private static final long NANOS_PER_DAY = 86400L * NANOS_PER_SECOND;

    private final int hour;
    private final int minute;
    private final int second;
    private final int nano;

    private LocalTime(int hour, int minute, int second, int nano) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.nano = nano;
    }

    /** Midnight, 00:00. */
    public static final LocalTime MIN = new LocalTime(0, 0, 0, 0);

    /** The last representable instant of the day, 23:59:59.999999999. */
    public static final LocalTime MAX = new LocalTime(23, 59, 59, 999999999);

    /** A synonym for `MIN`, with the name that reads better against a date. */
    public static final LocalTime MIDNIGHT = new LocalTime(0, 0, 0, 0);

    /** Noon, 12:00. */
    public static final LocalTime NOON = new LocalTime(12, 0, 0, 0);

    /** The time `temporal` holds. */
    public static LocalTime from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof LocalTime) {
            return (LocalTime) temporal;
        }
        if (!temporal.isSupported(ChronoField.NANO_OF_DAY)) {
            throw new java.time.DateTimeException(
                    "Unable to obtain LocalTime from TemporalAccessor: " + temporal);
        }
        return LocalTime.ofNanoOfDay(temporal.getLong(ChronoField.NANO_OF_DAY));
    }

    /** The time `clock` reads. The testable form of `now()`. */
    public static LocalTime now(java.time.Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        return LocalTime.ofInstant(clock.instant(), clock.getZone());
    }

    /** The time in that zone, right now. */
    public static LocalTime now(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return LocalTime.ofInstant(Instant.now(), zone);
    }

    /**
     * The local time that instant falls on in that zone.
     *
     * <p>The date is dropped on purpose: the same instant is the same clock time across the whole
     * zone, and `LocalTime` is exactly that -- a time with no day.
     */
    public static LocalTime ofInstant(Instant instant, ZoneId zone) {
        if (instant == null || zone == null) {
            throw new NullPointerException();
        }
        ZoneOffset offset = zone.getRules().getOffset(instant);
        long localSecs = instant.getEpochSecond() + offset.getTotalSeconds();
        int secsOfDay = (int) Math.floorMod(localSecs, 86400L);
        return new LocalTime(secsOfDay / 3600, (secsOfDay / 60) % 60, secsOfDay % 60,
                instant.getNano());
    }

    public static LocalTime of(int hour, int minute) {
        return new LocalTime(hour, minute, 0, 0);
    }

    public static LocalTime of(int hour, int minute, int second) {
        return new LocalTime(hour, minute, second, 0);
    }

    public static LocalTime of(int hour, int minute, int second, int nanoOfSecond) {
        return new LocalTime(hour, minute, second, nanoOfSecond);
    }

    public static LocalTime ofSecondOfDay(long secondOfDay) {
        int hour = (int) (secondOfDay / 3600L);
        long rem = secondOfDay - hour * 3600L;
        int minute = (int) (rem / 60L);
        int second = (int) (rem - minute * 60L);
        return new LocalTime(hour, minute, second, 0);
    }

    public static LocalTime ofNanoOfDay(long nanoOfDay) {
        int hour = (int) (nanoOfDay / 3600000000000L);
        long rem = nanoOfDay - hour * 3600000000000L;
        int minute = (int) (rem / 60000000000L);
        rem = rem - minute * 60000000000L;
        int second = (int) (rem / NANOS_PER_SECOND);
        int nano = (int) (rem - second * NANOS_PER_SECOND);
        return new LocalTime(hour, minute, second, nano);
    }

    public static LocalTime now() {
        return LocalTime.ofNanoOfDay((System.currentTimeMillis() % 86400000L) * 1000000L);
    }

    public int getHour() {
        return this.hour;
    }

    public int getMinute() {
        return this.minute;
    }

    public int getSecond() {
        return this.second;
    }

    public int getNano() {
        return this.nano;
    }

    public int toSecondOfDay() {
        return this.hour * 3600 + this.minute * 60 + this.second;
    }

    public long toNanoOfDay() {
        return this.hour * 3600000000000L + this.minute * 60000000000L
            + this.second * NANOS_PER_SECOND + this.nano;
    }

    // --- arithmetic (wraps within the day) ---

    public LocalTime plusNanos(long nanosToAdd) {
        long nofd = this.toNanoOfDay();
        long newNofd = ((nofd + nanosToAdd) % NANOS_PER_DAY + NANOS_PER_DAY) % NANOS_PER_DAY;
        return LocalTime.ofNanoOfDay(newNofd);
    }

    public LocalTime plusSeconds(long secondsToAdd) {
        return this.plusNanos((secondsToAdd % 86400L) * NANOS_PER_SECOND);
    }

    public LocalTime plusMinutes(long minutesToAdd) {
        return this.plusSeconds((minutesToAdd % 1440L) * 60L);
    }

    public LocalTime plusHours(long hoursToAdd) {
        return this.plusSeconds((hoursToAdd % 24L) * 3600L);
    }

    public LocalTime minusNanos(long nanosToSubtract) {
        return this.plusNanos(-(nanosToSubtract % NANOS_PER_DAY));
    }

    public LocalTime minusSeconds(long secondsToSubtract) {
        return this.plusSeconds(-(secondsToSubtract % 86400L));
    }

    public LocalTime minusMinutes(long minutesToSubtract) {
        return this.plusMinutes(-(minutesToSubtract % 1440L));
    }

    public LocalTime minusHours(long hoursToSubtract) {
        return this.plusHours(-(hoursToSubtract % 24L));
    }

    // --- comparison ---

    public int compareTo(LocalTime other) {
        if (this.hour != other.hour) {
            return this.hour - other.hour;
        }
        if (this.minute != other.minute) {
            return this.minute - other.minute;
        }
        if (this.second != other.second) {
            return this.second - other.second;
        }
        return this.nano - other.nano;
    }

    public boolean isBefore(LocalTime other) {
        return this.compareTo(other) < 0;
    }

    public boolean isAfter(LocalTime other) {
        return this.compareTo(other) > 0;
    }

    // --- Temporal ---

    /**
     * The fields a time has.
     *
     * <p>The right answer is "all the time-based ones", and that is why it asks about the category
     * instead of enumerating: a list drifts out of step with `getLong` --it happened, and
     * `SECOND_OF_DAY` said no and was then computed all the same-- whereas the category cannot.
     */
    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return ((ChronoField) field).isTimeBased();
        }
        return field != null && field.isSupportedBy(this);
    }

    /**
     * That field's value.
     *
     * <p>The first four are the state; the other nine are **derived**, and they are here because they
     * are exact functions of what there is -- there is no decision to take in computing them. They
     * used to be missing, and that made a formatter with `hh:mm a` or with `SSS` unable to read the
     * time in front of it.
     *
     * <p>The two `CLOCK_HOUR_*` are the only ones with a twist: they count 1 to 12 (or 1 to 24)
     * instead of 0 to 11, so zero maps to the maximum. It is what makes midnight be written
     * `12:00 AM` and not `0:00 AM`.
     */
    public long getLong(TemporalField field) {
        if (field == ChronoField.HOUR_OF_DAY) {
            return (long) this.hour;
        }
        if (field == ChronoField.MINUTE_OF_HOUR) {
            return (long) this.minute;
        }
        if (field == ChronoField.SECOND_OF_MINUTE) {
            return (long) this.second;
        }
        if (field == ChronoField.NANO_OF_SECOND) {
            return (long) this.nano;
        }
        if (field == ChronoField.NANO_OF_DAY) {
            return this.toNanoOfDay();
        }
        if (field == ChronoField.MICRO_OF_SECOND) {
            return (long) (this.nano / 1000);
        }
        if (field == ChronoField.MICRO_OF_DAY) {
            return this.toNanoOfDay() / 1000L;
        }
        if (field == ChronoField.MILLI_OF_SECOND) {
            return (long) (this.nano / 1000000);
        }
        if (field == ChronoField.MILLI_OF_DAY) {
            return this.toNanoOfDay() / 1000000L;
        }
        if (field == ChronoField.SECOND_OF_DAY) {
            return (long) this.toSecondOfDay();
        }
        if (field == ChronoField.MINUTE_OF_DAY) {
            return (long) (this.hour * 60 + this.minute);
        }
        if (field == ChronoField.HOUR_OF_AMPM) {
            return (long) (this.hour % 12);
        }
        if (field == ChronoField.CLOCK_HOUR_OF_AMPM) {
            int h = this.hour % 12;
            return (long) (h == 0 ? 12 : h);
        }
        if (field == ChronoField.CLOCK_HOUR_OF_DAY) {
            return (long) (this.hour == 0 ? 24 : this.hour);
        }
        if (field == ChronoField.AMPM_OF_DAY) {
            return (long) (this.hour / 12);
        }
        if (field != null && !(field instanceof ChronoField)) {
            // A third party's field knows how to read itself: the ball is passed to it instead of
            // rejecting it for not being on the list.
            return field.getFrom(this);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
    }

    public boolean isSupported(TemporalUnit unit) {
        return unit == ChronoUnit.NANOS || unit == ChronoUnit.MILLIS || unit == ChronoUnit.SECONDS
            || unit == ChronoUnit.MINUTES || unit == ChronoUnit.HOURS;
    }

    /** This time with another hour of the day; the rest is left alone. */
    public LocalTime withHour(int hour) {
        if (this.hour == hour) {
            return this;
        }
        ChronoField.HOUR_OF_DAY.checkValidValue((long) hour);
        return new LocalTime(hour, this.minute, this.second, this.nano);
    }

    public LocalTime withMinute(int minute) {
        if (this.minute == minute) {
            return this;
        }
        ChronoField.MINUTE_OF_HOUR.checkValidValue((long) minute);
        return new LocalTime(this.hour, minute, this.second, this.nano);
    }

    public LocalTime withSecond(int second) {
        if (this.second == second) {
            return this;
        }
        ChronoField.SECOND_OF_MINUTE.checkValidValue((long) second);
        return new LocalTime(this.hour, this.minute, second, this.nano);
    }

    public LocalTime withNano(int nanoOfSecond) {
        if (this.nano == nanoOfSecond) {
            return this;
        }
        ChronoField.NANO_OF_SECOND.checkValidValue((long) nanoOfSecond);
        return new LocalTime(this.hour, this.minute, this.second, nanoOfSecond);
    }

    /**
     * This time truncated to a multiple of `unit`, counting from midnight.
     *
     * @throws java.time.DateTimeException if the unit does not divide a day
     */
    public LocalTime truncatedTo(TemporalUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (unit == ChronoUnit.NANOS) {
            return this;
        }
        long unitNanos = unit.getDuration().toNanos();
        if (unitNanos > 86400000000000L) {
            throw new java.time.temporal.UnsupportedTemporalTypeException(
                    "Unit is too large to be used for truncation");
        }
        if (86400000000000L % unitNanos != 0L) {
            throw new java.time.temporal.UnsupportedTemporalTypeException(
                    "Unit must divide into a standard day without remainder");
        }
        // A time of day is never negative, so integer division is enough -- there is no need for the
        // `floorMod` that `Instant` does need, where the seconds can be before the epoch.
        long nanos = this.toNanoOfDay();
        return LocalTime.ofNanoOfDay((nanos / unitNanos) * unitNanos);
    }

    /** This time on that date. */
    public LocalDateTime atDate(LocalDate date) {
        if (date == null) {
            throw new NullPointerException("date");
        }
        return LocalDateTime.of(date, this);
    }

    /** This time with that offset. */
    public java.time.OffsetTime atOffset(ZoneOffset offset) {
        if (offset == null) {
            throw new NullPointerException("offset");
        }
        return java.time.OffsetTime.of(this, offset);
    }

    /**
     * The seconds since the epoch of this time on that date and with that offset.
     *
     * <p>All three are needed and no fewer: a time alone does not place a point on the timeline, and
     * neither does a date and time with no offset.
     */
    public long toEpochSecond(LocalDate date, ZoneOffset offset) {
        if (date == null || offset == null) {
            throw new NullPointerException();
        }
        return date.toEpochDay() * 86400L + this.toSecondOfDay() - offset.getTotalSeconds();
    }

    // The return is narrowed to `LocalTime`, as in the JDK (a covariant override, §8.4.8.3), and the
    // fields and units that were missing are covered -- before there were only four fields, and the
    // exception was the wrong one: the contract asks for `UnsupportedTemporalTypeException`, not
    // `IllegalArgumentException`.
    public LocalTime with(TemporalField field, long newValue) {
        if (field == ChronoField.HOUR_OF_DAY) {
            return new LocalTime((int) newValue, this.minute, this.second, this.nano);
        }
        if (field == ChronoField.MINUTE_OF_HOUR) {
            return new LocalTime(this.hour, (int) newValue, this.second, this.nano);
        }
        if (field == ChronoField.SECOND_OF_MINUTE) {
            return new LocalTime(this.hour, this.minute, (int) newValue, this.nano);
        }
        if (field == ChronoField.NANO_OF_SECOND) {
            return new LocalTime(this.hour, this.minute, this.second, (int) newValue);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
    }

    public LocalTime plus(long amountToAdd, TemporalUnit unit) {
        if (unit == ChronoUnit.NANOS) {
            return this.plusNanos(amountToAdd);
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
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    public LocalTime minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        LocalTime end = (LocalTime) endExclusive;
        long nanosDiff = end.toNanoOfDay() - this.toNanoOfDay();
        if (unit == ChronoUnit.NANOS) {
            return nanosDiff;
        }
        if (unit == ChronoUnit.SECONDS) {
            return nanosDiff / NANOS_PER_SECOND;
        }
        if (unit == ChronoUnit.MINUTES) {
            return nanosDiff / (60L * NANOS_PER_SECOND);
        }
        if (unit == ChronoUnit.HOURS) {
            return nanosDiff / (3600L * NANOS_PER_SECOND);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.NANO_OF_SECOND, this.nano);
    }

    // --- value-type methods (ISO-8601) ---

    // ISO-8601: HH:mm, plus :ss when seconds or nanos are non-zero, plus a fractional part
    // (3, 6 or 9 digits) when nanos are non-zero — the same layout java.time uses.
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (this.hour < 10) {
            buf.append("0");
        }
        buf.append(Integer.toString(this.hour));
        if (this.minute < 10) {
            buf.append(":0");
        } else {
            buf.append(":");
        }
        buf.append(Integer.toString(this.minute));
        if (this.second > 0 || this.nano > 0) {
            if (this.second < 10) {
                buf.append(":0");
            } else {
                buf.append(":");
            }
            buf.append(Integer.toString(this.second));
            if (this.nano > 0) {
                buf.append(".");
                buf.append(nanoString(this.nano));
            }
        }
        return buf.toString();
    }

    private static String nanoString(int nano) {
        if (nano % 1000000 == 0) {
            String t = Integer.toString(nano / 1000000 + 1000);
            return t.substring(1, t.length());
        }
        if (nano % 1000 == 0) {
            String t = Integer.toString(nano / 1000 + 1000000);
            return t.substring(1, t.length());
        }
        String t = Integer.toString(nano + 1000000000);
        return t.substring(1, t.length());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LocalTime) {
            LocalTime o = (LocalTime) obj;
            return this.hour == o.hour && this.minute == o.minute
                && this.second == o.second && this.nano == o.nano;
        }
        return false;
    }

    public int hashCode() {
        long nod = this.hour * 3600000000000L + this.minute * 60000000000L
            + this.second * 1000000000L + this.nano;
        return (int) (nod ^ (nod >>> 32));
    }

    // --- generic Temporal-typed conveniences ---

    public int get(TemporalField field) {
        return (int) this.getLong(field);
    }

    public LocalTime plus(TemporalAmount amount) {
        return (LocalTime) amount.addTo(this);
    }

    public LocalTime minus(TemporalAmount amount) {
        return (LocalTime) amount.subtractFrom(this);
    }

    public LocalTime with(TemporalAdjuster adjuster) {
        return (LocalTime) adjuster.adjustInto(this);
    }

    // Parses an ISO-8601 time (HH:mm[:ss[.fraction]]).
    public static LocalTime parse(CharSequence text) {
        String s = text.toString();
        int hour = parseDigits(s, 0, 2);
        int minute = parseDigits(s, 3, 5);
        int second = 0;
        int nano = 0;
        if (s.length() > 5 && s.charAt(5) == ':') {
            second = parseDigits(s, 6, 8);
            if (s.length() > 8 && s.charAt(8) == '.') {
                String frac = s.substring(9, s.length());
                while (frac.length() < 9) {
                    frac = frac + "0";
                }
                nano = parseDigits(frac, 0, 9);
            }
        }
        return LocalTime.of(hour, minute, second, nano);
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    private static int parseDigits(String s, int from, int to) {
        int v = 0;
        for (int k = from; k < to; k = k + 1) {
            v = v * 10 + (s.charAt(k) - '0');
        }
        return v;
    }

    /**
     * It reads `text` with that formatter.
     *
     * <p>The one that decides which fields are there is the formatter; this class only says
     * **which of them it wants**, by passing its own `from`. That is why a pattern that brings no time
     * fails here and not when the result is used.
     *
     * @throws java.time.format.DateTimeParseException if the text does not fit the pattern, or what
     *     fits is not enough for a time
     */
    public static LocalTime parse(CharSequence text, java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        // Bound to a local: chaining through an interface-typed intermediate gets lost (#108).
        java.time.temporal.TemporalQuery<LocalTime> queryOf = LocalTime::from;
        return formatter.parse(text, queryOf);
    }
}
