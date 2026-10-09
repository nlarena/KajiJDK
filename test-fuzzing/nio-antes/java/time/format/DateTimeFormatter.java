package java.time.format;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// The `java.time` formatter: writes a temporal value as text and reads it back.
//
// **It formats and parses exactly the same language**, and that symmetry is deliberate: whatever
// this formatter writes, it reads back. There is no part that only knows one of the two.
//
// Inside there is no pattern string: there is a list of parts (`Part`), and `ofPattern` is a
// compiler into that list. The why is written in `Part` and in `DateTimeFormatterBuilder`; what
// matters from outside is the consequence, which is that the `ISO_*` here are the real ones --with
// their optional sections-- and not approximations.
//
// **THE ONE PLACE WHERE THE DATA IS MISSING, AND THE CRITERION.**
//
// `ofLocalizedDate`, `ofLocalizedTime`, both `ofLocalizedDateTime` and `ofLocalizedPattern` all ask
// for the same thing: **the pattern a locale uses** for a short, medium, long or full date. That
// pattern is not derived from anything, it is data: `M/d/yy` in the United States, `d/M/yy` in
// Argentina, `yyyy/MM/dd` in Japan, and for `FULL` with the day name in front as well. This library
// does not ship the CLDR data, and **there is no default pattern that is correct**: whichever one
// is chosen is right for one locale and lies for the other hundred-odd. An `ofLocalizedDate(SHORT)`
// returning `dd/MM/yyyy` would not be an incomplete version of the right answer, it would be a
// wrong answer with the shape of the right one, and the caller finds out in production.
//
// The distinction, which governs the whole package: **it is not that the data is missing, it is
// that making it up would be lying**. Where the data is missing but the answer does not depend on
// it, the member is here: the `ISO_*` are fixed by standard and do not look at the locale;
// `withLocale` keeps the locale and gives it back unchanged; `appendText(field, Map)` lets the
// caller supply the names. So these factories are here and none of them invents anything: the four
// by style resolve their pattern through `java.text.DateFormat`, and `ofLocalizedPattern` through
// `LocaleTemplates`, a table extracted by running the JDK over every template the grammar admits.
// What is not in the table is not made up: it throws `DateTimeException`, the same as over there.
//
// `localizedBy(Locale)` follows the same rule from the other side: it is not "the formatter with
// another locale" --that is `withLocale`-- but "the formatter with the locale's Unicode extensions
// applied": `u-ca` the calendar, `u-nu` the numbering system, `u-rg` the format region, `u-tz` the
// zone. Of the four, two are honoured here, and **the javadoc says which two and why the other two
// are not**, so the caller is never left believing all four were applied.
//
// **What does depend on the locale is a single part**: the one with the names (`MMM`, `EEEE`, `a`,
// `G`). With `Locale.ROOT` or English it writes the English names, which is correct; with any other
// it **throws** a `DateTimeException` saying the data is missing, instead of writing English under
// another flag. The full reasoning is in `TextPart`.
public final class DateTimeFormatter {

    // ---- the predefined ones --------------------------------------------------------------------
    //
    // The JDK's fifteen, built with the same builder anyone can use. None of them looks at the
    // locale: ISO-8601 and RFC 1123 fix the text, and there is nothing to translate there.
    // `RFC_1123` uses `appendText(field, Map)` with the RFC's names precisely for that reason: they
    // are not "the English names", they are the names the format demands, and an explicit map says
    // so.

    public static final DateTimeFormatter ISO_LOCAL_DATE = isoLocalDate();

    public static final DateTimeFormatter ISO_OFFSET_DATE = isoOffsetDate();

    public static final DateTimeFormatter ISO_DATE = isoDate();

    public static final DateTimeFormatter ISO_LOCAL_TIME = isoLocalTime();

    public static final DateTimeFormatter ISO_OFFSET_TIME = isoOffsetTime();

    public static final DateTimeFormatter ISO_TIME = isoTime();

    public static final DateTimeFormatter ISO_LOCAL_DATE_TIME = isoLocalDateTime();

    public static final DateTimeFormatter ISO_OFFSET_DATE_TIME = isoOffsetDateTime();

    public static final DateTimeFormatter ISO_ZONED_DATE_TIME = isoZonedDateTime();

    public static final DateTimeFormatter ISO_DATE_TIME = isoDateTime();

