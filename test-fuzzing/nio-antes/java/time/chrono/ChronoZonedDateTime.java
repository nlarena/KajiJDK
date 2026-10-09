package java.time.chrono;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;

// KajiLibrary's java.time.chrono.ChronoZonedDateTime — a date-time WITH a zone, in an arbitrary
// calendar system: the chronology-agnostic supertype of ZonedDateTime.
//
// The distinction that earns this its own interface: a ChronoLocalDateTime is a *reading on a
// wall clock*, which is not a point in time until you say where the clock hangs. Adding the zone
// gives an offset, and the offset is what turns the reading into an instant. That is why
// `toEpochSecond()` takes no argument here and takes a ZoneOffset one level up.
//
// The two `with*OffsetAtOverlap` methods exist for the hour that happens TWICE when a zone falls
// back: the same local reading maps to two instants, and something has to choose.
//
// A KajiLibrary subset, following ChronoLocalDate/ChronoLocalDateTime: not generic in the date type
// (the JDK's `<D extends ChronoLocalDate>` erases to what the descriptors below already say), not
// Comparable, and without `query`/`range`.
public interface ChronoZonedDateTime extends Temporal, Comparable<ChronoZonedDateTime> {

    ChronoLocalDateTime toLocalDateTime();

    /**
     * This zoned date and time, formatted with that formatter.
     *
     * @throws java.time.DateTimeException if it cannot be formatted
     */
    default String format(java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        return formatter.format(this);
    }

    ZoneOffset getOffset();

    ZoneId getZone();

    ChronoZonedDateTime withEarlierOffsetAtOverlap();

    ChronoZonedDateTime withLaterOffsetAtOverlap();

    ChronoZonedDateTime withZoneSameLocal(ZoneId zone);

    ChronoZonedDateTime withZoneSameInstant(ZoneId zone);

    boolean isSupported(TemporalField field);

