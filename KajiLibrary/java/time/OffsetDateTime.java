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

// KajiLibrary's java.time.OffsetDateTime -- a date and time with a fixed offset from UTC, such as
// 2026-08-04T15:30+05:30. It composes a `LocalDateTime` and a `ZoneOffset`.
//
// `OffsetTime`'s note on the two `withOffset*` holds here too, and here it matters more because the
// shift can change the **day**:
//
//   - `withOffsetSameLocal` keeps the date and time as written and changes the instant;
//   - `withOffsetSameInstant` keeps the instant and corrects the date and time.
//
// The difference between this class and `ZonedDateTime` is that here the offset is **fixed**: no
// daylight saving, no rules, no jumps. It is what is wanted for recording a moment --a log entry, a
// timestamp-- and what is **not** wanted for scheduling something in the future, where the zone can
// change its rules before the date arrives.
public final class OffsetDateTime implements Temporal, TemporalAdjuster, Comparable<OffsetDateTime>, Serializable {

    private final LocalDateTime dateTime;
    private final ZoneOffset offset;

    private OffsetDateTime(LocalDateTime dateTime, ZoneOffset offset) {
        this.dateTime = dateTime;
        this.offset = offset;
    }

    /** The smallest representable value. */
    public static final OffsetDateTime MIN = new OffsetDateTime(LocalDateTime.MIN, ZoneOffset.MAX);

    /** The largest. */
    public static final OffsetDateTime MAX = new OffsetDateTime(LocalDateTime.MAX, ZoneOffset.MIN);

    /** Now, in the default zone. */
    public static OffsetDateTime now() {
        return OffsetDateTime.now(Clock.systemDefaultZone());
    }

    /** The one `clock` reads. The testable form of `now()`. */
    public static OffsetDateTime now(Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        return OffsetDateTime.ofInstant(clock.instant(), clock.getZone());
    }

    /** That zone's, right now. */
    public static OffsetDateTime now(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return OffsetDateTime.ofInstant(Instant.now(), zone);
    }

