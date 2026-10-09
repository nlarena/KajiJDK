package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

/**
 * A gradient that comes **out of a point** in concentric circles.
 *
 * <p>The first stop is at the focus and the last on the circumference; in between, the colour
 * depends on what fraction of the way from the focus to the edge has been covered.
 *
 * <p>The **focus** is what gives it the look of a highlight. With the focus at the centre, the
 * rings are concentric and it looks flat; moving it, the rings crowd on one side and spread on the
 * other, and the illusion of a sphere lit from that point appears.
 *
 * <p>A focus on the circumference or outside it has no solution —the fraction would go to infinity—
 * so it is pushed in to 99% of the radius. The JDK does the same with a slightly different
 * threshold: it moves the focus when its squared distance passes 99% of the squared radius, which
 * leaves it at about 99.5% of the radius. That is preferable to throwing over a point whoever draws
 * did not choose on purpose.
 */
public final class RadialGradientPaint extends MultipleGradientPaint {

    private final Point2D center;
    private final Point2D focus;
    private final float radius;

    /**
     * With centre and radius, the focus at the centre, without cycling and in sRGB.
     *
     * @throws IllegalArgumentException if the radius is not positive or the stops are not valid
     */
    public RadialGradientPaint(float cx, float cy, float radius, float[] fractions,
            Color[] colors) {
        this(new Point2D.Float(cx, cy), radius, fractions, colors, CycleMethod.NO_CYCLE);
    }

    /**
     * The same, with the centre as an object.
     *
     * @throws IllegalArgumentException if the radius is not positive or the stops are not valid
     */
    public RadialGradientPaint(Point2D center, float radius, float[] fractions, Color[] colors) {
        this(center, radius, fractions, colors, CycleMethod.NO_CYCLE);
    }

    /**
     * With the given cycle.
     *
     * @throws IllegalArgumentException if the radius is not positive or the stops are not valid
     */
    public RadialGradientPaint(float cx, float cy, float radius, float[] fractions,
            Color[] colors, CycleMethod cycleMethod) {
        this(new Point2D.Float(cx, cy), radius, fractions, colors, cycleMethod);
    }

    /**
     * With the given cycle and the centre as an object.
     *
     * @throws IllegalArgumentException if the radius is not positive or the stops are not valid
     */
    public RadialGradientPaint(Point2D center, float radius, float[] fractions, Color[] colors,
            CycleMethod cycleMethod) {
        this(center, radius, center, fractions, colors, cycleMethod, ColorSpaceType.SRGB,
                new AffineTransform());
    }

    /**
     * With the focus apart from the centre.
     *
     * @throws IllegalArgumentException if the radius is not positive or the stops are not valid
     */
    public RadialGradientPaint(float cx, float cy, float radius, float fx, float fy,
            float[] fractions, Color[] colors, CycleMethod cycleMethod) {
        this(new Point2D.Float(cx, cy), radius, new Point2D.Float(fx, fy), fractions, colors,
                cycleMethod);
    }

    /**
     * With the focus apart from the centre, both as objects.
     *
     * @throws IllegalArgumentException if the radius is not positive or the stops are not valid
     */
    public RadialGradientPaint(Point2D center, float radius, Point2D focus, float[] fractions,
            Color[] colors, CycleMethod cycleMethod) {
        this(center, radius, focus, fractions, colors, cycleMethod, ColorSpaceType.SRGB,
                new AffineTransform());
    }

    /**
     * From the rectangle that encloses the circle.
     *
     * <p>With a rectangle that is not square, the gradient comes out **elliptical**: the
     * transformation that takes the square to the rectangle is kept as the gradient's own
     * transformation.
     *
     * @throws IllegalArgumentException if the rectangle is empty or the stops are not valid
     */
    public RadialGradientPaint(Rectangle2D gradientBounds, float[] fractions, Color[] colors,
            CycleMethod cycleMethod) {
        this(new Point2D.Double(gradientBounds.getCenterX(), gradientBounds.getCenterY()),
                1.0f,
                new Point2D.Double(gradientBounds.getCenterX(), gradientBounds.getCenterY()),
                fractions, colors, cycleMethod, ColorSpaceType.SRGB,
                boundsTransform(gradientBounds));
    }

