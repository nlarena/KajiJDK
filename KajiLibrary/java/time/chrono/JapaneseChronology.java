package java.time.chrono;

import java.time.LocalDate;

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
}
