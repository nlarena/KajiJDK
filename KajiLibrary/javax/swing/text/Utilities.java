package javax.swing.text;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.SwingConstants;

/**
 * The text computations every view shares: measuring, drawing and finding word bounds.
 *
 * <h2>Why the tabs complicate everything</h2>
 *
 * <p>Without tabs, measuring a text is adding up character widths and it can be done piece by
 * piece. With tabs it cannot: a tab jumps to the next stop, so how much a stretch takes up
 * depends on <em>where it starts</em>. That is why every method here takes the starting position
 * {@code x} and a {@link TabExpander}, and why there are so many variants.
 *
 * <p>The three operations are the same walk with a different ending: drawing, adding up the
 * width, or stopping when a position has been passed. They are separate because drawing cannot
 * afford to measure twice.
 *
 * <h2>What needs a component</h2>
 *
 * <p>{@code getRowStart}, {@code getPositionAbove} and the word ones work on a
 * {@link JTextComponent}: the first ones need its view tree --where the <em>visible row</em>
 * starts cannot be known from the text alone-- and the word ones need the component's language.
 * The row ones return {@code -1} if the component has no look and feel installed, which in this
 * library is always: there is no {@code BasicTextUI}.
 */
public class Utilities {

    public Utilities() {
    }

    /** The Swing component that view lives in, or {@code null}. */
    static javax.swing.JComponent getJComponent(View view) {
        if (view != null) {
            java.awt.Component component = view.getContainer();
            if (component instanceof javax.swing.JComponent) {
                return (javax.swing.JComponent) component;
            }
        }
        return null;
    }

    /**
     * It draws the text expanding the tabs; it returns where it ended.
     *
     * <p>It draws in stretches between tabs: each stretch goes in one go to the graphics context,
     * which is much cheaper than drawing character by character.
     */
    public static final int drawTabbedText(Segment s, int x, int y, Graphics g,
            TabExpander e, int startOffset) {
        return (int) drawTabbedText(null, s, x, y, g, e, startOffset, null);
    }

    /** Like the previous one, with fractional coordinates. */
    public static final float drawTabbedText(Segment s, float x, float y, Graphics2D g,
            TabExpander e, int startOffset) {
        return drawTabbedText(null, s, x, y, g, e, startOffset, null, true);
    }

    static final int drawTabbedText(View view, Segment s, int x, int y, Graphics g,
            TabExpander e, int startOffset) {
        return (int) drawTabbedText(view, s, x, y, g, e, startOffset, null);
    }

    static final int drawTabbedText(View view, Segment s, int x, int y, Graphics g,
            TabExpander e, int startOffset, int[] justificationData) {
        return (int) drawTabbedText(view, s, x, y, g, e, startOffset, justificationData, true);
    }

    /** The version that does the work; the others call it. */
    static final float drawTabbedText(View view, Segment s, float x, float y, Graphics g,
            TabExpander e, int startOffset, int[] justificationData, boolean useFPAPI) {
        FontMetrics metrics = g.getFontMetrics();
        float nextX = x;
        char[] txt = s.array;
        int txtOffset = s.offset;
        int flushLen = 0;
        int flushIndex = s.offset;
        int n = s.offset + s.count;
        for (int i = txtOffset; i < n; i++) {
            char c = txt[i];
            if (c == '\t' || c == '\n') {
                if (flushLen > 0) {
                    g.drawChars(txt, flushIndex, flushLen, (int) nextX, (int) y);
                    nextX = nextX + metrics.charsWidth(txt, flushIndex, flushLen);
                    flushLen = 0;
                }
                flushIndex = i + 1;
                if (c == '\t') {
                    if (e != null) {
                        nextX = e.nextTabStop(nextX, startOffset + i - txtOffset);
                    } else {
                        nextX = nextX + metrics.charWidth(' ');
                    }
                }
                // A line ending inside a stretch draws nothing: the view already cut it.
            } else {
                flushLen = flushLen + 1;
            }
        }
        if (flushLen > 0) {
            g.drawChars(txt, flushIndex, flushLen, (int) nextX, (int) y);
            nextX = nextX + metrics.charsWidth(txt, flushIndex, flushLen);
        }
        return nextX;
    }

