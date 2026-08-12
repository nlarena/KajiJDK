package java.time;

// KajiLibrary's java.time.OffsetDateTime — a date-time with a fixed offset from UTC, e.g.
// 2026-08-04T15:30+05:30. A value type composing a LocalDateTime and a ZoneOffset. A KajiLibrary
// subset (no zone/formatter methods; comparison is by the resulting instant).
public final class OffsetDateTime implements Comparable<OffsetDateTime> {

    private final LocalDateTime dateTime;
    private final ZoneOffset offset;

    private OffsetDateTime(LocalDateTime dateTime, ZoneOffset offset) {
        this.dateTime = dateTime;
        this.offset = offset;
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

    // Ordered by the resulting instant, then by nanos (a subset of the JDK's full ordering).
    public int compareTo(OffsetDateTime other) {
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
        return 0;
    }
}
