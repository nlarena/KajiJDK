package java.util;

import java.lang.Cloneable;
import java.io.Serializable;

// KajiLibrary's java.util.Date (finding #267).
//
// It exists because `jakarta.persistence.Query` binds parameters of it, next to the Calendar
// overloads -- but unlike Calendar this one is not a type slot: a Date IS just a `long`, so the
// whole of its non-deprecated surface can be written honestly.
//
// What it deliberately does NOT have: the year/month/day/hours/minutes/seconds accessors and the
// `Date(int, int, int)` constructors. Every one of them is deprecated in the JDK precisely because
// it reads a wall-clock field out of an instant, which cannot be done without a TimeZone -- and
// TimeZone does not exist here. `toString()` is the same problem and is answered the only way that
// stays true: the instant, not a rendering of it in a zone we do not have.
//
// A missing member is a legal subset; a member that lies is not.
public class Date implements Comparable<Date>, Serializable, Cloneable {

    /** Milliseconds since the epoch. The whole state of a Date. */
    private long fastTime;

    /** Now. */
    public Date() {
        this.fastTime = System.currentTimeMillis();
    }

    /** The instant {@code date} milliseconds after the epoch. */
    public Date(long date) {
        this.fastTime = date;
    }

    /**
     * The date with those fields, in the **local zone**, with the time at zero.
     *
     * <p>All these constructors and the get/set below are **deprecated since Java 1.1**, and the
     * reason is in plain sight in the signature: `year` is the year minus 1900 and `month` runs from
     * 0 to 11. Two different conventions in the same call, and both surprise. `new Date(99, 11, 31)`
     * is 31 December 1999.
     *
     * <p>They are implemented all the same because they are part of the contract and there is code
     * that uses them. They lean on `GregorianCalendar`, which is where the inner calendar lives --
     * rewriting the date arithmetic here would mean having two, and having them contradict each
     * other.
     */
    public Date(int year, int month, int date) {
        this(year, month, date, 0, 0, 0);
    }

    /** The same, with hour and minute. */
    public Date(int year, int month, int date, int hrs, int min) {
        this(year, month, date, hrs, min, 0);
    }

