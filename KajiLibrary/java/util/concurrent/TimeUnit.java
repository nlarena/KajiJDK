package java.util.concurrent;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

// A granularity for a duration, and the conversions between granularities. Each
// constant carries its scale in nanoseconds; conversions divide (to a coarser unit)
// or multiply with saturation (to a finer unit), exactly like the JDK's {@code cvt}.
//
// {@code timedWait}/{@code timedJoin} are intentionally absent: they need timed
// monitor/join primitives ({@code Object.wait(long)}, {@code Thread.join(long)}) that
// KajiJDK's runtime does not expose yet. {@code sleep} rides on the untimed
// {@code Thread.sleep(long ms)} it does have.
public enum TimeUnit {
    NANOSECONDS(1L),
    MICROSECONDS(1000L),
    MILLISECONDS(1000L * 1000L),
    SECONDS(1000L * 1000L * 1000L),
    MINUTES(1000L * 1000L * 1000L * 60L),
    HOURS(1000L * 1000L * 1000L * 60L * 60L),
    DAYS(1000L * 1000L * 1000L * 60L * 60L * 24L);

    // Scale in nanoseconds per unit (NANOSECONDS = 1, DAYS = 86_400_000_000_000).
    private final long scale;

    TimeUnit(long scale) {
        this.scale = scale;
    }

    // Convert d, measured in `src` nanos-per-unit, to `dst` nanos-per-unit. Coarser →
    // finer multiplies and saturates to Long.MAX/MIN on overflow; finer → coarser
    // divides (truncating toward zero). This is the JDK's private cvt, verbatim.
    // The saturation bounds are written as literals rather than Long.MAX_VALUE/MIN_VALUE:
    // reading a static field of another compiled class emits `getfield` and traps at run time
    // (finding #110). This code predates that discovery, and the bug was live — every
    // coarse-to-fine conversion (SECONDS.toMillis(1), NANOSECONDS.convert(x, MILLISECONDS))
    // took the branch that reads the bound and died. Only src == dst avoided it, which is
    // exactly the shape the original self-test happened to cover.
    private static long maxLong() {
        return 9223372036854775807L;
    }

    private static long minLong() {
        return -9223372036854775807L - 1L;
    }
    private static long cvt(long d, long dst, long src) {
        long r, m;
        if (src == dst) {
            return d;
        } else if (src < dst) {
            return d / (dst / src);
        } else if (d > (m = maxLong() / (r = src / dst))) {
            return maxLong();
        } else if (d < -m) {
            return minLong();
        } else {
            return d * r;
        }
    }

    public long convert(long sourceDuration, TimeUnit sourceUnit) {
        return cvt(sourceDuration, scale, sourceUnit.scale);
    }

    public long convert(Duration duration) {
        long secs = duration.getSeconds();
        int nano = duration.getNano();
        if (secs < 0 && nano > 0) {
            // Duration keeps nanos in [0, 1e9); rebuild a same-sign (secs, nano) pair
            // so the two conversions below don't disagree on rounding direction.
            secs++;
            nano -= 1000000000;
        }
        long secPart = cvt(secs, scale, SECONDS.scale);
        if (secPart == maxLong() || secPart == minLong()) {
            return secPart;
        }
        // `nano` is an int: cast explicitly, since the implicit widening at a call site
        // omits `i2l` (finding #103) and would pass an int where cvt expects a long.
        long nanoPart = cvt((long) nano, scale, NANOSECONDS.scale);
        return secPart + nanoPart;
    }

    public long toNanos(long duration) {
        return cvt(duration, NANOSECONDS.scale, scale);
    }

    public long toMicros(long duration) {
        return cvt(duration, MICROSECONDS.scale, scale);
    }

    public long toMillis(long duration) {
        return cvt(duration, MILLISECONDS.scale, scale);
    }

    public long toSeconds(long duration) {
        return cvt(duration, SECONDS.scale, scale);
    }

    public long toMinutes(long duration) {
        return cvt(duration, MINUTES.scale, scale);
    }

    public long toHours(long duration) {
        return cvt(duration, HOURS.scale, scale);
    }

    public long toDays(long duration) {
        return cvt(duration, DAYS.scale, scale);
    }

    public void sleep(long timeout) throws InterruptedException {
        if (timeout > 0) {
            Thread.sleep(toMillis(timeout));
        }
    }

    public ChronoUnit toChronoUnit() {
        if (this == NANOSECONDS) return ChronoUnit.NANOS;
        if (this == MICROSECONDS) return ChronoUnit.MICROS;
        if (this == MILLISECONDS) return ChronoUnit.MILLIS;
        if (this == SECONDS) return ChronoUnit.SECONDS;
        if (this == MINUTES) return ChronoUnit.MINUTES;
        if (this == HOURS) return ChronoUnit.HOURS;
        return ChronoUnit.DAYS;
    }

    public static TimeUnit of(ChronoUnit chronoUnit) {
        if (chronoUnit == ChronoUnit.NANOS) return NANOSECONDS;
        if (chronoUnit == ChronoUnit.MICROS) return MICROSECONDS;
        if (chronoUnit == ChronoUnit.MILLIS) return MILLISECONDS;
        if (chronoUnit == ChronoUnit.SECONDS) return SECONDS;
        if (chronoUnit == ChronoUnit.MINUTES) return MINUTES;
        if (chronoUnit == ChronoUnit.HOURS) return HOURS;
        if (chronoUnit == ChronoUnit.DAYS) return DAYS;
        throw new IllegalArgumentException("No TimeUnit equivalent for " + chronoUnit);
    }
}