    /** How much that text takes up starting at {@code x}, with the tabs expanded. */
    public static final int getTabbedTextWidth(Segment s, FontMetrics metrics, int x,
            TabExpander e, int startOffset) {
        return (int) getTabbedTextWidth(null, s, metrics, x, e, startOffset, null);
    }

    public static final float getTabbedTextWidth(Segment s, FontMetrics metrics, float x,
            TabExpander e, int startOffset) {
        return getTabbedTextWidth(null, s, metrics, x, e, startOffset, null);
    }

    static final int getTabbedTextWidth(View view, Segment s, FontMetrics metrics, int x,
            TabExpander e, int startOffset, int[] justificationData) {
        return (int) getTabbedTextWidth(view, s, metrics, (float) x, e, startOffset,
                justificationData);
    }

    static final float getTabbedTextWidth(View view, Segment s, FontMetrics metrics, float x,
            TabExpander e, int startOffset, int[] justificationData) {
        return getTabbedTextWidth(view, s, metrics, x, e, startOffset, justificationData, true);
    }

    /** The version that does the work. */
    static final float getTabbedTextWidth(View view, Segment s, FontMetrics metrics, float x,
            TabExpander e, int startOffset, int[] justificationData, boolean useFPAPI) {
        float nextX = x;
        char[] txt = s.array;
        int txtOffset = s.offset;
        int n = s.offset + s.count;
        int charCount = 0;
        for (int i = txtOffset; i < n; i++) {
            char c = txt[i];
            if (c == '\t') {
                nextX = nextX + metrics.charsWidth(txt, i - charCount, charCount);
                charCount = 0;
                if (e != null) {
                    nextX = e.nextTabStop(nextX, startOffset + i - txtOffset);
                } else {
                    nextX = nextX + metrics.charWidth(' ');
                }
            } else if (c == '\n') {
                nextX = nextX + metrics.charsWidth(txt, i - charCount, charCount);
                charCount = 0;
            } else {
                charCount = charCount + 1;
            }
        }
        nextX = nextX + metrics.charsWidth(txt, n - charCount, charCount);
        return nextX - x;
    }

    /**
     * Which character falls at that horizontal position.
     *
     * <p>It returns the offset within the segment, not within the document. It is the operation
     * that turns a click into a position in the text.
     */
    public static final int getTabbedTextOffset(Segment s, FontMetrics metrics, int x0, int x,
            TabExpander e, int startOffset) {
        return getTabbedTextOffset(s, metrics, x0, x, e, startOffset, true);
    }

    static final int getTabbedTextOffset(View view, Segment s, FontMetrics metrics, int x0,
            int x, TabExpander e, int startOffset, int[] justificationData) {
        return getTabbedTextOffset(view, s, metrics, (float) x0, (float) x, e, startOffset, true,
                justificationData, true);
    }

    static final int getTabbedTextOffset(View view, Segment s, FontMetrics metrics, float x0,
            float x, TabExpander e, int startOffset, int[] justificationData) {
        return getTabbedTextOffset(view, s, metrics, x0, x, e, startOffset, true,
                justificationData, true);
    }

    public static final int getTabbedTextOffset(Segment s, FontMetrics metrics, int x0, int x,
            TabExpander e, int startOffset, boolean round) {
        return getTabbedTextOffset(null, s, metrics, (float) x0, (float) x, e, startOffset, round,
                null, true);
    }

    public static final int getTabbedTextOffset(Segment s, FontMetrics metrics, float x0,
            float x, TabExpander e, int startOffset, boolean round) {
        return getTabbedTextOffset(null, s, metrics, x0, x, e, startOffset, round, null, true);
    }

