package javax.swing.plaf.metal;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicLabelUI;

/**
 * Metal's label.
 *
 * <p>It changes a single thing from the basic one, and that is how a disabled label looks. The
 * basic one draws it twice shifted by a pixel -- white and then grey -- which is Windows 95's
 * engraving. Metal draws it once in the theme's grey. It is flatter and it is what suits a look
 * and feel that imitates nobody.
 *
 * <p>It shares its instance: {@link #createUI} always returns the same one. It can do so because
 * it keeps nothing of the label it draws, and a single instance for a program's thousand labels
 * is the reason {@link #metalLabelUI} exists.
 */
public class MetalLabelUI extends BasicLabelUI {

    /** The only one; see the class note. */
    protected static MetalLabelUI metalLabelUI = new MetalLabelUI();

    public MetalLabelUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return metalLabelUI;
    }

    /** A single pass in the theme's grey; see the class note. */
    protected void paintDisabledText(JLabel l, Graphics g, String s, int textX, int textY) {
        int index = l.getDisplayedMnemonicIndex();
        g.setColor(MetalLookAndFeel.getInactiveSystemTextColor());
        BasicGraphicsUtils.drawStringUnderlineCharAt(g, s, index, textX, textY);
    }
}
