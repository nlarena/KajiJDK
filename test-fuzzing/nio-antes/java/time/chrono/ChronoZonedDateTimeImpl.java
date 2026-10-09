package java.time.chrono;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ValueRange;

// KajiLibrary's java.time.chrono.ChronoZonedDateTimeImpl -- a date and time **with a zone** in a
// calendar that is not ISO: a `ChronoLocalDateTime` plus the zone and the resolved offset.
//
// The same limitation as in `java.time.ZonedDateTime` holds: **fixed-offset zones only**. The
// regional ones need the IANA database's rules, and `ZoneId.of` already rejects them. The
// consequence is that there are no gaps and no overlaps, and both `*OffsetAtOverlap` return `this`.
//
// It is package-private, as in the JDK: one gets in through `atZone` or through
// `Chronology.zonedDateTime(...)`.
final class ChronoZonedDateTimeImpl implements ChronoZonedDateTime {

    private final ChronoLocalDateTime dateTime;
    private final ZoneOffset offset;
    private final ZoneId zone;

    private ChronoZonedDateTimeImpl(ChronoLocalDateTime dateTime, ZoneOffset offset, ZoneId zone) {
        this.dateTime = dateTime;
        this.offset = offset;
        this.zone = zone;
    }

    // A zone with no rules has nothing to give an offset with: it is rejected here and not further
    // on, where the error would no longer say where it came from.
    private static ZoneOffset resolveWith(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        if (zone instanceof ZoneOffset) {
            return (ZoneOffset) zone;
        }
        throw new java.time.zone.ZoneRulesException(
                "Region-based zones require time-zone rules (tzdb), unsupported in KajiLibrary: "
                        + zone.getId());
    }

    static ChronoZonedDateTime of(ChronoLocalDateTime dateTime, ZoneId zone) {
        if (dateTime == null) {
            throw new NullPointerException("dateTime");
        }
        ZoneOffset offset = resolveWith(zone);
        if (dateTime instanceof java.time.LocalDateTime) {
            // ISO has a class of its own, which knows more than this one.
            return java.time.ZonedDateTime.of((java.time.LocalDateTime) dateTime, zone);
        }
        return new ChronoZonedDateTimeImpl(dateTime, offset, zone);
    }

    /** The instant `instant` seen from `zone`, in `chrono`'s calendar. */
    static ChronoZonedDateTime ofInstant(Chronology chrono, Instant instant, ZoneId zone) {
        ZoneOffset offset = resolveWith(zone);
        long localSecond = instant.getEpochSecond() + (long) offset.getTotalSeconds();
        long epochDay = Math.floorDiv(localSecond, 86400L);
        int secondOfDay = (int) Math.floorMod(localSecond, 86400L);
        ChronoLocalDate date = chrono.dateEpochDay(epochDay);
        LocalTime time = LocalTime.ofNanoOfDay(secondOfDay * 1000000000L + (long) instant.getNano());
        return of(ChronoLocalDateTimeImpl.of(date, time), zone);
    }

    public ChronoLocalDateTime toLocalDateTime() {
        return this.dateTime;
    }

    public ZoneOffset getOffset() {
        return this.offset;
    }

    public ZoneId getZone() {
        return this.zone;
    }

    public LocalTime toLocalTime() {
        return this.dateTime.toLocalTime();
    }

    public long toEpochSecond() {
        return this.dateTime.toEpochSecond(this.offset);
    }

    private ChronoZonedDateTime resolveLocal(ChronoLocalDateTime newOne) {
        if (newOne == this.dateTime) {
            return this;
        }
        return of(newOne, this.zone);
    }

    public ChronoZonedDateTime withEarlierOffsetAtOverlap() {
        return this;
    }

    public ChronoZonedDateTime withLaterOffsetAtOverlap() {
        return this;
    }

    /** Another zone, the same date and time as written. It is another instant. */
    public ChronoZonedDateTime withZoneSameLocal(ZoneId zone) {
        return of(this.dateTime, zone);
    }

