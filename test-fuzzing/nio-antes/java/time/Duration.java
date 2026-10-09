package java.time;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

// KajiLibrary's java.time.Duration — a time-based amount, as seconds + nanoseconds (nanos kept
// normalised to [0, 1e9), seconds may be negative). Immutable value type: every operation returns
// a fresh Duration. Implements TemporalAmount (so `temporal.plus(duration)` works) and Comparable.
// It is no longer a subset: the public surface is complete.
public final class Duration implements TemporalAmount, Comparable<Duration>, Serializable {

    private static final long NANOS_PER_SECOND = 1000000000L;

    private final long seconds;
    private final int nanos;

    private Duration(long seconds, int nanos) {
        this.seconds = seconds;
        this.nanos = nanos;
    }

    // Build a Duration from seconds + a nano adjustment, carrying nanos into seconds with floor
    // semantics so nanos ends in [0, 1e9) even when the adjustment is negative.
    private static Duration create(long seconds, long nanoAdjustment) {
        long extraSeconds = nanoAdjustment / NANOS_PER_SECOND;
        long nos = nanoAdjustment % NANOS_PER_SECOND;
        if (nos < 0) {
            nos = nos + NANOS_PER_SECOND;
            extraSeconds = extraSeconds - 1;
        }
        return new Duration(seconds + extraSeconds, (int) nos);
    }

    /** The duration of zero length. */
    public static final Duration ZERO = new Duration(0L, 0);

    public static Duration ofSeconds(long seconds) {
        return new Duration(seconds, 0);
    }

    public static Duration ofSeconds(long seconds, long nanoAdjustment) {
        return create(seconds, nanoAdjustment);
    }

    public static Duration ofMillis(long millis) {
        return create(millis / 1000L, (millis % 1000L) * 1000000L);
    }

    public static Duration ofNanos(long nanos) {
        return create(nanos / NANOS_PER_SECOND, nanos % NANOS_PER_SECOND);
    }

    public static Duration ofMinutes(long minutes) {
        return new Duration(minutes * 60L, 0);
    }

    public static Duration ofHours(long hours) {
        return new Duration(hours * 3600L, 0);
    }

    public static Duration ofDays(long days) {
        return new Duration(days * 86400L, 0);
    }

    public long getSeconds() {
        return this.seconds;
    }

    public int getNano() {
        return this.nanos;
    }

    public boolean isZero() {
        return this.seconds == 0 && this.nanos == 0;
    }

    public boolean isNegative() {
        return this.seconds < 0;
    }

    public Duration plusSeconds(long secondsToAdd) {
        return create(this.seconds + secondsToAdd, this.nanos);
    }

    public Duration minusSeconds(long secondsToSubtract) {
        return create(this.seconds - secondsToSubtract, this.nanos);
    }

    public Duration plus(Duration other) {
        return create(this.seconds + other.seconds, (long) this.nanos + other.nanos);
    }

    public Duration minus(Duration other) {
        return create(this.seconds - other.seconds, (long) this.nanos - other.nanos);
    }

    public Duration negated() {
        return create(-this.seconds, -(long) this.nanos);
    }

    public long toMillis() {
        return this.seconds * 1000L + this.nanos / 1000000L;
    }

    public long toNanos() {
        return this.seconds * NANOS_PER_SECOND + this.nanos;
    }

    // Natural order by length. Synthesizes the compareTo(Object) bridge.
    // ---- factories ------------------------------------------------------------------------------

    /**
     * `amount` units of `unit`.
     *
     * <p>It accepts only **exact** units: the time-based ones, and `DAYS` --which here is exactly 24
     * hours--. `MONTHS` and `YEARS` are rejected, and that is not a limitation: a month does not
     * always last the same, so there is no number of seconds that corresponds to it. That is what
     * `Period` models, not `Duration`.
     *
     * @throws java.time.DateTimeException if the unit has no exact duration
     */
    public static Duration of(long amount, TemporalUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return ZERO.plus(amount, unit);
    }

    /**
     * The duration between two points, measured in seconds and nanos.
     *
     * <p>Negative if `end` is before `start`, which is what makes it composable:
     * `start.plus(between(start, end)).equals(end)` always holds.
     */
    public static Duration between(Temporal startInclusive, Temporal endExclusive) {
        if (startInclusive == null || endExclusive == null) {
            throw new NullPointerException();
        }
        long secs = startInclusive.until(endExclusive, ChronoUnit.SECONDS);
        long nanos = 0L;
        if (startInclusive.isSupported(ChronoField.NANO_OF_SECOND)
                && endExclusive.isSupported(ChronoField.NANO_OF_SECOND)) {
            nanos = endExclusive.getLong(ChronoField.NANO_OF_SECOND)
                    - startInclusive.getLong(ChronoField.NANO_OF_SECOND);
        }
        return create(secs, nanos);
    }

