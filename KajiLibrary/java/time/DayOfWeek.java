package java.time;

// KajiLibrary's java.time.DayOfWeek — the seven days, MONDAY (1) … SUNDAY (7) (ISO order). A
// KajiLibrary subset (the JDK's also implements TemporalAccessor/TemporalAdjuster); a plain enum
// with the value and rotation helpers.
public enum DayOfWeek {

    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;

    // Explicit (empty) constructor — see Month (finding #18): without it the enum's synthesised
    // constructor comes out as a degenerate public ()V.
    DayOfWeek() {}

    public int getValue() {
        return this.ordinal() + 1;
    }

    public static DayOfWeek of(int dayOfWeek) {
        return DayOfWeek.values()[dayOfWeek - 1];
    }

    public DayOfWeek plus(long days) {
        int amount = (int) (days % 7);
        return DayOfWeek.values()[(this.ordinal() + (amount + 7)) % 7];
    }

    public DayOfWeek minus(long days) {
        return this.plus(-(days % 7));
    }
}
