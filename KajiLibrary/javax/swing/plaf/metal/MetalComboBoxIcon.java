package javax.swing.plaf.metal;

import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.Icon;

/**
 * A Metal combo box's little arrow: ten wide by five high.
 *
 * <p>It is a filled triangle pointing down, drawn row by row: it starts with a line of ten and
 * one is taken off each side on every row. Five rows for ten pixels of width is no coincidence:
 * it is what makes the sides come out at forty-five degrees and not look stepped.
 *
 * <p>It is not a {@code UIResource}, and that matters: a combo box the program set this icon on
 * by hand keeps it even if the look and feel changes. The icons that are {@code UIResource} --
 * the check box's, for instance -- are replaced by themselves.
 */
public class MetalComboBoxIcon implements Icon, Serializable {

    public MetalComboBoxIcon() {
    }

    public int getIconWidth() {
        return 10;
    }

    public int getIconHeight() {
        return 5;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        int width = getIconWidth();
        g.translate(x, y);
        g.setColor((c == null || c.isEnabled())
                ? MetalLookAndFeel.getControlInfo()
                : MetalLookAndFeel.getControlShadow());
        // Row i: it starts at i and ends two pixels before the previous one.
        for (int i = 0; i < getIconHeight(); i++) {
            g.drawLine(i, i, i + (width - 1 - 2 * i), i);
        }
        g.translate(-x, -y);
    }
}