    /**
     * The version that does the work.
     *
     * <p>{@code round} decides what happens when the point falls in the middle of a character: with
     * {@code true} the nearest edge is chosen, which is what makes the cursor fall where one
     * pointed and not always to the left.
     */
    static final int getTabbedTextOffset(View view, Segment s, FontMetrics metrics, float x0,
            float x, TabExpander e, int startOffset, boolean round, int[] justificationData,
            boolean useFPAPI) {
        if (x0 >= x) {
            return 0;
        }
        float currX = x0;
        float nextX = currX;
        char[] txt = s.array;
        int txtOffset = s.offset;
        int txtCount = s.count;
        int n = s.offset + s.count;
        for (int i = s.offset; i < n; i++) {
            if (txt[i] == '\t') {
                if (e != null) {
                    nextX = e.nextTabStop(nextX, startOffset + i - txtOffset);
                } else {
                    nextX = nextX + metrics.charWidth(' ');
                }
            } else {
                nextX = nextX + metrics.charWidth(txt[i]);
            }
            if (x >= currX && x < nextX) {
                if (round) {
                    if ((x - currX) < (nextX - x)) {
                        return i - txtOffset;
                    }
                    return i + 1 - txtOffset;
                }
                return i - txtOffset;
            }
            currX = nextX;
        }
        return txtCount;
    }

    /**
     * Where to break the text so that it fits that width.
     *
     * <p>It breaks at the last space before the limit, not at the exact character: breaking words
     * in the middle looks wrong and it is what tells this method apart from
     * {@link #getTabbedTextOffset}.
     */
    public static final int getBreakLocation(Segment s, FontMetrics metrics, int x0, int x,
            TabExpander e, int startOffset) {
        return getBreakLocation(s, metrics, (float) x0, (float) x, e, startOffset, true);
    }

    public static final int getBreakLocation(Segment s, FontMetrics metrics, float x0, float x,
            TabExpander e, int startOffset) {
        return getBreakLocation(s, metrics, x0, x, e, startOffset, true);
    }

    static final int getBreakLocation(Segment s, FontMetrics metrics, float x0, float x,
            TabExpander e, int startOffset, boolean useFPAPI) {
        char[] txt = s.array;
        int txtOffset = s.offset;
        int txtCount = s.count;
        int index = getTabbedTextOffset(null, s, metrics, x0, x, e, startOffset, false, null,
                useFPAPI);

        if (index >= txtCount - 1) {
            return txtCount;
        }

        for (int i = txtOffset + index; i >= txtOffset; i--) {
            char ch = txt[i];
            if (ch < 256) {
                if (ch == ' ' || ch == '\t') {
                    // It breaks after the space: the space stays on the line above.
                    index = i - txtOffset + 1;
                    return index;
                }
            } else if (Character.isWhitespace(ch)) {
                index = i - txtOffset + 1;
                return index;
            }
        }
        return index;
    }

    /**
     * Where the visible row that contains that position starts.
     *
     * <p>{@code -1} with no look and feel installed; see the class note.
     */
    public static final int getRowStart(JTextComponent c, int offs) throws BadLocationException {
        Rectangle r = allocation(c, offs);
        if (r == null) {
            return -1;
        }
        int lastOffs = offs;
        int y = r.y;
        while ((r != null) && (y == r.y)) {
            offs = lastOffs;
            lastOffs = lastOffs - 1;
            r = (lastOffs >= 0) ? allocation(c, lastOffs) : null;
        }
        return offs;
    }

    /** Where the visible row that contains that position ends. */
    public static final int getRowEnd(JTextComponent c, int offs) throws BadLocationException {
        Rectangle r = allocation(c, offs);
        if (r == null) {
            return -1;
        }
        int n = c.getDocument().getLength();
        int lastOffs = offs;
        int y = r.y;
        while ((r != null) && (y == r.y)) {
            offs = lastOffs;
            lastOffs = lastOffs + 1;
            r = (lastOffs <= n) ? allocation(c, lastOffs) : null;
        }
        return offs;
    }

    /** Where that position falls, or {@code null} if the component has no look and feel. */
    private static Rectangle allocation(JTextComponent c, int offs) throws BadLocationException {
        javax.swing.plaf.TextUI ui = c.getUI();
        if (ui == null) {
            return null;
        }
        return ui.modelToView(c, offs);
    }

    /** The position right above, at the same horizontal height. */
    public static final int getPositionAbove(JTextComponent c, int offs, int x)
            throws BadLocationException {
        return getPositionAbove(c, offs, (float) x, true);
    }

    public static final int getPositionAbove(JTextComponent c, int offs, float x)
            throws BadLocationException {
        return getPositionAbove(c, offs, x, true);
    }

