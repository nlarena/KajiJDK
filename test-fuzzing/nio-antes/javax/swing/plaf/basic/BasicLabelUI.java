package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component$BaselineResizeBehavior;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.LabelUI;

/**
 * The basic look and feel of a label: it places text and icon and paints them.
 *
 * <h2>A single object for every label</h2>
 *
 * <p>{@link #createUI} always returns the same instance, and it can because this look and feel
 * <strong>keeps nothing of the component</strong>: each method receives the label and works on
 * it. It is the reason the working rectangles are locals and not fields -- a field shared
 * between a thousand labels would be a datum of the last one that was painted.
 *
 * <h2>What it installs, and where the values come from</h2>
 *
 * <p>{@link #installDefaults} sets the typeface and the colours that in the JDK come from
 * {@code UIManager} under {@code Label.font}, {@code Label.foreground} and
 * {@code Label.background}. With no {@code UIManager}, they are the values <em>measured</em> in
 * the JDK 25 Metal look and feel: Dialog bold 12, grey (51, 51, 51) and (238, 238, 238). They
 * are installed only where the component has nothing: the JDK tells "the user set it" from
 * "a look and feel set it" with the {@code UIResource} classes, which are not there, and the
 * honest approximation is not to overwrite what was already there.
 *
 * <p>The bold is the API's: this VM's rasterizer draws every typeface with the same regular
 * face, so the text comes out regular even though {@code getFont} says bold. It is the
 * substitution of {@code jdk.internal.awt.BitmapFont}, said at every place where it shows.
 */
public class BasicLabelUI extends LabelUI implements PropertyChangeListener {

    /** The shared instance; see the class note. */
    protected static BasicLabelUI labelUI = new BasicLabelUI();

    private static final Font DEFAULT_FONT = new Font("Dialog", Font.BOLD, 12);
    private static final Color DEFAULT_FOREGROUND = new Color(51, 51, 51);
    private static final Color DEFAULT_BACKGROUND = new Color(238, 238, 238);

    /** A new look and feel. The usual thing is to ask for {@link #createUI}'s. */
    public BasicLabelUI() {
    }

    /** The shared look and feel. */
    public static ComponentUI createUI(JComponent c) {
        return labelUI;
    }

    /**
     * It places text and icon; it returns the text, possibly clipped.
     *
     * <p>Delegating to {@link SwingUtilities#layoutCompoundLabel} is what makes a label and a
     * button place their text the same way: it is a single algorithm with two callers.
     */
    protected String layoutCL(JLabel label, FontMetrics fontMetrics, String text, Icon icon,
            Rectangle viewR, Rectangle iconR, Rectangle textR) {
        return SwingUtilities.layoutCompoundLabel(label, fontMetrics, text, icon,
                label.getVerticalAlignment(), label.getHorizontalAlignment(),
                label.getVerticalTextPosition(), label.getHorizontalTextPosition(),
                viewR, iconR, textR, label.getIconTextGap());
    }

    /** It paints an enabled label's text, with its mnemonic underlined. */
    protected void paintEnabledText(JLabel l, Graphics g, String s, int textX, int textY) {
        int index = l.getDisplayedMnemonicIndex();
        g.setColor(l.getForeground());
        BasicGraphicsUtils.drawStringUnderlineCharAt(g, s, index, textX, textY);
    }

    /**
     * It paints a disabled label's text, in relief.
     *
     * <p>Two passes: the background lightened one pixel down and to the right, and the background
     * darkened in its place. The text ends up as though etched into the background, which is how
     * the basic look and feel says "this does not answer".
     */
    protected void paintDisabledText(JLabel l, Graphics g, String s, int textX, int textY) {
        int index = l.getDisplayedMnemonicIndex();
        Color background = l.getBackground();
        g.setColor(background.brighter());
        BasicGraphicsUtils.drawStringUnderlineCharAt(g, s, index, textX + 1, textY + 1);
        g.setColor(background.darker());
        BasicGraphicsUtils.drawStringUnderlineCharAt(g, s, index, textX, textY);
    }

    /**
     * It paints icon and text.
     *
     * <p>The background is not painted here: it is painted by {@link #update} if the label is
     * opaque, which by default it is not. Hence a label over a panel of another colour looks
     * transparent.
     */
    public void paint(Graphics g, JComponent c) {
        JLabel label = (JLabel) c;
        String text = label.getText();
        Icon icon = label.isEnabled() ? label.getIcon() : label.getDisabledIcon();
        if (icon == null && text == null) {
            return;
        }
        FontMetrics fm = label.getFontMetrics(label.getFont());
        Insets insets = c.getInsets(new Insets(0, 0, 0, 0));
        Rectangle viewRect = new Rectangle(insets.left, insets.top,
                c.getWidth() - (insets.left + insets.right),
                c.getHeight() - (insets.top + insets.bottom));
        Rectangle iconRect = new Rectangle();
        Rectangle textRect = new Rectangle();
        String clipped = layoutCL(label, fm, text, icon, viewRect, iconRect, textRect);
        if (icon != null) {
            icon.paintIcon(c, g, iconRect.x, iconRect.y);
        }
        if (text != null) {
            int textX = textRect.x;
            int textY = textRect.y + fm.getAscent();
            if (label.isEnabled()) {
                paintEnabledText(label, g, clipped, textX, textY);
            } else {
                paintDisabledText(label, g, clipped, textX, textY);
            }
        }
    }