    public static final DateTimeFormatter ISO_ORDINAL_DATE = isoOrdinalDate();

    public static final DateTimeFormatter ISO_WEEK_DATE = isoWeekDate();

    public static final DateTimeFormatter ISO_INSTANT = isoInstant();

    public static final DateTimeFormatter BASIC_ISO_DATE = basicIsoDate();

    public static final DateTimeFormatter RFC_1123_DATE_TIME = rfc1123();

    private static final TemporalQuery<Period> EXCESS = new ExcessQuery();

    private static final TemporalQuery<Boolean> LEAP_SECOND = new LeapSecondQuery();

    private final CompositePart parts;
    private final Locale locale;
    private final DecimalStyle symbols;
    private final ResolverStyle resolverStyle;
    private final Set<TemporalField> resolverFields;
    private final Chronology chronology;
    private final ZoneId zone;

    DateTimeFormatter(CompositePart parts, Locale locale, DecimalStyle symbols,
            ResolverStyle resolverStyle, Set<TemporalField> resolverFields, Chronology chronology,
            ZoneId zone) {
        this.parts = parts;
        this.locale = locale;
        this.symbols = symbols;
        this.resolverStyle = resolverStyle;
        this.resolverFields = resolverFields;
        this.chronology = chronology;
        this.zone = zone;
    }

    CompositePart parts() {
        return this.parts;
    }

    // ---- factories ------------------------------------------------------------------------------

    public static DateTimeFormatter ofPattern(String pattern) {
        return new DateTimeFormatterBuilder().appendPattern(pattern).toFormatter();
    }

    // The locale is stored and given back as it stands; the only thing that changes with it are the
    // names, and those exist in English only. A pattern with no names gives the same text in any
    // locale.
    public static DateTimeFormatter ofPattern(String pattern, Locale locale) {
        return new DateTimeFormatterBuilder().appendPattern(pattern).toFormatter(locale);
    }

    // The four localized-format factories.
    //
    // **Here the machine's locale IS taken**, and not `Locale.ROOT` the way `toFormatter()` does.
    // It is not an inconsistency: what these four ask for *is* the locale's format, so ignoring it
    // would be failing to do what they say. `toFormatter()` picks ROOT because a hand-written
    // pattern speaks of no locale in particular. The JDK uses the same FORMAT category used here.
    //
    // The pattern comes from the locale; the **names** that pattern asks for --`MMMM`, `EEEE`--
    // still come from the only set there is, the English one. An `ofLocalizedDate(FULL)` under a
    // non-English locale builds fine and throws on use, saying which name is missing.
    private static DateTimeFormatter localized(FormatStyle date, FormatStyle time) {
        return new DateTimeFormatterBuilder().appendLocalized(date, time)
                .toFormatter(Locale.getDefault(Locale.Category.FORMAT))
                .withChronology(IsoChronology.INSTANCE);
    }

    /**
     * The format the machine's locale uses for that template.
     *
     * <p>A template --`yMMMd`, `Hm`-- says which fields are wanted and in how much detail, and lets
     * the language decide the order and the separators. It is what is needed when none of the four
     * styles will do; see {@link DateTimeFormatterBuilder#appendLocalized(String)}.
     *
     * @param requestedTemplate the template
     * @return the formatter
     * @throws NullPointerException if the template is null
     * @throws IllegalArgumentException if the template is malformed
     * @since 19
     */
    public static DateTimeFormatter ofLocalizedPattern(String requestedTemplate) {
        if (requestedTemplate == null) {
            throw new NullPointerException("requestedTemplate");
        }
        return new DateTimeFormatterBuilder().appendLocalized(requestedTemplate)
                .toFormatter(Locale.getDefault(Locale.Category.FORMAT));
    }

    /**
     * The date format of that style in the machine's locale.
     *
     * @throws NullPointerException if `dateStyle` is null
     */
    public static DateTimeFormatter ofLocalizedDate(FormatStyle dateStyle) {
        if (dateStyle == null) {
            throw new NullPointerException("dateStyle");
        }
        return localized(dateStyle, null);
    }

    /**
     * The time format of that style in the machine's locale.
     *
     * @throws NullPointerException if `timeStyle` is null
     */
    public static DateTimeFormatter ofLocalizedTime(FormatStyle timeStyle) {
        if (timeStyle == null) {
            throw new NullPointerException("timeStyle");
        }
        return localized(null, timeStyle);
    }

