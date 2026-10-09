package javax.swing.border;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Rectangle2D$Float;

/**
 * A border drawn with a {@link BasicStroke}, that is with all of Java2D's stroking machinery.
 *
 * <h2>What it can do that the others cannot</h2>
 *
 * <p>The other borders in this package draw one-pixel lines with {@code drawLine}. This one
 * delegates to Java2D, so it inherits for free whatever a {@code BasicStroke} knows how to do:
 * dashes, round caps, bevelled joins, fractional thicknesses. It is the only
 * <em>configurable</em> border in the package in that sense -- the others have the shape they
 * have.
 *
 * <p>The {@link Paint} is optional, and when it is missing the component's colour is used. That
 * allows one same dotted border to follow the text colour of every component that carries it.
 *
 * <h2>The insets arithmetic</h2>
 *
 * <p>A stroke of thickness {@code n} is drawn <strong>centred</strong> over the line: half
 * outwards and half inwards. That is why the insets are the thickness rounded up and not the
 * thickness plain -- with less, the inner half of the stroke would cover the content.
 */
public class StrokeBorder extends AbstractBorder {

    private final BasicStroke stroke;
    private final Paint paint;

    /**
     * With the given stroke, painted with the component's colour.
     *
     * @throws NullPointerException if {@code stroke} is {@code null}
     */
    public StrokeBorder(BasicStroke stroke) {
        this(stroke, null);
    }

    /**
     * With the given stroke and paint.
     *
     * @throws NullPointerException if {@code stroke} is {@code null}
     */
    public StrokeBorder(BasicStroke stroke, Paint paint) {
        if (stroke == null) {
            throw new NullPointerException("The stroke cannot be null");
        }
        this.stroke = stroke;
        this.paint = paint;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        float thickness = this.stroke.getLineWidth();
        if (thickness <= 0.0f) {
            return;
        }
        if (!(g instanceof Graphics2D)) {
            // Without Java2D there is no stroke to apply. Declining is better than drawing a plain
            // rectangle: that would be a different border from the one asked for, and silently so.
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setStroke(this.stroke);
        if (this.paint != null) {
            g2.setPaint(this.paint);
        } else {
            g2.setPaint(c.getForeground());
        }
        // The rectangle shrinks by half a thickness on each side so that the stroke, which is drawn
        // centred, falls whole inside the border's area.
        float half = thickness / 2.0f;
        // `Rectangle2D$Float` with the binary name: the Java name of a nested type from another
        // file does not resolve in our compiler (#101), the same as in `AbstractBorder`.
        Shape r = new Rectangle2D$Float(x + half, y + half,
                (float) width - thickness, (float) height - thickness);
        g2.draw(r);
        g2.dispose();
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        int side = (int) Math.ceil((double) this.stroke.getLineWidth());
        insets.top = side;
        insets.left = side;
        insets.right = side;
        insets.bottom = side;
        return insets;
    }

    /** The stroke it is drawn with. Never {@code null}. */
    public BasicStroke getStroke() {
        return this.stroke;
    }

    /** The paint, or {@code null} if it follows the component's colour. */
    public Paint getPaint() {
        return this.paint;
    }
}
