package javax.swing.plaf.basic;

import java.awt.Component$BaselineResizeBehavior;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseMotionListener;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.metal.MetalBorders;

/**
 * The basic look and feel of a button: it places icon and text with
 * {@code layoutCompoundLabel} and paints them according to the model.
 *
 * <h2>A single object for every button</h2>
 *
 * <p>{@link #createUI} always returns the same instance; it can because the look and feel keeps
 * nothing of the button save the text's shift when pressed, which is set and erased inside a
 * single {@link #paint}. The working rectangles are locals for the same reason.
 *
 * <h2>What it installs, and where it comes from</h2>
 *
 * <p>{@link #installDefaults} sets what in the JDK comes from {@code UIManager} under
 * {@code Button.*}, with the values measured in Metal (JDK 25): typeface Dialog bold 12,
 * foreground (51, 51, 51), background (238, 238, 238), margin (2, 14, 2, 14), icon-text gap 4,
 * rollover enabled, text shift 0, and the border from {@link MetalBorders#getButtonBorder}.
 * They are installed as a {@link UIResource}, and only where the button has nothing or a look
 * and feel value: what the user set is respected, which is the JDK's rule.
 *
 * <p>The bold is the API's; the rasterizer draws every typeface with the same regular face
 * ({@code jdk.internal.awt.BitmapFont}).
 *
 * <p>What Metal paints on top -- the background's gradient, the focus frame, the selection
 * background when pressed, the disabled text in flat grey -- belongs to {@code MetalButtonUI},
 * which is not there. This is the basic look and feel as it is: flat background, no focus
 * frame, disabled text in relief.
 */
public class BasicButtonUI extends ButtonUI {

    private static final BasicButtonUI buttonUI = new BasicButtonUI();

    private static final Font DEFAULT_FONT = new FontUIResource("Dialog", Font.BOLD, 12);
    private static final ColorUIResource DEFAULT_FOREGROUND = new ColorUIResource(51, 51, 51);
    private static final ColorUIResource DEFAULT_BACKGROUND = new ColorUIResource(238, 238, 238);

    /** The gap between icon and text that {@link #getDefaultTextIconGap} returns. */
    protected int defaultTextIconGap;

    private int shift = 0;

    /** How much the text shifts when pressed; zero in Metal. */
    protected int defaultTextShiftOffset;

    public BasicButtonUI() {
    }

    /** The shared look and feel. */
    public static ComponentUI createUI(JComponent c) {
        return buttonUI;
    }

    /** The prefix of this component's {@code UIManager} keys. */
    protected String getPropertyPrefix() {
        return "Button.";
    }

    public void installUI(JComponent c) {
        AbstractButton b = (AbstractButton) c;
        installDefaults(b);
        installListeners(b);
        installKeyboardActions(b);
    }

    /** See the class note. */
    protected void installDefaults(AbstractButton b) {
        defaultTextShiftOffset = 0;

        if (b.isContentAreaFilled()) {
            LookAndFeel.installProperty(b, "opaque", Boolean.TRUE);
        } else {
            LookAndFeel.installProperty(b, "opaque", Boolean.FALSE);
        }

        if (b.getMargin() == null || (b.getMargin() instanceof UIResource)) {
            b.setMargin(defaultMargin());
        }
        if (b.getBackground() == null || (b.getBackground() instanceof UIResource)) {
            b.setBackground(DEFAULT_BACKGROUND);
        }
        if (b.getForeground() == null || (b.getForeground() instanceof UIResource)) {
            b.setForeground(DEFAULT_FOREGROUND);
        }
        if (b.getFont() == null || (b.getFont() instanceof UIResource)) {
            b.setFont(DEFAULT_FONT);
        }
        if (b.getBorder() == null || (b.getBorder() instanceof UIResource)) {
            b.setBorder(defaultBorder());
        }
        Boolean rollover = defaultRollover();
        if (rollover != null) {
            LookAndFeel.installProperty(b, "rolloverEnabled", rollover);
        }
        LookAndFeel.installProperty(b, "iconTextGap", Integer.valueOf(4));
    }

    /**
     * What {@code UIManager} would give under {@code prefix + "margin"}: (2, 14, 2, 14) for a
     * button. The subclasses answer for their prefix.
     */
    Insets defaultMargin() {
        return new InsetsUIResource(2, 14, 2, 14);
    }

    /** What {@code UIManager} would give under {@code prefix + "border"}. */
    Border defaultBorder() {
        return MetalBorders.getButtonBorder();
    }

    /**
     * What {@code UIManager} would give under {@code prefix + "rollover"}; {@code null} if there
     * is no value, and then nothing is installed. Metal defines it for all but the button with
     * state.
     */
    Boolean defaultRollover() {
        return Boolean.TRUE;
    }

