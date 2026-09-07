package java.awt.font;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

/**
 * Replaces the Latin digits with another script's.
 *
 * <p>The number twelve is written `12` in every script in the world: same figures, same order, same
 * positional value. What changes are the ten figures' **drawings**. This class makes exactly that
 * substitution, character by character, touching nothing else.
 *
 * <p>And there is the point: it is an operation of **presentation**, not of content. The text goes
 * on saying twelve; it only looks different. That is why it is applied on drawing and not on saving.
 *
 * <p>There are two modes and the difference matters. The **fixed** one converts every digit to the
 * chosen script. The **contextual** one looks at the text surrounding each digit and uses the script
 * of what came before, which is what is needed in a document mixing languages: the numbers in the
 * Arabic part come out Arabic-Indic and those in the English part stay Latin, without marking them
 * up by hand.
 *
 * <p>The context is followed with the **strong direction** characters —letters, not punctuation nor
 * spaces—, because they are the ones that identify a script unambiguously. A digit appearing before
 * any letter uses the default context declared when the shaper was asked for.
 *
 * <p>Ethiopic is the exception that proves the rule: its system of figures **has no zero**, so the
 * `0` is left as it stands instead of turning into something that does not exist.
 */
public final class NumericShaper implements Serializable {

    private static final long serialVersionUID = -8022764705923730308L;

    /** The usual Latin digits. */
    public static final int EUROPEAN = 1 << 0;

    /** The Arabic-Indic digits. */
    public static final int ARABIC = 1 << 1;

    /** The Eastern Arabic-Indic ones, of Persia and Urdu. */
    public static final int EASTERN_ARABIC = 1 << 2;

    /** Devanagari. */
    public static final int DEVANAGARI = 1 << 3;

    /** Bengali. */
    public static final int BENGALI = 1 << 4;

    /** Gurmukhi. */
    public static final int GURMUKHI = 1 << 5;

    /** Gujarati. */
    public static final int GUJARATI = 1 << 6;

    /** Oriya. */
    public static final int ORIYA = 1 << 7;

    /** Tamil. */
    public static final int TAMIL = 1 << 8;

    /** Telugu. */
    public static final int TELUGU = 1 << 9;

    /** Kannada. */
    public static final int KANNADA = 1 << 10;

    /** Malayalam. */
    public static final int MALAYALAM = 1 << 11;

    /** Thai. */
    public static final int THAI = 1 << 12;

    /** Lao. */
    public static final int LAO = 1 << 13;

    /** Tibetan. */
    public static final int TIBETAN = 1 << 14;

    /** Burmese. */
    public static final int MYANMAR = 1 << 15;

    /** Ethiopic, which **has no zero**. */
    public static final int ETHIOPIC = 1 << 16;

    /** Khmer. */
    public static final int KHMER = 1 << 17;

    /** Mongolian. */
    public static final int MONGOLIAN = 1 << 18;

    /** Every script the constants above can name. */
    public static final int ALL_RANGES = 0x7FFFF;

    /** Each script's zero digit, in the constants' order. */
    private static final char[] BASES = {
        '\u0030',
        '\u0660',
        '\u06F0',
        '\u0966',
        '\u09E6',
        '\u0A66',
        '\u0AE6',
        '\u0B66',
        '\u0BE6',
        '\u0C66',
        '\u0CE6',
        '\u0D66',
        '\u0E50',
        '\u0ED0',
        '\u0F20',
        '\u1040',
        '\u1369',
        '\u17E0',
        '\u1810'
    };

    /** Where each script's block begins, for recognizing the context. */
    private static final char[] CTX_LO = { '\u0000', '\u0600', '\u0600', '\u0900', '\u0980', '\u0A00', '\u0A80', '\u0B00', '\u0B80', '\u0C00', '\u0C80', '\u0D00', '\u0E00', '\u0E80', '\u0F00', '\u1000', '\u1200', '\u1780', '\u1800' };

