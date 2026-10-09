package javax.swing.text;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent$ElementChange;
import javax.swing.event.DocumentEvent$EventType;

/**
 * How a piece of the document looks: the visual half of the text's model-view.
 *
 * <h2>A parallel tree</h2>
 *
 * <p>The document has its tree of {@link Element}s; the view builds another tree on top, with
 * one view per element. They are two trees and not one because they do not always correspond: a
 * long paragraph is seen as several lines, and each line is a view, but in the document it is
 * still a single element. That freedom is the whole point of this class.
 *
 * <h2>The two questions</h2>
 *
 * <p>A view has to know how to answer two things, and they are inverses:
 *
 * <ul>
 * <li>{@link #modelToView}: where this position of the text falls on the screen. It is what
 * places the cursor.
 * <li>{@link #viewToModel}: which position of the text is at this point of the screen. It is what
 * makes a click take the cursor where one was looking.
 * </ul>
 *
 * <p>The {@link Position.Bias} both carry is the answer to a real ambiguity: the point between
 * two characters belongs to the one on the left or to the one on the right, and at a line ending
 * or at a change of writing direction that falls in two different places on the screen.
 *
 * <h2>How it copes with the changes</h2>
 *
 * <p>When the document changes, the notice goes down the tree: {@link #insertUpdate} and its
 * siblings see whether the change touched their structure ({@link #updateChildren}) and then
 * forward it to the children that span the changed stretch ({@link #forwardUpdate}). A view
 * whose shape did not change does nothing, and that is why typing a letter does not redo the
 * whole document.
 *
 * <h2>The weights</h2>
 *
 * <p>{@link #getBreakWeight} says how well a view can be broken at a point --a space breaks
 * better than the middle of a word-- and {@link #getResizeWeight} how willing it is to stretch.
 * Both are numbers and not yes/no because whoever arranges has to choose the best of several
 * evils.
 */
public abstract class View implements SwingConstants {

    /** It cannot be broken here. */
    public static final int BadBreakWeight = 0;

    /** It can be broken, but badly. */
    public static final int GoodBreakWeight = 1000;

    /** It is a good place to break. */
    public static final int ExcellentBreakWeight = 2000;

    /** It has to be broken here no matter what; it is what a line ending returns. */
    public static final int ForcedBreakWeight = 3000;

    public static final int X_AXIS = HORIZONTAL;

    public static final int Y_AXIS = VERTICAL;

    /** A one-element array for returning the bias without allocating memory. */
    static final Position.Bias[] sharedBiasReturn = new Position.Bias[1];

    private View parent;
    private Element elem;

    /** The first and the last child the last change touched; {@link #forwardUpdate} uses them. */
    int firstUpdateIndex;
    int lastUpdateIndex;

    /** A view of that element, still with no parent. */
    public View(Element elem) {
        this.elem = elem;
    }

    public View getParent() {
        return parent;
    }

    /** Whether it is shown; a view with no parent is considered visible. */
    public boolean isVisible() {
        return true;
    }

    /** How much it would like to measure on that axis, in pixels. */
    public abstract float getPreferredSpan(int axis);

    /** The least it can measure; by default, the preferred one. */
    public float getMinimumSpan(int axis) {
        int w = getResizeWeight(axis);
        if (w == 0) {
            return getPreferredSpan(axis);
        }
        return 0;
    }

    /** The most it can measure; with no cap if it can stretch. */
    public float getMaximumSpan(int axis) {
        int w = getResizeWeight(axis);
        if (w == 0) {
            return getPreferredSpan(axis);
        }
        return Integer.MAX_VALUE;
    }

    /**
     * It tells the parent that a child changed size.
     *
     * <p>It goes up the tree until somebody decides to redo the layout. That it goes up instead of
     * the parent asking is what makes a local change cost what that change costs and not what the
     * document costs.
     */
    public void preferenceChanged(View child, boolean width, boolean height) {
        View parent = getParent();
        if (parent != null) {
            parent.preferenceChanged(this, width, height);
        }
    }

