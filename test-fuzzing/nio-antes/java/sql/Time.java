package java.sql;

/**
 * KajiLibrary's java.sql.Time -- a time **with no date**, for a `TIME` column.
 *
 * <p>The exact reverse of {@link Date}: it inherits from `java.util.Date`, and the contract asks
 * that the date part be 1 January 1970. And for the same reason {@link #getYear} and company fail
 * -- "1970" would be an invented answer to a question that does not apply.
 */
public class Time extends java.util.Date {

    /**
     * The time of that hour, minute and second.
     *
     * @deprecated use {@link #Time(long)} or {@link #valueOf(java.time.LocalTime)}
     */
    @Deprecated
    public Time(int hour, int minute, int second) {
        super(70, 0, 1, hour, minute, second);
    }

    /** The time of that instant in milliseconds. */
    public Time(long time) {
        super(time);
    }

    public void setTime(long time) {
        super.setTime(time);
    }

    /**
     * The time written `hh:mm:ss`.
     *
     * @throws IllegalArgumentException if it does not have that shape
     */
    public static Time valueOf(String s) {
        if (s == null) {
            throw new IllegalArgumentException("null");
        }
        int firstColon = s.indexOf(':');
        int secondColon = firstColon < 0 ? -1 : s.indexOf(':', firstColon + 1);
        if (firstColon <= 0 || secondColon <= firstColon + 1 || secondColon == s.length() - 1) {
            throw new IllegalArgumentException(s);
        }
        int hour;
        int minute;
        int second;
        try {
            hour = Integer.parseInt(s.substring(0, firstColon));
            minute = Integer.parseInt(s.substring(firstColon + 1, secondColon));
            second = Integer.parseInt(s.substring(secondColon + 1, s.length()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(s);
        }
        return new Time(hour, minute, second);
    }

    /** The time of that {@link java.time.LocalTime}. */
    public static Time valueOf(java.time.LocalTime time) {
        return new Time(time.getHour(), time.getMinute(), time.getSecond());
    }

    /** This time as a {@link java.time.LocalTime}. */
    public java.time.LocalTime toLocalTime() {
        return java.time.LocalTime.of(this.getHours(), this.getMinutes(), this.getSeconds());
    }

    /** `hh:mm:ss`, zero-padded. */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        twoDigits(sb, this.getHours());
        sb.append(':');
        twoDigits(sb, this.getMinutes());
        sb.append(':');
        twoDigits(sb, this.getSeconds());
        return sb.toString();
    }

    private static void twoDigits(StringBuilder sb, int v) {
        if (v < 10) {
            sb.append('0');
        }
        sb.append(v);
    }

    // ---- what does not apply --------------------------------------------------------------------

    /** @throws IllegalArgumentException always: an SQL time has no date */
    public int getYear() {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException always */
    public int getMonth() {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException always */
    public int getDay() {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException always */
    public int getDate() {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException always */
    public void setYear(int i) {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException always */
    public void setMonth(int i) {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException always */
    public void setDate(int i) {
        throw new IllegalArgumentException();
    }

    /** @throws UnsupportedOperationException always: the date is missing */
    public java.time.Instant toInstant() {
        throw new UnsupportedOperationException();
    }
}
