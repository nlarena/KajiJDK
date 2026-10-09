package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicRadioButtonUI;

/**
 * Metal's radio button.
 *
 * <p>The same three colours as the button -- focus, selection and disabled text -- taken from
 * the theme. The only different thing is {@link #paintFocus}, which draws the dotted rectangle
 * around the <em>text</em> and not around the whole component: the little circle on the left
 * does not go inside the focus frame, because marking the circle as well as the text reads as
 * two focused things.
 *
 * <p>{@link MetalCheckBoxUI} inherits from this class, and not the other way round, even though
 * a check box is simpler than a radio button. It is because the drawing -- icon on the left,
 * text beside it, focus around the text -- is the same, and the only thing that changes is which
 * prefix the values come from.
 */
public class MetalRadioButtonUI extends BasicRadioButtonUI {

    private static final MetalRadioButtonUI SHARED = new MetalRadioButtonUI();

    protected Color focusColor;
    protected Color selectColor;
    protected Color disabledTextColor;

    public MetalRadioButtonUI() {
    }

    public static ComponentUI createUI(JComponent b) {
        return SHARED;
    }

    protected Color getFocusColor() {
        if (focusColor == null) {
            focusColor = MetalLookAndFeel.tableColor(getPropertyPrefix() + "focus");
        }
        return focusColor;
    }

    protected Color getSelectColor() {
        if (selectColor == null) {
            selectColor = MetalLookAndFeel.tableColor(getPropertyPrefix() + "select");
        }
        return selectColor;
    }

    protected Color getDisabledTextColor() {
        if (disabledTextColor == null) {
            disabledTextColor =
                    MetalLookAndFeel.tableColor(getPropertyPrefix() + "disabledText");
        }
        return disabledTextColor;
    }

    public void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        focusColor = MetalLookAndFeel.tableColor(getPropertyPrefix() + "focus");
        selectColor = MetalLookAndFeel.tableColor(getPropertyPrefix() + "select");
        disabledTextColor =
                MetalLookAndFeel.tableColor(getPropertyPrefix() + "disabledText");
    }

    protected void uninstallDefaults(AbstractButton b) {
        super.uninstallDefaults(b);
        // The three colours are not released: this UI is shared by every button in the
                // program, and releasing them when one is uninstalled would leave the rest with no
                // colour. Measured.
    }

    public synchronized void paint(Graphics g, JComponent c) {
        super.paint(g, c);
    }

    /** Around the text, not around the component; see the class note. */
    protected void paintFocus(Graphics g, Rectangle t, Dimension d) {
        g.setColor(getFocusColor());
        g.drawRect(t.x - 1, t.y - 1, t.width + 1, t.height + 1);
    }
}