    /** The duration equivalent to `amount`, which has to be in exact units. */
    public static Duration from(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        Duration d = ZERO;
        List<TemporalUnit> units = amount.getUnits();
        int i = 0;
        while (i < units.size()) {
            TemporalUnit u = units.get(i);
            d = d.plus(amount.get(u), u);
            i = i + 1;
        }
        return d;
    }

    // ---- the parts -------------------------------------------------------------------------------
    //
    // Two families that are easy to confuse, and the difference matters: `toMinutes()` is the
    // **whole** duration expressed in minutes, and `toMinutesPart()` is the minutes field within the
    // hour --0 to 59--. For 3661 seconds, the first gives 61 and the second 1.

    /** The total, in 24-hour days, truncated towards zero. */
    public long toDays() {
        return this.seconds / 86400L;
    }

    /** The total, in hours, truncated towards zero. */
    public long toHours() {
        return this.seconds / 3600L;
    }

    /** The total, in minutes, truncated towards zero. */
    public long toMinutes() {
        return this.seconds / 60L;
    }

    /** The total, in seconds, truncated towards zero. */
    public long toSeconds() {
        return this.seconds;
    }

    /** The days, as a part. The same as `toDays()`: there is no larger unit for it to be part of. */
    public long toDaysPart() {
        return this.seconds / 86400L;
    }

    /** The hours within the day, 0 to 23. */
    public int toHoursPart() {
        return (int) (this.toHours() % 24L);
    }

    /** The minutes within the hour, 0 to 59. */
    public int toMinutesPart() {
        return (int) (this.toMinutes() % 60L);
    }

    /** The seconds within the minute, 0 to 59. */
    public int toSecondsPart() {
        return (int) (this.seconds % 60L);
    }

    /** The milliseconds within the second, 0 to 999. */
    public int toMillisPart() {
        return this.nanos / 1000000;
    }

    /** The nanoseconds within the second, 0 to 999999999. */
    public int toNanosPart() {
        return this.nanos;
    }

    /** Whether it is strictly greater than zero. The complement of `isNegative`, minus zero. */
    public boolean isPositive() {
        return this.seconds > 0L || (this.seconds == 0L && this.nanos > 0);
    }

    // ---- arithmetic ------------------------------------------------------------------------------

    /**
     * This duration plus `amount` units of `unit`.
     *
     * @throws java.time.DateTimeException if the unit has no exact duration
     */
    public Duration plus(long amountToAdd, TemporalUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (unit == ChronoUnit.DAYS) {
            return this.plusSeconds(amountToAdd * 86400L);
        }
        if (unit == ChronoUnit.HALF_DAYS) {
            return this.plusSeconds(amountToAdd * 43200L);
        }
        if (unit == ChronoUnit.HOURS) {
            return this.plusHours(amountToAdd);
        }
        if (unit == ChronoUnit.MINUTES) {
            return this.plusMinutes(amountToAdd);
        }
        if (unit == ChronoUnit.SECONDS) {
            return this.plusSeconds(amountToAdd);
        }
        if (unit == ChronoUnit.MILLIS) {
            return this.plusMillis(amountToAdd);
        }
        if (unit == ChronoUnit.MICROS) {
            return this.plusNanos(amountToAdd * 1000L);
        }
        if (unit == ChronoUnit.NANOS) {
            return this.plusNanos(amountToAdd);
        }
        throw new java.time.DateTimeException("Unit must not have an estimated duration: " + unit);
    }

    public Duration minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    public Duration plusDays(long daysToAdd) {
        return this.plusSeconds(daysToAdd * 86400L);
    }

    public Duration plusHours(long hoursToAdd) {
        return this.plusSeconds(hoursToAdd * 3600L);
    }

    public Duration plusMinutes(long minutesToAdd) {
        return this.plusSeconds(minutesToAdd * 60L);
    }

    public Duration plusMillis(long millisToAdd) {
        return create(this.seconds + millisToAdd / 1000L,
                (long) this.nanos + (millisToAdd % 1000L) * 1000000L);
    }

    public Duration plusNanos(long nanosToAdd) {
        return create(this.seconds, (long) this.nanos + nanosToAdd);
    }

    public Duration minusDays(long daysToSubtract) {
        return this.plusDays(-daysToSubtract);
    }

    public Duration minusHours(long hoursToSubtract) {
        return this.plusHours(-hoursToSubtract);
    }

