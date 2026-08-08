package java.time;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

// KajiLibrary's java.time.Instant — a point on the time-line, as seconds since the Unix epoch +
// a nanosecond-of-second (nanos normalised to [0, 1e9)). Immutable value type. Implements Temporal
// (so it supports plus/minus over units and cooperates with Duration.addTo), TemporalAdjuster and
// Comparable. A KajiLibrary subset: the field/unit support covers what our ChronoField/ChronoUnit
// subset offers (NANO_OF_SECOND; NANOS/MILLIS/SECONDS/MINUTES/HOURS/DAYS); toString/ISO deferred.
public final class Instant implements Temporal, TemporalAdjuster, Comparable<Instant> {

    private static final long NANOS_PER_SECOND = 1000000000L;

    private final long seconds;
    private final int nanos;

    private Instant(long seconds, int nanos) {
        this.seconds = seconds;
        this.nanos = nanos;
    }

    // Normalise a nano adjustment into [0, 1e9), carrying into seconds (floor semantics).
    private static Instant create(long seconds, long nanoAdjustment) {
        long extraSeconds = nanoAdjustment / NANOS_PER_SECOND;
        long nos = nanoAdjustment % NANOS_PER_SECOND;
        if (nos < 0) {
            nos = nos + NANOS_PER_SECOND;
            extraSeconds = extraSeconds - 1;
        }
        return new Instant(seconds + extraSeconds, (int) nos);
    }

    public static Instant ofEpochSecond(long epochSecond) {
        return new Instant(epochSecond, 0);
    }

    public static Instant ofEpochSecond(long epochSecond, long nanoAdjustment) {
        return create(epochSecond, nanoAdjustment);
    }

    public static Instant ofEpochMilli(long epochMilli) {
        return create(epochMilli / 1000L, (epochMilli % 1000L) * 1000000L);
    }

    // The current instant from the VM's wall clock (the one native seam).
    public static Instant now() {
        return Instant.ofEpochMilli(System.currentTimeMillis());
    }

    public long getEpochSecond() {
        return this.seconds;
    }

    public int getNano() {
        return this.nanos;
    }

    public long toEpochMilli() {
        return this.seconds * 1000L + this.nanos / 1000000L;
    }

    public Instant plusSeconds(long secondsToAdd) {
        return create(this.seconds + secondsToAdd, this.nanos);
    }

    public Instant plusMillis(long millisToAdd) {
        return create(this.seconds + millisToAdd / 1000L, (long) this.nanos + (millisToAdd % 1000L) * 1000000L);
    }

    public Instant plusNanos(long nanosToAdd) {
        return create(this.seconds, (long) this.nanos + nanosToAdd);
    }

    public Instant minusSeconds(long secondsToSubtract) {
        return this.plusSeconds(-secondsToSubtract);
    }

    public Instant minusMillis(long millisToSubtract) {
        return this.plusMillis(-millisToSubtract);
    }

    public Instant minusNanos(long nanosToSubtract) {
        return this.plusNanos(-nanosToSubtract);
    }

    public boolean isBefore(Instant other) {
        return this.compareTo(other) < 0;
    }

    public boolean isAfter(Instant other) {
        return this.compareTo(other) > 0;
    }

    public int compareTo(Instant other) {
        if (this.seconds < other.seconds) {
            return -1;
        }
        if (this.seconds > other.seconds) {
            return 1;
        }
        return this.nanos - other.nanos;
    }

    // --- Temporal ---

