package java.time;

import java.io.Serializable;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.ValueRange;

// KajiLibrary's java.time.OffsetTime -- a time with a fixed offset from UTC, such as 15:30+05:00.
// A value type composing a `LocalTime` and a `ZoneOffset`.
//
// **The distinction that organises the whole class** is the two `withOffset*`, and it is worth
// understanding before reading the rest:
//
//   - `withOffsetSameLocal` changes the offset and **keeps the time**: 15:30+02:00 becomes
//     15:30+05:00. It is another instant.
//   - `withOffsetSameInstant` changes the offset and **corrects the time** so that it stays the same
//     moment: 15:30+02:00 becomes 18:30+05:00.
//
// The same pair exists on `OffsetDateTime` and `ZonedDateTime`, and choosing the wrong one gives an
// error of hours that no size-shaped test detects.
//
// Comparison is by the equivalent instant in UTC, not by the written time: 15:30+02:00 is **before**
// 15:00+00:00 even though its clock time is greater. That is why `compareTo` then breaks ties by
// local time -- otherwise two different times naming the same instant would compare 0 and a
// `TreeSet` would keep only one.
public final class OffsetTime implements Temporal, TemporalAdjuster, Comparable<OffsetTime>, Serializable {

    private final LocalTime time;
    private final ZoneOffset offset;

    private OffsetTime(LocalTime time, ZoneOffset offset) {
        if (time == null || offset == null) {
            throw new NullPointerException();
        }
        this.time = time;
        this.offset = offset;
    }

    /** The smallest representable time, 00:00+18:00. */
    public static final OffsetTime MIN = new OffsetTime(LocalTime.MIN, ZoneOffset.MAX);

    /** The largest, 23:59:59.999999999-18:00. */
    public static final OffsetTime MAX = new OffsetTime(LocalTime.MAX, ZoneOffset.MIN);

    public static OffsetTime of(LocalTime time, ZoneOffset offset) {
        return new OffsetTime(time, offset);
    }

    public static OffsetTime of(int hour, int minute, int second, int nanoOfSecond, ZoneOffset offset) {
        return new OffsetTime(LocalTime.of(hour, minute, second, nanoOfSecond), offset);
    }

    /** The offset time of now, in the default zone. */
    public static OffsetTime now() {
        return OffsetTime.now(Clock.systemDefaultZone());
    }

    /** The one `clock` reads. The testable form of `now()`. */
    public static OffsetTime now(Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        return OffsetTime.ofInstant(clock.instant(), clock.getZone());
    }

    /** That zone's, right now. */
    public static OffsetTime now(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return OffsetTime.ofInstant(Instant.now(), zone);
    }

    /** The local time that instant falls on in that zone, with the zone's offset. */
    public static OffsetTime ofInstant(Instant instant, ZoneId zone) {
        if (instant == null || zone == null) {
            throw new NullPointerException();
        }
        ZoneOffset off = zone.getRules().getOffset(instant);
        long localSecs = instant.getEpochSecond() + off.getTotalSeconds();
        int secsOfDay = (int) Math.floorMod(localSecs, 86400L);
        return new OffsetTime(LocalTime.of(secsOfDay / 3600, (secsOfDay / 60) % 60,
                secsOfDay % 60, instant.getNano()), off);
    }

