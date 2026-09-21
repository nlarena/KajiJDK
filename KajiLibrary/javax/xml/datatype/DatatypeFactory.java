package javax.xml.datatype;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;

/**
 * KajiLibrary's javax.xml.datatype.DatatypeFactory -- where {@link Duration}s and
 * {@link XMLGregorianCalendar}s come from.
 *
 * <p>It is a pluggable factory like the other JAXP ones: the application asks for an abstract type
 * and the implementation is discovered at run time. {@link #newInstance()} looks, in this order, at
 * the system property {@link #DATATYPEFACTORY_PROPERTY}, the file {@code
 * $java.home/conf/jaxp.properties}, the providers declared via {@link ServiceLoader}, and finally
 * the platform implementation.
 *
 * <h2>Here the last step exists</h2>
 *
 * <p>{@link javax.xml.transform.TransformerFactory} fails when it reaches the end, because what it
 * would have to return is an XSLT processor and this library comes with none. (The note also named
 * {@code XMLInputFactory}; since then {@code javax.xml.stream} has its own StAX implementation, and
 * {@code newDefaultFactory()} returns it.)
 *
 * <p>This one does not. A {@code Duration} is arithmetic over six numbers and an {@code
 * XMLGregorianCalendar} is a calendar with optional fields: <b>neither of the two needs to read a
 * document</b>. Their lexical form, {@code P1Y2M3DT4H5M6S} or {@code 2024-05-25T12:00:00-03:00}, is
 * a string of numbers and letters, not XML. So {@link #newDefaultInstance()} returns a real
 * implementation, and {@link #newInstance()} finds it: this library's durations and dates compute.
 *
 * <h2>The abstract and the concrete</h2>
 *
 * <p>The division is the same as the original's and it is worth understanding. The seven abstract
 * methods are the <b>general</b> ones: the ones that take {@link BigInteger} and {@link
 * BigDecimal}, which is the only thing that loses no precision. The fourteen concrete ones are
 * <b>shortcuts</b> written in terms of those: the one that takes {@code int} converts and
 * delegates, {@code newDurationDayTime(long)} builds the whole duration and keeps the day and time
 * fields, {@code newXMLGregorianCalendarDate} calls the general one with the time fields at {@link
 * DatatypeConstants#FIELD_UNDEFINED}. (The note said twelve.)
 *
 * <p>So an implementation that writes the seven gets the fourteen working, and --what matters
 * more-- the fourteen behave the same in any implementation, because they are written once and
 * here.
 */
public abstract class DatatypeFactory {

    /**
     * The system property another implementation is plugged in with:
     * {@code javax.xml.datatype.DatatypeFactory}.
     */
    public static final String DATATYPEFACTORY_PROPERTY = "javax.xml.datatype.DatatypeFactory";

    /**
     * The class name of the platform implementation.
     *
     * <p>It is Xerces's in the JDK; here it is ours. The constant exists because it is part of the
     * public API --there is code that compares it-- but it is an informative string and not an
     * extension point: changing it does not change what {@link #newDefaultInstance()} returns.
     */
    public static final String DATATYPEFACTORY_IMPLEMENTATION_CLASS =
            "javax.xml.datatype.KajiDatatypeFactory";

    /** For the subclasses; there is no state to initialize. */
    protected DatatypeFactory() {
    }

    // ---- discovery ------------------------------------------------------------------------

    /**
     * The platform implementation, without looking at the configuration.
     *
     * <p>It skips the steps of {@link #newInstance()} on purpose: it exists so that a piece that
     * needs the reference implementation --and not the one the application may have plugged in--
     * can ask for it.
     *
     * <p>Unlike the other factories of this library, here there is one and this method returns it.
     * See the class header.
     *
     * @return the platform implementation
     */
    public static DatatypeFactory newDefaultInstance() {
        return new KajiDatatypeFactory();
    }

    /**
     * The configured factory, looked for in the four steps of the header.
     *
     * @return the factory found; never null
     * @throws DatatypeConfigurationException if a step names a class that cannot be loaded
     */
    public static DatatypeFactory newInstance() throws DatatypeConfigurationException {
        // 1. The system property.
        String className = null;
        try {
            className = System.getProperty(DATATYPEFACTORY_PROPERTY);
        } catch (SecurityException ignored) {
            // Without permission to read it, it is the same as not being set.
        }
        if (className != null && className.length() > 0) {
            return instantiate(className, null);
        }

        // 2. $java.home/conf/jaxp.properties.
        className = fromJaxpProperties();
        if (className != null && className.length() > 0) {
            return instantiate(className, null);
        }

        // 3. The providers declared on the classpath.
        DatatypeFactory fromService = fromServiceLoader();
        if (fromService != null) {
            return fromService;
        }

        // 4. The platform implementation, which does exist here.
        return newDefaultInstance();
    }

