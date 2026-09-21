package java.awt;

import java.io.Serializable;

/**
 * Five places: the four edges and the centre.
 *
 * <p>It is AWT's most used layout and the one that best distributes spare space: **the centre keeps
 * whatever is left**. The edges get their preferred size in the direction that bounds them —north
 * its height, west its width— and stretch in the other.
 *
 * <p>Order matters: north and south are reserved first across the whole width, then east and west
 * with the height that is left, and the centre takes the rest. That is why a toolbar in the north
 * runs from end to end and a sidebar in the west does not.
 *
 * <p>The constants come in **two sets**, and that is the most confusing part. `NORTH` and company
 * are absolute; {@link #PAGE_START} and {@link #LINE_START} are relative to how the text reads, so
 * in Arabic `LINE_START` is the right. Placing components with both forms at once is a mistake, and
 * the relative position wins.
 */
public class BorderLayout implements LayoutManager2, Serializable {

    private static final long serialVersionUID = -8658291919501921765L;

    /** At the top, from end to end. */
    public static final String NORTH = "North";

    /** At the bottom, from end to end. */
    public static final String SOUTH = "South";

    /** On the right, between north and south. */
    public static final String EAST = "East";

    /** On the left, between north and south. */
    public static final String WEST = "West";

    /** The rest of the space. */
    public static final String CENTER = "Center";

    /** Where the page starts: the top in horizontal scripts. */
    public static final String PAGE_START = "First";

    /** Where the page ends. */
    public static final String PAGE_END = "Last";

    /** Where the line starts: the left, or the right in Arabic and Hebrew. */
    public static final String LINE_START = "Before";

    /** Where the line ends. */
    public static final String LINE_END = "After";

    /**
     * The old name of {@link #PAGE_START}.
     *
     * @deprecated renamed in 1.4 so that the relative set would be consistent.
     */
    @Deprecated
    public static final String BEFORE_FIRST_LINE = PAGE_START;

    /**
     * The old name of {@link #PAGE_END}.
     *
     * @deprecated renamed in 1.4.
     */
    @Deprecated
    public static final String AFTER_LAST_LINE = PAGE_END;

    /**
     * The old name of {@link #LINE_START}.
     *
     * @deprecated renamed in 1.4.
     */
    @Deprecated
    public static final String BEFORE_LINE_BEGINS = LINE_START;

    /**
     * The old name of {@link #LINE_END}.
     *
     * @deprecated renamed in 1.4.
     */
    @Deprecated
    public static final String AFTER_LINE_ENDS = LINE_END;

    private int hgap;
    private int vgap;

    private Component north;
    private Component west;
    private Component east;
    private Component south;
    private Component center;

    private Component firstLine;
    private Component lastLine;
    private Component firstItem;
    private Component lastItem;

    /** With no gap between the five places. */
    public BorderLayout() {
        this(0, 0);
    }

    /** With the given gaps. */
    public BorderLayout(int hgap, int vgap) {
        this.hgap = hgap;
        this.vgap = vgap;
    }

    /** The horizontal gap. */
    public int getHgap() {
        return this.hgap;
    }

    /** Changes the horizontal gap. */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /** The vertical gap. */
    public int getVgap() {
        return this.vgap;
    }

    /** Changes the vertical gap. */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    /**
     * Records which place a child goes in.
     *
     * @param constraints one of the nine constants, or `null` for the centre
     * @throws IllegalArgumentException if it is none of them
     */
    public void addLayoutComponent(Component comp, Object constraints) {
        synchronized (comp.getTreeLock()) {
            Object c = constraints == null ? CENTER : constraints;
            if (!(c instanceof String)) {
                throw new IllegalArgumentException(
                        "cannot add to layout: constraint must be a string (or null)");
            }
            this.place(comp, (String) c);
        }
    }

    /** Stores the child in the slot it belongs to. */
    private void place(Component comp, String name) {
        if (CENTER.equals(name)) {
            this.center = comp;
        } else if (NORTH.equals(name)) {
            this.north = comp;
        } else if (SOUTH.equals(name)) {
            this.south = comp;
        } else if (EAST.equals(name)) {
            this.east = comp;
        } else if (WEST.equals(name)) {
            this.west = comp;
        } else if (PAGE_START.equals(name)) {
            this.firstLine = comp;
        } else if (PAGE_END.equals(name)) {
            this.lastLine = comp;
        } else if (LINE_START.equals(name)) {
            this.firstItem = comp;
        } else if (LINE_END.equals(name)) {
            this.lastItem = comp;
        } else {
            throw new IllegalArgumentException("cannot add to layout: unknown constraint: " + name);
        }
    }

