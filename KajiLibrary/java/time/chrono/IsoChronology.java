package java.time.chrono;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.ValueRange;
import java.util.Arrays;
import java.util.List;

// KajiLibrary's java.time.chrono.IsoChronology — the ISO-8601 calendar system, the default used by
// LocalDate. A singleton reachable through INSTANCE. date/dateEpochDay return LocalDate (covariantly
// over Chronology's ChronoLocalDate) and eraOf returns IsoEra (covariantly over Era).
public final class IsoChronology extends AbstractChronology {

    public static final IsoChronology INSTANCE = new IsoChronology();

    private IsoChronology() {
    }

    public String getId() {
        return "ISO";
    }

    public String getCalendarType() {
        return "iso8601";
    }

    public boolean isLeapYear(long prolepticYear) {
        return (prolepticYear & 3) == 0 && (prolepticYear % 100 != 0 || prolepticYear % 400 == 0);
    }

    public LocalDate date(int prolepticYear, int month, int dayOfMonth) {
        return LocalDate.of(prolepticYear, month, dayOfMonth);
    }

    public LocalDate dateEpochDay(long epochDay) {
        return LocalDate.ofEpochDay(epochDay);
    }

    public IsoEra eraOf(int eraValue) {
        return IsoEra.of(eraValue);
    }

    // ---- lo que el calendario tiene que saber contestar ------------------------------------------

    /**
     * El anio proleptico: para el ISO, el mismo numero con signo.
     *
     * <p>`CE 2024` es 2024; `BCE 100` es -99, no -100, porque no hay anio cero: el anio 1 antes de
     * Cristo es el proleptico 0.
     */
    public int prolepticYear(Era era, int yearOfEra) {
        if (!(era instanceof IsoEra)) {
            throw new ClassCastException("Era must be IsoEra");
        }
        if (era == IsoEra.CE) {
            return yearOfEra;
        }
        return 1 - yearOfEra;
    }

    public LocalDate date(TemporalAccessor temporal) {
        return LocalDate.from(temporal);
    }

    public LocalDate dateYearDay(int prolepticYear, int dayOfYear) {
        return LocalDate.ofYearDay(prolepticYear, dayOfYear);
    }

    public LocalDate dateNow() {
        return LocalDate.now();
    }

    public LocalDate dateNow(ZoneId zone) {
        return LocalDate.now(zone);
    }

    public LocalDate dateNow(Clock clock) {
        return LocalDate.now(clock);
    }

    public LocalDate date(Era era, int yearOfEra, int month, int dayOfMonth) {
        return this.date(this.prolepticYear(era, yearOfEra), month, dayOfMonth);
    }

    public LocalDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
        return this.dateYearDay(this.prolepticYear(era, yearOfEra), dayOfYear);
    }

    /** El ISO **es** el ISO. */
    public boolean isIsoBased() {
        return true;
    }

    /** Los rangos del ISO son los que los campos declaran de fabrica. */
    public ValueRange range(ChronoField field) {
        return field.range();
    }

    public List<Era> eras() {
        return Arrays.asList(new Era[] { IsoEra.BCE, IsoEra.CE });
    }

    // ---- los compuestos, con el tipo del ISO en el retorno ---------------------------------------

    public LocalDateTime localDateTime(TemporalAccessor temporal) {
        return LocalDateTime.from(temporal);
    }

    public ZonedDateTime zonedDateTime(TemporalAccessor temporal) {
        return ZonedDateTime.from(temporal);
    }

    public ZonedDateTime zonedDateTime(Instant instant, ZoneId zone) {
        return ZonedDateTime.ofInstant(instant, zone);
    }

    /** Un `Period` de verdad, no un `ChronoPeriod` generico: el ISO tiene su propia clase. */
    public Period period(int years, int months, int days) {
        return Period.of(years, months, days);
    }

    public LocalDate resolveDate(java.util.Map<java.time.temporal.TemporalField, Long> fieldValues,
            java.time.format.ResolverStyle resolverStyle) {
        // Ligado a una local: encadenar por un intermedio de tipo interfaz se pierde (#108).
        ChronoLocalDate resuelta = super.resolveDate(fieldValues, resolverStyle);
        return (LocalDate) resuelta;
    }
}
