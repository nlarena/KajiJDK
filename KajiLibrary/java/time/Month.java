package java.time;

// KajiLibrary's java.time.Month — the twelve months, JANUARY (1) … DECEMBER (12). A KajiLibrary
// subset (the JDK's Month also implements TemporalAccessor/TemporalAdjuster and has firstDayOfYear
// etc.); here it's a plain enum with the value, length, and rotation helpers LocalDate needs.
public enum Month {

    JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE,
    JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER;

    // Explicit (empty) constructor: without one, the frozen javac synthesises a degenerate
    // public ()V constructor for this enum instead of the private (String,int) one (finding #18).
    Month() {}

    // 1..12 (unlike ordinal()'s 0..11).
    public int getValue() {
        return this.ordinal() + 1;
    }

    public static Month of(int month) {
        return Month.values()[month - 1];
    }

    // Days in this month; February depends on whether it's a leap year.
    public int length(boolean leapYear) {
        int o = this.ordinal();
        if (o == 1) {
            return leapYear ? 29 : 28;
        }
        if (o == 3 || o == 5 || o == 8 || o == 10) {
            return 30;
        }
        return 31;
    }

    public Month plus(long months) {
        int amount = (int) (months % 12);
        return Month.values()[(this.ordinal() + (amount + 12)) % 12];
    }

    public Month minus(long months) {
        return this.plus(-(months % 12));
    }
}
