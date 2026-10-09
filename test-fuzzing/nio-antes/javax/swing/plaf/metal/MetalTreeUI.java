package javax.swing.plaf.metal;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreePath;

/**
 * Metal's tree.
 *
 * <h2>Three line styles, chosen by the program</h2>
 *
 * <p>Metal is the only look and feel that lets one choose how the lines joining the branches are
 * drawn, and not through a property of the tree's but through a <em>client property</em>:
 * {@code tree.putClientProperty("JTree.lineStyle", "Angled")}. The three values are
 * {@code "Angled"} -- the usual staircase --, {@code "Horizontal"} -- a line between
 * first-level nodes, with no verticals -- and {@code "None"}.
 *
 * <p>That it is a client property and not a normal property is what allows it to exist without
 * dirtying {@code JTree}'s API with something only a look and feel understands. The price is
 * that there is no way of discovering it by looking at the methods.
 *
 * <h2>The handle's sensitive area is wider than the handle</h2>
 *
 * <p>{@link #isLocationInExpandControl} accepts twenty-five columns for an eighteen-pixel icon:
 * the icon's width plus twice {@link #getHorizontalLegBuffer}, which in Metal is worth three.
 * Measured, and it is deliberate: the handle is small and missing it by two pixels should not
 * mean selecting the node instead of opening it.
 *
 * <p>Note that this method has <em>four</em> parameters and does not match the basic one's: it
 * takes the row and its level already computed, instead of the path. It does not override it --
 * it adds it --, so the two versions coexist.
 */
public class MetalTreeUI extends BasicTreeUI {

    /** The three styles; see the class note. */
    private static final int ANGLED = 1;
    private static final int HORIZONTAL = 2;
    private static final int NONE = 3;

    private static final String LINE_STYLE_KEY = "JTree.lineStyle";

    private int style = ANGLED;

    public MetalTreeUI() {
    }

    public static ComponentUI createUI(JComponent x) {
        return new MetalTreeUI();
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        decodeLineStyle(c.getClientProperty(LINE_STYLE_KEY));
        // The handles: the basic one leaves them null because they come from the look and feel's
                // table, and Metal draws them. On their width -- eighteen -- depends the sensitive
                // area; see the class note.
        if (getExpandedIcon() == null) {
            setExpandedIcon(MetalIconFactory.getTreeControlIcon(false));
        }
        if (getCollapsedIcon() == null) {
            setCollapsedIcon(MetalIconFactory.getTreeControlIcon(true));
        }
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
    }

    /**
     * It translates the client property's value.
     *
     * <p>Anything that is not one of the three texts leaves the style at {@code "Angled"}, which is
     * the default one. It does not throw: a client property is written by anybody and breaking the
     * tree over a mistyped text would be worse than ignoring it.
     */
    protected void decodeLineStyle(Object lineStyleFlag) {
        if ("Horizontal".equals(lineStyleFlag)) {
            style = HORIZONTAL;
        } else if ("None".equals(lineStyleFlag)) {
            style = NONE;
        } else {
            style = ANGLED;
        }
    }

    /** Three; see the class note. */
    protected int getHorizontalLegBuffer() {
        return 3;
    }

    /**
     * Whether that point falls on that row's handle.
     *
     * @param row the row
     * @param rowLevel its level in the tree
     * @param mouseX the horizontal coordinate
     * @param mouseY the vertical one, which is not looked at
     */
    protected boolean isLocationInExpandControl(int row, int rowLevel, int mouseX, int mouseY) {
        if (tree == null || isLeaf(row)) {
            return false;
        }
        int width = ((getExpandedIcon() != null) ? getExpandedIcon().getIconWidth() : 8)
                + 2 * getHorizontalLegBuffer();
        Insets i = tree.getInsets();
        int left = ((i != null) ? i.left : 0)
                + (((rowLevel + depthOffset - 1) * totalChildIndent) + getLeftChildIndent())
                - width / 2;
        return mouseX >= left && mouseX <= left + width;
    }

    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);
        if (style == HORIZONTAL) {
            paintHorizontalSeparators(g, c);
        }
    }

    /** The line between first-level nodes of the {@code "Horizontal"} style. */
    protected void paintHorizontalSeparators(Graphics g, JComponent c) {
        g.setColor(MetalLookAndFeel.getPrimaryControl());
        Rectangle clip = g.getClipBounds();
        if (clip == null) {
            return;
        }
        TreePath from = getClosestPathForLocation(tree, 0, clip.y);
        TreePath to = getClosestPathForLocation(tree, 0, clip.y + clip.height);
        if (from == null || to == null) {
            return;
        }
        int first = getRowForPath(tree, from);
        int last = getRowForPath(tree, to);
        for (int row = first; row <= last; row++) {
            TreePath p = getPathForRow(tree, row);
            if (p != null && p.getPathCount() == 2) {
                Rectangle b = getPathBounds(tree, p);
                if (b != null) {
                    g.drawLine(clip.x, b.y, clip.x + clip.width, b.y);
                }
            }
        }
    }

    protected void paintVerticalPartOfLeg(Graphics g, Rectangle clipBounds, Insets insets,
            TreePath path) {
        if (style == ANGLED) {
            super.paintVerticalPartOfLeg(g, clipBounds, insets, path);
        }
    }

    protected void paintHorizontalPartOfLeg(Graphics g, Rectangle clipBounds, Insets insets,
            Rectangle bounds, TreePath path, int row, boolean isExpanded,
            boolean hasBeenExpanded, boolean isLeaf) {
        if (style == ANGLED) {
            super.paintHorizontalPartOfLeg(g, clipBounds, insets, bounds, path, row,
                    isExpanded, hasBeenExpanded, isLeaf);
        }
    }
}