    /** The zoned date and time `temporal` holds, in whatever calendar it names itself. */
    static ChronoZonedDateTime from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof ChronoZonedDateTime) {
            return (ChronoZonedDateTime) temporal;
        }
        return java.time.ZonedDateTime.from(temporal);
    }

    /**
     * The order **by instant alone**, ignoring the zone and the calendar.
     *
     * <p>It is the complement of `compareTo`, which breaks ties by local date and by zone. This one
     * says "the same instant is the same instant", which is what one wants for ordering by when
     * things recorded in different offsets happened.
     *
     * <p>Beware of using it in a `TreeSet`: breaking no ties, ten o'clock in Buenos Aires and two
     * o'clock in London of the same moment compare 0 and the set keeps only one.
     */
    static java.util.Comparator<ChronoZonedDateTime> timeLineOrder() {
        return new ZonedTimeLine();
    }

    // ---- the six covariant redeclarations -------------------------------------------------------
    //
    // `Temporal` already declares these six with a `Temporal` return, so at first sight repeating
    // them looks redundant. It is not: they are what tells the caller that adding hours to a zoned
    // date-time is still a zoned date-time, with no cast. And they are what makes the compiler emit
    // the **bridge methods** in the implementors that narrow the return further --`ZonedDateTime`--
    // which is what lets a call through the interface find the real implementation. Without this
    // declaration the bridges do not exist and `Temporal`'s contract is honoured only by luck.
    //
    // The two abstract ones are the same two that are abstract in the JDK: they are the only ones
    // that **cannot** be written up here, because there is no generic way of applying a field or a
    // unit.

    ChronoZonedDateTime with(TemporalField field, long newValue);

    ChronoZonedDateTime plus(long amountToAdd, TemporalUnit unit);

    default ChronoZonedDateTime with(java.time.temporal.TemporalAdjuster adjuster) {
        // Bound to a local: a call chained through an interface-typed intermediate is silently lost
        // (#108).
        Temporal adjustedOne = adjuster.adjustInto(this);
        return (ChronoZonedDateTime) adjustedOne;
    }

    default ChronoZonedDateTime plus(java.time.temporal.TemporalAmount amount) {
        Temporal added = amount.addTo(this);
        return (ChronoZonedDateTime) added;
    }

    default ChronoZonedDateTime minus(java.time.temporal.TemporalAmount amount) {
        Temporal subtracted = amount.subtractFrom(this);
        return (ChronoZonedDateTime) subtracted;
    }

    default ChronoZonedDateTime minus(long amountToSubtract, TemporalUnit unit) {
        // `Long.MIN_VALUE` cannot be negated: it is subtracted in two steps, as in the JDK.
        if (amountToSubtract == Long.MIN_VALUE) {
            ChronoZonedDateTime half = this.plus(Long.MAX_VALUE, unit);
            return half.plus(1L, unit);
        }
        return this.plus(-amountToSubtract, unit);
    }

    // Each intermediate is bound to a local rather than chained: a chained call through an
    // interface-typed intermediate is silently dropped (finding #108).
    /**
     * The natural order: by instant, then by local date-time, then by zone id.
     *
     * <p>Deliberately NOT the same as {@code isBefore}/{@code isAfter}/{@code isEqual}, which
     * compare the instant and nothing else -- and that difference is the whole point. Two zoned
     * date-times at the same instant in different zones are not equal, so an ordering that stopped
     * at the instant would return 0 for them and a {@code TreeSet} would keep only one. The JDK
     * breaks the tie for exactly that reason, and so does this.
     *
     * <p>A {@code default} because it is one in the JDK, so adding {@link Comparable} (#276)
     * breaks no implementor.
     */
    @Override
    default int compareTo(ChronoZonedDateTime other) {
        int byInstant = InstantOrder.compare(this, other);
        if (byInstant != 0) {
            return byInstant;
        }
        ChronoLocalDateTime mine = this.toLocalDateTime();
        ChronoLocalDateTime theirs = other.toLocalDateTime();
        int byLocal = mine.compareTo(theirs);
        if (byLocal != 0) {
            return byLocal;
        }
        ZoneId zone = this.getZone();
        ZoneId otherZone = other.getZone();
        return zone.getId().compareTo(otherZone.getId());
    }

    default ChronoLocalDate toLocalDate() {
        ChronoLocalDateTime dateTime = this.toLocalDateTime();
        return dateTime.toLocalDate();
    }

    default LocalTime toLocalTime() {
        ChronoLocalDateTime dateTime = this.toLocalDateTime();
        return dateTime.toLocalTime();
    }

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

    // The zone is already fixed, so no offset argument: this is a real point on the timeline.
    default long toEpochSecond() {
        ChronoLocalDateTime dateTime = this.toLocalDateTime();
        ZoneOffset offset = this.getOffset();
        return dateTime.toEpochSecond(offset);
    }

    default Instant toInstant() {
        LocalTime time = this.toLocalTime();
        return Instant.ofEpochSecond(this.toEpochSecond(), (long) time.getNano());
    }

    // Comparisons on the TIMELINE, unlike ChronoLocalDateTime's, which compare wall readings: two
    // zoned date-times in different zones can show different clocks and still be the same instant.
    default boolean isBefore(ChronoZonedDateTime other) {
        return InstantOrder.compare(this, other) < 0;
    }

    default boolean isAfter(ChronoZonedDateTime other) {
        return InstantOrder.compare(this, other) > 0;
    }

    default boolean isEqual(ChronoZonedDateTime other) {
        return InstantOrder.compare(this, other) == 0;
    }
}

// The timeline ordering behind isBefore/isAfter/isEqual — package-private for the same reason as
// ChronoLocalDateTime's LocalOrder: a `default` helper would be public surface the JDK lacks.
final class InstantOrder {

    private InstantOrder() {
    }

    static int compare(ChronoZonedDateTime self, ChronoZonedDateTime other) {
        long a = self.toEpochSecond();
        long b = other.toEpochSecond();
        int result = 0;
        if (a < b) {
            result = -1;
        } else if (a > b) {
            result = 1;
        } else {
            LocalTime selfTime = self.toLocalTime();
            LocalTime otherTime = other.toLocalTime();
            int na = selfTime.getNano();
            int nb = otherTime.getNano();
            if (na < nb) {
                result = -1;
            } else if (na > nb) {
                result = 1;
            }
        }
        return result;
    }
}

// The comparator `timeLineOrder()` returns: the instant alone, with no zone tie-break.
final class ZonedTimeLine implements java.util.Comparator<ChronoZonedDateTime> {

    public int compare(ChronoZonedDateTime a, ChronoZonedDateTime b) {
        return InstantOrder.compare(a, b);
    }
}
