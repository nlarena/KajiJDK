package javax.swing.plaf.basic;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * A popup menu's separator.
 *
 * <p>The same as {@link BasicSeparatorUI} save for one thing: inside a menu there is no more
 * than one possible orientation, so it does not look at it, and the two lines are drawn
 * <em>below</em> the top margin instead of flush against the component's edge. That is why it
 * redefines {@link #paint} and {@link #getPreferredSize}.
 */
public class BasicPopupMenuSeparatorUI extends BasicSeparatorUI {

    public BasicPopupMenuSeparatorUI() {
    }

    /** A new one each time, like the one of the class it comes from. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicPopupMenuSeparatorUI();
    }

    /** The two lines, starting after the top margin. */
    public void paint(Graphics g, JComponent c) {
        Dimension s = c.getSize();
        Insets insets = c.getInsets();
        int y = insets.top;
        g.setColor(c.getForeground());
        g.drawLine(0, y, s.width, y);
        g.setColor(c.getBackground());
        g.drawLine(0, y + 1, s.width, y + 1);
    }

    /** A height of two plus the margins; the width is set by the menu. */
    public Dimension getPreferredSize(JComponent c) {
        Insets insets = c.getInsets();
        return new Dimension(0, insets.top + insets.bottom + 2);
    }
}
