package java.time.chrono;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;

// KajiLibrary's java.time.chrono.ChronoLocalDateTime — a date-time in an arbitrary calendar system:
// the chronology-agnostic supertype of LocalDateTime. It is exactly a ChronoLocalDate plus a
// LocalTime, and that split is the whole design — the TIME half is calendar-independent (every
// calendar's day has the same 24 hours), so only the date half is generic over the chronology.
//
// A KajiLibrary subset, mirroring the choices already made in ChronoLocalDate: NOT generic in the
// date type (the JDK's `<D extends ChronoLocalDate>` erases to ChronoLocalDate anyway, which is
// what the descriptors below say, and a bounded type variable erases to Object in our compiler —
// finding #100), not Comparable, and without `query`/`format`/`timeLineOrder`/`from`. The covariant
// redeclarations of with/plus/minus are omitted too: the ones inherited from Temporal carry the
// same contract, and each covariant override would only add a bridge method.
public interface ChronoLocalDateTime extends Temporal, TemporalAdjuster, Comparable<ChronoLocalDateTime> {

    ChronoLocalDate toLocalDate();

    LocalTime toLocalTime();

    boolean isSupported(TemporalField field);

    // The chronology comes from the date half — the time half has none. The intermediate is bound
    // to a local instead of chaining `toLocalDate().getChronology()`: a chained call through an
    // interface-typed intermediate is silently dropped (finding #108).
    default Chronology getChronology() {
        ChronoLocalDate date = this.toLocalDate();
        return date.getChronology();
    }

    default boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit != null;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    default Temporal adjustInto(Temporal temporal) {
        ChronoLocalDate date = this.toLocalDate();
        LocalTime time = this.toLocalTime();
        Temporal withDate = temporal.with(ChronoField.EPOCH_DAY, date.toEpochDay());
        return withDate.with(ChronoField.NANO_OF_SECOND, time.toNanoOfDay());
    }

    // An offset turns a local date-time into an instant on the timeline: seconds since the epoch,
    // minus the offset that says how far this local reading runs ahead of UTC.
    default long toEpochSecond(ZoneOffset offset) {
        ChronoLocalDate date = this.toLocalDate();
        LocalTime time = this.toLocalTime();
        long epochDay = date.toEpochDay();
        long secs = epochDay * 86400L + (long) time.toSecondOfDay();
        return secs - (long) offset.getTotalSeconds();
    }

    default Instant toInstant(ZoneOffset offset) {
        LocalTime time = this.toLocalTime();
        return Instant.ofEpochSecond(this.toEpochSecond(offset), (long) time.getNano());
    }

    // Comparisons on the LOCAL reading (date first, then time) — not on the timeline, which needs
    // an offset. `isEqual` is not `equals`: two date-times of different calendars can name the same
    // local instant and still not be equal objects.
    /**
     * The natural order: by date, then by time. A {@code default} because it is one in the JDK,
     * so adding {@link Comparable} to this interface (#276) breaks no implementor.
     *
     * <p>The ordering itself already existed -- {@code isAfter}/{@code isBefore}/{@code isEqual}
     * are built on it. What was missing was the NAME the language knows it by: without
     * {@code Comparable}, none of these could be sorted, put in a {@code TreeSet}, or handed to
     * anything that orders.
     */
    @Override
    default int compareTo(ChronoLocalDateTime other) {
        return LocalOrder.compare(this, other);
    }

    default boolean isAfter(ChronoLocalDateTime other) {
        return LocalOrder.compare(this, other) > 0;
    }

    default boolean isBefore(ChronoLocalDateTime other) {
        return LocalOrder.compare(this, other) < 0;
    }

    default boolean isEqual(ChronoLocalDateTime other) {
        return LocalOrder.compare(this, other) == 0;
    }
}

// The shared ordering behind isAfter/isBefore/isEqual. It lives in a package-private class rather
// than a `private` interface method (Java 9+, and not worth betting the file on) and rather than a
// `default` one, which would be public surface the JDK's interface doesn't have — an EXTRA for the
// gate. The JDK spells this `compareTo`, inherited from Comparable, which this subset omits.
final class LocalOrder {

    private LocalOrder() {
    }

    static int compare(ChronoLocalDateTime self, ChronoLocalDateTime other) {
        ChronoLocalDate selfDate = self.toLocalDate();
        ChronoLocalDate otherDate = other.toLocalDate();
        long a = selfDate.toEpochDay();
        long b = otherDate.toEpochDay();
        int result = 0;
        if (a < b) {
            result = -1;
        } else if (a > b) {
            result = 1;
        } else {
            LocalTime selfTime = self.toLocalTime();
            LocalTime otherTime = other.toLocalTime();
            long ta = selfTime.toNanoOfDay();
            long tb = otherTime.toNanoOfDay();
            if (ta < tb) {
                result = -1;
            } else if (ta > tb) {
                result = 1;
            }
        }
        return result;
    }
}
