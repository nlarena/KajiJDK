package java.time.chrono;

import java.time.DateTimeException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;

// KajiLibrary's java.time.chrono.HijrahDate — a date in the Umm al-Qura Hijrah calendar.
//
// Unlike MinguoDate and ThaiBuddhistDate, this one CANNOT be stored as an ISO LocalDate with a
// shifted year: the Hijrah calendar is lunar, so its months do not line up with ISO months at all
// and its year is ~354 days. The date is therefore stored as an epoch day — the one thing both
// calendars agree on — with year/month/day derived through the Umm al-Qura table.
//
// A KajiLibrary subset, mirroring MinguoDate/ThaiBuddhistDate.
public final class HijrahDate implements ChronoLocalDate {

    private final long epochDay;

    private HijrahDate(long epochDay) {
        this.epochDay = epochDay;
    }

    public static HijrahDate of(int prolepticYear, int month, int dayOfMonth) {
        return new HijrahDate(HijrahTable.epochDayOf(prolepticYear, month, dayOfMonth));
    }

    public HijrahChronology getChronology() {
        return HijrahChronology.INSTANCE;
    }

    public HijrahEra getEra() {
        return HijrahEra.of(1);
    }

    public int lengthOfMonth() {
        int year = HijrahTable.yearOfEpochDay(this.epochDay);
        int month = HijrahTable.monthOfEpochDay(this.epochDay);
        return HijrahTable.lengthOfMonth(year, month);
    }