    /**
     * The date and time format of that style, the same one for both.
     *
     * @throws NullPointerException if `dateTimeStyle` is null
     */
    public static DateTimeFormatter ofLocalizedDateTime(FormatStyle dateTimeStyle) {
        if (dateTimeStyle == null) {
            throw new NullPointerException("dateTimeStyle");
        }
        return localized(dateTimeStyle, dateTimeStyle);
    }

    /**
     * The date and time format, with a style for each.
     *
     * @throws NullPointerException if either of the two is null
     */
    public static DateTimeFormatter ofLocalizedDateTime(FormatStyle dateStyle,
            FormatStyle timeStyle) {
        if (dateStyle == null) {
            throw new NullPointerException("dateStyle");
        }
        if (timeStyle == null) {
            throw new NullPointerException("timeStyle");
        }
        return localized(dateStyle, timeStyle);
    }

    // The two queries that only make sense over the result of a parse. They return **the same
    // instance every time**: they are compared by identity, and a new one on each call would never
    // match the one the result recognizes (it was a real bug in `TemporalQueries`).
    public static final TemporalQuery<Period> parsedExcessDays() {
        return EXCESS;
    }

    public static final TemporalQuery<Boolean> parsedLeapSecond() {
        return LEAP_SECOND;
    }

    // ---- copies with one setting changed --------------------------------------------------------

    public Locale getLocale() {
        return this.locale;
    }

    public DateTimeFormatter withLocale(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        if (locale.equals(this.locale)) {
            return this;
        }
        return new DateTimeFormatter(this.parts, locale, this.symbols, this.resolverStyle,
                this.resolverFields, this.chronology, this.zone);
    }

    /**
     * A copy with the locale set **and with the chronology and the symbols derived from it**.
     *
     * <p>That is what tells it apart from {@link #withLocale}, and the difference is not one of
     * nuance: `withLocale` changes the locale and **keeps** whatever was set by hand, while this
     * one **overwrites** it with what the locale says. A formatter with a Thai chronology set by
     * hand is still Thai after `withLocale(FRANCE)`, and becomes ISO after `localizedBy(FRANCE)`.
     *
     * <p>Where each thing comes from, verified against the real JDK and not assumed:
     *
     * <ul>
     * <li><strong>Chronology</strong>: from the locale's `u-ca` Unicode extension if it carries
     *     one, and otherwise from the calendar that locale uses by default --which is what
     *     {@link Chronology#ofLocale} does--. It never ends up `null`: `localizedBy` of an ordinary
     *     locale leaves ISO, not "no chronology".</li>
     * <li><strong>Symbols</strong>: {@link DecimalStyle#of}, which reads the `u-nu` extension.</li>
     * <li><strong>Zone</strong>: **the one that was there is kept**, unless the locale carries
     *     `u-tz`. It is not cleared the way the chronology is, and that is surprising until it is
     *     measured.</li>
     * </ul>
     *
     * <p><strong>The one approximation, stated:</strong> the `u-tz` extension is ignored. Its
     * values are short CLDR identifiers --`uslax` for `America/Los_Angeles`-- and the table
     * translating them is some four hundred and fifty rows of opaque data with no rule to derive
     * them. Inventing it from memory would give exactly what this package avoids everywhere:
     * plausible and wrong zone names. A locale with `u-tz` keeps the zone it had, which is the same
     * answer a locale with no extension gives.
     *
     * @throws NullPointerException if `locale` is null
     */
    public DateTimeFormatter localizedBy(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        Chronology chrono = Chronology.ofLocale(locale);
        DecimalStyle symbols = DecimalStyle.of(locale);
        return new DateTimeFormatter(this.parts, locale, symbols, this.resolverStyle,
                this.resolverFields, chrono, this.zone);
    }

    public DecimalStyle getDecimalStyle() {
        return this.symbols;
    }

    public DateTimeFormatter withDecimalStyle(DecimalStyle decimalStyle) {
        if (decimalStyle == null) {
            throw new NullPointerException("decimalStyle");
        }
        if (decimalStyle.equals(this.symbols)) {
            return this;
        }
        return new DateTimeFormatter(this.parts, this.locale, decimalStyle, this.resolverStyle,
                this.resolverFields, this.chronology, this.zone);
    }

