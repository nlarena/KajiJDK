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
// finding #100), not Comparable, and without `query`.
public interface ChronoLocalDateTime extends Temporal, TemporalAdjuster, Comparable<ChronoLocalDateTime> {

    ChronoLocalDate toLocalDate();

    LocalTime toLocalTime();

    /**
     * This date and time, formatted with that formatter.
     *
     * @throws java.time.DateTimeException if it cannot be formatted
     */
    default String format(java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        return formatter.format(this);
    }

    /** The date and time `temporal` holds, in whatever calendar it names itself. */
    static ChronoLocalDateTime from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof ChronoLocalDateTime) {
            return (ChronoLocalDateTime) temporal;
        }
        Chronology chrono = temporal.query(java.time.temporal.TemporalQueries.chronology());
        if (chrono == null) {
            // With no calendar declared, ISO: it is what the JDK does when the temporal does not say.
            return java.time.LocalDateTime.from(temporal);
        }
        ChronoLocalDate date = chrono.dateEpochDay(temporal.getLong(ChronoField.EPOCH_DAY));
        LocalTime time = LocalTime.ofNanoOfDay(temporal.getLong(ChronoField.NANO_OF_DAY));
        return ChronoLocalDateTimeImpl.of(date, time);
    }

    /**
     * The order **by timeline alone**, ignoring the calendar.
     *
     * <p>It is the complement of `compareTo`, which breaks ties by calendar. Beware of using it in a
     * `TreeSet`: breaking no ties, two equal moments of different calendars compare 0 and the set
     * keeps only one.
     */
    static java.util.Comparator<ChronoLocalDateTime> timeLineOrder() {
        return new LocalTimeLine();
    }

    boolean isSupported(TemporalField field);

    /**
     * This local date and time **in a zone**, which is what turns them into an instant.
     *
     * <p>A wall-clock reading is not a moment until one says where the clock hangs. This is the
     * method that says it.
     */
    ChronoZonedDateTime atZone(java.time.ZoneId zone);

    // ---- the six covariant redeclarations -------------------------------------------------------
    //
    // They repeat what `Temporal` already declares, but with the return narrowed: adding hours to a
    // date-time is still a date-time, and the caller need not cast. They are also what makes the
    // compiler emit the **bridge methods** in `LocalDateTime`, which narrows further -- without them
    // a call through the interface does not find the implementation.

    ChronoLocalDateTime with(TemporalField field, long newValue);

    ChronoLocalDateTime plus(long amountToAdd, TemporalUnit unit);

    default ChronoLocalDateTime with(TemporalAdjuster adjuster) {
        // Bound to a local: chaining through an interface-typed intermediate gets lost (#108).
        Temporal adjustedOne = adjuster.adjustInto(this);
        return (ChronoLocalDateTime) adjustedOne;
    }

    default ChronoLocalDateTime plus(java.time.temporal.TemporalAmount amount) {
        Temporal added = amount.addTo(this);
        return (ChronoLocalDateTime) added;
    }

    default ChronoLocalDateTime minus(java.time.temporal.TemporalAmount amount) {
        Temporal subtracted = amount.subtractFrom(this);
        return (ChronoLocalDateTime) subtracted;
    }

    default ChronoLocalDateTime minus(long amountToSubtract, TemporalUnit unit) {
        // `Long.MIN_VALUE` cannot be negated: it is subtracted in two steps, as in the JDK.
        if (amountToSubtract == Long.MIN_VALUE) {
            ChronoLocalDateTime half = this.plus(Long.MAX_VALUE, unit);
            return half.plus(1L, unit);
        }
        return this.plus(-amountToSubtract, unit);
    }

    // The chronology comes from the date half — the time half has none. The intermediate is bound
    // to a local instead of chaining `toLocalDate().getChronology()`: a chained call through an
    // interface-typed intermediate is silently dropped (finding #108).
    default Chronology getChronology() {
        ChronoLocalDate date = this.toLocalDate();
        return date.getChronology();
    }

    default boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return unit != ChronoUnit.FOREVER;
        }
        return unit != null && unit.isSupportedBy(this);
    }

    default Temporal adjustInto(Temporal temporal) {
        ChronoLocalDate date = this.toLocalDate();
        LocalTime time = this.toLocalTime();
        Temporal withDate = temporal.with(ChronoField.EPOCH_DAY, date.toEpochDay());
        return withDate.with(ChronoField.NANO_OF_DAY, time.toNanoOfDay());
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

// The comparator `timeLineOrder()` returns: epoch day and nano of day, without looking at the calendar.
final class LocalTimeLine implements java.util.Comparator<ChronoLocalDateTime> {

    public int compare(ChronoLocalDateTime a, ChronoLocalDateTime b) {
        return LocalOrder.compare(a, b);
    }
}
