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
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.metal.MetalIconFactory;

/**
 * The basic look and feel of a radio button: a default icon that paints the state, and the
 * text beside it.
 *
 * <p>The icon is what is new with respect to {@link BasicToggleButtonUI}: if the button does not
 * have one of its own, {@link #getDefaultIcon} is used, which in Metal is the circle of
 * {@link MetalIconFactory#getRadioButtonIcon}, and that icon reads the model in order to know
 * how to paint itself. An icon of the button's own is chosen by state as in any button with
 * state.
 *
 * <p>The default values are those of {@code RadioButton.*} in Metal: margin (2, 2, 2, 2), the
 * border from {@link BasicBorders#getRadioButtonBorder} (which is not painted: the button is
 * born with {@code borderPainted} at {@code false}, but its insets count), and rollover.
 */
public class BasicRadioButtonUI extends BasicToggleButtonUI {

    private static final BasicRadioButtonUI radioButtonUI = new BasicRadioButtonUI();

    /** The default icon; see the class note. */
    protected Icon icon;

    private boolean defaults_initialized = false;

    private static final String propertyPrefix = "RadioButton.";

    public BasicRadioButtonUI() {
    }

    /** The shared look and feel. */
    public static ComponentUI createUI(JComponent b) {
        return radioButtonUI;
    }

    protected String getPropertyPrefix() {
        return propertyPrefix;
    }

    Insets defaultMargin() {
        return new InsetsUIResource(2, 2, 2, 2);
    }

    Border defaultBorder() {
        return BasicBorders.getRadioButtonBorder();
    }

    Boolean defaultRollover() {
        return Boolean.TRUE;
    }

    /** What {@code UIManager} would give under {@code prefix + "icon"}. */
    Icon defaultIcon() {
        return MetalIconFactory.getRadioButtonIcon();
    }

    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        if (!defaults_initialized) {
            icon = defaultIcon();
            defaults_initialized = true;
        }
    }

    protected void uninstallDefaults(AbstractButton b) {
        super.uninstallDefaults(b);
        defaults_initialized = false;
    }

    public Icon getDefaultIcon() {
        return icon;
    }

    public synchronized void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel model = b.getModel();

        Font f = c.getFont();
        g.setFont(f);
        FontMetrics fm = b.getFontMetrics(f);

        Insets i = c.getInsets();
        Dimension size = b.getSize();
        Rectangle viewRect = new Rectangle(i.left, i.top, size.width - (i.right + i.left),
                size.height - (i.bottom + i.top));
        Rectangle iconRect = new Rectangle();
        Rectangle textRect = new Rectangle();

        Icon own = b.getIcon();
        String text = SwingUtilities.layoutCompoundLabel(c, fm, b.getText(),
                own != null ? own : getDefaultIcon(), b.getVerticalAlignment(),
                b.getHorizontalAlignment(), b.getVerticalTextPosition(),
                b.getHorizontalTextPosition(), viewRect, iconRect, textRect,
                b.getText() == null ? 0 : b.getIconTextGap());

        if (c.isOpaque()) {
            g.setColor(b.getBackground());
            g.fillRect(0, 0, size.width, size.height);
        }

        if (own != null) {
            Icon ofState = own;
            if (!model.isEnabled()) {
                if (model.isSelected()) {
                    ofState = b.getDisabledSelectedIcon();
                } else {
                    ofState = b.getDisabledIcon();
                }
            } else if (model.isPressed() && model.isArmed()) {
                ofState = b.getPressedIcon();
                if (ofState == null) {
                    ofState = b.getSelectedIcon();
                }
            } else if (model.isSelected()) {
                if (b.isRolloverEnabled() && model.isRollover()) {
                    ofState = b.getRolloverSelectedIcon();
                    if (ofState == null) {
                        ofState = b.getSelectedIcon();
                    }
                } else {
                    ofState = b.getSelectedIcon();
                }
            } else if (b.isRolloverEnabled() && model.isRollover()) {
                ofState = b.getRolloverIcon();
            }
            if (ofState == null) {
                ofState = b.getIcon();
            }
            ofState.paintIcon(c, g, iconRect.x, iconRect.y);
        } else {
            getDefaultIcon().paintIcon(c, g, iconRect.x, iconRect.y);
        }

        if (text != null) {
            paintText(g, b, textRect, text);
            if (b.hasFocus() && b.isFocusPainted() && textRect.width > 0 && textRect.height > 0) {
                paintFocus(g, textRect, size);
            }
        }
    }

    /** Nothing: the basic look and feel does not mark the focus; those that derive from it do. */
    protected void paintFocus(Graphics g, Rectangle textRect, Dimension size) {
    }

    /**
     * Icon and text in an infinite view, plus the insets; {@code null} if the button has children.
     */
    public Dimension getPreferredSize(JComponent c) {
        if (c.getComponentCount() > 0) {
            return null;
        }
        AbstractButton b = (AbstractButton) c;
        String text = b.getText();
        Icon icon = b.getIcon();
        if (icon == null) {
            icon = getDefaultIcon();
        }
        Font font = b.getFont();
        FontMetrics fm = b.getFontMetrics(font);

        Rectangle viewRect = new Rectangle(0, 0, Short.MAX_VALUE, Short.MAX_VALUE);
        Rectangle iconRect = new Rectangle();
        Rectangle textRect = new Rectangle();
        SwingUtilities.layoutCompoundLabel(c, fm, text, icon, b.getVerticalAlignment(),
                b.getHorizontalAlignment(), b.getVerticalTextPosition(),
                b.getHorizontalTextPosition(), viewRect, iconRect, textRect,
                text == null ? 0 : b.getIconTextGap());

        int x1 = Math.min(iconRect.x, textRect.x);
        int x2 = Math.max(iconRect.x + iconRect.width, textRect.x + textRect.width);
        int y1 = Math.min(iconRect.y, textRect.y);
        int y2 = Math.max(iconRect.y + iconRect.height, textRect.y + textRect.height);
        int width = x2 - x1;
        int height = y2 - y1;

        Insets insets = b.getInsets();
        width = width + insets.left + insets.right;
        height = height + insets.top + insets.bottom;
        return new Dimension(width, height);
    }
}
