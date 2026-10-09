package javax.xml.datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.namespace.QName;

/**
 * This library's concrete {@link XMLGregorianCalendar}.
 *
 * <p>Internal: it is not API and whoever uses it sees it as an {@code XMLGregorianCalendar}. It
 * really does the three things needed --parsing and writing the eight XML Schema lexical forms,
 * comparing with normalization to UTC, and adding durations-- without any XML parser involved: a
 * lexical date is a string of digits, hyphens and colons.
 *
 * <h2>The eight types, and how they are told apart</h2>
 *
 * <p>The class has no field saying which type it is: the type <b>is</b> the set of fields that are
 * set, and {@link #getXMLSchemaType} deduces it. A {@code gMonth} is an instance with the month set
 * and everything else at {@link DatatypeConstants#FIELD_UNDEFINED}.
 *
 * <p>It is what lets the eight types fit in a single class, and also what makes building it by hand
 * with the setters able to leave it in a state that is none of the eight. There {@code
 * getXMLSchemaType} and {@link #toXMLFormat} raise {@link IllegalStateException}, which is the only
 * honest thing: there is no lexical form to write.
 *
 * <h2>The comparison and the time zone</h2>
 *
 * <p>Two dates with a zone are taken to UTC and compared field by field. Two without a zone are
 * compared as they are. One with and one without is the interesting case: the one without a zone
 * could be anywhere in the 28-hour interval from {@code +14:00} to {@code -14:00}, so it is
 * compared against both ends and, if both give the same, that is the result; if not, it is {@link
 * DatatypeConstants#INDETERMINATE}.
 *
 * <p>That is why {@code 2024-05-25T12:00:00} against {@code 2024-05-25T12:00:00Z} gives
 * indeterminate --12 o'clock in Auckland has already passed and 12 o'clock in Honolulu has not
 * arrived-- and against {@code 2030-...} gives lesser, because six years are more than 28 hours.
 */
final class KajiXMLGregorianCalendar extends XMLGregorianCalendar {

    /** One minute in milliseconds. */
    private static final long MS_PER_MINUTE = 60000L;

    /** The billions of the year, or null. Always a multiple of a billion. */
    private BigInteger eon;

    /** The year without the eon, or {@link DatatypeConstants#FIELD_UNDEFINED}. */
    private int yearValue;

    /** The month from 1 to 12, or {@link DatatypeConstants#FIELD_UNDEFINED}. */
    private int monthValue;

    /** The day from 1 to 31, or {@link DatatypeConstants#FIELD_UNDEFINED}. */
    private int dayValue;

    /** The hour from 0 to 24, or {@link DatatypeConstants#FIELD_UNDEFINED}. */
    private int hourValue;

    /** The minute from 0 to 59, or {@link DatatypeConstants#FIELD_UNDEFINED}. */
    private int minuteValue;

    /** The second from 0 to 60, or {@link DatatypeConstants#FIELD_UNDEFINED}. */
    private int secondValue;

    /** The fraction of a second, from 0 inclusive to 1 exclusive, or null. */
    private BigDecimal fractionValue;

    /** The zone in minutes, from -840 to 840, or {@link DatatypeConstants#FIELD_UNDEFINED}. */
    private int timezoneValue;

    /** The values {@link #reset} leaves it with: the ones it was built with. */
    private final int[] initial;

    /** The initial eon, separate because it is not an {@code int}. */
    private final BigInteger initialEon;

    /** The initial fraction, likewise. */
    private final BigDecimal initialFraction;

    /** A date with all the fields undefined. */
    KajiXMLGregorianCalendar() {
        clearAll();
        this.initial = snapshot();
        this.initialEon = eon;
        this.initialFraction = fractionValue;
    }

    /**
     * Field by field, validating.
     *
     * @throws IllegalArgumentException if some field is out of range or the date does not exist
     */
    KajiXMLGregorianCalendar(BigInteger yearValue, int monthValue, int dayValue, int hourValue, int minuteValue, int secondValue,
            BigDecimal fractionValue, int timezoneValue) {
        clearAll();
        setYear(yearValue);
        setMonth(monthValue);
        setDay(dayValue);
        setHour(hourValue);
        setMinute(minuteValue);
        setSecond(secondValue);
        setFractionalSecond(fractionValue);
        setTimezone(timezoneValue);
        if (!isValid()) {
            throw new IllegalArgumentException(
                    "Year = " + this.yearValue + ", Month = " + this.monthValue + ", Day = " + this.dayValue
                            + ", Hour = " + this.hourValue + ", Minute = " + this.minuteValue
                            + ", Second = " + this.secondValue
                            + ", fractionalSecond = " + this.fractionValue
                            + ", Timezone = " + this.timezoneValue
                            + " , is not a valid representation of an XML Gregorian Calendar"
                            + " value.");
        }
        this.initial = snapshot();
        this.initialEon = this.eon;
        this.initialFraction = this.fractionValue;
    }

