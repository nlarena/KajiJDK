package java.text;

import java.io.InvalidObjectException;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// KajiLibrary's java.text.NumberFormat — the abstract base for number formatters, and the layer
// that turns Format's Object-shaped contract into a numeric one.
//
// Its job in the hierarchy is dispatch: Format speaks Object, but a number formatter wants a
// primitive, and `double` and `long` are NOT interchangeable — a long past 2^53 cannot round-trip
// through a double. So NumberFormat declares two abstract seams, one per primitive, and routes
// Object to whichever one preserves the value.
//
// The other half of this class is STATE, not behaviour: how many digits at least, how many at most,
// whether it groups, whether parsing stops at the point. It lives here and not in DecimalFormat
// because it is the part of the contract a caller can adjust without knowing which implementation
// they have in front of them.
//
// On `getCompactNumberInstance()` and `getCompactNumberInstance(Locale, Style)`: this note used to
// say they were left out because the CLDR's table of compact patterns --"0 thousand", "0 million",
// "0万", "0億"-- is text translated per locale and per plural rule, and filling it with "K/M/B" for
// everyone would give a plausible and false result for most. The second half is still true; what
// changed is that the data is no longer invented.
//
// The table below carries the **exact** patterns of the same six locales `DecimalFormatSymbols`
// covers, and they are not transcribed by hand: they were read out of JDK 25 by reflection over its
// own `CompactNumberFormat`'s `compactPatterns` field. An unknown locale falls back to ROOT, which
// is what the JDK does with a locale it has no data for.
//
// `CompactNumberFormat` already knew how to read them: it evaluates the `{one:... other:...}`
// variants against the locale's plural rules. The only thing it lacked was where to get them
// from.
public abstract class NumberFormat extends Format {

    /**
     * It marks the integer field for {@link FieldPosition}. It coexists with
     * {@link java.text.NumberFormat.Field#INTEGER}, which is the new way of naming the same thing;
     * neither replaces the other in an API already published.
     */
    public static final int INTEGER_FIELD = 0;

    /** It marks the fraction field for {@link FieldPosition}. */
    public static final int FRACTION_FIELD = 1;

    /**
     * The key a numeric formatter marks each piece of the text it produced with.
     *
     * <p>It is what allows asking "where did the thousands separator end up?" without reparsing the
     * output, and what a renderer uses to, for instance, set the currency sign in another
     * typeface.
     */
    public static class Field extends java.text.Format.Field {

        // The same register by name as Attribute, and for the same reason: it is only populated with
        // instances of THIS exact class, so a subclass cannot overwrite its constants.
        private static final Map<String, java.text.NumberFormat.Field> INSTANCES =
                new HashMap<String, java.text.NumberFormat.Field>();

        protected Field(String name) {
            super(name);
            if (this.getClass() == java.text.NumberFormat.Field.class) {
                INSTANCES.put(name, this);
            }
        }

        protected Object readResolve() throws InvalidObjectException {
            if (this.getClass() != java.text.NumberFormat.Field.class) {
                throw new InvalidObjectException("subclass didn't correctly implement readResolve");
            }
            java.text.NumberFormat.Field f = INSTANCES.get(this.getName());
            if (f != null) {
                return f;
            }
            throw new InvalidObjectException("unknown attribute name");
        }

