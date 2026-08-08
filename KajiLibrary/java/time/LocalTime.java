package java.time;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;

// KajiLibrary's java.time.LocalTime — a time of day (hour/minute/second/nano) without date or zone.
// Immutable value type. Arithmetic wraps within a 24h day (via nano-of-day). Implements Temporal,
// TemporalAdjuster and Comparable. A KajiLibrary subset (toString/parse and some fields deferred).
public final class LocalTime implements Temporal, TemporalAdjuster, Comparable<LocalTime> {

    private static final long NANOS_PER_SECOND = 1000000000L;
    private static final long NANOS_PER_DAY = 86400L * NANOS_PER_SECOND;

    private final int hour;
    private final int minute;
    private final int second;
    private final int nano;

    private LocalTime(int hour, int minute, int second, int nano) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.nano = nano;
    }

    public static LocalTime of(int hour, int minute) {
        return new LocalTime(hour, minute, 0, 0);
    }

    public static LocalTime of(int hour, int minute, int second) {
        return new LocalTime(hour, minute, second, 0);
    }

    public static LocalTime of(int hour, int minute, int second, int nanoOfSecond) {
        return new LocalTime(hour, minute, second, nanoOfSecond);
    }

    public static LocalTime ofSecondOfDay(long secondOfDay) {
        int hour = (int) (secondOfDay / 3600L);
        long rem = secondOfDay - hour * 3600L;
        int minute = (int) (rem / 60L);
        int second = (int) (rem - minute * 60L);
        return new LocalTime(hour, minute, second, 0);
    }

    public static LocalTime ofNanoOfDay(long nanoOfDay) {
        int hour = (int) (nanoOfDay / 3600000000000L);
        long rem = nanoOfDay - hour * 3600000000000L;
        int minute = (int) (rem / 60000000000L);
        rem = rem - minute * 60000000000L;
        int second = (int) (rem / NANOS_PER_SECOND);
        int nano = (int) (rem - second * NANOS_PER_SECOND);
        return new LocalTime(hour, minute, second, nano);
    }

    public static LocalTime now() {
        return LocalTime.ofNanoOfDay((System.currentTimeMillis() % 86400000L) * 1000000L);
    }

    public int getHour() {
        return this.hour;
    }

    public int getMinute() {
        return this.minute;
    }

    public int getSecond() {
        return this.second;
    }

    public int getNano() {
        return this.nano;
    }

    public int toSecondOfDay() {
        return this.hour * 3600 + this.minute * 60 + this.second;
    }

    public long toNanoOfDay() {
        return this.hour * 3600000000000L + this.minute * 60000000000L
            + this.second * NANOS_PER_SECOND + this.nano;
    }

    // --- arithmetic (wraps within the day) ---

    public LocalTime plusNanos(long nanosToAdd) {
        long nofd = this.toNanoOfDay();
        long newNofd = ((nofd + nanosToAdd) % NANOS_PER_DAY + NANOS_PER_DAY) % NANOS_PER_DAY;
        return LocalTime.ofNanoOfDay(newNofd);
    }

    public LocalTime plusSeconds(long secondsToAdd) {
        return this.plusNanos((secondsToAdd % 86400L) * NANOS_PER_SECOND);
    }

    public LocalTime plusMinutes(long minutesToAdd) {
        return this.plusSeconds((minutesToAdd % 1440L) * 60L);
    }

    public LocalTime plusHours(long hoursToAdd) {
        return this.plusSeconds((hoursToAdd % 24L) * 3600L);
    }

    public LocalTime minusNanos(long nanosToSubtract) {
        return this.plusNanos(-(nanosToSubtract % NANOS_PER_DAY));
    }

    public LocalTime minusSeconds(long secondsToSubtract) {
        return this.plusSeconds(-(secondsToSubtract % 86400L));
    }

    public LocalTime minusMinutes(long minutesToSubtract) {
        return this.plusMinutes(-(minutesToSubtract % 1440L));
    }

    public LocalTime minusHours(long hoursToSubtract) {
        return this.plusHours(-(hoursToSubtract % 24L));
    }

    // --- comparison ---

    public int compareTo(LocalTime other) {
        if (this.hour != other.hour) {
            return this.hour - other.hour;
        }
        if (this.minute != other.minute) {
            return this.minute - other.minute;
        }
        if (this.second != other.second) {
            return this.second - other.second;
        }
        return this.nano - other.nano;
    }

    public boolean isBefore(LocalTime other) {
        return this.compareTo(other) < 0;
    }

    public boolean isAfter(LocalTime other) {
        return this.compareTo(other) > 0;
    }

    // --- Temporal ---

    public boolean isSupported(TemporalField field) {
        return field == ChronoField.HOUR_OF_DAY || field == ChronoField.MINUTE_OF_HOUR
            || field == ChronoField.SECOND_OF_MINUTE || field == ChronoField.NANO_OF_SECOND;
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.HOUR_OF_DAY) {
            return this.hour;
        }
        if (field == ChronoField.MINUTE_OF_HOUR) {
            return this.minute;
        }
        if (field == ChronoField.SECOND_OF_MINUTE) {
            return this.second;
        }
        if (field == ChronoField.NANO_OF_SECOND) {
            return this.nano;
        }
        throw new IllegalArgumentException();
    }

    public boolean isSupported(TemporalUnit unit) {
        return unit == ChronoUnit.NANOS || unit == ChronoUnit.MILLIS || unit == ChronoUnit.SECONDS
            || unit == ChronoUnit.MINUTES || unit == ChronoUnit.HOURS;
    }

    public Temporal with(TemporalField field, long newValue) {
        if (field == ChronoField.HOUR_OF_DAY) {
            return new LocalTime((int) newValue, this.minute, this.second, this.nano);
        }
        if (field == ChronoField.MINUTE_OF_HOUR) {
            return new LocalTime(this.hour, (int) newValue, this.second, this.nano);
        }
        if (field == ChronoField.SECOND_OF_MINUTE) {
            return new LocalTime(this.hour, this.minute, (int) newValue, this.nano);
        }
        if (field == ChronoField.NANO_OF_SECOND) {
            return new LocalTime(this.hour, this.minute, this.second, (int) newValue);
        }
        throw new IllegalArgumentException();
    }

    public Temporal plus(long amountToAdd, TemporalUnit unit) {
        if (unit == ChronoUnit.NANOS) {
            return this.plusNanos(amountToAdd);
        }
        if (unit == ChronoUnit.SECONDS) {
            return this.plusSeconds(amountToAdd);
        }
        if (unit == ChronoUnit.MINUTES) {
            return this.plusMinutes(amountToAdd);
        }
        if (unit == ChronoUnit.HOURS) {
            return this.plusHours(amountToAdd);
        }
        throw new IllegalArgumentException();
    }

    public Temporal minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        LocalTime end = (LocalTime) endExclusive;
        long nanosDiff = end.toNanoOfDay() - this.toNanoOfDay();
        if (unit == ChronoUnit.NANOS) {
            return nanosDiff;
        }
        if (unit == ChronoUnit.SECONDS) {
            return nanosDiff / NANOS_PER_SECOND;
        }
        if (unit == ChronoUnit.MINUTES) {
            return nanosDiff / (60L * NANOS_PER_SECOND);
        }
        if (unit == ChronoUnit.HOURS) {
            return nanosDiff / (3600L * NANOS_PER_SECOND);
        }
        throw new IllegalArgumentException();
    }

    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.NANO_OF_SECOND, this.nano);
    }

    // --- value-type methods (ISO-8601) ---

    // ISO-8601: HH:mm, plus :ss when seconds or nanos are non-zero, plus a fractional part
    // (3, 6 or 9 digits) when nanos are non-zero — the same layout java.time uses.
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (this.hour < 10) {
            buf.append("0");
        }
        buf.append(Integer.toString(this.hour));
        if (this.minute < 10) {
            buf.append(":0");
        } else {
            buf.append(":");
        }
        buf.append(Integer.toString(this.minute));
        if (this.second > 0 || this.nano > 0) {
            if (this.second < 10) {
                buf.append(":0");
            } else {
                buf.append(":");
            }
            buf.append(Integer.toString(this.second));
            if (this.nano > 0) {
                buf.append(".");
                buf.append(nanoString(this.nano));
            }
        }
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
        if (obj instanceof LocalTime) {
            LocalTime o = (LocalTime) obj;
            return this.hour == o.hour && this.minute == o.minute
                && this.second == o.second && this.nano == o.nano;
        }
        return false;
    }

    public int hashCode() {
        long nod = this.hour * 3600000000000L + this.minute * 60000000000L
            + this.second * 1000000000L + this.nano;
        return (int) (nod ^ (nod >>> 32));
    }

    // --- generic Temporal-typed conveniences ---

    public int get(TemporalField field) {
        return (int) this.getLong(field);
    }

    public LocalTime plus(TemporalAmount amount) {
        return (LocalTime) amount.addTo(this);
    }

    public LocalTime minus(TemporalAmount amount) {
        return (LocalTime) amount.subtractFrom(this);
    }

    public LocalTime with(TemporalAdjuster adjuster) {
        return (LocalTime) adjuster.adjustInto(this);
    }

    // Parses an ISO-8601 time (HH:mm[:ss[.fraction]]).
    public static LocalTime parse(CharSequence text) {
        String s = text.toString();
        int hour = parseDigits(s, 0, 2);
        int minute = parseDigits(s, 3, 5);
        int second = 0;
        int nano = 0;
        if (s.length() > 5 && s.charAt(5) == ':') {
            second = parseDigits(s, 6, 8);
            if (s.length() > 8 && s.charAt(8) == '.') {
                String frac = s.substring(9, s.length());
                while (frac.length() < 9) {
                    frac = frac + "0";
                }
                nano = parseDigits(frac, 0, 9);
            }
        }
        return LocalTime.of(hour, minute, second, nano);
    }

    public String format(DateTimeFormatter formatter) {
        return formatter.format(this);
    }

    private static int parseDigits(String s, int from, int to) {
        int v = 0;
        for (int k = from; k < to; k = k + 1) {
            v = v * 10 + (s.charAt(k) - '0');
        }
        return v;
    }
}
