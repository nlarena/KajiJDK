package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.io.Serializable;

/**
 * It lays every child out <em>one on top of another</em>, aligned by their alignment points.
 *
 * <h2>It is not "everybody at the same position"</h2>
 *
 * <p>The difference is in the alignment. Each component has an {@code alignmentX} and an
 * {@code alignmentY} between zero and one, which say which point of itself it wants to align.
 * With {@code 0.5} it aligns by the centre, with {@code 0} by the left or top edge, with
 * {@code 1} by the right or bottom one. This layout looks for a common point and makes each
 * child's alignment point fall there.
 *
 * <p>That is why two children of different sizes with the same alignment end up centred one
 * over the other, and with different alignments they end up shifted. With everybody at
 * {@code 0} and of the same size the trivial case is left, which is what most people expect and
 * is not what the class does in general.
 *
 * <h2>The drawing order is decided by the container</h2>
 *
 * <p>This layout only sets positions and sizes. Which one is seen on top is decided by the
 * children's order, which belongs to the container: the first is drawn last and therefore ends
 * up on top.
 *
 * <h2>The measurements are kept and have to be invalidated</h2>
 *
 * <p>Measuring every child is expensive, so the result is kept. {@link #invalidateLayout}
 * throws it away; the container calls it by itself when something changes.
 */
public class OverlayLayout implements LayoutManager2, Serializable {

    private final Container target;

    private transient SizeRequirements[] xChildren;
    private transient SizeRequirements[] yChildren;
    private transient SizeRequirements xTotal;
    private transient SizeRequirements yTotal;

    /**
     * For that container.
     *
     * <p>It is passed the container in the constructor and afterwards it cannot be changed: it is
     * the same as {@link BoxLayout} does, and that is why one of these layouts is not shared
     * between two containers.
     */
    public OverlayLayout(Container target) {
        this.target = target;
    }

    /** The container it is tied to. */
    public final Container getTarget() {
        return this.target;
    }

    /** It throws away the kept measurements; see the class note. */
    public void invalidateLayout(Container target) {
        checkContainer(target);
        xChildren = null;
        yChildren = null;
        xTotal = null;
        yTotal = null;
    }

    /** It does nothing: this layout does not use names. */
    public void addLayoutComponent(String name, Component comp) {
        invalidateLayout(comp.getParent());
    }

    public void removeLayoutComponent(Component comp) {
        invalidateLayout(comp.getParent());
    }

    /** It does nothing: this layout does not use constraints. */
    public void addLayoutComponent(Component comp, Object constraints) {
        invalidateLayout(comp.getParent());
    }

    /**
     * What the container would like to measure.
     *
     * @throws AWTError if it is not the container it is tied to
     */
    public Dimension preferredLayoutSize(Container target) {
        checkContainer(target);
        checkRequests();
        Dimension size = new Dimension(xTotal.preferred, yTotal.preferred);
        Insets insets = target.getInsets();
        size.width = size.width + insets.left + insets.right;
        size.height = size.height + insets.top + insets.bottom;
        return size;
    }

    /**
     * The least it makes do with.
     *
     * @throws AWTError if it is not the container it is tied to
     */
    public Dimension minimumLayoutSize(Container target) {
        checkContainer(target);
        checkRequests();
        Dimension size = new Dimension(xTotal.minimum, yTotal.minimum);
        Insets insets = target.getInsets();
        size.width = size.width + insets.left + insets.right;
        size.height = size.height + insets.top + insets.bottom;
        return size;
    }

    /**
     * The most it may take up.
     *
     * @throws AWTError if it is not the container it is tied to
     */
    public Dimension maximumLayoutSize(Container target) {
        checkContainer(target);
        checkRequests();
        Dimension size = new Dimension(xTotal.maximum, yTotal.maximum);
        Insets insets = target.getInsets();
        size.width = size.width + insets.left + insets.right;
        size.height = size.height + insets.top + insets.bottom;
        return size;
    }

    /**
     * The set's horizontal alignment.
     *
     * @throws AWTError if it is not the container it is tied to
     */
    public float getLayoutAlignmentX(Container target) {
        checkContainer(target);
        checkRequests();
        return xTotal.alignment;
    }

    /**
     * The set's vertical alignment.
     *
     * @throws AWTError if it is not the container it is tied to
     */
    public float getLayoutAlignmentY(Container target) {
        checkContainer(target);
        checkRequests();
        return yTotal.alignment;
    }

    /**
     * It puts each child with its alignment point over the common point.
     *
     * @throws AWTError if it is not the container it is tied to
     */
    public void layoutContainer(Container target) {
        checkContainer(target);
        checkRequests();
        int nChildren = target.getComponentCount();
        int[] xOffsets = new int[nChildren];
        int[] xSpans = new int[nChildren];
        int[] yOffsets = new int[nChildren];
        int[] ySpans = new int[nChildren];
        Dimension alloc = target.getSize();
        Insets in = target.getInsets();
        alloc.width = alloc.width - (in.left + in.right);
        alloc.height = alloc.height - (in.top + in.bottom);
        SizeRequirements.calculateAlignedPositions(alloc.width, xTotal, xChildren, xOffsets,
                xSpans);
        SizeRequirements.calculateAlignedPositions(alloc.height, yTotal, yChildren, yOffsets,
                ySpans);
        for (int i = 0; i < nChildren; i++) {
            Component c = target.getComponent(i);
            c.setBounds(in.left + xOffsets[i], in.top + yOffsets[i], xSpans[i], ySpans[i]);
        }
    }

    /**
     * @throws AWTError if it is not the container it is tied to
     */
    void checkContainer(Container target) {
        if (this.target != target) {
            throw new java.awt.AWTError("OverlayLayout can't be shared");
        }
    }

    /** It measures the children again if needed; see the class note. */
    void checkRequests() {
        if (xChildren == null || yChildren == null) {
            int n = target.getComponentCount();
            xChildren = new SizeRequirements[n];
            yChildren = new SizeRequirements[n];
            for (int i = 0; i < n; i++) {
                Component c = target.getComponent(i);
                Dimension min = c.getMinimumSize();
                Dimension typ = c.getPreferredSize();
                Dimension max = c.getMaximumSize();
                xChildren[i] = new SizeRequirements(min.width, typ.width, max.width,
                        c.getAlignmentX());
                yChildren[i] = new SizeRequirements(min.height, typ.height, max.height,
                        c.getAlignmentY());
            }
            xTotal = SizeRequirements.getAlignedSizeRequirements(xChildren);
            yTotal = SizeRequirements.getAlignedSizeRequirements(yChildren);
        }
    }
}