    /** Where it ends. */
    private static final char[] CTX_HI = { '\u0300', '\u0780', '\u0780', '\u0980', '\u0A00', '\u0A80', '\u0B00', '\u0B80', '\u0C00', '\u0C80', '\u0D00', '\u0D80', '\u0E80', '\u0F00', '\u1000', '\u10A0', '\u1380', '\u1800', '\u18B0' };

    /** The Ethiopic script's position, the one with no zero. */
    private static final int ETHIOPIC_KEY = 16;

    /** The Latin script's position. */
    private static final int EUROPEAN_KEY = 0;

    /**
     * The scripts that can be asked for, as an enumeration.
     *
     * <p>It is the modern way of naming the same thing as the `int` constants above, and it admits
     * a good many more scripts: the constants are bits of a mask and they ran out of room.
     */
    public static enum Range {

        /** The usual Latin digits. */
        EUROPEAN('\u0030', '\u0000', '\u0300'),

        /** The Arabic-Indic digits. */
        ARABIC('\u0660', '\u0600', '\u0780'),

        /** The Eastern Arabic-Indic ones, of Persia and Urdu. */
        EASTERN_ARABIC('\u06F0', '\u0600', '\u0780'),

        /** Devanagari. */
        DEVANAGARI('\u0966', '\u0900', '\u0980'),

        /** Bengali. */
        BENGALI('\u09E6', '\u0980', '\u0A00'),

        /** Gurmukhi. */
        GURMUKHI('\u0A66', '\u0A00', '\u0A80'),

        /** Gujarati. */
        GUJARATI('\u0AE6', '\u0A80', '\u0B00'),

        /** Oriya. */
        ORIYA('\u0B66', '\u0B00', '\u0B80'),

        /** Tamil. */
        TAMIL('\u0BE6', '\u0B80', '\u0C00'),

        /** Telugu. */
        TELUGU('\u0C66', '\u0C00', '\u0C80'),

        /** Kannada. */
        KANNADA('\u0CE6', '\u0C80', '\u0D00'),

        /** Malayalam. */
        MALAYALAM('\u0D66', '\u0D00', '\u0D80'),

        /** Thai. */
        THAI('\u0E50', '\u0E00', '\u0E80'),

        /** Lao. */
        LAO('\u0ED0', '\u0E80', '\u0F00'),

        /** Tibetan. */
        TIBETAN('\u0F20', '\u0F00', '\u1000'),

        /** Burmese. */
        MYANMAR('\u1040', '\u1000', '\u10A0'),

        /** Ethiopic, which **has no zero**. */
        ETHIOPIC('\u1369', '\u1200', '\u1380'),

        /** Khmer. */
        KHMER('\u17E0', '\u1780', '\u1800'),

        /** Mongolian. */
        MONGOLIAN('\u1810', '\u1800', '\u18B0'),

        /** N’Ko. */
        NKO('\u07C0', '\u07C0', '\u0800'),

        /** Burmese, Shan variant. */
        MYANMAR_SHAN('\u1090', '\u1000', '\u10A0'),

        /** Limbu. */
        LIMBU('\u1946', '\u1900', '\u1950'),

        /** New Tai Lue. */
        NEW_TAI_LUE('\u19D0', '\u1980', '\u19E0'),

        /** Balinese. */
        BALINESE('\u1B50', '\u1B00', '\u1B80'),

        /** Sundanese. */
        SUNDANESE('\u1BB0', '\u1B80', '\u1BC0'),

        /** Lepcha. */
        LEPCHA('\u1C40', '\u1C00', '\u1C50'),

        /** Ol Chiki. */
        OL_CHIKI('\u1C50', '\u1C50', '\u1C80'),

        /** Vai. */
        VAI('\uA620', '\uA500', '\uA640'),

        /** Saurashtra. */
        SAURASHTRA('\uA8D0', '\uA880', '\uA8E0'),

        /** Kayah Li. */
        KAYAH_LI('\uA900', '\uA900', '\uA930'),

        /** Cham. */
        CHAM('\uAA50', '\uAA00', '\uAA60'),

