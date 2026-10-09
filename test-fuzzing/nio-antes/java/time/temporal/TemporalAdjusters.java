package java.time.temporal;

import java.time.DayOfWeek;

// KajiLibrary's java.time.temporal.TemporalAdjusters — static factories for the common date
// adjusters (first/last day of month/year, and day-of-week relative moves). Each returns a
// TemporalAdjuster backed by one of the package-private strategy classes below. The month/year
// adjusters walk with with()/plus()/minus() instead of leaning on `range()`.
public final class TemporalAdjusters {

    private TemporalAdjusters() {
    }

    /**
     * An adjuster built out of a function over `LocalDate`.
     *
     * <p>It is the door for the adjustments the library does not carry: the rule is written as a
     * date-to-date function and this wraps it in a `TemporalAdjuster` that `with()` accepts.
     *
     * <p>The wrapper converts the `Temporal` it receives to a `LocalDate`, applies the function, and
     * returns the original temporal **adjusted** to the result --not the loose date-- so that
     * adjusting a `LocalDateTime` keeps its time.
     */
    public static TemporalAdjuster ofDateAdjuster(
            java.util.function.UnaryOperator<java.time.LocalDate> dateBasedAdjuster) {
        if (dateBasedAdjuster == null) {
            throw new NullPointerException("dateBasedAdjuster");
        }
        return new DateAdjuster(dateBasedAdjuster);
    }

    public static TemporalAdjuster firstDayOfMonth() {
        return new FieldAdjuster(0);
    }

    public static TemporalAdjuster lastDayOfMonth() {
        return new FieldAdjuster(1);
    }

    public static TemporalAdjuster firstDayOfNextMonth() {
        return new FieldAdjuster(2);
    }

    public static TemporalAdjuster firstDayOfYear() {
        return new FieldAdjuster(3);
    }

    public static TemporalAdjuster lastDayOfYear() {
        return new FieldAdjuster(4);
    }

    public static TemporalAdjuster firstDayOfNextYear() {
        return new FieldAdjuster(5);
    }

    public static TemporalAdjuster firstInMonth(DayOfWeek dayOfWeek) {
        return new DowInMonthAdjuster(1, dayOfWeek.getValue());
    }

    public static TemporalAdjuster lastInMonth(DayOfWeek dayOfWeek) {
        return new DowInMonthAdjuster(-1, dayOfWeek.getValue());
    }

    public static TemporalAdjuster dayOfWeekInMonth(int ordinal, DayOfWeek dayOfWeek) {
        return new DowInMonthAdjuster(ordinal, dayOfWeek.getValue());
    }

    public static TemporalAdjuster next(DayOfWeek dayOfWeek) {
        return new RelativeDowAdjuster(1, dayOfWeek.getValue());
    }

    public static TemporalAdjuster nextOrSame(DayOfWeek dayOfWeek) {
        return new RelativeDowAdjuster(0, dayOfWeek.getValue());
    }

    public static TemporalAdjuster previous(DayOfWeek dayOfWeek) {
        return new RelativeDowAdjuster(3, dayOfWeek.getValue());
    }

    public static TemporalAdjuster previousOrSame(DayOfWeek dayOfWeek) {
        return new RelativeDowAdjuster(2, dayOfWeek.getValue());
    }
}

// first/last day of month/year via field sets + month/year stepping (types 0..5).
final class FieldAdjuster implements TemporalAdjuster {

    private final int type;

    FieldAdjuster(int type) {
        this.type = type;
    }

