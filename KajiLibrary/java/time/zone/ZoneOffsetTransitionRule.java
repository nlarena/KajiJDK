package java.time.zone;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;

// KajiLibrary's java.time.zone.ZoneOffsetTransitionRule — a transition that RECURS every year,
// which is how a zone's rules extend past the last explicit transition in the database.
//
// tzdb stores concrete transitions up to some year and then a rule like "last Sunday in March at
// 01:00 UTC". This class is that rule, and `createTransition(year)` turns it into a concrete
// ZoneOffsetTransition. Without it, every zone would need a transition row per year forever.
//
// Three things make it fiddly, and all three are real:
//   - the day is "the first DAY-OF-WEEK on or after day N", or with a negative N, on or before the
//     Nth-from-last day of the month;
//   - the time can be 24:00, meaning midnight at the END of the day (`isMidnightEndOfDay`);
//   - the time is stated in one of three frames — UTC, wall time, or standard time — and
//     converting between them is what `timeDefinition` selects.
//
// A KajiLibrary subset: the JDK's `of(...)` factory and `getTimeDefinition()` both mention the
// nested enum `ZoneOffsetTransitionRule.TimeDefinition`, and a nested type does not resolve
// (finding #101), so both are omitted. The definition is carried internally as an int.
public final class ZoneOffsetTransitionRule {

    private final int month;
    private final int dayOfMonthIndicator;
    private final int dayOfWeek;
    private final int secondOfDay;
    private final boolean timeEndOfDay;
    private final int timeDefinition;
    private final int standardOffset;
    private final int offsetBefore;
    private final int offsetAfter;

    ZoneOffsetTransitionRule(int month, int dayOfMonthIndicator, int dayOfWeek, int secondOfDay,
            boolean timeEndOfDay, int timeDefinition, int standardOffset, int offsetBefore,
            int offsetAfter) {
        this.month = month;
        this.dayOfMonthIndicator = dayOfMonthIndicator;
        this.dayOfWeek = dayOfWeek;
        this.secondOfDay = secondOfDay;
        this.timeEndOfDay = timeEndOfDay;
        this.timeDefinition = timeDefinition;
        this.standardOffset = standardOffset;
        this.offsetBefore = offsetBefore;
        this.offsetAfter = offsetAfter;
    }

    public Month getMonth() {
        return Month.of(this.month);
    }

    public int getDayOfMonthIndicator() {
        return this.dayOfMonthIndicator;
    }

    public DayOfWeek getDayOfWeek() {
        if (this.dayOfWeek == 0) {
            return null;
        }
        return DayOfWeek.of(this.dayOfWeek);
    }

    public LocalTime getLocalTime() {
        return LocalTime.ofSecondOfDay((long) this.secondOfDay);
    }

    public boolean isMidnightEndOfDay() {
        return this.timeEndOfDay;
    }

    public ZoneOffset getStandardOffset() {
        return ZoneOffset.ofTotalSeconds(this.standardOffset);
    }

    public ZoneOffset getOffsetBefore() {
        return ZoneOffset.ofTotalSeconds(this.offsetBefore);
    }

    public ZoneOffset getOffsetAfter() {
        return ZoneOffset.ofTotalSeconds(this.offsetAfter);
    }

    // The rule, applied to one year.
    public ZoneOffsetTransition createTransition(int year) {
        LocalDate date = this.ruleDate(year);
        LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.ofSecondOfDay((long) this.secondOfDay));
        if (this.timeEndOfDay) {
            dateTime = dateTime.plusDays(1L);
        }
        // The stated time is in one frame; the transition instant needs it in the "before" frame.
        // UTC: shift by the wall offset. STANDARD: shift by wall minus standard. WALL: already there.
        int shift = 0;
        if (this.timeDefinition == 0) {
            shift = this.offsetBefore;
        } else if (this.timeDefinition == 2) {
            shift = this.offsetBefore - this.standardOffset;
        }
        LocalDateTime local = dateTime.plusSeconds((long) shift);
        long epoch = ZoneMath.toEpochSecond(local, this.offsetBefore);
        return ZoneOffsetTransition.ofRaw(epoch, this.offsetBefore, this.offsetAfter);
    }

    // "The first <dayOfWeek> on or after day N", or with N negative, "on or before the |N|th day
    // from the end of the month".
    private LocalDate ruleDate(int year) {
        LocalDate date;
        if (this.dayOfMonthIndicator < 0) {
            LocalDate firstOfMonth = LocalDate.of(year, this.month, 1);
            int lastDay = firstOfMonth.lengthOfMonth();
            int day = lastDay + 1 + this.dayOfMonthIndicator;
            date = LocalDate.of(year, this.month, day);
            if (this.dayOfWeek != 0) {
                date = ZoneOffsetTransitionRule.previousOrSame(date, this.dayOfWeek);
            }
        } else {
            date = LocalDate.of(year, this.month, this.dayOfMonthIndicator);
            if (this.dayOfWeek != 0) {
                date = ZoneOffsetTransitionRule.nextOrSame(date, this.dayOfWeek);
            }
        }
        return date;
    }

    private static LocalDate nextOrSame(LocalDate date, int dayOfWeek) {
        DayOfWeek current = date.getDayOfWeek();
        int diff = dayOfWeek - current.getValue();
        if (diff < 0) {
            diff = diff + 7;
        }
        return date.plusDays((long) diff);
    }

    private static LocalDate previousOrSame(LocalDate date, int dayOfWeek) {
        DayOfWeek current = date.getDayOfWeek();
        int diff = current.getValue() - dayOfWeek;
        if (diff < 0) {
            diff = diff + 7;
        }
        return date.minusDays((long) diff);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof ZoneOffsetTransitionRule) {
            ZoneOffsetTransitionRule o = (ZoneOffsetTransitionRule) other;
            return this.month == o.month
                    && this.dayOfMonthIndicator == o.dayOfMonthIndicator
                    && this.dayOfWeek == o.dayOfWeek
                    && this.secondOfDay == o.secondOfDay
                    && this.timeEndOfDay == o.timeEndOfDay
                    && this.timeDefinition == o.timeDefinition
                    && this.standardOffset == o.standardOffset
                    && this.offsetBefore == o.offsetBefore
                    && this.offsetAfter == o.offsetAfter;
        }
        return false;
    }

    public int hashCode() {
        return this.month ^ (this.dayOfMonthIndicator << 4) ^ (this.dayOfWeek << 8)
                ^ this.secondOfDay ^ this.offsetAfter;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("TransitionRule[month=");
        buf.append(Integer.toString(this.month));
        buf.append(", dom=");
        buf.append(Integer.toString(this.dayOfMonthIndicator));
        buf.append(", dow=");
        buf.append(Integer.toString(this.dayOfWeek));
        buf.append(", offsetAfter=");
        buf.append(this.getOffsetAfter().toString());
        buf.append("]");
        return buf.toString();
    }
}
