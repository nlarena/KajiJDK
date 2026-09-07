package java.time.format;

import java.time.DateTimeException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

// A field written with its name and not with digits: `July`, `Monday`, `PM`.
//
// **THE WALL, AND WHERE IT WAS DECIDED TO PUT IT.** This library does not ship the CLDR's text
// data: there is no table of month names per language, and there will not be one by inventing it.
// All there is is the **English** set, written by hand below, which is real, checkable data and not
// filler.
//
// That is where the rule governing this part comes from: if the effective locale is English --or
// `ROOT`, which in CLDR *is* the set English derives from-- it writes and reads those names, which
// is correct. With any other locale it **throws**, saying the data is missing.
//
// What it does not do: a `DateTimeException` saying "I do not have the names in French" is true
// information. Writing `July` under `Locale.FRENCH` would be an answer with the shape of the right
// one and the wrong content --exactly what this project does not allow-- and a silent one at that:
// the caller would find out in production, not here. The alternative of leaving `appendText` out
// altogether would also cost the `MMM`/`EEEE` patterns in English, which **do** work.
//
// Only this part depends on the locale. A pattern with no names --`yyyy-MM-dd`, every `ISO_*`
// except `RFC_1123_DATE_TIME`-- gives the same text in any locale, and that is why `withLocale` is
// still an honest operation: it keeps the locale, gives it back unchanged, and changes nothing that
// should not change.
final class TextPart extends Part {

    private final TemporalField field;
    private final TextStyle style;

    TextPart(TemporalField field, TextStyle style) {
        this.field = field;
        this.style = style;
        if (EnglishText.names(field, style) == null) {
            throw new IllegalArgumentException("Field cannot be printed as text: " + field);
        }
    }

    // ROOT and any variant of English. `Locale.ROOT` counts because it is the "no language" locale,
    // and CLDR's root data is precisely these names.
    static boolean hasText(Locale locale) {
        if (locale == null) {
            return true;
        }
        String language = locale.getLanguage();
        return language.length() == 0 || language.equals("en");
    }

    private void requireData(Locale locale) {
        if (!TextPart.hasText(locale)) {
            throw new DateTimeException("No text data available for locale " + locale
                    + ": this library ships the English/root names only, and has no CLDR text data."
                    + " Use a numeric pattern, or appendText(field, Map) with your own names.");
        }
    }

    boolean print(PrintContext ctx, StringBuilder out) {
        Long v = ctx.value(this.field);
        if (v == null) {
            return false;
        }
        this.requireData(ctx.locale);
        String[] names = EnglishText.names(this.field, this.style);
        int i = EnglishText.index(this.field, v.longValue());
        if (i < 0 || i >= names.length) {
            throw new DateTimeException("Value " + v + " is out of range for field " + this.field);
        }
        out.append(names[i]);
        return true;
    }

    int parse(ParseContext ctx, String text, int pos) {
        this.requireData(ctx.locale);
        String[] names = EnglishText.names(this.field, this.style);
        // The **longest** is tried first. Without that, `EEEE` reading `Saturday` would settle for
        // `Sat` --which also fits-- and leave `urday` unconsumed.
        int best = -1;
        int bestLength = -1;
        int k = 0;
        while (k < names.length) {
            String n = names[k];
            if (n.length() > bestLength
                    && text.regionMatches(!ctx.caseSensitive, pos, n, 0, n.length())) {
                best = k;
                bestLength = n.length();
            }
            k = k + 1;
        }
        if (best < 0) {
            return ~pos;
        }
        ctx.put(this.field, EnglishText.value(this.field, best));
        return pos + bestLength;
    }
}

// `appendText(field, Map)`: the names come from the caller.
//
// This part **depends on no locale data** --the map is the data-- and that is why it is the honest
// way out for whoever needs names in another language: `RFC_1123_DATE_TIME` uses it, because the
// RFC fixes English names that are not "the locale's English" but part of the format.
final class MapTextPart extends Part {

