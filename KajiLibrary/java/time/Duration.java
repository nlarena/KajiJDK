package java.time;

import java.util.List;
import java.util.ArrayList;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoUnit;

// KajiLibrary's java.time.Duration — a time-based amount, as seconds + nanoseconds (nanos kept
// normalised to [0, 1e9), seconds may be negative). Immutable value type: every operation returns
// a fresh Duration. Implements TemporalAmount (so `temporal.plus(duration)` works) and Comparable.
// A KajiLibrary subset (the JDK adds toString/ISO parse, plusMinutes/…, dividedBy, etc.).
public final class Duration implements TemporalAmount, Comparable<Duration> {

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
        throw new IllegalArgumentException();
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
