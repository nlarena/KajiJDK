package java.time.chrono;

import java.time.LocalDate;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.ChronoField;

// KajiLibrary's java.time.chrono.MinguoDate — a date in the Minguo (Republic of China) calendar, which
// runs 1911 years behind the ISO calendar and is otherwise identical. Stored as the equivalent ISO
// LocalDate; only the year (and era) are reinterpreted. Implements ChronoLocalDate, inheriting
// isLeapYear/lengthOfYear/isSupported/adjustInto as defaults. The same surface as ThaiBuddhistDate,
// including its one gap: range(ChronoField) is not overridden, so it gives ChronoField's generic
// range rather than one refined by this calendar.
public final class MinguoDate implements ChronoLocalDate {

    private static final int YEARS_DIFFERENCE = 1911;

    private final LocalDate isoDate;

    private MinguoDate(LocalDate isoDate) {
        this.isoDate = isoDate;
    }

    public static MinguoDate of(int prolepticYear, int month, int dayOfMonth) {
        return new MinguoDate(LocalDate.of(prolepticYear + YEARS_DIFFERENCE, month, dayOfMonth));
    }

    public MinguoChronology getChronology() {
        return MinguoChronology.INSTANCE;
    }

    public MinguoEra getEra() {
        if (this.isoDate.getYear() - YEARS_DIFFERENCE >= 1) {
            return MinguoEra.ROC;
        }
        return MinguoEra.BEFORE_ROC;
    }

    public int lengthOfMonth() {
        return this.isoDate.lengthOfMonth();
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.YEAR) {
            return this.isoDate.getYear() - YEARS_DIFFERENCE;
        }
        return this.isoDate.getLong(field);
    }

    public MinguoDate with(TemporalField field, long newValue) {
        if (field == ChronoField.YEAR) {
            return new MinguoDate((LocalDate) this.isoDate.with(ChronoField.YEAR, newValue + YEARS_DIFFERENCE));
        }
        return new MinguoDate((LocalDate) this.isoDate.with(field, newValue));
    }

    public MinguoDate with(TemporalAdjuster adjuster) {
        return (MinguoDate) adjuster.adjustInto(this);
    }

    public MinguoDate plus(long amountToAdd, TemporalUnit unit) {
        return new MinguoDate((LocalDate) this.isoDate.plus(amountToAdd, unit));
    }

    public MinguoDate minus(long amountToSubtract, TemporalUnit unit) {
        return new MinguoDate((LocalDate) this.isoDate.minus(amountToSubtract, unit));
    }

    public MinguoDate plus(TemporalAmount amount) {
        return (MinguoDate) amount.addTo(this);
    }

    public MinguoDate minus(TemporalAmount amount) {
        return (MinguoDate) amount.subtractFrom(this);
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        MinguoDate end = (MinguoDate) endExclusive;
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
        if (obj instanceof MinguoDate) {
            MinguoDate other = (MinguoDate) obj;
            return this.isoDate.equals(other.isoDate);
        }
        return false;
    }

    public int hashCode() {
        return this.getChronology().getId().hashCode() ^ this.isoDate.hashCode();
    }

    // e.g. "Minguo ROC 114-08-04" (chronology, era, year-of-era, then -MM-dd zero-padded).
    public String toString() {
        long prolepticYear = this.isoDate.getYear() - YEARS_DIFFERENCE;
        long yearOfEra;
        String era;
        if (prolepticYear >= 1) {
            yearOfEra = prolepticYear;
            era = "ROC";
        } else {
            yearOfEra = 1 - prolepticYear;
            era = "BEFORE_ROC";
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
    // way of having today's MinguoDate was to work out the Minguo year by hand, which is exactly what
    // the class exists to save.

    /** Today, in the system's default zone. */
    public static MinguoDate now() {
        return MinguoDate.fromIso(LocalDate.now());
    }

    /** Today in that zone. */
    public static MinguoDate now(java.time.ZoneId zone) {
        return MinguoDate.fromIso(LocalDate.now(zone));
    }

    /** Today **according to that clock**, the form that can be tested with a `Clock.fixed`. */
    public static MinguoDate now(java.time.Clock clock) {
        return MinguoDate.fromIso(LocalDate.now(clock));
    }

    /**
     * The date `temporal` holds, read in this calendar.
     *
     * @throws java.time.DateTimeException if `temporal` carries no date
     */
    public static MinguoDate from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof MinguoDate) {
            return (MinguoDate) temporal;
        }
        return MinguoDate.fromIso(LocalDate.from(temporal));
    }

    // The bridge from ISO, which is how this class is stored inside.
    private static MinguoDate fromIso(LocalDate iso) {
        return MinguoDate.of(iso.getYear() - YEARS_DIFFERENCE, iso.getMonthValue(), iso.getDayOfMonth());
    }
}
