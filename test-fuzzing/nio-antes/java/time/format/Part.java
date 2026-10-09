package java.time.format;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.Chronology;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// The engine of `java.time.format`: a part knows how to **write** a piece of text and how to
// **read** it.
//
// Why this class exists, and not a pattern string. The earlier version of `DateTimeFormatter` kept
// the literal pattern (`"yyyy-MM-dd"`) and interpreted it on every call. That was enough for
// `ofPattern`, but not for the rest of the package: an **optional section** --the `.SSS` of
// `ISO_LOCAL_TIME`, which only comes out when there are nanos-- cannot be written in a pattern
// string, and without optional sections there is not a single real `ISO_*`. Neither does
// `padNext`'s padding fit, nor an `appendText` with a map of names the caller chooses.
//
// So the pattern became **a compiler to parts** (`DateTimeFormatterBuilder.appendPattern`) instead
// of the representation. `ofPattern("yyyy-MM-dd")` gives exactly the same list of parts as writing
// the three `appendValue` calls by hand, and that is why the two halves of the package cannot drift
// apart.
//
// **The contract of the two methods, which is where all the subtlety lives:**
//
//   - `print` returns `false` when the field it needs is not there and **we are inside an optional
//     section**. The whole section is discarded. Outside an optional section a missing field is an
//     error and `ctx.value` throws: a formatter that swallows a missing field would write an
//     incomplete date without saying so.
//
//   - `parse` returns the index just past the text consumed, or **the ones' complement of the index
//     of the error** (`~i`) when it does not fit. The complement and not `-1` because *where* it
//     failed has to be known: `DateTimeParseException.getErrorIndex()` publishes it, and an
//     optional section that backs out needs to tell "there was nothing" from "there was something
//     wrong". Since `~i` is negative for every `i >= 0`, the check is still `if (p < 0)`.
abstract class Part {

    abstract boolean print(PrintContext ctx, StringBuilder out);

    abstract int parse(ParseContext ctx, String text, int pos);

    // The value of the digit in `c`, or -1 when it is not a digit. It goes through `DecimalStyle`
    // and not through plain `'0'` because the style may move zero to another point of Unicode.
    static int digit(ParseContext ctx, char c) {
        int d = c - ctx.symbols.getZeroDigit();
        if (d < 0 || d > 9) {
            return -1;
        }
        return d;
    }

    // The digits of `value` (which arrives already unsigned) with the zero the style asks for.
    static void writeDigits(PrintContext ctx, StringBuilder out, String digits, int minimum) {
        char zero = ctx.symbols.getZeroDigit();
        int missing = minimum - digits.length();
        while (missing > 0) {
            out.append(zero);
            missing = missing - 1;
        }
        int i = 0;
        while (i < digits.length()) {
            out.append((char) (zero + (digits.charAt(i) - '0')));
            i = i + 1;
        }
    }
}

// What a part needs to know in order to write: where it takes the values from, with which symbols,
// and whether or not it is inside an optional section.
final class PrintContext {

    private final TemporalAccessor temporal;
    final Locale locale;
    final DecimalStyle symbols;
    private int optional;

    PrintContext(TemporalAccessor temporal, Locale locale, DecimalStyle symbols) {
        this.temporal = temporal;
        this.locale = locale;
        this.symbols = symbols;
        this.optional = 0;
    }

    TemporalAccessor temporal() {
        return this.temporal;
    }

    void enterOptional() {
        this.optional = this.optional + 1;
    }

    void leaveOptional() {
        this.optional = this.optional - 1;
    }

    boolean inOptional() {
        return this.optional > 0;
    }

    // What `value` does, for the parts that take their datum not from a field but from a query
    // --the zone, the calendar--: inside an optional section the absence is tolerated; outside it
    // is an error, because the text that would come out is a date with a piece missing.
    boolean missingOrThrow(String what) {
        if (this.optional > 0) {
            return false;
        }
        throw new DateTimeException("Unable to extract " + what + " from temporal " + this.temporal);
    }

    // `null` means "the field is not there and we are in an optional section, discard it". Outside
    // an optional section no `null` is possible: it is an error and it throws.
    Long value(TemporalField field) {
        if (this.temporal.isSupported(field)) {
            return Long.valueOf(this.temporal.getLong(field));
        }
        if (this.optional > 0) {
            return null;
        }
        throw new DateTimeException("Unsupported field: " + field);
    }