        public static final java.text.NumberFormat.Field INTEGER = new java.text.NumberFormat.Field("integer");
        public static final java.text.NumberFormat.Field FRACTION = new java.text.NumberFormat.Field("fraction");
        public static final java.text.NumberFormat.Field EXPONENT = new java.text.NumberFormat.Field("exponent");
        public static final java.text.NumberFormat.Field DECIMAL_SEPARATOR =
                new java.text.NumberFormat.Field("decimal separator");
        public static final java.text.NumberFormat.Field SIGN = new java.text.NumberFormat.Field("sign");
        public static final java.text.NumberFormat.Field GROUPING_SEPARATOR =
                new java.text.NumberFormat.Field("grouping separator");
        public static final java.text.NumberFormat.Field EXPONENT_SYMBOL =
                new java.text.NumberFormat.Field("exponent symbol");
        public static final java.text.NumberFormat.Field PERCENT = new java.text.NumberFormat.Field("percent");
        public static final java.text.NumberFormat.Field PERMILLE = new java.text.NumberFormat.Field("per mille");
        public static final java.text.NumberFormat.Field CURRENCY = new java.text.NumberFormat.Field("currency");
        public static final java.text.NumberFormat.Field EXPONENT_SIGN = new java.text.NumberFormat.Field("exponent sign");
        public static final java.text.NumberFormat.Field PREFIX = new java.text.NumberFormat.Field("prefix");
        public static final java.text.NumberFormat.Field SUFFIX = new java.text.NumberFormat.Field("suffix");
    }

    /**
     * How long a compact formatter writes its suffixes: {@code 1K} against {@code 1 thousand}.
     */
    public static enum Style {
        SHORT,
        LONG
    }

    private boolean groupingUsed;
    private boolean parseIntegerOnly;
    private int maximumIntegerDigits;
    private int minimumIntegerDigits;
    private int maximumFractionDigits;
    private int minimumFractionDigits;

    protected NumberFormat() {
        this.groupingUsed = true;
        this.parseIntegerOnly = false;
        this.maximumIntegerDigits = 40;
        this.minimumIntegerDigits = 1;
        this.maximumFractionDigits = 3;
        this.minimumFractionDigits = 0;
    }

    // The Object entry point. Integral wrappers go through the long seam so their exact value
    // survives; everything else through the double one.
    public StringBuffer format(Object number, StringBuffer toAppendTo, FieldPosition pos) {
        if (number instanceof Long || number instanceof Integer
                || number instanceof Short || number instanceof Byte) {
            Number n = (Number) number;
            return this.format(n.longValue(), toAppendTo, pos);
        }
        if (number instanceof Number) {
            Number n = (Number) number;
            return this.format(n.doubleValue(), toAppendTo, pos);
        }
        throw new IllegalArgumentException("Cannot format given Object as a Number");
    }

    public final String format(double number) {
        return this.format(number, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public final String format(long number) {
        return this.format(number, new StringBuffer(), new FieldPosition(0)).toString();
    }

    // The two seams a concrete formatter fills in.
    public abstract StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos);

    public abstract StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos);

    public abstract Number parse(String source, ParsePosition parsePosition);

    /**
     * It parses from the start and fails with an exception.
     *
     * <p>As in {@link Format#parseObject(String)}, failure is detected by the cursor not advancing
     * and not by a null.
     */
    public Number parse(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Number result = this.parse(source, pos);
        if (pos.getIndex() == 0) {
            throw new ParseException("Unparseable number: \"" + source + "\"", pos.getErrorIndex());
        }
        return result;
    }

    // Final: a numeric formatter parses numbers, and letting a subclass return something else
    // through this door would break the equivalence with parse().
    public final Object parseObject(String source, ParsePosition pos) {
        return this.parse(source, pos);
    }

    public boolean isParseIntegerOnly() {
        return this.parseIntegerOnly;
    }

    public void setParseIntegerOnly(boolean value) {
        this.parseIntegerOnly = value;
    }

    public boolean isGroupingUsed() {
        return this.groupingUsed;
    }

    public void setGroupingUsed(boolean newValue) {
        this.groupingUsed = newValue;
    }

    public int getMaximumIntegerDigits() {
        return this.maximumIntegerDigits;
    }

    // The four setters overwrite each other on purpose: a maximum below the minimum is not a
    // representable state, so the one just set wins and the other follows it. Rejecting it with an
    // exception would force the caller to know the order they have to be called in.
    public void setMaximumIntegerDigits(int newValue) {
        this.maximumIntegerDigits = Math.max(0, newValue);
        if (this.minimumIntegerDigits > this.maximumIntegerDigits) {
            this.minimumIntegerDigits = this.maximumIntegerDigits;
        }
    }

