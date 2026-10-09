package javax.swing.text;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent$ElementChange;

/**
 * A box that lays its children out on another thread.
 *
 * <h2>So that the screen does not freeze</h2>
 *
 * <p>Laying out a large document takes time. If that happens on the event thread, the window
 * does not respond while it lasts. This view puts each child in a queue ({@link LayoutQueue})
 * and lays it out on a separate thread; meanwhile, it answers with estimates.
 *
 * <h2>The price: nothing is exact until it finishes</h2>
 *
 * <p>As the children are measured one at a time, the total length is an estimate that is
 * corrected as the results arrive. That is why {@link ChildState} keeps per child whether its
 * layout already holds, and the {@link ChildLocator} remembers up to where the offsets are
 * trustworthy: beyond that point it computes them assuming that those still missing measure what
 * the estimate says.
 *
 * <h2>Two threads over the same data</h2>
 *
 * <p>The layout thread writes the sizes and the event one reads them. Everything shared is
 * touched under the view's lock or the child's, and never both at once in a different order:
 * that is what avoids a deadlock between painting and laying out.
 */
public class AsyncBoxView extends View {

    /** Who knows where each child falls. */
    protected ChildLocator locator;

    int axis;
    List<ChildState> stats;
    float majorSpan;
    float minorSpan;
    boolean estimatedMajorSpan;
    int majorAxis;
    int minorAxis;
    float topInset;
    float bottomInset;
    float leftInset;
    float rightInset;
    ChildState minRequest;
    ChildState prefRequest;
    boolean majorChanged;
    boolean minorChanged;
    Runnable flushTask;

    /** An asynchronous box on that axis. */
    public AsyncBoxView(Element elem, int axis) {
        super(elem);
        stats = new ArrayList<ChildState>();
        this.axis = axis;
        locator = new ChildLocator(this);
        flushTask = new FlushTask(this);
        minorSpan = Short.MAX_VALUE;
    }

    /** The axis the children are stacked on. */
    public int getMajorAxis() {
        return axis;
    }

    /** The other one. */
    public int getMinorAxis() {
        return (axis == X_AXIS) ? Y_AXIS : X_AXIS;
    }

    public float getTopInset() {
        return topInset;
    }

    public void setTopInset(float i) {
        topInset = i;
    }

    public float getBottomInset() {
        return bottomInset;
    }

    public void setBottomInset(float i) {
        bottomInset = i;
    }

    public float getLeftInset() {
        return leftInset;
    }

    public void setLeftInset(float i) {
        leftInset = i;
    }

    public float getRightInset() {
        return rightInset;
    }

    public void setRightInset(float i) {
        rightInset = i;
    }

    /** How much the margin eats on that axis. */
    protected float getInsetSpan(int axis) {
        float margin = (axis == X_AXIS)
                ? getLeftInset() + getRightInset() : getTopInset() + getBottomInset();
        return margin;
    }

    /** Whether the total length is still an estimate; see the class note. */
    protected void setEstimatedMajorSpan(boolean isEstimated) {
        estimatedMajorSpan = isEstimated;
    }

    protected boolean getEstimatedMajorSpan() {
        return estimatedMajorSpan;
    }

    /** The state of child number such and such. */
    protected ChildState getChildState(int index) {
        synchronized (stats) {
            if ((index >= 0) && (index < stats.size())) {
                return stats.get(index);
            }
            return null;
        }
    }

    /** The queue where the layouts are queued. */
    protected LayoutQueue getLayoutQueue() {
        return LayoutQueue.getDefaultQueue();
    }

    protected ChildState createChildState(View v) {
        return new ChildState(this, v);
    }

    /**
     * A child changed length: the total is corrected without laying everything out again.
     *
     * <p>What it measured is subtracted and what it measures now is added. Recomputing the whole
     * sum every time would be quadratic in the number of children.
     */
    protected synchronized void majorRequirementChange(ChildState cs, float delta) {
        if (!estimatedMajorSpan) {
            majorSpan = majorSpan + delta;
        }
        majorChanged = true;
    }

    /** A child changed width: the box's width may change. */
    protected synchronized void minorRequirementChange(ChildState cs) {
        minorChanged = true;
    }

