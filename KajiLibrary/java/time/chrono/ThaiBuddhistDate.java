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
// isLeapYear/lengthOfYear/isSupported/adjustInto as defaults. A KajiLibrary subset: now()/from()/
// range()/atTime()/until(ChronoLocalDate) are omitted (they need Clock, TemporalAccessor.from,
// ValueRange plumbing, or the ChronoLocalDateTime/ChronoPeriod types).
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

    // ---- las cuatro entradas que faltaban --------------------------------------------------------
    //
    // `now()` y `from(...)` son las dos formas de conseguir una fecha sin escribir sus numeros: una
    // la saca del reloj, la otra la traduce de otro temporal. Sin ellas, la unica manera de tener
    // una ThaiBuddhistDate de hoy era calcular a mano el anio budista, que es justo lo que la clase existe
    // para no tener que hacer.

    /** Hoy, en la zona por defecto del sistema. */
    public static ThaiBuddhistDate now() {
        return ThaiBuddhistDate.deIso(LocalDate.now());
    }

    /** Hoy en esa zona. */
    public static ThaiBuddhistDate now(java.time.ZoneId zone) {
        return ThaiBuddhistDate.deIso(LocalDate.now(zone));
    }

    /** Hoy **segun ese reloj**, que es la forma que se puede probar con un `Clock.fixed`. */
    public static ThaiBuddhistDate now(java.time.Clock clock) {
        return ThaiBuddhistDate.deIso(LocalDate.now(clock));
    }

    /**
     * La fecha que `temporal` tiene, leida en este calendario.
     *
     * @throws java.time.DateTimeException si `temporal` no lleva una fecha
     */
    public static ThaiBuddhistDate from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof ThaiBuddhistDate) {
            return (ThaiBuddhistDate) temporal;
        }
        return ThaiBuddhistDate.deIso(LocalDate.from(temporal));
    }

    // El puente desde el ISO, que es como esta clase esta guardada por dentro.
    private static ThaiBuddhistDate deIso(LocalDate iso) {
        return ThaiBuddhistDate.of(iso.getYear() + YEARS_DIFFERENCE, iso.getMonthValue(), iso.getDayOfMonth());
    }
}