    /**
     * The transformation that takes the unit circle centred on the rectangle to the rectangle.
     *
     * @throws IllegalArgumentException if the rectangle is empty
     */
    private static AffineTransform boundsTransform(Rectangle2D r) {
        if (r == null) {
            throw new NullPointerException("Gradient bounds cannot be null");
        }
        if (r.getWidth() <= 0 || r.getHeight() <= 0) {
            throw new IllegalArgumentException("Gradient bounds must be non-empty");
        }
        AffineTransform t = AffineTransform.getTranslateInstance(r.getX(), r.getY());
        t.scale(r.getWidth(), r.getHeight());
        t.translate(-0.5 + 0.5, -0.5 + 0.5);
        AffineTransform g = AffineTransform.getTranslateInstance(r.getCenterX(), r.getCenterY());
        g.scale(r.getWidth() / 2, r.getHeight() / 2);
        g.translate(-r.getCenterX(), -r.getCenterY());
        return g;
    }

    /**
     * The general constructor.
     *
     * @throws NullPointerException if any of the arguments is missing
     * @throws IllegalArgumentException if the radius is not positive, if there are fewer than two
     *     stops or if the fractions do not increase
     */
    public RadialGradientPaint(Point2D center, float radius, Point2D focus, float[] fractions,
            Color[] colors, CycleMethod cycleMethod, ColorSpaceType colorSpace,
            AffineTransform gradientTransform) {
        super(fractions, colors, cycleMethod, colorSpace, gradientTransform);
        if (center == null) {
            throw new NullPointerException("Center point must be non-null");
        }
        if (focus == null) {
            throw new NullPointerException("Focus point must be non-null");
        }
        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be greater than zero");
        }
        this.center = new Point2D.Double(center.getX(), center.getY());
        this.radius = radius;
        this.focus = clampFocus(center, focus, radius);
    }

    /** The focus, pushed inside the circle if needed. */
    private static Point2D clampFocus(Point2D center, Point2D focus, float radius) {
        double dx = focus.getX() - center.getX();
        double dy = focus.getY() - center.getY();
        double d = Math.sqrt(dx * dx + dy * dy);
        double limit = radius * 0.99;
        if (d <= limit) {
            return new Point2D.Double(focus.getX(), focus.getY());
        }
        double scale = limit / d;
        return new Point2D.Double(center.getX() + dx * scale, center.getY() + dy * scale);
    }

    /** The centre of the circle. */
    public Point2D getCenterPoint() {
        return new Point2D.Double(this.center.getX(), this.center.getY());
    }

    /** Where the gradient comes out of. */
    public Point2D getFocusPoint() {
        return new Point2D.Double(this.focus.getX(), this.focus.getY());
    }

    /** The radius of the circle. */
    public float getRadius() {
        return this.radius;
    }

    /**
     * Builds the machine that generates the pixels.
     *
     * <p>If the combined transformation cannot be inverted, the gradient degrades to the first
     * stop.
     */
    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds,
            Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
        AffineTransform total = new AffineTransform(xform);
        total.concatenate(this.gradientTransform);
        try {
            return new GradientContext(total);
        } catch (NoninvertibleTransformException e) {
            return this.colors[0].createContext(cm, deviceBounds, userBounds, xform, hints);
        }
    }

    /** The context that computes the gradient point by point. */
    private final class GradientContext extends RasterPaintContext {

        GradientContext(AffineTransform xform) throws NoninvertibleTransformException {
            super(xform);
        }

        int colorAt(double ux, double uy) {
            RadialGradientPaint p = RadialGradientPaint.this;
            double dx = ux - p.focus.getX();
            double dy = uy - p.focus.getY();
            double a = dx * dx + dy * dy;
            if (a == 0.0) {
                return p.colorForFraction(0.0f);
            }
            // How far the focus->point ray reaches before leaving the circle: the quadratic
            // |f + s*d - c|^2 = r^2 is solved and the positive root taken. The fraction is 1/s,
            // because the point is at distance 1 from the focus in units of that ray.
            double fx = p.focus.getX() - p.center.getX();
            double fy = p.focus.getY() - p.center.getY();
            double b = 2 * (fx * dx + fy * dy);
            double c = fx * fx + fy * fy - p.radius * (double) p.radius;
            double disc = b * b - 4 * a * c;
            if (disc < 0) {
                return p.colorForFraction(1.0f);
            }
            double s = (-b + Math.sqrt(disc)) / (2 * a);
            if (s <= 0) {
                return p.colorForFraction(1.0f);
            }
            return p.colorForFraction((float) (1.0 / s));
        }
    }
}
