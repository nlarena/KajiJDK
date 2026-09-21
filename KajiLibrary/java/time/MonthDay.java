package java.time;

import java.io.Serializable;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoField;

// KajiLibrary's java.time.MonthDay — a month-and-day, e.g. --08-04, without a year (a recurring
// annual date like a birthday). Immutable. Implements TemporalAccessor (read-only — a MonthDay
// isn't a full Temporal), TemporalAdjuster and Comparable. A KajiLibrary subset.
public final class MonthDay implements TemporalAccessor, TemporalAdjuster, Comparable<MonthDay>, Serializable {

    private final int month;
    private final int day;

    private MonthDay(int month, int day) {
        this.month = month;
        this.day = day;
    }

    /** Today's month-and-day, in the default zone. */
    public static MonthDay now() {
        LocalDate d = LocalDate.now();
        return MonthDay.of(d.getMonthValue(), d.getDayOfMonth());
    }

    /** The one `clock` reads. The testable form of `now()`. */
    public static MonthDay now(java.time.Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        LocalDate d = LocalDate.now(clock);
        return MonthDay.of(d.getMonthValue(), d.getDayOfMonth());
    }

    /** That zone's, right now. */
    public static MonthDay now(ZoneId zone) {
        if (zone == null) {
            throw new NullPointerException("zone");
        }
        LocalDate d = LocalDate.now(zone);
        return MonthDay.of(d.getMonthValue(), d.getDayOfMonth());
    }

    /** The month-and-day `temporal` holds. */
    public static MonthDay from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof MonthDay) {
            return (MonthDay) temporal;
        }
        return MonthDay.of(temporal.get(ChronoField.MONTH_OF_YEAR),
                temporal.get(ChronoField.DAY_OF_MONTH));
    }

    /** With the month as an enum. */
    public static MonthDay of(Month month, int dayOfMonth) {
        if (month == null) {
            throw new NullPointerException("month");
        }
        return MonthDay.of(month.getValue(), dayOfMonth);
    }

    public static MonthDay of(int month, int dayOfMonth) {
        return new MonthDay(month, dayOfMonth);
    }

    public int getMonthValue() {
        return this.month;
    }

    public Month getMonth() {
        return Month.of(this.month);
    }

    public int getDayOfMonth() {
        return this.day;
    }

    /**
     * Whether this month-and-day exists in that year.
     *
     * <p>Only the 29th of February can fail to exist, and it is precisely the case for which
     * `MonthDay` keeps 1..29 for February and not 1..28: a 29th of February is a valid
     * month-and-day, and which years it falls in is another question.
     */
    public boolean isValidYear(int year) {
        return !(this.getDayOfMonth() == 29 && this.getMonthValue() == 2
                && !Year.isLeap((long) year));
    }

    /** This month-and-day with another month; if the day does not exist there, it is clipped to the last. */
    public MonthDay withMonth(int month) {
        return this.with(Month.of(month));
    }

    public MonthDay with(Month month) {
        if (month == null) {
            throw new NullPointerException("month");
        }
        if (month.getValue() == this.getMonthValue()) {
            return this;
        }
        // It is clipped, not rejected: the 31st of January with the month set to April is the 30th of
        // April. It is what the JDK does, and the alternative --throwing-- would make `with` unusable
        // over any day past the 28th.
        int day = Math.min(this.getDayOfMonth(), month.maxLength());
        return MonthDay.of(month.getValue(), day);
    }

    /** With another day of the month. */
    public MonthDay withDayOfMonth(int dayOfMonth) {
        if (dayOfMonth == this.getDayOfMonth()) {
            return this;
        }
        return MonthDay.of(this.getMonthValue(), dayOfMonth);
    }

    /** Formateado. */
    public String format(java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        return formatter.format(this);
    }

    public LocalDate atYear(int year) {
        return LocalDate.of(year, this.month, this.day);
    }

    public boolean isBefore(MonthDay other) {
        return this.compareTo(other) < 0;
    }

    public boolean isAfter(MonthDay other) {
        return this.compareTo(other) > 0;
    }

    public int compareTo(MonthDay other) {
        if (this.month != other.month) {
            return this.month - other.month;
        }
        return this.day - other.day;
    }

    // --- TemporalAccessor / TemporalAdjuster ---

    public boolean isSupported(TemporalField field) {
        return field == ChronoField.MONTH_OF_YEAR || field == ChronoField.DAY_OF_MONTH;
    }

    public long getLong(TemporalField field) {
        if (field == ChronoField.MONTH_OF_YEAR) {
            return this.month;
        }
        if (field == ChronoField.DAY_OF_MONTH) {
            return this.day;
        }
        throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
    }

    public Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.MONTH_OF_YEAR, this.month).with(ChronoField.DAY_OF_MONTH, this.day);
    }

    // ISO-8601: --MM-dd
    public String toString() {
        StringBuilder buf = new StringBuilder("--");
        if (this.month < 10) {
            buf.append("0");
        }
        buf.append(Integer.toString(this.month));
        if (this.day < 10) {
            buf.append("-0");
        } else {
            buf.append("-");
        }
        buf.append(Integer.toString(this.day));
        return buf.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MonthDay) {
            MonthDay o = (MonthDay) obj;
            return this.month == o.month && this.day == o.day;
        }
        return false;
    }

    public int hashCode() {
        return (this.month << 6) + this.day;
    }

    // Parses --MM-dd.
    public static MonthDay parse(CharSequence text) {
        String s = text.toString();
        int month = parseDigits(s, 2, 4);
        int day = parseDigits(s, 5, 7);
        return MonthDay.of(month, day);
    }

    private static int parseDigits(String s, int from, int to) {
        int v = 0;
        for (int k = from; k < to; k = k + 1) {
            v = v * 10 + (s.charAt(k) - '0');
        }
        return v;
    }

    /**
     * It reads `text` with that formatter.
     *
     * <p>The one that decides which fields are there is the formatter; this class only says
     * **which of them it wants**, by passing its own `from`. That is why a pattern that brings no month and day
     * fails here and not when the result is used.
     *
     * @throws java.time.format.DateTimeParseException if the text does not fit the pattern, or what
     *     fits is not enough for a month and day
     */
    public static MonthDay parse(CharSequence text, java.time.format.DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        // Bound to a local: chaining through an interface-typed intermediate gets lost (#108).
        java.time.temporal.TemporalQuery<MonthDay> queryOf = MonthDay::from;
        return formatter.parse(text, queryOf);
    }
}