        /** Tai Tham, Hora digits. */
        TAI_THAM_HORA('\u1A80', '\u1A20', '\u1AB0'),

        /** Tai Tham, Tham digits. */
        TAI_THAM_THAM('\u1A90', '\u1A20', '\u1AB0'),

        /** Javanese. */
        JAVANESE('\uA9D0', '\uA980', '\uA9E0'),

        /** Meetei Mayek. */
        MEETEI_MAYEK('\uABF0', '\uABC0', '\uAC00'),

        /** Sinhala. */
        SINHALA('\u0DE6', '\u0D80', '\u0E00'),

        /** Burmese, Tai Laing variant. */
        MYANMAR_TAI_LAING('\uA9F0', '\uA9E0', '\uAA00');

        private final char base;
        private final char start;
        private final char end;

        /** With the script's zero digit and the block it lives in. */
        private Range(char base, char start, char end) {
            this.base = base;
            this.start = start;
            this.end = end;
        }

        /** This script's zero digit. */
        char getNumericBase() {
            return this.base;
        }

        /** Whether that character belongs to this script. */
        boolean contains(char c) {
            return c >= this.start && c < this.end;
        }

        /** Whether this script has no figure for zero. */
        boolean noZero() {
            return this == ETHIOPIC;
        }
    }

    /** The mask of scripts, if it was asked for with `int` constants. */
    private final int mask;

    /** The set of scripts, if it was asked for with `Range`. */
    private final Set<Range> rangeSet;

    /** Which script to use before any context is found. */
    private final int key;

    /** The same, if it was asked for with `Range`. */
    private final Range shapingRange;

    /** Whether it looks at the surrounding text. */
    private final boolean contextual;

    /** The common constructor; it is reached through the factories. */
    private NumericShaper(int mask, Set<Range> rangeSet, int key, Range shapingRange,
            boolean contextual) {
        this.mask = mask;
        this.rangeSet = rangeSet;
        this.key = key;
        this.shapingRange = shapingRange;
        this.contextual = contextual;
    }

    /** The position of the mask's only script, or -1 if there is not exactly one. */
    private static int singleKey(int singleRange) {
        int key = -1;
        for (int i = 0; i < BASES.length; i++) {
            if ((singleRange & (1 << i)) != 0) {
                if (key >= 0) {
                    return -1;
                }
                key = i;
            }
        }
        return key;
    }

    /**
     * A fixed shaper to that script.
     *
     * @throws IllegalArgumentException if not exactly one script is named
     */
    public static NumericShaper getShaper(int singleRange) {
        int key = singleKey(singleRange);
        if (key < 0) {
            throw new IllegalArgumentException("invalid shaper: " + Integer.toHexString(singleRange));
        }
        return new NumericShaper(singleRange, null, key, null, false);
    }

    /**
     * A fixed shaper to that script.
     *
     * @throws NullPointerException if the script is `null`
     */
    public static NumericShaper getShaper(Range singleRange) {
        if (singleRange == null) {
            throw new NullPointerException();
        }
        Set<Range> one = EnumSet.of(singleRange);
        return new NumericShaper(0, one, 0, singleRange, false);
    }

    /**
     * A contextual shaper among those scripts, with the Latin one as the initial context.
     *
     * @throws IllegalArgumentException if no known script is named
     */
    public static NumericShaper getContextualShaper(int ranges) {
        int r = ranges & ALL_RANGES;
        return new NumericShaper(r, null, EUROPEAN_KEY, null, true);
    }

    /**
     * A contextual shaper with the given initial context.
     *
     * @throws IllegalArgumentException if the initial context does not name exactly one script
     */
    public static NumericShaper getContextualShaper(int ranges, int defaultContext) {
        int key = singleKey(defaultContext);
        if (key < 0) {
            throw new IllegalArgumentException("invalid shaper: "
                    + Integer.toHexString(defaultContext));
        }
        return new NumericShaper(ranges & ALL_RANGES, null, key, null, true);
    }

