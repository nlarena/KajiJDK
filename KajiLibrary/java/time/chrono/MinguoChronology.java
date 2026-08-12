package java.time.chrono;

import java.time.LocalDate;

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
}