    /** A copy of another Gregorian calendar; all the fields end up defined. */
    KajiXMLGregorianCalendar(GregorianCalendar cal) {
        clearAll();
        if (cal == null) {
            throw new NullPointerException("cal is null");
        }
        setYear(cal.get(Calendar.YEAR));
        setMonth(cal.get(Calendar.MONTH) - Calendar.JANUARY + DatatypeConstants.JANUARY);
        setDay(cal.get(Calendar.DAY_OF_MONTH));
        setHour(cal.get(Calendar.HOUR_OF_DAY));
        setMinute(cal.get(Calendar.MINUTE));
        setSecond(cal.get(Calendar.SECOND));
        setMillisecond(cal.get(Calendar.MILLISECOND));
        // A `GregorianCalendar` always has a zone, so the result is always a complete dateTime. The
        // offset is taken from the calendar and not from the zone, so that a moment in daylight
        // saving time keeps the offset it really had.
        int offsetMs = cal.getTimeZone().getOffset(cal.getTimeInMillis());
        setTimezone((int) (((long) offsetMs) / MS_PER_MINUTE));
        this.initial = snapshot();
        this.initialEon = this.eon;
        this.initialFraction = this.fractionValue;
    }

    /** Leaves all the fields undefined, without touching the initial values. */
    private void clearAll() {
        eon = null;
        yearValue = DatatypeConstants.FIELD_UNDEFINED;
        monthValue = DatatypeConstants.FIELD_UNDEFINED;
        dayValue = DatatypeConstants.FIELD_UNDEFINED;
        hourValue = DatatypeConstants.FIELD_UNDEFINED;
        minuteValue = DatatypeConstants.FIELD_UNDEFINED;
        secondValue = DatatypeConstants.FIELD_UNDEFINED;
        fractionValue = null;
        timezoneValue = DatatypeConstants.FIELD_UNDEFINED;
    }

    /** The seven integer fields, for {@link #reset}. */
    private int[] snapshot() {
        return new int[] {yearValue, monthValue, dayValue, hourValue, minuteValue, secondValue, timezoneValue};
    }

    // ---- parsing of the eight lexical forms -----------------------------------------------------

    /**
     * From one of the eight XML Schema lexical forms.
     *
     * <p>Which one it is is decided by the form and not by a parameter, which is how the type
     * works: two hyphens at the start announce that there is no year, three that there is no month
     * either, and a {@code T} separates the date from the time. A string that starts with digits
     * and has colons before hyphens is a loose time.
     *
     * @param lexical the lexical form; cannot be null
     * @return the date
     * @throws IllegalArgumentException if it is none of the eight forms
     * @throws NullPointerException if it is null
     */
    static KajiXMLGregorianCalendar parse(String lexical) {
        if (lexical == null) {
            throw new NullPointerException("lexicalRepresentation is null");
        }
        Parser a = new Parser(lexical);
        KajiXMLGregorianCalendar c = new KajiXMLGregorianCalendar();
        try {
            if (a.peek("---")) {
                a.consume("---");
                c.setDay(a.fixedInt(2));
                a.optionalTimezone(c);
            } else if (a.peek("--")) {
                a.consume("--");
                c.setMonth(a.fixedInt(2));
                if (a.peek("-")) {
                    a.consume("-");
                    c.setDay(a.fixedInt(2));
                }
                a.optionalTimezone(c);
            } else if (a.hasLooseTime()) {
                a.readTime(c);
                a.optionalTimezone(c);
            } else {
                c.setYear(a.yearValue());
                if (a.peek("-")) {
                    a.consume("-");
                    c.setMonth(a.fixedInt(2));
                    if (a.peek("-")) {
                        a.consume("-");
                        c.setDay(a.fixedInt(2));
                        if (a.peek("T")) {
                            a.consume("T");
                            a.readTime(c);
                        }
                    }
                }
                a.optionalTimezone(c);
            }
            a.requireEnd();
            if (c.nextDay) {
                // `24:00:00` is midnight of the next day: only here, with the complete date, can
                // the day be moved.
                c.nextDay = false;
                c.shiftDays(1);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "\"" + lexical + "\" is not a valid representation of an XML Gregorian Calendar"
                            + " value: " + e.getMessage());
        }
        if (!c.isValid()) {
            throw new IllegalArgumentException(
                    "\"" + lexical + "\" is not a valid representation of an XML Gregorian Calendar"
                            + " value.");
        }
        return c;
    }

    /** A cursor over the lexical string; it exists only so that the parsing reads top to bottom. */
    private static final class Parser {

        private final String s;
        private int i;

        Parser(String s) {
            this.s = s;
            this.i = 0;
        }

        boolean peek(String p) {
            return s.startsWith(p, i);
        }

        void consume(String p) {
            if (!peek(p)) {
                throw new IllegalArgumentException("expected '" + p + "'");
            }
            i += p.length();
        }

        /** Whether what follows is a loose time: two digits and a colon. */
        boolean hasLooseTime() {
            return i + 2 < s.length() && isDigit(s.charAt(i)) && isDigit(s.charAt(i + 1))
                    && s.charAt(i + 2) == ':';
        }

        /** The year, which can have a sign and more than four digits. */
        BigInteger yearValue() {
            int from = i;
            if (i < s.length() && s.charAt(i) == '-') {
                i++;
            }
            int digits = 0;
            while (i < s.length() && isDigit(s.charAt(i))) {
                i++;
                digits++;
            }
            if (digits < 4) {
                throw new IllegalArgumentException("the year needs at least four digits");
            }
            return new BigInteger(s.substring(from, i));
        }

        /** An integer of exactly {@code n} digits. */
        int fixedInt(int n) {
            if (i + n > s.length()) {
                throw new IllegalArgumentException("expected " + n + " digits");
            }
            int v = 0;
            for (int k = 0; k < n; k++) {
                char c = s.charAt(i + k);
                if (!isDigit(c)) {
                    throw new IllegalArgumentException("expected " + n + " digits");
                }
                v = v * 10 + (c - '0');
            }
            i += n;
            return v;
        }

