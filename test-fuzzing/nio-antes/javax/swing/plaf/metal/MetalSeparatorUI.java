package javax.swing.plaf.metal;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSeparatorUI;

/**
 * Metal's separator: two pixels and two lines.
 *
 * <p>The top line goes in the theme's foreground colour and the bottom one in white. That pair
 * -- one dark and one light, in that order -- is what makes it read as a groove and not as a
 * stripe: the light comes from the top left, so a hollow's upper edge is in shadow and its lower
 * edge catches the light.
 *
 * <p>That is why the preferred size is two pixels and not one, and why Metal's separator looks
 * different from the basic one even though neither draws more than two lines.
 */
public class MetalSeparatorUI extends BasicSeparatorUI {

    public MetalSeparatorUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalSeparatorUI();
    }

    /** White at the bottom and the theme's colour at the top; see the class note. */
    protected void installDefaults(JSeparator s) {
        s.setBackground(MetalLookAndFeel.getSeparatorBackground());
        s.setForeground(MetalLookAndFeel.getSeparatorForeground());
    }

    public void paint(Graphics g, JComponent c) {
        Dimension s = c.getSize();
        if (((JSeparator) c).getOrientation() == SwingConstants.VERTICAL) {
            g.setColor(c.getForeground());
            g.drawLine(0, 0, 0, s.height);
            g.setColor(c.getBackground());
            g.drawLine(1, 0, 1, s.height);
        } else {
            g.setColor(c.getForeground());
            g.drawLine(0, 0, s.width, 0);
            g.setColor(c.getBackground());
            g.drawLine(0, 1, s.width, 1);
        }
    }

    public Dimension getPreferredSize(JComponent c) {
        if (((JSeparator) c).getOrientation() == SwingConstants.VERTICAL) {
            return new Dimension(2, 0);
        }
        return new Dimension(0, 2);
    }
}
