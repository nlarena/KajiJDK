package java.time.chrono;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ValueRange;

// KajiLibrary's java.time.chrono.ChronoLocalDateTimeImpl -- a local date and time in a calendar
// **that is not ISO**: exactly a `ChronoLocalDate` plus a `LocalTime`, which is what the interface
// says.
//
// It exists for a concrete reason: without it, `minguoDate.atTime(time)` returned a `LocalDateTime`,
// and a `LocalDateTime` **belongs to the ISO calendar**. The result compiled, looked right, and its
// `getChronology()` answered `ISO` over a Minguo date. A member that lies.
//
// The date/time split is the whole design: the time half does not depend on the calendar --a day of
// any calendar has the same 24 hours-- so the time arithmetic is done over the `LocalTime`, the days
// that overflow are counted, and they are added to the date. The date never learns that hours exist
// and the time never learns that calendars do.
//
// It is package-private: nobody names it from outside, it is obtained through `atTime` or through
// `Chronology.localDateTime(...)`, as in the JDK.
final class ChronoLocalDateTimeImpl implements ChronoLocalDateTime {

    private final ChronoLocalDate date;
    private final LocalTime time;

    private ChronoLocalDateTimeImpl(ChronoLocalDate date, LocalTime time) {
        this.date = date;
        this.time = time;
    }

    static ChronoLocalDateTime of(ChronoLocalDate date, LocalTime time) {
        if (date == null) {
            throw new NullPointerException("date");
        }
        if (time == null) {
            throw new NullPointerException("time");
        }
        // ISO has a class of its own, and a better one: `LocalDateTime` knows things this does not.
        if (date instanceof java.time.LocalDate) {
            return java.time.LocalDateTime.of((java.time.LocalDate) date, time);
        }
        return new ChronoLocalDateTimeImpl(date, time);
    }

    public ChronoLocalDate toLocalDate() {
        return this.date;
    }

    public LocalTime toLocalTime() {
        return this.time;
    }

    // **It does not go through `of`**, and that is the class's only subtlety. `of` diverts an ISO
    // date to `LocalDateTime`, which is right for building from outside; but the `with`/`plus` here
    // promise to return a `ChronoLocalDateTimeImpl` --a narrowed return, as in the JDK-- so they have
    // to build one. There is no contradiction: an `Impl` **never** carries an ISO date, because `of`
    // would have diverted it before building one, and no operation changes calendar.
    //
    // That it cannot change calendar is exactly what `ensureSameChronology` checks.
    private ChronoLocalDateTimeImpl resolveLocal(ChronoLocalDate newDate, LocalTime newTime) {
        if (this.date == newDate && this.time == newTime) {
            return this;
        }
        this.sameCalendar(newDate);
        return new ChronoLocalDateTimeImpl(newDate, newTime);
    }

    // That the new date belongs to the **same** calendar as this one. Without this, adjusting a
    // Minguo date with a `LocalDate` would give an object that says it is Minguo and carries an ISO
    // date inside --and the narrowed return, which promises an `Impl`, would be the least of it--. The
    // JDK throws `ClassCastException` in this case, with this message.
    private void sameCalendar(ChronoLocalDate otherOne) {
        Chronology mine = this.date.getChronology();
        Chronology theirs = otherOne.getChronology();
        if (!mine.equals(theirs)) {
            throw new ClassCastException("Chronology mismatch, expected: " + mine.getId()
                    + ", actual: " + theirs.getId());
        }
    }

