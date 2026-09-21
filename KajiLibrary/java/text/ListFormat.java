package java.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * It joins a list of texts the way a language joins them: {@code "a, b and c"}, not
 * {@code "[a, b, c]"}.
 *
 * <p>It looks trivial and it is not. The conjunction does not go between every element but only
 * before the last; the separator of the middle ones may not be the same as the first pair's; and
 * there are languages where a list of two is written differently from the first two of a list of
 * three. That is why the patterns are FIVE and not one: {@code start}, {@code middle},
 * {@code end}, {@code two} and {@code three}.
 *
 * <p>A long list is built by composing: {@code start} joins the first two, {@code middle} keeps
 * adding, and {@code end} attaches the last. Those of two and of three have a pattern of their own
 * because in several languages they are not a particular case of the general formula.
 *
 * @implNote All three factories are here. This note used to say the two localised ones were left
 *           out because the per-locale patterns are CLDR data --the "and", the "or", the comma that
 *           in Japanese is {@code U+3001}-- and putting "and"/"or" for everyone would give a
 *           plausible and false result for most. The second half is still true; what changed is that
 *           the data is no longer invented: the table carries the <b>exact</b> patterns of the same
 *           six locales {@link DecimalFormatSymbols} covers, extracted from JDK 25 and not
 *           transcribed by hand, and an unknown locale falls back to ROOT -- which is what the JDK
 *           does with a locale it has no data for.
 *
 * @implNote What remains a subset is the DATA, not the surface: six locales and not the JDK's
 *           hundreds. Widening it is adding rows to the table, not writing code.
 */
public final class ListFormat extends Format {

    /** What relation the list has: enumeration, alternative, or compound units. */
    public static enum Type {
        STANDARD,
        OR,
        UNIT
    }

    /** How long the conjunction's form is. */
    public static enum Style {
        FULL,
        SHORT,
        NARROW
    }

    private static final int START = 0;
    private static final int MIDDLE = 1;
    private static final int END = 2;
    private static final int TWO = 3;
    private static final int THREE = 4;

    private final Locale locale;
    private final String[] patterns;

    private ListFormat(Locale locale, String[] patterns) {
        this.locale = locale;
        this.patterns = patterns;
    }

    /**
     * The locales with data of their own.
     *
     * <p>They are <b>the same</b> as {@link DecimalFormatSymbols}'s, and not by chance: this class
     * reads its table with the same index, so both cover exactly the same set. Tying them together
     * is what avoids the awkward state of having one locale's symbols and another's patterns.
     */
    public static Locale[] getAvailableLocales() {
        return DecimalFormatSymbols.getAvailableLocales();
    }

    /**
     * The default locale's list formatter, in the standard, full form.
     *
     * <p>It is {@code getInstance(Locale.getDefault(FORMAT), Type.STANDARD, Style.FULL)}, which is
     * what the contract defines. The category is {@code FORMAT} and not the plain default: on a
     * machine where the display locale and the format locale differ --it happens, and it was checked
     * against JDK 25-- they are two different answers, and the one this method promises is the format
     * one.
     */
    public static ListFormat getInstance() {
        return ListFormat.getInstance(Locale.getDefault(Locale.Category.FORMAT), Type.STANDARD,
                                      Style.FULL);
    }

    /**
     * That locale, type and style's list formatter.
     *
     * <p>A locale with no data of its own falls back to ROOT, which is the same as what the JDK does
     * with a locale it has no data for -- not an approximation of this library's.
     *
     * @throws NullPointerException if any of the three is null
     */
    public static ListFormat getInstance(Locale locale, Type type, Style style) {
        if (locale == null || type == null || style == null) {
            throw new NullPointerException();
        }
        String[] row = ListFormat.table(type, style)[DecimalFormatSymbols.indexOf(locale)];
        String[] copy = new String[5];
        for (int i = 0; i < 5; i = i + 1) {
            copy[i] = row[i];
        }
        return new ListFormat(locale, copy);
    }

