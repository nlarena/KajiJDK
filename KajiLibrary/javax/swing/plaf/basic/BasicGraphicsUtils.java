package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.InputEvent;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * The strokes the basic looks and feels share: bevels, grooves, text with a mnemonic.
 *
 * <p>Static and stateless on purpose: they are pure drawing routines, and putting them in one
 * place is what makes a button, a panel with a border and a bar look with the same relief.
 *
 * <p>The four relief routines draw the same illusion as {@code BevelBorder}: the light comes
 * from the top left, so that side goes light and the opposite one dark. Changing which colour
 * goes on each side is the whole difference between raised and sunken.
 */
public class BasicGraphicsUtils {

    private static final Insets ETCHED_INSETS = new Insets(2, 2, 2, 2);
    private static final Insets GROOVE_INSETS = new Insets(2, 2, 2, 2);

    /** The JDK leaves it instantiable, even though there is nothing to instantiate. */
    public BasicGraphicsUtils() {
    }

    /** A two-pixel etched rectangle: shadow outside top-left, highlight bottom-right. */
    public static void drawEtchedRect(Graphics g, int x, int y, int w, int h, Color shadow,
            Color darkShadow, Color highlight, Color lightHighlight) {
        Color old = g.getColor();
        g.translate(x, y);

        g.setColor(shadow);
        g.drawLine(0, 0, w - 1, 0);
        g.drawLine(0, 1, 0, h - 2);

        g.setColor(darkShadow);
        g.drawLine(1, 1, w - 3, 1);
        g.drawLine(1, 2, 1, h - 3);

        g.setColor(lightHighlight);
        g.drawLine(w - 1, 0, w - 1, h - 1);
        g.drawLine(0, h - 1, w - 1, h - 1);

        g.setColor(highlight);
        g.drawLine(w - 2, 1, w - 2, h - 3);
        g.drawLine(1, h - 2, w - 2, h - 2);

        g.translate(-x, -y);
        g.setColor(old);
    }

    /** How much an etched rectangle takes up: two pixels on each side. */
    public static Insets getEtchedInsets() {
        return ETCHED_INSETS;
    }

    /** A groove: a shadow line and a highlight one, shifted by a pixel. */
    public static void drawGroove(Graphics g, int x, int y, int w, int h, Color shadow,
            Color highlight) {
        Color old = g.getColor();
        g.translate(x, y);

        g.setColor(shadow);
        g.drawRect(0, 0, w - 2, h - 2);

        g.setColor(highlight);
        g.drawLine(1, h - 3, 1, 1);
        g.drawLine(1, 1, w - 3, 1);
        g.drawLine(0, h - 1, w - 1, h - 1);
        g.drawLine(w - 1, h - 1, w - 1, 0);

        g.translate(-x, -y);
        g.setColor(old);
    }

    /** How much a groove takes up: two pixels on each side. */
    public static Insets getGrooveInsets() {
        return GROOVE_INSETS;
    }

    /**
     * A button's bevel, in its four states.
     *
     * <p>{@code isPressed} swaps the colours -- the button sinks -- and {@code isDefault} adds the
     * outer dark frame that marks a dialog's default button. The two combine.
     */
    public static void drawBezel(Graphics g, int x, int y, int w, int h, boolean isPressed,
            boolean isDefault, Color shadow, Color darkShadow, Color highlight,
            Color lightHighlight) {
        Color old = g.getColor();
        g.translate(x, y);

        if (isPressed && isDefault) {
            g.setColor(darkShadow);
            g.drawRect(0, 0, w - 1, h - 1);
            g.setColor(shadow);
            g.drawRect(1, 1, w - 3, h - 3);
        } else if (isPressed) {
            drawLoweredBezel(g, 0, 0, w, h, shadow, darkShadow, highlight, lightHighlight);
        } else if (isDefault) {
            g.setColor(darkShadow);
            g.drawRect(0, 0, w - 1, h - 1);

            g.setColor(lightHighlight);
            g.drawLine(1, 1, 1, h - 3);
            g.drawLine(2, 1, w - 3, 1);

            g.setColor(highlight);
            g.drawLine(2, 2, 2, h - 4);
            g.drawLine(3, 2, w - 4, 2);

            g.setColor(shadow);
            g.drawLine(2, h - 3, w - 3, h - 3);
            g.drawLine(w - 3, 2, w - 3, h - 4);

            g.setColor(darkShadow);
            g.drawLine(1, h - 2, w - 2, h - 2);
            g.drawLine(w - 2, h - 2, w - 2, 1);
        } else {
            g.setColor(lightHighlight);
            g.drawLine(0, 0, 0, h - 1);
            g.drawLine(1, 0, w - 2, 0);

            g.setColor(highlight);
            g.drawLine(1, 1, 1, h - 3);
            g.drawLine(2, 1, w - 3, 1);

            g.setColor(shadow);
            g.drawLine(1, h - 2, w - 2, h - 2);
            g.drawLine(w - 2, 1, w - 2, h - 3);

            g.setColor(darkShadow);
            g.drawLine(0, h - 1, w - 1, h - 1);
            g.drawLine(w - 1, h - 1, w - 1, 0);
        }

        g.translate(-x, -y);
        g.setColor(old);
    }