    public Chronology getChronology() {
        return this.chronology;
    }

    public DateTimeFormatter withChronology(Chronology chrono) {
        if (chrono == null ? this.chronology == null : chrono.equals(this.chronology)) {
            return this;
        }
        return new DateTimeFormatter(this.parts, this.locale, this.symbols, this.resolverStyle,
                this.resolverFields, chrono, this.zone);
    }

    public ZoneId getZone() {
        return this.zone;
    }

    // When writing, it converts the value to this zone --if it carries an instant-- and otherwise
    // lends the zone to it: a `LocalDateTime` does not know where it is, and with `withZone` it
    // comes to know. When reading, it is the zone that applies when the text carried none, which is
    // what allows a `ZonedDateTime` to come out of text with no zone.
    public DateTimeFormatter withZone(ZoneId zone) {
        if (zone == null ? this.zone == null : zone.equals(this.zone)) {
            return this;
        }
        return new DateTimeFormatter(this.parts, this.locale, this.symbols, this.resolverStyle,
                this.resolverFields, this.chronology, zone);
    }

    public ResolverStyle getResolverStyle() {
        return this.resolverStyle;
    }

    public DateTimeFormatter withResolverStyle(ResolverStyle resolverStyle) {
        if (resolverStyle == null) {
            throw new NullPointerException("resolverStyle");
        }
        if (resolverStyle.equals(this.resolverStyle)) {
            return this;
        }
        return new DateTimeFormatter(this.parts, this.locale, this.symbols, resolverStyle,
                this.resolverFields, this.chronology, this.zone);
    }

    public Set<TemporalField> getResolverFields() {
        return this.resolverFields;
    }

    public DateTimeFormatter withResolverFields(TemporalField... resolverFields) {
        Set<TemporalField> set = null;
        if (resolverFields != null) {
            set = new HashSet<TemporalField>();
            int i = 0;
            while (i < resolverFields.length) {
                set.add(resolverFields[i]);
                i = i + 1;
            }
        }
        return new DateTimeFormatter(this.parts, this.locale, this.symbols, this.resolverStyle,
                set, this.chronology, this.zone);
    }

    public DateTimeFormatter withResolverFields(Set<TemporalField> resolverFields) {
        Set<TemporalField> set = null;
        if (resolverFields != null) {
            set = new HashSet<TemporalField>(resolverFields);
        }
        return new DateTimeFormatter(this.parts, this.locale, this.symbols, this.resolverStyle,
                set, this.chronology, this.zone);
    }

    // ---- writing --------------------------------------------------------------------------------

    public String format(TemporalAccessor temporal) {
        StringBuilder out = new StringBuilder(32);
        this.printTo(temporal, out);
        return out.toString();
    }

    public void formatTo(TemporalAccessor temporal, Appendable appendable) {
        if (appendable == null) {
            throw new NullPointerException("appendable");
        }
        // The JDK does not declare `throws IOException` here and wraps instead: whoever asks to
        // format into an `Appendable` has no reason to catch I/O, and `DateTimeException` is this
        // package's exception. The error is not lost, it changes shape.
        try {
            appendable.append(this.format(temporal));
        } catch (java.io.IOException e) {
            throw new java.time.DateTimeException("failed to write to the destination", e);
        }
    }

