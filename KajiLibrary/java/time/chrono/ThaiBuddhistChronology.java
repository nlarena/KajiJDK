package java.time.chrono;

import java.time.LocalDate;

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
}
