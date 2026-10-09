package java.time.chrono;

import java.time.LocalDate;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.ChronoField;

// KajiLibrary's java.time.chrono.ThaiBuddhistDate — a date in the Thai Buddhist calendar, which runs
// 543 years ahead of the ISO calendar and is otherwise identical. Stored as the equivalent ISO
// LocalDate; only the year (and era) are reinterpreted. Implements ChronoLocalDate, inheriting
// isLeapYear/lengthOfYear/isSupported/adjustInto as defaults.
//
// This note used to list now()/from()/range()/atTime()/until(ChronoLocalDate) as omitted. Four of
// the five answer today: now(), from() and until(ChronoLocalDate) are declared below, and atTime()
// comes from ChronoLocalDate as a default. What is still missing is range(ChronoField): it is not
// overridden here, so it gives ChronoField's generic range rather than one refined by this
// calendar.
public final class ThaiBuddhistDate implements ChronoLocalDate {

    private static final int YEARS_DIFFERENCE = 543;

    private final LocalDate isoDate;

    private ThaiBuddhistDate(LocalDate isoDate) {
        this.isoDate = isoDate;
    }

    public static ThaiBuddhistDate of(int prolepticYear, int month, int dayOfMonth) {
        return new ThaiBuddhistDate(LocalDate.of(prolepticYear - YEARS_DIFFERENCE, month, dayOfMonth));
    }

    public ThaiBuddhistChronology getChronology() {
        return ThaiBuddhistChronology.INSTANCE;
    }

    public ThaiBuddhistEra getEra() {
        if (this.isoDate.getYear() + YEARS_DIFFERENCE >= 1) {
            return ThaiBuddhistEra.BE;
        }
        return ThaiBuddhistEra.BEFORE_BE;
    }

    public int lengthOfMonth() {
        return this.isoDate.lengthOfMonth();
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.YEAR) {
            return this.isoDate.getYear() + YEARS_DIFFERENCE;
        }
        return this.isoDate.getLong(field);
    }

    public ThaiBuddhistDate with(TemporalField field, long newValue) {
        if (field == ChronoField.YEAR) {
            return new ThaiBuddhistDate((LocalDate) this.isoDate.with(ChronoField.YEAR, newValue - YEARS_DIFFERENCE));
        }
        return new ThaiBuddhistDate((LocalDate) this.isoDate.with(field, newValue));
    }

    public ThaiBuddhistDate with(TemporalAdjuster adjuster) {
        return (ThaiBuddhistDate) adjuster.adjustInto(this);
    }

    public ThaiBuddhistDate plus(long amountToAdd, TemporalUnit unit) {
        return new ThaiBuddhistDate((LocalDate) this.isoDate.plus(amountToAdd, unit));
    }

    public ThaiBuddhistDate minus(long amountToSubtract, TemporalUnit unit) {
        return new ThaiBuddhistDate((LocalDate) this.isoDate.minus(amountToSubtract, unit));
    }

    public ThaiBuddhistDate plus(TemporalAmount amount) {
        return (ThaiBuddhistDate) amount.addTo(this);
    }

    public ThaiBuddhistDate minus(TemporalAmount amount) {
        return (ThaiBuddhistDate) amount.subtractFrom(this);
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        ThaiBuddhistDate end = (ThaiBuddhistDate) endExclusive;
        return this.isoDate.until(end.isoDate, unit);
    }

    /**
     * The period between this date and `endDateExclusive`, in **this** calendar.
     *
     * <p>It is computed over the equivalent ISO dates and returned as a `ChronoPeriod` of this
     * calendar. The sum is the same --this library's three calendars only renumber the years, they
     * do not change the months' lengths-- and that is why delegating is enough; a calendar with
     * months of another length would need a sum of its own.
     */
    public ChronoPeriod until(ChronoLocalDate endDateExclusive) {
        if (endDateExclusive == null) {
            throw new NullPointerException("endDateExclusive");
        }
        java.time.LocalDate end = java.time.LocalDate.ofEpochDay(endDateExclusive.toEpochDay());
        java.time.Period p = java.time.Period.between(
                java.time.LocalDate.ofEpochDay(this.toEpochDay()), end);
        return new ChronoPeriodImpl(this.getChronology(), p.getYears(), p.getMonths(), p.getDays());
    }

    public long toEpochDay() {
        return this.isoDate.toEpochDay();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ThaiBuddhistDate) {
            ThaiBuddhistDate other = (ThaiBuddhistDate) obj;
            return this.isoDate.equals(other.isoDate);
        }
        return false;
    }

    public int hashCode() {
        return this.getChronology().getId().hashCode() ^ this.isoDate.hashCode();
    }

    // e.g. "ThaiBuddhist BE 2569-08-04" (chronology, era, year-of-era, then -MM-dd zero-padded).
    public String toString() {
        long prolepticYear = this.isoDate.getYear() + YEARS_DIFFERENCE;
        long yearOfEra;
        String era;
        if (prolepticYear >= 1) {
            yearOfEra = prolepticYear;
            era = "BE";
        } else {
            yearOfEra = 1 - prolepticYear;
            era = "BEFORE_BE";
        }
        int month = this.isoDate.getMonthValue();
        int day = this.isoDate.getDayOfMonth();
        StringBuilder buf = new StringBuilder();
        buf.append(this.getChronology().getId());
        buf.append(" ");
        buf.append(era);
        buf.append(" ");
        buf.append(Long.toString(yearOfEra));
        buf.append("-");
        if (month < 10) {
            buf.append("0");
        }
        buf.append(Integer.toString(month));
        buf.append("-");
        if (day < 10) {
            buf.append("0");
        }
        buf.append(Integer.toString(day));
        return buf.toString();
    }

    // ---- the four entry points that were missing -------------------------------------------------
    //
    // `now()` and `from(...)` are the two ways of getting a date without writing its numbers: one
    // takes it from the clock, the other translates it from another temporal. Without them, the only
    // way of having today's ThaiBuddhistDate was to work out the Buddhist year by hand, which is
    // exactly what the class exists to save.

    /** Today, in the system's default zone. */
    public static ThaiBuddhistDate now() {
        return ThaiBuddhistDate.fromIso(LocalDate.now());
    }

    /** Today in that zone. */
    public static ThaiBuddhistDate now(java.time.ZoneId zone) {
        return ThaiBuddhistDate.fromIso(LocalDate.now(zone));
    }

    /** Today **according to that clock**, the form that can be tested with a `Clock.fixed`. */
    public static ThaiBuddhistDate now(java.time.Clock clock) {
        return ThaiBuddhistDate.fromIso(LocalDate.now(clock));
    }

    /**
     * The date `temporal` holds, read in this calendar.
     *
     * @throws java.time.DateTimeException if `temporal` carries no date
     */
    public static ThaiBuddhistDate from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof ThaiBuddhistDate) {
            return (ThaiBuddhistDate) temporal;
        }
        return ThaiBuddhistDate.fromIso(LocalDate.from(temporal));
    }

    // The bridge from ISO, which is how this class is stored inside.
    private static ThaiBuddhistDate fromIso(LocalDate iso) {
        return ThaiBuddhistDate.of(iso.getYear() + YEARS_DIFFERENCE, iso.getMonthValue(), iso.getDayOfMonth());
    }
}