    private void printTo(TemporalAccessor temporal, StringBuilder out) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        PrintContext ctx = new PrintContext(this.adjust(temporal), this.locale, this.symbols);
        this.parts.print(ctx, out);
    }

    // The value as the parts see it: with the derived fields, and with the replacements from
    // `withZone` and `withChronology` already applied.
    private TemporalAccessor adjust(TemporalAccessor temporal) {
        // It is wrapped **first** to derive, and only then is it checked for an instant: an
        // `Instant` from this library does not answer `INSTANT_SECONDS` by itself, and without the
        // wrapper `withZone` would not recognize it as convertible.
        TemporalAccessor base = new DerivedTemporal(temporal, null, null);
        if (this.zone != null && !this.zone.equals(base.query(TemporalQueries.zoneId()))
                && base.isSupported(ChronoField.INSTANT_SECONDS)) {
            // With an instant at hand the change of zone is a real conversion: the date and the
            // time that come out are **that** place's. With no instant the only honest thing is to
            // lend the zone to the value without moving the time --which is what `DerivedTemporal`
            // does--.
            base = ZonedDateTime.ofInstant(Instant.from(base), this.zone);
        }
        return new DerivedTemporal(base, this.chronology, this.zone);
    }

    // ---- reading --------------------------------------------------------------------------------

    /**
     * Reads `text` with this formatter and returns the fields it found.
     *
     * <p>What comes back **is not a date**: it is the set of fields the text carried, already
     * resolved --year+month+day become the epoch day, hour+minute the nano of day-- but with no
     * decision about which class they go into. That decision belongs to the caller, and that is why
     * the other version exists: `parse(text, LocalDate::from)`.
     *
     * @throws java.time.format.DateTimeParseException if the text does not fit
     */
    public TemporalAccessor parse(CharSequence text) {
        return this.parseWhole(text);
    }

    /**
     * Reads `text` and builds out of it whatever `query` asks for.
     *
     * <p>It is the form used by the `parse(text, formatter)` of `LocalDate`, `LocalTime` and the
     * rest: each one passes its own `from`.
     *
     * @throws java.time.format.DateTimeParseException if the text does not fit, or if what fits is
     *     not enough for what `query` asks for
     */
    public <T> T parse(CharSequence text, TemporalQuery<T> query) {
        if (query == null) {
            throw new NullPointerException("query");
        }
        Parsed parsed = this.parseWhole(text);
        try {
            return query.queryFrom(parsed);
        } catch (DateTimeException e) {
            // The text fitted but did not carry what was needed --a time-only pattern asked for a
            // date--. It is relabelled as a parse error because from outside it is the same thing:
            // the text did not give what was asked for. The original message goes inside.
            throw new DateTimeParseException(
                    "Text '" + text + "' could not be parsed: " + e.getMessage(), text, 0);
        }
    }

    /**
     * Reads from `position` and **leaves the rest**, moving `position` to where it got.
     *
     * <p>Unlike the other `parse` methods, it does not require the whole text to be consumed: it is
     * the form that serves to read a date sitting inside a longer text. An error does not throw: it
     * is noted in `position.getErrorIndex()` and `null` is returned.
     */
    public TemporalAccessor parse(CharSequence text, ParsePosition position) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        if (position == null) {
            throw new NullPointerException("position");
        }
        String t = text.toString();
        ParseContext ctx = this.read(t, position);
        // **It throws, it does not return `null`.** That is what the JDK does, even though the
        // position has an `errorIndex` to note it in: the version that does not throw is
        // `parseUnresolved`. That both take a `ParsePosition` does not make them the same operation
        // --this one resolves, and resolving can fail for a reason an index cannot tell--.
        if (ctx == null) {
            throw new DateTimeParseException("Text '" + t + "' could not be parsed at index "
                    + position.getErrorIndex(), text, position.getErrorIndex());
        }
        try {
            return this.resolve(ctx);
        } catch (DateTimeParseException e) {
            throw e;
        } catch (RuntimeException e) {
            position.setErrorIndex(position.getIndex());
            throw new DateTimeParseException(
                    "Text '" + t + "' could not be parsed: " + e.getMessage(), text, 0, e);
        }
    }

    /**
     * The same as `parse(text, position)` but **without resolving**: the raw fields, as the text
     * carried them.
     *
     * <p>It serves to see what the text said before the chronology decides what it means. An error
     * does not throw: it is noted in `position`.
     */
    public TemporalAccessor parseUnresolved(CharSequence text, ParsePosition position) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        if (position == null) {
            throw new NullPointerException("position");
        }
        ParseContext ctx = this.read(text.toString(), position);
        if (ctx == null) {
            return null;
        }
        return new Unresolved(ctx);
    }

    /**
     * Reads the text and returns **the first** of `queries` that can be built out of what it
     * carried.
     *
     * <p>That is what the optional sections are for: `ISO_DATE_TIME` reads text with a zone or
     * without one, and `parseBest(t, ZonedDateTime::from, LocalDateTime::from)` returns what the
     * text really said instead of forcing the richest and failing. The order rules: they are tried
     * from most specific to least.
     *
     * @throws java.time.format.DateTimeParseException if the text does not fit, or if none of the
     *     queries can be built
     */
    public TemporalAccessor parseBest(CharSequence text, TemporalQuery<?>... queries) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        if (queries == null || queries.length < 2) {
            throw new IllegalArgumentException(
                    "At least two queries must be specified");
        }
        Parsed parsed = this.parseWhole(text);
        int i = 0;
        while (i < queries.length) {
            try {
                return (TemporalAccessor) queries[i].queryFrom(parsed);
            } catch (RuntimeException e) {
                i = i + 1;
            }
        }
        throw new DateTimeParseException("Text '" + text
                + "' could not be parsed: unable to obtain any of the requested types", text, 0);
    }

    private Parsed parseWhole(CharSequence text) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        String t = text.toString();
        ParsePosition pos = new ParsePosition(0);
        ParseContext ctx = this.read(t, pos);
        if (ctx == null) {
            int i = pos.getErrorIndex();
            throw new DateTimeParseException(
                    "Text '" + t + "' could not be parsed at index " + i, text, i);
        }
        if (pos.getIndex() != t.length()) {
            throw new DateTimeParseException("Text '" + t
                    + "' could not be parsed, unparsed text found at index " + pos.getIndex(),
                    text, pos.getIndex());
        }
        try {
            return this.resolve(ctx);
        } catch (DateTimeParseException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new DateTimeParseException(
                    "Text '" + t + "' could not be parsed: " + e.getMessage(), text, 0, e);
        }
    }

    private ParseContext read(String t, ParsePosition pos) {
        ParseContext ctx = new ParseContext(this.locale, this.symbols,
                this.resolverStyle != ResolverStyle.LENIENT, this.chronology, null);
        int r = this.parts.parse(ctx, t, pos.getIndex());
        if (r < 0) {
            pos.setErrorIndex(~r);
            return null;
        }
        pos.setIndex(r);
        return ctx;
    }

    private Parsed resolve(ParseContext ctx) {
        return new Parsed(ctx, this.resolverStyle, this.resolverFields, this.zone, this.chronology);
    }

    // ---- bridge with java.text ------------------------------------------------------------------

    /**
     * This formatter seen as a `java.text.Format`.
     *
     * <p>It writes **and** reads: `parseObject` goes through `parse(text, ParsePosition)`, which is
     * the same operation without the middleman.
     *
     * <p>This note used to say that it only wrote, because `java.text.Format` did not declare
     * `parseObject` at the time. It does now, and until finding #284 caught it the adapter was a
     * concrete class missing an inherited abstract method: it compiled, and any caller reaching for
     * the reading half would have got an `AbstractMethodError`.
     */
    public Format toFormat() {
        return new FormatAdapter(this, null);
    }

    /**
     * Like `toFormat()`, but the result asked of the parse is fixed up front.
     */
    public Format toFormat(TemporalQuery<?> parseQuery) {
        if (parseQuery == null) {
            throw new NullPointerException("parseQuery");
        }
        return new FormatAdapter(this, parseQuery);
    }

    public String toString() {
        return this.parts.toString();
    }

    // ---- the predefined ones, built -------------------------------------------------------------

    private static DateTimeFormatter strict(DateTimeFormatterBuilder b) {
        // The `ISO_*` resolve in `STRICT` --as in the JDK-- because ISO-8601 admits neither a 31st
        // of February nor rounding it down to the 28th: text that is not a date has to fail.
        return b.toFormatter(Locale.ROOT).withResolverStyle(ResolverStyle.STRICT)
                .withChronology(IsoChronology.INSTANCE);
    }

    private static DateTimeFormatterBuilder localDate() {
        return new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                .appendLiteral('-')
                .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                .appendLiteral('-')
                .appendValue(ChronoField.DAY_OF_MONTH, 2);
    }

    private static DateTimeFormatterBuilder localTime() {
        return new DateTimeFormatterBuilder()
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .optionalStart()
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                .optionalEnd()
                .optionalEnd();
    }

    private static DateTimeFormatterBuilder localDateTime() {
        return localDate().appendLiteral('T').append(ISO_LOCAL_TIME);
    }

    private static DateTimeFormatter isoLocalDate() {
        return strict(localDate());
    }

    private static DateTimeFormatter isoOffsetDate() {
        return strict(new DateTimeFormatterBuilder().append(ISO_LOCAL_DATE).appendOffsetId());
    }

    private static DateTimeFormatter isoDate() {
        return strict(new DateTimeFormatterBuilder().append(ISO_LOCAL_DATE)
                .optionalStart().appendOffsetId().optionalEnd());
    }

    private static DateTimeFormatter isoLocalTime() {
        return strict(localTime());
    }

    private static DateTimeFormatter isoOffsetTime() {
        return strict(new DateTimeFormatterBuilder().append(ISO_LOCAL_TIME).appendOffsetId());
    }

    private static DateTimeFormatter isoTime() {
        return strict(new DateTimeFormatterBuilder().append(ISO_LOCAL_TIME)
                .optionalStart().appendOffsetId().optionalEnd());
    }

    private static DateTimeFormatter isoLocalDateTime() {
        return strict(localDateTime());
    }

    private static DateTimeFormatter isoOffsetDateTime() {
        return strict(new DateTimeFormatterBuilder().append(ISO_LOCAL_DATE_TIME).appendOffsetId());
    }

    // The `[Europe/Paris]` goes **after** the offset and inside an optional section: an
    // `OffsetDateTime` comes out without brackets and a `ZonedDateTime` with them, from the same
    // formatter.
    private static DateTimeFormatter isoZonedDateTime() {
        return strict(new DateTimeFormatterBuilder().append(ISO_OFFSET_DATE_TIME)
                .optionalStart()
                .appendLiteral('[')
                .parseCaseSensitive()
                .appendZoneRegionId()
                .appendLiteral(']')
                .optionalEnd());
    }

    private static DateTimeFormatter isoDateTime() {
        return strict(new DateTimeFormatterBuilder().append(ISO_LOCAL_DATE_TIME)
                .optionalStart()
                .appendOffsetId()
                .optionalStart()
                .appendLiteral('[')
                .parseCaseSensitive()
                .appendZoneRegionId()
                .appendLiteral(']')
                .optionalEnd()
                .optionalEnd());
    }

    private static DateTimeFormatter isoOrdinalDate() {
        return strict(new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                .appendLiteral('-')
                .appendValue(ChronoField.DAY_OF_YEAR, 3)
                .optionalStart()
                .appendOffsetId()
                .optionalEnd());
    }

    private static DateTimeFormatter isoWeekDate() {
        return strict(new DateTimeFormatterBuilder()
                .appendValue(IsoFields.WEEK_BASED_YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                .appendLiteral("-W")
                .appendValue(IsoFields.WEEK_OF_WEEK_BASED_YEAR, 2)
                .appendLiteral('-')
                .appendValue(ChronoField.DAY_OF_WEEK, 1)
                .optionalStart()
                .appendOffsetId()
                .optionalEnd());
    }

    private static DateTimeFormatter isoInstant() {
        return new DateTimeFormatterBuilder().parseCaseInsensitive().appendInstant()
                .toFormatter(Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    }

    // No separators: the three fields are of fixed width and that is why they can be run together.
    // The offset goes in lenient mode because `+0000` and `Z` are both legal and strict would
    // reject one of them.
    private static DateTimeFormatter basicIsoDate() {
        return strict(new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4)
                .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                .appendValue(ChronoField.DAY_OF_MONTH, 2)
                .optionalStart()
                .parseLenient()
                .appendOffset("+HHMMss", "Z")
                .parseStrict()
                .optionalEnd());
    }

    // The RFC 1123 names go through `appendText(field, Map)` and not through the English set: the
    // RFC fixes them, it does not take them from the locale. With an explicit map the formatter
    // says exactly that, and it keeps working under any `withLocale`.
    private static DateTimeFormatter rfc1123() {
        Map<Long, String> days = new HashMap<Long, String>();
        days.put(Long.valueOf(1L), "Mon");
        days.put(Long.valueOf(2L), "Tue");
        days.put(Long.valueOf(3L), "Wed");
        days.put(Long.valueOf(4L), "Thu");
        days.put(Long.valueOf(5L), "Fri");
        days.put(Long.valueOf(6L), "Sat");
        days.put(Long.valueOf(7L), "Sun");
        Map<Long, String> months = new HashMap<Long, String>();
        months.put(Long.valueOf(1L), "Jan");
        months.put(Long.valueOf(2L), "Feb");
        months.put(Long.valueOf(3L), "Mar");
        months.put(Long.valueOf(4L), "Apr");
        months.put(Long.valueOf(5L), "May");
        months.put(Long.valueOf(6L), "Jun");
        months.put(Long.valueOf(7L), "Jul");
        months.put(Long.valueOf(8L), "Aug");
        months.put(Long.valueOf(9L), "Sep");
        months.put(Long.valueOf(10L), "Oct");
        months.put(Long.valueOf(11L), "Nov");
        months.put(Long.valueOf(12L), "Dec");
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .parseLenient()
                .optionalStart()
                .appendText(ChronoField.DAY_OF_WEEK, days)
                .appendLiteral(", ")
                .optionalEnd()
                .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                .appendLiteral(' ')
                .appendText(ChronoField.MONTH_OF_YEAR, months)
                .appendLiteral(' ')
                .appendValue(ChronoField.YEAR, 4)
                .appendLiteral(' ')
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .optionalEnd()
                .appendLiteral(' ')
                .appendOffset("+HHMM", "GMT")
                .toFormatter(Locale.ROOT)
                .withResolverStyle(ResolverStyle.SMART)
                .withChronology(IsoChronology.INSTANCE);
    }
}