    public int getMinimumIntegerDigits() {
        return this.minimumIntegerDigits;
    }

    public void setMinimumIntegerDigits(int newValue) {
        this.minimumIntegerDigits = Math.max(0, newValue);
        if (this.minimumIntegerDigits > this.maximumIntegerDigits) {
            this.maximumIntegerDigits = this.minimumIntegerDigits;
        }
    }

    public int getMaximumFractionDigits() {
        return this.maximumFractionDigits;
    }

    public void setMaximumFractionDigits(int newValue) {
        this.maximumFractionDigits = Math.max(0, newValue);
        if (this.minimumFractionDigits > this.maximumFractionDigits) {
            this.minimumFractionDigits = this.maximumFractionDigits;
        }
    }

    public int getMinimumFractionDigits() {
        return this.minimumFractionDigits;
    }

    public void setMinimumFractionDigits(int newValue) {
        this.minimumFractionDigits = Math.max(0, newValue);
        if (this.minimumFractionDigits > this.maximumFractionDigits) {
            this.maximumFractionDigits = this.minimumFractionDigits;
        }
    }

    /**
     * @throws UnsupportedOperationException always, unless the subclass overrides it
     * @implSpec The base has NO currency: it does not know which symbol it would use nor where it
     *           would put it. Throwing is the behaviour the contract defines for that case --
     *           returning {@code null} would pass off as "no currency" what is really "this class
     *           does not know".
     */
    public Currency getCurrency() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always, unless the subclass overrides it
     */
    public void setCurrency(Currency currency) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always, unless the subclass overrides it
     */
    public RoundingMode getRoundingMode() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always, unless the subclass overrides it
     */
    public void setRoundingMode(RoundingMode roundingMode) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always, unless the subclass overrides it
     */
    public boolean isStrict() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always, unless the subclass overrides it
     */
    public void setStrict(boolean strict) {
        throw new UnsupportedOperationException();
    }

    // ---- factories by locale ----
    //
    // They all end in a DecimalFormat built out of two separate things: the PATTERN, which says the
    // order and comes from LocalePatterns, and the SYMBOLS, which say the characters and come from
    // DecimalFormatSymbols. That separation is what makes a class per locale unnecessary.

    public static final NumberFormat getInstance() {
        return NumberFormat.getNumberInstance(Locale.getDefault());
    }

    public static NumberFormat getInstance(Locale inLocale) {
        return NumberFormat.getNumberInstance(inLocale);
    }

    public static final NumberFormat getNumberInstance() {
        return NumberFormat.getNumberInstance(Locale.getDefault());
    }

    public static NumberFormat getNumberInstance(Locale inLocale) {
        return new DecimalFormat(LocalePatterns.number(inLocale),
                new DecimalFormatSymbols(inLocale));
    }

    public static final NumberFormat getIntegerInstance() {
        return NumberFormat.getIntegerInstance(Locale.getDefault());
    }

    // parseIntegerOnly goes in true, which is what tells this factory apart from a getNumberInstance
    // with zero decimals: besides not printing them, when parsing it stops at the decimal
    // separator.
    public static NumberFormat getIntegerInstance(Locale inLocale) {
        DecimalFormat f = new DecimalFormat(LocalePatterns.integerPart(inLocale),
                new DecimalFormatSymbols(inLocale));
        f.setParseIntegerOnly(true);
        return f;
    }

    public static final NumberFormat getCurrencyInstance() {
        return NumberFormat.getCurrencyInstance(Locale.getDefault());
    }

    public static NumberFormat getCurrencyInstance(Locale inLocale) {
        return new DecimalFormat(LocalePatterns.currency(inLocale),
                new DecimalFormatSymbols(inLocale));
    }

    public static final NumberFormat getPercentInstance() {
        return NumberFormat.getPercentInstance(Locale.getDefault());
    }

