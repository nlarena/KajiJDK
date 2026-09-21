package java.text;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * The compact formatter: {@code 1234} comes out {@code 1K} and {@code 1234567} comes out
 * {@code 1M}.
 *
 * <p><b>Why this class IS here and {@link NumberFormat}'s factories are not.</b> The compact
 * suffixes are CLDR data --"K", "mil", "\u4e07"-- and this library does not carry them. But this
 * class's constructors <em>receive the patterns from the caller</em>: whoever uses it says
 * {@code {"", "", "", "0K", "00K", "000K", "0M", ...}} and the class has nothing to invent. That is
 * why it can work honestly with no table, whereas
 * {@code NumberFormat.getCompactNumberInstance(locale, style)} --which has to produce those patterns
 * per locale-- cannot, and was left out.
 *
 * <p><b>How the pattern array is read.</b> The position IS the magnitude: index {@code i} governs
 * the numbers of {@code i+1} digits. An empty pattern means "at this magnitude nothing is compacted"
 * and the number comes out whole. The pattern's number of zeros says how much is divided out: at
 * index 3, {@code "0K"} (one zero) divides by {@code 10^3}, and at index 5, {@code "000K"} (three
 * zeros) divides by {@code 10^(5-3+1)}, that is, by a thousand as well. It is what makes 1,000,
 * 12,000 and 999,000 all be written in thousands with three different patterns.
 *
 * <p><b>Rounding can change the magnitude, and the choice has to be made again.</b> 999,999 falls
 * at index 5, is divided by a thousand and rounds to 1000 -- writing it there would give "1000K".
 * So, after rounding, which magnitude the number ended up at is looked at again and the step is
 * repeated: out comes "1M", which is what the JDK returns.
 *
 * <p>The long constructor's fourth argument is the locale's plural rules, in the CLDR's syntax
 * ({@code "one:i = 1 and v = 0"}). They serve the patterns that carry variants
 * ({@code "{one:0 mil other:0 mil}"}): the rule is evaluated against the already divided number and
 * the category is chosen. If no rule matches, {@code other} is used, which is the one the CLDR
 * guarantees in every locale.
 */
public final class CompactNumberFormat extends NumberFormat {

    private final String decimalPattern;
    private final DecimalFormatSymbols symbols;
    private final String[] compactPatterns;
    private final String pluralRules;
    private final DecimalFormat base;

    private RoundingMode roundingMode;
    private int groupingSize;
    private boolean strict;
    private boolean parseBigDecimal;

    public CompactNumberFormat(String decimalPattern, DecimalFormatSymbols symbols,
                               String[] compactPatterns) {
        this(decimalPattern, symbols, compactPatterns, "");
    }

    public CompactNumberFormat(String decimalPattern, DecimalFormatSymbols symbols,
                               String[] compactPatterns, String pluralRules) {
        if (decimalPattern == null || symbols == null || compactPatterns == null
                || pluralRules == null) {
            throw new NullPointerException();
        }
        this.decimalPattern = decimalPattern;
        this.symbols = (DecimalFormatSymbols) symbols.clone();
        this.compactPatterns = new String[compactPatterns.length];
        for (int i = 0; i < compactPatterns.length; i = i + 1) {
            this.compactPatterns[i] = compactPatterns[i];
        }
        this.pluralRules = pluralRules;
        this.base = new DecimalFormat(decimalPattern, this.symbols);
        this.roundingMode = RoundingMode.HALF_EVEN;
        this.groupingSize = 0;
        this.strict = false;
        this.parseBigDecimal = false;
        // A compact formatter neither groups nor shows decimals by default: "1K" and not "1.234K"
        // nor "1,2K". The decimal pattern it receives is used for the symbols and for the case where
        // the magnitude is NOT compacted, not to decide this.
        this.setGroupingUsed(false);
        this.setMinimumFractionDigits(0);
        this.setMaximumFractionDigits(0);
    }

    // ---- formatting -----------------------------------------------------------------------------

