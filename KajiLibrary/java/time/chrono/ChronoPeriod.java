package java.time.chrono;

import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.List;

// KajiLibrary's java.time.chrono.ChronoPeriod — a date-based amount ("3 years, 2 months") in an
// arbitrary calendar system. It is the chronology-agnostic supertype of Period.
//
// Why it can't just be Period: the UNITS are a property of the calendar. An ISO period is
// years/months/days, but a calendar with different cycles reports different units — which is why
// the amount is read through `getUnits()` and `get(unit)` instead of three fixed getters.
//
// A KajiLibrary subset: the static `between(ChronoLocalDate, ChronoLocalDate)` is omitted (it
// dispatches on the chronology of its arguments, which needs `Chronology.period(...)`), as are the
// redeclarations of equals/hashCode/toString — inheriting them from Object is the same contract.
public interface ChronoPeriod extends TemporalAmount {

    Chronology getChronology();

    ChronoPeriod plus(TemporalAmount amountToAdd);

    ChronoPeriod minus(TemporalAmount amountToSubtract);

    ChronoPeriod multipliedBy(int scalar);

    ChronoPeriod normalized();

    // Zero when every one of this calendar's units reads zero. The loop is INDEXED rather than a
    // for-each: the enhanced-for over a KajiLibrary collection erases its element type to Object
    // and is unusable (finding #113).
    default boolean isZero() {
        List<TemporalUnit> units = this.getUnits();
        boolean zero = true;
        int i = 0;
        while (i < units.size()) {
            TemporalUnit unit = units.get(i);
            if (this.get(unit) != 0L) {
                zero = false;
            }
            i = i + 1;
        }
        return zero;
    }

    // Negative when ANY unit is negative — a period is not a single magnitude, so "3 years, -2
    // months" counts as negative even though the years are positive.
    default boolean isNegative() {
        List<TemporalUnit> units = this.getUnits();
        boolean negative = false;
        int i = 0;
        while (i < units.size()) {
            TemporalUnit unit = units.get(i);
            if (this.get(unit) < 0L) {
                negative = true;
            }
            i = i + 1;
        }
        return negative;
    }

    default ChronoPeriod negated() {
        return this.multipliedBy(-1);
    }
}
