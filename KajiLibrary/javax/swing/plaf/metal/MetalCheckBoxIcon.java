package javax.swing.plaf.metal;

import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.plaf.UIResource;

/**
 * The little square of a Metal check box: thirteen by thirteen.
 *
 * <p>The size comes from {@link #getControlSize}, which is {@code protected} on purpose: a theme
 * that wants larger boxes inherits and changes that number, and the rest -- the frame, the tick,
 * the four states -- goes on working.
 *
 * <p>The tick is also separate, in {@link #drawCheck}, for the same reason: it is the part a
 * theme might want to draw differently -- a cross, a dot -- without touching the frame.
 *
 * <p>The icon draws four states and not two: on and off, each enabled or not. A disabled square
 * is not a grey square; it is a square <strong>with no sunken frame</strong>, because what
 * communicates that something does not respond is that it stops having relief.
 */
public class MetalCheckBoxIcon implements Icon, UIResource, Serializable {

    public MetalCheckBoxIcon() {
    }

    /** Thirteen; see the class note. */
    protected int getControlSize() {
        return 13;
    }

    public int getIconWidth() {
        return getControlSize();
    }

    public int getIconHeight() {
        return getControlSize();
    }

    /** The tick: two strokes, the short one down and the long one up. */
    protected void drawCheck(Component c, Graphics g, int x, int y) {
        int side = getControlSize();
        g.fillRect(x + 3, y + 5, 2, side - 8);
        g.drawLine(x + (side - 4), y + 3, x + 5, y + (side - 6));
        g.drawLine(x + (side - 4), y + 4, x + 5, y + (side - 5));
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        int side = getControlSize();
        ButtonModel m = (c instanceof AbstractButton)
                ? ((AbstractButton) c).getModel() : null;
        boolean on = (m != null) && m.isSelected();
        boolean enabled = (m == null) || m.isEnabled();
        boolean pressed = (m != null) && m.isPressed() && m.isArmed();

        if (enabled) {
            // The sunken frame: dark at the top and on the left, light at the bottom and on the
            // right.
            g.setColor(pressed ? MetalLookAndFeel.getControlShadow()
                    : MetalLookAndFeel.getControlDarkShadow());
            g.drawLine(x, y, x + side - 1, y);
            g.drawLine(x, y, x, y + side - 1);
            g.setColor(MetalLookAndFeel.getControlHighlight());
            g.drawLine(x + side - 1, y, x + side - 1, y + side - 1);
            g.drawLine(x, y + side - 1, x + side - 1, y + side - 1);
            g.setColor(MetalLookAndFeel.getControlInfo());
        } else {
            // Without relief; see the class note.
            g.setColor(MetalLookAndFeel.getControlShadow());
            g.drawRect(x, y, side - 1, side - 1);
        }
        if (on) {
            drawCheck(c, g, x, y);
        }
    }
}