    /** That instant's local date and time in that zone, with the zone's offset. */
    public static OffsetDateTime ofInstant(Instant instant, ZoneId zone) {
        if (instant == null || zone == null) {
            throw new NullPointerException();
        }
        ZoneOffset off = zone.getRules().getOffset(instant);
        return new OffsetDateTime(
                LocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), off), off);
    }

    /** The offset date and time `temporal` holds. */
    public static OffsetDateTime from(TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof OffsetDateTime) {
            return (OffsetDateTime) temporal;
        }
        return new OffsetDateTime(LocalDateTime.from(temporal), ZoneOffset.from(temporal));
    }

    /** It parses the ISO form, `yyyy-MM-ddTHH:mm:ss+HH:MM`. */
    public static OffsetDateTime parse(CharSequence text) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        String s = text.toString();
        // The offset starts after the `T`: searching from the beginning would find the date's
        // hyphens.
        int t = s.indexOf('T');
        if (t < 0) {
            throw new java.time.format.DateTimeParseException(
                    "Text '" + s + "' could not be parsed: no time part", text, 0);
        }
        int i = t + 1;
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
        return new OffsetDateTime(LocalDateTime.parse(s.substring(0, cut)),
                ZoneOffset.of(s.substring(cut)));
    }

    /** With the month as an enum. */
    public static OffsetDateTime of(int year, Month month, int dayOfMonth, int hour, int minute,
            int second, int nanoOfSecond, ZoneOffset offset) {
        if (month == null) {
            throw new NullPointerException("month");
        }
        return OffsetDateTime.of(year, month.getValue(), dayOfMonth, hour, minute, second,
                nanoOfSecond, offset);
    }

    public static OffsetDateTime of(LocalDateTime dateTime, ZoneOffset offset) {
        return new OffsetDateTime(dateTime, offset);
    }

    public static OffsetDateTime of(LocalDate date, LocalTime time, ZoneOffset offset) {
        return new OffsetDateTime(LocalDateTime.of(date, time), offset);
    }

    public static OffsetDateTime of(int year, int month, int dayOfMonth, int hour, int minute,
                                    int second, int nanoOfSecond, ZoneOffset offset) {
        LocalDateTime dt = LocalDateTime.of(LocalDate.of(year, month, dayOfMonth),
            LocalTime.of(hour, minute, second, nanoOfSecond));
        return new OffsetDateTime(dt, offset);
    }

    public LocalDateTime toLocalDateTime() {
        return this.dateTime;
    }

    public LocalDate toLocalDate() {
        return this.dateTime.toLocalDate();
    }

    public LocalTime toLocalTime() {
        return this.dateTime.toLocalTime();
    }

    public ZoneOffset getOffset() {
        return this.offset;
    }

    public int getYear() {
        return this.dateTime.getYear();
    }

    public int getMonthValue() {
        return this.dateTime.getMonthValue();
    }

    public int getDayOfMonth() {
        return this.dateTime.getDayOfMonth();
    }

    public int getHour() {
        return this.dateTime.getHour();
    }

    public int getMinute() {
        return this.dateTime.getMinute();
    }

    public int getSecond() {
        return this.dateTime.getSecond();
    }

    public int getNano() {
        return this.dateTime.getNano();
    }

    public DayOfWeek getDayOfWeek() {
        return this.dateTime.getDayOfWeek();
    }

    public int getDayOfYear() {
        return this.dateTime.getDayOfYear();
    }

    public Month getMonth() {
        return this.dateTime.getMonth();
    }

    // ---- the two `withOffset` -------------------------------------------------------------------

    /**
     * Another offset, **the same date and time as written**. It is another instant.
     *
     * <p>See the class's note: choosing this one when the other was meant shifts the moment by the
     * difference between the two offsets, with nothing to warn of it.
     */
    public OffsetDateTime withOffsetSameLocal(ZoneOffset offset) {
        if (offset == null) {
            throw new NullPointerException("offset");
        }
        return offset.equals(this.offset) ? this : new OffsetDateTime(this.dateTime, offset);
    }

    /**
     * Another offset, **the same instant**: the date and time are corrected, and can change day.
     */
    public OffsetDateTime withOffsetSameInstant(ZoneOffset offset) {
        if (offset == null) {
            throw new NullPointerException("offset");
        }
        if (offset.equals(this.offset)) {
            return this;
        }
        int difference = offset.getTotalSeconds() - this.offset.getTotalSeconds();
        return new OffsetDateTime(this.dateTime.plusSeconds((long) difference), offset);
    }

    // ---- the per-field `with*` -----------------------------------------------------------------

    public OffsetDateTime withYear(int year) {
        return this.resolveLocal(this.dateTime.withYear(year));
    }

    public OffsetDateTime withMonth(int month) {
        return this.resolveLocal(this.dateTime.withMonth(month));
    }

    public OffsetDateTime withDayOfMonth(int dayOfMonth) {
        return this.resolveLocal(this.dateTime.withDayOfMonth(dayOfMonth));
    }

    public OffsetDateTime withDayOfYear(int dayOfYear) {
        return this.resolveLocal(this.dateTime.withDayOfYear(dayOfYear));
    }

    public OffsetDateTime withHour(int hour) {
        return this.resolveLocal(this.dateTime.withHour(hour));
    }

    public OffsetDateTime withMinute(int minute) {
        return this.resolveLocal(this.dateTime.withMinute(minute));
    }

    public OffsetDateTime withSecond(int second) {
        return this.resolveLocal(this.dateTime.withSecond(second));
    }

    public OffsetDateTime withNano(int nanoOfSecond) {
        return this.resolveLocal(this.dateTime.withNano(nanoOfSecond));
    }

    public OffsetDateTime truncatedTo(TemporalUnit unit) {
        return this.resolveLocal(this.dateTime.truncatedTo(unit));
    }

    private OffsetDateTime resolveLocal(LocalDateTime newOne) {
        return newOne.equals(this.dateTime) ? this : new OffsetDateTime(newOne, this.offset);
    }

    // ---- arithmetic -----------------------------------------------------------------------------
    //
    // All of it goes to the local date and time, keeping the offset. Adding a day to
    // 2026-03-28T23:00+01:00 gives 2026-03-29T23:00+01:00: the written time does not move. That is
    // the difference from `ZonedDateTime`, where the same day can have 23 or 25 hours.

    public OffsetDateTime plusYears(long years) {
        return this.resolveLocal(this.dateTime.plusYears(years));
    }

    public OffsetDateTime plusMonths(long months) {
        return this.resolveLocal(this.dateTime.plusMonths(months));
    }

    public OffsetDateTime plusWeeks(long weeks) {
        return this.resolveLocal(this.dateTime.plusWeeks(weeks));
    }

    public OffsetDateTime plusDays(long days) {
        return this.resolveLocal(this.dateTime.plusDays(days));
    }

    public OffsetDateTime plusHours(long hours) {
        return this.resolveLocal(this.dateTime.plusHours(hours));
    }

    public OffsetDateTime plusMinutes(long minutes) {
        return this.resolveLocal(this.dateTime.plusMinutes(minutes));
    }

    public OffsetDateTime plusSeconds(long seconds) {
        return this.resolveLocal(this.dateTime.plusSeconds(seconds));
    }

    public OffsetDateTime plusNanos(long nanos) {
        return this.resolveLocal(this.dateTime.plusNanos(nanos));
    }

    public OffsetDateTime minusYears(long years) {
        return this.plusYears(-years);
    }

    public OffsetDateTime minusMonths(long months) {
        return this.plusMonths(-months);
    }

    public OffsetDateTime minusWeeks(long weeks) {
        return this.plusWeeks(-weeks);
    }

    public OffsetDateTime minusDays(long days) {
        return this.plusDays(-days);
    }

    public OffsetDateTime minusHours(long hours) {
        return this.plusHours(-hours);
    }

    public OffsetDateTime minusMinutes(long minutes) {
        return this.plusMinutes(-minutes);
    }

    public OffsetDateTime minusSeconds(long seconds) {
        return this.plusSeconds(-seconds);
    }

    public OffsetDateTime minusNanos(long nanos) {
        return this.plusNanos(-nanos);
    }

    public OffsetDateTime plus(long amountToAdd, TemporalUnit unit) {
        return this.resolveLocal(this.dateTime.plus(amountToAdd, unit));
    }

    public OffsetDateTime minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    public OffsetDateTime plus(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        return (OffsetDateTime) amount.addTo(this);
    }

    public OffsetDateTime minus(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        return (OffsetDateTime) amount.subtractFrom(this);
    }

    public OffsetDateTime with(TemporalAdjuster adjuster) {
        if (adjuster == null) {
            throw new NullPointerException("adjuster");
        }
        if (adjuster instanceof LocalDate || adjuster instanceof LocalTime
                || adjuster instanceof LocalDateTime) {
            return this.resolveLocal((LocalDateTime) LocalDateTime.from(
                    (TemporalAccessor) adjuster.adjustInto(this.dateTime)));
        }
        if (adjuster instanceof ZoneOffset) {
            return this.withOffsetSameLocal((ZoneOffset) adjuster);
        }
        if (adjuster instanceof OffsetDateTime) {
            return (OffsetDateTime) adjuster;
        }
        return (OffsetDateTime) adjuster.adjustInto(this);
    }

    public OffsetDateTime with(TemporalField field, long newValue) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (field == ChronoField.OFFSET_SECONDS) {
            return this.withOffsetSameLocal(ZoneOffset.ofTotalSeconds(
                    (int) ChronoField.OFFSET_SECONDS.checkValidValue(newValue)));
        }
        if (field == ChronoField.INSTANT_SECONDS) {
            return OffsetDateTime.ofInstant(
                    Instant.ofEpochSecond(newValue, (long) this.getNano()), this.offset);
        }
        if (field instanceof ChronoField) {
            return this.resolveLocal(this.dateTime.with(field, newValue));
        }
        return (OffsetDateTime) field.adjustInto(this, newValue);
    }

    // ---- TemporalAccessor / Temporal -------------------------------------------------------------

    /**
     * The fields an offset date and time has: **all** of `ChronoField`'s.
     *
     * <p>And that is the right answer, not a simplification: having a date, a time and an offset,
     * there is enough to answer `INSTANT_SECONDS` --which is what a `LocalDateTime` alone cannot--
     * and the date ones and the time ones as well.
     *
     * <p>What was written was `field != INSTANT_SECONDS || true`, which is always true: the first
     * half does nothing. It gave the right answer by accident, and the `|| true` hid the intent --
     * anyone reading it would wonder which case had been meant to be excluded.
     */
    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return true;
        }
        return field != null && field.isSupportedBy(this);
    }

    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit != ChronoUnit.FOREVER;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS) {
            return this.offset.getTotalSeconds();
        }
        if (field == ChronoField.INSTANT_SECONDS) {
            return this.toEpochSecond();
        }
        return this.dateTime.getLong(field);
    }

    public int get(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS) {
            return this.offset.getTotalSeconds();
        }
        return this.dateTime.get(field);
    }

    public ValueRange range(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS || field == ChronoField.INSTANT_SECONDS) {
            return field.range();
        }
        return this.dateTime.range(field);
    }

    public <R> R query(java.time.temporal.TemporalQuery<R> query) {
        if (query == java.time.temporal.TemporalQueries.offset()
                || query == java.time.temporal.TemporalQueries.zone()) {
            return (R) this.offset;
        }
        if (query == java.time.temporal.TemporalQueries.localDate()) {
            return (R) this.toLocalDate();
        }
        if (query == java.time.temporal.TemporalQueries.localTime()) {
            return (R) this.toLocalTime();
        }
        if (query == java.time.temporal.TemporalQueries.precision()) {
            return (R) ChronoUnit.NANOS;
        }
        return query.queryFrom(this);
    }

    public Temporal adjustInto(Temporal temporal) {
        return temporal
                .with(ChronoField.EPOCH_DAY, this.toLocalDate().toEpochDay())
                .with(ChronoField.NANO_OF_DAY, this.toLocalTime().toNanoOfDay())
                .with(ChronoField.OFFSET_SECONDS, this.offset.getTotalSeconds());
    }

    /**
     * How many `unit` there are to `endExclusive`.
     *
     * <p>The other one is brought **to this offset** before counting, keeping its instant. Without
     * that, the difference between 15:00+02:00 and 15:00+00:00 would give zero hours when it is
     * two.
     */
    public long until(Temporal endExclusive, TemporalUnit unit) {
        OffsetDateTime end = OffsetDateTime.from(endExclusive);
        end = end.withOffsetSameInstant(this.offset);
        return this.dateTime.until(end.dateTime, unit);
    }

    // ---- conversions and comparison --------------------------------------------------------------

    /** This date and time in that zone, **keeping the instant**. */
    public ZonedDateTime atZoneSameInstant(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return ZonedDateTime.ofInstant(this.toInstant(), zone);
    }

    /**
     * This date and time in that zone, **keeping the date and time as written**.
     *
     * <p>It is another instant, and it can also fall into a daylight-saving gap or overlap -- there
     * the zone rules, not this object.
     */
    public ZonedDateTime atZoneSimilarLocal(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return ZonedDateTime.of(this.dateTime, zone);
    }

    /** This date and time as a `ZonedDateTime` with the offset as the zone. */
    public ZonedDateTime toZonedDateTime() {
        return ZonedDateTime.of(this.dateTime, this.offset);
    }

    /**
     * The order **by instant alone**, ignoring the offset.
     *
     * <p>It is the complement of `compareTo`, which breaks ties by local date and time. This one says
     * "the same moment is the same moment", and serves to order records from different offsets by
     * when they happened.
     *
     * <p>Beware of using it in a `TreeSet`: breaking no ties, two dates of the same instant and
     * different offset compare 0 and the set keeps only one.
     */
    public static java.util.Comparator<OffsetDateTime> timeLineOrder() {
        return new OdtTimeLine();
    }

    /** The time alone, with this offset. */
    public OffsetTime toOffsetTime() {
        return OffsetTime.of(this.toLocalTime(), this.offset);
    }

    /** The local date's epoch day. */
    public long toEpochDay() {
        return this.toLocalDate().toEpochDay();
    }

    /** Formateada. */
    public String format(java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        return formatter.format(this);
    }

    /** Whether they name the **same instant**, even if their written date and time differ. */
    public boolean isEqual(OffsetDateTime other) {
        return this.toEpochSecond() == other.toEpochSecond()
                && this.getNano() == other.getNano();
    }

    public boolean isBefore(OffsetDateTime other) {
        long a = this.toEpochSecond();
        long b = other.toEpochSecond();
        return a < b || (a == b && this.getNano() < other.getNano());
    }

    public boolean isAfter(OffsetDateTime other) {
        long a = this.toEpochSecond();
        long b = other.toEpochSecond();
        return a > b || (a == b && this.getNano() > other.getNano());
    }

    // Seconds from the epoch of 1970-01-01T00:00:00Z (the local date-time shifted by the offset).
    public long toEpochSecond() {
        long epochDay = this.dateTime.toLocalDate().toEpochDay();
        long secs = epochDay * 86400L + this.getHour() * 3600L + this.getMinute() * 60L + this.getSecond();
        return secs - this.offset.getTotalSeconds();
    }

    public Instant toInstant() {
        return Instant.ofEpochSecond(this.toEpochSecond(), this.getNano());
    }

    public String toString() {
        return this.dateTime.toString() + this.offset.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OffsetDateTime) {
            OffsetDateTime o = (OffsetDateTime) obj;
            return this.dateTime.equals(o.dateTime) && this.offset.equals(o.offset);
        }
        return false;
    }

    public int hashCode() {
        return this.dateTime.hashCode() ^ this.offset.hashCode();
    }

    /**
     * By instant, and at equal instants by local date and time.
     *
     * <p>The tie-break is not decoration: without it, 15:00+02:00 and 13:00+00:00 compare 0 without
     * being `equals`, and a `TreeSet` would silently keep only one of the two.
     */
    public int compareTo(OffsetDateTime other) {
        if (this.offset.equals(other.offset)) {
            return this.dateTime.compareTo(other.dateTime);
        }
        long a = this.toEpochSecond();
        long b = other.toEpochSecond();
        if (a < b) {
            return -1;
        }
        if (a > b) {
            return 1;
        }
        int na = this.getNano();
        int nb = other.getNano();
        if (na < nb) {
            return -1;
        }
        if (na > nb) {
            return 1;
        }
        return this.dateTime.compareTo(other.dateTime);
    }

    /**
     * It reads `text` with that formatter.
     *
     * <p>The one that decides which fields are there is the formatter; this class only says
     * **which of them it wants**, by passing its own `from`. That is why a pattern that brings no date, time and
     * offset fails here and not when the result is used.
     *
     * @throws java.time.format.DateTimeParseException if the text does not fit the pattern, or what
     *     fits is not enough for a date and time with an offset
     */
    public static OffsetDateTime parse(CharSequence text, java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        // Bound to a local: chaining through an interface-typed intermediate gets lost (#108).
        java.time.temporal.TemporalQuery<OffsetDateTime> queryOf = OffsetDateTime::from;
        return formatter.parse(text, queryOf);
    }
}

// The comparator `OffsetDateTime.timeLineOrder()` returns: the instant alone.
final class OdtTimeLine implements java.util.Comparator<OffsetDateTime> {

    public int compare(OffsetDateTime a, OffsetDateTime b) {
        int c = Long.compare(a.toEpochSecond(), b.toEpochSecond());
        return c != 0 ? c : Integer.compare(a.getNano(), b.getNano());
    }
}