    /**
     * The preferred size: what text and icon take up placed in an infinite view, plus the
     * insets.
     *
     * <p>The infinite view is the trick: with no width limit nothing is clipped, and the box that
     * is left is the label's natural size.
     */
    public Dimension getPreferredSize(JComponent c) {
        JLabel label = (JLabel) c;
        String text = label.getText();
        Icon icon = label.isEnabled() ? label.getIcon() : label.getDisabledIcon();
        Insets insets = label.getInsets(new Insets(0, 0, 0, 0));
        Font font = label.getFont();
        int dx = insets.left + insets.right;
        int dy = insets.top + insets.bottom;

        if (icon == null && (text == null || font == null)) {
            return new Dimension(dx, dy);
        }
        if (text == null || (icon != null && font == null)) {
            return new Dimension(icon.getIconWidth() + dx, icon.getIconHeight() + dy);
        }
        FontMetrics fm = label.getFontMetrics(font);
        Rectangle iconRect = new Rectangle();
        Rectangle textRect = new Rectangle();
        Rectangle viewRect = new Rectangle(dx, dy, Short.MAX_VALUE, Short.MAX_VALUE);
        layoutCL(label, fm, text, icon, viewRect, iconRect, textRect);
        int x1 = Math.min(iconRect.x, textRect.x);
        int x2 = Math.max(iconRect.x + iconRect.width, textRect.x + textRect.width);
        int y1 = Math.min(iconRect.y, textRect.y);
        int y2 = Math.max(iconRect.y + iconRect.height, textRect.y + textRect.height);
        Dimension rv = new Dimension(x2 - x1, y2 - y1);
        rv.width = rv.width + dx;
        rv.height = rv.height + dy;
        return rv;
    }

    /** The minimum is the preferred one: a label does not shrink without clipping. */
    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    /** The maximum is the preferred one: a label does not grow by itself. */
    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }

    /** The baseline of the text, placed in that box; {@code -1} with no text. */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        JLabel label = (JLabel) c;
        String text = label.getText();
        if (text == null || text.isEmpty() || label.getFont() == null) {
            return -1;
        }
        FontMetrics fm = label.getFontMetrics(label.getFont());
        Insets insets = label.getInsets(new Insets(0, 0, 0, 0));
        Rectangle viewRect = new Rectangle(insets.left, insets.top,
                width - (insets.left + insets.right), height - (insets.top + insets.bottom));
        Rectangle iconRect = new Rectangle();
        Rectangle textRect = new Rectangle();
        layoutCL(label, fm, text, label.isEnabled() ? label.getIcon() : label.getDisabledIcon(),
                viewRect, iconRect, textRect);
        return textRect.y + fm.getAscent();
    }

    /** How the baseline moves: according to where the text is aligned vertically. */
    public Component$BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        JLabel label = (JLabel) c;
        int v = label.getVerticalAlignment();
        if (v == JLabel.TOP) {
            return Component$BaselineResizeBehavior.CONSTANT_ASCENT;
        }
        if (v == JLabel.BOTTOM) {
            return Component$BaselineResizeBehavior.CONSTANT_DESCENT;
        }
        if (v == JLabel.CENTER) {
            return Component$BaselineResizeBehavior.CENTER_OFFSET;
        }
        return Component$BaselineResizeBehavior.OTHER;
    }

    public void installUI(JComponent c) {
        JLabel label = (JLabel) c;
        installDefaults(label);
        installComponents(label);
        installListeners(label);
        installKeyboardActions(label);
    }

    public void uninstallUI(JComponent c) {
        JLabel label = (JLabel) c;
        uninstallDefaults(label);
        uninstallComponents(label);
        uninstallListeners(label);
        uninstallKeyboardActions(label);
    }

    /** Default typeface and colours, only where there is nothing; see the class note. */
    protected void installDefaults(JLabel c) {
        if (c.getFont() == null) {
            c.setFont(DEFAULT_FONT);
        }
        if (c.getForeground() == null) {
            c.setForeground(DEFAULT_FOREGROUND);
        }
        if (c.getBackground() == null) {
            c.setBackground(DEFAULT_BACKGROUND);
        }
    }

    /** This look and feel listens to the label's property changes. */
    protected void installListeners(JLabel c) {
        c.addPropertyChangeListener(this);
    }

    /** A label has no subcomponents; nothing to install. */
    protected void installComponents(JLabel c) {
    }

    /**
     * Nothing: a label's keyboard actions -- giving the focus to its {@code labelFor} with the
     * mnemonic -- need {@code InputMap} and {@code ActionMap}, which are not there.
     */
    protected void installKeyboardActions(JLabel l) {
    }

    /**
     * What was installed stayed in the component and may go on being there; the JDK does not erase
     * it either.
     */
    protected void uninstallDefaults(JLabel c) {
    }

    protected void uninstallListeners(JLabel c) {
        c.removePropertyChangeListener(this);
    }

    protected void uninstallComponents(JLabel c) {
    }

    protected void uninstallKeyboardActions(JLabel c) {
    }

    /**
     * A property of the label changed.
     *
     * <p>The JDK uses this in order to renew the keyboard actions when the text, the mnemonic or
     * the {@code labelFor} change. With no keyboard actions there is nothing to renew: the repaint
     * and the relayout are already asked for by the label itself on changing.
     */
    public void propertyChange(PropertyChangeEvent e) {
    }
}
