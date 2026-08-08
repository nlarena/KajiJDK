package java.time;

// KajiLibrary's java.time.OffsetTime — a time with a fixed offset from UTC, e.g. 15:30+05:00.
// A value type composing a LocalTime and a ZoneOffset. A KajiLibrary subset (no zone/formatter
// methods; comparison is by the UTC-equivalent instant).
public final class OffsetTime implements Comparable<OffsetTime> {

    private final LocalTime time;
    private final ZoneOffset offset;

    private OffsetTime(LocalTime time, ZoneOffset offset) {
        this.time = time;
        this.offset = offset;
    }

    public static OffsetTime of(LocalTime time, ZoneOffset offset) {
        return new OffsetTime(time, offset);
    }

    public static OffsetTime of(int hour, int minute, int second, int nanoOfSecond, ZoneOffset offset) {
        return new OffsetTime(LocalTime.of(hour, minute, second, nanoOfSecond), offset);
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

    public int compareTo(OffsetTime other) {
        long a = this.toEpochNanoUtc();
        long b = other.toEpochNanoUtc();
        if (a < b) {
            return -1;
        }
        if (a > b) {
            return 1;
        }
        return 0;
    }

    private long toEpochNanoUtc() {
        long nod = this.getHour() * 3600000000000L + this.getMinute() * 60000000000L
            + this.getSecond() * 1000000000L + this.getNano();
        return nod - this.offset.getTotalSeconds() * 1000000000L;
    }
}