    /**
     * Records which place a child goes in, by name.
     *
     * @deprecated it is from the 1.0 model. Use {@link #addLayoutComponent(Component, Object)}.
     * @throws IllegalArgumentException if the name is not one of the nine
     */
    @Deprecated
    public void addLayoutComponent(String name, Component comp) {
        synchronized (comp.getTreeLock()) {
            this.place(comp, name == null ? CENTER : name);
        }
    }

    /** Removes that child from whichever slot it is in. */
    public void removeLayoutComponent(Component comp) {
        synchronized (comp.getTreeLock()) {
            if (comp == this.center) {
                this.center = null;
            } else if (comp == this.north) {
                this.north = null;
            } else if (comp == this.south) {
                this.south = null;
            } else if (comp == this.east) {
                this.east = null;
            } else if (comp == this.west) {
                this.west = null;
            } else if (comp == this.firstLine) {
                this.firstLine = null;
            } else if (comp == this.lastLine) {
                this.lastLine = null;
            } else if (comp == this.firstItem) {
                this.firstItem = null;
            } else if (comp == this.lastItem) {
                this.lastItem = null;
            }
        }
    }

    /**
     * Which child is in that place.
     *
     * @return the child, or `null` if the place is empty
     * @throws IllegalArgumentException if the position is not one of the nine
     */
    public Component getLayoutComponent(Object constraints) {
        if (CENTER.equals(constraints)) {
            return this.center;
        }
        if (NORTH.equals(constraints)) {
            return this.north;
        }
        if (SOUTH.equals(constraints)) {
            return this.south;
        }
        if (WEST.equals(constraints)) {
            return this.west;
        }
        if (EAST.equals(constraints)) {
            return this.east;
        }
        if (PAGE_START.equals(constraints)) {
            return this.firstLine;
        }
        if (PAGE_END.equals(constraints)) {
            return this.lastLine;
        }
        if (LINE_START.equals(constraints)) {
            return this.firstItem;
        }
        if (LINE_END.equals(constraints)) {
            return this.lastItem;
        }
        throw new IllegalArgumentException("cannot get component: invalid constraint: "
                + constraints);
    }

    /**
     * Which child is in that place, resolving the relative positions for that container.
     *
     * <p>Asking for `NORTH` here returns the one in `PAGE_START` if there is one, and the one in
     * `NORTH` otherwise: it is the query needed when what matters is where it will be drawn and not
     * which constant it was added with. (This javadoc had that precedence the other way round.)
     *
     * @throws IllegalArgumentException if the position is not one of the nine
     */
    public Component getLayoutComponent(Container target, Object constraints) {
        boolean ltr = target.getComponentOrientation().isLeftToRight();
        if (CENTER.equals(constraints)) {
            return this.center;
        }
        if (NORTH.equals(constraints)) {
            return this.firstLine != null ? this.firstLine : this.north;
        }
        if (SOUTH.equals(constraints)) {
            return this.lastLine != null ? this.lastLine : this.south;
        }
        if (WEST.equals(constraints)) {
            Component c = ltr ? this.firstItem : this.lastItem;
            return c != null ? c : this.west;
        }
        if (EAST.equals(constraints)) {
            Component c = ltr ? this.lastItem : this.firstItem;
            return c != null ? c : this.east;
        }
        if (PAGE_START.equals(constraints)) {
            return this.firstLine != null ? this.firstLine : this.north;
        }
        if (PAGE_END.equals(constraints)) {
            return this.lastLine != null ? this.lastLine : this.south;
        }
        if (LINE_START.equals(constraints)) {
            Component c = this.firstItem;
            return c != null ? c : (ltr ? this.west : this.east);
        }
        if (LINE_END.equals(constraints)) {
            Component c = this.lastItem;
            return c != null ? c : (ltr ? this.east : this.west);
        }
        throw new IllegalArgumentException("cannot get component: unknown constraint: "
                + constraints);
    }

    /**
     * Which place that child is in.
     *
     * @return the constant it was added with, or `null` if it is not in this layout
     */
    public Object getConstraints(Component comp) {
        if (comp == null) {
            return null;
        }
        if (comp == this.center) {
            return CENTER;
        }
        if (comp == this.north) {
            return NORTH;
        }
        if (comp == this.south) {
            return SOUTH;
        }
        if (comp == this.west) {
            return WEST;
        }
        if (comp == this.east) {
            return EAST;
        }
        if (comp == this.firstLine) {
            return PAGE_START;
        }
        if (comp == this.lastLine) {
            return PAGE_END;
        }
        if (comp == this.firstItem) {
            return LINE_START;
        }
        if (comp == this.lastItem) {
            return LINE_END;
        }
        return null;
    }