    /** It reports to the parent the changes gathered so far. */
    protected void flushRequirementChanges() {
        AbstractDocument doc = (AbstractDocument) getDocument();
        try {
            doc.readLock();

            View parent = null;
            boolean horizontal = false;
            boolean vertical = false;

            synchronized (this) {
                if (majorChanged || minorChanged) {
                    parent = getParent();
                    if (parent != null) {
                        if (axis == X_AXIS) {
                            horizontal = majorChanged;
                            vertical = minorChanged;
                        } else {
                            vertical = majorChanged;
                            horizontal = minorChanged;
                        }
                    }
                    majorChanged = false;
                    minorChanged = false;
                }
            }

            if (parent != null) {
                parent.preferenceChanged(this, horizontal, vertical);
                java.awt.Component c = getContainer();
                if (c != null) {
                    c.repaint();
                }
            }
        } finally {
            doc.readUnlock();
        }
    }

    /** It swaps the children and queues the layout of the new ones. */
    public void replace(int offset, int length, View[] views) {
        synchronized (stats) {
            for (int i = 0; i < length; i++) {
                ChildState cs = stats.remove(offset);
                float csSpan = cs.getMajorSpan();
                cs.getChildView().setParent(null);
                if (csSpan != 0) {
                    majorRequirementChange(cs, -csSpan);
                }
            }
            if (views != null) {
                for (int i = 0; i < views.length; i++) {
                    ChildState s = createChildState(views[i]);
                    stats.add(offset + i, s);
                    majorRequirementChange(s, s.getMajorSpan());
                }
            }
        }
        if (views != null) {
            LayoutQueue q = getLayoutQueue();
            for (int i = 0; i < views.length; i++) {
                q.addTask(getChildState(offset + i));
            }
            q.addTask(flushTask);
        }
    }

    protected void loadChildren(ViewFactory f) {
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

    protected synchronized int getViewIndexAtPosition(int pos, Position.Bias b) {
        boolean isBackward = (b == Position.Bias.Backward);
        pos = (isBackward) ? Math.max(0, pos - 1) : pos;
        Element elem = getElement();
        return elem.getElementIndex(pos);
    }

    protected void updateLayout(DocumentEvent$ElementChange ec, DocumentEvent e, Shape a) {
        if (ec != null) {
            // The children changed: the kept layout no longer serves.
            locator.childChanged(null);
        }
    }

    public void setParent(View parent) {
        super.setParent(parent);
        if ((parent != null) && (getViewCount() == 0)) {
            ViewFactory f = getViewFactory();
            loadChildren(f);
        }
    }

    public synchronized void preferenceChanged(View child, boolean width, boolean height) {
        if (child == null) {
            getParent().preferenceChanged(this, width, height);
        } else {
            if (minRequest == child) {
                minRequest = null;
            }
            if (prefRequest == child) {
                prefRequest = null;
            }
            int index = getViewIndex(child.getStartOffset(), Position.Bias.Forward);
            ChildState cs = getChildState(index);
            if (cs != null) {
                cs.preferenceChanged(width, height);
                LayoutQueue q = getLayoutQueue();
                q.addTask(cs);
                q.addTask(flushTask);
            }
        }
    }

    /** On a size change only the minor axis changes: the major one is decided by the children. */
    public void setSize(float width, float height) {
        setSpanOnAxis(X_AXIS, width);
        setSpanOnAxis(Y_AXIS, height);
    }

    float getSpanOnAxis(int axis) {
        if (axis == getMajorAxis()) {
            return majorSpan;
        }
        return minorSpan;
    }

    void setSpanOnAxis(int axis, float span) {
        float margin = getInsetSpan(axis);
        if (axis == getMinorAxis()) {
            float targetSpan = span - margin;
            if (targetSpan != minorSpan) {
                minorSpan = targetSpan;
                // Changing the width invalidates every child's height.
                int n = getViewCount();
                LayoutQueue q = getLayoutQueue();
                for (int i = 0; i < n; i++) {
                    ChildState cs = getChildState(i);
                    cs.childSizeValid = false;
                    q.addTask(cs);
                }
                q.addTask(flushTask);
            }
        } else {
            // The major axis is not imposed: whatever the children measure is accepted.
            if (estimatedMajorSpan) {
                majorSpan = span - margin;
            }
        }
    }

    public void paint(Graphics g, Shape alloc) {
        synchronized (locator) {
            locator.setAllocation(alloc);
            locator.paintChildren(g);
        }
    }

    public float getPreferredSpan(int axis) {
        float margin = getInsetSpan(axis);
        if (axis == this.axis) {
            return majorSpan + margin;
        }
        if (prefRequest != null) {
            View child = prefRequest.getChildView();
            return child.getPreferredSpan(axis) + margin;
        }
        return getMinorSpan() + margin;
    }

    public float getMinimumSpan(int axis) {
        if (axis == this.axis) {
            return getPreferredSpan(axis);
        }
        if (minRequest != null) {
            View child = minRequest.getChildView();
            return child.getMinimumSpan(axis) + getInsetSpan(axis);
        }
        return getInsetSpan(axis);
    }

    public float getMaximumSpan(int axis) {
        if (axis == this.axis) {
            return getPreferredSpan(axis);
        }
        return Integer.MAX_VALUE;
    }

    /** The width is that of the widest child measured so far. */
    float getMinorSpan() {
        float span = 0;
        int n = getViewCount();
        for (int i = 0; i < n; i++) {
            ChildState cs = getChildState(i);
            span = Math.max(span, cs.getMinorSpan());
        }
        return span;
    }

    public int getViewCount() {
        synchronized (stats) {
            return stats.size();
        }
    }

    public View getView(int n) {
        ChildState cs = getChildState(n);
        if (cs != null) {
            return cs.getChildView();
        }
        return null;
    }

    public Shape getChildAllocation(int index, Shape a) {
        if (a == null) {
            return null;
        }
        synchronized (locator) {
            locator.setAllocation(a);
            return locator.getChildAllocation(index);
        }
    }

    public int getViewIndex(int pos, Position.Bias b) {
        return getViewIndexAtPosition(pos, b);
    }

    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        int index = getViewIndex(pos, b);
        Shape ca = locator.getChildAllocationSync(index, a);
        View cv = getView(index);
        Shape v = cv.modelToView(pos, ca, b);
        return v;
    }