    public boolean isSupported(TemporalField field) {
        return field == ChronoField.NANO_OF_SECOND;
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.NANO_OF_SECOND) {
            return this.nanos;
        }
        throw new IllegalArgumentException();
    }

    public boolean isSupported(TemporalUnit unit) {
        return unit == ChronoUnit.NANOS || unit == ChronoUnit.MILLIS || unit == ChronoUnit.SECONDS
            || unit == ChronoUnit.MINUTES || unit == ChronoUnit.HOURS || unit == ChronoUnit.DAYS;
    }

    public Temporal with(TemporalField field, long newValue) {
        if (field == ChronoField.NANO_OF_SECOND) {
            return create(this.seconds, newValue);
        }
        throw new IllegalArgumentException();
    }

    public Temporal plus(long amountToAdd, TemporalUnit unit) {
        if (unit == ChronoUnit.NANOS) {
            return this.plusNanos(amountToAdd);
        }
        if (unit == ChronoUnit.MILLIS) {
            return this.plusMillis(amountToAdd);
        }
        if (unit == ChronoUnit.SECONDS) {
            return this.plusSeconds(amountToAdd);
        }
        if (unit == ChronoUnit.MINUTES) {
            return this.plusSeconds(amountToAdd * 60L);
        }
        if (unit == ChronoUnit.HOURS) {
            return this.plusSeconds(amountToAdd * 3600L);
        }
        if (unit == ChronoUnit.DAYS) {
            return this.plusSeconds(amountToAdd * 86400L);
        }
        throw new IllegalArgumentException();
    }

    public Temporal minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        Instant end = (Instant) endExclusive;
        long secondsDiff = end.seconds - this.seconds;
        long nanosDiff = (long) end.nanos - this.nanos;
        if (unit == ChronoUnit.SECONDS) {
            return secondsDiff;
        }
        if (unit == ChronoUnit.NANOS) {
            return secondsDiff * NANOS_PER_SECOND + nanosDiff;
        }
        if (unit == ChronoUnit.MILLIS) {
            return secondsDiff * 1000L + nanosDiff / 1000000L;
        }
        throw new IllegalArgumentException();
    }

    // --- TemporalAdjuster ---

    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.NANO_OF_SECOND, this.nanos);
    }

    // --- value-type methods (ISO-8601 instant, always UTC / 'Z') ---

    public String toString() {
        long epochDay = floorDiv(this.seconds, 86400L);
        int secsOfDay = (int) (this.seconds - epochDay * 86400L);
        String datePart = LocalDate.ofEpochDay(epochDay).toString();
        int hour = secsOfDay / 3600;
        int minute = (secsOfDay % 3600) / 60;
        int second = secsOfDay % 60;
        StringBuilder buf = new StringBuilder();
        buf.append(datePart);
        buf.append("T");
        if (hour < 10) {
            buf.append("0");
        }
        buf.append(Integer.toString(hour));
        if (minute < 10) {
            buf.append(":0");
        } else {
            buf.append(":");
        }
        buf.append(Integer.toString(minute));
        if (second < 10) {
            buf.append(":0");
        } else {
            buf.append(":");
        }
        buf.append(Integer.toString(second));
        if (this.nanos > 0) {
            buf.append(".");
            buf.append(nanoString(this.nanos));
        }
        buf.append("Z");
        return buf.toString();
    }

    private static String nanoString(int nano) {
        if (nano % 1000000 == 0) {
            String t = Integer.toString(nano / 1000000 + 1000);
            return t.substring(1, t.length());
        }
        if (nano % 1000 == 0) {
            String t = Integer.toString(nano / 1000 + 1000000);
            return t.substring(1, t.length());
        }
        String t = Integer.toString(nano + 1000000000);
        return t.substring(1, t.length());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Instant) {
            Instant o = (Instant) obj;
            return this.seconds == o.seconds && this.nanos == o.nanos;
        }
        return false;
    }

    public int hashCode() {
        return (int) (this.seconds ^ (this.seconds >>> 32)) + 51 * this.nanos;
    }

    private static long floorDiv(long a, long b) {
        long q = a / b;
        if ((a % b != 0) && ((a ^ b) < 0)) {
            q = q - 1;
        }
        return q;
    }

    // --- generic Temporal-typed conveniences ---

    public int get(TemporalField field) {
        return (int) this.getLong(field);
    }

    public Instant plus(TemporalAmount amount) {
        return (Instant) amount.addTo(this);
    }

    public Instant minus(TemporalAmount amount) {
        return (Instant) amount.subtractFrom(this);
    }

    public Instant with(TemporalAdjuster adjuster) {
        return (Instant) adjuster.adjustInto(this);
    }

    // Parses an ISO instant (uuuu-MM-ddTHH:mm:ss[.fff]Z), reusing LocalDate/LocalTime parsing.
    public static Instant parse(CharSequence text) {
        String s = text.toString();
        int t = -1;
        for (int k = 0; k < s.length(); k = k + 1) {
            if (s.charAt(k) == 'T') {
                t = k;
                break;
            }
        }
        LocalDate d = LocalDate.parse(s.substring(0, t));
        LocalTime tm = LocalTime.parse(s.substring(t + 1, s.length() - 1));
        long epochSecond = d.toEpochDay() * 86400L + tm.getHour() * 3600L + tm.getMinute() * 60L + tm.getSecond();
        return Instant.ofEpochSecond(epochSecond, tm.getNano());
    }
}