// The fields as the text carried them, unresolved. It is what `parseUnresolved` returns.
final class Unresolved implements TemporalAccessor {

    private final Map<TemporalField, Long> fields;
    private final ZoneId zone;
    private final Chronology chronology;

    Unresolved(ParseContext ctx) {
        this.fields = ctx.fields;
        this.zone = ctx.zone != null ? ctx.zone : ctx.offset;
        this.chronology = ctx.chronology;
    }

    public boolean isSupported(TemporalField field) {
        return field != null && this.fields.containsKey(field);
    }

    public long getLong(TemporalField field) {
        Long v = this.fields.get(field);
        if (v == null) {
            throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: "
                    + field);
        }
        return v.longValue();
    }

    public int get(TemporalField field) {
        return field.range().checkValidIntValue(this.getLong(field), field);
    }

    public <R> R query(TemporalQuery<R> query) {
        if (query == TemporalQueries.zoneId() || query == TemporalQueries.zone()) {
            return (R) this.zone;
        }
        if (query == TemporalQueries.chronology()) {
            return (R) this.chronology;
        }
        return query.queryFrom(this);
    }

    public String toString() {
        return this.fields.toString();
    }
}

// `DateTimeFormatter.toFormat()`.
//
// It writes AND reads. This note used to say it only wrote, because this library's
// `java.text.Format` declared only the writing half; it declares both now, and `parseObject` is
// implemented below. The `parseQuery` is what that half reads with, and it is also what keeps the
// object the one the caller asked for -- two `toFormat` with different queries are not equal.
final class FormatAdapter extends Format {

