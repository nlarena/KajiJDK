package javax.swing.border;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * An etched line: two pixels, one light and one dark, faking a groove or a ridge.
 *
 * <p>The same illusion of light as {@link BevelBorder} but with two colours instead of four, and
 * with another purpose: a bevel makes the <em>component</em> look raised, and this makes it look
 * as if there were a <em>carved line</em> around it. It is used to group, not to suggest that
 * something can be pressed.
 *
 * <p>Etched inwards and outwards are --again-- the same drawing with the two colours swapped.
 */
public class EtchedBorder extends AbstractBorder {

    private static final long serialVersionUID = 4001244046866360638L;

    /** The line looks as if it sticks out. */
    public static final int RAISED = 0;
    /** The line looks like a groove. */
    public static final int LOWERED = 1;

    protected int etchType;
    protected Color highlight;
    protected Color shadow;

    /** Etched inwards, with the colours derived from the background. */
    public EtchedBorder() {
        this(LOWERED);
    }

    /** Of the given type, with the colours derived from the background. */
    public EtchedBorder(int etchType) {
        this.etchType = etchType;
    }

    /** Etched inwards, with the two given colours. */
    public EtchedBorder(Color highlight, Color shadow) {
        this(LOWERED, highlight, shadow);
    }

    /** Of the given type and colours. */
    public EtchedBorder(int etchType, Color highlight, Color shadow) {
        this(etchType);
        this.highlight = highlight;
        this.shadow = shadow;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        int w = width;
        int h = height;
        Color old = g.getColor();

        g.translate(x, y);

        // The outer rectangle carries the "shadow" colour when it is sunk and the "highlight" one
        // when it sticks out; the inner one, the other way round. That swap is the whole difference
        // between the two types.
        if (this.etchType == LOWERED) {
            g.setColor(getShadowColor(c));
        } else {
            g.setColor(getHighlightColor(c));
        }
        g.drawRect(0, 0, w - 2, h - 2);

        if (this.etchType == LOWERED) {
            g.setColor(getHighlightColor(c));
        } else {
            g.setColor(getShadowColor(c));
        }
        g.drawLine(1, h - 3, 1, 1);
        g.drawLine(1, 1, w - 3, 1);
        g.drawLine(0, h - 1, w - 1, h - 1);
        g.drawLine(w - 1, h - 1, w - 1, 0);

        g.translate(-x, -y);
        g.setColor(old);
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = 2;
        insets.top = 2;
        insets.right = 2;
        insets.bottom = 2;
        return insets;
    }

    /** Opaque: the two pixels on each side are painted whole. */
    public boolean isBorderOpaque() {
        return true;
    }

    /** {@link #RAISED} or {@link #LOWERED}. */
    public int getEtchType() {
        return this.etchType;
    }

    /** The light colour; derived from {@code c}'s background if none was set. */
    public Color getHighlightColor(Component c) {
        if (this.highlight != null) {
            return this.highlight;
        }
        return c.getBackground().brighter();
    }

    /** The light colour that was set, or {@code null}. */
    public Color getHighlightColor() {
        return this.highlight;
    }

    /** The dark colour; derived from {@code c}'s background if none was set. */
    public Color getShadowColor(Component c) {
        if (this.shadow != null) {
            return this.shadow;
        }
        return c.getBackground().darker();
    }

    /** The dark colour that was set, or {@code null}. */
    public Color getShadowColor() {
        return this.shadow;
    }
}
