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
// isLeapYear/lengthOfYear/isSupported/adjustInto as defaults. A KajiLibrary subset (same omissions as
// ThaiBuddhistDate).
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
     * El periodo entre esta fecha y `endDateExclusive`, en **este** calendario.
     *
     * <p>Se calcula sobre las fechas ISO equivalentes y se devuelve como `ChronoPeriod` de este
     * calendario. La cuenta es la misma --los tres calendarios de esta biblioteca solo renumeran los
     * años, no cambian la longitud de los meses--, y por eso alcanza con delegar; un calendario con
     * meses de otra longitud necesitaria su propia cuenta.
     */
    public ChronoPeriod until(ChronoLocalDate endDateExclusive) {
        if (endDateExclusive == null) {
            throw new NullPointerException("endDateExclusive");
        }
        java.time.LocalDate fin = java.time.LocalDate.ofEpochDay(endDateExclusive.toEpochDay());
        java.time.Period p = java.time.Period.between(
                java.time.LocalDate.ofEpochDay(this.toEpochDay()), fin);
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
}
