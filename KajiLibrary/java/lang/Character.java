package java.lang;

// Por import y nombre simple: calificar el tipo en el uso no resuelve desde java.lang
// (finding #210).
import java.lang.constant.ConstantDescs;
import java.lang.constant.DynamicConstantDesc;
import java.util.Optional;

import java.io.Serializable;


/**
 * The boxed {@code char}, and the place Unicode lives.
 *
 * <p>Two jobs in one class, and the second is the larger. As a box it is the counterpart of
 * {@link Integer} for a 16-bit code unit. As a Unicode facade it answers what a character IS —
 * a letter, a digit, whitespace — and what it maps to when case is changed, which no amount of
 * arithmetic can decide: the answers are a property of the Unicode database, not of the number.
 *
 * <p>Which is why this class carries TABLES. They are generated from the reference JDK rather
 * than transcribed, in two shapes: a mapping is a run of {@code (start, end, delta)} because
 * almost all case mapping is "this range maps to that one at a fixed distance", and a predicate
 * is a list of {@code (start, end)} spans. Both are searched by bisection, so a lookup costs a
 * handful of comparisons rather than a walk.
 *
 * <p>A {@code char} is 16 bits and Unicode is not, so everything above U+FFFF is a SURROGATE
 * PAIR of two chars. That is why half the methods here come in two forms: one taking a
 * {@code char}, which cannot see past U+FFFF, and one taking an {@code int} code point, which
 * can. When they disagree, the {@code int} form is the one telling the truth.
 *
 * @implNote A KajiLibrary subset. Absent are the classification families that need their own
 *           tables — {@code UnicodeBlock}, {@code UnicodeScript} and the Java-identifier predicates —
 *           and the {@code Constable} interface. The name lookups {@code getName} and
 *           {@code codePointOf} are present in shape but throw {@code UnsupportedOperationException}
 *           for the same reason: they need the Unicode name table ({@code uniName.dat}), not carried
 *           here. What works fully is the part the rest of the library needs: case, the basic
 *           classes, code points, and radix conversion.
 */
public final class Character implements Comparable<Character>, Serializable {

    /** The smallest {@code char}, {@code '\u0000'}. */
    public static final char MIN_VALUE = (char) 0;

    /** The largest {@code char}, {@code '\uffff'}. */
    public static final char MAX_VALUE = (char) 0xffff;

    /** The smallest radix {@link #digit} and {@link #forDigit} accept. */
    public static final int MIN_RADIX = 2;

    /** The largest radix, which is what a digit plus the whole Latin alphabet reaches. */
    public static final int MAX_RADIX = 36;

    /** How many bits a {@code char} occupies. */
    public static final int SIZE = 16;

    /** How many bytes a {@code char} occupies. */
    public static final int BYTES = 2;

    /** The first half of a surrogate pair starts here. */
    public static final char MIN_HIGH_SURROGATE = (char) 0xd800;

    public static final char MAX_HIGH_SURROGATE = (char) 0xdbff;

    /** The second half of a surrogate pair starts here. */
    public static final char MIN_LOW_SURROGATE = (char) 0xdc00;

    public static final char MAX_LOW_SURROGATE = (char) 0xdfff;

    /** The whole surrogate range, both halves. */
    public static final char MIN_SURROGATE = (char) 0xd800;

    public static final char MAX_SURROGATE = (char) 0xdfff;

    /** The lowest code point, zero. */
    public static final int MIN_CODE_POINT = 0;

    /** The highest code point Unicode defines, U+10FFFF. */
    public static final int MAX_CODE_POINT = 0x10ffff;

    /** The first code point that does not fit in a {@code char}. */
    public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x10000;

    private final char value;

    /**
     * Boxes {@code value}.
     *
     * @deprecated use {@link #valueOf}, which is free to hand back a shared box
     */
    @Deprecated(since = "9")
    public Character(char value) {
        this.value = value;
    }

    // Las instancias compartidas de 0..127. Viven en una clase anidada para que se construyan
    // en el primer `valueOf` y no en la primera mencion de la clase.
    //
    // No es una optimizacion: **JLS 5.1.7 exige que boxear un valor de ese rango devuelva la
    // MISMA referencia**, asi que `Character.valueOf('k') == Character.valueOf('k')` es una promesa del lenguaje. Sin la cache
    // la promesa se rompe en silencio -- el codigo sigue andando hasta que alguien compara con
    // `==`, que es justo lo que la cache existe para permitir.
    private static final class CharacterCache {

        static final Character[] CACHE = CharacterCache.fill();

        private static Character[] fill() {
            Character[] out = new Character[128];
            int i = 0;
            while (i < 128) {
                out[i] = new Character((char) (i + (0)));
                i = i + 1;
            }
            return out;
        }
    }

    /** Boxes {@code c}. */
    public static Character valueOf(char c) {
        if (c <= 127) {
            return CharacterCache.CACHE[c];
        }
        return new Character(c);
    }

    /** The boxed value. */
    public char charValue() {
        return this.value;
    }