        /** {@code hh:mm:ss} with optional fraction. */
        void readTime(KajiXMLGregorianCalendar c) {
            int h = fixedInt(2);
            consume(":");
            c.setMinute(fixedInt(2));
            consume(":");
            c.setSecond(fixedInt(2));
            if (peek(".")) {
                int from = i;
                i++;
                int digits = 0;
                while (i < s.length() && isDigit(s.charAt(i))) {
                    i++;
                    digits++;
                }
                if (digits == 0) {
                    throw new IllegalArgumentException("the fraction needs at least one digit");
                }
                c.setFractionalSecond(new BigDecimal("0" + s.substring(from, i)));
            }
            if (h == 24) {
                // `24:00:00` is midnight of the next day and the only way the hour can be 24. It is
                // normalized here instead of being stored, because a stored 24 leaks later into the
                // comparison and `toXMLFormat`.
                if (c.minuteValue != 0 || c.secondValue != 0
                        || (c.fractionValue != null && c.fractionValue.signum() != 0)) {
                    throw new IllegalArgumentException("hour 24 is only valid as 24:00:00");
                }
                c.setHour(0);
                c.nextDay = true;
            } else {
                c.setHour(h);
            }
        }

        /** {@code Z} or {@code (+|-)hh:mm}, if there is one. */
        void optionalTimezone(KajiXMLGregorianCalendar c) {
            if (i >= s.length()) {
                return;
            }
            char ch = s.charAt(i);
            if (ch == 'Z') {
                i++;
                c.setTimezone(0);
                return;
            }
            if (ch != '+' && ch != '-') {
                return;
            }
            int sign = ch == '-' ? -1 : 1;
            i++;
            int h = fixedInt(2);
            consume(":");
            int m = fixedInt(2);
            c.setTimezone(sign * (h * 60 + m));
        }

        void requireEnd() {
            if (i != s.length()) {
                throw new IllegalArgumentException("trailing characters: \"" + s.substring(i) + "\"");
            }
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }
    }

    /** Marker of {@code 24:00:00}: a day has to be moved once the date is complete. */
    private transient boolean nextDay;

    // ---- setters -------------------------------------------------------------------------------

    /** {@inheritDoc} */
    public void clear() {
        clearAll();
    }

    /** {@inheritDoc} */
    public void reset() {
        yearValue = initial[0];
        monthValue = initial[1];
        dayValue = initial[2];
        hourValue = initial[3];
        minuteValue = initial[4];
        secondValue = initial[5];
        timezoneValue = initial[6];
        eon = initialEon;
        fractionValue = initialFraction;
    }

    /** {@inheritDoc} */
    public void setYear(BigInteger year) {
        if (year == null) {
            eon = null;
            yearValue = DatatypeConstants.FIELD_UNDEFINED;
            return;
        }
        // The year is split in two: what fits in an `int` --the remainder of dividing by a
        // billion-- and the rest, which is kept apart. It is how the original does it, and what
        // lets `getYear()` stay an `int` without capping the year.
        BigInteger ONE_BILLION = BigInteger.valueOf(1000000000L);
        BigInteger[] parts = year.divideAndRemainder(ONE_BILLION);
        BigInteger high = parts[0];
        BigInteger low = parts[1];
        if (high.signum() == 0) {
            eon = null;
        } else {
            eon = high.multiply(ONE_BILLION);
        }
        yearValue = low.intValue();
    }

    /** {@inheritDoc} */
    public void setYear(int year) {
        if (year == DatatypeConstants.FIELD_UNDEFINED) {
            eon = null;
            yearValue = DatatypeConstants.FIELD_UNDEFINED;
            return;
        }
        eon = null;
        yearValue = year;
    }

    /** {@inheritDoc} */
    public void setMonth(int month) {
        if (month != DatatypeConstants.FIELD_UNDEFINED
                && (month < DatatypeConstants.JANUARY || month > DatatypeConstants.DECEMBER)) {
            throw new IllegalArgumentException("invalid month: " + month);
        }
        monthValue = month;
    }

    /** {@inheritDoc} */
    public void setDay(int day) {
        if (day != DatatypeConstants.FIELD_UNDEFINED && (day < 1 || day > 31)) {
            throw new IllegalArgumentException("invalid day: " + day);
        }
        dayValue = day;
    }

    /** {@inheritDoc} */
    public void setTimezone(int offset) {
        if (offset != DatatypeConstants.FIELD_UNDEFINED
                && (offset < DatatypeConstants.MAX_TIMEZONE_OFFSET
                        || offset > DatatypeConstants.MIN_TIMEZONE_OFFSET)) {
            // The names of the constants are the other way round from the numbers; see
            // DatatypeConstants.
            throw new IllegalArgumentException("invalid timezone: " + offset);
        }
        timezoneValue = offset;
    }

    /** {@inheritDoc} */
    public void setHour(int hour) {
        if (hour != DatatypeConstants.FIELD_UNDEFINED && (hour < 0 || hour > 23)) {
            throw new IllegalArgumentException("invalid hour: " + hour);
        }
        hourValue = hour;
    }

    /** {@inheritDoc} */
    public void setMinute(int minute) {
        if (minute != DatatypeConstants.FIELD_UNDEFINED && (minute < 0 || minute > 59)) {
            throw new IllegalArgumentException("invalid minute: " + minute);
        }
        minuteValue = minute;
    }