    static final int getPositionAbove(JTextComponent c, int offs, float x, boolean useFPAPI)
            throws BadLocationException {
        int lastOffs = getRowStart(c, offs) - 1;
        if (lastOffs < 0) {
            return -1;
        }
        return positionInRow(c, lastOffs, x);
    }

    public static final int getPositionBelow(JTextComponent c, int offs, int x)
            throws BadLocationException {
        return getPositionBelow(c, offs, (float) x, true);
    }

    public static final int getPositionBelow(JTextComponent c, int offs, float x)
            throws BadLocationException {
        return getPositionBelow(c, offs, x, true);
    }

    static final int getPositionBelow(JTextComponent c, int offs, float x, boolean useFPAPI)
            throws BadLocationException {
        int lastOffs = getRowEnd(c, offs) + 1;
        if (lastOffs <= 0 || lastOffs > c.getDocument().getLength()) {
            return -1;
        }
        return positionInRow(c, lastOffs, x);
    }

    /** The position of that row that comes closest to that column. */
    private static int positionInRow(JTextComponent c, int offsetInRow, float x)
            throws BadLocationException {
        int start = getRowStart(c, offsetInRow);
        int end = getRowEnd(c, offsetInRow);
        if (start < 0 || end < 0) {
            return -1;
        }
        int best = start;
        float bestDist = Float.MAX_VALUE;
        for (int i = start; i <= end; i++) {
            Rectangle r = allocation(c, i);
            if (r != null) {
                float d = Math.abs(r.x - x);
                if (d < bestDist) {
                    bestDist = d;
                    best = i;
                }
            }
        }
        return best;
    }

    /** Where the word that contains that position starts. */
    public static final int getWordStart(JTextComponent c, int offs) throws BadLocationException {
        Document doc = c.getDocument();
        Element line = getParagraphElement(c, offs);
        if (line == null) {
            throw new BadLocationException("No word at " + offs, offs);
        }
        int lineStart = line.getStartOffset();
        int lineEnd = Math.min(line.getEndOffset(), doc.getLength());
        Segment seg = new Segment();
        doc.getText(lineStart, lineEnd - lineStart, seg);
        if (seg.count > 0) {
            int i = offs - lineStart;
            if (i >= seg.count) {
                i = seg.count - 1;
            }
            while (i > 0 && !isSeparator(seg.array[seg.offset + i - 1])) {
                i = i - 1;
            }
            return lineStart + i;
        }
        return offs;
    }

    /** Where the word that contains that position ends. */
    public static final int getWordEnd(JTextComponent c, int offs) throws BadLocationException {
        Document doc = c.getDocument();
        Element line = getParagraphElement(c, offs);
        if (line == null) {
            throw new BadLocationException("No word at " + offs, offs);
        }
        int lineStart = line.getStartOffset();
        int lineEnd = Math.min(line.getEndOffset(), doc.getLength());
        Segment seg = new Segment();
        doc.getText(lineStart, lineEnd - lineStart, seg);
        if (seg.count > 0) {
            int i = offs - lineStart;
            while (i < seg.count && !isSeparator(seg.array[seg.offset + i])) {
                i = i + 1;
            }
            return lineStart + i;
        }
        return offs;
    }

    /** The beginning of the next word. */
    public static final int getNextWord(JTextComponent c, int offs) throws BadLocationException {
        Document doc = c.getDocument();
        int n = doc.getLength();
        Segment seg = new Segment();
        doc.getText(offs, n - offs, seg);
        boolean sawSeparator = false;
        for (int i = 0; i < seg.count; i++) {
            char ch = seg.array[seg.offset + i];
            if (isSeparator(ch)) {
                sawSeparator = true;
            } else if (sawSeparator) {
                return offs + i;
            }
        }
        throw new BadLocationException("No more words", offs);
    }

    static int getNextWordInParagraph(JTextComponent c, Element line, int offs, boolean first)
            throws BadLocationException {
        return getNextWord(c, offs);
    }

