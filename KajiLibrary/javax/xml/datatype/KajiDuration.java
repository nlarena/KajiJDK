package javax.xml.datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * This library's concrete {@link Duration}.
 *
 * <p>Internal: it is not part of the API, it is not public, and whoever uses it sees it as a {@code
 * Duration}. What this class contributes is that this library's durations <b>really compute</b>
 * --they parse their lexical form, add, multiply, normalize and compare-- without any XML parser
 * being needed: a duration is arithmetic over six numbers.
 *
 * <h2>How it is stored</h2>
 *
 * <p>A sign and six non-negative magnitudes, each of which can be null --the field is not set--. It
 * is the representation the API asks for: {@link Duration#getField} returns null for an absent
 * field and {@link Duration#getSign} gives the sign separately, so storing six signed numbers would
 * force rebuilding that form on every query.
 *
 * <h2>The comparison, which is the most delicate part</h2>
 *
 * <p>{@link #compare} implements XML Schema's normative algorithm, which is indirect and worth
 * knowing: both durations are added to <b>four reference instants</b> fixed by the specification
 * --1696-09-01, 1697-02-01, 1903-03-01 and 1903-07-01-- and, if the four comparisons agree, that is
 * the result; if not, it is {@link DatatypeConstants#INDETERMINATE}.
 *
 * <p>The four are not arbitrary: between them they cover a February of 28 days and one of 29, and a
 * month of 30 and one of 31. That is, they are a counterexample for each way a month can measure
 * differently. If the four give the same, no choice of month changes the result, and there the
 * order does exist.
 *
 * <p>That is why {@code P1M} against {@code P30D} gives indeterminate --in a 28-day February it is
 * shorter and in March it is longer-- and {@code PT60S} against {@code PT1M} gives equal.
 */
final class KajiDuration extends Duration {

    /** The specification's four reference instants, as {@code yyyymmdd}. */
    private static final int[][] REFERENCES = {
        {1696, 9, 1}, {1697, 2, 1}, {1903, 3, 1}, {1903, 7, 1},
    };

    /** -1, 0 or 1. Zero only if all the fields set are zero. */
    private final int sign;

    /** The years, non-negative, or null if the field is not there. */
    private final BigInteger years;

    /** The months, non-negative, or null. */
    private final BigInteger months;

    /** The days, non-negative, or null. */
    private final BigInteger days;

    /** The hours, non-negative, or null. */
    private final BigInteger hours;

    /** The minutes, non-negative, or null. */
    private final BigInteger minutes;

    /** The seconds, non-negative and with fraction, or null. */
    private final BigDecimal seconds;

    /**
     * Field by field, which is the constructor all the others end up using.
     *
     * @param positive the requested sign
     * @param years the years, or null
     * @param months the months, or null
     * @param days the days, or null
     * @param hours the hours, or null
     * @param minutes the minutes, or null
     * @param seconds the seconds, or null
     * @throws IllegalArgumentException if all six are null or if some is negative
     */
    KajiDuration(boolean positive, BigInteger years, BigInteger months, BigInteger days,
            BigInteger hours, BigInteger minutes, BigDecimal seconds) {
        this.years = years;
        this.months = months;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;

        if (years == null && months == null && days == null
                && hours == null && minutes == null && seconds == null) {
            throw new IllegalArgumentException(
                    "all the fields are null, at least one field must be non-null");
        }
        requireNonNegative(years, "years");
        requireNonNegative(months, "months");
        requireNonNegative(days, "days");
        requireNonNegative(hours, "hours");
        requireNonNegative(minutes, "minutes");
        if (seconds != null && seconds.signum() < 0) {
            throw new IllegalArgumentException("seconds is negative: " + seconds);
        }

        // The zero sign is not requested: it is deduced. A duration of all zeros is the same
        // whether asked for positive or negative, and `getSign()` answering 1 for `-P0D` would be
        // an observable difference between two objects that represent the same thing.
        if (isAllZero()) {
            this.sign = 0;
        } else {
            this.sign = positive ? 1 : -1;
        }
    }

    /** An integer field has to be non-negative: the sign goes separately. */
    private static void requireNonNegative(BigInteger v, String fieldName) {
        if (v != null && v.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " is negative: " + v);
        }
    }

    /** Whether all the fields set are zero. */
    private boolean isAllZero() {
        return isZero(years) && isZero(months) && isZero(days)
                && isZero(hours) && isZero(minutes)
                && (seconds == null || seconds.signum() == 0);
    }

    /** Null counts as zero for deciding the sign. */
    private static boolean isZero(BigInteger v) {
        return v == null || v.signum() == 0;
    }

    // ---- the three ways of building -------------------------------------------------------------

    /**
     * From the lexical form {@code -?PnYnMnDTnHnMnS}.
     *
     * <p>The parsing is by hand and not with a regular expression, for two concrete reasons. The
     * first is that error messages that say <b>what</b> is wrong are needed --an expression that
     * does not match can only say that it does not match--. The second is that two of the rules are
     * not comfortable to express in an expression: that there has to be at least one field, and
     * that the {@code T} cannot be alone.
     *
     * @param lexical the lexical form; cannot be null
     * @return the duration
     * @throws IllegalArgumentException if the form is wrong
     * @throws NullPointerException if it is null
     */
    static KajiDuration parse(String lexical) {
        if (lexical == null) {
            throw new NullPointerException("lexicalRepresentation is null");
        }
        int n = lexical.length();
        int i = 0;
        boolean positive = true;
        if (i < n && lexical.charAt(i) == '-') {
            positive = false;
            i++;
        }
        if (i >= n || lexical.charAt(i) != 'P') {
            throw new IllegalArgumentException(badFormat(lexical, "missing 'P'"));
        }
        i++;

        BigInteger[] datePart = new BigInteger[3];   // Y, M, D
        BigInteger[] hourValue = new BigInteger[2];    // H, M
        BigDecimal[] secs = new BigDecimal[1];

        // The date part: the designators have to come in order and not repeat.
        String dateDesignators = "YMD";
        int next = 0;
        boolean sawSomething = false;
        while (i < n && lexical.charAt(i) != 'T') {
            int end = endOfNumber(lexical, i);
            if (end == i) {
                throw new IllegalArgumentException(badFormat(lexical, "expected a number"));
            }
            if (end >= n) {
                throw new IllegalArgumentException(badFormat(lexical, "number without a designator"));
            }
            char d = lexical.charAt(end);
            int pos = dateDesignators.indexOf(d);
            if (pos < 0 || pos < next) {
                throw new IllegalArgumentException(
                        badFormat(lexical, "unexpected designator '" + d + "'"));
            }
            datePart[pos] = intOf(lexical.substring(i, end), lexical);
            next = pos + 1;
            sawSomething = true;
            i = end + 1;
        }

        if (i < n && lexical.charAt(i) == 'T') {
            i++;
            if (i >= n) {
                // The `T` announces that a time part follows; with nothing after it, it is an
                // invalid form and not an empty time part.
                throw new IllegalArgumentException(badFormat(lexical, "'T' without a time part"));
            }
            String timeDesignators = "HMS";
            next = 0;
            while (i < n) {
                int end = endOfNumber(lexical, i);
                if (end == i) {
                    throw new IllegalArgumentException(badFormat(lexical, "expected a number"));
                }
                if (end >= n) {
                    throw new IllegalArgumentException(
                            badFormat(lexical, "number without a designator"));
                }
                char d = lexical.charAt(end);
                int pos = timeDesignators.indexOf(d);
                if (pos < 0 || pos < next) {
                    throw new IllegalArgumentException(
                            badFormat(lexical, "unexpected designator '" + d + "'"));
                }
                String text = lexical.substring(i, end);
                if (pos == 2) {
                    secs[0] = decimalOf(text, lexical);
                } else {
                    if (text.indexOf('.') >= 0) {
                        // Only the seconds can have a fraction; half an hour is written `PT30M`.
                        throw new IllegalArgumentException(
                                badFormat(lexical, "only seconds may have a fraction"));
                    }
                    hourValue[pos] = intOf(text, lexical);
                }
                next = pos + 1;
                sawSomething = true;
                i = end + 1;
            }
        }

        if (!sawSomething) {
            throw new IllegalArgumentException(badFormat(lexical, "no fields"));
        }
        return new KajiDuration(
                positive, datePart[0], datePart[1], datePart[2], hourValue[0], hourValue[1], secs[0]);
    }

    /** Where the number that starts at {@code i} ends (digits and at most one point). */
    private static int endOfNumber(String s, int i) {
        int j = i;
        while (j < s.length()) {
            char c = s.charAt(j);
            if ((c >= '0' && c <= '9') || c == '.') {
                j++;
            } else {
                break;
            }
        }
        return j;
    }

    /** An integer field, with the contract's error if it is not one. */
    private static BigInteger intOf(String text, String lexical) {
        try {
            return new BigInteger(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(badFormat(lexical, "'" + text + "' is not a number"));
        }
    }

    /** The seconds field, which can have a fraction. */
    private static BigDecimal decimalOf(String text, String lexical) {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(badFormat(lexical, "'" + text + "' is not a number"));
        }
    }

    /** The invalid lexical form message, with the concrete reason. */
    private static String badFormat(String lexical, String reason) {
        return "\"" + lexical + "\" is not a valid representation of an XML Schema duration: "
                + reason;
    }

    /**
     * From a number of milliseconds, with the six fields set.
     *
     * <p>The years and months do not come from dividing: they come from <b>counting on the
     * calendar</b> from the epoch. It is the only right way, because a month has no fixed length
     * and any divisor chosen --30 days, 30.44 days-- gives a result that corresponds to no real
     * date.
     *
     * <p>The trick that makes the calculation simple: the epoch is <b>1</b> January, so the day of
     * the target month is always greater than or equal to 1 and the month always greater than or
     * equal to January. The field-by-field subtraction never borrows, and the adjustment a date
     * difference normally needs is not required.
     *
     * @param milliseconds the milliseconds, signed
     * @return the duration
     */
    static KajiDuration fromMillis(long milliseconds) {
        boolean positive = milliseconds >= 0;
        long remainder = milliseconds < 0 ? -milliseconds : milliseconds;

        long millis = remainder % 1000L;
        remainder /= 1000L;
        long secs = remainder % 60L;
        remainder /= 60L;
        long min = remainder % 60L;
        remainder /= 60L;
        long hrs = remainder % 24L;
        long totalDays = remainder / 24L;

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(0L);
        // `Calendar.add` takes an `int`; for huge values it is added in stretches.
        long pending = totalDays;
        while (pending > 0L) {
            int segment = pending > (long) Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pending;
            cal.add(Calendar.DAY_OF_MONTH, segment);
            pending -= (long) segment;
        }

        int years = cal.get(Calendar.YEAR) - 1970;
        int months = cal.get(Calendar.MONTH) - Calendar.JANUARY;
        int days = cal.get(Calendar.DAY_OF_MONTH) - 1;

        return new KajiDuration(
                positive,
                BigInteger.valueOf((long) years),
                BigInteger.valueOf((long) months),
                BigInteger.valueOf((long) days),
                BigInteger.valueOf(hrs),
                BigInteger.valueOf(min),
                // Scale three: the milliseconds are the fraction, and keeping it that way makes
                // `toString` write `1.000S` like the original and not `1S`.
                BigDecimal.valueOf(secs * 1000L + millis, 3));
    }

    // ---- the six integer accessors, which the reference implementation redefines ----------------

    // The abstract class documents that `getYears()` and its siblings answer FIELD_UNDEFINED when
    // the field is not there, and that is how it is written in `Duration`. The reference
    // implementation --Xerces, the one the JDK brings-- redefines them and answers **zero**:
    // checked against `H:/jdk-25.0.2`, where `newDuration("P1M").getDays()` gives 0 and not
    // -2147483648, while `getField(DAYS)` gives null and `isSet(DAYS)` gives false in both.
    //
    // That is, the JDK departs from its own contract there. The reference implementation's
    // behaviour is replicated and not the javadoc's, for a concrete reason: code written against
    // the JDK works against this, and whoever wants to tell "absent" from "zero" has `isSet` and
    // `getField`, which do tell the truth in both.

    /** {@inheritDoc} */
    public int getYears() {
        return intOrZero(DatatypeConstants.YEARS);
    }

    /** {@inheritDoc} */
    public int getMonths() {
        return intOrZero(DatatypeConstants.MONTHS);
    }

    /** {@inheritDoc} */
    public int getDays() {
        return intOrZero(DatatypeConstants.DAYS);
    }

    /** {@inheritDoc} */
    public int getHours() {
        return intOrZero(DatatypeConstants.HOURS);
    }

    /** {@inheritDoc} */
    public int getMinutes() {
        return intOrZero(DatatypeConstants.MINUTES);
    }

    /** {@inheritDoc} */
    public int getSeconds() {
        return intOrZero(DatatypeConstants.SECONDS);
    }

    /** The field as an integer, or zero if it is not set. */
    private int intOrZero(javax.xml.datatype.DatatypeConstants.Field fieldId) {
        Number n = getField(fieldId);
        return n == null ? 0 : n.intValue();
    }

    // ---- what the abstract class asks for -------------------------------------------------------

    /** {@inheritDoc} */
    public int getSign() {
        return sign;
    }

    /** {@inheritDoc} */
    public Number getField(javax.xml.datatype.DatatypeConstants.Field field) {
        if (field == null) {
            throw new NullPointerException("field is null");
        }
        if (field == DatatypeConstants.YEARS) {
            return years;
        }
        if (field == DatatypeConstants.MONTHS) {
            return months;
        }
        if (field == DatatypeConstants.DAYS) {
            return days;
        }
        if (field == DatatypeConstants.HOURS) {
            return hours;
        }
        if (field == DatatypeConstants.MINUTES) {
            return minutes;
        }
        if (field == DatatypeConstants.SECONDS) {
            return seconds;
        }
        throw new IllegalArgumentException("unknown field: " + field);
    }

    /** {@inheritDoc} */
    public boolean isSet(javax.xml.datatype.DatatypeConstants.Field field) {
        return getField(field) != null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>It adds field by field with the signs applied, and then requires all the results to have
     * the same sign. That requirement is what makes {@code P1M + (-P30D)} throw: it would give a
     * positive month and thirty negative days, and there is no way of writing that as a duration
     * --which is a sign and six magnitudes-- nor of knowing what its sign would be without choosing
     * a month.
     *
     * <p>A field is set in the result if it was set in either of the two addends.
     */
    public Duration add(Duration rhs) {
        if (rhs == null) {
            throw new NullPointerException("rhs is null");
        }
        BigDecimal[] sum = new BigDecimal[6];
        boolean[] wasSet = new boolean[6];
        for (int i = 0; i < 6; i++) {
            javax.xml.datatype.DatatypeConstants.Field fieldId = fieldByIndex(i);
            BigDecimal a = withSign(this, fieldId);
            BigDecimal b = withSign(rhs, fieldId);
            sum[i] = a.add(b);
            wasSet[i] = isSet(fieldId) || rhs.isSet(fieldId);
        }

        // All the non-null fields of the result have to point the same way.
        int resultSign = 0;
        for (int i = 0; i < 6; i++) {
            int s = sum[i].signum();
            if (s == 0) {
                continue;
            }
            if (resultSign == 0) {
                resultSign = s;
            } else if (resultSign != s) {
                throw new IllegalStateException(
                        this + " + " + rhs + " is not a valid duration:"
                                + " the result would have fields of both signs");
            }
        }
        boolean positive = resultSign >= 0;

        return new KajiDuration(
                positive,
                wasSet[0] ? sum[0].abs().toBigInteger() : null,
                wasSet[1] ? sum[1].abs().toBigInteger() : null,
                wasSet[2] ? sum[2].abs().toBigInteger() : null,
                wasSet[3] ? sum[3].abs().toBigInteger() : null,
                wasSet[4] ? sum[4].abs().toBigInteger() : null,
                wasSet[5] ? sum[5].abs() : null);
    }

    /** The field with the duration's sign applied; zero if it is not set. */
    private static BigDecimal withSign(Duration d, javax.xml.datatype.DatatypeConstants.Field fieldId) {
        Number n = d.getField(fieldId);
        if (n == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal v = n instanceof BigDecimal ? (BigDecimal) n : new BigDecimal((BigInteger) n);
        return d.getSign() < 0 ? v.negate() : v;
    }

    /** The field corresponding to index 0..5. */
    private static javax.xml.datatype.DatatypeConstants.Field fieldByIndex(int i) {
        switch (i) {
            case 0: return DatatypeConstants.YEARS;
            case 1: return DatatypeConstants.MONTHS;
            case 2: return DatatypeConstants.DAYS;
            case 3: return DatatypeConstants.HOURS;
            case 4: return DatatypeConstants.MINUTES;
            default: return DatatypeConstants.SECONDS;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The order --years, months, days, hours, minutes, seconds-- is fixed by the specification
     * and is not interchangeable: adding a month and a day to 31 January gives 29 February, and
     * adding a day and a month gives 1 March. A fixed order is the only thing that makes the
     * operation reproducible.
     */
    public void addTo(Calendar calendar) {
        if (calendar == null) {
            throw new NullPointerException("calendar is null");
        }
        int s = sign;
        if (s == 0) {
            return;
        }
        addField(calendar, Calendar.YEAR, years, s);
        addField(calendar, Calendar.MONTH, months, s);
        addField(calendar, Calendar.DAY_OF_MONTH, days, s);
        addField(calendar, Calendar.HOUR_OF_DAY, hours, s);
        addField(calendar, Calendar.MINUTE, minutes, s);
        if (seconds != null) {
            // The seconds are added in milliseconds so as not to lose the fraction, which is
            // precisely what tells `PT0.5S` from `PT0S`.
            long millis = seconds.movePointRight(3).setScale(0, RoundingMode.DOWN).longValue();
            calendar.setTimeInMillis(calendar.getTimeInMillis() + (long) s * millis);
        }
    }

    /** Adds an integer field to the calendar, respecting the duration's sign. */
    private static void addField(Calendar cal, int fieldId, BigInteger value, int sign) {
        if (value == null || value.signum() == 0) {
            return;
        }
        long pending = value.longValue();
        while (pending > 0L) {
            int segment = pending > (long) Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pending;
            cal.add(fieldId, sign * segment);
            pending -= (long) segment;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The fraction left in a field goes down to the next one --half a year is six months, half a
     * day is twelve hours-- with a single exception: from months one cannot go down to days,
     * because there is no fixed equivalence. Multiplying {@code P1M} by {@code 0.5} throws, and
     * that is the same ambiguity that makes {@link DatatypeConstants#INDETERMINATE} necessary.
     */
    public Duration multiply(BigDecimal factor) {
        if (factor == null) {
            throw new NullPointerException("factor is null");
        }
        boolean positive = (sign >= 0) == (factor.signum() >= 0);
        BigDecimal f = factor.abs();

        BigDecimal[] v = new BigDecimal[6];
        for (int i = 0; i < 6; i++) {
            Number n = getField(fieldByIndex(i));
            if (n == null) {
                v[i] = null;
            } else if (n instanceof BigDecimal) {
                v[i] = ((BigDecimal) n).multiply(f);
            } else {
                v[i] = new BigDecimal((BigInteger) n).multiply(f);
            }
        }

        // Fractions go down from one field to the next, from top to bottom.
        BigInteger[] ints = new BigInteger[6];
        BigDecimal carry = BigDecimal.ZERO;
        for (int i = 0; i < 5; i++) {
            if (v[i] == null) {
                if (carry.signum() != 0) {
                    // The carry has nowhere to go: the target field does not exist in this
                    // duration, so it is pushed to the next one that does.
                    carry = carry.multiply(BigDecimal.valueOf(factorTo(i + 1)));
                }
                continue;
            }
            BigDecimal total = v[i].add(carry);
            BigDecimal integerPart = total.setScale(0, RoundingMode.DOWN);
            BigDecimal fractionValue = total.subtract(integerPart);
            ints[i] = integerPart.toBigInteger();
            if (fractionValue.signum() != 0) {
                if (i == 1) {
                    throw new IllegalStateException(
                            "multiplying " + this + " by " + factor
                                    + " leaves a fraction of a month, which cannot be converted"
                                    + " to days: a month has no fixed number of days");
                }
                carry = fractionValue.multiply(BigDecimal.valueOf(factorTo(i + 1)));
            } else {
                carry = BigDecimal.ZERO;
            }
        }
        BigDecimal secs = v[5];
        if (secs != null) {
            secs = secs.add(carry);
        } else if (carry.signum() != 0) {
            // The same as above: if there is no seconds field, the fraction is lost. A field the
            // duration did not have is not invented.
            carry = BigDecimal.ZERO;
        }

        return new KajiDuration(
                positive, ints[0], ints[1], ints[2], ints[3], ints[4], secs);
    }

    /** How many units of field {@code i} fit into one of field {@code i-1}. */
    private static long factorTo(int i) {
        switch (i) {
            case 1: return 12L;   // months in a year
            case 3: return 24L;   // hours in a day
            case 4: return 60L;   // minutes in an hour
            case 5: return 60L;   // seconds in a minute
            default: return 1L;   // no factor from months to days: that case throws earlier
        }
    }

    /** {@inheritDoc} */
    public Duration negate() {
        return new KajiDuration(sign >= 0 ? false : true,
                years, months, days, hours, minutes, seconds);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Once the starting point is fixed, a month has a number of days: the years and months are
     * added to the calendar, how many days it moved is measured, and those days replace the two
     * fields. The result has no months, so it can be compared with any other without giving
     * indeterminate.
     */
    public Duration normalizeWith(Calendar startTimeInstant) {
        if (startTimeInstant == null) {
            throw new NullPointerException("startTimeInstant is null");
        }
        Calendar cal = new GregorianCalendar();
        cal.setTimeZone(startTimeInstant.getTimeZone());
        cal.setTimeInMillis(startTimeInstant.getTimeInMillis());
        long from = cal.getTimeInMillis();

        addField(cal, Calendar.YEAR, years, 1);
        addField(cal, Calendar.MONTH, months, 1);
        long millisOfYearsAndMonths = cal.getTimeInMillis() - from;

        BigInteger extraDays = BigInteger.valueOf(millisOfYearsAndMonths / 86400000L);
        BigInteger totalDays = days == null ? extraDays : days.add(extraDays);

        return new KajiDuration(
                sign >= 0, null, null, totalDays, hours, minutes, seconds);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The algorithm of the four reference instants; see the class header.
     */
    public int compare(Duration duration) {
        if (duration == null) {
            throw new NullPointerException("duration is null");
        }
        int accumulated = 0;
        boolean first = true;
        for (int i = 0; i < REFERENCES.length; i++) {
            int[] ref = REFERENCES[i];
            long onThis = instantPlus(ref, this);
            long onOther = instantPlus(ref, duration);
            int c;
            if (onThis < onOther) {
                c = DatatypeConstants.LESSER;
            } else if (onThis > onOther) {
                c = DatatypeConstants.GREATER;
            } else {
                c = DatatypeConstants.EQUAL;
            }
            if (first) {
                accumulated = c;
                first = false;
            } else if (c != accumulated) {
                return DatatypeConstants.INDETERMINATE;
            }
        }
        return accumulated;
    }

    /** The instant resulting from adding {@code d} to the reference date, in milliseconds. */
    private static long instantPlus(int[] ref, Duration d) {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.clear();
        // The months of REFERENCES go from 1 to 12 and `Calendar`'s from `JANUARY`, which is zero.
        cal.set(ref[0], Calendar.JANUARY + ref[1] - 1, ref[2], 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        d.addTo(cal);
        return cal.getTimeInMillis();
    }

    /**
     * {@inheritDoc}
     *
     * <p>It hashes the instant resulting from adding the duration to the first of the four
     * reference instants. It is the only thing coherent with {@link Duration#equals}, which is
     * defined by {@link #compare}: two equal durations give the same instant in <b>all four</b>
     * --by definition of the comparison-- so in particular they give the same one in that one.
     *
     * <p>Two durations that are not equal can coincide at that instant and collide, which is
     * something a hash may do. What cannot happen is the opposite, which is what matters.
     */
    public int hashCode() {
        long instant = instantPlus(REFERENCES[0], this);
        return (int) (instant ^ (instant >>> 32));
    }
}
