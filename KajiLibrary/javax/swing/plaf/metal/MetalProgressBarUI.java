package javax.swing.plaf.metal;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;

/**
 * Metal's progress bar.
 *
 * <p>The only thing it adds to the basic one is a shadow line: where the filled part ends a
 * border of the theme's dark colour is drawn. It is what keeps the fill and the background from
 * touching with nothing in between, which halfway along reads as a smudge and not as progress.
 *
 * <p>The two methods are the same drawing for the two modes: the one that knows how much is left
 * and the one that goes from side to side without knowing.
 */
public class MetalProgressBarUI extends BasicProgressBarUI {

    public MetalProgressBarUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalProgressBarUI();
    }

    public void paintDeterminate(Graphics g, JComponent c) {
        super.paintDeterminate(g, c);
        shadow(g, c, amountDone(c));
    }

    public void paintIndeterminate(Graphics g, JComponent c) {
        super.paintIndeterminate(g, c);
        Rectangle box = getBox(null);
        if (box != null) {
            g.setColor(MetalLookAndFeel.getControlDarkShadow());
            g.drawRect(box.x, box.y, box.width - 1, box.height - 1);
        }
    }

    /** The line where the filled part ends. */
    private void shadow(Graphics g, JComponent c, int filled) {
        if (filled <= 0) {
            return;
        }
        JProgressBar b = (JProgressBar) c;
        Insets i = b.getInsets();
        g.setColor(MetalLookAndFeel.getControlDarkShadow());
        if (b.getOrientation() == JProgressBar.HORIZONTAL) {
            int height = b.getHeight() - i.top - i.bottom;
            g.drawRect(i.left, i.top, filled - 1, height - 1);
        } else {
            int width = b.getWidth() - i.left - i.right;
            int height = b.getHeight() - i.top - i.bottom;
            g.drawRect(i.left, i.top + height - filled, width - 1, filled - 1);
        }
    }

    /** The filled pixels, along the bar. */
    private int amountDone(JComponent c) {
        JProgressBar b = (JProgressBar) c;
        Insets i = b.getInsets();
        int length = (b.getOrientation() == JProgressBar.HORIZONTAL)
                ? b.getWidth() - i.left - i.right
                : b.getHeight() - i.top - i.bottom;
        return getAmountFull(i, length, length);
    }
}