    /** Where its attachment line falls on that axis, from 0 to 1; in the middle by default. */
    public float getAlignment(int axis) {
        return 0.5f;
    }

    /** It is drawn in that shape, which is the place it got. */
    public abstract void paint(Graphics g, Shape allocation);

    /**
     * It sets its parent, or takes it away with {@code null}.
     *
     * <p>Taking it away is what dismantles the subtree: each view takes the parent away from its
     * children, and that way the upward references are released.
     */
    public void setParent(View parent) {
        if (parent == null) {
            for (int i = 0; i < getViewCount(); i++) {
                if (getView(i).getParent() == this) {
                    getView(i).setParent(null);
                }
            }
        }
        this.parent = parent;
    }

    /** How many child views it has; zero if it is a leaf. */
    public int getViewCount() {
        return 0;
    }

    public View getView(int n) {
        return null;
    }

    public void removeAll() {
        replace(0, getViewCount(), null);
    }

    public void remove(int i) {
        replace(i, 1, null);
    }

    public void insert(int offs, View v) {
        View[] one = new View[1];
        one[0] = v;
        replace(offs, 0, one);
    }

    public void append(View v) {
        View[] one = new View[1];
        one[0] = v;
        replace(getViewCount(), 0, one);
    }

    /** It swaps a stretch of children for another; a view with no children does nothing. */
    public void replace(int offset, int length, View[] views) {
    }

    /** Which child covers that position of the document, or {@code -1}. */
    public int getViewIndex(int pos, Position.Bias b) {
        return -1;
    }

    /** Which part of its place that child gets. */
    public Shape getChildAllocation(int index, Shape a) {
        return null;
    }

    /**
     * Where the cursor goes from that position in that direction.
     *
     * <p>It is the one that makes the keyboard arrows move by what is seen and not by how it is
     * stored: going down one line is a different jump on each line.
     */
    public int getNextVisualPositionFrom(int pos, Position.Bias b, Shape a, int direction,
            Position.Bias[] biasRet) throws BadLocationException {
        biasRet[0] = Position.Bias.Forward;
        if (direction == NORTH || direction == SOUTH) {
            return pos;
        }
        if (direction == WEST) {
            return Math.max(getStartOffset(), pos - 1);
        }
        if (direction == EAST) {
            return Math.min(getEndOffset() - 1, pos + 1);
        }
        throw new IllegalArgumentException("Bad direction: " + direction);
    }

    /** Where that position of the document falls; see the class note. */
    public abstract Shape modelToView(int pos, Shape a, Position.Bias b)
            throws BadLocationException;

    /**
     * The region the two positions take up together.
     *
     * <p>The union of the two, and that is why a selection crossing lines gives a rectangle that
     * covers both: whoever paints the selection has to walk the lines, not trust this rectangle.
     */
    public Shape modelToView(int p0, Position.Bias b0, int p1, Position.Bias b1, Shape a)
            throws BadLocationException {
        Shape s0 = modelToView(p0, a, b0);
        Shape s1;
        if (p1 == getEndOffset()) {
            try {
                s1 = modelToView(p1, a, b1);
            } catch (BadLocationException ble) {
                s1 = null;
            }
            if (s1 == null) {
                Rectangle alloc = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
                s1 = new Rectangle(alloc.x + alloc.width - 1, alloc.y, 1, alloc.height);
            }
        } else {
            s1 = modelToView(p1, a, b1);
        }
        Rectangle r0 = (s0 instanceof Rectangle) ? (Rectangle) s0 : s0.getBounds();
        Rectangle r1 = (s1 instanceof Rectangle) ? (Rectangle) s1 : s1.getBounds();
        if (r0.y != r1.y) {
            // Two different lines: the rectangle covers from one to the other, full width.
            Rectangle alloc = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
            r0.x = alloc.x;
            r0.width = alloc.width;
        }
        r0.add(r1);
        return r0;
    }

    /** Which position of the document is at that point; see the class note. */
    public abstract int viewToModel(float x, float y, Shape a, Position.Bias[] biasReturn);

