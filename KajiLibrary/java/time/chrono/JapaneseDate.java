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
    // ---- the four entry points that were missing -------------------------------------------------

    /** Today, in the system's default zone. */
    public static JapaneseDate now() {
        return JapaneseDate.fromIso(java.time.LocalDate.now());
    }

    /** Today in that zone. */
    public static JapaneseDate now(java.time.ZoneId zone) {
        return JapaneseDate.fromIso(java.time.LocalDate.now(zone));
    }

    /** Today **according to that clock**, the form that can be tested with a `Clock.fixed`. */
    public static JapaneseDate now(java.time.Clock clock) {
        return JapaneseDate.fromIso(java.time.LocalDate.now(clock));
    }

    /**
     * The date `temporal` holds, read in the Japanese calendar.
     *
     * @throws java.time.DateTimeException if `temporal` carries no date, or if it falls before the
     *     start of the Meiji era
     */
    public static JapaneseDate from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof JapaneseDate) {
            return (JapaneseDate) temporal;
        }
        return JapaneseDate.fromIso(java.time.LocalDate.from(temporal));
    }

    // The Japanese proleptic year **is** the ISO one --the only thing of its own is the era layer--
    // so there is no shift to undo here, unlike Minguo or Thai Buddhist.
    private static JapaneseDate fromIso(java.time.LocalDate iso) {
        return JapaneseDate.of(iso.getYear(), iso.getMonthValue(), iso.getDayOfMonth());
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

    /** The eras supported, from the oldest to the most recent. */
    static java.util.List<Era> all() {
        return java.util.Arrays.asList(new Era[] {
            JapaneseEra.MEIJI, JapaneseEra.TAISHO, JapaneseEra.SHOWA, JapaneseEra.HEISEI,
            JapaneseEra.REIWA,
        });
    }

    /**
     * The `ERA` field's range.
     *
     * <p>The Japanese eras' values start at -1 (Meiji) and not at 0, which is the JDK's numbering;
     * the maximum is the current era's and **grows when there is a new era**, the only part of this
     * calendar that depends on a fact of the world.
     */
    static java.time.temporal.ValueRange eraRange() {
        return java.time.temporal.ValueRange.of((long) JapaneseEra.MEIJI.getValue(),
                (long) JapaneseEra.REIWA.getValue());
    }

    /**
     * The `YEAR_OF_ERA` field's range.
     *
     * <p>The minimum is 1 --every era starts at its year 1-- and the maximum is the longest era's,
     * which is Showa with 64 years. It is a **loose** range: it says how large the field can get in
     * some era, not how large it gets in a given date's.
     */
    static java.time.temporal.ValueRange yearOfEraRange() {
        return java.time.temporal.ValueRange.of(1L, 64L);
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
