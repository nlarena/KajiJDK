package java.time;

import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRulesException;

// KajiLibrary's java.time.ZonedDateTime — a date-time with a time-zone, stored as a LocalDateTime, the
// resolved ZoneOffset, and the ZoneId. A KajiLibrary subset: only fixed-offset zones are supported
// (the zone is always a ZoneOffset, so the offset never changes across a plus/minus). Region-based
// zones need IANA tzdb transition rules — a data wall — and are rejected by ZoneId.of. Implements
// TemporalAccessor (so DateTimeFormatter.format works) and Comparable, ordered by instant.
public final class ZonedDateTime implements TemporalAccessor {

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

    public boolean isSupported(TemporalField field) {
        return this.dateTime.isSupported(field);
    }

    public long getLong(TemporalField field) {
        return this.dateTime.getLong(field);
    }

    public int get(TemporalField field) {
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
}
