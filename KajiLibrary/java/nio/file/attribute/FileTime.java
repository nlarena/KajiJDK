package java.nio.file.attribute;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

// A file timestamp: an instant plus the granularity it was measured at.
//
// **It is pure value.** It does not touch the disk and needs no native. This note used to add that
// what was missing was **something to produce it** -- no native for a file's modification date, so
// `Files.getLastModifiedTime` did not exist. `Fs.mtime` exists now and that method is declared, so
// the type is produced from the filesystem as well as from a ZIP entry's own dates, which is where
// `java.util.zip` gets them.
//
// **Why `(value, unit)` is stored and not nanoseconds.** A `long` of nanos covers only ~292 years
// around 1970, and `FileTime.from(Long.MAX_VALUE, DAYS)` has to go on working. By storing the unit,
// the representable range is that of the coarsest unit used, and the conversion saturates --to
// `Long.MIN_VALUE`/`Long.MAX_VALUE`-- only when it really does not fit.
//
// **A `FileTime` built from an `Instant` has no unit.** The `Instant` is stored as it is and `unit`
// is left `null`; the `value` field is unused in that case. It is why almost every method has two
// branches.
public final class FileTime implements Comparable<FileTime> {

    private static final long HOURS_PER_DAY = 24L;
    private static final long MINUTES_PER_HOUR = 60L;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
    private static final long SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long MICROS_PER_SECOND = 1000000L;
    private static final long NANOS_PER_SECOND = 1000000000L;
    private static final int NANOS_PER_MILLI = 1000000;
    private static final int NANOS_PER_MICRO = 1000;

    // `Instant`'s bounds, in seconds. Outside them `toInstant()` saturates instead of throwing.
    private static final long MIN_SECOND = -31557014167219200L;
    private static final long MAX_SECOND = 31556889864403199L;

    // A 400-year cycle has 146097 days; a 10000-year one, 25 of those.
    private static final long SECONDS_PER_10000_YEARS = 146097L * 25L * 86400L;
    private static final long SECONDS_0000_TO_1970 = ((146097L * 5L) - (30L * 365L + 7L)) * 86400L;

    /** The granularity; `null` if it was built from an `Instant`. */
    private final TimeUnit unit;

    /** The value since the epoch, in `unit`. It can be negative. Meaningless if `unit` is null. */
    private final long value;

    // Memoised: `toInstant()` and `toString()` are pure, and both are called repeatedly when sorting
    // or formatting a list of files. There is no race that matters -- the worst case is two threads
    // computing the same thing and one overwriting the other with an identical value.
    private Instant instant;
    private String text;

    private FileTime(long value, TimeUnit unit, Instant instant) {
        this.value = value;
        this.unit = unit;
        this.instant = instant;
    }

    /**
     * A `FileTime` at `value` units since the epoch.
     *
     * @param value the value since 1970-01-01T00:00:00Z; it can be negative
     * @param unit how to read `value`
     */
    public static FileTime from(long value, TimeUnit unit) {
        Objects.requireNonNull(unit, "unit");
        return new FileTime(value, unit, null);
    }

    /** A `FileTime` from milliseconds since the epoch. */
    public static FileTime fromMillis(long value) {
        return new FileTime(value, TimeUnit.MILLISECONDS, null);
    }

    /** A `FileTime` at the same point on the time line as `instant`. */
    public static FileTime from(Instant instant) {
        Objects.requireNonNull(instant, "instant");
        return new FileTime(0, null, instant);
    }

