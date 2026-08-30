package java.time.chrono;

import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;

// KajiLibrary's java.time.chrono.JapaneseDate — a date in the Japanese imperial calendar. Stored as
// the equivalent ISO LocalDate: the months and days are identical to ISO, and only the ERA and the
// year-of-era are reinterpreted (see JapaneseEra for why that reinterpretation is data-driven).
//
// The proleptic year IS the ISO year, exactly as in the JDK — so `getLong(YEAR)` needs no shift,
// unlike MinguoDate. The era-relative year is the derived quantity, and it is what resets to 1
// mid-calendar-year at an era boundary.
//
// Supported from Meiji 6 (1873-01-01), the JDK's own lower bound: before that, Japan used a
// lunisolar calendar, so ISO month/day mapping would be a lie.
//
// A KajiLibrary subset, mirroring MinguoDate/ThaiBuddhistDate.
public final class JapaneseDate implements ChronoLocalDate {

    private final LocalDate isoDate;

    private JapaneseDate(LocalDate isoDate) {
        this.isoDate = isoDate;
    }

    public static JapaneseDate of(int prolepticYear, int month, int dayOfMonth) {
        LocalDate iso = LocalDate.of(prolepticYear, month, dayOfMonth);
        if (iso.toEpochDay() < EraTable.firstSupportedEpochDay()) {
            throw new java.time.DateTimeException("JapaneseDate before Meiji 6 is not supported");
        }
        return new JapaneseDate(iso);
    }

    // The era-relative form: Heisei 31 and Reiwa 1 are both 2019, distinguished only by the era.
    public static JapaneseDate of(JapaneseEra era, int yearOfEra, int month, int dayOfMonth) {
        int prolepticYear = EraTable.prolepticYear(era, yearOfEra);
        return JapaneseDate.of(prolepticYear, month, dayOfMonth);
    }

    public JapaneseChronology getChronology() {
        return JapaneseChronology.INSTANCE;
    }

    public JapaneseEra getEra() {
        return EraTable.eraOf(this.isoDate.toEpochDay());
    }

    public int lengthOfMonth() {
        return this.isoDate.lengthOfMonth();
    }

    public long getLong(TemporalField field) {
        return this.isoDate.getLong(field);
    }

    public JapaneseDate with(TemporalField field, long newValue) {
        return new JapaneseDate((LocalDate) this.isoDate.with(field, newValue));
    }

    public JapaneseDate with(TemporalAdjuster adjuster) {
        return (JapaneseDate) adjuster.adjustInto(this);
    }

    public JapaneseDate plus(long amountToAdd, TemporalUnit unit) {
        return new JapaneseDate((LocalDate) this.isoDate.plus(amountToAdd, unit));
    }

    public JapaneseDate minus(long amountToSubtract, TemporalUnit unit) {
        return new JapaneseDate((LocalDate) this.isoDate.minus(amountToSubtract, unit));
    }

    public JapaneseDate plus(TemporalAmount amount) {
        return (JapaneseDate) amount.addTo(this);
    }

    public JapaneseDate minus(TemporalAmount amount) {
        return (JapaneseDate) amount.subtractFrom(this);
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        JapaneseDate end = (JapaneseDate) endExclusive;
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
        if (obj instanceof JapaneseDate) {
            JapaneseDate other = (JapaneseDate) obj;
            return this.isoDate.equals(other.isoDate);
        }
        return false;
    }

    public int hashCode() {
        return this.getChronology().getId().hashCode() ^ this.isoDate.hashCode();
    }

    // e.g. "Japanese Reiwa 8-08-18" — chronology, era name, year-of-era, then -MM-dd zero-padded.
    public String toString() {
        long epochDay = this.isoDate.toEpochDay();
        JapaneseEra era = EraTable.eraOf(epochDay);
        int yearOfEra = EraTable.yearOfEra(era, this.isoDate.getYear());
        int month = this.isoDate.getMonthValue();
        int day = this.isoDate.getDayOfMonth();
        StringBuilder buf = new StringBuilder();
        buf.append(this.getChronology().getId());
        buf.append(" ");
        buf.append(EraTable.name(era));
        buf.append(" ");
        buf.append(Integer.toString(yearOfEra));
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

// The era boundaries — the part of this calendar that is DATA rather than arithmetic.
//
// Each era's first ISO day, extracted from the JDK by scanning JapaneseDate.getEra() day by day:
//
//   Meiji  (-1)  from 1873-01-01  (Meiji 6; the JDK supports no earlier Japanese date)
//   Taisho  (0)  from 1912-07-30
//   Showa   (1)  from 1926-12-25
//   Heisei  (2)  from 1989-01-08
//   Reiwa   (3)  from 2019-05-01
//
// The boundaries are computed from LocalDate rather than stored as `static final long` epoch-day
// constants: a static-final primitive reads back as 0 at runtime (finding #112), which would put
// every date in the first era.
final class EraTable {

    private EraTable() {
    }

    static long firstSupportedEpochDay() {
        return LocalDate.of(1873, 1, 1).toEpochDay();
    }

    static long taishoStart() {
        return LocalDate.of(1912, 7, 30).toEpochDay();
    }

    static long showaStart() {
        return LocalDate.of(1926, 12, 25).toEpochDay();
    }

    static long heiseiStart() {
        return LocalDate.of(1989, 1, 8).toEpochDay();
    }

    static long reiwaStart() {
        return LocalDate.of(2019, 5, 1).toEpochDay();
    }

    static JapaneseEra eraOf(long epochDay) {
        JapaneseEra era = JapaneseEra.of(-1);
        if (epochDay >= EraTable.reiwaStart()) {
            era = JapaneseEra.of(3);
        } else if (epochDay >= EraTable.heiseiStart()) {
            era = JapaneseEra.of(2);
        } else if (epochDay >= EraTable.showaStart()) {
            era = JapaneseEra.of(1);
        } else if (epochDay >= EraTable.taishoStart()) {
            era = JapaneseEra.of(0);
        }
        return era;
    }

    // The ISO year in which each era began — the anchor for converting between the era-relative
    // year and the proleptic one. Year-of-era 1 is the year the era started, so the arithmetic is
    // `prolepticYear = startYear + yearOfEra - 1`.
    static int startYear(JapaneseEra era) {
        int value = era.getValue();
        int year = 1868;
        if (value == 0) {
            year = 1912;
        } else if (value == 1) {
            year = 1926;
        } else if (value == 2) {
            year = 1989;
        } else if (value == 3) {
            year = 2019;
        }
        return year;
    }

    static int prolepticYear(JapaneseEra era, int yearOfEra) {
        return EraTable.startYear(era) + yearOfEra - 1;
    }

    static int yearOfEra(JapaneseEra era, int prolepticYear) {
        return prolepticYear - EraTable.startYear(era) + 1;
    }

    static String name(JapaneseEra era) {
        int value = era.getValue();
        String name = "Meiji";
        if (value == 0) {
            name = "Taisho";
        } else if (value == 1) {
            name = "Showa";
        } else if (value == 2) {
            name = "Heisei";
        } else if (value == 3) {
            name = "Reiwa";
        }
        return name;
    }
}
