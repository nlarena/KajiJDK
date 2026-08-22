package java.time.chrono;

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
}
