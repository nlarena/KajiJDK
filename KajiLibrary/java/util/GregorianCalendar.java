package java.util;

import java.time.ZonedDateTime;

// The Gregorian calendar: `Calendar`'s concrete implementation.
//
// It translates in both directions between an instant —milliseconds since 1970-01-01T00:00:00Z—
// and the civil fields (year, month, day, hour...) in a time zone. That translation is all it does,
// and it is less obvious than it looks: months have different lengths, leap years follow three
// chained rules, and the zone shifts the instant before it is split.
//
// **A KajiLibrary subset, and this has to be known before using it with old dates:** the calendar is
// **proleptic**, that is, it applies the Gregorian rules backwards without end. The JDK switches to
// Julian before 15 October 1582 —the ten days pope Gregory deleted— and exposes that cutover with
// `setGregorianChange`. Here that cutover does not exist: `getGregorianChange()` returns the oldest
// representable instant and `setGregorianChange` rejects it rather than pretend. For any date after
// 1582 there is no difference; for an earlier one, this class gives the proleptic date and the JDK
// the Julian one.
//
// The day arithmetic is Howard Hinnant's: exact, with no tables and no loops, over a calendar that
// starts the year in March so the leap day falls at the end.
public class GregorianCalendar extends Calendar {

    // The era before year 1.
    public static final int BC = 0;

    // The era from year 1 onwards.
    public static final int AD = 1;

    private static final long MS_PER_DAY = 86400000L;
    private static final long MS_PER_HOUR = 3600000L;
    private static final long MS_PER_MINUTE = 60000L;

    // The length of each month, and of February in a leap year.
    private static final int[] MONTH_LENGTHS = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    // A calendar with the current date and time, in the default zone and locale.
    public GregorianCalendar() {
        this(TimeZone.getDefault(), Locale.getDefault());
    }

    public GregorianCalendar(TimeZone zone) {
        this(zone, Locale.getDefault());
    }

    public GregorianCalendar(Locale aLocale) {
        this(TimeZone.getDefault(), aLocale);
    }

