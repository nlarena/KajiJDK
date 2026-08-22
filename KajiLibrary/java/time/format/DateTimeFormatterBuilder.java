package java.time.format;

import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;

// KajiLibrary's java.time.format.DateTimeFormatterBuilder — the composer behind
// DateTimeFormatter.ofPattern: instead of writing "yyyy-MM-dd" you name the pieces, and the
// builder assembles the formatter.
//
// HOW THIS ONE WORKS, AND WHY IT MATTERS: the JDK's builder accumulates a list of printer/parser
// objects and hands them to a formatter that walks that list. Our DateTimeFormatter is simpler —
// it holds a PATTERN STRING and interprets it — so this builder composes the equivalent pattern
// and calls ofPattern. The result is a real, working formatter for every piece it accepts; it is
// not a shell.
//
// The price of that choice is an honest, narrow surface. Only what our pattern language can
// express is offered; the other 39 members of the JDK's builder are OMITTED rather than shipped as
// no-ops, and they fall into three groups:
//
//   - needs CLDR data: appendLocalized, appendZoneText, appendGenericZoneText, appendChronologyText,
//     appendDayPeriodText, getLocalizedDateTimePattern, toFormatter(Locale), and the TextStyle
//     overloads (our formatter prints one hardcoded English set, so a style argument would be a lie).
//   - needs a parser: parseCaseSensitive/Insensitive, parseStrict/Lenient, parseDefaulting,
//     appendOptional. Our formatter only prints.
//   - needs the list-of-pieces model rather than a pattern: padNext, optionalStart/optionalEnd,
//     appendValueReduced, appendFraction, appendInstant, appendValue(field, min, max, SignStyle),
//     appendText(field, Map). A pattern string cannot carry padding or optional sections.
//
// Widening this is a DateTimeFormatter refactor (pattern → composed pieces), not more builder code.
public final class DateTimeFormatterBuilder {

    private final StringBuilder pattern;

    public DateTimeFormatterBuilder() {
        this.pattern = new StringBuilder();
    }

    // A variable-width number: one pattern letter, so no zero padding.
    public DateTimeFormatterBuilder appendValue(TemporalField field) {
        return this.appendValue(field, 1);
    }

    // A zero-padded number of exactly `width` digits — "MM" for a two-digit month.
    public DateTimeFormatterBuilder appendValue(TemporalField field, int width) {
        if (width < 1 || width > 19) {
            throw new IllegalArgumentException("The width must be from 1 to 19 inclusive");
        }
        char letter = PatternLetter.of(field);
        int i = 0;
        while (i < width) {
            this.pattern.append(letter);
            i = i + 1;
        }
        return this;
    }

    // The field as text rather than digits ("January", "Monday"): four letters is the full form in
    // the pattern language. Only the fields our formatter can spell as text are accepted.
    public DateTimeFormatterBuilder appendText(TemporalField field) {
        char letter = PatternLetter.of(field);
        if (letter != 'M' && letter != 'E') {
            throw new IllegalArgumentException("Field cannot be printed as text: " + field);
        }
        this.pattern.append(letter);
        this.pattern.append(letter);
        this.pattern.append(letter);
        this.pattern.append(letter);
        return this;
    }

    public DateTimeFormatterBuilder appendLiteral(char literal) {
        return this.appendLiteral(String.valueOf(literal));
    }

    // Literal text has to survive the pattern interpreter, which reads bare letters as fields. The
    // pattern language quotes with `'…'`, and a literal quote is written `''`.
    public DateTimeFormatterBuilder appendLiteral(String literal) {
        if (literal.length() > 0) {
            this.pattern.append('\'');
            int i = 0;
            while (i < literal.length()) {
                char c = literal.charAt(i);
                if (c == '\'') {
                    this.pattern.append('\'');
                }
                this.pattern.append(c);
                i = i + 1;
            }
            this.pattern.append('\'');
        }
        return this;
    }

    public DateTimeFormatterBuilder appendPattern(String pattern) {
        this.pattern.append(pattern);
        return this;
    }

    // Composition: another formatter's pattern becomes part of this one.
    public DateTimeFormatterBuilder append(DateTimeFormatter formatter) {
        this.pattern.append(formatter.pattern());
        return this;
    }

    public DateTimeFormatter toFormatter() {
        return DateTimeFormatter.ofPattern(this.pattern.toString());
    }
}

// TemporalField → pattern letter.
//
// The comparisons go through `ChronoField.valueOf("…")` rather than `ChronoField.YEAR`: reading a
// static field of a classpath class emits `getfield` and traps at runtime (finding #110), while a
// static METHOD call resolves correctly. `valueOf` returns the same enum instance, so identity
// still holds. Revert these to plain constants once #110 is fixed.
final class PatternLetter {

    private PatternLetter() {
    }

    static char of(TemporalField field) {
        char letter = ' ';
        if (field == ChronoField.valueOf("YEAR")) {
            letter = 'u';
        } else if (field == ChronoField.valueOf("MONTH_OF_YEAR")) {
            letter = 'M';
        } else if (field == ChronoField.valueOf("DAY_OF_MONTH")) {
            letter = 'd';
        } else if (field == ChronoField.valueOf("DAY_OF_YEAR")) {
            letter = 'D';
        } else if (field == ChronoField.valueOf("DAY_OF_WEEK")) {
            letter = 'E';
        } else if (field == ChronoField.valueOf("HOUR_OF_DAY")) {
            letter = 'H';
        } else if (field == ChronoField.valueOf("MINUTE_OF_HOUR")) {
            letter = 'm';
        } else if (field == ChronoField.valueOf("SECOND_OF_MINUTE")) {
            letter = 's';
        } else if (field == ChronoField.valueOf("NANO_OF_SECOND")) {
            letter = 'S';
        } else {
            throw new IllegalArgumentException("Unsupported field: " + field);
        }
        return letter;
    }
}
