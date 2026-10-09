package java.awt;

import java.io.Serializable;
import java.util.Hashtable;

/**
 * A grid of cells **of unequal size**, where each child says how many cells it occupies and how it
 * behaves when there is space to spare.
 *
 * <p>It is AWT's most powerful layout and the hardest to use, and both come from the same place:
 * each child brings a {@link GridBagConstraints} with eleven decisions. It is worth splitting them
 * into three groups.
 *
 * <ul>
 *   <li><strong>where it goes</strong>: {@code gridx}, {@code gridy}, and how many cells it
 *       occupies with {@code gridwidth} and {@code gridheight};
 *   <li><strong>what happens when there is space to spare</strong>: {@code weightx} and
 *       {@code weighty} say what fraction of the spare space goes to its row or column;
 *   <li><strong>what it does with the space it got</strong>: {@code fill} if it stretches,
 *       {@code anchor} if not.
 * </ul>
 *
 * <p>The most confusing point is that **the weight belongs to the row or the column, not to the
 * component**. A child with weight 1 does not grow: it makes its column grow, and only then does
 * `fill` decide whether it fills it or stays centred in it. A component with weight and no `fill`
 * stays at its size in the middle of a huge column, which is the baffling result everyone runs into
 * the first time.
 *
 * <p>The methods come in pairs that differ only by the capital letter —{@code getLayoutInfo} and
 * {@code GetLayoutInfo}— and it is not a mistake: the capitalized ones are from 1.1 and stayed for
 * compatibility. Both do the same. Here they, and {@code addLayoutComponent(String, Component)},
 * are marked deprecated; the JDK only calls them obsolete and does not deprecate them.
 */
public class GridBagLayout implements LayoutManager2, Serializable {

    private static final long serialVersionUID = 8838754796412211005L;

    /** The maximum size of the grid. */
    protected static final int MAXGRIDSIZE = 512;

    /** The smallest possible grid size. */
    protected static final int MINSIZE = 1;

    /** The flag for asking for preferred sizes instead of minimum ones. */
    protected static final int PREFERREDSIZE = 2;

    /** Each child's constraints. */
    protected Hashtable<Component, GridBagConstraints> comptable =
            new Hashtable<Component, GridBagConstraints>();

    /** What a child added without constraints gets. */
    protected GridBagConstraints defaultConstraints = new GridBagConstraints();

    /** The computed grid, or `null` if it has to be recomputed. */
    protected GridBagLayoutInfo layoutInfo;

    /** Minimum widths per column, if they are to be imposed from outside. */
    public int[] columnWidths;

    /** Minimum heights per row. */
    public int[] rowHeights;

    /** Minimum weights per column. */
    public double[] columnWeights;

    /** Minimum weights per row. */
    public double[] rowWeights;

    /** An empty layout. */
    public GridBagLayout() {
    }

    /**
     * Sets a child's constraints.
     *
     * <p>A **copy** is stored: constraints are mutable, and whoever passed them may keep using the
     * same object for the next child, which is exactly how this class is used.
     *
     * @throws NullPointerException if the constraints are `null`
     */
    public void setConstraints(Component comp, GridBagConstraints constraints) {
        this.comptable.put(comp, (GridBagConstraints) constraints.clone());
    }

    /**
     * A child's constraints.
     *
     * @return a copy; changing it changes nothing until it is set again with {@link
     *     #setConstraints}
     */
    public GridBagConstraints getConstraints(Component comp) {
        GridBagConstraints c = this.comptable.get(comp);
        if (c == null) {
            this.setConstraints(comp, this.defaultConstraints);
            c = this.comptable.get(comp);
        }
        return (GridBagConstraints) c.clone();
    }

    /**
     * A child's constraints, **without** copying.
     *
     * <p>It is for the layout's internal use: returning the real object avoids one copy per child
     * and per pass, and there are several passes.
     */
    protected GridBagConstraints lookupConstraints(Component comp) {
        GridBagConstraints c = this.comptable.get(comp);
        if (c == null) {
            this.setConstraints(comp, this.defaultConstraints);
            c = this.comptable.get(comp);
        }
        return c;
    }

    /** Removes a child's constraints. */
    private void removeConstraints(Component comp) {
        this.comptable.remove(comp);
    }

    /**
     * Where the grid starts within the container.
     *
     * @return the top-left corner, or (0,0) if it has not been laid out yet
     */
    public Point getLayoutOrigin() {
        Point origin = new Point(0, 0);
        if (this.layoutInfo != null) {
            origin.x = this.layoutInfo.startx;
            origin.y = this.layoutInfo.starty;
        }
        return origin;
    }

