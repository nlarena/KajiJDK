package java.sql;

/**
 * KajiLibrary's java.sql.Timestamp -- date, time and **nanoseconds**.
 *
 * <p>The nanoseconds are the class's reason to exist, and also the cause of all its problems. They
 * are kept in a separate field because `java.util.Date` only reaches the millisecond; the inherited
 * part keeps the whole seconds and the new field the fraction. That is why {@link #getTime} has to
 * rebuild the milliseconds by adding the fraction back in.
 *
 * <p>Out of that inheritance comes the famous asymmetry: `equals` with a `java.util.Date` standing
 * for the same instant gives `false` --the other cannot have nanos-- but `compareTo` with the same
 * object gives zero. It is a violation of `Comparable`'s contract, it has been documented in the
 * JDK forever, and it is reproduced here because changing it would break the code that knows it.
 */
public class Timestamp extends java.util.Date {

    // The fraction of a second, from 0 to 999999999. The inherited part keeps only whole seconds.
    private int nanos;

    /**
     * @deprecated use {@link #Timestamp(long)} or {@link #valueOf(java.time.LocalDateTime)}
     */
    @Deprecated
    public Timestamp(int year, int month, int date, int hour, int minute, int second, int nano) {
        super(year, month, date, hour, minute, second);
        if (nano > 999999999 || nano < 0) {
            throw new IllegalArgumentException("nanos out of range");
        }
        this.nanos = nano;
    }

    /** The instant of those milliseconds; the fraction is shared between the two fields. */
    public Timestamp(long time) {
        super(time);
        this.splitMillis(time);
    }

    public void setTime(long time) {
        super.setTime(time);
        this.splitMillis(time);
    }

    // The inherited part keeps the whole seconds and the fraction goes here. The `+ 1000` is for
    // the instants before 1970: a negative's remainder is negative, and nanos cannot be.
    private void splitMillis(long time) {
        int millis = (int) (time % 1000);
        if (millis < 0) {
            millis = millis + 1000;
        }
        this.nanos = millis * 1000000;
        super.setTime(time - millis);
    }

    /** The instant's milliseconds, adding the fraction in. */
    public long getTime() {
        return super.getTime() + (this.nanos / 1000000);
    }

    /** The fraction of a second, in nanoseconds. */
    public int getNanos() {
        return this.nanos;
    }

    /** It sets the fraction of a second. */
    public void setNanos(int n) {
        if (n > 999999999 || n < 0) {
            throw new IllegalArgumentException("nanos out of range");
        }
        this.nanos = n;
    }

