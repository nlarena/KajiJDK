package javax.swing.border;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * The two-pixel border that fakes relief: as if the component were raised or sunk.
 *
 * <h2>How relief is faked with four colours</h2>
 *
 * <p>The illusion is old and simple: if the light comes from the top left, the edges facing that
 * light look lighter and the opposite ones darker. Raised and lowered are <strong>the same
 * drawing with the colours swapped</strong> -- hence {@link #paintRaisedBevel} and
 * {@link #paintLoweredBevel} are almost the same code.
 *
 * <p>The four colours are two per side because the border is two pixels thick and each one
 * carries its own shade: the outer one more extreme, the inner one softer. All are optional, and
 * when they are missing they are derived from the component's background with
 * {@link Color#brighter} and {@link Color#darker} -- that way the same border works over any
 * colour without configuring it.
 */
public class BevelBorder extends AbstractBorder {

    private static final long serialVersionUID = -1034942243356299676L;

    /** The component looks raised. */
    public static final int RAISED = 0;
    /** The component looks sunk. */
    public static final int LOWERED = 1;

    protected int bevelType;
    protected Color highlightOuter;
    protected Color highlightInner;
    protected Color shadowInner;
    protected Color shadowOuter;

    /** With the colours derived from the component's background. */
    public BevelBorder(int bevelType) {
        this.bevelType = bevelType;
    }

    /**
     * With one light colour and one dark one.
     *
     * <p>The four shades come from those two: the outer pair is brightened or darkened one step
     * more.
     */
    public BevelBorder(int bevelType, Color highlight, Color shadow) {
        this(bevelType, highlight.brighter(), highlight, shadow, shadow.brighter());
    }

    /** With the four shades explicit. */
    public BevelBorder(int bevelType, Color highlightOuterColor, Color highlightInnerColor,
            Color shadowOuterColor, Color shadowInnerColor) {
        this(bevelType);
        this.highlightOuter = highlightOuterColor;
        this.highlightInner = highlightInnerColor;
        this.shadowOuter = shadowOuterColor;
        this.shadowInner = shadowInnerColor;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        if (this.bevelType == RAISED) {
            paintRaisedBevel(c, g, x, y, width, height);
        } else if (this.bevelType == LOWERED) {
            paintLoweredBevel(c, g, x, y, width, height);
        }
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = 2;
        insets.top = 2;
        insets.right = 2;
        insets.bottom = 2;
        return insets;
    }

    /** The outer light shade; derived from {@code c}'s background if none was set. */
    public Color getHighlightOuterColor(Component c) {
        Color own = getHighlightOuterColor();
        if (own != null) {
            return own;
        }
        return c.getBackground().brighter().brighter();
    }

    /** The inner light shade. */
    public Color getHighlightInnerColor(Component c) {
        Color own = getHighlightInnerColor();
        if (own != null) {
            return own;
        }
        return c.getBackground().brighter();
    }

    /** The inner dark shade. */
    public Color getShadowInnerColor(Component c) {
        Color own = getShadowInnerColor();
        if (own != null) {
            return own;
        }
        return c.getBackground().darker();
    }

    /** The outer dark shade. */
    public Color getShadowOuterColor(Component c) {
        Color own = getShadowOuterColor();
        if (own != null) {
            return own;
        }
        return c.getBackground().darker().darker();
    }

    /** The outer light shade that was set, or {@code null}. */
    public Color getHighlightOuterColor() {
        return this.highlightOuter;
    }

    /** The inner light shade that was set, or {@code null}. */
    public Color getHighlightInnerColor() {
        return this.highlightInner;
    }

    /** The inner dark shade that was set, or {@code null}. */
    public Color getShadowInnerColor() {
        return this.shadowInner;
    }

    /** The outer dark shade that was set, or {@code null}. */
    public Color getShadowOuterColor() {
        return this.shadowOuter;
    }

    /** {@link #RAISED} or {@link #LOWERED}. */
    public int getBevelType() {
        return this.bevelType;
    }

    /** Opaque: the two pixels on each side are painted whole. */
    public boolean isBorderOpaque() {
        return true;
    }

    /** Draws the raised relief: light at the top and on the left. */
    protected void paintRaisedBevel(Component c, Graphics g, int x, int y, int width, int height) {
        paintBevel(g, x, y, width, height,
                getHighlightOuterColor(c), getHighlightInnerColor(c),
                getShadowOuterColor(c), getShadowInnerColor(c));
    }

    /**
     * Draws the lowered relief.
     *
     * <p>It is the same drawing as {@link #paintRaisedBevel} with the pairs swapped: the dark goes
     * to the top and to the left. The whole illusion is in that swap.
     */
    protected void paintLoweredBevel(Component c, Graphics g, int x, int y, int width, int height) {
        paintBevel(g, x, y, width, height,
                getShadowInnerColor(c), getShadowOuterColor(c),
                getHighlightInnerColor(c), getHighlightOuterColor(c));
    }

    /**
     * The drawing, parameterized by the four shades.
     *
     * <p>It exists so that the two forms of relief are not two copies of the same path: a copy is
     * where a bug gets fixed once out of twice.
     */
    private void paintBevel(Graphics g, int x, int y, int width, int height,
            Color outerHighlight, Color innerHighlight, Color outerShadow, Color innerShadow) {
        Color oldColor = g.getColor();
        int h = height;
        int w = width;

        g.translate(x, y);

        g.setColor(outerHighlight);
        g.drawLine(0, 0, 0, h - 2);
        g.drawLine(1, 0, w - 2, 0);

        g.setColor(innerHighlight);
        g.drawLine(1, 1, 1, h - 3);
        g.drawLine(2, 1, w - 3, 1);

        g.setColor(outerShadow);
        g.drawLine(0, h - 1, w - 1, h - 1);
        g.drawLine(w - 1, 0, w - 1, h - 2);

        g.setColor(innerShadow);
        g.drawLine(1, h - 2, w - 2, h - 2);
        g.drawLine(w - 2, 1, w - 2, h - 3);

        g.translate(-x, -y);
        g.setColor(oldColor);
    }
}