    <R> R query(TemporalQuery<R> query) {
        return this.temporal.query(query);
    }
}

// What gets collected while reading. It is mutable on purpose: the parts deposit fields here, and
// the optional sections save it and restore it when they back out.
final class ParseContext {

    Map<TemporalField, Long> fields;
    ZoneId zone;
    ZoneOffset offset;
    Chronology chronology;
    boolean caseSensitive;
    boolean strict;
    final Locale locale;
    final DecimalStyle symbols;
    // The two cases the text may carry that **no time can represent**: `24:00`, which is the next
    // day's midnight, and `:60`, the leap second. They are marked here instead of being forced into
    // a `LocalTime` --which does not admit them-- and published through `parsedExcessDays` and
    // `parsedLeapSecond`, which is exactly what the JDK has them for.
    boolean excessDay;
    boolean leapSecond;

    ParseContext(Locale locale, DecimalStyle symbols, boolean strict, Chronology chronology,
            ZoneId zone) {
        this.fields = new HashMap<TemporalField, Long>();
        this.caseSensitive = true;
        this.strict = strict;
        this.locale = locale;
        this.symbols = symbols;
        this.chronology = chronology;
        this.zone = zone;
        this.excessDay = false;
        this.leapSecond = false;
    }

    void put(TemporalField field, long value) {
        this.fields.put(field, Long.valueOf(value));
    }

    ParseState save() {
        ParseState e = new ParseState();
        e.fields = new HashMap<TemporalField, Long>(this.fields);
        e.zone = this.zone;
        e.offset = this.offset;
        e.chronology = this.chronology;
        e.caseSensitive = this.caseSensitive;
        e.strict = this.strict;
        e.excessDay = this.excessDay;
        e.leapSecond = this.leapSecond;
        return e;
    }

    void restore(ParseState e) {
        this.fields = e.fields;
        this.zone = e.zone;
        this.offset = e.offset;
        this.chronology = e.chronology;
        this.caseSensitive = e.caseSensitive;
        this.strict = e.strict;
        this.excessDay = e.excessDay;
        this.leapSecond = e.leapSecond;
    }
}

// The snapshot of `ParseContext` an optional section takes before trying.
final class ParseState {

    Map<TemporalField, Long> fields;
    ZoneId zone;
    ZoneOffset offset;
    Chronology chronology;
    boolean caseSensitive;
    boolean strict;
    boolean excessDay;
    boolean leapSecond;
}

// Fixed text: the `-` of `2024-02-29`, the `T` of `2024-02-29T10:15`.
final class LiteralPart extends Part {

    private final String text;

    LiteralPart(String text) {
        this.text = text;
    }

    boolean print(PrintContext ctx, StringBuilder out) {
        out.append(this.text);
        return true;
    }

    int parse(ParseContext ctx, String text, int pos) {
        int length = this.text.length();
        if (pos + length > text.length()) {
            return ~pos;
        }
        if (!text.regionMatches(!ctx.caseSensitive, pos, this.text, 0, length)) {
            return ~pos;
        }
        return pos + length;
    }

    public String toString() {
        return "'" + this.text + "'";
    }
}

// A list of parts, and --when it is optional-- the one that gets discarded whole when something is
// missing.
//
// **All or nothing**, in both directions, and it is what makes `ISO_LOCAL_TIME` work: `:30.5` is
// written when there are seconds, and when there are none not even the `:` is written; and while
// reading, a text that brings `:30` but stops before the fraction does not leave the `.` half
// consumed, it backs up to where the section started.
final class CompositePart extends Part {

    private final Part[] parts;
    private final boolean optional;

    CompositePart(Part[] parts, boolean optional) {
        this.parts = parts;
        this.optional = optional;
    }

    Part[] parts() {
        return this.parts;
    }

    boolean isOptional() {
        return this.optional;
    }

    boolean print(PrintContext ctx, StringBuilder out) {
        int mark = out.length();
        if (this.optional) {
            ctx.enterOptional();
        }
        boolean whole = true;
        int i = 0;
        while (i < this.parts.length) {
            if (!this.parts[i].print(ctx, out)) {
                whole = false;
                i = this.parts.length;
            } else {
                i = i + 1;
            }
        }
        if (this.optional) {
            ctx.leaveOptional();
        }
        if (!whole) {
            out.setLength(mark);
            // If this section was optional the gap is legitimate and things carry on; if it was
            // not, the gap travels up and the optional section further out handles it --or, if
            // there is none, `value` would already have thrown before getting here.
            return this.optional;
        }
        return true;
    }

