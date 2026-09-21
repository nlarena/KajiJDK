package javax.swing.text;

import java.awt.Rectangle;
import java.awt.Shape;

/**
 * A view with children: the base of everything that groups.
 *
 * <h2>What it solves, and what it leaves open</h2>
 *
 * <p>It keeps the children, creates them from the document's elements ({@link #loadChildren})
 * and translates between model and screen by delegating to the child that corresponds. What it
 * does <em>not</em> decide is how they are placed: that is answered by {@link #childAllocation},
 * {@link #isBefore} and {@link #isAfter}, which each subclass implements. Putting the children in
 * a row, in a column or in a grid is the only difference between the subclasses.
 *
 * <h2>The margins</h2>
 *
 * <p>The four insets are {@code short} and not {@code int}: a document may have thousands of
 * views and every field counts. {@link #getInsideAllocation} is the one that subtracts the
 * margins and leaves the rectangle where the children really go.
 *
 * <h2>Moving with the arrows</h2>
 *
 * <p>{@link #getNextVisualPositionFrom} splits in two: up and down are resolved by the parent
 * --the child has to change-- and left and right by the child while it can. That division is what
 * makes going down one line work the same in a paragraph as in a table.
 */
public abstract class CompositeView extends View {

    private View[] children;
    private int nchildren;
    private short left;
    private short right;
    private short top;
    private short bottom;
    private Rectangle childAlloc;

    /** A view of that element, with no children yet. */
    public CompositeView(Element elem) {
        super(elem);
        children = new View[1];
        nchildren = 0;
        childAlloc = new Rectangle();
    }

    /**
     * It creates the child views, one per child element.
     *
     * <p>It is called by itself the first time the view has a parent: until that moment there is no
     * factory to ask them for.
     */
    protected void loadChildren(ViewFactory f) {
        if (f == null) {
            return;
        }
        Element e = getElement();
        int n = e.getElementCount();
        if (n > 0) {
            View[] added = new View[n];
            for (int i = 0; i < n; i++) {
                added[i] = f.create(e.getElement(i));
            }
            replace(0, 0, added);
        }
    }

    /** On having a parent for the first time, it loads the children. */
    public void setParent(View parent) {
        super.setParent(parent);
        if ((parent != null) && (nchildren == 0)) {
            ViewFactory f = getViewFactory();
            loadChildren(f);
        }
    }

    public int getViewCount() {
        return nchildren;
    }

    public View getView(int n) {
        return children[n];
    }

    /** It swaps a stretch of children for another; those that leave have their parent removed. */
    public void replace(int offset, int length, View[] views) {
        if (views == null) {
            views = new View[0];
        }

        for (int i = offset; i < offset + length; i++) {
            if (children[i].getParent() == this) {
                children[i].setParent(null);
            }
            children[i] = null;
        }

        int delta = views.length - length;
        int src = offset + length;
        int nmove = nchildren - src;
        int dest = src + delta;
        if ((nchildren + delta) >= children.length) {
            int newLength = Math.max(2 * children.length, nchildren + delta);
            View[] newChildren = new View[newLength];
            System.arraycopy(children, 0, newChildren, 0, offset);
            System.arraycopy(views, 0, newChildren, offset, views.length);
            System.arraycopy(children, src, newChildren, dest, nmove);
            children = newChildren;
        } else {
            System.arraycopy(children, src, children, dest, nmove);
            System.arraycopy(views, 0, children, offset, views.length);
        }
        nchildren = nchildren + delta;

        for (int i = 0; i < views.length; i++) {
            views[i].setParent(this);
        }
    }

    /** That child's place, with the margins already subtracted. */
    public Shape getChildAllocation(int index, Shape a) {
        Rectangle alloc = getInsideAllocation(a);
        childAllocation(index, alloc);
        return alloc;
    }

