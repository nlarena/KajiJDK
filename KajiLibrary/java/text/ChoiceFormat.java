package java.text;

/**
 * Picks one of several strings according to which numeric interval a number falls into.
 *
 * <p>It exists for the plural problem. "There are 3 files" is easy; "there is 1 file" and "there
 * are no files" are the same sentence with a different word, and a formatter that only knows how to
 * render the number cannot produce them. {@code ChoiceFormat} attaches a string to each of a set of
 * ascending limits and, given a number, hands back the string for the interval that contains it:
 *
 * <pre>
 *     double[] limits = {0, 1, 2};
 *     String[] names  = {"no files", "one file", "many files"};
 *     ChoiceFormat f = new ChoiceFormat(limits, names);
 *     f.format(0);   // "no files"
 *     f.format(1);   // "one file"
 *     f.format(7);   // "many files"
 * </pre>
 *
 * <p>The same thing is written as a <em>pattern</em>, which is how {@link MessageFormat} embeds it:
 * {@code "0#no files|1#one file|2#many files"}. Each segment is <var>limit</var> <var>relation</var>
 * <var>text</var>, separated by {@code |}. The relation is {@code #} or {@code \u2264} for
 * "&gt;= limit" and {@code <} for "&gt; limit" -- and the second form is not a separate mode, it is
 * stored by bumping the limit to the very next representable {@code double}, which is why
 * {@link #nextDouble(double)} is part of the public API of a formatting class.
 *
 * <p>Lookup is a search for the <em>last</em> limit that the number reaches, so the intervals must
 * be given in ascending order; a number below every limit gets the first string rather than an
 * error, and NaN -- which compares false against everything -- lands there too.
 *
 * @implNote A KajiLibrary subset in three places, each forced by a missing dependency rather than
 *           by a shortcut:
 *           <ul>
 *           <li>{@code formatToCharacterIterator} is absent package-wide: it returns an
 *               {@code AttributedCharacterIterator}, whose API is built on the nested type
 *               {@code AttributedCharacterIterator.Attribute}, and a cross-file nested type does not
 *               resolve (finding #101).</li>
 *           <li>{@code parseObject(String, ParsePosition)} is not inherited, because this subset's
 *               {@link Format} declares no parsing half; {@link #parse(String, ParsePosition)} is
 *               declared here directly and has the JDK's signature and behaviour.</li>
 *           <li>{@code serialVersionUID} and {@code readObject} are omitted along with the rest of
 *               serialization, which this library does not implement.</li>
 *           </ul>
 *
 * @implNote {@link #nextDouble(double, boolean)} is computed by <em>bisection</em>, not by
 *           incrementing the bit pattern as the JDK does. {@code Double.longBitsToDouble} does not
 *           exist in this library and there is no way to build a {@code double} back out of a
 *           {@code long} without it. See that method for why the bisection lands on exactly the
 *           adjacent value and not merely near it.
 */
public class ChoiceFormat extends NumberFormat {

    private double[] choiceLimits;
    private String[] choiceFormats;
    private boolean strict;

    /**
     * Builds a formatter from a pattern.
     *
     * @param newPattern the pattern, as described in the class documentation
     * @throws IllegalArgumentException if the pattern is malformed or its limits do not ascend
     */
    public ChoiceFormat(String newPattern) {
        this.applyPatternImpl(newPattern);
    }

    /**
     * Builds a formatter from parallel arrays of limits and strings.
     *
     * @param limits the ascending interval lower bounds
     * @param formats the string for each interval
     * @throws IllegalArgumentException if the arrays have different lengths
     * @throws NullPointerException if either array is {@code null}
     */
    public ChoiceFormat(double[] limits, String[] formats) {
        this.setChoicesImpl(limits, formats);
    }

    /**
     * Replaces this formatter's choices with those described by a pattern.
     *
     * @param newPattern the pattern, as described in the class documentation
     * @throws IllegalArgumentException if the pattern is malformed or its limits do not ascend
     */
    public void applyPattern(String newPattern) {
        this.applyPatternImpl(newPattern);
    }