    /**
     * The value at the granularity asked for.
     *
     * <p>If it does not fit in a `long` it **saturates** instead of flipping the sign: going from
     * days down to nanoseconds multiplies by 86400000000000, and a silent overflow would turn a date
     * far in the future into one in the past.
     */
    public long to(TimeUnit unit) {
        Objects.requireNonNull(unit, "unit");
        if (this.unit != null) {
            return unit.convert(this.value, this.unit);
        }
        long secs = unit.convert(this.instant.getEpochSecond(), TimeUnit.SECONDS);
        if (secs == Long.MIN_VALUE || secs == Long.MAX_VALUE) {
            return secs;
        }
        long nanos = unit.convert(this.instant.getNano(), TimeUnit.NANOSECONDS);
        long r = secs + nanos;
        // The sum overflowed if both addends have the sign opposite to the result.
        if (((secs ^ r) & (nanos ^ r)) < 0) {
            return (secs < 0) ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return r;
    }

    /** The value in milliseconds, saturating as `to` does. */
    public long toMillis() {
        if (this.unit != null) {
            return this.unit.toMillis(this.value);
        }
        long secs = this.instant.getEpochSecond();
        int nanos = this.instant.getNano();
        long r = secs * 1000;
        long ax = Math.abs(secs);
        if (((ax | 1000) >>> 31) != 0) {
            if ((r / 1000) != secs) {
                return (secs < 0) ? Long.MIN_VALUE : Long.MAX_VALUE;
            }
        }
        return r + nanos / 1000000;
    }

    // It multiplies `d` by `m` saturating: above `cap` the product does not fit in a long.
    private static long scale(long d, long m, long cap) {
        if (d > cap) {
            return Long.MAX_VALUE;
        }
        if (d < -cap) {
            return Long.MIN_VALUE;
        }
        return d * m;
    }

    /**
     * The same point on the time line, as an `Instant`.
     *
     * <p>`FileTime` reaches further than `Instant` in both directions, so whatever falls outside
     * saturates at `Instant.MIN` or `Instant.MAX`.
     */
    public Instant toInstant() {
        Instant i = this.instant;
        if (i != null) {
            return i;
        }
        long secs = 0L;
        int nanos = 0;
        TimeUnit u = this.unit;
        if (u == TimeUnit.DAYS) {
            secs = scale(this.value, SECONDS_PER_DAY, Long.MAX_VALUE / SECONDS_PER_DAY);
        } else if (u == TimeUnit.HOURS) {
            secs = scale(this.value, SECONDS_PER_HOUR, Long.MAX_VALUE / SECONDS_PER_HOUR);
        } else if (u == TimeUnit.MINUTES) {
            secs = scale(this.value, SECONDS_PER_MINUTE, Long.MAX_VALUE / SECONDS_PER_MINUTE);
        } else if (u == TimeUnit.SECONDS) {
            secs = this.value;
        } else if (u == TimeUnit.MILLISECONDS) {
            secs = Math.floorDiv(this.value, MILLIS_PER_SECOND);
            nanos = ((int) Math.floorMod(this.value, MILLIS_PER_SECOND)) * NANOS_PER_MILLI;
        } else if (u == TimeUnit.MICROSECONDS) {
            secs = Math.floorDiv(this.value, MICROS_PER_SECOND);
            nanos = ((int) Math.floorMod(this.value, MICROS_PER_SECOND)) * NANOS_PER_MICRO;
        } else {
            secs = Math.floorDiv(this.value, NANOS_PER_SECOND);
            nanos = (int) Math.floorMod(this.value, NANOS_PER_SECOND);
        }
        if (secs <= MIN_SECOND) {
            i = Instant.MIN;
        } else if (secs >= MAX_SECOND) {
            i = Instant.MAX;
        } else {
            i = Instant.ofEpochSecond(secs, nanos);
        }
        this.instant = i;
        return i;
    }

    /** Equal if they stand for the same moment, even if the units differ. */
    public boolean equals(Object obj) {
        return (obj instanceof FileTime) && this.compareTo((FileTime) obj) == 0;
    }

    /**
     * The equivalent `Instant`'s hash.
     *
     * <p>It has to come from there and not from `(value, unit)`: `from(1, SECONDS)` and
     * `fromMillis(1000)` are equal by `equals`, so they must agree on the hash.
     */
    public int hashCode() {
        return this.toInstant().hashCode();
    }

    private long asDays() {
        if (this.unit != null) {
            return this.unit.toDays(this.value);
        }
        return TimeUnit.SECONDS.toDays(this.toInstant().getEpochSecond());
    }

    private long leftoverNanos(long days) {
        if (this.unit != null) {
            return this.unit.toNanos(this.value - this.unit.convert(days, TimeUnit.DAYS));
        }
        return TimeUnit.SECONDS.toNanos(
                this.toInstant().getEpochSecond() - TimeUnit.DAYS.toSeconds(days));
    }

    /**
     * Chronological order.
     *
     * <p>With the same unit comparing the values is enough. With different units one has to go
     * through `Instant`, and there the odd case that justifies the last branch turns up: two **very**
     * distant moments both saturate to the same `Instant.MAX` and would look equal. When the seconds
     * land exactly on the bound the comparison is redone in days and nanos of the day, which do not
     * saturate.
     */
    public int compareTo(FileTime other) {
        if (this.unit != null && this.unit == other.unit) {
            return Long.compare(this.value, other.value);
        }
        long secs = this.toInstant().getEpochSecond();
        long otherSecs = other.toInstant().getEpochSecond();
        int cmp = Long.compare(secs, otherSecs);
        if (cmp != 0) {
            return cmp;
        }
        cmp = Long.compare(this.toInstant().getNano(), other.toInstant().getNano());
        if (cmp != 0) {
            return cmp;
        }
        if (secs != MAX_SECOND && secs != MIN_SECOND) {
            return 0;
        }
        long days = this.asDays();
        long otherDays = other.asDays();
        if (days == otherDays) {
            return Long.compare(this.leftoverNanos(days), other.leftoverNanos(otherDays));
        }
        return Long.compare(days, otherDays);
    }

    // It writes `d` with `width` digits and leading zeros; `width` arrives as a power of ten.
    private static StringBuilder pad(StringBuilder sb, int width, int d) {
        int w = width;
        int v = d;
        while (w > 0) {
            sb.append((char) (v / w + '0'));
            v = v % w;
            w = w / 10;
        }
        return sb;
    }

    /**
     * The date in ISO 8601: `YYYY-MM-DDThh:mm:ss[.s+]Z`, always in UTC.
     *
     * <p>The fraction of a second appears only if it is not zero, and with no trailing zeros:
     * `fromMillis(1234567890000L)` gives `"2009-02-13T23:31:30Z"` and not `"...30.000Z"`.
     *
     * <p>For years outside `0001..9999` --which `FileTime` can represent and ISO 8601 cannot-- XML
     * Schema's deviation is followed: more than four digits, no leading zeros, and a minus sign for
     * the earlier dates. The `hi`/`lo` arithmetic is there for that: it splits the seconds into
     * 10000-year cycles **before** handing them to `LocalDateTime`, which only covers the small
     * range.
     */
    public String toString() {
        String s = this.text;
        if (s != null) {
            return s;
        }
        long secs;
        int nanos = 0;
        if (this.instant == null && this.unit.compareTo(TimeUnit.SECONDS) >= 0) {
            secs = this.unit.toSeconds(this.value);
        } else {
            secs = this.toInstant().getEpochSecond();
            nanos = this.toInstant().getNano();
        }
        LocalDateTime ldt;
        int year;
        if (secs >= -SECONDS_0000_TO_1970) {
            long zeroSecs = secs - SECONDS_PER_10000_YEARS + SECONDS_0000_TO_1970;
            long hi = Math.floorDiv(zeroSecs, SECONDS_PER_10000_YEARS) + 1;
            long lo = Math.floorMod(zeroSecs, SECONDS_PER_10000_YEARS);
            ldt = LocalDateTime.ofEpochSecond(lo - SECONDS_0000_TO_1970, nanos, ZoneOffset.UTC);
            year = ldt.getYear() + ((int) hi) * 10000;
        } else {
            long zeroSecs = secs + SECONDS_0000_TO_1970;
            long hi = zeroSecs / SECONDS_PER_10000_YEARS;
            long lo = zeroSecs % SECONDS_PER_10000_YEARS;
            ldt = LocalDateTime.ofEpochSecond(lo - SECONDS_0000_TO_1970, nanos, ZoneOffset.UTC);
            year = ldt.getYear() + ((int) hi) * 10000;
        }
        // There is no year zero: the one before 0001 is -0001.
        if (year <= 0) {
            year = year - 1;
        }
        int fraction = ldt.getNano();
        StringBuilder sb = new StringBuilder(64);
        sb.append(year < 0 ? "-" : "");
        year = (int) Math.abs((long) year);
        if (year < 10000) {
            pad(sb, 1000, year);
        } else {
            sb.append(String.valueOf(year));
        }
        sb.append('-');
        pad(sb, 10, ldt.getMonthValue());
        sb.append('-');
        pad(sb, 10, ldt.getDayOfMonth());
        sb.append('T');
        pad(sb, 10, ldt.getHour());
        sb.append(':');
        pad(sb, 10, ldt.getMinute());
        sb.append(':');
        pad(sb, 10, ldt.getSecond());
        if (fraction != 0) {
            sb.append('.');
            int w = 100000000;
            while (fraction % 10 == 0) {
                fraction = fraction / 10;
                w = w / 10;
            }
            pad(sb, w, fraction);
        }
        sb.append('Z');
        s = sb.toString();
        this.text = s;
        return s;
    }
}