    /** The sunken bevel: the raised one's colours, swapped. */
    public static void drawLoweredBezel(Graphics g, int x, int y, int w, int h, Color shadow,
            Color darkShadow, Color highlight, Color lightHighlight) {
        Color old = g.getColor();
        g.translate(x, y);

        g.setColor(darkShadow);
        g.drawLine(0, 0, 0, h - 1);
        g.drawLine(1, 0, w - 2, 0);

        g.setColor(shadow);
        g.drawLine(1, 1, 1, h - 2);
        g.drawLine(1, 1, w - 3, 1);

        g.setColor(lightHighlight);
        g.drawLine(0, h - 1, w - 1, h - 1);
        g.drawLine(w - 1, h - 1, w - 1, 0);

        g.setColor(highlight);
        g.drawLine(1, h - 2, w - 2, h - 2);
        g.drawLine(w - 2, h - 2, w - 2, 1);

        g.translate(-x, -y);
        g.setColor(old);
    }

    /**
     * It draws text underlining the mnemonic's first appearance.
     *
     * <p>It looks for the upper-case letter first and the lower-case one afterwards, like
     * {@code JLabel} when choosing what to underline. A mnemonic that is not in the text
     * underlines nothing, and that is not an error: a button may have a shortcut without the
     * letter appearing.
     */
    public static void drawString(Graphics g, String text, int underlinedChar, int x, int y) {
        int index = -1;
        if (underlinedChar != '\0') {
            char shift = Character.toUpperCase((char) underlinedChar);
            char minus = Character.toLowerCase((char) underlinedChar);
            int i1 = text.indexOf(shift);
            int i2 = text.indexOf(minus);
            if (i1 == -1) {
                index = i2;
            } else if (i2 == -1) {
                index = i1;
            } else {
                index = Math.min(i1, i2);
            }
        }
        drawStringUnderlineCharAt(g, text, index, x, y);
    }

    /**
     * It draws text underlining the character at that position.
     *
     * <p>The stroke goes one pixel above the bottom of the descent, the character's width, and one
     * pixel high: it is where the JDK puts it, and what keeps the underline from clashing with the
     * letters' baseline or from drifting away from them.
     */
    public static void drawStringUnderlineCharAt(Graphics g, String text, int underlinedIndex,
            int x, int y) {
        g.drawString(text, x, y);
        if (underlinedIndex >= 0 && underlinedIndex < text.length()) {
            FontMetrics fm = g.getFontMetrics();
            int stripeX = x + fm.stringWidth(text.substring(0, underlinedIndex));
            int stripeY = y;
            int stripeWidth = fm.charWidth(text.charAt(underlinedIndex));
            int stripeHeight = 1;
            g.fillRect(stripeX, stripeY + fm.getDescent() - 1, stripeWidth, stripeHeight);
        }
    }

    /**
     * A dotted rectangle, one pixel on and one off.
     *
     * <p>It is the focus frame of the basic looks and feels. It is drawn pixel by pixel and not
     * with a dashed stroke because it has to come out the same at the corners, where a stroke
     * would get out of phase.
     */
    public static void drawDashedRect(Graphics g, int x, int y, int width, int height) {
        int vx;
        int vy;
        for (vx = x; vx < (x + width); vx = vx + 2) {
            g.fillRect(vx, y, 1, 1);
            g.fillRect(vx, y + height - 1, 1, 1);
        }
        for (vy = y; vy < (y + height); vy = vy + 2) {
            g.fillRect(x, vy, 1, 1);
            g.fillRect(x + width - 1, vy, 1, 1);
        }
    }

