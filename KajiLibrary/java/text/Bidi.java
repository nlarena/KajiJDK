package java.text;

/**
 * The drawing order of a text mixing left-to-right and right-to-left scripts.
 *
 * <p>A {@code String} keeps the text in LOGICAL order --the order in which it is read-- and that
 * is not enough to draw it: in "the file ‏שלום‎ is here", the Hebrew word is drawn backwards and
 * the rest is not, and where each stretch begins and ends is not decided character by character.
 * This class runs the Unicode algorithm (UAX #9) and answers the only thing needed: what LEVEL each
 * character has. An even level is left to right, an odd one is right to left, and the number says
 * how deeply nested it is.
 *
 * <p>Everything else in the API comes out of that. The RUNS are the stretches of constant level,
 * which are the units a renderer can treat as a single piece. {@link #reorderVisually} orders them
 * for painting. And {@link #createLineBidi} exists because breaking into lines changes the result:
 * a line's trailing space goes back to the paragraph's direction, something that in the middle of
 * the text would not happen.
 *
 * @implNote The algorithm is complete except for rule N0 (bracket pairs, added in Unicode 6.3),
 *           which needs the {@code BidiBrackets.txt} table. Without it brackets resolve as ordinary
 *           neutrals, which is what the algorithm did before 6.3; the difference shows up only when
 *           a bracket pair encloses text of the opposite direction to its surroundings. See
 *           {@code BidiAlgorithm} for the detail and for why half a table was not transcribed.
 */
public final class Bidi {

    /** A left-to-right base, without looking at the text. */
    public static final int DIRECTION_LEFT_TO_RIGHT = 0;

    /** A right-to-left base, without looking at the text. */
    public static final int DIRECTION_RIGHT_TO_LEFT = 1;

    /** The base comes from the first strong character; if there is none, left to right. */
    public static final int DIRECTION_DEFAULT_LEFT_TO_RIGHT = -2;

    /** The base comes from the first strong character; if there is none, right to left. */
    public static final int DIRECTION_DEFAULT_RIGHT_TO_LEFT = -1;

    private final byte[] levels;
    private final byte baseLevel;
    private final int length;

    public Bidi(String paragraph, int flags) {
        this(Bidi.toChars(paragraph), 0, null, 0, Bidi.lengthOf(paragraph), flags);
    }

