package java.sql;

/**
 * KajiLibrary's java.sql.Date -- a date **with no time**, for a `DATE` column.
 *
 * <p>It inherits from `java.util.Date`, which does have a time, and that is why the contract asks
 * that the time's milliseconds be **zero** in the default time zone. A `java.sql.Date` with a time
 * inside is not an error that jumps out: it is a value that compares wrong against the same day
 * stored properly.
 *
 * <p>Hence {@link #getHours} and company **fail** rather than return zero. Zero would be an answer,
 * and the correct answer is that the question does not apply -- an SQL date has no time, and saying
 * "midnight" would invite doing arithmetic with it.
 *
 * <p>The same for {@link #toInstant}: an instant is a point on the time line and a date is not one;
 * converting needs a time zone, which this class does not have. The JDK fails here too, for the
 * same reason.
 */
public class Date extends java.util.Date {

    /**
     * The date of that year (from 1900), month (from zero) and day.
     *
     * @deprecated use {@link #Date(long)} or {@link #valueOf(java.time.LocalDate)}
     */
    @Deprecated
    public Date(int year, int month, int day) {
        super(year, month, day);
    }

    /** The date of that instant in milliseconds. */
    public Date(long date) {
        super(date);
    }

    public void setTime(long date) {
        super.setTime(date);
    }

    /**
     * The date written `yyyy-[m]m-[d]d`.
     *
     * @throws IllegalArgumentException if it is not in that shape
     */
    public static Date valueOf(String s) {
        if (s == null) {
            throw new IllegalArgumentException("null");
        }
        int firstDash = s.indexOf('-');
        int secondDash = firstDash < 0 ? -1 : s.indexOf('-', firstDash + 1);
        if (firstDash <= 0 || secondDash <= firstDash + 1 || secondDash == s.length() - 1) {
            throw new IllegalArgumentException(s);
        }
        int year;
        int month;
        int day;
        try {
            year = Integer.parseInt(s.substring(0, firstDash));
            month = Integer.parseInt(s.substring(firstDash + 1, secondDash));
            day = Integer.parseInt(s.substring(secondDash + 1, s.length()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(s);
        }
        return new Date(year - 1900, month - 1, day);
    }

    /** That {@link java.time.LocalDate}'s date. */
    public static Date valueOf(java.time.LocalDate date) {
        return new Date(date.getYear() - 1900, date.getMonthValue() - 1, date.getDayOfMonth());
    }

    /** This date as a {@link java.time.LocalDate}. */
    public java.time.LocalDate toLocalDate() {
        return java.time.LocalDate.of(this.getYear() + 1900, this.getMonth() + 1, this.getDate());
    }

    /** `yyyy-mm-dd`, with leading zeros. */
    public String toString() {
        int year = this.getYear() + 1900;
        int month = this.getMonth() + 1;
        int day = this.getDate();
        StringBuilder sb = new StringBuilder();
        sb.append(year);
        sb.append('-');
        if (month < 10) {
            sb.append('0');
        }
        sb.append(month);
        sb.append('-');
        if (day < 10) {
            sb.append('0');
        }
        sb.append(day);
        return sb.toString();
    }

    // ---- what does not apply --------------------------------------------------------------------

    /** @throws IllegalArgumentException always: an SQL date has no time */
    public int getHours() {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException always */
    public int getMinutes() {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException always */
    public int getSeconds() {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException always */
    public void setHours(int i) {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException always */
    public void setMinutes(int i) {
        throw new IllegalArgumentException();
    }

    /** @throws IllegalArgumentException always */
    public void setSeconds(int i) {
        throw new IllegalArgumentException();
    }

    /** @throws UnsupportedOperationException always: the time zone is missing */
    public java.time.Instant toInstant() {
        throw new UnsupportedOperationException();
    }
}
