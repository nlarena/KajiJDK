package java.time.chrono;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.ValueRange;
import java.util.Arrays;
import java.util.List;

// KajiLibrary's java.time.chrono.HijrahChronology — the Umm al-Qura Hijrah calendar, the civil
// calendar of Saudi Arabia. A singleton reachable through INSTANCE.
//
// This is the first calendar here that is NOT the ISO calendar with a shifted year: it is lunar,
// its year is 354 or 355 days, and its months come from a table rather than a rule (see
// HijrahTable). That is also why it has a hard supported range — outside the tabulated years there
// is no defined answer, and extrapolating would be inventing a calendar.
//
// A KajiLibrary subset of the JDK class, mirroring MinguoChronology/ThaiBuddhistChronology. The
// JDK's `of(String)` variants for the other Hijrah variants are omitted: we ship one table.
public final class HijrahChronology extends AbstractChronology {

    public static final HijrahChronology INSTANCE = new HijrahChronology();

    private HijrahChronology() {
    }

    public String getId() {
        return "Hijrah-umalqura";
    }

    public String getCalendarType() {
        return "islamic-umalqura";
    }

    public HijrahDate date(int prolepticYear, int month, int dayOfMonth) {
        return HijrahDate.of(prolepticYear, month, dayOfMonth);
    }

    public HijrahDate dateEpochDay(long epochDay) {
        int year = HijrahTable.yearOfEpochDay(epochDay);
        int month = HijrahTable.monthOfEpochDay(epochDay);
        int day = HijrahTable.dayOfEpochDay(epochDay);
        return HijrahDate.of(year, month, day);
    }

    // A lunar leap year is one that runs 355 days instead of 354 — the extra day falls in the
    // twelfth month. It has nothing to do with the ISO leap rule.
    public boolean isLeapYear(long prolepticYear) {
        return HijrahTable.isLeapYear((int) prolepticYear);
    }

    public HijrahEra eraOf(int eraValue) {
        return HijrahEra.of(eraValue);
    }

    // A single era counting forward, so the proleptic year IS the year of era.
    public int prolepticYear(Era era, int yearOfEra) {
        return yearOfEra;
    }

    // ---- what the calendar has to know how to answer ---------------------------------------------

    public HijrahDate date(TemporalAccessor temporal) {
        if (temporal instanceof HijrahDate) {
            return (HijrahDate) temporal;
        }
        return this.dateEpochDay(temporal.getLong(ChronoField.EPOCH_DAY));
    }

    public HijrahDate dateYearDay(int prolepticYear, int dayOfYear) {
        // Hijrah is not ISO shifted: its months come from a table, so the day of the year is
        // converted with the table and not with `LocalDate`.
        return HijrahDate.ofEpochDay(HijrahTable.epochDayOfYearDay(prolepticYear, dayOfYear));
    }

    public HijrahDate dateNow() {
        return this.dateNow(Clock.systemDefaultZone());
    }

    public HijrahDate dateNow(ZoneId zone) {
        return this.dateNow(Clock.system(zone));
    }

    public HijrahDate dateNow(Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        LocalDate today = LocalDate.now(clock);
        return this.dateEpochDay(today.toEpochDay());
    }

    public HijrahDate date(Era era, int yearOfEra, int month, int dayOfMonth) {
        return this.date(this.prolepticYear(era, yearOfEra), month, dayOfMonth);
    }

    public HijrahDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
        return this.dateYearDay(this.prolepticYear(era, yearOfEra), dayOfYear);
    }

    /** No: Hijrah is lunar, its months come from a table and its year lasts 354 or 355 days. */
    public boolean isIsoBased() {
        return false;
    }

    public ValueRange range(ChronoField field) {
        // Hijrah's ranges are its own: its year lasts 354 or 355 days, its months 29 or 30, and they
        // are only defined inside the table. Outside it there is no answer, and extrapolating would
        // be inventing a calendar.
        if (field == ChronoField.DAY_OF_MONTH) {
            return ValueRange.of(1L, 29L, 30L);
        }
        if (field == ChronoField.DAY_OF_YEAR) {
            return ValueRange.of(1L, 354L, 355L);
        }
        if (field == ChronoField.YEAR || field == ChronoField.YEAR_OF_ERA) {
            return ValueRange.of((long) HijrahTable.firstYear(), (long) HijrahTable.lastYear());
        }
        if (field == ChronoField.ERA) {
            return ValueRange.of(1L, 1L);
        }
        if (field == ChronoField.PROLEPTIC_MONTH) {
            return ValueRange.of((long) HijrahTable.firstYear() * 12L,
                    (long) HijrahTable.lastYear() * 12L + 11L);
        }
        return field.range();
    }

    public List<Era> eras() {
        return Arrays.asList(new Era[] { HijrahEra.AH });
    }

    public HijrahDate resolveDate(java.util.Map<java.time.temporal.TemporalField, Long> fieldValues,
            java.time.format.ResolverStyle resolverStyle) {
        // Bound to a local: chaining through an interface-typed intermediate gets lost (#108).
        ChronoLocalDate resolvedOne = super.resolveDate(fieldValues, resolverStyle);
        return (HijrahDate) resolvedOne;
    }
}
