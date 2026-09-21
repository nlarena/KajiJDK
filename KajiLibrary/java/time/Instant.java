package java.time;

import java.io.Serializable;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

// KajiLibrary's java.time.Instant — a point on the time-line, as seconds since the Unix epoch +
// a nanosecond-of-second (nanos normalised to [0, 1e9)). Immutable value type. Implements Temporal
// (so it supports plus/minus over units and cooperates with Duration.addTo), TemporalAdjuster and
// Comparable. A KajiLibrary subset: the field/unit support covers what our ChronoField/ChronoUnit
// subset offers (NANO_OF_SECOND; NANOS/MILLIS/SECONDS/MINUTES/HOURS/DAYS); toString/ISO deferred.
public final class Instant implements Temporal, TemporalAdjuster, Comparable<Instant>, Serializable {

    private static final long NANOS_PER_SECOND = 1000000000L;

    private final long seconds;
    private final int nanos;

    private Instant(long seconds, int nanos) {
        this.seconds = seconds;
        this.nanos = nanos;
    }

    // Normalise a nano adjustment into [0, 1e9), carrying into seconds (floor semantics).
    private static Instant create(long seconds, long nanoAdjustment) {
        long extraSeconds = nanoAdjustment / NANOS_PER_SECOND;
        long nos = nanoAdjustment % NANOS_PER_SECOND;
        if (nos < 0) {
            nos = nos + NANOS_PER_SECOND;
            extraSeconds = extraSeconds - 1;
        }
        return new Instant(seconds + extraSeconds, (int) nos);
    }

    /** The zero instant: 1970-01-01T00:00:00Z. */
    public static final Instant EPOCH = new Instant(0L, 0);

    /** The earliest representable instant. */
    public static final Instant MIN = new Instant(-31557014167219200L, 0);

    /** The latest. */
    public static final Instant MAX = new Instant(31556889864403199L, 999999999);

