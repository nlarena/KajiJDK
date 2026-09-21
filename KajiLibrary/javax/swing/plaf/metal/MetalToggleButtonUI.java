package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;

/**
 * Metal's toggle button.
 *
 * <p>It is {@link MetalButtonUI}'s twin and shares not a line of code with it, because the two
 * inherit from different places: one from the basic button and this one from the basic toggle.
 * Java has no multiple inheritance and this is the class where it shows most: the three colour
 * fields, the three {@code getXxx}, {@code installDefaults} and {@code paintText} are written
 * twice, identically.
 *
 * <p>The only thing it really adds is {@link #paintIcon}: a toggle with an icon and chosen draws
 * the icon over the selection colour, something an ordinary button does not need because it does
 * not stay chosen.
 */
public class MetalToggleButtonUI extends BasicToggleButtonUI {

    private static final MetalToggleButtonUI SHARED = new MetalToggleButtonUI();

    protected Color focusColor;
    protected Color selectColor;
    protected Color disabledTextColor;

    public MetalToggleButtonUI() {
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

    protected void uninstallDefaults(AbstractButton b) {
        super.uninstallDefaults(b);
        // The three colours are not released: this UI is shared by every button in the
                // program, and releasing them when one is uninstalled would leave the rest with no
                // colour. Measured.
    }

    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        if (b.isContentAreaFilled()) {
            Dimension s = b.getSize();
            g.setColor(getSelectColor());
            g.fillRect(0, 0, s.width, s.height);
        }
    }

    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect,
            Rectangle textRect, Rectangle iconRect) {
        Rectangle f = b.getVisibleRect();
        g.setColor(getFocusColor());
        g.drawRect(f.x + 1, f.y + 1, f.width - 3, f.height - 3);
    }

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

    /** A chosen toggle carries the icon over the selection colour. */
    protected void paintIcon(Graphics g, AbstractButton b, Rectangle iconRect) {
        ButtonModel m = b.getModel();
        if (m.isSelected() && !m.isArmed() && b.isContentAreaFilled() && b.isOpaque()) {
            g.setColor(getSelectColor());
            g.fillRect(iconRect.x, iconRect.y, iconRect.width, iconRect.height);
        }
        super.paintIcon(g, b, iconRect);
    }

    public void update(Graphics g, JComponent c) {
        super.update(g, c);
    }
}