    public Temporal adjustInto(Temporal temporal) {
        if (this.type == 0) {
            return temporal.with(ChronoField.DAY_OF_MONTH, 1);
        }
        if (this.type == 1) {
            return temporal.with(ChronoField.DAY_OF_MONTH, 1).plus(1, ChronoUnit.MONTHS).minus(1, ChronoUnit.DAYS);
        }
        if (this.type == 2) {
            return temporal.with(ChronoField.DAY_OF_MONTH, 1).plus(1, ChronoUnit.MONTHS);
        }
        if (this.type == 3) {
            return temporal.with(ChronoField.MONTH_OF_YEAR, 1).with(ChronoField.DAY_OF_MONTH, 1);
        }
        if (this.type == 4) {
            return temporal.with(ChronoField.MONTH_OF_YEAR, 12).with(ChronoField.DAY_OF_MONTH, 31);
        }
        return temporal.with(ChronoField.MONTH_OF_YEAR, 1).with(ChronoField.DAY_OF_MONTH, 1).plus(1, ChronoUnit.YEARS);
    }
}

// the nth (or last, ordinal<0) given-day-of-week within the month.
final class DowInMonthAdjuster implements TemporalAdjuster {

    private final int ordinal;
    private final int dowValue;

    DowInMonthAdjuster(int ordinal, int dowValue) {
        this.ordinal = ordinal;
        this.dowValue = dowValue;
    }

    public Temporal adjustInto(Temporal temporal) {
        if (this.ordinal >= 0) {
            Temporal temp = temporal.with(ChronoField.DAY_OF_MONTH, 1);
            int curDow = temp.get(ChronoField.DAY_OF_WEEK);
            int dowDiff = (this.dowValue - curDow + 7) % 7;
            dowDiff = dowDiff + (this.ordinal - 1) * 7;
            return temp.plus(dowDiff, ChronoUnit.DAYS);
        }
        Temporal temp = temporal.with(ChronoField.DAY_OF_MONTH, 1).plus(1, ChronoUnit.MONTHS).minus(1, ChronoUnit.DAYS);
        int curDow = temp.get(ChronoField.DAY_OF_WEEK);
        int daysDiff = this.dowValue - curDow;
        if (daysDiff > 0) {
            daysDiff = daysDiff - 7;
        }
        daysDiff = daysDiff - ((-this.ordinal - 1) * 7);
        return temp.plus(daysDiff, ChronoUnit.DAYS);
    }
}

// next / nextOrSame / previous / previousOrSame a given day-of-week (relative 1/0/3/2).
final class RelativeDowAdjuster implements TemporalAdjuster {

    private final int relative;
    private final int dowValue;

    RelativeDowAdjuster(int relative, int dowValue) {
        this.relative = relative;
        this.dowValue = dowValue;
    }

    public Temporal adjustInto(Temporal temporal) {
        int calDow = temporal.get(ChronoField.DAY_OF_WEEK);
        if (this.relative == 0) {
            int d = (this.dowValue - calDow + 7) % 7;
            return temporal.plus(d, ChronoUnit.DAYS);
        }
        if (this.relative == 1) {
            int d = (this.dowValue - calDow + 7) % 7;
            if (d == 0) {
                d = 7;
            }
            return temporal.plus(d, ChronoUnit.DAYS);
        }
        if (this.relative == 2) {
            int d = (calDow - this.dowValue + 7) % 7;
            return temporal.minus(d, ChronoUnit.DAYS);
        }
        int d = (calDow - this.dowValue + 7) % 7;
        if (d == 0) {
            d = 7;
        }
        return temporal.minus(d, ChronoUnit.DAYS);
    }
}

// The adjuster `ofDateAdjuster` returns: it carries the function and applies it to the temporal's
// date, returning the temporal adjusted to the new date.
final class DateAdjuster implements TemporalAdjuster {

    private final java.util.function.UnaryOperator<java.time.LocalDate> f;

    DateAdjuster(java.util.function.UnaryOperator<java.time.LocalDate> f) {
        this.f = f;
    }

    public Temporal adjustInto(Temporal temporal) {
        java.time.LocalDate current = java.time.LocalDate.from(temporal);
        java.time.LocalDate fresh = this.f.apply(current);
        // The temporal received is adjusted instead of returning the date: that way a
        // `LocalDateTime` keeps its time, which is what `with`'s contract promises.
        return temporal.with(ChronoField.EPOCH_DAY, fresh.toEpochDay());
    }
}
