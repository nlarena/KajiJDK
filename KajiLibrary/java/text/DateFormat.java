package java.text;

import java.io.InvalidObjectException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * The abstract base of the date and time formatters.
 *
 * <p><b>On the project's old note.</b> This file was missing because
 * "{@code DateFormat}/{@code SimpleDateFormat} are blocked -- their whole API leans on
 * {@code java.util.Date}, {@code Calendar} and {@code TimeZone}, which do not exist". That is no
 * longer true: all three exist and are complete, so the class could be written whole. Not one member
 * was left out for want of dependencies.
 *
 * <p><b>What this class really does.</b> It does not format: it delegates. A {@code Date} is an
 * instant --a number of milliseconds-- and knows nothing of years or months; the one that knows is
 * the {@link Calendar}, which translates that instant into fields according to a time zone and a
 * calendar. That is why {@code calendar} is a {@code protected} field and not an internal detail:
 * changing it changes the result, and {@link #setTimeZone} is no more than a shortcut for touching
 * it.
 *
 * <p>The {@code numberFormat} is there for the same reason one step further down: a date's fields
 * are written with digits, and which digits each locale uses is decided by a {@link NumberFormat}.
 *
 * <p><b>The styles run the opposite way to what one expects</b>: {@code FULL} is 0 and {@code SHORT}
 * is 3, so the "largest" style is the smallest number. It is like that in the original API and
 * cannot be changed; it is worth bearing in mind when reading any comparison between styles.
 *
 * @implNote The per-locale, per-style patterns come from {@code LocalePatterns}, which covers six
 *           locales; an unknown one falls back to ROOT, just as in the JDK when it has no data. The
 *           names --months, days, am/pm-- come from {@link DateFormatSymbols}.
 */
public abstract class DateFormat extends Format {

    public static final int ERA_FIELD = 0;
    public static final int YEAR_FIELD = 1;
    public static final int MONTH_FIELD = 2;
    public static final int DATE_FIELD = 3;
    public static final int HOUR_OF_DAY1_FIELD = 4;
    public static final int HOUR_OF_DAY0_FIELD = 5;
    public static final int MINUTE_FIELD = 6;
    public static final int SECOND_FIELD = 7;
    public static final int MILLISECOND_FIELD = 8;
    public static final int DAY_OF_WEEK_FIELD = 9;
    public static final int DAY_OF_YEAR_FIELD = 10;
    public static final int DAY_OF_WEEK_IN_MONTH_FIELD = 11;
    public static final int WEEK_OF_YEAR_FIELD = 12;
    public static final int WEEK_OF_MONTH_FIELD = 13;
    public static final int AM_PM_FIELD = 14;
    public static final int HOUR1_FIELD = 15;
    public static final int HOUR0_FIELD = 16;
    public static final int TIMEZONE_FIELD = 17;

    public static final int FULL = 0;
    public static final int LONG = 1;
    public static final int MEDIUM = 2;
    public static final int SHORT = 3;
    public static final int DEFAULT = 2;

    /**
     * The key a date formatter marks each field of the text it produced with.
     *
     * <p>Unlike {@link java.text.NumberFormat.Field}, this one also carries the equivalent
     * {@link Calendar} field: it is the bridge between the two ways of naming a date field --
     * {@code java.text}'s and {@code java.util}'s -- and that is why it has
     * {@link #getCalendarField()} and {@link #ofCalendarField(int)}, which
     * {@code java.text.NumberFormat.Field} does not need.
     */
    public static class Field extends java.text.Format.Field {

        private static final Map<String, java.text.DateFormat.Field> BY_NAME =
                new HashMap<String, java.text.DateFormat.Field>();

        // An index by Calendar field, for ofCalendarField. An array and not a Map because the keys
        // are the dense integers 0..FIELD_COUNT and the array IS the index.
        private static final java.text.DateFormat.Field[] BY_FIELD = new java.text.DateFormat.Field[Calendar.FIELD_COUNT];

        private final int calendarField;

        protected Field(String name, int calendarField) {
            super(name);
            this.calendarField = calendarField;
            if (this.getClass() == java.text.DateFormat.Field.class) {
                BY_NAME.put(name, this);
                if (calendarField >= 0 && calendarField < Calendar.FIELD_COUNT) {
                    BY_FIELD[calendarField] = this;
                }
            }
        }

        /**
         * The key corresponding to a {@link Calendar} field.
         *
         * @throws IllegalArgumentException if the integer names no Calendar field. It is thrown
         *         instead of returning {@code null} because a non-existent field is the caller's
         *         mistake, not a "there is no datum".
         */
        public static java.text.DateFormat.Field ofCalendarField(int calendarField) {
            if (calendarField < 0 || calendarField >= Calendar.FIELD_COUNT) {
                throw new IllegalArgumentException("Unknown Calendar constant " + calendarField);
            }
            return BY_FIELD[calendarField];
        }

        public int getCalendarField() {
            return this.calendarField;
        }

        protected Object readResolve() throws InvalidObjectException {
            if (this.getClass() != java.text.DateFormat.Field.class) {
                throw new InvalidObjectException("subclass didn't correctly implement readResolve");
            }
            java.text.DateFormat.Field f = BY_NAME.get(this.getName());
            if (f != null) {
                return f;
            }
            throw new InvalidObjectException("unknown attribute name");
        }

        public static final java.text.DateFormat.Field ERA = new java.text.DateFormat.Field("era", Calendar.ERA);
        public static final java.text.DateFormat.Field YEAR = new java.text.DateFormat.Field("year", Calendar.YEAR);
        public static final java.text.DateFormat.Field MONTH = new java.text.DateFormat.Field("month", Calendar.MONTH);
        public static final java.text.DateFormat.Field DAY_OF_MONTH =
                new java.text.DateFormat.Field("day of month", Calendar.DAY_OF_MONTH);
        public static final java.text.DateFormat.Field HOUR_OF_DAY1 =
                new java.text.DateFormat.Field("hour of day 1", -1);
        public static final java.text.DateFormat.Field HOUR_OF_DAY0 =
                new java.text.DateFormat.Field("hour of day", Calendar.HOUR_OF_DAY);
        public static final java.text.DateFormat.Field MINUTE = new java.text.DateFormat.Field("minute", Calendar.MINUTE);
        public static final java.text.DateFormat.Field SECOND = new java.text.DateFormat.Field("second", Calendar.SECOND);
        public static final java.text.DateFormat.Field MILLISECOND =
                new java.text.DateFormat.Field("millisecond", Calendar.MILLISECOND);
        public static final java.text.DateFormat.Field DAY_OF_WEEK =
                new java.text.DateFormat.Field("day of week", Calendar.DAY_OF_WEEK);
        public static final java.text.DateFormat.Field DAY_OF_YEAR =
                new java.text.DateFormat.Field("day of year", Calendar.DAY_OF_YEAR);
        public static final java.text.DateFormat.Field DAY_OF_WEEK_IN_MONTH =
                new java.text.DateFormat.Field("day of week in month", Calendar.DAY_OF_WEEK_IN_MONTH);
        public static final java.text.DateFormat.Field WEEK_OF_YEAR =
                new java.text.DateFormat.Field("week of year", Calendar.WEEK_OF_YEAR);
        public static final java.text.DateFormat.Field WEEK_OF_MONTH =
                new java.text.DateFormat.Field("week of month", Calendar.WEEK_OF_MONTH);
        public static final java.text.DateFormat.Field AM_PM = new java.text.DateFormat.Field("am pm", Calendar.AM_PM);
        public static final java.text.DateFormat.Field HOUR1 = new java.text.DateFormat.Field("hour 1", -1);
        public static final java.text.DateFormat.Field HOUR0 = new java.text.DateFormat.Field("hour", Calendar.HOUR);
        public static final java.text.DateFormat.Field TIME_ZONE = new java.text.DateFormat.Field("time zone", -1);
    }

    /**
     * The calendar that translates the instant into fields. It is {@code protected} because a
     * subclass reads it directly in order to format, and because changing it is the documented way
     * of changing the time zone or the calendar system.
     */
    protected Calendar calendar;

    /** What each field's digits are written with. */
    protected NumberFormat numberFormat;

    protected DateFormat() {
    }

    public final StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        if (obj instanceof Date) {
            return this.format((Date) obj, toAppendTo, fieldPosition);
        }
        if (obj instanceof Number) {
            // A Number is read as milliseconds since the epoch. It is not a whimsical convenience:
            // it is what makes a MessageFormat with {0,date} accept the raw long.
            return this.format(new Date(((Number) obj).longValue()), toAppendTo, fieldPosition);
        }
        throw new IllegalArgumentException("Cannot format given Object as a Date");
    }

    public abstract StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition);

    public final String format(Date date) {
        return this.format(date, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public Date parse(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Date result = this.parse(source, pos);
        if (pos.getIndex() == 0) {
            throw new ParseException("Unparseable date: \"" + source + "\"", pos.getErrorIndex());
        }
        return result;
    }

    public abstract Date parse(String source, ParsePosition pos);

    public Object parseObject(String source, ParsePosition pos) {
        return this.parse(source, pos);
    }

    // ---- factories ----

    public static final DateFormat getTimeInstance() {
        return DateFormat.getTimeInstance(DateFormat.DEFAULT, Locale.getDefault());
    }

    public static final DateFormat getTimeInstance(int style) {
        return DateFormat.getTimeInstance(style, Locale.getDefault());
    }

    public static final DateFormat getTimeInstance(int style, Locale toLocale) {
        return new SimpleDateFormat(LocalePatterns.hour(DateFormat.check(style), toLocale), toLocale);
    }

    public static final DateFormat getDateInstance() {
        return DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());
    }

    public static final DateFormat getDateInstance(int style) {
        return DateFormat.getDateInstance(style, Locale.getDefault());
    }

    public static final DateFormat getDateInstance(int style, Locale toLocale) {
        return new SimpleDateFormat(LocalePatterns.date(DateFormat.check(style), toLocale), toLocale);
    }

    public static final DateFormat getDateTimeInstance() {
        return DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT,
                Locale.getDefault());
    }

    public static final DateFormat getDateTimeInstance(int dateStyle, int timeStyle) {
        return DateFormat.getDateTimeInstance(dateStyle, timeStyle, Locale.getDefault());
    }

    public static final DateFormat getDateTimeInstance(int dateStyle, int timeStyle, Locale toLocale) {
        return new SimpleDateFormat(LocalePatterns.dateTime(DateFormat.check(dateStyle),
                DateFormat.check(timeStyle), toLocale), toLocale);
    }

    /** Date and time, both in the SHORT style: the API's "give me something short". */
    public static final DateFormat getInstance() {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    }

    private static int check(int style) {
        if (style < DateFormat.FULL || style > DateFormat.SHORT) {
            throw new IllegalArgumentException("Illegal date/time style " + style);
        }
        return style;
    }

    /**
     * The locales with data of their own. They are {@link NumberFormat}'s same six: patterns and
     * names come from the package's two tables, and both cover the same rows.
     */
    public static Locale[] getAvailableLocales() {
        return DecimalFormatSymbols.getAvailableLocales();
    }

    // ---- state  ----

    public void setCalendar(Calendar newCalendar) {
        this.calendar = newCalendar;
    }

    public Calendar getCalendar() {
        return this.calendar;
    }

    public void setNumberFormat(NumberFormat newNumberFormat) {
        this.numberFormat = newNumberFormat;
    }

    public NumberFormat getNumberFormat() {
        return this.numberFormat;
    }

    // The zone is not kept here: it lives in the calendar, which is the only one that uses it.
    // Keeping a copy would be keeping two truths, and the one that rules when formatting would always
    // be the calendar's.
    public void setTimeZone(TimeZone zone) {
        this.calendar.setTimeZone(zone);
    }

    public TimeZone getTimeZone() {
        return this.calendar.getTimeZone();
    }

    public void setLenient(boolean lenient) {
        this.calendar.setLenient(lenient);
    }

    public boolean isLenient() {
        return this.calendar.isLenient();
    }

    public int hashCode() {
        return this.numberFormat.hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        DateFormat other = (DateFormat) obj;
        return this.calendar.getFirstDayOfWeek() == other.calendar.getFirstDayOfWeek()
                && this.calendar.getMinimalDaysInFirstWeek() == other.calendar.getMinimalDaysInFirstWeek()
                && this.calendar.isLenient() == other.calendar.isLenient()
                // By ID and not by equals: our java.util.TimeZone does not redefine equals, so two
                // instances of the SAME zone asked for separately would come out different and two
                // identical formatters would never be equal.
                && this.calendar.getTimeZone().getID().equals(other.calendar.getTimeZone().getID())
                && this.numberFormat.equals(other.numberFormat);
    }
}