    public final StringBuffer format(Object number, StringBuffer toAppendTo, FieldPosition pos) {
        if (number instanceof BigDecimal) {
            return this.write((BigDecimal) number, toAppendTo, pos, null);
        }
        if (number instanceof BigInteger) {
            return this.write(new BigDecimal((BigInteger) number), toAppendTo, pos, null);
        }
        if (number instanceof Long || number instanceof Integer
                || number instanceof Short || number instanceof Byte) {
            return this.format(((Number) number).longValue(), toAppendTo, pos);
        }
        if (number instanceof Number) {
            return this.format(((Number) number).doubleValue(), toAppendTo, pos);
        }
        throw new IllegalArgumentException("Cannot format given Object as a Number");
    }

    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        if (Double.isNaN(number) || Double.isInfinite(number)) {
            // Neither NaN nor infinity has a magnitude, so there is no compact index to choose:
            // they are written by the decimal formatter, which does know their names in this
            // locale.
            return this.base.format(number, toAppendTo, pos);
        }
        return this.write(new BigDecimal(number), toAppendTo, pos, null);
    }

    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        return this.write(BigDecimal.valueOf(number), toAppendTo, pos, null);
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        if (!(obj instanceof Number)) {
            throw new IllegalArgumentException("Cannot format given Object as a Number");
        }
        FieldMarks marks = new FieldMarks();
        StringBuffer sb = new StringBuffer();
        BigDecimal v;
        if (obj instanceof BigDecimal) {
            v = (BigDecimal) obj;
        } else if (obj instanceof BigInteger) {
            v = new BigDecimal((BigInteger) obj);
        } else if (obj instanceof Long || obj instanceof Integer
                || obj instanceof Short || obj instanceof Byte) {
            v = BigDecimal.valueOf(((Number) obj).longValue());
        } else {
            v = new BigDecimal(((Number) obj).doubleValue());
        }
        this.write(v, sb, null, marks);
        return marks.iterator(sb.toString());
    }

    private StringBuffer write(BigDecimal value, StringBuffer out, FieldPosition pos,
                                  FieldMarks marks) {
        FieldMarks m = marks;
        if (m == null) {
            m = new FieldMarks();
        }
        int index = this.indexFor(value);
        String pattern = null;
        if (index >= 0 && index < this.compactPatterns.length) {
            pattern = this.compactPatterns[index];
        }
        if (pattern == null || pattern.length() == 0) {
            return this.base.format(value, out, pos);
        }
        // A pattern with neither prefix nor suffix --"{one:0 other:0}"-- **is not a compact form**:
        // it is how the CLDR writes "this magnitude is not compacted in this locale". German does not
        // compact below the million and Japanese does not compact the thousand, and both say so this
        // way. Compacting it anyway would give "1" where "1,000" belongs, which is exactly the wrong
        // number by three orders of magnitude.
        //
        // It is looked at before the loop and not after because the loop can only go up in magnitude
        // --rounding grows-- and the affixless entries are always the bottom ones.
        if (CompactNumberFormat.hasNoAffixes(pattern)) {
            return this.base.format(value, out, pos);
        }

        BigDecimal divided = value;
        int turns = 0;
        // Two turns at most: the first chooses by the original magnitude, the second by the one left
        // after rounding. A third can change nothing -- rounding an already rounded number to the
        // same scale leaves it as it is.
        while (turns < 2) {
            int zeros = CompactNumberFormat.zerosOf(pattern);
            int exp = index - zeros + 1;
            if (exp < 0) {
                exp = 0;
            }
            divided = value.movePointLeft(exp).setScale(this.getMaximumFractionDigits(),
                    this.roundingMode);
            BigDecimal rebuilt = divided.movePointRight(exp);
            int raised = this.indexFor(rebuilt);
            if (raised == index || raised < 0 || raised >= this.compactPatterns.length) {
                break;
            }
            String other = this.compactPatterns[raised];
            if (other == null || other.length() == 0) {
                break;
            }
            index = raised;
            pattern = other;
            turns = turns + 1;
        }

        String chosen = this.chooseVariant(pattern, divided);
        String prefix = CompactNumberFormat.affix(chosen, true);
        String suffix = CompactNumberFormat.affix(chosen, false);

        int base0 = out.length();
        StringBuilder sb = new StringBuilder();
        boolean negative = divided.signum() < 0;
        if (negative) {
            int d = sb.length();
            sb.append(this.symbols.getMinusSign());
            m.mark((AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.SIGN, -1, base0 + d, base0 + sb.length());
        }
        int dp = sb.length();
        sb.append(prefix);
        m.mark((AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.PREFIX, -1, base0 + dp, base0 + sb.length());

        DecimalFormat body = new DecimalFormat(this.decimalPattern, this.symbols);
        body.setGroupingUsed(this.isGroupingUsed());
        if (this.groupingSize > 0) {
            body.setGroupingSize(this.groupingSize);
        }
        body.setMaximumIntegerDigits(this.getMaximumIntegerDigits());
        body.setMinimumIntegerDigits(this.getMinimumIntegerDigits());
        body.setMaximumFractionDigits(this.getMaximumFractionDigits());
        body.setMinimumFractionDigits(this.getMinimumFractionDigits());
        body.setRoundingMode(this.roundingMode);
        int dn = sb.length();
        sb.append(body.format(divided.abs()));
        m.mark((AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.INTEGER, NumberFormat.INTEGER_FIELD,
                base0 + dn, base0 + sb.length());

        int ds = sb.length();
        sb.append(suffix);
        m.mark((AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.SUFFIX, -1, base0 + ds, base0 + sb.length());

        out.append(sb.toString());
        m.apply(pos);
        return out;
    }

    // The index is "how many integer digits it has, minus one". A zero falls at index 0 just like a
    // one: both have a single digit.
    private int indexFor(BigDecimal value) {
        BigDecimal abs = value.abs();
        if (abs.signum() == 0) {
            return 0;
        }
        String integerDigits = abs.setScale(0, RoundingMode.DOWN).toPlainString();
        int digits = integerDigits.length();
        if (integerDigits.equals("0")) {
            digits = 1;
        }
        int i = digits - 1;
        if (i >= this.compactPatterns.length) {
            i = this.compactPatterns.length - 1;
        }
        return i;
    }

    // The zeros are counted over ONE variant, not over the whole pattern: "{one:0 mil other:0
    // miles}" has two zeros written but divides by a thousand, not by a hundred. Every variant of a
    // compact pattern shares the magnitude --it is what makes them variants of one pattern-- so the
    // first one is enough.
    private static int zerosOf(String pattern) {
        String p = CompactNumberFormat.variant(pattern, null);
        int n = 0;
        boolean quoted = false;
        for (int i = 0; i < p.length(); i = i + 1) {
            char c = p.charAt(i);
            if (c == '\'') {
                quoted = !quoted;
            } else if (!quoted && c == '0') {
                n = n + 1;
            }
        }
        if (n == 0) {
            return 1;
        }
        return n;
    }

    // Whether the pattern contributes neither prefix nor suffix. It is looked at over ONE variant,
    // like `zerosOf`: a pattern's variants differ in the plural's text, not in having one at all.
    private static boolean hasNoAffixes(String pattern) {
        String p = CompactNumberFormat.variant(pattern, null);
        return CompactNumberFormat.affix(p, true).length() == 0
                && CompactNumberFormat.affix(p, false).length() == 0;
    }

    private static String affix(String pattern, boolean prefix) {
        StringBuilder sb = new StringBuilder();
        boolean quoted = false;
        boolean seenDigit = false;
        for (int i = 0; i < pattern.length(); i = i + 1) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                quoted = !quoted;
                continue;
            }
            if (!quoted && c == '0') {
                seenDigit = true;
                continue;
            }
            if (prefix && !seenDigit) {
                sb.append(c);
            } else if (!prefix && seenDigit) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // A pattern with variants is written "{cat:pattern cat:pattern}". With no variants it is
    // returned as it is, which is the three-argument constructors' case.
    private String chooseVariant(String pattern, BigDecimal value) {
        if (pattern.length() == 0 || pattern.charAt(0) != '{') {
            return pattern;
        }
        return CompactNumberFormat.variant(pattern,
                PluralRules.category(this.pluralRules, value));
    }

    /**
     * A category's variant inside a pattern with variants.
     *
     * @param category the category sought, or {@code null} to keep the first one. The {@code null}
     *                  is not a disguised "I do not know": it is used by {@code zerosOf}, which only
     *                  cares about the magnitude, and that is the same in every variant.
     */
    private static String variant(String pattern, String category) {
        if (pattern.length() == 0 || pattern.charAt(0) != '{') {
            return pattern;
        }
        String body = pattern.substring(1, pattern.length() - 1);
        String otherOne = null;
        int i = 0;
        while (i < body.length()) {
            int colon = body.indexOf(':', i);
            if (colon < 0) {
                break;
            }
            String cat = body.substring(i, colon).trim();
            int end = body.length();
            // A category's pattern ends where the next one starts, which is recognised by "word:"
            // -- the space alone is not enough, because a pattern can have spaces.
            for (int k = colon + 1; k < body.length(); k = k + 1) {
                if (body.charAt(k) == ':') {
                    int back = k - 1;
                    while (back > colon && body.charAt(back) != ' ') {
                        back = back - 1;
                    }
                    if (back > colon) {
                        end = back;
                        break;
                    }
                }
            }
            String patValue = body.substring(colon + 1, end).trim();
            if (category == null || cat.equals(category)) {
                return patValue;
            }
            if (cat.equals("other")) {
                otherOne = patValue;
            }
            i = end + 1;
            if (end >= body.length()) {
                break;
            }
        }
        if (otherOne != null) {
            return otherOne;
        }
        return "";
    }

    // ---- parsing --------------------------------------------------------------------------------

    /**
     * It reads a compact number.
     *
     * <p>The longest suffix that matches is sought and multiplied by its magnitude: {@code "12K"}
     * gives 12000. The longest and not the first, because the suffixes of one table contain each
     * other ({@code "mil"} and {@code "millones"} start alike) and keeping the first would give a
     * factor a thousand times smaller without anyone noticing.
     */
    public Number parse(String text, ParsePosition pos) {
        if (text == null) {
            throw new NullPointerException();
        }
        int start = pos.getIndex();
        ParsePosition tmp = new ParsePosition(start);
        Number n = this.base.parse(text, tmp);
        if (n == null || tmp.getIndex() == start) {
            pos.setErrorIndex(start);
            return null;
        }
        int after = tmp.getIndex();
        int bestExp = -1;
        int bestLength = -1;
        for (int i = 0; i < this.compactPatterns.length; i = i + 1) {
            String p = this.compactPatterns[i];
            if (p == null || p.length() == 0) {
                continue;
            }
            String chosen = this.chooseVariant(p, BigDecimal.valueOf(n.longValue()));
            String suffix = CompactNumberFormat.affix(chosen, false);
            if (suffix.length() == 0) {
                continue;
            }
            if (suffix.length() > bestLength && text.startsWith(suffix, after)) {
                bestLength = suffix.length();
                bestExp = i - CompactNumberFormat.zerosOf(chosen) + 1;
            }
        }
        BigDecimal v = new BigDecimal(n.toString());
        if (bestExp >= 0) {
            v = v.movePointRight(bestExp);
            after = after + bestLength;
        }
        pos.setIndex(after);
        if (this.parseBigDecimal) {
            return v;
        }
        BigDecimal clean = v.stripTrailingZeros();
        if (clean.scale() <= 0) {
            try {
                return Long.valueOf(clean.longValueExact());
            } catch (ArithmeticException e) {
                return Double.valueOf(v.doubleValue());
            }
        }
        return Double.valueOf(v.doubleValue());
    }

    // ---- state  ----

    public void setMaximumIntegerDigits(int newValue) {
        super.setMaximumIntegerDigits(newValue);
    }

    public void setMinimumIntegerDigits(int newValue) {
        super.setMinimumIntegerDigits(newValue);
    }

    public void setMinimumFractionDigits(int newValue) {
        super.setMinimumFractionDigits(newValue);
    }

    public void setMaximumFractionDigits(int newValue) {
        super.setMaximumFractionDigits(newValue);
    }

    public RoundingMode getRoundingMode() {
        return this.roundingMode;
    }

    public void setRoundingMode(RoundingMode roundingMode) {
        if (roundingMode == null) {
            throw new NullPointerException();
        }
        this.roundingMode = roundingMode;
    }

    public int getGroupingSize() {
        return this.groupingSize;
    }

    public void setGroupingSize(int newValue) {
        this.groupingSize = newValue;
    }

    public boolean isGroupingUsed() {
        return super.isGroupingUsed();
    }

    public void setGroupingUsed(boolean newValue) {
        super.setGroupingUsed(newValue);
    }

    public boolean isParseIntegerOnly() {
        return super.isParseIntegerOnly();
    }

    public void setParseIntegerOnly(boolean value) {
        super.setParseIntegerOnly(value);
    }

    public boolean isStrict() {
        return this.strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public boolean isParseBigDecimal() {
        return this.parseBigDecimal;
    }

    public void setParseBigDecimal(boolean newValue) {
        this.parseBigDecimal = newValue;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        CompactNumberFormat other = (CompactNumberFormat) obj;
        if (!this.decimalPattern.equals(other.decimalPattern)
                || !this.symbols.equals(other.symbols)
                || !this.pluralRules.equals(other.pluralRules)
                || this.roundingMode != other.roundingMode
                || this.groupingSize != other.groupingSize
                || this.strict != other.strict
                || this.parseBigDecimal != other.parseBigDecimal
                || this.compactPatterns.length != other.compactPatterns.length) {
            return false;
        }
        for (int i = 0; i < this.compactPatterns.length; i = i + 1) {
            if (!this.compactPatterns[i].equals(other.compactPatterns[i])) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        return this.decimalPattern.hashCode() * 31 + this.compactPatterns.length;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CompactNumberFormat [decimal pattern: \"");
        sb.append(this.decimalPattern);
        sb.append("\", compact patterns: \"[");
        for (int i = 0; i < this.compactPatterns.length; i = i + 1) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(this.compactPatterns[i]);
        }
        sb.append("]\"]");
        return sb.toString();
    }

    public CompactNumberFormat clone() {
        CompactNumberFormat c = new CompactNumberFormat(this.decimalPattern, this.symbols,
                this.compactPatterns, this.pluralRules);
        c.roundingMode = this.roundingMode;
        c.groupingSize = this.groupingSize;
        c.strict = this.strict;
        c.parseBigDecimal = this.parseBigDecimal;
        c.setGroupingUsed(this.isGroupingUsed());
        c.setParseIntegerOnly(this.isParseIntegerOnly());
        c.setMaximumIntegerDigits(this.getMaximumIntegerDigits());
        c.setMinimumIntegerDigits(this.getMinimumIntegerDigits());
        c.setMaximumFractionDigits(this.getMaximumFractionDigits());
        c.setMinimumFractionDigits(this.getMinimumFractionDigits());
        return c;
    }
}