    private final DateTimeFormatter formatter;
    private final TemporalQuery<?> query;

    FormatAdapter(DateTimeFormatter formatter, TemporalQuery<?> query) {
        this.formatter = formatter;
        this.query = query;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        if (obj == null) {
            throw new NullPointerException("obj");
        }
        if (toAppendTo == null) {
            throw new NullPointerException("toAppendTo");
        }
        if (!(obj instanceof TemporalAccessor)) {
            throw new IllegalArgumentException("Format target must implement TemporalAccessor");
        }
        toAppendTo.append(this.formatter.format((TemporalAccessor) obj));
        return toAppendTo;
    }

    /**
     * Reads from `pos` and leaves the rest, as `Format` asks.
     *
     * <p>**A failure is noted in the cursor, not thrown.** That is the difference between this form
     * and `parseObject(String)`, and it is why the call to `parse(text, position)` --which does
     * throw-- goes inside a `try`: the two halves have opposite conventions for the same failure,
     * and returning the exception through would break the contract of whoever called this one.
     */
    public Object parseObject(String source, ParsePosition pos) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        if (pos == null) {
            throw new NullPointerException("pos");
        }
        try {
            TemporalAccessor read = this.formatter.parse(source, pos);
            if (this.query == null) {
                return read;
            }
            return read.query(this.query);
        } catch (RuntimeException e) {
            // `parse` may have moved the index before failing; the error index is what marks it.
            if (pos.getErrorIndex() < 0) {
                pos.setErrorIndex(pos.getIndex());
            }
            return null;
        }
    }
}