    public Duration minusMinutes(long minutesToSubtract) {
        return this.plusMinutes(-minutesToSubtract);
    }

    public Duration minusMillis(long millisToSubtract) {
        return this.plusMillis(-millisToSubtract);
    }

    public Duration minusNanos(long nanosToSubtract) {
        return this.plusNanos(-nanosToSubtract);
    }

    /** This duration multiplied by `multiplicand`. */
    public Duration multipliedBy(long multiplicand) {
        if (multiplicand == 0L) {
            return ZERO;
        }
        if (multiplicand == 1L) {
            return this;
        }
        // It works in total nanos, which is where the multiplication is a single sum. The useful
        // range is bounded by the `long`, just as in the JDK.
        return Duration.ofNanos(this.toNanos() * multiplicand);
    }

    /**
     * This duration divided by `divisor`, truncating towards zero.
     *
     * @throws ArithmeticException if `divisor` is zero
     */
    public Duration dividedBy(long divisor) {
        if (divisor == 0L) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        if (divisor == 1L) {
            return this;
        }
        return Duration.ofNanos(this.toNanos() / divisor);
    }

    /**
     * How many times `divisor` fits into this duration, truncating towards zero.
     *
     * @throws ArithmeticException if `divisor` is zero
     */
    public long dividedBy(Duration divisor) {
        if (divisor == null) {
            throw new NullPointerException("divisor");
        }
        long d = divisor.toNanos();
        if (d == 0L) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return this.toNanos() / d;
    }

    /** The absolute value: this one if it is not negative, the negated one if it is. */
    public Duration abs() {
        return this.isNegative() ? this.negated() : this;
    }

    /** This duration with other seconds, keeping the nanos. */
    public Duration withSeconds(long seconds) {
        return create(seconds, (long) this.nanos);
    }

    /**
     * This duration with other nanos, keeping the seconds.
     *
     * @throws java.time.DateTimeException if `nanoOfSecond` falls outside [0, 999999999]
     */
    public Duration withNanos(int nanoOfSecond) {
        if (nanoOfSecond < 0 || nanoOfSecond > 999999999) {
            throw new java.time.DateTimeException(
                    "Invalid value for NanoOfSecond (valid values 0 - 999999999): " + nanoOfSecond);
        }
        return new Duration(this.seconds, nanoOfSecond);
    }

    /**
     * This duration truncated to a multiple of `unit`.
     *
     * <p>It truncates **towards zero**, and that is why not any unit will do: it has to divide a day
     * exactly. `HOURS` yes, `DAYS` yes, but not an estimated unit.
     *
     * @throws java.time.DateTimeException if the unit does not divide a day
     */
    public Duration truncatedTo(TemporalUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (unit == ChronoUnit.SECONDS && this.seconds >= 0 && this.nanos == 0) {
            return this;
        }
        long unitInNanos = 0L;
        if (unit == ChronoUnit.NANOS) {
            unitInNanos = 1L;
        } else if (unit == ChronoUnit.MICROS) {
            unitInNanos = 1000L;
        } else if (unit == ChronoUnit.MILLIS) {
            unitInNanos = 1000000L;
        } else if (unit == ChronoUnit.SECONDS) {
            unitInNanos = NANOS_PER_SECOND;
        } else if (unit == ChronoUnit.MINUTES) {
            unitInNanos = 60L * NANOS_PER_SECOND;
        } else if (unit == ChronoUnit.HOURS) {
            unitInNanos = 3600L * NANOS_PER_SECOND;
        } else if (unit == ChronoUnit.HALF_DAYS) {
            unitInNanos = 43200L * NANOS_PER_SECOND;
        } else if (unit == ChronoUnit.DAYS) {
            unitInNanos = 86400L * NANOS_PER_SECOND;
        } else {
            throw new java.time.DateTimeException("Unit is too large to be used for truncation");
        }
        // It truncates **towards zero**, not downwards: -1.5s to seconds is -1s, and -90s to minutes
        // is -60s.
        //
        // This was checked against the real `java` because it had been the other way round.
        // "Truncate" suggests going downwards --which is what `Math.floorDiv` does-- and here it is
        // towards zero, which for the negatives is the opposite direction. Java's integer division
        // already truncates towards zero, so the sum comes out by itself; what was needed was **not**
        // to correct the negative remainder.
        return Duration.ofNanos(this.toNanos() / unitInNanos * unitInNanos);
    }

    public int compareTo(Duration other) {
        if (this.seconds < other.seconds) {
            return -1;
        }
        if (this.seconds > other.seconds) {
            return 1;
        }
        return this.nanos - other.nanos;
    }

    // --- TemporalAmount ---

