package java.time.zone;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

// Local-time <-> epoch-second conversions for the zone package.
//
// These live here because KajiLibrary's LocalDateTime does not expose `toEpochSecond(ZoneOffset)`
// or `ofEpochSecond(...)` — the JDK's do. Rather than widen LocalDateTime's public surface (which
// the gate would accept, but which is a change to a class already validated against the JDK), the
// zone package keeps its own arithmetic. It is four lines either way.
final class ZoneMath {

    private ZoneMath() {
    }

    static long toEpochSecond(LocalDateTime dateTime, int offsetSeconds) {
        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();
        long secs = date.toEpochDay() * 86400L + (long) time.toSecondOfDay();
        return secs - (long) offsetSeconds;
    }

    static LocalDateTime ofEpochSecond(long epochSecond, int offsetSeconds) {
        long local = epochSecond + (long) offsetSeconds;
        // Floor division: `/` and `%` truncate toward zero, which would be wrong before 1970.
        long epochDay = local / 86400L;
        long secondOfDay = local % 86400L;
        if (secondOfDay < 0L) {
            secondOfDay = secondOfDay + 86400L;
            epochDay = epochDay - 1L;
        }
        LocalDate date = LocalDate.ofEpochDay(epochDay);
        LocalTime time = LocalTime.ofSecondOfDay(secondOfDay);
        return LocalDateTime.of(date, time);
    }

    static ZoneOffset offsetOf(int totalSeconds) {
        return ZoneOffset.ofTotalSeconds(totalSeconds);
    }
}
