package javax.swing.text;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;

/**
 * A paragraph: rows of text with indentation, line spacing, alignment and tabs.
 *
 * <h2>What it adds over {@link FlowView}</h2>
 *
 * <p>Flow knows how to break into rows. This one knows <em>how a paragraph looks</em>: the first
 * line may be indented differently, the lines may be spaced out, the text may be centred or
 * right-aligned, and there are tab stops. All that comes from the paragraph attributes, and that
 * is why {@link #setPropertiesFromAttributes} is the central method.
 *
 * <p>It is a {@link TabExpander} because the stops belong to the paragraph: a tab in the middle
 * of a row asks its paragraph, not the row.
 */
public class ParagraphView extends FlowView implements TabExpander {

    /** How much the first line is indented, in pixels. */
    protected int firstLineIndent = 0;

    static Class<?> i18nStrategy;

    /** The characters that break at a decimal tab. */
    static char[] tabChars = {'\t'};

    static char[] tabDecimalChars = {'\t', '.'};

    private int justification;
    private float lineSpacing;
    private TabSet tabSet;

    /** A paragraph of that element, stacking rows downwards. */
    public ParagraphView(Element elem) {
        super(elem, View.Y_AXIS);
        setPropertiesFromAttributes();
    }

    protected void setJustification(int j) {
        justification = j;
    }

    protected void setLineSpacing(float ls) {
        lineSpacing = ls;
    }

    protected void setFirstLineIndent(float fi) {
        firstLineIndent = (int) fi;
    }

    /** It takes indents, line spacing, alignment and tabs from the attributes. */
    protected void setPropertiesFromAttributes() {
        AttributeSet attr = getAttributes();
        if (attr != null) {
            setParagraphInsets(attr);
            setJustification(StyleConstants.getAlignment(attr));
            setLineSpacing(StyleConstants.getLineSpacing(attr));
            setFirstLineIndent(StyleConstants.getFirstLineIndent(attr));
            tabSet = StyleConstants.getTabSet(attr);
        }
    }

    /** How many views there are in the logical tree. */
    protected int getLayoutViewCount() {
        return layoutPool.getViewCount();
    }

    protected View getLayoutView(int index) {
        return layoutPool.getView(index);
    }

    /** Going up or down one line within the paragraph. */
    protected int getNextNorthSouthVisualPositionFrom(int pos, Position.Bias b, Shape a,
            int direction, Position.Bias[] biasRet) throws BadLocationException {
        int vIndex;
        if (pos == -1) {
            vIndex = (direction == NORTH) ? getViewCount() - 1 : 0;
        } else {
            if (b == Position.Bias.Backward && pos > 0) {
                vIndex = getViewIndexAtPosition(pos - 1);
            } else {
                vIndex = getViewIndexAtPosition(pos);
            }
            if (direction == NORTH) {
                vIndex = vIndex - 1;
            } else {
                vIndex = vIndex + 1;
            }
        }
        if (vIndex < 0 || vIndex >= getViewCount()) {
            return -1;
        }
        int x = 0;
        try {
            Shape s = modelToView(pos < 0 ? getStartOffset() : pos, a, Position.Bias.Forward);
            if (s != null) {
                x = s.getBounds().x;
            }
        } catch (BadLocationException e) {
            x = 0;
        }
        return getClosestPositionTo(pos, b, a, direction, biasRet, vIndex, x);
    }

    /** The position of that row that comes closest to that column. */
    protected int getClosestPositionTo(int pos, Position.Bias b, Shape a, int direction,
            Position.Bias[] biasRet, int rowIndex, int x) throws BadLocationException {
        View row = getView(rowIndex);
        Shape rowAlloc = getChildAllocation(rowIndex, a);
        biasRet[0] = Position.Bias.Forward;
        if (rowAlloc == null) {
            return row.getStartOffset();
        }
        return row.viewToModel(x, rowAlloc.getBounds().y, rowAlloc, biasRet);
    }

    protected boolean flipEastAndWestAtEnds(int position, Position.Bias bias) {
        return false;
    }