    // Kept separate from applyPattern for the same reason the JDK keeps it separate: the constructor
    // must not call an overridable method, or a subclass sees a half-built object.
    private void applyPatternImpl(String newPattern) {
        // Two accumulators: segment 0 collects the limit being read, segment 1 the text after the
        // relation character. `part` says which one the current character belongs to, and the
        // relation and `|` characters are what move between them.
        StringBuffer segment0 = new StringBuffer();
        StringBuffer segment1 = new StringBuffer();

        double[] limits = new double[8];
        String[] formats = new String[8];
        int count = 0;

        int part = 0;
        double startValue = 0.0;
        // NaN, so that the very first `startValue <= oldStartValue` order check cannot fire: every
        // comparison against NaN is false.
        double oldStartValue = ChoiceFormat.nan();
        boolean inQuote = false;

        for (int i = 0; i < newPattern.length(); ++i) {
            char ch = newPattern.charAt(i);
            if (ch == '\'') {
                // "''" is a literal quote; a lone quote toggles the literal region.
                if (i + 1 < newPattern.length() && newPattern.charAt(i + 1) == ch) {
                    if (part == 0) {
                        segment0.append(ch);
                    } else {
                        segment1.append(ch);
                    }
                    ++i;
                } else {
                    inQuote = !inQuote;
                }
            } else if (inQuote) {
                if (part == 0) {
                    segment0.append(ch);
                } else {
                    segment1.append(ch);
                }
            } else if (ch == '<' || ch == '#' || ch == '\u2264') {
                if (segment0.length() == 0) {
                    throw new IllegalArgumentException(
                            "Each interval must contain a number before a format");
                }
                startValue = ChoiceFormat.stringToNum(segment0.toString());
                // `<` is stored as "the next double up", so that the ordinary >= test at format
                // time implements a strict >. This is the whole reason nextDouble is public API.
                if (ch == '<' && startValue != ChoiceFormat.inf()
                        && startValue != -ChoiceFormat.inf()) {
                    startValue = ChoiceFormat.nextDouble(startValue);
                }
                if (startValue <= oldStartValue) {
                    throw new IllegalArgumentException(
                            "Incorrect order of intervals, must be in ascending order");
                }
                segment0.setLength(0);
                part = 1;
            } else if (ch == '|') {
                if (count == limits.length) {
                    limits = ChoiceFormat.growDoubles(limits);
                    formats = ChoiceFormat.growStrings(formats);
                }
                limits[count] = startValue;
                formats[count] = segment1.toString();
                count = count + 1;
                oldStartValue = startValue;
                segment1.setLength(0);
                part = 0;
            } else {
                if (part == 0) {
                    segment0.append(ch);
                } else {
                    segment1.append(ch);
                }
            }
        }

        // The trailing segment has no `|` to close it.
        if (part == 1) {
            if (count == limits.length) {
                limits = ChoiceFormat.growDoubles(limits);
                formats = ChoiceFormat.growStrings(formats);
            }
            limits[count] = startValue;
            formats[count] = segment1.toString();
            count = count + 1;
        }

        double[] finalLimits = new double[count];
        String[] finalFormats = new String[count];
        for (int i = 0; i < count; ++i) {
            finalLimits[i] = limits[i];
            finalFormats[i] = formats[i];
        }
        this.choiceLimits = finalLimits;
        this.choiceFormats = finalFormats;
    }