    /**
     * A button's preferred size: icon and text placed in an infinite view, plus the insets.
     *
     * <p>{@code null} if the button has children: then the size is decided by its layout, not by
     * its text. With no text the icon-text gap is zero, as when painting, so that an icon-only
     * button measures what the icon measures.
     */
    public static Dimension getPreferredButtonSize(AbstractButton b, int textIconGap) {
        if (b.getComponentCount() > 0) {
            return null;
        }
        Icon icon = b.getIcon();
        String text = b.getText();
        Font font = b.getFont();
        FontMetrics fm = b.getFontMetrics(font);
        Rectangle iconRect = new Rectangle();
        Rectangle textRect = new Rectangle();
        Rectangle viewRect = new Rectangle(Short.MAX_VALUE, Short.MAX_VALUE);
        SwingUtilities.layoutCompoundLabel(b, fm, text, icon, b.getVerticalAlignment(),
                b.getHorizontalAlignment(), b.getVerticalTextPosition(),
                b.getHorizontalTextPosition(), viewRect, iconRect, textRect,
                text == null ? 0 : textIconGap);
        Rectangle r = iconRect.union(textRect);
        Insets insets = b.getInsets();
        r.width = r.width + insets.left + insets.right;
        r.height = r.height + insets.top + insets.bottom;
        return new Dimension(r.width, r.height);
    }

    /** Whether the component is read left to right. */
    static boolean isLeftToRight(Component c) {
        return c.getComponentOrientation().isLeftToRight();
    }

    /**
     * Whether the system's shortcut modifier is held down.
     *
     * <p>Control, in this VM: there is no {@code Toolkit.getMenuShortcutKeyMaskEx} to consult, and
     * Control is what it returns on every platform that is not macOS.
     */
    static boolean isMenuShortcutKeyDown(InputEvent event) {
        return (event.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
    }

    /** It draws text with the component's rendering hints; see {@link #getStringWidth}'s note. */
    public static void drawString(JComponent c, Graphics2D g, String string, float x, float y) {
        if (string == null || string.isEmpty()) {
            return;
        }
        g.drawString(string, x, y);
    }

    /** It draws text underlining a position, with fractional coordinates. */
    public static void drawStringUnderlineCharAt(JComponent c, Graphics2D g, String string,
            int underlinedIndex, float x, float y) {
        if (string == null || string.isEmpty()) {
            return;
        }
        drawStringUnderlineCharAt(g, string, underlinedIndex, Math.round(x), Math.round(y));
    }

    /**
     * It clips the string with an ellipsis so that it fits that width.
     *
     * <p>It returns the whole string if it fits, and only the dots if not even they fit: never
     * {@code null}. The loop adds one character at a time and stops at the first that goes over,
     * just like {@code SwingUtilities.layoutCompoundLabel}, so that a label and a text clipped by
     * hand cut at the same place.
     */
    public static String getClippedString(JComponent c, FontMetrics fm, String string,
            int availTextWidth) {
        if (string == null || string.isEmpty()) {
            return "";
        }
        int width = fm.stringWidth(string);
        if (width <= availTextWidth) {
            return string;
        }
        String points = "...";
        int available = availTextWidth - fm.stringWidth(points);
        if (available <= 0) {
            return points;
        }
        int total = 0;
        int n;
        for (n = 0; n < string.length(); n++) {
            total = total + fm.charWidth(string.charAt(n));
            if (total > available) {
                break;
            }
        }
        return string.substring(0, n) + points;
    }

    /**
     * A string's width.
     *
     * <p>An integer, even though the signature says {@code float}: this VM has no fractional
     * metrics, so the width is {@link FontMetrics#stringWidth}'s. The signature is the JDK's,
     * which does have them when the component asks for them.
     */
    public static float getStringWidth(JComponent c, FontMetrics fm, String string) {
        if (string == null || string.isEmpty()) {
            return 0.0f;
        }
        return fm.stringWidth(string);
    }
}