    /** The first row may have less room, because of the indentation. */
    public int getFlowSpan(int index) {
        View row = getView(index);
        int adjust = 0;
        if (row != null && index == 0) {
            adjust = adjust + firstLineIndent;
        }
        return (layoutSpan == Integer.MAX_VALUE) ? layoutSpan : layoutSpan - adjust;
    }

    public int getFlowStart(int index) {
        if (index == 0) {
            return firstLineIndent;
        }
        return 0;
    }

    /** A row; it is a horizontal box with the paragraph's alignment. */
    protected View createRow() {
        return new Row(getElement(), this);
    }

    /** Where the next tab falls, according to the paragraph's stops. */
    public float nextTabStop(float x, int tabOffset) {
        if (tabSet == null) {
            // With no stops of its own: every half inch, like the JDK.
            float tabBase = getTabBase();
            float defaultWidth = 36;
            int ntabs = (int) ((x - tabBase) / defaultWidth);
            return tabBase + ((ntabs + 1) * defaultWidth);
        }
        float tabBase = getTabBase();
        TabStop tab = tabSet.getTabAfter(x - tabBase + 0.01f);
        if (tab == null) {
            return tabBase + ((int) ((x - tabBase) / 36) + 1) * 36;
        }
        int alignment = tab.getAlignment();
        if (alignment == TabStop.ALIGN_LEFT || alignment == TabStop.ALIGN_BAR) {
            return tabBase + tab.getPosition();
        }
        // The other alignments need to measure what is coming: it measures up to the next break.
        int p = getPartialLength(tabOffset, alignment);
        float tabPos = tabBase + tab.getPosition();
        if (alignment == TabStop.ALIGN_RIGHT || alignment == TabStop.ALIGN_DECIMAL) {
            return Math.max(x, tabPos - p);
        }
        return Math.max(x, tabPos - (p / 2));
    }

    /** How much what follows up to the next break takes up, for aligning a tab. */
    private int getPartialLength(int tabOffset, int alignment) {
        char[] splits = (alignment == TabStop.ALIGN_DECIMAL) ? tabDecimalChars : tabChars;
        int to = findOffsetToCharactersInString(splits, tabOffset + 1);
        if (to == -1) {
            to = getEndOffset();
        }
        return (int) getPartialSize(tabOffset + 1, to);
    }

    protected TabSet getTabSet() {
        return StyleConstants.getTabSet(getElement().getAttributes());
    }

    /** How much that stretch of the paragraph takes up. */
    protected float getPartialSize(int startOffset, int endOffset) {
        float size = 0;
        int viewIndex;
        int numViews = getViewCount();
        View view;
        int viewEnd;
        int tempEnd;

        viewIndex = getElement().getElementIndex(startOffset);
        numViews = getLayoutViewCount();
        while (startOffset < endOffset && viewIndex < numViews) {
            view = getLayoutView(viewIndex);
            viewEnd = view.getEndOffset();
            tempEnd = Math.min(endOffset, viewEnd);
            if (view instanceof TabableView) {
                size = size + ((TabableView) view).getPartialSpan(startOffset, tempEnd);
            } else if (startOffset == view.getStartOffset() && tempEnd == viewEnd) {
                size = size + view.getPreferredSpan(View.X_AXIS);
            }
            startOffset = viewEnd;
            viewIndex = viewIndex + 1;
        }
        return size;
    }

    /** Where the first of those characters is from that position on. */
    protected int findOffsetToCharactersInString(char[] string, int start) {
        int stringLength = string.length;
        int end = getEndOffset();
        Segment seg = new Segment();
        try {
            getDocument().getText(start, end - start, seg);
        } catch (BadLocationException ble) {
            return -1;
        }
        int maxCounter = seg.offset + seg.count;
        for (int counter = seg.offset; counter < maxCounter; counter++) {
            char currentChar = seg.array[counter];
            for (int subCounter = 0; subCounter < stringLength; subCounter++) {
                if (currentChar == string[subCounter]) {
                    return counter - seg.offset + start;
                }
            }
        }
        return -1;
    }

    /** Where the tabs are counted from: the paragraph's left edge. */
    protected float getTabBase() {
        return (float) (getLeftInset() + firstLineIndent);
    }

