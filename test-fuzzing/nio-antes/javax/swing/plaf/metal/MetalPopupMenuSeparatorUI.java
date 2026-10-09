package javax.swing.plaf.metal;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * The separator inside a menu.
 *
 * <p>It is the same two-line groove as {@link MetalSeparatorUI}, but it asks for four pixels of
 * height instead of two and draws the lines in the middle. The two pixels of air on each side are
 * what separates the groove from the items above and below; without them a menu is cramped and
 * the separator is confused with an item's border.
 *
 * <p>The orientation is not looked at: a menu separator is always horizontal.
 */
public class MetalPopupMenuSeparatorUI extends MetalSeparatorUI {

    public MetalPopupMenuSeparatorUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalPopupMenuSeparatorUI();
    }

    public void paint(Graphics g, JComponent c) {
        Dimension s = c.getSize();
        g.setColor(c.getForeground());
        g.drawLine(0, 1, s.width, 1);
        g.setColor(c.getBackground());
        g.drawLine(0, 2, s.width, 2);
    }

    /** Four: two of line and two of air; see the class note. */
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(0, 4);
    }
}
