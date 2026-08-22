package java.time.format;

import java.time.temporal.TemporalAccessor;
import java.time.temporal.ChronoField;

// KajiLibrary's java.time.format.DateTimeFormatter — a pattern-based formatter for java.time value
// types. Built from a pattern string via ofPattern (e.g. "yyyy-MM-dd HH:mm:ss") and applied with
// format(TemporalAccessor), reading fields through TemporalAccessor.getLong(ChronoField).
//
// Supported pattern letters (count = run length): u/y year (yy → 2-digit, else zero-padded to count),
// M month (1-2 numeric, 3 short name, 4+ full name), d day-of-month, D day-of-year, H hour-of-day,
// h clock-hour 1-12, m minute, s second, S fraction-of-second (first `count` nano digits), a AM/PM,
// E day-of-week (1-3 short name, 4+ full name). Text in single quotes is literal ('' → a literal
// quote); non-letter characters pass through. Names are English (Locale-aware output is out of scope,
// as is parse()). A KajiLibrary subset of the JDK class.
public final class DateTimeFormatter {

    private static final String[] MONTHS = {"January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"};
    private static final String[] MON3 = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
        "Saturday", "Sunday"};
    private static final String[] DAY3 = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private final String pattern;

    private DateTimeFormatter(String pattern) {
        this.pattern = pattern;
    }

    public static DateTimeFormatter ofPattern(String pattern) {
        return new DateTimeFormatter(pattern);
    }

    // Package-private seam for DateTimeFormatterBuilder.append(DateTimeFormatter), which composes
    // by concatenating patterns. Not public: the JDK has no such accessor, so it would be an EXTRA.
    String pattern() {
        return this.pattern;
    }

    public String format(TemporalAccessor temporal) {
        StringBuilder out = new StringBuilder();
        String p = this.pattern;
        int i = 0;
        while (i < p.length()) {
            char c = p.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                int start = i;
                while (i < p.length() && p.charAt(i) == c) {
                    i = i + 1;
                }
                out.append(field(c, i - start, temporal));
            } else if (c == '\'') {
                i = i + 1;
                if (i < p.length() && p.charAt(i) == '\'') {
                    out.append('\'');
                    i = i + 1;
                } else {
                    while (i < p.length() && p.charAt(i) != '\'') {
                        out.append(p.charAt(i));
                        i = i + 1;
                    }
                    i = i + 1;
                }
            } else {
                out.append(c);
                i = i + 1;
            }
        }
        return out.toString();
    }

    private static String field(char c, int count, TemporalAccessor ta) {
        if (c == 'y' || c == 'u') {
            long y = ta.getLong(ChronoField.YEAR);
            if (count == 2) {
                return padN(y % 100, 2);
            }
            return padN(y, count);
        }
        if (c == 'M') {
            long m = ta.getLong(ChronoField.MONTH_OF_YEAR);
            if (count <= 2) {
                return padN(m, count);
            }
            if (count == 3) {
                return MON3[(int) m - 1];
            }
            return MONTHS[(int) m - 1];
        }
        if (c == 'd') {
            return padN(ta.getLong(ChronoField.DAY_OF_MONTH), count);
        }
        if (c == 'H') {
            return padN(ta.getLong(ChronoField.HOUR_OF_DAY), count);
        }
        if (c == 'h') {
            long h = ta.getLong(ChronoField.HOUR_OF_DAY) % 12;
            if (h == 0) {
                h = 12;
            }
            return padN(h, count);
        }
        if (c == 'm') {
            return padN(ta.getLong(ChronoField.MINUTE_OF_HOUR), count);
        }
        if (c == 's') {
            return padN(ta.getLong(ChronoField.SECOND_OF_MINUTE), count);
        }
        if (c == 'S') {
            String n9 = padN(ta.getLong(ChronoField.NANO_OF_SECOND), 9);
            return n9.substring(0, count);
        }
        if (c == 'a') {
            if (ta.getLong(ChronoField.HOUR_OF_DAY) < 12) {
                return "AM";
            }
            return "PM";
        }
        if (c == 'E') {
            long dow = ta.getLong(ChronoField.DAY_OF_WEEK);
            if (count <= 3) {
                return DAY3[(int) dow - 1];
            }
            return DAYS[(int) dow - 1];
        }
        if (c == 'D') {
            return padN(ta.getLong(ChronoField.DAY_OF_YEAR), count);
        }
        throw new IllegalArgumentException("Unsupported pattern letter: " + c);
    }

    private static String padN(long v, int n) {
        String s = Long.toString(v);
        while (s.length() < n) {
            s = "0" + s;
        }
        return s;
    }
}