    /** The offset time `temporal` holds. */
    public static OffsetTime from(TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof OffsetTime) {
            return (OffsetTime) temporal;
        }
        return new OffsetTime(LocalTime.from(temporal), ZoneOffset.from(temporal));
    }

    /** It parses the ISO form, `HH:mm:ss+HH:MM`. */
    public static OffsetTime parse(CharSequence text) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        String s = text.toString();
        // The offset starts at the first `+`, `-` or `Z` **after** the time: searching from the
        // beginning would find the `-` of a negative time, which does not exist, but the code would
        // be left fragile against a different format.
        int i = 1;
        int cut = -1;
        while (i < s.length() && cut < 0) {
            char c = s.charAt(i);
            if (c == '+' || c == '-' || c == 'Z') {
                cut = i;
            }
            i = i + 1;
        }
        if (cut < 0) {
            throw new java.time.format.DateTimeParseException(
                    "Text '" + s + "' could not be parsed: no offset", text, 0);
        }
        return new OffsetTime(LocalTime.parse(s.substring(0, cut)),
                ZoneOffset.of(s.substring(cut)));
    }

    public LocalTime toLocalTime() {
        return this.time;
    }

    public ZoneOffset getOffset() {
        return this.offset;
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

    // ---- the two `withOffset` -------------------------------------------------------------------

    /**
     * Another offset, **the same time as written**: 15:30+02:00 becomes 15:30+05:00.
     *
     * <p>It is **another instant**. See the class's note.
     */
    public OffsetTime withOffsetSameLocal(ZoneOffset offset) {
        if (offset == null) {
            throw new NullPointerException("offset");
        }
        return offset.equals(this.offset) ? this : new OffsetTime(this.time, offset);
    }

    /**
     * Another offset, **the same instant**: 15:30+02:00 becomes 18:30+05:00.
     *
     * <p>The time is corrected by the difference between the two offsets.
     */
    public OffsetTime withOffsetSameInstant(ZoneOffset offset) {
        if (offset == null) {
            throw new NullPointerException("offset");
        }
        if (offset.equals(this.offset)) {
            return this;
        }
        int difference = offset.getTotalSeconds() - this.offset.getTotalSeconds();
        return new OffsetTime(this.time.plusSeconds((long) difference), offset);
    }

    // ---- the per-field `with*` ------------------------------------------------------------------

    public OffsetTime withHour(int hour) {
        return this.withTime(this.time.withHour(hour));
    }

    public OffsetTime withMinute(int minute) {
        return this.withTime(this.time.withMinute(minute));
    }

    public OffsetTime withSecond(int second) {
        return this.withTime(this.time.withSecond(second));
    }

    public OffsetTime withNano(int nanoOfSecond) {
        return this.withTime(this.time.withNano(nanoOfSecond));
    }

    /** Truncated to a multiple of `unit`; the offset is not touched. */
    public OffsetTime truncatedTo(TemporalUnit unit) {
        return this.withTime(this.time.truncatedTo(unit));
    }

    private OffsetTime withTime(LocalTime fresh) {
        return fresh.equals(this.time) ? this : new OffsetTime(fresh, this.offset);
    }

    // ---- arithmetic -----------------------------------------------------------------------------
    //
    // All of it goes to the local time and keeps the offset. That is right: adding an hour to
    // 23:30+02:00 gives 00:30+02:00, it does not change zone.

    public OffsetTime plusHours(long hours) {
        return this.withTime(this.time.plusHours(hours));
    }

    public OffsetTime plusMinutes(long minutes) {
        return this.withTime(this.time.plusMinutes(minutes));
    }

    public OffsetTime plusSeconds(long seconds) {
        return this.withTime(this.time.plusSeconds(seconds));
    }

    public OffsetTime plusNanos(long nanos) {
        return this.withTime(this.time.plusNanos(nanos));
    }

    public OffsetTime minusHours(long hours) {
        return this.plusHours(-hours);
    }

    public OffsetTime minusMinutes(long minutes) {
        return this.plusMinutes(-minutes);
    }

    public OffsetTime minusSeconds(long seconds) {
        return this.plusSeconds(-seconds);
    }

    public OffsetTime minusNanos(long nanos) {
        return this.plusNanos(-nanos);
    }

    public OffsetTime plus(long amountToAdd, TemporalUnit unit) {
        return this.withTime(this.time.plus(amountToAdd, unit));
    }

    public OffsetTime minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    public OffsetTime plus(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        return (OffsetTime) amount.addTo(this);
    }

    public OffsetTime minus(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        return (OffsetTime) amount.subtractFrom(this);
    }

    public OffsetTime with(TemporalAdjuster adjuster) {
        if (adjuster == null) {
            throw new NullPointerException("adjuster");
        }
        // An adjuster that **is** an offset or a time replaces that half; any other one is applied
        // to the whole.
        if (adjuster instanceof LocalTime) {
            return this.withTime((LocalTime) adjuster);
        }
        if (adjuster instanceof ZoneOffset) {
            return this.withOffsetSameLocal((ZoneOffset) adjuster);
        }
        if (adjuster instanceof OffsetTime) {
            return (OffsetTime) adjuster;
        }
        return (OffsetTime) adjuster.adjustInto(this);
    }

    public OffsetTime with(TemporalField field, long newValue) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (field == ChronoField.OFFSET_SECONDS) {
            return this.withOffsetSameLocal(
                    ZoneOffset.ofTotalSeconds((int) ChronoField.OFFSET_SECONDS.checkValidValue(newValue)));
        }
        if (field instanceof ChronoField) {
            return this.withTime(this.time.with(field, newValue));
        }
        return (OffsetTime) field.adjustInto(this, newValue);
    }

    // ---- TemporalAccessor / Temporal -------------------------------------------------------------

    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return field.isTimeBased() || field == ChronoField.OFFSET_SECONDS;
        }
        return field != null && field.isSupportedBy(this);
    }

    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit.isTimeBased();
        }
        return unit != null && unit.isSupportedBy(this);
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS) {
            return this.offset.getTotalSeconds();
        }
        return this.time.getLong(field);
    }

    public int get(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS) {
            return this.offset.getTotalSeconds();
        }
        return this.time.get(field);
    }

    public ValueRange range(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS) {
            return field.range();
        }
        return this.time.range(field);
    }

    public <R> R query(java.time.temporal.TemporalQuery<R> query) {
        if (query == java.time.temporal.TemporalQueries.offset()
                || query == java.time.temporal.TemporalQueries.zone()) {
            return (R) this.offset;
        }
        if (query == java.time.temporal.TemporalQueries.localTime()) {
            return (R) this.time;
        }
        if (query == java.time.temporal.TemporalQueries.precision()) {
            return (R) ChronoUnit.NANOS;
        }
        return query.queryFrom(this);
    }

    /** It puts this one's time and offset into `temporal`. */
    public Temporal adjustInto(Temporal temporal) {
        return temporal
                .with(ChronoField.NANO_OF_DAY, this.time.toNanoOfDay())
                .with(ChronoField.OFFSET_SECONDS, this.offset.getTotalSeconds());
    }

    /** How many `unit` there are between this time and `endExclusive`, **compared in UTC**. */
    public long until(Temporal endExclusive, TemporalUnit unit) {
        OffsetTime end = OffsetTime.from(endExclusive);
        long nanos = end.toEpochNanoUtc() - this.toEpochNanoUtc();
        if (unit == ChronoUnit.NANOS) {
            return nanos;
        }
        if (unit == ChronoUnit.MICROS) {
            return nanos / 1000L;
        }
        if (unit == ChronoUnit.MILLIS) {
            return nanos / 1000000L;
        }
        if (unit == ChronoUnit.SECONDS) {
            return nanos / 1000000000L;
        }
        if (unit == ChronoUnit.MINUTES) {
            return nanos / 60000000000L;
        }
        if (unit == ChronoUnit.HOURS) {
            return nanos / 3600000000000L;
        }
        if (unit == ChronoUnit.HALF_DAYS) {
            return nanos / 43200000000000L;
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    // ---- conversions and comparison --------------------------------------------------------------

    /** This time on that date, keeping the offset. */
    public OffsetDateTime atDate(LocalDate date) {
        if (date == null) {
            throw new NullPointerException("date");
        }
        return OffsetDateTime.of(LocalDateTime.of(date, this.time), this.offset);
    }

    /** The seconds since the epoch of this time on that date. */
    public long toEpochSecond(LocalDate date) {
        if (date == null) {
            throw new NullPointerException("date");
        }
        return date.toEpochDay() * 86400L + this.time.toSecondOfDay() - this.offset.getTotalSeconds();
    }

    /** Formateada. */
    public String format(java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        return formatter.format(this);
    }

    /** Whether they name the **same instant**, even if their written time differs. */
    public boolean isEqual(OffsetTime other) {
        return this.toEpochNanoUtc() == other.toEpochNanoUtc();
    }

    public boolean isBefore(OffsetTime other) {
        return this.toEpochNanoUtc() < other.toEpochNanoUtc();
    }

    public boolean isAfter(OffsetTime other) {
        return this.toEpochNanoUtc() > other.toEpochNanoUtc();
    }

    public String toString() {
        return this.time.toString() + this.offset.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OffsetTime) {
            OffsetTime o = (OffsetTime) obj;
            return this.time.equals(o.time) && this.offset.equals(o.offset);
        }
        return false;
    }

    public int hashCode() {
        return this.time.hashCode() ^ this.offset.hashCode();
    }

    /**
     * By instant, and at equal instants by local time.
     *
     * <p>The tie-break is not decoration: without it, 15:30+02:00 and 13:30+00:00 compare 0 without
     * being `equals`, and a `TreeSet` would silently keep only one of the two.
     */
    public int compareTo(OffsetTime other) {
        if (this.offset.equals(other.offset)) {
            return this.time.compareTo(other.time);
        }
        long a = this.toEpochNanoUtc();
        long b = other.toEpochNanoUtc();
        if (a < b) {
            return -1;
        }
        if (a > b) {
            return 1;
        }
        return this.time.compareTo(other.time);
    }

    private long toEpochNanoUtc() {
        long nod = this.getHour() * 3600000000000L + this.getMinute() * 60000000000L
            + this.getSecond() * 1000000000L + this.getNano();
        return nod - this.offset.getTotalSeconds() * 1000000000L;
    }

    /**
     * It reads `text` with that formatter.
     *
     * <p>The one that decides which fields are there is the formatter; this class only says
     * **which of them it wants**, by passing its own `from`. That is why a pattern that brings no time and offset
     * fails here and not when the result is used.
     *
     * @throws java.time.format.DateTimeParseException if the text does not fit the pattern, or what
     *     fits is not enough for a time with an offset
     */
    public static OffsetTime parse(CharSequence text, java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        // Bound to a local: chaining through an interface-typed intermediate gets lost (#108).
        java.time.temporal.TemporalQuery<OffsetTime> queryOf = OffsetTime::from;
        return formatter.parse(text, queryOf);
    }
}