    /** {@inheritDoc} */
    public void setSecond(int second) {
        // Sixty and not fifty-nine: XML Schema leaves room for the leap second.
        if (second != DatatypeConstants.FIELD_UNDEFINED && (second < 0 || second > 60)) {
            throw new IllegalArgumentException("invalid second: " + second);
        }
        secondValue = second;
    }

    /** {@inheritDoc} */
    public void setMillisecond(int millisecond) {
        if (millisecond == DatatypeConstants.FIELD_UNDEFINED) {
            fractionValue = null;
            return;
        }
        if (millisecond < 0 || millisecond > 999) {
            throw new IllegalArgumentException("invalid millisecond: " + millisecond);
        }
        fractionValue = BigDecimal.valueOf((long) millisecond, 3);
    }

    /** {@inheritDoc} */
    public void setFractionalSecond(BigDecimal fractional) {
        if (fractional != null) {
            if (fractional.signum() < 0 || fractional.compareTo(BigDecimal.ONE) >= 0) {
                throw new IllegalArgumentException(
                        "invalid fractional second: " + fractional
                                + ", must be in [0, 1)");
            }
        }
        fractionValue = fractional;
    }

    // ---- getters -------------------------------------------------------------------------------

    /** {@inheritDoc} */
    public BigInteger getEon() {
        return eon;
    }

    /** {@inheritDoc} */
    public int getYear() {
        return yearValue;
    }

    /** {@inheritDoc} */
    public BigInteger getEonAndYear() {
        if (yearValue == DatatypeConstants.FIELD_UNDEFINED) {
            return null;
        }
        BigInteger v = BigInteger.valueOf((long) yearValue);
        return eon == null ? v : eon.add(v);
    }

    /** {@inheritDoc} */
    public int getMonth() {
        return monthValue;
    }

    /** {@inheritDoc} */
    public int getDay() {
        return dayValue;
    }

    /** {@inheritDoc} */
    public int getTimezone() {
        return timezoneValue;
    }

    /** {@inheritDoc} */
    public int getHour() {
        return hourValue;
    }

    /** {@inheritDoc} */
    public int getMinute() {
        return minuteValue;
    }

    /** {@inheritDoc} */
    public int getSecond() {
        return secondValue;
    }

    /** {@inheritDoc} */
    public BigDecimal getFractionalSecond() {
        return fractionValue;
    }

    // ---- comparison ----------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>See the class header for the case of one with a zone and one without.
     */
    public int compare(XMLGregorianCalendar rhs) {
        if (rhs == null) {
            throw new NullPointerException("rhs is null");
        }
        boolean thisHasTimezone = timezoneValue != DatatypeConstants.FIELD_UNDEFINED;
        boolean otherHasTimezone = rhs.getTimezone() != DatatypeConstants.FIELD_UNDEFINED;

        if (thisHasTimezone == otherHasTimezone) {
            XMLGregorianCalendar a = thisHasTimezone ? normalize() : this;
            XMLGregorianCalendar b = otherHasTimezone ? rhs.normalize() : rhs;
            return fieldByField(a, b);
        }

        // One has a zone and the other does not: the one without can be at any point of the 28-hour
        // interval from +14:00 to -14:00. It is compared against both ends.
        XMLGregorianCalendar withTimezone = thisHasTimezone ? this : rhs;
        XMLGregorianCalendar withoutTimezone = thisHasTimezone ? rhs : this;
        XMLGregorianCalendar fixed = withTimezone.normalize();

        XMLGregorianCalendar boundA = withOffset(withoutTimezone, 840).normalize();
        XMLGregorianCalendar boundB = withOffset(withoutTimezone, -840).normalize();

        int c1;
        int c2;
        if (thisHasTimezone) {
            c1 = fieldByField(fixed, boundA);
            c2 = fieldByField(fixed, boundB);
        } else {
            c1 = fieldByField(boundA, fixed);
            c2 = fieldByField(boundB, fixed);
        }
        if (c1 == c2) {
            return c1;
        }
        return DatatypeConstants.INDETERMINATE;
    }

    /** A copy of {@code c} with the zone set to {@code offset}. */
    private static XMLGregorianCalendar withOffset(XMLGregorianCalendar c, int offset) {
        XMLGregorianCalendar copy = (XMLGregorianCalendar) c.clone();
        copy.setTimezone(offset);
        return copy;
    }

    /**
     * Compares two already normalized dates field by field.
     *
     * <p>A field that is in one and not in the other gives {@link DatatypeConstants#INDETERMINATE}:
     * it is not that they are different, it is that they are not of the same type and there is no
     * order between a {@code gYear} and a {@code gMonth}.
     */
    private static int fieldByField(XMLGregorianCalendar a, XMLGregorianCalendar b) {
        BigInteger yearA = a.getEonAndYear();
        BigInteger yearB = b.getEonAndYear();
        if ((yearA == null) != (yearB == null)) {
            return DatatypeConstants.INDETERMINATE;
        }
        if (yearA != null) {
            int c = yearA.compareTo(yearB);
            if (c != 0) {
                return c < 0 ? DatatypeConstants.LESSER : DatatypeConstants.GREATER;
            }
        }
        int c = compareInt(a.getMonth(), b.getMonth());
        if (c != DatatypeConstants.EQUAL) {
            return c;
        }
        c = compareInt(a.getDay(), b.getDay());
        if (c != DatatypeConstants.EQUAL) {
            return c;
        }
        c = compareInt(a.getHour(), b.getHour());
        if (c != DatatypeConstants.EQUAL) {
            return c;
        }
        c = compareInt(a.getMinute(), b.getMinute());
        if (c != DatatypeConstants.EQUAL) {
            return c;
        }
        c = compareInt(a.getSecond(), b.getSecond());
        if (c != DatatypeConstants.EQUAL) {
            return c;
        }
        BigDecimal fa = a.getFractionalSecond();
        BigDecimal fb = b.getFractionalSecond();
        BigDecimal ca = fa == null ? BigDecimal.ZERO : fa;
        BigDecimal cb = fb == null ? BigDecimal.ZERO : fb;
        int cf = ca.compareTo(cb);
        if (cf < 0) {
            return DatatypeConstants.LESSER;
        }
        if (cf > 0) {
            return DatatypeConstants.GREATER;
        }
        return DatatypeConstants.EQUAL;
    }

