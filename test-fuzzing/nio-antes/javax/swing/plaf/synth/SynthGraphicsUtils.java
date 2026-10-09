package javax.swing.plaf.synth;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * The drawing computations a look and feel may want to do differently.
 *
 * <h2>Why it is an object and not static methods</h2>
 *
 * <p>Because measuring and drawing text is exactly where a look and feel distinguishes itself:
 * where it puts the ellipsis when something does not fit, how it underlines the keyboard
 * shortcut, how much room it leaves between the icon and the word. A look and feel that wants to
 * change it inherits from this class and returns it from its style.
 *
 * <p>The three static methods at the end are the exception, and for a reason: drawing an icon
 * that may be context-sensitive --one that looks different if the component is disabled-- is the
 * same operation for every look and feel.
 *
 * <h2>State in this library</h2>
 *
 * <p>What is computation is done: measuring a text, measuring an icon, computing the preferred
 * size. Drawing needs the graphics engine, and what this library has of {@code java.awt} is
 * enough for the measurements but not for painting; the {@code paint} methods delegate to
 * whatever is there and invent nothing.
 *
 * @since 1.5
 */
public class SynthGraphicsUtils {

    /** One. */
    public SynthGraphicsUtils() {
    }

    /**
     * It draws a line.
     *
     * @param context what is being drawn
     * @param paintKey what the line is for, or {@code null}
     * @param g where to draw
     * @param x1 from
     * @param y1 from
     * @param x2 to
     * @param y2 to
     */
    public void drawLine(SynthContext context, Object paintKey, Graphics g, int x1, int y1,
            int x2, int y2) {
        g.drawLine(x1, y1, x2, y2);
    }

    /**
     * It draws a line with a style.
     *
     * <p>The style --dotted, for instance-- is interpreted by each look and feel. This version does
     * not look at it, which is what the JDK's base one does: it draws the whole line.
     *
     * @param context what is being drawn
     * @param paintKey what the line is for, or {@code null}
     * @param g where to draw
     * @param x1 from
     * @param y1 from
     * @param x2 to
     * @param y2 to
     * @param styleKey the style, or {@code null}
     */
    public void drawLine(SynthContext context, Object paintKey, Graphics g, int x1, int y1,
            int x2, int y2, Object styleKey) {
        drawLine(context, paintKey, g, x1, y1, x2, y2);
    }

    /**
     * It shares the text and the icon out inside the available rectangle.
     *
     * @param ss what is being drawn
     * @param fm the typeface's metrics
     * @param text the text, or {@code null}
     * @param icon the icon, or {@code null}
     * @param hAlign the horizontal alignment
     * @param vAlign the vertical alignment
     * @param hTextPosition where the text goes relative to the icon, horizontally
     * @param vTextPosition where the text goes relative to the icon, vertically
     * @param viewR the available rectangle
     * @param iconR it is filled with where the icon goes
     * @param textR it is filled with where the text goes
     * @param iconTextGap how much room to leave between the two
     * @return the text just as it is going to be drawn, shortened if it did not fit
     */
    public String layoutText(SynthContext ss, FontMetrics fm, String text, Icon icon, int hAlign,
            int vAlign, int hTextPosition, int vTextPosition, Rectangle viewR, Rectangle iconR,
            Rectangle textR, int iconTextGap) {
        return SwingUtilities.layoutCompoundLabel(ss.getComponent(), fm, text, icon, vAlign,
                hAlign, vTextPosition, hTextPosition, viewR, iconR, textR, iconTextGap);
    }

    /**
     * How much that text measures.
     *
     * @param ss what is being drawn
     * @param font the typeface
     * @param metrics its measurements
     * @param text the text, or {@code null}
     * @return the width in pixels
     */
    public int computeStringWidth(SynthContext ss, Font font, FontMetrics metrics, String text) {
        return text == null ? 0 : metrics.stringWidth(text);
    }

