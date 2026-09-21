package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

/**
 * A linear gradient between **two** colours.
 *
 * <p>It is defined by two points and two colours: at the first point the colour is the first, at
 * the second the second, and in between it is interpolated along the line joining them.
 * Perpendicular to that line the colour does not change, which is what makes a gradient look like
 * parallel bands.
 *
 * <p>Outside the segment there are two behaviours. Without cycling, the colour is stretched:
 * everything beyond the second point stays the second colour. With cycling, the gradient bounces
 * between the two points, and since it bounces **as a mirror** no seam is left where it repeats.
 */
public class GradientPaint implements Paint {

    private final Point2D.Float p1;
    private final Point2D.Float p2;
    private final Color color1;
    private final Color color2;
    private final boolean cyclic;

    /**
     * With the two points given by coordinates, without cycling.
     *
     * @throws NullPointerException if either of the two colours is missing
     */
    public GradientPaint(float x1, float y1, Color color1, float x2, float y2, Color color2) {
        this(x1, y1, color1, x2, y2, color2, false);
    }

    /**
     * With the two points given as objects, without cycling.
     *
     * @throws NullPointerException if any of the four is missing
     */
    public GradientPaint(Point2D pt1, Color color1, Point2D pt2, Color color2) {
        this(pt1, color1, pt2, color2, false);
    }

    /**
     * With the two points given by coordinates.
     *
     * @throws NullPointerException if either of the two colours is missing
     */
    public GradientPaint(float x1, float y1, Color color1, float x2, float y2, Color color2,
            boolean cyclic) {
        if (color1 == null || color2 == null) {
            throw new NullPointerException("Colors cannot be null");
        }
        this.p1 = new Point2D.Float(x1, y1);
        this.p2 = new Point2D.Float(x2, y2);
        this.color1 = color1;
        this.color2 = color2;
        this.cyclic = cyclic;
    }

    /**
     * With the two points given as objects.
     *
     * @throws NullPointerException if any of the four is missing
     */
    public GradientPaint(Point2D pt1, Color color1, Point2D pt2, Color color2, boolean cyclic) {
        if (color1 == null || color2 == null || pt1 == null || pt2 == null) {
            throw new NullPointerException("Colors and points should be non-null");
        }
        this.p1 = new Point2D.Float((float) pt1.getX(), (float) pt1.getY());
        this.p2 = new Point2D.Float((float) pt2.getX(), (float) pt2.getY());
        this.color1 = color1;
        this.color2 = color2;
        this.cyclic = cyclic;
    }

    /** The point where the colour is {@link #getColor1}. */
    public Point2D getPoint1() {
        return new Point2D.Float(this.p1.x, this.p1.y);
    }

    /** The colour of the first point. */
    public Color getColor1() {
        return this.color1;
    }

    /** The point where the colour is {@link #getColor2}. */
    public Point2D getPoint2() {
        return new Point2D.Float(this.p2.x, this.p2.y);
    }

    /** The colour of the second point. */
    public Color getColor2() {
        return this.color2;
    }

    /** Whether the gradient bounces between the two points instead of stretching. */
    public boolean isCyclic() {
        return this.cyclic;
    }

    /** `OPAQUE` if both colours are opaque, `TRANSLUCENT` if either is not. */
    public int getTransparency() {
        if (this.color1.getAlpha() == 0xFF && this.color2.getAlpha() == 0xFF) {
            return Transparency.OPAQUE;
        }
        return Transparency.TRANSLUCENT;
    }

    /**
     * Builds the machine that generates the pixels.
     *
     * <p>If the transformation cannot be inverted, the gradient degrades to a flat colour: the
     * first one. It is the only thing that can be done without being able to take a pixel back to
     * user coordinates, and it is preferable to throwing in the middle of drawing.
     */
    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds,
            Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
        try {
            return new GradientContext(xform);
        } catch (NoninvertibleTransformException e) {
            return this.color1.createContext(cm, deviceBounds, userBounds, xform, hints);
        }
    }

    /** The context that computes the gradient point by point. */
    private final class GradientContext extends RasterPaintContext {

        private final double dx;
        private final double dy;
        private final double lengthSq;

        GradientContext(AffineTransform xform) throws NoninvertibleTransformException {
            super(xform);
            this.dx = GradientPaint.this.p2.x - GradientPaint.this.p1.x;
            this.dy = GradientPaint.this.p2.y - GradientPaint.this.p1.y;
            this.lengthSq = this.dx * this.dx + this.dy * this.dy;
        }

        int colorAt(double ux, double uy) {
            if (this.lengthSq == 0.0) {
                return GradientPaint.this.color2.getRGB();
            }
            // The scalar projection of the point onto the segment, normalized: how much of the way
            // from p1 to p2 has been covered. What is perpendicular to the segment does not take
            // part, and that is why the colour does not change in that direction.
            double t = ((ux - GradientPaint.this.p1.x) * this.dx
                    + (uy - GradientPaint.this.p1.y) * this.dy) / this.lengthSq;
            if (GradientPaint.this.cyclic) {
                double folded = t - Math.floor(t / 2) * 2;
                t = folded > 1.0 ? 2.0 - folded : folded;
            } else if (t < 0.0) {
                t = 0.0;
            } else if (t > 1.0) {
                t = 1.0;
            }
            Color a = GradientPaint.this.color1;
            Color b = GradientPaint.this.color2;
            int al = toByte(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
            int r = toByte(a.getRed() + (b.getRed() - a.getRed()) * t);
            int g = toByte(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
            int bl = toByte(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
            return (al << 24) | (r << 16) | (g << 8) | bl;
        }
    }

    /** A value brought into a byte. */
    private static int toByte(double v) {
        int i = (int) (v + 0.5);
        if (i < 0) {
            return 0;
        }
        if (i > 255) {
            return 255;
        }
        return i;
    }
}
