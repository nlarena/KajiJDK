package java.time.format;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalQueries;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

// The three parts that depend on the locale and **can** be given: the localized pattern, the offset
// with the word `GMT` in front, and the calendar's name.
//
// **Where the pattern comes from, and why it is possible now.** `DateTimeFormatterBuilder`'s header
// said `appendLocalized` was out because the pattern of a short date --`M/d/yy` in the United
// States, `dd.MM.yy` in Germany-- is CLDR data and cannot be deduced. That is still true, but the
// data is no longer missing: `java.text.PatronesLocales` brings it, extracted from JDK 25, and it
// is reached through `DateFormat.getDateInstance(style, locale)`.
//
// And it is not an assumed equivalence: it was measured against the real JDK for the four styles,
// the three combinations (date, time, both) and seven locales, and **the pattern
// `DateTimeFormatterBuilder.getLocalizedDateTimePattern` returns is exactly the one
// `((SimpleDateFormat) DateFormat.getXxxInstance(style, locale)).toPattern()` returns**. It makes
// sense: both read the same CLDR row, and the pattern letters that turn up there --`y M d E H h m s
// a z`-- mean the same in `java.text` and in `java.time`.
//
// **What is still out, and why.** Zone names (`appendZoneText`), day periods
// (`appendDayPeriodText`) and templates like `yMMMd` (`ofLocalizedPattern`) ask for CLDR text
// tables this library does not ship. The distinction is the usual one: a missing pattern can be
// looked up elsewhere, an invented name says "this is the name" and it is not.

// `appendLocalized(FormatStyle, FormatStyle)`: the pattern comes from the locale **at the moment
// the formatter is used**, not when it is built.
//
// It is the reason this is a part and not an `appendPattern` resolved in the constructor:
// `withLocale` may change the locale afterwards, and then the pattern has to change with it. The
// compiled formatters are kept per locale and chronology, which is what the JDK does too.
final class LocalizedPart extends Part {

    private final FormatStyle dateStyle;
    private final FormatStyle timeStyle;
    private final Map<String, CompositePart> compiled;

    LocalizedPart(FormatStyle dateStyle, FormatStyle timeStyle) {
        this.dateStyle = dateStyle;
        this.timeStyle = timeStyle;
        this.compiled = new HashMap<String, CompositePart>();
    }

    // The formatter for that locale and that chronology, compiling the first time.
    private CompositePart forLocale(Locale locale, Chronology chronology) {
        Chronology c = chronology == null ? IsoChronology.INSTANCE : chronology;
        String key = locale.toString() + "|" + c.getId();
        CompositePart already = this.compiled.get(key);
        if (already != null) {
            return already;
        }
        String pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                this.dateStyle, this.timeStyle, c, locale);
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendPattern(pattern)
                .toFormatter(locale);
        CompositePart built = f.parts();
        this.compiled.put(key, built);
        return built;
    }

    boolean print(PrintContext ctx, StringBuilder out) {
        Chronology c = ctx.query(TemporalQueries.chronology());
        return this.forLocale(ctx.locale, c).print(ctx, out);
    }

    int parse(ParseContext ctx, String text, int pos) {
        return this.forLocale(ctx.locale, ctx.chronology).parse(ctx, text, pos);
    }

    public String toString() {
        return "Localized(" + this.dateStyle + "," + this.timeStyle + ")";
    }
}

// `appendLocalized(String)`: the same as `LocalizedPart` but asking for the fields by template
// instead of by style.
//
// The difference from the four styles is one of grain. A style --`MEDIUM`-- says "a middling date"
// and the language decides which fields go in; a template says exactly which fields are wanted and
// lets the language decide the order. It is what is needed when the program knows it needs the
// month and the day but does not want the year, which no style offers.
//
// The pattern is resolved when the formatter is used, not when it is built, for the same reason as
// over there: a later `withLocale` has to change the result.
final class TemplatePart extends Part {

    private final String template;
    private final Map<String, CompositePart> compiled;

    TemplatePart(String template) {
        this.template = template;
        this.compiled = new HashMap<String, CompositePart>();
    }

