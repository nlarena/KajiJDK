package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonListener;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Metal's button.
 *
 * <h2>Three colours the basic one does not have</h2>
 *
 * <p>{@link #focusColor} is the dotted rectangle inside, {@link #selectColor} the fill while the
 * button is pressed and {@link #disabledTextColor} the text of a disabled one. The basic one
 * does not keep them: it draws the focus with the foreground colour and the pressed state by
 * darkening the background. Metal takes them from the theme, and that is why a change of theme
 * changes all three at once.
 *
 * <p>The three fields are read through their {@code getXxx}, and those methods exist for
 * something concrete: {@link MetalToggleButtonUI} has the same three fields and does
 * <em>not</em> inherit from this class -- it inherits from the basic toggle -- so the only way
 * of sharing the drawing is for each to answer with its own.
 *
 * <h2>It shares its instance</h2>
 *
 * <p>{@link #createUI} always returns the same one. The three colours belong to the theme and
 * not to the button, so there is nothing per button to keep.
 */
public class MetalButtonUI extends BasicButtonUI {

    private static final MetalButtonUI SHARED = new MetalButtonUI();

    protected Color focusColor;
    protected Color selectColor;
    protected Color disabledTextColor;

    public MetalButtonUI() {
    }

    public static ComponentUI createUI(JComponent c) {
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

    public void uninstallDefaults(AbstractButton b) {
        super.uninstallDefaults(b);
        // The three colours are not released: this UI is shared by every button in the
                // program, and releasing them when one is uninstalled would leave the rest with no
                // colour. Measured.
    }

    protected BasicButtonListener createButtonListener(AbstractButton b) {
        return super.createButtonListener(b);
    }

    /** The pressed button's fill, in the theme's selection colour. */
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        if (b.isContentAreaFilled()) {
            Dimension s = b.getSize();
            g.setColor(getSelectColor());
            g.fillRect(0, 0, s.width, s.height);
        }
    }

    /** A dotted rectangle inside the border. */
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect,
            Rectangle textRect, Rectangle iconRect) {
        Rectangle f = b.getVisibleRect();
        g.setColor(getFocusColor());
        g.drawRect(f.x + 1, f.y + 1, f.width - 3, f.height - 3);
    }

    /** Disabled text goes in the theme's grey, not in a darkened background. */
    protected void paintText(Graphics g, JComponent c, Rectangle textRect, String text) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel m = b.getModel();
        if (m.isEnabled()) {
            super.paintText(g, c, textRect, text);
            return;
        }
        Color before = g.getColor();
        g.setColor(getDisabledTextColor());
        super.paintText(g, c, textRect, text);
        g.setColor(before);
    }

    public void update(Graphics g, JComponent c) {
        super.update(g, c);
    }
}
