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
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRulesException;

// KajiLibrary's java.time.ZonedDateTime -- a date and time with a zone, kept as a
// `LocalDateTime`, the resolved `ZoneOffset`, and the `ZoneId`.
//
// **Only fixed-offset zones are accepted.** The regional ones --`America/Argentina/Buenos_Aires`--
// need the IANA database's transition rules, which is a wall of data and not of code: `ZoneId.of`
// rejects them. The visible consequence is that there is no daylight saving here, and therefore no
// gaps and no overlaps: both `*OffsetAtOverlap` return `this` and are documented as doing so.
//
// What does **not** change is the shape: the arithmetic re-resolves the offset against the zone on
// every operation, even though today it always gives the same one. The day there are real rules, the
// place they enter is `resolveLocal(...)` and `ZonedDateTime.of`, and nothing above finds out.
public final class ZonedDateTime
        implements Temporal, TemporalAdjuster, java.time.chrono.ChronoZonedDateTime, Serializable {

    private final LocalDateTime dateTime;
    private final ZoneOffset offset;
    private final ZoneId zone;

    private ZonedDateTime(LocalDateTime dateTime, ZoneOffset offset, ZoneId zone) {
        this.dateTime = dateTime;
        this.offset = offset;
        this.zone = zone;
    }

    // Fixed-offset only: the zone must itself be a ZoneOffset, otherwise tzdb rules would be needed.
    private static ZoneOffset resolveOffset(ZoneId zone) {
        if (zone instanceof ZoneOffset) {
            return (ZoneOffset) zone;
        }
        throw new ZoneRulesException(
            "Region-based zones require time-zone rules (tzdb), unsupported in KajiLibrary: " + zone.getId());
    }

    public static ZonedDateTime of(LocalDateTime dateTime, ZoneId zone) {
        ZoneOffset offset = resolveOffset(zone);
        return new ZonedDateTime(dateTime, offset, zone);
    }

    public static ZonedDateTime of(int year, int month, int dayOfMonth, int hour, int minute,
            int second, int nanoOfSecond, ZoneId zone) {
        LocalDateTime dateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond);
        return of(dateTime, zone);
    }

    public static ZonedDateTime ofInstant(Instant instant, ZoneId zone) {
        ZoneOffset offset = resolveOffset(zone);
        long localSecond = instant.getEpochSecond() + offset.getTotalSeconds();
        long epochDay = floorDiv(localSecond, 86400L);
        int secondOfDay = (int) (localSecond - epochDay * 86400L);
        LocalDate date = LocalDate.ofEpochDay(epochDay);
        LocalTime time = LocalTime.of(secondOfDay / 3600, (secondOfDay % 3600) / 60, secondOfDay % 60, instant.getNano());
        return new ZonedDateTime(LocalDateTime.of(date, time), offset, zone);
    }

    public static ZonedDateTime now() {
        return ofInstant(Instant.now(), ZoneOffset.UTC);
    }

    /** Now, in `zone`. */
    public static ZonedDateTime now(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return ofInstant(Instant.now(), zone);
    }

    /**
     * Now **according to `clock`**, and in the clock's zone.
     *
     * <p>The testable form: a `Clock.fixed` makes this always return the same thing, which is the
     * only way of writing a test over code that looks at the time.
     */
    public static ZonedDateTime now(Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        // Bound to locals: a call chained through an interface-typed intermediate gets lost (#108).
        Instant now = clock.instant();
        ZoneId zone = clock.getZone();
        return ofInstant(now, zone);
    }

    /** The date and the time separately. */
    public static ZonedDateTime of(LocalDate date, LocalTime time, ZoneId zone) {
        return of(LocalDateTime.of(date, time), zone);
    }

    /**
     * The instant `dateTime` names **read with `offset`**, seen from `zone`.
     *
     * <p>The first two arguments say *which instant it is*; the third, *how to show it*. If the
     * zone's offset is not the same, the result has a local date and time other than the ones passed
     * in, and that is right: the instant rules.
     */
    public static ZonedDateTime ofInstant(LocalDateTime dateTime, ZoneOffset offset, ZoneId zone) {
        if (dateTime == null) {
            throw new NullPointerException("dateTime");
        }
        if (offset == null) {
            throw new NullPointerException("offset");
        }
        return ofInstant(dateTime.toInstant(offset), zone);
    }

    /**
     * The local date and time in `zone`, with `preferredOffset` to break an overlap's tie.
     *
     * <p>There are no overlaps here --the zones are fixed-offset-- so the preferred one never gets to
     * decide anything and the offset comes from the zone. `null` is accepted, as in the JDK. See the
     * class's note.
     */
    public static ZonedDateTime ofLocal(LocalDateTime localDateTime, ZoneId zone, ZoneOffset preferredOffset) {
        return of(localDateTime, zone);
    }

    /**
     * All three, **demanding that they agree**: if `offset` is not a valid offset of `zone` for that
     * date and time, it throws instead of correcting.
     *
     * <p>It is the strict version of {@link #ofInstant(LocalDateTime, ZoneOffset, ZoneId)}, which
     * given the same thing keeps the instant and changes the local time. Which of the two is wanted
     * depends on whether the data comes from a source that is trusted.
     */
    public static ZonedDateTime ofStrict(LocalDateTime localDateTime, ZoneOffset offset, ZoneId zone) {
        if (localDateTime == null) {
            throw new NullPointerException("localDateTime");
        }
        if (offset == null) {
            throw new NullPointerException("offset");
        }
        ZoneOffset valid = resolveOffset(zone);
        if (!offset.equals(valid)) {
            throw new java.time.DateTimeException("ZoneOffset '" + offset
                    + "' is not valid for ZoneId '" + zone.getId() + "'");
        }
        return new ZonedDateTime(localDateTime, offset, zone);
    }

    /**
     * It parses the ISO form: `2007-12-03T10:15:30+01:00`, with an optional `[zone]` at the end.
     *
     * <p>The bracket exists because the offset is **not enough** to recover the zone: `+01:00` may be
     * Paris or Lagos, and they behave differently six months later. Only fixed-offset zones are
     * accepted here, so a `[Europe/Paris]` is rejected by `ZoneId.of` with its own message, which
     * says exactly what is missing.
     */
    public static ZonedDateTime parse(CharSequence text) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        String s = text.toString();
        String zoneBetween = null;
        int open = s.indexOf('[');
        if (open >= 0) {
            if (s.charAt(s.length() - 1) != ']') {
                throw new java.time.format.DateTimeParseException(
                        "Text '" + s + "' could not be parsed: unclosed zone region", text, open);
            }
            zoneBetween = s.substring(open + 1, s.length() - 1);
            s = s.substring(0, open);
        }
        OffsetDateTime odt = OffsetDateTime.parse(s);
        ZoneOffset off = odt.getOffset();
        ZoneId zone = zoneBetween == null ? off : ZoneId.of(zoneBetween);
        return ofStrict(odt.toLocalDateTime(), off, zone);
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

    public ZoneOffset getOffset() {
        return this.offset;
    }

    public ZoneId getZone() {
        return this.zone;
    }

    public long toEpochSecond() {
        long epochDay = this.dateTime.toLocalDate().toEpochDay();
        long secondOfDay = this.dateTime.getHour() * 3600L + this.dateTime.getMinute() * 60L + this.dateTime.getSecond();
        return epochDay * 86400L + secondOfDay - this.offset.getTotalSeconds();
    }

    public Instant toInstant() {
        return Instant.ofEpochSecond(this.toEpochSecond(), this.dateTime.getNano());
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

    // ---- the two `withZone`, and the two of the overlap ------------------------------------------
    //
    // Four methods that exist for a single reason: **a local date and time does not always name a
    // unique instant**. When the clock goes back at the end of daylight saving, the hour that repeats
    // happens twice; when it goes forward, there is an hour that does not happen.
    //
    // In this library the zones are **fixed**-offset --the regional ones need the IANA database-- so
    // there are no overlaps and no gaps, and both `*OffsetAtOverlap` return `this`. They are here all
    // the same, and with this note, because the signature is part of the contract and because the day
    // there are real rules this is where they are implemented.

    /** Another zone, **the same date and time as written**. It is another instant. */
    public ZonedDateTime withZoneSameLocal(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return zone.equals(this.zone) ? this : ZonedDateTime.of(this.dateTime, zone);
    }

    /** Another zone, **the same instant**: the date and time are corrected. */
    public ZonedDateTime withZoneSameInstant(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        return zone.equals(this.zone) ? this : ZonedDateTime.ofInstant(this.toInstant(), zone);
    }

    /**
     * This same date and time with the zone reduced to its offset.
     *
     * <p>It serves to freeze the moment: a `ZonedDateTime` of a zone with rules can change offset if
     * the rules change, and this one cannot.
     */
    public ZonedDateTime withFixedOffsetZone() {
        return this.zone.equals(this.offset) ? this : ZonedDateTime.of(this.dateTime, this.offset);
    }

    /**
     * In an overlap, the **first** of the two possible instants.
     *
     * <p>It returns `this`: this library's zones are fixed-offset, so there are no overlaps. See the
     * note above.
     */
    public ZonedDateTime withEarlierOffsetAtOverlap() {
        return this;
    }

    /** The **second**. See the note above. */
    public ZonedDateTime withLaterOffsetAtOverlap() {
        return this;
    }

    // ---- the per-field `with*` -----------------------------------------------------------------

    public ZonedDateTime withYear(int year) {
        return this.resolveLocal(this.dateTime.withYear(year));
    }

    public ZonedDateTime withMonth(int month) {
        return this.resolveLocal(this.dateTime.withMonth(month));
    }

    public ZonedDateTime withDayOfMonth(int dayOfMonth) {
        return this.resolveLocal(this.dateTime.withDayOfMonth(dayOfMonth));
    }

    public ZonedDateTime withDayOfYear(int dayOfYear) {
        return this.resolveLocal(this.dateTime.withDayOfYear(dayOfYear));
    }

    public ZonedDateTime withHour(int hour) {
        return this.resolveLocal(this.dateTime.withHour(hour));
    }

    public ZonedDateTime withMinute(int minute) {
        return this.resolveLocal(this.dateTime.withMinute(minute));
    }

    public ZonedDateTime withSecond(int second) {
        return this.resolveLocal(this.dateTime.withSecond(second));
    }

    public ZonedDateTime withNano(int nanoOfSecond) {
        return this.resolveLocal(this.dateTime.withNano(nanoOfSecond));
    }

    public ZonedDateTime truncatedTo(TemporalUnit unit) {
        return this.resolveLocal(this.dateTime.truncatedTo(unit));
    }

    // It remakes the object with another local date and time, **re-resolving** the offset against
    // the zone. With fixed zones it gives the same one; with real rules this is where daylight saving
    // enters.
    private ZonedDateTime resolveLocal(LocalDateTime newOne) {
        return newOne.equals(this.dateTime) ? this : ZonedDateTime.of(newOne, this.zone);
    }

    public ZonedDateTime plusNanos(long nanos) {
        return this.resolveLocal(this.dateTime.plusNanos(nanos));
    }

    public ZonedDateTime minusNanos(long nanos) {
        return this.plusNanos(-nanos);
    }

    public ZonedDateTime plus(long amountToAdd, TemporalUnit unit) {
        return this.resolveLocal(this.dateTime.plus(amountToAdd, unit));
    }

    public ZonedDateTime minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    public ZonedDateTime plus(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        return (ZonedDateTime) amount.addTo(this);
    }

    public ZonedDateTime minus(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        return (ZonedDateTime) amount.subtractFrom(this);
    }

    public ZonedDateTime with(TemporalAdjuster adjuster) {
        if (adjuster == null) {
            throw new NullPointerException("adjuster");
        }
        if (adjuster instanceof LocalDateTime) {
            return this.resolveLocal((LocalDateTime) adjuster);
        }
        if (adjuster instanceof LocalDate) {
            return this.resolveLocal(LocalDateTime.of((LocalDate) adjuster, this.dateTime.toLocalTime()));
        }
        if (adjuster instanceof LocalTime) {
            return this.resolveLocal(LocalDateTime.of(this.dateTime.toLocalDate(), (LocalTime) adjuster));
        }
        if (adjuster instanceof ZonedDateTime) {
            return (ZonedDateTime) adjuster;
        }
        return (ZonedDateTime) adjuster.adjustInto(this);
    }

    public ZonedDateTime with(TemporalField field, long newValue) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (field == ChronoField.OFFSET_SECONDS) {
            // Changing a fixed zone's offset is changing the zone.
            return ZonedDateTime.of(this.dateTime, ZoneOffset.ofTotalSeconds(
                    (int) ChronoField.OFFSET_SECONDS.checkValidValue(newValue)));
        }
        if (field == ChronoField.INSTANT_SECONDS) {
            return ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(newValue, (long) this.getNano()), this.zone);
        }
        if (field instanceof ChronoField) {
            return this.resolveLocal(this.dateTime.with(field, newValue));
        }
        return (ZonedDateTime) field.adjustInto(this, newValue);
    }

    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit != ChronoUnit.FOREVER;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    public ValueRange range(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS || field == ChronoField.INSTANT_SECONDS) {
            return field.range();
        }
        return this.dateTime.range(field);
    }

    public <R> R query(java.time.temporal.TemporalQuery<R> query) {
        if (query == java.time.temporal.TemporalQueries.zoneId()
                || query == java.time.temporal.TemporalQueries.zone()) {
            return (R) this.zone;
        }
        if (query == java.time.temporal.TemporalQueries.offset()) {
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

    /** How many `unit` there are to `endExclusive`, bringing it into **this** zone first. */
    public long until(Temporal endExclusive, TemporalUnit unit) {
        ZonedDateTime end = ZonedDateTime.from(endExclusive);
        end = end.withZoneSameInstant(this.zone);
        return this.dateTime.until(end.dateTime, unit);
    }

    /** The zoned date and time `temporal` holds. */
    public static ZonedDateTime from(TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof ZonedDateTime) {
            return (ZonedDateTime) temporal;
        }
        ZoneId z = temporal.query(java.time.temporal.TemporalQueries.zone());
        if (z == null) {
            throw new java.time.DateTimeException(
                    "Unable to obtain ZonedDateTime from TemporalAccessor: " + temporal);
        }
        if (temporal.isSupported(ChronoField.INSTANT_SECONDS)) {
            return ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(temporal.getLong(ChronoField.INSTANT_SECONDS),
                            temporal.getLong(ChronoField.NANO_OF_SECOND)), z);
        }
        return ZonedDateTime.of(LocalDateTime.from(temporal), z);
    }

    public OffsetDateTime toOffsetDateTime() {
        return OffsetDateTime.of(this.dateTime, this.offset);
    }

    public ZonedDateTime plusYears(long years) {
        return new ZonedDateTime(this.dateTime.plusYears(years), this.offset, this.zone);
    }

    public ZonedDateTime plusMonths(long months) {
        return new ZonedDateTime(this.dateTime.plusMonths(months), this.offset, this.zone);
    }

    public ZonedDateTime plusWeeks(long weeks) {
        return new ZonedDateTime(this.dateTime.plusWeeks(weeks), this.offset, this.zone);
    }

    public ZonedDateTime plusDays(long days) {
        return new ZonedDateTime(this.dateTime.plusDays(days), this.offset, this.zone);
    }

    public ZonedDateTime plusHours(long hours) {
        return new ZonedDateTime(this.dateTime.plusHours(hours), this.offset, this.zone);
    }

    public ZonedDateTime plusMinutes(long minutes) {
        return new ZonedDateTime(this.dateTime.plusMinutes(minutes), this.offset, this.zone);
    }

    public ZonedDateTime plusSeconds(long seconds) {
        return new ZonedDateTime(this.dateTime.plusSeconds(seconds), this.offset, this.zone);
    }

    public ZonedDateTime minusYears(long years) {
        return this.plusYears(-years);
    }

    public ZonedDateTime minusMonths(long months) {
        return this.plusMonths(-months);
    }

    public ZonedDateTime minusWeeks(long weeks) {
        return this.plusWeeks(-weeks);
    }

    public ZonedDateTime minusDays(long days) {
        return this.plusDays(-days);
    }

    public ZonedDateTime minusHours(long hours) {
        return this.plusHours(-hours);
    }

    public ZonedDateTime minusMinutes(long minutes) {
        return this.plusMinutes(-minutes);
    }

    public ZonedDateTime minusSeconds(long seconds) {
        return this.plusSeconds(-seconds);
    }

    // --- TemporalAccessor (field access delegates to the local date-time) ---

    // The two fields that **belong to the zone and not to the date**: the offset, and the epoch
    // second. The three methods used to delegate entirely to the `LocalDateTime`, which does not have
    // them, so asking for them threw. The symptom was not obvious: `formatter.format(zdt)` with a
    // pattern carrying an `X` failed with a message-less `IllegalArgumentException`, and from outside
    // it looked like a problem of the formatter's. `FmtTest` found it, formatting a zoned date.
    public boolean isSupported(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS || field == ChronoField.INSTANT_SECONDS) {
            return true;
        }
        return this.dateTime.isSupported(field);
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.OFFSET_SECONDS) {
            return (long) this.offset.getTotalSeconds();
        }
        if (field == ChronoField.INSTANT_SECONDS) {
            return this.toEpochSecond();
        }
        return this.dateTime.getLong(field);
    }

    public int get(TemporalField field) {
        if (field == ChronoField.INSTANT_SECONDS) {
            // It does not fit in an `int` and truncating it would give a plausible and wrong
            // number, which is the worst thing that can happen here. The JDK throws, with the same
            // message.
            throw new java.time.temporal.UnsupportedTemporalTypeException(
                    "Invalid field 'InstantSeconds' for get() method, use getLong() instead");
        }
        return (int) this.getLong(field);
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    // ISO-8601: local date-time + offset, with "[zoneId]" appended only for a non-offset (region)
    // zone. In the fixed-offset subset the zone is always the offset, so the bracket is never added.
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.dateTime.toString());
        buf.append(this.offset.getId());
        if (this.zone != this.offset) {
            buf.append("[");
            buf.append(this.zone.getId());
            buf.append("]");
        }
        return buf.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ZonedDateTime) {
            ZonedDateTime other = (ZonedDateTime) obj;
            return this.dateTime.equals(other.dateTime) && this.offset.equals(other.offset)
                && this.zone.equals(other.zone);
        }
        return false;
    }

    public int hashCode() {
        int z = this.zone.hashCode();
        int zRot = (z << 3) | (z >>> 29);
        return this.dateTime.hashCode() ^ this.offset.hashCode() ^ zRot;
    }

    private static long floorDiv(long a, long b) {
        long q = a / b;
        if ((a % b != 0) && ((a ^ b) < 0)) {
            q = q - 1;
        }
        return q;
    }

    /**
     * It reads `text` with that formatter.
     *
     * <p>The one that decides which fields are there is the formatter; this class only says
     * **which of them it wants**, by passing its own `from`. That is why a pattern that brings no date, time and zone
     * fails here and not when the result is used.
     *
     * @throws java.time.format.DateTimeParseException if the text does not fit the pattern, or what
     *     fits is not enough for a zoned date and time
     */
    public static ZonedDateTime parse(CharSequence text, java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        // Bound to a local: chaining through an interface-typed intermediate gets lost (#108).
        java.time.temporal.TemporalQuery<ZonedDateTime> queryOf = ZonedDateTime::from;
        return formatter.parse(text, queryOf);
    }
}