    /**
     * How wide each column and how tall each row is.
     *
     * @return two arrays: widths and heights, or two empty ones if it has not been laid out yet
     */
    public int[][] getLayoutDimensions() {
        if (this.layoutInfo == null) {
            return new int[2][0];
        }
        int[][] dim = new int[2][];
        dim[0] = new int[this.layoutInfo.width];
        dim[1] = new int[this.layoutInfo.height];
        System.arraycopy(this.layoutInfo.minWidth, 0, dim[0], 0, this.layoutInfo.width);
        System.arraycopy(this.layoutInfo.minHeight, 0, dim[1], 0, this.layoutInfo.height);
        return dim;
    }

    /**
     * What weight each column and each row has.
     *
     * @return two arrays: horizontal and vertical weights
     */
    public double[][] getLayoutWeights() {
        if (this.layoutInfo == null) {
            return new double[2][0];
        }
        double[][] w = new double[2][];
        w[0] = new double[this.layoutInfo.width];
        w[1] = new double[this.layoutInfo.height];
        System.arraycopy(this.layoutInfo.weightX, 0, w[0], 0, this.layoutInfo.width);
        System.arraycopy(this.layoutInfo.weightY, 0, w[1], 0, this.layoutInfo.height);
        return w;
    }

    /**
     * Which cell that point of the container falls in.
     *
     * <p>A point to the left of the grid gives column 0 and one to the right gives the number of
     * columns: the result is always a valid cell to insert at, even if the point falls outside.
     */
    public Point location(int x, int y) {
        Point loc = new Point(0, 0);
        if (this.layoutInfo == null) {
            return loc;
        }
        int d = this.layoutInfo.startx;
        int i;
        for (i = 0; i < this.layoutInfo.width; i++) {
            d = d + this.layoutInfo.minWidth[i];
            if (d > x) {
                break;
            }
        }
        loc.x = i;
        d = this.layoutInfo.starty;
        for (i = 0; i < this.layoutInfo.height; i++) {
            d = d + this.layoutInfo.minHeight[i];
            if (d > y) {
                break;
            }
        }
        loc.y = i;
        return loc;
    }

