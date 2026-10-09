package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

// A time zone with daylight-saving rules expressed as two dates of the year: when it starts and
// when it ends. It is the JDK's only concrete public TimeZone, and the only way of writing a zone by
// hand without the IANA database.
//
// What makes it useful -- and what makes it complicated -- is that the rules are not given as fixed
// dates but as **patterns**, because the inner transitions fall on days of the week: "the second
// Sunday in March", "the last Sunday in October". There are four ways of saying it, and the JDK
// encodes them in the signs of two integers instead of having four fields:
//
//   day   dow   mode               example
//   ---   ---   ----------------   ----------------------------------------------------------
//    >0     0   DOM                the 15th of the month
//    >0    >0   DOW_IN_MONTH       the 2nd Sunday (day=2, dow=SUNDAY)
//    <0    >0   DOW_IN_MONTH       the LAST Sunday (day=-1)
//    >0    <0   DOW_GE_DOM         the first Sunday ON OR AFTER the 8th
//    <0    <0   DOW_LE_DOM         the last Sunday ON OR BEFORE the 21st
//
// That encoding is history, not design, but it is contract: the public constructors take those
// integers and they have to be decoded just as the JDK does. `setStartRule`/`setEndRule` are the
// readable face of the same thing.
//
// The other detail that gets overlooked is the **time's mode**. `startTime` may be given in wall
// time (the usual), in standard time, or in UTC, and the three mean different instants -- precisely
// because the clock jumps at that moment. Here everything is normalised to **local standard** time,
// which is the clock `getOffset`'s arguments arrive on.
//
// **A deliberate divergence**: the JDK accepts `startYear` and applies the rule only from that year
// onwards, but it does **not** model historical transitions -- a SimpleTimeZone says the same thing
// for 1970 as for 2030. This one does too. For inner historical dates the tzdb is needed, and there
// is none (see FixedTimeZone's note).
public class SimpleTimeZone extends TimeZone {

    // The three modes a rule's time can be given in.
    public static final int WALL_TIME = 0;
    public static final int STANDARD_TIME = 1;
    public static final int UTC_TIME = 2;

    // The four rule modes, decoded from the signs. Internal.
    private static final int DOM_MODE = 1;
    private static final int DOW_IN_MONTH_MODE = 2;
    private static final int DOW_GE_DOM_MODE = 3;
    private static final int DOW_LE_DOM_MODE = 4;

    private static final int MS_PER_DAY = 86400000;
    private static final int ONE_HOUR = 3600000;

    private static final int[] MONTH_LENGTHS = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    private int rawOffset;
    private int dstSavings;

    private int startMonth;
    private int startDay;
    private int startDayOfWeek;
    private int startTime;
    private int startTimeMode;

    private int endMonth;
    private int endDay;
    private int endDayOfWeek;
    private int endTime;
    private int endTimeMode;

    // The first year the rule holds in. Before it, the zone is of constant offset.
    private int startYear;

    private boolean useDaylight;
    private int startMode;
    private int endMode;

    // A zone with no daylight saving: constant offset.
    public SimpleTimeZone(int rawOffset, String ID) {
        this.rawOffset = rawOffset;
        this.setID(ID);
        this.dstSavings = ONE_HOUR;
        this.useDaylight = false;
    }

    public SimpleTimeZone(int rawOffset, String ID, int startMonth, int startDay,
            int startDayOfWeek, int startTime, int endMonth, int endDay, int endDayOfWeek,
            int endTime) {
        this(rawOffset, ID, startMonth, startDay, startDayOfWeek, startTime, WALL_TIME,
                endMonth, endDay, endDayOfWeek, endTime, WALL_TIME, ONE_HOUR);
    }

    public SimpleTimeZone(int rawOffset, String ID, int startMonth, int startDay,
            int startDayOfWeek, int startTime, int endMonth, int endDay, int endDayOfWeek,
            int endTime, int dstSavings) {
        this(rawOffset, ID, startMonth, startDay, startDayOfWeek, startTime, WALL_TIME,
                endMonth, endDay, endDayOfWeek, endTime, WALL_TIME, dstSavings);
    }

    public SimpleTimeZone(int rawOffset, String ID, int startMonth, int startDay,
            int startDayOfWeek, int startTime, int startTimeMode, int endMonth, int endDay,
            int endDayOfWeek, int endTime, int endTimeMode, int dstSavings) {
        this.rawOffset = rawOffset;
        this.setID(ID);
        this.startMonth = startMonth;
        this.startDay = startDay;
        this.startDayOfWeek = startDayOfWeek;
        this.startTime = startTime;
        this.startTimeMode = startTimeMode;
        this.endMonth = endMonth;
        this.endDay = endDay;
        this.endDayOfWeek = endDayOfWeek;
        this.endTime = endTime;
        this.endTimeMode = endTimeMode;
        this.dstSavings = dstSavings;
        this.startYear = 0;
        this.decodeRules();
    }

