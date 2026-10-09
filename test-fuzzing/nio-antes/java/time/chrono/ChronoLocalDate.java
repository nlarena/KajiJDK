package java.time.chrono;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

// KajiLibrary's java.time.chrono.ChronoLocalDate — a date in an arbitrary calendar system, the
// chronology-agnostic supertype of LocalDate (and of the Minguo/ThaiBuddhist dates). Mirrors the
// JDK's abstract/default split so concrete calendar dates only implement the calendar-specific
// primitives (getChronology, lengthOfMonth, toEpochDay) plus the Temporal arithmetic, and inherit the
// rest as defaults.
public interface ChronoLocalDate extends Temporal, TemporalAdjuster, Comparable<ChronoLocalDate> {

    Chronology getChronology();

    /**
     * The period between this date and `endDateExclusive`, in **this** one's calendar.
     *
     * <p>Abstract and not `default` because the answer depends on the calendar: "a month" does not
     * mean the same in ISO as in Hijrah, and there is no generic sum that serves both.
     */
    ChronoPeriod until(ChronoLocalDate endDateExclusive);

    // ---- the comparisons across calendars -------------------------------------------------------
    //
    // All three compare by **epoch day**, not by year/month/day, and that is the only way for a
    // comparison across calendars to mean anything: a Japanese 1st of January and an ISO one are the
    // same day if they fall at the same point on the line, however each of them numbers it.

    default boolean isAfter(ChronoLocalDate other) {
        return this.toEpochDay() > other.toEpochDay();
    }

    default boolean isBefore(ChronoLocalDate other) {
        return this.toEpochDay() < other.toEpochDay();
    }

    /**
     * Whether they name the **same day**, even if they belong to different calendars.
     *
     * <p>Unlike `equals`, which also demands the same calendar. It is the difference between "it is
     * the same day" and "it is the same date", and that is why both exist.
     */
    default boolean isEqual(ChronoLocalDate other) {
        return this.toEpochDay() == other.toEpochDay();
    }

    /**
     * It answers the standard queries.
     *
     * <p>The one that matters is `chronology()`: a date is the only thing that knows which calendar
     * it belongs to, and without this method `Chronology.from(minguoDate)` answered **ISO**. The
     * reason is the same as `TemporalQueries`' bug: the marker queries are recognised **by
     * identity**, and if nobody recognises them the generic `queryFrom` returns `null` -- which here
     * ended up at ISO by default.
     *
     * <p>The three that return `null` on purpose are needed too: a date has **no** zone, no offset
     * and no time, and saying `null` is different from letting the generic `queryFrom` guess.
     */
    default <R> R query(java.time.temporal.TemporalQuery<R> query) {
        if (query == java.time.temporal.TemporalQueries.zoneId()
                || query == java.time.temporal.TemporalQueries.zone()
                || query == java.time.temporal.TemporalQueries.offset()
                || query == java.time.temporal.TemporalQueries.localTime()) {
            return null;
        }
        if (query == java.time.temporal.TemporalQueries.chronology()) {
            return (R) this.getChronology();
        }
        if (query == java.time.temporal.TemporalQueries.precision()) {
            return (R) ChronoUnit.DAYS;
        }
        return query.queryFrom(this);
    }

    /** This date's era, according to its calendar. */
    default Era getEra() {
        return this.getChronology().eraOf(this.get(ChronoField.ERA));
    }

    /**
     * This date with that time, **in this calendar**.
     *
     * <p>This used to return a `LocalDateTime` built from the epoch day, and a `LocalDateTime`
     * belongs to the ISO calendar: `minguoDate.atTime(time).getChronology()` answered `ISO` over a
     * Minguo date. The epoch day was right and everything else lied. Now ISO still gives a
     * `LocalDateTime` --which knows more-- and the other calendars give the implementation that
     * keeps them.
     */
    default ChronoLocalDateTime atTime(java.time.LocalTime localTime) {
        if (localTime == null) {
            throw new NullPointerException("localTime");
        }
        return ChronoLocalDateTimeImpl.of(this, localTime);
    }

    /**
     * This date formatted with that formatter.
     *
     * @throws java.time.DateTimeException if it cannot be formatted
     */
    default String format(java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        return formatter.format(this);
    }