    // The CLDR's patterns, in the order [start, middle, end, two, three] and with one row per
    // locale, in the same order as `DecimalFormatSymbols`'s table: und, en-US, es-AR, de-DE, fr-FR,
    // ja-JP. Index 0 is ROOT, which is also the fallback.
    //
    // **They are not transcribed by hand.** They were extracted from JDK 25 by formatting lists with
    // unique markers and looking at what was left between them: a CLDR list pattern always has the
    // shape `{0}<literal>{1}`, so the literal is exactly the text separating two markers. It was done
    // this way because this data is translated text and one comma too many in the wrong locale is
    // seen by nobody until a user sees it.
    //
    // Every non-ASCII character goes in as a `\uXXXX` escape, for the same reason as in
    // `DecimalFormatSymbols`: the source stays ASCII and no encoding mishap can corrupt it.
    private static String[][] table(Type type, Style style) {
        if (type == Type.STANDARD) {
            if (style == Style.FULL) {
                return new String[][] {
                    {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
                    {"{0}, {1}", "{0}, {1}", "{0}, and {1}", "{0} and {1}",
                     "{0}, {1}, and {2}"},
                    {"{0}, {1}", "{0}, {1}", "{0} y {1}", "{0} y {1}", "{0}, {1} y {2}"},
                    {"{0}, {1}", "{0}, {1}", "{0} und {1}", "{0} und {1}", "{0}, {1} und {2}"},
                    {"{0}, {1}", "{0}, {1}", "{0} et {1}", "{0} et {1}", "{0}, {1} et {2}"},
                    {"{0}\u3001{1}", "{0}\u3001{1}", "{0}\u3001{1}", "{0}\u3001{1}",
                     "{0}\u3001{1}\u3001{2}"},
                };
            }
            if (style == Style.SHORT) {
                return new String[][] {
                    {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
                    {"{0}, {1}", "{0}, {1}", "{0}, & {1}", "{0} & {1}", "{0}, {1}, & {2}"},
                    {"{0}, {1}", "{0}, {1}", "{0} y {1}", "{0} y {1}", "{0}, {1} y {2}"},
                    {"{0}, {1}", "{0}, {1}", "{0} und {1}", "{0} und {1}", "{0}, {1} und {2}"},
                    {"{0}, {1}", "{0}, {1}", "{0} et {1}", "{0} et {1}", "{0}, {1} et {2}"},
                    {"{0}\u3001{1}", "{0}\u3001{1}", "{0}\u3001{1}", "{0}\u3001{1}",
                     "{0}\u3001{1}\u3001{2}"},
                };
            }
            return new String[][] {
                {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
                {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} y {1}", "{0} y {1}", "{0}, {1} y {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} und {1}", "{0} und {1}", "{0}, {1} und {2}"},
                {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
                {"{0}\u3001{1}", "{0}\u3001{1}", "{0}\u3001{1}", "{0}\u3001{1}",
                 "{0}\u3001{1}\u3001{2}"},
            };
        }
        if (type == Type.OR) {
            // The three OR forms matches in the five Latin locales; the Japanese one uses
            // "\u307e\u305f\u306f" (mataha) in all three.
            return new String[][] {
                {"{0}, {1}", "{0}, {1}", "{0}, or {1}", "{0} or {1}", "{0}, {1}, or {2}"},
                {"{0}, {1}", "{0}, {1}", "{0}, or {1}", "{0} or {1}", "{0}, {1}, or {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} o {1}", "{0} o {1}", "{0}, {1} o {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} oder {1}", "{0} oder {1}", "{0}, {1} oder {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} ou {1}", "{0} ou {1}", "{0}, {1} ou {2}"},
                {"{0}\u3001{1}", "{0}\u3001{1}", "{0}\u3001\u307e\u305f\u306f{1}",
                 "{0}\u307e\u305f\u306f{1}", "{0}\u3001{1}\u3001\u307e\u305f\u306f{2}"},
            };
        }
        if (style == Style.FULL) {
            return new String[][] {
                {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
                {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} y {1}", "{0} y {1}", "{0}, {1} y {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} und {1}", "{0} und {1}", "{0}, {1} und {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} et {1}", "{0} et {1}", "{0}, {1} et {2}"},
                {"{0}\u3001{1}", "{0}\u3001{1}", "{0}\u3001{1}", "{0}\u3001{1}",
                 "{0}\u3001{1}\u3001{2}"},
            };
        }
        if (style == Style.SHORT) {
            return new String[][] {
                {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
                {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
                {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0} y {1}", "{0}, {1}, {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} und {1}", "{0}, {1}", "{0}, {1} und {2}"},
                {"{0}, {1}", "{0}, {1}", "{0} et {1}", "{0} et {1}", "{0}, {1} et {2}"},
                {"{0} {1}", "{0} {1}", "{0} {1}", "{0} {1}", "{0} {1} {2}"},
            };
        }
        return new String[][] {
            {"{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}", "{0}, {1}, {2}"},
            {"{0} {1}", "{0} {1}", "{0} {1}", "{0} {1}", "{0} {1} {2}"},
            {"{0} {1}", "{0} {1}", "{0} {1}", "{0} {1}", "{0} {1} {2}"},
            {"{0}, {1}", "{0}, {1}", "{0} und {1}", "{0} und {1}", "{0}, {1} und {2}"},
            {"{0} {1}", "{0} {1}", "{0} {1}", "{0} {1}", "{0} {1} {2}"},
            {"{0}{1}", "{0}{1}", "{0}{1}", "{0}{1}", "{0}{1}{2}"},
        };
    }

    /**
     * It builds a formatter with the five patterns given, in the order
     * {@code [start, middle, end, two, three]}.
     *
     * @throws IllegalArgumentException if the array does not have five entries, or if one of them
     *         does not reference the arguments it should. The validation is what stops a badly
     *         written pattern being discovered only when formatting, with a list in hand.
     */
    public static ListFormat getInstance(String[] patterns) {
        if (patterns == null) {
            throw new NullPointerException();
        }
        if (patterns.length != 5) {
            throw new IllegalArgumentException("Pattern array length should be 5");
        }
        String[] copy = new String[5];
        for (int i = 0; i < 5; i = i + 1) {
            copy[i] = patterns[i];
        }
        ListFormat.check(copy[ListFormat.START], 2, "start");
        ListFormat.check(copy[ListFormat.MIDDLE], 2, "middle");
        ListFormat.check(copy[ListFormat.END], 2, "end");
        ListFormat.check(copy[ListFormat.TWO], 2, "two");
        ListFormat.check(copy[ListFormat.THREE], 3, "three");
        return new ListFormat(Locale.ROOT, copy);
    }

    private static void check(String pattern, int count, String name) {
        if (pattern == null) {
            throw new NullPointerException();
        }
        for (int i = 0; i < count; i = i + 1) {
            if (pattern.indexOf("{" + Integer.toString(i) + "}") < 0) {
                throw new IllegalArgumentException("pattern for " + name + " is incorrect: "
                        + pattern);
            }
        }
    }

    /**
     * This formatter's locale. For one built with explicit patterns it is {@code ROOT}: the patterns
     * came from no locale and saying otherwise would attribute an origin to them.
     */
    public Locale getLocale() {
        return this.locale;
    }

    public String[] getPatterns() {
        String[] out = new String[5];
        for (int i = 0; i < 5; i = i + 1) {
            out[i] = this.patterns[i];
        }
        return out;
    }

    public String format(List<String> input) {
        return this.format(input, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        List<String> list = ListFormat.toList(obj);
        MessageFormat mf = new MessageFormat(this.patternFor(list.size()), this.locale);
        return mf.format(list.toArray(), toAppendTo, pos);
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        List<String> list = ListFormat.toList(obj);
        MessageFormat mf = new MessageFormat(this.patternFor(list.size()), this.locale);
        return mf.formatToCharacterIterator(list.toArray());
    }

    private static List<String> toList(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        List<String> out = new ArrayList<String>();
        if (obj instanceof List) {
            List<?> l = (List<?>) obj;
            for (int i = 0; i < l.size(); i = i + 1) {
                Object o = l.get(i);
                if (o == null) {
                    throw new NullPointerException();
                }
                out.add(o.toString());
            }
        } else if (obj instanceof Object[]) {
            Object[] arr = (Object[]) obj;
            for (int i = 0; i < arr.length; i = i + 1) {
                if (arr[i] == null) {
                    throw new NullPointerException();
                }
                out.add(arr[i].toString());
            }
        } else {
            throw new IllegalArgumentException("Cannot format given Object as a List");
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException("There should at least be one input string");
        }
        return out;
    }

    // The MessageFormat pattern that corresponds to N elements. One alone joins with nothing; two
    // and three have a pattern of their own; from four on it composes start + middle... + end.
    private String patternFor(int n) {
        if (n == 1) {
            return "{0}";
        }
        if (n == 2) {
            return this.patterns[ListFormat.TWO];
        }
        if (n == 3) {
            return this.patterns[ListFormat.THREE];
        }
        String acc = "{0}";
        for (int i = 1; i < n; i = i + 1) {
            String p;
            if (i == 1) {
                p = this.patterns[ListFormat.START];
            } else if (i == n - 1) {
                p = this.patterns[ListFormat.END];
            } else {
                p = this.patterns[ListFormat.MIDDLE];
            }
            acc = ListFormat.substitute(p, acc, "{" + Integer.toString(i) + "}");
        }
        return acc;
    }

    // Textual substitution of {0} and {1}, done in ONE pass: replacing {0} and then {1} over the
    // result would touch again the braces the first replacement had just inserted.
    private static String substitute(String pattern, String zero, String one) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < pattern.length()) {
            if (i + 2 < pattern.length() && pattern.charAt(i) == '{' && pattern.charAt(i + 2) == '}') {
                char d = pattern.charAt(i + 1);
                if (d == '0') {
                    sb.append(zero);
                    i = i + 3;
                    continue;
                }
                if (d == '1') {
                    sb.append(one);
                    i = i + 3;
                    continue;
                }
            }
            sb.append(pattern.charAt(i));
            i = i + 1;
        }
        return sb.toString();
    }

    /**
     * It recovers the list out of a text this formatter could have produced.
     *
     * <p>One element count after another is tried until one fits whole, starting from the largest
     * possible. Largest to smallest and not the other way round: with patterns where the middle
     * separator also appears inside the last pair, the short reading would fit just as well and would
     * swallow elements.
     */
    public List<String> parse(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Object r = this.parseObject(source, pos);
        if (r == null) {
            throw new ParseException("Parse failed", pos.getErrorIndex());
        }
        return (List<String>) r;
    }

    public Object parseObject(String source, ParsePosition parsePos) {
        if (source == null) {
            throw new NullPointerException();
        }
        int start = parsePos.getIndex();
        int max = source.length() + 1;
        for (int n = max; n >= 1; n = n - 1) {
            MessageFormat mf = new MessageFormat(this.patternFor(n), this.locale);
            ParsePosition p = new ParsePosition(start);
            Object[] got = mf.parse(source, p);
            if (got != null && p.getIndex() == source.length() && got.length == n) {
                List<String> out = new ArrayList<String>();
                boolean completo = true;
                for (int i = 0; i < n; i = i + 1) {
                    if (got[i] == null) {
                        completo = false;
                    } else {
                        out.add(got[i].toString());
                    }
                }
                if (completo) {
                    parsePos.setIndex(source.length());
                    return out;
                }
            }
        }
        parsePos.setErrorIndex(start);
        return null;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        ListFormat other = (ListFormat) obj;
        if (!this.locale.equals(other.locale)) {
            return false;
        }
        for (int i = 0; i < 5; i = i + 1) {
            if (!this.patterns[i].equals(other.patterns[i])) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = this.locale.hashCode();
        for (int i = 0; i < 5; i = i + 1) {
            h = h * 31 + this.patterns[i].hashCode();
        }
        return h;
    }

    public String toString() {
        return "ListFormat [locale: \"" + this.locale.toString()
                + "\", start: \"" + this.patterns[ListFormat.START]
                + "\", middle: \"" + this.patterns[ListFormat.MIDDLE]
                + "\", end: \"" + this.patterns[ListFormat.END]
                + "\", two: \"" + this.patterns[ListFormat.TWO]
                + "\", three: \"" + this.patterns[ListFormat.THREE] + "\"]";
    }
}