    protected void installListeners(AbstractButton b) {
        BasicButtonListener listener = createButtonListener(b);
        if (listener != null) {
            b.addMouseListener(listener);
            b.addMouseMotionListener(listener);
            b.addFocusListener(listener);
            b.addPropertyChangeListener(listener);
            b.addChangeListener(listener);
        }
    }

    protected void installKeyboardActions(AbstractButton b) {
        BasicButtonListener listener = listenerOf(b);
        if (listener != null) {
            listener.installKeyboardActions(b);
        }
    }

    public void uninstallUI(JComponent c) {
        uninstallKeyboardActions((AbstractButton) c);
        uninstallListeners((AbstractButton) c);
        uninstallDefaults((AbstractButton) c);
    }

    protected void uninstallKeyboardActions(AbstractButton b) {
        BasicButtonListener listener = listenerOf(b);
        if (listener != null) {
            listener.uninstallKeyboardActions(b);
        }
    }

    protected void uninstallListeners(AbstractButton b) {
        BasicButtonListener listener = listenerOf(b);
        if (listener != null) {
            b.removeMouseListener(listener);
            b.removeMouseMotionListener(listener);
            b.removeFocusListener(listener);
            b.removeChangeListener(listener);
            b.removePropertyChangeListener(listener);
        }
    }

    /**
     * It removes the border if it is the look and feel's; the colours and the typeface stay, as in
     * the JDK.
     */
    protected void uninstallDefaults(AbstractButton b) {
        LookAndFeel.uninstallBorder(b);
    }

    protected BasicButtonListener createButtonListener(AbstractButton b) {
        return new BasicButtonListener(b);
    }

