package java.util;

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
public class Date implements Comparable<Date> {

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