    private CompositePart forLocale(Locale locale, Chronology chronology) {
        Chronology c = chronology == null ? IsoChronology.INSTANCE : chronology;
        String key = locale.toString() + "|" + c.getId();
        CompositePart already = this.compiled.get(key);
        if (already != null) {
            return already;
        }
        String pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                this.template, c, locale);
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendPattern(pattern)
                .toFormatter(locale);
        CompositePart built = f.parts();
        this.compiled.put(key, built);
        return built;
    }

    boolean print(PrintContext ctx, StringBuilder out) {
        Chronology c = ctx.query(TemporalQueries.chronology());
        return this.forLocale(ctx.locale, c).print(ctx, out);
    }

    int parse(ParseContext ctx, String text, int pos) {
        return this.forLocale(ctx.locale, ctx.chronology).parse(ctx, text, pos);
    }

    public String toString() {
        return "Localized(" + this.template + ")";
    }
}

// `appendLocalizedOffset`: `GMT`, and the offset behind it.
//
// **The word `GMT` is fixed on purpose, and it is not an approximation of this library's**: the JDK
// has a `// TODO: get localized version of 'GMT'` right there and writes the constant. Copying the
// code and not the intent is what makes the two outputs agree.
//
// The difference between the two styles is only in the hours: `FULL` always writes two digits and
// the minutes, `SHORT` writes the hour unpadded and skips the minutes when they are zero. An offset
// of zero is a bare `GMT` in both.
final class LocalizedOffsetPart extends Part {

    private static final String GMT = "GMT";

    private final TextStyle style;

    LocalizedOffsetPart(TextStyle style) {
        this.style = style;
    }

    private static void twoDigits(StringBuilder out, int value) {
        out.append((char) (value / 10 + '0'));
        out.append((char) (value % 10 + '0'));
    }

    boolean print(PrintContext ctx, StringBuilder out) {
        Long seconds = ctx.value(ChronoField.OFFSET_SECONDS);
        if (seconds == null) {
            return false;
        }
        out.append(GMT);
        int total = (int) seconds.longValue();
        if (total == 0) {
            return true;
        }
        int hours = Math.abs((total / 3600) % 100);
        int minutes = Math.abs((total / 60) % 60);
        int secs = Math.abs(total % 60);
        out.append(total < 0 ? '-' : '+');
        if (this.style == TextStyle.FULL) {
            twoDigits(out, hours);
            out.append(':');
            twoDigits(out, minutes);
            if (secs != 0) {
                out.append(':');
                twoDigits(out, secs);
            }
        } else {
            if (hours >= 10) {
                out.append((char) (hours / 10 + '0'));
            }
            out.append((char) (hours % 10 + '0'));
            if (minutes != 0 || secs != 0) {
                out.append(':');
                twoDigits(out, minutes);
                if (secs != 0) {
                    out.append(':');
                    twoDigits(out, secs);
                }
            }
        }
        return true;
    }

    // Reads `GMT` and, if what follows is a sign, the offset. A bare `GMT` is worth zero, which is
    // right: with no sign there is no offset written.
    int parse(ParseContext ctx, String text, int pos) {
        int p = pos;
        if (!text.regionMatches(!ctx.caseSensitive, p, GMT, 0, GMT.length())) {
            return ~p;
        }
        p = p + GMT.length();
        char sign = p < text.length() ? text.charAt(p) : ' ';
        if (sign != '+' && sign != '-') {
            ctx.put(ChronoField.OFFSET_SECONDS, 0L);
            return p;
        }
        int negative = sign == '-' ? -1 : 1;
        p = p + 1;
        // The hours come with no colon in front and may be one or two digits; the minutes and the
        // seconds each come behind their colon, and they are optional from the outside in: there
        // are no seconds without minutes.
        int d1 = digitAt(text, p);
        if (d1 < 0) {
            return ~p;
        }
        p = p + 1;
        int hours = d1;
        int d2 = digitAt(text, p);
        if (d2 >= 0) {
            hours = hours * 10 + d2;
            p = p + 1;
        }
        int minutes = 0;
        int secs = 0;
        if (p + 2 < text.length() && text.charAt(p) == ':') {
            int m1 = digitAt(text, p + 1);
            int m2 = digitAt(text, p + 2);
            if (m1 >= 0 && m2 >= 0) {
                minutes = m1 * 10 + m2;
                p = p + 3;
                if (p + 2 < text.length() && text.charAt(p) == ':') {
                    int s1 = digitAt(text, p + 1);
                    int s2 = digitAt(text, p + 2);
                    if (s1 >= 0 && s2 >= 0) {
                        secs = s1 * 10 + s2;
                        p = p + 3;
                    }
                }
            }
        }
        long total = negative * (hours * 3600L + minutes * 60L + secs);
        ctx.put(ChronoField.OFFSET_SECONDS, total);
        return p;
    }

