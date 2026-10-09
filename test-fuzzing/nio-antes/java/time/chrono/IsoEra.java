package java.time.chrono;

import java.time.DateTimeException;

// KajiLibrary's java.time.chrono.IsoEra — the two eras of the ISO calendar system: BCE (before
// current era, numeric value 0) and CE (current era, value 1).
public enum IsoEra implements Era {

    BCE,
    CE;

    public static IsoEra of(int isoEra) {
        if (isoEra == 0) {
            return BCE;
        }
        if (isoEra == 1) {
            return CE;
        }
        throw new DateTimeException("Invalid era: " + isoEra);
    }

    public int getValue() {
        return this.ordinal();
    }
}
