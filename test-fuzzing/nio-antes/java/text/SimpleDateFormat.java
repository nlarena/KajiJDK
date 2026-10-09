package java.text;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The pattern-driven date formatter: {@code "dd/MM/yyyy HH:mm"} and whatever is read back from it.
 *
 * <p>The same separation as in {@link DecimalFormat}, one floor up: the PATTERN says which fields
 * come out and in what order, and {@link DateFormatSymbols} says what they are called. {@code MMMM}
 * means "the month, in full"; whether that prints {@code enero} or {@code January} is decided by the
 * other side.
 *
 * <p><b>The number of letters is not decoration.</b> It is the field's argument: {@code M} gives
 * {@code 1}, {@code MM} gives {@code 01}, {@code MMM} gives {@code ene} and {@code MMMM} gives
 * {@code enero}.
 * For the numeric fields the count is the minimum width; for the text ones, the threshold between
 * the short form and the long one (four or more). And {@code yy} is the special case of them all: it
 * means "two digits", not "width two", and when parsing it is interpreted against the hundred-year
 * window fixed by
 * {@link #set2DigitYearStart}.
 *
 * <p>When parsing, the fields are loaded into the {@link Calendar} and it is the one that computes
 * the instant. That division of labour is the JDK's, and it brings a consequence worth knowing:
 * {@code setLenient(false)} rejects nothing by itself, it only asks the calendar to. And this
 * library's {@code java.util.GregorianCalendar} <b>does not validate the fields in strict mode</b>
 * --a 32nd of January overflows into the 1st of February just as in lenient mode-- so today both
 * modes give the same thing. The hole is in {@code java.util}, not here: when the calendar
 * validates, this starts rejecting without a line being touched.
 *
 * @implNote A declared subset: the pattern letters implemented are
 *           {@code G y Y M d E u a H k K h m s S D F w W z Z X}. The missing ones --{@code L}
 *           (standalone month), {@code c} (standalone day), {@code B} (day period)-- need
 *           "standalone" forms and day-period names that {@code DateFormatSymbols} does not have and
 *           that are CLDR data; instead of printing the contextual form and passing it off as the
 *           standalone one, the pattern REJECTS them with {@code IllegalArgumentException}.
 */
public class SimpleDateFormat extends DateFormat {

    // These letters' order IS the encoding: a letter's index here is the "field number" it is named
    // by in a localised pattern. It comes from DateFormatSymbols.
    private static final String LETTERS = "GyMdkHmsSEDFwWahKzZ";

    private String pattern;
    private DateFormatSymbols formatData;
    private Locale locale;
    private Date defaultCenturyStart;

    public SimpleDateFormat() {
        this(LocalePatterns.dateTime(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault()),
                Locale.getDefault());
    }

    public SimpleDateFormat(String pattern) {
        this(pattern, Locale.getDefault());
    }

    public SimpleDateFormat(String pattern, Locale locale) {
        if (pattern == null || locale == null) {
            throw new NullPointerException();
        }
        this.locale = locale;
        this.formatData = new DateFormatSymbols(locale);
        this.init(pattern);
    }

    public SimpleDateFormat(String pattern, DateFormatSymbols formatSymbols) {
        if (pattern == null || formatSymbols == null) {
            throw new NullPointerException();
        }
        this.locale = Locale.getDefault();
        this.formatData = (DateFormatSymbols) formatSymbols.clone();
        this.init(pattern);
    }

    private void init(String pattern) {
        this.calendar = Calendar.getInstance(TimeZone.getDefault(), this.locale);
        this.numberFormat = NumberFormat.getNumberInstance(this.locale);
        this.numberFormat.setGroupingUsed(false);
        this.numberFormat.setParseIntegerOnly(true);
        this.applyPattern(pattern);
        this.defaultCenturyStart = this.defaultCentury();
    }

    // The two-digit window starts eighty years back: it is what the JDK does, and the asymmetry (80
    // back, 20 forward) is deliberate -- two-digit dates are usually in the past.
    private Date defaultCentury() {
        Calendar c = Calendar.getInstance(this.calendar.getTimeZone(), this.locale);
        c.setTime(new Date());
        c.set(Calendar.YEAR, c.get(Calendar.YEAR) - 80);
        return c.getTime();
    }

    public void set2DigitYearStart(Date startDate) {
        if (startDate == null) {
            throw new NullPointerException();
        }
        this.defaultCenturyStart = new Date(startDate.getTime());
    }

    public Date get2DigitYearStart() {
        return new Date(this.defaultCenturyStart.getTime());
    }

    public String toPattern() {
        return this.pattern;
    }

    /**
     * The pattern written with the locale's letters.
     *
     * <p>In the library's six locales the local letters matches with the standard ones, so today it
     * returns the same as {@link #toPattern()}. It is implemented all the same because the
     * translation is real --it walks {@link DateFormatSymbols#getLocalPatternChars()}'s table-- and
     * starts giving something different the moment a row with other letters is added.
     */
    public String toLocalizedPattern() {
        return this.translate(this.pattern, SimpleDateFormat.LETTERS,
                this.formatData.getLocalPatternChars());
    }

    public void applyPattern(String pattern) {
        if (pattern == null) {
            throw new NullPointerException();
        }
        this.checkPattern(pattern);
        this.pattern = pattern;
    }

    public void applyLocalizedPattern(String pattern) {
        if (pattern == null) {
            throw new NullPointerException();
        }
        this.applyPattern(this.translate(pattern, this.formatData.getLocalPatternChars(),
                SimpleDateFormat.LETTERS));
    }

    private String translate(String pat, String from, String towards) {
        StringBuilder sb = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < pat.length(); i = i + 1) {
            char c = pat.charAt(i);
            if (c == '\'') {
                quoted = !quoted;
                sb.append(c);
                continue;
            }
            if (quoted) {
                sb.append(c);
                continue;
            }
            int k = from.indexOf(c);
            if (k >= 0 && k < towards.length()) {
                sb.append(towards.charAt(k));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // It rejects when the pattern is applied, not when formatting: an invalid pattern has to fail
    // when it is written, not the first time somebody formats with it in production.
    private void checkPattern(String pat) {
        boolean quoted = false;
        for (int i = 0; i < pat.length(); i = i + 1) {
            char c = pat.charAt(i);
            if (c == '\'') {
                quoted = !quoted;
            } else if (!quoted && SimpleDateFormat.isLetter(c)) {
                if (!SimpleDateFormat.supported(c)) {
                    throw new IllegalArgumentException("Illegal pattern character '" + c + "'");
                }
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Unterminated quote");
        }
    }

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean supported(char c) {
        return "GyYMdEuaHkKhmsSDFwWzZX".indexOf(c) >= 0;
    }

    public DateFormatSymbols getDateFormatSymbols() {
        return (DateFormatSymbols) this.formatData.clone();
    }

    public void setDateFormatSymbols(DateFormatSymbols newFormatSymbols) {
        if (newFormatSymbols == null) {
            throw new NullPointerException();
        }
        this.formatData = (DateFormatSymbols) newFormatSymbols.clone();
    }

    // ---- formatting -----------------------------------------------------------------------------

    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
        this.write(date, toAppendTo, pos, null);
        return toAppendTo;
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        Date d;
        if (obj instanceof Date) {
            d = (Date) obj;
        } else if (obj instanceof Number) {
            d = new Date(((Number) obj).longValue());
        } else {
            throw new IllegalArgumentException("Cannot format given Object as a Date");
        }
        FieldMarks marks = new FieldMarks();
        StringBuffer sb = new StringBuffer();
        this.write(d, sb, null, marks);
        return marks.iterator(sb.toString());
    }

    private void write(Date date, StringBuffer out, FieldPosition pos, FieldMarks marks) {
        if (date == null) {
            throw new NullPointerException();
        }
        FieldMarks m = marks;
        if (m == null) {
            m = new FieldMarks();
        }
        this.calendar.setTime(date);
        int base = out.length();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int n = this.pattern.length();
        while (i < n) {
            char c = this.pattern.charAt(i);
            if (c == '\'') {
                i = i + 1;
                if (i < n && this.pattern.charAt(i) == '\'') {
                    sb.append('\'');
                    i = i + 1;
                    continue;
                }
                while (i < n && this.pattern.charAt(i) != '\'') {
                    sb.append(this.pattern.charAt(i));
                    i = i + 1;
                }
                i = i + 1;
                continue;
            }
            if (!SimpleDateFormat.isLetter(c)) {
                sb.append(c);
                i = i + 1;
                continue;
            }
            int count = 0;
            while (i < n && this.pattern.charAt(i) == c) {
                count = count + 1;
                i = i + 1;
            }
            int d = sb.length();
            this.writeField(c, count, sb);
            m.mark(SimpleDateFormat.fieldOf(c), SimpleDateFormat.numberOf(c),
                    base + d, base + sb.length());
        }
        out.append(sb.toString());
        m.apply(pos);
    }

    private void writeField(char c, int count, StringBuilder sb) {
        Calendar cal = this.calendar;
        if (c == 'G') {
            sb.append(this.formatData.getEras()[cal.get(Calendar.ERA)]);
        } else if (c == 'y' || c == 'Y') {
            int year = cal.get(Calendar.YEAR);
            if (c == 'Y') {
                year = cal.getWeekYear();
            }
            // `yy` is NOT "width two": it is "the last two digits". Treating it as a width would
            // give 2026 instead of 26, which is exactly what the short pattern asks to avoid.
            if (count == 2) {
                this.number(year % 100, 2, sb);
            } else {
                this.number(year, count, sb);
            }
        } else if (c == 'M') {
            int month = cal.get(Calendar.MONTH);
            if (count >= 4) {
                sb.append(this.formatData.getMonths()[month]);
            } else if (count == 3) {
                sb.append(this.formatData.getShortMonths()[month]);
            } else {
                this.number(month + 1, count, sb);
            }
        } else if (c == 'E') {
            int day = cal.get(Calendar.DAY_OF_WEEK);
            if (count >= 4) {
                sb.append(this.formatData.getWeekdays()[day]);
            } else {
                sb.append(this.formatData.getShortWeekdays()[day]);
            }
        } else if (c == 'a') {
            sb.append(this.formatData.getAmPmStrings()[cal.get(Calendar.AM_PM)]);
        } else if (c == 'd') {
            this.number(cal.get(Calendar.DAY_OF_MONTH), count, sb);
        } else if (c == 'H') {
            this.number(cal.get(Calendar.HOUR_OF_DAY), count, sb);
        } else if (c == 'k') {
            // k is 1..24: midnight is written 24, not 0. The conversion lives here and not in the
            // calendar because it is a convention of presentation, not of time.
            int h = cal.get(Calendar.HOUR_OF_DAY);
            if (h == 0) {
                h = 24;
            }
            this.number(h, count, sb);
        } else if (c == 'K') {
            this.number(cal.get(Calendar.HOUR), count, sb);
        } else if (c == 'h') {
            int h = cal.get(Calendar.HOUR);
            if (h == 0) {
                h = 12;
            }
            this.number(h, count, sb);
        } else if (c == 'm') {
            this.number(cal.get(Calendar.MINUTE), count, sb);
        } else if (c == 's') {
            this.number(cal.get(Calendar.SECOND), count, sb);
        } else if (c == 'S') {
            this.number(cal.get(Calendar.MILLISECOND), count, sb);
        } else if (c == 'D') {
            this.number(cal.get(Calendar.DAY_OF_YEAR), count, sb);
        } else if (c == 'F') {
            this.number(cal.get(Calendar.DAY_OF_WEEK_IN_MONTH), count, sb);
        } else if (c == 'w') {
            this.number(cal.get(Calendar.WEEK_OF_YEAR), count, sb);
        } else if (c == 'W') {
            this.number(cal.get(Calendar.WEEK_OF_MONTH), count, sb);
        } else if (c == 'u') {
            // u numbers Monday(1) to Sunday(7); Calendar numbers Sunday(1) to Saturday(7).
            int day = cal.get(Calendar.DAY_OF_WEEK) - 1;
            if (day == 0) {
                day = 7;
            }
            this.number(day, count, sb);
        } else if (c == 'z') {
            boolean daylight = this.calendar.get(Calendar.DST_OFFSET) != 0;
            boolean length = count >= 4;
            // The names set with setZoneStrings overrule the zone's own: otherwise that setter would
            // change nothing visible and would be a decorative member.
            String[] row = this.formatData.zoneRow(cal.getTimeZone().getID());
            if (row != null) {
                int col = 2;
                if (length) {
                    col = 1;
                }
                if (daylight) {
                    col = col + 2;
                }
                sb.append(row[col]);
                return;
            }
            int style = TimeZone.SHORT;
            if (length) {
                style = TimeZone.LONG;
            }
            sb.append(cal.getTimeZone().getDisplayName(daylight, style, this.locale));
        } else if (c == 'Z') {
            sb.append(this.offsetOf(false));
        } else if (c == 'X') {
            sb.append(this.offsetOf(true));
        } else {
            throw new IllegalArgumentException("Illegal pattern character '" + c + "'");
        }
    }

    // The total offset includes daylight saving: adding only the raw offset would give an hour less
    // for half the year, which is this field's classic mistake.
    private String offsetOf(boolean withColon) {
        int ms = this.calendar.get(Calendar.ZONE_OFFSET) + this.calendar.get(Calendar.DST_OFFSET);
        String sign = "+";
        int abs = ms;
        if (ms < 0) {
            sign = "-";
            abs = -ms;
        }
        int minutes = abs / 60000;
        int hh = minutes / 60;
        int mm = minutes % 60;
        StringBuilder sb = new StringBuilder();
        sb.append(sign);
        this.plainNumber(hh, 2, sb);
        if (withColon) {
            sb.append(':');
        }
        this.plainNumber(mm, 2, sb);
        return sb.toString();
    }

    // The digits come from the numberFormat because the locale may not use the Western Arabic ones;
    // the minimum width is set on it and restored, so as not to leave it altered between fields.
    private void number(int value, int width, StringBuilder sb) {
        int previousOne = this.numberFormat.getMinimumIntegerDigits();
        this.numberFormat.setMinimumIntegerDigits(width);
        sb.append(this.numberFormat.format((long) value));
        this.numberFormat.setMinimumIntegerDigits(previousOne);
    }

    private void plainNumber(int value, int width, StringBuilder sb) {
        String s = Integer.toString(value);
        while (s.length() < width) {
            s = "0" + s;
        }
        sb.append(s);
    }

    private static java.text.DateFormat.Field fieldOf(char c) {
        if (c == 'G') {
            return java.text.DateFormat.Field.ERA;
        }
        if (c == 'y' || c == 'Y') {
            return java.text.DateFormat.Field.YEAR;
        }
        if (c == 'M') {
            return java.text.DateFormat.Field.MONTH;
        }
        if (c == 'd') {
            return java.text.DateFormat.Field.DAY_OF_MONTH;
        }
        if (c == 'k') {
            return java.text.DateFormat.Field.HOUR_OF_DAY1;
        }
        if (c == 'H') {
            return java.text.DateFormat.Field.HOUR_OF_DAY0;
        }
        if (c == 'm') {
            return java.text.DateFormat.Field.MINUTE;
        }
        if (c == 's') {
            return java.text.DateFormat.Field.SECOND;
        }
        if (c == 'S') {
            return java.text.DateFormat.Field.MILLISECOND;
        }
        if (c == 'E' || c == 'u') {
            return java.text.DateFormat.Field.DAY_OF_WEEK;
        }
        if (c == 'D') {
            return java.text.DateFormat.Field.DAY_OF_YEAR;
        }
        if (c == 'F') {
            return java.text.DateFormat.Field.DAY_OF_WEEK_IN_MONTH;
        }
        if (c == 'w') {
            return java.text.DateFormat.Field.WEEK_OF_YEAR;
        }
        if (c == 'W') {
            return java.text.DateFormat.Field.WEEK_OF_MONTH;
        }
        if (c == 'a') {
            return java.text.DateFormat.Field.AM_PM;
        }
        if (c == 'h') {
            return java.text.DateFormat.Field.HOUR1;
        }
        if (c == 'K') {
            return java.text.DateFormat.Field.HOUR0;
        }
        return java.text.DateFormat.Field.TIME_ZONE;
    }

    private static int numberOf(char c) {
        if (c == 'G') {
            return DateFormat.ERA_FIELD;
        }
        if (c == 'y' || c == 'Y') {
            return DateFormat.YEAR_FIELD;
        }
        if (c == 'M') {
            return DateFormat.MONTH_FIELD;
        }
        if (c == 'd') {
            return DateFormat.DATE_FIELD;
        }
        if (c == 'k') {
            return DateFormat.HOUR_OF_DAY1_FIELD;
        }
        if (c == 'H') {
            return DateFormat.HOUR_OF_DAY0_FIELD;
        }
        if (c == 'm') {
            return DateFormat.MINUTE_FIELD;
        }
        if (c == 's') {
            return DateFormat.SECOND_FIELD;
        }
        if (c == 'S') {
            return DateFormat.MILLISECOND_FIELD;
        }
        if (c == 'E' || c == 'u') {
            return DateFormat.DAY_OF_WEEK_FIELD;
        }
        if (c == 'D') {
            return DateFormat.DAY_OF_YEAR_FIELD;
        }
        if (c == 'F') {
            return DateFormat.DAY_OF_WEEK_IN_MONTH_FIELD;
        }
        if (c == 'w') {
            return DateFormat.WEEK_OF_YEAR_FIELD;
        }
        if (c == 'W') {
            return DateFormat.WEEK_OF_MONTH_FIELD;
        }
        if (c == 'a') {
            return DateFormat.AM_PM_FIELD;
        }
        if (c == 'h') {
            return DateFormat.HOUR1_FIELD;
        }
        if (c == 'K') {
            return DateFormat.HOUR0_FIELD;
        }
        return DateFormat.TIMEZONE_FIELD;
    }

    // ---- parsing --------------------------------------------------------------------------------

    /**
     * It reads a date written with this pattern.
     *
     * <p>The fields are not combined here: they are loaded into the {@link Calendar} and it computes
     * the instant. That is why {@code isLenient()} genuinely rules --a 32nd of January is an error or
     * the 1st of February depending on how the calendar stands-- and why a pattern with no year gives
     * the current year and not year zero.
     */
    public Date parse(String text, ParsePosition pos) {
        if (text == null || pos == null) {
            throw new NullPointerException();
        }
        int start = pos.getIndex();
        int t = start;
        this.calendar.clear();
        int i = 0;
        int n = this.pattern.length();
        while (i < n) {
            char c = this.pattern.charAt(i);
            if (c == '\'') {
                i = i + 1;
                if (i < n && this.pattern.charAt(i) == '\'') {
                    if (t >= text.length() || text.charAt(t) != '\'') {
                        pos.setErrorIndex(t);
                        return null;
                    }
                    t = t + 1;
                    i = i + 1;
                    continue;
                }
                while (i < n && this.pattern.charAt(i) != '\'') {
                    if (t >= text.length() || text.charAt(t) != this.pattern.charAt(i)) {
                        pos.setErrorIndex(t);
                        return null;
                    }
                    t = t + 1;
                    i = i + 1;
                }
                i = i + 1;
                continue;
            }
            if (!SimpleDateFormat.isLetter(c)) {
                if (t >= text.length() || text.charAt(t) != c) {
                    pos.setErrorIndex(t);
                    return null;
                }
                t = t + 1;
                i = i + 1;
                continue;
            }
            int count = 0;
            while (i < n && this.pattern.charAt(i) == c) {
                count = count + 1;
                i = i + 1;
            }
            boolean gluedToNumber = i < n && SimpleDateFormat.isLetter(this.pattern.charAt(i))
                    && SimpleDateFormat.isNumeric(this.pattern.charAt(i), 1);
            int nextLevel = this.readField(c, count, text, t, gluedToNumber);
            if (nextLevel < 0) {
                pos.setErrorIndex(t);
                return null;
            }
            t = nextLevel;
        }
        Date d;
        try {
            d = this.calendar.getTime();
        } catch (IllegalArgumentException e) {
            // Strict mode: the calendar rejects a 32nd of January. It is reported as a parse failure
            // with the cursor NOT advanced, which is how the contract tells "I could not" from "I
            // read null".
            pos.setErrorIndex(start);
            return null;
        }
        pos.setIndex(t);
        return d;
    }

    private static boolean isNumeric(char c, int count) {
        if (c == 'M' || c == 'E') {
            return count < 3;
        }
        return "yYdHkKhmsSDFwWu".indexOf(c) >= 0;
    }

    // It returns the index after the field, or -1 if it could not be read.
    private int readField(char c, int count, String text, int from, boolean gluedToNumber) {
        if (c == 'G') {
            return this.readText(text, from, this.formatData.getEras(), Calendar.ERA, 0);
        }
        if (c == 'M' && count >= 3) {
            int r = this.readText(text, from, this.formatData.getMonths(), Calendar.MONTH, 0);
            if (r < 0) {
                r = this.readText(text, from, this.formatData.getShortMonths(), Calendar.MONTH, 0);
            }
            return r;
        }
        if (c == 'E') {
            int r = this.readText(text, from, this.formatData.getWeekdays(), Calendar.DAY_OF_WEEK, 0);
            if (r < 0) {
                r = this.readText(text, from, this.formatData.getShortWeekdays(),
                        Calendar.DAY_OF_WEEK, 0);
            }
            return r;
        }
        if (c == 'a') {
            return this.readText(text, from, this.formatData.getAmPmStrings(), Calendar.AM_PM, 0);
        }
        if (c == 'z' || c == 'Z' || c == 'X') {
            return this.readZone(text, from);
        }
        // The fixed width is only imposed when the next field is numeric too: with no separator
        // between two numbers, the only way of knowing where the first ends is the pattern's count.
        // With a separator it is better to read every digit there is.
        int max = 10;
        if (gluedToNumber || (c == 'y' && count == 2)) {
            max = count;
        }
        int end = from;
        while (end < text.length() && end - from < max && SimpleDateFormat.isDigit(text.charAt(end))) {
            end = end + 1;
        }
        if (end == from) {
            return -1;
        }
        int value = 0;
        for (int k = from; k < end; k = k + 1) {
            value = value * 10 + (text.charAt(k) - '0');
        }
        this.load(c, count, value, end - from);
        return end;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private void load(char c, int count, int value, int digits) {
        if (c == 'y') {
            int year = value;
            // Two digits written are two digits read: they are placed in the hundred-year window
            // starting at defaultCenturyStart. With more digits the year is literal, and that is why
            // "0080" and "80" do not mean the same -- which is exactly what the JDK says.
            if (count == 2 && digits == 2) {
                year = this.inWindow(value);
            }
            this.calendar.set(Calendar.YEAR, year);
        } else if (c == 'Y') {
            this.calendar.set(Calendar.YEAR, value);
        } else if (c == 'M') {
            this.calendar.set(Calendar.MONTH, value - 1);
        } else if (c == 'd') {
            this.calendar.set(Calendar.DAY_OF_MONTH, value);
        } else if (c == 'H') {
            this.calendar.set(Calendar.HOUR_OF_DAY, value);
        } else if (c == 'k') {
            int h = value;
            if (h == 24) {
                h = 0;
            }
            this.calendar.set(Calendar.HOUR_OF_DAY, h);
        } else if (c == 'K') {
            this.calendar.set(Calendar.HOUR, value);
        } else if (c == 'h') {
            int h = value;
            if (h == 12) {
                h = 0;
            }
            this.calendar.set(Calendar.HOUR, h);
        } else if (c == 'm') {
            this.calendar.set(Calendar.MINUTE, value);
        } else if (c == 's') {
            this.calendar.set(Calendar.SECOND, value);
        } else if (c == 'S') {
            this.calendar.set(Calendar.MILLISECOND, value);
        } else if (c == 'D') {
            this.calendar.set(Calendar.DAY_OF_YEAR, value);
        } else if (c == 'F') {
            this.calendar.set(Calendar.DAY_OF_WEEK_IN_MONTH, value);
        } else if (c == 'w') {
            this.calendar.set(Calendar.WEEK_OF_YEAR, value);
        } else if (c == 'W') {
            this.calendar.set(Calendar.WEEK_OF_MONTH, value);
        } else if (c == 'u') {
            int day = value + 1;
            if (day > 7) {
                day = 1;
            }
            this.calendar.set(Calendar.DAY_OF_WEEK, day);
        }
    }

    private int inWindow(int twoDigits) {
        Calendar c = Calendar.getInstance(this.calendar.getTimeZone(), this.locale);
        c.setTime(this.defaultCenturyStart);
        int start = c.get(Calendar.YEAR);
        int candidate = (start / 100) * 100 + twoDigits;
        if (candidate < start) {
            candidate = candidate + 100;
        }
        return candidate;
    }

    // It returns the index after the longest name that matches, and loads the field with its
    // position. The longest and not the first: "sept" and "sep" can coexist in the same table, and
    // keeping the first would leave the "t" loose for the following literal.
    private int readText(String text, int from, String[] names, int field, int base) {
        int best = -1;
        int bestLength = 0;
        for (int i = 0; i < names.length; i = i + 1) {
            String name = names[i];
            if (name == null || name.length() == 0) {
                continue;
            }
            if (name.length() > bestLength && this.matchesIgnoringCase(text, from, name)) {
                best = i;
                bestLength = name.length();
            }
        }
        if (best < 0) {
            return -1;
        }
        this.calendar.set(field, best + base);
        return from + bestLength;
    }

    private boolean matchesIgnoringCase(String text, int from, String name) {
        if (from + name.length() > text.length()) {
            return false;
        }
        for (int i = 0; i < name.length(); i = i + 1) {
            char a = text.charAt(from + i);
            char b = name.charAt(i);
            if (a != b && Character.toLowerCase(a) != Character.toLowerCase(b)) {
                return false;
            }
        }
        return true;
    }

    // It accepts "+HH:MM", "+HHMM", "GMT+H:MM" and the names the current zone can give of itself. It
    // does not search the zone database by name: with no CLDR data there is no "EST -> America/
    // New_York" table, and guessing one would choose wrongly as soon as two zones shared an
    // abbreviation.
    private int readZone(String text, int from) {
        int t = from;
        if (t + 3 <= text.length() && text.substring(t, t + 3).equals("GMT")) {
            t = t + 3;
            if (t >= text.length() || (text.charAt(t) != '+' && text.charAt(t) != '-')) {
                this.calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
                return t;
            }
        }
        if (t < text.length() && (text.charAt(t) == '+' || text.charAt(t) == '-')) {
            int sign = 1;
            if (text.charAt(t) == '-') {
                sign = -1;
            }
            t = t + 1;
            int hh = 0;
            int readSoFar = 0;
            while (t < text.length() && readSoFar < 2 && SimpleDateFormat.isDigit(text.charAt(t))) {
                hh = hh * 10 + (text.charAt(t) - '0');
                t = t + 1;
                readSoFar = readSoFar + 1;
            }
            if (readSoFar == 0) {
                return -1;
            }
            if (t < text.length() && text.charAt(t) == ':') {
                t = t + 1;
            }
            int mm = 0;
            readSoFar = 0;
            while (t < text.length() && readSoFar < 2 && SimpleDateFormat.isDigit(text.charAt(t))) {
                mm = mm * 10 + (text.charAt(t) - '0');
                t = t + 1;
                readSoFar = readSoFar + 1;
            }
            int total = sign * (hh * 3600000 + mm * 60000);
            this.calendar.set(Calendar.ZONE_OFFSET, total);
            this.calendar.set(Calendar.DST_OFFSET, 0);
            return t;
        }
        // Last resort: the name the formatter's own zone declares. It is enough to read back what
        // this same formatter wrote, which is the round-trip case.
        TimeZone z = this.calendar.getTimeZone();
        String[] candidates = new String[] {
            z.getID(),
            z.getDisplayName(false, TimeZone.LONG, this.locale),
            z.getDisplayName(true, TimeZone.LONG, this.locale),
            z.getDisplayName(false, TimeZone.SHORT, this.locale),
            z.getDisplayName(true, TimeZone.SHORT, this.locale),
        };
        int bestLength = 0;
        for (int i = 0; i < candidates.length; i = i + 1) {
            String s = candidates[i];
            if (s != null && s.length() > bestLength && this.matchesIgnoringCase(text, from, s)) {
                bestLength = s.length();
            }
        }
        if (bestLength == 0) {
            return -1;
        }
        return from + bestLength;
    }

    // ---- identity -------------------------------------------------------------------------------

    public Object clone() {
        SimpleDateFormat copy = new SimpleDateFormat(this.pattern, this.locale);
        copy.formatData = (DateFormatSymbols) this.formatData.clone();
        copy.calendar = Calendar.getInstance(this.calendar.getTimeZone(), this.locale);
        copy.calendar.setLenient(this.calendar.isLenient());
        copy.numberFormat = this.numberFormat;
        copy.defaultCenturyStart = new Date(this.defaultCenturyStart.getTime());
        return copy;
    }

    public int hashCode() {
        return this.pattern.hashCode();
    }

    public String toString() {
        return "java.text.SimpleDateFormat[pattern=" + this.pattern + "]";
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        SimpleDateFormat other = (SimpleDateFormat) obj;
        return this.pattern.equals(other.pattern) && this.formatData.equals(other.formatData);
    }
}