    // ---- the narrowed returns -------------------------------------------------------------------
    //
    // The six repeat `Temporal`'s with the return narrowed to `ChronoLocalDate`. They are not
    // ornament: without them, `date.plus(1, DAYS)` over a `ChronoLocalDate` reference returns a
    // `Temporal` and has to be cast. And they are what makes the compiler emit the **bridge methods**
    // in every concrete implementation -- without the bridges, a call through the supertype ends in
    // `NoSuchMethodError`.

    // The three `Temporal` declares **abstract** are re-declared narrowed, with no body: every
    // concrete calendar already implements them, and this only changes the type the caller sees.
    ChronoLocalDate plus(long amountToAdd, TemporalUnit unit);

    ChronoLocalDate minus(long amountToSubtract, TemporalUnit unit);

    ChronoLocalDate with(TemporalField field, long newValue);

    // And the three `Temporal` declares `default` repeat **its very body**, not a call to it.
    //
    // The JDK writes `Temporal.super.plus(amount)`, the qualified form of calling a superinterface's
    // default (§15.12.1). Our parser does not accept it yet, and calling a plain `plus(amount)` would
    // be infinite recursion -- this method **is** the most specific one. Repeating the body is one
    // line and does exactly the same; it is noted here in case one day it can be written that way.
    default ChronoLocalDate plus(java.time.temporal.TemporalAmount amount) {
        return (ChronoLocalDate) amount.addTo(this);
    }

    default ChronoLocalDate minus(java.time.temporal.TemporalAmount amount) {
        return (ChronoLocalDate) amount.subtractFrom(this);
    }

    default ChronoLocalDate with(TemporalAdjuster adjuster) {
        return (ChronoLocalDate) adjuster.adjustInto(this);
    }

    /** The date `temporal` holds, in whatever calendar it names itself. */
    static ChronoLocalDate from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof ChronoLocalDate) {
            return (ChronoLocalDate) temporal;
        }
        return java.time.LocalDate.from(temporal);
    }

    /**
     * The order **by timeline alone**, ignoring the calendar.
     *
     * <p>It is the complement of `compareTo`, which breaks ties by calendar. This one says "the same
     * day is the same day", and serves to order dates from mixed calendars by when they happened.
     *
     * <p>Beware of using it in a `TreeSet`: breaking no ties, two dates of the same day and different
     * calendar compare 0 and the set keeps only one.
     */
    static java.util.Comparator<ChronoLocalDate> timeLineOrder() {
        return new TimeLine();
    }

    int lengthOfMonth();

    long toEpochDay();

    /**
     * The natural order: by epoch day, and -- when two dates of DIFFERENT calendars name the same
     * day -- by chronology id. That tie-break is not decoration: without it two dates that are not
     * {@code equals} would compare 0, and a {@code TreeSet} would silently drop one of them.
     *
     * <p>A {@code default} because it is one in the JDK, so adding {@link Comparable} (#276)
     * breaks no implementor.
     */
    @Override
    default int compareTo(ChronoLocalDate other) {
        long mine = this.toEpochDay();
        long theirs = other.toEpochDay();
        if (mine < theirs) {
            return -1;
        }
        if (mine > theirs) {
            return 1;
        }
        Chronology chrono = this.getChronology();
        Chronology otherChrono = other.getChronology();
        return chrono.getId().compareTo(otherChrono.getId());
    }

    default boolean isLeapYear() {
        return this.getChronology().isLeapYear(this.getLong(ChronoField.YEAR));
    }

    default int lengthOfYear() {
        if (this.isLeapYear()) {
            return 366;
        }
        return 365;
    }

    default boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return ((ChronoField) field).isDateBased();
        }
        return field != null && field.isSupportedBy(this);
    }

    default boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return ((ChronoUnit) unit).isDateBased();
        }
        return unit != null && unit.isSupportedBy(this);
    }

    default Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.EPOCH_DAY, this.toEpochDay());
    }
}

// The comparator `timeLineOrder()` returns: the epoch day alone, with no calendar tie-break.
final class TimeLine implements java.util.Comparator<ChronoLocalDate> {

    public int compare(ChronoLocalDate a, ChronoLocalDate b) {
        return Long.compare(a.toEpochDay(), b.toEpochDay());
    }
}