    /**
     * Adds a child with constraints.
     *
     * @throws IllegalArgumentException if the constraints are not a {@link GridBagConstraints}
     */
    public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints == null) {
            this.setConstraints(comp, this.defaultConstraints);
        } else if (constraints instanceof GridBagConstraints) {
            this.setConstraints(comp, (GridBagConstraints) constraints);
        } else {
            throw new IllegalArgumentException(
                    "cannot add to layout: constraints must be a GridBagConstraint");
        }
    }

    /**
     * Adds a child by name.
     *
     * @deprecated this layout does not use names: it does nothing. Use
     *     {@link #addLayoutComponent(Component, Object)}.
     */
    @Deprecated
    public void addLayoutComponent(String name, Component comp) {
    }

    /** Removes that child's constraints. */
    public void removeLayoutComponent(Component comp) {
        this.removeConstraints(comp);
    }

    /** What the grid needs with each child at its preferred size. */
    public Dimension preferredLayoutSize(Container parent) {
        GridBagLayoutInfo info = this.getLayoutInfo(parent, PREFERREDSIZE);
        return this.getMinSize(parent, info);
    }

    /** The same, with the minimum sizes. */
    public Dimension minimumLayoutSize(Container parent) {
        GridBagLayoutInfo info = this.getLayoutInfo(parent, MINSIZE);
        return this.getMinSize(parent, info);
    }

    /** No limit: the weighted columns make use of everything they are given. */
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /** Centred. */
    public float getLayoutAlignmentX(Container parent) {
        return 0.5f;
    }

    /** Centred. */
    public float getLayoutAlignmentY(Container parent) {
        return 0.5f;
    }

    /** Throws away the computed grid: the next query builds it again. */
    public void invalidateLayout(Container target) {
        this.layoutInfo = null;
    }

    /** Builds the grid and places the children. */
    public void layoutContainer(Container parent) {
        this.arrangeGrid(parent);
    }

    /**
     * Computes the grid: how many rows and columns, how big each one is and how much it weighs.
     *
     * <p>It goes in two passes and cannot be done in one. The first resolves the **relative**
     * positions —a child with {@code gridx} set to {@code RELATIVE} goes after the previous one—
     * and on the way finds out the grid's size. Only with the grid sized does the second distribute
     * the widths and the weights, because a child that spans three columns has to spread its size
     * over the three, and which ones they are is not known until the first pass is done.
     *
     * @param sizeflag {@link #MINSIZE} or {@link #PREFERREDSIZE}
     */
    protected GridBagLayoutInfo getLayoutInfo(Container parent, int sizeflag) {
        synchronized (parent.getTreeLock()) {
            int ncomponents = parent.getComponentCount();
            // --- first pass: resolve positions and find out the grid's size
            int[] gx = new int[ncomponents];
            int[] gy = new int[ncomponents];
            int[] gw = new int[ncomponents];
            int[] gh = new int[ncomponents];
            int cursorX = 0;
            int cursorY = 0;
            int maxX = 0;
            int maxY = 0;
            for (int i = 0; i < ncomponents; i++) {
                Component comp = parent.getComponent(i);
                GridBagConstraints c = this.lookupConstraints(comp);
                int w = c.gridwidth;
                int h = c.gridheight;
                if (w <= 0) {
                    w = 1;
                }
                if (h <= 0) {
                    h = 1;
                }
                int x = c.gridx;
                int y = c.gridy;
                if (x == GridBagConstraints.RELATIVE) {
                    x = cursorX;
                }
                if (y == GridBagConstraints.RELATIVE) {
                    y = cursorY;
                }
                gx[i] = x;
                gy[i] = y;
                gw[i] = w;
                gh[i] = h;
                // A child with gridwidth REMAINDER closes the row: the next one starts below.
                if (c.gridwidth == GridBagConstraints.REMAINDER) {
                    cursorX = 0;
                    cursorY = y + h;
                } else {
                    cursorX = x + w;
                    cursorY = y;
                }
                maxX = Math.max(maxX, x + w);
                maxY = Math.max(maxY, y + h);
            }
            if (maxX == 0) {
                maxX = 1;
            }
            if (maxY == 0) {
                maxY = 1;
            }
            GridBagLayoutInfo info = new GridBagLayoutInfo(maxX, maxY);
            // --- second pass: distribute sizes and weights
            for (int i = 0; i < ncomponents; i++) {
                Component comp = parent.getComponent(i);
                if (!comp.isVisible()) {
                    continue;
                }
                GridBagConstraints c = this.lookupConstraints(comp);
                Dimension d = sizeflag == PREFERREDSIZE ? comp.getPreferredSize()
                        : comp.getMinimumSize();
                // It is recorded in the constraints: `adjustForGravity` needs it later, and
                // measuring again there would be measuring the same thing twice.
                c.minWidth = d.width;
                c.minHeight = d.height;
                int totalWidth = d.width + c.insets.left + c.insets.right + c.ipadx;
                int totalHeight = d.height + c.insets.top + c.insets.bottom + c.ipady;
                spreadSize(info.minWidth, gx[i], gw[i], totalWidth);
                spreadSize(info.minHeight, gy[i], gh[i], totalHeight);
                spreadWeight(info.weightX, gx[i], gw[i], c.weightx);
                spreadWeight(info.weightY, gy[i], gh[i], c.weighty);
            }
            // What the user has imposed from outside is a floor, not a replacement.
            impose(info.minWidth, this.columnWidths);
            impose(info.minHeight, this.rowHeights);
            imposeWeights(info.weightX, this.columnWeights);
            imposeWeights(info.weightY, this.rowWeights);
            return info;
        }
    }

    /**
     * Spreads a child's size over the cells it occupies.
     *
     * <p>A child that occupies a single cell imposes its size directly. One that occupies several
     * only requires that **the sum** be enough: if it already is, nothing is touched, and if not,
     * the difference is added to the last one. Splitting it in equal parts would be worse — it
     * would widen columns that do not need it.
     */
    private static void spreadSize(int[] sizes, int start, int count, int total) {
        if (start < 0 || start + count > sizes.length) {
            return;
        }
        if (count == 1) {
            sizes[start] = Math.max(sizes[start], total);
            return;
        }
        int sum = 0;
        for (int i = start; i < start + count; i++) {
            sum = sum + sizes[i];
        }
        if (sum < total) {
            sizes[start + count - 1] = sizes[start + count - 1] + (total - sum);
        }
    }

    /**
     * The same for the weights: a child spanning several cells only requires that their sum reach
     * its weight, and the difference goes to the last one. (This javadoc said the child's weight is
     * that of the largest cell.)
     */
    private static void spreadWeight(double[] weights, int start, int count, double weight) {
        if (weight <= 0 || start < 0 || start + count > weights.length) {
            return;
        }
        if (count == 1) {
            weights[start] = Math.max(weights[start], weight);
            return;
        }
        double sum = 0;
        for (int i = start; i < start + count; i++) {
            sum = sum + weights[i];
        }
        if (sum < weight) {
            weights[start + count - 1] = weights[start + count - 1] + (weight - sum);
        }
    }

    /** Applies the minimums imposed from outside. */
    private static void impose(int[] dest, int[] imposed) {
        if (imposed == null) {
            return;
        }
        int n = Math.min(dest.length, imposed.length);
        for (int i = 0; i < n; i++) {
            dest[i] = Math.max(dest[i], imposed[i]);
        }
    }

    /** The same for the weights. */
    private static void imposeWeights(double[] dest, double[] imposed) {
        if (imposed == null) {
            return;
        }
        int n = Math.min(dest.length, imposed.length);
        for (int i = 0; i < n; i++) {
            dest[i] = Math.max(dest[i], imposed[i]);
        }
    }

    /**
     * Computes the grid.
     *
     * @deprecated the capitalized name is from 1.1. Use {@link #getLayoutInfo}.
     */
    @Deprecated
    protected GridBagLayoutInfo GetLayoutInfo(Container parent, int sizeflag) {
        return this.getLayoutInfo(parent, sizeflag);
    }

    /**
     * Adjusts a child's rectangle according to its fill and its anchor.
     *
     * <p>It is where `fill` and `anchor` are really applied: the rectangle that comes in is the
     * cell it got and the one that goes out is where the component will sit inside it. Only the
     * nine absolute anchors move it; a relative or baseline anchor, which the JDK resolves against
     * the orientation or the baseline, leaves it at the top-left of the cell.
     */
    protected void adjustForGravity(GridBagConstraints constraints, Rectangle r) {
        int diffx = 0;
        int diffy = 0;
        r.x = r.x + constraints.insets.left;
        r.width = r.width - (constraints.insets.left + constraints.insets.right);
        r.y = r.y + constraints.insets.top;
        r.height = r.height - (constraints.insets.top + constraints.insets.bottom);
        // Without `fill`, the component keeps its size, and what is left of the cell is `diffx`,
        // which the anchor then splits between the two sides.
        int ownWidth = constraints.minWidth + constraints.ipadx;
        if (constraints.fill != GridBagConstraints.HORIZONTAL
                && constraints.fill != GridBagConstraints.BOTH
                && r.width > ownWidth) {
            diffx = r.width - ownWidth;
            r.width = ownWidth;
        }
        int ownHeight = constraints.minHeight + constraints.ipady;
        if (constraints.fill != GridBagConstraints.VERTICAL
                && constraints.fill != GridBagConstraints.BOTH
                && r.height > ownHeight) {
            diffy = r.height - ownHeight;
            r.height = ownHeight;
        }
        int a = constraints.anchor;
        if (a == GridBagConstraints.CENTER) {
            r.x = r.x + diffx / 2;
            r.y = r.y + diffy / 2;
        } else if (a == GridBagConstraints.NORTH) {
            r.x = r.x + diffx / 2;
        } else if (a == GridBagConstraints.NORTHEAST) {
            r.x = r.x + diffx;
        } else if (a == GridBagConstraints.EAST) {
            r.x = r.x + diffx;
            r.y = r.y + diffy / 2;
        } else if (a == GridBagConstraints.SOUTHEAST) {
            r.x = r.x + diffx;
            r.y = r.y + diffy;
        } else if (a == GridBagConstraints.SOUTH) {
            r.x = r.x + diffx / 2;
            r.y = r.y + diffy;
        } else if (a == GridBagConstraints.SOUTHWEST) {
            r.y = r.y + diffy;
        } else if (a == GridBagConstraints.WEST) {
            r.y = r.y + diffy / 2;
        }
    }

    /**
     * Adjusts a child's rectangle.
     *
     * @deprecated the capitalized name is from 1.1. Use {@link #adjustForGravity}.
     */
    @Deprecated
    protected void AdjustForGravity(GridBagConstraints constraints, Rectangle r) {
        this.adjustForGravity(constraints, r);
    }

    /** The sum of the columns and of the rows, plus the container's insets. */
    protected Dimension getMinSize(Container parent, GridBagLayoutInfo info) {
        if (info == null) {
            return new Dimension(0, 0);
        }
        int t = 0;
        for (int i = 0; i < info.width; i++) {
            t = t + info.minWidth[i];
        }
        int u = 0;
        for (int i = 0; i < info.height; i++) {
            u = u + info.minHeight[i];
        }
        Insets insets = parent.getInsets();
        return new Dimension(t + insets.left + insets.right, u + insets.top + insets.bottom);
    }

    /**
     * The sum of the grid.
     *
     * @deprecated the capitalized name is from 1.1. Use {@link #getMinSize}.
     */
    @Deprecated
    protected Dimension GetMinSize(Container parent, GridBagLayoutInfo info) {
        return this.getMinSize(parent, info);
    }

    /**
     * Places the children.
     *
     * <p>The spare space is distributed **by weight**, and there is the part that surprises: the
     * weight belongs to the column, not to the component. Only afterwards, with the cell already
     * sized, {@link #adjustForGravity} decides whether the component fills it or stays anchored
     * inside.
     */
    protected void arrangeGrid(Container parent) {
        synchronized (parent.getTreeLock()) {
            int ncomponents = parent.getComponentCount();
            if (ncomponents == 0) {
                return;
            }
            GridBagLayoutInfo info = this.getLayoutInfo(parent, PREFERREDSIZE);
            Dimension d = this.getMinSize(parent, info);
            if (parent.getWidth() < d.width || parent.getHeight() < d.height) {
                info = this.getLayoutInfo(parent, MINSIZE);
                d = this.getMinSize(parent, info);
            }
            this.layoutInfo = info;
            Insets insets = parent.getInsets();
            int spareX = parent.getWidth() - d.width;
            int spareY = parent.getHeight() - d.height;
            distributeSpare(info.minWidth, info.weightX, spareX);
            distributeSpare(info.minHeight, info.weightY, spareY);
            info.startx = insets.left;
            info.starty = insets.top;
            // Where each column and each row starts, accumulating.
            int[] xs = new int[info.width + 1];
            xs[0] = info.startx;
            for (int i = 0; i < info.width; i++) {
                xs[i + 1] = xs[i] + info.minWidth[i];
            }
            int[] ys = new int[info.height + 1];
            ys[0] = info.starty;
            for (int i = 0; i < info.height; i++) {
                ys[i + 1] = ys[i] + info.minHeight[i];
            }
            int cursorX = 0;
            int cursorY = 0;
            for (int i = 0; i < ncomponents; i++) {
                Component comp = parent.getComponent(i);
                GridBagConstraints c = this.lookupConstraints(comp);
                int w = c.gridwidth <= 0 ? 1 : c.gridwidth;
                int h = c.gridheight <= 0 ? 1 : c.gridheight;
                int x = c.gridx == GridBagConstraints.RELATIVE ? cursorX : c.gridx;
                int y = c.gridy == GridBagConstraints.RELATIVE ? cursorY : c.gridy;
                if (c.gridwidth == GridBagConstraints.REMAINDER) {
                    w = Math.max(1, info.width - x);
                    cursorX = 0;
                    cursorY = y + h;
                } else {
                    cursorX = x + w;
                    cursorY = y;
                }
                if (!comp.isVisible()) {
                    continue;
                }
                if (x < 0 || y < 0 || x + w > info.width || y + h > info.height) {
                    continue;
                }
                Rectangle r = new Rectangle(xs[x], ys[y], xs[x + w] - xs[x], ys[y + h] - ys[y]);
                this.adjustForGravity(c, r);
                comp.setBounds(r.x, r.y, r.width, r.height);
            }
        }
    }

    /**
     * Distributes the space left over among the cells, in proportion to their weight.
     *
     * <p>If no weight is positive nothing is distributed: the grid stays at its natural size,
     * starting at the container's top-left inset. This javadoc said it is centred in the container;
     * the JDK centres it, but this implementation does not.
     */
    private static void distributeSpare(int[] sizes, double[] weights, int spare) {
        if (spare <= 0) {
            return;
        }
        double total = 0;
        for (int i = 0; i < weights.length; i++) {
            total = total + weights[i];
        }
        if (total <= 0) {
            return;
        }
        int distributed = 0;
        for (int i = 0; i < sizes.length; i++) {
            int share = (int) (spare * (weights[i] / total));
            sizes[i] = sizes[i] + share;
            distributed = distributed + share;
        }
        // The remainder of the integer division goes to the last weighted cell: if it were left
        // undistributed, the grid would fall a few pixels short of filling the container.
        if (distributed < spare) {
            for (int i = sizes.length - 1; i >= 0; i--) {
                if (weights[i] > 0) {
                    sizes[i] = sizes[i] + (spare - distributed);
                    break;
                }
            }
        }
    }

    /**
     * Places the children.
     *
     * @deprecated the capitalized name is from 1.1. Use {@link #arrangeGrid}.
     */
    @Deprecated
    protected void ArrangeGrid(Container parent) {
        this.arrangeGrid(parent);
    }

    public String toString() {
        return this.getClass().getName();
    }
}
