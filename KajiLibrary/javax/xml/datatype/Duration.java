package javax.xml.datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.datatype.Duration -- an amount of time written as {@code PnYnMnDTnHnMnS},
 * with the peculiarity that two of them cannot always be compared.
 *
 * <h2>Why it does not implement {@code Comparable}</h2>
 *
 * <p>It is the design decision that explains almost the whole class. A duration has six fields, and
 * two of them --years and months-- measure in a unit that <b>has no fixed length</b>. {@code P1M}
 * is 28 days if it starts on 1 February 2023 and 31 if it starts on 1 March. So the question "is
 * {@code P1M} longer than {@code P30D}?" has no answer: it has two, depending on when.
 *
 * <p>That is why {@link #compare} returns four values and not three, and the fourth, {@link
 * DatatypeConstants#INDETERMINATE}, is a right answer and not an error. And that is why this class
 * cannot implement {@code Comparable}: there is no total order to implement.
 *
 * <p>The trap that claims its victims: <b>{@code !isLongerThan(d)} does not mean "it is shorter or
 * equal"</b>. It means "it is not longer", which includes "unknown". Code that sorts durations with
 * it gives different results depending on the order they arrive in.
 *
 * <p>And one more, equally surprising: {@link #equals} is <b>not</b> a field-by-field comparison
 * either. It is defined as {@code compare(other) == EQUAL}, so {@code P1M} and {@code P30D} are not
 * equal --they give {@code INDETERMINATE}--, but {@code PT60S} and {@code PT1M} are.
 *
 * <h2>The sign is outside the fields</h2>
 *
 * <p>A duration is a sign plus six magnitudes, and not six signed numbers. {@link #getField} always
 * returns non-negative values and {@link #getSign} returns -1, 0 or 1 separately. It is what XML
 * Schema says --{@code -P1Y2M} is "minus (one year and two months)", not "minus one year, plus two
 * months"-- and it makes {@code P1Y2M} and {@code -P1Y2M} have the same fields.
 *
 * <h2>Fields may be absent</h2>
 *
 * <p>{@code P1Y} is not the same as {@code P1Y0M0DT0H0M0S}: the first has a single field set and
 * the rest absent. {@link #getField} returns null for an absent field and {@link #isSet} says so
 * unambiguously; {@link #getYears} and its siblings, which return {@code int}, answer {@link
 * DatatypeConstants#FIELD_UNDEFINED} in that case. Which fields are set is also what decides {@link
 * #getXMLSchemaType}.
 *
 * <h2>What is here</h2>
 *
 * <p>The whole class, with the same division as the original between the abstract and the concrete:
 * the ten abstract methods are the ones that depend on how the fields are stored, and the rest are
 * written <b>in terms of those ten</b> --{@code subtract} is {@code add(rhs.negate())}, {@code
 * multiply(int)} is {@code multiply(BigDecimal)}, {@code equals} is {@code compare}--, so a
 * subclass that implements the ten gets the other sixteen really working. (The note said fourteen.)
 *
 * <p>This library comes with that subclass: {@link DatatypeFactory#newInstance()} returns a factory
 * that produces real durations, with parsing of the lexical form, arithmetic and comparison. No XML
 * parser is needed for that --a duration is a string of numbers and letters, not a document-- so
 * nothing is cut down here.
 */
public abstract class Duration {

    /**
     * For the subclasses.
     *
     * <p>Public as in the original, even though the class is abstract: there is code that extends
     * it from another package.
     */
    public Duration() {
    }

    /**
     * Which of the three XML duration types this is, according to which fields it has set.
     *
     * <p>The three legal forms, and there are no more:
     *
     * <ul>
     *   <li>the six fields set: {@link DatatypeConstants#DURATION}, XML Schema's {@code
     *     xs:duration};
     *   <li>days, hours, minutes and seconds, without years or months:
     *       {@link DatatypeConstants#DURATION_DAYTIME};
     *   <li>years and months, with nothing else: {@link DatatypeConstants#DURATION_YEARMONTH}.
     * </ul>
     *
     * <p>The last two are from XPath 2.0 and exist precisely because they <b>can</b> be ordered:
     * without months everything is counted in seconds, and without days everything is counted in
     * months. It is {@link DatatypeConstants#DURATION} that can give {@link
     * DatatypeConstants#INDETERMINATE}.
     *
     * <p>Any other combination --{@code P1Y1D}, for example, with years and days but without
     * months-- is none of the three types and throws. It is a buildable value but without a name in
     * the XML type system.
     *
     * @return one of the three qualified names
     * @throws IllegalStateException if the fields set do not make any of the three
     */
    public QName getXMLSchemaType() {
        boolean hasYears = isSet(DatatypeConstants.YEARS);
        boolean hasMonths = isSet(DatatypeConstants.MONTHS);
        boolean hasDays = isSet(DatatypeConstants.DAYS);
        boolean hasHours = isSet(DatatypeConstants.HOURS);
        boolean hasMinutes = isSet(DatatypeConstants.MINUTES);
        boolean hasSeconds = isSet(DatatypeConstants.SECONDS);

        if (hasYears && hasMonths && hasDays && hasHours && hasMinutes && hasSeconds) {
            return DatatypeConstants.DURATION;
        }
        if (!hasYears && !hasMonths && hasDays && hasHours && hasMinutes && hasSeconds) {
            return DatatypeConstants.DURATION_DAYTIME;
        }
        if (hasYears && hasMonths && !hasDays && !hasHours && !hasMinutes && !hasSeconds) {
            return DatatypeConstants.DURATION_YEARMONTH;
        }
        throw new IllegalStateException(
                "javax.xml.datatype.Duration#getXMLSchemaType():"
                        + " this Duration does not match one of the XML Schema date/time datatypes:"
                        + " year set = " + hasYears
                        + " month set = " + hasMonths
                        + " day set = " + hasDays
                        + " hour set = " + hasHours
                        + " minute set = " + hasMinutes
                        + " second set = " + hasSeconds);
    }

    /**
     * The sign of the whole duration: -1, 0 or 1.
     *
     * <p>Zero only when all the fields set are zero.
     *
     * @return -1, 0 or 1
     */
    public abstract int getSign();

    /**
     * The years, always non-negative.
     *
     * @return the years, or {@link DatatypeConstants#FIELD_UNDEFINED} if the field is not there
     */
    public int getYears() {
        return valueAsInt(DatatypeConstants.YEARS);
    }

    /**
     * The months, always non-negative.
     *
     * @return the months, or {@link DatatypeConstants#FIELD_UNDEFINED} if the field is not there
     */
    public int getMonths() {
        return valueAsInt(DatatypeConstants.MONTHS);
    }

    /**
     * The days, always non-negative.
     *
     * @return the days, or {@link DatatypeConstants#FIELD_UNDEFINED} if the field is not there
     */
    public int getDays() {
        return valueAsInt(DatatypeConstants.DAYS);
    }

    /**
     * The hours, always non-negative.
     *
     * @return the hours, or {@link DatatypeConstants#FIELD_UNDEFINED} if the field is not there
     */
    public int getHours() {
        return valueAsInt(DatatypeConstants.HOURS);
    }

    /**
     * The minutes, always non-negative.
     *
     * @return the minutes, or {@link DatatypeConstants#FIELD_UNDEFINED} if the field is not there
     */
    public int getMinutes() {
        return valueAsInt(DatatypeConstants.MINUTES);
    }

    /**
     * The <b>whole</b> seconds: the fractional part is lost here.
     *
     * <p>{@code PT1.5S} gives 1. To keep it, one has to ask for {@code
     * getField(DatatypeConstants.SECONDS)}, which returns the complete {@link BigDecimal}.
     *
     * @return the seconds, or {@link DatatypeConstants#FIELD_UNDEFINED} if the field is not there
     */
    public int getSeconds() {
        return valueAsInt(DatatypeConstants.SECONDS);
    }

    /**
     * A field as an {@code int}, with {@link DatatypeConstants#FIELD_UNDEFINED} for the absent one.
     *
     * @param fieldId which
     * @return the integer value
     */
    private int valueAsInt(javax.xml.datatype.DatatypeConstants.Field fieldId) {
        Number n = getField(fieldId);
        if (n == null) {
            return DatatypeConstants.FIELD_UNDEFINED;
        }
        return n.intValue();
    }

    /**
     * How many milliseconds this duration lasts <b>if it starts at this instant</b>.
     *
     * <p>That a starting instant is needed is the whole story of this class in one signature:
     * without it, the question has no answer. The calculation is literal --the calendar is copied,
     * the duration is added to it, and the two instants are subtracted--, so the result also comes
     * out right when there is a daylight saving change in between.
     *
     * <p>The calendar passed is <b>not</b> touched: the work is done on a copy.
     *
     * <p>The copy is built with the original's instant and time zone and not with {@code clone()},
     * which is what the JDK does. The difference shows in a single case --a {@code Calendar} of
     * another subclass that is not Gregorian, whose type the copy loses--; for everything this API
     * models, which is the Gregorian calendar, the result is the same. (The note gave as the reason
     * that this library's {@link Calendar} is not {@code Cloneable} yet; it is now.)
     *
     * @param startInstant the starting instant; its time zone and calendar are also used
     * @return the milliseconds, signed
     * @throws NullPointerException if {@code startInstant} is null
     */
    public long getTimeInMillis(Calendar startInstant) {
        Calendar copy = new GregorianCalendar();
        copy.setTimeZone(startInstant.getTimeZone());
        copy.setTimeInMillis(startInstant.getTimeInMillis());
        addTo(copy);
        return copy.getTimeInMillis() - startInstant.getTimeInMillis();
    }

    /**
     * The same, starting from a {@link Date}.
     *
     * <p>It uses a {@link GregorianCalendar} with the default time zone, because a {@code Date}
     * brings none. If that matters --and with months involved it does-- the version that takes a
     * {@link Calendar} is preferable.
     *
     * @param startInstant the starting instant
     * @return the milliseconds, signed
     * @throws NullPointerException if {@code startInstant} is null
     */
    public long getTimeInMillis(Date startInstant) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(startInstant);
        addTo(cal);
        return cal.getTimeInMillis() - startInstant.getTime();
    }

    /**
     * The value of a field, or null if the field is not set.
     *
     * <p>The type of the result depends on the field: {@link BigInteger} for the first five and
     * {@link BigDecimal} for {@link DatatypeConstants#SECONDS}, which is the only fractional one.
     * Always non-negative: the sign is in {@link #getSign}.
     *
     * @param field which field
     * @return the value, or null
     * @throws NullPointerException if {@code field} is null
     */
    public abstract Number getField(javax.xml.datatype.DatatypeConstants.Field field);

    /**
     * Whether the field is set.
     *
     * <p>Different from "is zero": {@code P0Y} has the years set to zero, and {@code P1D} has them
     * absent.
     *
     * @param field which field
     * @return true if it is set
     * @throws NullPointerException if {@code field} is null
     */
    public abstract boolean isSet(javax.xml.datatype.DatatypeConstants.Field field);

    /**
     * The sum of the two durations.
     *
     * <p>It does not always exist, and the reason is the usual one: adding {@code P1M} and {@code
     * -P30D} would give a duration whose sign depends on the month, and there is no way of writing
     * that. In that case it throws.
     *
     * @param rhs the other; cannot be null
     * @return the sum
     * @throws IllegalStateException if the result would have fields of both signs
     * @throws NullPointerException if {@code rhs} is null
     */
    public abstract Duration add(Duration rhs);

    /**
     * Adds this duration to the calendar, in place.
     *
     * <p>The order matters and is fixed by the specification: first years, then months, days,
     * hours, minutes and seconds. Adding a month and then a day does not give the same as the other
     * way round when starting on 31 January, so a fixed order is the only thing that makes the
     * operation reproducible.
     *
     * @param calendar the calendar to modify; cannot be null
     * @throws NullPointerException if {@code calendar} is null
     */
    public abstract void addTo(Calendar calendar);

    /**
     * Adds this duration to the date, in place.
     *
     * <p>It goes through a {@link GregorianCalendar} with the default time zone, with the same
     * caveat as {@link #getTimeInMillis(Date)}.
     *
     * @param date the date to modify; cannot be null
     * @throws NullPointerException if {@code date} is null
     */
    public void addTo(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        addTo(cal);
        date.setTime(cal.getTimeInMillis());
    }

    /**
     * The subtraction, which is {@code add(rhs.negate())} and nothing more.
     *
     * @param rhs the one subtracted; cannot be null
     * @return the difference
     * @throws IllegalStateException if the equivalent sum cannot be represented
     * @throws NullPointerException if {@code rhs} is null
     */
    public Duration subtract(Duration rhs) {
        return add(rhs.negate());
    }

    /**
     * The duration multiplied by an integer.
     *
     * @param factor by how much
     * @return the product
     */
    public Duration multiply(int factor) {
        return multiply(BigDecimal.valueOf(factor));
    }

    /**
     * The duration multiplied by a decimal.
     *
     * <p>The fractional part left in a non-fractional field <b>goes down to the next field</b>:
     * half a year is six months, half a day is twelve hours. With one exception that surprises and
     * that is the usual one: from months one cannot go down to days, because there is no fixed
     * equivalence. Multiplying {@code P1M} by {@code 0.5} throws.
     *
     * @param factor by how much; cannot be null
     * @return the product
     * @throws IllegalStateException if a fraction of a month would be left
     * @throws NullPointerException if {@code factor} is null
     */
    public abstract Duration multiply(BigDecimal factor);

    /**
     * The same duration with the sign changed; the fields are not touched.
     *
     * @return the opposite
     */
    public abstract Duration negate();

    /**
     * The same duration with the years and months converted to days, using this reference calendar.
     *
     * <p>It is the operation that <b>removes</b> the ambiguity: once the starting point is fixed, a
     * month has a number of days, so the result is a duration without months --and therefore
     * comparable with any other like it--.
     *
     * @param startTimeInstant the reference point; cannot be null
     * @return the normalized duration
     * @throws NullPointerException if {@code startTimeInstant} is null
     */
    public abstract Duration normalizeWith(Calendar startTimeInstant);

    /**
     * Compares the two durations, and may answer that they cannot be compared.
     *
     * <p>The four results are {@link DatatypeConstants#LESSER}, {@link DatatypeConstants#EQUAL},
     * {@link DatatypeConstants#GREATER} and {@link DatatypeConstants#INDETERMINATE}. The last is
     * not an error: see the class header.
     *
     * <p>The specification's definition is indirect and worth knowing, because it explains why some
     * cases are indeterminate and others are not: both durations are added to four chosen instants
     * --1696-09-01, 1697-02-01, 1903-03-01 and 1903-07-01, which cover all the combinations of
     * February length and months of 30 and 31 days-- and, if the four comparisons agree, that is
     * the result; if not, it is indeterminate.
     *
     * @param duration the other; cannot be null
     * @return one of the four
     * @throws NullPointerException if {@code duration} is null
     */
    public abstract int compare(Duration duration);

    /**
     * Whether this duration is strictly longer than the other.
     *
     * <p><b>Careful</b>: false does not mean "it is shorter or equal". With
     * {@link DatatypeConstants#INDETERMINATE} both directions give false at the same time.
     *
     * @param duration the other; cannot be null
     * @return true only if the comparison gave {@link DatatypeConstants#GREATER}
     * @throws NullPointerException if {@code duration} is null
     */
    public boolean isLongerThan(Duration duration) {
        return compare(duration) == DatatypeConstants.GREATER;
    }

    /**
     * Whether this duration is strictly shorter than the other, with the same caveat.
     *
     * @param duration the other; cannot be null
     * @return true only if the comparison gave {@link DatatypeConstants#LESSER}
     * @throws NullPointerException if {@code duration} is null
     */
    public boolean isShorterThan(Duration duration) {
        return compare(duration) == DatatypeConstants.LESSER;
    }

    /**
     * Two durations are equal if {@link #compare} says {@link DatatypeConstants#EQUAL}.
     *
     * <p>It is not a field-by-field comparison, and the two consequences go in opposite directions:
     * {@code PT60S} and {@code PT1M} <b>are</b> equal although they have different fields, and
     * {@code P1M} and {@code P30D} are <b>not</b> because the comparison gives indeterminate.
     *
     * <p>What remains awkward, and is like this in the original: {@code equals} is not transitive
     * in the presence of indeterminate durations, so a {@code HashSet} of durations with months
     * does not behave as one expects. It is the price of the type having no total order.
     *
     * @param duration the other object
     * @return true if it is a {@code Duration} that compares equal
     */
    public boolean equals(Object duration) {
        if (duration == this) {
            return true;
        }
        if (!(duration instanceof Duration)) {
            return false;
        }
        return compare((Duration) duration) == DatatypeConstants.EQUAL;
    }

    /**
     * The hash, which the subclass has to give.
     *
     * <p>It is abstract precisely because {@link #equals} is defined in terms of {@link #compare}:
     * there is no formula over the fields that is coherent with that, and the subclass is the only
     * one that knows how to normalize before hashing.
     *
     * @return the hash
     */
    public abstract int hashCode();

    /**
     * The duration in XML Schema's lexical form: {@code PnYnMnDTnHnMnS}.
     *
     * <p>Only the fields that are set come out, the {@code T} appears only if there is some time
     * field, and the sign goes before the {@code P}. The seconds are written with the scale they
     * carry --{@code PT1.50S} stays {@code PT1.50S}, as in JDK 25-- and without scientific
     * notation. (The note said without extra zeros.)
     *
     * @return the lexical representation
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (getSign() < 0) {
            buf.append('-');
        }
        buf.append('P');

        BigInteger years = (BigInteger) getField(DatatypeConstants.YEARS);
        if (years != null) {
            buf.append(years).append('Y');
        }
        BigInteger months = (BigInteger) getField(DatatypeConstants.MONTHS);
        if (months != null) {
            buf.append(months).append('M');
        }
        BigInteger days = (BigInteger) getField(DatatypeConstants.DAYS);
        if (days != null) {
            buf.append(days).append('D');
        }

        BigInteger hours = (BigInteger) getField(DatatypeConstants.HOURS);
        BigInteger minutes = (BigInteger) getField(DatatypeConstants.MINUTES);
        BigDecimal seconds = (BigDecimal) getField(DatatypeConstants.SECONDS);
        if (hours != null || minutes != null || seconds != null) {
            buf.append('T');
            if (hours != null) {
                buf.append(hours).append('H');
            }
            if (minutes != null) {
                buf.append(minutes).append('M');
            }
            if (seconds != null) {
                buf.append(asText(seconds)).append('S');
            }
        }
        return buf.toString();
    }

    /**
     * A non-negative {@link BigDecimal} written without scientific notation.
     *
     * <p>It has to be written by hand because {@code BigDecimal}'s {@code toString()} can produce
     * an exponent --{@code 1E+2}-- and that is not a valid XML Schema lexical form. It is built by
     * inserting the point into the unscaled value, which is the very definition of the scale.
     *
     * @param bd the number, which here always comes non-negative
     * @return the text
     */
    private String asText(BigDecimal bd) {
        String ints = bd.unscaledValue().toString();
        int scaleFactor = bd.scale();
        if (scaleFactor == 0) {
            return ints;
        }
        int cut = ints.length() - scaleFactor;
        if (cut == 0) {
            return "0." + ints;
        }
        if (cut > 0) {
            StringBuilder buf = new StringBuilder(ints);
            buf.insert(cut, '.');
            return buf.toString();
        }
        StringBuilder buf = new StringBuilder();
        buf.append("0.");
        for (int i = 0; i < -cut; i++) {
            buf.append('0');
        }
        buf.append(ints);
        return buf.toString();
    }
}
