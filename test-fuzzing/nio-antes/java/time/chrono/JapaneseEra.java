package java.time.chrono;

import java.time.DateTimeException;

// KajiLibrary's java.time.chrono.JapaneseEra — the eras of the Japanese imperial calendar.
//
// What makes this calendar different from Minguo or ThaiBuddhist: those are the ISO calendar with a
// constant added to the year, so their eras are decoration. Here the era CHANGES WHEN AN EMPEROR
// DOES, on a date nobody can compute — and the year-of-era resets to 1 in the middle of a calendar
// year. 1989-01-07 is Showa 64; 1989-01-08 is Heisei 1. That is why the boundaries below are data,
// not arithmetic, and why the data has to be extended by hand every time an era ends.
//
// The five values and their boundary dates were extracted from the JDK (JapaneseEra.values() and a
// day-by-day scan of JapaneseDate.getEra()), not transcribed from a reference.
//
// A KajiLibrary subset: getDisplayName (locale text) and range (ValueRange) are omitted.
public enum JapaneseEra implements Era {

    // The era value is carried explicitly rather than derived from `ordinal()`: a call to a method
    // inherited from an EXTERNAL superclass — java.lang.Enum here — is silently dropped by our
    // compiler (finding #120). MEIJI is -1 in the JDK's numbering, so the offsets are not ordinals
    // anyway.
    MEIJI(-1),
    TAISHO(0),
    SHOWA(1),
    HEISEI(2),
    REIWA(3);

    private final int value;

    JapaneseEra(int value) {
        this.value = value;
    }

    public static JapaneseEra of(int japaneseEra) {
        if (japaneseEra == -1) {
            return MEIJI;
        }
        if (japaneseEra == 0) {
            return TAISHO;
        }
        if (japaneseEra == 1) {
            return SHOWA;
        }
        if (japaneseEra == 2) {
            return HEISEI;
        }
        if (japaneseEra == 3) {
            return REIWA;
        }
        throw new DateTimeException("Invalid era: " + japaneseEra);
    }

    public int getValue() {
        return this.value;
    }
}