    /**
     * A contextual shaper among those scripts, with the Latin one as the initial context.
     *
     * @throws NullPointerException if the set is `null`
     */
    public static NumericShaper getContextualShaper(Set<Range> ranges) {
        Set<Range> copy = EnumSet.noneOf(Range.class);
        copy.addAll(ranges);
        return new NumericShaper(0, copy, 0, Range.EUROPEAN, true);
    }

    /**
     * A contextual shaper with the given initial context.
     *
     * @throws NullPointerException if the set or the context is `null`
     */
    public static NumericShaper getContextualShaper(Set<Range> ranges, Range defaultContext) {
        if (defaultContext == null) {
            throw new NullPointerException();
        }
        Set<Range> copy = EnumSet.noneOf(Range.class);
        copy.addAll(ranges);
        return new NumericShaper(0, copy, 0, defaultContext, true);
    }

    /** Whether it looks at the surrounding text to decide. */
    public boolean isContextual() {
        return this.contextual;
    }

    /**
     * The scripts, as a mask of `int` constants.
     *
     * <p>It returns 0 if the shaper was built with `Range`: there are scripts with no constant, and
     * giving an incomplete mask would be worse than saying there is none.
     */
    public int getRanges() {
        return this.mask;
    }

    /** The scripts, as a set. */
    public Set<Range> getRangeSet() {
        if (this.rangeSet != null) {
            Set<Range> copy = EnumSet.noneOf(Range.class);
            copy.addAll(this.rangeSet);
            return copy;
        }
        Set<Range> copy = EnumSet.noneOf(Range.class);
        Range[] all = Range.values();
        for (int i = 0; i < BASES.length; i++) {
            if ((this.mask & (1 << i)) != 0) {
                copy.add(all[i]);
            }
        }
        return copy;
    }

    /**
     * Converts the stretch's digits, in place.
     *
     * @throws NullPointerException if the text is `null`
     * @throws IndexOutOfBoundsException if the stretch runs off the array
     */
    public void shape(char[] text, int start, int count) {
        check(text, start, count);
        if (this.contextual) {
            this.contextually(text, start, count, this.key, this.shapingRange);
        } else if (this.rangeSet != null) {
            convert(text, start, count, this.shapingRange.getNumericBase(),
                    this.shapingRange.noZero());
        } else {
            convert(text, start, count, BASES[this.key], this.key == ETHIOPIC_KEY);
        }
    }

    /**
     * Like the previous one, with another initial context.
     *
     * @throws IllegalArgumentException if the context does not name exactly one script
     * @throws NullPointerException if the text is `null`
     */
    public void shape(char[] text, int start, int count, int context) {
        check(text, start, count);
        int key = singleKey(context);
        if (key < 0) {
            throw new IllegalArgumentException("invalid context");
        }
        if (this.contextual) {
            this.contextually(text, start, count, key, null);
        } else {
            this.shape(text, start, count);
        }
    }

    /**
     * Like the previous one, with the initial context given as a `Range`.
     *
     * @throws NullPointerException if the text or the context is `null`
     */
    public void shape(char[] text, int start, int count, Range context) {
        check(text, start, count);
        if (context == null) {
            throw new NullPointerException();
        }
        if (this.contextual) {
            this.contextually(text, start, count, 0, context);
        } else {
            this.shape(text, start, count);
        }
    }

    /**
     * Checks the stretch.
     *
     * @throws NullPointerException if the text is `null`
     * @throws IndexOutOfBoundsException if the stretch runs off
     */
    private static void check(char[] text, int start, int count) {
        if (text == null) {
            throw new NullPointerException("text is null");
        }
        if (start < 0 || count < 0 || start + count > text.length || start + count < 0) {
            throw new IndexOutOfBoundsException("bad start or count");
        }
    }

    /** Replaces the stretch's Latin digits with that script's. */
    private static void convert(char[] text, int start, int count, char base, boolean noZero) {
        char lowest = noZero ? '1' : '0';
        char shift = (char) (base - '0');
        for (int i = start; i < start + count; i++) {
            char c = text[i];
            if (c >= lowest && c <= '9') {
                text[i] = (char) (c + shift);
            }
        }
    }