    /** Two boxes are equal when they hold the same char. */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Character)) {
            return false;
        }
        Character other = (Character) obj;
        return this.value == other.charValue();
    }

    /** The char itself, which is already a small distinct number. */
    @Override
    public int hashCode() {
        return this.value;
    }

    /** The one-character string this box holds. */
    @Override
    public String toString() {
        return Character.toString(this.value);
    }

    /** The one-character string holding {@code c}. */
    public static String toString(char c) {
        char[] one = new char[1];
        one[0] = c;
        return String.valueOf(one, 0, 1);
    }

    /** The string holding the code point {@code codePoint}, one char or two. */
    public static String toString(int codePoint) {
        char[] out = Character.toChars(codePoint);
        return String.valueOf(out, 0, out.length);
    }

    /** Order by code-unit value. */
    @Override
    public int compareTo(Character other) {
        return Character.compare(this.value, other.charValue());
    }

    /** Order by code-unit value, without boxing. */
    public static int compare(char x, char y) {
        return x - y;
    }

    /** The char reinterpreted as an unsigned 16-bit number, which it already is. */
    public static int hashCode(char c) {
        return c;
    }

    // ---- the two numbering schemes Unicode assigns to every character ----
    //
    // A code point carries a GENERAL CATEGORY (is it a letter? a digit? punctuation?) and a
    // BIDIRECTIONAL CLASS (does it read left to right?). They are separate properties of the
    // standard and these are their two sets of numbers -- the values `getType` and
    // `getDirectionality` answer with.
    //
    // They are `byte` and not `int`, which looks like an accident of history and is: the
    // categories are small enough to fit and `getType` still returns an `int`, so a caller
    // comparing the two has a widening in the middle. Matching the reference means matching that
    // too.

    // The general categories, in the standard's own order -- which is why UNASSIGNED is 0 and
    // the letters come first.
    public static final byte UNASSIGNED = 0;
    public static final byte UPPERCASE_LETTER = 1;
    public static final byte LOWERCASE_LETTER = 2;
    public static final byte TITLECASE_LETTER = 3;
    public static final byte MODIFIER_LETTER = 4;
    public static final byte OTHER_LETTER = 5;
    public static final byte NON_SPACING_MARK = 6;
    public static final byte ENCLOSING_MARK = 7;
    public static final byte COMBINING_SPACING_MARK = 8;
    public static final byte DECIMAL_DIGIT_NUMBER = 9;
    public static final byte LETTER_NUMBER = 10;
    public static final byte OTHER_NUMBER = 11;
    public static final byte SPACE_SEPARATOR = 12;
    public static final byte LINE_SEPARATOR = 13;
    public static final byte PARAGRAPH_SEPARATOR = 14;
    public static final byte CONTROL = 15;
    public static final byte FORMAT = 16;
    public static final byte PRIVATE_USE = 18;
    public static final byte SURROGATE = 19;
    public static final byte DASH_PUNCTUATION = 20;
    public static final byte START_PUNCTUATION = 21;
    public static final byte END_PUNCTUATION = 22;
    public static final byte CONNECTOR_PUNCTUATION = 23;
    public static final byte OTHER_PUNCTUATION = 24;
    public static final byte MATH_SYMBOL = 25;
    public static final byte CURRENCY_SYMBOL = 26;
    public static final byte MODIFIER_SYMBOL = 27;
    public static final byte OTHER_SYMBOL = 28;
    public static final byte INITIAL_QUOTE_PUNCTUATION = 29;
    public static final byte FINAL_QUOTE_PUNCTUATION = 30;

    // The bidirectional classes. UNDEFINED is -1 rather than 0 here, because 0 is already
    // LEFT_TO_RIGHT and there has to be a value that means "no answer".
    public static final byte DIRECTIONALITY_UNDEFINED = -1;
    public static final byte DIRECTIONALITY_LEFT_TO_RIGHT = 0;
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT = 1;
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC = 2;
    public static final byte DIRECTIONALITY_EUROPEAN_NUMBER = 3;
    public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR = 4;
    public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR = 5;
    public static final byte DIRECTIONALITY_ARABIC_NUMBER = 6;
    public static final byte DIRECTIONALITY_COMMON_NUMBER_SEPARATOR = 7;
    public static final byte DIRECTIONALITY_NONSPACING_MARK = 8;
    public static final byte DIRECTIONALITY_BOUNDARY_NEUTRAL = 9;
    public static final byte DIRECTIONALITY_PARAGRAPH_SEPARATOR = 10;
    public static final byte DIRECTIONALITY_SEGMENT_SEPARATOR = 11;
    public static final byte DIRECTIONALITY_WHITESPACE = 12;
    public static final byte DIRECTIONALITY_OTHER_NEUTRALS = 13;
    public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING = 14;
    public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE = 15;
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING = 16;
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE = 17;
    public static final byte DIRECTIONALITY_POP_DIRECTIONAL_FORMAT = 18;
    public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_ISOLATE = 19;
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_ISOLATE = 20;
    public static final byte DIRECTIONALITY_FIRST_STRONG_ISOLATE = 21;
    public static final byte DIRECTIONALITY_POP_DIRECTIONAL_ISOLATE = 22;

    /**
     * The mirror of the primitive type {@code char}.
     *
     * <p>Not {@code Character.class}: that one names this class.
     */
    public static final Class<Character> TYPE = Class.getPrimitiveClass("char");

    // ---- what a character IS ----

    /** Whether {@code c} is a letter in any script. */
    public static boolean isLetter(char c) {
        return Character.isLetter((int) c);
    }

    /** Whether the code point {@code cp} is a letter in any script. */
    public static boolean isLetter(int cp) {
        return Character.inSpans(Character.LETTER, cp);
    }

    /** Whether {@code c} is a decimal digit in any script. */
    public static boolean isDigit(char c) {
        return Character.isDigit((int) c);
    }

    public static boolean isDigit(int cp) {
        return Character.inSpans(Character.DIGIT, cp);
    }

    public static boolean isLetterOrDigit(char c) {
        return Character.isLetterOrDigit((int) c);
    }

    public static boolean isLetterOrDigit(int cp) {
        return Character.isLetter(cp) || Character.isDigit(cp);
    }

    /**
     * Whether {@code c} is whitespace by the JAVA definition.
     *
     * <p>Not the same question as {@link #isSpaceChar}: this one counts the ASCII controls that
     * separate lines and columns — tab, newline, form feed — and excludes the non-breaking
     * spaces, whose whole purpose is NOT to be a line break. That is why both exist.
     */
    public static boolean isWhitespace(char c) {
        return Character.isWhitespace((int) c);
    }

    public static boolean isWhitespace(int cp) {
        return Character.inSpans(Character.WHITESPACE, cp);
    }

    /** Whether {@code c} is a space by the UNICODE definition — see {@link #isWhitespace}. */
    public static boolean isSpaceChar(char c) {
        return Character.isSpaceChar((int) c);
    }

    public static boolean isSpaceChar(int cp) {
        return Character.inSpans(Character.SPACECHAR, cp);
    }

    public static boolean isUpperCase(char c) {
        return Character.isUpperCase((int) c);
    }

    public static boolean isUpperCase(int cp) {
        return Character.inSpans(Character.UPPERCASE, cp);
    }

    public static boolean isLowerCase(char c) {
        return Character.isLowerCase((int) c);
    }

    public static boolean isLowerCase(int cp) {
        return Character.inSpans(Character.LOWERCASE, cp);
    }

    // ---- case ----

    /**
     * {@code c} in lower case, or {@code c} when it has no lower case.
     *
     * <p>The {@code char} form cannot express every mapping: a few characters lower-case to
     * something outside the BMP, or to more than one character, and those come back unchanged.
     * The JDK has the same limit and the same remedy — use the code-point form, or
     * {@link String#toLowerCase}, which can grow.
     */
    public static char toLowerCase(char c) {
        int mapped = Character.toLowerCase((int) c);
        if (mapped > 0xffff) {
            return c;
        }
        return (char) mapped;
    }

    /** The code point {@code cp} in lower case, or {@code cp} when it has no lower case. */
    public static int toLowerCase(int cp) {
        return Character.mapped(Character.LOWER, cp);
    }

    /** {@code c} in upper case. See {@link #toLowerCase(char)} on what a char cannot express. */
    public static char toUpperCase(char c) {
        int mapped = Character.toUpperCase((int) c);
        if (mapped > 0xffff) {
            return c;
        }
        return (char) mapped;
    }

    public static int toUpperCase(int cp) {
        return Character.mapped(Character.UPPER, cp);
    }

    /**
     * {@code c} in title case.
     *
     * <p>Almost always the same as upper case. It differs for the handful of digraphs written
     * as one character, where upper-casing gives two capitals and title-casing gives a capital
     * followed by a small letter.
     */
    public static char toTitleCase(char c) {
        int mapped = Character.toTitleCase((int) c);
        if (mapped > 0xffff) {
            return c;
        }
        return (char) mapped;
    }

    public static int toTitleCase(int cp) {
        return Character.mapped(Character.TITLE, cp);
    }

    // ---- radix ----

    /**
     * The value of {@code c} as a digit in {@code radix}, or {@code -1} if it is not one.
     *
     * <p>Letters count from 10, so {@code 'f'} in radix 16 is 15 — and {@code 'f'} in radix 10
     * is {@code -1}, because the radix decides which digits exist, not just what they are worth.
     */
    public static int digit(char c, int radix) {
        return Character.digit((int) c, radix);
    }

    public static int digit(int cp, int radix) {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            return -1;
        }
        int value = Character.decimalValue(cp);
        if (value < 0) {
            value = Character.letterValue(cp);
        }
        if (value < 0 || value >= radix) {
            return -1;
        }
        return value;
    }

    /**
     * The character for {@code digit} in {@code radix}, or {@code '\u0000'} if there is none.
     *
     * <p>Lower case, always: the pair with {@link #digit} is not symmetric, since that one
     * accepts either case.
     */
    public static char forDigit(int digit, int radix) {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            return (char) 0;
        }
        if (digit < 0 || digit >= radix) {
            return (char) 0;
        }
        if (digit < 10) {
            return (char) ('0' + digit);
        }
        return (char) ('a' + digit - 10);
    }

    /**
     * The number {@code c} denotes, or {@code -1} if it denotes none.
     *
     * <p>Wider than {@link #digit}: it takes any letter as its alphabet position, so {@code 'z'}
     * is 35 without a radix being named.
     */
    public static int getNumericValue(char c) {
        return Character.getNumericValue((int) c);
    }

    public static int getNumericValue(int cp) {
        int digit = Character.decimalValue(cp);
        if (digit >= 0) {
            return digit;
        }
        return Character.letterValue(cp);
    }

    // ---- surrogates and code points ----

    /** Whether {@code c} is the FIRST half of a surrogate pair. */
    public static boolean isHighSurrogate(char c) {
        return c >= Character.MIN_HIGH_SURROGATE && c <= Character.MAX_HIGH_SURROGATE;
    }

    /** Whether {@code c} is the SECOND half of a surrogate pair. */
    public static boolean isLowSurrogate(char c) {
        return c >= Character.MIN_LOW_SURROGATE && c <= Character.MAX_LOW_SURROGATE;
    }

    /** Whether {@code c} is either half of a surrogate pair. */
    public static boolean isSurrogate(char c) {
        return c >= Character.MIN_SURROGATE && c <= Character.MAX_SURROGATE;
    }

    /** Whether {@code high} and {@code low} form a valid pair, in that order. */
    public static boolean isSurrogatePair(char high, char low) {
        return Character.isHighSurrogate(high) && Character.isLowSurrogate(low);
    }

    /** How many chars {@code cp} needs: two above U+FFFF, one below. */
    public static int charCount(int cp) {
        return cp >= Character.MIN_SUPPLEMENTARY_CODE_POINT ? 2 : 1;
    }

    /** The code point a surrogate pair denotes. */
    public static int toCodePoint(char high, char low) {
        return Character.MIN_SUPPLEMENTARY_CODE_POINT
                + ((high - Character.MIN_HIGH_SURROGATE) << 10)
                + (low - Character.MIN_LOW_SURROGATE);
    }

    /** The first half of the pair encoding {@code cp}. */
    public static char highSurrogate(int cp) {
        return (char) (Character.MIN_HIGH_SURROGATE
                + ((cp - Character.MIN_SUPPLEMENTARY_CODE_POINT) >> 10));
    }

    /** The second half of the pair encoding {@code cp}. */
    public static char lowSurrogate(int cp) {
        return (char) (Character.MIN_LOW_SURROGATE
                + ((cp - Character.MIN_SUPPLEMENTARY_CODE_POINT) & 0x3ff));
    }

    /** Whether {@code cp} fits in a single char. */
    public static boolean isBmpCodePoint(int cp) {
        return cp >= 0 && cp < Character.MIN_SUPPLEMENTARY_CODE_POINT;
    }

    /** Whether {@code cp} needs a surrogate pair. */
    public static boolean isSupplementaryCodePoint(int cp) {
        return cp >= Character.MIN_SUPPLEMENTARY_CODE_POINT && cp <= Character.MAX_CODE_POINT;
    }

    /** Whether {@code cp} is a code point at all. */
    public static boolean isValidCodePoint(int cp) {
        return cp >= Character.MIN_CODE_POINT && cp <= Character.MAX_CODE_POINT;
    }

    /**
     * The chars encoding {@code cp}.
     *
     * @throws IllegalArgumentException if {@code cp} is not a code point
     */
    public static char[] toChars(int cp) {
        if (!Character.isValidCodePoint(cp)) {
            throw new IllegalArgumentException("not a code point: " + cp);
        }
        if (Character.isBmpCodePoint(cp)) {
            char[] one = new char[1];
            one[0] = (char) cp;
            return one;
        }
        char[] two = new char[2];
        two[0] = Character.highSurrogate(cp);
        two[1] = Character.lowSurrogate(cp);
        return two;
    }

    /**
     * Writes the chars encoding {@code cp} into {@code dst} at {@code dstIndex}.
     *
     * @return how many chars were written, one or two
     */
    public static int toChars(int cp, char[] dst, int dstIndex) {
        char[] made = Character.toChars(cp);
        for (int i = 0; i < made.length; i++) {
            dst[dstIndex + i] = made[i];
        }
        return made.length;
    }

    /** The code point starting at {@code index} of {@code seq}. */
    public static int codePointAt(CharSequence seq, int index) {
        char c = seq.charAt(index);
        if (Character.isHighSurrogate(c) && index + 1 < seq.length()) {
            char next = seq.charAt(index + 1);
            if (Character.isLowSurrogate(next)) {
                return Character.toCodePoint(c, next);
            }
        }
        return c;
    }

    /** The code point ending just before {@code index} of {@code seq}. */
    public static int codePointBefore(CharSequence seq, int index) {
        char c = seq.charAt(index - 1);
        if (Character.isLowSurrogate(c) && index - 2 >= 0) {
            char prev = seq.charAt(index - 2);
            if (Character.isHighSurrogate(prev)) {
                return Character.toCodePoint(prev, c);
            }
        }
        return c;
    }

    // ---- code points over an array, and counting them ----
    //
    // The array forms exist for the same reason String.getChars does: a buffer is a char[]
    // before it is anything else, and reading one through a CharSequence would mean building
    // the CharSequence first. They carry an explicit limit rather than trusting a.length,
    // because the caller is usually looking at a PREFIX of a longer array -- a builder's
    // backing store outruns its contents, and a surrogate read past the count would be a
    // character the caller does not have.

    /**
     * The code point starting at {@code index} of {@code a}, reading no further than the end.
     *
     * @param a where to read
     * @param index the position
     */
    public static int codePointAt(char[] a, int index) {
        return Character.codePointAt(a, index, a.length);
    }

    /**
     * The code point starting at {@code index} of {@code a}, reading no further than
     * {@code limit}.
     *
     * @param a where to read
     * @param index the position
     * @param limit where the readable part ends
     */
    public static int codePointAt(char[] a, int index, int limit) {
        if (index >= limit || index < 0 || limit > a.length) {
            throw new IndexOutOfBoundsException("index " + index + ", limit " + limit);
        }
        char c = a[index];
        if (Character.isHighSurrogate(c) && index + 1 < limit) {
            char next = a[index + 1];
            if (Character.isLowSurrogate(next)) {
                return Character.toCodePoint(c, next);
            }
        }
        return c;
    }

    /**
     * The code point ending just before {@code index} of {@code a}.
     *
     * @param a where to read
     * @param index the position to look back from
     */
    public static int codePointBefore(char[] a, int index) {
        return Character.codePointBefore(a, index, 0);
    }

    /**
     * The code point ending just before {@code index} of {@code a}, reading no earlier than
     * {@code start}.
     *
     * @param a where to read
     * @param index the position to look back from
     * @param start where the readable part begins
     */
    public static int codePointBefore(char[] a, int index, int start) {
        if (index <= start || index > a.length || start < 0 || start >= a.length) {
            throw new IndexOutOfBoundsException("index " + index + ", start " + start);
        }
        char c = a[index - 1];
        if (Character.isLowSurrogate(c) && index - 2 >= start) {
            char prev = a[index - 2];
            if (Character.isHighSurrogate(prev)) {
                return Character.toCodePoint(prev, c);
            }
        }
        return c;
    }

    /**
     * How many code points {@code [beginIndex, endIndex)} of {@code seq} holds.
     *
     * <p>Not the number of characters, and the gap between the two is the whole reason this
     * method exists: a supplementary code point is written as two of them.
     *
     * @param seq where to count
     * @param beginIndex where to start
     * @param endIndex where to stop, exclusive
     */
    public static int codePointCount(CharSequence seq, int beginIndex, int endIndex) {
        if (beginIndex < 0 || endIndex > seq.length() || beginIndex > endIndex) {
            throw new IndexOutOfBoundsException("begin " + beginIndex + ", end " + endIndex);
        }
        int found = 0;
        int i = beginIndex;
        while (i < endIndex) {
            char c = seq.charAt(i);
            i = i + 1;
            if (Character.isHighSurrogate(c) && i < endIndex
                    && Character.isLowSurrogate(seq.charAt(i))) {
                i = i + 1;
            }
            found = found + 1;
        }
        return found;
    }

    /**
     * How many code points the {@code count} characters from {@code offset} hold.
     *
     * @param a where to count
     * @param offset where to start
     * @param count how many characters to look at
     */
    public static int codePointCount(char[] a, int offset, int count) {
        if (count > a.length - offset || offset < 0 || count < 0) {
            throw new IndexOutOfBoundsException("offset " + offset + ", count " + count);
        }
        int end = offset + count;
        int found = 0;
        int i = offset;
        while (i < end) {
            char c = a[i];
            i = i + 1;
            if (Character.isHighSurrogate(c) && i < end && Character.isLowSurrogate(a[i])) {
                i = i + 1;
            }
            found = found + 1;
        }
        return found;
    }

    /**
     * The index {@code codePointOffset} code points away from {@code index}.
     *
     * @param seq where to walk
     * @param index where to start
     * @param codePointOffset how many code points to move; negative walks backwards
     */
    public static int offsetByCodePoints(CharSequence seq, int index, int codePointOffset) {
        if (index < 0 || index > seq.length()) {
            throw new IndexOutOfBoundsException("index " + index);
        }
        int at = index;
        if (codePointOffset >= 0) {
            int step = 0;
            while (step < codePointOffset) {
                if (at >= seq.length()) {
                    throw new IndexOutOfBoundsException("ran off the end at " + at);
                }
                char c = seq.charAt(at);
                at = at + 1;
                if (Character.isHighSurrogate(c) && at < seq.length()
                        && Character.isLowSurrogate(seq.charAt(at))) {
                    at = at + 1;
                }
                step = step + 1;
            }
            return at;
        }
        int back = 0;
        while (back > codePointOffset) {
            if (at <= 0) {
                throw new IndexOutOfBoundsException("ran off the start at " + at);
            }
            char c = seq.charAt(at - 1);
            at = at - 1;
            if (Character.isLowSurrogate(c) && at > 0
                    && Character.isHighSurrogate(seq.charAt(at - 1))) {
                at = at - 1;
            }
            back = back - 1;
        }
        return at;
    }

    /**
     * The index {@code codePointOffset} code points away from {@code index}, within the
     * {@code count} characters starting at {@code start}.
     *
     * @param a where to walk
     * @param start where the readable part begins
     * @param count how long the readable part is
     * @param index where to start walking
     * @param codePointOffset how many code points to move; negative walks backwards
     */
    public static int offsetByCodePoints(char[] a, int start, int count, int index,
            int codePointOffset) {
        int end = start + count;
        if (count < 0 || start < 0 || end > a.length || index < start || index > end) {
            throw new IndexOutOfBoundsException(
                    "start " + start + ", count " + count + ", index " + index);
        }
        int at = index;
        if (codePointOffset >= 0) {
            int step = 0;
            while (step < codePointOffset) {
                if (at >= end) {
                    throw new IndexOutOfBoundsException("ran off the end at " + at);
                }
                char c = a[at];
                at = at + 1;
                if (Character.isHighSurrogate(c) && at < end && Character.isLowSurrogate(a[at])) {
                    at = at + 1;
                }
                step = step + 1;
            }
            return at;
        }
        int back = 0;
        while (back > codePointOffset) {
            if (at <= start) {
                throw new IndexOutOfBoundsException("ran off the start at " + at);
            }
            char c = a[at - 1];
            at = at - 1;
            if (Character.isLowSurrogate(c) && at > start
                    && Character.isHighSurrogate(a[at - 1])) {
                at = at - 1;
            }
            back = back - 1;
        }
        return at;
    }

    // ---- the tables, and how they are read ----

    // The alphabet position of `cp` plus ten, so that 'a' is 10 and 'z' is 35, or -1 when it is
    // not a letter that counts as a digit.
    //
    // The FULLWIDTH forms count too, and that is not an afterthought: they are the letters as
    // written in CJK text, and a hexadecimal number typed there is still a hexadecimal number.
    private static int letterValue(int cp) {
        if (cp >= 'a' && cp <= 'z') {
            return cp - 'a' + 10;
        }
        if (cp >= 'A' && cp <= 'Z') {
            return cp - 'A' + 10;
        }
        if (cp >= 0xff41 && cp <= 0xff5a) {
            return cp - 0xff41 + 10;
        }
        if (cp >= 0xff21 && cp <= 0xff3a) {
            return cp - 0xff21 + 10;
        }
        return -1;
    }

    // The value of `cp` as a DECIMAL digit, in whatever script writes it, or -1.
    //
    // Every span of the digit table is exactly the ten digits of one script, in order, so the
    // value is the distance from the start of the span. That is a property of Unicode -- decimal
    // digits are always encoded as a contiguous run of ten -- and not an assumption about the
    // table.
    private static int decimalValue(int cp) {
        int lo = 0;
        int hi = Character.DIGIT.length / 2 - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int start = Character.DIGIT[mid * 2];
            int end = Character.DIGIT[mid * 2 + 1];
            if (cp < start) {
                hi = mid - 1;
            } else if (cp > end) {
                lo = mid + 1;
            } else {
                return cp - start;
            }
        }
        return -1;
    }

    // Bisection over (start, end) spans: the spans are disjoint and sorted, so the answer is
    // "is there a span whose start is at or below cp and whose end is at or above it".
    private static boolean inSpans(int[] spans, int cp) {
        int lo = 0;
        int hi = spans.length / 2 - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int start = spans[mid * 2];
            int end = spans[mid * 2 + 1];
            if (cp < start) {
                hi = mid - 1;
            } else if (cp > end) {
                lo = mid + 1;
            } else {
                return true;
            }
        }
        return false;
    }

    // The same bisection over (start, end, delta) runs, returning cp itself when no run holds
    // it -- which is the identity mapping, and the common case.
    private static int mapped(int[] runs, int cp) {
        int lo = 0;
        int hi = runs.length / 3 - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int start = runs[mid * 3];
            int end = runs[mid * 3 + 1];
            if (cp < start) {
                hi = mid - 1;
            } else if (cp > end) {
                lo = mid + 1;
            } else {
                return cp + runs[mid * 3 + 2];
            }
        }
        return cp;
    }

    // Generated from the reference JDK by tools/gencase, not transcribed by hand. Each table is
    // built by its own method rather than in a field initializer: ten thousand array stores in
    // one <clinit> would run into the 64 KB limit on a method's code.
    private static final int[] LOWER = Character.lowerTable();
    private static final int[] UPPER = Character.upperTable();
    private static final int[] TITLE = Character.titleTable();
    private static final int[] LETTER = Character.letterTable();
    private static final int[] DIGIT = Character.digitTable();
    private static final int[] WHITESPACE = Character.whitespaceTable();
    private static final int[] SPACECHAR = Character.spacecharTable();
    private static final int[] UPPERCASE = Character.uppercaseTable();
    private static final int[] LOWERCASE = Character.lowercaseTable();

    private static int[] lowerTable() {
        return new int[] {
            65, 90, 32, 192, 214, 32, 216, 222, 32, 256, 256, 1,
            258, 258, 1, 260, 260, 1, 262, 262, 1, 264, 264, 1,
            266, 266, 1, 268, 268, 1, 270, 270, 1, 272, 272, 1,
            274, 274, 1, 276, 276, 1, 278, 278, 1, 280, 280, 1,
            282, 282, 1, 284, 284, 1, 286, 286, 1, 288, 288, 1,
            290, 290, 1, 292, 292, 1, 294, 294, 1, 296, 296, 1,
            298, 298, 1, 300, 300, 1, 302, 302, 1, 304, 304, -199,
            306, 306, 1, 308, 308, 1, 310, 310, 1, 313, 313, 1,
            315, 315, 1, 317, 317, 1, 319, 319, 1, 321, 321, 1,
            323, 323, 1, 325, 325, 1, 327, 327, 1, 330, 330, 1,
            332, 332, 1, 334, 334, 1, 336, 336, 1, 338, 338, 1,
            340, 340, 1, 342, 342, 1, 344, 344, 1, 346, 346, 1,
            348, 348, 1, 350, 350, 1, 352, 352, 1, 354, 354, 1,
            356, 356, 1, 358, 358, 1, 360, 360, 1, 362, 362, 1,
            364, 364, 1, 366, 366, 1, 368, 368, 1, 370, 370, 1,
            372, 372, 1, 374, 374, 1, 376, 376, -121, 377, 377, 1,
            379, 379, 1, 381, 381, 1, 385, 385, 210, 386, 386, 1,
            388, 388, 1, 390, 390, 206, 391, 391, 1, 393, 394, 205,
            395, 395, 1, 398, 398, 79, 399, 399, 202, 400, 400, 203,
            401, 401, 1, 403, 403, 205, 404, 404, 207, 406, 406, 211,
            407, 407, 209, 408, 408, 1, 412, 412, 211, 413, 413, 213,
            415, 415, 214, 416, 416, 1, 418, 418, 1, 420, 420, 1,
            422, 422, 218, 423, 423, 1, 425, 425, 218, 428, 428, 1,
            430, 430, 218, 431, 431, 1, 433, 434, 217, 435, 435, 1,
            437, 437, 1, 439, 439, 219, 440, 440, 1, 444, 444, 1,
            452, 452, 2, 453, 453, 1, 455, 455, 2, 456, 456, 1,
            458, 458, 2, 459, 459, 1, 461, 461, 1, 463, 463, 1,
            465, 465, 1, 467, 467, 1, 469, 469, 1, 471, 471, 1,
            473, 473, 1, 475, 475, 1, 478, 478, 1, 480, 480, 1,
            482, 482, 1, 484, 484, 1, 486, 486, 1, 488, 488, 1,
            490, 490, 1, 492, 492, 1, 494, 494, 1, 497, 497, 2,
            498, 498, 1, 500, 500, 1, 502, 502, -97, 503, 503, -56,
            504, 504, 1, 506, 506, 1, 508, 508, 1, 510, 510, 1,
            512, 512, 1, 514, 514, 1, 516, 516, 1, 518, 518, 1,
            520, 520, 1, 522, 522, 1, 524, 524, 1, 526, 526, 1,
            528, 528, 1, 530, 530, 1, 532, 532, 1, 534, 534, 1,
            536, 536, 1, 538, 538, 1, 540, 540, 1, 542, 542, 1,
            544, 544, -130, 546, 546, 1, 548, 548, 1, 550, 550, 1,
            552, 552, 1, 554, 554, 1, 556, 556, 1, 558, 558, 1,
            560, 560, 1, 562, 562, 1, 570, 570, 10795, 571, 571, 1,
            573, 573, -163, 574, 574, 10792, 577, 577, 1, 579, 579, -195,
            580, 580, 69, 581, 581, 71, 582, 582, 1, 584, 584, 1,
            586, 586, 1, 588, 588, 1, 590, 590, 1, 880, 880, 1,
            882, 882, 1, 886, 886, 1, 895, 895, 116, 902, 902, 38,
            904, 906, 37, 908, 908, 64, 910, 911, 63, 913, 929, 32,
            931, 939, 32, 975, 975, 8, 984, 984, 1, 986, 986, 1,
            988, 988, 1, 990, 990, 1, 992, 992, 1, 994, 994, 1,
            996, 996, 1, 998, 998, 1, 1000, 1000, 1, 1002, 1002, 1,
            1004, 1004, 1, 1006, 1006, 1, 1012, 1012, -60, 1015, 1015, 1,
            1017, 1017, -7, 1018, 1018, 1, 1021, 1023, -130, 1024, 1039, 80,
            1040, 1071, 32, 1120, 1120, 1, 1122, 1122, 1, 1124, 1124, 1,
            1126, 1126, 1, 1128, 1128, 1, 1130, 1130, 1, 1132, 1132, 1,
            1134, 1134, 1, 1136, 1136, 1, 1138, 1138, 1, 1140, 1140, 1,
            1142, 1142, 1, 1144, 1144, 1, 1146, 1146, 1, 1148, 1148, 1,
            1150, 1150, 1, 1152, 1152, 1, 1162, 1162, 1, 1164, 1164, 1,
            1166, 1166, 1, 1168, 1168, 1, 1170, 1170, 1, 1172, 1172, 1,
            1174, 1174, 1, 1176, 1176, 1, 1178, 1178, 1, 1180, 1180, 1,
            1182, 1182, 1, 1184, 1184, 1, 1186, 1186, 1, 1188, 1188, 1,
            1190, 1190, 1, 1192, 1192, 1, 1194, 1194, 1, 1196, 1196, 1,
            1198, 1198, 1, 1200, 1200, 1, 1202, 1202, 1, 1204, 1204, 1,
            1206, 1206, 1, 1208, 1208, 1, 1210, 1210, 1, 1212, 1212, 1,
            1214, 1214, 1, 1216, 1216, 15, 1217, 1217, 1, 1219, 1219, 1,
            1221, 1221, 1, 1223, 1223, 1, 1225, 1225, 1, 1227, 1227, 1,
            1229, 1229, 1, 1232, 1232, 1, 1234, 1234, 1, 1236, 1236, 1,
            1238, 1238, 1, 1240, 1240, 1, 1242, 1242, 1, 1244, 1244, 1,
            1246, 1246, 1, 1248, 1248, 1, 1250, 1250, 1, 1252, 1252, 1,
            1254, 1254, 1, 1256, 1256, 1, 1258, 1258, 1, 1260, 1260, 1,
            1262, 1262, 1, 1264, 1264, 1, 1266, 1266, 1, 1268, 1268, 1,
            1270, 1270, 1, 1272, 1272, 1, 1274, 1274, 1, 1276, 1276, 1,
            1278, 1278, 1, 1280, 1280, 1, 1282, 1282, 1, 1284, 1284, 1,
            1286, 1286, 1, 1288, 1288, 1, 1290, 1290, 1, 1292, 1292, 1,
            1294, 1294, 1, 1296, 1296, 1, 1298, 1298, 1, 1300, 1300, 1,
            1302, 1302, 1, 1304, 1304, 1, 1306, 1306, 1, 1308, 1308, 1,
            1310, 1310, 1, 1312, 1312, 1, 1314, 1314, 1, 1316, 1316, 1,
            1318, 1318, 1, 1320, 1320, 1, 1322, 1322, 1, 1324, 1324, 1,
            1326, 1326, 1, 1329, 1366, 48, 4256, 4293, 7264, 4295, 4295, 7264,
            4301, 4301, 7264, 5024, 5103, 38864, 5104, 5109, 8, 7305, 7305, 1,
            7312, 7354, -3008, 7357, 7359, -3008, 7680, 7680, 1, 7682, 7682, 1,
            7684, 7684, 1, 7686, 7686, 1, 7688, 7688, 1, 7690, 7690, 1,
            7692, 7692, 1, 7694, 7694, 1, 7696, 7696, 1, 7698, 7698, 1,
            7700, 7700, 1, 7702, 7702, 1, 7704, 7704, 1, 7706, 7706, 1,
            7708, 7708, 1, 7710, 7710, 1, 7712, 7712, 1, 7714, 7714, 1,
            7716, 7716, 1, 7718, 7718, 1, 7720, 7720, 1, 7722, 7722, 1,
            7724, 7724, 1, 7726, 7726, 1, 7728, 7728, 1, 7730, 7730, 1,
            7732, 7732, 1, 7734, 7734, 1, 7736, 7736, 1, 7738, 7738, 1,
            7740, 7740, 1, 7742, 7742, 1, 7744, 7744, 1, 7746, 7746, 1,
            7748, 7748, 1, 7750, 7750, 1, 7752, 7752, 1, 7754, 7754, 1,
            7756, 7756, 1, 7758, 7758, 1, 7760, 7760, 1, 7762, 7762, 1,
            7764, 7764, 1, 7766, 7766, 1, 7768, 7768, 1, 7770, 7770, 1,
            7772, 7772, 1, 7774, 7774, 1, 7776, 7776, 1, 7778, 7778, 1,
            7780, 7780, 1, 7782, 7782, 1, 7784, 7784, 1, 7786, 7786, 1,
            7788, 7788, 1, 7790, 7790, 1, 7792, 7792, 1, 7794, 7794, 1,
            7796, 7796, 1, 7798, 7798, 1, 7800, 7800, 1, 7802, 7802, 1,
            7804, 7804, 1, 7806, 7806, 1, 7808, 7808, 1, 7810, 7810, 1,
            7812, 7812, 1, 7814, 7814, 1, 7816, 7816, 1, 7818, 7818, 1,
            7820, 7820, 1, 7822, 7822, 1, 7824, 7824, 1, 7826, 7826, 1,
            7828, 7828, 1, 7838, 7838, -7615, 7840, 7840, 1, 7842, 7842, 1,
            7844, 7844, 1, 7846, 7846, 1, 7848, 7848, 1, 7850, 7850, 1,
            7852, 7852, 1, 7854, 7854, 1, 7856, 7856, 1, 7858, 7858, 1,
            7860, 7860, 1, 7862, 7862, 1, 7864, 7864, 1, 7866, 7866, 1,
            7868, 7868, 1, 7870, 7870, 1, 7872, 7872, 1, 7874, 7874, 1,
            7876, 7876, 1, 7878, 7878, 1, 7880, 7880, 1, 7882, 7882, 1,
            7884, 7884, 1, 7886, 7886, 1, 7888, 7888, 1, 7890, 7890, 1,
            7892, 7892, 1, 7894, 7894, 1, 7896, 7896, 1, 7898, 7898, 1,
            7900, 7900, 1, 7902, 7902, 1, 7904, 7904, 1, 7906, 7906, 1,
            7908, 7908, 1, 7910, 7910, 1, 7912, 7912, 1, 7914, 7914, 1,
            7916, 7916, 1, 7918, 7918, 1, 7920, 7920, 1, 7922, 7922, 1,
            7924, 7924, 1, 7926, 7926, 1, 7928, 7928, 1, 7930, 7930, 1,
            7932, 7932, 1, 7934, 7934, 1, 7944, 7951, -8, 7960, 7965, -8,
            7976, 7983, -8, 7992, 7999, -8, 8008, 8013, -8, 8025, 8025, -8,
            8027, 8027, -8, 8029, 8029, -8, 8031, 8031, -8, 8040, 8047, -8,
            8072, 8079, -8, 8088, 8095, -8, 8104, 8111, -8, 8120, 8121, -8,
            8122, 8123, -74, 8124, 8124, -9, 8136, 8139, -86, 8140, 8140, -9,
            8152, 8153, -8, 8154, 8155, -100, 8168, 8169, -8, 8170, 8171, -112,
            8172, 8172, -7, 8184, 8185, -128, 8186, 8187, -126, 8188, 8188, -9,
            8486, 8486, -7517, 8490, 8490, -8383, 8491, 8491, -8262, 8498, 8498, 28,
            8544, 8559, 16, 8579, 8579, 1, 9398, 9423, 26, 11264, 11311, 48,
            11360, 11360, 1, 11362, 11362, -10743, 11363, 11363, -3814, 11364, 11364, -10727,
            11367, 11367, 1, 11369, 11369, 1, 11371, 11371, 1, 11373, 11373, -10780,
            11374, 11374, -10749, 11375, 11375, -10783, 11376, 11376, -10782, 11378, 11378, 1,
            11381, 11381, 1, 11390, 11391, -10815, 11392, 11392, 1, 11394, 11394, 1,
            11396, 11396, 1, 11398, 11398, 1, 11400, 11400, 1, 11402, 11402, 1,
            11404, 11404, 1, 11406, 11406, 1, 11408, 11408, 1, 11410, 11410, 1,
            11412, 11412, 1, 11414, 11414, 1, 11416, 11416, 1, 11418, 11418, 1,
            11420, 11420, 1, 11422, 11422, 1, 11424, 11424, 1, 11426, 11426, 1,
            11428, 11428, 1, 11430, 11430, 1, 11432, 11432, 1, 11434, 11434, 1,
            11436, 11436, 1, 11438, 11438, 1, 11440, 11440, 1, 11442, 11442, 1,
            11444, 11444, 1, 11446, 11446, 1, 11448, 11448, 1, 11450, 11450, 1,
            11452, 11452, 1, 11454, 11454, 1, 11456, 11456, 1, 11458, 11458, 1,
            11460, 11460, 1, 11462, 11462, 1, 11464, 11464, 1, 11466, 11466, 1,
            11468, 11468, 1, 11470, 11470, 1, 11472, 11472, 1, 11474, 11474, 1,
            11476, 11476, 1, 11478, 11478, 1, 11480, 11480, 1, 11482, 11482, 1,
            11484, 11484, 1, 11486, 11486, 1, 11488, 11488, 1, 11490, 11490, 1,
            11499, 11499, 1, 11501, 11501, 1, 11506, 11506, 1, 42560, 42560, 1,
            42562, 42562, 1, 42564, 42564, 1, 42566, 42566, 1, 42568, 42568, 1,
            42570, 42570, 1, 42572, 42572, 1, 42574, 42574, 1, 42576, 42576, 1,
            42578, 42578, 1, 42580, 42580, 1, 42582, 42582, 1, 42584, 42584, 1,
            42586, 42586, 1, 42588, 42588, 1, 42590, 42590, 1, 42592, 42592, 1,
            42594, 42594, 1, 42596, 42596, 1, 42598, 42598, 1, 42600, 42600, 1,
            42602, 42602, 1, 42604, 42604, 1, 42624, 42624, 1, 42626, 42626, 1,
            42628, 42628, 1, 42630, 42630, 1, 42632, 42632, 1, 42634, 42634, 1,
            42636, 42636, 1, 42638, 42638, 1, 42640, 42640, 1, 42642, 42642, 1,
            42644, 42644, 1, 42646, 42646, 1, 42648, 42648, 1, 42650, 42650, 1,
            42786, 42786, 1, 42788, 42788, 1, 42790, 42790, 1, 42792, 42792, 1,
            42794, 42794, 1, 42796, 42796, 1, 42798, 42798, 1, 42802, 42802, 1,
            42804, 42804, 1, 42806, 42806, 1, 42808, 42808, 1, 42810, 42810, 1,
            42812, 42812, 1, 42814, 42814, 1, 42816, 42816, 1, 42818, 42818, 1,
            42820, 42820, 1, 42822, 42822, 1, 42824, 42824, 1, 42826, 42826, 1,
            42828, 42828, 1, 42830, 42830, 1, 42832, 42832, 1, 42834, 42834, 1,
            42836, 42836, 1, 42838, 42838, 1, 42840, 42840, 1, 42842, 42842, 1,
            42844, 42844, 1, 42846, 42846, 1, 42848, 42848, 1, 42850, 42850, 1,
            42852, 42852, 1, 42854, 42854, 1, 42856, 42856, 1, 42858, 42858, 1,
            42860, 42860, 1, 42862, 42862, 1, 42873, 42873, 1, 42875, 42875, 1,
            42877, 42877, -35332, 42878, 42878, 1, 42880, 42880, 1, 42882, 42882, 1,
            42884, 42884, 1, 42886, 42886, 1, 42891, 42891, 1, 42893, 42893, -42280,
            42896, 42896, 1, 42898, 42898, 1, 42902, 42902, 1, 42904, 42904, 1,
            42906, 42906, 1, 42908, 42908, 1, 42910, 42910, 1, 42912, 42912, 1,
            42914, 42914, 1, 42916, 42916, 1, 42918, 42918, 1, 42920, 42920, 1,
            42922, 42922, -42308, 42923, 42923, -42319, 42924, 42924, -42315, 42925, 42925, -42305,
            42926, 42926, -42308, 42928, 42928, -42258, 42929, 42929, -42282, 42930, 42930, -42261,
            42931, 42931, 928, 42932, 42932, 1, 42934, 42934, 1, 42936, 42936, 1,
            42938, 42938, 1, 42940, 42940, 1, 42942, 42942, 1, 42944, 42944, 1,
            42946, 42946, 1, 42948, 42948, -48, 42949, 42949, -42307, 42950, 42950, -35384,
            42951, 42951, 1, 42953, 42953, 1, 42955, 42955, -42343, 42956, 42956, 1,
            42960, 42960, 1, 42966, 42966, 1, 42968, 42968, 1, 42970, 42970, 1,
            42972, 42972, -42561, 42997, 42997, 1, 65313, 65338, 32, 66560, 66599, 40,
            66736, 66771, 40, 66928, 66938, 39, 66940, 66954, 39, 66956, 66962, 39,
            66964, 66965, 39, 68736, 68786, 64, 68944, 68965, 32, 71840, 71871, 32,
            93760, 93791, 32, 125184, 125217, 34,
        };
    }

    private static int[] upperTable() {
        return new int[] {
            97, 122, -32, 181, 181, 743, 224, 246, -32, 248, 254, -32,
            255, 255, 121, 257, 257, -1, 259, 259, -1, 261, 261, -1,
            263, 263, -1, 265, 265, -1, 267, 267, -1, 269, 269, -1,
            271, 271, -1, 273, 273, -1, 275, 275, -1, 277, 277, -1,
            279, 279, -1, 281, 281, -1, 283, 283, -1, 285, 285, -1,
            287, 287, -1, 289, 289, -1, 291, 291, -1, 293, 293, -1,
            295, 295, -1, 297, 297, -1, 299, 299, -1, 301, 301, -1,
            303, 303, -1, 305, 305, -232, 307, 307, -1, 309, 309, -1,
            311, 311, -1, 314, 314, -1, 316, 316, -1, 318, 318, -1,
            320, 320, -1, 322, 322, -1, 324, 324, -1, 326, 326, -1,
            328, 328, -1, 331, 331, -1, 333, 333, -1, 335, 335, -1,
            337, 337, -1, 339, 339, -1, 341, 341, -1, 343, 343, -1,
            345, 345, -1, 347, 347, -1, 349, 349, -1, 351, 351, -1,
            353, 353, -1, 355, 355, -1, 357, 357, -1, 359, 359, -1,
            361, 361, -1, 363, 363, -1, 365, 365, -1, 367, 367, -1,
            369, 369, -1, 371, 371, -1, 373, 373, -1, 375, 375, -1,
            378, 378, -1, 380, 380, -1, 382, 382, -1, 383, 383, -300,
            384, 384, 195, 387, 387, -1, 389, 389, -1, 392, 392, -1,
            396, 396, -1, 402, 402, -1, 405, 405, 97, 409, 409, -1,
            410, 410, 163, 411, 411, 42561, 414, 414, 130, 417, 417, -1,
            419, 419, -1, 421, 421, -1, 424, 424, -1, 429, 429, -1,
            432, 432, -1, 436, 436, -1, 438, 438, -1, 441, 441, -1,
            445, 445, -1, 447, 447, 56, 453, 453, -1, 454, 454, -2,
            456, 456, -1, 457, 457, -2, 459, 459, -1, 460, 460, -2,
            462, 462, -1, 464, 464, -1, 466, 466, -1, 468, 468, -1,
            470, 470, -1, 472, 472, -1, 474, 474, -1, 476, 476, -1,
            477, 477, -79, 479, 479, -1, 481, 481, -1, 483, 483, -1,
            485, 485, -1, 487, 487, -1, 489, 489, -1, 491, 491, -1,
            493, 493, -1, 495, 495, -1, 498, 498, -1, 499, 499, -2,
            501, 501, -1, 505, 505, -1, 507, 507, -1, 509, 509, -1,
            511, 511, -1, 513, 513, -1, 515, 515, -1, 517, 517, -1,
            519, 519, -1, 521, 521, -1, 523, 523, -1, 525, 525, -1,
            527, 527, -1, 529, 529, -1, 531, 531, -1, 533, 533, -1,
            535, 535, -1, 537, 537, -1, 539, 539, -1, 541, 541, -1,
            543, 543, -1, 547, 547, -1, 549, 549, -1, 551, 551, -1,
            553, 553, -1, 555, 555, -1, 557, 557, -1, 559, 559, -1,
            561, 561, -1, 563, 563, -1, 572, 572, -1, 575, 576, 10815,
            578, 578, -1, 583, 583, -1, 585, 585, -1, 587, 587, -1,
            589, 589, -1, 591, 591, -1, 592, 592, 10783, 593, 593, 10780,
            594, 594, 10782, 595, 595, -210, 596, 596, -206, 598, 599, -205,
            601, 601, -202, 603, 603, -203, 604, 604, 42319, 608, 608, -205,
            609, 609, 42315, 611, 611, -207, 612, 612, 42343, 613, 613, 42280,
            614, 614, 42308, 616, 616, -209, 617, 617, -211, 618, 618, 42308,
            619, 619, 10743, 620, 620, 42305, 623, 623, -211, 625, 625, 10749,
            626, 626, -213, 629, 629, -214, 637, 637, 10727, 640, 640, -218,
            642, 642, 42307, 643, 643, -218, 647, 647, 42282, 648, 648, -218,
            649, 649, -69, 650, 651, -217, 652, 652, -71, 658, 658, -219,
            669, 669, 42261, 670, 670, 42258, 837, 837, 84, 881, 881, -1,
            883, 883, -1, 887, 887, -1, 891, 893, 130, 940, 940, -38,
            941, 943, -37, 945, 961, -32, 962, 962, -31, 963, 971, -32,
            972, 972, -64, 973, 974, -63, 976, 976, -62, 977, 977, -57,
            981, 981, -47, 982, 982, -54, 983, 983, -8, 985, 985, -1,
            987, 987, -1, 989, 989, -1, 991, 991, -1, 993, 993, -1,
            995, 995, -1, 997, 997, -1, 999, 999, -1, 1001, 1001, -1,
            1003, 1003, -1, 1005, 1005, -1, 1007, 1007, -1, 1008, 1008, -86,
            1009, 1009, -80, 1010, 1010, 7, 1011, 1011, -116, 1013, 1013, -96,
            1016, 1016, -1, 1019, 1019, -1, 1072, 1103, -32, 1104, 1119, -80,
            1121, 1121, -1, 1123, 1123, -1, 1125, 1125, -1, 1127, 1127, -1,
            1129, 1129, -1, 1131, 1131, -1, 1133, 1133, -1, 1135, 1135, -1,
            1137, 1137, -1, 1139, 1139, -1, 1141, 1141, -1, 1143, 1143, -1,
            1145, 1145, -1, 1147, 1147, -1, 1149, 1149, -1, 1151, 1151, -1,
            1153, 1153, -1, 1163, 1163, -1, 1165, 1165, -1, 1167, 1167, -1,
            1169, 1169, -1, 1171, 1171, -1, 1173, 1173, -1, 1175, 1175, -1,
            1177, 1177, -1, 1179, 1179, -1, 1181, 1181, -1, 1183, 1183, -1,
            1185, 1185, -1, 1187, 1187, -1, 1189, 1189, -1, 1191, 1191, -1,
            1193, 1193, -1, 1195, 1195, -1, 1197, 1197, -1, 1199, 1199, -1,
            1201, 1201, -1, 1203, 1203, -1, 1205, 1205, -1, 1207, 1207, -1,
            1209, 1209, -1, 1211, 1211, -1, 1213, 1213, -1, 1215, 1215, -1,
            1218, 1218, -1, 1220, 1220, -1, 1222, 1222, -1, 1224, 1224, -1,
            1226, 1226, -1, 1228, 1228, -1, 1230, 1230, -1, 1231, 1231, -15,
            1233, 1233, -1, 1235, 1235, -1, 1237, 1237, -1, 1239, 1239, -1,
            1241, 1241, -1, 1243, 1243, -1, 1245, 1245, -1, 1247, 1247, -1,
            1249, 1249, -1, 1251, 1251, -1, 1253, 1253, -1, 1255, 1255, -1,
            1257, 1257, -1, 1259, 1259, -1, 1261, 1261, -1, 1263, 1263, -1,
            1265, 1265, -1, 1267, 1267, -1, 1269, 1269, -1, 1271, 1271, -1,
            1273, 1273, -1, 1275, 1275, -1, 1277, 1277, -1, 1279, 1279, -1,
            1281, 1281, -1, 1283, 1283, -1, 1285, 1285, -1, 1287, 1287, -1,
            1289, 1289, -1, 1291, 1291, -1, 1293, 1293, -1, 1295, 1295, -1,
            1297, 1297, -1, 1299, 1299, -1, 1301, 1301, -1, 1303, 1303, -1,
            1305, 1305, -1, 1307, 1307, -1, 1309, 1309, -1, 1311, 1311, -1,
            1313, 1313, -1, 1315, 1315, -1, 1317, 1317, -1, 1319, 1319, -1,
            1321, 1321, -1, 1323, 1323, -1, 1325, 1325, -1, 1327, 1327, -1,
            1377, 1414, -48, 4304, 4346, 3008, 4349, 4351, 3008, 5112, 5117, -8,
            7296, 7296, -6254, 7297, 7297, -6253, 7298, 7298, -6244, 7299, 7300, -6242,
            7301, 7301, -6243, 7302, 7302, -6236, 7303, 7303, -6181, 7304, 7304, 35266,
            7306, 7306, -1, 7545, 7545, 35332, 7549, 7549, 3814, 7566, 7566, 35384,
            7681, 7681, -1, 7683, 7683, -1, 7685, 7685, -1, 7687, 7687, -1,
            7689, 7689, -1, 7691, 7691, -1, 7693, 7693, -1, 7695, 7695, -1,
            7697, 7697, -1, 7699, 7699, -1, 7701, 7701, -1, 7703, 7703, -1,
            7705, 7705, -1, 7707, 7707, -1, 7709, 7709, -1, 7711, 7711, -1,
            7713, 7713, -1, 7715, 7715, -1, 7717, 7717, -1, 7719, 7719, -1,
            7721, 7721, -1, 7723, 7723, -1, 7725, 7725, -1, 7727, 7727, -1,
            7729, 7729, -1, 7731, 7731, -1, 7733, 7733, -1, 7735, 7735, -1,
            7737, 7737, -1, 7739, 7739, -1, 7741, 7741, -1, 7743, 7743, -1,
            7745, 7745, -1, 7747, 7747, -1, 7749, 7749, -1, 7751, 7751, -1,
            7753, 7753, -1, 7755, 7755, -1, 7757, 7757, -1, 7759, 7759, -1,
            7761, 7761, -1, 7763, 7763, -1, 7765, 7765, -1, 7767, 7767, -1,
            7769, 7769, -1, 7771, 7771, -1, 7773, 7773, -1, 7775, 7775, -1,
            7777, 7777, -1, 7779, 7779, -1, 7781, 7781, -1, 7783, 7783, -1,
            7785, 7785, -1, 7787, 7787, -1, 7789, 7789, -1, 7791, 7791, -1,
            7793, 7793, -1, 7795, 7795, -1, 7797, 7797, -1, 7799, 7799, -1,
            7801, 7801, -1, 7803, 7803, -1, 7805, 7805, -1, 7807, 7807, -1,
            7809, 7809, -1, 7811, 7811, -1, 7813, 7813, -1, 7815, 7815, -1,
            7817, 7817, -1, 7819, 7819, -1, 7821, 7821, -1, 7823, 7823, -1,
            7825, 7825, -1, 7827, 7827, -1, 7829, 7829, -1, 7835, 7835, -59,
            7841, 7841, -1, 7843, 7843, -1, 7845, 7845, -1, 7847, 7847, -1,
            7849, 7849, -1, 7851, 7851, -1, 7853, 7853, -1, 7855, 7855, -1,
            7857, 7857, -1, 7859, 7859, -1, 7861, 7861, -1, 7863, 7863, -1,
            7865, 7865, -1, 7867, 7867, -1, 7869, 7869, -1, 7871, 7871, -1,
            7873, 7873, -1, 7875, 7875, -1, 7877, 7877, -1, 7879, 7879, -1,
            7881, 7881, -1, 7883, 7883, -1, 7885, 7885, -1, 7887, 7887, -1,
            7889, 7889, -1, 7891, 7891, -1, 7893, 7893, -1, 7895, 7895, -1,
            7897, 7897, -1, 7899, 7899, -1, 7901, 7901, -1, 7903, 7903, -1,
            7905, 7905, -1, 7907, 7907, -1, 7909, 7909, -1, 7911, 7911, -1,
            7913, 7913, -1, 7915, 7915, -1, 7917, 7917, -1, 7919, 7919, -1,
            7921, 7921, -1, 7923, 7923, -1, 7925, 7925, -1, 7927, 7927, -1,
            7929, 7929, -1, 7931, 7931, -1, 7933, 7933, -1, 7935, 7935, -1,
            7936, 7943, 8, 7952, 7957, 8, 7968, 7975, 8, 7984, 7991, 8,
            8000, 8005, 8, 8017, 8017, 8, 8019, 8019, 8, 8021, 8021, 8,
            8023, 8023, 8, 8032, 8039, 8, 8048, 8049, 74, 8050, 8053, 86,
            8054, 8055, 100, 8056, 8057, 128, 8058, 8059, 112, 8060, 8061, 126,
            8064, 8071, 8, 8080, 8087, 8, 8096, 8103, 8, 8112, 8113, 8,
            8115, 8115, 9, 8126, 8126, -7205, 8131, 8131, 9, 8144, 8145, 8,
            8160, 8161, 8, 8165, 8165, 7, 8179, 8179, 9, 8526, 8526, -28,
            8560, 8575, -16, 8580, 8580, -1, 9424, 9449, -26, 11312, 11359, -48,
            11361, 11361, -1, 11365, 11365, -10795, 11366, 11366, -10792, 11368, 11368, -1,
            11370, 11370, -1, 11372, 11372, -1, 11379, 11379, -1, 11382, 11382, -1,
            11393, 11393, -1, 11395, 11395, -1, 11397, 11397, -1, 11399, 11399, -1,
            11401, 11401, -1, 11403, 11403, -1, 11405, 11405, -1, 11407, 11407, -1,
            11409, 11409, -1, 11411, 11411, -1, 11413, 11413, -1, 11415, 11415, -1,
            11417, 11417, -1, 11419, 11419, -1, 11421, 11421, -1, 11423, 11423, -1,
            11425, 11425, -1, 11427, 11427, -1, 11429, 11429, -1, 11431, 11431, -1,
            11433, 11433, -1, 11435, 11435, -1, 11437, 11437, -1, 11439, 11439, -1,
            11441, 11441, -1, 11443, 11443, -1, 11445, 11445, -1, 11447, 11447, -1,
            11449, 11449, -1, 11451, 11451, -1, 11453, 11453, -1, 11455, 11455, -1,
            11457, 11457, -1, 11459, 11459, -1, 11461, 11461, -1, 11463, 11463, -1,
            11465, 11465, -1, 11467, 11467, -1, 11469, 11469, -1, 11471, 11471, -1,
            11473, 11473, -1, 11475, 11475, -1, 11477, 11477, -1, 11479, 11479, -1,
            11481, 11481, -1, 11483, 11483, -1, 11485, 11485, -1, 11487, 11487, -1,
            11489, 11489, -1, 11491, 11491, -1, 11500, 11500, -1, 11502, 11502, -1,
            11507, 11507, -1, 11520, 11557, -7264, 11559, 11559, -7264, 11565, 11565, -7264,
            42561, 42561, -1, 42563, 42563, -1, 42565, 42565, -1, 42567, 42567, -1,
            42569, 42569, -1, 42571, 42571, -1, 42573, 42573, -1, 42575, 42575, -1,
            42577, 42577, -1, 42579, 42579, -1, 42581, 42581, -1, 42583, 42583, -1,
            42585, 42585, -1, 42587, 42587, -1, 42589, 42589, -1, 42591, 42591, -1,
            42593, 42593, -1, 42595, 42595, -1, 42597, 42597, -1, 42599, 42599, -1,
            42601, 42601, -1, 42603, 42603, -1, 42605, 42605, -1, 42625, 42625, -1,
            42627, 42627, -1, 42629, 42629, -1, 42631, 42631, -1, 42633, 42633, -1,
            42635, 42635, -1, 42637, 42637, -1, 42639, 42639, -1, 42641, 42641, -1,
            42643, 42643, -1, 42645, 42645, -1, 42647, 42647, -1, 42649, 42649, -1,
            42651, 42651, -1, 42787, 42787, -1, 42789, 42789, -1, 42791, 42791, -1,
            42793, 42793, -1, 42795, 42795, -1, 42797, 42797, -1, 42799, 42799, -1,
            42803, 42803, -1, 42805, 42805, -1, 42807, 42807, -1, 42809, 42809, -1,
            42811, 42811, -1, 42813, 42813, -1, 42815, 42815, -1, 42817, 42817, -1,
            42819, 42819, -1, 42821, 42821, -1, 42823, 42823, -1, 42825, 42825, -1,
            42827, 42827, -1, 42829, 42829, -1, 42831, 42831, -1, 42833, 42833, -1,
            42835, 42835, -1, 42837, 42837, -1, 42839, 42839, -1, 42841, 42841, -1,
            42843, 42843, -1, 42845, 42845, -1, 42847, 42847, -1, 42849, 42849, -1,
            42851, 42851, -1, 42853, 42853, -1, 42855, 42855, -1, 42857, 42857, -1,
            42859, 42859, -1, 42861, 42861, -1, 42863, 42863, -1, 42874, 42874, -1,
            42876, 42876, -1, 42879, 42879, -1, 42881, 42881, -1, 42883, 42883, -1,
            42885, 42885, -1, 42887, 42887, -1, 42892, 42892, -1, 42897, 42897, -1,
            42899, 42899, -1, 42900, 42900, 48, 42903, 42903, -1, 42905, 42905, -1,
            42907, 42907, -1, 42909, 42909, -1, 42911, 42911, -1, 42913, 42913, -1,
            42915, 42915, -1, 42917, 42917, -1, 42919, 42919, -1, 42921, 42921, -1,
            42933, 42933, -1, 42935, 42935, -1, 42937, 42937, -1, 42939, 42939, -1,
            42941, 42941, -1, 42943, 42943, -1, 42945, 42945, -1, 42947, 42947, -1,
            42952, 42952, -1, 42954, 42954, -1, 42957, 42957, -1, 42961, 42961, -1,
            42967, 42967, -1, 42969, 42969, -1, 42971, 42971, -1, 42998, 42998, -1,
            43859, 43859, -928, 43888, 43967, -38864, 65345, 65370, -32, 66600, 66639, -40,
            66776, 66811, -40, 66967, 66977, -39, 66979, 66993, -39, 66995, 67001, -39,
            67003, 67004, -39, 68800, 68850, -64, 68976, 68997, -32, 71872, 71903, -32,
            93792, 93823, -32, 125218, 125251, -34,
        };
    }

    private static int[] titleTable() {
        return new int[] {
            97, 122, -32, 181, 181, 743, 224, 246, -32, 248, 254, -32,
            255, 255, 121, 257, 257, -1, 259, 259, -1, 261, 261, -1,
            263, 263, -1, 265, 265, -1, 267, 267, -1, 269, 269, -1,
            271, 271, -1, 273, 273, -1, 275, 275, -1, 277, 277, -1,
            279, 279, -1, 281, 281, -1, 283, 283, -1, 285, 285, -1,
            287, 287, -1, 289, 289, -1, 291, 291, -1, 293, 293, -1,
            295, 295, -1, 297, 297, -1, 299, 299, -1, 301, 301, -1,
            303, 303, -1, 305, 305, -232, 307, 307, -1, 309, 309, -1,
            311, 311, -1, 314, 314, -1, 316, 316, -1, 318, 318, -1,
            320, 320, -1, 322, 322, -1, 324, 324, -1, 326, 326, -1,
            328, 328, -1, 331, 331, -1, 333, 333, -1, 335, 335, -1,
            337, 337, -1, 339, 339, -1, 341, 341, -1, 343, 343, -1,
            345, 345, -1, 347, 347, -1, 349, 349, -1, 351, 351, -1,
            353, 353, -1, 355, 355, -1, 357, 357, -1, 359, 359, -1,
            361, 361, -1, 363, 363, -1, 365, 365, -1, 367, 367, -1,
            369, 369, -1, 371, 371, -1, 373, 373, -1, 375, 375, -1,
            378, 378, -1, 380, 380, -1, 382, 382, -1, 383, 383, -300,
            384, 384, 195, 387, 387, -1, 389, 389, -1, 392, 392, -1,
            396, 396, -1, 402, 402, -1, 405, 405, 97, 409, 409, -1,
            410, 410, 163, 411, 411, 42561, 414, 414, 130, 417, 417, -1,
            419, 419, -1, 421, 421, -1, 424, 424, -1, 429, 429, -1,
            432, 432, -1, 436, 436, -1, 438, 438, -1, 441, 441, -1,
            445, 445, -1, 447, 447, 56, 452, 452, 1, 454, 454, -1,
            455, 455, 1, 457, 457, -1, 458, 458, 1, 460, 460, -1,
            462, 462, -1, 464, 464, -1, 466, 466, -1, 468, 468, -1,
            470, 470, -1, 472, 472, -1, 474, 474, -1, 476, 476, -1,
            477, 477, -79, 479, 479, -1, 481, 481, -1, 483, 483, -1,
            485, 485, -1, 487, 487, -1, 489, 489, -1, 491, 491, -1,
            493, 493, -1, 495, 495, -1, 497, 497, 1, 499, 499, -1,
            501, 501, -1, 505, 505, -1, 507, 507, -1, 509, 509, -1,
            511, 511, -1, 513, 513, -1, 515, 515, -1, 517, 517, -1,
            519, 519, -1, 521, 521, -1, 523, 523, -1, 525, 525, -1,
            527, 527, -1, 529, 529, -1, 531, 531, -1, 533, 533, -1,
            535, 535, -1, 537, 537, -1, 539, 539, -1, 541, 541, -1,
            543, 543, -1, 547, 547, -1, 549, 549, -1, 551, 551, -1,
            553, 553, -1, 555, 555, -1, 557, 557, -1, 559, 559, -1,
            561, 561, -1, 563, 563, -1, 572, 572, -1, 575, 576, 10815,
            578, 578, -1, 583, 583, -1, 585, 585, -1, 587, 587, -1,
            589, 589, -1, 591, 591, -1, 592, 592, 10783, 593, 593, 10780,
            594, 594, 10782, 595, 595, -210, 596, 596, -206, 598, 599, -205,
            601, 601, -202, 603, 603, -203, 604, 604, 42319, 608, 608, -205,
            609, 609, 42315, 611, 611, -207, 612, 612, 42343, 613, 613, 42280,
            614, 614, 42308, 616, 616, -209, 617, 617, -211, 618, 618, 42308,
            619, 619, 10743, 620, 620, 42305, 623, 623, -211, 625, 625, 10749,
            626, 626, -213, 629, 629, -214, 637, 637, 10727, 640, 640, -218,
            642, 642, 42307, 643, 643, -218, 647, 647, 42282, 648, 648, -218,
            649, 649, -69, 650, 651, -217, 652, 652, -71, 658, 658, -219,
            669, 669, 42261, 670, 670, 42258, 837, 837, 84, 881, 881, -1,
            883, 883, -1, 887, 887, -1, 891, 893, 130, 940, 940, -38,
            941, 943, -37, 945, 961, -32, 962, 962, -31, 963, 971, -32,
            972, 972, -64, 973, 974, -63, 976, 976, -62, 977, 977, -57,
            981, 981, -47, 982, 982, -54, 983, 983, -8, 985, 985, -1,
            987, 987, -1, 989, 989, -1, 991, 991, -1, 993, 993, -1,
            995, 995, -1, 997, 997, -1, 999, 999, -1, 1001, 1001, -1,
            1003, 1003, -1, 1005, 1005, -1, 1007, 1007, -1, 1008, 1008, -86,
            1009, 1009, -80, 1010, 1010, 7, 1011, 1011, -116, 1013, 1013, -96,
            1016, 1016, -1, 1019, 1019, -1, 1072, 1103, -32, 1104, 1119, -80,
            1121, 1121, -1, 1123, 1123, -1, 1125, 1125, -1, 1127, 1127, -1,
            1129, 1129, -1, 1131, 1131, -1, 1133, 1133, -1, 1135, 1135, -1,
            1137, 1137, -1, 1139, 1139, -1, 1141, 1141, -1, 1143, 1143, -1,
            1145, 1145, -1, 1147, 1147, -1, 1149, 1149, -1, 1151, 1151, -1,
            1153, 1153, -1, 1163, 1163, -1, 1165, 1165, -1, 1167, 1167, -1,
            1169, 1169, -1, 1171, 1171, -1, 1173, 1173, -1, 1175, 1175, -1,
            1177, 1177, -1, 1179, 1179, -1, 1181, 1181, -1, 1183, 1183, -1,
            1185, 1185, -1, 1187, 1187, -1, 1189, 1189, -1, 1191, 1191, -1,
            1193, 1193, -1, 1195, 1195, -1, 1197, 1197, -1, 1199, 1199, -1,
            1201, 1201, -1, 1203, 1203, -1, 1205, 1205, -1, 1207, 1207, -1,
            1209, 1209, -1, 1211, 1211, -1, 1213, 1213, -1, 1215, 1215, -1,
            1218, 1218, -1, 1220, 1220, -1, 1222, 1222, -1, 1224, 1224, -1,
            1226, 1226, -1, 1228, 1228, -1, 1230, 1230, -1, 1231, 1231, -15,
            1233, 1233, -1, 1235, 1235, -1, 1237, 1237, -1, 1239, 1239, -1,
            1241, 1241, -1, 1243, 1243, -1, 1245, 1245, -1, 1247, 1247, -1,
            1249, 1249, -1, 1251, 1251, -1, 1253, 1253, -1, 1255, 1255, -1,
            1257, 1257, -1, 1259, 1259, -1, 1261, 1261, -1, 1263, 1263, -1,
            1265, 1265, -1, 1267, 1267, -1, 1269, 1269, -1, 1271, 1271, -1,
            1273, 1273, -1, 1275, 1275, -1, 1277, 1277, -1, 1279, 1279, -1,
            1281, 1281, -1, 1283, 1283, -1, 1285, 1285, -1, 1287, 1287, -1,
            1289, 1289, -1, 1291, 1291, -1, 1293, 1293, -1, 1295, 1295, -1,
            1297, 1297, -1, 1299, 1299, -1, 1301, 1301, -1, 1303, 1303, -1,
            1305, 1305, -1, 1307, 1307, -1, 1309, 1309, -1, 1311, 1311, -1,
            1313, 1313, -1, 1315, 1315, -1, 1317, 1317, -1, 1319, 1319, -1,
            1321, 1321, -1, 1323, 1323, -1, 1325, 1325, -1, 1327, 1327, -1,
            1377, 1414, -48, 5112, 5117, -8, 7296, 7296, -6254, 7297, 7297, -6253,
            7298, 7298, -6244, 7299, 7300, -6242, 7301, 7301, -6243, 7302, 7302, -6236,
            7303, 7303, -6181, 7304, 7304, 35266, 7306, 7306, -1, 7545, 7545, 35332,
            7549, 7549, 3814, 7566, 7566, 35384, 7681, 7681, -1, 7683, 7683, -1,
            7685, 7685, -1, 7687, 7687, -1, 7689, 7689, -1, 7691, 7691, -1,
            7693, 7693, -1, 7695, 7695, -1, 7697, 7697, -1, 7699, 7699, -1,
            7701, 7701, -1, 7703, 7703, -1, 7705, 7705, -1, 7707, 7707, -1,
            7709, 7709, -1, 7711, 7711, -1, 7713, 7713, -1, 7715, 7715, -1,
            7717, 7717, -1, 7719, 7719, -1, 7721, 7721, -1, 7723, 7723, -1,
            7725, 7725, -1, 7727, 7727, -1, 7729, 7729, -1, 7731, 7731, -1,
            7733, 7733, -1, 7735, 7735, -1, 7737, 7737, -1, 7739, 7739, -1,
            7741, 7741, -1, 7743, 7743, -1, 7745, 7745, -1, 7747, 7747, -1,
            7749, 7749, -1, 7751, 7751, -1, 7753, 7753, -1, 7755, 7755, -1,
            7757, 7757, -1, 7759, 7759, -1, 7761, 7761, -1, 7763, 7763, -1,
            7765, 7765, -1, 7767, 7767, -1, 7769, 7769, -1, 7771, 7771, -1,
            7773, 7773, -1, 7775, 7775, -1, 7777, 7777, -1, 7779, 7779, -1,
            7781, 7781, -1, 7783, 7783, -1, 7785, 7785, -1, 7787, 7787, -1,
            7789, 7789, -1, 7791, 7791, -1, 7793, 7793, -1, 7795, 7795, -1,
            7797, 7797, -1, 7799, 7799, -1, 7801, 7801, -1, 7803, 7803, -1,
            7805, 7805, -1, 7807, 7807, -1, 7809, 7809, -1, 7811, 7811, -1,
            7813, 7813, -1, 7815, 7815, -1, 7817, 7817, -1, 7819, 7819, -1,
            7821, 7821, -1, 7823, 7823, -1, 7825, 7825, -1, 7827, 7827, -1,
            7829, 7829, -1, 7835, 7835, -59, 7841, 7841, -1, 7843, 7843, -1,
            7845, 7845, -1, 7847, 7847, -1, 7849, 7849, -1, 7851, 7851, -1,
            7853, 7853, -1, 7855, 7855, -1, 7857, 7857, -1, 7859, 7859, -1,
            7861, 7861, -1, 7863, 7863, -1, 7865, 7865, -1, 7867, 7867, -1,
            7869, 7869, -1, 7871, 7871, -1, 7873, 7873, -1, 7875, 7875, -1,
            7877, 7877, -1, 7879, 7879, -1, 7881, 7881, -1, 7883, 7883, -1,
            7885, 7885, -1, 7887, 7887, -1, 7889, 7889, -1, 7891, 7891, -1,
            7893, 7893, -1, 7895, 7895, -1, 7897, 7897, -1, 7899, 7899, -1,
            7901, 7901, -1, 7903, 7903, -1, 7905, 7905, -1, 7907, 7907, -1,
            7909, 7909, -1, 7911, 7911, -1, 7913, 7913, -1, 7915, 7915, -1,
            7917, 7917, -1, 7919, 7919, -1, 7921, 7921, -1, 7923, 7923, -1,
            7925, 7925, -1, 7927, 7927, -1, 7929, 7929, -1, 7931, 7931, -1,
            7933, 7933, -1, 7935, 7935, -1, 7936, 7943, 8, 7952, 7957, 8,
            7968, 7975, 8, 7984, 7991, 8, 8000, 8005, 8, 8017, 8017, 8,
            8019, 8019, 8, 8021, 8021, 8, 8023, 8023, 8, 8032, 8039, 8,
            8048, 8049, 74, 8050, 8053, 86, 8054, 8055, 100, 8056, 8057, 128,
            8058, 8059, 112, 8060, 8061, 126, 8064, 8071, 8, 8080, 8087, 8,
            8096, 8103, 8, 8112, 8113, 8, 8115, 8115, 9, 8126, 8126, -7205,
            8131, 8131, 9, 8144, 8145, 8, 8160, 8161, 8, 8165, 8165, 7,
            8179, 8179, 9, 8526, 8526, -28, 8560, 8575, -16, 8580, 8580, -1,
            9424, 9449, -26, 11312, 11359, -48, 11361, 11361, -1, 11365, 11365, -10795,
            11366, 11366, -10792, 11368, 11368, -1, 11370, 11370, -1, 11372, 11372, -1,
            11379, 11379, -1, 11382, 11382, -1, 11393, 11393, -1, 11395, 11395, -1,
            11397, 11397, -1, 11399, 11399, -1, 11401, 11401, -1, 11403, 11403, -1,
            11405, 11405, -1, 11407, 11407, -1, 11409, 11409, -1, 11411, 11411, -1,
            11413, 11413, -1, 11415, 11415, -1, 11417, 11417, -1, 11419, 11419, -1,
            11421, 11421, -1, 11423, 11423, -1, 11425, 11425, -1, 11427, 11427, -1,
            11429, 11429, -1, 11431, 11431, -1, 11433, 11433, -1, 11435, 11435, -1,
            11437, 11437, -1, 11439, 11439, -1, 11441, 11441, -1, 11443, 11443, -1,
            11445, 11445, -1, 11447, 11447, -1, 11449, 11449, -1, 11451, 11451, -1,
            11453, 11453, -1, 11455, 11455, -1, 11457, 11457, -1, 11459, 11459, -1,
            11461, 11461, -1, 11463, 11463, -1, 11465, 11465, -1, 11467, 11467, -1,
            11469, 11469, -1, 11471, 11471, -1, 11473, 11473, -1, 11475, 11475, -1,
            11477, 11477, -1, 11479, 11479, -1, 11481, 11481, -1, 11483, 11483, -1,
            11485, 11485, -1, 11487, 11487, -1, 11489, 11489, -1, 11491, 11491, -1,
            11500, 11500, -1, 11502, 11502, -1, 11507, 11507, -1, 11520, 11557, -7264,
            11559, 11559, -7264, 11565, 11565, -7264, 42561, 42561, -1, 42563, 42563, -1,
            42565, 42565, -1, 42567, 42567, -1, 42569, 42569, -1, 42571, 42571, -1,
            42573, 42573, -1, 42575, 42575, -1, 42577, 42577, -1, 42579, 42579, -1,
            42581, 42581, -1, 42583, 42583, -1, 42585, 42585, -1, 42587, 42587, -1,
            42589, 42589, -1, 42591, 42591, -1, 42593, 42593, -1, 42595, 42595, -1,
            42597, 42597, -1, 42599, 42599, -1, 42601, 42601, -1, 42603, 42603, -1,
            42605, 42605, -1, 42625, 42625, -1, 42627, 42627, -1, 42629, 42629, -1,
            42631, 42631, -1, 42633, 42633, -1, 42635, 42635, -1, 42637, 42637, -1,
            42639, 42639, -1, 42641, 42641, -1, 42643, 42643, -1, 42645, 42645, -1,
            42647, 42647, -1, 42649, 42649, -1, 42651, 42651, -1, 42787, 42787, -1,
            42789, 42789, -1, 42791, 42791, -1, 42793, 42793, -1, 42795, 42795, -1,
            42797, 42797, -1, 42799, 42799, -1, 42803, 42803, -1, 42805, 42805, -1,
            42807, 42807, -1, 42809, 42809, -1, 42811, 42811, -1, 42813, 42813, -1,
            42815, 42815, -1, 42817, 42817, -1, 42819, 42819, -1, 42821, 42821, -1,
            42823, 42823, -1, 42825, 42825, -1, 42827, 42827, -1, 42829, 42829, -1,
            42831, 42831, -1, 42833, 42833, -1, 42835, 42835, -1, 42837, 42837, -1,
            42839, 42839, -1, 42841, 42841, -1, 42843, 42843, -1, 42845, 42845, -1,
            42847, 42847, -1, 42849, 42849, -1, 42851, 42851, -1, 42853, 42853, -1,
            42855, 42855, -1, 42857, 42857, -1, 42859, 42859, -1, 42861, 42861, -1,
            42863, 42863, -1, 42874, 42874, -1, 42876, 42876, -1, 42879, 42879, -1,
            42881, 42881, -1, 42883, 42883, -1, 42885, 42885, -1, 42887, 42887, -1,
            42892, 42892, -1, 42897, 42897, -1, 42899, 42899, -1, 42900, 42900, 48,
            42903, 42903, -1, 42905, 42905, -1, 42907, 42907, -1, 42909, 42909, -1,
            42911, 42911, -1, 42913, 42913, -1, 42915, 42915, -1, 42917, 42917, -1,
            42919, 42919, -1, 42921, 42921, -1, 42933, 42933, -1, 42935, 42935, -1,
            42937, 42937, -1, 42939, 42939, -1, 42941, 42941, -1, 42943, 42943, -1,
            42945, 42945, -1, 42947, 42947, -1, 42952, 42952, -1, 42954, 42954, -1,
            42957, 42957, -1, 42961, 42961, -1, 42967, 42967, -1, 42969, 42969, -1,
            42971, 42971, -1, 42998, 42998, -1, 43859, 43859, -928, 43888, 43967, -38864,
            65345, 65370, -32, 66600, 66639, -40, 66776, 66811, -40, 66967, 66977, -39,
            66979, 66993, -39, 66995, 67001, -39, 67003, 67004, -39, 68800, 68850, -64,
            68976, 68997, -32, 71872, 71903, -32, 93792, 93823, -32, 125218, 125251, -34,
        };
    }

    private static int[] letterTable() {
        return new int[] {
            65, 90, 97, 122, 170, 170, 181, 181, 186, 186, 192, 214,
            216, 246, 248, 705, 710, 721, 736, 740, 748, 748, 750, 750,
            880, 884, 886, 887, 890, 893, 895, 895, 902, 902, 904, 906,
            908, 908, 910, 929, 931, 1013, 1015, 1153, 1162, 1327, 1329, 1366,
            1369, 1369, 1376, 1416, 1488, 1514, 1519, 1522, 1568, 1610, 1646, 1647,
            1649, 1747, 1749, 1749, 1765, 1766, 1774, 1775, 1786, 1788, 1791, 1791,
            1808, 1808, 1810, 1839, 1869, 1957, 1969, 1969, 1994, 2026, 2036, 2037,
            2042, 2042, 2048, 2069, 2074, 2074, 2084, 2084, 2088, 2088, 2112, 2136,
            2144, 2154, 2160, 2183, 2185, 2190, 2208, 2249, 2308, 2361, 2365, 2365,
            2384, 2384, 2392, 2401, 2417, 2432, 2437, 2444, 2447, 2448, 2451, 2472,
            2474, 2480, 2482, 2482, 2486, 2489, 2493, 2493, 2510, 2510, 2524, 2525,
            2527, 2529, 2544, 2545, 2556, 2556, 2565, 2570, 2575, 2576, 2579, 2600,
            2602, 2608, 2610, 2611, 2613, 2614, 2616, 2617, 2649, 2652, 2654, 2654,
            2674, 2676, 2693, 2701, 2703, 2705, 2707, 2728, 2730, 2736, 2738, 2739,
            2741, 2745, 2749, 2749, 2768, 2768, 2784, 2785, 2809, 2809, 2821, 2828,
            2831, 2832, 2835, 2856, 2858, 2864, 2866, 2867, 2869, 2873, 2877, 2877,
            2908, 2909, 2911, 2913, 2929, 2929, 2947, 2947, 2949, 2954, 2958, 2960,
            2962, 2965, 2969, 2970, 2972, 2972, 2974, 2975, 2979, 2980, 2984, 2986,
            2990, 3001, 3024, 3024, 3077, 3084, 3086, 3088, 3090, 3112, 3114, 3129,
            3133, 3133, 3160, 3162, 3165, 3165, 3168, 3169, 3200, 3200, 3205, 3212,
            3214, 3216, 3218, 3240, 3242, 3251, 3253, 3257, 3261, 3261, 3293, 3294,
            3296, 3297, 3313, 3314, 3332, 3340, 3342, 3344, 3346, 3386, 3389, 3389,
            3406, 3406, 3412, 3414, 3423, 3425, 3450, 3455, 3461, 3478, 3482, 3505,
            3507, 3515, 3517, 3517, 3520, 3526, 3585, 3632, 3634, 3635, 3648, 3654,
            3713, 3714, 3716, 3716, 3718, 3722, 3724, 3747, 3749, 3749, 3751, 3760,
            3762, 3763, 3773, 3773, 3776, 3780, 3782, 3782, 3804, 3807, 3840, 3840,
            3904, 3911, 3913, 3948, 3976, 3980, 4096, 4138, 4159, 4159, 4176, 4181,
            4186, 4189, 4193, 4193, 4197, 4198, 4206, 4208, 4213, 4225, 4238, 4238,
            4256, 4293, 4295, 4295, 4301, 4301, 4304, 4346, 4348, 4680, 4682, 4685,
            4688, 4694, 4696, 4696, 4698, 4701, 4704, 4744, 4746, 4749, 4752, 4784,
            4786, 4789, 4792, 4798, 4800, 4800, 4802, 4805, 4808, 4822, 4824, 4880,
            4882, 4885, 4888, 4954, 4992, 5007, 5024, 5109, 5112, 5117, 5121, 5740,
            5743, 5759, 5761, 5786, 5792, 5866, 5873, 5880, 5888, 5905, 5919, 5937,
            5952, 5969, 5984, 5996, 5998, 6000, 6016, 6067, 6103, 6103, 6108, 6108,
            6176, 6264, 6272, 6276, 6279, 6312, 6314, 6314, 6320, 6389, 6400, 6430,
            6480, 6509, 6512, 6516, 6528, 6571, 6576, 6601, 6656, 6678, 6688, 6740,
            6823, 6823, 6917, 6963, 6981, 6988, 7043, 7072, 7086, 7087, 7098, 7141,
            7168, 7203, 7245, 7247, 7258, 7293, 7296, 7306, 7312, 7354, 7357, 7359,
            7401, 7404, 7406, 7411, 7413, 7414, 7418, 7418, 7424, 7615, 7680, 7957,
            7960, 7965, 7968, 8005, 8008, 8013, 8016, 8023, 8025, 8025, 8027, 8027,
            8029, 8029, 8031, 8061, 8064, 8116, 8118, 8124, 8126, 8126, 8130, 8132,
            8134, 8140, 8144, 8147, 8150, 8155, 8160, 8172, 8178, 8180, 8182, 8188,
            8305, 8305, 8319, 8319, 8336, 8348, 8450, 8450, 8455, 8455, 8458, 8467,
            8469, 8469, 8473, 8477, 8484, 8484, 8486, 8486, 8488, 8488, 8490, 8493,
            8495, 8505, 8508, 8511, 8517, 8521, 8526, 8526, 8579, 8580, 11264, 11492,
            11499, 11502, 11506, 11507, 11520, 11557, 11559, 11559, 11565, 11565, 11568, 11623,
            11631, 11631, 11648, 11670, 11680, 11686, 11688, 11694, 11696, 11702, 11704, 11710,
            11712, 11718, 11720, 11726, 11728, 11734, 11736, 11742, 11823, 11823, 12293, 12294,
            12337, 12341, 12347, 12348, 12353, 12438, 12445, 12447, 12449, 12538, 12540, 12543,
            12549, 12591, 12593, 12686, 12704, 12735, 12784, 12799, 13312, 19903, 19968, 42124,
            42192, 42237, 42240, 42508, 42512, 42527, 42538, 42539, 42560, 42606, 42623, 42653,
            42656, 42725, 42775, 42783, 42786, 42888, 42891, 42957, 42960, 42961, 42963, 42963,
            42965, 42972, 42994, 43009, 43011, 43013, 43015, 43018, 43020, 43042, 43072, 43123,
            43138, 43187, 43250, 43255, 43259, 43259, 43261, 43262, 43274, 43301, 43312, 43334,
            43360, 43388, 43396, 43442, 43471, 43471, 43488, 43492, 43494, 43503, 43514, 43518,
            43520, 43560, 43584, 43586, 43588, 43595, 43616, 43638, 43642, 43642, 43646, 43695,
            43697, 43697, 43701, 43702, 43705, 43709, 43712, 43712, 43714, 43714, 43739, 43741,
            43744, 43754, 43762, 43764, 43777, 43782, 43785, 43790, 43793, 43798, 43808, 43814,
            43816, 43822, 43824, 43866, 43868, 43881, 43888, 44002, 44032, 55203, 55216, 55238,
            55243, 55291, 63744, 64109, 64112, 64217, 64256, 64262, 64275, 64279, 64285, 64285,
            64287, 64296, 64298, 64310, 64312, 64316, 64318, 64318, 64320, 64321, 64323, 64324,
            64326, 64433, 64467, 64829, 64848, 64911, 64914, 64967, 65008, 65019, 65136, 65140,
            65142, 65276, 65313, 65338, 65345, 65370, 65382, 65470, 65474, 65479, 65482, 65487,
            65490, 65495, 65498, 65500, 65536, 65547, 65549, 65574, 65576, 65594, 65596, 65597,
            65599, 65613, 65616, 65629, 65664, 65786, 66176, 66204, 66208, 66256, 66304, 66335,
            66349, 66368, 66370, 66377, 66384, 66421, 66432, 66461, 66464, 66499, 66504, 66511,
            66560, 66717, 66736, 66771, 66776, 66811, 66816, 66855, 66864, 66915, 66928, 66938,
            66940, 66954, 66956, 66962, 66964, 66965, 66967, 66977, 66979, 66993, 66995, 67001,
            67003, 67004, 67008, 67059, 67072, 67382, 67392, 67413, 67424, 67431, 67456, 67461,
            67463, 67504, 67506, 67514, 67584, 67589, 67592, 67592, 67594, 67637, 67639, 67640,
            67644, 67644, 67647, 67669, 67680, 67702, 67712, 67742, 67808, 67826, 67828, 67829,
            67840, 67861, 67872, 67897, 67968, 68023, 68030, 68031, 68096, 68096, 68112, 68115,
            68117, 68119, 68121, 68149, 68192, 68220, 68224, 68252, 68288, 68295, 68297, 68324,
            68352, 68405, 68416, 68437, 68448, 68466, 68480, 68497, 68608, 68680, 68736, 68786,
            68800, 68850, 68864, 68899, 68938, 68965, 68975, 68997, 69248, 69289, 69296, 69297,
            69314, 69316, 69376, 69404, 69415, 69415, 69424, 69445, 69488, 69505, 69552, 69572,
            69600, 69622, 69635, 69687, 69745, 69746, 69749, 69749, 69763, 69807, 69840, 69864,
            69891, 69926, 69956, 69956, 69959, 69959, 69968, 70002, 70006, 70006, 70019, 70066,
            70081, 70084, 70106, 70106, 70108, 70108, 70144, 70161, 70163, 70187, 70207, 70208,
            70272, 70278, 70280, 70280, 70282, 70285, 70287, 70301, 70303, 70312, 70320, 70366,
            70405, 70412, 70415, 70416, 70419, 70440, 70442, 70448, 70450, 70451, 70453, 70457,
            70461, 70461, 70480, 70480, 70493, 70497, 70528, 70537, 70539, 70539, 70542, 70542,
            70544, 70581, 70583, 70583, 70609, 70609, 70611, 70611, 70656, 70708, 70727, 70730,
            70751, 70753, 70784, 70831, 70852, 70853, 70855, 70855, 71040, 71086, 71128, 71131,
            71168, 71215, 71236, 71236, 71296, 71338, 71352, 71352, 71424, 71450, 71488, 71494,
            71680, 71723, 71840, 71903, 71935, 71942, 71945, 71945, 71948, 71955, 71957, 71958,
            71960, 71983, 71999, 71999, 72001, 72001, 72096, 72103, 72106, 72144, 72161, 72161,
            72163, 72163, 72192, 72192, 72203, 72242, 72250, 72250, 72272, 72272, 72284, 72329,
            72349, 72349, 72368, 72440, 72640, 72672, 72704, 72712, 72714, 72750, 72768, 72768,
            72818, 72847, 72960, 72966, 72968, 72969, 72971, 73008, 73030, 73030, 73056, 73061,
            73063, 73064, 73066, 73097, 73112, 73112, 73440, 73458, 73474, 73474, 73476, 73488,
            73490, 73523, 73648, 73648, 73728, 74649, 74880, 75075, 77712, 77808, 77824, 78895,
            78913, 78918, 78944, 82938, 82944, 83526, 90368, 90397, 92160, 92728, 92736, 92766,
            92784, 92862, 92880, 92909, 92928, 92975, 92992, 92995, 93027, 93047, 93053, 93071,
            93504, 93548, 93760, 93823, 93952, 94026, 94032, 94032, 94099, 94111, 94176, 94177,
            94179, 94179, 94208, 100343, 100352, 101589, 101631, 101640, 110576, 110579, 110581, 110587,
            110589, 110590, 110592, 110882, 110898, 110898, 110928, 110930, 110933, 110933, 110948, 110951,
            110960, 111355, 113664, 113770, 113776, 113788, 113792, 113800, 113808, 113817, 119808, 119892,
            119894, 119964, 119966, 119967, 119970, 119970, 119973, 119974, 119977, 119980, 119982, 119993,
            119995, 119995, 119997, 120003, 120005, 120069, 120071, 120074, 120077, 120084, 120086, 120092,
            120094, 120121, 120123, 120126, 120128, 120132, 120134, 120134, 120138, 120144, 120146, 120485,
            120488, 120512, 120514, 120538, 120540, 120570, 120572, 120596, 120598, 120628, 120630, 120654,
            120656, 120686, 120688, 120712, 120714, 120744, 120746, 120770, 120772, 120779, 122624, 122654,
            122661, 122666, 122928, 122989, 123136, 123180, 123191, 123197, 123214, 123214, 123536, 123565,
            123584, 123627, 124112, 124139, 124368, 124397, 124400, 124400, 124896, 124902, 124904, 124907,
            124909, 124910, 124912, 124926, 124928, 125124, 125184, 125251, 125259, 125259, 126464, 126467,
            126469, 126495, 126497, 126498, 126500, 126500, 126503, 126503, 126505, 126514, 126516, 126519,
            126521, 126521, 126523, 126523, 126530, 126530, 126535, 126535, 126537, 126537, 126539, 126539,
            126541, 126543, 126545, 126546, 126548, 126548, 126551, 126551, 126553, 126553, 126555, 126555,
            126557, 126557, 126559, 126559, 126561, 126562, 126564, 126564, 126567, 126570, 126572, 126578,
            126580, 126583, 126585, 126588, 126590, 126590, 126592, 126601, 126603, 126619, 126625, 126627,
            126629, 126633, 126635, 126651, 131072, 173791, 173824, 177977, 177984, 178205, 178208, 183969,
            183984, 191456, 191472, 192093, 194560, 195101, 196608, 201546, 201552, 205743,
        };
    }

    private static int[] digitTable() {
        return new int[] {
            48, 57, 1632, 1641, 1776, 1785, 1984, 1993, 2406, 2415, 2534, 2543,
            2662, 2671, 2790, 2799, 2918, 2927, 3046, 3055, 3174, 3183, 3302, 3311,
            3430, 3439, 3558, 3567, 3664, 3673, 3792, 3801, 3872, 3881, 4160, 4169,
            4240, 4249, 6112, 6121, 6160, 6169, 6470, 6479, 6608, 6617, 6784, 6793,
            6800, 6809, 6992, 7001, 7088, 7097, 7232, 7241, 7248, 7257, 42528, 42537,
            43216, 43225, 43264, 43273, 43472, 43481, 43504, 43513, 43600, 43609, 44016, 44025,
            65296, 65305, 66720, 66729, 68912, 68921, 68928, 68937, 69734, 69743, 69872, 69881,
            69942, 69951, 70096, 70105, 70384, 70393, 70736, 70745, 70864, 70873, 71248, 71257,
            71360, 71369, 71376, 71395, 71472, 71481, 71904, 71913, 72016, 72025, 72688, 72697,
            72784, 72793, 73040, 73049, 73120, 73129, 73552, 73561, 90416, 90425, 92768, 92777,
            92864, 92873, 93008, 93017, 93552, 93561, 118000, 118009, 120782, 120831, 123200, 123209,
            123632, 123641, 124144, 124153, 124401, 124410, 125264, 125273, 130032, 130041,
        };
    }

    private static int[] whitespaceTable() {
        return new int[] {
            9, 13, 28, 32, 5760, 5760, 8192, 8198, 8200, 8202, 8232, 8233,
            8287, 8287, 12288, 12288,
        };
    }

    private static int[] spacecharTable() {
        return new int[] {
            32, 32, 160, 160, 5760, 5760, 8192, 8202, 8232, 8233, 8239, 8239,
            8287, 8287, 12288, 12288,
        };
    }

    private static int[] uppercaseTable() {
        return new int[] {
            65, 90, 192, 214, 216, 222, 256, 256, 258, 258, 260, 260,
            262, 262, 264, 264, 266, 266, 268, 268, 270, 270, 272, 272,
            274, 274, 276, 276, 278, 278, 280, 280, 282, 282, 284, 284,
            286, 286, 288, 288, 290, 290, 292, 292, 294, 294, 296, 296,
            298, 298, 300, 300, 302, 302, 304, 304, 306, 306, 308, 308,
            310, 310, 313, 313, 315, 315, 317, 317, 319, 319, 321, 321,
            323, 323, 325, 325, 327, 327, 330, 330, 332, 332, 334, 334,
            336, 336, 338, 338, 340, 340, 342, 342, 344, 344, 346, 346,
            348, 348, 350, 350, 352, 352, 354, 354, 356, 356, 358, 358,
            360, 360, 362, 362, 364, 364, 366, 366, 368, 368, 370, 370,
            372, 372, 374, 374, 376, 377, 379, 379, 381, 381, 385, 386,
            388, 388, 390, 391, 393, 395, 398, 401, 403, 404, 406, 408,
            412, 413, 415, 416, 418, 418, 420, 420, 422, 423, 425, 425,
            428, 428, 430, 431, 433, 435, 437, 437, 439, 440, 444, 444,
            452, 452, 455, 455, 458, 458, 461, 461, 463, 463, 465, 465,
            467, 467, 469, 469, 471, 471, 473, 473, 475, 475, 478, 478,
            480, 480, 482, 482, 484, 484, 486, 486, 488, 488, 490, 490,
            492, 492, 494, 494, 497, 497, 500, 500, 502, 504, 506, 506,
            508, 508, 510, 510, 512, 512, 514, 514, 516, 516, 518, 518,
            520, 520, 522, 522, 524, 524, 526, 526, 528, 528, 530, 530,
            532, 532, 534, 534, 536, 536, 538, 538, 540, 540, 542, 542,
            544, 544, 546, 546, 548, 548, 550, 550, 552, 552, 554, 554,
            556, 556, 558, 558, 560, 560, 562, 562, 570, 571, 573, 574,
            577, 577, 579, 582, 584, 584, 586, 586, 588, 588, 590, 590,
            880, 880, 882, 882, 886, 886, 895, 895, 902, 902, 904, 906,
            908, 908, 910, 911, 913, 929, 931, 939, 975, 975, 978, 980,
            984, 984, 986, 986, 988, 988, 990, 990, 992, 992, 994, 994,
            996, 996, 998, 998, 1000, 1000, 1002, 1002, 1004, 1004, 1006, 1006,
            1012, 1012, 1015, 1015, 1017, 1018, 1021, 1071, 1120, 1120, 1122, 1122,
            1124, 1124, 1126, 1126, 1128, 1128, 1130, 1130, 1132, 1132, 1134, 1134,
            1136, 1136, 1138, 1138, 1140, 1140, 1142, 1142, 1144, 1144, 1146, 1146,
            1148, 1148, 1150, 1150, 1152, 1152, 1162, 1162, 1164, 1164, 1166, 1166,
            1168, 1168, 1170, 1170, 1172, 1172, 1174, 1174, 1176, 1176, 1178, 1178,
            1180, 1180, 1182, 1182, 1184, 1184, 1186, 1186, 1188, 1188, 1190, 1190,
            1192, 1192, 1194, 1194, 1196, 1196, 1198, 1198, 1200, 1200, 1202, 1202,
            1204, 1204, 1206, 1206, 1208, 1208, 1210, 1210, 1212, 1212, 1214, 1214,
            1216, 1217, 1219, 1219, 1221, 1221, 1223, 1223, 1225, 1225, 1227, 1227,
            1229, 1229, 1232, 1232, 1234, 1234, 1236, 1236, 1238, 1238, 1240, 1240,
            1242, 1242, 1244, 1244, 1246, 1246, 1248, 1248, 1250, 1250, 1252, 1252,
            1254, 1254, 1256, 1256, 1258, 1258, 1260, 1260, 1262, 1262, 1264, 1264,
            1266, 1266, 1268, 1268, 1270, 1270, 1272, 1272, 1274, 1274, 1276, 1276,
            1278, 1278, 1280, 1280, 1282, 1282, 1284, 1284, 1286, 1286, 1288, 1288,
            1290, 1290, 1292, 1292, 1294, 1294, 1296, 1296, 1298, 1298, 1300, 1300,
            1302, 1302, 1304, 1304, 1306, 1306, 1308, 1308, 1310, 1310, 1312, 1312,
            1314, 1314, 1316, 1316, 1318, 1318, 1320, 1320, 1322, 1322, 1324, 1324,
            1326, 1326, 1329, 1366, 4256, 4293, 4295, 4295, 4301, 4301, 5024, 5109,
            7305, 7305, 7312, 7354, 7357, 7359, 7680, 7680, 7682, 7682, 7684, 7684,
            7686, 7686, 7688, 7688, 7690, 7690, 7692, 7692, 7694, 7694, 7696, 7696,
            7698, 7698, 7700, 7700, 7702, 7702, 7704, 7704, 7706, 7706, 7708, 7708,
            7710, 7710, 7712, 7712, 7714, 7714, 7716, 7716, 7718, 7718, 7720, 7720,
            7722, 7722, 7724, 7724, 7726, 7726, 7728, 7728, 7730, 7730, 7732, 7732,
            7734, 7734, 7736, 7736, 7738, 7738, 7740, 7740, 7742, 7742, 7744, 7744,
            7746, 7746, 7748, 7748, 7750, 7750, 7752, 7752, 7754, 7754, 7756, 7756,
            7758, 7758, 7760, 7760, 7762, 7762, 7764, 7764, 7766, 7766, 7768, 7768,
            7770, 7770, 7772, 7772, 7774, 7774, 7776, 7776, 7778, 7778, 7780, 7780,
            7782, 7782, 7784, 7784, 7786, 7786, 7788, 7788, 7790, 7790, 7792, 7792,
            7794, 7794, 7796, 7796, 7798, 7798, 7800, 7800, 7802, 7802, 7804, 7804,
            7806, 7806, 7808, 7808, 7810, 7810, 7812, 7812, 7814, 7814, 7816, 7816,
            7818, 7818, 7820, 7820, 7822, 7822, 7824, 7824, 7826, 7826, 7828, 7828,
            7838, 7838, 7840, 7840, 7842, 7842, 7844, 7844, 7846, 7846, 7848, 7848,
            7850, 7850, 7852, 7852, 7854, 7854, 7856, 7856, 7858, 7858, 7860, 7860,
            7862, 7862, 7864, 7864, 7866, 7866, 7868, 7868, 7870, 7870, 7872, 7872,
            7874, 7874, 7876, 7876, 7878, 7878, 7880, 7880, 7882, 7882, 7884, 7884,
            7886, 7886, 7888, 7888, 7890, 7890, 7892, 7892, 7894, 7894, 7896, 7896,
            7898, 7898, 7900, 7900, 7902, 7902, 7904, 7904, 7906, 7906, 7908, 7908,
            7910, 7910, 7912, 7912, 7914, 7914, 7916, 7916, 7918, 7918, 7920, 7920,
            7922, 7922, 7924, 7924, 7926, 7926, 7928, 7928, 7930, 7930, 7932, 7932,
            7934, 7934, 7944, 7951, 7960, 7965, 7976, 7983, 7992, 7999, 8008, 8013,
            8025, 8025, 8027, 8027, 8029, 8029, 8031, 8031, 8040, 8047, 8120, 8123,
            8136, 8139, 8152, 8155, 8168, 8172, 8184, 8187, 8450, 8450, 8455, 8455,
            8459, 8461, 8464, 8466, 8469, 8469, 8473, 8477, 8484, 8484, 8486, 8486,
            8488, 8488, 8490, 8493, 8496, 8499, 8510, 8511, 8517, 8517, 8544, 8559,
            8579, 8579, 9398, 9423, 11264, 11311, 11360, 11360, 11362, 11364, 11367, 11367,
            11369, 11369, 11371, 11371, 11373, 11376, 11378, 11378, 11381, 11381, 11390, 11392,
            11394, 11394, 11396, 11396, 11398, 11398, 11400, 11400, 11402, 11402, 11404, 11404,
            11406, 11406, 11408, 11408, 11410, 11410, 11412, 11412, 11414, 11414, 11416, 11416,
            11418, 11418, 11420, 11420, 11422, 11422, 11424, 11424, 11426, 11426, 11428, 11428,
            11430, 11430, 11432, 11432, 11434, 11434, 11436, 11436, 11438, 11438, 11440, 11440,
            11442, 11442, 11444, 11444, 11446, 11446, 11448, 11448, 11450, 11450, 11452, 11452,
            11454, 11454, 11456, 11456, 11458, 11458, 11460, 11460, 11462, 11462, 11464, 11464,
            11466, 11466, 11468, 11468, 11470, 11470, 11472, 11472, 11474, 11474, 11476, 11476,
            11478, 11478, 11480, 11480, 11482, 11482, 11484, 11484, 11486, 11486, 11488, 11488,
            11490, 11490, 11499, 11499, 11501, 11501, 11506, 11506, 42560, 42560, 42562, 42562,
            42564, 42564, 42566, 42566, 42568, 42568, 42570, 42570, 42572, 42572, 42574, 42574,
            42576, 42576, 42578, 42578, 42580, 42580, 42582, 42582, 42584, 42584, 42586, 42586,
            42588, 42588, 42590, 42590, 42592, 42592, 42594, 42594, 42596, 42596, 42598, 42598,
            42600, 42600, 42602, 42602, 42604, 42604, 42624, 42624, 42626, 42626, 42628, 42628,
            42630, 42630, 42632, 42632, 42634, 42634, 42636, 42636, 42638, 42638, 42640, 42640,
            42642, 42642, 42644, 42644, 42646, 42646, 42648, 42648, 42650, 42650, 42786, 42786,
            42788, 42788, 42790, 42790, 42792, 42792, 42794, 42794, 42796, 42796, 42798, 42798,
            42802, 42802, 42804, 42804, 42806, 42806, 42808, 42808, 42810, 42810, 42812, 42812,
            42814, 42814, 42816, 42816, 42818, 42818, 42820, 42820, 42822, 42822, 42824, 42824,
            42826, 42826, 42828, 42828, 42830, 42830, 42832, 42832, 42834, 42834, 42836, 42836,
            42838, 42838, 42840, 42840, 42842, 42842, 42844, 42844, 42846, 42846, 42848, 42848,
            42850, 42850, 42852, 42852, 42854, 42854, 42856, 42856, 42858, 42858, 42860, 42860,
            42862, 42862, 42873, 42873, 42875, 42875, 42877, 42878, 42880, 42880, 42882, 42882,
            42884, 42884, 42886, 42886, 42891, 42891, 42893, 42893, 42896, 42896, 42898, 42898,
            42902, 42902, 42904, 42904, 42906, 42906, 42908, 42908, 42910, 42910, 42912, 42912,
            42914, 42914, 42916, 42916, 42918, 42918, 42920, 42920, 42922, 42926, 42928, 42932,
            42934, 42934, 42936, 42936, 42938, 42938, 42940, 42940, 42942, 42942, 42944, 42944,
            42946, 42946, 42948, 42951, 42953, 42953, 42955, 42956, 42960, 42960, 42966, 42966,
            42968, 42968, 42970, 42970, 42972, 42972, 42997, 42997, 65313, 65338, 66560, 66599,
            66736, 66771, 66928, 66938, 66940, 66954, 66956, 66962, 66964, 66965, 68736, 68786,
            68944, 68965, 71840, 71871, 93760, 93791, 119808, 119833, 119860, 119885, 119912, 119937,
            119964, 119964, 119966, 119967, 119970, 119970, 119973, 119974, 119977, 119980, 119982, 119989,
            120016, 120041, 120068, 120069, 120071, 120074, 120077, 120084, 120086, 120092, 120120, 120121,
            120123, 120126, 120128, 120132, 120134, 120134, 120138, 120144, 120172, 120197, 120224, 120249,
            120276, 120301, 120328, 120353, 120380, 120405, 120432, 120457, 120488, 120512, 120546, 120570,
            120604, 120628, 120662, 120686, 120720, 120744, 120778, 120778, 125184, 125217, 127280, 127305,
            127312, 127337, 127344, 127369,
        };
    }

    private static int[] lowercaseTable() {
        return new int[] {
            97, 122, 170, 170, 181, 181, 186, 186, 223, 246, 248, 255,
            257, 257, 259, 259, 261, 261, 263, 263, 265, 265, 267, 267,
            269, 269, 271, 271, 273, 273, 275, 275, 277, 277, 279, 279,
            281, 281, 283, 283, 285, 285, 287, 287, 289, 289, 291, 291,
            293, 293, 295, 295, 297, 297, 299, 299, 301, 301, 303, 303,
            305, 305, 307, 307, 309, 309, 311, 312, 314, 314, 316, 316,
            318, 318, 320, 320, 322, 322, 324, 324, 326, 326, 328, 329,
            331, 331, 333, 333, 335, 335, 337, 337, 339, 339, 341, 341,
            343, 343, 345, 345, 347, 347, 349, 349, 351, 351, 353, 353,
            355, 355, 357, 357, 359, 359, 361, 361, 363, 363, 365, 365,
            367, 367, 369, 369, 371, 371, 373, 373, 375, 375, 378, 378,
            380, 380, 382, 384, 387, 387, 389, 389, 392, 392, 396, 397,
            402, 402, 405, 405, 409, 411, 414, 414, 417, 417, 419, 419,
            421, 421, 424, 424, 426, 427, 429, 429, 432, 432, 436, 436,
            438, 438, 441, 442, 445, 447, 454, 454, 457, 457, 460, 460,
            462, 462, 464, 464, 466, 466, 468, 468, 470, 470, 472, 472,
            474, 474, 476, 477, 479, 479, 481, 481, 483, 483, 485, 485,
            487, 487, 489, 489, 491, 491, 493, 493, 495, 496, 499, 499,
            501, 501, 505, 505, 507, 507, 509, 509, 511, 511, 513, 513,
            515, 515, 517, 517, 519, 519, 521, 521, 523, 523, 525, 525,
            527, 527, 529, 529, 531, 531, 533, 533, 535, 535, 537, 537,
            539, 539, 541, 541, 543, 543, 545, 545, 547, 547, 549, 549,
            551, 551, 553, 553, 555, 555, 557, 557, 559, 559, 561, 561,
            563, 569, 572, 572, 575, 576, 578, 578, 583, 583, 585, 585,
            587, 587, 589, 589, 591, 659, 661, 696, 704, 705, 736, 740,
            837, 837, 881, 881, 883, 883, 887, 887, 890, 893, 912, 912,
            940, 974, 976, 977, 981, 983, 985, 985, 987, 987, 989, 989,
            991, 991, 993, 993, 995, 995, 997, 997, 999, 999, 1001, 1001,
            1003, 1003, 1005, 1005, 1007, 1011, 1013, 1013, 1016, 1016, 1019, 1020,
            1072, 1119, 1121, 1121, 1123, 1123, 1125, 1125, 1127, 1127, 1129, 1129,
            1131, 1131, 1133, 1133, 1135, 1135, 1137, 1137, 1139, 1139, 1141, 1141,
            1143, 1143, 1145, 1145, 1147, 1147, 1149, 1149, 1151, 1151, 1153, 1153,
            1163, 1163, 1165, 1165, 1167, 1167, 1169, 1169, 1171, 1171, 1173, 1173,
            1175, 1175, 1177, 1177, 1179, 1179, 1181, 1181, 1183, 1183, 1185, 1185,
            1187, 1187, 1189, 1189, 1191, 1191, 1193, 1193, 1195, 1195, 1197, 1197,
            1199, 1199, 1201, 1201, 1203, 1203, 1205, 1205, 1207, 1207, 1209, 1209,
            1211, 1211, 1213, 1213, 1215, 1215, 1218, 1218, 1220, 1220, 1222, 1222,
            1224, 1224, 1226, 1226, 1228, 1228, 1230, 1231, 1233, 1233, 1235, 1235,
            1237, 1237, 1239, 1239, 1241, 1241, 1243, 1243, 1245, 1245, 1247, 1247,
            1249, 1249, 1251, 1251, 1253, 1253, 1255, 1255, 1257, 1257, 1259, 1259,
            1261, 1261, 1263, 1263, 1265, 1265, 1267, 1267, 1269, 1269, 1271, 1271,
            1273, 1273, 1275, 1275, 1277, 1277, 1279, 1279, 1281, 1281, 1283, 1283,
            1285, 1285, 1287, 1287, 1289, 1289, 1291, 1291, 1293, 1293, 1295, 1295,
            1297, 1297, 1299, 1299, 1301, 1301, 1303, 1303, 1305, 1305, 1307, 1307,
            1309, 1309, 1311, 1311, 1313, 1313, 1315, 1315, 1317, 1317, 1319, 1319,
            1321, 1321, 1323, 1323, 1325, 1325, 1327, 1327, 1376, 1416, 4304, 4346,
            4348, 4351, 5112, 5117, 7296, 7304, 7306, 7306, 7424, 7615, 7681, 7681,
            7683, 7683, 7685, 7685, 7687, 7687, 7689, 7689, 7691, 7691, 7693, 7693,
            7695, 7695, 7697, 7697, 7699, 7699, 7701, 7701, 7703, 7703, 7705, 7705,
            7707, 7707, 7709, 7709, 7711, 7711, 7713, 7713, 7715, 7715, 7717, 7717,
            7719, 7719, 7721, 7721, 7723, 7723, 7725, 7725, 7727, 7727, 7729, 7729,
            7731, 7731, 7733, 7733, 7735, 7735, 7737, 7737, 7739, 7739, 7741, 7741,
            7743, 7743, 7745, 7745, 7747, 7747, 7749, 7749, 7751, 7751, 7753, 7753,
            7755, 7755, 7757, 7757, 7759, 7759, 7761, 7761, 7763, 7763, 7765, 7765,
            7767, 7767, 7769, 7769, 7771, 7771, 7773, 7773, 7775, 7775, 7777, 7777,
            7779, 7779, 7781, 7781, 7783, 7783, 7785, 7785, 7787, 7787, 7789, 7789,
            7791, 7791, 7793, 7793, 7795, 7795, 7797, 7797, 7799, 7799, 7801, 7801,
            7803, 7803, 7805, 7805, 7807, 7807, 7809, 7809, 7811, 7811, 7813, 7813,
            7815, 7815, 7817, 7817, 7819, 7819, 7821, 7821, 7823, 7823, 7825, 7825,
            7827, 7827, 7829, 7837, 7839, 7839, 7841, 7841, 7843, 7843, 7845, 7845,
            7847, 7847, 7849, 7849, 7851, 7851, 7853, 7853, 7855, 7855, 7857, 7857,
            7859, 7859, 7861, 7861, 7863, 7863, 7865, 7865, 7867, 7867, 7869, 7869,
            7871, 7871, 7873, 7873, 7875, 7875, 7877, 7877, 7879, 7879, 7881, 7881,
            7883, 7883, 7885, 7885, 7887, 7887, 7889, 7889, 7891, 7891, 7893, 7893,
            7895, 7895, 7897, 7897, 7899, 7899, 7901, 7901, 7903, 7903, 7905, 7905,
            7907, 7907, 7909, 7909, 7911, 7911, 7913, 7913, 7915, 7915, 7917, 7917,
            7919, 7919, 7921, 7921, 7923, 7923, 7925, 7925, 7927, 7927, 7929, 7929,
            7931, 7931, 7933, 7933, 7935, 7943, 7952, 7957, 7968, 7975, 7984, 7991,
            8000, 8005, 8016, 8023, 8032, 8039, 8048, 8061, 8064, 8071, 8080, 8087,
            8096, 8103, 8112, 8116, 8118, 8119, 8126, 8126, 8130, 8132, 8134, 8135,
            8144, 8147, 8150, 8151, 8160, 8167, 8178, 8180, 8182, 8183, 8305, 8305,
            8319, 8319, 8336, 8348, 8458, 8458, 8462, 8463, 8467, 8467, 8495, 8495,
            8500, 8500, 8505, 8505, 8508, 8509, 8518, 8521, 8526, 8526, 8560, 8575,
            8580, 8580, 9424, 9449, 11312, 11359, 11361, 11361, 11365, 11366, 11368, 11368,
            11370, 11370, 11372, 11372, 11377, 11377, 11379, 11380, 11382, 11389, 11393, 11393,
            11395, 11395, 11397, 11397, 11399, 11399, 11401, 11401, 11403, 11403, 11405, 11405,
            11407, 11407, 11409, 11409, 11411, 11411, 11413, 11413, 11415, 11415, 11417, 11417,
            11419, 11419, 11421, 11421, 11423, 11423, 11425, 11425, 11427, 11427, 11429, 11429,
            11431, 11431, 11433, 11433, 11435, 11435, 11437, 11437, 11439, 11439, 11441, 11441,
            11443, 11443, 11445, 11445, 11447, 11447, 11449, 11449, 11451, 11451, 11453, 11453,
            11455, 11455, 11457, 11457, 11459, 11459, 11461, 11461, 11463, 11463, 11465, 11465,
            11467, 11467, 11469, 11469, 11471, 11471, 11473, 11473, 11475, 11475, 11477, 11477,
            11479, 11479, 11481, 11481, 11483, 11483, 11485, 11485, 11487, 11487, 11489, 11489,
            11491, 11492, 11500, 11500, 11502, 11502, 11507, 11507, 11520, 11557, 11559, 11559,
            11565, 11565, 42561, 42561, 42563, 42563, 42565, 42565, 42567, 42567, 42569, 42569,
            42571, 42571, 42573, 42573, 42575, 42575, 42577, 42577, 42579, 42579, 42581, 42581,
            42583, 42583, 42585, 42585, 42587, 42587, 42589, 42589, 42591, 42591, 42593, 42593,
            42595, 42595, 42597, 42597, 42599, 42599, 42601, 42601, 42603, 42603, 42605, 42605,
            42625, 42625, 42627, 42627, 42629, 42629, 42631, 42631, 42633, 42633, 42635, 42635,
            42637, 42637, 42639, 42639, 42641, 42641, 42643, 42643, 42645, 42645, 42647, 42647,
            42649, 42649, 42651, 42653, 42787, 42787, 42789, 42789, 42791, 42791, 42793, 42793,
            42795, 42795, 42797, 42797, 42799, 42801, 42803, 42803, 42805, 42805, 42807, 42807,
            42809, 42809, 42811, 42811, 42813, 42813, 42815, 42815, 42817, 42817, 42819, 42819,
            42821, 42821, 42823, 42823, 42825, 42825, 42827, 42827, 42829, 42829, 42831, 42831,
            42833, 42833, 42835, 42835, 42837, 42837, 42839, 42839, 42841, 42841, 42843, 42843,
            42845, 42845, 42847, 42847, 42849, 42849, 42851, 42851, 42853, 42853, 42855, 42855,
            42857, 42857, 42859, 42859, 42861, 42861, 42863, 42872, 42874, 42874, 42876, 42876,
            42879, 42879, 42881, 42881, 42883, 42883, 42885, 42885, 42887, 42887, 42892, 42892,
            42894, 42894, 42897, 42897, 42899, 42901, 42903, 42903, 42905, 42905, 42907, 42907,
            42909, 42909, 42911, 42911, 42913, 42913, 42915, 42915, 42917, 42917, 42919, 42919,
            42921, 42921, 42927, 42927, 42933, 42933, 42935, 42935, 42937, 42937, 42939, 42939,
            42941, 42941, 42943, 42943, 42945, 42945, 42947, 42947, 42952, 42952, 42954, 42954,
            42957, 42957, 42961, 42961, 42963, 42963, 42965, 42965, 42967, 42967, 42969, 42969,
            42971, 42971, 42994, 42996, 42998, 42998, 43000, 43002, 43824, 43866, 43868, 43881,
            43888, 43967, 64256, 64262, 64275, 64279, 65345, 65370, 66600, 66639, 66776, 66811,
            66967, 66977, 66979, 66993, 66995, 67001, 67003, 67004, 67456, 67456, 67459, 67461,
            67463, 67504, 67506, 67514, 68800, 68850, 68976, 68997, 71872, 71903, 93792, 93823,
            119834, 119859, 119886, 119892, 119894, 119911, 119938, 119963, 119990, 119993, 119995, 119995,
            119997, 120003, 120005, 120015, 120042, 120067, 120094, 120119, 120146, 120171, 120198, 120223,
            120250, 120275, 120302, 120327, 120354, 120379, 120406, 120431, 120458, 120485, 120514, 120538,
            120540, 120545, 120572, 120596, 120598, 120603, 120630, 120654, 120656, 120661, 120688, 120712,
            120714, 120719, 120746, 120770, 120772, 120777, 120779, 120779, 122624, 122633, 122635, 122654,
            122661, 122666, 122928, 122989, 125218, 125251,
        };
    }

    /**
     * The value with its two bytes swapped.
     *
     * @param ch the value
     */
    public static char reverseBytes(char ch) {
        return (char) (((ch & 0xff00) >> 8) | (ch << 8));
    }

    /**
     * Whether {@code ch} is an ISO control character: {@code [0x00, 0x1F]} or
     * {@code [0x7F, 0x9F]}.
     *
     * <p>Two ranges and not one, and the second is the one that gets forgotten: C1 controls sit
     * where Latin-1 would otherwise put printable characters, so a byte-oriented reader that only
     * checks below 0x20 lets them through.
     *
     * @param ch the character
     */
    public static boolean isISOControl(char ch) {
        return Character.isISOControl((int) ch);
    }

    /**
     * Whether {@code codePoint} is an ISO control character.
     *
     * @param codePoint the code point
     */
    public static boolean isISOControl(int codePoint) {
        return (codePoint >= 0x0000 && codePoint <= 0x001f)
                || (codePoint >= 0x007f && codePoint <= 0x009f);
    }

    /**
     * Whether {@code ch} is a titlecase letter.
     *
     * <p>A third case, distinct from upper and lower, and it exists for the handful of digraphs
     * that are written with only their first letter capitalised: {@code Dz} is the titlecase of
     * {@code dz}, and {@code DZ} is its uppercase. Three characters, not two.
     *
     * @param ch the character
     */
    public static boolean isTitleCase(char ch) {
        return Character.isTitleCase((int) ch);
    }

    /**
     * Whether {@code codePoint} is a titlecase letter.
     *
     * <p>Read from a table of ten ranges rather than derived from the case mappings, and the
     * first version of this method did derive it -- "its own titlecase but not its own
     * uppercase" -- which sounds airtight and disagrees with the reference on 73 code points.
     * Titlecase is a CATEGORY the standard assigns, not a consequence of the mappings, and
     * thirty-one characters have it.
     *
     * @param codePoint the code point
     */
    public static boolean isTitleCase(int codePoint) {
        int[] ranges = Character.TITLECASE;
        int i = 0;
        while (i < ranges.length) {
            if (codePoint < ranges[i]) {
                return false;
            }
            if (codePoint <= ranges[i + 1]) {
                return true;
            }
            i = i + 2;
        }
        return false;
    }

    // Los treinta y un caracteres de categoria Lt, como pares [desde, hasta]. Son los digrafos
    // que se escriben con solo su primera letra en mayuscula -- `Dz` frente a `DZ` y `dz` -- mas
    // las formas griegas con iota suscrita.
    private static final int[] TITLECASE = new int[] {
        453, 453, 456, 456, 459, 459, 498, 498, 8072, 8079,
        8088, 8095, 8104, 8111, 8124, 8124, 8140, 8140, 8188, 8188,
    };

    /**
     * This character as a constant that can be written into a class file.
     *
     * <p>A dynamic constant, like {@link Short}'s and for the same reason: the pool holds an int
     * and the descriptor says to cast it.
     */
    public Optional<DynamicConstantDesc<Character>> describeConstable() {
        return Optional.of(DynamicConstantDesc.ofNamed(ConstantDescs.BSM_EXPLICIT_CAST,
                ConstantDescs.DEFAULT_NAME, ConstantDescs.CD_char,
                Integer.valueOf(this.charValue())));
    }


    // ---- lo que Unicode dice de cada code point ----
    //
    // Todo lo que sigue sale de UNA tabla: la CATEGORIA GENERAL. Unicode le asigna a cada code
    // point exactamente una de treinta categorias -- letra mayuscula, digito decimal, separador
    // de linea, sin asignar -- y casi toda pregunta sobre un caracter es una pregunta sobre a que
    // conjunto de categorias pertenece. Por eso `getType` esta primero y las demas se escriben en
    // una linea encima de el.
    //
    // Casi. Tres propiedades NO se derivan de la categoria y se comprobo una por una barriendo
    // los 0x110000 code points contra la referencia: `isAlphabetic` (Unicode marca ademas
    // Other_Alphabetic, 1495 code points mas), y los dos identificadores Unicode (Other_ID_Start
    // y Other_ID_Continue, seis y dieciocho). Esas tienen su propia tabla o su propia lista de
    // excepciones -- que es exactamente el error que `isTitleCase` cometio antes de esta tanda:
    // una derivacion que suena impecable y discrepa en 73 lugares.

    private static final int[] TYPES = Character.typeTable();
    private static final int[] DIRECTIONS = Character.directionTable();
    private static final int[] ALPHABETIC = Character.alphabeticTable();
    private static final int[] MIRRORED = Character.mirroredTable();
    private static final int[] IDEOGRAPHIC = Character.ideographicTable();
    private static final int[] EMOJI = Character.emojiTable();
    private static final int[] EMOJI_COMPONENT = Character.emojiComponentTable();
    private static final int[] EMOJI_MODIFIER = Character.emojiModifierTable();
    private static final int[] EMOJI_MODIFIER_BASE = Character.emojiModifierBaseTable();
    private static final int[] EMOJI_PRESENTATION = Character.emojiPresentationTable();
    private static final int[] PICTOGRAPHIC = Character.pictographicTable();

    // Las categorias que cuentan como "letra", como mascara de bits: preguntar por cinco
    // categorias es un corrimiento y un and, en vez de cinco comparaciones.
    private static final int LETTER_MASK = (1 << Character.UPPERCASE_LETTER)
            | (1 << Character.LOWERCASE_LETTER) | (1 << Character.TITLECASE_LETTER)
            | (1 << Character.MODIFIER_LETTER) | (1 << Character.OTHER_LETTER);

    /**
     * The general category of {@code ch}, one of the constants above.
     *
     * @param ch the character
     */
    public static int getType(char ch) {
        return Character.getType((int) ch);
    }

    /**
     * The general category of {@code codePoint}, one of the constants above.
     *
     * <p>{@link #UNASSIGNED} for a code point the standard has not given a meaning to -- which is
     * a real answer and not an error, since Unicode leaves room for what it has not encoded yet.
     *
     * @param codePoint the code point
     */
    public static int getType(int codePoint) {
        return Character.valueAt(Character.TYPES, codePoint, Character.UNASSIGNED);
    }

    /**
     * The bidirectional class of {@code ch}.
     *
     * @param ch the character
     */
    public static byte getDirectionality(char ch) {
        return Character.getDirectionality((int) ch);
    }

    /**
     * The bidirectional class of {@code codePoint}.
     *
     * <p>A separate property from the category, and the one that decides how a line of mixed
     * Hebrew and English is laid out. {@link #DIRECTIONALITY_UNDEFINED} is -1 rather than 0,
     * because 0 already means left-to-right and there has to be a value for "no answer".
     *
     * @param codePoint the code point
     */
    public static byte getDirectionality(int codePoint) {
        return (byte) Character.valueAt(Character.DIRECTIONS, codePoint,
                Character.DIRECTIONALITY_UNDEFINED);
    }

    /**
     * Whether {@code ch} has a defined meaning in Unicode.
     *
     * @param ch the character
     */
    public static boolean isDefined(char ch) {
        return Character.isDefined((int) ch);
    }

    /**
     * Whether {@code codePoint} has a defined meaning in Unicode.
     *
     * @param codePoint the code point
     */
    public static boolean isDefined(int codePoint) {
        return Character.getType(codePoint) != Character.UNASSIGNED;
    }

    /**
     * Whether {@code ch} has a mirrored form when the text runs right to left.
     *
     * @param ch the character
     */
    public static boolean isMirrored(char ch) {
        return Character.isMirrored((int) ch);
    }

    /**
     * Whether {@code codePoint} has a mirrored form when the text runs right to left.
     *
     * <p>An opening parenthesis in Arabic text is drawn as a closing one, and this is the
     * property that says so. Note it does NOT say what the mirror is -- only that there is one.
     *
     * @param codePoint the code point
     */
    public static boolean isMirrored(int codePoint) {
        return Character.inRanges(Character.MIRRORED, codePoint);
    }

    /**
     * Whether {@code codePoint} is alphabetic.
     *
     * <p>Wider than {@link #isLetter(int)}, and by a lot: the standard marks 1495 code points as
     * alphabetic that are in no letter category -- the vowel signs of the Indic scripts, mostly,
     * which are combining marks and are unquestionably part of a word.
     *
     * @param codePoint the code point
     */
    public static boolean isAlphabetic(int codePoint) {
        return Character.inRanges(Character.ALPHABETIC, codePoint);
    }

    /**
     * Whether {@code codePoint} is an ideograph -- a CJK character that stands for a word.
     *
     * @param codePoint the code point
     */
    public static boolean isIdeographic(int codePoint) {
        return Character.inRanges(Character.IDEOGRAPHIC, codePoint);
    }

    // ---- identifiers ----
    //
    // Java's rule and Unicode's rule are DIFFERENT, and both are here. Java adds the currency
    // symbols and the connectors -- which is why `$name` and `_name` compile -- and Unicode does
    // not. Getting the two confused is how a compiler ends up accepting an identifier the
    // language forbids.

    /**
     * Whether {@code ch} may start a Java identifier.
     *
     * @param ch the character
     */
    public static boolean isJavaIdentifierStart(char ch) {
        return Character.isJavaIdentifierStart((int) ch);
    }

    /**
     * Whether {@code codePoint} may start a Java identifier.
     *
     * @param codePoint the code point
     */
    public static boolean isJavaIdentifierStart(int codePoint) {
        return Character.typeIn(codePoint, Character.LETTER_MASK
                | (1 << Character.LETTER_NUMBER) | (1 << Character.CURRENCY_SYMBOL)
                | (1 << Character.CONNECTOR_PUNCTUATION));
    }

    /**
     * Whether {@code ch} may appear after the first character of a Java identifier.
     *
     * @param ch the character
     */
    public static boolean isJavaIdentifierPart(char ch) {
        return Character.isJavaIdentifierPart((int) ch);
    }

    /**
     * Whether {@code codePoint} may appear after the first character of a Java identifier.
     *
     * @param codePoint the code point
     */
    public static boolean isJavaIdentifierPart(int codePoint) {
        if (Character.typeIn(codePoint, Character.LETTER_MASK | (1 << Character.LETTER_NUMBER)
                | (1 << Character.CURRENCY_SYMBOL) | (1 << Character.CONNECTOR_PUNCTUATION)
                | (1 << Character.DECIMAL_DIGIT_NUMBER)
                | (1 << Character.COMBINING_SPACING_MARK)
                | (1 << Character.NON_SPACING_MARK))) {
            return true;
        }
        return Character.isIdentifierIgnorable(codePoint);
    }

    /**
     * Whether {@code ch} may start a Unicode identifier.
     *
     * @param ch the character
     */
    public static boolean isUnicodeIdentifierStart(char ch) {
        return Character.isUnicodeIdentifierStart((int) ch);
    }

    /**
     * Whether {@code codePoint} may start a Unicode identifier.
     *
     * @param codePoint the code point
     */
    public static boolean isUnicodeIdentifierStart(int codePoint) {
        if (Character.typeIn(codePoint,
                Character.LETTER_MASK | (1 << Character.LETTER_NUMBER))) {
            return true;
        }
        return Character.listed(Character.OTHER_ID_START, codePoint);
    }

    /**
     * Whether {@code ch} may appear after the first character of a Unicode identifier.
     *
     * @param ch the character
     */
    public static boolean isUnicodeIdentifierPart(char ch) {
        return Character.isUnicodeIdentifierPart((int) ch);
    }

    /**
     * Whether {@code codePoint} may appear after the first character of a Unicode identifier.
     *
     * @param codePoint the code point
     */
    public static boolean isUnicodeIdentifierPart(int codePoint) {
        if (Character.typeIn(codePoint, Character.LETTER_MASK | (1 << Character.LETTER_NUMBER)
                | (1 << Character.CONNECTOR_PUNCTUATION)
                | (1 << Character.DECIMAL_DIGIT_NUMBER)
                | (1 << Character.COMBINING_SPACING_MARK)
                | (1 << Character.NON_SPACING_MARK))) {
            return true;
        }
        if (Character.isIdentifierIgnorable(codePoint)) {
            return true;
        }
        return Character.listed(Character.OTHER_ID_CONTINUE, codePoint);
    }

    /**
     * Whether {@code ch} may appear in an identifier and be ignored.
     *
     * @param ch the character
     */
    public static boolean isIdentifierIgnorable(char ch) {
        return Character.isIdentifierIgnorable((int) ch);
    }

    /**
     * Whether {@code codePoint} may appear in an identifier and be ignored.
     *
     * <p>The formatting characters -- a zero-width joiner, a directional override -- plus the
     * control characters that are not whitespace. They are legal INSIDE an identifier and
     * contribute nothing to it, which is what makes two identifiers that differ only in them the
     * same identifier.
     *
     * @param codePoint the code point
     */
    public static boolean isIdentifierIgnorable(int codePoint) {
        if (codePoint >= 0x0000 && codePoint <= 0x0008) {
            return true;
        }
        if (codePoint >= 0x000e && codePoint <= 0x001b) {
            return true;
        }
        if (codePoint >= 0x007f && codePoint <= 0x009f) {
            return true;
        }
        return Character.getType(codePoint) == Character.FORMAT;
    }

    /**
     * Whether {@code ch} may start a Java identifier.
     *
     * @param ch the character
     * @deprecated renamed to {@link #isJavaIdentifierStart(char)}, which says what it means.
     */
    @Deprecated(since = "1.1")
    public static boolean isJavaLetter(char ch) {
        return Character.isJavaIdentifierStart(ch);
    }

    /**
     * Whether {@code ch} may appear after the first character of a Java identifier.
     *
     * @param ch the character
     * @deprecated renamed to {@link #isJavaIdentifierPart(char)}.
     */
    @Deprecated(since = "1.1")
    public static boolean isJavaLetterOrDigit(char ch) {
        return Character.isJavaIdentifierPart(ch);
    }

    /**
     * Whether {@code ch} is one of the five ASCII space characters.
     *
     * @param ch the character
     * @deprecated replaced by {@link #isWhitespace(char)}, which knows about the rest of Unicode.
     *         This one predates it and answers only for ASCII, which is why it survives: changing
     *         it would change the meaning of code written before Unicode.
     */
    @Deprecated(since = "1.1")
    public static boolean isSpace(char ch) {
        return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\f' || ch == '\r';
    }

    // ---- emoji ----
    //
    // Six properties and not one, because "emoji" is not a single question. A digit is
    // Emoji_Component (it can be part of a keycap sequence) without being an emoji anybody would
    // call one; a skin tone modifier is an emoji that never stands alone; and
    // Extended_Pictographic is the widest of them, drawn to make text segmentation work rather
    // than to describe what looks like an emoji.

    /**
     * Whether {@code codePoint} is an emoji.
     *
     * @param codePoint the code point
     */
    public static boolean isEmoji(int codePoint) {
        return Character.inRanges(Character.EMOJI, codePoint);
    }

    /**
     * Whether {@code codePoint} may appear as part of an emoji sequence.
     *
     * @param codePoint the code point
     */
    public static boolean isEmojiComponent(int codePoint) {
        return Character.inRanges(Character.EMOJI_COMPONENT, codePoint);
    }

    /**
     * Whether {@code codePoint} is an emoji modifier -- one of the five skin tones.
     *
     * @param codePoint the code point
     */
    public static boolean isEmojiModifier(int codePoint) {
        return Character.inRanges(Character.EMOJI_MODIFIER, codePoint);
    }

    /**
     * Whether {@code codePoint} is an emoji a modifier can be applied to.
     *
     * @param codePoint the code point
     */
    public static boolean isEmojiModifierBase(int codePoint) {
        return Character.inRanges(Character.EMOJI_MODIFIER_BASE, codePoint);
    }

    /**
     * Whether {@code codePoint} is drawn as an emoji by default rather than as text.
     *
     * @param codePoint the code point
     */
    public static boolean isEmojiPresentation(int codePoint) {
        return Character.inRanges(Character.EMOJI_PRESENTATION, codePoint);
    }

    /**
     * Whether {@code codePoint} is an extended pictographic character.
     *
     * @param codePoint the code point
     */
    public static boolean isExtendedPictographic(int codePoint) {
        return Character.inRanges(Character.PICTOGRAPHIC, codePoint);
    }

    // ---- how the tables are read ----

    // Whether `codePoint` falls in any of the categories `mask` names. One shift and one `and`
    // instead of a chain of comparisons -- which is the only reason the categories fit in a byte
    // and the mask in an int.
    private static boolean typeIn(int codePoint, int mask) {
        return ((mask >> Character.getType(codePoint)) & 1) != 0;
    }

    // The value a [start, value] table gives `cp`. A range runs to the next start minus one,
    // which is what lets a range cost two ints instead of three -- and it is why the table has to
    // cover the whole space, gaps included: a gap IS an entry, holding UNASSIGNED.
    private static int valueAt(int[] table, int cp, int fallback) {
        int lo = 0;
        int hi = table.length / 2 - 1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            if (cp < table[mid * 2]) {
                hi = mid - 1;
            } else if (mid * 2 + 2 < table.length && cp >= table[mid * 2 + 2]) {
                lo = mid + 1;
            } else {
                return table[mid * 2 + 1];
            }
        }
        return fallback;
    }

    // Whether `cp` falls inside a [start, end] table. Here the ranges are disjoint and do NOT
    // cover the space, so a miss is a real answer.
    private static boolean inRanges(int[] table, int cp) {
        int lo = 0;
        int hi = table.length / 2 - 1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            if (cp < table[mid * 2]) {
                hi = mid - 1;
            } else if (cp > table[mid * 2 + 1]) {
                lo = mid + 1;
            } else {
                return true;
            }
        }
        return false;
    }

    // Whether `cp` is in a short, unsorted exception list. Linear on purpose: six and eighteen
    // entries, and a binary search over them would cost more to read than to run.
    private static boolean listed(int[] list, int cp) {
        int i = 0;
        while (i < list.length) {
            if (list[i] == cp) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    // La categoria general de cada code point, como pares [desde, categoria].
    // 8198 enteros, 4099 rangos.
    private static int[] typeTable() {
        int[] part0 = Character.typeTable0();
        int[] part1 = Character.typeTable1();
        int[] part2 = Character.typeTable2();
        int[] out = new int[part0.length + part1.length + part2.length];
        int at = 0;
        System.arraycopy(part0, 0, out, at, part0.length);
        at = at + part0.length;
        System.arraycopy(part1, 0, out, at, part1.length);
        at = at + part1.length;
        System.arraycopy(part2, 0, out, at, part2.length);
        return out;
    }

    private static int[] typeTable0() {
        return new int[] {
            0, 15, 32, 12, 33, 24, 36, 26, 37, 24, 40, 21, 
            41, 22, 42, 24, 43, 25, 44, 24, 45, 20, 46, 24, 
            48, 9, 58, 24, 60, 25, 63, 24, 65, 1, 91, 21, 
            92, 24, 93, 22, 94, 27, 95, 23, 96, 27, 97, 2, 
            123, 21, 124, 25, 125, 22, 126, 25, 127, 15, 160, 12, 
            161, 24, 162, 26, 166, 28, 167, 24, 168, 27, 169, 28, 
            170, 5, 171, 29, 172, 25, 173, 16, 174, 28, 175, 27, 
            176, 28, 177, 25, 178, 11, 180, 27, 181, 2, 182, 24, 
            184, 27, 185, 11, 186, 5, 187, 30, 188, 11, 191, 24, 
            192, 1, 215, 25, 216, 1, 223, 2, 247, 25, 248, 2, 
            256, 1, 257, 2, 258, 1, 259, 2, 260, 1, 261, 2, 
            262, 1, 263, 2, 264, 1, 265, 2, 266, 1, 267, 2, 
            268, 1, 269, 2, 270, 1, 271, 2, 272, 1, 273, 2, 
            274, 1, 275, 2, 276, 1, 277, 2, 278, 1, 279, 2, 
            280, 1, 281, 2, 282, 1, 283, 2, 284, 1, 285, 2, 
            286, 1, 287, 2, 288, 1, 289, 2, 290, 1, 291, 2, 
            292, 1, 293, 2, 294, 1, 295, 2, 296, 1, 297, 2, 
            298, 1, 299, 2, 300, 1, 301, 2, 302, 1, 303, 2, 
            304, 1, 305, 2, 306, 1, 307, 2, 308, 1, 309, 2, 
            310, 1, 311, 2, 313, 1, 314, 2, 315, 1, 316, 2, 
            317, 1, 318, 2, 319, 1, 320, 2, 321, 1, 322, 2, 
            323, 1, 324, 2, 325, 1, 326, 2, 327, 1, 328, 2, 
            330, 1, 331, 2, 332, 1, 333, 2, 334, 1, 335, 2, 
            336, 1, 337, 2, 338, 1, 339, 2, 340, 1, 341, 2, 
            342, 1, 343, 2, 344, 1, 345, 2, 346, 1, 347, 2, 
            348, 1, 349, 2, 350, 1, 351, 2, 352, 1, 353, 2, 
            354, 1, 355, 2, 356, 1, 357, 2, 358, 1, 359, 2, 
            360, 1, 361, 2, 362, 1, 363, 2, 364, 1, 365, 2, 
            366, 1, 367, 2, 368, 1, 369, 2, 370, 1, 371, 2, 
            372, 1, 373, 2, 374, 1, 375, 2, 376, 1, 378, 2, 
            379, 1, 380, 2, 381, 1, 382, 2, 385, 1, 387, 2, 
            388, 1, 389, 2, 390, 1, 392, 2, 393, 1, 396, 2, 
            398, 1, 402, 2, 403, 1, 405, 2, 406, 1, 409, 2, 
            412, 1, 414, 2, 415, 1, 417, 2, 418, 1, 419, 2, 
            420, 1, 421, 2, 422, 1, 424, 2, 425, 1, 426, 2, 
            428, 1, 429, 2, 430, 1, 432, 2, 433, 1, 436, 2, 
            437, 1, 438, 2, 439, 1, 441, 2, 443, 5, 444, 1, 
            445, 2, 448, 5, 452, 1, 453, 3, 454, 2, 455, 1, 
            456, 3, 457, 2, 458, 1, 459, 3, 460, 2, 461, 1, 
            462, 2, 463, 1, 464, 2, 465, 1, 466, 2, 467, 1, 
            468, 2, 469, 1, 470, 2, 471, 1, 472, 2, 473, 1, 
            474, 2, 475, 1, 476, 2, 478, 1, 479, 2, 480, 1, 
            481, 2, 482, 1, 483, 2, 484, 1, 485, 2, 486, 1, 
            487, 2, 488, 1, 489, 2, 490, 1, 491, 2, 492, 1, 
            493, 2, 494, 1, 495, 2, 497, 1, 498, 3, 499, 2, 
            500, 1, 501, 2, 502, 1, 505, 2, 506, 1, 507, 2, 
            508, 1, 509, 2, 510, 1, 511, 2, 512, 1, 513, 2, 
            514, 1, 515, 2, 516, 1, 517, 2, 518, 1, 519, 2, 
            520, 1, 521, 2, 522, 1, 523, 2, 524, 1, 525, 2, 
            526, 1, 527, 2, 528, 1, 529, 2, 530, 1, 531, 2, 
            532, 1, 533, 2, 534, 1, 535, 2, 536, 1, 537, 2, 
            538, 1, 539, 2, 540, 1, 541, 2, 542, 1, 543, 2, 
            544, 1, 545, 2, 546, 1, 547, 2, 548, 1, 549, 2, 
            550, 1, 551, 2, 552, 1, 553, 2, 554, 1, 555, 2, 
            556, 1, 557, 2, 558, 1, 559, 2, 560, 1, 561, 2, 
            562, 1, 563, 2, 570, 1, 572, 2, 573, 1, 575, 2, 
            577, 1, 578, 2, 579, 1, 583, 2, 584, 1, 585, 2, 
            586, 1, 587, 2, 588, 1, 589, 2, 590, 1, 591, 2, 
            660, 5, 661, 2, 688, 4, 706, 27, 710, 4, 722, 27, 
            736, 4, 741, 27, 748, 4, 749, 27, 750, 4, 751, 27, 
            768, 6, 880, 1, 881, 2, 882, 1, 883, 2, 884, 4, 
            885, 27, 886, 1, 887, 2, 888, 0, 890, 4, 891, 2, 
            894, 24, 895, 1, 896, 0, 900, 27, 902, 1, 903, 24, 
            904, 1, 907, 0, 908, 1, 909, 0, 910, 1, 912, 2, 
            913, 1, 930, 0, 931, 1, 940, 2, 975, 1, 976, 2, 
            978, 1, 981, 2, 984, 1, 985, 2, 986, 1, 987, 2, 
            988, 1, 989, 2, 990, 1, 991, 2, 992, 1, 993, 2, 
            994, 1, 995, 2, 996, 1, 997, 2, 998, 1, 999, 2, 
            1000, 1, 1001, 2, 1002, 1, 1003, 2, 1004, 1, 1005, 2, 
            1006, 1, 1007, 2, 1012, 1, 1013, 2, 1014, 25, 1015, 1, 
            1016, 2, 1017, 1, 1019, 2, 1021, 1, 1072, 2, 1120, 1, 
            1121, 2, 1122, 1, 1123, 2, 1124, 1, 1125, 2, 1126, 1, 
            1127, 2, 1128, 1, 1129, 2, 1130, 1, 1131, 2, 1132, 1, 
            1133, 2, 1134, 1, 1135, 2, 1136, 1, 1137, 2, 1138, 1, 
            1139, 2, 1140, 1, 1141, 2, 1142, 1, 1143, 2, 1144, 1, 
            1145, 2, 1146, 1, 1147, 2, 1148, 1, 1149, 2, 1150, 1, 
            1151, 2, 1152, 1, 1153, 2, 1154, 28, 1155, 6, 1160, 7, 
            1162, 1, 1163, 2, 1164, 1, 1165, 2, 1166, 1, 1167, 2, 
            1168, 1, 1169, 2, 1170, 1, 1171, 2, 1172, 1, 1173, 2, 
            1174, 1, 1175, 2, 1176, 1, 1177, 2, 1178, 1, 1179, 2, 
            1180, 1, 1181, 2, 1182, 1, 1183, 2, 1184, 1, 1185, 2, 
            1186, 1, 1187, 2, 1188, 1, 1189, 2, 1190, 1, 1191, 2, 
            1192, 1, 1193, 2, 1194, 1, 1195, 2, 1196, 1, 1197, 2, 
            1198, 1, 1199, 2, 1200, 1, 1201, 2, 1202, 1, 1203, 2, 
            1204, 1, 1205, 2, 1206, 1, 1207, 2, 1208, 1, 1209, 2, 
            1210, 1, 1211, 2, 1212, 1, 1213, 2, 1214, 1, 1215, 2, 
            1216, 1, 1218, 2, 1219, 1, 1220, 2, 1221, 1, 1222, 2, 
            1223, 1, 1224, 2, 1225, 1, 1226, 2, 1227, 1, 1228, 2, 
            1229, 1, 1230, 2, 1232, 1, 1233, 2, 1234, 1, 1235, 2, 
            1236, 1, 1237, 2, 1238, 1, 1239, 2, 1240, 1, 1241, 2, 
            1242, 1, 1243, 2, 1244, 1, 1245, 2, 1246, 1, 1247, 2, 
            1248, 1, 1249, 2, 1250, 1, 1251, 2, 1252, 1, 1253, 2, 
            1254, 1, 1255, 2, 1256, 1, 1257, 2, 1258, 1, 1259, 2, 
            1260, 1, 1261, 2, 1262, 1, 1263, 2, 1264, 1, 1265, 2, 
            1266, 1, 1267, 2, 1268, 1, 1269, 2, 1270, 1, 1271, 2, 
            1272, 1, 1273, 2, 1274, 1, 1275, 2, 1276, 1, 1277, 2, 
            1278, 1, 1279, 2, 1280, 1, 1281, 2, 1282, 1, 1283, 2, 
            1284, 1, 1285, 2, 1286, 1, 1287, 2, 1288, 1, 1289, 2, 
            1290, 1, 1291, 2, 1292, 1, 1293, 2, 1294, 1, 1295, 2, 
            1296, 1, 1297, 2, 1298, 1, 1299, 2, 1300, 1, 1301, 2, 
            1302, 1, 1303, 2, 1304, 1, 1305, 2, 1306, 1, 1307, 2, 
            1308, 1, 1309, 2, 1310, 1, 1311, 2, 1312, 1, 1313, 2, 
            1314, 1, 1315, 2, 1316, 1, 1317, 2, 1318, 1, 1319, 2, 
            1320, 1, 1321, 2, 1322, 1, 1323, 2, 1324, 1, 1325, 2, 
            1326, 1, 1327, 2, 1328, 0, 1329, 1, 1367, 0, 1369, 4, 
            1370, 24, 1376, 2, 1417, 24, 1418, 20, 1419, 0, 1421, 28, 
            1423, 26, 1424, 0, 1425, 6, 1470, 20, 1471, 6, 1472, 24, 
            1473, 6, 1475, 24, 1476, 6, 1478, 24, 1479, 6, 1480, 0, 
            1488, 5, 1515, 0, 1519, 5, 1523, 24, 1525, 0, 1536, 16, 
            1542, 25, 1545, 24, 1547, 26, 1548, 24, 1550, 28, 1552, 6, 
            1563, 24, 1564, 16, 1565, 24, 1568, 5, 1600, 4, 1601, 5, 
            1611, 6, 1632, 9, 1642, 24, 1646, 5, 1648, 6, 1649, 5, 
            1748, 24, 1749, 5, 1750, 6, 1757, 16, 1758, 28, 1759, 6, 
            1765, 4, 1767, 6, 1769, 28, 1770, 6, 1774, 5, 1776, 9, 
            1786, 5, 1789, 28, 1791, 5, 1792, 24, 1806, 0, 1807, 16, 
            1808, 5, 1809, 6, 1810, 5, 1840, 6, 1867, 0, 1869, 5, 
            1958, 6, 1969, 5, 1970, 0, 1984, 9, 1994, 5, 2027, 6, 
            2036, 4, 2038, 28, 2039, 24, 2042, 4, 2043, 0, 2045, 6, 
            2046, 26, 2048, 5, 2070, 6, 2074, 4, 2075, 6, 2084, 4, 
            2085, 6, 2088, 4, 2089, 6, 2094, 0, 2096, 24, 2111, 0, 
            2112, 5, 2137, 6, 2140, 0, 2142, 24, 2143, 0, 2144, 5, 
            2155, 0, 2160, 5, 2184, 27, 2185, 5, 2191, 0, 2192, 16, 
            2194, 0, 2199, 6, 2208, 5, 2249, 4, 2250, 6, 2274, 16, 
            2275, 6, 2307, 8, 2308, 5, 2362, 6, 2363, 8, 2364, 6, 
            2365, 5, 2366, 8, 2369, 6, 2377, 8, 2381, 6, 2382, 8, 
            2384, 5, 2385, 6, 2392, 5, 2402, 6, 2404, 24, 2406, 9, 
            2416, 24, 2417, 4, 2418, 5, 2433, 6, 2434, 8, 2436, 0, 
            2437, 5, 2445, 0, 2447, 5, 2449, 0, 2451, 5, 2473, 0, 
            2474, 5, 2481, 0, 2482, 5, 2483, 0, 2486, 5, 2490, 0, 
            2492, 6, 2493, 5, 2494, 8, 2497, 6, 2501, 0, 2503, 8, 
            2505, 0, 2507, 8, 2509, 6, 2510, 5, 2511, 0, 2519, 8, 
            2520, 0, 2524, 5, 2526, 0, 2527, 5, 2530, 6, 2532, 0, 
            2534, 9, 2544, 5, 2546, 26, 2548, 11, 2554, 28, 2555, 26, 
            2556, 5, 2557, 24, 2558, 6, 2559, 0, 2561, 6, 2563, 8, 
            2564, 0, 2565, 5, 2571, 0, 2575, 5, 2577, 0, 2579, 5, 
            2601, 0, 2602, 5, 2609, 0, 2610, 5, 2612, 0, 2613, 5, 
            2615, 0, 2616, 5, 2618, 0, 2620, 6, 2621, 0, 2622, 8, 
            2625, 6, 2627, 0, 2631, 6, 2633, 0, 2635, 6, 2638, 0, 
            2641, 6, 2642, 0, 2649, 5, 2653, 0, 2654, 5, 2655, 0, 
            2662, 9, 2672, 6, 2674, 5, 2677, 6, 2678, 24, 2679, 0, 
            2689, 6, 2691, 8, 2692, 0, 2693, 5, 2702, 0, 2703, 5, 
            2706, 0, 2707, 5, 2729, 0, 2730, 5, 2737, 0, 2738, 5, 
            2740, 0, 2741, 5, 2746, 0, 2748, 6, 2749, 5, 2750, 8, 
            2753, 6, 2758, 0, 2759, 6, 2761, 8, 2762, 0, 2763, 8, 
            2765, 6, 2766, 0, 2768, 5, 2769, 0, 2784, 5, 2786, 6, 
            2788, 0, 2790, 9, 2800, 24, 2801, 26, 2802, 0, 2809, 5, 
            2810, 6, 2816, 0, 2817, 6, 2818, 8, 2820, 0, 2821, 5, 
            2829, 0, 2831, 5, 2833, 0, 2835, 5, 2857, 0, 2858, 5, 
            2865, 0, 2866, 5, 2868, 0, 2869, 5, 2874, 0, 2876, 6, 
            2877, 5, 2878, 8, 2879, 6, 2880, 8, 2881, 6, 2885, 0, 
            2887, 8, 2889, 0, 2891, 8, 2893, 6, 2894, 0, 2901, 6, 
            2903, 8, 2904, 0, 2908, 5, 2910, 0, 2911, 5, 2914, 6, 
            2916, 0, 2918, 9, 2928, 28, 2929, 5, 2930, 11, 2936, 0, 
            2946, 6, 2947, 5, 2948, 0, 2949, 5, 2955, 0, 2958, 5, 
            2961, 0, 2962, 5, 2966, 0, 2969, 5, 2971, 0, 2972, 5, 
            2973, 0, 2974, 5, 2976, 0, 2979, 5, 2981, 0, 2984, 5, 
            2987, 0, 2990, 5, 3002, 0, 3006, 8, 3008, 6, 3009, 8, 
            3011, 0, 3014, 8, 3017, 0, 3018, 8, 3021, 6, 3022, 0, 
            3024, 5, 3025, 0, 3031, 8, 3032, 0, 3046, 9, 3056, 11, 
            3059, 28, 3065, 26, 3066, 28, 3067, 0, 3072, 6, 3073, 8, 
            3076, 6, 3077, 5, 3085, 0, 3086, 5, 3089, 0, 3090, 5, 
            3113, 0, 3114, 5, 3130, 0, 3132, 6, 3133, 5, 3134, 6, 
            3137, 8, 3141, 0, 3142, 6, 3145, 0, 3146, 6, 3150, 0, 
            3157, 6, 3159, 0, 3160, 5, 3163, 0, 3165, 5, 3166, 0, 
            3168, 5, 3170, 6, 3172, 0, 3174, 9, 3184, 0, 3191, 24, 
            3192, 11, 3199, 28, 3200, 5, 3201, 6, 3202, 8, 3204, 24, 
            3205, 5, 3213, 0, 3214, 5, 3217, 0, 3218, 5, 3241, 0, 
            3242, 5, 3252, 0, 3253, 5, 3258, 0, 3260, 6, 3261, 5, 
            3262, 8, 3263, 6, 3264, 8, 3269, 0, 3270, 6, 3271, 8, 
            3273, 0, 3274, 8, 3276, 6, 3278, 0, 3285, 8, 3287, 0, 
            3293, 5, 3295, 0, 3296, 5, 3298, 6, 3300, 0, 3302, 9, 
            3312, 0, 3313, 5, 3315, 8, 3316, 0, 3328, 6, 3330, 8, 
            3332, 5, 3341, 0, 3342, 5, 3345, 0, 3346, 5, 3387, 6, 
            3389, 5, 3390, 8, 3393, 6, 3397, 0, 3398, 8, 3401, 0, 
            3402, 8, 3405, 6, 3406, 5, 3407, 28, 3408, 0, 3412, 5, 
            3415, 8, 3416, 11, 3423, 5, 3426, 6, 3428, 0, 3430, 9, 
            3440, 11, 3449, 28, 3450, 5, 3456, 0, 3457, 6, 3458, 8, 
            3460, 0, 3461, 5, 3479, 0, 3482, 5, 3506, 0, 3507, 5, 
            3516, 0, 3517, 5, 3518, 0, 3520, 5, 3527, 0, 3530, 6, 
            3531, 0, 3535, 8, 3538, 6, 3541, 0, 3542, 6, 3543, 0, 
            3544, 8, 3552, 0, 3558, 9, 3568, 0, 3570, 8, 3572, 24, 
            3573, 0, 3585, 5, 3633, 6, 3634, 5, 3636, 6, 3643, 0, 
            3647, 26, 3648, 5, 3654, 4, 3655, 6, 3663, 24, 3664, 9, 
            3674, 24, 3676, 0, 3713, 5, 3715, 0, 3716, 5, 3717, 0, 
            3718, 5, 3723, 0, 3724, 5, 3748, 0, 3749, 5, 3750, 0, 
            3751, 5, 3761, 6, 3762, 5, 3764, 6, 3773, 5, 3774, 0, 
            3776, 5, 3781, 0, 3782, 4, 3783, 0, 3784, 6, 3791, 0, 
            3792, 9, 3802, 0, 3804, 5, 3808, 0, 3840, 5, 3841, 28, 
            3844, 24, 3859, 28, 3860, 24, 3861, 28, 3864, 6, 3866, 28, 
            3872, 9, 3882, 11, 3892, 28, 3893, 6, 3894, 28, 3895, 6, 
            3896, 28, 3897, 6, 3898, 21, 3899, 22, 3900, 21, 3901, 22, 
            3902, 8, 3904, 5, 3912, 0, 3913, 5, 3949, 0, 3953, 6, 
            3967, 8, 3968, 6, 3973, 24, 3974, 6, 3976, 5, 3981, 6, 
            3992, 0, 3993, 6, 4029, 0, 4030, 28, 4038, 6, 4039, 28, 
            4045, 0, 4046, 28, 4048, 24, 4053, 28, 4057, 24, 4059, 0, 
            4096, 5, 4139, 8, 4141, 6, 4145, 8, 4146, 6, 4152, 8, 
            4153, 6, 4155, 8, 4157, 6, 4159, 5, 4160, 9, 4170, 24, 
            4176, 5, 4182, 8, 4184, 6, 4186, 5, 4190, 6, 4193, 5, 
            4194, 8, 4197, 5, 4199, 8, 4206, 5, 4209, 6, 4213, 5, 
            4226, 6, 4227, 8, 4229, 6, 4231, 8, 4237, 6, 4238, 5, 
            4239, 8, 4240, 9, 4250, 8, 4253, 6, 4254, 28, 4256, 1, 
            4294, 0, 4295, 1, 4296, 0, 4301, 1, 4302, 0, 4304, 2, 
            4347, 24, 4348, 4, 4349, 2, 4352, 5, 4681, 0, 4682, 5, 
            4686, 0, 4688, 5, 4695, 0, 4696, 5, 4697, 0, 4698, 5, 
            4702, 0, 4704, 5, 4745, 0, 4746, 5, 4750, 0, 4752, 5, 
            4785, 0, 4786, 5, 4790, 0, 4792, 5, 4799, 0, 4800, 5, 
            4801, 0, 4802, 5, 4806, 0, 4808, 5, 4823, 0, 4824, 5, 
            4881, 0, 4882, 5, 4886, 0, 4888, 5, 4955, 0, 4957, 6, 
            4960, 24, 4969, 11, 4989, 0, 4992, 5, 5008, 28, 5018, 0, 
            5024, 1, 5110, 0, 5112, 2, 5118, 0, 5120, 20, 5121, 5, 
            5741, 28, 5742, 24, 5743, 5, 5760, 12, 5761, 5, 5787, 21, 
            5788, 22, 5789, 0, 5792, 5, 5867, 24, 5870, 10, 5873, 5, 
            5881, 0, 5888, 5, 5906, 6, 5909, 8, 5910, 0, 5919, 5, 
            5938, 6, 5940, 8, 5941, 24, 5943, 0, 5952, 5, 5970, 6, 
            5972, 0, 5984, 5, 5997, 0, 5998, 5, 6001, 0, 6002, 6, 
            6004, 0, 6016, 5, 6068, 6, 6070, 8, 6071, 6, 6078, 8, 
            6086, 6, 6087, 8, 6089, 6, 6100, 24, 6103, 4, 6104, 24, 
            6107, 26, 6108, 5, 6109, 6, 6110, 0, 6112, 9, 6122, 0, 
            6128, 11, 6138, 0, 6144, 24, 6150, 20, 6151, 24, 6155, 6, 
            6158, 16, 6159, 6, 6160, 9, 6170, 0, 6176, 5, 6211, 4, 
            6212, 5, 6265, 0, 6272, 5, 6277, 6, 6279, 5, 6313, 6, 
            6314, 5, 6315, 0, 6320, 5, 6390, 0, 6400, 5, 6431, 0, 
            6432, 6, 6435, 8, 6439, 6, 6441, 8, 6444, 0, 6448, 8, 
            6450, 6, 6451, 8, 6457, 6, 6460, 0, 6464, 28, 6465, 0, 
            6468, 24, 6470, 9, 6480, 5, 6510, 0, 6512, 5, 6517, 0, 
            6528, 5, 6572, 0, 6576, 5, 6602, 0, 6608, 9, 6618, 11, 
            6619, 0, 6622, 28, 6656, 5, 6679, 6, 6681, 8, 6683, 6, 
            6684, 0, 6686, 24, 6688, 5, 6741, 8, 6742, 6, 6743, 8, 
            6744, 6, 6751, 0, 6752, 6, 6753, 8, 6754, 6, 6755, 8, 
            6757, 6, 6765, 8, 6771, 6, 6781, 0, 6783, 6, 6784, 9, 
            6794, 0, 6800, 9, 6810, 0, 6816, 24, 6823, 4, 6824, 24, 
            6830, 0, 6832, 6, 6846, 7, 6847, 6, 6863, 0, 6912, 6, 
            6916, 8, 6917, 5, 6964, 6, 6965, 8, 6966, 6, 6971, 8, 
            6972, 6, 6973, 8, 6978, 6, 6979, 8, 6981, 5, 6989, 0, 
            6990, 24, 6992, 9, 7002, 24, 7009, 28, 7019, 6, 7028, 28, 
            7037, 24, 7040, 6, 7042, 8, 7043, 5, 7073, 8, 7074, 6, 
            7078, 8, 7080, 6, 7082, 8, 7083, 6, 7086, 5, 7088, 9, 
            7098, 5, 7142, 6, 7143, 8, 7144, 6, 7146, 8, 7149, 6, 
            7150, 8, 7151, 6, 7154, 8, 7156, 0, 7164, 24, 7168, 5, 
            7204, 8, 7212, 6, 7220, 8, 7222, 6, 7224, 0, 7227, 24, 
            7232, 9, 7242, 0, 7245, 5, 7248, 9, 7258, 5, 7288, 4, 
            7294, 24, 7296, 2, 7305, 1, 7306, 2, 7307, 0, 7312, 1, 
            7355, 0, 7357, 1, 7360, 24, 7368, 0, 7376, 6, 7379, 24, 
            7380, 6, 7393, 8, 7394, 6, 7401, 5, 7405, 6, 7406, 5, 
            7412, 6, 7413, 5, 7415, 8, 7416, 6, 7418, 5, 7419, 0, 
            7424, 2, 7468, 4, 7531, 2, 7544, 4, 7545, 2, 7579, 4, 
            7616, 6, 7680, 1, 7681, 2, 7682, 1, 7683, 2, 7684, 1, 
            7685, 2, 7686, 1, 7687, 2, 7688, 1, 7689, 2, 7690, 1, 
            7691, 2, 7692, 1, 7693, 2, 7694, 1, 7695, 2, 7696, 1, 
            7697, 2, 7698, 1, 7699, 2, 7700, 1, 7701, 2, 7702, 1, 
        };
    }

    private static int[] typeTable1() {
        return new int[] {
            7703, 2, 7704, 1, 7705, 2, 7706, 1, 7707, 2, 7708, 1, 
            7709, 2, 7710, 1, 7711, 2, 7712, 1, 7713, 2, 7714, 1, 
            7715, 2, 7716, 1, 7717, 2, 7718, 1, 7719, 2, 7720, 1, 
            7721, 2, 7722, 1, 7723, 2, 7724, 1, 7725, 2, 7726, 1, 
            7727, 2, 7728, 1, 7729, 2, 7730, 1, 7731, 2, 7732, 1, 
            7733, 2, 7734, 1, 7735, 2, 7736, 1, 7737, 2, 7738, 1, 
            7739, 2, 7740, 1, 7741, 2, 7742, 1, 7743, 2, 7744, 1, 
            7745, 2, 7746, 1, 7747, 2, 7748, 1, 7749, 2, 7750, 1, 
            7751, 2, 7752, 1, 7753, 2, 7754, 1, 7755, 2, 7756, 1, 
            7757, 2, 7758, 1, 7759, 2, 7760, 1, 7761, 2, 7762, 1, 
            7763, 2, 7764, 1, 7765, 2, 7766, 1, 7767, 2, 7768, 1, 
            7769, 2, 7770, 1, 7771, 2, 7772, 1, 7773, 2, 7774, 1, 
            7775, 2, 7776, 1, 7777, 2, 7778, 1, 7779, 2, 7780, 1, 
            7781, 2, 7782, 1, 7783, 2, 7784, 1, 7785, 2, 7786, 1, 
            7787, 2, 7788, 1, 7789, 2, 7790, 1, 7791, 2, 7792, 1, 
            7793, 2, 7794, 1, 7795, 2, 7796, 1, 7797, 2, 7798, 1, 
            7799, 2, 7800, 1, 7801, 2, 7802, 1, 7803, 2, 7804, 1, 
            7805, 2, 7806, 1, 7807, 2, 7808, 1, 7809, 2, 7810, 1, 
            7811, 2, 7812, 1, 7813, 2, 7814, 1, 7815, 2, 7816, 1, 
            7817, 2, 7818, 1, 7819, 2, 7820, 1, 7821, 2, 7822, 1, 
            7823, 2, 7824, 1, 7825, 2, 7826, 1, 7827, 2, 7828, 1, 
            7829, 2, 7838, 1, 7839, 2, 7840, 1, 7841, 2, 7842, 1, 
            7843, 2, 7844, 1, 7845, 2, 7846, 1, 7847, 2, 7848, 1, 
            7849, 2, 7850, 1, 7851, 2, 7852, 1, 7853, 2, 7854, 1, 
            7855, 2, 7856, 1, 7857, 2, 7858, 1, 7859, 2, 7860, 1, 
            7861, 2, 7862, 1, 7863, 2, 7864, 1, 7865, 2, 7866, 1, 
            7867, 2, 7868, 1, 7869, 2, 7870, 1, 7871, 2, 7872, 1, 
            7873, 2, 7874, 1, 7875, 2, 7876, 1, 7877, 2, 7878, 1, 
            7879, 2, 7880, 1, 7881, 2, 7882, 1, 7883, 2, 7884, 1, 
            7885, 2, 7886, 1, 7887, 2, 7888, 1, 7889, 2, 7890, 1, 
            7891, 2, 7892, 1, 7893, 2, 7894, 1, 7895, 2, 7896, 1, 
            7897, 2, 7898, 1, 7899, 2, 7900, 1, 7901, 2, 7902, 1, 
            7903, 2, 7904, 1, 7905, 2, 7906, 1, 7907, 2, 7908, 1, 
            7909, 2, 7910, 1, 7911, 2, 7912, 1, 7913, 2, 7914, 1, 
            7915, 2, 7916, 1, 7917, 2, 7918, 1, 7919, 2, 7920, 1, 
            7921, 2, 7922, 1, 7923, 2, 7924, 1, 7925, 2, 7926, 1, 
            7927, 2, 7928, 1, 7929, 2, 7930, 1, 7931, 2, 7932, 1, 
            7933, 2, 7934, 1, 7935, 2, 7944, 1, 7952, 2, 7958, 0, 
            7960, 1, 7966, 0, 7968, 2, 7976, 1, 7984, 2, 7992, 1, 
            8000, 2, 8006, 0, 8008, 1, 8014, 0, 8016, 2, 8024, 0, 
            8025, 1, 8026, 0, 8027, 1, 8028, 0, 8029, 1, 8030, 0, 
            8031, 1, 8032, 2, 8040, 1, 8048, 2, 8062, 0, 8064, 2, 
            8072, 3, 8080, 2, 8088, 3, 8096, 2, 8104, 3, 8112, 2, 
            8117, 0, 8118, 2, 8120, 1, 8124, 3, 8125, 27, 8126, 2, 
            8127, 27, 8130, 2, 8133, 0, 8134, 2, 8136, 1, 8140, 3, 
            8141, 27, 8144, 2, 8148, 0, 8150, 2, 8152, 1, 8156, 0, 
            8157, 27, 8160, 2, 8168, 1, 8173, 27, 8176, 0, 8178, 2, 
            8181, 0, 8182, 2, 8184, 1, 8188, 3, 8189, 27, 8191, 0, 
            8192, 12, 8203, 16, 8208, 20, 8214, 24, 8216, 29, 8217, 30, 
            8218, 21, 8219, 29, 8221, 30, 8222, 21, 8223, 29, 8224, 24, 
            8232, 13, 8233, 14, 8234, 16, 8239, 12, 8240, 24, 8249, 29, 
            8250, 30, 8251, 24, 8255, 23, 8257, 24, 8260, 25, 8261, 21, 
            8262, 22, 8263, 24, 8274, 25, 8275, 24, 8276, 23, 8277, 24, 
            8287, 12, 8288, 16, 8293, 0, 8294, 16, 8304, 11, 8305, 4, 
            8306, 0, 8308, 11, 8314, 25, 8317, 21, 8318, 22, 8319, 4, 
            8320, 11, 8330, 25, 8333, 21, 8334, 22, 8335, 0, 8336, 4, 
            8349, 0, 8352, 26, 8385, 0, 8400, 6, 8413, 7, 8417, 6, 
            8418, 7, 8421, 6, 8433, 0, 8448, 28, 8450, 1, 8451, 28, 
            8455, 1, 8456, 28, 8458, 2, 8459, 1, 8462, 2, 8464, 1, 
            8467, 2, 8468, 28, 8469, 1, 8470, 28, 8472, 25, 8473, 1, 
            8478, 28, 8484, 1, 8485, 28, 8486, 1, 8487, 28, 8488, 1, 
            8489, 28, 8490, 1, 8494, 28, 8495, 2, 8496, 1, 8500, 2, 
            8501, 5, 8505, 2, 8506, 28, 8508, 2, 8510, 1, 8512, 25, 
            8517, 1, 8518, 2, 8522, 28, 8523, 25, 8524, 28, 8526, 2, 
            8527, 28, 8528, 11, 8544, 10, 8579, 1, 8580, 2, 8581, 10, 
            8585, 11, 8586, 28, 8588, 0, 8592, 25, 8597, 28, 8602, 25, 
            8604, 28, 8608, 25, 8609, 28, 8611, 25, 8612, 28, 8614, 25, 
            8615, 28, 8622, 25, 8623, 28, 8654, 25, 8656, 28, 8658, 25, 
            8659, 28, 8660, 25, 8661, 28, 8692, 25, 8960, 28, 8968, 21, 
            8969, 22, 8970, 21, 8971, 22, 8972, 28, 8992, 25, 8994, 28, 
            9001, 21, 9002, 22, 9003, 28, 9084, 25, 9085, 28, 9115, 25, 
            9140, 28, 9180, 25, 9186, 28, 9258, 0, 9280, 28, 9291, 0, 
            9312, 11, 9372, 28, 9450, 11, 9472, 28, 9655, 25, 9656, 28, 
            9665, 25, 9666, 28, 9720, 25, 9728, 28, 9839, 25, 9840, 28, 
            10088, 21, 10089, 22, 10090, 21, 10091, 22, 10092, 21, 10093, 22, 
            10094, 21, 10095, 22, 10096, 21, 10097, 22, 10098, 21, 10099, 22, 
            10100, 21, 10101, 22, 10102, 11, 10132, 28, 10176, 25, 10181, 21, 
            10182, 22, 10183, 25, 10214, 21, 10215, 22, 10216, 21, 10217, 22, 
            10218, 21, 10219, 22, 10220, 21, 10221, 22, 10222, 21, 10223, 22, 
            10224, 25, 10240, 28, 10496, 25, 10627, 21, 10628, 22, 10629, 21, 
            10630, 22, 10631, 21, 10632, 22, 10633, 21, 10634, 22, 10635, 21, 
            10636, 22, 10637, 21, 10638, 22, 10639, 21, 10640, 22, 10641, 21, 
            10642, 22, 10643, 21, 10644, 22, 10645, 21, 10646, 22, 10647, 21, 
            10648, 22, 10649, 25, 10712, 21, 10713, 22, 10714, 21, 10715, 22, 
            10716, 25, 10748, 21, 10749, 22, 10750, 25, 11008, 28, 11056, 25, 
            11077, 28, 11079, 25, 11085, 28, 11124, 0, 11126, 28, 11158, 0, 
            11159, 28, 11264, 1, 11312, 2, 11360, 1, 11361, 2, 11362, 1, 
            11365, 2, 11367, 1, 11368, 2, 11369, 1, 11370, 2, 11371, 1, 
            11372, 2, 11373, 1, 11377, 2, 11378, 1, 11379, 2, 11381, 1, 
            11382, 2, 11388, 4, 11390, 1, 11393, 2, 11394, 1, 11395, 2, 
            11396, 1, 11397, 2, 11398, 1, 11399, 2, 11400, 1, 11401, 2, 
            11402, 1, 11403, 2, 11404, 1, 11405, 2, 11406, 1, 11407, 2, 
            11408, 1, 11409, 2, 11410, 1, 11411, 2, 11412, 1, 11413, 2, 
            11414, 1, 11415, 2, 11416, 1, 11417, 2, 11418, 1, 11419, 2, 
            11420, 1, 11421, 2, 11422, 1, 11423, 2, 11424, 1, 11425, 2, 
            11426, 1, 11427, 2, 11428, 1, 11429, 2, 11430, 1, 11431, 2, 
            11432, 1, 11433, 2, 11434, 1, 11435, 2, 11436, 1, 11437, 2, 
            11438, 1, 11439, 2, 11440, 1, 11441, 2, 11442, 1, 11443, 2, 
            11444, 1, 11445, 2, 11446, 1, 11447, 2, 11448, 1, 11449, 2, 
            11450, 1, 11451, 2, 11452, 1, 11453, 2, 11454, 1, 11455, 2, 
            11456, 1, 11457, 2, 11458, 1, 11459, 2, 11460, 1, 11461, 2, 
            11462, 1, 11463, 2, 11464, 1, 11465, 2, 11466, 1, 11467, 2, 
            11468, 1, 11469, 2, 11470, 1, 11471, 2, 11472, 1, 11473, 2, 
            11474, 1, 11475, 2, 11476, 1, 11477, 2, 11478, 1, 11479, 2, 
            11480, 1, 11481, 2, 11482, 1, 11483, 2, 11484, 1, 11485, 2, 
            11486, 1, 11487, 2, 11488, 1, 11489, 2, 11490, 1, 11491, 2, 
            11493, 28, 11499, 1, 11500, 2, 11501, 1, 11502, 2, 11503, 6, 
            11506, 1, 11507, 2, 11508, 0, 11513, 24, 11517, 11, 11518, 24, 
            11520, 2, 11558, 0, 11559, 2, 11560, 0, 11565, 2, 11566, 0, 
            11568, 5, 11624, 0, 11631, 4, 11632, 24, 11633, 0, 11647, 6, 
            11648, 5, 11671, 0, 11680, 5, 11687, 0, 11688, 5, 11695, 0, 
            11696, 5, 11703, 0, 11704, 5, 11711, 0, 11712, 5, 11719, 0, 
            11720, 5, 11727, 0, 11728, 5, 11735, 0, 11736, 5, 11743, 0, 
            11744, 6, 11776, 24, 11778, 29, 11779, 30, 11780, 29, 11781, 30, 
            11782, 24, 11785, 29, 11786, 30, 11787, 24, 11788, 29, 11789, 30, 
            11790, 24, 11799, 20, 11800, 24, 11802, 20, 11803, 24, 11804, 29, 
            11805, 30, 11806, 24, 11808, 29, 11809, 30, 11810, 21, 11811, 22, 
            11812, 21, 11813, 22, 11814, 21, 11815, 22, 11816, 21, 11817, 22, 
            11818, 24, 11823, 4, 11824, 24, 11834, 20, 11836, 24, 11840, 20, 
            11841, 24, 11842, 21, 11843, 24, 11856, 28, 11858, 24, 11861, 21, 
            11862, 22, 11863, 21, 11864, 22, 11865, 21, 11866, 22, 11867, 21, 
            11868, 22, 11869, 20, 11870, 0, 11904, 28, 11930, 0, 11931, 28, 
            12020, 0, 12032, 28, 12246, 0, 12272, 28, 12288, 12, 12289, 24, 
            12292, 28, 12293, 4, 12294, 5, 12295, 10, 12296, 21, 12297, 22, 
            12298, 21, 12299, 22, 12300, 21, 12301, 22, 12302, 21, 12303, 22, 
            12304, 21, 12305, 22, 12306, 28, 12308, 21, 12309, 22, 12310, 21, 
            12311, 22, 12312, 21, 12313, 22, 12314, 21, 12315, 22, 12316, 20, 
            12317, 21, 12318, 22, 12320, 28, 12321, 10, 12330, 6, 12334, 8, 
            12336, 20, 12337, 4, 12342, 28, 12344, 10, 12347, 4, 12348, 5, 
            12349, 24, 12350, 28, 12352, 0, 12353, 5, 12439, 0, 12441, 6, 
            12443, 27, 12445, 4, 12447, 5, 12448, 20, 12449, 5, 12539, 24, 
            12540, 4, 12543, 5, 12544, 0, 12549, 5, 12592, 0, 12593, 5, 
            12687, 0, 12688, 28, 12690, 11, 12694, 28, 12704, 5, 12736, 28, 
            12774, 0, 12783, 28, 12784, 5, 12800, 28, 12831, 0, 12832, 11, 
            12842, 28, 12872, 11, 12880, 28, 12881, 11, 12896, 28, 12928, 11, 
            12938, 28, 12977, 11, 12992, 28, 13312, 5, 19904, 28, 19968, 5, 
            40981, 4, 40982, 5, 42125, 0, 42128, 28, 42183, 0, 42192, 5, 
            42232, 4, 42238, 24, 42240, 5, 42508, 4, 42509, 24, 42512, 5, 
            42528, 9, 42538, 5, 42540, 0, 42560, 1, 42561, 2, 42562, 1, 
            42563, 2, 42564, 1, 42565, 2, 42566, 1, 42567, 2, 42568, 1, 
            42569, 2, 42570, 1, 42571, 2, 42572, 1, 42573, 2, 42574, 1, 
            42575, 2, 42576, 1, 42577, 2, 42578, 1, 42579, 2, 42580, 1, 
            42581, 2, 42582, 1, 42583, 2, 42584, 1, 42585, 2, 42586, 1, 
            42587, 2, 42588, 1, 42589, 2, 42590, 1, 42591, 2, 42592, 1, 
            42593, 2, 42594, 1, 42595, 2, 42596, 1, 42597, 2, 42598, 1, 
            42599, 2, 42600, 1, 42601, 2, 42602, 1, 42603, 2, 42604, 1, 
            42605, 2, 42606, 5, 42607, 6, 42608, 7, 42611, 24, 42612, 6, 
            42622, 24, 42623, 4, 42624, 1, 42625, 2, 42626, 1, 42627, 2, 
            42628, 1, 42629, 2, 42630, 1, 42631, 2, 42632, 1, 42633, 2, 
            42634, 1, 42635, 2, 42636, 1, 42637, 2, 42638, 1, 42639, 2, 
            42640, 1, 42641, 2, 42642, 1, 42643, 2, 42644, 1, 42645, 2, 
            42646, 1, 42647, 2, 42648, 1, 42649, 2, 42650, 1, 42651, 2, 
            42652, 4, 42654, 6, 42656, 5, 42726, 10, 42736, 6, 42738, 24, 
            42744, 0, 42752, 27, 42775, 4, 42784, 27, 42786, 1, 42787, 2, 
            42788, 1, 42789, 2, 42790, 1, 42791, 2, 42792, 1, 42793, 2, 
            42794, 1, 42795, 2, 42796, 1, 42797, 2, 42798, 1, 42799, 2, 
            42802, 1, 42803, 2, 42804, 1, 42805, 2, 42806, 1, 42807, 2, 
            42808, 1, 42809, 2, 42810, 1, 42811, 2, 42812, 1, 42813, 2, 
            42814, 1, 42815, 2, 42816, 1, 42817, 2, 42818, 1, 42819, 2, 
            42820, 1, 42821, 2, 42822, 1, 42823, 2, 42824, 1, 42825, 2, 
            42826, 1, 42827, 2, 42828, 1, 42829, 2, 42830, 1, 42831, 2, 
            42832, 1, 42833, 2, 42834, 1, 42835, 2, 42836, 1, 42837, 2, 
            42838, 1, 42839, 2, 42840, 1, 42841, 2, 42842, 1, 42843, 2, 
            42844, 1, 42845, 2, 42846, 1, 42847, 2, 42848, 1, 42849, 2, 
            42850, 1, 42851, 2, 42852, 1, 42853, 2, 42854, 1, 42855, 2, 
            42856, 1, 42857, 2, 42858, 1, 42859, 2, 42860, 1, 42861, 2, 
            42862, 1, 42863, 2, 42864, 4, 42865, 2, 42873, 1, 42874, 2, 
            42875, 1, 42876, 2, 42877, 1, 42879, 2, 42880, 1, 42881, 2, 
            42882, 1, 42883, 2, 42884, 1, 42885, 2, 42886, 1, 42887, 2, 
            42888, 4, 42889, 27, 42891, 1, 42892, 2, 42893, 1, 42894, 2, 
            42895, 5, 42896, 1, 42897, 2, 42898, 1, 42899, 2, 42902, 1, 
            42903, 2, 42904, 1, 42905, 2, 42906, 1, 42907, 2, 42908, 1, 
            42909, 2, 42910, 1, 42911, 2, 42912, 1, 42913, 2, 42914, 1, 
            42915, 2, 42916, 1, 42917, 2, 42918, 1, 42919, 2, 42920, 1, 
            42921, 2, 42922, 1, 42927, 2, 42928, 1, 42933, 2, 42934, 1, 
            42935, 2, 42936, 1, 42937, 2, 42938, 1, 42939, 2, 42940, 1, 
            42941, 2, 42942, 1, 42943, 2, 42944, 1, 42945, 2, 42946, 1, 
            42947, 2, 42948, 1, 42952, 2, 42953, 1, 42954, 2, 42955, 1, 
            42957, 2, 42958, 0, 42960, 1, 42961, 2, 42962, 0, 42963, 2, 
            42964, 0, 42965, 2, 42966, 1, 42967, 2, 42968, 1, 42969, 2, 
            42970, 1, 42971, 2, 42972, 1, 42973, 0, 42994, 4, 42997, 1, 
            42998, 2, 42999, 5, 43000, 4, 43002, 2, 43003, 5, 43010, 6, 
            43011, 5, 43014, 6, 43015, 5, 43019, 6, 43020, 5, 43043, 8, 
            43045, 6, 43047, 8, 43048, 28, 43052, 6, 43053, 0, 43056, 11, 
            43062, 28, 43064, 26, 43065, 28, 43066, 0, 43072, 5, 43124, 24, 
            43128, 0, 43136, 8, 43138, 5, 43188, 8, 43204, 6, 43206, 0, 
            43214, 24, 43216, 9, 43226, 0, 43232, 6, 43250, 5, 43256, 24, 
            43259, 5, 43260, 24, 43261, 5, 43263, 6, 43264, 9, 43274, 5, 
            43302, 6, 43310, 24, 43312, 5, 43335, 6, 43346, 8, 43348, 0, 
            43359, 24, 43360, 5, 43389, 0, 43392, 6, 43395, 8, 43396, 5, 
            43443, 6, 43444, 8, 43446, 6, 43450, 8, 43452, 6, 43454, 8, 
            43457, 24, 43470, 0, 43471, 4, 43472, 9, 43482, 0, 43486, 24, 
            43488, 5, 43493, 6, 43494, 4, 43495, 5, 43504, 9, 43514, 5, 
            43519, 0, 43520, 5, 43561, 6, 43567, 8, 43569, 6, 43571, 8, 
            43573, 6, 43575, 0, 43584, 5, 43587, 6, 43588, 5, 43596, 6, 
            43597, 8, 43598, 0, 43600, 9, 43610, 0, 43612, 24, 43616, 5, 
            43632, 4, 43633, 5, 43639, 28, 43642, 5, 43643, 8, 43644, 6, 
            43645, 8, 43646, 5, 43696, 6, 43697, 5, 43698, 6, 43701, 5, 
            43703, 6, 43705, 5, 43710, 6, 43712, 5, 43713, 6, 43714, 5, 
            43715, 0, 43739, 5, 43741, 4, 43742, 24, 43744, 5, 43755, 8, 
            43756, 6, 43758, 8, 43760, 24, 43762, 5, 43763, 4, 43765, 8, 
            43766, 6, 43767, 0, 43777, 5, 43783, 0, 43785, 5, 43791, 0, 
            43793, 5, 43799, 0, 43808, 5, 43815, 0, 43816, 5, 43823, 0, 
            43824, 2, 43867, 27, 43868, 4, 43872, 2, 43881, 4, 43882, 27, 
            43884, 0, 43888, 2, 43968, 5, 44003, 8, 44005, 6, 44006, 8, 
            44008, 6, 44009, 8, 44011, 24, 44012, 8, 44013, 6, 44014, 0, 
            44016, 9, 44026, 0, 44032, 5, 55204, 0, 55216, 5, 55239, 0, 
            55243, 5, 55292, 0, 55296, 19, 57344, 18, 63744, 5, 64110, 0, 
            64112, 5, 64218, 0, 64256, 2, 64263, 0, 64275, 2, 64280, 0, 
            64285, 5, 64286, 6, 64287, 5, 64297, 25, 64298, 5, 64311, 0, 
            64312, 5, 64317, 0, 64318, 5, 64319, 0, 64320, 5, 64322, 0, 
            64323, 5, 64325, 0, 64326, 5, 64434, 27, 64451, 0, 64467, 5, 
            64830, 22, 64831, 21, 64832, 28, 64848, 5, 64912, 0, 64914, 5, 
            64968, 0, 64975, 28, 64976, 0, 65008, 5, 65020, 26, 65021, 28, 
            65024, 6, 65040, 24, 65047, 21, 65048, 22, 65049, 24, 65050, 0, 
            65056, 6, 65072, 24, 65073, 20, 65075, 23, 65077, 21, 65078, 22, 
            65079, 21, 65080, 22, 65081, 21, 65082, 22, 65083, 21, 65084, 22, 
            65085, 21, 65086, 22, 65087, 21, 65088, 22, 65089, 21, 65090, 22, 
            65091, 21, 65092, 22, 65093, 24, 65095, 21, 65096, 22, 65097, 24, 
            65101, 23, 65104, 24, 65107, 0, 65108, 24, 65112, 20, 65113, 21, 
            65114, 22, 65115, 21, 65116, 22, 65117, 21, 65118, 22, 65119, 24, 
            65122, 25, 65123, 20, 65124, 25, 65127, 0, 65128, 24, 65129, 26, 
            65130, 24, 65132, 0, 65136, 5, 65141, 0, 65142, 5, 65277, 0, 
            65279, 16, 65280, 0, 65281, 24, 65284, 26, 65285, 24, 65288, 21, 
            65289, 22, 65290, 24, 65291, 25, 65292, 24, 65293, 20, 65294, 24, 
            65296, 9, 65306, 24, 65308, 25, 65311, 24, 65313, 1, 65339, 21, 
            65340, 24, 65341, 22, 65342, 27, 65343, 23, 65344, 27, 65345, 2, 
            65371, 21, 65372, 25, 65373, 22, 65374, 25, 65375, 21, 65376, 22, 
            65377, 24, 65378, 21, 65379, 22, 65380, 24, 65382, 5, 65392, 4, 
            65393, 5, 65438, 4, 65440, 5, 65471, 0, 65474, 5, 65480, 0, 
            65482, 5, 65488, 0, 65490, 5, 65496, 0, 65498, 5, 65501, 0, 
            65504, 26, 65506, 25, 65507, 27, 65508, 28, 65509, 26, 65511, 0, 
            65512, 28, 65513, 25, 65517, 28, 65519, 0, 65529, 16, 65532, 28, 
            65534, 0, 65536, 5, 65548, 0, 65549, 5, 65575, 0, 65576, 5, 
            65595, 0, 65596, 5, 65598, 0, 65599, 5, 65614, 0, 65616, 5, 
            65630, 0, 65664, 5, 65787, 0, 65792, 24, 65795, 0, 65799, 11, 
            65844, 0, 65847, 28, 65856, 10, 65909, 11, 65913, 28, 65930, 11, 
            65932, 28, 65935, 0, 65936, 28, 65949, 0, 65952, 28, 65953, 0, 
            66000, 28, 66045, 6, 66046, 0, 66176, 5, 66205, 0, 66208, 5, 
            66257, 0, 66272, 6, 66273, 11, 66300, 0, 66304, 5, 66336, 11, 
            66340, 0, 66349, 5, 66369, 10, 66370, 5, 66378, 10, 66379, 0, 
            66384, 5, 66422, 6, 66427, 0, 66432, 5, 66462, 0, 66463, 24, 
            66464, 5, 66500, 0, 66504, 5, 66512, 24, 66513, 10, 66518, 0, 
            66560, 1, 66600, 2, 66640, 5, 66718, 0, 66720, 9, 66730, 0, 
            66736, 1, 66772, 0, 66776, 2, 66812, 0, 66816, 5, 66856, 0, 
            66864, 5, 66916, 0, 66927, 24, 66928, 1, 66939, 0, 66940, 1, 
            66955, 0, 66956, 1, 66963, 0, 66964, 1, 66966, 0, 66967, 2, 
            66978, 0, 66979, 2, 66994, 0, 66995, 2, 67002, 0, 67003, 2, 
            67005, 0, 67008, 5, 67060, 0, 67072, 5, 67383, 0, 67392, 5, 
            67414, 0, 67424, 5, 67432, 0, 67456, 4, 67462, 0, 67463, 4, 
        };
    }

    private static int[] typeTable2() {
        return new int[] {
            67505, 0, 67506, 4, 67515, 0, 67584, 5, 67590, 0, 67592, 5, 
            67593, 0, 67594, 5, 67638, 0, 67639, 5, 67641, 0, 67644, 5, 
            67645, 0, 67647, 5, 67670, 0, 67671, 24, 67672, 11, 67680, 5, 
            67703, 28, 67705, 11, 67712, 5, 67743, 0, 67751, 11, 67760, 0, 
            67808, 5, 67827, 0, 67828, 5, 67830, 0, 67835, 11, 67840, 5, 
            67862, 11, 67868, 0, 67871, 24, 67872, 5, 67898, 0, 67903, 24, 
            67904, 0, 67968, 5, 68024, 0, 68028, 11, 68030, 5, 68032, 11, 
            68048, 0, 68050, 11, 68096, 5, 68097, 6, 68100, 0, 68101, 6, 
            68103, 0, 68108, 6, 68112, 5, 68116, 0, 68117, 5, 68120, 0, 
            68121, 5, 68150, 0, 68152, 6, 68155, 0, 68159, 6, 68160, 11, 
            68169, 0, 68176, 24, 68185, 0, 68192, 5, 68221, 11, 68223, 24, 
            68224, 5, 68253, 11, 68256, 0, 68288, 5, 68296, 28, 68297, 5, 
            68325, 6, 68327, 0, 68331, 11, 68336, 24, 68343, 0, 68352, 5, 
            68406, 0, 68409, 24, 68416, 5, 68438, 0, 68440, 11, 68448, 5, 
            68467, 0, 68472, 11, 68480, 5, 68498, 0, 68505, 24, 68509, 0, 
            68521, 11, 68528, 0, 68608, 5, 68681, 0, 68736, 1, 68787, 0, 
            68800, 2, 68851, 0, 68858, 11, 68864, 5, 68900, 6, 68904, 0, 
            68912, 9, 68922, 0, 68928, 9, 68938, 5, 68942, 4, 68943, 5, 
            68944, 1, 68966, 0, 68969, 6, 68974, 20, 68975, 4, 68976, 2, 
            68998, 0, 69006, 25, 69008, 0, 69216, 11, 69247, 0, 69248, 5, 
            69290, 0, 69291, 6, 69293, 20, 69294, 0, 69296, 5, 69298, 0, 
            69314, 5, 69317, 0, 69372, 6, 69376, 5, 69405, 11, 69415, 5, 
            69416, 0, 69424, 5, 69446, 6, 69457, 11, 69461, 24, 69466, 0, 
            69488, 5, 69506, 6, 69510, 24, 69514, 0, 69552, 5, 69573, 11, 
            69580, 0, 69600, 5, 69623, 0, 69632, 8, 69633, 6, 69634, 8, 
            69635, 5, 69688, 6, 69703, 24, 69710, 0, 69714, 11, 69734, 9, 
            69744, 6, 69745, 5, 69747, 6, 69749, 5, 69750, 0, 69759, 6, 
            69762, 8, 69763, 5, 69808, 8, 69811, 6, 69815, 8, 69817, 6, 
            69819, 24, 69821, 16, 69822, 24, 69826, 6, 69827, 0, 69837, 16, 
            69838, 0, 69840, 5, 69865, 0, 69872, 9, 69882, 0, 69888, 6, 
            69891, 5, 69927, 6, 69932, 8, 69933, 6, 69941, 0, 69942, 9, 
            69952, 24, 69956, 5, 69957, 8, 69959, 5, 69960, 0, 69968, 5, 
            70003, 6, 70004, 24, 70006, 5, 70007, 0, 70016, 6, 70018, 8, 
            70019, 5, 70067, 8, 70070, 6, 70079, 8, 70081, 5, 70085, 24, 
            70089, 6, 70093, 24, 70094, 8, 70095, 6, 70096, 9, 70106, 5, 
            70107, 24, 70108, 5, 70109, 24, 70112, 0, 70113, 11, 70133, 0, 
            70144, 5, 70162, 0, 70163, 5, 70188, 8, 70191, 6, 70194, 8, 
            70196, 6, 70197, 8, 70198, 6, 70200, 24, 70206, 6, 70207, 5, 
            70209, 6, 70210, 0, 70272, 5, 70279, 0, 70280, 5, 70281, 0, 
            70282, 5, 70286, 0, 70287, 5, 70302, 0, 70303, 5, 70313, 24, 
            70314, 0, 70320, 5, 70367, 6, 70368, 8, 70371, 6, 70379, 0, 
            70384, 9, 70394, 0, 70400, 6, 70402, 8, 70404, 0, 70405, 5, 
            70413, 0, 70415, 5, 70417, 0, 70419, 5, 70441, 0, 70442, 5, 
            70449, 0, 70450, 5, 70452, 0, 70453, 5, 70458, 0, 70459, 6, 
            70461, 5, 70462, 8, 70464, 6, 70465, 8, 70469, 0, 70471, 8, 
            70473, 0, 70475, 8, 70478, 0, 70480, 5, 70481, 0, 70487, 8, 
            70488, 0, 70493, 5, 70498, 8, 70500, 0, 70502, 6, 70509, 0, 
            70512, 6, 70517, 0, 70528, 5, 70538, 0, 70539, 5, 70540, 0, 
            70542, 5, 70543, 0, 70544, 5, 70582, 0, 70583, 5, 70584, 8, 
            70587, 6, 70593, 0, 70594, 8, 70595, 0, 70597, 8, 70598, 0, 
            70599, 8, 70603, 0, 70604, 8, 70606, 6, 70607, 8, 70608, 6, 
            70609, 5, 70610, 6, 70611, 5, 70612, 24, 70614, 0, 70615, 24, 
            70617, 0, 70625, 6, 70627, 0, 70656, 5, 70709, 8, 70712, 6, 
            70720, 8, 70722, 6, 70725, 8, 70726, 6, 70727, 5, 70731, 24, 
            70736, 9, 70746, 24, 70748, 0, 70749, 24, 70750, 6, 70751, 5, 
            70754, 0, 70784, 5, 70832, 8, 70835, 6, 70841, 8, 70842, 6, 
            70843, 8, 70847, 6, 70849, 8, 70850, 6, 70852, 5, 70854, 24, 
            70855, 5, 70856, 0, 70864, 9, 70874, 0, 71040, 5, 71087, 8, 
            71090, 6, 71094, 0, 71096, 8, 71100, 6, 71102, 8, 71103, 6, 
            71105, 24, 71128, 5, 71132, 6, 71134, 0, 71168, 5, 71216, 8, 
            71219, 6, 71227, 8, 71229, 6, 71230, 8, 71231, 6, 71233, 24, 
            71236, 5, 71237, 0, 71248, 9, 71258, 0, 71264, 24, 71277, 0, 
            71296, 5, 71339, 6, 71340, 8, 71341, 6, 71342, 8, 71344, 6, 
            71350, 8, 71351, 6, 71352, 5, 71353, 24, 71354, 0, 71360, 9, 
            71370, 0, 71376, 9, 71396, 0, 71424, 5, 71451, 0, 71453, 6, 
            71454, 8, 71455, 6, 71456, 8, 71458, 6, 71462, 8, 71463, 6, 
            71468, 0, 71472, 9, 71482, 11, 71484, 24, 71487, 28, 71488, 5, 
            71495, 0, 71680, 5, 71724, 8, 71727, 6, 71736, 8, 71737, 6, 
            71739, 24, 71740, 0, 71840, 1, 71872, 2, 71904, 9, 71914, 11, 
            71923, 0, 71935, 5, 71943, 0, 71945, 5, 71946, 0, 71948, 5, 
            71956, 0, 71957, 5, 71959, 0, 71960, 5, 71984, 8, 71990, 0, 
            71991, 8, 71993, 0, 71995, 6, 71997, 8, 71998, 6, 71999, 5, 
            72000, 8, 72001, 5, 72002, 8, 72003, 6, 72004, 24, 72007, 0, 
            72016, 9, 72026, 0, 72096, 5, 72104, 0, 72106, 5, 72145, 8, 
            72148, 6, 72152, 0, 72154, 6, 72156, 8, 72160, 6, 72161, 5, 
            72162, 24, 72163, 5, 72164, 8, 72165, 0, 72192, 5, 72193, 6, 
            72203, 5, 72243, 6, 72249, 8, 72250, 5, 72251, 6, 72255, 24, 
            72263, 6, 72264, 0, 72272, 5, 72273, 6, 72279, 8, 72281, 6, 
            72284, 5, 72330, 6, 72343, 8, 72344, 6, 72346, 24, 72349, 5, 
            72350, 24, 72355, 0, 72368, 5, 72441, 0, 72448, 24, 72458, 0, 
            72640, 5, 72673, 24, 72674, 0, 72688, 9, 72698, 0, 72704, 5, 
            72713, 0, 72714, 5, 72751, 8, 72752, 6, 72759, 0, 72760, 6, 
            72766, 8, 72767, 6, 72768, 5, 72769, 24, 72774, 0, 72784, 9, 
            72794, 11, 72813, 0, 72816, 24, 72818, 5, 72848, 0, 72850, 6, 
            72872, 0, 72873, 8, 72874, 6, 72881, 8, 72882, 6, 72884, 8, 
            72885, 6, 72887, 0, 72960, 5, 72967, 0, 72968, 5, 72970, 0, 
            72971, 5, 73009, 6, 73015, 0, 73018, 6, 73019, 0, 73020, 6, 
            73022, 0, 73023, 6, 73030, 5, 73031, 6, 73032, 0, 73040, 9, 
            73050, 0, 73056, 5, 73062, 0, 73063, 5, 73065, 0, 73066, 5, 
            73098, 8, 73103, 0, 73104, 6, 73106, 0, 73107, 8, 73109, 6, 
            73110, 8, 73111, 6, 73112, 5, 73113, 0, 73120, 9, 73130, 0, 
            73440, 5, 73459, 6, 73461, 8, 73463, 24, 73465, 0, 73472, 6, 
            73474, 5, 73475, 8, 73476, 5, 73489, 0, 73490, 5, 73524, 8, 
            73526, 6, 73531, 0, 73534, 8, 73536, 6, 73537, 8, 73538, 6, 
            73539, 24, 73552, 9, 73562, 6, 73563, 0, 73648, 5, 73649, 0, 
            73664, 11, 73685, 28, 73693, 26, 73697, 28, 73714, 0, 73727, 24, 
            73728, 5, 74650, 0, 74752, 10, 74863, 0, 74864, 24, 74869, 0, 
            74880, 5, 75076, 0, 77712, 5, 77809, 24, 77811, 0, 77824, 5, 
            78896, 16, 78912, 6, 78913, 5, 78919, 6, 78934, 0, 78944, 5, 
            82939, 0, 82944, 5, 83527, 0, 90368, 5, 90398, 6, 90410, 8, 
            90413, 6, 90416, 9, 90426, 0, 92160, 5, 92729, 0, 92736, 5, 
            92767, 0, 92768, 9, 92778, 0, 92782, 24, 92784, 5, 92863, 0, 
            92864, 9, 92874, 0, 92880, 5, 92910, 0, 92912, 6, 92917, 24, 
            92918, 0, 92928, 5, 92976, 6, 92983, 24, 92988, 28, 92992, 4, 
            92996, 24, 92997, 28, 92998, 0, 93008, 9, 93018, 0, 93019, 11, 
            93026, 0, 93027, 5, 93048, 0, 93053, 5, 93072, 0, 93504, 4, 
            93507, 5, 93547, 4, 93549, 24, 93552, 9, 93562, 0, 93760, 1, 
            93792, 2, 93824, 11, 93847, 24, 93851, 0, 93952, 5, 94027, 0, 
            94031, 6, 94032, 5, 94033, 8, 94088, 0, 94095, 6, 94099, 4, 
            94112, 0, 94176, 4, 94178, 24, 94179, 4, 94180, 6, 94181, 0, 
            94192, 8, 94194, 0, 94208, 5, 100344, 0, 100352, 5, 101590, 0, 
            101631, 5, 101641, 0, 110576, 4, 110580, 0, 110581, 4, 110588, 0, 
            110589, 4, 110591, 0, 110592, 5, 110883, 0, 110898, 5, 110899, 0, 
            110928, 5, 110931, 0, 110933, 5, 110934, 0, 110948, 5, 110952, 0, 
            110960, 5, 111356, 0, 113664, 5, 113771, 0, 113776, 5, 113789, 0, 
            113792, 5, 113801, 0, 113808, 5, 113818, 0, 113820, 28, 113821, 6, 
            113823, 24, 113824, 16, 113828, 0, 117760, 28, 118000, 9, 118010, 0, 
            118016, 28, 118452, 0, 118528, 6, 118574, 0, 118576, 6, 118599, 0, 
            118608, 28, 118724, 0, 118784, 28, 119030, 0, 119040, 28, 119079, 0, 
            119081, 28, 119141, 8, 119143, 6, 119146, 28, 119149, 8, 119155, 16, 
            119163, 6, 119171, 28, 119173, 6, 119180, 28, 119210, 6, 119214, 28, 
            119275, 0, 119296, 28, 119362, 6, 119365, 28, 119366, 0, 119488, 11, 
            119508, 0, 119520, 11, 119540, 0, 119552, 28, 119639, 0, 119648, 11, 
            119673, 0, 119808, 1, 119834, 2, 119860, 1, 119886, 2, 119893, 0, 
            119894, 2, 119912, 1, 119938, 2, 119964, 1, 119965, 0, 119966, 1, 
            119968, 0, 119970, 1, 119971, 0, 119973, 1, 119975, 0, 119977, 1, 
            119981, 0, 119982, 1, 119990, 2, 119994, 0, 119995, 2, 119996, 0, 
            119997, 2, 120004, 0, 120005, 2, 120016, 1, 120042, 2, 120068, 1, 
            120070, 0, 120071, 1, 120075, 0, 120077, 1, 120085, 0, 120086, 1, 
            120093, 0, 120094, 2, 120120, 1, 120122, 0, 120123, 1, 120127, 0, 
            120128, 1, 120133, 0, 120134, 1, 120135, 0, 120138, 1, 120145, 0, 
            120146, 2, 120172, 1, 120198, 2, 120224, 1, 120250, 2, 120276, 1, 
            120302, 2, 120328, 1, 120354, 2, 120380, 1, 120406, 2, 120432, 1, 
            120458, 2, 120486, 0, 120488, 1, 120513, 25, 120514, 2, 120539, 25, 
            120540, 2, 120546, 1, 120571, 25, 120572, 2, 120597, 25, 120598, 2, 
            120604, 1, 120629, 25, 120630, 2, 120655, 25, 120656, 2, 120662, 1, 
            120687, 25, 120688, 2, 120713, 25, 120714, 2, 120720, 1, 120745, 25, 
            120746, 2, 120771, 25, 120772, 2, 120778, 1, 120779, 2, 120780, 0, 
            120782, 9, 120832, 28, 121344, 6, 121399, 28, 121403, 6, 121453, 28, 
            121461, 6, 121462, 28, 121476, 6, 121477, 28, 121479, 24, 121484, 0, 
            121499, 6, 121504, 0, 121505, 6, 121520, 0, 122624, 2, 122634, 5, 
            122635, 2, 122655, 0, 122661, 2, 122667, 0, 122880, 6, 122887, 0, 
            122888, 6, 122905, 0, 122907, 6, 122914, 0, 122915, 6, 122917, 0, 
            122918, 6, 122923, 0, 122928, 4, 122990, 0, 123023, 6, 123024, 0, 
            123136, 5, 123181, 0, 123184, 6, 123191, 4, 123198, 0, 123200, 9, 
            123210, 0, 123214, 5, 123215, 28, 123216, 0, 123536, 5, 123566, 6, 
            123567, 0, 123584, 5, 123628, 6, 123632, 9, 123642, 0, 123647, 26, 
            123648, 0, 124112, 5, 124139, 4, 124140, 6, 124144, 9, 124154, 0, 
            124368, 5, 124398, 6, 124400, 5, 124401, 9, 124411, 0, 124415, 24, 
            124416, 0, 124896, 5, 124903, 0, 124904, 5, 124908, 0, 124909, 5, 
            124911, 0, 124912, 5, 124927, 0, 124928, 5, 125125, 0, 125127, 11, 
            125136, 6, 125143, 0, 125184, 1, 125218, 2, 125252, 6, 125259, 4, 
            125260, 0, 125264, 9, 125274, 0, 125278, 24, 125280, 0, 126065, 11, 
            126124, 28, 126125, 11, 126128, 26, 126129, 11, 126133, 0, 126209, 11, 
            126254, 28, 126255, 11, 126270, 0, 126464, 5, 126468, 0, 126469, 5, 
            126496, 0, 126497, 5, 126499, 0, 126500, 5, 126501, 0, 126503, 5, 
            126504, 0, 126505, 5, 126515, 0, 126516, 5, 126520, 0, 126521, 5, 
            126522, 0, 126523, 5, 126524, 0, 126530, 5, 126531, 0, 126535, 5, 
            126536, 0, 126537, 5, 126538, 0, 126539, 5, 126540, 0, 126541, 5, 
            126544, 0, 126545, 5, 126547, 0, 126548, 5, 126549, 0, 126551, 5, 
            126552, 0, 126553, 5, 126554, 0, 126555, 5, 126556, 0, 126557, 5, 
            126558, 0, 126559, 5, 126560, 0, 126561, 5, 126563, 0, 126564, 5, 
            126565, 0, 126567, 5, 126571, 0, 126572, 5, 126579, 0, 126580, 5, 
            126584, 0, 126585, 5, 126589, 0, 126590, 5, 126591, 0, 126592, 5, 
            126602, 0, 126603, 5, 126620, 0, 126625, 5, 126628, 0, 126629, 5, 
            126634, 0, 126635, 5, 126652, 0, 126704, 25, 126706, 0, 126976, 28, 
            127020, 0, 127024, 28, 127124, 0, 127136, 28, 127151, 0, 127153, 28, 
            127168, 0, 127169, 28, 127184, 0, 127185, 28, 127222, 0, 127232, 11, 
            127245, 28, 127406, 0, 127462, 28, 127491, 0, 127504, 28, 127548, 0, 
            127552, 28, 127561, 0, 127568, 28, 127570, 0, 127584, 28, 127590, 0, 
            127744, 28, 127995, 27, 128000, 28, 128728, 0, 128732, 28, 128749, 0, 
            128752, 28, 128765, 0, 128768, 28, 128887, 0, 128891, 28, 128986, 0, 
            128992, 28, 129004, 0, 129008, 28, 129009, 0, 129024, 28, 129036, 0, 
            129040, 28, 129096, 0, 129104, 28, 129114, 0, 129120, 28, 129160, 0, 
            129168, 28, 129198, 0, 129200, 28, 129212, 0, 129216, 28, 129218, 0, 
            129280, 28, 129620, 0, 129632, 28, 129646, 0, 129648, 28, 129661, 0, 
            129664, 28, 129674, 0, 129679, 28, 129735, 0, 129742, 28, 129757, 0, 
            129759, 28, 129770, 0, 129776, 28, 129785, 0, 129792, 28, 129939, 0, 
            129940, 28, 130032, 9, 130042, 0, 131072, 5, 173792, 0, 173824, 5, 
            177978, 0, 177984, 5, 178206, 0, 178208, 5, 183970, 0, 183984, 5, 
            191457, 0, 191472, 5, 192094, 0, 194560, 5, 195102, 0, 196608, 5, 
            201547, 0, 201552, 5, 205744, 0, 917505, 16, 917506, 0, 917536, 16, 
            917632, 0, 917760, 6, 918000, 0, 983040, 18, 1048574, 0, 1048576, 18, 
            1114110, 0, 
        };
    }

    // La clase bidireccional de cada code point, como pares [desde, clase].
    // 4600 enteros, 2300 rangos.
    private static int[] directionTable() {
        int[] part0 = Character.directionTable0();
        int[] part1 = Character.directionTable1();
        int[] out = new int[part0.length + part1.length];
        int at = 0;
        System.arraycopy(part0, 0, out, at, part0.length);
        at = at + part0.length;
        System.arraycopy(part1, 0, out, at, part1.length);
        return out;
    }

    private static int[] directionTable0() {
        return new int[] {
            0, 9, 9, 11, 10, 10, 11, 11, 12, 12, 13, 10, 
            14, 9, 28, 10, 31, 11, 32, 12, 33, 13, 35, 5, 
            38, 13, 43, 4, 44, 7, 45, 4, 46, 7, 48, 3, 
            58, 7, 59, 13, 65, 0, 91, 13, 97, 0, 123, 13, 
            127, 9, 133, 10, 134, 9, 160, 7, 161, 13, 162, 5, 
            166, 13, 170, 0, 171, 13, 173, 9, 174, 13, 176, 5, 
            178, 3, 180, 13, 181, 0, 182, 13, 185, 3, 186, 0, 
            187, 13, 192, 0, 215, 13, 216, 0, 247, 13, 248, 0, 
            697, 13, 699, 0, 706, 13, 720, 0, 722, 13, 736, 0, 
            741, 13, 750, 0, 751, 13, 768, 8, 880, 0, 884, 13, 
            886, 0, 888, -1, 890, 0, 894, 13, 895, 0, 896, -1, 
            900, 13, 902, 0, 903, 13, 904, 0, 907, -1, 908, 0, 
            909, -1, 910, 0, 930, -1, 931, 0, 1014, 13, 1015, 0, 
            1155, 8, 1162, 0, 1328, -1, 1329, 0, 1367, -1, 1369, 0, 
            1418, 13, 1419, -1, 1421, 13, 1423, 5, 1424, -1, 1425, 8, 
            1470, 1, 1471, 8, 1472, 1, 1473, 8, 1475, 1, 1476, 8, 
            1478, 1, 1479, 8, 1480, -1, 1488, 1, 1515, -1, 1519, 1, 
            1525, -1, 1536, 6, 1542, 13, 1544, 2, 1545, 5, 1547, 2, 
            1548, 7, 1549, 2, 1550, 13, 1552, 8, 1563, 2, 1611, 8, 
            1632, 6, 1642, 5, 1643, 6, 1645, 2, 1648, 8, 1649, 2, 
            1750, 8, 1757, 6, 1758, 13, 1759, 8, 1765, 2, 1767, 8, 
            1769, 13, 1770, 8, 1774, 2, 1776, 3, 1786, 2, 1806, -1, 
            1807, 2, 1809, 8, 1810, 2, 1840, 8, 1867, -1, 1869, 2, 
            1958, 8, 1969, 2, 1970, -1, 1984, 1, 2027, 8, 2036, 1, 
            2038, 13, 2042, 1, 2043, -1, 2045, 8, 2046, 1, 2070, 8, 
            2074, 1, 2075, 8, 2084, 1, 2085, 8, 2088, 1, 2089, 8, 
            2094, -1, 2096, 1, 2111, -1, 2112, 1, 2137, 8, 2140, -1, 
            2142, 1, 2143, -1, 2144, 2, 2155, -1, 2160, 2, 2191, -1, 
            2192, 6, 2194, -1, 2199, 8, 2208, 2, 2250, 8, 2274, 6, 
            2275, 8, 2307, 0, 2362, 8, 2363, 0, 2364, 8, 2365, 0, 
            2369, 8, 2377, 0, 2381, 8, 2382, 0, 2385, 8, 2392, 0, 
            2402, 8, 2404, 0, 2433, 8, 2434, 0, 2436, -1, 2437, 0, 
            2445, -1, 2447, 0, 2449, -1, 2451, 0, 2473, -1, 2474, 0, 
            2481, -1, 2482, 0, 2483, -1, 2486, 0, 2490, -1, 2492, 8, 
            2493, 0, 2497, 8, 2501, -1, 2503, 0, 2505, -1, 2507, 0, 
            2509, 8, 2510, 0, 2511, -1, 2519, 0, 2520, -1, 2524, 0, 
            2526, -1, 2527, 0, 2530, 8, 2532, -1, 2534, 0, 2546, 5, 
            2548, 0, 2555, 5, 2556, 0, 2558, 8, 2559, -1, 2561, 8, 
            2563, 0, 2564, -1, 2565, 0, 2571, -1, 2575, 0, 2577, -1, 
            2579, 0, 2601, -1, 2602, 0, 2609, -1, 2610, 0, 2612, -1, 
            2613, 0, 2615, -1, 2616, 0, 2618, -1, 2620, 8, 2621, -1, 
            2622, 0, 2625, 8, 2627, -1, 2631, 8, 2633, -1, 2635, 8, 
            2638, -1, 2641, 8, 2642, -1, 2649, 0, 2653, -1, 2654, 0, 
            2655, -1, 2662, 0, 2672, 8, 2674, 0, 2677, 8, 2678, 0, 
            2679, -1, 2689, 8, 2691, 0, 2692, -1, 2693, 0, 2702, -1, 
            2703, 0, 2706, -1, 2707, 0, 2729, -1, 2730, 0, 2737, -1, 
            2738, 0, 2740, -1, 2741, 0, 2746, -1, 2748, 8, 2749, 0, 
            2753, 8, 2758, -1, 2759, 8, 2761, 0, 2762, -1, 2763, 0, 
            2765, 8, 2766, -1, 2768, 0, 2769, -1, 2784, 0, 2786, 8, 
            2788, -1, 2790, 0, 2801, 5, 2802, -1, 2809, 0, 2810, 8, 
            2816, -1, 2817, 8, 2818, 0, 2820, -1, 2821, 0, 2829, -1, 
            2831, 0, 2833, -1, 2835, 0, 2857, -1, 2858, 0, 2865, -1, 
            2866, 0, 2868, -1, 2869, 0, 2874, -1, 2876, 8, 2877, 0, 
            2879, 8, 2880, 0, 2881, 8, 2885, -1, 2887, 0, 2889, -1, 
            2891, 0, 2893, 8, 2894, -1, 2901, 8, 2903, 0, 2904, -1, 
            2908, 0, 2910, -1, 2911, 0, 2914, 8, 2916, -1, 2918, 0, 
            2936, -1, 2946, 8, 2947, 0, 2948, -1, 2949, 0, 2955, -1, 
            2958, 0, 2961, -1, 2962, 0, 2966, -1, 2969, 0, 2971, -1, 
            2972, 0, 2973, -1, 2974, 0, 2976, -1, 2979, 0, 2981, -1, 
            2984, 0, 2987, -1, 2990, 0, 3002, -1, 3006, 0, 3008, 8, 
            3009, 0, 3011, -1, 3014, 0, 3017, -1, 3018, 0, 3021, 8, 
            3022, -1, 3024, 0, 3025, -1, 3031, 0, 3032, -1, 3046, 0, 
            3059, 13, 3065, 5, 3066, 13, 3067, -1, 3072, 8, 3073, 0, 
            3076, 8, 3077, 0, 3085, -1, 3086, 0, 3089, -1, 3090, 0, 
            3113, -1, 3114, 0, 3130, -1, 3132, 8, 3133, 0, 3134, 8, 
            3137, 0, 3141, -1, 3142, 8, 3145, -1, 3146, 8, 3150, -1, 
            3157, 8, 3159, -1, 3160, 0, 3163, -1, 3165, 0, 3166, -1, 
            3168, 0, 3170, 8, 3172, -1, 3174, 0, 3184, -1, 3191, 0, 
            3192, 13, 3199, 0, 3201, 8, 3202, 0, 3213, -1, 3214, 0, 
            3217, -1, 3218, 0, 3241, -1, 3242, 0, 3252, -1, 3253, 0, 
            3258, -1, 3260, 8, 3261, 0, 3269, -1, 3270, 0, 3273, -1, 
            3274, 0, 3276, 8, 3278, -1, 3285, 0, 3287, -1, 3293, 0, 
            3295, -1, 3296, 0, 3298, 8, 3300, -1, 3302, 0, 3312, -1, 
            3313, 0, 3316, -1, 3328, 8, 3330, 0, 3341, -1, 3342, 0, 
            3345, -1, 3346, 0, 3387, 8, 3389, 0, 3393, 8, 3397, -1, 
            3398, 0, 3401, -1, 3402, 0, 3405, 8, 3406, 0, 3408, -1, 
            3412, 0, 3426, 8, 3428, -1, 3430, 0, 3456, -1, 3457, 8, 
            3458, 0, 3460, -1, 3461, 0, 3479, -1, 3482, 0, 3506, -1, 
            3507, 0, 3516, -1, 3517, 0, 3518, -1, 3520, 0, 3527, -1, 
            3530, 8, 3531, -1, 3535, 0, 3538, 8, 3541, -1, 3542, 8, 
            3543, -1, 3544, 0, 3552, -1, 3558, 0, 3568, -1, 3570, 0, 
            3573, -1, 3585, 0, 3633, 8, 3634, 0, 3636, 8, 3643, -1, 
            3647, 5, 3648, 0, 3655, 8, 3663, 0, 3676, -1, 3713, 0, 
            3715, -1, 3716, 0, 3717, -1, 3718, 0, 3723, -1, 3724, 0, 
            3748, -1, 3749, 0, 3750, -1, 3751, 0, 3761, 8, 3762, 0, 
            3764, 8, 3773, 0, 3774, -1, 3776, 0, 3781, -1, 3782, 0, 
            3783, -1, 3784, 8, 3791, -1, 3792, 0, 3802, -1, 3804, 0, 
            3808, -1, 3840, 0, 3864, 8, 3866, 0, 3893, 8, 3894, 0, 
            3895, 8, 3896, 0, 3897, 8, 3898, 13, 3902, 0, 3912, -1, 
            3913, 0, 3949, -1, 3953, 8, 3967, 0, 3968, 8, 3973, 0, 
            3974, 8, 3976, 0, 3981, 8, 3992, -1, 3993, 8, 4029, -1, 
            4030, 0, 4038, 8, 4039, 0, 4045, -1, 4046, 0, 4059, -1, 
            4096, 0, 4141, 8, 4145, 0, 4146, 8, 4152, 0, 4153, 8, 
            4155, 0, 4157, 8, 4159, 0, 4184, 8, 4186, 0, 4190, 8, 
            4193, 0, 4209, 8, 4213, 0, 4226, 8, 4227, 0, 4229, 8, 
            4231, 0, 4237, 8, 4238, 0, 4253, 8, 4254, 0, 4294, -1, 
            4295, 0, 4296, -1, 4301, 0, 4302, -1, 4304, 0, 4681, -1, 
            4682, 0, 4686, -1, 4688, 0, 4695, -1, 4696, 0, 4697, -1, 
            4698, 0, 4702, -1, 4704, 0, 4745, -1, 4746, 0, 4750, -1, 
            4752, 0, 4785, -1, 4786, 0, 4790, -1, 4792, 0, 4799, -1, 
            4800, 0, 4801, -1, 4802, 0, 4806, -1, 4808, 0, 4823, -1, 
            4824, 0, 4881, -1, 4882, 0, 4886, -1, 4888, 0, 4955, -1, 
            4957, 8, 4960, 0, 4989, -1, 4992, 0, 5008, 13, 5018, -1, 
            5024, 0, 5110, -1, 5112, 0, 5118, -1, 5120, 13, 5121, 0, 
            5760, 12, 5761, 0, 5787, 13, 5789, -1, 5792, 0, 5881, -1, 
            5888, 0, 5906, 8, 5909, 0, 5910, -1, 5919, 0, 5938, 8, 
            5940, 0, 5943, -1, 5952, 0, 5970, 8, 5972, -1, 5984, 0, 
            5997, -1, 5998, 0, 6001, -1, 6002, 8, 6004, -1, 6016, 0, 
            6068, 8, 6070, 0, 6071, 8, 6078, 0, 6086, 8, 6087, 0, 
            6089, 8, 6100, 0, 6107, 5, 6108, 0, 6109, 8, 6110, -1, 
            6112, 0, 6122, -1, 6128, 13, 6138, -1, 6144, 13, 6155, 8, 
            6158, 9, 6159, 8, 6160, 0, 6170, -1, 6176, 0, 6265, -1, 
            6272, 0, 6277, 8, 6279, 0, 6313, 8, 6314, 0, 6315, -1, 
            6320, 0, 6390, -1, 6400, 0, 6431, -1, 6432, 8, 6435, 0, 
            6439, 8, 6441, 0, 6444, -1, 6448, 0, 6450, 8, 6451, 0, 
            6457, 8, 6460, -1, 6464, 13, 6465, -1, 6468, 13, 6470, 0, 
            6510, -1, 6512, 0, 6517, -1, 6528, 0, 6572, -1, 6576, 0, 
            6602, -1, 6608, 0, 6619, -1, 6622, 13, 6656, 0, 6679, 8, 
            6681, 0, 6683, 8, 6684, -1, 6686, 0, 6742, 8, 6743, 0, 
            6744, 8, 6751, -1, 6752, 8, 6753, 0, 6754, 8, 6755, 0, 
            6757, 8, 6765, 0, 6771, 8, 6781, -1, 6783, 8, 6784, 0, 
            6794, -1, 6800, 0, 6810, -1, 6816, 0, 6830, -1, 6832, 8, 
            6863, -1, 6912, 8, 6916, 0, 6964, 8, 6965, 0, 6966, 8, 
            6971, 0, 6972, 8, 6973, 0, 6978, 8, 6979, 0, 6989, -1, 
            6990, 0, 7019, 8, 7028, 0, 7040, 8, 7042, 0, 7074, 8, 
            7078, 0, 7080, 8, 7082, 0, 7083, 8, 7086, 0, 7142, 8, 
            7143, 0, 7144, 8, 7146, 0, 7149, 8, 7150, 0, 7151, 8, 
            7154, 0, 7156, -1, 7164, 0, 7212, 8, 7220, 0, 7222, 8, 
            7224, -1, 7227, 0, 7242, -1, 7245, 0, 7307, -1, 7312, 0, 
            7355, -1, 7357, 0, 7368, -1, 7376, 8, 7379, 0, 7380, 8, 
            7393, 0, 7394, 8, 7401, 0, 7405, 8, 7406, 0, 7412, 8, 
            7413, 0, 7416, 8, 7418, 0, 7419, -1, 7424, 0, 7616, 8, 
            7680, 0, 7958, -1, 7960, 0, 7966, -1, 7968, 0, 8006, -1, 
            8008, 0, 8014, -1, 8016, 0, 8024, -1, 8025, 0, 8026, -1, 
            8027, 0, 8028, -1, 8029, 0, 8030, -1, 8031, 0, 8062, -1, 
            8064, 0, 8117, -1, 8118, 0, 8125, 13, 8126, 0, 8127, 13, 
            8130, 0, 8133, -1, 8134, 0, 8141, 13, 8144, 0, 8148, -1, 
            8150, 0, 8156, -1, 8157, 13, 8160, 0, 8173, 13, 8176, -1, 
            8178, 0, 8181, -1, 8182, 0, 8189, 13, 8191, -1, 8192, 12, 
            8203, 9, 8206, 0, 8207, 1, 8208, 13, 8232, 12, 8233, 10, 
            8234, 14, 8235, 16, 8236, 18, 8237, 15, 8238, 17, 8239, 7, 
            8240, 5, 8245, 13, 8260, 7, 8261, 13, 8287, 12, 8288, 9, 
            8293, -1, 8294, 19, 8295, 20, 8296, 21, 8297, 22, 8298, 9, 
            8304, 3, 8305, 0, 8306, -1, 8308, 3, 8314, 4, 8316, 13, 
            8319, 0, 8320, 3, 8330, 4, 8332, 13, 8335, -1, 8336, 0, 
            8349, -1, 8352, 5, 8385, -1, 8400, 8, 8433, -1, 8448, 13, 
            8450, 0, 8451, 13, 8455, 0, 8456, 13, 8458, 0, 8468, 13, 
            8469, 0, 8470, 13, 8473, 0, 8478, 13, 8484, 0, 8485, 13, 
            8486, 0, 8487, 13, 8488, 0, 8489, 13, 8490, 0, 8494, 5, 
            8495, 0, 8506, 13, 8508, 0, 8512, 13, 8517, 0, 8522, 13, 
            8526, 0, 8528, 13, 8544, 0, 8585, 13, 8588, -1, 8592, 13, 
            8722, 4, 8723, 5, 8724, 13, 9014, 0, 9083, 13, 9109, 0, 
            9110, 13, 9258, -1, 9280, 13, 9291, -1, 9312, 13, 9352, 3, 
            9372, 0, 9450, 13, 9900, 0, 9901, 13, 10240, 0, 10496, 13, 
            11124, -1, 11126, 13, 11158, -1, 11159, 13, 11264, 0, 11493, 13, 
            11499, 0, 11503, 8, 11506, 0, 11508, -1, 11513, 13, 11520, 0, 
            11558, -1, 11559, 0, 11560, -1, 11565, 0, 11566, -1, 11568, 0, 
            11624, -1, 11631, 0, 11633, -1, 11647, 8, 11648, 0, 11671, -1, 
            11680, 0, 11687, -1, 11688, 0, 11695, -1, 11696, 0, 11703, -1, 
            11704, 0, 11711, -1, 11712, 0, 11719, -1, 11720, 0, 11727, -1, 
            11728, 0, 11735, -1, 11736, 0, 11743, -1, 11744, 8, 11776, 13, 
            11870, -1, 11904, 13, 11930, -1, 11931, 13, 12020, -1, 12032, 13, 
            12246, -1, 12272, 13, 12288, 12, 12289, 13, 12293, 0, 12296, 13, 
            12321, 0, 12330, 8, 12334, 0, 12336, 13, 12337, 0, 12342, 13, 
            12344, 0, 12349, 13, 12352, -1, 12353, 0, 12439, -1, 12441, 8, 
            12443, 13, 12445, 0, 12448, 13, 12449, 0, 12539, 13, 12540, 0, 
            12544, -1, 12549, 0, 12592, -1, 12593, 0, 12687, -1, 12688, 0, 
            12736, 13, 12774, -1, 12783, 13, 12784, 0, 12829, 13, 12831, -1, 
            12832, 0, 12880, 13, 12896, 0, 12924, 13, 12927, 0, 12977, 13, 
            12992, 0, 13004, 13, 13008, 0, 13175, 13, 13179, 0, 13278, 13, 
            13280, 0, 13311, 13, 13312, 0, 19904, 13, 19968, 0, 42125, -1, 
            42128, 13, 42183, -1, 42192, 0, 42509, 13, 42512, 0, 42540, -1, 
            42560, 0, 42607, 8, 42611, 13, 42612, 8, 42622, 13, 42624, 0, 
            42654, 8, 42656, 0, 42736, 8, 42738, 0, 42744, -1, 42752, 13, 
            42786, 0, 42888, 13, 42889, 0, 42958, -1, 42960, 0, 42962, -1, 
            42963, 0, 42964, -1, 42965, 0, 42973, -1, 42994, 0, 43010, 8, 
            43011, 0, 43014, 8, 43015, 0, 43019, 8, 43020, 0, 43045, 8, 
            43047, 0, 43048, 13, 43052, 8, 43053, -1, 43056, 0, 43064, 5, 
            43066, -1, 43072, 0, 43124, 13, 43128, -1, 43136, 0, 43204, 8, 
            43206, -1, 43214, 0, 43226, -1, 43232, 8, 43250, 0, 43263, 8, 
            43264, 0, 43302, 8, 43310, 0, 43335, 8, 43346, 0, 43348, -1, 
            43359, 0, 43389, -1, 43392, 8, 43395, 0, 43443, 8, 43444, 0, 
            43446, 8, 43450, 0, 43452, 8, 43454, 0, 43470, -1, 43471, 0, 
            43482, -1, 43486, 0, 43493, 8, 43494, 0, 43519, -1, 43520, 0, 
            43561, 8, 43567, 0, 43569, 8, 43571, 0, 43573, 8, 43575, -1, 
            43584, 0, 43587, 8, 43588, 0, 43596, 8, 43597, 0, 43598, -1, 
            43600, 0, 43610, -1, 43612, 0, 43644, 8, 43645, 0, 43696, 8, 
            43697, 0, 43698, 8, 43701, 0, 43703, 8, 43705, 0, 43710, 8, 
            43712, 0, 43713, 8, 43714, 0, 43715, -1, 43739, 0, 43756, 8, 
            43758, 0, 43766, 8, 43767, -1, 43777, 0, 43783, -1, 43785, 0, 
            43791, -1, 43793, 0, 43799, -1, 43808, 0, 43815, -1, 43816, 0, 
            43823, -1, 43824, 0, 43882, 13, 43884, -1, 43888, 0, 44005, 8, 
            44006, 0, 44008, 8, 44009, 0, 44013, 8, 44014, -1, 44016, 0, 
            44026, -1, 44032, 0, 55204, -1, 55216, 0, 55239, -1, 55243, 0, 
            55292, -1, 55296, 0, 64110, -1, 64112, 0, 64218, -1, 64256, 0, 
            64263, -1, 64275, 0, 64280, -1, 64285, 1, 64286, 8, 64287, 1, 
            64297, 4, 64298, 1, 64311, -1, 64312, 1, 64317, -1, 64318, 1, 
            64319, -1, 64320, 1, 64322, -1, 64323, 1, 64325, -1, 64326, 1, 
            64336, 2, 64451, -1, 64467, 2, 64830, 13, 64848, 2, 64912, -1, 
            64914, 2, 64968, -1, 64975, 13, 64976, -1, 65008, 2, 65021, 13, 
            65024, 8, 65040, 13, 65050, -1, 65056, 8, 65072, 13, 65104, 7, 
            65105, 13, 65106, 7, 65107, -1, 65108, 13, 65109, 7, 65110, 13, 
            65119, 5, 65120, 13, 65122, 4, 65124, 13, 65127, -1, 65128, 13, 
            65129, 5, 65131, 13, 65132, -1, 65136, 2, 65141, -1, 65142, 2, 
            65277, -1, 65279, 9, 65280, -1, 65281, 13, 65283, 5, 65286, 13, 
            65291, 4, 65292, 7, 65293, 4, 65294, 7, 65296, 3, 65306, 7, 
            65307, 13, 65313, 0, 65339, 13, 65345, 0, 65371, 13, 65382, 0, 
            65471, -1, 65474, 0, 65480, -1, 65482, 0, 65488, -1, 65490, 0, 
            65496, -1, 65498, 0, 65501, -1, 65504, 5, 65506, 13, 65509, 5, 
            65511, -1, 65512, 13, 65519, -1, 65529, 13, 65534, -1, 65536, 0, 
            65548, -1, 65549, 0, 65575, -1, 65576, 0, 65595, -1, 65596, 0, 
            65598, -1, 65599, 0, 65614, -1, 65616, 0, 65630, -1, 65664, 0, 
            65787, -1, 65792, 0, 65793, 13, 65794, 0, 65795, -1, 65799, 0, 
            65844, -1, 65847, 0, 65856, 13, 65933, 0, 65935, -1, 65936, 13, 
            65949, -1, 65952, 13, 65953, -1, 66000, 0, 66045, 8, 66046, -1, 
            66176, 0, 66205, -1, 66208, 0, 66257, -1, 66272, 8, 66273, 3, 
            66300, -1, 66304, 0, 66340, -1, 66349, 0, 66379, -1, 66384, 0, 
            66422, 8, 66427, -1, 66432, 0, 66462, -1, 66463, 0, 66500, -1, 
            66504, 0, 66518, -1, 66560, 0, 66718, -1, 66720, 0, 66730, -1, 
            66736, 0, 66772, -1, 66776, 0, 66812, -1, 66816, 0, 66856, -1, 
            66864, 0, 66916, -1, 66927, 0, 66939, -1, 66940, 0, 66955, -1, 
            66956, 0, 66963, -1, 66964, 0, 66966, -1, 66967, 0, 66978, -1, 
            66979, 0, 66994, -1, 66995, 0, 67002, -1, 67003, 0, 67005, -1, 
            67008, 0, 67060, -1, 67072, 0, 67383, -1, 67392, 0, 67414, -1, 
            67424, 0, 67432, -1, 67456, 0, 67462, -1, 67463, 0, 67505, -1, 
            67506, 0, 67515, -1, 67584, 1, 67590, -1, 67592, 1, 67593, -1, 
            67594, 1, 67638, -1, 67639, 1, 67641, -1, 67644, 1, 67645, -1, 
            67647, 1, 67670, -1, 67671, 1, 67743, -1, 67751, 1, 67760, -1, 
            67808, 1, 67827, -1, 67828, 1, 67830, -1, 67835, 1, 67868, -1, 
            67871, 13, 67872, 1, 67898, -1, 67903, 1, 67904, -1, 67968, 1, 
            68024, -1, 68028, 1, 68048, -1, 68050, 1, 68097, 8, 68100, -1, 
            68101, 8, 68103, -1, 68108, 8, 68112, 1, 68116, -1, 68117, 1, 
            68120, -1, 68121, 1, 68150, -1, 68152, 8, 68155, -1, 68159, 8, 
            68160, 1, 68169, -1, 68176, 1, 68185, -1, 68192, 1, 68256, -1, 
            68288, 1, 68325, 8, 68327, -1, 68331, 1, 68343, -1, 68352, 1, 
            68406, -1, 68409, 13, 68416, 1, 68438, -1, 68440, 1, 68467, -1, 
            68472, 1, 68498, -1, 68505, 1, 68509, -1, 68521, 1, 68528, -1, 
            68608, 1, 68681, -1, 68736, 1, 68787, -1, 68800, 1, 68851, -1, 
            68858, 1, 68864, 2, 68900, 8, 68904, -1, 68912, 6, 68922, -1, 
            68928, 6, 68938, 1, 68966, -1, 68969, 8, 68974, 13, 68975, 1, 
            68998, -1, 69006, 1, 69008, -1, 69216, 6, 69247, -1, 69248, 1, 
            69290, -1, 69291, 8, 69293, 1, 69294, -1, 69296, 1, 69298, -1, 
            69314, 2, 69317, -1, 69372, 8, 69376, 1, 69416, -1, 69424, 2, 
            69446, 8, 69457, 2, 69466, -1, 69488, 1, 69506, 8, 69510, 1, 
            69514, -1, 69552, 1, 69580, -1, 69600, 1, 69623, -1, 69632, 0, 
            69633, 8, 69634, 0, 69688, 8, 69703, 0, 69710, -1, 69714, 13, 
            69734, 0, 69744, 8, 69745, 0, 69747, 8, 69749, 0, 69750, -1, 
            69759, 8, 69762, 0, 69811, 8, 69815, 0, 69817, 8, 69819, 0, 
            69826, 8, 69827, -1, 69837, 0, 69838, -1, 69840, 0, 69865, -1, 
            69872, 0, 69882, -1, 69888, 8, 69891, 0, 69927, 8, 69932, 0, 
        };
    }

    private static int[] directionTable1() {
        return new int[] {
            69933, 8, 69941, -1, 69942, 0, 69960, -1, 69968, 0, 70003, 8, 
            70004, 0, 70007, -1, 70016, 8, 70018, 0, 70070, 8, 70079, 0, 
            70089, 8, 70093, 0, 70095, 8, 70096, 0, 70112, -1, 70113, 0, 
            70133, -1, 70144, 0, 70162, -1, 70163, 0, 70191, 8, 70194, 0, 
            70196, 8, 70197, 0, 70198, 8, 70200, 0, 70206, 8, 70207, 0, 
            70209, 8, 70210, -1, 70272, 0, 70279, -1, 70280, 0, 70281, -1, 
            70282, 0, 70286, -1, 70287, 0, 70302, -1, 70303, 0, 70314, -1, 
            70320, 0, 70367, 8, 70368, 0, 70371, 8, 70379, -1, 70384, 0, 
            70394, -1, 70400, 8, 70402, 0, 70404, -1, 70405, 0, 70413, -1, 
            70415, 0, 70417, -1, 70419, 0, 70441, -1, 70442, 0, 70449, -1, 
            70450, 0, 70452, -1, 70453, 0, 70458, -1, 70459, 8, 70461, 0, 
            70464, 8, 70465, 0, 70469, -1, 70471, 0, 70473, -1, 70475, 0, 
            70478, -1, 70480, 0, 70481, -1, 70487, 0, 70488, -1, 70493, 0, 
            70500, -1, 70502, 8, 70509, -1, 70512, 8, 70517, -1, 70528, 0, 
            70538, -1, 70539, 0, 70540, -1, 70542, 0, 70543, -1, 70544, 0, 
            70582, -1, 70583, 0, 70587, 8, 70593, -1, 70594, 0, 70595, -1, 
            70597, 0, 70598, -1, 70599, 0, 70603, -1, 70604, 0, 70606, 8, 
            70607, 0, 70608, 8, 70609, 0, 70610, 8, 70611, 0, 70614, -1, 
            70615, 0, 70617, -1, 70625, 8, 70627, -1, 70656, 0, 70712, 8, 
            70720, 0, 70722, 8, 70725, 0, 70726, 8, 70727, 0, 70748, -1, 
            70749, 0, 70750, 8, 70751, 0, 70754, -1, 70784, 0, 70835, 8, 
            70841, 0, 70842, 8, 70843, 0, 70847, 8, 70849, 0, 70850, 8, 
            70852, 0, 70856, -1, 70864, 0, 70874, -1, 71040, 0, 71090, 8, 
            71094, -1, 71096, 0, 71100, 8, 71102, 0, 71103, 8, 71105, 0, 
            71132, 8, 71134, -1, 71168, 0, 71219, 8, 71227, 0, 71229, 8, 
            71230, 0, 71231, 8, 71233, 0, 71237, -1, 71248, 0, 71258, -1, 
            71264, 13, 71277, -1, 71296, 0, 71339, 8, 71340, 0, 71341, 8, 
            71342, 0, 71344, 8, 71350, 0, 71351, 8, 71352, 0, 71354, -1, 
            71360, 0, 71370, -1, 71376, 0, 71396, -1, 71424, 0, 71451, -1, 
            71453, 8, 71454, 0, 71455, 8, 71456, 0, 71458, 8, 71462, 0, 
            71463, 8, 71468, -1, 71472, 0, 71495, -1, 71680, 0, 71727, 8, 
            71736, 0, 71737, 8, 71739, 0, 71740, -1, 71840, 0, 71923, -1, 
            71935, 0, 71943, -1, 71945, 0, 71946, -1, 71948, 0, 71956, -1, 
            71957, 0, 71959, -1, 71960, 0, 71990, -1, 71991, 0, 71993, -1, 
            71995, 8, 71997, 0, 71998, 8, 71999, 0, 72003, 8, 72004, 0, 
            72007, -1, 72016, 0, 72026, -1, 72096, 0, 72104, -1, 72106, 0, 
            72148, 8, 72152, -1, 72154, 8, 72156, 0, 72160, 8, 72161, 0, 
            72165, -1, 72192, 0, 72193, 8, 72199, 0, 72201, 8, 72203, 0, 
            72243, 8, 72249, 0, 72251, 8, 72255, 0, 72263, 8, 72264, -1, 
            72272, 0, 72273, 8, 72279, 0, 72281, 8, 72284, 0, 72330, 8, 
            72343, 0, 72344, 8, 72346, 0, 72355, -1, 72368, 0, 72441, -1, 
            72448, 0, 72458, -1, 72640, 0, 72674, -1, 72688, 0, 72698, -1, 
            72704, 0, 72713, -1, 72714, 0, 72752, 8, 72759, -1, 72760, 8, 
            72766, 0, 72774, -1, 72784, 0, 72813, -1, 72816, 0, 72848, -1, 
            72850, 8, 72872, -1, 72873, 0, 72874, 8, 72881, 0, 72882, 8, 
            72884, 0, 72885, 8, 72887, -1, 72960, 0, 72967, -1, 72968, 0, 
            72970, -1, 72971, 0, 73009, 8, 73015, -1, 73018, 8, 73019, -1, 
            73020, 8, 73022, -1, 73023, 8, 73030, 0, 73031, 8, 73032, -1, 
            73040, 0, 73050, -1, 73056, 0, 73062, -1, 73063, 0, 73065, -1, 
            73066, 0, 73103, -1, 73104, 8, 73106, -1, 73107, 0, 73109, 8, 
            73110, 0, 73111, 8, 73112, 0, 73113, -1, 73120, 0, 73130, -1, 
            73440, 0, 73459, 8, 73461, 0, 73465, -1, 73472, 8, 73474, 0, 
            73489, -1, 73490, 0, 73526, 8, 73531, -1, 73534, 0, 73536, 8, 
            73537, 0, 73538, 8, 73539, 0, 73562, 8, 73563, -1, 73648, 0, 
            73649, -1, 73664, 0, 73685, 13, 73693, 5, 73697, 13, 73714, -1, 
            73727, 0, 74650, -1, 74752, 0, 74863, -1, 74864, 0, 74869, -1, 
            74880, 0, 75076, -1, 77712, 0, 77811, -1, 77824, 0, 78912, 8, 
            78913, 0, 78919, 8, 78934, -1, 78944, 0, 82939, -1, 82944, 0, 
            83527, -1, 90368, 0, 90398, 8, 90410, 0, 90413, 8, 90416, 0, 
            90426, -1, 92160, 0, 92729, -1, 92736, 0, 92767, -1, 92768, 0, 
            92778, -1, 92782, 0, 92863, -1, 92864, 0, 92874, -1, 92880, 0, 
            92910, -1, 92912, 8, 92917, 0, 92918, -1, 92928, 0, 92976, 8, 
            92983, 0, 92998, -1, 93008, 0, 93018, -1, 93019, 0, 93026, -1, 
            93027, 0, 93048, -1, 93053, 0, 93072, -1, 93504, 0, 93562, -1, 
            93760, 0, 93851, -1, 93952, 0, 94027, -1, 94031, 8, 94032, 0, 
            94088, -1, 94095, 8, 94099, 0, 94112, -1, 94176, 0, 94178, 13, 
            94179, 0, 94180, 8, 94181, -1, 94192, 0, 94194, -1, 94208, 0, 
            100344, -1, 100352, 0, 101590, -1, 101631, 0, 101641, -1, 110576, 0, 
            110580, -1, 110581, 0, 110588, -1, 110589, 0, 110591, -1, 110592, 0, 
            110883, -1, 110898, 0, 110899, -1, 110928, 0, 110931, -1, 110933, 0, 
            110934, -1, 110948, 0, 110952, -1, 110960, 0, 111356, -1, 113664, 0, 
            113771, -1, 113776, 0, 113789, -1, 113792, 0, 113801, -1, 113808, 0, 
            113818, -1, 113820, 0, 113821, 8, 113823, 0, 113824, 9, 113828, -1, 
            117760, 13, 117974, 0, 118000, 3, 118010, -1, 118016, 13, 118452, -1, 
            118528, 8, 118574, -1, 118576, 8, 118599, -1, 118608, 0, 118724, -1, 
            118784, 0, 119030, -1, 119040, 0, 119079, -1, 119081, 0, 119143, 8, 
            119146, 0, 119155, 9, 119163, 8, 119171, 0, 119173, 8, 119180, 0, 
            119210, 8, 119214, 0, 119273, 13, 119275, -1, 119296, 13, 119362, 8, 
            119365, 13, 119366, -1, 119488, 0, 119508, -1, 119520, 0, 119540, -1, 
            119552, 13, 119639, -1, 119648, 0, 119673, -1, 119808, 0, 119893, -1, 
            119894, 0, 119965, -1, 119966, 0, 119968, -1, 119970, 0, 119971, -1, 
            119973, 0, 119975, -1, 119977, 0, 119981, -1, 119982, 0, 119994, -1, 
            119995, 0, 119996, -1, 119997, 0, 120004, -1, 120005, 0, 120070, -1, 
            120071, 0, 120075, -1, 120077, 0, 120085, -1, 120086, 0, 120093, -1, 
            120094, 0, 120122, -1, 120123, 0, 120127, -1, 120128, 0, 120133, -1, 
            120134, 0, 120135, -1, 120138, 0, 120145, -1, 120146, 0, 120486, -1, 
            120488, 0, 120513, 13, 120514, 0, 120539, 13, 120540, 0, 120571, 13, 
            120572, 0, 120597, 13, 120598, 0, 120629, 13, 120630, 0, 120655, 13, 
            120656, 0, 120687, 13, 120688, 0, 120713, 13, 120714, 0, 120745, 13, 
            120746, 0, 120771, 13, 120772, 0, 120780, -1, 120782, 3, 120832, 0, 
            121344, 8, 121399, 0, 121403, 8, 121453, 0, 121461, 8, 121462, 0, 
            121476, 8, 121477, 0, 121484, -1, 121499, 8, 121504, -1, 121505, 8, 
            121520, -1, 122624, 0, 122655, -1, 122661, 0, 122667, -1, 122880, 8, 
            122887, -1, 122888, 8, 122905, -1, 122907, 8, 122914, -1, 122915, 8, 
            122917, -1, 122918, 8, 122923, -1, 122928, 0, 122990, -1, 123023, 8, 
            123024, -1, 123136, 0, 123181, -1, 123184, 8, 123191, 0, 123198, -1, 
            123200, 0, 123210, -1, 123214, 0, 123216, -1, 123536, 0, 123566, 8, 
            123567, -1, 123584, 0, 123628, 8, 123632, 0, 123642, -1, 123647, 5, 
            123648, -1, 124112, 0, 124140, 8, 124144, 0, 124154, -1, 124368, 0, 
            124398, 8, 124400, 0, 124411, -1, 124415, 0, 124416, -1, 124896, 0, 
            124903, -1, 124904, 0, 124908, -1, 124909, 0, 124911, -1, 124912, 0, 
            124927, -1, 124928, 1, 125125, -1, 125127, 1, 125136, 8, 125143, -1, 
            125184, 1, 125252, 8, 125259, 1, 125260, -1, 125264, 1, 125274, -1, 
            125278, 1, 125280, -1, 126065, 2, 126133, -1, 126209, 2, 126270, -1, 
            126464, 2, 126468, -1, 126469, 2, 126496, -1, 126497, 2, 126499, -1, 
            126500, 2, 126501, -1, 126503, 2, 126504, -1, 126505, 2, 126515, -1, 
            126516, 2, 126520, -1, 126521, 2, 126522, -1, 126523, 2, 126524, -1, 
            126530, 2, 126531, -1, 126535, 2, 126536, -1, 126537, 2, 126538, -1, 
            126539, 2, 126540, -1, 126541, 2, 126544, -1, 126545, 2, 126547, -1, 
            126548, 2, 126549, -1, 126551, 2, 126552, -1, 126553, 2, 126554, -1, 
            126555, 2, 126556, -1, 126557, 2, 126558, -1, 126559, 2, 126560, -1, 
            126561, 2, 126563, -1, 126564, 2, 126565, -1, 126567, 2, 126571, -1, 
            126572, 2, 126579, -1, 126580, 2, 126584, -1, 126585, 2, 126589, -1, 
            126590, 2, 126591, -1, 126592, 2, 126602, -1, 126603, 2, 126620, -1, 
            126625, 2, 126628, -1, 126629, 2, 126634, -1, 126635, 2, 126652, -1, 
            126704, 13, 126706, -1, 126976, 13, 127020, -1, 127024, 13, 127124, -1, 
            127136, 13, 127151, -1, 127153, 13, 127168, -1, 127169, 13, 127184, -1, 
            127185, 13, 127222, -1, 127232, 3, 127243, 13, 127248, 0, 127279, 13, 
            127280, 0, 127338, 13, 127344, 0, 127405, 13, 127406, -1, 127462, 0, 
            127491, -1, 127504, 0, 127548, -1, 127552, 0, 127561, -1, 127568, 0, 
            127570, -1, 127584, 13, 127590, -1, 127744, 13, 128728, -1, 128732, 13, 
            128749, -1, 128752, 13, 128765, -1, 128768, 13, 128887, -1, 128891, 13, 
            128986, -1, 128992, 13, 129004, -1, 129008, 13, 129009, -1, 129024, 13, 
            129036, -1, 129040, 13, 129096, -1, 129104, 13, 129114, -1, 129120, 13, 
            129160, -1, 129168, 13, 129198, -1, 129200, 13, 129212, -1, 129216, 13, 
            129218, -1, 129280, 13, 129620, -1, 129632, 13, 129646, -1, 129648, 13, 
            129661, -1, 129664, 13, 129674, -1, 129679, 13, 129735, -1, 129742, 13, 
            129757, -1, 129759, 13, 129770, -1, 129776, 13, 129785, -1, 129792, 13, 
            129939, -1, 129940, 13, 130032, 3, 130042, -1, 131072, 0, 173792, -1, 
            173824, 0, 177978, -1, 177984, 0, 178206, -1, 178208, 0, 183970, -1, 
            183984, 0, 191457, -1, 191472, 0, 192094, -1, 194560, 0, 195102, -1, 
            196608, 0, 201547, -1, 201552, 0, 205744, -1, 917505, 9, 917506, -1, 
            917536, 9, 917632, -1, 917760, 8, 918000, -1, 983040, 0, 1048574, -1, 
            1048576, 0, 1114110, -1, 
        };
    }

    // Los rangos alfabeticos. NO es derivable de la categoria: Unicode marca ademas una propiedad Other_Alphabetic
    // que abarca 1495 code points mas.
    // 1514 enteros, 757 rangos.
    private static int[] alphabeticTable() {
        return alphabeticTable0();
    }

    private static int[] alphabeticTable0() {
        return new int[] {
            65, 90, 97, 122, 170, 170, 181, 181, 186, 186, 192, 214, 
            216, 246, 248, 705, 710, 721, 736, 740, 748, 748, 750, 750, 
            837, 837, 867, 884, 886, 887, 890, 893, 895, 895, 902, 902, 
            904, 906, 908, 908, 910, 929, 931, 1013, 1015, 1153, 1162, 1327, 
            1329, 1366, 1369, 1369, 1376, 1416, 1456, 1469, 1471, 1471, 1473, 1474, 
            1476, 1477, 1479, 1479, 1488, 1514, 1519, 1522, 1552, 1562, 1568, 1623, 
            1625, 1631, 1646, 1747, 1749, 1756, 1761, 1768, 1773, 1775, 1786, 1788, 
            1791, 1791, 1808, 1855, 1869, 1969, 1994, 2026, 2036, 2037, 2042, 2042, 
            2048, 2071, 2074, 2092, 2112, 2136, 2144, 2154, 2160, 2183, 2185, 2190, 
            2199, 2199, 2208, 2249, 2260, 2271, 2275, 2281, 2288, 2363, 2365, 2380, 
            2382, 2384, 2389, 2403, 2417, 2435, 2437, 2444, 2447, 2448, 2451, 2472, 
            2474, 2480, 2482, 2482, 2486, 2489, 2493, 2500, 2503, 2504, 2507, 2508, 
            2510, 2510, 2519, 2519, 2524, 2525, 2527, 2531, 2544, 2545, 2556, 2556, 
            2561, 2563, 2565, 2570, 2575, 2576, 2579, 2600, 2602, 2608, 2610, 2611, 
            2613, 2614, 2616, 2617, 2622, 2626, 2631, 2632, 2635, 2636, 2641, 2641, 
            2649, 2652, 2654, 2654, 2672, 2677, 2689, 2691, 2693, 2701, 2703, 2705, 
            2707, 2728, 2730, 2736, 2738, 2739, 2741, 2745, 2749, 2757, 2759, 2761, 
            2763, 2764, 2768, 2768, 2784, 2787, 2809, 2812, 2817, 2819, 2821, 2828, 
            2831, 2832, 2835, 2856, 2858, 2864, 2866, 2867, 2869, 2873, 2877, 2884, 
            2887, 2888, 2891, 2892, 2902, 2903, 2908, 2909, 2911, 2915, 2929, 2929, 
            2946, 2947, 2949, 2954, 2958, 2960, 2962, 2965, 2969, 2970, 2972, 2972, 
            2974, 2975, 2979, 2980, 2984, 2986, 2990, 3001, 3006, 3010, 3014, 3016, 
            3018, 3020, 3024, 3024, 3031, 3031, 3072, 3084, 3086, 3088, 3090, 3112, 
            3114, 3129, 3133, 3140, 3142, 3144, 3146, 3148, 3157, 3158, 3160, 3162, 
            3165, 3165, 3168, 3171, 3200, 3203, 3205, 3212, 3214, 3216, 3218, 3240, 
            3242, 3251, 3253, 3257, 3261, 3268, 3270, 3272, 3274, 3276, 3285, 3286, 
            3293, 3294, 3296, 3299, 3313, 3315, 3328, 3340, 3342, 3344, 3346, 3386, 
            3389, 3396, 3398, 3400, 3402, 3404, 3406, 3406, 3412, 3415, 3423, 3427, 
            3450, 3455, 3457, 3459, 3461, 3478, 3482, 3505, 3507, 3515, 3517, 3517, 
            3520, 3526, 3535, 3540, 3542, 3542, 3544, 3551, 3570, 3571, 3585, 3642, 
            3648, 3654, 3661, 3661, 3713, 3714, 3716, 3716, 3718, 3722, 3724, 3747, 
            3749, 3749, 3751, 3769, 3771, 3773, 3776, 3780, 3782, 3782, 3789, 3789, 
            3804, 3807, 3840, 3840, 3904, 3911, 3913, 3948, 3953, 3971, 3976, 3991, 
            3993, 4028, 4096, 4150, 4152, 4152, 4155, 4159, 4176, 4239, 4250, 4253, 
            4256, 4293, 4295, 4295, 4301, 4301, 4304, 4346, 4348, 4680, 4682, 4685, 
            4688, 4694, 4696, 4696, 4698, 4701, 4704, 4744, 4746, 4749, 4752, 4784, 
            4786, 4789, 4792, 4798, 4800, 4800, 4802, 4805, 4808, 4822, 4824, 4880, 
            4882, 4885, 4888, 4954, 4992, 5007, 5024, 5109, 5112, 5117, 5121, 5740, 
            5743, 5759, 5761, 5786, 5792, 5866, 5870, 5880, 5888, 5907, 5919, 5939, 
            5952, 5971, 5984, 5996, 5998, 6000, 6002, 6003, 6016, 6067, 6070, 6088, 
            6103, 6103, 6108, 6108, 6176, 6264, 6272, 6314, 6320, 6389, 6400, 6430, 
            6432, 6443, 6448, 6456, 6480, 6509, 6512, 6516, 6528, 6571, 6576, 6601, 
            6656, 6683, 6688, 6750, 6753, 6772, 6823, 6823, 6847, 6848, 6860, 6862, 
            6912, 6963, 6965, 6979, 6981, 6988, 7040, 7081, 7084, 7087, 7098, 7141, 
            7143, 7153, 7168, 7222, 7245, 7247, 7258, 7293, 7296, 7306, 7312, 7354, 
            7357, 7359, 7401, 7404, 7406, 7411, 7413, 7414, 7418, 7418, 7424, 7615, 
            7635, 7668, 7680, 7957, 7960, 7965, 7968, 8005, 8008, 8013, 8016, 8023, 
            8025, 8025, 8027, 8027, 8029, 8029, 8031, 8061, 8064, 8116, 8118, 8124, 
            8126, 8126, 8130, 8132, 8134, 8140, 8144, 8147, 8150, 8155, 8160, 8172, 
            8178, 8180, 8182, 8188, 8305, 8305, 8319, 8319, 8336, 8348, 8450, 8450, 
            8455, 8455, 8458, 8467, 8469, 8469, 8473, 8477, 8484, 8484, 8486, 8486, 
            8488, 8488, 8490, 8493, 8495, 8505, 8508, 8511, 8517, 8521, 8526, 8526, 
            8544, 8584, 9398, 9449, 11264, 11492, 11499, 11502, 11506, 11507, 11520, 11557, 
            11559, 11559, 11565, 11565, 11568, 11623, 11631, 11631, 11648, 11670, 11680, 11686, 
            11688, 11694, 11696, 11702, 11704, 11710, 11712, 11718, 11720, 11726, 11728, 11734, 
            11736, 11742, 11744, 11775, 11823, 11823, 12293, 12295, 12321, 12329, 12337, 12341, 
            12344, 12348, 12353, 12438, 12445, 12447, 12449, 12538, 12540, 12543, 12549, 12591, 
            12593, 12686, 12704, 12735, 12784, 12799, 13312, 19903, 19968, 42124, 42192, 42237, 
            42240, 42508, 42512, 42527, 42538, 42539, 42560, 42606, 42612, 42619, 42623, 42735, 
            42775, 42783, 42786, 42888, 42891, 42957, 42960, 42961, 42963, 42963, 42965, 42972, 
            42994, 43013, 43015, 43047, 43072, 43123, 43136, 43203, 43205, 43205, 43250, 43255, 
            43259, 43259, 43261, 43263, 43274, 43306, 43312, 43346, 43360, 43388, 43392, 43442, 
            43444, 43455, 43471, 43471, 43488, 43503, 43514, 43518, 43520, 43574, 43584, 43597, 
            43616, 43638, 43642, 43710, 43712, 43712, 43714, 43714, 43739, 43741, 43744, 43759, 
            43762, 43765, 43777, 43782, 43785, 43790, 43793, 43798, 43808, 43814, 43816, 43822, 
            43824, 43866, 43868, 43881, 43888, 44010, 44032, 55203, 55216, 55238, 55243, 55291, 
            63744, 64109, 64112, 64217, 64256, 64262, 64275, 64279, 64285, 64296, 64298, 64310, 
            64312, 64316, 64318, 64318, 64320, 64321, 64323, 64324, 64326, 64433, 64467, 64829, 
            64848, 64911, 64914, 64967, 65008, 65019, 65136, 65140, 65142, 65276, 65313, 65338, 
            65345, 65370, 65382, 65470, 65474, 65479, 65482, 65487, 65490, 65495, 65498, 65500, 
            65536, 65547, 65549, 65574, 65576, 65594, 65596, 65597, 65599, 65613, 65616, 65629, 
            65664, 65786, 65856, 65908, 66176, 66204, 66208, 66256, 66304, 66335, 66349, 66378, 
            66384, 66426, 66432, 66461, 66464, 66499, 66504, 66511, 66513, 66517, 66560, 66717, 
            66736, 66771, 66776, 66811, 66816, 66855, 66864, 66915, 66928, 66938, 66940, 66954, 
            66956, 66962, 66964, 66965, 66967, 66977, 66979, 66993, 66995, 67001, 67003, 67004, 
            67008, 67059, 67072, 67382, 67392, 67413, 67424, 67431, 67456, 67461, 67463, 67504, 
            67506, 67514, 67584, 67589, 67592, 67592, 67594, 67637, 67639, 67640, 67644, 67644, 
            67647, 67669, 67680, 67702, 67712, 67742, 67808, 67826, 67828, 67829, 67840, 67861, 
            67872, 67897, 67968, 68023, 68030, 68031, 68096, 68099, 68101, 68102, 68108, 68115, 
            68117, 68119, 68121, 68149, 68192, 68220, 68224, 68252, 68288, 68295, 68297, 68324, 
            68352, 68405, 68416, 68437, 68448, 68466, 68480, 68497, 68608, 68680, 68736, 68786, 
            68800, 68850, 68864, 68903, 68938, 68965, 68969, 68969, 68975, 68997, 69248, 69289, 
            69291, 69292, 69296, 69297, 69314, 69316, 69372, 69372, 69376, 69404, 69415, 69415, 
            69424, 69445, 69488, 69505, 69552, 69572, 69600, 69622, 69632, 69701, 69745, 69749, 
            69760, 69816, 69826, 69826, 69840, 69864, 69888, 69938, 69956, 69959, 69968, 70002, 
            70006, 70006, 70016, 70079, 70081, 70084, 70094, 70095, 70106, 70106, 70108, 70108, 
            70144, 70161, 70163, 70196, 70199, 70199, 70206, 70209, 70272, 70278, 70280, 70280, 
            70282, 70285, 70287, 70301, 70303, 70312, 70320, 70376, 70400, 70403, 70405, 70412, 
            70415, 70416, 70419, 70440, 70442, 70448, 70450, 70451, 70453, 70457, 70461, 70468, 
            70471, 70472, 70475, 70476, 70480, 70480, 70487, 70487, 70493, 70499, 70528, 70537, 
            70539, 70539, 70542, 70542, 70544, 70581, 70583, 70592, 70594, 70594, 70597, 70597, 
            70599, 70602, 70604, 70605, 70609, 70609, 70611, 70611, 70656, 70721, 70723, 70725, 
            70727, 70730, 70751, 70753, 70784, 70849, 70852, 70853, 70855, 70855, 71040, 71093, 
            71096, 71102, 71128, 71133, 71168, 71230, 71232, 71232, 71236, 71236, 71296, 71349, 
            71352, 71352, 71424, 71450, 71453, 71466, 71488, 71494, 71680, 71736, 71840, 71903, 
            71935, 71942, 71945, 71945, 71948, 71955, 71957, 71958, 71960, 71989, 71991, 71992, 
            71995, 71996, 71999, 72002, 72096, 72103, 72106, 72151, 72154, 72159, 72161, 72161, 
            72163, 72164, 72192, 72242, 72245, 72254, 72272, 72343, 72349, 72349, 72368, 72440, 
            72640, 72672, 72704, 72712, 72714, 72758, 72760, 72766, 72768, 72768, 72818, 72847, 
            72850, 72871, 72873, 72886, 72960, 72966, 72968, 72969, 72971, 73014, 73018, 73018, 
            73020, 73021, 73023, 73025, 73027, 73027, 73030, 73031, 73056, 73061, 73063, 73064, 
            73066, 73102, 73104, 73105, 73107, 73110, 73112, 73112, 73440, 73462, 73472, 73488, 
            73490, 73530, 73534, 73536, 73648, 73648, 73728, 74649, 74752, 74862, 74880, 75075, 
            77712, 77808, 77824, 78895, 78913, 78918, 78944, 82938, 82944, 83526, 90368, 90414, 
            92160, 92728, 92736, 92766, 92784, 92862, 92880, 92909, 92928, 92975, 92992, 92995, 
            93027, 93047, 93053, 93071, 93504, 93548, 93760, 93823, 93952, 94026, 94031, 94087, 
            94095, 94111, 94176, 94177, 94179, 94179, 94192, 94193, 94208, 100343, 100352, 101589, 
            101631, 101640, 110576, 110579, 110581, 110587, 110589, 110590, 110592, 110882, 110898, 110898, 
            110928, 110930, 110933, 110933, 110948, 110951, 110960, 111355, 113664, 113770, 113776, 113788, 
            113792, 113800, 113808, 113817, 113822, 113822, 119808, 119892, 119894, 119964, 119966, 119967, 
            119970, 119970, 119973, 119974, 119977, 119980, 119982, 119993, 119995, 119995, 119997, 120003, 
            120005, 120069, 120071, 120074, 120077, 120084, 120086, 120092, 120094, 120121, 120123, 120126, 
            120128, 120132, 120134, 120134, 120138, 120144, 120146, 120485, 120488, 120512, 120514, 120538, 
            120540, 120570, 120572, 120596, 120598, 120628, 120630, 120654, 120656, 120686, 120688, 120712, 
            120714, 120744, 120746, 120770, 120772, 120779, 122624, 122654, 122661, 122666, 122880, 122886, 
            122888, 122904, 122907, 122913, 122915, 122916, 122918, 122922, 122928, 122989, 123023, 123023, 
            123136, 123180, 123191, 123197, 123214, 123214, 123536, 123565, 123584, 123627, 124112, 124139, 
            124368, 124397, 124400, 124400, 124896, 124902, 124904, 124907, 124909, 124910, 124912, 124926, 
            124928, 125124, 125184, 125251, 125255, 125255, 125259, 125259, 126464, 126467, 126469, 126495, 
            126497, 126498, 126500, 126500, 126503, 126503, 126505, 126514, 126516, 126519, 126521, 126521, 
            126523, 126523, 126530, 126530, 126535, 126535, 126537, 126537, 126539, 126539, 126541, 126543, 
            126545, 126546, 126548, 126548, 126551, 126551, 126553, 126553, 126555, 126555, 126557, 126557, 
            126559, 126559, 126561, 126562, 126564, 126564, 126567, 126570, 126572, 126578, 126580, 126583, 
            126585, 126588, 126590, 126590, 126592, 126601, 126603, 126619, 126625, 126627, 126629, 126633, 
            126635, 126651, 127280, 127305, 127312, 127337, 127344, 127369, 131072, 173791, 173824, 177977, 
            177984, 178205, 178208, 183969, 183984, 191456, 191472, 192093, 194560, 195101, 196608, 201546, 
            201552, 205743, 
        };
    }

    // Los rangos de caracteres con espejo bidireccional.
    // 228 enteros, 114 rangos.
    private static int[] mirroredTable() {
        return mirroredTable0();
    }

    private static int[] mirroredTable0() {
        return new int[] {
            40, 41, 60, 60, 62, 62, 91, 91, 93, 93, 123, 123, 
            125, 125, 171, 171, 187, 187, 3898, 3901, 5787, 5788, 8249, 8250, 
            8261, 8262, 8317, 8318, 8333, 8334, 8512, 8512, 8705, 8708, 8712, 8717, 
            8721, 8721, 8725, 8726, 8730, 8733, 8735, 8738, 8740, 8740, 8742, 8742, 
            8747, 8755, 8761, 8761, 8763, 8780, 8786, 8789, 8799, 8800, 8802, 8802, 
            8804, 8811, 8813, 8844, 8847, 8850, 8856, 8856, 8866, 8867, 8870, 8888, 
            8894, 8895, 8905, 8909, 8912, 8913, 8918, 8941, 8944, 8959, 8968, 8971, 
            8992, 8993, 9001, 9002, 10088, 10101, 10176, 10176, 10179, 10182, 10184, 10185, 
            10187, 10189, 10195, 10198, 10204, 10206, 10210, 10223, 10627, 10648, 10651, 10656, 
            10658, 10671, 10680, 10680, 10688, 10693, 10697, 10697, 10702, 10706, 10708, 10709, 
            10712, 10716, 10721, 10721, 10723, 10725, 10728, 10729, 10740, 10745, 10748, 10749, 
            10762, 10780, 10782, 10785, 10788, 10788, 10790, 10790, 10793, 10793, 10795, 10798, 
            10804, 10805, 10812, 10814, 10839, 10840, 10852, 10853, 10858, 10861, 10863, 10864, 
            10867, 10868, 10873, 10915, 10918, 10925, 10927, 10966, 10972, 10972, 10974, 10974, 
            10978, 10982, 10988, 10990, 10995, 10995, 10999, 11003, 11005, 11005, 11262, 11262, 
            11778, 11781, 11785, 11786, 11788, 11789, 11804, 11805, 11808, 11817, 11861, 11868, 
            12296, 12305, 12308, 12315, 65113, 65118, 65124, 65125, 65288, 65289, 65308, 65308, 
            65310, 65310, 65339, 65339, 65341, 65341, 65371, 65371, 65373, 65373, 65375, 65376, 
            65378, 65379, 120539, 120539, 120597, 120597, 120655, 120655, 120713, 120713, 120771, 120771, 
        };
    }

    // Los rangos ideograficos.
    // 42 enteros, 21 rangos.
    private static int[] ideographicTable() {
        return ideographicTable0();
    }

    private static int[] ideographicTable0() {
        return new int[] {
            12294, 12295, 12321, 12329, 12344, 12346, 13312, 19903, 19968, 40959, 63744, 64109, 
            64112, 64217, 94180, 94180, 94208, 100343, 100352, 101589, 101631, 101640, 110960, 111355, 
            131072, 173791, 173824, 177977, 177984, 178205, 178208, 183969, 183984, 191456, 191472, 192093, 
            194560, 195101, 196608, 201546, 201552, 205743, 
        };
    }

    // Los rangos de Emoji.
    // 300 enteros, 150 rangos.
    private static int[] emojiTable() {
        return emojiTable0();
    }

    private static int[] emojiTable0() {
        return new int[] {
            35, 35, 42, 42, 48, 57, 169, 169, 174, 174, 8252, 8252, 
            8265, 8265, 8482, 8482, 8505, 8505, 8596, 8601, 8617, 8618, 8986, 8987, 
            9000, 9000, 9167, 9167, 9193, 9203, 9208, 9210, 9410, 9410, 9642, 9643, 
            9654, 9654, 9664, 9664, 9723, 9726, 9728, 9732, 9742, 9742, 9745, 9745, 
            9748, 9749, 9752, 9752, 9757, 9757, 9760, 9760, 9762, 9763, 9766, 9766, 
            9770, 9770, 9774, 9775, 9784, 9786, 9792, 9792, 9794, 9794, 9800, 9811, 
            9823, 9824, 9827, 9827, 9829, 9830, 9832, 9832, 9851, 9851, 9854, 9855, 
            9874, 9879, 9881, 9881, 9883, 9884, 9888, 9889, 9895, 9895, 9898, 9899, 
            9904, 9905, 9917, 9918, 9924, 9925, 9928, 9928, 9934, 9935, 9937, 9937, 
            9939, 9940, 9961, 9962, 9968, 9973, 9975, 9978, 9981, 9981, 9986, 9986, 
            9989, 9989, 9992, 9997, 9999, 9999, 10002, 10002, 10004, 10004, 10006, 10006, 
            10013, 10013, 10017, 10017, 10024, 10024, 10035, 10036, 10052, 10052, 10055, 10055, 
            10060, 10060, 10062, 10062, 10067, 10069, 10071, 10071, 10083, 10084, 10133, 10135, 
            10145, 10145, 10160, 10160, 10175, 10175, 10548, 10549, 11013, 11015, 11035, 11036, 
            11088, 11088, 11093, 11093, 12336, 12336, 12349, 12349, 12951, 12951, 12953, 12953, 
            126980, 126980, 127183, 127183, 127344, 127345, 127358, 127359, 127374, 127374, 127377, 127386, 
            127462, 127487, 127489, 127490, 127514, 127514, 127535, 127535, 127538, 127546, 127568, 127569, 
            127744, 127777, 127780, 127891, 127894, 127895, 127897, 127899, 127902, 127984, 127987, 127989, 
            127991, 128253, 128255, 128317, 128329, 128334, 128336, 128359, 128367, 128368, 128371, 128378, 
            128391, 128391, 128394, 128397, 128400, 128400, 128405, 128406, 128420, 128421, 128424, 128424, 
            128433, 128434, 128444, 128444, 128450, 128452, 128465, 128467, 128476, 128478, 128481, 128481, 
            128483, 128483, 128488, 128488, 128495, 128495, 128499, 128499, 128506, 128591, 128640, 128709, 
            128715, 128722, 128725, 128727, 128732, 128741, 128745, 128745, 128747, 128748, 128752, 128752, 
            128755, 128764, 128992, 129003, 129008, 129008, 129292, 129338, 129340, 129349, 129351, 129535, 
            129648, 129660, 129664, 129673, 129679, 129734, 129742, 129756, 129759, 129769, 129776, 129784, 
        };
    }

    // Los rangos de Emoji_Component.
    // 20 enteros, 10 rangos.
    private static int[] emojiComponentTable() {
        return emojiComponentTable0();
    }

    private static int[] emojiComponentTable0() {
        return new int[] {
            35, 35, 42, 42, 48, 57, 8205, 8205, 8419, 8419, 65039, 65039, 
            127462, 127487, 127995, 127999, 129456, 129459, 917536, 917631, 
        };
    }

    // Los rangos de Emoji_Modifier.
    // 2 enteros, 1 rangos.
    private static int[] emojiModifierTable() {
        return emojiModifierTable0();
    }

    private static int[] emojiModifierTable0() {
        return new int[] {
            127995, 127999, 
        };
    }

    // Los rangos de Emoji_Modifier_Base.
    // 80 enteros, 40 rangos.
    private static int[] emojiModifierBaseTable() {
        return emojiModifierBaseTable0();
    }

    private static int[] emojiModifierBaseTable0() {
        return new int[] {
            9757, 9757, 9977, 9977, 9994, 9997, 127877, 127877, 127938, 127940, 127943, 127943, 
            127946, 127948, 128066, 128067, 128070, 128080, 128102, 128120, 128124, 128124, 128129, 128131, 
            128133, 128135, 128143, 128143, 128145, 128145, 128170, 128170, 128372, 128373, 128378, 128378, 
            128400, 128400, 128405, 128406, 128581, 128583, 128587, 128591, 128675, 128675, 128692, 128694, 
            128704, 128704, 128716, 128716, 129292, 129292, 129295, 129295, 129304, 129311, 129318, 129318, 
            129328, 129337, 129340, 129342, 129399, 129399, 129461, 129462, 129464, 129465, 129467, 129467, 
            129485, 129487, 129489, 129501, 129731, 129733, 129776, 129784, 
        };
    }

    // Los rangos de Emoji_Presentation.
    // 160 enteros, 80 rangos.
    private static int[] emojiPresentationTable() {
        return emojiPresentationTable0();
    }

    private static int[] emojiPresentationTable0() {
        return new int[] {
            8986, 8987, 9193, 9196, 9200, 9200, 9203, 9203, 9725, 9726, 9748, 9749, 
            9800, 9811, 9855, 9855, 9875, 9875, 9889, 9889, 9898, 9899, 9917, 9918, 
            9924, 9925, 9934, 9934, 9940, 9940, 9962, 9962, 9970, 9971, 9973, 9973, 
            9978, 9978, 9981, 9981, 9989, 9989, 9994, 9995, 10024, 10024, 10060, 10060, 
            10062, 10062, 10067, 10069, 10071, 10071, 10133, 10135, 10160, 10160, 10175, 10175, 
            11035, 11036, 11088, 11088, 11093, 11093, 126980, 126980, 127183, 127183, 127374, 127374, 
            127377, 127386, 127462, 127487, 127489, 127489, 127514, 127514, 127535, 127535, 127538, 127542, 
            127544, 127546, 127568, 127569, 127744, 127776, 127789, 127797, 127799, 127868, 127870, 127891, 
            127904, 127946, 127951, 127955, 127968, 127984, 127988, 127988, 127992, 128062, 128064, 128064, 
            128066, 128252, 128255, 128317, 128331, 128334, 128336, 128359, 128378, 128378, 128405, 128406, 
            128420, 128420, 128507, 128591, 128640, 128709, 128716, 128716, 128720, 128722, 128725, 128727, 
            128732, 128735, 128747, 128748, 128756, 128764, 128992, 129003, 129008, 129008, 129292, 129338, 
            129340, 129349, 129351, 129535, 129648, 129660, 129664, 129673, 129679, 129734, 129742, 129756, 
            129759, 129769, 129776, 129784, 
        };
    }

    // Los rangos de Extended_Pictographic.
    // 156 enteros, 78 rangos.
    private static int[] pictographicTable() {
        return pictographicTable0();
    }

    private static int[] pictographicTable0() {
        return new int[] {
            169, 169, 174, 174, 8252, 8252, 8265, 8265, 8482, 8482, 8505, 8505, 
            8596, 8601, 8617, 8618, 8986, 8987, 9000, 9000, 9096, 9096, 9167, 9167, 
            9193, 9203, 9208, 9210, 9410, 9410, 9642, 9643, 9654, 9654, 9664, 9664, 
            9723, 9726, 9728, 9733, 9735, 9746, 9748, 9861, 9872, 9989, 9992, 10002, 
            10004, 10004, 10006, 10006, 10013, 10013, 10017, 10017, 10024, 10024, 10035, 10036, 
            10052, 10052, 10055, 10055, 10060, 10060, 10062, 10062, 10067, 10069, 10071, 10071, 
            10083, 10087, 10133, 10135, 10145, 10145, 10160, 10160, 10175, 10175, 10548, 10549, 
            11013, 11015, 11035, 11036, 11088, 11088, 11093, 11093, 12336, 12336, 12349, 12349, 
            12951, 12951, 12953, 12953, 126976, 127231, 127245, 127247, 127279, 127279, 127340, 127345, 
            127358, 127359, 127374, 127374, 127377, 127386, 127405, 127461, 127489, 127503, 127514, 127514, 
            127535, 127535, 127538, 127546, 127548, 127551, 127561, 127994, 128000, 128317, 128326, 128591, 
            128640, 128767, 128884, 128895, 128981, 129023, 129036, 129039, 129096, 129103, 129114, 129119, 
            129160, 129167, 129198, 129279, 129292, 129338, 129340, 129349, 129351, 129791, 130048, 131069, 
        };
    }

    // Other_ID_Start: los que son inicio de identificador Unicode sin ser letra ni numero-letra.
    private static final int[] OTHER_ID_START = new int[] {6277, 6278, 8472, 8494, 12443, 12444, };

    // Other_ID_Continue: los que continuan un identificador Unicode sin entrar en las categorias.
    private static final int[] OTHER_ID_CONTINUE = new int[] {183, 903, 4969, 4970, 4971, 4972, 4973, 4974, 4975, 4976, 4977, 6618, 8472, 8494, 12443, 12444, 12539, 65381, };

    // ---- nombres Unicode: getName / codePointOf ----
    //
    // Los dos dependen de la BASE DE NOMBRES Unicode (`getName('A')` == "LATIN CAPITAL LETTER A"),
    // que el JDK envia como un recurso aparte (`uniName.dat`, ~150.000 entradas) y NO se calcula.
    // KajiLibrary no lleva esa tabla (ver el @implNote de la clase), asi que la busqueda del nombre
    // en si no esta disponible y lanza UnsupportedOperationException. Lo que SI es fiel y no necesita
    // tabla se conserva: la validacion del code point en getName y el saneo del argumento en
    // codePointOf (incluida la NPE si es null), de modo que la superficie observable coincide con la
    // del JDK hasta el punto exacto donde haria falta la tabla.

    /**
     * The Unicode name of the character {@code codePoint}, e.g. {@code "LATIN CAPITAL LETTER A"}.
     *
     * @throws IllegalArgumentException if {@code codePoint} is not a valid Unicode code point
     * @throws UnsupportedOperationException for every valid code point: KajiLibrary does not carry
     *         the Unicode name table ({@code uniName.dat}) the lookup needs
     */
    public static String getName(int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("Not a valid Unicode code point: 0x"
                    + Integer.toHexString(codePoint).toUpperCase());
        }
        throw new UnsupportedOperationException(
                "la tabla de nombres Unicode (uniName.dat) no esta en KajiLibrary");
    }

    /**
     * The code point of the character whose Unicode name is {@code name} — the inverse of
     * {@link #getName(int)}.
     *
     * @throws NullPointerException if {@code name} is null
     * @throws UnsupportedOperationException KajiLibrary does not carry the Unicode name table
     *         ({@code uniName.dat}) the lookup needs
     */
    public static int codePointOf(String name) {
        // Fiel hasta donde no hace falta la tabla: NPE si es null, y el mismo saneo que el JDK.
        name = name.trim().toUpperCase();
        throw new UnsupportedOperationException(
                "la tabla de nombres Unicode (uniName.dat) no esta en KajiLibrary");
    }


    /**
     * A named range of characters, compared by identity.
     *
     * <p>The point of the class is what it does <em>not</em> do: {@code equals} is final and is
     * reference equality, and {@code hashCode} is the identity hash. Two subsets with the same name
     * are two different subsets. That is deliberate — a subset is a singleton defined by whoever
     * declares it, and letting two of them compare equal because their names collide would silently
     * merge unrelated ranges.
     *
     * <p>Subclasses supply the constants; this class carries only the name, for {@code toString}.
     */
    public static class Subset {

        private final String name;

        /**
         * Creates a subset with the given name.
         *
         * @throws NullPointerException if the name is {@code null}
         */
        protected Subset(String name) {
            if (name == null) {
                throw new NullPointerException("name");
            }
            this.name = name;
        }

        /** Reference equality; final, so no subclass can loosen it. */
        public final boolean equals(Object obj) {
            return this == obj;
        }

        /** The identity hash, to match {@link #equals}. */
        public final int hashCode() {
            return super.hashCode();
        }

        /** The name this subset was created with. */
        public final String toString() {
            return this.name;
        }
    }
}
