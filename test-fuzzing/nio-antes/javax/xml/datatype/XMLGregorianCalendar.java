package javax.xml.datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.datatype.XMLGregorianCalendar -- an XML Schema date and time, where the
 * characteristic thing is that <b>almost any field can be missing</b>.
 *
 * <h2>Eight types in one class</h2>
 *
 * <p>XML Schema has not one date type but eight, and the difference between them is exactly which
 * fields they carry: {@code xs:date} has no time, {@code xs:time} has no date, {@code xs:gMonth} is
 * "May" without year or day, {@code xs:gMonthDay} is "25 May" of any year. They all fit here, and
 * {@link #getXMLSchemaType} answers which it is by looking at which fields are set.
 *
 * <p>The fields are {@code int}s, so absence is marked with {@link
 * DatatypeConstants#FIELD_UNDEFINED} and not with null. It is the number one trap of the class:
 * {@code getYear()} returns {@code Integer.MIN_VALUE} for a {@code gMonth} and raises no exception,
 * so whoever does not compare against the constant carries that number into a calculation.
 *
 * <h2>The time zone can also be missing, and that ruins the order</h2>
 *
 * <p>A date without a time zone is not an instant: it is a date "somewhere". Comparing {@code
 * 2024-05-25T12:00:00} (without zone) with {@code 2024-05-25T14:00:00Z} has no answer, because the
 * first can fall before or after depending on where it is read. That is why {@link #compare} can
 * also return {@link DatatypeConstants#INDETERMINATE}, and why this class, like {@link Duration},
 * does <b>not</b> implement {@code Comparable}.
 *
 * <p>{@link #normalize} is the way out when there is a zone: it takes everything to UTC and there
 * it can be compared.
 *
 * <h2>The year does not fit in an {@code int}</h2>
 *
 * <p>XML Schema puts no cap on the year, so there are two accessors. {@link #getYear} returns an
 * {@code int} and is enough for everything that ever existed; {@link #getEon} returns the billions
 * left over, and {@link #getEonAndYear} puts them together in a {@link BigInteger}. For normal
 * dates {@code getEon()} is null and only {@code getYear()} is needed.
 *
 * <p>And year zero: in XML Schema 1.0 it <b>does not exist</b> --it goes from -1 to 1--, and this
 * API allows it anyway because 1.1 added it. {@link #isValid} is the one that decides in each case.
 *
 * <h2>What is here</h2>
 *
 * <p>The whole class, with the same division as the original: the abstract part is what depends on
 * how the fields are stored, and the concrete part --the three {@code setTime}s, {@code
 * getMillisecond}, {@code equals}, {@code hashCode}, {@code toString}-- is written in terms of the
 * abstract one and works for any subclass.
 *
 * <p>This library's {@link DatatypeFactory#newInstance()} returns a factory that produces real
 * instances of this class: parsing of the eight lexical forms, comparison with normalization,
 * adding durations and conversion to {@link GregorianCalendar}. None of that needs an XML parser.
 */
public abstract class XMLGregorianCalendar implements Cloneable {

    /**
     * For the subclasses.
     *
     * <p>Public as in the original, even though the class is abstract.
     */
    public XMLGregorianCalendar() {
    }

    /**
     * Sets all the fields to {@link DatatypeConstants#FIELD_UNDEFINED}.
     *
     * <p>Different from {@link #reset}: this empties, that goes back to how it was when created.
     */
    public abstract void clear();

    /**
     * Goes back to the values it had when just built.
     *
     * <p>It exists to reuse the instance in a loop without asking the factory for it again, which
     * is the kind of optimization that makes sense when large documents are processed.
     */
    public abstract void reset();

    /**
     * The year, unbounded.
     *
     * <p>Null leaves the year undefined. A value that fits in an {@code int} is kept in the small
     * field with {@code eon} at null; a larger one is split between the two.
     *
     * @param year the year, or null to clear it
     */
    public abstract void setYear(BigInteger year);

    /**
     * The year, in the range of an {@code int}.
     *
     * @param year the year, or {@link DatatypeConstants#FIELD_UNDEFINED} to clear it
     */
    public abstract void setYear(int year);

    /**
     * The month, from {@link DatatypeConstants#JANUARY} to {@link DatatypeConstants#DECEMBER}.
     *
     * <p>Counted from <b>one</b>. {@code java.util.Calendar} counts it from zero, and that
     * difference of one between the two APIs is a classic source of mistakes.
     *
     * @param month from 1 to 12, or {@link DatatypeConstants#FIELD_UNDEFINED}
     * @throws IllegalArgumentException if it is out of range
     */
    public abstract void setMonth(int month);

    /**
     * The day of the month, from 1 to 31.
     *
     * @param day from 1 to 31, or {@link DatatypeConstants#FIELD_UNDEFINED}
     * @throws IllegalArgumentException if it is out of range
     */
    public abstract void setDay(int day);

    /**
     * The time zone, in minutes and with the same sign it is written with.
     *
     * <p>{@code -03:00} is -180 and {@code +05:30} is 330. To go to UTC it has to be
     * <b>subtracted</b> from the local time.
     *
     * <p>The admitted range is from -840 to 840 inclusive, that is from {@code -14:00} to {@code
     * +14:00}. Watch the names of the constants, which are the other way round from the numbers:
     * {@link DatatypeConstants#MAX_TIMEZONE_OFFSET} keeps the minimum and {@link
     * DatatypeConstants#MIN_TIMEZONE_OFFSET} the maximum.
     *
     * @param offset from -840 to 840, or {@link DatatypeConstants#FIELD_UNDEFINED}
     * @throws IllegalArgumentException if it is out of range
     */
    public abstract void setTimezone(int offset);

    /**
     * Hour, minute and second at once.
     *
     * <p>A shortcut for the three setters; the fractional seconds stay as they were.
     *
     * @param hour the hour
     * @param minute the minute
     * @param second the second
     * @throws IllegalArgumentException if some is out of range
     */
    public void setTime(int hour, int minute, int second) {
        setHour(hour);
        setMinute(minute);
        setSecond(second);
    }

    /**
     * The hour, from 0 to 23.
     *
     * <p>24 is accepted only in the lexical value {@code 24:00:00}, which the implementation
     * normalizes to the next day; not through this route.
     *
     * @param hour from 0 to 23, or {@link DatatypeConstants#FIELD_UNDEFINED}
     * @throws IllegalArgumentException if it is out of range
     */
    public abstract void setHour(int hour);

    /**
     * The minute, from 0 to 59.
     *
     * @param minute from 0 to 59, or {@link DatatypeConstants#FIELD_UNDEFINED}
     * @throws IllegalArgumentException if it is out of range
     */
    public abstract void setMinute(int minute);

    /**
     * The second, from 0 to 60.
     *
     * <p>Sixty, not fifty-nine: XML Schema leaves room for the leap second.
     *
     * @param second from 0 to 60, or {@link DatatypeConstants#FIELD_UNDEFINED}
     * @throws IllegalArgumentException if it is out of range
     */
    public abstract void setSecond(int second);

    /**
     * The milliseconds, which are the fractional part of the second with three decimals.
     *
     * @param millisecond from 0 to 999, or {@link DatatypeConstants#FIELD_UNDEFINED}
     * @throws IllegalArgumentException if it is out of range
     */
    public abstract void setMillisecond(int millisecond);

    /**
     * The fractional part of the second, with whatever precision.
     *
     * <p>It is the general form of {@link #setMillisecond}: XML Schema puts no limit on the
     * decimals, so a {@link BigDecimal} is the only thing that loses nothing.
     *
     * @param fractional from 0 inclusive to 1 exclusive, or null to clear it
     * @throws IllegalArgumentException if it is outside that range
     */
    public abstract void setFractionalSecond(BigDecimal fractional);

    /**
     * Hour, minute, second and fraction of a second.
     *
     * @param hour the hour
     * @param minute the minute
     * @param second the second
     * @param fractional the fraction, from 0 inclusive to 1 exclusive
     * @throws IllegalArgumentException if some is out of range
     */
    public void setTime(int hour, int minute, int second, BigDecimal fractional) {
        setHour(hour);
        setMinute(minute);
        setSecond(second);
        setFractionalSecond(fractional);
    }

    /**
     * Hour, minute, second and millisecond.
     *
     * @param hour the hour
     * @param minute the minute
     * @param second the second
     * @param millisecond from 0 to 999
     * @throws IllegalArgumentException if some is out of range
     */
    public void setTime(int hour, int minute, int second, int millisecond) {
        setHour(hour);
        setMinute(minute);
        setSecond(second);
        setMillisecond(millisecond);
    }

    /**
     * The billions of the year, or null if the year fits in an {@code int}.
     *
     * <p>Always a multiple of a billion: the part that does not fit in {@link #getYear}.
     *
     * @return the eon, or null
     */
    public abstract BigInteger getEon();

    /**
     * The year, without the eon.
     *
     * @return the year, or {@link DatatypeConstants#FIELD_UNDEFINED} if it is not set
     */
    public abstract int getYear();

    /**
     * The complete year, eon included.
     *
     * @return the year, or null if it is not set
     */
    public abstract BigInteger getEonAndYear();

    /**
     * The month, counted from one.
     *
     * @return from 1 to 12, or {@link DatatypeConstants#FIELD_UNDEFINED}
     */
    public abstract int getMonth();

    /**
     * The day of the month.
     *
     * @return from 1 to 31, or {@link DatatypeConstants#FIELD_UNDEFINED}
     */
    public abstract int getDay();

    /**
     * The time zone in minutes, with the same sign it is written with; see {@link #setTimezone}.
     *
     * @return the minutes, or {@link DatatypeConstants#FIELD_UNDEFINED} if the date has no zone
     */
    public abstract int getTimezone();

    /**
     * The hour.
     *
     * @return from 0 to 23, or {@link DatatypeConstants#FIELD_UNDEFINED}
     */
    public abstract int getHour();

    /**
     * The minute.
     *
     * @return from 0 to 59, or {@link DatatypeConstants#FIELD_UNDEFINED}
     */
    public abstract int getMinute();

    /**
     * The whole second; the fraction is in {@link #getFractionalSecond}.
     *
     * @return from 0 to 60, or {@link DatatypeConstants#FIELD_UNDEFINED}
     */
    public abstract int getSecond();

    /**
     * The milliseconds, taken from the fraction of a second.
     *
     * <p>It moves the point three places and truncates: a fraction with more than three decimals
     * loses what is left over, which is what has to happen when whoever asks wants milliseconds.
     *
     * @return from 0 to 999, or {@link DatatypeConstants#FIELD_UNDEFINED} if there is no fraction
     */
    public int getMillisecond() {
        BigDecimal fractionValue = getFractionalSecond();
        if (fractionValue == null) {
            return DatatypeConstants.FIELD_UNDEFINED;
        }
        return fractionValue.movePointRight(3).intValue();
    }

    /**
     * The fractional part of the second, with all its precision.
     *
     * @return from 0 inclusive to 1 exclusive, or null if it is not set
     */
    public abstract BigDecimal getFractionalSecond();

    /**
     * Compares the two dates, and may answer that they cannot be compared.
     *
     * <p>The four results are {@link DatatypeConstants#LESSER}, {@link DatatypeConstants#EQUAL},
     * {@link DatatypeConstants#GREATER} and {@link DatatypeConstants#INDETERMINATE}. The last
     * appears when one of the two has a time zone and the other does not --and the one without
     * could fall on either side--, or when they are missing different fields.
     *
     * @param xmlGregorianCalendar the other; cannot be null
     * @return one of the four
     * @throws NullPointerException if it is null
     */
    public abstract int compare(XMLGregorianCalendar xmlGregorianCalendar);

    /**
     * The same date taken to UTC.
     *
     * <p>It is what makes two dates with different zones comparable. A date <b>without</b> a zone
     * is returned as is: there is nothing to normalize it to, and assuming UTC would be inventing
     * the missing datum.
     *
     * @return a new instance in UTC
     */
    public abstract XMLGregorianCalendar normalize();

    /**
     * Two dates are equal if {@link #compare} says {@link DatatypeConstants#EQUAL}.
     *
     * <p>So {@code 2024-05-25T12:00:00-03:00} and {@code 2024-05-25T15:00:00Z} <b>are</b> equal
     * even though they do not have a single field in common: they are the same instant. And two
     * dates that compare indeterminate are not equal.
     *
     * @param obj the other object
     * @return true if it is an {@code XMLGregorianCalendar} that compares equal
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof XMLGregorianCalendar)) {
            return false;
        }
        return compare((XMLGregorianCalendar) obj) == DatatypeConstants.EQUAL;
    }

    /**
     * The hash, computed over the date normalized to UTC.
     *
     * <p>The normalization is not a detail: without it {@code 12:00-03:00} and {@code 15:00Z}
     * --which are equal by {@link #equals}-- would give different hashes, and a {@code HashMap}
     * would lose one of the two. It normalizes only when there is a zone and it is not zero, so as
     * not to pay the cost needlessly.
     *
     * @return the hash
     */
    public int hashCode() {
        int timezoneValue = getTimezone();
        if (timezoneValue == DatatypeConstants.FIELD_UNDEFINED) {
            timezoneValue = 0;
        }
        XMLGregorianCalendar gc = this;
        if (timezoneValue != 0) {
            gc = normalize();
        }
        return gc.getYear() + gc.getMonth() + gc.getDay()
                + gc.getHour() + gc.getMinute() + gc.getSecond();
    }

    /**
     * The date in the XML Schema lexical form that corresponds to the fields it has.
     *
     * @return the text, for example {@code 2024-05-25T12:00:00-03:00}
     * @throws IllegalStateException if the fields set make none of the eight types
     */
    public abstract String toXMLFormat();

    /**
     * Which of the eight XML Schema date types this is, according to which fields it has.
     *
     * @return one of {@link DatatypeConstants#DATETIME}, {@link DatatypeConstants#DATE},
     *     {@link DatatypeConstants#TIME}, {@link DatatypeConstants#GYEARMONTH},
     *     {@link DatatypeConstants#GMONTHDAY}, {@link DatatypeConstants#GYEAR},
     *     {@link DatatypeConstants#GMONTH} or {@link DatatypeConstants#GDAY}
     * @throws IllegalStateException if the fields set make none
     */
    public abstract QName getXMLSchemaType();

    /**
     * The same as {@link #toXMLFormat}.
     *
     * @return the text
     * @throws IllegalStateException if the fields set make none of the eight types
     */
    public String toString() {
        return toXMLFormat();
    }

    /**
     * Whether the fields set make a date that exists.
     *
     * <p>It looks at what the setters cannot look at one by one: 31 February passes both range
     * checks separately and is not a date. It also decides about year zero, which XML Schema 1.0
     * does not admit.
     *
     * @return true if it is valid
     */
    public abstract boolean isValid();

    /**
     * Adds a duration to it, in place.
     *
     * <p>The order of the fields is fixed by the specification --years, months, days, hours,
     * minutes, seconds-- and there is an adjustment that surprises: if adding months leaves a day
     * that does not exist in the target month, the day is <b>clipped</b> to the last of the month.
     * 31 January plus one month is 28 February, not 3 March.
     *
     * @param duration the duration to add; cannot be null
     * @throws NullPointerException if it is null
     */
    public abstract void add(Duration duration);

    /**
     * The same date as a {@link GregorianCalendar}.
     *
     * <p>It is a lossy conversion and it has to be known: {@code GregorianCalendar} has no absent
     * fields, so the missing ones are filled in with those of the default epoch. A converted {@code
     * xs:time} brings a date nobody set.
     *
     * @return the equivalent calendar
     */
    public abstract GregorianCalendar toGregorianCalendar();

    /**
     * The same, choosing what to fill in the missing parts with.
     *
     * <p>It is the honest version of the previous one: {@code defaults} says explicitly which
     * values to use for the absent fields, instead of the implementation inventing them.
     *
     * @param timezone the zone to use if this date has none; can be null
     * @param aLocale the locale for the calendar; can be null
     * @param defaults where to take the missing fields from; can be null
     * @return the equivalent calendar
     */
    public abstract GregorianCalendar toGregorianCalendar(
            TimeZone timezone, Locale aLocale, XMLGregorianCalendar defaults);

    /**
     * The time zone of this date as a {@link TimeZone}.
     *
     * @param defaultZoneoffset what to use if this date has no zone; can be
     *     {@link DatatypeConstants#FIELD_UNDEFINED}
     * @return the zone, or null if there is none, not even by default
     */
    public abstract TimeZone getTimeZone(int defaultZoneoffset);

    /**
     * An independent copy.
     *
     * <p>It is abstract and does not inherit {@link Object}'s because the class is mutable: a
     * shallow copy would share the state and modifying one would change the other.
     *
     * @return the copy
     */
    public abstract Object clone();
}