    public int viewToModel(float x, float y, Shape a, Position.Bias[] biasReturn) {
        int index = locator.getViewIndexAtPoint(x, y, a);
        Shape ca = locator.getChildAllocationSync(index, a);
        View v = getView(index);
        return v.viewToModel(x, y, ca, biasReturn);
    }

    public int getNextVisualPositionFrom(int pos, Position.Bias b, Shape a, int direction,
            Position.Bias[] biasRet) throws BadLocationException {
        if (pos < -1) {
            throw new BadLocationException("invalid position", pos);
        }
        return Utilities.getNextVisualPositionFrom(this, pos, b, a, direction, biasRet);
    }

    /**
     * What the box knows about a child: its size, whether it holds and where it starts.
     *
     * <p>It is also the task that is queued: {@link #run} is what the layout thread runs.
     *
     * <p>In the JDK it is an inner class; here it is static and takes the box as its first
     * parameter, which is the same signature the JDK generates in the compiled file. See
     * {@link TableView.TableRow}'s note.
     */
    public static class ChildState implements Runnable {

        private final AsyncBoxView box;
        private View child;
        private float majorSpan;
        private float minorSpan;
        private float minorMin;
        private float minorPref;
        private float minorMax;
        private float majorOffset;
        boolean childSizeValid;
        private boolean minorValid;
        private boolean majorValid;

        /** That child's state, still unmeasured. */
        public ChildState(AsyncBoxView box, View v) {
            this.box = box;
            child = v;
            minorValid = false;
            majorValid = false;
            childSizeValid = false;
            child.setParent(box);
        }

        public View getChildView() {
            return child;
        }

        /**
         * It measures the child. The layout thread runs it.
         *
         * <p>It takes the document's read lock: without that, the document could change in the
         * middle of the measurement and the result would hold for no state of the document.
         */
        public void run() {
            AbstractDocument doc = (AbstractDocument) box.getDocument();
            try {
                doc.readLock();
                if (minorValid && majorValid && childSizeValid) {
                    return;
                }
                if (child.getParent() == box) {
                    // It may have been removed while it waited in the queue.
                    updateChild();
                    while (!(minorValid && majorValid && childSizeValid)
                            && child.getParent() == box) {
                        updateChild();
                    }
                }
            } finally {
                doc.readUnlock();
            }
        }

        void updateChild() {
            boolean minorUpdated = false;
            synchronized (this) {
                if (!minorValid) {
                    int minorAxis = box.getMinorAxis();
                    minorMin = child.getMinimumSpan(minorAxis);
                    minorPref = child.getPreferredSpan(minorAxis);
                    minorMax = child.getMaximumSpan(minorAxis);
                    minorValid = true;
                    minorUpdated = true;
                }
            }
            if (minorUpdated) {
                box.minorRequirementChange(this);
            }

            boolean majorUpdated = false;
            float delta = 0.0f;
            synchronized (this) {
                if (!majorValid) {
                    float oldSpan = majorSpan;
                    majorSpan = child.getPreferredSpan(box.axis);
                    delta = majorSpan - oldSpan;
                    majorValid = true;
                    majorUpdated = true;
                }
            }
            if (majorUpdated) {
                box.majorRequirementChange(this, delta);
                box.locator.childChanged(this);
            }

            synchronized (this) {
                if (!childSizeValid) {
                    float w;
                    float h;
                    if (box.axis == X_AXIS) {
                        w = majorSpan;
                        h = getMinorSpan();
                    } else {
                        w = getMinorSpan();
                        h = majorSpan;
                    }
                    childSizeValid = true;
                    child.setSize(w, h);
                }
            }
        }