    /**
     * A factory of a named class, without any discovery.
     *
     * @param factoryClassName the fully qualified name of the class; cannot be null
     * @param classLoader what to load it with; null uses the one that corresponds by default
     * @return the factory
     * @throws DatatypeConfigurationException if the class is not there or cannot be instantiated
     */
    public static DatatypeFactory newInstance(String factoryClassName, ClassLoader classLoader)
            throws DatatypeConfigurationException {
        if (factoryClassName == null) {
            throw new DatatypeConfigurationException(
                    "Provider for " + DATATYPEFACTORY_PROPERTY + " cannot be found");
        }
        return instantiate(factoryClassName, classLoader);
    }

    /**
     * Loads and instantiates the named class, with the two error messages the contract tells apart.
     *
     * <p>They are separate because they are fixed differently: **not found** is a missing jar,
     * **could not be instantiated** is a class that is there but is no good.
     */
    private static DatatypeFactory instantiate(String className, ClassLoader loader)
            throws DatatypeConfigurationException {
        Class<?> cls;
        try {
            if (loader == null) {
                cls = Class.forName(className);
            } else {
                cls = Class.forName(className, false, loader);
            }
        } catch (ClassNotFoundException e) {
            throw new DatatypeConfigurationException("Provider " + className + " not found", e);
        }
        Object obj;
        try {
            obj = cls.newInstance();
        } catch (Exception e) {
            throw new DatatypeConfigurationException(
                    "Provider " + className + " could not be instantiated: " + e, e);
        }
        if (!(obj instanceof DatatypeFactory)) {
            throw new DatatypeConfigurationException(
                    "Provider " + className + " could not be instantiated: "
                            + className + " cannot be cast to " + DATATYPEFACTORY_PROPERTY);
        }
        return (DatatypeFactory) obj;
    }