    /** The instant `temporal` holds. */
    public static Instant from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof Instant) {
            return (Instant) temporal;
        }
        if (!temporal.isSupported(ChronoField.INSTANT_SECONDS)) {
            throw new java.time.DateTimeException(
                    "Unable to obtain Instant from TemporalAccessor: " + temporal);
        }
        return Instant.ofEpochSecond(temporal.getLong(ChronoField.INSTANT_SECONDS),
                temporal.getLong(ChronoField.NANO_OF_SECOND));
    }

    /**
     * The instant `clock` reads.
     *
     * <p>It is the testable form of `now()`: with a `Clock.fixed` the result is the test's to
     * choose.
     */
    public static Instant now(Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        return clock.instant();
    }

    public static Instant ofEpochSecond(long epochSecond) {
        return new Instant(epochSecond, 0);
    }

    public static Instant ofEpochSecond(long epochSecond, long nanoAdjustment) {
        return create(epochSecond, nanoAdjustment);
    }

    public static Instant ofEpochMilli(long epochMilli) {
        return create(epochMilli / 1000L, (epochMilli % 1000L) * 1000000L);
    }

    // The current instant from the VM's wall clock (the one native seam).
    public static Instant now() {
        return Instant.ofEpochMilli(System.currentTimeMillis());
    }

    public long getEpochSecond() {
        return this.seconds;
    }

    public int getNano() {
        return this.nanos;
    }

    public long toEpochMilli() {
        return this.seconds * 1000L + this.nanos / 1000000L;
    }

    public Instant plusSeconds(long secondsToAdd) {
        return create(this.seconds + secondsToAdd, this.nanos);
    }

    public Instant plusMillis(long millisToAdd) {
        return create(this.seconds + millisToAdd / 1000L, (long) this.nanos + (millisToAdd % 1000L) * 1000000L);
    }

    public Instant plusNanos(long nanosToAdd) {
        return create(this.seconds, (long) this.nanos + nanosToAdd);
    }

    public Instant minusSeconds(long secondsToSubtract) {
        return this.plusSeconds(-secondsToSubtract);
    }

    public Instant minusMillis(long millisToSubtract) {
        return this.plusMillis(-millisToSubtract);
    }

    public Instant minusNanos(long nanosToSubtract) {
        return this.plusNanos(-nanosToSubtract);
    }

    public boolean isBefore(Instant other) {
        return this.compareTo(other) < 0;
    }

    public boolean isAfter(Instant other) {
        return this.compareTo(other) > 0;
    }

    public int compareTo(Instant other) {
        if (this.seconds < other.seconds) {
            return -1;
        }
        if (this.seconds > other.seconds) {
            return 1;
        }
        return this.nanos - other.nanos;
    }

    // --- Temporal ---

    /**
     * The four fields an instant has.
     *
     * <p>`INSTANT_SECONDS` is the one that **defines** the class, and it was missing: without it,
     * `Instant.from(anInstant)` threw and `DateTimeFormatter.ISO_INSTANT.format(...)` could not work.
     * It is the kind of hole that measuring by shape does not see -- the method was there, and
     * answered `false` about its own field.
     *
     * <p>The other three are the nano at its three granularities. An instant has **no** date and no
     * time of day: that needs a zone, and that is why `HOUR_OF_DAY` is not here.
     */
    public boolean isSupported(TemporalField field) {
        return field == ChronoField.INSTANT_SECONDS || field == ChronoField.NANO_OF_SECOND
                || field == ChronoField.MICRO_OF_SECOND || field == ChronoField.MILLI_OF_SECOND;
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.INSTANT_SECONDS) {
            return this.seconds;
        }
        if (field == ChronoField.NANO_OF_SECOND) {
            return (long) this.nanos;
        }
        if (field == ChronoField.MICRO_OF_SECOND) {
            return (long) (this.nanos / 1000);
        }
        if (field == ChronoField.MILLI_OF_SECOND) {
            return (long) (this.nanos / 1000000);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
    }

    public boolean isSupported(TemporalUnit unit) {
        return unit == ChronoUnit.NANOS || unit == ChronoUnit.MILLIS || unit == ChronoUnit.SECONDS
            || unit == ChronoUnit.MINUTES || unit == ChronoUnit.HOURS || unit == ChronoUnit.DAYS;
    }

    // The return is narrowed to `Instant`, as in the JDK: that way `i.plus(1, DAYS).getNano()`
    // compiles without a cast. It is a covariant override (§8.4.8.3).
    public Instant plus(long amountToAdd, TemporalUnit unit) {
        if (unit == ChronoUnit.NANOS) {
            return this.plusNanos(amountToAdd);
        }
        if (unit == ChronoUnit.MILLIS) {
            return this.plusMillis(amountToAdd);
        }
        if (unit == ChronoUnit.SECONDS) {
            return this.plusSeconds(amountToAdd);
        }
        if (unit == ChronoUnit.MINUTES) {
            return this.plusSeconds(amountToAdd * 60L);
        }
        if (unit == ChronoUnit.HOURS) {
            return this.plusSeconds(amountToAdd * 3600L);
        }
        if (unit == ChronoUnit.DAYS) {
            return this.plusSeconds(amountToAdd * 86400L);
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    public Instant minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    /** The duration from this instant to `endExclusive`. Negative if that one is earlier. */
    public Duration until(Instant endExclusive) {
        if (endExclusive == null) {
            throw new NullPointerException("endExclusive");
        }
        return Duration.between(this, endExclusive);
    }



    /**
     * This instant with `field` set to `newValue`.
     *
     * <p>The four fields an `Instant` knows. `NANO_OF_SECOND` and its two coarser siblings change the
     * fraction; `INSTANT_SECONDS` changes the whole second.
     */
    public Instant with(TemporalField field, long newValue) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (field == ChronoField.NANO_OF_SECOND) {
            ChronoField.NANO_OF_SECOND.checkValidValue(newValue);
            return Instant.ofEpochSecond(this.getEpochSecond(), newValue);
        }
        if (field == ChronoField.MICRO_OF_SECOND) {
            ChronoField.MICRO_OF_SECOND.checkValidValue(newValue);
            return Instant.ofEpochSecond(this.getEpochSecond(), newValue * 1000L);
        }
        if (field == ChronoField.MILLI_OF_SECOND) {
            ChronoField.MILLI_OF_SECOND.checkValidValue(newValue);
            return Instant.ofEpochSecond(this.getEpochSecond(), newValue * 1000000L);
        }
        if (field == ChronoField.INSTANT_SECONDS) {
            return Instant.ofEpochSecond(newValue, (long) this.getNano());
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
    }

    /**
     * This instant truncated to a multiple of `unit`, counting from the start of the day.
     *
     * <p>It truncates **downwards** --towards the past-- for the instants before the epoch as well,
     * which is where it differs from `Duration.truncatedTo`: that one truncates towards zero. The
     * difference is real and is in the JDK: an instant is a point on a line, and truncating a point
     * is going to the start of the interval containing it, whatever its sign.
     *
     * @throws java.time.DateTimeException if the unit does not divide a day
     */
    public Instant truncatedTo(TemporalUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (unit == ChronoUnit.NANOS) {
            return this;
        }
        Duration d = unit.getDuration();
        long unitNanos = d.toNanos();
        if (unitNanos > 86400000000000L) {
            throw new java.time.temporal.UnsupportedTemporalTypeException("Unit is too large to be used for truncation");
        }
        if (86400000000000L % unitNanos != 0L) {
            throw new java.time.temporal.UnsupportedTemporalTypeException("Unit must divide into a standard day without remainder");
        }
        // The nanos **within the day**, which is where truncating makes sense. `floorMod` is used so
        // that the instants before the epoch --with negative seconds-- still land at the start of
        // their interval and not at the end of the previous one.
        long secsOfDay = Math.floorMod(this.getEpochSecond(), 86400L);
        long nanosOfDay = secsOfDay * 1000000000L + this.getNano();
        long truncated = (nanosOfDay / unitNanos) * unitNanos;
        return this.plusNanos(truncated - nanosOfDay);
    }

    /**
     * This instant with that offset, as a local date and time.
     *
     * <p>The instant does not change: how it is read does. The same point on the timeline is 15:00 in
     * `+02:00` and 13:00 in UTC.
     */
    public OffsetDateTime atOffset(ZoneOffset offset) {
        if (offset == null) {
            throw new NullPointerException("offset");
        }
        long localSecs = this.getEpochSecond() + offset.getTotalSeconds();
        long day = Math.floorDiv(localSecs, 86400L);
        int secsOfDay = (int) Math.floorMod(localSecs, 86400L);
        LocalTime time = LocalTime.of(secsOfDay / 3600, (secsOfDay / 60) % 60,
                secsOfDay % 60, this.getNano());
        return OffsetDateTime.of(LocalDateTime.of(LocalDate.ofEpochDay(day), time), offset);
    }

    /** This instant in that zone. */
    public ZonedDateTime atZone(ZoneId zone) {
        return ZonedDateTime.ofInstant(this, zone);
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        Instant end = (Instant) endExclusive;
        long secondsDiff = end.seconds - this.seconds;
        long nanosDiff = (long) end.nanos - this.nanos;
        if (unit == ChronoUnit.SECONDS) {
            return secondsDiff;
        }
        if (unit == ChronoUnit.NANOS) {
            return secondsDiff * NANOS_PER_SECOND + nanosDiff;
        }
        if (unit == ChronoUnit.MILLIS) {
            return secondsDiff * 1000L + nanosDiff / 1000000L;
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    // --- TemporalAdjuster ---

    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.NANO_OF_SECOND, this.nanos);
    }

    // --- value-type methods (ISO-8601 instant, always UTC / 'Z') ---

    public String toString() {
        long epochDay = floorDiv(this.seconds, 86400L);
        int secsOfDay = (int) (this.seconds - epochDay * 86400L);
        String datePart = LocalDate.ofEpochDay(epochDay).toString();
        int hour = secsOfDay / 3600;
        int minute = (secsOfDay % 3600) / 60;
        int second = secsOfDay % 60;
        StringBuilder buf = new StringBuilder();
        buf.append(datePart);
        buf.append("T");
        if (hour < 10) {
            buf.append("0");
        }
        buf.append(Integer.toString(hour));
        if (minute < 10) {
            buf.append(":0");
        } else {
            buf.append(":");
        }
        buf.append(Integer.toString(minute));
        if (second < 10) {
            buf.append(":0");
        } else {
            buf.append(":");
        }
        buf.append(Integer.toString(second));
        if (this.nanos > 0) {
            buf.append(".");
            buf.append(nanoString(this.nanos));
        }
        buf.append("Z");
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
        if (obj instanceof Instant) {
            Instant o = (Instant) obj;
            return this.seconds == o.seconds && this.nanos == o.nanos;
        }
        return false;
    }

    public int hashCode() {
        return (int) (this.seconds ^ (this.seconds >>> 32)) + 51 * this.nanos;
    }

    private static long floorDiv(long a, long b) {
        long q = a / b;
        if ((a % b != 0) && ((a ^ b) < 0)) {
            q = q - 1;
        }
        return q;
    }

    // --- generic Temporal-typed conveniences ---

    public int get(TemporalField field) {
        return (int) this.getLong(field);
    }

    public Instant plus(TemporalAmount amount) {
        return (Instant) amount.addTo(this);
    }

    public Instant minus(TemporalAmount amount) {
        return (Instant) amount.subtractFrom(this);
    }

    public Instant with(TemporalAdjuster adjuster) {
        return (Instant) adjuster.adjustInto(this);
    }

    // Parses an ISO instant (uuuu-MM-ddTHH:mm:ss[.fff]Z), reusing LocalDate/LocalTime parsing.
    public static Instant parse(CharSequence text) {
        String s = text.toString();
        int t = -1;
        for (int k = 0; k < s.length(); k = k + 1) {
            if (s.charAt(k) == 'T') {
                t = k;
                break;
            }
        }
        LocalDate d = LocalDate.parse(s.substring(0, t));
        LocalTime tm = LocalTime.parse(s.substring(t + 1, s.length() - 1));
        long epochSecond = d.toEpochDay() * 86400L + tm.getHour() * 3600L + tm.getMinute() * 60L + tm.getSecond();
        return Instant.ofEpochSecond(epochSecond, tm.getNano());
    }
}