    private static int digitAt(String text, int p) {
        if (p >= text.length()) {
            return -1;
        }
        char c = text.charAt(p);
        if (c < '0' || c > '9') {
            return -1;
        }
        return c - '0';
    }

    public String toString() {
        return "LocalizedOffset(" + this.style + ")";
    }
}

// `appendChronologyText`: the calendar's name.
//
// It delegates to `Chronology.getDisplayName(TextStyle, Locale)`, which is what the JDK does. That
// library does not ship the translated names and that method always falls into its fallback branch
// --the calendar's id; for ISO it agrees with the JDK, for the rest it falls a word short. It is
// said over there and not repeated here as if it were new.
final class ChronoTextPart extends Part {

    private final TextStyle style;

    ChronoTextPart(TextStyle style) {
        this.style = style;
    }

    boolean print(PrintContext ctx, StringBuilder out) {
        Chronology c = ctx.query(TemporalQueries.chronology());
        if (c == null) {
            return ctx.missingOrThrow("Chronology");
        }
        out.append(c.getDisplayName(this.style, ctx.locale));
        return true;
    }

    // The longest name that fits wins, as in `appendChronologyId`: without that, a calendar whose
    // name is a prefix of another would take the read.
    int parse(ParseContext ctx, String text, int pos) {
        Chronology best = null;
        int bestLength = 0;
        Iterator<Chronology> it = Chronology.getAvailableChronologies().iterator();
        while (it.hasNext()) {
            Chronology c = it.next();
            String name = c.getDisplayName(this.style, ctx.locale);
            if (name.length() > bestLength
                    && text.regionMatches(!ctx.caseSensitive, pos, name, 0, name.length())) {
                best = c;
                bestLength = name.length();
            }
        }
        if (best == null) {
            return ~pos;
        }
        ctx.chronology = best;
        return pos + bestLength;
    }

    public String toString() {
        return "ChronologyText(" + this.style + ")";
    }
}

// The localized pattern lookup, kept apart from the public class so that its javadoc does not have
// to tell where it comes from.
//
// `FormatStyle`'s four styles are in the same order as `DateFormat`'s four constants --FULL, LONG,
// MEDIUM, SHORT-- so the ordinal would be enough. The table is written all the same, because
// relying on two foreign enums staying aligned is the kind of assumption that breaks quietly.
final class LocalizedPattern {

    private static final int[] STYLES = {
        DateFormat.FULL,
        DateFormat.LONG,
        DateFormat.MEDIUM,
        DateFormat.SHORT,
    };

    private LocalizedPattern() {
    }

    static String of(FormatStyle dateStyle, FormatStyle timeStyle, Locale locale) {
        DateFormat df;
        if (dateStyle != null && timeStyle != null) {
            df = DateFormat.getDateTimeInstance(STYLES[dateStyle.ordinal()],
                    STYLES[timeStyle.ordinal()], locale);
        } else if (dateStyle != null) {
            df = DateFormat.getDateInstance(STYLES[dateStyle.ordinal()], locale);
        } else {
            df = DateFormat.getTimeInstance(STYLES[timeStyle.ordinal()], locale);
        }
        return ((SimpleDateFormat) df).toPattern();
    }
}