    /** The width of the widest row and the height of everything stacked. */
    public Dimension minimumLayoutSize(Container target) {
        return this.measure(target, false);
    }

    /** The same, with the preferred sizes. */
    public Dimension preferredLayoutSize(Container target) {
        return this.measure(target, true);
    }

    /** North and south add up height; east, west and centre add up width. */
    private Dimension measure(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);
            boolean ltr = target.getComponentOrientation().isLeftToRight();
            Component c = this.getChild(EAST, ltr);
            if (c != null) {
                Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                dim.width = dim.width + d.width + this.hgap;
                dim.height = Math.max(d.height, dim.height);
            }
            c = this.getChild(WEST, ltr);
            if (c != null) {
                Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                dim.width = dim.width + d.width + this.hgap;
                dim.height = Math.max(d.height, dim.height);
            }
            c = this.getChild(CENTER, ltr);
            if (c != null) {
                Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                dim.width = dim.width + d.width;
                dim.height = Math.max(d.height, dim.height);
            }
            c = this.getChild(NORTH, ltr);
            if (c != null) {
                Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                dim.width = Math.max(d.width, dim.width);
                dim.height = dim.height + d.height + this.vgap;
            }
            c = this.getChild(SOUTH, ltr);
            if (c != null) {
                Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                dim.width = Math.max(d.width, dim.width);
                dim.height = dim.height + d.height + this.vgap;
            }
            Insets insets = target.getInsets();
            dim.width = dim.width + insets.left + insets.right;
            dim.height = dim.height + insets.top + insets.bottom;
            return dim;
        }
    }

    /**
     * The child in that place, the relative position taking precedence; invisible ones count as
     * absent.
     */
    private Component getChild(String key, boolean ltr) {
        Component result = null;
        if (NORTH.equals(key)) {
            result = this.firstLine != null ? this.firstLine : this.north;
        } else if (SOUTH.equals(key)) {
            result = this.lastLine != null ? this.lastLine : this.south;
        } else if (WEST.equals(key)) {
            result = ltr ? this.firstItem : this.lastItem;
            if (result == null) {
                result = this.west;
            }
        } else if (EAST.equals(key)) {
            result = ltr ? this.lastItem : this.firstItem;
            if (result == null) {
                result = this.east;
            }
        } else if (CENTER.equals(key)) {
            result = this.center;
        }
        if (result != null && !result.isVisible()) {
            result = null;
        }
        return result;
    }

    /** No limit: the centre makes use of everything it is given. */
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

    /** It keeps no computations between calls, so there is nothing to throw away. */
    public void invalidateLayout(Container target) {
    }

    /**
     * Lays out the five places.
     *
     * <p>The order is what defines the layout: north and south take the full width, east and west
     * the height left over, and the centre what remains. Changing that order would change which
     * component reaches the corners.
     */
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int top = insets.top;
            int bottom = target.getHeight() - insets.bottom;
            int left = insets.left;
            int right = target.getWidth() - insets.right;
            boolean ltr = target.getComponentOrientation().isLeftToRight();
            Component c = this.getChild(NORTH, ltr);
            if (c != null) {
                c.setSize(right - left, c.getHeight());
                Dimension d = c.getPreferredSize();
                c.setBounds(left, top, right - left, d.height);
                top = top + d.height + this.vgap;
            }
            c = this.getChild(SOUTH, ltr);
            if (c != null) {
                c.setSize(right - left, c.getHeight());
                Dimension d = c.getPreferredSize();
                c.setBounds(left, bottom - d.height, right - left, d.height);
                bottom = bottom - d.height - this.vgap;
            }
            c = this.getChild(EAST, ltr);
            if (c != null) {
                c.setSize(c.getWidth(), bottom - top);
                Dimension d = c.getPreferredSize();
                c.setBounds(right - d.width, top, d.width, bottom - top);
                right = right - d.width - this.hgap;
            }
            c = this.getChild(WEST, ltr);
            if (c != null) {
                c.setSize(c.getWidth(), bottom - top);
                Dimension d = c.getPreferredSize();
                c.setBounds(left, top, d.width, bottom - top);
                left = left + d.width + this.hgap;
            }
            c = this.getChild(CENTER, ltr);
            if (c != null) {
                c.setBounds(left, top, right - left, bottom - top);
            }
        }
    }

    public String toString() {
        return this.getClass().getName() + "[hgap=" + this.hgap + ",vgap=" + this.vgap + "]";
    }
}