    int parse(ParseContext ctx, String text, int pos) {
        if (!this.optional) {
            int p = pos;
            int i = 0;
            while (i < this.parts.length) {
                p = this.parts[i].parse(ctx, text, p);
                if (p < 0) {
                    return p;
                }
                i = i + 1;
            }
            return p;
        }
        ParseState saved = ctx.save();
        int p = pos;
        int i = 0;
        while (i < this.parts.length) {
            p = this.parts[i].parse(ctx, text, p);
            if (p < 0) {
                // A full back-out: the fields the section managed to deposit go too. Without this
                // an `ISO_DATE_TIME` reading `2024-01-01T00:00+05:00[Bad/Zone]` would keep the
                // offset of a section that did not fit.
                ctx.restore(saved);
                return pos;
            }
            i = i + 1;
        }
        return p;
    }
}

// `padNext`: pads on the left whatever the part inside writes, up to `width`.
//
// It is the one part that **cannot be a pattern string**: the padding depends on the length of what
// came out, which is not known until it came out.
final class PadPart extends Part {

    private final Part part;
    private final int width;
    private final char pad;

    PadPart(Part part, int width, char pad) {
        this.part = part;
        this.width = width;
        this.pad = pad;
    }

    boolean print(PrintContext ctx, StringBuilder out) {
        int mark = out.length();
        if (!this.part.print(ctx, out)) {
            return false;
        }
        int written = out.length() - mark;
        if (written > this.width) {
            throw new DateTimeException("Cannot print as output of " + written
                    + " characters exceeds pad width of " + this.width);
        }
        int missing = this.width - written;
        while (missing > 0) {
            out.insert(mark, this.pad);
            missing = missing - 1;
        }
        return true;
    }

    int parse(ParseContext ctx, String text, int pos) {
        // While reading, the padding is skipped and the part inside sees **only** its slice of
        // `width` characters. Bounding the text is what keeps an `appendValue` of variable width
        // from eating the digits of the next field.
        boolean strict = ctx.strict;
        int p = pos;
        int end = pos + this.width;
        if (end > text.length()) {
            if (strict) {
                return ~pos;
            }
            end = text.length();
        }
        while (p < end && text.charAt(p) == this.pad) {
            p = p + 1;
        }
        String slice = text.substring(0, end);
        int r = this.part.parse(ctx, slice, p);
        if (r < 0) {
            return r;
        }
        if (r != end && strict) {
            return ~r;
        }
        return r;
    }
}

// The parsing switches --`parseCaseInsensitive`, `parseLenient` and their opposites. They write
// nothing; their only effect is on the reading context, and that is why they are one more part and
// not a field of the formatter: they hold **from where they appear to the end**, which is what the
// JDK says.
final class SettingsPart extends Part {

    static final int CASE_SENSITIVE = 0;
    static final int CASE_INSENSITIVE = 1;
    static final int STRICT = 2;
    static final int LENIENT = 3;

    private final int which;

    SettingsPart(int which) {
        this.which = which;
    }

    boolean print(PrintContext ctx, StringBuilder out) {
        return true;
    }

    int parse(ParseContext ctx, String text, int pos) {
        if (this.which == CASE_SENSITIVE) {
            ctx.caseSensitive = true;
        } else if (this.which == CASE_INSENSITIVE) {
            ctx.caseSensitive = false;
        } else if (this.which == STRICT) {
            ctx.strict = true;
        } else {
            ctx.strict = false;
        }
        return pos;
    }
}

// `parseDefaulting`: the value to use **if the text did not bring it**.
//
// It writes nothing, and while reading it consumes nothing: it only fills a gap. It is what lets a
// pattern of `"yyyy-MM"` give a `LocalDate` --with `parseDefaulting(DAY_OF_MONTH, 1)`-- without the
// pattern lying about having read a day.
final class DefaultPart extends Part {

    private final TemporalField field;
    private final long value;

    DefaultPart(TemporalField field, long value) {
        this.field = field;
        this.value = value;
    }

    boolean print(PrintContext ctx, StringBuilder out) {
        return true;
    }

    int parse(ParseContext ctx, String text, int pos) {
        if (!ctx.fields.containsKey(this.field)) {
            ctx.put(this.field, this.value);
        }
        return pos;
    }
}