    // Without parseIntegerOnly, unlike the integer one: the percentage pattern has no decimals, but
    // when PARSING "12.5%" the 12.5 is a legitimate value and cutting it at the point would lose
    // it.
    public static NumberFormat getPercentInstance(Locale inLocale) {
        return new DecimalFormat(LocalePatterns.percentSign(inLocale),
                new DecimalFormatSymbols(inLocale));
    }

    /**
     * The locales there is genuine data for.
     *
     * <p>There are six, not hundreds, and the list tells the truth about that: a locale that is not
     * here still works, but falls back to ROOT. Returning a long list feigning coverage would be
     * exactly the kind of lie this package avoids.
     */
    public static Locale[] getAvailableLocales() {
        return DecimalFormatSymbols.getAvailableLocales();
    }

    public int hashCode() {
        int h = this.maximumIntegerDigits * 37 + this.maxFractionSeed();
        if (this.groupingUsed) {
            h = h + 1;
        }
        return h;
    }

    private int maxFractionSeed() {
        return this.maximumFractionDigits;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        NumberFormat other = (NumberFormat) obj;
        return this.maximumIntegerDigits == other.maximumIntegerDigits
                && this.minimumIntegerDigits == other.minimumIntegerDigits
                && this.maximumFractionDigits == other.maximumFractionDigits
                && this.minimumFractionDigits == other.minimumFractionDigits
                && this.groupingUsed == other.groupingUsed
                && this.parseIntegerOnly == other.parseIntegerOnly;
    }

    // ---- the compact formatters --------------------------------------------------------------

    /**
     * The default locale's compact formatter, in the short style.
     *
     * <p>It is {@code getCompactNumberInstance(Locale.getDefault(FORMAT), Style.SHORT)}, which is
     * what the contract defines. The category is {@code FORMAT} and not the plain default: on a
     * machine where the display locale and the format locale differ they are two different answers,
     * and the one this method promises is the format one. Checked against JDK 25.
     */
    public static NumberFormat getCompactNumberInstance() {
        return NumberFormat.getCompactNumberInstance(Locale.getDefault(Locale.Category.FORMAT),
                                                     NumberFormat.Style.SHORT);
    }

    /**
     * That locale and style's compact formatter.
     *
     * <p>A locale with no data of its own falls back to ROOT. See the header's note on where the
     * patterns come from.
     *
     * @throws NullPointerException if either of the two is null
     */
    public static NumberFormat getCompactNumberInstance(Locale locale,
                                                        NumberFormat.Style formatStyle) {
        if (locale == null || formatStyle == null) {
            throw new NullPointerException();
        }
        int i = DecimalFormatSymbols.indexOf(locale);
        String[] row = formatStyle == NumberFormat.Style.SHORT
                ? NumberFormat.shortCompacts()[i]
                : NumberFormat.longCompacts()[i];
        String[] copy = new String[row.length];
        for (int k = 0; k < row.length; k = k + 1) {
            copy[k] = row[k];
        }
        return new CompactNumberFormat(NumberFormat.DECIMAL_PATTERN,
                                       new DecimalFormatSymbols(locale), copy,
                                       NumberFormat.pluralRules()[i]);
    }

    // The base decimal pattern is the same in all six locales; what changes between them is the
    // symbols, and `DecimalFormatSymbols` brings those.
    private static final String DECIMAL_PATTERN = "#,##0.###";

    // The locale's plural rules, in the CLDR's syntax. Empty where the language does not
    // distinguish --Japanese has no grammatical plural-- and there `CompactNumberFormat` uses
    // `other`, which is the category the CLDR guarantees in every locale.
    private static String[] pluralRules() {
        return new String[] {
            "",
            "one:i = 1 and v = 0",
            "one:n = 1;many:e = 0 and i != 0 and i % 1000000 = 0 and v = 0 or e != 0..5",
            "one:i = 1 and v = 0",
            "one:i = 0,1;many:e = 0 and i != 0 and i % 1000000 = 0 and v = 0 or e != 0..5",
            "",
        };
    }