    /**
     * The listener this look and feel installed, looked up among the mouse ones; {@code null} if
     * there is none.
     */
    private BasicButtonListener listenerOf(AbstractButton b) {
        MouseMotionListener[] listeners = b.getMouseMotionListeners();
        if (listeners != null) {
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i] instanceof BasicButtonListener) {
                    return (BasicButtonListener) listeners[i];
                }
            }
        }
        return null;
    }

    public int getDefaultTextIconGap(AbstractButton b) {
        return defaultTextIconGap;
    }

    /**
     * It places icon and text in a button of that size; it returns the text, clipped if it does
     * not fit.
     *
     * <p>With no text, the icon-text gap is zero: an icon-only button centres it without leaving
     * room for a text that is not there.
     */
    private String place(AbstractButton b, FontMetrics fm, int width, int height,
            Rectangle viewRect, Rectangle iconRect, Rectangle textRect) {
        Insets i = b.getInsets();
        viewRect.x = i.left;
        viewRect.y = i.top;
        viewRect.width = width - (i.right + viewRect.x);
        viewRect.height = height - (i.bottom + viewRect.y);
        textRect.x = 0;
        textRect.y = 0;
        textRect.width = 0;
        textRect.height = 0;
        iconRect.x = 0;
        iconRect.y = 0;
        iconRect.width = 0;
        iconRect.height = 0;
        return SwingUtilities.layoutCompoundLabel(b, fm, b.getText(), b.getIcon(),
                b.getVerticalAlignment(), b.getHorizontalAlignment(),
                b.getVerticalTextPosition(), b.getHorizontalTextPosition(), viewRect, iconRect,
                textRect, b.getText() == null ? 0 : b.getIconTextGap());
    }

    /**
     * It paints the button: what is pressed, the icon, the text and the focus, in that order.
     *
     * <p>The background is not painted here but in {@link #update}, if the button is opaque; and
     * the border is painted by the button itself afterwards, if {@code isBorderPainted}.
     */
    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel model = b.getModel();
        FontMetrics fm = b.getFontMetrics(b.getFont());
        Rectangle viewRect = new Rectangle();
        Rectangle iconRect = new Rectangle();
        Rectangle textRect = new Rectangle();
        String text = place(b, fm, b.getWidth(), b.getHeight(), viewRect, iconRect, textRect);

        clearTextShiftOffset();

        if (model.isArmed() && model.isPressed()) {
            paintButtonPressed(g, b);
        }
        if (b.getIcon() != null) {
            paintIcon(g, c, iconRect);
        }
        if (text != null && !text.isEmpty()) {
            paintText(g, b, textRect, text);
        }
        if (b.isFocusPainted() && b.hasFocus()) {
            paintFocus(g, b, viewRect, textRect, iconRect);
        }
    }

    /**
     * It paints the icon that corresponds to the state.
     *
     * <p>The choice goes from more to less specific: disabled (and selected), pressed, rollover
     * (and selected), selected, and the ordinary icon if the state's is not there. A pressed icon
     * that exists cancels the text's shift: it says "pressed" all by itself.
     */
    protected void paintIcon(Graphics g, JComponent c, Rectangle iconRect) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel model = b.getModel();
        Icon icon = b.getIcon();
        Icon ofState = null;
        if (icon == null) {
            return;
        }
        if (model.isSelected()) {
            Icon selected = b.getSelectedIcon();
            if (selected != null) {
                icon = selected;
            }
        }
        if (!model.isEnabled()) {
            if (model.isSelected()) {
                ofState = b.getDisabledSelectedIcon();
                if (ofState == null) {
                    ofState = b.getSelectedIcon();
                }
            }
            if (ofState == null) {
                ofState = b.getDisabledIcon();
            }
        } else if (model.isPressed() && model.isArmed()) {
            ofState = b.getPressedIcon();
            if (ofState != null) {
                clearTextShiftOffset();
            }
        } else if (b.isRolloverEnabled() && model.isRollover()) {
            if (model.isSelected()) {
                ofState = b.getRolloverSelectedIcon();
                if (ofState == null) {
                    ofState = b.getSelectedIcon();
                }
            }
            if (ofState == null) {
                ofState = b.getRolloverIcon();
            }
        }
        if (ofState != null) {
            icon = ofState;
        }
        if (model.isPressed() && model.isArmed()) {
            icon.paintIcon(c, g, iconRect.x + getTextShiftOffset(),
                    iconRect.y + getTextShiftOffset());
        } else {
            icon.paintIcon(c, g, iconRect.x, iconRect.y);
        }
    }

    protected void paintText(Graphics g, JComponent c, Rectangle textRect, String text) {
        paintText(g, (AbstractButton) c, textRect, text);
    }

    /**
     * It paints the text: in the foreground colour if it is enabled, in relief if not.
     *
     * <p>The relief is the basic look and feel's: the background lightened in its place and the
     * background darkened one pixel up and to the left. Metal replaces it with a flat grey; see
     * the class note.
     */
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        ButtonModel model = b.getModel();
        FontMetrics fm = b.getFontMetrics(b.getFont());
        int index = b.getDisplayedMnemonicIndex();
        if (model.isEnabled()) {
            g.setColor(b.getForeground());
            BasicGraphicsUtils.drawStringUnderlineCharAt(g, text, index,
                    textRect.x + getTextShiftOffset(),
                    textRect.y + fm.getAscent() + getTextShiftOffset());
        } else {
            g.setColor(b.getBackground().brighter());
            BasicGraphicsUtils.drawStringUnderlineCharAt(g, text, index, textRect.x,
                    textRect.y + fm.getAscent());
            g.setColor(b.getBackground().darker());
            BasicGraphicsUtils.drawStringUnderlineCharAt(g, text, index, textRect.x - 1,
                    textRect.y + fm.getAscent() - 1);
        }
    }

    /** Nothing: the basic look and feel does not mark the focus; those that derive from it do. */
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect,
            Rectangle textRect, Rectangle iconRect) {
    }

    /** Nothing: the basic look and feel shows what is pressed with the border alone. */
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
    }

    protected void clearTextShiftOffset() {
        shift = 0;
    }

    protected void setTextShiftOffset() {
        shift = defaultTextShiftOffset;
    }

    protected int getTextShiftOffset() {
        return shift;
    }

    /** The minimum is the preferred one: a button does not shrink without clipping the text. */
    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    /** What icon and text take up in an infinite view, plus the insets. */
    public Dimension getPreferredSize(JComponent c) {
        AbstractButton b = (AbstractButton) c;
        return BasicGraphicsUtils.getPreferredButtonSize(b, b.getIconTextGap());
    }

    /** The maximum is the preferred one: a button does not grow by itself. */
    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }

    /** The baseline of the text, placed in that box; {@code -1} with no text. */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        AbstractButton b = (AbstractButton) c;
        String text = b.getText();
        if (text == null || text.isEmpty()) {
            return -1;
        }
        FontMetrics fm = b.getFontMetrics(b.getFont());
        Rectangle viewRect = new Rectangle();
        Rectangle iconRect = new Rectangle();
        Rectangle textRect = new Rectangle();
        place(b, fm, width, height, viewRect, iconRect, textRect);
        return textRect.y + fm.getAscent();
    }

    /** How the baseline moves: according to where the text is aligned vertically. */
    public Component$BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        int v = ((AbstractButton) c).getVerticalAlignment();
        if (v == AbstractButton.TOP) {
            return Component$BaselineResizeBehavior.CONSTANT_ASCENT;
        }
        if (v == AbstractButton.BOTTOM) {
            return Component$BaselineResizeBehavior.CONSTANT_DESCENT;
        }
        if (v == AbstractButton.CENTER) {
            return Component$BaselineResizeBehavior.CENTER_OFFSET;
        }
        return Component$BaselineResizeBehavior.OTHER;
    }
}