    // Overridden rather than inherited from ChronoLocalDate's default: the default computes 365 or
    // 366, which is the SOLAR year. A lunar year is 354 or 355.
    public int lengthOfYear() {
        return HijrahTable.lengthOfYear(HijrahTable.yearOfEpochDay(this.epochDay));
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.YEAR) {
            return (long) HijrahTable.yearOfEpochDay(this.epochDay);
        }
        if (field == ChronoField.MONTH_OF_YEAR) {
            return (long) HijrahTable.monthOfEpochDay(this.epochDay);
        }
        if (field == ChronoField.DAY_OF_MONTH) {
            return (long) HijrahTable.dayOfEpochDay(this.epochDay);
        }
        if (field == ChronoField.EPOCH_DAY) {
            return this.epochDay;
        }
        if (field == ChronoField.DAY_OF_YEAR) {
            int year = HijrahTable.yearOfEpochDay(this.epochDay);
            return this.epochDay - HijrahTable.epochDayOf(year, 1, 1) + 1L;
        }
        if (field == ChronoField.DAY_OF_WEEK) {
            // The week is calendar-independent: it just runs, so it is derived from the epoch day
            // exactly as in ISO. Day 0 of the epoch (1970-01-01) was a Thursday.
            long dow = (this.epochDay + 3L) % 7L;
            if (dow < 0L) {
                dow = dow + 7L;
            }
            return dow + 1L;
        }
        throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
    }

    public HijrahDate with(TemporalField field, long newValue) {
        int year = HijrahTable.yearOfEpochDay(this.epochDay);
        int month = HijrahTable.monthOfEpochDay(this.epochDay);
        int day = HijrahTable.dayOfEpochDay(this.epochDay);
        HijrahDate result = this;
        if (field == ChronoField.YEAR) {
            result = HijrahDate.of((int) newValue, month, day);
        } else if (field == ChronoField.MONTH_OF_YEAR) {
            result = HijrahDate.of(year, (int) newValue, day);
        } else if (field == ChronoField.DAY_OF_MONTH) {
            result = HijrahDate.of(year, month, (int) newValue);
        } else if (field == ChronoField.EPOCH_DAY) {
            result = new HijrahDate(newValue);
        } else {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return result;
    }

    public HijrahDate with(TemporalAdjuster adjuster) {
        return (HijrahDate) adjuster.adjustInto(this);
    }

    public HijrahDate plus(long amountToAdd, TemporalUnit unit) {
        return new HijrahDate(this.epochDay + HijrahDate.daysIn(amountToAdd, unit, this.epochDay, true));
    }

    public HijrahDate minus(long amountToSubtract, TemporalUnit unit) {
        return new HijrahDate(this.epochDay - HijrahDate.daysIn(amountToSubtract, unit, this.epochDay, false));
    }

    // Months and years are NOT fixed numbers of days in a lunar calendar, so they are walked
    // through the table rather than multiplied.
    private static long daysIn(long amount, TemporalUnit unit, long from, boolean forward) {
        long days = 0L;
        if (unit == ChronoUnit.DAYS) {
            days = amount;
        } else if (unit == ChronoUnit.WEEKS) {
            days = amount * 7L;
        } else if (unit == ChronoUnit.MONTHS) {
            days = HijrahDate.walkMonths(from, amount, forward);
        } else if (unit == ChronoUnit.YEARS) {
            days = HijrahDate.walkMonths(from, amount * 12L, forward);
        } else {
            throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        }
        return days;
    }

    private static long walkMonths(long from, long months, boolean forward) {
        int year = HijrahTable.yearOfEpochDay(from);
        int month = HijrahTable.monthOfEpochDay(from);
        int day = HijrahTable.dayOfEpochDay(from);
        long total = (long) ((year - 1) * 12 + (month - 1));
        if (forward) {
            total = total + months;
        } else {
            total = total - months;
        }
        int newYear = (int) (total / 12L) + 1;
        int newMonth = (int) (total % 12L) + 1;
        int maxDay = HijrahTable.lengthOfMonth(newYear, newMonth);
        int newDay = day;
        if (newDay > maxDay) {
            newDay = maxDay;
        }
        long target = HijrahTable.epochDayOf(newYear, newMonth, newDay);
        long delta = target - from;
        if (!forward) {
            delta = -delta;
        }
        return delta;
    }

    public HijrahDate plus(TemporalAmount amount) {
        return (HijrahDate) amount.addTo(this);
    }

    public HijrahDate minus(TemporalAmount amount) {
        return (HijrahDate) amount.subtractFrom(this);
    }

    public long until(Temporal endExclusive, TemporalUnit unit) {
        HijrahDate end = (HijrahDate) endExclusive;
        long days = end.epochDay - this.epochDay;
        long result = days;
        if (unit == ChronoUnit.WEEKS) {
            result = days / 7L;
        } else if (unit == ChronoUnit.MONTHS) {
            result = HijrahDate.monthsBetween(this.epochDay, end.epochDay);
        } else if (unit == ChronoUnit.YEARS) {
            result = HijrahDate.monthsBetween(this.epochDay, end.epochDay) / 12L;
        }
        return result;
    }

    private static long monthsBetween(long from, long to) {
        int y1 = HijrahTable.yearOfEpochDay(from);
        int m1 = HijrahTable.monthOfEpochDay(from);
        int d1 = HijrahTable.dayOfEpochDay(from);
        int y2 = HijrahTable.yearOfEpochDay(to);
        int m2 = HijrahTable.monthOfEpochDay(to);
        int d2 = HijrahTable.dayOfEpochDay(to);
        long months = (long) ((y2 - y1) * 12 + (m2 - m1));
        if (months > 0L && d2 < d1) {
            months = months - 1L;
        } else if (months < 0L && d2 > d1) {
            months = months + 1L;
        }
        return months;
    }

    public long toEpochDay() {
        return this.epochDay;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof HijrahDate) {
            HijrahDate other = (HijrahDate) obj;
            return this.epochDay == other.epochDay;
        }
        return false;
    }

    public int hashCode() {
        return this.getChronology().getId().hashCode() ^ (int) (this.epochDay ^ (this.epochDay >>> 32));
    }

    // e.g. "Hijrah-umalqura AH 1447-02-24"
    public String toString() {
        int year = HijrahTable.yearOfEpochDay(this.epochDay);
        int month = HijrahTable.monthOfEpochDay(this.epochDay);
        int day = HijrahTable.dayOfEpochDay(this.epochDay);
        StringBuilder buf = new StringBuilder();
        buf.append(this.getChronology().getId());
        buf.append(" AH ");
        buf.append(Integer.toString(year));
        buf.append("-");
        if (month < 10) {
            buf.append("0");
        }
        buf.append(Integer.toString(month));
        buf.append("-");
        if (day < 10) {
            buf.append("0");
        }
        buf.append(Integer.toString(day));
        return buf.toString();
    }
}
