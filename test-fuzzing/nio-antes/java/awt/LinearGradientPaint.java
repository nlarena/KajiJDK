package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

/**
 * A linear gradient with **several stops**.
 *
 * <p>It is {@link GradientPaint} with more than two colours and with the three cycle options
 * instead of two. The geometry is the same: the colour depends on how far along the segment one is,
 * and does not change in the perpendicular direction.
 */
public final class LinearGradientPaint extends MultipleGradientPaint {

    private final Point2D start;
    private final Point2D end;

    /**
     * With the ends by coordinates, without cycling and in sRGB.
     *
     * @throws IllegalArgumentException if the two ends coincide, if there are fewer than two stops
     *     or if the fractions do not increase
     * @throws NullPointerException if any of the arrays is missing
     */
    public LinearGradientPaint(float startX, float startY, float endX, float endY,
            float[] fractions, Color[] colors) {
        this(new Point2D.Float(startX, startY), new Point2D.Float(endX, endY), fractions, colors,
                CycleMethod.NO_CYCLE);
    }

    /**
     * With the ends by coordinates and the given cycle.
     *
     * @throws IllegalArgumentException if the two ends coincide or the stops are not valid
     */
    public LinearGradientPaint(float startX, float startY, float endX, float endY,
            float[] fractions, Color[] colors, CycleMethod cycleMethod) {
        this(new Point2D.Float(startX, startY), new Point2D.Float(endX, endY), fractions, colors,
                cycleMethod);
    }

    /**
     * With the ends as objects, without cycling and in sRGB.
     *
     * @throws IllegalArgumentException if the two ends coincide or the stops are not valid
     */
    public LinearGradientPaint(Point2D start, Point2D end, float[] fractions, Color[] colors) {
        this(start, end, fractions, colors, CycleMethod.NO_CYCLE);
    }

    /**
     * With the ends as objects and the given cycle.
     *
     * @throws IllegalArgumentException if the two ends coincide or the stops are not valid
     */
    public LinearGradientPaint(Point2D start, Point2D end, float[] fractions, Color[] colors,
            CycleMethod cycleMethod) {
        this(start, end, fractions, colors, cycleMethod, ColorSpaceType.SRGB,
                new AffineTransform());
    }

    /**
     * The general constructor.
     *
     * @throws NullPointerException if any of the arguments is missing
     * @throws IllegalArgumentException if the two ends coincide, if there are fewer than two stops
     *     or if the fractions do not increase
     */
    public LinearGradientPaint(Point2D start, Point2D end, float[] fractions, Color[] colors,
            CycleMethod cycleMethod, ColorSpaceType colorSpace,
            AffineTransform gradientTransform) {
        super(fractions, colors, cycleMethod, colorSpace, gradientTransform);
        if (start == null || end == null) {
            throw new NullPointerException("Start and end points must be non-null");
        }
        if (start.equals(end)) {
            throw new IllegalArgumentException("Start point cannot equal endpoint");
        }
        this.start = new Point2D.Double(start.getX(), start.getY());
        this.end = new Point2D.Double(end.getX(), end.getY());
    }

    /** Where the gradient starts. */
    public Point2D getStartPoint() {
        return new Point2D.Double(this.start.getX(), this.start.getY());
    }

    /** Where it ends. */
    public Point2D getEndPoint() {
        return new Point2D.Double(this.end.getX(), this.end.getY());
    }

    /**
     * Builds the machine that generates the pixels.
     *
     * <p>If the combined transformation cannot be inverted, the gradient degrades to the first
     * stop, which is all that is left when there is no geometry.
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

        private final double dx;
        private final double dy;
        private final double lengthSq;

        GradientContext(AffineTransform xform) throws NoninvertibleTransformException {
            super(xform);
            this.dx = LinearGradientPaint.this.end.getX() - LinearGradientPaint.this.start.getX();
            this.dy = LinearGradientPaint.this.end.getY() - LinearGradientPaint.this.start.getY();
            this.lengthSq = this.dx * this.dx + this.dy * this.dy;
        }

        int colorAt(double ux, double uy) {
            double t = ((ux - LinearGradientPaint.this.start.getX()) * this.dx
                    + (uy - LinearGradientPaint.this.start.getY()) * this.dy) / this.lengthSq;
            return LinearGradientPaint.this.colorForFraction((float) t);
        }
    }
}