        /** The child's width, within what the box gives it. */
        public float getMinorSpan() {
            if (minorMax < box.minorSpan) {
                return minorMax;
            }
            return Math.max(minorMin, box.minorSpan);
        }

        /** Where the child starts on the minor axis, according to its alignment. */
        public float getMinorOffset() {
            if (minorMax < box.minorSpan) {
                float align = child.getAlignment(box.getMinorAxis());
                return ((box.minorSpan - minorMax) * align);
            }
            return 0f;
        }

        public float getMajorSpan() {
            return majorSpan;
        }

        public float getMajorOffset() {
            return majorOffset;
        }

        /** The {@link ChildLocator} sets it while walking the children. */
        public void setMajorOffset(float offs) {
            majorOffset = offs;
        }

        /** It marks what has to be measured again and queues the work. */
        public void preferenceChanged(boolean width, boolean height) {
            if (box.axis == X_AXIS) {
                if (width) {
                    majorValid = false;
                }
                if (height) {
                    minorValid = false;
                }
            } else {
                if (width) {
                    minorValid = false;
                }
                if (height) {
                    majorValid = false;
                }
            }
            childSizeValid = false;
        }

        public boolean isLayoutValid() {
            return (minorValid && majorValid && childSizeValid);
        }
    }

    /**
     * It knows where each child falls inside the box.
     *
     * <p>It keeps up to which child the offsets have already been computed. When one changes size,
     * those below it stop holding and are recomputed only when somebody asks for them: recomputing
     * them all on every change would be quadratic.
     */
    public static class ChildLocator {

        private final AsyncBoxView box;

        /** The last child whose offset holds. */
        protected ChildState lastValidOffset;

        /** The place given to the box the last time it was painted. */
        protected Rectangle lastAlloc;

        /** A rectangle that is reused so as not to allocate one per child. */
        protected Rectangle childAlloc;

        /** A locator for that box. */
        public ChildLocator(AsyncBoxView box) {
            this.box = box;
            lastAlloc = new Rectangle();
            childAlloc = new Rectangle();
        }

        /** A child changed: from it on, the offsets stop holding. */
        public synchronized void childChanged(ChildState cs) {
            if (lastValidOffset == null) {
                return;
            }
            if (cs == null || cs.getChildView().getStartOffset()
                    < lastValidOffset.getChildView().getStartOffset()) {
                lastValidOffset = cs;
            }
        }

        /** It draws the children that fall inside the clip. */
        public synchronized void paintChildren(Graphics g) {
            Rectangle clip = g.getClipBounds();
            float targetOffset = (box.axis == X_AXIS)
                    ? clip.x - lastAlloc.x : clip.y - lastAlloc.y;
            int index = getViewIndexAtVisualOffset(targetOffset);
            int n = box.getViewCount();
            float offs = box.getChildState(index).getMajorOffset();
            for (int i = index; i < n; i++) {
                ChildState cs = box.getChildState(i);
                cs.setMajorOffset(offs);
                Shape ca = getChildAllocation(i);
                if (intersectsClip(ca, clip)) {
                    synchronized (cs) {
                        View v = cs.getChildView();
                        v.paint(g, ca);
                    }
                } else {
                    // It already went past the clip: what follows is not seen either.
                    break;
                }
                offs = offs + cs.getMajorSpan();
            }
        }

        private boolean intersectsClip(Shape ca, Rectangle clip) {
            if (ca == null) {
                return false;
            }
            Rectangle r = ca.getBounds();
            return r.intersects(clip);
        }

        /** The place of child number such and such, within that place. */
        public synchronized Shape getChildAllocation(int index, Shape a) {
            if (a == null) {
                return null;
            }
            setAllocation(a);
            ChildState cs = box.getChildState(index);
            if (cs.getChildView().getParent() != box) {
                return null;
            }
            updateChildOffsetsToIndex(index);
            Shape ca = getChildAllocation(index);
            return ca;
        }

