package java.time.chrono;

import java.time.DateTimeException;

// KajiLibrary's java.time.chrono.ThaiBuddhistEra — the two eras of the Thai Buddhist calendar:
// BEFORE_BE (value 0) and BE (value 1, the current era). A KajiLibrary subset: getDisplayName is
// omitted (it needs locale text data).
public enum ThaiBuddhistEra implements Era {

    BEFORE_BE,
    BE;

    public static ThaiBuddhistEra of(int thaiBuddhistEra) {
        if (thaiBuddhistEra == 0) {
            return BEFORE_BE;
        }
        if (thaiBuddhistEra == 1) {
            return BE;
        }
        throw new DateTimeException("Invalid era: " + thaiBuddhistEra);
    }

    public int getValue() {
        return this.ordinal();
    }
}