    /**
     * Returns a pattern that would rebuild this formatter.
     *
     * @return the pattern string
     */
    public String toPattern() {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < this.choiceLimits.length; ++i) {
            if (i != 0) {
                result.append('|');
            }
            // Which of the two spellings -- `limit#` or `(limit-1ulp)<` -- is the one a human wrote?
            // Whichever of the two numbers is closer to a whole number, because a hand-written
            // pattern says "1#" and a `<` pattern round-trips as the ulp below the next integer.
            double less = ChoiceFormat.previousDouble(this.choiceLimits[i]);
            double tryLessOrEqual = ChoiceFormat.distanceToInteger(this.choiceLimits[i]);
            double tryLess = ChoiceFormat.distanceToInteger(less);
            if (tryLessOrEqual < tryLess) {
                result.append(Double.toString(this.choiceLimits[i]));
                result.append('#');
            } else {
                if (this.choiceLimits[i] == ChoiceFormat.inf()) {
                    result.append("\u221e");
                } else if (this.choiceLimits[i] == -ChoiceFormat.inf()) {
                    result.append("-\u221e");
                } else {
                    result.append(Double.toString(less));
                }
                result.append('<');
            }
            // The text has to come back out as text, so any character that would be read as syntax
            // is wrapped in quotes.
            String text = this.choiceFormats[i];
            boolean needQuote = ChoiceFormat.indexOfChar(text, '<') >= 0
                    || ChoiceFormat.indexOfChar(text, '#') >= 0
                    || ChoiceFormat.indexOfChar(text, '\u2264') >= 0
                    || ChoiceFormat.indexOfChar(text, '|') >= 0;
            if (needQuote) {
                result.append('\'');
            }
            if (ChoiceFormat.indexOfChar(text, '\'') < 0) {
                result.append(text);
            } else {
                for (int j = 0; j < text.length(); ++j) {
                    char c = text.charAt(j);
                    result.append(c);
                    if (c == '\'') {
                        result.append(c);
                    }
                }
            }
            if (needQuote) {
                result.append('\'');
            }
        }
        return result.toString();
    }

    /**
     * Replaces this formatter's choices.
     *
     * @param limits the ascending interval lower bounds
     * @param formats the string for each interval
     * @throws IllegalArgumentException if the arrays have different lengths
     * @throws NullPointerException if either array is {@code null}
     */
    public void setChoices(double[] limits, String[] formats) {
        this.setChoicesImpl(limits, formats);
    }

    private void setChoicesImpl(double[] limits, String[] formats) {
        if (limits.length != formats.length) {
            throw new IllegalArgumentException(
                    "Array and limit arrays must be of the same length.");
        }
        double[] copiedLimits = new double[limits.length];
        String[] copiedFormats = new String[formats.length];
        for (int i = 0; i < limits.length; ++i) {
            copiedLimits[i] = limits[i];
            copiedFormats[i] = formats[i];
        }
        this.choiceLimits = copiedLimits;
        this.choiceFormats = copiedFormats;
    }

    /**
     * Returns a copy of the interval lower bounds.
     *
     * @return the limits, ascending
     */
    public double[] getLimits() {
        double[] copy = new double[this.choiceLimits.length];
        for (int i = 0; i < copy.length; ++i) {
            copy[i] = this.choiceLimits[i];
        }
        return copy;
    }

    /**
     * Returns a copy of the strings, one per interval.
     *
     * @return the strings, in the same order as {@link #getLimits}
     * @implNote The JDK declares this {@code Object[]}, not {@code String[]}, because the field it
     *           returns was once allowed to hold {@link Format} objects. The declared type is kept.
     */
    public Object[] getFormats() {
        Object[] copy = new Object[this.choiceFormats.length];
        for (int i = 0; i < copy.length; ++i) {
            copy[i] = this.choiceFormats[i];
        }
        return copy;
    }

    /**
     * Appends the string for the interval containing {@code number}.
     *
     * @param number the value to look up
     * @param toAppendTo the buffer to append to
     * @param status unused; a choice has no sub-fields to report
     * @return {@code toAppendTo}
     */
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition status) {
        return this.format((double) number, toAppendTo, status);
    }

    /**
     * Appends the string for the interval containing {@code number}.
     *
     * @param number the value to look up
     * @param toAppendTo the buffer to append to
     * @param status unused; a choice has no sub-fields to report
     * @return {@code toAppendTo}
     */
    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition status) {
        // Walk up while the number still reaches the limit, then step back one: the answer is the
        // LAST interval entered. Written as `!(number >= limit)` rather than `number < limit` so
        // that NaN -- false against both -- stops the walk at 0 instead of running off the end.
        int i;
        for (i = 0; i < this.choiceLimits.length; ++i) {
            if (!(number >= this.choiceLimits[i])) {
                break;
            }
        }
        --i;
        if (i < 0) {
            i = 0;
        }
        return toAppendTo.append(this.choiceFormats[i]);
    }

    /**
     * Reads one of the choice strings and returns the limit it stands for.
     *
     * <p>The longest match wins, not the first: {@code "one"} and {@code "one file"} can both start
     * at the same offset, and returning the shorter would strand the rest of the text.
     *
     * @param text the text to read
     * @param status where to start; updated to the end of the match, or its error index set
     * @return the matching limit boxed as a {@code Double}, or {@code Double} NaN if nothing matched
     */
    public Number parse(String text, ParsePosition status) {
        int start = status.getIndex();
        int furthest = start;
        double bestNumber = ChoiceFormat.nan();
        for (int i = 0; i < this.choiceFormats.length; ++i) {
            String tempString = this.choiceFormats[i];
            if (ChoiceFormat.regionMatches(text, start, tempString)) {
                int end = start + tempString.length();
                if (end > furthest) {
                    furthest = end;
                    bestNumber = this.choiceLimits[i];
                    if (furthest == text.length()) {
                        break;
                    }
                }
            }
        }
        status.setIndex(furthest);
        if (status.getIndex() == start) {
            status.setErrorIndex(furthest);
        }
        return Double.valueOf(bestNumber);
    }

    /**
     * Reports whether this formatter parses strictly.
     *
     * @return the strict-parsing flag
     * @implNote The flag is stored and reported, and this subset's {@link #parse} does not yet vary
     *           its behaviour on it. Declaring the pair is what lets calling code compile; making
     *           the strict mode bite is a change to {@code parse} only.
     */
    public boolean isStrict() {
        return this.strict;
    }

    /**
     * Sets whether this formatter parses strictly.
     *
     * @param strict the flag
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    /**
     * Returns the smallest {@code double} strictly greater than {@code d}.
     *
     * @param d the starting value
     * @return the adjacent value above
     */
    public static final double nextDouble(double d) {
        return ChoiceFormat.nextDouble(d, true);
    }

    /**
     * Returns the largest {@code double} strictly less than {@code d}.
     *
     * @param d the starting value
     * @return the adjacent value below
     */
    public static final double previousDouble(double d) {
        return ChoiceFormat.nextDouble(d, false);
    }

    /**
     * Returns the {@code double} adjacent to {@code d} in the given direction.
     *
     * @param d the starting value
     * @param positive {@code true} for the value above, {@code false} for the value below
     * @return the adjacent value
     *
     * @implNote The JDK does this in three instructions: take the bit pattern with
     *           {@code doubleToLongBits}, add or subtract one from the magnitude, and turn it back
     *           into a {@code double} with {@code longBitsToDouble}. This library has the first
     *           call and not the third, and a {@code double} cannot be rebuilt from a {@code long}
     *           without it -- so the value is found by <em>bisecting</em> instead.
     *
     *           <p>Bisection lands on exactly the adjacent value, not merely near it, and the
     *           reason is that the search runs in the same arithmetic it is searching. Starting
     *           from {@code lo = d} and {@code hi = 2d}, each midpoint is rounded to a
     *           representable {@code double}; the interval shrinks until {@code lo} and {@code hi}
     *           are adjacent, at which point the computed midpoint is forced to equal one of them
     *           and cannot lie strictly between. That is the termination test, and it is exact:
     *           "no double lies between these two" is precisely the definition of adjacent. Since
     *           the gap between neighbours is about {@code d * 2^-52}, halving an interval of size
     *           {@code d} reaches it in roughly 52 steps.
     *
     *           <p>Zero is the one case the doubling trick cannot start, and it is handled by
     *           halving 1.0 until the next halving underflows -- which is the definition of the
     *           smallest subnormal, the value {@code longBitsToDouble(1L)} would have produced.
     */
    public static double nextDouble(double d, boolean positive) {
        // NaN has no neighbours.
        if (d != d) {
            return d;
        }
        if (d == 0.0) {
            double tiny = ChoiceFormat.smallestSubnormal();
            return positive ? tiny : -tiny;
        }
        if (d < 0.0) {
            // Below zero the number line runs the other way, so mirror and recurse once.
            return -ChoiceFormat.nextDouble(-d, !positive);
        }
        // From here d > 0.
        if (positive) {
            if (d == ChoiceFormat.inf()) {
                return d;
            }
            double lo = d;
            double hi = d * 2.0;
            if (hi == ChoiceFormat.inf()) {
                // d is within one doubling of overflow; the neighbour above is still finite, and
                // the largest finite double is a valid upper bound for the search.
                hi = ChoiceFormat.maxValue();
                if (!(hi > lo)) {
                    return ChoiceFormat.inf();
                }
            }
            while (true) {
                double mid = lo + (hi - lo) * 0.5;
                if (mid <= lo || mid >= hi) {
                    return hi;
                }
                hi = mid;
            }
        }
        if (d == ChoiceFormat.inf()) {
            return ChoiceFormat.maxValue();
        }
        double lo = d * 0.5;
        double hi = d;
        if (lo == 0.0) {
            // d is subnormal enough that halving underflows; its neighbour below is zero or the
            // next subnormal, and bisecting from zero finds it.
            lo = 0.0;
        }
        while (true) {
            double mid = lo + (hi - lo) * 0.5;
            if (mid <= lo || mid >= hi) {
                return lo;
            }
            lo = mid;
        }
    }

    /**
     * Returns a copy of this formatter.
     *
     * @return an independent copy with the same choices
     * @implNote Built by construction rather than by {@code super.clone()}, because
     *           {@code Object.clone} does not exist in this library.
     */
    public Object clone() {
        ChoiceFormat other = new ChoiceFormat(this.choiceLimits, this.choiceFormats);
        other.strict = this.strict;
        return other;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        int result = this.choiceLimits.length;
        if (this.choiceFormats.length > 0) {
            // Enough for reasonable distribution: the JDK hashes only the last string too.
            result = result ^ this.choiceFormats[this.choiceFormats.length - 1].hashCode();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ChoiceFormat)) {
            return false;
        }
        ChoiceFormat other = (ChoiceFormat) obj;
        if (this.choiceLimits.length != other.choiceLimits.length) {
            return false;
        }
        for (int i = 0; i < this.choiceLimits.length; ++i) {
            if (this.choiceLimits[i] != other.choiceLimits[i]) {
                return false;
            }
            if (!this.choiceFormats[i].equals(other.choiceFormats[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "java.text.ChoiceFormat[pattern=" + this.toPattern() + "]";
    }

    // ---- private helpers ------------------------------------------------------------------
    //
    // Everything below stands in for a java.lang method this library does not have. They are
    // private, so they do not widen the public surface; each says which method it replaces.

    // Replaces Double.parseDouble, which does not exist here. Also handles the two infinity
    // spellings the JDK's stringToNum special-cases.
    private static double stringToNum(String str) {
        if (str.equals("\u221e")) {
            return ChoiceFormat.inf();
        }
        if (str.equals("-\u221e")) {
            return -ChoiceFormat.inf();
        }
        return ChoiceFormat.parseDouble(str);
    }

    // A decimal-literal reader: [+-] digits [. digits] [eE [+-] digits].
    //
    // It accumulates digits into a long and applies the decimal exponent by repeated multiplication,
    // which is NOT the correctly-rounded algorithm Double.parseDouble uses -- a value needing more
    // than 17 significant digits can land one ulp off. That is a real difference and it is written
    // down rather than hidden; for the numbers that appear in choice patterns (small integers, and
    // occasionally a decimal) the result is exact, because both the mantissa and the power of ten
    // are exactly representable.
    private static double parseDouble(String str) {
        int len = str.length();
        int i = 0;
        boolean negative = false;
        if (i < len && (str.charAt(i) == '+' || str.charAt(i) == '-')) {
            negative = str.charAt(i) == '-';
            ++i;
        }
        long mantissa = 0L;
        int digits = 0;
        int exponent = 0;
        boolean any = false;
        while (i < len && str.charAt(i) >= '0' && str.charAt(i) <= '9') {
            any = true;
            if (digits < 18) {
                mantissa = mantissa * 10L + (long) (str.charAt(i) - '0');
                digits = digits + 1;
            } else {
                // Past the precision a long can hold, further integer digits only scale.
                exponent = exponent + 1;
            }
            ++i;
        }
        if (i < len && str.charAt(i) == '.') {
            ++i;
            while (i < len && str.charAt(i) >= '0' && str.charAt(i) <= '9') {
                any = true;
                if (digits < 18) {
                    mantissa = mantissa * 10L + (long) (str.charAt(i) - '0');
                    digits = digits + 1;
                    exponent = exponent - 1;
                }
                ++i;
            }
        }
        if (!any) {
            throw new NumberFormatException("For input string: \"" + str + "\"");
        }
        if (i < len && (str.charAt(i) == 'e' || str.charAt(i) == 'E')) {
            ++i;
            boolean expNegative = false;
            if (i < len && (str.charAt(i) == '+' || str.charAt(i) == '-')) {
                expNegative = str.charAt(i) == '-';
                ++i;
            }
            int expValue = 0;
            boolean anyExp = false;
            while (i < len && str.charAt(i) >= '0' && str.charAt(i) <= '9') {
                anyExp = true;
                if (expValue < 100000) {
                    expValue = expValue * 10 + (str.charAt(i) - '0');
                }
                ++i;
            }
            if (!anyExp) {
                throw new NumberFormatException("For input string: \"" + str + "\"");
            }
            exponent = exponent + (expNegative ? -expValue : expValue);
        }
        if (i != len) {
            throw new NumberFormatException("For input string: \"" + str + "\"");
        }
        double value = (double) mantissa;
        while (exponent > 0) {
            value = value * 10.0;
            exponent = exponent - 1;
        }
        while (exponent < 0) {
            value = value / 10.0;
            exponent = exponent + 1;
        }
        return negative ? -value : value;
    }

    // Replaces String.indexOf(int), which does not exist here.
    private static int indexOfChar(String s, char c) {
        for (int i = 0; i < s.length(); ++i) {
            if (s.charAt(i) == c) {
                return i;
            }
        }
        return -1;
    }

    // Replaces String.regionMatches(int, String, int, int): does `text` contain `what` at `at`?
    private static boolean regionMatches(String text, int at, String what) {
        if (at < 0 || at + what.length() > text.length()) {
            return false;
        }
        for (int i = 0; i < what.length(); ++i) {
            if (text.charAt(at + i) != what.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    // Replaces Math.abs(Math.IEEEremainder(d, 1.0)): how far is d from the nearest whole number?
    // Only the ORDER of two such distances is ever used, so exactness beyond the double grid does
    // not matter; what matters is that a whole number scores 0 and its neighbour scores ~1 ulp.
    private static double distanceToInteger(double d) {
        if (d != d) {
            return d;
        }
        if (d == ChoiceFormat.inf() || d == -ChoiceFormat.inf()) {
            return 0.0;
        }
        // A double this large has no fractional part left to measure.
        if (d >= 9.007199254740992E15 || d <= -9.007199254740992E15) {
            return 0.0;
        }
        long whole = (long) d;
        double frac = d - (double) whole;
        if (frac < 0.0) {
            frac = -frac;
        }
        return frac > 0.5 ? 1.0 - frac : frac;
    }

    // Replaces Double.NaN.
    private static double nan() {
        return 0.0 / 0.0;
    }

    // Replaces Double.POSITIVE_INFINITY. Reached by overflow rather than written as a literal,
    // because a source literal for infinity does not exist in Java.
    private static double inf() {
        return 1.0e308 * 10.0;
    }

    // Replaces Double.MAX_VALUE.
    private static double maxValue() {
        return 1.7976931348623157E308;
    }

    // Replaces Double.MIN_VALUE (= longBitsToDouble(1L)): halve until the next halving underflows.
    private static double smallestSubnormal() {
        double x = 1.0;
        while (x * 0.5 > 0.0) {
            x = x * 0.5;
        }
        return x;
    }

    private static double[] growDoubles(double[] a) {
        double[] bigger = new double[a.length * 2];
        for (int i = 0; i < a.length; ++i) {
            bigger[i] = a[i];
        }
        return bigger;
    }

    private static String[] growStrings(String[] a) {
        String[] bigger = new String[a.length * 2];
        for (int i = 0; i < a.length; ++i) {
            bigger[i] = a[i];
        }
        return bigger;
    }
}