        Shape getChildAllocationSync(int index, Shape a) {
            synchronized (this) {
                return getChildAllocation(index, a);
            }
        }

        /** Which child falls on that point. */
        public int getViewIndexAtPoint(float x, float y, Shape a) {
            setAllocation(a);
            float targetOffset = (box.axis == X_AXIS) ? x - lastAlloc.x : y - lastAlloc.y;
            int index = getViewIndexAtVisualOffset(targetOffset);
            return index;
        }

        /** The place of child number such and such, in the last place given to the box. */
        protected Shape getChildAllocation(int index) {
            ChildState cs = box.getChildState(index);
            if (!cs.isLayoutValid()) {
                cs.run();
            }
            if (box.axis == X_AXIS) {
                childAlloc.x = lastAlloc.x + (int) cs.getMajorOffset();
                childAlloc.y = lastAlloc.y + (int) cs.getMinorOffset();
                childAlloc.width = (int) cs.getMajorSpan();
                childAlloc.height = (int) cs.getMinorSpan();
            } else {
                childAlloc.x = lastAlloc.x + (int) cs.getMinorOffset();
                childAlloc.y = lastAlloc.y + (int) cs.getMajorOffset();
                childAlloc.width = (int) cs.getMinorSpan();
                childAlloc.height = (int) cs.getMajorSpan();
            }
            return childAlloc;
        }

        /** It remembers the place given to the box, already without margins. */
        protected void setAllocation(Shape a) {
            if (a instanceof Rectangle) {
                lastAlloc.setBounds((Rectangle) a);
            } else {
                lastAlloc.setBounds(a.getBounds());
            }
            box.setSize(lastAlloc.width, lastAlloc.height);
            lastAlloc.x = lastAlloc.x + (int) box.getLeftInset();
            lastAlloc.y = lastAlloc.y + (int) box.getTopInset();
            lastAlloc.width = lastAlloc.width
                    - (int) (box.getLeftInset() + box.getRightInset());
            lastAlloc.height = lastAlloc.height
                    - (int) (box.getTopInset() + box.getBottomInset());
        }

        /** Which child starts at that distance from the beginning. */
        protected int getViewIndexAtVisualOffset(float targetOffset) {
            int n = box.getViewCount();
            if (n > 0) {
                boolean lastValid = (lastValidOffset != null);
                if (lastValidOffset == null) {
                    lastValidOffset = box.getChildState(0);
                }
                if (targetOffset > box.majorSpan) {
                    targetOffset = box.majorSpan;
                }
                if (targetOffset > lastValidOffset.getMajorOffset()) {
                    return updateChildOffsets(targetOffset);
                }
                float offs = 0f;
                for (int i = 0; i < n; i++) {
                    ChildState cs = box.getChildState(i);
                    float nextOffs = offs + cs.getMajorSpan();
                    if (targetOffset < nextOffs) {
                        return i;
                    }
                    offs = nextOffs;
                }
            }
            return n - 1;
        }

        /** It goes on computing offsets until it reaches that distance. */
        int updateChildOffsets(float targetOffset) {
            int n = box.getViewCount();
            int targetIndex = n - 1;
            int pos = box.stats.indexOf(lastValidOffset);
            float start = lastValidOffset.getMajorOffset();
            float lastOffset = start;
            for (int i = pos; i < n; i++) {
                ChildState cs = box.getChildState(i);
                cs.setMajorOffset(lastOffset);
                lastOffset = lastOffset + cs.getMajorSpan();
                lastValidOffset = cs;
                if (targetOffset < lastOffset) {
                    targetIndex = i;
                    break;
                }
            }
            return targetIndex;
        }

        /** It goes on computing offsets up to that child. */
        void updateChildOffsetsToIndex(int index) {
            int pos = (lastValidOffset != null) ? box.stats.indexOf(lastValidOffset) : 0;
            if (index <= pos) {
                return;
            }
            float lastOffset = (lastValidOffset != null) ? lastValidOffset.getMajorOffset() : 0f;
            for (int i = pos; i <= index; i++) {
                ChildState cs = box.getChildState(i);
                cs.setMajorOffset(lastOffset);
                lastOffset = lastOffset + cs.getMajorSpan();
                lastValidOffset = cs;
            }
        }
    }

    /** The task that reports the gathered changes; it is queued at the end of each batch. */
    static class FlushTask implements Runnable {

        private final AsyncBoxView box;

        FlushTask(AsyncBoxView box) {
            this.box = box;
        }

        public void run() {
            box.flushRequirementChanges();
        }
    }
}