    // One row per locale, in the same order as `DecimalFormatSymbols`'s table: und, en-US, es-AR,
    // de-DE, fr-FR, ja-JP. Each entry is the power of ten of its index, and the first three are
    // empty because nothing below a thousand gets compacted.
    //
    // The Japanese row is longer than the rest: its numeral system groups by ten thousand (万, 億,
    // 兆, 京) and the CLDR gives it entries up to 10^18. It is not an oversight in the others.
    private static String[][] shortCompacts() {
        return new String[][] {
            {"", "", "", "{other:0K}", "{other:00K}", "{other:000K}", "{other:0M}", "{other:00M}",
             "{other:000M}", "{other:0G}", "{other:00G}", "{other:000G}", "{other:0T}",
             "{other:00T}", "{other:000T}"},
            {"", "", "", "{one:0K other:0K}", "{one:00K other:00K}", "{one:000K other:000K}",
             "{one:0M other:0M}", "{one:00M other:00M}", "{one:000M other:000M}",
             "{one:0B other:0B}", "{one:00B other:00B}", "{one:000B other:000B}",
             "{one:0T other:0T}", "{one:00T other:00T}", "{one:000T other:000T}"},
            {"", "", "", "{one:0\u00a0K other:0\u00a0K}", "{one:00\u00a0k other:00\u00a0k}",
             "{one:000\u00a0k other:000\u00a0k}", "{one:0\u00a0M other:0\u00a0M}",
             "{one:00\u00a0M other:00\u00a0M}", "{one:000\u00a0M other:000\u00a0M}",
             "{one:0000\u00a0M other:0000\u00a0M}",
             "{one:00\u00a0mil\u00a0M other:00\u00a0mil\u00a0M}",
             "{one:000\u00a0mil\u00a0M other:000\u00a0mil\u00a0M}",
             "{one:0\u00a0B other:0\u00a0B}", "{one:00\u00a0B other:00\u00a0B}",
             "{one:000\u00a0B other:000\u00a0B}"},
            {"", "", "", "{one:0 other:0}", "{one:0 other:0}", "{one:0 other:0}",
             "{one:0\u00a0Mio'.' other:0\u00a0Mio'.'}",
             "{one:00\u00a0Mio'.' other:00\u00a0Mio'.'}",
             "{one:000\u00a0Mio'.' other:000\u00a0Mio'.'}",
             "{one:0\u00a0Mrd'.' other:0\u00a0Mrd'.'}",
             "{one:00\u00a0Mrd'.' other:00\u00a0Mrd'.'}",
             "{one:000\u00a0Mrd'.' other:000\u00a0Mrd'.'}",
             "{one:0\u00a0Bio'.' other:0\u00a0Bio'.'}",
             "{one:00\u00a0Bio'.' other:00\u00a0Bio'.'}",
             "{one:000\u00a0Bio'.' other:000\u00a0Bio'.'}"},
            {"", "", "", "{one:0\u00a0k other:0\u00a0k}", "{one:00\u00a0k other:00\u00a0k}",
             "{one:000\u00a0k other:000\u00a0k}", "{one:0\u00a0M other:0\u00a0M}",
             "{one:00\u00a0M other:00\u00a0M}", "{one:000\u00a0M other:000\u00a0M}",
             "{one:0\u00a0Md other:0\u00a0Md}", "{one:00\u00a0Md other:00\u00a0Md}",
             "{one:000\u00a0Md other:000\u00a0Md}", "{one:0\u00a0Bn other:0\u00a0Bn}",
             "{one:00\u00a0Bn other:00\u00a0Bn}", "{one:000\u00a0Bn other:000\u00a0Bn}"},
            {"", "", "", "{other:0}", "{other:0\u4e07}", "{other:00\u4e07}", "{other:000\u4e07}",
             "{other:0000\u4e07}", "{other:0\u5104}", "{other:00\u5104}", "{other:000\u5104}",
             "{other:0000\u5104}", "{other:0\u5146}", "{other:00\u5146}", "{other:000\u5146}",
             "{other:0000\u5146}", "{other:0\u4eac}", "{other:00\u4eac}", "{other:000\u4eac}",
             "{other:0000\u4eac}"},
        };
    }

