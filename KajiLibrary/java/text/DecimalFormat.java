package java.text;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Currency;

// KajiLibrary's java.text.DecimalFormat — pattern-based number formatting and parsing. A pattern
// like `#,##0.00` is parsed into a prefix/suffix, a minimum integer-digit count, grouping, and a
// min/max fraction-digit count; format(double) then renders a number to match, and parse() reads it
// back.
//
// The PATTERN and the SYMBOLS are separate concerns, and keeping them apart is the design:
// the pattern says the structure and is always written in the standard alphabet ('#', '0',
// ',', '.', '%', '\u00a4'), while DecimalFormatSymbols says which characters draw it. So the same
// `#,##0.00` renders 1,234.50 with US symbols and 1.234,50 with German ones, and applyPattern
// never has to know which locale it is in. `applyLocalizedPattern` is the back door for the
// opposite case --a pattern already written in the locale's alphabet-- and all it does is translate
// it to the standard one before applying it, so there are not two pattern parsers to maintain.
//
// The AFFIXES are kept twice, and it is worth knowing why: once as a PATTERN (with the `¤`
// unresolved) and once already EXPANDED. Were only the expanded one kept, changing the currency with
// setCurrency would leave the old symbol stuck on; were only the pattern kept, getPositivePrefix()
// would have to expand on every call and could not return a prefix set by hand with
// setPositivePrefix, which comes from no pattern at all.
//
// Rounding goes through java.math.BigDecimal, not through double arithmetic. That matters twice:
//   - a value on an exact rounding boundary now agrees with the JDK. format(2.675) with "0.00"
//     gives "2.67", because `new BigDecimal(double)` sees the double's EXACT binary value
//     (2.674999999999999822...), which is the same thing the JDK's formatter rounds;
//   - magnitudes past ~9.2e18 work. Computing the integer part with `(long) magnitude` SATURATES at
//     Long.MAX_VALUE and produces structurally corrupt output; digits come from
//     BigDecimal.toPlainString(), which has no such ceiling.
//
// The subset that remains: scientific notation (patterns with an 'E') is not here --neither when
// formatting nor when parsing-- and the pattern rejects it instead of ignoring it silently.
public class DecimalFormat extends NumberFormat {

    private static final char PAT_ZERO = '0';
    private static final char PAT_DIGIT = '#';
    private static final char PAT_GROUP = ',';
    private static final char PAT_DECIMAL = '.';
    private static final char PAT_SEPARATOR = ';';
    private static final char PAT_PERCENT = '%';
    // Written as escapes, just as in the symbol tables: they are two characters an editor can break
    // without it showing, and the '\u00a4' is invisible in several fonts as well.
    private static final char PAT_PERMILLE = '\u2030';
    private static final char PAT_CURRENCY = '\u00a4';
    private static final char PAT_MINUS = '-';

    // The affix's pattern (null if the affix was set by hand) and the affix already expanded.
    private String posPrefixPat;
    private String posSuffixPat;
    private String negPrefixPat;
    private String negSuffixPat;
    private String posPrefix;
    private String posSuffix;
    private String negPrefix;
    private String negSuffix;

    private int groupingSize;
    private int multiplier;
    private boolean decimalSeparatorAlwaysShown;
    private boolean parseBigDecimal;
    private boolean strict;
    private RoundingMode roundingMode;
    private DecimalFormatSymbols symbols;

    public DecimalFormat() {
        this.symbols = new DecimalFormatSymbols();
        this.init();
        this.applyPattern("#,##0.###");
    }

    public DecimalFormat(String pattern) {
        this.symbols = new DecimalFormatSymbols();
        this.init();
        this.applyPattern(pattern);
    }

    // The symbols are assigned first: applyPattern expands the affixes through them, so a pattern
    // applied before them would bake in the wrong currency symbol.
    public DecimalFormat(String pattern, DecimalFormatSymbols symbols) {
        this.symbols = (DecimalFormatSymbols) symbols.clone();
        this.init();
        this.applyPattern(pattern);
    }

    private void init() {
        this.groupingSize = 3;
        this.multiplier = 1;
        this.decimalSeparatorAlwaysShown = false;
        this.parseBigDecimal = false;
        this.strict = false;
        this.roundingMode = RoundingMode.HALF_EVEN;
    }

    public DecimalFormatSymbols getDecimalFormatSymbols() {
        return (DecimalFormatSymbols) this.symbols.clone();
    }

    // Copied on the way in and out, so a caller mutating its own instance cannot reach inside a
    // live formatter. It re-expands the affixes because the currency symbol and the minus sign come
    // from here.
    public void setDecimalFormatSymbols(DecimalFormatSymbols newSymbols) {
        this.symbols = (DecimalFormatSymbols) newSymbols.clone();
        this.expandAffixes();
    }