    /**
     * The smallest size the text and the icon fit in.
     *
     * @param ss what is being drawn
     * @param font the typeface
     * @param text the text, or {@code null}
     * @param icon the icon, or {@code null}
     * @param hAlign the horizontal alignment
     * @param vAlign the vertical alignment
     * @param hTextPosition where the text goes relative to the icon, horizontally
     * @param vTextPosition where the text goes relative to the icon, vertically
     * @param iconTextGap how much room to leave between the two
     * @param mnemonicIndex the shortcut letter, or -1
     * @return the size
     */
    public Dimension getMinimumSize(SynthContext ss, Font font, String text, Icon icon,
            int hAlign, int vAlign, int hTextPosition, int vTextPosition, int iconTextGap,
            int mnemonicIndex) {
        return getPreferredSize(ss, font, text, icon, hAlign, vAlign, hTextPosition, vTextPosition,
                iconTextGap, mnemonicIndex);
    }

    /**
     * The largest size it accepts.
     *
     * @param ss what is being drawn
     * @param font the typeface
     * @param text the text, or {@code null}
     * @param icon the icon, or {@code null}
     * @param hAlign the horizontal alignment
     * @param vAlign the vertical alignment
     * @param hTextPosition where the text goes relative to the icon, horizontally
     * @param vTextPosition where the text goes relative to the icon, vertically
     * @param iconTextGap how much room to leave between the two
     * @param mnemonicIndex the shortcut letter, or -1
     * @return the size
     */
    public Dimension getMaximumSize(SynthContext ss, Font font, String text, Icon icon,
            int hAlign, int vAlign, int hTextPosition, int vTextPosition, int iconTextGap,
            int mnemonicIndex) {
        return getPreferredSize(ss, font, text, icon, hAlign, vAlign, hTextPosition, vTextPosition,
                iconTextGap, mnemonicIndex);
    }

    /**
     * How much height the tallest letter takes up.
     *
     * @param context what is being drawn
     * @return the height in pixels
     */
    public int getMaximumCharHeight(SynthContext context) {
        final Font f = context.getStyle().getFont(context);
        final FontMetrics fm = context.getComponent().getFontMetrics(f);
        return fm == null ? 0 : fm.getHeight();
    }

    /**
     * The size it would prefer to have.
     *
     * @param ss what is being drawn
     * @param font the typeface
     * @param text the text, or {@code null}
     * @param icon the icon, or {@code null}
     * @param hAlign the horizontal alignment
     * @param vAlign the vertical alignment
     * @param hTextPosition where the text goes relative to the icon, horizontally
     * @param vTextPosition where the text goes relative to the icon, vertically
     * @param iconTextGap how much room to leave between the two
     * @param mnemonicIndex the shortcut letter, or -1
     * @return the size
     */
    public Dimension getPreferredSize(SynthContext ss, Font font, String text, Icon icon,
            int hAlign, int vAlign, int hTextPosition, int vTextPosition, int iconTextGap,
            int mnemonicIndex) {
        final FontMetrics fm = ss.getComponent().getFontMetrics(font);
        final int textWidth = text == null || fm == null ? 0 : fm.stringWidth(text);
        final int textHeight = text == null || fm == null ? 0 : fm.getHeight();
        final int iconWidth = icon == null ? 0 : icon.getIconWidth();
        final int iconHeight = icon == null ? 0 : icon.getIconHeight();
        if (icon == null) {
            return new Dimension(textWidth, textHeight);
        }
        if (text == null) {
            return new Dimension(iconWidth, iconHeight);
        }
        // Side by side or one on top of the other, according to where the text goes. The gap only
                // counts in the direction they are separated in.
        if (hTextPosition == SwingConstants.CENTER) {
            return new Dimension(Math.max(textWidth, iconWidth),
                    textHeight + iconHeight + iconTextGap);
        }
        return new Dimension(textWidth + iconWidth + iconTextGap,
                Math.max(textHeight, iconHeight));
    }