    /**
     * The class name {@code $java.home/conf/jaxp.properties} declares, or null.
     *
     * <p>Any read failure returns null instead of propagating: the file is optional, and not being
     * able to read it is the absence of configuration and not an error.
     */
    private static String fromJaxpProperties() {
        try {
            String home = System.getProperty("java.home");
            if (home == null) {
                return null;
            }
            File f = new File(new File(new File(home), "conf"), "jaxp.properties");
            if (!f.exists()) {
                return null;
            }
            Properties props = new Properties();
            InputStream in = new FileInputStream(f);
            try {
                props.load(in);
            } finally {
                in.close();
            }
            return props.getProperty(DATATYPEFACTORY_PROPERTY);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * The first factory a classpath provider declares, or null.
     *
     * <p>Today it always gives null, and not because of a shortcut here: this library's {@link
     * ServiceLoader} cannot enumerate {@code META-INF/services} because our {@code ClassLoader} has
     * no resources. The machinery is plugged in where it goes.
     */
    private static DatatypeFactory fromServiceLoader() {
        try {
            ServiceLoader<DatatypeFactory> sl = ServiceLoader.load(DatatypeFactory.class);
            Iterator<DatatypeFactory> it = sl.iterator();
            if (it.hasNext()) {
                return it.next();
            }
        } catch (Throwable ignored) {
            // A broken provider cannot stop the next step from being tried.
        }
        return null;
    }

    // ---- durations --------------------------------------------------------------------------

    /**
     * A duration from its lexical form {@code PnYnMnDTnHnMnS}.
     *
     * <p>Only the fields that are written appear: {@code P1Y} leaves the other five absent, which
     * is not the same as setting them to zero. See {@link Duration#isSet}.
     *
     * @param lexicalRepresentation the lexical form; cannot be null
     * @return the duration
     * @throws IllegalArgumentException if it is not a valid lexical form
     * @throws UnsupportedOperationException if the implementation does not support it
     */
    public abstract Duration newDuration(String lexicalRepresentation);

    /**
     * A duration of that many milliseconds, with the six fields set.
     *
     * <p>The years and months come from counting on the calendar from the epoch, which is the only
     * way of distributing milliseconds into fields of variable length.
     *
     * @param durationInMilliseconds the milliseconds, signed
     * @return the duration
     */
    public abstract Duration newDuration(long durationInMilliseconds);

    /**
     * A duration field by field; null in a field leaves it absent.
     *
     * <p>It is the general constructor and the one the others end up calling. The values have to be
     * non-negative: the sign goes separately, in {@code isPositive}.
     *
     * @param isPositive the sign
     * @param years the years, or null
     * @param months the months, or null
     * @param days the days, or null
     * @param hours the hours, or null
     * @param minutes the minutes, or null
     * @param seconds the seconds, with fraction, or null
     * @return the duration
     * @throws IllegalArgumentException if all the fields are null or if some is negative
     */
    public abstract Duration newDuration(
            boolean isPositive,
            BigInteger years,
            BigInteger months,
            BigInteger days,
            BigInteger hours,
            BigInteger minutes,
            BigDecimal seconds);

    /**
     * The same with {@code int}, where {@link DatatypeConstants#FIELD_UNDEFINED} leaves the field
     * absent.
     *
     * @param isPositive the sign
     * @param years the years
     * @param months the months
     * @param days the days
     * @param hours the hours
     * @param minutes the minutes
     * @param seconds the seconds, without fraction
     * @return the duration
     * @throws IllegalArgumentException if all are undefined or if some is negative
     */
    public Duration newDuration(
            final boolean isPositive,
            final int years,
            final int months,
            final int days,
            final int hours,
            final int minutes,
            final int seconds) {
        return newDuration(
                isPositive,
                toInt(years),
                toInt(months),
                toInt(days),
                toInt(hours),
                toInt(minutes),
                seconds != DatatypeConstants.FIELD_UNDEFINED
                        ? BigDecimal.valueOf((long) seconds) : null);
    }

    /**
     * An {@code xdt:dayTimeDuration} from its lexical form.
     *
     * <p>It is a duration without years or months, and that restriction is the whole point of the
     * type: without months the comparison never gives {@link DatatypeConstants#INDETERMINATE},
     * because a day always lasts the same. Hence the check is on the <b>lexical form</b> --that it
     * has no {@code Y} nor {@code M} before the {@code T}-- and not on the fields: a string with
     * years is rejected here and not later.
     *
     * @param lexicalRepresentation the lexical form, {@code PnDTnHnMnS}; cannot be null
     * @return the duration
     * @throws IllegalArgumentException if it has years or months, or if the form is wrong
     * @throws NullPointerException if it is null
     */
    public Duration newDurationDayTime(final String lexicalRepresentation) {
        if (lexicalRepresentation == null) {
            throw new NullPointerException(
                    "Trying to create an xdt:dayTimeDuration with an invalid"
                            + " lexical representation of \"null\"");
        }
        if (!isDayTimeForm(lexicalRepresentation)) {
            throw new IllegalArgumentException(
                    "Trying to create an xdt:dayTimeDuration with an invalid"
                            + " lexical representation of \"" + lexicalRepresentation
                            + "\", data model requires PnDTnHnMnS.");
        }
        return newDuration(lexicalRepresentation);
    }

    /**
     * An {@code xdt:dayTimeDuration} of that many milliseconds.
     *
     * <p>It builds the whole duration and keeps the four day and time fields; the year and month
     * ones are discarded, not added to the days.
     *
     * @param durationInMilliseconds the milliseconds, signed
     * @return the duration
     */
    public Duration newDurationDayTime(final long durationInMilliseconds) {
        Duration complete = newDuration(durationInMilliseconds);
        BigInteger days = (BigInteger) complete.getField(DatatypeConstants.DAYS);
        BigInteger hours = (BigInteger) complete.getField(DatatypeConstants.HOURS);
        BigInteger minutes = (BigInteger) complete.getField(DatatypeConstants.MINUTES);
        BigDecimal seconds = (BigDecimal) complete.getField(DatatypeConstants.SECONDS);
        return newDuration(
                complete.getSign() != -1,
                null,
                null,
                days != null ? days : BigInteger.ZERO,
                hours != null ? hours : BigInteger.ZERO,
                minutes != null ? minutes : BigInteger.ZERO,
                seconds != null ? seconds : BigDecimal.ZERO);
    }

    /**
     * An {@code xdt:dayTimeDuration} field by field.
     *
     * @param isPositive the sign
     * @param day the days, or null
     * @param hour the hours, or null
     * @param minute the minutes, or null
     * @param second the seconds, or null
     * @return the duration
     * @throws IllegalArgumentException if all are null or if some is negative
     */
    public Duration newDurationDayTime(
            final boolean isPositive,
            final BigInteger day,
            final BigInteger hour,
            final BigInteger minute,
            final BigInteger second) {
        return newDuration(
                isPositive, null, null, day, hour, minute,
                second != null ? new BigDecimal(second) : null);
    }

    /**
     * The same with {@code int}.
     *
     * @param isPositive the sign
     * @param day the days
     * @param hour the hours
     * @param minute the minutes
     * @param second the seconds
     * @return the duration
     * @throws IllegalArgumentException if some is negative
     */
    public Duration newDurationDayTime(
            final boolean isPositive,
            final int day,
            final int hour,
            final int minute,
            final int second) {
        return newDurationDayTime(
                isPositive,
                BigInteger.valueOf((long) day),
                BigInteger.valueOf((long) hour),
                BigInteger.valueOf((long) minute),
                BigInteger.valueOf((long) second));
    }

    /**
     * An {@code xdt:yearMonthDuration} from its lexical form.
     *
     * <p>The other orderable half: only years and months. Counted in months it has no ambiguity
     * either.
     *
     * @param lexicalRepresentation the lexical form, {@code PnYnM}; cannot be null
     * @return the duration
     * @throws IllegalArgumentException if it has days or time, or if the form is wrong
     * @throws NullPointerException if it is null
     */
    public Duration newDurationYearMonth(final String lexicalRepresentation) {
        if (lexicalRepresentation == null) {
            throw new NullPointerException(
                    "Trying to create an xdt:yearMonthDuration with an invalid"
                            + " lexical representation of \"null\"");
        }
        if (!isYearMonthForm(lexicalRepresentation)) {
            throw new IllegalArgumentException(
                    "Trying to create an xdt:yearMonthDuration with an invalid"
                            + " lexical representation of \"" + lexicalRepresentation
                            + "\", data model requires PnYnM.");
        }
        return newDuration(lexicalRepresentation);
    }

    /**
     * An {@code xdt:yearMonthDuration} of that many milliseconds.
     *
     * <p>It builds the whole duration and keeps the years and months; the days and time are
     * discarded. That is why {@code newDurationYearMonth} of a whole day gives {@code P0Y0M} and
     * not a fraction of a month.
     *
     * @param durationInMilliseconds the milliseconds, signed
     * @return the duration
     */
    public Duration newDurationYearMonth(final long durationInMilliseconds) {
        Duration complete = newDuration(durationInMilliseconds);
        BigInteger years = (BigInteger) complete.getField(DatatypeConstants.YEARS);
        BigInteger months = (BigInteger) complete.getField(DatatypeConstants.MONTHS);
        return newDurationYearMonth(
                complete.getSign() != -1,
                years != null ? years : BigInteger.ZERO,
                months != null ? months : BigInteger.ZERO);
    }

    /**
     * An {@code xdt:yearMonthDuration} field by field.
     *
     * @param isPositive the sign
     * @param year the years, or null
     * @param month the months, or null
     * @return the duration
     * @throws IllegalArgumentException if both are null or if some is negative
     */
    public Duration newDurationYearMonth(
            final boolean isPositive, final BigInteger year, final BigInteger month) {
        return newDuration(isPositive, year, month, null, null, null, null);
    }

    /**
     * The same with {@code int}.
     *
     * @param isPositive the sign
     * @param year the years
     * @param month the months
     * @return the duration
     * @throws IllegalArgumentException if some is negative
     */
    public Duration newDurationYearMonth(
            final boolean isPositive, final int year, final int month) {
        return newDurationYearMonth(
                isPositive, BigInteger.valueOf((long) year), BigInteger.valueOf((long) month));
    }

    /**
     * The lexical form of a {@code dayTimeDuration}: {@code [^YM]*[DT][^Y]*}.
     *
     * <p>The expression is not the one one would write from memory and it is worth reading slowly.
     * It asks for three things: that there is a {@code D} or a {@code T}, that before it there is
     * neither {@code Y} nor {@code M}, and that after it there is no {@code Y}. The asymmetry
     * --{@code M} forbidden before but allowed after-- is precisely the point: the {@code M} of
     * <b>months</b> goes before the {@code T} and is forbidden, and the one of <b>minutes</b> goes
     * after and is legal. A symmetric expression would reject {@code PT1M}, which is a perfectly
     * valid day-time duration.
     *
     * <p>It is the JDK's, and it is checked against it: the eight forms of the
     * {@code XmlDatatypeDurTest} table --{@code PT1M}, {@code P1DT1M}, {@code PT1H}, {@code P1D},
     * {@code PT0.5S}, {@code -P1DT2H}, {@code P1M}, {@code P1Y}-- were run against {@code java.exe}
     * and the eight answers match.
     */
    private static final java.util.regex.Pattern DAYTIME_FORM =
            java.util.regex.Pattern.compile("[^YM]*[DT][^Y]*");

    /**
     * The lexical form of a {@code yearMonthDuration}: {@code [^DT]*}.
     *
     * <p>Here forbidding {@code D} and {@code T} is enough, because neither of the two ever appears
     * in a duration of years and months, and without {@code T} there are no minutes to confuse the
     * {@code M} with.
     */
    private static final java.util.regex.Pattern YEARMONTH_FORM =
            java.util.regex.Pattern.compile("[^DT]*");

    /** Whether the lexical form is that of a {@code dayTimeDuration}. */
    private static boolean isDayTimeForm(String lexical) {
        return DAYTIME_FORM.matcher(lexical).matches();
    }

    /** Whether the lexical form is that of a {@code yearMonthDuration}. */
    private static boolean isYearMonthForm(String lexical) {
        return YEARMONTH_FORM.matcher(lexical).matches();
    }

    /** {@link DatatypeConstants#FIELD_UNDEFINED} becomes null; the rest, a {@link BigInteger}. */
    private static BigInteger toInt(int v) {
        return v != DatatypeConstants.FIELD_UNDEFINED ? BigInteger.valueOf((long) v) : null;
    }

    // ---- dates -------------------------------------------------------------------------------

    /**
     * A date with all the fields undefined.
     *
     * <p>To fill it later with the setters; see {@link XMLGregorianCalendar#clear}.
     *
     * @return the empty date
     */
    public abstract XMLGregorianCalendar newXMLGregorianCalendar();

    /**
     * A date from its lexical form.
     *
     * <p>It accepts the eight of XML Schema --{@code dateTime}, {@code date}, {@code time}, {@code
     * gYearMonth}, {@code gMonthDay}, {@code gYear}, {@code gMonth} and {@code gDay}-- and decides
     * which it is by the form. The fields the type does not have are left at {@link
     * DatatypeConstants#FIELD_UNDEFINED}.
     *
     * @param lexicalRepresentation the lexical form; cannot be null
     * @return the date
     * @throws IllegalArgumentException if it is none of the eight forms
     * @throws NullPointerException if it is null
     */
    public abstract XMLGregorianCalendar newXMLGregorianCalendar(String lexicalRepresentation);

    /**
     * A date copied from a {@link GregorianCalendar}.
     *
     * <p>All the fields end up defined, the time zone included: a {@code GregorianCalendar} always
     * has one, so the result is always a complete {@code xs:dateTime}.
     *
     * @param cal the calendar; cannot be null
     * @return the date
     * @throws NullPointerException if it is null
     */
    public abstract XMLGregorianCalendar newXMLGregorianCalendar(GregorianCalendar cal);

    /**
     * A date field by field, with an unbounded year.
     *
     * <p>It is the general constructor. {@link DatatypeConstants#FIELD_UNDEFINED} --or null for the
     * year and the fraction-- leaves the field undefined, which is how the partial types are built.
     *
     * @param year the year, or null
     * @param month the month from 1 to 12, or {@link DatatypeConstants#FIELD_UNDEFINED}
     * @param day the day from 1 to 31, or {@link DatatypeConstants#FIELD_UNDEFINED}
     * @param hour the hour from 0 to 23, or {@link DatatypeConstants#FIELD_UNDEFINED}
     * @param minute the minute from 0 to 59, or {@link DatatypeConstants#FIELD_UNDEFINED}
     * @param second the second from 0 to 60, or {@link DatatypeConstants#FIELD_UNDEFINED}
     * @param fractionalSecond the fraction, from 0 inclusive to 1 exclusive, or null
     * @param timezone the offset minutes, or {@link DatatypeConstants#FIELD_UNDEFINED}
     * @return the date
     * @throws IllegalArgumentException if some field is out of range
     */
    public abstract XMLGregorianCalendar newXMLGregorianCalendar(
            BigInteger year,
            int month,
            int day,
            int hour,
            int minute,
            int second,
            BigDecimal fractionalSecond,
            int timezone);

    /**
     * The same with the year as an {@code int} and the fraction as milliseconds.
     *
     * @param year the year, or {@link DatatypeConstants#FIELD_UNDEFINED}
     * @param month the month
     * @param day the day
     * @param hour the hour
     * @param minute the minute
     * @param second the second
     * @param millisecond the milliseconds from 0 to 1000, or {@link
     *     DatatypeConstants#FIELD_UNDEFINED}
     * @param timezone the offset minutes
     * @return the date
     * @throws IllegalArgumentException if some field is out of range
     */
    public XMLGregorianCalendar newXMLGregorianCalendar(
            final int year,
            final int month,
            final int day,
            final int hour,
            final int minute,
            final int second,
            final int millisecond,
            final int timezone) {
        BigInteger realYear = toInt(year);
        BigDecimal fractionValue = null;
        if (millisecond != DatatypeConstants.FIELD_UNDEFINED) {
            if (millisecond < 0 || millisecond > 1000) {
                throw new IllegalArgumentException(
                        "javax.xml.datatype.DatatypeFactory#newXMLGregorianCalendar("
                                + "int year, int month, int day, int hour, int minute,"
                                + " int second, int millisecond, int timezone)"
                                + " with invalid millisecond: " + millisecond);
            }
            // Scale three: milliseconds are the fraction with three decimals, and keeping them that
            // way makes `toXMLFormat` write `.500` and not `.5`, which is what the original does.
            fractionValue = BigDecimal.valueOf((long) millisecond, 3);
        }
        return newXMLGregorianCalendar(
                realYear, month, day, hour, minute, second, fractionValue, timezone);
    }

    /**
     * An {@code xs:date}: year, month, day and zone, without time.
     *
     * @param year the year
     * @param month the month
     * @param day the day
     * @param timezone the offset minutes, or {@link DatatypeConstants#FIELD_UNDEFINED}
     * @return the date
     * @throws IllegalArgumentException if some field is out of range
     */
    public XMLGregorianCalendar newXMLGregorianCalendarDate(
            final int year, final int month, final int day, final int timezone) {
        return newXMLGregorianCalendar(
                year,
                month,
                day,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                timezone);
    }

    /**
     * An {@code xs:time}: hour, minute, second and zone, without date.
     *
     * @param hours the hour
     * @param minutes the minute
     * @param seconds the second
     * @param timezone the offset minutes, or {@link DatatypeConstants#FIELD_UNDEFINED}
     * @return the time
     * @throws IllegalArgumentException if some field is out of range
     */
    public XMLGregorianCalendar newXMLGregorianCalendarTime(
            final int hours, final int minutes, final int seconds, final int timezone) {
        return newXMLGregorianCalendar(
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                hours,
                minutes,
                seconds,
                DatatypeConstants.FIELD_UNDEFINED,
                timezone);
    }

    /**
     * An {@code xs:time} with the complete fraction of a second.
     *
     * @param hours the hour
     * @param minutes the minute
     * @param seconds the second
     * @param fractionalSecond the fraction, from 0 inclusive to 1 exclusive, or null
     * @param timezone the offset minutes
     * @return the time
     * @throws IllegalArgumentException if some field is out of range
     */
    public XMLGregorianCalendar newXMLGregorianCalendarTime(
            final int hours,
            final int minutes,
            final int seconds,
            final BigDecimal fractionalSecond,
            final int timezone) {
        return newXMLGregorianCalendar(
                null,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                hours,
                minutes,
                seconds,
                fractionalSecond,
                timezone);
    }

    /**
     * An {@code xs:time} with the fraction given in milliseconds.
     *
     * @param hours the hour
     * @param minutes the minute
     * @param seconds the second
     * @param milliseconds the milliseconds
     * @param timezone the offset minutes
     * @return the time
     * @throws IllegalArgumentException if some field is out of range
     */
    public XMLGregorianCalendar newXMLGregorianCalendarTime(
            final int hours,
            final int minutes,
            final int seconds,
            final int milliseconds,
            final int timezone) {
        return newXMLGregorianCalendar(
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                hours,
                minutes,
                seconds,
                milliseconds,
                timezone);
    }
}