    /**
     * Text was inserted.
     *
     * <p>The three notices do the same thing under different names: see whether the structure
     * changed and forward to the children it touches.
     */
    public void insertUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        if (getViewCount() > 0) {
            Element elem = getElement();
            DocumentEvent$ElementChange ec = e.getChange(elem);
            if (ec != null) {
                if (!updateChildren(ec, e, f)) {
                    ec = null;
                }
            }
            forwardUpdate(ec, e, a, f);
            updateLayout(ec, e, a);
        }
    }

    public void removeUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        if (getViewCount() > 0) {
            Element elem = getElement();
            DocumentEvent$ElementChange ec = e.getChange(elem);
            if (ec != null) {
                if (!updateChildren(ec, e, f)) {
                    ec = null;
                }
            }
            forwardUpdate(ec, e, a, f);
            updateLayout(ec, e, a);
        }
    }

    public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        if (getViewCount() > 0) {
            Element elem = getElement();
            DocumentEvent$ElementChange ec = e.getChange(elem);
            if (ec != null) {
                if (!updateChildren(ec, e, f)) {
                    ec = null;
                }
            }
            forwardUpdate(ec, e, a, f);
            updateLayout(ec, e, a);
        }
    }

    public Document getDocument() {
        return elem.getDocument();
    }

    public int getStartOffset() {
        return elem.getStartOffset();
    }

    public int getEndOffset() {
        return elem.getEndOffset();
    }

    public Element getElement() {
        return elem;
    }

    /** The context to draw in, from the component; {@code null} with no component. */
    public Graphics getGraphics() {
        Container c = getContainer();
        if (c != null) {
            return c.getGraphics();
        }
        return null;
    }

    public AttributeSet getAttributes() {
        return elem.getAttributes();
    }

    /**
     * It splits at that point to fit that room.
     *
     * <p>It returns {@code this} if it does not know how to split itself, which is what an atomic
     * view does. Whoever calls it has to be ready for that: it is not always possible to break.
     */
    public View breakView(int axis, int offset, float pos, float len) {
        return this;
    }

    /** A view of a piece of this element; {@code this} if it does not know how to do it. */
    public View createFragment(int p0, int p1) {
        return this;
    }

    /** How well it breaks at that point; see the class note. */
    public int getBreakWeight(int axis, float pos, float len) {
        if (len > getPreferredSpan(axis)) {
            return GoodBreakWeight;
        }
        return BadBreakWeight;
    }

    /** How willing it is to change size; zero is "in no way". */
    public int getResizeWeight(int axis) {
        return 0;
    }

    /**
     * It is given that size.
     *
     * <p>By default it does nothing: a view that arranges itself redefines it. Receiving a size is
     * not the same as asking for one, and a view may ignore it.
     */
    public void setSize(float width, float height) {
    }

    /** The component it is drawn in; it goes up the tree until it finds it. */
    public Container getContainer() {
        View v = getParent();
        return (v != null) ? v.getContainer() : null;
    }

    /** Who builds the child views; it goes up the tree until it finds it. */
    public ViewFactory getViewFactory() {
        View v = getParent();
        return (v != null) ? v.getViewFactory() : null;
    }

    /** The tooltip text at that point; that of the child that contains it. */
    public String getToolTipText(float x, float y, Shape allocation) {
        int viewIndex = getViewIndex(x, y, allocation);
        if (viewIndex >= 0) {
            allocation = getChildAllocation(viewIndex, allocation);
            Rectangle rect = (allocation instanceof Rectangle) ? (Rectangle) allocation
                    : allocation.getBounds();
            if (rect.contains(x, y)) {
                return getView(viewIndex).getToolTipText(x, y, allocation);
            }
        }
        return null;
    }

    /** Which child is at that point, or {@code -1}. */
    public int getViewIndex(float x, float y, Shape allocation) {
        return -1;
    }

    /**
     * It redoes the children the structure change affected.
     *
     * <p>It returns whether anything really changed. It is what avoids forwarding a notice to
     * children that no longer exist.
     */
    protected boolean updateChildren(DocumentEvent$ElementChange ec, DocumentEvent e,
            ViewFactory f) {
        Element[] removedElems = ec.getChildrenRemoved();
        Element[] addedElems = ec.getChildrenAdded();
        View[] added = null;
        if (addedElems != null) {
            added = new View[addedElems.length];
            for (int i = 0; i < addedElems.length; i++) {
                added[i] = f.create(addedElems[i]);
            }
        }
        int nremoved = 0;
        int index = ec.getIndex();
        if (removedElems != null) {
            nremoved = removedElems.length;
        }
        replace(index, nremoved, added);
        return true;
    }

    /**
     * It forwards the notice to the children that span the changed stretch.
     *
     * <p>Only to those: walking them all would cost the same as redoing the document.
     */
    protected void forwardUpdate(DocumentEvent$ElementChange ec, DocumentEvent e, Shape a,
            ViewFactory f) {
        calculateUpdateIndexes(e);

        int hole0 = lastUpdateIndex + 1;
        int hole1 = hole0;
        Element[] addedElems = (ec != null) ? ec.getChildrenAdded() : null;
        if ((addedElems != null) && (addedElems.length > 0)) {
            hole0 = ec.getIndex();
            hole1 = hole0 + addedElems.length - 1;
        }

        // The new children are already built: they do not need to be told.
        for (int i = firstUpdateIndex; i <= lastUpdateIndex; i++) {
            if (!((i >= hole0) && (i <= hole1))) {
                View v = getView(i);
                if (v != null) {
                    Shape childAlloc = getChildAllocation(i, a);
                    forwardUpdateToView(v, e, childAlloc, f);
                }
            }
        }
        lastUpdateIndex = Math.max(lastUpdateIndex - 1, 0);
        firstUpdateIndex = Math.min(firstUpdateIndex, lastUpdateIndex);
    }

    /** Which children the change touches; it leaves the range in the two fields. */
    void calculateUpdateIndexes(DocumentEvent e) {
        int pos = e.getOffset();
        firstUpdateIndex = getViewIndex(pos, Position.Bias.Forward);
        if (firstUpdateIndex == -1 && e.getType() == javax.swing.event.DocumentEvent$EventType.REMOVE
                && pos >= getEndOffset()) {
            firstUpdateIndex = getViewCount() - 1;
        }
        lastUpdateIndex = firstUpdateIndex;
        View v = (firstUpdateIndex >= 0) ? getView(firstUpdateIndex) : null;
        if ((v != null) && (v.getEndOffset() == pos)) {
            // Right at the edge: the change touches the neighbouring child too.
            lastUpdateIndex = Math.min(firstUpdateIndex + 1, getViewCount() - 1);
        }
    }

    /** It leaves the indices as if the change had already been applied. */
    void updateAfterChange() {
    }

    protected void forwardUpdateToView(View v, DocumentEvent e, Shape a, ViewFactory f) {
        DocumentEvent$EventType type = e.getType();
        if (type == DocumentEvent$EventType.INSERT) {
            v.insertUpdate(e, a, f);
        } else if (type == DocumentEvent$EventType.REMOVE) {
            v.removeUpdate(e, a, f);
        } else {
            v.changedUpdate(e, a, f);
        }
    }

    /** It asks for the layout to be redone if the change warrants it. */
    protected void updateLayout(DocumentEvent$ElementChange ec, DocumentEvent e, Shape a) {
        if ((ec != null) && (a != null)) {
            preferenceChanged(null, true, true);
            Container host = getContainer();
            if (host != null) {
                host.repaint();
            }
        }
    }

    /** @deprecated it is {@link #modelToView(int, Shape, Position.Bias)} with a forward bias. */
    @Deprecated
    public Shape modelToView(int pos, Shape a) throws BadLocationException {
        return modelToView(pos, a, Position.Bias.Forward);
    }

    /** @deprecated it is {@link #viewToModel(float, float, Shape, Position.Bias[])}. */
    @Deprecated
    public int viewToModel(float x, float y, Shape a) {
        sharedBiasReturn[0] = Position.Bias.Forward;
        return viewToModel(x, y, a, sharedBiasReturn);
    }
}
