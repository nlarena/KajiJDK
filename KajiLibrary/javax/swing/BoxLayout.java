package javax.swing;

import java.awt.AWTError;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.io.PrintStream;
import java.io.Serializable;

/**
 * It lays the children out in a single row or in a single column.
 *
 * <h2>One axis is shared out, the other is aligned</h2>
 *
 * <p>The whole class is that. On the chosen axis the children go one after another and share
 * out the length with {@link SizeRequirements#calculateTiledPositions}; on the perpendicular
 * axis they overlap and are aligned with {@link SizeRequirements#calculateAlignedPositions},
 * each one by its {@code alignmentX} or {@code alignmentY}. Hence the two classic surprises: a
 * child may end up wider than it asked for, because its maximum allowed it, and a column of
 * buttons ends up centred because the default alignment is 0.5.
 *
 * <h2>Absolute axes and the language's axes</h2>
 *
 * <p>{@link #X_AXIS} and {@link #Y_AXIS} are fixed directions. {@link #LINE_AXIS} and
 * {@link #PAGE_AXIS} are "the direction a line advances in" and "the direction lines advance
 * in", which depend on the container's orientation: in a language that is read right to left,
 * a line-axis box fills from the right. That is the only place where this class looks at the
 * orientation.
 *
 * <h2>One layout per container</h2>
 *
 * <p>The container is passed on construction and cannot be changed: the class keeps the
 * children's requests between calls, and sharing it would be mixing up two containers'. Asking
 * it to lay another one out is an {@link AWTError}, not an exception: it is a mistake of the
 * program, not a condition a program may handle.
 */
public class BoxLayout implements LayoutManager2, Serializable {

    /** Left to right. */
    public static final int X_AXIS = 0;

    /** Top to bottom. */
    public static final int Y_AXIS = 1;

    /** The direction a line of text advances in. */
    public static final int LINE_AXIS = 2;

    /** The direction lines are stacked in. */
    public static final int PAGE_AXIS = 3;

    private int axis;
    private Container target;

    private transient SizeRequirements[] xChildren;
    private transient SizeRequirements[] yChildren;
    private transient SizeRequirements xTotal;
    private transient SizeRequirements yTotal;

    private transient PrintStream dbg;

    /** It lays that container's children out on that axis. */
    public BoxLayout(Container target, int axis) {
        if (axis != X_AXIS && axis != Y_AXIS && axis != LINE_AXIS && axis != PAGE_AXIS) {
            throw new AWTError("Invalid axis");
        }
        this.axis = axis;
        this.target = target;
    }

    /**
     * Like the other constructor, also writing what it decides to that stream.
     *
     * @deprecated it is for debugging; the JDK left it for compatibility.
     */
    @Deprecated
    BoxLayout(Container target, int axis, PrintStream dbg) {
        this(target, axis);
        this.dbg = dbg;
    }

    /** The container it lays out. */
    public final Container getTarget() {
        return this.target;
    }

    /** The axis just as it was asked for, without resolving it against the orientation. */
    public final int getAxis() {
        return this.axis;
    }

    /** It forgets the kept requests: something changed and they have to be asked for again. */
    public synchronized void invalidateLayout(Container target) {
        checkContainer(target);
        xChildren = null;
        yChildren = null;
        xTotal = null;
        yTotal = null;
    }

    /** Nothing: this arrangement does not use names. */
    public void addLayoutComponent(String name, Component comp) {
        invalidateLayout(comp.getParent());
    }

    public void removeLayoutComponent(Component comp) {
        invalidateLayout(comp.getParent());
    }

    /** Nothing: this arrangement does not use constraints. */
    public void addLayoutComponent(Component comp, Object constraints) {
        invalidateLayout(comp.getParent());
    }

    public Dimension preferredLayoutSize(Container target) {
        Dimension size;
        synchronized (this) {
            checkContainer(target);
            checkRequests();
            size = new Dimension(xTotal.preferred, yTotal.preferred);
        }
        Insets insets = target.getInsets();
        size.width = (int) Math.min((long) size.width + (long) insets.left + (long) insets.right,
                Integer.MAX_VALUE);
        size.height = (int) Math.min((long) size.height + (long) insets.top + (long) insets.bottom,
                Integer.MAX_VALUE);
        return size;
    }

    public Dimension minimumLayoutSize(Container target) {
        Dimension size;
        synchronized (this) {
            checkContainer(target);
            checkRequests();
            size = new Dimension(xTotal.minimum, yTotal.minimum);
        }
        Insets insets = target.getInsets();
        size.width = (int) Math.min((long) size.width + (long) insets.left + (long) insets.right,
                Integer.MAX_VALUE);
        size.height = (int) Math.min((long) size.height + (long) insets.top + (long) insets.bottom,
                Integer.MAX_VALUE);
        return size;
    }