    // ---- decoding the rules            -----------------------------------------------------------

    // It translates the signs of (day, dayOfWeek) into one of the four modes, **normalising** the
    // two fields to positive where called for. It is destructive on purpose: the JDK stores the
    // already decoded values, and `hasSameRules` compares them that way.
    private void decodeRules() {
        this.useDaylight = this.startDay != 0 && this.endDay != 0;
        if (!this.useDaylight) {
            return;
        }
        this.startMode = this.decodeOne(true);
        this.endMode = this.decodeOne(false);
    }

    private int decodeOne(boolean isStartOf) {
        int dayNum = isStartOf ? this.startDay : this.endDay;
        int dow = isStartOf ? this.startDayOfWeek : this.endDayOfWeek;
        int monthNum = isStartOf ? this.startMonth : this.endMonth;
        if (monthNum < Calendar.JANUARY || monthNum > Calendar.DECEMBER) {
            throw new IllegalArgumentException("Illegal month " + monthNum);
        }
        int mode;
        if (dow == 0) {
            mode = DOM_MODE;
        } else if (dow > 0) {
            mode = DOW_IN_MONTH_MODE;
        } else {
            // negative dow: "on or after" if the day is positive, "on or before" if it is
            // negative.
            dow = -dow;
            if (dayNum > 0) {
                mode = DOW_GE_DOM_MODE;
            } else {
                dayNum = -dayNum;
                mode = DOW_LE_DOM_MODE;
            }
        }
        if (dow > Calendar.SATURDAY) {
            throw new IllegalArgumentException("Illegal day of week " + dow);
        }
        if (mode == DOW_IN_MONTH_MODE) {
            if (dayNum < -5 || dayNum > 5) {
                throw new IllegalArgumentException("Illegal day of week in month " + dayNum);
            }
        } else if (dayNum < 1 || dayNum > MONTH_LENGTHS[monthNum]) {
            throw new IllegalArgumentException("Illegal day " + dayNum);
        }
        if (isStartOf) {
            this.startDay = dayNum;
            this.startDayOfWeek = dow;
        } else {
            this.endDay = dayNum;
            this.endDayOfWeek = dow;
        }
        return mode;
    }

    // ---- the rules, in readable form     ---------------------------------------------------------

    public void setStartRule(int startMonth, int startDay, int startDayOfWeek, int startTime) {
        this.startMonth = startMonth;
        this.startDay = startDay;
        this.startDayOfWeek = startDayOfWeek;
        this.startTime = startTime;
        this.startTimeMode = WALL_TIME;
        this.decodeRules();
    }

    // An exact day of the month: with no day of the week.
    public void setStartRule(int startMonth, int startDay, int startTime) {
        this.setStartRule(startMonth, startDay, 0, startTime);
    }

    // The first `dayOfWeek` on or **after** the day (`after`), or the last on or **before**.
    public void setStartRule(int startMonth, int startDay, int startDayOfWeek, int startTime,
            boolean after) {
        if (after) {
            this.setStartRule(startMonth, startDay, -startDayOfWeek, startTime);
        } else {
            this.setStartRule(startMonth, -startDay, -startDayOfWeek, startTime);
        }
    }

    public void setEndRule(int endMonth, int endDay, int endDayOfWeek, int endTime) {
        this.endMonth = endMonth;
        this.endDay = endDay;
        this.endDayOfWeek = endDayOfWeek;
        this.endTime = endTime;
        this.endTimeMode = WALL_TIME;
        this.decodeRules();
    }

    public void setEndRule(int endMonth, int endDay, int endTime) {
        this.setEndRule(endMonth, endDay, 0, endTime);
    }

    public void setEndRule(int endMonth, int endDay, int endDayOfWeek, int endTime,
            boolean after) {
        if (after) {
            this.setEndRule(endMonth, endDay, -endDayOfWeek, endTime);
        } else {
            this.setEndRule(endMonth, -endDay, -endDayOfWeek, endTime);
        }
    }

    public void setStartYear(int year) {
        this.startYear = year;
    }

    // ---- offsets ---------------------------------------------------------------------------------

    public void setRawOffset(int offsetMillis) {
        this.rawOffset = offsetMillis;
    }

    public int getRawOffset() {
        return this.rawOffset;
    }

    // How far the clock goes forward during daylight saving. Zero if this zone does not use it.
    public int getDSTSavings() {
        if (this.useDaylight) {
            return this.dstSavings;
        }
        return 0;
    }

