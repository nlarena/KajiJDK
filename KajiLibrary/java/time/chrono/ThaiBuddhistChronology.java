package java.time.chrono;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.ValueRange;
import java.util.Arrays;
import java.util.List;

// KajiLibrary's java.time.chrono.ThaiBuddhistChronology — the Thai Buddhist calendar system, 543
// years ahead of ISO. A singleton reachable through INSTANCE. date/dateEpochDay return
// ThaiBuddhistDate (covariantly over Chronology's ChronoLocalDate) and eraOf returns ThaiBuddhistEra.
// A KajiLibrary subset of the JDK class (the date(Era,…)/dateYearDay/dateNow/localDateTime/
// zonedDateTime/eras/range/resolveDate methods are omitted).
public final class ThaiBuddhistChronology extends AbstractChronology {

    public static final ThaiBuddhistChronology INSTANCE = new ThaiBuddhistChronology();

    private static final int YEARS_DIFFERENCE = 543;

    private ThaiBuddhistChronology() {
    }

    public String getId() {
        return "ThaiBuddhist";
    }

    public String getCalendarType() {
        return "buddhist";
    }

    public ThaiBuddhistDate date(int prolepticYear, int month, int dayOfMonth) {
        return ThaiBuddhistDate.of(prolepticYear, month, dayOfMonth);
    }

    public ThaiBuddhistDate dateEpochDay(long epochDay) {
        LocalDate iso = LocalDate.ofEpochDay(epochDay);
        return ThaiBuddhistDate.of(iso.getYear() + YEARS_DIFFERENCE, iso.getMonthValue(), iso.getDayOfMonth());
    }

    public boolean isLeapYear(long prolepticYear) {
        return IsoChronology.INSTANCE.isLeapYear(prolepticYear - YEARS_DIFFERENCE);
    }

    public ThaiBuddhistEra eraOf(int eraValue) {
        return ThaiBuddhistEra.of(eraValue);
    }

    public int prolepticYear(Era era, int yearOfEra) {
        if (era == ThaiBuddhistEra.BE) {
            return yearOfEra;
        }
        return 1 - yearOfEra;
    }

    // ---- lo que el calendario tiene que saber contestar ------------------------------------------

    public ThaiBuddhistDate date(TemporalAccessor temporal) {
        if (temporal instanceof ThaiBuddhistDate) {
            return (ThaiBuddhistDate) temporal;
        }
        return this.dateEpochDay(temporal.getLong(ChronoField.EPOCH_DAY));
    }

    public ThaiBuddhistDate dateYearDay(int prolepticYear, int dayOfYear) {
        LocalDate iso = LocalDate.ofYearDay(prolepticYear - YEARS_DIFFERENCE, dayOfYear);
        return ThaiBuddhistDate.of(prolepticYear, iso.getMonthValue(), iso.getDayOfMonth());
    }

    public ThaiBuddhistDate dateNow() {
        return this.dateNow(Clock.systemDefaultZone());
    }

    public ThaiBuddhistDate dateNow(ZoneId zone) {
        return this.dateNow(Clock.system(zone));
    }

    public ThaiBuddhistDate dateNow(Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        LocalDate hoy = LocalDate.now(clock);
        return this.dateEpochDay(hoy.toEpochDay());
    }

    public ThaiBuddhistDate date(Era era, int yearOfEra, int month, int dayOfMonth) {
        return this.date(this.prolepticYear(era, yearOfEra), month, dayOfMonth);
    }

    public ThaiBuddhistDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
        return this.dateYearDay(this.prolepticYear(era, yearOfEra), dayOfYear);
    }

    /** Si: los meses, los dias y los anios bisiestos son exactamente los del ISO. */
    public boolean isIsoBased() {
        return true;
    }

    public ValueRange range(ChronoField field) {
        // Igual que el minguo pero al reves: el anio va adelantado, no atrasado.
        if (field == ChronoField.PROLEPTIC_MONTH) {
            ValueRange iso = ChronoField.PROLEPTIC_MONTH.range();
            return ValueRange.of(iso.getMinimum() + (long) YEARS_DIFFERENCE * 12L,
                    iso.getMaximum() + (long) YEARS_DIFFERENCE * 12L);
        }
        if (field == ChronoField.YEAR_OF_ERA) {
            ValueRange iso = ChronoField.YEAR.range();
            return ValueRange.of(1L, iso.getMaximum() + (long) YEARS_DIFFERENCE,
                    (long) YEARS_DIFFERENCE - iso.getMinimum());
        }
        if (field == ChronoField.YEAR) {
            ValueRange iso = ChronoField.YEAR.range();
            return ValueRange.of(iso.getMinimum() + (long) YEARS_DIFFERENCE,
                    iso.getMaximum() + (long) YEARS_DIFFERENCE);
        }
        return field.range();
    }

    public List<Era> eras() {
        return Arrays.asList(new Era[] { ThaiBuddhistEra.BEFORE_BE, ThaiBuddhistEra.BE });
    }

    public ThaiBuddhistDate resolveDate(java.util.Map<java.time.temporal.TemporalField, Long> fieldValues,
            java.time.format.ResolverStyle resolverStyle) {
        // Ligado a una local: encadenar por un intermedio de tipo interfaz se pierde (#108).
        ChronoLocalDate resuelta = super.resolveDate(fieldValues, resolverStyle);
        return (ThaiBuddhistDate) resuelta;
    }
}
