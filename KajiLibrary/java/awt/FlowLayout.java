package java.awt;

import java.io.Serializable;

/**
 * Puts the children in a row and wraps to a new line when they do not fit.
 *
 * <p>It is the layout of a paragraph, applied to components. Each child keeps its preferred size
 * and they are placed from left to right; when the next one does not fit, the line is broken.
 *
 * <p>{@link #LEADING} and {@link #TRAILING} are not synonyms of left and right: they follow the
 * **container's orientation**, so in a text that reads right to left they flip by themselves. It is
 * the difference between an interface that translates well and one that has to be redone.
 *
 * <p>Baseline alignment lines the children up by the text's baseline and not by the top edge, which
 * is what makes a label next to a field look really aligned. This implementation stores that
 * setting but does not apply it: rows are always centred vertically.
 */
public class FlowLayout implements LayoutManager, Serializable {

    private static final long serialVersionUID = -7262534875583282631L;

    /** Flush to the left. */
    public static final int LEFT = 0;

    /** Centred. */
    public static final int CENTER = 1;

    /** Flush to the right. */
    public static final int RIGHT = 2;

    /** Flush to the side where the text starts. */
    public static final int LEADING = 3;

    /** Flush to the side where it ends. */
    public static final int TRAILING = 4;

    private int align;
    private int hgap;
    private int vgap;
    private boolean alignOnBaseline;

    /** Centred, with a five-pixel gap. */
    public FlowLayout() {
        this(CENTER, 5, 5);
    }

    /** With that alignment and a five-pixel gap. */
    public FlowLayout(int align) {
        this(align, 5, 5);
    }

    /** With the given alignment and gaps. */
    public FlowLayout(int align, int hgap, int vgap) {
        this.hgap = hgap;
        this.vgap = vgap;
        this.setAlignment(align);
    }

    /** How they align within the line. */
    public int getAlignment() {
        return this.align;
    }

    /**
     * Changes the alignment; a value that is not one of the five is taken as centred. (The JDK
     * stores it as given.)
     */
    public void setAlignment(int align) {
        if (align < LEFT || align > TRAILING) {
            this.align = CENTER;
        } else {
            this.align = align;
        }
    }

    /** How much they are separated horizontally. */
    public int getHgap() {
        return this.hgap;
    }

    /** Changes the horizontal gap. */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /** How much they are separated vertically. */
    public int getVgap() {
        return this.vgap;
    }

    /** Changes the vertical gap. */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    /** Declares whether they align on the text's baseline; see the class note. */
    public void setAlignOnBaseline(boolean alignOnBaseline) {
        this.alignOnBaseline = alignOnBaseline;
    }

    /** Whether they align on the baseline. */
    public boolean getAlignOnBaseline() {
        return this.alignOnBaseline;
    }

    /** It does nothing: this layout keeps nothing per child. */
    public void addLayoutComponent(String name, Component comp) {
    }

    /** It does nothing, for the same reason. */
    public void removeLayoutComponent(Component comp) {
    }

    /**
     * What the container needs to put everything **on a single line**.
     *
     * <p>That is on purpose: the preferred size of a row is that of the whole row. If it does not
     * fit, only then is it broken, but preferring an already broken size would leave the container
     * no room to grow.
     */
    public Dimension preferredLayoutSize(Container target) {
        return this.measure(target, true);
    }

    /** The minimum, with each child at its minimum size. */
    public Dimension minimumLayoutSize(Container target) {
        return this.measure(target, false);
    }

    /** The sum of the widths and the height of the tallest, plus the gaps and the insets. */
    private Dimension measure(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);
            int n = target.getComponentCount();
            boolean first = true;
            for (int i = 0; i < n; i++) {
                Component m = target.getComponent(i);
                if (!m.isVisible()) {
                    continue;
                }
                Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                dim.height = Math.max(dim.height, d.height);
                if (!first) {
                    dim.width = dim.width + this.hgap;
                }
                dim.width = dim.width + d.width;
                first = false;
            }
            Insets insets = target.getInsets();
            dim.width = dim.width + insets.left + insets.right + this.hgap * 2;
            dim.height = dim.height + insets.top + insets.bottom + this.vgap * 2;
            return dim;
        }
    }

    /**
     * Lays the children out in lines.
     *
     * <p>Each line is placed **when it closes**, not while it fills: until it is known how many
     * fit, it is not known how much space is left over, and without that one cannot centre or
     * right-align.
     */
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int maxwidth = target.getWidth() - (insets.left + insets.right + this.hgap * 2);
            int n = target.getComponentCount();
            int x = 0;
            int y = insets.top + this.vgap;
            int rowh = 0;
            int start = 0;
            boolean ltr = target.getComponentOrientation().isLeftToRight();
            for (int i = 0; i < n; i++) {
                Component m = target.getComponent(i);
                if (!m.isVisible()) {
                    continue;
                }
                Dimension d = m.getPreferredSize();
                m.setSize(d.width, d.height);
                if (x == 0 || x + d.width <= maxwidth) {
                    if (x > 0) {
                        x = x + this.hgap;
                    }
                    x = x + d.width;
                    rowh = Math.max(rowh, d.height);
                } else {
                    this.placeRow(target, start, i, maxwidth - x, y, rowh, ltr);
                    x = d.width;
                    y = y + this.vgap + rowh;
                    rowh = d.height;
                    start = i;
                }
            }
            this.placeRow(target, start, n, maxwidth - x, y, rowh, ltr);
        }
    }

    /** Places the children of an already closed line, distributing the space left over. */
    private void placeRow(Container target, int rowStart, int rowEnd, int spare, int y,
            int height, boolean ltr) {
        int a = this.align;
        // LEADING and TRAILING are resolved to left or right according to the orientation: this is
        // where an interface in Arabic flips by itself.
        if (a == LEADING) {
            a = ltr ? LEFT : RIGHT;
        } else if (a == TRAILING) {
            a = ltr ? RIGHT : LEFT;
        }
        Insets insets = target.getInsets();
        int x = insets.left + this.hgap;
        if (a == CENTER) {
            x = x + spare / 2;
        } else if (a == RIGHT) {
            x = x + spare;
        }
        for (int i = rowStart; i < rowEnd; i++) {
            Component m = target.getComponent(i);
            if (!m.isVisible()) {
                continue;
            }
            int cy = y + (height - m.getHeight()) / 2;
            m.setLocation(x, cy);
            x = x + m.getWidth() + this.hgap;
        }
    }

    public String toString() {
        String s;
        if (this.align == LEFT) {
            s = ",align=left";
        } else if (this.align == CENTER) {
            s = ",align=center";
        } else if (this.align == RIGHT) {
            s = ",align=right";
        } else if (this.align == LEADING) {
            s = ",align=leading";
        } else {
            s = ",align=trailing";
        }
        return this.getClass().getName() + "[hgap=" + this.hgap + ",vgap=" + this.vgap + s + "]";
    }
}