    public void setDSTSavings(int millisSavedDuringDST) {
        if (millisSavedDuringDST <= 0) {
            throw new IllegalArgumentException("Illegal daylight saving value: "
                    + millisSavedDuringDST);
        }
        this.dstSavings = millisSavedDuringDST;
    }

    public boolean useDaylightTime() {
        return this.useDaylight;
    }

    public boolean observesDaylightTime() {
        return this.useDaylight;
    }

    public boolean inDaylightTime(Date date) {
        return this.getOffset(date.getTime()) != this.rawOffset;
    }

    /**
     * The total offset (standard plus daylight) at the given instant.
     *
     * <p>The instant is moved to **local standard time** and the by-fields form is delegated to,
     * which is where the decision lives. That is the only delicate step: the other form's arguments
     * are in standard time, not wall time, and confusing them moves the answer by a whole hour.
     */
    public int getOffset(long date) {
        long local = date + this.rawOffset;
        long days = Math.floorDiv(local, (long) MS_PER_DAY);
        int inTheDay = (int) Math.floorMod(local, (long) MS_PER_DAY);
        long[] civil = GregorianCalendar.civilFromDays(days);
        int yearNum = (int) civil[0];
        int monthNum = (int) civil[1] - 1;
        int dayNum = (int) civil[2];
        int dow = dayOfWeekOf(days);
        int era = GregorianCalendar.AD;
        if (yearNum < 1) {
            era = GregorianCalendar.BC;
            yearNum = 1 - yearNum;
        }
        return this.getOffset(era, yearNum, monthNum, dayNum, dow, inTheDay);
    }

    /**
     * The total offset for a date given by fields.
     *
     * <p>`milliseconds` is the millisecond of the day in **local standard time**, which is what
     * TimeZone's contract fixes. The whole method leans on that: the two rules are normalised to
     * that same clock before comparing, and there the time's mode stops mattering.
     */
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        if (era != GregorianCalendar.AD && era != GregorianCalendar.BC) {
            throw new IllegalArgumentException("Illegal era " + era);
        }
        if (month < Calendar.JANUARY || month > Calendar.DECEMBER) {
            throw new IllegalArgumentException("Illegal month " + month);
        }
        if (dayOfWeek < Calendar.SUNDAY || dayOfWeek > Calendar.SATURDAY) {
            throw new IllegalArgumentException("Illegal day of week " + dayOfWeek);
        }
        if (milliseconds < 0 || milliseconds >= MS_PER_DAY) {
            throw new IllegalArgumentException("Illegal millis " + milliseconds);
        }
        if (!this.useDaylight || era != GregorianCalendar.AD || year < this.startYear) {
            return this.rawOffset;
        }

        // Everything is taken to "milliseconds since the epoch, in local standard time", which is a
        // total order and does not break when the time adjustment pushes the instant outside the
        // day.
        long asked = GregorianCalendar.daysFromCivil(year, month + 1, day)
                * (long) MS_PER_DAY + milliseconds;
        long start = this.ruleInstant(year, true);
        long end = this.ruleInstant(year, false);