    /**
     * Walks the text following the context and converts each digit according to what came before.
     *
     * <p>The context only changes on **strong direction** characters: spaces and punctuation belong
     * to no script, and letting them cut it would make the number in "12 Arabics" come out written
     * differently from the one in "12Arabics".
     */
    private void contextually(char[] text, int start, int count, int ctxKey, Range ctxRange) {
        char base;
        boolean noZero;
        if (this.rangeSet != null) {
            Range current = ctxRange == null ? this.shapingRange : ctxRange;
            current = this.admitted(current);
            base = current.getNumericBase();
            noZero = current.noZero();
        } else {
            int k = ctxKey;
            if ((this.mask & (1 << k)) == 0) {
                k = EUROPEAN_KEY;
            }
            base = BASES[k];
            noZero = k == ETHIOPIC_KEY;
        }
        for (int i = start; i < start + count; i++) {
            char c = text[i];
            char lowest = noZero ? '1' : '0';
            if (c >= lowest && c <= '9') {
                text[i] = (char) (c + (char) (base - '0'));
                continue;
            }
            if (!isStrongDirection(c)) {
                continue;
            }
            if (this.rangeSet != null) {
                Range found = this.rangeOf(c);
                if (found != null) {
                    base = found.getNumericBase();
                    noZero = found.noZero();
                }
            } else {
                int found = keyOf(c);
                if (found >= 0) {
                    if ((this.mask & (1 << found)) == 0) {
                        found = EUROPEAN_KEY;
                    }
                    base = BASES[found];
                    noZero = found == ETHIOPIC_KEY;
                }
            }
        }
    }

    /** That range if the shaper admits it; the Latin one if not. */
    private Range admitted(Range r) {
        if (this.rangeSet.contains(r)) {
            return r;
        }
        return Range.EUROPEAN;
    }

    /** The admitted range that character belongs to, or `null` if to none. */
    private Range rangeOf(char c) {
        java.util.Iterator<Range> it = this.rangeSet.iterator();
        while (it.hasNext()) {
            Range r = it.next();
            if (r != Range.EUROPEAN && r.contains(c)) {
                return r;
            }
        }
        if (c < '\u0300') {
            return Range.EUROPEAN;
        }
        return null;
    }

    /** The position of the script that character belongs to, or -1. */
    private static int keyOf(char c) {
        for (int i = 1; i < CTX_LO.length; i++) {
            if (c >= CTX_LO[i] && c < CTX_HI[i]) {
                return i;
            }
        }
        if (c < CTX_HI[0]) {
            return EUROPEAN_KEY;
        }
        return -1;
    }

    /** Whether the character identifies a script on its own. */
    private static boolean isStrongDirection(char c) {
        byte d = Character.getDirectionality(c);
        return d == Character.DIRECTIONALITY_LEFT_TO_RIGHT
                || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
    }

    public int hashCode() {
        int h = this.mask;
        if (this.rangeSet != null) {
            h = h ^ this.rangeSet.hashCode();
        }
        if (this.contextual) {
            h = h ^ 1;
        }
        return h;
    }

    /** Equality by scripts, initial context and mode. */
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        NumericShaper that = (NumericShaper) o;
        if (this.contextual != that.contextual || this.mask != that.mask
                || this.key != that.key) {
            return false;
        }
        if (this.rangeSet == null) {
            return that.rangeSet == null;
        }
        return this.rangeSet.equals(that.rangeSet) && this.shapingRange == that.shapingRange;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("[contextual:").append(this.contextual);
        if (this.contextual) {
            sb.append(", context:");
            if (this.rangeSet != null) {
                sb.append(this.shapingRange);
            } else {
                sb.append(Integer.toHexString(1 << this.key));
            }
        }
        sb.append(", range(s): ");
        if (this.rangeSet != null) {
            sb.append(this.rangeSet);
        } else {
            sb.append(Integer.toHexString(this.mask));
        }
        sb.append(']');
        return sb.toString();
    }
}
