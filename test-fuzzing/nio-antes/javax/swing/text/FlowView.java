package javax.swing.text;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Vector;

import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;

/**
 * A view that shares its content out into rows: the base of a paragraph with line breaking.
 *
 * <h2>Two view trees for the same thing</h2>
 *
 * <p>There is a <em>logical</em> tree --one view per document element, in
 * {@link #layoutPool}-- and a <em>physical</em> one, which are the rows that are seen. The
 * logical one does not change when the width changes; the physical one is rebuilt whole. Having
 * both is what allows breaking into lines again without creating a view per stretch of text
 * again.
 *
 * <p>Who decides where to break is the {@link FlowStrategy}, which is outside the view and can
 * be changed: breaking by words, by characters or in another way is changing that object.
 */
public abstract class FlowView extends BoxView {

    /** The width available for each row. */
    protected int layoutSpan;

    /** The logical tree; see the class note. */
    protected View layoutPool;

    protected FlowStrategy strategy;

    public FlowView(Element elem, int axis) {
        super(elem, axis);
        layoutSpan = Integer.MAX_VALUE;
        strategy = new FlowStrategy();
    }

    /** The axis the content flows on: the one perpendicular to the stacking one. */
    public int getFlowAxis() {
        if (getAxis() == Y_AXIS) {
            return X_AXIS;
        }
        return Y_AXIS;
    }

    /** How much room that row has; all the same, unless a subclass says otherwise. */
    public int getFlowSpan(int index) {
        return layoutSpan;
    }

    /** Where that row starts. */
    public int getFlowStart(int index) {
        return 0;
    }

    /** An empty row; the subclass decides of what kind. */
    protected abstract View createRow();

    /**
     * It builds the logical tree and leaves the physical one empty.
     *
     * <p>The rows are created only when laying out, when the width is known: before that it cannot
     * be known how many are needed.
     */
    protected void loadChildren(ViewFactory f) {
        if (layoutPool == null) {
            layoutPool = new LogicalView(getElement());
        }
        layoutPool.setParent(this);
        int p0 = getStartOffset();
        int p1 = getEndOffset();
        strategy.insertUpdate(this, null, null);
    }

    protected int getViewIndexAtPosition(int pos) {
        if (pos >= getStartOffset() && (pos < getEndOffset())) {
            for (int counter = 0; counter < getViewCount(); counter++) {
                View v = getView(counter);
                if (pos >= v.getStartOffset() && pos < v.getEndOffset()) {
                    return counter;
                }
            }
        }
        return -1;
    }

    /** Before arranging, it breaks into rows again if the width changed. */
    protected void layout(int width, int height) {
        final int faxis = getFlowAxis();
        int newSpan;
        if (faxis == X_AXIS) {
            newSpan = width;
        } else {
            newSpan = height;
        }
        if (layoutSpan != newSpan) {
            layoutChanged(faxis);
            layoutChanged(getAxis());
            layoutSpan = newSpan;
        }

        if (!isLayoutValid(faxis)) {
            int heightAxis = getAxis();
            int oldFlowHeight = (heightAxis == X_AXIS) ? getWidth() : getHeight();
            strategy.layout(this);
            int newFlowHeight = (int) getPreferredSpan(heightAxis);
            if (oldFlowHeight != newFlowHeight) {
                View p = getParent();
                if (p != null) {
                    p.preferenceChanged(this, (heightAxis == X_AXIS), (heightAxis == Y_AXIS));
                }
            }
        }
        super.layout(width, height);
    }

