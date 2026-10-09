package java.time.format;

import java.time.chrono.ChronoLocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// The `DateTimeFormatter` builder: the pieces are named and it builds the formatter.
//
// **This is the real model, and `ofPattern` is sugar over it.** The earlier version was the other
// way round: the formatter kept a pattern string and the builder composed it. That put a hard
// ceiling on things --whatever a pattern string cannot say, the builder could not offer-- and left
// out the optional sections, the padding and hand-supplied names. Now `appendPattern` compiles the
// string into the **same parts** the `append*` methods produce, so the two entry points cannot
// drift apart and the ceiling is gone.
//
// **Nothing is missing any more, and the road taken to close it was the same one three times: bring
// the data, do not invent it.** Eight members were left out because they asked for CLDR *text*
// tables --names, not formats-- and an `appendZoneText(FULL)` writing `Europe/Paris` is not
// incomplete, it is wrong: it says "this is the name" and it is not. This is how each one was
// closed:
//
//   - `appendLocalized(FormatStyle, FormatStyle)`, `getLocalizedDateTimePattern` in its style form
//     and the four `ofLocalized*` of `DateTimeFormatter`: the pattern of a short date --`M/d/yy`
//     here, `dd.MM.yy` there-- turned up in `java.text.LocalePatterns`. See `LocalizedPart`, which
//     holds the measurement showing that the two routes give the same pattern.
//   - `ofLocalizedPattern(String)`, `getLocalizedDateTimePattern(String, ...)` and
//     `appendLocalized(String)`: the list of formats available in each locale now lives in
//     `LocaleTemplates`, extracted by running the JDK over the nearly forty-eight thousand
//     templates the grammar admits. What is not in the table is not invented: it gives
//     `DateTimeException`, the same as over there.
//   - `appendDayPeriodText`: each language's names and cut points are in `DayPeriods`, extracted by
//     asking the JDK for the 1440 minutes of the day in each locale.
//   - `appendZoneText` and `appendGenericZoneText` (two forms each): see `ZoneTextPart`. No table
//     was needed, and the reason is uncomfortable: `ZoneId.of` builds no named zone in this
//     library, so the only one a formatter can receive is a `ZoneOffset`, and for an offset the JDK
//     writes the identifier as it stands. **That is complete for what the library can represent,
//     not for what the API promises**: the day there is a zone database, those four need the CLDR
//     table and today they would be left lying.
//
// `appendText(TemporalField, Map)` remains the way out for whoever needs their own names: they
// supply them and the result is true by construction.

public final class DateTimeFormatterBuilder {

    private final DateTimeFormatterBuilder parent;
    private final boolean optional;
    private final List<Part> parts;
    // The section the `append` calls land in. It is `this` while no `optionalStart` is open; inside
    // one it points at the child builder. It lives only in the root builder --the one the caller
    // holds in their hand-- because every `append` returns the root.
    private DateTimeFormatterBuilder active;
    private int padWidth;
    private char padChar;

    public DateTimeFormatterBuilder() {
        this(null, false);
    }

    private DateTimeFormatterBuilder(DateTimeFormatterBuilder parent, boolean optional) {
        this.parent = parent;
        this.optional = optional;
        this.parts = new ArrayList<Part>();
        this.active = this;
        this.padWidth = 0;
        this.padChar = ' ';
    }

    private DateTimeFormatterBuilder add(Part part) {
        DateTimeFormatterBuilder a = this.active;
        Part p = part;
        if (a.padWidth > 0) {
            p = new PadPart(p, a.padWidth, a.padChar);
            a.padWidth = 0;
        }
        a.parts.add(p);
        return this;
    }

    // ---------------------------------------------------------------- numbers