    private final TemporalField field;
    private final Map<Long, String> names;

    MapTextPart(TemporalField field, Map<Long, String> names) {
        this.field = field;
        this.names = names;
    }

    boolean print(PrintContext ctx, StringBuilder out) {
        Long v = ctx.value(this.field);
        if (v == null) {
            return false;
        }
        String n = this.names.get(v);
        if (n == null) {
            throw new DateTimeException("Value " + v + " has no text in the supplied map for field "
                    + this.field);
        }
        out.append(n);
        return true;
    }

    int parse(ParseContext ctx, String text, int pos) {
        Long best = null;
        int bestLength = -1;
        Iterator<Map.Entry<Long, String>> it = this.names.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, String> e = it.next();
            String n = e.getValue();
            if (n.length() > bestLength
                    && text.regionMatches(!ctx.caseSensitive, pos, n, 0, n.length())) {
                best = e.getKey();
                bestLength = n.length();
            }
        }
        if (best == null) {
            return ~pos;
        }
        ctx.put(this.field, best.longValue());
        return pos + bestLength;
    }
}

// The English names, which are all the text data this library has.
//
// They are written down and not derived: the `NARROW` forms are English CLDR's --the initial-- and
// not an invention. That they are ambiguous while reading (`J` is January, June and July) is a
// property of the data, not a defect of this implementation; it is resolved by first match, just
// like the JDK.
final class EnglishText {

    static final String[] MONTHS = {"January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"};
    static final String[] MONTHS_SHORT = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    static final String[] MONTHS_NARROW = {"J", "F", "M", "A", "M", "J",
        "J", "A", "S", "O", "N", "D"};
    static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
        "Saturday", "Sunday"};
    static final String[] DAYS_SHORT = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    static final String[] DAYS_NARROW = {"M", "T", "W", "T", "F", "S", "S"};
    static final String[] MERIDIEM = {"AM", "PM"};
    static final String[] MERIDIEM_NARROW = {"a", "p"};
    static final String[] ERAS = {"Before Christ", "Anno Domini"};
    static final String[] ERAS_SHORT = {"BC", "AD"};
    static final String[] ERAS_NARROW = {"B", "A"};

    private EnglishText() {
    }

    // `null` --and not an exception-- because `TextPart`'s constructor uses it as a test: if a
    // field has no names, the `append` fails at the place where it was written, not while
    // formatting.
    static String[] names(TemporalField field, TextStyle style) {
        boolean short_ = style == TextStyle.SHORT || style == TextStyle.SHORT_STANDALONE;
        boolean narrow = style == TextStyle.NARROW || style == TextStyle.NARROW_STANDALONE;
        if (field == ChronoField.MONTH_OF_YEAR) {
            return narrow ? MONTHS_NARROW : (short_ ? MONTHS_SHORT : MONTHS);
        }
        if (field == ChronoField.DAY_OF_WEEK) {
            return narrow ? DAYS_NARROW : (short_ ? DAYS_SHORT : DAYS);
        }
        if (field == ChronoField.AMPM_OF_DAY) {
            return narrow ? MERIDIEM_NARROW : MERIDIEM;
        }
        if (field == ChronoField.ERA) {
            return narrow ? ERAS_NARROW : (short_ ? ERAS_SHORT : ERAS);
        }
        return null;
    }

    // From field value to array index. Month and day-of-week start at 1; AM/PM and era at 0.
    static int index(TemporalField field, long value) {
        if (field == ChronoField.MONTH_OF_YEAR || field == ChronoField.DAY_OF_WEEK) {
            return (int) value - 1;
        }
        return (int) value;
    }

    static long value(TemporalField field, int index) {
        if (field == ChronoField.MONTH_OF_YEAR || field == ChronoField.DAY_OF_WEEK) {
            return (long) (index + 1);
        }
        return (long) index;
    }
}