    /** Compares two integer fields; one defined and the other not gives indeterminate. */
    private static int compareInt(int a, int b) {
        boolean da = a != DatatypeConstants.FIELD_UNDEFINED;
        boolean db = b != DatatypeConstants.FIELD_UNDEFINED;
        if (da != db) {
            return DatatypeConstants.INDETERMINATE;
        }
        if (!da) {
            return DatatypeConstants.EQUAL;
        }
        if (a < b) {
            return DatatypeConstants.LESSER;
        }
        if (a > b) {
            return DatatypeConstants.GREATER;
        }
        return DatatypeConstants.EQUAL;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Without a zone there is nothing to normalize and a copy is returned: assuming UTC would be
     * inventing the missing datum, which is precisely what makes the comparison indeterminate.
     */
    public XMLGregorianCalendar normalize() {
        if (timezoneValue == DatatypeConstants.FIELD_UNDEFINED) {
            return (XMLGregorianCalendar) clone();
        }
        KajiXMLGregorianCalendar c = (KajiXMLGregorianCalendar) clone();
        if (timezoneValue != 0) {
            // To go to UTC the offset is subtracted from the local time. It is done on the fields
            // and not on an instant because the date may have no year: a `gMonthDay` with a zone
            // corresponds to no instant and yet it is normalized all the same.
            c.shiftMinutes(-timezoneValue);
        }
        c.timezoneValue = 0;
        return c;
    }

    /** Moves the date that many minutes, carrying between the fields that are defined. */
    private void shiftMinutes(int minutes) {
        if (minutes == 0 || hourValue == DatatypeConstants.FIELD_UNDEFINED) {
            return;
        }
        int total = hourValue * 60 + (minuteValue == DatatypeConstants.FIELD_UNDEFINED ? 0 : minuteValue) + minutes;
        int elapsedDays = Math.floorDiv(total, 1440);
        int withinDay = Math.floorMod(total, 1440);
        hourValue = withinDay / 60;
        if (minuteValue != DatatypeConstants.FIELD_UNDEFINED) {
            minuteValue = withinDay % 60;
        }
        if (elapsedDays != 0) {
            shiftDays(elapsedDays);
        }
    }

    /**
     * Moves the date that many days; without year and month there is nowhere to carry and it stays.
     */
    private void shiftDays(int days) {
        if (days == 0) {
            return;
        }
        if (yearValue == DatatypeConstants.FIELD_UNDEFINED || monthValue == DatatypeConstants.FIELD_UNDEFINED
                || dayValue == DatatypeConstants.FIELD_UNDEFINED) {
            // An `xs:time` with a zone has no date to move: the hour wraps around and that is it.
            // It is what the specification does, which defines the normalization of `time` modulo
            // 24 hours.
            return;
        }
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(yearValue, Calendar.JANUARY + monthValue - DatatypeConstants.JANUARY, dayValue);
        cal.add(Calendar.DAY_OF_MONTH, days);
        setYearFromCalendar(cal);
        monthValue = cal.get(Calendar.MONTH) - Calendar.JANUARY + DatatypeConstants.JANUARY;
        dayValue = cal.get(Calendar.DAY_OF_MONTH);
    }

    /** The calendar's year, keeping whatever eon there already was. */
    private void setYearFromCalendar(Calendar cal) {
        int fresh = cal.get(Calendar.YEAR);
        if (cal.get(Calendar.ERA) == GregorianCalendar.BC) {
            fresh = 1 - fresh;
        }
        yearValue = fresh;
    }

    // ---- lexical form ---------------------------------------------------------------------------

    /** {@inheritDoc} */
    public String toXMLFormat() {
        QName type = getXMLSchemaType();
        StringBuilder b = new StringBuilder();
        if (type == DatatypeConstants.DATETIME) {
            writeDate(b);
            b.append('T');
            writeTime(b);
        } else if (type == DatatypeConstants.DATE) {
            writeDate(b);
        } else if (type == DatatypeConstants.TIME) {
            writeTime(b);
        } else if (type == DatatypeConstants.GYEARMONTH) {
            writeYear(b);
            b.append('-');
            twoDigits(b, monthValue);
        } else if (type == DatatypeConstants.GMONTHDAY) {
            b.append("--");
            twoDigits(b, monthValue);
            b.append('-');
            twoDigits(b, dayValue);
        } else if (type == DatatypeConstants.GYEAR) {
            writeYear(b);
        } else if (type == DatatypeConstants.GMONTH) {
            b.append("--");
            twoDigits(b, monthValue);
        } else {
            b.append("---");
            twoDigits(b, dayValue);
        }
        writeTimezone(b);
        return b.toString();
    }

    private void writeDate(StringBuilder b) {
        writeYear(b);
        b.append('-');
        twoDigits(b, monthValue);
        b.append('-');
        twoDigits(b, dayValue);
    }

    private void writeTime(StringBuilder b) {
        twoDigits(b, hourValue);
        b.append(':');
        twoDigits(b, minuteValue);
        b.append(':');
        twoDigits(b, secondValue);
        if (fractionValue != null && fractionValue.signum() != 0) {
            // The fraction is written without the leading zero: `.500`, not `0.500`.
            String t = fractionValue.toPlainString();
            b.append(t.substring(t.indexOf('.')));
        }
    }

    /** The year with at least four digits, and the sign in front if it is negative. */
    private void writeYear(StringBuilder b) {
        BigInteger full = getEonAndYear();
        boolean negative = full.signum() < 0;
        String digits = full.abs().toString();
        if (negative) {
            b.append('-');
        }
        for (int i = digits.length(); i < 4; i++) {
            b.append('0');
        }
        b.append(digits);
    }

    private void writeTimezone(StringBuilder b) {
        if (timezoneValue == DatatypeConstants.FIELD_UNDEFINED) {
            return;
        }
        if (timezoneValue == 0) {
            b.append('Z');
            return;
        }
        int v = timezoneValue;
        if (v < 0) {
            b.append('-');
            v = -v;
        } else {
            b.append('+');
        }
        twoDigits(b, v / 60);
        b.append(':');
        twoDigits(b, v % 60);
    }

    private static void twoDigits(StringBuilder b, int v) {
        if (v < 10) {
            b.append('0');
        }
        b.append(v);
    }

    /** {@inheritDoc} */
    public QName getXMLSchemaType() {
        boolean hasYear = yearValue != DatatypeConstants.FIELD_UNDEFINED;
        boolean hasMonth = monthValue != DatatypeConstants.FIELD_UNDEFINED;
        boolean hasDay = dayValue != DatatypeConstants.FIELD_UNDEFINED;
        boolean hasHour = hourValue != DatatypeConstants.FIELD_UNDEFINED;

        if (hasYear && hasMonth && hasDay && hasHour) {
            return DatatypeConstants.DATETIME;
        }
        if (hasYear && hasMonth && hasDay) {
            return DatatypeConstants.DATE;
        }
        if (!hasYear && !hasMonth && !hasDay && hasHour) {
            return DatatypeConstants.TIME;
        }
        if (hasYear && hasMonth && !hasDay && !hasHour) {
            return DatatypeConstants.GYEARMONTH;
        }
        if (!hasYear && hasMonth && hasDay && !hasHour) {
            return DatatypeConstants.GMONTHDAY;
        }
        if (hasYear && !hasMonth && !hasDay && !hasHour) {
            return DatatypeConstants.GYEAR;
        }
        if (!hasYear && hasMonth && !hasDay && !hasHour) {
            return DatatypeConstants.GMONTH;
        }
        if (!hasYear && !hasMonth && hasDay && !hasHour) {
            return DatatypeConstants.GDAY;
        }
        throw new IllegalStateException(
                "javax.xml.datatype.XMLGregorianCalendar#getXMLSchemaType():"
                        + " this XMLGregorianCalendar does not match one of the eight XML Schema"
                        + " date/time datatypes: year set = " + hasYear
                        + " month set = " + hasMonth
                        + " day set = " + hasDay
                        + " time set = " + hasHour);
    }

    // ---- validity and arithmetic ----------------------------------------------------------------

    /** {@inheritDoc} */
    public boolean isValid() {
        // Year zero does not exist in XML Schema 1.0 and does in 1.1; 1.0 is followed, which is
        // what the original does: `0000` is not a date.
        if (yearValue == 0 && eon == null) {
            return false;
        }
        if (monthValue != DatatypeConstants.FIELD_UNDEFINED
                && (monthValue < DatatypeConstants.JANUARY || monthValue > DatatypeConstants.DECEMBER)) {
            return false;
        }
        if (dayValue != DatatypeConstants.FIELD_UNDEFINED) {
            if (dayValue < 1) {
                return false;
            }
            // The check no setter can do on its own: 31 February passes both ranges separately and
            // does not exist.
            int limit = monthValue == DatatypeConstants.FIELD_UNDEFINED
                    ? 31
                    : daysInMonth(monthValue, yearValue == DatatypeConstants.FIELD_UNDEFINED ? 2000 : yearValue);
            if (dayValue > limit) {
                return false;
            }
        }
        if (secondValue == 60) {
            // The leap second is only valid at 23:59:60 UTC. It is not validated more finely than
            // that because when there was a leap second is a historical table this library does not
            // have, and rejecting the ones that did exist would be worse than accepting too much.
            return true;
        }
        return true;
    }

    /**
     * How many days that month of that year has.
     *
     * <p>With {@code if} and not with {@code switch} because the frozen {@code bin/javac} still
     * does not fold a {@code case} whose label is a named constant read from a {@code .class}
     * --finding #503, closed in the compiler source but not in that binary-- and the ones of {@link
     * DatatypeConstants} are.
     */
    private static int daysInMonth(int monthValue, int yearValue) {
        if (monthValue == DatatypeConstants.FEBRUARY) {
            return isLeapYear(yearValue) ? 29 : 28;
        }
        if (monthValue == DatatypeConstants.APRIL || monthValue == DatatypeConstants.JUNE
                || monthValue == DatatypeConstants.SEPTEMBER || monthValue == DatatypeConstants.NOVEMBER) {
            return 30;
        }
        return 31;
    }

    /** The Gregorian rule. */
    private static boolean isLeapYear(int yearValue) {
        return (yearValue % 4 == 0 && yearValue % 100 != 0) || yearValue % 400 == 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The adjustment that surprises: when adding months leaves a day that does not exist in the
     * target month, the day is <b>clipped</b> to the last of the month. 31 January plus one month
     * is 28 February. The specification says so and it is what makes adding a month never change
     * month twice.
     */
    public void add(Duration duration) {
        if (duration == null) {
            throw new NullPointerException("duration is null");
        }
        int s = duration.getSign();
        if (s == 0) {
            return;
        }

        // Years and months together, which is how the specification defines them: they are added in
        // months and only afterwards is the day clipped.
        long monthsToAdd = 0L;
        BigInteger a = (BigInteger) duration.getField(DatatypeConstants.YEARS);
        if (a != null) {
            monthsToAdd += a.longValue() * 12L;
        }
        BigInteger m = (BigInteger) duration.getField(DatatypeConstants.MONTHS);
        if (m != null) {
            monthsToAdd += m.longValue();
        }
        if (monthsToAdd != 0L && monthValue != DatatypeConstants.FIELD_UNDEFINED) {
            long total = (long) (monthValue - DatatypeConstants.JANUARY) + (long) s * monthsToAdd;
            long extraYears = Math.floorDiv(total, 12L);
            int newMonth = (int) Math.floorMod(total, 12L) + DatatypeConstants.JANUARY;
            monthValue = newMonth;
            if (yearValue != DatatypeConstants.FIELD_UNDEFINED) {
                yearValue = (int) (yearValue + extraYears);
            }
            if (dayValue != DatatypeConstants.FIELD_UNDEFINED) {
                int limit = daysInMonth(
                        monthValue, yearValue == DatatypeConstants.FIELD_UNDEFINED ? 2000 : yearValue);
                if (dayValue > limit) {
                    dayValue = limit;
                }
            }
        }

        // Days, hours, minutes and seconds: all of those do have a fixed length, so they are
        // gathered into a single number of seconds --the fraction included-- and added at once.
        // Splitting it by field would force carrying by hand four times and it is where the
        // mistakes creep in.
        BigDecimal secondsToAdd = BigDecimal.ZERO;
        BigInteger d = (BigInteger) duration.getField(DatatypeConstants.DAYS);
        if (d != null) {
            secondsToAdd = secondsToAdd.add(new BigDecimal(d).multiply(BigDecimal.valueOf(86400L)));
        }
        BigInteger h = (BigInteger) duration.getField(DatatypeConstants.HOURS);
        if (h != null) {
            secondsToAdd = secondsToAdd.add(new BigDecimal(h).multiply(BigDecimal.valueOf(3600L)));
        }
        BigInteger mi = (BigInteger) duration.getField(DatatypeConstants.MINUTES);
        if (mi != null) {
            secondsToAdd = secondsToAdd.add(new BigDecimal(mi).multiply(BigDecimal.valueOf(60L)));
        }
        BigDecimal sec = (BigDecimal) duration.getField(DatatypeConstants.SECONDS);
        if (sec != null) {
            secondsToAdd = secondsToAdd.add(sec);
        }
        if (s < 0) {
            secondsToAdd = secondsToAdd.negate();
        }
        if (secondsToAdd.signum() == 0) {
            return;
        }

        // The current time of day, in seconds. An undefined field counts as zero: in an `xs:date`
        // there is no time to move and the only thing that survives is the carry into days.
        BigDecimal withinDay = BigDecimal.ZERO;
        if (hourValue != DatatypeConstants.FIELD_UNDEFINED) {
            withinDay = withinDay.add(BigDecimal.valueOf((long) hourValue * 3600L));
        }
        if (minuteValue != DatatypeConstants.FIELD_UNDEFINED) {
            withinDay = withinDay.add(BigDecimal.valueOf((long) minuteValue * 60L));
        }
        if (secondValue != DatatypeConstants.FIELD_UNDEFINED) {
            withinDay = withinDay.add(BigDecimal.valueOf((long) secondValue));
        }
        if (fractionValue != null) {
            withinDay = withinDay.add(fractionValue);
        }

        BigDecimal total = withinDay.add(secondsToAdd);
        BigDecimal perDay = BigDecimal.valueOf(86400L);
        BigDecimal wholeDays = total.divide(perDay, 0, RoundingMode.FLOOR);
        BigDecimal remainder = total.subtract(wholeDays.multiply(perDay));

        BigDecimal intRemainder = remainder.setScale(0, RoundingMode.FLOOR);
        long secondsRemainder = intRemainder.longValue();
        BigDecimal newFraction = remainder.subtract(intRemainder);

        if (hourValue != DatatypeConstants.FIELD_UNDEFINED) {
            hourValue = (int) (secondsRemainder / 3600L);
        }
        if (minuteValue != DatatypeConstants.FIELD_UNDEFINED) {
            minuteValue = (int) ((secondsRemainder % 3600L) / 60L);
        }
        if (secondValue != DatatypeConstants.FIELD_UNDEFINED) {
            secondValue = (int) (secondsRemainder % 60L);
        }
        if (fractionValue != null || newFraction.signum() != 0) {
            fractionValue = newFraction;
        }
        shiftDaysLong(wholeDays.longValue());
    }

    /** Like {@link #shiftMinutes} but for values that do not fit in an {@code int}. */
    private void shiftMinutesLong(long minutes) {
        if (hourValue == DatatypeConstants.FIELD_UNDEFINED) {
            long days = Math.floorDiv(minutes, 1440L);
            shiftDaysLong(days);
            return;
        }
        long total = (long) hourValue * 60L
                + (minuteValue == DatatypeConstants.FIELD_UNDEFINED ? 0L : (long) minuteValue)
                + minutes;
        long days = Math.floorDiv(total, 1440L);
        int withinDay = (int) Math.floorMod(total, 1440L);
        hourValue = withinDay / 60;
        if (minuteValue != DatatypeConstants.FIELD_UNDEFINED) {
            minuteValue = withinDay % 60;
        }
        shiftDaysLong(days);
    }

    /** Moves that many days, in stretches that fit in an {@code int}. */
    private void shiftDaysLong(long days) {
        long pending = days;
        while (pending != 0L) {
            int segment;
            if (pending > (long) Integer.MAX_VALUE) {
                segment = Integer.MAX_VALUE;
            } else if (pending < (long) Integer.MIN_VALUE) {
                segment = Integer.MIN_VALUE;
            } else {
                segment = (int) pending;
            }
            shiftDays(segment);
            pending -= (long) segment;
        }
    }

    // ---- conversions ---------------------------------------------------------------------------

    /** {@inheritDoc} */
    public GregorianCalendar toGregorianCalendar() {
        return toGregorianCalendar(null, null, null);
    }

    /** {@inheritDoc} */
    public GregorianCalendar toGregorianCalendar(
            TimeZone timezone, Locale aLocale, XMLGregorianCalendar defaults) {
        TimeZone tz = timezone;
        if (timezoneValue != DatatypeConstants.FIELD_UNDEFINED) {
            tz = getTimeZone(DatatypeConstants.FIELD_UNDEFINED);
        } else if (tz == null && defaults != null
                && defaults.getTimezone() != DatatypeConstants.FIELD_UNDEFINED) {
            tz = defaults.getTimeZone(DatatypeConstants.FIELD_UNDEFINED);
        }
        GregorianCalendar cal = tz == null
                ? new GregorianCalendar()
                : new GregorianCalendar(tz);
        cal.clear();

        cal.set(Calendar.YEAR, fieldOrDefault(yearValue, defaults == null
                ? DatatypeConstants.FIELD_UNDEFINED : defaults.getYear(), 1970));
        cal.set(Calendar.MONTH,
                Calendar.JANUARY + fieldOrDefault(monthValue, defaults == null
                        ? DatatypeConstants.FIELD_UNDEFINED : defaults.getMonth(),
                        DatatypeConstants.JANUARY) - DatatypeConstants.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, fieldOrDefault(dayValue, defaults == null
                ? DatatypeConstants.FIELD_UNDEFINED : defaults.getDay(), 1));
        cal.set(Calendar.HOUR_OF_DAY, fieldOrDefault(hourValue, defaults == null
                ? DatatypeConstants.FIELD_UNDEFINED : defaults.getHour(), 0));
        cal.set(Calendar.MINUTE, fieldOrDefault(minuteValue, defaults == null
                ? DatatypeConstants.FIELD_UNDEFINED : defaults.getMinute(), 0));
        cal.set(Calendar.SECOND, fieldOrDefault(secondValue, defaults == null
                ? DatatypeConstants.FIELD_UNDEFINED : defaults.getSecond(), 0));
        BigDecimal f = fractionValue;
        if (f == null && defaults != null) {
            f = defaults.getFractionalSecond();
        }
        cal.set(Calendar.MILLISECOND,
                f == null ? 0 : f.movePointRight(3).intValue());
        return cal;
    }

    /** The field, or the one of the default calendar, or the one of the epoch. */
    private static int fieldOrDefault(int own, int defaultOf, int fromEpoch) {
        if (own != DatatypeConstants.FIELD_UNDEFINED) {
            return own;
        }
        if (defaultOf != DatatypeConstants.FIELD_UNDEFINED) {
            return defaultOf;
        }
        return fromEpoch;
    }

    /** {@inheritDoc} */
    public TimeZone getTimeZone(int defaultZoneoffset) {
        int use = timezoneValue != DatatypeConstants.FIELD_UNDEFINED ? timezoneValue : defaultZoneoffset;
        if (use == DatatypeConstants.FIELD_UNDEFINED) {
            return null;
        }
        StringBuilder id = new StringBuilder("GMT");
        int v = use;
        if (v < 0) {
            id.append('-');
            v = -v;
        } else {
            id.append('+');
        }
        twoDigits(id, v / 60);
        id.append(':');
        twoDigits(id, v % 60);
        return TimeZone.getTimeZone(id.toString());
    }

    /** {@inheritDoc} */
    public Object clone() {
        KajiXMLGregorianCalendar c = new KajiXMLGregorianCalendar();
        c.eon = eon;
        c.yearValue = yearValue;
        c.monthValue = monthValue;
        c.dayValue = dayValue;
        c.hourValue = hourValue;
        c.minuteValue = minuteValue;
        c.secondValue = secondValue;
        c.fractionValue = fractionValue;
        c.timezoneValue = timezoneValue;
        return c;
    }
}