    public DateTimeFormatterBuilder appendValue(TemporalField field) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        return this.add(new NumberPart(field, 1, 19, SignStyle.NORMAL));
    }

    public DateTimeFormatterBuilder appendValue(TemporalField field, int width) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (width < 1 || width > 19) {
            throw new IllegalArgumentException("The width must be from 1 to 19 inclusive but was "
                    + width);
        }
        return this.add(new NumberPart(field, width, width, SignStyle.NOT_NEGATIVE));
    }

    public DateTimeFormatterBuilder appendValue(TemporalField field, int minWidth, int maxWidth,
            SignStyle signStyle) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (signStyle == null) {
            throw new NullPointerException("signStyle");
        }
        if (minWidth < 1 || minWidth > 19) {
            throw new IllegalArgumentException("The minimum width must be from 1 to 19 inclusive but"
                    + " was " + minWidth);
        }
        if (maxWidth < 1 || maxWidth > 19) {
            throw new IllegalArgumentException("The maximum width must be from 1 to 19 inclusive but"
                    + " was " + maxWidth);
        }
        if (maxWidth < minWidth) {
            throw new IllegalArgumentException("The maximum width must exceed or equal the minimum"
                    + " width but " + maxWidth + " < " + minWidth);
        }
        return this.add(new NumberPart(field, minWidth, maxWidth, signStyle));
    }

    public DateTimeFormatterBuilder appendValueReduced(TemporalField field, int width, int maxWidth,
            int baseValue) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        return this.add(new ReducedNumberPart(field, width, maxWidth, baseValue, null));
    }

    public DateTimeFormatterBuilder appendValueReduced(TemporalField field, int width, int maxWidth,
            ChronoLocalDate baseDate) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (baseDate == null) {
            throw new NullPointerException("baseDate");
        }
        return this.add(new ReducedNumberPart(field, width, maxWidth, 0, baseDate));
    }

    public DateTimeFormatterBuilder appendFraction(TemporalField field, int minWidth, int maxWidth,
            boolean decimalPoint) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (minWidth < 0 || minWidth > 9) {
            throw new IllegalArgumentException("The minimum width must be from 0 to 9 inclusive but"
                    + " was " + minWidth);
        }
        if (maxWidth < 1 || maxWidth > 9) {
            throw new IllegalArgumentException("The maximum width must be from 1 to 9 inclusive but"
                    + " was " + maxWidth);
        }
        if (maxWidth < minWidth) {
            throw new IllegalArgumentException("The maximum width must exceed or equal the minimum"
                    + " width but " + maxWidth + " < " + minWidth);
        }
        return this.add(new FractionPart(field, minWidth, maxWidth, decimalPoint));
    }

    // ---------------------------------------------------------------- text

    public DateTimeFormatterBuilder appendText(TemporalField field) {
        return this.appendText(field, TextStyle.FULL);
    }

    // The names are the English ones --the only set this library has-- and under another locale
    // this part **throws** instead of writing them anyway. The why is in `TextPart`.
    public DateTimeFormatterBuilder appendText(TemporalField field, TextStyle textStyle) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (textStyle == null) {
            throw new NullPointerException("textStyle");
        }
        return this.add(new TextPart(field, textStyle));
    }

    // The form that depends on **no** locale data: the caller brings the names.
    public DateTimeFormatterBuilder appendText(TemporalField field, Map<Long, String> textLookup) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (textLookup == null) {
            throw new NullPointerException("textLookup");
        }
        return this.add(new MapTextPart(field,
                new HashMap<Long, String>(textLookup)));
    }

    // ---------------------------------------------------------------- literals

    public DateTimeFormatterBuilder appendLiteral(char literal) {
        return this.add(new LiteralPart(String.valueOf(literal)));
    }

    public DateTimeFormatterBuilder appendLiteral(String literal) {
        if (literal == null) {
            throw new NullPointerException("literal");
        }
        if (literal.length() > 0) {
            this.add(new LiteralPart(literal));
        }
        return this;
    }

    // ---------------------------------------------------------------- zone and calendar

    public DateTimeFormatterBuilder appendOffsetId() {
        return this.appendOffset("+HH:MM:ss", "Z");
    }

    public DateTimeFormatterBuilder appendOffset(String pattern, String noOffsetText) {
        if (pattern == null) {
            throw new NullPointerException("pattern");
        }
        if (noOffsetText == null) {
            throw new NullPointerException("noOffsetText");
        }
        return this.add(new OffsetPart(pattern, noOffsetText));
    }

    public DateTimeFormatterBuilder appendZoneId() {
        return this.add(new ZoneIdPart(ZoneIdPart.ZONE));
    }

    public DateTimeFormatterBuilder appendZoneOrOffsetId() {
        return this.add(new ZoneIdPart(ZoneIdPart.ZONE_OR_OFFSET));
    }

    public DateTimeFormatterBuilder appendZoneRegionId() {
        return this.add(new ZoneIdPart(ZoneIdPart.REGION));
    }

    public DateTimeFormatterBuilder appendChronologyId() {
        return this.add(new ChronoIdPart());
    }

    /**
     * The calendar's name.
     *
     * <p>Delegates to {@link java.time.chrono.Chronology#getDisplayName}, as the JDK does. That
     * library does not ship the translated names and always falls back to its reserve --the
     * calendar id--; for ISO it agrees with the JDK.
     *
     * @throws NullPointerException if `textStyle` is null
     */
    public DateTimeFormatterBuilder appendChronologyText(TextStyle textStyle) {
        if (textStyle == null) {
            throw new NullPointerException("textStyle");
        }
        return this.add(new ChronoTextPart(textStyle));
    }

    /**
     * A date, a time or both, asking for the fields by template.
     *
     * <p>A template --`yMMMd`, `Hm`-- says which fields are wanted and in how much detail, and lets
     * the language decide the order and the separators. It is what is needed when none of the four
     * styles will do: asking for the month and the day without the year, for instance.
     *
     * <p>The pattern is resolved when the formatter is used and not when it is built, so a later
     * {@link DateTimeFormatter#withLocale} changes the format.
     *
     * @param requestedTemplate the template
     * @return this builder
     * @throws NullPointerException if the template is null
     * @throws IllegalArgumentException if the template is malformed
     * @since 19
     */
    public DateTimeFormatterBuilder appendLocalized(String requestedTemplate) {
        if (requestedTemplate == null) {
            throw new NullPointerException("requestedTemplate");
        }
        Template.check(requestedTemplate);
        return this.add(new TemplatePart(requestedTemplate));
    }

    /**
     * The pattern that language uses for that template.
     *
     * <p>See {@code LocaleTemplates}, where the table comes from, and {@code Template}, which
     * explains why there are two different ways to fail.
     *
     * @param requestedTemplate the template
     * @param chrono the calendar; it is not used to resolve the template, but it cannot be null
     * @param locale in which language
     * @return the pattern
     * @throws NullPointerException if any of the three is null
     * @throws IllegalArgumentException if the template is malformed
     * @throws java.time.DateTimeException if that language has no pattern for that template
     * @since 19
     */
    public static String getLocalizedDateTimePattern(String requestedTemplate,
            java.time.chrono.Chronology chrono, Locale locale) {
        if (requestedTemplate == null) {
            throw new NullPointerException("requestedTemplate");
        }
        if (chrono == null) {
            throw new NullPointerException("chrono");
        }
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        Template.check(requestedTemplate);
        String pattern = LocaleTemplates.pattern(requestedTemplate, locale);
        if (pattern == null) {
            throw new java.time.DateTimeException("Requested template \"" + requestedTemplate
                    + "\" cannot be resolved in the locale \"" + locale + "\"");
        }
        return pattern;
    }

    /**
     * The period of the day in words: "in the morning", "nachmittags", "madrugada".
     *
     * <p>It is not AM/PM translated: each language splits the day into the pieces it names, which
     * are neither two nor even. See {@code DayPeriods}, where the data comes from.
     *
     * <p>While parsing it resolves to the midpoint of the stretch, which is what the JDK does: the
     * name of a period does not say what time it is.
     *
     * @param textStyle the style
     * @return this builder
     * @throws NullPointerException if `textStyle` is null
     * @since 16
     */
    public DateTimeFormatterBuilder appendDayPeriodText(TextStyle textStyle) {
        if (textStyle == null) {
            throw new NullPointerException("textStyle");
        }
        return this.add(new DayPeriodPart(textStyle));
    }

    /**
     * The zone's name, as it is written in that language.
     *
     * <p>See {@code ZoneTextPart} for why this can be fulfilled without the CLDR name table: the
     * only zone a formatter can receive in this library is a {@link java.time.ZoneOffset}, and for
     * an offset the JDK writes the identifier as it stands --`+05:30`, `Z`-- in any language and in
     * any style.
     *
     * @param textStyle the style
     * @return this builder
     * @throws NullPointerException if `textStyle` is null
     */
    public DateTimeFormatterBuilder appendZoneText(TextStyle textStyle) {
        if (textStyle == null) {
            throw new NullPointerException("textStyle");
        }
        return this.add(new ZoneTextPart(textStyle, null, false));
    }

    /**
     * The same, saying which zones to prefer when one name belongs to several.
     *
     * <p>The set changes nothing while there are no names: it only serves to break ties.
     *
     * @param textStyle the style
     * @param preferredZones the zones to prefer
     * @return this builder
     * @throws NullPointerException if either of the two is null
     */
    public DateTimeFormatterBuilder appendZoneText(TextStyle textStyle,
            java.util.Set<java.time.ZoneId> preferredZones) {
        if (textStyle == null) {
            throw new NullPointerException("textStyle");
        }
        if (preferredZones == null) {
            throw new NullPointerException("preferredZones");
        }
        return this.add(new ZoneTextPart(textStyle, preferredZones, false));
    }

    /**
     * The zone's name without telling summer time apart.
     *
     * <p>"Pacific Time" instead of "Pacific Standard Time": it is what gets written when there is
     * no concrete instant to decide whether summer time is in force.
     *
     * @param textStyle the style
     * @return this builder
     * @throws NullPointerException if `textStyle` is null
     */
    public DateTimeFormatterBuilder appendGenericZoneText(TextStyle textStyle) {
        if (textStyle == null) {
            throw new NullPointerException("textStyle");
        }
        return this.add(new ZoneTextPart(textStyle, null, true));
    }

    /**
     * The same, with preferred zones.
     *
     * <p>The set is not checked: the JDK does not check it in this form either, unlike
     * {@link #appendZoneText(TextStyle, java.util.Set)}. The asymmetry is theirs and it is copied.
     *
     * @param textStyle the style
     * @param preferredZones the zones to prefer
     * @return this builder
     * @throws NullPointerException if `textStyle` is null
     */
    public DateTimeFormatterBuilder appendGenericZoneText(TextStyle textStyle,
            java.util.Set<java.time.ZoneId> preferredZones) {
        if (textStyle == null) {
            throw new NullPointerException("textStyle");
        }
        return this.add(new ZoneTextPart(textStyle, preferredZones, true));
    }

    /**
     * The zone offset written with `GMT` in front: `GMT+08:00`, `GMT+8`, `GMT`.
     *
     * <p>It admits {@link TextStyle#FULL} and {@link TextStyle#SHORT} only. The word `GMT` is a
     * literal --in the JDK too, which has an unresolved `TODO` there-- so the two outputs agree in
     * any locale.
     *
     * @throws IllegalArgumentException if the style is neither FULL nor SHORT
     * @throws NullPointerException if `style` is null
     */
    public DateTimeFormatterBuilder appendLocalizedOffset(TextStyle style) {
        if (style == null) {
            throw new NullPointerException("style");
        }
        if (style != TextStyle.FULL && style != TextStyle.SHORT) {
            throw new IllegalArgumentException("Style must be either full or short");
        }
        return this.add(new LocalizedOffsetPart(style));
    }

    /**
     * A date, a time or both, with the format the locale uses for that style.
     *
     * <p>The pattern is resolved when the formatter is used and not when it is built, so a later
     * {@link DateTimeFormatter#withLocale} changes the format. See
     * {@link #getLocalizedDateTimePattern}.
     *
     * @throws IllegalArgumentException if both styles are null
     */
    public DateTimeFormatterBuilder appendLocalized(FormatStyle dateStyle, FormatStyle timeStyle) {
        if (dateStyle == null && timeStyle == null) {
            throw new IllegalArgumentException("Either the date or time style must be non-null");
        }
        return this.add(new LocalizedPart(dateStyle, timeStyle));
    }

    /**
     * The pattern that locale uses for a date and/or a time of that style.
     *
     * <p><strong>Where it comes from.</strong> From the same table as
     * {@code DateFormat.getDateInstance(style, locale)}, which this library does ship. It was
     * verified against the real JDK --four styles, three combinations, seven locales-- that the two
     * return exactly the same pattern; it is not an assumed equivalence.
     *
     * <p>The chronology is taken and required to be non-null, as in the JDK, but it does not change
     * the result: the only patterns there are are the ISO calendar's.
     *
     * <p><strong>A locale with no data of its own falls back to the nearest one there is</strong>,
     * which is how {@code java.text} behaves in this library: {@code en_GB} ends up returning
     * {@code en_US}'s patterns. Which ones have data of their own is told by
     * {@link DecimalStyle#getAvailableLocales}. It is not an invented answer --it comes out of a
     * real table-- but neither is it the requested locale's, and that is worth knowing before
     * believing it.
     *
     * @throws IllegalArgumentException if both styles are null
     * @throws NullPointerException if `chrono` or `locale` are null
     */
    public static String getLocalizedDateTimePattern(FormatStyle dateStyle, FormatStyle timeStyle,
            java.time.chrono.Chronology chrono, Locale locale) {
        if (chrono == null) {
            throw new NullPointerException("chrono");
        }
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        if (dateStyle == null && timeStyle == null) {
            throw new IllegalArgumentException("Either dateStyle or timeStyle must be non-null");
        }
        return LocalizedPattern.of(dateStyle, timeStyle, locale);
    }

    public DateTimeFormatterBuilder appendInstant() {
        return this.add(new InstantPart(-2));
    }

    public DateTimeFormatterBuilder appendInstant(int fractionalDigits) {
        if (fractionalDigits < -1 || fractionalDigits > 9) {
            throw new IllegalArgumentException("The fractional digits must be from -1 to 9 inclusive"
                    + " but was " + fractionalDigits);
        }
        return this.add(new InstantPart(fractionalDigits));
    }

    // ---------------------------------------------------------------- parse settings

    public DateTimeFormatterBuilder parseCaseSensitive() {
        return this.add(new SettingsPart(SettingsPart.CASE_SENSITIVE));
    }

    public DateTimeFormatterBuilder parseCaseInsensitive() {
        return this.add(new SettingsPart(SettingsPart.CASE_INSENSITIVE));
    }

    public DateTimeFormatterBuilder parseStrict() {
        return this.add(new SettingsPart(SettingsPart.STRICT));
    }

    public DateTimeFormatterBuilder parseLenient() {
        return this.add(new SettingsPart(SettingsPart.LENIENT));
    }

    public DateTimeFormatterBuilder parseDefaulting(TemporalField field, long value) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        return this.add(new DefaultPart(field, value));
    }

    // ---------------------------------------------------------------- padding

    public DateTimeFormatterBuilder padNext(int padWidth) {
        return this.padNext(padWidth, ' ');
    }

    // It affects **the next part and no other**. That is what the JDK says, and it is the
    // reasonable thing: the padding belongs to the field, not to the formatter.
    public DateTimeFormatterBuilder padNext(int padWidth, char padChar) {
        if (padWidth < 1) {
            throw new IllegalArgumentException("The pad width must be at least one but was "
                    + padWidth);
        }
        this.active.padWidth = padWidth;
        this.active.padChar = padChar;
        return this;
    }

    // ---------------------------------------------------------------- optional sections

    public DateTimeFormatterBuilder optionalStart() {
        this.active = new DateTimeFormatterBuilder(this.active, true);
        return this;
    }

    public DateTimeFormatterBuilder optionalEnd() {
        if (this.active.parent == null) {
            throw new IllegalStateException("Cannot call optionalEnd() as there was no previous call"
                    + " to optionalStart()");
        }
        DateTimeFormatterBuilder closed = this.active;
        this.active = closed.parent;
        if (closed.parts.size() > 0) {
            this.add(closed.composite());
        }
        return this;
    }

    // ---------------------------------------------------------------- composition

    public DateTimeFormatterBuilder append(DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        return this.add(formatter.parts());
    }

    public DateTimeFormatterBuilder appendOptional(DateTimeFormatter formatter) {
        if (formatter == null) {
            throw new NullPointerException("formatter");
        }
        return this.add(new CompositePart(new Part[] {formatter.parts()}, true));
    }

    private CompositePart composite() {
        Part[] a = new Part[this.parts.size()];
        int i = 0;
        while (i < a.length) {
            a[i] = this.parts.get(i);
            i = i + 1;
        }
        return new CompositePart(a, this.optional);
    }

    public DateTimeFormatter toFormatter() {
        // **Deliberate divergence**: the JDK uses the machine's default locale. Here it is
        // `Locale.ROOT`, because the only name set there is is the root's, and taking the machine's
        // locale would make `ofPattern("dd MMM yyyy")` throw on any box that is not in English
        // --over a data limitation that has nothing to do with what the caller asked for--.
        // `toFormatter(Locale)` lets the choice be made explicitly.
        return this.toFormatter(Locale.ROOT);
    }

    public DateTimeFormatter toFormatter(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        while (this.active.parent != null) {
            this.optionalEnd();
        }
        return new DateTimeFormatter(this.composite(), locale, DecimalStyle.STANDARD,
                ResolverStyle.SMART, null, null, null);
    }

    // ---------------------------------------------------------------- the pattern

    public DateTimeFormatterBuilder appendPattern(String pattern) {
        if (pattern == null) {
            throw new NullPointerException("pattern");
        }
        this.compile(pattern);
        return this;
    }

    // The pattern language's compiler. Each letter translates into the same parts the corresponding
    // `append` produces, and **the letters that would need CLDR are rejected with the reason**: an
    // error at the place where the pattern was written is preferable to a formatter that writes in
    // the wrong language.
    private void compile(String p) {
        int i = 0;
        while (i < p.length()) {
            char c = p.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                int start = i;
                while (i < p.length() && p.charAt(i) == c) {
                    i = i + 1;
                }
                this.letter(c, i - start, p);
            } else if (c == '\'') {
                i = i + 1;
                StringBuilder lit = new StringBuilder();
                boolean closed = false;
                while (i < p.length()) {
                    if (p.charAt(i) == '\'') {
                        if (i + 1 < p.length() && p.charAt(i + 1) == '\'') {
                            lit.append('\'');
                            i = i + 2;
                        } else {
                            i = i + 1;
                            closed = true;
                            break;
                        }
                    } else {
                        lit.append(p.charAt(i));
                        i = i + 1;
                    }
                }
                if (!closed) {
                    throw new IllegalArgumentException("Pattern ends with an incomplete string"
                            + " literal: " + p);
                }
                if (lit.length() == 0) {
                    // A lone `''` is a literal quote.
                    this.appendLiteral('\'');
                } else {
                    this.appendLiteral(lit.toString());
                }
            } else if (c == '[') {
                this.optionalStart();
                i = i + 1;
            } else if (c == ']') {
                this.optionalEnd();
                i = i + 1;
            } else if (c == '#' || c == '{' || c == '}') {
                throw new IllegalArgumentException("Pattern includes reserved character: '" + c
                        + "'");
            } else {
                this.appendLiteral(c);
                i = i + 1;
            }
        }
    }

    private static void noCldr(char c, String what) {
        throw new IllegalArgumentException("Pattern letter '" + c + "' needs " + what
                + ", which comes from CLDR locale data that this library does not ship."
                + " Use a numeric field, or appendText(field, Map) with your own names.");
    }

    private void letter(char c, int n, String pattern) {
        if (c == 'u' || c == 'y') {
            // `u` is the proleptic year and `y` the year of era. With two letters both are cut down
            // to the last two digits, with the 2000-2099 window.
            TemporalField field = c == 'u' ? ChronoField.YEAR : ChronoField.YEAR_OF_ERA;
            if (n == 2) {
                this.appendValueReduced(field, 2, 2, 2000);
            } else {
                this.appendValue(field, n, 10, n < 4 ? SignStyle.NORMAL : SignStyle.EXCEEDS_PAD);
            }
        } else if (c == 'M' || c == 'L') {
            if (n <= 2) {
                this.appendValue(ChronoField.MONTH_OF_YEAR, n, 2, SignStyle.NOT_NEGATIVE);
            } else {
                this.appendText(ChronoField.MONTH_OF_YEAR, style(n, c == 'L'));
            }
        } else if (c == 'd') {
            this.appendValue(ChronoField.DAY_OF_MONTH, n, 2, SignStyle.NOT_NEGATIVE);
        } else if (c == 'D') {
            this.appendValue(ChronoField.DAY_OF_YEAR, n, 3, SignStyle.NOT_NEGATIVE);
        } else if (c == 'g') {
            this.appendValue(JulianFields.MODIFIED_JULIAN_DAY, n, 19, SignStyle.NORMAL);
        } else if (c == 'E') {
            this.appendText(ChronoField.DAY_OF_WEEK, style(n, false));
        } else if (c == 'G') {
            this.appendText(ChronoField.ERA, style(n, false));
        } else if (c == 'a') {
            this.appendText(ChronoField.AMPM_OF_DAY, style(n, false));
        } else if (c == 'h') {
            this.appendValue(ChronoField.CLOCK_HOUR_OF_AMPM, n, 2, SignStyle.NOT_NEGATIVE);
        } else if (c == 'K') {
            this.appendValue(ChronoField.HOUR_OF_AMPM, n, 2, SignStyle.NOT_NEGATIVE);
        } else if (c == 'k') {
            this.appendValue(ChronoField.CLOCK_HOUR_OF_DAY, n, 2, SignStyle.NOT_NEGATIVE);
        } else if (c == 'H') {
            this.appendValue(ChronoField.HOUR_OF_DAY, n, 2, SignStyle.NOT_NEGATIVE);
        } else if (c == 'm') {
            this.appendValue(ChronoField.MINUTE_OF_HOUR, n, 2, SignStyle.NOT_NEGATIVE);
        } else if (c == 's') {
            this.appendValue(ChronoField.SECOND_OF_MINUTE, n, 2, SignStyle.NOT_NEGATIVE);
        } else if (c == 'S') {
            // `S` is the fraction, not the number: `.5` is half a second and not five nanos.
            this.appendFraction(ChronoField.NANO_OF_SECOND, n, n, false);
        } else if (c == 'A') {
            this.appendValue(ChronoField.MILLI_OF_DAY, n, 19, SignStyle.NOT_NEGATIVE);
        } else if (c == 'n') {
            this.appendValue(ChronoField.NANO_OF_SECOND, n, 19, SignStyle.NOT_NEGATIVE);
        } else if (c == 'N') {
            this.appendValue(ChronoField.NANO_OF_DAY, n, 19, SignStyle.NOT_NEGATIVE);
        } else if (c == 'Q') {
            if (n <= 2) {
                this.appendValue(IsoFields.QUARTER_OF_YEAR, n, 2, SignStyle.NOT_NEGATIVE);
            } else {
                noCldr(c, "quarter names");
            }
        } else if (c == 'V') {
            if (n != 2) {
                throw new IllegalArgumentException("Pattern letter count must be 2 for 'V': "
                        + pattern);
            }
            this.appendZoneId();
        } else if (c == 'v') {
            noCldr(c, "generic zone names");
        } else if (c == 'z') {
            noCldr(c, "zone names");
        } else if (c == 'O') {
            noCldr(c, "localized offset text (\"GMT+8\")");
        } else if (c == 'B') {
            noCldr(c, "day period text (\"in the morning\")");
        } else if (c == 'w' || c == 'W' || c == 'e' || c == 'c') {
            // The week of the year depends on which day the week starts on and on how many days the
            // first one has, and both change by region --CLDR data--. `IsoFields` gives the ISO
            // version, which is fixed: it is reached through
            // `appendValue(IsoFields.WEEK_OF_WEEK_BASED_YEAR, ...)`.
            noCldr(c, "locale week rules (first day of week, minimal days in first week)");
        } else if (c == 'p') {
            // The padding applies to whatever comes next, which is how the JDK defines it.
            this.padNext(n);
        } else if (c == 'x' || c == 'X' || c == 'Z') {
            this.patternOffset(c, n, pattern);
        } else {
            throw new IllegalArgumentException("Unknown pattern letter: " + c);
        }
    }

    private static TextStyle style(int n, boolean standalone) {
        TextStyle t;
        if (n == 5) {
            t = TextStyle.NARROW;
        } else if (n >= 4) {
            t = TextStyle.FULL;
        } else {
            t = TextStyle.SHORT;
        }
        return standalone ? t.asStandalone() : t;
    }

    private void patternOffset(char c, int n, String pattern) {
        String[] forms = {"+HHmm", "+HHMM", "+HH:MM", "+HHMMss", "+HH:MM:ss"};
        if (c == 'Z') {
            if (n <= 3) {
                this.appendOffset("+HHMM", "+0000");
            } else if (n == 4) {
                noCldr(c, "localized offset text (\"GMT+8\")");
            } else if (n == 5) {
                this.appendOffset("+HH:MM:ss", "Z");
            } else {
                throw new IllegalArgumentException("Too many pattern letters: " + c);
            }
            return;
        }
        if (n < 1 || n > 5) {
            throw new IllegalArgumentException("Too many pattern letters: " + c);
        }
        // `X` writes `Z` for zero; `x` writes it with digits. It is the only difference between the
        // two letters, and that is why they share the table of forms.
        this.appendOffset(forms[n - 1], c == 'X' ? "Z" : "");
    }
}