    // The JDK's `ensureValid`: what a generic operation --`adjustInto`, `addTo`-- returned has to
    // still be a date and time of **this** calendar.
    private ChronoLocalDateTimeImpl ensureSameChronology(Temporal result) {
        ChronoLocalDateTime cldt = (ChronoLocalDateTime) result;
        ChronoLocalDate date = cldt.toLocalDate();
        this.sameCalendar(date);
        if (cldt instanceof ChronoLocalDateTimeImpl) {
            return (ChronoLocalDateTimeImpl) cldt;
        }
        return new ChronoLocalDateTimeImpl(date, cldt.toLocalTime());
    }

    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            return f.isDateBased() || f.isTimeBased();
        }
        return field != null && field.isSupportedBy(this);
    }

    public ValueRange range(TemporalField field) {
        if (field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            if (f.isTimeBased()) {
                return this.time.range(field);
            }
            return this.date.range(field);
        }
        return field.rangeRefinedBy(this);
    }

    public int get(TemporalField field) {
        if (field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            if (f.isTimeBased()) {
                return this.time.get(field);
            }
            return this.date.get(field);
        }
        return (int) field.getFrom(this);
    }

    public long getLong(TemporalField field) {
        if (field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            if (f.isTimeBased()) {
                return this.time.getLong(field);
            }
            return this.date.getLong(field);
        }
        return field.getFrom(this);
    }

    public ChronoLocalDateTimeImpl with(TemporalField field, long newValue) {
        if (field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            if (f.isTimeBased()) {
                return this.resolveLocal(this.date, this.time.with(field, newValue));
            }
            return this.resolveLocal(this.date.with(field, newValue), this.time);
        }
        Temporal adjustedOne = field.adjustInto(this, newValue);
        return this.ensureSameChronology(adjustedOne);
    }

    public ChronoLocalDateTimeImpl with(TemporalAdjuster adjuster) {
        if (adjuster instanceof ChronoLocalDate) {
            return this.resolveLocal((ChronoLocalDate) adjuster, this.time);
        }
        if (adjuster instanceof LocalTime) {
            return this.resolveLocal(this.date, (LocalTime) adjuster);
        }
        if (adjuster instanceof ChronoLocalDateTime) {
            return this.ensureSameChronology((Temporal) adjuster);
        }
        Temporal adjustedOne = adjuster.adjustInto(this);
        return this.ensureSameChronology(adjustedOne);
    }

    public ChronoLocalDateTimeImpl plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit) {
            ChronoUnit u = (ChronoUnit) unit;
            if (u == ChronoUnit.DAYS) {
                return this.resolveLocal(this.date.plus(amountToAdd, ChronoUnit.DAYS), this.time);
            }
            if (u.isDateBased()) {
                // Months, years and the rest: they are the calendar's business, the time does not change.
                return this.resolveLocal(this.date.plus(amountToAdd, unit), this.time);
            }
            return this.plusNanosOf(amountToAdd, u);
        }
        Temporal added = unit.addTo(this, amountToAdd);
        return this.ensureSameChronology(added);
    }

    // It adds in nanos and **carries the overflowing days into the date**, which is the only thing
    // joining the two halves. The floor is taken with downward division: adding -1 hour to midnight
    // has to land on the previous day, not stay on the same one with a negative time.
    private ChronoLocalDateTimeImpl plusNanosOf(long count, ChronoUnit unit) {
        long nanosPerUnit = nanosOf(unit);
        long total = this.time.toNanoOfDay() + count * nanosPerUnit;
        long day = Math.floorDiv(total, 86400000000000L);
        long remainder = Math.floorMod(total, 86400000000000L);
        ChronoLocalDate newDate = this.date;
        if (day != 0L) {
            newDate = this.date.plus(day, ChronoUnit.DAYS);
        }
        return this.resolveLocal(newDate, LocalTime.ofNanoOfDay(remainder));
    }

    private static long nanosOf(ChronoUnit unit) {
        if (unit == ChronoUnit.NANOS) {
            return 1L;
        }
        if (unit == ChronoUnit.MICROS) {
            return 1000L;
        }
        if (unit == ChronoUnit.MILLIS) {
            return 1000000L;
        }
        if (unit == ChronoUnit.SECONDS) {
            return 1000000000L;
        }
        if (unit == ChronoUnit.MINUTES) {
            return 60000000000L;
        }
        if (unit == ChronoUnit.HOURS) {
            return 3600000000000L;
        }
        if (unit == ChronoUnit.HALF_DAYS) {
            return 43200000000000L;
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }

    public ChronoZonedDateTime atZone(ZoneId zone) {
        return ChronoZonedDateTimeImpl.of(this, zone);
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        ChronoLocalDateTime end = (ChronoLocalDateTime) endExclusive;
        if (unit instanceof ChronoUnit) {
            ChronoUnit u = (ChronoUnit) unit;
            LocalTime endTime = end.toLocalTime();
            ChronoLocalDate endDate = end.toLocalDate();
            if (u.isDateBased()) {
                // A day is not complete if the arrival time is earlier: one is taken off.
                ChronoLocalDate adjusted = endDate;
                if (endTime.toNanoOfDay() < this.time.toNanoOfDay()) {
                    adjusted = endDate.minus(1L, ChronoUnit.DAYS);
                }
                return this.date.until(adjusted, unit);
            }
            long days = this.date.until(endDate, ChronoUnit.DAYS);
            long nanos = days * 86400000000000L + endTime.toNanoOfDay() - this.time.toNanoOfDay();
            return nanos / nanosOf(u);
        }
        return unit.between(this, end);
    }

    public <R> R query(TemporalQuery<R> query) {
        if (query == java.time.temporal.TemporalQueries.localDate()) {
            return (R) java.time.LocalDate.ofEpochDay(this.date.toEpochDay());
        }
        if (query == java.time.temporal.TemporalQueries.localTime()) {
            return (R) this.time;
        }
        if (query == java.time.temporal.TemporalQueries.chronology()) {
            return (R) this.date.getChronology();
        }
        if (query == java.time.temporal.TemporalQueries.precision()) {
            return (R) ChronoUnit.NANOS;
        }
        return query.queryFrom(this);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ChronoLocalDateTime) {
            ChronoLocalDateTime other = (ChronoLocalDateTime) obj;
            ChronoLocalDate theirDate = other.toLocalDate();
            LocalTime theirTime = other.toLocalTime();
            return this.date.equals(theirDate) && this.time.equals(theirTime);
        }
        return false;
    }

    public int hashCode() {
        return this.date.hashCode() ^ this.time.hashCode();
    }

    public String toString() {
        return this.date.toString() + "T" + this.time.toString();
    }
}
