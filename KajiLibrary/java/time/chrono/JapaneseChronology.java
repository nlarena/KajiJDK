package java.time.chrono;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.ValueRange;
import java.util.Arrays;
import java.util.List;

// KajiLibrary's java.time.chrono.JapaneseChronology — the Japanese imperial calendar system. A
// singleton reachable through INSTANCE.
//
// It is ISO-based: months, days and leap years are exactly ISO's, and the proleptic year is the ISO
// year. Only the era layer differs — which is why isLeapYear delegates straight to IsoChronology
// while eraOf/prolepticYear carry the era boundary data (see JapaneseDate.EraTable).
//
// A KajiLibrary subset of the JDK class, mirroring MinguoChronology/ThaiBuddhistChronology.
public final class JapaneseChronology extends AbstractChronology {

    public static final JapaneseChronology INSTANCE = new JapaneseChronology();

    private JapaneseChronology() {
    }

    public String getId() {
        return "Japanese";
    }

    public String getCalendarType() {
        return "japanese";
    }

    public JapaneseDate date(int prolepticYear, int month, int dayOfMonth) {
        return JapaneseDate.of(prolepticYear, month, dayOfMonth);
    }

    public JapaneseDate dateEpochDay(long epochDay) {
        LocalDate iso = LocalDate.ofEpochDay(epochDay);
        return JapaneseDate.of(iso.getYear(), iso.getMonthValue(), iso.getDayOfMonth());
    }

    public boolean isLeapYear(long prolepticYear) {
        return IsoChronology.INSTANCE.isLeapYear(prolepticYear);
    }

    public JapaneseEra eraOf(int eraValue) {
        return JapaneseEra.of(eraValue);
    }

    public int prolepticYear(Era era, int yearOfEra) {
        JapaneseEra japaneseEra = (JapaneseEra) era;
        return EraTable.prolepticYear(japaneseEra, yearOfEra);
    }

    // ---- what the calendar has to know how to answer ---------------------------------------------

    public JapaneseDate date(TemporalAccessor temporal) {
        if (temporal instanceof JapaneseDate) {
            return (JapaneseDate) temporal;
        }
        return this.dateEpochDay(temporal.getLong(ChronoField.EPOCH_DAY));
    }

    public JapaneseDate dateYearDay(int prolepticYear, int dayOfYear) {
        LocalDate iso = LocalDate.ofYearDay(prolepticYear, dayOfYear);
        return JapaneseDate.of(prolepticYear, iso.getMonthValue(), iso.getDayOfMonth());
    }

    public JapaneseDate dateNow() {
        return this.dateNow(Clock.systemDefaultZone());
    }

    public JapaneseDate dateNow(ZoneId zone) {
        return this.dateNow(Clock.system(zone));
    }

    public JapaneseDate dateNow(Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        LocalDate today = LocalDate.now(clock);
        return this.dateEpochDay(today.toEpochDay());
    }

    public JapaneseDate date(Era era, int yearOfEra, int month, int dayOfMonth) {
        return this.date(this.prolepticYear(era, yearOfEra), month, dayOfMonth);
    }

    public JapaneseDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
        return this.dateYearDay(this.prolepticYear(era, yearOfEra), dayOfYear);
    }

    /** Yes: the months, the days and the leap years are exactly ISO's. */
    public boolean isIsoBased() {
        return true;
    }

    public ValueRange range(ChronoField field) {
        // The Japanese proleptic year **is** the ISO year: the months, the days and the leap years
        // are the same. The only thing of its own is the era layer, and that is why those are the
        // only two fields with a different range.
        if (field == ChronoField.ERA) {
            return EraTable.eraRange();
        }
        if (field == ChronoField.YEAR_OF_ERA) {
            return EraTable.yearOfEraRange();
        }
        return field.range();
    }

    public List<Era> eras() {
        return EraTable.all();
    }

    public JapaneseDate resolveDate(java.util.Map<java.time.temporal.TemporalField, Long> fieldValues,
            java.time.format.ResolverStyle resolverStyle) {
        // Bound to a local: chaining through an interface-typed intermediate gets lost (#108).
        ChronoLocalDate resolvedOne = super.resolveDate(fieldValues, resolverStyle);
        return (JapaneseDate) resolvedOne;
    }
}
