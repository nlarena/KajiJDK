package java.time.chrono;

import java.time.LocalDate;

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
}