    public long get(TemporalUnit unit) {
        if (unit == ChronoUnit.SECONDS) {
            return this.seconds;
        }
        if (unit == ChronoUnit.NANOS) {
            return this.nanos;
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    public List<TemporalUnit> getUnits() {
        List<TemporalUnit> units = new ArrayList<TemporalUnit>();
        units.add(ChronoUnit.SECONDS);
        units.add(ChronoUnit.NANOS);
        return units;
    }

    public Temporal addTo(Temporal temporal) {
        Temporal result = temporal;
        if (this.seconds != 0) {
            result = result.plus(this.seconds, ChronoUnit.SECONDS);
        }
        if (this.nanos != 0) {
            result = result.plus(this.nanos, ChronoUnit.NANOS);
        }
        return result;
    }

    public Temporal subtractFrom(Temporal temporal) {
        Temporal result = temporal;
        if (this.seconds != 0) {
            result = result.minus(this.seconds, ChronoUnit.SECONDS);
        }
        if (this.nanos != 0) {
            result = result.minus(this.nanos, ChronoUnit.NANOS);
        }
        return result;
    }

    // ISO-8601: PTnHnMnS (PT0S for zero), with a fractional seconds part when nanos are set.
    public String toString() {
        if (this.seconds == 0 && this.nanos == 0) {
            return "PT0S";
        }
        long hours = this.seconds / 3600;
        long minutes = (this.seconds % 3600) / 60;
        long secs = this.seconds % 60;
        StringBuilder buf = new StringBuilder("PT");
        if (hours != 0) {
            buf.append(Long.toString(hours));
            buf.append("H");
        }
        if (minutes != 0) {
            buf.append(Long.toString(minutes));
            buf.append("M");
        }
        if (secs == 0 && this.nanos == 0 && buf.length() > 2) {
            return buf.toString();
        }
        if (secs < 0 && this.nanos > 0) {
            if (secs == -1) {
                buf.append("-0");
            } else {
                buf.append(Long.toString(secs + 1));
            }
        } else {
            buf.append(Long.toString(secs));
        }
        if (this.nanos > 0) {
            long v;
            if (secs < 0) {
                v = 2000000000L - this.nanos;
            } else {
                v = this.nanos + 1000000000L;
            }
            String s = Long.toString(v);
            String frac = s.substring(1, s.length());
            int end = frac.length();
            while (end > 1 && frac.charAt(end - 1) == '0') {
                end = end - 1;
            }
            buf.append(".");
            buf.append(frac.substring(0, end));
        }
        buf.append("S");
        return buf.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Duration) {
            Duration o = (Duration) obj;
            return this.seconds == o.seconds && this.nanos == o.nanos;
        }
        return false;
    }

    public int hashCode() {
        return (int) (this.seconds ^ (this.seconds >>> 32)) + (51 * this.nanos);
    }

    // Parses an ISO duration PT[nH][nM][n[.n]S] (time components only, each optionally signed).
    public static Duration parse(CharSequence text) {
        String s = text.toString();
        int i = 2;
        long totalSeconds = 0;
        int nanos = 0;
        while (i < s.length()) {
            int sign = 1;
            if (s.charAt(i) == '-') {
                sign = -1;
                i = i + 1;
            } else if (s.charAt(i) == '+') {
                i = i + 1;
            }
            long whole = 0;
            while (i < s.length() && isDigit(s.charAt(i))) {
                whole = whole * 10 + (s.charAt(i) - '0');
                i = i + 1;
            }
            int frac = 0;
            if (i < s.length() && s.charAt(i) == '.') {
                i = i + 1;
                int fStart = i;
                while (i < s.length() && isDigit(s.charAt(i))) {
                    i = i + 1;
                }
                String f = s.substring(fStart, i);
                while (f.length() < 9) {
                    f = f + "0";
                }
                for (int k = 0; k < 9; k = k + 1) {
                    frac = frac * 10 + (f.charAt(k) - '0');
                }
            }
            char u = s.charAt(i);
            i = i + 1;
            if (u == 'H') {
                totalSeconds = totalSeconds + sign * whole * 3600;
            } else if (u == 'M') {
                totalSeconds = totalSeconds + sign * whole * 60;
            } else if (u == 'S') {
                totalSeconds = totalSeconds + sign * whole;
                nanos = nanos + sign * frac;
            }
        }
        while (nanos < 0) {
            nanos = nanos + 1000000000;
            totalSeconds = totalSeconds - 1;
        }
        while (nanos >= 1000000000) {
            nanos = nanos - 1000000000;
            totalSeconds = totalSeconds + 1;
        }
        return Duration.ofSeconds(totalSeconds, nanos);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
