package javax.swing.plaf.metal;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * The divider of a Metal split pane.
 *
 * <p>It is not public and is not in the census, but it is seen:
 * {@code createDefaultDivider().getClass()} returns it, and a program may ask it for its name.
 *
 * <p>The only thing it draws beyond the basic one is the three dots in the middle, which is what
 * tells whoever looks that that grey strip can be dragged. Without them a Metal divider is
 * indistinguishable from a separator.
 */
class MetalSplitPaneDivider extends BasicSplitPaneDivider {

    MetalSplitPaneDivider(BasicSplitPaneUI ui) {
        super(ui);
    }

    public void paint(Graphics g) {
        super.paint(g);
        Dimension s = getSize();
        int cx = s.width / 2;
        int cy = s.height / 2;
        g.setColor(MetalLookAndFeel.getControlDarkShadow());
        boolean horizontal = (splitPane != null)
                && splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT;
        for (int i = -1; i <= 1; i++) {
            if (horizontal) {
                g.fillRect(cx - 1, cy + i * 4 - 1, 2, 2);
            } else {
                g.fillRect(cx + i * 4 - 1, cy - 1, 2, 2);
            }
        }
    }
}