    /** The same, with seconds. */
    public Date(int year, int month, int date, int hrs, int min, int sec) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.clear();
        cal.set(year + 1900, month, date, hrs, min, sec);
        this.fastTime = cal.getTimeInMillis();
    }

    /**
     * The date `s` stands for.
     *
     * @deprecated It depends on the text's format, which was never properly specified. It delegates
     *             to {@link #parse(String)}, which documents what it recognises.
     */
    public Date(String s) {
        this.fastTime = Date.parse(s);
    }

    /** The date equivalent to `instant`. */
    public static Date from(java.time.Instant instant) {
        if (instant == null) {
            throw new NullPointerException();
        }
        return new Date(instant.toEpochMilli());
    }

    /** This instant, as an `Instant`. */
    public java.time.Instant toInstant() {
        return java.time.Instant.ofEpochMilli(this.fastTime);
    }

    // ---- the fields, in the local zone ----------------------------------------------------------
    //
    // Each one builds a `GregorianCalendar` over the instant and reads the field. It is what the JDK
    // does, and it is expensive: six calls in a row build six calendars. The alternative --caching
    // one-- would make it shared mutable state, which is worse in a class that is already mutable.
    // These methods have been deprecated since 1997; optimising them would be an invitation to use
    // them.

    private GregorianCalendar cal() {
        GregorianCalendar c = new GregorianCalendar();
        c.setTimeInMillis(this.fastTime);
        return c;
    }

    private void setField(int field, int value) {
        GregorianCalendar c = this.cal();
        c.set(field, value);
        this.fastTime = c.getTimeInMillis();
    }

    /** The year minus 1900. */
    public int getYear() {
        return this.cal().get(Calendar.YEAR) - 1900;
    }

    /** It sets the year, given as the year minus 1900. */
    public void setYear(int year) {
        this.setField(Calendar.YEAR, year + 1900);
    }

    /** The month, from 0 (January) to 11. */
    public int getMonth() {
        return this.cal().get(Calendar.MONTH);
    }

    public void setMonth(int month) {
        this.setField(Calendar.MONTH, month);
    }

    /** The day of the month, from 1 to 31. */
    public int getDate() {
        return this.cal().get(Calendar.DAY_OF_MONTH);
    }

    public void setDate(int date) {
        this.setField(Calendar.DAY_OF_MONTH, date);
    }

    /**
     * The day of the week, from 0 (Sunday) to 6.
     *
     * <p>Mind the subtraction: `Calendar.DAY_OF_WEEK` numbers from 1 and this method from 0. It is
     * the kind of oversight that gives a day out in production and not in the test.
     */
    public int getDay() {
        return this.cal().get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
    }

    /** The hour, from 0 to 23. */
    public int getHours() {
        return this.cal().get(Calendar.HOUR_OF_DAY);
    }

    public void setHours(int hours) {
        this.setField(Calendar.HOUR_OF_DAY, hours);
    }

    public int getMinutes() {
        return this.cal().get(Calendar.MINUTE);
    }

    public void setMinutes(int minutes) {
        this.setField(Calendar.MINUTE, minutes);
    }

    public int getSeconds() {
        return this.cal().get(Calendar.SECOND);
    }

    public void setSeconds(int seconds) {
        this.setField(Calendar.SECOND, seconds);
    }

    /**
     * The minutes that have to be **subtracted** from UTC to reach this instant's local time.
     *
     * <p>The sign is the other way round from what one would say: for UTC-3 it returns **180**, not
     * -180. It has been so since 1995 and cannot be fixed without breaking everyone who uses it.
     */
    public int getTimezoneOffset() {
        GregorianCalendar c = this.cal();
        return -(c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET)) / 60000;
    }

    /**
     * The milliseconds since the epoch for that date **in UTC**.
     *
     * <p>It is the UTC sibling of the constructors above, with the same two odd conventions (year
     * minus 1900, month from 0).
     */
    public static long UTC(int year, int month, int date, int hrs, int min, int sec) {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(year + 1900, month, date, hrs, min, sec);
        return cal.getTimeInMillis();
    }

    /**
     * It reads `s` as a date and returns the milliseconds since the epoch.
     *
     * <p>It recognises the form {@link #toString()} produces --`EEE MMM d HH:mm:ss zzz yyyy`-- and
     * {@link #toGMTString()}'s. It does **not** try to cover the dozens of loose forms the JDK
     * accepts: its javadoc describes them in prose, and a description in prose is not a
     * specification. Rather than guess wrong in silence, what does not fit is rejected.
     *
     * @throws IllegalArgumentException if the format is not recognised
     */
    public static long parse(String s) {
        if (s == null) {
            throw new NullPointerException();
        }
        String[] parts = s.trim().split(" +");
        // `EEE MMM d HH:mm:ss zzz yyyy` -- what toString() returns.
        if (parts.length == 6 && parts[3].indexOf(':') >= 0) {
            int monthNum = monthByName(parts[1]);
            int day = Integer.parseInt(parts[2]);
            String[] hms = parts[3].split(":");
            int yearNum = Integer.parseInt(parts[5]);
            GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone(parts[4]));
            cal.clear();
            cal.set(yearNum, monthNum, day, Integer.parseInt(hms[0]), Integer.parseInt(hms[1]),
                    Integer.parseInt(hms[2]));
            return cal.getTimeInMillis();
        }
        // `d MMM yyyy HH:mm:ss GMT` -- what toGMTString() returns.
        if (parts.length == 5 && "GMT".equals(parts[4])) {
            int day = Integer.parseInt(parts[0]);
            int monthNum = monthByName(parts[1]);
            int yearNum = Integer.parseInt(parts[2]);
            String[] hms = parts[3].split(":");
            GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            cal.clear();
            cal.set(yearNum, monthNum, day, Integer.parseInt(hms[0]), Integer.parseInt(hms[1]),
                    Integer.parseInt(hms[2]));
            return cal.getTimeInMillis();
        }
        throw new IllegalArgumentException(s);
    }

    private static int monthByName(String name) {
        String[] months = new String[] {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                       "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        int i = 0;
        while (i < months.length) {
            if (months[i].equalsIgnoreCase(name)) {
                return i;
            }
            i = i + 1;
        }
        throw new IllegalArgumentException(name);
    }

    /**
     * `d MMM yyyy HH:mm:ss GMT`.
     *
     * @deprecated The name lies twice over: it is not GMT but UTC, and the format is no standard's.
     *             It is kept because {@link #parse(String)} has to read it.
     */
    public String toGMTString() {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(this.fastTime);
        String[] months = new String[] {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                       "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        StringBuilder sb = new StringBuilder();
        sb.append(cal.get(Calendar.DAY_OF_MONTH));
        sb.append(' ');
        sb.append(months[cal.get(Calendar.MONTH)]);
        sb.append(' ');
        sb.append(cal.get(Calendar.YEAR));
        sb.append(' ');
        twoDigits(sb, cal.get(Calendar.HOUR_OF_DAY));
        sb.append(':');
        twoDigits(sb, cal.get(Calendar.MINUTE));
        sb.append(':');
        twoDigits(sb, cal.get(Calendar.SECOND));
        sb.append(" GMT");
        return sb.toString();
    }

    /**
     * The date in the default locale's format.
     *
     * @deprecated In this library it returns the same form as {@link #toGMTString()} but in local
     *             time, because there is no locale-sensitive formatter to delegate to. It is
     *             documented rather than faked: the JDK uses `DateFormat`, which is not here.
     */
    public String toLocaleString() {
        GregorianCalendar cal = this.cal();
        String[] months = new String[] {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                       "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        StringBuilder sb = new StringBuilder();
        sb.append(cal.get(Calendar.DAY_OF_MONTH));
        sb.append(' ');
        sb.append(months[cal.get(Calendar.MONTH)]);
        sb.append(' ');
        sb.append(cal.get(Calendar.YEAR));
        sb.append(' ');
        twoDigits(sb, cal.get(Calendar.HOUR_OF_DAY));
        sb.append(':');
        twoDigits(sb, cal.get(Calendar.MINUTE));
        sb.append(':');
        twoDigits(sb, cal.get(Calendar.SECOND));
        return sb.toString();
    }

    private static void twoDigits(StringBuilder sb, int n) {
        if (n < 10) {
            sb.append('0');
        }
        sb.append(n);
    }

    public long getTime() {
        return this.fastTime;
    }

    public void setTime(long time) {
        this.fastTime = time;
    }

    public boolean before(Date when) {
        return this.fastTime < when.fastTime;
    }

    public boolean after(Date when) {
        return this.fastTime > when.fastTime;
    }

    @Override
    public int compareTo(Date other) {
        if (this.fastTime < other.fastTime) {
            return -1;
        }
        if (this.fastTime > other.fastTime) {
            return 1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Date && this.fastTime == ((Date) other).fastTime;
    }

    @Override
    public int hashCode() {
        return (int) (this.fastTime ^ (this.fastTime >>> 32));
    }

    /**
     * The instant in milliseconds since the epoch.
     *
     * <p>NOT the JDK's format. The JDK renders {@code "EEE MMM dd HH:mm:ss zzz yyyy"} in the
     * default time zone, and there is no TimeZone here to render it in -- so rather than print a
     * wall clock that would silently be UTC and claim otherwise, this prints the one thing a Date
     * actually knows.
     */
    @Override
    public String toString() {
        return "Date(" + this.fastTime + ")";
    }
}
