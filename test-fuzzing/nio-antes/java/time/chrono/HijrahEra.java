package java.time.chrono;

import java.time.DateTimeException;

// KajiLibrary's java.time.chrono.HijrahEra — the Hijrah calendar has a single era, AH (Anno
// Hegirae), with value 1. There is no "before" era: the calendar starts at the Hijra and counts
// forward, so unlike Minguo or ThaiBuddhist there is nothing to reinterpret on the other side.
//
// A KajiLibrary subset: getDisplayName (locale text) and range (ValueRange) are omitted.
public enum HijrahEra implements Era {

    AH;

    public static HijrahEra of(int hijrahEra) {
        if (hijrahEra == 1) {
            return AH;
        }
        throw new DateTimeException("Invalid era: " + hijrahEra);
    }

    // Carried as a literal rather than derived from `ordinal()`: a call to a method inherited from
    // an external superclass (java.lang.Enum) is silently dropped by our compiler (finding #120).
    // AH is 1, not 0, so it was never the ordinal anyway.
    public int getValue() {
        return 1;
    }
}