    // ---- pattern ----

    public void applyPattern(String pattern) {
        this.apply(pattern, false);
    }

    /**
     * It applies a pattern written in the locale's alphabet ({@code #.##0,00} in German).
     *
     * <p>It translates to the standard alphabet and delegates: keeping two pattern parsers --one per
     * alphabet-- is the sure way for the two to drift apart over time.
     */
    public void applyLocalizedPattern(String pattern) {
        this.apply(pattern, true);
    }

    private void apply(String pattern, boolean localized) {
        if (pattern == null) {
            throw new NullPointerException();
        }
        String pat = pattern;
        if (localized) {
            pat = this.toStandard(pattern);
        }
        this.parsePatternText(pat);
    }

    // It translates from the locale's alphabet to the standard one, respecting quoting: a decimal
    // separator INSIDE quotes is literal affix text and must not be touched.
    private String toStandard(String pat) {
        StringBuilder sb = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < pat.length(); i = i + 1) {
            char c = pat.charAt(i);
            if (c == '\'') {
                quoted = !quoted;
                sb.append(c);
            } else if (quoted) {
                sb.append(c);
            } else if (c == this.symbols.getZeroDigit()) {
                sb.append(DecimalFormat.PAT_ZERO);
            } else if (c == this.symbols.getDigit()) {
                sb.append(DecimalFormat.PAT_DIGIT);
            } else if (c == this.symbols.getGroupingSeparator()) {
                sb.append(DecimalFormat.PAT_GROUP);
            } else if (c == this.symbols.getDecimalSeparator()) {
                sb.append(DecimalFormat.PAT_DECIMAL);
            } else if (c == this.symbols.getPatternSeparator()) {
                sb.append(DecimalFormat.PAT_SEPARATOR);
            } else if (c == this.symbols.getPercent()) {
                sb.append(DecimalFormat.PAT_PERCENT);
            } else if (c == this.symbols.getPerMill()) {
                sb.append(DecimalFormat.PAT_PERMILLE);
            } else if (c == this.symbols.getMinusSign()) {
                sb.append(DecimalFormat.PAT_MINUS);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void parsePatternText(String pat) {
        int semi = this.subpatternEnd(pat);
        String pos = pat;
        String neg = null;
        if (semi >= 0) {
            pos = pat.substring(0, semi);
            neg = pat.substring(semi + 1, pat.length());
        }

        int ini = this.numberStart(pos);
        int end = this.numberEnd(pos, ini);
        this.posPrefixPat = pos.substring(0, ini);
        this.posSuffixPat = pos.substring(end, pos.length());
        String num = pos.substring(ini, end);

        // The digits are NOT counted as "the ones left of the point and the ones right of it",
        // which is the intuitive way and gives the wrong answer. They are counted in three stretches
        // --the '#' before the first '0', the '0's, and the '#' after the last '0'-- plus the point's
        // position INSIDE that sequence. It is the JDK's model and the only one that settles
        // "###.###" and "#." properly: in both there is no '0' at all and the minimum of integer
        // digits is still 1, not 0.
        int digitsLeft = 0;
        int zeros = 0;
        int digitsRight = 0;
        int dotPos = -1;
        int inGroup = -1;
        for (int i = 0; i < num.length(); i = i + 1) {
            char c = num.charAt(i);
            if (c == DecimalFormat.PAT_DIGIT) {
                if (zeros > 0) {
                    digitsRight = digitsRight + 1;
                } else {
                    digitsLeft = digitsLeft + 1;
                }
                if (inGroup >= 0 && dotPos < 0) {
                    inGroup = inGroup + 1;
                }
            } else if (c == DecimalFormat.PAT_ZERO) {
                if (digitsRight > 0) {
                    throw new IllegalArgumentException("Unexpected '0' in pattern: " + pat);
                }
                zeros = zeros + 1;
                if (inGroup >= 0 && dotPos < 0) {
                    inGroup = inGroup + 1;
                }
            } else if (c == DecimalFormat.PAT_GROUP) {
                inGroup = 0;
            } else if (c == DecimalFormat.PAT_DECIMAL) {
                if (dotPos < 0) {
                    dotPos = digitsLeft + zeros + digitsRight;
                }
            }
        }
        int digitsTotal = digitsLeft + zeros + digitsRight;

        // A pattern with no '0' at all and with a point ("###.###", "#.", ".###") is rewritten as if
        // it had a zero at the point's position. Without this the minimum of integers would come out
        // 0 and the formatting would lose the units digit.
        if (zeros == 0 && digitsLeft > 0 && dotPos >= 0) {
            int n = dotPos;
            if (n == 0) {
                n = 1;
            }
            digitsRight = digitsLeft - n;
            digitsLeft = n - 1;
            zeros = 1;
        }

        int effectiveDot = digitsTotal;
        if (dotPos >= 0) {
            effectiveDot = dotPos;
        }
        int minIntegerDigits = effectiveDot - digitsLeft;
        int maxFrac = 0;
        int minFrac = 0;
        if (dotPos >= 0) {
            maxFrac = digitsTotal - dotPos;
            minFrac = digitsLeft + zeros - dotPos;
        }

        // The group's size is the digits that followed the LAST comma: "#,##,##0" groups by three
        // because that is what the last one says, not the average of the two.
        this.setGroupingUsed(inGroup > 0);
        if (inGroup > 0) {
            this.groupingSize = inGroup;
        } else {
            this.groupingSize = 0;
        }

        // The order matters: the maxima first, the minima after. The setters adjust each other and
        // doing it the other way round would leave the maximum overwritten by a transient
        // minimum.
        this.setMaximumIntegerDigits(Integer.MAX_VALUE);
        this.setMinimumIntegerDigits(minIntegerDigits);
        this.setMaximumFractionDigits(maxFrac);
        this.setMinimumFractionDigits(minFrac);
        // The point is always shown when it sits at either end of the digit sequence: "#." asks for
        // the separator even with no decimals, and so does ".##".
        this.decimalSeparatorAlwaysShown = dotPos == 0 || dotPos == digitsTotal;

        if (neg != null) {
            // Of the negative subpattern the JDK uses only the AFFIXES: the number's shape is
            // dictated by the positive one, and accepting two different shapes would give a formatter
            // with two grammars.
            int numStart = this.numberStart(neg);
            int numEnd = this.numberEnd(neg, numStart);
            this.negPrefixPat = neg.substring(0, numStart);
            this.negSuffixPat = neg.substring(numEnd, neg.length());
        } else {
            this.negPrefixPat = DecimalFormat.PAT_MINUS + this.posPrefixPat;
            this.negSuffixPat = this.posSuffixPat;
        }

        this.multiplier = 1;
        if (this.has(this.posPrefixPat, DecimalFormat.PAT_PERCENT)
                || this.has(this.posSuffixPat, DecimalFormat.PAT_PERCENT)) {
            this.multiplier = 100;
        } else if (this.has(this.posPrefixPat, DecimalFormat.PAT_PERMILLE)
                || this.has(this.posSuffixPat, DecimalFormat.PAT_PERMILLE)) {
            this.multiplier = 1000;
        }

        this.expandAffixes();
    }

    // The ';' that separates subpatterns, skipping any that is quoted.
    private int subpatternEnd(String pat) {
        boolean quoted = false;
        for (int i = 0; i < pat.length(); i = i + 1) {
            char c = pat.charAt(i);
            if (c == '\'') {
                quoted = !quoted;
            } else if (!quoted && c == DecimalFormat.PAT_SEPARATOR) {
                return i;
            }
        }
        return -1;
    }

    private int numberStart(String s) {
        boolean quoted = false;
        for (int i = 0; i < s.length(); i = i + 1) {
            char c = s.charAt(i);
            if (c == '\'') {
                quoted = !quoted;
            } else if (!quoted && (c == DecimalFormat.PAT_DIGIT || c == DecimalFormat.PAT_ZERO)) {
                return i;
            } else if (!quoted && (c == 'E' || c == 'e')) {
                throw new IllegalArgumentException("Scientific notation is not supported: " + s);
            }
        }
        return s.length();
    }

    private int numberEnd(String s, int from) {
        int i = from;
        while (i < s.length() && DecimalFormat.belongsToNumber(s.charAt(i))) {
            i = i + 1;
        }
        if (i < s.length() && (s.charAt(i) == 'E' || s.charAt(i) == 'e')) {
            throw new IllegalArgumentException("Scientific notation is not supported: " + s);
        }
        return i;
    }

    private static boolean belongsToNumber(char c) {
        return c == DecimalFormat.PAT_DIGIT || c == DecimalFormat.PAT_ZERO
                || c == DecimalFormat.PAT_GROUP || c == DecimalFormat.PAT_DECIMAL;
    }

    private boolean has(String s, char c) {
        if (s == null) {
            return false;
        }
        boolean quoted = false;
        for (int i = 0; i < s.length(); i = i + 1) {
            char x = s.charAt(i);
            if (x == '\'') {
                quoted = !quoted;
            } else if (!quoted && x == c) {
                return true;
            }
        }
        return false;
    }

    private void expandAffixes() {
        if (this.posPrefixPat != null) {
            this.posPrefix = this.expand(this.posPrefixPat, null, 0);
        }
        if (this.posSuffixPat != null) {
            this.posSuffix = this.expand(this.posSuffixPat, null, 0);
        }
        if (this.negPrefixPat != null) {
            this.negPrefix = this.expand(this.negPrefixPat, null, 0);
        }
        if (this.negSuffixPat != null) {
            this.negSuffix = this.expand(this.negSuffixPat, null, 0);
        }
    }

    // It resolves an affix's pattern against the symbols and, on the way, marks the pieces that are
    // a field (currency, sign, percentage). The marks are only asked for when formatting; for the
    // affix's getter `marks` is passed as null.
    private String expand(String pattern, FieldMarks marks, int base) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                i = i + 1;
                if (i < pattern.length() && pattern.charAt(i) == '\'') {
                    sb.append('\'');
                    i = i + 1;
                } else {
                    while (i < pattern.length() && pattern.charAt(i) != '\'') {
                        sb.append(pattern.charAt(i));
                        i = i + 1;
                    }
                    i = i + 1;
                }
            } else if (c == DecimalFormat.PAT_CURRENCY) {
                int d = sb.length();
                if (i + 1 < pattern.length() && pattern.charAt(i + 1) == DecimalFormat.PAT_CURRENCY) {
                    sb.append(this.symbols.getInternationalCurrencySymbol());
                    i = i + 2;
                } else {
                    sb.append(this.symbols.getCurrencySymbol());
                    i = i + 1;
                }
                this.mark(marks, (AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.CURRENCY, base + d, base + sb.length());
            } else if (c == DecimalFormat.PAT_PERCENT) {
                int d = sb.length();
                sb.append(this.symbols.getPercent());
                this.mark(marks, (AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.PERCENT, base + d, base + sb.length());
                i = i + 1;
            } else if (c == DecimalFormat.PAT_PERMILLE) {
                int d = sb.length();
                sb.append(this.symbols.getPerMill());
                this.mark(marks, (AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.PERMILLE, base + d, base + sb.length());
                i = i + 1;
            } else if (c == DecimalFormat.PAT_MINUS) {
                int d = sb.length();
                sb.append(this.symbols.getMinusSign());
                this.mark(marks, (AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.SIGN, base + d, base + sb.length());
                i = i + 1;
            } else {
                sb.append(c);
                i = i + 1;
            }
        }
        return sb.toString();
    }

    private void mark(FieldMarks marks, AttributedCharacterIterator.Attribute field,
                        int d, int h) {
        if (marks != null) {
            marks.mark((AttributedCharacterIterator.Attribute) field, -1, d, h);
        }
    }

    /**
     * It synthesises the pattern describing the CURRENT state, which is not necessarily the one that
     * was applied: after a {@code setMinimumFractionDigits(5)} the pattern returned carries five
     * zeros. Returning the original string would be easier and would lie the moment somebody touched
     * a setter.
     */
    public String toPattern() {
        return this.synthesise(false);
    }

    public String toLocalizedPattern() {
        return this.synthesise(true);
    }

    private String synthesise(boolean localized) {
        char zero = DecimalFormat.PAT_ZERO;
        char digit = DecimalFormat.PAT_DIGIT;
        char group = DecimalFormat.PAT_GROUP;
        char decimal = DecimalFormat.PAT_DECIMAL;
        char separator = DecimalFormat.PAT_SEPARATOR;
        if (localized) {
            zero = this.symbols.getZeroDigit();
            digit = this.symbols.getDigit();
            group = this.symbols.getGroupingSeparator();
            decimal = this.symbols.getDecimalSeparator();
            separator = this.symbols.getPatternSeparator();
        }
        StringBuilder r = new StringBuilder();
        int j = 1;
        while (j >= 0) {
            if (j == 1) {
                r.append(this.affixForPattern(this.posPrefixPat, this.posPrefix, localized));
            } else {
                r.append(this.affixForPattern(this.negPrefixPat, this.negPrefix, localized));
            }
            int count = Math.max(this.groupingSize, this.getMinimumIntegerDigits()) + 1;
            int i = count;
            while (i > 0) {
                if (i != count && this.isGroupingUsed() && this.groupingSize != 0
                        && i % this.groupingSize == 0) {
                    r.append(group);
                }
                if (i <= this.getMinimumIntegerDigits()) {
                    r.append(zero);
                } else {
                    r.append(digit);
                }
                i = i - 1;
            }
            if (this.getMaximumFractionDigits() > 0 || this.decimalSeparatorAlwaysShown) {
                r.append(decimal);
            }
            i = 0;
            while (i < this.getMaximumFractionDigits()) {
                if (i < this.getMinimumFractionDigits()) {
                    r.append(zero);
                } else {
                    r.append(digit);
                }
                i = i + 1;
            }
            if (j == 1) {
                r.append(this.affixForPattern(this.posSuffixPat, this.posSuffix, localized));
                // The negative subpattern is left out when it says nothing new: just "minus and the
                // same".
                if (this.negativeIsDefault()) {
                    j = -1;
                } else {
                    r.append(separator);
                    j = 0;
                }
            } else {
                r.append(this.affixForPattern(this.negSuffixPat, this.negSuffix, localized));
                j = -1;
            }
        }
        return r.toString();
    }

    private boolean negativeIsDefault() {
        String expectedPrefix = DecimalFormat.PAT_MINUS + this.textOf(this.posPrefixPat, this.posPrefix);
        return this.textOf(this.negPrefixPat, this.negPrefix).equals(expectedPrefix)
                && this.textOf(this.negSuffixPat, this.negSuffix)
                        .equals(this.textOf(this.posSuffixPat, this.posSuffix));
    }

    private String textOf(String pattern, String literal) {
        if (pattern != null) {
            return pattern;
        }
        if (literal == null) {
            return "";
        }
        return literal;
    }

    // An affix set by hand has no pattern: it is requoted so that reapplying the synthesised pattern
    // gives the same affix and does not reinterpret it as currency or percentage.
    private String affixForPattern(String pattern, String literal, boolean localized) {
        if (pattern != null) {
            if (!localized) {
                return pattern;
            }
            return this.toLocalized(pattern);
        }
        if (literal == null || literal.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < literal.length(); i = i + 1) {
            char c = literal.charAt(i);
            if (c == '\'') {
                sb.append("''");
            } else if (DecimalFormat.belongsToNumber(c) || c == DecimalFormat.PAT_SEPARATOR
                    || c == DecimalFormat.PAT_PERCENT || c == DecimalFormat.PAT_PERMILLE
                    || c == DecimalFormat.PAT_CURRENCY || c == DecimalFormat.PAT_MINUS) {
                sb.append('\'').append(c).append('\'');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String toLocalized(String pattern) {
        StringBuilder sb = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < pattern.length(); i = i + 1) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                quoted = !quoted;
                sb.append(c);
            } else if (quoted) {
                sb.append(c);
            } else if (c == DecimalFormat.PAT_PERCENT) {
                sb.append(this.symbols.getPercent());
            } else if (c == DecimalFormat.PAT_PERMILLE) {
                sb.append(this.symbols.getPerMill());
            } else if (c == DecimalFormat.PAT_MINUS) {
                sb.append(this.symbols.getMinusSign());
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ---- formatting -----------------------------------------------------------------------------

    // Overridden so BigDecimal and BigInteger do NOT go through double: they are precisely the types
    // the caller chose in order not to lose digits, and converting them to double would lose them.
    public final StringBuffer format(Object number, StringBuffer toAppendTo, FieldPosition pos) {
        if (number instanceof BigDecimal) {
            return this.write((BigDecimal) number, toAppendTo, pos, null);
        }
        if (number instanceof BigInteger) {
            return this.write(new BigDecimal((BigInteger) number), toAppendTo, pos, null);
        }
        return super.format(number, toAppendTo, pos);
    }

    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        if (Double.isNaN(number)) {
            return this.writeSpecial(this.symbols.getNaN(), false, toAppendTo, pos, null);
        }
        if (Double.isInfinite(number)) {
            return this.writeSpecial(this.symbols.getInfinity(), number < 0.0, toAppendTo, pos, null);
        }
        // new BigDecimal(double), NOT valueOf: rounding has to see the double's EXACT binary value.
        return this.write(new BigDecimal(number), toAppendTo, pos, null);
    }

    // A long is exact all the way through — no double ever appears, so values past 2^53 keep every
    // digit. That is the whole reason NumberFormat declares a separate long seam.
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        return this.write(BigDecimal.valueOf(number), toAppendTo, pos, null);
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        FieldMarks marks = new FieldMarks();
        StringBuffer sb = new StringBuffer();
        if (obj instanceof BigDecimal) {
            this.write((BigDecimal) obj, sb, null, marks);
        } else if (obj instanceof BigInteger) {
            this.write(new BigDecimal((BigInteger) obj), sb, null, marks);
        } else if (obj instanceof Long || obj instanceof Integer
                || obj instanceof Short || obj instanceof Byte) {
            this.write(BigDecimal.valueOf(((Number) obj).longValue()), sb, null, marks);
        } else if (obj instanceof Number) {
            double d = ((Number) obj).doubleValue();
            if (Double.isNaN(d)) {
                this.writeSpecial(this.symbols.getNaN(), false, sb, null, marks);
            } else if (Double.isInfinite(d)) {
                this.writeSpecial(this.symbols.getInfinity(), d < 0.0, sb, null, marks);
            } else {
                this.write(new BigDecimal(d), sb, null, marks);
            }
        } else {
            throw new IllegalArgumentException("Cannot format given Object as a Number");
        }
        return marks.iterator(sb.toString());
    }

    private StringBuffer writeSpecial(String text, boolean negative, StringBuffer out,
                                          FieldPosition pos, FieldMarks marks) {
        int base = out.length();
        FieldMarks m = marks;
        if (m == null) {
            m = new FieldMarks();
        }
        StringBuilder sb = new StringBuilder();
        this.putAffix(sb, negative, true, m, base);
        int d = sb.length();
        sb.append(text);
        m.mark((AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.INTEGER, NumberFormat.INTEGER_FIELD, base + d, base + sb.length());
        this.putAffix(sb, negative, false, m, base);
        out.append(sb.toString());
        m.apply(pos);
        return out;
    }

    private StringBuffer write(BigDecimal value, StringBuffer out, FieldPosition pos,
                                  FieldMarks marks) {
        int base = out.length();
        FieldMarks m = marks;
        if (m == null) {
            m = new FieldMarks();
        }
        BigDecimal scaled = value;
        if (this.multiplier != 1) {
            scaled = scaled.multiply(BigDecimal.valueOf((long) this.multiplier));
        }
        boolean negative = scaled.signum() < 0;
        BigDecimal rounded = scaled.abs().setScale(this.getMaximumFractionDigits(),
                this.roundingMode);
        // At scale maxFrac, toPlainString() is exactly "<integers>.<maxFrac digits>" -- or just the
        // integers when maxFrac is 0. Splitting on the point IS the separation of digits.
        String plain = rounded.toPlainString();
        int dot = -1;
        for (int i = 0; i < plain.length(); i = i + 1) {
            if (plain.charAt(i) == '.') {
                dot = i;
                i = plain.length();
            }
        }
        String integerDigits = plain;
        String fraction = "";
        if (dot >= 0) {
            integerDigits = plain.substring(0, dot);
            fraction = plain.substring(dot + 1, plain.length());
        }

        StringBuilder sb = new StringBuilder();
        this.putAffix(sb, negative, true, m, base);

        String ent = integerDigits;
        while (ent.length() < this.getMinimumIntegerDigits()) {
            ent = "0" + ent;
        }
        // A maximum of integer digits below the number trims from the LEFT: the JDK keeps the least
        // significant digits, which are the ones the pattern asked to show.
        if (ent.length() > this.getMaximumIntegerDigits()) {
            ent = ent.substring(ent.length() - this.getMaximumIntegerDigits(), ent.length());
        }

        int integerStart = sb.length();
        boolean groups = this.isGroupingUsed() && this.groupingSize > 0;
        for (int i = 0; i < ent.length(); i = i + 1) {
            if (i > 0 && groups && (ent.length() - i) % this.groupingSize == 0) {
                int d = sb.length();
                sb.append(this.groupingSep());
                m.mark((AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.GROUPING_SEPARATOR, -1, base + d, base + sb.length());
            }
            sb.append(this.digit(ent.charAt(i)));
        }
        // The integer field covers the thousands separators AS WELL, which is why it is marked at
        // the end over the complete range: it is what the JDK reports, and what a caller wanting to
        // highlight "the integer part" needs.
        m.mark((AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.INTEGER, NumberFormat.INTEGER_FIELD,
                base + integerStart, base + sb.length());

        String frac = "";
        if (this.getMaximumFractionDigits() > 0) {
            frac = fraction;
            while (frac.length() < this.getMaximumFractionDigits()) {
                frac = frac + "0";
            }
            int end = frac.length();
            while (end > this.getMinimumFractionDigits() && frac.charAt(end - 1) == '0') {
                end = end - 1;
            }
            frac = frac.substring(0, end);
        }
        if (frac.length() > 0 || this.decimalSeparatorAlwaysShown) {
            int d = sb.length();
            sb.append(this.decimalSep());
            m.mark((AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.DECIMAL_SEPARATOR, -1, base + d, base + sb.length());
        }
        if (frac.length() > 0) {
            int d = sb.length();
            for (int i = 0; i < frac.length(); i = i + 1) {
                sb.append(this.digit(frac.charAt(i)));
            }
            m.mark((AttributedCharacterIterator.Attribute) java.text.NumberFormat.Field.FRACTION, NumberFormat.FRACTION_FIELD,
                    base + d, base + sb.length());
        }

        this.putAffix(sb, negative, false, m, base);
        out.append(sb.toString());
        m.apply(pos);
        return out;
    }

    // A locale's digits need not start at '0' (Arabic-Indic, Devanagari): the conversion is by
    // offset from the locale's zero.
    private char digit(char ascii) {
        return (char) (this.symbols.getZeroDigit() + (ascii - '0'));
    }

    private char decimalSep() {
        if (this.isCurrency()) {
            return this.symbols.getMonetaryDecimalSeparator();
        }
        return this.symbols.getDecimalSeparator();
    }

    private char groupingSep() {
        if (this.isCurrency()) {
            return this.symbols.getMonetaryGroupingSeparator();
        }
        return this.symbols.getGroupingSeparator();
    }

    // The monetary separators are used only if the pattern speaks of currency; a pattern set by hand
    // with no '\u00a4' does not want them even when the locale has different ones.
    private boolean isCurrency() {
        return this.has(this.posPrefixPat, DecimalFormat.PAT_CURRENCY)
                || this.has(this.posSuffixPat, DecimalFormat.PAT_CURRENCY);
    }

    private void putAffix(StringBuilder sb, boolean negative, boolean prefix,
                            FieldMarks m, int base) {
        String pattern;
        String literal;
        if (negative) {
            pattern = prefix ? this.negPrefixPat : this.negSuffixPat;
            literal = prefix ? this.negPrefix : this.negSuffix;
        } else {
            pattern = prefix ? this.posPrefixPat : this.posSuffixPat;
            literal = prefix ? this.posPrefix : this.posSuffix;
        }
        if (pattern != null) {
            sb.append(this.expand(pattern, m, base + sb.length()));
        } else if (literal != null) {
            sb.append(literal);
        }
    }

    // ---- parsing --------------------------------------------------------------------------------

    /**
     * It reads a number written with this pattern.
     *
     * <p>It returns a {@code Long} when the value is an integer and fits in 64 bits, and a
     * {@code Double} when it does not -- or a {@code BigDecimal} if that was asked for with
     * {@link #setParseBigDecimal}. The distinction is not cosmetic: always returning a
     * {@code Double} would lose digits of a large integer, which is exactly what the formatting took
     * care not to do.
     *
     * <p>In strict mode what lenient mode accepts is rejected: misplaced thousands separators, a
     * missing suffix, more than one decimal separator. Lenient mode, like the JDK's, stops at the
     * first character it does not understand and returns what it read up to there.
     */
    public Number parse(String text, ParsePosition pos) {
        if (text == null) {
            throw new NullPointerException();
        }
        int i = pos.getIndex();
        int n = text.length();
        if (i < 0 || i > n) {
            pos.setErrorIndex(i);
            return null;
        }

        boolean negative = false;
        int after = -1;
        // Both prefixes are tried and the longer wins: with "#;(#)" the positive prefix is empty and
        // always "matches", so keeping the first would never see the negative one.
        int posPos = this.matches(text, i, this.posPrefix);
        int posNeg = this.matches(text, i, this.negPrefix);
        if (posNeg > posPos) {
            negative = true;
            after = posNeg;
        } else if (posPos >= 0) {
            after = posPos;
        } else if (posNeg >= 0) {
            negative = true;
            after = posNeg;
        }
        if (after < 0) {
            pos.setErrorIndex(i);
            return null;
        }

        StringBuilder digits = new StringBuilder();
        int scale = 0;
        boolean seenDot = false;
        boolean hasDigit = false;
        int sinceLastGroup = -1;
        int groupsSeen = 0;
        int firstGroup = -1;
        boolean invalidGroup = false;
        char zero = this.symbols.getZeroDigit();
        char decSep = this.decimalSep();
        char groupSep = this.groupingSep();

        int j = after;
        while (j < n) {
            char c = text.charAt(j);
            int v = c - zero;
            if (v >= 0 && v <= 9) {
                digits.append((char) ('0' + v));
                hasDigit = true;
                if (seenDot) {
                    scale = scale + 1;
                } else if (sinceLastGroup >= 0) {
                    sinceLastGroup = sinceLastGroup + 1;
                }
                j = j + 1;
            } else if (c == decSep && !seenDot && !this.isParseIntegerOnly()) {
                // The last group before the point has to be complete; if it is not, the grouping was
                // decorative and in strict mode that is an error.
                if (sinceLastGroup >= 0 && sinceLastGroup != this.groupingSize) {
                    invalidGroup = true;
                }
                seenDot = true;
                j = j + 1;
            } else if (c == groupSep && !seenDot) {
                if (!hasDigit) {
                    invalidGroup = true;
                }
                if (groupsSeen == 0) {
                    firstGroup = digits.length();
                } else if (sinceLastGroup != this.groupingSize) {
                    invalidGroup = true;
                }
                groupsSeen = groupsSeen + 1;
                sinceLastGroup = 0;
                j = j + 1;
            } else {
                break;
            }
        }

        if (!hasDigit) {
            pos.setErrorIndex(j);
            return null;
        }
        if (groupsSeen > 0) {
            if (!this.isGroupingUsed() || this.groupingSize <= 0) {
                invalidGroup = true;
            } else {
                if (firstGroup < 1 || firstGroup > this.groupingSize) {
                    invalidGroup = true;
                }
                if (!seenDot && sinceLastGroup != this.groupingSize) {
                    invalidGroup = true;
                }
            }
        }

        String suffix = negative ? this.negSuffix : this.posSuffix;
        int afterSuffix = this.matches(text, j, suffix);
        if (this.strict) {
            if (invalidGroup || afterSuffix < 0) {
                pos.setErrorIndex(j);
                return null;
            }
            j = afterSuffix;
        } else if (afterSuffix >= 0) {
            j = afterSuffix;
        }

        BigDecimal value = new BigDecimal(new BigInteger(digits.toString()), scale);
        if (negative) {
            value = value.negate();
        }
        if (this.multiplier != 1) {
            // The division is done with enough scale not to lose digits of the quotient: a 123450%
            // divided by 100 is exactly 1234.5, and rounding it here would be inventing.
            value = value.divide(BigDecimal.valueOf((long) this.multiplier), scale + 4,
                    RoundingMode.HALF_EVEN).stripTrailingZeros();
        }
        pos.setIndex(j);
        return this.wrap(value);
    }

    private Number wrap(BigDecimal value) {
        if (this.parseBigDecimal) {
            return value;
        }
        BigDecimal clean = value.stripTrailingZeros();
        if (clean.scale() <= 0) {
            try {
                return Long.valueOf(clean.longValueExact());
            } catch (ArithmeticException e) {
                return Double.valueOf(value.doubleValue());
            }
        }
        return Double.valueOf(value.doubleValue());
    }

    // -1 if it does not match; if it does, the index just after. An empty affix always matches and
    // returns the same index, which is what makes a pattern with no prefix work.
    private int matches(String text, int from, String affix) {
        if (affix == null || affix.length() == 0) {
            return from;
        }
        if (from + affix.length() > text.length()) {
            return -1;
        }
        for (int k = 0; k < affix.length(); k = k + 1) {
            if (text.charAt(from + k) != affix.charAt(k)) {
                return -1;
            }
        }
        return from + affix.length();
    }

    // ---- state  ----

    public String getPositivePrefix() {
        return this.posPrefix;
    }

    public void setPositivePrefix(String newValue) {
        this.posPrefix = newValue;
        this.posPrefixPat = null;
    }

    public String getPositiveSuffix() {
        return this.posSuffix;
    }

    public void setPositiveSuffix(String newValue) {
        this.posSuffix = newValue;
        this.posSuffixPat = null;
    }

    public String getNegativePrefix() {
        return this.negPrefix;
    }

    public void setNegativePrefix(String newValue) {
        this.negPrefix = newValue;
        this.negPrefixPat = null;
    }

    public String getNegativeSuffix() {
        return this.negSuffix;
    }

    public void setNegativeSuffix(String newValue) {
        this.negSuffix = newValue;
        this.negSuffixPat = null;
    }

    public int getMultiplier() {
        return this.multiplier;
    }

    public void setMultiplier(int newValue) {
        this.multiplier = newValue;
    }

    public int getGroupingSize() {
        return this.groupingSize;
    }

    public void setGroupingSize(int newValue) {
        this.groupingSize = newValue;
    }

    public boolean isDecimalSeparatorAlwaysShown() {
        return this.decimalSeparatorAlwaysShown;
    }

    public void setDecimalSeparatorAlwaysShown(boolean newValue) {
        this.decimalSeparatorAlwaysShown = newValue;
    }

    public boolean isParseBigDecimal() {
        return this.parseBigDecimal;
    }

    public void setParseBigDecimal(boolean newValue) {
        this.parseBigDecimal = newValue;
    }

    public boolean isStrict() {
        return this.strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
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

    // Overridden because MessageFormat.toPattern() tells subformats apart by comparing them against
    // what the locale's factories return: with the inherited equals --which looks only at the digit
    // counts-- a "#,##0.00" set by hand would pass itself off as the locale's getNumberInstance.
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        DecimalFormat other = (DecimalFormat) obj;
        return this.toPattern().equals(other.toPattern())
                && this.multiplier == other.multiplier
                && this.groupingSize == other.groupingSize
                && this.decimalSeparatorAlwaysShown == other.decimalSeparatorAlwaysShown
                && this.parseBigDecimal == other.parseBigDecimal
                && this.strict == other.strict
                && this.roundingMode == other.roundingMode
                && this.symbols.equals(other.symbols);
    }

    public int hashCode() {
        return super.hashCode() * 37 + this.toPattern().hashCode();
    }

    public Currency getCurrency() {
        return this.symbols.getCurrency();
    }

    /**
     * It changes the currency, and with it the symbol the pattern's {@code ¤} resolves to.
     *
     * <p>It does NOT touch the number of decimals: the pattern has already fixed it, and the yen or
     * the dinar have their own. Changing it silently would make an explicit pattern stop counting.
     */
    public void setCurrency(Currency currency) {
        if (currency == null) {
            throw new NullPointerException();
        }
        this.symbols.setCurrency(currency);
        this.expandAffixes();
    }
}
