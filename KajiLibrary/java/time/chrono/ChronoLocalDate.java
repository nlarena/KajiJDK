package java.time.chrono;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

// KajiLibrary's java.time.chrono.ChronoLocalDate — a date in an arbitrary calendar system, the
// chronology-agnostic supertype of LocalDate (and of the Minguo/ThaiBuddhist dates). Mirrors the
// JDK's abstract/default split so concrete calendar dates only implement the calendar-specific
// primitives (getChronology, lengthOfMonth, toEpochDay) plus the Temporal arithmetic, and inherit the
// rest as defaults. A KajiLibrary subset: unlike the JDK it does not extend Comparable, and omits the
// members that would pull in ChronoPeriod / ChronoLocalDateTime (until(ChronoLocalDate), atTime) or
// locale/format machinery (getEra, format, query).
public interface ChronoLocalDate extends Temporal, TemporalAdjuster {

    Chronology getChronology();

    int lengthOfMonth();

    long toEpochDay();

    default boolean isLeapYear() {
        return this.getChronology().isLeapYear(this.getLong(ChronoField.YEAR));
    }

    default int lengthOfYear() {
        if (this.isLeapYear()) {
            return 366;
        }
        return 365;
    }

    default boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return ((ChronoField) field).isDateBased();
        }
        return field != null && field.isSupportedBy(this);
    }

    default boolean isSupported(TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            return ((ChronoUnit) unit).isDateBased();
        }
        return unit != null && unit.isSupportedBy(this);
    }

    default Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.EPOCH_DAY, this.toEpochDay());
    }
}