    /**
     * It draws the text inside that rectangle.
     *
     * @param ss what is being drawn
     * @param g where to draw
     * @param text the text, or {@code null}
     * @param bounds the rectangle
     * @param mnemonicIndex the shortcut letter, or -1
     */
    public void paintText(SynthContext ss, Graphics g, String text, Rectangle bounds,
            int mnemonicIndex) {
        if (text != null) {
            paintText(ss, g, text, bounds.x, bounds.y, mnemonicIndex);
        }
    }

    /**
     * It draws the text at that position.
     *
     * @param ss what is being drawn
     * @param g where to draw
     * @param text the text, or {@code null}
     * @param x the left corner
     * @param y the top corner
     * @param mnemonicIndex the shortcut letter, or -1
     */
    public void paintText(SynthContext ss, Graphics g, String text, int x, int y,
            int mnemonicIndex) {
        if (text == null) {
            return;
        }
        final FontMetrics fm = g.getFontMetrics();
        // The `y` that arrives is the rectangle's top one and `drawString` wants the baseline's:
                // without adding the ascent, the text comes out above where it has to go.
        g.drawString(text, x, y + (fm == null ? 0 : fm.getAscent()));
    }

    /**
     * It draws the text and the icon together.
     *
     * @param ss what is being drawn
     * @param g where to draw
     * @param text the text, or {@code null}
     * @param icon the icon, or {@code null}
     * @param hAlign the horizontal alignment
     * @param vAlign the vertical alignment
     * @param hTextPosition where the text goes relative to the icon, horizontally
     * @param vTextPosition where the text goes relative to the icon, vertically
     * @param iconTextGap how much room to leave between the two
     * @param mnemonicIndex the shortcut letter, or -1
     * @param textOffset how much to shift the text, for the pressed effect
     */
    public void paintText(SynthContext ss, Graphics g, String text, Icon icon, int hAlign,
            int vAlign, int hTextPosition, int vTextPosition, int iconTextGap, int mnemonicIndex,
            int textOffset) {
        final Rectangle available = new Rectangle(ss.getComponent().getSize());
        final Rectangle iconRect = new Rectangle();
        final Rectangle textRect = new Rectangle();
        final FontMetrics fm = g.getFontMetrics();
        final String clipped = layoutText(ss, fm, text, icon, hAlign, vAlign, hTextPosition,
                vTextPosition, available, iconRect, textRect, iconTextGap);
        if (icon != null) {
            paintIcon(icon, ss, g, iconRect.x + textOffset, iconRect.y + textOffset,
                    iconRect.width, iconRect.height);
        }
        paintText(ss, g, clipped, textRect.x + textOffset, textRect.y + textOffset, mnemonicIndex);
    }

    /**
     * An icon's width, which may depend on the context.
     *
     * @param icon the icon, or {@code null}
     * @param context what is being drawn
     * @return the width, or zero
     */
    public static int getIconWidth(Icon icon, SynthContext context) {
        return icon == null ? 0 : icon.getIconWidth();
    }

    /**
     * An icon's height, which may depend on the context.
     *
     * @param icon the icon, or {@code null}
     * @param context what is being drawn
     * @return the height, or zero
     */
    public static int getIconHeight(Icon icon, SynthContext context) {
        return icon == null ? 0 : icon.getIconHeight();
    }

    /**
     * It draws an icon, which may depend on the context.
     *
     * @param icon the icon, or {@code null}
     * @param context what is being drawn
     * @param g where to draw
     * @param x the left corner
     * @param y the top corner
     * @param w the width
     * @param h the height
     */
    public static void paintIcon(Icon icon, SynthContext context, Graphics g, int x, int y,
            int w, int h) {
        if (icon != null) {
            icon.paintIcon(context.getComponent(), g, x, y);
        }
    }
}