    /** Another zone, the same instant: the date and the time are corrected. */
    public ChronoZonedDateTime withZoneSameInstant(ZoneId zone) {
        if (zone.equals(this.zone)) {
            return this;
        }
        ChronoLocalDate date = this.dateTime.toLocalDate();
        Chronology chrono = date.getChronology();
        return ofInstant(chrono, this.toInstant(), zone);
    }

    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return true;
        }
        return field != null && field.isSupportedBy(this);
    }

    public ValueRange range(TemporalField field) {
        if (field == ChronoField.INSTANT_SECONDS || field == ChronoField.OFFSET_SECONDS) {
            return field.range();
        }
        return this.dateTime.range(field);
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.INSTANT_SECONDS) {
            return this.toEpochSecond();
        }
        if (field == ChronoField.OFFSET_SECONDS) {
            return (long) this.offset.getTotalSeconds();
        }
        return this.dateTime.getLong(field);
    }

    public int get(TemporalField field) {
        if (field == ChronoField.INSTANT_SECONDS) {
            throw new java.time.temporal.UnsupportedTemporalTypeException(
                    "Invalid field 'InstantSeconds' for get() method, use getLong() instead");
        }
        if (field == ChronoField.OFFSET_SECONDS) {
            return this.offset.getTotalSeconds();
        }
        return this.dateTime.get(field);
    }

    public ChronoZonedDateTime with(TemporalField field, long newValue) {
        if (field == ChronoField.INSTANT_SECONDS) {
            ChronoLocalDate date = this.dateTime.toLocalDate();
            Chronology chrono = date.getChronology();
            LocalTime time = this.dateTime.toLocalTime();
            return ofInstant(chrono, Instant.ofEpochSecond(newValue, (long) time.getNano()), this.zone);
        }
        if (field == ChronoField.OFFSET_SECONDS) {
            // With fixed-offset zones, changing the offset is changing the zone.
            long valid = ChronoField.OFFSET_SECONDS.checkValidValue(newValue);
            return of(this.dateTime, ZoneOffset.ofTotalSeconds((int) valid));
        }
        return this.resolveLocal(this.dateTime.with(field, newValue));
    }

    public ChronoZonedDateTime plus(long amountToAdd, TemporalUnit unit) {
        return this.resolveLocal(this.dateTime.plus(amountToAdd, unit));
    }

    public ChronoZonedDateTime with(TemporalAdjuster adjuster) {
        if (adjuster instanceof ChronoZonedDateTime) {
            return (ChronoZonedDateTime) adjuster;
        }
        return this.resolveLocal(this.dateTime.with(adjuster));
    }

    /** How many `unit` there are to `endExclusive`, bringing it into **this** zone first. */
    public long until(Temporal endExclusive, TemporalUnit unit) {
        ChronoZonedDateTime end = (ChronoZonedDateTime) endExclusive;
        ChronoZonedDateTime inMyZone = end.withZoneSameInstant(this.zone);
        ChronoLocalDateTime local = inMyZone.toLocalDateTime();
        return this.dateTime.until(local, unit);
    }

    public boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit != ChronoUnit.FOREVER;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    public <R> R query(TemporalQuery<R> query) {
        if (query == java.time.temporal.TemporalQueries.zoneId()
                || query == java.time.temporal.TemporalQueries.zone()) {
            return (R) this.zone;
        }
        if (query == java.time.temporal.TemporalQueries.offset()) {
            return (R) this.offset;
        }
        if (query == java.time.temporal.TemporalQueries.localTime()) {
            return (R) this.dateTime.toLocalTime();
        }
        if (query == java.time.temporal.TemporalQueries.chronology()) {
            ChronoLocalDate date = this.dateTime.toLocalDate();
            return (R) date.getChronology();
        }
        if (query == java.time.temporal.TemporalQueries.precision()) {
            return (R) ChronoUnit.NANOS;
        }
        return query.queryFrom(this);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ChronoZonedDateTime) {
            ChronoZonedDateTime other = (ChronoZonedDateTime) obj;
            ChronoLocalDateTime theirLocal = other.toLocalDateTime();
            ZoneId theirZone = other.getZone();
            return this.dateTime.equals(theirLocal) && this.zone.equals(theirZone);
        }
        return false;
    }

    public int hashCode() {
        return this.dateTime.hashCode() ^ this.zone.hashCode();
    }

    public String toString() {
        String s = this.dateTime.toString() + this.offset.toString();
        if (!this.zone.equals(this.offset)) {
            s = s + "[" + this.zone.getId() + "]";
        }
        return s;
    }
}