    public GregorianCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
        this.setTimeInMillis(System.currentTimeMillis());
    }

    // A calendar on the given date, at midnight. `month` is 0-based, as everywhere in Calendar.
    public GregorianCalendar(int year, int month, int dayOfMonth) {
        this(year, month, dayOfMonth, 0, 0, 0);
    }

    public GregorianCalendar(int year, int month, int dayOfMonth, int hourOfDay, int minute) {
        this(year, month, dayOfMonth, hourOfDay, minute, 0);
    }

    public GregorianCalendar(int year, int month, int dayOfMonth, int hourOfDay, int minute,
                             int second) {
        super(TimeZone.getDefault(), Locale.getDefault());
        this.set(YEAR, year);
        this.set(MONTH, month);
        this.set(DAY_OF_MONTH, dayOfMonth);
        this.set(HOUR_OF_DAY, hourOfDay);
        this.set(MINUTE, minute);
        this.set(SECOND, second);
        this.set(MILLISECOND, 0);
    }

    // ---- the day arithmetic    ---------------------------------------------------------------

    // Days since 1970-01-01 for a civil date. `m` is 1..12.
    //
    // The trick is shifting the year so it starts in March: that way 29 February falls at the END of
    // the year and the months' lengths turn into a regular progression, which is what allows the day
    // of the year to be computed with a single formula instead of a table.
    static long daysFromCivil(long y, int m, int d) {
        long yy = y;
        if (m <= 2) {
            yy = yy - 1;
        }
        long era = (yy >= 0 ? yy : yy - 399) / 400;
        long yoe = yy - era * 400;
        int shifted = m + (m > 2 ? -3 : 9);
        long doy = (153L * shifted + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    // The inverse: the civil date of a day since 1970-01-01. It returns { year, month 1..12, day }.
    static long[] civilFromDays(long z) {
        long zz = z + 719468;
        long era = (zz >= 0 ? zz : zz - 146096) / 146097;
        long doe = zz - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp + (mp < 10 ? 3 : -9);
        if (m <= 2) {
            y = y + 1;
        }
        long[] out = new long[3];
        out[0] = y;
        out[1] = m;
        out[2] = d;
        return out;
    }

    // Whether `year` is a leap year: divisible by 4, except the century ones which are not unless
    // by 400.
    public boolean isLeapYear(int year) {
        if (year % 4 != 0) {
            return false;
        }
        if (year % 100 != 0) {
            return true;
        }
        return year % 400 == 0;
    }

    // The days of month `month` (0-based) of the given year.
    private int daysInMonth(int year, int month) {
        if (month == 1 && this.isLeapYear(year)) {
            return 29;
        }
        return MONTH_LENGTHS[month];
    }

    // ---- the two translations ----------------------------------------------------------------

    // Instant -> fields.
    protected void computeFields() {
        int offset = this.getTimeZone().getOffset(this.time);
        long local = this.time + offset;
        long days = Math.floorDiv(local, MS_PER_DAY);
        int msOfDay = (int) Math.floorMod(local, MS_PER_DAY);

        long[] ymd = civilFromDays(days);
        int yearNum = (int) ymd[0];
        int monthNum = (int) ymd[1] - 1;
        int day = (int) ymd[2];

        if (yearNum > 0) {
            this.fields[ERA] = AD;
            this.fields[YEAR] = yearNum;
        } else {
            this.fields[ERA] = BC;
            this.fields[YEAR] = 1 - yearNum;
        }
        this.fields[MONTH] = monthNum;
        this.fields[DAY_OF_MONTH] = day;

        // 1970-01-01 was a Thursday, and THURSDAY is 5 with SUNDAY = 1.
        this.fields[DAY_OF_WEEK] = (int) Math.floorMod(days + 4, 7L) + 1;

        long firstOfYear = daysFromCivil(ymd[0], 1, 1);
        int dayOfYear = (int) (days - firstOfYear) + 1;
        this.fields[DAY_OF_YEAR] = dayOfYear;
        this.fields[DAY_OF_WEEK_IN_MONTH] = (day - 1) / 7 + 1;

        int dowOfFirstOfYear = (int) Math.floorMod(firstOfYear + 4, 7L) + 1;
        this.fields[WEEK_OF_YEAR] = weekNumber(dayOfYear, dowOfFirstOfYear);

        long firstOfMonth = daysFromCivil(ymd[0], (int) ymd[1], 1);
        int dowOfFirstOfMonth = (int) Math.floorMod(firstOfMonth + 4, 7L) + 1;
        this.fields[WEEK_OF_MONTH] = weekNumber(day, dowOfFirstOfMonth);

        int hour = msOfDay / (int) MS_PER_HOUR;
        this.fields[HOUR_OF_DAY] = hour;
        this.fields[AM_PM] = hour < 12 ? 0 : 1;
        this.fields[HOUR] = hour % 12;
        this.fields[MINUTE] = (msOfDay / (int) MS_PER_MINUTE) % 60;
        this.fields[SECOND] = (msOfDay / 1000) % 60;
        this.fields[MILLISECOND] = msOfDay % 1000;
        this.fields[ZONE_OFFSET] = this.getTimeZone().getRawOffset();
        this.fields[DST_OFFSET] = offset - this.getTimeZone().getRawOffset();

        int i = 0;
        while (i < this.fields.length) {
            this.isSet[i] = true;
            i = i + 1;
        }
    }

    // The week number of `dayOfPeriod`, knowing which day of the week the first fell on.
    //
    // The two configurable conventions come in here: `firstDayOfWeek` decides where the week is cut,
    // and `minimalDaysInFirstWeek` decides whether the first loose days count as week 1 or as the
    // last of the previous period (and then this returns 0).
    private int weekNumber(int dayOfPeriod, int dowOfFirst) {
        int displacement = Math.floorMod(dowOfFirst - this.getFirstDayOfWeek(), 7);
        int week = (dayOfPeriod + displacement - 1) / 7 + 1;
        if (7 - displacement < this.getMinimalDaysInFirstWeek()) {
            week = week - 1;
        }
        return week;
    }

    // Fields -> instant.
    protected void computeTime() {
        int yearNum = this.fields[YEAR];
        if (this.isSet[ERA] && this.fields[ERA] == BC) {
            yearNum = 1 - yearNum;
        }
        int monthNum = this.fields[MONTH];
        // A month outside 0..11 overflows into the year: `set(MONTH, 12)` is January of the next
        // one. It is `lenient` mode, which is the default.
        yearNum = yearNum + Math.floorDiv(monthNum, 12);
        monthNum = Math.floorMod(monthNum, 12);

        int day = this.isSet[DAY_OF_MONTH] ? this.fields[DAY_OF_MONTH] : 1;

        int hour;
        if (this.isSet[HOUR_OF_DAY]) {
            hour = this.fields[HOUR_OF_DAY];
        } else if (this.isSet[HOUR]) {
            hour = this.fields[HOUR] + (this.isSet[AM_PM] && this.fields[AM_PM] == 1 ? 12 : 0);
        } else {
            hour = 0;
        }

        long days = daysFromCivil(yearNum, monthNum + 1, day);
        long local = days * MS_PER_DAY
            + hour * MS_PER_HOUR
            + this.fields[MINUTE] * MS_PER_MINUTE
            + this.fields[SECOND] * 1000L
            + this.fields[MILLISECOND];
        this.time = local - this.getTimeZone().getRawOffset();
    }

    // ---- arithmetic over fields  --------------------------------------------------------------

    // It adds `amount` to the field, carrying into the larger ones.
    public void add(int field, int amount) {
        if (amount == 0) {
            return;
        }
        this.complete();
        if (field == YEAR || field == MONTH) {
            int yearNum = this.get(YEAR);
            int monthNum = this.get(MONTH);
            int day = this.get(DAY_OF_MONTH);
            if (field == YEAR) {
                yearNum = yearNum + amount;
            } else {
                int total = yearNum * 12 + monthNum + amount;
                yearNum = Math.floorDiv(total, 12);
                monthNum = Math.floorMod(total, 12);
            }
            // The clamping goes BEFORE writing the fields, not after.
            //
            // If "31 February" is written and only looked at afterwards, `computeTime` has already
            // turned it into 2 March and no trace is left that there was an overflow: day 2 is
            // perfectly valid in March. The JDK gives 29 February, and that is the semantics that
            // matters — adding one month should not jump two.
            int max = this.daysInMonth(yearNum, monthNum);
            if (day > max) {
                day = max;
            }
            this.set(YEAR, yearNum);
            this.set(MONTH, monthNum);
            this.set(DAY_OF_MONTH, day);
            return;
        }
        long delta;
        if (field == DAY_OF_MONTH || field == DAY_OF_YEAR || field == DAY_OF_WEEK
                || field == DAY_OF_WEEK_IN_MONTH) {
            delta = (long) amount * MS_PER_DAY;
        } else if (field == WEEK_OF_YEAR || field == WEEK_OF_MONTH) {
            delta = (long) amount * 7 * MS_PER_DAY;
        } else if (field == HOUR || field == HOUR_OF_DAY) {
            delta = (long) amount * MS_PER_HOUR;
        } else if (field == MINUTE) {
            delta = (long) amount * MS_PER_MINUTE;
        } else if (field == SECOND) {
            delta = (long) amount * 1000L;
        } else if (field == MILLISECOND) {
            delta = amount;
        } else {
            throw new IllegalArgumentException("" + field);
        }
        this.setTimeInMillis(this.getTimeInMillis() + delta);
    }

    // It adds 1 (or subtracts 1) to the field WITHOUT touching the larger ones.
    public void roll(int field, boolean up) {
        this.roll(field, up ? 1 : -1);
    }

    // It adds `amount` to the field without touching the larger ones, wrapping within its range.
    public void roll(int field, int amount) {
        if (amount == 0) {
            return;
        }
        this.complete();
        int min = this.getActualMinimum(field);
        int max = this.getActualMaximum(field);
        int span = max - min + 1;
        int value = this.get(field);
        int updated = Math.floorMod(value - min + amount, span) + min;
        if (field == YEAR || field == MONTH) {
            // The same care as in `add`: clamp the day before writing, not after.
            int yearNum = field == YEAR ? updated : this.get(YEAR);
            int monthNum = field == MONTH ? updated : this.get(MONTH);
            int day = this.get(DAY_OF_MONTH);
            // `maxDay` and not `max`: the `max` above --the maximum of the field being rolled--
            // is still in scope, and §6.4 does not allow redeclaring it. They are two different
            // things besides: one is the field's ceiling and the other the days of the month.
            int maxDay = this.daysInMonth(yearNum, monthNum);
            if (day > maxDay) {
                day = maxDay;
            }
            this.set(YEAR, yearNum);
            this.set(MONTH, monthNum);
            this.set(DAY_OF_MONTH, day);
            return;
        }
        this.set(field, updated);
    }

    // ---- the fields' ranges   ------------------------------------------------------------------

    public int getMinimum(int field) {
        if (field == ERA) {
            return BC;
        }
        if (field == YEAR) {
            return 1;
        }
        if (field == MONTH || field == HOUR || field == HOUR_OF_DAY || field == MINUTE
                || field == SECOND || field == MILLISECOND || field == AM_PM) {
            return 0;
        }
        if (field == ZONE_OFFSET) {
            return -50400000;
        }
        if (field == DST_OFFSET) {
            return 0;
        }
        return 1;
    }

    public int getMaximum(int field) {
        if (field == ERA) {
            return AD;
        }
        if (field == YEAR) {
            return 292278994;
        }
        if (field == MONTH) {
            return 11;
        }
        if (field == WEEK_OF_YEAR) {
            return 53;
        }
        if (field == WEEK_OF_MONTH) {
            return 6;
        }
        if (field == DAY_OF_MONTH) {
            return 31;
        }
        if (field == DAY_OF_YEAR) {
            return 366;
        }
        if (field == DAY_OF_WEEK) {
            return 7;
        }
        if (field == DAY_OF_WEEK_IN_MONTH) {
            return 6;
        }
        if (field == AM_PM) {
            return 1;
        }
        if (field == HOUR) {
            return 11;
        }
        if (field == HOUR_OF_DAY) {
            return 23;
        }
        if (field == MINUTE || field == SECOND) {
            return 59;
        }
        if (field == MILLISECOND) {
            return 999;
        }
        if (field == ZONE_OFFSET) {
            return 50400000;
        }
        return 7200000;
    }

    // The largest value the field reaches in ALL cases.
    //
    // Different from `getMaximum`: DAY_OF_MONTH reaches 31 in some month, but 28 is the only one
    // guaranteed in every one. Code that wants a day valid for any month has to use this.
    public int getLeastMaximum(int field) {
        if (field == DAY_OF_MONTH) {
            return 28;
        }
        if (field == DAY_OF_YEAR) {
            return 365;
        }
        if (field == WEEK_OF_YEAR) {
            return 52;
        }
        if (field == WEEK_OF_MONTH) {
            return 4;
        }
        if (field == DAY_OF_WEEK_IN_MONTH) {
            return 4;
        }
        return this.getMaximum(field);
    }

    public int getGreatestMinimum(int field) {
        return this.getMinimum(field);
    }

    public int getActualMinimum(int field) {
        return this.getMinimum(field);
    }

    // The largest value of the field ON THIS date: this is where DAY_OF_MONTH returns 28, 29, 30
    // or 31.
    public int getActualMaximum(int field) {
        this.complete();
        if (field == DAY_OF_MONTH) {
            return this.daysInMonth(this.get(YEAR), this.get(MONTH));
        }
        if (field == DAY_OF_YEAR) {
            return this.isLeapYear(this.get(YEAR)) ? 366 : 365;
        }
        if (field == DAY_OF_WEEK_IN_MONTH) {
            return (this.daysInMonth(this.get(YEAR), this.get(MONTH)) - 1) / 7 + 1;
        }
        return this.getMaximum(field);
    }

    // ---- the Julian/Gregorian cutover, which here does not exist ----------------------------------------

    // It refuses to change the cutover.
    //
    // A KajiLibrary subset: the calendar is proleptic, with no cutover. Throwing is preferable to
    // accepting the call and going on giving proleptic dates, which is what a no-op would do: the
    // caller would believe they had Julian dates and would not.
    public void setGregorianChange(Date date) {
        throw new UnsupportedOperationException(
            "KajiLibrary uses a proleptic Gregorian calendar, with no Julian cutover");
    }

    // The instant of the cutover. Being proleptic, the oldest representable one.
    public final Date getGregorianChange() {
        return new Date(-9223372036854775808L);
    }

    public String getCalendarType() {
        return "gregory";
    }

    // ---- date by ISO week      -----------------------------------------------------------------

    public final boolean isWeekDateSupported() {
        return true;
    }

    // The year this date's week belongs to, which is not always the calendar year: 1 January can
    // fall in the last week of the previous year.
    public int getWeekYear() {
        this.complete();
        int week = this.get(WEEK_OF_YEAR);
        int monthNum = this.get(MONTH);
        if (week >= 52 && monthNum == 0) {
            return this.get(YEAR) - 1;
        }
        if (week == 1 && monthNum == 11) {
            return this.get(YEAR) + 1;
        }
        return this.get(YEAR);
    }

    public void setWeekDate(int weekYear, int weekOfYear, int dayOfWeek) {
        if (dayOfWeek < SUNDAY || dayOfWeek > SATURDAY) {
            throw new IllegalArgumentException("invalid dayOfWeek: " + dayOfWeek);
        }
        this.set(YEAR, weekYear);
        this.set(MONTH, 0);
        this.set(DAY_OF_MONTH, 1);
        this.complete();
        int dowOfFirst = this.get(DAY_OF_WEEK);
        int displacement = Math.floorMod(dowOfFirst - this.getFirstDayOfWeek(), 7);
        int dayOfYear = (weekOfYear - 1) * 7 + Math.floorMod(dayOfWeek - this.getFirstDayOfWeek(), 7)
            - displacement + 1;
        if (7 - displacement < this.getMinimalDaysInFirstWeek()) {
            dayOfYear = dayOfYear + 7;
        }
        this.set(DAY_OF_MONTH, 1);
        this.setTimeInMillis(this.getTimeInMillis() + (long) (dayOfYear - 1) * MS_PER_DAY);
    }

    public int getWeeksInWeekYear() {
        this.complete();
        int yearNum = this.getWeekYear();
        GregorianCalendar end = new GregorianCalendar(yearNum, 11, 31);
        end.setFirstDayOfWeek(this.getFirstDayOfWeek());
        end.setMinimalDaysInFirstWeek(this.getMinimalDaysInFirstWeek());
        int week = end.get(WEEK_OF_YEAR);
        if (week == 1) {
            return 52;
        }
        return week;
    }

    // ---- equality, copying and bridges to java.time ----------------------------------------------

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GregorianCalendar)) {
            return false;
        }
        GregorianCalendar that = (GregorianCalendar) obj;
        return this.getTimeInMillis() == that.getTimeInMillis()
            && this.getTimeZone().equals(that.getTimeZone());
    }

    public int hashCode() {
        long t = this.getTimeInMillis();
        return (int) (t ^ (t >>> 32));
    }

    public Object clone() {
        GregorianCalendar copied = new GregorianCalendar(this.getTimeZone(), Locale.getDefault());
        copied.setTimeInMillis(this.getTimeInMillis());
        copied.setFirstDayOfWeek(this.getFirstDayOfWeek());
        copied.setMinimalDaysInFirstWeek(this.getMinimalDaysInFirstWeek());
        return copied;
    }

    // This date as a ZonedDateTime.
    public ZonedDateTime toZonedDateTime() {
        return ZonedDateTime.ofInstant(this.toInstant(), this.getTimeZone().toZoneId());
    }

    // A calendar at the given ZonedDateTime's instant and zone.
    public static GregorianCalendar from(ZonedDateTime zdt) {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone(zdt.getZone()),
            Locale.getDefault());
        cal.setTimeInMillis(zdt.toInstant().toEpochMilli());
        return cal;
    }
}
