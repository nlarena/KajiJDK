package java.time.chrono;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.ValueRange;
import java.util.Arrays;
import java.util.List;

// KajiLibrary's java.time.chrono.MinguoChronology — the Minguo (Republic of China) calendar system,
// 1911 years behind ISO. A singleton reachable through INSTANCE. date/dateEpochDay return MinguoDate
// (covariantly over Chronology's ChronoLocalDate) and eraOf returns MinguoEra. A KajiLibrary subset of
// the JDK class (same omissions as ThaiBuddhistChronology).
public final class MinguoChronology extends AbstractChronology {

    public static final MinguoChronology INSTANCE = new MinguoChronology();

    private static final int YEARS_DIFFERENCE = 1911;

    private MinguoChronology() {
    }

    public String getId() {
        return "Minguo";
    }

    public String getCalendarType() {
        return "roc";
    }

    public MinguoDate date(int prolepticYear, int month, int dayOfMonth) {
        return MinguoDate.of(prolepticYear, month, dayOfMonth);
    }

    public MinguoDate dateEpochDay(long epochDay) {
        LocalDate iso = LocalDate.ofEpochDay(epochDay);
        return MinguoDate.of(iso.getYear() - YEARS_DIFFERENCE, iso.getMonthValue(), iso.getDayOfMonth());
    }

    public boolean isLeapYear(long prolepticYear) {
        return IsoChronology.INSTANCE.isLeapYear(prolepticYear + YEARS_DIFFERENCE);
    }

    public MinguoEra eraOf(int eraValue) {
        return MinguoEra.of(eraValue);
    }

    public int prolepticYear(Era era, int yearOfEra) {
        if (era == MinguoEra.ROC) {
            return yearOfEra;
        }
        return 1 - yearOfEra;
    }

    // ---- lo que el calendario tiene que saber contestar ------------------------------------------

    public MinguoDate date(TemporalAccessor temporal) {
        if (temporal instanceof MinguoDate) {
            return (MinguoDate) temporal;
        }
        return this.dateEpochDay(temporal.getLong(ChronoField.EPOCH_DAY));
    }

    public MinguoDate dateYearDay(int prolepticYear, int dayOfYear) {
        LocalDate iso = LocalDate.ofYearDay(prolepticYear + YEARS_DIFFERENCE, dayOfYear);
        return MinguoDate.of(prolepticYear, iso.getMonthValue(), iso.getDayOfMonth());
    }

    public MinguoDate dateNow() {
        return this.dateNow(Clock.systemDefaultZone());
    }

    public MinguoDate dateNow(ZoneId zone) {
        return this.dateNow(Clock.system(zone));
    }

    public MinguoDate dateNow(Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        LocalDate hoy = LocalDate.now(clock);
        return this.dateEpochDay(hoy.toEpochDay());
    }

    public MinguoDate date(Era era, int yearOfEra, int month, int dayOfMonth) {
        return this.date(this.prolepticYear(era, yearOfEra), month, dayOfMonth);
    }

    public MinguoDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
        return this.dateYearDay(this.prolepticYear(era, yearOfEra), dayOfYear);
    }

    /** Si: los meses, los dias y los anios bisiestos son exactamente los del ISO. */
    public boolean isIsoBased() {
        return true;
    }

    public ValueRange range(ChronoField field) {
        // Los meses y los dias son los del ISO; lo unico que se corre es el anio, y con el los dos
        // campos que lo cuentan.
        if (field == ChronoField.PROLEPTIC_MONTH) {
            ValueRange iso = ChronoField.PROLEPTIC_MONTH.range();
            return ValueRange.of(iso.getMinimum() - (long) YEARS_DIFFERENCE * 12L,
                    iso.getMaximum() - (long) YEARS_DIFFERENCE * 12L);
        }
        if (field == ChronoField.YEAR_OF_ERA) {
            ValueRange iso = ChronoField.YEAR.range();
            return ValueRange.of(1L, iso.getMaximum() - (long) YEARS_DIFFERENCE,
                    (long) YEARS_DIFFERENCE - iso.getMinimum());
        }
        if (field == ChronoField.YEAR) {
            ValueRange iso = ChronoField.YEAR.range();
            return ValueRange.of(iso.getMinimum() - (long) YEARS_DIFFERENCE,
                    iso.getMaximum() - (long) YEARS_DIFFERENCE);
        }
        return field.range();
    }

    public List<Era> eras() {
        return Arrays.asList(new Era[] { MinguoEra.BEFORE_ROC, MinguoEra.ROC });
    }

    public MinguoDate resolveDate(java.util.Map<java.time.temporal.TemporalField, Long> fieldValues,
            java.time.format.ResolverStyle resolverStyle) {
        // Ligado a una local: encadenar por un intermedio de tipo interfaz se pierde (#108).
        ChronoLocalDate resuelta = super.resolveDate(fieldValues, resolverStyle);
        return (MinguoDate) resuelta;
    }
}