    /** Where that position falls: in the child that contains it. */
    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        boolean isBackward = (b == Position.Bias.Backward);
        int testPos = (isBackward) ? Math.max(0, pos - 1) : pos;
        if (isBackward && testPos < getStartOffset()) {
            return null;
        }
        int vIndex = getViewIndexAtPosition(testPos);
        if ((vIndex != -1) && (vIndex < getViewCount())) {
            View v = getView(vIndex);
            if (v != null && testPos >= v.getStartOffset() && testPos < v.getEndOffset()) {
                Shape childShape = getChildAllocation(vIndex, a);
                if (childShape == null) {
                    return null;
                }
                Shape retShape = v.modelToView(pos, childShape, b);
                if (retShape == null && v.getEndOffset() == pos) {
                    // A child's end is the next one's beginning.
                    if (++vIndex < getViewCount()) {
                        v = getView(vIndex);
                        retShape = v.modelToView(pos, getChildAllocation(vIndex, a), b);
                    }
                }
                return retShape;
            }
        }
        throw new BadLocationException("Position not represented by view", pos);
    }

    /** The region of the two positions; if they fall in the same child it is asked of it. */
    public Shape modelToView(int p0, Position.Bias b0, int p1, Position.Bias b1, Shape a)
            throws BadLocationException {
        if (p0 == getStartOffset() && p1 == getEndOffset()) {
            return a;
        }
        Rectangle alloc = getInsideAllocation(a);
        Rectangle r0 = new Rectangle(alloc);
        View v0 = getViewAtPosition((b0 == Position.Bias.Backward) ? Math.max(0, p0 - 1) : p0, r0);
        Rectangle r1 = new Rectangle(alloc);
        View v1 = getViewAtPosition((b1 == Position.Bias.Backward) ? Math.max(0, p1 - 1) : p1, r1);
        if (v0 == v1) {
            if (v0 == null) {
                return a;
            }
            return v0.modelToView(p0, b0, p1, b1, r0);
        }
        // In different children: the union of the two places.
        Shape r = (v0 != null) ? v0.modelToView(p0, b0, v0.getEndOffset(),
                Position.Bias.Backward, r0) : a;
        Rectangle rr = (r instanceof Rectangle) ? (Rectangle) r : r.getBounds();
        if (v1 != null) {
            Shape r1s = v1.modelToView(v1.getStartOffset(), Position.Bias.Forward, p1, b1, r1);
            Rectangle rr1 = (r1s instanceof Rectangle) ? (Rectangle) r1s : r1s.getBounds();
            rr.add(rr1);
        }
        return rr;
    }

    /** Which position is at that point: whatever the child that contains it says. */
    public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
        Rectangle alloc = getInsideAllocation(a);
        if (isBefore((int) x, (int) y, alloc)) {
            // Before the beginning: the first position.
            int retValue = -1;
            try {
                View v = getViewAtPoint((int) x, (int) y, alloc);
                if (v != null) {
                    retValue = v.getStartOffset();
                }
            } catch (Exception e) {
                retValue = -1;
            }
            if (retValue == -1) {
                retValue = getStartOffset();
                bias[0] = Position.Bias.Forward;
            }
            return retValue;
        } else if (isAfter((int) x, (int) y, alloc)) {
            int retValue = -1;
            try {
                View v = getViewAtPoint((int) x, (int) y, alloc);
                if (v != null) {
                    retValue = v.getEndOffset() - 1;
                }
            } catch (Exception e) {
                retValue = -1;
            }
            if (retValue == -1) {
                retValue = Math.max(0, getEndOffset() - 1);
                bias[0] = Position.Bias.Forward;
            }
            return retValue;
        } else {
            View v = getViewAtPoint((int) x, (int) y, alloc);
            if (v != null) {
                return v.viewToModel(x, y, alloc, bias);
            }
        }
        return -1;
    }

    /** See the class note about why it splits in two. */
    public int getNextVisualPositionFrom(int pos, Position.Bias b, Shape a, int direction,
            Position.Bias[] biasRet) throws BadLocationException {
        if (pos < -1) {
            throw new BadLocationException("invalid position", pos);
        }
        Rectangle alloc = getInsideAllocation(a);

        if (direction == NORTH || direction == SOUTH) {
            return getNextNorthSouthVisualPositionFrom(pos, b, a, direction, biasRet);
        }
        if (direction == EAST || direction == WEST) {
            return getNextEastWestVisualPositionFrom(pos, b, a, direction, biasRet);
        }
        throw new IllegalArgumentException("Bad direction: " + direction);
    }

    /** The child that covers that position, or {@code -1}. */
    public int getViewIndex(int pos, Position.Bias b) {
        if (b == Position.Bias.Backward) {
            pos = pos - 1;
        }
        if ((pos >= getStartOffset()) && (pos < getEndOffset())) {
            return getViewIndexAtPosition(pos);
        }
        return -1;
    }

    /** Whether that point is before the region's beginning; each subclass answers it. */
    protected abstract boolean isBefore(int x, int y, Rectangle alloc);

    protected abstract boolean isAfter(int x, int y, Rectangle alloc);

    protected abstract View getViewAtPoint(int x, int y, Rectangle alloc);

    /** It leaves that child's place in the rectangle; each subclass answers it. */
    protected abstract void childAllocation(int index, Rectangle a);

    /** The child that covers that position, with its place put in the rectangle. */
    protected View getViewAtPosition(int pos, Rectangle a) {
        int index = getViewIndexAtPosition(pos);
        if ((index >= 0) && (index < getViewCount())) {
            View v = getView(index);
            if (a != null) {
                childAllocation(index, a);
            }
            return v;
        }
        return null;
    }

    /**
     * Which child corresponds to that position.
     *
     * <p>The <em>element</em> answers it, not the children. It looks like a detour with the
     * children at hand, but it is what makes the answer the same before and after building them: a
     * view that has not created its children yet, or that discarded them, still knows where each
     * position would fall. Searching among the children, a half-built view would answer that the
     * position does not exist.
     *
     * <p>Whoever really has children that do not follow the elements one to one overrides it; it is
     * what {@link ZoneView} does.
     */
    protected int getViewIndexAtPosition(int pos) {
        Element elem = getElement();
        return elem.getElementIndex(pos);
    }

    /** The inner rectangle: the place minus the margins. */
    protected Rectangle getInsideAllocation(Shape a) {
        if (a != null) {
            Rectangle alloc;
            if (a instanceof Rectangle) {
                alloc = (Rectangle) a;
            } else {
                alloc = a.getBounds();
            }
            childAlloc.setBounds(alloc);
            childAlloc.x = childAlloc.x + getLeftInset();
            childAlloc.y = childAlloc.y + getTopInset();
            childAlloc.width = childAlloc.width - getLeftInset() - getRightInset();
            childAlloc.height = childAlloc.height - getTopInset() - getBottomInset();
            return childAlloc;
        }
        return null;
    }

    /** It takes the margins from the paragraph attributes: indents and space before and after. */
    protected void setParagraphInsets(AttributeSet attr) {
        top = (short) StyleConstants.getSpaceAbove(attr);
        left = (short) StyleConstants.getLeftIndent(attr);
        bottom = (short) StyleConstants.getSpaceBelow(attr);
        right = (short) StyleConstants.getRightIndent(attr);
    }

    protected void setInsets(short top, short left, short bottom, short right) {
        this.top = top;
        this.left = left;
        this.right = right;
        this.bottom = bottom;
    }

    protected short getLeftInset() {
        return left;
    }

    protected short getRightInset() {
        return right;
    }

    protected short getTopInset() {
        return top;
    }

    protected short getBottomInset() {
        return bottom;
    }

    /**
     * Going up or down one line.
     *
     * <p>By default it does not move: a view that does not stack children vertically does not know
     * what "the line above" is. Vertical {@code BoxView} and {@code ParagraphView} redefine it.
     */
    protected int getNextNorthSouthVisualPositionFrom(int pos, Position.Bias b, Shape a,
            int direction, Position.Bias[] biasRet) throws BadLocationException {
        return nextInChildren(pos, b, a, direction, biasRet);
    }

    /**
     * One character to the left or to the right.
     *
     * <p>It is asked of the child that has the position; if the child says it has run out, it moves
     * on to the one beside it. It is what makes the right arrow jump from one word to the next
     * without anybody having to know where each one ends.
     */
    protected int getNextEastWestVisualPositionFrom(int pos, Position.Bias b, Shape a,
            int direction, Position.Bias[] biasRet) throws BadLocationException {
        return nextInChildren(pos, b, a, direction, biasRet);
    }

    /**
     * It asks the child that has the current position for the next one, and if it runs out, the
     * one beside it.
     *
     * <p>The {@code -1} as a position means "I come from outside": the child starts at its tip.
     * It is how one goes from one child to the next without either having to know about the other.
     */
    private int nextInChildren(int pos, Position.Bias b, Shape a, int direction,
            Position.Bias[] biasRet) throws BadLocationException {
        if (getViewCount() == 0) {
            return pos;
        }
        boolean backwards = (direction == NORTH || direction == WEST);
        int retValue;
        if (pos == -1) {
            int childIndex = backwards ? getViewCount() - 1 : 0;
            View child = getView(childIndex);
            Shape childBounds = getChildAllocation(childIndex, a);
            retValue = child.getNextVisualPositionFrom(pos, b, childBounds, direction, biasRet);
        } else {
            int increment = backwards ? -1 : 1;
            int childIndex;
            if (b == Position.Bias.Backward && pos > 0) {
                childIndex = getViewIndex(pos - 1, Position.Bias.Forward);
            } else {
                childIndex = getViewIndex(pos, Position.Bias.Forward);
            }
            if (childIndex < 0) {
                return pos;
            }
            View child = getView(childIndex);
            Shape childBounds = getChildAllocation(childIndex, a);
            retValue = child.getNextVisualPositionFrom(pos, b, childBounds, direction, biasRet);
            if ((direction == EAST || direction == WEST) && flipEastAndWestAtEnds(pos, b)) {
                increment = increment * -1;
            }
            childIndex = childIndex + increment;
            if (retValue == -1 && childIndex >= 0 && childIndex < getViewCount()) {
                child = getView(childIndex);
                childBounds = getChildAllocation(childIndex, a);
                retValue = child.getNextVisualPositionFrom(-1, b, childBounds, direction,
                        biasRet);
            }
        }
        return retValue;
    }

    /**
     * Whether at the ends left and right have to be swapped.
     *
     * <p>It is needed in text read from right to left: there "the next one to the right" is the
     * previous one in the document. Without bidirectional analysis, always {@code false}.
     */
    protected boolean flipEastAndWestAtEnds(int position, Position.Bias bias) {
        return false;
    }
}
