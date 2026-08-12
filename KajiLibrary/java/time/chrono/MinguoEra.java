package java.time.chrono;

import java.time.DateTimeException;

// KajiLibrary's java.time.chrono.MinguoEra — the two eras of the Minguo (Republic of China) calendar:
// BEFORE_ROC (value 0) and ROC (value 1, the current era). A KajiLibrary subset: getDisplayName is
// omitted (it needs locale text data).
public enum MinguoEra implements Era {

    BEFORE_ROC,
    ROC;

    public static MinguoEra of(int minguoEra) {
        if (minguoEra == 0) {
            return BEFORE_ROC;
        }
        if (minguoEra == 1) {
            return ROC;
        }
        throw new DateTimeException("Invalid era: " + minguoEra);
    }

    public int getValue() {
        return this.ordinal();
    }
}