    /**
     * The instant written `yyyy-mm-dd hh:mm:ss[.f...]`.
     *
     * @throws IllegalArgumentException if it is not in that shape
     */
    public static Timestamp valueOf(String s) {
        if (s == null) {
            throw new IllegalArgumentException("null");
        }
        int space = s.indexOf(' ');
        if (space < 0) {
            throw new IllegalArgumentException(s);
        }
        Date date = Date.valueOf(s.substring(0, space));
        String rest = s.substring(space + 1, s.length());
        int dot = rest.indexOf('.');
        String timeOnly = dot < 0 ? rest : rest.substring(0, dot);
        Time hour = Time.valueOf(timeOnly);
        int nanos = 0;
        if (dot >= 0) {
            String fraction = rest.substring(dot + 1, rest.length());
            if (fraction.length() == 0 || fraction.length() > 9) {
                throw new IllegalArgumentException(s);
            }
            // It is padded to nine digits: `.5` is half a second, not five nanoseconds.
            StringBuilder sb = new StringBuilder(fraction);
            while (sb.length() < 9) {
                sb.append('0');
            }
            try {
                nanos = Integer.parseInt(sb.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(s);
            }
        }
        return new Timestamp(date.getYear(), date.getMonth(), date.getDate(), hour.getHours(),
                hour.getMinutes(), hour.getSeconds(), nanos);
    }

    /** That {@link java.time.LocalDateTime}'s instant. */
    public static Timestamp valueOf(java.time.LocalDateTime dateTime) {
        return new Timestamp(dateTime.getYear() - 1900, dateTime.getMonthValue() - 1,
                dateTime.getDayOfMonth(), dateTime.getHour(), dateTime.getMinute(),
                dateTime.getSecond(), dateTime.getNano());
    }

    /** This instant as a {@link java.time.LocalDateTime}. */
    public java.time.LocalDateTime toLocalDateTime() {
        return java.time.LocalDateTime.of(this.getYear() + 1900, this.getMonth() + 1,
                this.getDate(), this.getHours(), this.getMinutes(), this.getSeconds(), this.nanos);
    }

    /** That {@link java.time.Instant}'s instant, without losing the nanoseconds. */
    public static Timestamp from(java.time.Instant instant) {
        Timestamp t = new Timestamp(instant.getEpochSecond() * 1000);
        t.setNanos(instant.getNano());
        return t;
    }

    /** This instant as a {@link java.time.Instant}, with the nanoseconds. */
    public java.time.Instant toInstant() {
        return java.time.Instant.ofEpochSecond(super.getTime() / 1000, this.nanos);
    }

    /** `yyyy-mm-dd hh:mm:ss.fffffffff`, without the fraction's trailing zeros. */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(new Date(super.getTime()).toString());
        sb.append(' ');
        int hour = this.getHours();
        int minute = this.getMinutes();
        int second = this.getSeconds();
        if (hour < 10) {
            sb.append('0');
        }
        sb.append(hour);
        sb.append(':');
        if (minute < 10) {
            sb.append('0');
        }
        sb.append(minute);
        sb.append(':');
        if (second < 10) {
            sb.append('0');
        }
        sb.append(second);
        sb.append('.');
        // Nine digits, and then the zeros are trimmed -- but at least one is left, because
        // `2020-01-01 00:00:00.` would not be a well-written instant.
        StringBuilder frac = new StringBuilder();
        frac.append(this.nanos);
        while (frac.length() < 9) {
            frac.insert(0, '0');
        }
        int end = 9;
        while (end > 1 && frac.charAt(end - 1) == '0') {
            end = end - 1;
        }
        sb.append(frac.substring(0, end));
        return sb.toString();
    }

    /** Whether they are the same instant, nanoseconds included. */
    public boolean equals(Timestamp ts) {
        if (ts == null) {
            return false;
        }
        return super.getTime() == ts.getTimeInternal() && this.nanos == ts.nanos;
    }

    long getTimeInternal() {
        return super.getTime();
    }

    /**
     * Whether `ts` is a `Timestamp` and they are the same instant.
     *
     * <p>It returns `false` for a `java.util.Date` standing for the same instant, even though
     * {@link #compareTo} returns zero. It is this class's documented asymmetry.
     */
    public boolean equals(Object ts) {
        if (ts instanceof Timestamp) {
            return this.equals((Timestamp) ts);
        }
        return false;
    }

    public int hashCode() {
        return (int) (super.getTime() ^ (super.getTime() >>> 32));
    }

    /** Whether this instant is before `ts`. */
    public boolean before(Timestamp ts) {
        return this.compareTo(ts) < 0;
    }

    /** Whether it is after. */
    public boolean after(Timestamp ts) {
        return this.compareTo(ts) > 0;
    }

    public int compareTo(Timestamp ts) {
        long a = super.getTime();
        long b = ts.getTimeInternal();
        if (a != b) {
            return a < b ? -1 : 1;
        }
        if (this.nanos == ts.nanos) {
            return 0;
        }
        return this.nanos < ts.nanos ? -1 : 1;
    }

    /** It compares against a `java.util.Date`, which has no nanoseconds. */
    public int compareTo(java.util.Date o) {
        if (o instanceof Timestamp) {
            return this.compareTo((Timestamp) o);
        }
        long a = this.getTime();
        long b = o.getTime();
        if (a == b) {
            return 0;
        }
        return a < b ? -1 : 1;
    }
}