    /** The minimum on the minor axis is that of the most demanding row. */
    protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
        if (r == null) {
            r = new SizeRequirements();
        }
        float pref = layoutPool.getPreferredSpan(axis);
        float min = layoutPool.getMinimumSpan(axis);
        r.minimum = (int) min;
        r.preferred = Math.max(r.minimum, (int) pref);
        r.maximum = Integer.MAX_VALUE;
        r.alignment = 0.5f;
        return r;
    }

    public void insertUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        layoutPool.insertUpdate(changes, a, f);
        strategy.insertUpdate(this, changes, getInsideAllocation(a));
    }

    public void removeUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        layoutPool.removeUpdate(changes, a, f);
        strategy.removeUpdate(this, changes, getInsideAllocation(a));
    }

    public void changedUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        layoutPool.changedUpdate(changes, a, f);
        strategy.changedUpdate(this, changes, getInsideAllocation(a));
    }

    public void setParent(View parent) {
        super.setParent(parent);
        if (parent == null && layoutPool != null) {
            layoutPool.setParent(null);
        }
    }

    /**
     * How the content is shared out into rows.
     *
     * <p>It is outside the view so as to be changeable; see {@link FlowView}'s note. The one here
     * breaks by words using the views' break weights.
     */
    public static class FlowStrategy {

        Position damageStart = null;
        Vector<View> viewBuffer;

        public FlowStrategy() {
        }

        void addDamage(FlowView fv, int offset) {
            if (offset >= fv.getStartOffset() && offset < fv.getEndOffset()) {
                if (damageStart == null || offset < damageStart.getOffset()) {
                    try {
                        damageStart = fv.getDocument().createPosition(offset);
                    } catch (BadLocationException e) {
                        damageStart = null;
                    }
                }
            }
        }

        void unsetDamage() {
            damageStart = null;
        }

        public void insertUpdate(FlowView fv, DocumentEvent e, Rectangle alloc) {
            if (alloc != null) {
                fv.layoutChanged(fv.getFlowAxis());
                java.awt.Container host = fv.getContainer();
                if (host != null) {
                    host.repaint(alloc.x, alloc.y, alloc.width, alloc.height);
                }
            } else {
                fv.layoutChanged(fv.getAxis());
                fv.layoutChanged(fv.getFlowAxis());
            }
        }

        public void removeUpdate(FlowView fv, DocumentEvent e, Rectangle alloc) {
            insertUpdate(fv, e, alloc);
        }

        public void changedUpdate(FlowView fv, DocumentEvent e, Rectangle alloc) {
            insertUpdate(fv, e, alloc);
        }

        /** That view's logical tree. */
        protected View getLogicalView(FlowView fv) {
            return fv.layoutPool;
        }

        /**
         * It rebuilds the rows.
         *
         * <p>It goes on creating rows and filling them until the content runs out. Each row is
         * filled with {@link #layoutRow}, which is the one that decides where to break.
         */
        public void layout(FlowView fv) {
            View pool = getLogicalView(fv);
            int p0 = fv.getStartOffset();
            int p1 = fv.getEndOffset();

            fv.removeAll();
            int rowIndex = 0;
            int p = p0;
            while (p < p1) {
                View row = fv.createRow();
                fv.append(row);
                int next = layoutRow(fv, rowIndex, p);
                if (next <= p) {
                    // Nothing fitted: the advance is forced so as not to hang.
                    next = p + 1;
                }
                p = next;
                rowIndex = rowIndex + 1;
            }
        }

        /**
         * It fills a row from that position and returns where it ended up.
         *
         * <p>It goes on adding views while they fit; the first one that does not fit is split with
         * {@code breakView}, which is where breaking by a word and not by a letter is decided.
         */
        protected int layoutRow(FlowView fv, int rowIndex, int pos) {
            View row = fv.getView(rowIndex);
            float x = fv.getFlowStart(rowIndex);
            float spanLeft = fv.getFlowSpan(rowIndex);
            int end = fv.getEndOffset();
            int flowAxis = fv.getFlowAxis();

            int p = pos;
            while (p < end && spanLeft >= 0) {
                View v = createView(fv, p, (int) spanLeft, rowIndex);
                if (v == null) {
                    break;
                }
                float chunk = v.getPreferredSpan(flowAxis);
                if (chunk > spanLeft && row.getViewCount() > 0) {
                    // It does not fit and there is already something in the row: it breaks here.
                    break;
                }
                row.append(v);
                spanLeft = spanLeft - chunk;
                p = v.getEndOffset();
                if (v.getEndOffset() <= pos) {
                    break;
                }
            }
            return p;
        }

        /** It arranges the row once it is full; without justification, it does nothing. */
        protected void adjustRow(FlowView fv, int rowIndex, int desiredSpan, int x) {
        }

        void reparentViews(View pool, int startPos) {
        }

        /**
         * The view to put starting at that position, trimmed to what fits.
         *
         * <p>It returns the logical tree's one whole if it fits, and a fragment if not.
         */
        protected View createView(FlowView fv, int startOffset, int spanLeft, int rowIndex) {
            View pool = getLogicalView(fv);
            int childIndex = pool.getViewIndex(startOffset, Position.Bias.Forward);
            if (childIndex < 0) {
                return null;
            }
            View v = pool.getView(childIndex);
            if (v == null) {
                return null;
            }
            if (startOffset > v.getStartOffset()) {
                v = v.createFragment(startOffset, v.getEndOffset());
            }
            int flowAxis = fv.getFlowAxis();
            float span = v.getPreferredSpan(flowAxis);
            if (span > spanLeft) {
                View split = v.breakView(flowAxis, v.getStartOffset(), 0f, spanLeft);
                if (split != null && split.getEndOffset() > v.getStartOffset()) {
                    return split;
                }
            }
            return v;
        }
    }

    /**
     * The logical tree: one view per child element, which is not rebuilt when the width changes.
     *
     * <p>It is private in the JDK and here too: it is reached through
     * {@link FlowStrategy#getLogicalView}.
     */
    static class LogicalView extends CompositeView {

        LogicalView(Element elem) {
            super(elem);
        }

        protected int getViewIndexAtPosition(int pos) {
            Element elem = getElement();
            if (elem.isLeaf()) {
                return 0;
            }
            return super.getViewIndexAtPosition(pos);
        }

        protected void loadChildren(ViewFactory f) {
            Element elem = getElement();
            if (elem.isLeaf()) {
                View v = new LabelView(elem);
                append(v);
            } else {
                super.loadChildren(f);
            }
        }

        public float getPreferredSpan(int axis) {
            float maxpref = 0;
            float pref = 0;
            int n = getViewCount();
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                pref = pref + v.getPreferredSpan(axis);
                if (v.getBreakWeight(axis, 0, Integer.MAX_VALUE) >= View.ForcedBreakWeight) {
                    maxpref = Math.max(maxpref, pref);
                    pref = 0;
                }
            }
            maxpref = Math.max(maxpref, pref);
            return maxpref;
        }

        public float getMinimumSpan(int axis) {
            float maxmin = 0;
            float min = 0;
            boolean nowrap = false;
            int n = getViewCount();
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                if (v.getBreakWeight(axis, 0, Integer.MAX_VALUE) == View.BadBreakWeight) {
                    min = min + v.getPreferredSpan(axis);
                    nowrap = true;
                } else if (nowrap) {
                    maxmin = Math.max(min, maxmin);
                    nowrap = false;
                    min = 0;
                }
            }
            maxmin = Math.max(maxmin, min);
            return maxmin;
        }

        protected void forwardUpdateToView(View v, DocumentEvent e, Shape a, ViewFactory f) {
            View parent = v.getParent();
            v.setParent(this);
            super.forwardUpdateToView(v, e, a, f);
            v.setParent(parent);
        }

        // -- what a logical view does not need, because it is not drawn ----------------------

        protected boolean isBefore(int x, int y, Rectangle alloc) {
            return false;
        }

        protected boolean isAfter(int x, int y, Rectangle alloc) {
            return false;
        }

        protected View getViewAtPoint(int x, int y, Rectangle alloc) {
            return null;
        }

        protected void childAllocation(int index, Rectangle a) {
        }

        public void paint(java.awt.Graphics g, Shape allocation) {
        }

        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            return null;
        }

        public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
            return -1;
        }
    }
}
