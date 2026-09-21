package java.text;

import java.io.InvalidObjectException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The formatter of messages with holes: {@code "There are {0} files in {1}"}.
 *
 * <p>It exists for a reason of translation, not of convenience. Concatenating
 * {@code "There are " + n + " files in " + d} leaves the pieces' ORDER fixed in the code, and there
 * are languages that want it different; with a pattern, the translator moves {@code {0}} and
 * {@code {1}} without touching anything. That is why the index is explicit and not implicit in the
 * position.
 *
 * <p><b>The internal representation is the JDK's and it is worth understanding</b>: the pattern is
 * not stored as it was written. All the literal text is stored concatenated into one string, plus a
 * list of "at offset {@code o} goes argument {@code a}, formatted with {@code f}". Formatting is
 * interleaving; parsing is recognising the literal pieces and letting each subformat read what is in
 * between. {@link #toPattern()} synthesises the pattern back from there, so after a
 * {@link #setFormat} it returns the new pattern and not the original.
 *
 * <p>Quoting is the part that surprises most: a single quote opens literal text
 * ({@code 'no {0} substitution'}) and two quotes in a row are one quote. It is what lets a message
 * speak of braces without being interpreted.
 *
 * @implNote A declared subset, and it is two different things. (1) Of the pattern SYNTAX,
 *           {@code number}, {@code date}, {@code time} and {@code choice} with their styles are
 *           here; the types {@code dtf_date}/{@code dtf_time}/{@code dtf_datetime} (which delegate
 *           to {@code java.time.format}) and {@code compact_short}/{@code compact_long} are not, and
 *           a pattern using them is REJECTED with {@code IllegalArgumentException} -- they are not
 *           silently ignored. (2) Of {@link #formatToCharacterIterator}, the iterator marks the
 *           arguments with {@link java.text.MessageFormat.Field#ARGUMENT} but does not re-export the
 *           attributes each subformat puts inside its own piece. Marking too little is a subset;
 *           marking wrongly would not be.
 */
public class MessageFormat extends Format {

    /**
     * The key each piece of the result that came out of an argument is marked with.
     *
     * <p>It has a single constant because a message has a single interesting question: which part of
     * the text is fixed text and which part was substituted.
     */
    public static class Field extends java.text.Format.Field {

        private static final Map<String, java.text.MessageFormat.Field> INSTANCES =
                new HashMap<String, java.text.MessageFormat.Field>();

        protected Field(String name) {
            super(name);
            if (this.getClass() == java.text.MessageFormat.Field.class) {
                INSTANCES.put(name, this);
            }
        }

        protected Object readResolve() throws InvalidObjectException {
            if (this.getClass() != java.text.MessageFormat.Field.class) {
                throw new InvalidObjectException("subclass didn't correctly implement readResolve");
            }
            java.text.MessageFormat.Field f = INSTANCES.get(this.getName());
            if (f != null) {
                return f;
            }
            throw new InvalidObjectException("unknown attribute name");
        }

        public static final java.text.MessageFormat.Field ARGUMENT =
                new java.text.MessageFormat.Field("message argument field");
    }

    private Locale locale;
    // All the literal text, without the format elements. The holes live in `offsets`.
    private String pattern;
    private int[] offsets;
    private int[] argumentNumbers;
    private Format[] formats;
    private int count;
    private int maxArgument;

    public MessageFormat(String pattern) {
        this.locale = Locale.getDefault();
        this.resetTo();
        this.applyPattern(pattern);
    }

    public MessageFormat(String pattern, Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        this.locale = locale;
        this.resetTo();
        this.applyPattern(pattern);
    }

    private void resetTo() {
        this.pattern = "";
        this.offsets = new int[8];
        this.argumentNumbers = new int[8];
        this.formats = new Format[8];
        this.count = 0;
        this.maxArgument = -1;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return this.locale;
    }

    // ---- pattern ----

    public void applyPattern(String pattern) {
        if (pattern == null) {
            throw new NullPointerException();
        }
        StringBuilder raw = new StringBuilder();
        StringBuilder index = new StringBuilder();
        StringBuilder type = new StringBuilder();
        StringBuilder style = new StringBuilder();
        this.resetTo();

        // part 0 = literal text, 1 = index, 2 = type, 3 = style. The automaton is the JDK's: the
        // commas separate parts only up to 3, because the style can contain commas of its own (a
        // choice subpattern uses them).
        int part = 0;
        boolean betweenQuotes = false;
        int braces = 0;
        int i = 0;
        int n = pattern.length();
        while (i < n) {
            char ch = pattern.charAt(i);
            if (part == 0) {
                if (ch == '\'') {
                    if (i + 1 < n && pattern.charAt(i + 1) == '\'') {
                        raw.append('\'');
                        i = i + 1;
                    } else {
                        betweenQuotes = !betweenQuotes;
                    }
                } else if (ch == '{' && !betweenQuotes) {
                    part = 1;
                } else {
                    raw.append(ch);
                }
            } else if (betweenQuotes) {
                this.toPart(part, index, type, style).append(ch);
                if (ch == '\'') {
                    betweenQuotes = false;
                }
            } else if (ch == ',' && part < 3) {
                part = part + 1;
            } else if (ch == '{') {
                braces = braces + 1;
                this.toPart(part, index, type, style).append(ch);
            } else if (ch == '}') {
                if (braces == 0) {
                    this.addElement(raw.length(), index.toString(), type.toString(),
                            style.toString());
                    index.setLength(0);
                    type.setLength(0);
                    style.setLength(0);
                    part = 0;
                } else {
                    braces = braces - 1;
                    this.toPart(part, index, type, style).append(ch);
                }
            } else {
                if (ch == '\'') {
                    betweenQuotes = true;
                }
                this.toPart(part, index, type, style).append(ch);
            }
            i = i + 1;
        }
        if (part != 0 || braces != 0) {
            throw new IllegalArgumentException("Unmatched braces in the pattern.");
        }
        this.pattern = raw.toString();
    }

    private StringBuilder toPart(int part, StringBuilder index, StringBuilder type,
                                 StringBuilder style) {
        if (part == 1) {
            return index;
        }
        if (part == 2) {
            return type;
        }
        return style;
    }

    private void addElement(int offset, String index, String type, String style) {
        // The index is digits ONLY: not empty, not with spaces around it, not with a sign. `{ 0 }` is
        // not `{0}` with decoration but a malformed pattern, and accepting it would make a mistyped
        // `{0}` work here and fail against every other implementation.
        int arg;
        if (!onlyDigits(index)) {
            throw new IllegalArgumentException("can't parse argument number: " + index);
        }
        try {
            arg = Integer.parseInt(index);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("can't parse argument number: " + index);
        }
        if (arg < 0) {
            throw new IllegalArgumentException("negative argument number: " + arg);
        }
        if (this.count == this.offsets.length) {
            int raised = this.count * 2;
            int[] o = new int[raised];
            int[] a = new int[raised];
            Format[] f = new Format[raised];
            for (int k = 0; k < this.count; k = k + 1) {
                o[k] = this.offsets[k];
                a[k] = this.argumentNumbers[k];
                f[k] = this.formats[k];
            }
            this.offsets = o;
            this.argumentNumbers = a;
            this.formats = f;
        }
        this.offsets[this.count] = offset;
        this.argumentNumbers[this.count] = arg;
        this.formats[this.count] = this.buildFormat(type.trim(), style.trim());
        this.count = this.count + 1;
        if (arg > this.maxArgument) {
            this.maxArgument = arg;
        }
    }

    private static boolean onlyDigits(String s) {
        if (s.length() == 0) {
            return false;
        }
        for (int i = 0; i < s.length(); i = i + 1) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private Format buildFormat(String type, String style) {
        if (type.length() == 0) {
            // With no type the argument carries no fixed formatter: it is decided when formatting,
            // according to the value's class. It is what makes {0} serve for a number and for a
            // text.
            return null;
        }
        if (type.equals("number")) {
            if (style.length() == 0) {
                return NumberFormat.getInstance(this.locale);
            }
            if (style.equals("currency")) {
                return NumberFormat.getCurrencyInstance(this.locale);
            }
            if (style.equals("percent")) {
                return NumberFormat.getPercentInstance(this.locale);
            }
            if (style.equals("integer")) {
                return NumberFormat.getIntegerInstance(this.locale);
            }
            return new DecimalFormat(style, new DecimalFormatSymbols(this.locale));
        }
        if (type.equals("date")) {
            return this.dateFormatter(style, true);
        }
        if (type.equals("time")) {
            return this.dateFormatter(style, false);
        }
        if (type.equals("choice")) {
            return new ChoiceFormat(style);
        }
        // An unknown type is an error in the pattern, not something to ignore: were it accepted
        // silently, {0,dtf_date} would come out as the Date's toString() and nobody would notice.
        throw new IllegalArgumentException("unknown format type: " + type);
    }

    private Format dateFormatter(String style, boolean date) {
        int st = -1;
        if (style.length() == 0 || style.equals("medium")) {
            st = DateFormat.DEFAULT;
        } else if (style.equals("short")) {
            st = DateFormat.SHORT;
        } else if (style.equals("long")) {
            st = DateFormat.LONG;
        } else if (style.equals("full")) {
            st = DateFormat.FULL;
        }
        if (st < 0) {
            return new SimpleDateFormat(style, this.locale);
        }
        if (date) {
            return DateFormat.getDateInstance(st, this.locale);
        }
        return DateFormat.getTimeInstance(st, this.locale);
    }

    /**
     * It synthesises the pattern describing the current state.
     *
     * <p>The subformats are recognised by comparing them against what the locale's factories return:
     * if one equals this locale's {@code getCurrencyInstance}, {@code ,number,currency} is written
     * and not the raw pattern. A formatter set by hand that resembles none of them is written with
     * its own pattern; one that cannot give a pattern comes out as a bare {@code {n}} -- which is
     * what the JDK does, and is preferable to inventing a syntax for it.
     */
    public String toPattern() {
        StringBuilder r = new StringBuilder();
        int last = 0;
        for (int i = 0; i < this.count; i = i + 1) {
            this.copyQuoting(this.pattern, last, this.offsets[i], r);
            last = this.offsets[i];
            r.append('{');
            r.append(Integer.toString(this.argumentNumbers[i]));
            this.describeFormat(this.formats[i], r);
            r.append('}');
        }
        this.copyQuoting(this.pattern, last, this.pattern.length(), r);
        return r.toString();
    }

    private void describeFormat(Format f, StringBuilder r) {
        if (f == null) {
            return;
        }
        if (f instanceof ChoiceFormat) {
            r.append(",choice,");
            r.append(((ChoiceFormat) f).toPattern());
            return;
        }
        if (f instanceof NumberFormat) {
            if (f.equals(NumberFormat.getInstance(this.locale))) {
                r.append(",number");
            } else if (f.equals(NumberFormat.getCurrencyInstance(this.locale))) {
                r.append(",number,currency");
            } else if (f.equals(NumberFormat.getPercentInstance(this.locale))) {
                r.append(",number,percent");
            } else if (f.equals(NumberFormat.getIntegerInstance(this.locale))) {
                r.append(",number,integer");
            } else if (f instanceof DecimalFormat) {
                r.append(",number,");
                r.append(((DecimalFormat) f).toPattern());
            }
            return;
        }
        if (f instanceof DateFormat) {
            for (int k = DateFormat.FULL; k <= DateFormat.SHORT; k = k + 1) {
                if (f.equals(DateFormat.getDateInstance(k, this.locale))) {
                    r.append(",date");
                    this.describeStyle(k, r);
                    return;
                }
            }
            for (int k = DateFormat.FULL; k <= DateFormat.SHORT; k = k + 1) {
                if (f.equals(DateFormat.getTimeInstance(k, this.locale))) {
                    r.append(",time");
                    this.describeStyle(k, r);
                    return;
                }
            }
            if (f instanceof SimpleDateFormat) {
                r.append(",date,");
                r.append(((SimpleDateFormat) f).toPattern());
            }
        }
    }

    private void describeStyle(int style, StringBuilder r) {
        if (style == DateFormat.FULL) {
            r.append(",full");
        } else if (style == DateFormat.LONG) {
            r.append(",long");
        } else if (style == DateFormat.SHORT) {
            r.append(",short");
        }
        // MEDIUM is the default style: writing it would be noise, and the pattern with no style
        // gives it back again.
    }

    // The literal text goes back into the pattern with the quotes doubled and the OPENING braces
    // quoted. Only the opening ones: a loose '}' in literal text opens nothing, so it does not need
    // protecting, and protecting it anyway would give a pattern different from the one the JDK
    // returns for the same message. Without this, a text with a '{' would stop round-tripping: the
    // second applyPattern would read a format element where there was text.
    private void copyQuoting(String s, int from, int to, StringBuilder r) {
        boolean open = false;
        for (int i = from; i < to; i = i + 1) {
            char c = s.charAt(i);
            if (c == '{') {
                if (!open) {
                    r.append('\'');
                    open = true;
                }
                r.append(c);
            } else if (c == '\'') {
                r.append("''");
            } else {
                if (open) {
                    r.append('\'');
                    open = false;
                }
                r.append(c);
            }
        }
        if (open) {
            r.append('\'');
        }
    }

    // ---- subformats -----------------------------------------------------------------------------

    public void setFormatsByArgumentIndex(Format[] newFormats) {
        for (int i = 0; i < this.count; i = i + 1) {
            int arg = this.argumentNumbers[i];
            if (arg < newFormats.length) {
                this.formats[i] = newFormats[arg];
            }
        }
    }

    public void setFormats(Format[] newFormats) {
        int n = this.count;
        if (newFormats.length < n) {
            n = newFormats.length;
        }
        for (int i = 0; i < n; i = i + 1) {
            this.formats[i] = newFormats[i];
        }
    }

    public void setFormatByArgumentIndex(int argumentIndex, Format newFormat) {
        for (int i = 0; i < this.count; i = i + 1) {
            if (this.argumentNumbers[i] == argumentIndex) {
                this.formats[i] = newFormat;
            }
        }
    }

    public void setFormat(int formatElementIndex, Format newFormat) {
        if (formatElementIndex < 0 || formatElementIndex >= this.count) {
            throw new ArrayIndexOutOfBoundsException(formatElementIndex);
        }
        this.formats[formatElementIndex] = newFormat;
    }

    /**
     * The subformats indexed by argument number.
     *
     * <p>If an argument appears twice in the pattern the LAST wins, which is what the JDK documents:
     * the array has one slot per argument and repeated appearances do not fit.
     */
    public Format[] getFormatsByArgumentIndex() {
        Format[] out = new Format[this.maxArgument + 1];
        for (int i = 0; i < this.count; i = i + 1) {
            out[this.argumentNumbers[i]] = this.formats[i];
        }
        return out;
    }

    /** The subformats in the order they appear in the pattern, one per element. */
    public Format[] getFormats() {
        Format[] out = new Format[this.count];
        for (int i = 0; i < this.count; i = i + 1) {
            out[i] = this.formats[i];
        }
        return out;
    }

    // ---- formatting -----------------------------------------------------------------------------

    public final StringBuffer format(Object[] arguments, StringBuffer result, FieldPosition pos) {
        return this.write(arguments, result, pos, null);
    }

    public final StringBuffer format(Object arguments, StringBuffer result, FieldPosition pos) {
        return this.write((Object[]) arguments, result, pos, null);
    }

    /** The one-shot shortcut: it builds the formatter, formats, and throws it away. */
    public static String format(String pattern, Object... arguments) {
        MessageFormat temp = new MessageFormat(pattern);
        return temp.format(arguments, new StringBuffer(), new FieldPosition(0)).toString();
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object arguments) {
        if (arguments == null) {
            throw new NullPointerException();
        }
        FieldMarks marks = new FieldMarks();
        StringBuffer sb = new StringBuffer();
        this.write((Object[]) arguments, sb, null, marks);
        return marks.iterator(sb.toString());
    }

    private StringBuffer write(Object[] arguments, StringBuffer result, FieldPosition pos,
                                  FieldMarks marks) {
        FieldMarks m = marks;
        if (m == null) {
            m = new FieldMarks();
        }
        int last = 0;
        for (int i = 0; i < this.count; i = i + 1) {
            result.append(this.pattern.substring(last, this.offsets[i]));
            last = this.offsets[i];
            int arg = this.argumentNumbers[i];
            int d = result.length();
            if (arguments == null || arg >= arguments.length) {
                // An argument that did not arrive is written as {n}, unsubstituted. It is
                // information: it says exactly what was missing, instead of leaving an empty hole or
                // blowing up.
                result.append('{');
                result.append(Integer.toString(arg));
                result.append('}');
            } else {
                this.writeArgument(arguments[arg], this.formats[i], result);
            }
            // The attribute's value is the argument's NUMBER, not the key: in a message with
            // several holes, "an argument goes here" does not say which, and that is precisely the
            // datum.
            m.mark((AttributedCharacterIterator.Attribute) java.text.MessageFormat.Field.ARGUMENT,
                    Integer.valueOf(arg), -1, d, result.length());
        }
        result.append(this.pattern.substring(last, this.pattern.length()));
        m.apply(pos);
        return result;
    }

    private void writeArgument(Object value, Format formatter, StringBuffer result) {
        if (value == null) {
            result.append("null");
            return;
        }
        Format f = formatter;
        if (f == null) {
            // With no declared formatter the value's type decides. A Number goes through the
            // locale's number formatter and a Date through its date and time one, because their
            // toString() respects no locale at all.
            if (value instanceof Number) {
                f = NumberFormat.getInstance(this.locale);
            } else if (value instanceof Date) {
                f = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, this.locale);
            } else {
                result.append(value.toString());
                return;
            }
        }
        String text = f.format(value);
        if (f instanceof ChoiceFormat && text.indexOf('{') >= 0) {
            // A ChoiceFormat can return a message pattern ("{0} files"): it is formatted again with
            // the same arguments. It is what makes writing plurals possible.
            result.append(new MessageFormat(text, this.locale).format(new Object[] {value}));
            return;
        }
        result.append(text);
    }

    // ---- parsing --------------------------------------------------------------------------------

    /**
     * It reads the arguments out of a text following this pattern.
     *
     * <p>The algorithm is the JDK's and its limit is worth stating: the literal pieces are searched
     * left to right and no alternatives are tried. A pattern whose literals are ambiguous
     * ({@code "{0}{1}"}) cannot be parsed, and the result is a failure, not a guess.
     *
     * @return an array with one slot per argument; the ones the pattern does not name stay null
     */
    public Object[] parse(String source, ParsePosition pos) {
        if (source == null) {
            return null;
        }
        Object[] result = new Object[this.maxArgument + 1];
        int patternOffset = 0;
        int sourceOffset = pos.getIndex();
        ParsePosition temp = new ParsePosition(0);
        for (int i = 0; i < this.count; i = i + 1) {
            int length = this.offsets[i] - patternOffset;
            if (length == 0 || this.pattern.regionMatches(patternOffset, source, sourceOffset, length)) {
                sourceOffset = sourceOffset + length;
                patternOffset = patternOffset + length;
            } else {
                pos.setErrorIndex(sourceOffset);
                return null;
            }
            if (this.formats[i] == null) {
                // An argument with no formatter: everything up to the next literal is taken. If it
                // is the last, up to the end -- hence the longest possible being the rule.
                int to = this.pattern.length();
                if (i + 1 < this.count) {
                    to = this.offsets[i + 1];
                }
                int nextLevel;
                if (patternOffset >= to) {
                    nextLevel = source.length();
                } else {
                    nextLevel = source.indexOf(this.pattern.substring(patternOffset, to),
                            sourceOffset);
                }
                if (nextLevel < 0) {
                    pos.setErrorIndex(sourceOffset);
                    return null;
                }
                String value = source.substring(sourceOffset, nextLevel);
                // A literal "{n}" in the input is the "this argument did not arrive" mark the
                // formatting puts there: it is read as absent and not as the string "{n}".
                if (!value.equals("{" + Integer.toString(this.argumentNumbers[i]) + "}")) {
                    result[this.argumentNumbers[i]] = value;
                }
                sourceOffset = nextLevel;
            } else {
                temp.setIndex(sourceOffset);
                result[this.argumentNumbers[i]] = this.formats[i].parseObject(source, temp);
                if (temp.getIndex() == sourceOffset) {
                    pos.setErrorIndex(sourceOffset);
                    return null;
                }
                sourceOffset = temp.getIndex();
            }
        }
        int length = this.pattern.length() - patternOffset;
        if (length == 0 || this.pattern.regionMatches(patternOffset, source, sourceOffset, length)) {
            pos.setIndex(sourceOffset + length);
        } else {
            pos.setErrorIndex(sourceOffset);
            return null;
        }
        return result;
    }

    public Object[] parse(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Object[] result = this.parse(source, pos);
        if (pos.getIndex() == 0) {
            throw new ParseException("MessageFormat parse error!", pos.getErrorIndex());
        }
        return result;
    }

    public Object parseObject(String source, ParsePosition pos) {
        return this.parse(source, pos);
    }

    // ---- identity -------------------------------------------------------------------------------

    public Object clone() {
        MessageFormat copy = new MessageFormat(this.toPattern(), this.locale);
        for (int i = 0; i < this.count && i < copy.count; i = i + 1) {
            copy.formats[i] = this.formats[i];
        }
        return copy;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        MessageFormat other = (MessageFormat) obj;
        if (this.count != other.count || !this.pattern.equals(other.pattern)
                || !this.locale.equals(other.locale)) {
            return false;
        }
        for (int i = 0; i < this.count; i = i + 1) {
            if (this.offsets[i] != other.offsets[i]
                    || this.argumentNumbers[i] != other.argumentNumbers[i]) {
                return false;
            }
            if (this.formats[i] == null) {
                if (other.formats[i] != null) {
                    return false;
                }
            } else if (!this.formats[i].equals(other.formats[i])) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        return this.pattern.hashCode();
    }
}