        boolean inDaylight;
        if (start < end) {
            // Northern hemisphere: the summer falls **within** the year.
            inDaylight = asked >= start && asked < end;
        } else {
            // Southern hemisphere: the summer crosses the year end, so it is the complement.
            inDaylight = asked >= start || asked < end;
        }
        if (inDaylight) {
            return this.rawOffset + this.dstSavings;
        }
        return this.rawOffset;
    }

    // The instant of a transition in the given year, in local standard time.
    //
    // The adjustment is the only thing that tells the two rules apart: at the start the clock still
    // reads standard time, so the wall time already is the standard one; at the end the clock comes
    // in put forward, so what it is put forward by has to be subtracted.
    private long ruleInstant(int year, boolean isStartOf) {
        int monthNum = isStartOf ? this.startMonth : this.endMonth;
        int mode = isStartOf ? this.startMode : this.endMode;
        int dayNum = isStartOf ? this.startDay : this.endDay;
        int dow = isStartOf ? this.startDayOfWeek : this.endDayOfWeek;
        int hour = isStartOf ? this.startTime : this.endTime;
        int timeMode = isStartOf ? this.startTimeMode : this.endTimeMode;

        int adjustment = 0;
        if (timeMode == UTC_TIME) {
            adjustment = this.rawOffset;
        } else if (timeMode == WALL_TIME && !isStartOf) {
            adjustment = -this.dstSavings;
        }

        int dom = this.dayOfMonthOf(mode, year, monthNum, dayNum, dow);
        return GregorianCalendar.daysFromCivil(year, monthNum + 1, dom) * (long) MS_PER_DAY
                + hour + adjustment;
    }

    // It resolves the pattern to a concrete day of the month.
    private int dayOfMonthOf(int mode, int year, int month, int day, int dayOfWeek) {
        int length = monthLength(year, month);
        if (mode == DOM_MODE) {
            return day;
        }
        if (mode == DOW_IN_MONTH_MODE) {
            if (day > 0) {
                int first = dayOfWeekOf(GregorianCalendar.daysFromCivil(year, month + 1, 1));
                int salto = (dayOfWeek - first + 7) % 7;
                int d = 1 + salto + (day - 1) * 7;
                // If the month has not that many weeks, the last one holds -- it is what the JDK
                // does with a `5` in a month that has only four of that day.
                while (d > length) {
                    d = d - 7;
                }
                return d;
            }
            int last = dayOfWeekOf(GregorianCalendar.daysFromCivil(year, month + 1, length));
            int salto = (last - dayOfWeek + 7) % 7;
            int d = length - salto + (day + 1) * 7;
            while (d < 1) {
                d = d + 7;
            }
            return d;
        }
        if (mode == DOW_GE_DOM_MODE) {
            int dw = dayOfWeekOf(GregorianCalendar.daysFromCivil(year, month + 1, day));
            return day + (dayOfWeek - dw + 7) % 7;
        }
        // DOW_LE_DOM_MODE
        int dw = dayOfWeekOf(GregorianCalendar.daysFromCivil(year, month + 1, day));
        return day - (dw - dayOfWeek + 7) % 7;
    }

    // The day of the week (Calendar.SUNDAY..SATURDAY) of a day since the epoch.
    //
    // 1970-01-01 was a **Thursday**, and that is where the `+ 4` comes from: day 0 has to give
    // THURSDAY, which is 5.
    private static int dayOfWeekOf(long daysFromEpoch) {
        return (int) Math.floorMod(daysFromEpoch + 4, 7L) + 1;
    }

    private static int monthLength(int year, int month) {
        if (month == Calendar.FEBRUARY && isLeap(year)) {
            return 29;
        }
        return MONTH_LENGTHS[month];
    }

    private static boolean isLeap(int year) {
        return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
    }

    // ---- identity -------------------------------------------------------------------------------

    /**
     * Whether the two zones have the **same rules**, even if they are called differently.
     *
     * <p>It is different from `equals`, which also requires the same ID. The distinction matters: two
     * zones with different names and equal rules give the same time always, and whoever needs only
     * that -- converting instants -- can treat them as interchangeable.
     */
    public boolean hasSameRules(TimeZone other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SimpleTimeZone)) {
            return false;
        }
        SimpleTimeZone that = (SimpleTimeZone) other;
        if (this.rawOffset != that.rawOffset || this.useDaylight != that.useDaylight) {
            return false;
        }
        if (!this.useDaylight) {
            return true;
        }
        return this.dstSavings == that.dstSavings
                && this.startMode == that.startMode
                && this.startMonth == that.startMonth
                && this.startDay == that.startDay
                && this.startDayOfWeek == that.startDayOfWeek
                && this.startTime == that.startTime
                && this.startTimeMode == that.startTimeMode
                && this.endMode == that.endMode
                && this.endMonth == that.endMonth
                && this.endDay == that.endDay
                && this.endDayOfWeek == that.endDayOfWeek
                && this.endTime == that.endTime
                && this.endTimeMode == that.endTimeMode
                && this.startYear == that.startYear;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SimpleTimeZone)) {
            return false;
        }
        SimpleTimeZone that = (SimpleTimeZone) obj;
        return this.getID().equals(that.getID()) && this.hasSameRules(that);
    }

    public int hashCode() {
        return this.startMonth ^ this.startDay ^ this.startDayOfWeek ^ this.startTime
                ^ this.endMonth ^ this.endDay ^ this.endDayOfWeek ^ this.endTime ^ this.rawOffset;
    }

    public String toString() {
        return this.getClass().getName()
                + "[id=" + this.getID()
                + ",offset=" + this.rawOffset
                + ",dstSavings=" + this.dstSavings
                + ",useDaylight=" + this.useDaylight
                + ",startYear=" + this.startYear
                + ",startMode=" + this.startMode
                + ",startMonth=" + this.startMonth
                + ",startDay=" + this.startDay
                + ",startDayOfWeek=" + this.startDayOfWeek
                + ",startTime=" + this.startTime
                + ",startTimeMode=" + this.startTimeMode
                + ",endMode=" + this.endMode
                + ",endMonth=" + this.endMonth
                + ",endDay=" + this.endDay
                + ",endDayOfWeek=" + this.endDayOfWeek
                + ",endTime=" + this.endTime
                + ",endTimeMode=" + this.endTimeMode
                + "]";
    }
}