    public Dimension maximumLayoutSize(Container target) {
        Dimension size;
        synchronized (this) {
            checkContainer(target);
            checkRequests();
            size = new Dimension(xTotal.maximum, yTotal.maximum);
        }
        Insets insets = target.getInsets();
        size.width = (int) Math.min((long) size.width + (long) insets.left + (long) insets.right,
                Integer.MAX_VALUE);
        size.height = (int) Math.min((long) size.height + (long) insets.top + (long) insets.bottom,
                Integer.MAX_VALUE);
        return size;
    }

    public synchronized float getLayoutAlignmentX(Container target) {
        checkContainer(target);
        checkRequests();
        return xTotal.alignment;
    }

    public synchronized float getLayoutAlignmentY(Container target) {
        checkContainer(target);
        checkRequests();
        return yTotal.alignment;
    }

    /** It places the children; see the class note. */
    public void layoutContainer(Container target) {
        checkContainer(target);
        int nChildren = target.getComponentCount();
        int[] xOffsets = new int[nChildren];
        int[] xSpans = new int[nChildren];
        int[] yOffsets = new int[nChildren];
        int[] ySpans = new int[nChildren];

        Dimension alloc = target.getSize();
        Insets in = target.getInsets();
        alloc.width = alloc.width - (in.left + in.right);
        alloc.height = alloc.height - (in.top + in.bottom);

        ComponentOrientation o = target.getComponentOrientation();
        int absoluteAxis = resolveAxis(axis, o);
        boolean ltr = (absoluteAxis != axis) ? o.isLeftToRight() : true;

        synchronized (this) {
            checkRequests();

            if (absoluteAxis == X_AXIS) {
                SizeRequirements.calculateTiledPositions(alloc.width, xTotal, xChildren, xOffsets,
                        xSpans, ltr);
                SizeRequirements.calculateAlignedPositions(alloc.height, yTotal, yChildren,
                        yOffsets, ySpans);
            } else {
                SizeRequirements.calculateAlignedPositions(alloc.width, xTotal, xChildren,
                        xOffsets, xSpans, ltr);
                SizeRequirements.calculateTiledPositions(alloc.height, yTotal, yChildren, yOffsets,
                        ySpans);
            }
        }

        for (int i = 0; i < nChildren; i++) {
            Component c = target.getComponent(i);
            c.setBounds((int) Math.min((long) in.left + (long) xOffsets[i], Integer.MAX_VALUE),
                    (int) Math.min((long) in.top + (long) yOffsets[i], Integer.MAX_VALUE),
                    xSpans[i], ySpans[i]);
        }
        if (dbg != null) {
            for (int i = 0; i < nChildren; i++) {
                Component c = target.getComponent(i);
                dbg.println(c.toString());
            }
        }
    }

    /** An {@link AWTError} if it is not its container; see the class note. */
    void checkContainer(Container target) {
        if (this.target != target) {
            throw new AWTError("BoxLayout can't be shared");
        }
    }

    /**
     * It asks each child again how much it wants to measure, if needed.
     *
     * <p>An invisible child asks for zero in everything but keeps its alignment: it takes up a
     * place in the arrays, so that the indices go on being the container's, but it takes up no
     * space.
     */
    void checkRequests() {
        if (xChildren == null || yChildren == null) {
            int n = target.getComponentCount();
            xChildren = new SizeRequirements[n];
            yChildren = new SizeRequirements[n];
            for (int i = 0; i < n; i++) {
                Component c = target.getComponent(i);
                if (!c.isVisible()) {
                    xChildren[i] = new SizeRequirements(0, 0, 0, c.getAlignmentX());
                    yChildren[i] = new SizeRequirements(0, 0, 0, c.getAlignmentY());
                    continue;
                }
                Dimension min = c.getMinimumSize();
                Dimension typ = c.getPreferredSize();
                Dimension max = c.getMaximumSize();
                xChildren[i] = new SizeRequirements(min.width, typ.width, max.width,
                        c.getAlignmentX());
                yChildren[i] = new SizeRequirements(min.height, typ.height, max.height,
                        c.getAlignmentY());
            }

            int absoluteAxis = resolveAxis(axis, target.getComponentOrientation());
            if (absoluteAxis == X_AXIS) {
                xTotal = SizeRequirements.getTiledSizeRequirements(xChildren);
                yTotal = SizeRequirements.getAlignedSizeRequirements(yChildren);
            } else {
                xTotal = SizeRequirements.getAlignedSizeRequirements(xChildren);
                yTotal = SizeRequirements.getTiledSizeRequirements(yChildren);
            }
        }
    }

    /** The language's axis taken to a fixed axis; see the class note. */
    private int resolveAxis(int axis, ComponentOrientation o) {
        int absoluteAxis;
        if (axis == LINE_AXIS) {
            absoluteAxis = o.isHorizontal() ? X_AXIS : Y_AXIS;
        } else if (axis == PAGE_AXIS) {
            absoluteAxis = o.isHorizontal() ? Y_AXIS : X_AXIS;
        } else {
            absoluteAxis = axis;
        }
        return absoluteAxis;
    }
}