    private static String[][] longCompacts() {
        return new String[][] {
            {"", "", "", "{other:0K}", "{other:00K}", "{other:000K}", "{other:0M}", "{other:00M}",
             "{other:000M}", "{other:0G}", "{other:00G}", "{other:000G}", "{other:0T}",
             "{other:00T}", "{other:000T}"},
            {"", "", "", "{one:0' 'thousand other:0' 'thousand}",
             "{one:00' 'thousand other:00' 'thousand}", "{one:000' 'thousand other:000' 'thousand}",
             "{one:0' 'million other:0' 'million}", "{one:00' 'million other:00' 'million}",
             "{one:000' 'million other:000' 'million}", "{one:0' 'billion other:0' 'billion}",
             "{one:00' 'billion other:00' 'billion}", "{one:000' 'billion other:000' 'billion}",
             "{one:0' 'trillion other:0' 'trillion}", "{one:00' 'trillion other:00' 'trillion}",
             "{one:000' 'trillion other:000' 'trillion}"},
            {"", "", "", "{one:0\u00a0K other:0\u00a0K}", "{one:00\u00a0k other:00\u00a0k}",
             "{one:000\u00a0k other:000\u00a0k}", "{one:0\u00a0M other:0\u00a0M}",
             "{one:00\u00a0M other:00\u00a0M}", "{one:000\u00a0M other:000\u00a0M}",
             "{one:0000\u00a0M other:0000\u00a0M}",
             "{one:00\u00a0mil\u00a0M other:00\u00a0mil\u00a0M}",
             "{one:000\u00a0mil\u00a0M other:000\u00a0mil\u00a0M}",
             "{one:0\u00a0B other:0\u00a0B}", "{one:00\u00a0B other:00\u00a0B}",
             "{one:000\u00a0B other:000\u00a0B}"},
            {"", "", "", "{one:0' 'Tausend other:0' 'Tausend}",
             "{one:00' 'Tausend other:00' 'Tausend}", "{one:000' 'Tausend other:000' 'Tausend}",
             "{one:0' 'Million other:0' 'Millionen}", "{one:00' 'Millionen other:00' 'Millionen}",
             "{one:000' 'Millionen other:000' 'Millionen}",
             "{one:0' 'Milliarde other:0' 'Milliarden}",
             "{one:00' 'Milliarden other:00' 'Milliarden}",
             "{one:000' 'Milliarden other:000' 'Milliarden}",
             "{one:0' 'Billion other:0' 'Billionen}", "{one:00' 'Billionen other:00' 'Billionen}",
             "{one:000' 'Billionen other:000' 'Billionen}"},
            {"", "", "", "{1:mille one:0' 'millier other:0' 'mille}",
             "{one:00' 'mille other:00' 'mille}", "{one:000' 'mille other:000' 'mille}",
             "{one:0' 'million other:0' 'millions}", "{one:00' 'million other:00' 'millions}",
             "{one:000' 'million other:000' 'millions}", "{one:0' 'milliard other:0' 'milliards}",
             "{one:00' 'milliard other:00' 'milliards}",
             "{one:000' 'milliard other:000' 'milliards}", "{one:0' 'billion other:0' 'billions}",
             "{one:00' 'billion other:00' 'billions}", "{one:000' 'billion other:000' 'billions}"},
            {"", "", "", "{other:0}", "{other:0\u4e07}", "{other:00\u4e07}", "{other:000\u4e07}",
             "{other:0000\u4e07}", "{other:0\u5104}", "{other:00\u5104}", "{other:000\u5104}",
             "{other:0000\u5104}", "{other:0\u5146}", "{other:00\u5146}", "{other:000\u5146}",
             "{other:0000\u5146}", "{other:0\u4eac}", "{other:00\u4eac}", "{other:000\u4eac}",
             "{other:0000\u4eac}"},
        };
    }
}