    public void paint(Graphics g, Shape a) {
        Rectangle alloc = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
        super.paint(g, a);
    }

    /** A paragraph is aligned at the top vertically. */
    public float getAlignment(int axis) {
        if (axis == View.Y_AXIS) {
            return 0;
        }
        return 0.5f;
    }

    /** A paragraph does not split: its rows are already the break. */
    public View breakView(int axis, float len, Shape a) {
        if (axis == View.Y_AXIS) {
            if (a != null) {
                Rectangle alloc = a.getBounds();
                setSize(alloc.width, alloc.height);
            }
            return this;
        }
        return this;
    }

    public int getBreakWeight(int axis, float len) {
        return BadBreakWeight;
    }

    /** The horizontal minimum is that of the longest word. */
    protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
        r = super.calculateMinorAxisRequirements(axis, r);
        float insets = getLeftInset() + getRightInset();
        r.minimum = (int) (r.minimum + insets + firstLineIndent);
        r.preferred = (int) (r.preferred + insets + firstLineIndent);
        r.maximum = Integer.MAX_VALUE;
        return r;
    }

    /** The attributes changed: indents and alignment have to be read again. */
    public void changedUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        setPropertiesFromAttributes();
        layoutChanged(X_AXIS);
        layoutChanged(Y_AXIS);
        super.changedUpdate(changes, a, f);
    }

    /**
     * A row of the paragraph: a horizontal box.
     *
     * <p>It is private in the JDK and here too. Its horizontal alignment is the paragraph's, and
     * hence a centred paragraph has centred rows without the row knowing anything.
     */
    static class Row extends BoxView {

        private final ParagraphView paragraph;

        Row(Element elem, ParagraphView paragraph) {
            super(elem, View.X_AXIS);
            this.paragraph = paragraph;
        }

        /** A row has no margins of its own: the paragraph's have already been applied. */
        protected void loadChildren(ViewFactory f) {
        }

        public AttributeSet getAttributes() {
            View p = getParent();
            return (p != null) ? p.getAttributes() : null;
        }

        public float getAlignment(int axis) {
            if (axis == View.X_AXIS) {
                int j = (paragraph != null) ? paragraph.justification : StyleConstants.ALIGN_LEFT;
                if (j == StyleConstants.ALIGN_LEFT) {
                    return 0;
                }
                if (j == StyleConstants.ALIGN_RIGHT) {
                    return 1;
                }
                if (j == StyleConstants.ALIGN_CENTER) {
                    return 0.5f;
                }
                return 0;
            }
            return super.getAlignment(axis);
        }

        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            Rectangle r = a.getBounds();
            View v = getViewAtPosition(pos, r);
            if ((v != null) && (!v.getElement().isLeaf())) {
                // A row's line ending is not drawn: the position falls at the end.
                return super.modelToView(pos, a, b);
            }
            r = a.getBounds();
            int height = r.height;
            int y = r.y;
            Shape loc = super.modelToView(pos, a, b);
            r = loc.getBounds();
            r.height = height;
            r.y = y;
            return r;
        }

        public int getStartOffset() {
            int offs = Integer.MAX_VALUE;
            int n = getViewCount();
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                offs = Math.min(offs, v.getStartOffset());
            }
            return offs;
        }

        public int getEndOffset() {
            int offs = 0;
            int n = getViewCount();
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                offs = Math.max(offs, v.getEndOffset());
            }
            return offs;
        }

        /** The rows are aligned by their baseline. */
        protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
            baselineLayout(targetSpan, axis, offsets, spans);
        }

        protected SizeRequirements calculateMinorAxisRequirements(int axis,
                SizeRequirements r) {
            return baselineRequirements(axis, r);
        }

        protected int getViewIndexAtPosition(int pos) {
            if (pos < getStartOffset() || pos >= getEndOffset()) {
                return -1;
            }
            for (int counter = getViewCount() - 1; counter >= 0; counter--) {
                View v = getView(counter);
                if (pos >= v.getStartOffset() && pos < v.getEndOffset()) {
                    return counter;
                }
            }
            return -1;
        }
    }
}