    /** The beginning of the previous word. */
    public static final int getPreviousWord(JTextComponent c, int offs)
            throws BadLocationException {
        if (offs <= 0) {
            throw new BadLocationException("No more words", offs);
        }
        Document doc = c.getDocument();
        Segment seg = new Segment();
        doc.getText(0, offs, seg);
        int i = seg.count - 1;
        while (i >= 0 && isSeparator(seg.array[seg.offset + i])) {
            i = i - 1;
        }
        while (i > 0 && !isSeparator(seg.array[seg.offset + i - 1])) {
            i = i - 1;
        }
        if (i < 0) {
            throw new BadLocationException("No more words", offs);
        }
        return i;
    }

    static int getPrevWordInParagraph(JTextComponent c, Element line, int offs)
            throws BadLocationException {
        return getPreviousWord(c, offs);
    }

    /** What separates one word from another. */
    private static boolean isSeparator(char ch) {
        return Character.isWhitespace(ch) || (!Character.isLetterOrDigit(ch) && ch != '_');
    }

    /** The paragraph that contains that position, according to the component's document. */
    public static final Element getParagraphElement(JTextComponent c, int offs) {
        Document doc = c.getDocument();
        if (doc instanceof StyledDocument) {
            return ((StyledDocument) doc).getParagraphElement(offs);
        }
        Element map = doc.getDefaultRootElement();
        int index = map.getElementIndex(offs);
        Element line = map.getElement(index);
        if ((offs >= line.getStartOffset()) && (offs < line.getEndOffset())) {
            return line;
        }
        return null;
    }

    /** Whether that stretch is an input method's text being composed; never, here. */
    static boolean isComposedTextElement(Document doc, int offset) {
        return false;
    }

    static boolean isComposedTextElement(Element elem) {
        return false;
    }

    static boolean isComposedTextAttributeDefined(AttributeSet as) {
        return false;
    }

    /** Without input methods there is no text being composed to draw. */
    static int drawComposedText(View view, AttributeSet attr, Graphics g, int x, int y, int p0,
            int p1) throws BadLocationException {
        return x;
    }

    static float drawComposedText(View view, AttributeSet attr, Graphics g, float x, float y,
            int p0, int p1) throws BadLocationException {
        return x;
    }

    static float drawComposedText(View view, AttributeSet attr, Graphics g, float x, float y,
            int p0, int p1, boolean useFPAPI) throws BadLocationException {
        return x;
    }

    static void paintComposedText(Graphics g, Rectangle alloc, GlyphView v) {
    }

    /** Whether that component reads from left to right. */
    static boolean isLeftToRight(java.awt.Component c) {
        return c.getComponentOrientation().isLeftToRight();
    }

    /**
     * The next visual position inside a view with children.
     *
     * <p>It asks the child that has the position and, if it runs out, the one beside it; see
     * {@link CompositeView}.
     */
    static int getNextVisualPositionFrom(View v, int pos, Position.Bias b, Shape alloc,
            int direction, Position.Bias[] biasRet) throws BadLocationException {
        if (v.getViewCount() == 0) {
            return pos;
        }
        boolean top = (direction == SwingConstants.NORTH || direction == SwingConstants.WEST);
        int retValue;
        if (pos == -1) {
            int childIndex = top ? v.getViewCount() - 1 : 0;
            View child = v.getView(childIndex);
            Shape childBounds = v.getChildAllocation(childIndex, alloc);
            retValue = child.getNextVisualPositionFrom(pos, b, childBounds, direction, biasRet);
        } else {
            int increment = top ? -1 : 1;
            int childIndex;
            if (b == Position.Bias.Backward && pos > 0) {
                childIndex = v.getViewIndex(pos - 1, Position.Bias.Forward);
            } else {
                childIndex = v.getViewIndex(pos, Position.Bias.Forward);
            }
            if (childIndex < 0) {
                return pos;
            }
            View child = v.getView(childIndex);
            Shape childBounds = v.getChildAllocation(childIndex, alloc);
            retValue = child.getNextVisualPositionFrom(pos, b, childBounds, direction, biasRet);
            childIndex = childIndex + increment;
            if (retValue == -1 && childIndex >= 0 && childIndex < v.getViewCount()) {
                child = v.getView(childIndex);
                childBounds = v.getChildAllocation(childIndex, alloc);
                retValue = child.getNextVisualPositionFrom(-1, b, childBounds, direction,
                        biasRet);
            }
        }
        return retValue;
    }
}
