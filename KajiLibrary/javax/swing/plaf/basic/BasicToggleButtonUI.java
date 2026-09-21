package javax.swing.plaf.basic;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalBorders;

/**
 * The basic look and feel of a button with state.
 *
 * <p>It differs from {@link BasicButtonUI} in three things: "pressed" includes "selected" when
 * choosing whether to paint the pressed background, the selected icon is chosen before the
 * rollover one, and the text never shifts. The default values are those of {@code ToggleButton.*}
 * in Metal: margin (2, 14, 2, 14), the border from
 * {@link MetalBorders#getToggleButtonBorder}, and no rollover.
 */
public class BasicToggleButtonUI extends BasicButtonUI {

    private static final BasicToggleButtonUI toggleButtonUI = new BasicToggleButtonUI();

    private static final String propertyPrefix = "ToggleButton.";

    public BasicToggleButtonUI() {
    }

    /** The shared look and feel. */
    public static ComponentUI createUI(JComponent b) {
        return toggleButtonUI;
    }

    protected String getPropertyPrefix() {
        return propertyPrefix;
    }

    Border defaultBorder() {
        return MetalBorders.getToggleButtonBorder();
    }

    /** {@code ToggleButton.rollover} is not defined in Metal: nothing is installed. */
    Boolean defaultRollover() {
        return null;
    }

    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel model = b.getModel();
        Dimension size = b.getSize();
        Insets i = c.getInsets();
        Rectangle viewRect = new Rectangle(size);
        viewRect.x = viewRect.x + i.left;
        viewRect.y = viewRect.y + i.top;
        viewRect.width = viewRect.width - (i.right + viewRect.x);
        viewRect.height = viewRect.height - (i.bottom + viewRect.y);
        Rectangle iconRect = new Rectangle();
        Rectangle textRect = new Rectangle();

        Font f = c.getFont();
        g.setFont(f);
        FontMetrics fm = b.getFontMetrics(f);

        String text = SwingUtilities.layoutCompoundLabel(c, fm, b.getText(), b.getIcon(),
                b.getVerticalAlignment(), b.getHorizontalAlignment(),
                b.getVerticalTextPosition(), b.getHorizontalTextPosition(), viewRect, iconRect,
                textRect, b.getText() == null ? 0 : b.getIconTextGap());

        g.setColor(b.getBackground());

        if ((model.isArmed() && model.isPressed()) || model.isSelected()) {
            paintButtonPressed(g, b);
        }
        if (b.getIcon() != null) {
            paintIcon(g, b, iconRect);
        }
        if (text != null && !text.isEmpty()) {
            paintText(g, b, textRect, text);
        }
        if (b.isFocusPainted() && b.hasFocus()) {
            paintFocus(g, b, viewRect, textRect, iconRect);
        }
    }

    /**
     * It paints the state's icon: disabled, pressed, selected (with or without rollover),
     * rollover, and the ordinary one if the state's is not there.
     */
    protected void paintIcon(Graphics g, AbstractButton b, Rectangle iconRect) {
        ButtonModel model = b.getModel();
        Icon icon = null;
        if (!model.isEnabled()) {
            if (model.isSelected()) {
                icon = b.getDisabledSelectedIcon();
            } else {
                icon = b.getDisabledIcon();
            }
        } else if (model.isPressed() && model.isArmed()) {
            icon = b.getPressedIcon();
            if (icon == null) {
                icon = b.getSelectedIcon();
            }
        } else if (model.isSelected()) {
            if (b.isRolloverEnabled() && model.isRollover()) {
                icon = b.getRolloverSelectedIcon();
                if (icon == null) {
                    icon = b.getSelectedIcon();
                }
            } else {
                icon = b.getSelectedIcon();
            }
        } else if (b.isRolloverEnabled() && model.isRollover()) {
            icon = b.getRolloverIcon();
        }
        if (icon == null) {
            icon = b.getIcon();
        }
        icon.paintIcon(b, g, iconRect.x, iconRect.y);
    }

    /** Zero: a button with state does not shift the text when pressed. */
    protected int getTextShiftOffset() {
        return 0;
    }
}
