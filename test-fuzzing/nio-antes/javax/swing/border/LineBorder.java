package javax.swing.border;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * A line of the same thickness on all four sides.
 *
 * <p>The simplest border that draws something. The two {@code create*} methods return
 * <strong>shared</strong> instances: a border keeps nothing of the component that uses it, so the
 * same object serves everybody who wants a one-pixel black line.
 */
public class LineBorder extends AbstractBorder {

    private static final long serialVersionUID = -787563427772288970L;

    private static Border BLACK_LINE;
    private static Border GRAY_LINE;

    protected int thickness;
    protected Color lineColor;
    protected boolean roundedCorners;

    /** A one-pixel black line, shared. */
    public static Border createBlackLineBorder() {
        if (BLACK_LINE == null) {
            BLACK_LINE = new LineBorder(Color.black, 1);
        }
        return BLACK_LINE;
    }

    /** A one-pixel grey line, shared. */
    public static Border createGrayLineBorder() {
        if (GRAY_LINE == null) {
            GRAY_LINE = new LineBorder(Color.gray, 1);
        }
        return GRAY_LINE;
    }

    /** A one-pixel line of the given colour. */
    public LineBorder(Color color) {
        this(color, 1, false);
    }

    /** A line of the given colour and thickness. */
    public LineBorder(Color color, int thickness) {
        this(color, thickness, false);
    }

    /** The same, choosing whether the corners are rounded. */
    public LineBorder(Color color, int thickness, boolean roundedCorners) {
        this.lineColor = color;
        this.thickness = thickness;
        this.roundedCorners = roundedCorners;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        if (this.thickness <= 0) {
            return;
        }
        Color old = g.getColor();
        g.setColor(this.lineColor);
        // One rectangle per pixel of thickness, shrinking inwards. The minus one is because
        // drawRect draws inclusively: a rectangle of width w goes from x to x+w.
        for (int i = 0; i < this.thickness; i++) {
            if (this.roundedCorners) {
                g.drawRoundRect(x + i, y + i, width - i - i - 1, height - i - i - 1,
                        this.thickness, this.thickness);
            } else {
                g.drawRect(x + i, y + i, width - i - i - 1, height - i - i - 1);
            }
        }
        g.setColor(old);
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = this.thickness;
        insets.top = this.thickness;
        insets.right = this.thickness;
        insets.bottom = this.thickness;
        return insets;
    }

    /** The line's colour. */
    public Color getLineColor() {
        return this.lineColor;
    }

    /** The thickness in pixels. */
    public int getThickness() {
        return this.thickness;
    }

    /** Whether the corners are rounded. */
    public boolean getRoundedCorners() {
        return this.roundedCorners;
    }

    /**
     * Opaque only if the corners are square.
     *
     * <p>With rounded corners four little pieces are left unpainted, so promising opacity would
     * leave rubbish right there. It is the clearest example of what that method is for.
     */
    public boolean isBorderOpaque() {
        return !this.roundedCorners;
    }
}
