package javax.xml.datatype;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.datatype.DatatypeConstants -- the constants of the XML Schema date and
 * duration types: the names of the types, the results of comparing, the months, and the value that
 * means "this field is not there".
 *
 * <p>It is a constants-only class --private constructor, nothing to instantiate-- but three of its
 * groups say things that are not obvious and are worth reading before using them.
 *
 * <h2>{@link #INDETERMINATE}, which is the surprising one</h2>
 *
 * <p>Comparing two durations does not always give a result. {@link Duration#compare} returns {@link
 * #LESSER}, {@link #EQUAL}, {@link #GREATER} <b>or {@link #INDETERMINATE}</b>, and that fourth case
 * is not an error nor a "could not": it is the right answer.
 *
 * <p>The reason is that a month has no fixed number of days. {@code P1M} lasts 28, 29, 30 or 31
 * days depending on when it starts, so against {@code P30D} there is no order: in February {@code
 * P1M} is shorter, in March it is longer. Saying {@code LESSER} or {@code GREATER} would be
 * inventing; saying {@code EQUAL} too. The only honest answer is that they cannot be ordered.
 *
 * <p>The practical consequence hits hard: {@code Duration} does <b>not</b> implement {@code
 * Comparable}, and cannot, because a total order does not exist. And {@link Duration#isLongerThan}
 * returning false does not mean "it is shorter or equal": it can mean that it is not known.
 *
 * <h2>{@link #MAX_TIMEZONE_OFFSET} is the minimum and {@link #MIN_TIMEZONE_OFFSET} the maximum</h2>
 *
 * <p>It is not a misreading: {@code MAX_TIMEZONE_OFFSET} is -840 and {@code MIN_TIMEZONE_OFFSET} is
 * 840. The names are the other way round from the numbers.
 *
 * <p>What can be stated without guessing why, because it is checked against JDK 25: the time zone
 * field counts minutes <b>as the zone is written</b> --{@code -03:00} gives -180 and {@code +05:30}
 * gives 330-- and {@link XMLGregorianCalendar#setTimezone} accepts exactly the interval from -840
 * to 840, rejecting 841 and -841 with {@link IllegalArgumentException}. So the <b>maximum</b> value
 * of the field is the one kept by the constant called {@code MIN_}, and the <b>minimum</b> the one
 * of the constant called {@code MAX_}.
 *
 * <p>They are replicated with those names and those values because they are public API and there is
 * code that uses them by name; swapping them would "fix" the reading and break whoever compares
 * them.
 *
 * <h2>{@link #FIELD_UNDEFINED} instead of null</h2>
 *
 * <p>The fields of {@link XMLGregorianCalendar} are {@code int}s, and an {@code int} cannot be
 * null. A {@code gMonth} --the type of "May, of any year"-- has a month and no year, so a value
 * meaning absent is needed. It is {@code Integer.MIN_VALUE}, chosen because it is not a possible
 * year, month, day nor hour. Whoever reads {@code getYear()} has to compare it against this
 * constant before using it: there is no exception to warn.
 *
 * <h2>What is here</h2>
 *
 * <p>The thirty-six public members, with the same values as the original --checked one by one
 * against JDK 25-- and the nested class {@link Field}, which is the type token with which {@link
 * Duration#getField} and {@link Duration#isSet} name a field without using strings.
 */
public final class DatatypeConstants {

    /** There is nothing to instantiate: they are all constants. */
    private DatatypeConstants() {
    }

    /** January, counted from one --unlike {@code java.util.Calendar}, which counts from zero--. */
    public static final int JANUARY = 1;

    /** Febrero. */
    public static final int FEBRUARY = 2;

    /** Marzo. */
    public static final int MARCH = 3;

    /** Abril. */
    public static final int APRIL = 4;

    /** Mayo. */
    public static final int MAY = 5;

    /** Junio. */
    public static final int JUNE = 6;

    /** Julio. */
    public static final int JULY = 7;

    /** Agosto. */
    public static final int AUGUST = 8;

    /** Septiembre. */
    public static final int SEPTEMBER = 9;

    /** Octubre. */
    public static final int OCTOBER = 10;

    /** Noviembre. */
    public static final int NOVEMBER = 11;

    /** December, which is twelve and not eleven. */
    public static final int DECEMBER = 12;

    /** The first is less than the second. */
    public static final int LESSER = -1;

    /** Both are the same value. */
    public static final int EQUAL = 0;

    /** The first is greater than the second. */
    public static final int GREATER = 1;

    /**
     * They cannot be ordered, and that is the answer and not a failure.
     *
     * <p>See the class header: it happens when the comparison depends on data that is not there
     * --which month it is, or in which time zone-- and any order chosen would be invented.
     */
    public static final int INDETERMINATE = 2;

    /**
     * The field is not set.
     *
     * <p>{@code Integer.MIN_VALUE}, written as a literal because that is how it appears in
     * the original.
     */
    public static final int FIELD_UNDEFINED = Integer.MIN_VALUE;

    /** The years field of a {@link Duration}. */
    public static final Field YEARS = new Field("YEARS", 0);

    /** The months field. */
    public static final Field MONTHS = new Field("MONTHS", 1);

    /** The days field. */
    public static final Field DAYS = new Field("DAYS", 2);

    /** The hours field. */
    public static final Field HOURS = new Field("HOURS", 3);

    /** The minutes field. */
    public static final Field MINUTES = new Field("MINUTES", 4);

    /**
     * The seconds field, which is the only fractional one.
     *
     * <p>{@link Duration#getField} returns it as a {@link java.math.BigDecimal} and not as a
     * {@link java.math.BigInteger}, because {@code PT0.5S} is a valid duration.
     */
    public static final Field SECONDS = new Field("SECONDS", 5);

    /** The qualified name of the {@code xs:dateTime} type. */
    public static final QName DATETIME =
            new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "dateTime");

    /** The qualified name of the {@code xs:time} type. */
    public static final QName TIME = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "time");

    /** The qualified name of the {@code xs:date} type. */
    public static final QName DATE = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "date");

    /** The qualified name of the {@code xs:gYearMonth} type: a month of a year, without a day. */
    public static final QName GYEARMONTH =
            new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "gYearMonth");

    /** The qualified name of the {@code xs:gMonthDay} type: a day of the year, without a year. */
    public static final QName GMONTHDAY =
            new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "gMonthDay");

    /** The qualified name of the {@code xs:gYear} type. */
    public static final QName GYEAR = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "gYear");

    /** The qualified name of the {@code xs:gMonth} type. */
    public static final QName GMONTH = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "gMonth");

    /** The qualified name of the {@code xs:gDay} type. */
    public static final QName GDAY = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "gDay");

    /** The qualified name of the {@code xs:duration} type, the one that can have the six fields. */
    public static final QName DURATION =
            new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "duration");

    /**
     * The qualified name of {@code xdt:dayTimeDuration}: days, hours, minutes and seconds.
     *
     * <p>It is from XPath 2.0 and not from XML Schema, hence the different namespace. It exists
     * precisely because of {@link #INDETERMINATE}: a duration without months <b>can</b> be ordered,
     * because a day always lasts the same.
     */
    public static final QName DURATION_DAYTIME =
            new QName(XMLConstants.W3C_XPATH_DATATYPE_NS_URI, "dayTimeDuration");

    /**
     * The qualified name of {@code xdt:yearMonthDuration}: years and months.
     *
     * <p>The other orderable half: counted in months, it has no ambiguity either.
     */
    public static final QName DURATION_YEARMONTH =
            new QName(XMLConstants.W3C_XPATH_DATATYPE_NS_URI, "yearMonthDuration");

    /**
     * The <b>lower</b> end of the time zone field, in minutes: -840, that is {@code -14:00}.
     *
     * <p>That the constant called "MAX" keeps the minimum is explained in the class header; the
     * name is the original's and is not touched.
     */
    public static final int MAX_TIMEZONE_OFFSET = -14 * 60;

    /** The <b>upper</b> end, in minutes: 840, that is {@code +14:00}. */
    public static final int MIN_TIMEZONE_OFFSET = 14 * 60;

    /**
     * One of the six fields of a {@link Duration}, as an object.
     *
     * <p>It exists so that {@link Duration#getField} and {@link Duration#isSet} take a field
     * without the caller passing a string that can be misspelt. The six possible instances are the
     * constants above and there is no way of creating others --the constructor is private--, so
     * comparing with {@code ==} is correct and it is what implementations do.
     *
     * <p>It predates {@code enum} in the language; with {@code enum} it would not be written this
     * way today, but changing it would break the comparison by identity of all the code that
     * already exists. (The note also said it would break serialization; the class is not {@code
     * Serializable}, here nor in the JDK.)
     */
    public static final class Field {

        /** The name, which is the only thing visible from outside. */
        private final String str;

        /** The index, from 0 to 5, in the order the fields go in the lexical representation. */
        private final int id;

        /**
         * Only from in here: the six instances are the constants of the enclosing class.
         *
         * @param str the name
         * @param id the index
         */
        private Field(String str, int id) {
            this.str = str;
            this.id = id;
        }

        /**
         * The name of the field, in upper case, the same as the constant's.
         *
         * @return for example {@code "YEARS"}
         */
        public String toString() {
            return str;
        }

        /**
         * The index of the field, from {@code YEARS} = 0 to {@code SECONDS} = 5.
         *
         * <p>The order is that of the lexical representation {@code PnYnMnDTnHnMnS}, which is what
         * makes it useful: it serves as an index into an array of fields without having to
         * translate anything.
         *
         * @return from 0 to 5
         */
        public int getId() {
            return id;
        }
    }
}