    /**
     * It runs the algorithm over an attributed iterator's text.
     *
     * <p>The attributes it looks at are the two the JDK defines for this: {@code RUN_DIRECTION} for
     * the base direction and {@code BIDI_EMBEDDING} for the imposed embeddings. Both are defined by
     * {@code java.awt.font.TextAttribute}, which is not part of {@code java.base} and does not exist
     * here; without those keys there is no way of reading them, so this constructor uses the text and
     * the direction deduced from the first strong character. It is the same result the JDK gives when
     * the iterator carries no such attributes, which is the case of every iterator built inside
     * {@code java.base}.
     */
    public Bidi(AttributedCharacterIterator paragraph) {
        this(Bidi.textOf(paragraph), 0, null, 0, Bidi.textOf(paragraph).length,
                Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
    }

    /**
     * It runs the algorithm with embeddings imposed by the caller.
     *
     * @param embeddings one per character: a positive value is an embedding (the text keeps its own
     *                   direction inside that level) and a negative one an override (the level's
     *                   direction rules). Zero lets the algorithm decide. {@code null} is the same as
     *                   all zeros.
     */
    public Bidi(char[] text, int textStart, byte[] embeddings, int embStart, int paragraphLength,
                int flags) {
        if (text == null) {
            throw new NullPointerException();
        }
        if (textStart < 0 || paragraphLength < 0 || textStart + paragraphLength > text.length) {
            throw new IllegalArgumentException("Invalid text range");
        }
        if (embeddings != null && (embStart < 0 || embStart + paragraphLength > embeddings.length)) {
            throw new IllegalArgumentException("Invalid embeddings range");
        }
        char[] piece = new char[paragraphLength];
        for (int i = 0; i < paragraphLength; i = i + 1) {
            piece[i] = text[textStart + i];
        }
        byte[] enc = null;
        if (embeddings != null) {
            enc = new byte[paragraphLength];
            boolean any = false;
            for (int i = 0; i < paragraphLength; i = i + 1) {
                enc[i] = embeddings[embStart + i];
                if (enc[i] != 0) {
                    any = true;
                }
            }
            if (!any) {
                enc = null;
            }
        }
        int base;
        if (flags == Bidi.DIRECTION_LEFT_TO_RIGHT) {
            base = 0;
        } else if (flags == Bidi.DIRECTION_RIGHT_TO_LEFT) {
            base = 1;
        } else if (flags == Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT) {
            base = -1;
        } else {
            base = -2;
        }
        BidiAlgorithm alg = new BidiAlgorithm(piece, enc, base);
        byte[] lv = alg.levels();
        byte nb = alg.paragraphLevel();
        Bidi.flattenIfUniform(lv, nb);
        this.levels = lv;
        this.baseLevel = nb;
        this.length = paragraphLength;
    }

    /**
     * When ALL the text goes in the base's direction, the levels are flattened to the base level.
     *
     * <p>It is what the JDK does, and it is not cosmetic: a text that resolved to levels 3-3-3-1 with
     * base 1 is drawn exactly like one of levels 1-1-1-1 --the visual order does not change if there
     * is never a change of direction-- and the JDK reports a single run instead of two. Reporting the
     * internal levels there would give a {@code getRunCount()} different from its own without any
     * renderer seeing the difference.
     *
     * <p>Looking at the parity is enough: if some level has a parity different from the base's, there
     * is a change of direction and the levels are kept as they are.
     */
    private static void flattenIfUniform(byte[] levels, byte baseLevel) {
        int parity = baseLevel & 1;
        for (int i = 0; i < levels.length; i = i + 1) {
            if ((levels[i] & 1) != parity) {
                return;
            }
        }
        for (int i = 0; i < levels.length; i = i + 1) {
            levels[i] = baseLevel;
        }
    }

    // Internal constructor for createLineBidi: the levels are already resolved and all that is left
    // is to trim and to apply L1 again over the new end of line.
    private Bidi(byte[] levels, byte baseLevel) {
        this.levels = levels;
        this.baseLevel = baseLevel;
        this.length = levels.length;
    }

    private static char[] toChars(String s) {
        if (s == null) {
            throw new NullPointerException();
        }
        return s.toCharArray();
    }

    private static int lengthOf(String s) {
        if (s == null) {
            throw new NullPointerException();
        }
        return s.length();
    }

    private static char[] textOf(AttributedCharacterIterator it) {
        if (it == null) {
            throw new NullPointerException();
        }
        int from = it.getBeginIndex();
        int to = it.getEndIndex();
        char[] out = new char[to - from];
        for (int i = from; i < to; i = i + 1) {
            it.setIndex(i);
            out[i - from] = it.current();
        }
        return out;
    }

    /**
     * The Bidi of a line trimmed out of this paragraph.
     *
     * <p>The algorithm is not run again: the paragraph's levels are already the right ones and
     * recomputing them over the piece would give ANOTHER result, because the piece does not see the
     * context. The only thing that changes is L1 --the line's trailing space goes back to the
     * paragraph's direction-- and that does depend on where the cut was made.
     */
    public Bidi createLineBidi(int lineStart, int lineLimit) {
        if (lineStart < 0 || lineLimit < lineStart || lineLimit > this.length) {
            throw new IllegalArgumentException("Invalid line range");
        }
        int m = lineLimit - lineStart;
        byte[] sub = new byte[m];
        for (int i = 0; i < m; i = i + 1) {
            sub[i] = this.levels[lineStart + i];
        }
        int i = m - 1;
        while (i >= 0 && sub[i] == this.baseLevel) {
            i = i - 1;
        }
        return new Bidi(sub, this.baseLevel);
    }

    public boolean isMixed() {
        return !this.isLeftToRight() && !this.isRightToLeft();
    }

    public boolean isLeftToRight() {
        if ((this.baseLevel & 1) != 0) {
            return false;
        }
        for (int i = 0; i < this.length; i = i + 1) {
            if ((this.levels[i] & 1) != 0) {
                return false;
            }
        }
        return true;
    }

    public boolean isRightToLeft() {
        if ((this.baseLevel & 1) == 0) {
            return false;
        }
        for (int i = 0; i < this.length; i = i + 1) {
            if ((this.levels[i] & 1) == 0) {
                return false;
            }
        }
        return true;
    }

    public int getLength() {
        return this.length;
    }

    public boolean baseIsLeftToRight() {
        return (this.baseLevel & 1) == 0;
    }

    public int getBaseLevel() {
        return this.baseLevel;
    }

    public int getLevelAt(int offset) {
        // Out of range it returns the base level instead of blowing up: it is what the JDK does, and
        // it saves the renderer a check at every line's edge.
        if (offset < 0 || offset >= this.length) {
            return this.baseLevel;
        }
        return this.levels[offset];
    }

    /** How many stretches of constant level there are. Each is drawn as one piece. */
    public int getRunCount() {
        if (this.length == 0) {
            return 0;
        }
        int c = 1;
        for (int i = 1; i < this.length; i = i + 1) {
            if (this.levels[i] != this.levels[i - 1]) {
                c = c + 1;
            }
        }
        return c;
    }

    public int getRunLevel(int run) {
        return this.levels[this.runStart(run)];
    }

    public int getRunStart(int run) {
        return this.runStart(run);
    }

    public int getRunLimit(int run) {
        int i = this.runStart(run) + 1;
        while (i < this.length && this.levels[i] == this.levels[i - 1]) {
            i = i + 1;
        }
        return i;
    }

    private int runStart(int run) {
        if (run < 0) {
            throw new IllegalArgumentException("Invalid run index " + run);
        }
        int c = 0;
        for (int i = 0; i < this.length; i = i + 1) {
            if (i == 0 || this.levels[i] != this.levels[i - 1]) {
                if (c == run) {
                    return i;
                }
                c = c + 1;
            }
        }
        throw new IllegalArgumentException("Invalid run index " + run);
    }

    /**
     * Whether the text needs the algorithm, or whether drawing it left to right is enough.
     *
     * <p>It exists so the whole algorithm can be skipped in the ordinary case, which is the commonest
     * one: a text with nothing right-to-left and no controls is drawn in logical order and not a
     * single level need be computed.
     */
    public static boolean requiresBidi(char[] text, int start, int limit) {
        if (text == null) {
            throw new NullPointerException();
        }
        if (start < 0 || limit > text.length || start > limit) {
            throw new IllegalArgumentException("Invalid range");
        }
        for (int i = start; i < limit; i = i + 1) {
            byte t = Character.getDirectionality(text[i]);
            if (t == BidiAlgorithm.R || t == BidiAlgorithm.AL || t == BidiAlgorithm.AN
                    || t == BidiAlgorithm.RLE || t == BidiAlgorithm.RLO
                    || t == BidiAlgorithm.LRE || t == BidiAlgorithm.LRO
                    || t == BidiAlgorithm.PDF || t == BidiAlgorithm.LRI
                    || t == BidiAlgorithm.RLI || t == BidiAlgorithm.FSI
                    || t == BidiAlgorithm.PDI) {
                return true;
            }
        }
        return false;
    }

    /**
     * It reorders the objects into visual order (rule L2).
     *
     * <p>The rule is short and not obvious: from the highest level down to the lowest, every
     * contiguous stretch of level greater than or equal is REVERSED. Doing it in a single pass does
     * not work -- a level-2 stretch inside a level-1 one has to be reversed twice, and the second
     * reversal returns it to its internal order.
     */
    public static void reorderVisually(byte[] levels, int levelStart, Object[] objects,
                                       int objectStart, int count) {
        if (levels == null || objects == null) {
            throw new NullPointerException();
        }
        if (count < 0 || levelStart < 0 || objectStart < 0
                || levelStart + count > levels.length || objectStart + count > objects.length) {
            throw new IllegalArgumentException("Invalid range");
        }
        if (count == 0) {
            return;
        }
        byte max = 0;
        byte minOdd = (byte) (BidiAlgorithm.MAX_DEPTH + 1);
        for (int i = 0; i < count; i = i + 1) {
            byte l = levels[levelStart + i];
            if (l > max) {
                max = l;
            }
            if ((l & 1) != 0 && l < minOdd) {
                minOdd = l;
            }
        }
        for (byte level = max; level >= minOdd; level = (byte) (level - 1)) {
            int i = 0;
            while (i < count) {
                if (levels[levelStart + i] < level) {
                    i = i + 1;
                    continue;
                }
                int end = i;
                while (end < count && levels[levelStart + end] >= level) {
                    end = end + 1;
                }
                int a = objectStart + i;
                int b = objectStart + end - 1;
                while (a < b) {
                    Object tmp = objects[a];
                    objects[a] = objects[b];
                    objects[b] = tmp;
                    a = a + 1;
                    b = b - 1;
                }
                i = end;
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("java.text.Bidi[direction: ");
        if (this.isMixed()) {
            sb.append("mixed");
        } else if (this.isLeftToRight()) {
            sb.append("ltr");
        } else {
            sb.append("rtl");
        }
        sb.append(" baselevel: ");
        sb.append(Integer.toString(this.baseLevel));
        sb.append(" length: ");
        sb.append(Integer.toString(this.length));
        sb.append(" runs:");
        int c = this.getRunCount();
        for (int i = 0; i < c; i = i + 1) {
            sb.append(" ");
            sb.append(Integer.toString(this.getRunStart(i)));
            sb.append("-");
            sb.append(Integer.toString(this.getRunLimit(i)));
            sb.append("(");
            sb.append(Integer.toString(this.getRunLevel(i)));
            sb.append(")");
        }
        sb.append("]");
        return sb.toString();
    }
}
